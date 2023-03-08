/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.text;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.application.Application;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

public class ArabicWrappingTest extends Application {

    static PrintStream systemErr = System.err;
    static SystemErrFilter systemErrFilter;

    static class SystemErrFilter extends PrintStream {
        private boolean foundException = false;
        private String exceptionMsg;

        public SystemErrFilter(OutputStream out) {
            super(out);
        }

        @Override
        public synchronized void print(String s) {
            System.out.flush();
            if (s.indexOf("Exception") >= 0) {
                foundException = true;
                exceptionMsg = s;
            }
            super.print(s);
        }

        boolean checkException() {
            return foundException;
        }

        String getExceptionString() {
            return exceptionMsg;
        }
    }

    @BeforeClass
    public static void initFX() {
        systemErrFilter = new SystemErrFilter(System.err);
        System.setErr(systemErrFilter);
        new Thread(() -> {
            Application.launch(ArabicWrappingTest.class);
        }).start();
    }

    @AfterClass
    public static void exitTest() {
        Platform.exit();
    }

    static volatile boolean testDone = false;
    static volatile boolean testPassed;

    /*
     * Junit will create an extra instance of the Application class
     * but there's no default constructor for the class that does
     * anything, and this test method just checks static variables
     * which are updated by the instance explicitly created in
     * the @BeforeClass annotated method.
     * In other words, this is fine here.
     */
    @Test(timeout=120000)
    public void testWrapping() {

       while (!ArabicWrappingTest.testDone) {
           try {
               Thread.sleep(2000);
           } catch (Exception e) {
           }
       }

       assertTrue(ArabicWrappingTest.testPassed);
    }

    public static void main(String[] args) {
        initFX();
        try {
            wrappingTest.testWrapping();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            exitTest();
        }
    }

    static final int MAX_WW = 200;
    static final int MIN_WW =   5;

    static String text = "Arabic \u0643\u064e\u062a\u064e\u0628\u064e\u200e kataba.";
    static ArabicWrappingTest wrappingTest;

    @Override
    public void start(Stage stage) throws Exception {
        wrappingTest = this;
        fontNames = Font.getFontNames().toArray(new String[0]);
        maxFonts = fontNames.length > MAXFONTS ? MAXFONTS : fontNames.length;
        String[] tmpFonts = new String[maxFonts+4];
        tmpFonts[0] = "System Regular";
        tmpFonts[1] = "SansSerif Regular";
        tmpFonts[2] = "Serif Regular";
        tmpFonts[3] = "Monospaced Regular";
        System.arraycopy(fontNames, 0, tmpFonts, 4, maxFonts);
        fontNames = tmpFonts;
        maxFonts = fontNames.length;
        textNode = new Text(text);
        font = new Font(fontNames[fontIndex++], 12);
        System.out.println(font); System.out.flush();
        textNode.setFont(font);
        textNode.setWrappingWidth(MAX_WW);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(textNode);
        Scene scene = new Scene(hbox);
        stage.setScene(scene);
        stage.setTitle("Test bidi text wrapping");
        stage.setWidth(MAX_WW+50);
        stage.setHeight(600);
        stage.show();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(wrappingTest::updateWrapWidth);
            }}, 1000, 10);
    }

    static String[] fontNames;
    static int fontIndex = 0;
    static final int MAXFONTS = 12; // else test takes too long
    static int maxFonts;
    Timer timer;
    Text textNode;
    boolean shrink = true;
    Font font;

    boolean nextFont() {
        if (fontIndex >= maxFonts) {
            return false;
        } else {
            font = new Font(fontNames[fontIndex++], 12);
            textNode.setFont(font);
            System.out.println("font="+font+" fonts to go = " + (maxFonts-fontIndex));
            System.out.flush();
            return true;
        }
    }

    void updateWrapWidth() {

        if ((systemErrFilter != null) && systemErrFilter.checkException()) {
            timer.cancel();
            System.setErr(systemErr);
            System.err.println("Exception with font " + font);
            System.err.print(systemErrFilter.getExceptionString());
            ArabicWrappingTest.testPassed = false;
            ArabicWrappingTest.testDone = true;
            return;
        }

        double cww = textNode.getWrappingWidth();
        double delta = (shrink) ? -1 : 1;
        if (cww < MIN_WW) {
           shrink = false;
        }
        if (cww > MAX_WW) {
           if (!nextFont()) {
               timer.cancel();
               System.setErr(systemErr);
               ArabicWrappingTest.testPassed = true;
               ArabicWrappingTest.testDone = true;
               return;
           } else {
               shrink = true;
               cww = MAX_WW;
               delta = -1;
           }
        }
       textNode.setWrappingWidth(cww+delta);
    }
}
