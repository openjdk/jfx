/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

public class CSSStyleSheetImpl extends StyleSheetImpl implements CSSStyleSheet {
    CSSStyleSheetImpl(long peer) {
        super(peer);
    }

    static CSSStyleSheet getImpl(long peer) {
        return (CSSStyleSheet)create(peer);
    }


// Attributes
    public CSSRule getOwnerRule() {
        return CSSRuleImpl.getImpl(getOwnerRuleImpl(getPeer()));
    }
    native static long getOwnerRuleImpl(long peer);

    public CSSRuleList getCssRules() {
        return CSSRuleListImpl.getImpl(getCssRulesImpl(getPeer()));
    }
    native static long getCssRulesImpl(long peer);

    public CSSRuleList getRules() {
        return CSSRuleListImpl.getImpl(getRulesImpl(getPeer()));
    }
    native static long getRulesImpl(long peer);


// Functions
    public int insertRule(String rule
        , int index) throws DOMException
    {
        return insertRuleImpl(getPeer()
            , rule
            , index);
    }
    native static int insertRuleImpl(long peer
        , String rule
        , int index);


    public void deleteRule(int index) throws DOMException
    {
        deleteRuleImpl(getPeer()
            , index);
    }
    native static void deleteRuleImpl(long peer
        , int index);


    public int addRule(String selector
        , String style
        , int index) throws DOMException
    {
        return addRuleImpl(getPeer()
            , selector
            , style
            , index);
    }
    native static int addRuleImpl(long peer
        , String selector
        , String style
        , int index);


    public void removeRule(int index) throws DOMException
    {
        removeRuleImpl(getPeer()
            , index);
    }
    native static void removeRuleImpl(long peer
        , int index);


}

