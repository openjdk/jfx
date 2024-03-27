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

package test.com.oracle.demo.rich.codearea;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.oracle.demo.rich.codearea.JavaSyntaxAnalyzer;

/**
 * Tests JavaSyntaxDecorator.
 */
public class TestJavaSyntaxDecorator {
    private static final JavaSyntaxAnalyzer.Type H = JavaSyntaxAnalyzer.Type.CHARACTER;
    private static final JavaSyntaxAnalyzer.Type C = JavaSyntaxAnalyzer.Type.COMMENT;
    private static final JavaSyntaxAnalyzer.Type K = JavaSyntaxAnalyzer.Type.KEYWORD;
    private static final JavaSyntaxAnalyzer.Type N = JavaSyntaxAnalyzer.Type.NUMBER;
    private static final JavaSyntaxAnalyzer.Type O = JavaSyntaxAnalyzer.Type.OTHER;
    private static final JavaSyntaxAnalyzer.Type S = JavaSyntaxAnalyzer.Type.STRING;
    private static final Object NL = new Object();

    private void someExamplesOfValidCode() {
        // text block
        var s = """
        ---/** */ -- // return ;
        """  ;

        // numbers
        double x = .1e15;
        x = 1.5e2;
        x = 1e3f;
        x = 1.1f;
        x = 5_0.1e2_3;
        x = 1_000_000;
        x = -1_000e-1;
        x = +1_000e+1;
        x = 1__1e-1_______________________________1;
        x = 0b10100001010001011010000101000101;
        x = 0b1010_0001_0100_0_1011_________01000010100010___1;
        x = 0x0_000__00;
    }

    @Test
    public void tests() {
        // FIX these fail
        // "FX.checkItem(m, "1", new Insets(1).equals(t.getContentPadding()), (on) -> {"
        //t(O, "tempState.point.y = ");
        //t(O, "import javafx.geometry.BoundingBox;");
        //t(O, "new StringPropertyBase(", S, "\"\"", O, ") {");

        // hex
        t(N, "0x0123456789abcdefL");
        t(N, "0x00", NL, N, "0x0123456789abcdefL");
        t(N, "0xeFeF", NL, N, "0x0123__4567__89ab_cdefL");

        // binary
        t(N, "0b00000");
        t(N, "0b1010101010L");

        // doubles
        t(N, "1___2e-3___6");
        t(N, ".15e2");
        t(N, "3.141592");
        t(N, ".12345");
        t(N, "1.5e2");
        t(N, "1.5e2_2");
        t(N, "1.5E-2");
        t(N, "1_2.5E-2");
        t(N, ".57E22");
        t(N, ".75E-5");
        t(N, "1D");
        t(N, "1___2e-3___6d");
        t(N, ".15e2d");
        t(N, "3.141592d");
        t(N, ".12345d");
        t(N, "1.5e2d");
        t(N, "1.5e2_2d");
        t(N, "1.5E-2d");
        t(N, "1_2.5E-2d");
        t(N, ".57E22d");
        t(N, ".75E-5d");
        t(N, "1D", NL, N, "1d", NL, N, "1.1D", NL, N, "1.1d", NL, N, "1.2e-3d", NL, N, "1.2e-3D", NL, N, "1.2E+3d");

        // floats
        t(N, "1f");
        t(N, "1___2e-3___6f");
        t(N, ".15e2f");
        t(N, "3.141592f");
        t(N, ".12345f");
        t(N, "1.5e2f");
        t(N, "1.5e2_2f");
        t(N, "1.5E-2f");
        t(N, "1_2.5E-2f");
        t(N, ".57E22f");
        t(N, ".75E-5f");
        t(N, "1F", NL, N, "1f", NL, N, "1.1F", NL, N, "1.1f", NL, N, "1.2e-3f", NL, N, "1.2e-3F", NL, N, "1.2E+3f");

        // longs
        t(N, "1L", NL, N, "1l", NL);
        t(N, "2_2L", NL, N, "2_2l", NL);
        t(N, "2____2L", NL, N, "2___2l", NL);
        t(O, "-", N, "99999L", NL);
        t(O, "5.L");

        // integers
        t(N, "1");
        t(N, "1_0");
        t(N, "1_000_000_000");
        t(N, "1______000___000_____000");
        // negative scenarios with integers
        t(O, "_1");
        t(O, "1_");
        t(O, "-", N, "9999");

        // text blocks
        t(O, "String s =", S, "\"\"\"   ", NL, S, " yo /* // */ */ \"\" \"  ", NL, S, "a  \"\"\"   ", O, ";");

        // strings
        t(O, " ", S, "\"\\\"/*\\\"\"", NL);
        t(S, "\"\\\"\\\"\\\"\"", O, " {", NL);
        t(S, "\"abc\"", NL, O, "s = ", S, "\"\"");

        // comments
        t(O, " ", C, "/* yo", NL, C, "yo yo", NL, C, " */", O, " ");
        t(O, " ", C, "// yo yo", NL, K, "int", O, " c;");
        t(C, "/* // yo", NL, C, "// */", O, " ");

        // chars
        t(H, "'\\b'");
        t(H, "'\\b'", NL);
        t(H, "'\\u0000'", NL, H, "'\\uFf9a'", NL);
        t(H, "'a'", NL, H, "'\\b'", NL, H, "'\\f'", NL, H, "'\\n'", NL, H, "'\\r'", NL);
        t(H, "'\\''", NL, H, "'\\\"'", NL, H, "'\\\\'", NL);

        // keywords
        t(K, "package", O, " java.com;", NL);
        t(K, "import", O, " java.util.ArrayList;", NL);
        t(K, "import", O, " java.util.ArrayList;", NL, K, "import", O, " java.util.ArrayList;", NL);
        t(K, "import", O, " com.oracle.demo");

        // misc
        t(K, "if", O, "(", S, "\"/*\"", O, " == null) {", NL);
        t(C, "// test", NL, O, "--", NL);
        t(O, "S_0,");
    }

    private void t(Object... items) {
        StringBuilder sb = new StringBuilder();
        ArrayList<JavaSyntaxAnalyzer.Line> expected = new ArrayList<>();
        JavaSyntaxAnalyzer.Line line = null;

        // builds the input string and the expected result array
        for (int i = 0; i < items.length; ) {
            Object x = items[i++];
            if (x == NL) {
                sb.append("\n");
                if (line == null) {
                    line = new JavaSyntaxAnalyzer.Line();
                }
                expected.add(line);
                line = null;
            } else {
                JavaSyntaxAnalyzer.Type t = (JavaSyntaxAnalyzer.Type)x;
                String text = (String)items[i++];
                if (line == null) {
                    line = new JavaSyntaxAnalyzer.Line();
                }
                line.addSegment(t, text);
                sb.append(text);
            }
        }

        if (line != null) {
            expected.add(line);
        }

        String input = sb.toString();
        List<JavaSyntaxAnalyzer.Line> res = new JavaSyntaxAnalyzer(input).analyze();
        Assertions.assertArrayEquals(expected.toArray(), res.toArray());
    }
}
