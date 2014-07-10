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

import webterminal.*;
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

/** A control that wraps an application runing in a terminal emulator.
 */

public class PtyConsole extends WebTerminal {
    Writer pin;
    Reader pout;
    PTY pty;

    @Override public void processInputCharacters(String text) {
        try {
            pin.write(text);
            pin.flush();
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    @Override protected void enter (KeyEvent ke) {
        String text = handleEnter(ke);
        if (pin != null) {
            synchronized (pin) {
                try {
                    pin.write(text);
                    pin.write("\r\n");
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

    static String[] defaultArgs = { "/bin/bash" };

    void start(String[] commandArgs) {
        String[] childArgs =
            commandArgs.length == 0 ? defaultArgs : commandArgs;
        pty = new PTY(childArgs, "jfxterm");
        try {
            pin = new OutputStreamWriter(pty.toChildInput);
            pout = new InputStreamReader(pty.fromChildOutput, "UTF-8");
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        setLineEditing(false);
    }

    WebWriter out_stream;
    WebWriter err_stream;
    public void initialize0 () {
        out_stream = new WebWriter(this, 'O');
        copyThread(pout, out_stream);
    }

    void copyThread(final Reader fromInferior, final WebWriter toPane) {
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

    @Override public void setRowsColumns(int rows, int columns) {
        pty.setWindowSize(rows, columns, 0, 0);
        super.setRowsColumns(rows, columns);
    }
}
