/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.scene.control.behavior.TextInputControlBehavior;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

import static com.sun.javafx.PlatformUtil.*;
import static com.sun.javafx.scene.control.skin.resources.ControlResources.*;

/**
 * Abstract base class for text input skins.
 */
public abstract class TextInputControlSkin<T extends TextInputControl, B extends TextInputControlBehavior<T>> extends SkinBase<T, B> {

    private static final boolean macOS = isMac();
    /**
     * The font to use with this control. In 1.3 and prior we had a font property
     * on the TextInputControl itself, however now we just do it via CSS
     */
    protected final ObjectProperty<Font> font = new StyleableObjectProperty<Font>(Font.getDefault()) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "font";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.FONT;
        }
    };
    protected final ObservableObjectValue<FontMetrics> fontMetrics = new ObjectBinding<FontMetrics>() {
        { bind(font); }
        @Override protected FontMetrics computeValue() {
            invalidateMetrics();
            return Toolkit.getToolkit().getFontLoader().getFontMetrics(font.get());
        }
    };

    /**
     * The fill to use for the text under normal conditions
     */
    protected final ObjectProperty<Paint> textFill = 
        new StyleableObjectProperty<Paint>(Color.BLACK) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "textFill";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.TEXT_FILL;
        }
    };
    protected final ObjectProperty<Paint> promptTextFill = 
        new StyleableObjectProperty<Paint>(Color.GRAY) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "promptTextFill";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.PROMPT_TEXT_FILL;
        }
    };
    /**
     * The fill to use for the text when highlighted.
     */
    protected final ObjectProperty<Paint> highlightFill = 
        new StyleableObjectProperty<Paint>(Color.DODGERBLUE) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "highlightFill";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.HIGHLIGHT_FILL;
        }
    };
    protected final ObjectProperty<Paint> highlightTextFill = 
        new StyleableObjectProperty<Paint>(Color.WHITE) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "highlightTextFill";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.HIGHLIGHT_TEXT_FILL;
        }
    };
    protected final BooleanProperty displayCaret = 
        new StyleableBooleanProperty(true) {

        @Override
        public Object getBean() {
            return TextInputControlSkin.this;
        }

        @Override
        public String getName() {
            return "displayCaret";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.DISPLAY_CARET;
        }
    };

    private BooleanProperty blink = new SimpleBooleanProperty(this, "blink", true);
    protected ObservableBooleanValue caretVisible;
    private Timeline caretTimeline = new Timeline();
    /**
     * A path, provided by the textNode, which represents the caret.
     * I assume this has to be updated whenever the caretPosition
     * changes. Perhaps more frequently (including text changes),
     * but I'm not sure.
     */
    protected final Path caretPath = new Path();

    protected StackPane caretHandle = null;
    protected StackPane selectionHandle1 = null;
    protected StackPane selectionHandle2 = null;

    public Point2D getMenuPosition() {
        if (isEmbedded()) {
            if (caretHandle.isVisible()) {
                return new Point2D(caretHandle.getLayoutX() + caretHandle.getWidth() / 2,
                                   caretHandle.getLayoutY());
            } else if (selectionHandle1.isVisible() && selectionHandle2.isVisible()) {
                return new Point2D((selectionHandle1.getLayoutX() + selectionHandle1.getWidth() / 2 +
                                    selectionHandle2.getLayoutX() + selectionHandle2.getWidth() / 2) / 2,
                                   selectionHandle2.getLayoutY() + selectionHandle2.getHeight() / 2);
            } else {
                return null;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }


    private static boolean useFXVK = isEmbedded();

    /* For testing only */
    static int vkType = -1;
    public void toggleUseVK() {
        vkType++;
        if (vkType < 4) {
            useFXVK = true;
            getSkinnable().getProperties().put(FXVK.VK_TYPE_PROP_KEY, FXVK.VK_TYPE_NAMES[vkType]);
            FXVK.attach(getSkinnable());
        } else {
            FXVK.detach();
            vkType = -1;
            useFXVK = false;
        }
    }


    public TextInputControlSkin(final T textInput, final B behavior) {
        super(textInput, behavior);

        caretTimeline.setCycleCount(Timeline.INDEFINITE);
        caretTimeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO, new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    blink.set(false);
                }
            }),
            new KeyFrame(Duration.seconds(.5), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    blink.set(true);
                }
            }),
            new KeyFrame(Duration.seconds(1))
        );

        /**
         * The caret is visible when the text box is focused AND when the selection
         * is empty. If the selection is non empty or the text box is not focused
         * then we don't want to show the caret. Also, we show the caret while
         * performing some operations such as most key strokes. In that case we
         * simply toggle its opacity.
         * <p>
         */
        caretVisible = new BooleanBinding() {
            { bind(textInput.focusedProperty(), textInput.anchorProperty(), textInput.caretPositionProperty(),
                    textInput.disabledProperty(), textInput.editableProperty(), displayCaret, blink);}
            @Override protected boolean computeValue() {
                // RT-10682: On Windows, we show the caret during selection, but on others we hide it
                return !blink.get() && displayCaret.get() && textInput.isFocused() &&
                        (isWindows() || (textInput.getCaretPosition() == textInput.getAnchor())) &&
                        !textInput.isDisabled() &&
                        textInput.isEditable();
            }
        };

        if (isEmbedded()) {
            caretHandle      = new StackPane();
            selectionHandle1 = new StackPane();
            selectionHandle2 = new StackPane();

            caretHandle.setManaged(false);
            selectionHandle1.setManaged(false);
            selectionHandle2.setManaged(false);

            caretHandle.visibleProperty().bind(new BooleanBinding() {
                { bind(textInput.focusedProperty(), textInput.anchorProperty(),
                       textInput.caretPositionProperty(), textInput.disabledProperty(),
                       textInput.editableProperty(), textInput.lengthProperty(), displayCaret);}
                @Override protected boolean computeValue() {
                    return (displayCaret.get() && textInput.isFocused() &&
                            textInput.getCaretPosition() == textInput.getAnchor() &&
                            !textInput.isDisabled() && textInput.isEditable() &&
                            textInput.getLength() > 0);
                }
            });


            selectionHandle1.visibleProperty().bind(new BooleanBinding() {
                { bind(textInput.focusedProperty(), textInput.anchorProperty(), textInput.caretPositionProperty(),
                       textInput.disabledProperty(), displayCaret);}
                @Override protected boolean computeValue() {
                    return (displayCaret.get() && textInput.isFocused() &&
                            textInput.getCaretPosition() != textInput.getAnchor() &&
                            !textInput.isDisabled());
                }
            });


            selectionHandle2.visibleProperty().bind(new BooleanBinding() {
                { bind(textInput.focusedProperty(), textInput.anchorProperty(), textInput.caretPositionProperty(),
                       textInput.disabledProperty(), displayCaret);}
                @Override protected boolean computeValue() {
                    return (displayCaret.get() && textInput.isFocused() &&
                            textInput.getCaretPosition() != textInput.getAnchor() &&
                            !textInput.isDisabled());
                }
            });


            caretHandle.getStyleClass().setAll("caret-handle");
            selectionHandle1.getStyleClass().setAll("selection-handle");
            selectionHandle2.getStyleClass().setAll("selection-handle");

            selectionHandle1.setId("selection-handle-1");
            selectionHandle2.setId("selection-handle-2");

            textInput.focusedProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    if (useFXVK) {
                        if (textInput.isEditable() && textInput.isFocused()) {
                            FXVK.attach(textInput);
                        } else if (getScene() == null ||
                                   getScene().getWindow() == null ||
                                   !getScene().getWindow().isFocused() ||
                                   !(getScene().getFocusOwner() instanceof TextInputControl &&
                                     ((TextInputControl)getScene().getFocusOwner()).isEditable())) {
                            FXVK.detach();
                        }
                    }
                }
            });
        }

        if (textInput.getOnInputMethodTextChanged() == null) {
            textInput.setOnInputMethodTextChanged(new EventHandler<InputMethodEvent>() {
                @Override public void handle(InputMethodEvent event) {
                    handleInputMethodEvent(event);
                }
            });
        }

        textInput.setInputMethodRequests(new InputMethodRequests() {
            @Override public Point2D getTextLocation(int offset) {
                Scene scene = getScene();
                Window window = scene.getWindow();
                Rectangle2D characterBounds = getCharacterBounds(imstart + offset);
                Point2D p = localToScene(characterBounds.getMinX(), characterBounds.getMaxY());
                // TODO: Find out where these offsets come from
                Point2D location = new Point2D(window.getX() + scene.getX() + p.getX() -  6,
                                               window.getY() + scene.getY() + p.getY() - 42);
                return location;
            }

            @Override
            public int getLocationOffset(int x, int y) {
                return getInsertionPoint(x, y);
            }

            @Override
            public void cancelLatestCommittedText() {
                // TODO
            }

            @Override
            public String getSelectedText() {
                TextInputControl textInput = getSkinnable();
                IndexRange selection = textInput.getSelection();

                return textInput.getText(selection.getStart(), selection.getEnd());
            }
        });
    }
    
    @Override public void dispose() {
        caretTimeline.stop();
        caretTimeline = null;
    }

    // For PasswordFieldSkin
    protected String maskText(String txt) {
        return txt;
    }

 
    /**
     * Returns the character at a given offset.
     *
     * @param index
     */
    public char getCharacter(int index) { return '\0'; }

    /**
     * Returns the insertion point for a given location.
     *
     * @param x
     * @param y
     */
    public int getInsertionPoint(double x, double y) { return 0; }

    /**
     * Returns the bounds of the character at a given index.
     *
     * @param index
     */
    public Rectangle2D getCharacterBounds(int index) { return null; }

    public double getLineHeight() {
        return fontMetrics.get().getLineHeight();
    }

    /**
     * Ensures that the character at a given index is visible.
     *
     * @param index
     */
    public void scrollCharacterToVisible(int index) {}

    protected void invalidateMetrics() {
    }

    protected void updateTextFill() {};
    protected void updateHighlightFill() {};
    protected void updateHighlightTextFill() {};

    // Start/Length of the text under input method composition
    private int imstart;
    private int imlength;
    // Holds concrete attributes for the composition runs
    private List<Shape> imattrs = new java.util.ArrayList<Shape>();

    protected void handleInputMethodEvent(InputMethodEvent event) {
        final TextInputControl textInput = getSkinnable();
        if (textInput.isEditable() && !textInput.textProperty().isBound() && !textInput.isDisabled()) {

            // remove previous input method text (if any) or selected text
            if (imlength != 0) {
                removeHighlight(imattrs);
                imattrs.clear();
                textInput.selectRange(imstart, imstart + imlength);
            }

            // Insert committed text
            if (event.getCommitted().length() != 0) {
                String committed = event.getCommitted();
                textInput.replaceText(textInput.getSelection(), committed);
            }

            // Insert composed text
            imstart = textInput.getSelection().getStart();
            StringBuilder composed = new StringBuilder();
            for (InputMethodTextRun run : event.getComposed()) {
                composed.append(run.getText());
            }
            imlength = composed.length();
            if (imlength != 0) {
                textInput.replaceText(textInput.getSelection(), composed.toString());
                int pos = imstart;
                for (InputMethodTextRun run : event.getComposed()) {
                    int endPos = pos + run.getText().length();
                    createInputMethodAttributes(run.getHighlight(), pos, endPos);
                    pos = endPos;
                }
                addHighlight(imattrs, imstart);

                // Set caret position in composed text
                int caretPos = event.getCaretPosition();
                if (caretPos >= 0 && caretPos < imlength) {
                    textInput.selectRange(imstart + caretPos, imstart + caretPos);
                }
            }
        }
    }

    protected abstract PathElement[] getUnderlineShape(int start, int end);
    protected abstract PathElement[] getRangeShape(int start, int end);
    protected abstract void addHighlight(List<? extends Node> nodes, int start);
    protected abstract void removeHighlight(List<? extends Node> nodes);

    private void createInputMethodAttributes(InputMethodHighlight highlight, int start, int end) {
        double minX = 0f;
        double maxX = 0f;
        double minY = 0f;
        double maxY = 0f;

        for (PathElement pe: getUnderlineShape(start, end)) {
            if (pe instanceof MoveTo) {
                minX = maxX = ((MoveTo)pe).getX();
                minY = maxY = ((MoveTo)pe).getY();
            } else if (pe instanceof LineTo) {
                minX = (minX < ((LineTo)pe).getX() ? minX : ((LineTo)pe).getX());
                maxX = (maxX > ((LineTo)pe).getX() ? maxX : ((LineTo)pe).getX());
                minY = (minY < ((LineTo)pe).getY() ? minY : ((LineTo)pe).getY());
                maxY = (maxY > ((LineTo)pe).getY() ? maxY : ((LineTo)pe).getY());
            } else if (pe instanceof HLineTo) {
                minX = (minX < ((HLineTo)pe).getX() ? minX : ((HLineTo)pe).getX());
                maxX = (maxX > ((HLineTo)pe).getX() ? maxX : ((HLineTo)pe).getX());
            } else if (pe instanceof VLineTo) {
                minY = (minY < ((VLineTo)pe).getY() ? minY : ((VLineTo)pe).getY());
                maxY = (maxY > ((VLineTo)pe).getY() ? maxY : ((VLineTo)pe).getY());
            } else if (pe instanceof ClosePath) {
                // Now, create the attribute.
                Shape attr = null;
                if (highlight == InputMethodHighlight.SELECTED_RAW) {
                    // blue background
                    attr = new Path();
                    ((Path)attr).getElements().addAll(getRangeShape(start, end));
                    attr.setFill(Color.BLUE);
                    attr.setOpacity(0.3f);
                } else if (highlight == InputMethodHighlight.UNSELECTED_RAW) {
                    // dash underline.
                    attr = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    attr.setStroke(textFill.get());
                    attr.setStrokeWidth(maxY - minY);
                    ObservableList<Double> dashArray = attr.getStrokeDashArray();
                    dashArray.add(Double.valueOf(2f));
                    dashArray.add(Double.valueOf(2f));
                } else if (highlight == InputMethodHighlight.SELECTED_CONVERTED) {
                    // thick underline.
                    attr = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    attr.setStroke(textFill.get());
                    attr.setStrokeWidth((maxY - minY) * 3);
                } else if (highlight == InputMethodHighlight.UNSELECTED_CONVERTED) {
                    // single underline.
                    attr = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    attr.setStroke(textFill.get());
                    attr.setStrokeWidth(maxY - minY);
                }
                attr.setManaged(false);
                attr.setSmooth(false);
                imattrs.add(attr);
            }
        }
    }

    public void setCaretAnimating(boolean value) {
        if (value) {
            caretTimeline.play();
        } else {
            caretTimeline.stop();
            blink.set(true);
        }
    }

    class ContextMenuItem extends MenuItem {
        ContextMenuItem(final String action) {
            super(getString("TextInputControl.menu." + action));
            setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    getBehavior().callAction(action);
                }
            });
        }
    }

    final MenuItem undoMI   = new ContextMenuItem("Undo");
    final MenuItem redoMI   = new ContextMenuItem("Redo");
    final MenuItem cutMI    = new ContextMenuItem("Cut");
    final MenuItem copyMI   = new ContextMenuItem("Copy");
    final MenuItem pasteMI  = new ContextMenuItem("Paste");
    final MenuItem deleteMI = new ContextMenuItem("DeleteSelection");
    final MenuItem selectWordMI = new ContextMenuItem("SelectWord");
    final MenuItem selectAllMI = new ContextMenuItem("SelectAll");
    final MenuItem separatorMI = new SeparatorMenuItem();

    public void populateContextMenu(ContextMenu contextMenu) {
        TextInputControl textInputControl = getSkinnable();
        boolean editable = textInputControl.isEditable();
        boolean hasText = (textInputControl.getLength() > 0);
        boolean hasSelection = (textInputControl.getSelection().getLength() > 0);
        boolean maskText = (maskText("A") != "A");
        ObservableList<MenuItem> items = contextMenu.getItems();

        if (isEmbedded()) {
            items.clear();
            if (!maskText && hasSelection) {
                if (editable) {
                    items.add(cutMI);
                }
                items.add(copyMI);
            }
            if (editable && Clipboard.getSystemClipboard().hasString()) {
                items.add(pasteMI);
            }
            if (hasText) {
                if (!hasSelection) {
                    items.add(selectWordMI);
                }
                items.add(selectAllMI);
            }
            selectWordMI.getProperties().put("refreshMenu", Boolean.TRUE);
            selectAllMI.getProperties().put("refreshMenu", Boolean.TRUE);
        } else {
            if (editable) {
                items.setAll(undoMI, redoMI, cutMI, copyMI, pasteMI, deleteMI,
                             separatorMI, selectAllMI);
            } else {
                items.setAll(copyMI, separatorMI, selectAllMI);
            }
            undoMI.setDisable(!getBehavior().canUndo());
            redoMI.setDisable(!getBehavior().canRedo());
            cutMI.setDisable(maskText || !hasSelection);
            copyMI.setDisable(maskText || !hasSelection);
            pasteMI.setDisable(!Clipboard.getSystemClipboard().hasString());
            deleteMI.setDisable(!hasSelection);
        }
    }

    private static class StyleableProperties {
        private static final StyleableProperty<TextInputControlSkin,Font> FONT =
           new StyleableProperty.FONT<TextInputControlSkin>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.font == null || !n.font.isBound();
            }

            @Override
            public WritableValue<Font> getWritableValue(TextInputControlSkin n) {
                return n.font;
            }
        };
        
        private static final StyleableProperty<TextInputControlSkin,Paint> TEXT_FILL =
            new StyleableProperty<TextInputControlSkin,Paint>("-fx-text-fill",
                PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.textFill == null || !n.textFill.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(TextInputControlSkin n) {
                return n.textFill;
            }
        };
       
        private static final StyleableProperty<TextInputControlSkin,Paint> PROMPT_TEXT_FILL =
            new StyleableProperty<TextInputControlSkin,Paint>("-fx-prompt-text-fill",
                PaintConverter.getInstance(), Color.GRAY) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.promptTextFill == null || !n.promptTextFill.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(TextInputControlSkin n) {
                return n.promptTextFill;
            }
        };
        
        private static final StyleableProperty<TextInputControlSkin,Paint> HIGHLIGHT_FILL =
            new StyleableProperty<TextInputControlSkin,Paint>("-fx-highlight-fill",
                PaintConverter.getInstance(), Color.DODGERBLUE) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.highlightFill == null || !n.highlightFill.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(TextInputControlSkin n) {
                return n.highlightFill;
            }
        };
        
        private static final StyleableProperty<TextInputControlSkin,Paint> HIGHLIGHT_TEXT_FILL =
            new StyleableProperty<TextInputControlSkin,Paint>("-fx-highlight-text-fill",
                PaintConverter.getInstance(), Color.WHITE) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.highlightTextFill == null || !n.highlightTextFill.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(TextInputControlSkin n) {
                return n.highlightTextFill;
            }
        };
        
        private static final StyleableProperty<TextInputControlSkin,Boolean> DISPLAY_CARET =
            new StyleableProperty<TextInputControlSkin,Boolean>("-fx-display-caret",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(TextInputControlSkin n) {
                return n.displayCaret == null || !n.displayCaret.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(TextInputControlSkin n) {
                return n.displayCaret;
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            List<StyleableProperty> styleables = new ArrayList<StyleableProperty>(SkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FONT,
                TEXT_FILL,
                PROMPT_TEXT_FILL,
                HIGHLIGHT_FILL,
                HIGHLIGHT_TEXT_FILL,
                DISPLAY_CARET
            );

            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return StyleableProperties.STYLEABLES;
     }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

}
