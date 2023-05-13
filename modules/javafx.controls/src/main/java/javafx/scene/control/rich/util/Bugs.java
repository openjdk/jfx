/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich.util;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Workarounds for some JFX bugs.
 */
public class Bugs {
    /**
     * https://bugs.openjdk.org/browse/JDK-8304831
     * TextFlow.hitTest() gives wrong value for emojis due to null text.
     */
    // FIX still returns an incorrect value when multiple Text instances are added to TextFlow
    public static int getInsertionIndex2(TextFlow flow, Point2D p) {
        int off = 0;
        for(Node ch: flow.getChildren()) {
            if(ch instanceof Text t) {
                Point2D p2 = t.parentToLocal(p);
                if(t.contains(p2)) {
                    HitInfo h = t.hitTest(p2);
                    try {
                        return off + h.getInsertionIndex();
                    } catch(Exception e) {
                        h = t.hitTest(p2);
                        h.getInsertionIndex();
                    }
                } else {
                    String text = t.getText();
                    off += text.length();
                }
            }
        }
        // fallback
        return flow.hitTest(p).getInsertionIndex();
    }
    
    public static int getInsertionIndex(TextFlow flow, Point2D p) {
        // FIX does not work with emojis
        // https://bugs.openjdk.org/browse/JDK-8304831
        return flow.hitTest(p).getInsertionIndex();
    }
}
