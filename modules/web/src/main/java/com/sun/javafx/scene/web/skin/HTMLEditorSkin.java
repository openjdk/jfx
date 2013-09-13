/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.web.skin;

import java.util.ResourceBundle;

import com.sun.javafx.application.PlatformImpl;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.ColorPickerSkin;
import com.sun.javafx.scene.control.skin.FXVK;
import com.sun.javafx.scene.web.behavior.HTMLEditorBehavior;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCFocusEvent;
import com.sun.javafx.webkit.Accessor;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import javafx.collections.ListChangeListener;

import static javafx.geometry.NodeOrientation.*;
import javafx.print.PrinterJob;

/**
 * HTML editor skin.
 */
public class HTMLEditorSkin extends BehaviorSkinBase<HTMLEditor, HTMLEditorBehavior> implements TraverseListener {
    private GridPane gridPane;

    private ToolBar toolbar1;
    private ToolBar toolbar2;

    private Button cutButton;
    private Button copyButton;
    private Button pasteButton;

//    private Button undoButton;
//    private Button redoButton;

    private Button insertHorizontalRuleButton;

    private ToggleGroup alignmentToggleGroup;
    private ToggleButton alignLeftButton;
    private ToggleButton alignCenterButton;
    private ToggleButton alignRightButton;
    private ToggleButton alignJustifyButton;

    private ToggleButton bulletsButton;
    private ToggleButton numbersButton;

    private Button indentButton;
    private Button outdentButton;

    private ComboBox<String> formatComboBox;
    private Map<String, String> formatStyleMap;
    private Map<String, String> styleFormatMap;

    private ComboBox<String> fontFamilyComboBox;

    private ComboBox<String> fontSizeComboBox;
    private Map<String, String> fontSizeMap;
    private Map<String, String> sizeFontMap;

    private ToggleButton boldButton;
    private ToggleButton italicButton;
    private ToggleButton underlineButton;
    private ToggleButton strikethroughButton;

    private ColorPicker fgColorButton;
    private ColorPicker bgColorButton;

    private WebView webView;
    private WebPage webPage;

    private static final String CUT_COMMAND = "cut";
    private static final String COPY_COMMAND = "copy";
    private static final String PASTE_COMMAND = "paste";

    private static final String UNDO_COMMAND = "undo";
    private static final String REDO_COMMAND = "redo";

    private static final String INSERT_HORIZONTAL_RULE_COMMAND = "inserthorizontalrule";

    private static final String ALIGN_LEFT_COMMAND = "justifyleft";
    private static final String ALIGN_CENTER_COMMAND = "justifycenter";
    private static final String ALIGN_RIGHT_COMMAND = "justifyright";
    private static final String ALIGN_JUSTIFY_COMMAND = "justifyfull";

    private static final String BULLETS_COMMAND = "insertUnorderedList";
    private static final String NUMBERS_COMMAND = "insertOrderedList";

    private static final String INDENT_COMMAND = "indent";
    private static final String OUTDENT_COMMAND = "outdent";

    private static final String FORMAT_COMMAND = "formatblock";
    private static final String FONT_FAMILY_COMMAND = "fontname";
    private static final String FONT_SIZE_COMMAND = "fontsize";

    private static final String BOLD_COMMAND = "bold";
    private static final String ITALIC_COMMAND = "italic";
    private static final String UNDERLINE_COMMAND = "underline";
    private static final String STRIKETHROUGH_COMMAND = "strikethrough";

    private static final String FOREGROUND_COLOR_COMMAND = "forecolor";
    private static final String BACKGROUND_COLOR_COMMAND = "backcolor";

    private static final Color DEFAULT_BG_COLOR = Color.WHITE;
    private static final Color DEFAULT_FG_COLOR = Color.BLACK;

    private static final String FORMAT_PARAGRAPH = "<p>";
    private static final String FORMAT_HEADING_1 = "<h1>";
    private static final String FORMAT_HEADING_2 = "<h2>";
    private static final String FORMAT_HEADING_3 = "<h3>";
    private static final String FORMAT_HEADING_4 = "<h4>";
    private static final String FORMAT_HEADING_5 = "<h5>";
    private static final String FORMAT_HEADING_6 = "<h6>";

    private static final String SIZE_XX_SMALL = "1";
    private static final String SIZE_X_SMALL = "2";
    private static final String SIZE_SMALL = "3";
    private static final String SIZE_MEDIUM = "4";
    private static final String SIZE_LARGE = "5";
    private static final String SIZE_X_LARGE = "6";
    private static final String SIZE_XX_LARGE = "7";

    private static final String INSERT_NEW_LINE_COMMAND = "insertnewline";
    private static final String INSERT_TAB_COMMAND = "inserttab";

    // As per RT-16330: default format -> bold/size mappings are as follows:
    private static final String[][] DEFAULT_FORMAT_MAPPINGS = {
        { FORMAT_PARAGRAPH,   "",             SIZE_SMALL     },
        { FORMAT_HEADING_1,   BOLD_COMMAND,   SIZE_X_LARGE   },
        { FORMAT_HEADING_2,   BOLD_COMMAND,   SIZE_LARGE     },
        { FORMAT_HEADING_3,   BOLD_COMMAND,   SIZE_MEDIUM    },
        { FORMAT_HEADING_4,   BOLD_COMMAND,   SIZE_SMALL     },
        { FORMAT_HEADING_5,   BOLD_COMMAND,   SIZE_X_SMALL   },
        { FORMAT_HEADING_6,   BOLD_COMMAND,   SIZE_XX_SMALL  },
    };

