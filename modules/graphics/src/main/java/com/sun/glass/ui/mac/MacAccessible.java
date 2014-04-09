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
        if (!_initEnum("MacAttributes")) {
            System.err.println("Fail linking MacAttributes");
        }
        if (!_initEnum("MacActions")) {
            System.err.println("Fail linking MacActions");
        }
        if (!_initEnum("MacRoles")) {
            System.err.println("Fail linking MacRoles");
        }
        if (!_initEnum("MacSubroles")) {
            System.err.println("Fail linking MacSubroles");
        }
        if (!_initEnum("MacNotifications")) {
            System.err.println("Fail linking MacNotifications");
        }
        if (!_initEnum("MacOrientations")) {
            System.err.println("Fail linking MacOrientations");
        }
    }

    enum MacAttributes {
        // Dynamic mapping to FX attribute, dynamic return type
        NSAccessibilityValueAttribute(null, null),

        // 1-to-1 mapping between FX attribute and Mac attribute, static return type
        NSAccessibilityChildrenAttribute(CHILDREN, MacVariant::createNSArray),
        NSAccessibilityDescriptionAttribute(DESCRIPTION, MacVariant::createNSString),
        NSAccessibilityEnabledAttribute(ENABLED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityHelpAttribute(TOOLTIP, MacVariant::createNSString),

        /* FOCUSED might not match the result of accessibilityFocusedUIElement() cause of FOCUS_ITEM */
        NSAccessibilityFocusedAttribute(FOCUSED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityExpandedAttribute(EXPANDED, MacVariant::createNSNumberForBoolean),
        NSAccessibilityMaxValueAttribute(MAX_VALUE, MacVariant::createNSNumberForDouble),
        NSAccessibilityMinValueAttribute(MIN_VALUE, MacVariant::createNSNumberForDouble),
        NSAccessibilityParentAttribute(PARENT, MacVariant::createNSObject),
        NSAccessibilityPositionAttribute(BOUNDS, MacVariant::createNSValueForPoint),
        NSAccessibilityRoleAttribute(ROLE, MacVariant::createNSObject),
        NSAccessibilitySubroleAttribute(ROLE, MacVariant::createNSObject),
        NSAccessibilityRoleDescriptionAttribute(ROLE, MacVariant::createNSString),
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
        NSAccessibilityInsertionPointLineNumberAttribute(SELECTION_START, MacVariant::createNSNumberForInt),
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

        /* Parameterized Attributes */
        NSAccessibilityLineForIndexParameterizedAttribute(SELECTION_START, MacVariant::createNSNumberForInt, MacVariant.NSNumber_Int),
        NSAccessibilityStringForRangeParameterizedAttribute(TITLE, MacVariant::createNSString, MacVariant.NSValue_range),
        NSAccessibilityRangeForLineParameterizedAttribute(TITLE, MacVariant::createNSValueForRange, MacVariant.NSNumber_Int),
        NSAccessibilityAttributedStringForRangeParameterizedAttribute(TITLE, MacVariant::createNSAttributedString, MacVariant.NSValue_range),
        NSAccessibilityCellForColumnAndRowParameterizedAttribute(CELL_AT_ROW_COLUMN, MacVariant::createNSObject, MacVariant.NSArray_int),
        ;

        long ptr; /* Initialized natively - treat as final */
        Attribute jfxAttr;
        Function<Object, MacVariant> map; /* Maps the object returned by JavaFX to the appropriate MacVariant */
        int inputType; /* Defined only for parameterized attributes to convert the native input parameter (id) to MacVariant */

        MacAttributes(Attribute jfxAttr, Function<Object, MacVariant> map, int inputType) {
            this.jfxAttr = jfxAttr;
            this.map = map;
            this.inputType = inputType;
        }

        MacAttributes(Attribute jfxAttr, Function<Object, MacVariant> map) {
            this.jfxAttr = jfxAttr;
            this.map = map;
        }

        static MacAttributes getAttribute(long ptr) {
            if (ptr == 0) return null;
            for (MacAttributes attr : values()) {
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
    enum MacRoles {
        NSAccessibilityUnknownRole(Role.NODE, null, null),
        NSAccessibilityGroupRole(Role.PARENT, null, null),
        NSAccessibilityButtonRole(new Role[] {Role.BUTTON, Role.INCREMENT_BUTTON, Role.DECREMENT_BUTTON, Role.HEADER, Role.SPLIT_MENU_BUTTON},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityTitleAttribute,
            },
            new MacActions[] {MacActions.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityImageRole(Role.IMAGE, null, null),
        NSAccessibilityRadioButtonRole(new Role[] {Role.RADIO_BUTTON, Role.TAB_ITEM, Role.PAGE},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityTitleAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
            },
            new MacActions[] {MacActions.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityCheckBoxRole(new Role[] {Role.CHECKBOX, Role.TOGGLE_BUTTON},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityTitleAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
            },
            new MacActions[] {MacActions.NSAccessibilityPressAction},
            null
        ),

        NSAccessibilityPopUpButtonRole(new Role[] {Role.COMBOBOX},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
                /* Expanded only needed for Combobox, and not for PopUpButton */
//                MacAttributes.NSAccessibilityExpandedAttribute,
            },
            new MacActions[] {MacActions.NSAccessibilityPressAction},
            null
        ),
        NSAccessibilityTabGroupRole(new Role[] {Role.TAB_PANE, Role.PAGINATION},
            new MacAttributes[] {
//              MacAttributes.NSAccessibilityContentsAttribute,
                MacAttributes.NSAccessibilityTabsAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
            },
            null,
            null
        ),
        NSAccessibilityProgressIndicatorRole(Role.PROGRESS_INDICATOR,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityOrientationAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityMaxValueAttribute,
                MacAttributes.NSAccessibilityMinValueAttribute,
            },
            null
        ),
        NSAccessibilityMenuRole(Role.CONTEXT_MENU,
            new MacAttributes[] {
                MacAttributes.NSAccessibilitySelectedChildrenAttribute,
                MacAttributes.NSAccessibilityEnabledAttribute,
            },
            new MacActions[] {
                MacActions.NSAccessibilityPressAction,
                MacActions.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuItemRole(Role.MENU_ITEM,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityTitleAttribute,
                MacAttributes.NSAccessibilitySelectedAttribute,
                MacAttributes.AXMenuItemCmdChar,
                MacAttributes.AXMenuItemCmdVirtualKey,
                MacAttributes.AXMenuItemCmdGlyph,
                MacAttributes.AXMenuItemCmdModifiers,
                MacAttributes.AXMenuItemMarkChar,
            },
            new MacActions[] {
                MacActions.NSAccessibilityPressAction,
                MacActions.NSAccessibilityCancelAction,
            }
        ),
        NSAccessibilityMenuButtonRole(Role.MENU_BUTTON,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityTitleAttribute,
            },
            new MacActions[] {
                MacActions.NSAccessibilityPressAction,
            }
        ),
        /* 
         * ProgressIndicator can be either a ProgressIndicatorRole or a BusyIndicatorRole.
         * Depending on the state of the indeterminate property.
         * Only in NSAccessibilityRoleAttribute and NSAccessibilityRoleDescriptionAttribute
         * the correct adjustments are made, on all other method BusyIndicatorRole reply 
         * as a ProgressIndicatorRole.
         */
        NSAccessibilityBusyIndicatorRole(Role.PROGRESS_INDICATOR, null, null),
        NSAccessibilityStaticTextRole(new Role[] {Role.TEXT, Role.TREE_TABLE_CELL},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityNumberOfCharactersAttribute,
                MacAttributes.NSAccessibilitySelectedTextAttribute,
                MacAttributes.NSAccessibilitySelectedTextRangeAttribute,
                MacAttributes.NSAccessibilityInsertionPointLineNumberAttribute,
                MacAttributes.NSAccessibilityVisibleCharacterRangeAttribute,
            },
            null,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityLineForIndexParameterizedAttribute,
                MacAttributes.NSAccessibilityRangeForLineParameterizedAttribute,
                MacAttributes.NSAccessibilityAttributedStringForRangeParameterizedAttribute,
                MacAttributes.NSAccessibilityStringForRangeParameterizedAttribute,
            }
        ),
        NSAccessibilityTextFieldRole(new Role[] {Role.TEXT_FIELD, Role.PASSWORD_FIELD},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityNumberOfCharactersAttribute,
                MacAttributes.NSAccessibilitySelectedTextAttribute,
                MacAttributes.NSAccessibilitySelectedTextRangeAttribute,
                MacAttributes.NSAccessibilityInsertionPointLineNumberAttribute,
                MacAttributes.NSAccessibilityVisibleCharacterRangeAttribute,
            },
            null,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityLineForIndexParameterizedAttribute,
                MacAttributes.NSAccessibilityRangeForLineParameterizedAttribute,
                MacAttributes.NSAccessibilityAttributedStringForRangeParameterizedAttribute,
                MacAttributes.NSAccessibilityStringForRangeParameterizedAttribute,
            }
        ),
        NSAccessibilityTextAreaRole(Role.TEXT_AREA,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityNumberOfCharactersAttribute,
                MacAttributes.NSAccessibilitySelectedTextAttribute,
                MacAttributes.NSAccessibilitySelectedTextRangeAttribute,
            },
            null
        ),
        NSAccessibilitySliderRole(Role.SLIDER,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityOrientationAttribute,
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityMaxValueAttribute,
                MacAttributes.NSAccessibilityMinValueAttribute,
            },
            new MacActions[] {
                MacActions.NSAccessibilityDecrementAction,
                MacActions.NSAccessibilityIncrementAction,
            }
        ),
        NSAccessibilityScrollAreaRole(Role.SCROLL_PANE,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityContentsAttribute,
                MacAttributes.NSAccessibilityHorizontalScrollBarAttribute,
                MacAttributes.NSAccessibilityVerticalScrollBarAttribute,
            },
            null
        ),
        NSAccessibilityScrollBarRole(Role.SCROLL_BAR,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityValueAttribute,
                MacAttributes.NSAccessibilityMinValueAttribute,
                MacAttributes.NSAccessibilityMaxValueAttribute,
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityOrientationAttribute,
            },
            null
        ),
        NSAccessibilityValueIndicatorRole(Role.THUMB,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityValueAttribute,
            },
            null
        ),
        NSAccessibilityRowRole(new Role[] {Role.LIST_ITEM, Role.TABLE_ROW, Role.TREE_ITEM, Role.TREE_TABLE_ITEM},
            new MacAttributes[] {
                MacAttributes.NSAccessibilitySubroleAttribute,
                MacAttributes.NSAccessibilityIndexAttribute,
                MacAttributes.NSAccessibilitySelectedAttribute,
                MacAttributes.NSAccessibilityVisibleChildrenAttribute,
            },
            null, null
        ),
        NSAccessibilityTableRole(new Role[] {Role.LIST_VIEW, Role.TABLE_VIEW},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityColumnsAttribute,
                MacAttributes.NSAccessibilityHeaderAttribute,
                MacAttributes.NSAccessibilityRowsAttribute,
                MacAttributes.NSAccessibilitySelectedRowsAttribute,
                MacAttributes.NSAccessibilityRowCountAttribute,
                MacAttributes.NSAccessibilityColumnCountAttribute,
                MacAttributes.NSAccessibilitySelectedCellsAttribute,
            },
            null,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityCellForColumnAndRowParameterizedAttribute,
            }
        ),
        NSAccessibilityColumnRole(Role.TABLE_COLUMN,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityHeaderAttribute,
                MacAttributes.NSAccessibilityIndexAttribute,
                MacAttributes.NSAccessibilityRowsAttribute,
                MacAttributes.NSAccessibilitySelectedAttribute,
            },
            null
        ),
        NSAccessibilityCellRole(new Role[] {Role.TABLE_CELL},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityColumnIndexRangeAttribute,
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityRowIndexRangeAttribute,
                MacAttributes.NSAccessibilitySelectedAttribute,
            },
            null,
            null
        ),
        NSAccessibilityLinkRole(Role.HYPERLINK,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.AXVisited
            },
            null
        ),
        NSAccessibilityOutlineRole(new Role[] {Role.TREE_VIEW, Role.TREE_TABLE_VIEW},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityColumnsAttribute,
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityHeaderAttribute,
                MacAttributes.NSAccessibilityRowsAttribute,
                MacAttributes.NSAccessibilitySelectedRowsAttribute,
            },
            null,
            null
        ),
        NSAccessibilityDisclosureTriangleRole(new Role[] {Role.DISCLOSURE_NODE, Role.TITLED_PANE},
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityValueAttribute
            },
            new MacActions[] {
                MacActions.NSAccessibilityPressAction
            },
            null
        ),
        NSAccessibilityToolbarRole(Role.TOOLBAR,
            new MacAttributes[] {
                MacAttributes.NSAccessibilityEnabledAttribute,
                MacAttributes.NSAccessibilityOverflowButtonAttribute,
            },
            null
        ),
        AXDateTimeArea(Role.DATE_PICKER,
                new MacAttributes[] {
                    MacAttributes.NSAccessibilityEnabledAttribute,
                    MacAttributes.NSAccessibilityValueAttribute,
                    MacAttributes.AXDateTimeComponents,
                },
                null
            ),
        ;

        long ptr; /* Initialized natively - treat as final */
        Role[] jfxRoles;
        List<MacAttributes> macAttributes;
        List<MacAttributes> macParameterizedAttributes;
        List<MacActions> macActions;
        MacRoles(Role jfxRole, MacAttributes[] macAttributes, MacActions[] macActions) {
            this(new Role[] {jfxRole}, macAttributes, macActions, null);
        }

        MacRoles(Role[] jfxRoles, MacAttributes[] macAttributes, MacActions[] macActions, MacAttributes[] macParameterizedAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
            this.macActions = macActions != null ? Arrays.asList(macActions) : null;
            this.macParameterizedAttributes = macParameterizedAttributes != null ? Arrays.asList(macParameterizedAttributes) : null;
        }

        static MacRoles getRole(Role targetRole) {
            if (targetRole == null) return null;
            for (MacRoles macRole : values()) {
                for (Role jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    enum MacSubroles {
        NSAccessibilityTableRowSubrole(Role.LIST_ITEM, Role.TABLE_ROW),
        NSAccessibilitySortButtonSubrole(Role.HEADER),
        NSAccessibilitySecureTextFieldSubrole(Role.PASSWORD_FIELD),
        NSAccessibilityOutlineRowSubrole(new Role[] { Role.TREE_ITEM, Role.TREE_TABLE_ITEM },
            new MacAttributes[] {
                MacAttributes.NSAccessibilityDisclosedByRowAttribute,
                MacAttributes.NSAccessibilityDisclosedRowsAttribute,
                MacAttributes.NSAccessibilityDisclosingAttribute,
                MacAttributes.NSAccessibilityDisclosureLevelAttribute
            }
        ),
        NSAccessibilityDecrementArrowSubrole(new Role[] { Role.DECREMENT_BUTTON },
            new MacAttributes[] {
                MacAttributes.NSAccessibilitySubroleAttribute
            }
        ),
        NSAccessibilityIncrementArrowSubrole(new Role[] { Role.INCREMENT_BUTTON },
            new MacAttributes[] {
                MacAttributes.NSAccessibilitySubroleAttribute
            }
        )
        ;

        long ptr; /* Initialized natively - treat as final */
        Role[] jfxRoles;
        List<MacAttributes> macAttributes;

        MacSubroles(Role... jfxRoles) {
            this(jfxRoles, null);
        }

        MacSubroles(Role[] jfxRoles, MacAttributes[] macAttributes) {
            this.jfxRoles = jfxRoles;
            this.macAttributes = macAttributes != null ? Arrays.asList(macAttributes) : null;
        }

        static MacSubroles getRole(Role targetRole) {
            if (targetRole == null) return null;
            for (MacSubroles macRole : values()) {
                for (Role jfxRole : macRole.jfxRoles) {
                    if (jfxRole == targetRole) {
                        return macRole;
                    }
                }
            }
            return null;
        }
    }

    enum MacActions {
        NSAccessibilityCancelAction,
        NSAccessibilityConfirmAction,
        NSAccessibilityDecrementAction(Action.DECREMENT),
        NSAccessibilityDeleteAction,
        NSAccessibilityIncrementAction(Action.INCREMENT),
        NSAccessibilityPickAction,
        NSAccessibilityPressAction(Action.FIRE),
        NSAccessibilityRaiseAction,
        NSAccessibilityShowMenuAction(Action.SHOW_MENU);

        long ptr; /* Initialized natively - treat as final */
        Action jfxAction;
        MacActions() {}
        MacActions(Action jfxAction) {
            this.jfxAction = jfxAction;
        }

        static MacActions getAction(long ptr) {
            for (MacActions macAction : MacActions.values()) {
                if (macAction.ptr == ptr || isEqualToString(macAction.ptr, ptr)) {
                    return macAction;
                }
            }
            return null;
        }
    }

    enum MacNotifications {
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
        ;long ptr;
    }

    enum MacOrientations {
        NSAccessibilityHorizontalOrientationValue,
        NSAccessibilityVerticalOrientationValue,
        NSAccessibilityUnknownOrientationValue;
        long ptr;
    }

    List<MacAttributes> baseAttributes = Arrays.asList(
        MacAttributes.NSAccessibilityRoleAttribute,
        MacAttributes.NSAccessibilityRoleDescriptionAttribute,
        MacAttributes.NSAccessibilityHelpAttribute,
        MacAttributes.NSAccessibilityFocusedAttribute,
        MacAttributes.NSAccessibilityParentAttribute,
        MacAttributes.NSAccessibilityChildrenAttribute,
        MacAttributes.NSAccessibilityPositionAttribute,
        MacAttributes.NSAccessibilitySizeAttribute,
        MacAttributes.NSAccessibilityWindowAttribute,
        MacAttributes.NSAccessibilityTopLevelUIElementAttribute,
        MacAttributes.NSAccessibilityTitleUIElementAttribute
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
                NSAccessibilityPostNotification(peer, MacNotifications.NSAccessibilityUIElementDestroyedNotification.ptr);
            }
            _destroyGlassAccessible(peer);
            peer = 0L;
        }
        super.dispose();
    }

    @Override
    public void sendNotification(Attribute notification) {
        if (isDisposed()) return;

        MacNotifications macNotification = null;
        switch (notification) {
            case SELECTED_TAB:
            case SELECTED_PAGE: {
                View view = getRootView((Scene)getAttribute(SCENE));
                if (view != null) {
                    long id = view.getNativeView();
                    NSAccessibilityPostNotification(id, MacNotifications.NSAccessibilityFocusedUIElementChangedNotification.ptr);
                }
                return;
            }
            case SELECTED_ROWS:
                macNotification = MacNotifications.NSAccessibilitySelectedRowsChangedNotification;
                break;
            case SELECTED_CELLS:
                macNotification = MacNotifications.NSAccessibilitySelectedCellsChangedNotification;
                break;
            case FOCUS_NODE:
                macNotification = MacNotifications.NSAccessibilityFocusedUIElementChangedNotification;
                break;
            case FOCUSED:
                return;
            case SELECTION_START:
            case SELECTION_END:
                macNotification = MacNotifications.NSAccessibilitySelectedTextChangedNotification;
                break;
            case EXPANDED:
                boolean expanded = Boolean.TRUE.equals(getAttribute(EXPANDED));
                if (expanded) {
                    macNotification = MacNotifications.NSAccessibilityRowExpandedNotification;
                } else {
                    macNotification = MacNotifications.NSAccessibilityRowCollapsedNotification;
                }

                Role role = (Role) getAttribute(ROLE);
                if (role == Role.TREE_ITEM || role == Role.TREE_TABLE_ITEM) {
                    Role container = role == Role.TREE_ITEM ? Role.TREE_VIEW : Role.TREE_TABLE_VIEW;
                    long control = getAccessible(getContainerNode(container));
                    if (control != 0) {
                        NSAccessibilityPostNotification(control, MacNotifications.NSAccessibilityRowCountChangedNotification.ptr);
                    }
                }
                break;
            case VISIBLE: {
                if (getAttribute(ROLE) == Role.CONTEXT_MENU) {
                    Boolean visible = (Boolean)getAttribute(VISIBLE);
                    if (Boolean.TRUE.equals(visible)) {
                        macNotification = MacNotifications.AXMenuOpened;
                    } else {
                        macNotification = MacNotifications.AXMenuClosed;

                        /* When a submenu closes the focus is returned to the main
                         * window, as opposite of the previous menu.
                         * The work around is to look for a previous menu
                         * and send a close and open event for it.
                         * */
                        Node menuItemOwner = (Node)getAttribute(MENU_FOR);
                        long menu = getAccessible(getContainerNode(menuItemOwner, Role.CONTEXT_MENU));
                        if (menu != 0) {
                            NSAccessibilityPostNotification(menu, MacNotifications.AXMenuClosed.ptr);
                            NSAccessibilityPostNotification(menu, MacNotifications.AXMenuOpened.ptr);
                        }
                    }
                }
                break;
            }
            default:
                macNotification = MacNotifications.NSAccessibilityValueChangedNotification;
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
        if (inMenu == null) {
            inMenu = getContainerNode(Role.CONTEXT_MENU) != null;
        }
        return inMenu;
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

    /* NSAccessibility Protocol - JNI entry points */
    long[] accessibilityAttributeNames() {
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        Role role = (Role)getAttribute(ROLE);
        if (role != null) {
            List<MacAttributes> attrs = new ArrayList<>(baseAttributes);
            MacRoles macRole = MacRoles.getRole(role);
            if (macRole != null && macRole.macAttributes != null) {
                attrs.addAll(macRole.macAttributes);
            }

            /* Look to see if there is a subrole that we should also get the attributes of */
            MacSubroles macSubrole = MacSubroles.getRole(role);
            if (macSubrole != null && macSubrole.macAttributes != null) {
                attrs.addAll(macSubrole.macAttributes);
            }

            /* ListView is row-based, must remove all the cell-based attributes */
            if (role == Role.LIST_VIEW || role == Role.TREE_TABLE_VIEW) {
                attrs.remove(MacAttributes.NSAccessibilitySelectedCellsAttribute);
            }

            /* Menu and MenuItem do have have Window and top-level UI Element*/
            if (role == Role.CONTEXT_MENU || role == Role.MENU_ITEM) {
                attrs.remove(MacAttributes.NSAccessibilityWindowAttribute);
                attrs.remove(MacAttributes.NSAccessibilityTopLevelUIElementAttribute);
            }

            return attrs.stream().mapToLong(a -> a.ptr).toArray();
        }
        return null;
    }

    int accessibilityArrayAttributeCount(long attribute) {
        MacAttributes attr = MacAttributes.getAttribute(attribute);
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
        MacAttributes attr = MacAttributes.getAttribute(attribute);
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
        MacAttributes attr = MacAttributes.getAttribute(attribute);
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
            default:
        }
        return false;
    }

    MacVariant accessibilityAttributeValue(long attribute) {
        MacAttributes attr = MacAttributes.getAttribute(attribute);
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
                default: return null;
            }
        }

        /* Some Attributes need to be modified before creating the MacVariant */
        switch (attr) {
            case NSAccessibilityWindowAttribute:
            case NSAccessibilityTopLevelUIElementAttribute: {
                if (role == Role.CONTEXT_MENU || role == Role.MENU_ITEM) {
                    return null;
                }
                Scene scene = (Scene)result;
                View view = getRootView(scene);
                if (view == null) return null;
                result = view.getWindow().getNativeWindow();
                break;
            }
            case NSAccessibilitySubroleAttribute: {
                MacSubroles subRole = MacSubroles.getRole((Role)result);
                result = subRole != null ? subRole.ptr : 0L;
                break;
            }
            case NSAccessibilityRoleAttribute: {
                MacRoles macRole = MacRoles.getRole((Role)result);
                if (macRole == MacRoles.NSAccessibilityProgressIndicatorRole) {
                    Boolean state = (Boolean)getAttribute(INDETERMINATE);
                    if (Boolean.TRUE.equals(state)) {
                        macRole = MacRoles.NSAccessibilityBusyIndicatorRole;
                    }
                }
                result = macRole != null ? macRole.ptr : 0L;
                break;
            }
            case NSAccessibilityRoleDescriptionAttribute: {
                MacRoles macRole = MacRoles.getRole((Role)result);
                if (macRole == null) return null;
                if (macRole == MacRoles.NSAccessibilityProgressIndicatorRole) {
                    Boolean state = (Boolean)getAttribute(INDETERMINATE);
                    if (Boolean.TRUE.equals(state)) {
                        macRole = MacRoles.NSAccessibilityBusyIndicatorRole;
                    }
                }
                /* 
                 * In some cases there is no proper mapping from a JFX role
                 * to a Mac role. For example, reporting 'disclosure triangle'
                 * for a TITLED_PANE is not appropriate.
                 * Providing a custom role description makes it much better.
                 */
                switch (role) {
                    case TITLED_PANE: result = "title pane"; break;
                    case SPLIT_MENU_BUTTON: result = "split button"; break;
                    default:
                        MacSubroles subRole = MacSubroles.getRole(role);
                        result = NSAccessibilityRoleDescription(macRole.ptr, subRole != null ? subRole.ptr : 0l);
                }
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
                View view = getRootView((Scene)getAttribute(SCENE));
                if (view != null) {
                    Screen screen = view.getWindow().getScreen();
                    float height = screen.getHeight();
                    Bounds bounds = (Bounds)result;
                    result = new BoundingBox(bounds.getMinX(),
                                             height - bounds.getMinY() - bounds.getHeight(),
                                             bounds.getWidth(),
                                             bounds.getHeight());
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
                    case TEXT:
                    case TEXT_FIELD:
                    case TEXT_AREA: return null;
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
                int offset = (Integer)result;
                if (offset < 0) result = 0;
                //TODO multi line support
                result = 0;
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
                    case HORIZONTAL: result = MacOrientations.NSAccessibilityHorizontalOrientationValue.ptr; break;
                    case VERTICAL: result = MacOrientations.NSAccessibilityVerticalOrientationValue.ptr; break;
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
        MacAttributes attr = MacAttributes.getAttribute(attribute);
        if (attr != null) {
            switch (attr) {
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
                default:
            }
        }
    }

    long accessibilityIndexOfChild(long child) {
        //TODO this method might not be necessary
        ObservableList<Node> children = (ObservableList<Node>)getAttribute(CHILDREN);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Node node = children.get(i);
                if (child == getAccessible(node)) {
                    return i;
                }
            }
        }
        return -1;
    }

    long[] accessibilityParameterizedAttributeNames() {
        if (getView() != null) return null; /* Let NSView answer for the Scene */
        Role role = (Role)getAttribute(ROLE);
        if (role != null) {
            MacRoles macRole = MacRoles.getRole(role);
            if (macRole != null && macRole.macParameterizedAttributes != null) {
                Stream<MacAttributes> attrs = macRole.macParameterizedAttributes.stream();

                /* ListView is row-based, must remove all the cell-based attributes */
                if (role == Role.LIST_VIEW || role == Role.TREE_TABLE_VIEW) {
                    attrs = attrs.filter(a -> a != MacAttributes.NSAccessibilityCellForColumnAndRowParameterizedAttribute);
                }
                return attrs.mapToLong(a -> a.ptr).toArray();
            }
        }
        return null;
    }

    MacVariant accessibilityAttributeValueForParameter(long attribute, long parameter) {
        MacAttributes attr = MacAttributes.getAttribute(attribute);
        if (attr == null || attr.inputType == 0 || attr.jfxAttr == null) {
            return null;
        }
        MacVariant variant = idToMacVariant(parameter, attr.inputType);
        if (variant == null) return null;
        Object value = variant.getValue();
        Object result;
        switch (attr) {
            case NSAccessibilityCellForColumnAndRowParameterizedAttribute:
                int[] intArray = (int[])value;
                result = getAttribute(attr.jfxAttr, intArray[1] /*row*/, intArray[0] /*column*/);
                break;
            default:
                result = getAttribute(attr.jfxAttr, value);
        }
        if (result == null) return null;
        switch (attr) {
            case NSAccessibilityAttributedStringForRangeParameterizedAttribute:
            case NSAccessibilityStringForRangeParameterizedAttribute: {
                String text = (String)result;
                result = text.substring(variant.int1, variant.int1 + variant.int2);
                break;
            }
            case NSAccessibilityLineForIndexParameterizedAttribute: {
                int offset = (Integer)result;
                //TODO multi line support
                if (offset < 0) result = 0;
                result = 0;
                break;
            }
            case NSAccessibilityRangeForLineParameterizedAttribute: {
                String text = (String)result;
                //TODO multi line support
                result = new int[] {0, text.length()}; 
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
        List<MacActions> actions = new ArrayList<>();
        if (role != null) {
            MacRoles macRole = MacRoles.getRole(role);
            if (macRole != null && macRole.macActions != null) {
                actions.addAll(macRole.macActions);
            }
            /* 
             * Consider add a attribute to indicate when the node
             * has a menu instead of using the role.
             */
            if (role != Role.NODE && role != Role.PARENT) {
                actions.add(MacActions.NSAccessibilityShowMenuAction);
            }
        }
        /* Return empty array instead of null to prevent warnings in the accessibility verifier */
        return actions.stream().mapToLong(a -> a.ptr).toArray();
    }

    String accessibilityActionDescription(long action) {
        return NSAccessibilityActionDescription(action);
    }

    void accessibilityPerformAction(long action) {
        MacActions macAction = MacActions.getAction(action);
        if (macAction == MacActions.NSAccessibilityPressAction) {
            if (getAttribute(ROLE) == Role.TITLED_PANE) {
                if (Boolean.TRUE.equals(getAttribute(EXPANDED))) {
                    executeAction(Action.COLLAPSE);
                } else {
                    executeAction(Action.EXPAND);
                }
                return;
            }
        }
        if (macAction == MacActions.NSAccessibilityShowMenuAction) {
            if (getAttribute(ROLE) == Role.SPLIT_MENU_BUTTON) {
                /* Note, it is not expected a split menu button 
                 * to have a context menu
                 */
                if (Boolean.TRUE.equals(getAttribute(EXPANDED))) {
                    executeAction(Action.COLLAPSE);
                } else {
                    executeAction(Action.EXPAND);
                }
                return;
            }
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
        if (isInMenu()) {
            Role role = (Role)getAttribute(ROLE);
            return role != Role.CONTEXT_MENU && role != Role.MENU_ITEM;
        } else {
            return isIgnored();
        }
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
