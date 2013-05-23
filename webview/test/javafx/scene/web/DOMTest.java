/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.*;
import org.w3c.dom.css.*;
import org.w3c.dom.events.*;
import org.w3c.dom.html.*;
import org.w3c.dom.stylesheets.*;
import org.w3c.dom.views.*;
import com.sun.webkit.dom.*;


/**
 * Tests for various aspects of DOM access.
 *
 * <p><strong>DOM should be accessed from FX thread only,
 * so please be sure to use submit(Callable).</strong>
 */
public class DOMTest extends TestBase {

    @Test public void testEmptyTextContent() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Element emptyP = doc.getElementById("empty-paragraph");
            String textContent = emptyP.getTextContent();
            assertEquals("Text content of an empty paragraph", "", textContent);
        }});
    }

    @Test public void testAppendChild() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p1 = doc.getElementById("p1");
            NodeList c1 = p1.getChildNodes();
            Node left1 = c1.item(2);
            int count1 = c1.getLength();

            Node p2 = doc.getElementById("p2");
            NodeList c2 = p2.getChildNodes();
            Node left2 = c2.item(0);
            Node n = c2.item(1);
            Node right2 = c2.item(2);
            int count2 = c2.getLength();

            // Some sanity/identity checks
            assertSame("Sibling expected", right2, n.getNextSibling());
            assertSame("Sibling expected", n, right2.getPreviousSibling());

            Node ret = p1.appendChild(n);
            assertSame("Sibling expected", left2, right2.getPreviousSibling());
            assertSame("Parent check", p2, right2.getParentNode());

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1);
            verifySiblings(n, left1, null);
            assertSame("Returned node", n, ret);
        }});
    }

    @Test public void testInsertBeforeEnd() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p1 = doc.getElementById("p1");
            NodeList c1 = p1.getChildNodes();
            Node left1 = c1.item(2);
            int count1 = c1.getLength();

            Node p2 = doc.getElementById("p2");
            NodeList c2 = p2.getChildNodes();
            Node left2 = c2.item(0);
            Node n = c2.item(1);
            Node right2 = c2.item(2);
            int count2 = c2.getLength();

            // Some sanity/identity checks
            assertSame("Sibling expected", right2, n.getNextSibling());
            assertSame("Sibling expected", n, right2.getPreviousSibling());

            try {
                p1.insertBefore(null, null);
                fail("DOMException expected but not thrown");
            } catch (DOMException ex) {
                // Expected.
            } catch (Throwable ex) {
                fail("DOMException expected but instead threw "+ex.getClass().getName());
            }

            Node ret = p1.insertBefore(n, null);
            assertSame("Sibling expected", left2, right2.getPreviousSibling());
            assertSame("Parent check", p2, right2.getParentNode());

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1);
            verifySiblings(n, left1, null);
            assertSame("Returned node", n, ret);
        }});
    }

    @Test public void testInsertBefore() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p1 = doc.getElementById("p1");
            NodeList c1 = p1.getChildNodes();
            Node left1 = c1.item(0);
            Node right1 = c1.item(1);
            int count1 = c1.getLength();

            Node p2 = doc.getElementById("p2");
            NodeList c2 = p2.getChildNodes();
            Node left2 = c2.item(0);
            Node n = c2.item(1);
            Node right2 = c2.item(2);
            int count2 = c2.getLength();

            Node ret = p1.insertBefore(n, right1);

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1);
            verifySiblings(n, left1, right1);
            assertEquals("Returned node", n, ret);
        }});
    }

    @Test public void testReplaceChild() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p1 = doc.getElementById("p1");
            NodeList c1 = p1.getChildNodes();
            Node left1 = c1.item(0);
            Node old = c1.item(1);
            Node right1 = c1.item(2);
            int count1 = c1.getLength();

            Node p2 = doc.getElementById("p2");
            NodeList c2 = p2.getChildNodes();
            Node left2 = c2.item(0);
            Node n = c2.item(1);
            Node right2 = c2.item(2);
            int count2 = c2.getLength();

            Node ret = p1.replaceChild(n, old);

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1 - 1);    // child count stays the same
            verifySiblings(n, left1, right1);
            verifyNodeRemoved(old);
            assertEquals("Returned node", old, ret);
        }});
    }

    @Test public void testRemoveChild() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p = doc.getElementById("p1");
            NodeList c = p.getChildNodes();
            Node left = c.item(0);
            Node n = c.item(1);
            Node right = c.item(2);
            int count = c.getLength();

            Node ret = p.removeChild(n);

            verifyChildRemoved(p, count, left, right);
            verifyNodeRemoved(n);
            assertEquals("Returned node", n, ret);
        }});
    }

    @Test public void testRemoveChildWithEventHandler() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Node p = doc.getElementById("p1");
            NodeList c = p.getChildNodes();
            Node left = c.item(0);
            final Node n = c.item(1);
            Node right = c.item(2);
            int count = c.getLength();
            final EventTarget[] evtTarget = new EventTarget[1];

            EventListener listener = new EventListener() {
                    public void handleEvent(Event evt) {
                        evtTarget[0] = evt.getTarget();
                    }
                };
            ((EventTarget) p).addEventListener("DOMNodeRemoved",
                                               listener, false);

            Node ret = p.removeChild(n);
            assertEquals("event target2", evtTarget[0], n);
            verifyChildRemoved(p, count, left, right);
            verifyNodeRemoved(n);
            assertEquals("Returned node", n, ret);
        }});
    }

    @Test public void testNodeTypes() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            Element p = doc.getElementById("showcase-paragraph");
            assertEquals("P element's node type", Node.ELEMENT_NODE, p.getNodeType());
            assertEquals("P element's tag name", "P", p.getTagName());

            NodeList children = p.getChildNodes();
            assertEquals("Paragraph child count", 3, children.getLength());
            Node text = children.item(0);
            assertEquals("Text node type", Node.TEXT_NODE, text.getNodeType());
            Node comment = children.item(1);
            assertEquals("Comment node type", Node.COMMENT_NODE, comment.getNodeType());
            Node element = children.item(2);
            assertEquals("SPAN element's node type", Node.ELEMENT_NODE, element.getNodeType());

            Element span = (Element) element;
            assertEquals("SPAN element's tag name", "SPAN", span.getTagName());
            assertTrue("SPAN has 'class' attribute", span.hasAttribute("class"));
            assertTrue("SPAN has 'CLASS' attribute", span.hasAttribute("CLASS"));
            assertEquals("SPAN attributes count", 1, span.getAttributes().getLength());

            Attr attr = span.getAttributeNode("class");
            assertEquals("Attr node type", Node.ATTRIBUTE_NODE, attr.getNodeType());
            children = span.getChildNodes();
            assertEquals("SPAN element child count", 1, children.getLength());
            text = children.item(0);
            assertEquals("SPAN text node type", Node.TEXT_NODE, text.getNodeType());
        }});
    }

    @Test public void testNodeTypification() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            NodeList inputsp = doc.getElementsByTagName("p");
            HTMLParagraphElement elp = (HTMLParagraphElement) inputsp.item(0);
            assertEquals("P element typification", "left", elp.getAlign());

            NodeList inputsi = doc.getElementsByTagName("img");
            HTMLImageElement eli = (HTMLImageElement) inputsi.item(0);
            assertEquals("Image element typification", "file:///C:/test.png", eli.getSrc());
        }});
    }

    @Test public void testEventListenerCascade() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            HTMLDocument htmlDoc = (HTMLDocument)doc;
            final HTMLBodyElement body = (HTMLBodyElement)htmlDoc.getBody();

            final EventListener listenerJS = ((HTMLBodyElementImpl)body).getOnclick();

            // typecast test
            UIEvent evKeyUp = (UIEvent)((DocumentEvent)htmlDoc).createEvent("KeyboardEvent");
            ((KeyboardEventImpl)evKeyUp).initKeyboardEvent(
                    "keyup"//String type
                    , true//boolean canBubble
                    , true//boolean cancelable
                    , ((DocumentView)htmlDoc).getDefaultView()//AbstractView view
                    , "K"//String keyIdentifier
                    , KeyboardEventImpl.KEY_LOCATION_STANDARD//int keyLocation
                    , false //boolean ctrlKey
                    , false //boolean altKey
                    , false // boolean shiftKey
                    , false //boolean metaKey
                    , false //boolean altGraphKey
            );
            WheelEventImpl evWheelUp = (WheelEventImpl)((DocumentEvent)htmlDoc).createEvent("WheelEvent");

            // dispatch test
            MouseEvent evClick = (MouseEvent)((DocumentEvent)htmlDoc).createEvent("MouseEvent");
            evClick.initMouseEvent(
                "click",
                true,
                true,
                ((DocumentView)htmlDoc).getDefaultView(),
                10,
                0, 0, 0, 0,
                true, true, true, true,
                (short)1, (EventTarget)body);

            //check start condition
            assertEquals("Wrong body initial state", "bodyClass", body.getClassName());

            //FIXME: ineffective - there is not ScriptExecutionContext
            listenerJS.handleEvent(evClick);
            //OK!
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("JS EventHandler does not work directly", "testClass", body.getClassName());

            EventListener listener1 = new EventListener() {
                @Override public void handleEvent(Event evt) {
                    EventTarget src = ((MouseEvent)evt).getTarget();
                    ((HTMLBodyElement)src).setClassName("newTestClass");
                }
            };
            ((EventTarget)body).addEventListener("click", listener1, true);
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("Java EventHandler does not work directly", "newTestClass", body.getClassName());

            EventListener listener2 = new EventListener() {
                @Override public void handleEvent(Event evt) {
                    //OK: stacked ScriptExecutionContext
                    listenerJS.handleEvent(evt);
                }
            };
            ((EventTarget)body).addEventListener("click", listener2, true);
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("JS EventHandler does not work from Java call", "testClass", body.getClassName());
        }});
    }

    @Test public void testDOMWindowAndStyleAccess() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            HTMLDocument htmlDoc = (HTMLDocument)doc;
            final HTMLBodyElement body = (HTMLBodyElement)htmlDoc.getBody();

            //JS [window] access
            DOMWindowImpl wnd =
                    (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
            wnd.resizeBy(1,1);

            //Style access
            CSSStyleDeclaration style = ((HTMLBodyElementImpl)body).getStyle();
            assertEquals("Style extraction", "blue", style.getPropertyValue("background-color"));
        }});
    }

    @Test public void testDOMCSS() {
        final Document doc = getDocumentFor("test/html/dom.html");
        submit(new Runnable() { public void run() {
            StyleSheetList shl = ((HTMLDocumentImpl)doc).getStyleSheets();
            for (int i = 0; i < shl.getLength(); ++i ) {
                StyleSheet sh = shl.item(i);
                String type = sh.getType();
                assertEquals("Style type", "text/css", type);
                String media = sh.getMedia().getMediaText();
                if (i == 0) {
                    assertEquals("Style media", "screen", media);
                }
                CSSRuleList rl = ((CSSStyleSheet)sh).getCssRules();
                for (int k = 0; k < rl.getLength(); ++k ) {
                    CSSRule r = rl.item(k);
                    switch (r.getType()) {
                    case CSSRule.MEDIA_RULE:
                        CSSRuleList mediaRl = ((CSSMediaRule)r).getCssRules();
                        break;
                    case CSSRule.IMPORT_RULE:
                        String url = ((CSSImportRule)r).getHref();
                        break;
                    }
                    String cssText = r.getCssText();
                }
            }
        }});
    }

    // helper methods

    private void verifyChildRemoved(Node parent,
            int oldChildrenCount, Node leftSibling, Node rightSibling) {
        assertSame("Children count",
                oldChildrenCount - 1, parent.getChildNodes().getLength());
        assertSame("Left sibling's next sibling",
                rightSibling, leftSibling.getNextSibling());
        assertSame("Right sibling's previous sibling",
                leftSibling, rightSibling.getPreviousSibling());
    }

    private void verifyChildAdded(Node n, Node parent, int oldChildrenCount) {
        assertEquals("Children count",
                oldChildrenCount + 1, parent.getChildNodes().getLength());
        assertEquals("Added node's parent",
                parent, n.getParentNode());
    }

    private void verifySiblings(Node n, Node leftSibling, Node rightSibling) {
        assertSame("Added node's previous sibling",
                leftSibling, n.getPreviousSibling());
        assertSame("Added node's next sibling",
                rightSibling, n.getNextSibling());

        if (leftSibling != null)
            assertSame("Previous sibling's next sibling",
                    n, leftSibling.getNextSibling());

        if (rightSibling != null)
            assertSame("Next sibling's previous sibling",
                    n, rightSibling.getPreviousSibling());
    }

    private void verifyNodeRemoved(Node n) {
        assertNull("Removed node's parent", n.getParentNode());
        assertNull("Removed node's previous sibling", n.getPreviousSibling());
        assertNull("Removed node's next sibling", n.getNextSibling());
    }
}
