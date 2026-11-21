package dev.dozyfj.pulsemessenger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import dev.dozyfj.pulsemessenger.UpdateManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

final class UpdateClient implements UpdateManager.Source {
    private static final int APK_LIMIT = 100 * 1024 * 1024;
    private static final int MANIFEST_LIMIT = 16384;
    private static final String RAW_REPO = "https://raw.githubusercontent.com/dozyFJ/chatPuls-/";
    private static final String UPDATES_REF = "https://api.github.com/repos/dozyFJ/chatPuls-/git/ref/heads/updates";
    private final Context context;
    private final PackageManager packages;
    static final String FEED = "https://raw.githubusercontent.com/dozyFJ/chatPuls-/updates/";
    private static final Pattern APK_URL = Pattern.compile(Pattern.quote(FEED) + "releases/PulseMessenger-[0-9]+(?:\\.[0-9]+)*\\.apk");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    record Update(long code, String name, String url, String hash) {}

    static final class FeedMissing extends IOException {
        FeedMissing() {
        }
    }

    UpdateClient(Context context) {
        this.context = context.getApplicationContext();
        this.packages = context.getPackageManager();
    }

    private PackageInfo installed() throws PackageManager.NameNotFoundException {
        return this.packages.getPackageInfo(this.context.getPackageName(), signingFlag());
    }

    static Update parseManifest(String data, long currentCode) throws Exception {
        JSONObject json = new JSONObject(data);
        long code = json.getLong("versionCode");
        String name = json.getString("versionName");
        String url = json.getString("url");
        String hash = json.getString("sha256").toLowerCase(Locale.ROOT);
        if (code <= 0 || code > 2147483647L || name.trim().isEmpty() || name.length() > 80 || !APK_URL.matcher(url).matches() || !HASH.matcher(hash).matches()) {
            throw new IOException("Invalid update manifest");
        }
        if (code > currentCode) {
            return new Update(code, name, url, hash);
        }
        return null;
    }

    @Override
    public Update check() throws Exception {
        // Запасной адрес с меткой времени: так CDN GitHub не отдаёт устаревший манифест до 5 минут.
        String manifestUrl = "https://raw.githubusercontent.com/dozyFJ/chatPuls-/refs/heads/updates/latest.json?t=" + System.currentTimeMillis();
        try {
            manifestUrl = manifestUrlForRef(readUpdateJson(UPDATES_REF));
        } catch (IOException e) {
        } catch (JSONException e2) {
        }
        return parseManifest(readUpdateJson(manifestUrl), versionCode(installed()));
    }

    static String manifestUrlForRef(String response) throws IOException, JSONException {
        String commit = new JSONObject(response).getJSONObject("object").getString("sha");
        if (!commit.matches("[a-f0-9]{40}")) {
            throw new IOException("Invalid update commit");
        }
        return RAW_REPO + commit + "/latest.json";
    }

    private static String readUpdateJson(String url) throws IOException {
        HttpsURLConnection connection = connect(url, true);
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            transfer(input, buffer, null, 16384);
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public File download(Update update) throws Exception {
        File dir = new File(this.context.getCacheDir(), "updates");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create update cache");
        }
        File destination = new File(dir, "PulseMessenger-" + update.code() + ".apk");
        if (destination.isFile()) {
            try {
                verifyReady(destination, update);
                return destination;
            } catch (Exception e) {
            }
        }
        File partial = File.createTempFile("download-", ".apk", dir);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            HttpsURLConnection connection = connect(update.url(), false);
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(partial)) {
                transfer(input, output, digest, APK_LIMIT);
            } finally {
                connection.disconnect();
            }
            if (!update.hash().equals(hex(digest.digest()))) {
                throw new IOException("Update checksum mismatch");
            }
            verifyPackage(partial, update);
            if (!partial.renameTo(destination)) {
                throw new IOException("Cannot save update");
            }
            return destination;
        } finally {
            partial.delete();
        }
    }

    @Override
    public void verifyReady(File file, Update update) throws Exception {
        File dir = new File(this.context.getCacheDir(), "updates").getCanonicalFile();
        if (!dir.equals(file.getCanonicalFile().getParentFile()) || !file.isFile()) {
            throw new IOException("Invalid update file");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream input = new FileInputStream(file);
        try {
            transfer(input, new OutputStream() {
                @Override
                public void write(int b) {
                }

                @Override
                public void write(byte[] data, int offset, int length) {
                }
            }, digest, APK_LIMIT);
            input.close();
            if (!update.hash().equals(hex(digest.digest()))) {
                throw new IOException("Update checksum mismatch");
            }
            verifyPackage(file, update);
        } catch (Throwable th) {
            try {
                input.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private void verifyPackage(File file, Update update) throws Exception {
        PackageInfo archive = this.packages.getPackageArchiveInfo(file.getPath(), signingFlag());
        if (!validPackage(archive, installed(), this.context.getPackageName(), update.code())) {
            throw new IOException("Update package, version or signature mismatch");
        }
    }

    static boolean validPackage(PackageInfo archive, PackageInfo installed, String packageName, long expectedCode) {
        if (archive == null || installed == null || !packageName.equals(archive.packageName) || versionCode(archive) != expectedCode || expectedCode <= versionCode(installed)) {
            return false;
        }
        Signature[] left = signers(archive);
        Signature[] right = signers(installed);
        return left != null && right != null && left.length > 0 && new HashSet(Arrays.asList(left)).equals(new HashSet(Arrays.asList(right)));
    }

    private static void transfer(InputStream input, OutputStream output, MessageDigest digest, int limit) throws IOException {
        byte[] data = new byte[16384];
        int total = 0;
        while (true) {
            int n = input.read(data);
            if (n != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Interrupted");
                }
                total += n;
                if (total > limit) {
                    throw new IOException("Update too large");
                }
                if (digest != null) {
                    digest.update(data, 0, n);
                }
                output.write(data, 0, n);
            } else {
                return;
            }
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 0xFF)));
        }
        return out.toString();
    }

    private static HttpsURLConnection connect(String url, boolean manifest) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        if (manifest) {
            connection.setRequestProperty("Cache-Control", "no-cache, max-age=0");
        }
        try {
            int status = connection.getResponseCode();
            if (manifest && status == 404) {
                throw new FeedMissing();
            }
            if (status != 200) {
                throw new IOException("Update server returned HTTP " + status);
            }
            return connection;
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    private static int signingFlag() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
    }

    private static long versionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private static Signature[] signers(PackageInfo info) {
        if (Build.VERSION.SDK_INT < 28) {
            return info.signatures;
        }
        if (info.signingInfo == null) {
            return null;
        }
        return info.signingInfo.getApkContentsSigners();
    }
}
