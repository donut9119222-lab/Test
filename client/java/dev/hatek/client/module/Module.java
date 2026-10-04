package dev.hatek.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hatek.client.module.setting.ModeSetting;
import dev.hatek.client.module.setting.Setting;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.UiSounds;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Module card + settings editor.
 * Clean layout — no nested clips (they broke text transforms).
 */
public class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();

    private boolean enabled;
    private boolean expanded;
    private int keyCode = GLFW.GLFW_KEY_UNKNOWN;
    private boolean binding;

    private final Anim toggleAnim = new Anim(0.0f, 16.0f);
    private final Anim expandAnim = new Anim(0.0f, 14.0f);
    private final Anim hoverAnim = new Anim(0.0f, 16.0f);
    private final Anim badgeAnim = new Anim(0.0f, 18.0f);

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Module with(Setting... values) {
        this.settings.addAll(List.of(values));
        return this;
    }

    public Module keybind(int key) {
        this.keyCode = key;
        return this;
    }

    public Module expanded(boolean value) {
        this.expanded = value;
        this.expandAnim.snap(value ? 1.0f : 0.0f);
        return this;
    }

    public Module enabled(boolean value) {
        this.enabled = value;
        this.toggleAnim.snap(value ? 1.0f : 0.0f);
        return this;
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public Category category() {
        return this.category;
    }

    public List<Setting> settings() {
        return this.settings;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public int keyCode() {
        return this.keyCode;
    }

    public boolean isBinding() {
        return this.binding;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void setEnabled(boolean value) {
        if (this.enabled != value) {
            toggle();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onClientTick() {
    }

    /** Vertical start of first setting baseline relative to card top. */
    private float settingsOriginY(float cardY) {
        return cardY + Style.HEADER_H + 8.0f;
    }

    public int expandedHeight() {
        if (this.settings.isEmpty()) {
            return Style.COLLAPSED_H;
        }
        int h = Style.HEADER_H + 8;
        for (Setting setting : this.settings) {
            h += Style.SETTING_GAP + setting.bottomOffset();
            // extra room so Mode overlay list is not clipped (no layout jump while closed)
            if (setting instanceof ModeSetting mode && mode.isOpen()) {
                h += 4 + mode.options().size() * 26;
            }
        }
        return h + Style.CARD_BOTTOM_PAD;
    }

    public float height() {
        this.expandAnim.to(this.expanded ? 1.0f : 0.0f);
        return Mth.lerp(Anim.easeInOut(this.expandAnim.get()),
                (float) Style.COLLAPSED_H, (float) expandedHeight());
    }

    public void render(GuiGraphicsExtractor gg, float x, float y, double mx, double my) {
        float h = height();
        float open = Anim.easeInOut(this.expandAnim.get());

        this.hoverAnim.to(Render2D.hovered(mx, my, x, y, Style.CARD_W, h) ? 1.0f : 0.0f);
        float hot = this.hoverAnim.get();

        // shell
        int bg = this.enabled
                ? Render2D.lerp(Style.WHITE_02, Style.white(0.05f), Math.max(hot, 0.4f))
                : Render2D.lerp(Style.WHITE_02, Style.white(0.04f), hot);
        Render2D.round(gg, x, y, Style.CARD_W, h, Style.CARD_R, bg);

        // accent rail when enabled
        this.toggleAnim.to(this.enabled ? 1.0f : 0.0f);
        float on = Anim.easeInOut(this.toggleAnim.get());
        if (on > 0.01f) {
            Render2D.round(gg, x + 5.0f, y + 11.0f, 3.0f, Style.HEADER_H - 16.0f, 1.5f,
                    Render2D.withAlpha(Style.accent(), 0.4f + on * 0.6f));
        }

        renderHeader(gg, x, y, mx, my);

        if (open <= 0.01f || this.settings.isEmpty()) {
            return;
        }

        // divider
        Render2D.round(gg, x + 14.0f, y + Style.HEADER_H,
                Style.CARD_W - 28.0f, 1.0f, 0.5f,
                Render2D.withAlpha(Style.WHITE_04, open));

        // ONE clip only — never nest clips (nested scissor flipped text)
        float sheetY = y + Style.HEADER_H + 1.0f;
        float sheetH = Math.max(0.0f, h - Style.HEADER_H - 1.0f);
        Render2D.pushClip(gg, x, sheetY, Style.CARD_W, sheetH);
        Render2D.pushAlpha(Mth.clamp((open - 0.12f) / 0.7f, 0.0f, 1.0f));

        float cursor = settingsOriginY(y);
        for (Setting setting : this.settings) {
            float baseline = cursor + Style.SETTING_GAP;
            setting.render(gg, x + Style.ROW_X, baseline, mx, my);
            cursor = baseline + setting.bottomOffset();
        }

        Render2D.popAlpha();
        Render2D.popClip(gg);
    }

    private void renderHeader(GuiGraphicsExtractor gg, float x, float y, double mx, double my) {
        int iconTint = this.enabled ? Style.accent() : Style.WHITE_25;
        Render2D.icon(gg, this.category.icon(),
                x + Style.ICON_X + 4.0f, y + Style.ICON_Y,
                Style.ICON_SIZE, Style.ICON_SIZE, iconTint);

        Fonts.title().draw(gg, this.name,
                x + Style.TITLE_X + 4.0f, y + Style.TITLE_BASELINE,
                this.enabled ? Style.WHITE : Style.WHITE_45);

        renderBadge(gg, x, y, mx, my);
        renderToggle(gg, x, y, mx, my);
    }

    private void renderBadge(GuiGraphicsExtractor gg, float x, float y, double mx, double my) {
        float bx = x + Style.BADGE_X;
        float by = y + Style.BADGE_Y;
        this.badgeAnim.to(this.binding
                || Render2D.hovered(mx, my, bx, by, Style.BADGE_W, Style.BADGE_H) ? 1.0f : 0.0f);
        float k = this.badgeAnim.get();
        int background = this.binding
                ? Render2D.lerp(Style.WHITE_04, Style.accent(), k)
                : Render2D.lerp(Style.WHITE_04, Style.white(0.09f), k);
        Render2D.round(gg, bx, by, Style.BADGE_W, Style.BADGE_H, Style.BADGE_R, background);

        int tint = this.binding
                ? Render2D.lerp(Style.WHITE_25, Style.PANEL, k)
                : Render2D.lerp(Style.WHITE_25, Style.WHITE_45, k);
        String label = this.binding && (System.currentTimeMillis() / 350L) % 2L == 0L ? "..." : keyLabel();
        Fonts.body().draw(gg, label, bx + Style.BADGE_KEY_X, by + Style.BADGE_KEY_BASELINE, tint);
        Render2D.icon(gg, "keyboard", bx + Style.BADGE_ICON_X, by + Style.BADGE_ICON_Y,
                Style.BADGE_ICON_W, Style.BADGE_ICON_H, tint);
    }

    private void renderToggle(GuiGraphicsExtractor gg, float x, float y, double mx, double my) {
        float k = Anim.easeInOut(this.toggleAnim.get());
        float tx = x + Style.TOGGLE_X;
        float ty = y + Style.TOGGLE_Y;
        boolean hover = Render2D.hovered(mx, my, tx, ty, Style.TOGGLE_W, Style.TOGGLE_H);
        Render2D.round(gg, tx, ty, Style.TOGGLE_W, Style.TOGGLE_H, Style.TOGGLE_R,
                Render2D.lerp(Style.WHITE_04, Style.accent(), k));
        float travel = Style.TOGGLE_W - Style.KNOB - Style.KNOB_INSET * 2;
        Render2D.circle(gg, tx + Style.KNOB_INSET + travel * k, ty + Style.KNOB_INSET,
                Style.KNOB, hover ? Style.WHITE : Style.white(0.94f));
    }

    public String keyLabel() {
        if (this.keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "-";
        }
        String name = InputConstants.Type.KEYSYM.getOrCreate(this.keyCode).getDisplayName().getString();
        return name.length() > 3
                ? name.substring(0, 3).toUpperCase(Locale.ROOT)
                : name.toUpperCase(Locale.ROOT);
    }

    public boolean mouseClicked(float x, float y, double mx, double my, int button) {
        // toggle
        if (button == 0 && Render2D.hovered(mx, my,
                x + Style.TOGGLE_X, y + Style.TOGGLE_Y, Style.TOGGLE_W, Style.TOGGLE_H)) {
            toggle();
            UiSounds.click();
            return true;
        }

        // keybind badge
        float bx = x + Style.BADGE_X;
        float by = y + Style.BADGE_Y;
        if (Render2D.hovered(mx, my, bx, by, Style.BADGE_W, Style.BADGE_H)) {
            if (button == 0) {
                this.binding = true;
                UiSounds.soft();
                return true;
            }
            if (button == 1) {
                this.keyCode = GLFW.GLFW_KEY_UNKNOWN;
                UiSounds.soft();
                return true;
            }
        }

        // settings (only when mostly open)
        if (this.expanded && this.expandAnim.get() > 0.45f) {
            float cursor = settingsOriginY(y);
            for (Setting setting : this.settings) {
                float baseline = cursor + Style.SETTING_GAP;
                if (setting.mouseClicked(x + Style.ROW_X, baseline, mx, my, button)) {
                    UiSounds.soft();
                    return true;
                }
                cursor = baseline + setting.bottomOffset();
            }
        }

        // expand / collapse on header
        if (Render2D.hovered(mx, my, x, y, Style.CARD_W, Style.COLLAPSED_H)
                && (button == 0 || button == 1)) {
            this.expanded = !this.expanded;
            if (!this.expanded) {
                this.settings.forEach(Setting::closePopups);
            }
            UiSounds.soft();
            return true;
        }
        return false;
    }

    public void mouseDragged(float x, float y, double mx, double my) {
        if (!this.expanded) {
            return;
        }
        float cursor = settingsOriginY(y);
        for (Setting setting : this.settings) {
            float baseline = cursor + Style.SETTING_GAP;
            setting.mouseDragged(x + Style.ROW_X, baseline, mx, my);
            cursor = baseline + setting.bottomOffset();
        }
    }

    public void mouseReleased() {
        this.settings.forEach(Setting::mouseReleased);
    }

    public boolean consumeBinding(int key) {
        if (!this.binding) {
            return false;
        }
        this.binding = false;
        this.keyCode = key == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : key;
        return true;
    }

    public boolean keyPressed(int key) {
        for (Setting setting : this.settings) {
            if (setting.capturesInput() && setting.keyPressed(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char c) {
        for (Setting setting : this.settings) {
            if (setting.capturesInput() && setting.charTyped(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean capturesInput() {
        for (Setting setting : this.settings) {
            if (setting.capturesInput()) {
                return true;
            }
        }
        return false;
    }
}
