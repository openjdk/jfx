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
package com.oracle.javafx.scenebuilder.kit.library.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * 
 */
public class JarExplorer {
    
    private final Path jar;
    
    public JarExplorer(Path jar) {
        assert jar != null;
        assert jar.isAbsolute();
        
        this.jar = jar;
    }
    
    public JarReport explore(ClassLoader classLoader) throws IOException {
        final JarReport result = new JarReport(jar);
        
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            final Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                final JarEntry entry = e.nextElement();
                result.getEntries().add(exploreEntry(entry, classLoader));
            }
        }
        
        return result;
    }
    
    public static String makeFxmlText(Class<?> klass) {
        final StringBuilder result = new StringBuilder();
        
        /*
         * <?xml version="1.0" encoding="UTF-8"?> //NOI18N
         * 
         * <?import a.b.C?>
         * 
         * <C/>
         */
        
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //NOI18N
        
        result.append("<?import "); //NOI18N
        result.append(klass.getCanonicalName());
        result.append("?>"); //NOI18N
        result.append("<"); //NOI18N
        result.append(klass.getSimpleName());
        result.append("/>\n"); //NOI18N
        
        return result.toString();
    }
    
    
    public static Object instantiateWithFXMLLoader(Class<?> klass, ClassLoader classLoader) throws IOException {
        Object result;
        
        final String fxmlText = makeFxmlText(klass);
        final byte[] fxmlBytes = fxmlText.getBytes(Charset.forName("UTF-8")); //NOI18N

        final FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            fxmlLoader.setClassLoader(classLoader);
            result = fxmlLoader.load(new ByteArrayInputStream(fxmlBytes));
        } catch(IOException x) {
            throw x;
        } catch(RuntimeException|Error x) {
            throw new IOException(x);
        }
        
        return result;
    }
    
    /*
     * Private
     */
    
    private JarReportEntry exploreEntry(JarEntry entry, ClassLoader classLoader) {
        JarReportEntry.Status status;
        Throwable entryException;
        Class<?> entryClass;
        
        if (entry.isDirectory()) {
            status = JarReportEntry.Status.IGNORED;
            entryClass = null;
            entryException = null;
        } else {
            final String className = makeClassName(entry.getName());
            if (className == null || className.startsWith("java.") //NOI18N
                    || className.startsWith("javax.") || className.startsWith("javafx.")) { //NOI18N
                status = JarReportEntry.Status.IGNORED;
                entryClass = null;
                entryException = null;
            } else {
                try {
                    // Some reading explaining why using Class.forName is not appropriate:
                    // http://blog.osgi.org/2011/05/what-you-should-know-about-class.html
                    // http://blog.bjhargrave.com/2007/09/classforname-caches-defined-class-in.html
                    // http://stackoverflow.com/questions/8100376/class-forname-vs-classloader-loadclass-which-to-use-for-dynamic-loading
                    entryClass = classLoader.loadClass(className); // Note: static intializers of entryClass are not run, this doesn't seem to be an issue
                    try {
                        instantiateWithFXMLLoader(entryClass, classLoader);
                        status = JarReportEntry.Status.OK;
                        entryException = null;
                    } catch(RuntimeException|IOException x) {
                        status = JarReportEntry.Status.CANNOT_INSTANTIATE;
                        entryException = x;
                    }
                } catch(Error | ClassNotFoundException x) {
                    status = JarReportEntry.Status.CANNOT_LOAD;
                    entryClass = null;
                    entryException = x;
                }
            }
        }
        
        return new JarReportEntry(entry.getName(), status, entryException, entryClass);
    }
    
    
    private String makeClassName(String entryName) {
        final String result;
        
        if (entryName.endsWith(".class") == false) { //NOI18N
            result = null;
        } else if (entryName.indexOf("$") != -1) { //NOI18N
            // We skip inner classes for now
            result = null;
        } else {
            final int endIndex = entryName.length()-6; // ".class" -> 6 //NOI18N
            result = entryName.substring(0, endIndex).replace("/", "."); //NOI18N
        }
        
        return result;
    }
}
