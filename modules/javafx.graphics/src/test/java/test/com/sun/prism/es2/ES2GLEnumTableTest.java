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

package test.com.sun.prism.es2;

import com.sun.prism.es2.ES2NativeShim;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the three Prism-to-GL translation tables the FFM port moved from {@code GLContext.c} into
 * {@code GLContext.java} - {@code translateScaleFactor}, {@code translatePrismToGL},
 * {@code translatePixelStore} - entry by entry against the OpenGL header values, transcribed here by hand
 * from the headers the native build compiles against ({@code src/main/native-prism-es2/GL/gl.h}, Mesa, and
 * {@code GL/glext.h}, {@code GL_GLEXT_VERSION 20190911}), with the header and line of every value.
 * <p>
 * Why by hand: the C tables resolved these names through the platform headers at compile time; the Java
 * replacement is fifty hex literals typed in a migration commit, and the C originals were deleted in the
 * same commit, so nothing in the tree compares the two. A wrong literal is not a crash: {@code 0x2702} in
 * place of {@code GL_LINEAR_MIPMAP_LINEAR} point-samples every mipmapped minification, and swapping
 * {@code GL_UNPACK_ROW_LENGTH} with {@code GL_UNPACK_SKIP_ROWS} shears every strided upload - on the
 * default Linux pipeline, silently.
 * <p>
 * Pure Java, but for one method: the translators are static and touch no native state, so the first five
 * methods run on every platform, including the Windows build that leaves {@code prism_es2} out.
 * {@link #TABLE} is kept in the order of the "GL enums" comment of {@code prism_es2_api.h} (blend factors,
 * pixel types, pixel formats, textures, wrap modes, pixel store, glGetIntegerv names), which is the order of
 * the {@code static const GLenum} table {@code prism_es2_api.c} exports through {@code es2_gl_enum_count()} /
 * {@code es2_gl_enum(i)}; {@link #theCTableAgreesWithEveryJavaEntry} compares the two index by index. That
 * method alone needs the library - {@code ES2Natives.require()} inside it, never in a {@code @BeforeAll} - so
 * it skips where {@code prism_es2} was left out and the pure methods stay un-skippable.
 */
public class ES2GLEnumTableTest {

    /** Which Java translator an entry goes through. */
    enum Translator {
        SCALE_FACTOR, PRISM_TO_GL, PIXEL_STORE
    }

    /**
     * One table entry: the {@code GLContext} constant ({@code prismName = prismIndex},
     * {@code GLContext.java:41-107}), the OpenGL name and value it must translate to, and the header line
     * that defines that value.
     */
    record GlEnum(Translator translator, String prismName, int prismIndex, String glName, int glValue,
            String header) {

        int translated() {
            return switch (translator) {
                case SCALE_FACTOR -> ES2NativeShim.translateScaleFactor(prismIndex);
                case PRISM_TO_GL -> ES2NativeShim.translatePrismToGL(prismIndex);
                case PIXEL_STORE -> ES2NativeShim.translatePixelStore(prismIndex);
            };
        }
    }

    private static GlEnum scale(String prismName, int prismIndex, String glName, int glValue, String header) {
        return new GlEnum(Translator.SCALE_FACTOR, prismName, prismIndex, glName, glValue, header);
    }

    private static GlEnum gl(String prismName, int prismIndex, String glName, int glValue, String header) {
        return new GlEnum(Translator.PRISM_TO_GL, prismName, prismIndex, glName, glValue, header);
    }

    private static GlEnum store(String prismName, int prismIndex, String glName, int glValue, String header) {
        return new GlEnum(Translator.PIXEL_STORE, prismName, prismIndex, glName, glValue, header);
    }

    /** The fifty entries, in the order of the {@code prism_es2_api.h} comment. */
    static final List<GlEnum> TABLE = List.of(
            // blend factors (translateScaleFactor)
            scale("GL_ZERO", 0, "GL_ZERO", 0x0000, "GL/gl.h:340"),
            scale("GL_ONE", 1, "GL_ONE", 0x0001, "GL/gl.h:341"),
            scale("GL_SRC_COLOR", 2, "GL_SRC_COLOR", 0x0300, "GL/gl.h:342"),
            scale("GL_ONE_MINUS_SRC_COLOR", 3, "GL_ONE_MINUS_SRC_COLOR", 0x0301, "GL/gl.h:343"),
            scale("GL_SRC_ALPHA", 6, "GL_SRC_ALPHA", 0x0302, "GL/gl.h:344"),
            scale("GL_ONE_MINUS_SRC_ALPHA", 7, "GL_ONE_MINUS_SRC_ALPHA", 0x0303, "GL/gl.h:345"),
            scale("GL_DST_ALPHA", 8, "GL_DST_ALPHA", 0x0304, "GL/gl.h:346"),
            scale("GL_ONE_MINUS_DST_ALPHA", 9, "GL_ONE_MINUS_DST_ALPHA", 0x0305, "GL/gl.h:347"),
            scale("GL_DST_COLOR", 4, "GL_DST_COLOR", 0x0306, "GL/gl.h:348"),
            scale("GL_ONE_MINUS_DST_COLOR", 5, "GL_ONE_MINUS_DST_COLOR", 0x0307, "GL/gl.h:349"),
            scale("GL_SRC_ALPHA_SATURATE", 14, "GL_SRC_ALPHA_SATURATE", 0x0308, "GL/gl.h:350"),
            scale("GL_CONSTANT_COLOR", 10, "GL_CONSTANT_COLOR", 0x8001, "GL/gl.h:1521"),
            scale("GL_ONE_MINUS_CONSTANT_COLOR", 11, "GL_ONE_MINUS_CONSTANT_COLOR", 0x8002, "GL/gl.h:1522"),
            scale("GL_CONSTANT_ALPHA", 12, "GL_CONSTANT_ALPHA", 0x8003, "GL/gl.h:1523"),
            scale("GL_ONE_MINUS_CONSTANT_ALPHA", 13, "GL_ONE_MINUS_CONSTANT_ALPHA", 0x8004, "GL/gl.h:1524"),
            // pixel types (translatePrismToGL)
            gl("GL_FLOAT", 20, "GL_FLOAT", 0x1406, "GL/gl.h:149"),
            gl("GL_UNSIGNED_BYTE", 21, "GL_UNSIGNED_BYTE", 0x1401, "GL/gl.h:144"),
            gl("GL_UNSIGNED_INT_8_8_8_8_REV", 22, "GL_UNSIGNED_INT_8_8_8_8_REV", 0x8367, "GL/gl.h:1461"),
            gl("GL_UNSIGNED_INT_8_8_8_8", 23, "GL_UNSIGNED_INT_8_8_8_8", 0x8035, "GL/gl.h:1460"),
            gl("GL_UNSIGNED_SHORT_8_8_APPLE", 24, "GL_UNSIGNED_SHORT_8_8_APPLE", 0x85BA, "GL/glext.h:5920"),
            // pixel formats (translatePrismToGL)
            gl("GL_RGBA", 40, "GL_RGBA", 0x1908, "GL/gl.h:469"),
            gl("GL_BGRA", 41, "GL_BGRA", 0x80E1, "GL/gl.h:1451"),
            gl("GL_RGB", 42, "GL_RGB", 0x1907, "GL/gl.h:468"),
            gl("GL_LUMINANCE", 43, "GL_LUMINANCE", 0x1909, "GL/gl.h:450"),
            gl("GL_ALPHA", 44, "GL_ALPHA", 0x1906, "GL/gl.h:449"),
            gl("GL_RGBA32F", 45, "GL_RGBA32F", 0x8814, "GL/glext.h:901"),
            gl("GL_YCBCR_422_APPLE", 46, "GL_YCBCR_422_APPLE", 0x85B9, "GL/glext.h:6021"),
            // textures (translatePrismToGL)
            gl("GL_TEXTURE_2D", 50, "GL_TEXTURE_2D", 0x0DE1, "GL/gl.h:611"),
            gl("GL_TEXTURE_BINDING_2D", 51, "GL_TEXTURE_BINDING_2D", 0x8069, "GL/gl.h:697"),
            gl("GL_NEAREST", 52, "GL_NEAREST", 0x2600, "GL/gl.h:644"),
            gl("GL_LINEAR", 53, "GL_LINEAR", 0x2601, "GL/gl.h:387"),
            gl("GL_NEAREST_MIPMAP_NEAREST", 54, "GL_NEAREST_MIPMAP_NEAREST", 0x2700, "GL/gl.h:633"),
            gl("GL_LINEAR_MIPMAP_LINEAR", 55, "GL_LINEAR_MIPMAP_LINEAR", 0x2703, "GL/gl.h:636"),
            // wrap modes (translatePrismToGL, WRAPMODE_*)
            gl("WRAPMODE_REPEAT", 100, "GL_REPEAT", 0x2901, "GL/gl.h:645"),
            gl("WRAPMODE_CLAMP_TO_EDGE", 101, "GL_CLAMP_TO_EDGE", 0x812F, "GL/gl.h:1447"),
            gl("WRAPMODE_CLAMP_TO_BORDER", 102, "GL_CLAMP_TO_BORDER", 0x812D, "GL/gl.h:1816"),
            // pixel store (translatePixelStore)
            store("GL_UNPACK_ALIGNMENT", 60, "GL_UNPACK_ALIGNMENT", 0x0CF5, "GL/gl.h:598"),
            store("GL_UNPACK_ROW_LENGTH", 61, "GL_UNPACK_ROW_LENGTH", 0x0CF2, "GL/gl.h:600"),
            store("GL_UNPACK_SKIP_PIXELS", 62, "GL_UNPACK_SKIP_PIXELS", 0x0CF4, "GL/gl.h:601"),
            store("GL_UNPACK_SKIP_ROWS", 63, "GL_UNPACK_SKIP_ROWS", 0x0CF3, "GL/gl.h:602"),
            // glGetIntegerv names (translatePrismToGL)
            gl("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS", 120, "GL_MAX_FRAGMENT_UNIFORM_COMPONENTS", 0x8B49,
                    "GL/glext.h:601"),
            gl("GL_MAX_FRAGMENT_UNIFORM_VECTORS", 121, "GL_MAX_FRAGMENT_UNIFORM_VECTORS", 0x8DFD, "GL/glext.h:1824"),
            gl("GL_MAX_TEXTURE_IMAGE_UNITS", 122, "GL_MAX_TEXTURE_IMAGE_UNITS", 0x8872, "GL/glext.h:598"),
            gl("GL_MAX_TEXTURE_SIZE", 123, "GL_MAX_TEXTURE_SIZE", 0x0D33, "GL/gl.h:476"),
            gl("GL_MAX_VARYING_COMPONENTS", 125, "GL_MAX_VARYING_COMPONENTS", 0x8B4B, "GL/glext.h:911"),
            gl("GL_MAX_VARYING_VECTORS", 126, "GL_MAX_VARYING_VECTORS", 0x8DFC, "GL/glext.h:1823"),
            gl("GL_MAX_VERTEX_ATTRIBS", 124, "GL_MAX_VERTEX_ATTRIBS", 0x8869, "GL/glext.h:596"),
            gl("GL_MAX_VERTEX_UNIFORM_COMPONENTS", 128, "GL_MAX_VERTEX_UNIFORM_COMPONENTS", 0x8B4A,
                    "GL/glext.h:602"),
            gl("GL_MAX_VERTEX_UNIFORM_VECTORS", 129, "GL_MAX_VERTEX_UNIFORM_VECTORS", 0x8DFB, "GL/glext.h:1822"),
            gl("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", 127, "GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", 0x8B4C,
                    "GL/glext.h:604"));

    @Test
    public void theTableHasTheFiftyEntriesOfTheHeaderComment() {
        assertEquals(50, TABLE.size());
        assertEquals(15, count(Translator.SCALE_FACTOR), "blend factors");
        assertEquals(31, count(Translator.PRISM_TO_GL), "pixel types, formats, textures, wrap modes, limits");
        assertEquals(4, count(Translator.PIXEL_STORE), "pixel store");
    }

    @Test
    public void everyJavaTranslationEqualsTheHeaderValue() {
        for (GlEnum entry : TABLE) {
            assertEquals(entry.glValue(), entry.translated(), () -> String.format(
                    "%s(GLContext.%s = %d) must give %s = 0x%04X (%s) but gave 0x%04X", entry.translator(),
                    entry.prismName(), entry.prismIndex(), entry.glName(), entry.glValue(), entry.header(),
                    entry.translated()));
        }
    }

    @Test
    public void noTwoEntriesOfATableShareAGlValue() {
        for (Translator translator : Translator.values()) {
            Set<Integer> values = new HashSet<>();
            for (GlEnum entry : TABLE) {
                if (entry.translator() == translator) {
                    assertEquals(true, values.add(entry.glValue()), () -> translator + ": duplicate value 0x"
                            + Integer.toHexString(entry.glValue()) + " at " + entry.prismName());
                }
            }
        }
    }

    @Test
    public void theLookalikeValuesAreExact() {
        assertEquals(0x2703, ES2NativeShim.translatePrismToGL(55), "GL_LINEAR_MIPMAP_LINEAR (GL/gl.h:636);"
                + " 0x2702 is GL_NEAREST_MIPMAP_LINEAR and point-samples every mipmapped minification");
        assertEquals(0x0CF2, ES2NativeShim.translatePixelStore(61), "GL_UNPACK_ROW_LENGTH (GL/gl.h:600)");
        assertEquals(0x0CF3, ES2NativeShim.translatePixelStore(63), "GL_UNPACK_SKIP_ROWS (GL/gl.h:602)");
        assertEquals(0x0302, ES2NativeShim.translateScaleFactor(6), "GL_SRC_ALPHA (GL/gl.h:344)");
        assertEquals(0x0303, ES2NativeShim.translateScaleFactor(7), "GL_ONE_MINUS_SRC_ALPHA (GL/gl.h:345)");
    }

    /** The two enum pairs {@code ES2Native} holds for the folded {@code nSetCullingMode} / {@code nSetWireframe}. */
    @Test
    public void cullingAndWireframeEnumsMatchTheHeader() {
        assertEquals(0x0404, ES2NativeShim.glFront(), "GL_FRONT (GL/gl.h:236)");
        assertEquals(0x0405, ES2NativeShim.glBack(), "GL_BACK (GL/gl.h:237)");
        assertEquals(0x1B01, ES2NativeShim.glLine(), "GL_LINE (GL/gl.h:232)");
        assertEquals(0x1B02, ES2NativeShim.glFill(), "GL_FILL (GL/gl.h:233)");
    }

    /**
     * The library's own table, through {@code es2_gl_enum_count()} / {@code es2_gl_enum(i)}: the fifty
     * values it resolved through its platform GL headers at compile time equal the Java literals at the same
     * index, so a header macro that disagrees with the comment, or a literal typed wrong, fails here instead
     * of reaching GL; one past the end and below the start answer -1. The number of comparisons is asserted
     * so that a run which compared nothing cannot pass. Skips on a build without {@code prism_es2} (Windows
     * by default, {@link ES2Natives}); on Linux the library is always built and this runs.
     */
    @Test
    public void theCTableAgreesWithEveryJavaEntry() {
        ES2Natives.require();
        assertEquals(50, ES2NativeShim.glEnumCount(), "es2_gl_enum_count()");
        assertEquals(TABLE.size(), ES2NativeShim.glEnumCount(), "the C table and TABLE have the same length");
        int compared = 0;
        for (int i = 0; i < TABLE.size(); i++) {
            int index = i;
            GlEnum entry = TABLE.get(index);
            int fromC = ES2NativeShim.glEnum(index);
            assertEquals(entry.glValue(), fromC, () -> String.format(
                    "es2_gl_enum(%d) must be %s = 0x%04X (%s) but the library answered 0x%04X", index,
                    entry.glName(), entry.glValue(), entry.header(), fromC));
            compared++;
        }
        assertEquals(50, compared, "compared every entry of the C table");
        assertEquals(-1, ES2NativeShim.glEnum(TABLE.size()), "one past the end");
        assertEquals(-1, ES2NativeShim.glEnum(-1), "below the start");
    }

    private static long count(Translator translator) {
        return TABLE.stream().filter(entry -> entry.translator() == translator).count();
    }
}