    // As per RT-16379: default OS -> font mappings:
    private static final String[] DEFAULT_WINDOWS_7_MAPPINGS = {
        "Windows 7",       "Segoe UI",        "12px",   "",     "120"
    };
    private static final String[][] DEFAULT_OS_MAPPINGS = {
        // OS               Font name           size      weight  DPI
        { "Windows XP",      "Tahoma",          "12px",   "",     "96"  },
        { "Windows Vista",   "Segoe UI",        "12px",   "",     "96"  },
        DEFAULT_WINDOWS_7_MAPPINGS,
        { "Mac OS X",        "Lucida Grande",   "12px",   "",     "72"  },
        { "Linux",           "Lucida Sans",   "12px",   "",     "96"  },
    };
    private static final String DEFAULT_OS_FONT = getOSMappings()[1];

    private static String[] getOSMappings() {
        String os = System.getProperty("os.name");
        for  (int i = 0; i < DEFAULT_OS_MAPPINGS.length; i++) {
            if (os.equals(DEFAULT_OS_MAPPINGS[i][0])) {
                return DEFAULT_OS_MAPPINGS[i];
            }
        }

        return DEFAULT_WINDOWS_7_MAPPINGS;
    }

    private TraversalEngine engine;

    private boolean resetToolbarState = false;
    private String cachedHTMLText = "<html><head></head><body contenteditable=\"true\"></body></html>";
    private ListChangeListener<Node> itemsListener = new ListChangeListener<Node>() {
        @Override public void onChanged(ListChangeListener.Change<? extends Node> c) {
            while (c.next()) {
                if (c.getRemovedSize() > 0) {
                    for (Node n : c.getList()) {
                        if (n instanceof WebView) {
                            // RT-28611 webView removed - set associated webPage to null
                            webPage.dispose();
                        }
                    }
                }
            }
        }
    };
    public HTMLEditorSkin(HTMLEditor htmlEditor) {
        super(htmlEditor, new HTMLEditorBehavior(htmlEditor));

        getChildren().clear();

        gridPane = new GridPane();
        gridPane.getStyleClass().add("grid");
        getChildren().addAll(gridPane);

        toolbar1 = new ToolBar();
        toolbar1.getStyleClass().add("top-toolbar");
        gridPane.add(toolbar1, 0, 0);

        toolbar2 = new ToolBar();
        toolbar2.getStyleClass().add("bottom-toolbar");
        gridPane.add(toolbar2, 0, 1);

//        populateToolbars();

        webView = new WebView();
        gridPane.add(webView, 0, 2);

        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().add(column);

        webPage = Accessor.getPageFor(webView.getEngine());

        webView.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        updateToolbarState(true);
                    }
                });
            }
        });


        webView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override public void handle(final KeyEvent event) {
                applyTextFormatting();
                if (event.getCode() == KeyCode.CONTROL || event.getCode() == KeyCode.META) {
                    return;
                }
                if (event.getCode() == KeyCode.TAB && !event.isControlDown()) {
                    if (!event.isShiftDown()) {
                        /*
                        ** if we are in either Bullet or Numbers mode then the
                        ** TAB key tells us to indent again.
                        */
                        if (getCommandState(BULLETS_COMMAND) || getCommandState(NUMBERS_COMMAND)) {
                            executeCommand(INDENT_COMMAND, null);
                        }
                        else {
                            executeCommand(INSERT_TAB_COMMAND, null);
                        }
                    }
                    else {
                        /*
                        ** if we are in either Bullet or Numbers mode then the
                        ** Shift-TAB key tells us to outdent.
                        */
                        if (getCommandState(BULLETS_COMMAND) || getCommandState(NUMBERS_COMMAND)) {
                            executeCommand(OUTDENT_COMMAND, null);
                        }
                    }
                    return;
                }
                // Work around for bug that sends events from ColorPicker to this Scene
                if ((fgColorButton != null && fgColorButton.isShowing()) ||
                    (bgColorButton != null && bgColorButton.isShowing())) {
                    return;
                }
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        if (webPage.getClientSelectedText().isEmpty()) {
                            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN ||
                                event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT ||
                                event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END) {
                                updateToolbarState(true);
                            } else if (event.isControlDown() || event.isMetaDown()) {
                                if (event.getCode() == KeyCode.B) {
                                    keyboardShortcuts(BOLD_COMMAND);
                                } else if(event.getCode() == KeyCode.I) {
                                    keyboardShortcuts(ITALIC_COMMAND);
                                } else if (event.getCode() == KeyCode.U) {
                                    keyboardShortcuts(UNDERLINE_COMMAND);
                                }
                                updateToolbarState(true);
                            } else {
                                resetToolbarState = event.getCode() == KeyCode.ENTER;
                                if (resetToolbarState) {
                                    if (getCommandState(BOLD_COMMAND) !=  boldButton.selectedProperty().getValue()) {
                                        executeCommand(BOLD_COMMAND, boldButton.selectedProperty().getValue().toString());
                                    }
                                }
                                updateToolbarState(false);
                            }
                            resetToolbarState = false;
                        }
                        else if (event.isShiftDown() && 
                                 (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN ||
                                  event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT)) {
                            updateToolbarState(true);
                        }
                    }
                });
            }
        });

        webView.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override public void handle(final KeyEvent event) {
                if (event.getCode() == KeyCode.CONTROL || event.getCode() == KeyCode.META) {
                    return;
                }
                // Work around for bug that sends events from ColorPicker to this Scene
                if ((fgColorButton != null && fgColorButton.isShowing()) ||
                    (bgColorButton != null && bgColorButton.isShowing())) {
                    return;
                }
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        if (webPage.getClientSelectedText().isEmpty()) {
                            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN ||
                                event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT ||
                                event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END) {
                                updateToolbarState(true);
                            } else if (event.isControlDown() || event.isMetaDown()) {
                                if (event.getCode() == KeyCode.B) {
                                    keyboardShortcuts(BOLD_COMMAND);
                                } else if(event.getCode() == KeyCode.I) {
                                    keyboardShortcuts(ITALIC_COMMAND);
                                } else if (event.getCode() == KeyCode.U) {
                                    keyboardShortcuts(UNDERLINE_COMMAND);
                                }
                                updateToolbarState(true);
                            } else {
                                resetToolbarState = event.getCode() == KeyCode.ENTER;
                                if (!resetToolbarState) {
                                    updateToolbarState(false);
                                }
                            }
                            resetToolbarState = false;
                        }
                    }
                });
            }
        });

        getSkinnable().focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, final Boolean newValue) {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        if (newValue) {
                            webView.requestFocus();
                        }
                    }
                });
            }
        });

        webView.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, final Boolean newValue) {
                // disabling as a fix for RT-30081
//                if (newValue) {
//                    webPage.dispatchFocusEvent(new WCFocusEvent(WCFocusEvent.FOCUS_GAINED, WCFocusEvent.FORWARD));
//                    enableToolbar(true);
//                } else {
//                    webPage.dispatchFocusEvent(new WCFocusEvent(WCFocusEvent.FOCUS_LOST, WCFocusEvent.FORWARD));
//                    enableToolbar(false);
//                }
                
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        updateToolbarState(true);
                        
                        if (PlatformImpl.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)) {
                            Scene scene = getSkinnable().getScene();
                            if (newValue) {
                                FXVK.attach(webView);
                            } else if (scene == null ||
                                       scene.getWindow() == null ||
                                       !scene.getWindow().isFocused() ||
                                       !(scene.getFocusOwner() instanceof TextInputControl /*||
                                         getScene().getFocusOwner() instanceof WebView*/)) {
                                FXVK.detach();
                            }
                        }
                    }
                });
            }
        });

        webView.getEngine().getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        webView.requestLayout();
                    }
                });

                double totalWork = webView.getEngine().getLoadWorker().getTotalWork();
                if (newValue.doubleValue() == totalWork) {                    
                    cachedHTMLText = null;
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            setContentEditable(true);
                            updateToolbarState(true);
                            updateNodeOrientation();
                        }
                    });
                }
            }
        });

        enableToolbar(true);
        setHTMLText(cachedHTMLText);

        engine = new TraversalEngine(getSkinnable(), false);
        engine.addTraverseListener(this);
        engine.reg(toolbar1);
        getSkinnable().setImpl_traversalEngine(engine);
        webView.setFocusTraversable(true);
        gridPane.getChildren().addListener(itemsListener);
    }
    
    public final String getHTMLText() {
        // RT17203 setHTMLText is asynchronous.  We use the cached version of
        // the html text until the page finishes loading.        
        return cachedHTMLText != null ? cachedHTMLText : webPage.getHtml(webPage.getMainFrame());
    }

    public final void setHTMLText(String htmlText) {
        cachedHTMLText = htmlText;
        webPage.load(webPage.getMainFrame(), htmlText, "text/html");

        Platform.runLater(new Runnable() {
            @Override public void run() {
                updateToolbarState(true);
            }
        });
    }

    private ResourceBundle resources;

    private void populateToolbars() {
        resources = ResourceBundle.getBundle(HTMLEditorSkin.class.getName());

        // Toolbar 1
        cutButton = addButton(toolbar1, resources.getString("cutIcon"), resources.getString("cut"), CUT_COMMAND, "html-editor-cut");
        copyButton = addButton(toolbar1, resources.getString("copyIcon"), resources.getString("copy"), COPY_COMMAND, "html-editor-copy");
        pasteButton = addButton(toolbar1, resources.getString("pasteIcon"), resources.getString("paste"), PASTE_COMMAND, "html-editor-paste");

        toolbar1.getItems().add(new Separator());

//        undoButton = addButton(toolbar1, "undoIcon", resources.getString("undo"), UNDO_COMMAND);
//        redoButton = addButton(toolbar1, "redoIcon", resources.getString("redo"), REDO_COMMAND);//
//        toolbar1.getItems().add(new Separator());

         alignmentToggleGroup = new ToggleGroup();
         alignLeftButton = addToggleButton(toolbar1, alignmentToggleGroup,
            resources.getString("alignLeftIcon"), resources.getString("alignLeft"), ALIGN_LEFT_COMMAND, "html-editor-align-left");
         alignCenterButton = addToggleButton(toolbar1, alignmentToggleGroup,
            resources.getString("alignCenterIcon"), resources.getString("alignCenter"), ALIGN_CENTER_COMMAND, "html-editor-align-center");
         alignRightButton = addToggleButton(toolbar1, alignmentToggleGroup,
            resources.getString("alignRightIcon"), resources.getString("alignRight"), ALIGN_RIGHT_COMMAND, "html-editor-align-right");
         alignJustifyButton = addToggleButton(toolbar1, alignmentToggleGroup,
            resources.getString("alignJustifyIcon"), resources.getString("alignJustify"), ALIGN_JUSTIFY_COMMAND, "html-editor-align-justify");

        toolbar1.getItems().add(new Separator());

        outdentButton = addButton(toolbar1, resources.getString("outdentIcon"), resources.getString("outdent"), OUTDENT_COMMAND, "html-editor-outdent");
        if (outdentButton.getGraphic() != null) outdentButton.getGraphic().setNodeOrientation(NodeOrientation.INHERIT);
        indentButton = addButton(toolbar1, resources.getString("indentIcon"), resources.getString("indent"), INDENT_COMMAND, "html-editor-indent");
        if (indentButton.getGraphic() != null) indentButton.getGraphic().setNodeOrientation(NodeOrientation.INHERIT);

        toolbar1.getItems().add(new Separator());

         ToggleGroup listStyleToggleGroup = new ToggleGroup();
         bulletsButton = addToggleButton(toolbar1, listStyleToggleGroup,
            resources.getString("bulletsIcon"), resources.getString("bullets"), BULLETS_COMMAND, "html-editor-bullets");
         if (bulletsButton.getGraphic() != null) bulletsButton.getGraphic().setNodeOrientation(NodeOrientation.INHERIT);
         numbersButton = addToggleButton(toolbar1, listStyleToggleGroup,
            resources.getString("numbersIcon"), resources.getString("numbers"), NUMBERS_COMMAND, "html-editor-numbers");

        toolbar1.getItems().add(new Separator());

        //toolbar1.getItems().add(new Separator());

        // Toolbar 2
        formatComboBox = new ComboBox<String>();
        formatComboBox.getStyleClass().add("font-menu-button");
        formatComboBox.setFocusTraversable(false);
        formatComboBox.setMinWidth(Region.USE_PREF_SIZE);
        toolbar2.getItems().add(formatComboBox);

        formatStyleMap = new HashMap<String, String>();
        styleFormatMap = new HashMap<String, String>();

        createFormatMenuItem(FORMAT_PARAGRAPH, resources.getString("paragraph"));
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                formatComboBox.setValue(resources.getString("paragraph"));
            }
        });
        createFormatMenuItem(FORMAT_HEADING_1, resources.getString("heading1"));
        createFormatMenuItem(FORMAT_HEADING_2, resources.getString("heading2"));
        createFormatMenuItem(FORMAT_HEADING_3, resources.getString("heading3"));
        createFormatMenuItem(FORMAT_HEADING_4, resources.getString("heading4"));
        createFormatMenuItem(FORMAT_HEADING_5, resources.getString("heading5"));
        createFormatMenuItem(FORMAT_HEADING_6, resources.getString("heading6"));

