# Release Notes for JavaFX 19

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 19 release. JavaFX 19 requires JDK 11 or later.

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8274771](https://bugs.openjdk.java.net/browse/JDK-8274771)|Map, FlatMap and OrElse fluent bindings for ObservableValue|base
[JDK-8283346](https://bugs.openjdk.java.net/browse/JDK-8283346)|Optimize observable ArrayList creation in FXCollections|base
[JDK-8286552](https://bugs.openjdk.java.net/browse/JDK-8286552)|TextFormatter: UpdateValue/UpdateText is called, when no ValueConverter is set|controls
[JDK-8268225](https://bugs.openjdk.java.net/browse/JDK-8268225)|Support :focus-visible and :focus-within CSS pseudoclasses|graphics
[JDK-8277309](https://bugs.openjdk.java.net/browse/JDK-8277309)|Add support for H.265/HEVC to HTTP Live Streaming|media

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8290331](https://bugs.openjdk.java.net/browse/JDK-8290331)|Binding value left null when immediately revalidated in invalidation listener|base
[JDK-8291906](https://bugs.openjdk.java.net/browse/JDK-8291906)|Bindings.createXxxBinding inherit incorrect method docs|base
[JDK-8187307](https://bugs.openjdk.java.net/browse/JDK-8187307)|ListView, TableView, TreeView: receives editCancel event when edit is committed|controls
[JDK-8187309](https://bugs.openjdk.java.net/browse/JDK-8187309)|TreeCell must not change tree's data|controls
[JDK-8187596](https://bugs.openjdk.java.net/browse/JDK-8187596)|TreeView selection incorrectly changes after deleting an unselected row|controls
[JDK-8193442](https://bugs.openjdk.java.net/browse/JDK-8193442)|Removing TreeItem from a TreeTableView sometime changes selectedItem|controls
[JDK-8244234](https://bugs.openjdk.java.net/browse/JDK-8244234)|MenuButton: NPE on removing from scene with open popup|controls
[JDK-8251480](https://bugs.openjdk.java.net/browse/JDK-8251480)|TableColumnHeader: calc of cell width must respect row styling|controls
[JDK-8251481](https://bugs.openjdk.java.net/browse/JDK-8251481)|TableCell accessing row: NPE on auto-sizing|controls
[JDK-8251483](https://bugs.openjdk.java.net/browse/JDK-8251483)|TableCell: NPE on modifying item's list|controls
[JDK-8273336](https://bugs.openjdk.java.net/browse/JDK-8273336)|Clicking a selected cell from a group of selected cells in a TableView clears the selected items list but remains selected|controls
[JDK-8273339](https://bugs.openjdk.java.net/browse/JDK-8273339)|IOOBE with ListChangeListener added to the selectedItems list of a TableView|controls
[JDK-8276056](https://bugs.openjdk.java.net/browse/JDK-8276056)|Control.skin.setSkin(Skin) fails to call dispose() on discarded Skin|controls
[JDK-8277122](https://bugs.openjdk.java.net/browse/JDK-8277122)|SplitPane divider drag can hang the layout|controls
[JDK-8277853](https://bugs.openjdk.java.net/browse/JDK-8277853)|With Touch enabled devices scrollbar disappears and the table is scrolled to the beginning|controls
[JDK-8277756](https://bugs.openjdk.java.net/browse/JDK-8277756)|DatePicker listener might not be added when using second constructor|controls
[JDK-8277785](https://bugs.openjdk.java.net/browse/JDK-8277785)|ListView scrollTo jumps to wrong location when CellHeight is changed|controls
[JDK-8279228](https://bugs.openjdk.java.net/browse/JDK-8279228)|Leak in ScrollPaneSkin, related to touch events|controls
[JDK-8281723](https://bugs.openjdk.java.net/browse/JDK-8281723)|Spinner with split horizontal arrows and a border places right arrow incorrectly|controls
[JDK-8282093](https://bugs.openjdk.java.net/browse/JDK-8282093)|LineChart path incorrect when outside lower bound|controls
[JDK-8282100](https://bugs.openjdk.java.net/browse/JDK-8282100)|Missed top/left bouncing for ScrollPane on Raspberry Pi with Touchscreen|controls
[JDK-8283509](https://bugs.openjdk.java.net/browse/JDK-8283509)|Invisible menus can lead to IndexOutOfBoundsException|controls
[JDK-8284676](https://bugs.openjdk.java.net/browse/JDK-8284676)|TreeTableView loses sort ordering when applied on empty table|controls
[JDK-8284665](https://bugs.openjdk.java.net/browse/JDK-8284665)|First selected item of a TreeItem multiple selection gets removed if new items are constantly added to the TreeTableView|controls
[JDK-8285197](https://bugs.openjdk.java.net/browse/JDK-8285197)|TableColumnHeader: calc of cell width must respect row styling (TreeTableView)|controls
[JDK-8286261](https://bugs.openjdk.java.net/browse/JDK-8286261)|Selection of non-expanded non-leaf treeItem grows unexpectedly when adding two-level descendants|controls
[JDK-8289751](https://bugs.openjdk.java.net/browse/JDK-8289751)|Multiple unit test failures after JDK-8251483|controls
[JDK-8290348](https://bugs.openjdk.java.net/browse/JDK-8290348)|TreeTableView jumping to top|controls
[JDK-8277572](https://bugs.openjdk.java.net/browse/JDK-8277572)|ImageStorage should correctly handle MIME types for images encoded in data URIs|graphics
[JDK-8279013](https://bugs.openjdk.java.net/browse/JDK-8279013)|ES2Pipeline fails to detect AMD vega20 graphics card|graphics
[JDK-8285217](https://bugs.openjdk.java.net/browse/JDK-8285217)|[Android] Window's screen is not updated after native screen was disposed|graphics
[JDK-8288137](https://bugs.openjdk.java.net/browse/JDK-8288137)|The set of available printers is not updated without application restart|graphics
[JDK-8291502](https://bugs.openjdk.java.net/browse/JDK-8291502)|Mouse or touch presses on a non-focusable region don't clear the focusVisible flag of the current focus owner|graphics
[JDK-8280840](https://bugs.openjdk.java.net/browse/JDK-8280840)|Update libFFI to 3.4.2|media 
[JDK-8282054](https://bugs.openjdk.java.net/browse/JDK-8282054)|Mediaplayer not working with HTTP Live Stream link with query parameter appended with file extension m3u8|media
[JDK-8283218](https://bugs.openjdk.java.net/browse/JDK-8283218)|Update GStreamer to 1.20.1|media
[JDK-8283318](https://bugs.openjdk.java.net/browse/JDK-8283318)|Videos with unusual sizes cannot be played on windows|media
[JDK-8283403](https://bugs.openjdk.java.net/browse/JDK-8283403)|Update Glib to 2.72.0|media
[JDK-8280369](https://bugs.openjdk.java.net/browse/JDK-8280369)|native library cache should be platform/arch specific|other
[JDK-8281089](https://bugs.openjdk.java.net/browse/JDK-8281089)|JavaFX built with VS2019 and jlinked into JDK 11.x fails to start|other
[JDK-8286678](https://bugs.openjdk.java.net/browse/JDK-8286678)|Fix mistakes in FX API docs|other
[JDK-8281953](https://bugs.openjdk.java.net/browse/JDK-8281953)|NullPointer in InputMethod components in JFXPanel|swing
[JDK-8088420](https://bugs.openjdk.java.net/browse/JDK-8088420)|JavaFX WebView memory leak via EventListener|web
[JDK-8255940](https://bugs.openjdk.java.net/browse/JDK-8255940)|localStorage is null after window.close()|web
[JDK-8269115](https://bugs.openjdk.java.net/browse/JDK-8269115)|WebView paste event contains old data|web
[JDK-8278759](https://bugs.openjdk.java.net/browse/JDK-8278759)|PointerEvent: buttons property set to 0 when mouse down|web
[JDK-8278980](https://bugs.openjdk.java.net/browse/JDK-8278980)|Update WebKit to 613.1|web 
[JDK-8280020](https://bugs.openjdk.java.net/browse/JDK-8280020)|Underline and line-through not straight in WebView|web
[JDK-8280841](https://bugs.openjdk.java.net/browse/JDK-8280841)|Update SQLite to 3.37.2|web
[JDK-8281711](https://bugs.openjdk.java.net/browse/JDK-8281711)|Cherry-pick WebKit 613.1 stabilization fixes|web
[JDK-8282099](https://bugs.openjdk.java.net/browse/JDK-8282099)|Cherry-pick WebKit 613.1 stabilization fixes (2)|web
[JDK-8282134](https://bugs.openjdk.java.net/browse/JDK-8282134)|Certain regex can cause a JS trap in WebView|web
[JDK-8283328](https://bugs.openjdk.java.net/browse/JDK-8283328)|Update libxml2 to 2.9.13|web
[JDK-8284184](https://bugs.openjdk.java.net/browse/JDK-8284184)|Crash in GraphicsContextJava::drawLinesForText on https://us.yahoo.com/|web
[JDK-8286256](https://bugs.openjdk.java.net/browse/JDK-8286256)|Update libxml2 to 2.9.14|web
[JDK-8286257](https://bugs.openjdk.java.net/browse/JDK-8286257)|Update libxslt to 1.1.35|web
[JDK-8289587](https://bugs.openjdk.java.net/browse/JDK-8289587)|IllegalArgumentException: Color.rgb's red parameter (-16776961) expects color values 0-255|web
[JDK-8271054](https://bugs.openjdk.java.net/browse/JDK-8271054)|[REDO] Wrong stage gets focused after modal stage creation|window-toolkit
[JDK-8284654](https://bugs.openjdk.java.net/browse/JDK-8284654)|Modal behavior returns to wrong stage|window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8276371 (not public) | Better long buffering | web
JDK-8282121 (not public) | Improve WebKit referencing | web
