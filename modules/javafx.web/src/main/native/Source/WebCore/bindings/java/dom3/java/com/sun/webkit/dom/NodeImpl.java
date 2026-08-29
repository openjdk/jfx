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

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import com.sun.webkit.Disposer;

public class NodeImpl extends JSObject implements Node, EventTarget {
    // We use a custom hash-table rather than java.util.HashMap,
    // because the latter requires 2 extra objects for each entry:
    // a Long for the key plus a Map.Entry.  Since we have a 'next'
    // field already in the SelfDisposer, we can use it as the entry.
    private static SelfDisposer[] hashTable = new SelfDisposer[64];
    private static int hashCount;

    private static int hashPeer(long peer) {
        return (int) (~peer ^ (peer >> 7)) & (hashTable.length-1);
    }

    private static Node getCachedImpl(long peer) {
        if (peer == 0)
            return null;
        int hash = hashPeer(peer);
        SelfDisposer head = hashTable[hash];
        SelfDisposer prev = null;
        for (SelfDisposer disposer = head; disposer != null;) {
            SelfDisposer next = disposer.next;
            if (disposer.peer == peer) {
                NodeImpl node = (NodeImpl) disposer.get();
                if (node != null) {
                    // the peer need to be deref'ed!
                    NodeImpl.dispose(peer);
                    return node;
                }
                if (prev != null)
                    prev.next = next;
                else
                    hashTable[hash] = next;
                break;
            }
            prev = disposer;
            disposer = next;
        }
        NodeImpl node = (NodeImpl)createInterface(peer);
        SelfDisposer disposer = new SelfDisposer(node, peer);
        Disposer.addRecord(disposer);
        disposer.next = head;
        hashTable[hash] = disposer;
        if (3 * hashCount >= 2 * hashTable.length)
            rehash();
        hashCount++;
        return node;
    }

    static int test_getHashCount() {
        return hashCount;
    }

    private static void rehash() {
        SelfDisposer[] oldTable = hashTable;
        int oldLength = oldTable.length;
        SelfDisposer[] newTable = new SelfDisposer[2*oldLength];
        hashTable = newTable;
        for (int i = oldLength; --i >= 0; ) {
            for (SelfDisposer disposer = oldTable[i];
                    disposer != null;) {
                SelfDisposer next = disposer.next;
                int hash = hashPeer(disposer.peer);
                disposer.next = newTable[hash];
                newTable[hash] = disposer;
                disposer = next;
            }
        }
    }

    private static final class SelfDisposer extends Disposer.WeakDisposerRecord {
        private final long peer;
        SelfDisposer next;
        SelfDisposer(Object referent, final long _peer) {
            super(referent);
            peer = _peer;
        }

        @Override
        public void dispose() {
            int hash = hashPeer(peer);
            SelfDisposer head = hashTable[hash];
            SelfDisposer prev = null;
            for (SelfDisposer disposer = head; disposer != null;) {
                SelfDisposer next = disposer.next;
                if (disposer.peer == peer) {
                    disposer.clear();
                    if (prev != null)
                        prev.next = next;
                    else
                        hashTable[hash] = next;
                    hashCount--;
                    break;
                }
                prev = disposer;
                disposer = next;
            }
            NodeImpl.dispose(peer);
        }
    }

    NodeImpl(long peer) {
        super(peer, JS_DOM_NODE_OBJECT);
    }

