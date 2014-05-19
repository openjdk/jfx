/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.library;

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReport;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReportEntry;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.beans.property.BooleanProperty;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 *
 */
public class ImportWindowController extends AbstractModalDialog {

    final List<File> importFiles;
    private final LibraryPanelController libPanelController;
    Task<List<JarReport>> exploringTask = null;
    URLClassLoader importClassLoader;
    Node zeNode = new Label(I18N.getString("import.preview.unable"));
    double builtinPrefWidth;
    double builtinPrefHeight;
    private int numOfImportedJar;
    
    // At first we put in this collection the items which are already excluded,
    // basically all which are listed in the filter file.
    // When constructing the list of items discovered in new jar file being imported
    // we uncheck already excluded items and remove them from the collection.
    // When the user clicks the Import button the collection might contain the
    // items we retain from older import actions.
    private List<String> alreadyExcludedItems = new ArrayList<>();

    public enum PrefSize {

        DEFAULT, TWO_HUNDRED_BY_ONE_HUNDRED, TWO_HUNDRED_BY_TWO_HUNDRED
    };
    
    @FXML
    private VBox leftHandSidePart;

    @FXML
    private Label processingLabel;

    @FXML
    ProgressIndicator processingProgressIndicator;

    @FXML
    ListView<ImportRow> importList = new ListView<>();

    @FXML
    ChoiceBox<String> defSizeChoice;
    
    @FXML
    private Label sizeLabel;

    @FXML
    private SplitPane topSplitPane;
    
    @FXML
    Group previewGroup;
    
    @FXML
    Label numOfItemsLabel;
    
    @FXML
    Label classNameLabel;
    
    @FXML
    Label previewHintLabel;
    
    @FXML
    ToggleButton checkAllUncheckAllToggle;
    
    public ImportWindowController(LibraryPanelController lpc, List<File> files, Window owner) {
        super(ImportWindowController.class.getResource("ImportDialog.fxml"), I18N.getBundle(), owner); //NOI18N
        libPanelController = lpc;
        importFiles = new ArrayList<>(files);
    }

    /*
     * Event handlers
     */
    /* TO BE SOLVED
     We have an issue with the exploration of SOME jar files.
     If e.g. you use sa-jdi.jar (take it in the JRE or JDK tree) then a NPE as
     the one below will be printed but cannot be caught in the code of this class.
     And from there we won't be able to exit from SB, whatever the action we take
     on the import window (Cancel or Import).
     Yes the window goes away but some thread refuse to give up.
     I noticed two non daemon threads:
     AWT-EventQueue-0
     AWT-Shutdown
    
     java.lang.NullPointerException
     at java.util.StringTokenizer.<init>(StringTokenizer.java:199)
     at java.util.StringTokenizer.<init>(StringTokenizer.java:221)
     at sun.jvm.hotspot.tools.jcore.PackageNameFilter.<init>(PackageNameFilter.java:41)
     at sun.jvm.hotspot.tools.jcore.PackageNameFilter.<init>(PackageNameFilter.java:36)
     at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
     at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:57)
     at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
     at java.lang.reflect.Constructor.newInstance(Constructor.java:414)
     at java.lang.Class.newInstance(Class.java:444)
     at sun.reflect.misc.ReflectUtil.newInstance(ReflectUtil.java:47)
     at javafx.fxml.FXMLLoader$InstanceDeclarationElement.constructValue(FXMLLoader.java:883)
     at javafx.fxml.FXMLLoader$ValueElement.processStartElement(FXMLLoader.java:614)
     at javafx.fxml.FXMLLoader.processStartElement(FXMLLoader.java:2491)
     at javafx.fxml.FXMLLoader.load(FXMLLoader.java:2300)
     at com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer.instantiateWithFXMLLoader(JarExplorer.java:83)
     at com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer.exploreEntry(JarExplorer.java:117)
     at com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer.explore(JarExplorer.java:43)
     at com.oracle.javafx.scenebuilder.kit.editor.panel.library.ImportWindowController$2.call(ImportWindowController.java:155)
     at com.oracle.javafx.scenebuilder.kit.editor.panel.library.ImportWindowController$2.call(ImportWindowController.java:138)
     at javafx.concurrent.Task$TaskCallable.call(Task.java:1376)
     at java.util.concurrent.FutureTask.run(FutureTask.java:262)
     at java.lang.Thread.run(Thread.java:724)
     */
    @Override
    protected void cancelButtonPressed(ActionEvent e) {
        if (exploringTask != null && exploringTask.isRunning()) {
            exploringTask.setOnCancelled(new EventHandler<WorkerStateEvent>() {

                @Override
                public void handle(WorkerStateEvent t) {
//                    System.out.println("Exploration of jar files has been cancelled"); //NOI18N
                    getStage().close();
                }
            });
            exploringTask.cancel(true);
        } else {
            getStage().close();
        }
        
        exploringTask = null;
        
        try {
            closeClassLoader();
        } catch (IOException ex) {
            showErrorDialog(ex);
        }
    }

