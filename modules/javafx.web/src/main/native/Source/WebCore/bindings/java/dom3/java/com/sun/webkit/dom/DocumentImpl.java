/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.events.DocumentEvent;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLHeadElement;
import org.w3c.dom.html.HTMLScriptElement;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.stylesheets.StyleSheetList;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.w3c.dom.views.AbstractView;
import org.w3c.dom.views.DocumentView;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathExpression;
import org.w3c.dom.xpath.XPathNSResolver;
import org.w3c.dom.xpath.XPathResult;

public class DocumentImpl extends NodeImpl implements Document, XPathEvaluator, DocumentView, DocumentEvent {
    DocumentImpl(long peer) {
        super(peer);
    }

    static Document getImpl(long peer) {
        return (Document)create(peer);
    }

    native static boolean isHTMLDocumentImpl(long peer);

    @Override public Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type, Object result) throws DOMException {
        return evaluate(expression, contextNode, resolver, type, (XPathResult)result);
    }


// Attributes
    public DocumentType getDoctype() {
        return DocumentTypeImpl.getImpl(getDoctypeImpl(getPeer()));
    }
    native static long getDoctypeImpl(long peer);

    public DOMImplementation getImplementation() {
        return DOMImplementationImpl.getImpl(getImplementationImpl(getPeer()));
    }
    native static long getImplementationImpl(long peer);

    public Element getDocumentElement() {
        return ElementImpl.getImpl(getDocumentElementImpl(getPeer()));
    }
    native static long getDocumentElementImpl(long peer);

    public String getInputEncoding() {
        return getInputEncodingImpl(getPeer());
    }
    native static String getInputEncodingImpl(long peer);

    public String getXmlEncoding() {
        return getXmlEncodingImpl(getPeer());
    }
    native static String getXmlEncodingImpl(long peer);

    public String getXmlVersion() {
        return getXmlVersionImpl(getPeer());
    }
    native static String getXmlVersionImpl(long peer);

    public void setXmlVersion(String value) throws DOMException {
        setXmlVersionImpl(getPeer(), value);
    }
    native static void setXmlVersionImpl(long peer, String value);

    public boolean getXmlStandalone() {
        return getXmlStandaloneImpl(getPeer());
    }
    native static boolean getXmlStandaloneImpl(long peer);

    public void setXmlStandalone(boolean value) throws DOMException {
        setXmlStandaloneImpl(getPeer(), value);
    }
    native static void setXmlStandaloneImpl(long peer, boolean value);

    public String getDocumentURI() {
        return getDocumentURIImpl(getPeer());
    }
    native static String getDocumentURIImpl(long peer);

    public void setDocumentURI(String value) {
        setDocumentURIImpl(getPeer(), value);
    }
    native static void setDocumentURIImpl(long peer, String value);

    public AbstractView getDefaultView() {
        return DOMWindowImpl.getImpl(getDefaultViewImpl(getPeer()));
    }
    native static long getDefaultViewImpl(long peer);

    public StyleSheetList getStyleSheets() {
        return StyleSheetListImpl.getImpl(getStyleSheetsImpl(getPeer()));
    }
    native static long getStyleSheetsImpl(long peer);

    public String getContentType() {
        return getContentTypeImpl(getPeer());
    }
    native static String getContentTypeImpl(long peer);

    public String getTitle() {
        return getTitleImpl(getPeer());
    }
    native static String getTitleImpl(long peer);

    public void setTitle(String value) {
        setTitleImpl(getPeer(), value);
    }
    native static void setTitleImpl(long peer, String value);

    public String getReferrer() {
        return getReferrerImpl(getPeer());
    }
    native static String getReferrerImpl(long peer);

    public String getDomain() {
        return getDomainImpl(getPeer());
    }
    native static String getDomainImpl(long peer);

    public String getURL() {
        return getURLImpl(getPeer());
    }
    native static String getURLImpl(long peer);

    public String getCookie() throws DOMException {
        return getCookieImpl(getPeer());
    }
    native static String getCookieImpl(long peer);

    public void setCookie(String value) throws DOMException {
        setCookieImpl(getPeer(), value);
    }
    native static void setCookieImpl(long peer, String value);

    public HTMLElement getBody() {
        return HTMLElementImpl.getImpl(getBodyImpl(getPeer()));
    }
    native static long getBodyImpl(long peer);

    public void setBody(HTMLElement value) throws DOMException {
        setBodyImpl(getPeer(), HTMLElementImpl.getPeer(value));
    }
    native static void setBodyImpl(long peer, long value);

    public HTMLHeadElement getHead() {
        return HTMLHeadElementImpl.getImpl(getHeadImpl(getPeer()));
    }
    native static long getHeadImpl(long peer);

    public HTMLCollection getImages() {
        return HTMLCollectionImpl.getImpl(getImagesImpl(getPeer()));
    }
    native static long getImagesImpl(long peer);

    public HTMLCollection getApplets() {
        return HTMLCollectionImpl.getImpl(getAppletsImpl(getPeer()));
    }
    native static long getAppletsImpl(long peer);

    public HTMLCollection getLinks() {
        return HTMLCollectionImpl.getImpl(getLinksImpl(getPeer()));
    }
    native static long getLinksImpl(long peer);

    public HTMLCollection getForms() {
        return HTMLCollectionImpl.getImpl(getFormsImpl(getPeer()));
    }
    native static long getFormsImpl(long peer);

    public HTMLCollection getAnchors() {
        return HTMLCollectionImpl.getImpl(getAnchorsImpl(getPeer()));
    }
    native static long getAnchorsImpl(long peer);

    public String getLastModified() {
        return getLastModifiedImpl(getPeer());
    }
    native static String getLastModifiedImpl(long peer);

    public String getCharset() {
        return getCharsetImpl(getPeer());
    }
    native static String getCharsetImpl(long peer);

    public String getDefaultCharset() {
        return getDefaultCharsetImpl(getPeer());
    }
    native static String getDefaultCharsetImpl(long peer);

    public String getReadyState() {
        return getReadyStateImpl(getPeer());
    }
    native static String getReadyStateImpl(long peer);

    public String getCharacterSet() {
        return getCharacterSetImpl(getPeer());
    }
    native static String getCharacterSetImpl(long peer);

    public String getPreferredStylesheetSet() {
        return getPreferredStylesheetSetImpl(getPeer());
    }
    native static String getPreferredStylesheetSetImpl(long peer);

    public String getSelectedStylesheetSet() {
        return getSelectedStylesheetSetImpl(getPeer());
    }
    native static String getSelectedStylesheetSetImpl(long peer);

    public void setSelectedStylesheetSet(String value) {
        setSelectedStylesheetSetImpl(getPeer(), value);
    }
    native static void setSelectedStylesheetSetImpl(long peer, String value);

    public Element getActiveElement() {
        return ElementImpl.getImpl(getActiveElementImpl(getPeer()));
    }
    native static long getActiveElementImpl(long peer);

    public String getCompatMode() {
        return getCompatModeImpl(getPeer());
    }
    native static String getCompatModeImpl(long peer);

    public boolean getWebkitIsFullScreen() {
        return getWebkitIsFullScreenImpl(getPeer());
    }
    native static boolean getWebkitIsFullScreenImpl(long peer);

    public boolean getWebkitFullScreenKeyboardInputAllowed() {
        return getWebkitFullScreenKeyboardInputAllowedImpl(getPeer());
    }
    native static boolean getWebkitFullScreenKeyboardInputAllowedImpl(long peer);

    public Element getWebkitCurrentFullScreenElement() {
        return ElementImpl.getImpl(getWebkitCurrentFullScreenElementImpl(getPeer()));
    }
    native static long getWebkitCurrentFullScreenElementImpl(long peer);

    public boolean getWebkitFullscreenEnabled() {
        return getWebkitFullscreenEnabledImpl(getPeer());
    }
    native static boolean getWebkitFullscreenEnabledImpl(long peer);

    public Element getWebkitFullscreenElement() {
        return ElementImpl.getImpl(getWebkitFullscreenElementImpl(getPeer()));
    }
    native static long getWebkitFullscreenElementImpl(long peer);

    public String getVisibilityState() {
        return getVisibilityStateImpl(getPeer());
    }
    native static String getVisibilityStateImpl(long peer);

    public boolean getHidden() {
        return getHiddenImpl(getPeer());
    }
    native static boolean getHiddenImpl(long peer);

    public HTMLScriptElement getCurrentScript() {
        return HTMLScriptElementImpl.getImpl(getCurrentScriptImpl(getPeer()));
    }
    native static long getCurrentScriptImpl(long peer);

    public String getOrigin() {
        return getOriginImpl(getPeer());
    }
    native static String getOriginImpl(long peer);

    public Element getScrollingElement() {
        return ElementImpl.getImpl(getScrollingElementImpl(getPeer()));
    }
    native static long getScrollingElementImpl(long peer);

    public EventListener getOnbeforecopy() {
        return EventListenerImpl.getImpl(getOnbeforecopyImpl(getPeer()));
    }
    native static long getOnbeforecopyImpl(long peer);

    public void setOnbeforecopy(EventListener value) {
        setOnbeforecopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforecopyImpl(long peer, long value);

    public EventListener getOnbeforecut() {
        return EventListenerImpl.getImpl(getOnbeforecutImpl(getPeer()));
    }
    native static long getOnbeforecutImpl(long peer);

    public void setOnbeforecut(EventListener value) {
        setOnbeforecutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforecutImpl(long peer, long value);

    public EventListener getOnbeforepaste() {
        return EventListenerImpl.getImpl(getOnbeforepasteImpl(getPeer()));
    }
    native static long getOnbeforepasteImpl(long peer);

    public void setOnbeforepaste(EventListener value) {
        setOnbeforepasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforepasteImpl(long peer, long value);

    public EventListener getOncopy() {
        return EventListenerImpl.getImpl(getOncopyImpl(getPeer()));
    }
    native static long getOncopyImpl(long peer);

    public void setOncopy(EventListener value) {
        setOncopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncopyImpl(long peer, long value);

    public EventListener getOncut() {
        return EventListenerImpl.getImpl(getOncutImpl(getPeer()));
    }
    native static long getOncutImpl(long peer);

    public void setOncut(EventListener value) {
        setOncutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncutImpl(long peer, long value);

    public EventListener getOnpaste() {
        return EventListenerImpl.getImpl(getOnpasteImpl(getPeer()));
    }
    native static long getOnpasteImpl(long peer);

    public void setOnpaste(EventListener value) {
        setOnpasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpasteImpl(long peer, long value);

    public EventListener getOnselectstart() {
        return EventListenerImpl.getImpl(getOnselectstartImpl(getPeer()));
    }
    native static long getOnselectstartImpl(long peer);

    public void setOnselectstart(EventListener value) {
        setOnselectstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnselectstartImpl(long peer, long value);

    public EventListener getOnselectionchange() {
        return EventListenerImpl.getImpl(getOnselectionchangeImpl(getPeer()));
    }
    native static long getOnselectionchangeImpl(long peer);

    public void setOnselectionchange(EventListener value) {
        setOnselectionchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnselectionchangeImpl(long peer, long value);

    public EventListener getOnreadystatechange() {
        return EventListenerImpl.getImpl(getOnreadystatechangeImpl(getPeer()));
    }
    native static long getOnreadystatechangeImpl(long peer);

    public void setOnreadystatechange(EventListener value) {
        setOnreadystatechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnreadystatechangeImpl(long peer, long value);

    public EventListener getOnabort() {
        return EventListenerImpl.getImpl(getOnabortImpl(getPeer()));
    }
    native static long getOnabortImpl(long peer);

    public void setOnabort(EventListener value) {
        setOnabortImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnabortImpl(long peer, long value);

    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    native static long getOnblurImpl(long peer);

    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnblurImpl(long peer, long value);

    public EventListener getOncanplay() {
        return EventListenerImpl.getImpl(getOncanplayImpl(getPeer()));
    }
    native static long getOncanplayImpl(long peer);

    public void setOncanplay(EventListener value) {
        setOncanplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncanplayImpl(long peer, long value);

    public EventListener getOncanplaythrough() {
        return EventListenerImpl.getImpl(getOncanplaythroughImpl(getPeer()));
    }
    native static long getOncanplaythroughImpl(long peer);

    public void setOncanplaythrough(EventListener value) {
        setOncanplaythroughImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncanplaythroughImpl(long peer, long value);

    public EventListener getOnchange() {
        return EventListenerImpl.getImpl(getOnchangeImpl(getPeer()));
    }
    native static long getOnchangeImpl(long peer);

    public void setOnchange(EventListener value) {
        setOnchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnchangeImpl(long peer, long value);

    public EventListener getOnclick() {
        return EventListenerImpl.getImpl(getOnclickImpl(getPeer()));
    }
    native static long getOnclickImpl(long peer);

    public void setOnclick(EventListener value) {
        setOnclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnclickImpl(long peer, long value);

    public EventListener getOncontextmenu() {
        return EventListenerImpl.getImpl(getOncontextmenuImpl(getPeer()));
    }
    native static long getOncontextmenuImpl(long peer);

    public void setOncontextmenu(EventListener value) {
        setOncontextmenuImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncontextmenuImpl(long peer, long value);

    public EventListener getOndblclick() {
        return EventListenerImpl.getImpl(getOndblclickImpl(getPeer()));
    }
    native static long getOndblclickImpl(long peer);

    public void setOndblclick(EventListener value) {
        setOndblclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndblclickImpl(long peer, long value);

    public EventListener getOndrag() {
        return EventListenerImpl.getImpl(getOndragImpl(getPeer()));
    }
    native static long getOndragImpl(long peer);

    public void setOndrag(EventListener value) {
        setOndragImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragImpl(long peer, long value);

    public EventListener getOndragend() {
        return EventListenerImpl.getImpl(getOndragendImpl(getPeer()));
    }
    native static long getOndragendImpl(long peer);

    public void setOndragend(EventListener value) {
        setOndragendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragendImpl(long peer, long value);

    public EventListener getOndragenter() {
        return EventListenerImpl.getImpl(getOndragenterImpl(getPeer()));
    }
    native static long getOndragenterImpl(long peer);

    public void setOndragenter(EventListener value) {
        setOndragenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragenterImpl(long peer, long value);

    public EventListener getOndragleave() {
        return EventListenerImpl.getImpl(getOndragleaveImpl(getPeer()));
    }
    native static long getOndragleaveImpl(long peer);

    public void setOndragleave(EventListener value) {
        setOndragleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragleaveImpl(long peer, long value);

    public EventListener getOndragover() {
        return EventListenerImpl.getImpl(getOndragoverImpl(getPeer()));
    }
    native static long getOndragoverImpl(long peer);

    public void setOndragover(EventListener value) {
        setOndragoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragoverImpl(long peer, long value);

    public EventListener getOndragstart() {
        return EventListenerImpl.getImpl(getOndragstartImpl(getPeer()));
    }
    native static long getOndragstartImpl(long peer);

    public void setOndragstart(EventListener value) {
        setOndragstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragstartImpl(long peer, long value);

    public EventListener getOndrop() {
        return EventListenerImpl.getImpl(getOndropImpl(getPeer()));
    }
    native static long getOndropImpl(long peer);

    public void setOndrop(EventListener value) {
        setOndropImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndropImpl(long peer, long value);

    public EventListener getOndurationchange() {
        return EventListenerImpl.getImpl(getOndurationchangeImpl(getPeer()));
    }
    native static long getOndurationchangeImpl(long peer);

    public void setOndurationchange(EventListener value) {
        setOndurationchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndurationchangeImpl(long peer, long value);

    public EventListener getOnemptied() {
        return EventListenerImpl.getImpl(getOnemptiedImpl(getPeer()));
    }
    native static long getOnemptiedImpl(long peer);

    public void setOnemptied(EventListener value) {
        setOnemptiedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnemptiedImpl(long peer, long value);

    public EventListener getOnended() {
        return EventListenerImpl.getImpl(getOnendedImpl(getPeer()));
    }
    native static long getOnendedImpl(long peer);

    public void setOnended(EventListener value) {
        setOnendedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnendedImpl(long peer, long value);

    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    native static long getOnerrorImpl(long peer);

    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnerrorImpl(long peer, long value);

    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    native static long getOnfocusImpl(long peer);

    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusImpl(long peer, long value);

    public EventListener getOninput() {
        return EventListenerImpl.getImpl(getOninputImpl(getPeer()));
    }
    native static long getOninputImpl(long peer);

    public void setOninput(EventListener value) {
        setOninputImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOninputImpl(long peer, long value);

    public EventListener getOninvalid() {
        return EventListenerImpl.getImpl(getOninvalidImpl(getPeer()));
    }
    native static long getOninvalidImpl(long peer);

    public void setOninvalid(EventListener value) {
        setOninvalidImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOninvalidImpl(long peer, long value);

    public EventListener getOnkeydown() {
        return EventListenerImpl.getImpl(getOnkeydownImpl(getPeer()));
    }
    native static long getOnkeydownImpl(long peer);

    public void setOnkeydown(EventListener value) {
        setOnkeydownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeydownImpl(long peer, long value);

    public EventListener getOnkeypress() {
        return EventListenerImpl.getImpl(getOnkeypressImpl(getPeer()));
    }
    native static long getOnkeypressImpl(long peer);

    public void setOnkeypress(EventListener value) {
        setOnkeypressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeypressImpl(long peer, long value);

    public EventListener getOnkeyup() {
        return EventListenerImpl.getImpl(getOnkeyupImpl(getPeer()));
    }
    native static long getOnkeyupImpl(long peer);

    public void setOnkeyup(EventListener value) {
        setOnkeyupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeyupImpl(long peer, long value);

    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    native static long getOnloadImpl(long peer);

    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadImpl(long peer, long value);

    public EventListener getOnloadeddata() {
        return EventListenerImpl.getImpl(getOnloadeddataImpl(getPeer()));
    }
    native static long getOnloadeddataImpl(long peer);

    public void setOnloadeddata(EventListener value) {
        setOnloadeddataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadeddataImpl(long peer, long value);

    public EventListener getOnloadedmetadata() {
        return EventListenerImpl.getImpl(getOnloadedmetadataImpl(getPeer()));
    }
    native static long getOnloadedmetadataImpl(long peer);

    public void setOnloadedmetadata(EventListener value) {
        setOnloadedmetadataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadedmetadataImpl(long peer, long value);

    public EventListener getOnloadstart() {
        return EventListenerImpl.getImpl(getOnloadstartImpl(getPeer()));
    }
    native static long getOnloadstartImpl(long peer);

    public void setOnloadstart(EventListener value) {
        setOnloadstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadstartImpl(long peer, long value);

    public EventListener getOnmousedown() {
        return EventListenerImpl.getImpl(getOnmousedownImpl(getPeer()));
    }
    native static long getOnmousedownImpl(long peer);

    public void setOnmousedown(EventListener value) {
        setOnmousedownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousedownImpl(long peer, long value);

    public EventListener getOnmouseenter() {
        return EventListenerImpl.getImpl(getOnmouseenterImpl(getPeer()));
    }
    native static long getOnmouseenterImpl(long peer);

    public void setOnmouseenter(EventListener value) {
        setOnmouseenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseenterImpl(long peer, long value);

    public EventListener getOnmouseleave() {
        return EventListenerImpl.getImpl(getOnmouseleaveImpl(getPeer()));
    }
    native static long getOnmouseleaveImpl(long peer);

    public void setOnmouseleave(EventListener value) {
        setOnmouseleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseleaveImpl(long peer, long value);

    public EventListener getOnmousemove() {
        return EventListenerImpl.getImpl(getOnmousemoveImpl(getPeer()));
    }
    native static long getOnmousemoveImpl(long peer);

    public void setOnmousemove(EventListener value) {
        setOnmousemoveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousemoveImpl(long peer, long value);

    public EventListener getOnmouseout() {
        return EventListenerImpl.getImpl(getOnmouseoutImpl(getPeer()));
    }
    native static long getOnmouseoutImpl(long peer);

    public void setOnmouseout(EventListener value) {
        setOnmouseoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseoutImpl(long peer, long value);

    public EventListener getOnmouseover() {
        return EventListenerImpl.getImpl(getOnmouseoverImpl(getPeer()));
    }
    native static long getOnmouseoverImpl(long peer);

    public void setOnmouseover(EventListener value) {
        setOnmouseoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseoverImpl(long peer, long value);

    public EventListener getOnmouseup() {
        return EventListenerImpl.getImpl(getOnmouseupImpl(getPeer()));
    }
    native static long getOnmouseupImpl(long peer);

    public void setOnmouseup(EventListener value) {
        setOnmouseupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseupImpl(long peer, long value);

    public EventListener getOnmousewheel() {
        return EventListenerImpl.getImpl(getOnmousewheelImpl(getPeer()));
    }
    native static long getOnmousewheelImpl(long peer);

    public void setOnmousewheel(EventListener value) {
        setOnmousewheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousewheelImpl(long peer, long value);

    public EventListener getOnpause() {
        return EventListenerImpl.getImpl(getOnpauseImpl(getPeer()));
    }
    native static long getOnpauseImpl(long peer);

    public void setOnpause(EventListener value) {
        setOnpauseImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpauseImpl(long peer, long value);

    public EventListener getOnplay() {
        return EventListenerImpl.getImpl(getOnplayImpl(getPeer()));
    }
    native static long getOnplayImpl(long peer);

    public void setOnplay(EventListener value) {
        setOnplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnplayImpl(long peer, long value);

    public EventListener getOnplaying() {
        return EventListenerImpl.getImpl(getOnplayingImpl(getPeer()));
    }
    native static long getOnplayingImpl(long peer);

    public void setOnplaying(EventListener value) {
        setOnplayingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnplayingImpl(long peer, long value);

    public EventListener getOnprogress() {
        return EventListenerImpl.getImpl(getOnprogressImpl(getPeer()));
    }
    native static long getOnprogressImpl(long peer);

    public void setOnprogress(EventListener value) {
        setOnprogressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnprogressImpl(long peer, long value);

    public EventListener getOnratechange() {
        return EventListenerImpl.getImpl(getOnratechangeImpl(getPeer()));
    }
    native static long getOnratechangeImpl(long peer);

    public void setOnratechange(EventListener value) {
        setOnratechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnratechangeImpl(long peer, long value);

    public EventListener getOnreset() {
        return EventListenerImpl.getImpl(getOnresetImpl(getPeer()));
    }
    native static long getOnresetImpl(long peer);

    public void setOnreset(EventListener value) {
        setOnresetImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnresetImpl(long peer, long value);

    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    native static long getOnresizeImpl(long peer);

    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnresizeImpl(long peer, long value);

    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    native static long getOnscrollImpl(long peer);

    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnscrollImpl(long peer, long value);

    public EventListener getOnseeked() {
        return EventListenerImpl.getImpl(getOnseekedImpl(getPeer()));
    }
    native static long getOnseekedImpl(long peer);

    public void setOnseeked(EventListener value) {
        setOnseekedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnseekedImpl(long peer, long value);

    public EventListener getOnseeking() {
        return EventListenerImpl.getImpl(getOnseekingImpl(getPeer()));
    }
    native static long getOnseekingImpl(long peer);

    public void setOnseeking(EventListener value) {
        setOnseekingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnseekingImpl(long peer, long value);

    public EventListener getOnselect() {
        return EventListenerImpl.getImpl(getOnselectImpl(getPeer()));
    }
    native static long getOnselectImpl(long peer);

    public void setOnselect(EventListener value) {
        setOnselectImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnselectImpl(long peer, long value);

    public EventListener getOnstalled() {
        return EventListenerImpl.getImpl(getOnstalledImpl(getPeer()));
    }
    native static long getOnstalledImpl(long peer);

    public void setOnstalled(EventListener value) {
        setOnstalledImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnstalledImpl(long peer, long value);

    public EventListener getOnsubmit() {
        return EventListenerImpl.getImpl(getOnsubmitImpl(getPeer()));
    }
    native static long getOnsubmitImpl(long peer);

    public void setOnsubmit(EventListener value) {
        setOnsubmitImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsubmitImpl(long peer, long value);

    public EventListener getOnsuspend() {
        return EventListenerImpl.getImpl(getOnsuspendImpl(getPeer()));
    }
    native static long getOnsuspendImpl(long peer);

    public void setOnsuspend(EventListener value) {
        setOnsuspendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsuspendImpl(long peer, long value);

    public EventListener getOntimeupdate() {
        return EventListenerImpl.getImpl(getOntimeupdateImpl(getPeer()));
    }
    native static long getOntimeupdateImpl(long peer);

    public void setOntimeupdate(EventListener value) {
        setOntimeupdateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOntimeupdateImpl(long peer, long value);

    public EventListener getOnvolumechange() {
        return EventListenerImpl.getImpl(getOnvolumechangeImpl(getPeer()));
    }
    native static long getOnvolumechangeImpl(long peer);

    public void setOnvolumechange(EventListener value) {
        setOnvolumechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnvolumechangeImpl(long peer, long value);

    public EventListener getOnwaiting() {
        return EventListenerImpl.getImpl(getOnwaitingImpl(getPeer()));
    }
    native static long getOnwaitingImpl(long peer);

    public void setOnwaiting(EventListener value) {
        setOnwaitingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwaitingImpl(long peer, long value);

    public EventListener getOnsearch() {
        return EventListenerImpl.getImpl(getOnsearchImpl(getPeer()));
    }
    native static long getOnsearchImpl(long peer);

    public void setOnsearch(EventListener value) {
        setOnsearchImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsearchImpl(long peer, long value);

    public EventListener getOnwheel() {
        return EventListenerImpl.getImpl(getOnwheelImpl(getPeer()));
    }
    native static long getOnwheelImpl(long peer);

    public void setOnwheel(EventListener value) {
        setOnwheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwheelImpl(long peer, long value);

    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    native static long getChildrenImpl(long peer);

    public Element getFirstElementChild() {
        return ElementImpl.getImpl(getFirstElementChildImpl(getPeer()));
    }
    native static long getFirstElementChildImpl(long peer);

    public Element getLastElementChild() {
        return ElementImpl.getImpl(getLastElementChildImpl(getPeer()));
    }
    native static long getLastElementChildImpl(long peer);

    public int getChildElementCount() {
        return getChildElementCountImpl(getPeer());
    }
    native static int getChildElementCountImpl(long peer);


// Functions
    public Element createElement(String tagName) throws DOMException
    {
        return ElementImpl.getImpl(createElementImpl(getPeer()
            , tagName));
    }
    native static long createElementImpl(long peer
        , String tagName);


    public DocumentFragment createDocumentFragment()
    {
        return DocumentFragmentImpl.getImpl(createDocumentFragmentImpl(getPeer()));
    }
    native static long createDocumentFragmentImpl(long peer);


    public Text createTextNode(String data)
    {
        return TextImpl.getImpl(createTextNodeImpl(getPeer()
            , data));
    }
    native static long createTextNodeImpl(long peer
        , String data);


    public Comment createComment(String data)
    {
        return CommentImpl.getImpl(createCommentImpl(getPeer()
            , data));
    }
    native static long createCommentImpl(long peer
        , String data);


    public CDATASection createCDATASection(String data) throws DOMException
    {
        return CDATASectionImpl.getImpl(createCDATASectionImpl(getPeer()
            , data));
    }
    native static long createCDATASectionImpl(long peer
        , String data);


    public ProcessingInstruction createProcessingInstruction(String target
        , String data) throws DOMException
    {
        return (ProcessingInstruction)ProcessingInstructionImpl.getImpl(createProcessingInstructionImpl(getPeer()
            , target
            , data));
    }
    native static long createProcessingInstructionImpl(long peer
        , String target
        , String data);


    public Attr createAttribute(String name) throws DOMException
    {
        return AttrImpl.getImpl(createAttributeImpl(getPeer()
            , name));
    }
    native static long createAttributeImpl(long peer
        , String name);


    public EntityReference createEntityReference(String name) throws DOMException
    {
        return EntityReferenceImpl.getImpl(createEntityReferenceImpl(getPeer()
            , name));
    }
    native static long createEntityReferenceImpl(long peer
        , String name);


    public NodeList getElementsByTagName(String tagname)
    {
        return NodeListImpl.getImpl(getElementsByTagNameImpl(getPeer()
            , tagname));
    }
    native static long getElementsByTagNameImpl(long peer
        , String tagname);


    public Node importNode(Node importedNode
        , boolean deep) throws DOMException
    {
        return NodeImpl.getImpl(importNodeImpl(getPeer()
            , NodeImpl.getPeer(importedNode)
            , deep));
    }
    native static long importNodeImpl(long peer
        , long importedNode
        , boolean deep);


    public Element createElementNS(String namespaceURI
        , String qualifiedName) throws DOMException
    {
        return ElementImpl.getImpl(createElementNSImpl(getPeer()
            , namespaceURI
            , qualifiedName));
    }
    native static long createElementNSImpl(long peer
        , String namespaceURI
        , String qualifiedName);


    public Attr createAttributeNS(String namespaceURI
        , String qualifiedName) throws DOMException
    {
        return AttrImpl.getImpl(createAttributeNSImpl(getPeer()
            , namespaceURI
            , qualifiedName));
    }
    native static long createAttributeNSImpl(long peer
        , String namespaceURI
        , String qualifiedName);


    public NodeList getElementsByTagNameNS(String namespaceURI
        , String localName)
    {
        return NodeListImpl.getImpl(getElementsByTagNameNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    native static long getElementsByTagNameNSImpl(long peer
        , String namespaceURI
        , String localName);


    public Node adoptNode(Node source) throws DOMException
    {
        return NodeImpl.getImpl(adoptNodeImpl(getPeer()
            , NodeImpl.getPeer(source)));
    }
    native static long adoptNodeImpl(long peer
        , long source);


    public Event createEvent(String eventType) throws DOMException
    {
        return EventImpl.getImpl(createEventImpl(getPeer()
            , eventType));
    }
    native static long createEventImpl(long peer
        , String eventType);


    public Range createRange()
    {
        return RangeImpl.getImpl(createRangeImpl(getPeer()));
    }
    native static long createRangeImpl(long peer);


    public NodeIterator createNodeIterator(Node root
        , int whatToShow
        , NodeFilter filter
        , boolean expandEntityReferences) throws DOMException
    {
        return NodeIteratorImpl.getImpl(createNodeIteratorImpl(getPeer()
            , NodeImpl.getPeer(root)
            , whatToShow
            , NodeFilterImpl.getPeer(filter)
            , expandEntityReferences));
    }
    native static long createNodeIteratorImpl(long peer
        , long root
        , int whatToShow
        , long filter
        , boolean expandEntityReferences);


    public TreeWalker createTreeWalker(Node root
        , int whatToShow
        , NodeFilter filter
        , boolean expandEntityReferences) throws DOMException
    {
        return TreeWalkerImpl.getImpl(createTreeWalkerImpl(getPeer()
            , NodeImpl.getPeer(root)
            , whatToShow
            , NodeFilterImpl.getPeer(filter)
            , expandEntityReferences));
    }
    native static long createTreeWalkerImpl(long peer
        , long root
        , int whatToShow
        , long filter
        , boolean expandEntityReferences);


    public CSSStyleDeclaration getOverrideStyle(Element element
        , String pseudoElement)
    {
        return CSSStyleDeclarationImpl.getImpl(getOverrideStyleImpl(getPeer()
            , ElementImpl.getPeer(element)
            , pseudoElement));
    }
    native static long getOverrideStyleImpl(long peer
        , long element
        , String pseudoElement);


    public XPathExpression createExpression(String expression
        , XPathNSResolver resolver) throws DOMException
    {
        return XPathExpressionImpl.getImpl(createExpressionImpl(getPeer()
            , expression
            , XPathNSResolverImpl.getPeer(resolver)));
    }
    native static long createExpressionImpl(long peer
        , String expression
        , long resolver);


    public XPathNSResolver createNSResolver(Node nodeResolver)
    {
        return XPathNSResolverImpl.getImpl(createNSResolverImpl(getPeer()
            , NodeImpl.getPeer(nodeResolver)));
    }
    native static long createNSResolverImpl(long peer
        , long nodeResolver);


    public XPathResult evaluate(String expression
        , Node contextNode
        , XPathNSResolver resolver
        , short type
        , XPathResult inResult) throws DOMException
    {
        return XPathResultImpl.getImpl(evaluateImpl(getPeer()
            , expression
            , NodeImpl.getPeer(contextNode)
            , XPathNSResolverImpl.getPeer(resolver)
            , type
            , XPathResultImpl.getPeer(inResult)));
    }
    native static long evaluateImpl(long peer
        , String expression
        , long contextNode
        , long resolver
        , short type
        , long inResult);


    public boolean execCommand(String command
        , boolean userInterface
        , String value)
    {
        return execCommandImpl(getPeer()
            , command
            , userInterface
            , value);
    }
    native static boolean execCommandImpl(long peer
        , String command
        , boolean userInterface
        , String value);


    public boolean queryCommandEnabled(String command)
    {
        return queryCommandEnabledImpl(getPeer()
            , command);
    }
    native static boolean queryCommandEnabledImpl(long peer
        , String command);


    public boolean queryCommandIndeterm(String command)
    {
        return queryCommandIndetermImpl(getPeer()
            , command);
    }
    native static boolean queryCommandIndetermImpl(long peer
        , String command);


    public boolean queryCommandState(String command)
    {
        return queryCommandStateImpl(getPeer()
            , command);
    }
    native static boolean queryCommandStateImpl(long peer
        , String command);


    public boolean queryCommandSupported(String command)
    {
        return queryCommandSupportedImpl(getPeer()
            , command);
    }
    native static boolean queryCommandSupportedImpl(long peer
        , String command);


    public String queryCommandValue(String command)
    {
        return queryCommandValueImpl(getPeer()
            , command);
    }
    native static String queryCommandValueImpl(long peer
        , String command);


    public NodeList getElementsByName(String elementName)
    {
        return NodeListImpl.getImpl(getElementsByNameImpl(getPeer()
            , elementName));
    }
    native static long getElementsByNameImpl(long peer
        , String elementName);


    public Element elementFromPoint(int x
        , int y)
    {
        return ElementImpl.getImpl(elementFromPointImpl(getPeer()
            , x
            , y));
    }
    native static long elementFromPointImpl(long peer
        , int x
        , int y);


    public Range caretRangeFromPoint(int x
        , int y)
    {
        return RangeImpl.getImpl(caretRangeFromPointImpl(getPeer()
            , x
            , y));
    }
    native static long caretRangeFromPointImpl(long peer
        , int x
        , int y);


    public CSSStyleDeclaration createCSSStyleDeclaration()
    {
        return CSSStyleDeclarationImpl.getImpl(createCSSStyleDeclarationImpl(getPeer()));
    }
    native static long createCSSStyleDeclarationImpl(long peer);


    public HTMLCollection getElementsByClassName(String classNames)
    {
        return HTMLCollectionImpl.getImpl(getElementsByClassNameImpl(getPeer()
            , classNames));
    }
    native static long getElementsByClassNameImpl(long peer
        , String classNames);


    public boolean hasFocus()
    {
        return hasFocusImpl(getPeer());
    }
    native static boolean hasFocusImpl(long peer);


    public void webkitCancelFullScreen()
    {
        webkitCancelFullScreenImpl(getPeer());
    }
    native static void webkitCancelFullScreenImpl(long peer);


    public void webkitExitFullscreen()
    {
        webkitExitFullscreenImpl(getPeer());
    }
    native static void webkitExitFullscreenImpl(long peer);


    public Element getElementById(String elementId)
    {
        return ElementImpl.getImpl(getElementByIdImpl(getPeer()
            , elementId));
    }
    native static long getElementByIdImpl(long peer
        , String elementId);


    public Element querySelector(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(querySelectorImpl(getPeer()
            , selectors));
    }
    native static long querySelectorImpl(long peer
        , String selectors);


    public NodeList querySelectorAll(String selectors) throws DOMException
    {
        return NodeListImpl.getImpl(querySelectorAllImpl(getPeer()
            , selectors));
    }
    native static long querySelectorAllImpl(long peer
        , String selectors);



//stubs
    public boolean getStrictErrorChecking() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public DOMConfiguration getDomConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void normalizeDocument() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

