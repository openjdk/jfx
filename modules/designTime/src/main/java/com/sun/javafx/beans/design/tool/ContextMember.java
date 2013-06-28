/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Modifier;

/**
 * A ContextMember represents a ContextField or a ContextMethod that is present in
 * a DesignContext.
 * 
 * @author Tor Norbye
 */
public abstract class ContextMember {
    protected DesignContext designContext;
    protected String name;
    protected int modifiers = Modifier.PUBLIC;
    protected String commentText;
    protected String annotationText;

    public ContextMember(DesignContext designContext, final String name) {
        this.designContext = designContext;
        this.name = name;
    }

    /**
     * Returns the DesignContext associated with this DesignContext
     *
     * @return The DesignContext associated with this DesignContext
     */
    public DesignContext getDesignContext() {
        return designContext;
    }

    /**
     *
     * @param designContext DesignContext
     */
    public void setDesignContext(final DesignContext designContext) {
        this.designContext = designContext;
    }

    /**
     * Returns the name of the method represented by this <code>Member</code>
     * object, as a <code>String</code>.
     * @see java.lang.reflect.Member#getName
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this member
     * @param name The name of this member 
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the Java language modifiers for the method represented
     * by this <code>ContextMember</code> object, as an integer. The <code>Modifier</code> class should
     * be used to decode the modifiers.
     *
     * @see java.lang.reflect.Member#getModifiers
     * @see java.lang.reflect.Modifier
     */
    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(final int modifiers) {
        this.modifiers = modifiers;
    }    
    
    /**
     * Set the comment text associated with this member.
     *
     * @param commentText The comment to be associated with this member
     */
    public void setCommentText(final String commentText) {
        this.commentText = commentText;
    }

    /**
     * Get the comment text associated with this member.
     * 
     * @return The comment associated with this member
     */
    public String getCommentText() {
        return commentText;
    }

    /**
     * Get the annotations associated with this member, expressed as a source string.
     * For example, a method intended to be overridden could have the annotationText
     * set to <code>"@java.lang.Override"</code>.
     * @return The annotations set on this member, or null if none are set
     */ 
    public String getAnnotationText() {
        return annotationText;
    }

    /**
     * Set the annotations associated with this member.
     * @param annotationText The annotations to be set on this member, or null to remove them
     * @see #getAnnotationText
     */
    public void setAnnotationText(String annotationText) {
        this.annotationText = annotationText;
    }
}
