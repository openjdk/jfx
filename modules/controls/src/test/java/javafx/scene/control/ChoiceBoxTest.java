/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import com.sun.javafx.scene.control.skin.ChoiceBoxSkin;
import com.sun.javafx.scene.control.skin.ChoiceBoxSkinNodesRetriever;
import javafx.application.Platform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ChoiceBoxTest {
    private final ChoiceBox<String> box = new ChoiceBox<String>();
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    
    @Before public void setup() {
        //This step is not needed (Just to make sure StubToolkit is loaded into VM)
        tk = (StubToolkit)Toolkit.getToolkit();
    }
    
    protected void startApp(Parent root) {
        scene = new Scene(root,800,600);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(box, "choice-box");
    }
    
    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(box.getSelectionModel());
    }
    
    @Test public void noArgConstructorSetsNonNullItems() {
        assertNotNull(box.getItems());
    }
    
    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
    }
    @Test public void noArgConstructor_converterIsNotNull() {
        assertNull(box.getConverter());
    }
    
    @Test public void singleArgConstructorSetsTheStyleClass() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "choice-box");
    }
    
    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }
    
    @Test public void singleArgConstructorAllowsNullItems() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(null);
        assertNull(b2.getItems());
    }
    
    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final ChoiceBox<String> b2 = new ChoiceBox<String>(items);
        assertSame(items, b2.getItems());
    }
    
    @Test public void singleArgConstructor_selectedItemIsNull() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }
    
    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void singleArgConstructor_converterIsNotNull() {
        final ChoiceBox<String> b2 = new ChoiceBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getConverter());
    }
    
    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/
    
    @Test public void selectionModelCanBeNull() {
        box.setSelectionModel(null);
        assertNull(box.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        SingleSelectionModel<String> sm = new ChoiceBox.ChoiceBoxSelectionModel<String>(box);
        ObjectProperty<SingleSelectionModel<String>> other = new SimpleObjectProperty<SingleSelectionModel<String>>(sm);
        box.selectionModelProperty().bind(other);
        assertSame(sm, box.getSelectionModel());
    }

    @Test public void selectionModelCanBeChanged() {
        SingleSelectionModel<String> sm = new ChoiceBox.ChoiceBoxSelectionModel<String>(box);
        box.setSelectionModel(sm);
        assertSame(sm, box.getSelectionModel());
    }
    
    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        box.getSelectionModel().select(randomString);
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertSame(randomString, box.getSelectionModel().getSelectedItem());
    }
        
    @Test public void canSetSelectedItemToAnItemNotInTheChoiceBoxItems() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        box.getSelectionModel().select(randomString);
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertSame(randomString, box.getSelectionModel().getSelectedItem());
    }
        
    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select("Orange");
        assertEquals(1, box.getSelectionModel().getSelectedIndex());
        assertSame("Orange", box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void settingTheSelectedItemToANonexistantItemAndThenAddingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        box.getSelectionModel().select("Orange");
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertEquals(1, box.getSelectionModel().getSelectedIndex());
        assertSame("Orange", box.getSelectionModel().getSelectedItem());
    }

    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        box.getSelectionModel().select("Orange");
        box.getItems().setAll("Apple", "Orange", "Banana");
        assertEquals(1, box.getSelectionModel().getSelectedIndex());
        assertSame("Orange", box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select(0);
        box.getItems().clear();
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getSelectionModel().getSelectedItem());
    }

    @Test public void ensureSelectionClearsWhenSettingSelectionBeforePopulatingItemsAndAllItemsAreRemoved() {
        box.getSelectionModel().select("Banana");
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getItems().clear();
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getSelectionModel().getSelectedItem());
    }

    @Ignore
    @Test public void ensureSelectionClearsWhenSettingSelectionBeforePopulatingItemsAndSelectedItemIsRemoved() {
        box.getSelectionModel().select("Banana");
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getItems().remove("Banana");
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getSelectionModel().getSelectedItem());
    }

    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select(2);
        box.getItems().clear();
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select(2);
        box.getItems().clear();
        assertNull(box.getSelectionModel().getSelectedItem());
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        
        box.getItems().addAll("Kiwifruit", "Mandarin", "Pineapple");
        box.getSelectionModel().select(2);
        assertEquals("Pineapple", box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        box.setItems(FXCollections.observableArrayList("Item 1"));
        box.getSelectionModel().select(0);
        assertEquals("Item 1", box.getSelectionModel().getSelectedItem());
        
        box.setItems(FXCollections.observableArrayList("Item 2"));
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectIndex() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select(0);
        assertEquals("Apple", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectItem() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select("Apple");
        assertEquals("Apple", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectPrevious() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select(2);
        box.getSelectionModel().selectPrevious();
        assertEquals("Orange", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectNext() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select("Orange");
        box.getSelectionModel().selectNext();
        assertEquals("Banana", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectFirst() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().selectFirst();
        assertEquals("Apple", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectLast() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().selectLast();
        assertEquals("Banana", box.getValue());
    }
    
    @Test public void ensureSelectionModelClearsValueProperty() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select(0);
        assertEquals("Apple", box.getValue());
        
        box.getSelectionModel().clearSelection();
        assertNull(box.getValue());
    }
    
    @Test public void ensureSelectionModelClearsValuePropertyWhenNegativeOneSelected() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getValue());
        box.getSelectionModel().select(0);
        assertEquals("Apple", box.getValue());
        
        box.getSelectionModel().select(-1);
        assertEquals(null, box.getValue());
    }
    
    @Test public void ensureValueIsCorrectWhenItemsIsAddedToWithExistingSelection() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select(1);
        box.getItems().add(0, "pineapple");
        assertEquals(2, box.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", box.getSelectionModel().getSelectedItem());
        assertEquals("Orange", box.getValue());
    }
    
    @Test public void ensureValueIsCorrectWhenItemsAreRemovedWithExistingSelection() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select(1);
        
        box.getItems().remove("Apple");
        
        assertEquals(0, box.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", box.getSelectionModel().getSelectedItem());
        assertEquals("Orange", box.getValue());
    }
    
    @Test public void ensureValueIsUpdatedByCorrectSelectionModelWhenSelectionModelIsChanged() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        SelectionModel sm1 = box.getSelectionModel();
        sm1.select(1);
        assertEquals("Orange", box.getValue());
        SingleSelectionModel sm2 = new ChoiceBox.ChoiceBoxSelectionModel(box);
        box.setSelectionModel(sm2);
        
        sm1.select(2);  // value should not change as we are using old SM
        assertEquals("Orange", box.getValue());
        
        sm2.select(0);  // value should change, as we are using new SM
        assertEquals("Apple", box.getValue());
    }
    
    @Test public void ensureValueDoesNotChangeWhenBoundAndNoExceptions() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        
        StringProperty sp = new SimpleStringProperty("empty");
        box.valueProperty().bind(sp);
        
        box.getSelectionModel().select(1);
        assertEquals("empty", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesWhenValueChanges() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(box.getSelectionModel().getSelectedItem());
        box.setValue("Orange");
        assertEquals("Orange", box.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureValueEqualsSelectedItemWhenNotInItemsList() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().setSelectedItem("pineapple");
        assertEquals("pineapple", box.getSelectionModel().getSelectedItem());
        assertEquals("pineapple", box.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesWhenValueChangesToNull() {
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.setValue("pineapple");
        assertEquals("pineapple", box.getSelectionModel().getSelectedItem());
        assertEquals("pineapple", box.getValue());
        box.setValue(null);
        assertEquals(null, box.getSelectionModel().getSelectedItem());
        assertEquals(-1, box.getSelectionModel().getSelectedIndex());
        assertEquals(null, box.getValue());
    }
    
    /*********************************************************************
     * Tests for showing property                                        *
     ********************************************************************/
    
    @Test public void showingIsFalseByDefault() {
        assertFalse(box.isShowing());
    }
    
    @Test public void showingCanBeSet() {
        box.show();
        assertTrue(box.isShowing());
    }
    
    @Test public void showingCanBeCleared() {
        box.show();
        box.hide();
        assertFalse(box.isShowing());
    }
    
    @Test public void showDoesntWorkWhenDisabled() {
        box.setDisable(true);
        box.show();
        assertFalse(box.isShowing());
    }
    
    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyShowingViaCSS() {
//        box.impl_cssSet("-fx-showing", true);
        assertFalse(box.isShowing());
    }
    
    @Test public void settingShowingSetsPseudoClass() {
        box.show();
        assertPseudoClassExists(box, "showing");
    }
    
    @Test public void clearingArmedClearsPseudoClass() {
        box.show();
        box.hide();
        assertPseudoClassDoesNotExist(box, "showing");
    }
    
    @Test public void testAddingEmptyChoiceBoxToLiveScene() {
        StackPane pane = new StackPane();
        pane.getChildren().add(box);
        startApp(pane);
        assertEquals(0, box.getItems().size());
    }
    
     @Test public void testSelectingItemBeforeFirstShow() {
        StackPane pane = new StackPane();
        pane.getChildren().add(box);
        box.getItems().addAll("Apple", "Orange", "Banana");
        box.getSelectionModel().select("Orange");
        startApp(pane);
        assertEquals(1, box.getSelectionModel().getSelectedIndex());
    }
     
    @Test public void checkLabelAfterCallingSetItemsFromPlatformRunLater_RT30317() {
        final String[] items = {"Apple", "Orange", "Banana"};
        StackPane pane = new StackPane();
        pane.getChildren().add(box);
        Runnable runnable = new Runnable() {
            public void run() {
                box.setItems(FXCollections.observableArrayList(items));
                box.getSelectionModel().setSelectedItem("Apple");
            }
        };
        Platform.runLater(runnable); 
        startApp(pane);
        assertEquals(0, box.getSelectionModel().getSelectedIndex());
        ChoiceBoxSkin skin = (ChoiceBoxSkin)box.getSkin();
        assertEquals("Apple", ChoiceBoxSkinNodesRetriever.getChoiceBoxSelectedText(skin));
        
    }
}
