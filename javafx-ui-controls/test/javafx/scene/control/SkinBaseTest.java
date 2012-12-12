/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

public class SkinBaseTest {
    private ControlStub c;
    private SkinBaseStub<ControlStub> s;

    @Before public void setup() {
        c = new ControlStub();
        s = new SkinBaseStub<ControlStub>(c);
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

    public static final class SkinBaseStub<C extends Control> extends SkinBase<C> {
        public SkinBaseStub(C control) {
            super(control);
        }
    }
}
