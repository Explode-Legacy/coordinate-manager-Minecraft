package dev.explodelegacy.coordinatemanager.client.gui;

import dev.explodelegacy.coordinatemanager.SavedLocations;
import dev.explodelegacy.coordinatemanager.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SavedLocationsScreen extends Screen {

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 270;

    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 34;

    private static final int ROW_HEIGHT = 48;
    private static final int MAX_VISIBLE_ROWS = 4;

    private static final int INFO_WIDTH = 210;

    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 3;

    private final Minecraft client;

    private List<Map.Entry<String, SavedLocations.Entry>> locations =
            new ArrayList<>();

    private int scrollOffset = 0;

    public SavedLocationsScreen() {
        super(Component.literal("Saved Locations"));
        this.client = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        loadLocations();
        rebuildButtons();
    }

    /**
     * Load saved locations and sort them.
     *
     * Ordering:
     *
     * 1. Names beginning with @
     * 2. Normal names
     *
     * Inside each group, names are alphabetical.
     */
    private void loadLocations() {

        locations.clear();

        if (client.player == null) {
            return;
        }

        UUID uuid = client.player.getUUID();

        locations.addAll(
                SavedLocations.all(uuid).entrySet()
        );

        locations.sort(
                Comparator
                        .comparing(
                                (Map.Entry<String, SavedLocations.Entry> entry) ->
                                        !entry.getKey().startsWith("@")
                        )
                        .thenComparing(
                                entry -> entry.getKey(),
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        int maxScroll =
                Math.max(
                        0,
                        locations.size() - MAX_VISIBLE_ROWS
                );

        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    /**
     * Rebuild all screen widgets.
     *
     * The background panel is added FIRST.
     * Buttons are added AFTER it so they render above the panel.
     */
    private void rebuildButtons() {

        clearWidgets();

        /*
         * Background / rows panel.
         *
         * This must be added before the buttons.
         */
        addRenderableOnly(
                new LocationPanelWidget()
        );

        int visibleCount =
                Math.min(
                        MAX_VISIBLE_ROWS,
                        Math.max(
                                0,
                                locations.size() - scrollOffset
                        )
                );

        for (int i = 0; i < visibleCount; i++) {

            int index = scrollOffset + i;

            Map.Entry<String, SavedLocations.Entry> location =
                    locations.get(index);

            String name = location.getKey();

            int rowY =
                    panelY()
                            + HEADER_HEIGHT
                            + i * ROW_HEIGHT;

            int actionsX =
                    panelX()
                            + PADDING
                            + INFO_WIDTH;

            /*
             * GO
             */
            addRenderableWidget(
                    Button.builder(
                            Component.literal("Go"),
                            button -> goTo(name)
                    ).bounds(
                            actionsX,
                            rowY + 2,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    ).build()
            );

            /*
             * RENAME
             */
            addRenderableWidget(
                    Button.builder(
                            Component.literal("Rename"),
                            button -> rename(name)
                    ).bounds(
                            actionsX + BUTTON_WIDTH + BUTTON_GAP,
                            rowY + 2,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    ).build()
            );

            /*
             * DELETE
             */
            addRenderableWidget(
                    Button.builder(
                            Component.literal("Delete"),
                            button -> delete(name)
                    ).bounds(
                            actionsX,
                            rowY + 24,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    ).build()
            );

            /*
             * FAVOURITE
             */
            addRenderableWidget(
                    Button.builder(
                            Component.literal("★ Fav"),
                            button -> favourite(name)
                    ).bounds(
                            actionsX + BUTTON_WIDTH + BUTTON_GAP,
                            rowY + 24,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    ).build()
            );
        }
    }

    private void goTo(String name) {

        if (client.getConnection() != null) {

            client.getConnection().sendCommand(
                    "tc @" + name
            );
        }

        client.gui.setScreen(null);
    }

    private void rename(String oldName) {

        SavedLocations.Entry entry =
                getEntry(oldName);

        String coordinates =
                entry == null
                        ? ""
                        : formatCoordinates(entry);

        client.setScreenAndShow(
                new TextPromptScreen(
                        "Rename " + oldName
                                + " (" + coordinates + ")"
                                + " to:",
                        oldName,
                        newName -> {

                            if (newName == null
                                    || newName.isBlank()
                                    || newName.equals(oldName)) {

                                client.gui.setScreen(
                                        new SavedLocationsScreen()
                                );

                                return;
                            }

                            if (client.getConnection() != null) {

                                client.getConnection().sendCommand(
                                        "tc rename "
                                                + oldName
                                                + " "
                                                + newName
                                );
                            }

                            client.gui.setScreen(
                                    new SavedLocationsScreen()
                            );
                        }
                )
        );
    }

    private void delete(String name) {

        if (client.getConnection() != null) {

            client.getConnection().sendCommand(
                    "tc remove " + name
            );
        }

        loadLocations();
        rebuildButtons();
    }

    private void favourite(String name) {

        if (client.getConnection() != null) {

            client.getConnection().sendCommand(
                    "tc favorite " + name
            );
        }

        loadLocations();
        rebuildButtons();
    }

    private SavedLocations.Entry getEntry(String name) {

        if (client.player == null) {
            return null;
        }

        return SavedLocations.get(
                client.player.getUUID(),
                name
        );
    }

    private String formatCoordinates(
            SavedLocations.Entry entry
    ) {

        return "X:"
                + formatNumber(entry.x)
                + "  Y:"
                + formatNumber(entry.y)
                + "  Z:"
                + formatNumber(entry.z);
    }

    private String formatNumber(double value) {

        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }

        return String.format(
                "%.2f",
                value
        );
    }

    private int panelX() {

        return (width - PANEL_WIDTH) / 2;
    }

    private int panelY() {

        return (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                partialTicks
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double deltaX,
            double deltaY
    ) {

        if (locations.size() <= MAX_VISIBLE_ROWS) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    deltaX,
                    deltaY
            );
        }

        if (deltaY < 0) {

            scrollOffset =
                    Math.min(
                            scrollOffset + 1,
                            locations.size()
                                    - MAX_VISIBLE_ROWS
                    );

            rebuildButtons();

            return true;
        }

        if (deltaY > 0) {

            scrollOffset =
                    Math.max(
                            scrollOffset - 1,
                            0
                    );

            rebuildButtons();

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                deltaX,
                deltaY
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {

        return false;
    }

    /**
     * Render-only panel.
     *
     * This is deliberately NOT used for the yellow selection
     * border. Rows always use neutral gray borders.
     */
    private class LocationPanelWidget extends AbstractWidget {

        public LocationPanelWidget() {

            super(
                    panelX(),
                    panelY(),
                    PANEL_WIDTH,
                    PANEL_HEIGHT,
                    Component.empty()
            );

            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTicks
        ) {

            int x = getX();
            int y = getY();

            /*
             * Main panel.
             */
            graphics.fill(
                    x,
                    y,
                    x + PANEL_WIDTH,
                    y + PANEL_HEIGHT,
                    0xE6101010
            );

            /*
             * Main panel border.
             */
            graphics.outline(
                    x,
                    y,
                    PANEL_WIDTH,
                    PANEL_HEIGHT,
                    0xFF555555
            );

            /*
             * Header.
             */
            graphics.text(
                    Minecraft.getInstance().font,
                    Component.literal("Saved Locations"),
                    x + PADDING,
                    y + 10,
                    0xFFFFFFFF
            );

            graphics.text(
                    Minecraft.getInstance().font,
                    Component.literal(
                            locations.size()
                                    + " saved"
                    ),
                    x + PANEL_WIDTH
                            - PADDING
                            - Minecraft.getInstance()
                            .font
                            .width(
                                    locations.size()
                                            + " saved"
                            ),
                    y + 10,
                    0xFFAAAAAA
            );

            /*
             * Rows.
             */
            int visibleCount =
                    Math.min(
                            MAX_VISIBLE_ROWS,
                            Math.max(
                                    0,
                                    locations.size()
                                            - scrollOffset
                            )
                    );

            for (int i = 0; i < visibleCount; i++) {

                int index =
                        scrollOffset + i;

                Map.Entry<String, SavedLocations.Entry>
                        location =
                        locations.get(index);

                String name =
                        location.getKey();

                SavedLocations.Entry entry =
                        location.getValue();

                int rowY =
                        y
                                + HEADER_HEIGHT
                                + i * ROW_HEIGHT;

                /*
                 * Normal row background.
                 */
                graphics.fill(
                        x + PADDING,
                        rowY,
                        x + PANEL_WIDTH - PADDING,
                        rowY + ROW_HEIGHT - 4,
                        0xFF161616
                );

                /*
                 * NORMAL GRAY BORDER.
                 *
                 * No yellow selection line.
                 */
                graphics.outline(
                        x + PADDING,
                        rowY,
                        PANEL_WIDTH - PADDING * 2,
                        ROW_HEIGHT - 4,
                        0xFF303030
                );

                /*
                 * Favourite.
                 */
                boolean favourite =
                        client.player != null
                                && name.equals(
                                SavedLocations
                                        .getFavoriteName(
                                                client.player
                                                        .getUUID()
                                        )
                        );

                /*
                 * Location name.
                 *
                 * Favourite = gold.
                 * Normal = white.
                 */
                int nameColor =
                        favourite
                                ? 0xFFFFD700
                                : 0xFFFFFFFF;

                String displayName =
                        (favourite ? "★ " : "")
                                + "@"
                                + name;

                graphics.text(
                        Minecraft.getInstance().font,
                        Component.literal(displayName),
                        x + PADDING + 8,
                        rowY + 8,
                        nameColor
                );

                /*
                 * Coordinates.
                 */
                String coordinates =
                        formatCoordinates(entry);

                graphics.text(
                        Minecraft.getInstance().font,
                        Component.literal(coordinates),
                        x + PADDING + 8,
                        rowY + 31,
                        0xFF55FFFF
                );
            }

            /*
             * Scroll indicator.
             */
            if (locations.size() > MAX_VISIBLE_ROWS) {

                String scrollText =
                        (scrollOffset + 1)
                                + "/"
                                + (
                                locations.size()
                                        - MAX_VISIBLE_ROWS
                                        + 1
                        );

                graphics.text(
                        Minecraft.getInstance().font,
                        Component.literal(scrollText),
                        x + PANEL_WIDTH
                                - PADDING
                                - Minecraft.getInstance()
                                .font
                                .width(scrollText),
                        y + PANEL_HEIGHT - 14,
                        0xFF777777
                );
            }
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration
        ) {
        }
    }
}