/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.robot.javafx.scene;

import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.AccordionSkin;
import javafx.scene.control.skin.ButtonBarSkin;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.control.skin.CheckBoxSkin;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.control.skin.HyperlinkSkin;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.control.skin.ProgressIndicatorSkin;
import javafx.scene.control.skin.RadioButtonSkin;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.control.skin.SeparatorSkin;
import javafx.scene.control.skin.SpinnerSkin;
import javafx.scene.control.skin.SplitMenuButtonSkin;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.control.skin.ToggleButtonSkin;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.robot.testharness.RobotTestBase;

/**
 * Stress tests the Node initialization from a background thread, per the {@link Node} specification:
 * "Node objects may be constructed and modified on any thread as long they are not yet attached to a Scene in a Window
 * that is showing. An application must attach nodes to such a Scene or modify them on the JavaFX Application Thread."
 *
 * Notable exceptions to this rule:
 * - HTMLEditor
 * - MenuBar
 * - WebView
 *
 * The test creates a visible node on the JavaFX application thread, and at the same time,
 * starts a number of background threads which also create nodes of the same type.
 * Each such thread makes repeated accesses of its own node for the duration
 * of test.
 *
 * Also, the visible node gets accessed periodically in the FX application thread just to shake things up.
 */
public class NodeInitializationStressTest extends RobotTestBase {
    /* debugging aid: set this flag to true and comment out assumeFalse(SKIP_TEST) to run specific test(s). */
    private static final boolean SKIP_TEST = false;
    /** Determines the amount of time background threads are active during each test. */
    private static final int DURATION = 5000;
    private static final AtomicLong seq = new AtomicLong();
    private static final AtomicBoolean failed = new AtomicBoolean();
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(Random::new);

