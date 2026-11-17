/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

public class TestStyledInput implements StyledInput {
    private final StyledSegment[] segments;
    private int index;

    private TestStyledInput(StyledSegment[] segments) {
        this.segments = segments;
    }

    public static TestStyledInput plainText(String text) {
        String[] lines = text.split("\n");
        ArrayList<StyledSegment> ss = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                ss.add(StyledSegment.LINE_BREAK);
            }
            ss.add(StyledSegment.of(lines[i]));
        }
        return new TestStyledInput(ss.toArray(StyledSegment[]::new));
    }

    public static SegmentStyledInput of(List<StyledSegment> segments) {
        StyledSegment[] ss = segments.toArray(new StyledSegment[segments.size()]);
        return new SegmentStyledInput(ss);
    }

    @Override
    public StyledSegment nextSegment() {
        if (index < segments.length) {
            return segments[index++];
        }
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}
