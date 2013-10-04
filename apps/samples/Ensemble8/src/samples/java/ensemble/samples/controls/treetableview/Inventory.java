/*
 * Copyright (c) 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.controls.treetableview;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Inventory {

        private BooleanProperty ordered;
        private StringProperty name;
        private ObjectProperty ob1;
        private StringProperty d1;
        private StringProperty d2;

        public Inventory(boolean ordered, String name, String d1, String d2, Part bigPart) {
            this.ordered = new SimpleBooleanProperty(ordered);
            this.name = new SimpleStringProperty(name);
            this.ob1 = new SimpleObjectProperty<>(bigPart);
            this.d1 = new SimpleStringProperty(d1);
            this.d2 = new SimpleStringProperty(d2);
        }

        public BooleanProperty orderedProperty() { return ordered; }

        public StringProperty nameProperty() { return name; }

        public StringProperty p1Property() { return d1; }

        public StringProperty p2Property() { return d2; }
        
        public ObjectProperty<Part> ob1Property() { return ob1; }

        public void setName(String name) { this.name.set(name); }

        public void setP1(String d1) { this.d1.set(d1); }

        public void setP2(String d2) { this.d2.set(d2); }
        
        public void setBigPart(Part bigPart) {this.ob1.set(bigPart);}
}
