package bisq.desktop.jfxfixture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless batch fixture: renders N (component, state, theme, side) tuples, writes PNGs, exits.
 *
 * Runs under Monocle (no display server). Required system props (set in launcher script):
 *   -Djava.awt.headless=true
 *   -Dglass.platform=Monocle
 *   -Dmonocle.platform=Headless
 *   -Dprism.order=sw
 *   -Dprism.lcdtext=false
 *   -Dprism.text=t2k
 *
 * Two CLI modes:
 *
 * 1. Single render:
 *    --component=<n> --side=<jfx|bisq> --state=<...> --theme=<dark|light>
 *    [--width=<px>] [--height=<px>] --out=<path.png>
 *
 * 2. Batch (faster: one JVM, many PNGs):
 *    --batch=<file.tsv>
 *    TSV columns: component\tside\tstate\ttheme\twidth\theight\toutPath
 *    Header line "#" comments are skipped.
 *
 * Pseudo-class states applied directly via Node.pseudoClassStateChanged — no real input events.
 * Each snapshot uses a fresh Scene+Stage to avoid skin/state leakage between tuples.
 */
public class BisqJfxFixtureApp extends Application {

    private static Map<String, String> parsedArgs;
    private static List<RenderJob> batch;

    public static void main(String[] args) throws IOException {
        parsedArgs = parseArgs(args);
        batch = resolveJobs(parsedArgs);
        launch(args);
    }

    @Override
    public void start(Stage hiddenStage) {
        // Primary stage stays hidden — each render gets its own.
        renderNext(0);
    }

    private void renderNext(int i) {
        if (i >= batch.size()) {
            Platform.exit();
            return;
        }
        RenderJob j = batch.get(i);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);

        ComponentRenderer renderer = ComponentRegistry.get(j.component, j.side);
        if (renderer == null) {
            System.err.println("FAIL " + j + " — unknown (component, side)");
            stage.close();
            renderNext(i + 1);
            return;
        }

        Node node = renderer.build();
        StackPane root = new StackPane(node);
        root.setStyle("-fx-background-color: " + (j.theme.equals("light") ? "#ffffff" : "#1c2026") + ";");
        Scene scene = new Scene(root, j.width, j.height, Color.TRANSPARENT);
        loadStylesheets(scene, j.theme);
        stage.setScene(scene);
        stage.setWidth(j.width);
        stage.setHeight(j.height);
        stage.show();

        applyState(node, j.state);

        // Prevent the scene's auto-focus on the first focusable node — without this every
        // default-state render becomes implicitly :focused, leaking stock focus rings.
        if (!j.state.equals("focus")) {
            root.setFocusTraversable(true);
            root.requestFocus();
        }

        // Two pulses so CSS + skin layout settle before snapshot.
        Platform.runLater(() -> Platform.runLater(() -> {
            applyState(node, j.state); // re-apply post-layout for focus
            if (!j.state.equals("focus")) {
                root.requestFocus();
            }
            root.applyCss();
            root.layout();
            try {
                snapshot(root, j.outPath);
                System.out.println("OK " + j);
            } catch (Exception e) {
                System.err.println("FAIL " + j + " — " + e.getMessage());
                e.printStackTrace();
            } finally {
                stage.close();
                Platform.runLater(() -> renderNext(i + 1));
            }
        }));
    }

    private void loadStylesheets(Scene scene, String theme) {
        addCss(scene, "/bisq/desktop/bisq.css");
        addCss(scene, "/bisq/desktop/images.css");
        addCss(scene, theme.equals("light") ? "/bisq/desktop/theme-light.css" : "/bisq/desktop/theme-dark.css");
        addCss(scene, "/bisq/desktop/bisq-controls.css");
    }

    private void addCss(Scene scene, String path) {
        var url = getClass().getResource(path);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }

    private void applyState(Node node, String state) {
        switch (state) {
            case "default" -> { /* nothing */ }
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

    private void setPseudo(Node node, String name, boolean on) {
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), on);
    }

    private void snapshot(StackPane root, String out) throws IOException {
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        WritableImage img = root.snapshot(p, null);
        File f = new File(out);
        f.getParentFile().mkdirs();
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
    }

    private static List<RenderJob> resolveJobs(Map<String, String> a) throws IOException {
        if (a.containsKey("batch")) {
            return RenderJob.readTsv(new File(a.get("batch")));
        }
        return List.of(RenderJob.single(a));
    }

    private static Map<String, String> parseArgs(String[] argv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String a : argv) {
            if (a.startsWith("--")) {
                int eq = a.indexOf('=');
                if (eq > 0) m.put(a.substring(2, eq), a.substring(eq + 1));
                else m.put(a.substring(2), "true");
            }
        }
        return m;
    }

    record RenderJob(String component, String side, String state, String theme,
                     int width, int height, String outPath) {

        @Override public String toString() {
            return component + "/" + side + "/" + state + "/" + theme + " → " + outPath;
        }

        static RenderJob single(Map<String, String> a) {
            return new RenderJob(
                    require(a, "component"),
                    a.getOrDefault("side", "jfx"),
                    a.getOrDefault("state", "default"),
                    a.getOrDefault("theme", "dark"),
                    Integer.parseInt(a.getOrDefault("width", "320")),
                    Integer.parseInt(a.getOrDefault("height", "120")),
                    require(a, "out"));
        }

        static List<RenderJob> readTsv(File f) throws IOException {
            List<RenderJob> jobs = new ArrayList<>();
            for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] c = line.split("\t");
                if (c.length < 7)
                    throw new IOException("Bad TSV line (need 7 cols): " + line);
                jobs.add(new RenderJob(c[0], c[1], c[2], c[3],
                        Integer.parseInt(c[4]), Integer.parseInt(c[5]), c[6]));
            }
            return jobs;
        }

        private static String require(Map<String, String> a, String k) {
            String v = a.get(k);
            if (v == null) throw new IllegalArgumentException("Missing --" + k);
            return v;
        }
    }
}
