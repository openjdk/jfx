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
package com.oracle.javafx.scenebuilder.app;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController.ActionStatus;
import com.oracle.javafx.scenebuilder.app.about.AboutWindowController;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.menubar.MenuBarController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordDocument;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesWindowController;
import com.oracle.javafx.scenebuilder.app.template.FxmlTemplates;
import com.oracle.javafx.scenebuilder.app.template.TemplateDialogController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AlertDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 */
public class SceneBuilderApp extends Application implements AppPlatform.AppNotificationHandler {

    public enum ApplicationControlAction {

        ABOUT,
        NEW_FILE,
        NEW_ALERT_DIALOG,
        NEW_ALERT_DIALOG_CSS,
        NEW_ALERT_DIALOG_I18N,
        NEW_BASIC_APPLICATION,
        NEW_BASIC_APPLICATION_CSS,
        NEW_BASIC_APPLICATION_I18N,
        NEW_COMPLEX_APPLICATION,
        NEW_COMPLEX_APPLICATION_CSS,
        NEW_COMPLEX_APPLICATION_I18N,
        OPEN_FILE,
        CLOSE_FRONT_WINDOW,
        USE_DEFAULT_THEME,
        USE_DARK_THEME,
        SHOW_PREFERENCES,
        EXIT
    }
    
    public enum ToolTheme {
        DEFAULT,
        DARK
    }

    private static SceneBuilderApp singleton;
    private static String darkToolStylesheet;
    
    private final List<DocumentWindowController> windowList = new ArrayList<>();
    private final PreferencesWindowController preferencesWindowController
            = new PreferencesWindowController();
    private final AboutWindowController aboutWindowController
            = new AboutWindowController();
    private UserLibrary userLibrary;
    private MenuBarController defaultSystemMenuBarController; // Mac only
    private File nextInitialDirectory;
    private ToolTheme toolTheme = ToolTheme.DEFAULT;


    /*
     * Public
     */
    public static SceneBuilderApp getSingleton() {
        return singleton;
    }

    public void performControlAction(ApplicationControlAction a, DocumentWindowController source) {
        switch (a) {
            case ABOUT:
                aboutWindowController.openWindow();
                break;

            case NEW_FILE:
                final DocumentWindowController newWindow = makeNewWindow();
                newWindow.loadWithDefaultContent();
                newWindow.openWindow();
                break;

            case NEW_ALERT_DIALOG:
            case NEW_BASIC_APPLICATION:
            case NEW_COMPLEX_APPLICATION:
                performNewTemplate(a);
                break;

            case NEW_ALERT_DIALOG_CSS:
            case NEW_ALERT_DIALOG_I18N:
            case NEW_BASIC_APPLICATION_CSS:
            case NEW_BASIC_APPLICATION_I18N:
            case NEW_COMPLEX_APPLICATION_CSS:
            case NEW_COMPLEX_APPLICATION_I18N:
                performNewTemplateWithResources(a);
                break;

            case OPEN_FILE:
                performOpenFile(source);
                break;

            case CLOSE_FRONT_WINDOW:
                performCloseFrontWindow();
                break;
                
            case USE_DEFAULT_THEME:
                performUseToolTheme(ToolTheme.DEFAULT);
                break;

            case USE_DARK_THEME:
                performUseToolTheme(ToolTheme.DARK);
                break;

            case SHOW_PREFERENCES:
                preferencesWindowController.openWindow();
                break;

            case EXIT:
                performExit();
                break;
        }
    }
    

