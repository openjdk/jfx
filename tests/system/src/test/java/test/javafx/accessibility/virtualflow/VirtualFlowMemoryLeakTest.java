/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.accessibility.virtualflow;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.javafx.PlatformUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.stage.Stage;
import test.util.Util;
import test.util.memory.JMemoryBuddy;

public class VirtualFlowMemoryLeakTest {

    private static CountDownLatch startupLatch;
    private static CountDownLatch screenReaderLatch = new CountDownLatch(1);

    public static class TestApp extends Application {
        @Override
        public void start(final Stage stage) throws Exception {
            final Scene scene = new Scene(createRoot(), 200, 200);
            stage.setScene(scene);
            Platform.runLater(startupLatch::countDown);
            stage.show();

            Platform.accessibilityActiveProperty()
                    .addListener((obs, prevActive, active) -> updateScreenReaderStatus(active));
            updateScreenReaderStatus(Platform.isAccessibilityActive());
        }
    }

    private static void updateScreenReaderStatus(final boolean active) {
        if (active) {
            screenReaderLatch.countDown();
        }
    }

    private final static ObservableList<Item> items = FXCollections.observableArrayList();
    private final static AtomicLong counter = new AtomicLong();

    static class Item {

        private final ReadOnlyObjectProperty<Long> id;

        public Item(final long id) {
            this.id = new SimpleObjectProperty<>(id);
        }

        public ReadOnlyObjectProperty<Long> idProperty() {
            return id;
        }
    }

    private static Parent createRoot() {
        tableView = createTableView();
        return tableView;
    }

    private static void addItem() {
        items.add(0, new Item(counter.incrementAndGet()));

        final TableViewSelectionModel<Item> sm = tableView.getSelectionModel();
        if (sm.getSelectedItems().isEmpty()) {
            sm.selectLast();
        }
        if (!tableView.isFocused()) {
            tableView.requestFocus();
        }
    }

    private static final AtomicBoolean itemsPopulatedAndCleared = new AtomicBoolean(false);
    private static WeakReference<TableRow<Item>> firstRowRef;

    private static TableView<Item> tableView;

    private static TableView<Item> createTableView() {
        final TableView<Item> tableView = new TableView<>(items);

        tableView.setRowFactory(param -> {
            final TableRow<Item> row = new TableRow<>();
            if (itemsPopulatedAndCleared.get()) {
                if (firstRowRef == null) {
                    firstRowRef = new WeakReference<>(row);
                }
            }
            return row;
        });

        final TableColumn<Item, Long> idColumn = new TableColumn<>();
        idColumn.setCellValueFactory(cd -> cd.getValue().idProperty());

        tableView.getColumns().add(idColumn);
        return tableView;
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        try {
            if (!startupLatch.await(10, SECONDS)) {
                fail("Timeout waiting for FX runtime to start");
            }
        } catch (final InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @AfterClass
    public static void teardown() {
        Platform.exit();
    }

    @Test
    public void test_JDK8203345() throws Exception {
        assumeTrue(PlatformUtil.isMac() || PlatformUtil.isWindows());

        final boolean screenReaderPresent = screenReaderLatch.await(5_000, MILLISECONDS);
        assumeTrue(screenReaderPresent);

        runAndWait(() -> addItem());
        runAndWait(() -> items.clear());

        itemsPopulatedAndCleared.set(true);

        for (int i = 0; i < 20; i++) {
            runAndWait(() -> addItem());
        }
        runAndWait(() -> items.clear());

        JMemoryBuddy.assertCollectable(firstRowRef);
    }

    private void runAndWait(final Runnable runnable) {
        Util.runAndWait(runnable);
    }
}
