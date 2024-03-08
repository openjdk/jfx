/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.Set;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.javafx.scene.control.input.EventHandlerPriority;
import com.sun.javafx.scene.control.input.PHList;

/**
 * Tests PHList as its implementation is not trivial due to optimization for space.
 * Not explicitly testing forEach() since it's used in most of the test cases.
 */
public class TestPHList {
    @Test
    public void testAdd() {
        Handler h1 = new Handler("u.high");
        Handler h2 = new Handler("s.high");
        Handler h3 = new Handler("u.low");
        PHList hs = new PHList();
        hs.add(EventHandlerPriority.USER_HIGH, h1);
        hs.add(EventHandlerPriority.SKIN_KB, null);
        hs.add(EventHandlerPriority.SKIN_HIGH, h2);
        hs.add(EventHandlerPriority.USER_LOW, h3);
        //System.out.println(hs);
        checkForEach(
            hs,
            EventHandlerPriority.USER_HIGH, h1,
            EventHandlerPriority.SKIN_KB, null,
            EventHandlerPriority.SKIN_HIGH, h2,
            EventHandlerPriority.USER_LOW, h3
        );
    }

    @Test
    public void testAddSame() {
        Handler h1 = new Handler("u.high1");
        Handler h2 = new Handler("u.high2");
        PHList hs = new PHList();
        hs.add(EventHandlerPriority.USER_HIGH, h1);
        hs.add(EventHandlerPriority.USER_HIGH, h2);
        checkForEach(
            hs,
            EventHandlerPriority.USER_HIGH, h1,
            EventHandlerPriority.USER_HIGH, h2
        );
        hs.validateInternalState(
            EventHandlerPriority.USER_HIGH,
            h1,
            h2
        );
    }

    @Test
    public void testRemove() {
        Handler h1 = new Handler("u.high");
        Handler h2 = new Handler("s.high");
        Handler h3 = new Handler("u.low");
        PHList hs = new PHList();
        hs.add(EventHandlerPriority.USER_HIGH, h1);
        hs.add(EventHandlerPriority.SKIN_KB, null);
        hs.add(EventHandlerPriority.SKIN_HIGH, h2);
        hs.add(EventHandlerPriority.USER_LOW, h3);

        hs.remove(h2);
        checkForEach(
            hs,
            EventHandlerPriority.USER_HIGH, h1,
            EventHandlerPriority.SKIN_KB, null,
            EventHandlerPriority.USER_LOW, h3
        );
        
        hs.remove(h3);
        checkForEach(
            hs,
            EventHandlerPriority.USER_HIGH, h1,
            EventHandlerPriority.SKIN_KB, null
        );
    }

    @Test
    public void testRemoveHandlers() {
        Handler h1 = new Handler("u.high");
        Handler h2 = new Handler("s.high");
        Handler h3 = new Handler("u.low");
        PHList hs = new PHList();
        hs.add(EventHandlerPriority.USER_HIGH, h1);
        hs.add(EventHandlerPriority.SKIN_KB, null);
        hs.add(EventHandlerPriority.SKIN_HIGH, h2);
        hs.add(EventHandlerPriority.USER_LOW, h3);

        hs.removeHandlers(Set.of(EventHandlerPriority.SKIN_KB));
        checkForEach(
            hs,
            EventHandlerPriority.USER_HIGH, h1,
            EventHandlerPriority.SKIN_HIGH, h2,
            EventHandlerPriority.USER_LOW, h3
        );
        
        boolean empty = hs.removeHandlers(Set.of(
            EventHandlerPriority.USER_HIGH,
            EventHandlerPriority.USER_LOW,
            EventHandlerPriority.SKIN_HIGH
            ));
        checkForEach(hs);
        Assertions.assertTrue(empty);
    }
    
    private void checkForEach(PHList hs, Object ... expected) {
        ArrayList<Object> items = new ArrayList<>();
        hs.forEach((p, h) -> {
            items.add(p);
            items.add(h);
            return true;
        });
        Assertions.assertArrayEquals(expected, items.toArray());
    }

    // test handler
    static class Handler implements EventHandler<Event> {
        private final String name;

        public Handler(String name) {
            this.name = name;
        }

        @Override
        public void handle(Event ev) {
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
