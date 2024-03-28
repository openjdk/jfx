/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
            new DemoPage("AreaChart", AreaChartPage::new),
            new DemoPage("BarChart", BarChartPage::new),
            new DemoPage("BubbleChart", BubbleChartPage::new),
            // TODO Button
            // TODO ButtonBar
            // TODO new DemoPage("Canvas", CanvasPage::new),
            // TODO CheckBox
            new DemoPage("ChoiceBox", ChoiceBoxPage::new),
            new DemoPage("ComboBox", ComboBoxPage::new),
            new DemoPage("ColorPicker", ColorPickerPage::new),
            new DemoPage("DatePicker", DatePickerPage::new),
            new DemoPage("HBox", HBoxPage::new),
            new DemoPage("HTMLEditor", HTMLEditor_Page::new),
            // TODO Hyperlink
            // TODO InputField: DoubleField, IntegerField, WebColorField
            new DemoPage("Label", LabelPage::new),
            new DemoPage("LineChart", LineChartPage::new),
            new DemoPage("ListView", ListViewPage::new),
            // TODO MenuBar
            new DemoPage("MenuButton", MenuButtonPage::new),
            new DemoPage("Pagination", PaginationPage::new),
            new DemoPage("PasswordField", PasswordFieldPage::new),
            new DemoPage("PieChart", PieChartPage::new),
            // TODO ProgressIndicator
            // TODO RadioButton
            new DemoPage("ScrollBar", ScrollBarPage::new),
            new DemoPage("ScrollPane", ScrollPanePage::new),
            // TODO Separator
            // TODO Slider
            new DemoPage("Spinner", SpinnerPage::new),
            // TODO SplitPane
            new DemoPage("StackedAreaChart", StackedAreaChartPage::new),
            new DemoPage("StackedBarChart", StackedBarChartPage::new),
            new DemoPage("TableView", TableViewPage::new),
            new DemoPage("TabPane", TabPanePage::new),
            new DemoPage("Text", TextPage::new),
            new DemoPage("TextArea", TextAreaPage::new),
            new DemoPage("TextField", TextFieldPage::new),
            new DemoPage("TextFlow", TextFlowPage::new),
            new DemoPage("TitledPane", TitledPanePage::new),
            // TODO ToggleButton
            new DemoPage("ToolBar", ToolBarPage::new),
            new DemoPage("Tooltip", TooltipPage::new),
            // TODO in tables: Cell, DateCell, IndexedCell* ?
            new DemoPage("TreeTableView", TreeTableViewPage::new),
            new DemoPage("TreeView", TreeViewPage::new),
            new DemoPage("VBox", VBoxPage::new),
            new DemoPage("WebView", WebViewPage::new),
        };
    }
}
