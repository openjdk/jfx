/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder.web;

// TODO: remove this class as part of fixing JDK-8089907.

/**
Builder class for javafx.scene.Parent
@see javafx.scene.Parent
@deprecated This class is deprecated and will be removed in the next version
* @since JavaFX 2.0
*/
@Deprecated
public abstract class ParentBuilder<B extends ParentBuilder<B>> extends NodeBuilder<B> {
    protected ParentBuilder() {
    }


    private int __set;
    public void applyTo(javafx.scene.Parent x) {
        super.applyTo(x);
        int set = __set;
        if ((set & (1 << 1)) != 0) x.getStylesheets().addAll(this.stylesheets);
    }

    private java.util.Collection<? extends java.lang.String> stylesheets;
    /**
    Add the given items to the List of items in the {@link javafx.scene.Parent#getStylesheets() stylesheets} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B stylesheets(java.util.Collection<? extends java.lang.String> x) {
        this.stylesheets = x;
        __set |= 1 << 1;
        return (B) this;
    }

    /**
    Add the given items to the List of items in the {@link javafx.scene.Parent#getStylesheets() stylesheets} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    public B stylesheets(java.lang.String... x) {
        return stylesheets(java.util.Arrays.asList(x));
    }

}
