package org.telegram.messenger;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of the available {@link LuminaTranscriber} engines and the currently selected
 * one. Vosk (offline) is the default. {@code WhisperTranscriber} and
 * {@code GoogleSpeechTranscriber} are provided by a parallel agent and referenced here by
 * name — this class compiles once those classes are merged in.
 */
public final class LuminaTranscribers {

    private LuminaTranscribers() {}

    /** Instantiate an engine by its stable id; unknown / null falls back to Vosk. */
    public static LuminaTranscriber byId(String id) {
        if (id != null) {
            switch (id) {
                case "whisper":
                    return new WhisperTranscriber();
                case "google":
                    return new GoogleSpeechTranscriber();
                case "vosk":
                default:
                    return new VoskTranscriber();
            }
        }
        return new VoskTranscriber();
    }

    /** The engine chosen in settings, defaulting to the offline Vosk engine. */
    public static LuminaTranscriber current() {
        return byId(LuminaConfig.getString("sttEngine", "vosk"));
    }

    /** All engines, in picker order (offline Vosk first). */
    public static List<LuminaTranscriber> all() {
        List<LuminaTranscriber> list = new ArrayList<>();
        list.add(new VoskTranscriber());
        list.add(new WhisperTranscriber());
        list.add(new GoogleSpeechTranscriber());
        return list;
    }
}
