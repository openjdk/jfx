/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.prism;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ImageTest {

    /**
     * All supported {@link ImageType} to {@link PixelFormat} conversions.
     */
    enum SupportedConversions {
        GRAY(ImageType.GRAY, PixelFormat.BYTE_GRAY, 2,
            byteArray(
                1, 2, 3, 4
            ),
            byteArray(
                1, 2, 3, 4
            )),

        GRAY_ALPHA(ImageType.GRAY_ALPHA, PixelFormat.BYTE_BGRA_PRE, 4,
            byteArray(
                100, 127, 50, 127,
                100, 0, 50, 0
            ),
            byteArray(
                50, 50, 50, 127, 25, 25, 25, 127,
                0, 0, 0, 0, 0, 0, 0, 0
            )),

        GRAY_ALPHA_PRE(ImageType.GRAY_ALPHA_PRE, PixelFormat.BYTE_BGRA_PRE, 4,
            byteArray(
                100, 127, 50, 127,
                100, 0, 50, 0
            ),
            byteArray(
                100, 100, 100, 127, 50, 50, 50, 127,
                100, 100, 100, 0, 50, 50, 50, 0
            )),

        RGB(ImageType.RGB, PixelFormat.BYTE_RGB, 6,
            byteArray(
                1, 2, 3, 4, 5, 6,
                7, 8, 9, 10, 11, 12
            ),
            byteArray(
                1, 2, 3, 4, 5, 6,
                7, 8, 9, 10, 11, 12
            )),

        BGR(ImageType.BGR, PixelFormat.BYTE_RGB, 6,
            byteArray(
                1, 2, 3, 4, 5, 6,
                7, 8, 9, 10, 11, 12
            ),
            byteArray(
                3, 2, 1, 6, 5, 4,
                9, 8, 7, 12, 11, 10
            )),

        RGBA(ImageType.RGBA, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                100, 0, 0, 127, 0, 50, 0, 127,
                255, 127, 0, 127, 50, 60, 70, 255
            ),
            byteArray(
                0, 0, 50, 127, 0, 25, 0, 127,
                0, 63, 127, 127, 70, 60, 50, 255
            )),

        RGBA_PRE(ImageType.RGBA_PRE, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                100, 0, 0, 127, 0, 50, 0, 127,
                255, 127, 0, 127, 50, 40, 30, 0
            ),
            byteArray(
                0, 0, 100, 127, 0, 50, 0, 127,
                0, 127, 255, 127, 30, 40, 50, 0
            )),

        BGRA(ImageType.BGRA, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                100, 0, 0, 127, 0, 50, 0, 127,
                255, 127, 0, 127, 50, 50, 50, 0
            ),
            byteArray(
                50, 0, 0, 127, 0, 25, 0, 127,
                127, 63, 0, 127, 0, 0, 0, 0
            )),

        BGRA_PRE(ImageType.BGRA_PRE, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                100, 0, 0, 127, 0, 50, 0, 127,
                255, 127, 0, 127, 50, 50, 50, 0
            ),
            byteArray(
                100, 0, 0, 127, 0, 50, 0, 127,
                255, 127, 0, 127, 50, 50, 50, 0
            )),

        ABGR(ImageType.ABGR, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                127, 100, 0, 0, 127, 50, 0, 100,
                127, 255, 127, 0, 0, 50, 50, 50
            ),
            byteArray(
                50, 0, 0, 127, 25, 0, 50, 127,
                127, 63, 0, 127, 0, 0, 0, 0
            )),

        ABGR_PRE(ImageType.ABGR_PRE, PixelFormat.BYTE_BGRA_PRE, 8,
            byteArray(
                127, 100, 0, 0, 127, 50, 0, 100,
                127, 255, 127, 0, 0, 50, 50, 50
            ),
            byteArray(
                100, 0, 0, 127, 50, 0, 100, 127,
                255, 127, 0, 127, 50, 50, 50, 0
            )),

        INT_RGB(ImageType.INT_RGB, PixelFormat.INT_ARGB_PRE, 2,
            new int[] {
                rgb(50, 100, 150), rgb(10, 20, 30),
                rgb(40, 50, 60), rgb(255, 255, 255)
            },
            new int[] {
                argb(255, 50, 100, 150), argb(255, 10, 20, 30),
                argb(255, 40, 50, 60), argb(255, 255, 255, 255)
            }),

        INT_BGR(ImageType.INT_BGR, PixelFormat.INT_ARGB_PRE, 2,
            new int[] {
                rgb(50, 100, 150), rgb(10, 20, 30),
                rgb(40, 50, 60), rgb(255, 255, 255)
            },
            new int[] {
                argb(255, 150, 100, 50), argb(255, 30, 20, 10),
                argb(255, 60, 50, 40), argb(255, 255, 255, 255)
            }),

        INT_ARGB(ImageType.INT_ARGB, PixelFormat.INT_ARGB_PRE, 2,
            new int[] {
                argb(127, 50, 100, 150), argb(127, 10, 20, 30),
                argb(255, 40, 50, 60), argb(0, 255, 255, 255)
            },
            new int[] {
                argb(127, 25, 50, 75), argb(127, 5, 10, 15),
                argb(255, 40, 50, 60), argb(0, 0, 0, 0)
            }),

        INT_ARGB_PRE(ImageType.INT_ARGB_PRE, PixelFormat.INT_ARGB_PRE, 2,
            new int[] {
                argb(127, 50, 100, 150), argb(127, 10, 20, 30),
                argb(255, 0, 0, 0), argb(0, 255, 255, 255)
            },
            new int[] {
                argb(127, 50, 100, 150), argb(127, 10, 20, 30),
                argb(255, 0, 0, 0), argb(0, 255, 255, 255)
            }),

        PALETTE_ONE_BIT_OPAQUE(ImageType.PALETTE, PixelFormat.BYTE_RGB, 1, 1,
            new int[] {
                rgb(255, 0, 0), rgb(0, 127, 0)
            },
            encodeBits(
                0, 1, /*padding:*/ 0, 0, 0, 0, 0, 0,
                1, 0, /*padding:*/ 0, 0, 0, 0, 0, 0
            ),
            byteArray(
                255, 0, 0, 0, 127, 0,
                0, 127, 0, 255, 0, 0
            )),

        PALETTE_ONE_BIT_ALPHA(ImageType.PALETTE_ALPHA, PixelFormat.BYTE_BGRA_PRE, 1, 1,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0)
            },
            encodeBits(
                0, 1, /*padding:*/ 0, 0, 0, 0, 0, 0,
                1, 0, /*padding:*/ 0, 0, 0, 0, 0, 0
            ),
            byteArray(
                0, 0, 127, 127, 0, 127, 0, 255,
                0, 127, 0, 255, 0, 0, 127, 127
            )),

        PALETTE_ONE_BIT_ALPHA_PRE(ImageType.PALETTE_ALPHA_PRE, PixelFormat.BYTE_BGRA_PRE, 1, 1,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0)
            },
            encodeBits(
                0, 1, /*padding:*/ 0, 0, 0, 0, 0, 0,
                1, 0, /*padding:*/ 0, 0, 0, 0, 0, 0
            ),
            byteArray(
                0, 0, 255, 127, 0, 127, 0, 255,
                0, 127, 0, 255, 0, 0, 255, 127
            )),

        PALETTE_TWO_BIT_OPAQUE(ImageType.PALETTE, PixelFormat.BYTE_RGB, 1, 2,
            new int[] {
                rgb(255, 0, 0), rgb(0, 127, 0), rgb(10, 20, 30), rgb(50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 1, /*padding:*/ 0, 0, 0, 0,
                1, 0, 1, 1, /*padding:*/ 0, 0, 0, 0
            ),
            byteArray(
                255, 0, 0, 0, 127, 0,
                10, 20, 30, 50, 60, 70
            )),

        PALETTE_TWO_BIT_ALPHA(ImageType.PALETTE_ALPHA, PixelFormat.BYTE_BGRA_PRE, 1, 2,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 1, /*padding:*/ 0, 0, 0, 0,
                1, 0, 1, 1, /*padding:*/ 0, 0, 0, 0
            ),
            byteArray(
                0, 0, 127, 127, 0, 127, 0, 255,
                0, 0, 0, 0, 70, 60, 50, 255
            )),

        PALETTE_TWO_BIT_ALPHA_PRE(ImageType.PALETTE_ALPHA_PRE, PixelFormat.BYTE_BGRA_PRE, 1, 2,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 1, /*padding:*/ 0, 0, 0, 0,
                1, 0, 1, 1, /*padding:*/ 0, 0, 0, 0
            ),
            byteArray(
                0, 0, 255, 127, 0, 127, 0, 255,
                30, 20, 10, 0, 70, 60, 50, 255
            )),

        PALETTE_FOUR_BIT_OPAQUE(ImageType.PALETTE, PixelFormat.BYTE_RGB, 1, 4,
            new int[] {
                rgb(255, 0, 0), rgb(0, 127, 0), rgb(10, 20, 30), rgb(50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 1, 0, 0, 0, 1, 1
            ),
            byteArray(
                255, 0, 0, 0, 127, 0,
                10, 20, 30, 50, 60, 70
            )),

        PALETTE_FOUR_BIT_ALPHA(ImageType.PALETTE_ALPHA, PixelFormat.BYTE_BGRA_PRE, 1, 4,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 1, 0, 0, 0, 1, 1
            ),
            byteArray(
                0, 0, 127, 127, 0, 127, 0, 255,
                0, 0, 0, 0, 70, 60, 50, 255
            )),

        PALETTE_FOUR_BIT_ALPHA_PRE(ImageType.PALETTE_ALPHA_PRE, PixelFormat.BYTE_BGRA_PRE, 1, 4,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 1, 0, 0, 0, 1, 1
            ),
            byteArray(
                0, 0, 255, 127, 0, 127, 0, 255,
                30, 20, 10, 0, 70, 60, 50, 255
            )),

        PALETTE_EIGHT_BIT_OPAQUE(ImageType.PALETTE, PixelFormat.BYTE_RGB, 2, 8,
            new int[] {
                rgb(255, 0, 0), rgb(0, 127, 0), rgb(10, 20, 30), rgb(50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1
            ),
            byteArray(
                255, 0, 0, 0, 127, 0,
                10, 20, 30, 50, 60, 70
            )),

        PALETTE_EIGHT_BIT_ALPHA(ImageType.PALETTE_ALPHA, PixelFormat.BYTE_BGRA_PRE, 2, 8,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1
            ),
            byteArray(
                0, 0, 127, 127, 0, 127, 0, 255,
                0, 0, 0, 0, 70, 60, 50, 255
            )),

        PALETTE_EIGHT_BIT_ALPHA_PRE(ImageType.PALETTE_ALPHA_PRE, PixelFormat.BYTE_BGRA_PRE, 2, 8,
            new int[] {
                argb(127, 255, 0, 0), argb(255, 0, 127, 0), argb(0, 10, 20, 30), argb(255, 50, 60, 70)
            },
            encodeBits(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1
            ),
            byteArray(
                0, 0, 255, 127, 0, 127, 0, 255,
                30, 20, 10, 0, 70, 60, 50, 255
            ));

        static int rgb(int r, int g, int b) {
            return 255 << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
        }

        static int argb(int a, int r, int g, int b) {
            return a << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
        }

        static byte[] encodeBits(int... bits) {
            var result = new byte[bits.length / 8 + 1];
            for (int i = 0; i < bits.length; ++i) {
                result[i / 8] |= (byte)(bits[i] << (7 - (i % 8)));
            }

            return result;
        }

        // Allows us to create byte arrays with unsigned literals without casting to (byte)
        static byte[] byteArray(int... bytes) {
            var result = new byte[bytes.length];
            for (int i = 0; i < bytes.length; ++i) {
                result[i] = (byte)Objects.checkIndex(bytes[i], 256);
            }

            return result;
        }

        SupportedConversions(ImageType sourceType, PixelFormat targetType, int stride,
                             int paletteIndexBits, int[] palette,
                             byte[] sourceData, byte[] targetData) {
            this(sourceType, targetType, stride, palette, paletteIndexBits,
                 ByteBuffer.wrap(sourceData), ByteBuffer.wrap(targetData));
        }

        SupportedConversions(ImageType sourceType, PixelFormat targetType, int stride,
                             byte[] sourceData, byte[] targetData) {
            this(sourceType, targetType, stride, null, -1,
                 ByteBuffer.wrap(sourceData), ByteBuffer.wrap(targetData));
        }

        SupportedConversions(ImageType sourceType, PixelFormat targetType, int stride,
                             int[] sourceData, int[] targetData) {
            this(sourceType, targetType, stride, null, -1,
                 IntBuffer.wrap(sourceData), IntBuffer.wrap(targetData));
        }

        SupportedConversions(ImageType sourceType, PixelFormat targetType, int stride,
                             int[] palette, int paletteIndexBits,
                             Buffer sourceData, Buffer targetData) {
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.sourceData = sourceData;
            this.targetData = targetData;
            this.palette = palette;
            this.paletteIndexBits = paletteIndexBits;
            this.stride = stride;
        }

        final ImageType sourceType;
        final PixelFormat targetType;
        final Buffer sourceData;
        final Buffer targetData;
        final int paletteIndexBits;
        final int[] palette;
        final int stride;
    }

    /**
     * Asserts that the content of an {@link ImageFrame} is correctly converted to {@link Image}
     * for all supported image formats.
     */
    @ParameterizedTest
    @EnumSource(SupportedConversions.class)
    void convertImageFrame(SupportedConversions conversion) {
        var imageFrame = new ImageFrame(
            conversion.sourceType, conversion.sourceData, 2, 2, conversion.stride,
            conversion.palette, conversion.paletteIndexBits, 1,
            new ImageMetadata(null, null, null, null, null, null, null, 2, 2, null, null, null));

        var image = Image.convertImageFrame(imageFrame);

        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(conversion.targetType, image.getPixelFormat());
        assertEquals(conversion.targetData, image.getPixelBuffer(), () ->
            "Expected: %s\nActual: %s".formatted(
                formatArray(conversion.targetData.array()),
                formatArray(image.getPixelBuffer().array())));
    }

    private static String formatArray(Object array) {
        return switch (array) {
            case byte[] byteArray -> Arrays.toString(byteArray);
            case int[] intArray -> Arrays.toString(intArray);
            default -> throw new AssertionError();
        };
    }
}
