/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Mnemonic;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableMapWrapper;
import com.sun.javafx.event.BasicEventDispatcher;
import com.sun.javafx.scene.traversal.Direction;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;

public final class KeyboardShortcutsHandler extends BasicEventDispatcher {
    private ObservableMap<KeyCombination, Runnable> accelerators;
    private ObservableMap<KeyCombination, ObservableList<Mnemonic>> mnemonics;

    public void addMnemonic(Mnemonic m) {
        ObservableList<Mnemonic> mnemonicsList = (ObservableList)getMnemonics().get(m.getKeyCombination());
        if (mnemonicsList == null) {
            mnemonicsList = new ObservableListWrapper<Mnemonic>(new ArrayList<Mnemonic>());
            getMnemonics().put(m.getKeyCombination(), mnemonicsList);
        }
        boolean foundMnemonic = false;
        for (int i = 0 ; i < mnemonicsList.size() ; i++) {
            if (mnemonicsList.get(i) == m) {
                foundMnemonic = true;
            }
        }
        if (foundMnemonic == false) {
            mnemonicsList.add(m);
        }
    }

    public void removeMnemonic(Mnemonic m) {
        ObservableList<Mnemonic> mnemonicsList = (ObservableList)getMnemonics().get(m.getKeyCombination());
        if (mnemonicsList != null) {
            for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                if (mnemonicsList.get(i).getNode() == m.getNode()) {
                    mnemonicsList.remove(i);
                }
            }
        }
    }

    public ObservableMap<KeyCombination, ObservableList<Mnemonic>> getMnemonics() {
        if (mnemonics == null) {
            mnemonics = new ObservableMapWrapper<KeyCombination, ObservableList<Mnemonic>>(new HashMap<KeyCombination, ObservableList<Mnemonic>>());
        }
        return mnemonics;
    }

    public ObservableMap<KeyCombination, Runnable> getAccelerators() {
        if (accelerators == null) {
            // Note: accelerators are iterated and the Runnable is executed during the iteration.
            // Since the Runnable can manipulate the accelerator list, we need to avoid ConcurrentModificationException in that case
            // Copying the whole list on every traversal (which is on every key press/release) would be constly, so
            // we use ConcurrentHashMap, which has iterators with weak consitency (it may or may not contain the changes done during the iteration)
            // instead of fail-fast behavior. This does not mean accelerators is really concurrent though, the wrapper still does not support multi-threaded access.
            accelerators = new ObservableMapWrapper<>(new ConcurrentHashMap<>());
        }
        return accelerators;
    }

    private void traverse(Event event, Node node, Direction dir) {
        if (node.impl_traverse(dir)) {
            event.consume();
        }
    }

    public void processTraversal(Event event) {
        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED) {
            if (!((KeyEvent)event).isMetaDown() && !((KeyEvent)event).isControlDown() && !((KeyEvent)event).isAltDown()) {
                Object obj = event.getTarget();
                if (obj instanceof Node) {
                
                    switch (((KeyEvent)event).getCode()) {
                      case TAB :
                          if (((KeyEvent)event).isShiftDown()) {
                              traverse(event, ((Node)obj), com.sun.javafx.scene.traversal.Direction.PREVIOUS);
                          }
                          else {
                              traverse(event, ((Node)obj), com.sun.javafx.scene.traversal.Direction.NEXT);
                          }
                          break;
                      case UP :
                          traverse(event, ((Node) obj), com.sun.javafx.scene.traversal.Direction.UP);
                          break;
                      case DOWN :
                          traverse(event, ((Node) obj), com.sun.javafx.scene.traversal.Direction.DOWN);
                          break;
                      case LEFT :
                          traverse(event, ((Node) obj), com.sun.javafx.scene.traversal.Direction.LEFT);
                          break;
                      case RIGHT :
                          traverse(event, ((Node) obj), com.sun.javafx.scene.traversal.Direction.RIGHT);
                          break;
                      default :
                          break;
                    }
                }
            }
        }
    }

    @Override
    public Event dispatchBubblingEvent(Event event) {
        /*
        ** If the key event hasn't been consumed then
        ** we will process global events in the order :
        **    . Mnemonics,
        **    . Accelerators,
        **    . Navigation.
        ** This processing is extra to that of listeners and
        ** the focus Node.
        */
        if (event.getEventType() == KeyEvent.KEY_PRESSED) {
            if (PlatformUtil.isMac()) {
                if (((KeyEvent)event).isMetaDown()) {
                    processMnemonics((KeyEvent)event);
                }
            } else {
                if (((KeyEvent)event).isAltDown() || isMnemonicsDisplayEnabled()) {
                    processMnemonics((KeyEvent)event);
                }
            }

            if (!event.isConsumed()) {
                processAccelerators((KeyEvent)event);
            }

            if (!event.isConsumed()) {
                processTraversal(event);
            }
        }

        /*
        ** if we're not on mac, and nobody consumed the event, then we should
        ** check to see if we should highlight the mnemonics on the scene
        */
        if (!PlatformUtil.isMac()) {
            if (event.getEventType() == KeyEvent.KEY_PRESSED) {
                if (((KeyEvent)event).isAltDown()  && !event.isConsumed()) {
                    /*
                    ** show mnemonics while alt is held
                    */
                    if (!isMnemonicsDisplayEnabled()) {
                        setMnemonicsDisplayEnabled(true);
                    }
                    else {
                        if (PlatformUtil.isWindows()) {
                            setMnemonicsDisplayEnabled(!isMnemonicsDisplayEnabled());
                        }
                    }
                }
            }
            if (event.getEventType() == KeyEvent.KEY_RELEASED) {
                if (!((KeyEvent)event).isAltDown()) {
                    if (!PlatformUtil.isWindows()) {
                        setMnemonicsDisplayEnabled(false);
                    }
                }
            }
        }
        return event;
    }

    private void processMnemonics(KeyEvent event) {
        if (mnemonics != null) {

            ObservableList<Mnemonic> mnemonicsList = null;

            for (Map.Entry<KeyCombination, ObservableList<Mnemonic>>
                    mnemonic: mnemonics.entrySet()) {

                if (!isMnemonicsDisplayEnabled()) {
                    if (mnemonic.getKey().match(event)) {
                        mnemonicsList = (ObservableList) mnemonic.getValue();
                        break;
                    }
                }
                else {
                    /*
                    ** Mnemonics display has been enabled, which means
                    ** we act as is the alt key is being held down.
                    */

                    KeyEvent fakeEvent = new KeyEvent(null, event.getTarget(), KeyEvent.KEY_PRESSED,
                                                                event.getCharacter(),
                                                                event.getText(),
                                                                event.getCode(),
                                                                event.isShiftDown(),
                                                                event.isControlDown(),
                                                                true,
                                                                event.isMetaDown());


                    if (mnemonic.getKey().match(fakeEvent)) {
                        mnemonicsList = (ObservableList) mnemonic.getValue();
                        break;
                    }
                }
            }

            if (mnemonicsList != null) {
                /*
                ** for mnemonics we need to check if visible and reachable....
                ** if a single Mnemonic on the keyCombo we
                ** fire the runnable in Mnemoninic, and transfer focus
                ** if there is more than one then we just
                ** transfer the focus
                **
                */
                boolean multipleNodes = false;
                Node firstNode = null;
                Mnemonic firstMnemonics = null;
                int focusedIndex = -1;
                int nextFocusable = -1;

                /*
                ** find first focusable node
                */
                for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                    if (mnemonicsList.get(i) instanceof Mnemonic) {
                        Node currentNode = (Node)mnemonicsList.get(i).getNode();

                        if (firstMnemonics == null && (currentNode.impl_isTreeVisible() && !currentNode.isDisabled())) {
                            firstMnemonics = mnemonicsList.get(i);
                        }

                        if (currentNode.impl_isTreeVisible() && (currentNode.isFocusTraversable() && !currentNode.isDisabled())) {
                            if (firstNode == null) {
                                firstNode = currentNode;
                            }
                            else {
                                /*
                                ** there is more than one node on this keyCombo
                                */
                                multipleNodes = true;
                                if (focusedIndex != -1) {
                                    if (nextFocusable == -1) {
                                        nextFocusable = i;
                                    }
                                }
                            }
                        }
                        /*
                        ** one of our targets has the focus already
                        */
                        if (currentNode.isFocused()) {
                            focusedIndex = i;
                        }
                    }
                }

                if (firstNode != null) {
                    if (!multipleNodes == true) {
                        /*
                        ** just one target
                        */
                        firstNode.requestFocus();
                        event.consume();
                    }
                    else {
                        /*
                        ** we have multiple nodes using the same mnemonic.
                        ** this is allowed for nmemonics, and we simple
                        ** focus traverse between them
                        */
                        if (focusedIndex == -1) {
                            firstNode.requestFocus();
                            event.consume();
                        }
                        else {
                            if (focusedIndex >= mnemonicsList.size()) {
                                firstNode.requestFocus();
                                event.consume();
                            }
                            else {
                                if (nextFocusable != -1) {
                                    ((Node)mnemonicsList.get(nextFocusable).getNode()).requestFocus();
                                }
                                else {
                                    firstNode.requestFocus();
                                }
                                event.consume();
                            }
                        }
                    }
                }
                if (!multipleNodes && firstMnemonics != null) {
                    firstMnemonics.fire();
                }
            }
        }
    }

    private void processAccelerators(KeyEvent event) {
        if (accelerators != null) {
            for (Map.Entry<KeyCombination, Runnable>
                     accelerator: accelerators.entrySet()) {
                
                if (accelerator.getKey().match(event)) {
                    Runnable acceleratorRunnable = accelerator.getValue();
                    if (acceleratorRunnable != null) {
                        /*
                        ** for accelerators there can only be one target
                        ** and we don't care whether it's visible or reachable....
                        ** we just run the Runnable.......
                        */
                        acceleratorRunnable.run();
                        event.consume();
                    }
                }
            }
        }
    }

    private void processMnemonicsKeyDisplay() {
        ObservableList<Mnemonic> mnemonicsList = null;
        if (mnemonics != null) {
            for (Map.Entry<KeyCombination, ObservableList<Mnemonic>> mnemonic: mnemonics.entrySet()) {
                mnemonicsList = (ObservableList) mnemonic.getValue();
         
                if (mnemonicsList != null) {
                    for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                        Node currentNode = (Node)mnemonicsList.get(i).getNode();
                        currentNode.impl_setShowMnemonics(mnemonicsDisplayEnabled);
                    }
                }
            }
        }
    }

    /*
    ** remember if the alt key is being held
    */
    private boolean mnemonicsDisplayEnabled = false;

    public boolean isMnemonicsDisplayEnabled() {
        return mnemonicsDisplayEnabled;
    }
    public void setMnemonicsDisplayEnabled(boolean b) {
        if (b != mnemonicsDisplayEnabled) {
            mnemonicsDisplayEnabled = b;
            processMnemonicsKeyDisplay();
        }
    }

    public void clearNodeMnemonics(Node node) {
        if (mnemonics != null) {
            for (ObservableList<Mnemonic> list : mnemonics.values()) {
                for (Iterator<Mnemonic> it = list.iterator(); it.hasNext(); ) {
                    Mnemonic m = it.next();
                    if (m.getNode() == node) {
                        it.remove();
                    }
                }
            }
        }
    }
}
