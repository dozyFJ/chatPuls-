package dev.dozyfj.pulsemessenger;

import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class CallHistory {
    private static final int MAX_ENTRIES = 100;
    private final SharedPreferences preferences;

    enum Outcome {
        COMPLETED("Завершён"),
        MISSED("Пропущен"),
        DECLINED("Отклонён"),
        UNAVAILABLE("Недоступен"),
        CANCELED("Отменён");

        final String label;

        Outcome(String label) {
            this.label = label;
        }
    }

    record Entry(String peer, boolean incoming, Outcome outcome, long time) {}

    CallHistory(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    List<Entry> entries(String owner) {
        List<Entry> entries = new ArrayList<>();
        try {
            JSONArray source = new JSONArray(this.preferences.getString(key(owner), "[]"));
            for (int i = 0; i < 100 && i < source.length(); i++) {
                JSONObject item = source.optJSONObject(i);
                if (item != null && !item.optString("peer").isEmpty()) {
                    try {
                        entries.add(new Entry(item.getString("peer"), item.getBoolean("incoming"), Outcome.valueOf(item.getString("outcome")), item.getLong("time")));
                    } catch (IllegalArgumentException e) {
                    } catch (JSONException e2) {
                    }
                }
            }
            return entries;
        } catch (JSONException e3) {
            return entries;
        }
    }

    void add(String owner, Entry entry) {
        List<Entry> entries = entries(owner);
        entries.add(0, entry);
        JSONArray result = new JSONArray();
        for (int i = 0; i < 100 && i < entries.size(); i++) {
            Entry item = entries.get(i);
            try {
                result.put(new JSONObject().put("peer", item.peer()).put("incoming", item.incoming()).put("outcome", item.outcome().name()).put("time", item.time()));
            } catch (JSONException error) {
                throw new IllegalStateException(error);
            }
        }
        this.preferences.edit().putString(key(owner), result.toString()).apply();
    }

    private static String key(String owner) {
        return "call.history/" + Uri.encode(owner);
    }
}
