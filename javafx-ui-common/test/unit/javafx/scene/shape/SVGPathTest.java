/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import static org.junit.Assert.assertEquals;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.NodeTest;

import org.junit.Test;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.pgstub.StubSVGPath;

public class SVGPathTest {
    
    @Test public void testBoundPropertySync_Content() throws Exception {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent("M40,60 C42,48 44,30 25,32");

        StringProperty content = new SimpleStringProperty();
        svgPath.contentProperty().bind(content);
        String s = "M11,11 C22,22 33,33";
        content.set(s);
        NodeTest.syncNode(svgPath);

        StubSVGPath.SVGPathImpl geometry = (StubSVGPath.SVGPathImpl)
                ((StubSVGPath) svgPath.impl_getPGSVGPath()).getGeometry();
        assertEquals(s, geometry.getContent());
    }

    @Test
    public void testDefaultValues() {
        SVGPath svgPath = new SVGPath();
        assertEquals("", svgPath.getContent());
        assertEquals("", svgPath.contentProperty().get());
        assertEquals(FillRule.NON_ZERO, svgPath.getFillRule());
        assertEquals(FillRule.NON_ZERO, svgPath.fillRuleProperty().get());
    }

    @Test public void testFillRuleSync() {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent("M40,60 C42,48 44,30 25,32");
        svgPath.setFillRule(FillRule.EVEN_ODD);
        StubSVGPath peer = (StubSVGPath) svgPath.impl_getPGSVGPath();
        peer.setAcceptsPath2dOnUpdate(true);

        NodeTest.syncNode(svgPath);
        Path2D geometry = (Path2D)((StubSVGPath)svgPath.impl_getPGSVGPath()).getGeometry();
        assertEquals(Path2D.WIND_EVEN_ODD, geometry.getWindingRule());

        svgPath.setFillRule(FillRule.NON_ZERO);
        NodeTest.syncNode(svgPath);
        // internal shape might have changed, getting it again
        geometry = (Path2D)((StubSVGPath)svgPath.impl_getPGSVGPath()).getGeometry();
        assertEquals(Path2D.WIND_NON_ZERO, geometry.getWindingRule());
    }
}
