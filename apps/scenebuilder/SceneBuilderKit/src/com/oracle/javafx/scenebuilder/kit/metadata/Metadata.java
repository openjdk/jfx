/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.metadata;

/*
 * THIS CODE IS AUTOMATICALLY GENERATED !
 */

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.keycombination.KeyCombinationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.PaintPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.ColorPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ComponentPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.*;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect.*;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.*;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPathComparator;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 */
public class Metadata {
    
    private static Metadata metadata = null;
    
    
    private final Map<Class<?>, ComponentClassMetadata> componentClassMap = new HashMap<>();
    private final Map<Class<?>, ComponentClassMetadata> customComponentClassMap = new WeakHashMap<>();
    private final Set<PropertyName> hiddenProperties = new HashSet<>();
    private final Set<PropertyName> parentRelatedProperties = new HashSet<>();
    private final List<String> sectionNames = new ArrayList<>();
    private final Map<String, List<String>> subSectionMap = new HashMap<>();
    
    public final InspectorPathComparator INSPECTOR_PATH_COMPARATOR
            = new InspectorPathComparator(sectionNames, subSectionMap);

    public static synchronized Metadata getMetadata() {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }
    
    public ComponentClassMetadata queryComponentMetadata(Class<?> componentClass) {
        final ComponentClassMetadata result;
        
        
        final ComponentClassMetadata componentMetadata
                = componentClassMap.get(componentClass);
        if (componentMetadata != null) {
            // componentClass is a certified component
            result = componentMetadata;
        } else {
            // componentClass is a custom component
            final ComponentClassMetadata customMetadata
                    = customComponentClassMap.get(componentClass);
            if (customMetadata != null) {
                // componentClass has already been introspected
                result = customMetadata;
            } else {
                // componentClass must be introspected
                // Let's find the first certified ancestor
                Class<?> ancestorClass = componentClass.getSuperclass();
                ComponentClassMetadata ancestorMetadata = null;
                while ((ancestorClass != null) && (ancestorMetadata == null)) {
                    ancestorMetadata = componentClassMap.get(ancestorClass);
                    ancestorClass = ancestorClass.getSuperclass();
                }
                final MetadataIntrospector introspector
                        = new MetadataIntrospector(componentClass, ancestorMetadata);
                result = introspector.introspect();
                customComponentClassMap.put(componentClass, result);
            }
        }
        
        return result;
    }
    
    public Set<PropertyMetadata> queryProperties(Class<?> componentClass) {
        final Map<PropertyName, PropertyMetadata> result = new HashMap<>();
        ComponentClassMetadata classMetadata = queryComponentMetadata(componentClass);
        
        while (classMetadata != null) {
            for (PropertyMetadata pm : classMetadata.getProperties()) {
                if (result.containsKey(pm.getName()) == false) {
                    result.put(pm.getName(), pm);
                }
            }
            classMetadata = classMetadata.getParentMetadata();
        }
        
        return new HashSet<>(result.values());
    }
    
    public Set<PropertyMetadata> queryProperties(Collection<Class<?>> componentClasses) {
        final Set<PropertyMetadata> result = new HashSet<>();
        
        int count = 0;
        for (Class<?> componentClass : componentClasses) {
            final Set<PropertyMetadata> propertyMetadata = queryProperties(componentClass);
            if (count == 0) {
                result.addAll(propertyMetadata);
            } else {
                result.retainAll(propertyMetadata);
            }
            count++;
        }
        
        return result;
    }
    
    public Set<ComponentPropertyMetadata> queryComponentProperties(Class<?> componentClass) {
        final Set<ComponentPropertyMetadata> result = new HashSet<>();
        
        for (PropertyMetadata propertyMetadata : queryProperties(Arrays.asList(componentClass))) {
            if (propertyMetadata instanceof ComponentPropertyMetadata) {
                result.add((ComponentPropertyMetadata) propertyMetadata);
            }
        }
        return result;
    }
    
    public Set<ValuePropertyMetadata> queryValueProperties(Set<Class<?>> componentClasses) {
        final Set<ValuePropertyMetadata> result = new HashSet<>();
        
        for (PropertyMetadata propertyMetadata : queryProperties(componentClasses)) {
            if (propertyMetadata instanceof ValuePropertyMetadata) {
                result.add((ValuePropertyMetadata) propertyMetadata);
            }
        }
        return result;
    }
    
