package dev.explodelegacy.coordinatemanager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TcCommand {

    private static final SuggestionProvider<CommandSourceStack>
            SAVED_LOCATION_SUGGESTIONS =
            (context, builder) -> {

                ServerPlayer player;

                try {
                    player =
                            context.getSource()
                                    .getPlayerOrException();
                } catch (Exception ignored) {
                    return CompletableFuture.completedFuture(
                            builder.build()
                    );
                }

                Map<String, SavedLocations.Entry> locations =
                        SavedLocations.all(
                                player.getUUID()
                        );

                /*
                 * Sort saved locations:
                 *
                 * 1. Names starting with letters first
                 * 2. Names starting with numbers,
                 *    '-' or other characters after
                 *
                 * Alphabetical order within each group.
                 */
                List<String> names =
                        new ArrayList<>(
                                locations.keySet()
                        );

                names.sort(
                        Comparator
                                .comparing(
                                        (String name) -> {

                                            if (name == null
                                                    || name.isEmpty()) {
                                                return 1;
                                            }

                                            char first =
                                                    name.charAt(0);

                                            return Character.isLetter(first)
                                                    ? 0
                                                    : 1;
                                        }
                                )
                                .thenComparing(
                                        String::toString,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                );

                /*
                 * Add @ back for command suggestions.
                 */
                List<String> suggestions =
                        names.stream()
                                .map(name -> "@" + name)
                                .toList();

                return SharedSuggestionProvider.suggest(
                        suggestions,
                        builder
                );
            };

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {

        dispatcher.register(
                Commands.literal("tc")

                        /*
                         * /tc
                         *
                         * Teleport to favourite.
                         */
                        .executes(
                                TcCommand::executeFavorite
                        )

                        /*
                         * /tc save <name>
                         */
                        .then(
                                Commands.literal("save")
                                        .then(
                                                Commands.argument(
                                                                "name",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                TcCommand::executeSave
                                                        )
                                        )
                        )

                        /*
                         * /tc remove <name>
                         */
                        .then(
                                Commands.literal("remove")
                                        .then(
                                                Commands.argument(
                                                                "name",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                TcCommand::executeRemove
                                                        )
                                        )
                        )

                        /*
                         * /tc list
                         */
                        .then(
                                Commands.literal("list")
                                        .executes(
                                                TcCommand::executeList
                                        )
                        )

                        /*
                         * /tc rename <old> <new>
                         */
                        .then(
                                Commands.literal("rename")
                                        .then(
                                                Commands.argument(
                                                                "old",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "new",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                TcCommand::executeRename
                                                                        )
                                                        )
                                        )
                        )

                        /*
                         * /tc favorite <name>
                         */
                        .then(
                                Commands.literal("favorite")
                                        .then(
                                                Commands.argument(
                                                                "name",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                TcCommand::executeFavoriteSet
                                                        )
                                        )
                        )

                        /*
                         * /tc <x> <y> <z>
                         */
                        .then(
                                Commands.argument(
                                                "pos",
                                                Vec3Argument.vec3()
                                        )
                                        .executes(
                                                TcCommand::executeTeleportCoords
                                        )
                        )

                        /*
                         * /tc @name
                         *
                         * greedyString() allows '@'.
                         */
                        .then(
                                Commands.argument(
                                                "saved",
                                                StringArgumentType.greedyString()
                                        )
                                        .suggests(
                                                SAVED_LOCATION_SUGGESTIONS
                                        )
                                        .executes(
                                                TcCommand::executeTeleportSaved
                                        )
                        )
        );
    }

    // ------------------------------------------------------------
    // /tc
    // ------------------------------------------------------------

    private static int executeFavorite(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String favoriteName =
                SavedLocations.getFavoriteName(
                        player.getUUID()
                );

        if (favoriteName == null
                || favoriteName.isBlank()) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "No favourite location is set."
                    )
            );

            return 0;
        }

        SavedLocations.Entry entry =
                SavedLocations.getFavorite(
                        player.getUUID()
                );

        if (entry == null) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Favourite location no longer exists."
                    )
            );

            return 0;
        }

        player.teleportTo(
                entry.x,
                entry.y,
                entry.z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Teleported to favourite @"
                                + favoriteName
                                + "  "
                                + coordinates(entry)
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // /tc save
    // ------------------------------------------------------------

    private static int executeSave(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String name =
                StringArgumentType.getString(
                        ctx,
                        "name"
                );

        String dimension =
                player.level()
                        .dimension()
                        .identifier()
                        .toString();

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        SavedLocations.set(
                player.getUUID(),
                name,
                dimension,
                x,
                y,
                z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Saved @" + name
                                + "  "
                                + coordinates(x, y, z)
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // /tc remove
    // ------------------------------------------------------------

    private static int executeRemove(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String name =
                StringArgumentType.getString(
                        ctx,
                        "name"
                );

        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        name
                );

        boolean removed =
                SavedLocations.remove(
                        player.getUUID(),
                        name
                );

        if (removed) {

            SavedLocations.Entry removedEntry = entry;

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Removed @" + name
                                    + (
                                    removedEntry != null
                                            ? "  " + coordinates(removedEntry)
                                            : ""
                            )
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "No saved location named @" + name
                )
        );

        return 0;
    }

    // ------------------------------------------------------------
    // /tc list
    // ------------------------------------------------------------

    private static int executeList(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        Map<String, SavedLocations.Entry> all =
                SavedLocations.all(
                        player.getUUID()
                );

        String favorite =
                SavedLocations.getFavoriteName(
                        player.getUUID()
                );

        if (all.isEmpty()) {

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "No saved locations."
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Saved locations:"
                ),
                false
        );

        for (Map.Entry<String, SavedLocations.Entry> location
                : all.entrySet()) {

            String name =
                    location.getKey();

            SavedLocations.Entry entry =
                    location.getValue();

            boolean isFavorite =
                    name.equals(favorite);

            String prefix =
                    isFavorite
                            ? "★ "
                            : "  ";

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            prefix
                                    + "@"
                                    + name
                                    + "  "
                                    + coordinates(entry)
                    ),
                    false
            );
        }

        String favoriteText =
                favorite == null
                        ? "(none)"
                        : "@" + favorite;

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Favourite: "
                                + favoriteText
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // /tc rename
    // ------------------------------------------------------------

    private static int executeRename(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String oldName =
                StringArgumentType.getString(
                        ctx,
                        "old"
                );

        String newName =
                StringArgumentType.getString(
                        ctx,
                        "new"
                );

        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        oldName
                );

        boolean renamed =
                SavedLocations.rename(
                        player.getUUID(),
                        oldName,
                        newName
                );

        if (renamed) {

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Renamed @"
                                    + oldName
                                    + " to @"
                                    + newName
                                    + (
                                    entry != null
                                            ? "  " + coordinates(entry)
                                            : ""
                            )
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "Could not rename @" + oldName
                )
        );

        return 0;
    }

    // ------------------------------------------------------------
    // /tc favorite
    // ------------------------------------------------------------

    private static int executeFavoriteSet(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String name =
                StringArgumentType.getString(
                        ctx,
                        "name"
                );

        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        name
                );

        boolean success =
                SavedLocations.setFavorite(
                        player.getUUID(),
                        name
                );

        if (!success) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "No saved location named @" + name
                    )
            );

            return 0;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Favourite set to @"
                                + name
                                + "  "
                                + coordinates(entry)
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // /tc @name
    // ------------------------------------------------------------

    private static int executeTeleportSaved(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String input =
                StringArgumentType.getString(
                        ctx,
                        "saved"
                );

        if (!input.startsWith("@")) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Use /tc @name for a saved location."
                    )
            );

            return 0;
        }

        String name =
                input.substring(1);

        if (name.isBlank()) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Saved location name cannot be empty."
                    )
            );

            return 0;
        }

        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        name
                );

        if (entry == null) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "No saved location named @" + name
                    )
            );

            return 0;
        }

        player.teleportTo(
                entry.x,
                entry.y,
                entry.z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Teleported to @"
                                + name
                                + "  "
                                + coordinates(entry)
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // /tc <x> <y> <z>
    // ------------------------------------------------------------

    private static int executeTeleportCoords(
            CommandContext<CommandSourceStack> ctx
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        Vec3 pos =
                Vec3Argument.getVec3(
                        ctx,
                        "pos"
                );

        player.teleportTo(
                pos.x,
                pos.y,
                pos.z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Teleported to "
                                + coordinates(
                                pos.x,
                                pos.y,
                                pos.z
                        )
                ),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    // Coordinate formatting
    // ------------------------------------------------------------

    private static String coordinates(
            SavedLocations.Entry entry
    ) {

        return coordinates(
                entry.x,
                entry.y,
                entry.z
        );
    }

    private static String coordinates(
            double x,
            double y,
            double z
    ) {

        return "X: "
                + formatNumber(x)
                + "  Y: "
                + formatNumber(y)
                + "  Z: "
                + formatNumber(z);
    }

    private static String formatNumber(
            double value
    ) {

        if (value == Math.floor(value)) {

            return String.valueOf(
                    (long) value
            );
        }

        return String.format(
                "%.2f",
                value
        );
    }
}