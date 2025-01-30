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

import static org.junit.jupiter.api.Assertions.fail;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.control.skin.RadioButtonSkin;
import javafx.scene.control.skin.ScrollPaneSkin;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
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
 * Tests Node initialization from a background thread, per
 *
 * https://openjfx.io/javadoc/23/javafx.graphics/javafx/scene/Node.html
 *
 * "Node objects may be constructed and modified on any thread as long they are not yet attached to a Scene in a Window
 * that is showing. An application must attach nodes to such a Scene or modify them on the JavaFX Application Thread."
 *
 * Notable exceptions to this rule:
 * - HTMLEditor
 * - MenuBar
 * - WebView
 *
 * The test creates a visible node of a given type, and at the same time, starts a number of background threads
 * which also create nodes of the same type.  Each such thread makes repeated accesses of its own node for the duration
 * of test.  Also, the visible node gets accessed periodically just to shake things up.
 *
 * NOTE: I suspect this test might be a bit unstable and/or platform-dependent, due to its multi-threaded nature.
 *
 * TODO add remaining Nodes to the test.
 */
public class NodeInitializationBackgroundThreadTest extends RobotTestBase {
    private static final int DURATION = 5000;
    private static final AtomicLong seq = new AtomicLong();
    private static final AtomicBoolean failed = new AtomicBoolean();

