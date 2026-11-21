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
import java.util.Objects;
import java.util.UUID;

final class ChatStore extends SQLiteOpenHelper {
    static final int ALL = 0;
    static final int ARCHIVED = 2;
    static final int INCOMING = 1;
    static final int UNREAD = 3;

    record Message(String id, String from, String to, String text, long time, long sequence, int status, String audio) {

        Message(String id, String from, String to, String text, long time, long sequence, int status) {
            this(id, from, to, text, time, sequence, status, "");
        }

    }

    ChatStore(Context context) {
        super(context.getApplicationContext(), "pulse.db", (SQLiteDatabase.CursorFactory) null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats (owner TEXT NOT NULL, peer TEXT NOT NULL, touched INTEGER NOT NULL, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(owner, peer))");
        db.execSQL("CREATE TABLE messages (owner TEXT NOT NULL, sender TEXT NOT NULL, id TEXT NOT NULL, recipient TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL, sequence INTEGER NOT NULL DEFAULT 0, pending INTEGER NOT NULL, unread INTEGER NOT NULL DEFAULT 0, audio TEXT NOT NULL DEFAULT '', PRIMARY KEY(owner, sender, id))");
        db.execSQL("CREATE TABLE cursors (owner TEXT PRIMARY KEY, sequence INTEGER NOT NULL)");
        createBlockedTable(db);
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
    }

    private void createBlockedTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE blocked (owner TEXT NOT NULL, peer TEXT NOT NULL, PRIMARY KEY(owner, peer))");
    }

    private void touch(SQLiteDatabase db, String owner, String peer, long time) {
        ContentValues values = new ContentValues();
        values.put("owner", owner);
        values.put("peer", peer);
        values.put("touched", Long.valueOf(time));
        db.insertWithOnConflict("chats", null, values, 4);
        db.execSQL("UPDATE chats SET touched = MAX(touched, ?) WHERE owner = ? AND peer = ?", new Object[]{Long.valueOf(time), owner, peer});
    }

    void createChat(String owner, String peer) {
        touch(getWritableDatabase(), owner, peer, System.currentTimeMillis());
    }

