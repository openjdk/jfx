/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import com.sun.javafx.util.Utils;
import javafx.scene.control.skin.ButtonBarSkin;
import javafx.css.StyleableProperty;

import java.util.Map;

/**
 * A ButtonBar is essentially a {@link HBox}, with the additional functionality
 * for operating system specific button placement. In other words, any Node may
 * be annotated (via the {@link ButtonBar#setButtonData(Node, ButtonData)}
 * method, placed inside a ButtonBar (via the {@link #getButtons()} list), and will
 * then be positioned relative to all other nodes in the button list based on their
 * annotations, as well as the overarching
 * {@link #buttonOrderProperty() button order} specified for the ButtonBar.
 *
 * <h2>Uniform button sizing</h2>
 * <p>By default all buttons are uniformly sized in a ButtonBar, meaning that all
 * buttons take the width of the widest button. It is possible to opt-out of this
 * on a per-button basis, but calling the {@link #setButtonUniformSize(Node, boolean)} method with
 * a boolean value of false.
 *
 * <p>If a button is excluded from uniform sizing, it is both excluded from
 * being resized away from its preferred size, and also excluded from the
 * measuring process, so its size will not influence the maximum size calculated
 * for all buttons in the ButtonBar.
 *
 * <h2>Screenshots</h2>
 * <p>Because a ButtonBar comes with built-in support for Windows, Mac OS
 * and Linux, there are three screenshots shown below, with the same buttons
 * laid out on each of the three operating systems.
 *
 * <p>
 * <strong>Windows:</strong><p><img src="doc-files/buttonBar-windows.png" alt=""></p>
 * <strong>Mac OS:</strong><p><img src="doc-files/buttonBar-mac.png" alt=""></p>
 * <strong>Linux:</strong><p><img src="doc-files/buttonBar-linux.png" alt=""></p>
 *
 * <h2>Code Samples</h2>
 * <p>Instantiating and using the ButtonBar is simple, simply do the following:
 *
 * <pre>
 * {@code
 * // Create the ButtonBar instance
 * ButtonBar buttonBar = new ButtonBar();
 *
 * // Create the buttons to go into the ButtonBar
 * Button yesButton = new Button("Yes");
 * ButtonBar.setButtonData(yesButton, ButtonData.YES);
 *
 * Button noButton = new Button("No");
 * ButtonBar.setButtonData(noButton, ButtonData.NO);
 *
 * // Add buttons to the ButtonBar
 * buttonBar.getButtons().addAll(yesButton, noButton);
 * }</pre>
 *
 * <p>The code sample above will position the Yes and No buttons relative to the
 * users operating system. This means that on Windows and Linux the Yes button
 * will come before the No button, whereas on Mac OS it'll be No and then Yes.
 *
 * <p>In most cases the OS-specific layout is the best choice, but in cases
 * where you want a custom layout, this is achieved be modifying the
 * {@link #buttonOrderProperty() button order property}. These are cryptic-looking
 * strings that are shorthand representations for the button order. The built-in
 * orders for Windows, Mac OS and Linux are:
 *
 * <table>
 * <caption>ButtonBar Layout Table</caption>
 *   <tr>
 *     <th scope="col">Operating System</th>
 *     <th scope="col">Button Order</th>
 *   </tr>
 *   <tr>
 *     <th scope="row">Windows</th>
 *     <td>L_E+U+FBXI_YNOCAH_R</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">Mac OS</th>
 *     <td>L_HE+U+FBIX_NCYOA_R</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">Linux</th>
 *     <td>L_HE+UNYACBXIO_R</td>
 *   </tr>
 * </table>
 *
 * <p>You should refer to the {@link ButtonData} enumeration for a description of
 * what each of these characters mean. However, if your ButtonBar only consisted
 * of {@link ButtonData#YES} and {@link ButtonData#NO} buttons, you always
 * wanted the yes buttons before the no buttons, and you wanted the buttons to
 * be {@link ButtonData#BIG_GAP right-aligned}, you could do the following:
 *
 * <pre>
 * {@code
 * // Create the ButtonBar instance
 * ButtonBar buttonBar = new ButtonBar();
 *
 * // Set the custom button order
 * buttonBar.setButtonOrder("+YN");
 * }</pre>
 *
 * @see ButtonData
 * @since JavaFX 8u40
 */
public class ButtonBar extends Control {

    // TODO add support for BUTTON_ORDER_NONE
    // TODO test and document what happens with unexpected button order strings

    /* ************************************************************************
     *
     * Static fields
     *
     **************************************************************************/

