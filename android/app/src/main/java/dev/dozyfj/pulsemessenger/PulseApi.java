package dev.dozyfj.pulsemessenger;

import dev.dozyfj.pulsemessenger.ChatStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONArray;
import org.json.JSONObject;

final class PulseApi {
    private final String base;
    private final String token;

    static String normalizeUrl(String input) throws Exception {
        URL url = new URL(input.trim());
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        String host = url.getHost().toLowerCase(Locale.ROOT);
        boolean local = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("10.0.2.2");
        if ((!protocol.equals("https") && (!protocol.equals("http") || !local)) || host.isEmpty() || url.getUserInfo() != null || url.getQuery() != null || ((url.getRef() != null && (!protocol.equals("https") || !url.getRef().matches("sha256=[a-f0-9]{64}"))) || (!url.getPath().isEmpty() && !url.getPath().equals("/")))) {
            throw new IOException("Укажите HTTPS-адрес сервера без пути");
        }
        int port = url.getPort();
        if (port == 0 || port > 65535 || port < -1 || host.contains(":")) {
            throw new IOException("Некорректный адрес сервера");
        }
        return protocol + "://" + host + (port < 0 ? "" : ":" + port) + (url.getRef() != null ? "#" + url.getRef() : "");
    }

    static final class ApiError extends IOException {
        final int code;

        ApiError(int code) {
            super("Ошибка сервера: " + code);
            this.code = code;
        }
    }

    PulseApi(String base, String token) {
        this.base = base;
        this.token = token;
    }

    JSONObject request(String path, JSONObject payload) throws Exception {
        URL endpoint = new URL(this.base);
        String address = this.base.split("#", 2)[0];
        URL url = new URL(address + path);
        boolean loopback = endpoint.getHost().equals("127.0.0.1") || endpoint.getHost().equals("localhost");
        HttpURLConnection connection = (HttpURLConnection) (loopback ? url.openConnection(Proxy.NO_PROXY) : url.openConnection());
        if (endpoint.getRef() != null) {
            if (!(connection instanceof HttpsURLConnection) || !endpoint.getRef().matches("sha256=[a-f0-9]{64}")) {
                throw new IOException("Некорректный отпечаток сервера");
            }
            final String pin = endpoint.getRef().substring(7);
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    throw new CertificateException("Client authentication unsupported");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        if (chain.length == 0 || !pin.equals(PhoneServerStore.hex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded())))) {
                            throw new CertificateException("Server fingerprint differs");
                        }
                        chain[0].checkValidity();
                    } catch (CertificateException error) {
                        throw error;
                    } catch (Exception error2) {
                        throw new CertificateException(error2);
                    }
                }
            }}, null);
            HttpsURLConnection https = (HttpsURLConnection) connection;
            https.setSSLSocketFactory(tls.getSocketFactory());
            https.setHostnameVerifier((str, sSLSession) -> PulseApi.matchesPin(pin, str, sSLSession));
        }
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false);
            if (this.token != null) {
                connection.setRequestProperty("Authorization", "Bearer " + this.token);
            }
            if (payload != null) {
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new ApiError(status);
            }
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] part = new byte[4096];
                int size = 0;
                int n;
                while ((n = input.read(part)) != -1) {
                    size += n;
                    if (size > 512000) {
                        throw new IOException("Ответ сервера слишком большой");
                    }
                    buffer.write(part, 0, n);
                }
                return new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    static boolean matchesPin(String pin, String hostname, SSLSession session) {
        try {
            return pin.equals(PhoneServerStore.hex(MessageDigest.getInstance("SHA-256").digest(session.getPeerCertificates()[0].getEncoded())));
        } catch (Exception e) {
            return false;
        }
    }

    static ChatStore.Message parseMessage(JSONObject json) throws Exception {
        return new ChatStore.Message(json.getString("id"), json.getString("from"), json.getString("to"), json.getString("text"), json.getLong("time"), json.getLong("sequence"), 0, VoiceNote.readAudio(json));
    }

    ChatStore.Message send(ChatStore.Message message) throws Exception {
        JSONObject body = new JSONObject().put("id", message.id()).put("to", message.to()).put("text", message.text());
        return message.audio().isEmpty() ? parseMessage(request("/v1/messages", body)) : parseMessage(request("/v1/voice", body.put("audio", message.audio())));
    }

    /** Пачка синхронизации: новые сообщения и удалённые модератором (from, id). */
    record Batch(List<ChatStore.Message> messages, List<String[]> removed, long removedCursor) {}

    Batch fetch(long after, long removedAfter) throws Exception {
        JSONObject response = request("/v1/messages?after=" + after + "&removed=" + removedAfter, null);
        JSONArray array = response.getJSONArray("messages");
        List<ChatStore.Message> messages = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            messages.add(parseMessage(array.getJSONObject(i)));
        }
        List<String[]> removed = new ArrayList<>();
        long cursor = removedAfter;
        JSONArray gone = response.optJSONArray("removed");
        for (int i = 0; gone != null && i < gone.length(); i++) {
            JSONObject item = gone.getJSONObject(i);
            removed.add(new String[]{item.getString("from"), item.getString("id")});
            cursor = Math.max(cursor, item.getLong("rseq"));
        }
        return new Batch(messages, removed, cursor);
    }

    void report(String target, String messageId, String reason) throws Exception {
        JSONObject body = new JSONObject().put("to", target).put("reason", reason);
        if (messageId != null) {
            body.put("id", messageId);
        }
        request("/v1/reports", body);
    }
}
