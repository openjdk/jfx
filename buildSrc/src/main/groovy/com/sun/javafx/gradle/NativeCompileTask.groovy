/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class NativeCompileTask extends DefaultTask {
    @Optional @Input String matches; // regex for matching input files
    @Input List<String> params = new ArrayList<String>();
    @Input List sourceRoots = new ArrayList();
    @OutputDirectory File output;
    @InputFiles List<File> allFiles = [];
    private final PatternFilterable patternSet = new PatternSet();

    public NativeCompileTask source(Object... sources) {
        for (Object source : sources) {
            if (source instanceof Collection) {
                sourceRoots.addAll((Collection)source);
            } else {
                sourceRoots.add(source);
            }
        }
        updateFiles();
        return this;
    }

    public NativeCompileTask include(String... includes) {
        patternSet.include(includes);
        return this;
    }

    public NativeCompileTask include(Iterable<String> includes) {
        patternSet.include(includes);
        return this;
    }

    private void updateFiles() {
        // Combine the different source roots into a single List<File> based on all files in each source root
        allFiles.clear();
        sourceRoots.each {
            def file = project.file(it);
            if (file && file.exists()) {
                allFiles += file.isDirectory() ? file.listFiles() : file;
            }
        }
    }

    @TaskAction void compile() {
        // Get the existing native-dependencies file from build/dependency-cache and load its contents into
        // memory. If the file doesn't exist, then we will just have an empty dependency map.
        final Map<String, Map> dependencies = new ConcurrentHashMap<>();
        final File nativeDependenciesFile = project.file("$project.buildDir/dependency-cache/native-dependencies-${output.getName()}");
        if (nativeDependenciesFile.exists()) {
            nativeDependenciesFile.splitEachLine("\t", { strings ->
                try {
                    dependencies.put(strings[0], ["DATE":Long.parseLong(strings[1]), "SIZE":Long.parseLong(strings[2])]);
                } catch (Exception e) {
                    // Might fail due to a corrupt native-dependencies file, in which case, we'll just not
                    // do anything which will cause the native code to execute again
                }
            });
        }

        project.mkdir(output);

        // Recompute the allFiles list as the input can come from auto-generated
        // content (HSLS files, for example) which might have changed since
        // the task was configured (i.e. when source() was called).
        updateFiles();
        def source = project.files(allFiles);
        boolean forceCompile = false;
        Set<File> files = new HashSet<File>();
        source.each { File file ->
            final Map fileData = dependencies.get(file.toString());
            final boolean isModified = fileData == null ||
                                       !fileData["DATE"].equals(file.lastModified()) ||
                                       !fileData["SIZE"].equals(file.length());

            if (matches == null || file.name.matches(matches)) {
                // If the source file is not listed in dependencies, then we must compile it.
                // If the target file(s) (.rc or .cur in the case of resources, .pdb or .obj for sources)
                //    do not exist, then compile.
                // If the source file date or size differs from dependencies, then compile it.
                if (isModified) {
                    files += file;
                } else {
                    final File outputFile = outputFile(file);
                    if (!outputFile.exists()) {
                        files += file;
                    }
                }
            } else {
                // This file can be header file or some other type of resource file.
                // Force all source files to be compile.
                if (isModified) {
                    forceCompile = true;
                    //let the iterator finish to update dependencies map
                }
            }
            if (isModified) {
                dependencies.put(file.toString(), ["DATE":file.lastModified(), "SIZE":file.length()]);
            }
        }
        if (forceCompile) {
            files +=  matches == null ? source.files : source.filter{it.name.matches(matches)}.files;
        }

        project.logger.info("Compiling native files: $files");
        final ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(project.NUM_COMPILE_THREADS.toString()));
        final CountDownLatch latch = new CountDownLatch(files.size());
        List futures = new ArrayList<Future>();
        files.each { File sourceFile ->
            futures.add(executor.submit(new Runnable() {
                @Override public void run() {
                    try {
                        final File outputFile = outputFile(sourceFile);
                        doCompile(sourceFile, outputFile)
                    } finally {
                        latch.countDown();
                    }
                }
            }));
        }
        latch.await();
        // Looking for whether an exception occurred while executing any of the futures.
        // By calling "get()" on each future an exception will be thrown if one had occurred
        // on the background thread.
        futures.each {it.get();}

        // Update the native-dependencies file
        if (nativeDependenciesFile.exists()) nativeDependenciesFile.delete();
        nativeDependenciesFile.getParentFile().mkdirs();
        nativeDependenciesFile.createNewFile();
        dependencies.each { key, value ->
            nativeDependenciesFile << key << "\t" << value["DATE"] << "\t" << value["SIZE"] << "\n";
        }
    }

    protected void doCompile(File sourceFile, File outputFile){ }
    protected File outputFile(File sourceFile) { return null; }
}

