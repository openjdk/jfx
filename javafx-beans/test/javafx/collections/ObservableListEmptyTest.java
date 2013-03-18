/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.collections;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for initially empty ObservableList.
 */
@RunWith(Parameterized.class)
public class ObservableListEmptyTest {

    static final List<String> EMPTY = Collections.emptyList();
    final Callable<ObservableList<String>> listFactory;
    ObservableList<String> list;
    MockListObserver<String> mlo;


    public ObservableListEmptyTest(final Callable<ObservableList<String>> listFactory) {
        this.listFactory = listFactory;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { TestedObservableLists.ARRAY_LIST },
            { TestedObservableLists.LINKED_LIST },
            { TestedObservableLists.VETOABLE_LIST },
            { TestedObservableLists.CHECKED_OBSERVABLE_ARRAY_LIST },
            { TestedObservableLists.SYNCHRONIZED_OBSERVABLE_ARRAY_LIST }
         };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        list = listFactory.call();
        mlo = new MockListObserver<String>();
        list.addListener(mlo);
    }

    @Test
    public void testClearEmpty() {
        list = FXCollections.observableList(EMPTY);
        list.addListener(mlo);
        list.clear();
        mlo.check0();
    }
}
