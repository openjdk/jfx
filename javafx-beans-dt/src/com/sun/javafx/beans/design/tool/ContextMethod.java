/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.design.tool;

/**
 * <p>The ContextMethod class represents a single source code method on a {@link DesignContext}.
 * Use the ContextMethod class to create, update, and remove methods via the DesignContext methods:
 * {@link DesignContext#createContextMethod(ContextMethod)},
 * {@link DesignContext#updateContextMethod(ContextMethod)}, and
 * {@link DesignContext#removeContextMethod(ContextMethod)}.  Use
 * {@link DesignContext#getContextMethods()} to retrieve the list of methods on a DesignContext.</p>
 *
 * <p>Note that a ContextMethod object is like a simple struct in that manipulations to an instance
 * of this class will not affect the underlying DesignContext (and source code) until the
 * ContextMethod has been passed as an argument to one of the above DesignContext methods.  At that
 * point, the underlying code is manipulated.</p>
 *
 * @author Joe Nuxoll
 * @author Tor Norbye
 */
public class ContextMethod extends ContextMember {
    private Class[] parameterTypes;
    private String[] parameterNames;
    private String[] parameterAnnotations;
    private Class returnType;
    private Class[] exceptionTypes;
    private String methodBodyText;

    /**
     * Constructs a default ContextMethod with the given name, modifier and associated DesignContext.
     */
    public ContextMethod(DesignContext designContext, final String name) {
        super(designContext, name);
    }

    /**
     * Constructs a ContextMethod with the specified DesignContext, name, modifiers,
     * returnType, parameterTypes, parameterNames, methodBody, and commentText.
     *
     * @param designContext DesignContext for this ContextMethod
     * @param name The method name for this ContextMethod
     * @param modifiers The method {@link Modifier} bits
     * @param parameterTypes The parameter types for this ContextMethod
     * @param parameterNames The parameter names for this ContextMethod
     * @param returnType The return type for this ContextMethod
     * @param methodBodyText The Java source code for the body of this ContextMethod
     * @param commentText The comment text for this ContextMethod
     */
    public ContextMethod(final DesignContext designContext, final String name, final int modifiers,
        final Class returnType, final Class[] parameterTypes, final String[] parameterNames,
        final String[] parameterAnnotations, final String methodBodyText, final String commentText, 
        final String annotationText) {
        this(designContext, name);
        this.commentText = commentText;
        this.annotationText = annotationText;
        this.modifiers = modifiers;

        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterNames = parameterNames;
        this.parameterAnnotations = parameterAnnotations;
        this.methodBodyText = methodBodyText;
    }

    /**
     *
     * @param returnType Class
     */
    public void setReturnType(final Class returnType) {
        this.returnType = returnType;
    }

    /**
     *
     * @return Class
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     *
     * @param parameterTypes Class[]
     */
    public void setParameterTypes(final Class[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     *
     * @return Class[]
     */
    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     *
     * @param parameterNames String[]
     */
    public void setParameterNames(final String[] parameterNames) {
        this.parameterNames = parameterNames;
    }
    
    /**
     * Get the annotations associated with the parameters expressed as
     * source code.
     * 
     * @see #setParameterAnnotations
     */
    public String[] getParameterAnnotations() {
        return parameterAnnotations;
    }
    
    /** 
     * Set the annotations associated with the parameters. The annotations are
     * expressed as source code, just as is the case for {@link #getAnnotationText},
     * and should correspond one-to-one to the parameters in {@link #getParameterNames}
     * and {@link #getParameterTypes}. Use null for array elements that should no be
     * annotated.
     * @see #getParameterAnnotations
     */
    public void setParameterAnnotations(String[] parameterAnnotations) {
        this.parameterAnnotations = parameterAnnotations;
    }

    /**
     *
     * @return String[]
     */
    public String[] getParameterNames() {
        return parameterNames;
    }

    /**
     *
     * @param exceptionTypes Class[]
     */
    public void setExceptionTypes(final Class[] exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    /**
     *
     * @return Class[]
     */
    public Class[] getExceptionTypes() {
        return exceptionTypes;
    }

    /**
     *
     * @param methodBodyText String
     */
    public void setMethodBodyText(final String methodBodyText) {
        this.methodBodyText = methodBodyText;
    }

    /**
     *
     * @return String
     */
    public String getMethodBodyText() {
        return methodBodyText;
    }

    public String toString() {
        // TODO - include more properties?
        return "ContextMethod(name=" + name + ")";
    }
}
