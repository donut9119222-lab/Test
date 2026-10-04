package dev.hatek.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class UiSounds {
    public enum Mode {
        CLICK("Click"),
        SOFT("Soft"),
        TICK("Tick"),
        POP("Pop"),
        NOTE("Note"),
        PLING("Pling"),
        ORB("Orb"),
        WOOD("Wood");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    private static boolean enabled = true;
    private static Mode mode = Mode.SOFT;

    private UiSounds() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enabled(boolean value) {
        enabled = value;
    }

    public static Mode mode() {
        return mode;
    }

    public static void mode(Mode value) {
        mode = value == null ? Mode.SOFT : value;
    }

    private static SoundEvent resolve(String... fields) {
        for (String field : fields) {
            try {
                Object raw = SoundEvents.class.getField(field).get(null);
                if (raw instanceof SoundEvent se) {
                    return se;
                }
                return (SoundEvent) raw.getClass().getMethod("value").invoke(raw);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static SoundEvent eventForMode() {
        return switch (mode) {
            case CLICK -> resolve("UI_BUTTON_CLICK");
            case SOFT -> resolve("UI_BUTTON_CLICK");
            case TICK -> resolve("UI_BUTTON_CLICK", "UI_HOTBAR_SELECT");
            case POP -> resolve(
                    "UI_TOAST_IN",
                    "UI_TOAST_CHALLENGE_COMPLETE",
                    "BOOK_PAGE_TURN",
                    "UI_BUTTON_CLICK"
            );
            case NOTE -> resolve("NOTE_BLOCK_HAT", "NOTE_BLOCK_BASEDRUM", "UI_BUTTON_CLICK");
            case PLING -> resolve("NOTE_BLOCK_PLING", "NOTE_BLOCK_BELL", "UI_BUTTON_CLICK");
            case ORB -> resolve("EXPERIENCE_ORB_PICKUP", "UI_BUTTON_CLICK");
            case WOOD -> resolve("WOODEN_BUTTON_CLICK_ON", "WOODEN_PRESSURE_PLATE_CLICK_ON", "UI_BUTTON_CLICK");
        };
    }

    public static void click() {
        float pitch = switch (mode) {
            case CLICK -> 1.08f;
            case SOFT -> 1.30f;
            case TICK -> 1.60f;
            case POP -> 1.15f;
            case NOTE -> 1.45f;
            case PLING -> 1.35f;
            case ORB -> 1.25f;
            case WOOD -> 1.05f;
        };
        float vol = switch (mode) {
            case CLICK -> 0.42f;
            case SOFT -> 0.28f;
            case TICK -> 0.22f;
            case POP -> 0.36f;
            case NOTE -> 0.24f;
            case PLING -> 0.26f;
            case ORB -> 0.30f;
            case WOOD -> 0.34f;
        };
        play(pitch, vol);
    }

    public static void soft() {
        play(1.38f, 0.16f);
    }

    private static void play(float pitch, float volume) {
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) {
            return;
        }
        SoundEvent event = eventForMode();
        if (event == null) {
            return;
        }
        float p = pitch + (float) (Math.random() * 0.05f);
        float v = volume * GuiPrefs.soundVolume();
        if (v <= 0.001f) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(event, p, v));
    }
}
