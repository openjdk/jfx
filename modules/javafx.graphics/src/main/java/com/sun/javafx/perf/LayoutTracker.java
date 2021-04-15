package com.sun.javafx.perf;

import com.sun.javafx.scene.PropertyHelper;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.util.Stack;
import java.util.function.Consumer;

public class LayoutTracker {

    private static final boolean LOGGING_ENABLED = PropertyHelper.getBooleanProperty("javafx.sg.layoutStats");

    public static boolean isLoggingEnabled() {
        return LOGGING_ENABLED;
    }

    private final Stack<LayoutFrame> stack = new Stack<>();
    private LayoutFrame currentFrame;
    private boolean pulseLayout;
    private boolean sceneLayout;
    private Consumer<LayoutCycle> handler;

    public void setLayoutFinished(Consumer<LayoutCycle> handler) {
        this.handler = handler;
    }

    public Consumer<LayoutCycle> getLayoutFinished() {
        return handler;
    }

    public void beginPulseLayout() {
        pulseLayout = true;
    }

    public void beginSceneLayout() {
        sceneLayout = true;
    }

    public void pushNode(Parent node, boolean layoutRoot) {
        if (currentFrame == null) {
            currentFrame = new LayoutFrame(node, layoutRoot);
        } else {
            LayoutFrame childFrame = currentFrame.getFrame(node);
            if (childFrame == null) {
                childFrame = new LayoutFrame(node, layoutRoot);
                currentFrame.getChildren().add(childFrame);
            }

            currentFrame = childFrame;
        }

        stack.push(currentFrame);
    }

    public void popNode(int passes) {
        currentFrame.updatePasses(passes);
        stack.pop();

        if (stack.isEmpty()) {
            layoutFinished(currentFrame);
            currentFrame = null;
            pulseLayout = false;
            sceneLayout = false;
        } else {
            currentFrame = stack.peek();
        }
    }

    private void layoutFinished(LayoutFrame rootFrame) {
        if (rootFrame.getPasses() == 0 && rootFrame.getChildren().isEmpty()) {
            return;
        }

        LayoutCycle layoutInfo;
        if (pulseLayout) {
            layoutInfo = new LayoutCycle(rootFrame, LayoutCycle.Type.PULSE);
        } else if (sceneLayout) {
            layoutInfo = new LayoutCycle(rootFrame, LayoutCycle.Type.SCENE);
        } else {
            layoutInfo = new LayoutCycle(rootFrame, LayoutCycle.Type.MANUAL);
        }

        if (handler != null) {
            handler.accept(layoutInfo);
        }
    }

    private static SceneAccessor sceneAccessor;

    public static void releaseSceneTracker(Scene scene) {
        if (sceneAccessor != null) {
            sceneAccessor.setPerfTracker(scene, null);
        }
    }

    public static void setSceneAccessor(SceneAccessor accessor) {
        sceneAccessor = accessor;
    }

    public static LayoutTracker getSceneTracker(Scene scene) {
        LayoutTracker tracker = null;
        if (sceneAccessor != null) {
            tracker = sceneAccessor.getPerfTracker(scene);
            if (tracker == null) {
                tracker = new LayoutTracker();
                sceneAccessor.setPerfTracker(scene, tracker);
            }
        }
        return tracker;
    }

    public interface SceneAccessor {
        void setPerfTracker(Scene scene, LayoutTracker tracker);
        LayoutTracker getPerfTracker(Scene scene);
    }

}
