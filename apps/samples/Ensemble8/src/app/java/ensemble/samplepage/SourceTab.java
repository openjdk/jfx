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
package ensemble.samplepage;

import ensemble.EnsembleApp;
import ensemble.SampleInfo.URL;
import ensemble.util.Utils;
import ensemble.util.WebViewWrapper;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.regex.Pattern;

/**
 * Source code tab - Shows Syntax highlighted source if web view is available if not just plain text.
 */
class SourceTab extends Tab {

    public SourceTab(URL sourceURL) {
        super(sourceURL.getName());
        String url = sourceURL.getURL();
        String ext = url.substring(url.lastIndexOf('.')).toLowerCase();
        switch (ext) {
            case ".java":
            case ".css":
            case ".fxml":
                String source = Utils.loadFile(getClass().getResource(url));
                if (EnsembleApp.IS_EMBEDDED || EnsembleApp.IS_IOS || EnsembleApp.IS_ANDROID || !Platform.isSupported(ConditionalFeature.WEB)) {
                    // TODO: Convert to TextFlow
                    //                    TextFlow textFlow = TextFlowBuilder.create()
                    //                            .build();
                    //
                    //                    Reader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(url)));
                    TextArea textArea = new TextArea(source);
                    textArea.setStyle("-fx-font-family: 'Courier New';");
                    textArea.setEditable(false);
                    setContent(textArea);
                } else {
                    String html = convertToHTML(source);
                    setContent(WebViewWrapper.createWebView(html));
                }
                break;
            case ".jpg":
            case ".png":
                ImageView imageView = new ImageView(new Image(url));
                StackPane stackPane = new StackPane(imageView);
                ScrollPane scrollPane = new ScrollPane(stackPane);
                scrollPane.setFitToHeight(true);
                scrollPane.setFitToWidth(true);
                setContent(scrollPane);
                break;
        }
        setTooltip(new Tooltip(url));
    }

    private static final Pattern JAVA_DOC_PATTERN = Pattern.compile("(^\\s+\\*$\\s)?^\\s+\\*\\s+@.*$\\s",Pattern.MULTILINE);
    private static String shCoreJs;
    private static String shBrushJScript;
    private static String shCoreDefaultCss;

    private static String convertToHTML(String source) {
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
        html.append("           overflow: visible;\n");
        if (EnsembleApp.IS_MAC) {
            html.append("           font: 12px Ayuthaya !important; line-height: 150% !important; \n");
            html.append("       }\n");
            html.append("       code { font: 12px Ayuthaya !important; line-height: 150% !important; } \n");
        } else {
            html.append("           font: 12px monospace !important; line-height: 150% !important; \n");
            html.append("       }\n");
            html.append("       code { font: 12px monospace !important; line-height: 150% !important; } \n");
        }
        html.append("       .syntaxhighlighter .preprocessor { color: #060 !important; }\n");
        html.append("       .syntaxhighlighter .comments, .syntaxhighlighter .comments a  { color: #009300 !important; }\n");
        html.append("       .syntaxhighlighter .string  { color: #555 !important; }\n");
        html.append("       .syntaxhighlighter .value  { color: blue !important; }\n");
        html.append("       .syntaxhighlighter .keyword  { color: #000080 !important; }\n");
        html.append("       .hidden { display: none; }\n");
        html.append("           .showing { display: block; }\n");
        html.append("           .button {\n");
        html.append("               font: 12px \"Consolas\", \"Bitstream Vera Sans Mono\", \"Courier New\", Courier, monospace !important;\n");
        html.append("               color: #009300 !important;\n");
        html.append("               text-decoration: underline;\n");
        html.append("               display: inline;\n");
        html.append("               cursor:pointer;\n");
        html.append("           }\n");
        html.append("        body {background-color: #f4f4f4;}\n");
        html.append("    </style>\n");
        html.append("    </head>\n");
        html.append("<body>\n");
        if (copyRight != null) {
            html.append("    <div onclick='document.getElementById(\"licenceText\").className = \"showing\";document.getElementById(\"licenseBtn\").className = \"hidden\";' id=\"licenseBtn\" class=\"button\">/* ....Show License.... */</div>\n");
            html.append("    <div id=\"licenceText\"class=\"hidden\">\n");
            html.append("    <pre class=\"brush: java; gutter: false; toolbar: false; quick-code: false;\">\n");
            html.append(copyRight);
            html.append('\n');
            html.append("    </pre>\n");
            html.append("    </div>\n");
        }
        html.append("    <pre class=\"brush: java; gutter: false; toolbar: false; quick-code: false;\">\n");
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
