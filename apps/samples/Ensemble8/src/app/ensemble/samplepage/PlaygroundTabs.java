/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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
package ensemble.samplepage;

import ensemble.playground.PlaygroundProperty;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxBuilder;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPaneBuilder;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SliderBuilder;
import javafx.scene.control.TabBuilder;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFieldBuilder;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.GridPaneBuilder;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

/**
 *
 */
class PlaygroundTabs extends TabPane {
    private final SamplePage samplePage;

    PlaygroundTabs(final SamplePage samplePage) {
        this.samplePage = samplePage;
        GridPane grid = GridPaneBuilder.create().hgap(SamplePage.INDENT).vgap(SamplePage.INDENT).padding(new Insets(SamplePage.INDENT)).build();
        int rowIndex = 0;
        for (PlaygroundProperty prop : samplePage.sample.playgroundProperties) {
            try {
                Object object = samplePage.sample.getApp();
                if (prop.fieldName != null) {
                    Field declaredField = samplePage.sample.getClz().getDeclaredField(prop.fieldName);
                    declaredField.setAccessible(true);
                    object = declaredField.get(object);
                }
                Object property = null;
                if (prop.propertyName.startsWith("-")) {
                    Label sectionLabel = new Label(prop.properties.get("name"));
                    Separator separator1 = new Separator(Orientation.HORIZONTAL);
                    HBox.setHgrow(separator1, Priority.ALWAYS);
                    Separator separator2 = new Separator(Orientation.HORIZONTAL);
                    HBox.setHgrow(separator2, Priority.ALWAYS);
                    HBox separator = new HBox(separator1, sectionLabel, separator2);
                    separator.setAlignment(Pos.CENTER);
                    grid.addRow(rowIndex++, separator);
                    GridPane.setColumnSpan(separator, 3);
                    if (rowIndex > 1) {
                        GridPane.setMargin(separator, new Insets(15, 0, 0, 0));
                    }
                    continue;
                }
                if (prop.propertyName.startsWith("get")) {
                    property = object.getClass().getMethod(prop.propertyName).invoke(object);
                } else {
                    for (Field f : object.getClass().getDeclaredFields()) {
                        if (f.getName().equals(prop.propertyName)) {
                            f.setAccessible(true);
                            property = f.get(object);
                            break;
                        }
                    }
                    if (property == null) {
                        property = object.getClass().getMethod(prop.propertyName + "Property").invoke(object);
                    }
                }
                if (object instanceof XYChart && prop.propertyName.equals("data")) {
                    getTabs().add(
                            TabBuilder.create()
                                .text("Data")
                                .content(new XYDataVisualizer((XYChart) object))
                                .build());
                } else if (object instanceof PieChart && prop.propertyName.equals("data")) {
                    getTabs().add(
                            TabBuilder.create()
                                .text("Data")
                                .content(new PieChartDataVisualizer((PieChart) object))
                                .build());
                } else {
                    PropertyController controller = newPropertyController(prop, object, property);
                    if (controller != null) {
                        Region controllerNode = controller.getController();
                        grid.addRow(rowIndex++, controller.getLabel(), controllerNode, controller.getPreview());
                        controllerNode.maxWidthProperty().bind(widthProperty());
                        GridPane.setHgrow(controllerNode, Priority.ALWAYS);
                    } else {
                        System.err.println("Warning! The following property doesn't have corresponding controller: " + prop);
                    }
                }
            } catch (InvocationTargetException ex) {
                Logger.getLogger(SamplePage.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(SamplePage.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(SamplePage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        getStyleClass().add("floating");
        ScrollPane scrollPane = ScrollPaneBuilder.create()
                .content(grid)
                .build();
        scrollPane.getStyleClass().clear();
        getTabs().add(
                TabBuilder.create()
                    .text("Properties")
                    .content(scrollPane)
                    .build());
        setMinSize(100, 100);
    }

    private PropertyController newPropertyController(PlaygroundProperty playgroundProperty, Object object, Object property) {
        if (playgroundProperty.propertyName.equals("getStrokeDashArray")) {
            return new StrokeDashArrayPropertyController(playgroundProperty, object, (ObservableList<Double>) property);
        }
        if (property instanceof DoubleProperty) {
            DoubleProperty prop = (DoubleProperty) property;
            return new DoublePropertyController(playgroundProperty, object, prop);
        } else if (property instanceof IntegerProperty) {
            IntegerProperty prop = (IntegerProperty) property;
            return new IntegerPropertyController(playgroundProperty, object, prop);
        } else if (property instanceof BooleanProperty) {
            BooleanProperty prop = (BooleanProperty) property;
            return new BooleanPropertyController(playgroundProperty, object, prop);
        } else if (property instanceof StringProperty) {
            StringProperty prop = (StringProperty) property;
            return new StringPropertyController(playgroundProperty, object, prop);
        } else if (property instanceof ObjectProperty) {
            final ObjectProperty prop = (ObjectProperty) property;
            if (prop.get() instanceof Color) {
                return new ColorPropertyController(playgroundProperty, object, prop);
            }
            if (prop.get() instanceof String) {
                return new StringPropertyController(playgroundProperty, object, prop);
            }
            if (prop.get() != null && prop.get().getClass().isEnum()) {
                return new EnumPropertyController(playgroundProperty, object, prop, (Enum) prop.get());
            }
        }
        return null;
    }

    private class PropertyController {

        private PlaygroundProperty playgroundProperty;
        private String name;
        private Label label;
        private Region controller;
        private Node preview;

        public PropertyController(PlaygroundProperty playgroundProperty) {
            this(playgroundProperty, playgroundProperty.propertyName);
        }
        
        public PropertyController(PlaygroundProperty playgroundProperty, String name) {
            if (playgroundProperty.properties.containsKey("name")) {
                this.name = playgroundProperty.properties.get("name");
            } else {
                this.name = name;
            }
            this.playgroundProperty = playgroundProperty;
        }

        public Region getLabel() {
            if (label == null) {
                label = LabelBuilder.create().text(name).alignment(Pos.BASELINE_RIGHT).labelFor(getController()).textOverrun(OverrunStyle.ELLIPSIS).maxWidth(200).build();
            }
            return label;
        }
        
        protected void setController(Region controller) {
            this.controller = controller;
        }
        
        protected void setPreview(Node preview) {
            this.preview = preview;
        }
        
        public Region getController() {
            if (controller == null) {
                controller = new Region();
            }
            return controller;
        }

        public Node getPreview() {
            if (preview == null) {
                preview = new Region();
            }
            return preview;
        }

        protected double getProperty(PlaygroundProperty playgroundProperty, String name, double defaultValue) throws NumberFormatException {
            String value = playgroundProperty.properties.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return Double.parseDouble(value);
            }
        }
    }

    private class DoublePropertyController extends PropertyController {
        public DoublePropertyController(PlaygroundProperty playgroundProperty, Object object, Property<Number> prop) {
            super(playgroundProperty);
            Slider slider = SliderBuilder.create().build();
            slider.setMin(getProperty(playgroundProperty, "min", 0));
            slider.setMax(getProperty(playgroundProperty, "max", 100));
            double step = getProperty(playgroundProperty, "step", 0);
            if (step > 0) {
                slider.setMajorTickUnit(step);
                slider.setMinorTickCount(0);
                slider.setSnapToTicks(true);
            }
            slider.valueProperty().bindBidirectional(prop);
            setController(slider);
            
            TextField preview = new TextField();
            preview.setPrefWidth(30);
            preview.textProperty().bindBidirectional(prop, new StringConverter<Number>() {

                @Override
                public String toString(Number number) {
                    return DecimalFormat.getInstance().format((double) number);
                }

                @Override
                public Number fromString(String string) {
                    try {
                        Number number = DecimalFormat.getInstance().parse(string);
                        return number;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            setPreview(preview);
        }
    }

    private class IntegerPropertyController extends PropertyController {
        public IntegerPropertyController(PlaygroundProperty playgroundProperty, Object object, Property<Number> prop) {
            super(playgroundProperty);
            Slider slider = SliderBuilder.create().build();
            slider.setMin(getProperty(playgroundProperty, "min", 0));
            slider.setMax(getProperty(playgroundProperty, "max", 100));
            slider.setSnapToTicks(true);
            slider.setMajorTickUnit(1);
            slider.valueProperty().bindBidirectional(prop);
            setController(slider);
            
            TextField preview = new TextField();
            preview.setPrefWidth(30);
            preview.textProperty().bindBidirectional(prop, new StringConverter<Number>() {

                @Override
                public String toString(Number number) {
                    return DecimalFormat.getInstance().format((int) number);
                }

                @Override
                public Number fromString(String string) {
                    try {
                        Number number = DecimalFormat.getInstance().parse(string);
                        return number;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            setPreview(preview);
        }
    }

    private class BooleanPropertyController extends PropertyController {
        public BooleanPropertyController(PlaygroundProperty playgroundProperty, Object object, Property<Boolean> prop) {
            super(playgroundProperty);
            CheckBox checkbox = CheckBoxBuilder.create().build();
            checkbox.selectedProperty().bindBidirectional(prop);
            setController(checkbox);
        }
    }

    private class StringPropertyController extends PropertyController {
        
        public StringPropertyController(PlaygroundProperty playgroundProperty, Object object, Property<String> prop) {
            super(playgroundProperty);
            TextField textField = TextFieldBuilder.create().build();
            textField.textProperty().bindBidirectional(prop);
            setController(textField);
        }
    }

    private class ColorPropertyController extends PropertyController {

        public ColorPropertyController(PlaygroundProperty playgroundProperty, Object object, final Property<Paint> prop) {
            super(playgroundProperty);
            
            final Rectangle colorRect = new Rectangle(20, 20, (Color) prop.getValue());
            colorRect.setStroke(Color.GRAY);
            final Label valueLabel = new Label(formatWebColor((Color) prop.getValue()));
            valueLabel.setGraphic(colorRect);
            valueLabel.setContentDisplay(ContentDisplay.LEFT);
            setPreview(valueLabel);
            
            final SimpleHSBColorPicker colorPicker = new SimpleHSBColorPicker();
            colorPicker.getColor().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable valueModel) {
                    Color c = colorPicker.getColor().get();
                    prop.setValue(c);
                    valueLabel.setText(formatWebColor(c));
                    colorRect.setFill(c);
                }
            });
            setController(colorPicker);
        }
    
        private String formatWebColor(Color c) {
            String r = Integer.toHexString((int) (c.getRed() * 255));
            if (r.length() == 1) {
                r = "0" + r;
            }
            String g = Integer.toHexString((int) (c.getGreen() * 255));
            if (g.length() == 1) {
                g = "0" + g;
            }
            String b = Integer.toHexString((int) (c.getBlue() * 255));
            if (b.length() == 1) {
                b = "0" + b;
            }
            return "#" + r + g + b;
        }
    }
    
    private class EnumPropertyController extends PropertyController {
        
        public EnumPropertyController(PlaygroundProperty playgroundProperty, Object object, Property prop, final Enum enumeration) {
            super(playgroundProperty);
            
            final ChoiceBox choiceBox = new ChoiceBox();
            choiceBox.setItems(FXCollections.observableArrayList(enumeration.getClass().getEnumConstants()));
            choiceBox.getSelectionModel().select(prop.getValue());
            prop.bind(choiceBox.getSelectionModel().selectedItemProperty());
            setController(choiceBox);
        }
    }

    private class StrokeDashArrayPropertyController extends PropertyController {

        public StrokeDashArrayPropertyController(final PlaygroundProperty playgroundProperty, Object object, final ObservableList<Double> list) {
            super(playgroundProperty, "strokeDashArray");
            
            final ComboBox<ObservableList<Double>> comboBox = new ComboBox<>();
            comboBox.setEditable(true);
            comboBox.setItems(FXCollections.observableArrayList(
                    FXCollections.<Double>observableArrayList(100d, 50d),
                    FXCollections.<Double>observableArrayList(0d, 20d),
                    FXCollections.<Double>observableArrayList(20d, 20d),
                    FXCollections.<Double>observableArrayList(30d, 15d, 0d, 15d)
                    ));
            comboBox.setConverter(new StringConverter<ObservableList<Double>>() {
                @Override public String toString(ObservableList<Double> t) {
                    if (t == null || t.isEmpty()) {
                        return null;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Double d : t) {
                        String str = String.valueOf(d);
                        if (str.endsWith(".0")) {
                            str = str.substring(0, str.length() - 2);
                        }
                        sb.append(str).append(' ');
                    }
                    return sb.substring(0, sb.length() - 1);
                }

                @Override public ObservableList<Double> fromString(String string) {
                    String[] values = string.trim().split(" +");
                    ObservableList<Double> res = FXCollections.observableArrayList();
                    double sum = 0;
                    for (String value : values) {
                        try {
                            double val = Math.min(Math.max(Double.parseDouble(value), 0), 1000);
                            res.add(val);
                            sum += val;
                            if (sum > 5000) {
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    if (sum == 0) {
                        res.clear();
                    }
                    return res;
                }
            });
            comboBox.valueProperty().addListener(new ChangeListener() {
                @Override public void changed(ObservableValue ov, Object t, Object key) {
                    ObservableList<Double> value = comboBox.getValue();
                    list.setAll(value);
                    
                    if (value != null && !value.isEmpty() && comboBox.getItems().indexOf(value) == -1) {
                        comboBox.getItems().add(value);
                    }
                }
            });
            setController(comboBox);
        }
    }
}
