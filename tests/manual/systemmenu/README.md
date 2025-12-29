# Compiling and Running

To compile the tests, run the following command:

```
javac --module-path /path/to/jfx/lib/ --add-modules javafx.controls,javafx.fxml,javafx.swing *.java
```

To run the tests, run the following command:

```
java --module-path /path/to/jfx/lib/ --add-modules javafx.controls,javafx.fxml,javafx.swing TestClass
```