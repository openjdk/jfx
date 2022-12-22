/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.skin.VirtualFlowShim.*;

import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Region;

/**
 * Utility methods to test virtualized controls and cells contained within.
 *
 * Note: there's a certain overlap with VirtualFlowTestUtils - differences:
 * - the controls passed as parameters must already have a skin extending VirtualContainerBase
 * - uses newer api (in VirtualizedContainerBase/VirtualFlow) to access children (vs. lookup)
 * - uses alternative constructor of MouseEventFirer for exact control of mouse location
 *
 */
public class VirtualizedControlTestUtils {

    /**
     * Fires a mouse event onto the middle of the vertical scrollbar's track.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static void fireMouseOnVerticalTrack(Control control) {
        ScrollBar scrollBar = getVerticalScrollBar(control);
        Region track = (Region) scrollBar.lookup(".track");
        MouseEventFirer firer = new MouseEventFirer(track, true);
        firer.fireMousePressAndRelease();
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Fires a mouse event onto the middle of the horizontal scrollbar's track.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static void fireMouseOnHorizontalTrack(Control control) {
        ScrollBar scrollBar = getHorizontalScrollBar(control);
        Region track = (Region) scrollBar.lookup(".track");
        MouseEventFirer firer = new MouseEventFirer(track, true);
        firer.fireMousePressAndRelease();
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Returns a vertical ScrollBar of the control.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static ScrollBar getVerticalScrollBar(Control control) {
        if (control.getSkin() instanceof VirtualContainerBase) {
            VirtualFlow<?> flow = getVirtualFlow((VirtualContainerBase<?, ?>) control.getSkin());
            return getVBar(flow);
        }
        throw new IllegalStateException("control's skin must be of type VirtualContainerBase but was: " + control.getSkin());
    }

    /**
     * Returns a horizontal ScrollBar of the control.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static ScrollBar getHorizontalScrollBar(Control control) {
        if (control.getSkin() instanceof VirtualContainerBase) {
            VirtualFlow<?> flow = getVirtualFlow((VirtualContainerBase<?, ?>) control.getSkin());
            return getHBar(flow);
        }
        throw new IllegalStateException("control's skin must be of type VirtualContainerBase but was: " + control.getSkin());
    }

    private VirtualizedControlTestUtils() {}

}
