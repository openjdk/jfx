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

package com.oracle.javafx.scenebuilder.kit.metadata.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;

/**
 *
 */
public class PrefixedValue {
    
    public enum Type {
        DOCUMENT_RELATIVE_PATH,
        CLASSLOADER_RELATIVE_PATH,
        RESOURCE_KEY,
        EXPRESSION,
        PLAIN_STRING,
        INVALID
    }
    
    private final String value;
    private final Type type;
    
    public PrefixedValue(String value) {
        assert value != null;
        this.value = value;
        this.type = getPrefixedValueType(this.value);
    }
    
    public PrefixedValue(Type type, String suffix) {
        assert type != Type.INVALID;
        assert suffix != null;
        
        this.type = type;
        switch(this.type) {
            case DOCUMENT_RELATIVE_PATH: {
                final String encoding = encodePath(new File(suffix));
                this.value = FXMLLoader.RELATIVE_PATH_PREFIX + encoding;
                break;
            }
            case CLASSLOADER_RELATIVE_PATH: {
                final String encoding = encodePath(new File(suffix));
                this.value = FXMLLoader.RELATIVE_PATH_PREFIX + "/" + encoding; //NOI18N
                break;
            }
            case RESOURCE_KEY: {
                this.value = FXMLLoader.RESOURCE_KEY_PREFIX + suffix; //NOI18N
                break;
            }
            case EXPRESSION: {
                this.value = FXMLLoader.EXPRESSION_PREFIX + suffix; //NOI18N
                break;
            }
            case PLAIN_STRING: {
                if (suffix.startsWith(FXMLLoader.ESCAPE_PREFIX) 
                        || suffix.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX) 
                        || suffix.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX)
                        || suffix.startsWith(FXMLLoader.EXPRESSION_PREFIX)) {
                    this.value = FXMLLoader.ESCAPE_PREFIX + suffix;
                } else {
                    this.value = suffix;
                }
                break;
            }
            default:
            case INVALID: {
                // Emergency code
                throw new IllegalArgumentException("Unexpected type " + Type.INVALID); //NOI18N
            }
        }
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean isDocumentRelativePath() {
        return type == Type.DOCUMENT_RELATIVE_PATH;
    }
    
    public boolean isClassLoaderRelativePath() {
        return type == Type.CLASSLOADER_RELATIVE_PATH;
    }
    
    public boolean isResourceKey() {
        return type == Type.RESOURCE_KEY;
    }
    
    public boolean isExpression() {
        return type == Type.EXPRESSION;
    }
    
    public boolean isPlainString() {
        return type == Type.PLAIN_STRING;
    }
    
    public boolean isInvalid() {
        return type == Type.INVALID;
    }
    
    public String getSuffix() {
        final String result;
        
        switch(this.type) {
            case DOCUMENT_RELATIVE_PATH: {
                assert value.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX);
                final String encoding = value.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length());
                result = decodePath(encoding).getPath();
                break;
            }
            case CLASSLOADER_RELATIVE_PATH: {
                assert value.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX+"/"); //NOI18N
                final String encoding = value.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length()+1);
                result = decodePath(encoding).getPath();
                break;
            }
            case RESOURCE_KEY: {
                assert value.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX);
                result = value.substring(FXMLLoader.RESOURCE_KEY_PREFIX.length());
                break;
            }
            case EXPRESSION: {
                assert value.startsWith(FXMLLoader.EXPRESSION_PREFIX);
                result = value.substring(FXMLLoader.EXPRESSION_PREFIX.length());
                break;
            }
            case PLAIN_STRING: {
                if (value.startsWith(FXMLLoader.ESCAPE_PREFIX)) {
                    result = value.substring(FXMLLoader.ESCAPE_PREFIX.length());
                } else if (value.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX+FXMLLoader.RELATIVE_PATH_PREFIX)) {
                    result = value.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length());
                } else if (value.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX+FXMLLoader.RESOURCE_KEY_PREFIX)) {
                    result = value.substring(FXMLLoader.RESOURCE_KEY_PREFIX.length());
                } else if (value.startsWith(FXMLLoader.EXPRESSION_PREFIX+FXMLLoader.EXPRESSION_PREFIX)) {
                    result = value.substring(FXMLLoader.EXPRESSION_PREFIX.length());
                } else {
                    result = value;
                }
                break;
            }
            default:
            case INVALID: {
                // Emergency code
                result = null;
            }
        }
        
        return result;
    }
    
    public URL resolveDocumentRelativePath(URL document) {
        assert document != null;
        assert type == Type.DOCUMENT_RELATIVE_PATH;
        
        URL result;
        try {
            final String path = value.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length());
            result = new URL(document, path);
        } catch(MalformedURLException x) {
            result = null;
        }
        
        return result;
    }
    
    public URL resolveClassLoaderRelativePath(ClassLoader classLoader) {
        assert classLoader != null;
        assert type == Type.CLASSLOADER_RELATIVE_PATH;
        assert value.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX+"/");
        
        final String path = value.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length()+1);
        return classLoader.getResource(path);
    }
    
    public String resolveResourceKey(ResourceBundle resources) {
        assert resources != null;
        assert type == Type.RESOURCE_KEY;
        assert value.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX);
        
        String result;
        try {
            final String key = value.substring(FXMLLoader.RESOURCE_KEY_PREFIX.length());
            result = resources.getString(key);
        } catch(MissingResourceException x) {
            result = null;
        }
        
        return result;
    }
    
    public static Type getPrefixedValueType(String prefixedValue) {
        final Type result;
        
        String v = prefixedValue;
        if (v.startsWith(FXMLLoader.ESCAPE_PREFIX)) {
            v = v.substring(FXMLLoader.ESCAPE_PREFIX.length());
            if (v.isEmpty()
                || !(v.startsWith(FXMLLoader.ESCAPE_PREFIX)
                    || v.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX)
                    || v.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX)
                    || v.startsWith(FXMLLoader.EXPRESSION_PREFIX)
                    || v.startsWith(FXMLLoader.BI_DIRECTIONAL_BINDING_PREFIX))) {
                result = Type.INVALID;
            } else {
                result = Type.PLAIN_STRING;
            }
        } else if (v.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX)) {
            v = v.substring(FXMLLoader.RELATIVE_PATH_PREFIX.length());
            if (v.isEmpty()) {
                result = Type.INVALID;
            } else if (v.startsWith(FXMLLoader.RELATIVE_PATH_PREFIX)) {
                // The prefix was escaped
                result = Type.PLAIN_STRING;
            } else if (v.charAt(0) == '/') {
                result = Type.CLASSLOADER_RELATIVE_PATH;
            } else {
                result = Type.DOCUMENT_RELATIVE_PATH;
            }
        } else if (v.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX)) {
            v = v.substring(FXMLLoader.RESOURCE_KEY_PREFIX.length());
            if (v.isEmpty()) {
                result = Type.INVALID;
            } else if (v.startsWith(FXMLLoader.RESOURCE_KEY_PREFIX)) {
                // The prefix was escaped
                result = Type.PLAIN_STRING;
            } else {
                result = Type.RESOURCE_KEY;
            }
        } else if (v.startsWith(FXMLLoader.EXPRESSION_PREFIX)) {
            v = v.substring(FXMLLoader.EXPRESSION_PREFIX.length());
            if (v.isEmpty()) {
                result = Type.INVALID;
            } else if (v.startsWith(FXMLLoader.EXPRESSION_PREFIX)) {
                // The prefix was escaped
                result = Type.PLAIN_STRING;
            } else {
                result = Type.EXPRESSION;
            }
        } else {
            result = Type.PLAIN_STRING;
        }

        return result;
    }
    
    public static PrefixedValue makePrefixedValue(URL assetURL, URL documentURL) {
        
        final File assetFile, documentFile;
        try {
            assetFile = new File(assetURL.toURI());
            documentFile = new File(documentURL.toURI());
        } catch(URISyntaxException x) {
            throw new IllegalArgumentException(x);
        }
        final File parentFile = documentFile.getParentFile();
        
        final PrefixedValue result;
        if ((parentFile == null) || parentFile.equals(assetFile)) {
            throw new IllegalArgumentException(documentURL.toString());
        } else {
            final Path relativePath = parentFile.toPath().relativize(assetFile.toPath());
            result = new PrefixedValue(Type.DOCUMENT_RELATIVE_PATH, relativePath.toString());
        }
        
        return result;
    }
    
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(getSuffix());
        hash = 97 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PrefixedValue other = (PrefixedValue) obj;
        if (!Objects.equals(this.getSuffix(), other.getSuffix())) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
    
    
    /*
     * Private
     */
        
    private static String encodePath(File file) {
        final String result;
        
        try {
            if (file.isAbsolute()) {
                result = file.toURI().toURL().getPath();
            } else {
                final Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir")); //NOI18N
                final String tmpPathEncoding = tmpPath.toFile().toURI().toURL().getPath();
                final Path absolutePath = tmpPath.resolve(file.toPath());
                final String absoluteEncoding = absolutePath.toFile().toURI().toURL().getPath();
                assert absoluteEncoding.startsWith(tmpPathEncoding);
                result = absoluteEncoding.substring(tmpPathEncoding.length());
            }
        } catch(MalformedURLException x) {
            throw new IllegalStateException(x);
        }
        
        return result;
    }
    
    private static File decodePath(String encoding) {
        File result;
        
        try {
            if (encoding.startsWith("/")) { //NOI18N
                result = new File(new URI("file:" + encoding)) ; //NOI18N
            } else {
                final Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir")); //NOI18N
                final URL tmpPathURL = tmpPath.toFile().toURI().toURL();
                final URL absoluteURL = new URL(tmpPathURL.toString() + "/" + encoding); //NOI18N
                final File absoluteFile = new File(absoluteURL.toURI());
                final Path absolutePath = absoluteFile.toPath();
                assert absolutePath.startsWith(tmpPath);
                final Path relativePath = tmpPath.relativize(absolutePath);
                result = relativePath.toFile();
            }
            
            assert encoding.equals(encodePath(result));
            
        } catch(MalformedURLException | URISyntaxException x) {
            result = new File(encoding);
        }
        
        
        return result;
    }
}