    @Test
    public void accordion() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Accordion c = new Accordion();
            c.setSkin(new AccordionSkin(c));
            c.getPanes().add(new TitledPane("Accordion", new BorderPane()));
            c.getPanes().add(new TitledPane("Accordion", new BorderPane()));
            return c;
        }, (c) -> {
            accessControl(c);
            TitledPane t = (TitledPane)c.getPanes().get(0);
            t.setExpanded(nextBoolean());
        });
    }

    @Test
    public void anchorPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new AnchorPane();
        }, (c) -> {
            accessPane(c, (n) -> {
                AnchorPane.setLeftAnchor(n, nextDouble(100));
            });
        });
    }

    @Test
    public void ambientLight() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new AmbientLight();
        }, (c) -> {
            accessNode(c);
            c.setColor(nextColor());
            c.setLightOn(nextBoolean());
        });
    }

    @Test
    public void arc() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Arc();
        }, (c) -> {
            accessShape(c);
            c.setCenterX(nextDouble(100));
            c.setCenterY(nextDouble(100));
            c.setLength(nextDouble(100));
            c.setRadiusX(nextDouble(100));
            c.setRadiusY(nextDouble(100));
            c.setStartAngle(nextDouble(720));
            c.setType(nextEnum(ArcType.class));
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void areaChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            AreaChart c = new AreaChart(createNumberAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createNumberSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createNumberSeries());
            accessChart(c);
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void barChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            BarChart c = new BarChart(createCategoryAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createCategorySeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createCategorySeries());
            accessChart(c);
        });
    }

    @Test
    public void borderPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new BorderPane();
        }, (c) -> {
            Node n = createNode();
            c.setCenter(n);
            BorderPane.setAlignment(n, nextEnum(Pos.class));
            accessRegion(c);
        });
    }

    @Test
    public void box() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Box();
        }, (c) -> {
            accessShape3D(c);
            c.setDepth(nextDouble(100));
            c.setHeight(nextDouble(100));
            c.setWidth(nextDouble(100));
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void bubbleChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            BubbleChart c = new BubbleChart(createNumberAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createNumberSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createNumberSeries());
            accessChart(c);
        });
    }

    @Test
    public void button() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Button c = new Button();
            c.setSkin(new ButtonSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setAlignment(Pos.CENTER);
            c.setText(nextString());
            c.setDefaultButton(nextBoolean());
        });
    }

    @Test
    public void buttonBar() {
        assumeFalse(SKIP_TEST);
        String[] buttonOrders = {
            ButtonBar.BUTTON_ORDER_LINUX,
            ButtonBar.BUTTON_ORDER_MAC_OS,
            ButtonBar.BUTTON_ORDER_NONE,
            ButtonBar.BUTTON_ORDER_WINDOWS
        };
        test(() -> {
            ButtonBar c = new ButtonBar();
            c.setSkin(new ButtonBarSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setButtonMinWidth(10 + nextDouble(100));
            c.setButtonOrder(nextItem(buttonOrders));
            ButtonData d = nextItem(ButtonData.values());
            Button b = new Button(d.toString());
            ButtonBar.setButtonData(b, d);
            c.getButtons().setAll(b);
        });
    }

    @Test
    public void canvas() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Canvas(200, 200);
        }, (c) -> {
            accessNode(c);
            GraphicsContext g = c.getGraphicsContext2D();
            g.setFill(nextColor());
            g.setStroke(Color.BLACK);
            g.setLineWidth(nextBoolean() ? 0.0 : 1.0);
            g.fillRect(nextDouble(200), nextDouble(200), nextDouble(50), nextDouble(50));
        });
    }

    @Test
    public void checkBox() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            CheckBox c = new CheckBox("checkbox");
            c.setSkin(new CheckBoxSkin(c));
            return c;
        }, (c) -> {
            c.setAllowIndeterminate(nextBoolean());
            c.setSelected(nextBoolean());
            accessControl(c);
        });
    }

    @Test
    public void choiceBox() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ChoiceBox c = new ChoiceBox();
            c.setSkin(new ChoiceBoxSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll("ChoiceBox", "1", "2", "3");
            c.getSelectionModel().select(nextInt(4));
            accessControl(c);
        });
    }

    @Test
    public void circle() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Circle();
        }, (c) -> {
            accessShape(c);
            c.setCenterX(nextDouble(100));
            c.setCenterY(nextDouble(100));
            c.setRadius(nextDouble(100));
        });
    }

    @Test
    public void colorPicker() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ColorPicker c = new ColorPicker();
            c.setSkin(new ColorPickerSkin(c));
            c.setValue(Color.GREEN);
            return c;
        }, (c) -> {
            c.setValue(Color.RED);
            c.prefHeight(-1);
            c.setValue(Color.BLACK);
            c.prefWidth(-1);
            accessControl(c);
            if (Platform.isFxApplicationThread()) {
                if (nextBoolean()) {
                    c.show();
                } else {
                    c.hide();
                }
            }
        });
    }

    @Test
    public void comboBox() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ComboBox c = new ComboBox();
            c.setSkin(new ComboBoxListViewSkin(c));
            c.getItems().setAll("ComboBox", "1", "2");
            return c;
        }, (c) -> {
            c.setEditable(true);
            c.getItems().setAll("ComboBox", nextString(), "2");
            c.getSelectionModel().select(0);
            accessControl(c);
            if (Platform.isFxApplicationThread()) {
                if (nextBoolean()) {
                    c.show();
                } else {
                    c.hide();
                }
            }
        });
    }

    @Test
    public void cubicCurve() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new CubicCurve();
        }, (c) -> {
            accessShape(c);
            c.setControlX1(nextDouble(100));
            c.setControlX2(nextDouble(100));
            c.setControlY1(nextDouble(100));
            c.setControlY2(nextDouble(100));
            c.setEndX(nextDouble(100));
            c.setEndY(nextDouble(100));
            c.setStartX(nextDouble(100));
            c.setStartY(nextDouble(100));
        });
    }

    @Test
    public void cylinder() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Cylinder();
        }, (c) -> {
            accessShape3D(c);
            c.setHeight(nextDouble(100));
            c.setRadius(nextDouble(100));
        });
    }

    @Test
    public void datePicker() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            DatePicker c = new DatePicker();
            c.setSkin(new DatePickerSkin(c));
            return c;
        }, (c) -> {
            c.setValue(LocalDate.now());
            c.prefHeight(-1);
            c.setValue(LocalDate.EPOCH);
            c.prefWidth(-1);
            accessControl(c);
            if (Platform.isFxApplicationThread()) {
                if (nextBoolean()) {
                    c.show();
                } else {
                    c.hide();
                }
            }
        });
    }

    @Test
    public void dialogPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new DialogPane();
        }, (c) -> {
            c.setContent(createNode());
            c.setGraphic(createNode());
            c.setExpandableContent(createNode());
            c.setExpanded(nextBoolean());
            accessNode(c);
        });
    }

    @Test
    public void directionalLight() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new DirectionalLight();
        }, (c) -> {
            c.setColor(nextColor());
            c.setLightOn(nextBoolean());
            Point3D p = new Point3D(nextDouble(100), nextDouble(100), nextDouble(100));
            c.setDirection(p);
        });
    }

    @Test
    public void ellipse() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Ellipse();
        }, (c) -> {
            accessShape(c);
            c.setCenterX(nextDouble(100));
            c.setCenterY(nextDouble(100));
            c.setRadiusX(nextDouble(100));
            c.setRadiusY(nextDouble(100));
        });
    }

    @Test
    public void flowPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new FlowPane();
        }, (c) -> {
            accessPane(c, (n) -> {
                FlowPane.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    @Test
    public void gridPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new GridPane();
        }, (c) -> {
            accessPane(c, (n) -> {
                GridPane.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    @Test
    public void group() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Group();
        }, (c) -> {
            accessNode(c);
            c.getChildren().setAll(createNodes());
        });
    }

    @Test
    public void hbox() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new HBox();
        }, (c) -> {
            accessPane(c, (n) -> {
                HBox.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    @Test
    public void hyperlink() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Hyperlink c = new Hyperlink("Hyperlink");
            c.setSkin(new HyperlinkSkin(c));
            return c;
        }, (c) -> {
            c.setVisited(nextBoolean());
            accessControl(c);
        });
    }

    @Test
    public void imageView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new ImageView();
        }, (c) -> {
            accessNode(c);
            c.setFitHeight(nextDouble(200));
            c.setFitWidth(nextDouble(200));
            c.setImage(nextImage());
            c.setPreserveRatio(nextBoolean());
            c.setSmooth(nextBoolean());
            c.setViewport(new Rectangle2D(nextDouble(100), nextDouble(100), nextDouble(100), nextDouble(100)));
            c.setX(nextDouble(100));
            c.setY(nextDouble(100));
        });
    }

    @Test
    public void label() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Label c = new Label("Label");
            c.setSkin(new LabelSkin(c));
            return c;
        }, (c) -> {
            c.setLabelFor(c);
            accessControl(c);
        });
    }

    @Test
    public void line() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Line();
        }, (c) -> {
            accessShape(c);
            c.setEndX(nextDouble(100));
            c.setEndY(nextDouble(100));
            c.setStartX(nextDouble(100));
            c.setStartY(nextDouble(100));
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void lineChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            LineChart c = new LineChart(createNumberAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createNumberSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createNumberSeries());
            accessChart(c);
        });
    }

    @Test
    public void listView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ListView c = new ListView();
            c.setSkin(new ListViewSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll("ListView", "1", "2");
            c.getSelectionModel().select(0);
            accessControl(c);
        });
    }

    @Test
    public void mediaView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new MediaView();
        }, (c) -> {
            accessNode(c);
            c.setFitHeight(nextDouble(200));
            c.setFitWidth(nextDouble(200));
            c.setPreserveRatio(nextBoolean());
            c.setSmooth(nextBoolean());
            c.setViewport(new Rectangle2D(nextDouble(100), nextDouble(100), nextDouble(100), nextDouble(100)));
            c.setX(nextDouble(100));
            c.setY(nextDouble(100));
        });
    }

    @Test
    public void menuButton() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            MenuButton c = new MenuButton();
            c.setSkin(new MenuButtonSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll(new MenuItem("MenuButton"));
            c.setPopupSide(nextEnum(Side.class));
            accessControl(c);
            if (Platform.isFxApplicationThread()) {
                if (nextBoolean()) {
                    c.show();
                } else {
                    c.hide();
                }
            }
        });
    }

    @Test
    public void meshView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new MeshView();
        }, (c) -> {
            accessShape3D(c);
            c.setMesh(createMesh());
        });
    }

    @Test
    public void pagination() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Pagination c = new Pagination();
            c.setSkin(new PaginationSkin(c));
            return c;
        }, (c) -> {
            c.setPageFactory((pageIndex) -> {
                return new Label(pageIndex + " " + nextString());
            });
            int mx = 1 + nextInt(100);
            accessControl(c);
            c.setPageCount(mx);
            c.setCurrentPageIndex(nextInt(mx));
        });
    }

    @Test
    public void pane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Pane();
        }, (c) -> {
            c.getChildren().setAll(createNodes());
            accessRegion(c);
        });
    }

    @Test
    public void parallelCamera() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new ParallelCamera();
        }, (c) -> {
            accessNode(c);
            double near = nextDouble(100);
            c.setFarClip(near + nextDouble(100));
            c.setNearClip(near);
        });
    }

    @Test
    public void path() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Path();
        }, (c) -> {
            accessShape(c);
            c.getElements().setAll(createPathElements());
            c.getElements().setAll(createPathElements());
            c.setFillRule(nextEnum(FillRule.class));
        });
    }

    @Test
    public void passwordField() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            PasswordField c = new PasswordField();
            c.setSkin(new TextFieldSkin(c));
            return c;
        }, (c) -> {
            accessTextInputControl(c);
            c.setAlignment(Pos.CENTER);
            c.getCharacters();
        });
    }

    @Test
    public void perspectiveCamera() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new PerspectiveCamera();
        }, (c) -> {
            accessNode(c);
            double near = nextDouble(100);
            c.setFarClip(near + nextDouble(100));
            c.setNearClip(near);
            c.setFieldOfView(nextDouble(360));
            c.setVerticalFieldOfView(nextBoolean());
        });
    }

    @Disabled("JDK-8349090") // FIX
    @Test
    public void pieChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            PieChart c = new PieChart();
            c.getData().setAll(createPieSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createPieSeries());
            accessChart(c);
        });
    }

    @Test
    public void pointLight() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new PointLight();
        }, (c) -> {
            accessNode(c);
            c.setColor(nextColor());
            c.setLightOn(nextBoolean());
            c.setConstantAttenuation(nextDouble(10));
            c.setLinearAttenuation(nextDouble(32));
            c.setMaxRange(nextDouble(1000));
            c.setQuadraticAttenuation(nextDouble(1000));
        });
    }

    @Test
    public void polygon() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Polygon();
        }, (c) -> {
            accessShape(c);
            c.getPoints().setAll(createPoints());
            c.getPoints().setAll(createPoints());
        });
    }

    @Test
    public void polyline() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Polyline();
        }, (c) -> {
            accessShape(c);
            c.getPoints().setAll(createPoints());
            c.getPoints().setAll(createPoints());
        });
    }

    @Test
    public void progressIndicator() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ProgressIndicator c = new ProgressIndicator();
            c.setSkin(new ProgressIndicatorSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setProgress(nextBoolean() ? -1 : nextDouble(1));
        });
    }

    @Test
    public void quadCurve() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new QuadCurve();
        }, (c) -> {
            accessShape(c);
            c.setControlX(nextDouble(100));
            c.setControlY(nextDouble(100));
            c.setEndX(nextDouble(100));
            c.setEndY(nextDouble(100));
            c.setStartX(nextDouble(100));
            c.setStartY(nextDouble(100));
        });
    }

    @Test
    public void rectangle() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Rectangle();
        }, (c) -> {
            accessShape(c);
            c.setArcHeight(nextDouble(10));
            c.setArcWidth(nextDouble(10));
            c.setHeight(nextDouble(100));
            c.setWidth(nextDouble(100));
            c.setX(nextDouble(100));
            c.setY(nextDouble(100));
        });
    }

    @Test
    public void radioButton() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            RadioButton c = new RadioButton("RadioButton");
            c.setSkin(new RadioButtonSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setSelected(nextBoolean());
        });
    }

    @Test
    public void region() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Region();
        }, (c) -> {
            accessRegion(c);
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void scatterChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ScatterChart c = new ScatterChart(createNumberAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createNumberSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createNumberSeries());
            accessChart(c);
        });
    }

    @Test
    public void scrollPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ScrollPane c = new ScrollPane(new Label("ScrollPane"));
            c.setSkin(new ScrollPaneSkin(c));
            return c;
        }, (c) -> {
            c.setPannable(nextBoolean());
            accessControl(c);
        });
    }

    @Test
    public void separator() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Separator c = new Separator();
            c.setSkin(new SeparatorSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setOrientation(nextBoolean() ? Orientation.VERTICAL : Orientation.HORIZONTAL);
            c.setHalignment(nextEnum(HPos.class));
            c.setValignment(nextEnum(VPos.class));
        });
    }

    @Test
    public void sphere() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Sphere();
        }, (c) -> {
            accessShape3D(c);
            c.setRadius(nextDouble(100));
        });
    }

    @Test
    public void spinner() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            Spinner c = new Spinner();
            c.setSkin(new SpinnerSkin(c));
            c.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50, 1));
            return c;
        }, (c) -> {
            c.setEditable(nextBoolean());
            accessControl(c);
        });
    }

    @Test
    public void splitMenuButton() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            SplitMenuButton c = new SplitMenuButton();
            c.setSkin(new SplitMenuButtonSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll(new MenuItem("SplitMenuButton"));
            c.setPopupSide(nextEnum(Side.class));
            accessControl(c);
            if (Platform.isFxApplicationThread()) {
                if (nextBoolean()) {
                    c.show();
                } else {
                    c.hide();
                }
            }
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void stackedAreaChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            StackedAreaChart c = new StackedAreaChart(createNumberAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createNumberSeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createNumberSeries());
            accessChart(c);
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void stackedBarChart() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            StackedBarChart c = new StackedBarChart(createCategoryAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createCategorySeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createCategorySeries());
            accessChart(c);
        });
    }

    @Test
    public void stackPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new StackPane();
        }, (c) -> {
            accessPane(c, (n) -> {
                StackPane.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    @Test
    public void svgPath() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new SVGPath();
        }, (c) -> {
            accessShape(c);
            c.setContent(createSvgPath());
            c.setFillRule(nextEnum(FillRule.class));
        });
    }

    @Test
    public void swingNode() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new SwingNode();
        }, (c) -> {
            accessNode(c);
        });
    }

    @Test
    public void tabPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TabPane c = new TabPane();
            c.setSkin(new TabPaneSkin(c));
            c.getTabs().setAll(createTabs());
            return c;
        }, (c) -> {
            c.getTabs().setAll(createTabs());
            accessControl(c);
        });
    }

    @Test
    public void tableView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TableView<String> c = new TableView<>();
            c.setSkin(new TableViewSkin(c));
            c.getItems().setAll(createTableItems());
            TableColumn<String,String> col = new TableColumn<>("Table");
            col.setCellValueFactory((cdf) -> {
                Object v = cdf.getValue();
                return new SimpleObjectProperty(v.toString());
            });
            c.getColumns().add(col);
            return c;
        }, (c) -> {
            c.getItems().setAll(createTableItems());
            accessControl(c);
        });
    }

    @Test
    public void text() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new Text(nextString());
        }, (c) -> {
            c.setText(nextString());
            accessNode(c);
        });
    }

    @Test
    public void textArea() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TextArea c = new TextArea();
            c.setSkin(new TextAreaSkin(c));
            return c;
        }, (c) -> {
            accessTextInputControl(c);
            c.setWrapText(nextBoolean());
        });
    }

    @Test
    public void textField() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TextField c = new TextField();
            c.setSkin(new TextFieldSkin(c));
            return c;
        }, (c) -> {
            accessTextInputControl(c);
            c.setAlignment(Pos.CENTER);
            c.getCharacters();
        });
    }

    @Test
    public void textFlow() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TextFlow c = new TextFlow();
            c.getChildren().setAll(createTextItems());
            return c;
        }, (c) -> {
            c.getChildren().setAll(createTextItems());
            accessRegion(c);
        });
    }

    @Test
    public void tilePane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new TilePane();
        }, (c) -> {
            accessPane(c, (n) -> {
                TilePane.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    @Test
    public void titledPane() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TitledPane c = new TitledPane("TitledPane", null);
            c.setSkin(new TitledPaneSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setAnimated(nextBoolean());
            c.setExpanded(nextBoolean());
            c.setCollapsible(nextBoolean(0.9));
            c.setContent(new Label(nextString()));
        });
    }

    @Test
    public void toggleButton() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ToggleButton c = new ToggleButton("ToggleButton");
            c.setSkin(new ToggleButtonSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setSelected(nextBoolean());
        });
    }

    @Test
    public void toolBar() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            ToolBar c = new ToolBar();
            c.setSkin(new ToolBarSkin(c));
            c.getItems().setAll(createButtons());
            return c;
        }, (c) -> {
            c.getItems().setAll(createButtons());
            accessControl(c);
        });
    }

    @Test
    public void tooltip() {
        assumeFalse(SKIP_TEST);
        AtomicBoolean phase = new AtomicBoolean();
        test(() -> {
            Tooltip t = new Tooltip("tooltip");
            t.setStyle("-fx-background-color:red; -fx-min-width:100px; -fx-min-height:100px; -fx-wrap-text:true; -fx-show-delay:0ms; -fx-hide-delay:0ms;");
            t.setShowDelay(Duration.ZERO);
            t.setHideDelay(Duration.ZERO);
            Label c = new Label("Tooltip");
            c.setSkin(new LabelSkin(c));
            c.setTooltip(t);
            c.setBorder(Border.stroke(Color.BLACK));
            c.setPrefHeight(500);
            c.setPrefWidth(500);
            VBox b = new VBox();
            b.getChildren().add(c);
            b.setId("Tooltip");
            return b;
        }, (c) -> {
            Tooltip t = new Tooltip();
            if (Platform.isFxApplicationThread()) {
                Label label = (Label)c.getChildren().get(0);
                Point2D p;
                if (phase.get()) {
                    p = c.localToScreen(c.getWidth() / 2.0, c.getHeight() / 2.0);
                } else {
                    p = c.localToScreen(c.getWidth() + 2, c.getHeight() + 2);
                }
                robot.mouseMove(p);
                double h = STAGE_HEIGHT;
                if (phase.get()) {
                    h += 10;
                }
                label.setMinHeight(h);
                label.setMaxHeight(h);
                stage.setHeight(h);
                phase.set(!phase.get());
            }
        });
    }

    @Test
    public void treeTableView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TreeTableView<String> c = new TreeTableView<>();
            c.setSkin(new TreeTableViewSkin(c));
            c.setRoot(createRoot());
            TreeTableColumn<String,String> col = new TreeTableColumn<>("TreeTable");
            col.setCellValueFactory((cdf) -> {
                Object v = cdf.getValue();
                return new SimpleObjectProperty(v.toString());
            });
            c.getColumns().add(col);
            return c;
        }, (c) -> {
            c.setRoot(createRoot());
            accessControl(c);
        });
    }

    @Test
    public void treeView() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            TreeView<String> c = new TreeView<>();
            c.setSkin(new TreeViewSkin(c));
            c.setRoot(createRoot());
            return c;
        }, (c) -> {
            c.setRoot(createRoot());
            accessControl(c);
        });
    }

    @Test
    public void vbox() {
        assumeFalse(SKIP_TEST);
        test(() -> {
            return new VBox();
        }, (c) -> {
            accessPane(c, (n) -> {
                VBox.setMargin(n, new Insets(nextDouble(30)));
            });
        });
    }

    private static void accessTextInputControl(TextInputControl c) {
        accessControl(c);
        c.setPromptText("yo");
        c.setText(nextString());
        c.prefHeight(-1);
    }

    private static void accessChart(Chart c) {
        String title = c.getClass().getSimpleName();
        c.setTitle(title);
        c.setAnimated(true);
        accessRegion(c);
    }

    private static void accessControl(Control c) {
        accessRegion(c);
        c.getCssMetaData();
    }

    private static void accessNode(Node c) {
        c.setFocusTraversable(true);
        c.requestFocus();
        c.toFront();
    }

    private static void accessPane(Pane p, Consumer<Node> onChild) {
        accessRegion(p);
        ObservableList<Node> children = p.getChildren();
        children.setAll(createNodes());
        children.setAll(createNodes());
        if (children.size() > 0) {
            int ix = random().nextInt(children.size());
            Node n = children.get(ix);
            onChild.accept(n);
        }
    }

    private static void accessRegion(Region c) {
        accessNode(c);
        c.prefHeight(-1);
        c.prefWidth(-1);
        c.setPrefWidth(nextBoolean(0.1) ? 20 : 100);
        c.setPrefHeight(nextBoolean(0.1) ? 20 : 100);
        c.setBackground(Background.fill(nextColor()));
    }

    private static void accessShape(Shape c) {
        c.setFill(nextColor());
        c.setSmooth(nextBoolean());
        c.setStroke(nextColor());
        c.setStrokeDashOffset(nextDouble(10));
        c.setStrokeLineCap(nextEnum(StrokeLineCap.class));
        c.setStrokeLineJoin(nextEnum(StrokeLineJoin.class));
        c.setStrokeMiterLimit(nextDouble(10));
        c.setStrokeType(nextEnum(StrokeType.class));
        c.setStrokeWidth(nextDouble(10));
    }

    private static void accessShape3D(Shape3D c) {
        c.setCullFace(nextEnum(CullFace.class));
        c.setDrawMode(nextEnum(DrawMode.class));
        PhongMaterial m = new PhongMaterial();
        m.setSelfIlluminationMap(nextImage());
        m.setSpecularColor(nextColor());
        m.setSpecularMap(nextImage());
        // there is technically no upper limit, but 1.0 - 200.0 is a good range
        m.setSpecularPower(1.0 + nextDouble(199));
        c.setMaterial(m);
    }

    private <T extends Node> void test(Supplier<T> generator, Consumer<T> operation) {
        AtomicReference<T> ref = new AtomicReference();
        runAndWait(() -> {
            T n = generator.get();
            ref.set(n);
        });
        T visibleNode = ref.get();
        String title = visibleNode.getId();
        if (title == null) {
            title = visibleNode.getClass().getSimpleName();
        }

        setTitle(title);
        setContent(visibleNode);

        int threadCount = 1 + Runtime.getRuntime().availableProcessors() * 2;
        AtomicBoolean running = new AtomicBoolean(true);
        int additionalThreads = 2; // jiggler + tight loop
        CountDownLatch counter = new CountDownLatch(threadCount + additionalThreads);

        try {
            // construct nodes in a tight loop
            new Thread(() -> {
                try {
                    while (running.get()) {
                        T n = generator.get();
                    }
                } finally {
                    counter.countDown();
                }
            }, "tight loop " + title).start();

            // periodically "jiggle" the visible node in the fx thread
            new Thread(() -> {
                try {
                    while (running.get()) {
                        sleep(1 + random().nextInt(20));
                        runAndWait(() -> {
                            operation.accept(visibleNode);
                        });
                    }
                } finally {
                    counter.countDown();
                }
            }, "jiggler " + title).start();

            // stress test from multiple background threads
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        T n = generator.get();
                        while (running.get()) {
                            operation.accept(n);
                        }
                    } finally {
                        counter.countDown();
                    }
                }, title).start();
            }

            sleep(DURATION);
        } finally {
            running.set(false);
        }

        // let all threads finish
        try {
            counter.await(500, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    private static boolean nextBoolean() {
        return random().nextBoolean();
    }

    /**
     * Randomly returns true with the specified probability.
     * @param probability the probability value in the range (0.0 ... 1.0)
     * @return the boolean value
     */
    private static boolean nextBoolean(double probability) {
        return random().nextDouble() < probability;
    }

    private static Color nextColor() {
        Random r = random();
        double hue = 360.0 * r.nextDouble();
        double saturation = 0.5 + 0.5 * r.nextDouble();
        double brightness = r.nextDouble();
        double opacity = r.nextDouble();
        return Color.hsb(hue, saturation, brightness, opacity);
    }

    private static double nextDouble(int min, int max) {
        return min + random().nextDouble() * (max - min);
    }

    private static double nextDouble(int max) {
        return max * random().nextDouble();
    }

    private static <T extends Enum> T nextEnum(Class<T> type) {
        T[] values = type.getEnumConstants();
        return nextItem(values);
    }

    private static Image nextImage() {
        // cannot generate WriteableImage because it's considered "animated" and will
        // throw an exception in ImageView:254
        // Toolkit.getImageAccessor().getImageProperty(_image).addListener(platformImageChangeListener.getWeakListener());
        switch(nextInt(2)) {
        case 0:
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAATklEQVR4XsXIIQEAMAwEsfdvuhNwPAMh2Xb3V0JLaAktoSW0hJbQElpCS2gJLaEltISW0BJaQktoCS2hJbSEltASWkJLaAktoSW0hJawHluV+GpNRXH/AAAAAElFTkSuQmCC");
        default:
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAWUlEQVR4XsXIoQEAAAyDMP5/uvMcwERM2NgnHDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HDUcNRw1HLUD9Br0ptaWcFoAAAAASUVORK5CYII=");
        }
    }

    private static int nextInt(int max) {
        return random().nextInt(max);
    }

    private static <T> T nextItem(T[] items) {
        int ix = nextInt(items.length);
        return items[ix];
    }

    private static String nextString() {
        long ix = seq.incrementAndGet();
        return "_a" + ix + "\nyo!";
    }

    private static Random random() {
        return random.get();
    }

    private static List<Node> createButtons() {
        int sz = random().nextInt(5);
        ArrayList<Node> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(new Button(nextString()));
        }
        return a;
    }

    private static CategoryAxis createCategoryAxis(String text) {
        CategoryAxis a = new CategoryAxis();
        a.setLabel(text);
        return a;
    }

    private static Mesh createMesh() {
        // see Mouse3DTest
        switch (nextInt(4)) {
        case 0:
            // meshesXY2
            return createMesh(
                new float[] { 0f, 0f, 0f, 100f, 0f, 0f, 100f, 100f, 0f, 0f, 0f, -7f, 100f, 0f, -7f, 100f, 100f, -7f },
                new float[] { 0f, 0f, 1f, 0f, 1f, 1f },
                new int[] { 3, 0, 5, 2, 4, 1, 0, 0, 2, 2, 1, 1 });
        case 1:
            // meshesXYFacingEachOther
            return createMesh(
                new float[] { 0f, 0f, 0f, 100f, 0f, 0f, 100f, 100f, 0f, 0f, 0f, -7f, 100f, 0f, -7f, 100f, 100f, -7f },
                new float[] { 0f, 0f, 1f, 0f, 1f, 1f },
                new int[] { 0, 0, 2, 2, 1, 1, 3, 0, 4, 1, 5, 2, });
        case 2:
            // meshXYBack
            return createMesh(
                new float[] { 0f, 0f, 7f, 100f, 0f, 7f, 100f, 100f, 7f },
                new float[] { 0f, 0f, 1f, 0f, 1f, 1f },
                new int[] { 0, 0, 1, 1, 2, 2 });
        default:
            // meshXYFlippedTexture
            return createMesh(
                new float[] { 0f, 0f, 7f, 100f, 0f, 7f, 100f, 100f, 7f },
                new float[] { 0f, 0f, 0f, 1f, 1f, 1f },
                new int[] { 0, 0, 2, 1, 1, 2 });
        }
    }

    private static Mesh createMesh(float[] points, float[] tex, int[] faces) {
        TriangleMesh m = new TriangleMesh();
        m.getPoints().setAll(points);
        m.getTexCoords().setAll(tex);
        m.getFaces().setAll(faces);
        return m;
    }

    private static Node createNode() {
        switch (random().nextInt(3)) {
        case 0:
            return new Text("Text");
        case 1:
            return new Button("Button");
        default:
            return new Label("Label");
        }
    }

    private static List<Node> createNodes() {
        int sz = random().nextInt(5);
        ArrayList<Node> nodes = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            nodes.add(createNode());
        }
        return nodes;
    }

    private static NumberAxis createNumberAxis(String text) {
        NumberAxis a = new NumberAxis();
        a.setLabel(text);
        return a;
    }

    private static Series<String, Number> createCategorySeries() {
        String name = "S" + seq.incrementAndGet();
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 7; i++) {
            double v = nextDouble(-20, 20);
            String cat = String.valueOf(i);
            s.getData().add(new XYChart.Data(cat, v));
        }
        return s;
    }

    private static Series<Number, Number> createNumberSeries() {
        String name = "S" + seq.incrementAndGet();
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 7; i++) {
            double v = nextDouble(-20, 20);
            s.getData().add(new XYChart.Data(i, v));
        }
        return s;
    }

    private static PathElement createPathElement() {
        switch (nextInt(7)) {
        case 0:
            {
                double radiusX = nextDouble(100);
                double radiusY = nextDouble(100);
                double xAxisRotation = nextDouble(1000);
                double x = nextDouble(100);
                double y = nextDouble(100);
                boolean largeArcFlag = nextBoolean();
                boolean sweepFlag = nextBoolean();
                return new ArcTo(radiusX, radiusY, xAxisRotation, x, y, largeArcFlag, sweepFlag);
            }
        case 1:
            {
                double controlX1 = nextDouble(100);
                double controlY1 = nextDouble(100);
                double controlX2 = nextDouble(100);
                double controlY2 = nextDouble(100);
                double x = nextDouble(100);
                double y = nextDouble(100);
                return new CubicCurveTo(controlX1, controlY1, controlX2, controlY2, x, y);
            }
        case 2:
            return new HLineTo(nextDouble(100));
        case 3:
            return new LineTo(nextDouble(100), nextDouble(100));
        case 4:
            return new MoveTo(nextDouble(100), nextDouble(100));
        case 5:
            {
                double controlX = nextDouble(100);
                double controlY = nextDouble(100);
                double x = nextDouble(100);
                double y = nextDouble(100);
                return new QuadCurveTo(controlX, controlY, x, y);
            }
        default:
            return new VLineTo(nextDouble(100));
        }
    }

    private static List<PathElement> createPathElements() {
        int sz = random().nextInt(5);
        ArrayList<PathElement> a = new ArrayList<>(sz);
        a.add(new MoveTo(nextDouble(100), nextDouble(100)));
        for (int i = 0; i < sz; i++) {
            a.add(createPathElement());
        }
        if (nextBoolean()) {
            a.add(new ClosePath());
        }
        return a;
    }

    private static List<PieChart.Data> createPieSeries() {
        int sz = 1 + random().nextInt(20);
        ArrayList<Data> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(new PieChart.Data("N" + i, random().nextDouble()));
        }
        return a;
    }

    private static List<Double> createPoints() {
        int sz = random().nextInt(8);
        ArrayList<Double> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(nextDouble(200));
            a.add(nextDouble(200));
        }
        return a;
    }

    private static TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>(null);
        int sz = random().nextInt(20);
        for (int i = 0; i < sz; i++) {
            root.getChildren().add(new TreeItem<>(nextString()));
        }
        root.setExpanded(nextBoolean());
        return root;
    }

    private static String createSvgPath() {
        switch (nextInt(4)) {
        case 0:
            // Arc
            return "M 5 310 L 100 210 A 25 45 0 0 1 162 164 L 175 155 A 35 45 -40 0 1 210 110 L 310 10";
        case 1:
            // Bézier
            return "M 10 70 C 30 20, 75 15, 90 85 S 140 145, 185 75 T 180 80";
        case 2:
            // quadratic
            return "M 10 80 Q 52.5 10, 95 80 T 180 80";
        default:
            // vertical/horizontal
            return "M 0 0 H 100 V 100 H 0 L 0 0";
        }
    }

    private static List<Tab> createTabs() {
        ArrayList<Tab> a = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            a.add(new Tab(nextString()));
        }
        return a;
    }

    private static List<String> createTableItems() {
        int sz = random().nextInt(20);
        ArrayList<String> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(nextString());
        }
        return a;
    }

    private static List<Text> createTextItems() {
        int sz = random().nextInt(20);
        ArrayList<Text> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(new Text(nextString()));
        }
        return a;
    }

    @BeforeAll
    public static void beforeAll() {
        // this might be made a part of the base class (RobotTestBase)
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            failed.set(true);
            // we could also accumulate stack trace(s) and send them to fail() in afterEach()
        });
    }

    @BeforeEach
    public void beforeEach() {
        failed.set(false);
    }

    @AfterEach
    public void afterEach() {
        if (failed.get()) {
            fail();
        }
    }
}
