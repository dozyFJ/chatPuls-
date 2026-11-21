package dev.dozyfj.chatpuls.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONObject;

/**
 * Клиент admin API серверов chatPuls (сервер в телефоне и Node.js).
 * Адрес с «#sha256=…» — самоподписанный сервер телефона: доверяем только сертификату с этим отпечатком.
 */
final class AdminApi {
    static final class Failure extends IOException {
        final int code;

        Failure(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }

    record Credentials(String server, String key) {}

    private static final Pattern KEY = Pattern.compile("\\b([a-f0-9]{64})\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");

    private final String base;
    private final String key;

    AdminApi(String base, String key) {
        this.base = base;
        this.key = key;
    }

    /** Достаёт адрес и ключ из текста, которым поделился chatPuls («Ключ администратора»). */
    static Credentials parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher url = URL_PATTERN.matcher(text);
        Matcher key = KEY.matcher(text.replaceAll("sha256=[a-f0-9]{64}", ""));
        return url.find() && key.find() ? new Credentials(url.group(1), key.group(1)) : null;
    }

    static String normalizeUrl(String input) throws IOException {
        URL url = new URL(input.trim());
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        String host = url.getHost().toLowerCase(Locale.ROOT);
        boolean local = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("10.0.2.2");
        String ref = url.getRef();
        if ((!protocol.equals("https") && !(protocol.equals("http") && local)) || host.isEmpty() || url.getUserInfo() != null
                || url.getQuery() != null || (ref != null && !ref.matches("sha256=[a-f0-9]{64}"))
                || (!url.getPath().isEmpty() && !url.getPath().equals("/"))) {
            throw new IOException("Нужен HTTPS-адрес сервера без пути");
        }
        int port = url.getPort();
        return protocol + "://" + host + (port < 0 ? "" : ":" + port) + (ref != null ? "#" + ref : "");
    }

    JSONObject get(String path) throws Exception {
        return request(path, null);
    }

    JSONObject post(String path, JSONObject body) throws Exception {
        return request(path, body);
    }

    static String encode(String value) throws IOException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private JSONObject request(String path, JSONObject payload) throws Exception {
        URL endpoint = new URL(this.base);
        URL url = new URL(this.base.split("#", 2)[0] + path);
        boolean loopback = endpoint.getHost().equals("127.0.0.1") || endpoint.getHost().equals("localhost");
        HttpURLConnection connection = (HttpURLConnection) (loopback ? url.openConnection(Proxy.NO_PROXY) : url.openConnection());
        String ref = endpoint.getRef();
        if (ref != null) {
            if (!(connection instanceof HttpsURLConnection)) {
                throw new IOException("Отпечаток работает только с HTTPS");
            }
            pin((HttpsURLConnection) connection, ref.substring("sha256=".length()));
        }
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("X-Pulse-Admin", this.key);
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
                throw new Failure(status);
            }
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] part = new byte[8192];
                int n;
                while ((n = input.read(part)) != -1) {
                    if (buffer.size() + n > 4_000_000) {
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

    private static String fingerprint(X509Certificate certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder out = new StringBuilder();
        for (byte b : digest) {
            out.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
        }
        return out.toString();
    }

    private static void pin(HttpsURLConnection https, String expected) throws Exception {
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
                    if (chain.length == 0 || !expected.equals(fingerprint(chain[0]))) {
                        throw new CertificateException("Отпечаток сервера не совпадает");
                    }
                    chain[0].checkValidity();
                } catch (CertificateException error) {
                    throw error;
                } catch (Exception error) {
                    throw new CertificateException(error);
                }
            }
        }}, null);
        https.setSSLSocketFactory(tls.getSocketFactory());
        https.setHostnameVerifier((hostname, session) -> {
            try {
                return expected.equals(fingerprint((X509Certificate) session.getPeerCertificates()[0]));
            } catch (Exception e) {
                return false;
            }
        });
    }
}
