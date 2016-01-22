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

package ensemble.compiletime;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static ensemble.compiletime.CodeGenerationUtils.*;
import ensemble.compiletime.Sample.URL;
import java.util.HashMap;
import javafx.application.ConditionalFeature;

/**
 * Scan all samples, finding all information needed and write a Samples.java file to generated sources.
 */
public class BuildSamplesList {
    private static final Pattern findJavaDocComment = Pattern.compile("\\/\\*\\*(.*?)\\*\\/\\s*",Pattern.DOTALL);
    private static final Pattern findSharedResource = Pattern.compile("\"(/ensemble/samples/shared-resources/[^\"]+)\"",Pattern.DOTALL);
    private static File samplesSrcDir;
    private static File samplesResourcesDir;
    private static List<Sample> highlightedSamples = new ArrayList<>();
    private static List<Sample> allSamples = new ArrayList<>();

    public static List<Sample> build(File samplesSrcDir, File samplesResourcesDir, File samplesSourceFile) {
        BuildSamplesList.samplesSrcDir = samplesSrcDir;
        File samplesDir = new File(samplesSrcDir,"ensemble/samples");
        BuildSamplesList.samplesResourcesDir = samplesResourcesDir; //Resources are in a different location from *.java files
        File resourcesDir = new File(samplesResourcesDir, "ensemble/samples");
        SampleCategory rootCategory = new SampleCategory("ROOT","",null);
        for(File dir: samplesDir.listFiles()) {
            if (dir.getName().charAt(0) != '.' && !"shared-resources".equals(dir.getName())) {
                processCategoryOrSampleDir(rootCategory, dir, resourcesDir);
            }
        }
        // generate code chunks so we have all samples in ALL_SAMPLES
        final String rootCategoryCode = rootCategory.generateCode();
        final String samplesArrayCode = sampleArrayToCode(highlightedSamples);
        // check if we want to write
        if (samplesSourceFile == null) return allSamples;
        // write
        PrintWriter fout = null;
        try {
            fout = new PrintWriter(samplesSourceFile, "UTF-8");
            fout.println("package ensemble.generated;");
            fout.println("import ensemble.*;");
            fout.println("import ensemble.playground.PlaygroundProperty;");
            fout.println("import javafx.application.ConditionalFeature;");
            fout.println("import java.util.HashMap;");
            fout.println("public class Samples{");
            // write samples
            for (int sampleIndex=0; sampleIndex < ALL_SAMPLES.size(); sampleIndex ++) {
                fout.print("    private static final SampleInfo SAMPLE_"+sampleIndex+" = ");
                fout.print(generateCode(ALL_SAMPLES.get(sampleIndex)));
                fout.println(";");
            }
            // write root category
            fout.print("    public static final SampleCategory ROOT = ");
            fout.print(rootCategoryCode);
            fout.println(";");
            // write highlights
            fout.print("    public static final SampleInfo[] HIGHLIGHTS = ");
            fout.print(samplesArrayCode);
            fout.println(";");
            // write docs to samples map
            fout.println("    private static final HashMap<String,SampleInfo[]> DOCS_URL_TO_SAMPLE = new HashMap<String,SampleInfo[]>("+DOCS_TO_SAMPLE_MAP.size()+");");
            fout.println("    static {");
            for (Map.Entry<String,Set<String>> entry: DOCS_TO_SAMPLE_MAP.entrySet()) {
                fout.println("        DOCS_URL_TO_SAMPLE.put(\""+entry.getKey().replace('$', '.')+"\","+variableNameArrayToCode("SampleInfo",entry.getValue())+");");
            }
            fout.println("    }");
            fout.println("    public static SampleInfo[] getSamplesForDoc(String docUrl) {");
            fout.println("        return DOCS_URL_TO_SAMPLE.get(docUrl);");
            fout.println("    }");
            // end class and flush
            fout.println("}");
            fout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) fout.close();
        }
        return allSamples;
    }

    private static void processCategoryOrSampleDir(SampleCategory category, File dir, File resourcesDir) {
        if (!dir.isDirectory()) {
            System.out.println("        found unexpected file: "+dir.getAbsolutePath());
            return;
        }
        // first see if we contain java files, if so assume we are a sample directory
        boolean containsJavaFile = false;
        for(String fileName: dir.list()) {
            if (fileName.endsWith(".java")) {
                containsJavaFile = true;
                break;
            }
        }
        if (containsJavaFile) {
            processSampleDir(category, dir, resourcesDir);
        } else {
            processCategoryDir(category, dir, resourcesDir);
        }
    }

    private static void processCategoryDir(SampleCategory category, File dir, File resourcesDir) {
        System.out.println("========= CATEGORY ["+formatName(dir.getName())+"] ===============");
        // create new category
        SampleCategory subCategory = new SampleCategory(
                formatName(dir.getName()),
                category.ensemblePath + "/" + formatName(dir.getName()),
                category);
        category.subCategories.add(subCategory);
        // process each subdir
        for(File subDir: dir.listFiles()) {
            if (subDir.getName().charAt(0) != '.' && !"shared-resources".equals(subDir.getName())) {
                processCategoryOrSampleDir(subCategory, subDir, resourcesDir);
            }
        }
    }

    private static void processSampleDir(SampleCategory category, File dir, File resourcesDir) {
        Sample sample = new Sample();
        Matcher matcher;
        System.out.println("============== SAMPLE ["+formatName(dir.getName())+"] ===============");
        // calculate base dir relative path
        sample.baseUri = dir.getAbsolutePath().substring(samplesSrcDir.getAbsolutePath().length()).replace('\\', '/');
        // find app main file
        File appFile = null;
        for (File file: dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith("App.java")) {
                appFile = file;
                break;
            }
        }
        if (appFile == null) {
            throw new IllegalArgumentException("Could not find JavaFX Application class for sample ["+dir.getName()+"] in ["+dir.getAbsolutePath()+"]");
        }
        sample.mainFileUrl = calculateRelativePath(appFile, samplesSrcDir);
        sample.appClass = sample.mainFileUrl.substring(1, sample.mainFileUrl.length()-5).replace('/', '.');
        // load app main file
        StringBuilder appFileContents = loadFile(appFile);
        // parse out java doc comment contents
        matcher = findJavaDocComment.matcher(appFileContents);
        if (!matcher.find()) throw new IllegalArgumentException("Failed to find java doc comment in sample file ["+appFile.getAbsolutePath()+"]");
        String javaDocComment = matcher.group(1);
        String[] lines = javaDocComment.split("([ \\t]*\\n[ \\t]*\\*[ \\t]*)+");
        StringBuilder descBuilder = new StringBuilder();
        for (String jdocline:lines) {
            String trimedLine = jdocline.trim();
            if (trimedLine.length()!= 0) {
                if(trimedLine.startsWith("@related")) {
                    sample.relatesSamplePaths.add(trimedLine.substring(8).trim());
                } else if(trimedLine.startsWith("@see")) {
                    sample.apiClasspaths.add(trimedLine.substring(4).trim());
                } else if(trimedLine.startsWith("@docUrl")) {
                    sample.docsUrls.add(new URL(trimedLine.substring(7).trim()));
                } else if(trimedLine.startsWith("@sampleName")) {
                    // todo resolve to a URL
                    sample.name = trimedLine.substring(11).trim();
                } else if(trimedLine.startsWith("@preview")) {
                    sample.previewUrl = sample.baseUri+"/"+trimedLine.substring(8).trim();
                } else if(trimedLine.startsWith("@highlight")) {
                    highlightedSamples.add(sample);
                } else if(trimedLine.startsWith("@playground")) {
                    String[] parts = trimedLine.substring(11).trim().split("\\s+\\(",2);
                    String name = parts[0].trim();
                    String[] nameparts = name.split("\\.");
                    Map<String,String> properties = new HashMap<>();
                    if (parts.length == 2) {
                        String props = parts[1].substring(0, parts[1].lastIndexOf(')')).trim();
                        parseProperties(props, properties);
                    }
                    String fieldName, propertyName;
                    if (nameparts.length >= 2) {
                        fieldName = nameparts[0].trim();
                        propertyName = nameparts[1].trim();
                    } else {
                        fieldName = null;
                        propertyName = nameparts[0].trim();
                    }
                    sample.playgroundProperties.add(
                            new Sample.PlaygroundProperty(fieldName, propertyName, properties));
                } else if (trimedLine.startsWith("@conditionalFeatures")) {
                    String[] features = trimedLine.substring(20).trim().split(",");
                    for (String feature : features) {
                        try {
                            ConditionalFeature cf = ConditionalFeature.valueOf(feature.trim());
                            sample.conditionalFeatures.add(cf);
                        } catch (IllegalArgumentException ex) {
                            System.err.println("@conditionalFeatures entry is not a feature: " + feature);
                        }
                    }
                } else if (trimedLine.startsWith("@embedded")) {
                    sample.runsOnEmbedded = true;
                } else {
                    descBuilder.append(trimedLine);
                    descBuilder.append(' ');
                }
            }
        }
        sample.description = descBuilder.toString();
        sample.ensemblePath = category.ensemblePath + "/" + sample.name;
        // scan sample dir for resources
        compileResources(sample, dir, true, samplesSrcDir);
        // scan samples/resources dir for resources too
        compileExtraResources(sample, resourcesDir, true, samplesResourcesDir);
        // add sample to category
        System.out.println(sample);
        category.addSample(sample);
        // add to all samples
        allSamples.add(sample);
    }

    private static void compileResources(Sample sample, File dir, boolean root, File baseDir) {
        for (File file: dir.listFiles()) {
            if (file.getName().charAt(0) != '.') { // ignore hidden unix files
                if (file.isDirectory()) {
                    compileResources(sample, file, false, baseDir);
                } else {
                    if (root && (file.getName().equalsIgnoreCase("preview.png")
                            || file.getName().equalsIgnoreCase("preview@2x.png"))) {
                        continue; // ignore preview files
                    }
                    if (file.getName().endsWith(".java")) {
                        // search for usage of shared resouces
                        StringBuilder fileContents = loadFile(file);
                        Matcher matcher = findSharedResource.matcher(fileContents);
                        while(matcher.find()) {
                            sample.resourceUrls.add(matcher.group(1));
                        }
                    }
                    // add file to resources list
                    sample.resourceUrls.add(calculateRelativePath(file, baseDir));
                }
            }
        }
    }

  private static void compileExtraResources(Sample sample, File dir, boolean root, File baseDir) {
      File specificResDir = new File(samplesResourcesDir, sample.baseUri.toString());
        for (File file: specificResDir.listFiles()) {
            if (file.getName().charAt(0) != '.') { // ignore hidden unix files
                if (file.isDirectory()) {
                    compileResources(sample, file, false, baseDir);
                } else {
                    if (file.getName().equalsIgnoreCase("preview.png")
                            || file.getName().equalsIgnoreCase("preview@2x.png")) {
                        continue; // ignore preview files
                    }
                    // add file to resources list
                    sample.resourceUrls.add(calculateRelativePath(file, baseDir));
                }
            }
        }
    }

    private static String calculateRelativePath(File file, File baseDir) {
        return file.getAbsolutePath().substring(baseDir.getAbsolutePath().length()).replace('\\', '/');
    }

    private static StringBuilder loadFile(File file) {
        StringBuilder builder = new StringBuilder();
        InputStream in = null;
        try {
            // load src into String
            in = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(BuildSamplesList.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ex) {
                Logger.getLogger(BuildSamplesList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return builder;
    }

    private static String formatName(String dirName) {
        // remove ending "Sample" from name
        if(dirName.endsWith("Sample")) dirName = dirName.substring(0,dirName.length()-"Sample".length());
        // add spaces
        dirName = dirName.replaceAll("([\\p{Upper}\\d])"," $1");
        // uppercase first char
        dirName = dirName.substring(0,1).toUpperCase() + dirName.substring(1);
        return dirName.trim();
    }

    private static void parseProperties(String props, Map<String, String> properties) throws RuntimeException {
        boolean failed = true;
        StreamTokenizer st = new StreamTokenizer(new StringReader(props));
        st.resetSyntax();
        st.wordChars('\u0001', '\uFFFF');
        st.quoteChar('"');
        st.quoteChar('\'');
        st.ordinaryChar(',');
        st.ordinaryChar('=');
        st.whitespaceChars(' ', ' ');
        st.whitespaceChars('\t', '\t');
        try {
            int t = st.nextToken();
            do {
                if (t == StreamTokenizer.TT_EOF || t == StreamTokenizer.TT_EOL) {
                    break;
                }
                if (t != StreamTokenizer.TT_WORD) {
                    throw new RuntimeException("Property name expected here: " + st.toString());
                }
                String name = st.sval.trim();
                t = st.nextToken();
                if (t != '=') {
                    throw new RuntimeException("'=' expected here: " + st.toString());
                }
                t = st.nextToken();
                if (t != StreamTokenizer.TT_WORD && t != '"' && t != '\'') {
                    throw new RuntimeException("Property value expected here: " + st.toString());
                }
                String value = st.sval.trim();
                properties.put(name, value);
                t = st.nextToken();
                if (t == ',') {
                    t = st.nextToken();
                }
            } while (true);
            failed = false;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse properties due to unexpected IOException", ex);
        } finally {
            if (failed) {
                System.err.println("Failed to parse: " + props);
            }
        }
    }
}
