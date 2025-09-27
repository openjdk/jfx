/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.RuleHelper;
import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import javafx.application.ColorScheme;
import javafx.css.CssParser;
import javafx.css.Stylesheet;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CssParser_mediaQuery_Test {

    @Test
    void parseSimpleMediaQuery() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parseNegatedMediaQuery() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media not (prefers-color-scheme: light) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new NegationExpression(new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT)),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parseNestedMediaQuery() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: reduce) {
                @media (prefers-color-scheme: light) {
                    .foo { bar: baz; }
                }
            }
            """);

        var innerMediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        var outerMediaRule = innerMediaRule.getParent();
        assertEquals(1, innerMediaRule.getQueries().size());
        assertEquals(1, outerMediaRule.getQueries().size());
        assertNull(outerMediaRule.getParent());
        assertEquals(
            new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
            innerMediaRule.getQueries().getFirst());
        assertEquals(
            new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> null, true),
            outerMediaRule.getQueries().getFirst());
    }

    @Test
    void parseMediaQueryWithMultipleRules() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) {
                .foo1 { bar: baz; }
                .foo2 { bar: baz; }
            }
            """);

        assertEquals(2, stylesheet.getRules().size());
        var rule1 = stylesheet.getRules().getFirst();
        var rule2 = stylesheet.getRules().getLast();
        assertEquals(Set.of("foo1"), rule1.getSelectors().getFirst().getStyleClassNames());
        assertEquals(Set.of("foo2"), rule2.getSelectors().getFirst().getStyleClassNames());

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parseNestedMediaQueryWithMultipleRules() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: reduce) {
                .foo1 { bar: baz; }
                .foo2 { bar: baz; }
                @media (prefers-color-scheme: light) {
                    .foo3 { bar: baz; }
                    .foo4 { bar: baz; }
                }
            }
            """);

        assertEquals(4, stylesheet.getRules().size());

        var rule1 = stylesheet.getRules().get(0);
        assertEquals(Set.of("foo1"), rule1.getSelectors().getFirst().getStyleClassNames());
        assertNull(RuleHelper.getMediaRule(rule1).getParent());

        var rule2 = stylesheet.getRules().get(1);
        assertEquals(Set.of("foo2"), rule2.getSelectors().getFirst().getStyleClassNames());
        assertNull(RuleHelper.getMediaRule(rule2).getParent());

        var rule3 = stylesheet.getRules().get(2);
        assertEquals(Set.of("foo3"), rule3.getSelectors().getFirst().getStyleClassNames());
        assertNull(RuleHelper.getMediaRule(rule3).getParent().getParent());

        var rule4 = stylesheet.getRules().get(3);
        assertEquals(Set.of("foo4"), rule4.getSelectors().getFirst().getStyleClassNames());
        assertNull(RuleHelper.getMediaRule(rule4).getParent().getParent());
    }

    @Test
    void parseMediaQueryList() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: dark),
                   (prefers-reduced-motion),
                   (prefers-reduced-transparency: no-preference) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(
            List.of(
                new FunctionExpression<>("prefers-color-scheme", "dark", _ -> null, ColorScheme.DARK),
                new FunctionExpression<>("prefers-reduced-motion", null, _ -> null, true),
                new FunctionExpression<>("prefers-reduced-transparency", "no-preference", _ -> null, false)
            ),
            mediaRule.getQueries());
    }

    @Test
    void parseConjunction() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) and (prefers-reduced-motion: reduce) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            List.of(new ConjunctionExpression(
                new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> null, true)
            )),
            mediaRule.getQueries());
    }

    @Test
    void parseMultiConjunction() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) and
                   (prefers-reduced-motion: reduce) and
                   (prefers-reduced-transparency: no-preference) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new ConjunctionExpression(
                new ConjunctionExpression(
                    new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                    new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> false, true)),
                new FunctionExpression<>("prefers-reduced-transparency", "no-preference", _ -> false, false)
            ),
            mediaRule.getQueries().getFirst()
        );
    }

    @Test
    void parseDisjunction() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) or (prefers-reduced-motion: reduce) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new DisjunctionExpression(
                new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> null, true)
            ),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parseMultiDisjunction() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) or
                   (prefers-reduced-motion: reduce) or
                   (-fx-prefers-persistent-scrollbars: persistent) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new DisjunctionExpression(
                new DisjunctionExpression(
                    new FunctionExpression<>("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                    new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> false, true)),
                new FunctionExpression<>("-fx-prefers-persistent-scrollbars", "persistent", _ -> false, true)
            ),
            mediaRule.getQueries().getFirst()
        );
    }

    @Test
    void disjunctionAndConjunctionCanNotBeCombined() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: light) or
                   (prefers-reduced-motion: reduce) and
                   (prefers-reduced-transparency: no-preference) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(new ConstantExpression(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void parsePrefersReducedMotion_booleanContext() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new FunctionExpression<>("prefers-reduced-motion", null, _ -> null, true),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parsePrefersReducedTransparency_booleanContext() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-transparency) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new FunctionExpression<>("prefers-reduced-transparency", null, _ -> null, true),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void parsePrefersPersistentScrollBars_booleanContext() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (-fx-prefers-persistent-scrollbars) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(
            new FunctionExpression<>("-fx-prefers-persistent-scrollbars", null, _ -> null, true),
            mediaRule.getQueries().getFirst());
    }

    @Test
    void emptyMediaQuery() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(0, mediaRule.getQueries().size());
    }

    @Test
    void emptyMediaQueryWithNotKeywordEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media not {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(new ConstantExpression(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void invalidMediaFeatureEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (invalid-media-feature) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(new ConstantExpression(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void invalidFeatureValueEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: invalid-value) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(new ConstantExpression(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void unbalancedParenthesisEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: reduce),
                   (prefers-color-scheme: dark,
                   (prefers-reduced-transparency: reduce) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(
            List.of(
                new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> null, true),
                new ConstantExpression(false) // the rest of the query is malformed and evaluates to false
            ),
            mediaRule.getQueries());
    }

    @Test
    void invalidCombinationOfConjunctionAndNegationEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: reduce) and not (prefers-color-scheme: dark) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(1, mediaRule.getQueries().size());
        assertEquals(new ConstantExpression(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void parserRecoversWhenMediaQueryIsMalformed() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (#123foo=malformed-query), (prefers-reduced-motion: reduce) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule = RuleHelper.getMediaRule(stylesheet.getRules().getFirst());
        assertEquals(
            List.of(
                new ConstantExpression(false), // the malformed query evaluates to false
                new FunctionExpression<>("prefers-reduced-motion", "reduce", _ -> null, true)
            ),
            mediaRule.getQueries());
    }

    @Test
    void parserRecoversFromUnbalancedCurlyBrace() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: dark) {{
                .foo { bar: baz; }
            }

            .foo { qux: quux; }
            """);

        assertEquals(2, stylesheet.getRules().size());
        assertEquals(List.of(), stylesheet.getRules().get(0).getDeclarations());
        assertEquals("qux", stylesheet.getRules().get(1).getDeclarations().getFirst().getProperty());
    }

    @Test
    void missingClosingCurlyBraceEmitsParserError() {
        CssParser.errorsProperty().clear();

        var stylesheet = new CssParser().parse("""
            @media (prefers-color-scheme: dark) {
                .foo { bar: baz; }
            """);

        assertTrue(CssParser.errorsProperty().getFirst().getMessage().startsWith("Expected RBRACE"));
        assertEquals(1, stylesheet.getRules().size());
        assertEquals(Set.of("foo"), stylesheet.getRules().getFirst().getSelectors().getFirst().getStyleClassNames());
        assertEquals(
            List.of(new FunctionExpression<>("prefers-color-scheme", "dark", _ -> null, ColorScheme.DARK)),
            RuleHelper.getMediaRule(stylesheet.getRules().getFirst()).getQueries());
    }
}
