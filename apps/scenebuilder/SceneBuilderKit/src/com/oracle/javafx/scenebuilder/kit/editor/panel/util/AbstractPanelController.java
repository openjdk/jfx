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
package com.oracle.javafx.scenebuilder.kit.editor.panel.util;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;

/**
 * AbstractPanelController is the abstract base class for all the panel 
 * controllers of Scene Builder Kit.
 * <p>
 * At instantiation time, each panel controller is passed a reference to its 
 * editor controller which is hold in <code>editorController</code>.
 * <p>
 * Subclasses must provide three methods:
 * <ul>
 * <li><code>makePanel</code> must create the FX components
 * which compose the panel
 * <li><code>fxomDocumentDidChange</code> must keep the panel up to date
 * after the editor controller has changed the base document
 * <li><code>editorSelectionDidChange</code> must keep the panel up to date
 * after the editor controller has changed the selected objects.
 * </ul>
 * 
 * 
 */
public abstract class AbstractPanelController {
    
    private static final Logger LOG = Logger.getLogger(AbstractPanelController.class.getName());
    
    private final EditorController editorController;
    private Parent panelRoot;
    
    /**
     * Base constructor for invocation by the subclasses.
     * Subclass implementations should make sure that this constructor can be
     * invoked outside of the JavaFX thread.
     * 
     * @param c the editor controller (should not be null).
     */
    protected AbstractPanelController(EditorController c) {
        assert c != null;
        this.editorController = c;
        startListeningToEditorSelection();
        startListeningToJobManagerRevision();
        editorController.fxomDocumentProperty().addListener(new ChangeListener<FXOMDocument>() {
            @Override
            public void changed(ObservableValue<? extends FXOMDocument> ov, FXOMDocument od, FXOMDocument nd) {
                assert editorController.getFxomDocument() == nd;
                if (od != null) {
                    od.sceneGraphRevisionProperty().removeListener(fxomDocumentRevisionListener);
                    od.cssRevisionProperty().removeListener(cssRevisionListener);
                }
                try {
                    fxomDocumentDidChange(od);
                } catch(RuntimeException x) {
                    LOG.log(Level.SEVERE, "Bug", x); //NOI18N
                }
                if (nd != null) {
                    nd.sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
                    nd.cssRevisionProperty().addListener(cssRevisionListener);
                }
            }
        });
        if (editorController.getFxomDocument() != null) {
            editorController.getFxomDocument().sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
            editorController.getFxomDocument().cssRevisionProperty().addListener(cssRevisionListener);
        }
        editorController.toolStylesheetProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String od, String nd) {
                toolStylesheetDidChange(od);
            }
        });
    }
    
    /**
     * Returns the editor controller associated to this panel controller.
     * 
     * @return the editor controller (never null).
     */
    public EditorController getEditorController() {
        return editorController;
    }
    
    /**
     * Returns the root FX object of this panel.
     * When called the first time, this method invokes {@link #makePanel()}
     * to build the FX components of the panel.
     * 
     * @return the root object of the panel (never null)
     */
    public Parent getPanelRoot() {
        if (panelRoot == null) {
            makePanel();
            assert panelRoot != null;
            
            // Installs the stylesheet from the editor controller
            final List<String> stylesheets = panelRoot.getStylesheets();
            if (stylesheets.contains(EditorController.getBuiltinToolStylesheet())) {
                toolStylesheetDidChange(EditorController.getBuiltinToolStylesheet());
            } else {
                toolStylesheetDidChange(null);
            }
        }
        
        return panelRoot;
    }
    
    /*
     * To be implemented by subclasses
     */
    
    /**
     * Creates the FX object composing the panel.
     * This routine is called by {@link AbstractPanelController#getPanelRoot}.
     * It *must* invoke {@link AbstractPanelController#setPanelRoot}.
     */
    protected abstract void makePanel();
    
    /**
     * Updates the panel after the editor controller has change
     * the base document. Subclass can use {@link EditorController#getFxomDocument() } 
     * to retrieve the newly set document (possibly null).
     * 
     * @param oldDocument the previous document (possibly null).
     */
    protected abstract void fxomDocumentDidChange(FXOMDocument oldDocument);
    
    /**
     * Updates the panel after the revision of the scene graph has changed.
     * Revision is incremented each time the fxom document rebuilds the
     * scene graph.
     */
    protected abstract void sceneGraphRevisionDidChange();
    
    /**
     * Updates the panel after the css revision has changed.
     * Revision is incremented each time the fxom document forces FX to
     * reload its stylesheets.
     */
    protected abstract void cssRevisionDidChange();
    
    /**
     * Updates the panel after the revision of job manager has changed.
     * Revision is incremented each time a job is executed, undone or redone.
     */
    protected abstract void jobManagerRevisionDidChange();
    
    /**
     * Updates the panel after the editor controller has changed the selected
     * objects. Subclass can use {@link EditorController#getSelection()} to
     * retrieve the currently selected objects.
     */
    protected abstract void editorSelectionDidChange();
    
    
    /*
     * For subclasses
     */
    
    /**
     * Set the root of this panel controller.
     * This routine must be invoked by subclass's makePanel() routine.
     * 
     * @param panelRoot the root panel (non null).
     */
    protected  final void setPanelRoot(Parent panelRoot) {
        assert panelRoot != null;
        this.panelRoot = panelRoot;
    }
    
    private final ChangeListener<Number> fxomDocumentRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    try {
                        sceneGraphRevisionDidChange();
                    } catch(RuntimeException x) {
                        LOG.log(Level.SEVERE, "Bug", x); //NOI18N
                    }
                }
            };
    
    private final ChangeListener<Number> cssRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    try {
                        cssRevisionDidChange();
                    } catch(RuntimeException x) {
                        LOG.log(Level.SEVERE, "Bug", x); //NOI18N
                    }
                }
            };
    
    private final ChangeListener<Number> jobManagerRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    try {
                        jobManagerRevisionDidChange();
                    } catch(RuntimeException x) {
                        LOG.log(Level.SEVERE, "Bug", x); //NOI18N
                    }
                }
            };
    
    private final ChangeListener<Number> editorSelectionListener = 
        new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                try {
                    assert editorController.getSelection().isValid(editorController.getFxomDocument());
                    editorSelectionDidChange();
                } catch(RuntimeException x) {
                    LOG.log(Level.SEVERE, "Bug", x); //NOI18N
                }
            }
        };
    
    /**
     * Setup a listener which invokes {@link #editorSelectionDidChange} each
     * time the editor controller changes the selected objects.
     * This routine is automatically called when the panel controller is 
     * instantiated. Subclasses may invoke it after temporarily disabling 
     * selection listening with {@link AbstractPanelController#stopListeningToEditorSelection}.
     */
    protected final void startListeningToEditorSelection() {
        editorController.getSelection().revisionProperty().addListener(editorSelectionListener);
    }
    
    /**
     * Removes the listener which invokes {@link #editorSelectionDidChange} each
     * time the editor controller changes the selected objects.
     * Subclasses may invoke this routine to temporarily stop listening to 
     * the selection changes from the editor controller. Use
     * {@link AbstractPanelController#startListeningToEditorSelection} to 
     * re-enable selection listening.
     */
    protected final void stopListeningToEditorSelection() {
        editorController.getSelection().revisionProperty().removeListener(editorSelectionListener);
    }
    
    
    /**
     * Setup a listener which invokes {@link #jobManagerRevisionDidChange} each
     * time the job manager has executed, undone or redone a job.
     * This routine is automatically called when the panel controller is 
     * instantiated. Subclasses may invoke it after temporarily disabling 
     * job manager listening with {@link AbstractPanelController#stopListeningToJobManagerRevision}.
     */
    protected final void startListeningToJobManagerRevision() {
        editorController.getJobManager().revisionProperty().addListener(jobManagerRevisionListener);
    }

    
    /**
     * Removes the listener which invokes {@link #jobManagerRevisionDidChange} each
     * time the job manager has executed, undone or redone a job.
     * Subclasses may invoke this routine to temporarily stop listening to 
     * the job manager from the editor controller. Use
     * {@link AbstractPanelController#startListeningToJobManagerRevision} to 
     * re-enable job manager listening.
     */
    protected final void stopListeningToJobManagerRevision() {
        editorController.getJobManager().revisionProperty().removeListener(jobManagerRevisionListener);
    }
    
    
    /*
     * Private
     */
    
    private void toolStylesheetDidChange(String oldStylesheet) {
        /*
         * Tool style sheet has changed in editor controller.
         * If the panel has been loaded, then we replace the old sheet
         * by the new one in the stylesheets property of its root object.
         */
        if (panelRoot != null) {
            final List<String> stylesheets = panelRoot.getStylesheets();
            if (oldStylesheet != null) {
                stylesheets.remove(oldStylesheet);
            }
            stylesheets.add(editorController.getToolStylesheet());
        }
    }
    
    
}
