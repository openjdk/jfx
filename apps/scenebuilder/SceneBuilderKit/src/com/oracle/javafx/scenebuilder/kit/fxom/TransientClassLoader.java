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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class loader delegates all its tasks to its parent class loader but
 * overrides ClassLoader.getResource().
 * 
 * The getResource() invokes the parent class loader implementation and checks
 * if the result is null or not. When null (ie no existing resource match), it
 * returns a dummy URL in place of null.
 * 
 * This class loader is instantiated by FXOMLoader and passed to FXMLLoader.
 * It avoids FXMLLoader to break and interrupt loading when a classpath relative
 * URL is unresolved.
 */
class TransientClassLoader extends ClassLoader {
    
    public TransientClassLoader(ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }
    
    /*
     * ClassLoader
     */
    
    @Override
    public URL getResource(String name) {
        URL  result = super.getResource(name);
        if (result == null) {
            try {
                result = new URL("file", null, name); //NOI18N
            } catch(MalformedURLException x) {
                throw new RuntimeException("Bug", x); //NOI18N
            }
        }
        return result;
    }
}