    static Node createInterface(long peer) {
        if (peer == 0L) return null;
        switch (NodeImpl.getNodeTypeImpl(peer)) {
        case ELEMENT_NODE :
               if( !ElementImpl.isHTMLElementImpl(peer))
                   return new ElementImpl(peer);
               else {
                   String tagName = ElementImpl.getTagNameImpl(peer).toUpperCase();
                   if ("A".equals(tagName)) return new HTMLAnchorElementImpl(peer);
                   if ("APPLET".equals(tagName)) return new HTMLAppletElementImpl(peer);
                   if ("AREA".equals(tagName)) return new HTMLAreaElementImpl(peer);
                   if ("BASE".equals(tagName)) return new HTMLBaseElementImpl(peer);
                   if ("BASEFONT".equals(tagName)) return new HTMLBaseFontElementImpl(peer);
                   if ("BODY".equals(tagName)) return new HTMLBodyElementImpl(peer);
                   if ("BR".equals(tagName)) return new HTMLBRElementImpl(peer);
                   if ("BUTTON".equals(tagName)) return new HTMLButtonElementImpl(peer);
                   if ("DIR".equals(tagName)) return new HTMLDirectoryElementImpl(peer);
                   if ("DIV".equals(tagName)) return new HTMLDivElementImpl(peer);
                   if ("DL".equals(tagName)) return new HTMLDListElementImpl(peer);
                   if ("FIELDSET".equals(tagName)) return new HTMLFieldSetElementImpl(peer);
                   if ("FONT".equals(tagName)) return new HTMLFontElementImpl(peer);
                   if ("FORM".equals(tagName)) return new HTMLFormElementImpl(peer);
                   if ("FRAME".equals(tagName)) return new HTMLFrameElementImpl(peer);
                   if ("FRAMESET".equals(tagName)) return new HTMLFrameSetElementImpl(peer);
                   if ("HEAD".equals(tagName)) return new HTMLHeadElementImpl(peer);
                   if (tagName.length() == 2 && tagName.charAt(0)=='H' && tagName.charAt(1) >= '1' && tagName.charAt(1) <= '6') return new HTMLHeadingElementImpl(peer);
                   if ("HR".equals(tagName)) return new HTMLHRElementImpl(peer);
                   if ("IFRAME".equals(tagName)) return new HTMLIFrameElementImpl(peer);
                   if ("IMG".equals(tagName)) return new HTMLImageElementImpl(peer);
                   if ("INPUT".equals(tagName)) return new HTMLInputElementImpl(peer);
                   if ("LABEL".equals(tagName)) return new HTMLLabelElementImpl(peer);
                   if ("LEGEND".equals(tagName)) return new HTMLLegendElementImpl(peer);
                   if ("LI".equals(tagName)) return new HTMLLIElementImpl(peer);
                   if ("LINK".equals(tagName)) return new HTMLLinkElementImpl(peer);
                   if ("MAP".equals(tagName)) return new HTMLMapElementImpl(peer);
                   if ("MENU".equals(tagName)) return new HTMLMenuElementImpl(peer);
                   if ("META".equals(tagName)) return new HTMLMetaElementImpl(peer);
                   if ("INS".equals(tagName) || "DEL".equals(tagName)) return new HTMLModElementImpl(peer);
                   if ("OBJECT".equals(tagName)) return new HTMLObjectElementImpl(peer);
                   if ("OL".equals(tagName)) return new HTMLOListElementImpl(peer);
                   if ("OPTGROUP".equals(tagName)) return new HTMLOptGroupElementImpl(peer);
                   if ("OPTION".equals(tagName)) return new HTMLOptionElementImpl(peer);
                   if ("P".equals(tagName)) return new HTMLParagraphElementImpl(peer);
                   if ("PARAM".equals(tagName)) return new HTMLParamElementImpl(peer);
                   if ("PRE".equals(tagName)) return new HTMLPreElementImpl(peer);
                   if ("Q".equals(tagName)) return new HTMLQuoteElementImpl(peer);
                   if ("SCRIPT".equals(tagName)) return new HTMLScriptElementImpl(peer);
                   if ("SELECT".equals(tagName)) return new HTMLSelectElementImpl(peer);
                   if ("STYLE".equals(tagName)) return new HTMLStyleElementImpl(peer);
                   if ("CAPTION".equals(tagName)) return new HTMLTableCaptionElementImpl(peer);
                   if ("TD".equals(tagName)) return new HTMLTableCellElementImpl(peer);
                   if ("COL".equals(tagName)) return new HTMLTableColElementImpl(peer);
                   if ("TABLE".equals(tagName)) return new HTMLTableElementImpl(peer);
                   if ("TR".equals(tagName)) return new HTMLTableRowElementImpl(peer);
                   if ("THEAD".equals(tagName) || "TFOOT".equals(tagName) || "TBODY".equals(tagName)) return new HTMLTableSectionElementImpl(peer);
                   if ("TEXTAREA".equals(tagName)) return new HTMLTextAreaElementImpl(peer);
                   if ("TITLE".equals(tagName)) return new HTMLTitleElementImpl(peer);
                   if ("UL".equals(tagName)) return new HTMLUListElementImpl(peer);
               }
               return new HTMLElementImpl(peer);
        case ATTRIBUTE_NODE: return new AttrImpl(peer);
        case TEXT_NODE: return new TextImpl(peer);
        case CDATA_SECTION_NODE: return new CDATASectionImpl(peer);
        case ENTITY_REFERENCE_NODE: return new EntityReferenceImpl(peer);
        case ENTITY_NODE: return new EntityImpl(peer);
        case PROCESSING_INSTRUCTION_NODE: return new ProcessingInstructionImpl(peer);
        case COMMENT_NODE: return new CommentImpl(peer);
        case DOCUMENT_NODE:
               if( DocumentImpl.isHTMLDocumentImpl(peer))
                   return new HTMLDocumentImpl(peer);
               return new DocumentImpl(peer);
        case DOCUMENT_TYPE_NODE: return new DocumentTypeImpl(peer);
        case DOCUMENT_FRAGMENT_NODE: return new DocumentFragmentImpl(peer);
        }
        return new NodeImpl(peer);
    }

