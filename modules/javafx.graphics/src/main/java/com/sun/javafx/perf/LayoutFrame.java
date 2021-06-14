package com.sun.javafx.perf;

import javafx.scene.Parent;
import java.util.ArrayList;
import java.util.List;

public class LayoutFrame {

    private final Parent node;
    private final boolean layoutRoot;
    private final List<LayoutFrame> children;
    private int passes;

    LayoutFrame(Parent node, boolean layoutRoot) {
        this.node = node;
        this.layoutRoot = layoutRoot;
        this.children = new ArrayList<>();
    }

    public Parent getNode() {
        return node;
    }

    public boolean isLayoutRoot() {
        return layoutRoot;
    }

    public List<LayoutFrame> getChildren() {
        return children;
    }

    public int getPasses() {
        return passes;
    }

    public int getCumulativePasses() {
        int total = passes;
        for (LayoutFrame child : children) {
            total += child.getCumulativePasses();
        }

        return total;
    }

    LayoutFrame getFrame(Parent node) {
        for (int i = 0, size = children.size(); i < size; ++i) {
            LayoutFrame frame = children.get(i);
            if (frame.node == node) {
                return frame;
            }
        }

        return null;
    }

    void updatePasses(int passes) {
        this.passes += passes;
    }

}
