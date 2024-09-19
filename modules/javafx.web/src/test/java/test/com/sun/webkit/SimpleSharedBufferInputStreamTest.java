/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.webkit;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.SharedBuffer;
import com.sun.webkit.SharedBufferShim;
import com.sun.webkit.SimpleSharedBufferInputStream;
import com.sun.webkit.WebPage;
import java.io.IOException;
import java.util.Random;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore("JDK-8290292")
public class SimpleSharedBufferInputStreamTest {

    private static final int SEGMENT_SIZE = 0x1000;
    private static final Random random = new Random();

    private final SharedBuffer sb = SharedBufferShim.createSharedBuffer();
    private final SimpleSharedBufferInputStream is =
            new SimpleSharedBufferInputStream(sb);


    @BeforeClass
    public static void beforeClass() throws ClassNotFoundException {
        if (PlatformUtil.isWindows()) {
            // Must load Microsoft libs before loading jfxwebkit.dll
            Toolkit.loadMSWindowsLibraries();
        }
        Class.forName(WebPage.class.getName());
    }


    @Test
    public void testConstructor() {
        new SimpleSharedBufferInputStream(sb);
    }

    @Test
    public void testConstructorNullSharedBuffer() {
        try {
            new SimpleSharedBufferInputStream(null);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testRead1FirstSegmentFirstByte() {
        append(SEGMENT_SIZE * 2.5);
        assertEquals(0, is.read());
    }

    @Test
    public void testRead1FirstSegmentInteriorByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(2);
        assertEquals(2, is.read());
    }

    @Test
    public void testRead1FirstSegmentLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE - 1);
        assertEquals((SEGMENT_SIZE - 1) & 0xff, is.read());
        assertEquals(SEGMENT_SIZE & 0xff, is.read());
    }

    @Test
    public void testRead1InteriorSegmentFirstByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE);
        assertEquals(SEGMENT_SIZE & 0xff, is.read());
    }

    @Test
    public void testRead1InteriorSegmentInteriorByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE + 2);
        assertEquals((SEGMENT_SIZE + 2) & 0xff, is.read());
    }

    @Test
    public void testRead1InteriorSegmentLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 - 1);
        assertEquals((SEGMENT_SIZE * 2 - 1) & 0xff, is.read());
        assertEquals((SEGMENT_SIZE * 2) & 0xff, is.read());
    }

    @Test
    public void testRead1LastSegmentFirstByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2);
        assertEquals((SEGMENT_SIZE * 2) & 0xff, is.read());
    }

    @Test
    public void testRead1LastSegmentInteriorByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 + 2);
        assertEquals((SEGMENT_SIZE * 2 + 2) & 0xff, is.read());
    }

    @Test
    public void testRead1LastSegmentLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5 - 1);
        assertEquals((int) (SEGMENT_SIZE * 2.5 - 1) & 0xff, is.read());
    }

    @Test
    public void testRead1ByteAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5);
        assertEquals(-1, is.read());
    }

    @Test
    public void testRead1ByteFromEmptyBuffer() {
        assertEquals(-1, is.read());
    }

    @Test
    public void testRead3FirstSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(0, 10), read(10));
    }

    @Test
    public void testRead3FirstSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(7);
        assertArrayEquals(g(7, 10), read(10));
    }

    @Test
    public void testRead3FirstSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE - 10);
        assertArrayEquals(g(SEGMENT_SIZE - 10, 10), read(10));
    }

    @Test
    public void testRead3InteriorSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE);
        assertArrayEquals(g(SEGMENT_SIZE, 10), read(10));
    }

    @Test
    public void testRead3InteriorSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE + 7);
        assertArrayEquals(g(SEGMENT_SIZE + 7, 10), read(10));
    }

    @Test
    public void testRead3InteriorSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 - 10);
        assertArrayEquals(g(SEGMENT_SIZE * 2 - 10, 10), read(10));
    }

    @Test
    public void testRead3LastSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2);
        assertArrayEquals(g(SEGMENT_SIZE * 2, 10), read(10));
    }

    @Test
    public void testRead3LastSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 + 7);
        assertArrayEquals(g(SEGMENT_SIZE * 2 + 7, 10), read(10));
    }

    @Test
    public void testRead3LastSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5 - 10);
        assertArrayEquals(g(SEGMENT_SIZE * 2.5 - 10, 10), read(10));
    }

    @Test
    public void testRead3LastSegmentLastTenBytesWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5 - 5);
        assertArrayEquals(g(SEGMENT_SIZE * 2.5 - 5, 5), read(10));
    }

    @Test
    public void testRead3TenBytesAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5);
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testRead3TenBytesFromEmptyBuffer() {
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testRead3FirstSegment() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(0, SEGMENT_SIZE), read(SEGMENT_SIZE));
    }

    @Test
    public void testRead3InteriorSegment() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE);
        assertArrayEquals(g(SEGMENT_SIZE, SEGMENT_SIZE), read(SEGMENT_SIZE));
    }

    @Test
    public void testRead3LastSegment() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE * 0.5),
                read(SEGMENT_SIZE));
    }

    @Test
    public void testRead3FirstSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3FirstSegmentInteriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 0.5);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3InteriorSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3InteriorSegmentInterriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 1.5);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3LastSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3LastSegmentInteriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 + 7);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3ZeroBytesAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3ZeroBytesFromEmptyBuffer() {
        assertArrayEquals(new byte[0], read(0));
    }

    @Test
    public void testRead3NullBuffer() {
        try {
            is.read(null, 0, 1);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testRead3NegativeOffset() {
        try {
            is.read(new byte[0], -1, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testRead3NegativeLength() {
        try {
            is.read(new byte[0], 0, -1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testRead3IllegalBufferOrOffsetOrLength() {
        try {
            is.read(new byte[0], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            is.read(new byte[0], 1, 0);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            is.read(new byte[10], 0, 11);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            is.read(new byte[10], 1, 10);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    private void testSkipSmallNumberOfBytes(long skip) {
        int streamSize = (int) (SEGMENT_SIZE * 2.5);
        int skipCount = streamSize / SEGMENT_SIZE + 1;
        append(streamSize);
        int position = 0;
        for (int i = 0; i < skipCount; i++) {
            long skipped = is.skip(skip);
            assertEquals(Math.max(skip, 0), skipped);
            position += skipped;
            long len = Math.min(SEGMENT_SIZE - skipped, streamSize - position);
            assertArrayEquals(g(position, len), read(SEGMENT_SIZE));
            position += len;
        }
    }

    @Test
    public void testSkipZeroBytes() {
        testSkipSmallNumberOfBytes(0);
    }

    @Test
    public void testSkipMinusOneByte() {
        testSkipSmallNumberOfBytes(-1);
    }

    @Test
    public void testSkipMinusTenBytes() {
        testSkipSmallNumberOfBytes(-10);
    }

    @Test
    public void testSkipIntegerMinValueBytes() {
        testSkipSmallNumberOfBytes(Integer.MIN_VALUE);
    }

    @Test
    public void testSkipSegment() {
        append(SEGMENT_SIZE * 2.5);
        long skipped = is.skip(SEGMENT_SIZE);
        assertEquals(SEGMENT_SIZE, skipped);
        assertArrayEquals(g(SEGMENT_SIZE, SEGMENT_SIZE), read(SEGMENT_SIZE));
        skipped = is.skip(SEGMENT_SIZE);
        assertEquals((long) (SEGMENT_SIZE * 0.5), skipped);
        assertArrayEquals(null, read(SEGMENT_SIZE));
    }

    @Test
    public void testSkipTwoSegments() {
        append(SEGMENT_SIZE * 2.5);
        long skipped = is.skip(SEGMENT_SIZE * 2);
        assertEquals(SEGMENT_SIZE * 2, skipped);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE * 0.5),
                read(SEGMENT_SIZE));
    }

    @Test
    public void testSkipAll() {
        append(SEGMENT_SIZE * 2.5);
        long skipped = is.skip(SEGMENT_SIZE * 3);
        assertEquals((long) (SEGMENT_SIZE * 2.5), skipped);
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testSkipIntegerMaxValueBytes() {
        append(SEGMENT_SIZE * 2.5);
        long skipped = is.skip(Integer.MAX_VALUE);
        assertEquals((long) (SEGMENT_SIZE * 2.5), skipped);
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testSkipLessThanAvailable() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2 + 10);
        long skipped = is.skip(SEGMENT_SIZE);
        assertEquals((long) (SEGMENT_SIZE * 0.5 - 10), skipped);
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testSkipAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5);
        assertEquals(0, is.skip(10));
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testSkipEmptyBuffer() {
        assertEquals(0, is.skip(10));
        assertArrayEquals(null, read(10));
    }

    @Test
    public void testAvailableVariousPositions() {
        int streamSize = (int) (SEGMENT_SIZE * 2.5);
        append(streamSize);

        assertEquals(streamSize, is.available());

        readOut(1);
        streamSize -= 1;
        assertEquals(streamSize, is.available());

        readOut(2);
        streamSize -= 2;
        assertEquals(streamSize, is.available());

        readOut(10);
        streamSize -= 10;
        assertEquals(streamSize, is.available());

        readOut(SEGMENT_SIZE);
        streamSize -= SEGMENT_SIZE;
        assertEquals(streamSize, is.available());

        readOut(SEGMENT_SIZE);
        streamSize -= SEGMENT_SIZE;
        assertEquals(streamSize, is.available());

        read(SEGMENT_SIZE);
        assertEquals(0, is.available());
    }

    @Test
    public void testAvailableRandomPositions() {
        int streamSize = (int) (SEGMENT_SIZE * 2.5);
        append(streamSize);
        while (streamSize > 0) {
            int bytesToRead = Math.min(random.nextInt(100), streamSize);
            readOut(bytesToRead);
            streamSize -= bytesToRead;
            assertEquals(streamSize, is.available());
        }
        assertEquals(0, is.available());
    }

    @Test
    public void testAvailableAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        readOut(SEGMENT_SIZE * 2.5);
        assertEquals(0, is.available());
    }

    @Test
    public void testAvailableEmptyBuffer() {
        assertEquals(0, is.available());
    }

    @Test
    public void testCloseBeforeFirstRead() throws IOException {
        append(SEGMENT_SIZE * 2.5);
        is.close();
        assertArrayEquals(g(0, SEGMENT_SIZE), read(SEGMENT_SIZE));
    }

    @Test
    public void testCloseEmptyBuffer() throws IOException {
        is.close();
        is.close();
        assertArrayEquals(null, read(SEGMENT_SIZE));
    }

    @Test
    public void testMarkVariousArguments() {
        append(SEGMENT_SIZE * 2.5);
        int[] args = new int[] {-1000, -100, -1, 0, 1, 10, 100, 1000};
        for (int arg : args) {
            is.mark(arg);
        }
        readOut(1000);
        for (int arg : args) {
            is.mark(arg);
        }
    }

    @Test
    public void testMarkRandomArguments() {
        append(SEGMENT_SIZE * 2.5);
        for (int i = 0; i < 100; i++) {
            is.mark(random.nextInt());
        }
        readOut(1000);
        for (int i = 0; i < 100; i++) {
            is.mark(random.nextInt());
        }
    }

    @Test
    public void testReset() {
        try {
            is.reset();
            fail("IOException expected but not thrown");
        } catch (IOException expected) {}
    }

    @Test
    public void testMarkSupported() {
        assertFalse(is.markSupported());
    }


    @After
    public void after() {
        SharedBufferShim.dispose(sb);
    }

    private void append(double length) {
        byte[] data = g(0, (int) length);
        SharedBufferShim.append(sb, data, 0, data.length);
    }

    private void readOut(double length) {
        int intLength = (int) length;
        byte[] buffer = new byte[intLength];
        while (intLength > 0) {
            int len = is.read(buffer, 0, intLength);
            if (len == -1) {
                fail("Unexpected end of stream");
            }
            intLength -= len;
        }
    }

    private static byte[] g(double start, double count) {
        int intCount = (int) count;
        byte[] result = new byte[intCount];
        for (int i = 0; i < intCount; i++) {
            result[i] = (byte) ((i + (int) start) & 0xff);
        }
        return result;
    }

    private byte[] read(int length) {
        int offset = random.nextBoolean() ? random.nextInt(100) : 0;
        int extraLength = random.nextBoolean() ? random.nextInt(200) : 0;
        byte[] buffer = g(0, offset + length + extraLength);
        int len = is.read(buffer, offset, length);
        if (length == 0) {
            assertEquals("Unexpected len", 0, len);
        }
        if (len == -1) {
            for (int i = 0; i < buffer.length; i++) {
                assertEquals((byte) (i & 0xff), buffer[i]);
            }
            return null;
        }
        assertTrue("Unexpected len: " + len, len >= 0);
        for (int i = 0; i < offset; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
        for (int i = offset + len; i < buffer.length; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
        byte[] result = new byte[len];
        System.arraycopy(buffer, offset, result, 0, len);
        return result;
    }
}
