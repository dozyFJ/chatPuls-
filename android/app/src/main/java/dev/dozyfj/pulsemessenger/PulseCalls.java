package dev.dozyfj.pulsemessenger;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import dev.dozyfj.pulsemessenger.CallHistory;
import dev.dozyfj.pulsemessenger.PulseApi;
import dev.dozyfj.pulsemessenger.PulseCalls;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.json.JSONArray;
import org.json.JSONObject;

final class PulseCalls {
    private static final String DOCUMENT = "https://appassets.androidplatform.net/pulse-call.html";
    private static final String ENDPOINT = "/v1/calls";
    private static final String HOST = "appassets.androidplatform.net";
    static final int MICROPHONE_PERMISSION = 46;
    private Session active;
    private final Activity activity;
    private final Function<String, Bitmap> avatar;
    private final Predicate<String> blocked;
    private final Supplier<Account> currentAccount;
    private final BiConsumer<Account, CallHistory.Entry> finished;
    private Session permissionTarget;
    private boolean polling;
    private boolean running;
    private Account unsupported;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Runnable poll = () -> PulseCalls.this.poll();

    record Account(String server, String token, String username) {}

    static final class Session {
        final Account account;
        boolean answered;
        long connectedAt;
        CallScreen dialog;
        Button endButton;
        final String id;
        Button muteButton;
        boolean muted;
        final String offer;
        final String peer;
        boolean published;
        Ringtone ringtone;
        TextView status;
        WebView web;
        final long startedAt = System.currentTimeMillis();
        int previousAudioMode = -1;

        Session(Account account, String id, String peer, String offer) {
            this.account = account;
            this.id = id;
            this.peer = peer;
            this.offer = offer;
        }

        boolean outgoing() {
            return this.offer == null;
        }
    }

    PulseCalls(Activity activity, Supplier<Account> currentAccount, Predicate<String> blocked, BiConsumer<Account, CallHistory.Entry> finished, Function<String, Bitmap> avatar) {
        this.activity = activity;
        this.currentAccount = currentAccount;
        this.blocked = blocked;
        this.finished = finished;
        this.avatar = avatar;
    }

    void resume() {
        this.running = true;
        schedulePoll(0L);
    }

    void pause() {
        this.running = false;
        this.main.removeCallbacks(this.poll);
        if (this.active != this.permissionTarget) {
            finish(this.active, true);
        }
    }

    void destroy() {
        this.running = false;
        this.main.removeCallbacksAndMessages(null);
        finish(this.active, true);
        this.permissionTarget = null;
        this.network.shutdown();
    }

    void call(String peer) {
        Account account = this.currentAccount.get();
        if (account == null) {
            toast("Для звонка войдите на сервер Pulse");
            return;
        }
        if (this.active != null || this.permissionTarget != null) {
            return;
        }
        if (this.blocked.test(peer)) {
            toast("Сначала разблокируйте контакт");
        } else {
            requestMicrophone(new Session(account, UUID.randomUUID().toString(), peer, null));
        }
    }

