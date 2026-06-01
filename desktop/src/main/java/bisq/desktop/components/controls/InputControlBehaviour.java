package bisq.desktop.components.controls;

import bisq.desktop.components.controls.validation.Validator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

/**
 * Shared behaviour that the JFX-style text/password/area subclasses delegate to. Validators
 * are bisq-native {@link Validator} instances.
 */
public final class InputControlBehaviour {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final TextInputControl owner;
    private final ObservableList<Validator> validators = FXCollections.observableArrayList();
    private final BooleanProperty labelFloat = new SimpleBooleanProperty(false);

    public InputControlBehaviour(TextInputControl owner) {
        this.owner = owner;
    }

    public ObservableList<Validator> getValidators() {
        return validators;
    }

    public void setValidators(Validator... vs) {
        validators.setAll(vs);
    }

    /** Returns {@code true} when every validator passes. Toggles {@code :error} on the owner. */
    public boolean validate() {
        boolean anyError = false;
        for (Validator v : validators) {
            v.validate(owner);
            if (v.getHasErrors()) {
                anyError = true;
            }
        }
        ((Node) owner).pseudoClassStateChanged(ERROR, anyError);
        return !anyError;
    }

    public boolean isLabelFloat() {
        return labelFloat.get();
    }

    public void setLabelFloat(boolean v) {
        labelFloat.set(v);
        ((Node) owner).pseudoClassStateChanged(PseudoClass.getPseudoClass("label-float"), v);
    }

    public BooleanProperty labelFloatProperty() {
        return labelFloat;
    }
}
