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
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import javafx.scene.input.KeyEvent;
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

    private static final String NID_INLINE_EDITOR = "inlineEditor";

    public enum Type {

        TEXT_AREA, TEXT_FIELD
    }

    public InlineEditController(final EditorController editorController) {
        this.editorController = editorController;
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
                break;
            case TEXT_FIELD:
                editor = new TextField(initialValue);
                minHeight = TEXT_FIELD_MIN_HEIGHT;
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

        return editor;
    }

    /**
     * Start an inline editing session. Display the specified TextInputControl
     * within a new popup window at the specified anchor node position.
     *
     * @param editor
     * @param anchor
     * @param requestCommit
     */
    public void startEditingSession(final TextInputControl editor, final Node anchor,
            final Callback<String, Boolean> requestCommit) {

        assert editor != null && anchor != null && requestCommit != null;

        final InlineEditPopupController popupController
                = new InlineEditPopupController(editor, requestCommit);

        // Handle key events
        // 1) Commit then stop inline editing when pressing Ctl/Meta + ENTER key
        // 2) Stop inline editing without commit when pressing ESCAPE key
        editor.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    // COMMIT the new value on ENTER key pressed
                    case ENTER:
                        // Commit inline editing on ENTER key :
                        // - if editor is a TextField
                        // - if META/CTL is down (both TextField and TextArea)
                        if ((editor instanceof TextField) || isModifierDown(event)) {
                            boolean commitSucceeded = requestCommit.call(editor.getText());
                            // If the commit succeeded, stop the editing session,
                            // otherwise keeps the editing session on-going
                            if (commitSucceeded) {
                                popupController.closeWindow();
                            }
                            // Consume the event so it is not received by the underlyting panel controller
                            event.consume();
                        }
                        break;
                    // STOP inline editing session without COMMIT on ESCAPE key pressed
                    case ESCAPE:
                        popupController.closeWindow();
                        break;
                    default:
                        break;
                }
            }
        });

        // Open the popup window and inform the editor controller that
        // an editing session has started.
        popupController.openWindow(anchor);
        editorController.textEditingSessionDidBegin(
                new EditingSessionDidBeginCallback(popupController));
    }

    private boolean isModifierDown(KeyEvent ke) {
        if (EditorPlatform.IS_MAC) {
            return ke.isMetaDown();
        } else {
            // Should cover Windows, Solaris, Linux
            return ke.isControlDown();
        }
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
        }

        @Override
        protected void makeRoot() {
            setRoot(editor);
        }

        @Override
        protected void onHidden(WindowEvent event) {
            // The inline editing popup auto hide when loosing focus :
            // need to commit inline editing
            if (initialValue.equals(editor.getText()) == false) {
                requestCommit.call(editor.getText());
            }
            // Inform the editor controller that the editing session has ended
            InlineEditController.this.editorController.textEditingSessionDidEnd();
        }

        @Override
        protected void anchorBoundsDidChange() {
        }

        @Override
        protected void anchorTransformDidChange() {
            // Called when scrolling the hierarchy for instance
            closeWindow();
        }

        @Override
        protected void anchorXYDidChange() {
            // This callback should not be needed for auto hiding popups
            // See RT-31292 : Popup does not auto hide when resizing the window
            closeWindow();
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

        private final InlineEditPopupController popupController;

        EditingSessionDidBeginCallback(final InlineEditPopupController popupController) {
            super();
            this.popupController = popupController;
        }

        @Override
        public Boolean call(Void p) {
            boolean commitSucceeded
                    = popupController.requestCommit.call(popupController.editor.getText());
            if (commitSucceeded) {
                popupController.closeWindow();
                return true;
            } else {
                return false;
            }
        }

    }
}
