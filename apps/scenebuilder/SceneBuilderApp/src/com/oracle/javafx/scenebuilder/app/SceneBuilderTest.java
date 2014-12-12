/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.app;

import com.oracle.javafx.scenebuilder.app.about.AboutWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractGenericHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Cell;
import javafx.scene.control.TreeItem;

/**
 * This class groups the entry points reserved to QE testing.
 * 
 * Design consideration
 * 
 * This class tries to hide SB internal architecture as much as possible; 
 * for example, an FXML document is represented by a DocumentWindowController 
 * instance in SB; however, in this class, the FXML document is
 * identified by the Scene instance holding the document window contents..
 * 
 * However some internals must be disclosed:
 * 
 * - FXOMObject : represents a design object ; it is paired with an object
 *   in the user scene graph ; FXOMObject.getSceneGraphObject() returns the
 *   matching scene graph object : sometimes it's a plain Node (eg Button),
 *   sometimes not (eg a Tab, a TableColumn...).
 * 
 * - ...
 * 
 */
public class SceneBuilderTest {

    /**
     * Performs [File/New] menu command and returns the Scene instance 
     * holding the new document window.
     * 
     * @return the scene instance holding the new document window (never null).
     */
    public static Scene newFxmlFile() {
        final DocumentWindowController newWindow 
                = SceneBuilderApp.getSingleton().makeNewWindow();
        newWindow.openWindow();
        return newWindow.getScene();
    }
    
    /**
     * Performs [File/Open] menu command with the file passed in argument.
     * If an error happens, the method throws the corresponding exception
     * (in place of displaying an alert dialog).
     * 
     * @param fxmlFile fxml file to be opened (never null)
     * @return the scene instance holding the new document window (never null).
     * @throws IOException if the open operation has failed.
     */
    public static Scene openFxmlFile(File fxmlFile) throws IOException {
        assert fxmlFile != null;
        
        final DocumentWindowController newWindow 
                = SceneBuilderApp.getSingleton().makeNewWindow();
        newWindow.loadFromFile(fxmlFile);
        newWindow.openWindow();
        return newWindow.getScene();
    }
    
