/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import java.nio.ByteBuffer;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

public class CustomTransfer extends ByteArrayTransfer {
    private String name, mime;
    
    public CustomTransfer (String name, String mime) {
        this.name = name;
        this.mime = mime;
    }
    
    public String getName () {
        return name;
    }
    
    public String getMime () {
        return mime;
    }

    public void javaToNative (Object object, TransferData transferData) {
        if (!checkCustom(object) || !isSupportedType(transferData)) {
            DND.error(DND.ERROR_INVALID_DATA);
        }
        byte [] bytes = null;
        if (object instanceof ByteBuffer) {
            bytes = ((ByteBuffer)object).array();
        } else {
            if (object instanceof byte []) bytes = (byte []) object;
        }
        if (bytes == null) DND.error(DND.ERROR_INVALID_DATA);
        super.javaToNative(bytes, transferData);
    }
    
    public Object nativeToJava(TransferData transferData){  
        if (isSupportedType(transferData)) {
            Object result = super.nativeToJava(transferData);
            if (result instanceof byte []) {
                return ByteBuffer.wrap((byte []) result);
            }
        }
        return null;
    }
    
    protected String[] getTypeNames(){
        return new String [] {name};
    }
    
    protected int[] getTypeIds(){
        return new int [] {registerType(name)};
    }
    
    boolean checkByteArray(Object object) {
        return (object != null && object instanceof byte[] && ((byte[])object).length > 0);
    }
    
    boolean checkByteBuffer(Object object) {
        return (object != null && object instanceof ByteBuffer && ((ByteBuffer)object).limit() > 0);
    }
    
    boolean checkCustom(Object object) {
        return checkByteArray(object) || checkByteBuffer(object);
    }
    
    protected boolean validate(Object object) {
        return checkCustom(object);
    }
}
