package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.gui.FontRenderer;

public class GuiNumberInputWithText extends GuiNumberInput {

    private String suffix;

    public GuiNumberInputWithText(FontRenderer fontRenderer,
                                  int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue,
                                  boolean acceptFloats, String suffix) {

        super(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats);
        if(suffix == null) suffix = "";

        this.suffix = suffix;
    }

    @Override
    public void drawTextBox() {
        int index = getCursorPosition();
        String textBefore = getText();
        setText(textBefore + suffix);
        setCursorPosition(index);
        super.drawTextBox();
        setText(textBefore);
        setCursorPosition(index);
    }
}
