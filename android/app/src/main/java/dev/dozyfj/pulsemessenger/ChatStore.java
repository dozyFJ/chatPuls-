package dev.dozyfj.pulsemessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Локальная база клиента: чаты, сообщения, очередь отправки и курсор синхронизации. */
final class ChatStore extends SQLiteOpenHelper {
    static final int ALL = 0;
    static final int INCOMING = 1;
    static final int ARCHIVED = 2;
    static final int UNREAD = 3;

    /** Статусы сообщения в колонке {@code pending}. */
    static final int DELIVERED = 0;
    static final int QUEUED = 1;
    static final int FAILED = 2;
    static final int LOCAL_ONLY = 3;

    private static final int VERSION = 6;
    private static final String MESSAGE_COLUMNS = "id,sender,recipient,body,created,sequence,pending,audio";

    record Message(String id, String from, String to, String text, long time, long sequence, int status, String audio) {
        Message(String id, String from, String to, String text, long time, long sequence, int status) {
            this(id, from, to, text, time, sequence, status, "");
        }
    }

    private static ChatStore instance;

    /** Одна база на процесс: раньше при каждом повороте экрана открывалось новое соединение. */
    static synchronized ChatStore get(Context context) {
        if (instance == null) {
            instance = new ChatStore(context);
        }
        return instance;
    }

    private ChatStore(Context context) {
        super(context.getApplicationContext(), "pulse.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats (owner TEXT NOT NULL, peer TEXT NOT NULL, touched INTEGER NOT NULL, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(owner, peer))");
        db.execSQL("CREATE TABLE messages (owner TEXT NOT NULL, sender TEXT NOT NULL, id TEXT NOT NULL, recipient TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL, sequence INTEGER NOT NULL DEFAULT 0, pending INTEGER NOT NULL, unread INTEGER NOT NULL DEFAULT 0, audio TEXT NOT NULL DEFAULT '', PRIMARY KEY(owner, sender, id))");
        db.execSQL("CREATE TABLE cursors (owner TEXT PRIMARY KEY, sequence INTEGER NOT NULL)");
        createBlockedTable(db);
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE chats ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE messages ADD COLUMN unread INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            createBlockedTable(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE messages ADD COLUMN audio TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 6) {
            createIndexes(db);
        }
    }

