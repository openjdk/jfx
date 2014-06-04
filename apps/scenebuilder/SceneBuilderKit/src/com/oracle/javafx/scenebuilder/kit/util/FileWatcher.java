/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;

/**
 *
 */
public class FileWatcher {
    
    private final String name;
    private final Set<Path> targets = new HashSet<>();
    private final Map<Path, FileTime> modifiedTimes = new HashMap<>();
    private final long pollingTime; // milliseconds
    private final Delegate delegate;
    private boolean started;
    private Timer watchingTimer;
    
    public FileWatcher(long pollingTime, Delegate delegate, String name) {
        assert pollingTime > 0;
        assert delegate != null;
        assert name != null;
        
        this.pollingTime = pollingTime;
        this.delegate = delegate;
        this.name = getClass().getSimpleName() + "[" + name + "]"; //NOI18N
    }
    
    public synchronized void addTarget(Path target) {
        assert target != null;
        assert targets.contains(target) == false;
        targets.add(target);
        try {
            final FileTime modifiedTime = Files.getLastModifiedTime(target);
            modifiedTimes.put(target, modifiedTime);
        } catch(IOException x) {
            // Nothing special to do here
        }
        updateWatchingTimer();
    }
    
    public synchronized void removeTarget(Path target) {
        assert target != null;
        assert targets.contains(target);
        targets.remove(target);
        modifiedTimes.remove(target);
        updateWatchingTimer();
    }
    
    public synchronized void setTargets(Collection<Path> newTargets) {
        
        final Set<Path> toBeAdded = new HashSet<>();
        toBeAdded.addAll(newTargets);
        toBeAdded.removeAll(targets);
        
        final Set<Path> toBeRemoved = new HashSet<>();
        toBeRemoved.addAll(targets);
        toBeRemoved.removeAll(newTargets);
        
        for (Path target : toBeAdded) {
            addTarget(target);
        }
        
        for (Path target : toBeRemoved) {
            removeTarget(target);
        }
    }
    
    public synchronized Set<Path> getTargets() {
        return Collections.unmodifiableSet(targets);
    }
    
    public synchronized void start() {
        assert isStarted() == false;
        started = true;
        updateWatchingTimer();
    }
    
    public synchronized void stop() {
        assert isStarted() == true;
        started = false;
        updateWatchingTimer();
    }
    
    public synchronized boolean isStarted() {
        return started;
    }
    
    public static interface Delegate {
        public void fileWatcherDidWatchTargetCreation(Path target);
        public void fileWatcherDidWatchTargetDeletion(Path target);
        public void fileWatcherDidWatchTargetModification(Path target);
    }
    
    
    /*
     * Private
     */
    
    private void updateWatchingTimer() {
        final boolean timerNeeded = started && (targets.isEmpty() == false);
        
        if (timerNeeded) {
            if (watchingTimer == null) {
                watchingTimer = new Timer(name, true /* isDaemon */);
                watchingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runWatching();
                    }
                }, pollingTime, pollingTime);
            }
        } else {
            if (watchingTimer != null) {
                watchingTimer.cancel();
                watchingTimer = null;
            }
        }
    }
    
    private synchronized void runWatching() {
        
        // Note : this method may be called a few times after the timer
        // is cancelled by stop() method. In that case, this.watchingTimer is 
        // null and we should simply do nothing.
        
        if (watchingTimer != null) {
            for (Path target : targets) {
                FileTime newModifiedTime;
                try {
                    newModifiedTime = Files.getLastModifiedTime(target);
                } catch(IOException x) {
                    newModifiedTime = null;
                }

                final FileTime lastModifiedTime = modifiedTimes.get(target);

                if ((lastModifiedTime == null) && (newModifiedTime != null)) {
                    // target has been created
                    modifiedTimes.put(target, newModifiedTime);
                    Platform.runLater(() -> delegate.fileWatcherDidWatchTargetCreation(target));
                } else if ((lastModifiedTime != null) && (newModifiedTime == null)) {
                    // target has been deleted
                    modifiedTimes.remove(target);
                    Platform.runLater(() -> delegate.fileWatcherDidWatchTargetDeletion(target));
                } else if (Objects.equals(lastModifiedTime, newModifiedTime) == false) {
                    // target has been modified
                    assert newModifiedTime != null;
                    modifiedTimes.put(target, newModifiedTime);
                    Platform.runLater(() -> delegate.fileWatcherDidWatchTargetModification(target));
                }
            }
        }
    }
}
