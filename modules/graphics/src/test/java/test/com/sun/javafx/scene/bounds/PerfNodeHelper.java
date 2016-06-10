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

package test.com.sun.javafx.scene.bounds;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import test.javafx.scene.bounds.PerfNode;

public class PerfNodeHelper extends NodeHelper {

    private static final PerfNodeHelper theInstance;
    private static PerfNodeAccessor perfNodeAccessor;

    static {
        theInstance = new PerfNodeHelper();
        Utils.forceInit(PerfNode.class);
    }

    private static PerfNodeHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(PerfNode perfNode) {
        setHelper(perfNode, getInstance());
    }

    public static void setPerfNodeAccessor(final PerfNodeAccessor newAccessor) {
        if (perfNodeAccessor != null) {
            throw new IllegalStateException();
        }

        perfNodeAccessor = newAccessor;
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return perfNodeAccessor.doCreatePeer(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return perfNodeAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return perfNodeAccessor.doComputeContains(node, localX, localY);
    }

    @Override
    protected Object processMXNodeImpl(Node node, MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return perfNodeAccessor.doProcessMXNode(node, alg, ctx);
    }

    public interface PerfNodeAccessor {
        NGNode doCreatePeer(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        boolean doComputeContains(Node node, double localX, double localY);
        Object doProcessMXNode(Node node, MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx);

    }

}
