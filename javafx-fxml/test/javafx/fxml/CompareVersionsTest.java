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

package javafx.fxml;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompareVersionsTest {
    @Test
    public void testCompareJFXVersions() throws IOException {
        assertTrue(FXMLLoader.compareJFXVersions("1.1", "1.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.1", "0.9.9") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2", "1.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2", "1.1.1") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2", "1.2.3") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2.1", "2.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2.1.1.1", "2.1.1.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.5.2", "1.3.5") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0-ea", "2") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0_fcs", "2.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0", "2.0.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0.1", "3.0.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.2.1", "3.2.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.2.1-ea", "1.2.3") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("2", "1") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("5", "3") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("6", "5") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0.1", "3.0.0.0") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0.1", "3.0.0.0.0.1") > 0);
        assertTrue(FXMLLoader.compareJFXVersions("8.0.0-ea", "2.2.5") > 0);

        assertTrue(FXMLLoader.compareJFXVersions("1", "1.0") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.0", "1") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3", "1.2.3") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1", "1") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("5", "5") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3.0-fcs", "1.2.3") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3_ea", "1.2.3.0.0.0") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3.0.0.0.0", "1.2.3") == 0);

        assertTrue(FXMLLoader.compareJFXVersions("ABC", "1.2.3") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("a.b.c", "1.2.3") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3", "abc") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("abc", "abc") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3.a.b", "1.2.3") == 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.3", "1.2.3.*") == 0);

        assertTrue(FXMLLoader.compareJFXVersions("0.9", "1.0") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1", "2") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1", "3") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1", "12") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("3", "3.0.0.1.2") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1", "1.2") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2", "1.2.3") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.0", "1.2.1") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("1.2.0", "1.2.0.0.0.1") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.2.1", "3.2.2") < 0);
        assertTrue(FXMLLoader.compareJFXVersions("3.0.0.1", "3.0.0.1.0.0.1") < 0);
    }
}
