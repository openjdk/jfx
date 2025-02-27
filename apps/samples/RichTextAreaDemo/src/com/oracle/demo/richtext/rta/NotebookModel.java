/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.rta;

import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Sample notebook model.
 *
 * @author Andy Goryachev
 */
public class NotebookModel extends SimpleViewOnlyStyledModel {

    public static final StyleAttribute<Boolean> OUTLINE = new StyleAttribute<>("OUTLINE", Boolean.class, true);

    public NotebookModel() {
        String GREEN = "green";
        String GRAY = "gray";
        String EQ = "equation";
        String SUB = "sub";
        String UNDER = "underline";

        addWithInlineAndStyleNames("Bifurcation Diagram", "-fx-font-size:200%;", UNDER);
        nl(2);
        addWithStyleNames("In mathematics, particularly in dynamical systems, a ", GRAY);
        addWithStyleNames("bifurcation diagram ", "-fx-font-weight:bold;"); // FIX does not work on mac
        addWithStyleNames("shows the values visited or approached asymptotically (fixed points, periodic orbits, or chaotic attractors) of a system as a function of a bifurcation parameter in the system. It is usual to represent stable values with a solid line and unstable values with a dotted line, although often the unstable points are omitted. Bifurcation diagrams enable the visualization of bifurcation theory.", GRAY);
        nl(2);
        addWithStyleNames("An example is the bifurcation diagram of the logistic map:", GRAY);
        nl(2);
        addWithStyleNames("   x", EQ);
        addWithStyleNames("n+1", EQ, SUB);
        addWithStyleNames(" = λx", EQ);
        addWithStyleNames("n", EQ, SUB);
        addWithStyleNames("(1 - x", EQ);
        addWithStyleNames("n", EQ, SUB);
        addWithStyleNames(")", EQ);
        setParagraphAttributes(StyleAttributeMap.of(OUTLINE, Boolean.TRUE));
        nl(2);
        addWithStyleNames("The bifurcation parameter λ is shown on the horizontal axis of the plot and the vertical axis shows the set of values of the logistic function visited asymptotically from almost all initial conditions.", GRAY);
        nl(2);
        addWithStyleNames("The bifurcation diagram shows the forking of the periods of stable orbits from 1 to 2 to 4 to 8 etc. Each of these bifurcation points is a period-doubling bifurcation. The ratio of the lengths of successive intervals between values of r for which bifurcation occurs converges to the first Feigenbaum constant.", GRAY);
        nl(2);
        addWithStyleNames("The diagram also shows period doublings from 3 to 6 to 12 etc., from 5 to 10 to 20 etc., and so forth.", GRAY);
        nl();
        addParagraph(BifurcationDiagram::generate);
        nl(2);
        addSegment("Source: Wikipedia");
        nl();
        addWithStyleNames("https://en.wikipedia.org/wiki/Bifurcation_diagram", GREEN, UNDER);
    }
}
