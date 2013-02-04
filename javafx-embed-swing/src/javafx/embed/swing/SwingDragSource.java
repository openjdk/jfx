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
package javafx.embed.swing;

import com.sun.javafx.embed.EmbeddedSceneDragSourceInterface;
import com.sun.javafx.tk.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javafx.scene.input.TransferMode;

/**
 * Drag source to deliver data from Swing environment to embedded FX scene.
 */
final class SwingDragSource implements EmbeddedSceneDragSourceInterface {

    private Map<String, Object> mimeType2Data = Collections.EMPTY_MAP;
    private int sourceActions;
    private Set<TransferMode> cachedTransferModes;
    
    SwingDragSource(final DropTargetDragEvent e) {
        setContents(e);
    }
    
    void updateContents(final DropTargetDragEvent e) {
        updateSourceActions(e.getSourceActions());
        updateData(e.getTransferable());
    }

    private void setContents(final DropTargetDragEvent e) {
        this.sourceActions = DnDConstants.ACTION_NONE;
        this.cachedTransferModes = null;
        this.mimeType2Data = Collections.EMPTY_MAP;
        updateContents(e);
    }

    private void updateSourceActions(final int newSourceActions) {
        if (newSourceActions != this.sourceActions) {
            this.sourceActions = newSourceActions;
            this.cachedTransferModes = null;
        }
    }

    private void updateData(final Transferable t) {
        final Map<String, DataFlavor> mimeType2DataFlavor =
                DataFlavorUtils.adjustSwingDataFlavors(
                t.getTransferDataFlavors());
        if (mimeType2DataFlavor.keySet().equals(mimeType2Data.keySet())) {
            // Mime types have't changed. Assume data has not been
            // changed as well, so don't need to reread it
            return;
        }
        //
        // Read data from the given Transferable in advance. Need to do this
        // because we don't want Transferable#getTransferData() to be called
        // from DropTargetListener#drop().
        //
        // When Transferable#getTransferData() is called from
        // DropTargetListener#drop() it may fail with
        // "java.awt.dnd.InvalidDnDOperationException: No drop current"
        // error if the call takes place prior to
        // DropTargetDropEvent#acceptDrop() call.
        // But if Transferable#getTransferData() is called from
        // DropTargetListener#dragEnter() and DropTargetListener#dragExit()
        // it works flawlessly without any extra calls.
        //
        // If we keep reference to source Transferable in SwingDragSource and
        // call Transferable#getTransferData() on it from
        // SwingDragSource#getData() we may run into
        // "java.awt.dnd.InvalidDnDOperationException" issue as
        // SwingDragSource#getData() is called from FX user code and from
        // QuantumClipboard#getContent() (sik!). These calls usually take
        // place in the context of 
        // EmbeddedSceneDropTargetInterface#handleDragDrop() method as the 
        // normal handling of DnD.
        // Instead of keeping reference to source Transferable we just read
        // all its data while in the context safe for calling
        // Transferable#getTransferData().
        //
        // This observation is true for standard AWT Transferable-s. 
        // Things may be totally broken for custom Transferable-s though.
        //
        try {
            mimeType2Data = DataFlavorUtils.readAllData(t, mimeType2DataFlavor);
        } catch (Exception e) {
            mimeType2Data = Collections.EMPTY_MAP;
        }
    }

    @Override
    public Set<TransferMode> getSupportedActions() {
        assert Toolkit.getToolkit().isFxUserThread();
        if (cachedTransferModes == null) {
            cachedTransferModes =
                    SwingDnD.dropActionsToTransferModes(sourceActions);
        }
        return cachedTransferModes;
    }

    @Override
    public Object getData(final String mimeType) {
        assert Toolkit.getToolkit().isFxUserThread();
        return mimeType2Data.get(mimeType);
    }

    @Override
    public String[] getMimeTypes() {
        assert Toolkit.getToolkit().isFxUserThread();
        return mimeType2Data.keySet().toArray(new String[0]);
    }

    @Override
    public boolean isMimeTypeAvailable(final String mimeType) {
        assert Toolkit.getToolkit().isFxUserThread();
        return Arrays.asList(getMimeTypes()).contains(mimeType);
    }

    @Override
    public void dragDropEnd(TransferMode performedAction) {
        throw new UnsupportedOperationException();                
    }
}
