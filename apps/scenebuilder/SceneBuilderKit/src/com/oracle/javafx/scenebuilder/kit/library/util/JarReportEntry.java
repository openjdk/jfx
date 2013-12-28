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

import javafx.scene.Node;

/**
 *
 * 
 */
public class JarReportEntry {
    
    public enum Status {
        IGNORED,
        CANNOT_LOAD,
        CANNOT_INSTANTIATE,
        OK
    }
    
    private final String name;
    private final Status status;
    private final Class<?> klass;
    private final Throwable exception;

    public JarReportEntry(String name, Status status, Throwable exception, Class<?> klass) {
        assert name != null;
        assert (klass != null) || (status != Status.OK);
        assert (exception == null) || (status != Status.OK);
        
        this.name = name;
        this.status = status;
        this.klass = klass;
        this.exception = exception;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public Throwable getException() {
        return exception;
    }
    
    public boolean isNode() {
        return (klass == null) ? false : Node.class.isAssignableFrom(klass);
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        switch(status) {
            case OK:
                assert klass != null;
                sb.append(klass.getCanonicalName());
                sb.append(" - OK"); //NOI18N
                break;
            case CANNOT_LOAD:
                assert klass == null;
                assert exception != null;
                sb.append(name);
                sb.append(" - CANNOT_LOAD - "); //NOI18N
                sb.append(exception.getMessage());
                break;
            case CANNOT_INSTANTIATE:
                assert klass != null;
                sb.append(klass.getCanonicalName());
                sb.append(" - CANNOT_INSTANTIATE - "); //NOI18N
                sb.append(exception.getMessage());
                break;
            case IGNORED:
                assert klass == null;
                sb.append(name);
                sb.append(" - IGNORED"); //NOI18N
                break;
            default:
                throw new IllegalStateException("Unexpected status " + status); //NOI18N
        }
        
        return sb.toString();
    }
}
