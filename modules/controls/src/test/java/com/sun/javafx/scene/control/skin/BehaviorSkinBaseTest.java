/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.scene.control.Control;
import javafx.scene.control.ControlStub;
import java.util.Collections;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BehaviorSkinBaseTest {
    private ControlStub c;
    private BehaviorBaseStub<ControlStub> b;
    private BehaviorSkinBaseStub<ControlStub, BehaviorBaseStub<ControlStub>> s;

    @Before public void setup() {
        c = new ControlStub();
        b = new BehaviorBaseStub<>(c);
        s = new BehaviorSkinBaseStub<>(c, b);
    }
    
    @Test public void skinNotAssignedToControlShouldStillHaveReferenceToControl() {
        assertSame(c, s.getSkinnable());
    }

    @Test public void skinAddedToControlShouldReferToControl() {
        c.setSkin(s);
        assertSame(c, s.getSkinnable());
    }
    
    @Test public void skinRemovedFromControlShouldHaveNullReferenceToControl() {
        c.setSkin(s);
        c.setSkin(null);
        assertNull(s.getSkinnable());
    }

    @Test public void skinRemovedFromControlShouldHaveNullReferenceToBehavior() {
        c.setSkin(s);
        c.setSkin(null);
        assertNull(s.getBehavior());
    }

    public static final class BehaviorSkinBaseStub<C extends Control, B extends BehaviorBase<C>> extends BehaviorSkinBase<C,B> {
        public BehaviorSkinBaseStub(C control, B behavior) {
            super(control, behavior);
        }
    }
    
    public static final class BehaviorBaseStub<C extends Control> extends BehaviorBase<C> {
        public BehaviorBaseStub(C control) {
            super(control, Collections.EMPTY_LIST);
        }        
    }
}