    public boolean canPerformControlAction(ApplicationControlAction a, DocumentWindowController source) {
        final boolean result;
        switch (a) {
            case ABOUT:
            case NEW_FILE:
            case NEW_ALERT_DIALOG:
            case NEW_BASIC_APPLICATION:
            case NEW_COMPLEX_APPLICATION:
            case NEW_ALERT_DIALOG_CSS:
            case NEW_ALERT_DIALOG_I18N:
            case NEW_BASIC_APPLICATION_CSS:
            case NEW_BASIC_APPLICATION_I18N:
            case NEW_COMPLEX_APPLICATION_CSS:
            case NEW_COMPLEX_APPLICATION_I18N:
            case OPEN_FILE:
            case SHOW_PREFERENCES:
            case EXIT:
                result = true;
                break;

            case CLOSE_FRONT_WINDOW:
                result = windowList.isEmpty() == false;
                break;
                
            case USE_DEFAULT_THEME:
                result = toolTheme != ToolTheme.DEFAULT;
                break;

            case USE_DARK_THEME:
                result = toolTheme != ToolTheme.DARK;
                break;

            default:
                result = false;
                assert false;
                break;
        }
        return result;
    }
    
    public void performOpenRecent(DocumentWindowController source, final File fxmlFile) {
        assert fxmlFile != null && fxmlFile.exists();

        final List<File> fxmlFiles = new ArrayList<>();
        fxmlFiles.add(fxmlFile);
        performOpenFiles(fxmlFiles, source);
    }

    public void documentWindowRequestClose(DocumentWindowController fromWindow) {
        closeWindow(fromWindow);
    }

    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    public List<DocumentWindowController> getDocumentWindowControllers() {
        return Collections.unmodifiableList(windowList);
    }

    public DocumentWindowController lookupDocumentWindowControllers(URL fxmlLocation) {
        assert fxmlLocation != null;

        DocumentWindowController result = null;
        try {
            final URI fxmlURI = fxmlLocation.toURI();
            for (DocumentWindowController dwc : windowList) {
                final URL docLocation = dwc.getEditorController().getFxmlLocation();
                if ((docLocation != null) && fxmlURI.equals(docLocation.toURI())) {
                    result = dwc;
                    break;
                }
            }
        } catch (URISyntaxException x) {
            // Should not happen
            throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
        }

        return result;
    }

    public DocumentWindowController lookupUnusedDocumentWindowController() {
        DocumentWindowController result = null;
        
        for (DocumentWindowController dwc : windowList) {
            if (dwc.isUnused()) {
                result = dwc;
                break;
            }
        }
        
        return result;
    }

    public void toggleDebugMenu() {
        final boolean visible;

        if (windowList.isEmpty()) {
            visible = false;
        } else {
            final DocumentWindowController dwc = windowList.get(0);
            visible = dwc.getMenuBarController().isDebugMenuVisible();
        }

        for (DocumentWindowController dwc : windowList) {
            dwc.getMenuBarController().setDebugMenuVisible(!visible);
        }

        if (defaultSystemMenuBarController != null) {
            defaultSystemMenuBarController.setDebugMenuVisible(!visible);
        }
    }

    public void updateNextInitialDirectory(File chosenFile) {
        assert chosenFile != null;

        final Path chosenFolder = chosenFile.toPath().getParent();
        if (chosenFolder != null) {
            nextInitialDirectory = chosenFolder.toFile();
        }
    }

    public File getNextInitialDirectory() {
        return nextInitialDirectory;
    }
    
    public static synchronized String getDarkToolStylesheet() {
        if (darkToolStylesheet == null) {
            final URL url = SceneBuilderApp.class.getResource("css/ThemeDark.css"); //NOI18N
            assert url != null;
            darkToolStylesheet = url.toExternalForm();
        }
        return darkToolStylesheet;
    }

