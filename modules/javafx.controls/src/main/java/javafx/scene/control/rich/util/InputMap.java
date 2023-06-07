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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;

/**
 * Input Map maps KeyBindings to function tags (any object except KeyBinding or Runnable),
 * followed by mapping from tags to the actual Runnable function.
 *  
 * Example:
 *  
 * Control:
 * - declares function tags (any object, typically an enum)
 * - declares public methods that execute using function id, which in turn find and execute corresponding function
 * - might declare public FxActions (ex.: copyAction which delegate to action id)
 * Skin:
 * - installs behavior mappings in Skin.install()
 * Behavior:
 * - maps key bindings to FunctionTags
 * - maps FunctionTags to methods in the behavior
 */
// TODO move to public pkg (which one?) javafx.incubator.scene.control.input
// TODO this can be renamed to KeyMap
public class InputMap {
    /** contains user- and skin-set key binding or function mappings */
    private static class Entry {
        Object userValue;
        Skin<?> skin;
        Object skinValue;

        public Object getValue() {
            if (userValue == NULL) {
                return null;
            } else if (userValue == null) {
                return skinValue;
            }
            return userValue;
        }
    }

    // KeyBinding -> Entry with value=FunctionTag
    // FunctionTag -> Entry with value=Runnable
    private final HashMap<Object,Entry> map = new HashMap<>();
    private static final Object NULL = new Object();

    public InputMap() {
    }

    /**
     * Adds a user-specified function under the given function tag.
     * This function will override any function set by the skin.
     */
    public void func(FunctionTag tag, Runnable function) {
        validateTag(tag);
        Objects.requireNonNull(function, "function must not be null");
        addFunction(tag, function, null);
    }

    /**
     * Maps a function to the function tag, for use by the skin.
     * This method will not override any previous mapping added by {@link #func(FunctionTag,Runnable)}.
     *
     * @param skin
     * @param tag
     * @param function
     */
    public void func(Skin<?> skin, FunctionTag tag, Runnable function) {
        Objects.requireNonNull(skin, "skin must not be null");
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(tag, function, skin);
    }
    
    /**
     * Link a key binding to the specified function tag.
     * This method will override a mapping set by the skin.
     *
     * @param k
     * @param tag
     */
    public void key(KeyBinding k, FunctionTag tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, null);
    }
    
    /**
     * Maps a key binding to the specified function tag, for use by the skin.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping added by {@link #key(KeyBinding,FunctionTag)}.
     *
     * @param skin
     * @param k key binding, can be null
     * @param tag function tag
     */
    public void key(Skin<?> skin, KeyBinding k, FunctionTag tag) {
        if (k == null) {
            return;
        }
        Objects.requireNonNull(skin, "skin must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, skin);
    }

    /**
     * Maps a key binding to the specified function tag, for use by the skin.
     * This method will not override a user mapping added by {@link #key(KeyBinding,FunctionTag)}.
     * 
     * @param skin
     * @param code key code to construct a {@link KeyBinding}
     * @param tag function tag
     */
    public void key(Skin<?> skin, KeyCode code, FunctionTag tag) {
        key(skin, KeyBinding.of(code), tag);
    }

    private void addFunction(FunctionTag tag, Runnable function, Skin<?> skin) {
        Entry en = map.get(tag);
        if (en == null) {
            en = new Entry();
            map.put(tag, en);
        }

        if (skin == null) {
            // user mapping
            en.userValue = function;
        } else {
            // skin mapping
            en.skin = skin;
            en.skinValue = function;
        }
    }

    private void addBinding(KeyBinding k, FunctionTag tag, Skin<?> skin) {
        Entry en = map.get(k);
        if (en == null) {
            en = new Entry();
            map.put(k, en);
        }
        
        if (skin == null) {
            // user mapping
            en.userValue = tag;
        } else {
            // skin mapping
            en.skin = skin;
            en.skinValue = tag;
        }
    }

    /**
     * Returns a {@code Runnable} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @param tag
     */
    public Runnable getFunction(FunctionTag tag) {
        Entry en = map.get(tag);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof Runnable r) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns a {@code Runnable} mapped to the specified {@link KeyBinding}, or null if no such mapping exists.
     *
     * @param k
     */
    public Runnable getFunction(KeyBinding k) {
        Entry en = map.get(k);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof FunctionTag tag) {
                return getFunction(tag);
            }
        }
        return null;
    }

    /**
     * Removes all the mappings set by the skin.
     *
     * @param skin
     */
    public void unregister(Skin<?> skin) {
        Objects.nonNull(skin);
        Iterator<Entry> it = map.values().iterator();
        while (it.hasNext()) {
            Entry en = it.next();
            if (en.skin == skin) {
                en.skin = null;
                en.skinValue = null;
            }
        }
    }

    /**
     * Unbinds the specified key binding.
     *
     * @param k
     */
    public void unbind(KeyBinding k) {
        Entry en = map.get(k);
        if (en != null) {
            en.userValue = NULL;
        }
    }

    /**
     * Resets all key bindings set by user to the values set by the skin, if any.
     */
    public void resetKeyBindings() {
        Iterator<Map.Entry<Object, Entry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Entry> me = it.next();
            if (me.getKey() instanceof KeyBinding) {
                Entry en = me.getValue();
                en.userValue = null;
            }
        }
    }

    /**
     * Restores the specified key binding to the value set by the skin, if any.
     *
     * @param k
     */
    public void restoreDefaultBinding(KeyBinding k) {
        Entry en = map.get(k);
        if (en != null) {
            en.userValue = null;
            if (en.skinValue == null) {
                map.remove(k);
            }
        }
    }

    /**
     * Restores the specified function tag to the value set by the skin, if any.
     *
     * @param tag
     */
    public void restoreDefaultFunction(FunctionTag tag) {
        validateTag(tag);
        Entry en = map.get(tag);
        if (en != null) {
            en.userValue = null;
            if (en.skinValue == null) {
                map.remove(tag);
            }
        }
    }

    private static void validateTag(FunctionTag tag) {
        if (tag instanceof KeyBinding) {
            // prevent common misuse
            throw new IllegalArgumentException("use key() method to register a KeyBinding");
        } else if (tag instanceof Runnable) {
            throw new IllegalArgumentException("function tag cannot be a Runnable");
        }
        Objects.requireNonNull(tag, "function tag must not be null");
    }

    /**
     * Collects all mapped key bindings (set either by the user or the skin).
     *
     * @return a Set of KeyBindings
     */
    public Set<KeyBinding> getKeyBindings() {
        return map.keySet().stream().
            filter((k) -> (k instanceof KeyBinding)).
            map((x) -> (KeyBinding)x).
            collect(Collectors.toSet());
    }
}
