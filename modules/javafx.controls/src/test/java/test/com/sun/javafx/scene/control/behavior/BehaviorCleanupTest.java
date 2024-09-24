/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.behavior;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.control.skin.TextInputSkinShim.isCaretBlinking;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createBehavior;
import java.lang.ref.WeakReference;
import java.util.Set;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ControlShim;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.input.KeyBinding;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ListCellBehavior;

/**
 * Test for misbehavior of individual implementations that turned
 * up in binch testing.
 *
 */
public class BehaviorCleanupTest {

    private Scene scene;
    private Stage stage;
    private Pane root;

//---------- TextAera

    @Test
    public void testTextAreaFocusListener() {
        TextArea control = new TextArea("some text");
        showControl(control, true);
        assertTrue(isCaretBlinking(control), "caret must be blinking if focused");
        Button button = new Button("dummy");
        showControl(button, true);
        assertFalse(isCaretBlinking(control), "caret must not be blinking if not focused");
    }

//---------- TextField

    @Test
    public void testFocusListener() {
        TextField control = new TextField("some text");
        showControl(control, true);
        assertTrue(isCaretBlinking(control), "caret must be blinking if focused");
        Button button = new Button("dummy");
        showControl(button, true);
        assertFalse(isCaretBlinking(control), "caret must not be blinking if not focused");
    }

    @Test
    public void testFocusOwnerListenerRegisteredInitially() {
        TextField control = new TextField("some text");
        showControl(control, true);
        assertEquals(control.getText(), control.getSelectedText(), "all text selected");
        Button button = new Button("dummy");
        showControl(button, true);
        assertEquals(0, control.getSelectedText().length(), "selection cleared");
    }

    /**
     * Tests that focusOwnerListener is re-wired as expected on changing scene
     * (here: by removing/adding the textField)
     *
     * Note: this tests both sceneListener and focusOwnerListener are properly updated.
     */
    @Test
    public void testFocusOwnerListenerOnSceneChanged() {
        // setup: two focusable nodes, textField focused
        String firstWord = "some";
        String secondWord = "text";
        String text = firstWord + " " + secondWord;
        TextField control = new TextField(text);
        showControl(control, true);
        Button button = new Button("dummy");
        showControl(button, false);
        control.selectNextWord();
        assertEquals(secondWord, control.getSelectedText(), "sanity: ");
        // detach textfield from scene
        root.getChildren().remove(control);
        assertEquals(secondWord, control.getSelectedText(), "selection unchanged after remove");
        // change scene's focusOwner to another node
        Button secondButton = new Button("another dummy");
        showControl(secondButton, true);
        assertEquals(secondWord, control.getSelectedText(), "selection unchanged after focusOwner change in old scene");
        // re-add textField
        root.getChildren().add(control);
        control.requestFocus();
        assertEquals(text, control.getSelectedText(), "selection changed on becoming scene's focusOwner");
    }

    /**
     * Guard against regression of JDK-8116975: activate another stage must not affect
     * selection of textField.
     */
    @Test
    public void testFocusOwnerListenerSecondStage() {
        String firstWord = "some";
        String secondWord = "text";
        String text = firstWord + " " + secondWord;
        TextField control = new TextField(text);
        showControl(control, true);
        Button button = new Button("dummy");
        showControl(button, false);
        control.selectNextWord();
        assertEquals(secondWord, control.getSelectedText(), "sanity: ");

        // build and activate second stage
        VBox secondRoot = new VBox(10, new Button("secondButton"));
        Scene secondScene = new Scene(secondRoot);
        Stage secondStage = new Stage();
        secondStage.setScene(secondScene);
        secondStage.show();
        secondStage.requestFocus();

        try {
            assertTrue(secondStage.isFocused(), "sanity: ");
            assertEquals(secondWord, control.getSelectedText(), "selection unchanged");
            // back to first
            stage.requestFocus();
            assertTrue(stage.isFocused(), "sanity: ");
            assertTrue(control.isFocused(), "sanity: ");
            assertEquals(secondWord, control.getSelectedText(), "selection unchanged");
        } finally {
            // cleanup
            secondStage.hide();
        }
    }

//---------- TextInputControl

//    @Test
//    public void testChildMapsCleared() {
//        TextField control = new TextField("some text");
//        TextFieldBehavior behavior = (TextFieldBehavior) createBehavior(control);
//        InputMap<?> inputMap = behavior.getInputMap();
//        assertFalse(inputMap.getChildInputMaps().isEmpty(), "sanity: inputMap has child maps");
//        behavior.dispose();
//        assertEquals(0, inputMap.getChildInputMaps().size(), "default child maps must be cleared");
//    }

//    @Test
//    public void testDefaultMappingsCleared() {
//        TextField control = new TextField("some text");
//        TextFieldBehavior behavior = (TextFieldBehavior) createBehavior(control);
//        InputMap<?> inputMap = behavior.getInputMap();
//        assertFalse(inputMap.getMappings().isEmpty(), "sanity: inputMap has mappings");
//        behavior.dispose();
//        assertEquals(0, inputMap.getMappings().size(), "default mappings must be cleared");
//    }

