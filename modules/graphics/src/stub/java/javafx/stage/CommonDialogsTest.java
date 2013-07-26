/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.pgstub.StubToolkit.CommonDialogsSupport;
import com.sun.javafx.tk.FileChooserType;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public final class CommonDialogsTest {
    private StubToolkit toolkit;
    private StubCommonDialogs stubDialogs;

    @Before
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        stubDialogs = new StubCommonDialogs();

        toolkit.setCommonDialogsSupport(stubDialogs);
    }

    @After
    public void tearDown() {
        toolkit.setCommonDialogsSupport(null);
    }

    @Test
    public void testFileChooser_showOpenDialog() {
        final FileChooser fileChooser = new FileChooser();
        final ExtensionFilter txtFiles =
                new ExtensionFilter("Text Files", "*.txt");
        final ExtensionFilter jpgFiles =
                new ExtensionFilter("JPEG Files", "*.jpg");
        final File initialDirectory = new File(".");

        fileChooser.setTitle("Open Single");
        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.setInitialFileName("open.txt");
        fileChooser.getExtensionFilters().addAll(txtFiles, jpgFiles);
        fileChooser.setSelectedExtensionFilter(txtFiles);

        Assert.assertEquals(
                null, fileChooser.showOpenDialog(null));
        Assert.assertEquals(FileChooserType.OPEN,
                            stubDialogs.getFileChooserType());
        Assert.assertEquals(null, stubDialogs.getOwnerWindow());
        Assert.assertEquals("Open Single", stubDialogs.getTitle());
        Assert.assertEquals(initialDirectory,
                            stubDialogs.getInitialDirectory());
        Assert.assertEquals("open.txt", stubDialogs.getInitialFileName());
        Assert.assertArrayEquals(
                new Object[] { txtFiles, jpgFiles },
                stubDialogs.getExtensionFilters().toArray());
        Assert.assertEquals(txtFiles, stubDialogs.getSelectedExtensionFilter());
    }

    @Test
    public void testFileChooser_showOpenMultipleDialog() {
        final FileChooser fileChooser = new FileChooser();
        final ExtensionFilter allFiles =
                new ExtensionFilter("All Files", "*.*");

        fileChooser.setTitle("Open Multiple");
        fileChooser.getExtensionFilters().addAll(allFiles);

        Assert.assertEquals(
                null, fileChooser.showOpenMultipleDialog(null));
        Assert.assertEquals(FileChooserType.OPEN_MULTIPLE,
                            stubDialogs.getFileChooserType());
        Assert.assertEquals(null, stubDialogs.getOwnerWindow());
        Assert.assertEquals("Open Multiple", stubDialogs.getTitle());
        Assert.assertEquals(null, stubDialogs.getInitialDirectory());
        Assert.assertEquals(null, stubDialogs.getInitialFileName());
        Assert.assertArrayEquals(
                new Object[] { allFiles },
                stubDialogs.getExtensionFilters().toArray());
        Assert.assertEquals(null, stubDialogs.getSelectedExtensionFilter());
    }

    @Test
    public void testFileChooser_showSaveDialog() {
        final FileChooser fileChooser = new FileChooser();
        final File initialDirectory = new File(".");

        fileChooser.setTitle("Save");
        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.setInitialFileName("save.txt");

        Assert.assertEquals(
                null, fileChooser.showSaveDialog(null));
        Assert.assertEquals(FileChooserType.SAVE,
                            stubDialogs.getFileChooserType());
        Assert.assertEquals(null, stubDialogs.getOwnerWindow());
        Assert.assertEquals("Save", stubDialogs.getTitle());
        Assert.assertEquals(initialDirectory,
                            stubDialogs.getInitialDirectory());
        Assert.assertEquals("save.txt", stubDialogs.getInitialFileName());
        Assert.assertEquals(0, stubDialogs.getExtensionFilters().size());
        Assert.assertEquals(null, stubDialogs.getSelectedExtensionFilter());
    }

    @Test
    public void testDirectoryChooser_showDialog() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        final File initialDirectory = new File(".");

        directoryChooser.setTitle("Open");
        directoryChooser.setInitialDirectory(initialDirectory);

        Assert.assertEquals(
                null, directoryChooser.showDialog(null));
        Assert.assertEquals(null, stubDialogs.getOwnerWindow());
        Assert.assertEquals("Open", stubDialogs.getTitle());
        Assert.assertEquals(initialDirectory,
                            stubDialogs.getInitialDirectory());
    }

    private static final class StubCommonDialogs
            implements CommonDialogsSupport {
        private FileChooserType fileChooserType;
        private TKStage ownerWindow;
        private String title;
        private File initialDirectory;
        private String initialFileName;
        private List<ExtensionFilter> extensionFilters;
        private ExtensionFilter selectedExtensionFilter;

        public FileChooserType getFileChooserType() {
            return fileChooserType;
        }

        public TKStage getOwnerWindow() {
            return ownerWindow;
        }

        public String getTitle() {
            return title;
        }

        public File getInitialDirectory() {
            return initialDirectory;
        }

        public String getInitialFileName() {
            return initialFileName;
        }

        public List<ExtensionFilter> getExtensionFilters() {
            return extensionFilters;
        }

        public ExtensionFilter getSelectedExtensionFilter() {
            return selectedExtensionFilter;
        }

        @Override
        public FileChooserResult showFileChooser(
                              final TKStage ownerWindow,
                              final String title,
                              final File initialDirectory,
                              final String initialFileName,
                              final FileChooserType fileChooserType,
                              final List<ExtensionFilter> extensionFilters,
                              final ExtensionFilter selectedFilter) {
            this.ownerWindow = ownerWindow;
            this.title = title;
            this.initialDirectory = initialDirectory;
            this.initialFileName = initialFileName;
            this.fileChooserType = fileChooserType;
            this.extensionFilters = extensionFilters;
            this.selectedExtensionFilter = selectedFilter;

            return null;
        }

        @Override
        public File showDirectoryChooser(final TKStage ownerWindow,
                                         final String title,
                                         final File initialDirectory) {
            this.ownerWindow = ownerWindow;
            this.title = title;
            this.initialDirectory = initialDirectory;

            return null;
        }
    }
}