    /**
     * The default button ordering on Windows.
     */
    public static final String BUTTON_ORDER_WINDOWS = "L_E+U+FBXI_YNOCAH_R"; //$NON-NLS-1$

    /**
     * The default button ordering on Mac OS.
     */
    public static final String BUTTON_ORDER_MAC_OS  = "L_HE+U+FBIX_NCYOA_R"; //$NON-NLS-1$

    /**
     * The default button ordering on Linux (specifically, GNOME).
     */
    public static final String BUTTON_ORDER_LINUX   = "L_HE+UNYACBXIO_R"; //$NON-NLS-1$

    /**
     * A button ordering string that specifies there is no button ordering. In
     * other words, buttons will be placed in the order that exist in the
     * {@link #getButtons()} list. The only aspect of layout that makes this
     * different than using an HBox is that the buttons are right-aligned.
     */
    public static final String BUTTON_ORDER_NONE   = ""; //$NON-NLS-1$



    /* ************************************************************************
     *
     * Static enumerations
     *
     **************************************************************************/

    /**
     * An enumeration of all available button data annotations. By annotating
     * every button in a {@link ButtonBar} with one of these annotations, the
     * buttons will be appropriately positioned relative to all other buttons in
     * the ButtonBar.
     *
     * <p>For details on the button order code for each ButtonData, refer to
     * the javadoc comment.
     *
     * @since JavaFX 8u40
     */
    public static enum ButtonData {
        /**
         * Buttons with this style tag will statically end up on the left end of the bar.
         *
         * <p><strong>Button order code:</strong> L
         */
        LEFT("L",false,false), //$NON-NLS-1$

        /**
         * Buttons with this style tag will statically end up on the right end of the bar.
         *
         * <p><strong>Button order code:</strong> R
         */
        RIGHT("R", false, false), //$NON-NLS-1$

        /**
         * A tag for the "help" button that normally is supposed to be on the right.
         *
         * <p><strong>Button order code:</strong> H
         */
        HELP("H", false, false ), //$NON-NLS-1$

        /**
         * A tag for the "help2" button that normally is supposed to be on the left.
         *
         * <p><strong>Button order code:</strong> E
         */
        HELP_2("E", false, false), //$NON-NLS-1$

        /**
         * A tag for the "yes" button.
         *
         * <p><strong>Is default button:</strong> True
         * <p><strong>Button order code:</strong> Y
         */
        YES("Y", false, true), //$NON-NLS-1$

        /**
         * A tag for the "no" button.
         *
         * <p><strong>Is cancel button:</strong> True
         * <p><strong>Button order code:</strong> N
         */
        NO("N", true, false), //$NON-NLS-1$

        /**
         * A tag for the "next" or "forward" button.
         *
         * <p><strong>Is default button:</strong> True
         * <p><strong>Button order code:</strong> X
         */
        NEXT_FORWARD("X", false, true), //$NON-NLS-1$

        /**
         * A tag for the "back" or "previous" button.
         *
         * <p><strong>Button order code:</strong> B
         */
        BACK_PREVIOUS("B", false, false), //$NON-NLS-1$

        /**
         * A tag for the "finish".
         *
         * <p><strong>Is default button:</strong> True
         * <p><strong>Button order code:</strong> I
         */
        FINISH("I", false, true), //$NON-NLS-1$

        /**
         * A tag for the "apply" button.
         *
         * <p><strong>Button order code:</strong> A
         */
        APPLY("A", false, false), //$NON-NLS-1$

        /**
         * A tag for the "cancel" or "close" button.
         *
         * <p><strong>Is cancel button:</strong> True
         * <p><strong>Button order code:</strong> C
         */
        CANCEL_CLOSE("C", true, false), //$NON-NLS-1$

        /**
         * A tag for the "ok" or "done" button.
         *
         * <p><strong>Is default button:</strong> True
         * <p><strong>Button order code:</strong> O
         */
        OK_DONE("O", false, true), //$NON-NLS-1$

        /**
         * All Uncategorized, Other, or "Unknown" buttons. Tag will be "other".
         *
         * <p><strong>Button order code:</strong> U
         */
        OTHER("U", false, false), //$NON-NLS-1$


        /**
         * A glue push gap that will take as much space as it can and at least
         * an "unrelated" gap. (Platform dependent)
         *
         * <p><strong>Button order code:</strong> +
         */
        BIG_GAP("+", false, false), //$NON-NLS-1$

        /**
         * An "unrelated" gap. (Platform dependent)
         *
         * <p><strong>Button order code:</strong> _ (underscore)
         */
        SMALL_GAP("_", false, false); //$NON-NLS-1$

