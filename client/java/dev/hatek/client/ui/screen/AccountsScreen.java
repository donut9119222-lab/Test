package dev.hatek.client.ui.screen;

import dev.hatek.client.feature.account.Account;
import dev.hatek.client.feature.account.AccountManager;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AccountsScreen extends Screen {
    private static final float PANEL_W = 300.0f;
    private static final float PAD = 18.0f;
    private static final float ROW_H = 42.0f;
    private static final float ROW_GAP = 8.0f;
    private static final float FIELD_H = 34.0f;

    private final Screen parent;
    private final Anim openAnim = new Anim(0.0f, 12.0f);
    private final Anim slideAnim = new Anim(0.0f, 14.0f);

    private final StringBuilder nameBuf = new StringBuilder();
    private boolean fieldFocused;
    private float scroll;
    private float scrollTarget;
    private long lastFrame = System.nanoTime();
    private long boot = System.currentTimeMillis();

    private PlayerSkinWidget skinWidget;
    private UUID previewUuid = UUID.randomUUID();
    private String previewName = "Steve";

    private int draggingIndex = -1;
    private boolean dragMoved;
    private float dragStartY;
    private float dragCurrentY;
    private int dropTarget = -1;
    private float listTop;

    private static final class Dust {
        float x, y, z, vx, vy, size, life, max, alpha;
    }

    private final Dust[] particles = new Dust[64];
    private static final float[] PARALLAX = new float[2];

    public AccountsScreen(Screen parent) {
        super(Component.literal("Аккаунты"));
        this.parent = parent;
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Dust();
            resetDust(particles[i], true);
        }
    }

    private void resetDust(Dust p, boolean anyY) {
        float w = Math.max(1, Render2D.screenWidth());
        float h = Math.max(1, Render2D.screenHeight());
        ThreadLocalRandom r = ThreadLocalRandom.current();
        p.x = r.nextFloat() * w;
        p.y = anyY ? r.nextFloat() * h : h + r.nextFloat() * 30;
        p.z = r.nextFloat();
        p.vx = (r.nextFloat() - 0.5f) * 0.35f;
        p.vy = -(0.05f + r.nextFloat() * 0.2f);
        p.size = 0.7f + r.nextFloat() * 2.2f;
        p.max = 4.0f + r.nextFloat() * 8.0f;
        p.life = p.max * (0.3f + r.nextFloat() * 0.7f);
        p.alpha = 0.12f + r.nextFloat() * 0.45f;
    }

    @Override
    protected void init() {
        AccountManager.all();
        this.openAnim.snap(0.0f);
        this.openAnim.to(1.0f);
        this.slideAnim.snap(0.0f);
        this.slideAnim.to(1.0f);
        this.scroll = this.scrollTarget = 0.0f;
        this.fieldFocused = false;
        this.nameBuf.setLength(0);
        refreshPreview();
        refreshSkin();
    }

    private void refreshPreview() {
        String active = AccountManager.activeName();
        if (active != null && !active.isEmpty()) {
            this.previewName = active;
            this.previewUuid = Account.offlineId(active);
            return;
        }
        try {
            if (this.minecraft != null && this.minecraft.getUser() != null) {
                this.previewName = this.minecraft.getUser().getName();
                try {
                    this.previewUuid = this.minecraft.getUser().getProfileId();
                } catch (Throwable t) {
                    this.previewUuid = Account.offlineId(this.previewName);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void refreshSkin() {
        if (this.skinWidget != null) {
            try {
                this.removeWidget(this.skinWidget);
            } catch (Throwable ignored) {
            }
            this.skinWidget = null;
        }
        try {
            int sw = Math.max(140, (int) (this.width * 0.28f));
            int sh = Math.max(220, (int) (this.height * 0.55f));
            final UUID u = this.previewUuid;
            this.skinWidget = new PlayerSkinWidget(sw, sh, this.minecraft.getEntityModels(),
                    () -> DefaultPlayerSkin.get(u));
            this.skinWidget.setPosition((int) (this.width * 0.62f - sw * 0.5f), (int) (this.height * 0.22f));
            this.addRenderableWidget(this.skinWidget);
        } catch (Throwable t) {
            this.skinWidget = null;
        }
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
        this.slideAnim.to(1.0f);
        float open = Anim.easeOut(this.openAnim.get());
        float slide = Anim.easeOut(this.slideAnim.get());
        float breath = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - boot) / 950.0);

        this.scroll += (this.scrollTarget - this.scroll) * 0.22f;

        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        int w = Render2D.screenWidth();
        int h = Render2D.screenHeight();

        PARALLAX[0] += (Mth.clamp((float) ((mx / w - 0.5) * 50), -40, 40) - PARALLAX[0]) * 0.05f;
        PARALLAX[1] += (Mth.clamp((float) ((my / h - 0.5) * 30), -24, 24) - PARALLAX[1]) * 0.05f;

        for (Dust p : particles) {
            p.life -= dt;
            p.x += p.vx + PARALLAX[0] * 0.01f;
            p.y += p.vy;
            if (p.life <= 0 || p.y < -10) {
                resetDust(p, false);
            }
        }

        Render2D.begin(gg);
        Render2D.rect(gg, 0, 0, w, h, 0xFF040406);
        Render2D.circle(gg, w * 0.55f + PARALLAX[0], h * 0.4f + PARALLAX[1], 280,
                Render2D.withAlpha(Style.WHITE, 0.03f * open));
        Render2D.circle(gg, w * 0.62f, h * 0.35f, 120, Render2D.withAlpha(Style.accent(), 0.04f * open));

        for (Dust p : particles) {
            float t = p.life / p.max;
            float a = p.alpha * t * open;
            if (a < 0.02f) continue;
            Render2D.circle(gg, p.x, p.y, p.size, Render2D.withAlpha(Style.WHITE, a));
        }

        String brand = "AXILINE";
        float bw = Fonts.title().width(brand);
        Fonts.title().draw(gg, brand, (w - bw) * 0.5f, 28, Render2D.withAlpha(Style.WHITE, open));
        String sub = "account switcher";
        Fonts.label().draw(gg, sub, (w - Fonts.label().width(sub)) * 0.5f, 46,
                Render2D.withAlpha(Style.WHITE_45, open));

        float rise = (1.0f - open) * 22.0f;
        float px = -PANEL_W + PANEL_W * slide + 10.0f * (1.0f - slide);
        float py = 28.0f + rise;
        float ph = h - 56.0f;
        drawPanel(gg, px, py, PANEL_W, ph, open, mx, my, breath);
        drawPreview(gg, w, h, open, breath);
        drawHints(gg, w, h, open);

        if (this.draggingIndex >= 0 && this.draggingIndex < AccountManager.all().size()) {
            drawGhost(gg, mx, my, open);
        }

        Render2D.end(gg);

        if (this.skinWidget != null) {
            try {
                this.skinWidget.extractRenderState(gg, mouseX, mouseY, partialTick);
            } catch (Throwable ignored) {
            }
        }
    }

    private void drawPanel(GuiGraphicsExtractor gg, float x, float y, float w, float h,
                           float open, double mx, double my, float breath) {
        Render2D.round(gg, x + 3, y + 5, w, h, 20, Render2D.withAlpha(0xFF000000, 0.35f * open));
        Render2D.round(gg, x, y, w, h, 20, Render2D.withAlpha(Style.PANEL, open * 0.95f));
        Render2D.round(gg, x, y, w, h, 20, Render2D.withAlpha(Style.WHITE_04, open));
        Render2D.round(gg, x + 14, y + 1.5f, w - 28, 2.2f, 1.0f,
                Render2D.withAlpha(Style.accent(), 0.55f * open));

        float cy = y + PAD;
        Fonts.title().draw(gg, "АККАУНТЫ", x + PAD, cy + 14, Render2D.withAlpha(Style.WHITE, open));
        cy += 28;

        String cur = AccountManager.activeName();
        if (cur == null || cur.isEmpty()) cur = this.previewName;
        Render2D.round(gg, x + PAD, cy, w - PAD * 2, 50, 14, Render2D.withAlpha(Style.PANEL_RAISED, open));
        Render2D.round(gg, x + PAD, cy, w - PAD * 2, 50, 14, Render2D.withAlpha(Style.accent(), 0.2f * open));
        drawAvatar(gg, x + PAD + 12, cy + 10, 30, cur, open);
        Fonts.body().draw(gg, cur, x + PAD + 52, cy + 22, Render2D.withAlpha(Style.WHITE, open));
        Fonts.label().draw(gg, "текущий профиль", x + PAD + 52, cy + 38,
                Render2D.withAlpha(Style.accent(), open));
        cy += 62;

        Fonts.label().draw(gg, "НОВЫЙ АККАУНТ", x + PAD, cy + 10, Render2D.withAlpha(Style.WHITE_45, open));
        cy += 18;

        float fieldW = w - PAD * 2 - 80;
        boolean fieldHot = Render2D.hovered(mx, my, x + PAD, cy, fieldW, FIELD_H) || this.fieldFocused;
        Render2D.round(gg, x + PAD, cy, fieldW, FIELD_H, 11,
                this.fieldFocused ? Style.white(0.1f) : Style.WHITE_02);
        Render2D.round(gg, x + PAD, cy, fieldW, FIELD_H, 11,
                Render2D.withAlpha(Style.WHITE_04, fieldHot ? 1.0f : 0.5f));
        String shown = this.nameBuf.length() == 0
                ? (this.fieldFocused ? "|" : "никнейм...")
                : this.nameBuf + (this.fieldFocused && (System.currentTimeMillis() / 480) % 2 == 0 ? "|" : "");
        Fonts.body().draw(gg, shown, x + PAD + 12, cy + 22,
                Render2D.withAlpha(this.nameBuf.length() == 0 ? Style.WHITE_25 : Style.WHITE, open));

        float addX = x + w - PAD - 72;
        boolean addHot = Render2D.hovered(mx, my, addX, cy, 72, FIELD_H);
        Render2D.round(gg, addX, cy, 72, FIELD_H, 11,
                Render2D.withAlpha(Style.accent(), (addHot ? 0.95f : 0.75f) * open));
        Fonts.label().draw(gg, "Добавить", addX + 12, cy + 22, Render2D.withAlpha(Style.PANEL, open));
        cy += FIELD_H + 10;

        boolean rndHot = Render2D.hovered(mx, my, x + PAD, cy, w - PAD * 2, 24);
        if (rndHot) {
            Render2D.round(gg, x + PAD, cy, w - PAD * 2, 24, 8, Style.WHITE_02);
        }
        Fonts.label().draw(gg, "*  Случайный ник", x + PAD + 6, cy + 16,
                Render2D.withAlpha(rndHot ? Style.WHITE : Style.WHITE_45, open));
        cy += 36;

        this.listTop = cy;
        float listBot = y + h - PAD - 36;
        float listH = Math.max(40, listBot - listTop);
        Render2D.round(gg, x + 6, listTop - 4, w - 12, listH + 8, 12,
                Render2D.withAlpha(0xFF000000, 0.2f * open));

        List<Account> accounts = AccountManager.all();
        float ry = listTop - this.scroll;
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            if (ry + ROW_H > listTop - 8 && ry < listBot + 8) {
                if (i == this.draggingIndex) {
                    Render2D.round(gg, x + PAD, ry, w - PAD * 2, ROW_H, 12,
                            Render2D.withAlpha(Style.WHITE, 0.06f * open));
                } else {
                    drawRow(gg, acc, x + PAD, ry, w - PAD * 2, open, mx, my, i);
                }
            }
            ry += ROW_H + ROW_GAP;
        }

        if (accounts.isEmpty()) {
            Fonts.label().draw(gg, "Список пуст — добавь ник", x + PAD, listTop + 24,
                    Render2D.withAlpha(Style.WHITE_25, open));
        }

        String readout = "saved: " + accounts.size() + " · " + (cur == null ? "-" : cur);
        Fonts.label().draw(gg, readout, x + PAD, y + h - PAD - 28,
                Render2D.withAlpha(Style.WHITE_25, open));

        boolean clrHot = Render2D.hovered(mx, my, x + PAD, y + h - PAD - 20, 140, 18);
        Fonts.label().draw(gg, "Очистить список", x + PAD, y + h - PAD - 12,
                Render2D.withAlpha(clrHot ? Style.DANGER : Render2D.withAlpha(Style.DANGER, 0.55f), open));
    }

    private void drawRow(GuiGraphicsExtractor gg, Account acc, float x, float y, float w,
                         float open, double mx, double my, int index) {
        boolean hot = Render2D.hovered(mx, my, x, y, w, ROW_H);
        boolean sel = acc.name().equalsIgnoreCase(AccountManager.activeName());
        boolean fav = AccountManager.isFavorite(acc);

        int bg = sel ? Render2D.withAlpha(Style.accent(), 0.14f)
                : (hot ? Style.white(0.06f) : Style.WHITE_02);
        Render2D.round(gg, x, y, w, ROW_H, 12, Render2D.withAlpha(bg, open));
        if (sel || hot) {
            Render2D.round(gg, x + 2, y + 10, 2.5f, ROW_H - 20, 1.2f,
                    Render2D.withAlpha(Style.accent(), open));
        }
        drawAvatar(gg, x + 10, y + 6, 28, acc.name(), open);
        Fonts.body().draw(gg, acc.name(), x + 48, y + 18, Render2D.withAlpha(Style.WHITE, open));

        String status = sel ? "в игре" : (fav ? "закреплён" : (hot ? "клик / тащи" : ""));
        if (!status.isEmpty()) {
            int col = sel ? 0xFF5AE682 : (fav ? 0xFFFFD24A : Style.WHITE_25);
            Fonts.label().draw(gg, status, x + 48, y + 34, Render2D.withAlpha(col, open));
        }

        Fonts.label().draw(gg, fav ? "★" : "☆", x + w - 22, y + 26,
                Render2D.withAlpha(fav ? 0xFFFFD24A : (hot ? Style.WHITE : Style.WHITE_25), open));
    }

    private void drawAvatar(GuiGraphicsExtractor gg, float x, float y, float s, String name, float a) {
        Render2D.round(gg, x, y, s, s, 8, Render2D.withAlpha(Style.PANEL_RAISED, a));
        Render2D.round(gg, x, y, s, s, 8, Render2D.withAlpha(Style.accent(), 0.2f * a));
        String letter = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        float lw = Fonts.title().width(letter);
        Fonts.title().draw(gg, letter, x + (s - lw) * 0.5f, y + s * 0.68f,
                Render2D.withAlpha(Style.WHITE, a));
    }

    private void drawPreview(GuiGraphicsExtractor gg, int w, int h, float open, float breath) {
        float cx = w * 0.62f;
        String name = AccountManager.activeName();
        if (name == null || name.isEmpty()) name = this.previewName;
        float nameW = Fonts.title().width(name) + 36;
        Render2D.round(gg, cx - nameW * 0.5f, h * 0.18f, nameW, 34, 14,
                Render2D.withAlpha(Style.PANEL, open * 0.9f));
        Fonts.title().draw(gg, name, cx - Fonts.title().width(name) * 0.5f, h * 0.18f + 22,
                Render2D.withAlpha(Style.WHITE, open));

        float halo = 90 + 10 * breath;
        Render2D.circle(gg, cx - halo, h * 0.48f - halo, halo * 2,
                Render2D.withAlpha(Style.WHITE, 0.03f * open));
        if (this.skinWidget == null) {
            drawAvatar(gg, cx - 48, h * 0.42f, 96, name, open);
        }
    }

    private void drawHints(GuiGraphicsExtractor gg, int w, int h, float open) {
        float boxW = 220;
        float boxH = 110;
        float bx = w - boxW - 18;
        float by = h - boxH - 18;
        Render2D.round(gg, bx, by, boxW, boxH, 14, Render2D.withAlpha(Style.PANEL, open * 0.92f));
        Render2D.round(gg, bx, by, boxW, boxH, 14, Render2D.withAlpha(Style.WHITE_04, open));
        Fonts.body().draw(gg, "УПРАВЛЕНИЕ", bx + 14, by + 22, Render2D.withAlpha(Style.WHITE, open));
        float ty = by + 38;
        String[][] hints = {
                {"ЛКМ", "выбрать / тащить"},
                {"ПКМ", "удалить"},
                {"★", "закрепить"},
                {"СКРОЛЛ", "прокрутка"},
                {"ESC", "назад"},
        };
        for (String[] pair : hints) {
            Fonts.label().draw(gg, pair[0], bx + 14, ty, Render2D.withAlpha(Style.WHITE, open));
            Fonts.label().draw(gg, pair[1], bx + 70, ty, Render2D.withAlpha(Style.WHITE_45, open));
            ty += 14;
        }
    }

    private void drawGhost(GuiGraphicsExtractor gg, double mx, double my, float open) {
        List<Account> all = AccountManager.all();
        if (this.draggingIndex < 0 || this.draggingIndex >= all.size()) return;
        Account acc = all.get(this.draggingIndex);
        float rowW = PANEL_W - PAD * 2;
        float gy = Mth.clamp(this.dragCurrentY - ROW_H * 0.5f, 80, Render2D.screenHeight() - 80);
        float gx = 10 + PAD;
        Render2D.round(gg, gx, gy, rowW, ROW_H, 12, Render2D.withAlpha(Style.PANEL_RAISED, open));
        Render2D.round(gg, gx, gy, rowW, ROW_H, 12, Render2D.withAlpha(Style.WHITE, 0.2f * open));
        drawAvatar(gg, gx + 10, gy + 6, 28, acc.name(), open);
        Fonts.body().draw(gg, acc.name(), gx + 48, gy + 18, Render2D.withAlpha(Style.WHITE, open));
        Fonts.label().draw(gg, "перетаскивание", gx + 48, gy + 34, Render2D.withAlpha(Style.WHITE_45, open));
    }

    private void addAccount() {
        String n = this.nameBuf.toString().trim();
        if (n.isEmpty()) n = randomName();
        if (!Account.valid(n)) return;
        Account acc = AccountManager.add(n);
        if (acc != null) {
            AccountManager.login(acc);
            this.previewName = acc.name();
            this.previewUuid = acc.id();
            refreshSkin();
        }
        this.nameBuf.setLength(0);
        this.fieldFocused = false;
    }

    private String randomName() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] a = {"Ax", "Neo", "Sky", "Void", "Frost", "Nova", "Echo", "Ray", "Lux", "Zen"};
        String[] b = {"Play", "Craft", "Mine", "Pro", "Dev", "Fox", "Wolf", "Lynx", "Core", "Dash"};
        String name = a[rng.nextInt(a.length)] + b[rng.nextInt(b.length)] + rng.nextInt(10, 100);
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        int btn = e.button();

        float slide = Anim.easeOut(this.slideAnim.get());
        float px = -PANEL_W + PANEL_W * slide;
        float py = 28.0f;
        float fieldY = py + PAD + 28 + 62 + 18;

        float fieldW = PANEL_W - PAD * 2 - 80;
        if (btn == 0 && Render2D.hovered(mx, my, px + PAD, fieldY, fieldW, FIELD_H)) {
            this.fieldFocused = true;
            return true;
        }
        this.fieldFocused = false;

        if (btn == 0 && Render2D.hovered(mx, my, px + PANEL_W - PAD - 72, fieldY, 72, FIELD_H)) {
            addAccount();
            return true;
        }

        float rndY = fieldY + FIELD_H + 10;
        if (btn == 0 && Render2D.hovered(mx, my, px + PAD, rndY, PANEL_W - PAD * 2, 24)) {
            this.nameBuf.setLength(0);
            this.nameBuf.append(randomName());
            return true;
        }

        int h = Render2D.screenHeight();
        if (btn == 0 && Render2D.hovered(mx, my, px + PAD, h - 28 - PAD - 20, 140, 18)) {
            AccountManager.clear();
            return true;
        }

        List<Account> accounts = AccountManager.all();
        float ry = this.listTop - this.scroll;
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            float rx = px + PAD;
            float rw = PANEL_W - PAD * 2;
            if (Render2D.hovered(mx, my, rx, ry, rw, ROW_H)) {
                if (btn == 0 && mx >= rx + rw - 36) {
                    AccountManager.toggleFavorite(acc);
                    return true;
                }
                if (btn == 1) {
                    AccountManager.remove(acc);
                    refreshPreview();
                    refreshSkin();
                    return true;
                }
                if (btn == 0) {
                    this.draggingIndex = i;
                    this.dragStartY = (float) my;
                    this.dragCurrentY = (float) my;
                    this.dragMoved = false;
                    this.dropTarget = i;
                    return true;
                }
            }
            ry += ROW_H + ROW_GAP;
        }
        return super.mouseClicked(e, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingIndex >= 0) {
            this.dragCurrentY = (float) Render2D.mouseY();
            if (Math.abs(this.dragCurrentY - this.dragStartY) > 4) this.dragMoved = true;
            int target = (int) Math.round((this.dragCurrentY - (this.listTop - this.scroll)) / (ROW_H + ROW_GAP));
            this.dropTarget = Mth.clamp(target, 0, AccountManager.all().size());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingIndex >= 0) {
            List<Account> all = AccountManager.all();
            if (this.dragMoved) {
                int from = this.draggingIndex;
                int to = this.dropTarget;
                if (to > from) to--;
                to = Math.max(0, Math.min(to, all.size() - 1));
                if (to != from) AccountManager.move(from, to);
            } else if (this.draggingIndex < all.size()) {
                Account acc = all.get(this.draggingIndex);
                AccountManager.login(acc);
                this.previewName = acc.name();
                this.previewUuid = acc.id();
                refreshSkin();
            }
            this.draggingIndex = -1;
            this.dragMoved = false;
            this.dropTarget = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        float max = Math.max(0, AccountManager.all().size() * (ROW_H + ROW_GAP) - 200);
        this.scrollTarget = Mth.clamp(this.scrollTarget - (float) v * 32, 0, max);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.gui.setScreen(this.parent != null ? this.parent : new MainScreen());
            return true;
        }
        if (this.fieldFocused) {
            if (e.key() == GLFW.GLFW_KEY_BACKSPACE && this.nameBuf.length() > 0) {
                this.nameBuf.deleteCharAt(this.nameBuf.length() - 1);
                return true;
            }
            if (e.key() == GLFW.GLFW_KEY_ENTER) {
                addAccount();
                return true;
            }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (this.fieldFocused) {
            char c = e.codepoint() > 0 ? (char) e.codepoint() : 0;
            if (c >= 32 && c < 127 && this.nameBuf.length() < Account.MAX_NAME) {
                this.nameBuf.append(c);
                return true;
            }
        }
        return super.charTyped(e);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent != null ? this.parent : new MainScreen());
    }
}