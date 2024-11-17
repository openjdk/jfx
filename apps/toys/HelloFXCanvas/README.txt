====================================
HOW TO RUN
====================================

1) Build HelloFXCanvas by running "gradle apps" task in root folder
2) Run "java @build/run.args --add-modules javafx.swt --enable-native-access=ALL-UNNAMED -cp apps/toys/HelloFXCanvas/dist/HelloFXCanvas.jar:build/libs/swt-debug.jar hellofxcanvas.HelloFXCanvas"
3) In case of macOS we also need to pass -XstartOnFirstThread JVM argument to make SWT work with JavaFX
4) In case of Windows path separator should be changed from ':' to ';'
=================================================================================================================
