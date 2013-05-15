/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;

import javafx.scene.Node;
import javafx.scene.Scene;

/*
 * A selector is a collection of selectors and declarations.
 */
final public class Rule {

    private final ObservableList<Selector> selectors =
            new TrackableObservableList<Selector>() {

                @Override
                protected void onChanged(Change<Selector> c) {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            List<Selector> added = c.getAddedSubList();
                            for(int i = 0, max = added.size(); i < max; i++) {
                                Selector sel = added.get(i);
                                sel.setRule(Rule.this);
                            }
                        }

                        if (c.wasRemoved()) {
                            List<Selector> removed = c.getAddedSubList();
                            for(int i = 0, max = removed.size(); i < max; i++) {
                                Selector sel = removed.get(i);
                                if (sel.getRule() == Rule.this) sel.setRule(null);
                            }
                        }
                    }
                }
            };

    public List<Selector> getSelectors() {
        return selectors;
    }

    private final ObservableList<Declaration> declarations =
        new TrackableObservableList<Declaration>() {

        @Override
        protected void onChanged(Change<Declaration> c) {
            while (c.next()) {
                if (c.wasAdded()) {
                    List<Declaration> added = c.getAddedSubList();
                    for(int i = 0, max = added.size(); i < max; i++) {
                        Declaration decl = added.get(i);
                        decl.rule = Rule.this;
                        
                        if (stylesheet != null && stylesheet.getUrl() != null) {

                            final URL stylesheetUrl = stylesheet.getUrl();
                            decl.fixUrl(stylesheetUrl);

                        }
                        
                    }
                }
                
                if (c.wasRemoved()) {
                    List<Declaration> removed = c.getRemoved();
                    for(int i = 0, max = removed.size(); i < max; i++) {
                        Declaration decl = removed.get(i);
                        if (decl.rule == Rule.this) decl.rule = null;
                    }
                }
            }
        }
    };
    
    public List<Declaration> getDeclarations() {
        if (serializedDecls != null && Rule.strings != null) {

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(serializedDecls);
                DataInputStream dis = new DataInputStream(bis);
                short nDeclarations = dis.readShort();
                List<Declaration> decls = new ArrayList<Declaration>(nDeclarations);
                for (int i = 0; i < nDeclarations; i++) {
                    Declaration d = Declaration.readBinary(dis, Rule.strings);
                    decls.add(d);
                }

                this.declarations.addAll(decls);

            } catch (IOException ioe) {
               assert false; ioe.getMessage();
            } finally {
                serializedDecls = null;
            }

        }
        return declarations;
    }

    /** The stylesheet this selector belongs to */
    private Stylesheet stylesheet;
    public Stylesheet getStylesheet() {
        return stylesheet;
    }

    /* package */
    void setStylesheet(Stylesheet stylesheet) {
        this.stylesheet = stylesheet;
        
        if (stylesheet != null && stylesheet.getUrl() != null) {
            
            final URL stylesheetUrl = stylesheet.getUrl();
            for (int d=0, dMax=declarations.size(); d<dMax; d++) {
                declarations.get(d).fixUrl(stylesheetUrl);
            }
            
        }
    }

    public StyleOrigin getOrigin() {
        return stylesheet != null ? stylesheet.getOrigin() : null;
    }


    public Rule(List<Selector> selectors, List<Declaration> declarations) {
        this.selectors.setAll(selectors);
        this.declarations.setAll(declarations);
        serializedDecls = null;
    }

    private static String[] strings = null;  // TBD: blech!
    private byte[] serializedDecls;

    private Rule(List<Selector> selectors, byte[] buf, String[] stringStoreStrings) {
        this.selectors.setAll(selectors);
        this.serializedDecls = buf;
        if (Rule.strings == null) Rule.strings = stringStoreStrings;
    }

    // Return mask of selectors that match
    long applies(Node node, Set<PseudoClass>[] triggerStates) {
        long mask = 0;
        for (int i = 0; i < selectors.size(); i++) {
            Selector sel = selectors.get(i);
            if (sel.applies(node, triggerStates, 0)) {                
                mask |= 1l << i;
            }
        }
        return mask;
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

        ByteArrayOutputStream bos = new ByteArrayOutputStream(5192);
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeShort(declarations.size());

        for (int i = 0; i < declarations.size(); i++) {
            Declaration decl = declarations.get(i);
            decl.writeBinary(dos, stringStore);
        }

        os.writeInt(bos.size());
        os.write(bos.toByteArray());
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


        // de-serialize decls into byte array
        int nBytes = is.readInt();
        byte[] buf = new byte[nBytes];

        is.readFully(buf);

        return new Rule(selectors, buf, strings);
    }
}
