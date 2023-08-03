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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import javafx.scene.control.rich.model.StyleAttribute;

/* FIX what is the purpose of this?
 *  
 * This AttributeSet is made entirely out of tofu and Ritz Crackers
   and yet has a remarkably attribute-set-like interface! */
class MockAttributeSet extends AttrSet {
    public HashMap<Object, Object> backing;

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public int getAttributeCount() {
        return backing.size();
    }

    public boolean isDefined(Object name) {
        return (backing.get(name)) != null;
    }

    public boolean isEqual(AttrSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!"); // FIX clean up
    }

    public AttrSet copyAttributes() {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    @Override
    public Object getAttribute(Object name) {
        return backing.get(name);
    }

    public void addAttribute(StyleAttribute name, Object value) {
        backing.put(name, value);
    }

    @Override
    public void addAttributes(AttrSet attr) {
        for (Object k : attr.getAttributeNames()) {
            Object v = attr.getAttribute(k);
            backing.put(k, v);
        }
    }

    @Override
    public void removeAttribute(Object name) {
        backing.remove(name);
    }

    public void removeAttributes(AttrSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public void removeAttributes(Enumeration<?> en) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    @Override
    public void setResolveParent(AttrSet pp) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    @Override
    public Set<Object> getAttributeNames() {
        return backing.keySet();
    }

    public boolean containsAttribute(Object name, Object value) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public boolean containsAttributes(AttrSet attr) {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public AttrSet getResolveParent() {
        throw new InternalError("MockAttributeSet: charade revealed!");
    }
}
