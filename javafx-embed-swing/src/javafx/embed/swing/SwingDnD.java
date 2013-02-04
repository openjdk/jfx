/*
 * Copyright (c) 2012, Oracle  and/or its affiliates. All rights reserved.
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
package javafx.embed.swing;

import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javafx.scene.input.TransferMode;

import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedSceneDragSourceInterface;
import com.sun.javafx.embed.EmbeddedSceneDragStartListenerInterface;
import com.sun.javafx.embed.EmbeddedSceneDropTargetInterface;
import com.sun.javafx.tk.Toolkit;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.Point;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * An utility class to connect DnD mechanism of Swing and FX.
 */
final class SwingDnD {

    private final JFXPanelFacade facade;
    private final Transferable dndTransferable = new DnDTransferable();
    private final DragSourceListener dragSourceListener;
    private SwingDragSource swingDragSource;
    private EmbeddedSceneDragSourceInterface fxDragSource;
    private EmbeddedSceneDropTargetInterface dropTarget;
    private MouseEvent me;

    interface JFXPanelFacade {

        EmbeddedSceneInterface getScene();
    }

    SwingDnD(final JComponent comp, final JFXPanelFacade facade) {
        this.facade = facade;

        comp.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                storeMouseEvent(me);
            }

            @Override
            public void mouseDragged(MouseEvent me) {
                storeMouseEvent(me);
            }

