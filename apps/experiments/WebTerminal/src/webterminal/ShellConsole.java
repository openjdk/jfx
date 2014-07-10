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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.*;
import javafx.stage.Stage;
import java.io.*;
import java.util.List;

/** Application to run a Process in a WebTerminal window.
 * The Process is run with standard input, output, and error streams
 * bound to an interactive WebTerminal window.
 * The WebTerminal window is bare, with no menubar or other "chrome".
 * (That should probably change, at least when running as an Application.)
 *
 * Usage:
 * <pre>
 * java webterminal.ShellConsole [options] [command]
 * </pre>
 * So far no {@code [options]} are supported.
 * The {@code [command]} is the arguments to the Process.
 * The default for [command] is "bash" "--noediting" "-i".
 */

public class ShellConsole extends Application
{
    WebTerminal replNode;

    Process process;

    Scene createScene() {
        replNode = new WebTerminal() {
                protected void enter (KeyEvent ke) {
                    String text = replNode.handleEnter(ke);
                    if (pin != null) {
                        synchronized (pin) {
                            try {
                                pin.write(text);
                                pin.write("\n");
                                pin.flush();
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
        VBox.setVgrow(replNode.webView, Priority.ALWAYS);

        VBox pane = replNode;
        Scene scene = new Scene(pane);
        // Make a bottom gray bar to free the resize corner on Mac
        //scene.setFill(Color.GRAY);
        return scene;
    }

    Writer pin;
    Reader pout;
    Reader perr;

    public static String[] defaultCommandWithArgs
        = {"bash", "--noediting", "-i" };

    @Override public void start(Stage stage) {
        try {
            List<String> args = getParameters().getRaw();
            int argsSize = args.size();
            String[] commandWithArgs = argsSize == 0 ? defaultCommandWithArgs
                : args.toArray(new String[argsSize]);
            ProcessBuilder pbuilder = new ProcessBuilder(commandWithArgs);
            pbuilder.redirectErrorStream(true);
            process = pbuilder.start();
            pin = new OutputStreamWriter(process.getOutputStream());
            pout = new InputStreamReader(process.getInputStream());
            perr = new InputStreamReader(process.getErrorStream());
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        final Scene scene = createScene();
        stage.setTitle("Jfx-Shell");

        stage.setScene(scene);
        stage.setWidth(900);
        stage.setHeight(700);
        stage.show();

        initialize0(); // ???
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private void copyThread(final Reader fromInferior, final WebWriter toPane) {
        Thread th = new Thread() {
                char[] buffer = new char[1024];
                public void run () {
                    for (;;) {
                        try {
                            int count = fromInferior.read(buffer);
                            if (count < 0)
                                break;
                            toPane.write(buffer, 0, count);
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            System.exit(-1);
                        }
                    }
                }
            }; 
        th.start();
    }

    WebWriter out_stream;
    WebWriter err_stream;
    public void initialize0 () {
        out_stream = new WebWriter(this.replNode, 'O');
        err_stream = new WebWriter(this.replNode, 'E');
        copyThread(pout, out_stream);
        copyThread(perr, err_stream);
    }
}
