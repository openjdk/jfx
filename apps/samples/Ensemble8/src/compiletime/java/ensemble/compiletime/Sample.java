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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.application.ConditionalFeature;

/**
 * Descriptor for a ensemble sample. Everything the ui needs is determined at
 * compile time from the sample sources and stored in these Sample objects so
 * we don't have to calculate anything at runtime.
 */
public class Sample {

    // =============== BASICS ==================================================

    public String name;
    public String description;
    public String ensemblePath;

//    private final String packageName;
//    private final String className;

    // =============== SOURCES & RESOURCES =====================================

    /** The base URI for all the source files and resources for this sample. */
    public String baseUri;
    /** All the files needed by this sample. Relative to the sample base URI. */
    public List<String> resourceUrls = new ArrayList<>();
    /** The URL for the source of the sample main file. Relative to the sample base URI */
    public String mainFileUrl;
    /** Full classpath for sample's application class */
    public String appClass;
    /** ClassPath Url for preview image of size 206x152 */
    public String previewUrl;
    /** List of properties in the sample that can be played with */
    public final List<PlaygroundProperty> playgroundProperties = new ArrayList<>();
    /** List of conditional features the platform must support to run certain samples */
    public final List<ConditionalFeature> conditionalFeatures = new ArrayList<>();
    /** If true, then the sample runs on embedded platform  */
    public boolean runsOnEmbedded = false;

    // =============== RELATED =================================================

    /** Array of classpaths to related api docs. */
    public List<String> apiClasspaths = new ArrayList<>();
    /** Array of urls to related (non-api) docs. */
    public List<URL> docsUrls = new ArrayList<>();
    /** Array of ensemble paths to related samples. */
    public List<String> relatesSamplePaths = new ArrayList<>();

    @Override public String toString() {
        return "Sample{" +
                 "\n         name                 =" + name +
                ",\n         description          =" + description +
                ",\n         ensemblePath         =" + ensemblePath +
                ",\n         previewUrl           =" + previewUrl +
                ",\n         baseUri              =" + baseUri +
                ",\n         resourceUrls         =" + resourceUrls +
                ",\n         mainFileUrl          =" + mainFileUrl +
                ",\n         appClass             =" + appClass +
                ",\n         apiClasspaths        =" + apiClasspaths +
                ",\n         docsUrls             =" + docsUrls +
                ",\n         relatesSamplePaths   =" + relatesSamplePaths +
                ",\n         playgroundProperties =" + playgroundProperties +
                ",\n         conditionalFeatures  =" + conditionalFeatures +
                ",\n         runsOnEmbedded       =" + runsOnEmbedded +
                '}';
    }

    public static class URL {
        public final String url;
        public final String name;

        public URL(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public URL(String raw) {
            int index = raw.indexOf(' ');
            if (index == -1) {
                name = url = raw;
            } else {
                url = raw.substring(0, index);
                name = raw.substring(index + 1);
            }
        }
    }

    public static class PlaygroundProperty {
        public final String fieldName;
        public final String propertyName;
        public final Map<String,String> properties;

        public PlaygroundProperty(String fieldName, String propertyName, Map<String,String> properties) {
            this.fieldName = fieldName;
            this.propertyName = propertyName;
            this.properties = properties;
        }

        @Override public String toString() {
            return fieldName+"."+propertyName+" ("+properties+")";
        }
    }
}
