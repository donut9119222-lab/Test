package dev.hatek.client.ui.screen;

import dev.hatek.client.feature.config.ConfigEntry;
import dev.hatek.client.feature.config.ConfigManager;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.UiSounds;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigsPage extends Page {
    private final Map<String, Anim> hover = new HashMap<>();
    private final Map<String, Anim> heart = new HashMap<>();

    private String draftName = "";
    private boolean nameFocused;
    private ConfigEntry renaming;

    private Anim anim(Map<String, Anim> map, String key, float speed) {
        return map.computeIfAbsent(key, k -> new Anim(0.0f, speed));
    }

    @Override
    public void onShow() {
        ConfigManager.refresh();
    }

    @Override
    public float contentHeight() {
        int count = ConfigManager.all().size();
        int rows = (Math.max(1, count) + Style.COLUMNS - 1) / Style.COLUMNS;
        return 56.0f + Math.max(0, rows * Style.ENTRY_PITCH - Style.CARD_GAP);
    }

    @Override
    public void render(GuiGraphicsExtractor gg, float x, float y, float scroll, double mx, double my) {
        float top = y - scroll;

        // name field
        float fieldW = Style.CARD_W * 1.35f;
        float fieldH = 34.0f;
        boolean fieldHover = Render2D.hovered(mx, my, x, top, fieldW, fieldH);
        Render2D.round(gg, x, top, fieldW, fieldH, 12.0f,
                this.nameFocused || fieldHover ? Style.white(0.08f) : Style.WHITE_02);
        String shown = this.draftName.isEmpty() && !this.nameFocused ? "Название конфига..." : this.draftName;
        int col = this.draftName.isEmpty() && !this.nameFocused ? Style.WHITE_25 : Style.WHITE_45;
        Fonts.label().draw(gg, shown, x + 14.0f, top + 22.0f, col);

        // save button
        float btnW = 110.0f;
        float btnX = x + fieldW + 10.0f;
        boolean btnHover = Render2D.hovered(mx, my, btnX, top, btnW, fieldH);
        Render2D.round(gg, btnX, top, btnW, fieldH, 12.0f,
                btnHover ? Style.accent() : Render2D.withAlpha(Style.accent(), 0.75f));
        String btn = this.renaming != null ? "Переименовать" : "Сохранить";
        Fonts.label().draw(gg, btn, btnX + (btnW - Fonts.label().width(btn)) / 2.0f, top + 22.0f, Style.WHITE);

        List<ConfigEntry> configs = ConfigManager.all();
        float listY = top + 50.0f;
        if (configs.isEmpty()) {
            Fonts.body().draw(gg, "Конфигов пока нет — сохрани текущий", x + 2.0f,
                    listY + 20.0f, Style.WHITE_25);
            return;
        }

        float viewport = Style.contentViewportH();
        for (int i = 0; i < configs.size(); i++) {
            float cx = x + (i % Style.COLUMNS) * Style.COLUMN_PITCH;
            float cy = listY + (i / Style.COLUMNS) * Style.ENTRY_PITCH;
            if (cy > y + viewport || cy + Style.ENTRY_H < y) {
                continue;
            }
            renderCard(gg, configs.get(i), cx, cy, mx, my);
        }
    }

    private void renderCard(GuiGraphicsExtractor gg, ConfigEntry config, float x, float y,
                            double mx, double my) {
        String key = config.name();
        Anim hoverAnim = anim(this.hover, key, 17.0f);
        Anim heartAnim = anim(this.heart, key, 15.0f);
        hoverAnim.to(Render2D.hovered(mx, my, x, y, Style.CARD_W, Style.ENTRY_H) ? 1.0f : 0.0f);
        heartAnim.to(config.favourite() ? 1.0f : 0.0f);
        float hot = hoverAnim.get();
        float fav = heartAnim.get();
        boolean loaded = config.name().equals(ConfigManager.loadedName());

        Render2D.round(gg, x, y, Style.CARD_W, Style.ENTRY_H, Style.CARD_R,
                Render2D.lerp(Style.WHITE_02, Style.white(0.055f), Math.max(hot, loaded ? 0.5f : 0.0f)));
        if (loaded) {
            Render2D.round(gg, x + 6.0f, y + 14.0f, 3.0f, Style.ENTRY_H - 28.0f, 1.5f, Style.accent());
        }

        Fonts.title().draw(gg, config.name(), x + 18.0f, y + 36.0f, Style.WHITE);
        Fonts.label().draw(gg, config.date(), x + 18.0f, y + 56.0f, Style.WHITE_25);

        // favourite heart
        float hx = x + Style.CARD_W - 36.0f;
        float hy = y + 18.0f;
        int heartTint = fav > 0.5f ? Style.FAVOURITE : Render2D.lerp(Style.WHITE_25, Style.WHITE_45, hot);
        Render2D.icon(gg, "heart", hx, hy, Style.HEART_W, Style.HEART_H,
                Render2D.withAlpha(heartTint, 0.55f + fav * 0.45f));

        // delete
        float dx = x + Style.CARD_W - 36.0f;
        float dy = y + Style.ENTRY_H - 28.0f;
        boolean delHover = Render2D.hovered(mx, my, dx - 4.0f, dy - 4.0f, 24.0f, 24.0f);
        Render2D.icon(gg, "trash", dx, dy, 12.0f, 12.0f,
                delHover ? Style.DANGER : Style.WHITE_25);
    }

    private void commitName() {
        String name = this.draftName.trim();
        if (name.isEmpty()) {
            name = ConfigManager.nextFreeName();
        }
        if (this.renaming != null) {
            if (ConfigManager.rename(this.renaming, name)) {
                UiSounds.click();
            }
            this.renaming = null;
        } else {
            ConfigManager.save(name);
            UiSounds.click();
        }
        this.draftName = "";
        this.nameFocused = false;
        ConfigManager.refresh();
    }

    @Override
    public boolean mouseClicked(float x, float y, float scroll, double mx, double my, int button) {
        float top = y - scroll;
        float fieldW = Style.CARD_W * 1.35f;
        float fieldH = 34.0f;
        float btnW = 110.0f;
        float btnX = x + fieldW + 10.0f;

        if (button == 0 && Render2D.hovered(mx, my, x, top, fieldW, fieldH)) {
            this.nameFocused = true;
            return true;
        }
        if (button == 0 && Render2D.hovered(mx, my, btnX, top, btnW, fieldH)) {
            commitName();
            return true;
        }
        this.nameFocused = false;

        if (button != 0 && button != 1) {
            return false;
        }

        List<ConfigEntry> configs = ConfigManager.all();
        float listY = top + 50.0f;
        for (int i = 0; i < configs.size(); i++) {
            ConfigEntry config = configs.get(i);
            float cx = x + (i % Style.COLUMNS) * Style.COLUMN_PITCH;
            float cy = listY + (i / Style.COLUMNS) * Style.ENTRY_PITCH;

            float hx = cx + Style.CARD_W - 36.0f;
            float hy = cy + 18.0f;
            if (button == 0 && Render2D.hovered(mx, my, hx - 4.0f, hy - 4.0f, 24.0f, 24.0f)) {
                ConfigManager.toggleFavourite(config);
                UiSounds.soft();
                return true;
            }
            float dx = cx + Style.CARD_W - 36.0f;
            float dy = cy + Style.ENTRY_H - 28.0f;
            if (button == 0 && Render2D.hovered(mx, my, dx - 4.0f, dy - 4.0f, 24.0f, 24.0f)) {
                ConfigManager.delete(config);
                UiSounds.soft();
                return true;
            }
            if (Render2D.hovered(mx, my, cx, cy, Style.CARD_W, Style.ENTRY_H)) {
                if (button == 1) {
                    this.renaming = config;
                    this.draftName = config.name();
                    this.nameFocused = true;
                    UiSounds.soft();
                    return true;
                }
                if (button == 0) {
                    ConfigManager.load(config);
                    UiSounds.click();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!this.nameFocused) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            commitName();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.nameFocused = false;
            this.renaming = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.draftName.isEmpty()) {
            this.draftName = this.draftName.substring(0, this.draftName.length() - 1);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char c) {
        if (!this.nameFocused) {
            return false;
        }
        if (!Character.isISOControl(c) && this.draftName.length() < 24) {
            this.draftName += c;
        }
        return true;
    }

    @Override
    public boolean capturesInput() {
        return this.nameFocused;
    }
}
