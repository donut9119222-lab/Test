package dev.hatek.client.ui;

/**
 * Shared ClickGUI preferences (sounds + background particles).
 */
public final class GuiPrefs {
    private static float soundVolume = 0.55f;
    private static boolean particles = true;
    private static int particleCount = 42;
    private static float particleSpeed = 0.55f;
    private static float particleOpacity = 0.55f;

    private GuiPrefs() {
    }

    public static float soundVolume() {
        return soundVolume;
    }

    public static void soundVolume(float v) {
        soundVolume = Math.max(0.0f, Math.min(1.0f, v));
    }

    public static boolean particles() {
        return particles;
    }

    public static void particles(boolean v) {
        particles = v;
    }

    public static int particleCount() {
        return particleCount;
    }

    public static void particleCount(int v) {
        particleCount = Math.max(8, Math.min(120, v));
    }

    public static float particleSpeed() {
        return particleSpeed;
    }

    public static void particleSpeed(float v) {
        particleSpeed = Math.max(0.1f, Math.min(2.0f, v));
    }

    public static float particleOpacity() {
        return particleOpacity;
    }

    public static void particleOpacity(float v) {
        particleOpacity = Math.max(0.05f, Math.min(1.0f, v));
    }
}
