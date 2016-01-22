/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.search;

import ensemble.generated.Samples;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.SearchGroup;
import org.apache.lucene.search.grouping.SecondPassGroupingCollector;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.util.Version;

/**
 * Class for searching the index
 */
public class IndexSearcher {
    private final static List<SearchGroup> searchGroups = new ArrayList<>();
    static {
        for (DocumentType dt: DocumentType.values()){
            SearchGroup searchGroup = new SearchGroup();
            searchGroup.groupValue = dt.toString();
            searchGroup.sortValues = new Comparable[]{5f};
            searchGroups.add(searchGroup);
        }
    }
    private org.apache.lucene.search.IndexSearcher searcher;
    private final Analyzer analyzer;
    private final MultiFieldQueryParser parser;

    public IndexSearcher() {
        try {
            searcher = new org.apache.lucene.search.IndexSearcher(new ClasspathDirectory());
        } catch (IOException e) {
            e.printStackTrace();
        }
        analyzer = new StandardAnalyzer(Version.LUCENE_31);
        parser = new MultiFieldQueryParser(Version.LUCENE_31, new String[]{"name","bookTitle","chapter","description"}, analyzer);
    }

    public Map<DocumentType, List<SearchResult>> search(String searchString) throws ParseException {
        Map<DocumentType, List<SearchResult>> resultMap = new EnumMap<>(DocumentType.class);
        try {
            Query query = parser.parse(searchString);
            final SecondPassGroupingCollector collector = new SecondPassGroupingCollector("documentType", searchGroups,
                    Sort.RELEVANCE, Sort.RELEVANCE, 10, true, false, true);
            searcher.search(query, collector);
            final TopGroups groups = collector.getTopGroups(0);
            for (GroupDocs groupDocs : groups.groups) {
                DocumentType docType = DocumentType.valueOf(groupDocs.groupValue);
                List<SearchResult> results = new ArrayList<>();
                for (ScoreDoc scoreDoc : groupDocs.scoreDocs) {
                    if ((Platform.isSupported(ConditionalFeature.WEB)) || (docType != DocumentType.DOC)) {
                        Document doc = searcher.doc(scoreDoc.doc);
                        SearchResult result = new SearchResult(
                                docType,
                                doc.get("name"),
                                doc.get("url"),
                                doc.get("className"),
                                doc.get("package"),
                                doc.get("ensemblePath"),
                                docType == DocumentType.DOC
                                        ? doc.get("bookTitle") == null ? doc.get("chapter") : doc.get("bookTitle")
                                        : doc.get("shortDescription").trim()
                        );
                        /* If the result is a sample, then filter out the samples that
                        * the runtime platform does not support. We really want to show
                        * just 5 results, but we search for 10 and filter out unsupported
                        * samples and show just 5.
                        */
                        if (docType == DocumentType.SAMPLE) {
                            if (Samples.ROOT.sampleForPath(result.getEnsemblePath().substring(9).trim()) == null) {

                                // Skip unsupported (not existing) samples
                                continue;
                            }
                            if (results.size() == 5) {

                                // 5 samples is enough
                                break;
                            }
                        }
                        results.add(result);
                    }
                }
                resultMap.put(docType, results);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * Simple command line test application
     * @param args command line arguments
     * @throws Exception for maps errors
     */
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        IndexSearcher indexSearcher = new IndexSearcher();
        while (true) {
            System.out.println("Enter query: ");
            String line = in.readLine();
            if (line == null || line.length() == -1) break;
            line = line.trim();
            if (line.length() == 0) break;
            Map<DocumentType, List<SearchResult>> results = indexSearcher.search(line);
            for (Map.Entry<DocumentType, List<SearchResult>> entry : results.entrySet()) {
                System.out.println("--------- "+entry.getKey()+" ["+entry.getValue().size()+"] --------------------------------");
                for(SearchResult result: entry.getValue()) {
                    System.out.println(result.toString());
                }
            }
        }
    }
}
