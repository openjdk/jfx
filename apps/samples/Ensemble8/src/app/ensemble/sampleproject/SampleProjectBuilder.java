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
package ensemble.sampleproject;

import ensemble.SampleInfo;
import ensemble.util.Utils;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.scene.Node;

/**
 * Helper class to build an IDE project for a sample and open it in the IDE
 */
public class SampleProjectBuilder {
 
    public static void createSampleProject(File projectDir, SampleInfo sampleInfo) {
        String nodeLoc = Node.class.getResource("Node.class").toExternalForm();
        String javafxrtPath = nodeLoc.substring(4, nodeLoc.indexOf('!'));
        try {
            File f = new File(new URI(javafxrtPath));
            javafxrtPath = f.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sep = System.getProperty("file.separator");
        if (sep.equals("\\")) {
            javafxrtPath = javafxrtPath.replaceAll("\\" + sep, "/");
        }
        // extract project name
        String projectName = projectDir.toURI().toString();
        projectName = projectName.substring(projectName.lastIndexOf('/') + 1);
        // create destDir
        projectDir.mkdirs();
        // unzip project template
        try {
            ZipInputStream zipinputstream = new ZipInputStream(
                    SampleProjectBuilder.class.getResourceAsStream("SampleProject.zip"));
            ZipEntry zipentry;
            while ((zipentry = zipinputstream.getNextEntry()) != null) {
                //for each entry to be extracted
                String entryName = zipentry.getName();
                File entryFile = new File(projectDir, entryName);
                if (zipentry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // assume all are text files, load text file into string so we can process it
                    StringBuilder sb = new StringBuilder();
                    String line;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipinputstream));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append('\n');
                    }
                    String contents = sb.toString();
                    // replace any place holders
                    contents = contents.replaceAll("ENSEMBLESAMPLE", projectName);
                    contents = contents.replaceAll("APPLICATIONCLASS", sampleInfo.appClass);
                    contents = contents.replaceAll("PATHTOJAVAFXRTJAR", javafxrtPath);
                    // save out file
                    FileWriter fileWriter = new FileWriter(entryFile);
                    fileWriter.write(contents);
                    fileWriter.flush();
                    fileWriter.close();
                }
                zipinputstream.closeEntry();
            }
            zipinputstream.close();
            //Put resources like images under src/
            File srcDestDir = new File(projectDir.getPath() + "/src/");
            loadSampleResourceUrls(srcDestDir, sampleInfo.resourceUrls);
            // open project in netbeans
            //TODO:      loadProject(projectDir, mainSrcFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadSampleResourceUrls(File destDir, String[] resourceUrlArray) {
        List<String> resourceUrlList = Arrays.asList(resourceUrlArray);
        //create resource files for each of the resources we use
        if (!resourceUrlList.isEmpty()) {
            for (String oneResourceUrl : resourceUrlList) {
                String resourceSubdir = oneResourceUrl.substring(0, oneResourceUrl.lastIndexOf('/'));
                try {
                    File parentDir = new File(destDir.getPath() + resourceSubdir);
                    parentDir.mkdirs();
                    Utils.copyFile(new URL(SampleProjectBuilder.class.getResource(oneResourceUrl).toExternalForm()),
                            destDir.getPath() + oneResourceUrl);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
