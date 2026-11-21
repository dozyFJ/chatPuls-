package dev.dozyfj.pulsemessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

final class PhoneServerStore extends SQLiteOpenHelper {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final String HOST_PREFERENCES = "pulse-host";
    private final Context context;

    PhoneServerStore(Context context) {
        super(context.getApplicationContext(), "pulse-host.db", null, 5);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (name TEXT PRIMARY KEY, salt TEXT NOT NULL, password TEXT NOT NULL)");
        db.execSQL("CREATE TABLE sessions (hash TEXT PRIMARY KEY, username TEXT NOT NULL, expires INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (sequence INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT NOT NULL, sender TEXT NOT NULL, recipient TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL, audio TEXT NOT NULL DEFAULT '', UNIQUE(sender,id))");
        db.execSQL("CREATE INDEX messages_sender ON messages(sender,sequence)");
        db.execSQL("CREATE INDEX messages_recipient ON messages(recipient,sequence)");
        createModerationTables(db);
    }

    private static void createModerationTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE banned (name TEXT PRIMARY KEY, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE reports (id INTEGER PRIMARY KEY AUTOINCREMENT, reporter TEXT NOT NULL, target TEXT NOT NULL, message_id TEXT, message_text TEXT NOT NULL DEFAULT '', reason TEXT NOT NULL, created INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'open', resolved INTEGER)");
        db.execSQL("CREATE TABLE removed (rseq INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, id TEXT NOT NULL, recipient TEXT NOT NULL, created INTEGER NOT NULL)");
        createRolesTable(db);
        createSocialTables(db);
    }

