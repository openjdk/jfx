/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlowShim;
import javafx.scene.control.skin.VirtualFlow;

import org.junit.Before;
import org.junit.Test;

/**
  A custom ListViewSkin class that re-implements createVirtualFlow() method
  to create a custom VirtualFlow
 */
class SkinWithCustomVirtualFlow<T> extends ListViewSkin<T> {

    public SkinWithCustomVirtualFlow(final ListView<T> control) {
        super(control);
    }

    @Override
    protected VirtualFlow<ListCell<T>> createVirtualFlow() {
       return new VirtualFlowShim<>();
    }

    /* Methods for test purpose */

    public boolean isVirtualFlowNull() {
        return (getVirtualFlow() == null);
    }

    public boolean isCustomVirtualFlow() {
        return (getVirtualFlow() instanceof VirtualFlowShim);
    }
}

/**
  A custom ListViewSkin class that does not re-implement createVirtualFlow()
  method. This results in default behavior of createVirtualFlow() provided
  in class VirtualContainerBase
 */
class SkinWithDefaultVirtualFlow<T> extends ListViewSkin<T> {

    public SkinWithDefaultVirtualFlow(final ListView<T> control) {
        super(control);
    }

    /* Methods for test purpose */

    public boolean isVirtualFlowNull() {
        return (getVirtualFlow() == null);
    }

    public boolean isDefaultVirtualFlow() {
        return (getVirtualFlow().getClass().equals(VirtualFlow.class));
    }
}

public class CustomListViewSkinTest {

    private ListView<String> listViewObj = null;

    @Before public void setup() {
        listViewObj = new ListView<>();
    }

    @Test public void testCustomVirtualFlow() {
        SkinWithCustomVirtualFlow skin =
            new SkinWithCustomVirtualFlow<>(listViewObj);
        listViewObj.setSkin(skin);

        assertFalse(skin.isVirtualFlowNull());
        assertTrue(skin.isCustomVirtualFlow());
    }

    @Test public void testDefaultVirtualFlow() {
        SkinWithDefaultVirtualFlow skin =
            new SkinWithDefaultVirtualFlow<>(listViewObj);
        listViewObj.setSkin(skin);

        assertFalse(skin.isVirtualFlowNull());
        assertTrue(skin.isDefaultVirtualFlow());
    }
}
