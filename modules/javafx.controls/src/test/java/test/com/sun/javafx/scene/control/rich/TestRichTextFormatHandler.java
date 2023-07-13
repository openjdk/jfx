/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.rich;

import java.util.ArrayList;
import javafx.scene.control.rich.model.StyledInput;
import javafx.scene.control.rich.model.StyledOutput;
import javafx.scene.control.rich.model.StyledSegment;
import org.junit.Test;
import com.sun.javafx.scene.control.rich.RichTextFormatHandler;

/**
 * Tests RichTextFormatHandler.
 */
public class TestRichTextFormatHandler {
    @Test
    public void testRoundTrip() {
        String[] ss = {
            "\\B\\\\A\\I\\\\B\n\\0\\\\C\\1\\\\D\n",
            //"\\Z200\\C808080\\\\name1\\0\\\\: \\Z200\\\\val\\1\\\\1\\1\\\\\n\n\\0\\\\name2\\0\\\\: \\1\\\\val2\n\n"
        };
        
        RichTextFormatHandler handler = new RichTextFormatHandler();
        
        for(String text: ss) {
            testRoundTrip(handler, text);
        }
    }
    
    private void testRoundTrip(RichTextFormatHandler handler, String text) {
        ArrayList<StyledSegment> segments = new ArrayList<>();
        
        StyledInput in = handler.getStyledInput(text);
        StyledSegment seg;
        while((seg = in.nextSegment()) != null) {
            segments.add(seg);
        }
        
        // TODO pass output stream or writer
//        StyledOutput out = handler.getStyledOutput();
//        for(StyledSegment s: segments) {
//            out.append(s);
//        }
        
        // TODO compare
    }
}
