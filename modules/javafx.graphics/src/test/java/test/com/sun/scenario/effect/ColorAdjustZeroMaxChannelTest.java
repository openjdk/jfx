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

package test.com.sun.scenario.effect;

import com.sun.scenario.effect.impl.sw.java.JSWColorAdjustPeer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import test.com.sun.scenario.effect.DecoraBackend.Image;
import test.com.sun.scenario.effect.DecoraBackend.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.scenario.effect.DecoraCorpus.PRIMARY_SEED;
import static test.com.sun.scenario.effect.DecoraCorpus.pattern;

/**
 * {@code ColorAdjust} with full contrast on pixels whose largest channel the contrast adjustment moves to exactly
 * {@code 0.0}: {@code rgb_to_hsb} in {@code ColorAdjust.jsl} must take its grey branch ({@code h = s = 0}) there
 * instead of dividing {@code cmax - cmin} by {@code cmax}, which made the saturation {@code +Inf} and the adjusted
 * saturation NaN, and the Java peer rendered two channels black.
 * <p>
 * With {@code hue = sat = bri = con = 1} the peer's parameters are {@code hue 0.5}, {@code saturation 2},
 * {@code brightness 2} and {@code contrast 4}. A channel {@code c} of an un-premultiplied source lands on
 * {@code (c - 0.5) * 4 + 0.5 = 0} for {@code c = 0.375}, which the premultiplied pixel {@code 3k/8k} (for example
 * red 48 at alpha 128) produces exactly in float arithmetic. The grey branch then gives {@code h = 0, s = 0, b = 0};
 * the hue becomes 0.5, the saturation {@code 0 + (1 - 0) * 1 = 1} and then {@code 1 * (1 - 1) = 0}, and the
 * brightness {@code 0 + (1 - 0) * 1 = 1}, so {@code hsb_to_rgb(0.5, 0, 1)} is white, premultiplied by the source
 * alpha. Full brightness sends every other pixel to white as well, up to float rounding in the brightness, so
 * these pixels were the only ones the effect left undefined.
 */
public class ColorAdjustZeroMaxChannelTest {

    /**
     * Pixels whose largest un-premultiplied channel is exactly 0.375, so full contrast moves it to 0.0 while the
     * other channels go to -1.5: the zero channel in red, green and blue, and at the lowest and highest alphas that
     * reach 0.375 exactly.
     */
    @ParameterizedTest(name = "0x{0}")
    @ValueSource(strings = {"80300000", "80003000", "80000030", "08030000", "F85D0000", "80301830"})
    void fullContrastRendersZeroMaxChannelAsWhite(String hex) {
        int source = Integer.parseUnsignedInt(hex, 16);
        assertTrue(DecoraCorpus.fullContrastZeroesTheMaxChannel(source), () -> "0x" + hex
                + " does not move its largest channel to 0.0 under full contrast");

        int rendered = renderFullContrast(new int[] {source}, 1, 1).pixels()[0];

        assertEquals(hex(premultipliedWhite(source >>> 24)), hex(rendered), () -> "ColorAdjust full contrast of 0x"
                + hex);
    }

    /** The pixels of the 64x48 Decora corpus input whose largest channel full contrast moves to 0.0 render as white. */
    @Test
    void fullContrastRendersZeroMaxChannelPixelsOfTheCorpusAsWhite() {
        int width = 64;
        int height = 48;
        int[] source = pattern(width, height, PRIMARY_SEED);
        int[] rendered = renderFullContrast(source, width, height).pixels();

        List<String> wrong = new ArrayList<>();
        int zeroMaxChannel = 0;
        for (int i = 0; i < source.length; i++) {
            if (!DecoraCorpus.fullContrastZeroesTheMaxChannel(source[i])) {
                continue;
            }
            zeroMaxChannel++;
            int expected = premultipliedWhite(source[i] >>> 24);
            if (rendered[i] != expected) {
                wrong.add(String.format(Locale.ROOT, "(%d,%d) src=%08x expected=%08x java=%08x", i % width,
                        i / width, source[i], expected, rendered[i]));
            }
        }
        assertTrue(zeroMaxChannel > 0, "the corpus input has no pixel whose largest channel full contrast moves to"
                + " 0.0");
        assertTrue(wrong.isEmpty(), () -> "ColorAdjust full contrast: " + wrong);
    }

    private static Result renderFullContrast(int[] argbPre, int width, int height) {
        DecoraBackend backend = DecoraBackend.java();
        Result result = DecoraCorpus.colorAdjust(1f, 1f, 1f, 1f).apply(backend,
                new DecoraCorpus.Inputs(width, height, Image.of(width, height, argbPre), null));
        assertEquals(List.of(JSWColorAdjustPeer.class.getName()), List.copyOf(backend.ranPeers()));
        return result;
    }

    /** White premultiplied by {@code alpha}, packed the way the peer packs {@code src.a * (1, 1, 1)}. */
    private static int premultipliedWhite(int alpha) {
        int c = (int) ((alpha / 255f) * 0xff);
        return (alpha << 24) | (c << 16) | (c << 8) | c;
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08x", argb);
    }
}