    private static void createBlockedTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE blocked (owner TEXT NOT NULL, peer TEXT NOT NULL, PRIMARY KEY(owner, peer))");
    }

    private static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS messages_recipient ON messages(owner, recipient, sender)");
        db.execSQL("CREATE INDEX IF NOT EXISTS messages_pending ON messages(owner, pending)");
    }

    // ---- Чаты ----

    private static void touch(SQLiteDatabase db, String owner, String peer, long time) {
        ContentValues values = new ContentValues();
        values.put("owner", owner);
        values.put("peer", peer);
        values.put("touched", time);
        db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.execSQL("UPDATE chats SET touched = MAX(touched, ?) WHERE owner = ? AND peer = ?", new Object[]{time, owner, peer});
    }

    void createChat(String owner, String peer) {
        touch(getWritableDatabase(), owner, peer, System.currentTimeMillis());
    }

    List<String> chats(String owner) {
        return strings("SELECT peer FROM chats WHERE owner = ? ORDER BY touched DESC, peer", owner);
    }

    List<String> chats(String owner, String user, int filter) {
        String where = filter == ARCHIVED ? "archived = 1" : "archived = 0";
        boolean incoming = filter == INCOMING || filter == UNREAD;
        if (incoming) {
            where += " AND EXISTS (SELECT 1 FROM messages m WHERE m.owner = chats.owner AND m.sender = chats.peer AND m.recipient = ?"
                    + (filter == UNREAD ? " AND m.unread = 1)" : ")");
        }
        String sql = "SELECT peer FROM chats WHERE owner = ? AND " + where + " ORDER BY touched DESC, peer";
        return incoming ? strings(sql, owner, user) : strings(sql, owner);
    }

    boolean isArchived(String owner, String peer) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT archived FROM chats WHERE owner = ? AND peer = ?", new String[]{owner, peer})) {
            return c.moveToFirst() && c.getInt(0) != 0;
        }
    }

    void setArchived(String owner, String peer, boolean archived) {
        ContentValues values = new ContentValues();
        values.put("archived", archived ? 1 : 0);
        getWritableDatabase().update("chats", values, "owner = ? AND peer = ?", new String[]{owner, peer});
    }

    void deleteChat(String owner, String user, String peer) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("messages", "owner = ? AND ((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?))", new String[]{owner, user, peer, peer, user});
            db.delete("chats", "owner = ? AND peer = ?", new String[]{owner, peer});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---- Блокировка ----

    boolean isBlocked(String owner, String peer) {
        return isBlocked(getReadableDatabase(), owner, peer);
    }

    private static boolean isBlocked(SQLiteDatabase db, String owner, String peer) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM blocked WHERE owner = ? AND peer = ?", new String[]{owner, peer})) {
            return c.moveToFirst();
        }
    }

    void setBlocked(String owner, String peer, boolean blocked) {
        SQLiteDatabase db = getWritableDatabase();
        if (!blocked) {
            db.delete("blocked", "owner = ? AND peer = ?", new String[]{owner, peer});
            return;
        }
        ContentValues values = new ContentValues();
        values.put("owner", owner);
        values.put("peer", peer);
        db.insertWithOnConflict("blocked", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ---- Сообщения ----

    void queue(String owner, String user, String peer, String text, boolean localOnly) {
        queue(owner, user, peer, text, localOnly, "");
    }

    void queue(String owner, String user, String peer, String text, boolean localOnly, String audio) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            if (isBlocked(db, owner, peer)) {
                return;
            }
            touch(db, owner, peer, now);
            ContentValues v = new ContentValues();
            v.put("owner", owner);
            v.put("sender", user);
            v.put("id", UUID.randomUUID().toString());
            v.put("recipient", peer);
            v.put("body", text);
            v.put("created", now);
            v.put("sequence", 0);
            v.put("pending", localOnly ? LOCAL_ONLY : QUEUED);
            v.put("audio", audio);
            db.insertOrThrow("messages", null, v);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    List<Message> pending(String owner) {
        return messageList("SELECT " + MESSAGE_COLUMNS + " FROM messages WHERE owner = ? AND pending = " + QUEUED
                + " AND NOT EXISTS (SELECT 1 FROM blocked WHERE blocked.owner = messages.owner AND peer = recipient) ORDER BY created,id", owner);
    }

    List<Message> messages(String owner, String user, String peer) {
        return messageList("SELECT " + MESSAGE_COLUMNS + " FROM messages WHERE owner = ? AND ((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)) ORDER BY created,sequence,id",
                owner, user, peer, peer, user);
    }

    /** Последнее сообщение каждого чата для превью. Аудио не читаем: превью оно не нужно, а весит много. */
    Map<String, Message> latestMessages(String owner, String user) {
        Map<String, Message> latest = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,sender,recipient,body,created,sequence,pending,'' FROM messages WHERE owner = ? AND (sender = ? OR recipient = ?) ORDER BY created DESC,sequence DESC,id DESC",
                new String[]{owner, user, user})) {
            while (c.moveToNext()) {
                String peer = user.equals(c.getString(1)) ? c.getString(2) : c.getString(1);
                latest.putIfAbsent(peer, fromCursor(c));
            }
        }
        return latest;
    }

    /** Поиск по тексту сообщений (без голосовых данных). Регистр не важен для латиницы и кириллицы. */
    List<Message> searchMessages(String owner, String user, String query, int limit) {
        List<Message> result = new ArrayList<>();
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,sender,recipient,body,created,sequence,pending,'' FROM messages WHERE owner = ? AND (sender = ? OR recipient = ?) AND audio = '' ORDER BY created DESC",
                new String[]{owner, user, user})) {
            while (c.moveToNext() && result.size() < limit) {
                if (c.getString(3).toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    result.add(fromCursor(c));
                }
            }
        }
        return result;
    }

    void markRead(String owner, String user, String peer, long throughSequence) {
        ContentValues values = new ContentValues();
        values.put("unread", 0);
        getWritableDatabase().update("messages", values, "owner = ? AND recipient = ? AND sender = ? AND unread = 1 AND sequence <= ?",
                new String[]{owner, user, peer, Long.toString(throughSequence)});
    }

    void markFailed(String owner, Message message) {
        setStatus(owner, message, QUEUED, FAILED);
    }

    void retry(String owner, Message message) {
        setStatus(owner, message, FAILED, QUEUED);
    }

    private void setStatus(String owner, Message message, int from, int to) {
        ContentValues values = new ContentValues();
        values.put("pending", to);
        getWritableDatabase().update("messages", values, "owner = ? AND sender = ? AND id = ? AND pending = ?",
                new String[]{owner, message.from(), message.id(), Integer.toString(from)});
    }

    void removeUnsent(String owner, Message message) {
        getWritableDatabase().delete("messages", "owner = ? AND sender = ? AND id = ? AND pending IN (" + QUEUED + ", " + FAILED + ")",
                new String[]{owner, message.from(), message.id()});
    }

    // ---- Синхронизация ----

    long cursor(String owner) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT sequence FROM cursors WHERE owner = ?", new String[]{owner})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    private static void storeDelivered(SQLiteDatabase db, String owner, String user, Message message) {
        String peer = message.from().equals(user) ? message.to() : message.from();
        touch(db, owner, peer, message.time());
        boolean incoming = !message.from().equals(user) && message.to().equals(user);
        ContentValues v = new ContentValues();
        v.put("owner", owner);
        v.put("sender", message.from());
        v.put("id", message.id());
        v.put("recipient", message.to());
        v.put("body", message.text());
        v.put("created", message.time());
        v.put("sequence", message.sequence());
        v.put("pending", DELIVERED);
        v.put("unread", incoming ? 1 : 0);
        v.put("audio", message.audio());
        // Входящее, полученное повторно, не должно снова стать непрочитанным.
        db.insertWithOnConflict("messages", null, v, incoming ? SQLiteDatabase.CONFLICT_IGNORE : SQLiteDatabase.CONFLICT_REPLACE);
    }

    void delivered(String owner, String user, Message message) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            storeDelivered(db, owner, user, message);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void confirmPendingDelivery(String owner, String user, Message message) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor pending = db.rawQuery("SELECT 1 FROM messages WHERE owner = ? AND sender = ? AND id = ? AND pending = " + QUEUED,
                    new String[]{owner, user, message.id()})) {
                if (pending.moveToFirst()) {
                    storeDelivered(db, owner, user, message);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void received(String owner, String user, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long cursor = cursor(owner);
            for (Message message : messages) {
                if (message.from().equals(user) || !isBlocked(db, owner, message.from())) {
                    storeDelivered(db, owner, user, message);
                }
                cursor = Math.max(cursor, message.sequence());
            }
            ContentValues v = new ContentValues();
            v.put("owner", owner);
            v.put("sequence", cursor);
            db.insertWithOnConflict("cursors", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Курсор удалений, которые прислал сервер (сообщения, удалённые модератором). */
    long removedCursor(String owner) {
        return cursor("removed|" + owner);
    }

    void applyRemovals(String owner, List<String[]> removed, long cursor) {
        if (removed.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String[] item : removed) {
                db.delete("messages", "owner = ? AND sender = ? AND id = ?", new String[]{owner, item[0], item[1]});
            }
            ContentValues v = new ContentValues();
            v.put("owner", "removed|" + owner);
            v.put("sequence", Math.max(cursor, removedCursor(owner)));
            db.insertWithOnConflict("cursors", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---- Вспомогательное ----

    private static Message fromCursor(Cursor c) {
        return new Message(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getLong(5), c.getInt(6), c.getString(7));
    }

    private List<Message> messageList(String sql, String... args) {
        List<Message> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                result.add(fromCursor(c));
            }
        }
        return result;
    }

    private List<String> strings(String sql, String... args) {
        List<String> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                result.add(c.getString(0));
            }
        }
        return result;
    }
}
