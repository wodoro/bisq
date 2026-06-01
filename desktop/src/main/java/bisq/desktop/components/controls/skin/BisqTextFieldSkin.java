package bisq.desktop.components.controls.skin;

import bisq.desktop.components.controls.LabelFloatable;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Pure-JavaFX replacement for {@code bisq.desktop.components.JFXTextFieldSkinBisqStyle}.
 *
 * Adds two bottom-border regions matching the original jfoenix visual:
 *   - {@code .input-line}: always-visible 1px underline.
 *   - {@code .input-focused-line}: thicker underline that scales in on focus.
 *
 * The CSS rules in {@code theme-{dark,light}.css} already target these style classes; this skin
 * just installs the nodes and animates {@code scaleX} between 0 (unfocused) and 1 (focused).
 *
 * Floating prompt label is NOT reproduced — pixel diff against jfoenix's floating label is
 * deferred until needed.
 */
public class BisqTextFieldSkin extends TextFieldSkin {

    private static final Duration ANIM_DURATION = Duration.millis(200);
    private static final double LINE_HEIGHT = 1;
    private static final double FOCUSED_LINE_HEIGHT = 2;

    private final Region line = new Region();
    private final Region focusedLine = new Region();
    private final Label topLabel = new Label();
    private final Timeline focusAnim = new Timeline();
    private final Timeline floatAnim = new Timeline();
    private final javafx.scene.transform.Scale promptScaleTransform =
            new javafx.scene.transform.Scale(1, 1, 0, 0);
    private final javafx.beans.property.DoubleProperty promptOffsetY =
            new javafx.beans.property.SimpleDoubleProperty(0);
    private final double inputLineExtension;
    private final BooleanBinding showTopLabel;

    public BisqTextFieldSkin(TextField control) {
        this(control, 0);
    }

    public BisqTextFieldSkin(TextField control, double inputLineExtension) {
        super(control);
        this.inputLineExtension = inputLineExtension;

        line.getStyleClass().add("input-line");
        line.setManaged(false);
        line.setPrefHeight(LINE_HEIGHT);

        focusedLine.getStyleClass().add("input-focused-line");
        focusedLine.setManaged(false);
        focusedLine.setPrefHeight(FOCUSED_LINE_HEIGHT);
        focusedLine.setScaleX(0);

        // Floating prompt label — when labelFloat=true the label is always present and animates
        // between two positions: INSIDE the input (idle, empty, full size, prompt colour) and
        // ABOVE the input (focused or filled, 0.85 scale, focus colour). Stock prompt is hidden
        // for label-float controls via the .jfx-text-field:floats { -fx-prompt-text-fill: transparent }
        // rule we add in bisq-controls.css.
        topLabel.getStyleClass().add("jfx-text-field-top-label");
        topLabel.setManaged(false);
        topLabel.setMouseTransparent(true);
        topLabel.textProperty().bind(control.promptTextProperty());
        topLabel.getTransforms().add(promptScaleTransform);
        topLabel.translateYProperty().bind(promptOffsetY);
        boolean isFloatCapable = control instanceof LabelFloatable;
        showTopLabel = Bindings.createBooleanBinding(() -> {
            if (!isFloatCapable) return false;
            return ((LabelFloatable) control).isLabelFloat();
        }, isFloatCapable ? ((LabelFloatable) control).labelFloatProperty()
                          : control.focusedProperty());
        topLabel.visibleProperty().bind(showTopLabel);

        getChildren().addAll(line, focusedLine, topLabel);

        // Float / colour transitions when focus or content changes.
        control.focusedProperty().addListener((o, ov, nv) -> { animateFocus(); updateFloatAnim(); });
        control.textProperty().addListener((o, ov, nv) -> updateFloatAnim());
        if (isFloatCapable) {
            ((LabelFloatable) control).labelFloatProperty()
                    .addListener((o, ov, nv) -> updateFloatAnim());
        }
        applyInitialFloatState();
    }

    private void applyInitialFloatState() {
        if (!isFloating()) return;
        // Defer so prefHeight is measurable.
        javafx.application.Platform.runLater(() -> {
            double labelH = topLabel.prefHeight(-1);
            promptScaleTransform.setX(0.85);
            promptScaleTransform.setY(0.85);
            promptOffsetY.set(-labelH);
            topLabel.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("floating"), true);
        });
    }

    private void updateFloatAnim() {
        boolean shouldFloat = isFloating();
        double targetScale = shouldFloat ? 0.85 : 1.0;
        // Negative translateY lifts the label above the field's content area.
        double labelH = topLabel.prefHeight(-1);
        double targetY = shouldFloat ? -labelH : 0;
        floatAnim.stop();
        floatAnim.getKeyFrames().setAll(new KeyFrame(ANIM_DURATION,
                new KeyValue(promptScaleTransform.xProperty(), targetScale, Interpolator.EASE_BOTH),
                new KeyValue(promptScaleTransform.yProperty(), targetScale, Interpolator.EASE_BOTH),
                new KeyValue(promptOffsetY, targetY, Interpolator.EASE_BOTH)));
        floatAnim.play();
        topLabel.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("floating"),
                shouldFloat);
    }

    private boolean isFloating() {
        TextField c = getSkinnable();
        if (c == null) return false;
        if (c.isFocused()) return true;
        String t = c.getText();
        return t != null && !t.isEmpty();
    }

    private void animateFocus() {
        double target = getSkinnable().isFocused() ? 1 : 0;
        focusAnim.stop();
        focusAnim.getKeyFrames().setAll(new KeyFrame(ANIM_DURATION,
                new KeyValue(focusedLine.scaleXProperty(), target, Interpolator.EASE_BOTH)));
        focusAnim.play();
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        double cw = getSkinnable().getWidth();
        double ch = getSkinnable().getHeight();
        double lineY = ch - LINE_HEIGHT;
        line.resizeRelocate(-inputLineExtension, lineY,
                cw + inputLineExtension * 2, LINE_HEIGHT);
        double focusedY = ch - FOCUSED_LINE_HEIGHT;
        focusedLine.resizeRelocate(-inputLineExtension, focusedY,
                cw + inputLineExtension * 2, FOCUSED_LINE_HEIGHT);
        // Floating label baseline INSIDE the field. translateY (driven by promptOffsetY) lifts
        // it above when focused/filled.
        if (topLabel.isVisible()) {
            double labelH = topLabel.prefHeight(-1);
            topLabel.resizeRelocate(x, y + (h - labelH) / 2, w, labelH);
        }
    }
}
