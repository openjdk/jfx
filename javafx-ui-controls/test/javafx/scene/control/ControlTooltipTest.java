/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
@Ignore
public class ControlTooltipTest {
    private ControlStub c;
    private SkinStub<ControlStub> s;
    private Tooltip t;

    @Before public void setUp() {
        c = new ControlStub();
        s = new SkinStub<ControlStub>(c);
        c.setSkin(s);
        t = new Tooltip();
//        t.setSkin(new SkinStub<Tooltip>(t));
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
