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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import javafx.scene.input.KeyEvent;

/**
 *
 * 
 */
public class EditCurveGesture extends AbstractMouseGesture {
    
    public enum Tunable {
        START,
        END,
        CONTROL1,
        CONTROL2
    }
    
    private final Tunable tunable;

    public EditCurveGesture(ContentPanelController contentPanelController, Tunable tunable) {
        super(contentPanelController);
        this.tunable = tunable;
    }

    public Tunable getTunable() {
        return tunable;
    }
    
    /*
     * AbstractMouseGesture
     */
    
    @Override
    protected void mousePressed() {
        System.out.println("EditCurveGesture.mousePressed: tunable=" + tunable);
    }

    @Override
    protected void mouseDragStarted() {
        System.out.println("EditCurveGesture.mouseDragStarted: tunable=" + tunable);
    }

    @Override
    protected void mouseDragged() {
        System.out.println("EditCurveGesture.mouseDragged: tunable=" + tunable);
    }

    @Override
    protected void mouseDragEnded() {
        System.out.println("EditCurveGesture.mouseDragEnded: tunable=" + tunable);
    }

    @Override
    protected void mouseReleased() {
        System.out.println("EditCurveGesture.mouseReleased: tunable=" + tunable);
    }

    @Override
    protected void keyEvent(KeyEvent e) {
         System.out.println("EditCurveGesture.keyEvent: tunable=" + tunable);
   }

    @Override
    protected void userDidCancel() {
         System.out.println("EditCurveGesture.keyEvent: tunable=" + tunable);
    }
    
}
