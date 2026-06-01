package bisq.desktop.jfxfixture;

import bisq.desktop.components.AutoTooltipButton;
import bisq.desktop.components.AutoTooltipCheckBox;
import bisq.desktop.components.AutoTooltipRadioButton;
import bisq.desktop.components.BisqTextArea;
import bisq.desktop.components.BisqTextField;
import bisq.desktop.components.BusyAnimation;
import bisq.desktop.components.InputTextField;
import bisq.desktop.components.PasswordTextField;
import bisq.desktop.components.controls.BisqAutoTooltipButton;
import bisq.desktop.components.controls.BisqAutoTooltipCheckBox;
import bisq.desktop.components.controls.BisqAutoTooltipRadioButton;
import bisq.desktop.components.controls.BisqJfxPasswordField;
import bisq.desktop.components.controls.BisqJfxProgressBar;
import bisq.desktop.components.controls.BisqJfxSpinner;
import bisq.desktop.components.controls.BisqJfxTextArea;
import bisq.desktop.components.controls.BisqJfxTextField;
import bisq.desktop.components.controls.BisqJfxToggleButton;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXToggleButton;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Supplier;

/**
 * Side-by-side interactive comparison: production Bisq wrappers (jfoenix-backed) vs pure-JavaFX
 * mirrors. Each row shows one state in two columns.
 *
 * The components compared are the ACTUAL production wrappers (AutoTooltipButton, BisqTextField,
 * InputTextField, ...) on the jfx side and their pure-JavaFX-base mirrors on the bisq side
 * (BisqAutoTooltipButton, BisqJfxTextField, ...). That keeps the visual contract identical to
 * what the running app uses.
 */
public class BisqJfxInteractiveApp extends Application {

    private record Pair(Supplier<Node> jfx, Supplier<Node> bisq, List<String> states) {}

    private static final List<String> S_BUTTON = List.of("default", "hover", "focus", "press", "disabled");
    private static final List<String> S_TEXT   = List.of("default", "focus", "error", "readonly", "disabled");
    private static final List<String> S_TOGGLE = List.of("default", "selected", "hover", "disabled");
    private static final List<String> S_NONE   = List.of("default");

