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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import java.util.List;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.behavior.TextFieldBehavior;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.tk.FontMetrics;

/**
 * Text field skin.
 */
public class TextFieldSkin extends TextInputControlSkin<TextField, TextFieldBehavior> {
    /**
     * This group contains the text, caret, and selection rectangle.
     * It is clipped. The textNode, selectionHighlightPath, and
     * caret are each translated individually when horizontal
     * translation is needed to keep the caretPosition visible.
     */
    private Pane textGroup = new Pane();
    private Group handleGroup;

    /**
     * The clip, applied to the textGroup. This makes sure that any
     * text / selection wandering off the text box is clipped
     */
    private Rectangle clip = new Rectangle();
    /**
     * The node actually displaying the text. Note that it has the
     * ability to render both the normal fill as well as the highlight
     * fill, to perform hit testing, fetching of the selection
     * highlight, and other such duties.
     */
    private Text textNode = new Text();
    /**
     *
     * The node used for showing the prompt text.
     */
    private Text promptNode;
    /**
     * A path, provided by the textNode, which represents the area
     * which is selected. The path elements which make up the
     * selection must be updated whenever the selection changes. We
     * don't need to keep track of text changes because those will
     * force the selection to be updated.
     */
    private Path selectionHighlightPath = new Path();

    private Path characterBoundingPath = new Path();
    private ObservableBooleanValue usePromptText;
    private DoubleProperty textTranslateX = new SimpleDoubleProperty(this, "textTranslateX");
    private double caretWidth;
    private BooleanProperty forwardBias = new BooleanPropertyBase() {
        @Override public Object getBean() {
            return TextFieldSkin.this;
        }

        @Override public String getName() {
            return "forwardBias";
        }

        @Override protected void invalidated() {
            if (getWidth() > 0) {
                // RT-25479: Disable caret bias handling for now.
                // textNode.impl_caretBiasProperty().set(get());
                updateCaretOff();
            }
        }
    };

    /**
     * Function to translate the text control's "dot" into the caret
     * position in the Text node.  This is possibly only meaningful for
     * the PasswordBoxSkin where the echoChar could be more than one
     * character.
     */
    protected int translateCaretPosition(int cp) { return cp; }
    protected Point2D translateCaretPosition(Point2D p) { return p; }

    /**
     * Right edge of the text region sans padding
     */
    protected ObservableDoubleValue textRight;

    private double pressX, pressY; // For dragging handles on embedded

    /**
     * Create a new TextFieldSkin.
     * @param textField not null
     */
    public TextFieldSkin(final TextField textField) {
        this(textField, new TextFieldBehavior(textField));
    }