    /*
     * Application
     */
    @Override
    public void start(Stage stage) throws Exception {
        assert singleton == null;
        singleton = this;
        setApplicationUncaughtExceptionHandler();

        try {
            if (AppPlatform.requestStart(this, getParameters()) == false) {
                // Start has been denied because another instance is running.
                Platform.exit();
            }
            // else {
            //      No other Scene Builder instance is already running.
            //      AppPlatform.requestStart() has/will invoke(d) handleLaunch().
            //      start() has now finished its job and should imply return.
            // }

        } catch (IOException x) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("alert.title.start"));
            errorDialog.setMessage(I18N.getString("alert.start.failure.message"));
            errorDialog.setDetails(I18N.getString("alert.start.failure.details"));
            errorDialog.setDebugInfoWithThrowable(x);
            errorDialog.showAndWait();
            Platform.exit();
        }
        
        logTimestamp(ACTION.START);
    }

    /*
     * AppPlatform.AppNotificationHandler
     */
    @Override
    public void handleLaunch(List<String> files) {
        setApplicationUncaughtExceptionHandler();

        // On Mac, AppPlatform disables implicit exit.
        // So we need to set a default system menu bar.
        if (Platform.isImplicitExit() == false) {
            defaultSystemMenuBarController = new MenuBarController(null);
            Deprecation.setDefaultSystemMenuBar(defaultSystemMenuBarController.getMenuBar());
        }

        // Load global application preferences
        final PreferencesController pc = PreferencesController.getSingleton();
        final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
        recordGlobal.readFromJavaPreferences();

        // Creates the user library
        userLibrary = new UserLibrary(AppPlatform.getUserLibraryFolder());
        // runLater below is here to fix DTL-6378
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                userLibrary.startWatching();
            }
        });

        if (files.isEmpty()) {
            // Creates an empty document
            final DocumentWindowController newWindow = makeNewWindow();
            newWindow.loadWithDefaultContent();
            newWindow.openWindow();

            // Show ScenicView Tool when the JVM is started with option -Dscenic.
            // NetBeans: set it on [VM Options] line in [Run] category of project's Properties.
            if (System.getProperty("scenic") != null) { //NOI18N
                Platform.runLater(new ScenicViewStarter(newWindow.getScene()));
            }
        } else {
            // Open files passed as arguments by the platform
            handleOpenFilesAction(files);
        }
    }

    @Override
    public void handleOpenFilesAction(List<String> files) {
        assert files != null;
        assert files.isEmpty() == false;

        final List<File> fileObjs = new ArrayList<>();
        for (String file : files) {
            fileObjs.add(new File(file));
        }

        performOpenFiles(fileObjs, null);
    }

    @Override
    public void handleMessageBoxFailure(Exception x) {
        final ErrorDialog errorDialog = new ErrorDialog(null);
        errorDialog.setTitle(I18N.getString("alert.title.messagebox"));
        errorDialog.setMessage(I18N.getString("alert.messagebox.failure.message"));
        errorDialog.setDetails(I18N.getString("alert.messagebox.failure.details"));
        errorDialog.setDebugInfoWithThrowable(x);
        errorDialog.showAndWait();
    }

    @Override
    public void handleQuitAction() {

        /*
         * Note : this callback is called on Mac OS X only when the user
         * selects the 'Quit App' command in the Application menu.
         * 
         * Before calling this callback, FX automatically sends a close event
         * to each open window ie DocumentWindowController.performCloseAction()
         * is invoked for each open window.
         * 
         * When we arrive here, windowList is empty if the user has confirmed
         * the close operation for each window : thus exit operation can
         * be performed. If windowList is not empty,  this means the user has 
         * cancelled at least one close operation : in that case, exit operation
         * should be not be executed.
         */
        if (windowList.isEmpty()) {
            logTimestamp(ACTION.STOP);
            Platform.exit();
        }
    }

    /**
     * Normally ignored in correctly deployed JavaFX application.
     * But on Mac OS, this method seems to be called by the javafx launcher.
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    /*
     * Private
     */
    public DocumentWindowController makeNewWindow() {
        final DocumentWindowController result = new DocumentWindowController();
        windowList.add(result);
        return result;
    }

    private void closeWindow(DocumentWindowController w) {
        assert windowList.contains(w);
        windowList.remove(w);
        w.closeWindow();
    }

    private static String displayName(String pathString) {
        return Paths.get(pathString).getFileName().toString();
    }

    /*
     * Private (control actions)
     */
    private void performOpenFile(DocumentWindowController fromWindow) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18N.getString("file.filter.label.fxml"),
                "*.fxml")); //NOI18N
        if (nextInitialDirectory != null) {
            fileChooser.setInitialDirectory(nextInitialDirectory);
        }
        final List<File> fxmlFiles = fileChooser.showOpenMultipleDialog(null);
        if (fxmlFiles != null) {
            assert fxmlFiles.isEmpty() == false;
            updateNextInitialDirectory(fxmlFiles.get(0));
            performOpenFiles(fxmlFiles, fromWindow);
        }
    }

    private void performNewTemplate(ApplicationControlAction action) {
        final DocumentWindowController newTemplateWindow = makeNewWindow();
        final URL url = FxmlTemplates.getContentURL(action);
        newTemplateWindow.loadFromURL(url);
        newTemplateWindow.openWindow();
    }

    private void performNewTemplateWithResources(ApplicationControlAction action) {
        final TemplateDialogController tdc = new TemplateDialogController(action);
        tdc.setToolStylesheet(getToolStylesheet());
        tdc.openWindow();
    }

    private void performCloseFrontWindow() {
        if (preferencesWindowController != null
                && preferencesWindowController.getStage().isFocused()) {
            preferencesWindowController.closeWindow();
        } else {
            for (DocumentWindowController dwc : windowList) {
                if (dwc.isFrontDocumentWindow()) {
                    dwc.performCloseFrontDocumentWindow();
                    break;
                }
            }
        }
    }

    private void performOpenFiles(List<File> fxmlFiles,
            DocumentWindowController fromWindow) {
        assert fxmlFiles != null;
        assert fxmlFiles.isEmpty() == false;

        final Map<File, IOException> exceptions = new HashMap<>();
        for (File fxmlFile : fxmlFiles) {
            try {
                final DocumentWindowController dwc
                        = lookupDocumentWindowControllers(fxmlFile.toURI().toURL());
                if (dwc != null) {
                    // fxmlFile is already opened
                    dwc.getStage().toFront();
                } else {
                    // Open fxmlFile
                    final DocumentWindowController hostWindow;
                    final DocumentWindowController unusedWindow
                            = lookupUnusedDocumentWindowController();
                    if (unusedWindow != null) {
                        hostWindow = unusedWindow;
                    } else {
                        hostWindow = makeNewWindow();
                    }
                    hostWindow.loadFromFile(fxmlFile);
                    hostWindow.openWindow();
                }
            } catch (IOException xx) {
                exceptions.put(fxmlFile, xx);
            }
        }

        switch (exceptions.size()) {
            case 0: { // Good
                // Update recent items with opened files
                final PreferencesController pc = PreferencesController.getSingleton();
                final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
                recordGlobal.addRecentItems(fxmlFiles);
                break;
            }
            case 1: {
                final File fxmlFile = exceptions.keySet().iterator().next();
                final Exception x = exceptions.get(fxmlFile);
                final ErrorDialog errorDialog = new ErrorDialog(null);
                errorDialog.setMessage(I18N.getString("alert.open.failure1.message", displayName(fxmlFile.getPath())));
                errorDialog.setDetails(I18N.getString("alert.open.failure1.details"));
                errorDialog.setDebugInfoWithThrowable(x);
                errorDialog.setTitle(I18N.getString("alert.title.open"));
                errorDialog.showAndWait();
                break;
            }
            default: {
                final ErrorDialog errorDialog = new ErrorDialog(null);
                if (exceptions.size() == fxmlFiles.size()) {
                    // Open operation has failed for all the files
                    errorDialog.setMessage(I18N.getString("alert.open.failureN.message"));
                    errorDialog.setDetails(I18N.getString("alert.open.failureN.details"));
                } else {
                    // Open operation has failed for some files
                    errorDialog.setMessage(I18N.getString("alert.open.failureMofN.message",
                            exceptions.size(), fxmlFiles.size()));
                    errorDialog.setDetails(I18N.getString("alert.open.failureMofN.details"));
                }
                errorDialog.setTitle(I18N.getString("alert.title.open"));
                errorDialog.showAndWait();
                break;
            }
        }
    }

    private void performExit() {

        // Collects the documents with pending changes
        final List<DocumentWindowController> pendingDocs = new ArrayList<>();
        for (DocumentWindowController dwc : windowList) {
            if (dwc.isDocumentDirty()) {
                pendingDocs.add(dwc);
            }
        }

        // Notifies the user if some documents are dirty
        final boolean exitConfirmed;
        switch (pendingDocs.size()) {
            case 0: {
                exitConfirmed = true;
                break;
            }

            case 1: {
                final DocumentWindowController dwc0 = pendingDocs.get(0);
                exitConfirmed = dwc0.performCloseAction() == ActionStatus.DONE;
                break;
            }

            default: {
                assert pendingDocs.size() >= 2;

                final AlertDialog d = new AlertDialog(null);
                d.setMessage(I18N.getString("alert.review.question.message", pendingDocs.size()));
                d.setDetails(I18N.getString("alert.review.question.details"));
                d.setOKButtonTitle(I18N.getString("label.review.changes"));
                d.setActionButtonTitle(I18N.getString("label.discard.changes"));
                d.setActionButtonVisible(true);

                switch (d.showAndWait()) {
                    default:
                    case OK: { // Review
                        int i = 0;
                        ActionStatus status;
                        do {
                            status = pendingDocs.get(i++).performCloseAction();
                        } while ((status == ActionStatus.DONE) && (i < pendingDocs.size()));
                        exitConfirmed = (status == ActionStatus.DONE);
                        break;
                    }
                    case CANCEL: {
                        exitConfirmed = false;
                        break;
                    }
                    case ACTION: { // Do not review
                        exitConfirmed = true;
                        break;
                    }
                }
                break;
            }
        }

        // Exit if confirmed
        if (exitConfirmed) {
            final PreferencesController pc = PreferencesController.getSingleton();
            for (DocumentWindowController dwc : new ArrayList<>(windowList)) {
                // Write to java preferences before closing
                final PreferencesRecordDocument recordDocument = pc.getRecordDocument(dwc);
                recordDocument.writeToJavaPreferences();
                documentWindowRequestClose(dwc);
            }
            logTimestamp(ACTION.STOP);
            // TODO (elp): something else here ?
            Platform.exit();
        }
    }
    
    private enum ACTION {START, STOP};
    
    private void logTimestamp(ACTION type) {
        switch (type) {
            case START:
                Logger.getLogger(this.getClass().getName()).info(I18N.getString("log.start"));
                break;
            case STOP:
                Logger.getLogger(this.getClass().getName()).info(I18N.getString("log.stop"));
                break;
            default:
                assert false;
        }
    }
    
    private void setApplicationUncaughtExceptionHandler() {
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            // Register a Default Uncaught Exception Handler for the application
            Thread.setDefaultUncaughtExceptionHandler(new SceneBuilderUncaughtExceptionHandler());
        }
    }
    
    private static class SceneBuilderUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            // Print the details of the exception in SceneBuilder log file
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "An exception was thrown:", e); //NOI18N
        }
    }
    
    
    private void performUseToolTheme(ToolTheme toolTheme) {
        this.toolTheme = toolTheme;
        
        final String toolStylesheet = getToolStylesheet();
        
        for (DocumentWindowController dwc : windowList) {
            dwc.setToolStylesheet(toolStylesheet);
        }
        preferencesWindowController.setToolStylesheet(toolStylesheet);
        aboutWindowController.setToolStylesheet(toolStylesheet);
    }
    
    
    private String getToolStylesheet() {
        final String result;
        
        switch(this.toolTheme) {
            
            default:
            case DEFAULT:
                result = EditorController.getBuiltinToolStylesheet();
                break;
                
            case DARK:
                result = getDarkToolStylesheet();
                break;
        }
        
        return result;
    }
}
