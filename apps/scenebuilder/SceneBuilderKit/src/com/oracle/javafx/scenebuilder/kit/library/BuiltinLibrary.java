/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.library;

import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtilsBase;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import javafx.scene.layout.Region;

/**
 *
 * @treatAsPrivate
 */
public class BuiltinLibrary extends Library {
    
    // In SB 1.1 the section names of the Library have been localized. We assume
    // for now we stick to this approach, but fact is the support of custom
    // sections could change the rules of the game.
    public static final String TAG_CONTAINERS     = "Containers"; //NOI18N
    public static final String TAG_CONTROLS       = "Controls"; //NOI18N
    public static final String TAG_MENU           = "Menu"; //NOI18N
    public static final String TAG_MISCELLANEOUS  = "Miscellaneous"; //NOI18N
    public static final String TAG_SHAPES         = "Shapes"; //NOI18N
    public static final String TAG_CHARTS         = "Charts"; //NOI18N
    public static final String TAG_3D             = "3D"; //NOI18N

    
    private static BuiltinLibrary library = null;
    
    private final BuiltinSectionComparator sectionComparator
            = new BuiltinSectionComparator();
    
    private static final String FX8_QUALIFIER = " (FX8)"; //NOI18N
    
    /*
     * Public
     */
    
    public static synchronized BuiltinLibrary getLibrary() {
        if (library == null) {
            library = new BuiltinLibrary();
        }
        return library;
    }
    
    
    /*
     * Library
     */
    
    @Override
    public Comparator<String> getSectionComparator() {
        return sectionComparator;
    }
    
    /*
     * Debug
     */
    
    public static void main(String[] args) {
        getLibrary();
    }
    
    /*
     * Private
     */
    
