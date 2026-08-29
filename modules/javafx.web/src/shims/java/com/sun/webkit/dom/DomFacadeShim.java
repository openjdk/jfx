/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Reaches the generated {@code *Native} DOM facades, which are package private, from the binding
 * tests. Nothing here does any marshalling of its own: each method is one call to the facade the
 * production {@code *Impl} class calls, so that a test exercises exactly the code path a DOM
 * operation takes and not a re-implementation of it.
 * <p>
 * One method per shape of the DOM ABI, chosen so that every code path the facade generator emits is
 * executed at least once (see {@code FFM-TEST-PLAN.md} section 4.11).
 */
public final class DomFacadeShim {

    /** The symbol {@link #attrGetName} calls, for programming returns and arming exceptions. */
    public static final String ATTR_GET_NAME = "wkj_dom_Attr_getName";

    /** The symbol {@link #attrSetValue} calls. Its spec row is not marked THROWS. */
    public static final String ATTR_SET_VALUE = "wkj_dom_Attr_setValue";

    /** The symbol {@link #attrGetSpecified} calls: the cheapest recordable call in the ABI. */
    public static final String ATTR_GET_SPECIFIED = "wkj_dom_Attr_getSpecified";

    /** The symbol {@link #attrGetOwnerElement} calls: {@code int64_t (int64_t)}. */
    public static final String ATTR_GET_OWNER_ELEMENT = "wkj_dom_Attr_getOwnerElement";

    /** The symbol {@link #nodeGetNodeType} calls: {@code int16_t (int64_t)}. */
    public static final String NODE_GET_NODE_TYPE = "wkj_dom_Node_getNodeType";

    /** The symbol {@link #nodeDispose} calls: {@code void (int64_t)}. */
    public static final String NODE_DISPOSE = "wkj_dom_Node_dispose";

    /** The symbol {@link #nodeGetTextContent} calls: a string getter that overflows in practice. */
    public static final String NODE_GET_TEXT_CONTENT = "wkj_dom_Node_getTextContent";

    /** The symbol {@link #elementSetAttribute} calls: a THROWS function returning {@code void}. */
    public static final String ELEMENT_SET_ATTRIBUTE = "wkj_dom_Element_setAttribute";

    /** The symbol {@link #cssPrimitiveValueGetFloatValue} calls: the ABI's only {@code float}. */
    public static final String CSS_GET_FLOAT_VALUE = "wkj_dom_CSSPrimitiveValue_getFloatValue";

    /** The symbol {@link #xpathResultGetNumberValue} calls: a THROWS {@code double}. */
    public static final String XPATH_GET_NUMBER_VALUE = "wkj_dom_XPathResult_getNumberValue";

    /** The symbol {@link #domImplementationCreateDocumentType} calls: three string pairs. */
    public static final String CREATE_DOCUMENT_TYPE = "wkj_dom_DOMImplementation_createDocumentType";

    private DomFacadeShim() {
    }

    /**
     * A string return, through the caller's buffer.
     *
     * @param peer the peer
     * @return the name, or {@code null}
     */
    public static String attrGetName(long peer) {
        return AttrNative.getName(peer);
    }

    /**
     * A second string return, so that a test can program two symbols independently.
     *
     * @param peer the peer
     * @return the value, or {@code null}
     */
    public static String attrGetValue(long peer) {
        return AttrNative.getValue(peer);
    }

    /**
     * A string argument, as {@code const uint16_t*, int32_t}.
     *
     * @param peer the peer
     * @param value the value, may be {@code null}
     */
    public static void attrSetValue(long peer, String value) {
        AttrNative.setValue(peer, value);
    }

    /**
     * An {@code int32_t} return decoded as a Java boolean.
     *
     * @param peer the peer
     * @return the flag
     */
    public static boolean attrGetSpecified(long peer) {
        return AttrNative.getSpecified(peer);
    }

    /**
     * An {@code int64_t} peer return.
     *
     * @param peer the peer
     * @return the owner element peer
     */
    public static long attrGetOwnerElement(long peer) {
        return AttrNative.getOwnerElement(peer);
    }

    /**
     * An {@code int16_t} return.
     *
     * @param peer the peer
     * @return the node type
     */
    public static short nodeGetNodeType(long peer) {
        return NodeNative.getNodeType(peer);
    }

    /**
     * A {@code void (int64_t)} call.
     *
     * @param peer the peer
     */
    public static void nodeDispose(long peer) {
        NodeNative.dispose(peer);
    }

    /**
     * A string return of the shape that overflows the facade's initial buffer in real use.
     *
     * @param peer the peer
     * @return the text content, or {@code null}
     */
    public static String nodeGetTextContent(long peer) {
        return NodeNative.getTextContent(peer);
    }

    /**
     * A THROWS function returning {@code void}: the shape where a missed exception check would both
     * swallow the exception and leave the slot dirty (contract section 13.1, finding 12).
     *
     * @param peer the peer
     * @param name the attribute name
     * @param value the attribute value
     */
    public static void elementSetAttribute(long peer, String name, String value) {
        ElementNative.setAttribute(peer, name, value);
    }

    /**
     * The ABI's only {@code float} return, and a THROWS one.
     *
     * @param peer the peer
     * @param unitType the CSS unit type
     * @return the value
     */
    public static float cssPrimitiveValueGetFloatValue(long peer, short unitType) {
        return CSSPrimitiveValueNative.getFloatValue(peer, unitType);
    }

    /**
     * A {@code double} return, and a THROWS one.
     *
     * @param peer the peer
     * @return the value
     */
    public static double xpathResultGetNumberValue(long peer) {
        return XPathResultNative.getNumberValue(peer);
    }

    /**
     * Three string arguments in one call, which is where an off by one in the argument order would
     * show up.
     *
     * @param peer the peer
     * @param qualifiedName the qualified name
     * @param publicId the public id
     * @param systemId the system id
     * @return the created peer
     */
    public static long domImplementationCreateDocumentType(long peer, String qualifiedName,
                                                           String publicId, String systemId) {
        return DOMImplementationNative.createDocumentType(peer, qualifiedName, publicId, systemId);
    }

    /**
     * Returns the initial buffer capacity, in UTF-16 code units, that a string getter offers before
     * it has to grow and retry. A test needs it to build a value that is certain to overflow.
     *
     * @return {@code DOMStringCodec.CAPACITY}
     */
    public static int stringCapacity() {
        return DOMStringCodec.CAPACITY;
    }
}
