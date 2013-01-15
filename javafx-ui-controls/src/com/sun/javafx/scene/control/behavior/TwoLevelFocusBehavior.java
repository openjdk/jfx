/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.PseudoClass;;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.PopupControl;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import com.sun.javafx.css.StyleManager;

/**
 * A two level focus handler allows a Control to behave as if it
 * has three focus states :
 *  - not focused
 *  - focused with internal focus
 *  - focused with external focus
 *
 * In external focus mode it intercepts focus and traversal events and
 * prevents the Controls acting upon them, or trapping focus.
 * In internal focus mode most events go to the Control, except
 * for events that are defined to exit the mode.
 */
public class TwoLevelFocusBehavior {

    Node tlNode = null;
    PopupControl tlPopup = null;
    EventDispatcher origEventDispatcher = null;

    public TwoLevelFocusBehavior() {
    }

    public TwoLevelFocusBehavior(Node node) {

        tlNode = node;
        tlPopup = null;

        tlNode.addEventHandler(KeyEvent.ANY, keyEventListener);
        tlNode.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventListener);
        tlNode.focusedProperty().addListener(focusListener);

        // block ScrollEvent from being passed down to scrollbar's skin
        origEventDispatcher = tlNode.getEventDispatcher();
        tlNode.setEventDispatcher(tlfEventDispatcher);
    }


    /*
    ** don't allow the Node to handle a key event if it is in externalFocus mode.
    ** the only keyboard actions allowed are the navigation keys......
    */ 
    final EventDispatcher preemptiveEventDispatcher = new EventDispatcher() {
        @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {

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
                                  ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.PREVIOUS);
                              }
                              else {
                                  ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.NEXT);
                              }
                              event.consume();
                              break;
                          case UP :
                              ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.UP);
                              event.consume();
                              break;
                          case DOWN :
                              ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.DOWN);
                              event.consume();
                              break;
                          case LEFT :
                              ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.LEFT);
                              event.consume();
                              break;
                          case RIGHT :
                              ((Node)obj).impl_traverse(com.sun.javafx.scene.traversal.Direction.RIGHT);
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
        }
    };

    final EventDispatcher tlfEventDispatcher = new EventDispatcher() {
           @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {

               if ((event instanceof KeyEvent)) {
                   if (isExternalFocus()) {
                       tail = tail.prepend(preemptiveEventDispatcher);
                       return tail.dispatchEvent(event);
                   }
               }
               return origEventDispatcher.dispatchEvent(event, tail);
           }
        };

    private Event postDispatchTidyup(Event event) {
        // block the event from being passed down to children
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


    private final EventHandler<KeyEvent> keyEventListener = new EventHandler<KeyEvent>() {
        @Override public void handle(KeyEvent e) {
            postDispatchTidyup(e);
        }
    };


    /*
    **  When a node gets focus, put it in external-focus mode.
    */
    final ChangeListener<Boolean> focusListener = new ChangeListener<Boolean>() {
        @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldVal, Boolean newVal) {
            if (newVal && tlPopup != null) {
                setExternalFocus(false);
            }
            else {
                setExternalFocus(true);
            }
        }
    };

    private final EventHandler<MouseEvent> mouseEventListener  = new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent e) {
            setExternalFocus(false);
        }
    };

    private boolean externalFocus = true;

    public boolean isExternalFocus() {
        return externalFocus;
    }

    private static final PseudoClass INTERNAL_PSEUDOCLASS_STATE = 
            PseudoClass.getPseudoClass("internal-focus");
    private static final PseudoClass EXTERNAL_PSEUDOCLASS_STATE = 
            PseudoClass.getPseudoClass("external-focus");

    public void setExternalFocus(boolean value) {
        externalFocus = value;

        if (tlNode != null && tlNode instanceof Control) {
            ((Control)tlNode).pseudoClassStateChanged(INTERNAL_PSEUDOCLASS_STATE, !value);
            ((Control)tlNode).pseudoClassStateChanged(EXTERNAL_PSEUDOCLASS_STATE,  value);
        }
        else if (tlPopup != null) {
            tlPopup.pseudoClassStateChanged(INTERNAL_PSEUDOCLASS_STATE, !value);
            tlPopup.pseudoClassStateChanged(EXTERNAL_PSEUDOCLASS_STATE,  value);
        }
    }

    Node getTwoLevelFocusNode() {
        return tlNode;
    }

}
