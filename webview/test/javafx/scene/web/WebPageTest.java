/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import com.sun.webkit.WebPage;
import java.util.concurrent.Callable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WebPageTest extends TestBase {

    final static String PLAIN = "<html><head></head><body></body></html>";
    final static String HTML  = "<html><head></head><body><p>Test</p></body></html>";
    final static String XML   = "<?xml version='1.0'?><root></root>";

    @Test public void testGetHtml() throws Exception {
        WebPage page = getEngine().getPage();

        loadContent(HTML);
        assertEquals("HTML document", HTML, getHtml(page));
        
        // With XML document, getHtml() should return null
        loadContent(XML, "application/xml");
        assertNull("XML document", getHtml(page));
        
        loadContent("");
        assertEquals("Empty document", PLAIN, getHtml(page));
        
        loadContent("", "text/plain");
        assertEquals("Empty text/plain document", PLAIN, getHtml(page));
    }
    
    private String getHtml(final WebPage page) throws Exception {
        return submit(new Callable<String>() { public String call() {
            return page.getHtml(page.getMainFrame());
        }});
    }

    @Test public void testGetHtmlIllegalFrameId() {
        WebPage page = getEngine().getPage();
        assertEquals(null, page.getHtml(1));
    }    
}
