/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.rich.util;

import java.util.HashMap;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;

/**
 * Input Map maps KeyBindings(2) to function tags (any object except Runnable),
 * followed by mapping from tags to the actual Runnable function.
 * 
 * The input map may not be limited to a keyboard event, so looking up a function from a function tag for a
 * built-in functionality such as copy, paste, etc. is also permitted.
 *  
 * Example:
 *  
 * Control:
 * - declares function tags (any object, typically an enum)
 * - declares public methods that execute using function id, which in turn find and execute corresponding function
 * - might declare public FxActions (ex.: copyAction which delegate to action id)
 * Behavior:
 * - maps key bindings to action ids
 * - maps action ids to methods in the behavior
 */
// TODO rename, move to public pkg (which one?)
public class InputMap2 {
    private static final Object USER = new Object();
    // keyBinding2 -> tag
    // tag -> Runnable
    // TODO Entry(owner, runnable or tag)
    private final HashMap<Object,Object> map = new HashMap<>();

    public InputMap2() {
    }
    
    public void assign(Object tag, Runnable function) {
        assignLocal(USER, tag, function);
    }
    
    public void assign(Skin<?> skin, Object tag, Runnable function) {
        assignLocal(skin, tag, function);
    }
    
    private void assignLocal(Object owner, Object tag, Runnable function) {
        
    }

    /** adds a mapping: tag -> function */
    public void add(Object tag, Runnable function) {
        map.put(tag, function);
    }

    /** adds a mapping: keyBinding -> tag */
    public void add(Object actionTag, KeyCode code, KCondition... modifiers) {
        // TODO check for nulls
        KeyBinding2 k = KeyBinding2.of(code, modifiers);
        if (k != null) {
            map.put(k, actionTag);
        }
    }

    // TODO or make KeyBinding2 class public with a bunch of factory methods
    // TODO should take additional FxAction argument instead of Runnable?
    /** adds a mapping: keyBinding -> actionTag; and actionTag -> function */
    public void add(Object actionTag, Runnable function, KeyCode code, KCondition... modifiers) {
        // TODO check for nulls
        KeyBinding2 k = KeyBinding2.of(code, modifiers);
        if (k != null) {
            map.put(k, actionTag);
            map.put(actionTag, function);
        }
    }

    /** returns a Runnable function object for the given function tag or KeyBinding.  Might return null. */
    public Runnable getFunction(Object k) {
        Object v = map.get(k);
        if (v instanceof Runnable r) {
            return r;
        } else if(v != null) {
            // try an action tag
            Object f = map.get(v);
            if(f instanceof Runnable r) {
                return r;
            }
        }
        return null;
    }

    public void add(Skin<?> owner, Object actionTag, Runnable function, KeyCode code, KCondition... modifiers) {
        // TODO check if user mapping exists
    }
    
    public void add(Skin<?> owner, Object actionTag, KeyCode code, KCondition... modifiers) {
        // TODO
    }

    public void add(Skin<?> owner, Object tag, Runnable function) {
        // TODO
    }

    public void unregister(Skin<?> skin) {
        // TODO
    }

    public void restoreDefaultBinding(KeyBinding2 b) {
        // TODO
    }
    
    public void restoreDefaultFunction(Object b) {
        // TODO
    }
    
    public void clearAllBindings() {
        // TODO
    }
}
