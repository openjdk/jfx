# Release Notes for JavaFX 21

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 21 release. JavaFX 21 requires JDK 17 or later.

## Important Changes

### JavaFX Requires macOS 11 or Later

On Mac platforms, JavaFX 21 requires macOS 11 or later. An exception will be thrown when initializing the JavaFX runtime on older versions of macOS.

See [JDK-8308114](https://bugs.openjdk.org/browse/JDK-8308114) for more information.

### JavaFX Requires GTK 3 on Linux

On Linux platforms, JavaFX 21 requires GTK3 version 3.8 or later. The JavaFX GTK2 library has been removed. An exception will be thrown when initializing the JavaFX runtime if the GTK 3 library cannot be loaded.

See [JDK-8299595](https://bugs.openjdk.org/browse/JDK-8299595) for more information.

### Return Type of javafx.css.Match::getPseudoClasses Changed

The return type of `javafx.css.Match.getPseudoClasses` has been changed to `Set<PseudoClass>` instead of a non-public type. Most applications will not be impacted by this change, since this is not a method that applications ever need to call. In the unlikely event that an existing application binary does call this method, a `NoSuchMethodError` will be thrown. The solution is to recompile the application.

See [JDK-8304959](https://bugs.openjdk.org/browse/JDK-8304959) for more information.

### Event Handler Methods Added to EventTarget Interface

Four new default methods were added to the `javafx.event.EventTarget` interface:

```
<E extends Event> void addEventHandler(EventType<E>, EventHandler<? super E>)
<E extends Event> void removeEventHandler(EventType<E>, EventHandler<? super E>)
<E extends Event> void addEventFilter(EventType<E>, EventHandler<? super E>)
<E extends Event> void removeEventFilter(EventType<E>, EventHandler<? super E>)
```

This change allows applications to manage event handlers for all `EventTarget` implementations.

Most applications will not run into any backward compatibility problems with this change. The change is fully binary compatible and, for most use cases, it is also source compatible.

A compilation error will occur in one of the following two unlikely cases:

* An application class extends `Menu`, `MenuItem`, `TableColumnBase`, or `TreeItem`, and overrides `addEventHandler` or `removeEventHandler`
* An application class that is not a subclass of `Node` implements their own `addEventHandler` or `removeEventHandler` using a signature other than that of the newly added methods (for example, copying the same incorrect pattern that `MenuItem` used prior to this release)

In these cases, the declared methods must be changed to conform to the updated interface method signature.

See [JDK-8306021](https://bugs.openjdk.org/browse/JDK-8306021) for more information.

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8284542](https://bugs.openjdk.org/browse/JDK-8284542)|[Win] Missing attribute for toggle state of CheckBox in CheckBoxTreeItem|accessibility
[JDK-8301312](https://bugs.openjdk.org/browse/JDK-8301312)|Create implementation of NSAccessibilityButton protocol|accessibility
[JDK-8283063](https://bugs.openjdk.org/browse/JDK-8283063)|Optimize Observable{List/Set/Map}Wrapper.retainAll/removeAll|base
[JDK-8304439](https://bugs.openjdk.org/browse/JDK-8304439)|Subscription based listeners|base
[JDK-8304960](https://bugs.openjdk.org/browse/JDK-8304960)|ObservableListBase should defer constructing ListChangeBuilder|base
[JDK-8305885](https://bugs.openjdk.org/browse/JDK-8305885)|Use ReadOnly*PropertyBase class where possible|base
[JDK-8306021](https://bugs.openjdk.org/browse/JDK-8306021)|Add event handler management to EventTarget|base
[JDK-8090647](https://bugs.openjdk.org/browse/JDK-8090647)|Mnemonics: on windows we should cancel the underscore latch when an app loses focus|controls
[JDK-8091153](https://bugs.openjdk.org/browse/JDK-8091153)|Customize the Table Button Menu|controls
[JDK-8091419](https://bugs.openjdk.org/browse/JDK-8091419)|TableView: invoke table menu button programmatically|controls
[JDK-8307960](https://bugs.openjdk.org/browse/JDK-8307960)|Create Table Column PopupMenu lazily|controls
[JDK-8309470](https://bugs.openjdk.org/browse/JDK-8309470)|Potential performance improvements in VirtualFlow|controls
[JDK-8290765](https://bugs.openjdk.org/browse/JDK-8290765)|Remove parent disabled/treeVisible listeners|graphics
[JDK-8307363](https://bugs.openjdk.org/browse/JDK-8307363)|TextFlow.underlineShape()|graphics
[JDK-8299756](https://bugs.openjdk.org/browse/JDK-8299756)|Minor updates in CSS Reference|other
[JDK-8306648](https://bugs.openjdk.org/browse/JDK-8306648)|Update the JavaDocs to show the NEW section and DEPRECATED versions|other
[JDK-8307208](https://bugs.openjdk.org/browse/JDK-8307208)|Add GridPane constructor that accepts hGap and vGap values|scenegraph
[JDK-8260528](https://bugs.openjdk.org/browse/JDK-8260528)|Clean glass-gtk sizing and positioning code|window-toolkit
[JDK-8299595](https://bugs.openjdk.org/browse/JDK-8299595)|Remove terminally deprecated JavaFX GTK 2 library|window-toolkit
[JDK-8302355](https://bugs.openjdk.org/browse/JDK-8302355)|Public API for Toolkit.canStartNestedEventLoop()|window-toolkit

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8284662](https://bugs.openjdk.org/browse/JDK-8284662)|[Win] Screen reader fails to read ListView/ComboBox item count > 100|accessibility
[JDK-8298382](https://bugs.openjdk.org/browse/JDK-8298382)|JavaFX ChartArea Accessibility Reader|accessibility
[JDK-8308191](https://bugs.openjdk.org/browse/JDK-8308191)|[macOS] VoiceOver decorations are shifted on second monitor|accessibility
[JDK-8224260](https://bugs.openjdk.org/browse/JDK-8224260)|ChangeListener not triggered when adding a new listener in invalidated method |base
[JDK-8303740](https://bugs.openjdk.org/browse/JDK-8303740)|JavaFX - Leak in Logging, Logging remembers last exception|base
[JDK-8303897](https://bugs.openjdk.org/browse/JDK-8303897)|ObservableValue's when binding should only invalidate when strictly needed|base
[JDK-8308114](https://bugs.openjdk.org/browse/JDK-8308114)|Bump minimum version of macOS for x64 to 11.0 (matching aarch64)|build
[JDK-8088594](https://bugs.openjdk.org/browse/JDK-8088594)|NullPointerException on showing submenu of a contextmenu|controls
[JDK-8088998](https://bugs.openjdk.org/browse/JDK-8088998)|XYChart: duplicate child added exception when remove & add a series in several charts|controls
[JDK-8090123](https://bugs.openjdk.org/browse/JDK-8090123)|Items are no longer visible when collection is changed|controls
[JDK-8137244](https://bugs.openjdk.org/browse/JDK-8137244)|Empty Tree/TableView with CONSTRAINED_RESIZE_POLICY is not properly resized|controls
[JDK-8138842](https://bugs.openjdk.org/browse/JDK-8138842)|TableViewSelectionModel.selectIndices does not select index 0|controls
[JDK-8154038](https://bugs.openjdk.org/browse/JDK-8154038)|Spinner's converter should update its editor|controls
[JDK-8172849](https://bugs.openjdk.org/browse/JDK-8172849)|Non-intuitive baseline alignment for labeled controls with graphics|controls
[JDK-8173321](https://bugs.openjdk.org/browse/JDK-8173321)|TableView: Click on right trough has no effect when cell height is higher than viewport height|controls
[JDK-8178368](https://bugs.openjdk.org/browse/JDK-8178368)|Right alignment of text fields and alignment of prompt text works incorrectly|controls
[JDK-8230833](https://bugs.openjdk.org/browse/JDK-8230833)|LabeledSkinBase computes wrong height with ContentDisplay.GRAPHIC_ONLY|controls
[JDK-8237505](https://bugs.openjdk.org/browse/JDK-8237505)|RadioMenuItem in ToggleGroup: deselected on accelerator|controls
[JDK-8245919](https://bugs.openjdk.org/browse/JDK-8245919)|Region#padding property rendering error|controls
[JDK-8283551](https://bugs.openjdk.org/browse/JDK-8283551)|ControlAcceleratorSupport menu items listener causes memory leak|controls
[JDK-8293836](https://bugs.openjdk.org/browse/JDK-8293836)|Rendering performance degradation at bottom of TableView with many rows|controls
[JDK-8299986](https://bugs.openjdk.org/browse/JDK-8299986)|Wrong sublist used in ListChangeListener|controls
[JDK-8300893](https://bugs.openjdk.org/browse/JDK-8300893)|Wrong state after deselecting two or more cells of a TableView selection |controls
[JDK-8303026](https://bugs.openjdk.org/browse/JDK-8303026)|[TextField] IOOBE on setting text with control characters that replaces existing text|controls
[JDK-8303680](https://bugs.openjdk.org/browse/JDK-8303680)|Virtual Flow freezes after calling scrollTo and scrollPixels in succession|controls
[JDK-8305248](https://bugs.openjdk.org/browse/JDK-8305248)|TableView not rendered correctly after column is made visible if fixed cell size is set|controls
[JDK-8306447](https://bugs.openjdk.org/browse/JDK-8306447)|Adding an element to a long existing list may cause the first visible element to jump|controls
[JDK-8307538](https://bugs.openjdk.org/browse/JDK-8307538)|Memory leak in TreeTableView when calling refresh|controls
[JDK-8310638](https://bugs.openjdk.org/browse/JDK-8310638)|Filtering a TableView with a large number of items freezes the UI|controls
[JDK-8311127](https://bugs.openjdk.org/browse/JDK-8311127)|The fix for TableView / TreeTableView menu button affects all table column headers|controls
[JDK-8233955](https://bugs.openjdk.org/browse/JDK-8233955)|VM crashes if more than one file are added to ClipboardContent via drag and drop|graphics
[JDK-8246104](https://bugs.openjdk.org/browse/JDK-8246104)|Some complex text doesn't render correctly on macOS|graphics
[JDK-8251862](https://bugs.openjdk.org/browse/JDK-8251862)|Wrong position of Popup windows at the intersection of 2 screens|graphics
[JDK-8281327](https://bugs.openjdk.org/browse/JDK-8281327)|JavaFX does not support fonts installed per-user on Windows 10/11|graphics
[JDK-8290092](https://bugs.openjdk.org/browse/JDK-8290092)|Temporary files are kept when call Clipboard.getSystemClipboard().getImage() |graphics
[JDK-8290866](https://bugs.openjdk.org/browse/JDK-8290866)|Apple Color Emoji turns gray after JavaFX version 18|graphics
[JDK-8295078](https://bugs.openjdk.org/browse/JDK-8295078)|TextField blurry when inside an TitledPane -> AnchorPane|graphics
[JDK-8299968](https://bugs.openjdk.org/browse/JDK-8299968)|Second call to Stage.setScene() create sizing issue with uiScale > 1.0|graphics
[JDK-8300872](https://bugs.openjdk.org/browse/JDK-8300872)|WebView's ColorChooser fails to initialize when running in security context|graphics
[JDK-8302511](https://bugs.openjdk.org/browse/JDK-8302511)|HitInfo.toString() throws IllegalArgumentException|graphics
[JDK-8302797](https://bugs.openjdk.org/browse/JDK-8302797)|ArrayIndexOutOfBoundsException in TextRun.getWrapIndex()|graphics
[JDK-8304831](https://bugs.openjdk.org/browse/JDK-8304831)|TextFlow.hitTest.insertionIndex incorrect with surrogate pairs|graphics
[JDK-8306708](https://bugs.openjdk.org/browse/JDK-8306708)|Region.layoutInArea uses incorrect snap scale value|graphics
[JDK-8306990](https://bugs.openjdk.org/browse/JDK-8306990)|Guarantees given by Region's floor and ceiling functions should work for larger values|graphics
[JDK-8309508](https://bugs.openjdk.org/browse/JDK-8309508)|Possible memory leak in JPEG image loader|graphics
[JDK-8309935](https://bugs.openjdk.org/browse/JDK-8309935)|Mac - SystemMenuBar, IndexOutOfBoundsException on change|graphics
[JDK-8313227](https://bugs.openjdk.org/browse/JDK-8313227)|Correct attenuation indicator for removed lights|graphics
[JDK-8304290](https://bugs.openjdk.org/browse/JDK-8304290)|Some JNI calls made without checking exceptions in media|media
[JDK-8306328](https://bugs.openjdk.org/browse/JDK-8306328)|Update libFFI to 3.4.4|media
[JDK-8194704](https://bugs.openjdk.org/browse/JDK-8194704)|Text/TextFlow hitTest() javadoc|scenegraph
[JDK-8301763](https://bugs.openjdk.org/browse/JDK-8301763)|Adding children to wrong index leaves inconsistent state in Parent#childrenSet|scenegraph
[JDK-8304933](https://bugs.openjdk.org/browse/JDK-8304933)|BitSet (used for CSS pseudo class states) listener management is incorrect|scenegraph
[JDK-8304959](https://bugs.openjdk.org/browse/JDK-8304959)|Public API in javafx.css.Match should not return private API class PseudoClassState|scenegraph
[JDK-8231865](https://bugs.openjdk.org/browse/JDK-8231865)|JFXPanel sends resize event with size 0x0 on HiDPI devices|swing
[JDK-8242419](https://bugs.openjdk.org/browse/JDK-8242419)|JFXPanel: MouseEvent always reports that Primary button changed state if held|swing
[JDK-8299977](https://bugs.openjdk.org/browse/JDK-8299977)|Update WebKit to 615.1|web
[JDK-8306115](https://bugs.openjdk.org/browse/JDK-8306115)|Update libxml2 to 2.10.4|web
[JDK-8306329](https://bugs.openjdk.org/browse/JDK-8306329)|Update ICU4C to 73.1|web
[JDK-8150709](https://bugs.openjdk.org/browse/JDK-8150709)|Mac OSX and German Keyboard Layout (Y/Z)|window-toolkit
[JDK-8275033](https://bugs.openjdk.org/browse/JDK-8275033)|Drag and drop a file produces NullPointerException Cannot read field "dragboard"|window-toolkit
[JDK-8278938](https://bugs.openjdk.org/browse/JDK-8278938)|[Win] Robot can target wrong key for punctuation and symbols|window-toolkit
[JDK-8299348](https://bugs.openjdk.org/browse/JDK-8299348)|Size-restricted window can be observed with incorrect dimensions|window-toolkit
[JDK-8304441](https://bugs.openjdk.org/browse/JDK-8304441)|[macos] Crash when putting invalid unicode char on clipboard|window-toolkit
[JDK-8306121](https://bugs.openjdk.org/browse/JDK-8306121)|Scene not rendered initially when changing scenes after fix for JDK-8296621|window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8304751 (not public)|Improve pipeline layout|graphics
JDK-8299781 (not public)|Improve JFX navigation|web
JDK-8303501 (not public)|Unable to navigate to relative URLs after fix for JDK-8299781|web
