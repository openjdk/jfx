/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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

package com.javafx.experiments.importers.maya;

import java.util.ArrayList;
import java.util.List;
import com.javafx.experiments.importers.maya.values.MArray;
import com.javafx.experiments.importers.maya.values.MData;
import com.javafx.experiments.importers.maya.values.MFloat2Array;
import com.javafx.experiments.importers.maya.values.MFloat3Array;
import com.javafx.experiments.importers.maya.values.MFloatArray;
import com.javafx.experiments.importers.maya.values.MInt3Array;
import com.javafx.experiments.importers.maya.values.MIntArray;

// ry => r[1];

public class MPath implements Comparable {

    static abstract class Component implements Comparable {
        public MData apply(MData data) {
            return null;
        }

        public MData apply(MNode node) {
            return null;
        }
    }

    // Index < Slice < Select in ordering

    static class Index extends Component {
        int index;

        public String toString() {
            return "[" + index + "]";
        }

        public Index(int i) {
            index = i;
        }

        public MData apply(MData data) {
            data.setSize(index + 1);
            return data.getData(index);
        }

        public int getIndex() {
            return index;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Index)) {
                return false;
            }
            return index == ((Index) other).index;
        }

        public int hashCode() {
            return 11 + 17 * index;
        }

        public int compareTo(Object arg) {
            if (arg instanceof Index) {
                return index - ((Index) arg).index;
            }

            if (arg instanceof Component) {
                return -1;
            }

            throw new ClassCastException(arg.getClass().getName());
        }
    }

    static class Slice extends Component {
        public String toString() {
            return "[" + start + ":" + end + "]";
        }

        int start, end;

        public Slice(int i, int j) {
            start = i;
            end = j;
        }

        public MData apply(MData data) {
            data.setSize(end + 1);
            return data.getData(start, end);
        }

        public boolean equals(Object arg) {
            if (!(arg instanceof Slice)) {
                return false;
            }
            Slice other = (Slice) arg;
            return (start == other.start &&
                    end == other.end);
        }

        public int hashCode() {
            return 11 + 17 * start + 23 * end;
        }

        public int compareTo(Object arg) {
            if (arg instanceof Slice) {
                Slice other = (Slice) arg;
                int diff = start - other.start;
                if (diff != 0) {
                    return diff;
                }
                return end - other.end;
            }

            if (arg instanceof Index) {
                return 1;
            }

            if (arg instanceof Select) {
                return -1;
            }

            throw new ClassCastException(arg.getClass().getName());
        }
    }

    static class Select extends Component {
        public String toString() {
            return "." + name;
        }

        String name;

        public Select(String n) {
            name = n;
        }

        public MData apply(MData data) {
            return data.getData(name);
        }

        public MData apply(MNode node) {
            return node.getAttrDirect(name);
        }

        public boolean equals(Object arg) {
            if (!(arg instanceof Select)) {
                return false;
            }
            Select other = (Select) arg;
            return (name.equals(other.name));
        }

        public int hashCode() {
            return name.hashCode();
        }

        public int compareTo(Object arg) {
            if (arg instanceof Select) {
                return name.compareTo(((Select) arg).name);
            }

            if (arg instanceof Component) {
                return 1;
            }

            throw new ClassCastException(arg.getClass().getName());
        }
    }

    // A Path always exists within the context of a given node
    MNode node;

    List<Component> components = new ArrayList();

    private void add(Component comp) {
        components.add(comp);
    }

    // Used to canonicalize for example in Mesh:
    //  .iog.og[n] -> .iog[0].og[n]
    private boolean selectsArray() {
        if (components.size() == 0) {
            return false;
        }
        // If we're already doing an array indexing as the last
        // operation in the path, state that we aren't selecting an
        // array
        if (components.get(components.size() - 1) instanceof Index) {
            return false;
        }
        MData data = apply();
        if (data == null) {
            return false;
        }
        // Should we be using MDataType instead for these type queries?
        return ((data instanceof MArray) ||
                (data instanceof MFloatArray) ||
                (data instanceof MFloat2Array) ||
                (data instanceof MFloat3Array) ||
                (data instanceof MIntArray) ||
                (data instanceof MInt3Array));
    }

    private String canonicalize(String name) {
        // FIXME: do we need to do this for deeper data types too?
        return node.getCanonicalName(name);
    }

    public MPath(MEnv env, String path) {
        String nodeName;
        int i = path.indexOf(".");
        if (i > 0) {
            nodeName = path.substring(0, i);
            path = path.substring(i);
        } else {
            nodeName = path;
        }
        node = env.findNode(nodeName);
        if (i > 0) {
            addComponents(path);
        }
    }

    public MPath(MNode node, String path) {
        this.node = node;
        addComponents(path);
    }

    // For copying
    private MPath(MNode node) {
        this.node = node;
    }

    public boolean equals(Object arg) {
        if (!(arg instanceof MPath)) {
            return false;
        }
        MPath other = (MPath) arg;
        return (node == other.node &&
                components.equals(other.components));
    }

    public int hashCode() {
        int hashCode = 0;
        for (Component comp : components) {
            hashCode += 17 * comp.hashCode();
        }
        return hashCode;
        /*
        if (node == null) {
            return 0;
        }
        return node.hashCode();
        */
    }

    public int compareTo(Object arg) {
        MPath other = (MPath) arg;
        if (node != other.node) {
            return node.hashCode() - other.node.hashCode();
        }
        int sz = Math.min(components.size(), other.components.size());
        for (int i = 0; i < sz; i++) {
            int diff = components.get(i).compareTo(other.components.get(i));
            if (diff != 0) {
                return diff;
            }
        }
        if (components.size() != other.components.size()) {
            return components.size() - other.components.size();
        }
        return 0;
    }

    public boolean isValid() {
        return getTargetNode() != null;
    }

    /** Indicates whether this path is a prefix of the given one. */
    public boolean isPrefixOf(MPath other) {
        if (node != other.node) {
            return false;
        }
        if (components.size() > other.components.size()) {
            return false;
        }
        for (int i = 0; i < components.size(); i++) {
            if (!(components.get(i).equals(other.components.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /** Returns the parent path of this one -- i.e., the path with the last component removed. */
    public MPath getParentPath() {
        MPath res = new MPath(node);
        for (int i = 0; i < components.size() - 1; i++) {
            res.add(components.get(i));
        }
        return res;
    }

    private void addComponents(String path) {
        if (node == null) {
            return;
        }
        int mark = 0;
        int i = 0;
        int len = path.length();
        for (; i < len; i++) {
            char ch = path.charAt(i);
            if (ch == '.') {
                if (i - mark > 0) {
                    String str = path.substring(mark, i);
                    String str1 = canonicalize(str);
                    if (str.equals(str1)) {
                        // Before performing component selection,
                        // canonicalize connections to arrays to point
                        // to the zeroth array index
                        if (selectsArray()) {
                            add(new Index(0));
                        }
                        add(new Select(str1));
                    } else {
                        addComponents(str1);
                    }
                }
                mark = i + 1;
            } else if (ch == '[') {
                if (i - mark > 0) {
                    String str = path.substring(mark, i);
                    String str1 = canonicalize(str);
                    if (str.equals(str1)) {
                        // Before performing component selection,
                        // canonicalize connections to arrays to point
                        // to the zeroth array index
                        if (selectsArray()) {
                            add(new Index(0));
                        }
                        add(new Select(str1));
                    } else {
                        addComponents(str1);
                    }
                }
                int j = i + 1;
                while (true) {
                    ch = path.charAt(j);
                    if (ch == ']') {
                        break;
                    }
                    j++;
                }
                String indexStr = path.substring(i + 1, j);
                int colon = indexStr.indexOf(':');
                if (colon > 0) {
                    int start = Integer.parseInt(indexStr.substring(0, colon));
                    int end = Integer.parseInt(indexStr.substring(colon + 1));
                    add(new Slice(start, end));
                } else {
                    int index = Integer.parseInt(indexStr);
                    add(new Index(index));
                }
                i = j + 1;
                if (i < len && path.charAt(i) == '.') {
                    i++;
                }
                mark = i;
            }
        }
        if (mark < len && i - mark > 0) {
            String str = path.substring(mark, i);
            String str1 = canonicalize(str);
            if (str.equals(str1)) {
                // Before performing component selection,
                // canonicalize connections to arrays to point
                // to the zeroth array index
                if (selectsArray()) {
                    add(new Index(0));
                }
                add(new Select(str1));
            } else {
                addComponents("." + str1);
            }
        }
    }

    public MData apply() {
        if (components.size() == 0) {
            return null;
        }
        MData data = components.get(0).apply(node);
        return apply(1, data);
    }

    private MData apply(int i, MData data) {
        while (i < components.size()) {
            if (data == null) {
                return null;
            }
            data = components.get(i++).apply(data);
        }
        return data;
    }

    public String toString() {
        if (node == null) {
            return "[invalid path -- no node]";
        } else {
            return node.getFullName() + components.toString();
        }
    }

    public MNode getTargetNode() {
        return node;
    }

    int attributeOffset = -1;
    MAttribute attr;

    int _getAttributeOffset() {
        if (attributeOffset == -1) {
            String selector = "";
            int i = 0;
            while (i < components.size()) {
                selector += components.get(i).toString();
                attr = getTargetNode().getNodeType().getAttribute(selector.substring(1));
                if (attr != null) {
                    attributeOffset = i;
                    break;
                }
                i++;
            }
        }
        return attributeOffset;
    }

    public MAttribute getTargetAttribute(MEnv env) {
        _getAttributeOffset();
        return attr;
    }

    public String getComponentSelector() {
        int i = 0;
        String result = "";
        while (i < components.size()) {
            result += components.get(i).toString();
            i++;
        }
        return result.substring(1);
    }

    public String getPathComponent(int i) {
        return components.get(i).toString();
    }

    public String getLastPathComponent() {
        return getPathComponent(components.size() - 1);
    }

    public String getLastSelectionPathComponent() {
        for (int i = components.size() - 1; i >= 0; --i) {
            Component comp = components.get(i);
            if (comp instanceof Select) {
                String res = "";
                for (int j = i; j < components.size(); j++) {
                    res += getPathComponent(j);
                }
                return res;
            }
        }
        return null;
    }

    public int getLastPathIndex() {
        Component component = components.get(components.size() - 1);
        if (component instanceof Index) {
            return ((Index) component).getIndex();
        }
        return -1;
    }

    public String getLastNamedPathComponent() {
        for (int i = components.size() - 1; i >= 0; --i) {
            Component comp = components.get(i);
            if (comp instanceof Select) {
                return comp.toString();
            }
        }
        return null;
    }

    public int size() {
        return components.size();
    }
}
