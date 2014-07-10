/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.util;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractPopupController;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Controller used to in line edit nodes. The inline edit controller will
 * display TextInputControl within a popup window.
 */
public class InlineEditController {

    // Style class used for styling the inline editor (TextInputControl)
    public static final String INLINE_EDITOR = "inline-editor"; //NOI18N
    private static final double TEXT_INPUT_CONTROL_MIN_WIDTH = 15;
    private static final double TEXT_AREA_MIN_HEIGHT = 80;
    private static final double TEXT_FIELD_MIN_HEIGHT = 15;
    private final EditorController editorController;
    private InlineEditPopupController popupController;

    private static final String NID_INLINE_EDITOR = "inlineEditor";

    public enum Type {

        TEXT_AREA, TEXT_FIELD
    }

    public InlineEditController(final EditorController editorController) {
        this.editorController = editorController;
    }

    public boolean isWindowOpened() {
        return (popupController == null) ? false : popupController.isWindowOpened();
    }

    public TextInputControl getEditor() {
        return popupController.getEditor();
    }

    /**
     * Helper method to create a TextInputControl using the specified target
     * bounds and initial value. The created TextInputControl will get same
     * width and height as the specified target node. It will be styled using
     * the INLINE_EDITOR style class defined in the panel root style sheet.
     *
     * @param type
     * @param target
     * @param initialValue
     * @return
     */
    public final TextInputControl createTextInputControl(final Type type, final Node target, final String initialValue) {

        TextInputControl editor = null;
        double minHeight = 0;
        switch (type) {
            case TEXT_AREA:
                editor = new TextArea(initialValue);
                minHeight = TEXT_AREA_MIN_HEIGHT;
                // Update some properties specific to TextArea
                final Boolean isWrapText = isWrapText(target);
                ((TextArea) editor).setWrapText(isWrapText);
                break;
            case TEXT_FIELD:
                editor = new TextField(initialValue);
                minHeight = TEXT_FIELD_MIN_HEIGHT;
                // Update some properties specific to TextField
                final Pos alignment = getAlignment(target);
                ((TextField) editor).setAlignment(alignment);
                break;
            default:
                // Should never occur
                assert false;
                break;
        }
        assert editor != null;

        // Update editor size
        final Bounds targetBounds = target.getLayoutBounds();
        double targetWidth = target.getScaleX() * targetBounds.getWidth();
        double targetHeight = target.getScaleY() * targetBounds.getHeight();
        double editorWidth = Math.max(targetWidth, TEXT_INPUT_CONTROL_MIN_WIDTH);
        double editorHeight = Math.max(targetHeight, minHeight);
        editor.setMaxSize(editorWidth, editorHeight);
        editor.setMinSize(editorWidth, editorHeight);
        editor.setPrefSize(editorWidth, editorHeight);
        editor.setId(NID_INLINE_EDITOR);

        // Update some properties with the target properties values
        final Insets padding = getPadding(target);
        editor.setPadding(padding);
        final Font font = getFont(target);
        editor.setFont(font);

        return editor;
    }

    /**
     * Start an inline editing session. Display the specified TextInputControl
     * within a new popup window at the specified anchor node position.
     *
     * @param editor
     * @param anchor
     * @param requestCommit
     * @param requestRevert
     */
    public void startEditingSession(final TextInputControl editor, final Node anchor,
            final Callback<String, Boolean> requestCommit,
            final Callback<Void, Boolean> requestRevert) {

        assert editor != null && anchor != null && requestCommit != null;
        assert getEditorController().isTextEditingSessionOnGoing() == false;

        popupController = new InlineEditPopupController(editor, requestCommit);

        // Handle key events
        // 1) Commit then stop inline editing when pressing Ctl/Meta + ENTER key
        // 2) Stop inline editing without commit when pressing ESCAPE key
        editor.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                // COMMIT the new value on ENTER key pressed
                case ENTER:
                    // Commit inline editing on ENTER key :
                    // - if editor is a TextField
                    // - if META/CTL is down (both TextField and TextArea)
                    if ((editor instanceof TextField) || isModifierDown(event)) {
                        requestCommitAndClose(requestCommit, editor.getText());
                        // Consume the event so it is not received by the underlyting panel controller
                        event.consume();
                    }
                    break;
                // COMMIT the new value on TAB key pressed
                case TAB:
                    // Commit inline editing on TAB key
                    requestCommitAndClose(requestCommit, editor.getText());
                    // Consume the event so it is not received by the underlyting panel controller
                    event.consume();
                    break;
                // STOP inline editing session without COMMIT on ESCAPE key pressed
                case ESCAPE:
                    requestRevertAndClose(requestRevert);
                    // Consume the event so it is not received by the underlyting panel controller
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        // Open the popup window and inform the editor controller that
        // an editing session has started.
        popupController.openWindow(anchor);
        editorController.textEditingSessionDidBegin(
                new EditingSessionDidBeginCallback(this));
    }

    public EditorController getEditorController() {
        return editorController;
    }

    InlineEditPopupController getPopupController() {
        return popupController;
    }

    private boolean requestCommitAndClose(
            final Callback<String, Boolean> requestCommit,
            final String newValue) {

        // Using PrefixedValue PLAIN_STRING allow to consider special characters (such as @, %,...)
        // as "standard" characters (i.e. to backslash them)
        final String newPlainValue = new PrefixedValue(PrefixedValue.Type.PLAIN_STRING, newValue).toString();
        boolean commitSucceeded = requestCommit.call(newPlainValue);
        // If the commit succeeded, stop the editing session,
        // otherwise keeps the editing session on-going
        if (commitSucceeded) {
            // First inform the editor controller that the editing session has ended
            getEditorController().textEditingSessionDidEnd();
            // Then close the window
            popupController.closeWindow();
        }
        return commitSucceeded;
    }

