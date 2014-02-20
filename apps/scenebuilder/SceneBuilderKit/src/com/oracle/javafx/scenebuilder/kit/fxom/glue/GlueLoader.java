/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.fxom.glue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * 
 */
class GlueLoader implements ContentHandler, ErrorHandler, LexicalHandler {
    
    
    private final GlueDocument document;
    private GlueElement currentElement;
    private int currentElementDepth = -1;
    private final List<GlueAuxiliary> auxiliaries = new ArrayList<>();
    private final Map<String, String> prefixMappings = new HashMap<>();

    public GlueLoader(GlueDocument document) {
        this.document = document;
    }
    
    public void load(String xmlText) throws IOException {
        assert xmlText != null;
        assert GlueDocument.isEmptyXmlText(xmlText) == false;
        
        final Charset utf8 = Charset.forName("UTF-8"); //NOI18N
        try (final InputStream is = new ByteArrayInputStream(xmlText.getBytes(utf8))) {
            load(is);
        }
    }
    
    public void load(InputStream is) throws IOException {
        assert currentElement == null;
        assert currentElementDepth == -1;
        assert auxiliaries.isEmpty();
        assert prefixMappings.isEmpty();
        
        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            xr.setProperty("http://xml.org/sax/properties/lexical-handler", this); //NOI18N
            xr.parse(new InputSource(is));
        } catch(SAXException x) {
            throw new IOException(x);
        }
        
        assert currentElement == null;
        assert currentElementDepth == -1;
        assert auxiliaries.isEmpty();
        assert prefixMappings.isEmpty();
    }

    /*
     * ContentHandler
     */
    
    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
        assert currentElement == null;
        assert currentElementDepth == -1;
        assert auxiliaries.isEmpty();
    }

    @Override
    public void endDocument() throws SAXException {
        assert document != null;
        assert currentElement == null;
        assert currentElementDepth == -1;
        assert auxiliaries.isEmpty();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        prefixMappings.put(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        assert prefixMappings.isEmpty();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        
        // Creates a new glue element and:
        // - puts atts content in GlueElement.attributes map
        // - puts prefixMappings content in GlueElement.attributes map
        // - puts this.auxiliaries content in GlueElement.front
        
        currentElementDepth++;
        final GlueElement newElement = new GlueElement(document, qName, currentElementDepth, false /* preset */);
        final Map<String, String> attributes = newElement.getAttributes();
        for (int i = 0, count = atts.getLength(); i < count; i++) {
            attributes.put(atts.getQName(i), atts.getValue(i));
        }
        for (Map.Entry<String,String> e : prefixMappings.entrySet()) {
            if (e.getKey().isEmpty()) {
                newElement.getAttributes().put("xmlns", e.getValue()); //NOI18N
            } else {
                newElement.getAttributes().put("xmlns:" + e.getKey(), e.getValue()); //NOI18N
            }
        }
        newElement.getFront().addAll(auxiliaries);
        
        if (currentElement == null) {
            // newElement is the root element
            assert currentElementDepth == 0;
            document.setRootElement(newElement);
        } else {
            newElement.addToParent(currentElement);
        }
        
        currentElement = newElement;
        auxiliaries.clear();
        prefixMappings.clear();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        assert currentElement != null;
        assert currentElement.getTagName().equals(qName);
        assert currentElementDepth >= 0;
        
        if (currentElement.getChildren().isEmpty()) {
            currentElement.getContent().addAll(auxiliaries);
        } else {
            currentElement.getTail().addAll(auxiliaries);
        }
        
        currentElement = currentElement.getParent();
        currentElementDepth--;
        auxiliaries.clear();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        final String data = new String(ch, start, length);
        final GlueAuxiliary auxiliary = new GlueCharacters(document, GlueCharacters.Type.TEXT, data);
        
        if (currentElement == null) {
            document.getHeader().add(auxiliary);
        } else {
            auxiliaries.add(auxiliary);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        characters(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        assert currentElement == null;
        assert currentElementDepth == -1;
        document.getHeader().add(new GlueInstruction(document, target, data));
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        throw new UnsupportedOperationException("name=" + name); //NOI18N
    }
    
    /*
     * ErrorHandler
     */
    
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }
    
    
    /*
     * LexicalHandler
     */
    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        throw new UnsupportedOperationException("name=" + name  //NOI18N
                + ", publicId=" + publicId //NOI18N
                + ", systemId=" + systemId); //NOI18N
    }

    @Override
    public void endDTD() throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startEntity(String name) throws SAXException {
        throw new UnsupportedOperationException("name=" + name); //NOI18N
    }

    @Override
    public void endEntity(String name) throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startCDATA() throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endCDATA() throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        final String data = new String(ch, start, length);
        final GlueAuxiliary auxiliary = new GlueCharacters(document, GlueCharacters.Type.COMMENT, data);
        
        if (currentElement == null) {
            document.getHeader().add(auxiliary);
        } else {
            auxiliaries.add(auxiliary);
        }
    }
}
