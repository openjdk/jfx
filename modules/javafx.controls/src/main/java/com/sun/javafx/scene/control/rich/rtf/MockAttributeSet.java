/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package com.sun.javafx.scene.control.rich.rtf;

import java.util.Dictionary;
import java.util.Enumeration;
import javafx.scene.control.rich.model.StyleAttribute;

/* FIX what is the purpose of this?
 *  
 * This AttributeSet is made entirely out of tofu and Ritz Crackers
   and yet has a remarkably attribute-set-like interface! */
class MockAttributeSet extends MutableAttributeSet {
    public Dictionary<Object, Object> backing;

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public int getAttributeCount() {
        return backing.size();
    }

    public boolean isDefined(Object name) {
        return (backing.get(name)) != null;
    }

    public boolean isEqual(MutableAttributeSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!"); // FIX clean up
    }

    public MutableAttributeSet copyAttributes() {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public Object getAttribute(Object name) {
        return backing.get(name);
    }

    public void addAttribute(StyleAttribute name, Object value) {
        backing.put(name, value);
    }

    public void addAttributes(MutableAttributeSet attr) {
        Enumeration<Object> as = attr.getAttributeNames();
        while (as.hasMoreElements()) {
            Object el = as.nextElement();
            backing.put(el, attr.getAttribute(el));
        }
    }

    public void removeAttribute(Object name) {
        backing.remove(name);
    }

    public void removeAttributes(MutableAttributeSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public void removeAttributes(Enumeration<?> en) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public void setResolveParent(MutableAttributeSet pp) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public Enumeration<Object> getAttributeNames() {
        return backing.keys();
    }

    public boolean containsAttribute(Object name, Object value) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public boolean containsAttributes(MutableAttributeSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public MutableAttributeSet getResolveParent() {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }
}
