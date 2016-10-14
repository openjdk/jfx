/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import com.sun.javafx.scene.NodeHelper;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;


public class TwoLevelFocusPopupBehavior extends TwoLevelFocusBehavior {

    public TwoLevelFocusPopupBehavior(PopupControl popup) {

        tlPopup = popup;

        setExternalFocus(false);  // popups go straight to internal focus

        tlPopup.addEventHandler(KeyEvent.ANY, keyEventListener);
        tlPopup.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventListener);
        tlPopup.focusedProperty().addListener(focusListener);

        // block ScrollEvent from being passed down to scrollbar's skin
        origEventDispatcher = tlPopup.getEventDispatcher();

        tlPopup.setEventDispatcher(tlfEventDispatcher);
    }


    public TwoLevelFocusPopupBehavior(Node node) {

        tlNode = node;

        setExternalFocus(false);  // popups go straight to internal focus

        // listen to all keyevents, maybe
        tlNode.addEventHandler(KeyEvent.ANY, keyEventListener);
        tlNode.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventListener);
        tlNode.focusedProperty().addListener(focusListener);

        // block ScrollEvent from being passed down to scrollbar's skin
        origEventDispatcher = tlNode.getEventDispatcher();

        tlNode.setEventDispatcher(tlfEventDispatcher);
    }

    /**
     * Invoked by the behavior when it is disposed, so that any listeners installed by
     * the TwoLevelFocusBehavior can also be uninstalled
     */
    public void dispose() {
        tlNode.removeEventHandler(KeyEvent.ANY, keyEventListener);
        tlNode.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventListener);
        tlNode.focusedProperty().removeListener(focusListener);
        tlNode.setEventDispatcher(origEventDispatcher);
    }

    /*
    ** don't allow the Node to handle a key event if it is in externalFocus mode.
    ** the only keyboard actions allowed are the navigation keys......
    */
    final EventDispatcher preemptiveEventDispatcher = (event, tail) -> {

        // block the event from being passed down to children
        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED) {
            if (!((KeyEvent)event).isMetaDown() && !((KeyEvent)event).isControlDown()  && !((KeyEvent)event).isAltDown()) {
                if (isExternalFocus()) {
                    //
                    // don't let the behaviour leak any navigation keys when
                    // we're not in blocking mode....
                    //
                    Object obj = event.getTarget();

                    switch (((KeyEvent)event).getCode()) {
                      case TAB :
                          if (((KeyEvent)event).isShiftDown()) {
                              NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.PREVIOUS);
                          }
                          else {
                              NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.NEXT);
                          }
                          event.consume();
                          break;
                      case UP :
                          NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.UP);
                          event.consume();
                          break;
                      case DOWN :
                          NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.DOWN);
                          event.consume();
                          break;
                      case LEFT :
                          NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.LEFT);
                          event.consume();
                          break;
                      case RIGHT :
                          NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.RIGHT);
                          event.consume();
                          break;
                      case ENTER :
                          setExternalFocus(false);
                          event.consume();
                          break;
                      default :
                          // this'll kill mnemonics.... unless!
                          Scene s = tlNode.getScene();
                          Event.fireEvent(s, event);
                          event.consume();
                          break;
                    }
                }
            }
        }
        return event;
    };
    final EventDispatcher preemptivePopupEventDispatcher = (event, tail) -> {

        // block the event from being passed down to children
        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED) {
            if (!((KeyEvent)event).isMetaDown() && !((KeyEvent)event).isControlDown()  && !((KeyEvent)event).isAltDown()) {
                if (!isExternalFocus()) {
                    //
                    // don't let the behaviour leak any navigation keys when
                    // we're not in blocking mode....
                    //
                    Object obj = event.getTarget();
                    switch (((KeyEvent)event).getCode()) {
                      case TAB :
                      case ENTER :
                          event.consume();
                          break;
                      case UP :
                      case DOWN :
                          break;
                      case LEFT :
                          if (obj instanceof Node) {
                              NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.LEFT);
                              event.consume();
                          }
                          else if (obj instanceof Scene) {
                              Node node = ((Scene)obj).getFocusOwner();
                              if (node != null) {
                                  NodeHelper.traverse(node, com.sun.javafx.scene.traversal.Direction.LEFT);
                                  event.consume();
                              }
                          }
                          break;
                      case RIGHT :
                          if (obj instanceof Node) {
                              NodeHelper.traverse((Node) obj, com.sun.javafx.scene.traversal.Direction.RIGHT);
                              event.consume();
                          }
                          else if (obj instanceof Scene) {
                              Node node = ((Scene)obj).getFocusOwner();
                              if (node != null) {
                                  NodeHelper.traverse(node, com.sun.javafx.scene.traversal.Direction.RIGHT);
                                  event.consume();
                              }
                          }
                          break;

                      default :
                          // this'll kill mnemonics.... unless!
                          Scene s = null;
                          if (tlNode != null) {
                              s = tlNode.getScene();
                              Event.fireEvent(s, event);
                          }
                          event.consume();
                          break;
                    }
                }
            }
        }
        return event;
    };


    final EventDispatcher tlfEventDispatcher = (event, tail) -> {

        if ((event instanceof KeyEvent)) {

            if (isExternalFocus()) {
                tail = tail.prepend(preemptiveEventDispatcher);
                return tail.dispatchEvent(event);
            }
            else {
                tail = tail.prepend(preemptivePopupEventDispatcher);
                tail = tail.prepend(origEventDispatcher);
                return tail.dispatchEvent(event);
            }
        }
        return origEventDispatcher.dispatchEvent(event, tail);
    };

    private Event postDispatchTidyup(Event event) {

        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED) {
            if (!isExternalFocus()) {
                //
                // don't let the behaviour leak any navigation keys when
                // we're not in blocking mode....
                //

                if (!((KeyEvent)event).isMetaDown() && !((KeyEvent)event).isControlDown()  && !((KeyEvent)event).isAltDown()) {

                    switch (((KeyEvent)event).getCode()) {
                      case TAB :
                      case UP :
                      case DOWN :
                      case LEFT :
                      case RIGHT :
                          event.consume();
                          break;

                      case ENTER :
                          setExternalFocus(true);
                          event.consume();
                          break;
                      default :
                          break;
                    }
                }
            }
        }
        return event;
    }


    private final EventHandler<KeyEvent> keyEventListener = e -> {
        postDispatchTidyup(e);
    };


    /*
    **  When a node gets focus, put it in external-focus mode.
    */
    final ChangeListener<Boolean> focusListener = (observable, oldVal, newVal) -> {
    };

    private final EventHandler<MouseEvent> mouseEventListener  = e -> {
        setExternalFocus(false);
    };
}
