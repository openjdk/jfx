/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.tk.Toolkit;

/**
 * A Dialog in JavaFX wraps a {@link DialogPane} and provides the necessary API
 * to present it to end users. In JavaFX 8u40, this essentially means that the
 * {@link DialogPane} is shown to users inside a {@link Stage}, but future releases
 * may offer alternative options (such as 'lightweight' or 'internal' dialogs).
 * This API therefore is intentionally ignorant of the underlying implementation,
 * and attempts to present a common API for all possible implementations.
 *
 * <p>The Dialog class has a single generic type, R, which is used to represent
 * the type of the {@link #resultProperty() result} property (and also, how to
 * convert from {@link ButtonType} to R, through the use of the
 * {@link #resultConverterProperty() result converter} {@link Callback}).
 *
 * <p><strong>Critical note:</strong> It is critical that all developers who choose
 * to create their own dialogs by extending the Dialog class understand the
 * importance of the {@link #resultConverterProperty() result converter} property.
 * A result converter must always be set, whenever the R type is not
 * {@link Void} or {@link ButtonType}. If this is not heeded, developers will find
 * that they get ClassCastExceptions in their code, for failure to convert from
 * {@link ButtonType} via the {@link #resultConverterProperty() result converter}.
 *
 * <p>It is likely that most developers would be better served using either the
 * {@link Alert} class (for pre-defined, notification-style alerts), or either of
 * the two pre-built dialogs ({@link TextInputDialog} and {@link ChoiceDialog}),
 * depending on their needs.
 *
 * <p>Once a Dialog is instantiated, the next step is to configure it. Almost
 * all properties on Dialog are not related to the content of the Dialog, the
 * only exceptions are {@link #contentTextProperty()},
 * {@link #headerTextProperty()}, and {@link #graphicProperty()}, and these
 * properties are simply forwarding API onto the respective properties on the
 * {@link DialogPane} stored in the {@link #dialogPaneProperty() dialog pane}
 * property. These three properties are forwarded from DialogPane for developer
 * convenience. For developers wanting to configure their dialog, they will in many
 * cases be required to use code along the lines of
 * {@code dialog.getDialogPane().setExpandableContent(node)}.
 *
 * <p>After configuring these properties, all that remains is to consider whether
 * the buttons (created using {@link ButtonType} and the
 * {@link DialogPane#createButton(ButtonType)} method) are fully configured.
 * Developers will quickly find that the amount of configurability offered
 * via the {@link ButtonType} class is minimal. This is intentional, but does not
 * mean that developers can not modify the buttons created by the {@link ButtonType}
 * that have been specified. To do this, developers simply call the
 * {@link DialogPane#lookupButton(ButtonType)} method with the ButtonType
 * (assuming it has already been set in the {@link DialogPane#getButtonTypes()}
 * list. The returned Node is typically of type {@link Button}, but this depends
 * on if the {@link DialogPane#createButton(ButtonType)} method has been overridden. A
 * typical approach is therefore along the following lines:
 *
 * <pre> {@code ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
 * Dialog<String> dialog = new Dialog<>();
 * dialog.setTitle("Login Dialog");
 * dialog.setContentText("Would you like to log in?");
 * dialog.getDialogPane().getButtonTypes().add(loginButtonType);
 * boolean disabled = false; // computed based on content of text fields, for example
 * dialog.getDialogPane().lookupButton(loginButtonType).setDisable(disabled);
 * dialog.showAndWait();}</pre>
 *
 * <img src="doc-files/Dialog.png" alt="Image of the Dialog control">
 *
 * <p>Once a Dialog is instantiated and fully configured, the next step is to
 * show it. More often than not, dialogs are shown in a modal and blocking
 * fashion. 'Modal' means that the dialog prevents user interaction with the
 * owning application whilst it is showing, and 'blocking' means that code
 * execution stops at the point in which the dialog is shown. This means that
 * you can show a dialog, await the user response, and then continue running the
 * code that directly follows the show call, giving developers the ability to
 * immediately deal with the user input from the dialog (if relevant).
 *
 * <p>JavaFX dialogs are modal by default (you can change this via the
 * {@link #initModality(javafx.stage.Modality)} API). To specify whether you want
 * blocking or non-blocking dialogs, developers simply choose to call
 * {@link #showAndWait()} or {@link #show()} (respectively). By default most
 * developers should choose to use {@link #showAndWait()}, given the ease of
 * coding in these situations. Shown below is three code snippets, showing three
 * equally valid ways of showing a dialog:
 *
 * <p><strong>Option 1: The 'traditional' approach</strong>
 * <pre> {@code Optional<ButtonType> result = dialog.showAndWait();
 * if (result.isPresent() && result.get() == ButtonType.OK) {
 *     formatSystem();
 * }}</pre>
 *
 * <p><strong>Option 2: The traditional + Optional approach</strong>
 * <pre> {@code dialog.showAndWait().ifPresent(response -> {
 *     if (response == ButtonType.OK) {
 *         formatSystem();
 *     }
 * });}</pre>
 *
 * <p><strong>Option 3: The fully lambda approach</strong>
 * <pre> {@code dialog.showAndWait()
 *       .filter(response -> response == ButtonType.OK)
 *       .ifPresent(response -> formatSystem());}</pre>
 *
 * <p>There is no better or worse option of the three listed above, so developers
 * are encouraged to work to their own style preferences. The purpose of showing
 * the above is to help introduce developers to the {@link Optional} API, which
 * is new in Java 8 and may be foreign to many developers.
 *
 * <h2>Dialog Validation / Intercepting Button Actions</h2>
 *
 * <p>In some circumstances it is desirable to prevent a dialog from closing
 * until some aspect of the dialog becomes internally consistent (e.g. a form
 * inside the dialog has all fields in a valid state). To do this, users of the
 * dialogs API should become familiar with the
 * {@link DialogPane#lookupButton(ButtonType)} method. By passing in a
 * {@link javafx.scene.control.ButtonType ButtonType} (that has already been set
 * in the {@link DialogPane#getButtonTypes() button types} list), users will be
 * returned a Node that is typically of type {@link Button} (but this depends
 * on if the {@link DialogPane#createButton(ButtonType)} method has been
 * overridden). With this button, users may add an event filter that is called
 * before the button does its usual event handling, and as such users may
 * prevent the event handling by {@code consuming} the event. Here's a simplified
 * example:
 *
 * <pre> {@code final Button btOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
 * btOk.addEventFilter(ActionEvent.ACTION, event -> {
 *     if (!validateAndStore()) {
 *         event.consume();
 *     }
 * });}</pre>
 *
 * <h2>Dialog Closing Rules</h2>
 *
 * <p>It is important to understand what happens when a Dialog is closed, and
 * also how a Dialog can be closed, especially in abnormal closing situations
 * (such as when the 'X' button is clicked in a dialogs title bar, or when
 * operating system specific keyboard shortcuts (such as alt-F4 on Windows)
 * are entered). Fortunately, the outcome is well-defined in these situations,
 * and can be best summarised in the following bullet points:
 *
 * <ul>
 *   <li>JavaFX dialogs can only be closed 'abnormally' (as defined above) in
 *   two situations:
 *     <ol>
 *       <li>When the dialog only has one button, or
 *       <li>When the dialog has multiple buttons, as long as one of them meets
 *       one of the following requirements:
 *       <ol>
 *           <li>The button has a {@link ButtonType} whose {@link ButtonData} is of type
 *           {@link ButtonData#CANCEL_CLOSE}.</li>
 *           <li>The button has a {@link ButtonType} whose {@link ButtonData} returns true
 *           when {@link ButtonData#isCancelButton()} is called.</li>
 *       </ol>
 *     </ol>
 *   <li>In all other situations, the dialog will refuse to respond to all
 *   close requests, remaining open until the user clicks on one of the available
 *   buttons in the {@link DialogPane} area of the dialog.
 *   <li>If a dialog is closed abnormally, and if the dialog contains a button
 *   which meets one of the two criteria above, the dialog will attempt to set
 *   the {@link #resultProperty() result} property to whatever value is returned
 *   from calling the {@link #resultConverterProperty() result converter} with
 *   the first matching {@link ButtonType}.
 *   <li>If for any reason the result converter returns null, or if the dialog
 *   is closed when only one non-cancel button is present, the
 *   {@link #resultProperty() result} property will be null, and the
 *   {@link #showAndWait()} method will return {@link Optional#empty()}. This
 *   later point means that, if you use either of option 2 or option 3 (as
 *   presented earlier in this class documentation), the
 *   {@link Optional#ifPresent(java.util.function.Consumer)} lambda will never
 *   be called, and code will continue executing as if the dialog had not
 *   returned any value at all.
 * </ul>
 *
 * @param <R> The return type of the dialog, via the
 *            {@link #resultProperty() result} property.
 * @see Alert
 * @see TextInputDialog
 * @see ChoiceDialog
 * @since JavaFX 8u40
 */
