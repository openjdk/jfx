/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing.oldimpl;

import com.sun.javafx.embed.swing.CachingTransferable;
import com.sun.javafx.embed.swing.FXDnD;
import com.sun.javafx.embed.swing.FXDnDInterop;
import com.sun.javafx.embed.swing.SwingDnD;
import com.sun.javafx.embed.swing.SwingEvents;
import com.sun.javafx.embed.swing.SwingNodeHelper;
import com.sun.javafx.tk.Toolkit;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.SecondaryLoop;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.dnd.peer.DropTargetContextPeer;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import sun.awt.AWTAccessor;
import sun.awt.dnd.SunDragSourceContextPeer;
import sun.swing.JLightweightFrame;

public class FXDnDInteropO extends FXDnDInterop {

    public Component findComponentAt(Object frame, int x, int y,
                                              boolean ignoreEnabled) {
        JLightweightFrame lwFrame = (JLightweightFrame) frame;
        return AWTAccessor.getContainerAccessor().findComponentAt(lwFrame,
                        x, y, false);
    }

    public boolean isCompEqual(Component c, Object frame) {
        JLightweightFrame lwFrame = (JLightweightFrame) frame;
        return c != lwFrame;
    }

    public int convertModifiersToDropAction(int modifiers,
                                                     int supportedActions) {
        return SunDragSourceContextPeer.convertModifiersToDropAction(modifiers,
                        supportedActions);
    }

    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
                        DragSource ds, Component c, int srcActions,
                           DragGestureListener dgl) {
        return (T) new FXDragGestureRecognizer(ds, c, srcActions, dgl);
    }

    public DragSourceContextPeer createDragSourceContext(DragGestureEvent dge)
            throws InvalidDnDOperationException {
        return new FXDragSourceContextPeer(dge);
    }

    private void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private SwingNode getNode() {
        return node;
    }

    public void setNode(SwingNode swnode) {
        node = swnode;
    }

    private SwingNode node = null;

    /**
     * Utility class that operates on Maps with Components as keys.
     * Useful when processing mouse events to choose an object from the map
     * based on the component located at the given coordinates.
     */
    private class ComponentMapper<T> {
        public int x, y;
        public T object = null;

        private ComponentMapper(Map<Component, T> map, int xArg, int yArg) {
            this.x = xArg;
            this.y = yArg;

            final JLightweightFrame lwFrame = (JLightweightFrame)SwingNodeHelper.getLightweightFrame(node);
            Component c = AWTAccessor.getContainerAccessor().findComponentAt(lwFrame, x, y, false);
            if (c == null) return;

            synchronized (c.getTreeLock()) {
                do {
                    object = map.get(c);
                } while (object == null && (c = c.getParent()) != null);

                if (object != null) {
                    // The object is either a DropTarget or a DragSource, so:
                    //assert c == object.getComponent();

                    // Translate x, y from lwFrame to component coordinates
                    while (c != lwFrame && c != null) {
                        x -= c.getX();
                        y -= c.getY();
                        c = c.getParent();
                    }
                }
            }
        }
    }

    public <T> ComponentMapper<T> mapComponent(Map<Component, T> map, int x, int y) {
        return new ComponentMapper<T>(map, x, y);
    }

    ///////////////////////////////////////////////////////////////////////////
    //     DRAG SOURCE IMPLEMENTATION
    ///////////////////////////////////////////////////////////////////////////


    private boolean isDragSourceListenerInstalled = false;

    // To keep track of where the DnD gesture actually started
    private MouseEvent pressEvent = null;
    private long pressTime = 0;

    private volatile SecondaryLoop loop;

    private final Map<Component, FXDragGestureRecognizer> recognizers = new HashMap<>();

    // Note that we don't really use the MouseDragGestureRecognizer facilities,
    // however some code in JDK may expect a descendant of this class rather
    // than a generic DragGestureRecognizer. So we inherit from it.
    private class FXDragGestureRecognizer extends MouseDragGestureRecognizer {
        FXDragGestureRecognizer(DragSource ds, Component c, int srcActions,
            DragGestureListener dgl)
        {
            super(ds, c, srcActions, dgl);

            if (c != null) recognizers.put(c, this);
        }

        @Override public void setComponent(Component c) {
            final Component old = getComponent();
            if (old != null) recognizers.remove(old);
            super.setComponent(c);
            if (c != null) recognizers.put(c, this);
        }

        protected void registerListeners() {
            runOnFxThread(() -> {
                if (!isDragSourceListenerInstalled) {
                    node.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressHandler);
                    node.addEventHandler(MouseEvent.DRAG_DETECTED, onDragStartHandler);
                    node.addEventHandler(DragEvent.DRAG_DONE, onDragDoneHandler);

                    isDragSourceListenerInstalled = true;
                }
            });
        }

        protected void unregisterListeners() {
            runOnFxThread(() -> {
                if (isDragSourceListenerInstalled) {
                    node.removeEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressHandler);
                    node.removeEventHandler(MouseEvent.DRAG_DETECTED, onDragStartHandler);
                    node.removeEventHandler(DragEvent.DRAG_DONE, onDragDoneHandler);

                    isDragSourceListenerInstalled = false;
                }
            });
        }

        private void fireEvent(int x, int y, long evTime, int modifiers) {
            // In theory we should register all the events that trigger the gesture (like PRESS, DRAG, DRAG, BINGO!)
            // But we can live with this hack for now.
            appendEvent(new java.awt.event.MouseEvent(getComponent(), java.awt.event.MouseEvent.MOUSE_PRESSED,
                        evTime, modifiers, x, y, 0, false));

            // Also, the modifiers here should've actually come from the last known mouse event (last MOVE or DRAG).
            // But we're OK with using the initial PRESS modifiers for now
            int initialAction = SunDragSourceContextPeer.convertModifiersToDropAction(
                    modifiers, getSourceActions());

            fireDragGestureRecognized(initialAction, new java.awt.Point(x, y));
        }
    }

    // Invoked on EDT
    private void fireEvent(int x, int y, long evTime, int modifiers) {
        ComponentMapper<FXDragGestureRecognizer> mapper = mapComponent(recognizers, x, y);

        final FXDragGestureRecognizer r = mapper.object;
        if (r != null) {
            r.fireEvent(mapper.x, mapper.y, evTime, modifiers);
        } else {
            // No recognizer, no DnD, no startDrag, so release the FX loop now
            SwingNodeHelper.leaveFXNestedLoop(this);
        }
    }

    private MouseEvent getInitialGestureEvent() {
        return pressEvent;
    }

    private final EventHandler<MouseEvent> onMousePressHandler = (event) -> {
        // It would be nice to maintain a list of all the events that initiate
        // a DnD gesture (see a comment in FXDragGestureRecognizer.fireEvent().
        // For now, we simply use the initial PRESS event for this purpose.
        pressEvent = event;
        pressTime = System.currentTimeMillis();
    };


    private volatile FXDragSourceContextPeer activeDSContextPeer;

    private final EventHandler<MouseEvent> onDragStartHandler = (event) -> {
        // Call to AWT and determine the active DragSourceContextPeer
        activeDSContextPeer = null;
        final MouseEvent firstEv = getInitialGestureEvent();
        SwingNodeHelper.runOnEDTAndWait(FXDnDInteropO.this, () -> fireEvent(
                    (int)firstEv.getX(), (int)firstEv.getY(), pressTime,
                    SwingEvents.fxMouseModsToMouseMods(firstEv)));
        if (activeDSContextPeer == null) return;

        // Since we're going to start DnD, consume the event.
        event.consume();

        Dragboard db = getNode().startDragAndDrop(SwingDnD.dropActionsToTransferModes(
                    activeDSContextPeer.sourceActions).toArray(new TransferMode[1]));

        // At this point the activeDSContextPeer.transferable contains all the data from AWT
        Map<DataFormat, Object> fxData = new HashMap<>();
        for (String mt : activeDSContextPeer.transferable.getMimeTypes()) {
            DataFormat f = DataFormat.lookupMimeType(mt);
            //TODO: what to do if f == null?
            if (f != null) fxData.put(f, activeDSContextPeer.transferable.getData(mt));
        }

        final boolean hasContent = db.setContent(fxData);
        if (!hasContent) {
            // No data, no DnD, no onDragDoneHandler, so release the AWT loop now
            if (!FXDnD.fxAppThreadIsDispatchThread) {
                loop.exit();
            }
        }
    };

    private final EventHandler<DragEvent> onDragDoneHandler = (event) -> {
        event.consume();

        // Release FXDragSourceContextPeer.startDrag()
        if (!FXDnD.fxAppThreadIsDispatchThread) {
            loop.exit();
        }

        if (activeDSContextPeer != null) {
            final TransferMode mode = event.getTransferMode();
            activeDSContextPeer.dragDone(
                    mode == null ? 0 : SwingDnD.transferModeToDropAction(mode),
                    (int)event.getX(), (int)event.getY());
        }
    };


    private final class FXDragSourceContextPeer extends SunDragSourceContextPeer {
        private volatile int sourceActions = 0;

        private final CachingTransferable transferable = new CachingTransferable();

        @Override public void startSecondaryEventLoop(){
            Toolkit.getToolkit().enterNestedEventLoop(this);
        }
        @Override public void quitSecondaryEventLoop(){
            assert !Platform.isFxApplicationThread();
            Platform.runLater(() -> Toolkit.getToolkit().exitNestedEventLoop(FXDragSourceContextPeer.this, null));
        }

        @Override protected void setNativeCursor(long nativeCtxt, Cursor c, int cType) {
            //TODO
        }


        private void dragDone(int operation, int x, int y) {
            dragDropFinished(operation != 0, operation, x, y);
        }

        FXDragSourceContextPeer(DragGestureEvent dge) {
            super(dge);
        }


        // It's Map<Long, DataFlavor> actually, but javac complains if the type isn't erased...
        @Override protected void startDrag(Transferable trans, long[] formats, Map formatMap)
        {
            activeDSContextPeer = this;

            // NOTE: we ignore the formats[] and the formatMap altogether.
            // AWT provides those to allow for more flexible representations of
            // e.g. text data (in various formats, encodings, etc.) However, FX
            // code isn't ready to handle those (e.g. it can't digest a
            // StringReader as data, etc.) So instead we perform our internal
            // translation.
            // Note that fetchData == true. FX doesn't support delayed data
            // callbacks yet anyway, so we have to fetch all the data from AWT upfront.
            transferable.updateData(trans, true);

            sourceActions = getDragSourceContext().getSourceActions();

            // Release the FX nested loop to allow onDragDetected to start the actual DnD operation,
            // and then start an AWT nested loop to wait until DnD finishes.
            if (!FXDnD.fxAppThreadIsDispatchThread) {
                loop = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
                SwingNodeHelper.leaveFXNestedLoop(FXDnDInteropO.this);
                if (!loop.enter()) {
                    // An error occured, but there's little we can do here...
                }
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    //     DROP TARGET IMPLEMENTATION
    ///////////////////////////////////////////////////////////////////////////


    private boolean isDropTargetListenerInstalled = false;
    private volatile FXDropTargetContextPeer activeDTContextPeer = null;
    private final Map<Component, DropTarget> dropTargets = new HashMap<>();

    private final EventHandler<DragEvent> onDragEnteredHandler = (event) -> {
        if (activeDTContextPeer == null) activeDTContextPeer = new FXDropTargetContextPeer();

        int action = activeDTContextPeer.postDropTargetEvent(event);

        // If AWT doesn't accept anything, let parent nodes handle the event
        if (action != 0) event.consume();
    };

    private final EventHandler<DragEvent> onDragExitedHandler = (event) -> {
        if (activeDTContextPeer == null) activeDTContextPeer = new FXDropTargetContextPeer();

        activeDTContextPeer.postDropTargetEvent(event);

        activeDTContextPeer = null;
    };

    private final EventHandler<DragEvent> onDragOverHandler = (event) -> {
        if (activeDTContextPeer == null) activeDTContextPeer = new FXDropTargetContextPeer();

        int action = activeDTContextPeer.postDropTargetEvent(event);

        // If AWT doesn't accept anything, let parent nodes handle the event
        if (action != 0) {
            // NOTE: in FX the acceptTransferModes() may ONLY be called from DRAG_OVER.
            // If the AWT app always reports NONE and suddenly decides to accept the
            // data in its DRAG_DROPPED handler, this just won't work. There's no way
            // to workaround this other than by modifing the AWT application code.
            event.acceptTransferModes(SwingDnD.dropActionsToTransferModes(action).toArray(new TransferMode[1]));
            event.consume();
        }
    };

    private final EventHandler<DragEvent> onDragDroppedHandler = (event) -> {
        if (activeDTContextPeer == null) activeDTContextPeer = new FXDropTargetContextPeer();

        int action = activeDTContextPeer.postDropTargetEvent(event);

        if (action != 0) {
            // NOTE: the dropAction is ignored since we use the action last
            // reported from the DRAG_OVER handler.
            //
            // We might want to:
            //
            //    assert activeDTContextPeer.dropAction == onDragDroppedHandler.currentAction;
            //
            // and maybe print a diagnostic message if they differ.
            event.setDropCompleted(activeDTContextPeer.success);

            event.consume();
        }

        activeDTContextPeer = null;
    };

    private final class FXDropTargetContextPeer implements DropTargetContextPeer {

        private int targetActions = DnDConstants.ACTION_NONE;
        private int currentAction = DnDConstants.ACTION_NONE;
        private DropTarget dt = null;
        private DropTargetContext ctx = null;

        private final CachingTransferable transferable = new CachingTransferable();

        // Drop result
        private boolean success = false;
        private int dropAction = 0;

        @Override public synchronized void setTargetActions(int actions) { targetActions = actions; }
        @Override public synchronized int getTargetActions() { return targetActions; }

        @Override public synchronized DropTarget getDropTarget() { return dt; }

        @Override public synchronized boolean isTransferableJVMLocal() { return false; }

        @Override public synchronized DataFlavor[] getTransferDataFlavors() { return transferable.getTransferDataFlavors(); }
        @Override public synchronized Transferable getTransferable() { return transferable; }

        @Override public synchronized void acceptDrag(int dragAction) { currentAction = dragAction; }
        @Override public synchronized void rejectDrag() { currentAction = DnDConstants.ACTION_NONE; }

        @Override public synchronized void acceptDrop(int dropAction) { this.dropAction = dropAction; }
        @Override public synchronized void rejectDrop() { dropAction = DnDConstants.ACTION_NONE; }

        @Override public synchronized void dropComplete(boolean success) { this.success = success; }


        private int postDropTargetEvent(DragEvent event)
        {
            ComponentMapper<DropTarget> mapper = mapComponent(dropTargets, (int)event.getX(), (int)event.getY());

            final EventType<?> fxEvType = event.getEventType();

            Dragboard db = event.getDragboard();
            transferable.updateData(db, DragEvent.DRAG_DROPPED.equals(fxEvType));

            final int sourceActions = SwingDnD.transferModesToDropActions(db.getTransferModes());
            final int userAction = event.getTransferMode() == null ? DnDConstants.ACTION_NONE
                : SwingDnD.transferModeToDropAction(event.getTransferMode());

            // A target for the AWT DnD event
            DropTarget target = mapper.object != null ? mapper.object : dt;

            SwingNodeHelper.runOnEDTAndWait(FXDnDInteropO.this, () -> {
                if (target != dt) {
                    if (ctx != null) {
                        AWTAccessor.getDropTargetContextAccessor().reset(ctx);
                    }
                    ctx = null;

                    currentAction = dropAction = DnDConstants.ACTION_NONE;
                }

                if (target != null) {
                    if (ctx == null) {
                        ctx = target.getDropTargetContext();
            AWTAccessor.getDropTargetContextAccessor()
                            .setDropTargetContextPeer(ctx, FXDropTargetContextPeer.this);
                    }

                    DropTargetListener dtl = (DropTargetListener)target;

                    if (DragEvent.DRAG_DROPPED.equals(fxEvType)) {
                        DropTargetDropEvent awtEvent = new DropTargetDropEvent(
                            ctx, new Point(mapper.x, mapper.y), userAction, sourceActions);

                        dtl.drop(awtEvent);
                    } else {
                        DropTargetDragEvent awtEvent = new DropTargetDragEvent(
                            ctx, new Point(mapper.x, mapper.y), userAction, sourceActions);

                        if (DragEvent.DRAG_OVER.equals(fxEvType)) dtl.dragOver(awtEvent);
                        else if (DragEvent.DRAG_ENTERED.equals(fxEvType)) dtl.dragEnter(awtEvent);
                        else if (DragEvent.DRAG_EXITED.equals(fxEvType)) dtl.dragExit(awtEvent);
                    }
                }

                dt = mapper.object;
                if (dt == null) {
                    // FIXME: once we switch to JDK 9 as the boot JDK
                    // we need to re-implement the following using
                    // available API.
                    /*
                    if (ctx != null) ctx.removeNotify();
                    */
                    ctx = null;

                    currentAction = dropAction = DnDConstants.ACTION_NONE;
                }
                if (DragEvent.DRAG_DROPPED.equals(fxEvType) || DragEvent.DRAG_EXITED.equals(fxEvType)) {
                    // This must be done to ensure that the data isn't being
                    // cached in AWT. Otherwise subsequent DnD operations will
                    // see the old data only.
                    // FIXME: once we switch to JDK 9 as the boot JDK
                    // we need to re-implement the following using
                    // available API.
                    /*
                    if (ctx != null) ctx.removeNotify();
                    */
                    ctx = null;
                }

                SwingNodeHelper.leaveFXNestedLoop(FXDnDInteropO.this);
            });

            if (DragEvent.DRAG_DROPPED.equals(fxEvType)) return dropAction;

            return currentAction;
        }
    }

    public void addDropTarget(DropTarget dt, SwingNode node) {
        dropTargets.put(dt.getComponent(), dt);
        Platform.runLater(() -> {
            if (!isDropTargetListenerInstalled) {
                node.addEventHandler(DragEvent.DRAG_ENTERED, onDragEnteredHandler);
                node.addEventHandler(DragEvent.DRAG_EXITED, onDragExitedHandler);
                node.addEventHandler(DragEvent.DRAG_OVER, onDragOverHandler);
                node.addEventHandler(DragEvent.DRAG_DROPPED, onDragDroppedHandler);

                isDropTargetListenerInstalled = true;
            }
        });
    }

    public void removeDropTarget(DropTarget dt, SwingNode node) {
        dropTargets.remove(dt.getComponent());
        Platform.runLater(() -> {
            if (isDropTargetListenerInstalled && dropTargets.isEmpty()) {
                node.removeEventHandler(DragEvent.DRAG_ENTERED, onDragEnteredHandler);
                node.removeEventHandler(DragEvent.DRAG_EXITED, onDragExitedHandler);
                node.removeEventHandler(DragEvent.DRAG_OVER, onDragOverHandler);
                node.removeEventHandler(DragEvent.DRAG_DROPPED, onDragDroppedHandler);

                isDropTargetListenerInstalled = false;
            }
        });
    }
}
