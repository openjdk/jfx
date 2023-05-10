# Release Notes for JavaFX 20

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 20 release. JavaFX 20 requires JDK 17 or later.

## Important Changes

### JavaFX 20 Requires JDK 17 or Later

JavaFX 20 is compiled with `--release 17` and thus requires JDK 17
or later in order to run. If you attempt to run with an older JDK,
the Java launcher will exit with an error message indicating that the
`javafx.base` module cannot be read.

See [JDK-8290530](https://bugs.openjdk.org/browse/JDK-8290530) for more information.

### FXML JavaScript Engine Disabled by Default

The “JavaScript script engine” for FXML is now disabled by default. Any `.fxml` file that has a "javascript" Processing Instruction (PI) will no longer load by default, and an exception will be thrown.

If the JDK has a JavaScript script engine, it can be enabled by setting the system property:

```
-Djavafx.allowjs=true
```

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8293839](https://bugs.openjdk.org/browse/JDK-8293839)|Documentation memory consistency effects of runLater|application-lifecycle
[JDK-8290040](https://bugs.openjdk.org/browse/JDK-8290040)|Provide simplified deterministic way to manage listeners|base
[JDK-8290530](https://bugs.openjdk.org/browse/JDK-8290530)|Bump minimum JDK version for JavaFX to JDK 17|build
[JDK-8290844](https://bugs.openjdk.org/browse/JDK-8290844)|Add Skin.install() method|controls
[JDK-8294809](https://bugs.openjdk.org/browse/JDK-8294809)|ListenerHelper for managing and disconnecting listeners|controls
[JDK-8287604](https://bugs.openjdk.org/browse/JDK-8287604)|Update MarlinFX to 0.9.4.6|graphics
[JDK-8287822](https://bugs.openjdk.org/browse/JDK-8287822)|[macos] Remove support of duplicated formats from macOS|media
[JDK-8293119](https://bugs.openjdk.org/browse/JDK-8293119)|Additional constrained resize policies for Tree/TableView|other

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8087557](https://bugs.openjdk.org/browse/JDK-8087557)|[Win] [Accessibility, Dialogs] Alert Dialog content is not fully read by Screen Reader|accessibility
[JDK-8284281](https://bugs.openjdk.org/browse/JDK-8284281)|[Accessibility] [Win] [Narrator] Exceptions with TextArea & TextField when deleted last char|accessibility
[JDK-8291087](https://bugs.openjdk.org/browse/JDK-8291087)|Wrong position of focus of screen reader on Windows with screen scale > 1|accessibility
[JDK-8293795](https://bugs.openjdk.org/browse/JDK-8293795)|[Accessibility] [Win] [Narrator] Exceptions When Deleting Text with Continuous Key Press in TextArea and TextField|accessibility
[JDK-8243115](https://bugs.openjdk.org/browse/JDK-8243115)|Spurious invalidations due to bug in IntegerBinding and other classes|base
[JDK-8087673](https://bugs.openjdk.org/browse/JDK-8087673)|[TableView] TableView and TreeTableView menu button overlaps columns when using a constrained resize policy.|controls
[JDK-8089009](https://bugs.openjdk.org/browse/JDK-8089009)|TableView with CONSTRAINED_RESIZE_POLICY incorrectly displays a horizontal scroll bar.|controls
[JDK-8089280](https://bugs.openjdk.org/browse/JDK-8089280)|horizontal scrollbar should never become visible in TableView with constrained resize policy|controls
[JDK-8187145](https://bugs.openjdk.org/browse/JDK-8187145)|(Tree)TableView with null selectionModel: throws NPE on sorting|controls
[JDK-8190411](https://bugs.openjdk.org/browse/JDK-8190411)|NPE in SliderSkin:140 if Slider.Tooltip.autohide is true|controls
[JDK-8209017](https://bugs.openjdk.org/browse/JDK-8209017)|CheckBoxTreeCell: graphic on TreeItem not always showing|controls
[JDK-8216507](https://bugs.openjdk.org/browse/JDK-8216507)|StyleablePropertyFactory: example in class javadoc does not compile|controls
[JDK-8218826](https://bugs.openjdk.org/browse/JDK-8218826)|TableRowSkinBase: horizontal layout broken if row has padding|controls
[JDK-8235491](https://bugs.openjdk.org/browse/JDK-8235491)|Tree/TableView: implementation of isSelected(int) violates contract|controls
[JDK-8245145](https://bugs.openjdk.org/browse/JDK-8245145)|Spinner: throws IllegalArgumentException when replacing skin|controls
[JDK-8252863](https://bugs.openjdk.org/browse/JDK-8252863)|Spinner keeps spinning if removed from Scene|controls
[JDK-8254676](https://bugs.openjdk.org/browse/JDK-8254676)|Alert disables Tab selection when TabDragPolicy REORDER is used|controls
[JDK-8256397](https://bugs.openjdk.org/browse/JDK-8256397)|MultipleSelectionModel throws IndexOutOfBoundException|controls
[JDK-8268877](https://bugs.openjdk.org/browse/JDK-8268877)|TextInputControlSkin: incorrect inputMethod event handler after switching skin|controls
[JDK-8279514](https://bugs.openjdk.org/browse/JDK-8279514)|NPE on clearing value of IntegerSpinnerValueFactory|controls
[JDK-8279640](https://bugs.openjdk.org/browse/JDK-8279640)|ListView with null SelectionModel/FocusModel throws NPE|controls
[JDK-8289357](https://bugs.openjdk.org/browse/JDK-8289357)|(Tree)TableView is null in (Tree)TableRowSkin during autosize|controls
[JDK-8290863](https://bugs.openjdk.org/browse/JDK-8290863)|Update the documentation of Virtualized controls to include the best practice of not using Nodes directly in the item list|controls
[JDK-8291625](https://bugs.openjdk.org/browse/JDK-8291625)|DialogPane without header nor headerText nor graphic node adds padding to the left of the content pane|controls
[JDK-8291853](https://bugs.openjdk.org/browse/JDK-8291853)|[CSS] ClassCastException in CssStyleHelper calculateValue|controls
[JDK-8291908](https://bugs.openjdk.org/browse/JDK-8291908)|VirtualFlow creates unneeded empty cells|controls
[JDK-8292009](https://bugs.openjdk.org/browse/JDK-8292009)|Wrong text artifacts in table header|controls
[JDK-8292353](https://bugs.openjdk.org/browse/JDK-8292353)|TableRow vs. TreeTableRow: inconsistent visuals in cell selection mode|controls
[JDK-8293171](https://bugs.openjdk.org/browse/JDK-8293171)|Minor typographical errors in JavaDoc javafx.scene.control.ScrollPane.java|controls
[JDK-8293444](https://bugs.openjdk.org/browse/JDK-8293444)|Creating ScrollPane with same content component causes memory leak|controls
[JDK-8294589](https://bugs.openjdk.org/browse/JDK-8294589)|MenuBarSkin: memory leak when changing skin|controls
[JDK-8295175](https://bugs.openjdk.org/browse/JDK-8295175)|SplitPaneSkin: memory leak when changing skin|controls
[JDK-8295242](https://bugs.openjdk.org/browse/JDK-8295242)|ScrollBarSkin: memory leak when changing skin|controls
[JDK-8295339](https://bugs.openjdk.org/browse/JDK-8295339)|DatePicker updates its value property with wrong date when dialog closes|controls
[JDK-8295426](https://bugs.openjdk.org/browse/JDK-8295426)|MenuButtonSkin: memory leak when changing skin|controls
[JDK-8295500](https://bugs.openjdk.org/browse/JDK-8295500)|AccordionSkin: memory leak when changing skin|controls
[JDK-8295506](https://bugs.openjdk.org/browse/JDK-8295506)|ButtonBarSkin: memory leak when changing skin|controls
[JDK-8295531](https://bugs.openjdk.org/browse/JDK-8295531)|ComboBoxBaseSkin: memory leak when changing skin|controls
[JDK-8295754](https://bugs.openjdk.org/browse/JDK-8295754)|PaginationSkin: memory leak when changing skin|controls
[JDK-8295796](https://bugs.openjdk.org/browse/JDK-8295796)|ScrollPaneSkin: memory leak when changing skin|controls
[JDK-8295806](https://bugs.openjdk.org/browse/JDK-8295806)|TableViewSkin: memory leak when changing skin|controls
[JDK-8295809](https://bugs.openjdk.org/browse/JDK-8295809)|TreeTableViewSkin: memory leak when changing skin|controls
[JDK-8296409](https://bugs.openjdk.org/browse/JDK-8296409)|Multiple copies of accelerator change listeners are added to MenuItems, but only 1 is removed|controls
[JDK-8296413](https://bugs.openjdk.org/browse/JDK-8296413)|Tree/TableView with null focus model throws NPE in queryAccessibleAttribute()|controls
[JDK-8298728](https://bugs.openjdk.org/browse/JDK-8298728)|Cells in VirtualFlow jump after resizing|controls
[JDK-8181084](https://bugs.openjdk.org/browse/JDK-8181084)|JavaFX show big icons in system menu on macOS with Retina display|graphics
[JDK-8231864](https://bugs.openjdk.org/browse/JDK-8231864)|JavaFX Labels in Tab's VBox is not displayed until it is clicked |graphics
[JDK-8238968](https://bugs.openjdk.org/browse/JDK-8238968)|Inconsisent formatting of Rectangle2D toString method|graphics
[JDK-8265835](https://bugs.openjdk.org/browse/JDK-8265835)|Exception in Quantum due to null platformWindow|graphics
[JDK-8271395](https://bugs.openjdk.org/browse/JDK-8271395)|Crash during printing when disposing textures|graphics
[JDK-8289542](https://bugs.openjdk.org/browse/JDK-8289542)|Update JPEG Image Decoding Software to 9e|graphics
[JDK-8290841](https://bugs.openjdk.org/browse/JDK-8290841)|Notify menu event after a long press gesture on Android is not dispatched|graphics
[JDK-8290990](https://bugs.openjdk.org/browse/JDK-8290990)|Clear .root style class from a root node that is removed from a Scene/SubScene|graphics
[JDK-8295236](https://bugs.openjdk.org/browse/JDK-8295236)|Update JavaDoc in javafx.geometry.Point3D|graphics
[JDK-8295324](https://bugs.openjdk.org/browse/JDK-8295324)|JavaFX: Blank pages when printing|graphics
[JDK-8295327](https://bugs.openjdk.org/browse/JDK-8295327)|JavaFX - IllegalArgumentException when printing with margins equal to 0|graphics
[JDK-8296854](https://bugs.openjdk.org/browse/JDK-8296854)|NULL check of CTFontCopyAvailableTables return value is required|graphics
[JDK-8297554](https://bugs.openjdk.org/browse/JDK-8297554)|Remove Scene.KeyHandler|graphics
[JDK-8297680](https://bugs.openjdk.org/browse/JDK-8297680)|JavaDoc example for PseudoClass has minor typo|graphics
[JDK-8293971](https://bugs.openjdk.org/browse/JDK-8293971)|Loading new Media from resources can sometimes fail when loading from FXML|media
[JDK-8294400](https://bugs.openjdk.org/browse/JDK-8294400)|Provide media support for libavcodec version 59|media
[JDK-8297362](https://bugs.openjdk.org/browse/JDK-8297362)|EOS might not be delivered by progressbuffer in some cases|media
[JDK-8293587](https://bugs.openjdk.org/browse/JDK-8293587)|Fix mistakes in FX API docs|other
[JDK-8295962](https://bugs.openjdk.org/browse/JDK-8295962)|Reference to State in Task.java is ambiguous when building with JDK 19|other
[JDK-8303019](https://bugs.openjdk.org/browse/JDK-8303019)|cssref.html incorrect internal link in Path|other
[JDK-8279214](https://bugs.openjdk.org/browse/JDK-8279214)|Memory leak in Scene after dragging a cell|scenegraph
[JDK-8297130](https://bugs.openjdk.org/browse/JDK-8297130)|ComboBox popup doesn't close after selecting value that was added with 'runLater'|scenegraph
[JDK-8300013](https://bugs.openjdk.org/browse/JDK-8300013)|Node.focusWithin doesn't account for nested focused nodes|scenegraph
[JDK-8222210](https://bugs.openjdk.org/browse/JDK-8222210)|JFXPanel popups open at wrong coordinates when using multiple hidpi monitors|swing
[JDK-8285881](https://bugs.openjdk.org/browse/JDK-8285881)|Update WebKit to 614.1|web
[JDK-8289541](https://bugs.openjdk.org/browse/JDK-8289541)|Update ICU4C to 71.1|web
[JDK-8292609](https://bugs.openjdk.org/browse/JDK-8292609)|Cherry-pick WebKit 614.1 stabilization fixes|web
[JDK-8295755](https://bugs.openjdk.org/browse/JDK-8295755)|Update SQLite to 3.39.4|web
[JDK-8298167](https://bugs.openjdk.org/browse/JDK-8298167)|Opacity in WebView not working anymore|web
[JDK-8292922](https://bugs.openjdk.org/browse/JDK-8292922)|[Linux] No more drag events when new Stage is created in drag handler|window-toolkit
[JDK-8296621](https://bugs.openjdk.org/browse/JDK-8296621)|Stage steals focus on scene change|window-toolkit
[JDK-8296654](https://bugs.openjdk.org/browse/JDK-8296654)|[macos] Crash when launching JavaFX app with JDK that targets SDK 13|window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8294779 (not public)|Improve FX pages|fxml
JDK-8289336 (not public)|Better platform image support|graphics
JDK-8289343 (not public)|Better GL support|graphics
JDK-8299628 (not public)|BMP top-down images fail to load after JDK-8289336|graphics
JDK-8292097 (not public)|Better video decoding|media
JDK-8292105 (not public)|Improve Robot functionality|window-toolkit
JDK-8292112 (not public)|Better DragView handling|window-toolkit
