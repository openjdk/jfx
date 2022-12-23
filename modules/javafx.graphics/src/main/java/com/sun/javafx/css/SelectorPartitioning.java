/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import javafx.css.CompoundSelector;
import javafx.css.Selector;
import javafx.css.SimpleSelector;
import javafx.css.StyleClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Code to partition selectors into a tree-like structure for faster matching.
 */
public final class SelectorPartitioning {

    /** package accessible */
    public SelectorPartitioning() {}

    /*
     * Wrapper so that we can have Map<ParitionKey, Partition> even though
     * the innards of the key might be a String or long[]
     */
    private final static class PartitionKey<K> {

        private final K key;

        private PartitionKey(K key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartitionKey<K> other = (PartitionKey<K>) obj;
            if (this.key != other.key && (this.key == null || !this.key.equals(other.key))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + (this.key != null ? this.key.hashCode() : 0);
            return hash;
        }

    }

    /**
     * A Partition corresponds to a selector type, id or styleclass. For any
     * given id (for example) there will be one Partition held in the
     * corresponding map (idMap, for example). Each Partition has Slots which
     * define a path from one Partition to the next. For example, if we have
     * A.b#c, there will be a Partition for each of A, .b and #c. The partition
     * for #c will have a Slot pointing to A. Likewise, A will have a Slot
     * corresponding to .b. Each Slot is capable of pointing to more than one
     * Partition. If another selector A.#c.z were partitioned, then the Slot
     * for A in Partition #c would now have Slots for both .b and .z.
     * <p>
     * Rules are added to the last Slot or to the Partition. If there is a
     * selector #c { -fx-fill: red; }, then the selector will be added to the
     * Partition for #c. If the selector were for A.b#c, then selector would be added
     * to the slot for '.b' which is in the slot for A in partion #c.
     * <p>
     * When Node is matched, it picks up the Selectors from the Partition and Slot
     * as the graph is traversed.
     */
    private static final class Partition {

        private final PartitionKey key;
        private final Map<PartitionKey, Slot> slots;
        private List<Selector> selectors;

        private Partition(PartitionKey key) {
           this.key = key;
            slots = new HashMap<>();
        }

        private void addSelector(Selector pair) {
            if (selectors == null) {
                selectors = new ArrayList<>();
            }
            selectors.add(pair);
        }

        /**
         * This routine finds the slot corresponding to the PartitionKey,
         * creating a Partition and Slot if necessary.
         */
        private Slot partition(PartitionKey id, Map<PartitionKey, Partition> map) {

            Slot slot = slots.get(id);
            if (slot == null) {
                Partition partition = getPartition(id,map);
                slot = new Slot(partition);
                slots.put(id, slot);
            }
            return slot;
        }

    }

    /**
     * A Slot is pointer to the next piece of the selector.
     */
    private static final class Slot {


        // The Partition to which this Slot belongs
        private final Partition partition;

        // The other Slots to which this Slot refers
        private final Map<PartitionKey, Slot> referents;

        // Selectors that match the path to this slot
        private List<Selector> selectors;

        private Slot(Partition partition) {
            this.partition = partition;
            this.referents = new HashMap<>();
        }

        private void addSelector(Selector pair) {
            if (selectors == null) {
                selectors = new ArrayList<>();
            }
            selectors.add(pair);
        }

        /**
         * This routine finds the slot corresponding to the PartitionKey,
         * creating a Partition and Slot if necessary.
         */
        private Slot partition(PartitionKey id, Map<PartitionKey, Partition> map) {
            Slot slot = referents.get(id);
            if (slot == null) {

                Partition p = getPartition(id, map);
                slot = new Slot(p);
                referents.put(id, slot);

            }
            return slot;
        }

    }

    /* A Map for selectors that have an id */
    private final Map<PartitionKey, Partition> idMap = new HashMap<>();

    /* A Map for selectors that have an element type */
    private final Map<PartitionKey, Partition> typeMap = new HashMap<>();

