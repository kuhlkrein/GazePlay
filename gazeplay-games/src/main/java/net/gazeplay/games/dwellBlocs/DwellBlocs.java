package net.gazeplay.games.dwellBlocs;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.configuration.BackgroundStyleVisitor;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.gaze.devicemanager.GazeEvent;
import net.gazeplay.commons.random.ReplayablePseudoRandom;
import net.gazeplay.commons.utils.games.ImageLibrary;
import net.gazeplay.commons.utils.games.ImageUtils;
import net.gazeplay.commons.utils.games.Utils;
import net.gazeplay.commons.utils.stats.Stats;
import net.gazeplay.components.AspectRatioImageRectangleUtil;

@Slf4j
public class DwellBlocs implements GameLifeCycle {

    private final EventHandler<Event> enterEvent;

    private final IGameContext gameContext;
    private final int nbLines;
    private final int nbColomns;
    private final boolean colors;
    private final float percents4Win;
    private final Stats stats;


    private ProgressIndicator progressIndicator;
    private Timeline currentTimeline = new Timeline();

    private final int initCount;

    private final int trail = 10;
    private final ImageLibrary imageLibrary;

    private ProgressIndicator createProgressIndicator(double width, double height) {
        ProgressIndicator indicator = new ProgressIndicator(0);
        indicator.setMinWidth(width * 0.5);
        indicator.setMinHeight(width * 0.5);
        indicator.setOpacity(0);
        return indicator;
    }

    @Data
    public static class CurrentRoundDetails {

        private int remainingCount;

        private boolean finished;

        private final StackBloc[][] stackBlocs;


        CurrentRoundDetails(final int initCount, final int nbLines, final int nbColumns) {
            this.remainingCount = initCount;
            this.finished = false;
            this.stackBlocs = new StackBloc[nbColumns][nbLines];
        }

    }

    private CurrentRoundDetails currentRoundDetails;

    private final ReplayablePseudoRandom randomGenerator;

    public DwellBlocs(final IGameContext gameContext, final int nbLines, final int nbColumns, final boolean colors, final float percents4Win,
                      final boolean useTrail, final Stats stats) {
        gameContext.startScoreLimiter();
        gameContext.startTimeLimiter();
        this.gameContext = gameContext;
        this.nbLines = nbLines;
        this.nbColomns = nbColumns;
        this.colors = colors;
        this.percents4Win = percents4Win;
        this.stats = stats;
        this.randomGenerator = new ReplayablePseudoRandom();
        this.stats.setGameSeed(randomGenerator.getSeed());

        imageLibrary = ImageUtils.createImageLibrary(Utils.getImagesSubdirectory("blocs"), randomGenerator);

        enterEvent = buildEvent(gameContext.getConfiguration());

        initCount = nbColumns * nbLines;
    }

    public DwellBlocs(final IGameContext gameContext, final int nbLines, final int nbColumns, final boolean colors, final float percents4Win,
                      final boolean useTrail, final Stats stats, double gameSeed) {
        this.gameContext = gameContext;
        this.nbLines = nbLines;
        this.nbColomns = nbColumns;
        this.colors = colors;
        this.percents4Win = percents4Win;
        this.stats = stats;
        this.randomGenerator = new ReplayablePseudoRandom(gameSeed);

        imageLibrary = ImageUtils.createImageLibrary(Utils.getImagesSubdirectory("blocs"), randomGenerator);

        enterEvent = buildEvent(gameContext.getConfiguration());

        initCount = nbColumns * nbLines;
    }

    private void setHiddenPicture(final IGameContext gameContext) {
        final Image randomPicture = imageLibrary.pickRandomImage();

        final Dimension2D dimension2D = gameContext.getGamePanelDimensionProvider().getDimension2D();

        final Rectangle imageRectangle = new Rectangle(0, 0, dimension2D.getWidth(), dimension2D.getHeight());
        imageRectangle.setFill(new ImagePattern(randomPicture, 0, 0, 1, 1, true));

        final AspectRatioImageRectangleUtil aspectRatioImageRectangleUtil = new AspectRatioImageRectangleUtil();
        aspectRatioImageRectangleUtil.setFillImageKeepingAspectRatio(imageRectangle, randomPicture, dimension2D);

        gameContext.getChildren().add(imageRectangle);
    }

