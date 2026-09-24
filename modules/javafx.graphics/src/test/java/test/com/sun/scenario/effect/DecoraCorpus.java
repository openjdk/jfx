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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Brightpass;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.ColorAdjust;
import com.sun.scenario.effect.DisplacementMap;
import com.sun.scenario.effect.Flood;
import com.sun.scenario.effect.FloatMap;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.InvertMask;
import com.sun.scenario.effect.PerspectiveTransform;
import com.sun.scenario.effect.PhongLighting;
import com.sun.scenario.effect.SepiaTone;
import com.sun.scenario.effect.light.DistantLight;
import com.sun.scenario.effect.light.Light;
import com.sun.scenario.effect.light.PointLight;
import com.sun.scenario.effect.light.SpotLight;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import test.com.sun.scenario.effect.DecoraBackend.Image;
import test.com.sun.scenario.effect.DecoraBackend.Result;

/**
 * The Decora software-peer corpus: backend-independent rendering recipes over deterministic inputs, their
 * parity bounds, and the pixel helpers that compare two renders.
 * <p>
 * Every recipe runs on any {@link DecoraBackend}, so one corpus drives every comparison of two backends and
 * every throughput measurement over the same inputs. Nothing here depends on which backend is available.
 * <p>
 * A case's bound ({@link Bound}) is stated per native library: a bound is tightened only on a platform where the
 * case was measured at the tighter value, and never rises above the bound the case had before the Java
 * {@code LinearConvolve} and {@code BoxShadow} peers were aligned with the native ones.
 */
final class DecoraCorpus {

    static final int EXACT = 0;
    static final int ONE_STEP = 1;
    /**
     * The Gaussian shadow bound from when the Java shadow peer ran {@code filterVector} (truncating) against the
     * native {@code filterHV} (rounding half-up) over two passes; kept only where no tighter bound was measured.
     */
    static final int THREE_STEPS = 3;

    static final int[][] SIZES = {{64, 48}, {257, 129}};

    static final Color4f SHADOW_TINT = new Color4f(1f, 0.5f, 0.25f, 0.8f);

    /** Filter-space size of the oversized-image cases; their physical image is rounded up to the pool quantum. */
    static final int OVERSIZED_WIDTH = 257;
    static final int OVERSIZED_HEIGHT = 129;

    /** The size {@code ImagePool.checkOut} rounds every requested width and height up to a multiple of. */
    private static final int POOL_QUANTUM = 32;

    /** The {@link #pattern} seed of every recipe's primary source image. */
    static final long PRIMARY_SEED = 0x9E3779B9L;
    /** The {@link #pattern} seed of the second source image of the two-input recipes. */
    static final long SECONDARY_SEED = 0x7F4A7C15L;

    /** Width and height of the {@code DisplacementMap} recipes' float map. */
    static final int FLOAT_MAP_SIZE = 16;

    /** The separator between the parts of a golden row key; no effect name or parameter text contains it. */
    static final String KEY_SEPARATOR = " | ";

    /** Finding 1: full positive contrast drives the largest channel to 0 and {@code rgb_to_hsb} divides by it. */
    static final Cause FULL_CONTRAST_DIVISION_BY_ZERO = new Cause("F1", DecoraCorpus::fullContrastZeroesTheMaxChannel);

    /**
     * The golden rows a planned change to the JSL shaders is expected to move: the {@code ColorAdjust} rows with
     * full contrast, whose differences finding 1 explains, and the {@code ColorAdjust} row whose Java render holds
     * NaN pixels from the same division. The golden stores their complete native frames at every size, so they stay
     * judgeable after the Java output of such a change moves.
     */
    static final Set<String> FULL_FRAME_ROWS = Set.of(
            key("ColorAdjust/full-contrast (JSL 0/0)", "hue=1 sat=1 bri=1 con=1", 64, 48),
            key("ColorAdjust/full-contrast (JSL 0/0)", "hue=1 sat=1 bri=1 con=1", 257, 129),
            key("ColorAdjust", "hue=-0.25 sat=0.90 bri=-0.60 con=0.40", 257, 129));

    private DecoraCorpus() {
    }

    /**
     * The native library a parity bound is stated against. {@code decora_sse} was compiled with {@code /fp:fast} on
     * Windows and with {@code -ffast-math} on Linux and macOS, so a distance measured against one library says
     * nothing about another.
     */
    enum NativePlatform {
        WINDOWS, LINUX, MAC
    }

    /**
     * A case's parity bound against each native library. {@code windows} is the bound the Decora golden holds the
     * Java peers to on every OS. {@code linux} is the bound the Java peers' distance from the Linux library was checked
     * against just before {@code decora_sse} was deleted (2026-09). It was not tightened to what that check measured,
     * so it may be looser; no test judges it any more, and {@link DecoraGoldens} summarises what the measurement found.
     * {@code before} is the bound the case had before the Java peers were aligned with the native ones: macOS, never
     * measured, keeps it for every case, and no platform's bound may exceed it.
     */
    record Bound(int windows, int linux, int before) {

        Bound {
            if (windows > before || linux > before) {
                throw new IllegalArgumentException("a bound may be tightened, never loosened: windows " + windows
                        + ", linux " + linux + ", before " + before);
            }
        }

