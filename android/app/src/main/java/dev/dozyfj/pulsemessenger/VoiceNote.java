package dev.dozyfj.pulsemessenger;

import java.util.Base64;
import org.json.JSONObject;

final class VoiceNote {
    static final String LABEL = "Голосовое сообщение";
    static final int MAX_AUDIO_CHARS = 96000;
    static final int MAX_REQUEST_BYTES = 100000;

    private VoiceNote() {
    }

    static boolean valid(String audio) {
        if (audio.isEmpty() || audio.length() > MAX_AUDIO_CHARS || audio.length() % 4 != 0) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(audio).length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static String readAudio(JSONObject message) {
        String audio = message.optString("audio", "");
        return valid(audio) ? audio : "";
    }
}
