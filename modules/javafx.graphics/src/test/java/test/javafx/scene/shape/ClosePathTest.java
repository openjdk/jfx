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

package test.javafx.scene.shape;

import com.sun.javafx.sg.prism.NGPath;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.ClosePathShim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClosePathTest {

    @Test
    public void testAddTo() throws Exception {
        final StubPathImpl pgPath = new StubPathImpl();
        ClosePath closePath = new ClosePath();
        ClosePathShim.addTo(closePath, pgPath);
        assertTrue(pgPath.isClosed());

    }

    @Test
    public void toStringShouldReturnNonEmptyString() {
        String s = new ClosePath().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private class StubPathImpl extends NGPath {

        boolean closed = false;

        @Override public void addClosePath() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
