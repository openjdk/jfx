/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.image;

import com.sun.javafx.image.impl.ByteBgr;
import com.sun.javafx.image.impl.ByteBgra;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteIndexed;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.General;
import com.sun.javafx.image.impl.IntArgb;
import com.sun.javafx.image.impl.IntArgbPre;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public class PixelUtils {
    private PixelUtils() {}

    public static int RgbToGray(int r, int g, int b) {
        return (int) (r * .3 + g * .59 + b * .11);
    }

    public static int RgbToGray(int xrgb) {
        return RgbToGray((xrgb >> 16) & 0xff,
                         (xrgb >>  8) & 0xff,
                         (xrgb      ) & 0xff);
    }

    public static int NonPretoPre(int nonpre, int alpha) {
        if (alpha == 0xff) return nonpre;
        if (alpha == 0x00) return 0;
        return (nonpre * alpha + 0x7f) / 0xff;
    }

    public static int PreToNonPre(int pre, int alpha) {
        if (alpha == 0xff || alpha == 0x00) return pre;
        return (pre >= alpha) ? 0xff : (pre * 0xff + (alpha >> 1)) / alpha;
    }

    public static int NonPretoPre(int nonpre) {
        int a = nonpre >>> 24;
        if (a == 0xff) return nonpre;
        if (a == 0x00) return 0;
        int r = (nonpre >> 16) & 0xff;
        int g = (nonpre >>  8) & 0xff;
        int b = (nonpre      ) & 0xff;
        r = (r * a + 0x7f) / 0xff;
        g = (g * a + 0x7f) / 0xff;
        b = (b * a + 0x7f) / 0xff;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int PretoNonPre(int pre) {
        int a = pre >>> 24;
        if (a == 0xff || a == 0x00) return pre;
        int r = (pre >> 16) & 0xff;
        int g = (pre >>  8) & 0xff;
        int b = (pre      ) & 0xff;
        int halfa = a >> 1;
        r = (r >= a) ? 0xff : (r * 0xff + halfa) / a;
        g = (g >= a) ? 0xff : (g * 0xff + halfa) / a;
        b = (b >= a) ? 0xff : (b * 0xff + halfa) / a;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static BytePixelGetter getByteGetter(PixelFormat<ByteBuffer> pf) {
        switch (pf.getType()) {
            case BYTE_BGRA:
                return ByteBgra.getter;
            case BYTE_BGRA_PRE:
                return ByteBgraPre.getter;
            case BYTE_RGB:
                return ByteRgb.getter;
            case BYTE_INDEXED:
                return ByteIndexed.createGetter(pf);
            case INT_ARGB:
            case INT_ARGB_PRE:
                // Impossible - not byte format
        }
        return null;
    }

    public static IntPixelGetter getIntGetter(PixelFormat<IntBuffer> pf) {
        switch (pf.getType()) {
            case INT_ARGB:
                return IntArgb.getter;
            case INT_ARGB_PRE:
                return IntArgbPre.getter;
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
            case BYTE_RGB:
            case BYTE_INDEXED:
                // Impossible - not int format
        }
        return null;
    }

    public static <T extends Buffer> PixelGetter<T> getGetter(PixelFormat<T> pf) {
        switch (pf.getType()) {
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
            case BYTE_RGB:
            case BYTE_INDEXED:
                return (PixelGetter<T>) getByteGetter((PixelFormat<ByteBuffer>) pf);
            case INT_ARGB:
            case INT_ARGB_PRE:
                return (PixelGetter<T>) getIntGetter((PixelFormat<IntBuffer>) pf);
        }
        return null;
    }

    public static BytePixelSetter getByteSetter(WritablePixelFormat<ByteBuffer> pf) {
        switch (pf.getType()) {
            case BYTE_BGRA:
                return ByteBgra.setter;
            case BYTE_BGRA_PRE:
                return ByteBgraPre.setter;
            case BYTE_RGB:
            case BYTE_INDEXED:
                // Impossible - not writable
            case INT_ARGB:
            case INT_ARGB_PRE:
                // Impossible - not byte format
        }
        return null;
    }

    public static IntPixelSetter getIntSetter(WritablePixelFormat<IntBuffer> pf) {
        switch (pf.getType()) {
            case INT_ARGB:
                return IntArgb.setter;
            case INT_ARGB_PRE:
                return IntArgbPre.setter;
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
            case BYTE_RGB:
            case BYTE_INDEXED:
                // Impossible - not int format
        }
        return null;
    }

    public static <T extends Buffer> PixelSetter<T> getSetter(WritablePixelFormat<T> pf) {
        switch (pf.getType()) {
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
                return (PixelSetter<T>) getByteSetter((WritablePixelFormat<ByteBuffer>) pf);
            case INT_ARGB:
            case INT_ARGB_PRE:
                return (PixelSetter<T>) getIntSetter((WritablePixelFormat<IntBuffer>) pf);
            case BYTE_RGB:
            case BYTE_INDEXED:
                // Impossible - not writable
        }
        return null;
    }

    public static <T extends Buffer, U extends Buffer>
        PixelConverter<T, U> getConverter(PixelGetter<T> src, PixelSetter<U> dst)
    {
        if (src instanceof BytePixelGetter) {
            if (dst instanceof BytePixelSetter) {
                return (PixelConverter<T, U>)
                    getB2BConverter((BytePixelGetter) src, (BytePixelSetter) dst);
            } else {
                return (PixelConverter<T, U>)
                    getB2IConverter((BytePixelGetter) src, (IntPixelSetter) dst);
            }
        } else {
            if (dst instanceof BytePixelSetter) {
                return (PixelConverter<T, U>)
                    getI2BConverter((IntPixelGetter) src, (BytePixelSetter) dst);
            } else {
                return (PixelConverter<T, U>)
                    getI2IConverter((IntPixelGetter) src, (IntPixelSetter) dst);
            }
        }
    }

    public static ByteToBytePixelConverter
        getB2BConverter(PixelGetter<ByteBuffer> src, PixelSetter<ByteBuffer> dst)
    {
        if (src ==        ByteBgra.getter) {
            if (dst ==               ByteBgra.setter) {
                return    ByteBgra.ToByteBgraConverter();
            } else if (dst ==        ByteBgraPre.setter) {
                return    ByteBgra.ToByteBgraPreConverter();
            }
        } else if (src == ByteBgraPre.getter) {
            if (dst ==                  ByteBgra.setter) {
                return    ByteBgraPre.ToByteBgraConverter();
            } else if (dst ==           ByteBgraPre.setter) {
                return    ByteBgraPre.ToByteBgraPreConverter();
            }
        } else if (src == ByteRgb.getter) {
            if (dst ==              ByteBgra.setter) {
                return    ByteRgb.ToByteBgraConverter();
            } else if (dst ==       ByteBgraPre.setter) {
                return    ByteRgb.ToByteBgraPreConverter();
            } else if (dst ==       ByteBgr.setter) {
                return    ByteRgb.ToByteBgrConverter();
            }
        } else if (src == ByteBgr.getter) {
            if (dst ==              ByteBgr.setter) {
                return    ByteBgr.ToByteBgrConverter();
            } else if (dst ==       ByteBgra.setter) {
                return    ByteBgr.ToByteBgraConverter();
            } else if (dst ==       ByteBgraPre.setter) {
                return    ByteBgr.ToByteBgraPreConverter();
            }
        } else if (src == ByteGray.getter) {
            if (dst ==               ByteGray.setter) {
                return    ByteGray.ToByteGrayConverter();
            } else if (dst ==        ByteBgr.setter) {
                return    ByteGray.ToByteBgrConverter();
            } else if (dst ==        ByteBgra.setter) {
                return    ByteGray.ToByteBgraConverter();
            } else if (dst ==        ByteBgraPre.setter) {
                return    ByteGray.ToByteBgraPreConverter();
            }
        } else if (src instanceof ByteIndexed.Getter) {
            if (dst == ByteBgra.setter || dst == ByteBgraPre.setter) {
                return ByteIndexed.createToByteBgraAny((BytePixelGetter) src,
                                                       (BytePixelSetter) dst);
            }
        }
        if (dst == ByteGray.setter) {
            return null;
        }
        if (src.getAlphaType() != AlphaType.OPAQUE &&
            dst.getAlphaType() == AlphaType.OPAQUE)
        {
            return null;
        }
        return General.create((BytePixelGetter) src, (BytePixelSetter) dst);
    }

    public static ByteToIntPixelConverter
        getB2IConverter(PixelGetter<ByteBuffer> src, PixelSetter<IntBuffer> dst)
    {
        if (src ==        ByteBgra.getter) {
            if (dst ==               IntArgb.setter) {
                return    ByteBgra.ToIntArgbConverter();
            } else if (dst ==        IntArgbPre.setter) {
                return    ByteBgra.ToIntArgbPreConverter();
            }
        } else if (src == ByteBgraPre.getter) {
            if (dst ==                  IntArgb.setter) {
                return    ByteBgraPre.ToIntArgbConverter();
            } else if (dst ==           IntArgbPre.setter) {
                return    ByteBgraPre.ToIntArgbPreConverter();
            }
        } else if (src == ByteRgb.getter) {
            if (dst ==              IntArgb.setter) {
                return    ByteRgb.ToIntArgbConverter();
            } else if (dst ==       IntArgbPre.setter) {
                return    ByteRgb.ToIntArgbPreConverter();
            }
        } else if (src == ByteBgr.getter) {
            if (dst ==              IntArgb.setter) {
                return    ByteBgr.ToIntArgbConverter();
            } else if (dst ==       IntArgbPre.setter) {
                return    ByteBgr.ToIntArgbPreConverter();
            }
        } else if (src == ByteGray.getter) {
            if (dst ==              IntArgbPre.setter) {
                return    ByteGray.ToIntArgbPreConverter();
            } else if (dst ==       IntArgb.setter) {
                return    ByteGray.ToIntArgbConverter();
            }
        } else if (src instanceof ByteIndexed.Getter) {
            if (dst == IntArgb.setter || dst == IntArgbPre.setter) {
                return ByteIndexed.createToIntArgbAny((BytePixelGetter) src,
                                                      (IntPixelSetter)  dst);
            }
        }
        if (src.getAlphaType() != AlphaType.OPAQUE &&
            dst.getAlphaType() == AlphaType.OPAQUE)
        {
            return null;
        }
        return General.create((BytePixelGetter) src, (IntPixelSetter) dst);
    }

    public static IntToBytePixelConverter
        getI2BConverter(PixelGetter<IntBuffer> src, PixelSetter<ByteBuffer> dst)
    {
        if (src ==        IntArgb.getter) {
            if (dst ==              ByteBgra.setter) {
                return    IntArgb.ToByteBgraConverter();
            } else if (dst ==       ByteBgraPre.setter) {
                return    IntArgb.ToByteBgraPreConverter();
            }
        } else if (src == IntArgbPre.getter) {
            if (dst ==                 ByteBgra.setter) {
                return    IntArgbPre.ToByteBgraConverter();
            } else if (dst ==          ByteBgraPre.setter) {
                return    IntArgbPre.ToByteBgraPreConverter();
            }
        }
        if (dst == ByteGray.setter) {
            return null;
        }
        if (src.getAlphaType() != AlphaType.OPAQUE &&
            dst.getAlphaType() == AlphaType.OPAQUE)
        {
            return null;
        }
        return General.create((IntPixelGetter) src, (BytePixelSetter) dst);
    }

    public static IntToIntPixelConverter
        getI2IConverter(PixelGetter<IntBuffer> src, PixelSetter<IntBuffer> dst)
    {
        if (src ==        IntArgb.getter) {
            if (dst ==              IntArgb.setter) {
                return    IntArgb.ToIntArgbConverter();
            } else if (dst ==       IntArgbPre.setter) {
                return    IntArgb.ToIntArgbPreConverter();
            }
        } else if (src == IntArgbPre.getter) {
            if (dst ==                 IntArgb.setter) {
                return    IntArgbPre.ToIntArgbConverter();
            } else if (dst ==          IntArgbPre.setter) {
                return    IntArgbPre.ToIntArgbPreConverter();
            }
        }
        if (src.getAlphaType() != AlphaType.OPAQUE &&
            dst.getAlphaType() == AlphaType.OPAQUE)
        {
            return null;
        }
        return General.create((IntPixelGetter) src, (IntPixelSetter) dst);
    }
}