        /** The same bound against every library. */
        static Bound everywhere(int bound) {
            return new Bound(bound, bound, bound);
        }

        int on(NativePlatform platform) {
            return switch (platform) {
                case WINDOWS -> windows;
                case LINUX -> linux;
                case MAC -> before;
            };
        }
    }

    /** One backend-independent rendering recipe. */
    record Case(String effect, String params, Bound bound, BiFunction<DecoraBackend, Inputs, Result> run) {

        /** A recipe with the same bound against every library. */
        Case(String effect, String params, int bound, BiFunction<DecoraBackend, Inputs, Result> run) {
            this(effect, params, Bound.everywhere(bound), run);
        }
    }

    /** The source images of one backend for one size; the second image feeds two-input peers. */
    record Inputs(int width, int height, Image primary, Image secondary) {
    }

    /**
     * A kernel rendered through a clip that cuts into its result, and the same kernel without a clip: inside
     * the clip, the clipped render has to reproduce the unclipped one.
     */
    record ClipCase(String effect, String params, Bound bound, ClippableRecipe kernel, ClipShape shape) {

        /** A clip case with the same bound against every library. */
        ClipCase(String effect, String params, int bound, ClippableRecipe kernel, ClipShape shape) {
            this(effect, params, Bound.everywhere(bound), kernel, shape);
        }

        Result clipped(DecoraBackend backend, Inputs in) {
            return kernel.render(backend, in, shape.clip(in.width(), in.height()));
        }

        Result unclipped(DecoraBackend backend, Inputs in) {
            return kernel.render(backend, in, null);
        }
    }

    /** A rendering recipe that takes its output clip as an argument; {@code null} renders unclipped. */
    @FunctionalInterface
    interface ClippableRecipe {
        Result render(DecoraBackend backend, Inputs in, Rectangle clip);
    }

    /** A clip rectangle for a {@code width x height} source whose bounds start at the origin. */
    @FunctionalInterface
    interface ClipShape {
        Rectangle clip(int width, int height);
    }

    /** The bound of a golden row against the native library of {@code platform}. */
    static int bound(GoldenRow row, NativePlatform platform) {
        return row.bound().on(platform);
    }

    /**
     * A diagnosed reason why the Java peers may differ from the native ones beyond a row's bound. {@code id} is what
     * the golden records; the predicate is asked about the primary source pixel at the position of a differing
     * result pixel, so a cause applies only to rows whose result has the bounds of their source.
     */
    record Cause(String id, IntPredicate explainsSourcePixel) {
    }

    /**
     * One row of the Decora golden: a recipe over {@code inputs(width, height)} with its Windows and Linux bounds
     * and an optional cause. A clipped row also carries the recipe of the same kernel without the clip
     * ({@code unclipped}), and a clipped blur the number of rows next to the clip's top and bottom edges within which
     * the clipped render may differ from the unclipped one by more than one step ({@code edgeRows}, else -1).
     */
    record GoldenRow(String effect, String params, int width, int height, Bound bound, Cause cause,
                     BiFunction<DecoraBackend, Inputs, Result> run, BiFunction<DecoraBackend, Inputs, Result> unclipped,
                     int edgeRows) {

        /** {@code effect | params | WxH}, unique within {@link #rows()}. */
        String key() {
            return DecoraCorpus.key(effect, params, width, height);
        }

        String size() {
            return width + "x" + height;
        }

        String causeId() {
            return cause == null ? "-" : cause.id();
        }

        boolean clipped() {
            return unclipped != null;
        }

        /** Renders the row on {@code backend} from freshly created inputs. */
        Result render(DecoraBackend backend) {
            return run.apply(backend, inputs(width, height));
        }

        /** Renders the row's kernel without its clip on {@code backend} from freshly created inputs. */
        Result renderUnclipped(DecoraBackend backend) {
            return unclipped.apply(backend, inputs(width, height));
        }
    }

    static String key(String effect, String params, int width, int height) {
        return effect + KEY_SEPARATOR + params + KEY_SEPARATOR + width + "x" + height;
    }

