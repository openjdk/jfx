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

package test.javafx.scene.control;

import javafx.scene.control.Tooltip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Assumptions;

/**
 *
 */
@Disabled
public class ControlTooltipTest {
    private ControlStub c;
    private SkinStub<ControlStub> s;
    private Tooltip t;

    @BeforeEach
    public void setUp() {
        c = new ControlStub();
        s = new SkinStub<>(c);
        c.setSkin(s);
        t = new Tooltip();
    }

    @Test public void controlsWithNoTooltipHaveNoTooltipAsAChild() {
        // only the skin's node should be a child
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(0));
    }

    @Test public void settingTooltipOnControlResultsInTooltipBeingFirstChild() {
        c.setTooltip(t);
        assertEquals(2, c.getChildrenUnmodifiable().size());
        assertSame(t, c.getChildrenUnmodifiable().get(0));
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(1));
    }

    @Test public void settingTooltipToNullRemovesTheTooltipFromChildren() {
        c.setTooltip(t);
        c.setTooltip(null);
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(0));
    }

    @Test public void settingTooltipTwiceIgnoresTheSecondAdd() {
        c.setTooltip(t);
        c.setTooltip(t);
        assertEquals(2, c.getChildrenUnmodifiable().size());
        assertSame(t, c.getChildrenUnmodifiable().get(0));
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(1));
    }

    @Test public void swappingTheTooltipForAnotherResultsInTheNewTooltipBeingAChildAndTheOldOneRemoved() {
        c.setTooltip(t);
        Tooltip t2 = new Tooltip();
//        t2.setSkin(new SkinStub<Tooltip>(t2));
        c.setTooltip(t2);
        assertEquals(2, c.getChildrenUnmodifiable().size());
        assertSame(t2, c.getChildrenUnmodifiable().get(0));
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(1));
    }
}
