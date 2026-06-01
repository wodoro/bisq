package bisq.desktop.jfxfixture;

import javafx.scene.Node;

/** Builds a single component instance for the fixture. Stateless — call build() per launch. */
@FunctionalInterface
public interface ComponentRenderer {
    Node build();
}
