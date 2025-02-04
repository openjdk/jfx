/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import com.sun.glass.ui.Accessible;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import static javafx.scene.AccessibleAttribute.*;

/**
 * Native Interface - Implements NSAccessibility Protocol
 *
 */
final class MacAccessible extends Accessible {

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

    private static enum MacAttribute {
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
        NSAccessibilityRoleDescriptionAttribute(ROLE_DESCRIPTION, MacVariant::createNSString),
        NSAccessibilitySizeAttribute(BOUNDS, MacVariant::createNSValueForSize),
        NSAccessibilityTabsAttribute(ITEM_COUNT, MacVariant::createNSArray),
        NSAccessibilityTitleAttribute(TEXT, MacVariant::createNSString),
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
        NSAccessibilityNumberOfCharactersAttribute(TEXT, MacVariant::createNSNumberForInt),
        NSAccessibilitySelectedTextAttribute(SELECTION_START, MacVariant::createNSString),
        NSAccessibilitySelectedTextRangeAttribute(SELECTION_START, MacVariant::createNSValueForRange),
        NSAccessibilitySelectedTextRangesAttribute(null, null), //TODO Array of ranges
        NSAccessibilityInsertionPointLineNumberAttribute(CARET_OFFSET, MacVariant::createNSNumberForInt),
        NSAccessibilityVisibleCharacterRangeAttribute(TEXT, MacVariant::createNSValueForRange),

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
        NSAccessibilitySelectedRowsAttribute(SELECTED_ITEMS, MacVariant::createNSArray),
        NSAccessibilityRowCountAttribute(ROW_COUNT, MacVariant::createNSNumberForInt),
        NSAccessibilityColumnCountAttribute(COLUMN_COUNT, MacVariant::createNSNumberForInt),
        NSAccessibilitySelectedCellsAttribute(SELECTED_ITEMS, MacVariant::createNSArray),
        NSAccessibilityRowIndexRangeAttribute(ROW_INDEX, MacVariant::createNSValueForRange),
        NSAccessibilityColumnIndexRangeAttribute(COLUMN_INDEX, MacVariant::createNSValueForRange),

        // Parameterized Attributes
        NSAccessibilityLineForIndexParameterizedAttribute(LINE_FOR_OFFSET, MacVariant::createNSNumberForInt, MacVariant.NSNumber_Int),
        NSAccessibilityStringForRangeParameterizedAttribute(TEXT, MacVariant::createNSString, MacVariant.NSValue_range),
        NSAccessibilityRangeForLineParameterizedAttribute(LINE_START, MacVariant::createNSValueForRange, MacVariant.NSNumber_Int),
        NSAccessibilityAttributedStringForRangeParameterizedAttribute(TEXT, MacVariant::createNSAttributedString, MacVariant.NSValue_range),
        NSAccessibilityCellForColumnAndRowParameterizedAttribute(CELL_AT_ROW_COLUMN, MacVariant::createNSObject, MacVariant.NSArray_int),
        NSAccessibilityRangeForPositionParameterizedAttribute(OFFSET_AT_POINT, MacVariant::createNSValueForRange, MacVariant.NSValue_point),
        NSAccessibilityBoundsForRangeParameterizedAttribute(BOUNDS_FOR_RANGE, MacVariant::createNSValueForRectangle, MacVariant.NSValue_range),

        ;long ptr; /* Initialized natively - treat as final */
        AccessibleAttribute jfxAttr;
        Function<Object, MacVariant> map; /* Maps the object returned by JavaFX to the appropriate MacVariant */
        int inputType; /* Defined only for parameterized attributes to convert the native input parameter (id) to MacVariant */

        MacAttribute(AccessibleAttribute jfxAttr, Function<Object, MacVariant> map, int inputType) {
            this.jfxAttr = jfxAttr;
            this.map = map;
            this.inputType = inputType;
        }

