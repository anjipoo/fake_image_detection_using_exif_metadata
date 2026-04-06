import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class FakeImageUI extends Application {

    // ── layout constants ──────────────────────────────────────────────────────
    private static final int W = 960, H = 700;

    // ── controls ──────────────────────────────────────────────────────────────
    private ImageView  imageView    = new ImageView();
    private Label      predLabel   = new Label("—");
    private Label      confLabel   = new Label("");
    private Label      simLabel    = new Label("");
    private TextArea   featArea    = new TextArea();
    private TextArea   topArea     = new TextArea();
    private Label      statusBar   = new Label("Ready. Open an image to analyse.");
    private ProgressIndicator spinner = new ProgressIndicator();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Fake Image Detection System");

        // ── Left panel: image display ─────────────────────────────────────────
        imageView.setFitWidth(360);
        imageView.setFitHeight(360);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.35),12,0,0,4);");

        Button openBtn = styled("📂  Open Image", "#3b82f6");
        openBtn.setOnAction(e -> chooseImage(stage));

        Button trainBtn = styled("⚙  Train Model", "#10b981");
        trainBtn.setOnAction(e -> runTraining(stage));

        VBox leftPanel = new VBox(14, sectionLabel("Image"), imageView, openBtn, trainBtn);
        leftPanel.setPadding(new Insets(18));
        leftPanel.setMinWidth(390);
        leftPanel.setStyle("-fx-background-color:#1e293b;-fx-background-radius:8;");

        // ── Right panel ───────────────────────────────────────────────────────
        predLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        predLabel.setTextFill(Color.WHITE);

        confLabel.setFont(Font.font("Monospace", 14));
        confLabel.setTextFill(Color.web("#94a3b8"));

        simLabel.setFont(Font.font("Monospace", 13));
        simLabel.setTextFill(Color.web("#64748b"));

        featArea.setEditable(false);
        featArea.setPrefHeight(140);
        featArea.setStyle("-fx-control-inner-background:#0f172a;-fx-text-fill:#94a3b8;-fx-font-family:Monospace;");

        topArea.setEditable(false);
        topArea.setPrefHeight(115);
        topArea.setStyle("-fx-control-inner-background:#0f172a;-fx-text-fill:#fbbf24;-fx-font-family:Monospace;");

        spinner.setVisible(false);
        spinner.setMaxSize(30, 30);

        VBox rightPanel = new VBox(12,
            sectionLabel("Prediction"),
            new HBox(10, predLabel, spinner),
            confLabel,
            simLabel,
            sectionLabel("Extracted Features"),
            featArea,
            sectionLabel("Top 5 Influential Features"),
            topArea
        );
        rightPanel.setPadding(new Insets(18));
        rightPanel.setStyle("-fx-background-color:#1e293b;-fx-background-radius:8;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // ── Root ──────────────────────────────────────────────────────────────
        HBox body = new HBox(14, leftPanel, rightPanel);
        body.setPadding(new Insets(14));
        body.setStyle("-fx-background-color:#0f172a;");

        statusBar.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#64748b;-fx-padding:6 14;");
        statusBar.setMaxWidth(Double.MAX_VALUE);

        VBox root = new VBox(body, statusBar);
        VBox.setVgrow(body, Priority.ALWAYS);
        root.setStyle("-fx-background-color:#0f172a;");

        stage.setScene(new Scene(root, W, H));
        stage.show();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void chooseImage(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.JPG","*.JPEG","*.PNG"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        // Show image immediately
        try {
            imageView.setImage(new Image(new FileInputStream(file)));
        } catch (Exception ex) { status("Cannot load image: " + ex.getMessage()); return; }

        clearResults();
        spinner.setVisible(true);
        status("Analysing " + file.getName() + " …");

        Task<Main.PredictionResult> task = new Task<>() {
            @Override protected Main.PredictionResult call() throws Exception {
                return Main.predictImage(file);
            }
        };
        task.setOnSucceeded(ev -> {
            spinner.setVisible(false);
            showResult(task.getValue());
            status("Done: " + file.getName());
        });
        task.setOnFailed(ev -> {
            spinner.setVisible(false);
            Throwable ex = task.getException();
            predLabel.setText("ERROR");
            predLabel.setTextFill(Color.web("#ef4444"));
            status("Error: " + ex.getMessage());
        });
        new Thread(task, "predict-thread").start();
    }

    private void runTraining(Stage stage) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select root folder (must contain 'au' and 'tp' sub-folders)");
        File root = dc.showDialog(stage);
        if (root == null) return;

        spinner.setVisible(true);
        status("Training model …");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String au = root.getAbsolutePath() + File.separator + "au";
                String tp = root.getAbsolutePath() + File.separator + "tp";
                DatasetGenerator.generate(au, tp);
                Object[] data = DatasetGenerator.load();
                double[][] X = (double[][]) data[0];
                int[] y      = (int[])     data[1];
                if (X.length == 0) throw new Exception("No images found in au/tp folders.");
                LogisticRegression model = new LogisticRegression(X[0].length);
                double[][] Xn = model.fitNormalize(X);
                model.train(Xn, y, 0.1, 1000);
                model.save(Main.MODEL_FILE);
                Main.indexImages(au, 0);
                Main.indexImages(tp, 1);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            spinner.setVisible(false);
            status("Model trained and saved ✓");
        });
        task.setOnFailed(ev -> {
            spinner.setVisible(false);
            status("Training failed: " + task.getException().getMessage());
        });
        new Thread(task, "train-thread").start();
    }

    private void showResult(Main.PredictionResult r) {
        boolean tampered = r.verdict.contains("TAMPERED");
        predLabel.setText(r.verdict);
        predLabel.setTextFill(tampered ? Color.web("#ef4444") : Color.web("#22c55e"));
        confLabel.setText(String.format("Confidence: %.1f%%", r.confidence));
        simLabel.setText(String.format("Closest match: %s  (%.1f%% similar)", r.closestMatch, r.similarity));

        // Features
        String[] names = FeatureExtractor.featureNames();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++)
            sb.append(String.format("%-20s  raw=%-10.4f  norm=%.4f%n", names[i], r.rawFeatures[i], r.normFeatures[i]));
        featArea.setText(sb.toString());

        // Top features
        topArea.setText(String.join("\n", r.topFeatures));
    }

    private void clearResults() {
        predLabel.setText("—"); predLabel.setTextFill(Color.WHITE);
        confLabel.setText(""); simLabel.setText("");
        featArea.clear(); topArea.clear();
    }

    private void status(String msg) {
        Platform.runLater(() -> statusBar.setText(msg));
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        l.setTextFill(Color.web("#38bdf8"));
        return l;
    }

    private static Button styled(String text, String color) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        b.setStyle(String.format(
            "-fx-background-color:%s;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:8 14;", color));
        return b;
    }

    public static void main(String[] args) { launch(args); }
}