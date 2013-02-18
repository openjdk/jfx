/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.design.DisplayItem;
import com.sun.javafx.beans.design.tool.DesignBean;
import com.sun.javafx.beans.design.tool.DesignProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;

/**
 * <p>The Customizer interface describes a context-aware customizer for a
 * JavaFX Bean. A component author may wish to supply a customizer for their
 * JavaFX Bean, which is a dialog that pops up and provides a rich set of UI
 * controls to manipulate the configuration of the entire JavaFX Bean. This
 * type of Customizer has significantly more access to the context that the
 * JavaFX Bean is being designed in, and thus allows for much greater
 * functionality.</p>
 *
 * TODO the following description should be updated to include the notion
 * of pop-overs
 *
 * <p>If a Customizer is apply capable (isApplyCapable() returns true), the host
 * dialog will have three buttons: "OK", "Apply", and "Cancel" (and possibly
 * "Help" if there is a helpKey). The 'isModified' method will be called each
 * time a PropertyChangeEvent is fired to check if the "Apply" button should be
 * enabled. When the user clicks "OK" or "Apply", the 'applyChanges' method is
 * called.  This implies that manipulations in the dialog are not directly
 * affecting the DesignBean. The DesignBean should not be touched until
 * 'applyChanges' has been called.</p>
 *
 * <p>If a Customizer is NOT apply capable (isApplyCapable() returns false), the
 * host dialog will only have one button: "Done" (and possibly "Help" if there
 * is a helpKey). The DesignBean may be manipulated at will in this dialog, as
 * it is considered to be non-stateful.  When the user clicks "Done", the
 * 'applyChanges' method will be called.</p>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to
 * be implemented by the component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface Customizer extends DisplayItem {

    /**
     * Returns a UI node (should be resizable) to be displayed to the user.
     * The passed in DesignBean is the design-time proxy representing the
     * JavaFX Bean being customized. This method (or
     * getCustomizerPanel(DesignProperty)) will be called *every* time the
     * Customizer is invoked.
     *
     * @param designBean the DesignBean to be customized
     * @return a resizable UI node to display to the user
     * @see #getCustomizerPanel(DesignProperty)
     */
    public Node getCustomizerPanel(DesignBean designBean);

    /**
     * Returns a UI node (should be resizable) to be displayed to the user. The
     * passed in DesignProperty is the design-time proxy representing the
     * JavaFX Bean property being customized. This method (or
     * getCustomizerPanel(DesignBean)) will be called *every* time the
     * Customizer is invoked.
     *
     * @param designProperty the DesignProperty to be customized
     * @return a resizable node to display to the user
     * @see #getCustomizerPanel(DesignBean)
     */
    public Node getCustomizerPanel(DesignProperty designProperty);

    /**
     * <p>If a Customizer is apply capable (isApplyCapable() returns true), the
     * host dialog will have three buttons: "OK", "Apply", and "Cancel" (and
     * possibly "Help" if there is a helpKey). The 'isModified' method will be
     * called each time a PropertyChangeEvent is fired to check if the "Apply"
     * button should be enabled.  When the user clicks "OK" or "Apply", the
     * 'applyChanges' method is called.  This implies that manipulations in the
     * dialog are not directly affecting the DesignBean. The DesignBean should
     * not be touched until 'applyChanges' has been called.</p>
     *
     * <p>If a Customizer is NOT apply capable (isApplyCapable() returns false),
     * the host dialog will only have one button: "Done" (and possibly "Help" if
     * there is a helpKey).  The DesignBean may be manipulated at will in this
     * dialog, as it is considered to be non-stateful.  When the user clicks
     * "Done", the 'applyChanges' method will be called.</p>
     *
     * @return returns <code>true</code> if the customizer is stateful and is
     *         capable of handling an apply operation
     * @see #isModified()
     * @see #applyChanges()
     */
    public boolean isApplyCapable();

    /**
     * Returns <code>true</code> if the customizer is in an edited state - to
     * notify the customizer dialog that the "Apply" button should be activated.
     *
     * @return returns <code>true</code> if the customizer is in an edited
     *         state, <code>false</code> if not
     */
    public boolean isModified();

    /**
     * Gets a reference to the modified Property.
     *
     * @return A reference to the modified property.
     */
    public ReadOnlyBooleanProperty modifiedProperty();

    /**
     * Notifies the customizer that the user has clicked "OK" or "Apply" and the
     * customizer should commit it's changes to the DesignBean.
     *
     * @return A Result object, indicating success or failure, and optionally
     *         including messages for the user
     */
    public Result applyChanges();
}
