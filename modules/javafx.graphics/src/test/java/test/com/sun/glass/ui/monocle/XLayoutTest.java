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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.XShim;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.StructLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Xlib layouts of X against the sizes and offsets measured on the LP64 libX11 1.8.13 of Xlib.h:
 * {@code XSetWindowAttributes} 112 (event_mask 72, override_redirect 88, cursor 104), {@code XEvent} 192 with
 * its members xany.window 32, xbutton.button 84, xmotion.x 64 / y 68, xclient.message_type 40 / format 48 /
 * data.l 56, {@code XColor} 16 (pixel 0, red 8, green 10, blue 12, flags 14), and the pinned private
 * {@code sizeof(struct _XDisplay)} 4720; the accessors over a Structure's own memory; the LP64 guard. Runs on
 * every platform: nothing here reaches libX11.
 */
public class XLayoutTest {

    private static long offset(StructLayout layout, String... path) {
        PathElement[] elements = new PathElement[path.length];
        for (int i = 0; i < path.length; i++) {
            elements[i] = PathElement.groupElement(path[i]);
        }
        return layout.byteOffset(elements);
    }

    @Test
    public void xSetWindowAttributesIs112Bytes() {
        StructLayout layout = XShim.xSetWindowAttributesLayout();
        assertEquals(112, layout.byteSize());
        assertEquals(0, offset(layout, "background_pixmap"));
        assertEquals(32, offset(layout, "bit_gravity"));
        assertEquals(40, offset(layout, "backing_store"));
        assertEquals(48, offset(layout, "backing_planes"));
        assertEquals(64, offset(layout, "save_under"));
        assertEquals(72, offset(layout, "event_mask"));
        assertEquals(80, offset(layout, "do_not_propagate_mask"));
        assertEquals(88, offset(layout, "override_redirect"));
        assertEquals(96, offset(layout, "colormap"));
        assertEquals(104, offset(layout, "cursor"));
    }

    @Test
    public void xEventIs24LongsAndItsMembersAreViewsOfIt() {
        assertEquals(192, XShim.xEventLayout().byteSize());
        StructLayout any = XShim.xAnyEventLayout();
        assertEquals(40, any.byteSize());
        assertEquals(0, offset(any, "type"));
        assertEquals(8, offset(any, "serial"));
        assertEquals(16, offset(any, "send_event"));
        assertEquals(24, offset(any, "display"));
        assertEquals(32, offset(any, "window"));
        StructLayout button = XShim.xButtonEventLayout();
        assertEquals(96, button.byteSize());
        assertEquals(32, offset(button, "window"));
        assertEquals(40, offset(button, "root"));
        assertEquals(48, offset(button, "subwindow"));
        assertEquals(56, offset(button, "time"));
        assertEquals(64, offset(button, "x"));
        assertEquals(68, offset(button, "y"));
        assertEquals(72, offset(button, "x_root"));
        assertEquals(76, offset(button, "y_root"));
        assertEquals(80, offset(button, "state"));
        assertEquals(84, offset(button, "button"));
        assertEquals(88, offset(button, "same_screen"));
        StructLayout motion = XShim.xMotionEventLayout();
        assertEquals(96, motion.byteSize());
        assertEquals(64, offset(motion, "x"));
        assertEquals(68, offset(motion, "y"));
        assertEquals(84, offset(motion, "is_hint"));
        assertEquals(88, offset(motion, "same_screen"));
        StructLayout client = XShim.xClientMessageEventLayout();
        assertEquals(96, client.byteSize());
        assertEquals(32, offset(client, "window"));
        assertEquals(40, offset(client, "message_type"));
        assertEquals(48, offset(client, "format"));
        assertEquals(56, offset(client, "data"));
        assertEquals(40, client.select(PathElement.groupElement("data")).byteSize(), "long data.l[5]");
        assertTrue(any.byteSize() <= 192 && button.byteSize() <= 192 && client.byteSize() <= 192);
    }

    @Test
    public void xColorIs16Bytes() {
        StructLayout layout = XShim.xColorLayout();
        assertEquals(16, layout.byteSize());
        assertEquals(0, offset(layout, "pixel"));
        assertEquals(8, offset(layout, "red"));
        assertEquals(10, offset(layout, "green"));
        assertEquals(12, offset(layout, "blue"));
        assertEquals(14, offset(layout, "flags"));
    }

    /**
     * {@code sizeof(struct _XDisplay)} of the private Xlibint.h of libX11 1.8.13 on LP64, the value the JNI
     * answered; no public API yields it, and only the monocle.maliSignedStruct workaround reads it.
     */
    @Test
    public void xDisplaySizeofIsThePinnedPrivateStructSize() {
        assertEquals(4720, XShim.XDISPLAY_PRIVATE_BYTES);
        assertEquals(4720, XShim.xDisplaySizeof());
    }

