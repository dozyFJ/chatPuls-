package dev.dozyfj.pulsemessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

final class PhoneServerStore extends SQLiteOpenHelper {
    private static final SecureRandom RANDOM = new SecureRandom();

    PhoneServerStore(Context context) {
        super(context.getApplicationContext(), "pulse-host.db", (SQLiteDatabase.CursorFactory) null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (name TEXT PRIMARY KEY, salt TEXT NOT NULL, password TEXT NOT NULL)");
        db.execSQL("CREATE TABLE sessions (hash TEXT PRIMARY KEY, username TEXT NOT NULL, expires INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (sequence INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT NOT NULL, sender TEXT NOT NULL, recipient TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL, audio TEXT NOT NULL DEFAULT '', UNIQUE(sender,id))");
        db.execSQL("CREATE INDEX messages_sender ON messages(sender,sequence)");
        db.execSQL("CREATE INDEX messages_recipient ON messages(recipient,sequence)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE messages ADD COLUMN audio TEXT NOT NULL DEFAULT ''");
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
                return c.getString(0);
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
        JSONArray messages;
        if (!after.matches("[0-9]{1,15}")) {
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
        return new JSONObject().put("messages", messages);
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
            if (!c.moveToFirst()) {
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
}