    public void requestMicrophone(Session session) {
        if (this.activity.checkSelfPermission("android.permission.RECORD_AUDIO") == 0) {
            startAudio(session);
        } else {
            this.permissionTarget = session;
            this.activity.requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 46);
        }
    }

    void microphoneResult(boolean granted) {
        Session target = this.permissionTarget;
        this.permissionTarget = null;
        if (target == null) {
            return;
        }
        if (!granted || !target.account.equals(this.currentAccount.get())) {
            if (this.active == target) {
                finish(target, true);
            }
            if (!granted) {
                toast("Для звонка нужен доступ к микрофону");
                return;
            }
            return;
        }
        startAudio(target);
    }

    private void schedulePoll(long delay) {
        this.main.removeCallbacks(this.poll);
        if (this.running) {
            this.main.postDelayed(this.poll, delay);
        }
    }

    public void poll() {
        if (!this.running || this.polling) {
            return;
        }
        final Account account = this.currentAccount.get();
        if (this.active != null && !this.active.account.equals(account)) {
            finish(this.active, true);
        }
        if (account == null || (this.active == null && account.equals(this.unsupported))) {
            schedulePoll(2500L);
        } else {
            this.polling = true;
            this.network.execute(() -> PulseCalls.this.pollStep3(account));
        }
    }

    public void pollStep3(final Account account) {
        Handler handler;
        Runnable runnable;
        try {
            try {
                final JSONArray items = new PulseApi(account.server, account.token).request(ENDPOINT, null).getJSONArray("calls");
                this.main.post(() -> PulseCalls.this.pollStep0(account, items));
                handler = this.main;
                runnable = () -> PulseCalls.this.pollStep2();
            } catch (PulseApi.ApiError error) {
                if (error.code == 404) {
                    this.main.post(() -> PulseCalls.this.pollStep1(account));
                }
                handler = this.main;
                runnable = () -> PulseCalls.this.pollStep2();
            } catch (Exception e) {
                handler = this.main;
                runnable = () -> PulseCalls.this.pollStep2();
            }
            handler.post(runnable);
        } catch (Throwable th) {
            this.main.post(() -> PulseCalls.this.pollStep2());
            throw th;
        }
    }

    public void pollStep0(Account account, JSONArray items) {
        if (!this.running || !account.equals(this.currentAccount.get())) {
            return;
        }
        receive(account, items);
    }

    public void pollStep1(Account account) {
        this.unsupported = account;
    }

    public void pollStep2() {
        this.polling = false;
        schedulePoll(1200L);
    }

    private void receive(Account account, JSONArray items) {
        CallHistory.Outcome outcome;
        Session session = this.active;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null) {
                String state = item.optString("state");
                if (session != null && session.id.equals(item.optString("id"))) {
                    if (state.equals("ended")) {
                        String reason = item.optString("reason");
                        if (reason.equals("declined")) {
                            outcome = CallHistory.Outcome.DECLINED;
                        } else if (reason.equals("unavailable")) {
                            outcome = CallHistory.Outcome.UNAVAILABLE;
                        } else {
                            outcome = remoteOutcome(session);
                        }
                        finish(session, false, outcome);
                        toast("Звонок завершён");
                        return;
                    }
                    if (session.outgoing() && !session.answered && state.equals("answered") && session.web != null) {
                        session.answered = true;
                        session.status.setText("Соединение…");
                        session.web.evaluateJavascript("acceptAnswer(" + JSONObject.quote(item.optString("answer")) + ")", null);
                        return;
                    }
                    return;
                }
                if (session == null && state.equals("ringing") && account.username.equals(item.optString("to"))) {
                    String peer = item.optString("from");
                    Session incoming = new Session(account, item.optString("id"), peer, item.optString("offer"));
                    incoming.published = true;
                    if (this.blocked.test(peer)) {
                        request(incoming, "end", null);
                        return;
                    } else {
                        if (this.permissionTarget != null) {
                            return;
                        }
                        this.active = incoming;
                        showIncoming(incoming);
                        return;
                    }
                }
            }
        }
        if (session != null && session.published) {
            finish(session, false, remoteOutcome(session));
            toast("Звонок завершён");
        }
    }

    private void showIncoming(final Session session) {
        session.dialog = new CallScreen(this.activity, session.peer, "Входящий звонок", true, this.avatar.apply(session.peer));
        session.dialog.action("Отклонить", -5031099, () -> PulseCalls.this.showIncomingStep4(session));
        session.dialog.action("Принять", -14191540, () -> PulseCalls.this.requestMicrophone(session));
        session.dialog.show(() -> PulseCalls.this.showIncomingStep6(session));
        try {
            session.ringtone = RingtoneManager.getRingtone(this.activity, RingtoneManager.getDefaultUri(1));
            if (session.ringtone != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    session.ringtone.setLooping(true);
                }
                session.ringtone.play();
            }
        } catch (RuntimeException e) {
        }
    }

    public void showIncomingStep4(Session session) {
        finish(session, true, CallHistory.Outcome.DECLINED);
    }

    public void showIncomingStep6(Session session) {
        finish(session, true);
    }

    private void startAudio(final Session session) {
        if (this.activity.isFinishing() || this.activity.isDestroyed()) {
            return;
        }
        if (this.active != null && this.active != session) {
            return;
        }
        this.active = session;
        stopRingtone(session);
        if (session.dialog != null) {
            session.dialog.dismiss();
        }
        try {
            session.dialog = new CallScreen(this.activity, session.peer, "Ожидание сети…", !session.outgoing(), this.avatar.apply(session.peer));
            session.status = session.dialog.status;
            session.web = new WebView(this.activity);
            session.dialog.attachMedia(session.web);
            session.muteButton = session.dialog.microphone(() -> PulseCalls.startAudioStep7(session));
            session.endButton = session.dialog.action("Отмена", -5031099, () -> PulseCalls.this.startAudioStep8(session));
            session.dialog.show(() -> PulseCalls.this.startAudioStep9(session));
            AudioManager audio = (AudioManager) this.activity.getSystemService(AudioManager.class);
            session.previousAudioMode = audio.getMode();
            audio.setMode(3);
            configureWeb(session);
            this.main.postDelayed(() -> PulseCalls.this.startAudioStep10(session), 22000L);
        } catch (RuntimeException e) {
            fail(session, "Не удалось запустить аудиосвязь");
        }
    }

    static void startAudioStep7(Session session) {
        session.muted = !session.muted;
        session.web.evaluateJavascript("mute(" + session.muted + ")", null);
        session.muteButton.setText(session.muted ? "Микрофон выкл." : "Микрофон вкл.");
        session.muteButton.setSelected(session.muted);
    }

    public void startAudioStep8(Session session) {
        finish(session, true);
    }

    public void startAudioStep9(Session session) {
        finish(session, true);
    }

    public void startAudioStep10(Session session) {
        if (this.active != session || session.published) {
            return;
        }
        fail(session, "Не удалось подготовить звонок");
    }

    private void configureWeb(final Session session) {
        WebView web = session.web;
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(session), "PulseCall");
        web.setWebChromeClient(new MediaPermissions(session));
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                PulseCalls.this.fail(session, "Аудиосвязь прервалась");
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if ("GET".equals(request.getMethod()) && PulseCalls.DOCUMENT.equals(request.getUrl().toString())) {
                    try {
                        return new WebResourceResponse("text/html", "UTF-8", PulseCalls.this.activity.getAssets().open("pulse-call.html"));
                    } catch (IOException e) {
                    }
                }
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (PulseCalls.this.active != session || !PulseCalls.DOCUMENT.equals(url)) {
                    return;
                }
                view.evaluateJavascript("begin(" + JSONObject.quote(session.offer == null ? "" : session.offer) + ")", null);
            }
        });
        web.loadUrl(DOCUMENT);
    }

    class MediaPermissions extends WebChromeClient {
        final Session session;

        MediaPermissions(Session session) {
            this.session = session;
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            Handler handler = PulseCalls.this.main;
            final Session session = this.session;
            handler.post(() -> PulseCalls.MediaPermissions.this.onPermissionRequestStep0(session, request));
        }

        public void onPermissionRequestStep0(Session session, PermissionRequest request) {
            if (PulseCalls.this.active == session && "https".equals(request.getOrigin().getScheme()) && PulseCalls.HOST.equals(request.getOrigin().getHost()) && PulseCalls.this.activity.checkSelfPermission("android.permission.RECORD_AUDIO") == 0) {
                request.grant(new String[]{"android.webkit.resource.AUDIO_CAPTURE"});
            } else {
                request.deny();
            }
        }
    }

    public final class Bridge {
        private final Session session;

        Bridge(Session session) {
            this.session = session;
        }

        @JavascriptInterface
        public void description(final String sdp) {
            PulseCalls.this.main.post(() -> PulseCalls.Bridge.this.descriptionStep0(sdp));
        }

        public void descriptionStep0(String sdp) {
            if (PulseCalls.this.active != this.session || this.session.web == null) {
                return;
            }
            if (this.session.muted) {
                this.session.web.evaluateJavascript("mute(true)", null);
            }
            this.session.published = false;
            PulseCalls.this.request(this.session, this.session.outgoing() ? "offer" : "answer", sdp);
        }

        @JavascriptInterface
        public void event(final String state) {
            PulseCalls.this.main.post(() -> PulseCalls.Bridge.this.eventStep1(state));
        }

        public void eventStep1(String state) {
            if (PulseCalls.this.active != this.session || this.session.web == null) {
                return;
            }
            if ("connected".equals(state) && this.session.connectedAt == 0) {
                this.session.connectedAt = SystemClock.elapsedRealtime();
                this.session.endButton.setText("Завершить");
                PulseCalls.this.updateDuration(this.session);
                return;
            }
            if ("failed".equals(state)) {
                PulseCalls.this.fail(this.session, "Не удалось соединиться. Проверьте, что оба телефона в одной сети Wi‑Fi");
            }
        }
    }

    public void updateDuration(final Session session) {
        if (this.active != session) {
            return;
        }
        long seconds = (SystemClock.elapsedRealtime() - session.connectedAt) / 1000;
        session.status.setText(String.format(Locale.getDefault(), "На связи · %02d:%02d", Long.valueOf(seconds / 60), Long.valueOf(seconds % 60)));
        this.main.postDelayed(() -> PulseCalls.this.updateDuration(session), 1000L);
    }

    public void request(Session session, String action, String sdp) {
        request(session, action, sdp, null);
    }

    private void request(final Session session, final String action, final String sdp, final String reason) {
        this.network.execute(() -> PulseCalls.this.requestStep14(session, action, sdp, reason));
    }

    public void requestStep14(final Session session, String action, String sdp, String reason) {
        try {
            JSONObject body = new JSONObject().put("id", session.id).put("action", action);
            if (sdp != null) {
                body.put("sdp", sdp);
            }
            if (reason != null) {
                body.put("reason", reason);
            }
            if (action.equals("offer")) {
                body.put("to", session.peer);
            }
            new PulseApi(session.account.server, session.account.token).request(ENDPOINT, body);
            if (!action.equals("end")) {
                this.main.post(() -> PulseCalls.this.requestStep12(session));
            }
        } catch (Exception error) {
            if (action.equals("end")) {
                return;
            }
            this.main.post(() -> PulseCalls.this.requestStep13(error, session));
        }
    }

    public void requestStep12(Session session) {
        if (this.active != session) {
            return;
        }
        this.unsupported = null;
        session.published = true;
        if (session.outgoing() && session.connectedAt == 0) {
            session.status.setText("Ожидание ответа…");
        }
        schedulePoll(0L);
    }

    public void requestStep13(Exception error, Session session) {
        if (error instanceof PulseApi.ApiError) {
            PulseApi.ApiError api = (PulseApi.ApiError) error;
            if (api.code == 409) {
                fail(session, "Собеседник занят или звонок уже завершён");
                return;
            } else if (api.code == 404) {
                fail(session, "Собеседник или функция звонков недоступны на сервере");
                return;
            }
        }
        fail(session, "Сервер звонков не ответил");
    }

    public void fail(Session session, String message) {
        if (this.active != session) {
            return;
        }
        finish(session, true, CallHistory.Outcome.UNAVAILABLE);
        toast(message);
    }

    private CallHistory.Outcome remoteOutcome(Session session) {
        return session.connectedAt > 0 ? CallHistory.Outcome.COMPLETED : CallHistory.Outcome.MISSED;
    }

    private void finish(Session session, boolean notify) {
        if (session == null) {
            return;
        }
        CallHistory.Outcome outcome = (session.outgoing() && session.connectedAt == 0) ? CallHistory.Outcome.CANCELED : remoteOutcome(session);
        finish(session, notify, outcome);
    }

    private void finish(Session session, boolean notify, CallHistory.Outcome outcome) {
        String reason;
        if (session != this.active) {
            return;
        }
        this.active = null;
        stopRingtone(session);
        if (this.permissionTarget == session) {
            this.permissionTarget = null;
        }
        if (notify) {
            switch (outcome) {
                case DECLINED:
                    reason = "declined";
                    break;
                case UNAVAILABLE:
                    reason = "unavailable";
                    break;
                default:
                    reason = null;
                    break;
            }
            request(session, "end", null, reason);
        }
        if (session.dialog != null) {
            session.dialog.dismiss();
        }
        if (session.web != null) {
            session.web.removeJavascriptInterface("PulseCall");
            ViewParent parent = session.web.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup parent2 = (ViewGroup) parent;
                parent2.removeView(session.web);
            }
            session.web.destroy();
        }
        if (session.previousAudioMode >= 0) {
            ((AudioManager) this.activity.getSystemService(AudioManager.class)).setMode(session.previousAudioMode);
        }
        this.finished.accept(session.account, new CallHistory.Entry(session.peer, !session.outgoing(), outcome, session.startedAt));
    }

    private void stopRingtone(Session session) {
        if (session.ringtone != null) {
            session.ringtone.stop();
            session.ringtone = null;
        }
    }

    private void toast(String message) {
        Toast.makeText(this.activity, message, 1).show();
    }
}
