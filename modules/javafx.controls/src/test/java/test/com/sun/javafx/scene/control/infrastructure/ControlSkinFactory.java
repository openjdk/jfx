/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ButtonBehavior;
import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior;
import com.sun.javafx.scene.control.behavior.ToggleButtonBehavior;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.AccordionSkin;
import javafx.scene.control.skin.ButtonBarSkin;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.control.skin.CheckBoxSkin;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.ControlSkinShim;
import javafx.scene.control.skin.DateCellSkin;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.control.skin.HyperlinkSkin;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.control.skin.ListCellSkin;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.MenuBarSkin;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.control.skin.ProgressBarSkin;
import javafx.scene.control.skin.ProgressIndicatorSkin;
import javafx.scene.control.skin.RadioButtonSkin;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.control.skin.SeparatorSkin;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.control.skin.SpinnerSkin;
import javafx.scene.control.skin.SplitMenuButtonSkin;
import javafx.scene.control.skin.SplitPaneSkin;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.skin.TableCellSkin;
import javafx.scene.control.skin.TableRowSkin;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.control.skin.ToggleButtonSkin;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.control.skin.TreeCellSkin;
import javafx.scene.control.skin.TreeTableCellSkin;
import javafx.scene.control.skin.TreeTableRowSkin;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.control.skin.TreeViewSkin;

/**
 * Utility class to create Controls, alternative Skins and access/create behaviors.
 * Note: the alternative skin class must be "different enough" from the default
 * to really trigger a replace (see skinProperty for details).
 * <p>
 *
 * Naming conventions for alternative skins: ControlName + Skin + 1.
 *
 */
public class ControlSkinFactory {

// ----------------- control support

    /**
     * Returns a list of all control classes in package controls.
     *
     * @return a list control classes in package controls
     */
    public static List<Class<Control>> getControlClasses() {
        List<Object[]> data = Arrays.asList(controlClasses);
        List<Class<Control>> controls = data.stream()
            .map(array -> array[0])
            .map(element -> (Class<Control>) element)
            .collect(toList());
        return controls;
    }

    /**
     * Returns a list of all controls in package controls.
     *
     * @return a list of controls in package controls
     */
    public static List<Control> getControls() {
        List<Control> controls = getControlClasses().stream()
            .map(ControlSkinFactory::createControl)
            .collect(toList());
        return controls;
    }

