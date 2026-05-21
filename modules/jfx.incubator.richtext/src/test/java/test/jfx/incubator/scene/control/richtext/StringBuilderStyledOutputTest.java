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
package test.jfx.incubator.scene.control.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.StringBuilderStyledOutput;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

public class StringBuilderStyledOutputTest {
    @Test
    public void noLimit() throws IOException {
        String text = "01234567890123456789";
        StringBuilder sb = new StringBuilder();
        StringBuilderStyledOutput out = new StringBuilderStyledOutput(sb, LineEnding.CRLF, Integer.MAX_VALUE);
        out.consume(StyledSegment.of(text));
        assertEquals(text, sb.toString());
    }

    @Test
    public void withLimit() {
        StringBuilder sb = new StringBuilder();
        StringBuilderStyledOutput out = new StringBuilderStyledOutput(sb, LineEnding.CRLF, 5);
        assertThrows(IOException.class, () -> {
            out.consume(StyledSegment.of("01234567890123456789"));
        });
        // appends as much as possible under the limit
        assertEquals("01234", sb.toString());
    }

    @Test
    public void newlineWithLimit() {
        StringBuilder sb = new StringBuilder();
        StringBuilderStyledOutput out = new StringBuilderStyledOutput(sb, LineEnding.CRLF, 1);
        assertThrows(IOException.class, () -> {
            out.consume(StyledSegment.LINE_BREAK);
        });
        // appends nothing
        assertEquals("", sb.toString());
    }
}
