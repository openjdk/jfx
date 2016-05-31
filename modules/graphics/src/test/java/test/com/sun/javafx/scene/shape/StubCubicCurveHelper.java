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

import com.sun.javafx.scene.shape.CubicCurveHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.CubicCurve;
import test.javafx.scene.shape.CubicCurveTest;

public class StubCubicCurveHelper extends CubicCurveHelper {

    private static final StubCubicCurveHelper theInstance;
    private static StubCubicCurveAccessor stubCubicCurveAccessor;

    static {
        theInstance = new StubCubicCurveHelper();
        Utils.forceInit(CubicCurveTest.StubCubicCurve.class);
    }

    private static StubCubicCurveHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(CubicCurve cubicCurve) {
        setHelper(cubicCurve, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return stubCubicCurveAccessor.doCreatePeer(node);
    }

    public static void setStubCubicCurveAccessor(final StubCubicCurveAccessor newAccessor) {
        if (stubCubicCurveAccessor != null) {
            throw new IllegalStateException();
        }

        stubCubicCurveAccessor = newAccessor;
    }

    public interface StubCubicCurveAccessor {
        NGNode doCreatePeer(Node node);
    }

}
