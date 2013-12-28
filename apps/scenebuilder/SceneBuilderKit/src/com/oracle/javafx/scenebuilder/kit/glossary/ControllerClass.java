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
package com.oracle.javafx.scenebuilder.kit.glossary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Refactored the code taken from SB 1.1
 */
class ControllerClass {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("#package# (.*?)#;#");//NOI18N
    private static final Pattern CLASS_PATTERN = Pattern.compile("#class# (.*?)\\s");//NOI18N
    private static final Pattern INITIALIZABLE_PATTERN = Pattern.compile("#implements# (.*?)\\s#\\{#");//NOI18N
    // Never found a ( before the ; or = for Fields.
    //#@#FXML#<\n><static></n><final></n><modifier><\n><type></n><instance></n>[=|;]
    private static final Pattern FXID_PATTERN_1 = Pattern.compile("#@#FXML#(\\s*?[^\\(]*?\\s*?)[;|=]#");//NOI18N

    //#public# #Button# b;
    private static final Pattern FXID_PATTERN_2 = Pattern.compile("#public#(\\s*?[^\\(\\{]*?\\s*?)[;|=]#");//NOI18N

    // Never found a = before the ( for methods.
    //#@#FXML#<\n><modifier><\n><return type></n><methodName></n>#(#
    private static final Pattern EVENT_PATTERN_1 = Pattern.compile("#@#FXML#(\\s*?[^=]*?\\s*?)#\\{#");//NOI18N

    private static final String anyMethodToken = "\\s*?[^\\(\\{]*?\\s*?";//NOI18N
    private static final String staticFinalToken = "\\s*?(?:#static#|#final#)?\\s*?";//NOI18N
    private static final String voidToken = "\\s*?#void#\\s*?";//NOI18N
    private static final String publicToken = "#public#";//NOI18N
    private static final String methodNameToken = "#[^\\(\\{]*?#\\s*?#?";//NOI18N
    private static final String typeEventToken = "#\\s*?.*?Event#\\s*?#";//NOI18N
    private static final String argEventToken = ".*?#\\s*?#?";//NOI18N
    private static final String throwsToken = "\\s*?(?:#throws#)?\\s*?";//NOI18N
    private static final String exceptionToken = anyMethodToken;
    private static final String startBlockToken = "#?\\{#";//NOI18N
    private static final Pattern EVENT_PATTERN_2 = Pattern.compile(publicToken + staticFinalToken + staticFinalToken + voidToken + "(" + //NOI18N
            methodNameToken + "\\(" + typeEventToken + argEventToken + "\\)#)" + throwsToken + exceptionToken + startBlockToken);//NOI18N
    private static final Pattern EVENT_PATTERN_3 = Pattern.compile(publicToken + staticFinalToken + staticFinalToken + voidToken + "(" + //NOI18N
            methodNameToken + "\\(#\\s*?#?\\)#)" + throwsToken + exceptionToken + startBlockToken);//NOI18N  
    private final String javaContent;
    private final String tokenizedContent;
    private final boolean isInitializable;
    private final File file;
    private final String className;
    private Set<String> fxids;
    private Set<String> events;

    private ControllerClass(File file) throws IOException, JavaTokenizer.ParseException {
        assert file != null;
        this.file = file;
        javaContent = readFile(file);
        tokenizedContent = JavaTokenizer.tokenize(javaContent);
        className = retrieveControllerClassName(tokenizedContent);
        assert className != null;
        isInitializable = retrieveInitializableInterface();
    }

    public String getClassName() {
        return className;
    }

    public File getFile() {
        return file;
    }

    public boolean isInitializable() {
        return isInitializable;
    }

