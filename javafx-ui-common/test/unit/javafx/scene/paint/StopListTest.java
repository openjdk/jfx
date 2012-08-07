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

package javafx.scene.paint;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StopListTest {

    private final Color color1 = new Color(0.0, 0.0, 0.0, 0.0);
    private final Color color2 = new Color(0.5, 0.5, 0.5, 0.5);
    private final Color color3 = new Color(1.0, 1.0, 1.0, 1.0);
    private final Stop zerostop = new Stop(0.0, color1);
    private final Stop stop1 = new Stop(0.1, color1);
    private final Stop stop2 = new Stop(0.2, color2);
    private final Stop stop3 = new Stop(0.3, color3);
    private final Stop onestop = new Stop(1.0, color3);

    static List<Stop> normalize(Stop... stops) {
        LinearGradient lg = new LinearGradient(0, 0, 1, 1, true,
                                               CycleMethod.NO_CYCLE, stops);
        RadialGradient rg = new RadialGradient(0, 0, 0, 0, 1, true,
                                               CycleMethod.NO_CYCLE, stops);
        assertEquals(lg.getStops(), rg.getStops());
        return lg.getStops();
    }

    static List<Stop> normalize(List<Stop> stops) {
        LinearGradient lg = new LinearGradient(0, 0, 1, 1, true,
                                               CycleMethod.NO_CYCLE, stops);
        RadialGradient rg = new RadialGradient(0, 0, 0, 0, 1, true,
                                               CycleMethod.NO_CYCLE, stops);
        assertEquals(lg.getStops(), rg.getStops());
        return lg.getStops();
    }

    @Test
    public void testNormalizeStopsEmpty() {
        List<Stop> zeroOneList = Arrays.asList(
            new Stop(0.0, Color.TRANSPARENT),
            new Stop(1.0, Color.TRANSPARENT)
        );

        assertEquals(zeroOneList, normalize((Stop[]) null));
        assertEquals(zeroOneList, normalize((List<Stop>) null));
        assertEquals(zeroOneList, normalize(new Stop(0.5, null)));
        assertEquals(zeroOneList, normalize(new Stop[0]));
        assertEquals(zeroOneList, normalize(new Stop[] { null }));
        assertEquals(zeroOneList, normalize(new Stop[] { null, null, null }));
    }

    @Test
    public void testNormalizeOneStop() {
        Stop[] justzero = new Stop[] { zerostop };
        Stop[] justone = new Stop[] { onestop };
        Stop[] justmid = new Stop[] { stop2 };
        List<Stop> allZeroList = Arrays.asList(zerostop, new Stop(1.0, color1));
        List<Stop> allOneList = Arrays.asList(new Stop(0.0, color3), onestop);
        List<Stop> allColor2List = Arrays.asList(
            new Stop(0.0, color2),
            new Stop(1.0, color2)
        );

        assertEquals(allZeroList, normalize(justzero));
        assertEquals(allOneList, normalize(justone));
        assertEquals(allColor2List, normalize(justmid));
    }

    @Test
    public void testNormalizeStopsNonEmpty() {
        Stop[] noNull = new Stop[] { stop1, stop2, stop3 };
        Stop[] nullFirst = new Stop[] { null, null, stop1, stop2, stop3 };
        Stop[] nullMiddle = new Stop[] { stop1, null, null, stop2, stop3 };
        Stop[] nullLast = new Stop[] { stop1, stop2, stop3, null, null };
        Stop[] manyNulls = new Stop[] { null, stop1, null, null, stop2, null,
                stop3, null };
        List<Stop> noNullList =
            Arrays.asList(zerostop, stop1, stop2, stop3, onestop);

        assertEquals(noNullList, normalize(noNull));
        assertEquals(noNullList, normalize(nullFirst));
        assertEquals(noNullList, normalize(nullMiddle));
        assertEquals(noNullList, normalize(nullLast));
        assertEquals(noNullList, normalize(manyNulls));
    }

    @Test
    public void testNormalizeStopsDuplicated() {
        Stop[] dupzeros = new Stop[] { zerostop, zerostop, zerostop,
                                       stop1, stop2, stop3 };
        Stop[] onedup = new Stop[] { stop1, stop1, stop2, stop2, stop3, stop3 };
        Stop[] twodups = new Stop[] { stop1, stop1, stop1,
                                      stop2, stop2, stop2,
                                      stop3, stop3, stop3 };
        List<Stop> singleList =
            Arrays.asList(zerostop, stop1, stop2, stop3, onestop);
        List<Stop> dupList =
            Arrays.asList(zerostop,
                          stop1, stop1, stop2, stop2, stop3, stop3,
                          onestop);

        assertEquals(singleList, normalize(dupzeros));
        assertEquals(dupList, normalize(onedup));
        assertEquals(dupList, normalize(twodups));
    }

    @Test
    public void testNormalizeStopsNonUnsorted() {
        Stop[] unordered = new Stop[] { stop2, stop3, stop1 };
        Stop[] unordereddups = new Stop[] { stop3, stop2, stop1,
                                            stop2, stop3, stop1 };
        List<Stop> sortedList =
            Arrays.asList(zerostop, stop1, stop2, stop3, onestop);
        List<Stop> dupSortedList =
            Arrays.asList(zerostop,
                          stop1, stop1, stop2, stop2, stop3, stop3,
                          onestop);

        assertEquals(sortedList, normalize(unordered));
        assertEquals(dupSortedList, normalize(unordereddups));
    }
}
