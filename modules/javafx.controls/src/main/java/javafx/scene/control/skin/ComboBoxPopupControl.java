/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.control.FakeFocusTextField;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.behavior.TextInputControlBehavior;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import com.sun.javafx.scene.traversal.Algorithm;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalContext;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

/**
 * An abstract class that extends the functionality of {@link ComboBoxBaseSkin}
 * to include API related to showing ComboBox-like controls as popups.
 *
 * @param <T> The type of the ComboBox-like control.
 * @since 9
 */
public abstract class ComboBoxPopupControl<T> extends ComboBoxBaseSkin<T> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    PopupControl popup;

    private boolean popupNeedsReconfiguring = true;

    private final ComboBoxBase<T> comboBoxBase;
    private TextField textField;

    private String initialTextFieldValue = null;



    /***************************************************************************
     *                                                                         *
     * TextField Listeners                                                     *
     *                                                                         *
     **************************************************************************/

    private EventHandler<MouseEvent> textFieldMouseEventHandler = event -> {
        ComboBoxBase<T> comboBoxBase = getSkinnable();
        if (!event.getTarget().equals(comboBoxBase)) {
            comboBoxBase.fireEvent(event.copyFor(comboBoxBase, comboBoxBase));
            event.consume();
        }
    };
    private EventHandler<DragEvent> textFieldDragEventHandler = event -> {
        ComboBoxBase<T> comboBoxBase = getSkinnable();
        if (!event.getTarget().equals(comboBoxBase)) {
            comboBoxBase.fireEvent(event.copyFor(comboBoxBase, comboBoxBase));
            event.consume();
        }
    };



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new instance of ComboBoxPopupControl, although note that this
     * instance does not handle any behavior / input mappings - this needs to be
     * handled appropriately by subclasses.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ComboBoxPopupControl(ComboBoxBase<T> control) {
        super(control);
        this.comboBoxBase = control;

        // editable input node
        this.textField = getEditor() != null ? getEditableInputNode() : null;

        // Fix for RT-29565. Without this the textField does not have a correct
        // pref width at startup, as it is not part of the scenegraph (and therefore
        // has no pref width until after the first measurements have been taken).
        if (this.textField != null) {
            getChildren().add(textField);
        }

        // move fake focus in to the textfield if the comboBox is editable
        comboBoxBase.focusedProperty().addListener((ov, t, hasFocus) -> {
            if (getEditor() != null) {
                // Fix for the regression noted in a comment in RT-29885.
                ((FakeFocusTextField)textField).setFakeFocus(hasFocus);
            }
        });

        comboBoxBase.addEventFilter(KeyEvent.ANY, ke -> {
            if (textField == null || getEditor() == null) {
                handleKeyEvent(ke, false);
            } else {
                // This prevents a stack overflow from our rebroadcasting of the
                // event to the textfield that occurs in the final else statement
                // of the conditions below.
                if (ke.getTarget().equals(textField)) return;

                switch (ke.getCode()) {
                  case ESCAPE:
                  case F10:
                      // Allow to bubble up.
                      break;

                  case ENTER:
                    handleKeyEvent(ke, true);
                    break;

                  default:
                    // Fix for the regression noted in a comment in RT-29885.
                    // This forwards the event down into the TextField when
                    // the key event is actually received by the ComboBox.
                    textField.fireEvent(ke.copyFor(textField, textField));
                    ke.consume();
                }
            }
        });

        // RT-38978: Forward input method events to TextField if editable.
        if (comboBoxBase.getOnInputMethodTextChanged() == null) {
            comboBoxBase.setOnInputMethodTextChanged(event -> {
                if (textField != null && getEditor() != null && comboBoxBase.getScene().getFocusOwner() == comboBoxBase) {
                    if (textField.getOnInputMethodTextChanged() != null) {
                        textField.getOnInputMethodTextChanged().handle(event);
                    }
                }
            });
        }

        // Fix for RT-36902, where focus traversal was getting stuck inside the ComboBox
        ParentHelper.setTraversalEngine(comboBoxBase,
                new ParentTraversalEngine(comboBoxBase, new Algorithm() {

            @Override public Node select(Node owner, Direction dir, TraversalContext context) {
                return null;
            }

            @Override public Node selectFirst(TraversalContext context) {
                return null;
            }

            @Override public Node selectLast(TraversalContext context) {
                return null;
            }
        }));

        updateEditable();
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * This method should return the Node that will be displayed when the user
     * clicks on the ComboBox 'button' area.
     * @return the Node that will be displayed when the user clicks on the
     * ComboBox 'button' area
     */
    protected abstract Node getPopupContent();

    /**
     * Subclasses are responsible for getting the editor. This will be removed
     * in FX 9 when the editor property is moved up to ComboBoxBase with
     * JDK-8130354
     *
     * Note: ComboBoxListViewSkin should return null if editable is false, even
     * if the ComboBox does have an editor set.
     * @return the editor
     */
    protected abstract TextField getEditor();

    /**
     * Subclasses are responsible for getting the converter. This will be
     * removed in FX 9 when the converter property is moved up to ComboBoxBase
     * with JDK-8130354.
     * @return the string converter
     */
    protected abstract StringConverter<T> getConverter();

    /** {@inheritDoc} */
    @Override public void show() {
        if (getSkinnable() == null) {
            throw new IllegalStateException("ComboBox is null");
        }

        Node content = getPopupContent();
        if (content == null) {
            throw new IllegalStateException("Popup node is null");
        }

        if (getPopup().isShowing()) return;

        positionAndShowPopup();
    }

    /** {@inheritDoc} */
    @Override public void hide() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    PopupControl getPopup() {
        if (popup == null) {
            createPopup();
        }
        return popup;
    }

    TextField getEditableInputNode() {
        if (textField == null && getEditor() != null) {
            textField = getEditor();
            textField.setFocusTraversable(false);
            textField.promptTextProperty().bind(comboBoxBase.promptTextProperty());
            textField.tooltipProperty().bind(comboBoxBase.tooltipProperty());

            // Fix for JDK-8145515 - in short the ComboBox was firing the event down to
            // the TextField, and then the TextField was firing it back up to the
            // ComboBox, resulting in stack overflows.
            textField.getProperties().put(TextInputControlBehavior.DISABLE_FORWARD_TO_PARENT, true);

            // Fix for RT-21406: ComboBox do not show initial text value
            initialTextFieldValue = textField.getText();
            // End of fix (see updateDisplayNode below for the related code)
        }

        return textField;
    }

    void setTextFromTextFieldIntoComboBoxValue() {
        if (getEditor() != null) {
            StringConverter<T> c = getConverter();
            if (c != null) {
                T oldValue = comboBoxBase.getValue();
                T value = oldValue;
                String text = textField.getText();

                // conditional check here added due to RT-28245
                if (oldValue == null && (text == null || text.isEmpty())) {
                    value = null;
                } else {
                    try {
                        value = c.fromString(text);
                    } catch (Exception ex) {
                        // Most likely a parsing error, such as DateTimeParseException
                    }
                }

                if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue))) {
                    // no point updating values needlessly if they are the same
                    comboBoxBase.setValue(value);
                }

                updateDisplayNode();
            }
        }
    }

    void updateDisplayNode() {
        if (textField != null && getEditor() != null) {
            T value = comboBoxBase.getValue();
            StringConverter<T> c = getConverter();

            if (initialTextFieldValue != null && ! initialTextFieldValue.isEmpty()) {
                // Remainder of fix for RT-21406: ComboBox do not show initial text value
                textField.setText(initialTextFieldValue);
                initialTextFieldValue = null;
                // end of fix
            } else {
                String stringValue = c.toString(value);
                if (value == null || stringValue == null) {
                    textField.setText("");
                } else if (! stringValue.equals(textField.getText())) {
                    textField.setText(stringValue);
                }
            }
        }
    }

    void updateEditable() {
        TextField newTextField = getEditor();

        if (getEditor() == null) {
            // remove event filters
            if (textField != null) {
                textField.removeEventFilter(MouseEvent.DRAG_DETECTED, textFieldMouseEventHandler);
                textField.removeEventFilter(DragEvent.ANY, textFieldDragEventHandler);

                comboBoxBase.setInputMethodRequests(null);
            }
        } else if (newTextField != null) {
            // add event filters

            // Fix for RT-31093 - drag events from the textfield were not surfacing
            // properly for the ComboBox.
            newTextField.addEventFilter(MouseEvent.DRAG_DETECTED, textFieldMouseEventHandler);
            newTextField.addEventFilter(DragEvent.ANY, textFieldDragEventHandler);

            // RT-38978: Forward input method requests to TextField.
            comboBoxBase.setInputMethodRequests(new ExtendedInputMethodRequests() {
                @Override public Point2D getTextLocation(int offset) {
                    return newTextField.getInputMethodRequests().getTextLocation(offset);
                }

                @Override public int getLocationOffset(int x, int y) {
                    return newTextField.getInputMethodRequests().getLocationOffset(x, y);
                }

                @Override public void cancelLatestCommittedText() {
                    newTextField.getInputMethodRequests().cancelLatestCommittedText();
                }

                @Override public String getSelectedText() {
                    return newTextField.getInputMethodRequests().getSelectedText();
                }

                @Override public int getInsertPositionOffset() {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getInsertPositionOffset();
                }

                @Override public String getCommittedText(int begin, int end) {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getCommittedText(begin, end);
                }

                @Override public int getCommittedTextLength() {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getCommittedTextLength();
                }
            });
        }

        textField = newTextField;
    }

    private Point2D getPrefPopupPosition() {
        return com.sun.javafx.util.Utils.pointRelativeTo(getSkinnable(), getPopupContent(), HPos.CENTER, VPos.BOTTOM, 0, 0, true);
    }

    private void positionAndShowPopup() {
        final ComboBoxBase<T> comboBoxBase = getSkinnable();
        if (comboBoxBase.getScene() == null) {
            return;
        }

        final PopupControl _popup = getPopup();
        _popup.getScene().setNodeOrientation(getSkinnable().getEffectiveNodeOrientation());


        final Node popupContent = getPopupContent();
        sizePopup();

        Point2D p = getPrefPopupPosition();

        popupNeedsReconfiguring = true;
        reconfigurePopup();

        _popup.show(comboBoxBase.getScene().getWindow(),
                    snapPositionX(p.getX()),
                    snapPositionY(p.getY()));

        popupContent.requestFocus();

        // second call to sizePopup here to enable proper sizing _after_ the popup
        // has been displayed. See RT-37622 for more detail.
        sizePopup();
    }

    private void sizePopup() {
        final Node popupContent = getPopupContent();

        if (popupContent instanceof Region) {
            // snap to pixel
            final Region r = (Region) popupContent;

            // 0 is used here for the width due to RT-46097
            double prefHeight = snapSizeY(r.prefHeight(0));
            double minHeight = snapSizeY(r.minHeight(0));
            double maxHeight = snapSizeY(r.maxHeight(0));
            double h = snapSizeY(Math.min(Math.max(prefHeight, minHeight), Math.max(minHeight, maxHeight)));

            double prefWidth = snapSizeX(r.prefWidth(h));
            double minWidth = snapSizeX(r.minWidth(h));
            double maxWidth = snapSizeX(r.maxWidth(h));
            double w = snapSizeX(Math.min(Math.max(prefWidth, minWidth), Math.max(minWidth, maxWidth)));

            popupContent.resize(w, h);
        } else {
            popupContent.autosize();
        }
    }

    private void createPopup() {
        popup = new PopupControl() {
            @Override public Styleable getStyleableParent() {
                return ComboBoxPopupControl.this.getSkinnable();
            }
            {
                setSkin(new Skin<Skinnable>() {
                    @Override public Skinnable getSkinnable() { return ComboBoxPopupControl.this.getSkinnable(); }
                    @Override public Node getNode() { return getPopupContent(); }
                    @Override public void dispose() { }
                });
            }
        };
        popup.getStyleClass().add(Properties.COMBO_BOX_STYLE_CLASS);
        popup.setConsumeAutoHidingEvents(false);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.setOnAutoHide(e -> getBehavior().onAutoHide(popup));
        popup.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            // RT-18529: We listen to mouse input that is received by the popup
            // but that is not consumed, and assume that this is due to the mouse
            // clicking outside of the node, but in areas such as the
            // dropshadow.
            getBehavior().onAutoHide(popup);
        });
        popup.addEventHandler(WindowEvent.WINDOW_HIDDEN, t -> {
            // Make sure the accessibility focus returns to the combo box
            // after the window closes.
            getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_NODE);
        });

        // Fix for RT-21207
        InvalidationListener layoutPosListener = o -> {
            popupNeedsReconfiguring = true;
            reconfigurePopup();
        };
        getSkinnable().layoutXProperty().addListener(layoutPosListener);
        getSkinnable().layoutYProperty().addListener(layoutPosListener);
        getSkinnable().widthProperty().addListener(layoutPosListener);
        getSkinnable().heightProperty().addListener(layoutPosListener);

        // RT-36966 - if skinnable's scene becomes null, ensure popup is closed
        getSkinnable().sceneProperty().addListener(o -> {
            if (((ObservableValue)o).getValue() == null) {
                hide();
            } else if (getSkinnable().isShowing()) {
                show();
            }
        });

    }

    void reconfigurePopup() {
        // RT-26861. Don't call getPopup() here because it may cause the popup
        // to be created too early, which leads to memory leaks like those noted
        // in RT-32827.
        if (popup == null) return;

        final boolean isShowing = popup.isShowing();
        if (! isShowing) return;

        if (! popupNeedsReconfiguring) return;
        popupNeedsReconfiguring = false;

        final Point2D p = getPrefPopupPosition();

        final Node popupContent = getPopupContent();
        final double minWidth = popupContent.prefWidth(Region.USE_COMPUTED_SIZE);
        final double minHeight = popupContent.prefHeight(Region.USE_COMPUTED_SIZE);

        if (p.getX() > -1) popup.setAnchorX(p.getX());
        if (p.getY() > -1) popup.setAnchorY(p.getY());
        if (minWidth > -1) popup.setMinWidth(minWidth);
        if (minHeight > -1) popup.setMinHeight(minHeight);

        final Bounds b = popupContent.getLayoutBounds();
        final double currentWidth = b.getWidth();
        final double currentHeight = b.getHeight();
        final double newWidth  = currentWidth < minWidth ? minWidth : currentWidth;
        final double newHeight = currentHeight < minHeight ? minHeight : currentHeight;

        if (newWidth != currentWidth || newHeight != currentHeight) {
            // Resizing content to resolve issues such as RT-32582 and RT-33700
            // (where RT-33700 was introduced due to a previous fix for RT-32582)
            popupContent.resize(newWidth, newHeight);
            if (popupContent instanceof Region) {
                ((Region)popupContent).setMinSize(newWidth, newHeight);
                ((Region)popupContent).setPrefSize(newWidth, newHeight);
            }
        }
    }

    private void handleKeyEvent(KeyEvent ke, boolean doConsume) {
        // When the user hits the enter or F4 keys, we respond before
        // ever giving the event to the TextField.
        if (ke.getCode() == KeyCode.ENTER) {
            if (ke.isConsumed() || ke.getEventType() != KeyEvent.KEY_RELEASED) {
                return;
            }
            setTextFromTextFieldIntoComboBoxValue();

            if (doConsume && comboBoxBase.getOnAction() != null) {
                ke.consume();
            } else if (textField != null) {
                textField.fireEvent(ke);
            }
        } else if (ke.getCode() == KeyCode.F4) {
            if (ke.getEventType() == KeyEvent.KEY_RELEASED) {
                if (comboBoxBase.isShowing()) comboBoxBase.hide();
                else comboBoxBase.show();
            }
            ke.consume(); // we always do a consume here (otherwise unit tests fail)
        } else if (ke.getCode() == KeyCode.F10 || ke.getCode() == KeyCode.ESCAPE) {
            // RT-23275: The TextField fires F10 and ESCAPE key events
            // up to the parent, which are then fired back at the
            // TextField, and this ends up in an infinite loop until
            // the stack overflows. So, here we consume these two
            // events and stop them from going any further.
            if (doConsume) ke.consume();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/





    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

}
