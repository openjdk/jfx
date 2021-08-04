/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import java.util.Arrays;
import java.util.Collection;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;

@RunWith(Parameterized.class)
public class EventAnyTest {
    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { CheckBoxTreeItem.TreeModificationEvent.ANY, checkBoxTreeEvent(), Rectangle.class, true},
            { CheckBoxTreeItem.TreeModificationEvent.ANY, listViewEditEvent(), Rectangle.class, false},
            { ListView.EditEvent.ANY, listViewEditEvent(), ListView.class, true},
            { ListView.EditEvent.ANY, checkBoxTreeEvent(), ListView.class, false},
            { ScrollToEvent.ANY, scrollToEvent(), Rectangle.class, true},
            { ScrollToEvent.ANY, listViewEditEvent(), Rectangle.class, false},
            { SortEvent.ANY, sortEvent(), Rectangle.class, true},
            { SortEvent.ANY, scrollToEvent(), Rectangle.class, false},
            { TableColumn.CellEditEvent.ANY, tableColumnCellEditEvent(), Rectangle.class, true },
            { TableColumn.CellEditEvent.ANY, listViewEditEvent(), Rectangle.class, false },
            { TreeItem.TreeModificationEvent.ANY, treeItemModificationEvent(), Rectangle.class, true},
            { TreeItem.TreeModificationEvent.ANY, checkBoxTreeEvent(), Rectangle.class, false},
            { TreeTableColumn.CellEditEvent.ANY, treeTableColumnCellEditEvent(), Rectangle.class, true },
            { TreeTableColumn.CellEditEvent.ANY, tableColumnCellEditEvent(), Rectangle.class, false },
            { TreeTableView.EditEvent.ANY, treeTableViewEditEvent(), TreeTableView.class, true },
            { TreeTableView.EditEvent.ANY, treeTableColumnCellEditEvent(), TreeTableView.class, false },
            { TreeView.EditEvent.ANY, treeViewEditEvent(), TreeView.class, true },
            { TreeView.EditEvent.ANY, treeTableViewEditEvent(), TreeView.class, false },
        });
    }

    private boolean delivered;
    private EventType type;
    private Event event;
    private Class target;
    private boolean matches;

    public EventAnyTest(EventType type, Event event, Class target, boolean matches) {
        this.type = type;
        this.event = event;
        this.matches = matches;
        this.target = target;
    }

    @Test
    public void testEventDelivery() throws Exception {
        Node n = (Node) target.getDeclaredConstructor().newInstance();
        delivered = false;

        n.addEventHandler(type, event1 -> {
            delivered = true;
        });

        Event.fireEvent(n, event);
        assertTrue(matches == delivered);
    }

    private static Event checkBoxTreeEvent() {
        return new CheckBoxTreeItem.TreeModificationEvent<>(
                CheckBoxTreeItem.checkBoxSelectionChangedEvent(), null, true);

    }

    private static Event listViewEditEvent() {
        return new ListView.EditEvent<String>(new ListView<String>(),
                ListView.<String>editCommitEvent(), "", 1);
    }

    private static Event scrollToEvent() {
        return new ScrollToEvent(null, null, ScrollToEvent.scrollToColumn(),
                new ScrollPane());
    }

    private static Event sortEvent() {
        return new SortEvent(null, null);
    }

    private static Event tableColumnCellEditEvent() {
        TableView<String> tw = new TableView<String>();
        return new TableColumn.CellEditEvent<String, String>(
                tw, new TablePosition<String, String>(tw, 1, null),
                TableColumn.<String, String>editCommitEvent(), "");

    }

    private static Event treeItemModificationEvent() {
        return new TreeItem.TreeModificationEvent<>(
                TreeItem.graphicChangedEvent(), null, true);

    }

    private static Event treeTableColumnCellEditEvent() {
        TreeTableView<String> tw = new TreeTableView<String>();
        return new TreeTableColumn.CellEditEvent<String, String>(
                tw, new TreeTablePosition<String, String>(tw, 1, null),
                TreeTableColumn.<String, String>editCommitEvent(), "");

    }

    private static Event treeTableViewEditEvent() {
        TreeTableView<String> tw = new TreeTableView<String>();
        return new TreeTableView.EditEvent<String>(
                tw, TreeTableView.editCommitEvent(), null, "", "");
    }

    private static Event treeViewEditEvent() {
        TreeView<String> tw = new TreeView<String>();
        return new TreeView.EditEvent<String>(
                tw, TreeView.editCommitEvent(), null, "", "");
    }
}
