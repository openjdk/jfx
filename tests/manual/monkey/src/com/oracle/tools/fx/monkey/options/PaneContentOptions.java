/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.options;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.util.ObjectSelector;

/**
 * Pane content Options.
 */
public class PaneContentOptions {
    public static Node createOptions(ObservableList<Node> children, Supplier<Builder> b) {
        ObjectSelector<List<Node>> s = new ObjectSelector<>("children", (cs) -> {
            children.setAll(cs);
        });
        s.addChoiceSupplier("progressive max", () -> {
            return b.get().
                d().max(30).
                d().max(31).
                d().max(32).
                d().max(33).
                d().max(34).
                d().max(35).
                d().max(36).
                d().max(37).
                d().max(38).
                d().max(39).
                d().max(40).
                d().max(41).
                d().max(30).
                build();
        });
        s.addChoiceSupplier("progressive min", () -> {
            return b.get().
                d().min(30).
                d().min(31).
                d().min(32).
                d().min(33).
                d().min(34).
                d().min(35).
                d().min(36).
                d().min(37).
                d().min(38).
                d().min(39).
                d().min(40).
                d().min(41).
                d().min(30).
                build();
        });
        s.addChoiceSupplier("fractional prefs", () -> {
            return b.get().
                d().pref(25.3).
                d().pref(25.3).
                d().pref(25.4).
                d().pref(25.3).max(100).
                d().pref(25.3).max(101).
                d().pref(25.4).
                build();
        });
        s.addChoiceSupplier("fill + max", () -> {
            return b.get().
                d().fill().
                d().max(200).
                build();
        });
        s.addChoiceSupplier("pref only", () -> {
            return b.get().
                d().pref(100).
                d().pref(150).
                d().pref(200).
                d().pref(250).
                build();
        });
        s.addChoiceSupplier("all set: min, pref, max", () -> {
            return b.get().
                d().
                d().min(20).pref(30).max(50).
                d().pref(200).
                d().pref(300).max(400).
                d().
                build();
        });
        s.addChoiceSupplier("min width", () -> {
            return b.get().
                d().
                d().
                d().
                d().min(300).
                build();
        });
        s.addChoiceSupplier("max width progressive", () -> {
            return b.get().
                d().max(30).fill().
                d().max(31).fill().
                d().max(32).fill().
                d().max(33).fill().
                d().max(34).fill().
                d().max(35).fill().
                d().max(36).fill().
                d().max(37).fill().
                d().max(38).fill().
                d().max(39).fill().
                build();
        });
        s.addChoiceSupplier("min width (beginning)", () -> {
            return b.get().
                d().min(300).
                d().min(300).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("max width (beginning)", () -> {
            return b.get().
                d().max(300).
                d().max(300).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("fixed width (beginning)", () -> {
            return b.get().
                d().min(100).max(100).
                d().min(100).max(100).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("min width (middle)", () -> {
            return b.get().
                d().
                d().
                d().min(300).
                d().min(300).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("max width (middle)", () -> {
            return b.get().
                d().
                d().
                d().max(300).
                d().max(300).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("fixed width (middle)", () -> {
            return b.get().
                d().
                d().
                d().min(100).max(100).
                d().min(100).max(100).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("min width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().min(300).
                d().min(300).
                build();
        });
        s.addChoiceSupplier("max width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().max(300).
                d().max(300).
                build();
        });
        s.addChoiceSupplier("fixed width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().min(100).max(100).
                d().min(100).max(100).
                build();
        });
        s.addChoiceSupplier("all fixed", () -> {
            return b.get().
                d().min(70).max(70).
                d().min(70).max(70).
                d().min(70).max(70).
                d().min(70).max(70).
                build();
        });
        s.addChoiceSupplier("all max", () -> {
            return b.get().
                d().max(70).
                d().max(70).
                d().max(70).
                d().max(70).
                build();
        });
        s.addChoiceSupplier("16 items, pref=30", () -> {
            return b.get().
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                build();
        });
        s.addChoiceSupplier("various", () -> {
            return b.get().
                d().pref(100).
                d().pref(200).
                d().pref(300).
                d().min(100).max(100).
                d().pref(100).
                d().min(100).
                d().max(100).
                d().pref(300).
                d().
                build();
        });
        s.selectFirst();
        return s;
    }

    public static abstract class Builder {
        protected abstract void setMin(Region r, double v);

        protected abstract void setPref(Region r, double v);

        protected abstract void setMax(Region r, double v);

        protected abstract void setGrow(Node n, Priority p);
        
        private final Function<List<Node>, Region> creator;
        private final ArrayList<Node> children = new ArrayList<>();
    
        public Builder(Function<List<Node>, Region> creator) {
            this.creator = creator;
        }
    
        public Builder d() {
            creator.apply(children);
            return this;
        }
    
        public Builder max(double v) {
            setMax(last(), v);
            return this;
        }
    
        public Builder min(double v) {
            setMin(last(), v);
            return this;
        }

        public Builder pref(double v) {
            setPref(last(), v);
            return this;
        }
        
        public Builder fill() {
            setGrow(last(), Priority.ALWAYS);
            return this;
        }

        private Region last() {
            return (Region)children.get(children.size() - 1);
        }
    
        public List<Node> build() {
            return children;
        }
    }
}
