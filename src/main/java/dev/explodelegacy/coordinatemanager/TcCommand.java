package dev.explodelegacy.coordinatemanager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TcCommand {

    // ============================================================
    // SAVED LOCATION SUGGESTIONS
    //
    // /tc #<TAB>
    //
    // Alphabetic names first.
    // Numeric / '-' names afterwards.
    // ============================================================

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

                List<String> suggestions =
                        names.stream()
                                .map(name -> "#" + name)
                                .toList();

                return SharedSuggestionProvider.suggest(
                        suggestions,
                        builder
                );
            };


    // ============================================================
    // PLAYER SUGGESTIONS
    //
    // /tc @<TAB>
    // ============================================================

    private static final SuggestionProvider<CommandSourceStack>
            PLAYER_SUGGESTIONS =
            (context, builder) -> {

                List<String> suggestions =
                        context.getSource()
                                .getOnlinePlayerNames()
                                .stream()
                                .map(name -> "@" + name)
                                .sorted(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .toList();

                return SharedSuggestionProvider.suggest(
                        suggestions,
                        builder
                );
            };


    // ============================================================
    // PLAYER OR SAVED LOCATION SUGGESTIONS
    //
    // /tc @Steve @<TAB>
    // /tc @Steve #<TAB>
    // ============================================================

    private static final SuggestionProvider<CommandSourceStack>
            PLAYER_OR_SAVED_SUGGESTIONS =
            (context, builder) -> {

                List<String> suggestions =
                        new ArrayList<>();

                /*
                 * Online players
                 */
                for (String name :
                        context.getSource()
                                .getOnlinePlayerNames()) {

                    suggestions.add(
                            "@" + name
                    );
                }

                /*
                 * Saved locations
                 */
                try {

                    ServerPlayer player =
                            context.getSource()
                                    .getPlayerOrException();

                    Map<String, SavedLocations.Entry>
                            locations =
                            SavedLocations.all(
                                    player.getUUID()
                            );

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

                    for (String name : names) {

                        suggestions.add(
                                "#" + name
                        );
                    }

                } catch (Exception ignored) {
                }

                return SharedSuggestionProvider.suggest(
                        suggestions,
                        builder
                );
            };


    // ============================================================
    // COMMAND REGISTRATION
    // ============================================================

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {

        dispatcher.register(
                Commands.literal("tc")

                        // ------------------------------------------------
                        // /tc
                        // Teleport yourself to favourite
                        // ------------------------------------------------

                        .executes(
                                TcCommand::executeFavorite
                        )


                        // ------------------------------------------------
                        // /tc save <name>
                        // ------------------------------------------------

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


                        // ------------------------------------------------
                        // /tc remove <name>
                        // ------------------------------------------------

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


                        // ------------------------------------------------
                        // /tc list
                        // ------------------------------------------------

                        .then(
                                Commands.literal("list")
                                        .executes(
                                                TcCommand::executeList
                                        )
                        )


                        // ------------------------------------------------
                        // /tc rename <old> <new>
                        // ------------------------------------------------

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


                        // ------------------------------------------------
                        // /tc favorite <name>
                        // ------------------------------------------------

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


                        // ------------------------------------------------
                        // /tc <x> <y> <z>
                        // ------------------------------------------------

                        .then(
                                Commands.argument(
                                                "x",
                                                DoubleArgumentType.doubleArg()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "y",
                                                                DoubleArgumentType.doubleArg()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "z",
                                                                                DoubleArgumentType.doubleArg()
                                                                        )
                                                                        .executes(
                                                                                TcCommand::executeTeleportCoords
                                                                        )
                                                        )
                                        )
                        )


                        // ------------------------------------------------
                        // /tc @player
                        // /tc #saved
                        // ------------------------------------------------

                        .then(
                                Commands.argument(
                                                "target",
                                                StringArgumentType.word()
                                        )
                                        .suggests(
                                                PLAYER_OR_SAVED_SUGGESTIONS
                                        )
                                        .executes(
                                                TcCommand::executeTeleportTarget
                                        )

                                        // --------------------------------
                                        // /tc @player @player
                                        // /tc @player #saved
                                        // --------------------------------

                                        .then(
                                                Commands.argument(
                                                                "destination",
                                                                StringArgumentType.word()
                                                        )
                                                        .suggests(
                                                                PLAYER_OR_SAVED_SUGGESTIONS
                                                        )
                                                        .executes(
                                                                TcCommand::executeTeleportTargetToTarget
                                                        )
                                        )

                                        // --------------------------------
                                        // /tc @player <x> <y> <z>
                                        // --------------------------------

                                        .then(
                                                Commands.argument(
                                                                "x",
                                                                DoubleArgumentType.doubleArg()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "y",
                                                                                DoubleArgumentType.doubleArg()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "z",
                                                                                                DoubleArgumentType.doubleArg()
                                                                                        )
                                                                                        .executes(
                                                                                                TcCommand::executeTeleportPlayerToCoords
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }


    // ============================================================
    // /tc
    // ============================================================

    private static int executeFavorite(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

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
                        "Teleported to favourite #"
                                + favoriteName
                                + "  "
                                + coordinates(entry)
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // /tc save
    // ============================================================

    private static int executeSave(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

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

        double x =
                player.getX();

        double y =
                player.getY();

        double z =
                player.getZ();

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
                        "Saved #"
                                + name
                                + "  "
                                + coordinates(x, y, z)
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // /tc remove
    // ============================================================

    private static int executeRemove(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        String name =
                StringArgumentType.getString(
                        ctx,
                        "name"
                );

        /*
         * Get the entry BEFORE removing it so we can
         * display its coordinates.
         */
        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        name
                );

        /*
         * Your SavedLocations.remove() returns boolean.
         */
        boolean removed =
                SavedLocations.remove(
                        player.getUUID(),
                        name
                );

        if (removed) {

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Removed #"
                                    + name
                                    + (
                                    entry != null
                                            ? "  "
                                            + coordinates(entry)
                                            : ""
                            )
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "No saved location named #"
                                + name
                ));

        return 0;
    }


    // ============================================================
    // /tc list
    // ============================================================

    private static int executeList(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

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

        /*
         * Same ordering as GUI and TAB:
         *
         * alphabetic names first
         * numeric / '-' names afterwards
         */
        List<Map.Entry<String, SavedLocations.Entry>>
                locations =
                new ArrayList<>(
                        all.entrySet()
                );

        locations.sort(
                Comparator
                        .comparing(
                                (Map.Entry<String, SavedLocations.Entry> entry) -> {

                                    String name =
                                            entry.getKey();

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
                                entry -> entry.getKey(),
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        for (
                Map.Entry<String, SavedLocations.Entry> location
                : locations
        ) {

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
                                    + "#"
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
                        : "#" + favorite;

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Favourite: "
                                + favoriteText
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // /tc rename
    // ============================================================

    private static int executeRename(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

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

        /*
         * Get old entry BEFORE renaming so we can
         * display the coordinates.
         */
        SavedLocations.Entry entry =
                SavedLocations.get(
                        player.getUUID(),
                        oldName
                );

        /*
         * Your SavedLocations.rename() returns boolean.
         */
        boolean renamed =
                SavedLocations.rename(
                        player.getUUID(),
                        oldName,
                        newName
                );

        if (renamed) {

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Renamed #"
                                    + oldName
                                    + " to #"
                                    + newName
                                    + (
                                    entry != null
                                            ? "  "
                                            + coordinates(entry)
                                            : ""
                            )
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "Could not rename #"
                                + oldName
                )
        );

        return 0;
    }


    // ============================================================
    // /tc favorite
    // ============================================================

    private static int executeFavoriteSet(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

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
                            "No saved location named #"
                                    + name
                    )
            );

            return 0;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Favourite set to #"
                                + name
                                + "  "
                                + coordinates(entry)
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // /tc @player
    // /tc #saved
    // ============================================================

    private static int executeTeleportTarget(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        ServerPlayer source =
                ctx.getSource()
                        .getPlayerOrException();

        String target =
                StringArgumentType.getString(
                        ctx,
                        "target"
                );

        /*
         * ========================================================
         * #saved
         *
         * Yourself -> saved location
         * ========================================================
         */
        if (target.startsWith("#")) {

            String name =
                    target.substring(1);

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
                            source.getUUID(),
                            name
                    );

            if (entry == null) {

                ctx.getSource().sendFailure(
                        Component.literal(
                                "No saved location named #"
                                        + name
                        )
                );

                return 0;
            }

            source.teleportTo(
                    entry.x,
                    entry.y,
                    entry.z
            );

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Teleported to #"
                                    + name
                                    + "  "
                                    + coordinates(entry)
                    ),
                    false
            );

            return 1;
        }


        /*
         * ========================================================
         * @player
         *
         * Yourself -> player
         * ========================================================
         */
        if (target.startsWith("@")) {

            String playerName =
                    target.substring(1);

            ServerPlayer destination =
                    findPlayer(
                            ctx,
                            playerName
                    );

            if (destination == null) {
                return 0;
            }

            source.teleportTo(
                    destination.getX(),
                    destination.getY(),
                    destination.getZ()
            );

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Teleported to @"
                                    + destination
                                    .getGameProfile()
                                    .name()
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "Use @player or #saved-location."
                )
        );

        return 0;
    }


    // ============================================================
    // /tc @player @player
    // /tc @player #saved
    // ============================================================

    private static int executeTeleportTargetToTarget(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        String targetInput =
                StringArgumentType.getString(
                        ctx,
                        "target"
                );

        String destinationInput =
                StringArgumentType.getString(
                        ctx,
                        "destination"
                );

        /*
         * First argument must be @player.
         */
        if (!targetInput.startsWith("@")) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "The first target must be @player."
                    )
            );

            return 0;
        }

        ServerPlayer target =
                findPlayer(
                        ctx,
                        targetInput.substring(1)
                );

        if (target == null) {
            return 0;
        }


        /*
         * ========================================================
         * @player -> @player
         * ========================================================
         */
        if (destinationInput.startsWith("@")) {

            ServerPlayer destination =
                    findPlayer(
                            ctx,
                            destinationInput.substring(1)
                    );

            if (destination == null) {
                return 0;
            }

            target.teleportTo(
                    destination.getX(),
                    destination.getY(),
                    destination.getZ()
            );

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Teleported @"
                                    + target
                                    .getGameProfile()
                                    .name()
                                    + " to @"
                                    + destination
                                    .getGameProfile()
                                    .name()
                    ),
                    false
            );

            return 1;
        }


        /*
         * ========================================================
         * @player -> #saved
         *
         * Saved location belongs to command executor.
         * ========================================================
         */
        if (destinationInput.startsWith("#")) {

            String name =
                    destinationInput.substring(1);

            ServerPlayer source =
                    ctx.getSource()
                            .getPlayerOrException();

            SavedLocations.Entry entry =
                    SavedLocations.get(
                            source.getUUID(),
                            name
                    );

            if (entry == null) {

                ctx.getSource().sendFailure(
                        Component.literal(
                                "No saved location named #"
                                        + name
                        )
                );

                return 0;
            }

            target.teleportTo(
                    entry.x,
                    entry.y,
                    entry.z
            );

            ctx.getSource().sendSuccess(
                    () -> Component.literal(
                            "Teleported @"
                                    + target
                                    .getGameProfile()
                                    .name()
                                    + " to #"
                                    + name
                                    + "  "
                                    + coordinates(entry)
                    ),
                    false
            );

            return 1;
        }

        ctx.getSource().sendFailure(
                Component.literal(
                        "Destination must be @player or #saved-location."
                )
        );

        return 0;
    }


    // ============================================================
    // /tc @player <x> <y> <z>
    // ============================================================

    private static int executeTeleportPlayerToCoords(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        String targetInput =
                StringArgumentType.getString(
                        ctx,
                        "target"
                );

        if (!targetInput.startsWith("@")) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "The target must be @player."
                    )
            );

            return 0;
        }

        ServerPlayer target =
                findPlayer(
                        ctx,
                        targetInput.substring(1)
                );

        if (target == null) {
            return 0;
        }

        double x =
                DoubleArgumentType.getDouble(
                        ctx,
                        "x"
                );

        double y =
                DoubleArgumentType.getDouble(
                        ctx,
                        "y"
                );

        double z =
                DoubleArgumentType.getDouble(
                        ctx,
                        "z"
                );

        target.teleportTo(
                x,
                y,
                z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Teleported @"
                                + target
                                .getGameProfile()
                                .name()
                                + " to "
                                + coordinates(x, y, z)
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // /tc <x> <y> <z>
    // ============================================================

    private static int executeTeleportCoords(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        ServerPlayer player =
                ctx.getSource()
                        .getPlayerOrException();

        double x =
                DoubleArgumentType.getDouble(
                        ctx,
                        "x"
                );

        double y =
                DoubleArgumentType.getDouble(
                        ctx,
                        "y"
                );

        double z =
                DoubleArgumentType.getDouble(
                        ctx,
                        "z"
                );

        player.teleportTo(
                x,
                y,
                z
        );

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Teleported to "
                                + coordinates(
                                x,
                                y,
                                z
                        )
                ),
                false
        );

        return 1;
    }


    // ============================================================
    // FIND ONLINE PLAYER
    // ============================================================

    private static ServerPlayer findPlayer(
            CommandContext<CommandSourceStack> ctx,
            String name
    ) {

        if (name == null || name.isBlank()) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Player name cannot be empty."
                    )
            );

            return null;
        }

        ServerPlayer player =
                ctx.getSource()
                        .getServer()
                        .getPlayerList()
                        .getPlayerByName(name);

        if (player == null) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Player @" + name
                                    + " is not online."
                    )
            );

            return null;
        }

        return player;
    }


    // ============================================================
    // COORDINATE FORMATTING
    // ============================================================

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