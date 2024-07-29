/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.SelectorPartitioning;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.DeclarationShim;
import javafx.css.Rule;
import javafx.css.RuleShim;
import javafx.css.Selector;
import javafx.css.SimpleSelector;
import javafx.css.SimpleSelectorShim;
import javafx.css.Stylesheet;
import javafx.scene.paint.Color;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SelectorPartitioningTest {

    public SelectorPartitioningTest(final Data data) {
        this.data = data;
        this.instance = new SelectorPartitioning();
    }
    private final Data data;
    private final SelectorPartitioning instance;

    private static class Data {
        final Color  color;
        final String stylesheetText;

        Data(Color color, String stylesheetText) {
            this.color = color;
            this.stylesheetText = stylesheetText;
        }

    }

    private static class SimpleData extends Data {

        final SimpleSelector selector;
        final boolean matches;

        SimpleData(String type, String styleClass, String id, Color color) {
            this(type, styleClass, id, color, true);
        }
        SimpleData(String type, String styleClass, String id, Color color, boolean matches) {
            super(color, (new StringBuilder()
                    .append((type != null ? type : "*"))
                    .append((styleClass != null ? ".".concat(styleClass) : ""))
                    .append((id != null ? "#".concat(id) : ""))
                    .append("{-fx-fill: rgb(")
                    .append(Double.valueOf(color.getRed()*255).intValue()).append(",")
                    .append(Double.valueOf(color.getGreen()*255).intValue()).append(",")
                    .append(Double.valueOf(color.getBlue()*255).intValue()).append(");}")
                ).toString());
            List<String> styleClasses =
                styleClass != null ? Arrays.asList(styleClass.split("\\.")) : null;
            this.selector =
                SimpleSelectorShim.getSimpleSelector(type, styleClasses, null, id);
            this.matches = matches;
        }
    }

    private static class ComplexData extends Data {

        final SimpleData[] data;
        final SimpleSelector selector;
        final int matches;

        ComplexData(SimpleSelector selector, SimpleData... data) {
            super(Color.TRANSPARENT, combineStylesheets(data));
            this.data = data;
            this.selector = selector;
            int n = 0;
            for (SimpleData datum : data) {
                if (datum.matches) n += 1;
            }
            this.matches = n;
        }

        static String combineStylesheets(Data... data) {
            StringBuilder buf = new StringBuilder();
            for (Data datum : data) {
                buf.append(datum.stylesheetText).append('\n');
            }
            return buf.toString();
        }
    }


    @Parameters
    public static Collection data() {

        int red = 0;
        int green = 0;
        int blue = 0;
        return Arrays.asList(new Object[] {
            /* selector = * */
            new Object[] {new SimpleData("*", null, null, Color.rgb(red += 10, 0, 0))},
            /* selector = A */
            new Object[] {new SimpleData("A", null, null, Color.rgb(red += 10, 0, 0))},
            /* selector = * A.b */
            new Object[] {new SimpleData("A", "b", null, Color.rgb(red += 10, 0, 0))},
            /* selector = A#c */
            new Object[] {new SimpleData("A", null, "c", Color.rgb(red += 10, 0, 0))},
            /* selector = A.b#c */
            new Object[] {new SimpleData("A", "b", "c", Color.rgb(red += 10, 0, 0))},
            /* selector = *.b */
            new Object[] {new SimpleData("*", "b", null, Color.rgb(red += 10, 0, 0))},
            /* selector = *#c */
            new Object[] {new SimpleData("*", null, "c", Color.rgb(red += 10, 0, 0))},
            /* selector = *.b#c */
            new Object[] {new SimpleData("*", "b", "c", Color.rgb(red += 10, 0, 0))},

            new Object[] {
                new ComplexData(
                    (SimpleSelector)Selector.createSelector("*.b"),
                    new SimpleData("*", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "c", null, Color.rgb(0, green += 10, 0), false),
                    new SimpleData("*", "b.c", null, Color.rgb(0, green += 10, 0), false)
                )},
            new Object[] {
                new ComplexData(
                    (SimpleSelector)Selector.createSelector("*.b.c"),
                    new SimpleData("*", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "c", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "b.c", null, Color.rgb(0, green += 10, 0), true)
                )},
            new Object[] {
                new ComplexData(
                    (SimpleSelector)Selector.createSelector("A.b"),
                    new SimpleData("*", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("A", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", null, null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "c", null, Color.rgb(0, green += 10, 0), false),
                    new SimpleData("*", "b.c", null, Color.rgb(0, green += 10, 0), false)
                )},
            new Object[] {
                new ComplexData(
                    (SimpleSelector)Selector.createSelector("A.c"),
                    new SimpleData("*", "b", null, Color.rgb(0, green += 10, 0), false),
                    new SimpleData("A", "b", null, Color.rgb(0, green += 10, 0), false),
                    new SimpleData("*", null, null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "c", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "b.c", null, Color.rgb(0, green += 10, 0), false)
                )},
            new Object[] {
                new ComplexData(
                    (SimpleSelector)Selector.createSelector("A.b.c"),
                    new SimpleData("*", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("A", "b", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", null, null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "c", null, Color.rgb(0, green += 10, 0), true),
                    new SimpleData("*", "b.c", null, Color.rgb(0, green += 10, 0), true)
                )}
        });
    }

    @Test
    public void testSelectorPartitionAndMatch() {

        Stylesheet stylesheet =
                new CssParser().parse(data.stylesheetText);

        for (Rule rule : stylesheet.getRules()) {
            for (Selector selector : RuleShim.getUnobservedSelectorList(rule)) {
                instance.partition(selector);
            }
        }

        if (data instanceof SimpleData) {
            testWithSimpleData((SimpleData)data, stylesheet);
        } else {
            testWithComplexData((ComplexData)data, stylesheet);
        }
    }

    private void testWithSimpleData(SimpleData simpleData, Stylesheet stylesheet) {

        SimpleSelector simple = simpleData.selector;

        List<Selector> matched = instance.match(simple.getId(), simple.getName(), simple.getStyleClassNames());

        assertEquals(1,matched.size());
        Selector selector = matched.get(0);

        Rule rule = selector.getRule();

        assertEquals(1,RuleShim.getUnobservedDeclarationList(rule).size());
        Declaration decl = RuleShim.getUnobservedDeclarationList(rule).get(0);

        assertEquals("-fx-fill", DeclarationShim.get_property(decl));

        Color color = (Color)DeclarationShim.get_parsedValue(decl).convert(null);

        assertEquals(simpleData.selector.toString(), data.color.getRed(), color.getRed(), 0.00001);
        assertEquals(simpleData.selector.toString(), data.color.getGreen(), color.getGreen(), 0.00001);
        assertEquals(simpleData.selector.toString(), data.color.getBlue(), color.getBlue(), 0.00001);

    }

    private void testWithComplexData(ComplexData complexData, Stylesheet stylesheet) {

        SimpleSelector simple = complexData.selector;

        List<Selector> matched = instance.match(simple.getId(), simple.getName(), simple.getStyleClassNames());
        assertEquals(complexData.matches, matched.size());

        for(Selector s1 : matched) {
            for (SimpleData datum : complexData.data) {
                Selector s2 = datum.selector;
                if (s1.equals(s2)) {
                    assertTrue(s1.toString() + " != " + s2.toString(), datum.matches);
                }
            }
        }

    }

}
