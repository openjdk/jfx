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
package com.oracle.javafx.scenebuilder.kit.glossary;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * 
 */
public class BuiltinGlossary extends Glossary {
    
    public BuiltinGlossary() {
    }

    /*
     * Glossary
     */
    
    @Override
    public List<String> queryControllerClasses(URL fxmlLocation) {
        if (fxmlLocation == null ) {
            return Collections.emptyList();
        } else {
            File fxmlFile = getFileFromURL(fxmlLocation);
            if (! fxmlFile.exists()) {
                // Suspicious ! May I print some warning ? or assert the file exists ?
                return Collections.emptyList();
            } else {
                List<String> res = new ArrayList<>();
                for (ControllerClass cc : ControllerClass.discoverFXMLControllerClasses(fxmlFile)) {
                    if (! res.contains(cc.getClassName())) {
                        res.add(cc.getClassName());
                    }
                }
                Collections.sort(res);
                return res;
            }
        }
    }

    @Override
    public List<String> queryFxIds(URL fxmlLocation, String controllerClass, Class<?> targetType) {
        // TODO fix DTL-5878
        assert controllerClass != null;
        if (fxmlLocation == null ) {
            return Collections.emptyList();
        } else {
            File fxmlFile = getFileFromURL(fxmlLocation);
            if (! fxmlFile.exists()) {
                // Suspicious ! May I print some warning ? or assert the file exists ?
                return Collections.emptyList();
            } else {
                List<String> res = new ArrayList<>();
                for (ControllerClass cc : ControllerClass.discoverFXMLControllerClasses(fxmlFile)) {
                    if (controllerClass.equals(cc.getClassName())) {
                        res.addAll(cc.getFxIds());
                        break;  // discoverFXMLControllerClasses may return duplicates.
                                // The first matching class name is good enough for now.
                    }
                }
                Collections.sort(res);
                return res;
            }
        }
    }
    
    @Override
    public List<String> queryEventHandlers(URL fxmlLocation, String controllerClass) {
        assert controllerClass != null;
        if (fxmlLocation == null ) {
            return Collections.emptyList();
        } else {
            File fxmlFile = getFileFromURL(fxmlLocation);
            if (! fxmlFile.exists()) {
                // Suspicious ! May I print some warning ? or assert the file exists ?
                return Collections.emptyList();
            } else {
                List<String> res = new ArrayList<>();
                for (ControllerClass cc : ControllerClass.discoverFXMLControllerClasses(fxmlFile)) {
                    if (controllerClass.equals(cc.getClassName())) {
                        res.addAll(cc.getEventHandlers());
                        break;  // discoverFXMLControllerClasses may return duplicates.
                                // The first matching class name is good enough for now.
                    }
                }
                Collections.sort(res);
                return res;
            }
        }
    }
    
    // It's better to use URL.toURI than URL.getPath to feed File constructor.
    private File getFileFromURL(URL location) {
        File res;
        
        try {
            res= new File(location.toURI());
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Bug", ex); //NOI18N
        }
        
        return res;
    }
}
