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
package com.oracle.javafx.scenebuilder.kit.editor;

import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.reference.UpdateReferencesJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * @treatAsPrivate 
 */
public class JobManager {
    
    private final EditorController editorController;
    private final int undoStackMaxSize;
    private final List<Job> undoStack = new ArrayList<>();
    private final List<Job> redoStack = new ArrayList<>();
    private final SimpleIntegerProperty revision = new SimpleIntegerProperty();
    private boolean lock;
    
    
    JobManager(EditorController editorController, int undoStackMaxSize) {
        assert editorController != null;
        assert undoStackMaxSize >= 1;
        this.editorController = editorController;
        this.undoStackMaxSize = undoStackMaxSize;
    }
    
    
    public List<Job> getUndoStack() {
        return Collections.unmodifiableList(undoStack);
    }
    
    public List<Job> getRedoStack() {
        return Collections.unmodifiableList(redoStack);
    }
    
    public void push(Job job) {
        assert job != null;
        assert job.getEditorController() == editorController;
        assert job.isExecutable();
        
        if (lock) {
            // Method is called from a revision property listener
            throw new IllegalStateException("Pushing jobs from another job or a job manager listener is forbidden"); //NOI18N
        }
        
        final Job fixJob = new UpdateReferencesJob(job);
        executeJob(fixJob);
        undoStack.add(0, fixJob);
        if (undoStack.size() > undoStackMaxSize) {
            undoStack.remove(undoStack.size()-1);
        }
        redoStack.clear();
        incrementRevision();
        
    }
    
    public void clear() {
        if (lock) {
            // Method is called from a revision property listener
            throw new IllegalStateException("Clearing job stack from another job or a job manager listener is forbidden"); //NOI18N
        }
        
        undoStack.clear();
        redoStack.clear();
        // We don't change the revision.
    }
    
    public boolean canUndo() {
        return undoStack.isEmpty() == false;
    }
    
    public String getUndoDescription() {
        final String result;
        if (canUndo()) {
            result = undoStack.get(0).getDescription();
        } else {
            result = null;
        }
        return result;
    }
    
    public void undo() {
        assert canUndo();
        
        if (lock) {
            // Method is called from a revision property listener
            throw new IllegalStateException("Undoing jobs from another job or a job manager listener is forbidden"); //NOI18N
        }
        
        final Job job = undoStack.get(0);
        undoJob(job);
        undoStack.remove(0);
        redoStack.add(0, job);
        incrementRevision();
    }
    
    public boolean canRedo() {
        return redoStack.isEmpty() == false;
    }
    
    public String getRedoDescription() {
        final String result;
        if (canRedo()) {
            result = redoStack.get(0).getDescription();
        } else {
            result = null;
        }
        return result;
    }
    
    public void redo() {
        assert canRedo();
        
        if (lock) {
            // Method is called from a revision property listener
            throw new IllegalStateException("Redoing jobs from another job or a job manager listener is forbidden"); //NOI18N
        }
        
        final Job job = redoStack.get(0);
        redoJob(job);
        redoStack.remove(0);
        undoStack.add(0, job);
        incrementRevision();
    }
    
    /**
     * Returns the property holding the revision number of this job manager.
     * Job manager adds +1 to this number each time a job is done or undone.
     * 
     * @return the property holding the revision number of this job manager.
     */
    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }
    
    /**
     * Returns the job which has just been processed and which can be undone.
     * 
     * @return the current job, which is the one at index 0 in the undo stack.
     * It can be null.
     */
    public Job getCurrentJob() {
        if (undoStack.size() > 0) {
            return undoStack.get(0);
        } else {
            return null;
        }
    }
    
    
    /*
     * Private
     */
    
    private void executeJob(Job job) {
        lock = true;
        try {
            job.execute();
        } finally {
            lock = false;
        }
    }
    
    
    private void undoJob(Job job) {
        lock = true;
        try {
            job.undo();
        } finally {
            lock = false;
        }
    }
    
    
    private void redoJob(Job job) {
        lock = true;
        try {
            job.redo();
        } finally {
            lock = false;
        }
    }
    
    
    private void incrementRevision() {
        lock = true;
        try {
            revision.set(revision.get()+1);
        } finally {
            lock = false;
        }
    }
}