    /**
     * Returns the root of the [user scene graph] ie the scene graph
     * constructed from the content of the FXML file. If documentScene does
     * not match any document window, returns null.
     * 
     * Note: the returned is an [Object] because an FXML file is not limited
     * to javafx.scene.Node.
     * 
     * @param documentScene a scene holding a document window
     * 
     * @return the user scene graph root or null if documentScene does 
     *         not hold a document window
     */
    public static Object getUserSceneGraphRoot(Scene documentScene) {
        assert documentScene != null;
        
        final Object result;
        final FXOMDocument fxomDocument = lookupFxomDocument(documentScene);
        if (fxomDocument == null) {
            result = null;
        } else {
            result = fxomDocument.getSceneGraphRoot();
        }
        
        return result;
    }
    
    
    /**
     * Returns the set of selected objects. Each selected object is represented
     * by an FXOMObject instance.
     * 
     * @param documentScene a scene holding a document window
     * @return the set of selected objects or null if documentScene does 
     *         not hold a document window
     */
    public static Set<FXOMObject> findSelectedObjects(Scene documentScene) {
        assert documentScene != null;
        
        final Set<FXOMObject> result;
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc == null) {
            result = null;
        } else {
            final Selection selection = dwc.getEditorController().getSelection();
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                result = Collections.unmodifiableSet(osg.getItems());
            } else {
                // TODO(elp) : will implement later
                result = Collections.emptySet();
            }
        }
        
        return result;
    }
    
    /**
     * Returns the fxom object matching a given node in the content panel. 
     * Returns null if nothing is found.
     * 
     * @param node a node part of the content panel (never null)
     * @return null or the matching fxom object
     */
    public static FXOMObject fxomObjectFromContentPanelNode(Node node) {
        assert node != null;
        assert node.getScene() != null;
        
        final FXOMObject result;
        final DocumentWindowController dwc = lookupWindowController(node.getScene());
        if (dwc == null) {
            result = null;
        } else {
            final Bounds b = node.getLayoutBounds();
            final double midX = (b.getMinX() + b.getMaxX()) / 2.0;
            final double midY = (b.getMinY() + b.getMaxY()) / 2.0;
            final Point2D nodeCenter = node.localToScene(midX, midY, true /* rootScene */);
            
            final ContentPanelController cpc = dwc.getContentPanelController();
            result = cpc.searchWithNode(node, nodeCenter.getX(), nodeCenter.getY());
        }
        
        return result;
    }
    
    /**
     * Returns the node in content panel matching a given fxom object.
     * This method invokes FXOMObject.getSceneGraphObject() and checks if
     * it is a Node. If it's not, it returns null.
     * 
     * @param documentScene a scene holding a document window
     * @param fxomObject an fxom object (never null)
     * @return null or the matching node in content panel
     */
    public static Node fxomObjectToContentPanelNode(
            Scene documentScene, FXOMObject fxomObject) {
        assert documentScene != null;
        assert fxomObject != null;
        
        final Node result;
        if (fxomObject.getSceneGraphObject() instanceof Node) {
            result = (Node) fxomObject.getSceneGraphObject();
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Returns the fxom object matching a given node in the hierarchy panel. 
     * Returns null if nothing is found.
     * This method lookups for a Cell object ancestor of the specified node parameter
     * and returns the associated FXOMObject.
     * If there is no Cell object ancestor, it returns null.
     * 
     * @param node a node part of the hierarchy panel (never null)
     * @return null or the matching fxom object
     */
    public static FXOMObject fxomObjectFromHierarchyPanelNode(Node node) {
        assert node != null;
        assert node.getScene() != null;

        final FXOMObject result;
        final DocumentWindowController dwc = lookupWindowController(node.getScene());
        if (dwc == null) {
            result = null;
        } else {
            Parent parent = node.getParent();
            Cell<?> cell = null;
            while (parent != null) {
                if (parent instanceof Cell) {
                    cell = (Cell<?>) parent;
                    break;
                }
            }
            // A cell has been found
            if (cell != null) {
                assert cell.isEmpty() == false;
                if (cell.isVisible()) {
                    final Object item = cell.getItem();
                    assert item instanceof HierarchyItem;
                    final HierarchyItem hierarchyItem = (HierarchyItem) item;
                    result = hierarchyItem.getFxomObject();
                } else {
                    result = null;
                }
            } else {
                result = null;
            }
        }

        return result;
    }
    
    /**
     * Returns the node in hierarchy panel matching a given fxom object.
     * Returns null if the FXOMObject is currently not displayed by hierarchy
     * panel.
     * The returned Node is a Cell object. 
     * 
     * @param documentScene a scene holding a document window
     * @param fxomObject an fxom object (never null)
     * @return null or the matching node in hierarchy panel
     */
    public static Node fxomObjectToHierarchyPanelNode(
            Scene documentScene, FXOMObject fxomObject) {
        assert documentScene != null;
        assert fxomObject != null;
        
        final Node result;
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc == null) {
            result = null;
        } else {
            final EditorController ec = dwc.getEditorController();
            assert fxomObject.getFxomDocument() == ec.getFxomDocument();

            final AbstractHierarchyPanelController hpc = dwc.getHierarchyPanelController();
            assert hpc != null;
            assert hpc.getPanelControl() != null;
            if (hpc.getPanelControl().isVisible()) {
                final TreeItem<HierarchyItem> treeItem = hpc.lookupTreeItem(fxomObject);
                if (treeItem != null) {
                    result = hpc.getCell(treeItem);
                } else {
                    result = null;
                }
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    /**
     * Looks for the TreeItem corresponding to the specified FXOM object.
     * If a TreeItem has been found, scroll to this TreeItem within the hierarchy panel.
     * 
     * @param documentScene
     * @param fxomObject 
     */
    public static void revealInHierarchyPanel(
            Scene documentScene, FXOMObject fxomObject) {
        assert documentScene != null;
        assert fxomObject != null;
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc != null) {
            final EditorController ec = dwc.getEditorController();
            assert fxomObject.getFxomDocument() == ec.getFxomDocument();

            final AbstractHierarchyPanelController hpc 
                    = dwc.getHierarchyPanelController();
            assert hpc != null;
            assert hpc.getPanelControl() != null;
            // First expand the hierarchy tree
            expandAllTreeItems(hpc.getRoot());
            // Then look for the fxom object
            if (hpc.getPanelControl().isVisible()) {
                final TreeItem<HierarchyItem> treeItem 
                        = hpc.lookupTreeItem(fxomObject);
                if (treeItem != null) {
                    hpc.scrollTo(treeItem);
                }
            }
        }
    }

    /**
     * Returns the node representing a resize handle.
     * 
     * @param documentScene a scene holding a document window
     * @param fxomObject one of the selected fxom object
     * @param cp the cardinal point of the target handle
     * @return null or the node representing the handle
     */
    public static Node lookupResizeHandle(
            Scene documentScene, FXOMObject fxomObject, CardinalPoint cp) {
        assert documentScene != null;
        assert fxomObject != null;
        
        final Node result;
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc == null) {
            result = null;
        } else {
            final EditorController ec = dwc.getEditorController();
            
            assert fxomObject.getFxomDocument() == ec.getFxomDocument();
            assert ec.getSelection().isSelected(fxomObject);
            
            final ContentPanelController cpc = dwc.getContentPanelController();
            final AbstractHandles<?> h = cpc.lookupHandles(fxomObject);
            if (h instanceof AbstractGenericHandles<?>) {
                final AbstractGenericHandles<?> gh = (AbstractGenericHandles<?>) h;
                result = gh.getHandleNode(cp);
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    /**
     * Returns the version string.
     * It has the format 'Version: [major].[minor]-b[ii], Changeset: [someValue]'.
     * <br>A typical value is 'Version: 2.0-b07, Changeset: 8a5ccd834b5f'.
     * 
     * @return a version string. It is never null: in the case something weird
     * would occur when constructing the proper value then what is returned is
     * 'UNSET'.
     */
    public static String getVersionString() {
        AboutWindowController awc = new AboutWindowController();
        return awc.getBuildInfo();
    }
    
    
    /**
     * Closes the preview window associated to a document window.
     * Performs nothing if documentScene is not a scene associated to a
     * document window or if preview window is not opened.
     * 
     * @param documentScene a scene holding a document window
     */
    public static void closePreviewWindow(Scene documentScene) {
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc != null) {
            dwc.getPreviewWindowController().closeWindow();
        }
    }
    
    /**
     * Starts the application in test mode.
     * In this mode, no files are opened at application startup.
     * 
     * @param args arguments to SceneBuilderApp.main()
     */
    public static void startApplication(String[] args) {
        AppPlatform.setStartingFromTestBed(true);
        SceneBuilderApp.main(args);
    }
    
    /*
     * Private
     */
    
    private static FXOMDocument lookupFxomDocument(Scene documentScene) {
        final FXOMDocument result;
        
        final DocumentWindowController dwc = lookupWindowController(documentScene);
        if (dwc == null) {
            result = null;
        } else {
            result = dwc.getEditorController().getFxomDocument();
        }
        
        return result;
    }
    
    private static DocumentWindowController lookupWindowController(Scene documentScene) {
        DocumentWindowController result = null;
        
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController c : app.getDocumentWindowControllers()) {
            if (c.getScene() == documentScene) {
                result = c;
                break;
            }
        }
        
        return result;
    }
    
    private static <T> void expandAllTreeItems(final TreeItem<T> parentTreeItem) {
        if (parentTreeItem != null) {
            parentTreeItem.setExpanded(true);
            final List<TreeItem<T>> children = parentTreeItem.getChildren();
            if (children != null) {
                for (TreeItem<T> child : children) {
                    expandAllTreeItems(child);
                }
            }
        }
    }
}
