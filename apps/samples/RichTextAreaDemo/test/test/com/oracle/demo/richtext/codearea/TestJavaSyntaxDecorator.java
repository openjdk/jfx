/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package test.com.oracle.demo.richtext.codearea;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.oracle.demo.richtext.codearea.JavaSyntaxAnalyzer;

/**
 * Tests JavaSyntaxDecorator.
 *
 * @author Andy Goryachev
 */
public class TestJavaSyntaxDecorator {
    private static final JavaSyntaxAnalyzer.Type H = JavaSyntaxAnalyzer.Type.CHARACTER;
    private static final JavaSyntaxAnalyzer.Type C = JavaSyntaxAnalyzer.Type.COMMENT;
    private static final JavaSyntaxAnalyzer.Type K = JavaSyntaxAnalyzer.Type.KEYWORD;
    private static final JavaSyntaxAnalyzer.Type N = JavaSyntaxAnalyzer.Type.NUMBER;
    private static final JavaSyntaxAnalyzer.Type T = JavaSyntaxAnalyzer.Type.OTHER;
    private static final JavaSyntaxAnalyzer.Type S = JavaSyntaxAnalyzer.Type.STRING;
    private static final Object NL = new Object();

    @Test
    public void specialCases() {
        t(T, "print(x);");
        t(K, "new", T, " StringPropertyBase(", S, "\"\"", T, ") {");
        t(K, "import", T, " javafx.geometry.BoundingBox;");
        t(T, "tempState.point.y = ");
        t(T, "FX.checkItem(m, ", S, "\"1\"", T, " ", K, "new", T, " Insets(", N, "1", T, ").equals(t.getContentPadding()), (on) -> {", NL);
        t(K, "import", T, " atry.a;");
    }

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
        t(T, "-", N, "99999L", NL);
        t(T, "5.L");

        // integers
        t(N, "1");
        t(N, "1_0");
        t(N, "1_000_000_000");
        t(N, "1______000___000_____000");
        // negative scenarios with integers
        t(T, "_1");
        t(T, "1_");
        t(T, "-", N, "9999");

        // text blocks
        t(T, "String s =", S, "\"\"\"   ", NL, S, " yo /* // */ */ \"\" \"  ", NL, S, "a  \"\"\"   ", T, ";");

        // strings
        t(T, " ", S, "\"\\\"/*\\\"\"", NL);
        t(S, "\"\\\"\\\"\\\"\"", T, " {", NL);
        t(S, "\"abc\"", NL, T, "s = ", S, "\"\"");

        // comments
        t(T, " ", C, "/* yo", NL, C, "yo yo", NL, C, " */", T, " ");
        t(T, " ", C, "// yo yo", NL, K, "int", T, " c;");
        t(C, "/* // yo", NL, C, "// */", T, " ");

        // chars
        t(H, "'\\b'");
        t(H, "'\\b'", NL);
        t(H, "'\\u0000'", NL, H, "'\\uFf9a'", NL);
        t(H, "'a'", NL, H, "'\\b'", NL, H, "'\\f'", NL, H, "'\\n'", NL, H, "'\\r'", NL);
        t(H, "'\\''", NL, H, "'\\\"'", NL, H, "'\\\\'", NL);

        // keywords
        t(K, "package", T, " java.com;", NL);
        t(K, "import", T, " java.util.ArrayList;", NL);
        t(K, "import", T, " java.util.ArrayList;", NL, K, "import", T, " java.util.ArrayList;", NL);
        t(K, "import", T, " com.oracle.demo");

        // misc
        t(K, "if", T, "(", S, "\"/*\"", T, " == null) {", NL);
        t(C, "// test", NL, T, "--", NL);
        t(T, "S_0,");
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
