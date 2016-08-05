/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BehaviorBase<N extends Node> {

    private final N node;
    private final List<Mapping<?>> installedDefaultMappings;
    private final List<Runnable> childInputMapDisposalHandlers;


    public BehaviorBase(N node) {
        this.node = node;
        this.installedDefaultMappings = new ArrayList<>();
        this.childInputMapDisposalHandlers = new ArrayList<>();
    }

    public abstract InputMap<N> getInputMap();

    public final N getNode() {
        return node;
    }

    public void dispose() {
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

    protected void addDefaultMapping(List<Mapping<?>> newMapping) {
        addDefaultMapping(getInputMap(), newMapping.toArray(new Mapping[newMapping.size()]));
    }

    protected void addDefaultMapping(Mapping<?>... newMapping) {
        addDefaultMapping(getInputMap(), newMapping);
    }

    protected void addDefaultMapping(InputMap<N> inputMap, Mapping<?>... newMapping) {
        // make a copy of the existing mappings, so we only check against those
        List<Mapping<?>> existingMappings = new ArrayList<>(inputMap.getMappings());

        for (Mapping<?> mapping : newMapping) {
            // check if a mapping already exists, and if so, do not add this mapping
            // TODO this is insufficient as we need to check entire InputMap hierarchy
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
