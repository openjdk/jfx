/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;

import com.sun.prism.impl.shape.MaskData;
import java.nio.ByteBuffer;
import java.util.Arrays;
import sun.misc.Unsafe;

public final class MaskMarlinAlphaConsumer implements MarlinAlphaConsumer {
    int x, y, width, height;
    final byte alphas[];
    final ByteBuffer alphabuffer;
    final MaskData maskdata = new MaskData();

    boolean useFastFill;
    int fastFillThreshold;

    public MaskMarlinAlphaConsumer(int alphalen) {
        this.alphas = new byte[alphalen];
        alphabuffer = ByteBuffer.wrap(alphas);
    }

    public void setBoundsNoClone(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        maskdata.update(alphabuffer, x, y, w, h);

        useFastFill = (w >= 32);
        if (useFastFill) {
            fastFillThreshold = (w >= 128) ? (w >> 1) : (w >> 2);
        }
    }

    @Override
    public int getOriginX() {
        return x;
    }

    @Override
    public int getOriginY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public int getAlphaLength() {
        return alphas.length;
    }

    public MaskData getMaskData() {
        return maskdata;
    }

    OffHeapArray ALPHA_MAP_USED = null;

    @Override
    public void setMaxAlpha(int maxalpha) {
        ALPHA_MAP_USED = (maxalpha == 1) ? ALPHA_MAP_UNSAFE_NO_AA : ALPHA_MAP_UNSAFE;
    }

    // The alpha map used by this object (taken out of our map cache) to convert
    // pixel coverage counts (which are in the range [0, maxalpha])
    // into alpha values, which are in [0,255]).
    static final byte[] ALPHA_MAP;
    static final OffHeapArray ALPHA_MAP_UNSAFE;

    static final byte[] ALPHA_MAP_NO_AA;
    static final OffHeapArray ALPHA_MAP_UNSAFE_NO_AA;

    static {
        final Unsafe _unsafe = OffHeapArray.UNSAFE;

        // AA:
        byte[] _ALPHA_MAP = buildAlphaMap(MarlinConst.MAX_AA_ALPHA);
        ALPHA_MAP = _ALPHA_MAP; // Keep alive the OffHeapArray
        ALPHA_MAP_UNSAFE = new OffHeapArray(ALPHA_MAP, ALPHA_MAP.length); // 1K

        long addr = ALPHA_MAP_UNSAFE.address;

        for (int i = 0; i < _ALPHA_MAP.length; i++) {
            _unsafe.putByte(addr + i, _ALPHA_MAP[i]);
        }

        // NoAA:
        byte[] _ALPHA_MAP_NO_AA = buildAlphaMap(1);
        ALPHA_MAP_NO_AA = _ALPHA_MAP_NO_AA; // Keep alive the OffHeapArray
        ALPHA_MAP_UNSAFE_NO_AA = new OffHeapArray(ALPHA_MAP_NO_AA, ALPHA_MAP_NO_AA.length);

        addr = ALPHA_MAP_UNSAFE_NO_AA.address;

        for (int i = 0; i < _ALPHA_MAP_NO_AA.length; i++) {
            _unsafe.putByte(addr + i, _ALPHA_MAP_NO_AA[i]);
        }
    }

    private static byte[] buildAlphaMap(final int maxalpha) {
        final byte[] alMap = new byte[maxalpha << 1];
        final int halfmaxalpha = maxalpha >> 2;
        for (int i = 0; i <= maxalpha; i++) {
            alMap[i] = (byte) ((i * 255 + halfmaxalpha) / maxalpha);
//            System.out.println("alphaMap[" + i + "] = "
//                               + Byte.toUnsignedInt(alMap[i]));
        }
        return alMap;
    }

    @Override
    public boolean supportBlockFlags() {
        return true;
    }

    @Override
    public void clearAlphas(final int pix_y) {
        final int w = width;
        final int off = (pix_y - y) * w;

        // Clear complete row:
       Arrays.fill(this.alphas, off, off + w, (byte)0);
    }

    @Override
    public void setAndClearRelativeAlphas(final int[] alphaDeltas, final int pix_y,
                                          final int pix_from, final int pix_to)
    {
//            System.out.println("setting row "+(pix_y - y)+
//                               " out of "+width+" x "+height);

        final byte[] out = this.alphas;
        final int w = width;
        final int off = (pix_y - y) * w;

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long addr_alpha = ALPHA_MAP_USED.address;

        final int from = pix_from - x;

        // skip useless pixels above boundary
        final int to = pix_to - x;
        final int ato = Math.min(to, width);

        // fast fill ?
        final boolean fast = useFastFill && ((ato - from) < fastFillThreshold);

        if (fast) {
            // Zero-fill complete row:
            Arrays.fill(out, off, off + w, (byte) 0);

            int i = from;
            int curAlpha = 0;

            while (i < ato) {
                curAlpha += alphaDeltas[i];

                out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]
                i++;
            }

        } else {
            int i = 0;

            while (i < from) {
                out[off + i] = 0;
                i++;
            }

            int curAlpha = 0;

            while (i < ato) {
                curAlpha += alphaDeltas[i];

                out[off + i] = _unsafe.getByte(addr_alpha + curAlpha); // [0..255]
                i++;
            }

            while (i < w) {
                out[off + i] = 0;
                i++;
            }
        }

