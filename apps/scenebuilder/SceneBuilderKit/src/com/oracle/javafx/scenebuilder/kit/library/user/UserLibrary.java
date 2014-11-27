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
package com.oracle.javafx.scenebuilder.kit.library.user;

import com.oracle.javafx.scenebuilder.kit.library.BuiltinSectionComparator;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * 
 */
public class UserLibrary extends Library {
    
    public enum State { READY, WATCHING };
    public static final String TAG_USER_DEFINED = "Custom"; //NOI18N
    
    private final String path;
    private final BuiltinSectionComparator sectionComparator
            = new BuiltinSectionComparator();
    
    private final ObservableList<JarReport> jarReports = FXCollections.observableArrayList();
    private final ObservableList<JarReport> previousJarReports = FXCollections.observableArrayList();
    private final ObservableList<Path> fxmlFileReports = FXCollections.observableArrayList();
    private final ObservableList<Path> previousFxmlFileReports = FXCollections.observableArrayList();
    private final SimpleIntegerProperty explorationCountProperty = new SimpleIntegerProperty();
    private final SimpleObjectProperty<Date> explorationDateProperty = new SimpleObjectProperty<>();

    private State state = State.READY;
    private Exception exception;
    private LibraryFolderWatcher watcher;
    private Thread watcherThread;
    // Where we store canonical class names of items we want to exclude from
    // the user defined one displayed in the Library panel.
    // As a consequence an empty file means we display all items.
    private final String filterFileName = "filter.txt"; //NOI18N
    
    
    /*
     * Public
     */
    
