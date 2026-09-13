# Additional Information for JDK-8340852

## Reverting to Previous `ScrollPane` Behavior:

The fix for [JDK-8340852](https://bugs.openjdk.org/browse/JDK-8340852) changed the behavior of `ScrollPane`. With the latest update, `ScrollPane` only responds to keyboard navigation when it is the focused node. If you prefer the previous behavior, where `ScrollPane` always reacts to arrow keys and other navigational inputs, you can manually restore it by adding an event handler:

```java
scrollPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
    double x = 0;
    double y = 0;

    switch (e.getCode()) {
        case LEFT -> x = -0.1;
        case RIGHT -> x = 0.1;
        case UP -> y = -0.1;
        case DOWN -> y = 0.1;
        case PAGE_UP -> y = -0.9;
        case PAGE_DOWN, SPACE -> y = 0.9;
        case HOME -> x = y = Double.NEGATIVE_INFINITY;
        case END -> x = y = Double.POSITIVE_INFINITY;
        default -> {}
    }

    if (x != 0 || y != 0) {
        scrollByFraction(scrollPane, x, y);
        e.consume();
    }
});
```
Using this helper method to convert scroll fractions to values for the scrollbars, and set them:
```java
static void scrollByFraction(ScrollPane scrollPane, double x, double y) {
    Node content = scrollPane.getContent();
    if (content == null) return;

    Bounds viewportBounds = scrollPane.getViewportBounds();
    Bounds layoutBounds = content.getLayoutBounds();

    if (x != 0) {
        double visibleFraction = viewportBounds.getWidth() / layoutBounds.getWidth();
        double range = scrollPane.getHmax() - scrollPane.getHmin();
        double scrollFactor = range * visibleFraction / (1 - visibleFraction);
        scrollPane.setHvalue(scrollPane.getHvalue() + x * scrollFactor);
    }

    if (y != 0) {
        double visibleFraction = viewportBounds.getHeight() / layoutBounds.getHeight();
        double range = scrollPane.getVmax() - scrollPane.getVmin();
        double scrollFactor = range * visibleFraction / (1 - visibleFraction);
        scrollPane.setVvalue(scrollPane.getVvalue() + y * scrollFactor);
    }
}
```
Adding this event handler will make `ScrollPane` react to navigation keys as it did before the update.
