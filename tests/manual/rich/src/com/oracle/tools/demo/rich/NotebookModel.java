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
package com.oracle.tools.demo.rich;

import javafx.scene.Node;

public class NotebookModel extends SimpleReadOnlyStyledModel {
    public NotebookModel() {
        String GREEN = "green";
        String GRAY = "gray";
        String EQ = "equation";
        String SUB = "sub";
        String UNDER = "underline";
        
        addSegment("Bifurcation Diagram", "-fx-font-size:200%;", UNDER);
        nl(2);
        addSegment("In mathematics, particularly in dynamical systems, a ", null, GRAY);
        addSegment("bifurcation diagram ", "-fx-font-weight:bold;"); // FIX does not work on mac
        addSegment("shows the values visited or approached asymptotically (fixed points, periodic orbits, or chaotic attractors) of a system as a function of a bifurcation parameter in the system. It is usual to represent stable values with a solid line and unstable values with a dotted line, although often the unstable points are omitted. Bifurcation diagrams enable the visualization of bifurcation theory.", null, GRAY);
        nl(2);
        addSegment("An example is the bifurcation diagram of the logistic map:", null, GRAY);
        nl(2);
        addSegment("   x", EQ);
        addSegment("n+1", null, EQ, SUB);
        addSegment(" = λx", EQ);
        addSegment("n", EQ, SUB);
        addSegment("(1 - x", EQ);
        addSegment("n", EQ, SUB);
        addSegment(")", null, EQ);
        nl(2);
        addSegment("The bifurcation parameter λ is shown on the horizontal axis of the plot and the vertical axis shows the set of values of the logistic function visited asymptotically from almost all initial conditions.", null, GRAY);
        nl(2);
        addSegment("The bifurcation diagram shows the forking of the periods of stable orbits from 1 to 2 to 4 to 8 etc. Each of these bifurcation points is a period-doubling bifurcation. The ratio of the lengths of successive intervals between values of r for which bifurcation occurs converges to the first Feigenbaum constant.", null, GRAY);
        nl(2);
        addSegment("The diagram also shows period doublings from 3 to 6 to 12 etc., from 5 to 10 to 20 etc., and so forth.", null, GRAY);
        nl();
        addParagraph(BifurcationDiagram::generate);
        nl(2);
        addSegment("Source: Wikipedia");
        nl();
        addSegment("https://en.wikipedia.org/wiki/Bifurcation_diagram", null, GREEN, UNDER);
    }
}
