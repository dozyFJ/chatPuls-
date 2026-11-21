package dev.dozyfj.pulsemessenger;

import dev.dozyfj.pulsemessenger.PhoneServerStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.json.JSONObject;

final class PhoneServer implements AutoCloseable {
    private Thread acceptor;
    private final SSLServerSocket listener;
    private final PhoneServerStore store;
    private final CallSessions calls = new CallSessions();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(16));
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final Map<String, long[]> attempts = new HashMap();

    PhoneServer(PhoneServerStore store, PhoneIdentity identity, InetAddress bind, int port) throws IOException {
        this.store = store;
        this.listener = (SSLServerSocket) identity.tls.getServerSocketFactory().createServerSocket(port, 16, bind);
        this.listener.setEnabledProtocols(new String[]{"TLSv1.2"});
    }

    int port() {
        return this.listener.getLocalPort();
    }

    void start() {
        this.acceptor = new Thread(() -> {
            while (!this.listener.isClosed()) {
                try {
                    final SSLSocket socket = (SSLSocket) this.listener.accept();
                    this.sockets.add(socket);
                    socket.setSoTimeout(8000);
                    try {
                        this.workers.execute(() -> PhoneServer.this.serve(socket));
                    } catch (RejectedExecutionException e) {
                        this.sockets.remove(socket);
                        socket.close();
                    }
                } catch (IOException e2) {
                    if (!this.listener.isClosed()) {
                        close();
                    }
                }
            }

        }, "pulse-phone-listener");
        this.acceptor.start();
    }

    private synchronized boolean allowed(String address) {
        final long now = System.currentTimeMillis();
        this.attempts.entrySet().removeIf(obj -> PhoneServer.isAttemptExpired(now, (Map.Entry) obj));
        long[] entry = this.attempts.get(address);
        if (entry != null) {
            long j = entry[1] + 1;
            entry[1] = j;
            return j <= 30;
        }
        if (this.attempts.size() >= 1024) {
            return false;
        }
        this.attempts.put(address, new long[]{now, 1});
        return true;
    }

    static boolean isAttemptExpired(long now, Map.Entry entry) {
        return now - ((long[]) entry.getValue())[0] >= 60000;
    }

    private static String line(InputStream input, int[] remaining) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            int b = input.read();
            if (b != -1) {
                int i = remaining[0] - 1;
                remaining[0] = i;
                if (i < 0) {
                    throw new PhoneServerStore.Failure(431);
                }
                if (b == 10) {
                    byte[] bytes = out.toByteArray();
                    if (bytes.length == 0 || bytes[bytes.length - 1] != 13) {
                        throw new PhoneServerStore.Failure(400);
                    }
                    return new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
                }
                out.write(b);
            } else {
                throw new IOException("Incomplete request");
            }
        }
    }

    private void serve(SSLSocket socket) {
        try (socket) {
            try {
                socket.startHandshake();
                InputStream input = socket.getInputStream();
                int[] remaining = {8192};
                String[] request = line(input, remaining).split(" ", -1);
                if (request.length != 3 || !request[2].equals("HTTP/1.1") || !request[1].startsWith("/")) {
                    throw new PhoneServerStore.Failure(400);
                }
                Map<String, String> headers = new HashMap<>();
                String header;
                while (!(header = line(input, remaining)).isEmpty()) {
                    int colon = header.indexOf(':');
                    if (colon < 1) {
                        throw new PhoneServerStore.Failure(400);
                    }
                    String name = header.substring(0, colon).toLowerCase(Locale.ROOT);
                    if (!name.matches("[a-z0-9-]+") || headers.put(name, header.substring(colon + 1).trim()) != null) {
                        throw new PhoneServerStore.Failure(400);
                    }
                }
                if (headers.containsKey("transfer-encoding")) {
                    throw new PhoneServerStore.Failure(400);
                }
                String lengthText = headers.getOrDefault("content-length", "0");
                if (!lengthText.matches("[0-9]{1,6}")) {
                    throw new PhoneServerStore.Failure(400);
                }
                int length = Integer.parseInt(lengthText);
                URI uri;
                try {
                    uri = new URI(request[1]);
                } catch (Exception error) {
                    throw new PhoneServerStore.Failure(400);
                }
                if (uri.getHost() != null || uri.getFragment() != null) {
                    throw new PhoneServerStore.Failure(400);
                }
                String path = uri.getPath();
                boolean voice = request[0].equals("POST") && path.equals("/v1/voice");
                if (length > (voice ? 100000 : 16384)) {
                    throw new PhoneServerStore.Failure(413);
                }
                boolean auth = request[0].equals("POST") && (path.equals("/v1/register") || path.equals("/v1/login"));
                boolean admin = path.startsWith("/v1/admin/");
                if (auth && !allowed(socket.getInetAddress().getHostAddress())) {
                    throw new PhoneServerStore.Failure(429);
                }
                PhoneServerStore.Actor actor = null;
                if (admin) {
                    try {
                        actor = this.store.adminActor(headers.get("x-pulse-admin"), headers.get("authorization"));
                    } catch (PhoneServerStore.Failure failure) {
                        if (!allowed("admin:" + socket.getInetAddress().getHostAddress())) {
                            throw new PhoneServerStore.Failure(429);
                        }
                        throw failure;
                    }
                }
                String user = null;
                if (!(request[0].equals("GET") && path.equals("/health")) && !auth && !admin) {
                    user = this.store.user(headers.get("authorization"));
                }
                JSONObject payload = null;
                if (request[0].equals("POST")) {
                    byte[] bytes = new byte[length];
                    int read = 0;
                    int n;
                    while (read < length && (n = input.read(bytes, read, length - read)) != -1) {
                        read += n;
                    }
                    if (read != length) {
                        throw new PhoneServerStore.Failure(400);
                    }
                    try {
                        payload = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                    } catch (Exception error) {
                        throw new PhoneServerStore.Failure(400);
                    }
                }
                JSONObject result;
                String query = uri.getRawQuery();
                if (admin) {
                    result = adminRequest(actor, request[0], path, query, payload);
                } else if (request[0].equals("GET") && path.equals("/health")) {
                    result = new JSONObject().put("ok", true);
                } else if (auth) {
                    result = this.store.authenticate(payload, path.equals("/v1/register"));
                } else if (request[0].equals("GET") && path.equals("/v1/calls")) {
                    result = this.calls.fetch(user);
                } else if (request[0].equals("POST") && path.equals("/v1/calls")) {
                    result = this.calls.change(user, payload, this.store);
                } else if (request[0].equals("POST") && (path.equals("/v1/messages") || voice)) {
                    result = this.store.send(user, payload, voice);
                } else if (request[0].equals("POST") && path.equals("/v1/reports")) {
                    result = this.store.report(user, payload);
                } else if (request[0].equals("GET") && path.equals("/v1/messages")) {
                    java.util.regex.Matcher cursors = java.util.regex.Pattern.compile("after=([0-9]{1,15})(?:&removed=([0-9]{1,15}))?").matcher(query == null ? "after=0" : query);
                    if (!cursors.matches()) {
                        throw new PhoneServerStore.Failure(400);
                    }
                    result = this.store.fetch(user, cursors.group(1), cursors.group(2) == null ? "0" : cursors.group(2));
                } else {
                    throw new PhoneServerStore.Failure(404);
                }
                reply(socket, 200, result.toString());
            } catch (PhoneServerStore.Failure failure) {
                reply(socket, failure.status, "{\"error\":\"request rejected\"}");
            } catch (Exception error) {
                if (!socket.isClosed()) {
                    reply(socket, 500, "{\"error\":\"server error\"}");
                }
            }
        } catch (IOException ignored) {
        } finally {
            this.sockets.remove(socket);
        }
    }

    private JSONObject adminRequest(PhoneServerStore.Actor actor, String method, String path, String query, JSONObject payload) throws Exception {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            for (String pair : query.split("&")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    params.put(pair.substring(0, equals), java.net.URLDecoder.decode(pair.substring(equals + 1), "UTF-8"));
                }
            }
        }
        if (method.equals("GET") && path.equals("/v1/admin/me")) {
            return this.store.adminMe(actor);
        }
        if (method.equals("POST") && path.equals("/v1/admin/roles")) {
            return this.store.adminRoles(actor, payload);
        }
        if (method.equals("GET") && path.equals("/v1/admin/stats")) {
            return this.store.adminStats();
        }
        if (method.equals("GET") && path.equals("/v1/admin/users")) {
            return this.store.adminUsers(params.getOrDefault("q", ""));
        }
        if (method.equals("POST") && path.equals("/v1/admin/users")) {
            return this.store.adminUserAction(actor, payload);
        }
        if (method.equals("GET") && path.equals("/v1/admin/reports")) {
            return this.store.adminReports("all".equals(params.get("status")));
        }
        if (method.equals("POST") && path.equals("/v1/admin/reports")) {
            return this.store.adminReportAction(actor, payload);
        }
        throw new PhoneServerStore.Failure(404);
    }

    private static void reply(Socket socket, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " Result\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: " + bytes.length + "\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n";
        OutputStream output = socket.getOutputStream();
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    @Override
    public void close() {
        try {
            this.listener.close();
        } catch (IOException e) {
        }
        for (Socket socket : this.sockets) {
            try {
                socket.close();
            } catch (IOException e2) {
            }
        }
        this.workers.shutdownNow();
        try {
            this.workers.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
        }
    }
}
