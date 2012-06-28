/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import com.sun.javafx.collections.TrackableObservableList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import javafx.scene.Node;
import javafx.scene.Scene;

/*
 * A rule is a collection of selectors and declarations.
 */
final public class Rule {

    final List<Selector> selectors;
    public List<Selector> getSelectors() {
        return selectors;
    }

    final ObservableList<Declaration> declarations = 
        new TrackableObservableList<Declaration>() {

        @Override
        protected void onChanged(Change<Declaration> c) {
            while (c.next()) {
                if (c.wasAdded()) {
                    for(Declaration decl : c.getAddedSubList()) {
                        decl.rule = Rule.this;
                    }
                } else if (c.wasRemoved()) {
                    for(Declaration decl : c.getRemoved()) {
                        if (decl.rule == Rule.this) decl.rule = null;
                    }
                }
            }
        }
    };
    
    public List<Declaration> getDeclarations() {
        return declarations;
    }

    /** The stylesheet this rule belongs to */
    private Stylesheet stylesheet;
    public Stylesheet getStylesheet() {
        return stylesheet;
    }
    /* package */
    void setStylesheet(Stylesheet stylesheet) {
        this.stylesheet = stylesheet;
    }

    public Stylesheet.Origin getOrigin() {
        return stylesheet != null ? stylesheet.getOrigin() : null;
    }


    public Rule(List<Selector> selectors, List<Declaration> declarations) {
        this.selectors = selectors;
        this.declarations.addAll(declarations);
    }

    private Rule() {
        this(null, null);
    }

    /**
     * Checks all selectors for a match, returning an array of all relevant
     * matches.  A match is considered irrelevant if its presence or absence
     * cannot affect whether or not the rule applies;  this means that among
     * static (non-pseudoclass) matches, only the highest priority one is
     * relevant, and among pseudoclass matches, only ones with higher priority
     * than the most specific static match are relevant.
     *
     *@param node the object to test against
     *@return an array of all relevant matches, or <code>null</code> if none
     */
    List<Match> matches(Node node) {
        List<Match> matches = new ArrayList<Match>();
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            matches.add(sel.matches(node));
        }
        return matches;
    }

    List<Match> matches(Scene scene) {
        List<Match> matches = new ArrayList<Match>();
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            matches.add(sel.matches(scene));
        }
        return matches;
    }

    public boolean mightApply(String className, String id, long[] styleClasses) {
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            if (sel.mightApply(className, id, styleClasses)) return true;
        }
        return false;
    }

    public boolean applies(Node node) {
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            if (sel.applies(node)) return true;
        }
        return false;
    }

    /** Converts this object to a string. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (selectors.size()>0) {
            sb.append(selectors.get(0));
        }
        for (int n=1; n<selectors.size(); n++) {
            sb.append(',');
            sb.append(selectors.get(n));
        }
        sb.append("{\n");
        for (Declaration decl : declarations) {
            sb.append("\t");
            sb.append(decl);
            sb.append("\n");
        }
        sb .append("}");
        return sb.toString();
    }

    void writeBinary(DataOutputStream os, StringStore stringStore)
            throws IOException {
        os.writeShort(selectors.size());
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            sel.writeBinary(os, stringStore);
        }
        os.writeShort(declarations.size());
        for (int i = 0; i < declarations.size(); i++) {
            Declaration decl = declarations.get(i);
            decl.writeBinary(os, stringStore);
        }
    }

    static Rule readBinary(DataInputStream is, String[] strings)
            throws IOException
    {
        short nSelectors = is.readShort();
        List<Selector> selectors = new ArrayList<Selector>(nSelectors);
        for (int i = 0; i < nSelectors; i++) {
            Selector s = Selector.readBinary(is, strings);
            selectors.add(s);
        }

        short nDeclarations = is.readShort();
        List<Declaration> declarations = new ArrayList<Declaration>(nDeclarations);
        for (int i = 0; i < nDeclarations; i++) {
            Declaration d = Declaration.readBinary(is, strings);
            declarations.add(d);
        }

        return new Rule(selectors, declarations);
    }

}
