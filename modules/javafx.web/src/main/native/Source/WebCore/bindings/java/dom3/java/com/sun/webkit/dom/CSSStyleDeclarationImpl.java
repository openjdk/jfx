/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;

public class CSSStyleDeclarationImpl implements CSSStyleDeclaration {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
        public void dispose() {
            CSSStyleDeclarationImpl.dispose(peer);
        }
    }

    CSSStyleDeclarationImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static CSSStyleDeclaration create(long peer) {
        if (peer == 0L) return null;
        return new CSSStyleDeclarationImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof CSSStyleDeclarationImpl) && (peer == ((CSSStyleDeclarationImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(CSSStyleDeclaration arg) {
        return (arg == null) ? 0L : ((CSSStyleDeclarationImpl)arg).getPeer();
    }

    private static void dispose(long peer) {
        CSSStyleDeclarationNative.dispose(peer);
    }

    static CSSStyleDeclaration getImpl(long peer) {
        return (CSSStyleDeclaration)create(peer);
    }


// Attributes
    @Override
    public String getCssText() {
        return getCssTextImpl(getPeer());
    }
    static String getCssTextImpl(long peer) {
        return CSSStyleDeclarationNative.getCssText(peer);
    }

    @Override
    public void setCssText(String value) throws DOMException {
        setCssTextImpl(getPeer(), value);
    }
    static void setCssTextImpl(long peer, String value) {
        CSSStyleDeclarationNative.setCssText(peer, value);
    }

    @Override
    public int getLength() {
        return getLengthImpl(getPeer());
    }
    static int getLengthImpl(long peer) {
        return CSSStyleDeclarationNative.getLength(peer);
    }

    @Override
    public CSSRule getParentRule() {
        return CSSRuleImpl.getImpl(getParentRuleImpl(getPeer()));
    }
    static long getParentRuleImpl(long peer) {
        return CSSStyleDeclarationNative.getParentRule(peer);
    }


// Functions
    @Override
    public String getPropertyValue(String propertyName)
    {
        return getPropertyValueImpl(getPeer()
            , propertyName);
    }
    static String getPropertyValueImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.getPropertyValue(peer, propertyName);
    }


    @Override
    public CSSValue getPropertyCSSValue(String propertyName)
    {
        return CSSValueImpl.getImpl(getPropertyCSSValueImpl(getPeer()
            , propertyName));
    }
    static long getPropertyCSSValueImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.getPropertyCSSValue(peer, propertyName);
    }


    @Override
    public String removeProperty(String propertyName) throws DOMException
    {
        return removePropertyImpl(getPeer()
            , propertyName);
    }
    static String removePropertyImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.removeProperty(peer, propertyName);
    }


    @Override
    public String getPropertyPriority(String propertyName)
    {
        return getPropertyPriorityImpl(getPeer()
            , propertyName);
    }
    static String getPropertyPriorityImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.getPropertyPriority(peer, propertyName);
    }


    @Override
    public void setProperty(String propertyName
        , String value
        , String priority) throws DOMException
    {
        setPropertyImpl(getPeer()
            , propertyName
            , value
            , priority);
    }
    static void setPropertyImpl(long peer
        , String propertyName
        , String value
        , String priority) {
        CSSStyleDeclarationNative.setProperty(peer, propertyName, value, priority);
    }


    @Override
    public String item(int index)
    {
        return itemImpl(getPeer()
            , index);
    }
    static String itemImpl(long peer
        , int index) {
        return CSSStyleDeclarationNative.item(peer, index);
    }


    public String getPropertyShorthand(String propertyName)
    {
        return getPropertyShorthandImpl(getPeer()
            , propertyName);
    }
    static String getPropertyShorthandImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.getPropertyShorthand(peer, propertyName);
    }


    public boolean isPropertyImplicit(String propertyName)
    {
        return isPropertyImplicitImpl(getPeer()
            , propertyName);
    }
    static boolean isPropertyImplicitImpl(long peer
        , String propertyName) {
        return CSSStyleDeclarationNative.isPropertyImplicit(peer, propertyName);
    }


}

