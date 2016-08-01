/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;

public class RuleShim {

    public static List<Declaration> getUnobservedDeclarationList(Rule r) {
        return r.getUnobservedDeclarationList();
    }

    public static List<Selector>  getUnobservedSelectorList(Rule r) {
        return r.getUnobservedSelectorList();
    }

    public static long applies(
            Rule r,
            Node node, Set<PseudoClass>[] triggerStates) {
        return r.applies(node, triggerStates);
    }

    public static Rule readBinary(
            int bssVersion, DataInputStream is, String[] strings)
            throws IOException {
        return Rule.readBinary(bssVersion, is, strings);
    }

    public static void writeBinary(
            Rule r,
            DataOutputStream os, StyleConverter.StringStore stringStore)
            throws IOException {
        r.writeBinary(os, stringStore);
    }

    public static Rule getRule(List<Selector> selectors, List<Declaration> declarations) {
        return new Rule(selectors, declarations);
    }

}
