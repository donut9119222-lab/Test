package dev.hatek.client.ui.screen;

import dev.hatek.client.ui.GuiPrefs;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.UiSounds;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

public final class SettingsPage extends Page {
    private final Anim hoverMaster = new Anim(0.0f, 16.0f);
    private final Map<UiSounds.Mode, Anim> chipHover = new EnumMap<>(UiSounds.Mode.class);
    private boolean dragVolume;
    private boolean dragCount;
    private boolean dragSpeed;
    private boolean dragOpacity;

    public SettingsPage() {
        for (UiSounds.Mode mode : UiSounds.Mode.values()) {
            this.chipHover.put(mode, new Anim(0.0f, 16.0f));
        }
    }

    @Override
    public float contentHeight() {
        return 430.0f;
    }

    @Override
    public void render(GuiGraphicsExtractor gg, float x, float y, float scroll, double mx, double my) {
        float cardX = x;
        float cardY = y - scroll;
        float cardW = Style.CARD_W * 1.55f;
        float cursor = cardY;

        // —— Sounds master ——
        float masterH = 64.0f;
        boolean hover = Render2D.hovered(mx, my, cardX, cursor, cardW, masterH);
        this.hoverMaster.to(hover ? 1.0f : 0.0f);
        Render2D.round(gg, cardX, cursor, cardW, masterH, Style.CARD_R,
                Render2D.lerp(Style.WHITE_02, Style.white(0.05f), this.hoverMaster.get()));
        Fonts.title().draw(gg, "Звуки", cardX + 16.0f, cursor + 22.0f, Style.WHITE);
        Fonts.label().draw(gg, "Клики интерфейса", cardX + 16.0f, cursor + 42.0f, Style.WHITE_45);
        drawToggle(gg, cardX + cardW - Style.TOGGLE_W - 18.0f,
                cursor + (masterH - Style.TOGGLE_H) / 2.0f, UiSounds.enabled());
        cursor += masterH + 12.0f;

        // —— Volume ——
        float volH = 72.0f;
        Render2D.round(gg, cardX, cursor, cardW, volH, Style.CARD_R, Style.WHITE_02);
        Fonts.body().draw(gg, "Громкость", cardX + 16.0f, cursor + 24.0f, Style.WHITE_45);
        String volTxt = Math.round(GuiPrefs.soundVolume() * 100.0f) + "%";
        Fonts.label().draw(gg, volTxt, cardX + cardW - 16.0f - Fonts.label().width(volTxt),
                cursor + 24.0f, Style.accent());
        drawSlider(gg, cardX + 16.0f, cursor + 42.0f, cardW - 32.0f, GuiPrefs.soundVolume(), mx, my);
        cursor += volH + 12.0f;

        // —— Sound chips ——
        float soundH = 120.0f;
        Render2D.round(gg, cardX, cursor, cardW, soundH, Style.CARD_R, Style.WHITE_02);
        Fonts.body().draw(gg, "Звук клика", cardX + 16.0f, cursor + 24.0f, Style.WHITE_45);
        float chipX = cardX + 16.0f;
        float chipY = cursor + 42.0f;
        float chipH = 28.0f;
        for (UiSounds.Mode mode : UiSounds.Mode.values()) {
            String label = mode.label();
            float chipW = Fonts.label().width(label) + 22.0f;
            if (chipX + chipW > cardX + cardW - 16.0f) {
                chipX = cardX + 16.0f;
                chipY += chipH + 8.0f;
            }
            boolean on = UiSounds.mode() == mode;
            boolean ch = Render2D.hovered(mx, my, chipX, chipY, chipW, chipH);
            Anim anim = this.chipHover.get(mode);
            anim.to(on || ch ? 1.0f : 0.0f);
            float a = anim.get();
            int bg = on ? Render2D.withAlpha(Style.accent(), 0.88f)
                    : Render2D.lerp(Style.WHITE_04, Style.white(0.08f), a);
            int fg = on ? Style.WHITE : Render2D.lerp(Style.WHITE_45, Style.WHITE, a);
            Render2D.round(gg, chipX, chipY, chipW, chipH, 10.0f, bg);
            Fonts.label().draw(gg, label, chipX + 11.0f, chipY + 18.0f, fg);
            chipX += chipW + 8.0f;
        }
        cursor += soundH + 14.0f;

        // —— Particles ——
        float partH = 150.0f;
        Render2D.round(gg, cardX, cursor, cardW, partH, Style.CARD_R, Style.WHITE_02);
        Fonts.title().draw(gg, "Фон", cardX + 16.0f, cursor + 24.0f, Style.WHITE);
        Fonts.label().draw(gg, "Частицы при открытом GUI", cardX + 16.0f, cursor + 42.0f, Style.WHITE_45);
        drawToggle(gg, cardX + cardW - Style.TOGGLE_W - 18.0f, cursor + 20.0f, GuiPrefs.particles());

        float labelCol = 108.0f;
        float trackX = cardX + 16.0f + labelCol;
        float trackW = cardW - 32.0f - labelCol;
        float sy = cursor + 58.0f;
        Fonts.label().draw(gg, "Кол-во", cardX + 16.0f, sy + 10.0f, Style.WHITE_45);
        drawSlider(gg, trackX, sy + 4.0f, trackW,
                (GuiPrefs.particleCount() - 8) / 112.0f, mx, my);

        sy += 34.0f;
        Fonts.label().draw(gg, "Скорость", cardX + 16.0f, sy + 10.0f, Style.WHITE_45);
        drawSlider(gg, trackX, sy + 4.0f, trackW,
                (GuiPrefs.particleSpeed() - 0.1f) / 1.9f, mx, my);

        sy += 34.0f;
        Fonts.label().draw(gg, "Прозрачн.", cardX + 16.0f, sy + 10.0f, Style.WHITE_45);
        drawSlider(gg, trackX, sy + 4.0f, trackW, GuiPrefs.particleOpacity(), mx, my);
    }

