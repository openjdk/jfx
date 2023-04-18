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

package javafx.content;

import com.sun.javafx.collections.UnmodifiableListSet;
import com.sun.javafx.content.ContentNodeHelper;
import javafx.collections.ObservableList;
import javafx.css.Selector;
import javafx.css.Styleable;
import javafx.scene.Parent;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link ContentNode} that can have children.
 *
 * @since 21
 */
public sealed interface ContentParent extends ContentNode permits Parent, ContentParentBase {

    /**
     * Gets the children of this {@code ContentParent}.
     *
     * @return an unmodifiable list of children, which can be objects of any kind
     */
    ObservableList<?> getContentChildren();

    @Override
    default Styleable lookupContent(String selector) {
        Styleable styleable = ContentNode.super.lookupContent(selector);
        if (styleable != null) {
            return styleable;
        }

        for (Object child : getContentChildren()) {
            styleable = child instanceof ContentNode node ? node.lookupContent(selector) : null;
            if (styleable != null) {
                return styleable;
            }
        }

        return null;
    }

    @Override
    default Set<Styleable> lookupAllContent(String selector) {
        Selector cssSelector = Selector.createSelector(selector);
        if (cssSelector == null) {
            return Set.of();
        }

        List<Styleable> results = ContentNodeHelper.lookupAllContent(this, cssSelector, null);
        return results == null ? Set.of() : new UnmodifiableListSet<>(results);
    }

}