    private static final java.util.LinkedHashMap<String, Pair> PAIRS = new java.util.LinkedHashMap<>();
    static {
        PAIRS.put("AutoTooltipButton", new Pair(
                () -> new AutoTooltipButton("Click me"),
                () -> new BisqAutoTooltipButton("Click me"),
                S_BUTTON));
        PAIRS.put("InputTextField", new Pair(
                () -> {
                    InputTextField f = new InputTextField();
                    f.setText("sample");
                    f.setPrefWidth(220);
                    return f;
                },
                () -> {
                    BisqJfxTextField f = new BisqJfxTextField("sample");
                    f.setPrefWidth(220);
                    return f;
                },
                S_TEXT));
        PAIRS.put("BisqTextField", new Pair(
                () -> { var f = new BisqTextField("sample"); f.setPrefWidth(220); return f; },
                () -> { var f = new BisqJfxTextField("sample"); f.setPrefWidth(220); return f; },
                S_TEXT));
        PAIRS.put("BisqTextArea", new Pair(
                () -> { var t = new BisqTextArea(); t.setText("multi\nline"); t.setPrefSize(220, 80); return t; },
                () -> { var t = new BisqJfxTextArea("multi\nline"); t.setPrefSize(220, 80); return t; },
                List.of("default", "focus", "disabled")));
        PAIRS.put("PasswordTextField", new Pair(
                () -> { var p = new PasswordTextField(); p.setText("secret"); p.setPrefWidth(220); return p; },
                () -> { var p = new BisqJfxPasswordField(); p.setText("secret"); p.setPrefWidth(220); return p; },
                S_TEXT));
        PAIRS.put("AutoTooltipCheckBox", new Pair(
                () -> new AutoTooltipCheckBox("Enabled"),
                () -> new BisqAutoTooltipCheckBox("Enabled"),
                S_TOGGLE));
        PAIRS.put("AutoTooltipRadioButton", new Pair(
                () -> new AutoTooltipRadioButton("Option"),
                () -> new BisqAutoTooltipRadioButton("Option"),
                S_TOGGLE));
        PAIRS.put("JFXToggleButton", new Pair(
                () -> new JFXToggleButton(),
                () -> new BisqJfxToggleButton(),
                S_TOGGLE));
        PAIRS.put("JFXTabPane", new Pair(
                () -> {
                    com.jfoenix.controls.JFXTabPane tp = new com.jfoenix.controls.JFXTabPane();
                    tp.getTabs().addAll(new javafx.scene.control.Tab("Market"),
                            new javafx.scene.control.Tab("OFFERS BY PAYMENT METHOD"),
                            new javafx.scene.control.Tab("Sell"));
                    tp.setPrefSize(560, 120);
                    return tp;
                },
                () -> {
                    bisq.desktop.components.controls.BisqJfxTabPane tp =
                            new bisq.desktop.components.controls.BisqJfxTabPane();
                    tp.getTabs().addAll(new javafx.scene.control.Tab("Market"),
                            new javafx.scene.control.Tab("OFFERS BY PAYMENT METHOD"),
                            new javafx.scene.control.Tab("Sell"));
                    tp.setPrefSize(560, 120);
                    return tp;
                },
                java.util.List.of("default")));
        PAIRS.put("JFXComboBox", new Pair(
                () -> {
                    JFXComboBox<String> c = new JFXComboBox<>(
                            javafx.collections.FXCollections.observableArrayList("One", "Two", "Three"));
                    c.getSelectionModel().selectFirst();
                    c.setPromptText("pick one");
                    c.setPrefWidth(220);
                    return c;
                },
                () -> {
                    bisq.desktop.components.controls.BisqJfxComboBox<String> c =
                            new bisq.desktop.components.controls.BisqJfxComboBox<>(
                                    javafx.collections.FXCollections.observableArrayList("One", "Two", "Three"));
                    c.getSelectionModel().selectFirst();
                    c.setPromptText("pick one");
                    c.setPrefWidth(220);
                    return c;
                },
                java.util.List.of("default", "focus", "disabled")));
        PAIRS.put("JFXProgressBar", new Pair(
                () -> { var p = new JFXProgressBar(0.5); p.setPrefWidth(220); return p; },
                () -> { var p = new BisqJfxProgressBar(0.5); p.setPrefWidth(220); return p; },
                S_NONE));
        PAIRS.put("ProgressBar (indeterminate)", new Pair(
                () -> {
                    var p = new JFXProgressBar();
                    p.setProgress(-1);
                    p.setPrefWidth(220);
                    return p;
                },
                () -> {
                    var p = new BisqJfxProgressBar();
                    p.setProgress(-1);
                    p.setPrefWidth(220);
                    return p;
                },
                S_NONE));
        PAIRS.put("BusyAnimation / JFXSpinner", new Pair(
                () -> { var s = new JFXSpinner(); s.setPrefSize(48, 48); return s; },
                () -> {
                    BisqJfxSpinner s = new BisqJfxSpinner();
                    s.setPrefSize(48, 48);
                    return s;
                },
                S_NONE));
    }

    private StackPane sceneRoot;
    private Scene scene;
    private GridPane grid;
    private String currentPair = "AutoTooltipButton";
    private String currentTheme = "dark";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane bp = new BorderPane();
        bp.setLeft(buildSidebar());
        bp.setCenter(buildContent());
        bp.setPadding(new Insets(10));

        sceneRoot = new StackPane(bp);
        scene = new Scene(sceneRoot, 1280, 820);

        applyTheme();

