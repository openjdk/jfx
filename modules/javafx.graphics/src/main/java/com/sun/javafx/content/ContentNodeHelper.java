/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.content;

import com.sun.javafx.scene.NodeHelper;
import javafx.content.ContentNode;
import javafx.content.ContentParent;
import javafx.content.ContentParentBase;
import javafx.css.Selector;
import javafx.css.Styleable;
import javafx.scene.Node;
import java.util.LinkedList;
import java.util.List;

public final class ContentNodeHelper {

    private ContentNodeHelper() {}

    public static void setContentParent(List<?> nodes, ContentParent parent) {
        for (Object node : nodes) {
            if (node instanceof Node n) {
                NodeHelper.setContentParent(n, parent);
            } else if (node instanceof ContentParentBase n) {
                ContentParentBaseHelper.setContentParent(n, parent);
            }
        }
    }

    public static List<Styleable> lookupAllContent(ContentNode node, Selector selector, List<Styleable> results) {
        if (node instanceof Styleable styleable && selector.applies(styleable)) {
            if (results == null) {
                results = new LinkedList<>();
            }

            results.add(styleable);
        }

        if (node instanceof ContentParent parent) {
            for (Object child : parent.getContentChildren()) {
                if (child instanceof ContentNode childNode) {
                    results = lookupAllContent(childNode, selector, results);
                }
            }
        }

        return results;
    }

}
