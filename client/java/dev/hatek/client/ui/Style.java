package dev.hatek.client.ui;

/**
 * Xentrix-inspired design tokens. Compatible names for Module/settings.
 */
public final class Style {
    private Style() {
    }

    public static final int PANEL = 0xF20E1016;
    public static final int PANEL_RAISED = 0xFF181C24;
    public static final int DEFAULT_ACCENT = 0xFF5B9DFF;

    private static int accent = DEFAULT_ACCENT;

    public static int accent() {
        return accent;
    }

    public static void accent(int argb) {
        accent = argb | 0xFF000000;
    }

    public static int white(float opacity) {
        return (Math.round(opacity * 255.0f) << 24) | 0x00FFFFFF;
    }

    public static final int WHITE_02 = white(0.02f);
    public static final int WHITE_04 = white(0.04f);
    public static final int WHITE_08 = white(0.08f);
    public static final int WHITE_25 = white(0.25f);
    public static final int WHITE_45 = white(0.45f);
    public static final int WHITE = 0xFFFFFFFF;

    public static final int SHADOW = 0x55000000;
    public static final int SHADOW_DY = 3;
    public static final float SHADOW_BLUR = 2.0f;

    public static final int DANGER = 0xFFFF6164;
    public static final int FAVOURITE = 0xFFFF0055;

    // Panel
    public static final int PANEL_W = 860;
    public static final int PANEL_H = 560;
    public static final int PANEL_R = 20;
    public static final int DESIGN_X = 530;
    public static final int DESIGN_Y = 230;
    public static final int DESIGN_SCREEN_W = 1920;
    public static final int DESIGN_SCREEN_H = 1080;

    // Sidebar (labeled)
    public static final int SIDEBAR_X = 16;
    public static final int SIDEBAR_W = 158;
    public static final int SIDEBAR_H = 360;
    public static final int SIDEBAR_R = 14;
    public static final int SIDEBAR_ICON = 14;
    public static final float SIDEBAR_STEP = 40.0f;
    public static final int SIDEBAR_TOP = 78;
    public static final int SELECTOR_W = 150;
    public static final int SELECTOR_H = 34;
    public static final int SELECTOR_R = 11;
    public static final int SELECTOR_X = 20;

    // Header / search
    public static final int HEADER_BAR_H = 70;
    public static final int SEARCH_W = 180;
    public static final int SEARCH_H = 30;
    public static final int SEARCH_R = 10;

    // Content — 2 columns, clear of sidebar/nav
    public static final int CONTENT_X = 190;
    public static final int CONTENT_TOP = 74;
    public static final int CONTENT_BOTTOM = 64; // reserved for bottom nav
    public static final int COLUMNS = 2;
    public static final int CARD_W = 310;
    public static final int COLUMN_PITCH = 322;
    public static final int CARD_GAP = 12;
    public static final int CARD_R = 14;

    public static final int SCROLLBAR_X = 846;
    public static final int SCROLLBAR_Y = 74;
    public static final int SCROLLBAR_W = 3;
    public static final int SCROLLBAR_H = 420;
    public static final int SCROLLBAR_MIN_THUMB = 32;

    // Module card
    public static final int HEADER_H = 52;
    public static final int COLLAPSED_H = 52;

    public static final int ICON_X = 14;
    public static final int ICON_Y = 18;
    public static final int ICON_SIZE = 13;

    public static final int TITLE_X = 38;
    public static final int TITLE_BASELINE = 31;

    public static final int DESC_X = 14;
    public static final int DESC_BASELINE = 0;
    public static final int DESC_LINE = 0;
    public static final int DESC_MAX_W = 190;

    public static final int BADGE_X = 228;
    public static final int BADGE_Y = 16;
    public static final int BADGE_W = 30;
    public static final int BADGE_H = 18;
    public static final int BADGE_R = 7;
    public static final int BADGE_KEY_X = 7;
    public static final int BADGE_KEY_BASELINE = 13;
    public static final int BADGE_ICON_X = 16;
    public static final int BADGE_ICON_Y = 5;
    public static final int BADGE_ICON_W = 8;
    public static final int BADGE_ICON_H = 6;

    public static final int TOGGLE_X = 266;
    public static final int TOGGLE_Y = 16;
    public static final int TOGGLE_W = 32;
    public static final int TOGGLE_H = 18;
    public static final float TOGGLE_R = 9.0f;
    public static final int KNOB = 14;
    public static final int KNOB_INSET = 2;

    public static final int ROW_X = 14;
    public static final int ROW_W = 282;
    public static final int ROW_H = 32;
    public static final int ROW_R = 8;
    public static final int SETTING_GAP = 20;
    public static final int LABEL_TO_CONTROL = 16;
    public static final int CARD_BOTTOM_PAD = 22;

