/*
 * Copyright (C) 2013-2015 2048FX 
 * Jose Pereda, Bruno Borges & Jens Deters
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpereda.game2048;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.LifecycleEvent;
import com.gluonhq.charm.down.plugins.LifecycleService;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The game 2048 built using JavaFX and Java 8. 
 * This is a fork based on the Javascript version: https://github.com/gabrielecirulli/2048
 * 
 * @author Bruno Borges bruno.borges@oracle.com & Jose Pereda jperedadnr@gmail.com 
 * Based on https://github.com/brunoborges/fx2048
 * 
 * Android Platform: Jose Pereda jperedadnr@gmail.com
 * 
 * iOS Platform: Jens Deters jens.deters@codecentric.de
 */
public class Game2048 extends Application {

    public static final String VERSION = "1.0.4";

    private final BooleanProperty stop = new SimpleBooleanProperty();
    private final BooleanProperty pause = new SimpleBooleanProperty();

    private GameManager gameManager;
    private Bounds gameBounds;
    private final static int MARGIN = 36;
    private StackPane root;

    @Override
    public void init() {
        
        // Downloaded from https://01.org/clear-sans/blogs
        // The font may be used and redistributed under the terms of the Apache License, Version 2.0.
        Font.loadFont(Game2048.class.getResource("ClearSans-Bold.ttf").toExternalForm(), 10.0);
        
        System.setOut(new PrintStream(new DevNull()));
    }
    
    private class DevNull extends OutputStream {

        @Override
        public void write(int b) throws IOException {
        }
        
    }
    
    @Override
    public void start(Stage primaryStage) {
        
        gameManager = new GameManager();
        gameManager.setToolBar(createToolBar());
        gameBounds = gameManager.getLayoutBounds();

        root = new StackPane(gameManager);
        root.getStyleClass().addAll("game-root");
        ChangeListener<Number> resize = (ov, v, v1) -> gameResize();
        root.widthProperty().addListener(resize);
        root.heightProperty().addListener(resize);

        Scene scene;
        if (Platform.isAndroid()) {
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
        } else {
            scene = new Scene(root);
        }        
        scene.getStylesheets().add(getClass().getResource("game.css").toExternalForm());
        
        addKeyHandler(scene);
        
        addSwipeHandlers(scene);

        if (Platform.isDesktop() && isARMDevice()) {
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
        }

        if (javafx.application.Platform.isSupported(ConditionalFeature.INPUT_TOUCH)) {
            scene.setCursor(Cursor.NONE);
        }

        if(Platform.isDesktop()){
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double factor = Math.min(visualBounds.getWidth() / (gameBounds.getWidth() + MARGIN),
                    visualBounds.getHeight() / (gameBounds.getHeight() + MARGIN));
            primaryStage.setMinWidth(gameBounds.getWidth() / 2d);
            primaryStage.setMinHeight(gameBounds.getHeight() / 2d);
            primaryStage.setWidth((gameBounds.getWidth() + MARGIN) * factor);
            primaryStage.setHeight((gameBounds.getHeight() + MARGIN) * factor);
        }

        Services.get(LifecycleService.class)
                .ifPresent(service -> {
                    service.addListener(LifecycleEvent.PAUSE, () -> pause.set(true));
                    service.addListener(LifecycleEvent.RESUME, () -> { pause.set(false); stop.set(false); });
                });
        
        stop.addListener((obs, b, b1) -> {
            if (b1) {
                gameManager.saveRecord();
            }
        });
        pause.addListener((obs,b,b1)->gameManager.externalPause(b,b1));
        primaryStage.setTitle("2048FX");
        primaryStage.setScene(scene);
        primaryStage.getIcons().addAll(new Image(Game2048.class.getResourceAsStream("Icon-60.png")));
        primaryStage.show();
    }
    
    private void gameResize(){
        gameBounds = gameManager.getLayoutBounds();
        double scale = Math.min((root.getWidth() - MARGIN) / gameBounds.getWidth(), (root.getHeight() - MARGIN) / gameBounds.getHeight());
        gameManager.setScale(scale);
                
        gameManager.setLayoutX((root.getWidth() - gameBounds.getWidth()) / 2d);
        gameManager.setLayoutY((root.getHeight() - gameBounds.getHeight()) / 2d);
    }

    private boolean isARMDevice() {
        return System.getProperty("os.arch").toUpperCase().contains("ARM");
    }

    private void addKeyHandler(Scene scene) {
        scene.setOnKeyPressed(ke -> {
            KeyCode keyCode = ke.getCode();
            if (keyCode.equals(KeyCode.S)) {
                gameManager.saveSession();
                return;
            }
            if (keyCode.equals(KeyCode.R)) {
                gameManager.restoreSession();
                return;
            }
            if (keyCode.equals(KeyCode.P)) {
                gameManager.pauseGame();
                return;
            }
            if (keyCode.equals(KeyCode.Q) || keyCode.equals(KeyCode.ESCAPE)) {
                gameManager.quitGame();
                return;
            }
            if (keyCode.isArrowKey()) {
                Direction direction = Direction.valueFor(keyCode);
                gameManager.move(direction);
            }
        });
    }

    private void addSwipeHandlers(Scene scene) {
        scene.setOnSwipeUp(e -> move(Direction.UP));
        scene.setOnSwipeRight(e -> move(Direction.RIGHT));
        scene.setOnSwipeLeft(e -> move(Direction.LEFT));
        scene.setOnSwipeDown(e -> move(Direction.DOWN));
    }
    
    private void move(Direction direction){
        gameManager.move(direction);    
    }

    private Button createButtonItem(String symbol, String text, EventHandler<ActionEvent> t){
        Button g=new Button();
        g.setPrefSize(40, 40);
        g.setId(symbol);
        g.setOnAction(t);
        g.setTooltip(new Tooltip(text));
        return g;
    }
    
    private HBox createToolBar(){
        HBox toolbar=new HBox();    
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(10.0));
        Button btItem1 = createButtonItem("mSave", "Save Session", t->gameManager.saveSession());
        Button btItem2 = createButtonItem("mRestore", "Restore Session", t->gameManager.restoreSession());
        Button btItem3 = createButtonItem("mPause", "Pause Game", t->gameManager.pauseGame());
        Button btItem4 = createButtonItem("mReplay", "Try Again", t->gameManager.tryAgain());
        Button btItem5 = createButtonItem("mInfo", "About the Game", t->gameManager.aboutGame());
        toolbar.getChildren().setAll(btItem1, btItem2, btItem3, btItem4, btItem5);
        if(Platform.isDesktop()){
            Button btItem6 = createButtonItem("mQuit", "Quit Game", t->gameManager.quitGame());
            toolbar.getChildren().add(btItem6);
        }
        return toolbar;
    }

    @Override
    public void stop() {
        gameManager.saveRecord();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