    public UserLibrary(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public ObservableList<JarReport> getJarReports() {
        return jarReports;
    }
    
    public ObservableList<JarReport> getPreviousJarReports() {
        return previousJarReports;
    }
    
    public ObservableList<Path> getFxmlFileReports() {
        return fxmlFileReports;
    }
    
    public ObservableList<Path> getPreviousFxmlFileReports() {
        return previousFxmlFileReports;
    }
    
    public synchronized State getState() {
        return state;
    }
    
    public synchronized void startWatching() {
        assert state == State.READY;
        
        if (state == State.READY) {
            assert watcher == null;
            assert watcherThread == null;

            watcher = new LibraryFolderWatcher(this);
            watcherThread = new Thread(watcher);
            watcherThread.setName(watcher.getClass().getSimpleName() + "(" + path  + ")"); //NOI18N
            watcherThread.setDaemon(true);
            watcherThread.start();
            state = State.WATCHING;
        }
    }
    
    public synchronized void stopWatching() {
        assert state == State.WATCHING;
        
        if (state == State.WATCHING) {
            assert watcher != null;
            assert watcherThread != null;
            assert exception == null;
            
            watcherThread.interrupt();
            
            try {
                watcherThread.join();
            } catch(InterruptedException x) {
                x.printStackTrace();
            } finally {
                watcher = null;
                watcherThread = null;
                state = State.READY;
                
                // In READY state, we release the class loader.
                // This enables library import to manipulate jar files.
                changeClassLoader(null);
                previousJarReports.clear();
            }
        }
    }
    
    public int getExplorationCount() {
        return explorationCountProperty.get();
    }
    
    public ReadOnlyIntegerProperty explorationCountProperty() {
        return explorationCountProperty;
    }
    
    public Object getExplorationDate() {
        return explorationDateProperty.get();
    }

    public ReadOnlyObjectProperty<Date> explorationDateProperty() {
        return explorationDateProperty;
    }
    
    public void setFilter(List<String> classnames) throws FileNotFoundException, IOException {
        if (classnames != null && classnames.size() > 0) {
            File filterFile = new File(getFilterFileName());
            // TreeSet to get natural order sorting and no duplicates
            TreeSet<String> allClassnames = new TreeSet<>();

            for (String classname : classnames) {
                allClassnames.add(classname);
            }
            
            Path filterFilePath = Paths.get(getPath(), filterFileName);
            Path formerFilterFilePath = Paths.get(getPath(), filterFileName + ".tmp"); //NOI18N
            Files.deleteIfExists(formerFilterFilePath);

            try {
                // Rename already existing filter file so that we can rollback
                if (Files.exists(filterFilePath)) {
                    Files.move(filterFilePath, formerFilterFilePath, StandardCopyOption.ATOMIC_MOVE);
                }

                // Create the new filter file
                Files.createFile(filterFilePath);

                // Write content of the new filter file
                try (PrintWriter writer = new PrintWriter(filterFile, "UTF-8")) { //NOI18N
                    for (String classname : allClassnames) {
                        writer.write(classname + "\n"); //NOI18N
                    }
                }

                // Delete the former filter file
                if (Files.exists(formerFilterFilePath)) {
                    Files.delete(formerFilterFilePath);
                }
            } catch (IOException ioe) {
                // Rollback
                if (Files.exists(formerFilterFilePath)) {
                    Files.move(formerFilterFilePath, filterFilePath, StandardCopyOption.ATOMIC_MOVE);
                }
                throw (ioe);
            }
        }
    }
    
    public List<String> getFilter() throws FileNotFoundException, IOException {
        List<String> res = new ArrayList<>();
        File filterFile = new File(getFilterFileName());

        if (filterFile.exists()) {
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(filterFile), "UTF-8"))) { //NOI18N
                String line;
                while ((line = reader.readLine()) != null) {
                    res.add(line);
                }
            }
        }

        return res;
    }

    /*
     * Package
     */
    
    String getFilterFileName() {
        return getPath() + File.separator + filterFileName;
    }
    
    void updateJarReports(Collection<JarReport> newJarReports) {
        if (Platform.isFxApplicationThread()) {
            previousJarReports.setAll(jarReports);
            jarReports.setAll(newJarReports);
        } else {
            Platform.runLater(() -> {
                previousJarReports.setAll(jarReports);
                jarReports.setAll(newJarReports);
            });
        }
    }
    
    void updateFxmlFileReports(Collection<Path> newFxmlFileReports) {
        if (Platform.isFxApplicationThread()) {
            previousFxmlFileReports.setAll(fxmlFileReports);
            fxmlFileReports.setAll(newFxmlFileReports);
        } else {
            Platform.runLater(() -> {
                previousFxmlFileReports.setAll(fxmlFileReports);
                fxmlFileReports.setAll(newFxmlFileReports);
            });
        }
    }
    
    void setItems(Collection<LibraryItem> items) {
        if (Platform.isFxApplicationThread()) {
            itemsProperty.setAll(items);
        } else {
            Platform.runLater(() -> itemsProperty.setAll(items));
        }
    }
    
    void addItems(Collection<LibraryItem> items) {
        if (Platform.isFxApplicationThread()) {
            itemsProperty.addAll(items);
        } else {
            Platform.runLater(() -> itemsProperty.addAll(items));
        }
    }
    
    void updateClassLoader(ClassLoader newClassLoader) {
        if (Platform.isFxApplicationThread()) {
            changeClassLoader(newClassLoader);
        } else {
            Platform.runLater(() -> changeClassLoader(newClassLoader));
        }
    }
    
    void updateExplorationCount(int count) {
        if (Platform.isFxApplicationThread()) {
            explorationCountProperty.set(count);
        } else {
            Platform.runLater(() -> explorationCountProperty.set(count));
        }
    }
    
    void updateExplorationDate(Date date) {
        if (Platform.isFxApplicationThread()) {
            explorationDateProperty.set(date);
        } else {
            Platform.runLater(() -> explorationDateProperty.set(date));
        }
    }
    
    /*
     * Library
     */
    @Override
    public Comparator<String> getSectionComparator() {
        return sectionComparator;
    }
    
    /*
     * Private
     */
    
    private void changeClassLoader(ClassLoader newClassLoader) {
        assert Platform.isFxApplicationThread();
        
        /*
         * Before changing to the new class loader,
         * we invoke URLClassLoader.close() on the existing one
         * so that it releases its associated jar files.
         */
        final ClassLoader classLoader = classLoaderProperty.get();
        if (classLoader instanceof URLClassLoader) {
            final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            try {
                urlClassLoader.close();
            } catch(IOException x) {
                x.printStackTrace();
            }
        }
        
        // Now moves to the new class loader
        classLoaderProperty.set(newClassLoader);
    }
    
    /*
     * Debug
     */
    
    public static void main(String[] args) throws Exception {
        final String path = "/Users/elp/Desktop/MyLib"; //NOI18N
        final UserLibrary lib = new UserLibrary(path);
        lib.startWatching();
        System.out.println("Starting to watch for 20 s"); //NOI18N
        Thread.sleep(20 * 1000);
        System.out.println("Stopping to watch for 20 s"); //NOI18N
        lib.stopWatching();
        Thread.sleep(20 * 1000);
        System.out.println("Exiting"); //NOI18N
    }
}
