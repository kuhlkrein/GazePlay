package net.gazeplay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.ui.Translator;
import net.gazeplay.commons.utils.FixationPoint;
import net.gazeplay.commons.utils.stats.LifeCycle;
import net.gazeplay.commons.utils.stats.RoundsDurationReport;
import net.gazeplay.commons.utils.stats.SavedStatsInfo;
import net.gazeplay.commons.utils.stats.Stats;
import net.gazeplay.ui.scenes.ingame.GameContext;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ReplayingGameFromJson {
    List<GameSpec> gamesList;
    private ApplicationContext applicationContext;
    private GameContext gameContext;
    private GazePlay gazePlay;
    private double currentGameSeed;
    private String currentGameNameCode;
    private String currentGameVariant;
    private GameSpec selectedGameSpec;
    private IGameVariant gameVariant;
    private JsonArray coordinatesAndTimeStamp;
    private ArrayList<LinkedList<FixationPoint>> fixationSequence;
    private int nbGoalsReached;
    private int nbGoalsToReach;
    private int nbUnCountedGoalsReached;
    private LifeCycle lifeCycle;
    private RoundsDurationReport roundsDurationReport;
    private SavedStatsInfo savedStatsInfo;
    private int nextTimeMouse, nextTimeGaze, prevTimeMouse, prevTimeGaze;
    private double sceneAspectRatio;

    private String fileName;

    public ReplayingGameFromJson(GazePlay gazePlay, ApplicationContext applicationContext, List<GameSpec> games) {
        this.applicationContext = applicationContext;
        this.gazePlay = gazePlay;
        this.gameContext = applicationContext.getBean(GameContext.class);
        this.gamesList = games;
    }

    public void pickJSONFile(String fileName) throws IOException {
        this.fileName = fileName;
        if (fileName == null) {
            return;
        }

        final File replayDataFile = new File(fileName);

        InputStream in = Files.newInputStream(Paths.get(fileName));
        JSONTokener tokener = new JSONTokener(in);
        JSONObject object = new JSONObject(tokener);

        try (InputStream inputStream = ReplayingGameFromJson.class.getResourceAsStream("JSON-schema-replayData.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(object); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = Files.newBufferedReader(replayDataFile.toPath(), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert bufferedReader != null;
        Gson gson = new Gson();
        JsonFile json = gson.fromJson(bufferedReader, JsonFile.class);
        currentGameSeed = json.getGameSeed();
        currentGameNameCode = json.getGameName();
        currentGameVariant = json.getGameVariant();
        coordinatesAndTimeStamp = json.getCoordinatesAndTimeStamp();
        fixationSequence = json.getFixationSequence();
        nbGoalsReached = json.getStatsNbGoalsReached();
        nbGoalsToReach = json.getStatsNbGoalsToReach();
        nbUnCountedGoalsReached = json.getStatsNbUnCountedGoalsReached();
        sceneAspectRatio = json.getSceneAspectRatio();
        lifeCycle = json.getLifeCycle();
        roundsDurationReport = json.getRoundsDurationReport();
        String filePrefix = fileName.substring(0, fileName.lastIndexOf("-"));
        final File gazeMetricsMouseFile = new File(filePrefix + "-metricsMouse.png");
        final File gazeMetricsGazeFile = new File(filePrefix + "-metricsGaze.png");
        final File gazeMetricsMouseAndGazeFile = new File(filePrefix + "-metricsMouseAndGaze.png");
        final File heatMapCsvFile = new File(filePrefix + "-heatmap.csv");
        final File screenShotFile = new File(filePrefix + "-screenshot.png");
        final File colorBandsFile = new File(filePrefix + "-colorBands.png");
        savedStatsInfo = new SavedStatsInfo(heatMapCsvFile, gazeMetricsMouseFile, gazeMetricsGazeFile, gazeMetricsMouseAndGazeFile, screenShotFile,
            colorBandsFile, replayDataFile);
    }

    public String getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON files", "*.json")
        );
        File selectedFile = fileChooser.showOpenDialog(gameContext.getPrimaryStage());
        String selectedFileName = null;
        if (selectedFile != null) {
            selectedFileName = selectedFile.getAbsolutePath();
        }
        return selectedFileName;
    }

    public void replayGame() {
        if (fileName == null) {
            return;
        }
        double height = 2*gameContext.getCurrentScreenDimensionSupplier().get().getHeight() - gameContext.getPrimaryScene().getHeight();
        double width = gameContext.getCurrentScreenDimensionSupplier().get().getWidth();
        double screenRatio = height /width;
        if(sceneAspectRatio < screenRatio) {
            height = gameContext.getCurrentScreenDimensionSupplier().get().getWidth() * sceneAspectRatio;
        } else {
            width = height/sceneAspectRatio;
        }

        getSpecAndVariant();

        launchGame((int)width,(int)height);
    }

    public void getSpecAndVariant(){
        gameContext = applicationContext.getBean(GameContext.class);
        gazePlay.onGameLaunch(gameContext);
        for (GameSpec gameSpec : gamesList) {
            if (currentGameNameCode.equals(gameSpec.getGameSummary().getNameCode())) {
                selectedGameSpec = gameSpec;
            }
        }
        final Translator translator = gazePlay.getTranslator();
        for (IGameVariant variant : selectedGameSpec.getGameVariantGenerator().getVariants()) {
            if (currentGameVariant.equals(variant.toString())) {
                gameVariant = variant;
            }
        }
    }

    public void drawLines(){
        if (fileName == null) {
            return;
        }
        getSpecAndVariant();
        IGameLauncher gameLauncher = selectedGameSpec.getGameLauncher();
        final Scene scene = gazePlay.getPrimaryScene();
        final Stats statsSaved = gameLauncher.createSavedStats(scene, nbGoalsReached, nbGoalsToReach, nbUnCountedGoalsReached, fixationSequence, lifeCycle, roundsDurationReport, savedStatsInfo);
        GameLifeCycle currentGame = gameLauncher.replayGame(gameContext, gameVariant, statsSaved, currentGameSeed);
        gameContext.createControlPanel(gazePlay, statsSaved, currentGame, "replay");
        gameContext.createQuitShortcut(gazePlay, statsSaved, currentGame);
        currentGame.launch();

        final Dimension2D screenDimension = gameContext.getCurrentScreenDimensionSupplier().get();
        //Drawing in canvas
        final javafx.scene.canvas.Canvas canvas = new Canvas(screenDimension.getWidth(), screenDimension.getHeight());
        gameContext.getChildren().add(canvas);

        new Thread(new Runnable() {
            @Override
            public void run() {
                drawFixationLines(canvas, coordinatesAndTimeStamp);
                Platform.runLater(() -> exit(statsSaved, currentGame));
            }
        }).start();
    }

    private void launchGame(final int width, final int height){
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                gazePlay.getPrimaryScene().setCursor(Cursor.WAIT);
                gazePlay.getPrimaryScene().setRoot(gazePlay.getLoading());
                return null;
            }
        };
        new Thread(task).start();
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
            File.separator + "bin" +
            File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder builder = createBuilder(height,width);
        try {
            builder.inheritIO().start();
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    Platform.exit();
                    System.exit(0);
                }
            }, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("IL Y A EU UNE ERREUR Là");
            e.printStackTrace();
        }
    }

    public ProcessBuilder createBuilder(int height, int width){
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
            File.separator + "bin" +
            File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        LinkedList<String> commands = new LinkedList<>(Arrays.asList(javaBin, "-cp", classpath, GazePlayLauncher.class.getName()));

        String user = ActiveConfigurationContext.getInstance().getUserName();
        if(user != null && !user.equals("")) {
            commands.addAll(Arrays.asList("--user", user));
        } else {;
            commands.add("--default-user");
        }

        commands.addAll(Arrays.asList("--game", selectedGameSpec.getGameSummary().getNameCode()));

        if (gameVariant != null) {
            commands.addAll(Arrays.asList( "--variant", gameVariant.toString()));
        }


        if(height != 0 && width != 0) {
            commands.addAll(Arrays.asList("--height", "" + height, "--width", "" + width));
        }

        commands.addAll(Arrays.asList("-json", this.fileName));

        return new ProcessBuilder(commands);
    }

    private void exit(Stats statsSaved, GameLifeCycle currentGame) {
        gameContext.exitGame(statsSaved, gazePlay, currentGame, "replay");
    }

    private void drawFixationLines(Canvas canvas, JsonArray coordinatesAndTimeStamp) {
        int sceneWidth = (int) gameContext.getPrimaryScene().getWidth();
        int sceneHeight = (int) gameContext.getPrimaryScene().getHeight();
        final GraphicsContext graphics = canvas.getGraphicsContext2D();
        synchronized (coordinatesAndTimeStamp) {
            for (JsonElement coordinateAndTimeStamp : coordinatesAndTimeStamp) {
                JsonObject coordinateAndTimeObj = coordinateAndTimeStamp.getAsJsonObject();
                String nextEvent = coordinateAndTimeObj.get("event").getAsString();
                int nextX = (int) (Double.parseDouble(coordinateAndTimeObj.get("X").getAsString()) * sceneWidth);
                int nextY = (int) (Double.parseDouble(coordinateAndTimeObj.get("Y").getAsString()) * sceneHeight);
                int delay;
                if (nextEvent.equals("gaze")) {
                    prevTimeGaze = nextTimeGaze;
                    nextTimeGaze = Integer.parseInt(coordinateAndTimeObj.get("time").getAsString());
                    delay = nextTimeGaze - prevTimeGaze;
                    Platform.runLater(()-> {
                        paint(graphics, canvas, nextX, nextY, "gaze");
                    });
                } else {
                    prevTimeMouse = nextTimeMouse;
                    nextTimeMouse = Integer.parseInt(coordinateAndTimeObj.get("time").getAsString());
                    delay = nextTimeMouse - prevTimeMouse;
                    Platform.runLater(()-> {
                        paint(graphics, canvas, nextX, nextY, "mouse");
                    });
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public void paint(GraphicsContext graphics, Canvas canvas, int nextX, int nextY, String event) {
        javafx.scene.paint.Color strokeColor, fillColor;
        int circleSize = 10;
        if (event.equals("gaze")) {
            strokeColor = Color.rgb(0, 0, 255, 0.5);//Color.BLUE;
            fillColor = Color.rgb(255, 255, 0, 0.1);
        } else { // if (event.equals("mouse")) {
            strokeColor = Color.rgb(255, 0, 0, 0.5);//Color.RED;
            fillColor = Color.rgb(0, 255, 255, 0.1);
        }
        //   graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setStroke(strokeColor);
        graphics.strokeOval(nextX - circleSize / 2d, nextY - circleSize / 2d, circleSize, circleSize);
        graphics.setFill(fillColor);
        graphics.fillOval(nextX - circleSize / 2d, nextY - circleSize / 2d, circleSize, circleSize);
        Point2D point = new Point2D(nextX, nextY);
        gameContext.getGazeDeviceManager().onSavedMovementsUpdate(point, event);
    }

}

class JsonFile {
    @Getter
    private double gameSeed;
    @Getter
    private String gameName;
    @Getter
    private String gameVariantClass;
    @Getter
    private String gameVariant;
    @Getter
    private double gameStartedTime;
    @Getter
    private String screenAspectRatio;
    @Getter
    private double sceneAspectRatio;
    @Getter
    private JsonArray coordinatesAndTimeStamp;
    @Getter
    private LifeCycle lifeCycle;
    @Getter
    private int statsNbGoalsReached;
    @Getter
    private int statsNbGoalsToReach;
    @Getter
    private int statsNbUnCountedGoalsReached;
    @Getter
    private RoundsDurationReport roundsDurationReport;
    @Getter
    private ArrayList<LinkedList<FixationPoint>> fixationSequence;
}