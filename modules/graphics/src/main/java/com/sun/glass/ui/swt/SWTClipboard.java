/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

class SWTClipboard extends SystemClipboard {
    org.eclipse.swt.dnd.Clipboard clipboard;
    static final String CLIPBOARD_KEY = "SWTClipboard";
    
    //TODO - temporary code to enable multiple transfers on Windows only
    static final boolean MULTIPLE_TRANSFERS = SWT.getPlatform().equals("win32");
    
    // Define local constants to avoid name conflicts
    static final int DROP_NONE = org.eclipse.swt.dnd.DND.DROP_NONE;
    static final int DROP_COPY = org.eclipse.swt.dnd.DND.DROP_COPY;
    static final int DROP_MOVE = org.eclipse.swt.dnd.DND.DROP_MOVE;
    static final int DROP_LINK = org.eclipse.swt.dnd.DND.DROP_LINK;

    private static final boolean MUTIPLE_TRANSFERS = false;
    
    // Define standard transfer types including custom transfers
    static Transfer [] StandardTransfers = new Transfer [] {
        TextTransfer.getInstance(),
        RTFTransfer.getInstance(),
        HTMLTransfer.getInstance(),
        URLTransfer.getInstance(),
        ImageTransfer.getInstance(),
        FileTransfer.getInstance(),
    };
    static Transfer [] CustomTransfers = new Transfer [0];

    public SWTClipboard(String name) {
        super(name);
        //TODO - implement selection clipboard for Linux
        if (name.equals(SYSTEM)) {
            Display display = Display.getDefault();
            clipboard = (org.eclipse.swt.dnd.Clipboard) display.getData(CLIPBOARD_KEY);
            if (clipboard == null) {
                clipboard = new org.eclipse.swt.dnd.Clipboard(display);
                display.setData(CLIPBOARD_KEY, clipboard);
                display.disposeExec(() -> clipboard.dispose());
            }
        }
    }
    
    static Transfer [] getAllTransfers () {
        Transfer [] transfers = new Transfer[StandardTransfers.length + CustomTransfers.length];
        System.arraycopy(StandardTransfers, 0, transfers, 0, StandardTransfers.length);
        System.arraycopy(CustomTransfers, 0, transfers, StandardTransfers.length, CustomTransfers.length);
        return transfers;
    }
    
    static Transfer getCustomTransfer(String mime) {
        for (int i=0; i<CustomTransfers.length; i++) {
            if (((CustomTransfer)CustomTransfers[i]).getMime().equals(mime)) {
                return CustomTransfers[i];
            }
        }
        Transfer transfer = new CustomTransfer (mime, mime);
        Transfer [] newCustom = new Transfer [CustomTransfers.length + 1];
        System.arraycopy(CustomTransfers, 0, newCustom, 0, CustomTransfers.length);
        newCustom[CustomTransfers.length] = transfer;
        CustomTransfers = newCustom;
        return transfer;
    }
    
    static Transfer [] getTransferTypes(String [] mimeTypes) {
        int count= 0;
        Transfer [] transfers = new Transfer [mimeTypes.length];
        for (int i=0; i<mimeTypes.length; i++) {
            Transfer transfer = getTransferType(mimeTypes[i]);
            if (transfer != null) transfers [count++] = transfer;
        }
        if (count != mimeTypes.length) {
            Transfer [] newTransfers = new Transfer[count];
            System.arraycopy(transfers, 0, newTransfers, 0, count);
            transfers = newTransfers;
        }
        return transfers;
    }
    
    //TODO - make this a lookup table
    static String getMime(TransferData data) {
        if (TextTransfer.getInstance().isSupportedType(data)) return TEXT_TYPE;
        if (RTFTransfer.getInstance().isSupportedType(data)) return RTF_TYPE;
        if (HTMLTransfer.getInstance().isSupportedType(data)) return HTML_TYPE;
        if (URLTransfer.getInstance().isSupportedType(data)) return URI_TYPE;
        if (ImageTransfer.getInstance().isSupportedType(data)) return RAW_IMAGE_TYPE;
        if (FileTransfer.getInstance().isSupportedType(data)) return FILE_LIST_TYPE;
        for (int i=0; i<CustomTransfers.length; i++) {
            if (CustomTransfers[i].isSupportedType(data)){
                return ((CustomTransfer)CustomTransfers[i]).getMime();
            }
        }
        return null;
    }

