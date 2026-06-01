package bisq.desktop.components.controls;

/**
 * Pure-JavaFX-base mirror of {@code bisq.desktop.components.AutoTooltipCheckBox}.
 * Extends {@link BisqJfxCheckBox} so the production wrapper's auto-tooltip behaviour can be
 * compared against jfoenix-based AutoTooltipCheckBox at the same level of customization.
 */
public class BisqAutoTooltipCheckBox extends BisqJfxCheckBox {

    public BisqAutoTooltipCheckBox() {
        super();
    }

    public BisqAutoTooltipCheckBox(String text) {
        super(text);
    }

}
