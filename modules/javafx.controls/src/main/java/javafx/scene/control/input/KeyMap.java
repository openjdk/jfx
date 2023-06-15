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

package javafx.scene.control.input;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.input.KeyCode;

/**
 * This class maps KeyBindings to function tags,
 * followed by mapping from tags to the actual Runnable function.
 *  
 * Example:
 *  
 * Control:
 * - declares function tags (any object, typically an enum)
 * - declares public methods that invoke mapped functions via the corresponding function tag
 * Skin:
 * - installs behavior mappings in Skin.install()
 * Behavior:
 * - maps key bindings to FunctionTags
 * - maps FunctionTags to methods in the behavior
 */
public class KeyMap {
    /** contains user- and skin-set key binding or function mappings */
    private static class Entry {
        Object userValue;
        IBehavior behavior;
        Object behaviorValue;

        public Object getValue() {
            if (userValue == NULL) {
                return null;
            } else if (userValue == null) {
                return behaviorValue;
            }
            return userValue;
        }
    }

    // KeyBinding -> Entry with value=FunctionTag
    // FunctionTag -> Entry with value=Runnable
    private final HashMap<Object,Entry> map = new HashMap<>();
    private static final Object NULL = new Object();

    public KeyMap() {
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
     * Maps a function to the function tag, for use by the behavior.
     * This method will not override any previous mapping added by {@link #func(FunctionTag,Runnable)}.
     *
     * @param behavior
     * @param tag
     * @param function
     */
    // TODO this method can be made package protected once BehaviorBase is moved to this pkg
    public void func(IBehavior behavior, FunctionTag tag, Runnable function) {
        Objects.requireNonNull(behavior, "skin must not be null");
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(tag, function, behavior);
    }
    
    /**
     * Link a key binding to the specified function tag.
     * This method will override a mapping set by the skin.
     *
     * @param k
     * @param tag
     */
    public void key(KeyBinding2 k, FunctionTag tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, null);
    }
    
    /**
     * Maps a key binding to the specified function tag, for use by the behavior.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param k key binding, can be null
     * @param tag function tag
     */
    // TODO this method can be made package protected once BehaviorBase is moved to this pkg
    public void key(IBehavior behavior, KeyBinding2 k, FunctionTag tag) {
        if (k == null) {
            return;
        }
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, behavior);
    }

    /**
     * Maps a key binding to the specified function tag, as a part of the behavior.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param code key code to construct a {@link KeyBinding2}
     * @param tag function tag
     */
    public void key(IBehavior behavior, KeyCode code, FunctionTag tag) {
        key(behavior, KeyBinding2.of(code), tag);
    }

    private void addFunction(FunctionTag tag, Runnable function, IBehavior behavior) {
        Entry en = map.get(tag);
        if (en == null) {
            en = new Entry();
            map.put(tag, en);
        }

        if (behavior == null) {
            // user mapping
            en.userValue = function;
        } else {
            // behavior mapping
            en.behavior = behavior;
            en.behaviorValue = function;
        }
    }

    private void addBinding(KeyBinding2 k, FunctionTag tag, IBehavior b) {
        Entry en = map.get(k);
        if (en == null) {
            en = new Entry();
            map.put(k, en);
        }
        
        if (b == null) {
            // user mapping
            en.userValue = tag;
        } else {
            // behavior mapping
            en.behavior = b;
            en.behaviorValue = tag;
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
     * Returns a {@code Runnable} mapped to the specified {@link KeyBinding2},
     * or null if no such mapping exists.
     *
     * @param k
     */
    public Runnable getFunction(KeyBinding2 k) {
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
     * Removes all the mappings set by the behavior.
     * Skin developers do not need to call this method directly, as it is being called in BehaviorBase.dispose().
     * TODO possibly make this method private.
     *
     * @param behavior
     */
    public void unregister(IBehavior behavior) {
        Objects.nonNull(behavior);
        Iterator<Entry> it = map.values().iterator();
        while (it.hasNext()) {
            Entry en = it.next();
            if (en.behavior == behavior) {
                en.behavior = null;
                en.behaviorValue = null;
            }
        }
    }

    /**
     * Unbinds the specified key binding.
     *
     * @param k
     */
    public void unbind(KeyBinding2 k) {
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
            if (me.getKey() instanceof KeyBinding2) {
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
    public void restoreDefaultBinding(KeyBinding2 k) {
        Entry en = map.get(k);
        if (en != null) {
            en.userValue = null;
            if (en.behaviorValue == null) {
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
            if (en.behaviorValue == null) {
                map.remove(tag);
            }
        }
    }

    private static void validateTag(FunctionTag tag) {
        if (tag instanceof KeyBinding2) {
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
    public Set<KeyBinding2> getKeyBindings() {
        return map.keySet().stream().
            filter((k) -> (k instanceof KeyBinding2)).
            map((x) -> (KeyBinding2)x).
            collect(Collectors.toSet());
    }

    /**
     * Maps a new KeyBinding as an alias to the existing one with the same owner and function tag.
     * This method does nothing if there is no mapping for k1.
     *
     * @param k1 existing key binding
     * @param k2 new key binding
     */
    public void addAlias(KeyBinding2 k1, KeyBinding2 k2) {
        Entry en1 = map.get(k1);
        if (en1 != null) {
            Entry en2 = new Entry();
            en2.behavior = en1.behavior;
            en2.behaviorValue = en1.behaviorValue;
            en2.userValue = en1.userValue;
            map.put(k2, en2);
        }
    }
}
