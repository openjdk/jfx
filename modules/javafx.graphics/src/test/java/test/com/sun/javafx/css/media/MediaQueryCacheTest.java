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

package test.com.sun.javafx.css.media;

import com.sun.javafx.css.media.MediaQuery;
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
import java.util.function.Supplier;
import javafx.css.Size;
import javafx.css.SizeUnits;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class MediaQueryCacheTest {

    enum MediaQueryDeduplicationTest {
        CONJUNCTION(() -> ConjunctionExpression.of(ConstantExpression.of(true), ConstantExpression.of(false))),
        DISJUNCTION(() -> DisjunctionExpression.of(ConstantExpression.of(true), ConstantExpression.of(false))),
        CONSTANT(() -> ConstantExpression.of(true)),
        FUNCTION(() -> FunctionExpression.of("test", "value", _ -> 0, 0)),
        NEGATION(() -> NegationExpression.of(ConstantExpression.of(true))),
        GREATER_SIZE(() -> GreaterExpression.ofSize(SizeQueryType.WIDTH, new Size(1, SizeUnits.PX))),
        GREATER_NUMBER(() -> GreaterExpression.ofNumber(SizeQueryType.WIDTH, 123)),
        GREATER_OR_EQUAL_SIZE(() -> GreaterOrEqualExpression.ofSize(SizeQueryType.WIDTH, new Size(1, SizeUnits.PX))),
        GREATER_OR_EQUAL_NUMBER(() -> GreaterOrEqualExpression.ofNumber(SizeQueryType.WIDTH, 123)),
        LESS_SIZE(() -> LessExpression.ofSize(SizeQueryType.WIDTH, new Size(1, SizeUnits.PX))),
        LESS_NUMBER(() -> LessExpression.ofNumber(SizeQueryType.WIDTH, 123)),
        LESS_OR_EQUAL_SIZE(() -> LessOrEqualExpression.ofSize(SizeQueryType.WIDTH, new Size(1, SizeUnits.PX))),
        LESS_OR_EQUAL_NUMBER(() -> LessOrEqualExpression.ofNumber(SizeQueryType.WIDTH, 123)),
        EQUAL_SIZE(() -> EqualExpression.ofSize(SizeQueryType.WIDTH, new Size(1, SizeUnits.PX))),
        EQUAL_NUMBER(() -> EqualExpression.ofNumber(SizeQueryType.WIDTH, 123));

        MediaQueryDeduplicationTest(Supplier<MediaQuery> supplier) {
            this.supplier = supplier;
        }

        final Supplier<MediaQuery> supplier;
    }

    @ParameterizedTest
    @EnumSource(MediaQueryDeduplicationTest.class)
    void equalMediaQueriesAreDeduplicated(MediaQueryDeduplicationTest test) {
        assertSame(test.supplier.get(), test.supplier.get());
    }
}