    @Test
    public void accordion() {
        test(() -> {
            Accordion c = new Accordion();
            c.setSkin(new AccordionSkin(c));
            c.getPanes().add(new TitledPane("Accordion", new BorderPane()));
            return c;
        }, this::accessControl);
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void areaChart() {
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
        test(() -> {
            BarChart c = new BarChart(createCategoryAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createCategorySeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createCategorySeries());
            accessChart(c);
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void bubbleChart() {
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
        test(() -> {
            Button c = new Button();
            c.setSkin(new ButtonSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setAlignment(Pos.CENTER);
            c.setText(nextString());
            c.setDefaultButton(true);
        });
    }

    @Test
    public void canvas() {
        test(() -> {
            return new Canvas(30, 30);
        }, (c) -> {
            accessNode(c);
            GraphicsContext g = c.getGraphicsContext2D();
            g.setFill(Color.RED);
            g.setStroke(Color.BLACK);
            g.fillRect(5, 5, 5, 5);
        });
    }

    @Test
    public void checkBox() {
        test(() -> {
            CheckBox c = new CheckBox("checkbox");
            c.setSkin(new CheckBoxSkin(c));
            return c;
        }, (c) -> {
            c.setAllowIndeterminate(true);
            c.setSelected(true);
            accessControl(c);
        });
    }

    @Test
    public void choiceBox() {
        test(() -> {
            ChoiceBox c = new ChoiceBox();
            c.setSkin(new ChoiceBoxSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll("ChoiceBox", "1", "2");
            c.getSelectionModel().select(0);
            accessControl(c);
        });
    }

    @Test
    public void colorPicker() {
        test(() -> {
            ColorPicker c = new ColorPicker();
            c.setSkin(new ColorPickerSkin(c));
            c.setValue(Color.GREEN);
            return c;
        }, (c) -> {
            c.show(); // does not fail here, unlike DatePicker?
            c.setValue(Color.RED);
            c.prefHeight(-1);
            c.setValue(Color.BLACK);
            c.prefWidth(-1);
            accessControl(c);
        });
    }

    @Test
    public void comboBox() {
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
            c.show(); // does not fail here
        });
    }

    @Disabled("JDK-8349004") // FIX
    @Test
    public void datePicker() {
        test(() -> {
            DatePicker c = new DatePicker();
            c.setSkin(new DatePickerSkin(c));
            return c;
        }, (c) -> {
            c.show(); // fails here
            c.setValue(LocalDate.now());
            c.prefHeight(-1);
            c.setValue(LocalDate.EPOCH);
            c.prefWidth(-1);
            accessControl(c);
        });
    }

    @Test
    public void hyperlink() {
        test(() -> {
            Hyperlink c = new Hyperlink("Hyperlink");
            c.setSkin(new HyperlinkSkin(c));
            return c;
        }, (c) -> {
            c.setVisited(true);
            accessControl(c);
        });
    }

    @Test
    public void label() {
        test(() -> {
            Label c = new Label("Label");
            c.setSkin(new LabelSkin(c));
            return c;
        }, (c) -> {
            c.setLabelFor(c);
            accessControl(c);
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void lineChart() {
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

    @Disabled("JDK-8349096") // FIX
    @Test
    public void menuButton() {
        test(() -> {
            MenuButton c = new MenuButton();
            c.setSkin(new MenuButtonSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll(new MenuItem("MenuButton"));
            c.setPopupSide(Side.RIGHT);
            accessControl(c);
            c.show();
        });
    }

    @Disabled("JDK-8349105") // FIX
    @Test
    public void pagination() {
        test(() -> {
            Pagination c = new Pagination();
            c.setSkin(new PaginationSkin(c));
            return c;
        }, (c) -> {
            c.setPageFactory((pageIndex) -> {
                return new Label(pageIndex + " " + nextString());
            });
            c.setPageCount(100);
            c.setCurrentPageIndex(nextInt(100));
            accessControl(c);
        });
    }

    @Test
    public void passwordField() {
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

    @Disabled("JDK-8349090") // FIX
    @Test
    public void pieChart() {
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
    public void radioButton() {
        test(() -> {
            RadioButton c = new RadioButton("RadioButton");
            c.setSkin(new RadioButtonSkin(c));
            return c;
        }, (c) -> {
            accessControl(c);
            c.setSelected(nextBoolean());
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void scatterChart() {
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
    public void spinner() {
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

    @Disabled("JDK-8349096") // FIX
    @Test
    public void splitMenuButton() {
        test(() -> {
            SplitMenuButton c = new SplitMenuButton();
            c.setSkin(new SplitMenuButtonSkin(c));
            return c;
        }, (c) -> {
            c.getItems().setAll(new MenuItem("SplitMenuButton"));
            c.setPopupSide(Side.RIGHT);
            accessControl(c);
            c.show();
        });
    }

    @Disabled("JDK-8349091") // FIX
    @Test
    public void stackedAreaChart() {
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
        test(() -> {
            StackedBarChart c = new StackedBarChart(createCategoryAxis("x"), createNumberAxis("y"));
            c.getData().setAll(createCategorySeries());
            return c;
        }, (c) -> {
            c.getData().setAll(createCategorySeries());
            accessChart(c);
        });
    }

    @Disabled("JDK-8349098") // FIX
    @Test
    public void tabPane() {
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
        test(() -> {
            return new Text(nextString());
        }, (c) -> {
            c.setText(nextString());
            accessNode(c);
        });
    }

    @Test
    public void textArea() {
        test(() -> {
            TextArea c = new TextArea();
            c.setSkin(new TextAreaSkin(c));
            return c;
        }, (c) -> {
            accessTextInputControl(c);
        });
    }

    @Test
    public void textField() {
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
    public void titledPane() {
        test(() -> {
            TitledPane c = new TitledPane("TitledPane", null);
            c.setSkin(new TitledPaneSkin(c));
            return c;
        }, (c) -> {
            c.setAnimated(nextBoolean());
            c.setExpanded(nextBoolean());
            c.setContent(new Label(nextString()));
            accessControl(c);
        });
    }

    @Test
    public void toggleButton() {
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

    @Disabled("JDK-8348100") // FIX
    @Test
    public void tooltip() {
        test(() -> {
            Tooltip t = new Tooltip("this is a tooltip");
            t.setShowDelay(Duration.ZERO);
            t.setHideDelay(Duration.ZERO);
            Label c = new Label("testing tooltip");
            c.setSkin(new LabelSkin(c));
            c.setTooltip(t);
            return c;
        }, (c) -> {
            Tooltip t = c.getTooltip();
            t.isShowing();
            t.setGraphic(new Label("yo!"));
            if (Platform.isFxApplicationThread()) {
                Point2D p = c.localToScreen(c.getWidth() / 2.0, c.getHeight() / 2.0);
                robot.mouseMove(p);
            }
        });
    }

    @Test
    public void treeTableView() {
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
        Supplier<TreeItem<String>> gen = () -> {
            TreeItem<String> root = new TreeItem<>(null);
            return root;
        };

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

    private void accessTextInputControl(TextInputControl c) {
        accessControl(c);
        c.setPromptText("yo");
        c.setText(nextString());
        c.prefHeight(-1);
    }

    private void accessChart(Chart c) {
        String title = c.getClass().getSimpleName();
        c.setTitle(title);
        c.setAnimated(true);
        accessRegion(c);
    }

    private void accessControl(Control c) {
        accessRegion(c);
        c.getCssMetaData();
    }

    private void accessNode(Node c) {
        c.setFocusTraversable(true);
        c.requestFocus();
        c.toFront();
    }

    private void accessRegion(Region c) {
        accessNode(c);
        c.prefHeight(-1);
        c.prefWidth(-1);
        c.setPrefWidth(20);
        c.setPrefHeight(20);
    }

    private <T extends Node> void test(Supplier<T> generator, Consumer<T> operation) {
        T visibleNode = generator.get();
        String title = visibleNode.getClass().getSimpleName();

        setTitle(title);
        setContent(visibleNode);

        int threadCount = 1 + Runtime.getRuntime().availableProcessors() * 2;
        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch counter = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                new Thread(title) {
                    @Override
                    public void run() {
                        try {
                            T n = generator.get();
                            int count = 0;
                            while (running.get()) {
                                operation.accept(n);

                                count++;
                                if ((count % 100) == 0) {
                                    inFx(() -> {
                                        operation.accept(visibleNode);
                                    });
                                }
                            }
                        } finally {
                            counter.countDown();
                        }
                    }
                }.start();
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
        return new Random().nextBoolean();
    }

    private static double nextDouble() {
        return new Random().nextInt(50) - 25;
    }

    private static int nextInt(int max) {
        return new Random().nextInt(max);
    }

    private static String nextString() {
        long ix = seq.incrementAndGet();
        return "_a" + ix + "\nyo!";
    }

    private static List<Node> createButtons() {
        int sz = new Random().nextInt(5);
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
            double v = nextDouble();
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
            double v = nextDouble();
            s.getData().add(new XYChart.Data(i, v));
        }
        return s;
    }

    private static List<PieChart.Data> createPieSeries() {
        Random rnd = new Random();
        int sz = 1 + rnd.nextInt(20);
        ArrayList<Data> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(new PieChart.Data("N" + i, rnd.nextDouble()));
        }
        return a;
    }

    private static TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>(null);
        int sz = new Random().nextInt(20);
        for (int i = 0; i < sz; i++) {
            root.getChildren().add(new TreeItem<>(nextString()));
        }
        root.setExpanded(nextBoolean());
        return root;
    }

    private static List<Tab> createTabs() {
        ArrayList<Tab> a = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            a.add(new Tab(nextString()));
        }
        return a;
    }

    private static List<String> createTableItems() {
        int sz = new Random().nextInt(20);
        ArrayList<String> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(nextString());
        }
        return a;
    }

    private static List<Text> createTextItems() {
        int sz = new Random().nextInt(20);
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
