/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.Node;

public interface Page {
    public void setPageContainer(PageContainer container);
    public PageContainer getPageContainer();

    /**
     * Get the node that represents the page.
     *
     * @return the page node.
     */
    public Node getUI();

    /**
     * Get the title to display for this page.
     *
     * @return The page title
     */
    public String getTitle();
    public ReadOnlyStringProperty titleProperty();

    /**
     * The text for left button, if null then button will be hidden.
     */
    public String getLeftButtonText();
    public ReadOnlyStringProperty leftButtonTextProperty();

    /**
     * Called on a click of the left button of the popover.
     */
    public void handleLeftButton();

    /**
     * The text for right button, if null then button will be hidden.
     */
    public String getRightButtonText();
    public ReadOnlyStringProperty rightButtonTextProperty();

    /**
     * Called on a click of the right button of the popover.
     */
    public void handleRightButton();

    public void handleShown();
    public void handleHidden();
}
