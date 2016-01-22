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

package ensemble.compiletime;

import ensemble.compiletime.search.BuildEnsembleSearchIndex;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * This is run during the build by ant to build sample files and search indexes
 */
public class EnsembleCompiletimeMain {

    static {
        System.setProperty("java.net.useSystemProxies", "true");
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("                 Ensemble Compile Time Build");
        System.out.println("args = "+Arrays.toString(args));
        System.out.println("==================================================================");
        File ensembleDir = new File(System.getProperty("user.dir"));
        System.out.println("ensembleDir = " + ensembleDir+" - "+ensembleDir.exists());
        File generatedSrcDir = new File(ensembleDir,"src/generated/java/ensemble/generated");
        System.out.println("generatedSrcDir = " + generatedSrcDir+" - "+generatedSrcDir.exists());
        generatedSrcDir.mkdirs();
        System.out.println("generatedSrcDir = " + generatedSrcDir.getAbsolutePath());
        File samplesDir = new File(ensembleDir,"src/samples/java");
        System.out.println("samplesDir = " + samplesDir.getAbsolutePath());
        File resourcesDir = new File(ensembleDir,"src/samples/resources");
        System.out.println("resourcesDir = " + resourcesDir.getAbsolutePath());
        // process args
        boolean buildSearchIndex = false, buildSampleClass = false;
        for (int a=0; a< args.length; a++) {
            if (args[a].equalsIgnoreCase("index")) buildSearchIndex = true;
            if (args[a].equalsIgnoreCase("samples")) buildSampleClass = true;
        }
        System.out.println("buildSearchIndex = " + buildSearchIndex);
        System.out.println("buildSampleClass = " + buildSampleClass);
        // build samples list
        List<Sample> allSamples = BuildSamplesList.build(samplesDir, resourcesDir, buildSampleClass ? new File(generatedSrcDir,"Samples.java") : null);
        System.out.println("TOTAL SAMPLES = " + allSamples.size());
        if (buildSampleClass) {
            System.out.println("==================================================================");
            System.out.println("                 Written Samples.java class file");
            System.out.println("==================================================================");
        }

        System.out.println("buildSearchIndex = " + buildSearchIndex);
        if(buildSearchIndex) {
            System.out.println("==================================================================");
            System.out.println("                     Building Search Index");
            System.out.println("==================================================================");
            File indexDir = new File(ensembleDir,"src/generated/resources/ensemble/search/index");
            indexDir.mkdirs();
            BuildEnsembleSearchIndex.buildSearchIndex(
                allSamples,
                "https://docs.oracle.com/javase/8/javafx/api/",
                "https://docs.oracle.com/javafx/index.html",
                indexDir);
        }
    }
}
