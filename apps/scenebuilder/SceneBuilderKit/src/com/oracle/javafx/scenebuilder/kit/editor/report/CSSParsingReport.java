/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.report;

import javafx.css.CssParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class CSSParsingReport {
    private final Path stylesheetPath;
    private IOException ioException;
    private final List<CssParser.ParseError> parseErrors = new ArrayList<>();

    public CSSParsingReport(Path stylesheetPath) {
        assert stylesheetPath != null;

        this.stylesheetPath = stylesheetPath;
        final Set<CssParser.ParseError> previousErrors = new HashSet<>(CssParser.errorsProperty());
        try {
            new CssParser().parse(stylesheetPath.toUri().toURL());
            // Leave this.ioException to null
            parseErrors.addAll(CssParser.errorsProperty());
            parseErrors.removeAll(previousErrors);
        } catch(IOException x) {
            this.ioException = x;
        } finally {
            CssParser.errorsProperty().removeAll(parseErrors);
        }
    }

    public Path getStylesheetPath() {
        return stylesheetPath;
    }

    public boolean isEmpty() {
        return (ioException == null) && parseErrors.isEmpty();
    }

    public IOException getIOException() {
        return ioException;
    }

    public List<CssParser.ParseError> getParseErrors() {
        return Collections.unmodifiableList(parseErrors);
    }
}
