/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.css.parser;

import java.util.List;
import java.util.function.Predicate;

public final class TokenStream {

    private final List<Token> source;
    private final int size;
    private int currentIndex = -1;
    private Token currentItem;

    public TokenStream(List<Token> source) {
        this.source = source;
        this.size = source.size();
    }

    public int size() {
        return size;
    }

    public int index() {
        return currentIndex;
    }

    public Token current() {
        return currentItem;
    }

    public Token consume() {
        if (currentIndex < size - 1) {
            return currentItem = source.get(++currentIndex);
        }

        if (currentIndex < size) {
            currentIndex++;
        }

        return null;
    }

    public Token consume(Predicate<Token> predicate) {
        Token nextToken = consume();
        if (nextToken != null && predicate.test(nextToken)) {
            return nextToken;
        }

        reconsume();
        return null;
    }

    public void reconsume() {
        if (currentIndex > 0) {
            currentItem = source.get(--currentIndex);
        } else {
            currentItem = null;
            currentIndex = -1;
        }
    }

    public Token peek() {
        return currentIndex < size - 1 ? source.get(currentIndex + 1) : null;
    }

    /**
     * Returns whether the next tokens in the stream satisfy the specified predicates.
     */
    @SafeVarargs
    public final boolean peekMany(Predicate<Token>... predicates) {
        int index = index();

        try {
            for (Predicate<Token> predicate : predicates) {
                if (consume(predicate) == null) {
                    return false;
                }
            }

            return true;
        } finally {
            reset(index);
        }
    }

    public void reset(int index) {
        currentIndex = index;
        currentItem = index >= 0 ? source.get(index) : null;
    }
}