    static Node create(long peer) {
        return getCachedImpl(peer);
    }

    static long getPeer(Node arg) {
        return (arg == null) ? 0L : ((NodeImpl)arg).getPeer();
    }

    private static void dispose(long peer) {
        NodeNative.dispose(peer);
    }

    static Node getImpl(long peer) {
        return (Node)create(peer);
    }


// Constants
    public static final int ELEMENT_NODE = 1;
    public static final int ATTRIBUTE_NODE = 2;
    public static final int TEXT_NODE = 3;
    public static final int CDATA_SECTION_NODE = 4;
    public static final int ENTITY_REFERENCE_NODE = 5;
    public static final int ENTITY_NODE = 6;
    public static final int PROCESSING_INSTRUCTION_NODE = 7;
    public static final int COMMENT_NODE = 8;
    public static final int DOCUMENT_NODE = 9;
    public static final int DOCUMENT_TYPE_NODE = 10;
    public static final int DOCUMENT_FRAGMENT_NODE = 11;
    public static final int NOTATION_NODE = 12;
    public static final int DOCUMENT_POSITION_DISCONNECTED = 0x01;
    public static final int DOCUMENT_POSITION_PRECEDING = 0x02;
    public static final int DOCUMENT_POSITION_FOLLOWING = 0x04;
    public static final int DOCUMENT_POSITION_CONTAINS = 0x08;
    public static final int DOCUMENT_POSITION_CONTAINED_BY = 0x10;
    public static final int DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

// Attributes
    @Override
    public String getNodeName() {
        return getNodeNameImpl(getPeer());
    }
    static String getNodeNameImpl(long peer) {
        return NodeNative.getNodeName(peer);
    }

    @Override
    public String getNodeValue() {
        return getNodeValueImpl(getPeer());
    }
    static String getNodeValueImpl(long peer) {
        return NodeNative.getNodeValue(peer);
    }

    @Override
    public void setNodeValue(String value) throws DOMException {
        setNodeValueImpl(getPeer(), value);
    }
    static void setNodeValueImpl(long peer, String value) {
        NodeNative.setNodeValue(peer, value);
    }

    @Override
    public short getNodeType() {
        return getNodeTypeImpl(getPeer());
    }
    static short getNodeTypeImpl(long peer) {
        return NodeNative.getNodeType(peer);
    }

    @Override
    public Node getParentNode() {
        return NodeImpl.getImpl(getParentNodeImpl(getPeer()));
    }
    static long getParentNodeImpl(long peer) {
        return NodeNative.getParentNode(peer);
    }

    @Override
    public NodeList getChildNodes() {
        return NodeListImpl.getImpl(getChildNodesImpl(getPeer()));
    }
    static long getChildNodesImpl(long peer) {
        return NodeNative.getChildNodes(peer);
    }

    @Override
    public Node getFirstChild() {
        return NodeImpl.getImpl(getFirstChildImpl(getPeer()));
    }
    static long getFirstChildImpl(long peer) {
        return NodeNative.getFirstChild(peer);
    }

    @Override
    public Node getLastChild() {
        return NodeImpl.getImpl(getLastChildImpl(getPeer()));
    }
    static long getLastChildImpl(long peer) {
        return NodeNative.getLastChild(peer);
    }