        MacAttribute(AccessibleAttribute jfxAttr, Function<Object, MacVariant> map) {
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
    private static enum MacRole {
        NSAccessibilityUnknownRole(AccessibleRole.NODE, null, null),
        NSAccessibilityGroupRole(AccessibleRole.PARENT, null, null),
        NSAccessibilityButtonRole(new AccessibleRole[] {AccessibleRole.BUTTON, AccessibleRole.INCREMENT_BUTTON, AccessibleRole.DECREMENT_BUTTON, AccessibleRole.SPLIT_MENU_BUTTON},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityIncrementorRole(AccessibleRole.SPINNER,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityIncrementAction,
                MacAction.NSAccessibilityDecrementAction,
            }
        ),
        /* AXJFXTOOLTIP is a custom name used to ignore the tooltip window. See GlassWindow.m for details. */
        AXJFXTOOLTIP(AccessibleRole.TOOLTIP, null, null),
        NSAccessibilityImageRole(AccessibleRole.IMAGE_VIEW, null, null),
        NSAccessibilityRadioButtonRole(new AccessibleRole[] {AccessibleRole.RADIO_BUTTON, AccessibleRole.TAB_ITEM, AccessibleRole.PAGE_ITEM},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityCheckBoxRole(new AccessibleRole[] {AccessibleRole.CHECK_BOX, AccessibleRole.TOGGLE_BUTTON},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction},
            null
        ),
        /* ComboBox can be either a NSAccessibilityComboBoxRole or a NSAccessibilityPopUpButtonRole (Based on EDITABLE) */
        NSAccessibilityComboBoxRole(AccessibleRole.COMBO_BOX,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityExpandedAttribute
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction}
        ),
        NSAccessibilityPopUpButtonRole(AccessibleRole.COMBO_BOX,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            new MacAction[] {MacAction.NSAccessibilityPressAction}
        ),
        NSAccessibilityTabGroupRole(new AccessibleRole[] {AccessibleRole.TAB_PANE, AccessibleRole.PAGINATION},
            new MacAttribute[] {
//              MacAttributes.NSAccessibilityContentsAttribute,
                MacAttribute.NSAccessibilityTabsAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
            },
            null,
            null
        ),
        NSAccessibilityProgressIndicatorRole(AccessibleRole.PROGRESS_INDICATOR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityOrientationAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.NSAccessibilityMaxValueAttribute,
                MacAttribute.NSAccessibilityMinValueAttribute,
            },
            null
        ),
        NSAccessibilityMenuBarRole(AccessibleRole.MENU_BAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySelectedChildrenAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuRole(AccessibleRole.CONTEXT_MENU,
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySelectedChildrenAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction,
                MacAction.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuItemRole(new AccessibleRole[] {AccessibleRole.MENU_ITEM, AccessibleRole.RADIO_MENU_ITEM, AccessibleRole.CHECK_MENU_ITEM, AccessibleRole.MENU},
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
            },
            null
        ),
        NSAccessibilityMenuButtonRole(AccessibleRole.MENU_BUTTON,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityTitleAttribute,
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction,
            }
        ),
        NSAccessibilityStaticTextRole(new AccessibleRole[] {AccessibleRole.TEXT},
            null, null, null
        ),
        NSAccessibilityTextFieldRole(new AccessibleRole[] {AccessibleRole.TEXT_FIELD, AccessibleRole.PASSWORD_FIELD},
            null, null, null
        ),
        NSAccessibilityTextAreaRole(AccessibleRole.TEXT_AREA, null, null),
        NSAccessibilitySliderRole(AccessibleRole.SLIDER,
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
        NSAccessibilityScrollAreaRole(AccessibleRole.SCROLL_PANE,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityContentsAttribute,
                MacAttribute.NSAccessibilityHorizontalScrollBarAttribute,
                MacAttribute.NSAccessibilityVerticalScrollBarAttribute,
            },
            null
        ),
        NSAccessibilityScrollBarRole(AccessibleRole.SCROLL_BAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.NSAccessibilityMinValueAttribute,
                MacAttribute.NSAccessibilityMaxValueAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityOrientationAttribute,
            },
            null
        ),
        NSAccessibilityValueIndicatorRole(AccessibleRole.THUMB,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityValueAttribute,
            },
            null
        ),
        NSAccessibilityRowRole(new AccessibleRole[] {
                AccessibleRole.LIST_ITEM,
                AccessibleRole.TABLE_ROW,
                AccessibleRole.TREE_ITEM,
                AccessibleRole.CHECK_BOX_TREE_ITEM,
                AccessibleRole.TREE_TABLE_ROW
            },
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute,
                MacAttribute.NSAccessibilityIndexAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
                MacAttribute.NSAccessibilityVisibleChildrenAttribute,
            },
            null, null
        ),
        NSAccessibilityTableRole(new AccessibleRole[] {AccessibleRole.LIST_VIEW, AccessibleRole.TABLE_VIEW},
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
        NSAccessibilityColumnRole(AccessibleRole.TABLE_COLUMN,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityHeaderAttribute,
                MacAttribute.NSAccessibilityIndexAttribute,
                MacAttribute.NSAccessibilityRowsAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
            },
            null
        ),
        NSAccessibilityCellRole(new AccessibleRole[] {AccessibleRole.TABLE_CELL, AccessibleRole.TREE_TABLE_CELL},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityColumnIndexRangeAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityRowIndexRangeAttribute,
                MacAttribute.NSAccessibilitySelectedAttribute,
            },
            null,
            null
        ),
        NSAccessibilityLinkRole(AccessibleRole.HYPERLINK,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.AXVisited
            },
            null
        ),
        NSAccessibilityOutlineRole(new AccessibleRole[] {AccessibleRole.TREE_VIEW, AccessibleRole.TREE_TABLE_VIEW},
            new MacAttribute[] {
                MacAttribute.NSAccessibilityColumnsAttribute,
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityHeaderAttribute,
                MacAttribute.NSAccessibilityRowsAttribute,
                MacAttribute.NSAccessibilitySelectedRowsAttribute,
                MacAttribute.NSAccessibilitySelectedCellsAttribute,
            },
            null,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityCellForColumnAndRowParameterizedAttribute,
            }
        ),
        NSAccessibilityDisclosureTriangleRole(AccessibleRole.TITLED_PANE,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute
            },
            new MacAction[] {
                MacAction.NSAccessibilityPressAction
            }
        ),
        NSAccessibilityToolbarRole(AccessibleRole.TOOL_BAR,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityOverflowButtonAttribute,
            },
            null
        ),
        AXDateTimeArea(AccessibleRole.DATE_PICKER,
            new MacAttribute[] {
                MacAttribute.NSAccessibilityEnabledAttribute,
                MacAttribute.NSAccessibilityValueAttribute,
                MacAttribute.AXDateTimeComponents,
            },
            null
        ),

        ;long ptr; /* Initialized natively - treat as final */
        AccessibleRole[] jfxRoles;
        List<MacAttribute> macAttributes;
        List<MacAttribute> macParameterizedAttributes;
        List<MacAction> macActions;
        MacRole(AccessibleRole jfxRole, MacAttribute[] macAttributes, MacAction[] macActions) {
            this(new AccessibleRole[] {jfxRole}, macAttributes, macActions, null);
        }

        MacRole(AccessibleRole[] jfxRoles, MacAttribute[] macAttributes, MacAction[] macActions, MacAttribute[] macParameterizedAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
            this.macActions = macActions != null ? Arrays.asList(macActions) : null;
            this.macParameterizedAttributes = macParameterizedAttributes != null ? Arrays.asList(macParameterizedAttributes) : null;
        }

        static MacRole getRole(AccessibleRole targetRole) {
            if (targetRole == null) return null;
            for (MacRole macRole : values()) {
                for (AccessibleRole jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    private static enum MacSubrole {
        NSAccessibilityTableRowSubrole(AccessibleRole.LIST_ITEM, AccessibleRole.TABLE_ROW),
        NSAccessibilitySecureTextFieldSubrole(AccessibleRole.PASSWORD_FIELD),
        NSAccessibilityOutlineRowSubrole(new AccessibleRole[] {
                AccessibleRole.TREE_ITEM,
                AccessibleRole.CHECK_BOX_TREE_ITEM,
                AccessibleRole.TREE_TABLE_ROW
            },
            new MacAttribute[] {
                MacAttribute.NSAccessibilityDisclosedByRowAttribute,
                MacAttribute.NSAccessibilityDisclosedRowsAttribute,
                MacAttribute.NSAccessibilityDisclosingAttribute,
                MacAttribute.NSAccessibilityDisclosureLevelAttribute
            }
        ),
        NSAccessibilityDecrementArrowSubrole(new AccessibleRole[] { AccessibleRole.DECREMENT_BUTTON },
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute
            }
        ),
        NSAccessibilityIncrementArrowSubrole(new AccessibleRole[] { AccessibleRole.INCREMENT_BUTTON },
            new MacAttribute[] {
                MacAttribute.NSAccessibilitySubroleAttribute
            }
        )

        ;long ptr; /* Initialized natively - treat as final */
        AccessibleRole[] jfxRoles;
        List<MacAttribute> macAttributes;

        MacSubrole(AccessibleRole... jfxRoles) {
            this(jfxRoles, null);
        }

        MacSubrole(AccessibleRole[] jfxRoles, MacAttribute[] macAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
        }

        static MacSubrole getRole(AccessibleRole targetRole) {
            if (targetRole == null) return null;
            for (MacSubrole macRole : values()) {
                for (AccessibleRole jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    private static enum MacAction {
        NSAccessibilityCancelAction,
        NSAccessibilityConfirmAction,
        NSAccessibilityDecrementAction(AccessibleAction.DECREMENT),
        NSAccessibilityDeleteAction,
        NSAccessibilityIncrementAction(AccessibleAction.INCREMENT),
        NSAccessibilityPickAction,
        NSAccessibilityPressAction(AccessibleAction.FIRE),
        NSAccessibilityRaiseAction,
        NSAccessibilityShowMenuAction(AccessibleAction.SHOW_MENU),

        ;long ptr; /* Initialized natively - treat as final */
        AccessibleAction jfxAction;
        MacAction() {}
        MacAction(AccessibleAction jfxAction) {
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

    private static enum MacNotification {
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

    private static enum MacOrientation {
        NSAccessibilityHorizontalOrientationValue,
        NSAccessibilityVerticalOrientationValue,
        NSAccessibilityUnknownOrientationValue,
        ;long ptr; /* Initialized natively - treat as final */
    }

    private static enum MacText {
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
    private static final List<MacAttribute> BASE_ATTRIBUTES = List.of(
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

    private static final List<MacAttribute> TEXT_ATTRIBUTES = List.of(
        MacAttribute.NSAccessibilityEnabledAttribute,
        MacAttribute.NSAccessibilityValueAttribute,
        MacAttribute.NSAccessibilityNumberOfCharactersAttribute,
        MacAttribute.NSAccessibilitySelectedTextAttribute,
        MacAttribute.NSAccessibilitySelectedTextRangeAttribute,
        MacAttribute.NSAccessibilityInsertionPointLineNumberAttribute,
        MacAttribute.NSAccessibilityVisibleCharacterRangeAttribute
    );

    private static final List<MacAttribute> TEXT_PARAMETRIZED_ATTRIBUTES = List.of(
        MacAttribute.NSAccessibilityLineForIndexParameterizedAttribute,
        MacAttribute.NSAccessibilityRangeForLineParameterizedAttribute,
        MacAttribute.NSAccessibilityAttributedStringForRangeParameterizedAttribute,
        MacAttribute.NSAccessibilityStringForRangeParameterizedAttribute
    );

    /* The native peer associated with the instance */
    private long peer;

    /* Creates a native accessible peer linked to the caller (GlobalRef) */
    private native long _createAccessiblePeer(String forRole);

    /* Releases the native accessible peer and deletes the GlobalRef */
    private native void _destroyAccessiblePeer(long accessible);

    /* Notify accessible peer about hierarchy change to invalidate parent */
    private native void _invalidateParent(long accessible);

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

    MacAccessible() {
    }

    @Override
    public void dispose() {
        Application.checkEventThread();
        if (peer != 0L) {
            if (getView() == null) {
                NSAccessibilityPostNotification(peer, MacNotification.NSAccessibilityUIElementDestroyedNotification.ptr);
            }
            _destroyAccessiblePeer(peer);
            peer = 0L;
        }
        super.dispose();
    }

    @Override
    public void sendNotification(AccessibleAttribute notification) {
        Application.checkEventThread();
        if (isDisposed()) return;

        MacNotification macNotification = null;
        switch (notification) {
            case FOCUS_ITEM: {
                AccessibleRole role = (AccessibleRole) getAttribute(ROLE);
                if (role == AccessibleRole.TABLE_VIEW || role == AccessibleRole.TREE_TABLE_VIEW) {
                    /* Cell based controls */
                    macNotification = MacNotification.NSAccessibilitySelectedCellsChangedNotification;
                } else if (role == AccessibleRole.LIST_VIEW || role == AccessibleRole.TREE_VIEW) {
                    /* Row based controls */
                    macNotification = MacNotification.NSAccessibilitySelectedRowsChangedNotification;
                } else {
                    /* TabPane and Pagination */
                    Node node = (Node)getAttribute(FOCUS_ITEM);
                    long id = getNativeAccessible(node);
                    if (id != 0) {
                        NSAccessibilityPostNotification(id, MacNotification.NSAccessibilityFocusedUIElementChangedNotification.ptr);
                    }
                }
                break;
            }
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
                        Accessible acc = getAccessible(scene);
                        if (acc != null) {
                            node = (Node)acc.getAttribute(FOCUS_NODE);
                        }
                    }
                }

                long id = 0L;
                if (node != null) {
                    Node item = (Node)getAccessible(node).getAttribute(FOCUS_ITEM);
                    id = item != null ? getNativeAccessible(item) : getNativeAccessible(node);
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

                AccessibleRole role = (AccessibleRole) getAttribute(ROLE);
                if (role == AccessibleRole.TREE_ITEM || role == AccessibleRole.CHECK_BOX_TREE_ITEM || role == AccessibleRole.TREE_TABLE_ROW) {
                    AccessibleRole containerRole = (role == AccessibleRole.TREE_ITEM || role == AccessibleRole.CHECK_BOX_TREE_ITEM) ?
                                                                                AccessibleRole.TREE_VIEW : AccessibleRole.TREE_TABLE_VIEW;
                    MacAccessible container = (MacAccessible)getContainerAccessible(containerRole);
                    if (container != null) {
                        NSAccessibilityPostNotification(container.getNativeAccessible(), MacNotification.NSAccessibilityRowCountChangedNotification.ptr);
                    }
                }
                break;
            case VISIBLE: {
                if (getAttribute(ROLE) == AccessibleRole.CONTEXT_MENU) {
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
                        Node menuItemOwner = (Node)getAttribute(PARENT_MENU);
                        MacAccessible acc  = (MacAccessible)getAccessible(menuItemOwner);
                        if (acc != null) {
                            MacAccessible menu = (MacAccessible)acc.getContainerAccessible(AccessibleRole.CONTEXT_MENU);
                            if (menu != null) {
                                long ptr = menu.getNativeAccessible();
                                NSAccessibilityPostNotification(ptr, MacNotification.AXMenuClosed.ptr);
                                NSAccessibilityPostNotification(ptr, MacNotification.AXMenuOpened.ptr);
                            }
                        }
                    }
                }
                break;
            }
            case TEXT:
                if (getAttribute(ROLE) == AccessibleRole.SPINNER) {
                    macNotification = MacNotification.NSAccessibilityTitleChangedNotification;
                } else {
                    macNotification = MacNotification.NSAccessibilityValueChangedNotification;
                }
                break;
            case VALUE_STRING:
                if (getAttribute(ROLE) == AccessibleRole.SPINNER) {
                    macNotification = MacNotification.NSAccessibilityValueChangedNotification;
                }
                break;
            case PARENT:
                if (peer != 0L) {
                    _invalidateParent(peer);
                }
                ignoreInnerText = null;
                break;
            default:
                macNotification = MacNotification.NSAccessibilityValueChangedNotification;
        }
        if (macNotification != null) {
            View view = getView();
            long id = view != null ? view.getNativeView() : getNativeAccessible();
            NSAccessibilityPostNotification(id, macNotification.ptr);
        }
    }

    @Override
    protected long getNativeAccessible() {
        Application.checkEventThread();
        if (this.peer == 0L) {
            AccessibleRole role = (AccessibleRole) getAttribute(ROLE);
            if (role == null) role = AccessibleRole.NODE;
            this.peer = _createAccessiblePeer(role.toString());
            if (this.peer == 0L) {
                throw new RuntimeException("could not create platform accessible");
            }
        }

        return peer;
    }

    private View getRootView(Scene scene) {
        Application.checkEventThread();
        if (scene == null) return null;
        Accessible acc = getAccessible(scene);
        if (acc == null || acc.isDisposed()) return null;
        View view = acc.getView();
        if (view == null || view.isClosed()) return null;
        return view;
    }

    private long[] getUnignoredChildren(ObservableList<Node> children) {
        Application.checkEventThread();
        if (children == null) return new long[0];
        long[] ids = children.stream()
                             .filter(Node::isVisible)
                             .mapToLong(n -> getNativeAccessible(n))
                             .filter(n -> n != 0)
                             .toArray();
        return NSAccessibilityUnignoredChildren(ids);
    }

    private Boolean inMenu;
    private boolean isInMenu() {
        /* This flag will be wrong if the Node is ever re-parented */
        if (inMenu == null) {
            inMenu = getContainerAccessible(AccessibleRole.CONTEXT_MENU) != null || getContainerAccessible(AccessibleRole.MENU_BAR) != null;
        }
        return inMenu;
    }

    private boolean isMenuElement(AccessibleRole role) {
        if (role == null) return false;
        switch (role) {
            case MENU_BAR:
            case CONTEXT_MENU:
            case MENU_ITEM:
            case RADIO_MENU_ITEM:
            case CHECK_MENU_ITEM:
            case MENU: return true;
            default: return false;
        }
    }

    private Boolean inSlider;
    private boolean isInSlider() {
        /* This flag will be wrong if the Node is ever re-parented */
        if (inSlider == null) {
            inSlider = getContainerAccessible(AccessibleRole.SLIDER) != null;
        }
        return inSlider;
    }

    private Boolean ignoreInnerText;
    private boolean ignoreInnerText() {
        if (ignoreInnerText != null) return ignoreInnerText;
        /*
         * JavaFX controls are implemented by the skin by adding new nodes.
         * In accessibility these nodes sometimes duplicate the data in the
         * control. For example, a Label is implemented using a Text, creating a
         * AXStaticText inside an AXStaticText. In order to  improve accessibility
         * navigation to following code ignores these inner text for the most
         * common cases.
         */
        AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
        ignoreInnerText = false;
        if (role == AccessibleRole.TEXT) {
            Node parent = (Node)getAttribute(PARENT);
            if (parent == null) return ignoreInnerText;
            AccessibleRole parentRole = (AccessibleRole)getAccessible(parent).getAttribute(ROLE);
            if (parentRole == null) return ignoreInnerText;
            switch (parentRole) {
                case BUTTON:
                case TOGGLE_BUTTON:
                case CHECK_BOX:
                case RADIO_BUTTON:
                case COMBO_BOX:
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

    private MacRole getRole(AccessibleRole role) {
        if (role == AccessibleRole.COMBO_BOX) {
            if (Boolean.TRUE.equals(getAttribute(EDITABLE))) {
                return MacRole.NSAccessibilityComboBoxRole;
            } else {
                return MacRole.NSAccessibilityPopUpButtonRole;
            }
        }
        return MacRole.getRole(role);
    }

    private Bounds flipBounds(Bounds bounds) {
        Screen screen = Screen.getMainScreen();
        float height = screen.getHeight();
        return new BoundingBox(bounds.getMinX(),
                               height - bounds.getMinY() - bounds.getHeight(),
                               bounds.getWidth(),
                               bounds.getHeight());
    }

    /* NSAccessibility Protocol - JNI entry points */
    private long[] accessibilityAttributeNames() {
        Application.checkEventThread();
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
        if (role != null) {
            List<MacAttribute> attrs = new ArrayList<>(BASE_ATTRIBUTES);
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
                case TREE_VIEW:
                    /* Row-based control, must remove all the cell-based attributes */
                    attrs.remove(MacAttribute.NSAccessibilitySelectedCellsAttribute);
                    break;
                case MENU_ITEM:
                case RADIO_MENU_ITEM:
                case CHECK_MENU_ITEM:
                case MENU:
                case CONTEXT_MENU:
                case MENU_BAR:
                    /* Menu and MenuItem do have have Window and top-level UI Element*/
                    attrs.remove(MacAttribute.NSAccessibilityWindowAttribute);
                    attrs.remove(MacAttribute.NSAccessibilityTopLevelUIElementAttribute);
                    break;
                case TEXT:
                case TEXT_FIELD:
                case TEXT_AREA:
                case PASSWORD_FIELD:
                case COMBO_BOX:
                    attrs.addAll(TEXT_ATTRIBUTES);
                    break;
                default:
            }
            return attrs.stream().mapToLong(a -> a.ptr).toArray();
        }
        return null;
    }

    private int accessibilityArrayAttributeCount(long attribute) {
        Application.checkEventThread();
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return -1;
        }
        switch (attr) {
            case NSAccessibilityRowsAttribute: {
                AccessibleAttribute jfxAttr;
                if (getAttribute(ROLE) == AccessibleRole.LIST_VIEW) {
                    jfxAttr = AccessibleAttribute.ITEM_COUNT;
                } else {
                    jfxAttr = AccessibleAttribute.ROW_COUNT;
                }
                Integer count = (Integer)getAttribute(jfxAttr);
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
                if (getAttribute(ROLE) == AccessibleRole.MENU) {
                    @SuppressWarnings("unchecked")
                    ObservableList<Node> children = (ObservableList<Node>)getAttribute(CHILDREN);
                    if (children == null) return 0;
                    long[] ids = getUnignoredChildren(children);
                    int count = ids.length;
                    if (getAttribute(SUBMENU) != null) {
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

    private long[] accessibilityArrayAttributeValues(long attribute, int index, int maxCount) {
        Application.checkEventThread();
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return null;
        }

        AccessibleAttribute jfxAttr = null;
        switch (attr) {
            case NSAccessibilityColumnsAttribute: jfxAttr = COLUMN_AT_INDEX; break;
            case NSAccessibilityRowsAttribute: {
                if (getAttribute(ROLE) == AccessibleRole.LIST_VIEW) {
                    jfxAttr = AccessibleAttribute.ITEM_AT_INDEX;
                } else {
                    jfxAttr = AccessibleAttribute.ROW_AT_INDEX;
                }
                break;
            }
            case NSAccessibilityDisclosedRowsAttribute: jfxAttr = TREE_ITEM_AT_INDEX; break;
            case NSAccessibilityChildrenAttribute: {
                if (getAttribute(ROLE) == AccessibleRole.MENU) {
                    long[] result = new long[maxCount];
                    int i = 0;
                    if (index == 0) {
                        Node menu = (Node)getAttribute(SUBMENU);
                        if (menu != null) result[i++] = getNativeAccessible(menu);
                    }
                    if (i < maxCount) {
                        @SuppressWarnings("unchecked")
                        ObservableList<Node> children = (ObservableList<Node>)getAttribute(CHILDREN);
                        if (children == null) return null;
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
                result[i] = getNativeAccessible(node);
                i++;
            }
            if (i == maxCount) return NSAccessibilityUnignoredChildren(result);
        }
        return null;
    }

    private boolean accessibilityIsAttributeSettable(long attribute) {
        Application.checkEventThread();
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
                return true;
            case NSAccessibilityValueAttribute:
            case NSAccessibilitySelectedTextRangeAttribute: {
                AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
                if (role == AccessibleRole.TEXT_FIELD || role == AccessibleRole.TEXT_AREA) {
                    if (Boolean.TRUE.equals(getAttribute(EDITABLE))) {
                        return true;
                    }
                }
                break;
            }
            default:
        }
        return false;
    }

    private MacVariant accessibilityAttributeValue(long attribute) {
        Application.checkEventThread();
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr == null) {
            return null;
        }

        Function<Object, MacVariant> map = attr.map;
        AccessibleAttribute jfxAttr = attr.jfxAttr;
        AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
        if (role == null) return null;
        if (jfxAttr == null) {
            switch (attr) {
                case NSAccessibilityValueAttribute: {
                    switch (role) {
                        case TAB_PANE:
                        case PAGINATION:
                            jfxAttr = FOCUS_ITEM;
                            map = MacVariant::createNSObject;
                            break;
                        case PAGE_ITEM:
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
                        case COMBO_BOX:
                            jfxAttr = TEXT;
                            map = MacVariant::createNSString;
                            break;
                        case SPINNER:
                            jfxAttr = VALUE_STRING;
                            map = MacVariant::createNSString;
                            break;
                        case CHECK_BOX:
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
                case NSAccessibilitySelectedChildrenAttribute: {
                    Node focus = null;
                    if (role == AccessibleRole.CONTEXT_MENU) {
                        /*
                         * ContextMenu is in its separate Window. It is safe to
                         * use FOCUS_NODE to locate the selected menu item
                         * within it.
                         */
                        Scene scene = (Scene)getAttribute(SCENE);
                        Accessible acc = getAccessible(scene);
                        if (acc != null) {
                            focus = (Node)acc.getAttribute(FOCUS_NODE);
                        }
                    }
                    if (role == AccessibleRole.MENU_BAR) {
                        /*
                         * MenuBar has special code to intercept the FOCUS_NODE
                         * (see Scene#transientFocusContainer).
                         */
                        focus = (Node)getAttribute(FOCUS_NODE);
                    }
                    if (focus != null) {
                        AccessibleRole focusRole = (AccessibleRole)getAccessible(focus).getAttribute(ROLE);
                        if (isMenuElement(focusRole)) {
                            long[] result = {getNativeAccessible(focus)};
                            return attr.map.apply(result);
                        }
                    }
                    return null;
                }
                case AXDateTimeComponents: {
                    /* Mac Yosemite crashes if AXDateTimeComponents set
                     * and NULL is returned for AXValue.
                     */
                    if (getAttribute(DATE) == null) return null;

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
                case NSAccessibilityColumnIndexRangeAttribute:
                    if (role == AccessibleRole.TABLE_COLUMN) {
                        /*
                         * Mac 10.10 sends AXColumnIndexRange instead of AXIndex for
                         * AXColumns. This is undocumented but without it the column
                         * header name is not read.
                         */
                        result = getAttribute(INDEX);
                        if (result != null) break;
                    }
                    return null;
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
                        case PAGE_ITEM: result = "page"; break;
                        case TAB_ITEM: result = "tab"; break;
                        case LIST_VIEW: result = "list"; break;
                        case DIALOG: result = "dialog"; break;
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
                if (isMenuElement(role)) {
                    return null;
                }
                Scene scene = (Scene)result;
                View view = getRootView(scene);
                if (view == null || view.getWindow() == null) return null;
                result = view.getWindow().getNativeWindow();
                break;
            }
            case NSAccessibilitySubroleAttribute: {
                MacSubrole subRole = MacSubrole.getRole((AccessibleRole)result);
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
            case NSAccessibilityTabsAttribute: {
                Integer count = (Integer)result;
                long[] tabs = new long[count];
                for (int i = 0; i < count; i++) {
                    Node child = (Node)getAttribute(ITEM_AT_INDEX, i);
                    tabs[i] = getNativeAccessible(child);
                }
                result = NSAccessibilityUnignoredChildren(tabs);
                break;
            }
            case NSAccessibilitySelectedCellsAttribute:
            case NSAccessibilitySelectedRowsAttribute:
            case NSAccessibilityVisibleChildrenAttribute:
            case NSAccessibilityChildrenAttribute: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> children = (ObservableList<Node>)result;
                result = getUnignoredChildren(children);
                break;
            }
            case NSAccessibilityParentAttribute: {
                if (getView() != null) {
                    if (getView().getWindow() == null) return null;
                    result = getView().getWindow().getNativeWindow();
                } else if (result != null) {
                    if (role == AccessibleRole.CONTEXT_MENU) {
                        Node menuItem = (Node)getAttribute(PARENT_MENU);
                        if (menuItem != null) {
                            if (getAccessible(menuItem).getAttribute(ROLE) == AccessibleRole.MENU) {
                                result = menuItem;
                            }
                        }
                    }
                    result = getNativeAccessible((Node)result);
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
                        result = getNativeAccessible((Node)result);
                        break;
                    case CHECK_BOX:
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
                    case COMBO_BOX:
                    case TEXT:
                    case TEXT_FIELD:
                    case TEXT_AREA:
                        return null;
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
                        int keyCode = code.getCode();
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
                result = text.length();
                break;
            }
            case NSAccessibilitySelectedTextAttribute: {
                int start = (Integer)result, end = -1;
                if (start != -1) {
                    result = getAttribute(SELECTION_END);
                    if (result == null) return null;
                    end = (Integer)result;
                }
                if (start < 0 || end < 0 || start > end) return null;
                String string = (String)getAttribute(TEXT);
                if (string == null) return null;
                if (end > string.length()) return null;
                result = string.substring(start, end);
                break;
            }
            case NSAccessibilitySelectedTextRangeAttribute: {
                int start = (Integer)result, end = -1;
                if (start != -1) {
                    result = getAttribute(SELECTION_END);
                    if (result == null) return null;
                    end = (Integer)result;
                }
                if (start < 0 || end < 0 || start > end) return null;
                String string = (String)getAttribute(TEXT);
                if (string == null) return null;
                if (end > string.length()) return null;
                result = new int[] {start, end - start};
                break;
            }
            case NSAccessibilityInsertionPointLineNumberAttribute: {
                if (role == AccessibleRole.TEXT_AREA) {
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
                    result = new long [] {getNativeAccessible((Node)result)};
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
                result = getNativeAccessible((Node)result);
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
        return result != null ? map.apply(result) : null;
    }

    private void accessibilitySetValue(long value, long attribute) {
        Application.checkEventThread();
        MacAttribute attr = MacAttribute.getAttribute(attribute);
        if (attr != null) {
            switch (attr) {
                case NSAccessibilityFocusedAttribute: {
                    MacVariant variant = idToMacVariant(value, MacVariant.NSNumber_Boolean);
                    if (variant != null && variant.int1 != 0) {
                        executeAction(AccessibleAction.REQUEST_FOCUS);
                    }
                    break;
                }
                case NSAccessibilityDisclosingAttribute: {
                    MacVariant variant = idToMacVariant(value, MacVariant.NSNumber_Boolean);
                    if (variant != null) {
                        if (variant.int1 != 0) {
                            executeAction(AccessibleAction.EXPAND);
                        } else {
                            executeAction(AccessibleAction.COLLAPSE);
                        }
                    }
                    break;
                }
                case NSAccessibilityExpandedAttribute:
                    if (getAttribute(ROLE) == AccessibleRole.COMBO_BOX) {
                        executeAction(AccessibleAction.EXPAND);
                    }
                    break;
                case NSAccessibilitySelectedCellsAttribute: {
                    /* Table and TreeTable */
                    MacVariant variant = idToMacVariant(value, MacVariant.NSArray_id);
                    if (variant != null && variant.longArray != null && variant.longArray.length > 0) {
                        long[] ids = variant.longArray;
                        ObservableList<Node> items = FXCollections.observableArrayList();
                        for (long id : ids) {
                            MacAccessible acc = GlassAccessibleToMacAccessible(id);
                            if (acc != null) {
                                Integer rowIndex = (Integer)acc.getAttribute(ROW_INDEX);
                                Integer columnIndex = (Integer)acc.getAttribute(COLUMN_INDEX);
                                if (rowIndex != null && columnIndex != null) {
                                    Node cell = (Node)getAttribute(CELL_AT_ROW_COLUMN, rowIndex, columnIndex);
                                    if (cell != null) {
                                        items.add(cell);
                                    }
                                }
                            }
                        }
                        executeAction(AccessibleAction.SET_SELECTED_ITEMS, items);
                    }
                    break;
                }
                case NSAccessibilitySelectedRowsAttribute: {
                    /* List and Tree */
                    MacVariant variant = idToMacVariant(value, MacVariant.NSArray_id);
                    if (variant != null && variant.longArray != null && variant.longArray.length > 0) {
                        long[] ids = variant.longArray;
                        ObservableList<Node> items = FXCollections.observableArrayList();
                        for (long id : ids) {
                            MacAccessible acc = GlassAccessibleToMacAccessible(id);
                            if (acc != null) {
                                Integer index = (Integer)acc.getAttribute(INDEX);
                                if (index != null) {
                                    Node cell = (Node)getAttribute(ROW_AT_INDEX, index);
                                    if (cell != null) {
                                        items.add(cell);
                                    }
                                }
                            }
                        }
                        executeAction(AccessibleAction.SET_SELECTED_ITEMS, items);
                    }
                    break;
                }
                case NSAccessibilitySelectedTextRangeAttribute: {
                    MacVariant variant = idToMacVariant(value, MacVariant.NSValue_range);
                    if (variant != null) {
                        int start = variant.int1; /* range.location */
                        int end = variant.int1 + variant.int2; /* range.location + range.length */
                        executeAction(AccessibleAction.SET_TEXT_SELECTION, start, end);
                    }
                    break;
                }
                default:
            }
        }
    }

    private long accessibilityIndexOfChild(long child) {
        /* Forward to native code */
        return -1;
    }

    private long[] accessibilityParameterizedAttributeNames() {
        Application.checkEventThread();
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
        if (role != null) {
            List<MacAttribute> attrs = new ArrayList<>();
            MacRole macRole = getRole(role);
            if (macRole != null && macRole.macParameterizedAttributes != null) {
                attrs.addAll(macRole.macParameterizedAttributes);
            }
            switch (role) {
                case LIST_VIEW:
                case TREE_VIEW:
                    /* Row-based control, must remove all the cell-based attributes */
                    attrs.remove(MacAttribute.NSAccessibilityCellForColumnAndRowParameterizedAttribute);
                    break;
                case TEXT:
                case TEXT_FIELD:
                case TEXT_AREA:
                case PASSWORD_FIELD:
                case COMBO_BOX:
                    attrs.addAll(TEXT_PARAMETRIZED_ATTRIBUTES);
                    break;
                default:
            }
            return attrs.stream().mapToLong(a -> a.ptr).toArray();
        }
        return null;
    }

    private MacVariant accessibilityAttributeValueForParameter(long attribute, long parameter) {
        Application.checkEventThread();
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
                if (getAttribute(ROLE) == AccessibleRole.TEXT_AREA) {
                    result = getAttribute(attr.jfxAttr, value /*charOffset*/);
                } else {
                    /* Combo and TextField */
                    result = 0;
                }
                break;
            }
            case NSAccessibilityRangeForLineParameterizedAttribute: {
                if (getAttribute(ROLE) == AccessibleRole.TEXT_AREA) {
                    Integer lineStart = (Integer)getAttribute(LINE_START, value /*line index*/);
                    Integer lineEnd = (Integer)getAttribute(LINE_END, value /*line index*/);
                    if (lineStart != null && lineEnd != null) {
                        result = new int[] {lineStart, lineEnd - lineStart};
                    } else {
                        result = null;
                    }
                } else {
                    /* Combo and TextField */
                    String text = (String)getAttribute(TEXT);
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
                result = getNativeAccessible((Node)result);
                break;
            }
            default:
        }
        return attr.map.apply(result);
    }

    private long[] accessibilityActionNames() {
        Application.checkEventThread();
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
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
            if (role != AccessibleRole.NODE && role != AccessibleRole.PARENT) {
                actions.add(MacAction.NSAccessibilityShowMenuAction);
            }
        }
        /* Return empty array instead of null to prevent warnings in the accessibility verifier */
        return actions.stream().mapToLong(a -> a.ptr).toArray();
    }

    private String accessibilityActionDescription(long action) {
        return NSAccessibilityActionDescription(action);
    }

    private void accessibilityPerformAction(long action) {
        MacAction macAction = MacAction.getAction(action);
        boolean expand = false;
        if (macAction == MacAction.NSAccessibilityPressAction) {
            AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
            if (role == AccessibleRole.TITLED_PANE || role == AccessibleRole.COMBO_BOX) {
                expand = true;
            }
        }
        if (macAction == MacAction.NSAccessibilityShowMenuAction) {
            if (getAttribute(ROLE) == AccessibleRole.SPLIT_MENU_BUTTON) {
                expand = true;
            }
        }
        if (expand) {
            if (Boolean.TRUE.equals(getAttribute(EXPANDED))) {
                executeAction(AccessibleAction.COLLAPSE);
            } else {
                executeAction(AccessibleAction.EXPAND);
            }
            return;
        }
        if (macAction != null && macAction.jfxAction != null) {
            executeAction(macAction.jfxAction);
        }
    }

    private long accessibilityFocusedUIElement() {
        Application.checkEventThread();
        Node node = (Node)getAttribute(FOCUS_NODE);
        if (node == null) return 0L;

        Node item = (Node)getAccessible(node).getAttribute(FOCUS_ITEM);
        if (item != null) return getNativeAccessible(item);
        return getNativeAccessible(node);
    }

    private boolean accessibilityIsIgnored() {
        Application.checkEventThread();
        if (isIgnored()) return true;
        if (isInSlider()) {
            /*
             * Ignoring the children within the slider, otherwise VoiceOver
             * reports 'multiple indicator slider' instead of the value.
             */
            return true;
        }
        if (isInMenu()) {
            AccessibleRole role = (AccessibleRole)getAttribute(ROLE);
            /* Within a ContextMenu or MenuBar ignore all nodes but menu item roles */
            return !isMenuElement(role);
        }
        if (ignoreInnerText()) {
            return true;
        }
        return false;
    }

    private long accessibilityHitTest(float x, float y) {
        Application.checkEventThread();
        View view = getView();
        if (view == null || view.getWindow() == null) {
            return 0L;
        }
        Screen screen = view.getWindow().getScreen();
        y = screen.getHeight() - y;
        Node node = (Node)getAttribute(NODE_AT_POINT, new Point2D(x, y));
        return NSAccessibilityUnignoredAncestor(getNativeAccessible(node));
  }

}