    //TODO - make this a lookup table
    static String getMime(Transfer transfer) {
        if (transfer.equals(TextTransfer.getInstance())) return TEXT_TYPE;
        if (transfer.equals(RTFTransfer.getInstance())) return RTF_TYPE; ;
        if (transfer.equals( HTMLTransfer.getInstance())) return HTML_TYPE;
        if (transfer.equals(URLTransfer.getInstance())) return URI_TYPE;
        if (transfer.equals( ImageTransfer.getInstance())) return RAW_IMAGE_TYPE;
        if (transfer.equals(FileTransfer.getInstance())) return FILE_LIST_TYPE;
        //if (mime.equals(FileTransfer.getInstance()) return "java.file-list";
        if (transfer instanceof CustomTransfer) {
            return ((CustomTransfer)transfer).getMime();
        }
        return null;
    }
    
    static String [] getMimes(TransferData [] transfers) {
        int count= 0;
        String [] result = new String [transfers.length];
        for (int i=0; i<transfers.length; i++) {
            result [count++] = getMime (transfers [i]);
        }
        if (count != result.length) {
            String [] newResult = new String[count];
            System.arraycopy(result, 0, newResult, 0, count);
            result = newResult;
        }
        return result;
    }
    
    static String [] getMimes(Transfer [] transfers, TransferData data) {
        int count= 0;
        String [] result = new String [transfers.length];
        for (int i=0; i<transfers.length; i++) {
            if (transfers[i].isSupportedType(data)) {
                result [count++] = getMime (transfers [i]);
            }
        }
        if (count != result.length) {
            String [] newResult = new String[count];
            System.arraycopy(result, 0, newResult, 0, count);
            result = newResult;
        }
        return result;
    }

    //TODO - make this a lookup table
    static Transfer getTransferType(String mime) {
        if (mime.equals(TEXT_TYPE)) return TextTransfer.getInstance();
        if (mime.equals(RTF_TYPE)) return RTFTransfer.getInstance();
        if (mime.equals(HTML_TYPE)) return HTMLTransfer.getInstance();
        if (mime.equals(URI_TYPE)) return URLTransfer.getInstance();
        if (mime.equals(RAW_IMAGE_TYPE)) return ImageTransfer.getInstance();
        if (mime.equals(FILE_LIST_TYPE)) {// || mime.equals("java.file-list")) {
            return FileTransfer.getInstance();
        }
        return getCustomTransfer(mime);
    }
    
    //TODO - make this a lookup table
    static Object getData(String mime, TransferData data) {
        if (mime.equals(TEXT_TYPE)) return TextTransfer.getInstance().nativeToJava(data);
        if (mime.equals(RTF_TYPE)) return RTFTransfer.getInstance().nativeToJava(data);
        if (mime.equals(HTML_TYPE)) return HTMLTransfer.getInstance().nativeToJava(data);
        if (mime.equals(URI_TYPE)) return URLTransfer.getInstance().nativeToJava(data);
        if (mime.equals(RAW_IMAGE_TYPE)) return ImageTransfer.getInstance().nativeToJava(data);
        if (mime.equals(FILE_LIST_TYPE)) {// || mime.equals("java.file-list")) {
            return FileTransfer.getInstance().nativeToJava(data);
        }
        Transfer transfer = getCustomTransfer(mime);
        if (transfer != null) return ((CustomTransfer)transfer).nativeToJava(data);
        return null;
    }

    @Override
    protected boolean isOwner() {
        return MUTIPLE_TRANSFERS;
    }
    
    static int getSWTAction(int actions) {
        int result = ACTION_NONE;
        if ((actions & ACTION_COPY) != 0) result |= DROP_COPY;
        if ((actions & ACTION_MOVE) != 0) result |= DROP_MOVE;
        if ((actions & ACTION_REFERENCE) != 0) result |=DROP_LINK;
        return result;
    }
    
    //TODO - better name that indicates it is the invers of getDragActions()
    static int getFXAction(int actions) {
        int result = DROP_NONE;
        if ((actions & DROP_COPY) != 0) result |= ACTION_COPY;
        if ((actions & DROP_MOVE) != 0) result |= ACTION_MOVE;
        if ((actions & DROP_LINK) != 0) result |= ACTION_REFERENCE;
        return result;
    }
    
