/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import com.sun.javafx.fxml.PropertyChangeEvent;
import javafx.event.EventHandler;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RT_25559Test {

    @Test
    public void testControllerFactory() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_25559.fxml"));
        final boolean[] ref = new boolean[] { false };
        fxmlLoader.getNamespace().put("manualAction", new EventHandler<PropertyChangeEvent<String>>() {
            @Override
            public void handle(PropertyChangeEvent<String> stringPropertyChangeEvent) {
                ref[0] = true;
            }
        });
        fxmlLoader.load();

        Widget w = fxmlLoader.getRoot();
        assertFalse(ref[0]);
        w.setName("abc");
        assertTrue(ref[0]);
    }

    public static void main(String[] args) throws IOException {
        new RT_25559Test().testControllerFactory();
    }

    @Test
    public void testNoValues() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_25559_err1.fxml"));
        try {
            fxmlLoader.load();
            fail("Exception should have been thrown.");
        } catch (LoadException loadException) {
            assertTrue(loadException.getMessage().contains("onNameChange='$doesNotExist'"));
        }
    }

}
