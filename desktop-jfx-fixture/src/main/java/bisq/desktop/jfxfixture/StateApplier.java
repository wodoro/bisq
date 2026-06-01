package bisq.desktop.jfxfixture;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

/** Maps the canonical state strings to JavaFX pseudo-class / property mutations. */
public final class StateApplier {

    private StateApplier() {}

    public static void apply(Node node, String state) {
        switch (state) {
            case "default" -> {}
            case "hover" -> setPseudo(node, "hover", true);
            case "focus" -> {
                node.requestFocus();
                setPseudo(node, "focused", true);
            }
            case "press", "armed" -> {
                setPseudo(node, "armed", true);
                setPseudo(node, "pressed", true);
            }
            case "disabled" -> node.setDisable(true);
            case "selected" -> setPseudo(node, "selected", true);
            case "error" -> setPseudo(node, "error", true);
            case "readonly" -> {
                if (node instanceof TextInputControl t) t.setEditable(false);
                setPseudo(node, "readonly", true);
            }
            default -> throw new IllegalArgumentException("Unknown state: " + state);
        }
    }

    private static void setPseudo(Node node, String name, boolean on) {
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), on);
    }
}
