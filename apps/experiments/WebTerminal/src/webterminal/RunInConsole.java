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

package webterminal;
import java.io.*;
import java.lang.reflect.Method;
import javafx.application.Application;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyEvent;

/** Run a console-based Java application in a WebTerminal.
 * Usage: java webterminal.RunInConsole [options] class.name class-args...
 * This re-binds System.in, System.out, and System.err to a WebTerminal
 * instance, and then invokes the main method of class.name, passing
 * it the class-args, roughly as if you'd called:
 * java class.name class-args...
 * (Right now no 'options' are supported, but some may be added.)
 */

public class RunInConsole extends Application {

    WebTerminal console;
    PipedOutputStream inputSink;
    Writer inputWriter;

    Scene createScene() {
        console = new WebTerminal() {
                protected void enter (KeyEvent ke) {
                    String text = console.handleEnter(ke);
                    if (inputWriter != null) {
                        synchronized (inputWriter) {
                            try {
                                inputWriter.write(text);
                                inputWriter.write("\r\n");
                                inputWriter.flush();
                                //pin.notifyAll();
                            }
                            catch (Throwable ex) {
                                ex.printStackTrace();
                                System.exit(-1);
                            }
                        }
                    }
                }
            };
        //VBox.setVgrow(console.webView, Priority.ALWAYS);

        VBox pane = console;
        Scene scene = new Scene(pane);
        // Make a bottom gray bar to free the resize corner on Mac
        //scene.setFill(Color.GRAY);
        return scene;
    }

    String[] restArgs;
    Method mainMethod;
    String className;

    void getMainMethod() throws Throwable {
        java.util.List<String> args = getParameters().getRaw();
        int nargs = args.size();
        className = args.get(0);
        restArgs = new String[nargs-1];
        args.subList(1, nargs).toArray(restArgs);
        Class clas = Class.forName(className, false, RunInConsole.class.getClassLoader());
        mainMethod = clas.getDeclaredMethod("main", String[].class);
    }

    @Override public void start(Stage stage) {
        final Scene scene = createScene();
        inputSink = new PipedOutputStream();
        try {
            getMainMethod();
            inputWriter = new OutputStreamWriter(inputSink);
            System.setIn(new PipedInputStream(inputSink));
        }
        catch (java.lang.ClassNotFoundException ex) {
            System.err.println("can't find class "+className);
            System.exit(-1);
        }
        catch (Throwable ex) {
            WTDebug.println("caught "+ex);
            System.exit(-1);
        }
        //WebTerminal.origErr = System.err;

        stage.setTitle("Jfx-Shell");

        stage.setScene(scene);
        //stage.sizeToScene();
        stage.setWidth(900);
        stage.setHeight(700);
        stage.show();

        OutputStream wout = new WebOutputStream(new WebWriter(console, 'O'));
        OutputStream werr = new WebOutputStream(new WebWriter(console, 'E'));
        System.setOut(new PrintStream(new BufferedOutputStream(wout, 128), true));
        System.setErr(new PrintStream(new BufferedOutputStream(werr, 128), true));
        (new Thread() {
                public void run() {
                    try {
                        mainMethod.invoke(null, new Object[] { restArgs });
                    } catch (Throwable ex) {
                        WTDebug.println("caught while executing main "+ex);
                    }
                }}).start();
    }

    public static void main(String[] args) {
        WTDebug.init();
        Application.launch(args);
    }

}
