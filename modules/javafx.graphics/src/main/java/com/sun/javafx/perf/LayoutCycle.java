package com.sun.javafx.perf;

public class LayoutCycle {

    public enum Type {
        MANUAL,
        SCENE,
        PULSE
    }

    private final LayoutFrame root;
    private final Type type;

    LayoutCycle(LayoutFrame root, Type type) {
        this.root = root;
        this.type = type;
    }

    public LayoutFrame getRoot() {
        return root;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Layouting ");
        builder.append(root.getNode().getClass().getSimpleName()).append(' ');

        switch (type) {
            case MANUAL:
                builder.append("(triggered manually)");
                break;
            case SCENE:
                builder.append("(triggered by scene out-of-pulse)");
                break;
            case PULSE:
                builder.append("(triggered by scene pulse)");
                break;
        }

        builder.append(", cumulative layout passes: ")
            .append(root.getCumulativePasses())
            .append(System.lineSeparator())
            .append("--> ");

        printLayoutTree(builder, root, "    ", false);

        return builder.toString();
    }

    private void printLayoutTree(StringBuilder text, LayoutFrame frame, String prefix, boolean skin) {
        text.append(frame.getNode().getClass().getSimpleName());

        String id = frame.getNode().getId();
        boolean bracket = id != null && !id.isEmpty() || frame.isLayoutRoot() || skin;

        if (bracket) {
            text.append("[");
            boolean comma = false;

            if (id != null && !id.isEmpty()) {
                text.append("id=").append(id);
                comma = true;
            }

            if (frame.isLayoutRoot()) {
                text.append(comma ? ", root" : "root");
                comma = true;
            }

            if (skin) {
                text.append(comma ? ", skin" : "skin");
            }

            text.append("]");
        }

        text.append(": ")
            .append(frame.getPasses())
            .append(System.lineSeparator());

        skin = isControl(frame.getNode().getClass());

        for (int i = 0; i < frame.getChildren().size(); ++i) {
            text.append(prefix);
            String appendix;

            if (i == frame.getChildren().size() - 1) {
                text.append("\\--- ");
                appendix = "     ";
            } else {
                text.append("+--- ");
                appendix = "|    ";
            }

            printLayoutTree(text, frame.getChildren().get(i), prefix + appendix, skin);
        }
    }

    private boolean isControl(Class<?> clazz) {
        return clazz.getName().equals("javafx.scene.control.Control") ||
            clazz.getSuperclass() != null && isControl(clazz.getSuperclass());
    }

}