    private boolean requestRevertAndClose(
            final Callback<Void, Boolean> requestRevert) {

        boolean revertSucceeded = requestRevert == null ? true : requestRevert.call(null);
        // If the revert succeeded, stop the editing session,
        // otherwise keeps the editing session on-going
        if (revertSucceeded) {
            // First inform the editor controller that the editing session has ended
            getEditorController().textEditingSessionDidEnd();
            // Then close the window
            popupController.closeWindow();
        }
        return revertSucceeded;
    }

    private boolean isModifierDown(KeyEvent ke) {
        if (EditorPlatform.IS_MAC) {
            return ke.isMetaDown();
        } else {
            // Should cover Windows, Solaris, Linux
            return ke.isControlDown();
        }
    }

    private Pos getAlignment(Node node) {
        final Pos result;
        if (node instanceof Labeled) {
            result = ((Labeled) node).getAlignment();
        } else if (node instanceof TextField) {
            result = ((TextField) node).getAlignment();
        } else {
            result = Pos.CENTER_LEFT;
        }
        return result;
    }

    private Font getFont(Node node) {
        final Font result;
        if (node instanceof Labeled) {
            result = ((Labeled) node).getFont();
        } else if (node instanceof Text) {
            result = ((Text) node).getFont();
        } else if (node instanceof TextInputControl) {
            result = ((TextInputControl) node).getFont();
        } else {
            result = Font.getDefault();
        }
        return result;
    }

    private Insets getPadding(Node node) {
        final Insets result;
        if (node instanceof Region) {
            result = ((Region) node).getPadding();
        } else {
            result = Insets.EMPTY;
        }
        return result;
    }

    private boolean isWrapText(Node node) {
        final boolean result;
        if (node instanceof TextArea) {
            result = ((TextArea) node).isWrapText();
        } else if (node instanceof Labeled) {
            result = ((Labeled) node).isWrapText();
        } else {
            result = false;
        }
        return result;
    }
    /*
     * *************************************************************************
     * Popup controller class
     * *************************************************************************
     */

    private class InlineEditPopupController extends AbstractPopupController {

        private final TextInputControl editor;
        private final Callback<String, Boolean> requestCommit;
        private final String initialValue;

        public InlineEditPopupController(final TextInputControl editor, final Callback<String, Boolean> requestCommit) {
            this.editor = editor;
            this.requestCommit = requestCommit;
            this.initialValue = editor.getText();

            this.editor.focusedProperty().addListener((ChangeListener<Boolean>) (ov, oldValue, newValue) -> {
                // The inline editing popup auto hide when loosing focus :
                // need to commit inline editing on focus change
                if (getEditorController().isTextEditingSessionOnGoing() // Editing session has not been ended by ENTER key
                        && newValue == false) {
                    requestCommitAndClose(requestCommit, editor.getText());
                }
            });
        }

        TextInputControl getEditor() {
            return editor;
        }

        @Override
        protected void makeRoot() {
            setRoot(editor);
        }

        @Override
        protected void onHidden(WindowEvent event) {
        }

        @Override
        protected void anchorBoundsDidChange() {
        }

        @Override
        protected void anchorTransformDidChange() {
            // When scrolling the hierarchy, the inline editor remains focused :
            // need to commit inline editing on transform change

            // The code below makes inline editing in content panel fail
            // (due to the received events ordering ??)
//            if (editorController.isTextEditingSessionOnGoing() // Editing session has not been ended by ENTER key
//                    && initialValue.equals(editor.getText()) == false) {
//                requestCommitAndClose(requestCommit, editor.getText());
//            }
        }

        @Override
        protected void anchorXYDidChange() {
            // When resizing the window, the inline editor remains focused :
            // need to commit inline editing on X/Y change
            if (getEditorController().isTextEditingSessionOnGoing() // Editing session has not been ended by ENTER key
                    && initialValue.equals(editor.getText()) == false) {
                requestCommitAndClose(requestCommit, editor.getText());
            }
        }

        @Override
        protected void controllerDidCreatePopup() {
            getPopup().setAutoFix(false);
            getPopup().setAutoHide(true);
        }

        /**
         * Update the popup location over the anchor node.
         */
        @Override
        protected void updatePopupLocation() {
            final Node anchor = getAnchor();
            final Popup popup = getPopup();
            assert anchor != null && popup != null;

            final Bounds anchorBounds = anchor.getLayoutBounds();
            assert anchorBounds != null;

            Point2D popupLocation;

            // At exit time, closeRequestHandler() is not always called.
            // So this method can be invoked after the anchor has been removed the
            // scene. This looks like a bug in FX...
            // Anway we protect ourself by checking.
            if (anchor.getScene() != null) {
                popupLocation = anchor.localToScreen(anchorBounds.getMinX(), anchorBounds.getMinY());
                popup.setX(popupLocation.getX());
                popup.setY(popupLocation.getY());
            }
        }
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class EditingSessionDidBeginCallback implements Callback<Void, Boolean> {

        private final InlineEditController inlineEditController;

        EditingSessionDidBeginCallback(final InlineEditController inlineEditController) {
            super();
            this.inlineEditController = inlineEditController;
        }

        @Override
        public Boolean call(Void p) {
            final InlineEditPopupController popupController
                    = inlineEditController.getPopupController();
            return inlineEditController.requestCommitAndClose(
                    popupController.requestCommit,
                    popupController.editor.getText());
        }
    }
}
