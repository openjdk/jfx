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
import javafx.collections.ListChangeListener;
import javafx.scene.Node;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
@Ignore
public class ControlChildrenTest {
    private ControlStub c;
    private SkinStub<ControlStub> s;
    private Tooltip t;
    private int changeNotificationCount;
    private ListChangeListener<Node> changeNotifier;

    @Before public void setUp() {
        c = new ControlStub();
        s = new SkinStub<ControlStub>(c);
        t = new Tooltip();
//        t.setSkin(new SkinStub<Tooltip>(t));
        changeNotificationCount = 0;
        changeNotifier = c1 -> {
            changeNotificationCount++;
        };
    }

    @Test public void controlWithNoSkinAndNoTooltipHasNoChildren() {
        assertEquals(0, c.getChildrenUnmodifiable().size());
    }

    @Test public void controlWithNoTooltipAndASkinHasOnlyTheSkinNodeAsChild() {
        c.setSkin(s);
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(0));
    }

    @Test public void controlWithNoSkinAndATooltipHasOnlyTheTooltipAsChild() {
        c.setTooltip(t);
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(t, c.getChildrenUnmodifiable().get(0));
    }
    
    @Test public void changingTheSkinResultsInBothNewSkinNodeAndTooltipAsChildren() {
        c.setSkin(s);
        c.setTooltip(t);
        SkinStub<ControlStub> s2 = new SkinStub<ControlStub>(c);
        c.setSkin(s2);
        assertEquals(2, c.getChildrenUnmodifiable().size());
        assertSame(t, c.getChildrenUnmodifiable().get(0));
        assertSame(s2.getNode(), c.getChildrenUnmodifiable().get(1));
    }

    @Test public void changingTheTooltipResultsInBothSkinNodeAndNewTooltipAsChildren() {
        c.setSkin(s);
        c.setTooltip(t);
        Tooltip t2 = new Tooltip();
//        t2.setSkin(new SkinStub<Tooltip>(t));
        c.setTooltip(t2);
        assertEquals(2, c.getChildrenUnmodifiable().size());
        assertSame(t2, c.getChildrenUnmodifiable().get(0));
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(1));
    }

    @Test public void clearingTheSkinShouldLeaveJustTheTooltip() {
        c.setSkin(s);
        c.setTooltip(t);
        c.setSkin(null);
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(t, c.getChildrenUnmodifiable().get(0));
    }

    @Test public void clearingTheTooltipShouldLeaveJustTheSkinNode() {
        c.setSkin(s);
        c.setTooltip(t);
        c.setTooltip(null);
        assertEquals(1, c.getChildrenUnmodifiable().size());
        assertSame(s.getNode(), c.getChildrenUnmodifiable().get(0));
    }

    @Test public void clearingTheTooltipAndSkinShouldLeaveNoChildren() {
        c.setSkin(s);
        c.setTooltip(t);
        c.setTooltip(null);
        c.setSkin(null);
        assertEquals(0, c.getChildrenUnmodifiable().size());
    }

    @Test public void changingTheSkinResultsInASingleChangeNotification() {
        c.setSkin(s);
        c.setTooltip(t);
        c.getChildrenUnmodifiable().addListener(changeNotifier);
        SkinStub<ControlStub> s2 = new SkinStub<ControlStub>(c);
        c.setSkin(s2);
        assertEquals(1, changeNotificationCount);
    }

    @Test public void changingTheTooltipResultsInASingleChangeNotification() {
        c.setSkin(s);
        c.setTooltip(t);
        c.getChildrenUnmodifiable().addListener(changeNotifier);
        Tooltip t2 = new Tooltip();
//        t2.setSkin(new SkinStub<Tooltip>(t));
        c.setTooltip(t2);
        assertEquals(1, changeNotificationCount);
    }

    @Test public void clearingTheSkinShouldFireASingleChangeNotification() {
        c.setSkin(s);
        c.setTooltip(t);
        c.getChildrenUnmodifiable().addListener(changeNotifier);
        c.setSkin(null);
        assertEquals(1, changeNotificationCount);
    }

    @Test public void clearingTheTooltipShouldFireASingleChangeNotification() {
        c.setSkin(s);
        c.setTooltip(t);
        c.getChildrenUnmodifiable().addListener(changeNotifier);
        c.setTooltip(null);
        assertEquals(1, changeNotificationCount);
    }

    @Test public void clearingTheTooltipAndSkinShouldFireTwoChangeNotifications() {
        c.setSkin(s);
        c.setTooltip(t);
        c.getChildrenUnmodifiable().addListener(changeNotifier);
        c.setTooltip(null);
        c.setSkin(null);
        assertEquals(2, changeNotificationCount);
    }
}
