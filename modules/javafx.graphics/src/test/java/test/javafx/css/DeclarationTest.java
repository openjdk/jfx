/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css;

import javafx.scene.paint.Color;
import com.sun.javafx.css.ParsedValueImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javafx.css.Declaration;
import javafx.css.DeclarationShim;
import javafx.css.RuleShim;
import javafx.css.SelectorShim;
import javafx.css.StyleOrigin;
import javafx.css.Stylesheet;
import javafx.css.StylesheetShim;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DeclarationTest {

    private static class Data {
        private final Declaration d1, d2;
        private final boolean expected;
        Data(Declaration d1, Declaration d2, boolean expected){
            this.d1 = d1;
            this.d2 = d2;
            this.expected = expected;
        }

        @Override public String toString() {
            return "\"" + d1 + "\" " + (expected ? "==" : "!=") + " \"" + d2 + "\"";
        }
    }

    public DeclarationTest(Data data) {
        this.data = data;
    }
    private final Data data;


    @Parameters
    public static Collection data() {

        int n = 0;
        final int GI = n++; // green inline
        final int YI = n++; // yellow inline
        final int GA1 = n++; // green author 1
        final int YA1 = n++; // yellow author 1
        final int GA2 = n++; // green author 2
        final int YA2 = n++; // yellow author 2

        final Declaration[] DECLS = new Declaration[n];

        Stylesheet inlineSS = new StylesheetShim() {
            {
                setOrigin(StyleOrigin.INLINE);

                DECLS[GI] = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YI] = DeclarationShim.getDeclaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);

                Collections.addAll(getRules(),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[GI])),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[YI]))
                );
            }
        };

        Stylesheet authorSS_1 = new StylesheetShim() {
            {
                setOrigin(StyleOrigin.AUTHOR);

                DECLS[GA1] = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YA1] = DeclarationShim.getDeclaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);

                Collections.addAll(getRules(),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[GA1])),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[YA1]))
                );
            }
        };

        Stylesheet authorSS_2 = new StylesheetShim() {
            {
                setOrigin(StyleOrigin.AUTHOR);

                DECLS[GA2] = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YA2] = DeclarationShim.getDeclaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);

                Collections.addAll(getRules(),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[GA2])),
                    RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(DECLS[YA2]))
                );
            }
        };

        return Arrays.asList(new Object[] {
            new Object[] { new Data(DECLS[GA1], DECLS[GA2], true) },
            new Object[] { new Data(DECLS[GA1], DECLS[YA1], false) },
            new Object[] { new Data(DECLS[GA1], DECLS[GI],  false) }
        });
    }

    @Test
    public void testEquals() {

        Declaration instance = data.d1;
        Declaration obj = data.d2;
        boolean expected = data.expected;
        boolean actual = instance.equals(obj);
        assertTrue(data.toString(), expected == actual);

    }

}
