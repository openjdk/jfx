/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates.
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

import ensemble.compiletime.Sample;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * Generate the lucene index that Ensemble uses for its search
 */
public class BuildEnsembleSearchIndex {

    public static void buildSearchIndex(List<Sample> allSamples, String javaDocBaseUrl, String javafxDocumentationHome, File indexDir){
        try {
            List<Document> docs = new ArrayList<>();
            List<Callable<List<Document>>> tasks = new ArrayList<>();
            // create callables to collect data
            System.out.println("Creating Documents for Samples...");
            docs.addAll(indexSamples(allSamples));
            System.out.println("Creating tasks for getting all documentation...");
            System.out.println("javaDocBaseUrl = " + javaDocBaseUrl);
            System.out.println("javafxDocumentationHome = " + javafxDocumentationHome);
            tasks.addAll(indexJavaDocAllClasses(javaDocBaseUrl));
            tasks.addAll(indexAllDocumentation(javafxDocumentationHome));
            // execute all the tasks in 32 threads, collecting all the documents to write
            System.out.println("Executing tasks getting all documentation...");
            try {
                ThreadPoolExecutor executor = new ThreadPoolExecutor(32,32,30, TimeUnit.SECONDS,new LinkedBlockingQueue());
                executor.setThreadFactory(new ThreadFactory() {
                    int index = 0;
                    @Override public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r,"Thread-"+(++index));
                        thread.setDaemon(true);
                        return thread;
                    }
                });
                List<Future<List<Document>>> results = executor.invokeAll(tasks);
                for(Future<List<Document>> future : results) {
                    docs.addAll(future.get());
                }
            } catch (ExecutionException | InterruptedException ex) {
                Logger.getLogger(BuildEnsembleSearchIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
            // create index
            System.out.println("Indexing to directory '" + indexDir + "'...");
            Directory dir = FSDirectory.open(indexDir.toPath());
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                // write all docs
                System.out.println("Writing ["+docs.size()+"] documents to index....");
                writer.addDocuments(docs);
                System.out.println("NUMBER OF INDEXED DOCUMENTS = ["+writer.numDocs()+"]");
            }
            // write file listing all the search index files, so we know what
            // is in the jar file at runtime
            try (FileWriter listAllOut = new FileWriter(new File(indexDir,"listAll.txt"))) {
                for (String fileName: dir.listAll()) {
                    // don't include the "listAll.txt" file or "write.lock"
                    if (!"listAll.txt".equals(fileName) && !"write.lock".equals(fileName)) {
                        Long length = dir.fileLength(fileName);
                        listAllOut.write(fileName);
                        listAllOut.write(':');
                        listAllOut.write(length.toString());
                        listAllOut.write('\n');
                    }
                }
                listAllOut.flush();
            }
            System.out.println("Finished writing search index to directory '" + indexDir);
        } catch (IOException ex) {
            Logger.getLogger(BuildEnsembleSearchIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static List<Callable<List<Document>>> indexAllDocumentation(String javafxDocumentationHome) throws IOException{
        List<Callable<List<Document>>> tasks = new ArrayList<>();
        CharSequence content = grabWebPage(javafxDocumentationHome);
        String baseUrl = javafxDocumentationHome.substring(0,javafxDocumentationHome.lastIndexOf('/')+1);
//        System.out.println("baseUrl = " + baseUrl);
        // parse page finding all docs pages
        Matcher matcher = docsHomeLink.matcher(content);
        System.out.println("Building a list of documentation to index");
        while (matcher.find()) {
            String foundUrl = matcher.group(1);
//            System.out.println("foundUrl = " + foundUrl);
            final String docPageUrl = (foundUrl.startsWith("http") ? foundUrl : baseUrl + foundUrl);
            if ("https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html".equals(docPageUrl) ||
                    "https://docs.oracle.com/javafx/2/api/index.html".equals(docPageUrl) ||
                    "http://www.oracle.com/technetwork/java/javafx/downloads/supportedconfigurations-1506746.html".equals(docPageUrl) ||
                    "http://www.oracle.com/technetwork/java/javase/downloads/".equals(docPageUrl) ||
                    "https://docs.oracle.com/javafx/2/api/javafx/fxml/doc-files/introduction_to_fxml.html".equals(docPageUrl)) {
                continue;
            }
            System.out.println(docPageUrl);
            tasks.add((Callable<List<Document>>) () -> indexDocumentationPage(docPageUrl));
        }
        System.out.println(" --- end of list ---");
        return tasks;
    }

    private static List<Document> indexDocumentationPage(String docPageUrl) throws IOException{
        List<Document> docs = new ArrayList<>();
        try {
//            System.out.println("PROCESSING... ["+docPageUrl+"] on Thread ["+Thread.currentThread().getName()+"]");
//            System.out.println("==================================================================");
//            System.out.println("Parsing docs page ["+docPageUrl+"] ...");
            DocumentationIndexer.DocPage docPage = DocumentationIndexer.parseDocsPage(docPageUrl, grabWebPage(docPageUrl).toString());
//            System.out.println("TITLE="+docPage.bookTitle+"   CHAPTER="+docPage.chapter+"    SECTIONS=["+docPage.sections.size()+"]");
            for (DocumentationIndexer.Section section: docPage.sections) {
                if (section.name == null) {
                    System.out.println("section.name = "+section.name+" docPage.bookTitle="+docPage.bookTitle+"    "+docPageUrl);
                }
                // write documentation section entry to index
                docs.add(createDocument(DocumentType.DOC,
                    new TextField("bookTitle", docPage.bookTitle, Field.Store.YES),
                    new TextField("chapter", docPage.chapter==null? "" : docPage.chapter, Field.Store.YES),
                    new TextField("name", section.name, Field.Store.YES),
                    new TextField("description", section.content, Field.Store.NO),
                    new StringField("ensemblePath", section.url, Field.Store.YES)
                ));
            }
            // handle next page if there is one
            if (docPage.nextUrl != null) {
                docs.addAll(indexDocumentationPage(docPage.nextUrl));
            }

        } catch (Exception ex) {
            System.out.println("FAILED TO PARSE DOCS PAGE SO IGNORED: ["+docPageUrl+"]");
            ex.printStackTrace(System.out);
        }
        return docs;
    }

    private static List<Callable<List<Document>>> indexJavaDocAllClasses(final String javaDocBaseUrl) throws IOException{
        CharSequence content = grabWebPage(javaDocBaseUrl+"allclasses-noframe.html");
        List<Callable<List<Document>>> tasks = new ArrayList<>();
        // parse package
        Matcher matcher = findClassUrl.matcher(content);
        while (matcher.find()) {
            final String classUrl = javaDocBaseUrl+matcher.group(1);
            tasks.add((Callable<List<Document>>) () -> indexApiDocs(classUrl));
        }
        return tasks;
    }

    /**
     * Add all samples to the search index
     */
    private static List<Document> indexSamples(List<Sample> allSamples) throws IOException {
        List<Document> docs = new ArrayList<>();
        for (Sample sample: allSamples) {
            // write class entry to index
            docs.add(createDocument(DocumentType.SAMPLE,
                new TextField("name", sample.name, Field.Store.YES),
                new TextField("description", sample.description, Field.Store.NO),
                new StringField("shortDescription", sample.description.substring(0, Math.min(160, sample.description.length())),
                        Field.Store.YES),
                new StringField("ensemblePath", "sample://"+sample.ensemblePath, Field.Store.YES)
            ));
        }
        return docs;
    }

    /**
     * Index a JavaDoc page for a single class, interface or enum
     *
     * @param writer The index writer to add documents to
     * @param url The url to the javadoc html file
     * @throws IOException If there was a problem indexing the file
     */
    private static List<Document> indexApiDocs(String url) throws IOException {
//        System.out.println("PROCESSING... ["+url+"] on Thread ["+Thread.currentThread().getName()+"]");
        final List<Document> docs = new ArrayList<>();
        CharSequence content = grabWebPage(url);
        // extract package and class
        Matcher packageAndClassMatcher = PACKAGE_AND_CLASS.matcher(content);
        // search and if we fail to find ignore this file
        if (!packageAndClassMatcher.find()) {
            //System.out.println("!!!! Ignoring [" + file + "] because no class or package was found");
            return docs;
        } else {
            //System.out.println("Adding [" + file + "]");
        }
        //System.out.println("        fileUrl = " + fileUrl);
        String packageName = packageAndClassMatcher.group(1);
        //System.out.println("        packageName = " + packageName);
        String classType = packageAndClassMatcher.group(2).toLowerCase();
        //System.out.println("        classType = " + classType);
        String className = packageAndClassMatcher.group(3);
        //System.out.println("        className = " + className);
        // extract document type
        DocumentType documentType = DocumentType.CLASS;
        if ("enum".equals(classType)) {
            documentType = DocumentType.ENUM;
        }
        // extract javadoc description
        Matcher classDescriptionMatcher = CLASS_DESCRIPTION.matcher(content);
        String classDescription = "";
        if (classDescriptionMatcher.find()) {
            classDescription = cleanHTML(classDescriptionMatcher.group(1));
        }
        //System.out.println("classDescription = " + classDescription);
        // write class entry to index
        docs.add(createDocument(documentType,
                new TextField("name", className, Field.Store.YES),
                new TextField("description", classDescription, Field.Store.NO),
                new StringField("shortDescription", classDescription.substring(0,Math.min(160,classDescription.length())),
                        Field.Store.YES),
                new TextField("package", packageName, Field.Store.YES),
                new StringField("url", url, Field.Store.YES),
                new StringField("ensemblePath", url, Field.Store.YES) // TODO what do we need here
        ));

        // extract properties
        Matcher propertySummaryMatcher = PROPERTY_SUMMARY.matcher(content);
        if (propertySummaryMatcher.find()) {
            String propertySummaryTable = propertySummaryMatcher.group(1);
            Matcher propertyMatcher = PROPERTY.matcher(propertySummaryTable);
            while (propertyMatcher.find()) {
                String propUrl = propertyMatcher.group(1);
                String propertyName = propertyMatcher.group(2);
                String description = cleanHTML(propertyMatcher.group(3));
                //System.out.println("            propertyName = " + propertyName);
                //System.out.println("                    description = " + description);
                //System.out.println("                    url = " + url);
                propUrl = url + "#" + propertyName;
                //System.out.println("                    oracle url = " + url);
                // write class entry to index
                docs.add(createDocument(DocumentType.PROPERTY,
                        new TextField("name", propertyName, Field.Store.YES),
                        new TextField("description", description, Field.Store.NO),
                        new StringField("shortDescription", description.substring(0,Math.min(160,description.length())),
                                Field.Store.YES),
                        new StringField("url", propUrl, Field.Store.YES),
                        new StringField("className", className, Field.Store.YES),
                        new StringField("package", packageName, Field.Store.YES),
                        new StringField("ensemblePath", url + "#" + propertyName, Field.Store.YES) // TODO what do we need here
                ));
            }
        }
        // extract methods
        Matcher methodSummaryMatcher = METHOD_SUMMARY.matcher(content);
        if (methodSummaryMatcher.find()) {
            String methodSummaryTable = methodSummaryMatcher.group(1);
            Matcher methodMatcher = PROPERTY.matcher(methodSummaryTable);
            while (methodMatcher.find()) {
                String methodUrl = methodMatcher.group(1);
                String methodName = methodMatcher.group(2);
                String description = cleanHTML(methodMatcher.group(3));
                //System.out.println("            methodName = " + methodName);
                //System.out.println("                    description = " + description);
                //System.out.println("                    url = " + url);
                methodUrl = url + "#" + methodName+"()";
                //System.out.println("                    oracle url = " + url);
                // write class entry to index
                docs.add(createDocument(DocumentType.METHOD,
                        new TextField("name", methodName, Field.Store.YES),
                        new TextField("description", description, Field.Store.NO),
                        new StringField("shortDescription", description.substring(0,Math.min(160,description.length())),
                                Field.Store.YES),
                        new StringField("url", methodUrl, Field.Store.YES),
                        new StringField("className", className, Field.Store.YES),
                        new StringField("package", packageName, Field.Store.YES),
                        new StringField("ensemblePath", url + "#" + methodName + "()", Field.Store.YES) // TODO what do we need here
                ));
            }
        }
        // extract fields
        Matcher fieldSummaryMatcher = FIELD_SUMMARY.matcher(content);
        if (fieldSummaryMatcher.find()) {
            String fieldSummaryTable = fieldSummaryMatcher.group(1);
            Matcher fieldMatcher = PROPERTY.matcher(fieldSummaryTable);
            while (fieldMatcher.find()) {
                String fieldUrl = fieldMatcher.group(1);
                String fieldName = fieldMatcher.group(2);
                String description = cleanHTML(fieldMatcher.group(3));
                //System.out.println(" #####     fieldName = " + fieldName);
                //System.out.println("                    description = " + description);
                //System.out.println("                    url = " + url);
                fieldUrl = url + "#" + fieldName;
                //System.out.println("                    oracle url = " + url);
                // write class entry to index
                docs.add(createDocument(DocumentType.FIELD,
                        new TextField("name", fieldName, Field.Store.YES),
                        new TextField("description", description, Field.Store.NO),
                        new StringField("shortDescription", description.substring(0,Math.min(160,description.length())),
                                Field.Store.YES),
                        new StringField("url", fieldUrl, Field.Store.YES),
                        new StringField("className", className, Field.Store.YES),
                        new StringField("package", packageName, Field.Store.YES),
                        new StringField("ensemblePath", url + "#" + fieldName, Field.Store.YES) // TODO what do we need here
                ));
            }
        }
        // extract enums
        Matcher enumSummaryMatcher = ENUM_SUMMARY.matcher(content);
        if (enumSummaryMatcher.find()) {
            String enumSummaryTable = enumSummaryMatcher.group(1);
            Matcher enumMatcher = PROPERTY.matcher(enumSummaryTable);
            while (enumMatcher.find()) {
                String enumUrl = enumMatcher.group(1);
                String enumName = enumMatcher.group(2);
                String description = cleanHTML(enumMatcher.group(3));
                //System.out.println("            enumName = " + enumName);
                //System.out.println("                    description = " + description);
                //System.out.println("                    url = " + url);
                enumUrl = url + "#" + enumName;
                //System.out.println("                    oracle url = " + url);
                // write class entry to index
                docs.add(createDocument(DocumentType.ENUM,
                        new TextField("name", enumName, Field.Store.YES),
                        new TextField("description", description, Field.Store.NO),
                        new StringField("shortDescription", description.substring(0,Math.min(160,description.length())),
                                Field.Store.YES),
                        new StringField("url", enumUrl, Field.Store.YES),
                        new StringField("className", className, Field.Store.YES),
                        new StringField("package", packageName, Field.Store.YES),
                        new StringField("ensemblePath", url+ "#" + enumName, Field.Store.YES) // TODO what do we need here
                ));
            }
        }
        return docs;
    }

    /**
     * Create a new document
     *
     * @param documentType The document type to save in the doc
     * @param fields       The searchable and data fields to write into doc
     * @throws IOException If there was problem writing doc
     */
    private static Document createDocument(DocumentType documentType, Field... fields) throws IOException {
        // make a new, empty document
        Document doc = new Document();
        // add doc type field + sorting field
        doc.add(new StringField("documentType", documentType.toString(), Field.Store.YES));
        doc.add(new SortedDocValuesField("documentType", new BytesRef(documentType.toString())));
        // add other fields
        if (fields != null) {
            for (Field field : fields) {
                doc.add(field);
            }
        }
        return doc;
    }

    /**
     * Create a new document and write it to the given writer
     *
     * @param writer       The writer to write out to
     * @param documentType The document type to save in the doc
     * @param fields       The searchable and data fields to write into doc
     * @throws IOException If there was problem writing doc
     */
    private static void addDocument(IndexWriter writer, DocumentType documentType, Field... fields) throws IOException {
        // make a new, empty document
        Document doc = new Document();
        // add doc type field + sorting field
        doc.add(new StringField("documentType", documentType.toString(), Field.Store.YES));
        doc.add(new SortedDocValuesField("documentType", new BytesRef(documentType.toString())));
        // add other fields
        if (fields != null) {
            for (Field field : fields) {
                doc.add(field);
            }
        }
        // write into index, assuming we are recreating every time
        writer.addDocument(doc);
    }

    /**
     * Clean HTML, removing all tags and un-escaping so that we can index it cleanly
     *
     * @param html The html to clean
     * @return cleaned html
     */
    private static String cleanHTML(String html) {
        html = html.replaceAll("(&nbsp;|\\s|[ ])+", " ").trim(); // cleanup whitespace
        html = html.replaceAll("<.*?>", " "); // remove html tags
        html = html.replaceAll("&lt;", "<"); // un-escape <
        html = html.replaceAll("&gt;", ">"); // un-escape >
        html = html.replaceAll("&quot;", "\""); // un-escape "
        html = html.replaceAll("&apos;", "\'"); // un-escape '
        html = html.replaceAll("&amp;", "&"); // un-escape &
        return html;
    }

    static CharSequence grabWebPage(String url) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        }
        return builder;
    }