        private final String typeCode;

        private final boolean cancelButton;
        private final boolean defaultButton;

        private ButtonData(String type, boolean cancelButton, boolean defaultButton) {
            this.typeCode = type;
            this.cancelButton = cancelButton;
            this.defaultButton = defaultButton;
        }

        /**
         * Returns the single character code used to represent the ButtonData
         * annotation in the {@link ButtonBar#buttonOrderProperty() button order}
         * string.
         * @return the single character code used to represent the ButtonData
         * annotation
         */
        public String getTypeCode() {
            return typeCode;
        }

        /**
         * Indicates whether buttons created from the ButtonData enumeration
         * should be the 'cancel' button in the user interface. This typically
         * means that the button will respond to the escape key press, even if
         * the button does not have focus.
         *
         * <p>ButtonData enumeration values that can be the cancel button have a
         * comment stating this in their javadoc.
         * @return true if this is a 'cancel' button
         */
        public final boolean isCancelButton() {
            return cancelButton;
        }

        /**
         * Indicates whether buttons created from the ButtonData enumeration
         * should be the 'default' button in the user interface. This typically
         * means that the button will respond to enter key presses, even if the
         * button does not have focus.
         *
         * <p>ButtonData enumeration values that can be the default button have
         * a comment stating this in their javadoc.
         * @return true if this is a 'default' button
         */
        public final boolean isDefaultButton() {
            return defaultButton;
        }
    }


    /**
     * Sets the given ButtonData on the given button. If this button is
     * subsequently placed in a {@link ButtonBar} it will be placed in the
     * correct position relative to all other buttons in the bar.
     *
     * @param button The button to annotate with the given {@link ButtonData} value.
     * @param buttonData The ButtonData to designate the button as.
     */
    public static void setButtonData(Node button, ButtonData buttonData) {
        final Map<Object,Object> properties = button.getProperties();
        final ObjectProperty<ButtonData> property =
                (ObjectProperty<ButtonData>) properties.getOrDefault(
                        Properties.BUTTON_DATA_PROPERTY,
                        new SimpleObjectProperty<>(button, "buttonData", buttonData));

        property.set(buttonData);
        properties.putIfAbsent(Properties.BUTTON_DATA_PROPERTY, property);
    }

    /**
     * Returns the previously set ButtonData property on the given button. If this
     * was never set, this method will return null.
     *
     * @param button The button to return the previously set ButtonData for.
     * @return the previously set ButtonData property on the given button
     */
    public static ButtonData getButtonData(Node button) {
        final Map<Object,Object> properties = button.getProperties();
        if (properties.containsKey(Properties.BUTTON_DATA_PROPERTY)) {
            ObjectProperty<ButtonData> property = (ObjectProperty<ButtonData>) properties.get(Properties.BUTTON_DATA_PROPERTY);
            return property == null ? null : property.get();
        }
        return null;
    }

    /**
     * By default all buttons are uniformly sized in a ButtonBar, meaning that all
     * buttons take the width of the widest button. It is possible to opt-out of this
     * on a per-button basis, but calling the setButtonUniformSize method with
     * a boolean value of false.
     *
     * <p>If a button is excluded from uniform sizing, it is both excluded from
     * being resized away from its preferred size, and also excluded from the
     * measuring process, so its size will not influence the maximum size calculated
     * for all buttons in the ButtonBar.
     *
     * @param button The button to include / exclude from uniform sizing.
     * @param uniformSize Boolean true to force uniform sizing on the button,
     *        false to exclude the button from uniform sizing.
     */
    public static void setButtonUniformSize(Node button, boolean uniformSize) {
        // we store the false, but remove the true (as the isButtonUniformSize
        // method returns true by default)
        if (uniformSize) {
            button.getProperties().remove(Properties.BUTTON_SIZE_INDEPENDENCE);
        } else {
            button.getProperties().put(Properties.BUTTON_SIZE_INDEPENDENCE, uniformSize);
        }
    }

    /**
     * Returns whether the given node is part of the uniform sizing calculations
     * or not. By default all nodes that have not opted out (via
     * {@link #setButtonUniformSize(Node, boolean)}) will return true here.
     * @param button the button
     * @return true if button is part of the uniform sizing calculations
     */
    public static boolean isButtonUniformSize(Node button) {
        return (boolean) button.getProperties().getOrDefault(Properties.BUTTON_SIZE_INDEPENDENCE, true);
    }



