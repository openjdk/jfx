/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.input.FunctionTag;
import javafx.scene.control.input.IBehavior;
import javafx.scene.control.input.KeyBinding2;
import javafx.scene.control.input.KeyMap;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.Mapping;

/**
 * Class provides a foundation for behaviors.
 *
 * @param <N> the actual class for which this behavior is intended
 */
// TODO add func() and key() can be added to BehaviorBase
public abstract class BehaviorBase<N extends Control> implements IBehavior {

    private final N node;
    private final List<Mapping<?>> installedDefaultMappings;
    private final List<Runnable> childInputMapDisposalHandlers;


    public BehaviorBase(N node) {
        this.node = node;
        this.installedDefaultMappings = new ArrayList<>();
        this.childInputMapDisposalHandlers = new ArrayList<>();
    }

    public abstract InputMap<N> getInputMap();

    /** invoked just before running a KeyMap function */
    protected void onKeyFunctionStart() { }

    /** invoked right after running a KeyMap function */
    protected void onKeyFunctionEnd() { }

    // TODO rename getControl()
    public final N getNode() {
        return node;
    }

    public void dispose() {
        node.getKeyMap().unregister(this);

        // when we dispose a behavior, we do NOT want to dispose the InputMap,
        // as that can remove input mappings that were not installed by the
        // behavior. Instead, we want to only remove mappings that the behavior
        // itself installed. This can be done by removing all input mappings that
        // were installed via the 'addDefaultMapping' method.

        // remove default mappings only
        for (Mapping<?> mapping : installedDefaultMappings) {
            getInputMap().getMappings().remove(mapping);
        }

        // Remove all default child mappings
        for (Runnable r : childInputMapDisposalHandlers) {
            r.run();
        }

//        InputMap<N> inputMap = getInputMap();
//        if (inputMap != null) {
//            inputMap.dispose();
//        }
    }
    
    protected void addKeyMap(Control c) {
        addDefaultMapping(
            getInputMap(),
            createKeyMap(getInputMap(), c, KeyEvent.KEY_PRESSED),
            createKeyMap(getInputMap(), c, KeyEvent.KEY_RELEASED),
            createKeyMap(getInputMap(), c, KeyEvent.KEY_TYPED)
        );
    }

    /**
     * Maps a function to the function tag.
     * This method will not override any previous mapping added by {@link #func(FunctionTag,Runnable)}.
     *
     * @param behavior
     * @param tag
     * @param function
     */
    public void func(FunctionTag tag, Runnable function) {
        getNode().getKeyMap().func(this, tag, function);
    }

    /**
     * Maps a key binding to the specified function tag.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping.
     *
     * @param behavior
     * @param k key binding, can be null
     * @param tag function tag
     */
    public void key(KeyBinding2 k, FunctionTag tag) {
        getNode().getKeyMap().key(this, k, tag);
    }

    /**
     * Maps a key binding to the specified function tag.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param code key code to construct a {@link KeyBinding2}
     * @param tag function tag
     */
    public void key(KeyCode code, FunctionTag tag) {
        getNode().getKeyMap().key(this, code, tag);
    }

    protected void addDefaultMapping(Mapping<?>... newMapping) {
        addDefaultMapping(getInputMap(), newMapping);
    }

    protected void addDefaultMapping(InputMap<N> inputMap, Mapping<?>... newMapping) {
        // make a copy of the existing mappings, so we only check against those
        List<Mapping<?>> existingMappings = new ArrayList<>(inputMap.getMappings());

        for (Mapping<?> mapping : newMapping) {
            // check if a mapping already exists, and if so, do not add this mapping
            // TODO: JDK-8250807: this is insufficient as we need to check entire InputMap hierarchy
//            for (Mapping<?> existingMapping : existingMappings) {
//                if (existingMapping != null && existingMapping.equals(mapping)) {
//                    return;
//                }
//            }
            if (existingMappings.contains(mapping)) continue;

            inputMap.getMappings().add(mapping);
            installedDefaultMappings.add(mapping);
        }
    }

    private Mapping<KeyEvent> createKeyMap(InputMap<N> inputMap, Control control, EventType<KeyEvent> t) {
        EventHandler<KeyEvent> handler = (ev) -> {
            KeyBinding2 k = KeyBinding2.from((KeyEvent)ev);
            KeyMap km = control.getKeyMap();
            Runnable f = km.getFunction(k);
            if (f != null) {
                onKeyFunctionStart();
                try {
                    f.run();
                    ev.consume();
                } finally {
                    onKeyFunctionEnd();
                }
            }
        };

        Mapping<KeyEvent> m = new Mapping<KeyEvent>(t, handler) {
            @Override
            public int getSpecificity(Event ev) {
                KeyBinding2 k = KeyBinding2.from((KeyEvent)ev);
                KeyMap m = control.getKeyMap();
                Runnable f = m.getFunction(k);
                if (f == null) {
                    return 0;
                }
                // Max value returned by KeyBinding:154
                return 6;
            }
        };
        m.setAutoConsume(false);
        return m;
    }

    protected <T extends Node> void addDefaultChildMap(InputMap<T> parentInputMap, InputMap<T> newChildInputMap) {
        parentInputMap.getChildInputMaps().add(newChildInputMap);

        childInputMapDisposalHandlers.add(() -> parentInputMap.getChildInputMaps().remove(newChildInputMap));
    }

    protected InputMap<N> createInputMap() {
        // TODO re-enable when InputMap moves back to Node / Control
//        return node.getInputMap() != null ?
//                (InputMap<N>)node.getInputMap() :
//                new InputMap<>(node);
        return new InputMap<>(node);
    }

    protected void removeMapping(Object key) {
        // TODO: JDK-8250807: Traverse the child maps of getInputMap() and remove the mapping from them.
        InputMap<?> inputMap = getInputMap();
        inputMap.lookupMapping(key).ifPresent(mapping -> {
            inputMap.getMappings().remove(mapping);
            installedDefaultMappings.remove(mapping);
        });
    }

    void rtl(Node node, Runnable rtlMethod, Runnable nonRtlMethod) {
        switch(node.getEffectiveNodeOrientation()) {
            case RIGHT_TO_LEFT: rtlMethod.run(); break;
            default: nonRtlMethod.run(); break;
        }
    }

    <T> void rtl(Node node, T object, Consumer<T> rtlMethod, Consumer<T> nonRtlMethod) {
        switch(node.getEffectiveNodeOrientation()) {
            case RIGHT_TO_LEFT: rtlMethod.accept(object); break;
            default: nonRtlMethod.accept(object); break;
        }
    }

    boolean isRTL(Node n) {
        switch(n.getEffectiveNodeOrientation()) {
            case RIGHT_TO_LEFT: return true;
            default: return false;
        }
    }
}
