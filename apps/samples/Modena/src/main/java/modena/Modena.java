/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package modena;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

public class Modena extends Application {
    public static final String TEST = "test";
    static {
        System.getProperties().put("javafx.pseudoClassOverrideEnabled", "true");
    }
    private static final String testAppCssUrl = Modena.class.getResource("TestApp.css").toExternalForm();
    private static String MODENA_STYLESHEET_URL;
    private static String MODENA_EMBEDDED_STYLESHEET_URL;
    private static String MODENA_STYLESHEET_BASE;
    private static String CASPIAN_STYLESHEET_URL;
    private static String CASPIAN_STYLESHEET_BASE;
    static {
        try {
            // these are not supported ways to find the platform themes and may 
            // change release to release. Just used here for testing.
            File caspianCssFile = new File("../../../modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/caspian/caspian.css");
            if (!caspianCssFile.exists()) {
                caspianCssFile = new File("rt/modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/caspian/caspian.css");
            }
            CASPIAN_STYLESHEET_URL = caspianCssFile.exists() ? 
                    caspianCssFile.toURI().toURL().toExternalForm() :
                    com.sun.javafx.scene.control.skin.ButtonSkin.class.getResource("caspian/caspian.css").toExternalForm();
            File modenaCssFile = new File("../../../modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css");
            if (!modenaCssFile.exists()) {
                modenaCssFile = new File("rt/modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css");
                System.out.println("modenaCssFile = " + modenaCssFile);
                System.out.println("modenaCssFile = " + modenaCssFile.getAbsolutePath());
            }
            MODENA_STYLESHEET_URL = modenaCssFile.exists() ? 
                    modenaCssFile.toURI().toURL().toExternalForm() : 
                    com.sun.javafx.scene.control.skin.ButtonSkin.class.getResource("modena/modena.css").toExternalForm();
            MODENA_STYLESHEET_BASE = MODENA_STYLESHEET_URL.substring(0,MODENA_STYLESHEET_URL.lastIndexOf('/')+1);
            CASPIAN_STYLESHEET_BASE = CASPIAN_STYLESHEET_URL.substring(0,CASPIAN_STYLESHEET_URL.lastIndexOf('/')+1);
            MODENA_EMBEDDED_STYLESHEET_URL = MODENA_STYLESHEET_BASE + "modena-embedded-performance.css";
            System.out.println("MODENA_EMBEDDED_STYLESHEET_URL = " + MODENA_EMBEDDED_STYLESHEET_URL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Modena.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private BorderPane outerRoot;
    private BorderPane root;
    private SamplePageNavigation samplePageNavigation;
    private SamplePage samplePage;
    private Node mosaic;
    private Node heightTest;
    private SimpleWindowPage simpleWindows;
    private Node combinationsTest;
    private Node customerTest;
    private Stage mainStage;
    private Color backgroundColor;
    private Color baseColor;
    private Color accentColor;
    private String fontName = null;
    private int fontSize = 13;
    private String styleSheetContent = "";
    private String styleSheetBase = "";
    private ToggleButton modenaButton,retinaButton,rtlButton,embeddedPerformanceButton;
    private TabPane contentTabs;
    private boolean test = false;
    private boolean embeddedPerformanceMode = false;
    private final EventHandler<ActionEvent> rebuild = event -> Platform.runLater(() -> {
        updateUserAgentStyleSheet();
        rebuildUI(modenaButton.isSelected(), retinaButton.isSelected(),
                  contentTabs.getSelectionModel().getSelectedIndex(),
                  samplePageNavigation.getCurrentSection());
    });
    
    private static Modena instance;

    public static Modena getInstance() {
        return instance;
    }
    
    public Map<String, Node> getContent() {
        return samplePage.getContent();
    }

    public void setRetinaMode(boolean retinaMode) {
        if (retinaMode) {
            contentTabs.getTransforms().setAll(new Scale(2,2));
        } else {
            contentTabs.getTransforms().setAll(new Scale(1,1));
        }
        contentTabs.requestLayout();
    }
    
    public void restart() {
        mainStage.close();
        root = null;
        accentColor = null;
        baseColor = null;
        backgroundColor = null;
        fontName = null;
        fontSize = 13;
        try {
            start(new Stage());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start another Modena window", ex);
        }
    }
    
    @Override public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().contains(TEST)) {
            test = true;
        }
        mainStage = stage;
        // set user agent stylesheet
        updateUserAgentStyleSheet(true);
        // build Menu Bar
        outerRoot = new BorderPane();
        outerRoot.setTop(buildMenuBar());
        outerRoot.setCenter(root);
        // build UI
        rebuildUI(true,false,0, null);
        // show UI
        Scene scene = new Scene(outerRoot, 1024, 768);
        scene.getStylesheets().add(testAppCssUrl);
        stage.setScene(scene);
        stage.setTitle("Modena");
//        stage.setIconified(test); // TODO: Blocked by http://javafx-jira.kenai.com/browse/JMY-203
        stage.show(); // see SamplePage.java:110 comment on how test fails without having stage shown
        instance = this;
    }
    
    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);
        Menu fontSizeMenu = new Menu("Font");
        ToggleGroup tg = new ToggleGroup();
        fontSizeMenu.getItems().addAll(
            buildFontRadioMenuItem("System Default", null, 0, tg),
            buildFontRadioMenuItem("Mac (13px)", "Lucida Grande", 13, tg),
            buildFontRadioMenuItem("Windows 100% (12px)", "Segoe UI", 12, tg),
            buildFontRadioMenuItem("Windows 125% (15px)", "Segoe UI", 15, tg),
            buildFontRadioMenuItem("Windows 150% (18px)", "Segoe UI", 18, tg),
            buildFontRadioMenuItem("Linux (13px)", "Lucida Sans", 13, tg),
            buildFontRadioMenuItem("Embedded Touch (22px)", "Arial", 22, tg),
            buildFontRadioMenuItem("Embedded Small (9px)", "Arial", 9, tg)
        );
        menuBar.getMenus().add(fontSizeMenu);
        return menuBar;
    }
    
    private void updateUserAgentStyleSheet() {
        updateUserAgentStyleSheet(modenaButton.isSelected());
    }
    
    private void updateUserAgentStyleSheet(boolean modena) {
        final SamplePage.Section scrolledSection = samplePageNavigation==null? null : samplePageNavigation.getCurrentSection();
        styleSheetContent = modena ?
                loadUrl(MODENA_STYLESHEET_URL) :
                loadUrl(CASPIAN_STYLESHEET_URL);
        if (!modena &&
            (baseColor == null || baseColor == Color.TRANSPARENT) &&
            (backgroundColor == null || backgroundColor == Color.TRANSPARENT) &&
            (accentColor == null || accentColor == Color.TRANSPARENT) &&
            (fontName == null)) {
            // no customizations
            System.out.println("USING NO CUSTIMIZATIONS TO CSS, stylesheet = "+(modena?"modena":"caspian"));

            // load theme
            setUserAgentStylesheet("internal:stylesheet"+Math.random()+".css");
            if (root != null) root.requestLayout();
            // restore scrolled section
            Platform.runLater(() -> samplePageNavigation.setCurrentSection(scrolledSection));
            return;
        }
        if (modena && embeddedPerformanceMode) styleSheetContent += loadUrl(MODENA_EMBEDDED_STYLESHEET_URL);
        styleSheetBase = modena ? MODENA_STYLESHEET_BASE : CASPIAN_STYLESHEET_BASE;
        styleSheetContent += "\n.root {\n";
        System.out.println("baseColor = "+baseColor);
        System.out.println("accentColor = " + accentColor);
        System.out.println("backgroundColor = " + backgroundColor);
        if (baseColor != null && baseColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-base:" + colorToRGBA(baseColor) + ";\n";
        }
        if (backgroundColor != null && backgroundColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-background:" + colorToRGBA(backgroundColor) + ";\n";
        }
        if (accentColor != null && accentColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-accent:" + colorToRGBA(accentColor) + ";\n";
        }
        if (fontName != null) {
            styleSheetContent += "    -fx-font:"+fontSize+"px \""+fontName+"\";\n";
        }
        styleSheetContent += "}\n";
        
        // set white background for caspian
        if (!modena) {
            styleSheetContent += ".needs-background {\n-fx-background-color: white;\n}";
        }
            
        // load theme
        setUserAgentStylesheet("internal:stylesheet"+Math.random()+".css");
        
        if (root != null) root.requestLayout();

        // restore scrolled section
        Platform.runLater(() -> samplePageNavigation.setCurrentSection(scrolledSection));
    }
    
    private void rebuildUI(boolean modena, boolean retina, int selectedTab, final SamplePage.Section scrolledSection) {
        try {
            if (root == null) {
                root = new BorderPane();
                outerRoot.setCenter(root);
            } else {
                // clear out old UI
                root.setTop(null);
                root.setCenter(null);
            }
            // Create sample page and nav
            samplePageNavigation = new SamplePageNavigation();
            samplePage = samplePageNavigation.getSamplePage();
            // Create Content Area
            contentTabs = new TabPane();
            Tab tab1 = new Tab("All Controls");
            tab1.setContent(samplePageNavigation);
            Tab tab2 = new Tab("UI Mosaic");
            tab2.setContent(new ScrollPane(mosaic = (Node)FXMLLoader.load(Modena.class.getResource("ui-mosaic.fxml"))));

            Tab tab3 = new Tab("Alignment Test");
            tab3.setContent(new ScrollPane(heightTest =
                    (Node)FXMLLoader.load(Modena.class.getResource("SameHeightTest.fxml"))));

            Tab tab4 = new Tab("Simple Windows");
            tab4.setContent(new ScrollPane(simpleWindows = new SimpleWindowPage()));

            Tab tab5 = new Tab("Combinations");
            tab5.setContent(new ScrollPane(combinationsTest =
                    (Node)FXMLLoader.load(Modena.class.getResource("CombinationTest.fxml"))));

            // Customer example from bug report http://javafx-jira.kenai.com/browse/DTL-5561
            Tab tab6 = new Tab("Customer Example");
            tab6.setContent(new ScrollPane(customerTest =
                    (Node)FXMLLoader.load(Modena.class.getResource("ScottSelvia.fxml"))));

            contentTabs.getTabs().addAll(tab1, tab2, tab3, tab4, tab5, tab6);
            contentTabs.getSelectionModel().select(selectedTab);
            samplePage.setMouseTransparent(test);
            // height test set selection for 
            Platform.runLater(() -> {
                for (Node n: heightTest.lookupAll(".choice-box")) {
                    ((ChoiceBox)n).getSelectionModel().selectFirst();
                }
                for (Node n: heightTest.lookupAll(".combo-box")) {
                    ((ComboBox)n).getSelectionModel().selectFirst();
                }
            });
            // Create Toolbar
            retinaButton = new ToggleButton("@2x");
            retinaButton.setSelected(retina);
            retinaButton.setOnAction(event -> {
                ToggleButton btn = (ToggleButton)event.getSource();
                setRetinaMode(btn.isSelected());
            });
            ToggleGroup themesToggleGroup = new ToggleGroup();
            modenaButton = new ToggleButton("Modena");
            modenaButton.setToggleGroup(themesToggleGroup);
            modenaButton.setSelected(modena);
            modenaButton.setOnAction(rebuild);
            modenaButton.getStyleClass().add("left-pill:");
            ToggleButton caspianButton = new ToggleButton("Caspian");
            caspianButton.setToggleGroup(themesToggleGroup);
            caspianButton.setSelected(!modena);
            caspianButton.setOnAction(rebuild);
            caspianButton.getStyleClass().add("right-pill");
            Button reloadButton = new Button("", new ImageView(new Image(Modena.class.getResource("reload_12x14.png").toString())));
            reloadButton.setOnAction(rebuild);

            rtlButton = new ToggleButton("RTL");
            rtlButton.setOnAction(event -> root.setNodeOrientation(rtlButton.isSelected() ?
                    NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT));

            embeddedPerformanceButton = new ToggleButton("EP");
            embeddedPerformanceButton.setSelected(embeddedPerformanceMode);
            embeddedPerformanceButton.setTooltip(new Tooltip("Apply Embedded Performance extra stylesheet"));
            embeddedPerformanceButton.setOnAction(event -> {
                embeddedPerformanceMode = embeddedPerformanceButton.isSelected();
                rebuild.handle(event);
            });

            Button saveButton = new Button("Save...");
            saveButton.setOnAction(saveBtnHandler);

            Button restartButton = new Button("Restart");
            retinaButton.setOnAction(event -> restart());

            ToolBar toolBar = new ToolBar(new HBox(modenaButton, caspianButton), reloadButton, rtlButton,
                    embeddedPerformanceButton, new Separator(), retinaButton,
                    new Label("Base:"),
                    createBaseColorPicker(),
                    new Label("Background:"),
                    createBackgroundColorPicker(),
                    new Label("Accent:"),
                    createAccentColorPicker(),
                    new Separator(), saveButton, restartButton
                    );
            toolBar.setId("TestAppToolbar");
            // Create content group used for scaleing @2x
            final Pane contentGroup = new Pane() {
                @Override protected void layoutChildren() {
                    double scale = contentTabs.getTransforms().isEmpty() ? 1 : ((Scale)contentTabs.getTransforms().get(0)).getX();
                    contentTabs.resizeRelocate(0,0,getWidth()/scale, getHeight()/scale);
                }
            };
            contentGroup.getChildren().add(contentTabs);
            // populate root
            root.setTop(toolBar);
            root.setCenter(contentGroup);
            
            samplePage.getStyleClass().add("needs-background");
            mosaic.getStyleClass().add("needs-background");
            heightTest.getStyleClass().add("needs-background");
            combinationsTest.getStyleClass().add("needs-background");
            customerTest.getStyleClass().add("needs-background");
            simpleWindows.setModena(modena);
            // apply retina scale
            if (retina) {
                contentTabs.getTransforms().setAll(new Scale(2,2));
            }
            root.applyCss();
            // update state
            Platform.runLater(() -> {
                // move focus out of the way
                modenaButton.requestFocus();
                samplePageNavigation.setCurrentSection(scrolledSection);
            });
        } catch (IOException ex) {
            Logger.getLogger(Modena.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public RadioMenuItem buildFontRadioMenuItem(String name, final String in_fontName, final int in_fontSize, ToggleGroup tg) {
        RadioMenuItem rmItem = new RadioMenuItem(name);
        rmItem.setOnAction(event -> setFont(in_fontName, in_fontSize));
        rmItem.setStyle("-fx-font: " + in_fontSize + "px \"" + in_fontName + "\";");
        rmItem.setToggleGroup(tg);
        return rmItem;
    }
    
    public void setFont(String in_fontName, int in_fontSize) {
        System.out.println("===================================================================");
        System.out.println("==   SETTING FONT TO "+in_fontName+" "+in_fontSize+"px");
        System.out.println("===================================================================");
        fontName = in_fontName;
        fontSize = in_fontSize;
        updateUserAgentStyleSheet();
    }
    
    private ColorPicker createBaseColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.TRANSPARENT);
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> setBaseColor(c));
        return colorPicker;
    }
    
    public void setBaseColor(Color c) {
        if (c == null) {
            baseColor = null;
        } else {
            baseColor = c;
        }
        updateUserAgentStyleSheet();
    }
    
    private ColorPicker createBackgroundColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.TRANSPARENT);
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> {
            if (c == null) {
                backgroundColor = null;
            } else {
                backgroundColor = c;
            }
            updateUserAgentStyleSheet();
        });
        return colorPicker;
    }
    
    private ColorPicker createAccentColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.web("#0096C9"));
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#0096C9"),
                Color.web("#4fb6d6"),
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> setAccentColor(c));
        return colorPicker;
    }

    public void setAccentColor(Color c) {
        if (c == null) {
            accentColor = null;
        } else {
            accentColor = c;
        }
        updateUserAgentStyleSheet();
    }
    
    private EventHandler<ActionEvent> saveBtnHandler = event -> {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = fc.showSaveDialog(mainStage);
        if (file != null) {
            try {
                samplePage.getStyleClass().add("root");
                int width = (int)(samplePage.getLayoutBounds().getWidth()+0.5d);
                int height = (int)(samplePage.getLayoutBounds().getHeight()+0.5d);
                BufferedImage imgBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = imgBuffer.createGraphics();
                for (int y=0; y<height; y+=2048) {
                    SnapshotParameters snapshotParameters = new SnapshotParameters();
                    int remainingHeight = Math.min(2048, height - y);
                    snapshotParameters.setViewport(new Rectangle2D(0,y,width,remainingHeight));
                    WritableImage img = samplePage.snapshot(snapshotParameters, null);
                    g2.drawImage(SwingFXUtils.fromFXImage(img,null),0,y,null);
                }
                g2.dispose();
                ImageIO.write(imgBuffer, "PNG", file);
                System.out.println("Written image: "+file.getAbsolutePath());
            } catch (IOException ex) {
                Logger.getLogger(Modena.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    public static void main(String[] args) {
        launch(args);
    }
    
    /** Utility method to load a URL into a string */
    private static String loadUrl(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException ex) {
            Logger.getLogger(Modena.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }
    
    // =========================================================================
    // URL Handler to create magic "internal:stylesheet.css" url for our css string buffer
    {
        URL.setURLStreamHandlerFactory(new StringURLStreamHandlerFactory());
    }

    private String colorToRGBA(Color color) {
        // Older version didn't care about opacity
//        return String.format((Locale) null, "#%02x%02x%02x", 
//                Math.round(color.getRed() * 255), 
//                Math.round(color.getGreen() * 255), 
//                Math.round(color.getBlue() * 255));
        return String.format((Locale) null, "rgba(%d, %d, %d, %f)", 
            (int) Math.round(color.getRed() * 255), 
            (int) Math.round(color.getGreen() * 255), 
            (int) Math.round(color.getBlue() * 255),
            color.getOpacity());
    }

    /**
     * Simple URLConnection that always returns the content of the cssBuffer
     */
    private class StringURLConnection extends URLConnection {
        public StringURLConnection(URL url){
            super(url);
        }
        
        @Override public void connect() throws IOException {}

        @Override public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(styleSheetContent.getBytes("UTF-8"));
        }
    }
    
    private class StringURLStreamHandlerFactory implements URLStreamHandlerFactory {
        URLStreamHandler streamHandler = new URLStreamHandler(){
            @Override protected URLConnection openConnection(URL url) throws IOException {
                if (url.toString().toLowerCase().endsWith(".css")) {
                    return new StringURLConnection(url);
                } else {
                    return new URL(styleSheetBase+url.getFile()).openConnection();
                }
            }
        };
        @Override public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("internal".equals(protocol)) {
                return streamHandler;
            }
            return null;
        }
    }
}