    /* A Map for selectors that have style classes */
    private final Map<PartitionKey, Partition> styleClassMap = new HashMap<>();

    /**
     * Keep track of the order in which a selector is added to the mapping so
     * the original order can be restored for the cascade.
     */
    private int ordinal;

    /** clear current partitioning */
    public void reset() {
        idMap.clear();
        typeMap.clear();
        styleClassMap.clear();
        ordinal = 0;
    }


    /**
     * Helper to lookup an id in the given map, creating and adding a Partition
     *
     */
    private static Partition getPartition(PartitionKey id, Map<PartitionKey,Partition> map) {

        Partition treeNode = map.get(id);
        if (treeNode == null) {
            treeNode = new Partition(id);
            map.put(id, treeNode);
        }
        return treeNode;
    }

    /* Mask that indicates the selector has an id part, e.g. #title */
    private static final int ID_BIT = 4;
    /* Mask that indicates the selector has a type part, e.g. Label */
    private static final int TYPE_BIT = 2;
    /* Mask that indicates the selector has a styleclass part, e.g. .label */
    private static final int STYLECLASS_BIT = 1;
    /* If there is no type part, then * is the default. */
    private static final PartitionKey WILDCARD = new PartitionKey<>("*");

    /* Place this selector into the partitioning map. Package accessible */
    public void partition(Selector selector) {

        SimpleSelector simpleSelector = null;
        if (selector instanceof CompoundSelector) {
            final List<SimpleSelector> selectors = ((CompoundSelector)selector).getSelectors();
            final int last = selectors.size()-1;
            simpleSelector = selectors.get(last);
        } else {
            simpleSelector = (SimpleSelector)selector;
        }

        final String selectorId = simpleSelector.getId();
        final boolean hasId =
            (selectorId != null && selectorId.isEmpty() == false);
        final PartitionKey idKey = hasId
                ? new PartitionKey(selectorId)
                : null;

        final String selectorType = simpleSelector.getName();
        final boolean hasType =
            (selectorType != null && selectorType.isEmpty() == false);
        final PartitionKey typeKey = hasType
                ? new PartitionKey(selectorType)
                : null;

        final Set<StyleClass> selectorStyleClass = simpleSelector.getStyleClassSet();
        final boolean hasStyleClass =
            (selectorStyleClass != null && selectorStyleClass.size() > 0);
        final PartitionKey styleClassKey = hasStyleClass
                ? new PartitionKey<>(selectorStyleClass)
                : null;

        final int c =
            (hasId ? ID_BIT : 0) | (hasType ? TYPE_BIT : 0) | (hasStyleClass ? STYLECLASS_BIT : 0);

        Partition partition = null;
        Slot slot = null;

        selector.setOrdinal(ordinal++);

        switch(c) {
            case ID_BIT | TYPE_BIT | STYLECLASS_BIT:
            case ID_BIT | TYPE_BIT:

                partition = getPartition(idKey, idMap);
                slot = partition.partition(typeKey, typeMap);
                if ((c & STYLECLASS_BIT) == STYLECLASS_BIT) {
                    slot = slot.partition(styleClassKey, styleClassMap);
                }
                slot.addSelector(selector);
                break;

            case TYPE_BIT | STYLECLASS_BIT:
            case TYPE_BIT:

                partition = getPartition(typeKey, typeMap);
                if ((c & STYLECLASS_BIT) == STYLECLASS_BIT) {
                    slot = partition.partition(styleClassKey, styleClassMap);
                    slot.addSelector(selector);
                } else {
                    partition.addSelector(selector);
                }
                break;

            // SimpleSelector always has a type which defaults to '*'
            case ID_BIT | STYLECLASS_BIT:
            case ID_BIT:
            case STYLECLASS_BIT:
            default:
                assert(false);
        }

    }

