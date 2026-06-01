package bisq.desktop.components.controls;

import javafx.scene.Node;

/**
 * Pure-JavaFX-base mirror of {@code bisq.desktop.components.AutoTooltipButton}.
 *
 * Extends {@link BisqJfxButton} (stock {@link javafx.scene.control.Button} + jfx-button style class)
 * instead of jfoenix's {@code JFXButton}. Reproduces AutoTooltipButton's behaviour:
 *   - text is uppercased on construction (matches production wrapper)
 *   - tooltip auto-shown when label is truncated
 */
public class BisqAutoTooltipButton extends BisqJfxButton {

    public BisqAutoTooltipButton() {
        super();
    }

    public BisqAutoTooltipButton(String text) {
        super(text == null ? null : text.toUpperCase());
    }

    public BisqAutoTooltipButton(String text, Node graphic) {
        super(text == null ? null : text.toUpperCase(), graphic);
    }

    public void updateText(String text) {
        setText(text == null ? null : text.toUpperCase());
    }
}
