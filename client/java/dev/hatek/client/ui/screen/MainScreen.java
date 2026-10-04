package dev.hatek.client.ui.screen;

import dev.hatek.client.feature.account.AccountManager;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Axiline main menu — ported from aethereal MainScreen to Hatek Render2D.
 */
public final class MainScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] UPDATES = {
            "03.10 | GUI | Новый ClickGUI в стиле iOS",
            "28.09 | Combat | AttackAura / AutoExplosion",
            "24.09 | Core | Старт Axiline 26.2",
    };

    private static final float CARD_W = 210.0f;
    private static final float CARD_H = 248.0f;
    private static final float BTN_W = 178.0f;
    private static final float PAD = 16.0f;

    private static final float[] PARALLAX = new float[2];

    private final Anim openAnim = new Anim(0.0f, 12.0f);
    private final Anim sideAnim = new Anim(0.0f, 14.0f);

    private final MenuBtn[] buttons = {
            new MenuBtn("Одиночный режим", 40.0f),
            new MenuBtn("Сетевая игра", 40.0f),
            new MenuBtn("Аккаунты", 34.0f),
            new MenuBtn("Настройки", 32.0f),
    };

    private static final class Dust {
        float x, y, z, vx, vy, size, life, max, alpha;
        boolean glow;
    }

    private final Dust[] particles = new Dust[72];
    private final long boot = System.currentTimeMillis();
    private long lastFrame = System.nanoTime();

    private float cardX, cardY;
    private float quitX, quitY, quitW = 140.0f, quitH = 36.0f;
    private float slideProgress;
    private float slideDrag = -1.0f;
    private float sideTarget;

    private static final class MenuBtn {
        final String label;
        final float h;
        float x, y;
        final Anim hover = new Anim(0.0f, 16.0f);

        MenuBtn(String label, float h) {
            this.label = label;
            this.h = h;
        }
    }

    public MainScreen() {
        super(Component.literal("Axiline"));
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Dust();
            resetParticle(particles[i], true);
        }
    }

    private void resetParticle(Dust p, boolean randomY) {
        float w = Render2D.screenWidth();
        float h = Render2D.screenHeight();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        p.x = r.nextFloat() * Math.max(1, w);
        p.y = randomY ? r.nextFloat() * Math.max(1, h) : h + r.nextFloat() * 40;
        p.z = r.nextFloat();
        p.vx = (r.nextFloat() - 0.5f) * (0.12f + p.z * 0.4f);
        p.vy = -(0.04f + r.nextFloat() * (0.12f + p.z * 0.4f));
        p.size = 0.6f + r.nextFloat() * (1.0f + p.z * 2.6f);
        p.max = 5.0f + r.nextFloat() * 10.0f;
        p.life = p.max * (0.2f + r.nextFloat() * 0.8f);
        p.alpha = 0.12f + r.nextFloat() * 0.5f;
        p.glow = p.z > 0.55f && r.nextBoolean();
    }

    @Override
    protected void init() {
        this.openAnim.snap(0.0f);
        this.openAnim.to(1.0f);
        this.sideAnim.snap(0.0f);
        this.slideProgress = 0.0f;
        this.slideDrag = -1.0f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - this.lastFrame) / 1_000_000_000.0f);
        this.lastFrame = now;
        Anim.beginFrame(dt);

        this.openAnim.to(1.0f);
        float open = Anim.easeOut(this.openAnim.get());
        float rise = (1.0f - open) * 28.0f;
        float breath = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - boot) / 1050.0);

        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        int w = Render2D.screenWidth();
        int h = Render2D.screenHeight();

        float marginX = w * 0.04f;
        float marginY = h * 0.04f;
        PARALLAX[0] += (Mth.clamp((float) ((mx / w - 0.5) * 2.0 * marginX), -marginX, marginX) - PARALLAX[0]) * 0.05f;
        PARALLAX[1] += (Mth.clamp((float) ((my / h - 0.5) * 2.0 * marginY), -marginY, marginY) - PARALLAX[1]) * 0.05f;

        this.sideTarget = mx < 40 ? 1.0f : (mx > 240 ? 0.0f : this.sideTarget);
        this.sideAnim.to(this.sideTarget);

        layout(w, h, rise);
        tickParticles(dt, w, h);

        Render2D.begin(gg);

        // background
        Render2D.rect(gg, 0, 0, w, h, 0xFF040406);
        drawDepth(gg, w, h, open);
        drawParticles(gg, open, false);

        // fade-in veil
        if (open < 0.998f) {
            Render2D.rect(gg, 0, 0, w, h, Render2D.withAlpha(0xFF000000, (1.0f - open) * 0.82f));
        }

        drawStatusIsland(gg, w, open, breath);
        drawBrand(gg, w * 0.5f, h * 0.14f + (1.0f - open) * 18.0f, open, breath);
        drawActionCard(gg, open);
        drawButtons(gg, mx, my, open);
        drawParticles(gg, open * 0.55f, true);
        drawQuit(gg, mx, my, open);
        drawSidePanel(gg, h, open, Anim.easeOut(this.sideAnim.get()));

        // edge hint when panel closed
        if (this.sideAnim.get() < 0.15f) {
            Render2D.round(gg, 0, h * 0.32f, 2.5f, h * 0.36f, 1.2f,
                    Render2D.withAlpha(Style.WHITE, (0.25f + 0.25f * breath) * open));
        }

        Render2D.end(gg);
    }

    private void layout(int width, int height, float rise) {
        cardX = (width - CARD_W) * 0.5f;
        cardY = (height - CARD_H) * 0.46f + rise;

        float x = cardX + PAD;
        float y = cardY + 30.0f;
        float gap = 9.0f;
        for (MenuBtn b : buttons) {
            b.x = x;
            b.y = y;
            y += b.h + gap;
        }

        quitW = 140.0f;
        quitH = 36.0f;
        quitX = (width - quitW) * 0.5f;
        quitY = height * 0.875f;
    }

    private void drawDepth(GuiGraphicsExtractor gg, int w, int h, float open) {
        float cx = w * 0.5f + PARALLAX[0] * 0.4f;
        float cy = h * 0.36f + PARALLAX[1] * 0.35f;
        Render2D.circle(gg, cx - 160, cy - 100, 320, Render2D.withAlpha(Style.WHITE, 0.02f * open));
        Render2D.circle(gg, cx - 90 + PARALLAX[0] * 0.1f, cy - 60, 180, Render2D.withAlpha(Style.WHITE, 0.03f * open));
        Render2D.circle(gg, cx - 40 + PARALLAX[0] * 0.25f, cy - 28, 80, Render2D.withAlpha(Style.WHITE, 0.04f * open));

        for (int i = 0; i < 8; i++) {
            float t = i / 7.0f;
            float y = h * 0.52f + t * h * 0.42f;
            float inset = 30 + t * t * 160;
            Render2D.rect(gg, inset + PARALLAX[0] * (0.15f + t * 0.3f), y + PARALLAX[1] * 0.08f,
                    w - inset * 2, 1.0f, Render2D.withAlpha(Style.WHITE, (0.025f + t * 0.04f) * open));
        }
    }

    private void tickParticles(float dt, int w, int h) {
        for (Dust p : particles) {
            p.life -= dt;
            p.x += p.vx * (0.35f + p.z) + PARALLAX[0] * 0.01f * p.z;
            p.y += p.vy * (0.35f + p.z);
            if (p.life <= 0 || p.y < -12 || p.x < -12 || p.x > w + 12) {
                resetParticle(p, false);
            }
        }
    }

    private void drawParticles(GuiGraphicsExtractor gg, float globalA, boolean front) {
        for (Dust p : particles) {
            if (front && p.z < 0.55f) continue;
            if (!front && p.z >= 0.78f) continue;
            float t = p.life / p.max;
            float a = p.alpha * t * globalA * (0.3f + p.z);
            if (a < 0.02f) continue;
            float s = p.size * (0.6f + 0.55f * p.z) * (0.7f + 0.3f * t);
            if (p.glow) {
                Render2D.circle(gg, p.x - s * 1.2f, p.y - s * 1.2f, s * 2.4f,
                        Render2D.withAlpha(Style.WHITE, a * 0.15f));
            }
            Render2D.circle(gg, p.x - s * 0.5f, p.y - s * 0.5f, s,
                    Render2D.withAlpha(Style.WHITE, a));
        }
    }

    private void drawStatusIsland(GuiGraphicsExtractor gg, int width, float open, float breath) {
        String time = LocalDateTime.now().format(CLOCK);
        String nick = "Player";
        try {
            if (this.minecraft != null && this.minecraft.getUser() != null) {
                nick = this.minecraft.getUser().getName();
            }
        } catch (Throwable ignored) {
        }
        String active = AccountManager.activeName();
        if (active != null && !active.isEmpty()) {
            nick = active;
        }

        float iw = 190.0f;
        float ih = 32.0f;
        float ix = (width - iw) * 0.5f;
        float iy = 12.0f + (1.0f - open) * -18.0f;

        Render2D.round(gg, ix, iy, iw, ih, 16.0f, Render2D.withAlpha(Style.PANEL, open * 0.95f));
        Render2D.round(gg, ix, iy, iw, ih, 16.0f, Render2D.withAlpha(Style.WHITE_04, open));
        Render2D.circle(gg, ix + 12, iy + 10, 10,
                Render2D.withAlpha(Style.WHITE, (0.45f + 0.3f * breath) * open));
        Fonts.body().draw(gg, nick, ix + 28, iy + 20, Render2D.withAlpha(Style.WHITE, open * 0.95f));
        float tw = Fonts.label().width(time);
        Fonts.label().draw(gg, time, ix + iw - 14 - tw, iy + 20, Render2D.withAlpha(Style.WHITE_45, open));
    }

    private void drawBrand(GuiGraphicsExtractor gg, float cx, float ty, float open, float breath) {
        String brand = "AXILINE";
        float scale = 0.9f + 0.1f * open;
        float tw = Fonts.title().width(brand) * 1.6f;
        Render2D.pushScale(gg, cx, ty + 12, scale);
        Render2D.round(gg, cx - tw * 0.55f, ty - 2, tw * 1.1f, 28, 12,
                Render2D.withAlpha(Style.WHITE, (0.04f + 0.04f * breath) * open));
        // approximate title size by drawing twice for weight
        Fonts.title().draw(gg, brand, cx - Fonts.title().width(brand) / 2.0f, ty + 14,
                Render2D.withAlpha(Style.WHITE, open));
        String sub = "CLIENT  ·  26.2";
        float sw = Fonts.label().width(sub);
        Fonts.label().draw(gg, sub, cx - sw / 2.0f, ty + 30, Render2D.withAlpha(Style.WHITE_45, open));
        float lw = 28 + 14 * breath;
        Render2D.round(gg, cx - lw * 0.5f, ty + 38, lw, 2.0f, 1.0f,
                Render2D.withAlpha(Style.WHITE, 0.4f * open));
        Render2D.popTransform(gg);
    }

    private void drawActionCard(GuiGraphicsExtractor gg, float open) {
        Render2D.round(gg, cardX, cardY, CARD_W, CARD_H, 22.0f,
                Render2D.withAlpha(Style.PANEL, open * 0.88f));
        Render2D.round(gg, cardX, cardY, CARD_W, CARD_H, 22.0f,
                Render2D.withAlpha(Style.WHITE_04, open));
        Render2D.round(gg, cardX + 16, cardY + 1.5f, CARD_W - 32, 1.2f, 0.6f,
                Render2D.withAlpha(Style.WHITE, 0.12f * open));
        Fonts.label().draw(gg, "МЕНЮ", cardX + 18, cardY + 18,
                Render2D.withAlpha(Style.WHITE_25, open));
    }

    private void drawButtons(GuiGraphicsExtractor gg, double mx, double my, float open) {
        for (int i = 0; i < buttons.length; i++) {
            MenuBtn b = buttons[i];
            boolean hot = Render2D.hovered(mx, my, b.x, b.y, BTN_W, b.h);
            b.hover.to(hot ? 1.0f : 0.0f);
            float k = b.hover.get();
            int bg = Render2D.lerp(Style.WHITE_02, Style.white(0.08f), k);
            if (i == 0) {
                bg = Render2D.lerp(Render2D.withAlpha(Style.accent(), 0.18f),
                        Render2D.withAlpha(Style.accent(), 0.32f), k);
            }
            Render2D.round(gg, b.x, b.y, BTN_W, b.h, 12.0f, Render2D.withAlpha(bg, open));
            int fg = i == 0 ? Style.WHITE : Render2D.lerp(Style.WHITE_45, Style.WHITE, k);
            Fonts.body().draw(gg, b.label, b.x + 14, b.y + b.h * 0.62f,
                    Render2D.withAlpha(fg, open));
        }
    }

    private void drawQuit(GuiGraphicsExtractor gg, double mx, double my, float open) {
        boolean hover = Render2D.hovered(mx, my, quitX, quitY, quitW, quitH);
        float target = 0.0f;
        if (slideDrag >= 0) {
            target = Mth.clamp((float) ((mx - slideDrag - quitX - 6) / (quitW - 36)), 0.0f, 1.0f);
        } else if (hover) {
            target = 0.06f;
        }
        slideProgress += (target - slideProgress) * (slideDrag >= 0 ? 0.32f : 0.14f);

        Render2D.round(gg, quitX, quitY, quitW, quitH, quitH * 0.5f,
                Render2D.withAlpha(Style.PANEL_RAISED, open * 0.94f));
        Render2D.round(gg, quitX, quitY, quitW, quitH, quitH * 0.5f,
                Render2D.withAlpha(Style.WHITE_04, open));

        if (slideProgress > 0.02f) {
            float inset = 3.0f;
            float fh = quitH - inset * 2;
            float fr = fh * 0.5f;
            float fw = Math.max(fr * 2, (quitW - inset * 2) * slideProgress);
            Render2D.round(gg, quitX + inset, quitY + inset, fw, fh, fr,
                    Render2D.withAlpha(Style.DANGER, open * (0.25f + 0.45f * slideProgress)));
        }

        String label = slideProgress > 0.75f ? "отпустите" : "удерживайте · выйти";
        float lw = Fonts.label().width(label);
        Fonts.label().draw(gg, label, quitX + (quitW - lw) * 0.5f, quitY + 22,
                Render2D.withAlpha(Style.WHITE, (0.65f + 0.25f * slideProgress) * open));
    }

    private void drawSidePanel(GuiGraphicsExtractor gg, int h, float open, float t) {
        if (t < 0.01f) {
            return;
        }
        float panelW = 210.0f;
        float x = -panelW + panelW * t;
        float y = 48.0f;
        float ph = h - 96.0f;

        Render2D.round(gg, x, y, panelW, ph, 18.0f, Render2D.withAlpha(Style.PANEL, open * t * 0.95f));
        Render2D.round(gg, x, y, panelW, ph, 18.0f, Render2D.withAlpha(Style.WHITE_04, open * t));
        Fonts.title().draw(gg, "Обновления", x + 16, y + 28, Render2D.withAlpha(Style.WHITE, open * t));
        Fonts.label().draw(gg, "Axiline changelog", x + 16, y + 44, Render2D.withAlpha(Style.WHITE_45, open * t));

        float rowY = y + 60;
        for (String raw : UPDATES) {
            if (rowY > y + ph - 40) break;
            String[] p = raw.split("\\s*\\|\\s*", 3);
            String date = p.length > 0 ? p[0].trim() : "";
            String title = p.length > 1 ? p[1].trim() : raw;
            String desc = p.length > 2 ? p[2].trim() : "";
            Render2D.round(gg, x + 12, rowY, panelW - 24, 44, 10,
                    Render2D.withAlpha(Style.PANEL_RAISED, open * t * 0.9f));
            Fonts.label().draw(gg, date, x + 20, rowY + 14, Render2D.withAlpha(Style.WHITE_25, open * t));
            Fonts.body().draw(gg, title, x + 20, rowY + 28, Render2D.withAlpha(Style.WHITE, open * t));
            if (!desc.isEmpty()) {
                Fonts.label().draw(gg, desc, x + 20, rowY + 40, Render2D.withAlpha(Style.WHITE_45, open * t));
            }
            rowY += 52;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        int button = event.button();

        if (button == 0 && Render2D.hovered(mx, my, quitX, quitY, quitW, quitH)) {
            slideDrag = (float) mx - quitX;
            return true;
        }

        if (button == 0) {
            for (int i = 0; i < buttons.length; i++) {
                MenuBtn b = buttons[i];
                if (Render2D.hovered(mx, my, b.x, b.y, BTN_W, b.h)) {
                    switch (i) {
                        case 0 -> this.minecraft.gui.setScreen(new SelectWorldScreen(this));
                        case 1 -> this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                        case 2 -> this.minecraft.gui.setScreen(new AccountsScreen(this));
                        case 3 -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false));
                        default -> {
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (slideDrag >= 0) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (slideDrag < 0) {
            return super.mouseReleased(event);
        }
        slideDrag = -1.0f;
        if (slideProgress >= 0.9f) {
            this.minecraft.stop();
        }
        return true;
    }

    @Override
    public void onClose() {
        // stay on main menu
    }
}
