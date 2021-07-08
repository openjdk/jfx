/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.oracle.javafx.fxml.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class TestLoadPerformance extends Application {
    private static class SAXHandler extends DefaultHandler {
        @Override
        public void processingInstruction(String target, String data) {
            target.trim();
        }

        @Override
        public void startElement(String uri, String name, String qName, Attributes atts) {
            for (int i = 0, n = atts.getLength(); i < n; i++) {
                atts.getLocalName(i);
                atts.getQName(i);
                atts.getValue(i);
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            // No-op
        }

        @Override
        public void characters (char ch[], int start, int length) {
            // No-op
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String[] files = new String[] {
            "project-with-all-p2-components.fxml",
            "stageview-drop-test.fxml",
            "svg-complex-gear-flowers.fxml",
            "svg-complex-tiger.fxml",
            "svg-complex-tux.fxml",
            "svg-complex-unhappy-sheep.fxml",
            "svg-svgspec-paints-radialGradient.fxml"
        };

        for (int i = 0; i < files.length; i++) {
            String file = files[i];
            URL location = TestLoadPerformance.class.getResource(file);
            URLConnection connection = location.openConnection();
            int size = connection.getContentLength();
            System.out.printf("%s (%dKB)\n", file, size / 1000);
            // loadSAX(location);
            // loadStAX(location);
            loadFXML(TestLoadPerformance.class, file);

            System.out.println();
        }

        System.exit(0);
    }

    @SuppressWarnings("deprecation")
    protected void loadSAX(URL location) throws Exception {
        long t0 = System.currentTimeMillis();

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();

        SAXHandler handler = new SAXHandler();
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);

        InputStream inputStream = location.openStream();
        xmlReader.parse(new InputSource(inputStream));

        long t1 = System.currentTimeMillis();
        System.out.printf("SAX: %dms\n", t1 - t0);
    }

    protected void loadStAX(URL location) throws Exception {
        long t0 = System.currentTimeMillis();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", true);

        InputStream inputStream = location.openStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        XMLStreamReader xmlStreamReader = new StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(inputStreamReader)) {
            @Override
            public String getPrefix() {
                String prefix = super.getPrefix();

                if (prefix != null && prefix.length() == 0) {
                    prefix = null;
                }

                return prefix;
            }

            @Override
            public String getAttributePrefix(int index) {
                String attributePrefix = super.getAttributePrefix(index);

                if (attributePrefix != null && attributePrefix.length() == 0) {
                    attributePrefix = null;
                }

                return attributePrefix;
            }
        };

        // Parse the XML stream
        while (xmlStreamReader.hasNext()) {
            int event = xmlStreamReader.next();

            switch (event) {
                case XMLStreamConstants.PROCESSING_INSTRUCTION: {
                    xmlStreamReader.getPITarget().trim();
                    xmlStreamReader.getPIData();
                    break;
                }

                case XMLStreamConstants.COMMENT: {
                    xmlStreamReader.getText();
                    break;
                }

                case XMLStreamConstants.START_ELEMENT: {
                    xmlStreamReader.getPrefix();
                    xmlStreamReader.getLocalName();

                    for (int i = 0, n = xmlStreamReader.getAttributeCount(); i < n; i++) {
                        xmlStreamReader.getAttributePrefix(i);
                        xmlStreamReader.getAttributeLocalName(i);
                        xmlStreamReader.getAttributeValue(i);
                    }

                    break;
                }

                case XMLStreamConstants.END_ELEMENT: {
                    break;
                }

                case XMLStreamConstants.CHARACTERS: {
                    break;
                }
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.printf("StAX: %dms\n", t1 - t0);
    }

    protected void loadFXML(Class<?> type, String name) throws Exception {
        long t0 = System.currentTimeMillis();
        FXMLLoader fxmlLoader = new FXMLLoader(type.getResource(name));
        fxmlLoader.load();

        long t1 = System.currentTimeMillis();
        System.out.printf("FXML: %dms\n", t1 - t0);
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
