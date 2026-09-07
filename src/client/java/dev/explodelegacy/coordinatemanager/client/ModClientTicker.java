package dev.explodelegacy.coordinatemanager.client;

import dev.explodelegacy.coordinatemanager.client.gui.SavedLocationsScreen;
import dev.explodelegacy.coordinatemanager.client.gui.TextPromptScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;

public final class ModClientTicker {

    public static void init() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            /*
             * J = Save current location automatically
             *
             * Name format:
             *
             * X-Y-Z
             *
             * Example:
             * 120-64--230
             */
            while (ModKeyMappings.SAVE_CURRENT_LOCATION.consumeClick()) {

                if (client.player != null
                        && client.getConnection() != null) {

                    BlockPos pos = client.player.blockPosition();

                    String name =
                            pos.getX()
                                    + "-"
                                    + pos.getY()
                                    + "-"
                                    + pos.getZ();

                    client.getConnection().sendCommand(
                            "tc save " + name
                    );
                }
            }

            /*
             * K = Save location with custom name
             */
            while (ModKeyMappings.SAVE_LOCATION.consumeClick()) {

                if (client.gui.screen() == null) {

                    client.setScreenAndShow(
                            new TextPromptScreen(
                                    "Save current location as:",
                                    "",
                                    name -> {

                                        if (client.getConnection() != null) {

                                            client.getConnection().sendCommand(
                                                    "tc save " + name
                                            );
                                        }

                                        client.gui.setScreen(null);
                                    }
                            )
                    );
                }
            }

            /*
             * L = Open saved locations
             */
            while (ModKeyMappings.OPEN_LIST.consumeClick()) {

                if (client.gui.screen() == null) {

                    client.setScreenAndShow(
                            new SavedLocationsScreen()
                    );
                }
            }
        });
    }

    private ModClientTicker() {
    }
}