    List<String> chats(String owner) {
        List<String> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT peer FROM chats WHERE owner = ? ORDER BY touched DESC, peer", new String[]{owner});
        while (c.moveToNext()) {
            try {
                result.add(c.getString(0));
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (c != null) {
            c.close();
        }
        return result;
    }

    List<String> chats(String owner, String user, int filter) {
        List<String> result = new ArrayList<>();
        String where = filter == 2 ? "archived = 1" : "archived = 0";
        boolean incoming = filter == 1 || filter == 3;
        if (incoming) {
            where = where + " AND EXISTS (SELECT 1 FROM messages m WHERE m.owner = chats.owner AND m.sender = chats.peer AND m.recipient = ?" + (filter == 3 ? " AND m.unread = 1)" : ")");
        }
        String[] args = incoming ? new String[]{owner, user} : new String[]{owner};
        Cursor c = getReadableDatabase().rawQuery("SELECT peer FROM chats WHERE owner = ? AND " + where + " ORDER BY touched DESC, peer", args);
        while (c.moveToNext()) {
            try {
                result.add(c.getString(0));
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (c != null) {
            c.close();
        }
        return result;
    }

    boolean isArchived(String owner, String peer) {
        boolean z = false;
        Cursor c = getReadableDatabase().rawQuery("SELECT archived FROM chats WHERE owner = ? AND peer = ?", new String[]{owner, peer});
        try {
            if (c.moveToFirst()) {
                if (c.getInt(0) != 0) {
                    z = true;
                }
            }
            if (c != null) {
                c.close();
            }
            return z;
        } catch (Throwable th) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    void setArchived(String str, String str2, boolean z) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("archived", Integer.valueOf(z ? 1 : 0));
        getWritableDatabase().update("chats", contentValues, "owner = ? AND peer = ?", new String[]{str, str2});
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

    boolean isBlocked(String owner, String peer) {
        return isBlocked(getReadableDatabase(), owner, peer);
    }

    private boolean isBlocked(SQLiteDatabase db, String owner, String peer) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM blocked WHERE owner = ? AND peer = ?", new String[]{owner, peer});
        try {
            boolean moveToFirst = cursor.moveToFirst();
            if (cursor != null) {
                cursor.close();
            }
            return moveToFirst;
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
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
        db.insertWithOnConflict("blocked", null, values, 4);
    }

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
            v.put("created", Long.valueOf(now));
            v.put("sequence", (Integer) 0);
            v.put("pending", Integer.valueOf(localOnly ? 3 : 1));
            v.put("audio", audio);
            db.insertOrThrow("messages", null, v);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Message fromCursor(Cursor c) {
        return new Message(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getLong(5), c.getInt(6), c.getString(7));
    }

    List<Message> pending(String owner) {
        List<Message> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,sender,recipient,body,created,sequence,pending,audio FROM messages WHERE owner = ? AND pending = 1 AND NOT EXISTS (SELECT 1 FROM blocked WHERE blocked.owner = messages.owner AND peer = recipient) ORDER BY created,id", new String[]{owner});
        while (c.moveToNext()) {
            try {
                result.add(fromCursor(c));
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (c != null) {
            c.close();
        }
        return result;
    }

    void markRead(String owner, String user, String peer, long throughSequence) {
        ContentValues values = new ContentValues();
        values.put("unread", (Integer) 0);
        getWritableDatabase().update("messages", values, "owner = ? AND recipient = ? AND sender = ? AND unread = 1 AND sequence <= ?", new String[]{owner, user, peer, Long.toString(throughSequence)});
    }

    List<Message> messages(String owner, String user, String peer) {
        List<Message> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,sender,recipient,body,created,sequence,pending,audio FROM messages WHERE owner = ? AND ((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)) ORDER BY created,sequence,id", new String[]{owner, user, peer, peer, user});
        while (c.moveToNext()) {
            try {
                result.add(fromCursor(c));
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (c != null) {
            c.close();
        }
        return result;
    }

    Map<String, Message> latestMessages(String owner, String user) {
        Map<String, Message> latest = new HashMap<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT id,sender,recipient,body,created,sequence,pending,audio FROM messages WHERE owner = ? AND (sender = ? OR recipient = ?) ORDER BY created DESC,sequence DESC,id DESC", new String[]{owner, user, user});
        while (cursor.moveToNext()) {
            try {
                String peer = user.equals(cursor.getString(1)) ? cursor.getString(2) : cursor.getString(1);
                if (!latest.containsKey(peer)) {
                    latest.put(peer, fromCursor(cursor));
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return latest;
    }

    void markFailed(String owner, Message message) {
        ContentValues values = new ContentValues();
        values.put("pending", (Integer) 2);
        getWritableDatabase().update("messages", values, "owner = ? AND sender = ? AND id = ? AND pending = 1", new String[]{owner, message.from(), message.id()});
    }

    void retry(String owner, Message message) {
        ContentValues values = new ContentValues();
        values.put("pending", (Integer) 1);
        getWritableDatabase().update("messages", values, "owner = ? AND sender = ? AND id = ? AND pending = 2", new String[]{owner, message.from(), message.id()});
    }

    void removeUnsent(String owner, Message message) {
        getWritableDatabase().delete("messages", "owner = ? AND sender = ? AND id = ? AND pending IN (1, 2)", new String[]{owner, message.from(), message.id()});
    }

    long cursor(String owner) {
        Cursor c = getReadableDatabase().rawQuery("SELECT sequence FROM cursors WHERE owner = ?", new String[]{owner});
        try {
            long j = c.moveToFirst() ? c.getLong(0) : 0L;
            if (c != null) {
                c.close();
            }
            return j;
        } catch (Throwable th) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void storeDelivered(SQLiteDatabase db, String owner, String user, Message message) {
        String peer = message.from().equals(user) ? message.to() : message.from();
        touch(db, owner, peer, message.time());
        ContentValues v = new ContentValues();
        v.put("owner", owner);
        v.put("sender", message.from());
        v.put("id", message.id());
        v.put("recipient", message.to());
        v.put("body", message.text());
        v.put("created", Long.valueOf(message.time()));
        v.put("sequence", Long.valueOf(message.sequence()));
        v.put("pending", (Integer) 0);
        boolean incoming = !message.from().equals(user) && message.to().equals(user);
        v.put("unread", Integer.valueOf(incoming ? 1 : 0));
        v.put("audio", message.audio());
        db.insertWithOnConflict("messages", null, v, incoming ? 4 : 5);
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
            try (Cursor pending = db.rawQuery("SELECT 1 FROM messages WHERE owner = ? AND sender = ? AND id = ? AND pending = 1", new String[]{owner, user, message.id()})) {
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
            v.put("sequence", Long.valueOf(cursor));
            db.insertWithOnConflict("cursors", null, v, 5);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