    /** Get the list of selectors that match this selector. Package accessible */
    public List<Selector> match(String selectorId, String selectorType, Set<StyleClass> selectorStyleClass) {

        final boolean hasId =
            (selectorId != null && selectorId.isEmpty() == false);
        final PartitionKey idKey = hasId
                ? new PartitionKey(selectorId)
                : null;

        final boolean hasType =
            (selectorType != null && selectorType.isEmpty() == false);
        final PartitionKey typeKey = hasType
                ? new PartitionKey(selectorType)
                : null;

        final boolean hasStyleClass =
            (selectorStyleClass != null && selectorStyleClass.size() > 0);
        final PartitionKey styleClassKey = hasStyleClass
                ? new PartitionKey<>(selectorStyleClass)
                : null;

        int c =
            (hasId ? ID_BIT : 0) | (hasType ? TYPE_BIT : 0) | (hasStyleClass ? STYLECLASS_BIT : 0);

        Partition partition = null;
        Slot slot = null;
        List<Selector> selectors = new ArrayList<>();

        while (c != 0) {

            switch(c) {
                case ID_BIT | TYPE_BIT | STYLECLASS_BIT:
                case ID_BIT | TYPE_BIT:
                {

                    partition = idMap.get(idKey);
                    if (partition != null) {
                        if (partition.selectors != null) {
                            selectors.addAll(partition.selectors);
                        }
                        // do-while handles A.b#c also matches A#c by first
                        // doing A.b#c then doing *.b#c
                        PartitionKey typePK = typeKey;
                        do {
                            slot = partition.slots.get(typePK);
                            if (slot != null) {

                                if (slot.selectors != null) {
                                    selectors.addAll(slot.selectors);
                                }
                                if ((c & STYLECLASS_BIT) == STYLECLASS_BIT) {
                                    Set<StyleClass> key = (Set<StyleClass>)styleClassKey.key;
                                    for (Slot s : slot.referents.values()) {
                                        if (s.selectors == null || s.selectors.isEmpty()) continue;
                                        Set<StyleClass> other = (Set<StyleClass>)s.partition.key.key;
                                        if (key.containsAll(other)) {
                                            selectors.addAll(s.selectors);
                                        }
                                    }
                                }

                            }
                            // if typePK is 'A', make it '*', if it is '*' make it null
                            typePK=WILDCARD.equals(typePK) == false ? WILDCARD : null;

                        } while(typePK != null);
                    }

                    c -= ID_BIT;
                    continue;
                }


                // SimpleSelector always has a type which defaults to '*'
                case ID_BIT | STYLECLASS_BIT:
                case ID_BIT:
                    c -= ID_BIT;
                    break;

                case TYPE_BIT | STYLECLASS_BIT:
                case TYPE_BIT:
                {

                    // do-while handles A.b also matches .b by first
                    // doing A.b then doing *.b
                    PartitionKey typePK = typeKey;
                    do {
                        partition = typeMap.get(typePK);
                        if (partition != null) {
                            if (partition.selectors != null) {
                                selectors.addAll(partition.selectors);
                            }
                            if ((c & STYLECLASS_BIT) == STYLECLASS_BIT) {
                                Set<StyleClass> key = (Set<StyleClass>)styleClassKey.key;
                                for (Slot s : partition.slots.values()) {
                                    if (s.selectors == null || s.selectors.isEmpty()) continue;
                                    Set<StyleClass> other = (Set<StyleClass>)s.partition.key.key;
                                    if (key.containsAll(other)) {
                                        selectors.addAll(s.selectors);
                                    }
                                }
                            }
                        }
                        // if typePK is 'A', make it '*', if it is '*' make it null
                        typePK=WILDCARD.equals(typePK) == false ? WILDCARD : null;

                    } while(typePK != null);

                    c -= TYPE_BIT;
                    continue;
                }

                // SimpleSelector always has a type which defaults to '*'
                case STYLECLASS_BIT:
                    c -= STYLECLASS_BIT;
                    break;

                default:
                    assert(false);
            }
        }

        Collections.sort(selectors, COMPARATOR);
        return selectors;
    }

    private static final Comparator<Selector> COMPARATOR =
            (o1, o2) -> o1.getOrdinal() - o2.getOrdinal();


}
