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
package ensemble.compiletime.search;

/**
 * Types of document stored in the search index
 */
public enum DocumentType {
    /**
     * Ensemble Sample,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : name of the sample
     *      description     : javadoc description (Not Stored)
     *      ensemblePath    : ensemble url
     */
    SAMPLE("Samples"),
    /**
     * Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : non fully qualified class name
     *      description     : javadoc description (Not Stored)
     *      url             : fully qualified oracle.com url
     *      package         : qualified package
     */
    CLASS("Classes"),
    /**
     * Single JavaFX Property of a Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : property name of style "translateX"
     *      description     : textual description (Not Stored)
     *      url             : fully qualified oracle.com url
     *      className       : non fully qualified
     *      package         : qualified package
     */
    PROPERTY("Properties"),
    /**
     * Single method of a Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : the name of the method, not including ()s
     *      description     : textual description (Not Stored)
     *      url             : fully qualified oracle.com url
     *      className       : non fully qualified
     *      package         : qualified package
     */
    METHOD("Methods"),
    /**
     * Single field of a Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : the name of the field
     *      description     : textual description (Not Stored)
     *      url             : fully qualified oracle.com url
     *      className       : non fully qualified
     *      package         : qualified package
     */
    FIELD("Fields"),
    /**
     * Single enum value of a Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      name            : name of the enum value, eg. "FIELD"
     *      description     : textual description (Not Stored)
     *      url             : fully qualified oracle.com url
     *      className       : non fully qualified
     *      package         : qualified package
     */
    ENUM("Enums"),
    /**
     * Single enum value of a Java Class,
     * fields:
     *      documentType    : Type of document, one of this enum's values
     *      bookTitle       : document title
     *      chapter         : document chapter
     *      sectionName     : document section
     *      sectionUrl      : fully qualified oracle.com url
     */
    DOC("Documentation");

    private final String pluralDisplayName;

    DocumentType(String pluralDisplayName) {
        this.pluralDisplayName = pluralDisplayName;
    }

    public String getPluralDisplayName() {
        return pluralDisplayName;
    }
}
