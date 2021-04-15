package com.sun.javafx.scene;

public enum InvalidateLayoutOption {
    /**
     * The current node will be scheduled for layout.
     */
    LOCAL_LAYOUT,

    /**
     * The parent node will be scheduled for layout, except if the parent node is currently
     * performing layout. In this case, no further layout cycle will be scheduled.
     */
    PARENT_LAYOUT,

    /**
     * The parent node will be scheduled for layout. If the parent node is currently
     * performing layout, a new layout cycle will be scheduled.
     */
    FORCE_PARENT_LAYOUT,

    /**
     * All parent nodes (up to the layout root) will be scheduled for layout.
     * If a parent node is currently performing layout, a new layout cycle will be scheduled.
     */
    FORCE_ROOT_LAYOUT
}
