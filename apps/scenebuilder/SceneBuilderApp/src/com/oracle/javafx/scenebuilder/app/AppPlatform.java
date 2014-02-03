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

import com.oracle.javafx.scenebuilder.app.util.MessageBox;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import static com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.IS_LINUX;
import static com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.IS_MAC;
import static com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.IS_WINDOWS;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;

/**
 *
 */
public class AppPlatform {
    
    private static String applicationDataFolder;
    private static String userLibraryFolder;
    private static String messageBoxFolder;
    private static MessageBox<MessageBoxMessage> messageBox;
    
    public static synchronized String getApplicationDataFolder() {
        
        if (applicationDataFolder == null) {
            final String appName = "Scene Builder"; //NOI18N
            
            if (IS_WINDOWS) {
                applicationDataFolder 
                        = System.getenv("APPDATA") + "\\" + appName; //NOI18N
            } else if (IS_MAC) {
                applicationDataFolder 
                        = System.getProperty("user.home") //NOI18N
                        + "/Library/Application Support/" //NOI18N
                        + appName;
            } else if (IS_LINUX) {
                applicationDataFolder
                        = System.getProperty("user.home") + "/.scenebuilder"; //NOI18N
            }
        }
        
        assert applicationDataFolder != null;
        
        return applicationDataFolder;
    }
    
    
    public static synchronized String getUserLibraryFolder() {
        
        if (userLibraryFolder == null) {
            userLibraryFolder = getApplicationDataFolder() + "/Library"; //NOI18N
        }
        
        return userLibraryFolder;
    }
    
    public static boolean requestStart(
            AppNotificationHandler notificationHandler, Application.Parameters parameters)  
    throws IOException {
        if (IS_MAC) {
            return requestStartMac(notificationHandler, parameters);
        } else {
            if (EditorPlatform.isAssertionEnabled()) {
                // Development mode : we do not delegate to the existing instance
                notificationHandler.handleLaunch(parameters.getUnnamed());
                return true;
            } else {
                return requestStartGeneric(notificationHandler, parameters);
            }
        }
    }
    
    public interface AppNotificationHandler {
        public void handleLaunch(List<String> files);
        public void handleOpenFilesAction(List<String> files);
        public void handleMessageBoxFailure(Exception x);
        public void handleQuitAction();
    }
    
    
    /*
     * Private (requestStartGeneric)
     */
    
    private static synchronized boolean requestStartGeneric(
            AppNotificationHandler notificationHandler, Application.Parameters parameters) 
    throws IOException {
        assert notificationHandler != null;
        assert parameters != null;
        assert messageBox == null;
        
        try {
            Files.createDirectories(Paths.get(getMessageBoxFolder()));
        } catch(FileAlreadyExistsException x) {
            // Fine
        }
        
        final boolean result;
        messageBox = new MessageBox<>(getMessageBoxFolder(), MessageBoxMessage.class, 1000 /* ms */);
        if (messageBox.grab(new MessageBoxDelegate(notificationHandler))) {
            notificationHandler.handleLaunch(parameters.getUnnamed());
            result = true;
        } else {
            result = false;
            final MessageBoxMessage unamedParameters 
                    = new MessageBoxMessage(parameters.getUnnamed());
            try {
                messageBox.sendMessage(unamedParameters);
            } catch(InterruptedException x) {
                throw new IOException(x);
            }
        }
        
        return result;
    }
    
    private static String getMessageBoxFolder() {
        if (messageBoxFolder == null) {
            messageBoxFolder = getApplicationDataFolder() + "/MB"; //NOI18N
        }
        
        return messageBoxFolder;
    }
    
    private static class MessageBoxMessage extends ArrayList<String> {
        static final long serialVersionUID = 10;
        public MessageBoxMessage(List<String> strings) {
            super(strings);
        };
    };
    
    private static class MessageBoxDelegate implements MessageBox.Delegate<MessageBoxMessage> {

        private final AppNotificationHandler eventHandler;
        
        public MessageBoxDelegate(AppNotificationHandler eventHandler) {
            assert eventHandler != null;
            this.eventHandler = eventHandler;
        }
        
        /*
         * MessageBox.Delegate
         */
        
        @Override
        public void messageBoxDidGetMessage(MessageBoxMessage message) {
            assert Platform.isFxApplicationThread() == false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    eventHandler.handleOpenFilesAction(message);
                }
            });
        }

        @Override
        public void messageBoxDidCatchException(Exception x) {
            assert Platform.isFxApplicationThread() == false;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    eventHandler.handleMessageBoxFailure(x);
                }
            });
        }
        
    } 
    
    
    
    /*
     * Private (requestStartMac)
     */
    
    private static boolean requestStartMac(
            AppNotificationHandler notificationHandler, Application.Parameters parameters) {
        
        Platform.setImplicitExit(false);
        notificationHandler.handleLaunch(Collections.emptyList());
        Deprecation.setPlatformEventHandler(new MacEventHandler(notificationHandler,
                Deprecation.getPlatformEventHandler()));
        
        return true;
    }
    
    private static class MacEventHandler extends com.sun.glass.ui.Application.EventHandler {
        
        private final AppNotificationHandler notificationHandler;
        private final com.sun.glass.ui.Application.EventHandler oldEventHandler;
        private int openFilesCount;
        
        public MacEventHandler(AppNotificationHandler notificationHandler,
                com.sun.glass.ui.Application.EventHandler oldEventHandler) {
            assert notificationHandler != null;
            this.notificationHandler = notificationHandler;
            this.oldEventHandler = oldEventHandler;
        }
        
        /*
         * com.sun.glass.ui.Application.AppNotificationHandler
         */
        @Override
        public void handleDidFinishLaunchingAction(com.sun.glass.ui.Application app, long time) {
            if (oldEventHandler != null) {
                oldEventHandler.handleDidFinishLaunchingAction(app, time);
            }
        }

        @Override
        public void handleDidBecomeActiveAction(com.sun.glass.ui.Application app, long time) {
            if (oldEventHandler != null) {
                oldEventHandler.handleDidBecomeActiveAction(app, time);
            }
        }

        @Override
        public void handleOpenFilesAction(com.sun.glass.ui.Application app, long time, final String[] files) {
            if (oldEventHandler != null) {
                oldEventHandler.handleOpenFilesAction(app, time, files);
            }
            
            /*
             * When SB is started from NB or test environment on Mac OS, this 
             * method is called a first time with dummy parameter like this:
             * files[0] == "com.oracle.javafx.scenebuilder.app.SceneBuilderApp". //NOI18N
             * We ignore this call here.
             */
            final boolean openRejected;
            if (startingFromTestBed) {
                openRejected = true;
            } else if (openFilesCount++ == 0) {
                openRejected = (files.length == 1) 
                        && files[0].equals(SceneBuilderApp.class.getName()); //NOI18N
            } else {
                openRejected = false;
            }
            
            if (openRejected == false) {
                notificationHandler.handleOpenFilesAction(Arrays.asList(files));
            }
        }

        @Override
        public void handleQuitAction(com.sun.glass.ui.Application app, long time) {
            if (oldEventHandler != null) {
                oldEventHandler.handleQuitAction(app, time);
            }
            notificationHandler.handleQuitAction();
        }  
    } 
    
    
    /*
     * Some code to help starting Scene Builder application from SQE java code.
     * This is relevant on Mac only.
     */
    
    private static boolean startingFromTestBed;
    
    public static void setStartingFromTestBed(boolean macWorkaroundEnabled) {
        AppPlatform.startingFromTestBed = macWorkaroundEnabled;
    }
}