    @Override
    protected void okButtonPressed(ActionEvent e) {
        exploringTask = null;
        getStage().close();
        
        try {
            closeClassLoader();
            libPanelController.copyFilesToUserLibraryDir(importFiles);
            UserLibrary userLib = ((UserLibrary) libPanelController.getEditorController().getLibrary());
            userLib.setFilter(getExcludedItems());
        } catch (IOException ex) {
            showErrorDialog(ex);
        } finally {
            alreadyExcludedItems.clear();
        }
    }

    @Override
    protected void actionButtonPressed(ActionEvent e) {
        // NOTHING TO DO (no ACTION button)
    }

    /*
     * AbstractFxmlWindowController
     */
    @Override
    public void onCloseRequest(WindowEvent event) {
        cancelButtonPressed(null);
    }

    @Override
    public void controllerDidLoadContentFxml() {
        assert topSplitPane != null;
        // The SplitPane should not be visible from the beginning: only the progressing bar is initially visible.
        assert topSplitPane.isVisible() == false;
        assert processingLabel != null;
        assert processingProgressIndicator != null;
        assert sizeLabel != null;
        assert previewGroup != null;
        assert importList != null;
        assert defSizeChoice != null;
        assert numOfItemsLabel != null;
        assert leftHandSidePart != null;
        assert classNameLabel != null;
        assert previewHintLabel != null;
        assert checkAllUncheckAllToggle != null;
        
        // Setup dialog buttons
        setOKButtonVisible(true);
        setDefaultButtonID(ButtonID.OK);
        setShowDefaultButton(true);
        
        // Setup size choice box
        defSizeChoice.getItems().clear();
        // Care to have values in sync with definition of PrefSize
        defSizeChoice.getItems().addAll(I18N.getString("import.choice.builtin"),
                "200 x 100", "200 x 200"); //NOI18N
        defSizeChoice.getSelectionModel().selectFirst();
        defSizeChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                assert t1 instanceof Integer;
                updateSize((Integer)t1);
            }
        });

        // Setup Select All / Unselect All toggle
        // Initially all items are Selected.
        checkAllUncheckAllToggle.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    for (ImportRow row : importList.getItems()) {
                        row.setImportRequired(false);
                    }
                    checkAllUncheckAllToggle.setText(I18N.getString("import.toggle.checkall"));
                } else {
                    for (ImportRow row : importList.getItems()) {
                        row.setImportRequired(true);
                    }
                    checkAllUncheckAllToggle.setText(I18N.getString("import.toggle.uncheckall"));
                }
            }
        });
                
        setProcessing();
        
        // We do not want the list becomes larger when the window is made larger.
        // The way to make the list larger is to use the splitter.
        SplitPane.setResizableWithParent(leftHandSidePart, false);
        
        work();
    }

    /*
     * AbstractWindowController
     */
    @Override
    protected void controllerDidCreateStage() {
        getStage().setTitle(I18N.getString("import.window.title"));
        getStage().initModality(Modality.APPLICATION_MODAL);
    }

    /*
     * Private
     */
    
    private void closeClassLoader() throws IOException {
        if (importClassLoader != null) {
            importClassLoader.close();
        }
    }

    // This method returns a new list of File made of the union of the provided
    // one and jar files found in the user library dir.
    List<File> buildListOfAllFiles(List<File> importFiles) throws IOException {
        final List<File> res = new ArrayList<>(importFiles);
        String userLibraryDir = ((UserLibrary) libPanelController.getEditorController().getLibrary()).getPath();
        Path userLibraryPath = new File(userLibraryDir).toPath();

        try (Stream<Path> pathStream = Files.list(userLibraryPath)) {
            Iterator<Path> pathIterator = pathStream.iterator();
            while (pathIterator.hasNext()) {
                Path element = pathIterator.next();
                if (element.toString().endsWith(".jar")) { //NOI18N
//                    System.out.println("ImportWindowController::buildListOfAllFiles: Adding " + element); //NOI18N
                    res.add(element.toFile());
                }
            }
        }
        
        return res;
    }

    private void work() {
        exploringTask = new Task<List<JarReport>>() {

            @Override
            protected List<JarReport> call() throws Exception {
                final List<JarReport> res = new ArrayList<>();
                numOfImportedJar = importFiles.size();
                // The classloader takes in addition all already existing
                // jar files stored in the user lib dir.
                final List<File> allFiles = buildListOfAllFiles(importFiles);
                final URLClassLoader classLoader = getClassLoaderForFiles(allFiles);
                int index = 1;
                for (File file : importFiles) {
                    if (isCancelled()) {
                        updateMessage(I18N.getString("import.work.cancelled"));
                        break;
                    }
                    updateMessage(I18N.getString("import.work.exploring", file.getName()));
//                    System.out.println("[" + index + "/" + max + "] Exploring file " + file.getName()); //NOI18N
                    final JarExplorer explorer = new JarExplorer(Paths.get(file.getAbsolutePath()));
                    final JarReport jarReport = explorer.explore(classLoader);
                    res.add(jarReport);
                    updateProgress(index, numOfImportedJar);
                    index++;
                }

                updateProgress(numOfImportedJar, numOfImportedJar);
                updateImportClassLoader(classLoader);
                return res;
            }
        };

        Thread th = new Thread(exploringTask);
        th.setDaemon(true);
        processingProgressIndicator.progressProperty().bind(exploringTask.progressProperty());

        // We typically enter this handler when dropping jar files such as
        // rt.jar from Java Runtime.
        exploringTask.setOnFailed(new EventHandler<WorkerStateEvent>() {

            @Override
            public void handle(WorkerStateEvent t) {
                // See in setOnSucceeded the explanation for the toFront below.
                getStage().toFront();
                updateNumOfItemsLabelAndSelectionToggleState();
            }
        });
        
        // We construct the import list only if exploration of jar files does well.
        // If Cancel is called during the construction of the list then the import
        // window is closed but the construction itself will continue up to the
        // end. Do we want to make it interruptible ?
        exploringTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

            @Override
            public void handle(WorkerStateEvent t) {
                assert Platform.isFxApplicationThread();
                // This toFront() might not be necessary because import window is modal
                // and is chained to the document window. Anyway experience showed
                // we need it (FX 8 b106). This is suspicious, to be investigated ...
                // But more tricky is why toFront() is called here. Mind that when toFront()
                // is called while isShowing() returns false isn't effective: that's
                // why toFront called at the end of controllerDidCreateStage() or
                // controllerDidLoadContentFxml() wasn't an option. Below is the
                // earliest place it has been proven effective, at least on my machine.
                getStage().toFront();
                
                try {
                    // We get the set of items which are already excluded prior to the current import.
                    UserLibrary userLib = ((UserLibrary) libPanelController.getEditorController().getLibrary());
                    alreadyExcludedItems = userLib.getFilter();
                    
                    List<JarReport> jarReportList = exploringTask.get(); // blocking call
                    final Callback<ImportRow, ObservableValue<Boolean>> importRequired
                            = new Callback<ImportRow, ObservableValue<Boolean>>() {
                                @Override
                                public BooleanProperty call(ImportRow row) {
                                    return row.importRequired();
                                }
                            };
                    importList.setCellFactory(CheckBoxListCell.forListView(importRequired));

                    for (JarReport jarReport : jarReportList) {
                        for (JarReportEntry e : jarReport.getEntries()) {
                            if ((e.getStatus() == JarReportEntry.Status.OK) && e.isNode()) {
                                boolean checked = true;
                                // If the class we import is already listed as an excluded one
                                // then it must appear unchecked in the list.
                                if (alreadyExcludedItems.contains(e.getKlass().getCanonicalName())) {
                                    checked = false;
                                    alreadyExcludedItems.remove(e.getKlass().getCanonicalName());
                                }
                                final ImportRow importRow = new ImportRow(checked, e, null);
                                importList.getItems().add(importRow);
                                importRow.importRequired().addListener(new ChangeListener<Boolean>() {

                                    @Override
                                    public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                                        final int numOfComponentToImport = getNumOfComponentToImport(importList);
                                        updateOKButtonTitle(numOfComponentToImport);
                                        updateSelectionToggleText(numOfComponentToImport);
                                    }
                                });
                            }
                        }
                    }
                    
                    // Sort based on the simple class name.
                    Collections.sort(importList.getItems(), new ImportRowComparator());

                    final int numOfComponentToImport = getNumOfComponentToImport(importList);
                    updateOKButtonTitle(numOfComponentToImport);
                    updateSelectionToggleText(numOfComponentToImport);
                    updateNumOfItemsLabelAndSelectionToggleState();
                } catch (InterruptedException | ExecutionException | IOException ex) {
                    getStage().close();
                    showErrorDialog(ex);
                }

                unsetProcessing();
            }
        });

        th.start();
    }
    
    private void showErrorDialog(Exception exception) {
        final ErrorDialog errorDialog = new ErrorDialog(null);
        errorDialog.setTitle(I18N.getString("import.error.title"));
        errorDialog.setMessage(I18N.getString("import.error.message"));
        errorDialog.setDetails(I18N.getString("import.error.details"));
        errorDialog.setDebugInfoWithThrowable(exception);
        errorDialog.showAndWait();
    }

    void updateImportClassLoader(URLClassLoader cl) {
        this.importClassLoader = cl;
    }

    void unsetProcessing() {
        processingProgressIndicator.setVisible(false);
        processingLabel.setVisible(false);
        topSplitPane.setVisible(true);

        importList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ImportRow>() {

            @Override
            public void changed(ObservableValue<? extends ImportRow> ov, ImportRow t, ImportRow t1) {
                previewGroup.getChildren().clear();
                final String fxmlText = JarExplorer.makeFxmlText(t1.getJarReportEntry().getKlass());
                try {
                    FXOMDocument fxomDoc = new FXOMDocument(fxmlText, null, importClassLoader, null);
                    zeNode = (Node) fxomDoc.getSceneGraphRoot();
                } catch (IOException ioe) {
                    showErrorDialog(ioe);
                }
                
                // In order to get valid bounds I need to put the node into a
                // scene and ask for full layout.
                try {
                    final Group visualGroup = new Group(zeNode);
                    final Scene hiddenScene = new Scene(visualGroup);
                    Stage hiddenStage = new Stage();
                    hiddenStage.setScene(hiddenScene);
                    visualGroup.applyCss();
                    visualGroup.layout();
                    final Bounds zeBounds = zeNode.getLayoutBounds();
                    builtinPrefWidth = zeBounds.getWidth();
                    builtinPrefHeight = zeBounds.getHeight();
                    // Detach the scene !
                    hiddenScene.setRoot(new Group());
                    hiddenStage.close();
                } catch (Error e) {
                    // Experience shows that with rogue jar files (a jar file
                    // unlikely to contain FX controls) we can enter here.
                    // Anything better to do than setting pref size to 0 ?
                    builtinPrefWidth = 0;
                    builtinPrefHeight = 0;
                }
                
                if (builtinPrefWidth == 0 || builtinPrefHeight == 0) {
                    ((Region) zeNode).setPrefSize(200, 200);
                    setSizeLabel(PrefSize.TWO_HUNDRED_BY_TWO_HUNDRED);
                    defSizeChoice.getSelectionModel().select(2);
                } else {
                    setSizeLabel(PrefSize.DEFAULT);
                    defSizeChoice.getSelectionModel().selectFirst();
                }
                previewGroup.getChildren().add(zeNode);
                defSizeChoice.setDisable(false);
                classNameLabel.setText(t1.getJarReportEntry().getKlass().getName());
            }
        });

        // We avoid to get an empty Preview area at first.
        if (importList.getItems().size() > 0) {
            importList.getSelectionModel().selectFirst();
        }
    }

    private URLClassLoader getClassLoaderForFiles(List<File> files) {
        return new URLClassLoader(makeURLArrayFromFiles(files));
    }

    private URL[] makeURLArrayFromFiles(List<File> files) {
        final URL[] result = new URL[files.size()];
        try {
            int index = 0;
            for (File file : files) {
                result[index] = file.toURI().toURL();
                index++;
            }
        } catch (MalformedURLException x) {
            throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
        }

        return result;
    }

    private void setProcessing() {
        cancelButton.setDefaultButton(true);
    }

    private int getNumOfComponentToImport(final ListView<ImportRow> list) {
        int res = 0;
        
        for (final ImportRow row : list.getItems()) {
            if (row.isImportRequired()) {
                res++;
            }
        }
        
        return res;
    }
    
    private List<String> getExcludedItems() {
        List<String> res = new ArrayList<>(alreadyExcludedItems);
        
        for (ImportRow row : importList.getItems()) {
            if (! row.isImportRequired()) {
                res.add(row.getCanonicalClassName());
            }
        }
        return res;
    }

    // The title of the button is important in the sense it says to the user
    // what action will be taken.
    // In the most common case one or more component are selected in the list,
    // but it is also possible to get an empty list, in which case the user may
    // want to import the jar file anyway; it makes sense in ooder to resolve
    // dependencies other jars have onto it.
    // See DTL-6531 for details.
    private void updateOKButtonTitle(int numOfComponentToImport) {
        if (numOfComponentToImport == 0) {
            if (numOfImportedJar == 1) {
                setOKButtonTitle(I18N.getString("import.button.import.jar"));
            } else {
                setOKButtonTitle(I18N.getString("import.button.import.jars"));
            }
        } else if (numOfComponentToImport == 1) {
            setOKButtonTitle(I18N.getString("import.button.import.component"));
        } else {
            setOKButtonTitle(I18N.getString("import.button.import.components"));
        }
    }
    
    void updateNumOfItemsLabelAndSelectionToggleState() {
        final int num = importList.getItems().size();
        if (num == 0 || num == 1) {
            numOfItemsLabel.setText(num + " " //NOI18N
                    + I18N.getString("import.num.item"));
        } else {
            numOfItemsLabel.setText(num + " " //NOI18N
                    + I18N.getString("import.num.items"));
        }
        
        if (num >= 1) {
            checkAllUncheckAllToggle.setDisable(false);
        }
    }

    private void updateSelectionToggleText(int numOfComponentToImport) {
        if (numOfComponentToImport == 0) {
            checkAllUncheckAllToggle.setText(I18N.getString("import.toggle.checkall"));
        } else {
            checkAllUncheckAllToggle.setText(I18N.getString("import.toggle.uncheckall"));
        }
    }
        
    // NOTE At the end of the day some tooling in metadata will supersedes the
    // use of this method that is only able to deal with a Region, ignoring all
    // other cases.
    private void updateSize(Integer choice) {
        if (zeNode instanceof Region) {
            PrefSize prefSize = PrefSize.values()[choice];
            switch (prefSize) {
                case DEFAULT:
                    ((Region) zeNode).setPrefSize(builtinPrefWidth, builtinPrefHeight);
                    setSizeLabel(prefSize);
                    break;
                case TWO_HUNDRED_BY_ONE_HUNDRED:
                    ((Region) zeNode).setPrefSize(200, 100);
                    setSizeLabel(prefSize);
                    break;
                case TWO_HUNDRED_BY_TWO_HUNDRED:
                    ((Region) zeNode).setPrefSize(200, 200);
                    setSizeLabel(prefSize);
                    break;
                default:
                    break;
            }
            
            defSizeChoice.getSelectionModel().select(choice);
        }
    }
    
    private void setSizeLabel(PrefSize ps) {
        switch (ps) {
            case DEFAULT:
                sizeLabel.setText(builtinPrefWidth + " x " + builtinPrefHeight); //NOI18N
                break;
            case TWO_HUNDRED_BY_ONE_HUNDRED:
                sizeLabel.setText("200 x 100"); //NOI18N
                break;
            case TWO_HUNDRED_BY_TWO_HUNDRED:
                sizeLabel.setText("200 x 200"); //NOI18N
                break;
            default:
                break;
        }
    }
}