    private void drawToggle(GuiGraphicsExtractor gg, float tx, float ty, boolean on) {
        float k = on ? 1.0f : 0.0f;
        Render2D.round(gg, tx, ty, Style.TOGGLE_W, Style.TOGGLE_H, Style.TOGGLE_R,
                Render2D.lerp(Style.WHITE_04, Style.accent(), k));
        float travel = Style.TOGGLE_W - Style.KNOB - Style.KNOB_INSET * 2;
        Render2D.circle(gg, tx + Style.KNOB_INSET + travel * k, ty + Style.KNOB_INSET,
                Style.KNOB, Style.WHITE);
    }

    private void drawSlider(GuiGraphicsExtractor gg, float x, float y, float w, float frac,
                            double mx, double my) {
        frac = Mth.clamp(frac, 0.0f, 1.0f);
        float h = 4.0f;
        Render2D.round(gg, x, y, w, h, 2.0f, Style.WHITE_04);
        Render2D.round(gg, x, y, w * frac, h, 2.0f, Style.accent());
        float kx = x + w * frac - 5.0f;
        Render2D.circle(gg, kx, y - 3.0f, 10.0f, Style.WHITE);
    }

    private boolean hitSlider(double mx, double my, float x, float y, float w) {
        return Render2D.hovered(mx, my, x - 4.0f, y - 8.0f, w + 8.0f, 20.0f);
    }

    private float sliderFrac(double mx, float x, float w) {
        return Mth.clamp((float) ((mx - x) / w), 0.0f, 1.0f);
    }

