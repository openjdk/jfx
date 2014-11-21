/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Note that this class tests non-blocking alerts only. For blocking alerts,
 * there exists an exact copy of this code in
 * rt\tests\system\src\test\java\javafx\scene\control
 *
 * Whenever this file changes, the contents of the class should be copied over
 * to the system test class __with no changes__.
 */
public class AlertTest {

    private static final String DUMMY_RESULT = "dummy";
    static boolean blocking = false;

    private Dialog<ButtonType> dialog;

    private boolean closeWasForcedButStageWasShowing = false;
    private boolean closeVetoed = false;
    private Object result = DUMMY_RESULT;

    @After public void cleanup() {
        getStage(dialog).close();
        dialog = null;
        result = DUMMY_RESULT;
        closeVetoed = false;
        closeWasForcedButStageWasShowing = false;
    }

    private static Stage getStage(Dialog<?> dialog) {
        return ((HeavyweightDialog) dialog.dialog).stage;
    }

    private void showAndHideDialog(Dialog<?> dialog, boolean normalClose) {
        if (dialog.isShowing()) return;

        if (blocking) {
            new Thread(() -> {
                try {
                    // wait a short while after showing the dialog and try to
                    // close it.
                    Thread.sleep(750);
                    Platform.runLater(() -> hideDialog(dialog, normalClose));

                    // wait again
                    Thread.sleep(750);
                    if (closeVetoed) {
                        // now we get serious and clobber the stage so that we
                        // can carry on with the next test.
                        result = dialog.getResult();
                        closeWasForcedButStageWasShowing = true;
                        Platform.runLater(() -> getStage(dialog).close());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            dialog.showAndWait();
        } else {
            dialog.show();
            hideDialog(dialog, normalClose);
        }
    }

    private void hideDialog(Dialog<?> dialog, boolean normalClose) {
        Stage stage = getStage(dialog);
        WindowEvent event = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);

        if (normalClose) {
            // we can't just click 'dialog.close()' here
            dialog.close();
        } else {
            // this is hacky, but effectively we're depending on the implementation
            // detail of heavyweight dialogs to call into the onCloseRequest
            // handler of the heavyweight stage.
            stage.getOnCloseRequest().handle(event);
        }

        // if at this point the dialog is still showing, then the close was obviously vetoed
        closeVetoed = dialog.isShowing();
    }

    private void assertResultValue(Object expected, Dialog<?> dialog, boolean normalClose) {
        showAndHideDialog(dialog, normalClose);

        if (result != DUMMY_RESULT) {
            assertEquals(expected, result);
        } else {
            assertEquals(expected, dialog.getResult());
        }
    }

    private void assertCloseRequestVetoed(Dialog<?> dialog, boolean normalClose) {
        Stage stage = getStage(dialog);
        showAndHideDialog(dialog, normalClose);

        assertTrue(closeWasForcedButStageWasShowing || stage.isShowing());
        assertTrue(closeWasForcedButStageWasShowing || dialog.isShowing());
    }

    private void assertCloseRequestAccepted(Dialog<?> dialog, boolean normalClose) {
        Stage stage = getStage(dialog);
        showAndHideDialog(dialog, normalClose);

        assertFalse(!closeWasForcedButStageWasShowing && stage.isShowing());
        assertFalse(!closeWasForcedButStageWasShowing && dialog.isShowing());
    }


    // --- Information alert tests
    // Information has one 'OK' button.
    // Because there is no cancel button, but only one button is present, we
    // can close the dialog without veto, and the result will be null.
    //
    // TODO review the above statement - should we return null, ButtonType.CANCEL
    // (even though it doesn't exist in the dialog) or, in cases where there is
    // only one button, do we return that button as the result?
    @Test public void alert_information_abnormalClose() {
        dialog = new Alert(Alert.AlertType.INFORMATION, "Hello World!");
        assertResultValue(null, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_information_normalClose() {
        dialog = new Alert(Alert.AlertType.INFORMATION, "Hello World!");
        assertResultValue(null, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }


    // --- Error alert tests
    // Error has one 'OK' button.
    // Because there is no cancel button, but only one button is present, we
    // can close the dialog without veto, and the result will be null.
    //
    // TODO review the above statement - should we return null, ButtonType.CANCEL
    // (even though it doesn't exist in the dialog) or, in cases where there is
    // only one button, do we return that button as the result?
    @Test public void alert_error_abnormalClose() {
        dialog = new Alert(Alert.AlertType.ERROR, "Hello World!");
        assertResultValue(null, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_error_normalClose() {
        dialog = new Alert(Alert.AlertType.ERROR, "Hello World!");
        assertResultValue(null, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }


    // --- Warning alert tests
    // Warning has one 'OK' button.
    // Because there is no cancel button, but only one button is present, we
    // can close the dialog without veto, and the result will be null.
    //
    // TODO review the above statement - should we return null, ButtonType.CANCEL
    // (even though it doesn't exist in the dialog) or, in cases where there is
    // only one button, do we return that button as the result?
    @Test public void alert_warning_abnormalClose() {
        dialog = new Alert(Alert.AlertType.WARNING, "Hello World!");
        assertResultValue(null, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_warning_normalClose() {
        dialog = new Alert(Alert.AlertType.WARNING, "Hello World!");
        assertResultValue(null, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }


    // --- Confirmation alert tests
    // Confirmation has two buttons: 'OK' and 'Cancel'
    // Because there is a cancel button, close requests are accepted, and the
    // result type is ButtonType.CANCEL
    @Test public void alert_confirmation_abnormalClose() {
        dialog = new Alert(Alert.AlertType.CONFIRMATION, "Hello World!");
        assertResultValue(ButtonType.CANCEL, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_confirmation_normalClose() {
        dialog = new Alert(Alert.AlertType.CONFIRMATION, "Hello World!");
        assertResultValue(ButtonType.CANCEL, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }


    // --- AlertType.NONE alert tests
    // None has no buttons
    // Because there is no cancel button, and zero other buttons, this dialog by
    // default should not be closable
    @Test public void alert_none_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        assertResultValue(null, dialog, false);
        assertCloseRequestVetoed(dialog, false);
    }

    @Test public void alert_none_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        assertResultValue(null, dialog, true);
        assertCloseRequestVetoed(dialog, true);
    }


    // --- Testing what happens with custom buttons and closing
    @Test public void alert_zeroButtons_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        assertResultValue(null, dialog, false);
        assertCloseRequestVetoed(dialog, false);
    }

    @Test public void alert_zeroButtons_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        assertResultValue(null, dialog, true);
        assertCloseRequestVetoed(dialog, true);
    }

    @Test public void alert_oneButton_noCancel_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.YES);
        assertResultValue(null, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_oneButton_noCancel_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.YES);
        assertResultValue(null, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }

    @Test public void alert_oneButton_withCancel_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        assertResultValue(ButtonType.CANCEL, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_oneButton_withCancel_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        assertResultValue(ButtonType.CANCEL, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }

    @Test public void alert_twoButtons_noCancel_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.YES);
        assertResultValue(null, dialog, false);
        assertCloseRequestVetoed(dialog, false);
    }

    @Test public void alert_twoButtons_noCancel_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.YES);
        assertResultValue(null, dialog, true);
        assertCloseRequestVetoed(dialog, true);
    }

    @Test public void alert_twoButtons_withCancel_abnormalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        assertResultValue(ButtonType.CANCEL, dialog, false);
        assertCloseRequestAccepted(dialog, false);
    }

    @Test public void alert_twoButtons_withCancel_normalClose() {
        dialog = new Alert(Alert.AlertType.NONE, "Hello World!");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        assertResultValue(ButtonType.CANCEL, dialog, true);
        assertCloseRequestAccepted(dialog, true);
    }
}