    @Override
    protected void pushToSystem(HashMap<String, Object> data, int supportedActions) {
        int count = 0;
        ImageData imageData = null;
        Set<String> keys = data.keySet();
        Object [] objects = new Object [keys.size()];
        Transfer[] transfers = new Transfer[keys.size()];
        for (String key : keys) {
            Transfer transfer = getTransferType(key);
            if (transfer != null) {
                //TODO - image not done, format wrong, alpha wrong
                if (transfer instanceof ImageTransfer && data.get(key) instanceof Pixels) {
                    Pixels pixels = (Pixels) data.get(key);
                    objects[count] = imageData = SWTApplication.createImageData(pixels);
                } else {
                    objects[count] = data.get(key);
                }
                transfers [count] = transfer;
                count++;
            }
        }
        if (count == 0) return;
        if (count != objects.length) {
            Object [] newObjects = new Object [objects.length];
            System.arraycopy(objects, 0, newObjects, 0, objects.length);
            objects = newObjects;
            Transfer [] newTransfers = new Transfer [transfers.length];
            System.arraycopy(transfers, 0, newTransfers, 0, transfers.length);
            transfers = newTransfers;
        }
        if (clipboard != null) {
            //TODO - setting and empty string to the clipboard causes an exception
            //TODO - clear the contents instead (what about multiple objects?)
            for (int i=0; i<transfers.length; i++) {
                if (transfers[i] instanceof TextTransfer && objects[i] instanceof String) {
                    if (((String)objects[i]).length() == 0) {
                        clipboard.clearContents();
                        return;
                    }
                }
            }
            clipboard.setContents(objects, transfers);
        } else {
            //TODO - does setting an empty string fail for drag and drop like the clipboard
            final Control control = Display.getDefault().getFocusControl();
            if (control != null && control.getData() instanceof SWTView) {
                final SWTView view = (SWTView) control.getData();
                int dragOperation = getSWTAction(supportedActions);
                final DragSource dragSource = new DragSource(control, dragOperation);
                dragSource.setTransfer(transfers);
                dragSource.setData("objects", objects);
                dragSource.setData("imageData", imageData);
                dragSource.addDragListener(new DragSourceListener() {
                    Image image = null;
                    public void dragFinished(DragSourceEvent event) {
                        if (image != null) {
                            image.dispose();
                            image = null;
                        }
                        dragSource.setData("objects", null);
                        dragSource.setData("imageData", null);
                        dragSource.dispose();
                        view.notifyDragEnd(getFXAction(event.detail));
                    }
                    public void dragSetData(DragSourceEvent event) {
                        Object [] objects = (Object []) dragSource.getData("objects");
                        Transfer [] transfers = dragSource.getTransfer();
                        for (int i=0; i<transfers.length; i++) {
                            if (transfers[i].isSupportedType(event.dataType)) {
                                String mime = getMime(transfers[i]);
                                if (mime != null) {
                                    event.doit = true;
                                    event.data = objects [i];
                                    return;
                                }
                            }
                            event.doit = false;
                        }
                    }
                    public void dragStart(DragSourceEvent event) {
                        ImageData imageData = (ImageData) dragSource.getData("imageData");
                        if (imageData != null) {
                            event.image = image = new Image(event.display, imageData);
                            event.offsetX = imageData.width / 2;
                            event.offsetY = imageData.height / 2;
                        }
                        Point point = control.toDisplay(event.x, event.y);
                        //TODO - button number is hard coded
                        view.notifyDragStart(1, event.x, event.y, point.x, point.y);
                    }
                });
                //TODO - not sure, why do we need to set the drop target transfers when drag starts?
                //TODO - is there another place that makes more sense to make sure they are up to date
                if (view.dropTarget != null) view.dropTarget.setTransfer(getAllTransfers());
                control.notifyListeners(SWT.DragDetect, null);
            }
        }
    }

    @Override
    protected void pushTargetActionToSystem(int actionDone) {
        //TODO - what is the correct implementation for this method?
        //System.out.println("SWTClipboard.pushTargetActionToSystem");
    }