    @Override
    public Node getPreviousSibling() {
        return NodeImpl.getImpl(getPreviousSiblingImpl(getPeer()));
    }
    static long getPreviousSiblingImpl(long peer) {
        return NodeNative.getPreviousSibling(peer);
    }

    @Override
    public Node getNextSibling() {
        return NodeImpl.getImpl(getNextSiblingImpl(getPeer()));
    }
    static long getNextSiblingImpl(long peer) {
        return NodeNative.getNextSibling(peer);
    }

    @Override
    public Document getOwnerDocument() {
        return DocumentImpl.getImpl(getOwnerDocumentImpl(getPeer()));
    }
    static long getOwnerDocumentImpl(long peer) {
        return NodeNative.getOwnerDocument(peer);
    }

    @Override
    public String getNamespaceURI() {
        return getNamespaceURIImpl(getPeer());
    }
    static String getNamespaceURIImpl(long peer) {
        return NodeNative.getNamespaceURI(peer);
    }

    @Override
    public String getPrefix() {
        return getPrefixImpl(getPeer());
    }
    static String getPrefixImpl(long peer) {
        return NodeNative.getPrefix(peer);
    }

    @Override
    public void setPrefix(String value) throws DOMException {
        setPrefixImpl(getPeer(), value);
    }
    static void setPrefixImpl(long peer, String value) {
        NodeNative.setPrefix(peer, value);
    }

    @Override
    public String getLocalName() {
        return getLocalNameImpl(getPeer());
    }
    static String getLocalNameImpl(long peer) {
        return NodeNative.getLocalName(peer);
    }

    @Override
    public NamedNodeMap getAttributes() {
        return NamedNodeMapImpl.getImpl(getAttributesImpl(getPeer()));
    }
    static long getAttributesImpl(long peer) {
        return NodeNative.getAttributes(peer);
    }

    @Override
    public String getBaseURI() {
        return getBaseURIImpl(getPeer());
    }
    static String getBaseURIImpl(long peer) {
        return NodeNative.getBaseURI(peer);
    }

    @Override
    public String getTextContent() {
        return getTextContentImpl(getPeer());
    }
    static String getTextContentImpl(long peer) {
        return NodeNative.getTextContent(peer);
    }

    @Override
    public void setTextContent(String value) throws DOMException {
        setTextContentImpl(getPeer(), value);
    }
    static void setTextContentImpl(long peer, String value) {
        NodeNative.setTextContent(peer, value);
    }

    public Element getParentElement() {
        return ElementImpl.getImpl(getParentElementImpl(getPeer()));
    }
    static long getParentElementImpl(long peer) {
        return NodeNative.getParentElement(peer);
    }


// Functions
    @Override
    public Node insertBefore(Node newChild
        , Node refChild) throws DOMException
    {
        return NodeImpl.getImpl(insertBeforeImpl(getPeer()
            , NodeImpl.getPeer(newChild)
            , NodeImpl.getPeer(refChild)));
    }
    static long insertBeforeImpl(long peer
        , long newChild
        , long refChild) {
        return NodeNative.insertBefore(peer, newChild, refChild);
    }


    @Override
    public Node replaceChild(Node newChild
        , Node oldChild) throws DOMException
    {
        return NodeImpl.getImpl(replaceChildImpl(getPeer()
            , NodeImpl.getPeer(newChild)
            , NodeImpl.getPeer(oldChild)));
    }
    static long replaceChildImpl(long peer
        , long newChild
        , long oldChild) {
        return NodeNative.replaceChild(peer, newChild, oldChild);
    }


    @Override
    public Node removeChild(Node oldChild) throws DOMException
    {
        return NodeImpl.getImpl(removeChildImpl(getPeer()
            , NodeImpl.getPeer(oldChild)));
    }
    static long removeChildImpl(long peer
        , long oldChild) {
        return NodeNative.removeChild(peer, oldChild);
    }


    @Override
    public Node appendChild(Node newChild) throws DOMException
    {
        return NodeImpl.getImpl(appendChildImpl(getPeer()
            , NodeImpl.getPeer(newChild)));
    }
    static long appendChildImpl(long peer
        , long newChild) {
        return NodeNative.appendChild(peer, newChild);
    }


    @Override
    public boolean hasChildNodes()
    {
        return hasChildNodesImpl(getPeer());
    }
    static boolean hasChildNodesImpl(long peer) {
        return NodeNative.hasChildNodes(peer);
    }


