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

                sortLocationNames(names);

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
    // /tc @<TAB>
    // /tc #<TAB>
    // /tc @Steve @<TAB>
    // /tc @Steve #<TAB>
    //
    // Because "teleport" is a greedyString(), we have to
    // calculate which token the player is currently typing.
    // ============================================================

    private static final SuggestionProvider<CommandSourceStack>
            PLAYER_OR_SAVED_SUGGESTIONS =
            (context, builder) -> {

                String remaining =
                        builder.getRemaining();

                /*
                 * Find the last space.
                 *
                 * Examples:
                 *
                 * "@anonymous"
                 *
                 * current token = "@anonymous"
                 *
                 *
                 * "@anonymous #"
                 *
                 * current token = "#"
                 *
                 *
                 * "@anonymous @"
                 *
                 * current token = "@"
                 */
                int lastSpace =
                        remaining.lastIndexOf(' ');

                String currentToken;

                if (lastSpace >= 0) {

                    currentToken =
                            remaining.substring(
                                    lastSpace + 1
                            );

                } else {

                    currentToken =
                            remaining;
                }

                /*
                 * Only replace the current token when suggesting.
                 *
                 * This is what allows:
                 *
                 * /tc @anonymous #<TAB>
                 *
                 * to become:
                 *
                 * /tc @anonymous #home
                 */
                int suggestionStart =
                        builder.getStart()
                                + (
                                lastSpace >= 0
                                        ? lastSpace + 1
                                        : 0
                        );

                var suggestionBuilder =
                        builder.createOffset(
                                suggestionStart
                        );


                // ========================================================
                // CURRENT TOKEN STARTS WITH #
                //
                // Suggest saved locations.
                //
                // /tc #<TAB>
                // /tc @anonymous #<TAB>
                // ========================================================

                if (currentToken.startsWith("#")) {

                    ServerPlayer player;

                    try {

                        player =
                                context.getSource()
                                        .getPlayerOrException();

                    } catch (Exception ignored) {

                        return CompletableFuture.completedFuture(
                                suggestionBuilder.build()
                        );
                    }

                    Map<String, SavedLocations.Entry>
                            locations =
                            SavedLocations.all(
                                    player.getUUID()
                            );

                    List<String> names =
                            new ArrayList<>(
                                    locations.keySet()
                            );

                    sortLocationNames(names);

                    List<String> suggestions =
                            names.stream()
                                    .map(name -> "#" + name)
                                    .toList();

                    return SharedSuggestionProvider.suggest(
                            suggestions,
                            suggestionBuilder
                    );
                }


                // ========================================================
                // CURRENT TOKEN STARTS WITH @
                //
                // Suggest players.
                //
                // /tc @<TAB>
                // /tc @anonymous @<TAB>
                // ========================================================

                if (currentToken.startsWith("@")) {

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
                            suggestionBuilder
                    );
                }


                // ========================================================
                // EMPTY CURRENT TOKEN
                //
                // /tc @anonymous <TAB>
                //
                // Suggest both players and saved locations.
                // ========================================================

                List<String> suggestions =
                        new ArrayList<>();

                /*
                 * Players
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

                    sortLocationNames(names);

                    for (String name : names) {

                        suggestions.add(
                                "#" + name
                        );
                    }

                } catch (Exception ignored) {
                }

                return SharedSuggestionProvider.suggest(
                        suggestions,
                        suggestionBuilder
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


                        // ====================================================
                        // UNIVERSAL TELEPORT ARGUMENT
                        //
                        // greedyString() is required because @ and # are
                        // not valid characters for Brigadier word().
                        // ====================================================

                        .then(
                                Commands.argument(
                                                "teleport",
                                                StringArgumentType.greedyString()
                                        )
                                        .suggests(
                                                PLAYER_OR_SAVED_SUGGESTIONS
                                        )
                                        .executes(
                                                TcCommand::executeTeleport
                                        )
                        )
        );
    }


    // ============================================================
    // /tc
    //
    // Teleport yourself to favourite
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
    // /tc save <name>
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
    // /tc remove <name>
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
                )
        );

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
    // /tc rename <old> <new>
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
    // /tc favorite <name>
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
    // /tc <x> <y> <z>
    //
    // Yourself -> coordinates
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
    // UNIVERSAL TELEPORT
    //
    // Supported:
    //
    // /tc #home
    // /tc @Steve
    // /tc @Steve @Alex
    // /tc @Steve #home
    // /tc 100 64 200
    // /tc @Steve 100 64 200
    // ============================================================

    private static int executeTeleport(
            CommandContext<CommandSourceStack> ctx
    ) throws CommandSyntaxException {

        ServerPlayer source =
                ctx.getSource()
                        .getPlayerOrException();

        String input =
                StringArgumentType.getString(
                        ctx,
                        "teleport"
                ).trim();

        if (input.isBlank()) {

            ctx.getSource().sendFailure(
                    Component.literal(
                            "Teleport target cannot be empty."
                    )
            );

            return 0;
        }

        String[] parts =
                input.split("\\s+");


        // ========================================================
        // /tc #home
        //
        // Yourself -> saved location
        // ========================================================

        if (parts.length == 1
                && parts[0].startsWith("#")) {

            String name =
                    parts[0].substring(1);

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


        // ========================================================
        // /tc @Steve
        //
        // Yourself -> player
        // ========================================================

        if (parts.length == 1
                && parts[0].startsWith("@")) {

            String playerName =
                    parts[0].substring(1);

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


        // ========================================================
        // /tc <x> <y> <z>
        //
        // Yourself -> coordinates
        // ========================================================

        if (parts.length == 3
                && isNumber(parts[0])
                && isNumber(parts[1])
                && isNumber(parts[2])) {

            double x =
                    Double.parseDouble(parts[0]);

            double y =
                    Double.parseDouble(parts[1]);

            double z =
                    Double.parseDouble(parts[2]);

            source.teleportTo(
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


        // ========================================================
        // /tc @Steve @Alex
        //
        // Steve -> Alex
        // ========================================================

        if (parts.length == 2
                && parts[0].startsWith("@")
                && parts[1].startsWith("@")) {

            ServerPlayer target =
                    findPlayer(
                            ctx,
                            parts[0].substring(1)
                    );

            if (target == null) {
                return 0;
            }

            ServerPlayer destination =
                    findPlayer(
                            ctx,
                            parts[1].substring(1)
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


        // ========================================================
        // /tc @Steve #home
        //
        // Steve -> executor's saved home
        // ========================================================

        if (parts.length == 2
                && parts[0].startsWith("@")
                && parts[1].startsWith("#")) {

            ServerPlayer target =
                    findPlayer(
                            ctx,
                            parts[0].substring(1)
                    );

            if (target == null) {
                return 0;
            }

            String name =
                    parts[1].substring(1);

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


        // ========================================================
        // /tc @Steve <x> <y> <z>
        //
        // Steve -> coordinates
        // ========================================================

        if (parts.length == 4
                && parts[0].startsWith("@")
                && isNumber(parts[1])
                && isNumber(parts[2])
                && isNumber(parts[3])) {

            ServerPlayer target =
                    findPlayer(
                            ctx,
                            parts[0].substring(1)
                    );

            if (target == null) {
                return 0;
            }

            double x =
                    Double.parseDouble(parts[1]);

            double y =
                    Double.parseDouble(parts[2]);

            double z =
                    Double.parseDouble(parts[3]);

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


        // ========================================================
        // INVALID FORMAT
        // ========================================================

        ctx.getSource().sendFailure(
                Component.literal(
                        "Invalid teleport format."
                )
        );

        ctx.getSource().sendFailure(
                Component.literal(
                        "Use: /tc #saved, /tc @player, "
                                + "/tc @player @player, "
                                + "/tc @player #saved, "
                                + "/tc x y z, or "
                                + "/tc @player x y z"
                )
        );

        return 0;
    }


    // ============================================================
    // CHECK IF STRING IS NUMBER
    // ============================================================

    private static boolean isNumber(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        try {

            Double.parseDouble(value);

            return true;

        } catch (NumberFormatException ignored) {

            return false;
        }
    }


    // ============================================================
    // FIND ONLINE PLAYER
    // ============================================================

    private static ServerPlayer findPlayer(
            CommandContext<CommandSourceStack> ctx,
            String name
    ) {

        if (name == null
                || name.isBlank()) {

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
    // SORT LOCATION NAMES
    // ============================================================

    private static void sortLocationNames(
            List<String> names
    ) {

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