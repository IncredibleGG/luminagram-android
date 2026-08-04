package org.telegram.messenger;

/**
 * LuminaGate — FULL flavor.
 * The ToS-sensitive relaxations (copy/save from restricted chats, premium-translate
 * bypass) have been removed. Retained only as the FULL build discriminator and a
 * hard-disabled {@link #allowSaveRestricted()} that PhotoViewer still references:
 * saving media from no-save chats is not permitted (Telegram ToS).
 */
public class LuminaGate {

    /** True only in the full (non-store) build. */
    public static final boolean FULL = true;

    /** Saving media from no-save (noforwards) chats is not permitted (Telegram ToS). */
    public static boolean allowSaveRestricted() {
        return false;
    }
}
