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
package com.oracle.javafx.scenebuilder.kit.fxom.sampledata;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 *
 */
class SpinnerSampleData extends AbstractSampleData {

    private final List<String> samples = new ArrayList<>();
    private SpinnerValueFactory<String> valueFactory;
    private int index = 0;
    private static final int ALPHABET_SIZE = 26;

    public SpinnerSampleData() {
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            samples.add(alphabet(i));
        }
    }

    /*
     * AbstractSampleData
     */
    @Override
    public void applyTo(Object sceneGraphObject) {
        assert sceneGraphObject != null;

        @SuppressWarnings("unchecked")
        final Spinner<String> spinner = (Spinner<String>) sceneGraphObject;
        valueFactory = spinner.getValueFactory();
        spinner.setValueFactory(new SpinnerValueFactory<String>() {

            @Override
            public void decrement(int steps) {
                index = Math.max((index - 1), 0);
                setValue(samples.get(index));
            }

            @Override
            public void increment(int steps) {
                index = Math.min((index + 1), ALPHABET_SIZE - 1);
                setValue(samples.get(index));
            }
        });
        assert index == 0;
        spinner.getValueFactory().setValue(samples.get(index));
    }

    @Override
    public void removeFrom(Object sceneGraphObject) {
        assert sceneGraphObject != null;

        @SuppressWarnings("unchecked")
        final Spinner<String> spinner = (Spinner<String>) sceneGraphObject;
        spinner.setValueFactory(valueFactory);
    }

}
