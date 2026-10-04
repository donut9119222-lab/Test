package dev.hatek.client.module.setting;

import dev.hatek.client.ui.Style;
import dev.hatek.client.ui.render.Anim;
import dev.hatek.client.ui.render.Fonts;
import dev.hatek.client.ui.render.Render2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode dropdown: open/close ONLY with right mouse button.
 * When open, reserves height so the list is fully visible (not under next settings).
 */
public final class ModeSetting extends Setting {
    private static final int OPTION_H = 26;
    private static final int LIST_GAP = 6;

    private final List<String> options;
    private int index;

    private boolean open;
    private final Anim openAnim = new Anim(0.0f, 14.0f);
    private final Anim hoverAnim = new Anim(0.0f, 18.0f);
    private final Anim chevronAnim = new Anim(0.0f, 16.0f);
    private final List<Anim> optionAnims = new ArrayList<>();

    public ModeSetting(String name, int index, String... options) {
        super(name);
        this.options = List.of(options);
        this.index = Math.floorMod(index, Math.max(1, this.options.size()));
        for (int i = 0; i < this.options.size(); i++) {
            this.optionAnims.add(new Anim(0.0f, 18.0f));
        }
    }

    public String value() {
        return this.options.get(this.index);
    }

    public boolean is(String option) {
        return value().equalsIgnoreCase(option);
    }

    public void value(String option) {
        int found = this.options.indexOf(option);
        if (found >= 0) {
            this.index = found;
        }
    }

    public List<String> options() {
        return this.options;
    }

    public boolean isOpen() {
        return this.open;
    }

    private float listHeight() {
        return LIST_GAP + this.options.size() * OPTION_H;
    }

    @Override
    public float bottomOffset() {
        this.openAnim.to(this.open ? 1.0f : 0.0f);
        float openK = Anim.easeInOut(this.openAnim.get());
        // reserve space only while open — list sits above next setting, fully visible
        return Style.LABEL_TO_CONTROL + Style.ROW_H + listHeight() * openK;
    }

    @Override
    public void closePopups() {
        this.open = false;
    }

    @Override
    public void render(GuiGraphicsExtractor gg, float x, float baseline, double mx, double my) {
        drawLabel(gg, x, baseline);
        float rowY = controlTop(baseline);
        boolean hover = Render2D.hovered(mx, my, x, rowY, Style.ROW_W, Style.ROW_H);
        this.hoverAnim.to(hover || this.open ? 1.0f : 0.0f);
        this.chevronAnim.to(this.open ? 1.0f : 0.0f);
        this.openAnim.to(this.open ? 1.0f : 0.0f);
        float k = this.hoverAnim.get();
        float openness = Anim.easeInOut(this.openAnim.get());

        Render2D.round(gg, x, rowY, Style.ROW_W, Style.ROW_H, Style.ROW_R,
                Render2D.lerp(Render2D.lerp(Style.WHITE_04, Style.white(0.08f), k),
                        Style.white(0.12f), openness));

        Render2D.icon(gg, "list", x + Style.FIELD_ICON_X + 1.0f,
                rowY + (Style.ROW_H - 6.0f) / 2.0f, 8.0f, 6.0f,
                Render2D.lerp(Style.WHITE_45, Style.WHITE, k * 0.5f));

        Fonts.body().draw(gg, value(), x + 24.0f, rowY + Style.FIELD_TEXT_BASELINE,
                Render2D.lerp(Style.WHITE_45, Style.WHITE, Math.max(k * 0.45f, openness)));

        float flip = Anim.easeInOut(this.chevronAnim.get());
        int chevronTint = Render2D.lerp(Style.WHITE_45, Style.WHITE, Math.max(k, openness));
        Render2D.icon(gg, "chevron", x + Style.ROW_W - 18.0f, rowY + Style.ROW_H / 2.0f - 3.0f,
                8.0f, 6.0f, chevronTint, flip > 0.5f);

        if (openness <= 0.002f) {
            return;
        }

        float listTop = rowY + Style.ROW_H + LIST_GAP;
        float listH = listHeight() * openness;

        Render2D.round(gg, x - 2.0f, listTop - 2.0f, Style.ROW_W + 4.0f, listH + 2.0f, 10.0f,
                Render2D.withAlpha(Style.PANEL_RAISED, openness * 0.98f));
        Render2D.round(gg, x - 2.0f, listTop - 2.0f, Style.ROW_W + 4.0f, listH + 2.0f, 10.0f,
                Render2D.withAlpha(Style.WHITE_04, openness));

        Render2D.pushAlpha(Mth.clamp((openness - 0.1f) / 0.7f, 0.0f, 1.0f));
        for (int i = 0; i < this.options.size(); i++) {
            float oy = listTop + i * OPTION_H;
            boolean selected = i == this.index;
            Anim anim = this.optionAnims.get(i);
            anim.to(Render2D.hovered(mx, my, x, oy, Style.ROW_W, OPTION_H) || selected ? 1.0f : 0.0f);
            float hot = anim.get();

            Render2D.round(gg, x, oy, Style.ROW_W, OPTION_H - 2.0f, 7.0f,
                    selected ? Render2D.withAlpha(Style.accent(), 0.22f)
                            : Render2D.lerp(0x00FFFFFF, Style.white(0.07f), hot));
            if (selected) {
                Render2D.round(gg, x + 6.0f, oy + (OPTION_H - 2.0f) / 2.0f - 4.0f, 2.5f, 8.0f, 1.2f,
                        Style.accent());
            }
            Fonts.body().draw(gg, this.options.get(i), x + 16.0f, oy + 16.0f,
                    selected ? Style.accent()
                            : Render2D.lerp(Style.WHITE_45, Style.WHITE, hot));
        }
        Render2D.popAlpha();
    }

    @Override
    public boolean mouseClicked(float x, float baseline, double mx, double my, int button) {
        float rowY = controlTop(baseline);

        // RMB on field toggles open
        if (Render2D.hovered(mx, my, x, rowY, Style.ROW_W, Style.ROW_H)) {
            if (button == 1) {
                this.open = !this.open;
                return true;
            }
            // LMB on closed field does nothing
            if (button == 0 && !this.open) {
                return false;
            }
        }

        if (!this.open) {
            return false;
        }

        float listTop = rowY + Style.ROW_H + LIST_GAP;
        for (int i = 0; i < this.options.size(); i++) {
            float oy = listTop + i * OPTION_H;
            if (button == 0 && Render2D.hovered(mx, my, x, oy, Style.ROW_W, OPTION_H)) {
                this.index = i;
                this.open = false;
                return true;
            }
        }

        // click outside closes
        if (button == 0 || button == 1) {
            this.open = false;
            return true;
        }
        return false;
    }

    @Override
    public String serialize() {
        return value();
    }

    @Override
    public void deserialize(String raw) {
        value(raw);
    }
}
