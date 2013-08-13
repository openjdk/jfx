/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.pgstub.SVGPathImpl;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGSVGPath;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.NodeTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class SVGPathTest {
    
    @Test public void testBoundPropertySync_Content() throws Exception {
        StubSVGPath svgPath = new StubSVGPath();
        svgPath.setContent("M40,60 C42,48 44,30 25,32");

        StringProperty content = new SimpleStringProperty();
        svgPath.contentProperty().bind(content);
        String s = "M11,11 C22,22 33,33";
        content.set(s);
        NodeTest.syncNode(svgPath);

        SVGPathImpl geometry = ((StubNGSVGPath) svgPath.impl_getPeer()).geometry;
        assertEquals(s, geometry.getContent());
    }

    @Test
    public void testDefaultValues() {
        StubSVGPath svgPath = new StubSVGPath();
        assertEquals("", svgPath.getContent());
        assertEquals("", svgPath.contentProperty().get());
        assertEquals(FillRule.NON_ZERO, svgPath.getFillRule());
        assertEquals(FillRule.NON_ZERO, svgPath.fillRuleProperty().get());
    }

    @Test public void testFillRuleSync() {
        StubSVGPath svgPath = new StubSVGPath();
        svgPath.setContent("M40,60 C42,48 44,30 25,32");
        svgPath.setFillRule(FillRule.EVEN_ODD);
        StubNGSVGPath peer = svgPath.impl_getPeer();
        peer.setAcceptsPath2dOnUpdate(true);

        NodeTest.syncNode(svgPath);
        Path2D path = peer.path;
        assertEquals(Path2D.WIND_EVEN_ODD, path.getWindingRule());

        svgPath.setFillRule(FillRule.NON_ZERO);
        NodeTest.syncNode(svgPath);
        // internal shape might have changed, getting it again
        path = peer.path;
        assertEquals(Path2D.WIND_NON_ZERO, path.getWindingRule());
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubSVGPath().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private class StubSVGPath extends SVGPath {
        @Override
        protected NGNode impl_createPeer() {
            return new StubNGSVGPath();
        }
    }

    private class StubNGSVGPath extends NGSVGPath {
        private SVGPathImpl geometry;
        private Path2D path;
        private boolean acceptsPath2dOnUpdate = false;

        @Override
        public void setContent(Object content) {
            if (acceptsPath2dOnUpdate) {
                path = (Path2D) content;
            } else {
                geometry = (SVGPathImpl) content;
            }
        }

        public void setAcceptsPath2dOnUpdate(boolean acceptsPath2dOnUpdate) {
            this.acceptsPath2dOnUpdate = acceptsPath2dOnUpdate;
        }

        @Override
        public boolean acceptsPath2dOnUpdate() {
            return acceptsPath2dOnUpdate;
        }
    }
}
