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

import com.sun.javafx.content.ContentChildrenList;
import com.sun.javafx.content.ContentNodeHelper;
import com.sun.javafx.content.ContentParentBaseHelper;
import com.sun.javafx.content.ContentParentChangedListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * Base class for implementations of {@link ContentParent} that are not subclasses of {@link Node}.
 *
 * @since 21
 */
public non-sealed abstract class ContentParentBase implements ContentParent {

    static {
        ContentParentBaseHelper.setAccessor(new ContentParentBaseHelper.Accessor() {
            @Override
            public void setContentParent(ContentParentBase node, ContentParent parent) {
                node.setContentParent(parent);
            }
        });
    }

    private final ContentParentChangedListener contentParentChanged = new ContentParentChangedListener(this);
    private ContentParent contentParent;
    private ContentParentProperty contentParentProperty;
    private ContentChildrenList contentChildren;
    private ObservableList<?> contentChildrenUnmodifiable;

    /**
     * Adds a content model to this node.
     * <p>
     * Derived classes should call this method to add their own content model, which may include
     * any number of objects. The content model is added to the content models that were added
     * by base classes.
     * <p>
     * See {@link ContentNode} for more information about the content graph.
     *
     * @param content an {@code ObservableList} that contains the content model
     */
    protected final void addContentModel(ObservableList<?> content) {
        if (contentChildren == null) {
            contentChildren = new ContentChildrenList(new ObservableList[] { content });
            contentChildren.addListener(contentParentChanged);
            ContentNodeHelper.setContentParent(contentChildren, this);
        } else {
            contentChildren.addList(content);
        }
    }

    @Override
    public final ObservableList<?> getContentChildren() {
        if (contentChildrenUnmodifiable == null) {
            if (contentChildren == null) {
                contentChildren = new ContentChildrenList(new ObservableList[0]);
                contentChildren.addListener(contentParentChanged);
            }

            contentChildrenUnmodifiable = FXCollections.unmodifiableObservableList(contentChildren);
        }

        return contentChildrenUnmodifiable;
    }

    public final ReadOnlyObjectProperty<ContentParent> contentParentProperty() {
        if (contentParentProperty == null) {
            contentParentProperty = new ContentParentProperty();
        }

        return contentParentProperty;
    }

    @Override
    public final ContentParent getContentParent() {
        return contentParent;
    }

    private void setContentParent(ContentParent parent) {
        if (contentParent != parent) {
            contentParent = parent;

            if (contentParentProperty != null) {
                contentParentProperty.notifyValueChanged();
            }
        }
    }

    private class ContentParentProperty extends ReadOnlyObjectPropertyBase<ContentParent> {
        @Override
        public Object getBean() {
            return ContentParentBase.this;
        }

        @Override
        public String getName() {
            return "contentParent";
        }

        @Override
        public ContentParent get() {
            return contentParent;
        }

        public void notifyValueChanged() {
            fireValueChangedEvent();
        }
    }

}
