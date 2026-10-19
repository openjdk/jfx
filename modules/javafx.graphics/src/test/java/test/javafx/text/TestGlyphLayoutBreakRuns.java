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
package test.javafx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import javafx.scene.text.Font;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.tk.Toolkit;

/**
 *
 */
public class TestGlyphLayoutBreakRuns {

    record TC(String text, int ... runs) { }

    private static List<TC> breakRunsSource() {
        return List.of(
            new TC("good", 4),
            new TC("Adlam Pular: 𞤀𞤣𞤤𞤢𞤥 𞤆𞤵𞤤𞤢𞤪", 13, 21),
            new TC("Ancient South Arabian: 𐩣𐩯𐩬𐩵", 23, 8),
            new TC("Old Aramaic: 𐤀𐤓𐤌𐤉𐤀;", 13, 10, 1),
            new TC("Imperial Aramaic: 𐡀𐡓𐡌𐡉𐡀", 18, 10)
        );
    }

    @ParameterizedTest
    @MethodSource("breakRunsSource")
    public void testBreakRunsSurrogatePairsAdlam(TC tc) {
        // JDK-8394318
        String text = tc.text();
        TextLayout layout = Toolkit.getToolkit().getTextLayoutFactory().createLayout();
        Font font = Font.getDefault();
        layout.setContent(text, FontHelper.getNativeFont(font));
        layout.setWrapWidth(0);
        layout.setLineSpacing(0);
        GlyphList[] runs = layout.getRuns();
        assertEquals(tc.runs().length, runs.length);
        for (int i = 0; i < tc.runs().length; i++) {
            int ix = tc.runs()[i];
            assertEquals(ix, runs[i].getGlyphCount(), "index=" + i + " text=" + text);
        }
    }
}
