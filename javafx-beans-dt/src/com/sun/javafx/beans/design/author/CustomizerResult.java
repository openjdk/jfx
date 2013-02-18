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

import com.sun.javafx.beans.design.tool.DesignBean;
import com.sun.javafx.beans.design.tool.DesignProperty;


/**
 * <p>The CustomizerResult is a special Result object that triggers the customizer dialog to be
 * displayed.  This Result object can be returned from any component-author operation and thus pop
 * up a customizer dialog.  Common uses include a context-menu item, which allows a right-click menu
 * item to launch a customizer, and a return value from a beanCreatedSetup method to pop up the
 * customizer dialog just as a component is dropped from the palette.</p>
 *
 * @todo Create a Builder?
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see Customizer
 * @see Result
 */
public class CustomizerResult extends Result {
    /**
     * Storage for the 'customizeBean' property
     */
    protected DesignBean customizeBean;

    /**
     * Storage for the 'customizeBean' property
     */
    protected DesignProperty customizeProperty;

    /**
     * Storage for the 'customizer' property
     */
    protected Customizer customizer;

    /**
     * Constructs a CustomizerResult without a DesignBean, DesignProperty or Customizer2 (which
     * must be specified via 'setDesignBean(...)' and/or 'setDesignProperty(...)' and
     * 'setCustomizer(...)' before being returned).
     */
    public CustomizerResult() {
        super(true);
    }

    /**
     * Constructs a CustomizerResult with the specified DesignBean and no Customizer2 (which must
     * be specified via 'setCustomizer2' before being returned).
     */
    public CustomizerResult(final DesignBean customizeBean) {
        super(true);
        this.customizeBean = customizeBean;
    }

    /**
     * Constructs a CustomizerResult with the specified DesignProperty and no Customizer2 (which
     * must be specified via 'setCustomizer2' before being returned).
     */
    public CustomizerResult(final DesignProperty customizeProperty) {
        super(true);
        this.customizeProperty = customizeProperty;
    }

    /**
     * Constructs a CustomizerResult with the specified DesignBean and Customizer2
     */
    public CustomizerResult(final DesignBean customizeBean, final Customizer customizer) {
        this(customizeBean);
        this.customizer = customizer;
    }

    /**
     * Constructs a CustomizerResult with the specified DesignProperty and Customizer2
     */
    public CustomizerResult(final DesignProperty customizeProperty, final Customizer customizer) {
        this(customizeProperty);
        this.customizer = customizer;
    }

    /**
     * Sets the 'customizeBean' property
     *
     * @param customizeBean DesignBean the desired DesignBean to be customized
     */
    public void setCustomizeBean(final DesignBean customizeBean) {
        this.customizeBean = customizeBean;
    }

    /**
     * Retrieves the 'customizeBean' property
     *
     * @return the current value of the 'customizeBean' property
     */
    public DesignBean getCustomizeBean() {
        return customizeBean;
    }

    /**
     * Sets the 'customizeProperty' property
     *
     * @param customizeProperty the desired DesignProperty to be customized
     */
    public void setCustomizeProperty(final DesignProperty customizeProperty) {
        this.customizeProperty = customizeProperty;
    }

    /**
     * Retrieves the 'customizeProperty' property
     *
     * @return the current value of the 'customizeProperty' property
     */
    public DesignProperty getCustomizeProperty() {
        return customizeProperty;
    }

    /**
     * Sets the 'customizer' property
     *
     * @param customizer the desired Customizer2 to use on this DesignBean or DesignProperty
     */
    public void setCustomizer(final Customizer customizer) {
        this.customizer = customizer;
    }

    /**
     * Retrieves the 'customizer' property
     *
     * @return the current value of the 'customizer' property
     */
    public Customizer getCustomizer() {
        return customizer;
    }
}
