/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.lang.ref.WeakReference;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyEvent;
import javafx.scene.Node;

import com.sun.javafx.event.BasicEventDispatcher;
import com.sun.javafx.event.CompositeEventDispatcher;
import com.sun.javafx.event.EventDispatchChainImpl;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;

/**
 * A focus delegating control delegates key and input method events
 * to a descendant node.
 *
 * The base class automatically manages the onInputMethodTextChanged and
 * inputMethodRequests properties. Derived classes should not set these
 * properties.
 *
 * @since 28
 */
public class FocusDelegatingControl extends Control {

    private WeakReference<Node> delegate;

    /**
     *  Create a new FocusDelegatingControl.
     */
    protected FocusDelegatingControl() {
        var dispatcher = getEventDispatcher();
        if (dispatcher instanceof BasicEventDispatcher) {
            var targetChanger = new TargetChangingDispatcher(this);
            var comp = new CompositeTargetChangingDispatcher((BasicEventDispatcher)dispatcher, targetChanger);
            setEventDispatcher(comp);
        }
    }

    /**
     * Set the delegate. May be null if there is no delegate.
     * @param delegate the new control to delegate to.
     */
    protected final void setFocusDelegate(Node delegate) {
        var oldDelegate = getDelegate();
        if (oldDelegate != delegate) {
            if (oldDelegate != null) {
                oldDelegate.inputMethodRequestsProperty().removeListener(weakInputMethodRequestsChangedListener);
                oldDelegate.onInputMethodTextChangedProperty().removeListener(weakOnInputMethodTextChangedListener);
            }
            if (delegate != null) {
                this.delegate = new WeakReference<>(delegate);
                delegate.inputMethodRequestsProperty().addListener(weakInputMethodRequestsChangedListener);
                delegate.onInputMethodTextChangedProperty().addListener(weakOnInputMethodTextChangedListener);
            } else {
                this.delegate = null;
                setOnInputMethodTextChanged(null);
                setInputMethodRequests(null);
            }
        }
    }

    /**
     * Get the delegate. May be null.
     * @return the current delegate
     */
    protected Node getDelegate() {
        if (delegate == null) return null;
        return delegate.get();
    }

    /**
     * Derived classes should override this to detemine whether a KeyEvent
     * should be fired at the delegate or the control. By default all
     * KeyEvents are fired at the control.
     *
     * @param event the key event
     * @return true if the key event should be sent to the delegate
     */
    protected boolean shouldDelegateEvent(KeyEvent event) {
        return false;
    }

    /**
     * Internal implementation
     */

    /**
     * If the delegate can enable InputMethodEvents than the control
     * can also. The base class sets the necessary properties and
     * forwards IM events to the delegate.
     */
    private final ChangeListener<InputMethodRequests> inputMethodRequestsChangedListener =
        (obs, old, current) -> updateInputMethodRequests(current);
    private final ChangeListener<EventHandler<? super InputMethodEvent>> onInputMethodTextChangedListener =
        (obs, old, current) -> updateOnInputMethodChanged(current);

    private final WeakChangeListener<InputMethodRequests> weakInputMethodRequestsChangedListener =
        new WeakChangeListener(inputMethodRequestsChangedListener);
    private final WeakChangeListener<EventHandler<? super InputMethodEvent>> weakOnInputMethodTextChangedListener =
        new WeakChangeListener(onInputMethodTextChangedListener);


    /**
     * By default the control builds dispatch chains that terminate at the
     * control. The first dispatcher determines whether the event should be
     * delegated instead. If so it builds a new chain that terminates at the
     * delegate.
     */
    private class DelegatingDispatcher implements EventDispatcher {
        private FocusDelegatingControl control;

        public DelegatingDispatcher(FocusDelegatingControl control) {
            this.control = control;
        }

        @Override
        public Event dispatchEvent(Event event, EventDispatchChain tail) {
            var delegate = control.getDelegate();

            // Neither of these should happen but just to be safe
            if (delegate == null || delegate == control) {
                return tail.dispatchEvent(event);
            }

            boolean doDelegate = false;
            if (event instanceof InputMethodEvent && delegate.getOnInputMethodTextChanged() != null) {
                doDelegate = true;
            } else if (event instanceof KeyEvent) {
                doDelegate = control.shouldDelegateEvent((KeyEvent)event);
            }
            if (!doDelegate) {
                return tail.dispatchEvent(event);
            }

            // If this isn't targeted at the control someone must be trying
            // to bypass the control by firing an event at the delegate.
            if (event.getTarget() != control) {
                event.consume();
                return event;
            }

            // Send this to the delegate with the target set to
            // the control.
            var delegateChain = delegate.buildEventDispatchChain(new EventDispatchChainImpl());
            event = event.copyFor(event.getSource(), control);
            return delegateChain.dispatchEvent(event);
        }
    }

