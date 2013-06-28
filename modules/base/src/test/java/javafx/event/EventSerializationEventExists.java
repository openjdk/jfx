/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventSerializationEventExists {

    private ByteArrayOutputStream byteArrayOutputStream;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    @Before
    public void setUp() throws IOException {
        byteArrayOutputStream = new ByteArrayOutputStream();
        objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    }

    public void turnToInput() throws IOException {
        objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }

    @Test
    @Ignore
    public void testSerializationEventExists() throws IOException, ClassNotFoundException {
        EventType ev = null;
        try {
            ev = new EventType(Event.ANY, "ACTION");
        } catch (IllegalArgumentException e) {
            fail("Test error: Event type already registered before the test");
        }

        List<String> l = new ArrayList<String>();
        l.add("ACTION");
        EventType.EventTypeSerialization e = new EventType.EventTypeSerialization(l);

        objectOutputStream.writeObject(e);
        turnToInput();

        //Normally, the DummyEvent class would be loaded during the event deserialization.
        Class.forName("javafx.event.ActionEvent");

        EventType eType = (EventType) objectInputStream.readObject();
        assertNotSame(ev, eType);

    }
}