    @Override
    public Node cloneNode(boolean deep) throws DOMException
    {
        return NodeImpl.getImpl(cloneNodeImpl(getPeer()
            , deep));
    }
    static long cloneNodeImpl(long peer
        , boolean deep) {
        return NodeNative.cloneNode(peer, deep);
    }


    @Override
    public void normalize()
    {
        normalizeImpl(getPeer());
    }
    static void normalizeImpl(long peer) {
        NodeNative.normalize(peer);
    }


    @Override
    public boolean isSupported(String feature
        , String version)
    {
        return isSupportedImpl(getPeer()
            , feature
            , version);
    }
    static boolean isSupportedImpl(long peer
        , String feature
        , String version) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.NodeImpl.isSupportedImpl: no wkj_* function exists for it in"
                + " any jfxwebkit build");
    }


    @Override
    public boolean hasAttributes()
    {
        return hasAttributesImpl(getPeer());
    }
    static boolean hasAttributesImpl(long peer) {
        return NodeNative.hasAttributes(peer);
    }


    @Override
    public boolean isSameNode(Node other)
    {
        return isSameNodeImpl(getPeer()
            , NodeImpl.getPeer(other));
    }
    static boolean isSameNodeImpl(long peer
        , long other) {
        return NodeNative.isSameNode(peer, other);
    }


    @Override
    public boolean isEqualNode(Node other)
    {
        return isEqualNodeImpl(getPeer()
            , NodeImpl.getPeer(other));
    }
    static boolean isEqualNodeImpl(long peer
        , long other) {
        return NodeNative.isEqualNode(peer, other);
    }


    @Override
    public String lookupPrefix(String namespaceURI)
    {
        return lookupPrefixImpl(getPeer()
            , namespaceURI);
    }
    static String lookupPrefixImpl(long peer
        , String namespaceURI) {
        return NodeNative.lookupPrefix(peer, namespaceURI);
    }


    @Override
    public boolean isDefaultNamespace(String namespaceURI)
    {
        return isDefaultNamespaceImpl(getPeer()
            , namespaceURI);
    }
    static boolean isDefaultNamespaceImpl(long peer
        , String namespaceURI) {
        return NodeNative.isDefaultNamespace(peer, namespaceURI);
    }


    @Override
    public String lookupNamespaceURI(String prefix)
    {
        return lookupNamespaceURIImpl(getPeer()
            , prefix);
    }
    static String lookupNamespaceURIImpl(long peer
        , String prefix) {
        return NodeNative.lookupNamespaceURI(peer, prefix);
    }


    @Override
    public short compareDocumentPosition(Node other)
    {
        return compareDocumentPositionImpl(getPeer()
            , NodeImpl.getPeer(other));
    }
    static short compareDocumentPositionImpl(long peer
        , long other) {
        return NodeNative.compareDocumentPosition(peer, other);
    }


    public boolean contains(Node other)
    {
        return containsImpl(getPeer()
            , NodeImpl.getPeer(other));
    }
    static boolean containsImpl(long peer
        , long other) {
        return NodeNative.contains(peer, other);
    }


    @Override
    public void addEventListener(String type
        , EventListener listener
        , boolean useCapture)
    {
        addEventListenerImpl(getPeer()
            , type
            , EventListenerImpl.getPeer(listener)
            , useCapture);
    }
    static void addEventListenerImpl(long peer
        , String type
        , long listener
        , boolean useCapture) {
        NodeNative.addEventListener(peer, type, listener, useCapture);
    }


    @Override
    public void removeEventListener(String type
        , EventListener listener
        , boolean useCapture)
    {
        removeEventListenerImpl(getPeer()
            , type
            , EventListenerImpl.getPeer(listener)
            , useCapture);
    }
    static void removeEventListenerImpl(long peer
        , String type
        , long listener
        , boolean useCapture) {
        NodeNative.removeEventListener(peer, type, listener, useCapture);
    }


    @Override
    public boolean dispatchEvent(Event event) throws DOMException
    {
        return dispatchEventImpl(getPeer()
            , EventImpl.getPeer(event));
    }
    static boolean dispatchEventImpl(long peer
        , long event) {
        return NodeNative.dispatchEvent(peer, event);
    }



//stubs
    @Override
    public Object getUserData(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getFeature(String feature, String version) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

