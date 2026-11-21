package dev.dozyfj.pulsemessenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLException;

public final class PhoneServerService extends Service {
    private static final String STOP = "dev.dozyfj.pulsemessenger.STOP_HOST";
    private volatile boolean destroyed;
    private PhoneHost host;
    private PhoneServer server;
    private PhoneServerStore store;
    private PowerManager.WakeLock wake;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        this.host = PhoneHost.get(this);
        this.host.attach(this);
        NotificationManager notifications = (NotificationManager) getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("phone-server", "Сервер на телефоне", 2));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) MainActivity.class), 201326592);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, (Class<?>) PhoneServerService.class).setAction(STOP), 201326592);
        Notification notification = new Notification.Builder(this, "phone-server").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Сервер Pulse включён").setContentText("Открыть Pulse или остановить сервер").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder((Icon) null, "Остановить", stop).build()).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(73, notification, 16);
            } else {
                startForeground(73, notification);
            }
            this.worker.execute(() -> PhoneServerService.this.onCreateStep0());
        } catch (RuntimeException e) {
            this.host.failed(this);
        }
    }

    public void onCreateStep0() {
        try {
            PhoneIdentity identity = PhoneIdentity.load(false);
            if (this.destroyed) {
                return;
            }
            this.store = new PhoneServerStore(this);
            this.store.getWritableDatabase();
            if (this.destroyed) {
                return;
            }
            this.wake = ((PowerManager) getSystemService(PowerManager.class)).newWakeLock(1, "Pulse:PhoneServer");
            this.wake.acquire();
            try {
                startServer(identity);
            } catch (SSLException legacyError) {
                PhoneIdentity compatible = PhoneIdentity.load(true);
                if (compatible.fingerprint.equals(identity.fingerprint)) {
                    throw legacyError;
                }
                this.server.close();
                if (this.destroyed) {
                    return;
                }
                identity = compatible;
                startServer(identity);
            }
            this.host.running(this, identity.fingerprint, this.server.port());
        } catch (Exception error) {
            Log.e("PulsePhoneServer", "Phone server startup failed", error);
            this.host.failed(this);
        }
    }

    private void startServer(PhoneIdentity identity) throws Exception {
        this.server = new PhoneServer(this.store, identity, InetAddress.getByName("0.0.0.0"), 8443);
        this.server.start();
        String local = "https://127.0.0.1:" + this.server.port() + "#sha256=" + identity.fingerprint;
        if (!new PulseApi(local, null).request("/health", null).optBoolean("ok")) {
            throw new IOException("Phone server health check failed");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !STOP.equals(intent.getAction())) {
            return 2;
        }
        stopSelf();
        return 2;
    }

    @Override
    public void onDestroy() {
        this.destroyed = true;
        this.host.stopping(this);
        this.worker.execute(() -> PhoneServerService.this.onDestroyStep1());
        this.worker.shutdown();
        stopForeground(1);
        super.onDestroy();
    }

    public void onDestroyStep1() {
        try {
            if (this.server != null) {
                this.server.close();
            }
            if (this.store != null) {
                this.store.close();
            }
        } finally {
            if (this.wake != null && this.wake.isHeld()) {
                this.wake.release();
            }
            this.host.stopped(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
