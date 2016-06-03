/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.shape;

import com.sun.javafx.scene.shape.EllipseHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.Ellipse;
import test.javafx.scene.shape.EllipseTest;

public class StubEllipseHelper extends EllipseHelper {

    private static final StubEllipseHelper theInstance;
    private static StubEllipseAccessor stubEllipseAccessor;

    static {
        theInstance = new StubEllipseHelper();
        Utils.forceInit(EllipseTest.StubEllipse.class);
    }

    private static StubEllipseHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Ellipse ellipse) {
        setHelper(ellipse, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return stubEllipseAccessor.doCreatePeer(node);
    }

    public static void setStubEllipseAccessor(final StubEllipseAccessor newAccessor) {
        if (stubEllipseAccessor != null) {
            throw new IllegalStateException();
        }

        stubEllipseAccessor = newAccessor;
    }

    public interface StubEllipseAccessor {
        NGNode doCreatePeer(Node node);
    }

}
