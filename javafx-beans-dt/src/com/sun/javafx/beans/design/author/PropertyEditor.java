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

import com.sun.javafx.beans.design.tool.DesignProperty;
import javafx.scene.Node;
import javafx.util.StringConverter;

/**
 * <p>This interface is used by the component author to specify a custom UI for
 * editing a specific DesignProperty. This is useful if a PropertyEditor author
 * wishes to display a list of instances within scope, or wishes to drill-in to
 * the object that this property is being set on, or simply wants to specify
 * some other method for editing the property (such as input constrained
 * text entry).</p>
 *
 * <p>NOTE: This class is expected to directly manipulate the property using the
 * passed DesignProperty instance.  All type conversions to and from String are
 * done using a StringConverter object.</p>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to
 * be implemented by the component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface PropertyEditor {

    /**
     * <p>When the PropetyEditor is being invoked, the matching DesignProperty
     * will be passed in for context. This can be used to dig into the
     * DesignBean being edited and its surrounding context. When a
     * PropertyEditor is "finished", this method is called with
     * <code>null</code>.</p>
     *
     * <p>NOTE: Any manipulation of the passed DesignProperty will
     * *immediately* affect the underlying value and produce the appropriate
     * code generation.</p>
     *
     * @param prop The DesignProperty currently being edited by this
     *        PropertyEditor - use this to modify the property as desired.
     */
    public void setDesignProperty(DesignProperty prop);

    //----------------------------------------------------------------------

    /**
     * Returns true if this PropertyEditor has a custom converter for the
     * property. If no custom StringConverter is supplied, the property type
     * is used to find one via the
     * <code>PropertyConverterManager.findConverter(Class type)</code> method.
     *
     * @return boolean
     */
    public boolean hasCustomConverter();

    /**
     * Returns a custom StringConverter for this property. If no custom
     * StringConverter is supplied, the property type is used to find one via
     * the <code>PropertyConverterManager.findConverter(Class type)</code>
     * method.
     *
     * @return StringConverter
     */
    public StringConverter getStringConverter();

    //----------------------------------------------------------------------

    /**
     * Specifies whether this property editor wishes to provide custom painting
     * of this property value.
     *
     * @return True if the class will honor the customPaintValue method.
     */
    public boolean isCustomPaintable();

    /**
     * <p>Gets a node (or hierarchy) which is used both for display and for
     * editing of the property. The node should be resizable, since the tool
     * will want to resize it to fit within some allocated space.</p>
     *
     * <p>If the PropertyEditor doesn't honor paint requests
     * (see isCustomPaintable) this method should return null.</p>
     */
    public Node getCustomEditor();

    //----------------------------------------------------------------------

    /**
     * Returns <code>true</code> if the user should be allowed to type a new
     * string value in to a property sheet. This includes properties with tag
     * items.
     *
     * @return boolean
     */
    public boolean isEditableAsString();

    //----------------------------------------------------------------------

//    public boolean isValueToggle();
//    public Result toggleValue();

//    public boolean hasDoubleClickAction();
//    public Result doubleClickAction();

    //----------------------------------------------------------------------

    /**
     * Returns true if this PropertyEditor has tag items (DisplayActions) to
     * display in a drop-down list.
     *
     * @return boolean
     */
    public boolean hasTagItems();

    /**
     * Returns an array of DisplayAction that will be shown in a drop-down list.
     */
    public DisplayAction[] getTagItems();

    //----------------------------------------------------------------------

    /**
     * Determines whether this property editor supplies a pop-up customizer.
     *
     * @return true if this property editor can provide a pop-up customizer.
     */
    public boolean hasCustomizer();

    /**
     * A PropertyEditor may choose to make available a full customizer that may
     * access the property (or any other property) with a complex UI.
     *
     * @return A Customizer that will allow a human to directly edit the current
     *         property value. May be null if this is not supported.
     */
    public Customizer getCustomizer();
}
