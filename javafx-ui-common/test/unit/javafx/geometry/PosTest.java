/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.geometry;

import static java.util.Arrays.asList;
import static javafx.geometry.HPos.LEFT;
import static javafx.geometry.HPos.RIGHT;
import static javafx.geometry.Pos.BASELINE_CENTER;
import static javafx.geometry.Pos.BASELINE_LEFT;
import static javafx.geometry.Pos.BASELINE_RIGHT;
import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.geometry.Pos.BOTTOM_RIGHT;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.geometry.Pos.TOP_RIGHT;
import static javafx.geometry.VPos.BASELINE;
import static javafx.geometry.VPos.BOTTOM;
import static javafx.geometry.VPos.TOP;
import static junit.framework.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class PosTest {

    @Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][] {
                {TOP_LEFT, TOP, LEFT},
                {TOP_CENTER, TOP, HPos.CENTER}, 
                {TOP_RIGHT, TOP, RIGHT},
                {CENTER_LEFT, VPos.CENTER, LEFT},
                {Pos.CENTER, VPos.CENTER, HPos.CENTER},
                {CENTER_RIGHT, VPos.CENTER, RIGHT},
                {BOTTOM_LEFT, BOTTOM, LEFT},
                {BOTTOM_CENTER, BOTTOM, HPos.CENTER},
                {BOTTOM_RIGHT, BOTTOM, RIGHT}, 
                {BASELINE_LEFT, BASELINE, LEFT},
                {BASELINE_CENTER, BASELINE, HPos.CENTER},
                {BASELINE_RIGHT, BASELINE, RIGHT},
         });
    }
    
    private final Pos pos;
    private final VPos vpos;
    private final HPos hpos;

    public PosTest(Pos pos, VPos vpos, HPos hpos) {
        this.pos = pos;
        this.vpos = vpos;
        this.hpos = hpos;
    }

    @Test public void shouldHaveVPosAndHPos() {
        assertEquals(pos.getVpos(), vpos);
        assertEquals(pos.getHpos(), hpos);
    }
}
