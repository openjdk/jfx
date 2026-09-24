/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

/**
 * JFX implementation for {@code IDWriteTextAnalysisSink} and {@code IDWriteTextAnalysisSource}.
 * <p>
 * The object itself is built by {@link DWNative}: two synthesized COM vtables over one block of
 * memory, with the source subinterface eight bytes into it, exactly as the C++ object had them
 * (it declared its bases in the order sink, source). <b>The inherited {@code ptr} is therefore a
 * registry id, not a COM pointer</b>: {@link #sinkPointer} and {@link #sourcePointer} are what
 * DirectWrite may be handed, and they are two <em>different</em> pointers to the same object.
 * <p>
 * The C++ object deleted itself when its reference count reached zero, so its Java peer needed no
 * disposal. This one owns a confined {@code Arena} that nothing else can close, so
 * {@link #dispose} is mandatory and must run on the thread that created the object -
 * {@code DWGlyphLayout.addTextRun} does it in a {@code finally}.
 */
class JFXTextAnalysisSink extends IUnknown {

    private JFXTextAnalysisSink(long id) {
        super(id);
    }

    /**
     * The replacement for {@code OS._NewJFXTextAnalysisSink}: the text is copied from {@code start}
     * for {@code length} UTF-16 units without a terminator, the locale whole with one. {@code null}
     * when the object could not be made, which {@code DWGlyphLayout} turns into an unshaped run.
     */
    static JFXTextAnalysisSink create(char[] text, int start, int length, String locale,
                                      int direction) {
        long id = DWNative.newAnalysisSink(text, start, length, (locale + '\0').toCharArray(),
                                           direction, 0);
        return id != 0 ? new JFXTextAnalysisSink(id) : null;
    }

    /** The {@code IDWriteTextAnalysisSink*} DirectWrite writes its runs into. */
    long sinkPointer() {
        return DWNative.analysisSinkPointer(ptr);
    }

    /**
     * The {@code IDWriteTextAnalysisSource*} DirectWrite reads the text through - <b>not</b> the
     * same pointer as {@link #sinkPointer}. Handing the sink pointer here dispatches
     * {@code GetTextAtPosition} into {@code SetScriptAnalysis}, which is how the C++ object's
     * implicit base conversion first showed itself: as an access violation inside dwrite.dll.
     */
    long sourcePointer() {
        return DWNative.analysisSourcePointer(ptr);
    }

    @Override
    int AddRef() {
        return DWNative.analysisSinkAddRef(ptr);
    }

    /**
     * The count DirectWrite shares, decremented. Unlike the base class this does not forget the
     * object: reaching zero frees nothing here, because the arena is the owner and {@link #dispose}
     * is what closes it.
     */
    @Override
    int Release() {
        return ptr != 0 ? DWNative.analysisSinkRelease(ptr) : 0;
    }

    /** Frees the object. Idempotent, and must run on the thread that called {@link #create}. */
    void dispose() {
        if (ptr != 0) {
            DWNative.disposeAnalysisSink(ptr);
            ptr = 0;
        }
    }

    boolean Next() {
        return DWNative.analysisSinkNext(ptr);
    }

    int GetStart() {
        return DWNative.analysisSinkGetStart(ptr);
    }

    int GetLength() {
        return DWNative.analysisSinkGetLength(ptr);
    }

    DWRITE_SCRIPT_ANALYSIS GetAnalysis() {
        return DWNative.analysisSinkGetAnalysis(ptr);
    }
}
