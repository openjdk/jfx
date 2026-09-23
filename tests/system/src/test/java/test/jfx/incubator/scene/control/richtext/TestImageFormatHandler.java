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

package test.jfx.incubator.scene.control.richtext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledOutput;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.EmbeddedImage;
import jfx.incubator.scene.control.richtext.model.ImageFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import test.com.sun.javafx.images.Images;
import test.robot.testharness.RobotTestBase;
import test.util.Util;

// Tests ImageFormatHandler
public class TestImageFormatHandler extends RobotTestBase {

    private static final byte[] PNG_SIGNATURE = Util.hexToByteArray("89504e47");
    private static final ImageFormatHandler handler = ImageFormatHandler.getInstance();
    private RichTextArea rta;

    @BeforeEach
    public void beforeEach() {
        rta = new RichTextArea();
        setContent(rta);
    }

    @Test
    public void processJpg() {
        test("banana-slug.jpg");
    }

    @Test
    public void processPng() {
        test("underlines.png");
    }

    @Test
    public void copy() {
        RichTextModel m = new RichTextModel();
        assertThrows(UnsupportedOperationException.class, () -> {
            handler.copy(m, null, TextPos.ZERO, TextPos.ZERO);
        });
    }

    @Test
    public void save() {
        RichTextModel m = new RichTextModel();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(UnsupportedOperationException.class, () -> {
            handler.save(m, null, TextPos.ZERO, TextPos.ZERO, out);
        });
    }

    private static List<StyledSegment> load(StyledInput in) {
        ArrayList<StyledSegment> ss = new ArrayList<>();
        StyledSegment s;
        while ((s = in.nextSegment()) != null) {
            ss.add(s);
        }
        return ss;
    }

    private void test(String name) {
        String url = Images.getURL(name);
        Image im = new Image(url);
        StyledInput in = null;
        try {
            in = handler.createStyledInput(im, null);
        } catch (IOException e) {
            fail(e);
        }

        List<StyledSegment> ss = load(in);
        assertEquals(1, ss.size());

        StyledSegment s = ss.getFirst();
        assertEquals(1, s.getText().length());
        StyleAttributeMap a = s.getStyleAttributeMap(null);
        EmbeddedImage em = a.get(StyleAttributeMap.EMBEDDED_IMAGE);
        assertNotNull(em);
        byte[] b = EmbeddedImageHelper.getBytes(em);
        assertNotNull(b);
        assertSignature(b);

        // let's see if it pastes
        Util.runAndWait(() -> {
            rta.clear();
            ClipboardContent cc = new ClipboardContent();
            cc.putImage(im);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        AtomicBoolean pasted = new AtomicBoolean();
        Util.runAndWait(() -> {
            rta.select(TextPos.ZERO);
            rta.paste();
            StyledTextModel m = rta.getModel();
            SegmentStyledOutput out = new SegmentStyledOutput(8);
            try {
                m.export(TextPos.ZERO, m.getDocumentEnd(), out);
            } catch (Exception e) {
                fail(e);
            }
            StyledSegment[] exported = out.getSegments();
            for (StyledSegment seg : exported) {
                StyleAttributeMap att = seg.getStyleAttributeMap(null);
                if (att != null) {
                    EmbeddedImage em2 = att.get(StyleAttributeMap.EMBEDDED_IMAGE);
                    if (em2 != null) {
                        pasted.set(true);
                    }
                }
            }
            assertTrue(pasted.get());
        });
    }

    private void assertSignature(byte[] bytes) {
        assertNotNull(bytes);
        assertTrue(bytes.length > PNG_SIGNATURE.length);
        byte[] b = Arrays.copyOf(bytes, PNG_SIGNATURE.length);
        assertArrayEquals(PNG_SIGNATURE, b, "wrong image signature");
    }
}