        stage.setTitle("Bisq jfoenix ↔ pure-JavaFX comparison");
        stage.setScene(scene);
        stage.show();
        rebuildGrid();
    }

    private Node buildSidebar() {
        Label header = boldLabel("Components");
        ObservableList<String> items = FXCollections.observableArrayList(PAIRS.keySet());
        ListView<String> list = new ListView<>(items);
        list.setPrefWidth(220);
        list.getSelectionModel().select(currentPair);
        list.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
            if (newV != null) {
                currentPair = newV;
                rebuildGrid();
            }
        });
        VBox box = new VBox(6, header, list);
        box.setPrefWidth(240);
        box.setPadding(new Insets(0, 12, 0, 0));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private Node buildContent() {
        VBox box = new VBox(10, buildThemeBar(), buildGridScroller());
        VBox.setVgrow(box.getChildren().get(1), Priority.ALWAYS);
        return box;
    }

    private Node buildThemeBar() {
        ToggleGroup tg = new ToggleGroup();
        ToggleButton light = themeToggle("light", tg);
        ToggleButton dark = themeToggle("dark", tg);
        (currentTheme.equals("dark") ? dark : light).setSelected(true);
        tg.selectedToggleProperty().addListener((o, oldV, newV) -> {
            if (newV != null) {
                currentTheme = ((ToggleButton) newV).getText();
                applyTheme();
                rebuildGrid();
            }
        });
        Button rebuild = new Button("Rebuild");
        rebuild.setOnAction(e -> rebuildGrid());
        HBox bar = new HBox(8, boldLabel("Theme:"), light, dark, new Separator(), rebuild);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4));
        return bar;
    }

    private ToggleButton themeToggle(String name, ToggleGroup g) {
        ToggleButton b = new ToggleButton(name);
        b.setToggleGroup(g);
        return b;
    }

    private ScrollPane gridScroller;

    private Node buildGridScroller() {
        grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(14);
        grid.setPadding(new Insets(12));
        gridScroller = new ScrollPane(grid);
        gridScroller.setFitToWidth(true);
        gridScroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return gridScroller;
    }

    private void rebuildGrid() {
        // Wipe children AND row/column constraints — stale constraints from a previous component
        // can push the new layout off-screen on switch.
        grid.getChildren().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();
        grid.add(boldLabel("state"),   0, 0);
        grid.add(boldLabel("jfoenix"), 1, 0);
        grid.add(boldLabel("bisq"),    2, 0);

        Pair pair = PAIRS.get(currentPair);
        if (pair == null) return;

        int row = 1;
        for (String state : pair.states()) {
            grid.add(new Label(state), 0, row);
            grid.add(cell(pair.jfx().get(),  state), 1, row);
            grid.add(cell(pair.bisq().get(), state), 2, row);
            row++;
        }
        // Reset scroll position so a previous component's offset doesn't carry over.
        if (gridScroller != null) {
            gridScroller.setVvalue(0);
            gridScroller.setHvalue(0);
        }
    }

    private Node cell(Node component, String state) {
        StateApplier.apply(component, state);
        StackPane wrap = new StackPane(component);
        wrap.setMinSize(260, 70);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.setStyle("-fx-border-color: rgba(127,127,127,0.30);"
                + " -fx-border-width: 1; -fx-border-radius: 4;");
        wrap.setPadding(new Insets(10));
        return wrap;
    }

    private void applyTheme() {
        scene.getStylesheets().clear();
        addCss("/bisq/desktop/bisq.css");
        addCss("/bisq/desktop/images.css");
        addCss(currentTheme.equals("light")
                ? "/bisq/desktop/theme-light.css"
                : "/bisq/desktop/theme-dark.css");
        addCss("/bisq/desktop/bisq-controls.css");
        Color bg = currentTheme.equals("light") ? Color.web("#f2f2f2") : Color.web("#1c2026");
        scene.setFill(bg);
        sceneRoot.setStyle("-fx-background-color: " + toCssHex(bg) + ";");
    }

    private void addCss(String path) {
        var url = getClass().getResource(path);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        else System.err.println("Stylesheet not found on classpath: " + path);
    }

    private static Label boldLabel(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static String toCssHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
