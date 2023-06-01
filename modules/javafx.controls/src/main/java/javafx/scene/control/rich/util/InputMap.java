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
import java.util.Objects;
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
// TODO move to public pkg (which one?) javafx.incubator.scene.control.input
public class InputMap {
    private static final Object USER = new Object();

    private static record Entry(Object owner, Object value) { }

    // KeyBinding -> Entry with functionTag(Object)
    // functionTag(Object) -> Entry with Runnable
    private final HashMap<Object,Entry> map = new HashMap<>();

    public InputMap() {
    }

    /**
     * Adds a user-specified function under the given function tag.
     * This function will override any function set by a skin.
     */
    public void func(Object tag, Runnable function) {
        if (tag instanceof KeyBinding) {
            // prevent common misuse
            throw new IllegalArgumentException("use register() method to register a KeyBinding");
        } else if (tag instanceof Runnable) {
            throw new IllegalArgumentException("function tag cannot be a Runnable");
        }
        Objects.requireNonNull(tag, "tag must not be null");
        addFunction(USER, tag, function);
    }

    /**
     * Maps a function to the function tag, for use by a Skin.
     * This method will not override any previous mapping added by {@link #func(Object,Runnable)}.
     *
     * @param skin
     * @param tag
     * @param function
     */
    public void func(Skin<?> skin, Object tag, Runnable function) {
        Objects.requireNonNull(skin, "skin must not be null");
        addFunction(skin, tag, function);
    }
    
    /**
     * Link a key binding to the specified function tag.
     * This method will override any previous mappings set by a Skin.
     *
     * @param k
     * @param tag
     */
    public void key(KeyBinding k, Object tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(USER, k, tag);
    }
    
    /**
     * Link a key binding to the specified function tag, for use by a Skin.
     * A null key binding will result in no change to this input map.
     * This method will not override any previous mappings added by {@link #key(KeyBinding,Object)}.
     *
     * @param skin
     * @param k key binding, can be null
     * @param tag function tag
     */
    public void key(Skin<?> skin, KeyBinding k, Object tag) {
        if (k == null) {
            return;
        }
        Objects.requireNonNull(skin, "skin must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(skin, k, tag);
    }

    public void key(Skin<?> skin, KeyCode k, Object tag) {
        key(skin, KeyBinding.of(k), tag);
    }
    
    private void addFunction(Object owner, Object tag, Runnable function) {
        Entry en = map.get(tag);
        if (canOverride(owner, en)) {
            map.put(tag, new Entry(owner, function));
        }
    }

    private void addBinding(Object owner, KeyBinding k, Object tag) {
        Entry en = map.get(k);
        if (canOverride(owner, en)) {
            map.put(k, new Entry(owner, tag));
        }
    }

    // TODO add an entry to revert the change!  if en != null
    private static boolean canOverride(Object owner, Entry en) {
        if (owner != USER) {
            if (en != null) {
                if (en.owner() == USER) {
                    return false;
                }
            }
        }
        return true;
    }

    /** returns a Runnable function object for the given function tag or KeyBinding.  Might return null. */
    public Runnable getFunction(Object k) {
        Entry en = map.get(k);
        if (en != null) {
            Object v = en.value();
            if (v instanceof Runnable r) {
                return r;
            } else if (v != null) {
                // try an action tag
                en = map.get(v);
                if (en != null) {
                    Object f = en.value();
                    if (f instanceof Runnable r) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    public void unregister(Skin<?> skin) {
        // TODO
    }

    public void restoreDefaultBinding(KeyBinding b) {
        // TODO
    }
    
    public void restoreDefaultFunction(Object b) {
        // TODO
    }
    
    public void clearAllBindings() {
        // TODO
    }
}
