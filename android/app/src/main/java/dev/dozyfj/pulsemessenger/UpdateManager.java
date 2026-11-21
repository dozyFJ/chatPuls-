package dev.dozyfj.pulsemessenger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import dev.dozyfj.pulsemessenger.UpdateClient;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> UpdateManager.newStep0(runnable));
    private final Handler main = new Handler(Looper.getMainLooper());
    private Phase phase = Phase.IDLE;

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

    static Thread newStep0(Runnable task) {
        Thread thread = new Thread(task, "pulse-updates");
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
        if (this.phase == Phase.IDLE || this.phase == Phase.CURRENT || this.phase == Phase.ERROR) {
            check();
        }
    }

    void check() {
        if (this.phase == Phase.CHECKING || this.phase == Phase.DOWNLOADING) {
            return;
        }
        this.phase = Phase.CHECKING;
        changed();
        this.executor.execute(() -> UpdateManager.this.checkStep6());
    }

    public void checkStep6() {
        try {
            final UpdateClient.Update next = this.client.check();
            if (next == null) {
                this.main.post(() -> UpdateManager.this.checkStep1());
                return;
            }
            this.main.post(() -> UpdateManager.this.checkStep2());
            final File file = this.client.download(next);
            this.main.post(() -> UpdateManager.this.checkStep3(next, file));
        } catch (UpdateClient.FeedMissing e) {
            this.main.post(() -> UpdateManager.this.checkStep4());
        } catch (Exception e2) {
            this.main.post(() -> UpdateManager.this.checkStep5());
        }
    }

    public void checkStep1() {
        this.update = null;
        this.apk = null;
        this.phase = Phase.CURRENT;
        changed();
    }

    public void checkStep2() {
        this.phase = Phase.DOWNLOADING;
        changed();
    }

    public void checkStep3(UpdateClient.Update next, File file) {
        this.update = next;
        this.apk = file;
        this.offered = false;
        this.phase = Phase.READY;
        changed();
    }

    public void checkStep4() {
        this.update = null;
        this.apk = null;
        this.phase = Phase.UNPUBLISHED;
        changed();
    }

    public void checkStep5() {
        this.update = null;
        this.apk = null;
        this.phase = Phase.ERROR;
        changed();
    }

    void verify(final Consumer<File> success, final Runnable failure) {
        Runnable failure2;
        if (this.phase != Phase.READY || this.update == null) {
            failure2 = failure;
        } else {
            if (this.apk != null) {
                final UpdateClient.Update expected = this.update;
                final File file = this.apk;
                this.executor.execute(() -> UpdateManager.this.verifyStep9(file, expected, success, failure));
                return;
            }
            failure2 = failure;
        }
        failure2.run();
    }

    public void verifyStep9(final File file, UpdateClient.Update expected, final Consumer success, final Runnable failure) {
        try {
            this.client.verifyReady(file, expected);
            this.main.post(() -> success.accept(file));
        } catch (Exception e) {
            this.main.post(() -> UpdateManager.this.verifyStep8(failure));
        }
    }

    public void verifyStep8(Runnable failure) {
        this.update = null;
        this.apk = null;
        this.phase = Phase.ERROR;
        changed();
        failure.run();
    }
}
