/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.scene.control;

import com.sun.javafx.scene.control.WeakReferenceWrapper;
import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;
import test.util.memory.JMemoryBuddy;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test WeakReferenceWrapper utility.
 */
public class WeakReferenceWrapperTest {

    // POJO with identity
    static class POJO {
        final int i;

        @Override
        public int hashCode() {
            return i;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof POJO otherPOJO) {
                return i == otherPOJO.i;
            } else {
                return false;
            }
        }

        POJO(int i) {
            this.i = i;
        }
    }

    // Test WeakReferenceWrapper with null, Integer (could be a value object),
    // String (always an identity object), and a POJO (identity object).
    @Test
    public void testWeakReferenceWrapper() {
        var nullRef = new WeakReferenceWrapper<Object>(null);
        assertNull(nullRef.get());

        Integer i = 123;
        var intRef = new WeakReferenceWrapper<Integer>(i);
        assertEquals(i, intRef.get());

        String str = "abc";
        var strRef = new WeakReferenceWrapper<String>(str);
        assertEquals(str, strRef.get());

        POJO pojo = new POJO(456);
        var pojoRef = new WeakReferenceWrapper<POJO>(pojo);
        assertEquals(pojo, pojoRef.get());
    }

    // Test that a WeakReferenceWrapper of an identity object holds the object
    // weakly and does not prevent the object from being collected.
    @Test
    public void testWeakReferenceToIdentityObjIsCollectable() {
        var pojo = new POJO(789);
        var pojoWeakRef = new WeakReference<POJO>(pojo);
        var pojoRef = new WeakReferenceWrapper<POJO>(pojo);

        JMemoryBuddy.assertNotCollectable(pojoWeakRef);
        assertNotNull(pojoRef.get());
        assertSame(pojo, pojoRef.get());

        pojo = null;
        JMemoryBuddy.assertCollectable(pojoWeakRef);
        assertNull(pojoRef.get());
    }
}
