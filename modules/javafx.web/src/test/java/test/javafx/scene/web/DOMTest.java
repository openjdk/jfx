/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javafx.scene.web.WebEngine;

import org.junit.jupiter.api.Test;
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

    @Test public void testGetSetId() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            NodeList ee = doc.getElementsByTagName("p");

            int numProcessed = 0;
            for (int i = 0 ; i < ee.getLength() ; i++) {
                Node n = ee.item(i);
                String s = ((ElementImpl)n).getId();
                String newId = "new" + s;
                ((ElementImpl)n).setId(newId);
                assertEquals(newId, ((ElementImpl)n).getId(), "New element id");
                numProcessed++;
            }

            assertTrue(numProcessed > 0, "Number of processed Elements is equal to 0");
        });
    }

    @Test public void testEmptyTextContent() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            Element emptyP = doc.getElementById("empty-paragraph");
            String textContent = emptyP.getTextContent();
            assertEquals("", textContent, "Text content of an empty paragraph");
        });
    }

    @Test public void testAppendChild() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
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
            assertSame(right2, n.getNextSibling(), "Sibling expected");
            assertSame(n, right2.getPreviousSibling(), "Sibling expected");

            Node ret = p1.appendChild(n);
            assertSame(left2, right2.getPreviousSibling(), "Sibling expected");
            assertSame(p2, right2.getParentNode(), "Parent check");

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1);
            verifySiblings(n, left1, null);
            assertSame(n, ret, "Returned node");
        });
    }

    @Test public void testInsertBeforeEnd() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
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
            assertSame(right2, n.getNextSibling(), "Sibling expected");
            assertSame(n, right2.getPreviousSibling(), "Sibling expected");

            try {
                p1.insertBefore(null, null);
                fail("DOMException expected but not thrown");
            } catch (DOMException ex) {
                // Expected.
            } catch (Throwable ex) {
                fail("DOMException expected but instead threw "+ex.getClass().getName());
            }

            Node ret = p1.insertBefore(n, null);
            assertSame(left2, right2.getPreviousSibling(), "Sibling expected");
            assertSame(p2, right2.getParentNode(), "Parent check");

            verifyChildRemoved(p2, count2, left2, right2);
            verifyChildAdded(n, p1, count1);
            verifySiblings(n, left1, null);
            assertSame(n, ret, "Returned node");
        });
    }

    @Test public void testInsertBefore() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
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
            assertEquals(n, ret, "Returned node");
        });
    }

    @Test public void testReplaceChild() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
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
            assertEquals(old, ret, "Returned node");
        });
    }

    @Test public void testRemoveChild() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            Node p = doc.getElementById("p1");
            NodeList c = p.getChildNodes();
            Node left = c.item(0);
            Node n = c.item(1);
            Node right = c.item(2);
            int count = c.getLength();

            Node ret = p.removeChild(n);

            verifyChildRemoved(p, count, left, right);
            verifyNodeRemoved(n);
            assertEquals(n, ret, "Returned node");
        });
    }

    @Test public void testRemoveChildWithEventHandler() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            Node p = doc.getElementById("p1");
            NodeList c = p.getChildNodes();
            Node left = c.item(0);
            final Node n = c.item(1);
            Node right = c.item(2);
            int count = c.getLength();
            final EventTarget[] evtTarget = new EventTarget[1];

            EventListener listener = new EventListener() {
                @Override
                public void handleEvent(Event evt) {
                    evtTarget[0] = evt.getTarget();
                }
            };
            ((EventTarget) p).addEventListener("DOMNodeRemoved",
                    listener, false);

            Node ret = p.removeChild(n);
            assertEquals(evtTarget[0], n, "event target2");
            verifyChildRemoved(p, count, left, right);
            verifyNodeRemoved(n);
            assertEquals(n, ret, "Returned node");
        });
    }

    @Test public void testNodeTypes() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            Element p = doc.getElementById("showcase-paragraph");
            assertEquals(Node.ELEMENT_NODE, p.getNodeType(), "P element's node type");
            assertEquals("P", p.getTagName(), "P element's tag name");

            NodeList children = p.getChildNodes();
            assertEquals(3, children.getLength(), "Paragraph child count");
            Node text = children.item(0);
            assertEquals(Node.TEXT_NODE, text.getNodeType(), "Text node type");
            Node comment = children.item(1);
            assertEquals(Node.COMMENT_NODE, comment.getNodeType(), "Comment node type");
            Node element = children.item(2);
            assertEquals(Node.ELEMENT_NODE, element.getNodeType(), "SPAN element's node type");

            Element span = (Element) element;
            assertEquals("SPAN", span.getTagName(), "SPAN element's tag name");
            assertTrue(span.hasAttribute("class"), "SPAN has 'class' attribute");
            assertTrue(span.hasAttribute("CLASS"), "SPAN has 'CLASS' attribute");
            assertEquals(1, span.getAttributes().getLength(), "SPAN attributes count");

            Attr attr = span.getAttributeNode("class");
            assertEquals(Node.ATTRIBUTE_NODE, attr.getNodeType(), "Attr node type");
            children = span.getChildNodes();
            assertEquals(1, children.getLength(), "SPAN element child count");
            text = children.item(0);
            assertEquals(Node.TEXT_NODE, text.getNodeType(), "SPAN text node type");
        });
    }

    @Test public void testNodeTypification() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            NodeList inputsp = doc.getElementsByTagName("p");
            HTMLParagraphElement elp = (HTMLParagraphElement) inputsp.item(0);
            assertEquals("left", elp.getAlign(), "P element typification");

            NodeList inputsi = doc.getElementsByTagName("img");
            HTMLImageElement eli = (HTMLImageElement) inputsi.item(0);
            assertEquals("file:///C:/test.png", eli.getSrc(), "Image element typification");
        });
    }

    @Test public void testEventListenerCascade() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
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
            assertEquals("bodyClass", body.getClassName(), "Wrong body initial state");

            //FIXME: ineffective - there is not ScriptExecutionContext
            listenerJS.handleEvent(evClick);
            //OK!
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("testClass", body.getClassName(), "JS EventHandler does not work directly");

            EventListener listener1 = evt -> {
                EventTarget src = ((MouseEvent) evt).getTarget();
                ((HTMLBodyElement) src).setClassName("newTestClass");
            };
            ((EventTarget)body).addEventListener("click", listener1, false);
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("newTestClass", body.getClassName(), "Java EventHandler does not work directly");

            EventListener listener2 = evt -> {
                //OK: stacked ScriptExecutionContext
                listenerJS.handleEvent(evt);
            };
            ((EventTarget)body).addEventListener("click", listener2, false);
            ((EventTarget)body).dispatchEvent(evClick);
            assertEquals("testClass", body.getClassName(), "JS EventHandler does not work from Java call");
        });
    }

    @Test public void testDOMWindowAndStyleAccess() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            HTMLDocument htmlDoc = (HTMLDocument)doc;
            final HTMLBodyElement body = (HTMLBodyElement)htmlDoc.getBody();

            //JS [window] access
            DOMWindowImpl wnd =
                    (DOMWindowImpl)((DocumentView)htmlDoc).getDefaultView();
            wnd.resizeBy(1,1);

            //Style access
            CSSStyleDeclaration style = ((HTMLBodyElementImpl)body).getStyle();
            assertEquals("blue", style.getPropertyValue("background-color"), "Style extraction");
        });
    }

    @Test public void testDOMCSS() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            StyleSheetList shl = ((HTMLDocumentImpl)doc).getStyleSheets();
            for (int i = 0; i < shl.getLength(); ++i ) {
                StyleSheet sh = shl.item(i);
                String type = sh.getType();
                assertEquals("text/css", type, "Style type");
                String media = sh.getMedia().getMediaText();
                if (i == 0) {
                    assertEquals("screen", media, "Style media");
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
        });
    }

    // JDK-8179321
    // Still we are supporting DOM3 interface, need to relook once we move to
    // DOM4 spec.
    @Test public void testDocumentURIForDOM3Compliance() {
        // According to DOM3 spec, page loaded without base url(i.e as String)
        // must have "document.documentURI" value as null.
        loadContent("test");
        submit(() -> {
            final WebEngine webEngine = getEngine();
            final Document document = webEngine.getDocument();
            assertNotNull(document);
            assertNull(document.getDocumentURI());
        });
    }

    // JDK-8233747
    @Test public void testCreateAttribute() {
        final Document doc = getDocumentFor("src/test/resources/test/html/dom.html");
        submit(() -> {
            try {
                //invalid attribute
                Attr attr = doc.createAttribute(":/test");
                fail("DOMException expected but not thrown");
            } catch (DOMException ex) {
                // Expected.
            } catch (Throwable ex) {
                fail("DOMException expected but instead threw " + ex.getClass().getName());
            }

            String attributeName = "test";
            Attr attr = doc.createAttribute(attributeName);
            assertEquals(attributeName, attr.getName(), "Created attribute");
        });
    }

    // helper methods

    private void verifyChildRemoved(Node parent,
                                    int oldChildrenCount, Node leftSibling, Node rightSibling) {
        assertSame(oldChildrenCount - 1, parent.getChildNodes().getLength(), "Children count");
        assertSame(rightSibling, leftSibling.getNextSibling(), "Left sibling's next sibling");
        assertSame(leftSibling, rightSibling.getPreviousSibling(), "Right sibling's previous sibling");
    }

    private void verifyChildAdded(Node n, Node parent, int oldChildrenCount) {
        assertEquals(oldChildrenCount + 1, parent.getChildNodes().getLength(), "Children count");
        assertEquals(parent, n.getParentNode(), "Added node's parent");
    }

    private void verifySiblings(Node n, Node leftSibling, Node rightSibling) {
        assertSame(leftSibling, n.getPreviousSibling(), "Added node's previous sibling");
        assertSame(rightSibling, n.getNextSibling(), "Added node's next sibling");

        if (leftSibling != null)
            assertSame(n, leftSibling.getNextSibling(), "Previous sibling's next sibling");

        if (rightSibling != null)
            assertSame(n, rightSibling.getPreviousSibling(), "Next sibling's previous sibling");
    }

    private void verifyNodeRemoved(Node n) {
        assertNull(n.getParentNode(), "Removed node's parent");
        assertNull(n.getPreviousSibling(), "Removed node's previous sibling");
        assertNull(n.getNextSibling(), "Removed node's next sibling");
    }
}
