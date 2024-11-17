/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import com.sun.javafx.util.Utils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlow;

/**
 * This custom ScrollBar is used to map the increment & decrement features
 * to pixel based scrolling rather than thumb/track based scrolling, if the
 * "virtual" attribute is true.
 */
public class VirtualScrollBar extends ScrollBar {

    /**************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    private final VirtualFlow flow;

    private boolean adjusting;



    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a new VirtualScrollBar, for use by the VirtualFlow control.
     */
    public VirtualScrollBar(final VirtualFlow flow) {
        this.flow = flow;

        super.valueProperty().addListener(valueModel -> {
            if (isVirtual()/* && oldValue != newValue*/) {
                if (adjusting) {
                    // no-op
                } else {
                    flow.setPosition(getValue());
                }
            }
        });
    }



    /**************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    // --- virtual
    private BooleanProperty virtual = new SimpleBooleanProperty(this, "virtual");
    public final void setVirtual(boolean value) {
        virtual.set(value);
    }

    public final boolean isVirtual() {
        return virtual.get();
    }

    public final BooleanProperty virtualProperty() {
        return virtual;
    }


    /**************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void decrement() {
        if (isVirtual()) {
            flow.scrollPixels(-10);
        } else {
            super.decrement();
        }
    }

    /** {@inheritDoc} */
    @Override public void increment() {
        if (isVirtual()) {
            flow.scrollPixels(10);
        } else {
            super.increment();
        }
    }

    // this method is called when the user clicks in the scrollbar track, so
    // we special-case it to allow for page-up and page-down clicking to work
    // as expected.
    /** {@inheritDoc} */
    @Override public void adjustValue(double pos) {
        if (isVirtual()) {
            adjusting = true;
            if (pos < getValue()) {
                flow.scrollPixels(-flow.getViewportLength());
            } else {
                flow.scrollPixels(flow.getViewportLength());
            }
            adjusting = false;
        } else {
            super.adjustValue(pos);
        }
    }
}