    // ===================  JAVAFX DOCUMENTATION PATTERNS ======================
    /*
     GET ALL LINKS FROM DOCS HOME PAGE
    <p class="fxblurblink"><a href="2/overview/jfxpub-overview.htm">What is JavaFX?</a></p>

    GROUP 1 = url
     */
    private static final Pattern docsHomeLink = Pattern.compile("<p\\s+class=\\\"fxblurblink\\\"\\s*>.*<a\\s*href=\\\"([^\\\"]+)");
    /*
     GET ALL LINKS FROM DOCS HOME PAGE
    <div id="bookTitle">
      <h1>Working With Layouts in JavaFX </h1>
    </div>

    GROUP 1 = book title
     */
    private static final Pattern bookTitle = Pattern.compile( "<div\\s+id=\\\"bookTitle\\\"\\s*>\\s*<h1>([^<]+)");
    /*
     GET ALL LINKS FROM DOCS HOME PAGE
    <h1 class="chapter">JavaFX Scene Builder Overview</h1>

    GROUP 1 = chapter name
     */
    private static final Pattern chapter = Pattern.compile("<h1\\s+class=\\\"chapter\\\"\\s*>([^<]+)");


    // ===================  API DOC PATTERNS ===================================
    /*
    Pull class urls from all classes page
     */
    private static final Pattern findClassUrl = Pattern.compile("a\\s+href=\\\"([^\\\"]+)\\\"");
    /*
    <div class="subTitle">javafx.scene</div>
    <h2 title="Class Scene" class="title">Class Scene</h2>
    </div>

    GROUP 1 = Package
    GROUP 2 = Class Type
    GROUP 3 = Class
     */
    //private static Pattern PACKAGE_AND_CLASS = Pattern.compile("<H2>\\s*<FONT SIZE=\"-1\">\\s*([^<]+)</FONT>\\s*<BR>\\s*(Class|Interface|Enum) ([^<&]+).*?</H2>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern PACKAGE_AND_CLASS = Pattern.compile("<div class=\"subTitle\">\\s*([^<]+)</div>\\s*<h2 title=\"(Class|Interface|Enum) ([^<&]+).*?\"\\sclass=\"title\">(Class|Interface|Enum) ([^<&]+).*?</h2>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    </PRE>
    <P>
    ...
    <HR>
    <div class="description">
    ...
    <p>
    ...
    </div>

