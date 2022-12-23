package test.javafx.scene.control;

/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

public class InputMapTest {

    @Test public void dummy() {
        // no-op
    }

//    /***************************************************************************
//     *
//     * Control InputMap mappings population / removal
//     *
//     **************************************************************************/
//
//    @Test public void testControlHasNonNullInputMap() {
//        Button btn = new Button();
//        assertNotNull(btn.getInputMap());
//    }
//
//    @Test public void testDefaultInputMapIsEmpty() {
//        Button btn = new Button();
//        assertEquals(0, btn.getInputMap().getMappings().size());
//    }
//
//    @Test public void addControlToSceneAndCheckInputMapIsPopulated() {
//        Button btn = new Button();
//        assertEquals(0, btn.getInputMap().getMappings().size());
//        StageLoader sl = new StageLoader(btn);
//        assertFalse(btn.getInputMap().getMappings().isEmpty());
//        sl.dispose();
//    }
//
//    @Test public void testSkinDoesNotOverrideUserDefinedMappings() {
//        Button btn = new Button();
//        assertEquals(0, btn.getInputMap().getMappings().size());
//
//        // ENTER is not a default mapping, but SPACE is
//        InputMap.KeyMapping customEnterMapping = new InputMap.KeyMapping(ENTER, e -> {
//            // no-op
//        });
//        InputMap.KeyMapping customSpaceMapping = new InputMap.KeyMapping(SPACE, e -> {
//            // no-op
//        });
//        btn.getInputMap().getMappings().addAll(customEnterMapping, customSpaceMapping);
//        assertEquals(2, btn.getInputMap().getMappings().size());
//
//        StageLoader sl = new StageLoader(btn);
//
//        // There are 18 mappings provided by ButtonBehavior. We add two above,
//        // but one of them displaces a default mapping, so we expect 19.
//        assertEquals(19, btn.getInputMap().getMappings().size());
//
//        // we want to ensure that our two mappings still exist
//        assertTrue(btn.getInputMap().getMappings().contains(customEnterMapping));
//        assertTrue(btn.getInputMap().getMappings().contains(customSpaceMapping));
//
//        // we also look up the mappings using their key codes, to double check
//        // that they are there
//        assertNotNull(btn.getInputMap().lookupMapping(ENTER));
//        assertNotNull(btn.getInputMap().lookupMapping(SPACE));
//
//        sl.dispose();
//    }
//
//    @Test public void removeSkinAndEnsureSkinMappingsAreRemoved() {
//        Button btn = new Button();
//        assertEquals(0, btn.getInputMap().getMappings().size());
//
//        StageLoader sl = new StageLoader(btn);
//
//        // There are 18 mappings provided by ButtonBehavior.
//        assertEquals(18, btn.getInputMap().getMappings().size());
//        btn.setSkin(null);
//        assertEquals(0, btn.getInputMap().getMappings().size());
//
//        sl.dispose();
//    }
//
//    @Test public void removeSkinAndEnsureSkinMappingsAreRemoved_shouldNotRemoveUserDefinedMappings() {
//        Button btn = new Button();
//        assertEquals(0, btn.getInputMap().getMappings().size());
//
//        // ENTER is not a default mapping, but SPACE is
//        InputMap.KeyMapping customEnterMapping = new InputMap.KeyMapping(ENTER, e -> {
//            // no-op
//        });
//        InputMap.KeyMapping customSpaceMapping = new InputMap.KeyMapping(SPACE, e -> {
//            // no-op
//        });
//        btn.getInputMap().getMappings().addAll(customEnterMapping, customSpaceMapping);
//        assertEquals(2, btn.getInputMap().getMappings().size());
//
//        StageLoader sl = new StageLoader(btn);
//
//        // There are 18 mappings provided by ButtonBehavior. We add two above,
//        // but one of them displaces a default mapping, so we expect 19.
//        assertEquals(19, btn.getInputMap().getMappings().size());
//
//        // remove the skin - we expect our two mappings to still exist - but that is it
//        btn.setSkin(null);
//        assertEquals(2, btn.getInputMap().getMappings().size());
//
//        // we want to ensure that our two mappings still exist
//        assertTrue(btn.getInputMap().getMappings().contains(customEnterMapping));
//        assertTrue(btn.getInputMap().getMappings().contains(customSpaceMapping));
//
//        // we also look up the mappings using their key codes, to double check
//        // that they are there
//        assertNotNull(btn.getInputMap().lookupMapping(ENTER));
//        assertNotNull(btn.getInputMap().lookupMapping(SPACE));
//
//        sl.dispose();
//    }
//
//
//    /***************************************************************************
//     *
//     * Control InputMap childMap population / removal
//     *
//     **************************************************************************/
//
//    @Test public void testButtonControlHasNoInputMapChildMaps_afterSkinLoaded() {
//        Button btn = new Button();
//
//        assertNotNull(btn.getInputMap().getChildInputMaps());
//        assertTrue(btn.getInputMap().getChildInputMaps().isEmpty());
//
//        StageLoader sl = new StageLoader(btn);
//        assertNotNull(btn.getInputMap().getChildInputMaps());
//        assertTrue(btn.getInputMap().getChildInputMaps().isEmpty());
//        sl.dispose();
//    }
//
//    @Test public void testListViewControlHasInputMapChildMaps_afterSkinLoaded() {
//        ListView listView = new ListView();
//
//        assertNotNull(listView.getInputMap().getChildInputMaps());
//        assertTrue(listView.getInputMap().getChildInputMaps().isEmpty());
//
//        StageLoader sl = new StageLoader(listView);
//        assertNotNull(listView.getInputMap().getChildInputMaps());
//        assertFalse(listView.getInputMap().getChildInputMaps().isEmpty());
//        sl.dispose();
//    }
//
//    @Test public void removeSkinAndEnsureSkinChildInputMapsAreRemoved() {
//        ListView listView = new ListView();
//
//        StageLoader sl = new StageLoader(listView);
//
//        assertNotNull(listView.getInputMap().getChildInputMaps());
//        assertFalse(listView.getInputMap().getChildInputMaps().isEmpty());
//
//        listView.setSkin(null);
//        assertTrue(listView.getInputMap().getChildInputMaps().isEmpty());
//
//        sl.dispose();
//    }
//
//    @Test public void removeSkinAndEnsureSkinChildInputMapsAreRemoved_shouldNotRemoveUserDefinedInputMaps() {
//        ListView<?> listView = new ListView<>();
//
//        InputMap<ListView<?>> dummyMap = new InputMap<>(listView);
//        dummyMap.getMappings().add(new InputMap.KeyMapping(ENTER, e -> {
//            // no-op
//        }));
//        ((InputMap<ListView<?>>)listView.getInputMap()).getChildInputMaps().add(dummyMap);
//
//        assertEquals(1, listView.getInputMap().getChildInputMaps().size());
//
//        StageLoader sl = new StageLoader(listView);
//
//        assertNotNull(listView.getInputMap().getChildInputMaps());
//        assertFalse(listView.getInputMap().getChildInputMaps().isEmpty());
//
//        listView.setSkin(null);
//        assertEquals(1, listView.getInputMap().getChildInputMaps().size());
//        assertEquals(dummyMap, listView.getInputMap().getChildInputMaps().get(0));
//
//        sl.dispose();
//    }
}
