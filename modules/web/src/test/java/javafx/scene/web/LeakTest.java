/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.webkit.WebPage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LeakTest extends TestBase {

    @Ignore // RT-26710: javafx.scene.web.LeakTest hangs
    @Test public void testOleg() throws InterruptedException{
        final String URL = new File("src/test/resources/html/guimark2-vector.html").toURI().toASCIIString();
        final int CYCLE_COUNT = 16;
        final int CYCLE_LENGTH = 5;
        final CountDownLatch latch = new CountDownLatch(CYCLE_COUNT);

        Timeline time = new Timeline();
        time.setCycleCount(CYCLE_LENGTH * CYCLE_COUNT);
        time.getKeyFrames().add(new KeyFrame(Duration.millis(1000), new EventHandler<ActionEvent>() {
            int counter = -1;
            @Override public void handle(final ActionEvent e) {
                ++counter;
                if (counter == 0) {
                    WebEngine engine = new WebEngine();
                    engine.load(URL);
                } else if (counter == CYCLE_LENGTH - 1) {
                    counter = -1;
                    latch.countDown();
                }
            }
        }));
        time.play();
        latch.await();
    }

    @Ignore // RT-26710: javafx.scene.web.LeakTest hangs
    @Test public void testGarbageCollectability() throws InterruptedException {
        final BlockingQueue<WeakReference<WebPage>> webPageRefQueue =
                new LinkedBlockingQueue<WeakReference<WebPage>>();
        submit(() -> {
            WebView webView = new WebView();
            WeakReference<WebView> webViewRef =
                    new WeakReference<WebView>(webView);
            WeakReference<WebEngine> webEngineRef =
                    new WeakReference<WebEngine>(webView.getEngine());
            webPageRefQueue.add(
                    new WeakReference<WebPage>(webView.getEngine().getPage()));

            webView = null;
            System.gc();
            assertNull("WebView has not been GCed", webViewRef.get());
            assertNull("WebEngine has not been GCed", webEngineRef.get());
        });
        
        WeakReference<WebPage> webPageRef = webPageRefQueue.take();
        long endTime = System.currentTimeMillis() + 5000;
        while (true) {
            System.gc();
            if (webPageRef.get() == null) {
                break;
            }
            if (System.currentTimeMillis() > endTime) {
                fail("WebPage has not been GCed");
            }
            Thread.sleep(100);
        }
    }
}
