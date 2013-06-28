/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.design.author;

import java.util.ArrayList;

/**
 * <p>A Result object represents the logical result of an operation.  The <b>SUCCESS</b> and
 * <b>FAILURE</b> Result objects are useful when returning an otherwise empty result with no user
 * messages. <b>null</b> Results should never be returned, but if one is, it will be treated
 * as Result.SUCCESS with no messages.</p>
 *
 * <p>Some Result objects may contain user messages, returned by the 'getMessages()' method.  These
 * messages will be displayed to the user in one of three ways:</p>
 *
 * <p><table border="1" width="100%">
 *   <tr>
 *     <td><b>Status Bar</b></td>
 *     <td>The Status Bar displays very low-importance messages.  Any ResultMessage objects of type
 *         ResultMessage.TYPE_INFORMATION will be displayed here, as long as there are no result
 *         options returned from the 'getResultOptions()' method.</td>
 *   </tr>
 *   <tr>
 *     <td><b>Message Window</b></td>
 *     <td>The Message Window displays more important messages than the status bar, as they are
 *         persisted until the user clears the message window.  Any ResultMessage objects of type
 *         ResultMessage.TYPE_WARNING will be displayed here, as long as there are no result
 *         options returned from the 'getResultOptions()' method.</td>
 *   </tr>
 *   <tr>
 *     <td><b>Message Dialog</b></td>
 *     <td>A pop-up modal Message Dialog displays the most important messages, as the user must stop
 *         work to 'handle' the dialog.  Any ResultMessage objects of type ResultMessage.TYPE_CRITICAL
 *         will be displayed in an modal dialog.  If there are any result options returned from the
 *         'getResultOptions()' method, they will be displayed as buttons in the dialog, otherwise
 *         a default 'OK' button will be displayed.  If a Result contains messages that are less
 *         than TYPE_CRITICAL, but result options are included - the messages will be 'upgraded' and
 *         displayed in the model dialog, as if they were TYPE_CRITICAL.</td>
 *   </tr>
 * </table></p>
 *
 * <p>The three most important methods of this class are:  'isSuccess()', 'getMessages()', and
 * 'getMessageOptions()'.  These are called by the IDE to investigate the details of a Result
 * object.</p>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public class Result {
    /**
     * Common instance of a successful result object
     */
    public static final Result SUCCESS = new Result(true);

    /**
     * Common instance of a failed result object
     */
    public static final Result FAILURE = new Result(false);

    /**
     * Protected storage for the 'success' property
     */
    protected boolean success;

    /**
     * Protected storage for the message list (ResultMessage[])
     */
    protected ArrayList messages;

    /**
     * Protected storage for the result option list (DisplayAction[])
     */
    protected ArrayList options;

    /**
     * Protected storage for the 'resultDialogTitle' property
     */
    protected String dialogTitle;

    /**
     * Protected storage for the 'dialogHelpKey' property
     */
    protected String helpKey;

    /**
     * Constructs a Result object with the specified success status
     *
     * @param success <code>true</code> if successful, <code>false</code> if not
     */
    public Result(final boolean success) {
        this.success = success;
    }

    /**
     * Constructs a Result object with the specified success status and message
     *
     * @param success <code>true</code> if successful, <code>false</code> if not
     * @param message A ResultMessage to display to the user
     */
    public Result(final boolean success, final ResultMessage message) {
        this(success);
        addMessage(message);
    }

    /**
     * Constructs a Result object with the specified success status and messages
     *
     * @param success <code>true</code> if successful, <code>false</code> if not
     * @param messages A set of ResultMessage(s) to display to the user
     */
    public Result(boolean success, ResultMessage[] messages) {
        this(success);

        if (messages != null) {
            for (ResultMessage message : messages) {
                addMessage(message);
            }
        }
    }

    /**
     * Constructs a Result object with the specified success status, messages, and options
     *
     * @param success <code>true</code> if successful, <code>false</code> if not
     * @param messages A set of ResultMessage(s) to display to the user
     * @param options A set of DisplayAction(s) to present to the user as option buttons
     */
    public Result(final boolean success, final ResultMessage[] messages,
        final DisplayAction[] options) {
        this(success, messages);

        if (options != null) {
            for (DisplayAction option : options) {
                addResultOption(option);
            }
        }
    }

    /**
     * Sets the success state of this Result
     *
     * @param success the success state of this Result
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * Returns <code>true</code> if the operation represented by this Result object was successful
     *
     * @return <code>true</code> if the operation was successful, <code>false</code> if not
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Adds a ResultMessage to this Result to be displayed to the user
     *
     * @param message The ResultMessage to add
     */
    public final void addMessage(ResultMessage message) {
        if (message == null) {
            return;
        }

        if (messages == null) {
            messages = new ArrayList();
        }

        if (!messages.contains(message)) {
            messages.add(message);
        }
    }

    /**
     * Adds an array of ResultMessage to this Result to be displayed to the user
     *
     * @param messages The array of ResultMessage(s) to add
     */
    public void addMessages(ResultMessage[] messages) {
        if ((messages == null) || (messages.length < 1)) {
            return;
        }

        if (this.messages == null) {
            this.messages = new ArrayList();
        }

        for (ResultMessage message : messages) {
            if (!this.messages.contains(message)) {
                this.messages.add(message);
            }
        }
    }

    /**
     * Removes a ResultMessage from the set to display to the user
     *
     * @param message the ResultMessage to remove from the set
     */
    public void removeMessage(ResultMessage message) {
        if ((messages == null) || (message == null)) {
            return;
        }

        messages.remove(message);

        if (messages.size() == 0) {
            messages = null;
        }
    }

    /**
     * Removes an array of ResultMessage(s) from the set to display to the user
     *
     * @param messages the ResultMessage(s) to remove from the set
     */
    public void removeMessages(ResultMessage[] messages) {
        if ((this.messages == null) || (messages == null) || (messages.length < 1)) {
            return;
        }

        for (ResultMessage message : messages) {
            this.messages.remove(message);
        }

        if (this.messages.size() == 0) {
            this.messages = null;
        }
    }

    /**
     * Returns the count of ResultMessage(s) in this Result
     *
     * @return the count of ResultMessage(s) in this Result
     */
    public int getMessageCount() {
        return (messages != null) ? messages.size() : 0;
    }

    /**
     * Returns an (optional) set of ResultMessage objects from the completed operation.  Any
     * ResultMessage objects will be displayed to the user either via the status-line, message
     * window, or in a dialog - depending on several conditions outlined above.
     *
     * @return An array of ResultMessage objects, or null if there the operation produced no
     *         messages
     */
    public ResultMessage[] getMessages() {
        return (messages != null)
        ? (ResultMessage[])messages.toArray(new ResultMessage[messages.size()]) : null;
    }

    /**
     * Adds a DisplayAction to this Result to be displayed to the user as an option button in a
     * dialog
     *
     * @param option The DisplayAction to add as an option button
     */
    public final void addResultOption(DisplayAction option) {
        if (option == null) {
            return;
        }

        if (options == null) {
            options = new ArrayList();
        }

        if (!options.contains(option)) {
            options.add(option);
        }
    }

    /**
     * Adds a set of DisplayAction(s) to this Result to be displayed to the user as option buttons
     * in a dialog
     *
     * @param options The DisplayAction(s) to add as option buttons
     */
    public void addResultOptions(DisplayAction[] options) {
        if ((options == null) || (options.length < 1)) {
            return;
        }

        if (this.options == null) {
            this.options = new ArrayList();
        }

        for (DisplayAction option : options) {
            if (!this.options.contains(option)) {
                this.options.add(option);
            }
        }
    }

    /**
     * Removes a DisplayAction (option button in a dialog) from this Result
     *
     * @param option The DisplayAction (option button) to remove
     */
    public void removeResultOption(DisplayAction option) {
        if ((options == null) || (option == null)) {
            return;
        }

        options.remove(option);

        if (options.size() == 0) {
            options = null;
        }
    }

    /**
     * Removes an array of DisplayAction(s) (option buttons in a dialog) from this Result
     *
     * @param options The DisplayAction(s) (option buttons) to remove
     */
    public void removeResultOptions(DisplayAction[] options) {
        if ((this.options == null) || (options == null) || (options.length < 1)) {
            return;
        }

        for (DisplayAction option : options) {
            this.options.remove(option);
        }

        if (this.options.size() == 0) {
            this.options = null;
        }
    }

    /**
     * Returns the count of DisplayAction(s) in this Result which will be displayed to the user as
     * option buttons in a dialog
     *
     * @return The count of option buttons to show in the dialog
     */
    public int getResultOptionCount() {
        return (options != null) ? options.size() : 0;
    }

    /**
     * Returns an (optional) set of DisplayAction objects representing options in a dialog box for
     * the user.  If the returned array is non-null and has more than zero elements, a dialog box
     * will automatically be shown to the user with a button for each returned DisplayAction.  When
     * the user 'clicks' a particular button, the DisplayAction will be 'invoked()'.
     *
     * @return An array of DisplayAction objects representing buttons in a dialog to show the user,
     *         or null
     */
    public DisplayAction[] getResultOptions() {
        return (options != null)
        ? (DisplayAction[])options.toArray(new DisplayAction[options.size()]) : null;
    }

    /**
     * Sets the 'dialogTitle' property.  The Result can only trigger a dialog if there is a
     * ResultMessage of TYPE_CRITICAL or there are result options to display.
     *
     * @param dialogTitle The desired title for the result dialog
     */
    public void setDialogTitle(final String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    /**
     * Returns the 'dialogTitle' property.  The Result can only trigger a dialog if there is a
     * ResultMessage of TYPE_CRITICAL or there are result options to display.
     *
     * @return The title for the result dialog
     */
    public String getDialogTitle() {
        return dialogTitle;
    }

    /**
     * Sets the 'dialogHelpKey' property.  The Result can only trigger a dialog if there is a
     * ResultMessage of TYPE_CRITICAL or there are result options to display.
     *
     * @param helpKey The desired help key for the result dialog
     */
    public void setDialogHelpKey(final String helpKey) {
        this.helpKey = helpKey;
    }

    /**
     * Returns the 'dialogHelpKey' property.  The Result can only trigger a dialog if there is a
     * ResultMessage of TYPE_CRITICAL or there are result options to display.
     *
     * @return The help key for the result dialog
     */
    public String getDialogHelpKey() {
        return helpKey;
    }
}
