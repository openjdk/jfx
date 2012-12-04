/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.control.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import javafx.scene.control.Control;
import javafx.scene.control.ControlStub;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

public class BehaviorSkinBaseTest {
    private ControlStub c;
    private BehaviorBaseStub<ControlStub> b;
    private BehaviorSkinBaseStub<ControlStub, BehaviorBaseStub<ControlStub>> s;

    @Before public void setup() {
        c = new ControlStub();
        b = new BehaviorBaseStub<ControlStub>(c);
        s = new BehaviorSkinBaseStub<ControlStub, BehaviorBaseStub<ControlStub>>(c, b);
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
            super(control);
        }        
    }
}