    @Override
    public boolean mouseClicked(float x, float y, float scroll, double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        float cardX = x;
        float cardY = y - scroll;
        float cardW = Style.CARD_W * 1.55f;
        float cursor = cardY;

        float masterH = 64.0f;
        if (Render2D.hovered(mx, my, cardX, cursor, cardW, masterH)) {
            UiSounds.enabled(!UiSounds.enabled());
            if (UiSounds.enabled()) {
                UiSounds.click();
            }
            return true;
        }
        cursor += masterH + 12.0f;

        float volH = 72.0f;
        float volTrackX = cardX + 16.0f;
        float volTrackY = cursor + 42.0f;
        float volTrackW = cardW - 32.0f;
        if (hitSlider(mx, my, volTrackX, volTrackY, volTrackW)) {
            this.dragVolume = true;
            GuiPrefs.soundVolume(sliderFrac(mx, volTrackX, volTrackW));
            UiSounds.click();
            return true;
        }
        cursor += volH + 12.0f;

        float soundH = 120.0f;
        float chipX = cardX + 16.0f;
        float chipY = cursor + 42.0f;
        float chipH = 28.0f;
        for (UiSounds.Mode mode : UiSounds.Mode.values()) {
            String label = mode.label();
            float chipW = Fonts.label().width(label) + 22.0f;
            if (chipX + chipW > cardX + cardW - 16.0f) {
                chipX = cardX + 16.0f;
                chipY += chipH + 8.0f;
            }
            if (Render2D.hovered(mx, my, chipX, chipY, chipW, chipH)) {
                UiSounds.mode(mode);
                UiSounds.enabled(true);
                UiSounds.click();
                return true;
            }
            chipX += chipW + 8.0f;
        }
        cursor += soundH + 14.0f;

        float partH = 150.0f;
        if (Render2D.hovered(mx, my, cardX + cardW - Style.TOGGLE_W - 28.0f, cursor + 12.0f,
                Style.TOGGLE_W + 20.0f, 36.0f)) {
            GuiPrefs.particles(!GuiPrefs.particles());
            UiSounds.soft();
            return true;
        }

        float labelCol = 108.0f;
        float trackX = cardX + 16.0f + labelCol;
        float trackW = cardW - 32.0f - labelCol;
        float sy = cursor + 58.0f + 4.0f;
        if (hitSlider(mx, my, trackX, sy, trackW)) {
            this.dragCount = true;
            GuiPrefs.particleCount(8 + Math.round(sliderFrac(mx, trackX, trackW) * 112.0f));
            return true;
        }
        sy += 34.0f;
        if (hitSlider(mx, my, trackX, sy, trackW)) {
            this.dragSpeed = true;
            GuiPrefs.particleSpeed(0.1f + sliderFrac(mx, trackX, trackW) * 1.9f);
            return true;
        }
        sy += 34.0f;
        if (hitSlider(mx, my, trackX, sy, trackW)) {
            this.dragOpacity = true;
            GuiPrefs.particleOpacity(sliderFrac(mx, trackX, trackW));
            return true;
        }
        return false;
    }

    @Override
    public void mouseDragged(float x, float y, float scroll, double mx, double my) {
        float cardX = x;
        float cardY = y - scroll;
        float cardW = Style.CARD_W * 1.55f;
        float cursor = cardY + 64.0f + 12.0f;

        if (this.dragVolume) {
            GuiPrefs.soundVolume(sliderFrac(mx, cardX + 16.0f, cardW - 32.0f));
            return;
        }
        cursor += 72.0f + 12.0f + 120.0f + 14.0f;
        float labelCol = 108.0f;
        float trackX = cardX + 16.0f + labelCol;
        float trackW = cardW - 32.0f - labelCol;
        if (this.dragCount) {
            GuiPrefs.particleCount(8 + Math.round(sliderFrac(mx, trackX, trackW) * 112.0f));
        } else if (this.dragSpeed) {
            GuiPrefs.particleSpeed(0.1f + sliderFrac(mx, trackX, trackW) * 1.9f);
        } else if (this.dragOpacity) {
            GuiPrefs.particleOpacity(sliderFrac(mx, trackX, trackW));
        }
    }

    @Override
    public void mouseReleased() {
        this.dragVolume = false;
        this.dragCount = false;
        this.dragSpeed = false;
        this.dragOpacity = false;
    }
}