    private BuiltinLibrary() {
        addCustomizedItem(javafx.scene.AmbientLight.class, TAG_3D, FX8_QUALIFIER);
        addDefaultItem(javafx.scene.Group.class, TAG_MISCELLANEOUS);
        addDefaultItem(javafx.scene.ParallelCamera.class, TAG_3D, FX8_QUALIFIER);
        addDefaultItem(javafx.scene.PerspectiveCamera.class, TAG_3D, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.PointLight.class, TAG_3D, FX8_QUALIFIER);
//        addDefaultItem(javafx.scene.SubScene.class, TAG_3D, FX8_QUALIFIER); TODO fix DTL-5862
        addCustomizedItem(javafx.scene.canvas.Canvas.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.scene.chart.AreaChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.BarChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.BubbleChart.class, TAG_CHARTS);
        addDefaultItem(javafx.scene.chart.CategoryAxis.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.LineChart.class, TAG_CHARTS);
        addDefaultItem(javafx.scene.chart.NumberAxis.class, TAG_CHARTS);
        addDefaultItem(javafx.scene.chart.PieChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.ScatterChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.StackedAreaChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.StackedBarChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.control.Accordion.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.Button.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.CheckBox.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.CheckMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.ChoiceBox.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.ColorPicker.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ComboBox.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ContextMenu.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.CustomMenuItem.class, TAG_MENU);
        addDefaultItem(javafx.scene.control.DatePicker.class, TAG_CONTROLS, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.control.Hyperlink.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.Label.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.ListView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.Menu.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.MenuBar.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.MenuButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.MenuItem.class, TAG_MENU);
        addRegionItem200x200(javafx.scene.control.Pagination.class, TAG_CONTAINERS);
        addDefaultItem(javafx.scene.control.PasswordField.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ProgressBar.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ProgressIndicator.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.RadioButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.RadioMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.ScrollBar.class, TAG_CONTROLS, 
                "ScrollBar (horizontal)", "ScrollBarH", "ScrollBar-h"); //NOI18N
        addCustomizedItem(javafx.scene.control.ScrollBar.class, TAG_CONTROLS, 
                "ScrollBar (vertical)", "ScrollBarV", "ScrollBar-v"); //NOI18N
        addCustomizedItem(javafx.scene.control.ScrollPane.class, TAG_CONTAINERS); // fxml
        addCustomizedItem(javafx.scene.control.Separator.class, TAG_CONTROLS, 
                "Separator (horizontal)", "SeparatorH", "Separator-h"); //NOI18N
        addCustomizedItem(javafx.scene.control.Separator.class, TAG_CONTROLS, 
                "Separator (vertical)", "SeparatorV", "Separator-v"); //NOI18N
        addCustomizedItem(javafx.scene.control.SeparatorMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.Slider.class, TAG_CONTROLS, 
                "Slider (horizontal)", "SliderH", "Slider-h"); //NOI18N
        addCustomizedItem(javafx.scene.control.Slider.class, TAG_CONTROLS, 
                "Slider (vertical)", "SliderV", "Slider-v"); //NOI18N
        addCustomizedItem(javafx.scene.control.SplitMenuButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.SplitPane.class, TAG_CONTAINERS, 
                "SplitPane (vertical)", "SplitPaneV", "SplitPane-v"); //NOI18N
        addCustomizedItem(javafx.scene.control.SplitPane.class, TAG_CONTAINERS, 
                "SplitPane (horizontal)", "SplitPaneH", "SplitPane-h"); //NOI18N
        addCustomizedItem(javafx.scene.control.Tab.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.TabPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.TableColumn.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TableView.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.TextArea.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.TextField.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TitledPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.ToggleButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ToolBar.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.Tooltip.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.scene.control.TreeTableColumn.class, TAG_CONTROLS, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.control.TreeTableView.class, TAG_CONTROLS, FX8_QUALIFIER);
        addRegionItem200x200(javafx.scene.control.TreeView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.image.ImageView.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.layout.AnchorPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.BorderPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.FlowPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.layout.GridPane.class, TAG_CONTAINERS);
        addRegionItem200x100(javafx.scene.layout.HBox.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.Pane.class, TAG_CONTAINERS);
        addRegionItem200x150(javafx.scene.layout.StackPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.Region.class, TAG_MISCELLANEOUS);
        addRegionItem200x200(javafx.scene.layout.TilePane.class, TAG_CONTAINERS);
        addRegionItem100x200(javafx.scene.layout.VBox.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.media.MediaView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.shape.Arc.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.ArcTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Box.class, TAG_SHAPES, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.shape.Circle.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.ClosePath.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.CubicCurve.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.CubicCurveTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Cylinder.class, TAG_SHAPES, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.shape.Ellipse.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.HLineTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Line.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.LineTo.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.MeshView.class, TAG_SHAPES, FX8_QUALIFIER);
        addDefaultItem(javafx.scene.shape.MoveTo.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.Path.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Polygon.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Polyline.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.QuadCurve.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.QuadCurveTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Rectangle.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.SVGPath.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Sphere.class, TAG_SHAPES, FX8_QUALIFIER);
        addDefaultItem(javafx.scene.shape.VLineTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.text.Text.class, TAG_SHAPES);
        addRegionItem200x200(javafx.scene.text.TextFlow.class, TAG_CONTAINERS, FX8_QUALIFIER);
        addCustomizedItem(javafx.scene.web.HTMLEditor.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.web.WebView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.embed.swing.SwingNode.class, TAG_MISCELLANEOUS, FX8_QUALIFIER);
    }
    
    
    private void addDefaultItem(Class<?> componentClass, String section, String... qualifiers) {
        final String name = componentClass.getSimpleName();
        String nameWithQualifier = name;
        if (qualifiers.length > 0) {
            nameWithQualifier += qualifiers[0];
        }
        final String fxmlText = makeFxmlText(componentClass);
        addItem(nameWithQualifier, fxmlText, section, name);
    }
    
    
    private void addRegionItem200x200(Class<? extends Region> componentClass, String section, String... qualifiers) {
        final String name = componentClass.getSimpleName();
        String nameWithQualifier = name;
        if (qualifiers.length > 0) {
            nameWithQualifier += qualifiers[0];
        }
        final String fxmlText = makeRegionFxmlText(componentClass, 200.0, 200.0);
        addItem(nameWithQualifier, fxmlText, section, name);
    }
    
    
    private void addRegionItem200x100(Class<? extends Region> componentClass, String section) {
        final String name = componentClass.getSimpleName();
        final String fxmlText = makeRegionFxmlText(componentClass, 200.0, 100.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addRegionItem200x150(Class<? extends Region> componentClass, String section) {
        final String name = componentClass.getSimpleName();
        final String fxmlText = makeRegionFxmlText(componentClass, 200.0, 150.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addRegionItem100x200(Class<? extends Region> componentClass, String section) {
        final String name = componentClass.getSimpleName();
        final String fxmlText = makeRegionFxmlText(componentClass, 100.0, 200.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addCustomizedItem(Class<?> componentClass, String section, String... qualifiers) {
        final String name = componentClass.getSimpleName();
        String nameWithQualifier = name;
        if (qualifiers.length > 0) {
            nameWithQualifier += qualifiers[0];
        }
        addCustomizedItem(componentClass, section, nameWithQualifier, name, name);
    }
    
    
    private void addCustomizedItem(Class<?> componentClass, String section, 
            String name, String fxmlBaseName, String iconName) {
        final String fxmlText = readCustomizedFxmlText(fxmlBaseName);
        assert fxmlText != null;
        addItem(name, fxmlText, section, iconName);
    }
    
    
    private void addItem(String name, String fxmlText, String section, String iconName) {
        final URL iconURL = ImageUtilsBase.getNodeIconURL(iconName + ".png"); //NOI18N
        final LibraryItem item = new LibraryItem(name, section, fxmlText, iconURL, this);
        itemsProperty.add(item);
    }
    
    
    private static String makeFxmlText(Class<?> componentClass) {
        final StringBuilder sb = new StringBuilder();
        
        /*
         * <?xml version="1.0" encoding="UTF-8"?> //NOI18N
         * 
         * <?import a.b.C?>
         * 
         * <C/>
         */
        
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //NOI18N
        
        sb.append("<?import "); //NOI18N
        sb.append(componentClass.getCanonicalName());
        sb.append("?>"); //NOI18N
        sb.append("<"); //NOI18N
        sb.append(componentClass.getSimpleName());
        sb.append("/>\n"); //NOI18N
        
        return sb.toString();
    }
    
    private static String makeRegionFxmlText(Class<? extends Region> componentClass, 
            double pw, double ph) {
        final StringBuilder sb = new StringBuilder();
        
        /*
         * <?xml version="1.0" encoding="UTF-8"?> //NOI18N
         * 
         * <?import a.b.C?>
         * 
         * <C prefWidth="pw" prefHeight="ph"/> //NOI18N
         */
        
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //NOI18N
        
        sb.append("<?import "); //NOI18N
        sb.append(componentClass.getCanonicalName());
        sb.append("?>"); //NOI18N
        sb.append("<"); //NOI18N
        sb.append(componentClass.getSimpleName());
        if (pw != Region.USE_COMPUTED_SIZE) {
            sb.append(" prefWidth=\""); //NOI18N
            sb.append(pw);
            sb.append("\""); //NOI18N
        }
        if (ph != Region.USE_COMPUTED_SIZE) {
            sb.append(" prefHeight=\""); //NOI18N
            sb.append(ph);
            sb.append("\""); //NOI18N
        }
        sb.append(" />\n"); //NOI18N
        
        return sb.toString();
    }
    
    
    private String readCustomizedFxmlText(String fxmlBaseName) {
        
        final StringBuilder fxmlPath = new StringBuilder();
        fxmlPath.append("builtin/"); //NOI18N
        fxmlPath.append(fxmlBaseName);
        fxmlPath.append(".fxml"); //NOI18N
        
        final URL fxmlURL = BuiltinLibrary.class.getResource(fxmlPath.toString());
        assert fxmlURL != null : "fxmlBaseName=" + fxmlBaseName; //NOI18N
        final String result;
        
        try {
            result = FXOMDocument.readContentFromURL(fxmlURL);
        } catch(IOException x) {
            throw new IllegalStateException("Bug in " + getClass().getSimpleName(), x); //NOI18N
        }
        
        return result;
    }
}
