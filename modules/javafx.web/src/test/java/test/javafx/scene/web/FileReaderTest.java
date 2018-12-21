/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import com.sun.javafx.webkit.UIClientImplShim;
import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngineShim;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import netscape.javascript.JSObject;
import org.junit.Before;
import org.junit.Test;

public class FileReaderTest extends TestBase {
    private final WebPage page = WebEngineShim.getPage(getEngine());
    private String[] fileList = { new File("src/test/resources/test/html/HelloWorld.txt").getAbsolutePath() };
    private CountDownLatch latch;
    private State getLoadState() {
        return submit(() -> getEngine().getLoadWorker().getState());
    }

    private String getScriptString(String readAPI, String slice, boolean abort) {
        String scriptContent = String.format("<script type='text/javascript'>" +
                                    "var result;" +
                                    "window.addEventListener('click', (e) => {" +
                                        "document.getElementById('file').click();" +
                                    "});" +
                                    "function readFile()" +
                                    "{" +
                                        "file = event.target.files[0];" +
                                        "var reader = new FileReader();" +
                                        "reader.onloadstart = () => {" +
                                        "%s" +
                                        "};" +
                                        "reader.onload = () => {" +
                                            "result = reader.result;" +
                                            "latch.countDown();" +
                                        "};" +
                                        "reader.onerror = () => {" +
                                            "result = 'failed due to error';" +
                                            "latch.countDown();" +
                                        "};" +
                                        "reader." + readAPI + "(file" + slice + ");" +
                                    "}" +
                               "</script>" +
                               "<body> <input type='file' id='file' onchange='readFile()'/></body>", (abort ? "reader.abort();" : ""));
        return scriptContent;
    }

    @Before
    public void before() {
        latch = new CountDownLatch(1);
        UIClientImplShim.test_setChooseFiles(fileList);
    }

    private void loadFileReaderTestScript(String testScript) {
        loadContent(testScript);
        testLatch(latch);
    }

    private void testLatch(CountDownLatch latch) {
        assertTrue("Page load is not finished yet", getLoadState() == SUCCEEDED);
        assertNotNull("Document should not be null", getEngine().getDocument());
        submit(() -> {
            final JSObject window = (JSObject) getEngine().executeScript("window");
            assertNotNull(window);
            window.setMember("latch", latch);
            // we send a dummy mouse click event at (0,0) to simulate click on file chooser button.
            WebPageShim.click(page, 0, 0);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test public void testReadAsTextWithoutSlice() {
        loadFileReaderTestScript(getScriptString("readAsText", "", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "Hello World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTextWithSlice() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice()", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "Hello World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithValidStartAndEnd() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(3, 7)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "lo W", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithEndAsFileLength() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(3, file.length)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "lo World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithOnlyStartAsFileLength() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(file.length)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "Hello World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithStartAsNegetiveValue() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(-7, file.length)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "o World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithStartAsFileLength() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(file.length, 3)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "Hel", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithEndAsNegetiveValue() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(file.length, -3)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "Hello Wo", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithEndAsBeyondFileLength() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(file.length, -100)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithBeginAsValidValue() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(6)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "World", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithOnlyStartAsNegetiveValue() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(-3)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "rld", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testReadAsTexWithSliceWithStartAndEndAsNegetiveValues() {
        loadFileReaderTestScript(getScriptString("readAsText", ".slice(-3, -7)", false));
        submit(() -> {
            assertEquals("Unexpected file content received", "", getEngine().executeScript("window.result"));
        });
    }

    @Test public void testreadAsBinaryString() throws FileNotFoundException, IOException {
        String binaryFile[] = { new File("src/test/resources/test/html/BinaryFile.dat").getAbsolutePath() };
        UIClientImplShim.test_setChooseFiles(binaryFile);
        loadFileReaderTestScript(getScriptString("readAsBinaryString", "", false));
        FileInputStream in = new FileInputStream(binaryFile[0]);
        final byte[] expectedBinaryData = in.readAllBytes();
        assertNotNull("BinaryFile content should not be null", expectedBinaryData);
        submit(() -> {
            try {
                final String obj = (String) getEngine().executeScript("window.result");
                // setting encoding scheme to ISO-8859-1 for binary data as webkit uses the same.
                final byte[] binBytes = obj.getBytes("ISO-8859-1");
                assertNotNull("BinaryFile content read should not be null", binBytes);
                assertArrayEquals("Unexpected file content received", expectedBinaryData, binBytes);
            } catch (UnsupportedEncodingException ex) {
                throw new AssertionError(ex);
            }
        });
    }

    @Test public void testreadAsArrayBuffer() throws FileNotFoundException, IOException {
        loadFileReaderTestScript(getScriptString("readAsArrayBuffer", "", false));
        try (FileInputStream in = new FileInputStream(fileList[0])) {
            final byte[] expectedArrayBuffer = in.readAllBytes();
            submit(() -> {
                final JSObject obj = (JSObject) getEngine().executeScript("new Uint8Array(window.result)");
                assertEquals(String.format("%s length must be equal in both Java & JavaScript", fileList),
                                       expectedArrayBuffer.length, obj.getMember("length"));
                for (int i = 0; i < expectedArrayBuffer.length; i++) {
                    assertEquals("Unexpected file content received", expectedArrayBuffer[i], ((Number)(obj.getSlot(i))).byteValue());
                }
            });
        } catch (IOException ex){
            throw new AssertionError(ex);
        }
    }

    @Test public void testreadAsDataURL() throws FileNotFoundException, IOException {
        loadFileReaderTestScript(getScriptString("readAsDataURL", "", false));
        try (FileInputStream in = new FileInputStream(fileList[0])) {
            final byte[] expectedArrayBuffer = in.readAllBytes();
            submit(() -> {
                try {
                    String encodedData = (String) getEngine().executeScript("window.result");
                    assertNotNull("window.result must have base64 encoded data", encodedData);
                    assertEquals("Base64 EncodedData is not same as window.result",
                                   "data:text/plain;base64,SGVsbG8gV29ybGQ=", encodedData);
                    encodedData = encodedData.split(",")[1];
                    assertNotNull(encodedData);
                    final byte[] decodedData = Base64.getDecoder().decode(encodedData);
                    assertNotNull("Base64 decoded data must be valid", decodedData);
                    assertEquals("Base64 DecodedData is not same as File Content",
                        new String(expectedArrayBuffer, "utf-8"), new String(decodedData, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError(e);
                }
            });
        } catch (IOException ex){
            throw new AssertionError(ex);
        }
    }

    @Test public void testAbort() {
        loadFileReaderTestScript(getScriptString("readAsText", "", true));
        submit(() -> {
            assertEquals("Unexpected file content received", "failed due to error",
                          getEngine().executeScript("window.result"));
        });
    }
}
