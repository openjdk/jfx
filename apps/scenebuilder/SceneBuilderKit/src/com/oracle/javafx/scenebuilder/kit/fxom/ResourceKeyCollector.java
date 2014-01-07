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
package com.oracle.javafx.scenebuilder.kit.fxom;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;

/**
 *
 * 
 */
class ResourceKeyCollector extends ResourceBundle {
    
    private final ResourceBundle backingResources;
    
    public ResourceKeyCollector(ResourceBundle backingResources) {
        this.backingResources = backingResources;
    }
            
    /*
     * ResourceBundle
     */
    
    @Override
    protected Object handleGetObject(String key) {
        Object result;
        
        if (backingResources != null) {
            try {
                result = backingResources.getObject(key);
            } catch(MissingResourceException x) {
                result = null;
            }
        } else {
            result = null;
        }
        if (result == null) {
            result = FXMLLoader.RESOURCE_KEY_PREFIX + key;
        }
        
        return result;
    }

    @Override
    public Enumeration<String> getKeys() {
        return new EnumerationFromIterator<>(Collections.emptyListIterator());
    }

    @Override
    public boolean containsKey(String key) {
        return true;
    }


    private static class EnumerationFromIterator<T> implements Enumeration<T> {
        
        private final Iterator<T> iterator;
        
        public EnumerationFromIterator(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }
        
    }
}