    GROUP 1 = Class JavaDoc Description
     */
    //private static Pattern CLASS_DESCRIPTION = Pattern.compile("</PRE>\\s*<P>(.*?)<HR>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern CLASS_DESCRIPTION = Pattern.compile("<div class=\"description\">.*?<[pP]>(.*?)</div>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    <table ...>
    ...
    </table>

    GROUP 1 = Property Summary Table
     */
   // private static Pattern PROPERTY_SUMMARY = Pattern.compile("NAME=\"property_summary\">.*?<TABLE[^>]+>(.*?)</TABLE>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern PROPERTY_SUMMARY = Pattern.compile("<h3>Property Summary</h3>.*?<table[^>]+>(.*?)</table>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    <h3>Method Summary</h3>
    <table...>
    ...
    </table>

    GROUP 1 = Method Summary Table
     */
  //  private static Pattern METHOD_SUMMARY = Pattern.compile("NAME=\"method_summary\">.*?<TABLE[^>]+>(.*?)</TABLE>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern METHOD_SUMMARY = Pattern.compile("<h3>Method Summary</h3>.*?<table[^>]+>(.*?)</table>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    <h3>Enum Constant Summary</h3>
    <table...>
    ...
    </table>

    GROUP 1 = Enum Summary Table
     */
   // private static Pattern ENUM_SUMMARY = Pattern.compile("NAME=\"enum_constant_summary\">.*?<TABLE[^>]+>(.*?)</TABLE>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern ENUM_SUMMARY = Pattern.compile("<h3>Enum Constant Summary</h3>.*?<table[^>]+>(.*?)</table>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    <h3>Field Summary</h3>
    <table...>
    ...
    </table>

    GROUP 1 = Field Summary Table
     */
   // private static Pattern FIELD_SUMMARY = Pattern.compile("NAME=\"field_summary\">.*?<TABLE[^>]+>(.*?)</TABLE>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern FIELD_SUMMARY = Pattern.compile("<h3>Field Summary</h3>.*?<table[^>]+>(.*?)</table>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /*
    <td class="colFirst"><code><a href="../../../javafx/beans/property/DoubleProperty.html" title="class in javafx.beans.property">DoubleProperty</a></code></td>
    GROUP 1 = Url
    GROUP 2 = Name
    GROUP 2 = Description
     */
    //private static Pattern PROPERTY = Pattern.compile("<TD>.*?<A HREF=\"([^\"]*)\">([^<]*)</A>.*?<BR>(.*?)</TD>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern PROPERTY = Pattern.compile("<td class=\"colFirst\">.*?<a href=\"([^\"]*)\">([^<]*)</a>(.*?)</td>",Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
}
