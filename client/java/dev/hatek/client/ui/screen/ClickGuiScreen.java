package dev.hatek.client.ui.screen;

import dev.hatek.client.feature.config.ConfigManager;
import dev.hatek.client.feature.theme.ThemeManager;
import dev.hatek.client.module.Category;
import dev.hatek.client.ui.GuiPrefs;
import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.UiSounds;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("ru"));

    private static float panelX = Float.NaN;
    private static float panelY = Float.NaN;
    private static Tab tab = Tab.MODULES;
    private static final Map<String, Float> SCROLL = new HashMap<>();

    private static final ModulesPage MODULES_PAGE = new ModulesPage();
    private static final ThemesPage THEMES_PAGE = new ThemesPage();
    private static final ConfigsPage CONFIGS_PAGE = new ConfigsPage();
    private static final SettingsPage SETTINGS_PAGE = new SettingsPage();

    private static final String[] SOURCE_ICONS = {"folder", "newfile", "cloud"};

    private final Anim openAnim = new Anim(0.0f, 14.0f);
    private final Anim switchAnim = new Anim(1.0f, 14.0f);
    private final Anim selectorAnim = new Anim(0.0f, 18.0f);
    private final Anim navAnim = new Anim(0.0f, 18.0f);
    private final Anim scrollAnim = new Anim(0.0f, 18.0f);
    private final Anim scrollbarAnim = new Anim(0.0f, 10.0f);
    private final Anim sourceAnim = new Anim(0.0f, 14.0f);
    private final Map<Category, Anim> tabHover = new EnumMap<>(Category.class);
    private final Map<Tab, Anim> navHover = new EnumMap<>(Tab.class);
    private final Anim[] sourceHover = {new Anim(0.0f, 18.0f), new Anim(0.0f, 18.0f), new Anim(0.0f, 18.0f)};

    private boolean closing;
    private boolean draggingPanel;
    private boolean searchFocused;
    private String search = "";
    private float grabX;
    private float grabY;
    private long lastFrame = System.nanoTime();
    private long lastScroll;

    private static final class Dust {
        float x, y, vx, vy, size, life, maxLife;
    }
    private final java.util.List<Dust> particles = new ArrayList<>();
    private final Random rng = new Random();

    public ClickGuiScreen() {
        super(Component.literal("Axiline"));
        for (Category value : Category.values()) {
            this.tabHover.put(value, new Anim(0.0f, 18.0f));
        }
        for (Tab value : Tab.values()) {
            this.navHover.put(value, new Anim(0.0f, 18.0f));
        }
    }

    private Page page() {
        return switch (tab) {
            case MODULES -> MODULES_PAGE;
            case THEMES -> THEMES_PAGE;
            case CONFIGS -> CONFIGS_PAGE;
            case SETTINGS -> SETTINGS_PAGE;
        };
    }

    private String scrollKey() {
        if (tab == Tab.MODULES) {
            return "modules:" + MODULES_PAGE.category().name() + ":" + this.search;
        }
        return tab.name();
    }

    private float viewportH() {
        return Style.contentViewportH();
    }

    @Override
    protected void init() {
        if (Float.isNaN(panelX)) {
            panelX = Render2D.screenWidth() * (Style.DESIGN_X / (float) Style.DESIGN_SCREEN_W);
            panelY = Render2D.screenHeight() * (Style.DESIGN_Y / (float) Style.DESIGN_SCREEN_H);
        }
        clampToScreen();
        ThemeManager.all();
        this.closing = false;
        this.searchFocused = false;
        this.openAnim.snap(0.0f);
        this.openAnim.to(1.0f);
        this.selectorAnim.snap(MODULES_PAGE.category().ordinal());
        this.navAnim.snap(tab.ordinal());
        this.sourceAnim.snap(tab == Tab.CONFIGS ? 1.0f : 0.0f);
        this.scrollAnim.snap(SCROLL.getOrDefault(scrollKey(), 0.0f));
        this.switchAnim.snap(1.0f);
        MODULES_PAGE.search(this.search);
        page().onShow();
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
        Anim.beginFrame((now - this.lastFrame) / 1_000_000_000.0f);
        this.lastFrame = now;

        this.openAnim.to(this.closing ? 0.0f : 1.0f);
        float open = this.openAnim.get();
        if (this.closing && open < 0.01f) {
            this.minecraft.gui.setScreen(null);
            return;
        }

        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        float eased = this.closing ? open : Anim.easeOut(open);

        float dt = Math.min(0.05f, (now - this.lastFrame) / 1_000_000_000.0f);
        // lastFrame already updated above; use open anim frame delta from Anim.beginFrame path
        tickParticles(0.016f);

        Render2D.begin(gg);

        // dim + floating particles behind the panel (iOS-like depth)
        Render2D.round(gg, 0, 0, Render2D.screenWidth(), Render2D.screenHeight(), 0.0f,
                Render2D.withAlpha(0xFF000000, 0.35f * eased));
        renderParticles(gg, eased);

        Render2D.pushScale(gg, panelX + Style.PANEL_W / 2.0f, panelY + Style.PANEL_H / 2.0f,
                0.97f + 0.03f * eased);
        Render2D.pushTranslate(gg, 0.0f, (1.0f - eased) * 10.0f);
        Render2D.pushAlpha(eased);

        // panel shell + soft outer glow ring
        Render2D.round(gg, panelX - 1.0f, panelY - 1.0f, Style.PANEL_W + 2.0f, Style.PANEL_H + 2.0f,
                Style.PANEL_R + 1.0f, Render2D.withAlpha(Style.accent(), 0.08f));
        Render2D.round(gg, panelX, panelY, Style.PANEL_W, Style.PANEL_H, Style.PANEL_R, Style.PANEL);
        Render2D.round(gg, panelX + 12.0f, panelY + 1.0f, Style.PANEL_W - 24.0f, 1.0f, 1.0f, Style.WHITE_04);
        // subtle inner top sheen
        Render2D.round(gg, panelX + 18.0f, panelY + 8.0f, Style.PANEL_W - 36.0f, 28.0f, 14.0f,
                Style.white(0.018f));
        // header / content divider
        Render2D.round(gg, panelX + Style.CONTENT_X, panelY + Style.CONTENT_TOP - 8.0f,
                Style.COLUMN_PITCH * Style.COLUMNS, 1.0f, 0.5f, Style.WHITE_04);

        renderHeader(gg, mx, my);
        renderSidebar(gg, mx, my);
        renderSidebarTools(gg, mx, my);
        renderContent(gg, mx, my);
        renderNav(gg, mx, my);

        Render2D.popAlpha();
        Render2D.popTransform(gg);
        Render2D.popTransform(gg);
        Render2D.end(gg);
    }


    private void ensureParticles() {
        int want = GuiPrefs.particles() ? GuiPrefs.particleCount() : 0;
        while (this.particles.size() < want) {
            Dust d = new Dust();
            respawn(d, true);
            this.particles.add(d);
        }
        while (this.particles.size() > want) {
            this.particles.remove(this.particles.size() - 1);
        }
    }

    private void respawn(Dust d, boolean randomLife) {
        float sw = Render2D.screenWidth();
        float sh = Render2D.screenHeight();
        d.x = this.rng.nextFloat() * sw;
        d.y = this.rng.nextFloat() * sh;
        float sp = GuiPrefs.particleSpeed();
        d.vx = (this.rng.nextFloat() - 0.5f) * 18.0f * sp;
        d.vy = (this.rng.nextFloat() - 0.5f) * 14.0f * sp - 6.0f * sp;
        d.size = 1.2f + this.rng.nextFloat() * 2.4f;
        d.maxLife = 2.5f + this.rng.nextFloat() * 4.0f;
        d.life = randomLife ? this.rng.nextFloat() * d.maxLife : d.maxLife;
    }

    private void tickParticles(float dt) {
        ensureParticles();
        if (!GuiPrefs.particles()) {
            return;
        }
        float sw = Render2D.screenWidth();
        float sh = Render2D.screenHeight();
        for (Dust d : this.particles) {
            d.x += d.vx * dt;
            d.y += d.vy * dt;
            d.life -= dt;
            if (d.life <= 0.0f || d.x < -20 || d.y < -20 || d.x > sw + 20 || d.y > sh + 20) {
                respawn(d, false);
            }
        }
    }

    private void renderParticles(GuiGraphicsExtractor gg, float alpha) {
        if (!GuiPrefs.particles() || alpha < 0.01f) {
            return;
        }
        float op = GuiPrefs.particleOpacity() * alpha;
        for (Dust d : this.particles) {
            float t = d.life / d.maxLife;
            float a = op * (t < 0.2f ? t / 0.2f : (t > 0.8f ? (1.0f - t) / 0.2f : 1.0f));
            int col = Render2D.withAlpha(Style.accent(), a * 0.55f);
            int col2 = Render2D.withAlpha(Style.WHITE, a * 0.25f);
            Render2D.circle(gg, d.x, d.y, d.size * 2.2f, col);
            Render2D.circle(gg, d.x + d.size * 0.3f, d.y + d.size * 0.3f, d.size, col2);
        }
    }

    private void renderHeader(GuiGraphicsExtractor gg, double mx, double my) {
        Fonts.title().draw(gg, "Axiline", panelX + 22.0f, panelY + 26.0f, Style.WHITE);

        String section = tab == Tab.MODULES
                ? MODULES_PAGE.category().displayName()
                : prettyTab(tab);
        Fonts.title().draw(gg, section, panelX + Style.CONTENT_X, panelY + 26.0f, Style.WHITE);

        // time under brand — always inside panel
        LocalDateTime now = LocalDateTime.now();
        String capsule = now.format(DATE) + "  " + now.format(CLOCK);
        float capW = Fonts.label().width(capsule) + 20.0f;
        float capH = 22.0f;
        float capX = panelX + 20.0f;
        float capY = panelY + 42.0f;
        Render2D.round(gg, capX, capY, capW, capH, 11.0f, Style.white(0.06f));
        Fonts.label().draw(gg, capsule, capX + 10.0f, capY + 15.0f, Style.WHITE_45);

        // search top-right inset
        float sx = panelX + Style.PANEL_W - Style.SEARCH_W - 24.0f;
        float sy = panelY + 20.0f;
        boolean hover = Render2D.hovered(mx, my, sx, sy, Style.SEARCH_W, Style.SEARCH_H);
        float focus = this.searchFocused ? 1.0f : (hover ? 0.55f : 0.0f);
        int bg = Render2D.lerp(Style.white(0.045f), Style.white(0.10f), focus);
        Render2D.round(gg, sx, sy, Style.SEARCH_W, Style.SEARCH_H, Style.SEARCH_R, bg);
        if (this.searchFocused) {
            Render2D.round(gg, sx, sy, Style.SEARCH_W, Style.SEARCH_H, Style.SEARCH_R,
                    Render2D.withAlpha(Style.accent(), 0.12f));
        }

        String shown = this.search.isEmpty() && !this.searchFocused ? "Поиск..." : this.search;
        int color = this.search.isEmpty() && !this.searchFocused ? Style.WHITE_25 : Style.WHITE_45;
        Fonts.label().draw(gg, ellipsize(shown, Style.SEARCH_W - 28.0f), sx + 12.0f, sy + 19.0f, color);

        if (this.searchFocused && (System.currentTimeMillis() / 520L) % 2L == 0L) {
            float caret = sx + 12.0f + Fonts.label().width(this.search);
            Render2D.rect(gg, caret, sy + 8.0f, 1.0f, 14.0f, Style.accent());
        }
    }

    private static String prettyTab(Tab t) {
        return switch (t) {
            case MODULES -> "Модули";
            case CONFIGS -> "Конфиги";
            case THEMES -> "Темы";
            case SETTINGS -> "Настройки";
        };
    }

    private static String ellipsize(String text, float maxW) {
        if (Fonts.label().width(text) <= maxW) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && Fonts.label().width(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private void renderSidebar(GuiGraphicsExtractor gg, double mx, double my) {
        float x = panelX + Style.SIDEBAR_X;
        float y = panelY + Style.SIDEBAR_TOP - 8.0f;
        float h = Style.SIDEBAR_STEP * Category.values().length + 12.0f;

        Render2D.round(gg, x, y, Style.SIDEBAR_W, h, Style.SIDEBAR_R, Style.WHITE_02);

        Category current = MODULES_PAGE.category();
        this.selectorAnim.to(current.ordinal());
        float slot = this.selectorAnim.get();
        float travel = Math.abs(slot - current.ordinal());
        float stretch = Style.SELECTOR_H * (1.0f + Math.min(travel, 1.0f) * 0.2f);
        float selY = iconCenter(slot) - stretch / 2.0f;

        float modulesAlpha = tab == Tab.MODULES ? 1.0f : 0.35f;
        Render2D.round(gg, panelX + Style.SELECTOR_X, selY,
                Style.SELECTOR_W, stretch, Style.SELECTOR_R,
                Render2D.withAlpha(Style.white(0.08f), modulesAlpha));
        Render2D.round(gg, panelX + Style.SELECTOR_X + 4.0f, selY + 8.0f,
                3.0f, stretch - 16.0f, 1.5f,
                Render2D.withAlpha(Style.accent(), modulesAlpha));

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category value = categories[i];
            float center = iconCenter(i);
            boolean hover = Render2D.hovered(mx, my, x, center - Style.SIDEBAR_STEP / 2.0f,
                    Style.SIDEBAR_W, Style.SIDEBAR_STEP);
            boolean active = value == current && tab == Tab.MODULES;
            Anim anim = this.tabHover.get(value);
            anim.to(hover || active ? 1.0f : 0.0f);
            float k = anim.get();

            int iconTint = active ? Style.accent()
                    : Render2D.lerp(Style.WHITE_25, Style.WHITE_45, k);
            int textTint = active ? Style.WHITE
                    : Render2D.lerp(Style.WHITE_25, Style.WHITE_45, k);

            float ix = x + 16.0f;
            Render2D.icon(gg, value.icon(), ix, center - value.iconHeight() / 2.0f,
                    value.iconWidth(), value.iconHeight(), iconTint);
            Fonts.body().draw(gg, value.displayName(),
                    ix + value.iconWidth() + 10.0f, center + 4.0f, textTint);
        }
    }

    private void renderSidebarTools(GuiGraphicsExtractor gg, double mx, double my) {
        this.sourceAnim.to(tab == Tab.CONFIGS ? 1.0f : 0.0f);
        float open = Anim.easeInOut(this.sourceAnim.get());
        if (open <= 0.01f) {
            return;
        }

        float x = panelX + Style.SIDEBAR_X;
        float y = panelY + Style.SOURCE_Y;
        float w = Style.SIDEBAR_W;
        float h = Style.SOURCE_H;

        Render2D.pushAlpha(open);
        Render2D.round(gg, x, y, w, h, Style.SIDEBAR_R, Style.WHITE_02);

        float iconSize = 14.0f;
        float gap = (w - iconSize * 3.0f) / 4.0f;
        for (int i = 0; i < SOURCE_ICONS.length; i++) {
            float ix = x + gap + i * (iconSize + gap);
            float iy = y + (h - iconSize) / 2.0f;
            boolean hover = Render2D.hovered(mx, my, ix - 6.0f, y, iconSize + 12.0f, h);
            this.sourceHover[i].to(hover ? 1.0f : 0.0f);
            float k = this.sourceHover[i].get();
            Render2D.icon(gg, SOURCE_ICONS[i], ix, iy, iconSize, iconSize,
                    Render2D.lerp(Style.WHITE_25, Style.accent(), k));
        }
        Render2D.popAlpha();
    }

    private void renderContent(GuiGraphicsExtractor gg, double mx, double my) {
        MODULES_PAGE.search(this.search);
        Page page = page();
        float contentHeight = page.contentHeight();
        float viewport = viewportH();
        float scroll = scroll(contentHeight, viewport);

        float clipX = panelX + Style.CONTENT_X;
        float clipY = panelY + Style.CONTENT_TOP;
        float clipW = Style.COLUMN_PITCH * Style.COLUMNS + 4.0f;

        this.switchAnim.to(1.0f);
        float appear = Anim.easeOut(this.switchAnim.get());

        Render2D.pushClip(gg, clipX, clipY, clipW, viewport);
        Render2D.pushAlpha(appear);
        Render2D.pushTranslate(gg, 0.0f, (1.0f - appear) * 12.0f);
        page.render(gg, clipX, clipY, scroll, mx, my);
        Render2D.popTransform(gg);
        Render2D.popAlpha();
        Render2D.popClip(gg);

        renderScrollbar(gg, contentHeight, viewport, scroll, mx, my);
    }

    private void renderNav(GuiGraphicsExtractor gg, double mx, double my) {
        float x = panelX + Style.NAV_X;
        float y = panelY + Style.NAV_Y;
        Render2D.round(gg, x, y, Style.NAV_W, Style.NAV_H, Style.NAV_R, Style.PANEL_RAISED);

        this.navAnim.to(tab.ordinal());
        float slot = this.navAnim.get();
        float indicatorX = x + Style.NAV_FIRST + slot * Style.NAV_STEP - Style.NAV_INDICATOR_W / 2.0f;
        float spread = Math.abs(slot - tab.ordinal());
        float width = Style.NAV_INDICATOR_W * (1.0f + Math.min(spread, 1.0f) * 1.15f);
        Render2D.round(gg, indicatorX - (width - Style.NAV_INDICATOR_W) / 2.0f,
                y + Style.NAV_INDICATOR_Y, width, Style.NAV_INDICATOR_H, 1.0f, Style.accent());

        for (Tab value : Tab.values()) {
            float center = x + Style.NAV_FIRST + value.ordinal() * Style.NAV_STEP;
            boolean hover = Render2D.hovered(mx, my, center - Style.NAV_STEP / 2.0f, y,
                    Style.NAV_STEP, Style.NAV_H);
            Anim anim = this.navHover.get(value);
            anim.to(hover || value == tab ? 1.0f : 0.0f);
            float k = anim.get();
            Render2D.icon(gg, value.icon(), center - value.iconWidth() / 2.0f,
                    y + Style.NAV_H / 2.0f - value.iconHeight() / 2.0f,
                    value.iconWidth(), value.iconHeight(),
                    value == tab ? Style.accent()
                            : Render2D.lerp(Style.WHITE_25, Style.WHITE_45, k));
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor gg, float contentHeight, float viewport,
                                 float scroll, double mx, double my) {
        float overflow = Math.max(0.0f, contentHeight - viewport);
        boolean recent = System.currentTimeMillis() - this.lastScroll < 900L;
        boolean nearby = Render2D.hovered(mx, my, panelX, panelY, Style.PANEL_W, Style.PANEL_H);
        this.scrollbarAnim.to(overflow > 0.0f && (recent || nearby) ? 1.0f : 0.2f);
        float visibility = this.scrollbarAnim.get();
        if (visibility < 0.05f) {
            return;
        }

        float x = panelX + Style.SCROLLBAR_X;
        float y = panelY + Style.SCROLLBAR_Y;
        float h = viewport;
        Render2D.round(gg, x, y, Style.SCROLLBAR_W, h, 1.5f,
                Render2D.withAlpha(Style.WHITE_04, visibility));

        float thumb = overflow <= 0.0f ? h
                : Math.max(Style.SCROLLBAR_MIN_THUMB, h * viewport / contentHeight);
        float travel = h - thumb;
        float offset = overflow <= 0.0f ? 0.0f : travel * (scroll / overflow);
        Render2D.round(gg, x, y + offset, Style.SCROLLBAR_W, thumb, 1.5f,
                Render2D.withAlpha(Style.accent(), visibility * 0.85f));
    }

    private float scroll(float contentHeight, float viewport) {
        float max = Math.max(0.0f, contentHeight - viewport);
        float target = Mth.clamp(SCROLL.getOrDefault(scrollKey(), 0.0f), 0.0f, max);
        SCROLL.put(scrollKey(), target);
        this.scrollAnim.to(target);
        return Math.round(this.scrollAnim.get());
    }

    /** Keep expanded module bottom inside viewport (fixes "settings off screen"). */
    private void ensureVisible(float moduleTop, float moduleHeight) {
        float viewport = viewportH();
        float scroll = SCROLL.getOrDefault(scrollKey(), 0.0f);
        float bottom = moduleTop + moduleHeight - scroll;
        if (bottom > viewport - 8.0f) {
            float need = bottom - viewport + 16.0f;
            SCROLL.put(scrollKey(), scroll + need);
            this.lastScroll = System.currentTimeMillis();
        }
    }

    private float iconCenter(float index) {
        return panelY + Style.SIDEBAR_TOP + index * Style.SIDEBAR_STEP + Style.SIDEBAR_STEP / 2.0f;
    }

    private void clampToScreen() {
        panelX = Math.round(Mth.clamp(panelX, 0.0f,
                Math.max(0, Render2D.screenWidth() - Style.PANEL_W)));
        panelY = Math.round(Mth.clamp(panelY, 0.0f,
                Math.max(0, Render2D.screenHeight() - Style.PANEL_H)));
    }

    private void switchTo(Tab value) {
        if (tab == value) {
            return;
        }
        tab = value;
        this.searchFocused = false;
        this.switchAnim.snap(0.0f);
        this.scrollAnim.snap(SCROLL.getOrDefault(scrollKey(), 0.0f));
        page().onShow();
        UiSounds.soft();
    }

    private boolean searchHit(double mx, double my) {
        float sx = panelX + Style.PANEL_W - Style.SEARCH_W - 24.0f;
        float sy = panelY + 20.0f;
        return Render2D.hovered(mx, my, sx, sy, Style.SEARCH_W, Style.SEARCH_H);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.closing) {
            return true;
        }
        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        int button = event.button();

        if (button == 0 && searchHit(mx, my)) {
            this.searchFocused = true;
            if (tab != Tab.MODULES) {
                switchTo(Tab.MODULES);
            }
            UiSounds.soft();
            return true;
        }
        this.searchFocused = false;

        for (Tab value : Tab.values()) {
            float center = panelX + Style.NAV_X + Style.NAV_FIRST + value.ordinal() * Style.NAV_STEP;
            if (Render2D.hovered(mx, my, center - Style.NAV_STEP / 2.0f,
                    panelY + Style.NAV_Y, Style.NAV_STEP, Style.NAV_H)) {
                switchTo(value);
                return true;
            }
        }

        float sidebarX = panelX + Style.SIDEBAR_X;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            float center = iconCenter(i);
            if (Render2D.hovered(mx, my, sidebarX, center - Style.SIDEBAR_STEP / 2.0f,
                    Style.SIDEBAR_W, Style.SIDEBAR_STEP)) {
                boolean changedCat = categories[i] != MODULES_PAGE.category();
                if (tab != Tab.MODULES) {
                    MODULES_PAGE.category(categories[i]);
                    switchTo(Tab.MODULES);
                } else if (changedCat) {
                    MODULES_PAGE.category(categories[i]);
                    this.switchAnim.snap(0.0f);
                    this.scrollAnim.snap(SCROLL.getOrDefault(scrollKey(), 0.0f));
                    UiSounds.soft();
                }
                return true;
            }
        }

        if (tab == Tab.CONFIGS && this.sourceAnim.get() > 0.5f) {
            float x = panelX + Style.SIDEBAR_X;
            float y = panelY + Style.SOURCE_Y;
            float w = Style.SIDEBAR_W;
            float h = Style.SOURCE_H;
            float iconSize = 14.0f;
            float gap = (w - iconSize * 3.0f) / 4.0f;
            for (int i = 0; i < SOURCE_ICONS.length; i++) {
                float ix = x + gap + i * (iconSize + gap);
                if (Render2D.hovered(mx, my, ix - 6.0f, y, iconSize + 12.0f, h)) {
                    switch (i) {
                        case 0 -> ConfigManager.openFolder();
                        case 1 -> ConfigManager.save(ConfigManager.nextFreeName());
                        default -> ConfigManager.refresh();
                    }
                    UiSounds.click();
                    return true;
                }
            }
        }

        float viewport = viewportH();
        float clipX = panelX + Style.CONTENT_X;
        float clipY = panelY + Style.CONTENT_TOP;
        if (Render2D.hovered(mx, my, clipX, clipY, Style.COLUMN_PITCH * Style.COLUMNS + 4.0f, viewport)) {
            Page page = page();
            float sc = scroll(page.contentHeight(), viewport);
            if (page.mouseClicked(clipX, clipY, sc, mx, my, button)) {
                UiSounds.click();
                // if modules page expanded something, nudge scroll so bottom settings stay visible
                if (tab == Tab.MODULES) {
                    float ch = page.contentHeight();
                    float max = Math.max(0.0f, ch - viewport);
                    float cur = SCROLL.getOrDefault(scrollKey(), 0.0f);
                    if (ch > viewport && cur < max) {
                        // soft follow when content grows
                        SCROLL.put(scrollKey(), Math.min(max, cur + 24.0f));
                    }
                }
                return true;
            }
        }

        if (button == 0 && Render2D.hovered(mx, my, panelX, panelY, Style.PANEL_W, Style.HEADER_BAR_H)
                && !searchHit(mx, my)) {
            this.draggingPanel = true;
            this.grabX = (float) mx - panelX;
            this.grabY = (float) my - panelY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mx = Render2D.mouseX();
        double my = Render2D.mouseY();
        if (this.draggingPanel) {
            panelX = (float) mx - this.grabX;
            panelY = (float) my - this.grabY;
            clampToScreen();
            return true;
        }
        float viewport = viewportH();
        page().mouseDragged(panelX + Style.CONTENT_X, panelY + Style.CONTENT_TOP,
                scroll(page().contentHeight(), viewport), mx, my);
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingPanel = false;
        page().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!Render2D.hovered(Render2D.mouseX(), Render2D.mouseY(),
                panelX, panelY, Style.PANEL_W, Style.PANEL_H)) {
            return false;
        }
        float viewport = viewportH();
        // recompute height live so expanded modules can scroll fully
        float max = Math.max(0.0f, page().contentHeight() - viewport);
        float value = Mth.clamp(SCROLL.getOrDefault(scrollKey(), 0.0f) - (float) scrollY * 42.0f,
                0.0f, max);
        SCROLL.put(scrollKey(), value);
        this.lastScroll = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (this.searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !this.search.isEmpty()) {
                this.search = this.search.substring(0, this.search.length() - 1);
                MODULES_PAGE.search(this.search);
                SCROLL.put(scrollKey(), 0.0f);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                this.searchFocused = false;
                return true;
            }
            return true;
        }
        if (page().keyPressed(key)) {
            return true;
        }
        if (page().capturesInput()) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchFocused) {
            char c = (char) event.codepoint();
            if (!Character.isISOControl(c) && this.search.length() < 32) {
                this.search += c;
                MODULES_PAGE.search(this.search);
                SCROLL.put(scrollKey(), 0.0f);
            }
            return true;
        }
        return page().charTyped((char) event.codepoint()) || super.charTyped(event);
    }

    @Override
    public void onClose() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        this.searchFocused = false;
        page().mouseReleased();
    }

    public static void preload() {
        Fonts.title();
        Fonts.body();
        Fonts.label();
    }
}
