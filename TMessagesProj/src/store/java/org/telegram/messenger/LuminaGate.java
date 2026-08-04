package org.telegram.messenger;

/**
 * LuminaGate — STORE flavor.
 * Google-Play-safe build: no ToS-sensitive capability exists. Retained only as the
 * build discriminator and a hard-disabled allowSaveRestricted() (referenced by
 * PhotoViewer).
 */
public class LuminaGate {

    /** False in the store build. */
    public static final boolean FULL = false;

    public static boolean allowSaveRestricted() {
        return false;
    }
}
