package dev.hatek.client.feature.theme;

import dev.hatek.client.feature.config.ConfigManager;
import dev.hatek.client.ui.Style;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ThemeManager {
    private static final List<ThemeEntry> THEMES = new ArrayList<>();
    private static String appliedName;
    private static boolean loaded;

    private ThemeManager() {
    }

    public static List<ThemeEntry> all() {
        ensure();
        return Collections.unmodifiableList(THEMES);
    }

    public static boolean isApplied(ThemeEntry theme) {
        ensure();
        return theme.name().equals(appliedName);
    }

    public static ThemeEntry applied() {
        ensure();
        for (ThemeEntry theme : THEMES) {
            if (isApplied(theme)) {
                return theme;
            }
        }
        return null;
    }

    public static void apply(ThemeEntry theme) {
        ensure();
        appliedName = theme.name();
        Style.accent(theme.color());
        save();
    }

    private static void ensure() {
        if (loaded) {
            return;
        }
        loaded = true;
        register();
        load();
        if (appliedName == null && !THEMES.isEmpty()) {
            appliedName = THEMES.get(0).name();
            Style.accent(THEMES.get(0).color());
        }
    }

    private static Path file() {
        return ConfigManager.root().resolve("theme.txt");
    }

    private static void save() {
        try {
            Files.createDirectories(ConfigManager.root());
            Files.writeString(file(), appliedName == null ? "" : appliedName, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static void load() {
        try {
            if (!Files.isRegularFile(file())) {
                return;
            }
            String saved = Files.readString(file(), StandardCharsets.UTF_8).trim();
            for (ThemeEntry theme : THEMES) {
                if (theme.name().equalsIgnoreCase(saved)) {
                    appliedName = theme.name();
                    Style.accent(theme.color());
                    return;
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static String hex(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0x00FFFFFF);
    }

    private static void register() {
        String author = "Axiline";
        String date = "03.10.2026";
        add("Midnight", author, date, 0xFF5B8CFF);
        add("Sakura", author, date, 0xFFFF7EB3);
        add("Aurora", author, date, 0xFF3DDC97);
        add("Sunset", author, date, 0xFFFF8A4C);
        add("Glacier", author, date, 0xFF7EC8FF);
        add("Velvet", author, date, 0xFFC084FC);
        add("Amber", author, date, 0xFFFFB020);
        add("Jade", author, date, 0xFF34D399);
        add("Rosewood", author, date, 0xFFF43F5E);
        add("Arctic", author, date, 0xFF94A3B8);
        add("Indigo", author, date, 0xFF818CF8);
        add("Mango", author, date, 0xFFFBBF24);
        appliedName = "Midnight";
        Style.accent(0xFF5B8CFF);
    }

    private static void add(String name, String author, String date, int color) {
        THEMES.add(new ThemeEntry(name, author, date, color));
    }
}