    @Test
    public void structuresAreAllocatedAtLayoutSize() {
        assertEquals(112, new XShim.XSetWindowAttributesShim().sizeof());
        assertEquals(192, new XShim.XEventShim().sizeof());
        assertEquals(16, new XShim.XColorShim().sizeof());
        assertEquals(192, new XShim.XEventShim().buffer().capacity());
    }

    @Test
    public void windowAttributeSettersWriteTheirFields() {
        var attrs = new XShim.XSetWindowAttributesShim();
        attrs.setEventMask(XShim.ButtonPressMask | XShim.ButtonReleaseMask | XShim.PointerMotionMask);
        attrs.setOverrideRedirect(true);
        attrs.setCursor(0x1234_5678_9abcL);
        ByteBuffer raw = attrs.buffer().order(ByteOrder.nativeOrder());
        assertEquals((1L << 2) | (1L << 3) | (1L << 6), raw.getLong(72), "event_mask");
        assertEquals(1, raw.getInt(88), "override_redirect True");
        assertEquals(0x1234_5678_9abcL, raw.getLong(104), "cursor");
        attrs.setOverrideRedirect(false);
        assertEquals(0, raw.getInt(88), "override_redirect False");
    }

    @Test
    public void eventAccessorsReadTheAnyButtonAndMotionMembers() {
        var event = new XShim.XEventShim();
        ByteBuffer raw = event.buffer().order(ByteOrder.nativeOrder());
        raw.putInt(0, XShim.ClientMessage);
        assertEquals(33, event.getType());
        event.setWindow(0x0102_0304_0506L);
        assertEquals(0x0102_0304_0506L, raw.getLong(32), "xany.window");
        assertEquals(0x0102_0304_0506L, event.getWindow());
        raw.putInt(84, 3);
        assertEquals(3, event.getButton(), "xbutton.button");
        raw.putInt(84, 0xffff_fffe);
        assertEquals(-2, event.getButton(), "an unsigned int above 2^31 arrives as the (jint) of the C");
        raw.putInt(64, 640);
        raw.putInt(68, 480);
        assertEquals(640, event.getX(), "xmotion.x");
        assertEquals(480, event.getY(), "xmotion.y");
        assertEquals(640L | (480L << 32), raw.getLong(64), "the union: xmotion.x and y share xclient.data.l[1]");
    }

    @Test
    public void clientMessageSettersWriteTheirFields() {
        var event = new XShim.XEventShim();
        ByteBuffer raw = event.buffer().order(ByteOrder.nativeOrder());
        event.setMessageType(0x77L);
        event.setFormat(32);
        event.setDataLong(0, 1L);
        event.setDataLong(2, 0x1122_3344_5566_7788L);
        event.setDataLong(4, -1L);
        assertEquals(0x77L, raw.getLong(40), "xclient.message_type");
        assertEquals(32, raw.getInt(48), "xclient.format");
        assertEquals(1L, raw.getLong(56), "xclient.data.l[0]");
        assertEquals(0L, raw.getLong(64), "xclient.data.l[1] untouched");
        assertEquals(0x1122_3344_5566_7788L, raw.getLong(72), "xclient.data.l[2]");
        assertEquals(0L, raw.getLong(80), "xclient.data.l[3] untouched");
        assertEquals(-1L, raw.getLong(88), "xclient.data.l[4]");
        assertThrows(IndexOutOfBoundsException.class, () -> event.setDataLong(5, 0L), "data.l has five slots");
    }

    @Test
    public void colorSettersWriteTheUnsignedShorts() {
        var color = new XShim.XColorShim();
        color.setRed(0x1234);
        color.setGreen(65535);
        color.setBlue(7);
        ByteBuffer raw = color.buffer().order(ByteOrder.nativeOrder());
        assertEquals(0x1234, raw.getShort(8));
        assertEquals(-1, raw.getShort(10), "65535 as the unsigned short 0xffff");
        assertEquals(7, raw.getShort(12));
        assertEquals(0L, raw.getLong(0), "pixel untouched");
    }

    @Test
    public void aCLongOf8BytesIsRequired() {
        UnsatisfiedLinkError refused = assertThrows(UnsatisfiedLinkError.class, () -> XShim.requireLp64(4));
        assertTrue(refused.getMessage().contains("C long is 4 bytes"), refused.getMessage());
        assertTrue(refused.getMessage().contains(XShim.LIB_X11), refused.getMessage());
        assertDoesNotThrow(() -> XShim.requireLp64(8));
    }
}
