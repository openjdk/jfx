package com.sun.glass.ui.headless;

import com.sun.glass.ui.Pixels;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class HeadlessPixels extends Pixels {

    HeadlessPixels(int width, int height, ByteBuffer data) {
        super(width, height, data);
    }

    HeadlessPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    HeadlessPixels(int width, int height, IntBuffer data) {
        super(width, height, data);
    }

    HeadlessPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    @Override
    protected void _fillDirectByteBuffer(ByteBuffer bb) {
        bb.put(this.bytes);
    }

    @Override
    protected void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