    /**
     * Sanity test: mappings to key pad keys.
     */
    @Test
    public void testKeyPadMapping() {
        /*
        TextField control = new TextField("some text");
        TextFieldBehavior behavior = (TextFieldBehavior) createBehavior(control);
        InputMap<?> inputMap = behavior.getInputMap();
        // FIXME: test for all?
        // Here we take one of the expected only - assumption being that
        // if the one is properly registered, it's siblings are handled as well
        KeyCode expectedCode = KeyCode.KP_LEFT;
        KeyMapping expectedMapping = new KeyMapping(expectedCode, null);
        assertTrue(inputMap.getMappings().contains(expectedMapping));
        */
        TextField control = new TextField("some text");
        ControlShim.installDefaultSkin(control);
        KeyCode[] codes = {
            KeyCode.KP_DOWN,
            KeyCode.KP_LEFT,
            KeyCode.KP_RIGHT,
            KeyCode.KP_UP,
        };

        Set<KeyBinding> keys = control.getInputMap().getKeyBindings();
        for (KeyCode c: codes) {
            KeyBinding k = KeyBinding.of(c);
            assertTrue(keys.contains(k));
        }
    }

    /**
     * Sanity test: child mappings to key pad keys.
     */
    /*
    // this test relies on too many assumptions on internals
    @Test
    public void testKeyPadMappingChildInputMap() {
        TextField control = new TextField("some text");
        TextFieldBehavior behavior = (TextFieldBehavior) createBehavior(control);
        InputMap<?> inputMap = behavior.getInputMap();
        // FIXME: test for all?
        // Here we take one of the expected only - assumption being that
        // if the one is properly registered, its siblings are handled as well
        KeyCode expectedCode = KeyCode.KP_LEFT;
        // test os specific child mappings
        InputMap<?> childInputMapMac = inputMap.getChildInputMaps().get(0);
        KeyMapping expectedMac = new KeyMapping(new KeyBinding(expectedCode).shortcut(), null);
        assertTrue(childInputMapMac.getMappings().contains(expectedMac));

        InputMap<?> childInputMapNotMac = inputMap.getChildInputMaps().get(1);
        KeyMapping expectedNotMac = new KeyMapping(new KeyBinding(expectedCode).ctrl(), null);
        assertTrue(childInputMapNotMac.getMappings().contains(expectedNotMac));
    }
    */

    /**
     * Ensures that ctrl- key pad keys are also mapped.
     * This test executes different code path between Mac and non-Mac platforms.
     */
    @Test
    public void testKeyPadMappingOnPlatform() {
        TextField control = new TextField("some text");
        ControlShim.installDefaultSkin(control);
        KeyCode[] codes = {
            KeyCode.KP_LEFT,
            KeyCode.KP_RIGHT,
        };

        Set<KeyBinding> keys = control.getInputMap().getKeyBindings();
        System.out.println(keys);
        for (KeyCode c: codes) {
            if (PlatformUtil.isMac()) {
                KeyBinding expectedMac = KeyBinding.command(c);
                System.out.println(expectedMac);
                assertTrue(keys.contains(expectedMac), "code=" + c);
            } else {
                KeyBinding expectedNotMac = KeyBinding.ctrl(c);
                assertTrue(keys.contains(expectedNotMac), "code=" + c);
            }
        }
    }

