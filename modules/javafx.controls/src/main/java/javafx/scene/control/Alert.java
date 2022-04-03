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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * The Alert class subclasses the {@link Dialog} class, and provides support for a number
 * of pre-built dialog types that can be easily shown to users to prompt for a
 * response. Therefore, for many users, the Alert class is the most suited class
 * for their needs (as opposed to using {@link Dialog} directly). Alternatively,
 * users who want to prompt a user for text input or to make a choice from a list
 * of options would be better served by using {@link TextInputDialog} and
 * {@link ChoiceDialog}, respectively.
 *
 * <p>When creating an Alert instance, users must pass in an {@link AlertType}
 * enumeration value. It is by passing in this value that the Alert instance will
 * configure itself appropriately (by setting default values for many of the
 * {@link Dialog} properties, including {@link #titleProperty() title},
 * {@link #headerTextProperty() header}, and {@link #graphicProperty() graphic},
 * as well as the default {@link #getButtonTypes() buttons} that are expected in
 * a dialog of the given type.
 *
 * <p>To instantiate (but not yet show) an Alert, simply use code such as the following:
 * <pre>{@code Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to format your system?");}</pre>
 *
 * <img src="doc-files/Alert.png" alt="Image of the Alert control">
 *
 * <p>Once an Alert is instantiated, we must show it. More often than not, alerts
 * (and dialogs in general) are shown in a modal and blocking fashion. 'Modal'
 * means that the dialog prevents user interaction with the owning application
 * whilst it is showing, and 'blocking' means that code execution stops at the
 * point in which the dialog is shown. This means that you can show a dialog,
 * await the user response, and then continue running the code that directly
 * follows the show call, giving developers the ability to immediately deal with
 * the user input from the dialog (if relevant).
 *
 * <p>JavaFX dialogs are modal by default (you can change this via the
 * {@link #initModality(javafx.stage.Modality)} API). To specify whether you want
 * blocking or non-blocking dialogs, developers simply choose to call
 * {@link #showAndWait()} or {@link #show()} (respectively). By default most
 * developers should choose to use {@link #showAndWait()}, given the ease of
 * coding in these situations. Shown below is three code snippets, showing three
 * equally valid ways of showing the Alert dialog that was specified above:
 *
 * <p><strong>Option 1: The 'traditional' approach</strong>
 * <pre>{@code Optional<ButtonType> result = alert.showAndWait();
 * if (result.isPresent() && result.get() == ButtonType.OK) {
 *     formatSystem();
 * }}</pre>
 *
 * <p><strong>Option 2: The traditional + Optional approach</strong>
 * <pre>{@code alert.showAndWait().ifPresent(response -> {
 *     if (response == ButtonType.OK) {
 *         formatSystem();
 *     }
 * });}</pre>
 *
 * <p><strong>Option 3: The fully lambda approach</strong>
 * <pre>{@code alert.showAndWait()
 *      .filter(response -> response == ButtonType.OK)
 *      .ifPresent(response -> formatSystem());
 * }</pre>
 *
 * <p>There is no better or worse option of the three listed above, so developers
 * are encouraged to work to their own style preferences. The purpose of showing
 * the above is to help introduce developers to the {@link Optional} API, which
 * is new in Java 8 and may be foreign to many developers.
 *
 * @see Dialog
 * @see AlertType
 * @see TextInputDialog
 * @see ChoiceDialog
 * @since JavaFX 8u40
 */
public class Alert extends Dialog<ButtonType> {

    /* ************************************************************************
     *
     * Static enums
     *
     **************************************************************************/

    /**
     * An enumeration containing the available, pre-built alert types that
     * the {@link Alert} class can use to pre-populate various properties.
     *
     * @since JavaFX 8u40
     */
    public static enum AlertType {
        /**
         * The NONE alert type has the effect of not setting any default properties
         * in the Alert.
         */
        NONE,

        /**
         * The INFORMATION alert type configures the Alert dialog to appear in a
         * way that suggests the content of the dialog is informing the user of
         * a piece of information. This includes an 'information' image, an
         * appropriate title and header, and just an OK button for the user to
         * click on to dismiss the dialog.
         */
        INFORMATION,

        /**
         * The WARNING alert type configures the Alert dialog to appear in a
         * way that suggests the content of the dialog is warning the user about
         * some fact or action. This includes a 'warning' image, an
         * appropriate title and header, and just an OK button for the user to
         * click on to dismiss the dialog.
         */
        WARNING,

        /**
         * The CONFIRMATION alert type configures the Alert dialog to appear in a
         * way that suggests the content of the dialog is seeking confirmation from
         * the user. This includes a 'confirmation' image, an
         * appropriate title and header, and both OK and Cancel buttons for the
         * user to click on to dismiss the dialog.
         */
        CONFIRMATION,

        /**
         * The ERROR alert type configures the Alert dialog to appear in a
         * way that suggests that something has gone wrong. This includes an
         * 'error' image, an appropriate title and header, and just an OK button
         * for the user to click on to dismiss the dialog.
         */
        ERROR
    }



    /* ************************************************************************
     *
     * Fields
     *
     **************************************************************************/

    private WeakReference<DialogPane> dialogPaneRef;

    private boolean installingDefaults = false;
    private boolean hasCustomButtons = false;
    private boolean hasCustomTitle = false;
    private boolean hasCustomHeaderText = false;

    private final InvalidationListener headerTextListener = o -> {
        if (!installingDefaults) hasCustomHeaderText = true;
    };

    private final InvalidationListener titleListener = o -> {
        if (!installingDefaults) hasCustomTitle = true;
    };

    private final ListChangeListener<ButtonType> buttonsListener = change -> {
        if (!installingDefaults) hasCustomButtons = true;
    };



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates an alert with the given AlertType (refer to the {@link AlertType}
     * documentation for clarification over which one is most appropriate).
     *
     * <p>By passing in an AlertType, default values for the
     * {@link #titleProperty() title}, {@link #headerTextProperty() headerText},
     * and {@link #graphicProperty() graphic} properties are set, as well as the
     * relevant {@link #getButtonTypes() buttons} being installed. Once the Alert
     * is instantiated, developers are able to modify the values of the alert as
     * desired.
     *
     * <p>It is important to note that the one property that does not have a
     * default value set, and which therefore the developer must set, is the
     * {@link #contentTextProperty() content text} property (or alternatively,
     * the developer may call {@code alert.getDialogPane().setContent(Node)} if
     * they want a more complex alert). If the contentText (or content) properties
     * are not set, there is no useful information presented to end users.
     * @param alertType an alert with the given AlertType
     */
    public Alert(@NamedArg("alertType") AlertType alertType) {
        this(alertType, "");
    }

    /**
     * Creates an alert with the given contentText, ButtonTypes, and AlertType
     * (refer to the {@link AlertType} documentation for clarification over which
     * one is most appropriate).
     *
     * <p>By passing in a variable number of ButtonType arguments, the developer
     * is directly overriding the default buttons that will be displayed in the
     * dialog, replacing the pre-defined buttons with whatever is specified in the
     * varargs array.
     *
     * <p>By passing in an AlertType, default values for the
     * {@link #titleProperty() title}, {@link #headerTextProperty() headerText},
     * and {@link #graphicProperty() graphic} properties are set. Once the Alert
     * is instantiated, developers are able to modify the values of the alert as
     * desired.
     * @param alertType the alert type
     * @param contentText the content text
     * @param buttons the button types
     */
    public Alert(@NamedArg("alertType") AlertType alertType,
                 @NamedArg("contentText") String contentText,
                 @NamedArg("buttonTypes") ButtonType... buttons) {
        super();

        final DialogPane dialogPane = getDialogPane();
        dialogPane.setContentText(contentText);
        getDialogPane().getStyleClass().add("alert");

        dialogPaneRef = new WeakReference<>(dialogPane);

        hasCustomButtons = buttons != null && buttons.length > 0;
        if (hasCustomButtons) {
            for (ButtonType btnType : buttons) {
                dialogPane.getButtonTypes().addAll(btnType);
            }
        }

        setAlertType(alertType);

        // listening to property changes on Dialog and DialogPane
        dialogPaneProperty().addListener(o -> updateListeners());
        titleProperty().addListener(titleListener);
        updateListeners();
    }



    /* ************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    /**
     * When creating an Alert instance, users must pass in an {@link AlertType}
     * enumeration value. It is by passing in this value that the Alert instance will
     * configure itself appropriately (by setting default values for many of the
     * {@link Dialog} properties, including {@link #titleProperty() title},
     * {@link #headerTextProperty() header}, and {@link #graphicProperty() graphic},
     * as well as the default {@link #getButtonTypes() buttons} that are expected in
     * a dialog of the given type.
     */
    // --- alertType
    private final ObjectProperty<AlertType> alertType = new SimpleObjectProperty<AlertType>(null) {
        final String[] styleClasses = new String[] { "information", "warning", "error", "confirmation" };

        @Override
        protected void invalidated() {
            String newTitle = "";
            String newHeader = "";
//            Node newGraphic = null;
            String styleClass = "";
            ButtonType[] newButtons = new ButtonType[] { ButtonType.OK };
            switch (getAlertType()) {
                case NONE: {
                    newButtons = new ButtonType[] { };
                    break;
                }
                case INFORMATION: {
                    newTitle = ControlResources.getString("Dialog.info.title");
                    newHeader = ControlResources.getString("Dialog.info.header");
                    styleClass = "information";
                    break;
                }
                case WARNING: {
                    newTitle = ControlResources.getString("Dialog.warning.title");
                    newHeader = ControlResources.getString("Dialog.warning.header");
                    styleClass = "warning";
                    break;
                }
                case ERROR: {
                    newTitle = ControlResources.getString("Dialog.error.title");
                    newHeader = ControlResources.getString("Dialog.error.header");
                    styleClass = "error";
                    break;
                }
                case CONFIRMATION: {
                    newTitle = ControlResources.getString("Dialog.confirm.title");
                    newHeader = ControlResources.getString("Dialog.confirm.header");
                    styleClass = "confirmation";
                    newButtons = new ButtonType[] { ButtonType.OK, ButtonType.CANCEL };
                    break;
                }
            }

            installingDefaults = true;
            if (!hasCustomTitle) setTitle(newTitle);
            if (!hasCustomHeaderText) setHeaderText(newHeader);
            if (!hasCustomButtons) getButtonTypes().setAll(newButtons);

            // update the style class based on the alert type. We use this to
            // specify the default graphic to use (i.e. via CSS).
            DialogPane dialogPane = getDialogPane();
            if (dialogPane != null) {
                List<String> toRemove = new ArrayList<>(Arrays.asList(styleClasses));
                toRemove.remove(styleClass);
                dialogPane.getStyleClass().removeAll(toRemove);
                if (! dialogPane.getStyleClass().contains(styleClass)) {
                    dialogPane.getStyleClass().add(styleClass);
                }
            }

            installingDefaults = false;
        }
    };

    public final AlertType getAlertType() {
        return alertType.get();
    }

    public final void setAlertType(AlertType alertType) {
        this.alertType.setValue(alertType);
    }

    public final ObjectProperty<AlertType> alertTypeProperty() {
        return alertType;
    }


    /**
     * Returns an {@link ObservableList} of all {@link ButtonType} instances that
     * are currently set inside this Alert instance. A ButtonType may either be one
     * of the pre-defined types (e.g. {@link ButtonType#OK}), or it may be a
     * custom type (created via the {@link ButtonType#ButtonType(String)} or
     * {@link ButtonType#ButtonType(String, javafx.scene.control.ButtonBar.ButtonData)}
     * constructors.
     *
     * <p>Readers should refer to the {@link ButtonType} class documentation for more details,
     * but at a high level, each ButtonType instance is converted to
     * a Node (although most commonly a {@link Button}) via the (overridable)
     * {@link DialogPane#createButton(ButtonType)} method on {@link DialogPane}.
     * @return an {@link ObservableList} of all {@link ButtonType} instances that
     * are currently set inside this Alert instance
     */
    // --- buttonTypes
    public final ObservableList<ButtonType> getButtonTypes() {
        return getDialogPane().getButtonTypes();
    }



    /* ************************************************************************
     *
     * Private Implementation
     *
     **************************************************************************/

    private void updateListeners() {
        DialogPane oldPane = dialogPaneRef.get();

        if (oldPane != null) {
            oldPane.headerTextProperty().removeListener(headerTextListener);
            oldPane.getButtonTypes().removeListener(buttonsListener);
        }

        // listen to changes to properties that would be changed by alertType being
        // changed, so that we only change values that are still at their default
        // value (i.e. the user hasn't changed them, so we are free to set them
        // to a new default value when the alertType changes).

        DialogPane newPane = getDialogPane();
        if (newPane != null) {
            newPane.headerTextProperty().addListener(headerTextListener);
            newPane.getButtonTypes().addListener(buttonsListener);
        }

        dialogPaneRef = new WeakReference<DialogPane>(newPane);
    }
}