    public PropertyMetadata queryProperty(Class<?> componentClass, PropertyName targetName) {
        final Set<PropertyMetadata> propertyMetadataSet = queryProperties(componentClass);
        final Iterator<PropertyMetadata> iterator = propertyMetadataSet.iterator();
        PropertyMetadata result = null;
                
        while ((result == null) && iterator.hasNext()) {
            final PropertyMetadata propertyMetadata = iterator.next();
            if (propertyMetadata.getName().equals(targetName)) {
                result = propertyMetadata;
            }
        }
        
        return result;
    }
    
    
    public ValuePropertyMetadata queryValueProperty(FXOMInstance fxomInstance, PropertyName targetName) {
        final ValuePropertyMetadata result;
        assert fxomInstance != null;
        assert targetName != null;
        
        if (fxomInstance.getSceneGraphObject() == null) {
            // FXOM object is unresolved
            result = null;
        } else {
            final Class<?> componentClass = fxomInstance.getSceneGraphObject().getClass();
            final PropertyMetadata m = Metadata.getMetadata().queryProperty(componentClass, targetName);
            if (m instanceof ValuePropertyMetadata) {
                result = (ValuePropertyMetadata) m;
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    
    public Collection<ComponentClassMetadata> getComponentClasses() {
        return componentClassMap.values();
    }

    public Set<PropertyName> getHiddenProperties() {
        return hiddenProperties;
    }

    public boolean isPropertyTrimmingNeeded(PropertyName name) {
        final boolean result;
        
        if (name.getResidenceClass() != null) {
            // It's a static property eg GridPane.rowIndex
            // All static property are "parent related" and needs trimming
            result = true;
        } else {
            result = parentRelatedProperties.contains(name);
        }
        
        return result;
    }


    // Abstract Component Classes

    private final ComponentClassMetadata NodeMetadata = 
            new ComponentClassMetadata(javafx.scene.Node.class, null);
    private final ComponentClassMetadata ParentMetadata = 
            new ComponentClassMetadata(javafx.scene.Parent.class, NodeMetadata);
    private final ComponentClassMetadata RegionMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.Region.class, ParentMetadata);
    private final ComponentClassMetadata PaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.Pane.class, RegionMetadata);
    private final ComponentClassMetadata ControlMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Control.class, RegionMetadata);
    private final ComponentClassMetadata LabeledMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Labeled.class, ControlMetadata);
    private final ComponentClassMetadata ButtonBaseMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ButtonBase.class, LabeledMetadata);
    private final ComponentClassMetadata ComboBoxBaseMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ComboBoxBase.class, ControlMetadata);
    private final ComponentClassMetadata PopupWindowMetadata = 
            new ComponentClassMetadata(javafx.stage.PopupWindow.class, null);
    private final ComponentClassMetadata PopupControlMetadata = 
            new ComponentClassMetadata(javafx.scene.control.PopupControl.class, PopupWindowMetadata);
    private final ComponentClassMetadata TextInputControlMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TextInputControl.class, ControlMetadata);
    private final ComponentClassMetadata TableColumnBaseMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TableColumnBase.class, null);
    private final ComponentClassMetadata MenuItemMetadata = 
            new ComponentClassMetadata(javafx.scene.control.MenuItem.class, null);
    private final ComponentClassMetadata TextFieldMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TextField.class, TextInputControlMetadata);
    private final ComponentClassMetadata ProgressIndicatorMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ProgressIndicator.class, ControlMetadata);
    private final ComponentClassMetadata ToggleButtonMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ToggleButton.class, ButtonBaseMetadata);
    private final ComponentClassMetadata AxisMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.Axis.class, RegionMetadata);
    private final ComponentClassMetadata ChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.Chart.class, RegionMetadata);
    private final ComponentClassMetadata ValueAxisMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.ValueAxis.class, AxisMetadata);
    private final ComponentClassMetadata XYChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.XYChart.class, ChartMetadata);
    private final ComponentClassMetadata ShapeMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Shape.class, NodeMetadata);
    private final ComponentClassMetadata PathElementMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.PathElement.class, null);
    private final ComponentClassMetadata CameraMetadata = 
            new ComponentClassMetadata(javafx.scene.Camera.class, NodeMetadata);
    private final ComponentClassMetadata LightBaseMetadata = 
            new ComponentClassMetadata(javafx.scene.LightBase.class, NodeMetadata);
    private final ComponentClassMetadata Shape3DMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Shape3D.class, NodeMetadata);



    // Other Component Classes (in alphabetical order)

    private final ComponentClassMetadata SwingNodeMetadata = 
            new ComponentClassMetadata(javafx.embed.swing.SwingNode.class, NodeMetadata);
    private final ComponentClassMetadata AmbientLightMetadata = 
            new ComponentClassMetadata(javafx.scene.AmbientLight.class, LightBaseMetadata);
    private final ComponentClassMetadata GroupMetadata = 
            new ComponentClassMetadata(javafx.scene.Group.class, ParentMetadata);
    private final ComponentClassMetadata ParallelCameraMetadata = 
            new ComponentClassMetadata(javafx.scene.ParallelCamera.class, CameraMetadata);
    private final ComponentClassMetadata PerspectiveCameraMetadata = 
            new ComponentClassMetadata(javafx.scene.PerspectiveCamera.class, CameraMetadata);
    private final ComponentClassMetadata PointLightMetadata = 
            new ComponentClassMetadata(javafx.scene.PointLight.class, LightBaseMetadata);
    private final ComponentClassMetadata SubSceneMetadata = 
            new ComponentClassMetadata(javafx.scene.SubScene.class, NodeMetadata);
    private final ComponentClassMetadata CanvasMetadata = 
            new ComponentClassMetadata(javafx.scene.canvas.Canvas.class, NodeMetadata);
    private final ComponentClassMetadata AreaChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.AreaChart.class, XYChartMetadata);
    private final ComponentClassMetadata BarChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.BarChart.class, XYChartMetadata);
    private final ComponentClassMetadata BubbleChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.BubbleChart.class, XYChartMetadata);
    private final ComponentClassMetadata CategoryAxisMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.CategoryAxis.class, AxisMetadata);
    private final ComponentClassMetadata LineChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.LineChart.class, XYChartMetadata);
    private final ComponentClassMetadata NumberAxisMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.NumberAxis.class, ValueAxisMetadata);
    private final ComponentClassMetadata PieChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.PieChart.class, ChartMetadata);
    private final ComponentClassMetadata ScatterChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.ScatterChart.class, XYChartMetadata);
    private final ComponentClassMetadata StackedAreaChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.StackedAreaChart.class, XYChartMetadata);
    private final ComponentClassMetadata StackedBarChartMetadata = 
            new ComponentClassMetadata(javafx.scene.chart.StackedBarChart.class, XYChartMetadata);
    private final ComponentClassMetadata AccordionMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Accordion.class, ControlMetadata);
    private final ComponentClassMetadata ButtonMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Button.class, ButtonBaseMetadata);
    private final ComponentClassMetadata CheckBoxMetadata = 
            new ComponentClassMetadata(javafx.scene.control.CheckBox.class, ButtonBaseMetadata);
    private final ComponentClassMetadata CheckMenuItemMetadata = 
            new ComponentClassMetadata(javafx.scene.control.CheckMenuItem.class, MenuItemMetadata);
    private final ComponentClassMetadata ChoiceBoxMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ChoiceBox.class, ControlMetadata);
    private final ComponentClassMetadata ColorPickerMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ColorPicker.class, ComboBoxBaseMetadata);
    private final ComponentClassMetadata ComboBoxMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ComboBox.class, ComboBoxBaseMetadata);
    private final ComponentClassMetadata ContextMenuMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ContextMenu.class, PopupControlMetadata);
    private final ComponentClassMetadata CustomMenuItemMetadata = 
            new ComponentClassMetadata(javafx.scene.control.CustomMenuItem.class, MenuItemMetadata);
    private final ComponentClassMetadata DatePickerMetadata = 
            new ComponentClassMetadata(javafx.scene.control.DatePicker.class, ComboBoxBaseMetadata);
    private final ComponentClassMetadata HyperlinkMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Hyperlink.class, ButtonBaseMetadata);
    private final ComponentClassMetadata LabelMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Label.class, LabeledMetadata);
    private final ComponentClassMetadata ListViewMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ListView.class, ControlMetadata);
    private final ComponentClassMetadata MenuMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Menu.class, MenuItemMetadata);
    private final ComponentClassMetadata MenuBarMetadata = 
            new ComponentClassMetadata(javafx.scene.control.MenuBar.class, ControlMetadata);
    private final ComponentClassMetadata MenuButtonMetadata = 
            new ComponentClassMetadata(javafx.scene.control.MenuButton.class, ButtonBaseMetadata);
    private final ComponentClassMetadata PaginationMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Pagination.class, ControlMetadata);
    private final ComponentClassMetadata PasswordFieldMetadata = 
            new ComponentClassMetadata(javafx.scene.control.PasswordField.class, TextFieldMetadata);
    private final ComponentClassMetadata ProgressBarMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ProgressBar.class, ProgressIndicatorMetadata);
    private final ComponentClassMetadata RadioButtonMetadata = 
            new ComponentClassMetadata(javafx.scene.control.RadioButton.class, ToggleButtonMetadata);
    private final ComponentClassMetadata RadioMenuItemMetadata = 
            new ComponentClassMetadata(javafx.scene.control.RadioMenuItem.class, MenuItemMetadata);
    private final ComponentClassMetadata ScrollBarMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ScrollBar.class, ControlMetadata);
    private final ComponentClassMetadata ScrollPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ScrollPane.class, ControlMetadata);
    private final ComponentClassMetadata SeparatorMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Separator.class, ControlMetadata);
    private final ComponentClassMetadata SeparatorMenuItemMetadata = 
            new ComponentClassMetadata(javafx.scene.control.SeparatorMenuItem.class, CustomMenuItemMetadata);
    private final ComponentClassMetadata SliderMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Slider.class, ControlMetadata);
    private final ComponentClassMetadata SplitMenuButtonMetadata = 
            new ComponentClassMetadata(javafx.scene.control.SplitMenuButton.class, MenuButtonMetadata);
    private final ComponentClassMetadata SplitPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.control.SplitPane.class, ControlMetadata);
    private final ComponentClassMetadata TabMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Tab.class, null);
    private final ComponentClassMetadata TabPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TabPane.class, ControlMetadata);
    private final ComponentClassMetadata TableColumnMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TableColumn.class, TableColumnBaseMetadata);
    private final ComponentClassMetadata TableViewMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TableView.class, ControlMetadata);
    private final ComponentClassMetadata TextAreaMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TextArea.class, TextInputControlMetadata);
    private final ComponentClassMetadata TitledPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TitledPane.class, LabeledMetadata);
    private final ComponentClassMetadata ToolBarMetadata = 
            new ComponentClassMetadata(javafx.scene.control.ToolBar.class, ControlMetadata);
    private final ComponentClassMetadata TooltipMetadata = 
            new ComponentClassMetadata(javafx.scene.control.Tooltip.class, PopupControlMetadata);
    private final ComponentClassMetadata TreeTableColumnMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TreeTableColumn.class, TableColumnBaseMetadata);
    private final ComponentClassMetadata TreeTableViewMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TreeTableView.class, ControlMetadata);
    private final ComponentClassMetadata TreeViewMetadata = 
            new ComponentClassMetadata(javafx.scene.control.TreeView.class, ControlMetadata);
    private final ComponentClassMetadata ImageViewMetadata = 
            new ComponentClassMetadata(javafx.scene.image.ImageView.class, NodeMetadata);
    private final ComponentClassMetadata AnchorPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.AnchorPane.class, PaneMetadata);
    private final ComponentClassMetadata BorderPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.BorderPane.class, PaneMetadata);
    private final ComponentClassMetadata ColumnConstraintsMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.ColumnConstraints.class, null);
    private final ComponentClassMetadata FlowPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.FlowPane.class, PaneMetadata);
    private final ComponentClassMetadata GridPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.GridPane.class, PaneMetadata);
    private final ComponentClassMetadata HBoxMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.HBox.class, PaneMetadata);
    private final ComponentClassMetadata RowConstraintsMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.RowConstraints.class, null);
    private final ComponentClassMetadata StackPaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.StackPane.class, PaneMetadata);
    private final ComponentClassMetadata TilePaneMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.TilePane.class, PaneMetadata);
    private final ComponentClassMetadata VBoxMetadata = 
            new ComponentClassMetadata(javafx.scene.layout.VBox.class, PaneMetadata);
    private final ComponentClassMetadata MediaViewMetadata = 
            new ComponentClassMetadata(javafx.scene.media.MediaView.class, NodeMetadata);
    private final ComponentClassMetadata ArcMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Arc.class, ShapeMetadata);
    private final ComponentClassMetadata ArcToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.ArcTo.class, PathElementMetadata);
    private final ComponentClassMetadata BoxMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Box.class, Shape3DMetadata);
    private final ComponentClassMetadata CircleMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Circle.class, ShapeMetadata);
    private final ComponentClassMetadata ClosePathMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.ClosePath.class, PathElementMetadata);
    private final ComponentClassMetadata CubicCurveMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.CubicCurve.class, ShapeMetadata);
    private final ComponentClassMetadata CubicCurveToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.CubicCurveTo.class, PathElementMetadata);
    private final ComponentClassMetadata CylinderMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Cylinder.class, Shape3DMetadata);
    private final ComponentClassMetadata EllipseMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Ellipse.class, ShapeMetadata);
    private final ComponentClassMetadata HLineToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.HLineTo.class, PathElementMetadata);
    private final ComponentClassMetadata LineMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Line.class, ShapeMetadata);
    private final ComponentClassMetadata LineToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.LineTo.class, PathElementMetadata);
    private final ComponentClassMetadata MeshViewMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.MeshView.class, Shape3DMetadata);
    private final ComponentClassMetadata MoveToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.MoveTo.class, PathElementMetadata);
    private final ComponentClassMetadata PathMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Path.class, ShapeMetadata);
    private final ComponentClassMetadata PolygonMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Polygon.class, ShapeMetadata);
    private final ComponentClassMetadata PolylineMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Polyline.class, ShapeMetadata);
    private final ComponentClassMetadata QuadCurveMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.QuadCurve.class, ShapeMetadata);
    private final ComponentClassMetadata QuadCurveToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.QuadCurveTo.class, PathElementMetadata);
    private final ComponentClassMetadata RectangleMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Rectangle.class, ShapeMetadata);
    private final ComponentClassMetadata SVGPathMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.SVGPath.class, ShapeMetadata);
    private final ComponentClassMetadata SphereMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.Sphere.class, Shape3DMetadata);
    private final ComponentClassMetadata VLineToMetadata = 
            new ComponentClassMetadata(javafx.scene.shape.VLineTo.class, PathElementMetadata);
    private final ComponentClassMetadata TextMetadata = 
            new ComponentClassMetadata(javafx.scene.text.Text.class, ShapeMetadata);
    private final ComponentClassMetadata TextFlowMetadata = 
            new ComponentClassMetadata(javafx.scene.text.TextFlow.class, PaneMetadata);
    private final ComponentClassMetadata HTMLEditorMetadata = 
            new ComponentClassMetadata(javafx.scene.web.HTMLEditor.class, ControlMetadata);
    private final ComponentClassMetadata WebViewMetadata = 
            new ComponentClassMetadata(javafx.scene.web.WebView.class, ParentMetadata);


    // Property Names

    private final PropertyName absoluteName = 
            new PropertyName("absolute");
    private final PropertyName acceleratorName = 
            new PropertyName("accelerator");
    private final PropertyName alignmentName = 
            new PropertyName("alignment");
    private final PropertyName allowIndeterminateName = 
            new PropertyName("allowIndeterminate");
    private final PropertyName alternativeColumnFillVisibleName = 
            new PropertyName("alternativeColumnFillVisible");
    private final PropertyName alternativeRowFillVisibleName = 
            new PropertyName("alternativeRowFillVisible");
    private final PropertyName anchorLocationName = 
            new PropertyName("anchorLocation");
    private final PropertyName anchorXName = 
            new PropertyName("anchorX");
    private final PropertyName anchorYName = 
            new PropertyName("anchorY");
    private final PropertyName animatedName = 
            new PropertyName("animated");
    private final PropertyName arcHeightName = 
            new PropertyName("arcHeight");
    private final PropertyName arcWidthName = 
            new PropertyName("arcWidth");
    private final PropertyName autoFixName = 
            new PropertyName("autoFix");
    private final PropertyName autoHideName = 
            new PropertyName("autoHide");
    private final PropertyName autoRangingName = 
            new PropertyName("autoRanging");
    private final PropertyName autoSizeChildrenName = 
            new PropertyName("autoSizeChildren");
    private final PropertyName barGapName = 
            new PropertyName("barGap");
    private final PropertyName baselineOffsetName = 
            new PropertyName("baselineOffset");
    private final PropertyName blendModeName = 
            new PropertyName("blendMode");
    private final PropertyName blockIncrementName = 
            new PropertyName("blockIncrement");
    private final PropertyName bottomName = 
            new PropertyName("bottom");
    private final PropertyName boundsInLocalName = 
            new PropertyName("boundsInLocal");
    private final PropertyName boundsInParentName = 
            new PropertyName("boundsInParent");
    private final PropertyName boundsTypeName = 
            new PropertyName("boundsType");
    private final PropertyName buttonCellName = 
            new PropertyName("buttonCell");
    private final PropertyName cacheName = 
            new PropertyName("cache");
    private final PropertyName cacheHintName = 
            new PropertyName("cacheHint");
    private final PropertyName cacheShapeName = 
            new PropertyName("cacheShape");
    private final PropertyName cancelButtonName = 
            new PropertyName("cancelButton");
    private final PropertyName categoriesName = 
            new PropertyName("categories");
    private final PropertyName categoryGapName = 
            new PropertyName("categoryGap");
    private final PropertyName categorySpacingName = 
            new PropertyName("categorySpacing");
    private final PropertyName centerName = 
            new PropertyName("center");
    private final PropertyName centerShapeName = 
            new PropertyName("centerShape");
    private final PropertyName centerXName = 
            new PropertyName("centerX");
    private final PropertyName centerYName = 
            new PropertyName("centerY");
    private final PropertyName childrenName = 
            new PropertyName("children");
    private final PropertyName clipName = 
            new PropertyName("clip");
    private final PropertyName clockwiseName = 
            new PropertyName("clockwise");
    private final PropertyName closableName = 
            new PropertyName("closable");
    private final PropertyName collapsibleName = 
            new PropertyName("collapsible");
    private final PropertyName colorName = 
            new PropertyName("color");
    private final PropertyName columnConstraintsName = 
            new PropertyName("columnConstraints");
    private final PropertyName columnHalignmentName = 
            new PropertyName("columnHalignment");
    private final PropertyName columnResizePolicyName = 
            new PropertyName("columnResizePolicy");
    private final PropertyName columnsName = 
            new PropertyName("columns");
    private final PropertyName consumeAutoHidingEventsName = 
            new PropertyName("consumeAutoHidingEvents");
    private final PropertyName contentName = 
            new PropertyName("content");
    private final PropertyName contentBiasName = 
            new PropertyName("contentBias");
    private final PropertyName contentDisplayName = 
            new PropertyName("contentDisplay");
    private final PropertyName contextMenuName = 
            new PropertyName("contextMenu");
    private final PropertyName contextMenuEnabledName = 
            new PropertyName("contextMenuEnabled");
    private final PropertyName controlXName = 
            new PropertyName("controlX");
    private final PropertyName controlX1Name = 
            new PropertyName("controlX1");
    private final PropertyName controlX2Name = 
            new PropertyName("controlX2");
    private final PropertyName controlYName = 
            new PropertyName("controlY");
    private final PropertyName controlY1Name = 
            new PropertyName("controlY1");
    private final PropertyName controlY2Name = 
            new PropertyName("controlY2");
    private final PropertyName createSymbolsName = 
            new PropertyName("createSymbols");
    private final PropertyName cullFaceName = 
            new PropertyName("cullFace");
    private final PropertyName currentPageIndexName = 
            new PropertyName("currentPageIndex");
    private final PropertyName cursorName = 
            new PropertyName("cursor");
    private final PropertyName defaultButtonName = 
            new PropertyName("defaultButton");
    private final PropertyName depthName = 
            new PropertyName("depth");
    private final PropertyName depthTestName = 
            new PropertyName("depthTest");
    private final PropertyName disableName = 
            new PropertyName("disable");
    private final PropertyName dividerPositionsName = 
            new PropertyName("dividerPositions");
    private final PropertyName divisionsName = 
            new PropertyName("divisions");
    private final PropertyName drawModeName = 
            new PropertyName("drawMode");
    private final PropertyName editableName = 
            new PropertyName("editable");
    private final PropertyName effectName = 
            new PropertyName("effect");
    private final PropertyName effectiveNodeOrientationName = 
            new PropertyName("effectiveNodeOrientation");
    private final PropertyName elementsName = 
            new PropertyName("elements");
    private final PropertyName ellipsisStringName = 
            new PropertyName("ellipsisString");
    private final PropertyName endMarginName = 
            new PropertyName("endMargin");
    private final PropertyName endXName = 
            new PropertyName("endX");
    private final PropertyName endYName = 
            new PropertyName("endY");
    private final PropertyName expandedName = 
            new PropertyName("expanded");
    private final PropertyName expandedItemCountName = 
            new PropertyName("expandedItemCount");
    private final PropertyName farClipName = 
            new PropertyName("farClip");
    private final PropertyName fieldOfViewName = 
            new PropertyName("fieldOfView");
    private final PropertyName fillName = 
            new PropertyName("fill");
    private final PropertyName fillHeightName = 
            new PropertyName("fillHeight");
    private final PropertyName fillRuleName = 
            new PropertyName("fillRule");
    private final PropertyName fillWidthName = 
            new PropertyName("fillWidth");
    private final PropertyName fitHeightName = 
            new PropertyName("fitHeight");
    private final PropertyName fitToHeightName = 
            new PropertyName("fitToHeight");
    private final PropertyName fitToWidthName = 
            new PropertyName("fitToWidth");
    private final PropertyName fitWidthName = 
            new PropertyName("fitWidth");
    private final PropertyName fixedCellSizeName = 
            new PropertyName("fixedCellSize");
    private final PropertyName fixedEyeAtCameraZeroName = 
            new PropertyName("fixedEyeAtCameraZero");
    private final PropertyName focusTraversableName = 
            new PropertyName("focusTraversable");
    private final PropertyName fontName = 
            new PropertyName("font");
    private final PropertyName fontScaleName = 
            new PropertyName("fontScale");
    private final PropertyName fontSmoothingTypeName = 
            new PropertyName("fontSmoothingType");
    private final PropertyName forceZeroInRangeName = 
            new PropertyName("forceZeroInRange");
    private final PropertyName gapStartAndEndName = 
            new PropertyName("gapStartAndEnd");
    private final PropertyName graphicName = 
            new PropertyName("graphic");
    private final PropertyName graphicTextGapName = 
            new PropertyName("graphicTextGap");
    private final PropertyName gridLinesVisibleName = 
            new PropertyName("gridLinesVisible");
    private final PropertyName halignmentName = 
            new PropertyName("halignment");
    private final PropertyName hbarPolicyName = 
            new PropertyName("hbarPolicy");
    private final PropertyName heightName = 
            new PropertyName("height");
    private final PropertyName hgapName = 
            new PropertyName("hgap");
    private final PropertyName hgrowName = 
            new PropertyName("hgrow");
    private final PropertyName hideOnClickName = 
            new PropertyName("hideOnClick");
    private final PropertyName hideOnEscapeName = 
            new PropertyName("hideOnEscape");
    private final PropertyName hmaxName = 
            new PropertyName("hmax");
    private final PropertyName hminName = 
            new PropertyName("hmin");
    private final PropertyName horizontalGridLinesVisibleName = 
            new PropertyName("horizontalGridLinesVisible");
    private final PropertyName horizontalZeroLineVisibleName = 
            new PropertyName("horizontalZeroLineVisible");
    private final PropertyName htmlTextName = 
            new PropertyName("htmlText");
    private final PropertyName hvalueName = 
            new PropertyName("hvalue");
    private final PropertyName idName = 
            new PropertyName("id");
    private final PropertyName imageName = 
            new PropertyName("image");
    private final PropertyName indeterminateName = 
            new PropertyName("indeterminate");
    private final PropertyName insetsName = 
            new PropertyName("insets");
    private final PropertyName itemsName = 
            new PropertyName("items");
    private final PropertyName labelName = 
            new PropertyName("label");
    private final PropertyName labelForName = 
            new PropertyName("labelFor");
    private final PropertyName labelFormatterName = 
            new PropertyName("labelFormatter");
    private final PropertyName labelLineLengthName = 
            new PropertyName("labelLineLength");
    private final PropertyName labelPaddingName = 
            new PropertyName("labelPadding");
    private final PropertyName labelsVisibleName = 
            new PropertyName("labelsVisible");
    private final PropertyName largeArcFlagName = 
            new PropertyName("largeArcFlag");
    private final PropertyName layoutBoundsName = 
            new PropertyName("layoutBounds");
    private final PropertyName layoutXName = 
            new PropertyName("layoutX");
    private final PropertyName layoutYName = 
            new PropertyName("layoutY");
    private final PropertyName leftName = 
            new PropertyName("left");
    private final PropertyName legendSideName = 
            new PropertyName("legendSide");
    private final PropertyName legendVisibleName = 
            new PropertyName("legendVisible");
    private final PropertyName lengthName = 
            new PropertyName("length");
    private final PropertyName lightOnName = 
            new PropertyName("lightOn");
    private final PropertyName lineSpacingName = 
            new PropertyName("lineSpacing");
    private final PropertyName lowerBoundName = 
            new PropertyName("lowerBound");
    private final PropertyName majorTickUnitName = 
            new PropertyName("majorTickUnit");
    private final PropertyName materialName = 
            new PropertyName("material");
    private final PropertyName maxName = 
            new PropertyName("max");
    private final PropertyName maxHeightName = 
            new PropertyName("maxHeight");
    private final PropertyName maxPageIndicatorCountName = 
            new PropertyName("maxPageIndicatorCount");
    private final PropertyName maxWidthName = 
            new PropertyName("maxWidth");
    private final PropertyName menusName = 
            new PropertyName("menus");
    private final PropertyName meshName = 
            new PropertyName("mesh");
    private final PropertyName minName = 
            new PropertyName("min");
    private final PropertyName minHeightName = 
            new PropertyName("minHeight");
    private final PropertyName minorTickCountName = 
            new PropertyName("minorTickCount");
    private final PropertyName minorTickLengthName = 
            new PropertyName("minorTickLength");
    private final PropertyName minorTickVisibleName = 
            new PropertyName("minorTickVisible");
    private final PropertyName minWidthName = 
            new PropertyName("minWidth");
    private final PropertyName mnemonicParsingName = 
            new PropertyName("mnemonicParsing");
    private final PropertyName mouseTransparentName = 
            new PropertyName("mouseTransparent");
    private final PropertyName nearClipName = 
            new PropertyName("nearClip");
    private final PropertyName nodeOrientationName = 
            new PropertyName("nodeOrientation");
    private final PropertyName onActionName = 
            new PropertyName("onAction");
    private final PropertyName onAutoHideName = 
            new PropertyName("onAutoHide");
    private final PropertyName onClosedName = 
            new PropertyName("onClosed");
    private final PropertyName onCloseRequestName = 
            new PropertyName("onCloseRequest");
    private final PropertyName onContextMenuRequestedName = 
            new PropertyName("onContextMenuRequested");
    private final PropertyName onDragDetectedName = 
            new PropertyName("onDragDetected");
    private final PropertyName onDragDoneName = 
            new PropertyName("onDragDone");
    private final PropertyName onDragDroppedName = 
            new PropertyName("onDragDropped");
    private final PropertyName onDragEnteredName = 
            new PropertyName("onDragEntered");
    private final PropertyName onDragExitedName = 
            new PropertyName("onDragExited");
    private final PropertyName onDragOverName = 
            new PropertyName("onDragOver");
    private final PropertyName onEditCancelName = 
            new PropertyName("onEditCancel");
    private final PropertyName onEditCommitName = 
            new PropertyName("onEditCommit");
    private final PropertyName onEditStartName = 
            new PropertyName("onEditStart");
    private final PropertyName onErrorName = 
            new PropertyName("onError");
    private final PropertyName onHiddenName = 
            new PropertyName("onHidden");
    private final PropertyName onHidingName = 
            new PropertyName("onHiding");
    private final PropertyName onInputMethodTextChangedName = 
            new PropertyName("onInputMethodTextChanged");
    private final PropertyName onKeyPressedName = 
            new PropertyName("onKeyPressed");
    private final PropertyName onKeyReleasedName = 
            new PropertyName("onKeyReleased");
    private final PropertyName onKeyTypedName = 
            new PropertyName("onKeyTyped");
    private final PropertyName onMenuValidationName = 
            new PropertyName("onMenuValidation");
    private final PropertyName onMouseClickedName = 
            new PropertyName("onMouseClicked");
    private final PropertyName onMouseDragEnteredName = 
            new PropertyName("onMouseDragEntered");
    private final PropertyName onMouseDragExitedName = 
            new PropertyName("onMouseDragExited");
    private final PropertyName onMouseDraggedName = 
            new PropertyName("onMouseDragged");
    private final PropertyName onMouseDragOverName = 
            new PropertyName("onMouseDragOver");
    private final PropertyName onMouseDragReleasedName = 
            new PropertyName("onMouseDragReleased");
    private final PropertyName onMouseEnteredName = 
            new PropertyName("onMouseEntered");
    private final PropertyName onMouseExitedName = 
            new PropertyName("onMouseExited");
    private final PropertyName onMouseMovedName = 
            new PropertyName("onMouseMoved");
    private final PropertyName onMousePressedName = 
            new PropertyName("onMousePressed");
    private final PropertyName onMouseReleasedName = 
            new PropertyName("onMouseReleased");
    private final PropertyName onRotateName = 
            new PropertyName("onRotate");
    private final PropertyName onRotationFinishedName = 
            new PropertyName("onRotationFinished");
    private final PropertyName onRotationStartedName = 
            new PropertyName("onRotationStarted");
    private final PropertyName onScrollName = 
            new PropertyName("onScroll");
    private final PropertyName onScrollFinishedName = 
            new PropertyName("onScrollFinished");
    private final PropertyName onScrollStartedName = 
            new PropertyName("onScrollStarted");
    private final PropertyName onScrollToName = 
            new PropertyName("onScrollTo");
    private final PropertyName onScrollToColumnName = 
            new PropertyName("onScrollToColumn");
    private final PropertyName onSelectionChangedName = 
            new PropertyName("onSelectionChanged");
    private final PropertyName onShowingName = 
            new PropertyName("onShowing");
    private final PropertyName onShownName = 
            new PropertyName("onShown");
    private final PropertyName onSortName = 
            new PropertyName("onSort");
    private final PropertyName onSwipeDownName = 
            new PropertyName("onSwipeDown");
    private final PropertyName onSwipeLeftName = 
            new PropertyName("onSwipeLeft");
    private final PropertyName onSwipeRightName = 
            new PropertyName("onSwipeRight");
    private final PropertyName onSwipeUpName = 
            new PropertyName("onSwipeUp");
    private final PropertyName onTouchMovedName = 
            new PropertyName("onTouchMoved");
    private final PropertyName onTouchPressedName = 
            new PropertyName("onTouchPressed");
    private final PropertyName onTouchReleasedName = 
            new PropertyName("onTouchReleased");
    private final PropertyName onTouchStationaryName = 
            new PropertyName("onTouchStationary");
    private final PropertyName onZoomName = 
            new PropertyName("onZoom");
    private final PropertyName onZoomFinishedName = 
            new PropertyName("onZoomFinished");
    private final PropertyName onZoomStartedName = 
            new PropertyName("onZoomStarted");
    private final PropertyName opacityName = 
            new PropertyName("opacity");
    private final PropertyName opaqueInsetsName = 
            new PropertyName("opaqueInsets");
    private final PropertyName orientationName = 
            new PropertyName("orientation");
    private final PropertyName paddingName = 
            new PropertyName("padding");
    private final PropertyName pageCountName = 
            new PropertyName("pageCount");
    private final PropertyName panesName = 
            new PropertyName("panes");
    private final PropertyName pannableName = 
            new PropertyName("pannable");
    private final PropertyName percentHeightName = 
            new PropertyName("percentHeight");
    private final PropertyName percentWidthName = 
            new PropertyName("percentWidth");
    private final PropertyName pickOnBoundsName = 
            new PropertyName("pickOnBounds");
    private final PropertyName placeholderName = 
            new PropertyName("placeholder");
    private final PropertyName pointsName = 
            new PropertyName("points");
    private final PropertyName popupSideName = 
            new PropertyName("popupSide");
    private final PropertyName prefColumnCountName = 
            new PropertyName("prefColumnCount");
    private final PropertyName prefColumnsName = 
            new PropertyName("prefColumns");
    private final PropertyName prefHeightName = 
            new PropertyName("prefHeight");
    private final PropertyName prefRowCountName = 
            new PropertyName("prefRowCount");
    private final PropertyName prefRowsName = 
            new PropertyName("prefRows");
    private final PropertyName prefTileHeightName = 
            new PropertyName("prefTileHeight");
    private final PropertyName prefTileWidthName = 
            new PropertyName("prefTileWidth");
    private final PropertyName prefViewportHeightName = 
            new PropertyName("prefViewportHeight");
    private final PropertyName prefViewportWidthName = 
            new PropertyName("prefViewportWidth");
    private final PropertyName prefWidthName = 
            new PropertyName("prefWidth");
    private final PropertyName prefWrapLengthName = 
            new PropertyName("prefWrapLength");
    private final PropertyName preserveRatioName = 
            new PropertyName("preserveRatio");
    private final PropertyName progressName = 
            new PropertyName("progress");
    private final PropertyName promptTextName = 
            new PropertyName("promptText");
    private final PropertyName radiusName = 
            new PropertyName("radius");
    private final PropertyName radiusXName = 
            new PropertyName("radiusX");
    private final PropertyName radiusYName = 
            new PropertyName("radiusY");
    private final PropertyName resizableName = 
            new PropertyName("resizable");
    private final PropertyName rightName = 
            new PropertyName("right");
    private final PropertyName rotateName = 
            new PropertyName("rotate");
    private final PropertyName rotateGraphicName = 
            new PropertyName("rotateGraphic");
    private final PropertyName rotationAxisName = 
            new PropertyName("rotationAxis");
    private final PropertyName rowConstraintsName = 
            new PropertyName("rowConstraints");
    private final PropertyName rowValignmentName = 
            new PropertyName("rowValignment");
    private final PropertyName scaleName = 
            new PropertyName("scale");
    private final PropertyName scaleShapeName = 
            new PropertyName("scaleShape");
    private final PropertyName scaleXName = 
            new PropertyName("scaleX");
    private final PropertyName scaleYName = 
            new PropertyName("scaleY");
    private final PropertyName scaleZName = 
            new PropertyName("scaleZ");
    private final PropertyName scopeName = 
            new PropertyName("scope");
    private final PropertyName scrollLeftName = 
            new PropertyName("scrollLeft");
    private final PropertyName scrollTopName = 
            new PropertyName("scrollTop");
    private final PropertyName selectedName = 
            new PropertyName("selected");
    private final PropertyName shapeName = 
            new PropertyName("shape");
    private final PropertyName showRootName = 
            new PropertyName("showRoot");
    private final PropertyName showTickLabelsName = 
            new PropertyName("showTickLabels");
    private final PropertyName showTickMarksName = 
            new PropertyName("showTickMarks");
    private final PropertyName showWeekNumbersName = 
            new PropertyName("showWeekNumbers");
    private final PropertyName sideName = 
            new PropertyName("side");
    private final PropertyName smoothName = 
            new PropertyName("smooth");
    private final PropertyName snapToPixelName = 
            new PropertyName("snapToPixel");
    private final PropertyName snapToTicksName = 
            new PropertyName("snapToTicks");
    private final PropertyName sortableName = 
            new PropertyName("sortable");
    private final PropertyName sortModeName = 
            new PropertyName("sortMode");
    private final PropertyName sortNodeName = 
            new PropertyName("sortNode");
    private final PropertyName sortOrderName = 
            new PropertyName("sortOrder");
    private final PropertyName sortTypeName = 
            new PropertyName("sortType");
    private final PropertyName spacingName = 
            new PropertyName("spacing");
    private final PropertyName startAngleName = 
            new PropertyName("startAngle");
    private final PropertyName startMarginName = 
            new PropertyName("startMargin");
    private final PropertyName startXName = 
            new PropertyName("startX");
    private final PropertyName startYName = 
            new PropertyName("startY");
    private final PropertyName strikethroughName = 
            new PropertyName("strikethrough");
    private final PropertyName strokeName = 
            new PropertyName("stroke");
    private final PropertyName strokeDashArrayName = 
            new PropertyName("strokeDashArray");
    private final PropertyName strokeDashOffsetName = 
            new PropertyName("strokeDashOffset");
    private final PropertyName strokeLineCapName = 
            new PropertyName("strokeLineCap");
    private final PropertyName strokeLineJoinName = 
            new PropertyName("strokeLineJoin");
    private final PropertyName strokeMiterLimitName = 
            new PropertyName("strokeMiterLimit");
    private final PropertyName strokeTypeName = 
            new PropertyName("strokeType");
    private final PropertyName strokeWidthName = 
            new PropertyName("strokeWidth");
    private final PropertyName styleName = 
            new PropertyName("style");
    private final PropertyName styleClassName = 
            new PropertyName("styleClass");
    private final PropertyName stylesheetsName = 
            new PropertyName("stylesheets");
    private final PropertyName sweepFlagName = 
            new PropertyName("sweepFlag");
    private final PropertyName tabClosingPolicyName = 
            new PropertyName("tabClosingPolicy");
    private final PropertyName tableMenuButtonVisibleName = 
            new PropertyName("tableMenuButtonVisible");
    private final PropertyName tabMaxHeightName = 
            new PropertyName("tabMaxHeight");
    private final PropertyName tabMaxWidthName = 
            new PropertyName("tabMaxWidth");
    private final PropertyName tabMinHeightName = 
            new PropertyName("tabMinHeight");
    private final PropertyName tabMinWidthName = 
            new PropertyName("tabMinWidth");
    private final PropertyName tabsName = 
            new PropertyName("tabs");
    private final PropertyName textName = 
            new PropertyName("text");
    private final PropertyName textAlignmentName = 
            new PropertyName("textAlignment");
    private final PropertyName textFillName = 
            new PropertyName("textFill");
    private final PropertyName textOriginName = 
            new PropertyName("textOrigin");
    private final PropertyName textOverrunName = 
            new PropertyName("textOverrun");
    private final PropertyName tickLabelFillName = 
            new PropertyName("tickLabelFill");
    private final PropertyName tickLabelFontName = 
            new PropertyName("tickLabelFont");
    private final PropertyName tickLabelFormatterName = 
            new PropertyName("tickLabelFormatter");
    private final PropertyName tickLabelGapName = 
            new PropertyName("tickLabelGap");
    private final PropertyName tickLabelRotationName = 
            new PropertyName("tickLabelRotation");
    private final PropertyName tickLabelsVisibleName = 
            new PropertyName("tickLabelsVisible");
    private final PropertyName tickLengthName = 
            new PropertyName("tickLength");
    private final PropertyName tickMarksName = 
            new PropertyName("tickMarks");
    private final PropertyName tickMarkVisibleName = 
            new PropertyName("tickMarkVisible");
    private final PropertyName tickUnitName = 
            new PropertyName("tickUnit");
    private final PropertyName tileAlignmentName = 
            new PropertyName("tileAlignment");
    private final PropertyName tileHeightName = 
            new PropertyName("tileHeight");
    private final PropertyName tileWidthName = 
            new PropertyName("tileWidth");
    private final PropertyName titleName = 
            new PropertyName("title");
    private final PropertyName titleSideName = 
            new PropertyName("titleSide");
    private final PropertyName toggleGroupName = 
            new PropertyName("toggleGroup");
    private final PropertyName tooltipName = 
            new PropertyName("tooltip");
    private final PropertyName topName = 
            new PropertyName("top");
    private final PropertyName translateXName = 
            new PropertyName("translateX");
    private final PropertyName translateYName = 
            new PropertyName("translateY");
    private final PropertyName translateZName = 
            new PropertyName("translateZ");
    private final PropertyName treeColumnName = 
            new PropertyName("treeColumn");
    private final PropertyName typeName = 
            new PropertyName("type");
    private final PropertyName underlineName = 
            new PropertyName("underline");
    private final PropertyName unitIncrementName = 
            new PropertyName("unitIncrement");
    private final PropertyName upperBoundName = 
            new PropertyName("upperBound");
    private final PropertyName userAgentStylesheetName = 
            new PropertyName("userAgentStylesheet");
    private final PropertyName valignmentName = 
            new PropertyName("valignment");
    private final PropertyName valueName = 
            new PropertyName("value");
    private final PropertyName vbarPolicyName = 
            new PropertyName("vbarPolicy");
    private final PropertyName verticalFieldOfViewName = 
            new PropertyName("verticalFieldOfView");
    private final PropertyName verticalGridLinesVisibleName = 
            new PropertyName("verticalGridLinesVisible");
    private final PropertyName verticalZeroLineVisibleName = 
            new PropertyName("verticalZeroLineVisible");
    private final PropertyName vgapName = 
            new PropertyName("vgap");
    private final PropertyName vgrowName = 
            new PropertyName("vgrow");
    private final PropertyName viewportName = 
            new PropertyName("viewport");
    private final PropertyName viewportBoundsName = 
            new PropertyName("viewportBounds");
    private final PropertyName visibleName = 
            new PropertyName("visible");
    private final PropertyName visibleAmountName = 
            new PropertyName("visibleAmount");
    private final PropertyName visibleRowCountName = 
            new PropertyName("visibleRowCount");
    private final PropertyName visitedName = 
            new PropertyName("visited");
    private final PropertyName vmaxName = 
            new PropertyName("vmax");
    private final PropertyName vminName = 
            new PropertyName("vmin");
    private final PropertyName vvalueName = 
            new PropertyName("vvalue");
    private final PropertyName widthName = 
            new PropertyName("width");
    private final PropertyName wrappingWidthName = 
            new PropertyName("wrappingWidth");
    private final PropertyName wrapTextName = 
            new PropertyName("wrapText");
    private final PropertyName xName = 
            new PropertyName("x");
    private final PropertyName XAxisName = 
            new PropertyName("XAxis");
    private final PropertyName XAxisRotationName = 
            new PropertyName("XAxisRotation");
    private final PropertyName yName = 
            new PropertyName("y");
    private final PropertyName YAxisName = 
            new PropertyName("YAxis");
    private final PropertyName zeroPositionName = 
            new PropertyName("zeroPosition");
    private final PropertyName zoomName = 
            new PropertyName("zoom");
    private final PropertyName SplitPane_resizableWithParentName = 
            new PropertyName("resizableWithParent", javafx.scene.control.SplitPane.class);
    private final PropertyName AnchorPane_bottomAnchorName = 
            new PropertyName("bottomAnchor", javafx.scene.layout.AnchorPane.class);
    private final PropertyName AnchorPane_leftAnchorName = 
            new PropertyName("leftAnchor", javafx.scene.layout.AnchorPane.class);
    private final PropertyName AnchorPane_rightAnchorName = 
            new PropertyName("rightAnchor", javafx.scene.layout.AnchorPane.class);
    private final PropertyName AnchorPane_topAnchorName = 
            new PropertyName("topAnchor", javafx.scene.layout.AnchorPane.class);
    private final PropertyName BorderPane_alignmentName = 
            new PropertyName("alignment", javafx.scene.layout.BorderPane.class);
    private final PropertyName BorderPane_marginName = 
            new PropertyName("margin", javafx.scene.layout.BorderPane.class);
    private final PropertyName FlowPane_marginName = 
            new PropertyName("margin", javafx.scene.layout.FlowPane.class);
    private final PropertyName GridPane_columnIndexName = 
            new PropertyName("columnIndex", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_columnSpanName = 
            new PropertyName("columnSpan", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_halignmentName = 
            new PropertyName("halignment", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_hgrowName = 
            new PropertyName("hgrow", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_marginName = 
            new PropertyName("margin", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_rowIndexName = 
            new PropertyName("rowIndex", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_rowSpanName = 
            new PropertyName("rowSpan", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_valignmentName = 
            new PropertyName("valignment", javafx.scene.layout.GridPane.class);
    private final PropertyName GridPane_vgrowName = 
            new PropertyName("vgrow", javafx.scene.layout.GridPane.class);
    private final PropertyName HBox_hgrowName = 
            new PropertyName("hgrow", javafx.scene.layout.HBox.class);
    private final PropertyName HBox_marginName = 
            new PropertyName("margin", javafx.scene.layout.HBox.class);
    private final PropertyName StackPane_alignmentName = 
            new PropertyName("alignment", javafx.scene.layout.StackPane.class);
    private final PropertyName StackPane_marginName = 
            new PropertyName("margin", javafx.scene.layout.StackPane.class);
    private final PropertyName TilePane_alignmentName = 
            new PropertyName("alignment", javafx.scene.layout.TilePane.class);
    private final PropertyName TilePane_marginName = 
            new PropertyName("margin", javafx.scene.layout.TilePane.class);
    private final PropertyName VBox_marginName = 
            new PropertyName("margin", javafx.scene.layout.VBox.class);
    private final PropertyName VBox_vgrowName = 
            new PropertyName("vgrow", javafx.scene.layout.VBox.class);


    // Property Metadata

    private final ValuePropertyMetadata absolutePropertyMetadata =
            new BooleanPropertyMetadata(
                absoluteName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 0));
    private final ValuePropertyMetadata acceleratorPropertyMetadata =
            new KeyCombinationPropertyMetadata(
                acceleratorName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 1));
    private final ValuePropertyMetadata alignment_TOP_LEFT_PropertyMetadata =
            new EnumerationPropertyMetadata(
                alignmentName,
                javafx.geometry.Pos.class,
                true, /* readWrite */
                javafx.geometry.Pos.TOP_LEFT, /* defaultValue */
                new InspectorPath("Properties", "Node", 0));
    private final ValuePropertyMetadata alignment_CENTER_LEFT_PropertyMetadata =
            new EnumerationPropertyMetadata(
                alignmentName,
                javafx.geometry.Pos.class,
                true, /* readWrite */
                javafx.geometry.Pos.CENTER_LEFT, /* defaultValue */
                new InspectorPath("Properties", "Node", 0));
    private final ValuePropertyMetadata alignment_CENTER_PropertyMetadata =
            new EnumerationPropertyMetadata(
                alignmentName,
                javafx.geometry.Pos.class,
                true, /* readWrite */
                javafx.geometry.Pos.CENTER, /* defaultValue */
                new InspectorPath("Properties", "Node", 0));
    private final ValuePropertyMetadata allowIndeterminatePropertyMetadata =
            new BooleanPropertyMetadata(
                allowIndeterminateName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 2));
    private final ValuePropertyMetadata alternativeColumnFillVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                alternativeColumnFillVisibleName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 88));
    private final ValuePropertyMetadata alternativeRowFillVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                alternativeRowFillVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 95));
    private final ValuePropertyMetadata anchorLocationPropertyMetadata =
            new EnumerationPropertyMetadata(
                anchorLocationName,
                javafx.stage.PopupWindow.AnchorLocation.class,
                true, /* readWrite */
                javafx.stage.PopupWindow.AnchorLocation.CONTENT_TOP_LEFT, /* defaultValue */
                new InspectorPath("Layout", "Position", 11));
    private final ValuePropertyMetadata anchorXPropertyMetadata =
            new DoublePropertyMetadata(
                anchorXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                Double.NaN, /* defaultValue */
                new InspectorPath("Layout", "Position", 9));
    private final ValuePropertyMetadata anchorYPropertyMetadata =
            new DoublePropertyMetadata(
                anchorYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                Double.NaN, /* defaultValue */
                new InspectorPath("Layout", "Position", 10));
    private final ValuePropertyMetadata animatedPropertyMetadata =
            new BooleanPropertyMetadata(
                animatedName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 41));
    private final ValuePropertyMetadata arcHeightPropertyMetadata =
            new DoublePropertyMetadata(
                arcHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 42));
    private final ValuePropertyMetadata arcWidthPropertyMetadata =
            new DoublePropertyMetadata(
                arcWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 3));
    private final ValuePropertyMetadata autoFixPropertyMetadata =
            new BooleanPropertyMetadata(
                autoFixName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 4));
    private final ValuePropertyMetadata autoHide_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                autoHideName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 43));
    private final ValuePropertyMetadata autoHide_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                autoHideName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 43));
    private final ValuePropertyMetadata autoRangingPropertyMetadata =
            new BooleanPropertyMetadata(
                autoRangingName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 70));
    private final ValuePropertyMetadata autoSizeChildrenPropertyMetadata =
            new BooleanPropertyMetadata(
                autoSizeChildrenName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Extras", 0));
    private final ValuePropertyMetadata barGapPropertyMetadata =
            new DoublePropertyMetadata(
                barGapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                4.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 76));
    private final ValuePropertyMetadata baselineOffsetPropertyMetadata =
            new DoublePropertyMetadata(
                baselineOffsetName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Extras", 1));
    private final ValuePropertyMetadata blendModePropertyMetadata =
            new EnumerationPropertyMetadata(
                blendModeName,
                javafx.scene.effect.BlendMode.class,
                "SRC_OVER", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Properties", "Extras", 0));
    private final ValuePropertyMetadata blockIncrementPropertyMetadata =
            new DoublePropertyMetadata(
                blockIncrementName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                10.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 71));
    private final ComponentPropertyMetadata bottomPropertyMetadata =
            new ComponentPropertyMetadata(
                bottomName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata boundsInLocalPropertyMetadata =
            new BoundsPropertyMetadata(
                boundsInLocalName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Bounds", 2));
    private final ValuePropertyMetadata boundsInParentPropertyMetadata =
            new BoundsPropertyMetadata(
                boundsInParentName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Bounds", 3));
    private final ValuePropertyMetadata boundsTypePropertyMetadata =
            new EnumerationPropertyMetadata(
                boundsTypeName,
                javafx.scene.text.TextBoundsType.class,
                true, /* readWrite */
                javafx.scene.text.TextBoundsType.LOGICAL, /* defaultValue */
                new InspectorPath("Layout", "Extras", 2));
    private final ValuePropertyMetadata buttonCellPropertyMetadata =
            new ListCellPropertyMetadata(
                buttonCellName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 38));
    private final ValuePropertyMetadata cachePropertyMetadata =
            new BooleanPropertyMetadata(
                cacheName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Extras", 2));
    private final ValuePropertyMetadata cacheHintPropertyMetadata =
            new EnumerationPropertyMetadata(
                cacheHintName,
                javafx.scene.CacheHint.class,
                true, /* readWrite */
                javafx.scene.CacheHint.DEFAULT, /* defaultValue */
                new InspectorPath("Properties", "Extras", 3));
    private final ValuePropertyMetadata cacheShapePropertyMetadata =
            new BooleanPropertyMetadata(
                cacheShapeName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Node", 7));
    private final ValuePropertyMetadata cancelButtonPropertyMetadata =
            new BooleanPropertyMetadata(
                cancelButtonName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 44));
    private final ValuePropertyMetadata categoriesPropertyMetadata =
            new StringListPropertyMetadata(
                categoriesName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "Specific", 77));
    private final ValuePropertyMetadata categoryGapPropertyMetadata =
            new DoublePropertyMetadata(
                categoryGapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                10.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 78));
    private final ValuePropertyMetadata categorySpacingPropertyMetadata =
            new DoublePropertyMetadata(
                categorySpacingName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 79));
    private final ComponentPropertyMetadata centerPropertyMetadata =
            new ComponentPropertyMetadata(
                centerName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata centerShapePropertyMetadata =
            new BooleanPropertyMetadata(
                centerShapeName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Node", 8));
    private final ValuePropertyMetadata centerXPropertyMetadata =
            new DoublePropertyMetadata(
                centerXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 0));
    private final ValuePropertyMetadata centerYPropertyMetadata =
            new DoublePropertyMetadata(
                centerYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 1));
    private final ComponentPropertyMetadata childrenPropertyMetadata =
            new ComponentPropertyMetadata(
                childrenName,
                NodeMetadata,
                true); /* collection */
    private final ComponentPropertyMetadata clipPropertyMetadata =
            new ComponentPropertyMetadata(
                clipName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata clockwisePropertyMetadata =
            new BooleanPropertyMetadata(
                clockwiseName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 89));
    private final ValuePropertyMetadata closablePropertyMetadata =
            new BooleanPropertyMetadata(
                closableName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 5));
    private final ValuePropertyMetadata collapsiblePropertyMetadata =
            new BooleanPropertyMetadata(
                collapsibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 60));
    private final ValuePropertyMetadata colorPropertyMetadata =
            new ColorPropertyMetadata(
                colorName,
                true, /* readWrite */
                javafx.scene.paint.Color.WHITE, /* defaultValue */
                new InspectorPath("Properties", "Specific", 6));
    private final ComponentPropertyMetadata columnConstraintsPropertyMetadata =
            new ComponentPropertyMetadata(
                columnConstraintsName,
                ColumnConstraintsMetadata,
                true); /* collection */
    private final ValuePropertyMetadata columnHalignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                columnHalignmentName,
                javafx.geometry.HPos.class,
                true, /* readWrite */
                javafx.geometry.HPos.LEFT, /* defaultValue */
                new InspectorPath("Properties", "Specific", 61));
    private final ValuePropertyMetadata columnResizePolicy_TABLEVIEW_UNCONSTRAINED_PropertyMetadata =
            new TableViewResizePolicyPropertyMetadata(
                columnResizePolicyName,
                true, /* readWrite */
                javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY, /* defaultValue */
                new InspectorPath("Properties", "Specific", 32));
    private final ValuePropertyMetadata columnResizePolicy_TREETABLEVIEW_UNCONSTRAINED_PropertyMetadata =
            new TreeTableViewResizePolicyPropertyMetadata(
                columnResizePolicyName,
                true, /* readWrite */
                javafx.scene.control.TreeTableView.UNCONSTRAINED_RESIZE_POLICY, /* defaultValue */
                new InspectorPath("Properties", "Specific", 32));
    private final ComponentPropertyMetadata columns_TableColumn_PropertyMetadata =
            new ComponentPropertyMetadata(
                columnsName,
                TableColumnMetadata,
                true); /* collection */
    private final ComponentPropertyMetadata columns_TreeTableColumn_PropertyMetadata =
            new ComponentPropertyMetadata(
                columnsName,
                TreeTableColumnMetadata,
                true); /* collection */
    private final ValuePropertyMetadata consumeAutoHidingEventsPropertyMetadata =
            new BooleanPropertyMetadata(
                consumeAutoHidingEventsName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 62));
    private final ComponentPropertyMetadata content_Node_NULL_PropertyMetadata =
            new ComponentPropertyMetadata(
                contentName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata content_String_PropertyMetadata =
            new StringPropertyMetadata(
                contentName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Specific", 9));
    private final ComponentPropertyMetadata content_Node_SEPARATOR_PropertyMetadata =
            new ComponentPropertyMetadata(
                contentName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata contentBiasPropertyMetadata =
            new EnumerationPropertyMetadata(
                contentBiasName,
                javafx.geometry.Orientation.class,
                "NONE", /* null equivalent */
                false, /* readWrite */
                new InspectorPath("Layout", "Extras", 4));
    private final ValuePropertyMetadata contentDisplayPropertyMetadata =
            new EnumerationPropertyMetadata(
                contentDisplayName,
                javafx.scene.control.ContentDisplay.class,
                true, /* readWrite */
                javafx.scene.control.ContentDisplay.LEFT, /* defaultValue */
                new InspectorPath("Properties", "Graphic", 1));
    private final ComponentPropertyMetadata contextMenuPropertyMetadata =
            new ComponentPropertyMetadata(
                contextMenuName,
                ContextMenuMetadata,
                false); /* collection */
    private final ValuePropertyMetadata contextMenuEnabledPropertyMetadata =
            new BooleanPropertyMetadata(
                contextMenuEnabledName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 10));
    private final ValuePropertyMetadata controlXPropertyMetadata =
            new DoublePropertyMetadata(
                controlXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 14));
    private final ValuePropertyMetadata controlX1PropertyMetadata =
            new DoublePropertyMetadata(
                controlX1Name,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 16));
    private final ValuePropertyMetadata controlX2PropertyMetadata =
            new DoublePropertyMetadata(
                controlX2Name,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 18));
    private final ValuePropertyMetadata controlYPropertyMetadata =
            new DoublePropertyMetadata(
                controlYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 15));
    private final ValuePropertyMetadata controlY1PropertyMetadata =
            new DoublePropertyMetadata(
                controlY1Name,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 17));
    private final ValuePropertyMetadata controlY2PropertyMetadata =
            new DoublePropertyMetadata(
                controlY2Name,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 19));
    private final ValuePropertyMetadata createSymbolsPropertyMetadata =
            new BooleanPropertyMetadata(
                createSymbolsName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 80));
    private final ValuePropertyMetadata cullFacePropertyMetadata =
            new EnumerationPropertyMetadata(
                cullFaceName,
                javafx.scene.shape.CullFace.class,
                true, /* readWrite */
                javafx.scene.shape.CullFace.BACK, /* defaultValue */
                new InspectorPath("Properties", "3D", 8));
    private final ValuePropertyMetadata currentPageIndexPropertyMetadata =
            new IntegerPropertyMetadata(
                currentPageIndexName,
                true, /* readWrite */
                0, /* defaultValue */
                new InspectorPath("Properties", "Pagination", 0));
    private final ValuePropertyMetadata cursor_HAND_PropertyMetadata =
            new CursorPropertyMetadata(
                cursorName,
                true, /* readWrite */
                javafx.scene.Cursor.HAND, /* defaultValue */
                new InspectorPath("Properties", "Node", 13));
    private final ValuePropertyMetadata cursor_NULL_PropertyMetadata =
            new CursorPropertyMetadata(
                cursorName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Node", 13));
    private final ValuePropertyMetadata defaultButtonPropertyMetadata =
            new BooleanPropertyMetadata(
                defaultButtonName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 12));
    private final ValuePropertyMetadata depthPropertyMetadata =
            new DoublePropertyMetadata(
                depthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                2.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 15));
    private final ValuePropertyMetadata depthTestPropertyMetadata =
            new EnumerationPropertyMetadata(
                depthTestName,
                javafx.scene.DepthTest.class,
                true, /* readWrite */
                javafx.scene.DepthTest.INHERIT, /* defaultValue */
                new InspectorPath("Properties", "Extras", 4));
    private final ValuePropertyMetadata disablePropertyMetadata =
            new BooleanPropertyMetadata(
                disableName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Node", 1));
    private final ValuePropertyMetadata dividerPositionsPropertyMetadata =
            new DoubleArrayPropertyMetadata(
                dividerPositionsName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "Specific", 14));
    private final ValuePropertyMetadata divisionsPropertyMetadata =
            new IntegerPropertyMetadata(
                divisionsName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "3D", 10));
    private final ValuePropertyMetadata drawModePropertyMetadata =
            new EnumerationPropertyMetadata(
                drawModeName,
                javafx.scene.shape.DrawMode.class,
                true, /* readWrite */
                javafx.scene.shape.DrawMode.FILL, /* defaultValue */
                new InspectorPath("Properties", "3D", 9));
    private final ValuePropertyMetadata editable_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                editableName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 15));
    private final ValuePropertyMetadata editable_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                editableName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 15));
    private final ValuePropertyMetadata effectPropertyMetadata =
            new EffectPropertyMetadata(
                effectName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Node", 14));
    private final ValuePropertyMetadata effectiveNodeOrientationPropertyMetadata =
            new EnumerationPropertyMetadata(
                effectiveNodeOrientationName,
                javafx.geometry.NodeOrientation.class,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Extras", 7));
    private final ComponentPropertyMetadata elementsPropertyMetadata =
            new ComponentPropertyMetadata(
                elementsName,
                PathElementMetadata,
                true); /* collection */
    private final ValuePropertyMetadata ellipsisStringPropertyMetadata =
            new StringPropertyMetadata(
                ellipsisStringName,
                true, /* readWrite */
                "...", /* defaultValue */
                new InspectorPath("Properties", "Text", 10));
    private final ValuePropertyMetadata endMarginPropertyMetadata =
            new DoublePropertyMetadata(
                endMarginName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                5.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 96));
    private final ValuePropertyMetadata endXPropertyMetadata =
            new DoublePropertyMetadata(
                endXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 7));
    private final ValuePropertyMetadata endYPropertyMetadata =
            new DoublePropertyMetadata(
                endYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 8));
    private final ValuePropertyMetadata expandedPropertyMetadata =
            new BooleanPropertyMetadata(
                expandedName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 16));
    private final ValuePropertyMetadata expandedItemCountPropertyMetadata =
            new IntegerPropertyMetadata(
                expandedItemCountName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 17));
    private final ValuePropertyMetadata farClipPropertyMetadata =
            new DoublePropertyMetadata(
                farClipName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                100.0, /* defaultValue */
                new InspectorPath("Properties", "3D", 3));
    private final ValuePropertyMetadata fieldOfViewPropertyMetadata =
            new DoublePropertyMetadata(
                fieldOfViewName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.ANGLE,
                true, /* readWrite */
                30.0, /* defaultValue */
                new InspectorPath("Properties", "3D", 4));
    private final ValuePropertyMetadata fill_NULL_PropertyMetadata =
            new PaintPropertyMetadata(
                fillName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 18));
    private final ValuePropertyMetadata fill_BLACK_PropertyMetadata =
            new PaintPropertyMetadata(
                fillName,
                true, /* readWrite */
                javafx.scene.paint.Color.BLACK, /* defaultValue */
                new InspectorPath("Properties", "Specific", 18));
    private final ValuePropertyMetadata fillHeightPropertyMetadata =
            new BooleanPropertyMetadata(
                fillHeightName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Specific", 0));
    private final ValuePropertyMetadata fillRulePropertyMetadata =
            new EnumerationPropertyMetadata(
                fillRuleName,
                javafx.scene.shape.FillRule.class,
                true, /* readWrite */
                javafx.scene.shape.FillRule.NON_ZERO, /* defaultValue */
                new InspectorPath("Properties", "Specific", 19));
    private final ValuePropertyMetadata fillWidthPropertyMetadata =
            new BooleanPropertyMetadata(
                fillWidthName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Specific", 1));
    private final ValuePropertyMetadata fitHeightPropertyMetadata =
            new DoublePropertyMetadata(
                fitHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 14));
    private final ValuePropertyMetadata fitToHeightPropertyMetadata =
            new BooleanPropertyMetadata(
                fitToHeightName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Layout", "Specific", 9));
    private final ValuePropertyMetadata fitToWidthPropertyMetadata =
            new BooleanPropertyMetadata(
                fitToWidthName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Layout", "Specific", 8));
    private final ValuePropertyMetadata fitWidthPropertyMetadata =
            new DoublePropertyMetadata(
                fitWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 13));
    private final ValuePropertyMetadata fixedCellSizePropertyMetadata =
            new DoublePropertyMetadata(
                fixedCellSizeName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 127));
    private final ValuePropertyMetadata fixedEyeAtCameraZeroPropertyMetadata =
            new BooleanPropertyMetadata(
                fixedEyeAtCameraZeroName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "3D", 6));
    private final ValuePropertyMetadata focusTraversable_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                focusTraversableName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Node", 6));
    private final ValuePropertyMetadata focusTraversable_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                focusTraversableName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Node", 6));
    private final ValuePropertyMetadata fontPropertyMetadata =
            new FontPropertyMetadata(
                fontName,
                true, /* readWrite */
                javafx.scene.text.Font.getDefault(), /* defaultValue */
                new InspectorPath("Properties", "Text", 3));
    private final ValuePropertyMetadata fontScalePropertyMetadata =
            new DoublePropertyMetadata(
                fontScaleName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Text", 0));
    private final ValuePropertyMetadata fontSmoothingType_GRAY_PropertyMetadata =
            new EnumerationPropertyMetadata(
                fontSmoothingTypeName,
                javafx.scene.text.FontSmoothingType.class,
                true, /* readWrite */
                javafx.scene.text.FontSmoothingType.GRAY, /* defaultValue */
                new InspectorPath("Properties", "Text", 4));
    private final ValuePropertyMetadata fontSmoothingType_LCD_PropertyMetadata =
            new EnumerationPropertyMetadata(
                fontSmoothingTypeName,
                javafx.scene.text.FontSmoothingType.class,
                true, /* readWrite */
                javafx.scene.text.FontSmoothingType.LCD, /* defaultValue */
                new InspectorPath("Properties", "Text", 4));
    private final ValuePropertyMetadata forceZeroInRangePropertyMetadata =
            new BooleanPropertyMetadata(
                forceZeroInRangeName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 120));
    private final ValuePropertyMetadata gapStartAndEndPropertyMetadata =
            new BooleanPropertyMetadata(
                gapStartAndEndName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 101));
    private final ComponentPropertyMetadata graphicPropertyMetadata =
            new ComponentPropertyMetadata(
                graphicName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata graphicTextGapPropertyMetadata =
            new DoublePropertyMetadata(
                graphicTextGapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                4.0, /* defaultValue */
                new InspectorPath("Properties", "Graphic", 0));
    private final ValuePropertyMetadata gridLinesVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                gridLinesVisibleName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 20));
    private final ValuePropertyMetadata halignment_NULL_PropertyMetadata =
            new EnumerationPropertyMetadata(
                halignmentName,
                javafx.geometry.HPos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Specific", 4));
    private final ValuePropertyMetadata halignment_CENTER_PropertyMetadata =
            new EnumerationPropertyMetadata(
                halignmentName,
                javafx.geometry.HPos.class,
                true, /* readWrite */
                javafx.geometry.HPos.CENTER, /* defaultValue */
                new InspectorPath("Layout", "Specific", 4));
    private final ValuePropertyMetadata hbarPolicyPropertyMetadata =
            new EnumerationPropertyMetadata(
                hbarPolicyName,
                javafx.scene.control.ScrollPane.ScrollBarPolicy.class,
                true, /* readWrite */
                javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED, /* defaultValue */
                new InspectorPath("Properties", "Specific", 46));
    private final ValuePropertyMetadata height_Double_200_PropertyMetadata =
            new DoublePropertyMetadata(
                heightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                2.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 7));
    private final ValuePropertyMetadata height_Double_0_PropertyMetadata =
            new DoublePropertyMetadata(
                heightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 7));
    private final ValuePropertyMetadata height_Double_ro_PropertyMetadata =
            new DoublePropertyMetadata(
                heightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Size", 7));
    private final ValuePropertyMetadata hgapPropertyMetadata =
            new DoublePropertyMetadata(
                hgapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Internal", 0));
    private final ValuePropertyMetadata hgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                hgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Specific", 2));
    private final ValuePropertyMetadata hideOnClick_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                hideOnClickName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 21));
    private final ValuePropertyMetadata hideOnClick_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                hideOnClickName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 21));
    private final ValuePropertyMetadata hideOnEscapePropertyMetadata =
            new BooleanPropertyMetadata(
                hideOnEscapeName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 63));
    private final ValuePropertyMetadata hmaxPropertyMetadata =
            new DoublePropertyMetadata(
                hmaxName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 90));
    private final ValuePropertyMetadata hminPropertyMetadata =
            new DoublePropertyMetadata(
                hminName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 81));
    private final ValuePropertyMetadata horizontalGridLinesVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                horizontalGridLinesVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 102));
    private final ValuePropertyMetadata horizontalZeroLineVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                horizontalZeroLineVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 107));
    private final ValuePropertyMetadata htmlTextPropertyMetadata =
            new StringPropertyMetadata(
                htmlTextName,
                true, /* readWrite */
                "<html><head></head><body contenteditable=\"true\"></body></html>", /* defaultValue */
                new InspectorPath("Properties", "Specific", 22));
    private final ValuePropertyMetadata hvaluePropertyMetadata =
            new DoublePropertyMetadata(
                hvalueName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 72));
    private final ValuePropertyMetadata idPropertyMetadata =
            new StringPropertyMetadata(
                idName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 3));
    private final ValuePropertyMetadata imagePropertyMetadata =
            new ImagePropertyMetadata(
                imageName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 23));
    private final ValuePropertyMetadata indeterminate_Boolean_PropertyMetadata =
            new BooleanPropertyMetadata(
                indeterminateName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 47));
    private final ValuePropertyMetadata indeterminate_Boolean_ro_PropertyMetadata =
            new BooleanPropertyMetadata(
                indeterminateName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 47));
    private final ValuePropertyMetadata insetsPropertyMetadata =
            new InsetsPropertyMetadata(
                insetsName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Extras", 5));
    private final ComponentPropertyMetadata items_MenuItem_PropertyMetadata =
            new ComponentPropertyMetadata(
                itemsName,
                MenuItemMetadata,
                true); /* collection */
    private final ComponentPropertyMetadata items_Node_PropertyMetadata =
            new ComponentPropertyMetadata(
                itemsName,
                NodeMetadata,
                true); /* collection */
    private final ValuePropertyMetadata labelPropertyMetadata =
            new StringPropertyMetadata(
                labelName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Specific", 24));
    private final ComponentPropertyMetadata labelForPropertyMetadata =
            new ComponentPropertyMetadata(
                labelForName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata labelFormatterPropertyMetadata =
            new StringConverterPropertyMetadata(
                labelFormatterName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 111));
    private final ValuePropertyMetadata labelLineLengthPropertyMetadata =
            new DoublePropertyMetadata(
                labelLineLengthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                20.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 108));
    private final ValuePropertyMetadata labelPaddingPropertyMetadata =
            new InsetsPropertyMetadata(
                labelPaddingName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Extras", 6));
    private final ValuePropertyMetadata labelsVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                labelsVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 103));
    private final ValuePropertyMetadata largeArcFlagPropertyMetadata =
            new BooleanPropertyMetadata(
                largeArcFlagName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 125));
    private final ValuePropertyMetadata layoutBoundsPropertyMetadata =
            new BoundsPropertyMetadata(
                layoutBoundsName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Bounds", 0));
    private final ValuePropertyMetadata layoutXPropertyMetadata =
            new DoublePropertyMetadata(
                layoutXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 2));
    private final ValuePropertyMetadata layoutYPropertyMetadata =
            new DoublePropertyMetadata(
                layoutYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 3));
    private final ComponentPropertyMetadata leftPropertyMetadata =
            new ComponentPropertyMetadata(
                leftName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata legendSidePropertyMetadata =
            new EnumerationPropertyMetadata(
                legendSideName,
                javafx.geometry.Side.class,
                true, /* readWrite */
                javafx.geometry.Side.BOTTOM, /* defaultValue */
                new InspectorPath("Properties", "Specific", 73));
    private final ValuePropertyMetadata legendVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                legendVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 64));
    private final ValuePropertyMetadata length_Double_PropertyMetadata =
            new DoublePropertyMetadata(
                lengthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 17));
    private final ValuePropertyMetadata length_Integer_ro_PropertyMetadata =
            new IntegerPropertyMetadata(
                lengthName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Size", 17));
    private final ValuePropertyMetadata lightOnPropertyMetadata =
            new BooleanPropertyMetadata(
                lightOnName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "3D", 7));
    private final ValuePropertyMetadata lineSpacingPropertyMetadata =
            new DoublePropertyMetadata(
                lineSpacingName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Text", 13));
    private final ValuePropertyMetadata lowerBoundPropertyMetadata =
            new DoublePropertyMetadata(
                lowerBoundName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 91));
    private final ValuePropertyMetadata majorTickUnitPropertyMetadata =
            new DoublePropertyMetadata(
                majorTickUnitName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                25.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 92));
    private final ValuePropertyMetadata materialPropertyMetadata =
            new MaterialPropertyMetadata(
                materialName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "3D", 0));
    private final ValuePropertyMetadata maxPropertyMetadata =
            new DoublePropertyMetadata(
                maxName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                100.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 48));
    private final ValuePropertyMetadata maxHeight_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                maxHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 5));
    private final ValuePropertyMetadata maxHeight_MAX_PropertyMetadata =
            new DoublePropertyMetadata(
                maxHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                Double.MAX_VALUE, /* defaultValue */
                new InspectorPath("Layout", "Size", 5));
    private final ValuePropertyMetadata maxPageIndicatorCountPropertyMetadata =
            new IntegerPropertyMetadata(
                maxPageIndicatorCountName,
                true, /* readWrite */
                10, /* defaultValue */
                new InspectorPath("Properties", "Pagination", 1));
    private final ValuePropertyMetadata maxWidth_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                maxWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 4));
    private final ValuePropertyMetadata maxWidth_500000_PropertyMetadata =
            new DoublePropertyMetadata(
                maxWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                5000.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 4));
    private final ValuePropertyMetadata maxWidth_MAX_PropertyMetadata =
            new DoublePropertyMetadata(
                maxWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                Double.MAX_VALUE, /* defaultValue */
                new InspectorPath("Layout", "Size", 4));
    private final ComponentPropertyMetadata menusPropertyMetadata =
            new ComponentPropertyMetadata(
                menusName,
                MenuMetadata,
                true); /* collection */
    private final ValuePropertyMetadata meshPropertyMetadata =
            new MeshPropertyMetadata(
                meshName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "3D", 1));
    private final ValuePropertyMetadata minPropertyMetadata =
            new DoublePropertyMetadata(
                minName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 27));
    private final ValuePropertyMetadata minHeight_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                minHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 1));
    private final ValuePropertyMetadata minHeight_0_PropertyMetadata =
            new DoublePropertyMetadata(
                minHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 1));
    private final ValuePropertyMetadata minorTickCount_3_PropertyMetadata =
            new IntegerPropertyMetadata(
                minorTickCountName,
                true, /* readWrite */
                3, /* defaultValue */
                new InspectorPath("Properties", "Specific", 97));
    private final ValuePropertyMetadata minorTickCount_5_PropertyMetadata =
            new IntegerPropertyMetadata(
                minorTickCountName,
                true, /* readWrite */
                5, /* defaultValue */
                new InspectorPath("Properties", "Specific", 97));
    private final ValuePropertyMetadata minorTickLengthPropertyMetadata =
            new DoublePropertyMetadata(
                minorTickLengthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                5.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 119));
    private final ValuePropertyMetadata minorTickVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                minorTickVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 117));
    private final ValuePropertyMetadata minWidth_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                minWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 0));
    private final ValuePropertyMetadata minWidth_1000_PropertyMetadata =
            new DoublePropertyMetadata(
                minWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                10.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 0));
    private final ValuePropertyMetadata minWidth_0_PropertyMetadata =
            new DoublePropertyMetadata(
                minWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 0));
    private final ValuePropertyMetadata mnemonicParsing_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                mnemonicParsingName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Extras", 1));
    private final ValuePropertyMetadata mnemonicParsing_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                mnemonicParsingName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Extras", 1));
    private final ValuePropertyMetadata mouseTransparentPropertyMetadata =
            new BooleanPropertyMetadata(
                mouseTransparentName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Extras", 7));
    private final ValuePropertyMetadata nearClipPropertyMetadata =
            new DoublePropertyMetadata(
                nearClipName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.1, /* defaultValue */
                new InspectorPath("Properties", "3D", 2));
    private final ValuePropertyMetadata nodeOrientation_LEFT_TO_RIGHT_PropertyMetadata =
            new EnumerationPropertyMetadata(
                nodeOrientationName,
                javafx.geometry.NodeOrientation.class,
                true, /* readWrite */
                javafx.geometry.NodeOrientation.LEFT_TO_RIGHT, /* defaultValue */
                new InspectorPath("Properties", "Node", 4));
    private final ValuePropertyMetadata nodeOrientation_INHERIT_PropertyMetadata =
            new EnumerationPropertyMetadata(
                nodeOrientationName,
                javafx.geometry.NodeOrientation.class,
                true, /* readWrite */
                javafx.geometry.NodeOrientation.INHERIT, /* defaultValue */
                new InspectorPath("Properties", "Node", 4));
    private final ValuePropertyMetadata onActionPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onActionName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Main", 0));
    private final ValuePropertyMetadata onAutoHidePropertyMetadata =
            new EventHandlerPropertyMetadata(
                onAutoHideName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "HideShow", 2));
    private final ValuePropertyMetadata onClosedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onClosedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Closing", 1));
    private final ValuePropertyMetadata onCloseRequestPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onCloseRequestName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Closing", 0));
    private final ValuePropertyMetadata onContextMenuRequestedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onContextMenuRequestedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 0));
    private final ValuePropertyMetadata onDragDetectedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragDetectedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 0));
    private final ValuePropertyMetadata onDragDonePropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragDoneName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 1));
    private final ValuePropertyMetadata onDragDroppedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragDroppedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 2));
    private final ValuePropertyMetadata onDragEnteredPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragEnteredName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 3));
    private final ValuePropertyMetadata onDragExitedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragExitedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 4));
    private final ValuePropertyMetadata onDragOverPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onDragOverName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 5));
    private final ValuePropertyMetadata onEditCancelPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onEditCancelName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Edit", 2));
    private final ValuePropertyMetadata onEditCommitPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onEditCommitName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Edit", 1));
    private final ValuePropertyMetadata onEditStartPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onEditStartName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Edit", 0));
    private final ValuePropertyMetadata onErrorPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onErrorName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Main", 2));
    private final ValuePropertyMetadata onHiddenPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onHiddenName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "HideShow", 0));
    private final ValuePropertyMetadata onHidingPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onHidingName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "HideShow", 1));
    private final ValuePropertyMetadata onInputMethodTextChangedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onInputMethodTextChangedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Keyboard", 0));
    private final ValuePropertyMetadata onKeyPressedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onKeyPressedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Keyboard", 1));
    private final ValuePropertyMetadata onKeyReleasedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onKeyReleasedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Keyboard", 2));
    private final ValuePropertyMetadata onKeyTypedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onKeyTypedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Keyboard", 3));
    private final ValuePropertyMetadata onMenuValidationPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMenuValidationName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Main", 1));
    private final ValuePropertyMetadata onMouseClickedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseClickedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 1));
    private final ValuePropertyMetadata onMouseDragEnteredPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseDragEnteredName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 6));
    private final ValuePropertyMetadata onMouseDragExitedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseDragExitedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 7));
    private final ValuePropertyMetadata onMouseDraggedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseDraggedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 2));
    private final ValuePropertyMetadata onMouseDragOverPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseDragOverName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 8));
    private final ValuePropertyMetadata onMouseDragReleasedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseDragReleasedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "DragDrop", 9));
    private final ValuePropertyMetadata onMouseEnteredPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseEnteredName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 3));
    private final ValuePropertyMetadata onMouseExitedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseExitedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 4));
    private final ValuePropertyMetadata onMouseMovedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseMovedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 5));
    private final ValuePropertyMetadata onMousePressedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMousePressedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 6));
    private final ValuePropertyMetadata onMouseReleasedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onMouseReleasedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 7));
    private final ValuePropertyMetadata onRotatePropertyMetadata =
            new EventHandlerPropertyMetadata(
                onRotateName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Rotation", 0));
    private final ValuePropertyMetadata onRotationFinishedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onRotationFinishedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Rotation", 2));
    private final ValuePropertyMetadata onRotationStartedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onRotationStartedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Rotation", 1));
    private final ValuePropertyMetadata onScrollPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onScrollName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 8));
    private final ValuePropertyMetadata onScrollFinishedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onScrollFinishedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 10));
    private final ValuePropertyMetadata onScrollStartedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onScrollStartedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 9));
    private final ValuePropertyMetadata onScrollToPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onScrollToName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 11));
    private final ValuePropertyMetadata onScrollToColumnPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onScrollToColumnName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Mouse", 12));
    private final ValuePropertyMetadata onSelectionChangedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSelectionChangedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Edit", 3));
    private final ValuePropertyMetadata onShowingPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onShowingName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "HideShow", 3));
    private final ValuePropertyMetadata onShownPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onShownName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "HideShow", 4));
    private final ValuePropertyMetadata onSortPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSortName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Main", 3));
    private final ValuePropertyMetadata onSwipeDownPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSwipeDownName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Swipe", 3));
    private final ValuePropertyMetadata onSwipeLeftPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSwipeLeftName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Swipe", 0));
    private final ValuePropertyMetadata onSwipeRightPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSwipeRightName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Swipe", 1));
    private final ValuePropertyMetadata onSwipeUpPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onSwipeUpName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Swipe", 2));
    private final ValuePropertyMetadata onTouchMovedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onTouchMovedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Touch", 0));
    private final ValuePropertyMetadata onTouchPressedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onTouchPressedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Touch", 1));
    private final ValuePropertyMetadata onTouchReleasedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onTouchReleasedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Touch", 2));
    private final ValuePropertyMetadata onTouchStationaryPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onTouchStationaryName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Touch", 3));
    private final ValuePropertyMetadata onZoomPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onZoomName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Zoom", 0));
    private final ValuePropertyMetadata onZoomFinishedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onZoomFinishedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Zoom", 2));
    private final ValuePropertyMetadata onZoomStartedPropertyMetadata =
            new EventHandlerPropertyMetadata(
                onZoomStartedName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Code", "Zoom", 1));
    private final ValuePropertyMetadata opacityPropertyMetadata =
            new DoublePropertyMetadata(
                opacityName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.OPACITY,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Node", 2));
    private final ValuePropertyMetadata opaqueInsetsPropertyMetadata =
            new InsetsPropertyMetadata(
                opaqueInsetsName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Node", 12));
    private final ValuePropertyMetadata orientation_HORIZONTAL_PropertyMetadata =
            new EnumerationPropertyMetadata(
                orientationName,
                javafx.geometry.Orientation.class,
                true, /* readWrite */
                javafx.geometry.Orientation.HORIZONTAL, /* defaultValue */
                new InspectorPath("Properties", "Node", 3));
    private final ValuePropertyMetadata orientation_VERTICAL_PropertyMetadata =
            new EnumerationPropertyMetadata(
                orientationName,
                javafx.geometry.Orientation.class,
                true, /* readWrite */
                javafx.geometry.Orientation.VERTICAL, /* defaultValue */
                new InspectorPath("Properties", "Node", 3));
    private final ValuePropertyMetadata paddingPropertyMetadata =
            new InsetsPropertyMetadata(
                paddingName,
                true, /* readWrite */
                javafx.geometry.Insets.EMPTY, /* defaultValue */
                new InspectorPath("Layout", "Internal", 2));
    private final ValuePropertyMetadata pageCountPropertyMetadata =
            new IntegerPropertyMetadata(
                pageCountName,
                true, /* readWrite */
                2147483647, /* defaultValue */
                new InspectorPath("Properties", "Pagination", 2));
    private final ComponentPropertyMetadata panesPropertyMetadata =
            new ComponentPropertyMetadata(
                panesName,
                TitledPaneMetadata,
                true); /* collection */
    private final ValuePropertyMetadata pannablePropertyMetadata =
            new BooleanPropertyMetadata(
                pannableName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 28));
    private final ValuePropertyMetadata percentHeightPropertyMetadata =
            new DoublePropertyMetadata(
                percentHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.PERCENTAGE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 19));
    private final ValuePropertyMetadata percentWidthPropertyMetadata =
            new DoublePropertyMetadata(
                percentWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.PERCENTAGE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 18));
    private final ValuePropertyMetadata pickOnBounds_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                pickOnBoundsName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Extras", 8));
    private final ValuePropertyMetadata pickOnBounds_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                pickOnBoundsName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Extras", 8));
    private final ComponentPropertyMetadata placeholderPropertyMetadata =
            new ComponentPropertyMetadata(
                placeholderName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata pointsPropertyMetadata =
            new DoubleListPropertyMetadata(
                pointsName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Layout", "Position", 4));
    private final ValuePropertyMetadata popupSidePropertyMetadata =
            new EnumerationPropertyMetadata(
                popupSideName,
                javafx.geometry.Side.class,
                true, /* readWrite */
                javafx.geometry.Side.BOTTOM, /* defaultValue */
                new InspectorPath("Properties", "Specific", 29));
    private final ValuePropertyMetadata prefColumnCount_40_PropertyMetadata =
            new IntegerPropertyMetadata(
                prefColumnCountName,
                true, /* readWrite */
                40, /* defaultValue */
                new InspectorPath("Layout", "Size", 8));
    private final ValuePropertyMetadata prefColumnCount_12_PropertyMetadata =
            new IntegerPropertyMetadata(
                prefColumnCountName,
                true, /* readWrite */
                12, /* defaultValue */
                new InspectorPath("Layout", "Size", 8));
    private final ValuePropertyMetadata prefColumnsPropertyMetadata =
            new IntegerPropertyMetadata(
                prefColumnsName,
                true, /* readWrite */
                5, /* defaultValue */
                new InspectorPath("Layout", "Specific", 16));
    private final ValuePropertyMetadata prefHeight_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                prefHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 3));
    private final ValuePropertyMetadata prefHeight_60000_PropertyMetadata =
            new DoublePropertyMetadata(
                prefHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE,
                true, /* readWrite */
                600.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 3));
    private final ValuePropertyMetadata prefRowCountPropertyMetadata =
            new IntegerPropertyMetadata(
                prefRowCountName,
                true, /* readWrite */
                10, /* defaultValue */
                new InspectorPath("Layout", "Size", 9));
    private final ValuePropertyMetadata prefRowsPropertyMetadata =
            new IntegerPropertyMetadata(
                prefRowsName,
                true, /* readWrite */
                5, /* defaultValue */
                new InspectorPath("Layout", "Specific", 17));
    private final ValuePropertyMetadata prefTileHeightPropertyMetadata =
            new DoublePropertyMetadata(
                prefTileHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 15));
    private final ValuePropertyMetadata prefTileWidthPropertyMetadata =
            new DoublePropertyMetadata(
                prefTileWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 14));
    private final ValuePropertyMetadata prefViewportHeightPropertyMetadata =
            new DoublePropertyMetadata(
                prefViewportHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 7));
    private final ValuePropertyMetadata prefViewportWidthPropertyMetadata =
            new DoublePropertyMetadata(
                prefViewportWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 6));
    private final ValuePropertyMetadata prefWidth_COMPUTED_PropertyMetadata =
            new DoublePropertyMetadata(
                prefWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 2));
    private final ValuePropertyMetadata prefWidth_8000_PropertyMetadata =
            new DoublePropertyMetadata(
                prefWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE,
                true, /* readWrite */
                80.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 2));
    private final ValuePropertyMetadata prefWidth_80000_PropertyMetadata =
            new DoublePropertyMetadata(
                prefWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE,
                true, /* readWrite */
                800.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 2));
    private final ValuePropertyMetadata prefWrapLengthPropertyMetadata =
            new DoublePropertyMetadata(
                prefWrapLengthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                400.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 74));
    private final ValuePropertyMetadata preserveRatio_false_PropertyMetadata =
            new BooleanPropertyMetadata(
                preserveRatioName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 30));
    private final ValuePropertyMetadata preserveRatio_true_PropertyMetadata =
            new BooleanPropertyMetadata(
                preserveRatioName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 30));
    private final ValuePropertyMetadata progressPropertyMetadata =
            new DoublePropertyMetadata(
                progressName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.PROGRESS,
                true, /* readWrite */
                -1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 31));
    private final ValuePropertyMetadata promptTextPropertyMetadata =
            new StringPropertyMetadata(
                promptTextName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Text", 1));
    private final ValuePropertyMetadata radius_0_PropertyMetadata =
            new DoublePropertyMetadata(
                radiusName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 10));
    private final ValuePropertyMetadata radius_100_PropertyMetadata =
            new DoublePropertyMetadata(
                radiusName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 10));
    private final ValuePropertyMetadata radiusXPropertyMetadata =
            new DoublePropertyMetadata(
                radiusXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 11));
    private final ValuePropertyMetadata radiusYPropertyMetadata =
            new DoublePropertyMetadata(
                radiusYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 12));
    private final ValuePropertyMetadata resizable_Boolean_ro_PropertyMetadata =
            new BooleanPropertyMetadata(
                resizableName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Extras", 3));
    private final ValuePropertyMetadata resizable_Boolean_PropertyMetadata =
            new BooleanPropertyMetadata(
                resizableName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Extras", 3));
    private final ComponentPropertyMetadata rightPropertyMetadata =
            new ComponentPropertyMetadata(
                rightName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata rotatePropertyMetadata =
            new DoublePropertyMetadata(
                rotateName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.ANGLE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 0));
    private final ValuePropertyMetadata rotateGraphicPropertyMetadata =
            new BooleanPropertyMetadata(
                rotateGraphicName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 65));
    private final ValuePropertyMetadata rotationAxisPropertyMetadata =
            new Point3DPropertyMetadata(
                rotationAxisName,
                true, /* readWrite */
                new javafx.geometry.Point3D(0.0, 0.0, 1.0), /* defaultValue */
                new InspectorPath("Layout", "Transforms", 1));
    private final ComponentPropertyMetadata rowConstraintsPropertyMetadata =
            new ComponentPropertyMetadata(
                rowConstraintsName,
                RowConstraintsMetadata,
                true); /* collection */
    private final ValuePropertyMetadata rowValignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                rowValignmentName,
                javafx.geometry.VPos.class,
                true, /* readWrite */
                javafx.geometry.VPos.CENTER, /* defaultValue */
                new InspectorPath("Properties", "Specific", 49));
    private final ValuePropertyMetadata scalePropertyMetadata =
            new DoublePropertyMetadata(
                scaleName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 82));
    private final ValuePropertyMetadata scaleShapePropertyMetadata =
            new BooleanPropertyMetadata(
                scaleShapeName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Node", 9));
    private final ValuePropertyMetadata scaleXPropertyMetadata =
            new DoublePropertyMetadata(
                scaleXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 2));
    private final ValuePropertyMetadata scaleYPropertyMetadata =
            new DoublePropertyMetadata(
                scaleYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 3));
    private final ValuePropertyMetadata scaleZPropertyMetadata =
            new DoublePropertyMetadata(
                scaleZName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 4));
    private final ComponentPropertyMetadata scopePropertyMetadata =
            new ComponentPropertyMetadata(
                scopeName,
                NodeMetadata,
                true); /* collection */
    private final ValuePropertyMetadata scrollLeftPropertyMetadata =
            new DoublePropertyMetadata(
                scrollLeftName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Text", 14));
    private final ValuePropertyMetadata scrollTopPropertyMetadata =
            new DoublePropertyMetadata(
                scrollTopName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Text", 15));
    private final ValuePropertyMetadata selected_Boolean_PropertyMetadata =
            new BooleanPropertyMetadata(
                selectedName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 33));
    private final ValuePropertyMetadata selected_Boolean_ro_PropertyMetadata =
            new BooleanPropertyMetadata(
                selectedName,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 33));
    private final ComponentPropertyMetadata shapePropertyMetadata =
            new ComponentPropertyMetadata(
                shapeName,
                ShapeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata showRootPropertyMetadata =
            new BooleanPropertyMetadata(
                showRootName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 50));
    private final ValuePropertyMetadata showTickLabelsPropertyMetadata =
            new BooleanPropertyMetadata(
                showTickLabelsName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 83));
    private final ValuePropertyMetadata showTickMarksPropertyMetadata =
            new BooleanPropertyMetadata(
                showTickMarksName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 75));
    private final ValuePropertyMetadata showWeekNumbersPropertyMetadata =
            new BooleanPropertyMetadata(
                showWeekNumbersName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 126));
    private final ValuePropertyMetadata side_NULL_PropertyMetadata =
            new EnumerationPropertyMetadata(
                sideName,
                javafx.geometry.Side.class,
                "BOTTOM", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Properties", "Specific", 34));
    private final ValuePropertyMetadata side_TOP_PropertyMetadata =
            new EnumerationPropertyMetadata(
                sideName,
                javafx.geometry.Side.class,
                true, /* readWrite */
                javafx.geometry.Side.TOP, /* defaultValue */
                new InspectorPath("Properties", "Specific", 34));
    private final ValuePropertyMetadata smoothPropertyMetadata =
            new BooleanPropertyMetadata(
                smoothName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 51));
    private final ValuePropertyMetadata snapToPixelPropertyMetadata =
            new BooleanPropertyMetadata(
                snapToPixelName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Extras", 5));
    private final ValuePropertyMetadata snapToTicksPropertyMetadata =
            new BooleanPropertyMetadata(
                snapToTicksName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 104));
    private final ValuePropertyMetadata sortablePropertyMetadata =
            new BooleanPropertyMetadata(
                sortableName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 52));
    private final ValuePropertyMetadata sortModePropertyMetadata =
            new EnumerationPropertyMetadata(
                sortModeName,
                javafx.scene.control.TreeSortMode.class,
                true, /* readWrite */
                javafx.scene.control.TreeSortMode.ALL_DESCENDANTS, /* defaultValue */
                new InspectorPath("Properties", "Specific", 54));
    private final ComponentPropertyMetadata sortNodePropertyMetadata =
            new ComponentPropertyMetadata(
                sortNodeName,
                NodeMetadata,
                false); /* collection */
    private final ComponentPropertyMetadata sortOrderPropertyMetadata =
            new ComponentPropertyMetadata(
                sortOrderName,
                TableColumnMetadata,
                true); /* collection */
    private final ValuePropertyMetadata sortType_SortType_PropertyMetadata =
            new EnumerationPropertyMetadata(
                sortTypeName,
                javafx.scene.control.TableColumn.SortType.class,
                true, /* readWrite */
                javafx.scene.control.TableColumn.SortType.ASCENDING, /* defaultValue */
                new InspectorPath("Properties", "Specific", 66));
    private final ValuePropertyMetadata spacingPropertyMetadata =
            new DoublePropertyMetadata(
                spacingName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Internal", 3));
    private final ValuePropertyMetadata startAnglePropertyMetadata =
            new DoublePropertyMetadata(
                startAngleName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.ANGLE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 16));
    private final ValuePropertyMetadata startMarginPropertyMetadata =
            new DoublePropertyMetadata(
                startMarginName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                5.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 93));
    private final ValuePropertyMetadata startXPropertyMetadata =
            new DoublePropertyMetadata(
                startXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 5));
    private final ValuePropertyMetadata startYPropertyMetadata =
            new DoublePropertyMetadata(
                startYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 6));
    private final ValuePropertyMetadata strikethroughPropertyMetadata =
            new BooleanPropertyMetadata(
                strikethroughName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Text", 11));
    private final ValuePropertyMetadata stroke_BLACK_PropertyMetadata =
            new PaintPropertyMetadata(
                strokeName,
                true, /* readWrite */
                javafx.scene.paint.Color.BLACK, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 0));
    private final ValuePropertyMetadata stroke_NULL_PropertyMetadata =
            new PaintPropertyMetadata(
                strokeName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 0));
    private final ValuePropertyMetadata strokeDashArrayPropertyMetadata =
            new DoubleListPropertyMetadata(
                strokeDashArrayName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "Specific", 122));
    private final ValuePropertyMetadata strokeDashOffsetPropertyMetadata =
            new DoublePropertyMetadata(
                strokeDashOffsetName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 6));
    private final ValuePropertyMetadata strokeLineCapPropertyMetadata =
            new EnumerationPropertyMetadata(
                strokeLineCapName,
                javafx.scene.shape.StrokeLineCap.class,
                true, /* readWrite */
                javafx.scene.shape.StrokeLineCap.SQUARE, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 3));
    private final ValuePropertyMetadata strokeLineJoinPropertyMetadata =
            new EnumerationPropertyMetadata(
                strokeLineJoinName,
                javafx.scene.shape.StrokeLineJoin.class,
                true, /* readWrite */
                javafx.scene.shape.StrokeLineJoin.MITER, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 4));
    private final ValuePropertyMetadata strokeMiterLimitPropertyMetadata =
            new DoublePropertyMetadata(
                strokeMiterLimitName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                10.0, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 5));
    private final ValuePropertyMetadata strokeTypePropertyMetadata =
            new EnumerationPropertyMetadata(
                strokeTypeName,
                javafx.scene.shape.StrokeType.class,
                true, /* readWrite */
                javafx.scene.shape.StrokeType.CENTERED, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 2));
    private final ValuePropertyMetadata strokeWidthPropertyMetadata =
            new DoublePropertyMetadata(
                strokeWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Stroke", 1));
    private final ValuePropertyMetadata stylePropertyMetadata =
            new StringPropertyMetadata(
                styleName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 0));
    private final ValuePropertyMetadata styleClass_c4_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("accordion"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c34_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("chart"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c42_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("axis"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c1_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("chart","bar-chart"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c17_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("button"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c38_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("radio-button"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c10_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("check-box"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c27_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item","check-menu-item"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c40_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("choice-box"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c5_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("combo-box-base","color-picker"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c11_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("combo-box-base","combo-box"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c8_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("context-menu"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c24_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("hyperlink"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c26_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item","custom-menu-item"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c9_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("combo-box-base","date-picker"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c21_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("html-editor"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c20_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("image-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c3_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("label"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c32_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("list-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c43_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("media-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c28_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item","menu"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c18_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-bar"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c49_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-button"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c33_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_empty_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c36_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("pagination"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c50_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("text-input","text-field","password-field"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c13_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("progress-bar"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c47_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("progress-indicator"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c7_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item","radio-menu-item"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c31_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("scroll-bar"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c35_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("scroll-pane"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c29_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("separator"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c23_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("menu-item","custom-menu-item","separator-menu-item"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c37_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("slider"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c2_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("split-menu-button"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c14_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("split-pane"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c12_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("chart","stacked-bar-chart"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c19_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tab"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c6_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tab-pane"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c39_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("table-column"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c46_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("table-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c48_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("text-input","text-area"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c44_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("text-input","text-field"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c25_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("titled-pane"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c41_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("toggle-button"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c16_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tool-bar"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c15_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tooltip"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c30_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tree-table-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c22_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("tree-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata styleClass_c45_PropertyMetadata =
            new StringListPropertyMetadata(
                styleClassName,
                true, /* readWrite */
                Arrays.asList("web-view"), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 1));
    private final ValuePropertyMetadata stylesheetsPropertyMetadata =
            new StringListPropertyMetadata(
                stylesheetsName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "JavaFX CSS", 2));
    private final ValuePropertyMetadata sweepFlagPropertyMetadata =
            new BooleanPropertyMetadata(
                sweepFlagName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 123));
    private final ValuePropertyMetadata tabClosingPolicyPropertyMetadata =
            new EnumerationPropertyMetadata(
                tabClosingPolicyName,
                javafx.scene.control.TabPane.TabClosingPolicy.class,
                true, /* readWrite */
                javafx.scene.control.TabPane.TabClosingPolicy.SELECTED_TAB, /* defaultValue */
                new InspectorPath("Properties", "Specific", 55));
    private final ValuePropertyMetadata tableMenuButtonVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                tableMenuButtonVisibleName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 56));
    private final ValuePropertyMetadata tabMaxHeightPropertyMetadata =
            new DoublePropertyMetadata(
                tabMaxHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                Double.MAX_VALUE, /* defaultValue */
                new InspectorPath("Layout", "Specific", 13));
    private final ValuePropertyMetadata tabMaxWidthPropertyMetadata =
            new DoublePropertyMetadata(
                tabMaxWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                Double.MAX_VALUE, /* defaultValue */
                new InspectorPath("Layout", "Specific", 12));
    private final ValuePropertyMetadata tabMinHeightPropertyMetadata =
            new DoublePropertyMetadata(
                tabMinHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 11));
    private final ValuePropertyMetadata tabMinWidthPropertyMetadata =
            new DoublePropertyMetadata(
                tabMinWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Specific", 10));
    private final ComponentPropertyMetadata tabsPropertyMetadata =
            new ComponentPropertyMetadata(
                tabsName,
                TabMetadata,
                true); /* collection */
    private final ValuePropertyMetadata textPropertyMetadata =
            new StringPropertyMetadata(
                textName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Text", 2));
    private final ValuePropertyMetadata textAlignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                textAlignmentName,
                javafx.scene.text.TextAlignment.class,
                true, /* readWrite */
                javafx.scene.text.TextAlignment.LEFT, /* defaultValue */
                new InspectorPath("Properties", "Text", 7));
    private final ValuePropertyMetadata textFillPropertyMetadata =
            new PaintPropertyMetadata(
                textFillName,
                true, /* readWrite */
                javafx.scene.paint.Color.BLACK, /* defaultValue */
                new InspectorPath("Properties", "Text", 5));
    private final ValuePropertyMetadata textOriginPropertyMetadata =
            new EnumerationPropertyMetadata(
                textOriginName,
                javafx.geometry.VPos.class,
                true, /* readWrite */
                javafx.geometry.VPos.BASELINE, /* defaultValue */
                new InspectorPath("Layout", "Extras", 6));
    private final ValuePropertyMetadata textOverrunPropertyMetadata =
            new EnumerationPropertyMetadata(
                textOverrunName,
                javafx.scene.control.OverrunStyle.class,
                true, /* readWrite */
                javafx.scene.control.OverrunStyle.ELLIPSIS, /* defaultValue */
                new InspectorPath("Properties", "Text", 9));
    private final ValuePropertyMetadata tickLabelFillPropertyMetadata =
            new PaintPropertyMetadata(
                tickLabelFillName,
                true, /* readWrite */
                javafx.scene.paint.Color.BLACK, /* defaultValue */
                new InspectorPath("Properties", "Specific", 98));
    private final ValuePropertyMetadata tickLabelFontPropertyMetadata =
            new FontPropertyMetadata(
                tickLabelFontName,
                true, /* readWrite */
                javafx.scene.text.Font.font("System",8.0), /* defaultValue */
                new InspectorPath("Properties", "Specific", 94));
    private final ValuePropertyMetadata tickLabelFormatterPropertyMetadata =
            new StringConverterPropertyMetadata(
                tickLabelFormatterName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 86));
    private final ValuePropertyMetadata tickLabelGapPropertyMetadata =
            new DoublePropertyMetadata(
                tickLabelGapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                3.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 105));
    private final ValuePropertyMetadata tickLabelRotationPropertyMetadata =
            new DoublePropertyMetadata(
                tickLabelRotationName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.ANGLE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 109));
    private final ValuePropertyMetadata tickLabelsVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                tickLabelsVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 85));
    private final ValuePropertyMetadata tickLengthPropertyMetadata =
            new DoublePropertyMetadata(
                tickLengthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                8.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 114));
    private final ValuePropertyMetadata tickMarksPropertyMetadata =
            new TickMarkListPropertyMetadata(
                tickMarksName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                new InspectorPath("Properties", "Specific", 84));
    private final ValuePropertyMetadata tickMarkVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                tickMarkVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 112));
    private final ValuePropertyMetadata tickUnitPropertyMetadata =
            new DoublePropertyMetadata(
                tickUnitName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                5.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 118));
    private final ValuePropertyMetadata tileAlignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                tileAlignmentName,
                javafx.geometry.Pos.class,
                true, /* readWrite */
                javafx.geometry.Pos.CENTER, /* defaultValue */
                new InspectorPath("Properties", "Specific", 57));
    private final ValuePropertyMetadata tileHeightPropertyMetadata =
            new DoublePropertyMetadata(
                tileHeightName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Specific", 19));
    private final ValuePropertyMetadata tileWidthPropertyMetadata =
            new DoublePropertyMetadata(
                tileWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Specific", 18));
    private final ValuePropertyMetadata titlePropertyMetadata =
            new StringPropertyMetadata(
                titleName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Specific", 35));
    private final ValuePropertyMetadata titleSidePropertyMetadata =
            new EnumerationPropertyMetadata(
                titleSideName,
                javafx.geometry.Side.class,
                true, /* readWrite */
                javafx.geometry.Side.TOP, /* defaultValue */
                new InspectorPath("Properties", "Specific", 58));
    private final ValuePropertyMetadata toggleGroupPropertyMetadata =
            new ToggleGroupPropertyMetadata(
                toggleGroupName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 36));
    private final ComponentPropertyMetadata tooltipPropertyMetadata =
            new ComponentPropertyMetadata(
                tooltipName,
                TooltipMetadata,
                false); /* collection */
    private final ComponentPropertyMetadata topPropertyMetadata =
            new ComponentPropertyMetadata(
                topName,
                NodeMetadata,
                false); /* collection */
    private final ValuePropertyMetadata translateXPropertyMetadata =
            new DoublePropertyMetadata(
                translateXName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 5));
    private final ValuePropertyMetadata translateYPropertyMetadata =
            new DoublePropertyMetadata(
                translateYName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 6));
    private final ValuePropertyMetadata translateZPropertyMetadata =
            new DoublePropertyMetadata(
                translateZName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Transforms", 7));
    private final ComponentPropertyMetadata treeColumnPropertyMetadata =
            new ComponentPropertyMetadata(
                treeColumnName,
                TreeTableColumnMetadata,
                false); /* collection */
    private final ValuePropertyMetadata typePropertyMetadata =
            new EnumerationPropertyMetadata(
                typeName,
                javafx.scene.shape.ArcType.class,
                true, /* readWrite */
                javafx.scene.shape.ArcType.OPEN, /* defaultValue */
                new InspectorPath("Properties", "Specific", 59));
    private final ValuePropertyMetadata underlinePropertyMetadata =
            new BooleanPropertyMetadata(
                underlineName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Text", 12));
    private final ValuePropertyMetadata unitIncrementPropertyMetadata =
            new DoublePropertyMetadata(
                unitIncrementName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 67));
    private final ValuePropertyMetadata upperBoundPropertyMetadata =
            new DoublePropertyMetadata(
                upperBoundName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                100.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 99));
    private final ValuePropertyMetadata userAgentStylesheetPropertyMetadata =
            new StringPropertyMetadata(
                userAgentStylesheetName,
                true, /* readWrite */
                "", /* defaultValue */
                new InspectorPath("Properties", "Specific", 128));
    private final ValuePropertyMetadata valignment_NULL_PropertyMetadata =
            new EnumerationPropertyMetadata(
                valignmentName,
                javafx.geometry.VPos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Specific", 5));
    private final ValuePropertyMetadata valignment_CENTER_PropertyMetadata =
            new EnumerationPropertyMetadata(
                valignmentName,
                javafx.geometry.VPos.class,
                true, /* readWrite */
                javafx.geometry.VPos.CENTER, /* defaultValue */
                new InspectorPath("Layout", "Specific", 5));
    private final ValuePropertyMetadata value_Object_PropertyMetadata =
            new ObjectPropertyMetadata(
                valueName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Properties", "Specific", 68));
    private final ValuePropertyMetadata value_Color_PropertyMetadata =
            new ColorPropertyMetadata(
                valueName,
                true, /* readWrite */
                javafx.scene.paint.Color.WHITE, /* defaultValue */
                new InspectorPath("Properties", "Specific", 68));
    private final ValuePropertyMetadata value_Double_PropertyMetadata =
            new DoublePropertyMetadata(
                valueName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 68));
    private final ValuePropertyMetadata vbarPolicyPropertyMetadata =
            new EnumerationPropertyMetadata(
                vbarPolicyName,
                javafx.scene.control.ScrollPane.ScrollBarPolicy.class,
                true, /* readWrite */
                javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED, /* defaultValue */
                new InspectorPath("Properties", "Specific", 69));
    private final ValuePropertyMetadata verticalFieldOfViewPropertyMetadata =
            new BooleanPropertyMetadata(
                verticalFieldOfViewName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "3D", 5));
    private final ValuePropertyMetadata verticalGridLinesVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                verticalGridLinesVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 113));
    private final ValuePropertyMetadata verticalZeroLineVisiblePropertyMetadata =
            new BooleanPropertyMetadata(
                verticalZeroLineVisibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Specific", 115));
    private final ValuePropertyMetadata vgapPropertyMetadata =
            new DoublePropertyMetadata(
                vgapName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Internal", 1));
    private final ValuePropertyMetadata vgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                vgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Specific", 3));
    private final ValuePropertyMetadata viewportPropertyMetadata =
            new Rectangle2DPropertyMetadata(
                viewportName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Specific", 0));
    private final ValuePropertyMetadata viewportBoundsPropertyMetadata =
            new BoundsPropertyMetadata(
                viewportBoundsName,
                true, /* readWrite */
                new javafx.geometry.BoundingBox(0.0, 0.0, 0.0, 0.0), /* defaultValue */
                new InspectorPath("Layout", "Bounds", 1));
    private final ValuePropertyMetadata visiblePropertyMetadata =
            new BooleanPropertyMetadata(
                visibleName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Properties", "Node", 5));
    private final ValuePropertyMetadata visibleAmountPropertyMetadata =
            new DoublePropertyMetadata(
                visibleAmountName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                15.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 87));
    private final ValuePropertyMetadata visibleRowCountPropertyMetadata =
            new IntegerPropertyMetadata(
                visibleRowCountName,
                true, /* readWrite */
                10, /* defaultValue */
                new InspectorPath("Properties", "Specific", 39));
    private final ValuePropertyMetadata visitedPropertyMetadata =
            new BooleanPropertyMetadata(
                visitedName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Specific", 40));
    private final ValuePropertyMetadata vmaxPropertyMetadata =
            new DoublePropertyMetadata(
                vmaxName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 110));
    private final ValuePropertyMetadata vminPropertyMetadata =
            new DoublePropertyMetadata(
                vminName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 106));
    private final ValuePropertyMetadata vvaluePropertyMetadata =
            new DoublePropertyMetadata(
                vvalueName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 100));
    private final ValuePropertyMetadata width_Double_200_PropertyMetadata =
            new DoublePropertyMetadata(
                widthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                2.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 6));
    private final ValuePropertyMetadata width_Double_0_PropertyMetadata =
            new DoublePropertyMetadata(
                widthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Size", 6));
    private final ValuePropertyMetadata width_Double_ro_PropertyMetadata =
            new DoublePropertyMetadata(
                widthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Layout", "Size", 6));
    private final ValuePropertyMetadata wrappingWidthPropertyMetadata =
            new DoublePropertyMetadata(
                wrappingWidthName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Text", 8));
    private final ValuePropertyMetadata wrapTextPropertyMetadata =
            new BooleanPropertyMetadata(
                wrapTextName,
                true, /* readWrite */
                false, /* defaultValue */
                new InspectorPath("Properties", "Text", 6));
    private final ValuePropertyMetadata x_0_PropertyMetadata =
            new DoublePropertyMetadata(
                xName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 12));
    private final ValuePropertyMetadata x_NaN_PropertyMetadata =
            new DoublePropertyMetadata(
                xName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                Double.NaN, /* defaultValue */
                new InspectorPath("Layout", "Position", 12));
    private final ComponentPropertyMetadata XAxisPropertyMetadata =
            new ComponentPropertyMetadata(
                XAxisName,
                AxisMetadata,
                false); /* collection */
    private final ValuePropertyMetadata XAxisRotationPropertyMetadata =
            new DoublePropertyMetadata(
                XAxisRotationName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.ANGLE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 124));
    private final ValuePropertyMetadata y_0_PropertyMetadata =
            new DoublePropertyMetadata(
                yName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                0.0, /* defaultValue */
                new InspectorPath("Layout", "Position", 13));
    private final ValuePropertyMetadata y_NaN_PropertyMetadata =
            new DoublePropertyMetadata(
                yName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                true, /* readWrite */
                Double.NaN, /* defaultValue */
                new InspectorPath("Layout", "Position", 13));
    private final ComponentPropertyMetadata YAxisPropertyMetadata =
            new ComponentPropertyMetadata(
                YAxisName,
                AxisMetadata,
                false); /* collection */
    private final ValuePropertyMetadata zeroPositionPropertyMetadata =
            new DoublePropertyMetadata(
                zeroPositionName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.COORDINATE,
                false, /* readWrite */
                null, /* No defaultValue for R/O property */
                new InspectorPath("Properties", "Specific", 116));
    private final ValuePropertyMetadata zoomPropertyMetadata =
            new DoublePropertyMetadata(
                zoomName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.SIZE,
                true, /* readWrite */
                1.0, /* defaultValue */
                new InspectorPath("Properties", "Specific", 121));
    private final ValuePropertyMetadata SplitPane_resizableWithParentPropertyMetadata =
            new BooleanPropertyMetadata(
                SplitPane_resizableWithParentName,
                true, /* readWrite */
                true, /* defaultValue */
                new InspectorPath("Layout", "Split Pane Constraints", 0));
    private final ValuePropertyMetadata AnchorPane_bottomAnchorPropertyMetadata =
            new DoublePropertyMetadata(
                AnchorPane_bottomAnchorName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.NULLABLE_COORDINATE,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Anchor Pane Constraints", 0));
    private final ValuePropertyMetadata AnchorPane_leftAnchorPropertyMetadata =
            new DoublePropertyMetadata(
                AnchorPane_leftAnchorName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.NULLABLE_COORDINATE,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Anchor Pane Constraints", 1));
    private final ValuePropertyMetadata AnchorPane_rightAnchorPropertyMetadata =
            new DoublePropertyMetadata(
                AnchorPane_rightAnchorName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.NULLABLE_COORDINATE,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Anchor Pane Constraints", 2));
    private final ValuePropertyMetadata AnchorPane_topAnchorPropertyMetadata =
            new DoublePropertyMetadata(
                AnchorPane_topAnchorName,
                com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind.NULLABLE_COORDINATE,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Anchor Pane Constraints", 3));
    private final ValuePropertyMetadata BorderPane_alignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                BorderPane_alignmentName,
                javafx.geometry.Pos.class,
                "AUTOMATIC", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Border Pane Constraints", 0));
    private final ValuePropertyMetadata BorderPane_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                BorderPane_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Border Pane Constraints", 1));
    private final ValuePropertyMetadata FlowPane_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                FlowPane_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Flow Pane Constraints", 0));
    private final ValuePropertyMetadata GridPane_columnIndexPropertyMetadata =
            new IntegerPropertyMetadata(
                GridPane_columnIndexName,
                true, /* readWrite */
                0, /* defaultValue */
                new InspectorPath("Layout", "Grid Pane Constraints", 1));
    private final ValuePropertyMetadata GridPane_columnSpanPropertyMetadata =
            new IntegerPropertyMetadata(
                GridPane_columnSpanName,
                true, /* readWrite */
                1, /* defaultValue */
                new InspectorPath("Layout", "Grid Pane Constraints", 3));
    private final ValuePropertyMetadata GridPane_halignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                GridPane_halignmentName,
                javafx.geometry.HPos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Grid Pane Constraints", 7));
    private final ValuePropertyMetadata GridPane_hgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                GridPane_hgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Grid Pane Constraints", 4));
    private final ValuePropertyMetadata GridPane_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                GridPane_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Grid Pane Constraints", 8));
    private final ValuePropertyMetadata GridPane_rowIndexPropertyMetadata =
            new IntegerPropertyMetadata(
                GridPane_rowIndexName,
                true, /* readWrite */
                0, /* defaultValue */
                new InspectorPath("Layout", "Grid Pane Constraints", 0));
    private final ValuePropertyMetadata GridPane_rowSpanPropertyMetadata =
            new IntegerPropertyMetadata(
                GridPane_rowSpanName,
                true, /* readWrite */
                1, /* defaultValue */
                new InspectorPath("Layout", "Grid Pane Constraints", 2));
    private final ValuePropertyMetadata GridPane_valignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                GridPane_valignmentName,
                javafx.geometry.VPos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Grid Pane Constraints", 6));
    private final ValuePropertyMetadata GridPane_vgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                GridPane_vgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Grid Pane Constraints", 5));
    private final ValuePropertyMetadata HBox_hgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                HBox_hgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "HBox Constraints", 0));
    private final ValuePropertyMetadata HBox_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                HBox_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "HBox Constraints", 1));
    private final ValuePropertyMetadata StackPane_alignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                StackPane_alignmentName,
                javafx.geometry.Pos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Stack Pane Constraints", 0));
    private final ValuePropertyMetadata StackPane_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                StackPane_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Stack Pane Constraints", 1));
    private final ValuePropertyMetadata TilePane_alignmentPropertyMetadata =
            new EnumerationPropertyMetadata(
                TilePane_alignmentName,
                javafx.geometry.Pos.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "Tile Pane Constraints", 0));
    private final ValuePropertyMetadata TilePane_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                TilePane_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "Tile Pane Constraints", 1));
    private final ValuePropertyMetadata VBox_marginPropertyMetadata =
            new InsetsPropertyMetadata(
                VBox_marginName,
                true, /* readWrite */
                null, /* defaultValue */
                new InspectorPath("Layout", "VBox Constraints", 1));
    private final ValuePropertyMetadata VBox_vgrowPropertyMetadata =
            new EnumerationPropertyMetadata(
                VBox_vgrowName,
                javafx.scene.layout.Priority.class,
                "INHERIT", /* null equivalent */
                true, /* readWrite */
                new InspectorPath("Layout", "VBox Constraints", 0));



    private Metadata() {

        // Populate componentClassMap
        componentClassMap.put(AccordionMetadata.getKlass(), AccordionMetadata);
        componentClassMap.put(AmbientLightMetadata.getKlass(), AmbientLightMetadata);
        componentClassMap.put(AnchorPaneMetadata.getKlass(), AnchorPaneMetadata);
        componentClassMap.put(ArcMetadata.getKlass(), ArcMetadata);
        componentClassMap.put(ArcToMetadata.getKlass(), ArcToMetadata);
        componentClassMap.put(AreaChartMetadata.getKlass(), AreaChartMetadata);
        componentClassMap.put(AxisMetadata.getKlass(), AxisMetadata);
        componentClassMap.put(BarChartMetadata.getKlass(), BarChartMetadata);
        componentClassMap.put(BorderPaneMetadata.getKlass(), BorderPaneMetadata);
        componentClassMap.put(BoxMetadata.getKlass(), BoxMetadata);
        componentClassMap.put(BubbleChartMetadata.getKlass(), BubbleChartMetadata);
        componentClassMap.put(ButtonMetadata.getKlass(), ButtonMetadata);
        componentClassMap.put(ButtonBaseMetadata.getKlass(), ButtonBaseMetadata);
        componentClassMap.put(CameraMetadata.getKlass(), CameraMetadata);
        componentClassMap.put(CanvasMetadata.getKlass(), CanvasMetadata);
        componentClassMap.put(CategoryAxisMetadata.getKlass(), CategoryAxisMetadata);
        componentClassMap.put(ChartMetadata.getKlass(), ChartMetadata);
        componentClassMap.put(CheckBoxMetadata.getKlass(), CheckBoxMetadata);
        componentClassMap.put(CheckMenuItemMetadata.getKlass(), CheckMenuItemMetadata);
        componentClassMap.put(ChoiceBoxMetadata.getKlass(), ChoiceBoxMetadata);
        componentClassMap.put(CircleMetadata.getKlass(), CircleMetadata);
        componentClassMap.put(ClosePathMetadata.getKlass(), ClosePathMetadata);
        componentClassMap.put(ColorPickerMetadata.getKlass(), ColorPickerMetadata);
        componentClassMap.put(ColumnConstraintsMetadata.getKlass(), ColumnConstraintsMetadata);
        componentClassMap.put(ComboBoxMetadata.getKlass(), ComboBoxMetadata);
        componentClassMap.put(ComboBoxBaseMetadata.getKlass(), ComboBoxBaseMetadata);
        componentClassMap.put(ContextMenuMetadata.getKlass(), ContextMenuMetadata);
        componentClassMap.put(ControlMetadata.getKlass(), ControlMetadata);
        componentClassMap.put(CubicCurveMetadata.getKlass(), CubicCurveMetadata);
        componentClassMap.put(CubicCurveToMetadata.getKlass(), CubicCurveToMetadata);
        componentClassMap.put(CustomMenuItemMetadata.getKlass(), CustomMenuItemMetadata);
        componentClassMap.put(CylinderMetadata.getKlass(), CylinderMetadata);
        componentClassMap.put(DatePickerMetadata.getKlass(), DatePickerMetadata);
        componentClassMap.put(EllipseMetadata.getKlass(), EllipseMetadata);
        componentClassMap.put(FlowPaneMetadata.getKlass(), FlowPaneMetadata);
        componentClassMap.put(GridPaneMetadata.getKlass(), GridPaneMetadata);
        componentClassMap.put(GroupMetadata.getKlass(), GroupMetadata);
        componentClassMap.put(HBoxMetadata.getKlass(), HBoxMetadata);
        componentClassMap.put(HLineToMetadata.getKlass(), HLineToMetadata);
        componentClassMap.put(HTMLEditorMetadata.getKlass(), HTMLEditorMetadata);
        componentClassMap.put(HyperlinkMetadata.getKlass(), HyperlinkMetadata);
        componentClassMap.put(ImageViewMetadata.getKlass(), ImageViewMetadata);
        componentClassMap.put(LabelMetadata.getKlass(), LabelMetadata);
        componentClassMap.put(LabeledMetadata.getKlass(), LabeledMetadata);
        componentClassMap.put(LightBaseMetadata.getKlass(), LightBaseMetadata);
        componentClassMap.put(LineMetadata.getKlass(), LineMetadata);
        componentClassMap.put(LineChartMetadata.getKlass(), LineChartMetadata);
        componentClassMap.put(LineToMetadata.getKlass(), LineToMetadata);
        componentClassMap.put(ListViewMetadata.getKlass(), ListViewMetadata);
        componentClassMap.put(MediaViewMetadata.getKlass(), MediaViewMetadata);
        componentClassMap.put(MenuMetadata.getKlass(), MenuMetadata);
        componentClassMap.put(MenuBarMetadata.getKlass(), MenuBarMetadata);
        componentClassMap.put(MenuButtonMetadata.getKlass(), MenuButtonMetadata);
        componentClassMap.put(MenuItemMetadata.getKlass(), MenuItemMetadata);
        componentClassMap.put(MeshViewMetadata.getKlass(), MeshViewMetadata);
        componentClassMap.put(MoveToMetadata.getKlass(), MoveToMetadata);
        componentClassMap.put(NodeMetadata.getKlass(), NodeMetadata);
        componentClassMap.put(NumberAxisMetadata.getKlass(), NumberAxisMetadata);
        componentClassMap.put(PaginationMetadata.getKlass(), PaginationMetadata);
        componentClassMap.put(PaneMetadata.getKlass(), PaneMetadata);
        componentClassMap.put(ParallelCameraMetadata.getKlass(), ParallelCameraMetadata);
        componentClassMap.put(ParentMetadata.getKlass(), ParentMetadata);
        componentClassMap.put(PasswordFieldMetadata.getKlass(), PasswordFieldMetadata);
        componentClassMap.put(PathMetadata.getKlass(), PathMetadata);
        componentClassMap.put(PathElementMetadata.getKlass(), PathElementMetadata);
        componentClassMap.put(PerspectiveCameraMetadata.getKlass(), PerspectiveCameraMetadata);
        componentClassMap.put(PieChartMetadata.getKlass(), PieChartMetadata);
        componentClassMap.put(PointLightMetadata.getKlass(), PointLightMetadata);
        componentClassMap.put(PolygonMetadata.getKlass(), PolygonMetadata);
        componentClassMap.put(PolylineMetadata.getKlass(), PolylineMetadata);
        componentClassMap.put(PopupControlMetadata.getKlass(), PopupControlMetadata);
        componentClassMap.put(PopupWindowMetadata.getKlass(), PopupWindowMetadata);
        componentClassMap.put(ProgressBarMetadata.getKlass(), ProgressBarMetadata);
        componentClassMap.put(ProgressIndicatorMetadata.getKlass(), ProgressIndicatorMetadata);
        componentClassMap.put(QuadCurveMetadata.getKlass(), QuadCurveMetadata);
        componentClassMap.put(QuadCurveToMetadata.getKlass(), QuadCurveToMetadata);
        componentClassMap.put(RadioButtonMetadata.getKlass(), RadioButtonMetadata);
        componentClassMap.put(RadioMenuItemMetadata.getKlass(), RadioMenuItemMetadata);
        componentClassMap.put(RectangleMetadata.getKlass(), RectangleMetadata);
        componentClassMap.put(RegionMetadata.getKlass(), RegionMetadata);
        componentClassMap.put(RowConstraintsMetadata.getKlass(), RowConstraintsMetadata);
        componentClassMap.put(SVGPathMetadata.getKlass(), SVGPathMetadata);
        componentClassMap.put(ScatterChartMetadata.getKlass(), ScatterChartMetadata);
        componentClassMap.put(ScrollBarMetadata.getKlass(), ScrollBarMetadata);
        componentClassMap.put(ScrollPaneMetadata.getKlass(), ScrollPaneMetadata);
        componentClassMap.put(SeparatorMetadata.getKlass(), SeparatorMetadata);
        componentClassMap.put(SeparatorMenuItemMetadata.getKlass(), SeparatorMenuItemMetadata);
        componentClassMap.put(ShapeMetadata.getKlass(), ShapeMetadata);
        componentClassMap.put(Shape3DMetadata.getKlass(), Shape3DMetadata);
        componentClassMap.put(SliderMetadata.getKlass(), SliderMetadata);
        componentClassMap.put(SphereMetadata.getKlass(), SphereMetadata);
        componentClassMap.put(SplitMenuButtonMetadata.getKlass(), SplitMenuButtonMetadata);
        componentClassMap.put(SplitPaneMetadata.getKlass(), SplitPaneMetadata);
        componentClassMap.put(StackPaneMetadata.getKlass(), StackPaneMetadata);
        componentClassMap.put(StackedAreaChartMetadata.getKlass(), StackedAreaChartMetadata);
        componentClassMap.put(StackedBarChartMetadata.getKlass(), StackedBarChartMetadata);
        componentClassMap.put(SubSceneMetadata.getKlass(), SubSceneMetadata);
        componentClassMap.put(SwingNodeMetadata.getKlass(), SwingNodeMetadata);
        componentClassMap.put(TabMetadata.getKlass(), TabMetadata);
        componentClassMap.put(TabPaneMetadata.getKlass(), TabPaneMetadata);
        componentClassMap.put(TableColumnMetadata.getKlass(), TableColumnMetadata);
        componentClassMap.put(TableColumnBaseMetadata.getKlass(), TableColumnBaseMetadata);
        componentClassMap.put(TableViewMetadata.getKlass(), TableViewMetadata);
        componentClassMap.put(TextMetadata.getKlass(), TextMetadata);
        componentClassMap.put(TextAreaMetadata.getKlass(), TextAreaMetadata);
        componentClassMap.put(TextFieldMetadata.getKlass(), TextFieldMetadata);
        componentClassMap.put(TextFlowMetadata.getKlass(), TextFlowMetadata);
        componentClassMap.put(TextInputControlMetadata.getKlass(), TextInputControlMetadata);
        componentClassMap.put(TilePaneMetadata.getKlass(), TilePaneMetadata);
        componentClassMap.put(TitledPaneMetadata.getKlass(), TitledPaneMetadata);
        componentClassMap.put(ToggleButtonMetadata.getKlass(), ToggleButtonMetadata);
        componentClassMap.put(ToolBarMetadata.getKlass(), ToolBarMetadata);
        componentClassMap.put(TooltipMetadata.getKlass(), TooltipMetadata);
        componentClassMap.put(TreeTableColumnMetadata.getKlass(), TreeTableColumnMetadata);
        componentClassMap.put(TreeTableViewMetadata.getKlass(), TreeTableViewMetadata);
        componentClassMap.put(TreeViewMetadata.getKlass(), TreeViewMetadata);
        componentClassMap.put(VBoxMetadata.getKlass(), VBoxMetadata);
        componentClassMap.put(VLineToMetadata.getKlass(), VLineToMetadata);
        componentClassMap.put(ValueAxisMetadata.getKlass(), ValueAxisMetadata);
        componentClassMap.put(WebViewMetadata.getKlass(), WebViewMetadata);
        componentClassMap.put(XYChartMetadata.getKlass(), XYChartMetadata);

        // ComponentMetadata -> PropertyMetadata
        AccordionMetadata.getProperties().add(panesPropertyMetadata);
        AccordionMetadata.getProperties().add(styleClass_c4_PropertyMetadata);

        AmbientLightMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        ArcMetadata.getProperties().add(centerXPropertyMetadata);
        ArcMetadata.getProperties().add(centerYPropertyMetadata);
        ArcMetadata.getProperties().add(length_Double_PropertyMetadata);
        ArcMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        ArcMetadata.getProperties().add(radiusXPropertyMetadata);
        ArcMetadata.getProperties().add(radiusYPropertyMetadata);
        ArcMetadata.getProperties().add(startAnglePropertyMetadata);
        ArcMetadata.getProperties().add(typePropertyMetadata);

        ArcToMetadata.getProperties().add(largeArcFlagPropertyMetadata);
        ArcToMetadata.getProperties().add(radiusXPropertyMetadata);
        ArcToMetadata.getProperties().add(radiusYPropertyMetadata);
        ArcToMetadata.getProperties().add(sweepFlagPropertyMetadata);
        ArcToMetadata.getProperties().add(x_0_PropertyMetadata);
        ArcToMetadata.getProperties().add(XAxisRotationPropertyMetadata);
        ArcToMetadata.getProperties().add(y_0_PropertyMetadata);

        AreaChartMetadata.getProperties().add(createSymbolsPropertyMetadata);
        AreaChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        AxisMetadata.getProperties().add(animatedPropertyMetadata);
        AxisMetadata.getProperties().add(autoRangingPropertyMetadata);
        AxisMetadata.getProperties().add(labelPropertyMetadata);
        AxisMetadata.getProperties().add(side_NULL_PropertyMetadata);
        AxisMetadata.getProperties().add(styleClass_c42_PropertyMetadata);
        AxisMetadata.getProperties().add(tickLabelFillPropertyMetadata);
        AxisMetadata.getProperties().add(tickLabelFontPropertyMetadata);
        AxisMetadata.getProperties().add(tickLabelGapPropertyMetadata);
        AxisMetadata.getProperties().add(tickLabelRotationPropertyMetadata);
        AxisMetadata.getProperties().add(tickLabelsVisiblePropertyMetadata);
        AxisMetadata.getProperties().add(tickLengthPropertyMetadata);
        AxisMetadata.getProperties().add(tickMarksPropertyMetadata);
        AxisMetadata.getProperties().add(tickMarkVisiblePropertyMetadata);
        AxisMetadata.getProperties().add(zeroPositionPropertyMetadata);

        BarChartMetadata.getProperties().add(barGapPropertyMetadata);
        BarChartMetadata.getProperties().add(categoryGapPropertyMetadata);
        BarChartMetadata.getProperties().add(styleClass_c1_PropertyMetadata);

        BorderPaneMetadata.getProperties().add(bottomPropertyMetadata);
        BorderPaneMetadata.getProperties().add(centerPropertyMetadata);
        BorderPaneMetadata.getProperties().add(contentBiasPropertyMetadata);
        BorderPaneMetadata.getProperties().add(leftPropertyMetadata);
        BorderPaneMetadata.getProperties().add(rightPropertyMetadata);
        BorderPaneMetadata.getProperties().add(topPropertyMetadata);

        BoxMetadata.getProperties().add(depthPropertyMetadata);
        BoxMetadata.getProperties().add(height_Double_200_PropertyMetadata);
        BoxMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        BoxMetadata.getProperties().add(width_Double_200_PropertyMetadata);

        BubbleChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        ButtonMetadata.getProperties().add(cancelButtonPropertyMetadata);
        ButtonMetadata.getProperties().add(defaultButtonPropertyMetadata);
        ButtonMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ButtonMetadata.getProperties().add(styleClass_c17_PropertyMetadata);

        ButtonBaseMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ButtonBaseMetadata.getProperties().add(onActionPropertyMetadata);
        ButtonBaseMetadata.getProperties().add(styleClass_c38_PropertyMetadata);

        CameraMetadata.getProperties().add(farClipPropertyMetadata);
        CameraMetadata.getProperties().add(nearClipPropertyMetadata);
        CameraMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        CanvasMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        CanvasMetadata.getProperties().add(nodeOrientation_LEFT_TO_RIGHT_PropertyMetadata);
        CanvasMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        CanvasMetadata.getProperties().add(width_Double_0_PropertyMetadata);

        CategoryAxisMetadata.getProperties().add(categoriesPropertyMetadata);
        CategoryAxisMetadata.getProperties().add(categorySpacingPropertyMetadata);
        CategoryAxisMetadata.getProperties().add(endMarginPropertyMetadata);
        CategoryAxisMetadata.getProperties().add(gapStartAndEndPropertyMetadata);
        CategoryAxisMetadata.getProperties().add(startMarginPropertyMetadata);
        CategoryAxisMetadata.getProperties().add(styleClass_c42_PropertyMetadata);
        CategoryAxisMetadata.getProperties().add(zeroPositionPropertyMetadata);

        ChartMetadata.getProperties().add(animatedPropertyMetadata);
        ChartMetadata.getProperties().add(legendSidePropertyMetadata);
        ChartMetadata.getProperties().add(legendVisiblePropertyMetadata);
        ChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);
        ChartMetadata.getProperties().add(titlePropertyMetadata);
        ChartMetadata.getProperties().add(titleSidePropertyMetadata);

        CheckBoxMetadata.getProperties().add(allowIndeterminatePropertyMetadata);
        CheckBoxMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        CheckBoxMetadata.getProperties().add(indeterminate_Boolean_PropertyMetadata);
        CheckBoxMetadata.getProperties().add(selected_Boolean_PropertyMetadata);
        CheckBoxMetadata.getProperties().add(styleClass_c10_PropertyMetadata);

        CheckMenuItemMetadata.getProperties().add(selected_Boolean_PropertyMetadata);
        CheckMenuItemMetadata.getProperties().add(styleClass_c27_PropertyMetadata);

        ChoiceBoxMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ChoiceBoxMetadata.getProperties().add(styleClass_c40_PropertyMetadata);
        ChoiceBoxMetadata.getProperties().add(value_Object_PropertyMetadata);

        CircleMetadata.getProperties().add(centerXPropertyMetadata);
        CircleMetadata.getProperties().add(centerYPropertyMetadata);
        CircleMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        CircleMetadata.getProperties().add(radius_0_PropertyMetadata);

        ColorPickerMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ColorPickerMetadata.getProperties().add(styleClass_c5_PropertyMetadata);
        ColorPickerMetadata.getProperties().add(value_Color_PropertyMetadata);

        ColumnConstraintsMetadata.getProperties().add(fillWidthPropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(halignment_NULL_PropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(hgrowPropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(maxWidth_COMPUTED_PropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(minWidth_COMPUTED_PropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(percentWidthPropertyMetadata);
        ColumnConstraintsMetadata.getProperties().add(prefWidth_COMPUTED_PropertyMetadata);

        ComboBoxMetadata.getProperties().add(buttonCellPropertyMetadata);
        ComboBoxMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ComboBoxMetadata.getProperties().add(placeholderPropertyMetadata);
        ComboBoxMetadata.getProperties().add(styleClass_c11_PropertyMetadata);
        ComboBoxMetadata.getProperties().add(visibleRowCountPropertyMetadata);

        ComboBoxBaseMetadata.getProperties().add(editable_false_PropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(onActionPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(onHiddenPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(onHidingPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(onShowingPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(onShownPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(promptTextPropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(styleClass_c5_PropertyMetadata);
        ComboBoxBaseMetadata.getProperties().add(value_Object_PropertyMetadata);

        ContextMenuMetadata.getProperties().add(autoHide_true_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(items_MenuItem_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(onActionPropertyMetadata);
        ContextMenuMetadata.getProperties().add(onCloseRequestPropertyMetadata);
        ContextMenuMetadata.getProperties().add(onHiddenPropertyMetadata);
        ContextMenuMetadata.getProperties().add(onHidingPropertyMetadata);
        ContextMenuMetadata.getProperties().add(onShowingPropertyMetadata);
        ContextMenuMetadata.getProperties().add(onShownPropertyMetadata);
        ContextMenuMetadata.getProperties().add(opacityPropertyMetadata);
        ContextMenuMetadata.getProperties().add(styleClass_c8_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(width_Double_0_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(x_NaN_PropertyMetadata);
        ContextMenuMetadata.getProperties().add(y_NaN_PropertyMetadata);

        ControlMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        ControlMetadata.getProperties().add(contextMenuPropertyMetadata);
        ControlMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ControlMetadata.getProperties().add(resizable_Boolean_ro_PropertyMetadata);
        ControlMetadata.getProperties().add(styleClass_c24_PropertyMetadata);
        ControlMetadata.getProperties().add(tooltipPropertyMetadata);

        CubicCurveMetadata.getProperties().add(controlX1PropertyMetadata);
        CubicCurveMetadata.getProperties().add(controlX2PropertyMetadata);
        CubicCurveMetadata.getProperties().add(controlY1PropertyMetadata);
        CubicCurveMetadata.getProperties().add(controlY2PropertyMetadata);
        CubicCurveMetadata.getProperties().add(endXPropertyMetadata);
        CubicCurveMetadata.getProperties().add(endYPropertyMetadata);
        CubicCurveMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        CubicCurveMetadata.getProperties().add(startXPropertyMetadata);
        CubicCurveMetadata.getProperties().add(startYPropertyMetadata);

        CubicCurveToMetadata.getProperties().add(controlX1PropertyMetadata);
        CubicCurveToMetadata.getProperties().add(controlX2PropertyMetadata);
        CubicCurveToMetadata.getProperties().add(controlY1PropertyMetadata);
        CubicCurveToMetadata.getProperties().add(controlY2PropertyMetadata);
        CubicCurveToMetadata.getProperties().add(x_0_PropertyMetadata);
        CubicCurveToMetadata.getProperties().add(y_0_PropertyMetadata);

        CustomMenuItemMetadata.getProperties().add(content_Node_NULL_PropertyMetadata);
        CustomMenuItemMetadata.getProperties().add(hideOnClick_true_PropertyMetadata);
        CustomMenuItemMetadata.getProperties().add(styleClass_c26_PropertyMetadata);

        CylinderMetadata.getProperties().add(divisionsPropertyMetadata);
        CylinderMetadata.getProperties().add(height_Double_200_PropertyMetadata);
        CylinderMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        CylinderMetadata.getProperties().add(radius_100_PropertyMetadata);

        DatePickerMetadata.getProperties().add(editable_true_PropertyMetadata);
        DatePickerMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        DatePickerMetadata.getProperties().add(showWeekNumbersPropertyMetadata);
        DatePickerMetadata.getProperties().add(styleClass_c9_PropertyMetadata);

        EllipseMetadata.getProperties().add(centerXPropertyMetadata);
        EllipseMetadata.getProperties().add(centerYPropertyMetadata);
        EllipseMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        EllipseMetadata.getProperties().add(radiusXPropertyMetadata);
        EllipseMetadata.getProperties().add(radiusYPropertyMetadata);

        FlowPaneMetadata.getProperties().add(alignment_TOP_LEFT_PropertyMetadata);
        FlowPaneMetadata.getProperties().add(columnHalignmentPropertyMetadata);
        FlowPaneMetadata.getProperties().add(contentBiasPropertyMetadata);
        FlowPaneMetadata.getProperties().add(hgapPropertyMetadata);
        FlowPaneMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        FlowPaneMetadata.getProperties().add(prefWrapLengthPropertyMetadata);
        FlowPaneMetadata.getProperties().add(rowValignmentPropertyMetadata);
        FlowPaneMetadata.getProperties().add(vgapPropertyMetadata);

        GridPaneMetadata.getProperties().add(alignment_TOP_LEFT_PropertyMetadata);
        GridPaneMetadata.getProperties().add(columnConstraintsPropertyMetadata);
        GridPaneMetadata.getProperties().add(contentBiasPropertyMetadata);
        GridPaneMetadata.getProperties().add(gridLinesVisiblePropertyMetadata);
        GridPaneMetadata.getProperties().add(hgapPropertyMetadata);
        GridPaneMetadata.getProperties().add(rowConstraintsPropertyMetadata);
        GridPaneMetadata.getProperties().add(vgapPropertyMetadata);

        GroupMetadata.getProperties().add(autoSizeChildrenPropertyMetadata);
        GroupMetadata.getProperties().add(childrenPropertyMetadata);
        GroupMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        HBoxMetadata.getProperties().add(alignment_TOP_LEFT_PropertyMetadata);
        HBoxMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        HBoxMetadata.getProperties().add(contentBiasPropertyMetadata);
        HBoxMetadata.getProperties().add(fillHeightPropertyMetadata);
        HBoxMetadata.getProperties().add(spacingPropertyMetadata);

        HLineToMetadata.getProperties().add(x_0_PropertyMetadata);

        HTMLEditorMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        HTMLEditorMetadata.getProperties().add(htmlTextPropertyMetadata);
        HTMLEditorMetadata.getProperties().add(styleClass_c21_PropertyMetadata);

        HyperlinkMetadata.getProperties().add(cursor_HAND_PropertyMetadata);
        HyperlinkMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        HyperlinkMetadata.getProperties().add(mnemonicParsing_false_PropertyMetadata);
        HyperlinkMetadata.getProperties().add(styleClass_c24_PropertyMetadata);
        HyperlinkMetadata.getProperties().add(visitedPropertyMetadata);

        ImageViewMetadata.getProperties().add(fitHeightPropertyMetadata);
        ImageViewMetadata.getProperties().add(fitWidthPropertyMetadata);
        ImageViewMetadata.getProperties().add(imagePropertyMetadata);
        ImageViewMetadata.getProperties().add(nodeOrientation_LEFT_TO_RIGHT_PropertyMetadata);
        ImageViewMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        ImageViewMetadata.getProperties().add(preserveRatio_false_PropertyMetadata);
        ImageViewMetadata.getProperties().add(smoothPropertyMetadata);
        ImageViewMetadata.getProperties().add(styleClass_c20_PropertyMetadata);
        ImageViewMetadata.getProperties().add(viewportPropertyMetadata);
        ImageViewMetadata.getProperties().add(x_0_PropertyMetadata);
        ImageViewMetadata.getProperties().add(y_0_PropertyMetadata);

        LabelMetadata.getProperties().add(labelForPropertyMetadata);
        LabelMetadata.getProperties().add(mnemonicParsing_false_PropertyMetadata);
        LabelMetadata.getProperties().add(styleClass_c3_PropertyMetadata);

        LabeledMetadata.getProperties().add(alignment_CENTER_LEFT_PropertyMetadata);
        LabeledMetadata.getProperties().add(contentBiasPropertyMetadata);
        LabeledMetadata.getProperties().add(contentDisplayPropertyMetadata);
        LabeledMetadata.getProperties().add(ellipsisStringPropertyMetadata);
        LabeledMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        LabeledMetadata.getProperties().add(fontPropertyMetadata);
        LabeledMetadata.getProperties().add(graphicPropertyMetadata);
        LabeledMetadata.getProperties().add(graphicTextGapPropertyMetadata);
        LabeledMetadata.getProperties().add(labelPaddingPropertyMetadata);
        LabeledMetadata.getProperties().add(lineSpacingPropertyMetadata);
        LabeledMetadata.getProperties().add(mnemonicParsing_true_PropertyMetadata);
        LabeledMetadata.getProperties().add(styleClass_c38_PropertyMetadata);
        LabeledMetadata.getProperties().add(textPropertyMetadata);
        LabeledMetadata.getProperties().add(textAlignmentPropertyMetadata);
        LabeledMetadata.getProperties().add(textFillPropertyMetadata);
        LabeledMetadata.getProperties().add(textOverrunPropertyMetadata);
        LabeledMetadata.getProperties().add(underlinePropertyMetadata);
        LabeledMetadata.getProperties().add(wrapTextPropertyMetadata);

        LightBaseMetadata.getProperties().add(colorPropertyMetadata);
        LightBaseMetadata.getProperties().add(lightOnPropertyMetadata);
        LightBaseMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        LightBaseMetadata.getProperties().add(scopePropertyMetadata);

        LineMetadata.getProperties().add(endXPropertyMetadata);
        LineMetadata.getProperties().add(endYPropertyMetadata);
        LineMetadata.getProperties().add(fill_NULL_PropertyMetadata);
        LineMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        LineMetadata.getProperties().add(startXPropertyMetadata);
        LineMetadata.getProperties().add(startYPropertyMetadata);
        LineMetadata.getProperties().add(stroke_BLACK_PropertyMetadata);

        LineChartMetadata.getProperties().add(createSymbolsPropertyMetadata);
        LineChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        LineToMetadata.getProperties().add(x_0_PropertyMetadata);
        LineToMetadata.getProperties().add(y_0_PropertyMetadata);

        ListViewMetadata.getProperties().add(editable_false_PropertyMetadata);
        ListViewMetadata.getProperties().add(fixedCellSizePropertyMetadata);
        ListViewMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ListViewMetadata.getProperties().add(onEditCancelPropertyMetadata);
        ListViewMetadata.getProperties().add(onEditCommitPropertyMetadata);
        ListViewMetadata.getProperties().add(onEditStartPropertyMetadata);
        ListViewMetadata.getProperties().add(onScrollToPropertyMetadata);
        ListViewMetadata.getProperties().add(orientation_VERTICAL_PropertyMetadata);
        ListViewMetadata.getProperties().add(placeholderPropertyMetadata);
        ListViewMetadata.getProperties().add(styleClass_c32_PropertyMetadata);

        MediaViewMetadata.getProperties().add(fitHeightPropertyMetadata);
        MediaViewMetadata.getProperties().add(fitWidthPropertyMetadata);
        MediaViewMetadata.getProperties().add(nodeOrientation_LEFT_TO_RIGHT_PropertyMetadata);
        MediaViewMetadata.getProperties().add(onErrorPropertyMetadata);
        MediaViewMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        MediaViewMetadata.getProperties().add(preserveRatio_true_PropertyMetadata);
        MediaViewMetadata.getProperties().add(smoothPropertyMetadata);
        MediaViewMetadata.getProperties().add(styleClass_c43_PropertyMetadata);
        MediaViewMetadata.getProperties().add(viewportPropertyMetadata);
        MediaViewMetadata.getProperties().add(x_0_PropertyMetadata);
        MediaViewMetadata.getProperties().add(y_0_PropertyMetadata);

        MenuMetadata.getProperties().add(items_MenuItem_PropertyMetadata);
        MenuMetadata.getProperties().add(onHiddenPropertyMetadata);
        MenuMetadata.getProperties().add(onHidingPropertyMetadata);
        MenuMetadata.getProperties().add(onShowingPropertyMetadata);
        MenuMetadata.getProperties().add(onShownPropertyMetadata);
        MenuMetadata.getProperties().add(styleClass_c28_PropertyMetadata);

        MenuBarMetadata.getProperties().add(menusPropertyMetadata);
        MenuBarMetadata.getProperties().add(styleClass_c18_PropertyMetadata);

        MenuButtonMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        MenuButtonMetadata.getProperties().add(items_MenuItem_PropertyMetadata);
        MenuButtonMetadata.getProperties().add(popupSidePropertyMetadata);
        MenuButtonMetadata.getProperties().add(styleClass_c49_PropertyMetadata);

        MenuItemMetadata.getProperties().add(acceleratorPropertyMetadata);
        MenuItemMetadata.getProperties().add(disablePropertyMetadata);
        MenuItemMetadata.getProperties().add(graphicPropertyMetadata);
        MenuItemMetadata.getProperties().add(idPropertyMetadata);
        MenuItemMetadata.getProperties().add(mnemonicParsing_true_PropertyMetadata);
        MenuItemMetadata.getProperties().add(onActionPropertyMetadata);
        MenuItemMetadata.getProperties().add(onMenuValidationPropertyMetadata);
        MenuItemMetadata.getProperties().add(stylePropertyMetadata);
        MenuItemMetadata.getProperties().add(styleClass_c33_PropertyMetadata);
        MenuItemMetadata.getProperties().add(textPropertyMetadata);
        MenuItemMetadata.getProperties().add(visiblePropertyMetadata);

        MeshViewMetadata.getProperties().add(meshPropertyMetadata);
        MeshViewMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        MoveToMetadata.getProperties().add(x_0_PropertyMetadata);
        MoveToMetadata.getProperties().add(y_0_PropertyMetadata);

        NodeMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        NodeMetadata.getProperties().add(blendModePropertyMetadata);
        NodeMetadata.getProperties().add(boundsInLocalPropertyMetadata);
        NodeMetadata.getProperties().add(boundsInParentPropertyMetadata);
        NodeMetadata.getProperties().add(cachePropertyMetadata);
        NodeMetadata.getProperties().add(cacheHintPropertyMetadata);
        NodeMetadata.getProperties().add(clipPropertyMetadata);
        NodeMetadata.getProperties().add(contentBiasPropertyMetadata);
        NodeMetadata.getProperties().add(cursor_NULL_PropertyMetadata);
        NodeMetadata.getProperties().add(depthTestPropertyMetadata);
        NodeMetadata.getProperties().add(disablePropertyMetadata);
        NodeMetadata.getProperties().add(effectPropertyMetadata);
        NodeMetadata.getProperties().add(effectiveNodeOrientationPropertyMetadata);
        NodeMetadata.getProperties().add(focusTraversable_false_PropertyMetadata);
        NodeMetadata.getProperties().add(idPropertyMetadata);
        NodeMetadata.getProperties().add(layoutBoundsPropertyMetadata);
        NodeMetadata.getProperties().add(layoutXPropertyMetadata);
        NodeMetadata.getProperties().add(layoutYPropertyMetadata);
        NodeMetadata.getProperties().add(mouseTransparentPropertyMetadata);
        NodeMetadata.getProperties().add(nodeOrientation_INHERIT_PropertyMetadata);
        NodeMetadata.getProperties().add(onContextMenuRequestedPropertyMetadata);
        NodeMetadata.getProperties().add(onDragDetectedPropertyMetadata);
        NodeMetadata.getProperties().add(onDragDonePropertyMetadata);
        NodeMetadata.getProperties().add(onDragDroppedPropertyMetadata);
        NodeMetadata.getProperties().add(onDragEnteredPropertyMetadata);
        NodeMetadata.getProperties().add(onDragExitedPropertyMetadata);
        NodeMetadata.getProperties().add(onDragOverPropertyMetadata);
        NodeMetadata.getProperties().add(onInputMethodTextChangedPropertyMetadata);
        NodeMetadata.getProperties().add(onKeyPressedPropertyMetadata);
        NodeMetadata.getProperties().add(onKeyReleasedPropertyMetadata);
        NodeMetadata.getProperties().add(onKeyTypedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseClickedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseDragEnteredPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseDragExitedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseDraggedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseDragOverPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseDragReleasedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseEnteredPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseExitedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseMovedPropertyMetadata);
        NodeMetadata.getProperties().add(onMousePressedPropertyMetadata);
        NodeMetadata.getProperties().add(onMouseReleasedPropertyMetadata);
        NodeMetadata.getProperties().add(onRotatePropertyMetadata);
        NodeMetadata.getProperties().add(onRotationFinishedPropertyMetadata);
        NodeMetadata.getProperties().add(onRotationStartedPropertyMetadata);
        NodeMetadata.getProperties().add(onScrollPropertyMetadata);
        NodeMetadata.getProperties().add(onScrollFinishedPropertyMetadata);
        NodeMetadata.getProperties().add(onScrollStartedPropertyMetadata);
        NodeMetadata.getProperties().add(onSwipeDownPropertyMetadata);
        NodeMetadata.getProperties().add(onSwipeLeftPropertyMetadata);
        NodeMetadata.getProperties().add(onSwipeRightPropertyMetadata);
        NodeMetadata.getProperties().add(onSwipeUpPropertyMetadata);
        NodeMetadata.getProperties().add(onTouchMovedPropertyMetadata);
        NodeMetadata.getProperties().add(onTouchPressedPropertyMetadata);
        NodeMetadata.getProperties().add(onTouchReleasedPropertyMetadata);
        NodeMetadata.getProperties().add(onTouchStationaryPropertyMetadata);
        NodeMetadata.getProperties().add(onZoomPropertyMetadata);
        NodeMetadata.getProperties().add(onZoomFinishedPropertyMetadata);
        NodeMetadata.getProperties().add(onZoomStartedPropertyMetadata);
        NodeMetadata.getProperties().add(opacityPropertyMetadata);
        NodeMetadata.getProperties().add(pickOnBounds_true_PropertyMetadata);
        NodeMetadata.getProperties().add(resizable_Boolean_ro_PropertyMetadata);
        NodeMetadata.getProperties().add(rotatePropertyMetadata);
        NodeMetadata.getProperties().add(rotationAxisPropertyMetadata);
        NodeMetadata.getProperties().add(scaleXPropertyMetadata);
        NodeMetadata.getProperties().add(scaleYPropertyMetadata);
        NodeMetadata.getProperties().add(scaleZPropertyMetadata);
        NodeMetadata.getProperties().add(stylePropertyMetadata);
        NodeMetadata.getProperties().add(styleClass_empty_PropertyMetadata);
        NodeMetadata.getProperties().add(translateXPropertyMetadata);
        NodeMetadata.getProperties().add(translateYPropertyMetadata);
        NodeMetadata.getProperties().add(translateZPropertyMetadata);
        NodeMetadata.getProperties().add(visiblePropertyMetadata);
        NodeMetadata.getProperties().add(SplitPane_resizableWithParentPropertyMetadata);
        NodeMetadata.getProperties().add(AnchorPane_bottomAnchorPropertyMetadata);
        NodeMetadata.getProperties().add(AnchorPane_leftAnchorPropertyMetadata);
        NodeMetadata.getProperties().add(AnchorPane_rightAnchorPropertyMetadata);
        NodeMetadata.getProperties().add(AnchorPane_topAnchorPropertyMetadata);
        NodeMetadata.getProperties().add(BorderPane_alignmentPropertyMetadata);
        NodeMetadata.getProperties().add(BorderPane_marginPropertyMetadata);
        NodeMetadata.getProperties().add(FlowPane_marginPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_columnIndexPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_columnSpanPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_halignmentPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_hgrowPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_marginPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_rowIndexPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_rowSpanPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_valignmentPropertyMetadata);
        NodeMetadata.getProperties().add(GridPane_vgrowPropertyMetadata);
        NodeMetadata.getProperties().add(HBox_hgrowPropertyMetadata);
        NodeMetadata.getProperties().add(HBox_marginPropertyMetadata);
        NodeMetadata.getProperties().add(StackPane_alignmentPropertyMetadata);
        NodeMetadata.getProperties().add(StackPane_marginPropertyMetadata);
        NodeMetadata.getProperties().add(TilePane_alignmentPropertyMetadata);
        NodeMetadata.getProperties().add(TilePane_marginPropertyMetadata);
        NodeMetadata.getProperties().add(VBox_marginPropertyMetadata);
        NodeMetadata.getProperties().add(VBox_vgrowPropertyMetadata);

        NumberAxisMetadata.getProperties().add(forceZeroInRangePropertyMetadata);
        NumberAxisMetadata.getProperties().add(styleClass_c42_PropertyMetadata);
        NumberAxisMetadata.getProperties().add(tickUnitPropertyMetadata);

        PaginationMetadata.getProperties().add(currentPageIndexPropertyMetadata);
        PaginationMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        PaginationMetadata.getProperties().add(maxPageIndicatorCountPropertyMetadata);
        PaginationMetadata.getProperties().add(pageCountPropertyMetadata);
        PaginationMetadata.getProperties().add(styleClass_c36_PropertyMetadata);

        PaneMetadata.getProperties().add(childrenPropertyMetadata);

        ParallelCameraMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        ParentMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        ParentMetadata.getProperties().add(stylesheetsPropertyMetadata);

        PasswordFieldMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        PasswordFieldMetadata.getProperties().add(styleClass_c50_PropertyMetadata);

        PathMetadata.getProperties().add(elementsPropertyMetadata);
        PathMetadata.getProperties().add(fill_NULL_PropertyMetadata);
        PathMetadata.getProperties().add(fillRulePropertyMetadata);
        PathMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        PathMetadata.getProperties().add(stroke_BLACK_PropertyMetadata);

        PathElementMetadata.getProperties().add(absolutePropertyMetadata);

        PerspectiveCameraMetadata.getProperties().add(fieldOfViewPropertyMetadata);
        PerspectiveCameraMetadata.getProperties().add(fixedEyeAtCameraZeroPropertyMetadata);
        PerspectiveCameraMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        PerspectiveCameraMetadata.getProperties().add(verticalFieldOfViewPropertyMetadata);

        PieChartMetadata.getProperties().add(clockwisePropertyMetadata);
        PieChartMetadata.getProperties().add(labelLineLengthPropertyMetadata);
        PieChartMetadata.getProperties().add(labelsVisiblePropertyMetadata);
        PieChartMetadata.getProperties().add(startAnglePropertyMetadata);
        PieChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        PointLightMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        PolygonMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        PolygonMetadata.getProperties().add(pointsPropertyMetadata);

        PolylineMetadata.getProperties().add(fill_NULL_PropertyMetadata);
        PolylineMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        PolylineMetadata.getProperties().add(pointsPropertyMetadata);
        PolylineMetadata.getProperties().add(stroke_BLACK_PropertyMetadata);

        PopupControlMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        PopupControlMetadata.getProperties().add(idPropertyMetadata);
        PopupControlMetadata.getProperties().add(maxHeight_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(maxWidth_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(minHeight_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(minWidth_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(onCloseRequestPropertyMetadata);
        PopupControlMetadata.getProperties().add(onHiddenPropertyMetadata);
        PopupControlMetadata.getProperties().add(onHidingPropertyMetadata);
        PopupControlMetadata.getProperties().add(onShowingPropertyMetadata);
        PopupControlMetadata.getProperties().add(onShownPropertyMetadata);
        PopupControlMetadata.getProperties().add(opacityPropertyMetadata);
        PopupControlMetadata.getProperties().add(prefHeight_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(prefWidth_COMPUTED_PropertyMetadata);
        PopupControlMetadata.getProperties().add(stylePropertyMetadata);
        PopupControlMetadata.getProperties().add(styleClass_empty_PropertyMetadata);
        PopupControlMetadata.getProperties().add(width_Double_0_PropertyMetadata);
        PopupControlMetadata.getProperties().add(x_NaN_PropertyMetadata);
        PopupControlMetadata.getProperties().add(y_NaN_PropertyMetadata);

        PopupWindowMetadata.getProperties().add(anchorLocationPropertyMetadata);
        PopupWindowMetadata.getProperties().add(anchorXPropertyMetadata);
        PopupWindowMetadata.getProperties().add(anchorYPropertyMetadata);
        PopupWindowMetadata.getProperties().add(autoFixPropertyMetadata);
        PopupWindowMetadata.getProperties().add(autoHide_false_PropertyMetadata);
        PopupWindowMetadata.getProperties().add(consumeAutoHidingEventsPropertyMetadata);
        PopupWindowMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        PopupWindowMetadata.getProperties().add(hideOnEscapePropertyMetadata);
        PopupWindowMetadata.getProperties().add(onAutoHidePropertyMetadata);
        PopupWindowMetadata.getProperties().add(onCloseRequestPropertyMetadata);
        PopupWindowMetadata.getProperties().add(onHiddenPropertyMetadata);
        PopupWindowMetadata.getProperties().add(onHidingPropertyMetadata);
        PopupWindowMetadata.getProperties().add(onShowingPropertyMetadata);
        PopupWindowMetadata.getProperties().add(onShownPropertyMetadata);
        PopupWindowMetadata.getProperties().add(opacityPropertyMetadata);
        PopupWindowMetadata.getProperties().add(width_Double_0_PropertyMetadata);
        PopupWindowMetadata.getProperties().add(x_NaN_PropertyMetadata);
        PopupWindowMetadata.getProperties().add(y_NaN_PropertyMetadata);

        ProgressBarMetadata.getProperties().add(styleClass_c13_PropertyMetadata);

        ProgressIndicatorMetadata.getProperties().add(indeterminate_Boolean_ro_PropertyMetadata);
        ProgressIndicatorMetadata.getProperties().add(progressPropertyMetadata);
        ProgressIndicatorMetadata.getProperties().add(styleClass_c47_PropertyMetadata);

        QuadCurveMetadata.getProperties().add(controlXPropertyMetadata);
        QuadCurveMetadata.getProperties().add(controlYPropertyMetadata);
        QuadCurveMetadata.getProperties().add(endXPropertyMetadata);
        QuadCurveMetadata.getProperties().add(endYPropertyMetadata);
        QuadCurveMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        QuadCurveMetadata.getProperties().add(startXPropertyMetadata);
        QuadCurveMetadata.getProperties().add(startYPropertyMetadata);

        QuadCurveToMetadata.getProperties().add(controlXPropertyMetadata);
        QuadCurveToMetadata.getProperties().add(controlYPropertyMetadata);
        QuadCurveToMetadata.getProperties().add(x_0_PropertyMetadata);
        QuadCurveToMetadata.getProperties().add(y_0_PropertyMetadata);

        RadioButtonMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        RadioButtonMetadata.getProperties().add(styleClass_c38_PropertyMetadata);

        RadioMenuItemMetadata.getProperties().add(selected_Boolean_PropertyMetadata);
        RadioMenuItemMetadata.getProperties().add(styleClass_c7_PropertyMetadata);
        RadioMenuItemMetadata.getProperties().add(toggleGroupPropertyMetadata);

        RectangleMetadata.getProperties().add(arcHeightPropertyMetadata);
        RectangleMetadata.getProperties().add(arcWidthPropertyMetadata);
        RectangleMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        RectangleMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        RectangleMetadata.getProperties().add(width_Double_0_PropertyMetadata);
        RectangleMetadata.getProperties().add(x_0_PropertyMetadata);
        RectangleMetadata.getProperties().add(y_0_PropertyMetadata);

        RegionMetadata.getProperties().add(cacheShapePropertyMetadata);
        RegionMetadata.getProperties().add(centerShapePropertyMetadata);
        RegionMetadata.getProperties().add(height_Double_ro_PropertyMetadata);
        RegionMetadata.getProperties().add(insetsPropertyMetadata);
        RegionMetadata.getProperties().add(maxHeight_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(maxWidth_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(minHeight_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(minWidth_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(opaqueInsetsPropertyMetadata);
        RegionMetadata.getProperties().add(paddingPropertyMetadata);
        RegionMetadata.getProperties().add(prefHeight_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(prefWidth_COMPUTED_PropertyMetadata);
        RegionMetadata.getProperties().add(resizable_Boolean_ro_PropertyMetadata);
        RegionMetadata.getProperties().add(scaleShapePropertyMetadata);
        RegionMetadata.getProperties().add(shapePropertyMetadata);
        RegionMetadata.getProperties().add(snapToPixelPropertyMetadata);
        RegionMetadata.getProperties().add(width_Double_ro_PropertyMetadata);

        RowConstraintsMetadata.getProperties().add(fillHeightPropertyMetadata);
        RowConstraintsMetadata.getProperties().add(maxHeight_COMPUTED_PropertyMetadata);
        RowConstraintsMetadata.getProperties().add(minHeight_COMPUTED_PropertyMetadata);
        RowConstraintsMetadata.getProperties().add(percentHeightPropertyMetadata);
        RowConstraintsMetadata.getProperties().add(prefHeight_COMPUTED_PropertyMetadata);
        RowConstraintsMetadata.getProperties().add(valignment_NULL_PropertyMetadata);
        RowConstraintsMetadata.getProperties().add(vgrowPropertyMetadata);

        SVGPathMetadata.getProperties().add(content_String_PropertyMetadata);
        SVGPathMetadata.getProperties().add(fillRulePropertyMetadata);
        SVGPathMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        ScatterChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        ScrollBarMetadata.getProperties().add(blockIncrementPropertyMetadata);
        ScrollBarMetadata.getProperties().add(maxPropertyMetadata);
        ScrollBarMetadata.getProperties().add(minPropertyMetadata);
        ScrollBarMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        ScrollBarMetadata.getProperties().add(styleClass_c31_PropertyMetadata);
        ScrollBarMetadata.getProperties().add(unitIncrementPropertyMetadata);
        ScrollBarMetadata.getProperties().add(value_Double_PropertyMetadata);
        ScrollBarMetadata.getProperties().add(visibleAmountPropertyMetadata);

        ScrollPaneMetadata.getProperties().add(content_Node_NULL_PropertyMetadata);
        ScrollPaneMetadata.getProperties().add(fitToHeightPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(fitToWidthPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(hbarPolicyPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(hmaxPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(hminPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(hvaluePropertyMetadata);
        ScrollPaneMetadata.getProperties().add(pannablePropertyMetadata);
        ScrollPaneMetadata.getProperties().add(prefViewportHeightPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(prefViewportWidthPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(styleClass_c35_PropertyMetadata);
        ScrollPaneMetadata.getProperties().add(vbarPolicyPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(viewportBoundsPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(vmaxPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(vminPropertyMetadata);
        ScrollPaneMetadata.getProperties().add(vvaluePropertyMetadata);

        SeparatorMetadata.getProperties().add(halignment_CENTER_PropertyMetadata);
        SeparatorMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        SeparatorMetadata.getProperties().add(styleClass_c29_PropertyMetadata);
        SeparatorMetadata.getProperties().add(valignment_CENTER_PropertyMetadata);

        SeparatorMenuItemMetadata.getProperties().add(content_Node_SEPARATOR_PropertyMetadata);
        SeparatorMenuItemMetadata.getProperties().add(hideOnClick_false_PropertyMetadata);
        SeparatorMenuItemMetadata.getProperties().add(styleClass_c23_PropertyMetadata);

        ShapeMetadata.getProperties().add(fill_BLACK_PropertyMetadata);
        ShapeMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        ShapeMetadata.getProperties().add(smoothPropertyMetadata);
        ShapeMetadata.getProperties().add(stroke_NULL_PropertyMetadata);
        ShapeMetadata.getProperties().add(strokeDashArrayPropertyMetadata);
        ShapeMetadata.getProperties().add(strokeDashOffsetPropertyMetadata);
        ShapeMetadata.getProperties().add(strokeLineCapPropertyMetadata);
        ShapeMetadata.getProperties().add(strokeLineJoinPropertyMetadata);
        ShapeMetadata.getProperties().add(strokeMiterLimitPropertyMetadata);
        ShapeMetadata.getProperties().add(strokeTypePropertyMetadata);
        ShapeMetadata.getProperties().add(strokeWidthPropertyMetadata);

        Shape3DMetadata.getProperties().add(cullFacePropertyMetadata);
        Shape3DMetadata.getProperties().add(drawModePropertyMetadata);
        Shape3DMetadata.getProperties().add(materialPropertyMetadata);
        Shape3DMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);

        SliderMetadata.getProperties().add(blockIncrementPropertyMetadata);
        SliderMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        SliderMetadata.getProperties().add(labelFormatterPropertyMetadata);
        SliderMetadata.getProperties().add(majorTickUnitPropertyMetadata);
        SliderMetadata.getProperties().add(maxPropertyMetadata);
        SliderMetadata.getProperties().add(minPropertyMetadata);
        SliderMetadata.getProperties().add(minorTickCount_3_PropertyMetadata);
        SliderMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        SliderMetadata.getProperties().add(showTickLabelsPropertyMetadata);
        SliderMetadata.getProperties().add(showTickMarksPropertyMetadata);
        SliderMetadata.getProperties().add(snapToTicksPropertyMetadata);
        SliderMetadata.getProperties().add(styleClass_c37_PropertyMetadata);
        SliderMetadata.getProperties().add(value_Double_PropertyMetadata);

        SphereMetadata.getProperties().add(divisionsPropertyMetadata);
        SphereMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        SphereMetadata.getProperties().add(radius_100_PropertyMetadata);

        SplitMenuButtonMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        SplitMenuButtonMetadata.getProperties().add(styleClass_c2_PropertyMetadata);

        SplitPaneMetadata.getProperties().add(dividerPositionsPropertyMetadata);
        SplitPaneMetadata.getProperties().add(items_Node_PropertyMetadata);
        SplitPaneMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        SplitPaneMetadata.getProperties().add(styleClass_c14_PropertyMetadata);

        StackPaneMetadata.getProperties().add(alignment_CENTER_PropertyMetadata);
        StackPaneMetadata.getProperties().add(contentBiasPropertyMetadata);

        StackedAreaChartMetadata.getProperties().add(createSymbolsPropertyMetadata);
        StackedAreaChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);

        StackedBarChartMetadata.getProperties().add(categoryGapPropertyMetadata);
        StackedBarChartMetadata.getProperties().add(styleClass_c12_PropertyMetadata);

        SubSceneMetadata.getProperties().add(fill_NULL_PropertyMetadata);
        SubSceneMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        SubSceneMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        SubSceneMetadata.getProperties().add(userAgentStylesheetPropertyMetadata);
        SubSceneMetadata.getProperties().add(width_Double_0_PropertyMetadata);

        SwingNodeMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        SwingNodeMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        SwingNodeMetadata.getProperties().add(resizable_Boolean_ro_PropertyMetadata);

        TabMetadata.getProperties().add(closablePropertyMetadata);
        TabMetadata.getProperties().add(content_Node_NULL_PropertyMetadata);
        TabMetadata.getProperties().add(contextMenuPropertyMetadata);
        TabMetadata.getProperties().add(disablePropertyMetadata);
        TabMetadata.getProperties().add(graphicPropertyMetadata);
        TabMetadata.getProperties().add(idPropertyMetadata);
        TabMetadata.getProperties().add(onClosedPropertyMetadata);
        TabMetadata.getProperties().add(onCloseRequestPropertyMetadata);
        TabMetadata.getProperties().add(onSelectionChangedPropertyMetadata);
        TabMetadata.getProperties().add(selected_Boolean_ro_PropertyMetadata);
        TabMetadata.getProperties().add(stylePropertyMetadata);
        TabMetadata.getProperties().add(styleClass_c19_PropertyMetadata);
        TabMetadata.getProperties().add(textPropertyMetadata);
        TabMetadata.getProperties().add(tooltipPropertyMetadata);

        TabPaneMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TabPaneMetadata.getProperties().add(rotateGraphicPropertyMetadata);
        TabPaneMetadata.getProperties().add(side_TOP_PropertyMetadata);
        TabPaneMetadata.getProperties().add(styleClass_c6_PropertyMetadata);
        TabPaneMetadata.getProperties().add(tabClosingPolicyPropertyMetadata);
        TabPaneMetadata.getProperties().add(tabMaxHeightPropertyMetadata);
        TabPaneMetadata.getProperties().add(tabMaxWidthPropertyMetadata);
        TabPaneMetadata.getProperties().add(tabMinHeightPropertyMetadata);
        TabPaneMetadata.getProperties().add(tabMinWidthPropertyMetadata);
        TabPaneMetadata.getProperties().add(tabsPropertyMetadata);

        TableColumnMetadata.getProperties().add(columns_TableColumn_PropertyMetadata);
        TableColumnMetadata.getProperties().add(onEditCancelPropertyMetadata);
        TableColumnMetadata.getProperties().add(onEditCommitPropertyMetadata);
        TableColumnMetadata.getProperties().add(onEditStartPropertyMetadata);
        TableColumnMetadata.getProperties().add(sortType_SortType_PropertyMetadata);

        TableColumnBaseMetadata.getProperties().add(contextMenuPropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(editable_true_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(graphicPropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(idPropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(maxWidth_500000_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(minWidth_1000_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(prefWidth_8000_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(resizable_Boolean_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(sortablePropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(sortNodePropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(stylePropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(styleClass_c39_PropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(textPropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(visiblePropertyMetadata);
        TableColumnBaseMetadata.getProperties().add(width_Double_ro_PropertyMetadata);

        TableViewMetadata.getProperties().add(columnResizePolicy_TABLEVIEW_UNCONSTRAINED_PropertyMetadata);
        TableViewMetadata.getProperties().add(columns_TableColumn_PropertyMetadata);
        TableViewMetadata.getProperties().add(editable_false_PropertyMetadata);
        TableViewMetadata.getProperties().add(fixedCellSizePropertyMetadata);
        TableViewMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TableViewMetadata.getProperties().add(onScrollToPropertyMetadata);
        TableViewMetadata.getProperties().add(onScrollToColumnPropertyMetadata);
        TableViewMetadata.getProperties().add(onSortPropertyMetadata);
        TableViewMetadata.getProperties().add(placeholderPropertyMetadata);
        TableViewMetadata.getProperties().add(sortOrderPropertyMetadata);
        TableViewMetadata.getProperties().add(styleClass_c46_PropertyMetadata);
        TableViewMetadata.getProperties().add(tableMenuButtonVisiblePropertyMetadata);

        TextMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        TextMetadata.getProperties().add(boundsTypePropertyMetadata);
        TextMetadata.getProperties().add(fontPropertyMetadata);
        TextMetadata.getProperties().add(fontSmoothingType_GRAY_PropertyMetadata);
        TextMetadata.getProperties().add(lineSpacingPropertyMetadata);
        TextMetadata.getProperties().add(strikethroughPropertyMetadata);
        TextMetadata.getProperties().add(textPropertyMetadata);
        TextMetadata.getProperties().add(textAlignmentPropertyMetadata);
        TextMetadata.getProperties().add(textOriginPropertyMetadata);
        TextMetadata.getProperties().add(underlinePropertyMetadata);
        TextMetadata.getProperties().add(wrappingWidthPropertyMetadata);
        TextMetadata.getProperties().add(x_0_PropertyMetadata);
        TextMetadata.getProperties().add(y_0_PropertyMetadata);

        TextAreaMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TextAreaMetadata.getProperties().add(prefColumnCount_40_PropertyMetadata);
        TextAreaMetadata.getProperties().add(prefRowCountPropertyMetadata);
        TextAreaMetadata.getProperties().add(scrollLeftPropertyMetadata);
        TextAreaMetadata.getProperties().add(scrollTopPropertyMetadata);
        TextAreaMetadata.getProperties().add(styleClass_c48_PropertyMetadata);
        TextAreaMetadata.getProperties().add(wrapTextPropertyMetadata);

        TextFieldMetadata.getProperties().add(alignment_CENTER_LEFT_PropertyMetadata);
        TextFieldMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TextFieldMetadata.getProperties().add(onActionPropertyMetadata);
        TextFieldMetadata.getProperties().add(prefColumnCount_12_PropertyMetadata);
        TextFieldMetadata.getProperties().add(styleClass_c44_PropertyMetadata);

        TextFlowMetadata.getProperties().add(baselineOffsetPropertyMetadata);
        TextFlowMetadata.getProperties().add(contentBiasPropertyMetadata);
        TextFlowMetadata.getProperties().add(lineSpacingPropertyMetadata);
        TextFlowMetadata.getProperties().add(textAlignmentPropertyMetadata);

        TextInputControlMetadata.getProperties().add(editable_true_PropertyMetadata);
        TextInputControlMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TextInputControlMetadata.getProperties().add(fontPropertyMetadata);
        TextInputControlMetadata.getProperties().add(length_Integer_ro_PropertyMetadata);
        TextInputControlMetadata.getProperties().add(promptTextPropertyMetadata);
        TextInputControlMetadata.getProperties().add(styleClass_c48_PropertyMetadata);
        TextInputControlMetadata.getProperties().add(textPropertyMetadata);

        TilePaneMetadata.getProperties().add(alignment_TOP_LEFT_PropertyMetadata);
        TilePaneMetadata.getProperties().add(contentBiasPropertyMetadata);
        TilePaneMetadata.getProperties().add(hgapPropertyMetadata);
        TilePaneMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        TilePaneMetadata.getProperties().add(prefColumnsPropertyMetadata);
        TilePaneMetadata.getProperties().add(prefRowsPropertyMetadata);
        TilePaneMetadata.getProperties().add(prefTileHeightPropertyMetadata);
        TilePaneMetadata.getProperties().add(prefTileWidthPropertyMetadata);
        TilePaneMetadata.getProperties().add(tileAlignmentPropertyMetadata);
        TilePaneMetadata.getProperties().add(tileHeightPropertyMetadata);
        TilePaneMetadata.getProperties().add(tileWidthPropertyMetadata);
        TilePaneMetadata.getProperties().add(vgapPropertyMetadata);

        TitledPaneMetadata.getProperties().add(animatedPropertyMetadata);
        TitledPaneMetadata.getProperties().add(collapsiblePropertyMetadata);
        TitledPaneMetadata.getProperties().add(content_Node_NULL_PropertyMetadata);
        TitledPaneMetadata.getProperties().add(contentBiasPropertyMetadata);
        TitledPaneMetadata.getProperties().add(expandedPropertyMetadata);
        TitledPaneMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TitledPaneMetadata.getProperties().add(mnemonicParsing_false_PropertyMetadata);
        TitledPaneMetadata.getProperties().add(styleClass_c25_PropertyMetadata);

        ToggleButtonMetadata.getProperties().add(alignment_CENTER_PropertyMetadata);
        ToggleButtonMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        ToggleButtonMetadata.getProperties().add(selected_Boolean_PropertyMetadata);
        ToggleButtonMetadata.getProperties().add(styleClass_c41_PropertyMetadata);
        ToggleButtonMetadata.getProperties().add(toggleGroupPropertyMetadata);

        ToolBarMetadata.getProperties().add(items_Node_PropertyMetadata);
        ToolBarMetadata.getProperties().add(orientation_HORIZONTAL_PropertyMetadata);
        ToolBarMetadata.getProperties().add(styleClass_c16_PropertyMetadata);

        TooltipMetadata.getProperties().add(contentDisplayPropertyMetadata);
        TooltipMetadata.getProperties().add(fontPropertyMetadata);
        TooltipMetadata.getProperties().add(graphicPropertyMetadata);
        TooltipMetadata.getProperties().add(graphicTextGapPropertyMetadata);
        TooltipMetadata.getProperties().add(height_Double_0_PropertyMetadata);
        TooltipMetadata.getProperties().add(onCloseRequestPropertyMetadata);
        TooltipMetadata.getProperties().add(onHiddenPropertyMetadata);
        TooltipMetadata.getProperties().add(onHidingPropertyMetadata);
        TooltipMetadata.getProperties().add(onShowingPropertyMetadata);
        TooltipMetadata.getProperties().add(onShownPropertyMetadata);
        TooltipMetadata.getProperties().add(opacityPropertyMetadata);
        TooltipMetadata.getProperties().add(styleClass_c15_PropertyMetadata);
        TooltipMetadata.getProperties().add(textPropertyMetadata);
        TooltipMetadata.getProperties().add(textAlignmentPropertyMetadata);
        TooltipMetadata.getProperties().add(textOverrunPropertyMetadata);
        TooltipMetadata.getProperties().add(width_Double_0_PropertyMetadata);
        TooltipMetadata.getProperties().add(wrapTextPropertyMetadata);
        TooltipMetadata.getProperties().add(x_NaN_PropertyMetadata);
        TooltipMetadata.getProperties().add(y_NaN_PropertyMetadata);

        TreeTableColumnMetadata.getProperties().add(columns_TreeTableColumn_PropertyMetadata);
        TreeTableColumnMetadata.getProperties().add(onEditCancelPropertyMetadata);
        TreeTableColumnMetadata.getProperties().add(onEditCommitPropertyMetadata);
        TreeTableColumnMetadata.getProperties().add(onEditStartPropertyMetadata);
        TreeTableColumnMetadata.getProperties().add(sortType_SortType_PropertyMetadata);

        TreeTableViewMetadata.getProperties().add(columnResizePolicy_TREETABLEVIEW_UNCONSTRAINED_PropertyMetadata);
        TreeTableViewMetadata.getProperties().add(columns_TreeTableColumn_PropertyMetadata);
        TreeTableViewMetadata.getProperties().add(editable_false_PropertyMetadata);
        TreeTableViewMetadata.getProperties().add(expandedItemCountPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(fixedCellSizePropertyMetadata);
        TreeTableViewMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TreeTableViewMetadata.getProperties().add(onScrollToPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(onScrollToColumnPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(onSortPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(placeholderPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(showRootPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(sortModePropertyMetadata);
        TreeTableViewMetadata.getProperties().add(sortOrderPropertyMetadata);
        TreeTableViewMetadata.getProperties().add(styleClass_c30_PropertyMetadata);
        TreeTableViewMetadata.getProperties().add(tableMenuButtonVisiblePropertyMetadata);
        TreeTableViewMetadata.getProperties().add(treeColumnPropertyMetadata);

        TreeViewMetadata.getProperties().add(editable_false_PropertyMetadata);
        TreeViewMetadata.getProperties().add(expandedItemCountPropertyMetadata);
        TreeViewMetadata.getProperties().add(fixedCellSizePropertyMetadata);
        TreeViewMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        TreeViewMetadata.getProperties().add(onEditCancelPropertyMetadata);
        TreeViewMetadata.getProperties().add(onEditCommitPropertyMetadata);
        TreeViewMetadata.getProperties().add(onEditStartPropertyMetadata);
        TreeViewMetadata.getProperties().add(onScrollToPropertyMetadata);
        TreeViewMetadata.getProperties().add(showRootPropertyMetadata);
        TreeViewMetadata.getProperties().add(styleClass_c22_PropertyMetadata);

        VBoxMetadata.getProperties().add(alignment_TOP_LEFT_PropertyMetadata);
        VBoxMetadata.getProperties().add(contentBiasPropertyMetadata);
        VBoxMetadata.getProperties().add(fillWidthPropertyMetadata);
        VBoxMetadata.getProperties().add(spacingPropertyMetadata);

        VLineToMetadata.getProperties().add(y_0_PropertyMetadata);

        ValueAxisMetadata.getProperties().add(lowerBoundPropertyMetadata);
        ValueAxisMetadata.getProperties().add(minorTickCount_5_PropertyMetadata);
        ValueAxisMetadata.getProperties().add(minorTickLengthPropertyMetadata);
        ValueAxisMetadata.getProperties().add(minorTickVisiblePropertyMetadata);
        ValueAxisMetadata.getProperties().add(scalePropertyMetadata);
        ValueAxisMetadata.getProperties().add(styleClass_c42_PropertyMetadata);
        ValueAxisMetadata.getProperties().add(tickLabelFormatterPropertyMetadata);
        ValueAxisMetadata.getProperties().add(upperBoundPropertyMetadata);
        ValueAxisMetadata.getProperties().add(zeroPositionPropertyMetadata);

        WebViewMetadata.getProperties().add(contextMenuEnabledPropertyMetadata);
        WebViewMetadata.getProperties().add(focusTraversable_true_PropertyMetadata);
        WebViewMetadata.getProperties().add(fontScalePropertyMetadata);
        WebViewMetadata.getProperties().add(fontSmoothingType_LCD_PropertyMetadata);
        WebViewMetadata.getProperties().add(height_Double_ro_PropertyMetadata);
        WebViewMetadata.getProperties().add(maxHeight_MAX_PropertyMetadata);
        WebViewMetadata.getProperties().add(maxWidth_MAX_PropertyMetadata);
        WebViewMetadata.getProperties().add(minHeight_0_PropertyMetadata);
        WebViewMetadata.getProperties().add(minWidth_0_PropertyMetadata);
        WebViewMetadata.getProperties().add(nodeOrientation_LEFT_TO_RIGHT_PropertyMetadata);
        WebViewMetadata.getProperties().add(pickOnBounds_false_PropertyMetadata);
        WebViewMetadata.getProperties().add(prefHeight_60000_PropertyMetadata);
        WebViewMetadata.getProperties().add(prefWidth_80000_PropertyMetadata);
        WebViewMetadata.getProperties().add(resizable_Boolean_ro_PropertyMetadata);
        WebViewMetadata.getProperties().add(styleClass_c45_PropertyMetadata);
        WebViewMetadata.getProperties().add(width_Double_ro_PropertyMetadata);
        WebViewMetadata.getProperties().add(zoomPropertyMetadata);

        XYChartMetadata.getProperties().add(alternativeColumnFillVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(alternativeRowFillVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(horizontalGridLinesVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(horizontalZeroLineVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(styleClass_c34_PropertyMetadata);
        XYChartMetadata.getProperties().add(verticalGridLinesVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(verticalZeroLineVisiblePropertyMetadata);
        XYChartMetadata.getProperties().add(XAxisPropertyMetadata);
        XYChartMetadata.getProperties().add(YAxisPropertyMetadata);


        // Populates hiddenProperties
        hiddenProperties.add(new PropertyName("activated"));
        hiddenProperties.add(new PropertyName("alignWithContentOrigin"));
        hiddenProperties.add(new PropertyName("armed"));
        hiddenProperties.add(new PropertyName("anchor"));
        hiddenProperties.add(new PropertyName("antiAliasing"));
        hiddenProperties.add(new PropertyName("border"));
        hiddenProperties.add(new PropertyName("background"));
        hiddenProperties.add(new PropertyName("caretPosition"));
        hiddenProperties.add(new PropertyName("camera"));
        hiddenProperties.add(new PropertyName("cellFactory"));
        hiddenProperties.add(new PropertyName("cellValueFactory"));
        hiddenProperties.add(new PropertyName("characters"));
        hiddenProperties.add(new PropertyName("childrenUnmodifiable"));
        hiddenProperties.add(new PropertyName("chronology"));
        hiddenProperties.add(new PropertyName("class"));
        hiddenProperties.add(new PropertyName("comparator"));
        hiddenProperties.add(new PropertyName("converter"));
        hiddenProperties.add(new PropertyName("controlCssMetaData"));
        hiddenProperties.add(new PropertyName("cssMetaData"));
        hiddenProperties.add(new PropertyName("customColors"));
        hiddenProperties.add(new PropertyName("data"));
        hiddenProperties.add(new PropertyName("dayCellFactory"));
        hiddenProperties.add(new PropertyName("depthBuffer"));
        hiddenProperties.add(new PropertyName("disabled"));
        hiddenProperties.add(new PropertyName("dividers"));
        hiddenProperties.add(new PropertyName("editingCell"));
        hiddenProperties.add(new PropertyName("editingIndex"));
        hiddenProperties.add(new PropertyName("editingItem"));
        hiddenProperties.add(new PropertyName("editor"));
        hiddenProperties.add(new PropertyName("engine"));
        hiddenProperties.add(new PropertyName("eventDispatcher"));
        hiddenProperties.add(new PropertyName("expandedPane"));
        hiddenProperties.add(new PropertyName("focused"));
        hiddenProperties.add(new PropertyName("focusModel"));
        hiddenProperties.add(new PropertyName("graphicsContext2D"));
        hiddenProperties.add(new PropertyName("hover"));
        hiddenProperties.add(new PropertyName("impl_caretBias"));
        hiddenProperties.add(new PropertyName("impl_caretPosition"));
        hiddenProperties.add(new PropertyName("impl_caretShape"));
        hiddenProperties.add(new PropertyName("impl_selectionEnd"));
        hiddenProperties.add(new PropertyName("impl_selectionShape"));
        hiddenProperties.add(new PropertyName("impl_selectionStart"));
        hiddenProperties.add(new PropertyName("impl_showRelativeToWindow"));
        hiddenProperties.add(new PropertyName("impl_traversalEngine"));
        hiddenProperties.add(new PropertyName("inputMethodRequests"));
        hiddenProperties.add(new PropertyName("localToParentTransform"));
        hiddenProperties.add(new PropertyName("localToSceneTransform"));
        hiddenProperties.add(new PropertyName("managed"));
        hiddenProperties.add(new PropertyName("mediaPlayer"));
        hiddenProperties.add(new PropertyName("needsLayout"));
        hiddenProperties.add(new PropertyName("nodeColumnEnd", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeColumnIndex", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeColumnSpan", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeHgrow", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeMargin", javafx.scene.layout.BorderPane.class));
        hiddenProperties.add(new PropertyName("nodeRowEnd", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeRowIndex", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeRowSpan", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("nodeVgrow", javafx.scene.layout.GridPane.class));
        hiddenProperties.add(new PropertyName("ownerWindow"));
        hiddenProperties.add(new PropertyName("ownerNode"));
        hiddenProperties.add(new PropertyName("pageFactory"));
        hiddenProperties.add(new PropertyName("paragraphs"));
        hiddenProperties.add(new PropertyName("parent"));
        hiddenProperties.add(new PropertyName("parentColumn"));
        hiddenProperties.add(new PropertyName("parentMenu"));
        hiddenProperties.add(new PropertyName("parentPopup"));
        hiddenProperties.add(new PropertyName("pressed"));
        hiddenProperties.add(new PropertyName("properties"));
        hiddenProperties.add(new PropertyName("pseudoClassStates"));
        hiddenProperties.add(new PropertyName("root"));
        hiddenProperties.add(new PropertyName("rowFactory"));
        hiddenProperties.add(new PropertyName("scene"));
        hiddenProperties.add(new PropertyName("selection"));
        hiddenProperties.add(new PropertyName("selectionModel"));
        hiddenProperties.add(new PropertyName("selectedText"));
        hiddenProperties.add(new PropertyName("showing"));
        hiddenProperties.add(new PropertyName("sortPolicy"));
        hiddenProperties.add(new PropertyName("skin"));
        hiddenProperties.add(new PropertyName("styleableParent"));
        hiddenProperties.add(new PropertyName("tableView"));
        hiddenProperties.add(new PropertyName("tabPane"));
        hiddenProperties.add(new PropertyName("transforms"));
        hiddenProperties.add(new PropertyName("treeTableView"));
        hiddenProperties.add(new PropertyName("typeInternal"));
        hiddenProperties.add(new PropertyName("typeSelector"));
        hiddenProperties.add(new PropertyName("userData"));
        hiddenProperties.add(new PropertyName("useSystemMenuBar"));
        hiddenProperties.add(new PropertyName("valueChanging"));
        hiddenProperties.add(new PropertyName("visibleLeafColumns"));

        // Populates parentRelatedProperties
        parentRelatedProperties.add(layoutXName);
        parentRelatedProperties.add(layoutYName);
        parentRelatedProperties.add(translateXName);
        parentRelatedProperties.add(translateYName);
        parentRelatedProperties.add(translateZName);
        parentRelatedProperties.add(scaleXName);
        parentRelatedProperties.add(scaleYName);
        parentRelatedProperties.add(scaleZName);
        parentRelatedProperties.add(rotationAxisName);
        parentRelatedProperties.add(rotateName);

        // Populates sectionNames
        sectionNames.add("Properties");
        sectionNames.add("Layout");
        sectionNames.add("Code");

        // Populates subSectionMap
        final List<String> ss0 = new ArrayList<>();
        ss0.add("Custom");
        ss0.add("Text");
        ss0.add("Specific");
        ss0.add("Graphic");
        ss0.add("3D");
        ss0.add("Pagination");
        ss0.add("Stroke");
        ss0.add("Node");
        ss0.add("JavaFX CSS");
        ss0.add("Extras");
        subSectionMap.put("Properties", ss0);
        final List<String> ss1 = new ArrayList<>();
        ss1.add("Anchor Pane Constraints");
        ss1.add("Border Pane Constraints");
        ss1.add("Flow Pane Constraints");
        ss1.add("Grid Pane Constraints");
        ss1.add("HBox Constraints");
        ss1.add("Split Pane Constraints");
        ss1.add("Stack Pane Constraints");
        ss1.add("Tile Pane Constraints");
        ss1.add("VBox Constraints");
        ss1.add("Internal");
        ss1.add("Specific");
        ss1.add("Size");
        ss1.add("Position");
        ss1.add("Transforms");
        ss1.add("Bounds");
        ss1.add("Extras");
        ss1.add("Specific");
        subSectionMap.put("Layout", ss1);
        final List<String> ss2 = new ArrayList<>();
        ss2.add("Main");
        ss2.add("Edit");
        ss2.add("DragDrop");
        ss2.add("Closing");
        ss2.add("HideShow");
        ss2.add("Keyboard");
        ss2.add("Mouse");
        ss2.add("Rotation");
        ss2.add("Swipe");
        ss2.add("Touch");
        ss2.add("Zoom");
        subSectionMap.put("Code", ss2);
    }


    // The following properties have been rejected:
    //     javafx.embed.swing.SwingNode -> content : Property type (JComponent) is not certified
    //     javafx.scene.control.ChoiceBox -> items : Property items has no section/subsection assigned
    //     javafx.scene.control.ComboBox -> items : Property items has no section/subsection assigned
    //     javafx.scene.control.ListView -> items : Property items has no section/subsection assigned
    //     javafx.scene.control.TableColumnBase -> columns : Property is a collection but type of its items is unknown
    //     javafx.scene.control.TableView -> items : Property items has no section/subsection assigned


    // No uncertified properties have been found

}
