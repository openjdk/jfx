package modena;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBoxBuilder;
import javafx.stage.Stage;

public class Modena extends Application {
    static {
        System.getProperties().put("javafx.pseudoClassOverrideEnabled", "true");
    }
    
    private BorderPane root;
    
    @Override public void start(Stage stage) throws Exception {
        // build UI
        rebuildUI(true, false);
        // show UI
        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(
                getClass().getResource("TestApp.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    
    private void rebuildUI(boolean modena, boolean retina) {
        // load theme
        if (modena) {
            setUserAgentStylesheet(
                    getClass().getResource("Modena.css").toExternalForm());
        } else {
            setUserAgentStylesheet(null);
        }
        if (root == null) {
            root = new BorderPane();
        } else {
            // clear out old UI
            root.setTop(null);
            root.setCenter(null);
        }
        // Create Toolbar
        final Group contentGroup = new Group();
        final ScrollPane contentScrollPane = new ScrollPane();
        final ToggleButton modenaButton;;
        final ToggleButton retinaButton = ToggleButtonBuilder.create()
            .text("Retina @2x")
            .selected(retina)
            .onAction(new EventHandler<ActionEvent>(){
                @Override public void handle(ActionEvent event) {
                    ToggleButton btn = (ToggleButton)event.getSource();
                    Node content = contentGroup.getChildren().get(0);
                    if (btn.isSelected()) {
                        content.setScaleX(2);
                        content.setScaleY(2);
                    } else {
                        content.setScaleX(1);
                        content.setScaleY(1);
                    }
                }
            })
            .build();
        ToggleGroup themesToggleGroup = new ToggleGroup();
        ToggleGroup colorToggleGroup = new ToggleGroup();
        ToolBar toolBar = new ToolBar(
            HBoxBuilder.create()
                .children(
                    modenaButton = ToggleButtonBuilder.create()
                        .text("Modena")
                        .toggleGroup(themesToggleGroup)
                        .selected(modena)
                        .onAction(new EventHandler<ActionEvent>(){
                            @Override public void handle(ActionEvent event) { 
                                rebuildUI(true,retinaButton.isSelected());
                            }
                        })
                        .styleClass("left-pill")
                        .build(),
                    ToggleButtonBuilder.create()
                        .text("Caspian")
                        .toggleGroup(themesToggleGroup)
                        .selected(!modena)
                        .onAction(new EventHandler<ActionEvent>(){
                            @Override public void handle(ActionEvent event) { 
                                rebuildUI(false,retinaButton.isSelected());
                            }
                        })
                        .styleClass("right-pill")
                        .build()
                )
                .build(),
            new Separator(),
            retinaButton,
            new Separator(),
            new Label("Base Color:"),
            HBoxBuilder.create()
                .spacing(3)
                .children(
                    createColorButton(null, colorToggleGroup, modena),
                    createColorButton("#f3622d", colorToggleGroup, modena),
                    createColorButton("#fba71b", colorToggleGroup, modena),
                    createColorButton("#57b757", colorToggleGroup, modena),
                    createColorButton("#41a9c9", colorToggleGroup, modena),
                    createColorButton("#888", colorToggleGroup, modena),
                    createColorButton("red", colorToggleGroup, modena),
                    createColorButton("orange", colorToggleGroup, modena),
                    createColorButton("yellow", colorToggleGroup, modena),
                    createColorButton("green", colorToggleGroup, modena),
                    createColorButton("cyan", colorToggleGroup, modena),
                    createColorButton("blue", colorToggleGroup, modena),
                    createColorButton("purple", colorToggleGroup, modena),
                    createColorButton("magenta", colorToggleGroup, modena),
                    createColorButton("black", colorToggleGroup, modena)
                )
                .build()
        );
        root.setTop(toolBar);
        contentScrollPane.setContent(contentGroup);
        root.setCenter(contentScrollPane);
        // create sample page
        contentGroup.getChildren().setAll(new SamplePage());
        // move foucus out of the way
        Platform.runLater(new Runnable() {
            @Override public void run() {
                modenaButton.requestFocus();
            }
        });
        // apply retina scale
        if (retina) {
            Node content = contentGroup.getChildren().get(0);
            content.setScaleX(2);
            content.setScaleY(2);
        }
    }
    
    private ToggleButton createColorButton(final String color, ToggleGroup toggleGroup, boolean modena) {
        final boolean isBase = color == null;
        String colorCSS;
        if (!isBase) {
            colorCSS = "-fx-base: "+color+";";
        } else {
            colorCSS = "-fx-base: "+(modena ? "#ececec" : "#d0d0d0")+";";
        }
        return ToggleButtonBuilder.create()
            .text(isBase?"default":null)
            .style(colorCSS)
            .toggleGroup(toggleGroup)
            .selected(isBase)
            .onAction(new EventHandler<ActionEvent>(){
                @Override public void handle(ActionEvent event) { 
                    if (isBase) {
                        root.setStyle(null);
                    } else {
                        root.setStyle("-fx-base: "+color+";");
                    }
                    root.impl_reapplyCSS();
                }
            })
            .styleClass("color-well")
            .build();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
