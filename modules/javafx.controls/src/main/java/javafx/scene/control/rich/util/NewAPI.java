/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor
package javafx.scene.control.rich.util;

import java.io.IOException;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import com.sun.javafx.util.Utils;

/**
 * We may consider making these APIs public.
 */
public class NewAPI {
    /**
     * may or may not be a good idea: what if TextFlow contains non-text Nodes?
     * this logic belongs to the model.
     */
    public static String getText(TextFlow f) {
        StringBuilder sb = new StringBuilder();
        for (Node n : f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                sb.append(t.getText());
            }
        }
        return sb.toString();
    }

    /** TextFlow.getTextLength() */
    public static int getTextLength(TextFlow f) {
        int len = 0;
        for (Node n : f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                len += t.getText().length();
            } else {
                // treat non-Text nodes as having 1 character
                len++;
            }
        }
        return len;
    }

    public static boolean isTouchSupported() {
        return Platform.isSupported(ConditionalFeature.INPUT_TOUCH);
    }

    // com.sun.javafx.util.Utils:769
    public static Screen getScreenForPoint(final double x, final double y) {
        ObservableList<Screen> screens = Screen.getScreens();

        // first check whether the point is inside some screen
        for (Screen screen: screens) {
            // can't use screen.bounds.contains, because it returns true for
            // the min + width point
            Rectangle2D r = screen.getBounds();
            if (
                (x >= r.getMinX()) && 
                (x < r.getMaxX()) && 
                (y >= r.getMinY()) && 
                (y < r.getMaxY())
            ) {
                return screen;
            }
        }

        // the point is not inside any screen, find the closest screen now
        Screen selectedScreen = Screen.getPrimary();
        double minDistance = Double.MAX_VALUE;
        for (Screen screen: screens) {
            Rectangle2D r = screen.getBounds();
            double dx = getOuterDistance(r.getMinX(), r.getMaxX(), x);
            double dy = getOuterDistance(r.getMinY(), r.getMaxY(), y);
            double distance = dx * dx + dy * dy;
            if (minDistance >= distance) {
                minDistance = distance;
                selectedScreen = screen;
            }
        }

        return selectedScreen;
    }

    // com.sun.javafx.util.Utils:839
    private static double getOuterDistance(double v0, double v1, double v) {
        if (v <= v0) {
            return v0 - v;
        }
        if (v >= v1) {
            return v - v1;
        }
        return 0;
    }

    /**
     * Invoked when the user attempts an invalid operation,
     * such as pasting into an uneditable <code>TextInputControl</code>
     * that has focus. The default implementation beeps.
     *
     * @param originator the <code>Node</code> the error occurred in, may be <code>null</code>
     *                   indicating the error condition is not directly associated with a <code>Node</code>
     */
    // TODO this probably should be in Platform
    public static void provideErrorFeedback(Node originator) {
        beep();
    }

    public static void beep() {
        // TODO not supported in FX
    }
    
    public static byte[] writePNG(Image im) throws IOException {
        return Utils.writePNG(im);
    }
}