        // Clear alpha row for reuse:
        ArrayCacheIntClean.fill(alphaDeltas, from, to + 1, 0);
    }

    @Override
    public void setAndClearRelativeAlphas(final int[] blkFlags, final int[] alphaDeltas, final int pix_y,
                                          final int pix_from, final int pix_to)
    {
//            System.out.println("setting row "+(pix_y - y)+
//                               " out of "+width+" x "+height);

        final byte[] out = this.alphas;
        final int w = width;
        final int off = (pix_y - y) * w;

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long addr_alpha = ALPHA_MAP_USED.address;

        final int from = pix_from - x;

        // skip useless pixels above boundary
        final int to = pix_to - x;
        final int ato = Math.min(to, width);

        // fast fill ?
        final boolean fast = useFastFill && ((ato - from) < fastFillThreshold);

        final int _BLK_SIZE_LG  = MarlinConst.BLOCK_SIZE_LG;

        // traverse flagged blocks:
        final int blkW = (from >> _BLK_SIZE_LG);
        final int blkE = (ato   >> _BLK_SIZE_LG) + 1;
        // ensure last block flag = 0 to process final block:
        blkFlags[blkE] = 0;

        // Perform run-length encoding and store results in the piscesCache
        int curAlpha = 0;

        final int _MAX_VALUE = Integer.MAX_VALUE;
        int last_t0 = _MAX_VALUE;
        byte val;

        if (fast) {
            int i = from;

            // Zero-fill complete row:
            Arrays.fill(out, off, off + w, (byte) 0);

            for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
                if (blkFlags[t] != 0) {
                    blkFlags[t] = 0;

                    if (last_t0 == _MAX_VALUE) {
                        last_t0 = t;
                    }
                    continue;
                }
                if (last_t0 != _MAX_VALUE) {
                    // emit blocks:
                    blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                    last_t0 = _MAX_VALUE;

                    // (last block pixel+1) inclusive => +1
                    blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, ato);

                    for (cx = blk_x0; cx < blk_x1; cx++) {
                        if ((delta = alphaDeltas[cx]) != 0) {
                            alphaDeltas[cx] = 0;

                            // fill span:
                            if (cx != i) {
                                // skip alpha = 0
                                if (curAlpha == 0) {
                                    i = cx;
                                } else {
                                    val = _unsafe.getByte(addr_alpha + curAlpha);
                                    do {
                                        out[off + i] = val;
                                        i++;
                                    } while (i < cx);
                                }
                            }

                            // alpha value = running sum of coverage delta:
                            curAlpha += delta;
                        }
                    }
                }
            }

            // Process remaining span:
            if (curAlpha != 0) {
                val = _unsafe.getByte(addr_alpha + curAlpha);
                while (i < ato) {
                    out[off + i] = val;
                    i++;
                }
            }

        } else {
            int i = 0;

            while (i < from) {
                out[off + i] = 0;
                i++;
            }

            for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
                if (blkFlags[t] != 0) {
                    blkFlags[t] = 0;

                    if (last_t0 == _MAX_VALUE) {
                        last_t0 = t;
                    }
                    continue;
                }
                if (last_t0 != _MAX_VALUE) {
                    // emit blocks:
                    blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                    last_t0 = _MAX_VALUE;

                    // (last block pixel+1) inclusive => +1
                    blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, ato);

                    for (cx = blk_x0; cx < blk_x1; cx++) {
                        if ((delta = alphaDeltas[cx]) != 0) {
                            alphaDeltas[cx] = 0;

                            // fill span:
                            if (cx != i) {
                                val = _unsafe.getByte(addr_alpha + curAlpha);
                                do {
                                    out[off + i] = val;
                                    i++;
                                } while (i < cx);
                            }

                            // alpha value = running sum of coverage delta:
                            curAlpha += delta;
                        }
                    }
                }
            }

            // Process remaining span:
            if (curAlpha != 0) {
                val = _unsafe.getByte(addr_alpha + curAlpha);
                while (i < ato) {
                    out[off + i] = val;
                    i++;
                }
            }

            while (i < w) {
                out[off + i] = 0;
                i++;
            }
        }

        // Clear alpha row for reuse:
        alphaDeltas[ato] = 0;

        if (MarlinConst.DO_CHECKS) {
            ArrayCacheIntClean.check(blkFlags, blkW, blkE, 0);
            ArrayCacheIntClean.check(alphaDeltas, from, to + 1, 0);
        }
    }
}
