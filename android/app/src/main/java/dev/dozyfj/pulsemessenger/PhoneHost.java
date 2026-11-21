package dev.dozyfj.pulsemessenger;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PhoneHost {
    private static PhoneHost instance;
    private final Context context;
    private int port;
    private PhoneServerService service;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Runnable> listeners = new LinkedHashSet();
    private Phase phase = Phase.STOPPED;
    private String fingerprint = "";

    enum Phase {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        ERROR
    }

    static synchronized PhoneHost get(Context context) {
        PhoneHost phoneHost;
        synchronized (PhoneHost.class) {
            if (instance == null) {
                instance = new PhoneHost(context.getApplicationContext());
            }
            phoneHost = instance;
        }
        return phoneHost;
    }

    private PhoneHost(Context context) {
        this.context = context;
    }

    Phase phase() {
        return this.phase;
    }

    void bind(Runnable listener) {
        this.listeners.add(listener);
        listener.run();
    }

    void unbind(Runnable listener) {
        this.listeners.remove(listener);
    }

    private void changed() {
        Iterator it = new ArrayList(this.listeners).iterator();
        while (it.hasNext()) {
            Runnable listener = (Runnable) it.next();
            listener.run();
        }
    }

    void start() {
        if (this.phase == Phase.STOPPED || this.phase == Phase.ERROR) {
            this.phase = Phase.STARTING;
            changed();
            try {
                this.context.startForegroundService(new Intent(this.context, (Class<?>) PhoneServerService.class));
            } catch (RuntimeException e) {
                this.phase = Phase.ERROR;
                changed();
            }
        }
    }

    void stop() {
        if (this.phase == Phase.RUNNING || this.phase == Phase.STARTING) {
            this.phase = Phase.STOPPING;
            changed();
            this.context.stopService(new Intent(this.context, (Class<?>) PhoneServerService.class));
        }
    }

    void attach(PhoneServerService owner) {
        this.service = owner;
        this.phase = Phase.STARTING;
        changed();
    }

    void running(final PhoneServerService owner, final String pin, final int actualPort) {
        this.main.post(() -> {
            if (this.service != owner || this.phase == Phase.STOPPING) {
                return;
            }
            this.fingerprint = pin;
            this.port = actualPort;
            this.phase = Phase.RUNNING;
            changed();

        });
    }

    void failed(final PhoneServerService owner) {
        this.main.post(() -> {
            if (this.service == owner) {
                this.phase = Phase.ERROR;
                changed();
                owner.stopSelf();
            }

        });
    }

    void stopping(PhoneServerService owner) {
        if (this.service != owner || this.phase == Phase.ERROR) {
            return;
        }
        this.phase = Phase.STOPPING;
        changed();
    }

    void stopped(final PhoneServerService owner) {
        this.main.post(() -> {
            if (this.service != owner) {
                return;
            }
            this.service = null;
            this.fingerprint = "";
            if (this.phase != Phase.ERROR) {
                this.phase = Phase.STOPPED;
            }
            changed();

        });
    }

    String localUrl() {
        return this.phase == Phase.RUNNING ? url("127.0.0.1") : "";
    }

    private String url(String address) {
        return "https://" + address + ":" + this.port + "#sha256=" + this.fingerprint;
    }

    List<String> lanUrls() {
        if (this.phase != Phase.RUNNING) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        try {
            Iterator it = Collections.list(NetworkInterface.getNetworkInterfaces()).iterator();
            while (it.hasNext()) {
                NetworkInterface network = (NetworkInterface) it.next();
                if (network.isUp() && !network.isLoopback()) {
                    Iterator it2 = Collections.list(network.getInetAddresses()).iterator();
                    while (it2.hasNext()) {
                        InetAddress address = (InetAddress) it2.next();
                        if ((address instanceof Inet4Address) && address.isSiteLocalAddress()) {
                            result.add(url(address.getHostAddress()));
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return result;
    }
}
