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

import javafx.scene.paint.Color;

/**
 *
 */
abstract class AbstractSampleData {
    
    private static final String[] lorem = {
        "Lorem ipsum ", //NOI18N
        "dolor sit amet, ", //NOI18N
        "consectetur adipiscing elit. ", //NOI18N
        "Donec eu justo ", //NOI18N
        "at tortor porta ", //NOI18N
        "commodo nec vitae magna. ", //NOI18N
        "Maecenas tempus ", //NOI18N
        "hendrerit elementum. ", //NOI18N
        "Nam sed mi ", //NOI18N
        "a lorem tincidunt ", //NOI18N
        "luctus sed non sem. ", //NOI18N
        "Aliquam erat volutpat. ", //NOI18N
        "Donec tempus egestas ", //NOI18N
        "libero a cursus. ", //NOI18N
        "In lectus nunc, ", //NOI18N
        "dapibus vel suscipit vel, ", //NOI18N
        "faucibus eget justo. ", //NOI18N
        "Aliquam erat volutpat. ", //NOI18N
        "Nulla facilisi. ", //NOI18N
        "Donec at enim ipsum, ", //NOI18N
        "sed facilisis leo. ", //NOI18N
        "Aliquam tincidunt ", //NOI18N
        "adipiscing euismod. ", //NOI18N
        "Sed aliquet eros ", //NOI18N
        "ut libero congue ", //NOI18N
        "quis bibendum ", //NOI18N
        "felis ullamcorper. ", //NOI18N
        "Vestibulum ipsum ante, ", //NOI18N
        "semper eu sollicitudin rutrum, ", //NOI18N
        "consectetur a enim. ", //NOI18N
        "Ut eget nisl sed turpis ", //NOI18N
        "egestas viverra ", //NOI18N
        "ut tristique sem. ", //NOI18N
        "Nunc in neque nulla. " //NOI18N
    };
    
    private final static Color[] colors = {
        Color.AZURE, Color.CHARTREUSE, Color.CRIMSON, Color.DARKCYAN
    };
    
    private static final String[] alphabet = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", //NOI18N
        "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" //NOI18N
    };
    
    public abstract void applyTo(Object sceneGraphObject);
    public abstract void removeFrom(Object sceneGraphObject);
    
    
    /*
     * Utilites for subclasses
     */
    
    protected static String lorem(int index) {
        return lorem[index % lorem.length];
    }
    
    protected static Color color(int index) {
        return colors[index % colors.length];
    }

    protected static String alphabet(int index) {
        return alphabet[index % alphabet.length];
    }
}