    /**
     * Sanity test: listener to textProperty still effective after fix
     * (accidentally added twice)
     */
    // behavior must be added in Skin.install() now
//    @Test
//    public void testTextPropertyListener() {
//        TextField control = new TextField("some text");
//        TextFieldBehavior behavior = (TextFieldBehavior) createBehavior(control);
//        assertNull(getRawBidi(behavior), "sanity: initial bidi");
//        // validate bidi field
//        isRTLText(behavior);
//        assertNotNull(getRawBidi(behavior));
//        control.setText("dummy");
//        assertNull(getRawBidi(behavior), "listener working (bidi is reset)");
//    }

//----------- TreeView

    /**
     * Test cleanup of selection listeners in TreeViewBehavior.
     */
    @Test
    public void testTreeViewBehaviorDisposeSelect() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(treeView));
        treeView.getSelectionModel().select(1);
        weakRef.get().dispose();
        treeView.getSelectionModel().select(0);
        assertNull(treeView.getProperties().get("anchor"), "anchor must remain cleared on selecting when disposed");
    }

    @Test
    public void testTreeViewBehaviorSelect() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        createBehavior(treeView);
        int last = 1;
        treeView.getSelectionModel().select(last);
        assertEquals(last, treeView.getProperties().get("anchor"), "anchor must be set");
    }

    @Test
    public void testTreeViewBehaviorDispose() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(treeView));
        treeView.getSelectionModel().select(1);
        weakRef.get().dispose();
        assertNull(treeView.getProperties().get("anchor"), "anchor must be cleared after dispose");
    }

    /**
     * Creates and returns an expanded treeItem with two children.
     */
    private TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        root.getChildren().addAll(new TreeItem<>("child one"), new TreeItem<>("child two"));
        return root;
    }


// ---------- ListView

    /**
     * Test cleanup of listener to itemsProperty.
     */
    @Test
    public void testListViewBehaviorDisposeSetItems() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        weakRef.get().dispose();
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.setItems(observableArrayList("other", "again"));
        assertEquals(last, listView.getProperties().get("anchor"), "sanity: anchor unchanged");
        listView.getItems().remove(0);
        assertEquals(last, listView.getProperties().get("anchor"), "anchor must not be updated on items modification when disposed");
    }

    @Test
    public void testListViewBehaviorSetItems() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.setItems(observableArrayList("other", "again"));
        assertEquals(last, listView.getProperties().get("anchor"), "sanity: anchor unchanged");
        listView.getItems().remove(0);
        assertEquals(last -1, listView.getProperties().get("anchor"), "anchor must be updated on items modification");
   }

    /**
     * Test cleanup of itemsList listener in ListViewBehavior.
     */
    @Test
    public void testListViewBehaviorDisposeRemoveItem() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        weakRef.get().dispose();
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.getItems().remove(0);
        assertEquals(
            last,
            listView.getProperties().get("anchor"),
            "anchor must not be updated on items modification when disposed");
    }

    @Test
    public void testListViewBehaviorRemoveItem() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        assertEquals(last, listView.getProperties().get("anchor"), "behavior must set anchor on select");
        listView.getItems().remove(0);
        assertEquals(last -1, listView.getProperties().get("anchor"), "anchor must be updated on items modification");
    }

    /**
     * Test cleanup of selection listeners in ListViewBehavior.
     */
    @Test
    public void testListViewBehaviorDisposeSelect() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        listView.getSelectionModel().select(1);
        weakRef.get().dispose();
        listView.getSelectionModel().select(0);
        assertNull(listView.getProperties().get("anchor"), "anchor must remain cleared on selecting when disposed");
    }

    @Test
    public void testListViewBehaviorSelect() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        listView.getSelectionModel().select(last);
        assertEquals(last, listView.getProperties().get("anchor"), "anchor must be set");
    }

    @Test
    public void testListViewBehaviorDispose() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        listView.getSelectionModel().select(1);
        weakRef.get().dispose();
        assertNull(listView.getProperties().get("anchor"), "anchor must be cleared after dispose");
    }

  //------------------ setup/cleanup

    /**
     * Ensures the control is shown and focused in an active scenegraph.
     *
     * @param control the control to show
     */
    protected void showControl(Control control) {
        showControl(control, true);
    }

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     */
    protected void showControl(Control control, boolean focused) {
        if (root == null) {
            root = new VBox();
            scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
        }
        if (!root.getChildren().contains(control)) {
            root.getChildren().add(control);
        }
        stage.show();
        if (focused) {
            stage.requestFocus();
            control.requestFocus();
            assertTrue(control.isFocused());
            assertSame(control, scene.getFocusOwner());
        }
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @BeforeEach
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }
}
