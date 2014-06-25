/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.accessibility.Accessible;
import javafx.scene.accessibility.Action;
import javafx.scene.accessibility.Attribute;
import javafx.scene.accessibility.Role;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import com.sun.glass.ui.PlatformAccessible;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import static javafx.scene.accessibility.Attribute.*;

/**
 * Native Interface - Implements NSAccessibility Protocol
 *
 */
final class MacAccessible extends PlatformAccessible {

    private native static void _initIDs();
    private native static boolean _initEnum(String enumName);
    static {
        _initIDs();
        if (!_initEnum("MacAttribute")) {
            System.err.println("Fail linking MacAttribute");
        }
        if (!_initEnum("MacAction")) {
            System.err.println("Fail linking MacAction");
        }
        if (!_initEnum("MacRole")) {
            System.err.println("Fail linking MacRole");
        }
        if (!_initEnum("MacSubrole")) {
            System.err.println("Fail linking MacSubrole");
        }
        if (!_initEnum("MacNotification")) {
            System.err.println("Fail linking MacNotification");
        }
        if (!_initEnum("MacOrientation")) {
            System.err.println("Fail linking MacOrientation");
        }
        if (!_initEnum("MacText")) {
            System.err.println("Fail linking MacText");
        }
    }

    static enum MacAttribute {
        // Dynamic mapping to FX attribute, dynamic return type
        NSAccessibilityValueAttribute(null, null),

