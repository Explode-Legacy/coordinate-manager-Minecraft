package dev.explodelegacy.coordinatemanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SavedLocations {

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("tcmod_locations.json");

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    /*
     * Stored format:
     *
     * {
     *   "player-uuid": {
     *     "favorite": "home",
     *     "locations": {
     *       "home": {
     *         "dimension": "minecraft:overworld",
     *         "x": 100.0,
     *         "y": 64.0,
     *         "z": 200.0
     *       }
     *     }
     *   }
     * }
     */
    private static final Map<UUID, UserData> DATA =
            new LinkedHashMap<>();

    private static boolean loaded = false;

    private SavedLocations() {
    }

    // ------------------------------------------------------------
    // Load / Save
    // ------------------------------------------------------------

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;

        if (!Files.exists(FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE)) {

            JsonElement rootElement =
                    JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root =
                    rootElement.getAsJsonObject();

            for (Map.Entry<String, JsonElement> playerEntry :
                    root.entrySet()) {

                try {
                    UUID uuid =
                            UUID.fromString(playerEntry.getKey());

                    JsonObject playerObject =
                            playerEntry.getValue().getAsJsonObject();

                    UserData userData =
                            new UserData();

                    /*
                     * New format
                     */
                    if (playerObject.has("locations")) {

                        if (playerObject.has("favorite")
                                && playerObject.get("favorite").isJsonPrimitive()) {

                            userData.favorite =
                                    playerObject
                                            .get("favorite")
                                            .getAsString();

                            if (userData.favorite.isBlank()) {
                                userData.favorite = null;
                            }
                        }

                        JsonObject locations =
                                playerObject
                                        .getAsJsonObject("locations");

                        for (Map.Entry<String, JsonElement> locationEntry :
                                locations.entrySet()) {

                            Entry entry =
                                    parseEntry(
                                            locationEntry.getValue()
                                                    .getAsJsonObject()
                                    );

                            if (entry != null) {
                                userData.locations.put(
                                        locationEntry.getKey(),
                                        entry
                                );
                            }
                        }

                    } else {

                        /*
                         * Compatibility with the older format:
                         *
                         * {
                         *   "uuid": {
                         *      "home": {...},
                         *      "base": {...}
                         *   }
                         * }
                         */
                        for (Map.Entry<String, JsonElement> locationEntry :
                                playerObject.entrySet()) {

                            if (!locationEntry.getValue().isJsonObject()) {
                                continue;
                            }

                            Entry entry =
                                    parseEntry(
                                            locationEntry.getValue()
                                                    .getAsJsonObject()
                                    );

                            if (entry != null) {
                                userData.locations.put(
                                        locationEntry.getKey(),
                                        entry
                                );
                            }
                        }
                    }

                    DATA.put(uuid, userData);

                } catch (Exception ignored) {
                    // Ignore malformed player data.
                }
            }

        } catch (Exception ignored) {
            // Ignore malformed config file.
        }
    }

    private static Entry parseEntry(JsonObject object) {

        try {

            String dimension =
                    object.has("dimension")
                            ? object.get("dimension").getAsString()
                            : "minecraft:overworld";

            double x =
                    object.has("x")
                            ? object.get("x").getAsDouble()
                            : 0.0;

            double y =
                    object.has("y")
                            ? object.get("y").getAsDouble()
                            : 0.0;

            double z =
                    object.has("z")
                            ? object.get("z").getAsDouble()
                            : 0.0;

            return new Entry(
                    dimension,
                    x,
                    y,
                    z
            );

        } catch (Exception ignored) {
            return null;
        }
    }

    private static synchronized void saveFile() {

        ensureLoaded();

        try {

            Path parent =
                    FILE.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonObject root =
                    new JsonObject();

            for (Map.Entry<UUID, UserData> playerEntry :
                    DATA.entrySet()) {

                UserData userData =
                        playerEntry.getValue();

                JsonObject playerObject =
                        new JsonObject();

                if (userData.favorite != null
                        && !userData.favorite.isBlank()) {

                    playerObject.add(
                            "favorite",
                            new JsonPrimitive(
                                    userData.favorite
                            )
                    );

                } else {
                    playerObject.add(
                            "favorite",
                            new JsonPrimitive("")
                    );
                }

                JsonObject locations =
                        new JsonObject();

                for (Map.Entry<String, Entry> locationEntry :
                        userData.locations.entrySet()) {

                    Entry entry =
                            locationEntry.getValue();

                    JsonObject locationObject =
                            new JsonObject();

                    locationObject.addProperty(
                            "dimension",
                            entry.dimension
                    );

                    locationObject.addProperty(
                            "x",
                            entry.x
                    );

                    locationObject.addProperty(
                            "y",
                            entry.y
                    );

                    locationObject.addProperty(
                            "z",
                            entry.z
                    );

                    locations.add(
                            locationEntry.getKey(),
                            locationObject
                    );
                }

                playerObject.add(
                        "locations",
                        locations
                );

                root.add(
                        playerEntry.getKey().toString(),
                        playerObject
                );
            }

            try (Writer writer =
                         Files.newBufferedWriter(FILE)) {

                GSON.toJson(
                        root,
                        writer
                );
            }

        } catch (IOException ignored) {
            // Ignore config save errors.
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static UserData getUser(UUID uuid) {

        ensureLoaded();

        return DATA.computeIfAbsent(
                uuid,
                ignored -> new UserData()
        );
    }

    // ------------------------------------------------------------
    // Locations
    // ------------------------------------------------------------

    public static synchronized void set(
            UUID uuid,
            String name,
            String dimension,
            double x,
            double y,
            double z
    ) {

        UserData user =
                getUser(uuid);

        user.locations.put(
                name,
                new Entry(
                        dimension,
                        x,
                        y,
                        z
                )
        );

        saveFile();
    }

    public static synchronized Entry get(
            UUID uuid,
            String name
    ) {

        UserData user =
                getUser(uuid);

        return user.locations.get(name);
    }

    public static synchronized Map<String, Entry> all(
            UUID uuid
    ) {

        UserData user =
                getUser(uuid);

        return new LinkedHashMap<>(
                user.locations
        );
    }

    public static synchronized boolean remove(
            UUID uuid,
            String name
    ) {

        UserData user =
                getUser(uuid);

        Entry removed =
                user.locations.remove(name);

        if (removed == null) {
            return false;
        }

        if (name.equals(user.favorite)) {
            user.favorite = null;
        }

        saveFile();

        return true;
    }

    public static synchronized boolean rename(
            UUID uuid,
            String oldName,
            String newName
    ) {

        UserData user =
                getUser(uuid);

        if (!user.locations.containsKey(oldName)) {
            return false;
        }

        if (newName == null || newName.isBlank()) {
            return false;
        }

        if (user.locations.containsKey(newName)) {
            return false;
        }

        Entry entry =
                user.locations.remove(oldName);

        user.locations.put(
                newName,
                entry
        );

        if (oldName.equals(user.favorite)) {
            user.favorite = newName;
        }

        saveFile();

        return true;
    }

    // ------------------------------------------------------------
    // Favourite
    // ------------------------------------------------------------

    public static synchronized boolean setFavorite(
            UUID uuid,
            String name
    ) {

        UserData user =
                getUser(uuid);

        if (!user.locations.containsKey(name)) {
            return false;
        }

        user.favorite = name;

        saveFile();

        return true;
    }

    public static synchronized String getFavoriteName(
            UUID uuid
    ) {

        UserData user =
                getUser(uuid);

        return user.favorite;
    }

    public static synchronized Entry getFavorite(
            UUID uuid
    ) {

        UserData user =
                getUser(uuid);

        if (user.favorite == null) {
            return null;
        }

        return user.locations.get(
                user.favorite
        );
    }

    // ------------------------------------------------------------
    // Data classes
    // ------------------------------------------------------------

    private static final class UserData {

        private final Map<String, Entry> locations =
                new LinkedHashMap<>();

        private String favorite;
    }

    public static final class Entry {

        public final String dimension;
        public final double x;
        public final double y;
        public final double z;

        public Entry(
                String dimension,
                double x,
                double y,
                double z
        ) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}