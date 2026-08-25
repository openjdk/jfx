/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package jfx.incubator.scene.control.richtext.skin;

import java.util.function.Consumer;

import com.sun.jfx.incubator.scene.control.richtext.RowMapHelper;
import jfx.incubator.scene.control.richtext.model.ContentChange;

/**
 * Defines the mapping between visible row indices and model paragraph indices.
 * <p>A "row" is the index of a paragraph in the visible sequence: hidden paragraphs do not occupy a row.
 * When there are no hidden paragraphs (by default), row indices and model indices are identical.</p>
 * <p>The RichTextArea uses the RowMap to determine the mapping between model indices that traverse the document,
 * and view rows, which are the paragraphs rendered in the viewport. While the model is based on the document,
 * the view is a filtered representation of the model. Therefore, subclasses can implement custom mapping logic
 * and achieve features like:</p>
 * <ul>
 *     <li>Search/filter mode, showing only paragraphs with results</li>
 *     <li>Log/console, filtering out paragraphs based on log levels</li>
 *     <li>Outline/summary mode, showing headings and hiding details</li>
 *     <li>Code folding in the {@code CodeArea}</li>
 *     <li>Collapsible sections</li>
 *     <li>Any other custom mapping logic to show and hide paragraphs</li>
 * </ul>
 * <p>In all cases, since the model doesn't change, applying or removing the filter is a fast operation,
 * as the model doesn't need to be recreated. In other words, {@link RowMap} plays a role for {@code RichTextArea}
 * and {@code CodeArea} similar to {@code FilteredList} for {@code ListView}.</p>
 * <p>As the model can change at any time, notifications are sent via {@link #onContentChange(ContentChange)}
 * so the view can be updated properly.</p>
 * <p>Subclasses should override the impl methods to provide its custom mapping logic, override {@link #refreshMap()}
 * to recompute the internal state, and call {@link #notifyChange(boolean)} when the external state changes.
 * {@link #dispose()} can be overridden if added listeners need to be released.</p>
 */
public class RowMap {

    private Consumer<Boolean> onChange;
    private boolean dirty = true;

    static {
        RowMapHelper.setAccessor(new RowMapHelper.Accessor() {
            @Override
            public void dispose(RowMap rowMap) {
                rowMap.dispose();
            }

            @Override
            public void onContentChange(RowMap rowMap, ContentChange change) {
                rowMap.onContentChange(change);
            }

            @Override
            public void setOnChange(RowMap rowMap, Consumer<Boolean> callback) {
                rowMap.setOnChange(callback);
            }
        });
    }

    /**
     * Creates a new RowMap instance.
     */
    public RowMap() {

    }

    /**
     * Returns the number of visible rows in the view, given the number of paragraphs in the model, once the map is
     * validated.
     * @param modelParagraphCount the number of paragraphs in the model
     * @return the number of visible rows in the view
     */
    public final int getRowCount(int modelParagraphCount) {
        validate();
        return getRowCountImpl(modelParagraphCount);
    }

    /**
     * Returns the number of visible rows in the view, given the number of paragraphs in the model.
     * This method is called after the map is validated, and subclasses can override it to provide custom logic for
     * computing the number of visible rows based on the model paragraph count.
     * @param modelParagraphCount the number of paragraphs in the model
     * @return the number of visible rows in the view
     */
    protected int getRowCountImpl(int modelParagraphCount) {
        return modelParagraphCount;
    }

    /**
     * Returns the model index of the paragraph at the given row in the view, once the map is
     * validated.
     * @param row the row index in the view
     * @return the model index of the paragraph at the given row
     */
    public final int getModelIndex(int row) {
        validate();
        return getModelIndexImpl(row);
    }

    /**
     * Returns the model index of the paragraph at the given row in the view.
     * This method is called after the map is validated, and subclasses can override it to provide
     * custom logic for computing the model index based on the view row index.
     * @param row the row index in the view
     * @return the model index of the paragraph at the given row
     */
    protected int getModelIndexImpl(int row) {
        return row;
    }

    /**
     * Returns the number of visible paragraphs preceding the given model index, once the mapping is validated.
     * For a visible paragraph, this is its view row index. For a hidden paragraph, this is the
     * view row index of the next visible paragraph.
     * @param modelIndex the model index of the paragraph
     * @return the view row index of the paragraph at the given model index
     */
    public final int getViewRow(int modelIndex) {
        validate();
        return getViewRowImpl(modelIndex);
    }

    /**
     * Returns the number of visible paragraphs preceding the given model index.
     * This method is called after the map is validated, and subclasses can override it to provide
     * custom logic for computing the view row index based on the model index.
     * @param modelIndex the model index of the paragraph
     * @return the view row index of the paragraph at the given model index
     */
    protected int getViewRowImpl(int modelIndex) {
        return modelIndex;
    }

    /**
     * Returns whether the paragraph at the given model index is hidden, once the mapping is validated.
     * @param modelIndex the model index of the paragraph
     * @return true if the paragraph is hidden, false otherwise
     */
    public final boolean isHidden(int modelIndex) {
        validate();
        return isHiddenImpl(modelIndex);
    }

    /**
     * Returns whether the paragraph at the given model index is hidden.
     * This method is called after the map is validated, and subclasses can override it to provide
     * custom logic for determining whether a paragraph is hidden based on its model index.
     * @param modelIndex the model index of the paragraph
     * @return true if the paragraph is hidden, false otherwise
     */
    protected boolean isHiddenImpl(int modelIndex) {
        return false;
    }

    /**
     * Called when the content of the model changes.
     * @param ch the content change
     */
    protected void onContentChange(ContentChange ch) {
        invalidate();
    }

    /**
     * Notifies the owning flow that the mapping has changed, and the view should be updated
     * by requesting a layout pass. The {@code clearCache} parameter indicates whether the cache should be cleared.
     * <p>The map is invalidated and lazily refreshed via {@link #refreshMap()}.</p>
     * @param clearCache whether to clear the cache of the owning flow
     */
    protected final void notifyChange(boolean clearCache) {
        invalidate();
        if (onChange != null) {
            onChange.accept(clearCache);
        }
    }

    /**
     * Refreshes the mapping.
     * Subclasses can override this method to recompute the mapping when it is marked as dirty.
     */
    protected void refreshMap() {
        // no-op
    }

    /**
     * Disposes of any resources held by this RowMap. Subclasses can override this method to perform
     * cleanup when the RowMap is no longer needed.
     */
    protected void dispose() {
        // no-op
    }

    /**
     * Marks the mapping as dirty, indicating that it needs to be refreshed. This method is called when the mapping changes,
     * but can be called for other purposes as well. The mapping will be refreshed the next time it is validated.
     */
    protected final void invalidate() {
        dirty = true;
    }

    /**
     * Validates the mapping, refreshing it if it is marked as dirty. This method is called before any mapping operations
     * to ensure that the mapping is up-to-date, but can be called for other purposes as well.
     */
    protected final void validate() {
        if (dirty) {
            refreshMap();
            dirty = false;
        }
    }

    /**
     * Installed by the owning flow to receive notifications when the mapping changes.
     * @param onChange the callback to be invoked when the mapping changes
     */
    final void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }
}
