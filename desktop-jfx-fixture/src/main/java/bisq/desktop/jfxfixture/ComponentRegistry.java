package bisq.desktop.jfxfixture;

import bisq.desktop.components.controls.BisqJfxBadge;
import bisq.desktop.components.controls.BisqJfxButton;
import bisq.desktop.components.controls.BisqJfxCheckBox;
import bisq.desktop.components.controls.BisqJfxComboBox;
import bisq.desktop.components.controls.BisqJfxPasswordField;
import bisq.desktop.components.controls.BisqJfxProgressBar;
import bisq.desktop.components.controls.BisqJfxRadioButton;
import bisq.desktop.components.controls.BisqJfxSpinner;
import bisq.desktop.components.controls.BisqJfxTabPane;
import bisq.desktop.components.controls.BisqJfxTextArea;
import bisq.desktop.components.controls.BisqJfxTextField;
import bisq.desktop.components.controls.BisqJfxToggleButton;

import com.jfoenix.controls.JFXBadge;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;

import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;

import java.util.HashMap;
import java.util.Map;

/**
 * Pair registry: each (component, side) → renderer.
 * "jfx"  = current jfoenix implementation (baseline).
 * "bisq" = pure-JavaFX replacement candidate (this migration).
 *
 * Both sides have identical text/sizing so any pixel diff is pure-style.
 */
public final class ComponentRegistry {

    private static final Map<String, ComponentRenderer> PAIRS = new HashMap<>();

    static {
        // ---- Button ----
        register("button", "jfx", () -> sized(new JFXButton("Click me"), 160, 32));
        register("button", "bisq", () -> sized(new BisqJfxButton("Click me"), 160, 32));

        // ---- TextField ----
        register("text-field", "jfx", () -> wide(new JFXTextField("sample"), 220));
        register("text-field", "bisq", () -> wide(new BisqJfxTextField("sample"), 220));

        // ---- TextArea ----
        register("text-area", "jfx", () -> sized(new JFXTextArea("multi\nline"), 220, 80));
        register("text-area", "bisq", () -> sized(new BisqJfxTextArea("multi\nline"), 220, 80));

        // ---- PasswordField ----
        register("password-field", "jfx", () -> {
            JFXPasswordField p = new JFXPasswordField();
            p.setText("secret");
            return wide(p, 220);
        });
        register("password-field", "bisq", () -> {
            BisqJfxPasswordField p = new BisqJfxPasswordField();
            p.setText("secret");
            return wide(p, 220);
        });

        // ---- CheckBox ----
        register("check-box", "jfx", () -> new JFXCheckBox("Enabled"));
        register("check-box", "bisq", () -> new BisqJfxCheckBox("Enabled"));

        // ---- RadioButton ----
        register("radio-button", "jfx", () -> new JFXRadioButton("Option"));
        register("radio-button", "bisq", () -> new BisqJfxRadioButton("Option"));

        // ---- ToggleButton ----
        register("toggle-button", "jfx", () -> new JFXToggleButton());
        register("toggle-button", "bisq", () -> new BisqJfxToggleButton());

        // ---- ComboBox ----
        register("combo-box", "jfx", () -> {
            JFXComboBox<String> c = new JFXComboBox<>(FXCollections.observableArrayList("One", "Two", "Three"));
            c.getSelectionModel().selectFirst();
            return wide(c, 220);
        });
        register("combo-box", "bisq", () -> {
            BisqJfxComboBox<String> c = new BisqJfxComboBox<>(FXCollections.observableArrayList("One", "Two", "Three"));
            c.getSelectionModel().selectFirst();
            return wide(c, 220);
        });

        // ---- ProgressBar ----
        register("progress-bar", "jfx", () -> wide(new JFXProgressBar(0.5), 220));
        register("progress-bar", "bisq", () -> wide(new BisqJfxProgressBar(0.5), 220));

        // ---- Spinner ----
        register("spinner", "jfx", () -> sized(new JFXSpinner(), 48, 48));
        register("spinner", "bisq", () -> sized(new BisqJfxSpinner(), 48, 48));

        // ---- TabPane (long-text + pre-select index 1 to verify line width matches tab) ----
        register("tab-pane", "jfx", () -> {
            JFXTabPane tp = new JFXTabPane();
            tp.getTabs().addAll(new Tab("Market"),
                    new Tab("OFFERS BY PAYMENT METHOD"),
                    new Tab("Sell"));
            tp.getSelectionModel().select(1);
            return sized(tp, 560, 100);
        });
        register("tab-pane", "bisq", () -> {
            BisqJfxTabPane tp = new BisqJfxTabPane();
            tp.getTabs().addAll(new Tab("Market"),
                    new Tab("OFFERS BY PAYMENT METHOD"),
                    new Tab("Sell"));
            tp.getSelectionModel().select(1);
            return sized(tp, 560, 100);
        });

        // ---- Badge ----
        register("badge", "jfx", () -> {
            JFXButton inner = new JFXButton("Inbox");
            sized(inner, 100, 32);
            JFXBadge badge = new JFXBadge(inner);
            badge.setText("9");
            badge.refreshBadge();
            return badge;
        });
        register("badge", "bisq", () -> {
            BisqJfxButton inner = new BisqJfxButton("Inbox");
            sized(inner, 100, 32);
            BisqJfxBadge badge = new BisqJfxBadge(inner);
            badge.setText("9");
            badge.refreshBadge();
            return badge;
        });
    }

    private static <T extends javafx.scene.layout.Region> T sized(T n, double w, double h) {
        n.setPrefSize(w, h);
        return n;
    }

    private static <T extends javafx.scene.control.Control> T wide(T c, double w) {
        c.setPrefWidth(w);
        return c;
    }

    private static void register(String component, String side, ComponentRenderer r) {
        PAIRS.put(key(component, side), r);
    }

    public static ComponentRenderer get(String component, String side) {
        return PAIRS.get(key(component, side));
    }

    public static java.util.Set<String> components() {
        java.util.Set<String> out = new java.util.TreeSet<>();
        for (String k : PAIRS.keySet()) out.add(k.substring(0, k.indexOf('|')));
        return out;
    }

    private static String key(String c, String s) { return c + "|" + s; }

    private ComponentRegistry() {}
}