    public static Set<ControllerClass> discoverFXMLControllerClasses(File fxmlFile) {
        ScanData data = new ScanData();

        try {
            String name = getNoExtensionName(fxmlFile);
            File parentFile = fxmlFile.getParentFile();
            // Current + go up 1 dir level and scan.
            int maxDepth = 2;
            for (int i = 0; i < maxDepth; i++) {
                scanDirectory(name, parentFile, data);
                if (!data.continueScanning()) {
                    break;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println(ex);
        }
        return data.getClasses();
    }

    private static void scanDirectory(String name, File directory, ScanData data) {
        try {
            //1) Same name .java
            File f = new File(directory, name + ".java");//NOI18N
            if (f.exists()) {
                try {
                    ControllerClass clazz = new ControllerClass(f);
                    data.add(clazz);
                } catch (IOException | JavaTokenizer.ParseException ex) {
                    // NOTE skipping class
                }
            }
            //2) Same nameController.java
            File f2 = new File(directory, name + "Controller.java");//NOI18N
            if (f2.exists()) {
                try {
                    ControllerClass clazz = new ControllerClass(f2);
                    data.add(clazz);
                } catch (IOException | JavaTokenizer.ParseException ex) {
                    // NOTE skipping class
                }
            }
            //3) Contains FXML, requires list all java files in the same directory.
            // Enter the list if it exists.
            for (File javaFile : filterJavaFiles(directory)) {
                if (javaFile.equals(f) || javaFile.equals(f2)) {
                    continue;
                }
                try {
                    ControllerClass clazz = new ControllerClass(javaFile);
                    if (!clazz.getFxIds().isEmpty()
                            || !clazz.getEventHandlers().isEmpty() || clazz.isInitializable()) {
                        data.add(clazz);
                    } else {
                        data.javaScanned();
                    }
                } catch (IOException | JavaTokenizer.ParseException ex) {
                    // NOTE skipping class
                }
                if (!data.continueScanning()) {
                    return;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println(ex);
        }
    }

    private static String getNoExtensionName(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");//NOI18N
        if (index == -1) {
            index = name.length();
        }
        return name.substring(0, index);
    }

    private static File[] filterJavaFiles(File directory) {
        // final int[] count = {0};
        final List<File> fileList = new ArrayList<>();
        FilenameFilter ff = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                // count[0] = count[0] + 1;
                if (name.endsWith(".java")) { //NOI18N
                    final File file = new File(dir, name);
                    if (file.canRead() && !file.isDirectory()) {
                        fileList.add(file);
                        return true;
                    }
                }
                return false;
            }
        };
        if (directory != null) {
            directory.list(ff);
        }
        // System.out.println("Parsed "+count[0]+" files");
        return fileList.toArray(new File[fileList.size()]);
    }

    private static String retrieveControllerClassName(String content) {
        String clazzName = null;
        String packageName;
        Matcher m = CLASS_PATTERN.matcher(content);
        if (m.find()) {
            clazzName = m.group(1);
            clazzName = clazzName.replaceAll("#", "");//NOI18N
        }
        m = PACKAGE_PATTERN.matcher(content);
        if (m.find()) {
            packageName = m.group(1);
            packageName = packageName.replaceAll("#", "");//NOI18N
            clazzName = packageName + "." + clazzName; //NOI18N
        }
        return clazzName;
    }

    private boolean retrieveInitializableInterface() {
        List<String> str = extract(INITIALIZABLE_PATTERN);
        if (str.isEmpty()) {
            return false;
        }
        for (String s : str) {
            if (s.contains("Initializable")) {//NOI18N
                return true;
            }
        }
        return false;
    }

    private static List<String> extract(Pattern p, String content) {
        List<String> result = new ArrayList<>();
        Matcher m = p.matcher(content);
        while (m.find()) {
            result.add(m.group(1));
        }

        return Collections.unmodifiableList(result);
    }

    private List<String> extract(Pattern p) {
        return extract(p, tokenizedContent);
    }

    public Set<String> getFxIds() {
        if (fxids == null) {
            Set<String> fxids1 = cleanFxIds(extract(FXID_PATTERN_1));
            Set<String> fxids2 = cleanFxIds(extract(FXID_PATTERN_2));
            fxids = new HashSet<>();
            fxids.addAll(fxids1);
            fxids.addAll(fxids2);
        }
        return fxids;
    }

    public Set<String> getEventHandlers() {
        if (events == null) {
            List<String> events1 = extract(EVENT_PATTERN_1);
            List<String> events2 = extract(EVENT_PATTERN_2);
            List<String> events3 = extract(EVENT_PATTERN_3);
            events = new HashSet<>();
            events.addAll(cleanEvents(events1));
            events.addAll(cleanEvents(events2));
            events.addAll(cleanEvents(events3));
        }
        return events;
    }

    private static List<String> cleanEvents(List<String> extracted) {
        List<String> ret = new ArrayList<>();
        for (String str : extracted) {
            int index = str.indexOf("#(#");//NOI18N
            if (index == -1) {
                continue;
            }
            String subStr = str.substring(0, index);
            String cleaned = lastJavaIdentifierPart(subStr);
            if (cleaned != null) {
                ret.add(cleaned);
            }
        }
        return ret;
    }

    private static Set<String> cleanFxIds(List<String> extracted) {
        Set<String> ret = new HashSet<>();
        for (String str : extracted) {
            // Make it <instance Name>
            String instanceName = lastJavaIdentifierPart(str);
            if (instanceName == null) {
                continue;
            }
            ret.add(instanceName);
        }
        return ret;
    }

    private static String lastJavaIdentifierPart(String str) {
        try {
            assert str != null;
            int index = str.length() - 1;
            Character c;
            while (!Character.isJavaIdentifierPart(c = str.charAt(index)) && index >= 0) {
                index--;
            }
            int indexEnd = index + 1;
            while (Character.isJavaIdentifierPart(c = str.charAt(index)) && index >= 0) {
                index--;
            }
            int indexStart = index + 1;
            return str.substring(indexStart, indexEnd);
        } catch (RuntimeException ex) {
            System.err.println(ex);
            return null;
        }
    }

    private static String readFile(File file) throws FileNotFoundException, IOException {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        StringBuilder sb = new StringBuilder();
        String line;

        try (BufferedReader bufReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")))) { //NOI18N
            while ((line = bufReader.readLine()) != null) {
                sb.append(line).append("\n"); //NOI18N
            }
        }
        return sb.toString();
    }

    private static class ScanData {

        private Set<ControllerClass> files = new HashSet<>();
        private int numJavaParsed;

        private boolean continueScanning() {
            return numJavaParsed < 200 && files.size() < 100;
        }

        private void javaScanned() {
            numJavaParsed++;
        }

        private void add(ControllerClass clazz) {
            numJavaParsed++;
            files.add(clazz);
        }

        private Set<ControllerClass> getClasses() {
            return files;
        }
    }
}
