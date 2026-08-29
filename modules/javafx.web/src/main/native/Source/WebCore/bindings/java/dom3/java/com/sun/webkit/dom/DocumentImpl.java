/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

    static boolean isHTMLDocumentImpl(long peer) {
        return DocumentNative.isHTMLDocument(peer);
    }

    @Override public Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type, Object result) throws DOMException {
        return evaluate(expression, contextNode, resolver, type, (XPathResult)result);
    }


// Attributes
    @Override
    public DocumentType getDoctype() {
        return DocumentTypeImpl.getImpl(getDoctypeImpl(getPeer()));
    }
    static long getDoctypeImpl(long peer) {
        return DocumentNative.getDoctype(peer);
    }

    @Override
    public DOMImplementation getImplementation() {
        return DOMImplementationImpl.getImpl(getImplementationImpl(getPeer()));
    }
    static long getImplementationImpl(long peer) {
        return DocumentNative.getImplementation(peer);
    }

    @Override
    public Element getDocumentElement() {
        return ElementImpl.getImpl(getDocumentElementImpl(getPeer()));
    }
    static long getDocumentElementImpl(long peer) {
        return DocumentNative.getDocumentElement(peer);
    }

    @Override
    public String getInputEncoding() {
        return getInputEncodingImpl(getPeer());
    }
    static String getInputEncodingImpl(long peer) {
        return DocumentNative.getInputEncoding(peer);
    }

    @Override
    public String getXmlEncoding() {
        return getXmlEncodingImpl(getPeer());
    }
    static String getXmlEncodingImpl(long peer) {
        return DocumentNative.getXmlEncoding(peer);
    }

    @Override
    public String getXmlVersion() {
        return getXmlVersionImpl(getPeer());
    }
    static String getXmlVersionImpl(long peer) {
        return DocumentNative.getXmlVersion(peer);
    }

    @Override
    public void setXmlVersion(String value) throws DOMException {
        setXmlVersionImpl(getPeer(), value);
    }
    static void setXmlVersionImpl(long peer, String value) {
        DocumentNative.setXmlVersion(peer, value);
    }

    @Override
    public boolean getXmlStandalone() {
        return getXmlStandaloneImpl(getPeer());
    }
    static boolean getXmlStandaloneImpl(long peer) {
        return DocumentNative.getXmlStandalone(peer);
    }

    @Override
    public void setXmlStandalone(boolean value) throws DOMException {
        setXmlStandaloneImpl(getPeer(), value);
    }
    static void setXmlStandaloneImpl(long peer, boolean value) {
        DocumentNative.setXmlStandalone(peer, value);
    }

    @Override
    public String getDocumentURI() {
        return getDocumentURIImpl(getPeer());
    }
    static String getDocumentURIImpl(long peer) {
        return DocumentNative.getDocumentURI(peer);
    }

    @Override
    public void setDocumentURI(String value) {
        setDocumentURIImpl(getPeer(), value);
    }
    static void setDocumentURIImpl(long peer, String value) {
        DocumentNative.setDocumentURI(peer, value);
    }

    @Override
    public AbstractView getDefaultView() {
        return DOMWindowImpl.getImpl(getDefaultViewImpl(getPeer()));
    }
    static long getDefaultViewImpl(long peer) {
        return DocumentNative.getDefaultView(peer);
    }

    public StyleSheetList getStyleSheets() {
        return StyleSheetListImpl.getImpl(getStyleSheetsImpl(getPeer()));
    }
    static long getStyleSheetsImpl(long peer) {
        return DocumentNative.getStyleSheets(peer);
    }

    public String getContentType() {
        return getContentTypeImpl(getPeer());
    }
    static String getContentTypeImpl(long peer) {
        return DocumentNative.getContentType(peer);
    }

    public String getTitle() {
        return getTitleImpl(getPeer());
    }
    static String getTitleImpl(long peer) {
        return DocumentNative.getTitle(peer);
    }

    public void setTitle(String value) {
        setTitleImpl(getPeer(), value);
    }
    static void setTitleImpl(long peer, String value) {
        DocumentNative.setTitle(peer, value);
    }

    public String getReferrer() {
        return getReferrerImpl(getPeer());
    }
    static String getReferrerImpl(long peer) {
        return DocumentNative.getReferrer(peer);
    }

    public String getDomain() {
        return getDomainImpl(getPeer());
    }
    static String getDomainImpl(long peer) {
        return DocumentNative.getDomain(peer);
    }

    public String getURL() {
        return getURLImpl(getPeer());
    }
    static String getURLImpl(long peer) {
        return DocumentNative.getURL(peer);
    }

    public String getCookie() throws DOMException {
        return getCookieImpl(getPeer());
    }
    static String getCookieImpl(long peer) {
        return DocumentNative.getCookie(peer);
    }

    public void setCookie(String value) throws DOMException {
        setCookieImpl(getPeer(), value);
    }
    static void setCookieImpl(long peer, String value) {
        DocumentNative.setCookie(peer, value);
    }

    public HTMLElement getBody() {
        return HTMLElementImpl.getImpl(getBodyImpl(getPeer()));
    }
    static long getBodyImpl(long peer) {
        return DocumentNative.getBody(peer);
    }

    public void setBody(HTMLElement value) throws DOMException {
        setBodyImpl(getPeer(), HTMLElementImpl.getPeer(value));
    }
    static void setBodyImpl(long peer, long value) {
        DocumentNative.setBody(peer, value);
    }

    public HTMLHeadElement getHead() {
        return HTMLHeadElementImpl.getImpl(getHeadImpl(getPeer()));
    }
    static long getHeadImpl(long peer) {
        return DocumentNative.getHead(peer);
    }

    public HTMLCollection getImages() {
        return HTMLCollectionImpl.getImpl(getImagesImpl(getPeer()));
    }
    static long getImagesImpl(long peer) {
        return DocumentNative.getImages(peer);
    }

    public HTMLCollection getApplets() {
        return HTMLCollectionImpl.getImpl(getAppletsImpl(getPeer()));
    }
    static long getAppletsImpl(long peer) {
        return DocumentNative.getApplets(peer);
    }

    public HTMLCollection getLinks() {
        return HTMLCollectionImpl.getImpl(getLinksImpl(getPeer()));
    }
    static long getLinksImpl(long peer) {
        return DocumentNative.getLinks(peer);
    }

    public HTMLCollection getForms() {
        return HTMLCollectionImpl.getImpl(getFormsImpl(getPeer()));
    }
    static long getFormsImpl(long peer) {
        return DocumentNative.getForms(peer);
    }

    public HTMLCollection getAnchors() {
        return HTMLCollectionImpl.getImpl(getAnchorsImpl(getPeer()));
    }
    static long getAnchorsImpl(long peer) {
        return DocumentNative.getAnchors(peer);
    }

    public String getLastModified() {
        return getLastModifiedImpl(getPeer());
    }
    static String getLastModifiedImpl(long peer) {
        return DocumentNative.getLastModified(peer);
    }

    public String getCharset() {
        return getCharsetImpl(getPeer());
    }
    static String getCharsetImpl(long peer) {
        return DocumentNative.getCharset(peer);
    }

    public String getDefaultCharset() {
        return getDefaultCharsetImpl(getPeer());
    }
    static String getDefaultCharsetImpl(long peer) {
        return DocumentNative.getDefaultCharset(peer);
    }

    public String getReadyState() {
        return getReadyStateImpl(getPeer());
    }
    static String getReadyStateImpl(long peer) {
        return DocumentNative.getReadyState(peer);
    }

    public String getCharacterSet() {
        return getCharacterSetImpl(getPeer());
    }
    static String getCharacterSetImpl(long peer) {
        return DocumentNative.getCharacterSet(peer);
    }

    public String getPreferredStylesheetSet() {
        return getPreferredStylesheetSetImpl(getPeer());
    }
    static String getPreferredStylesheetSetImpl(long peer) {
        return DocumentNative.getPreferredStylesheetSet(peer);
    }

    public String getSelectedStylesheetSet() {
        return getSelectedStylesheetSetImpl(getPeer());
    }
    static String getSelectedStylesheetSetImpl(long peer) {
        return DocumentNative.getSelectedStylesheetSet(peer);
    }

    public void setSelectedStylesheetSet(String value) {
        setSelectedStylesheetSetImpl(getPeer(), value);
    }
    static void setSelectedStylesheetSetImpl(long peer, String value) {
        DocumentNative.setSelectedStylesheetSet(peer, value);
    }

    public Element getActiveElement() {
        return ElementImpl.getImpl(getActiveElementImpl(getPeer()));
    }
    static long getActiveElementImpl(long peer) {
        return DocumentNative.getActiveElement(peer);
    }

    public String getCompatMode() {
        return getCompatModeImpl(getPeer());
    }
    static String getCompatModeImpl(long peer) {
        return DocumentNative.getCompatMode(peer);
    }

    public boolean getWebkitIsFullScreen() {
        return getWebkitIsFullScreenImpl(getPeer());
    }
    static boolean getWebkitIsFullScreenImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getWebkitIsFullScreenImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public boolean getWebkitFullScreenKeyboardInputAllowed() {
        return getWebkitFullScreenKeyboardInputAllowedImpl(getPeer());
    }
    static boolean getWebkitFullScreenKeyboardInputAllowedImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getWebkitFullScreenKeyboardInputAllowedImpl: no"
                + " wkj_* function exists for it in any jfxwebkit build");
    }

    public Element getWebkitCurrentFullScreenElement() {
        return ElementImpl.getImpl(getWebkitCurrentFullScreenElementImpl(getPeer()));
    }
    static long getWebkitCurrentFullScreenElementImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getWebkitCurrentFullScreenElementImpl: no wkj_*"
                + " function exists for it in any jfxwebkit build");
    }

    public boolean getWebkitFullscreenEnabled() {
        return getWebkitFullscreenEnabledImpl(getPeer());
    }
    static boolean getWebkitFullscreenEnabledImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getWebkitFullscreenEnabledImpl: no wkj_*"
                + " function exists for it in any jfxwebkit build");
    }

    public Element getWebkitFullscreenElement() {
        return ElementImpl.getImpl(getWebkitFullscreenElementImpl(getPeer()));
    }
    static long getWebkitFullscreenElementImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getWebkitFullscreenElementImpl: no wkj_*"
                + " function exists for it in any jfxwebkit build");
    }

    public String getVisibilityState() {
        return getVisibilityStateImpl(getPeer());
    }
    static String getVisibilityStateImpl(long peer) {
        return DocumentNative.getVisibilityState(peer);
    }

    public boolean getHidden() {
        return getHiddenImpl(getPeer());
    }
    static boolean getHiddenImpl(long peer) {
        return DocumentNative.getHidden(peer);
    }

    public HTMLScriptElement getCurrentScript() {
        return HTMLScriptElementImpl.getImpl(getCurrentScriptImpl(getPeer()));
    }
    static long getCurrentScriptImpl(long peer) {
        return DocumentNative.getCurrentScript(peer);
    }

    public String getOrigin() {
        return getOriginImpl(getPeer());
    }
    static String getOriginImpl(long peer) {
        return DocumentNative.getOrigin(peer);
    }

    public Element getScrollingElement() {
        return ElementImpl.getImpl(getScrollingElementImpl(getPeer()));
    }
    static long getScrollingElementImpl(long peer) {
        return DocumentNative.getScrollingElement(peer);
    }

    public EventListener getOnbeforecopy() {
        return EventListenerImpl.getImpl(getOnbeforecopyImpl(getPeer()));
    }
    static long getOnbeforecopyImpl(long peer) {
        return DocumentNative.getOnbeforecopy(peer);
    }

    public void setOnbeforecopy(EventListener value) {
        setOnbeforecopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforecopyImpl(long peer, long value) {
        DocumentNative.setOnbeforecopy(peer, value);
    }

    public EventListener getOnbeforecut() {
        return EventListenerImpl.getImpl(getOnbeforecutImpl(getPeer()));
    }
    static long getOnbeforecutImpl(long peer) {
        return DocumentNative.getOnbeforecut(peer);
    }

    public void setOnbeforecut(EventListener value) {
        setOnbeforecutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforecutImpl(long peer, long value) {
        DocumentNative.setOnbeforecut(peer, value);
    }

    public EventListener getOnbeforepaste() {
        return EventListenerImpl.getImpl(getOnbeforepasteImpl(getPeer()));
    }
    static long getOnbeforepasteImpl(long peer) {
        return DocumentNative.getOnbeforepaste(peer);
    }

    public void setOnbeforepaste(EventListener value) {
        setOnbeforepasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforepasteImpl(long peer, long value) {
        DocumentNative.setOnbeforepaste(peer, value);
    }

    public EventListener getOncopy() {
        return EventListenerImpl.getImpl(getOncopyImpl(getPeer()));
    }
    static long getOncopyImpl(long peer) {
        return DocumentNative.getOncopy(peer);
    }

    public void setOncopy(EventListener value) {
        setOncopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncopyImpl(long peer, long value) {
        DocumentNative.setOncopy(peer, value);
    }

    public EventListener getOncut() {
        return EventListenerImpl.getImpl(getOncutImpl(getPeer()));
    }
    static long getOncutImpl(long peer) {
        return DocumentNative.getOncut(peer);
    }

    public void setOncut(EventListener value) {
        setOncutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncutImpl(long peer, long value) {
        DocumentNative.setOncut(peer, value);
    }

    public EventListener getOnpaste() {
        return EventListenerImpl.getImpl(getOnpasteImpl(getPeer()));
    }
    static long getOnpasteImpl(long peer) {
        return DocumentNative.getOnpaste(peer);
    }

    public void setOnpaste(EventListener value) {
        setOnpasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpasteImpl(long peer, long value) {
        DocumentNative.setOnpaste(peer, value);
    }

    public EventListener getOnselectstart() {
        return EventListenerImpl.getImpl(getOnselectstartImpl(getPeer()));
    }
    static long getOnselectstartImpl(long peer) {
        return DocumentNative.getOnselectstart(peer);
    }

    public void setOnselectstart(EventListener value) {
        setOnselectstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectstartImpl(long peer, long value) {
        DocumentNative.setOnselectstart(peer, value);
    }

    public EventListener getOnselectionchange() {
        return EventListenerImpl.getImpl(getOnselectionchangeImpl(getPeer()));
    }
    static long getOnselectionchangeImpl(long peer) {
        return DocumentNative.getOnselectionchange(peer);
    }

    public void setOnselectionchange(EventListener value) {
        setOnselectionchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectionchangeImpl(long peer, long value) {
        DocumentNative.setOnselectionchange(peer, value);
    }

    public EventListener getOnreadystatechange() {
        return EventListenerImpl.getImpl(getOnreadystatechangeImpl(getPeer()));
    }
    static long getOnreadystatechangeImpl(long peer) {
        return DocumentNative.getOnreadystatechange(peer);
    }

    public void setOnreadystatechange(EventListener value) {
        setOnreadystatechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnreadystatechangeImpl(long peer, long value) {
        DocumentNative.setOnreadystatechange(peer, value);
    }

    public EventListener getOnabort() {
        return EventListenerImpl.getImpl(getOnabortImpl(getPeer()));
    }
    static long getOnabortImpl(long peer) {
        return DocumentNative.getOnabort(peer);
    }

    public void setOnabort(EventListener value) {
        setOnabortImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnabortImpl(long peer, long value) {
        DocumentNative.setOnabort(peer, value);
    }

    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    static long getOnblurImpl(long peer) {
        return DocumentNative.getOnblur(peer);
    }

    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnblurImpl(long peer, long value) {
        DocumentNative.setOnblur(peer, value);
    }

    public EventListener getOncanplay() {
        return EventListenerImpl.getImpl(getOncanplayImpl(getPeer()));
    }
    static long getOncanplayImpl(long peer) {
        return DocumentNative.getOncanplay(peer);
    }

    public void setOncanplay(EventListener value) {
        setOncanplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncanplayImpl(long peer, long value) {
        DocumentNative.setOncanplay(peer, value);
    }

    public EventListener getOncanplaythrough() {
        return EventListenerImpl.getImpl(getOncanplaythroughImpl(getPeer()));
    }
    static long getOncanplaythroughImpl(long peer) {
        return DocumentNative.getOncanplaythrough(peer);
    }

    public void setOncanplaythrough(EventListener value) {
        setOncanplaythroughImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncanplaythroughImpl(long peer, long value) {
        DocumentNative.setOncanplaythrough(peer, value);
    }

    public EventListener getOnchange() {
        return EventListenerImpl.getImpl(getOnchangeImpl(getPeer()));
    }
    static long getOnchangeImpl(long peer) {
        return DocumentNative.getOnchange(peer);
    }

    public void setOnchange(EventListener value) {
        setOnchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnchangeImpl(long peer, long value) {
        DocumentNative.setOnchange(peer, value);
    }

    public EventListener getOnclick() {
        return EventListenerImpl.getImpl(getOnclickImpl(getPeer()));
    }
    static long getOnclickImpl(long peer) {
        return DocumentNative.getOnclick(peer);
    }

    public void setOnclick(EventListener value) {
        setOnclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnclickImpl(long peer, long value) {
        DocumentNative.setOnclick(peer, value);
    }

    public EventListener getOncontextmenu() {
        return EventListenerImpl.getImpl(getOncontextmenuImpl(getPeer()));
    }
    static long getOncontextmenuImpl(long peer) {
        return DocumentNative.getOncontextmenu(peer);
    }

    public void setOncontextmenu(EventListener value) {
        setOncontextmenuImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncontextmenuImpl(long peer, long value) {
        DocumentNative.setOncontextmenu(peer, value);
    }

    public EventListener getOndblclick() {
        return EventListenerImpl.getImpl(getOndblclickImpl(getPeer()));
    }
    static long getOndblclickImpl(long peer) {
        return DocumentNative.getOndblclick(peer);
    }

    public void setOndblclick(EventListener value) {
        setOndblclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndblclickImpl(long peer, long value) {
        DocumentNative.setOndblclick(peer, value);
    }

    public EventListener getOndrag() {
        return EventListenerImpl.getImpl(getOndragImpl(getPeer()));
    }
    static long getOndragImpl(long peer) {
        return DocumentNative.getOndrag(peer);
    }

    public void setOndrag(EventListener value) {
        setOndragImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragImpl(long peer, long value) {
        DocumentNative.setOndrag(peer, value);
    }

    public EventListener getOndragend() {
        return EventListenerImpl.getImpl(getOndragendImpl(getPeer()));
    }
    static long getOndragendImpl(long peer) {
        return DocumentNative.getOndragend(peer);
    }

    public void setOndragend(EventListener value) {
        setOndragendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragendImpl(long peer, long value) {
        DocumentNative.setOndragend(peer, value);
    }

    public EventListener getOndragenter() {
        return EventListenerImpl.getImpl(getOndragenterImpl(getPeer()));
    }
    static long getOndragenterImpl(long peer) {
        return DocumentNative.getOndragenter(peer);
    }

    public void setOndragenter(EventListener value) {
        setOndragenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragenterImpl(long peer, long value) {
        DocumentNative.setOndragenter(peer, value);
    }

    public EventListener getOndragleave() {
        return EventListenerImpl.getImpl(getOndragleaveImpl(getPeer()));
    }
    static long getOndragleaveImpl(long peer) {
        return DocumentNative.getOndragleave(peer);
    }

    public void setOndragleave(EventListener value) {
        setOndragleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragleaveImpl(long peer, long value) {
        DocumentNative.setOndragleave(peer, value);
    }

    public EventListener getOndragover() {
        return EventListenerImpl.getImpl(getOndragoverImpl(getPeer()));
    }
    static long getOndragoverImpl(long peer) {
        return DocumentNative.getOndragover(peer);
    }

    public void setOndragover(EventListener value) {
        setOndragoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragoverImpl(long peer, long value) {
        DocumentNative.setOndragover(peer, value);
    }

    public EventListener getOndragstart() {
        return EventListenerImpl.getImpl(getOndragstartImpl(getPeer()));
    }
    static long getOndragstartImpl(long peer) {
        return DocumentNative.getOndragstart(peer);
    }

    public void setOndragstart(EventListener value) {
        setOndragstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragstartImpl(long peer, long value) {
        DocumentNative.setOndragstart(peer, value);
    }

    public EventListener getOndrop() {
        return EventListenerImpl.getImpl(getOndropImpl(getPeer()));
    }
    static long getOndropImpl(long peer) {
        return DocumentNative.getOndrop(peer);
    }

    public void setOndrop(EventListener value) {
        setOndropImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndropImpl(long peer, long value) {
        DocumentNative.setOndrop(peer, value);
    }

    public EventListener getOndurationchange() {
        return EventListenerImpl.getImpl(getOndurationchangeImpl(getPeer()));
    }
    static long getOndurationchangeImpl(long peer) {
        return DocumentNative.getOndurationchange(peer);
    }

    public void setOndurationchange(EventListener value) {
        setOndurationchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndurationchangeImpl(long peer, long value) {
        DocumentNative.setOndurationchange(peer, value);
    }

    public EventListener getOnemptied() {
        return EventListenerImpl.getImpl(getOnemptiedImpl(getPeer()));
    }
    static long getOnemptiedImpl(long peer) {
        return DocumentNative.getOnemptied(peer);
    }

    public void setOnemptied(EventListener value) {
        setOnemptiedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnemptiedImpl(long peer, long value) {
        DocumentNative.setOnemptied(peer, value);
    }

    public EventListener getOnended() {
        return EventListenerImpl.getImpl(getOnendedImpl(getPeer()));
    }
    static long getOnendedImpl(long peer) {
        return DocumentNative.getOnended(peer);
    }

    public void setOnended(EventListener value) {
        setOnendedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnendedImpl(long peer, long value) {
        DocumentNative.setOnended(peer, value);
    }

    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    static long getOnerrorImpl(long peer) {
        return DocumentNative.getOnerror(peer);
    }

    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnerrorImpl(long peer, long value) {
        DocumentNative.setOnerror(peer, value);
    }

    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    static long getOnfocusImpl(long peer) {
        return DocumentNative.getOnfocus(peer);
    }

    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusImpl(long peer, long value) {
        DocumentNative.setOnfocus(peer, value);
    }

    public EventListener getOninput() {
        return EventListenerImpl.getImpl(getOninputImpl(getPeer()));
    }
    static long getOninputImpl(long peer) {
        return DocumentNative.getOninput(peer);
    }

    public void setOninput(EventListener value) {
        setOninputImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOninputImpl(long peer, long value) {
        DocumentNative.setOninput(peer, value);
    }

    public EventListener getOninvalid() {
        return EventListenerImpl.getImpl(getOninvalidImpl(getPeer()));
    }
    static long getOninvalidImpl(long peer) {
        return DocumentNative.getOninvalid(peer);
    }

    public void setOninvalid(EventListener value) {
        setOninvalidImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOninvalidImpl(long peer, long value) {
        DocumentNative.setOninvalid(peer, value);
    }

    public EventListener getOnkeydown() {
        return EventListenerImpl.getImpl(getOnkeydownImpl(getPeer()));
    }
    static long getOnkeydownImpl(long peer) {
        return DocumentNative.getOnkeydown(peer);
    }

    public void setOnkeydown(EventListener value) {
        setOnkeydownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeydownImpl(long peer, long value) {
        DocumentNative.setOnkeydown(peer, value);
    }

    public EventListener getOnkeypress() {
        return EventListenerImpl.getImpl(getOnkeypressImpl(getPeer()));
    }
    static long getOnkeypressImpl(long peer) {
        return DocumentNative.getOnkeypress(peer);
    }

    public void setOnkeypress(EventListener value) {
        setOnkeypressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeypressImpl(long peer, long value) {
        DocumentNative.setOnkeypress(peer, value);
    }

    public EventListener getOnkeyup() {
        return EventListenerImpl.getImpl(getOnkeyupImpl(getPeer()));
    }
    static long getOnkeyupImpl(long peer) {
        return DocumentNative.getOnkeyup(peer);
    }

    public void setOnkeyup(EventListener value) {
        setOnkeyupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeyupImpl(long peer, long value) {
        DocumentNative.setOnkeyup(peer, value);
    }

    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    static long getOnloadImpl(long peer) {
        return DocumentNative.getOnload(peer);
    }

    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadImpl(long peer, long value) {
        DocumentNative.setOnload(peer, value);
    }

    public EventListener getOnloadeddata() {
        return EventListenerImpl.getImpl(getOnloadeddataImpl(getPeer()));
    }
    static long getOnloadeddataImpl(long peer) {
        return DocumentNative.getOnloadeddata(peer);
    }

    public void setOnloadeddata(EventListener value) {
        setOnloadeddataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadeddataImpl(long peer, long value) {
        DocumentNative.setOnloadeddata(peer, value);
    }

    public EventListener getOnloadedmetadata() {
        return EventListenerImpl.getImpl(getOnloadedmetadataImpl(getPeer()));
    }
    static long getOnloadedmetadataImpl(long peer) {
        return DocumentNative.getOnloadedmetadata(peer);
    }

    public void setOnloadedmetadata(EventListener value) {
        setOnloadedmetadataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadedmetadataImpl(long peer, long value) {
        DocumentNative.setOnloadedmetadata(peer, value);
    }

    public EventListener getOnloadstart() {
        return EventListenerImpl.getImpl(getOnloadstartImpl(getPeer()));
    }
    static long getOnloadstartImpl(long peer) {
        return DocumentNative.getOnloadstart(peer);
    }

    public void setOnloadstart(EventListener value) {
        setOnloadstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadstartImpl(long peer, long value) {
        DocumentNative.setOnloadstart(peer, value);
    }

    public EventListener getOnmousedown() {
        return EventListenerImpl.getImpl(getOnmousedownImpl(getPeer()));
    }
    static long getOnmousedownImpl(long peer) {
        return DocumentNative.getOnmousedown(peer);
    }

    public void setOnmousedown(EventListener value) {
        setOnmousedownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousedownImpl(long peer, long value) {
        DocumentNative.setOnmousedown(peer, value);
    }

    public EventListener getOnmouseenter() {
        return EventListenerImpl.getImpl(getOnmouseenterImpl(getPeer()));
    }
    static long getOnmouseenterImpl(long peer) {
        return DocumentNative.getOnmouseenter(peer);
    }

    public void setOnmouseenter(EventListener value) {
        setOnmouseenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseenterImpl(long peer, long value) {
        DocumentNative.setOnmouseenter(peer, value);
    }

    public EventListener getOnmouseleave() {
        return EventListenerImpl.getImpl(getOnmouseleaveImpl(getPeer()));
    }
    static long getOnmouseleaveImpl(long peer) {
        return DocumentNative.getOnmouseleave(peer);
    }

    public void setOnmouseleave(EventListener value) {
        setOnmouseleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseleaveImpl(long peer, long value) {
        DocumentNative.setOnmouseleave(peer, value);
    }

    public EventListener getOnmousemove() {
        return EventListenerImpl.getImpl(getOnmousemoveImpl(getPeer()));
    }
    static long getOnmousemoveImpl(long peer) {
        return DocumentNative.getOnmousemove(peer);
    }

    public void setOnmousemove(EventListener value) {
        setOnmousemoveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousemoveImpl(long peer, long value) {
        DocumentNative.setOnmousemove(peer, value);
    }

    public EventListener getOnmouseout() {
        return EventListenerImpl.getImpl(getOnmouseoutImpl(getPeer()));
    }
    static long getOnmouseoutImpl(long peer) {
        return DocumentNative.getOnmouseout(peer);
    }

    public void setOnmouseout(EventListener value) {
        setOnmouseoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseoutImpl(long peer, long value) {
        DocumentNative.setOnmouseout(peer, value);
    }

    public EventListener getOnmouseover() {
        return EventListenerImpl.getImpl(getOnmouseoverImpl(getPeer()));
    }
    static long getOnmouseoverImpl(long peer) {
        return DocumentNative.getOnmouseover(peer);
    }

    public void setOnmouseover(EventListener value) {
        setOnmouseoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseoverImpl(long peer, long value) {
        DocumentNative.setOnmouseover(peer, value);
    }

    public EventListener getOnmouseup() {
        return EventListenerImpl.getImpl(getOnmouseupImpl(getPeer()));
    }
    static long getOnmouseupImpl(long peer) {
        return DocumentNative.getOnmouseup(peer);
    }

    public void setOnmouseup(EventListener value) {
        setOnmouseupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseupImpl(long peer, long value) {
        DocumentNative.setOnmouseup(peer, value);
    }

    public EventListener getOnmousewheel() {
        return EventListenerImpl.getImpl(getOnmousewheelImpl(getPeer()));
    }
    static long getOnmousewheelImpl(long peer) {
        return DocumentNative.getOnmousewheel(peer);
    }

    public void setOnmousewheel(EventListener value) {
        setOnmousewheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousewheelImpl(long peer, long value) {
        DocumentNative.setOnmousewheel(peer, value);
    }

    public EventListener getOnpause() {
        return EventListenerImpl.getImpl(getOnpauseImpl(getPeer()));
    }
    static long getOnpauseImpl(long peer) {
        return DocumentNative.getOnpause(peer);
    }

    public void setOnpause(EventListener value) {
        setOnpauseImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpauseImpl(long peer, long value) {
        DocumentNative.setOnpause(peer, value);
    }

    public EventListener getOnplay() {
        return EventListenerImpl.getImpl(getOnplayImpl(getPeer()));
    }
    static long getOnplayImpl(long peer) {
        return DocumentNative.getOnplay(peer);
    }

    public void setOnplay(EventListener value) {
        setOnplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnplayImpl(long peer, long value) {
        DocumentNative.setOnplay(peer, value);
    }

    public EventListener getOnplaying() {
        return EventListenerImpl.getImpl(getOnplayingImpl(getPeer()));
    }
    static long getOnplayingImpl(long peer) {
        return DocumentNative.getOnplaying(peer);
    }

    public void setOnplaying(EventListener value) {
        setOnplayingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnplayingImpl(long peer, long value) {
        DocumentNative.setOnplaying(peer, value);
    }

    public EventListener getOnprogress() {
        return EventListenerImpl.getImpl(getOnprogressImpl(getPeer()));
    }
    static long getOnprogressImpl(long peer) {
        return DocumentNative.getOnprogress(peer);
    }

    public void setOnprogress(EventListener value) {
        setOnprogressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnprogressImpl(long peer, long value) {
        DocumentNative.setOnprogress(peer, value);
    }

    public EventListener getOnratechange() {
        return EventListenerImpl.getImpl(getOnratechangeImpl(getPeer()));
    }
    static long getOnratechangeImpl(long peer) {
        return DocumentNative.getOnratechange(peer);
    }

    public void setOnratechange(EventListener value) {
        setOnratechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnratechangeImpl(long peer, long value) {
        DocumentNative.setOnratechange(peer, value);
    }

    public EventListener getOnreset() {
        return EventListenerImpl.getImpl(getOnresetImpl(getPeer()));
    }
    static long getOnresetImpl(long peer) {
        return DocumentNative.getOnreset(peer);
    }

    public void setOnreset(EventListener value) {
        setOnresetImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresetImpl(long peer, long value) {
        DocumentNative.setOnreset(peer, value);
    }

    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    static long getOnresizeImpl(long peer) {
        return DocumentNative.getOnresize(peer);
    }

    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresizeImpl(long peer, long value) {
        DocumentNative.setOnresize(peer, value);
    }

    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    static long getOnscrollImpl(long peer) {
        return DocumentNative.getOnscroll(peer);
    }

    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnscrollImpl(long peer, long value) {
        DocumentNative.setOnscroll(peer, value);
    }

    public EventListener getOnseeked() {
        return EventListenerImpl.getImpl(getOnseekedImpl(getPeer()));
    }
    static long getOnseekedImpl(long peer) {
        return DocumentNative.getOnseeked(peer);
    }

    public void setOnseeked(EventListener value) {
        setOnseekedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnseekedImpl(long peer, long value) {
        DocumentNative.setOnseeked(peer, value);
    }

    public EventListener getOnseeking() {
        return EventListenerImpl.getImpl(getOnseekingImpl(getPeer()));
    }
    static long getOnseekingImpl(long peer) {
        return DocumentNative.getOnseeking(peer);
    }

    public void setOnseeking(EventListener value) {
        setOnseekingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnseekingImpl(long peer, long value) {
        DocumentNative.setOnseeking(peer, value);
    }

    public EventListener getOnselect() {
        return EventListenerImpl.getImpl(getOnselectImpl(getPeer()));
    }
    static long getOnselectImpl(long peer) {
        return DocumentNative.getOnselect(peer);
    }

    public void setOnselect(EventListener value) {
        setOnselectImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectImpl(long peer, long value) {
        DocumentNative.setOnselect(peer, value);
    }

    public EventListener getOnstalled() {
        return EventListenerImpl.getImpl(getOnstalledImpl(getPeer()));
    }
    static long getOnstalledImpl(long peer) {
        return DocumentNative.getOnstalled(peer);
    }

    public void setOnstalled(EventListener value) {
        setOnstalledImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnstalledImpl(long peer, long value) {
        DocumentNative.setOnstalled(peer, value);
    }

    public EventListener getOnsubmit() {
        return EventListenerImpl.getImpl(getOnsubmitImpl(getPeer()));
    }
    static long getOnsubmitImpl(long peer) {
        return DocumentNative.getOnsubmit(peer);
    }

    public void setOnsubmit(EventListener value) {
        setOnsubmitImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsubmitImpl(long peer, long value) {
        DocumentNative.setOnsubmit(peer, value);
    }

    public EventListener getOnsuspend() {
        return EventListenerImpl.getImpl(getOnsuspendImpl(getPeer()));
    }
    static long getOnsuspendImpl(long peer) {
        return DocumentNative.getOnsuspend(peer);
    }

    public void setOnsuspend(EventListener value) {
        setOnsuspendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsuspendImpl(long peer, long value) {
        DocumentNative.setOnsuspend(peer, value);
    }

    public EventListener getOntimeupdate() {
        return EventListenerImpl.getImpl(getOntimeupdateImpl(getPeer()));
    }
    static long getOntimeupdateImpl(long peer) {
        return DocumentNative.getOntimeupdate(peer);
    }

    public void setOntimeupdate(EventListener value) {
        setOntimeupdateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOntimeupdateImpl(long peer, long value) {
        DocumentNative.setOntimeupdate(peer, value);
    }

    public EventListener getOnvolumechange() {
        return EventListenerImpl.getImpl(getOnvolumechangeImpl(getPeer()));
    }
    static long getOnvolumechangeImpl(long peer) {
        return DocumentNative.getOnvolumechange(peer);
    }

    public void setOnvolumechange(EventListener value) {
        setOnvolumechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnvolumechangeImpl(long peer, long value) {
        DocumentNative.setOnvolumechange(peer, value);
    }

    public EventListener getOnwaiting() {
        return EventListenerImpl.getImpl(getOnwaitingImpl(getPeer()));
    }
    static long getOnwaitingImpl(long peer) {
        return DocumentNative.getOnwaiting(peer);
    }

    public void setOnwaiting(EventListener value) {
        setOnwaitingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwaitingImpl(long peer, long value) {
        DocumentNative.setOnwaiting(peer, value);
    }

    public EventListener getOnsearch() {
        return EventListenerImpl.getImpl(getOnsearchImpl(getPeer()));
    }
    static long getOnsearchImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.getOnsearchImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }

    public void setOnsearch(EventListener value) {
        setOnsearchImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsearchImpl(long peer, long value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.setOnsearchImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }

    public EventListener getOnwheel() {
        return EventListenerImpl.getImpl(getOnwheelImpl(getPeer()));
    }
    static long getOnwheelImpl(long peer) {
        return DocumentNative.getOnwheel(peer);
    }

    public void setOnwheel(EventListener value) {
        setOnwheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwheelImpl(long peer, long value) {
        DocumentNative.setOnwheel(peer, value);
    }

    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    static long getChildrenImpl(long peer) {
        return DocumentNative.getChildren(peer);
    }

    public Element getFirstElementChild() {
        return ElementImpl.getImpl(getFirstElementChildImpl(getPeer()));
    }
    static long getFirstElementChildImpl(long peer) {
        return DocumentNative.getFirstElementChild(peer);
    }

    public Element getLastElementChild() {
        return ElementImpl.getImpl(getLastElementChildImpl(getPeer()));
    }
    static long getLastElementChildImpl(long peer) {
        return DocumentNative.getLastElementChild(peer);
    }

    public int getChildElementCount() {
        return getChildElementCountImpl(getPeer());
    }
    static int getChildElementCountImpl(long peer) {
        return DocumentNative.getChildElementCount(peer);
    }


// Functions
    @Override
    public Element createElement(String tagName) throws DOMException
    {
        return ElementImpl.getImpl(createElementImpl(getPeer()
            , tagName));
    }
    static long createElementImpl(long peer
        , String tagName) {
        return DocumentNative.createElement(peer, tagName);
    }


    @Override
    public DocumentFragment createDocumentFragment()
    {
        return DocumentFragmentImpl.getImpl(createDocumentFragmentImpl(getPeer()));
    }
    static long createDocumentFragmentImpl(long peer) {
        return DocumentNative.createDocumentFragment(peer);
    }


    @Override
    public Text createTextNode(String data)
    {
        return TextImpl.getImpl(createTextNodeImpl(getPeer()
            , data));
    }
    static long createTextNodeImpl(long peer
        , String data) {
        return DocumentNative.createTextNode(peer, data);
    }


    @Override
    public Comment createComment(String data)
    {
        return CommentImpl.getImpl(createCommentImpl(getPeer()
            , data));
    }
    static long createCommentImpl(long peer
        , String data) {
        return DocumentNative.createComment(peer, data);
    }


    @Override
    public CDATASection createCDATASection(String data) throws DOMException
    {
        return CDATASectionImpl.getImpl(createCDATASectionImpl(getPeer()
            , data));
    }
    static long createCDATASectionImpl(long peer
        , String data) {
        return DocumentNative.createCDATASection(peer, data);
    }


    @Override
    public ProcessingInstruction createProcessingInstruction(String target
        , String data) throws DOMException
    {
        return (ProcessingInstruction)ProcessingInstructionImpl.getImpl(createProcessingInstructionImpl(getPeer()
            , target
            , data));
    }
    static long createProcessingInstructionImpl(long peer
        , String target
        , String data) {
        return DocumentNative.createProcessingInstruction(peer, target, data);
    }


    @Override
    public Attr createAttribute(String name) throws DOMException
    {
        return AttrImpl.getImpl(createAttributeImpl(getPeer()
            , name));
    }
    static long createAttributeImpl(long peer
        , String name) {
        return DocumentNative.createAttribute(peer, name);
    }


    @Override
    public EntityReference createEntityReference(String name) throws DOMException
    {
        return EntityReferenceImpl.getImpl(createEntityReferenceImpl(getPeer()
            , name));
    }
    static long createEntityReferenceImpl(long peer
        , String name) {
        return DocumentNative.createEntityReference(peer, name);
    }


    @Override
    public NodeList getElementsByTagName(String tagname)
    {
        return NodeListImpl.getImpl(getElementsByTagNameImpl(getPeer()
            , tagname));
    }
    static long getElementsByTagNameImpl(long peer
        , String tagname) {
        return DocumentNative.getElementsByTagName(peer, tagname);
    }


    @Override
    public Node importNode(Node importedNode
        , boolean deep) throws DOMException
    {
        return NodeImpl.getImpl(importNodeImpl(getPeer()
            , NodeImpl.getPeer(importedNode)
            , deep));
    }
    static long importNodeImpl(long peer
        , long importedNode
        , boolean deep) {
        return DocumentNative.importNode(peer, importedNode, deep);
    }


    @Override
    public Element createElementNS(String namespaceURI
        , String qualifiedName) throws DOMException
    {
        return ElementImpl.getImpl(createElementNSImpl(getPeer()
            , namespaceURI
            , qualifiedName));
    }
    static long createElementNSImpl(long peer
        , String namespaceURI
        , String qualifiedName) {
        return DocumentNative.createElementNS(peer, namespaceURI, qualifiedName);
    }


    @Override
    public Attr createAttributeNS(String namespaceURI
        , String qualifiedName) throws DOMException
    {
        return AttrImpl.getImpl(createAttributeNSImpl(getPeer()
            , namespaceURI
            , qualifiedName));
    }
    static long createAttributeNSImpl(long peer
        , String namespaceURI
        , String qualifiedName) {
        return DocumentNative.createAttributeNS(peer, namespaceURI, qualifiedName);
    }


    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI
        , String localName)
    {
        return NodeListImpl.getImpl(getElementsByTagNameNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    static long getElementsByTagNameNSImpl(long peer
        , String namespaceURI
        , String localName) {
        return DocumentNative.getElementsByTagNameNS(peer, namespaceURI, localName);
    }


    @Override
    public Node adoptNode(Node source) throws DOMException
    {
        return NodeImpl.getImpl(adoptNodeImpl(getPeer()
            , NodeImpl.getPeer(source)));
    }
    static long adoptNodeImpl(long peer
        , long source) {
        return DocumentNative.adoptNode(peer, source);
    }


    @Override
    public Event createEvent(String eventType) throws DOMException
    {
        return EventImpl.getImpl(createEventImpl(getPeer()
            , eventType));
    }
    static long createEventImpl(long peer
        , String eventType) {
        return DocumentNative.createEvent(peer, eventType);
    }


    public Range createRange()
    {
        return RangeImpl.getImpl(createRangeImpl(getPeer()));
    }
    static long createRangeImpl(long peer) {
        return DocumentNative.createRange(peer);
    }


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
    static long createNodeIteratorImpl(long peer
        , long root
        , int whatToShow
        , long filter
        , boolean expandEntityReferences) {
        return DocumentNative.createNodeIterator(peer, root, whatToShow, filter, expandEntityReferences);
    }


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
    static long createTreeWalkerImpl(long peer
        , long root
        , int whatToShow
        , long filter
        , boolean expandEntityReferences) {
        return DocumentNative.createTreeWalker(peer, root, whatToShow, filter, expandEntityReferences);
    }


    public CSSStyleDeclaration getOverrideStyle(Element element
        , String pseudoElement)
    {
        return CSSStyleDeclarationImpl.getImpl(getOverrideStyleImpl(getPeer()
            , ElementImpl.getPeer(element)
            , pseudoElement));
    }
    static long getOverrideStyleImpl(long peer
        , long element
        , String pseudoElement) {
        return DocumentNative.getOverrideStyle(peer, element, pseudoElement);
    }


    @Override
    public XPathExpression createExpression(String expression
        , XPathNSResolver resolver) throws DOMException
    {
        return XPathExpressionImpl.getImpl(createExpressionImpl(getPeer()
            , expression
            , XPathNSResolverImpl.getPeer(resolver)));
    }
    static long createExpressionImpl(long peer
        , String expression
        , long resolver) {
        return DocumentNative.createExpression(peer, expression, resolver);
    }


    @Override
    public XPathNSResolver createNSResolver(Node nodeResolver)
    {
        return XPathNSResolverImpl.getImpl(createNSResolverImpl(getPeer()
            , NodeImpl.getPeer(nodeResolver)));
    }
    static long createNSResolverImpl(long peer
        , long nodeResolver) {
        return DocumentNative.createNSResolver(peer, nodeResolver);
    }


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
    static long evaluateImpl(long peer
        , String expression
        , long contextNode
        , long resolver
        , short type
        , long inResult) {
        return DocumentNative.evaluate(peer, expression, contextNode, resolver, type, inResult);
    }


    public boolean execCommand(String command
        , boolean userInterface
        , String value)
    {
        return execCommandImpl(getPeer()
            , command
            , userInterface
            , value);
    }
    static boolean execCommandImpl(long peer
        , String command
        , boolean userInterface
        , String value) {
        return DocumentNative.execCommand(peer, command, userInterface, value);
    }


    public boolean queryCommandEnabled(String command)
    {
        return queryCommandEnabledImpl(getPeer()
            , command);
    }
    static boolean queryCommandEnabledImpl(long peer
        , String command) {
        return DocumentNative.queryCommandEnabled(peer, command);
    }


    public boolean queryCommandIndeterm(String command)
    {
        return queryCommandIndetermImpl(getPeer()
            , command);
    }
    static boolean queryCommandIndetermImpl(long peer
        , String command) {
        return DocumentNative.queryCommandIndeterm(peer, command);
    }


    public boolean queryCommandState(String command)
    {
        return queryCommandStateImpl(getPeer()
            , command);
    }
    static boolean queryCommandStateImpl(long peer
        , String command) {
        return DocumentNative.queryCommandState(peer, command);
    }


    public boolean queryCommandSupported(String command)
    {
        return queryCommandSupportedImpl(getPeer()
            , command);
    }
    static boolean queryCommandSupportedImpl(long peer
        , String command) {
        return DocumentNative.queryCommandSupported(peer, command);
    }


    public String queryCommandValue(String command)
    {
        return queryCommandValueImpl(getPeer()
            , command);
    }
    static String queryCommandValueImpl(long peer
        , String command) {
        return DocumentNative.queryCommandValue(peer, command);
    }


    public NodeList getElementsByName(String elementName)
    {
        return NodeListImpl.getImpl(getElementsByNameImpl(getPeer()
            , elementName));
    }
    static long getElementsByNameImpl(long peer
        , String elementName) {
        return DocumentNative.getElementsByName(peer, elementName);
    }


    public Element elementFromPoint(int x
        , int y)
    {
        return ElementImpl.getImpl(elementFromPointImpl(getPeer()
            , x
            , y));
    }
    static long elementFromPointImpl(long peer
        , int x
        , int y) {
        return DocumentNative.elementFromPoint(peer, x, y);
    }


    public Range caretRangeFromPoint(int x
        , int y)
    {
        return RangeImpl.getImpl(caretRangeFromPointImpl(getPeer()
            , x
            , y));
    }
    static long caretRangeFromPointImpl(long peer
        , int x
        , int y) {
        return DocumentNative.caretRangeFromPoint(peer, x, y);
    }


    public CSSStyleDeclaration createCSSStyleDeclaration()
    {
        return CSSStyleDeclarationImpl.getImpl(createCSSStyleDeclarationImpl(getPeer()));
    }
    static long createCSSStyleDeclarationImpl(long peer) {
        return DocumentNative.createCSSStyleDeclaration(peer);
    }


    public HTMLCollection getElementsByClassName(String classNames)
    {
        return HTMLCollectionImpl.getImpl(getElementsByClassNameImpl(getPeer()
            , classNames));
    }
    static long getElementsByClassNameImpl(long peer
        , String classNames) {
        return DocumentNative.getElementsByClassName(peer, classNames);
    }


    public boolean hasFocus()
    {
        return hasFocusImpl(getPeer());
    }
    static boolean hasFocusImpl(long peer) {
        return DocumentNative.hasFocus(peer);
    }


    public void webkitCancelFullScreen()
    {
        webkitCancelFullScreenImpl(getPeer());
    }
    static void webkitCancelFullScreenImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.webkitCancelFullScreenImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }


    public void webkitExitFullscreen()
    {
        webkitExitFullscreenImpl(getPeer());
    }
    static void webkitExitFullscreenImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DocumentImpl.webkitExitFullscreenImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }


    @Override
    public Element getElementById(String elementId)
    {
        return ElementImpl.getImpl(getElementByIdImpl(getPeer()
            , elementId));
    }
    static long getElementByIdImpl(long peer
        , String elementId) {
        return DocumentNative.getElementById(peer, elementId);
    }


    public Element querySelector(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(querySelectorImpl(getPeer()
            , selectors));
    }
    static long querySelectorImpl(long peer
        , String selectors) {
        return DocumentNative.querySelector(peer, selectors);
    }


    public NodeList querySelectorAll(String selectors) throws DOMException
    {
        return NodeListImpl.getImpl(querySelectorAllImpl(getPeer()
            , selectors));
    }
    static long querySelectorAllImpl(long peer
        , String selectors) {
        return DocumentNative.querySelectorAll(peer, selectors);
    }



//stubs
    @Override
    public boolean getStrictErrorChecking() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DOMConfiguration getDomConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void normalizeDocument() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