            @Override
            public void mousePressed(MouseEvent me) {
                storeMouseEvent(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                storeMouseEvent(me);
            }
        });

        dragSourceListener = new DragSourceAdapter() {

            @Override
            public void dragDropEnd(final DragSourceDropEvent dsde) {
                // Fix for RT-21836
                if (fxDragSource == null) {
                    return;
                }

                assert hasFxScene();

                try {
                    fxDragSource.dragDropEnd(dropActionToTransferMode(dsde.
                            getDropAction()));
                } finally {
                    fxDragSource = null;
                }
            }
        };

        new DropTarget(comp, DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE |
                DnDConstants.ACTION_LINK, new DropTargetAdapter() {

            @Override
            public void dragEnter(final DropTargetDragEvent e) {
                if (!hasFxScene()) {
                    e.rejectDrag();
                    return;
                }

                if (fxDragSource == null) {
                    // There is no FX drag source, create wrapper for external
                    // drag source.
                    assert swingDragSource == null;
                    swingDragSource = new SwingDragSource(e);
                }

                final Point orig = e.getLocation();
                final Point screen = new Point(orig);
                SwingUtilities.convertPointToScreen(screen, comp);
                applyDragResult(getDropTarget().handleDragEnter(orig.x, orig.y,
                                                                screen.x,
                                                                screen.y,
                                                                dropActionToTransferMode(e.
                        getDropAction()), getDragSource()), e);
            }

            @Override
            public void dragExit(final DropTargetEvent e) {
                if (!hasFxScene()) {
                    // The drag has been already rejected in dragEnter(), but it doesn't
                    // prevent dragExit(), dragOver() and drop() from being called
                    return;
                }
                
                try {
                    dropTarget.handleDragLeave();
                } finally {
                    endDnD();
                }
            }

            @Override
            public void dragOver(final DropTargetDragEvent e) {
                if (!hasFxScene()) {
                    // The drag has been already rejected in dragEnter(), but it doesn't
                    // prevent dragExit(), dragOver() and drop() from being called
                    return;
                }

                if (swingDragSource != null) {
                    swingDragSource.updateContents(e);
                }

                final Point orig = e.getLocation();
                final Point screen = new Point(orig);
                SwingUtilities.convertPointToScreen(screen, comp);
                applyDragResult(dropTarget.handleDragOver(orig.x, orig.y,
                                                          screen.x, screen.y,
                                                          dropActionToTransferMode(e.
                        getDropAction())), e);
            }

            @Override
            public void drop(final DropTargetDropEvent e) {
                if (!hasFxScene()) {
                    // The drag has been already rejected in dragEnter(), but it doesn't
                    // prevent dragExit(), dragOver() and drop() from being called
                    return;
                }

                final Point orig = e.getLocation();
                final Point screen = new Point(orig);
                SwingUtilities.convertPointToScreen(screen, comp);

                try {
                    final TransferMode dropResult =
                            dropTarget.handleDragDrop(orig.x, orig.y, screen.x,
                                                      screen.y,
                                                      dropActionToTransferMode(e.
                            getDropAction()));
                    applyDropResult(dropResult, e);

                    e.dropComplete(dropResult != null);
                } finally {
                    endDnD();
                }
            }
        });
    }

    void addNotify() {
        DragSource.getDefaultDragSource().addDragSourceListener(
                dragSourceListener);
    }

    void removeNotify() {
        // RT-22049: Multi-JFrame/JFXPanel app leaks JFXPanels
        // Don't forget to unregister drag source listener!
        DragSource.getDefaultDragSource().removeDragSourceListener(
                dragSourceListener);
    }

    EmbeddedSceneDragStartListenerInterface getDragStartListener() {
        return new EmbeddedSceneDragStartListenerInterface() {

            @Override
            public void dragStarted(
                    final EmbeddedSceneDragSourceInterface dragSource,
                    final TransferMode dragAction) {
                assert Toolkit.getToolkit().isFxUserThread();
                assert dragSource != null;
                
                //
                // The method is called from FX Scene just before entering
                // nested event loop servicing DnD events.
                // It should initialize DnD in AWT EDT.
                //

                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        assert fxDragSource == null;
                        assert swingDragSource == null;
                        assert dropTarget == null;
                        
                        fxDragSource = dragSource;

                        startDrag(me, dndTransferable, dragSource.
                                getSupportedActions(), dragAction);
                    }
                });
            }
        };
    }

    private static void startDrag(final MouseEvent e, final Transferable t,
                                  final Set<TransferMode> sa,
                                  final TransferMode dragAction) {

        assert sa.contains(dragAction);

        //
        // This is a replacement for the default AWT drag gesture recognizer.
        // Not sure DragGestureRecognizer was ever supposed to be used this way.
        //

        final class StubDragGestureRecognizer extends DragGestureRecognizer {

            StubDragGestureRecognizer() {
                super(DragSource.getDefaultDragSource(), e.getComponent());
                super.setSourceActions(transferModesToDropActions(sa));
                super.appendEvent(e);
            }

            @Override
            protected void registerListeners() {
            }

            @Override
            protected void unregisterListeners() {
            }
        }

        final Point pt = new Point(e.getX(), e.getY());

        final int action = transferModeToDropAction(dragAction);

        final DragGestureRecognizer dgs = new StubDragGestureRecognizer();

        final List<InputEvent> events = Arrays.asList(new InputEvent[]{dgs.
                    getTriggerEvent()});

        final DragGestureEvent dse = new DragGestureEvent(dgs, action, pt,
                                                          events);

        dse.startDrag(null, t);
    }

    private boolean hasFxScene() {
        assert SwingUtilities.isEventDispatchThread();
        return getFxScene() != null;
    }

    private EmbeddedSceneInterface getFxScene() {
        return facade.getScene();
    }

    private EmbeddedSceneDragSourceInterface getDragSource() {
        assert hasFxScene();

        assert (swingDragSource == null) != (fxDragSource == null);

        if (swingDragSource != null) {
            return swingDragSource;
        }
        return fxDragSource;
    }

    private EmbeddedSceneDropTargetInterface getDropTarget() {
        assert hasFxScene();

        if (dropTarget == null) {
            dropTarget = getFxScene().createDropTarget();
        }
        return dropTarget;
    }
    
    private void endDnD() {
        assert dropTarget != null;
        
        dropTarget = null;
        if (swingDragSource != null) {
            swingDragSource = null;
        }
    }

    private void storeMouseEvent(final MouseEvent me) {
        this.me = me;
    }

    private static void applyDragResult(final TransferMode dragResult,
                                        final DropTargetDragEvent e) {
        if (dragResult == null) {
            e.rejectDrag();
        } else {
            e.acceptDrag(transferModeToDropAction(dragResult));
        }
    }

    private static void applyDropResult(final TransferMode dropResult,
                                        final DropTargetDropEvent e) {
        if (dropResult == null) {
            e.rejectDrop();
        } else {
            e.acceptDrop(transferModeToDropAction(dropResult));
        }
    }

    static TransferMode dropActionToTransferMode(final int dropAction) {
        switch (dropAction) {
            case DnDConstants.ACTION_COPY:
                return TransferMode.COPY;
            case DnDConstants.ACTION_MOVE:
                return TransferMode.MOVE;
            case DnDConstants.ACTION_LINK:
                return TransferMode.LINK;
            case DnDConstants.ACTION_NONE:
                return null;
            default:
                throw new IllegalArgumentException();
        }
    }

    static int transferModeToDropAction(final TransferMode tm) {
        switch (tm) {
            case COPY:
                return DnDConstants.ACTION_COPY;
            case MOVE:
                return DnDConstants.ACTION_MOVE;
            case LINK:
                return DnDConstants.ACTION_LINK;
            default:
                throw new IllegalArgumentException();
        }
    }

    static Set<TransferMode> dropActionsToTransferModes(
            final int dropActions) {
        final Set<TransferMode> tms = EnumSet.noneOf(TransferMode.class);
        if ((dropActions & DnDConstants.ACTION_COPY) != 0) {
            tms.add(TransferMode.COPY);
        }
        if ((dropActions & DnDConstants.ACTION_MOVE) != 0) {
            tms.add(TransferMode.MOVE);
        }
        if ((dropActions & DnDConstants.ACTION_LINK) != 0) {
            tms.add(TransferMode.LINK);
        }
        return Collections.unmodifiableSet(tms);
    }

    static int transferModesToDropActions(final Set<TransferMode> tms) {
        int dropActions = DnDConstants.ACTION_NONE;
        for (TransferMode tm : tms) {
            dropActions |= transferModeToDropAction(tm);
        }
        return dropActions;
    }

    //
    // This is facade to export data from FX to outer world.
    //
    private final class DnDTransferable implements Transferable {

        @Override
        public Object getTransferData(final DataFlavor flavor) throws
                UnsupportedFlavorException, IOException {
            checkSwingEventDispatchThread();

            if (!hasFxScene()) {
                return null;
            }

            final String mimeType = DataFlavorUtils.getFxMimeType(flavor);

            return DataFlavorUtils.adjustFxData(flavor, getDragSource().getData(
                    mimeType));
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            checkSwingEventDispatchThread();

            if (!hasFxScene()) {
                return null;
            }

            final String mimeTypes[] = getDragSource().getMimeTypes();

            final ArrayList<DataFlavor> flavors =
                    new ArrayList<DataFlavor>(mimeTypes.length);
            for (String mime : mimeTypes) {
                DataFlavor flavor = null;
                try {
                    flavor = new DataFlavor(mime);
                } catch (ClassNotFoundException e) {
                    // FIXME: what to do?
                    continue;
                }
                flavors.add(flavor);
            }
            return flavors.toArray(new DataFlavor[0]);
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            checkSwingEventDispatchThread();

            if (!hasFxScene()) {
                return false;
            }

            return getDragSource().isMimeTypeAvailable(DataFlavorUtils.
                    getFxMimeType(flavor));
        }
    };

    private static void checkSwingEventDispatchThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
    }
}