    /* ************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    private ObservableList<Node> buttons = FXCollections.<Node>observableArrayList();



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a default ButtonBar instance using the default properties for
     * the users operating system.
     */
    public ButtonBar() {
        this(null);
    }

    /**
     * Creates a ButtonBar with the given button order (refer to
     * {@link #buttonOrderProperty()} for more information).
     *
     * @param buttonOrder The button order to use in this button bar instance.
     */
    public ButtonBar(final String buttonOrder) {
        getStyleClass().add("button-bar"); //$NON-NLS-1$

        // we allow for the buttons inside the ButtonBar to be focus traversable,
        // but the ButtonBar itself is not.
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not
        // override. Initializing focusTraversable by calling set on the
        // CssMetaData ensures that css will be able to override the value.
        ((StyleableProperty<Boolean>)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);

        final boolean buttonOrderEmpty = buttonOrder == null || buttonOrder.isEmpty();

        if (Utils.isMac()) {
            setButtonOrder(buttonOrderEmpty ? BUTTON_ORDER_MAC_OS : buttonOrder);
            setButtonMinWidth(70);
        } else if (Utils.isUnix()) {
            setButtonOrder(buttonOrderEmpty ? BUTTON_ORDER_LINUX : buttonOrder);
            setButtonMinWidth(85);
        } else {
            // windows by default
            setButtonOrder(buttonOrderEmpty ? BUTTON_ORDER_WINDOWS : buttonOrder);
            setButtonMinWidth(75);
        }
    }



    /* ************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /**
     * {@inheritDoc}
     */
    @Override protected Skin<?> createDefaultSkin() {
        return new ButtonBarSkin(this);
    }

    /**
     * Placing buttons inside this ObservableList will instruct the ButtonBar
     * to position them relative to each other based on their specified
     * {@link ButtonData}. To set the ButtonData for a button, simply call
     * {@link ButtonBar#setButtonData(Node, ButtonData)}, passing in the
     * relevant ButtonData.
     *
     * @return A list containing all buttons currently in the button bar, and
     *      allowing for further buttons to be added or removed.
     */
    public final ObservableList<Node> getButtons() {
        return buttons;
    }



    /* ************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    // --- Button order
    /**
     * The order for the typical buttons in a standard button bar. It is
     * one letter per {@link ButtonData} enumeration value. Default button orders
     * for operating systems are also available: {@link #BUTTON_ORDER_WINDOWS},
     * {@link #BUTTON_ORDER_MAC_OS}, and {@link #BUTTON_ORDER_LINUX}.
     * @return the button order property
     */
    public final StringProperty buttonOrderProperty() {
        return buttonOrderProperty;
    }
    private final StringProperty buttonOrderProperty =
            new SimpleStringProperty(this, "buttonOrder"); //$NON-NLS-1$

    /**
     * Sets the {@link #buttonOrderProperty() button order}
     * @param buttonOrder The currently set button order, which by default will
     *      be the OS-specific button order.
     */
    public final void setButtonOrder(String buttonOrder) {
        buttonOrderProperty.set(buttonOrder);
    }

    /**
     * Returns the current {@link #buttonOrderProperty() button order}.
     * @return The current {@link #buttonOrderProperty() button order}.
     */
    public final String getButtonOrder() {
        return buttonOrderProperty.get();
    }


    // --- button min width
    /**
     * Specifies the minimum width of all buttons placed in this button bar.
     * @return the minimum width property
     */
    public final DoubleProperty buttonMinWidthProperty() {
        return buttonMinWidthProperty;
    }
    private final DoubleProperty buttonMinWidthProperty =
            new SimpleDoubleProperty(this, "buttonMinWidthProperty"); //$NON-NLS-1$

    /**
     * Sets the minimum width of all buttons placed in this button bar.
     * @param value the minimum width value
     */
    public final void setButtonMinWidth(double value) {
        buttonMinWidthProperty.set(value);
    }

    /**
     * Returns the minimum width of all buttons placed in this button bar.
     * @return the minimum width value
     */
    public final double getButtonMinWidth() {
        return buttonMinWidthProperty.get();
    }



    /* ************************************************************************
     *
     * Implementation
     *
     **************************************************************************/

    /**
     * Returns the initial focus traversable state of this control, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * is overridden as by default UI controls have focus traversable set to true,
     * but that is not appropriate for this control.
     *
     * @since 9
     */
    @Override protected Boolean getInitialFocusTraversable() {
        return Boolean.FALSE;
    }



    /* ************************************************************************
     *
     * Support classes / enums
     *
     **************************************************************************/

}