    /**
     * Creates and returns an instance of the given control class.
     * @param <T> the type of the control
     * @param controlClass the class of the control
     * @return an instance of the class
     */
    public static <T extends Control> T createControl(Class<T> controlClass) {
        try {
            return controlClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//----------- behavior support

    /**
     * Returns a List of controlClasses that have skins with behaviour.
     *
     * @return list of controlClasses that have skins with behavior
     */
    public static List<Class<Control>> getControlClassesWithBehavior() {
        List<Class<Control>> controlClasses = getControlClasses();
        controlClasses.removeAll(withoutBehaviors);
        return controlClasses;
    }

    /**
     * Returns the skin's behavior.
     *
     * @param skin the skin to get the behavior from
     * @return the skin's behavior
     */
    public static BehaviorBase<?> getBehavior(Skin<?> skin) {
        return ControlSkinShim.getBehavior(skin);
    }

    /**
     * Creates and returns the default behavior for the given control.
     *
     * @param <T> the type of the control
     * @param control the control to create the behavior for
     * @return the default behavior for the control
     * @throws RuntimeException with the exception thrown when instantiating the behavior
     *
     */
    public static <T extends Control> BehaviorBase<T> createBehavior(T control) {
        Class<?> controlClass = control.getClass();
        Function<Control, BehaviorBase> creator = specialBehaviorMap.get(controlClass);
        if (creator != null) {
            return creator.apply(control);
        }

        String behaviorClassName = "com.sun.javafx.scene.control.behavior." + controlClass.getSimpleName() + "Behavior";
        try {
            Class<?>  behaviorClass = Class.forName(behaviorClassName);
             return  (BehaviorBase<T>) behaviorClass.getDeclaredConstructor(controlClass).newInstance(control);
        } catch (Exception e) {
            throw new RuntimeException("failed to instantiate a default behavior", e);
        }
    }


    // map for behaviors that don't have the standard name or are shared for several control classes
    static Map<Class<? extends Control>, Function<Control, BehaviorBase>> specialBehaviorMap = new HashMap<>();

    static {
        specialBehaviorMap.put(Button.class, (Function<Control, BehaviorBase>) c -> new ButtonBehavior((ButtonBase) c));
        specialBehaviorMap.put(CheckBox.class, (Function<Control, BehaviorBase>) c -> new ButtonBehavior((ButtonBase) c));
        specialBehaviorMap.put(ComboBox.class, (Function<Control, BehaviorBase>) c -> new ComboBoxListViewBehavior((ComboBox) c));
        specialBehaviorMap.put(Hyperlink.class, (Function<Control, BehaviorBase>) c -> new ButtonBehavior((ButtonBase) c));
        specialBehaviorMap.put(RadioButton.class, (Function<Control, BehaviorBase>) c -> new ToggleButtonBehavior((ToggleButton) c));
        specialBehaviorMap.put(ToggleButton.class, (Function<Control, BehaviorBase>) c -> new ToggleButtonBehavior((ToggleButton) c));
    }

    // list of control classes that have no behavior
    static List<Class<? extends Control>> withoutBehaviors = List.of(
            ButtonBar.class,
            Label.class,
            MenuBar.class,
            ProgressBar.class,
            ProgressIndicator.class,
            Separator.class,
            SplitPane.class
            );

///---------------- misc

    /**
     * Tries to let the weakRef be gc'ed.
     * @param weakRef the weakRef to be gc'ed
     */
    public static void attemptGC(WeakReference<?> weakRef) {
        for (int i = 0; i < 10; i++) {
            System.gc();
            System.runFinalization();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("InterruptedException occurred during Thread.sleep()");
            }
        }
    }

    /**
     * Nasty hack to keep JUnit pre-4.12 happy.
     * Before 4.12, Parameterized can only handle
     * a collection of arrays.
     *
     * @param data the list of data
     * @return the list of the data converted to one-dimensional arrays
     */
    public static List<Object[]> asArrays(List<?> data) {
        List<Object[]> result =  (List) data.stream()
                .map(d -> new Object[] {d, })
                .collect(toList());
        return result;
    }

  //------------- skin support
    /**
     * Creates and sets an alternative skin for the given control.
     *
     * @param <T> the type of the control
     * @param control the control to set the alternative skin to
     * @return the old skin of the control.
     */
    public static <T extends Control> Skin<?> replaceSkin(T control) {
        Skin<?> old = control.getSkin();
        control.setSkin(createAlternativeSkin(control));
        return old;
    }

    /**
     * Creates and returns an alternative skin for the given control.
     * This implementation uses the alternativeSkinsMap to lookup the
     * class for the alternative skin and instantiates it.
     *
     * @param <T> the type of the control
     * @param control the control to create an alternative skin for
     * @return the alternative skin for the control
     * @throws RuntimeException with the exception thrown thrown when instantiating the skin
     */
    public static <T extends Control> Skin<?> createAlternativeSkin(T control) {
        Class<?> controlClass = control.getClass();
        try {
            Class<?> skinClass =
                alternativeSkinClassMap.get(controlClass);
             return  (Skin<?>) skinClass.getDeclaredConstructor(controlClass).newInstance(control);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // map for alternative skins
    static Map<Class<?>, Class<?>> alternativeSkinClassMap = new HashMap<>();

    // filling the map .. could do without and create the alternative
    // skin classes by naming convention
    static {
        alternativeSkinClassMap.put(Accordion.class, AccordionSkin1.class);
        alternativeSkinClassMap.put(Button.class, ButtonSkin1.class);
        alternativeSkinClassMap.put(ButtonBar.class, ButtonBarSkin1.class);
        alternativeSkinClassMap.put(CheckBox.class, CheckBoxSkin1.class);
        alternativeSkinClassMap.put(ChoiceBox.class, ChoiceBoxSkin1.class);
        alternativeSkinClassMap.put(ColorPicker.class, ColorPickerSkin1.class);
        alternativeSkinClassMap.put(ComboBox.class, ComboBoxSkin1.class);
        alternativeSkinClassMap.put(DateCell.class, DateCellSkin1.class);
        alternativeSkinClassMap.put(DatePicker.class, DatePickerSkin1.class);
        alternativeSkinClassMap.put(Hyperlink.class, HyperlinkSkin1.class);
        alternativeSkinClassMap.put(Label.class, LabelSkin1.class);
        alternativeSkinClassMap.put(ListCell.class, ListCellSkin1.class);
        alternativeSkinClassMap.put(ListView.class, ListViewSkin1.class);
        alternativeSkinClassMap.put(MenuBar.class, MenuBarSkin1.class);
        alternativeSkinClassMap.put(MenuButton.class, MenuButtonSkin1.class);
        alternativeSkinClassMap.put(Pagination.class, PaginationSkin1.class);
        alternativeSkinClassMap.put(PasswordField.class, PasswordFieldSkin1.class);
        alternativeSkinClassMap.put(ProgressBar.class, ProgressBarSkin1.class);
        alternativeSkinClassMap.put(ProgressIndicator.class, ProgressIndicatorSkin1.class);
        alternativeSkinClassMap.put(RadioButton.class, RadioButtonSkin1.class);
        alternativeSkinClassMap.put(ScrollBar.class, ScrollBarSkin1.class);
        alternativeSkinClassMap.put(ScrollPane.class, ScrollPaneSkin1.class);
        alternativeSkinClassMap.put(Separator.class, SeparatorSkin1.class);
        alternativeSkinClassMap.put(Slider.class, SliderSkin1.class);
        alternativeSkinClassMap.put(Spinner.class, SpinnerSkin1.class);
        alternativeSkinClassMap.put(SplitMenuButton.class, SplitMenuButtonSkin1.class);
        alternativeSkinClassMap.put(SplitPane.class, SplitPaneSkin1.class);
        alternativeSkinClassMap.put(TableCell.class, TableCellSkin1.class);
        alternativeSkinClassMap.put(TableRow.class, TableRowSkin1.class);
        alternativeSkinClassMap.put(TableView.class, TableViewSkin1.class);
        alternativeSkinClassMap.put(TabPane.class, TabPaneSkin1.class);
        alternativeSkinClassMap.put(TextArea.class, TextAreaSkin1.class);
        alternativeSkinClassMap.put(TextField.class, TextFieldSkin1.class);
        alternativeSkinClassMap.put(TitledPane.class, TitledPaneSkin1.class);
        alternativeSkinClassMap.put(ToggleButton.class, ToggleButtonSkin1.class);
        alternativeSkinClassMap.put(ToolBar.class, ToolBarSkin1.class);
        alternativeSkinClassMap.put(TreeCell.class, TreeCellSkin1.class);
        alternativeSkinClassMap.put(TreeTableCell.class, TreeTableCellSkin1.class);
        alternativeSkinClassMap.put(TreeTableRow.class, TreeTableRowSkin1.class);
        alternativeSkinClassMap.put(TreeTableView.class, TreeTableViewSkin1.class);
        alternativeSkinClassMap.put(TreeView.class, TreeViewSkin1.class);
    }

//----------------- alternative skins for all controls

    public static class AccordionSkin1 extends AccordionSkin {

        public AccordionSkin1(Accordion control) {
            super(control);
        }

    }
    public static class ButtonSkin1 extends ButtonSkin {

        public ButtonSkin1(Button control) {
            super(control);
        }

    }

    public static class ButtonBarSkin1 extends ButtonBarSkin {

        public ButtonBarSkin1(ButtonBar control) {
            super(control);
        }

    }

    public static class CheckBoxSkin1 extends CheckBoxSkin {

        public CheckBoxSkin1(CheckBox control) {
            super(control);
        }

    }
    public static class ChoiceBoxSkin1 extends ChoiceBoxSkin {

        public ChoiceBoxSkin1(ChoiceBox control) {
            super(control);
        }

    }

    public static class ColorPickerSkin1 extends ColorPickerSkin {

        public ColorPickerSkin1(ColorPicker control) {
            super(control);
        }

    }

    public static class ComboBoxSkin1 extends ComboBoxListViewSkin {

        public ComboBoxSkin1(ComboBox control) {
            super(control);
        }

    }

    public static class DateCellSkin1 extends DateCellSkin {

        public DateCellSkin1(DateCell control) {
            super(control);
        }

    }

    public static class DatePickerSkin1 extends DatePickerSkin {

        public DatePickerSkin1(DatePicker control) {
            super(control);
        }

    }

    public static class HyperlinkSkin1 extends HyperlinkSkin {

        public HyperlinkSkin1(Hyperlink control) {
            super(control);
        }

    }

    public static class LabelSkin1 extends LabelSkin {

        public LabelSkin1(Label control) {
            super(control);
        }

    }

    public static class ListCellSkin1 extends ListCellSkin {

        public ListCellSkin1(ListCell control) {
            super(control);
        }

    }

    public static class ListViewSkin1 extends ListViewSkin {

        public ListViewSkin1(ListView control) {
            super(control);
        }

    }

    public static class MenuBarSkin1 extends MenuBarSkin {

        public MenuBarSkin1(MenuBar control) {
            super(control);
        }

    }

    public static class MenuButtonSkin1 extends MenuButtonSkin {

        public MenuButtonSkin1(MenuButton control) {
            super(control);
        }

    }

    public static class PaginationSkin1 extends PaginationSkin {

        public PaginationSkin1(Pagination control) {
            super(control);
        }

    }

    public static class PasswordFieldSkin1 extends TextFieldSkin {

        public PasswordFieldSkin1(PasswordField control) {
            super(control);
        }

    }

    public static class ProgressBarSkin1 extends ProgressBarSkin {

        public ProgressBarSkin1(ProgressBar control) {
            super(control);
        }

    }

    public static class ProgressIndicatorSkin1 extends ProgressIndicatorSkin {

        public ProgressIndicatorSkin1(ProgressIndicator control) {
            super(control);
        }

    }

    public static class RadioButtonSkin1 extends RadioButtonSkin {

        public RadioButtonSkin1(RadioButton control) {
            super(control);
        }

    }

    public static class ScrollBarSkin1 extends ScrollBarSkin {

        public ScrollBarSkin1(ScrollBar control) {
            super(control);
        }

    }

    public static class ScrollPaneSkin1 extends ScrollPaneSkin {

        public ScrollPaneSkin1(ScrollPane control) {
            super(control);
        }

    }

    public static class SeparatorSkin1 extends SeparatorSkin {

        public SeparatorSkin1(Separator control) {
            super(control);
        }

    }

    public static class SliderSkin1 extends SliderSkin {

        public SliderSkin1(Slider control) {
            super(control);
        }

    }

    public static class SpinnerSkin1 extends SpinnerSkin {

        public SpinnerSkin1(Spinner control) {
            super(control);
        }

    }

    public static class SplitMenuButtonSkin1 extends SplitMenuButtonSkin {

        public SplitMenuButtonSkin1(SplitMenuButton control) {
            super(control);
        }

    }

    public static class SplitPaneSkin1 extends SplitPaneSkin {

        public SplitPaneSkin1(SplitPane control) {
            super(control);
        }

    }

    public static class TableCellSkin1 extends TableCellSkin {

        public TableCellSkin1(TableCell control) {
            super(control);
        }

    }

    public static class TableRowSkin1 extends TableRowSkin {

        public TableRowSkin1(TableRow control) {
            super(control);
        }

    }

    public static class TableViewSkin1 extends TableViewSkin {

        public TableViewSkin1(TableView control) {
            super(control);
        }

    }

    public static class TabPaneSkin1 extends TabPaneSkin {

        public TabPaneSkin1(TabPane control) {
            super(control);
        }

    }

    public static class TextAreaSkin1 extends TextAreaSkin {

        public TextAreaSkin1(TextArea control) {
            super(control);
        }

    }

    public static class TextFieldSkin1 extends TextFieldSkin {

        public TextFieldSkin1(TextField control) {
            super(control);
        }

    }

    public static class TitledPaneSkin1 extends TitledPaneSkin {

        public TitledPaneSkin1(TitledPane control) {
            super(control);
        }

    }

    public static class ToggleButtonSkin1 extends ToggleButtonSkin {

        public ToggleButtonSkin1(ToggleButton control) {
            super(control);
        }

    }

    public static class ToolBarSkin1 extends ToolBarSkin {

        public ToolBarSkin1(ToolBar control) {
            super(control);
        }

    }

    public static class TreeCellSkin1 extends TreeCellSkin {

        public TreeCellSkin1(TreeCell control) {
            super(control);
        }

    }

    public static class TreeTableCellSkin1 extends TreeTableCellSkin {

        public TreeTableCellSkin1(TreeTableCell control) {
            super(control);
        }

    }

    public static class TreeTableRowSkin1 extends TreeTableRowSkin {

        public TreeTableRowSkin1(TreeTableRow control) {
            super(control);
        }

    }

    public static class TreeTableViewSkin1<T> extends TreeTableViewSkin<T> {

        public TreeTableViewSkin1(TreeTableView<T> control) {
            super(control);
        }

    }

    public static class TreeViewSkin1<T> extends TreeViewSkin<T> {

        public TreeViewSkin1(TreeView<T> control) {
            super(control);
        }

    }

    // all control classes in package controls
    // can be c&p'ed into parameterized test
    static Object[][] controlClasses = new Object[][] {
        {Accordion.class, },
        {Button.class, },
        {ButtonBar.class, }, // no behavior
        {CheckBox.class, }, // ButtonBehavior
        {ChoiceBox.class, },
        {ColorPicker.class, },
        {ComboBox.class, }, // ComboBoxListViewBehavior
        {DateCell.class, },
        {DatePicker.class, },
        {Hyperlink.class, }, // ButtonBehavior
        {Label.class, },    // no behavior
        {ListCell.class, },
        {ListView.class, },
        {MenuBar.class, },  // no behavior
        {MenuButton.class, },
        {Pagination.class, },
        {PasswordField.class, },
        {ProgressBar.class, },  // no behavior
        {ProgressIndicator.class, }, // no behavior
        {RadioButton.class, }, // ToggleButtonBehavior
        {ScrollBar.class, },
        {ScrollPane.class, },
        {Separator.class, }, // no behavior
        {Slider.class, },
        {Spinner.class, },
        {SplitMenuButton.class, },
        {SplitPane.class, }, // no behavior
        {TableCell.class, },
        {TableRow.class, },
        {TableView.class, },
        {TabPane.class, },
        {TextArea.class, },
        {TextField.class, },
        {TitledPane.class, },
        {ToggleButton.class, },
        {ToolBar.class, },
        {TreeCell.class, },
        {TreeTableCell.class, },
        {TreeTableRow.class, },
        {TreeTableView.class, },
        {TreeView.class, },
        };

    private ControlSkinFactory() {}
}
