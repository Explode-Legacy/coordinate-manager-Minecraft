package dev.explodelegacy.coordinatemanager.client;

import net.fabricmc.api.ClientModInitializer;

public class CoordinateManagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModKeyMappings.init();
        ModClientTicker.init();
    }
}