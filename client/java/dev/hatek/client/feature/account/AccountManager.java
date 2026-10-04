package dev.hatek.client.feature.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hatek.mixin.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<Account> CACHE = new ArrayList<>();
    private static final Set<String> FAVORITES = new HashSet<>();
    private static boolean loaded;

    private AccountManager() {
    }

    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("hatek");
    }

    public static Path file() {
        return root().resolve("accounts.json");
    }

    public static List<Account> all() {
        if (!loaded) {
            load();
        }
        return List.copyOf(CACHE);
    }

    public static String activeName() {
        User user = Minecraft.getInstance().getUser();
        return user == null ? "" : user.getName();
    }

    public static boolean isActive(Account account) {
        return account != null && account.name().equalsIgnoreCase(activeName());
    }

    public static boolean isFavorite(Account account) {
        if (account == null) {
            return false;
        }
        all();
        return FAVORITES.contains(account.name().toLowerCase(Locale.ROOT));
    }

    public static void toggleFavorite(Account account) {
        if (account == null) {
            return;
        }
        all();
        String key = account.name().toLowerCase(Locale.ROOT);
        if (FAVORITES.contains(key)) {
            FAVORITES.remove(key);
        } else {
            FAVORITES.add(key);
        }
        // pinned accounts float to top
        CACHE.sort((a, b) -> {
            boolean fa = FAVORITES.contains(a.name().toLowerCase(Locale.ROOT));
            boolean fb = FAVORITES.contains(b.name().toLowerCase(Locale.ROOT));
            if (fa == fb) {
                return 0;
            }
            return fa ? -1 : 1;
        });
        save();
    }

    public static Account add(String name) {
        if (!Account.valid(name)) {
            return null;
        }
        Account account = Account.of(name);
        all();
        for (Account existing : CACHE) {
            if (existing.name().equalsIgnoreCase(account.name())) {
                return existing;
            }
        }
        CACHE.add(account);
        save();
        return account;
    }

    public static void remove(Account account) {
        all();
        CACHE.removeIf(entry -> entry.name().equalsIgnoreCase(account.name()));
        FAVORITES.remove(account.name().toLowerCase(Locale.ROOT));
        save();
    }

    /** Move account at fromIndex to toIndex (0-based). Persists order. */
    public static void move(int fromIndex, int toIndex) {
        all();
        if (fromIndex < 0 || fromIndex >= CACHE.size()) {
            return;
        }
        toIndex = Math.max(0, Math.min(toIndex, CACHE.size() - 1));
        if (fromIndex == toIndex) {
            return;
        }
        Account moved = CACHE.remove(fromIndex);
        CACHE.add(toIndex, moved);
        save();
    }

    public static void clear() {
        all();
        CACHE.clear();
        FAVORITES.clear();
        save();
    }

    public static boolean login(Account account) {
        Minecraft mc = Minecraft.getInstance();
        if (account == null || mc.getConnection() != null) {
            return false;
        }
        User user = new User(account.name(), account.id(), "",
                Optional.empty(), Optional.empty());

        MinecraftAccessor accessor = (MinecraftAccessor) mc;
        accessor.hatek$setUser(user);
        accessor.hatek$setProfileFuture(CompletableFuture.completedFuture(null));
        return true;
    }

    public static boolean canSwitch() {
        return Minecraft.getInstance().getConnection() == null;
    }

    private static void load() {
        loaded = true;
        CACHE.clear();
        FAVORITES.clear();
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonArray()) {
                return;
            }
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                if (!object.has("name")) {
                    continue;
                }
                String name = object.get("name").getAsString();
                if (Account.valid(name)) {
                    CACHE.add(Account.of(name));
                    if (object.has("fav") && object.get("fav").getAsBoolean()) {
                        FAVORITES.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            CACHE.clear();
            FAVORITES.clear();
        }
    }

    private static void save() {
        JsonArray array = new JsonArray();
        for (Account account : CACHE) {
            JsonObject object = new JsonObject();
            object.addProperty("name", account.name());
            object.addProperty("id", account.id().toString());
            object.addProperty("fav", FAVORITES.contains(account.name().toLowerCase(Locale.ROOT)));
            array.add(object);
        }
        try {
            Files.createDirectories(root());
            Files.writeString(file(), GSON.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static boolean has(String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        for (Account account : all()) {
            if (account.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }
}