    @Override
    protected Object popFromSystem(String mimeType) {
        Transfer transfer = getTransferType(mimeType);
        if (transfer != null) {
            //TODO - image not done, format wrong, alpha wrong
            Object data = null;
            if (clipboard != null) {
                data = clipboard.getContents(transfer);
            } else {
                if (MULTIPLE_TRANSFERS) {
                    for (int i=0; i<transferData.length; i++) {
                        if (transfer.isSupportedType(transferData[i])) {
                            data = getData(mimeType, transferData[i]);
                            break;
                        }
                    }
                } else {
                    data = currentData;
                }
            }
            if (data instanceof ImageData) {
                return SWTApplication.createPixels((ImageData) data);
            }
            return data;
        }
        return null;
    }

    //TODO - glass API should include the idea that a control takes part in DND
    //TODO - don't use statics, they are never cleared, shared state is not correct etc.
    static int operations = DROP_NONE;
    static TransferData currentTransferData;
    static TransferData [] transferData;
    static Object currentData;
    
    @Override
    protected int supportedSourceActionsFromSystem() {
        if (clipboard != null)  return Clipboard.ACTION_COPY;
        return getFXAction(operations);
    }

    static DropTarget createDropTarget(final Control control) {
        final SWTView view = (SWTView) control.getData();
        final DropTarget dropTarget = new DropTarget(control, DROP_COPY | DROP_LINK | DROP_MOVE);
        dropTarget.setTransfer(getAllTransfers());
        dropTarget.addDropListener(new DropTargetListener() {
            //Object currentData;
            //TransferData [] transferData;
            //TransferData currentTransferData;
            int detail = DROP_NONE;
            //int operations = DROP_NONE;
            public void dragEnter(DropTargetEvent event) {
                dropTarget.setTransfer(getAllTransfers());
                detail = event.detail;
                operations = event.operations;
                dragOver (event, true, detail);
            }
            public void dragLeave(DropTargetEvent event) {
                detail = operations = DROP_NONE;
                currentData = null;
                transferData = null;
                currentTransferData = null;
                view.notifyDragLeave();
            }
            public void dragOperationChanged(DropTargetEvent event) {
                detail = event.detail;
                operations = event.operations;
                dragOver(event, false, detail);
            }
            public void dragOver(DropTargetEvent event) {
                operations = event.operations;
                dragOver (event, false, detail);
            }
            public void dragOver(DropTargetEvent event, boolean enter, int detail) {
                transferData = event.dataTypes;
                currentTransferData = event.currentDataType;
                Point pt = control.toControl(event.x, event.y);
                if (detail == DROP_NONE) detail = DROP_COPY;
                int action = getFXAction(detail), acceptAction;
                if (enter) {
                    acceptAction = view.notifyDragEnter(pt.x, pt.y, event.x, event.y, action);
                } else {
                    acceptAction = view.notifyDragOver(pt.x, pt.y, event.x, event.y, action);
                }
                event.detail = getSWTAction(acceptAction);
                //TODO - FX should set the transfer type that is desired for the drop
                //currentTransferData = event.currentDataType;
            }
            public void drop(DropTargetEvent event) {
                detail = event.detail;
                operations = event.operations;
                currentData = event.data;
                transferData = event.dataTypes;
                currentTransferData = event.currentDataType;
                Point pt = control.toControl(event.x, event.y);
                int action = getFXAction(event.detail);
                int acceptAction = view.notifyDragDrop(pt.x, pt.y, event.x, event.y, action);
                event.detail = getSWTAction(acceptAction);
                currentData = null;
                transferData = null;
                currentTransferData = null;
            }
            public void dropAccept(DropTargetEvent event) {
            }
        });
        return dropTarget;
    }
    
    @Override
    protected String[] mimesFromSystem() {
        //TODO - return non-standard clipboard/drag and drop mimes
        if (clipboard != null) {
            int count= 0;
            TransferData[] data = clipboard.getAvailableTypes();
            String [] result = new String [data.length];
            for (int i=0; i<data.length; i++) {
                String mime = getMime(data[i]);
                if (mime != null) result [count++] = mime;
            }
            if (count == result.length) return result;
            String [] newResult = new String [count];
            System.arraycopy(result,  0, newResult, 0, count);
            return newResult;
        } else {
            if (MULTIPLE_TRANSFERS) {
                // Gets the mime type for the available objects
                return getMimes(transferData);
            } else {
                // Gets the mime type for the dropped object
                if (currentTransferData == null) return new String [0];
                return getMimes(getAllTransfers(), currentTransferData);
            }
        }
    }
}
