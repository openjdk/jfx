/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.io.File;
import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.w3c.dom.Document;

public class MiscellaneousTest extends TestBase {

    @Test public void testNoEffectOnFollowRedirects() {
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
        load("test/html/ipsum.html");
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
    }

    @Test public void testRT22458() throws Exception {
        final WebEngine webEngine = createWebEngine();
        Platform.runLater(new Runnable() {
            @Override public void run() {
                webEngine.load(format("file://%d.ajax.googleapis.com/ajax",
                                      new Random().nextInt()));
            }
        });
        Thread.sleep(200);
        long startTime = System.currentTimeMillis();
        DummyClass.dummyField++;
        long time = System.currentTimeMillis() - startTime;
        if (time > 2000) {
            fail(format("DummyClass took %f seconds to load", time / 1000.));
        }
    }

    private static final class DummyClass {
        private static int dummyField;
    }

    @Test public void testRT30835() throws Exception {
        class Record {
            private final Document document;
            private final String location;
            public Record(Document document, String location) {
                this.document = document;
                this.location = location;
            }
        }
        final ArrayList<Record> records = new ArrayList<Record>();
        ChangeListener<State> listener = new ChangeListener<State>() {
            public void changed(ObservableValue<? extends State> ov,
                                State oldValue, State newValue)
            {
                if (newValue == State.SUCCEEDED) {
                    records.add(new Record(
                            getEngine().getDocument(),
                            getEngine().getLocation()));
                }
            }
        };
        submit(new Runnable() { public void run() {
            getEngine().getLoadWorker().stateProperty().addListener(listener);
        }});
        String location = new File("src/test/resources/html/RT30835.html")
                .toURI().toASCIIString().replaceAll("^file:/", "file:///");
        load(location);
        assertEquals(1, records.size());
        assertNotNull(records.get(0).document);
        assertEquals(location, records.get(0).location);
    }

    private WebEngine createWebEngine() {
        return submit(new Callable<WebEngine>() {
            public WebEngine call() {
                return new WebEngine();
            }
        });
    }
}
