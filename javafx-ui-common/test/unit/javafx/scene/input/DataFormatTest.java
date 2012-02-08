/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.input;

import java.util.Collection;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DataFormatTest {
    
    static DataFormat customFormat = new DataFormat("Custom1", "Custom2");
    static DataFormat uniqueFormat = new DataFormat("Unique");

    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { DataFormat.PLAIN_TEXT, "text/plain", null },
            { DataFormat.HTML, "text/html", null },
            { DataFormat.RTF, "text/rtf", null },
            { DataFormat.URL, "text/uri-list", null },
            { DataFormat.IMAGE, "application/x-java-rawimage", null },
            { DataFormat.FILES, "application/x-java-file-list",  "java.file-list" },
            { customFormat, "Custom1", "Custom2" }
        });
    }
    
    private DataFormat format;
    private String mime1;
    private String mime2;
    
    public DataFormatTest(DataFormat format, String mime1, String mime2) {
        this.format = format;
        this.mime1 = mime1;
        this.mime2 = mime2;
    }
    
    @Test
    public void testMimeTypes() {
        assertEquals(mime2 != null ? 2 : 1, format.getIdentifiers().size());
        assertTrue(format.getIdentifiers().contains(mime1));
        if (mime2 != null) {
            assertTrue(format.getIdentifiers().contains(mime2));
        }
    }

    @Test
    public void dataFormatsShouldBeFound() {
        assertSame(format, DataFormat.lookupMimeType(mime1));
        if (mime2 != null) {
            assertSame(format, DataFormat.lookupMimeType(mime2));            
        }
    }

    @Test
    public void testToString() {
        assertNotNull(customFormat.toString());
        assertFalse("".equals(customFormat.toString()));
    }    
    
    @Test
    public void testEqualsAndHashCode() {
        DataFormat customEqual = new DataFormat(format.getIdentifiers().toArray(
                new String[format.getIdentifiers().size()]));
        
        assertEquals(customEqual, format);
        assertEquals(customEqual.hashCode(), format.hashCode());
        assertFalse(uniqueFormat.equals(format));
        assertFalse(uniqueFormat.hashCode() == format.hashCode());
    }
}
