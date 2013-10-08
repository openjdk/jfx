/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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
package ensemble.samplepage;

import ensemble.EnsembleApp;
import ensemble.Page;
import ensemble.PageBrowser;
import ensemble.SampleInfo;
import ensemble.control.BendingPages;
import ensemble.util.Utils;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Page for showing a sample
 */
public class SamplePage extends Region implements Page {
    static final double INDENT = 8;
    SampleInfo sample;
    PageBrowser pageBrowser;
    private final ReadOnlyStringProperty titleProperty;
    
    public SamplePage(SampleInfo sample, String url, final PageBrowser pageBrowser) {
        this.sample = sample;
        this.pageBrowser = pageBrowser;
        getStyleClass().add("sample-page");
        titleProperty = new ReadOnlyStringWrapper(sample.name);

        if (EnsembleApp.IS_IPHONE) {
            IPhoneLayout iPhoneLayout = new IPhoneLayout(this);
            iPhoneLayout.prefWidthProperty().bind(widthProperty());
            iPhoneLayout.prefHeightProperty().bind(heightProperty());
            getChildren().setAll(iPhoneLayout);
        } else {
            FrontPage frontPage = new FrontPage(this);
            BackPage backPage = new BackPage(this);

            if (EnsembleApp.IS_EMBEDDED || EnsembleApp.IS_IOS) {
                SlidingPages slidingPages = new SlidingPages();
                slidingPages.prefWidthProperty().bind(widthProperty());
                slidingPages.prefHeightProperty().bind(heightProperty());
                slidingPages.setFrontPage(frontPage);
                slidingPages.setBackPage(backPage);
                getChildren().setAll(slidingPages);
            } else {
                
                BendingPages bendingPages = new BendingPages();
                bendingPages.prefWidthProperty().bind(widthProperty());
                bendingPages.prefHeightProperty().bind(heightProperty());
                bendingPages.setFrontPage(frontPage);
                bendingPages.setBackPage(backPage);
                bendingPages.setColors(Color.rgb(3, 95, 188), Color.rgb(4, 164, 231), Color.rgb(0, 57, 117));
                bendingPages.setClosedOffset(new Point2D(50, 40));
                getChildren().setAll(bendingPages);
            }
        }
    }
    
    @Override public ReadOnlyStringProperty titleProperty() {
        return titleProperty;
    }

    @Override public String getTitle() {
        return sample.name;
    }

    @Override public String getUrl() {
        return "sample://"+sample.ensemblePath;
    }

    @Override public Node getNode() {
        return this;
    }
    
    String apiClassToUrl(String classname) {
        String urlEnd = classname.replace('.', '/').replace('$', '.');
        if (classname.startsWith("javafx")) {
            return "http://download.java.net/jdk8/jfxdocs/"+urlEnd+".html";
        } else {
            return "http://download.java.net/jdk8/docs/api/"+urlEnd+".html";
        }
    }

    private String shCoreJs;
    private String shBrushJScript;
    private String shCoreDefaultCss;
    
    private static final Pattern JAVA_DOC_PATTERN = Pattern.compile("(^\\s+\\*$\\s)?^\\s+\\*\\s+@.*$\\s",Pattern.MULTILINE);
    String convertToHTML(String source) {
        // load syntax highlighter
        if (shCoreJs == null) {
            shCoreJs = Utils.loadFile(EnsembleApp.class.getResource("syntaxhighlighter/shCore.js")) +";";
        }
        if (shBrushJScript == null) {
            shBrushJScript = Utils.loadFile(EnsembleApp.class.getResource("syntaxhighlighter/shBrushJava.js"));
        }
        if (shCoreDefaultCss == null) {
            shCoreDefaultCss = Utils.loadFile(EnsembleApp.class.getResource("syntaxhighlighter/shCoreDefault.css")).replaceAll("!important","");
        }
        // split copy right and source
        String[] parts = source.split("\\*/",2);
        String copyRight = null;
        if (parts.length > 1) {
            copyRight = parts[0]+"*/";
            source = parts[1];
        }
        // remove JavaDoc @xxxx lines
        source = JAVA_DOC_PATTERN.matcher(source).replaceAll("");
        // escape < & >
        source = source.replaceAll("&","&amp;");
        source = source.replaceAll("<","&lt;");
        source = source.replaceAll(">","&gt;");
        source = source.replaceAll("\"","&quot;");
        source = source.replaceAll("\'","&apos;");
        // create content
        StringBuilder html = new StringBuilder();
        html.append("<html>\n");
        html.append("    <head>\n");
        html.append("    <script type=\"text/javascript\">\n");
        html.append(shCoreJs);
        html.append('\n');
        html.append(shBrushJScript);
        html.append("    </script>\n");
        html.append("    <style>\n");
        html.append(shCoreDefaultCss);
        html.append('\n');
        html.append("        .syntaxhighlighter {\n");
        html.append("			overflow: visible;\n");
        if (EnsembleApp.IS_MAC) {
            html.append("			font: 12px Ayuthaya !important; line-height: 150% !important; \n");
            html.append("		}\n");
            html.append("		code { font: 12px Ayuthaya !important; line-height: 150% !important; } \n");
        } else {
            html.append("			font: 12px monospace !important; line-height: 150% !important; \n");
            html.append("		}\n");
            html.append("		code { font: 12px monospace !important; line-height: 150% !important; } \n");
        }
        html.append("		.syntaxhighlighter .preprocessor { color: #060 !important; }\n");
        html.append("		.syntaxhighlighter .comments, .syntaxhighlighter .comments a  { color: #009300 !important; }\n");
        html.append("		.syntaxhighlighter .string  { color: #555 !important; }\n");
        html.append("		.syntaxhighlighter .value  { color: blue !important; }\n");
        html.append("		.syntaxhighlighter .keyword  { color: #000080 !important; }\n");
        html.append("		.hidden { display: none; }\n");
        html.append("           .showing { display: block; }\n");
        html.append("           .button {\n");
        html.append("               font: 12px \"Consolas\", \"Bitstream Vera Sans Mono\", \"Courier New\", Courier, monospace !important;\n");
        html.append("               color: #009300 !important;\n");
        html.append("               text-decoration: underline;\n");
        html.append("               display: inline;\n");
        html.append("               cursor:pointer;\n");
        html.append("           }\n");
        html.append("    </style>\n");
        html.append("    </head>\n");
        html.append("<body>\n");
        if (copyRight != null) {
            html.append("    <div onclick='document.getElementById(\"licenceText\").className = \"showing\";document.getElementById(\"licenseBtn\").className = \"hidden\";' id=\"licenseBtn\" class=\"button\">/* ....Show License.... */</div>\n");
            html.append("    <div id=\"licenceText\"class=\"hidden\">\n");
            html.append("    <pre class=\"brush: java;gutter: false;toolbar: false;\">\n");
            html.append(copyRight);
            html.append('\n');
            html.append("    </pre>\n");
            html.append("    </div>\n");
        }
        html.append("    <pre class=\"brush: java;gutter: false;toolbar: false;\">\n");
        html.append(source);
        html.append('\n');
        html.append("    </pre>\n");
        html.append("    <script type=\"text/javascript\"> SyntaxHighlighter.all(); </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
//        System.out.println("------------------------------------------------------------");
//        System.out.println(html);
//        System.out.println("------------------------------------------------------------");
        return html.toString();
    }
}
