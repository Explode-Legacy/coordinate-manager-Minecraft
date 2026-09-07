package dev.explodelegacy.coordinatemanager.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import static dev.explodelegacy.coordinatemanager.Coordinate.MOD_ID;

public final class ModKeyMappings {

    public static final KeyMapping.Category COORDINATE_CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "key_category"
                    )
            );

    public static KeyMapping SAVE_CURRENT_LOCATION;
    public static KeyMapping SAVE_LOCATION;
    public static KeyMapping OPEN_LIST;

    public static KeyMapping TELEPORT_FAVORITE;

    public static void init() {

        /*
         * J = Save current location automatically
         */
        SAVE_CURRENT_LOCATION =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.coordinate.save_current_location",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_J,
                                COORDINATE_CATEGORY
                        )
                );

        /*
         * K = Save location with custom name
         */
        SAVE_LOCATION =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.coordinate.save_location",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_K,
                                COORDINATE_CATEGORY
                        )
                );

        /*
         * L = Open saved locations
         */
        OPEN_LIST =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.coordinate.open_list",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_L,
                                COORDINATE_CATEGORY
                        )
                );

        TELEPORT_FAVORITE =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.coordinate.teleport_favorite",
                                InputConstants.Type.KEYSYM,
                                InputConstants.KEY_H,
                                COORDINATE_CATEGORY
                        )
                );
    }

    private ModKeyMappings() {
    }
}