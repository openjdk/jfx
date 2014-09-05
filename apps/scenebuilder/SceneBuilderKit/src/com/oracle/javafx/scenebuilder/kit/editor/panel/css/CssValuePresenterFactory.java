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
package com.oracle.javafx.scenebuilder.kit.editor.panel.css;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

/**
 *
 * @treatAsPrivate
 */
public class CssValuePresenterFactory {

    public abstract class CssValuePresenter<T> {

        private final T value;
        private Node node;
        protected CssValuePresenter(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public Node getCustomPresenter() {
            if (node == null) {
                node = doGetPresenter();
            }
            return node;
        }
        
        public Label getTextPresenter() {
            return new Label(CssValueConverter.toCssString(value));
        }

        protected abstract Node doGetPresenter();
    }
    
    private static final CssValuePresenterFactory singleton = new CssValuePresenterFactory();

    protected CssValuePresenterFactory() {
    }

    public static CssValuePresenterFactory getInstance() {
        return singleton;
    }

    public <T> CssValuePresenter<T> newValuePresenter(T value) {
        final CssValuePresenter<?> ret;
        if(Paint.class.isAssignableFrom(value.getClass())){
            ret = new PaintValuePresenter((Paint)value);
        } else {
            if(value instanceof Image){
                ret = new ImageValuePresenter((Image) value);
            } else {
                ret = new DefaultValuePresenter(value);
            }
        }
        @SuppressWarnings("unchecked")
        final CssValuePresenter<T> castedRet = (CssValuePresenter<T>)ret;
        return castedRet;
    }
    
    private class PaintValuePresenter extends CssValuePresenter<Paint> {
        private PaintValuePresenter(Paint p){
            super(p);
        }
        
        @Override
        protected Node doGetPresenter() {
            Rectangle rect = new Rectangle(10, 10);
            rect.setStroke(Color.BLACK);
            rect.setFill(getValue());
            return rect;
        }
    }
    
    private class ImageValuePresenter extends CssValuePresenter<Image> {
        private ImageValuePresenter(Image img){
            super(img);
        }
        
        @Override
        protected Node doGetPresenter() {
            ImageView imgView = new ImageView((getValue()));
            imgView.setFitWidth(15);
            imgView.setPreserveRatio(true);
            return imgView;
        }
    }
    
    private class DefaultValuePresenter extends CssValuePresenter<Object> {
        private DefaultValuePresenter(Object val){
            super(val);
        }
        @Override
        protected Node doGetPresenter() {
            return null;
        }
    }
}