    /**
     * When an event is dispatched to the delegate its target will initially
     * be the control. When it reaches the part of the dispatch chain after
     * the control we re-write the target to be the delegate.
     */
    private class TargetChangingDispatcher extends BasicEventDispatcher {
        private FocusDelegatingControl control;

        public TargetChangingDispatcher(FocusDelegatingControl control) {
            this.control = control;
        }

        @Override
        public Event dispatchCapturingEvent(Event event) {
            var delegate = control.getDelegate();
            if (delegate != null) {
                if (event instanceof InputMethodEvent || event instanceof KeyEvent) {
                    // Update the event's target to be the delegate
                    return event.copyFor(event.getSource(), delegate);
                }
            }
            return event;
        }

        @Override
        public Event dispatchBubblingEvent(Event event) {
            var delegate = control.getDelegate();
            if (delegate != null) {
                if (event instanceof InputMethodEvent || event instanceof KeyEvent) {
                    // Update the event's target to be the control
                    return event.copyFor(event.getSource(), control);
                }
            }
            return event;
        }
    }

    private class CompositeTargetChangingDispatcher extends CompositeEventDispatcher {
        private final BasicEventDispatcher originalDispatcher;
        private final BasicEventDispatcher targetChangingDispatcher;

        public CompositeTargetChangingDispatcher(
                final BasicEventDispatcher originalDispatcher,
                final BasicEventDispatcher targetChangingDispatcher) {
            this.originalDispatcher = originalDispatcher;
            this.targetChangingDispatcher = targetChangingDispatcher;

            originalDispatcher.insertNextDispatcher(targetChangingDispatcher);
        }

        @Override
        public BasicEventDispatcher getFirstDispatcher() {
            return originalDispatcher;
        }

        @Override
        public BasicEventDispatcher getLastDispatcher() {
            return targetChangingDispatcher;
        }
    }

    /**
     */
    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        var chain = super.buildEventDispatchChain(tail);
        var delegate = getDelegate();
        if (delegate != null && delegate != this) {
            chain = chain.prepend(new DelegatingDispatcher(this));
        }
        return chain;
    }

    /**
     * Forward input method requests from the control to the delegate
     */
    private void updateInputMethodRequests(InputMethodRequests requests) {
        if (requests instanceof ExtendedInputMethodRequests) {
            setInputMethodRequests(new ExtendedInputMethodRequests() {
                @Override public Point2D getTextLocation(int offset) {
                    return requests.getTextLocation(offset);
                }

                @Override public int getLocationOffset(int x, int y) {
                    return requests.getLocationOffset(x, y);
                }

                @Override public void cancelLatestCommittedText() {
                    requests.cancelLatestCommittedText();
                }

                @Override public String getSelectedText() {
                    return requests.getSelectedText();
                }

                @Override public int getInsertPositionOffset() {
                    return ((ExtendedInputMethodRequests)requests).getInsertPositionOffset();
                }

                @Override public String getCommittedText(int begin, int end) {
                    return ((ExtendedInputMethodRequests)requests).getCommittedText(begin, end);
                }

                @Override public int getCommittedTextLength() {
                    return ((ExtendedInputMethodRequests)requests).getCommittedTextLength();
                }
            });
        } else if (requests instanceof InputMethodRequests) {
            setInputMethodRequests(new InputMethodRequests() {
                @Override public Point2D getTextLocation(int offset) {
                    return requests.getTextLocation(offset);
                }

                @Override public int getLocationOffset(int x, int y) {
                    return requests.getLocationOffset(x, y);
                }

                @Override public void cancelLatestCommittedText() {
                    requests.cancelLatestCommittedText();
                }

                @Override public String getSelectedText() {
                    return requests.getSelectedText();
                }
            });
        } else {
            setInputMethodRequests(null);
        }
    }

    /**
     * If the delegate handles input method change events than the control
     * can, too. We just need to set this property to non-null to signal
     * that the control handles these events. The actual events are sent
     * directly to the delegate.
     */
    private void updateOnInputMethodChanged(EventHandler<? super InputMethodEvent> handler) {
        if (handler == null) {
            setOnInputMethodTextChanged(null);
        } else {
            setOnInputMethodTextChanged(event -> {
            });
        }
    }
}