    public static final int TRACK_W = 168;
    public static final int TRACK_H = 4;
    public static final int TRACK_R = 2;
    public static final int SLIDER_KNOB = 8;

    public static final int CHECKBOX = 16;
    public static final int CHECKBOX_R = 5;
    public static final int CHECK_GLYPH = 5;

    public static final int FIELD_ICON = 8;
    public static final int FIELD_ICON_X = 8;
    public static final int FIELD_TEXT_BASELINE = 18;
    public static final int CHEVRON_X = 266;
    public static final int CHEVRON_Y = 12;
    public static final int CHEVRON_W = 6;
    public static final int CHEVRON_H = 4;
    public static final int SWATCH = 12;
    public static final int SWATCH_X = 260;
    public static final int SWATCH_Y = 8;
    public static final int SWATCH_R = 4;

    public static final int CHIP_H = 24;
    public static final int CHIP_R = 7;
    public static final int CHIP_GAP = 8;
    public static final int CHIP_ROW_STEP = 34;
    public static final int CHIP_PAD = 10;
    public static final int CHIP_BASELINE = 15;

    // Configs / themes entries
    public static final int ENTRY_H = 96;
    public static final int ENTRY_PITCH = ENTRY_H + CARD_GAP;
    public static final int AVATAR = 20;
    public static final int AVATAR_X = 15;
    public static final int AVATAR_Y = 13;
    public static final int ENTRY_META_BASELINE = 27;
    public static final int ENTRY_AUTHOR_X = 41;
    public static final int ENTRY_DATE_RIGHT = 257;
    public static final int ENTRY_NAME_X = 15;
    public static final int ENTRY_NAME_BASELINE = 52;

    public static final int SWOOSH_W = 123;
    public static final int SWOOSH_H = 52;

    public static final int HEART_W = 12;
    public static final int HEART_H = 11;
    public static final int HEART_X = 244;
    public static final int HEART_Y = 42;
    public static final int ACTION_W = 10;
    public static final int ACTION_H = 12;
    public static final int ACTION_X = 18;
    public static final int ACTION_Y = 72;
    public static final int ACTION_STEP = 19;

    // Bottom nav — single strip, no overlap with sidebar tools
    public static final int NAV_X = 190;
    public static final int NAV_Y = PANEL_H - 44;
    public static final int NAV_W = 184;
    public static final int NAV_H = 32;
    public static final int NAV_R = 12;
    public static final int NAV_ICON = 14;
    public static final int NAV_FIRST = 22;
    public static final int NAV_STEP = 44;
    public static final int NAV_INDICATOR_W = 18;
    public static final int NAV_INDICATOR_H = 2;
    public static final int NAV_INDICATOR_Y = 26;

    // Sidebar footer tools (configs only) — ABOVE nav, under categories
    public static final int SOURCE_Y = 450;
    public static final int SOURCE_H = 40;
    public static final float SOURCE_STEP = 44.0f; // horizontal spacing

    // HUD
    public static final int HUD_PANEL = 0xDB0E1016;
    public static final int HUD_MARGIN = 10;
    public static final int HUD_CARD_R = CARD_R;
    public static final int HUD_PAD = 13;
    public static final int HUD_HEADER_H = 38;
    public static final int HUD_ICON = ICON_SIZE;
    public static final int HUD_ICON_X = ICON_X;
    public static final int HUD_ICON_Y = 13;
    public static final int HUD_TITLE_X = TITLE_X;
    public static final int HUD_TITLE_BASELINE = 26;
    public static final int HUD_ROW_H = 20;
    public static final int HUD_BOTTOM_PAD = 12;
    public static final int HUD_MIN_W = 150;
    public static final int HUD_BADGE_H = 16;
    public static final int HUD_BADGE_R = CHIP_R;
    public static final int HUD_BADGE_PAD = 7;
    public static final int HUD_STUB_W = 2;
    public static final int HUD_STUB_H = 10;

    public static final int WM_H = 34;
    public static final int WM_R = CARD_R;
    public static final int WM_PAD = 14;
    public static final int WM_BASELINE = 22;
    public static final int WM_SEP_W = 1;
    public static final int WM_SEP_H = 12;
    public static final int WM_SEP_GAP = 11;
    public static final int WM_UNIT_GAP = 3;

    public static final float FONT_HUD = 12.0f;
    public static final float FONT_TITLE = 14.5f;
    public static final float FONT_BODY = 12.0f;
    public static final float FONT_LABEL = 11.5f;

    public static int contentViewportH() {
        return PANEL_H - CONTENT_TOP - CONTENT_BOTTOM;
    }
}
