package dev.dozyfj.pulsemessenger;

import dev.dozyfj.pulsemessenger.CallSessions;
import dev.dozyfj.pulsemessenger.PhoneServerStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;

final class CallSessions {
    private final Map<String, Call> calls = new LinkedHashMap();

    CallSessions() {
    }

    static final class Call {
        long calleeSeen;
        long callerSeen;
        long changed;
        final String from;
        final String id;
        final String offer;
        final String to;
        String state = "ringing";
        String answer = "";
        String reason = "";

        Call(String id, String from, String to, String offer, long now) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.offer = offer;
            this.changed = now;
            this.calleeSeen = now;
            this.callerSeen = now;
        }

        boolean includes(String user) {
            return this.from.equals(user) || this.to.equals(user);
        }

        boolean live() {
            return this.state.equals("ringing") || this.state.equals("answered");
        }

        JSONObject json() throws Exception {
            JSONObject out = new JSONObject().put("id", this.id).put("from", this.from).put("to", this.to).put("state", this.state);
            if (this.state.equals("ringing")) {
                out.put("offer", this.offer);
            }
            if (this.state.equals("answered")) {
                out.put("answer", this.answer);
            }
            if (this.state.equals("ended") && !this.reason.isEmpty()) {
                out.put("reason", this.reason);
            }
            return out;
        }
    }

    private long prune() {
        final long now = System.currentTimeMillis();
        for (Call call : this.calls.values()) {
            if ((call.state.equals("ringing") && now - call.changed > 60000) || (call.state.equals("answered") && now - Math.min(call.callerSeen, call.calleeSeen) > 45000)) {
                call.state = "ended";
                call.changed = now;
            }
        }
        this.calls.values().removeIf(obj -> CallSessions.isStale(now, (CallSessions.Call) obj));
        return now;
    }

    static boolean isStale(long now, Call call) {
        return !call.live() && now - call.changed > 20000;
    }

    synchronized JSONObject fetch(String user) throws Exception {
        JSONArray items;
        long now = prune();
        items = new JSONArray();
        for (Call call : this.calls.values()) {
            if (call.includes(user)) {
                if (call.from.equals(user)) {
                    call.callerSeen = now;
                } else {
                    call.calleeSeen = now;
                }
                items.put(call.json());
            }
        }
        return new JSONObject().put("calls", items);
    }

    synchronized JSONObject change(final String user, JSONObject input, PhoneServerStore store) throws Exception {
        long now = prune();
        String id = input.optString("id");
        String action = input.optString("action");
        if (!id.matches("[a-f0-9-]{36}")) {
            throw new PhoneServerStore.Failure(400);
        }
        Call call = this.calls.get(id);
        if (action.equals("offer")) {
            final String to = input.optString("to");
            String offer = description(input);
            if (!PhoneServerStore.validName(to) || to.equals(user)) {
                throw new PhoneServerStore.Failure(400);
            }
            if (call != null && call.from.equals(user) && call.to.equals(to) && call.offer.equals(offer)) {
                return call.json();
            }
            if (!store.userExists(to)) {
                throw new PhoneServerStore.Failure(404);
            }
            if (call == null && this.calls.size() < 128) {
                if (!this.calls.values().stream().anyMatch(obj -> CallSessions.isBusyWith(user, to, (CallSessions.Call) obj))) {
                    Call created = new Call(id, user, to, offer, now);
                    this.calls.put(id, created);
                    return created.json();
                }
            }
            throw new PhoneServerStore.Failure(409);
        }
        if (call == null || !call.includes(user)) {
            throw new PhoneServerStore.Failure(404);
        }
        if (action.equals("answer") && call.to.equals(user)) {
            String answer = description(input);
            if (call.state.equals("answered") && call.answer.equals(answer)) {
                return call.json();
            }
            if (!call.state.equals("ringing")) {
                throw new PhoneServerStore.Failure(409);
            }
            call.state = "answered";
            call.answer = answer;
            call.calleeSeen = now;
        } else if (action.equals("end")) {
            if (!call.live()) {
                return call.json();
            }
            String reason = input.optString("reason");
            if (reason.equals("unavailable") || (reason.equals("declined") && call.state.equals("ringing") && call.to.equals(user))) {
                call.reason = reason;
            }
            call.state = "ended";
        } else {
            throw new PhoneServerStore.Failure(400);
        }
        call.changed = now;
        return call.json();
    }

    static boolean isBusyWith(String user, String to, Call item) {
        return item.live() && (item.includes(user) || item.includes(to));
    }

    private static String description(JSONObject input) throws PhoneServerStore.Failure {
        String sdp = input.optString("sdp");
        if (!sdp.startsWith("v=0") || sdp.length() > 12000) {
            throw new PhoneServerStore.Failure(400);
        }
        return sdp;
    }
}
