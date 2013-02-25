/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.sun.javafx.PlatformUtil;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class RT17588Test extends Application {

    /**
     * @param args the command line arguments
     */
    @BeforeClass public static void prepare() {
        String[] args = null;
        
        /* 
         * This test hangs on Mac
         */
        Assume.assumeTrue(PlatformUtil.isWindows());

        launch(args);
    }
    
    private Stage stage, newStage;
    
    @Test public void testModalFileChooser() {
        try {
            stage.show();
            final Stage newStage = new Stage();
            newStage.initModality(Modality.NONE);
            newStage.initOwner(stage);
            newStage.show();
            
            final FileChooser fc = new FileChooser();        
            
            Thread t = new Thread(new Runnable() {
                public void run(){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RT17588Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(RT17588Test.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            stage.hide();
                            newStage.hide();
                        }
                    });
                }
            });
            t.start();
            
            fc.showOpenDialog(newStage);
            
        } catch (Throwable th) {
            th.printStackTrace();
            fail(th.getMessage());
        }
    }
                 
    @Override
    public void start(final Stage st) throws Exception {
        stage = st;
        testModalFileChooser();
    }
    
}
