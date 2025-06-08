/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey;

import com.oracle.tools.fx.monkey.pages.*;

/**
 * Monkey Tester Pages.
 */
public class Pages {
    public static DemoPage[] create() {
        return new DemoPage[] {
            new DemoPage("Accordion", AccordionPage::new),
            new DemoPage("AnchorPane", AnchorPanePage::new),
            new DemoPage("AreaChart", AreaChartPage::new),
            new DemoPage("BarChart", BarChartPage::new),
            new DemoPage("BorderPane", BorderPanePage::new),
            new DemoPage("BubbleChart", BubbleChartPage::new),
            new DemoPage("Button", ButtonPage::new),
            new DemoPage("ButtonBar", ButtonBarPage::new),
            new DemoPage("Canvas", CanvasPage::new),
            new DemoPage("CheckBox", CheckBoxPage::new),
            new DemoPage("ChoiceBox", ChoiceBoxPage::new),
            new DemoPage("ComboBox", ComboBoxPage::new),
            new DemoPage("ColorPicker", ColorPickerPage::new),
            new DemoPage("DatePicker", DatePickerPage::new),
            // TODO DialogPane
            new DemoPage("Drag and Drop", DnDPage::new),
            new DemoPage("FlowPane", FlowPanePage::new),
            new DemoPage("GridPane", GridPanePage::new),
            new DemoPage("HBox", HBoxPage::new),
            new DemoPage("HTMLEditor", HTMLEditor_Page::new),
            new DemoPage("Hyperlink", HyperlinkPage::new),
            new DemoPage("Label", LabelPage::new),
            new DemoPage("LineChart", LineChartPage::new),
            new DemoPage("ListView", ListViewPage::new),
            new DemoPage("MediaPlayer", MediaPlayerPage::new),
            new DemoPage("MenuBar", MenuBarPage::new),
            new DemoPage("MenuButton", MenuButtonPage::new),
            new DemoPage("Pagination", PaginationPage::new),
            new DemoPage("PasswordField", PasswordFieldPage::new),
            new DemoPage("PieChart", PieChartPage::new),
            new DemoPage("ProgressIndicator", ProgressIndicatorPage::new),
            new DemoPage("RadioButton", RadioButtonPage::new),
            new DemoPage("ScatterChart", ScatterChartPage::new),
            new DemoPage("ScrollBar", ScrollBarPage::new),
            new DemoPage("ScrollPane", ScrollPanePage::new),
            new DemoPage("Separator", SeparatorPage::new),
            new DemoPage("Shape", ShapePage::new),
            new DemoPage("Slider", SliderPage::new),
            new DemoPage("Spinner", SpinnerPage::new),
            new DemoPage("SplitMenuButton", SplitMenuButtonPage::new),
            new DemoPage("StackedAreaChart", StackedAreaChartPage::new),
            new DemoPage("StackedBarChart", StackedBarChartPage::new),
            new DemoPage("StackPane", StackPanePage::new),
            new DemoPage("Stage", StagePage::new),
            new DemoPage("TableView", TableViewPage::new),
            new DemoPage("TabPane", TabPanePage::new),
            new DemoPage("Text", TextPage::new),
            new DemoPage("TextArea", TextAreaPage::new),
            new DemoPage("TextField", TextFieldPage::new),
            new DemoPage("TextFlow", TextFlowPage::new),
            new DemoPage("TilePane", TilePanePage::new),
            new DemoPage("TitledPane", TitledPanePage::new),
            new DemoPage("ToggleButton", ToggleButtonPage::new),
            new DemoPage("ToolBar", ToolBarPage::new),
            new DemoPage("Tooltip", TooltipPage::new),
            new DemoPage("TreeTableView", TreeTableViewPage::new),
            new DemoPage("TreeView", TreeViewPage::new),
            new DemoPage("VBox", VBoxPage::new),
            new DemoPage("WebView", WebViewPage::new),
        };
    }
}