    /**
     * Every golden row, in a fixed order: the corpus cases, {@code ColorAdjust} with full contrast, the Gaussian blur
     * clipped on all four edges, the clip matrix, the oversized pool images, the translated inputs and the
     * degenerate inputs. The corpus, clip-matrix and degenerate rows are exactly the cases the parity providers
     * return, in their order.
     */
    static List<GoldenRow> rows() {
        List<GoldenRow> rows = new ArrayList<>();
        cases().forEach(a -> {
            Case c = (Case) a.get()[1];
            rows.add(new GoldenRow(c.effect(), c.params(), (int) a.get()[2], (int) a.get()[3],
                    c.bound(), null, c.run(), null, -1));
        });
        for (int[] size : SIZES) {
            rows.add(new GoldenRow("ColorAdjust/full-contrast (JSL 0/0)",
                    "hue=1 sat=1 bri=1 con=1", size[0], size[1], Bound.everywhere(ONE_STEP),
                    FULL_CONTRAST_DIVISION_BY_ZERO, colorAdjust(1f, 1f, 1f, 1f), null, -1));
        }
        for (float radius : new float[] {5f, 25f}) {
            for (int[] size : SIZES) {
                String params = String.format(Locale.ROOT, "radius=%.1f clip 10,8 %dx%d", radius, size[0] - 20,
                        size[1] - 16);
                rows.add(new GoldenRow("LinearConvolve/gaussian clipped", params, size[0],
                        size[1], Bound.everywhere(ONE_STEP), null,
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, 0f, false, null,
                                new Rectangle(10, 8, in.width() - 20, in.height() - 16)),
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, 0f, false, null, null),
                        (int) Math.ceil(radius)));
            }
        }
        clipCases().forEach(a -> {
            ClipCase c = (ClipCase) a.get()[1];
            rows.add(new GoldenRow(c.effect(), c.params(), (int) a.get()[2], (int) a.get()[3],
                    c.bound(), null, c::clipped, c::unclipped, -1));
        });
        for (Case c : oversizedCases()) {
            rows.add(new GoldenRow(c.effect(), c.params(), OVERSIZED_WIDTH, OVERSIZED_HEIGHT,
                    c.bound(), null, c.run(), null, -1));
        }
        for (int[] size : SIZES) {
            for (Case c : translatedCases()) {
                rows.add(new GoldenRow(c.effect(), c.params(), size[0], size[1], c.bound(), null,
                        c.run(), null, -1));
            }
        }
        degenerateCases().forEach(a -> {
            Case c = (Case) a.get()[1];
            rows.add(new GoldenRow(c.effect(), c.params(), (int) a.get()[2], (int) a.get()[3],
                    c.bound(), null, c.run(), null, -1));
        });
        Set<String> keys = new HashSet<>();
        for (GoldenRow row : rows) {
            if (row.key().split(" \\| ", -1).length != 3 || !keys.add(row.key())) {
                throw new IllegalStateException("golden row key not unique or not three parts: " + row.key());
            }
        }
        if (!keys.containsAll(FULL_FRAME_ROWS)) {
            throw new IllegalStateException("FULL_FRAME_ROWS names rows the corpus does not have: "
                    + FULL_FRAME_ROWS);
        }
        return rows;
    }

    static Stream<Arguments> cases() {
        List<Arguments> args = new ArrayList<>();
        for (Case c : allCases()) {
            for (int[] size : SIZES) {
                args.add(Arguments.of(c.effect() + " " + c.params() + " " + size[0] + "x" + size[1], c, size[0],
                        size[1]));
            }
        }
        return args.stream();
    }

    static List<Case> allCases() {
        List<Case> cases = new ArrayList<>();
        boxCases(cases);
        gaussianCases(cases);
        generatedCases(cases);
        return cases;
    }

    static void boxCases(List<Case> cases) {
        // (hsize, vsize, passes): no-op, odd, even, three passes, wide, one-dimensional, tall.
        float[][] sizes = {{0, 0, 1}, {3, 3, 1}, {4, 6, 1}, {5, 5, 3}, {25, 25, 2}, {51, 3, 1}, {1, 7, 3}};
        for (float[] s : sizes) {
            String params = String.format(Locale.ROOT, "h=%.0f v=%.0f passes=%.0f", s[0], s[1], s[2]);
            cases.add(new Case("BoxBlur", params, EXACT,
                    (b, in) -> b.box(b.data(in.primary(), 0, 0), s[0], s[1], (int) s[2], 0f, false, null, null)));
            cases.add(new Case("BoxShadow", params + " black", EXACT,
                    (b, in) -> b.box(b.data(in.primary(), 0, 0), s[0], s[1], (int) s[2], 0f, true, Color4f.BLACK,
                            null)));
            cases.add(new Case("BoxShadow", params + " tinted", EXACT,
                    (b, in) -> b.box(b.data(in.primary(), 0, 0), s[0], s[1], (int) s[2], 0f, true, SHADOW_TINT,
                            null)));
        }
        // Non-zero origin of the source in filter space.
        cases.add(new Case("BoxBlur", "h=5 v=5 passes=1 origin=-7,3", EXACT,
                (b, in) -> b.box(b.data(in.primary(), -7, 3), 5, 5, 1, 0f, false, null, null)));
        // spread != 0 makes BoxRenderState choose the LinearConvolveShadow peer (GENERAL_VECTOR pass); exact against
        // both measured libraries.
        Bound spreadBound = new Bound(EXACT, EXACT, ONE_STEP);
        float[][] spreadSizes = {{5, 5, 1}, {25, 25, 2}, {4, 6, 3}};
        for (float[] s : spreadSizes) {
            for (float spread : new float[] {0.3f, 1f}) {
                String params = String.format(Locale.ROOT, "box h=%.0f v=%.0f passes=%.0f spread=%.1f", s[0], s[1],
                        s[2], spread);
                cases.add(new Case("LinearConvolveShadow/box", params + " black", spreadBound,
                        (b, in) -> b.box(b.data(in.primary(), 0, 0), s[0], s[1], (int) s[2], spread, true,
                                Color4f.BLACK, null)));
                cases.add(new Case("LinearConvolveShadow/box", params + " tinted", spreadBound,
                        (b, in) -> b.box(b.data(in.primary(), 0, 0), s[0], s[1], (int) s[2], spread, true,
                                SHADOW_TINT, null)));
            }
        }
    }

    static void gaussianCases(List<Case> cases) {
        // Centered passes: filterHV on both sides, clamped with the same constant. Linux keeps one step: its
        // library's -ffast-math reassociates the native filterHV sums, which moves a truncated channel by one on
        // most radii (an IEEE-strict build of the same C++ is exact).
        Bound centered = new Bound(EXACT, ONE_STEP, ONE_STEP);
        for (float radius : new float[] {0f, 0.5f, 1f, 2.5f, 5f, 12.3f, 25f, 63f}) {
            String params = String.format(Locale.ROOT, "radius=%.1f centered", radius);
            cases.add(new Case("LinearConvolve/gaussian", params, centered,
                    (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, 0f, false, null, null)));
        }
        cases.add(new Case("LinearConvolve/gaussian", "xradius=7 yradius=2 centered", centered,
                (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), 7f, 2f, 0f, false, null, null)));
        // A clip that leaves the raw result intact is honoured identically; cutting clips are the clip matrix.
        cases.add(new Case("LinearConvolve/gaussian", "radius=5.0 enclosing clip", centered,
                (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), 5f, 5f, 0f, false, null,
                        new Rectangle(-100, -100, in.width() + 200, in.height() + 200))));
        // Shadows: filterHV on both sides, two passes.
        for (float radius : new float[] {1f, 5f, 25f}) {
            for (float spread : new float[] {0f, 0.5f}) {
                // r=25, spread=0: the native loops' reassociated float sums (/fp:fast, -ffast-math) move one step on
                // a few pixels; an IEEE-strict native build of the same C++ is exact.
                int tightened = (radius == 25f && spread == 0f) ? ONE_STEP : EXACT;
                Bound bound = new Bound(tightened, tightened, THREE_STEPS);
                String params = String.format(Locale.ROOT, "radius=%.1f spread=%.1f", radius, spread);
                cases.add(new Case("LinearConvolveShadow/gaussian", params + " black", bound,
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, spread, true,
                                Color4f.BLACK, null)));
                cases.add(new Case("LinearConvolveShadow/gaussian", params + " tinted", bound,
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, spread, true,
                                SHADOW_TINT, null)));
            }
        }
        // Directional (motion) kernels: GENERAL_VECTOR, filterVector on both sides.
        float[][] motions = {{5f, 30f}, {10f, 90f}, {25f, 0f}, {3.5f, 200f}};
        for (float[] m : motions) {
            double angle = Math.toRadians(m[1]);
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            String params = String.format(Locale.ROOT, "motion radius=%.1f angle=%.0f", m[0], m[1]);
            cases.add(new Case("LinearConvolve/motion", params, ONE_STEP,
                    (b, in) -> b.motion(b.data(in.primary(), 0, 0), m[0], dx, dy, null)));
        }
    }

    static void generatedCases(List<Case> cases) {
        // hue, saturation, brightness, contrast in [-1, 1]; full positive contrast is finding 1.
        float[][] adjusts = {{0, 0, 0, 0}, {1, 1, 1, 0}, {-1, -1, -1, -1}, {0.5f, -0.3f, 0.2f, -0.7f},
                {-0.25f, 0.9f, -0.6f, 0.4f}};
        for (float[] a : adjusts) {
            String params = String.format(Locale.ROOT, "hue=%.2f sat=%.2f bri=%.2f con=%.2f", a[0], a[1], a[2], a[3]);
            cases.add(new Case("ColorAdjust", params, ONE_STEP, colorAdjust(a[0], a[1], a[2], a[3])));
        }
        for (float level : new float[] {0f, 0.5f, 1f}) {
            cases.add(new Case("SepiaTone", String.format(Locale.ROOT, "level=%.1f", level), ONE_STEP, (b, in) -> {
                SepiaTone effect = new SepiaTone();
                effect.setLevel(level);
                return b.generated(effect, "SepiaTone", null, b.data(in.primary(), 0, 0));
            }));
        }
        // Brightpass and Blend SRC_IN: exact against both measured libraries.
        Bound exactWhereMeasured = new Bound(EXACT, EXACT, ONE_STEP);
        for (float threshold : new float[] {0f, 0.3f, 0.7f, 1f}) {
            cases.add(new Case("Brightpass", String.format(Locale.ROOT, "threshold=%.1f", threshold),
                    exactWhereMeasured, (b, in) -> {
                        Brightpass effect = new Brightpass();
                        effect.setThreshold(threshold);
                        return b.generated(effect, "Brightpass", null, b.data(in.primary(), 0, 0));
                    }));
        }
        for (Blend.Mode mode : Blend.Mode.values()) {
            Bound bound = mode == Blend.Mode.SRC_IN ? exactWhereMeasured : Bound.everywhere(ONE_STEP);
            for (float opacity : new float[] {1f, 0.5f}) {
                String params = String.format(Locale.ROOT, "%s opacity=%.1f", mode.name(), opacity);
                cases.add(new Case("Blend_" + mode.name(), params, bound, (b, in) -> {
                    Blend effect = new Blend(mode, null, null);
                    effect.setOpacity(opacity);
                    return b.generated(effect, "Blend_" + mode.name(), null, b.data(in.primary(), 0, 0),
                            b.data(in.secondary(), 0, 0));
                }));
            }
        }
        int[][] masks = {{0, 0, 0}, {5, 0, 0}, {0, 3, -2}, {5, 3, -2}};
        for (int[] m : masks) {
            String params = String.format(Locale.ROOT, "pad=%d offset=%d,%d", m[0], m[1], m[2]);
            cases.add(new Case("InvertMask", params, ONE_STEP, (b, in) -> {
                InvertMask effect = new InvertMask(m[0]);
                effect.setOffsetX(m[1]);
                effect.setOffsetY(m[2]);
                return b.generated(effect, "InvertMask", null, b.data(in.primary(), 0, 0));
            }));
        }
        cases.add(new Case("PerspectiveTransform", "identity quad", ONE_STEP,
                (b, in) -> perspective(b, in, 0, 0, in.width(), 0, in.width(), in.height(), 0, in.height())));
        cases.add(new Case("PerspectiveTransform", "skewed quad", ONE_STEP,
                (b, in) -> perspective(b, in, 10, 5, in.width() - 4, 12, in.width() - 12, in.height() - 3, 6,
                        in.height() - 8)));
        float[][] phongs = {{1.5f, 1f, 0.3f, 20f}, {5f, 2f, 1f, 1f}};
        for (float[] p : phongs) {
            String params = String.format(Locale.ROOT, "scale=%.1f kd=%.1f ks=%.1f exp=%.0f", p[0], p[1], p[2], p[3]);
            cases.add(new Case("PhongLighting_DISTANT", params, ONE_STEP,
                    (b, in) -> phong(b, in, new DistantLight(45f, 60f, Color4f.WHITE), p)));
            cases.add(new Case("PhongLighting_POINT", params, ONE_STEP, (b, in) -> phong(b, in,
                    new PointLight(in.width() / 2f, in.height() / 3f, 40f, new Color4f(1f, 0.8f, 0.6f, 1f)), p)));
            cases.add(new Case("PhongLighting_SPOT", params, ONE_STEP, (b, in) -> {
                SpotLight light = new SpotLight(in.width() / 4f, in.height() / 2f, 30f, Color4f.WHITE);
                light.setPointsAtX(in.width() / 2f);
                light.setPointsAtY(in.height() / 2f);
                light.setPointsAtZ(0f);
                light.setSpecularExponent(2f);
                return phong(b, in, light, p);
            }));
        }
        cases.add(new Case("DisplacementMap", "scale=1,1 offset=0,0 wrap=false", ONE_STEP,
                (b, in) -> displacement(b, in, 1f, 1f, 0f, 0f, false)));
        cases.add(new Case("DisplacementMap", "scale=0.5,-0.5 offset=0.1,-0.05 wrap=true", ONE_STEP,
                (b, in) -> displacement(b, in, 0.5f, -0.5f, 0.1f, -0.05f, true)));
    }

    static BiFunction<DecoraBackend, Inputs, Result> colorAdjust(float hue, float saturation, float brightness,
                                                                 float contrast) {
        return (b, in) -> {
            ColorAdjust effect = new ColorAdjust();
            effect.setHue(hue);
            effect.setSaturation(saturation);
            effect.setBrightness(brightness);
            effect.setContrast(contrast);
            return b.generated(effect, "ColorAdjust", null, b.data(in.primary(), 0, 0));
        };
    }

    /**
     * The Java peer's arithmetic for {@code contrast = 1} ({@code c * 3 + 1 = 4}) up to {@code rgb_to_hsb}:
     * does the largest un-premultiplied, contrast-adjusted channel land on exactly {@code 0.0} while the
     * channels are not all equal, so that {@code s = (cmax - cmin) / cmax} divides by zero?
     */
    static boolean fullContrastZeroesTheMaxChannel(int argb) {
        float a = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xff) / 255f;
        float g = ((argb >> 8) & 0xff) / 255f;
        float bl = (argb & 0xff) / 255f;
        if (a > 0.0f) {
            r /= a;
            g /= a;
            bl /= a;
        }
        float contrast = 4f;
        r = ((r - 0.5f) * contrast) + 0.5f;
        g = ((g - 0.5f) * contrast) + 0.5f;
        bl = ((bl - 0.5f) * contrast) + 0.5f;
        float cmax = Math.max(Math.max(r, g), bl);
        float cmin = Math.min(Math.min(r, g), bl);
        return cmax == 0.0f && cmax > cmin;
    }

    static Result perspective(DecoraBackend b, Inputs in, float ulx, float uly, float urx, float ury, float lrx,
                              float lry, float llx, float lly) {
        // The peer reads the inverse transform the effect computes in its private setupTransforms; the
        // public transform(Point2D, Effect) runs it for the identity transform, which is what we filter with.
        PerspectiveTransform effect = new PerspectiveTransform(
                new Flood(new Object(), new RectBounds(0, 0, in.width(), in.height())));
        effect.setQuadMapping(ulx, uly, urx, ury, lrx, lry, llx, lly);
        effect.transform(new Point2D(0, 0), null);
        Rectangle clip = new Rectangle(0, 0, in.width(), in.height());
        return b.generated(effect, "PerspectiveTransform", clip, b.data(in.primary(), 0, 0));
    }

    static Result phong(DecoraBackend b, Inputs in, Light light, float[] p) {
        PhongLighting effect = new PhongLighting(light);
        effect.setSurfaceScale(p[0]);
        effect.setDiffuseConstant(p[1]);
        effect.setSpecularConstant(p[2]);
        effect.setSpecularExponent(p[3]);
        ImageData bump = b.data(in.secondary(), 0, 0);
        ImageData content = b.data(in.primary(), 0, 0);
        return b.generated(effect, "PhongLighting_" + light.getType().name(), null, bump, content);
    }

    static Result displacement(DecoraBackend b, Inputs in, float scaleX, float scaleY, float offsetX, float offsetY,
                               boolean wrap) {
        FloatMap map = new FloatMap(FLOAT_MAP_SIZE, FLOAT_MAP_SIZE);
        float[] samples = displacementSamples();
        for (int y = 0; y < FLOAT_MAP_SIZE; y++) {
            for (int x = 0; x < FLOAT_MAP_SIZE; x++) {
                int i = (y * FLOAT_MAP_SIZE + x) * 4;
                map.setSamples(x, y, samples[i], samples[i + 1], samples[i + 2], samples[i + 3]);
            }
        }
        DisplacementMap effect = new DisplacementMap(map);
        effect.setScaleX(scaleX);
        effect.setScaleY(scaleY);
        effect.setOffsetX(offsetX);
        effect.setOffsetY(offsetY);
        effect.setWrap(wrap);
        return b.generated(effect, "DisplacementMap", null, b.data(in.primary(), 0, 0));
    }

    /**
     * The samples of the {@code DisplacementMap} recipes' float map, four per texel ({@code u, v, 0, 0}), row-major.
     * They come from {@code Math.sin} and {@code Math.cos}, so a JDK whose trigonometry differs changes them.
     */
    static float[] displacementSamples() {
        float[] samples = new float[FLOAT_MAP_SIZE * FLOAT_MAP_SIZE * 4];
        for (int y = 0; y < FLOAT_MAP_SIZE; y++) {
            for (int x = 0; x < FLOAT_MAP_SIZE; x++) {
                int i = (y * FLOAT_MAP_SIZE + x) * 4;
                samples[i] = (float) Math.sin(x * 0.7 + y * 0.3) * 0.2f;
                samples[i + 1] = (float) Math.cos(x * 0.2 - y * 0.9) * 0.2f;
            }
        }
        return samples;
    }

    static Stream<Arguments> clipCases() {
        List<Arguments> args = new ArrayList<>();
        for (ClipCase c : allClipCases()) {
            for (int[] size : SIZES) {
                args.add(Arguments.of(c.effect() + " " + c.params() + " " + size[0] + "x" + size[1], c, size[0],
                        size[1]));
            }
        }
        return args.stream();
    }

    /**
     * The clip matrix: six clips, each cutting a different set of edges of the result and reaching 100
     * pixels beyond the edges it leaves alone, times six kernels that run the {@code LinearConvolve},
     * {@code LinearConvolveShadow} (a Gaussian shadow, and a box kernel with a spread) and {@code BoxShadow}
     * peers.
     */
    static List<ClipCase> allClipCases() {
        String[] names = {"bottom only", "right only", "bottom+right", "top only", "left only", "all four edges"};
        ClipShape[] shapes = {
            (w, h) -> new Rectangle(-100, -100, w + 200, 100 + h - 8),
            (w, h) -> new Rectangle(-100, -100, 100 + w - 10, h + 200),
            (w, h) -> new Rectangle(-100, -100, 100 + w - 10, 100 + h - 8),
            (w, h) -> new Rectangle(-100, 8, w + 200, h + 200),
            (w, h) -> new Rectangle(10, -100, w + 200, h + 200),
            (w, h) -> new Rectangle(10, 8, w - 20, h - 16)};
        // A clip that cuts the top or the left edge moves the result origin, so both blur peers take filterVector
        // (JDK-8092042), which is one step off on a few pixels. The shadow kernels are exact against the Windows
        // library at both sizes. Linux keeps their bounds from before the Java peers were aligned, as the earlier Linux
        // trial did not cover these cases; the Linux measurement just before decora_sse was deleted did, and found them
        // within those bounds: largest delta 0 against 3 for the Gaussian shadow and 0 against 1 for the box spread.
        Bound gaussianShadow = new Bound(EXACT, THREE_STEPS, THREE_STEPS);
        Bound boxSpread = new Bound(EXACT, ONE_STEP, ONE_STEP);
        List<ClipCase> cases = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String where = " clip " + names[i];
            ClipShape shape = shapes[i];
            for (float radius : new float[] {3f, 12.3f}) {
                cases.add(new ClipCase("clip/LinearConvolve/gaussian",
                        String.format(Locale.ROOT, "radius=%.1f", radius) + where, ONE_STEP,
                        (b, in, clip) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, 0f, false, null,
                                clip), shape));
            }
            for (float radius : new float[] {3f, 12.3f}) {
                cases.add(new ClipCase("clip/LinearConvolveShadow/gaussian",
                        String.format(Locale.ROOT, "radius=%.1f spread=0.5 black", radius) + where, gaussianShadow,
                        (b, in, clip) -> b.gaussian(b.data(in.primary(), 0, 0), radius, radius, 0.5f, true,
                                Color4f.BLACK, clip), shape));
            }
            cases.add(new ClipCase("clip/LinearConvolveShadow/box", "box h=9 v=9 passes=3 spread=0.5 black" + where,
                    boxSpread, (b, in, clip) -> b.box(b.data(in.primary(), 0, 0), 9f, 9f, 3, 0.5f, true,
                            Color4f.BLACK, clip), shape));
            cases.add(new ClipCase("clip/BoxShadow", "h=9 v=9 passes=3 black" + where, EXACT,
                    (b, in, clip) -> b.box(b.data(in.primary(), 0, 0), 9f, 9f, 3, 0f, true, Color4f.BLACK, clip),
                    shape));
        }
        return cases;
    }

    /**
     * A Gaussian blur of a source whose physical image is larger than its bounds, as every pool image is:
     * the pattern fills the bounds at the image origin and the rest of the image holds {@code padding}, so
     * a peer that samples outside the bounds or scales its texture coordinates wrongly shows it. Sized for
     * {@link #OVERSIZED_WIDTH} x {@link #OVERSIZED_HEIGHT}.
     */
    static List<Case> oversizedCases() {
        // Linux keeps one step: its -ffast-math filterHV sums move the r=5 blur by one, as for the centered cases.
        Bound bound = new Bound(EXACT, ONE_STEP, ONE_STEP);
        List<Case> cases = new ArrayList<>();
        for (int padding : new int[] {0, 0xFF00FF00}) {
            String params = String.format(Locale.ROOT, "radius=5.0 physical %dx%d padding %08x",
                    poolSize(OVERSIZED_WIDTH), poolSize(OVERSIZED_HEIGHT), padding);
            cases.add(new Case("oversized/LinearConvolve/gaussian", params, bound,
                    (b, in) -> b.gaussian(b.data(oversized(in.width(), in.height(), padding),
                            new Rectangle(0, 0, in.width(), in.height())), 5f, 5f, 0f, false, null, null)));
        }
        return cases;
    }

    /** A pool-sized image holding the primary pattern for {@code width x height} and padding elsewhere. */
    static Image oversized(int width, int height, int padding) {
        Image image = new Image(poolSize(width), poolSize(height));
        int[] content = pattern(width, height, PRIMARY_SEED);
        int[] pixels = image.getPixelArray();
        int scan = image.getScanlineStride();
        for (int y = 0; y < image.getPhysicalHeight(); y++) {
            for (int x = 0; x < image.getPhysicalWidth(); x++) {
                pixels[y * scan + x] = (x < width && y < height) ? content[y * width + x] : padding;
            }
        }
        return image;
    }

    private static int poolSize(int size) {
        return (size + POOL_QUANTUM - 1) / POOL_QUANTUM * POOL_QUANTUM;
    }

    /**
     * Box kernels over an input whose {@code ImageData} carries a translation, as the input of a
     * {@code DropShadow} over an {@code ImageInput} on a translated node does. The peers filter the
     * untransformed pixels; the result has to carry the input's translation on to the renderer.
     */
    static List<Case> translatedCases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("translated/BoxShadow", "h=9 v=9 passes=3 black translate=5,7", EXACT,
                (b, in) -> b.box(translated(b, in), 9f, 9f, 3, 0f, true, Color4f.BLACK, null)));
        cases.add(new Case("translated/BoxBlur", "h=9 v=9 passes=3 translate=5,7", EXACT,
                (b, in) -> b.box(translated(b, in), 9f, 9f, 3, 0f, false, null, null)));
        return cases;
    }

    private static ImageData translated(DecoraBackend b, Inputs in) {
        return b.data(in.primary(), 0, 0).transform(BaseTransform.getTranslateInstance(5, 7));
    }

    /**
     * Sources one pixel wide or tall, and clips disjoint from the result, which exercise the range checks
     * the native loops return early on. Rendered on a fresh backend, a loop that returns early leaves a
     * zeroed pool image, so the result is deterministic either way.
     */
    static Stream<Arguments> degenerateCases() {
        // Exact against the Windows library. Linux keeps the float kernels' bound from before the Java peers were
        // aligned, as the earlier Linux trial did not cover these cases; the Linux measurement just before decora_sse
        // was deleted did, and found them within it: SepiaTone 0 against 1 on every source; the Gaussian blur 1 against
        // 1 on the 1x48 source, 0 on the other rows.
        Bound floatKernel = new Bound(EXACT, ONE_STEP, ONE_STEP);
        List<Case> kernels = List.of(
                new Case("degenerate/BoxBlur", "h=3 v=3 passes=1 source", EXACT,
                        (b, in) -> b.box(b.data(in.primary(), 0, 0), 3f, 3f, 1, 0f, false, null, null)),
                new Case("degenerate/LinearConvolve/gaussian", "radius=3.0 source", floatKernel,
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), 3f, 3f, 0f, false, null, null)),
                new Case("degenerate/SepiaTone", "level=0.5 source", floatKernel, (b, in) -> {
                    SepiaTone effect = new SepiaTone();
                    effect.setLevel(0.5f);
                    return b.generated(effect, "SepiaTone", null, b.data(in.primary(), 0, 0));
                }));
        List<Case> disjointClips = List.of(
                new Case("degenerate/LinearConvolve/gaussian", "radius=5.0 disjoint clip 1000,1000 10x10", floatKernel,
                        (b, in) -> b.gaussian(b.data(in.primary(), 0, 0), 5f, 5f, 0f, false, null,
                                new Rectangle(1000, 1000, 10, 10))),
                new Case("degenerate/BoxShadow", "h=9 v=9 passes=3 black disjoint clip 1000,1000 10x10", EXACT,
                        (b, in) -> b.box(b.data(in.primary(), 0, 0), 9f, 9f, 3, 0f, true, Color4f.BLACK,
                                new Rectangle(1000, 1000, 10, 10))));
        List<Arguments> args = new ArrayList<>();
        for (int[] source : new int[][] {{1, 1}, {1, 48}, {64, 1}}) {
            for (Case c : kernels) {
                args.add(Arguments.of(c.effect() + " " + c.params() + " " + source[0] + "x" + source[1], c,
                        source[0], source[1]));
            }
        }
        for (Case c : disjointClips) {
            args.add(Arguments.of(c.effect() + " " + c.params() + " 64x48", c, 64, 48));
        }
        return args.stream();
    }

    static Inputs inputs(int width, int height) {
        return new Inputs(width, height, Image.of(width, height, pattern(width, height, PRIMARY_SEED)),
                Image.of(width, height, pattern(width, height, SECONDARY_SEED)));
    }

    /**
     * Deterministic ARGB-pre test content: an opaque white frame, a transparent frame inside it, a hard
     * opaque red diagonal band, and pseudo-random premultiplied noise (alpha first, colour never above
     * alpha) everywhere else. The generator is a fixed xorshift so the pattern does not depend on the JDK.
     */
    static int[] pattern(int width, int height, long seed) {
        int[] pixels = new int[width * height];
        long state = seed;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                state ^= state << 13;
                state ^= state >>> 7;
                state ^= state << 17;
                int argb;
                if (x < 3 || y < 3 || x >= width - 3 || y >= height - 3) {
                    argb = 0xFFFFFFFF;
                } else if (x < 6 || y < 6 || x >= width - 6 || y >= height - 6) {
                    argb = 0;
                } else if (Math.abs((x - y) % 23) < 3) {
                    argb = 0xFFFF0000;
                } else {
                    int a = (int) (state & 0xFF);
                    int r = (int) ((state >>> 8) & 0xFF) * a / 255;
                    int g = (int) ((state >>> 16) & 0xFF) * a / 255;
                    int bl = (int) ((state >>> 24) & 0xFF) * a / 255;
                    argb = (a << 24) | (r << 16) | (g << 8) | bl;
                }
                pixels[y * width + x] = argb;
            }
        }
        return pixels;
    }

    /** The pixels of {@code full} that fall inside the bounds of {@code window}, as a result with those bounds. */
    static Result crop(Result full, Result window) {
        int[] pixels = new int[window.width() * window.height()];
        for (int row = 0; row < window.height(); row++) {
            int srcRow = window.y() - full.y() + row;
            int srcCol = window.x() - full.x();
            System.arraycopy(full.pixels(), srcRow * full.width() + srcCol, pixels, row * window.width(),
                    window.width());
        }
        return new Result(window.x(), window.y(), window.width(), window.height(), pixels, full.image(),
                full.transform());
    }

    static int maxDelta(Result a, Result b) {
        int maxDelta = 0;
        for (int i = 0; i < a.pixels().length; i++) {
            maxDelta = Math.max(maxDelta, channelDelta(a.pixels()[i], b.pixels()[i]));
        }
        return maxDelta;
    }

    static int channelDelta(int p, int q) {
        int d = Math.abs((p >>> 24) - (q >>> 24));
        d = Math.max(d, Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF)));
        d = Math.max(d, Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF)));
        return Math.max(d, Math.abs((p & 0xFF) - (q & 0xFF)));
    }

    /** The six entries {@code mxx,myx,mxy,myy,mxt,myt} of a result's 2D transform, for comparing and reporting. */
    static String matrix(BaseTransform tx) {
        return tx.getMxx() + "," + tx.getMyx() + "," + tx.getMxy() + "," + tx.getMyy() + "," + tx.getMxt() + ","
                + tx.getMyt();
    }

    /** Gaussian weights summing to {@code scale}, laid out twice in a row as the peers do for {@code filterHV}. */
    static float[] duplicatedGaussianWeights(int radius, float scale) {
        int count = 2 * radius + 1;
        float[] w = new float[count];
        float sum = 0f;
        for (int i = 0; i < count; i++) {
            w[i] = (float) Math.exp(-(i - radius) * (i - radius) / (2.0 * radius * radius / 9.0));
            sum += w[i];
        }
        float[] doubled = new float[count * 2];
        for (int i = 0; i < count; i++) {
            doubled[i] = w[i] * scale / sum;
            doubled[count + i] = doubled[i];
        }
        return doubled;
    }
}
