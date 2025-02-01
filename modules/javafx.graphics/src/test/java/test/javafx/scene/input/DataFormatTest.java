/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.input;

import java.util.stream.Stream;
import javafx.scene.input.DataFormat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataFormatTest {

    static DataFormat customFormat = new DataFormat("Custom1", "Custom2");
    static DataFormat uniqueFormat = new DataFormat("Unique");


    public static Stream<Arguments> getParams() {
        return Stream.of(
            Arguments.of( DataFormat.PLAIN_TEXT, "text/plain", null ),
            Arguments.of( DataFormat.HTML, "text/html", null ),
            Arguments.of( DataFormat.RTF, "text/rtf", null ),
            Arguments.of( DataFormat.URL, "text/uri-list", null ),
            Arguments.of( DataFormat.IMAGE, "application/x-java-rawimage", null ),
            Arguments.of( DataFormat.FILES, "application/x-java-file-list",  "java.file-list" ),
            Arguments.of( customFormat, "Custom1", "Custom2" )
        );
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testMimeTypes(DataFormat format, String mime1, String mime2) {
        assertEquals(mime2 != null ? 2 : 1, format.getIdentifiers().size());
        assertTrue(format.getIdentifiers().contains(mime1));
        if (mime2 != null) {
            assertTrue(format.getIdentifiers().contains(mime2));
        }
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void dataFormatsShouldBeFound(DataFormat format, String mime1, String mime2) {
        assertSame(format, DataFormat.lookupMimeType(mime1));
        if (mime2 != null) {
            assertSame(format, DataFormat.lookupMimeType(mime2));
        }
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testToString(DataFormat format, String mime1, String mime2) {
        assertNotNull(customFormat.toString());
        assertFalse("".equals(customFormat.toString()));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void shouldNotBePossibleToReuseMimeTypes(DataFormat format, String mime1, String mime2) {
        assertThrows(IllegalArgumentException.class, () -> {
            DataFormat customEqual = new DataFormat(format.getIdentifiers().toArray(
                    new String[format.getIdentifiers().size()]));
        });
    }


    @ParameterizedTest
    @MethodSource("getParams")
    public void testEqualsAndHashCode(DataFormat format, String mime1, String mime2) {
        //cannot have two different equal data formats
        assertEquals(format, format);
        assertEquals(format.hashCode(), format.hashCode());
        assertFalse(uniqueFormat.equals(format));
        assertFalse(uniqueFormat.hashCode() == format.hashCode());
    }
}
