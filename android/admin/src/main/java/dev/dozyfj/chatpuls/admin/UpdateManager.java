package dev.dozyfj.chatpuls.admin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

final class UpdateManager {
    private static UpdateManager instance;
    private File apk;
    private final Source client;
    private Listener listener;
    private boolean offered;
    private UpdateClient.Update update;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> UpdateManager.newWorkerThread(runnable));
    private final Handler main = new Handler(Looper.getMainLooper());
    private Phase phase = Phase.IDLE;
    private long lastCheck;

    interface Listener {
        void changed();
    }

    enum Phase {
        IDLE,
        CHECKING,
        DOWNLOADING,
        CURRENT,
        UNPUBLISHED,
        READY,
        ERROR
    }

    interface Source {
        UpdateClient.Update check() throws Exception;

        File download(UpdateClient.Update update) throws Exception;

        void verifyReady(File file, UpdateClient.Update update) throws Exception;
    }

    static synchronized UpdateManager get(Context context) {
        UpdateManager updateManager;
        synchronized (UpdateManager.class) {
            if (instance == null) {
                instance = new UpdateManager(new UpdateClient(context));
            }
            updateManager = instance;
        }
        return updateManager;
    }

    static Thread newWorkerThread(Runnable task) {
        Thread thread = new Thread(task, "admin-updates");
        thread.setDaemon(true);
        return thread;
    }

    UpdateManager(Source client) {
        this.client = client;
    }

    Phase phase() {
        return this.phase;
    }

    UpdateClient.Update update() {
        return this.update;
    }

    void bind(Listener listener) {
        this.listener = listener;
        listener.changed();
    }

    void unbind(Listener listener) {
        if (this.listener == listener) {
            this.listener = null;
        }
    }

    private void changed() {
        if (this.listener != null) {
            this.listener.changed();
        }
    }

    boolean takeOffer() {
        if (this.phase != Phase.READY || this.offered) {
            return false;
        }
        this.offered = true;
        return true;
    }

    void checkIfNeeded() {
        checkIfStale(60_000L);
    }

    /**
     * Проверяет обновления, если с прошлой проверки прошло больше maxAge.
     * Раньше проверка шла только при открытии приложения, а уже скачанное обновление
     * больше не перепроверялось — поэтому новые версии приходили с задержкой.
     */
    void checkIfStale(long maxAge) {
        if (this.phase == Phase.CHECKING || this.phase == Phase.DOWNLOADING) {
            return;
        }
        if (this.phase != Phase.IDLE && android.os.SystemClock.elapsedRealtime() - this.lastCheck < maxAge) {
            return;
        }
        check();
    }

    void check() {
        if (this.phase == Phase.CHECKING || this.phase == Phase.DOWNLOADING) {
            return;
        }
        final UpdateClient.Update ready = this.phase == Phase.READY ? this.update : null;
        final File readyApk = this.phase == Phase.READY ? this.apk : null;
        this.lastCheck = android.os.SystemClock.elapsedRealtime();
        this.phase = Phase.CHECKING;
        changed();
        this.executor.execute(() -> {
            try {
                final UpdateClient.Update next = this.client.check();
                if (next != null && ready != null && readyApk != null && next.code() == ready.code() && readyApk.isFile()) {
                    // Та же версия уже скачана — не качаем заново и не показываем предложение повторно.
                    this.main.post(() -> {
                        this.update = ready;
                        this.apk = readyApk;
                        this.phase = Phase.READY;
                        changed();
                    });
                    return;
                }
                if (next == null) {
                    this.main.post(() -> {
                        this.update = null;
                        this.apk = null;
                        this.phase = Phase.CURRENT;
                        changed();

                    });
                    return;
                }
                this.main.post(() -> {
                    this.phase = Phase.DOWNLOADING;
                    changed();

                });
                final File file = this.client.download(next);
                this.main.post(() -> {
                    this.update = next;
                    this.apk = file;
                    this.offered = false;
                    this.phase = Phase.READY;
                    changed();

                });
            } catch (UpdateClient.FeedMissing e) {
                this.main.post(() -> {
                    this.update = null;
                    this.apk = null;
                    this.phase = Phase.UNPUBLISHED;
                    changed();

                });
            } catch (Exception e2) {
                this.main.post(() -> {
                    this.update = null;
                    this.apk = null;
                    this.phase = Phase.ERROR;
                    changed();

                });
            }

        });
    }

    void verify(final Consumer<File> success, final Runnable failure) {
        Runnable failure2;
        if (this.phase != Phase.READY || this.update == null) {
            failure2 = failure;
        } else {
            if (this.apk != null) {
                final UpdateClient.Update expected = this.update;
                final File file = this.apk;
                this.executor.execute(() -> {
                    try {
                        this.client.verifyReady(file, expected);
                        this.main.post(() -> success.accept(file));
                    } catch (Exception e) {
                        this.main.post(() -> {
                            this.update = null;
                            this.apk = null;
                            this.phase = Phase.ERROR;
                            changed();
                            failure.run();

                        });
                    }

                });
                return;
            }
            failure2 = failure;
        }
        failure2.run();
    }

}
