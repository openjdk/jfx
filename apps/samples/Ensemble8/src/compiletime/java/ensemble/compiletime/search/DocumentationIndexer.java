/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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
package ensemble.compiletime.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class DocumentationIndexer {
    static {
        System.setProperty("java.net.useSystemProxies", "true");
    }

    private enum State {DEFAULT, BOOK_TITLE, CHAPTER, SECT1, SECT_H1_H2};

    public static class Section {
        public final String name;
        public final String content;
        public final String url;

        public Section(String name, String content, String url) {
            this.name = name;
            this.content = content;
            this.url = url;
        }

        @Override public String toString() {
            return "Section{" + "name=" + name + ", content=" + content + '}';
        }
    }

    public static class DocPage {
        public final String bookTitle;
        public final String chapter;
        public final String nextUrl;
        public final List<Section> sections;

        public DocPage(String bookTitle, String chapter, String nextUrl, List<Section> sections) {
            this.bookTitle = bookTitle;
            this.chapter = chapter;
            this.nextUrl = nextUrl;
            this.sections = sections;
        }
    }

    private static int tmpIndex = 0;

    private static final String[][] REPLACEMENTS = {

        // Remove any comments
        { "(?s)<!--.*?-->", "" },

        // Remove scripts and styles
        { "(?s)<(script|style).*?</\\1>", "" },

        // Remove unnecessary tags
        { "(?i)</?(?!html\\b|div\\b|h1\\b|h2\\b|a\\b|img\\b)(\\w+\\b)[^>]*>", "" },

        // Remove malformed garbage from links
        { "(?x) <a (?:\\s+ (?: (href \\s* = \\s* \\\"[^\\\"]*\\\") "
            + "| (name \\s* = \\s* \\\"[^\\\"]*\\\") "
            + "| \\w+\\s*=\\s*\\\"[^\\\"]*\\\" "
            + "| \\w+\\s*=\\s*[^\\s\\\">]+))* \\s* >", "<a $1 $2>" },

//        { "(?i)</?[a-z]+\b(<!div|h1|h2|a|img)[^>]*>", "" }, // Remove unnecessary tags
//        { "</?(?:p|br)[^>]*>", "" }, // Remove unnecessary tags
//        { "<meta [^>]+>", "" },
//        { "[ \\t\\x0B\\f\\r]+", " " },
//        { "[ \\t\\x0B\\f\\r\\n]+", "\\n" },
//        // fix links like <a href=init-window-big.gif> to <a href="init-window-big.gif">
//        { "<a href=([^\">]+)>", "<a href=\"$1\">" },
//        { "type=\"text/css\" media=\"screen\"", "" },
//        { "<input [^>]+>", "" },
//        { "target=_top", "" },
//        { "xWebsiteObjectType <Matches> `Data File`", "" },
//        { "<hr>", "<hr/>" },
//        { "&", "&amp;" },
//        { "<span class=red>", "<span class=\"red\">" },
    };
    private static final Pattern[] COMPILED_PATTERNS = new Pattern[REPLACEMENTS.length];

    static {
        for (int i = 0; i < REPLACEMENTS.length; i++) {
            COMPILED_PATTERNS[i] = Pattern.compile(REPLACEMENTS[i][0]);
        }
    }

    public static DocPage parseDocsPage(final String url, String content) throws Exception {

        for (int i = 0; i < REPLACEMENTS.length; i++) {
            content = COMPILED_PATTERNS[i].matcher(content).replaceAll(REPLACEMENTS[i][1]);
        }
        try {
            DocHandler handler = new DocHandler(url);
            XMLReader xmlParser = XMLReaderFactory.createXMLReader();
            xmlParser.setContentHandler(handler);
            xmlParser.setEntityResolver(handler);
            xmlParser.parse(new InputSource(new StringReader(content)));
            return handler.getDocPage();
        } catch (SAXException | IOException e) {
            String filename = "tmp" + tmpIndex++ + ".txt";
            Files.write(new File(filename).toPath(), FXCollections.observableArrayList(content));
            throw new RuntimeException("\"Failed to parse '" + url + "', see content in " + filename + ".", e);
        }
    }

    public static void main(String[] args) throws Exception {
//        final String url = "https://docs.oracle.com/javafx/2/layout/builtin_layouts.htm#sthref15";
        final String url = "https://docs.oracle.com/javafx/2/overview/jfxpub-overview.htm";
        parseDocsPage(url, BuildEnsembleSearchIndex.grabWebPage(url).toString());
    }

    private static class DocHandler extends DefaultHandler {
        private final String url;
        private State state = State.DEFAULT;
        private int divDepth = 0;
        private StringBuilder buf = new StringBuilder();
        private String bookTitle;
        private String chapter;
        private String sectName;
        private String sectContent;
        private String sectUrl;
        private String currentLink;
        private String currentLinkName;
        private String nextUrl;
        private List<Section> sections = new ArrayList<>();
        private DocPage docPage;

        public DocHandler(String url) {
            this.url = url;
        }

        public DocPage getDocPage() {
            return docPage;
        }

        @Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//                System.out.println("<<<<<  localName = " + localName+"  qName = " + qName+" ["+state+"]");
            switch(state) {
                case DEFAULT:
                    if ("div".equals(localName)) {
                        String id = attributes.getValue("id");
                        String classSt = attributes.getValue("class");
                        if ("bookTitle".equals(id)) {
                            state = State.BOOK_TITLE;
                            buf.setLength(0);
                        } else if ("sect1".equals(classSt) || "refsect1".equals(classSt)) {
                            state = State.SECT1;
                            buf.setLength(0);
                            divDepth = 0;
                        }
                    } else if ("h1".equals(localName)) {
                        String classSt = attributes.getValue("class");

                        if ("chapter".equals(classSt)) {
                            state = State.CHAPTER;
                            buf.setLength(0);
                        }
                    } else if ("a".equals(localName)) {
                        currentLink = attributes.getValue("href");
                        currentLinkName = attributes.getValue("name");
                    } else if ("img".equals(localName) && "Next".equals(attributes.getValue("alt"))) {
                        nextUrl = url.substring(0,url.lastIndexOf('/')+1) + currentLink;
                    }
                    break;
                case SECT1:
                    if ("div".equals(localName)) {
                        divDepth ++;
                    } else if ("h1".equals(localName) || "h2".equals(localName)) {
                        state = State.SECT_H1_H2;
                        buf.setLength(0);
                    }
                    break;
            }
        }

        @Override public void endElement(String uri, String localName, String qName) throws SAXException {
//                System.out.println(">>>>  localName = " + localName+"  qName = " + qName+" ["+state+"]");
            switch(state) {
                case SECT1:
                    if ("div".equals(localName)) {
                        if (divDepth == 0) {
                            sectContent = buf.toString().trim();
                            final int hashIndex = url.indexOf('#');
                            final String sectionUrl = (hashIndex == -1 ? url : url.substring(0,hashIndex)) + "#" + currentLinkName;
                            sections.add(new Section(sectName, sectContent, sectionUrl));
                            state = State.DEFAULT;
                        } else {
                            divDepth --;
                        }
                    }
                    break;
                case SECT_H1_H2:
                    if ("h1".equals(localName) || "h2".equals(localName)) {
                        state = State.SECT1;
                        sectName = buf.toString().trim();
                        buf.setLength(0);
                    }
                case BOOK_TITLE:
                    if ("div".equals(localName)) {
                        bookTitle = buf.toString().trim();
                        state = State.DEFAULT;
                    }
                    break;
                case CHAPTER:
                    if ("h1".equals(localName)) {
                        chapter = buf.toString().trim();
                        state = State.DEFAULT;
                    }
                    break;
            }
        }

        @Override public void endDocument() throws SAXException {
            docPage = new DocPage(bookTitle, chapter, nextUrl, sections);
        }

        @Override public void characters(char[] ch, int start, int length) throws SAXException {
            switch(state) {
                case BOOK_TITLE:
                case CHAPTER:
                case SECT1:
                case SECT_H1_H2:
                    buf.append(ch,start,length);
                    break;
            }
        }

        // AVOID LONG WAITS FOR
        @Override public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            return new InputSource(new StringReader(""));
        }
    }
}
