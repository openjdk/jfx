/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans;

import javafx.beans.property.ReadOnlyProperty;

public enum BeanErrors {

    BINDING_TARGET_NULL("Binding target cannot be null."),
    BINDING_SOURCE_NULL("Binding source cannot be null."),
    CANNOT_SET_BOUND_PROPERTY("Cannot set the value of a bound property."),
    CANNOT_SET_CONTENT_BOUND_PROPERTY("Cannot set the value of a content-bound property."),
    CANNOT_BIND_PROPERTY_TO_ITSELF("Cannot bind property to itself."),
    CANNOT_UNBIND_PROPERTY_FROM_ITSELF("Cannot unbind property from itself."),
    CANNOT_BIND_COLLECTION_TO_ITSELF("Cannot bind collection to itself."),
    CANNOT_UNBIND_COLLECTION_FROM_ITSELF("Cannot unbind collection from itself."),
    BIND_CONFLICT_BIDIRECTIONAL("Bidirectional binding cannot target a bound property."),
    BIND_CONFLICT_UNIDIRECTIONAL("Cannot bind a property that is targeted by a bidirectional binding."),
    CONTENT_BIND_CONFLICT_BIDIRECTIONAL("Bidirectional content binding cannot target a bound collection."),
    CONTENT_BIND_CONFLICT_UNIDIRECTIONAL("Cannot bind a collection that is targeted by a bidirectional content binding."),
    ILLEGAL_LIST_MODIFICATION("Illegal list modification: Content binding was removed because the lists are out-of-sync."),
    ILLEGAL_SET_MODIFICATION("Illegal set modification: Content binding was removed because the sets are out-of-sync."),
    ILLEGAL_MAP_MODIFICATION("Illegal map modification: Content binding was removed because the maps are out-of-sync.");

    BeanErrors(String message) {
        this.message = message;
    }

    private final String message;

    /**
     * Returns the error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the error message.
     * If the specified value is a {@link ReadOnlyProperty}, the returned message includes the
     * name of the property; otherwise it is identical to {@link #getMessage()}.
     */
    public String getMessage(Object property) {
        return formatPropertyName(property) + message;
    }

    private String formatPropertyName(Object property) {
        if (property instanceof ReadOnlyProperty<?>) {
            Object bean = ((ReadOnlyProperty<?>)property).getBean();
            String name = ((ReadOnlyProperty<?>)property).getName();
            if (bean != null && name != null && !name.isBlank()) {
                return bean.getClass().getName() + "." + name + ": ";
            }
        }

        return "";
    }

}
