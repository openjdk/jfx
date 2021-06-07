/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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

package ensemble;

import ensemble.samples.animation.interpolator.InterpolatorApp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.stage.Stage;

public class EmbeddedApplication {
    private static Stage TEMP_STAGE = new Stage() {

    };

    public static Node createApplication(String className) {
            System.out.println("EmbeddedApplication.createApplication()");
        Node node = null;
        try {
//            Class appClass = Thread.currentThread().getContextClassLoader().loadClass(className);
//            Class appClass = EmbeddedApplication.class.getClassLoader().loadClass(className);
            Class appClass = InterpolatorApp.class;
            System.out.println("appClass = " + appClass);
            Application app = (Application)appClass.getDeclaredConstructor().newInstance();
            System.out.println("app = " + app);
            app.init();
            app.start(TEMP_STAGE);
            node = TEMP_STAGE.getScene().getRoot();
            System.out.println("node = " + node);
            TEMP_STAGE.setScene(null);
        } catch (Exception ex) {
            Logger.getLogger(EmbeddedApplication.class.getName()).log(Level.SEVERE, "Error loading application class", ex);
        }
        return node;
    }
}