    public TextFieldSkin(final TextField textField, final TextFieldBehavior behavior) {
        super(textField, behavior);
        behavior.setTextFieldSkin(this);


        textField.caretPositionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (getWidth() > 0) {
                    textNode.impl_caretPositionProperty().set(translateCaretPosition(textField.getCaretPosition()));
                    if (!forwardBias.get()) {
                        forwardBias.set(true);
                    }
                    updateCaretOff();
                }
            }
        });

        textRight = new DoubleBinding() {
            { bind(textGroup.widthProperty()); }
            @Override protected double computeValue() {
                return textGroup.getWidth();
            }
        };

        // Once this was crucial for performance, not sure now.
        clip.setSmooth(false);
        clip.setX(0);
        clip.widthProperty().bind(textGroup.widthProperty());
        clip.heightProperty().bind(textGroup.heightProperty());

        // Add content
        textGroup.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        textGroup.setClip(clip);
        // Hack to defeat the fact that otherwise when the caret blinks the parent group
        // bounds are completely invalidated and therefore the dirty region is much
        // larger than necessary.
        textGroup.getChildren().addAll(selectionHighlightPath, textNode, new Group(caretPath));
        getChildren().add(textGroup);
        if (PlatformUtil.isEmbedded()) {
            handleGroup = new Group();
            handleGroup.getChildren().addAll(caretHandle, selectionHandle1, selectionHandle2);
            getChildren().add(handleGroup);
        }

        // Add text
        textNode.setManaged(false);
        textNode.getStyleClass().add("text");
        textNode.fontProperty().bind(font);

        textNode.layoutXProperty().bind(textTranslateX);
        textNode.textProperty().bind(new StringBinding() {
            { bind(textField.textProperty()); }
            @Override protected String computeValue() {
                String txt = maskText(textField.getText());
                return txt == null ? "" : txt;
            }
        });
        textNode.fillProperty().bind(textFill);
        textNode.impl_selectionFillProperty().bind(new ObjectBinding<Paint>() {
            { bind(highlightTextFill, textFill, textField.focusedProperty()); }
            @Override protected Paint computeValue() {
                return textField.isFocused() ? highlightTextFill.get() : textFill.get();
            }
        });
        // updated by listener on caretPosition to ensure order
        textNode.impl_caretPositionProperty().set(textField.getCaretPosition());
        textField.selectionProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                updateSelection();
            }
        });

        // Add selection
        selectionHighlightPath.setManaged(false);
        selectionHighlightPath.setStroke(null);
        selectionHighlightPath.layoutXProperty().bind(textTranslateX);
        selectionHighlightPath.visibleProperty().bind(textField.anchorProperty().isNotEqualTo(textField.caretPositionProperty()).and(textField.focusedProperty()));
        selectionHighlightPath.fillProperty().bind(highlightFill);
        textNode.impl_selectionShapeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                updateSelection();
            }
        });

        // Add caret
        caretPath.setManaged(false);
        caretPath.setStrokeWidth(1);
        caretPath.fillProperty().bind(textFill);
        caretPath.strokeProperty().bind(textFill);
        caretPath.visibleProperty().bind(caretVisible);
        caretPath.layoutXProperty().bind(textTranslateX);
        textNode.impl_caretShapeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                caretPath.getElements().setAll(textNode.impl_caretShapeProperty().get());
                caretWidth = Math.round(caretPath.getLayoutBounds().getWidth());
            }
        });

        // Be sure to get the control to request layout when the font changes,
        // since this will affect the pref height and pref width.
        font.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                // I do both so that any cached values for prefWidth/height are cleared.
                // The problem is that the skin is unmanaged and so calling request layout
                // doesn't walk up the tree all the way. I think....
                requestLayout();
                getSkinnable().requestLayout();
            }
        });

        registerChangeListener(textField.prefColumnCountProperty(), "prefColumnCount");
        if (textField.isFocused()) setCaretAnimating(true);

        textField.alignmentProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                if (getWidth() > 0) {
                    updateTextPos();
                    updateCaretOff();
                    requestLayout();
                }
            }
        });

        usePromptText = new BooleanBinding() {
            { bind(textField.textProperty(),
                   textField.promptTextProperty(),
                   promptTextFill); }
            @Override protected boolean computeValue() {
                String txt = textField.getText();
                String promptTxt = textField.getPromptText();
                return ((txt == null || txt.isEmpty()) &&
                        promptTxt != null && !promptTxt.isEmpty() &&
                        !promptTextFill.get().equals(Color.TRANSPARENT));
            }
        };

        promptTextFill.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                updateTextPos();
            }
        });

        textField.textProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                if (!getBehavior().isEditing()) {
                    // Text changed, but not by user action
                    updateTextPos();
                }
            }
        });

        if (usePromptText.get()) {
            createPromptNode();
        }

        usePromptText.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                createPromptNode();
                requestLayout();
            }
        });

        if (PlatformUtil.isEmbedded()) {
            selectionHandle1.setRotate(180);

            EventHandler<MouseEvent> handlePressHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    pressX = e.getX();
                    pressY = e.getY();
                    e.consume();
                }
            };

            caretHandle.setOnMousePressed(handlePressHandler);
            selectionHandle1.setOnMousePressed(handlePressHandler);
            selectionHandle2.setOnMousePressed(handlePressHandler);

            caretHandle.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    Point2D p = new Point2D(caretHandle.getLayoutX() + e.getX() + pressX - textNode.getLayoutX(),
                                            caretHandle.getLayoutY() + e.getY() - pressY - 6);
                    HitInfo hit = textNode.impl_hitTestChar(translateCaretPosition(p));
                    positionCaret(hit, false);
                    e.consume();
                }
            });

            selectionHandle1.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    TextField textField = getSkinnable();
                    Point2D tp = textNode.localToScene(0, 0);
                    Point2D p = new Point2D(e.getSceneX() - tp.getX() + 10/*??*/ - pressX + selectionHandle1.getWidth() / 2,
                                            e.getSceneY() - tp.getY() - pressY - 6);
                    HitInfo hit = textNode.impl_hitTestChar(translateCaretPosition(p));
                    int pos = hit.getCharIndex();
                    if (textField.getAnchor() < textField.getCaretPosition()) {
                        // Swap caret and anchor
                        textField.selectRange(textField.getCaretPosition(), textField.getAnchor());
                    }
                    if (pos >= 0) {
                        if (pos >= textField.getAnchor() - 1) {
                            hit.setCharIndex(Math.max(0, textField.getAnchor() - 1));
                        }
                        positionCaret(hit, true);
                    }
                    e.consume();
                }
            });

            selectionHandle2.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    TextField textField = getSkinnable();
                    Point2D tp = textNode.localToScene(0, 0);
                    Point2D p = new Point2D(e.getSceneX() - tp.getX() + 10/*??*/ - pressX + selectionHandle2.getWidth() / 2,
                                            e.getSceneY() - tp.getY() - pressY - 6);
                    HitInfo hit = textNode.impl_hitTestChar(translateCaretPosition(p));
                    int pos = hit.getCharIndex();
                    if (textField.getAnchor() > textField.getCaretPosition()) {
                        // Swap caret and anchor
                        textField.selectRange(textField.getCaretPosition(), textField.getAnchor());
                    }
                    if (pos > 0) {
                        if (pos <= textField.getAnchor()) {
                            hit.setCharIndex(Math.min(textField.getAnchor() + 1, textField.getLength()));
                        }
                        positionCaret(hit, true);
                    }
                    e.consume();
                }
            });
        }
    }

    private void createPromptNode() {
        if (promptNode != null || !usePromptText.get()) return;

        promptNode = new Text();
        textGroup.getChildren().add(0, promptNode);
        promptNode.setManaged(false);
        promptNode.getStyleClass().add("text");
        promptNode.visibleProperty().bind(usePromptText);
        promptNode.fontProperty().bind(font);

        promptNode.textProperty().bind(getSkinnable().promptTextProperty());
        promptNode.fillProperty().bind(promptTextFill);
        updateSelection();
    }

    private void updateSelection() {
        IndexRange newValue = getSkinnable().getSelection();
        if (newValue == null || newValue.getLength() == 0) {
            textNode.impl_selectionStartProperty().set(-1);
            textNode.impl_selectionEndProperty().set(-1);
        } else {
            textNode.impl_selectionStartProperty().set(newValue.getStart());
            // This intermediate value is needed to force selection shape layout.
            textNode.impl_selectionEndProperty().set(newValue.getStart());
            textNode.impl_selectionEndProperty().set(newValue.getEnd());
        }

        PathElement[] elements = textNode.impl_selectionShapeProperty().get();
        if (elements == null) {
            selectionHighlightPath.getElements().clear();
        } else {
            selectionHighlightPath.getElements().setAll(elements);
        }

        if (PlatformUtil.isEmbedded() && newValue != null && newValue.getLength() > 0) {
            Bounds b = selectionHighlightPath.getBoundsInParent();
            if (isRTL()) {
                selectionHandle1.setLayoutX(textGroup.getWidth() - b.getMaxX() - selectionHandle2.getWidth() / 2);
                selectionHandle2.setLayoutX(textGroup.getWidth() - b.getMinX() - selectionHandle1.getWidth() / 2);
            } else {
                selectionHandle1.setLayoutX(b.getMinX() - selectionHandle1.getWidth() / 2);
                selectionHandle2.setLayoutX(b.getMaxX() - selectionHandle2.getWidth() / 2);
            }
        }
    }

    @Override protected void handleControlPropertyChanged(String propertyReference) {
        if ("prefColumnCount".equals(propertyReference)) {
            requestLayout();
            getSkinnable().requestLayout();
        } else {
            super.handleControlPropertyChanged(propertyReference);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        TextField textField = getSkinnable();

        double characterWidth = fontMetrics.get().computeStringWidth("W");

        int columnCount = textField.getPrefColumnCount();
        Insets padding = getInsets();

        return columnCount * characterWidth + (padding.getLeft() + padding.getRight());
    }

    @Override
    protected double computePrefHeight(double width) {
        double lineHeight = fontMetrics.get().getLineHeight();
        Insets padding = getInsets();

        return lineHeight + (padding.getTop() + padding.getBottom());
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }

    @Override
    public double getBaselineOffset() {
        FontMetrics fontMetrics = super.fontMetrics.get();
        return getInsets().getTop() + fontMetrics.getAscent();
    }

    private void updateTextPos() {
        switch (getHAlignment()) {
          case CENTER:
            double midPoint = textRight.get() / 2;
            if (usePromptText.get()) {
                promptNode.setLayoutX(midPoint - promptNode.getLayoutBounds().getWidth() / 2);
                textTranslateX.set(promptNode.getLayoutX());
            } else {
                textTranslateX.set(midPoint - textNode.getLayoutBounds().getWidth() / 2);
            }
            break;

          case RIGHT:
            textTranslateX.set(textRight.get() - textNode.getLayoutBounds().getWidth() -
                               caretWidth / 2);
            if (usePromptText.get()) {
                promptNode.setLayoutX(textRight.get() - promptNode.getLayoutBounds().getWidth() -
                                      caretWidth / 2);
            }
            break;

          case LEFT:
          default:
            textTranslateX.set(caretWidth / 2);
            if (usePromptText.get()) {
                promptNode.layoutXProperty().set(caretWidth / 2);
            }
        }
    }

    // should be called when the padding changes, or the text box width, or
    // the dot moves
    protected void updateCaretOff() {
        double delta = 0.0;
        double caretX = caretPath.getLayoutBounds().getMinX() + textTranslateX.get();
        // If the caret position is less than or equal to the left edge of the
        // clip then the caret will be clipped. We want the caret to end up
        // being positioned one pixel right of the clip's left edge. The same
        // applies on the right edge (but going the other direction of course).
        if (caretX < 0) {
            // I'll end up with a negative number
            delta = caretX;
        } else if (caretX > (textRight.get() - caretWidth)) {
            // I'll end up with a positive number
            delta = caretX - (textRight.get() - caretWidth);
        }

        // If delta is negative, then translate in the negative direction
        // to cause the text to scroll to the right. Vice-versa for positive.
        switch (getHAlignment()) {
          case CENTER:
            textTranslateX.set(textTranslateX.get() - delta);
            break;

          case RIGHT:
            textTranslateX.set(Math.max(textTranslateX.get() - delta,
                                        textRight.get() - textNode.getLayoutBounds().getWidth() -
                                        caretWidth / 2));
            break;

          case LEFT:
          default:
            textTranslateX.set(Math.min(textTranslateX.get() - delta,
                                        caretWidth / 2));
        }
        if (PlatformUtil.isEmbedded()) {
            if (isRTL()) {
                caretHandle.setLayoutX(textGroup.getWidth() - caretX - caretHandle.getWidth() / 2 - 1);
            } else {
                caretHandle.setLayoutX(caretX - caretHandle.getWidth() / 2 + 1);
            }
        }
    }

    /**
     * Use this implementation instead of the one provided on TextInputControl.
     * updateCaretOff would get called to position the caret, but the text needs
     * to be scrolled appropriately.
     */
    public void replaceText(int start, int end, String txt) {
        final double textMaxXOld = textNode.getBoundsInParent().getMaxX();
        final double caretMaxXOld = caretPath.getLayoutBounds().getMaxX() + textTranslateX.get();
        getSkinnable().replaceText(start, end, txt);
        scrollAfterDelete(textMaxXOld, caretMaxXOld);
    }

    /**
     * Use this implementation instead of the one provided on TextInputControl
     * Simply calls into TextInputControl.deletePrevious/NextChar and responds appropriately
     * based on the return value.
     */
    public void deleteChar(boolean previous) {
        final double textMaxXOld = textNode.getBoundsInParent().getMaxX();
        final double caretMaxXOld = caretPath.getLayoutBounds().getMaxX() + textTranslateX.get();
        final boolean shouldBeep = previous ?
                !getSkinnable().deletePreviousChar() :
                !getSkinnable().deleteNextChar();

        if (shouldBeep) {
//            beep();
        } else {
            scrollAfterDelete(textMaxXOld, caretMaxXOld);
        }
    }

    public void scrollAfterDelete(double textMaxXOld, double caretMaxXOld) {
        final Bounds textLayoutBounds = textNode.getLayoutBounds();
        final Bounds textBounds = textNode.localToParent(textLayoutBounds);
        final Bounds clipBounds = clip.getBoundsInParent();
        final Bounds caretBounds = caretPath.getLayoutBounds();

        switch (getHAlignment()) {
          case CENTER:
            updateTextPos();
            break;

          case RIGHT:
            if (textBounds.getMaxX() > clipBounds.getMaxX()) {
                double delta = caretMaxXOld - caretBounds.getMaxX() - textTranslateX.get();
                if (textBounds.getMaxX() + delta < clipBounds.getMaxX()) {
                    if (textMaxXOld <= clipBounds.getMaxX()) {
                        delta = textMaxXOld - textBounds.getMaxX();
                    } else {
                        delta = clipBounds.getMaxX() - textBounds.getMaxX();
                    }
                }
                textTranslateX.set(textTranslateX.get() + delta);
            } else {
                updateTextPos();
            }
            break;

          case LEFT:
          default:
            if (textBounds.getMinX() < clipBounds.getMinX() + caretWidth / 2 &&
                textBounds.getMaxX() <= clipBounds.getMaxX()) {
                double delta = caretMaxXOld - caretBounds.getMaxX() - textTranslateX.get();
                if (textBounds.getMaxX() + delta < clipBounds.getMaxX()) {
                    if (textMaxXOld <= clipBounds.getMaxX()) {
                        delta = textMaxXOld - textBounds.getMaxX();
                    } else {
                        delta = clipBounds.getMaxX() - textBounds.getMaxX();
                    }
                }
                textTranslateX.set(textTranslateX.get() + delta);
            }
        }

        updateCaretOff();
    }

    public HitInfo getIndex(MouseEvent e) {
        // adjust the event to be in the same coordinate space as the
        // text content of the textInputControl
        Insets insets = getSkinnable().getInsets();
        Point2D p;

        if (isRTL()) {
            p = new Point2D(insets.getLeft() + textGroup.getWidth() - e.getX() - textTranslateX.get(),
                            e.getY() - insets.getTop());
        } else {
            p = new Point2D(e.getX() - textTranslateX.get() - insets.getLeft(),
                            e.getY() - insets.getTop());
        }
        return textNode.impl_hitTestChar(translateCaretPosition(p));
    }

    public void setForwardBias(boolean isLeading) {
        forwardBias.set(isLeading);
    }

    public void positionCaret(HitInfo hit, boolean select) {
//         int pos = hit.getCharIndex();
        int pos = hit.getInsertionIndex();
        boolean isNewLine =
               (pos > 0 &&
                pos < getSkinnable().getLength() &&
                maskText(getSkinnable().getText()).codePointAt(pos-1) == 0x0a);

        // special handling for a new line
        if (!hit.isLeading() && isNewLine) {
            hit.setLeading(true);
            pos -= 1;
        }

        if (select) {
            getSkinnable().selectPositionCaret(pos);
        } else {
            getSkinnable().positionCaret(pos);
        }

        setForwardBias(hit.isLeading());
    }

    @Override public Rectangle2D getCharacterBounds(int index) {
        double x, y;
        double width, height;
        if (index == textNode.getText().length()) {
            Bounds textNodeBounds = textNode.getBoundsInLocal();
            x = textNodeBounds.getMaxX();
            y = 0;
            width = 0;
            height = textNodeBounds.getMaxY();
        } else {
            characterBoundingPath.getElements().clear();
            characterBoundingPath.getElements().addAll(textNode.impl_getRangeShape(index, index + 1));
            characterBoundingPath.setLayoutX(textNode.getLayoutX());
            characterBoundingPath.setLayoutY(textNode.getLayoutY());

            Bounds bounds = characterBoundingPath.getBoundsInLocal();

            x = bounds.getMinX();
            y = bounds.getMinY();
            // Sometimes the bounds is empty, in which case we must ignore the width/height
            width  = bounds.isEmpty() ? 0 : bounds.getWidth();
            height = bounds.isEmpty() ? 0 : bounds.getHeight();
        }

        Bounds textBounds = textGroup.getBoundsInParent();

        return new Rectangle2D(x + textBounds.getMinX(), y + textBounds.getMinY(),
                               width, height);
    }

    @Override protected PathElement[] getUnderlineShape(int start, int end) {
        return textNode.impl_getUnderlineShape(start, end);
    }

    @Override protected PathElement[] getRangeShape(int start, int end) {
        return textNode.impl_getRangeShape(start, end);
    }

    @Override protected void addHighlight(List<? extends Node> nodes, int start) {
        textGroup.getChildren().addAll(nodes);
    }

    @Override protected void removeHighlight(List<? extends Node> nodes) {
        textGroup.getChildren().removeAll(nodes);
    }

    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        super.layoutChildren(x, y, w, h);

        if (textNode != null) {
            double textY;
            FontMetrics fm = fontMetrics.get();
            switch (getSkinnable().getAlignment().getVpos()) {
              case TOP:
                textY = fm.getMaxAscent();
                break;

              case CENTER:
                textY = (fm.getMaxAscent() +
                         textGroup.getHeight() - fm.getMaxDescent()) / 2;
                break;

              case BOTTOM:
              default:
                textY = textGroup.getHeight() - fm.getMaxDescent();
            }
            textNode.setY(textY);
            if (promptNode != null) {
                promptNode.setY(textY);
            }

            if (getWidth() > 0) {
                updateTextPos();
                updateCaretOff();
            }
        }

        if (PlatformUtil.isEmbedded()) {
            handleGroup.setLayoutX(getSkinnable().getInsets().getLeft());
            handleGroup.setLayoutY(getSkinnable().getInsets().getTop());

            TextField textField = getSkinnable();

            // Resize handles for caret and anchor.
//            IndexRange selection = textField.getSelection();
            selectionHandle1.resize(selectionHandle1.prefWidth(-1),
                                    selectionHandle1.prefHeight(-1));
            selectionHandle2.resize(selectionHandle2.prefWidth(-1),
                                    selectionHandle2.prefHeight(-1));
            caretHandle.resize(caretHandle.prefWidth(-1),
                               caretHandle.prefHeight(-1));

            Bounds b = caretPath.getBoundsInParent();
            caretHandle.setLayoutY(b.getMaxY() - 1);
            //selectionHandle1.setLayoutY(b.getMaxY() - 1);
            selectionHandle1.setLayoutY(b.getMinY() - selectionHandle1.getHeight() + 1);
            selectionHandle2.setLayoutY(b.getMaxY() - 1);
        }
    }

    protected HPos getHAlignment() {
        HPos hPos = getSkinnable().getAlignment().getHpos();
        if (hPos != HPos.CENTER) {
            if (isRTL()) {
                hPos = (hPos == HPos.LEFT) ? HPos.RIGHT : HPos.LEFT;
            }
        }
        return hPos;
    }

    @Override public Point2D getMenuPosition() {
        Point2D p = super.getMenuPosition();
        if (p != null) {
            Insets padding = getInsets();
            p = new Point2D(Math.max(0, p.getX() - textNode.getLayoutX() - padding.getLeft() + textTranslateX.get()),
                            Math.max(0, p.getY() - textNode.getLayoutY() - padding.getTop()));
        }
        return p;
    }
}
