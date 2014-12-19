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

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReport;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReportEntry;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * 
 */
class LibraryFolderWatcher implements Runnable {
    
    private final UserLibrary library;
    private enum FILE_TYPE {FXML, JAR};
    
    public LibraryFolderWatcher(UserLibrary library) {
        this.library = library;
    }

    /*
     * Runnable
     */
    
    @Override
    public void run() {
        
        try {
            library.updateExplorationCount(0);
            library.updateExplorationDate(new Date());
            runDiscovery();
            runWatching();
        } catch(InterruptedException x) {
            // Let's stop
        }
    }
    
    
    /*
     * Private
     */
    
    
    private void runDiscovery() throws InterruptedException {
        final Path folder = Paths.get(library.getPath());
       
        // First put the builtin items in the library
        library.setItems(BuiltinLibrary.getLibrary().getItems());

        // Now attempts to discover the user library folder
        boolean retry;
        do {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
                final Set<Path> currentJars = new HashSet<>();
                final Set<Path> currentFxmls = new HashSet<>();
                for (Path entry: stream) {
                    if (isJarPath(entry)) {
                        currentJars.add(entry);
                    } else if (isFxmlPath(entry)) {
                        currentFxmls.add(entry);
                    }
                }
               
                updateLibrary(currentFxmls);
                exploreAndUpdateLibrary(currentJars);
                retry = false;
            } catch(IOException x) {
                Thread.sleep(2000 /* ms */);
                retry = true;
            } finally {
                library.updateExplorationCount(library.getExplorationCount()+1);
            }
        }
        while (retry);
        
    }
    
    private void runWatching() throws InterruptedException {
        while (true) {
            final Path folder = Paths.get(library.getPath());

            WatchService watchService = null;
            while (watchService == null) {
                try {
                    watchService = folder.getFileSystem().newWatchService();
                } catch(IOException x) {
                    System.out.println("FileSystem.newWatchService() failed"); //NOI18N
                    System.out.println("Sleeping..."); //NOI18N
                    Thread.sleep(1000 /* ms */);
                }
            }

            WatchKey watchKey = null;
            while ((watchKey == null) || (watchKey.isValid() == false)) {
                try {
                    watchKey = folder.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE, 
                    StandardWatchEventKinds.ENTRY_DELETE, 
                    StandardWatchEventKinds.ENTRY_MODIFY);

                    WatchKey wk;
                    do {
                        wk = watchService.take();
                        assert wk == watchKey;
                        
                        boolean isDirty = false;
                        for (WatchEvent<?> e: wk.pollEvents()) {
                            final WatchEvent.Kind<?> kind = e.kind();
                            final Object context = e.context();

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                    || kind == StandardWatchEventKinds.ENTRY_DELETE
                                    || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                assert context instanceof Path;
                                if (isJarPath((Path)context) || isFxmlPath((Path)context)) {
                                    isDirty = true;
                                }
                            } else {
                                assert kind == StandardWatchEventKinds.OVERFLOW;
                            }
                        }
                                                
                        // We reconstruct a full set from scratch as soon as the
                        // dirty flag is set.
                        if (isDirty) {
                            // First put the builtin items in the library
                            library.setItems(BuiltinLibrary.getLibrary().getItems());
                            
                            final Set<Path> fxmls = new HashSet<>();
                            fxmls.addAll(getAllFiles(FILE_TYPE.FXML));
                            updateLibrary(fxmls);

                            final Set<Path> jars = new HashSet<>();
                            jars.addAll(getAllFiles(FILE_TYPE.JAR));
                            exploreAndUpdateLibrary(jars);
                            
                            library.updateExplorationCount(library.getExplorationCount()+1);
                        }
                    } while (wk.reset());
                } catch(IOException x) {
                    Thread.sleep(1000 /* ms */);
                }
            }

        }
    }
    
    
    private Set<Path> getAllFiles(FILE_TYPE fileType) throws IOException {
        Set<Path> res = new HashSet<>();
        final Path folder = Paths.get(library.getPath());

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path p : ds) {
                switch (fileType) {
                    case FXML:
                        if (isFxmlPath(p)) {
                            res.add(p);
                        }
                        break;
                    case JAR:
                        if (isJarPath(p)) {
                            res.add(p);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return res;
    }
    
    
    private static boolean isJarPath(Path path) {
        final String pathString = path.toString().toLowerCase(Locale.ROOT);
        return pathString.endsWith(".jar"); //NOI18N
    }
    
    
    private static boolean isFxmlPath(Path path) {
        final String pathString = path.toString().toLowerCase(Locale.ROOT);
        return pathString.endsWith(".fxml"); //NOI18N
    }
    
    
    private void updateLibrary(Collection<Path> paths) throws IOException {
        final List<LibraryItem> newItems = new ArrayList<>();
        
        for (Path path : paths) {
            newItems.add(makeLibraryItem(path));
        }

        library.addItems(newItems);
        library.updateFxmlFileReports(paths);
        library.updateExplorationDate(new Date());
    }
    
    
    private LibraryItem makeLibraryItem(Path path) throws IOException {
        final URL iconURL = ImageUtils.getNodeIconURL(null);
        String fileName = path.getFileName().toString();
        String itemName = fileName.substring(0, fileName.indexOf(".fxml")); //NOI18N
        String fxmlText = ""; //NOI18N
        StringBuilder buf = new StringBuilder();

        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(path.toFile()), "UTF-8"))) { //NOI18N
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line).append("\n"); //NOI18N
            }
            
            fxmlText = buf.toString();
        }

        final LibraryItem res = new LibraryItem(itemName, UserLibrary.TAG_USER_DEFINED, fxmlText, iconURL, library);
        return res;
    }
    
    
    private void exploreAndUpdateLibrary(Collection<Path> jars) throws IOException {

        //  1) we create a classloader
        //  2) we explore all the jars
        //  3) we construct a list of library items
        //  4) we update the user library with the class loader and items

        // 1)
        final ClassLoader classLoader;
        if (jars.isEmpty()) {
            classLoader = null;
        } else {
            classLoader = new URLClassLoader(makeURLArrayFromPaths(jars));
        }

        // 2)
        final List<JarReport> jarReports = new ArrayList<>();
        for (Path currentJar : jars) {
            Logger.getLogger(this.getClass().getSimpleName()).info(I18N.getString("log.info.explore.jar", currentJar));
            final JarExplorer explorer = new JarExplorer(currentJar);
            jarReports.add(explorer.explore(classLoader));
        }

        // 3)
        final List<LibraryItem> newItems = new ArrayList<>();
        for (JarReport jarReport : jarReports) {
            newItems.addAll(makeLibraryItems(jarReport));
        }

        // 4)
        library.updateClassLoader(classLoader);
        library.addItems(newItems);
        library.updateJarReports(new ArrayList<>(jarReports));
        library.updateExplorationDate(new Date());
    }
    
    
    private Collection<LibraryItem> makeLibraryItems(JarReport jarReport) throws IOException {
        final List<LibraryItem> result = new ArrayList<>();
        final URL iconURL = ImageUtils.getNodeIconURL(null);
        final List<String> excludedItems = library.getFilter();
                
        for (JarReportEntry e : jarReport.getEntries()) {
            if ((e.getStatus() == JarReportEntry.Status.OK) && e.isNode()) {
                // We filter out items listed in the excluded list, based on canonical name of the class.
                final String canonicalName = e.getKlass().getCanonicalName();
                if (! excludedItems.contains(canonicalName)) {
                    final String name = e.getKlass().getSimpleName();
                    final String fxmlText = JarExplorer.makeFxmlText(e.getKlass());
                    result.add(new LibraryItem(name, UserLibrary.TAG_USER_DEFINED, fxmlText, iconURL, library));
                }
            }
        }
        
        return result;
    }
    
    
    private URL[] makeURLArrayFromPaths(Collection<Path> paths) {
        final URL[] result = new URL[paths.size()];
        int i = 0;
        for (Path p : paths) {
            try {
                result[i++] = p.toUri().toURL();
            } catch(MalformedURLException x) {
                throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
            }
        }
        
        return result;
    }
    
}
