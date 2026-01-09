/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.media.MediaQuerySerializer;
import com.sun.javafx.css.media.SizeQueryType;
import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.EqualExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.GreaterExpression;
import com.sun.javafx.css.media.expression.GreaterOrEqualExpression;
import com.sun.javafx.css.media.expression.LessExpression;
import com.sun.javafx.css.media.expression.LessOrEqualExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.MediaQuery;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class MediaQuerySerializerTest {

    private StyleConverter.StringStore stringStore;

    @BeforeEach
    void setup() {
        stringStore = new StyleConverter.StringStore();
    }

    @Test
    void serializeConstantExpression() throws IOException {
        var expected = ConstantExpression.of(true);
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = ConstantExpression.of(false);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeFunctionExpression() throws IOException {
        var expected = FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true);
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = FunctionExpression.of("prefers-reduced-motion", null, _ -> null, true);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeNotExpression() throws IOException {
        var expected = NegationExpression.of(ConstantExpression.of(true));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeConjunctionExpression() throws IOException {
        var expected = ConjunctionExpression.of(ConstantExpression.of(true), ConstantExpression.of(false));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeDisjunctionExpression() throws IOException {
        var expected = DisjunctionExpression.of(ConstantExpression.of(true), ConstantExpression.of(false));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeGreaterOrEqualExpression() throws IOException {
        var expected = GreaterOrEqualExpression.ofSize(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = GreaterOrEqualExpression.ofNumber(SizeQueryType.WIDTH, 123);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeGreaterExpression() throws IOException {
        var expected = GreaterExpression.ofSize(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = GreaterExpression.ofNumber(SizeQueryType.WIDTH, 123);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeLessOrEqualExpression() throws IOException {
        var expected = LessOrEqualExpression.ofSize(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = LessOrEqualExpression.ofNumber(SizeQueryType.WIDTH, 123);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeLessExpression() throws IOException {
        var expected = LessExpression.ofSize(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = LessExpression.ofNumber(SizeQueryType.WIDTH, 123);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeEqualExpression() throws IOException {
        var expected = EqualExpression.ofSize(SizeQueryType.WIDTH, new Size(100, SizeUnits.PX));
        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);

        expected = EqualExpression.ofNumber(SizeQueryType.WIDTH, 123);
        actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    @Test
    void serializeComplexExpression() throws IOException {
        var expected = ConjunctionExpression.of(
            DisjunctionExpression.of(
                FunctionExpression.of("prefers-reduced-motion", "reduce", _ -> null, true),
                FunctionExpression.of("prefers-reduced-transparency", "no-preference", _ -> null, false)
            ),
            ConjunctionExpression.of(
                NegationExpression.of(
                    FunctionExpression.of("prefers-reduced-motion", null, _ -> null, true)
                ),
                ConstantExpression.of(true)
            )
        );

        var actual = deserialize(serialize(expected));
        assertEquals(expected, actual);
    }

    private byte[] serialize(MediaQuery mediaQuery) throws IOException {
        var output = new ByteArrayOutputStream();
        MediaQuerySerializer.writeBinary(mediaQuery, new DataOutputStream(output), stringStore);
        return output.toByteArray();
    }

    private MediaQuery deserialize(byte[] data) throws IOException {
        var input = new ByteArrayInputStream(data);
        return MediaQuerySerializer.readBinary(
            new DataInputStream(input), stringStore.strings.toArray(new String[0]));
    }
}
