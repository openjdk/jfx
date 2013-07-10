/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import static javafx.concurrent.Worker.State.FAILED;
import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.Callable;

import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


public class LoadTest extends TestBase {
    
    private State getLoadState() {
        return submit(new Callable<State>() {
            public State call() {
                return getEngine().getLoadWorker().getState();
            }
        });
    }

    @Test public void testLoadGoodUrl() {
        final String FILE = "src/test/resources/html/ipsum.html";
        load(new File(FILE));
        WebEngine web = getEngine();

        assertTrue("Load task completed successfully", getLoadState() == SUCCEEDED);
        assertTrue("Location.endsWith(FILE)", web.getLocation().endsWith(FILE));
        assertEquals("Title", "Lorem Ipsum", web.getTitle());
        assertNotNull("Document should not be null", web.getDocument());
    }

    @Test public void testLoadBadUrl() {
        final String URL = "bad://url";
        load(URL);
        WebEngine web = getEngine();

        assertTrue("Load task failed", getLoadState() == FAILED);
        assertEquals("Location", URL, web.getLocation());
        assertNull("Title should be null", web.getTitle());
        assertNull("Document should be null", web.getDocument());
    }

    @Test public void testLoadHtmlContent() {
        final String TITLE = "In a Silent Way";
        loadContent("<html><head><title>" + TITLE + "</title></head></html>");
        WebEngine web = getEngine();

        assertTrue("Load task completed successfully", getLoadState() == SUCCEEDED);
        assertEquals("Location", "", web.getLocation());
        assertEquals("Title", TITLE, web.getTitle());
        assertNotNull("Document should not be null", web.getDocument());
    }

    @Test public void testLoadPlainContent() {
        final String TEXT =
                "<html><head><title>Hidden Really Well</title></head></html>";
        loadContent(TEXT, "text/plain");
        final WebEngine web = getEngine();

        assertTrue("Load task completed successfully", getLoadState() == SUCCEEDED);
        assertEquals("Location", "", web.getLocation());
        assertNull("Title should be null", web.getTitle());

        // DOM access should happen on FX thread
        submit(new Runnable() { public void run() {
            Document doc = web.getDocument();
            assertNotNull("Document should not be null", doc);
            Node el = // html -> body -> pre -> text
                    doc.getDocumentElement().getLastChild().getFirstChild().getFirstChild();
            String text = ((Text)el).getNodeValue();
            assertEquals("Plain text should not be interpreted as HTML",
                    TEXT, text);
        }});
    }

    @Test public void testLoadNull() {
        load((String) null);
        final WebEngine web = getEngine();

        assertTrue("Load task completed successfully", getLoadState() == SUCCEEDED);
        assertEquals("Location", "", web.getLocation());
        assertNull("Title should be null", web.getTitle());

        submit(new Runnable() { public void run() {
            Document doc = web.getDocument();
            assertNotNull("Document should not be null", doc);

            Element html = doc.getDocumentElement();
            assertNotNull("There should be an HTML element", html);
            assertEquals("HTML element should have tag HTML", "HTML", html.getTagName());

            NodeList htmlNodes = html.getChildNodes();
            assertNotNull("HTML element should have two children", htmlNodes);
            assertEquals("HTML element should have two children", 2, htmlNodes.getLength());

            Element head = (Element) htmlNodes.item(0);
            NodeList headNodes = head.getChildNodes();
            assertNotNull("There should be a HEAD element", head);
            assertEquals("HEAD element should have tag HEAD", "HEAD", head.getTagName());
            assertTrue("HEAD element should have no children",
                    headNodes == null || headNodes.getLength() == 0);

            Element body = (Element) htmlNodes.item(1);
            NodeList bodyNodes = body.getChildNodes();
            assertNotNull("There should be a BODY element", body);
            assertEquals("BODY element should have tag BODY", "BODY", body.getTagName());
            assertTrue("BODY element should have no children",
                    bodyNodes == null || bodyNodes.getLength() == 0);
        }});
    }

    @Test public void testLoadUrlWithEncodedSpaces() {
        final String URL = "http://localhost/test.php?param=a%20b%20c";
        load(URL);
        WebEngine web = getEngine();

        assertEquals("Unexpected location", URL, web.getLocation());
    }

    @Test public void testLoadUrlWithUnencodedSpaces() {
        final String URL = "http://localhost/test.php?param=a b c";
        load(URL);
        WebEngine web = getEngine();

        assertEquals("Unexpected location",
                URL.replace(" ", "%20"), web.getLocation());
    }

    @Test public void testLoadContentWithLocalScript() {
        WebEngine webEngine = getEngine();
        
        final StringBuilder result = new StringBuilder();
        webEngine.setOnAlert(new EventHandler<WebEvent<String>>() {
            @Override public void handle(WebEvent<String> event) {
                result.append("ALERT: ").append(event.getData());
            }
        });
        
        String scriptUrl =
                new File("src/test/resources/html/invoke-alert.js").toURI().toASCIIString();
        String html =
                "<html>\n" +
                "<head><script src=\"" + scriptUrl + "\"></script></head>\n" +
                "<body><script>invokeAlert('foo');</script></body>\n" +
                "</html>";
        loadContent(html);
        
        assertEquals("Unexpected result", "ALERT: foo", result.toString());
        assertEquals("Unexpected load state", SUCCEEDED, getLoadState());
        assertEquals("Unexpected location", "", webEngine.getLocation());
        assertNotNull("Document is null", webEngine.getDocument());
    }
}
