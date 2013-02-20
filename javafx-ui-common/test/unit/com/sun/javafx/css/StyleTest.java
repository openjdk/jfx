/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.css.parser.CSSParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StyleTest {
    
    private static class Data {
        private final String s1, s2;
        private final boolean expected;
        Data(String s1, String s2, boolean expected){
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        
        @Override public String toString() {
            return "\"" + s1 + "\" " + (expected ? "==" : "!=") + " \"" + s2 + "\"";
        }
    }
    
    public StyleTest(Data data) {
        this.data = data;
    }
    private final Data data;
    
    private static Style createStyle(String stylesheetText) {
        
        Stylesheet stylesheet = CSSParser.getInstance().parse(stylesheetText);
        Rule rule = stylesheet.getRules().get(0);
        Selector sel = rule.getSelectors().get(0);
        Declaration decl = rule.getDeclarations().get(0);
        return new Style(sel, decl);
    }

    @Parameters
    public static Collection data() {
        
        return Arrays.asList(new Object[] {
            new Object[] { new Data("*.style { -fx-fill: red; }", 
                                    "*.style { -fx-fill: red; }", true) },
            new Object[] { new Data("*.style { -fx-fill: red; }", 
                                    "*.bad   { -fx-fill: red; }", false) },
            new Object[] { new Data("*.style:p { -fx-fill: red; }", 
                                    "*.style:p { -fx-fill: red; }", true) },
            new Object[] { new Data("*.style:p { -fx-fill: red; }", 
                                    "*.style:q { -fx-fill: red; }", false) },
            new Object[] { new Data("*.style:p { -fx-fill: red; }", 
                                    "*.bad:p   { -fx-fill: red; }", false) },
            new Object[] { new Data("*.style#c { -fx-fill: red; }", 
                                    "*.style#c { -fx-fill: red; }", true) },
            new Object[] { new Data("*.style#c { -fx-fill: red; }", 
                                    "*.style#d { -fx-fill: red; }", false) },
            new Object[] { new Data("*.style#c:p { -fx-fill: red; }", 
                                    "*.style#c:p { -fx-fill: red; }", true) },
            new Object[] { new Data("*.style#c:p { -fx-fill: red; }", 
                                    "*.style#c:q { -fx-fill: red; }", false) },
            new Object[] { new Data("*.style { -fx-fill: red; }", 
                                    "*.style { -fx-fill: green; }", false) },
            new Object[] { new Data("*.style { -fx-border-color: red; }", 
                                    "*.style { -fx-fill: red; }", false) },
        });
    }
    
    @Test
    public void testEquals() {

        Style instance = createStyle(data.s1);
        Style obj = createStyle(data.s2);
        boolean expected = data.expected;
        boolean actual = instance.equals(obj);
        assertTrue(data.toString(), expected == actual);
        
    }

}
