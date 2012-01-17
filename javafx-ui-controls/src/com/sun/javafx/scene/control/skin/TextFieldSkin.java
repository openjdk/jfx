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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import java.util.List;

import com.sun.javafx.Utils;
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
    public Group textGroup = new Group(); // TODO RICHARD make private again
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
     *
     * This node is also used for showing the prompt text. We just
     * switch the fill & content of the textNode depending on whether
     * there is any text specified on the TextInputControl
     */
    private Text textNode = new Text();
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
    private DoubleProperty textTranslateY = new SimpleDoubleProperty(this, "textTranslateY");
    private ObservableIntegerValue caretPosition;
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
                textNode.impl_caretBiasProperty().set(get());
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
     * Left edge of the text region sans padding
     */
    protected ObservableDoubleValue textLeft;

    /**
     * Right edge of the text region sans padding
     */
    protected ObservableDoubleValue textRight;

    @Override public boolean showContextMenu(ContextMenu menu, double x, double y, boolean isKeyboardTrigger) {
        if (isKeyboardTrigger) {
            Bounds caretBounds = caretPath.getLayoutBounds();
            Point2D p = Utils.pointRelativeTo(textNode, null, caretBounds.getMinX(),
                                              caretBounds.getMaxY(), false);
            x = p.getX();
            y = p.getY();
        }
        return super.showContextMenu(menu, x, y, isKeyboardTrigger);
    }

    /**
     * Create a new TextFieldSkin.
     * @param textField not null
     */
    public TextFieldSkin(final TextField textField) {
        super(textField, new TextFieldBehavior(textField));
        getBehavior().setTextFieldSkin(this);


        caretPosition = new IntegerBinding() {
            { bind(textField.caretPositionProperty()); }
            @Override protected int computeValue() {
                return textField.getCaretPosition();
            }
        };
        caretPosition.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (getWidth() > 0) {
                    textNode.impl_caretPositionProperty().set(translateCaretPosition(caretPosition.get()));
                    if (!forwardBias.get()) {
                        forwardBias.set(true);
                    }
                    updateCaretOff();
                }
            }
        });
        textLeft = new DoubleBinding() {
            { bind(insets()); }
            @Override protected double computeValue() {
                return getInsets().getLeft();
            }
        };
        ChangeListener<Number> leftRightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (getWidth() > 0) {
                    updateTextPos();
                    updateCaretOff();
                }
            }
        };
        textLeft.addListener(leftRightListener);
        textRight = new DoubleBinding() {
            { bind(widthProperty(), insets()); }
            @Override protected double computeValue() {
                return getWidth() - getInsets().getRight();
            }
        };
        textRight.addListener(leftRightListener);

        // Pretty much everything here is getting managed set to false,
        // because we want to control when layout is triggered
        setManaged(false);

        // Once this was crucial for performance, not sure now.
        clip.setSmooth(false);
        clip.xProperty().bind(new DoubleBinding() {
            { bind(insets()); }
            @Override protected double computeValue() {
                return getInsets().getLeft();
            }
        });
        clip.yProperty().bind(new DoubleBinding() {
            { bind(insets()); }
            @Override protected double computeValue() {
                return getInsets().getTop();
            }
        });
        clip.widthProperty().bind(new DoubleBinding() {
            { bind(widthProperty(), insets()); }
            @Override protected double computeValue() {
                return getWidth() - getInsets().getRight() - getInsets().getLeft();
            }
        });
        clip.heightProperty().bind(new DoubleBinding() {
            { bind(heightProperty(), insets()); }
            @Override protected double computeValue() {
                return getHeight() - getInsets().getTop() - getInsets().getBottom();
            }
        });

        // Add content
        textGroup.setManaged(false);
        textGroup.setClip(clip);
        textGroup.getChildren().addAll(selectionHighlightPath, textNode, caretPath);
        getChildren().add(textGroup);

        // Add text
        textNode.setManaged(false);
        textNode.fontProperty().bind(font);
        textNode.xProperty().bind(textLeft);
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
        selectionHighlightPath.layoutYProperty().bind(textTranslateY);
        selectionHighlightPath.visibleProperty().bind(textField.anchorProperty().isNotEqualTo(caretPosition).and(textField.focusedProperty()));
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
        caretPath.layoutYProperty().bind(textTranslateY);
        textNode.impl_caretShapeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                caretPath.getElements().setAll(textNode.impl_caretShapeProperty().get());
                caretWidth = caretPath.getLayoutBounds().getWidth();
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
            { bind(textField.textProperty(), textField.promptTextProperty()); }
            @Override protected boolean computeValue() {
                String txt = textField.getText();
                String promptTxt = textField.getPromptText();
                return ((txt == null || txt.isEmpty()) &&
                        promptTxt != null && !promptTxt.isEmpty());
            }
        };

        if (usePromptText.get()) {
            createPromptNode();
        }

        usePromptText.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                createPromptNode();
            }
        });
    }

    private void createPromptNode() {
        if (promptNode != null || !usePromptText.get()) return;

        promptNode = new Text();
        textGroup.getChildren().add(0, promptNode);
        promptNode.setManaged(false);
        promptNode.visibleProperty().bind(usePromptText);
        promptNode.fontProperty().bind(font);
        promptNode.xProperty().bind(textLeft);
        promptNode.layoutXProperty().set(0.0);
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

    @Override
    public double getBaselineOffset() {
        FontMetrics fontMetrics = super.fontMetrics.get();       
        return getInsets().getTop() + fontMetrics.getAscent();
    }

    private void updateTextPos() {
        switch (getSkinnable().getAlignment().getHpos()) {
          case CENTER:
            double midPoint = (textRight.get() - textLeft.get()) / 2;
            textTranslateX.set(midPoint - textNode.getLayoutBounds().getWidth() / 2);
            break;

          case RIGHT:
            textTranslateX.set(textRight.get() - textNode.getLayoutBounds().getWidth() -
                               caretWidth / 2 - 5);
            break;

          case LEFT:
          default:
            textTranslateX.set(caretWidth / 2);
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
        if (caretX < textLeft.get()) {
            // I'll end up with a negative number
            delta = caretX - textLeft.get();
        } else if (caretX > (textRight.get() - caretWidth)) {
            // I'll end up with a positive number
            delta = caretX - (textRight.get() - caretWidth);
        }

        // If delta is negative, then translate in the negative direction
        // to cause the text to scroll to the right. Vice-versa for positive.
        switch (getSkinnable().getAlignment().getHpos()) {
          case CENTER:
            textTranslateX.set(textTranslateX.get() - delta);
            break;

          case RIGHT:
            textTranslateX.set(Math.max(textTranslateX.get() - delta,
                                        textRight.get() - textNode.getLayoutBounds().getWidth() -
                                        caretWidth / 2 - 5));
            break;

          case LEFT:
          default:
            textTranslateX.set(Math.min(textTranslateX.get() - delta,
                                        caretWidth / 2));
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

        switch (getSkinnable().getAlignment().getHpos()) {
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
            if (textBounds.getMinX() < clipBounds.getMinX() + caretWidth / 2) {
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
        Point2D p = new Point2D(e.getX() - textNode.getLayoutX(), e.getY() - textTranslateY.get());
        return textNode.impl_hitTestChar(translateCaretPosition(p));
    }

    public void setForwardBias(boolean isLeading) {
        forwardBias.set(isLeading);
    }

    public void positionCaret(HitInfo hit, boolean select) {
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
            width = bounds.getWidth();
            height = bounds.getHeight();
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

    @Override protected void layoutChildren() {
        super.layoutChildren();

        if (textNode != null) {
            double textY;
            Insets insets = getInsets();
            switch (getSkinnable().getAlignment().getVpos()) {
              case TOP:
                textY = insets.getTop() + fontMetrics.get().getMaxAscent();
                break;

              case CENTER:
                textY = (getHeight() - insets.getBottom() - insets.getTop()
                         + fontMetrics.get().getLineHeight()) / 2;
                break;

              case BOTTOM:
              default:
                textY = getHeight() - insets.getBottom() - fontMetrics.get().getMaxDescent();
            }
            textNode.setY(textY);
            if (promptNode != null) {
                promptNode.setY(textY);
            }
        }
    }
}
