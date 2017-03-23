/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.webkit.BackForwardList;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCChangeEvent;
import com.sun.webkit.event.WCChangeListener;
import java.net.URL;
import java.util.Date;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The {@code WebHistory} class represents a session history associated with
 * a {@link WebEngine} instance.
 *
 * A single instance of {@code WebHistory} for a particular web engine can be
 * obtained through the {@link WebEngine#getHistory()} method.
 *
 * The history is basically a list of entries. Each entry represents a visited page
 * and it provides access to relevant page info, such as URL, title, and the date
 * the page was last visited. Entries in the list are arranged in the order
 * in which the corresponding pages were visited from oldest to newest. The list can
 * be obtained by using the {@link #getEntries()} method.
 *
 * The history and the corresponding list of entries change as {@code WebEngine} navigates
 * across the web. The list may expand or shrink depending on browser actions. These
 * changes can be listened to by the {@link javafx.collections.ObservableList}
 * API that the list exposes.
 *
 * The index of the history entry associated with the currently visited page
 * is represented by the {@link #currentIndexProperty}. The current index can be
 * used to navigate to any entry in the history by using the {@link #go(int)} method.
 *
 * The {@link #maxSizeProperty()} sets the maximum history size, which is the size of the
 * history list.
 *
 * @since JavaFX 2.2
 */
public final class WebHistory {
    /**
     * The {@code Entry} class represents a single entry in the session history.
     * An entry instance is associated with the visited page.
     *
     * @since JavaFX 2.2
     */
    public final class Entry {
        private final URL url;
        private final ReadOnlyObjectWrapper<String> title = new ReadOnlyObjectWrapper(this, "title");
        private final ReadOnlyObjectWrapper<Date> lastVisitedDate = new ReadOnlyObjectWrapper(this, "lastVisitedDate");
        private final BackForwardList.Entry peer;

        private Entry(final BackForwardList.Entry entry) {
            this.url = entry.getURL();
            this.title.set(entry.getTitle());
            this.lastVisitedDate.set(entry.getLastVisitedDate());
            this.peer = entry;

            entry.addChangeListener(e -> {
                String _title = entry.getTitle();
                // null title is acceptable
                if (_title == null || !_title.equals(getTitle())) {
                    title.set(_title);
                }

                Date _date = entry.getLastVisitedDate();
                // null date is not acceptable
                if (_date != null && !_date.equals(getLastVisitedDate())) {
                    lastVisitedDate.set(_date);
                }
            });
        }

        /**
         * Returns the URL of the page.
         *
         * @return the url of the page
         */
        public String getUrl() {
            assert url != null;
            return url.toString();
        }

        /**
         * Defines the title of the page.
         * @return the title property
         */
        public ReadOnlyObjectProperty<String> titleProperty() {
            return title.getReadOnlyProperty();
        }

        public String getTitle() {
            return title.get();
        }

        /**
         * Defines the {@link java.util.Date} the page was last visited.
         * @return the lastVisitedDate property
         */
        public ReadOnlyObjectProperty<Date> lastVisitedDateProperty() {
            return lastVisitedDate.getReadOnlyProperty();
        }

        public Date getLastVisitedDate() {
            return lastVisitedDate.get();
        }

        boolean isPeer(BackForwardList.Entry entry) {
            return peer == entry;
        }

        @Override
        public String toString() {
            return "[url: " + getUrl()
                 + ", title: " + getTitle()
                 + ", date: " + getLastVisitedDate()
                 + "]";
        }
    }

    private final BackForwardList bfl; // backend history impl

    private final ObservableList<Entry> list;
    private final ObservableList<Entry> ulist; // unmodifiable wrapper

    WebHistory(WebPage page) {
        this.list = FXCollections.<Entry>observableArrayList();
        this.ulist = FXCollections.unmodifiableObservableList(list);
        this.bfl = page.createBackForwardList();

        setMaxSize(getMaxSize()); // init default

        this.bfl.addChangeListener(e -> {
            // 1. Size has increased
            //    - one new entry is appended.
            //    - currentIndex is set to the new entry.
            if (bfl.size() > list.size()) {
                assert (bfl.size() == list.size() + 1);
                list.add(new Entry(bfl.getCurrentEntry()));

                WebHistory.this.setCurrentIndex(list.size() - 1);
                return;
            }

            // 2. Size hasn't changed
            if (bfl.size() == list.size()) {
                if (list.size() == 0) {
                    return; // no changes
                }
                assert (list.size() > 0);
                BackForwardList.Entry last = bfl.get(list.size() - 1);
                BackForwardList.Entry first = bfl.get(0);

                // - currentIndex may change
                if (list.get(list.size() - 1).isPeer(last)) {
                    WebHistory.this.setCurrentIndex(bfl.getCurrentIndex());
                    return;

                // - first entry is removed.
                // - one new entry is appended.
                // - currentIndex is set to the new entry.
                } else if (!list.get(0).isPeer(first)) {
                    list.remove(0);
                    list.add(new Entry(last));
                    WebHistory.this.setCurrentIndex(bfl.getCurrentIndex());
                    return;
                }
            }

            // 3. Size has decreased or hasn't changed (due to maxSize or navigation)
            //    - one or more entries are popped.
            //    - one new entry may be appended.
            //    - currentIndex may be set to the new entry.
            assert (bfl.size() <= list.size());
            list.remove(bfl.size(), list.size()); // no-op if equals
            int lastIndex = list.size() - 1;
            if (lastIndex >= 0 && !list.get(lastIndex).isPeer(bfl.get(lastIndex))) {
                list.remove(lastIndex);
                list.add(new Entry(bfl.get(lastIndex)));
            }
            WebHistory.this.setCurrentIndex(bfl.getCurrentIndex());
        });
    }

    private final ReadOnlyIntegerWrapper currentIndex =
            new ReadOnlyIntegerWrapper(this, "currentIndex");

    /**
     * Defines the index of the current {@code Entry} in the history.
     * The current entry is the entry associated with the currently loaded page.
     * The index belongs to the range of {@code(index >= 0 && index < getEntries().size())}.
     *
     * @return the currentIndex property
     */
    public ReadOnlyIntegerProperty currentIndexProperty() {
        return currentIndex.getReadOnlyProperty();
    }

    public int getCurrentIndex() {
        return currentIndexProperty().get();
    }

    private void setCurrentIndex(int value) {
        currentIndex.set(value);
    }

    private IntegerProperty maxSize;

    /**
     * Defines the maximum size of the history list.
     * If the list reaches its maximum and a new entry is added,
     * the first entry is removed from the history.
     * <p>
     * The value specified for this property can not be negative, otherwise
     * {@code IllegalArgumentException} is thrown.
     *
     * @defaultValue 100
     * @return the maxSize property
     */
    public IntegerProperty maxSizeProperty()  {
        if (maxSize == null) {
            maxSize = new SimpleIntegerProperty(this, "maxSize", 100) {
                @Override
                public void set(int value) {
                    if (value < 0) {
                        throw new IllegalArgumentException("value cannot be negative.");
                    }
                    super.set(value);
                }
            };
        }
        return maxSize;
    }

    public void setMaxSize(int value) {
        maxSizeProperty().set(value);
        bfl.setMaximumSize(value);
    }

    public int getMaxSize() {
        return maxSizeProperty().get();
    }

    /**
     * Returns an unmodifiable observable list of all entries in the history.
     *
     * @return list of all history entries
     */
    public ObservableList<Entry> getEntries() {
        return ulist;
    }

    /**
     * Navigates the web engine to the URL defined by the {@code Entry} object
     * within the specified position relative to the current entry. A negative
     * {@code offset} value specifies the position preceding to the current entry,
     * and a positive {@code offset} value specifies the position following the
     * current entry. For example, -1 points to the previous entry, and 1 points
     * to the next entry, corresponding to pressing a web browser's 'back'
     * and 'forward' buttons, respectively.
     *
     * The zero {@code offset} value is silently ignored (no-op).
     *
     * The effective entry position should belong to the rage of [0..size-1].
     * Otherwise, {@code IndexOutOfBoundsException} is thrown.
     *
     * @param offset a negative value specifies a position preceding the
     *        current entry, a positive value specifies a position following
     *        the current entry, zero value causes no effect
     * @throws IndexOutOfBoundsException if the effective entry position is out
     *           of range
     */
    public void go(int offset) throws IndexOutOfBoundsException {
        if (offset == 0)
            return;

        int index = getCurrentIndex() + offset;
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("the effective index " + index
                                                + " is out of the range [0.."
                                                + (list.size() - 1) + "]");
        }
        bfl.setCurrentIndex(index);
    }
}
