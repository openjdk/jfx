/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
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
            // a
            new DemoPage("Accordion", true, AccordionPage::new),
            new DemoPage("AnchorPane", AnchorPanePage::new),
            new DemoPage("AreaChart", AreaChartPage::new),
            new DemoPage("AudioClip", AudioClipPage::new),
            // b
            new DemoPage("BarChart", true, BarChartPage::new),
            new DemoPage("BorderPane", BorderPanePage::new),
            new DemoPage("BubbleChart", BubbleChartPage::new),
            new DemoPage("Button", ButtonPage::new),
            new DemoPage("ButtonBar", ButtonBarPage::new),
            // c
            new DemoPage("Canvas", true, CanvasPage::new),
            new DemoPage("CheckBox", CheckBoxPage::new),
            new DemoPage("ChoiceBox", ChoiceBoxPage::new),
            new DemoPage("Clipboard", ClipboardPage::new),
            new DemoPage("CodeArea", CodeAreaPage::new),
            new DemoPage("ComboBox", ComboBoxPage::new),
            new DemoPage("ColorPicker", ColorPickerPage::new),
            // d
            new DemoPage("DatePicker", true, DatePickerPage::new),
            new DemoPage("Dialog", DialogPage::new),
            new DemoPage("Drag and Drop", DnDPage::new),
            // f
            new DemoPage("FileChooser", true, FileChooserPage::new),
            new DemoPage("FlowPane", FlowPanePage::new),
            // g
            new DemoPage("GridPane", true, GridPanePage::new),
            // h
            new DemoPage("HBox", true, HBoxPage::new),
            new DemoPage("HTMLEditor", HTMLEditor_Page::new),
            new DemoPage("Hyperlink", HyperlinkPage::new),
            // i
            new DemoPage("ImageView", true, ImageViewPage::new),
            // l
            new DemoPage("Label", true, LabelPage::new),
            new DemoPage("LineChart", LineChartPage::new),
            new DemoPage("ListView", ListViewPage::new),
            // m
            new DemoPage("MediaPlayer", true, MediaPlayerPage::new),
            new DemoPage("MenuBar", MenuBarPage::new),
            new DemoPage("MenuButton", MenuButtonPage::new),
            // p
            new DemoPage("Pagination", true, PaginationPage::new),
            new DemoPage("PasswordField", PasswordFieldPage::new),
            new DemoPage("PieChart", PieChartPage::new),
            new DemoPage("Popup", PopupPage::new),
            new DemoPage("ProgressBar", ProgressBarPage::new),
            new DemoPage("ProgressIndicator", ProgressIndicatorPage::new),
            // r
            new DemoPage("RadioButton", true, RadioButtonPage::new),
            new DemoPage("RichTextArea", RichTextAreaPage::new),
            // s
            new DemoPage("ScatterChart", true, ScatterChartPage::new),
            new DemoPage("Screen", ScreenPage::new),
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
            // t
            new DemoPage("TableView", true, TableViewPage::new),
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
            // v
            new DemoPage("VBox", true, VBoxPage::new),
            new DemoPage("Virtual Keyboard", VirtualKeyboardPage::new),
            // w
            new DemoPage("WebView", true, WebViewPage::new),
        };
    }
}
