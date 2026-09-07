package dev.explodelegacy.coordinatemanager.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;

import java.util.function.Consumer;

public class TextPromptScreen extends Screen {

    private final String prompt;
    private final String initialText;
    private final Consumer<String> onSubmit;

    private EditBox textBox;

    public TextPromptScreen(
            String prompt,
            String initialText,
            Consumer<String> onSubmit
    ) {

        super(
                Component.literal(prompt)
        );

        this.prompt = prompt;
        this.initialText = initialText;
        this.onSubmit = onSubmit;
    }

    @Override
    protected void init() {

        super.init();

        int width = 300;
        int x =
                (this.width - width) / 2;

        int y =
                this.height / 2 - 20;

        textBox =
                new EditBox(
                        this.font,
                        x,
                        y,
                        width,
                        20,
                        Component.literal("Name")
                );

        textBox.setValue(
                initialText
        );

        textBox.setMaxLength(
                32
        );

        this.addRenderableWidget(
                textBox
        );

        /*
         * Confirm
         */
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Save"),
                        button -> submit()
                ).bounds(
                        x,
                        y + 30,
                        145,
                        20
                ).build()
        );

        /*
         * Cancel
         */
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Cancel"),
                        button -> cancel()
                ).bounds(
                        x + 155,
                        y + 30,
                        145,
                        20
                ).build()
        );

        this.setInitialFocus(
                textBox
        );

        textBox.setFocused(
                true
        );
    }

    private void submit() {

        String value =
                textBox.getValue()
                        .trim();

        if (value.isBlank()) {
            return;
        }

        onSubmit.accept(
                value
        );
    }

    private void cancel() {

        this.minecraft.gui.setScreen(
                null
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        int keyCode = event.key();

        // Enter = submit
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }

        // Escape = cancel
        if (keyCode == 256) {
            cancel();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

        int x =
                (this.width - 300) / 2;

        int y =
                this.height / 2 - 55;

        graphics.text(
                this.font,
                prompt,
                x,
                y,
                0xFFFFFFFF,
                true
        );
    }


}