public class Dialog<R> implements EventTarget {

    /* ************************************************************************
     *
     * Static fields
     *
     **************************************************************************/




    /* ************************************************************************
     *
     * Static methods
     *
     **************************************************************************/



    /* ************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    final FXDialog dialog;

    private boolean isClosing;



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a dialog without a specified owner.
     */
    public Dialog() {
        this.dialog = new HeavyweightDialog(this);
        setDialogPane(new DialogPane());
        initModality(Modality.APPLICATION_MODAL);
    }



    /* ************************************************************************
     *
     * Abstract methods
     *
     **************************************************************************/




    /* ************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /**
     * Shows the dialog but does not wait for a user response (in other words,
     * this brings up a non-blocking dialog). Users of this API must either
     * poll the {@link #resultProperty() result property}, or else add a listener
     * to the result property to be informed of when it is set.
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     */
    public final void show() {
        Toolkit.getToolkit().checkFxUserThread();

        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWING));
        if (Double.isNaN(getWidth()) && Double.isNaN(getHeight())) {
            dialog.sizeToScene();
        }

        dialog.show();

        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWN));
    }

    /**
     * Shows the dialog and waits for the user response (in other words, brings
     * up a blocking dialog, with the returned value the users input).
     * <p>
     * This method must be called on the JavaFX Application thread.
     * Additionally, it must either be called from an input event handler or
     * from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     * </p>
     *
     * @return An {@link Optional} that contains the {@link #resultProperty() result}.
     *         Refer to the {@link Dialog} class documentation for more detail.
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     * @throws IllegalStateException if this method is called during
     *     animation or layout processing.
     */
    public final Optional<R> showAndWait() {
        Toolkit.getToolkit().checkFxUserThread();

        if (!Toolkit.getToolkit().canStartNestedEventLoop()) {
            throw new IllegalStateException("showAndWait is not allowed during animation or layout processing");
        }

        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWING));
        if (Double.isNaN(getWidth()) && Double.isNaN(getHeight())) {
            dialog.sizeToScene();
        }


        // this is slightly odd - we fire the SHOWN event before the show()
        // call, so that users get the event before the dialog blocks
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWN));

        dialog.showAndWait();

        return Optional.ofNullable(getResult());
    }

    /**
     * Closes this {@code Dialog}.
     * This call is equivalent to {@link #hide}.
     */
    public final void close() {
        if (isClosing) return;
        isClosing = true;

        final R result = getResult();

        // if the result is null and we do not have permission to close the
        // dialog, then we cancel the close request before any events are
        // even fired
        if (result == null && ! dialog.requestPermissionToClose(this)) {
            isClosing = false;
            return;
        }

        // if we are here we have permission to close the dialog. However, we
        // may not have a result set to return to the user. Therefore, we need
        // to handle that before the dialog closes (especially in case the
        // dialog is blocking, in which case having a null result is really going
        // to mess up users).
        //
        // In cases where the result is null, and where the dialog has a cancel
        // button, we call into the result converter to see what to do. This is
        // used primarily to handle the requirement that the X button has the
        // same result as clicking the cancel button.
        //
        // A 'cancel button' can mean two different things (although they may
        // be the same thing):
        // 1) A button whose ButtonData is of type CANCEL_CLOSE.
        // 2) A button whose ButtonData returns true for isCancelButton().
        if (result == null) {
            ButtonType cancelButton = null;

            // we do two things here. We are primarily looking for a button with
            // ButtonData.CANCEL_CLOSE. If we find one, we use it as the result.
            // However, if we don't find one, we can also use any button that
            // is a cancel button.
            for (ButtonType button : getDialogPane().getButtonTypes()) {
                ButtonData buttonData = button.getButtonData();
                if (buttonData == null) continue;

                if (buttonData == ButtonData.CANCEL_CLOSE) {
                    cancelButton = button;
                    break;
                }
                if (buttonData.isCancelButton()) {
                    cancelButton = button;
                }
            }

            setResultAndClose(cancelButton, false);
        }

        // start normal closing process
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_HIDING));

        DialogEvent closeRequestEvent = new DialogEvent(this, DialogEvent.DIALOG_CLOSE_REQUEST);
        Event.fireEvent(this, closeRequestEvent);
        if (closeRequestEvent.isConsumed()) {
            isClosing = false;
            return;
        }

        dialog.close();

        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_HIDDEN));

        isClosing = false;
    }

    /**
     * Hides this {@code Dialog}.
     */
    public final void hide() {
        close();
    }

    /**
     * Specifies the modality for this dialog. This must be done prior to making
     * the dialog visible. The modality is one of: Modality.NONE,
     * Modality.WINDOW_MODAL, or Modality.APPLICATION_MODAL.
     *
     * @param modality the modality for this dialog.
     *
     * @throws IllegalStateException if this property is set after the dialog
     * has ever been made visible.
     *
     * @defaultValue Modality.APPLICATION_MODAL
     */
    public final void initModality(Modality modality) {
        dialog.initModality(modality);
    }

    /**
     * Retrieves the modality attribute for this dialog.
     *
     * @return the modality.
     */
    public final Modality getModality() {
        return dialog.getModality();
    }

    /**
     * Specifies the style for this dialog. This must be done prior to making
     * the dialog visible. The style is one of: StageStyle.DECORATED,
     * StageStyle.UNDECORATED, StageStyle.TRANSPARENT, StageStyle.UTILITY,
     * or StageStyle.UNIFIED.
     *
     * @param style the style for this dialog.
     *
     * @throws IllegalStateException if this property is set after the dialog
     * has ever been made visible.
     *
     * @defaultValue StageStyle.DECORATED
     */
    public final void initStyle(StageStyle style) {
        dialog.initStyle(style);
    }

    /**
     * Specifies the owner {@link Window} for this dialog, or null for a top-level,
     * unowned dialog. This must be done prior to making the dialog visible.
     *
     * @param window the owner {@link Window} for this dialog.
     *
     * @throws IllegalStateException if this property is set after the dialog
     * has ever been made visible.
     *
     * @defaultValue null
     */
    public final void initOwner(Window window) {
        dialog.initOwner(window);
    }

    /**
     * Retrieves the owner Window for this dialog, or null for an unowned dialog.
     *
     * @return the owner Window.
     */
    public final Window getOwner() {
        return dialog.getOwner();
    }



    /* ************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    // --- dialog Pane
    /**
     * The root node of the dialog, the {@link DialogPane} contains all visual
     * elements shown in the dialog. As such, it is possible to completely adjust
     * the display of the dialog by modifying the existing dialog pane or creating
     * a new one.
     */
    private ObjectProperty<DialogPane> dialogPane = new SimpleObjectProperty<DialogPane>(this, "dialogPane", new DialogPane()) {
        final InvalidationListener expandedListener = o -> {
            DialogPane dialogPane = getDialogPane();
            if (dialogPane == null) return;

            final Node content = dialogPane.getExpandableContent();
            final boolean isExpanded = content == null ? false : content.isVisible();
            setResizable(isExpanded);

            Dialog.this.dialog.sizeToScene();
        };

        final InvalidationListener headerListener = o -> {
            updatePseudoClassState();
        };

        WeakReference<DialogPane> dialogPaneRef = new WeakReference<>(null);

        @Override
        protected void invalidated() {
            DialogPane oldDialogPane = dialogPaneRef.get();
            if (oldDialogPane != null) {
                // clean up
                oldDialogPane.expandedProperty().removeListener(expandedListener);
                oldDialogPane.headerProperty().removeListener(headerListener);
                oldDialogPane.headerTextProperty().removeListener(headerListener);
                oldDialogPane.setDialog(null);
            }

            final DialogPane newDialogPane = getDialogPane();

            if (newDialogPane != null) {
                newDialogPane.setDialog(Dialog.this);

                // if the buttons change, we dynamically update the dialog
                newDialogPane.getButtonTypes().addListener((ListChangeListener<ButtonType>) c -> {
                    newDialogPane.requestLayout();
                });
                newDialogPane.expandedProperty().addListener(expandedListener);
                newDialogPane.headerProperty().addListener(headerListener);
                newDialogPane.headerTextProperty().addListener(headerListener);

                updatePseudoClassState();
                newDialogPane.requestLayout();
            }

            // push the new dialog down into the implementation for rendering
            dialog.setDialogPane(newDialogPane);

            dialogPaneRef = new WeakReference<DialogPane>(newDialogPane);
        }
    };

    public final ObjectProperty<DialogPane> dialogPaneProperty() {
        return dialogPane;
    }

    public final DialogPane getDialogPane() {
        return dialogPane.get();
    }

    public final void setDialogPane(DialogPane value) {
        dialogPane.set(value);
    }


    // --- content text (forwarded from DialogPane)
    /**
     * A property representing the content text for the dialog pane. The content text
     * is lower precedence than the {@link DialogPane#contentProperty() content node}, meaning
     * that if both the content node and the contentText properties are set, the
     * content text will not be displayed in a default DialogPane instance.
     * @return the property representing the content text for the dialog pane
     */
    public final StringProperty contentTextProperty() {
        return getDialogPane().contentTextProperty();
    }

    /**
     * Returns the currently-set content text for this DialogPane.
     * @return the currently-set content text for this DialogPane
     */
    public final String getContentText() {
        return getDialogPane().getContentText();
    }

    /**
     * Sets the string to show in the dialog content area. Note that the content text
     * is lower precedence than the {@link DialogPane#contentProperty() content node}, meaning
     * that if both the content node and the contentText properties are set, the
     * content text will not be displayed in a default DialogPane instance.
     * @param contentText the string to show in the dialog content area
     */
    public final void setContentText(String contentText) {
        getDialogPane().setContentText(contentText);
    }


    // --- header text (forwarded from DialogPane)
    /**
     * A property representing the header text for the dialog pane. The header text
     * is lower precedence than the {@link DialogPane#headerProperty() header node}, meaning
     * that if both the header node and the headerText properties are set, the
     * header text will not be displayed in a default DialogPane instance.
     * @return a property representing the header text for the dialog pane
     */
    public final StringProperty headerTextProperty() {
        return getDialogPane().headerTextProperty();
    }

    /**
     * Returns the currently-set header text for this DialogPane.
     * @return the currently-set header text for this DialogPane
     */
    public final String getHeaderText() {
        return getDialogPane().getHeaderText();
    }

    /**
     * Sets the string to show in the dialog header area. Note that the header text
     * is lower precedence than the {@link DialogPane#headerProperty() header node}, meaning
     * that if both the header node and the headerText properties are set, the
     * header text will not be displayed in a default DialogPane instance.
     * @param headerText the string to show in the dialog header area
     */
    public final void setHeaderText(String headerText) {
        getDialogPane().setHeaderText(headerText);
    }


    // --- graphic (forwarded from DialogPane)
    /**
     * The dialog graphic, presented either in the header, if one is showing, or
     * to the left of the {@link DialogPane#contentProperty() content}.
     *
     * @return An ObjectProperty wrapping the current graphic.
     */
    public final ObjectProperty<Node> graphicProperty() {
        return getDialogPane().graphicProperty();
    }

    public final Node getGraphic() {
        return getDialogPane().getGraphic();
    }

    /**
     * Sets the dialog graphic, which will be displayed either in the header, if
     * one is showing, or to the left of the {@link DialogPane#contentProperty() content}.
     *
     * @param graphic
     *            The new dialog graphic, or null if no graphic should be shown.
     */
    public final void setGraphic(Node graphic) {
        getDialogPane().setGraphic(graphic);
    }


    // --- result
    private final ObjectProperty<R> resultProperty = new SimpleObjectProperty<R>() {
        protected void invalidated() {
            close();
        }
    };

    /**
     * A property representing what has been returned from the dialog. A result
     * is generated through the {@link #resultConverterProperty() result converter},
     * which is intended to convert from the {@link ButtonType} that the user
     * clicked on into a value of type R. Refer to the {@link Dialog} class
     * JavaDoc for more details.
     * @return a property representing what has been returned from the dialog
     */
    public final ObjectProperty<R> resultProperty() {
        return resultProperty;
    }

    public final R getResult() {
        return resultProperty().get();
    }

    public final void setResult(R value) {
        this.resultProperty().set(value);
    }


    // --- result converter
    private final ObjectProperty<Callback<ButtonType, R>> resultConverterProperty
        = new SimpleObjectProperty<>(this, "resultConverter");

    /**
     * API to convert the {@link ButtonType} that the user clicked on into a
     * result that can be returned via the {@link #resultProperty() result}
     * property. This is necessary as {@link ButtonType} represents the visual
     * button within the dialog, and do not know how to map themselves to a valid
     * result - that is a requirement of the dialog implementation by making use
     * of the result converter. In some cases, the result type of a Dialog
     * subclass is ButtonType (which means that the result converter can be null),
     * but in some cases (where the result type, R, is not ButtonType or Void),
     * this callback must be specified.
     * @return the API to convert the {@link ButtonType} that the user clicked on
     */
    public final ObjectProperty<Callback<ButtonType, R>> resultConverterProperty() {
        return resultConverterProperty;
    }

    public final Callback<ButtonType, R> getResultConverter() {
        return resultConverterProperty().get();
    }

    public final void setResultConverter(Callback<ButtonType, R> value) {
        this.resultConverterProperty().set(value);
    }


    // --- showing
    /**
     * Represents whether the dialog is currently showing.
     * @return the property representing whether the dialog is currently showing
     */
    public final ReadOnlyBooleanProperty showingProperty() {
        return dialog.showingProperty();
    }

    /**
     * Returns whether or not the dialog is showing.
     *
     * @return true if dialog is showing.
     */
    public final boolean isShowing() {
        return showingProperty().get();
    }


    // --- resizable
    /**
     * Represents whether the dialog is resizable.
     * @return the property representing whether the dialog is resizable
     */
    public final BooleanProperty resizableProperty() {
        return dialog.resizableProperty();
    }

    /**
     * Returns whether or not the dialog is resizable.
     *
     * @return true if dialog is resizable.
     */
    public final boolean isResizable() {
        return resizableProperty().get();
    }

    /**
     * Sets whether the dialog can be resized by the user.
     * Resizable dialogs can also be maximized ( maximize button
     * becomes visible)
     *
     * @param resizable true if dialog should be resizable.
     */
    public final void setResizable(boolean resizable) {
        resizableProperty().set(resizable);
    }


    // --- width
    /**
     * Property representing the width of the dialog.
     * @return the property representing the width of the dialog
     */
    public final ReadOnlyDoubleProperty widthProperty() {
        return dialog.widthProperty();
    }

    /**
     * Returns the width of the dialog.
     * @return the width of the dialog
     */
    public final double getWidth() {
        return widthProperty().get();
    }

    /**
     * Sets the width of the dialog.
     * @param width the width of the dialog
     */
    public final void setWidth(double width) {
        dialog.setWidth(width);
    }


    // --- height
    /**
     * Property representing the height of the dialog.
     * @return the property representing the height of the dialog
     */
    public final ReadOnlyDoubleProperty heightProperty() {
        return dialog.heightProperty();
    }

    /**
     * Returns the height of the dialog.
     * @return the height of the dialog
     */
    public final double getHeight() {
        return heightProperty().get();
    }

    /**
     * Sets the height of the dialog.
     * @param height the height of the dialog
     */
    public final void setHeight(double height) {
        dialog.setHeight(height);
    }


    // --- title
    /**
     * Return the titleProperty of the dialog.
     * @return the titleProperty of the dialog
     */
    public final StringProperty titleProperty(){
        return this.dialog.titleProperty();
    }

    /**
     * Return the title of the dialog.
     * @return the title of the dialog
     */
    public final String getTitle(){
        return this.dialog.titleProperty().get();
    }
    /**
     * Change the Title of the dialog.
     * @param title the Title of the dialog
     */
    public final void setTitle(String title){
        this.dialog.titleProperty().set(title);
    }


    // --- x
    public final double getX() {
        return dialog.getX();
    }

    public final void setX(double x) {
        dialog.setX(x);
    }

    /**
     * The horizontal location of this {@code Dialog}. Changing this attribute
     * will move the {@code Dialog} horizontally.
     * @return the horizontal location of this {@code Dialog}
     */
    public final ReadOnlyDoubleProperty xProperty() {
        return dialog.xProperty();
    }

    // --- y
    public final double getY() {
        return dialog.getY();
    }

    public final void setY(double y) {
        dialog.setY(y);
    }

    /**
     * The vertical location of this {@code Dialog}. Changing this attribute
     * will move the {@code Dialog} vertically.
     * @return the vertical location of this {@code Dialog}
     */
    public final ReadOnlyDoubleProperty yProperty() {
        return dialog.yProperty();
    }



    /* *************************************************************************
     *
     * Events
     *
     **************************************************************************/

    private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

    /** {@inheritDoc} */
    @Override public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }

    /**
     * Called just prior to the Dialog being shown.
     */
    private ObjectProperty<EventHandler<DialogEvent>> onShowing;
    public final void setOnShowing(EventHandler<DialogEvent> value) { onShowingProperty().set(value); }
    public final EventHandler<DialogEvent> getOnShowing() {
        return onShowing == null ? null : onShowing.get();
    }
    public final ObjectProperty<EventHandler<DialogEvent>> onShowingProperty() {
        if (onShowing == null) {
            onShowing = new SimpleObjectProperty<EventHandler<DialogEvent>>(this, "onShowing") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(DialogEvent.DIALOG_SHOWING, get());
                }
            };
        }
        return onShowing;
    }

    /**
     * Called just after the Dialog is shown.
     */
    private ObjectProperty<EventHandler<DialogEvent>> onShown;
    public final void setOnShown(EventHandler<DialogEvent> value) { onShownProperty().set(value); }
    public final EventHandler<DialogEvent> getOnShown() {
        return onShown == null ? null : onShown.get();
    }
    public final ObjectProperty<EventHandler<DialogEvent>> onShownProperty() {
        if (onShown == null) {
            onShown = new SimpleObjectProperty<EventHandler<DialogEvent>>(this, "onShown") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(DialogEvent.DIALOG_SHOWN, get());
                }
            };
        }
        return onShown;
    }

    /**
     * Called just prior to the Dialog being hidden.
     */
    private ObjectProperty<EventHandler<DialogEvent>> onHiding;
    public final void setOnHiding(EventHandler<DialogEvent> value) { onHidingProperty().set(value); }
    public final EventHandler<DialogEvent> getOnHiding() {
        return onHiding == null ? null : onHiding.get();
    }
    public final ObjectProperty<EventHandler<DialogEvent>> onHidingProperty() {
        if (onHiding == null) {
            onHiding = new SimpleObjectProperty<EventHandler<DialogEvent>>(this, "onHiding") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(DialogEvent.DIALOG_HIDING, get());
                }
            };
        }
        return onHiding;
    }

    /**
     * Called just after the Dialog has been hidden.
     * When the {@code Dialog} is hidden, this event handler is invoked allowing
     * the developer to clean up resources or perform other tasks when the
     * {@link Alert} is closed.
     */
    private ObjectProperty<EventHandler<DialogEvent>> onHidden;
    public final void setOnHidden(EventHandler<DialogEvent> value) { onHiddenProperty().set(value); }
    public final EventHandler<DialogEvent> getOnHidden() {
        return onHidden == null ? null : onHidden.get();
    }
    public final ObjectProperty<EventHandler<DialogEvent>> onHiddenProperty() {
        if (onHidden == null) {
            onHidden = new SimpleObjectProperty<EventHandler<DialogEvent>>(this, "onHidden") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(DialogEvent.DIALOG_HIDDEN, get());
                }
            };
        }
        return onHidden;
    }

    /**
     * Called when there is an external request to close this {@code Dialog}.
     * The installed event handler can prevent dialog closing by consuming the
     * received event.
     */
    private ObjectProperty<EventHandler<DialogEvent>> onCloseRequest;
    public final void setOnCloseRequest(EventHandler<DialogEvent> value) {
        onCloseRequestProperty().set(value);
    }
    public final EventHandler<DialogEvent> getOnCloseRequest() {
        return (onCloseRequest != null) ? onCloseRequest.get() : null;
    }
    public final ObjectProperty<EventHandler<DialogEvent>>
            onCloseRequestProperty() {
        if (onCloseRequest == null) {
            onCloseRequest = new SimpleObjectProperty<EventHandler<DialogEvent>>(this, "onCloseRequest") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(DialogEvent.DIALOG_CLOSE_REQUEST, get());
                }
            };
        }
        return onCloseRequest;
    }



    /* *************************************************************************
     *
     * Private implementation
     *
     **************************************************************************/

    // This code is called both in the normal and in the abnormal case (i.e.
    // both when a button is clicked and when the user forces a window closed
    // with keyboard OS-specific shortcuts or OS-native titlebar buttons).
    @SuppressWarnings("unchecked")
    void setResultAndClose(ButtonType cmd, boolean close) {
        Callback<ButtonType, R> resultConverter = getResultConverter();

        R priorResultValue = getResult();
        R newResultValue = null;

        if (resultConverter == null) {
            // The choice to cast cmd to R here was a conscious decision, taking
            // into account the choices available to us. Firstly, to summarise the
            // issue, at this point here we have a null result converter, and no
            // idea how to convert the given ButtonType to R. Our options are:
            //
            // 1) We could throw an exception here, but this requires that all
            // developers who create a dialog set a result converter (at least
            // setResultConverter(buttonType -> (R) buttonType)). This is
            // non-intuitive and depends on the developer reading documentation.
            //
            // 2) We could set a default result converter in the resultConverter
            // property that does the identity conversion. This saves people from
            // having to set a default result converter, but it is a little odd
            // that the result converter is non-null by default.
            //
            // 3) We can cast the button type here, which is what we do. This means
            // that the result converter is null by default.
            //
            // In the case of option 1), developers will receive a NPE when the
            // dialog is closed, regardless of how it was closed. In the case of
            // option 2) and 3), the user unfortunately receives a ClassCastException
            // in their code. This is unfortunate as it is not immediately obvious
            // why the ClassCastException occurred, and how to resolve it. However,
            // we decided to take this later approach as it prevents the issue of
            // requiring all custom dialog developers from having to supply their
            // own result converters.
            newResultValue = (R) cmd;
        } else {
            newResultValue = resultConverter.call(cmd);
        }

        setResult(newResultValue);

        // fix for the case where we set the same result as what
        // was already set. We should still close the dialog, but
        // we need to special-case it here, as the result property
        // won't fire any event if the value won't change.
        if (close && priorResultValue == newResultValue) {
            close();
        }
    }




    /* *************************************************************************
     *
     * Stylesheet Handling
     *
     **************************************************************************/
    private static final PseudoClass HEADER_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("header"); //$NON-NLS-1$
    private static final PseudoClass NO_HEADER_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("no-header"); //$NON-NLS-1$

    private void updatePseudoClassState() {
        DialogPane dialogPane = getDialogPane();
        if (dialogPane != null) {
            final boolean hasHeader = getDialogPane().hasHeader();
            dialogPane.pseudoClassStateChanged(HEADER_PSEUDO_CLASS,     hasHeader);
            dialogPane.pseudoClassStateChanged(NO_HEADER_PSEUDO_CLASS, !hasHeader);
        }
    }
}
