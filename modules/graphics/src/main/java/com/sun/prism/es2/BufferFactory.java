/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import java.nio.*;

class BufferFactory {
  static final int SIZEOF_BYTE = 1;
  static final int SIZEOF_SHORT = 2;
  static final int SIZEOF_CHAR = 2;
  static final int SIZEOF_INT = 4;
  static final int SIZEOF_FLOAT = 4;
  static final int SIZEOF_LONG = 8;
  static final int SIZEOF_DOUBLE = 8;

  private static final boolean isLittleEndian;

  static {
    ByteBuffer tst_b = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_INT); // 32bit in native order
    IntBuffer tst_i = tst_b.asIntBuffer();
    ShortBuffer tst_s = tst_b.asShortBuffer();
    tst_i.put(0, 0x0A0B0C0D);
    isLittleEndian = 0x0C0D == tst_s.get(0) ;
  }

  static boolean isLittleEndian() {
    return isLittleEndian;
  }

  /** Helper routine to create a direct ByteBuffer with native order */
  static ByteBuffer newDirectByteBuffer(int size) {
    return nativeOrder(ByteBuffer.allocateDirect(size));
  }

  /** Helper routine to set a ByteBuffer to the native byte order, if
      that operation is supported by the underlying NIO
      implementation. */
  static ByteBuffer nativeOrder(ByteBuffer buf) {
    return buf.order(ByteOrder.nativeOrder());
  }

  /** Helper routine to tell whether a buffer is direct or not. Null
      pointers are considered direct. isDirect() should really be
      in Buffer and not replicated in all subclasses. */
  static boolean isDirect(Object buf) {
    if (buf == null) {
      return true;
    }
    if (buf instanceof ByteBuffer) {
      return ((ByteBuffer) buf).isDirect();
    } else if (buf instanceof FloatBuffer) {
      return ((FloatBuffer) buf).isDirect();
    } else if (buf instanceof DoubleBuffer) {
      return ((DoubleBuffer) buf).isDirect();
    } else if (buf instanceof CharBuffer) {
      return ((CharBuffer) buf).isDirect();
    } else if (buf instanceof ShortBuffer) {
      return ((ShortBuffer) buf).isDirect();
    } else if (buf instanceof IntBuffer) {
      return ((IntBuffer) buf).isDirect();
    } else if (buf instanceof LongBuffer) {
      return ((LongBuffer) buf).isDirect();
    }
    throw new RuntimeException("Unexpected buffer type " + buf.getClass().getName());
  }


  /** Helper routine to get the Buffer byte offset by taking into
      account the Buffer position and the underlying type.  This is
      the total offset for Direct Buffers.  */
  static int getDirectBufferByteOffset(Object buf) {
    if(buf == null) {
      return 0;
    }
    if(buf instanceof Buffer) {
        int pos = ((Buffer)buf).position();
        if(buf instanceof ByteBuffer) {
          return pos;
        } else if (buf instanceof FloatBuffer) {
          return pos * SIZEOF_FLOAT;
        } else if (buf instanceof IntBuffer) {
          return pos * SIZEOF_INT;
        } else if (buf instanceof ShortBuffer) {
          return pos * SIZEOF_SHORT;
        } else if (buf instanceof DoubleBuffer) {
          return pos * SIZEOF_DOUBLE;
        } else if (buf instanceof LongBuffer) {
          return pos * SIZEOF_LONG;
        } else if (buf instanceof CharBuffer) {
          return pos * SIZEOF_CHAR;
        }
    }

    throw new RuntimeException("Disallowed array backing store type in buffer "
                               + buf.getClass().getName());
  }


  /** Helper routine to return the array backing store reference from
      a Buffer object.  */
   static Object getArray(Object buf) {
     if (buf == null) {
       return null;
     }
     if(buf instanceof ByteBuffer) {
       return ((ByteBuffer) buf).array();
     } else if (buf instanceof FloatBuffer) {
       return ((FloatBuffer) buf).array();
     } else if (buf instanceof IntBuffer) {
       return ((IntBuffer) buf).array();
     } else if (buf instanceof ShortBuffer) {
       return ((ShortBuffer) buf).array();
     } else if (buf instanceof DoubleBuffer) {
       return ((DoubleBuffer) buf).array();
     } else if (buf instanceof LongBuffer) {
       return ((LongBuffer) buf).array();
     } else if (buf instanceof CharBuffer) {
       return ((CharBuffer) buf).array();
     }
     throw new RuntimeException("Disallowed array backing store type in buffer "
                                + buf.getClass().getName());
   }


  /** Helper routine to get the full byte offset from the beginning of
      the array that is the storage for the indirect Buffer
      object.  The array offset also includes the position offset
      within the buffer, in addition to any array offset. */
  static int getIndirectBufferByteOffset(Object buf) {
    if(buf == null) {
      return 0;
    }
    if (buf instanceof Buffer) {
        int pos = ((Buffer)buf).position();
        if(buf instanceof ByteBuffer) {
          return (((ByteBuffer)buf).arrayOffset() + pos);
        } else if(buf instanceof FloatBuffer) {
          return (SIZEOF_FLOAT*(((FloatBuffer)buf).arrayOffset() + pos));
        } else if(buf instanceof IntBuffer) {
          return (SIZEOF_INT*(((IntBuffer)buf).arrayOffset() + pos));
        } else if(buf instanceof ShortBuffer) {
          return (SIZEOF_SHORT*(((ShortBuffer)buf).arrayOffset() + pos));
        } else if(buf instanceof DoubleBuffer) {
          return (SIZEOF_DOUBLE*(((DoubleBuffer)buf).arrayOffset() + pos));
        } else if(buf instanceof LongBuffer) {
          return (SIZEOF_LONG*(((LongBuffer)buf).arrayOffset() + pos));
        } else if(buf instanceof CharBuffer) {
          return (SIZEOF_CHAR*(((CharBuffer)buf).arrayOffset() + pos));
        }
    }
    throw new RuntimeException("Unknown buffer type " + buf.getClass().getName());
  }
}
