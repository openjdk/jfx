/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import netscape.javascript.JSException;

class JSObjectIosImpl extends netscape.javascript.JSObject {
    
    final JS2JavaBridge owner;
    final String jsObjectId; // JSObject's index to exportedObjects[] JS Array
    
    JSObjectIosImpl(JS2JavaBridge owner, String id) {
        this.owner = owner;
        this.jsObjectId = id;
    }
    
    @Override
    public Object eval(String script) throws JSException {
        if (script == null) {
            throw new NullPointerException("JSObject.eval() called with 'null' script.");
        }
        return owner.executeScript(script);
    }
    
    @Override
    public Object getMember(String name) {
        if (name == null) {
            throw new NullPointerException("JSObject.getMember() called with 'null' member name.");
        }
        StringBuilder sb = toScript();
        sb.append("[\"").append(name).append("\"]");
        return eval(sb.toString());
    }

    @Override
    public void setMember(String name, Object value) throws JSException {
        if (name == null) {
            throw new NullPointerException("JSObject.setMember() called with 'null' member name.");
        }

        StringBuilder sb = toScript();
        sb.append("[\"").append(name).append("\"]=");
        appendObjectToScript(value, sb);
        eval(sb.toString());
    }

    @Override
    public void removeMember(String name) throws JSException {
        if (name == null) {
            throw new NullPointerException("JSObject.removeMember() called with 'null' member name.");
        }
        StringBuilder sb = new StringBuilder("delete ");
        sb.append(toScript().toString());
        sb.append(".").append(name);
        eval(sb.toString());
    }
    
    @Override
    public Object getSlot(int index) throws JSException {
        StringBuilder sb = toScript();
        sb.append("[").append(index).append("]");
        return eval(sb.toString());
    }

    @Override
    public void setSlot(int index, Object value) throws JSException {
        StringBuilder sb = toScript();
        sb.append("[").append(index).append("]=");
        appendObjectToScript(value, sb);
        eval(sb.toString());
    }

    StringBuilder toScript() {
        StringBuilder script = new StringBuilder();
        script.append(owner.getJavaBridge()).append(".exportedJSObjects[").append(jsObjectId).append("]");
        return script;
    }
    
    @Override
    public Object call(String methodName, Object... args) throws JSException {
        if (methodName == null) {
            throw new NullPointerException("JSObject.call() called with 'null' methodName");
        }
        
        StringBuilder script = toScript();
        script.append(".").append(methodName).append("(");
        int i = 1;
        for(Object arg : args) {
            appendObjectToScript(arg,script);
            if (i++ < args.length) {
                script.append(",");
            }
        }
        script.append(")");
        return owner.executeScript(script.toString());
    }

    @Override
    public String toString() {
        Object ret = call("toString");
        if (ret != null) {
            return ret.toString();
        }
        return "null"; // shouldn't happen
    }
    
    @Override
    public boolean equals(Object other) {
        return other == this
          || (other != null && other.getClass() == JSObjectIosImpl.class
              && jsObjectId.equals(((JSObjectIosImpl) other).jsObjectId));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.jsObjectId != null ? this.jsObjectId.hashCode() : 0);
        return hash;
    }

    private void appendObjectToScript(Object arg, StringBuilder script) {
        owner.encode(arg, script);
    }
}