    private static void createRolesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS roles (name TEXT PRIMARY KEY, role TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE messages ADD COLUMN audio TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 3) {
            createModerationTables(db);
        }
        if (oldVersion < 4) {
            createRolesTable(db);
        }
        if (oldVersion < 5) {
            createSocialTables(db);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 0xFF)));
        }
        return out.toString();
    }

    private static String random(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return hex(bytes);
    }

    static String hash(String text) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String derive(String password, String salt) throws Exception {
        PBEKeySpec key = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 120000, 256);
        try {
            return hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded());
        } finally {
            key.clearPassword();
        }
    }

    static boolean validName(String value) {
        return (!value.matches("[a-z0-9_]{3,32}") || value.equals("__proto__") || value.equals("constructor") || value.equals("prototype")) ? false : true;
    }

    static final class Failure extends Exception {
        final int status;

        Failure(int status) {
            this.status = status;
        }
    }

    synchronized JSONObject authenticate(JSONObject input, boolean register) throws Exception {
        Object rawName = input.opt("username");
        Object rawPassword = input.opt("password");
        if (!(rawName instanceof String) || !(rawPassword instanceof String)) {
            throw new Failure(400);
        }
        String name = ((String) rawName).trim().toLowerCase(Locale.ROOT);
        String password = (String) rawPassword;
        if (!validName(name) || password.length() < 10 || password.length() > 128) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        String salt = null;
        String expected = null;
        try (Cursor c = db.rawQuery("SELECT salt,password FROM users WHERE name = ?", new String[]{name})) {
            if (c.moveToFirst()) {
                salt = c.getString(0);
                expected = c.getString(1);
            }
        }
        if (!register && isBanned(db, name)) {
            throw new Failure(403);
        }
        ContentValues newUser = null;
        if (register) {
            if (salt != null) {
                throw new Failure(409);
            }
            String newSalt = random(16);
            newUser = new ContentValues();
            newUser.put("name", name);
            newUser.put("salt", newSalt);
            newUser.put("password", derive(password, newSalt));
        } else {
            String actual = derive(password, salt == null ? "00000000000000000000000000000000" : salt);
            if (expected == null || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII))) {
                throw new Failure(401);
            }
        }
        String token = random(32);
        long now = System.currentTimeMillis();
        ContentValues session = new ContentValues();
        session.put("hash", hash(token));
        session.put("username", name);
        session.put("expires", Long.valueOf(now + 2592000000L));
        JSONObject result = new JSONObject().put("username", name).put("token", token);
        db.beginTransaction();
        try {
            if (newUser != null) {
                db.insertOrThrow("users", null, newUser);
            }
            db.delete("sessions", "expires <= ?", new String[]{Long.toString(now)});
            db.insertOrThrow("sessions", null, session);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    synchronized String user(String authorization) throws Exception {
        if (authorization == null || !authorization.matches("Bearer [a-f0-9]{64}")) {
            throw new Failure(401);
        }
        try (Cursor c = getReadableDatabase().rawQuery("SELECT username FROM sessions WHERE hash = ? AND expires > ?", new String[]{hash(authorization.substring(7)), Long.toString(System.currentTimeMillis())})) {
            if (c.moveToFirst()) {
                String name = c.getString(0);
                if (isBanned(getReadableDatabase(), name)) {
                    throw new Failure(403);
                }
                return name;
            }
        }
        throw new Failure(401);
    }

    synchronized boolean userExists(String name) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT 1 FROM users WHERE name = ?", new String[]{name})) {
            return cursor.moveToFirst();
        }
    }

    private JSONObject message(Cursor c) throws Exception {
        JSONObject item = new JSONObject().put("sequence", c.getLong(0)).put("id", c.getString(1)).put("from", c.getString(2)).put("to", c.getString(3)).put("text", c.getString(4)).put("time", c.getLong(5));
        if (!c.getString(6).isEmpty()) {
            item.put("audio", c.getString(6));
        }
        return item;
    }

    synchronized JSONObject fetch(String user, String after) throws Exception {
        return fetch(user, after, "0");
    }

    synchronized JSONObject fetch(String user, String after, String removedAfter) throws Exception {
        JSONArray messages;
        if (!after.matches("[0-9]{1,15}") || !removedAfter.matches("[0-9]{1,15}")) {
            throw new Failure(400);
        }
        messages = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT sequence,id,sender,recipient,body,created,audio FROM messages WHERE sequence > ? AND (sender = ? OR recipient = ?) ORDER BY sequence LIMIT 100", new String[]{after, user, user})) {
            int size = 0;
            while (c.moveToNext()) {
                JSONObject item = message(c);
                size += item.toString().getBytes(StandardCharsets.UTF_8).length + 1;
                if (size > 380000) {
                    break;
                }
                messages.put(item);
            }
        }
        JSONArray removed = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT rseq,sender,id FROM removed WHERE rseq > ? AND (sender = ? OR recipient = ?) ORDER BY rseq LIMIT 200", new String[]{removedAfter, user, user})) {
            while (c.moveToNext()) {
                removed.put(new JSONObject().put("rseq", c.getLong(0)).put("from", c.getString(1)).put("id", c.getString(2)));
            }
        }
        return new JSONObject().put("messages", messages).put("removed", removed);
    }

    synchronized JSONObject send(String user, JSONObject input, boolean voice) throws Exception {
        String audio = voice ? VoiceNote.readAudio(input) : "";
        if (voice && audio.isEmpty()) {
            throw new Failure(400);
        }
        Object rawTo = input.opt("to");
        Object rawId = input.opt("id");
        Object rawText = input.opt("text");
        if (!(rawTo instanceof String) || !(rawId instanceof String) || !(rawText instanceof String)) {
            throw new Failure(400);
        }
        String to = ((String) rawTo).trim().toLowerCase(Locale.ROOT);
        String id = (String) rawId;
        String text = (String) rawText;
        if (!validName(to) || to.equals(user) || !id.matches("[a-f0-9-]{36}") || text.trim().isEmpty() || text.length() > 2000) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT name FROM users WHERE name = ?", new String[]{to})) {
            if (!c.moveToFirst() || isBanned(db, to)) {
                throw new Failure(404);
            }
        }
        try (Cursor c = db.rawQuery("SELECT sequence,id,sender,recipient,body,created,audio FROM messages WHERE sender = ? AND id = ?", new String[]{user, id})) {
            if (c.moveToFirst()) {
                JSONObject existing = message(c);
                if (!to.equals(existing.getString("to")) || !text.equals(existing.getString("text")) || !audio.equals(existing.optString("audio", ""))) {
                    throw new Failure(409);
                }
                return existing;
            }
        }
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("sender", user);
        values.put("recipient", to);
        values.put("body", text);
        values.put("created", Long.valueOf(System.currentTimeMillis()));
        values.put("audio", audio);
        long sequence = db.insertOrThrow("messages", null, values);
        JSONObject item = new JSONObject().put("sequence", sequence).put("id", id).put("from", user).put("to", to).put("text", text).put("time", values.getAsLong("created"));
        if (voice) {
            item.put("audio", audio);
        }
        return item;
    }

    // ---- Модерация ----

    private static boolean isBanned(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM banned WHERE name = ?", new String[]{name})) {
            return c.moveToFirst();
        }
    }

    /** Ключ для приложения chatPuls Admin. Хранится в закрытых настройках приложения, создаётся при первом запросе. */
    static synchronized String adminToken(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(HOST_PREFERENCES, Context.MODE_PRIVATE);
        String token = preferences.getString("adminToken", "");
        if (!token.matches("[a-f0-9]{64}")) {
            token = random(32);
            preferences.edit().putString("adminToken", token).apply();
        }
        return token;
    }

    static synchronized String rotateAdminToken(Context context) {
        String token = random(32);
        context.getSharedPreferences(HOST_PREFERENCES, Context.MODE_PRIVATE).edit().putString("adminToken", token).apply();
        return token;
    }

    /** Кто действует в админке: владелец сервера по ключу или пользователь с ролью. */
    record Actor(String name, String role) {
        boolean owner() {
            return this.name == null;
        }
    }

    private static int rank(String role) {
        switch (role) {
            case "owner":
                return 3;
            case "superadmin":
                return 2;
            case "admin":
                return 1;
            default:
                return 0;
        }
    }

    private static String roleOf(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT role FROM roles WHERE name = ?", new String[]{name})) {
            return c.moveToFirst() ? c.getString(0) : "user";
        }
    }

    /** Над кем можно действовать: только над теми, кто ниже по роли. Владелец может всё. */
    private static boolean canTouch(SQLiteDatabase db, Actor actor, String target) {
        return actor.owner() || (rank(actor.role()) > rank(roleOf(db, target)) && !target.equals(actor.name()));
    }

    synchronized Actor adminActor(String keyHeader, String authorization) throws Exception {
        if (keyHeader != null && keyHeader.matches("[a-f0-9]{64}")) {
            byte[] expected = hash(adminToken(this.context)).getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(expected, hash(keyHeader).getBytes(StandardCharsets.US_ASCII))) {
                return new Actor(null, "owner");
            }
            throw new Failure(401);
        }
        String name;
        try {
            name = user(authorization);
        } catch (Failure failure) {
            throw new Failure(401);
        }
        String role = roleOf(getReadableDatabase(), name);
        if (rank(role) < 1) {
            throw new Failure(401);
        }
        return new Actor(name, role);
    }

    JSONObject adminMe(Actor actor) throws Exception {
        return new JSONObject().put("name", actor.owner() ? JSONObject.NULL : actor.name()).put("role", actor.role());
    }

    synchronized JSONObject adminRoles(Actor actor, JSONObject input) throws Exception {
        String name = input.optString("name", "").trim().toLowerCase(Locale.ROOT);
        String role = input.optString("role", "");
        if (!userExists(name) || !(role.equals("user") || role.equals("admin") || role.equals("superadmin"))) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        // Главный админ назначает и снимает обычных админов; главных назначает только владелец сервера.
        boolean allowed = actor.owner() || (actor.role().equals("superadmin") && !role.equals("superadmin")
                && !roleOf(db, name).equals("superadmin") && !name.equals(actor.name()));
        if (!allowed) {
            throw new Failure(403);
        }
        db.beginTransaction();
        try {
            if (role.equals("user")) {
                db.delete("roles", "name = ?", new String[]{name});
            } else {
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("role", role);
                db.insertWithOnConflict("roles", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                db.delete("banned", "name = ?", new String[]{name});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new JSONObject().put("ok", true);
    }

    synchronized JSONObject report(String user, JSONObject input) throws Exception {
        String target = input.optString("to", "").trim().toLowerCase(Locale.ROOT);
        Object rawReason = input.opt("reason");
        String reason = rawReason instanceof String ? ((String) rawReason).trim() : "";
        Object rawId = input.opt("id");
        String messageId = rawId instanceof String ? (String) rawId : null;
        if (rawId != null && rawId != JSONObject.NULL && messageId == null) {
            throw new Failure(400);
        }
        if (!userExists(target) || target.equals(user) || reason.isEmpty() || reason.length() > 300 || (messageId != null && !messageId.matches("[a-f0-9-]{36}"))) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM reports WHERE reporter = ? AND status = 'open'", new String[]{user})) {
            if (c.moveToFirst() && c.getInt(0) >= 50) {
                throw new Failure(429);
            }
        }
        String text = "";
        if (messageId != null) {
            try (Cursor c = db.rawQuery("SELECT body FROM messages WHERE sender = ? AND recipient = ? AND id = ?", new String[]{target, user, messageId})) {
                if (!c.moveToFirst()) {
                    throw new Failure(404);
                }
                text = c.getString(0);
            }
        }
        ContentValues values = new ContentValues();
        values.put("reporter", user);
        values.put("target", target);
        values.put("message_id", messageId);
        values.put("message_text", text);
        values.put("reason", reason);
        values.put("created", System.currentTimeMillis());
        db.insertOrThrow("reports", null, values);
        return new JSONObject().put("ok", true);
    }

    private static long count(SQLiteDatabase db, String sql, String... args) {
        try (Cursor c = db.rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    synchronized JSONObject adminStats() throws Exception {
        SQLiteDatabase db = getReadableDatabase();
        long now = System.currentTimeMillis();
        String since = Long.toString(now - DAY);
        return new JSONObject()
                .put("users", count(db, "SELECT COUNT(*) FROM users"))
                .put("banned", count(db, "SELECT COUNT(*) FROM banned"))
                .put("sessions", count(db, "SELECT COUNT(*) FROM sessions WHERE expires > ?", Long.toString(now)))
                .put("messages", count(db, "SELECT COUNT(*) FROM messages"))
                .put("messagesToday", count(db, "SELECT COUNT(*) FROM messages WHERE created > ?", since))
                .put("activeToday", count(db, "SELECT COUNT(DISTINCT sender) FROM messages WHERE created > ?", since))
                .put("openReports", count(db, "SELECT COUNT(*) FROM reports WHERE status = 'open'"));
    }

    synchronized JSONObject adminUsers(String query) throws Exception {
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT).replace("\\", "").replace("%", "").replace("_", "\\_") + "%";
        JSONArray users = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT u.name, EXISTS(SELECT 1 FROM banned b WHERE b.name = u.name), COALESCE((SELECT role FROM roles r WHERE r.name = u.name), 'user'), (SELECT COUNT(*) FROM messages m WHERE m.sender = u.name), COALESCE((SELECT MAX(created) FROM messages m WHERE m.sender = u.name), 0) AS last FROM users u WHERE u.name LIKE ? ESCAPE '\\' ORDER BY last DESC, u.name LIMIT 200", new String[]{pattern})) {
            while (c.moveToNext()) {
                users.put(new JSONObject().put("name", c.getString(0)).put("banned", c.getInt(1) != 0).put("role", c.getString(2)).put("messages", c.getLong(3)).put("lastActive", c.getLong(4)));
            }
        }
        return new JSONObject().put("users", users);
    }

    synchronized JSONObject adminUserAction(Actor actor, JSONObject input) throws Exception {
        String name = input.optString("name", "").trim().toLowerCase(Locale.ROOT);
        String action = input.optString("action", "");
        if (!userExists(name) || !(action.equals("ban") || action.equals("unban") || action.equals("logout"))) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        if (!canTouch(db, actor, name)) {
            throw new Failure(403);
        }
        if (action.equals("unban")) {
            db.delete("banned", "name = ?", new String[]{name});
        } else {
            db.beginTransaction();
            try {
                if (action.equals("ban")) {
                    ban(db, name);
                } else {
                    db.delete("sessions", "username = ?", new String[]{name});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return new JSONObject().put("ok", true);
    }

    private static void ban(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("created", System.currentTimeMillis());
        db.insertWithOnConflict("banned", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.delete("sessions", "username = ?", new String[]{name});
    }

    synchronized JSONObject adminReports(boolean all) throws Exception {
        JSONArray reports = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,reporter,target,message_id,message_text,reason,created,status FROM reports" + (all ? "" : " WHERE status = 'open'") + " ORDER BY id DESC LIMIT 200", null)) {
            while (c.moveToNext()) {
                reports.put(new JSONObject().put("id", c.getLong(0)).put("reporter", c.getString(1)).put("target", c.getString(2))
                        .put("messageId", c.isNull(3) ? JSONObject.NULL : c.getString(3)).put("text", c.getString(4)).put("reason", c.getString(5))
                        .put("time", c.getLong(6)).put("status", c.getString(7)));
            }
        }
        return new JSONObject().put("reports", reports);
    }

    synchronized JSONObject adminReportAction(Actor actor, JSONObject input) throws Exception {
        long id = input.optLong("id", -1);
        String action = input.optString("action", "");
        if (!(action.equals("dismiss") || action.equals("delete") || action.equals("ban") || action.equals("delete_ban"))) {
            throw new Failure(400);
        }
        SQLiteDatabase db = getWritableDatabase();
        String target;
        String messageId;
        try (Cursor c = db.rawQuery("SELECT target, message_id FROM reports WHERE id = ?", new String[]{Long.toString(id)})) {
            if (!c.moveToFirst()) {
                throw new Failure(404);
            }
            target = c.getString(0);
            messageId = c.isNull(1) ? null : c.getString(1);
        }
        if (!action.equals("dismiss") && !canTouch(db, actor, target)) {
            throw new Failure(403);
        }
        db.beginTransaction();
        try {
            if ((action.equals("delete") || action.equals("delete_ban")) && messageId != null) {
                String recipient = null;
                try (Cursor c = db.rawQuery("SELECT recipient FROM messages WHERE sender = ? AND id = ?", new String[]{target, messageId})) {
                    if (c.moveToFirst()) {
                        recipient = c.getString(0);
                    }
                }
                if (recipient != null) {
                    db.delete("messages", "sender = ? AND id = ?", new String[]{target, messageId});
                    ContentValues removed = new ContentValues();
                    removed.put("sender", target);
                    removed.put("id", messageId);
                    removed.put("recipient", recipient);
                    removed.put("created", System.currentTimeMillis());
                    db.insertOrThrow("removed", null, removed);
                }
            }
            if (action.equals("ban") || action.equals("delete_ban")) {
                ban(db, target);
            }
            ContentValues status = new ContentValues();
            status.put("status", action.equals("dismiss") ? "dismissed" : "resolved");
            status.put("resolved", System.currentTimeMillis());
            db.update("reports", status, "id = ?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new JSONObject().put("ok", true);
    }

    // ---- «Пульс дружбы»: подписки, друзья, близкие, профили, стена, истории ----

    private static final long PULSE_WINDOW = 14L * 24 * 60 * 60 * 1000;
    private static final long STORY_LIFETIME = 24L * 60 * 60 * 1000;

    private static void createSocialTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS follows (follower TEXT NOT NULL, target TEXT NOT NULL, created INTEGER NOT NULL, PRIMARY KEY(follower, target))");
        db.execSQL("CREATE INDEX IF NOT EXISTS follows_target ON follows(target)");
        db.execSQL("CREATE TABLE IF NOT EXISTS profiles (name TEXT PRIMARY KEY, display_name TEXT NOT NULL DEFAULT '', avatar TEXT NOT NULL DEFAULT '', cover TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS posts (id INTEGER PRIMARY KEY AUTOINCREMENT, author TEXT NOT NULL, text TEXT NOT NULL, audience TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS posts_author ON posts(author, id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS stories (author TEXT PRIMARY KEY, image TEXT NOT NULL, audience TEXT NOT NULL, created INTEGER NOT NULL)");
    }

    private static boolean isAudience(String value) {
        return value.equals("all") || value.equals("friends") || value.equals("close");
    }

    private static boolean isBase64(String value) {
        return value.matches("[A-Za-z0-9+/]*={0,2}");
    }

    private static boolean follows(SQLiteDatabase db, String a, String b) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM follows WHERE follower = ? AND target = ?", new String[]{a, b})) {
            return c.moveToFirst();
        }
    }

    /** Пульс пары 0–100: растёт, когда переписка идёт в обе стороны за последние 14 дней. */
    private static int pulse(SQLiteDatabase db, String a, String b) {
        String since = Long.toString(System.currentTimeMillis() - PULSE_WINDOW);
        long ab = count(db, "SELECT COUNT(*) FROM messages WHERE sender = ? AND recipient = ? AND created >= ?", a, b, since);
        long ba = count(db, "SELECT COUNT(*) FROM messages WHERE sender = ? AND recipient = ? AND created >= ?", b, a, since);
        return (int) Math.min(100, Math.round(25 * (Math.log(1 + Math.min(ab, ba)) / Math.log(2))));
    }

    /** Отношение viewer → target: self | none | following | follower | friend | close. */
    private static JSONObject relation(SQLiteDatabase db, String viewer, String target) throws Exception {
        if (viewer.equals(target)) {
            return new JSONObject().put("level", "self").put("pulse", 100).put("following", false).put("followsMe", false);
        }
        boolean following = follows(db, viewer, target);
        boolean followsMe = follows(db, target, viewer);
        int score = following && followsMe ? pulse(db, viewer, target) : 0;
        String level = following && followsMe ? (score >= 60 ? "close" : "friend") : following ? "following" : followsMe ? "follower" : "none";
        return new JSONObject().put("level", level).put("pulse", score).put("following", following).put("followsMe", followsMe);
    }

    private static boolean canSee(SQLiteDatabase db, String viewer, String author, String audience) throws Exception {
        if (viewer.equals(author)) {
            return true;
        }
        String level = relation(db, viewer, author).getString("level");
        if (audience.equals("close")) {
            return level.equals("close");
        }
        if (audience.equals("friends")) {
            return level.equals("friend") || level.equals("close");
        }
        return !level.equals("none") && !level.equals("follower");
    }

    private static JSONObject person(SQLiteDatabase db, String viewer, String name) throws Exception {
        JSONObject result = relation(db, viewer, name);
        result.put("name", name);
        try (Cursor c = db.rawQuery("SELECT display_name, avatar != '' FROM profiles WHERE name = ?", new String[]{name})) {
            boolean found = c.moveToFirst();
            result.put("displayName", found ? c.getString(0) : "").put("hasAvatar", found && c.getInt(1) != 0);
        }
        return result;
    }

    private static void putCounts(SQLiteDatabase db, JSONObject target, String name) throws Exception {
        target.put("followers", count(db, "SELECT COUNT(*) FROM follows WHERE target = ?", name))
                .put("following", count(db, "SELECT COUNT(*) FROM follows WHERE follower = ?", name))
                .put("friends", count(db, "SELECT COUNT(*) FROM follows f WHERE f.target = ? AND EXISTS (SELECT 1 FROM follows g WHERE g.follower = ? AND g.target = f.follower)", name, name));
    }

    private static void sortByPulse(List<JSONObject> people) {
        people.sort((a, b) -> {
            int byPulse = Integer.compare(b.optInt("pulse"), a.optInt("pulse"));
            return byPulse != 0 ? byPulse : a.optString("name").compareTo(b.optString("name"));
        });
    }

    private JSONObject storyRow(SQLiteDatabase db, String author) {
        try (Cursor c = db.rawQuery("SELECT image, audience, created FROM stories WHERE author = ? AND created > ?", new String[]{author, Long.toString(System.currentTimeMillis() - STORY_LIFETIME)})) {
            if (!c.moveToFirst()) {
                return null;
            }
            return new JSONObject().put("image", c.getString(0)).put("audience", c.getString(1)).put("time", c.getLong(2));
        } catch (Exception e) {
            return null;
        }
    }

    /** Обрабатывает запросы соцчасти; null — путь не относится к ней. */
    synchronized JSONObject social(String user, String method, String path, java.util.Map<String, String> params, JSONObject input) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        if (method.equals("GET") && path.equals("/v1/people")) {
            String query = params.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
            List<JSONObject> people = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT name FROM users WHERE name != ? AND NOT EXISTS (SELECT 1 FROM banned b WHERE b.name = users.name) ORDER BY name LIMIT 500", new String[]{user})) {
                while (c.moveToNext()) {
                    if (c.getString(0).contains(query)) {
                        people.add(person(db, user, c.getString(0)));
                    }
                }
            }
            sortByPulse(people);
            return new JSONObject().put("people", new JSONArray(people.subList(0, Math.min(50, people.size()))));
        }
        if (method.equals("GET") && path.equals("/v1/friends")) {
            List<JSONObject> people = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT target FROM follows WHERE follower = ? UNION SELECT follower FROM follows WHERE target = ?", new String[]{user, user})) {
                while (c.moveToNext()) {
                    people.add(person(db, user, c.getString(0)));
                }
            }
            sortByPulse(people);
            JSONObject result = new JSONObject().put("people", new JSONArray(people));
            putCounts(db, result, user);
            return result;
        }
        if (method.equals("POST") && path.equals("/v1/follow")) {
            String target = input.optString("name", "").trim().toLowerCase(Locale.ROOT);
            Object follow = input.opt("follow");
            if (!userExists(target) || target.equals(user) || !(follow instanceof Boolean)) {
                throw new Failure(400);
            }
            if ((Boolean) follow) {
                ContentValues values = new ContentValues();
                values.put("follower", user);
                values.put("target", target);
                values.put("created", System.currentTimeMillis());
                db.insertWithOnConflict("follows", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                db.delete("follows", "follower = ? AND target = ?", new String[]{user, target});
            }
            return person(db, user, target);
        }
        if (method.equals("GET") && path.equals("/v1/profile")) {
            String name = params.getOrDefault("name", user).trim().toLowerCase(Locale.ROOT);
            if (!userExists(name) || isBanned(db, name)) {
                throw new Failure(404);
            }
            JSONObject result = person(db, user, name);
            try (Cursor c = db.rawQuery("SELECT avatar, cover FROM profiles WHERE name = ?", new String[]{name})) {
                boolean found = c.moveToFirst();
                result.put("avatar", found ? c.getString(0) : "").put("cover", found ? c.getString(1) : "");
            }
            putCounts(db, result, name);
            JSONObject story = storyRow(db, name);
            result.put("story", story != null && canSee(db, user, name, story.getString("audience")));
            return result;
        }
        if (method.equals("POST") && path.equals("/v1/profile")) {
            ContentValues values = new ContentValues();
            if (input.has("displayName")) {
                String displayName = input.optString("displayName", "").trim();
                if (displayName.length() > 60) {
                    throw new Failure(400);
                }
                values.put("display_name", displayName);
            }
            String[][] images = {{"avatar", "120000"}, {"cover", "400000"}};
            for (String[] image : images) {
                if (!input.has(image[0])) {
                    continue;
                }
                Object value = input.opt(image[0]);
                if (!(value instanceof String) || ((String) value).length() > Integer.parseInt(image[1]) || !isBase64((String) value)) {
                    throw new Failure(400);
                }
                values.put(image[0], (String) value);
            }
            ContentValues base = new ContentValues();
            base.put("name", user);
            db.insertWithOnConflict("profiles", null, base, SQLiteDatabase.CONFLICT_IGNORE);
            if (values.size() > 0) {
                db.update("profiles", values, "name = ?", new String[]{user});
            }
            return new JSONObject().put("ok", true);
        }
        if (method.equals("POST") && path.equals("/v1/posts")) {
            String text = input.optString("text", "").trim();
            String audience = input.optString("audience", "all");
            if (text.isEmpty() || text.length() > 2000 || !isAudience(audience)) {
                throw new Failure(400);
            }
            ContentValues values = new ContentValues();
            values.put("author", user);
            values.put("text", text);
            values.put("audience", audience);
            values.put("created", System.currentTimeMillis());
            long id = db.insertOrThrow("posts", null, values);
            return new JSONObject().put("id", id).put("author", user).put("text", text).put("audience", audience).put("time", values.getAsLong("created"));
        }
        if (method.equals("POST") && path.equals("/v1/posts/delete")) {
            if (db.delete("posts", "id = ? AND author = ?", new String[]{Long.toString(input.optLong("id", -1)), user}) == 0) {
                throw new Failure(404);
            }
            return new JSONObject().put("ok", true);
        }
        if (method.equals("GET") && (path.equals("/v1/posts") || path.equals("/v1/feed"))) {
            boolean feed = path.equals("/v1/feed");
            String name = params.getOrDefault("name", user).trim().toLowerCase(Locale.ROOT);
            String sql = feed
                    ? "SELECT id, author, text, audience, created FROM posts p WHERE (author = ? OR EXISTS (SELECT 1 FROM follows f WHERE f.follower = ? AND f.target = p.author)) AND NOT EXISTS (SELECT 1 FROM banned b WHERE b.name = p.author) ORDER BY id DESC LIMIT 300"
                    : "SELECT id, author, text, audience, created FROM posts WHERE author = ? AND ? != '' ORDER BY id DESC LIMIT 300";
            JSONArray posts = new JSONArray();
            try (Cursor c = db.rawQuery(sql, new String[]{feed ? user : name, user})) {
                while (c.moveToNext() && posts.length() < 100) {
                    if (canSee(db, user, c.getString(1), c.getString(3))) {
                        posts.put(new JSONObject().put("id", c.getLong(0)).put("author", c.getString(1)).put("text", c.getString(2))
                                .put("audience", c.getString(3)).put("time", c.getLong(4)).put("level", relation(db, user, c.getString(1)).getString("level")));
                    }
                }
            }
            return new JSONObject().put("posts", posts);
        }
        if (method.equals("POST") && path.equals("/v1/stories")) {
            Object image = input.opt("image");
            String audience = input.optString("audience", "all");
            if (!(image instanceof String) || ((String) image).isEmpty() || ((String) image).length() > 500000 || !isBase64((String) image) || !isAudience(audience)) {
                throw new Failure(400);
            }
            ContentValues values = new ContentValues();
            values.put("author", user);
            values.put("image", (String) image);
            values.put("audience", audience);
            values.put("created", System.currentTimeMillis());
            db.insertWithOnConflict("stories", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return new JSONObject().put("ok", true);
        }
        if (method.equals("POST") && path.equals("/v1/stories/delete")) {
            db.delete("stories", "author = ?", new String[]{user});
            return new JSONObject().put("ok", true);
        }
        if (method.equals("GET") && path.equals("/v1/stories")) {
            List<JSONObject> stories = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT author, audience, created FROM stories s WHERE created > ? AND (author = ? OR EXISTS (SELECT 1 FROM follows f WHERE f.follower = ? AND f.target = s.author)) AND NOT EXISTS (SELECT 1 FROM banned b WHERE b.name = s.author)",
                    new String[]{Long.toString(System.currentTimeMillis() - STORY_LIFETIME), user, user})) {
                while (c.moveToNext()) {
                    if (canSee(db, user, c.getString(0), c.getString(1))) {
                        stories.add(person(db, user, c.getString(0)).put("time", c.getLong(2)).put("audience", c.getString(1)));
                    }
                }
            }
            stories.sort((a, b) -> {
                int self = Boolean.compare(b.optString("name").equals(user), a.optString("name").equals(user));
                if (self != 0) {
                    return self;
                }
                int byPulse = Integer.compare(b.optInt("pulse"), a.optInt("pulse"));
                return byPulse != 0 ? byPulse : Long.compare(b.optLong("time"), a.optLong("time"));
            });
            return new JSONObject().put("stories", new JSONArray(stories));
        }
        if (method.equals("GET") && path.equals("/v1/story")) {
            String name = params.getOrDefault("name", "").trim().toLowerCase(Locale.ROOT);
            JSONObject story = storyRow(db, name);
            if (story == null || !canSee(db, user, name, story.getString("audience"))) {
                throw new Failure(404);
            }
            return story.put("name", name);
        }
        return null;
    }
}