    @Override
    public void launch() {
        gameContext.setLimiterAvailable();
        this.currentRoundDetails = new CurrentRoundDetails(initCount, nbLines, nbColomns);

        final Dimension2D dimension2D = gameContext.getGamePanelDimensionProvider().getDimension2D();

        setHiddenPicture(gameContext);

        final double width = dimension2D.getWidth() / nbColomns;
        final double height = dimension2D.getHeight() / nbLines;

        this.progressIndicator = createProgressIndicator((width), (height));

        for (int i = 0; i < nbColomns; i++) {
            for (int j = 0; j < nbLines; j++) {

                final StackBloc stackBloc = new StackBloc(i * width, j * height, width + 1, height + 1, i, j);// width+1, height+1 to avoid
                // spaces between blocks for
                // Scratchcard
                if (colors) {
                    stackBloc.setFill(new Color(randomGenerator.nextDouble(), randomGenerator.nextDouble(), randomGenerator.nextDouble(), 1));
                } else {
                    final Color c = gameContext.getConfiguration().getBackgroundStyle().accept(new BackgroundStyleVisitor<Color>() {
                        @Override
                        public Color visitLight() {
                            return Color.WHITE;
                        }

                        @Override
                        public Color visitDark() {
                            return Color.BLACK;
                        }
                    });
                    stackBloc.setFill(c);
                }
                gameContext.getChildren().add(stackBloc);
                currentRoundDetails.stackBlocs[i][j] = stackBloc;

                stackBloc.toFront();

                gameContext.getGazeDeviceManager().addEventFilter(stackBloc);

                stackBloc.addEventFilter(MouseEvent.ANY, enterEvent);

                stackBloc.addEventFilter(GazeEvent.ANY, enterEvent);

            }
        }
        stats.notifyNewRoundReady();
        gameContext.getGazeDeviceManager().addStats(stats);
        gameContext.firstStart();
    }

    @Override
    public void dispose() {

    }

    private void removeAllBlocs() {
        final StackBloc[][] stackBlocs = currentRoundDetails.stackBlocs;
        final int maxY = stackBlocs[0].length;
        for (final StackBloc[] stackBloc : stackBlocs) {
            for (int j = 0; j < maxY; j++) {

                removeBloc(stackBloc[j]);
            }
        }
    }

    private void removeBloc(final StackBloc toRemove) {

        if (toRemove == null) {
            return;
        }

        toRemove.removeEventFilter(MouseEvent.ANY, enterEvent);
        toRemove.removeEventFilter(GazeEvent.ANY, enterEvent);
        gameContext.getGazeDeviceManager().removeEventFilter(toRemove);
        toRemove.setTranslateX(-10000);
        toRemove.setOpacity(0);
        currentRoundDetails.remainingCount--;
    }

    private EventHandler<Event> buildEvent(Configuration config) {

        final int fixationlength = config.getFixationLength();

        return e -> {

            if (!(e.getTarget() instanceof StackBloc)) {
                return;
            }

            StackBloc element = ((StackBloc) e.getTarget());

            if (e.getEventType() == MouseEvent.MOUSE_ENTERED || e.getEventType() == GazeEvent.GAZE_ENTERED) {

                progressIndicator.setOpacity(1);
                progressIndicator.setProgress(0);

                element.getChildren().add(progressIndicator);

                currentTimeline.stop();
                currentTimeline = new Timeline();

                currentTimeline.getKeyFrames().add(new KeyFrame(new Duration(fixationlength),
                    new KeyValue(progressIndicator.progressProperty(), 1)));

                currentTimeline.setOnFinished(actionEvent -> {

                    element.removeEventFilter(MouseEvent.ANY, enterEvent);
                    element.removeEventFilter(GazeEvent.ANY, enterEvent);

                    onCorrectBlocSelected(element);
                });
                currentTimeline.play();
            } else if (e.getEventType() == MouseEvent.MOUSE_EXITED || e.getEventType() == GazeEvent.GAZE_EXITED) {


                element.getChildren().remove(progressIndicator);

                currentTimeline.stop();

                progressIndicator.setOpacity(0);
                progressIndicator.setProgress(0);
            }
        };

    }

    private void onCorrectBlocSelected(StackBloc stackBloc) {
        double finalZoom = 1.0;

        progressIndicator.setOpacity(0);

        removeBloc(stackBloc);

        log.info("remaining count is equal to " + currentRoundDetails.remainingCount);

        currentTimeline.stop();
        currentTimeline = new Timeline();

        if (currentRoundDetails.remainingCount == 0) {
            currentTimeline.onFinishedProperty().set(actionEvent -> gameContext.playWinTransition(500, actionEvent1 -> {
                dispose();

                gameContext.clear();

                launch();

                gameContext.onGameStarted();
            }));
        }

        currentTimeline.play();
    }

    private static class Bloc extends Rectangle {

        final int posX;
        final int posY;

        Bloc(final double x, final double y, final double width, final double height, final int posX, final int posY) {
            super(x, y, width, height);
            this.posX = posX;
            this.posY = posY;
        }

    }

    private static class StackBloc extends StackPane {

        final Bloc bloc;

        StackBloc(final double x, final double y, final double width, final double height, final int posX, final int posY) {
            super();
            this.bloc = new Bloc(0, 0, width, height, posX, posY);
            this.getChildren().add(bloc);
            this.setLayoutX(x);
            this.setLayoutY(y);
        }

        public void setFill(Paint value) {
            bloc.setFill(value);
        }
    }

}
