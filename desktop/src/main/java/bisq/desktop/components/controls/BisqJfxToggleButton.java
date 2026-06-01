package bisq.desktop.components.controls;

import bisq.desktop.components.controls.skin.BisqToggleButtonSkin;

import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;

/** Drop-in replacement for {@code com.jfoenix.controls.JFXToggleButton}. */
public class BisqJfxToggleButton extends ToggleButton {

    public BisqJfxToggleButton() {
        super();
        getStyleClass().add("jfx-toggle-button");
    }

    public BisqJfxToggleButton(String text) {
        super(text);
        getStyleClass().add("jfx-toggle-button");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new BisqToggleButtonSkin(this);
    }
}
