/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates.
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

package ptyconsole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javax.net.ssl.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.*;
import javafx.stage.Stage;
import java.io.*;

/** A simple application that just contains a WebConsole.
 * FIXME - needs a menubar with Edit menu and other settings.
 */

public class App extends Application
{
    PtyConsole console;
    static String[] commandArgs;

    Scene createScene() {
        console = new PtyConsole();
        //VBox.setVgrow(console.webView, Priority.ALWAYS);

        //FIXME web.addChangeListener(WebEngine.DOCUMENT, this);

        VBox pane = console;
        Scene scene = new Scene(pane);
        return scene;
    }

    @Override public void start(Stage stage) {
        final Scene scene = createScene();
        console.start(commandArgs);
        console.setRowsColumns(24, 80);
        stage.setTitle("Jfx-Terminal");

        stage.setScene(scene);
        //stage.sizeToScene();
        stage.setWidth(700);
        stage.setHeight(500);
        stage.show();
        console.initialize0(); // ???
   }

    public static void main(String[] args) throws Throwable {
        commandArgs = args;
        Application.launch(args);
    }

}
