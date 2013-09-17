/*
 * Copyright (c) 2012,2013 Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import java.lang.reflect.Array;


class JSONEncoder {
    
    private final JS2JavaBridge owner;
    
    public JSONEncoder(JS2JavaBridge owner) {
        this.owner = owner;
    }
    
    public String encode(Object object) {
        StringBuffer sb = new StringBuffer();
        encode(sb, object);
        return sb.toString();
    }
    
    private static char[] hexChars = "0123456789abcdef".toCharArray();
    
    private static void encodeString(StringBuffer sb, String s) {
        sb.append('"');
        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (Character.isLetterOrDigit(ch)) {
                        sb.append(ch);
                    } else {
                        // encode as unicode
                        sb.append("\\u");
                        sb.append(hexChars[(ch & 0xf000) >> 12]);
                        sb.append(hexChars[(ch & 0x0f00) >> 8]);
                        sb.append(hexChars[(ch & 0x00f0) >> 4]);
                        sb.append(hexChars[(ch & 0x000f)]);
                    }
                    break;
            }
        }
        sb.append('"');
    }
    
    private void encode(StringBuffer sb, Object object) {
        if (object == null) {
            sb.append("null");
        } else if (object instanceof String || object instanceof Character) {
            encodeString(sb, object.toString());
        } else if (object instanceof Number || object instanceof Boolean) {
            sb.append(object.toString());
        } else if (object.getClass().isArray()) {
            sb.append("[");
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                if (i>0) {
                    sb.append(",");
                }
                encode(sb, Array.get(object, i));
            }
            sb.append("]");
        } else if (object instanceof JSObjectIosImpl) {
            JSObjectIosImpl jsArg = (JSObjectIosImpl) object;
            sb.append(jsArg.toScript().toString());
        } else {
            encodeJavaObject(object, sb);
        }
    }

    //return true if we were able to find index into exportedJSObjects[] for 
    //passed object; false otherwise
    private boolean encodedJavaObject(Object object, StringBuffer sb) {
        String jsId = owner.getjsIdForJavaObject(object);
        if (jsId != null) {
            sb.append(owner.getJavaBridge()).append(".exportedJSObjects[").append(jsId).append("]"); //reuse object
            return true;
        }
        return false;
    }
    
    private void encodeJavaObject(Object object, StringBuffer sb) {
        if (!encodedJavaObject(object, sb)) {
            owner.exportObject("anyname",object);
            if (!encodedJavaObject(object, sb)) { // bridge was not exported yet
                ExportedJavaObject jsObj = owner.createExportedJavaObject(object);
                sb.append(jsObj.getJSDecl()); //create new object in JS
            }
        }
    }
}
