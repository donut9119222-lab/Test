package dev.hatek.client.ui.screen;

import dev.hatek.client.feature.theme.ThemeEntry;
import dev.hatek.client.feature.theme.ThemeManager;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.UiSounds;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThemesPage extends Page {
    private final Map<String, Anim> hover = new HashMap<>();
    private final Map<String, Anim> applied = new HashMap<>();

    private Anim hover(String name) {
        return this.hover.computeIfAbsent(name, k -> new Anim(0.0f, 17.0f));
    }

    private Anim applied(String name) {
        return this.applied.computeIfAbsent(name, k -> new Anim(0.0f, 14.0f));
    }

    @Override
    public float contentHeight() {
        int rows = (ThemeManager.all().size() + Style.COLUMNS - 1) / Style.COLUMNS;
        return Math.max(0, rows) * Style.ENTRY_PITCH - (rows > 0 ? Style.CARD_GAP : 0);
    }

    @Override
    public void render(GuiGraphicsExtractor gg, float x, float y, float scroll, double mx, double my) {
        List<ThemeEntry> themes = ThemeManager.all();
        float viewport = Style.contentViewportH();
        for (int i = 0; i < themes.size(); i++) {
            ThemeEntry theme = themes.get(i);
            float cx = x + (i % Style.COLUMNS) * Style.COLUMN_PITCH;
            float cy = y + (i / Style.COLUMNS) * Style.ENTRY_PITCH - scroll;
            if (cy > y + viewport || cy + Style.ENTRY_H < y) {
                continue;
            }
            renderCard(gg, theme, cx, cy, mx, my);
        }
    }

    private void renderCard(GuiGraphicsExtractor gg, ThemeEntry theme, float x, float y,
                            double mx, double my) {
        Anim hoverAnim = hover(theme.name());
        Anim appliedAnim = applied(theme.name());
        hoverAnim.to(Render2D.hovered(mx, my, x, y, Style.CARD_W, Style.ENTRY_H) ? 1.0f : 0.0f);
        appliedAnim.to(ThemeManager.isApplied(theme) ? 1.0f : 0.0f);
        float hot = hoverAnim.get();
        float on = appliedAnim.get();

        Render2D.round(gg, x, y, Style.CARD_W, Style.ENTRY_H, Style.CARD_R,
                Render2D.lerp(Style.WHITE_02, Style.white(0.055f), Math.max(hot, on * 0.55f)));

        Render2D.round(gg, x + 8.0f, y + 14.0f, 3.0f, Style.ENTRY_H - 28.0f, 1.5f,
                Render2D.withAlpha(theme.color(), 0.55f + on * 0.45f));

        float orb = 28.0f;
        float ox = x + Style.CARD_W - orb - 18.0f;
        float oy = y + (Style.ENTRY_H - orb) / 2.0f;
        Render2D.circle(gg, ox + 3.0f, oy + 3.0f, orb, Render2D.withAlpha(theme.color(), 0.22f));
        Render2D.circle(gg, ox, oy, orb, theme.color());
        if (on > 0.01f) {
            Render2D.circle(gg, ox + orb / 2.0f - 4.0f, oy + orb / 2.0f - 4.0f, 8.0f,
                    Render2D.withAlpha(Style.WHITE, on));
        }

        Fonts.label().draw(gg, theme.author(), x + 22.0f, y + 28.0f, Style.WHITE_25);
        Fonts.title().draw(gg, theme.name(), x + 22.0f, y + 50.0f, Style.WHITE);
        Fonts.label().draw(gg, theme.date(), x + 22.0f, y + 70.0f, Style.WHITE_25);

        if (hot > 0.05f) {
            Fonts.label().draw(gg, ThemeManager.hex(theme.color()), x + 22.0f, y + Style.ENTRY_H - 14.0f,
                    Render2D.withAlpha(Style.WHITE_45, hot));
        }
    }

    @Override
    public boolean mouseClicked(float x, float y, float scroll, double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        List<ThemeEntry> themes = ThemeManager.all();
        for (int i = 0; i < themes.size(); i++) {
            float cx = x + (i % Style.COLUMNS) * Style.COLUMN_PITCH;
            float cy = y + (i / Style.COLUMNS) * Style.ENTRY_PITCH - scroll;
            if (Render2D.hovered(mx, my, cx, cy, Style.CARD_W, Style.ENTRY_H)) {
                ThemeManager.apply(themes.get(i));
                UiSounds.click();
                return true;
            }
        }
        return false;
    }
}