//        formatComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
//            @Override public ListCell<String> call(ListView<String> param) {
//                final ListCell<String> cell = new ListCell<String>() {
//                    @Override public void updateItem(String item, boolean empty) {
//                        super.updateItem(item, empty);
//                        if (item != null) {
//                            setText(item);
//                        }
//                    }
//                };
//                return cell;
//            }
//        });

        formatComboBox.setTooltip(new Tooltip(resources.getString("format")));

        formatComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue == null) {
                    formatComboBox.setValue(null);
                } else {
                    String formatValue = formatStyleMap.get(newValue);
                    executeCommand(FORMAT_COMMAND, formatValue);
                    updateToolbarState(false);

                    // RT-16330 match the new font format with the required weight and size
                    for (int i = 0; i < DEFAULT_FORMAT_MAPPINGS.length; i++) {
                        String[] mapping = DEFAULT_FORMAT_MAPPINGS[i];
                        if (mapping[0].equalsIgnoreCase(formatValue)) {
                            executeCommand(FONT_SIZE_COMMAND, mapping[2]);
                            updateToolbarState(false);
                            break;
                        }
                    }
                }
            }
        });

        fontFamilyComboBox = new ComboBox<String>();
        fontFamilyComboBox.getStyleClass().add("font-menu-button");
        fontFamilyComboBox.setMinWidth(FONT_FAMILY_MENUBUTTON_WIDTH);
        fontFamilyComboBox.setPrefWidth(FONT_FAMILY_MENUBUTTON_WIDTH);
        fontFamilyComboBox.setMaxWidth(FONT_FAMILY_MENUBUTTON_WIDTH);
        fontFamilyComboBox.setFocusTraversable(false);
        fontFamilyComboBox.setTooltip(new Tooltip(resources.getString("fontFamily")));
        toolbar2.getItems().add(fontFamilyComboBox);

        // Fix for RT-32906, where all rows were being put through the cell factory
        // so that they could be measured. Because we have a fixed width for the
        // button this is unnecessary and so we tell the ComboBox to not measure
        // any rows.
        fontFamilyComboBox.getProperties().put("comboBoxRowsToMeasureWidth", 0);

        fontFamilyComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> param) {
                final ListCell<String> cell = new ListCell<String>() {
                    @Override public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            setFont(new Font(item, 12));
                        }
                    }
                };
                cell.setMinWidth(FONT_FAMILY_MENU_WIDTH);
                cell.setPrefWidth(FONT_FAMILY_MENU_WIDTH);
                cell.setMaxWidth(FONT_FAMILY_MENU_WIDTH);
                return cell;
            }
        });

        Platform.runLater(new Runnable() {
                @Override public void run() {
                    final ObservableList<String> fonts = FXCollections.observableArrayList(Font.getFamilies());
                    for (String fontFamily : fonts) {
                        if (DEFAULT_OS_FONT.equals(fontFamily)) {
                            fontFamilyComboBox.setValue(fontFamily);
                        }
                        fontFamilyComboBox.setItems(fonts);
                    }
                }
            });

        fontFamilyComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                executeCommand(FONT_FAMILY_COMMAND, newValue);
            }
        });

        fontSizeComboBox = new ComboBox<String>();
        fontSizeComboBox.getStyleClass().add("font-menu-button");
        fontSizeComboBox.setFocusTraversable(false);
        toolbar2.getItems().add(fontSizeComboBox);

        fontSizeMap = new HashMap<String, String>();
        sizeFontMap = new HashMap<String, String>();

        createFontSizeMenuItem(SIZE_XX_SMALL, resources.getString("extraExtraSmall"));
        createFontSizeMenuItem(SIZE_X_SMALL, resources.getString("extraSmall"));
        createFontSizeMenuItem(SIZE_SMALL, resources.getString("small"));
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                fontSizeComboBox.setValue(resources.getString("small"));
            }
        });
        createFontSizeMenuItem(SIZE_MEDIUM, resources.getString("medium"));
        createFontSizeMenuItem(SIZE_LARGE, resources.getString("large"));
        createFontSizeMenuItem(SIZE_X_LARGE, resources.getString("extraLarge"));
        createFontSizeMenuItem(SIZE_XX_LARGE, resources.getString("extraExtraLarge"));
        fontSizeComboBox.setTooltip(new Tooltip(resources.getString("fontSize")));

        fontSizeComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> param) {
                final ListCell<String> cell = new ListCell<String>() {
                    @Override public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            setFont(new Font((String)fontFamilyComboBox.getValue(), Double.valueOf(item.substring(0, item.indexOf(" ")))));
                        }
                    }
                };
                return cell;
            }
        });


        fontSizeComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Object fontSizeValue = getCommandValue(FONT_SIZE_COMMAND);
                if (!newValue.equals(fontSizeValue)) {
                    executeCommand(FONT_SIZE_COMMAND, fontSizeMap.get(newValue));
                }
            }
        });

        toolbar2.getItems().add(new Separator());

        boldButton = addToggleButton(toolbar2, null,
            resources.getString("boldIcon"), resources.getString("bold"), BOLD_COMMAND, "html-editor-bold");
        boldButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Only use the bold button for paragraphs.  We don't
                // want to turn bold off for headings.

                if ("<p>".equals(formatStyleMap.get(formatComboBox.getValue())))  {
                    executeCommand(BOLD_COMMAND, boldButton.selectedProperty().getValue().toString());
                }
            }
        });
        italicButton = addToggleButton(toolbar2, null,
            resources.getString("italicIcon"), resources.getString("italic"), ITALIC_COMMAND, "html-editor-italic");
        underlineButton = addToggleButton(toolbar2, null,
            resources.getString("underlineIcon"), resources.getString("underline"), UNDERLINE_COMMAND, "html-editor-underline");
        strikethroughButton = addToggleButton(toolbar2, null,
            resources.getString("strikethroughIcon"), resources.getString("strikethrough"), STRIKETHROUGH_COMMAND, "html-editor-strike");

        toolbar2.getItems().add(new Separator());

        insertHorizontalRuleButton = addButton(toolbar2, resources.getString("insertHorizontalRuleIcon"),
            resources.getString("insertHorizontalRule"), INSERT_HORIZONTAL_RULE_COMMAND, "html-editor-hr");
        // We override setOnAction to insert a new line.  This fixes RT-16453
        insertHorizontalRuleButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                executeCommand(INSERT_NEW_LINE_COMMAND, null);
                executeCommand(INSERT_HORIZONTAL_RULE_COMMAND, null);
                updateToolbarState(false);
            }
        });

        fgColorButton = new ColorPicker();
        fgColorButton.getStyleClass().add("html-editor-foreground");
        fgColorButton.setFocusTraversable(false);
        toolbar1.getItems().add(fgColorButton);

        fgColorButton.impl_processCSS(true);
        ColorPickerSkin fgColorPickerSkin = (ColorPickerSkin) fgColorButton.getSkin();
        String fgIcon = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override public String run() {
                return HTMLEditorSkin.class.getResource(resources.getString("foregroundColorIcon")).toString();
            }
        });
        ((StyleableProperty)fgColorPickerSkin.imageUrlProperty()).applyStyle(null,fgIcon);

        fgColorButton.setValue(DEFAULT_FG_COLOR);
        fgColorButton.setTooltip(new Tooltip(resources.getString("foregroundColor")));
        fgColorButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent ev) {
                Color newValue = fgColorButton.getValue();
                if (newValue != null) {
                    executeCommand(FOREGROUND_COLOR_COMMAND, colorValueToHex(newValue));
                    fgColorButton.hide();
                }
            }
        });

        bgColorButton = new ColorPicker();
        bgColorButton.getStyleClass().add("html-editor-background");
        bgColorButton.setFocusTraversable(false);
        toolbar1.getItems().add(bgColorButton);

        bgColorButton.impl_processCSS(true);
        ColorPickerSkin  bgColorPickerSkin = (ColorPickerSkin) bgColorButton.getSkin();
        String bgIcon = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override public String run() {
                return HTMLEditorSkin.class.getResource(resources.getString("backgroundColorIcon")).toString();
            }
        });
        ((StyleableProperty)bgColorPickerSkin.imageUrlProperty()).applyStyle(null,bgIcon);

        bgColorButton.setValue(DEFAULT_BG_COLOR);
        bgColorButton.setTooltip(new Tooltip(resources.getString("backgroundColor")));

        bgColorButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent ev) {
                Color newValue = bgColorButton.getValue();
                if (newValue != null) {
                    executeCommand(BACKGROUND_COLOR_COMMAND, colorValueToHex(newValue));
                    bgColorButton.hide();
                }
            }
        });
    }
    
    private String colorValueToHex(Color c) {
        return String.format((Locale)null, "#%02x%02x%02x",
                             Math.round(c.getRed() * 255),
                             Math.round(c.getGreen() * 255),
                             Math.round(c.getBlue() * 255));
    }

    private Button addButton(ToolBar toolbar, final String iconName, String tooltipText,
            final String command, final String styleClass) {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.getStyleClass().add(styleClass);
        toolbar.getItems().add(button);

        Image icon = AccessController.doPrivileged(new PrivilegedAction<Image>() {
            @Override public Image run() {
                return new Image(HTMLEditorSkin.class.getResource(iconName).toString());
            }
        });
//        button.setGraphic(new ImageView(icon));
        ((StyleableProperty)button.graphicProperty()).applyStyle(null,new ImageView(icon));
        button.setTooltip(new Tooltip(tooltipText));

        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                executeCommand(command, null);
                updateToolbarState(false);
            }
        });

        return button;
    }

    private ToggleButton addToggleButton(ToolBar toolbar, ToggleGroup toggleGroup,
            final String iconName, String tooltipText, final String command, final String styleClass) {
        ToggleButton toggleButton = new ToggleButton();
        toggleButton.setUserData(command);
        toggleButton.setFocusTraversable(false);
        toggleButton.getStyleClass().add(styleClass);
        toolbar.getItems().add(toggleButton);
        if (toggleGroup != null) {
            toggleButton.setToggleGroup(toggleGroup);
        }

        Image icon = AccessController.doPrivileged(new PrivilegedAction<Image>() {
            @Override public Image run() {
                return new Image(HTMLEditorSkin.class.getResource(iconName).toString());
            }
        });
        ((StyleableProperty)toggleButton.graphicProperty()).applyStyle(null,new ImageView(icon));
//        toggleButton.setGraphic(new ImageView(icon));

        toggleButton.setTooltip(new Tooltip(tooltipText));

        if (!BOLD_COMMAND.equals(command)) {
            toggleButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (getCommandState(command) != newValue.booleanValue()) {
                        executeCommand(command, null);
                    }
                }
            });
        }
        return toggleButton;
    }

    private void createFormatMenuItem(String formatValue, String label) {
        formatComboBox.getItems().add(label);
        formatStyleMap.put(label, formatValue);
        styleFormatMap.put(formatValue, label);
    }

    private void createFontSizeMenuItem(String fontSizeValue, String label) {
        fontSizeComboBox.getItems().add(label);
        fontSizeMap.put(label, fontSizeValue);
        sizeFontMap.put(fontSizeValue, label);
    }

    private void updateNodeOrientation() {
        NodeOrientation orientation = getSkinnable().getEffectiveNodeOrientation();

        HTMLDocument htmlDocument = (HTMLDocument)webPage.getDocument(webPage.getMainFrame());
        HTMLElement htmlDocumentElement = (HTMLElement)htmlDocument.getDocumentElement();
        if (htmlDocumentElement.getAttribute("dir") == null) {
            htmlDocumentElement.setAttribute("dir", (orientation == RIGHT_TO_LEFT) ? "rtl" : "ltr");
        }

        if (orientation == RIGHT_TO_LEFT) {
            try {
                final String iconName = resources.getString("numbersIcon-rtl");
                Image icon = AccessController.doPrivileged(new PrivilegedAction<Image>() {
                    @Override public Image run() {
                        return new Image(HTMLEditorSkin.class.getResource(iconName).toString());
                    }
                });
                numbersButton.setGraphic(new ImageView(icon));
            } catch (java.util.MissingResourceException ex) {
                // ignore
            }
        }
    }

    private void updateToolbarState(final boolean updateAlignment) {
        if (!webView.isFocused()) {
            return;
        }

        // These command aways return true.
        copyButton.setDisable(!isCommandEnabled(CUT_COMMAND));
        cutButton.setDisable(!isCommandEnabled(COPY_COMMAND));
        pasteButton.setDisable(!isCommandEnabled(PASTE_COMMAND));

        // undoButton.setDisable(!isCommandEnabled(UNDO_COMMAND));
        // redoButton.setDisable(!isCommandEnabled(REDO_COMMAND));

//        undoButton.setDisable(!isCommandEnabled(FORMAT_COMMAND));
//        redoButton.setDisable(!isCommandEnabled(FORMAT_COMMAND));

        insertHorizontalRuleButton.setDisable(!isCommandEnabled(INSERT_HORIZONTAL_RULE_COMMAND));

        if (updateAlignment) {
            alignLeftButton.setDisable(!isCommandEnabled(ALIGN_LEFT_COMMAND));
            alignLeftButton.setSelected(getCommandState(ALIGN_LEFT_COMMAND));
            alignCenterButton.setDisable(!isCommandEnabled(ALIGN_CENTER_COMMAND));
            alignCenterButton.setSelected(getCommandState(ALIGN_CENTER_COMMAND));
            alignRightButton.setDisable(!isCommandEnabled(ALIGN_RIGHT_COMMAND));
            alignRightButton.setSelected(getCommandState(ALIGN_RIGHT_COMMAND));
            alignJustifyButton.setDisable(!isCommandEnabled(ALIGN_JUSTIFY_COMMAND));
            alignJustifyButton.setSelected(getCommandState(ALIGN_JUSTIFY_COMMAND));
        } else {
            if (alignmentToggleGroup.getSelectedToggle() != null) {
                String command = alignmentToggleGroup.getSelectedToggle().getUserData().toString();
                if (isCommandEnabled(command) && !getCommandState(command) ) {
                    executeCommand(command, null);
                }
            }
        }

        if (alignmentToggleGroup.getSelectedToggle() == null) {
            alignmentToggleGroup.selectToggle(alignLeftButton);
        }

        bulletsButton.setDisable(!isCommandEnabled(BULLETS_COMMAND));
        bulletsButton.setSelected(getCommandState(BULLETS_COMMAND));
        numbersButton.setDisable(!isCommandEnabled(NUMBERS_COMMAND));
        numbersButton.setSelected(getCommandState(NUMBERS_COMMAND));

        indentButton.setDisable(!isCommandEnabled(INDENT_COMMAND));
        outdentButton.setDisable(!isCommandEnabled(OUTDENT_COMMAND));

        formatComboBox.setDisable(!isCommandEnabled(FORMAT_COMMAND));


        String formatValue = getCommandValue(FORMAT_COMMAND);
        if (formatValue != null) {
            String htmlTag = "<" + formatValue + ">";
            String comboFormatValue = styleFormatMap.get(htmlTag);
            String format = formatComboBox.getValue();

            // if the format value is then we assume that we're dealing with a paragraph,
            // which seems to correspond with the HTML output we receive.
            if ((resetToolbarState || htmlTag.equals("<>") || htmlTag.equalsIgnoreCase("<div>"))) {
                formatComboBox.setValue(resources.getString("paragraph"));
            } else if (format != null && ! format.equalsIgnoreCase(comboFormatValue)) {
                formatComboBox.setValue(comboFormatValue);
            }
        }

        fontFamilyComboBox.setDisable(!isCommandEnabled(FONT_FAMILY_COMMAND));
        final String fontFamilyValue = getCommandValue(FONT_FAMILY_COMMAND);
        if (fontFamilyValue != null) {
            String fontFamilyStr = fontFamilyValue;

            // stripping out apostrophe characters, which are appended to either
            // end of the font face name when the font face has one or more spaces.
            if (fontFamilyStr.startsWith("'")) {
                fontFamilyStr = fontFamilyStr.substring(1);
            }
            if (fontFamilyStr.endsWith("'")) {
                fontFamilyStr = fontFamilyStr.substring(0,fontFamilyStr.length() - 1);
            }

            Object selectedFont = fontFamilyComboBox.getValue();
            if (selectedFont instanceof String) {
                if (!selectedFont.equals(fontFamilyStr)) { 

                    ObservableList<String> fontFamilyItems = fontFamilyComboBox.getItems();
                    String selectedComboFont = null;
                    for (String comboFontFamilyValue : fontFamilyItems) {

                        if (comboFontFamilyValue.equals(fontFamilyStr)) {
                            selectedComboFont = comboFontFamilyValue;
                            break;
                        }
                        // Note: By default, 'Dialog' is the font returned from webview.
                        // For presidio, we're just mapping to an OS-specific font.
                        if (comboFontFamilyValue.equals(DEFAULT_OS_FONT) && fontFamilyStr.equals("Dialog")) {
                            selectedComboFont = comboFontFamilyValue;
                            break;
                        }
                    }

                    if (selectedComboFont != null) {
                        fontFamilyComboBox.setValue(selectedComboFont);
                    }
                }
            }
        }

        fontSizeComboBox.setDisable(!isCommandEnabled(FONT_SIZE_COMMAND));
        String fontSizeValue = getCommandValue(FONT_SIZE_COMMAND);

        // added test for fontSizeValue == null to combat RT-28847
        if (resetToolbarState && fontSizeValue == null) {
            fontSizeComboBox.setValue(sizeFontMap.get(SIZE_SMALL));
        } else {
            if (fontSizeValue != null) {
                if (!fontSizeComboBox.getValue().equals(sizeFontMap.get(fontSizeValue))) {
                    fontSizeComboBox.setValue(sizeFontMap.get(fontSizeValue));
                }
            }
            else {
                /*
                ** these is no font size set in webview,
                ** let's just use the default....
                */
                if (!fontSizeComboBox.getValue().equals(sizeFontMap.get(SIZE_SMALL))) {
                    fontSizeComboBox.setValue(sizeFontMap.get(SIZE_SMALL));
                }
            }
        }

        boldButton.setDisable(!isCommandEnabled(BOLD_COMMAND));
        boldButton.setSelected(getCommandState(BOLD_COMMAND));
        italicButton.setDisable(!isCommandEnabled(ITALIC_COMMAND));
        italicButton.setSelected(getCommandState(ITALIC_COMMAND));
        underlineButton.setDisable(!isCommandEnabled(UNDERLINE_COMMAND));
        underlineButton.setSelected(getCommandState(UNDERLINE_COMMAND));
        strikethroughButton.setDisable(!isCommandEnabled(STRIKETHROUGH_COMMAND));
        strikethroughButton.setSelected(getCommandState(STRIKETHROUGH_COMMAND));

        fgColorButton.setDisable(!isCommandEnabled(FOREGROUND_COLOR_COMMAND));
        String foregroundColorValue = getCommandValue(FOREGROUND_COLOR_COMMAND);
        if (foregroundColorValue != null) {
            Color c = Color.web(rgbToHex((String)foregroundColorValue));
            fgColorButton.setValue(c);
        }

        bgColorButton.setDisable(!isCommandEnabled(BACKGROUND_COLOR_COMMAND));
        String backgroundColorValue = getCommandValue(BACKGROUND_COLOR_COMMAND);
        if (backgroundColorValue != null) {
            Color c = Color.web(rgbToHex((String)backgroundColorValue));
            bgColorButton.setValue(c);
        }
    }

    private void enableToolbar(final boolean enable) {
        Platform.runLater(new Runnable() {
            @Override public void run() {

                // Make sure buttons have been created to avoid NPE
                if (copyButton == null) return;

                /*
                ** if we're to enable, we still only enable
                ** the cut/copy/paste buttons that make sense
                */
                if (enable) {
                    copyButton.setDisable(!isCommandEnabled(COPY_COMMAND));
                    cutButton.setDisable(!isCommandEnabled(CUT_COMMAND));
                    pasteButton.setDisable(!isCommandEnabled(PASTE_COMMAND));
                }
                else {
                    copyButton.setDisable(true);
                    cutButton.setDisable(true);
                    pasteButton.setDisable(true);
                }

//                undoButton.setDisable(!enable);
//                redoButton.setDisable(!enable);
                insertHorizontalRuleButton.setDisable(!enable);
                alignLeftButton.setDisable(!enable);
                alignCenterButton.setDisable(!enable);
                alignRightButton.setDisable(!enable);
                alignJustifyButton.setDisable(!enable);
                bulletsButton.setDisable(!enable);
                numbersButton.setDisable(!enable);
                indentButton.setDisable(!enable);
                outdentButton.setDisable(!enable);
                formatComboBox.setDisable(!enable);
                fontFamilyComboBox.setDisable(!enable);
                fontSizeComboBox.setDisable(!enable);
                boldButton.setDisable(!enable);
                italicButton.setDisable(!enable);
                underlineButton.setDisable(!enable);
                strikethroughButton.setDisable(!enable);
                fgColorButton.setDisable(!enable);
                bgColorButton.setDisable(!enable);
            }
        });
    }

    private boolean executeCommand(String command, String value) {
        return webPage.executeCommand(command, value);
    }

    private boolean isCommandEnabled(String command) {
        return webPage.queryCommandEnabled(command);
    }
    
    private void setContentEditable(boolean b) {
        HTMLDocument htmlDocument = (HTMLDocument)webPage.getDocument(webPage.getMainFrame());
        HTMLElement htmlDocumentElement = (HTMLElement)htmlDocument.getDocumentElement();
        HTMLElement htmlBodyElement = (HTMLElement)htmlDocumentElement.getElementsByTagName("body").item(0);
        htmlBodyElement.setAttribute("contenteditable", Boolean.toString(b));
    }

    private boolean getCommandState(String command) {
        return webPage.queryCommandState(command);
    }

    private String getCommandValue(String command) {
        return webPage.queryCommandValue(command);
    }

    private static String rgbToHex(String value) {
        if (value.startsWith("rgba")) {
            String[] components = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')')).split(",");
            value = String.format("#%02X%02X%02X%02X",
                Integer.parseInt(components[0].trim()),
                Integer.parseInt(components[1].trim()),
                Integer.parseInt(components[2].trim()),
                Integer.parseInt(components[3].trim()));
            // The default background color for WebView, according to the HTML
            // standard is rgba=#00000000 (black). The canvas background is expected
            // to be white.
            if ("#00000000".equals(value)) {
                return "#FFFFFFFF";
            }
        } else if (value.startsWith("rgb")) {
            String[] components = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')')).split(",");
            value = String.format("#%02X%02X%02X",
                Integer.parseInt(components[0].trim()),
                Integer.parseInt(components[1].trim()),
                Integer.parseInt(components[2].trim()));
        }

        return value;
    }

    private void applyTextFormatting() {
        if (getCommandState(BULLETS_COMMAND) || getCommandState(NUMBERS_COMMAND)) {
            return;
        }

        if (webPage.getClientCommittedTextLength() == 0) {
            String format = formatStyleMap.get(formatComboBox.getValue());
            String font   = fontFamilyComboBox.getValue().toString();

            executeCommand(FORMAT_COMMAND, format);
            executeCommand(FONT_FAMILY_COMMAND, font);
        }
    }
    
    public void keyboardShortcuts(final String name) {
        if ("bold".equals(name)) {
            boldButton.fire();
        } else if ("italic".equals(name)) {
            italicButton.setSelected(!italicButton.isSelected());
        } else if ("underline".equals(name)) {
            underlineButton.setSelected(!underlineButton.isSelected());
        }
    }

    @Override
    public void onTraverse(Node node, Bounds bounds) {
        cutButton.requestFocus();
    }
    
    private boolean isFirstRun = true;

    @Override
    protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        
        if (isFirstRun) {
            populateToolbars();
            isFirstRun = false;
        }
        super.layoutChildren(x,y,w,h);
        double toolbarWidth = Math.max(toolbar1.prefWidth(-1), toolbar2.prefWidth(-1));
        toolbar1.setMinWidth(toolbarWidth);
        toolbar1.setPrefWidth(toolbarWidth);
        toolbar2.setMinWidth(toolbarWidth);
        toolbar2.setPrefWidth(toolbarWidth);
    }

    private static final int FONT_FAMILY_MENUBUTTON_WIDTH = 150;
    private static final int FONT_FAMILY_MENU_WIDTH = 100;
    private static final int FONT_SIZE_MENUBUTTON_WIDTH = 80;

    public void print(PrinterJob job) {
        webView.getEngine().print(job);
    }
}
