package net.gazeplay.games.rushHour;

import java.awt.MouseInfo;
import java.awt.Point;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.DefaultGamesLocator;
import net.gazeplay.GameContext;
import net.gazeplay.commons.configuration.Configuration;

@Slf4j
public class Car extends Rectangle {

    @Setter
    private boolean endOfGame = false;

    @Getter
    private boolean direction;
    private int size;
    private int move = 0;
    @Setter
    private boolean intersect = false;
    private Timeline timelineProgressBar;
    GameContext gameContext;
    private ProgressIndicator pi;

    private boolean selected = false;

    Car(int l, int h, Color c, boolean direction, int size, ProgressIndicator pi, GameContext gameContext) {
        super(0, 0, l * size, h * size);
        this.setFill(c);
        this.gameContext = gameContext;
        this.direction = direction;
        this.size = size;
        this.setOpacity(0.7);
        this.pi = pi;

        EventHandler<Event> enterEvent = new EventHandler<Event>() {
            @Override
            public void handle(Event e) {
                pi.setLayoutX(getX() + getWidth() / 2 - pi.getWidth() / 2);
                pi.setLayoutY(getY() + getHeight() / 2 - pi.getHeight() / 2);
                pi.setOpacity(1);
                pi.toFront();
                timelineProgressBar = new Timeline();
                timelineProgressBar.getKeyFrames()
                        .add(new KeyFrame(new Duration(Configuration.getInstance().getFixationLength()),
                                new KeyValue(pi.progressProperty(), 1)));
                timelineProgressBar.play();
                timelineProgressBar.setOnFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent actionEvent) {
                        setSelected(true);
                    }
                });
            }
        };

        this.addEventFilter(MouseEvent.MOUSE_ENTERED, enterEvent);

        EventHandler<Event> exitEvent = new EventHandler<Event>() {
            @Override
            public void handle(Event e) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();

                // if (e.getEventType() == MouseEvent.MOUSE_EXITED) {
                int way = checkPos(mouse);
                if (selected && !endOfGame && !intersect && !onMouse(mouse)) {
                    // moveToMouse(mouse);
                    moveTo(way, mouse);
                }
                intersect = false;
                // }

                if (!selected) {
                    timelineProgressBar.stop();
                    pi.setProgress(0);
                    pi.setOpacity(0);
                }
            }
        };

        this.addEventFilter(MouseEvent.MOUSE_EXITED, exitEvent);

    }

    private int checkPos(Point mouse) {
        Point2D coord = this.sceneToLocal(
                mouse.getX() - gameContext.getGazePlay().getPrimaryScene().getX()
                        - gameContext.getGazePlay().getPrimaryStage().getX(),
                mouse.getY() - gameContext.getGazePlay().getPrimaryScene().getY()
                        - gameContext.getGazePlay().getPrimaryStage().getY());

        if (!direction && (this.getX() < coord.getX()) && (coord.getX() < this.getX() + this.getWidth())) {
            return (this.getY() > coord.getY()) ? -1 : 1;
        } else if (direction && (this.getY() < coord.getY()) && (coord.getY() < this.getY() + this.getHeight())) {
            return (this.getX() > coord.getX()) ? -1 : 1;
        }

        setSelected(false);
        timelineProgressBar.stop();
        pi.setProgress(0);
        pi.setOpacity(0);
        return 0;
    }

    private void moveTo(int way, Point mouse) {
        double saveX = this.getX();
        double saveY = this.getY();
        if (this.isDirection()) {
            this.setX(saveX + way * size);
        } else {
            this.setY(saveY + way * size);
        }
        if (intersect) {
            this.setX(saveX);
            this.setY(saveY);
        }
    }

    private void moveToMouse(Point mouse) {
        Point2D coord = this.sceneToLocal(
                mouse.getX() - gameContext.getGazePlay().getPrimaryScene().getX()
                        - gameContext.getGazePlay().getPrimaryStage().getX(),
                mouse.getY() - gameContext.getGazePlay().getPrimaryScene().getY()
                        - gameContext.getGazePlay().getPrimaryStage().getY());
        double saveX = this.getX();
        double saveY = this.getY();
        if (this.isDirection()) {
            this.setX(((int) coord.getX() / size) * size);
        } else {
            this.setY(((int) coord.getY() / size) * size);
        }
        if (intersect) {
            this.setX(saveX);
            this.setY(saveY);
        }
    }

    private boolean onMouse(Point mouse) {
        Point2D coord = this.sceneToLocal(
                mouse.getX() - gameContext.getGazePlay().getPrimaryScene().getX()
                        - gameContext.getGazePlay().getPrimaryStage().getX(),
                mouse.getY() - gameContext.getGazePlay().getPrimaryScene().getY()
                        - gameContext.getGazePlay().getPrimaryStage().getY());
        return (this.getX() < coord.getX()) && (coord.getX() < this.getX() + this.getWidth())
                && (this.getY() < coord.getY()) && (coord.getY() < this.getY() + this.getHeight());
    }

    public void setToX(int i) {
        super.setX(size + i * size);
    }

    public void setToY(int i) {
        super.setY(size + i * size);
    }

    public void setSelected(boolean b) {
        selected = b;
        if (selected) {
            this.setOpacity(1);
        } else {
            this.setOpacity(0.7);
        }
    }

}