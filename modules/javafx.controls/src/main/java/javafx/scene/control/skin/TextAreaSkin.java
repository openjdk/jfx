/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.TextAreaBehavior;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.geometry.VerticalDirection;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.HitInfo;
import javafx.util.Duration;

import java.util.List;

import static com.sun.javafx.PlatformUtil.isMac;
import static com.sun.javafx.PlatformUtil.isWindows;

/**
 * Default skin implementation for the {@link TextArea} control.
 *
 * @see TextArea
 * @since 9
 */
public class TextAreaSkin extends TextInputControlSkin<TextArea> {

    /* ************************************************************************
     *
     * Static fields
     *
     **************************************************************************/

    /** A shared helper object, used only by downLines(). */
    private static final Path tmpCaretPath = new Path();



    /* ************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    final private TextArea textArea;

    // *** NOTE: Multiple node mode is not yet fully implemented *** //
    private static final boolean USE_MULTIPLE_NODES = false;

    private final TextAreaBehavior behavior;

    private double computedMinWidth = Double.NEGATIVE_INFINITY;
    private double computedMinHeight = Double.NEGATIVE_INFINITY;
    private double computedPrefWidth = Double.NEGATIVE_INFINITY;
    private double computedPrefHeight = Double.NEGATIVE_INFINITY;
    private double widthForComputedPrefHeight = Double.NEGATIVE_INFINITY;
    private double characterWidth;
    private double lineHeight;

    private ContentView contentView = new ContentView();
    private Group paragraphNodes = new Group();

    private Text promptNode;
    private ObservableBooleanValue usePromptText;

    private ObservableIntegerValue caretPosition;
    private Group selectionHighlightGroup = new Group();

    private ScrollPane scrollPane;
    private Bounds oldViewportBounds;

    private VerticalDirection scrollDirection = null;

    private Path characterBoundingPath = new Path();

    private Timeline scrollSelectionTimeline = new Timeline();
    private EventHandler<ActionEvent> scrollSelectionHandler = event -> {
        switch (scrollDirection) {
            case UP: {
                // TODO Get previous offset
                break;
            }

            case DOWN: {
                // TODO Get next offset
                break;
            }
        }
    };

    private double pressX, pressY; // For dragging handles on embedded
    private boolean handlePressed;

    /**
     * Remembers horizontal position when traversing up / down.
     */
    double targetCaretX = -1;



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a new TextAreaSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TextAreaSkin(final TextArea control) {
        super(control);

        // install default input map for the text area control
        this.behavior = new TextAreaBehavior(control);
        this.behavior.setTextAreaSkin(this);
//        control.setInputMap(behavior.getInputMap());

        this.textArea = control;

        caretPosition = new IntegerBinding() {
            { bind(control.caretPositionProperty()); }
            @Override protected int computeValue() {
                return control.getCaretPosition();
            }
        };
        caretPosition.addListener((observable, oldValue, newValue) -> {
            targetCaretX = -1;
            if (control.getWidth() > 0) {
                setForwardBias(true);
            }
        });

        forwardBiasProperty().addListener(observable -> {
            if (control.getWidth() > 0) {
                updateTextNodeCaretPos(control.getCaretPosition());
            }
        });

//        setManaged(false);

        // Initialize content
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(control.isWrapText());
        scrollPane.setContent(contentView);
        getChildren().add(scrollPane);

        getSkinnable().addEventFilter(ScrollEvent.ANY, event -> {
            if (event.isDirect() && handlePressed) {
                event.consume();
            }
        });

        // Add selection
        selectionHighlightGroup.setManaged(false);
        selectionHighlightGroup.setVisible(false);
        contentView.getChildren().add(selectionHighlightGroup);

        // Add content view
        paragraphNodes.setManaged(false);
        contentView.getChildren().add(paragraphNodes);

        // Add caret
        caretPath.setManaged(false);
        caretPath.setStrokeWidth(1);
        caretPath.fillProperty().bind(textFillProperty());
        caretPath.strokeProperty().bind(textFillProperty());
        // modifying visibility of the caret forces a layout-pass (RT-32373), so
        // instead we modify the opacity.
        caretPath.opacityProperty().bind(new DoubleBinding() {
            { bind(caretVisibleProperty()); }
            @Override protected double computeValue() {
                return caretVisibleProperty().get() ? 1.0 : 0.0;
            }
        });
        contentView.getChildren().add(caretPath);

        if (SHOW_HANDLES) {
            contentView.getChildren().addAll(caretHandle, selectionHandle1, selectionHandle2);
        }

        scrollPane.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            getSkinnable().setScrollLeft(newValue.doubleValue() * getScrollLeftMax());
        });

        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            getSkinnable().setScrollTop(newValue.doubleValue() * getScrollTopMax());
        });

        // Initialize the scroll selection timeline
        scrollSelectionTimeline.setCycleCount(Timeline.INDEFINITE);
        List<KeyFrame> scrollSelectionFrames = scrollSelectionTimeline.getKeyFrames();
        scrollSelectionFrames.clear();
        scrollSelectionFrames.add(new KeyFrame(Duration.millis(350), scrollSelectionHandler));

        // Add initial text content
        for (int i = 0, n = USE_MULTIPLE_NODES ? control.getParagraphs().size() : 1; i < n; i++) {
            CharSequence paragraph = (n == 1) ? control.textProperty().getValueSafe() : control.getParagraphs().get(i);
            addParagraphNode(i, paragraph.toString());
        }

        control.selectionProperty().addListener((observable, oldValue, newValue) -> {
            // TODO Why do we need two calls here?
            control.requestLayout();
            contentView.requestLayout();
        });

        control.wrapTextProperty().addListener((observable, oldValue, newValue) -> {
            invalidateMetrics();
            scrollPane.setFitToWidth(newValue);
        });

        control.prefColumnCountProperty().addListener((observable, oldValue, newValue) -> {
            invalidateMetrics();
            updatePrefViewportWidth();
        });

        control.prefRowCountProperty().addListener((observable, oldValue, newValue) -> {
            invalidateMetrics();
            updatePrefViewportHeight();
        });

        updateFontMetrics();
        fontMetrics.addListener(valueModel -> {
            updateFontMetrics();
        });

        contentView.paddingProperty().addListener(valueModel -> {
            updatePrefViewportWidth();
            updatePrefViewportHeight();
        });

        scrollPane.viewportBoundsProperty().addListener(valueModel -> {
            if (scrollPane.getViewportBounds() != null) {
                // ScrollPane creates a new Bounds instance for each
                // layout pass, so we need to check if the width/height
                // have really changed to avoid infinite layout requests.
                Bounds newViewportBounds = scrollPane.getViewportBounds();
                if (oldViewportBounds == null ||
                    oldViewportBounds.getWidth() != newViewportBounds.getWidth() ||
                    oldViewportBounds.getHeight() != newViewportBounds.getHeight()) {

                    invalidateMetrics();
                    oldViewportBounds = newViewportBounds;
                    contentView.requestLayout();
                }
            }
        });

        control.scrollTopProperty().addListener((observable, oldValue, newValue) -> {
            double vValue = (newValue.doubleValue() < getScrollTopMax())
                               ? (newValue.doubleValue() / getScrollTopMax()) : 1.0;
            scrollPane.setVvalue(vValue);
        });

        control.scrollLeftProperty().addListener((observable, oldValue, newValue) -> {
            double hValue = (newValue.doubleValue() < getScrollLeftMax())
                               ? (newValue.doubleValue() / getScrollLeftMax()) : 1.0;
            scrollPane.setHvalue(hValue);
        });

        if (USE_MULTIPLE_NODES) {
            control.getParagraphs().addListener((ListChangeListener.Change<? extends CharSequence> change) -> {
                while (change.next()) {
                    int from = change.getFrom();
                    int to = change.getTo();
                    List<? extends CharSequence> removed = change.getRemoved();
                    if (from < to) {

                        if (removed.isEmpty()) {
                            // This is an add
                            for (int i = from, n = to; i < n; i++) {
                                addParagraphNode(i, change.getList().get(i).toString());
                            }
                        } else {
                            // This is an update
                            for (int i = from, n = to; i < n; i++) {
                                Node node = paragraphNodes.getChildren().get(i);
                                Text paragraphNode = (Text) node;
                                paragraphNode.setText(change.getList().get(i).toString());
                            }
                        }
                    } else {
                        // This is a remove
                        paragraphNodes.getChildren().subList(from, from + removed.size()).clear();
                    }
                }
            });
        } else {
            control.textProperty().addListener(observable -> {
                invalidateMetrics();
                ((Text)paragraphNodes.getChildren().get(0)).setText(control.textProperty().getValueSafe());
                contentView.requestLayout();
            });
        }

        usePromptText = new BooleanBinding() {
            { bind(control.textProperty(), control.promptTextProperty()); }
            @Override protected boolean computeValue() {
                String txt = control.getText();
                String promptTxt = control.getPromptText();
                return ((txt == null || txt.isEmpty()) &&
                        promptTxt != null && !promptTxt.isEmpty());
            }
        };

        if (usePromptText.get()) {
            createPromptNode();
        }

        usePromptText.addListener(observable -> {
            createPromptNode();
            control.requestLayout();
        });

        updateHighlightFill();
        updatePrefViewportWidth();
        updatePrefViewportHeight();
        if (control.isFocused()) setCaretAnimating(true);

        if (SHOW_HANDLES) {
            selectionHandle1.setRotate(180);

            EventHandler<MouseEvent> handlePressHandler = e -> {
                pressX = e.getX();
                pressY = e.getY();
                handlePressed = true;
                e.consume();
            };

            EventHandler<MouseEvent> handleReleaseHandler = event -> {
                handlePressed = false;
            };

            caretHandle.setOnMousePressed(handlePressHandler);
            selectionHandle1.setOnMousePressed(handlePressHandler);
            selectionHandle2.setOnMousePressed(handlePressHandler);

            caretHandle.setOnMouseReleased(handleReleaseHandler);
            selectionHandle1.setOnMouseReleased(handleReleaseHandler);
            selectionHandle2.setOnMouseReleased(handleReleaseHandler);

            caretHandle.setOnMouseDragged(e -> {
                Text textNode = getTextNode();
                Point2D tp = textNode.localToScene(0, 0);
                Point2D p = new Point2D(e.getSceneX() - tp.getX() - pressX + caretHandle.getWidth() / 2,
                                        e.getSceneY() - tp.getY() - pressY - 6);
                HitInfo hit = textNode.hitTest(translateCaretPosition(p));
                positionCaret(hit, false);
                e.consume();
            });

            selectionHandle1.setOnMouseDragged(e -> {
                TextArea control1 = getSkinnable();
                Text textNode = getTextNode();
                Point2D tp = textNode.localToScene(0, 0);
                Point2D p = new Point2D(e.getSceneX() - tp.getX() - pressX + selectionHandle1.getWidth() / 2,
                                        e.getSceneY() - tp.getY() - pressY + selectionHandle1.getHeight() + 5);
                HitInfo hit = textNode.hitTest(translateCaretPosition(p));
                if (control1.getAnchor() < control1.getCaretPosition()) {
                    // Swap caret and anchor
                    control1.selectRange(control1.getCaretPosition(), control1.getAnchor());
                }
                int pos = hit.getCharIndex();
                if (pos > 0) {
                    if (pos >= control1.getAnchor()) {
                        pos = control1.getAnchor();
                    }
                }
                positionCaret(hit, true);
                e.consume();
            });

            selectionHandle2.setOnMouseDragged(e -> {
                TextArea control1 = getSkinnable();
                Text textNode = getTextNode();
                Point2D tp = textNode.localToScene(0, 0);
                Point2D p = new Point2D(e.getSceneX() - tp.getX() - pressX + selectionHandle2.getWidth() / 2,
                                        e.getSceneY() - tp.getY() - pressY - 6);
                HitInfo hit = textNode.hitTest(translateCaretPosition(p));
                if (control1.getAnchor() > control1.getCaretPosition()) {
                    // Swap caret and anchor
                    control1.selectRange(control1.getCaretPosition(), control1.getAnchor());
                }
                int pos = hit.getCharIndex();
                if (pos > 0) {
                    if (pos <= control1.getAnchor() + 1) {
                        pos = Math.min(control1.getAnchor() + 2, control1.getLength());
                    }
                    positionCaret(hit, true);
                }
                e.consume();
            });
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void invalidateMetrics() {
        computedMinWidth = Double.NEGATIVE_INFINITY;
        computedMinHeight = Double.NEGATIVE_INFINITY;
        computedPrefWidth = Double.NEGATIVE_INFINITY;
        computedPrefHeight = Double.NEGATIVE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        scrollPane.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
    }

    /** {@inheritDoc} */
    @Override protected void updateHighlightFill() {
        for (Node node : selectionHighlightGroup.getChildren()) {
            Path selectionHighlightPath = (Path)node;
            selectionHighlightPath.setFill(highlightFillProperty().get());
        }
    }

    // Public for behavior
    /**
     * Performs a hit test, mapping point to index in the content.
     *
     * @param x the x coordinate of the point.
     * @param y the y coordinate of the point.
     * @return a {@code HitInfo} object describing the index and forward bias.
     */
    public HitInfo getIndex(double x, double y) {
        // adjust the event to be in the same coordinate space as the
        // text content of the textInputControl
        Text textNode = getTextNode();
        Point2D p = new Point2D(x - textNode.getLayoutX(), y - getTextTranslateY());
        HitInfo hit = textNode.hitTest(translateCaretPosition(p));
        return hit;
    };

    /** {@inheritDoc} */
    @Override public void moveCaret(TextUnit unit, Direction dir, boolean select) {
        switch (unit) {
            case CHARACTER:
                switch (dir) {
                    case LEFT:
                    case RIGHT:
                        nextCharacterVisually(dir == Direction.RIGHT);
                        break;
                    default:
                        throw new IllegalArgumentException(""+dir);
                }
                break;

            case LINE:
                switch (dir) {
                    case UP:
                        previousLine(select);
                        break;
                    case DOWN:
                        nextLine(select);
                        break;
                    case BEGINNING:
                        lineStart(select, select && isMac());
                        break;
                    case END:
                        lineEnd(select, select && isMac());
                        break;
                    default:
                        throw new IllegalArgumentException(""+dir);
                }
                break;

            case PAGE:
                switch (dir) {
                    case UP:
                        previousPage(select);
                        break;
                    case DOWN:
                        nextPage(select);
                        break;
                    default:
                        throw new IllegalArgumentException(""+dir);
                }
                break;

            case PARAGRAPH:
                switch (dir) {
                    case UP:
                        paragraphStart(true, select);
                        break;
                    case DOWN:
                        paragraphEnd(true, select);
                        break;
                    case BEGINNING:
                        paragraphStart(false, select);
                        break;
                    case END:
                        paragraphEnd(false, select);
                        break;
                    default:
                        throw new IllegalArgumentException(""+dir);
                }
                break;

            default:
                throw new IllegalArgumentException(""+unit);
        }
    }

    private void nextCharacterVisually(boolean moveRight) {
        if (isRTL()) {
            // Text node is mirrored.
            moveRight = !moveRight;
        }

        Text textNode = getTextNode();
        Bounds caretBounds = caretPath.getLayoutBounds();
        if (caretPath.getElements().size() == 4) {
            // The caret is split
            // TODO: Find a better way to get the primary caret position
            // instead of depending on the internal implementation.
            // See RT-25465.
            caretBounds = new Path(caretPath.getElements().get(0), caretPath.getElements().get(1)).getLayoutBounds();
        }
        double hitX = moveRight ? caretBounds.getMaxX() : caretBounds.getMinX();
        double hitY = (caretBounds.getMinY() + caretBounds.getMaxY()) / 2;
        HitInfo hit = textNode.hitTest(new Point2D(hitX, hitY));
        boolean leading = hit.isLeading();
        Path charShape = new Path(textNode.rangeShape(hit.getCharIndex(), hit.getCharIndex() + 1));
        if ((moveRight && charShape.getLayoutBounds().getMaxX() > caretBounds.getMaxX()) ||
                (!moveRight && charShape.getLayoutBounds().getMinX() < caretBounds.getMinX())) {
            leading = !leading;
            positionCaret(hit.getInsertionIndex(), leading, false, false);
        } else {
            // We're at beginning or end of line. Try moving up / down.
            int dot = textArea.getCaretPosition();
            targetCaretX = moveRight ? 0 : Double.MAX_VALUE;
            // TODO: Use Bidi sniffing instead of assuming right means forward here?
            downLines(moveRight ? 1 : -1, false, false);
            targetCaretX = -1;
            if (dot == textArea.getCaretPosition()) {
                if (moveRight) {
                    textArea.forward();
                } else {
                    textArea.backward();
                }
            }
        }
    }

    private void downLines(int nLines, boolean select, boolean extendSelection) {
        Text textNode = getTextNode();
        Bounds caretBounds = caretPath.getLayoutBounds();

        // The middle y coordinate of the the line we want to go to.
        double targetLineMidY = (caretBounds.getMinY() + caretBounds.getMaxY()) / 2 + nLines * lineHeight;
        if (targetLineMidY < 0) {
            targetLineMidY = 0;
        }

        // The target x for the caret. This may have been set during a
        // previous call.
        double x = (targetCaretX >= 0) ? targetCaretX : (caretBounds.getMaxX());

        // Find a text position for the target x,y.
        HitInfo hit = textNode.hitTest(translateCaretPosition(new Point2D(x, targetLineMidY)));
        int pos = hit.getCharIndex();

        // Save the old pos temporarily while testing the new one.
        int oldPos = textNode.getCaretPosition();
        boolean oldBias = textNode.isCaretBias();
        textNode.setCaretBias(hit.isLeading());
        textNode.setCaretPosition(pos);
        tmpCaretPath.getElements().clear();
        tmpCaretPath.getElements().addAll(textNode.getCaretShape());
        tmpCaretPath.setLayoutX(textNode.getLayoutX());
        tmpCaretPath.setLayoutY(textNode.getLayoutY());
        Bounds tmpCaretBounds = tmpCaretPath.getLayoutBounds();
        // The y for the middle of the row we found.
        double foundLineMidY = (tmpCaretBounds.getMinY() + tmpCaretBounds.getMaxY()) / 2;
        textNode.setCaretBias(oldBias);
        textNode.setCaretPosition(oldPos);

        // Test if the found line is in the correct direction and move
        // the caret.
        if (nLines == 0 ||
                (nLines > 0 && foundLineMidY > caretBounds.getMaxY()) ||
                (nLines < 0 && foundLineMidY < caretBounds.getMinY())) {

            positionCaret(hit.getInsertionIndex(), hit.isLeading(), select, extendSelection);
            targetCaretX = x;
        }
    }

    private void previousLine(boolean select) {
        downLines(-1, select, false);
    }

    private void nextLine(boolean select) {
        downLines(1, select, false);
    }

    private void previousPage(boolean select) {
        downLines(-(int)(scrollPane.getViewportBounds().getHeight() / lineHeight),
                select, false);
    }

    private void nextPage(boolean select) {
        downLines((int)(scrollPane.getViewportBounds().getHeight() / lineHeight),
                select, false);
    }

    private void lineStart(boolean select, boolean extendSelection) {
        targetCaretX = 0;
        downLines(0, select, extendSelection);
        targetCaretX = -1;
    }

    private void lineEnd(boolean select, boolean extendSelection) {
        targetCaretX = Double.MAX_VALUE;
        downLines(0, select, extendSelection);
        targetCaretX = -1;
    }


    private void paragraphStart(boolean previousIfAtStart, boolean select) {
        TextArea textArea = getSkinnable();
        String text = textArea.textProperty().getValueSafe();
        int pos = textArea.getCaretPosition();

        if (pos > 0) {
            if (previousIfAtStart && text.codePointAt(pos-1) == 0x0a) {
                // We are at the beginning of a paragraph.
                // Back up to the previous paragraph.
                pos--;
            }
            // Back up to the beginning of this paragraph
            while (pos > 0 && text.codePointAt(pos-1) != 0x0a) {
                pos--;
            }
            if (select) {
                textArea.selectPositionCaret(pos);
            } else {
                textArea.positionCaret(pos);
                setForwardBias(true);
            }
        }
    }

    private void paragraphEnd(boolean goPastInitialNewline, boolean select) {
        TextArea textArea = getSkinnable();
        String text = textArea.textProperty().getValueSafe();
        int pos = textArea.getCaretPosition();
        int len = text.length();
        boolean wentPastInitialNewline = false;
        boolean goPastTrailingNewline = isWindows();

        if (pos < len) {
            if (goPastInitialNewline && text.codePointAt(pos) == 0x0a) {
                // We are at the end of a paragraph, start by moving to the
                // next paragraph.
                pos++;
                wentPastInitialNewline = true;
            }
            if (!(goPastTrailingNewline && wentPastInitialNewline)) {
                // Go to the end of this paragraph
                while (pos < len && text.codePointAt(pos) != 0x0a) {
                    pos++;
                }
                if (goPastTrailingNewline && pos < len) {
                    // We are at the end of a paragraph, finish by moving to
                    // the beginning of the next paragraph (Windows behavior).
                    pos++;
                }
            }
            if (select) {
                textArea.selectPositionCaret(pos);
            } else {
                textArea.positionCaret(pos);
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected PathElement[] getUnderlineShape(int start, int end) {
        int pStart = 0;
        for (Node node : paragraphNodes.getChildren()) {
            Text p = (Text)node;
            int pEnd = pStart + p.textProperty().getValueSafe().length();
            if (pEnd >= start) {
                return p.underlineShape(start - pStart, end - pStart);
            }
            pStart = pEnd + 1;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override protected PathElement[] getRangeShape(int start, int end) {
        int pStart = 0;
        for (Node node : paragraphNodes.getChildren()) {
            Text p = (Text)node;
            int pEnd = pStart + p.textProperty().getValueSafe().length();
            if (pEnd >= start) {
                return p.rangeShape(start - pStart, end - pStart);
            }
            pStart = pEnd + 1;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override protected void addHighlight(List<? extends Node> nodes, int start) {
        int pStart = 0;
        Text paragraphNode = null;
        for (Node node : paragraphNodes.getChildren()) {
            Text p = (Text)node;
            int pEnd = pStart + p.textProperty().getValueSafe().length();
            if (pEnd >= start) {
                paragraphNode = p;
                break;
            }
            pStart = pEnd + 1;
        }

        if (paragraphNode != null) {
            for (Node node : nodes) {
                node.setLayoutX(paragraphNode.getLayoutX());
                node.setLayoutY(paragraphNode.getLayoutY());
            }
        }
        contentView.getChildren().addAll(nodes);
    }

    /** {@inheritDoc} */
    @Override protected void removeHighlight(List<? extends Node> nodes) {
        contentView.getChildren().removeAll(nodes);
    }

    /** {@inheritDoc} */
    @Override public Point2D getMenuPosition() {
        contentView.layoutChildren();
        Point2D p = super.getMenuPosition();
        if (p != null) {
            p = new Point2D(Math.max(0, p.getX() - contentView.snappedLeftInset() - getSkinnable().getScrollLeft()),
                    Math.max(0, p.getY() - contentView.snappedTopInset() - getSkinnable().getScrollTop()));
        }
        return p;
    }

    // Public for FXVKSkin
    /**
     * Gets the {@code Bounds} of the caret of the skinned {@code TextArea}.
     * @return the {@code Bounds} of the caret shape, relative to the {@code TextArea}.
     */
    public Bounds getCaretBounds() {
        return getSkinnable().sceneToLocal(caretPath.localToScene(caretPath.getBoundsInLocal()));
    }

    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case LINE_FOR_OFFSET:
            case LINE_START:
            case LINE_END:
            case BOUNDS_FOR_RANGE:
            case OFFSET_AT_POINT:
                Text text = getTextNode();
                return text.queryAccessibleAttribute(attribute, parameters);
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }

        // TODO Unregister listeners on text editor, paragraph list
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        Text firstParagraph = (Text) paragraphNodes.getChildren().get(0);
        return Utils.getAscent(getSkinnable().getFont(), firstParagraph.getBoundsType())
                + contentView.snappedTopInset() + textArea.snappedTopInset();
    }

    private char getCharacter(int index) {
        int n = paragraphNodes.getChildren().size();

        int paragraphIndex = 0;
        int offset = index;

        String paragraph = null;
        while (paragraphIndex < n) {
            Text paragraphNode = (Text)paragraphNodes.getChildren().get(paragraphIndex);
            paragraph = paragraphNode.getText();
            int count = paragraph.length() + 1;

            if (offset < count) {
                break;
            }

            offset -= count;
            paragraphIndex++;
        }

        return offset == paragraph.length() ? '\n' : paragraph.charAt(offset);
    }

    /** {@inheritDoc} */
    @Override protected int getInsertionPoint(double x, double y) {
        TextArea textArea = getSkinnable();

        int n = paragraphNodes.getChildren().size();
        int index = -1;

        if (n > 0) {
            if (y < contentView.snappedTopInset()) {
                // Select the character at x in the first row
                Text paragraphNode = (Text)paragraphNodes.getChildren().get(0);
                index = getNextInsertionPoint(paragraphNode, x, -1, VerticalDirection.DOWN);
            } else if (y > contentView.snappedTopInset() + contentView.getHeight()) {
                // Select the character at x in the last row
                int lastParagraphIndex = n - 1;
                Text lastParagraphView = (Text)paragraphNodes.getChildren().get(lastParagraphIndex);

                index = getNextInsertionPoint(lastParagraphView, x, -1, VerticalDirection.UP)
                        + (textArea.getLength() - lastParagraphView.getText().length());
            } else {
                // Select the character at x in the row at y
                int paragraphOffset = 0;
                for (int i = 0; i < n; i++) {
                    Text paragraphNode = (Text)paragraphNodes.getChildren().get(i);

                    Bounds bounds = paragraphNode.getBoundsInLocal();
                    double paragraphViewY = paragraphNode.getLayoutY() + bounds.getMinY();
                    if (y >= paragraphViewY
                            && y < paragraphViewY + paragraphNode.getBoundsInLocal().getHeight()) {
                        index = getInsertionPoint(paragraphNode,
                                x - paragraphNode.getLayoutX(),
                                y - paragraphNode.getLayoutY()) + paragraphOffset;
                        break;
                    }

                    paragraphOffset += paragraphNode.getText().length() + 1;
                }
            }
        }

        return index;
    }

    // Public for behavior
    /**
     * Moves the caret to the specified position.
     *
     * @param hit the new position and forward bias of the caret.
     * @param select whether to extend selection to the new position.
     */
    public void positionCaret(HitInfo hit, boolean select) {
        positionCaret(hit.getInsertionIndex(), hit.isLeading(), select, false);
    }

    private void positionCaret(int pos, boolean leading, boolean select, boolean extendSelection) {
        boolean isNewLine =
                (pos > 0 &&
                        pos <= getSkinnable().getLength() &&
                        getSkinnable().getText().codePointAt(pos-1) == 0x0a);

        // special handling for a new line
        if (!leading && isNewLine) {
            leading = true;
            pos -= 1;
        }

        if (select) {
            if (extendSelection) {
                getSkinnable().extendSelection(pos);
            } else {
                getSkinnable().selectPositionCaret(pos);
            }
        } else {
            getSkinnable().positionCaret(pos);
        }

        setForwardBias(leading);
    }

    /** {@inheritDoc} */
    @Override public Rectangle2D getCharacterBounds(int index) {
        TextArea textArea = getSkinnable();

        int paragraphIndex = paragraphNodes.getChildren().size();
        int paragraphOffset = textArea.getLength() + 1;

        Text paragraphNode = null;
        do {
            paragraphNode = (Text)paragraphNodes.getChildren().get(--paragraphIndex);
            paragraphOffset -= paragraphNode.getText().length() + 1;
        } while (index < paragraphOffset);

        int characterIndex = index - paragraphOffset;
        boolean terminator = false;

        if (characterIndex == paragraphNode.getText().length()) {
            characterIndex--;
            terminator = true;
        }

        characterBoundingPath.getElements().clear();
        characterBoundingPath.getElements().addAll(paragraphNode.rangeShape(characterIndex, characterIndex + 1));
        characterBoundingPath.setLayoutX(paragraphNode.getLayoutX());
        characterBoundingPath.setLayoutY(paragraphNode.getLayoutY());

        Bounds bounds = characterBoundingPath.getBoundsInLocal();

        double x = bounds.getMinX() + paragraphNode.getLayoutX() - textArea.getScrollLeft();
        double y = bounds.getMinY() + paragraphNode.getLayoutY() - textArea.getScrollTop();

        // Sometimes the bounds is empty, in which case we must ignore the width/height
        double width = bounds.isEmpty() ? 0 : bounds.getWidth();
        double height = bounds.isEmpty() ? 0 : bounds.getHeight();

        if (terminator) {
            x += width;
            width = 0;
        }

        return new Rectangle2D(x, y, width, height);
    }

    /** {@inheritDoc} */
    @Override protected void scrollCharacterToVisible(final int index) {
        // TODO We queue a callback because when characters are added or
        // removed the bounds are not immediately updated; is this really
        // necessary?

        Platform.runLater(() -> {
            if (getSkinnable().getLength() == 0) {
                return;
            }
            Rectangle2D characterBounds = getCharacterBounds(index);
            scrollBoundsToVisible(characterBounds);
        });
    }



    /* ************************************************************************
     *
     * Private implementation
     *
     **************************************************************************/

    TextAreaBehavior getBehavior() {
        return behavior;
    }

    private void createPromptNode() {
        if (promptNode == null && usePromptText.get()) {
            promptNode = new Text();
            contentView.getChildren().add(0, promptNode);
            promptNode.setManaged(false);
            promptNode.getStyleClass().add("text");
            promptNode.visibleProperty().bind(usePromptText);
            promptNode.fontProperty().bind(getSkinnable().fontProperty());
            promptNode.textProperty().bind(getSkinnable().promptTextProperty());
            promptNode.fillProperty().bind(promptTextFillProperty());
        }
    }

    private void addParagraphNode(int i, String string) {
        final TextArea textArea = getSkinnable();
        Text paragraphNode = new Text(string);
        paragraphNode.setTextOrigin(VPos.TOP);
        paragraphNode.setManaged(false);
        paragraphNode.getStyleClass().add("text");
        paragraphNode.boundsTypeProperty().addListener((observable, oldValue, newValue) -> {
            invalidateMetrics();
            updateFontMetrics();
        });
        paragraphNodes.getChildren().add(i, paragraphNode);

        paragraphNode.fontProperty().bind(textArea.fontProperty());
        paragraphNode.fillProperty().bind(textFillProperty());
        paragraphNode.selectionFillProperty().bind(highlightTextFillProperty());
    }

    private double getScrollTopMax() {
        return Math.max(0, contentView.getHeight() - scrollPane.getViewportBounds().getHeight());
    }

    private double getScrollLeftMax() {
        return Math.max(0, contentView.getWidth() - scrollPane.getViewportBounds().getWidth());
    }

    private int getInsertionPoint(Text paragraphNode, double x, double y) {
        HitInfo hitInfo = paragraphNode.hitTest(new Point2D(x, y));
        return hitInfo.getInsertionIndex();
    }

    private int getNextInsertionPoint(Text paragraphNode, double x, int from,
        VerticalDirection scrollDirection) {
        // TODO
        return 0;
    }

    private void scrollCaretToVisible() {
        TextArea textArea = getSkinnable();
        Bounds bounds = caretPath.getLayoutBounds();
        double x = bounds.getMinX() - textArea.getScrollLeft();
        double y = bounds.getMinY() - textArea.getScrollTop();
        double w = bounds.getWidth();
        double h = bounds.getHeight();

        if (SHOW_HANDLES) {
            if (caretHandle.isVisible()) {
                h += caretHandle.getHeight();
            } else if (selectionHandle1.isVisible() && selectionHandle2.isVisible()) {
                x -= selectionHandle1.getWidth() / 2;
                y -= selectionHandle1.getHeight();
                w += selectionHandle1.getWidth() / 2 + selectionHandle2.getWidth() / 2;
                h += selectionHandle1.getHeight() + selectionHandle2.getHeight();
            }
        }

        if (w > 0 && h > 0) {
            scrollBoundsToVisible(new Rectangle2D(x, y, w, h));
        }
    }

    private void scrollBoundsToVisible(Rectangle2D bounds) {
        TextArea textArea = getSkinnable();
        Bounds viewportBounds = scrollPane.getViewportBounds();

        double viewportWidth = viewportBounds.getWidth();
        double viewportHeight = viewportBounds.getHeight();
        double scrollTop = textArea.getScrollTop();
        double scrollLeft = textArea.getScrollLeft();
        double slop = 6.0;

        if (bounds.getMinY() < 0) {
            double y = scrollTop + bounds.getMinY();
            if (y <= contentView.snappedTopInset()) {
                y = 0;
            }
            textArea.setScrollTop(y);
        } else if (contentView.snappedTopInset() + bounds.getMaxY() > viewportHeight) {
            double y = scrollTop + contentView.snappedTopInset() + bounds.getMaxY() - viewportHeight;
            if (y >= getScrollTopMax() - contentView.snappedBottomInset()) {
                y = getScrollTopMax();
            }
            textArea.setScrollTop(y);
        }


        if (bounds.getMinX() < 0) {
            double x = scrollLeft + bounds.getMinX() - slop;
            if (x <= contentView.snappedLeftInset() + slop) {
                x = 0;
            }
            textArea.setScrollLeft(x);
        } else if (contentView.snappedLeftInset() + bounds.getMaxX() > viewportWidth) {
            double x = scrollLeft + contentView.snappedLeftInset() + bounds.getMaxX() - viewportWidth + slop;
            if (x >= getScrollLeftMax() - contentView.snappedRightInset() - slop) {
                x = getScrollLeftMax();
            }
            textArea.setScrollLeft(x);
        }
    }

    private void updatePrefViewportWidth() {
        int columnCount = getSkinnable().getPrefColumnCount();
        scrollPane.setPrefViewportWidth(columnCount * characterWidth + contentView.snappedLeftInset() + contentView.snappedRightInset());
        scrollPane.setMinViewportWidth(characterWidth + contentView.snappedLeftInset() + contentView.snappedRightInset());
    }

    private void updatePrefViewportHeight() {
        int rowCount = getSkinnable().getPrefRowCount();
        scrollPane.setPrefViewportHeight(rowCount * lineHeight + contentView.snappedTopInset() + contentView.snappedBottomInset());
        scrollPane.setMinViewportHeight(lineHeight + contentView.snappedTopInset() + contentView.snappedBottomInset());
    }

    private void updateFontMetrics() {
        Text firstParagraph = (Text)paragraphNodes.getChildren().get(0);
        lineHeight = Utils.getLineHeight(getSkinnable().getFont(), firstParagraph.getBoundsType());
        characterWidth = fontMetrics.get().getCharWidth('W');
    }

    private double getTextTranslateX() {
        return contentView.snappedLeftInset();
    }

    private double getTextTranslateY() {
        return contentView.snappedTopInset();
    }

    private double getTextLeft() {
        return 0;
    }

    private Point2D translateCaretPosition(Point2D p) {
        return p;
    }

    private Text getTextNode() {
        if (USE_MULTIPLE_NODES) {
            throw new IllegalArgumentException("Multiple node traversal is not yet implemented.");
        }
        return (Text)paragraphNodes.getChildren().get(0);
    }

    private void updateTextNodeCaretPos(int pos) {
        Text textNode = getTextNode();
        if (isForwardBias()) {
            textNode.setCaretPosition(pos);
        } else {
            textNode.setCaretPosition(pos - 1);
        }
        textNode.caretBiasProperty().set(isForwardBias());
    }



    /* ************************************************************************
     *
     * Support classes
     *
     **************************************************************************/

    private class ContentView extends Region {
        {
            getStyleClass().add("content");

            addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                behavior.mousePressed(event);
                event.consume();
            });

            addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                behavior.mouseReleased(event);
                event.consume();
            });

            addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
                behavior.mouseDragged(event);
                event.consume();
            });
        }

        @Override protected ObservableList<Node> getChildren() {
            return super.getChildren();
        }

        @Override public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override protected double computePrefWidth(double height) {
            if (computedPrefWidth < 0) {
                double prefWidth = 0;

                for (Node node : paragraphNodes.getChildren()) {
                    Text paragraphNode = (Text)node;
                    prefWidth = Math.max(prefWidth,
                            Utils.computeTextWidth(paragraphNode.getFont(),
                                    paragraphNode.getText(), 0));
                }

                prefWidth += snappedLeftInset() + snappedRightInset();

                Bounds viewPortBounds = scrollPane.getViewportBounds();
                computedPrefWidth = Math.max(prefWidth, (viewPortBounds != null) ? viewPortBounds.getWidth() : 0);
            }
            return computedPrefWidth;
        }

        @Override protected double computePrefHeight(double width) {
            if (width != widthForComputedPrefHeight) {
                invalidateMetrics();
                widthForComputedPrefHeight = width;
            }

            if (computedPrefHeight < 0) {
                double wrappingWidth;
                if (width == -1) {
                    wrappingWidth = 0;
                } else {
                    wrappingWidth = Math.max(width - (snappedLeftInset() + snappedRightInset()), 0);
                }

                double prefHeight = 0;

                for (Node node : paragraphNodes.getChildren()) {
                    Text paragraphNode = (Text)node;
                    prefHeight += Utils.computeTextHeight(
                            paragraphNode.getFont(),
                            paragraphNode.getText(),
                            wrappingWidth,
                            paragraphNode.getBoundsType());
                }

                prefHeight += snappedTopInset() + snappedBottomInset();

                Bounds viewPortBounds = scrollPane.getViewportBounds();
                computedPrefHeight = Math.max(prefHeight, (viewPortBounds != null) ? viewPortBounds.getHeight() : 0);
            }
            return computedPrefHeight;
        }

        @Override protected double computeMinWidth(double height) {
            if (computedMinWidth < 0) {
                double hInsets = snappedLeftInset() + snappedRightInset();
                computedMinWidth = Math.min(characterWidth + hInsets, computePrefWidth(height));
            }
            return computedMinWidth;
        }

        @Override protected double computeMinHeight(double width) {
            if (computedMinHeight < 0) {
                double vInsets = snappedTopInset() + snappedBottomInset();
                computedMinHeight = Math.min(lineHeight + vInsets, computePrefHeight(width));
            }
            return computedMinHeight;
        }

        @Override public void layoutChildren() {
            TextArea textArea = getSkinnable();
            double width = getWidth();

            // Lay out paragraphs
            final double topPadding = snappedTopInset();
            final double leftPadding = snappedLeftInset();

            double wrappingWidth = Math.max(width - (leftPadding + snappedRightInset()), 0);

            double y = topPadding;

            final List<Node> paragraphNodesChildren = paragraphNodes.getChildren();

            for (int i = 0; i < paragraphNodesChildren.size(); i++) {
                Node node = paragraphNodesChildren.get(i);
                Text paragraphNode = (Text)node;
                paragraphNode.setWrappingWidth(wrappingWidth);

                Bounds bounds = paragraphNode.getBoundsInLocal();
                paragraphNode.setLayoutX(leftPadding);
                paragraphNode.setLayoutY(y);

                y += bounds.getHeight();
            }

            if (promptNode != null) {
                promptNode.setLayoutX(leftPadding);
                promptNode.setLayoutY(topPadding + promptNode.getBaselineOffset());
                promptNode.setWrappingWidth(wrappingWidth);
            }

            // Update the selection
            IndexRange selection = textArea.getSelection();
            Bounds oldCaretBounds = caretPath.getBoundsInParent();

            selectionHighlightGroup.getChildren().clear();

            int caretPos = textArea.getCaretPosition();
            int anchorPos = textArea.getAnchor();

            if (SHOW_HANDLES) {
                // Install and resize the handles for caret and anchor.
                if (selection.getLength() > 0) {
                    selectionHandle1.resize(selectionHandle1.prefWidth(-1),
                            selectionHandle1.prefHeight(-1));
                    selectionHandle2.resize(selectionHandle2.prefWidth(-1),
                            selectionHandle2.prefHeight(-1));
                } else {
                    caretHandle.resize(caretHandle.prefWidth(-1),
                            caretHandle.prefHeight(-1));
                }

                // Position the handle for the anchor. This could be handle1 or handle2.
                // Do this before positioning the actual caret.
                if (selection.getLength() > 0) {
                    int paragraphIndex = paragraphNodesChildren.size();
                    int paragraphOffset = textArea.getLength() + 1;
                    Text paragraphNode = null;
                    do {
                        paragraphNode = (Text)paragraphNodesChildren.get(--paragraphIndex);
                        paragraphOffset -= paragraphNode.getText().length() + 1;
                    } while (anchorPos < paragraphOffset);

                    updateTextNodeCaretPos(anchorPos - paragraphOffset);
                    caretPath.getElements().clear();
                    caretPath.getElements().addAll(paragraphNode.getCaretShape());
                    caretPath.setLayoutX(paragraphNode.getLayoutX());
                    caretPath.setLayoutY(paragraphNode.getLayoutY());

                    Bounds b = caretPath.getBoundsInParent();
                    if (caretPos < anchorPos) {
                        selectionHandle2.setLayoutX(b.getMinX() - selectionHandle2.getWidth() / 2);
                        selectionHandle2.setLayoutY(b.getMaxY() - 1);
                    } else {
                        selectionHandle1.setLayoutX(b.getMinX() - selectionHandle1.getWidth() / 2);
                        selectionHandle1.setLayoutY(b.getMinY() - selectionHandle1.getHeight() + 1);
                    }
                }
            }

            {
                // Position caret
                int paragraphIndex = paragraphNodesChildren.size();
                int paragraphOffset = textArea.getLength() + 1;

                Text paragraphNode = null;
                do {
                    paragraphNode = (Text)paragraphNodesChildren.get(--paragraphIndex);
                    paragraphOffset -= paragraphNode.getText().length() + 1;
                } while (caretPos < paragraphOffset);

                updateTextNodeCaretPos(caretPos - paragraphOffset);

                caretPath.getElements().clear();
                caretPath.getElements().addAll(paragraphNode.getCaretShape());

                caretPath.setLayoutX(paragraphNode.getLayoutX());

                // TODO: Remove this temporary workaround for RT-27533
                paragraphNode.setLayoutX(2 * paragraphNode.getLayoutX() - paragraphNode.getBoundsInParent().getMinX());

                caretPath.setLayoutY(paragraphNode.getLayoutY());
                if (oldCaretBounds == null || !oldCaretBounds.equals(caretPath.getBoundsInParent())) {
                    scrollCaretToVisible();
                }
            }

            // Update selection fg and bg
            int start = selection.getStart();
            int end = selection.getEnd();
            for (int i = 0, max = paragraphNodesChildren.size(); i < max; i++) {
                Node paragraphNode = paragraphNodesChildren.get(i);
                Text textNode = (Text)paragraphNode;
                int paragraphLength = textNode.getText().length() + 1;
                if (end > start && start < paragraphLength) {
                    textNode.setSelectionStart(start);
                    textNode.setSelectionEnd(Math.min(end, paragraphLength));

                    Path selectionHighlightPath = new Path();
                    selectionHighlightPath.setManaged(false);
                    selectionHighlightPath.setStroke(null);
                    PathElement[] selectionShape = textNode.getSelectionShape();
                    if (selectionShape != null) {
                        selectionHighlightPath.getElements().addAll(selectionShape);
                    }
                    selectionHighlightGroup.getChildren().add(selectionHighlightPath);
                    selectionHighlightGroup.setVisible(true);
                    selectionHighlightPath.setLayoutX(textNode.getLayoutX());
                    selectionHighlightPath.setLayoutY(textNode.getLayoutY());
                    updateHighlightFill();
                } else {
                    textNode.setSelectionStart(-1);
                    textNode.setSelectionEnd(-1);
                    selectionHighlightGroup.setVisible(false);
                }
                start = Math.max(0, start - paragraphLength);
                end   = Math.max(0, end   - paragraphLength);
            }

            if (SHOW_HANDLES) {
                // Position handle for the caret. This could be handle1 or handle2 when
                // a selection is active.
                Bounds b = caretPath.getBoundsInParent();
                if (selection.getLength() > 0) {
                    if (caretPos < anchorPos) {
                        selectionHandle1.setLayoutX(b.getMinX() - selectionHandle1.getWidth() / 2);
                        selectionHandle1.setLayoutY(b.getMinY() - selectionHandle1.getHeight() + 1);
                    } else {
                        selectionHandle2.setLayoutX(b.getMinX() - selectionHandle2.getWidth() / 2);
                        selectionHandle2.setLayoutY(b.getMaxY() - 1);
                    }
                } else {
                    caretHandle.setLayoutX(b.getMinX() - caretHandle.getWidth() / 2 + 1);
                    caretHandle.setLayoutY(b.getMaxY());
                }
            }

            if (scrollPane.getPrefViewportWidth() == 0
                    || scrollPane.getPrefViewportHeight() == 0) {
                updatePrefViewportWidth();
                updatePrefViewportHeight();
                if (getParent() != null && scrollPane.getPrefViewportWidth() > 0
                        || scrollPane.getPrefViewportHeight() > 0) {
                    // Force layout of viewRect in ScrollPaneSkin
                    getParent().requestLayout();
                }
            }

            // RT-36454: Fit to width/height only if smaller than viewport.
            // That is, grow to fit but don't shrink to fit.
            Bounds viewportBounds = scrollPane.getViewportBounds();
            boolean wasFitToWidth = scrollPane.isFitToWidth();
            boolean wasFitToHeight = scrollPane.isFitToHeight();
            boolean setFitToWidth = textArea.isWrapText() || computePrefWidth(-1) <= viewportBounds.getWidth();
            boolean setFitToHeight = computePrefHeight(width) <= viewportBounds.getHeight();
            if (wasFitToWidth != setFitToWidth || wasFitToHeight != setFitToHeight) {
                Platform.runLater(() -> {
                    scrollPane.setFitToWidth(setFitToWidth);
                    scrollPane.setFitToHeight(setFitToHeight);
                });
                getParent().requestLayout();
            }
        }
    }
}