        // 1-to-1 mapping between FX attribute and Mac attribute, static return type
        NSAccessibilityChildrenAttribute(CHILDREN, MacVariant::createNSArray),
        NSAccessibilityEnabledAttribute(DISABLED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityHelpAttribute(HELP, MacVariant::createNSString),

        // FOCUSED might not match the result of accessibilityFocusedUIElement() cause of FOCUS_ITEM 
        NSAccessibilityFocusedAttribute(FOCUSED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityExpandedAttribute(EXPANDED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityMaxValueAttribute(MAX_VALUE, MacVariant::createNSNumberForDouble),
        NSAccessibilityMinValueAttribute(MIN_VALUE, MacVariant::createNSNumberForDouble),
        NSAccessibilityParentAttribute(PARENT, MacVariant::createNSObject),
        NSAccessibilityPositionAttribute(BOUNDS, MacVariant::createNSValueForPoint),
        NSAccessibilityRoleAttribute(ROLE, MacVariant::createNSObject),
        NSAccessibilitySubroleAttribute(ROLE, MacVariant::createNSObject),
        NSAccessibilityRoleDescriptionAttribute(DESCRIPTION, MacVariant::createNSString),
        NSAccessibilitySizeAttribute(BOUNDS, MacVariant::createNSValueForSize),
        NSAccessibilityTabsAttribute(null, MacVariant::createNSArray),
        NSAccessibilityTitleAttribute(TITLE, MacVariant::createNSString),
        NSAccessibilityTopLevelUIElementAttribute(SCENE, MacVariant::createNSObject),
        NSAccessibilityWindowAttribute(SCENE, MacVariant::createNSObject),
        NSAccessibilityTitleUIElementAttribute(LABELED_BY, MacVariant::createNSObject),
        NSAccessibilityOrientationAttribute(ORIENTATION, MacVariant::createNSObject),
        NSAccessibilityOverflowButtonAttribute(OVERFLOW_BUTTON, MacVariant::createNSObject),

        // Custom attributes
        AXVisited(VISITED, MacVariant::createNSNumberForBoolean),
        AXMenuItemCmdChar(ACCELERATOR, MacVariant::createNSString),
        AXMenuItemCmdVirtualKey(ACCELERATOR, MacVariant::createNSNumberForInt),
        AXMenuItemCmdGlyph(ACCELERATOR, MacVariant::createNSNumberForInt),
        AXMenuItemCmdModifiers(ACCELERATOR, MacVariant::createNSNumberForInt),
        AXMenuItemMarkChar(SELECTED, MacVariant::createNSString),
        AXDateTimeComponents(null, MacVariant::createNSNumberForInt),

        // NSAccessibilityMenuRole
        NSAccessibilitySelectedChildrenAttribute(null, MacVariant::createNSArray),

        // NSAccessibilityStaticText
        NSAccessibilityNumberOfCharactersAttribute(TITLE, MacVariant::createNSNumberForInt),
        NSAccessibilitySelectedTextAttribute(SELECTION_START, MacVariant::createNSString),
        NSAccessibilitySelectedTextRangeAttribute(SELECTION_START, MacVariant::createNSValueForRange),
        NSAccessibilitySelectedTextRangesAttribute(null, null), //TODO Array of ranges
        NSAccessibilityInsertionPointLineNumberAttribute(CARET_OFFSET, MacVariant::createNSNumberForInt),
        NSAccessibilityVisibleCharacterRangeAttribute(TITLE, MacVariant::createNSValueForRange),

        // NSAccessibilityScrollAreaRole
        NSAccessibilityContentsAttribute(CONTENTS, MacVariant::createNSArray),
        NSAccessibilityHorizontalScrollBarAttribute(HORIZONTAL_SCROLLBAR, MacVariant::createNSObject),
        NSAccessibilityVerticalScrollBarAttribute(VERTICAL_SCROLLBAR, MacVariant::createNSObject),

        // NSAccessibilityRowRole
        NSAccessibilityIndexAttribute(INDEX, MacVariant::createNSNumberForInt),
        NSAccessibilitySelectedAttribute(SELECTED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityVisibleChildrenAttribute(CHILDREN, MacVariant::createNSArray),

        // NSAccessibilityOutlineRowRole
        NSAccessibilityDisclosedByRowAttribute(TREE_ITEM_PARENT, MacVariant::createNSObject),
        NSAccessibilityDisclosedRowsAttribute(null, null), // virtual only
        NSAccessibilityDisclosingAttribute(EXPANDED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityDisclosureLevelAttribute(DISCLOSURE_LEVEL, MacVariant::createNSNumberForInt),

        // NSAccessibilityTableRole
        NSAccessibilityColumnsAttribute(null, null), //virtual only
        NSAccessibilityRowsAttribute(null, null), //virtual only
        NSAccessibilityHeaderAttribute(HEADER, MacVariant::createNSObject),
        NSAccessibilitySelectedRowsAttribute(SELECTED_ROWS, MacVariant::createNSArray),
        NSAccessibilityRowCountAttribute(ROW_COUNT, MacVariant::createNSNumberForInt),
        NSAccessibilityColumnCountAttribute(COLUMN_COUNT, MacVariant::createNSNumberForInt),
        NSAccessibilitySelectedCellsAttribute(SELECTED_CELLS, MacVariant::createNSArray),
        NSAccessibilityRowIndexRangeAttribute(ROW_INDEX, MacVariant::createNSValueForRange),
        NSAccessibilityColumnIndexRangeAttribute(COLUMN_INDEX, MacVariant::createNSValueForRange),

        // Parameterized Attributes
        NSAccessibilityLineForIndexParameterizedAttribute(LINE_FOR_OFFSET, MacVariant::createNSNumberForInt, MacVariant.NSNumber_Int),
        NSAccessibilityStringForRangeParameterizedAttribute(TITLE, MacVariant::createNSString, MacVariant.NSValue_range),
        NSAccessibilityRangeForLineParameterizedAttribute(LINE_START, MacVariant::createNSValueForRange, MacVariant.NSNumber_Int),
        NSAccessibilityAttributedStringForRangeParameterizedAttribute(TITLE, MacVariant::createNSAttributedString, MacVariant.NSValue_range),
        NSAccessibilityCellForColumnAndRowParameterizedAttribute(CELL_AT_ROW_COLUMN, MacVariant::createNSObject, MacVariant.NSArray_int),
        NSAccessibilityRangeForPositionParameterizedAttribute(OFFSET_AT_POINT, MacVariant::createNSValueForRange, MacVariant.NSValue_point),
        NSAccessibilityBoundsForRangeParameterizedAttribute(BOUNDS_FOR_RANGE, MacVariant::createNSValueForRectangle, MacVariant.NSValue_range),

        ;long ptr; /* Initialized natively - treat as final */
        Attribute jfxAttr;
        Function<Object, MacVariant> map; /* Maps the object returned by JavaFX to the appropriate MacVariant */
        int inputType; /* Defined only for parameterized attributes to convert the native input parameter (id) to MacVariant */

        MacAttribute(Attribute jfxAttr, Function<Object, MacVariant> map, int inputType) {
            this.jfxAttr = jfxAttr;
            this.map = map;
            this.inputType = inputType;
        }

        MacAttribute(Attribute jfxAttr, Function<Object, MacVariant> map) {
            this.jfxAttr = jfxAttr;
            this.map = map;
        }

        static MacAttribute getAttribute(long ptr) {
            if (ptr == 0) return null;
            for (MacAttribute attr : values()) {
                if (ptr == attr.ptr || isEqualToString(attr.ptr, ptr)) {
                    return attr;
                }
            }
            return null;
        }
    }

    /* 
     * The Attribute and Action for roles are defined in
     * https://developer.apple.com/library/mac/documentation/UserExperience/Reference/Accessibility_RoleAttribute_Ref/Introduction.html
     */
    static enum MacRole {
        NSAccessibilityUnknownRole(Role.NODE, null, null),
        NSAccessibilityGroupRole(Role.PARENT, null, null),
        NSAccessibilityButtonRole(new Role[] {Role.BUTTON, Role.INCREMENT_BUTTON, Role.DECREMENT_BUTTON, Role.HEADER, Role.SPLIT_MENU_BUTTON},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        /* AXJFXTOOLTIP is a custom name used to ignore the tooltip window. See GlassWindow.m for details. */
        AXJFXTOOLTIP(Role.TOOLTIP, null, null),
        NSAccessibilityImageRole(Role.IMAGE, null, null),
        NSAccessibilityRadioButtonRole(new Role[] {Role.RADIO_BUTTON, Role.TAB_ITEM, Role.PAGE},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityCheckBoxRole(new Role[] {Role.CHECKBOX, Role.TOGGLE_BUTTON},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        /* ComboBox can be either a NSAccessibilityComboBoxRole or a NSAccessibilityPopUpButtonRole (Based on EDITABLE) */
        NSAccessibilityComboBoxRole(Role.COMBOBOX,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityExpandedAttribute
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction}
        ),
        NSAccessibilityPopUpButtonRole(Role.COMBOBOX,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction}
        ),
        NSAccessibilityTabGroupRole(new Role[] {Role.TAB_PANE, Role.PAGINATION},
            new MacAttribute[] {
//              MacAttributes.NSAccessibilityContentsAttribute,
                MacAttribute.NSAccessibilityTabsAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            null,
            null
        ),
        NSAccessibilityProgressIndicatorRole(Role.PROGRESS_INDICATOR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityOrientationAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.NSAccessibilityMaxValueAttribute,
                MacAttribute.NSAccessibilityMinValueAttribute,
            },
            null
        ),
        NSAccessibilityMenuBarRole(Role.MENU_BAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySelectedChildrenAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuRole(Role.CONTEXT_MENU,
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySelectedChildrenAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction,
                MacAction.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuItemRole(Role.MENU_ITEM,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
                MacAttribute.AXMenuItemCmdChar,
                MacAttribute.AXMenuItemCmdVirtualKey,
                MacAttribute.AXMenuItemCmdGlyph,
                MacAttribute.AXMenuItemCmdModifiers,
                MacAttribute.AXMenuItemMarkChar,
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction,
                MacAction.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuButtonRole(Role.MENU_BUTTON,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction,
            }
        ),
        NSAccessibilityStaticTextRole(new Role[] {Role.TEXT, Role.TREE_TABLE_CELL},
            null, null, null
        ),
        NSAccessibilityTextFieldRole(new Role[] {Role.TEXT_FIELD, Role.PASSWORD_FIELD},
            null, null, null
        ),
        NSAccessibilityTextAreaRole(Role.TEXT_AREA, null, null),
        NSAccessibilitySliderRole(Role.SLIDER,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityOrientationAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.NSAccessibilityMaxValueAttribute,
                MacAttribute.NSAccessibilityMinValueAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityDecrementAction,
                MacAction.NSAccessibilityIncrementAction,
            }
        ),
        NSAccessibilityScrollAreaRole(Role.SCROLL_PANE,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityContentsAttribute,
                MacAttribute.NSAccessibilityHorizontalScrollBarAttribute,
                MacAttribute.NSAccessibilityVerticalScrollBarAttribute,
            },
            null
        ),
        NSAccessibilityScrollBarRole(Role.SCROLL_BAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.NSAccessibilityMinValueAttribute,
                MacAttribute.NSAccessibilityMaxValueAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityOrientationAttribute,
            },
            null
        ),
        NSAccessibilityValueIndicatorRole(Role.THUMB,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityValueAttribute,
            },
            null
        ),
        NSAccessibilityRowRole(new Role[] {Role.LIST_ITEM, Role.TABLE_ROW, Role.TREE_ITEM, Role.TREE_TABLE_ITEM},
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute,
                MacAttribute.NSAccessibilityIndexAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
                MacAttribute.NSAccessibilityVisibleChildrenAttribute,
            },
            null, null
        ),
        NSAccessibilityTableRole(new Role[] {Role.LIST_VIEW, Role.TABLE_VIEW},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityColumnsAttribute,
                MacAttribute.NSAccessibilityHeaderAttribute,
                MacAttribute.NSAccessibilityRowsAttribute,
                MacAttribute.NSAccessibilitySelectedRowsAttribute,
                MacAttribute.NSAccessibilityRowCountAttribute,
                MacAttribute.NSAccessibilityColumnCountAttribute,
                MacAttribute.NSAccessibilitySelectedCellsAttribute,
            },
            null,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityCellForColumnAndRowParameterizedAttribute,
            }
        ),
        NSAccessibilityColumnRole(Role.TABLE_COLUMN,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityHeaderAttribute,
                MacAttribute.NSAccessibilityIndexAttribute,
                MacAttribute.NSAccessibilityRowsAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
            },
            null
        ),
        NSAccessibilityCellRole(new Role[] {Role.TABLE_CELL},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityColumnIndexRangeAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityRowIndexRangeAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
            },
            null,
            null
        ),
        NSAccessibilityLinkRole(Role.HYPERLINK,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.AXVisited
            },
            null
        ),
        NSAccessibilityOutlineRole(new Role[] {Role.TREE_VIEW, Role.TREE_TABLE_VIEW},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityColumnsAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityHeaderAttribute,
                MacAttribute.NSAccessibilityRowsAttribute,
                MacAttribute.NSAccessibilitySelectedRowsAttribute,
            },
            null,
            null
        ),
        NSAccessibilityDisclosureTriangleRole(new Role[] {Role.DISCLOSURE_NODE, Role.TITLED_PANE},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction
            },
            null
        ),
        NSAccessibilityToolbarRole(Role.TOOLBAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityOverflowButtonAttribute,
            },
            null
        ),
        AXDateTimeArea(Role.DATE_PICKER,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.AXDateTimeComponents,
            },
            null
        ),

        ;long ptr; /* Initialized natively - treat as final */
        Role[] jfxRoles;
        List<MacAttribute> macAttributes;
        List<MacAttribute> macParameterizedAttributes;
        List<MacAction> macActions;
        MacRole(Role jfxRole, MacAttribute[] macAttributes, MacAction[] macActions) {
            this(new Role[] {jfxRole}, macAttributes, macActions, null);
        }

        MacRole(Role[] jfxRoles, MacAttribute[] macAttributes, MacAction[] macActions, MacAttribute[] macParameterizedAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
            this.macActions = macActions != null ? Arrays.asList(macActions) : null;
            this.macParameterizedAttributes = macParameterizedAttributes != null ? Arrays.asList(macParameterizedAttributes) : null;
        }

        static MacRole getRole(Role targetRole) {
            if (targetRole == null) return null;
            for (MacRole macRole : values()) {
                for (Role jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    static enum MacSubrole {
        NSAccessibilityTableRowSubrole(Role.LIST_ITEM, Role.TABLE_ROW),
        NSAccessibilitySortButtonSubrole(Role.HEADER),
        NSAccessibilitySecureTextFieldSubrole(Role.PASSWORD_FIELD),
        NSAccessibilityOutlineRowSubrole(new Role[] { Role.TREE_ITEM, Role.TREE_TABLE_ITEM },
            new MacAttribute[] {
                MacAttribute.NSAccessibilityDisclosedByRowAttribute,
                MacAttribute.NSAccessibilityDisclosedRowsAttribute,
                MacAttribute.NSAccessibilityDisclosingAttribute,
                MacAttribute.NSAccessibilityDisclosureLevelAttribute
            }
        ),
        NSAccessibilityDecrementArrowSubrole(new Role[] { Role.DECREMENT_BUTTON },
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute
            }
        ),
        NSAccessibilityIncrementArrowSubrole(new Role[] { Role.INCREMENT_BUTTON },
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute
            }
        )

        ;long ptr; /* Initialized natively - treat as final */
        Role[] jfxRoles;
        List<MacAttribute> macAttributes;

        MacSubrole(Role... jfxRoles) {
            this(jfxRoles, null);
        }

        MacSubrole(Role[] jfxRoles, MacAttribute[] macAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
        }

        static MacSubrole getRole(Role targetRole) {
            if (targetRole == null) return null;
            for (MacSubrole macRole : values()) {
                for (Role jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    static enum MacAction {
        NSAccessibilityCancelAction,
        NSAccessibilityConfirmAction,
        NSAccessibilityDecrementAction(Action.DECREMENT),
        NSAccessibilityDeleteAction,
        NSAccessibilityIncrementAction(Action.INCREMENT),
        NSAccessibilityPickAction,
        NSAccessibilityPressAction(Action.FIRE),
        NSAccessibilityRaiseAction,
        NSAccessibilityShowMenuAction(Action.SHOW_MENU),

        ;long ptr; /* Initialized natively - treat as final */
        Action jfxAction;
        MacAction() {}
        MacAction(Action jfxAction) {
            this.jfxAction = jfxAction;
        }

        static MacAction getAction(long ptr) {
            for (MacAction macAction : MacAction.values()) {
                if (macAction.ptr == ptr || isEqualToString(macAction.ptr, ptr)) {
                    return macAction;
                }
            }
            return null;
        }
    }

    static enum MacNotification {
        NSAccessibilityCreatedNotification,
        NSAccessibilityFocusedUIElementChangedNotification,
        NSAccessibilityValueChangedNotification,
        NSAccessibilitySelectedChildrenChangedNotification,
        NSAccessibilitySelectedRowsChangedNotification,
        NSAccessibilityTitleChangedNotification,
        NSAccessibilityRowCountChangedNotification,
        NSAccessibilitySelectedCellsChangedNotification,
        NSAccessibilityUIElementDestroyedNotification,
        NSAccessibilitySelectedTextChangedNotification,
        NSAccessibilityRowExpandedNotification,
        NSAccessibilityRowCollapsedNotification,
        AXMenuOpened,
        AXMenuClosed,
        ;long ptr; /* Initialized natively - treat as final */
    }

    static enum MacOrientation {
        NSAccessibilityHorizontalOrientationValue,
        NSAccessibilityVerticalOrientationValue,
        NSAccessibilityUnknownOrientationValue,
        ;long ptr; /* Initialized natively - treat as final */
    }

    static enum MacText {
        NSAccessibilityBackgroundColorTextAttribute,
        NSAccessibilityForegroundColorTextAttribute,
        NSAccessibilityUnderlineTextAttribute,
        NSAccessibilityStrikethroughTextAttribute,
        NSAccessibilityMarkedMisspelledTextAttribute,
        NSAccessibilityFontTextAttribute,
        NSAccessibilityFontNameKey,
        NSAccessibilityFontFamilyKey,
        NSAccessibilityVisibleNameKey,
        NSAccessibilityFontSizeKey,
        ;long ptr; /* Initialized natively - treat as final */
    }

    /* 
     * Do not access the following lists directly from the Mac enums.
     * It can cause the static initialization to happen in an unexpected order.
     */
    static final List<MacAttribute> baseAttributes = Arrays.asList(
        MacAttribute.NSAccessibilityRoleAttribute,
        MacAttribute.NSAccessibilityRoleDescriptionAttribute,
        MacAttribute.NSAccessibilityHelpAttribute,
        MacAttribute.NSAccessibilityFocusedAttribute,
        MacAttribute.NSAccessibilityParentAttribute,
        MacAttribute.NSAccessibilityChildrenAttribute,
        MacAttribute.NSAccessibilityPositionAttribute,
        MacAttribute.NSAccessibilitySizeAttribute,
        MacAttribute.NSAccessibilityWindowAttribute,
        MacAttribute.NSAccessibilityTopLevelUIElementAttribute,
        MacAttribute.NSAccessibilityTitleUIElementAttribute
    );

    static final List<MacAttribute> textAttributes = Arrays.asList(
        MacAttribute.NSAccessibilityEnabledAttribute,
        MacAttribute.NSAccessibilityValueAttribute,
        MacAttribute.NSAccessibilityNumberOfCharactersAttribute,
        MacAttribute.NSAccessibilitySelectedTextAttribute,
        MacAttribute.NSAccessibilitySelectedTextRangeAttribute,
        MacAttribute.NSAccessibilityInsertionPointLineNumberAttribute,
        MacAttribute.NSAccessibilityVisibleCharacterRangeAttribute
    );

    static final List<MacAttribute> textParameterizedAttributes = Arrays.asList(
        MacAttribute.NSAccessibilityLineForIndexParameterizedAttribute,
        MacAttribute.NSAccessibilityRangeForLineParameterizedAttribute,
        MacAttribute.NSAccessibilityAttributedStringForRangeParameterizedAttribute,
        MacAttribute.NSAccessibilityStringForRangeParameterizedAttribute
    );

    /* The native peer associated with the instance */
    private long peer;

    /* Creates a GlassAccessible linked to the caller (GlobalRef) */
    private native long _createGlassAccessible();

    /* Releases the GlassAccessible and deletes the GlobalRef */
    private native void _destroyGlassAccessible(long accessible);

    private static native String getString(long nsString);
    private static native boolean isEqualToString(long nsString1, long nsString);
    private static native long NSAccessibilityUnignoredAncestor(long id);
    private static native long[] NSAccessibilityUnignoredChildren(long[] originalChildren);
    private static native void NSAccessibilityPostNotification(long element, long notification);
    private static native String NSAccessibilityActionDescription(long action);
    private static native String NSAccessibilityRoleDescription(long role, long subrole);
    private static native MacVariant idToMacVariant(long id, int type);
    private static native MacAccessible GlassAccessibleToMacAccessible(long glassAccessible);
    private static final int kAXMenuItemModifierNone         = 0;
    private static final int kAXMenuItemModifierShift        = (1 << 0);
    private static final int kAXMenuItemModifierOption       = (1 << 1);
    private static final int kAXMenuItemModifierControl      = (1 << 2);
    private static final int kAXMenuItemModifierNoCommand    = (1 << 3);

    private MacAccessible(Accessible accessible) {
        super(accessible);
        this.peer = _createGlassAccessible();
    }

    static MacAccessible createAccessible(Accessible accessible) {
        if (accessible == null) return null;
        MacAccessible macAccessible = new MacAccessible(accessible);
        if (macAccessible.peer == 0L) return null;
        return macAccessible;
    }

    @Override
    public void dispose() {
        if (peer != 0L) {
            if (getView() == null) {
                NSAccessibilityPostNotification(peer, MacNotification.NSAccessibilityUIElementDestroyedNotification.ptr);
            }
            _destroyGlassAccessible(peer);
            peer = 0L;
        }
        super.dispose();
    }

    @Override
    public void sendNotification(Attribute notification) {
        if (isDisposed()) return;

        MacNotification macNotification = null;
        switch (notification) {
            case SELECTED_TAB:
            case SELECTED_PAGE: {
                View view = getRootView((Scene)getAttribute(SCENE));
                if (view != null) {
                    long id = view.getNativeView();
                    NSAccessibilityPostNotification(id, MacNotification.NSAccessibilityFocusedUIElementChangedNotification.ptr);
                }
                return;
            }
            case SELECTED_ROWS:
                macNotification = MacNotification.NSAccessibilitySelectedRowsChangedNotification;
                break;
            case SELECTED_CELLS:
                macNotification = MacNotification.NSAccessibilitySelectedCellsChangedNotification;
                break;
            case FOCUS_NODE: {
                Node node = (Node)getAttribute(FOCUS_NODE);
                View view = getView();
                if (node == null && view == null) {
                    /* 
                     * The transientFocusContainer resigns focus.
                     * Delegate to the scene.
                     */
                    Scene scene = (Scene)getAttribute(SCENE);
                    if (scene != null) {
                        Accessible acc = scene.getAccessible();
                        if (acc != null) {
                            node = (Node)acc.getAttribute(FOCUS_NODE);
                        }
                    }
                }

                long id = 0L;
                if (node != null) {
                    Node item = (Node)node.getAccessible().getAttribute(FOCUS_ITEM);
                    id = item != null ? getAccessible(item) : getAccessible(node);
                } else {
                    /* 
                     * No focused element. Send the notification to the scene itself.
                     * Note, the view is NULL when the FOCUS_NODE notification is sent
                     * by the transientFocusContainer.
                     */
                    if (view == null) view = getRootView((Scene)getAttribute(SCENE));
                    if (view != null) id = view.getNativeView();
                }

                if (id != 0) {
                    NSAccessibilityPostNotification(id, MacNotification.NSAccessibilityFocusedUIElementChangedNotification.ptr);
                }
                return;
            }
            case FOCUSED:
                return;
            case SELECTION_START:
            case SELECTION_END:
                macNotification = MacNotification.NSAccessibilitySelectedTextChangedNotification;
                break;
            case EXPANDED:
                boolean expanded = Boolean.TRUE.equals(getAttribute(EXPANDED));
                if (expanded) {
                    macNotification = MacNotification.NSAccessibilityRowExpandedNotification;
                } else {
                    macNotification = MacNotification.NSAccessibilityRowCollapsedNotification;
                }

                Role role = (Role) getAttribute(ROLE);
                if (role == Role.TREE_ITEM || role == Role.TREE_TABLE_ITEM) {
                    Role container = role == Role.TREE_ITEM ? Role.TREE_VIEW : Role.TREE_TABLE_VIEW;
                    long control = getAccessible(getContainerNode(container));
                    if (control != 0) {
                        NSAccessibilityPostNotification(control, MacNotification.NSAccessibilityRowCountChangedNotification.ptr);
                    }
                }
                break;
            case VISIBLE: {
                if (getAttribute(ROLE) == Role.CONTEXT_MENU) {
                    Boolean visible = (Boolean)getAttribute(VISIBLE);
                    if (Boolean.TRUE.equals(visible)) {
                        macNotification = MacNotification.AXMenuOpened;
                    } else {
                        macNotification = MacNotification.AXMenuClosed;

                        /* 
                         * When a submenu closes the focus is returned to the main
                         * window, as opposite of the previous menu.
                         * The work around is to look for a previous menu
                         * and send a close and open event for it.
                         */
                        Node menuItemOwner = (Node)getAttribute(MENU_FOR);
                        long menu = getAccessible(getContainerNode(menuItemOwner, Role.CONTEXT_MENU));
                        if (menu != 0) {
                            NSAccessibilityPostNotification(menu, MacNotification.AXMenuClosed.ptr);
                            NSAccessibilityPostNotification(menu, MacNotification.AXMenuOpened.ptr);
                        }
                    }
                }
                break;
            }
            case PARENT:
                ignoreInnerText = null;
                break;
            default:
                macNotification = MacNotification.NSAccessibilityValueChangedNotification;
        }
        if (macNotification != null) {
            View view = getView();
            long id = view != null ? view.getNativeView() : peer;
            NSAccessibilityPostNotification(id, macNotification.ptr);
        }
    }

    @Override
    protected long getNativeAccessible() {
        return peer;
    }

    @SuppressWarnings("deprecation")
    private View getRootView(Scene scene) {
        if (scene == null) return null;
        Accessible acc = scene.getAccessible();
        if (acc == null) return null;
        MacAccessible macAcc = (MacAccessible)acc.impl_getDelegate();
        if (macAcc == null || macAcc.isDisposed()) return null;
        View view = macAcc.getView();
        if (view == null || view.isClosed()) return null;
        return view;
    }

    long[] getUnignoredChildren(ObservableList<Node> children) {
        if (children == null) return new long[0];
        long[] ids = children.stream()
                             .mapToLong(n -> getAccessible(n))
                             .filter(n -> n != 0)
                             .toArray();
        return NSAccessibilityUnignoredChildren(ids);
    }

    private Boolean inMenu;
    private boolean isInMenu() {
        /* This flag will be wrong if the Node is ever re-parented */
        if (inMenu == null) {
            inMenu = getContainerNode(Role.CONTEXT_MENU) != null || getContainerNode(Role.MENU_BAR) != null;
        }
        return inMenu;
    }

    private Boolean inSlider;
    private boolean isInSlider() {
        /* This flag will be wrong if the Node is ever re-parented */
        if (inSlider == null) {
            inSlider = getContainerNode(Role.SLIDER) != null;
        }
        return inSlider;
    }

    Boolean ignoreInnerText;
    boolean ignoreInnerText() {
        if (ignoreInnerText != null) return ignoreInnerText;
        /* 
         * JavaFX controls are implemented by the skin by adding new nodes.
         * In accessibility these nodes sometimes duplicate the data in the
         * control. For example, a Label is implemented using a Text, creating a
         * AXStaticText inside an AXStaticText. In order to  improve accessibility
         * navigation to following code ignores these inner text for the most 
         * common cases.
         */
        Role role = (Role)getAttribute(ROLE);
        ignoreInnerText = false;
        if (role == Role.TEXT) {
            Node parent = (Node)getAttribute(PARENT);
            if (parent == null) return ignoreInnerText;
            Role parentRole = (Role)parent.getAccessible().getAttribute(ROLE);
            if (parentRole == null) return ignoreInnerText;
            switch (parentRole) {
                case BUTTON:
                case TOGGLE_BUTTON:
                case CHECKBOX:
                case RADIO_BUTTON:
                case COMBOBOX:
                case TEXT:
                case HYPERLINK:
                case TAB_ITEM:
                    ignoreInnerText = true;
                default:
            }
        }
        return ignoreInnerText;
    }

    private int getMenuItemCmdGlyph(KeyCode code) {
        // Based on System/Library/Frameworks/Carbon.framework/Frameworks/HIToolbox.framework/Headers/Menus.h
        switch (code) {
            case ENTER:        return 0x04;
            case SHIFT:        return 0x05;
            case CONTROL:      return 0x06;
            case META:         return 0x07;
            case SPACE:        return 0x09;
            case COMMAND:      return 0x11;
            case ESCAPE:       return 0x1b;
            case CLEAR:        return 0x1c;
            case PAGE_UP:      return 0x62;
            case CAPS:         return 0x63;
            case LEFT:
            case KP_LEFT:      return 0x64;
            case RIGHT:
            case KP_RIGHT:     return 0x65;
            case HELP:         return 0x67;
            case UP:
            case KP_UP:        return 0x68;
            case DOWN:
            case KP_DOWN:      return 0x6a;
            case PAGE_DOWN:    return 0x6b;
            case CONTEXT_MENU: return 0x6d;
            case POWER:        return 0x6e;
            case F1:           return 0x6f;
            case F2:           return 0x70;
            case F3:           return 0x71;
            case F4:           return 0x72;
            case F5:           return 0x73;
            case F6:           return 0x74;
            case F7:           return 0x75;
            case F8:           return 0x76;
            case F9:           return 0x77;
            case F10:          return 0x78;
            case F11:          return 0x79;
            case F12:          return 0x7a;
            case F13:          return 0x87;
            case F14:          return 0x88;
            case F15:          return 0x89;
            default: return 0;
        }
    }

    private boolean isCmdCharBased(KeyCode code) {
        return code.isLetterKey() || (code.isDigitKey() && !code.isKeypadKey());
    }

    private MacRole getRole(Role role) {
        if (role == Role.COMBOBOX) {
            if (Boolean.TRUE.equals(getAttribute(EDITABLE))) {
                return MacRole.NSAccessibilityComboBoxRole;
            } else {
                return MacRole.NSAccessibilityPopUpButtonRole;
            }
        }
        return MacRole.getRole(role);
    }

    private Bounds flipBounds(Bounds bounds) {
        View view = getRootView((Scene)getAttribute(SCENE));
        if (view == null) return null;
        Screen screen = view.getWindow().getScreen();
        float height = screen.getHeight();
        return new BoundingBox(bounds.getMinX(),
                               height - bounds.getMinY() - bounds.getHeight(),
                               bounds.getWidth(),
                               bounds.getHeight());
    }

    /* NSAccessibility Protocol - JNI entry points */
    long[] accessibilityAttributeNames() {
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        Role role = (Role)getAttribute(ROLE);
        if (role != null) {
            List<MacAttribute> attrs = new ArrayList<>(baseAttributes);
            MacRole macRole = getRole(role);
            if (macRole != null && macRole.macAttributes != null) {
                attrs.addAll(macRole.macAttributes);
            }

            /* Look to see if there is a subrole that we should also get the attributes of */
            MacSubrole macSubrole = MacSubrole.getRole(role);
            if (macSubrole != null && macSubrole.macAttributes != null) {
                attrs.addAll(macSubrole.macAttributes);
            }

            switch (role) {
                case LIST_VIEW:
                case TREE_TABLE_VIEW:
                    /* ListView is row-based, must remove all the cell-based attributes */
                    attrs.remove(MacAttribute.NSAccessibilitySelectedCellsAttribute);
                    break;
                case CONTEXT_MENU:
                case MENU_ITEM:
                case MENU_BAR:
                    /* Menu and MenuItem do have have Window and top-level UI Element*/
                    attrs.remove(MacAttribute.NSAccessibilityWindowAttribute);
                    attrs.remove(MacAttribute.NSAccessibilityTopLevelUIElementAttribute);
                    break;
                case TEXT:
                case TEXT_FIELD:
                case TEXT_AREA:
                case PASSWORD_FIELD:
                case COMBOBOX:
                    attrs.addAll(textAttributes);
                    break;
                default:
            }
            return attrs.stream().mapToLong(a -> a.ptr).toArray();
        }
        return null;
    }

    int accessibilityArrayAttributeCount(long attribute) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return -1;
        }
        switch (attr) {
            case NSAccessibilityRowsAttribute: {
                Integer count = (Integer)getAttribute(ROW_COUNT);
                return count != null ? count : 0;
            }
            case NSAccessibilityColumnsAttribute: {
                Integer count = (Integer)getAttribute(COLUMN_COUNT);

                /* 
                 * JFX does not require ListView to report column count == 1
                 * But Mac needs NSAccessibilityColumnCountAttribute == 1 to work
                 */
                return count != null ? count : 1;
            }
            case NSAccessibilityChildrenAttribute: {
                /*
                 * The way VoiceOver identifies a menu item as having a sub menu is
                 * by detecting an AXMenu child. It is important that the AXMenu
                 * child be the actual sub menu so that navigation between menus
                 * work.
                 * Note: strictly the context menu is a child of the PopWindow.
                 */
                if (getAttribute(ROLE) == Role.MENU_ITEM) {
                    @SuppressWarnings("unchecked")
                    ObservableList<Node> children = (ObservableList<Node>)getAttribute(CHILDREN);
                    long[] ids = getUnignoredChildren(children);
                    int count = ids.length;
                    if (getAttribute(MENU_ITEM_TYPE) == Role.CONTEXT_MENU) {
                        count++;
                    }
                    return count;
                }
                return -1;
            }
            case NSAccessibilityDisclosedRowsAttribute: {
                Integer count = (Integer)getAttribute(TREE_ITEM_COUNT);
                return count != null ? count : 0;
            }
            default:
        }
        return -1;
    }

    long[] accessibilityArrayAttributeValues(long attribute, int index, int maxCount) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return null;
        }

        Attribute jfxAttr = null;
        switch (attr) {
            case NSAccessibilityColumnsAttribute: jfxAttr = COLUMN_AT_INDEX; break;
            case NSAccessibilityRowsAttribute: jfxAttr = ROW_AT_INDEX; break;
            case NSAccessibilityDisclosedRowsAttribute: jfxAttr = TREE_ITEM_AT_INDEX; break;
            case NSAccessibilityChildrenAttribute: {
                if (getAttribute(ROLE) == Role.MENU_ITEM) {
                    long[] result = new long[maxCount];
                    int i = 0;
                    if (index == 0) {
                        Node menu = (Node)getAttribute(MENU);
                        result[i++] = getAccessible(menu);
                    }
                    if (i < maxCount) {
                        @SuppressWarnings("unchecked")
                        ObservableList<Node> children = (ObservableList<Node>)getAttribute(CHILDREN);
                        long[] ids = getUnignoredChildren(children);
                        index--;
                        while (i < maxCount && index < ids.length) {
                            result[i++] = ids[index++];
                        }
                    }
                    if (i < maxCount) {
                        result = Arrays.copyOf(result, i);
                    }
                    return result;
                }
                break;
            }
            default:
        }
        if (jfxAttr != null) {
            long[] result = new long[maxCount];
            int i = 0;
            while (i < maxCount) {
                Node node = (Node)getAttribute(jfxAttr, index + i);
                if (node == null) break;
                result[i] = getAccessible(node);
                i++;
            }
            if (i == maxCount) return NSAccessibilityUnignoredChildren(result);;
        }
        return null;
    }

    boolean accessibilityIsAttributeSettable(long attribute) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) return false;
        switch (attr) {
            case NSAccessibilityDisclosingAttribute:
                Integer itemCount = (Integer)getAttribute(TREE_ITEM_COUNT);
                return itemCount != null && itemCount > 0;
            case NSAccessibilityFocusedAttribute:
            case NSAccessibilitySelectedAttribute:
            case NSAccessibilitySelectedRowsAttribute:
            case NSAccessibilitySelectedCellsAttribute:
            case NSAccessibilitySelectedTextRangeAttribute:
                return true;
            default:
        }
        return false;
    }

    MacVariant accessibilityAttributeValue(long attribute) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return null;
        }

        Function<Object, MacVariant> map = attr.map;
        Attribute jfxAttr = attr.jfxAttr;
        Role role = (Role)getAttribute(ROLE);
        if (role == null) return null;
        if (jfxAttr == null) {
            switch (attr) {
                case NSAccessibilityValueAttribute: {
                    switch (role) {
                        case TAB_PANE:
                            jfxAttr = SELECTED_TAB;
                            map = MacVariant::createNSObject;
                            break;
                        case PAGINATION:
                            jfxAttr = SELECTED_PAGE;
                            map = MacVariant::createNSObject;
                            break;
                        case PAGE:
                        case TAB_ITEM:
                        case RADIO_BUTTON:
                            jfxAttr = SELECTED;
                            map = MacVariant::createNSNumberForBoolean;
                            break;
                        case SCROLL_BAR:
                        case SLIDER:
                        case PROGRESS_INDICATOR:
                        case THUMB:
                            jfxAttr = VALUE;
                            map = MacVariant::createNSNumberForDouble;
                            break;
                        case TEXT:
                        case TEXT_FIELD:
                        case TEXT_AREA:
                        case COMBOBOX:
                            jfxAttr = TITLE;
                            map = MacVariant::createNSString;
                            break;
                        case CHECKBOX:
                        case TOGGLE_BUTTON:
                            jfxAttr = SELECTED;
                            map = MacVariant::createNSNumberForInt;
                            break;
                        case DATE_PICKER:
                            jfxAttr = DATE;
                            map = MacVariant::createNSDate;
                            break;
                        case TITLED_PANE:
                            jfxAttr = EXPANDED;
                            map = MacVariant::createNSNumberForInt;
                            break;
                        default:
                            /* VoiceOver can ask NSAccessibilityValueAttribute in unexpected cases, AXColumn for example. */
                            return null;
                    }
                    break;
                }
                case NSAccessibilityTabsAttribute: {
                    switch (role) {
                        case TAB_PANE: jfxAttr = TABS; break;
                        case PAGINATION: jfxAttr = PAGES; break;
                        default:
                    }
                    break;
                }
                case NSAccessibilitySelectedChildrenAttribute: {
                    /* Used for ContextMenu's*/
                    if (role == Role.CONTEXT_MENU) {
                        Scene scene = (Scene)getAttribute(SCENE);
                        if (scene != null) {
                            Accessible acc = scene.getAccessible();
                            if (acc != null) {
                                Node focus = (Node)acc.getAttribute(FOCUS_NODE);
                                if (focus != null && focus.getAccessible().getAttribute(ROLE) == Role.MENU_ITEM) {
                                    long[] result = {getAccessible(focus)};
                                    return attr.map.apply(result);
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                    if (role == Role.MENU_BAR) {
                        Node focus = (Node)getAttribute(FOCUS_NODE);
                        if (focus != null && focus.getAccessible().getAttribute(ROLE) == Role.MENU_ITEM) {
                            long[] result = {getAccessible(focus)};
                            return attr.map.apply(result);
                        } else {
                            return null;
                        }
                    }
                    return null;
                }
                case AXDateTimeComponents: {
                    /* 
                     * AXDateTimeComponents is an undocumented attribute which
                     * is used by native DateTime controls in Cocoa.
                     * It it used a bit vector and 224 indicates that
                     * month, day, and year should be read out.
                     */
                    return attr.map.apply(224);
                }
                default:
              }
        }
        if (jfxAttr == null) {
            return null;
        }
        Object result = getAttribute(jfxAttr);
        if (result == null) {
            switch (attr) {
                case NSAccessibilityParentAttribute: break;
                case NSAccessibilityColumnCountAttribute:
                    /* 
                     * JFX does not require ListView to report column count == 1
                     * But Mac needs NSAccessibilityColumnCountAttribute == 1 to work
                     */
                    result = 1;
                    break;
                case AXMenuItemCmdModifiers:
                    return attr.map.apply(kAXMenuItemModifierNoCommand);
                case NSAccessibilityRoleDescriptionAttribute: {
                    /*
                     * In some cases there is no proper mapping from a JFX role
                     * to a Mac role. For example, reporting 'disclosure triangle'
                     * for a TITLED_PANE is not appropriate.
                     * Providing a custom role description makes it much better.
                     * 
                     * Note: The user can redefine this attribuet by specifying
                     * a DESCRIPTION.
                     */
                    switch (role) {
                        case TITLED_PANE: result = "title pane"; break;
                        case SPLIT_MENU_BUTTON: result = "split button"; break;
                        case PAGE: result = "page"; break;
                        case TAB_ITEM: result = "tab"; break;
                        default:
                            MacRole macRole = getRole(role);
                            MacSubrole subRole = MacSubrole.getRole(role);
                            result = NSAccessibilityRoleDescription(macRole.ptr, subRole != null ? subRole.ptr : 0l);
                    }
                    break;
                }
                default: return null;
            }
        }

        /* Some Attributes need to be modified before creating the MacVariant */
        switch (attr) {
            case NSAccessibilityWindowAttribute:
            case NSAccessibilityTopLevelUIElementAttribute: {
                if (role == Role.CONTEXT_MENU || role == Role.MENU_ITEM || role == Role.MENU_BAR) {
                    return null;
                }
                Scene scene = (Scene)result;
                View view = getRootView(scene);
                if (view == null) return null;
                result = view.getWindow().getNativeWindow();
                break;
            }
            case NSAccessibilitySubroleAttribute: {
                MacSubrole subRole = MacSubrole.getRole((Role)result);
                result = subRole != null ? subRole.ptr : 0L;
                break;
            }
            case NSAccessibilityRoleAttribute: {
                MacRole macRole = getRole(role);
                result = macRole != null ? macRole.ptr : 0L;
                break;
            }
            case NSAccessibilityEnabledAttribute: {
                result = Boolean.FALSE.equals(result);
                break;
            }
            case NSAccessibilitySelectedCellsAttribute:
            case NSAccessibilitySelectedRowsAttribute:
            case NSAccessibilityTabsAttribute:
            case NSAccessibilityVisibleChildrenAttribute:
            case NSAccessibilityChildrenAttribute: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> children = (ObservableList<Node>)result;
                result = getUnignoredChildren(children);
                break;
            }
            case NSAccessibilityParentAttribute: {
                if (getView() != null) {
                    result = getView().getWindow().getNativeWindow();
                } else if (result != null) {
                    if (role == Role.CONTEXT_MENU) {
                        Node menuItem = (Node)getAttribute(MENU_FOR);
                        if (menuItem != null) {
                            if (menuItem.getAccessible().getAttribute(ROLE) == Role.MENU_ITEM) {
                                result = menuItem;
                            }
                        }
                    }
                    result = getAccessible((Node)result);
                } else {
                    /* Root node: return the NSView (instead of acc.getNativeAccessible()) */
                    View view = getRootView((Scene)getAttribute(SCENE));
                    if (view == null) return null;
                    result = view.getNativeView();
                }
                result = NSAccessibilityUnignoredAncestor((long)result);
                break;
            }
            case NSAccessibilityValueAttribute: {
                switch (role) {
                    case TAB_PANE:
                    case PAGINATION:
                        result = getAccessible((Node)result);
                        break;
                    case CHECKBOX:
                    case TOGGLE_BUTTON:
                        if (Boolean.TRUE.equals(getAttribute(INDETERMINATE))) {
                            result = 2;
                        } else {
                            result = Boolean.TRUE.equals(result) ? 1 : 0;
                        }
                        break;
                    case TITLED_PANE:
                        result = Boolean.TRUE.equals(result) ? 1 : 0;
                        break;
                    default:
                }
                break;
            }
            case NSAccessibilityPositionAttribute: {
                /* 
                 * NSAccessibilityPositionAttribute requires the point relative
                 * to the lower-left corner in screen.
                 */
                result = flipBounds((Bounds)result);
                break;
            }
            case NSAccessibilityMaxValueAttribute: {
                /* 
                 * VoiceOver reports 'Indeterminate Progress Indicator' when
                 * the max value is not specified.
                 */
                if (Boolean.TRUE.equals(getAttribute(INDETERMINATE))) {
                    return null;
                }
                break;
            }
            case NSAccessibilityTitleAttribute: {
                /*
                 * Voice over sends title attributes in unexpected cases.
                 * For text roles, where the title is reported in AXValue, reporting 
                 * the value again in AXTitle will cause voice over to read the text twice. 
                 */
                switch (role) {
                    case COMBOBOX:
                    case TEXT:
                    case TEXT_FIELD:
                    case TEXT_AREA:
                    case TREE_TABLE_ITEM: return null;
                    case TREE_TABLE_CELL: {
                        /*
                         * When clicking on a TreeTableRow, only a single cell is selected
                         * by VoiceOver to be read out. Here we add the text for the other
                         * cells in the row, so that all cells are read out.
                         */
                        Node parent = (Node)getAttribute(PARENT);
                        if (parent == null) return null;
                        Accessible acc = parent.getAccessible();
                        if (acc.getAttribute(ROLE) == Role.TREE_TABLE_ITEM) {
                            @SuppressWarnings("unchecked")
                            Stream<Node> children = ((List<Node>)acc.getAttribute(CHILDREN)).stream();

                            result = children.map(n -> n.getAccessible())
                                             .filter(a -> a.getAttribute(ROLE) == Role.TREE_TABLE_CELL)
                                             .map(a -> (String)a.getAttribute(TITLE))
                                             .filter(t -> t != null && !t.isEmpty()) //Consider reporting empty cells as "(blank)"
                                             .reduce((s1, s2) -> s1 + " " + s2)
                                             .orElse("");
                        }
                        break;
                    }
                    default:
                }
                break;
            }
            case AXMenuItemCmdChar: {
                KeyCombination kc = (KeyCombination)result;
                result = null;
                if (kc instanceof KeyCharacterCombination) {
                    result = ((KeyCharacterCombination)kc).getCharacter();
                } 
                if (kc instanceof KeyCodeCombination) {
                    KeyCode code = ((KeyCodeCombination)kc).getCode();
                    if (isCmdCharBased(code)) {
                        result = code.getName();
                    }
                }
                if (result == null) return null;
                break;
            }
            case AXMenuItemCmdVirtualKey: {
                KeyCombination kc = (KeyCombination)result;
                result = null;
                if (kc instanceof KeyCodeCombination) {
                    KeyCode code = ((KeyCodeCombination)kc).getCode();
                    if (!isCmdCharBased(code)) {
                        @SuppressWarnings("deprecation")
                        int keyCode = code.impl_getCode();
                        result = MacApplication._getMacKey(keyCode);
                    }
                }
                if (result == null) return null;
                break;
            }
            case AXMenuItemCmdGlyph: {
                KeyCombination kc = (KeyCombination)result;
                result = null;
                if (kc instanceof KeyCodeCombination) {
                    KeyCode code = ((KeyCodeCombination)kc).getCode();
                    if (!isCmdCharBased(code)) {
                        result = getMenuItemCmdGlyph(code);
                    }                    
                }
                if (result == null) return null;
                break;
            }
            case AXMenuItemCmdModifiers: {
                KeyCombination kc = (KeyCombination)result;
                int mod = kAXMenuItemModifierNoCommand;
                if (kc != null) {
                    if (kc.getShortcut() == KeyCombination.ModifierValue.DOWN) {
                        mod = kAXMenuItemModifierNone;
                    }
                    if (kc.getAlt() == KeyCombination.ModifierValue.DOWN) {
                        mod |= kAXMenuItemModifierOption;
                    }
                    if (kc.getControl() == KeyCombination.ModifierValue.DOWN) {
                        mod |= kAXMenuItemModifierControl;
                    }
                    if (kc.getShift() == KeyCombination.ModifierValue.DOWN) {
                        mod |= kAXMenuItemModifierShift;
                    }
                }
                result = mod;
                break;
            }
            case AXMenuItemMarkChar: {
                if (Boolean.TRUE.equals(result)) {
                    result = "\u2713";
                } else {
                    return null;
                }
                break;
            }
            case NSAccessibilityNumberOfCharactersAttribute: {
                String text = (String)result;
                result = (Integer)text.length();
                break;
            }
            case NSAccessibilitySelectedTextAttribute: {
                int start = (Integer)result, end = -1;
                if (start != -1) {
                    end = (Integer)getAttribute(SELECTION_END);
                }
                if (start < 0 || end < 0 || start > end) return null;
                String string = (String)getAttribute(TITLE);
                if (string == null) return null;
                if (end > string.length()) return null;
                result = string.substring(start, end);
                break;
            }
            case NSAccessibilitySelectedTextRangeAttribute: {
                int start = (Integer)result, end = -1;
                if (start != -1) {
                    end = (Integer)getAttribute(SELECTION_END);
                }
                if (start < 0 || end < 0 || start > end) return null;
                String string = (String)getAttribute(TITLE);
                if (string == null) return null;
                if (end > string.length()) return null;
                result = new int[] {start, end - start};
                break;
            }
            case NSAccessibilityInsertionPointLineNumberAttribute: {
                if (role == Role.TEXT_AREA) {
                    Integer lineIndex = (Integer)getAttribute(LINE_FOR_OFFSET, result /*CARET_OFFSET*/);
                    result = lineIndex != null ? lineIndex : 0;
                } else {
                    /* Combo and TextArea */ 
                    result = 0;
                }
                break;
            }
            case NSAccessibilityVisibleCharacterRangeAttribute: {
                String string = (String)result;
                result = new int[] {0, string.length()};
                break;
            }
            case NSAccessibilityContentsAttribute: {
                if (result != null) {
                    result = new long [] {getAccessible((Node)result)};
                }
                break;
            }
            case NSAccessibilityRowIndexRangeAttribute:
            case NSAccessibilityColumnIndexRangeAttribute: {
                Integer location = (Integer)result;
                result = new int[] {location, 1 /* length */};
                break;
            }
            case NSAccessibilityDisclosedByRowAttribute:
            case NSAccessibilityOverflowButtonAttribute:
            case NSAccessibilityTitleUIElementAttribute:
            case NSAccessibilityHeaderAttribute:
            case NSAccessibilityHorizontalScrollBarAttribute:
            case NSAccessibilityVerticalScrollBarAttribute: {
                result = getAccessible((Node)result);
                break;
            }
            case NSAccessibilityOrientationAttribute:
                Orientation orientation = (Orientation)result;
                switch (orientation) {
                    case HORIZONTAL: result = MacOrientation.NSAccessibilityHorizontalOrientationValue.ptr; break;
                    case VERTICAL: result = MacOrientation.NSAccessibilityVerticalOrientationValue.ptr; break;
                    default: return null;
                }
                break;
            case NSAccessibilityDisclosingAttribute: {
                if (result/*Expanded*/ == Boolean.TRUE) {
                    if (Boolean.TRUE.equals(getAttribute(LEAF))) {
                        result = Boolean.FALSE;
                    }
                }
                break;
            }
            default:
        }
        return map.apply(result);
    }

    void accessibilitySetValue(long value, long attribute) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr != null) {
            switch (attr) {
                case NSAccessibilityExpandedAttribute:
                    if (getAttribute(ROLE) == Role.COMBOBOX) {
                        executeAction(Action.EXPAND);
                    }
                    break;
                case NSAccessibilitySelectedCellsAttribute:
                case NSAccessibilitySelectedRowsAttribute: {
                    MacVariant variant = idToMacVariant(value, MacVariant.NSArray_id);
                    if (variant != null && variant.longArray != null && variant.longArray.length > 0) {
                        long[] ids = variant.longArray;
                        for (long id : ids) {
                            MacAccessible acc = GlassAccessibleToMacAccessible(id);
                            if (acc != null) {
                                acc.executeAction(Action.SELECT);
                            }
                        }
                    }
                    break;
                }
                case NSAccessibilitySelectedTextRangeAttribute: {
                    MacVariant variant = idToMacVariant(value, MacVariant.NSValue_range);
                    if (variant != null) {
                        int start = variant.int1; /* range.location */
                        int end = variant.int1 + variant.int2; /* range.location + range.length */
                        executeAction(Action.SELECT, start, end);
                    }
                    break;
                }
                default:
            }
        }
    }

    long accessibilityIndexOfChild(long child) {
        /* Forward to native code */
        return -1;
    }

    long[] accessibilityParameterizedAttributeNames() {
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        Role role = (Role)getAttribute(ROLE);
        if (role != null) {
            List<MacAttribute> attrs = new ArrayList<>();
            MacRole macRole = getRole(role);
            if (macRole != null && macRole.macParameterizedAttributes != null) {
                attrs.addAll(macRole.macParameterizedAttributes);
            }
            switch (role) {
                case LIST_VIEW:
                case TREE_TABLE_VIEW:
                    /* ListView is row-based, must remove all the cell-based attributes */
                    attrs.remove(MacAttribute.NSAccessibilityCellForColumnAndRowParameterizedAttribute);
                    break;
                case TEXT:
                case TEXT_FIELD:
                case TEXT_AREA:
                case PASSWORD_FIELD:
                case COMBOBOX:
                    attrs.addAll(textParameterizedAttributes);
                    break;
                default:
            }
            return attrs.stream().mapToLong(a -> a.ptr).toArray();
        }
        return null;
    }

    MacVariant accessibilityAttributeValueForParameter(long attribute, long parameter) {
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null || attr.inputType == 0 || attr.jfxAttr == null) {
            return null;
        }
        MacVariant variant = idToMacVariant(parameter, attr.inputType);
        if (variant == null) return null;
        Object value = variant.getValue();
        Object result;
        switch (attr) {
            case NSAccessibilityCellForColumnAndRowParameterizedAttribute: {
                int[] intArray = (int[])value;
                result = getAttribute(attr.jfxAttr, intArray[1] /*row*/, intArray[0] /*column*/);
                break;
            }
            case NSAccessibilityLineForIndexParameterizedAttribute: {
                if (getAttribute(ROLE) == Role.TEXT_AREA) {
                    result = getAttribute(attr.jfxAttr, value /*charOffset*/);
                } else {
                    /* Combo and TextField */
                    result = 0;
                }
                break;
            }
            case NSAccessibilityRangeForLineParameterizedAttribute: {
                if (getAttribute(ROLE) == Role.TEXT_AREA) {
                    Integer lineStart = (Integer)getAttribute(LINE_START, value /*line index*/);
                    Integer lineEnd = (Integer)getAttribute(LINE_END, value /*line index*/);
                    if (lineStart != null && lineEnd != null) {
                        result = new int[] {lineStart, lineEnd - lineStart}; 
                    } else {
                        result = null;
                    }
                } else {
                    /* Combo and TextField */
                    String text = (String)getAttribute(TITLE);
                    result = new int[] {0, text != null ? text.length() : 0};
                }
                break;
            }
            case NSAccessibilityBoundsForRangeParameterizedAttribute: {
                int[] intArray = (int[])value; /* range.location, range.length */
                Bounds[] bounds = (Bounds[])getAttribute(attr.jfxAttr, intArray[0], intArray[0] + intArray[1] - 1);
                double left = Double.POSITIVE_INFINITY;
                double top = Double.POSITIVE_INFINITY;
                double right = Double.NEGATIVE_INFINITY;
                double bottom = Double.NEGATIVE_INFINITY;
                if (bounds != null) {
                    for (int i = 0; i < bounds.length; i++) {
                        Bounds b = bounds[i];
                        if (b != null) {
                            if (b.getMinX() < left) left = b.getMinX();
                            if (b.getMinY() < top) top = b.getMinY();
                            if (b.getMaxX() > right) right = b.getMaxX();
                            if (b.getMaxY() > bottom) bottom = b.getMaxY();
                        }
                    }
                }
                result = flipBounds(new BoundingBox(left, top, right - left, bottom - top));
                break;
            }
            case NSAccessibilityRangeForPositionParameterizedAttribute: {
                float[] floatArray = (float[])value;
                Integer offset = (Integer)getAttribute(attr.jfxAttr, new Point2D(floatArray[0], floatArray[1]));
                if (offset != null) {
                    result = new int[] {offset, 1};
                } else {
                    result = null;
                }
                break;
            }
            default:
                result = getAttribute(attr.jfxAttr, value);
        }
        if (result == null) return null;
        switch (attr) {
            case NSAccessibilityAttributedStringForRangeParameterizedAttribute: {
                String text = (String)result;
                text = text.substring(variant.int1, variant.int1 + variant.int2);
                List<MacVariant> styles = new ArrayList<>();
                Font font = (Font)getAttribute(FONT);
                if (font != null) {
                    MacVariant fontDict = new MacVariant();
                    fontDict.type = MacVariant.NSDictionary;
                    fontDict.longArray = new long[] {
                        MacText.NSAccessibilityFontNameKey.ptr,
                        MacText.NSAccessibilityFontFamilyKey.ptr,
                        MacText.NSAccessibilityVisibleNameKey.ptr,
                        MacText.NSAccessibilityFontSizeKey.ptr,
                    };
                    fontDict.variantArray = new MacVariant[] {
                        MacVariant.createNSString(font.getName()),
                        MacVariant.createNSString(font.getFamily()),
                        MacVariant.createNSString(font.getName()),
                        MacVariant.createNSNumberForDouble(font.getSize()),
                    };

                    fontDict.key = MacText.NSAccessibilityFontTextAttribute.ptr;
                    fontDict.location = 0;
                    fontDict.length = text.length();
                    styles.add(fontDict);
                }
                MacVariant attrString = attr.map.apply(text);
                attrString.variantArray = styles.toArray(new MacVariant[0]);
                return attrString;
            }
            case NSAccessibilityStringForRangeParameterizedAttribute: {
                String text = (String)result;
                result = text.substring(variant.int1, variant.int1 + variant.int2);
                break;
            }
            case NSAccessibilityCellForColumnAndRowParameterizedAttribute: {
                result = getAccessible((Node)result);
                break;
            }
            default:
        }
        return attr.map.apply(result);
    }

    long[] accessibilityActionNames() {
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        Role role = (Role)getAttribute(ROLE);
        List<MacAction> actions = new ArrayList<>();
        if (role != null) {
            MacRole macRole = getRole(role);
            if (macRole != null && macRole.macActions != null) {
                actions.addAll(macRole.macActions);
            }
            /* 
             * Consider add a attribute to indicate when the node
             * has a menu instead of using the role.
             */
            if (role != Role.NODE && role != Role.PARENT) {
                actions.add(MacAction.NSAccessibilityShowMenuAction);
            }
        }
        /* Return empty array instead of null to prevent warnings in the accessibility verifier */
        return actions.stream().mapToLong(a -> a.ptr).toArray();
    }

    String accessibilityActionDescription(long action) {
        return NSAccessibilityActionDescription(action);
    }

    void accessibilityPerformAction(long action) {
        MacAction macAction = MacAction.getAction(action);
        boolean expand = false;
        if (macAction == MacAction.NSAccessibilityPressAction) {
            Role role = (Role)getAttribute(ROLE);
            if (role == Role.TITLED_PANE || role == Role.COMBOBOX) {
                expand = true;
            }
        }
        if (macAction == MacAction.NSAccessibilityShowMenuAction) {
            if (getAttribute(ROLE) == Role.SPLIT_MENU_BUTTON) {
                expand = true;
            }
        }
        if (expand) {
            if (Boolean.TRUE.equals(getAttribute(EXPANDED))) {
                executeAction(Action.COLLAPSE);
            } else {
                executeAction(Action.EXPAND);
            }
            return;
        }
        if (macAction != null && macAction.jfxAction != null) {
            executeAction(macAction.jfxAction);
        }
    }

    long accessibilityFocusedUIElement() {
        Node node = (Node)getAttribute(FOCUS_NODE);
        if (node == null) return 0L;

        Node item = (Node)node.getAccessible().getAttribute(FOCUS_ITEM);
        if (item != null) return getAccessible(item);
        return getAccessible(node);
    }

    boolean accessibilityIsIgnored() {
        if (isIgnored()) return true;
        if (isInSlider()) {
            /* 
             * Ignoring the children within the slider, otherwise VoiceOver
             * reports 'multiple indicator slider' instead of the value.
             */
            return true;
        }
        if (isInMenu()) {
            Role role = (Role)getAttribute(ROLE);
            return role != Role.CONTEXT_MENU && role != Role.MENU_ITEM && role != Role.MENU_BAR;
        }
        if (ignoreInnerText()) {
            return true;
        }
        return false;
    }

    long accessibilityHitTest(float x, float y) {
        View view = getView();
        if (view == null) {
            return 0L;
        }
        Screen screen = view.getWindow().getScreen();
        y = screen.getHeight() - y;
        Node node = (Node)getAttribute(NODE_AT_POINT, new Point2D(x, y));
        return NSAccessibilityUnignoredAncestor(getAccessible(node));
  }

}
