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
import com.sun.javafx.css.media.SizeQueryType;
import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.EqualExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.GreaterExpression;
import com.sun.javafx.css.media.expression.GreaterOrEqualExpression;
import com.sun.javafx.css.media.expression.LessExpression;
import com.sun.javafx.css.media.expression.LessOrEqualExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import javafx.application.ColorScheme;
import javafx.css.CssParser;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.Stylesheet;
import java.util.List;
import org.junit.jupiter.api.Test;

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
            FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
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
            NegationExpression.of(FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT)),
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
            FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
            innerMediaRule.getQueries().getFirst());
        assertEquals(
            FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true),
            outerMediaRule.getQueries().getFirst());
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
                FunctionExpression.of("prefers-color-scheme", "dark", _ -> null, ColorScheme.DARK),
                FunctionExpression.of("prefers-reduced-motion", null, _ -> null, true),
                FunctionExpression.of("prefers-reduced-transparency", "no-preference", _ -> null, false)
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
            List.of(ConjunctionExpression.of(
                FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true)
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
            ConjunctionExpression.of(
                ConjunctionExpression.of(
                    FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                    FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> false, true)),
                FunctionExpression.of("prefers-reduced-transparency", "no-preference", _ -> false, false)
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
            DisjunctionExpression.of(
                FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true)
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
            DisjunctionExpression.of(
                DisjunctionExpression.of(
                    FunctionExpression.of("prefers-color-scheme", "light", _ -> null, ColorScheme.LIGHT),
                    FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> false, true)),
                FunctionExpression.of("-fx-prefers-persistent-scrollbars", "persistent", _ -> false, true)
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
        assertEquals(ConstantExpression.of(false), mediaRule.getQueries().getFirst());
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
            FunctionExpression.of("prefers-reduced-motion", null, _ -> null, true),
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
            FunctionExpression.of("prefers-reduced-transparency", null, _ -> null, true),
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
            FunctionExpression.of("-fx-prefers-persistent-scrollbars", null, _ -> null, true),
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
        assertEquals(ConstantExpression.of(false), mediaRule.getQueries().getFirst());
    }

    @Test
    void invalidMediaFeatureEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (invalid-media-feature) {
                .foo { bar: baz; }
            }

            @media (invalid-feature > 100px) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule1 = RuleHelper.getMediaRule(stylesheet.getRules().get(0));
        assertEquals(1, mediaRule1.getQueries().size());
        assertEquals(ConstantExpression.of(false), mediaRule1.getQueries().getFirst());

        var mediaRule2 = RuleHelper.getMediaRule(stylesheet.getRules().get(1));
        assertEquals(1, mediaRule2.getQueries().size());
        assertEquals(ConstantExpression.of(false), mediaRule2.getQueries().getFirst());
    }

    @Test
    void invalidFeatureValueEvaluatesToFalse() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (prefers-reduced-motion: invalid-value) {
                .foo { bar: baz; }
            }

            @media (width > invalid-value) {
                .foo { bar: baz; }
            }
            """);

        var mediaRule1 = RuleHelper.getMediaRule(stylesheet.getRules().get(0));
        assertEquals(1, mediaRule1.getQueries().size());
        assertEquals(ConstantExpression.of(false), mediaRule1.getQueries().getFirst());

        var mediaRule2 = RuleHelper.getMediaRule(stylesheet.getRules().get(1));
        assertEquals(1, mediaRule2.getQueries().size());
        assertEquals(ConstantExpression.of(false), mediaRule2.getQueries().getFirst());
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
                FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true),
                ConstantExpression.of(false) // the rest of the query is malformed and evaluates to false
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
        assertEquals(ConstantExpression.of(false), mediaRule.getQueries().getFirst());
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
                ConstantExpression.of(false), // the malformed query evaluates to false
                FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true)
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
    void parseRangeForm_leadingValue() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (100px >= width) {
                .foo { bar: baz; }
            }

            @media (100px > width) {
                .foo { bar: baz; }
            }

            @media (100px <= width) {
                .foo { bar: baz; }
            }

            @media (100px < width) {
                .foo { bar: baz; }
            }

            @media (100px = width) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(5, stylesheet.getRules().size());
        assertEquals(
            List.of(LessOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(0)).getQueries());
        assertEquals(
            List.of(LessExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(1)).getQueries());
        assertEquals(
            List.of(GreaterOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(2)).getQueries());
        assertEquals(
            List.of(GreaterExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(3)).getQueries());
        assertEquals(
            List.of(EqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(4)).getQueries());
    }

    @Test
    void parseRangeForm_trailingValue() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (width >= 100px) {
                .foo { bar: baz; }
            }

            @media (width > 100px) {
                .foo { bar: baz; }
            }

            @media (width <= 100px) {
                .foo { bar: baz; }
            }

            @media (width < 100px) {
                .foo { bar: baz; }
            }

            @media (width = 100px) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(5, stylesheet.getRules().size());
        assertEquals(
            List.of(GreaterOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(0)).getQueries());
        assertEquals(
            List.of(GreaterExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(1)).getQueries());
        assertEquals(
            List.of(LessOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(2)).getQueries());
        assertEquals(
            List.of(LessExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(3)).getQueries());
        assertEquals(
            List.of(EqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(4)).getQueries());
    }

    @Test
    void parseRangeForm_interval() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (50px > width >= 100px) {
                .foo { bar: baz; }
            }

            @media (50px >= width > 100px) {
                .foo { bar: baz; }
            }

            @media (50px < width <= 100px) {
                .foo { bar: baz; }
            }

            @media (50px <= width < 100px) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(4, stylesheet.getRules().size());
        assertEquals(
            List.of(ConjunctionExpression.of(
                LessExpression.of(SizeQueryType.WIDTH, new Size(50, SizeUnits.PX)),
                GreaterOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX)))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(0)).getQueries());
        assertEquals(
            List.of(ConjunctionExpression.of(
                LessOrEqualExpression.of(SizeQueryType.WIDTH, new Size(50, SizeUnits.PX)),
                GreaterExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX)))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(1)).getQueries());
        assertEquals(
            List.of(ConjunctionExpression.of(
                GreaterExpression.of(SizeQueryType.WIDTH, new Size(50, SizeUnits.PX)),
                LessOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX)))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(2)).getQueries());
        assertEquals(
            List.of(ConjunctionExpression.of(
                GreaterOrEqualExpression.of(SizeQueryType.WIDTH, new Size(50, SizeUnits.PX)),
                LessExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX)))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(3)).getQueries());
    }

    @Test
    void parseRangeForm_invalidInterval() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (50px > width = 100px) {
                .foo { bar: baz; }
            }

            @media (50px > width < 100px) {
                .foo { bar: baz; }
            }

            @media (50px < width > 100px) {
                .foo { bar: baz; }
            }

            @media (50px = width < 100px) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(4, stylesheet.getRules().size());
        assertEquals(
            List.of(ConstantExpression.of(false)), // error: interval has trailing '=' operator
            RuleHelper.getMediaRule(stylesheet.getRules().get(0)).getQueries());
        assertEquals(
            List.of(ConstantExpression.of(false)), // error: operators have different directions
            RuleHelper.getMediaRule(stylesheet.getRules().get(1)).getQueries());
        assertEquals(
            List.of(ConstantExpression.of(false)), // error: operators have different directions
            RuleHelper.getMediaRule(stylesheet.getRules().get(2)).getQueries());
        assertEquals(
            List.of(ConstantExpression.of(false)), // error: interval has leading '=' operator
            RuleHelper.getMediaRule(stylesheet.getRules().get(3)).getQueries());
    }

    @Test
    void parseRangeForm_prefix() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (min-width: 100px) {
                .foo { bar: baz; }
            }

            @media (max-width: 100px) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(2, stylesheet.getRules().size());
        assertEquals(
            List.of(GreaterOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(0)).getQueries());
        assertEquals(
            List.of(LessOrEqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().get(1)).getQueries());
    }

    @Test
    void parseRangeValue_asDiscrete() {
        Stylesheet stylesheet = new CssParser().parse("""
            @media (width: 100px) {
                .foo { bar: baz; }
            }
            """);

        assertEquals(1, stylesheet.getRules().size());
        assertEquals(
            List.of(EqualExpression.of(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX))),
            RuleHelper.getMediaRule(stylesheet.getRules().getFirst()).getQueries());
    }
}
