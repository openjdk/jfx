# Release Notes for JavaFX 18

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 18 release. JavaFX 18 requires JDK 11 or later.

## Important Changes

### Deprecate JavaFX GTK 2 library for removal

The JavaFX GTK 2 library is deprecated and will be removed in a future release. The JavaFX runtime issues a warning if the GTK 2 library is requested on the command line via `java -Djdk.gtk.version=2`.
The JavaFX runtime also issues a warning if the GTK 2 library is selected as a fallback, which happens if the GTK 3 library cannot be loaded. Application developers should avoid requesting the GTK 2 library.

See [JDK-8273089](https://bugs.openjdk.org/browse/JDK-8273089) for more information.

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8267472](https://bugs.openjdk.org/browse/JDK-8267472)|JavaFX modules to include version information|build
[JDK-8172095](https://bugs.openjdk.org/browse/JDK-8172095)|Let Node.managed become CSS-styleable|controls
[JDK-8234921](https://bugs.openjdk.org/browse/JDK-8234921)|Add DirectionalLight to the selection of 3D light types|graphics
[JDK-8272870](https://bugs.openjdk.org/browse/JDK-8272870)|Add convenience factory methods for Border and Background|graphics
[JDK-8278595](https://bugs.openjdk.org/browse/JDK-8278595)|Provide more information when a pipeline can't be used|graphics
[JDK-8278860](https://bugs.openjdk.org/browse/JDK-8278860)|Streamline properties for Monocle|graphics
[JDK-8273096](https://bugs.openjdk.org/browse/JDK-8273096)|Add support for H.265/HEVC to JavaFX Media|media
[JDK-8214158](https://bugs.openjdk.org/browse/JDK-8214158)|Implement HostServices.showDocument on macOS without calling AWT|other
[JDK-8090547](https://bugs.openjdk.org/browse/JDK-8090547)|Allow for transparent backgrounds in WebView|web
[JDK-8273089](https://bugs.openjdk.org/browse/JDK-8273089)|Deprecate JavaFX GTK 2 library for removal|window-toolkit

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8203463](https://bugs.openjdk.org/browse/JDK-8203463)|[Accessibility, Narrator] NPE in TableView|accessibility
[JDK-8273969](https://bugs.openjdk.org/browse/JDK-8273969)|Memory Leak on the Runnable provided to Platform.startup|application-lifecycle
[JDK-8270838](https://bugs.openjdk.org/browse/JDK-8270838)|Remove deprecated protected access members from DateTimeStringConverter|base
[JDK-8273138](https://bugs.openjdk.org/browse/JDK-8273138)|BidirectionalBinding fails to observe changes of invalid properties|base
[JDK-8273754](https://bugs.openjdk.org/browse/JDK-8273754)|Re-introduce Automatic-Module-Name in empty jars|build
[JDK-8278260](https://bugs.openjdk.org/browse/JDK-8278260)|JavaFX shared libraries not stripped on Linux or macOS|build
[JDK-8089398](https://bugs.openjdk.org/browse/JDK-8089398)|[ChoiceBox, ComboBox] throws NPE on setting value on null selectionModel|controls
[JDK-8090158](https://bugs.openjdk.org/browse/JDK-8090158)|Wrong implementation of adjustValue in scrollBars|controls
[JDK-8187474](https://bugs.openjdk.org/browse/JDK-8187474)|Tree-/TableCell, TreeCell: editingCell/Item not updated in cell.startEdit|controls
[JDK-8188026](https://bugs.openjdk.org/browse/JDK-8188026)|TextFieldXXCell: NPE on calling startEdit|controls
[JDK-8188027](https://bugs.openjdk.org/browse/JDK-8188027)|List/TableCell: must not fire event in startEdit if already editing|controls
[JDK-8191995](https://bugs.openjdk.org/browse/JDK-8191995)|Regression: DatePicker must commit on focusLost|controls
[JDK-8197991](https://bugs.openjdk.org/browse/JDK-8197991)|Selecting many items in a TableView is very slow|controls
[JDK-8205915](https://bugs.openjdk.org/browse/JDK-8205915)|[macOS] Accelerator assigned to button in dialog fires menuItem in owning stage|controls
[JDK-8231644](https://bugs.openjdk.org/browse/JDK-8231644)|TreeTableView Regression: Indentation wrong using Label as column content type|controls
[JDK-8240506](https://bugs.openjdk.org/browse/JDK-8240506)|TextFieldSkin/Behavior: misbehavior on switching skin|controls
[JDK-8244419](https://bugs.openjdk.org/browse/JDK-8244419)|TextAreaSkin: throws UnsupportedOperation on dispose|controls
[JDK-8268295](https://bugs.openjdk.org/browse/JDK-8268295)|Tree- and TableCell sub implementations should respect the row editability|controls
[JDK-8269081](https://bugs.openjdk.org/browse/JDK-8269081)|Tree/ListViewSkin: must remove flow on dispose|controls
[JDK-8269871](https://bugs.openjdk.org/browse/JDK-8269871)|CellEditEvent: must not throw NPE in accessors|controls
[JDK-8271474](https://bugs.openjdk.org/browse/JDK-8271474)|Tree-/TableCell: inconsistent edit event firing pattern|controls
[JDK-8271484](https://bugs.openjdk.org/browse/JDK-8271484)|Tree-/TableCell: NPE when accessing edit event from startEdit|controls
[JDK-8272118](https://bugs.openjdk.org/browse/JDK-8272118)|ListViewSkin et al: must not cancel edit on scrolling|controls
[JDK-8273071](https://bugs.openjdk.org/browse/JDK-8273071)|SeparatorSkin: must remove child on dispose|controls
[JDK-8273324](https://bugs.openjdk.org/browse/JDK-8273324)|IllegalArgumentException: fromIndex(0) > toIndex(-1) after clear and select TableCell|controls
[JDK-8274022](https://bugs.openjdk.org/browse/JDK-8274022)|Additional Memory Leak in ControlAcceleratorSupport|controls
[JDK-8274061](https://bugs.openjdk.org/browse/JDK-8274061)|Tree-/TableRowSkin: misbehavior on switching skin|controls
[JDK-8274137](https://bugs.openjdk.org/browse/JDK-8274137)|TableView scrollbar/header misaligned when reloading data|controls
[JDK-8274854](https://bugs.openjdk.org/browse/JDK-8274854)|Mnemonics for menu containing numeric text not working|controls
[JDK-8274433](https://bugs.openjdk.org/browse/JDK-8274433)|All Cells: misbehavior of startEdit|controls
[JDK-8274699](https://bugs.openjdk.org/browse/JDK-8274699)|Certain blend modes cannot be set from CSS|controls
[JDK-8274669](https://bugs.openjdk.org/browse/JDK-8274669)|Dialog sometimes ignores max height|controls
[JDK-8275911](https://bugs.openjdk.org/browse/JDK-8275911)|Keyboard doesn't show when tapping inside an iOS text input control|controls
[JDK-8276167](https://bugs.openjdk.org/browse/JDK-8276167)|VirtualFlow.scrollToTop doesn't scroll to the top of the last element|controls
[JDK-8276313](https://bugs.openjdk.org/browse/JDK-8276313)|ScrollPane scroll delta incorrectly depends on content height|controls
[JDK-8276553](https://bugs.openjdk.org/browse/JDK-8276553)|ListView scrollTo() is broken after fix for JDK-8089589|controls
[JDK-8281207](https://bugs.openjdk.org/browse/JDK-8281207)|TableView scrollTo() will not show last row for a custom cell factory.|controls
[JDK-8232812](https://bugs.openjdk.org/browse/JDK-8232812)|[MacOS] Double click title bar does not restore window size|graphics
[JDK-8236689](https://bugs.openjdk.org/browse/JDK-8236689)|macOS 10.15 Catalina: LCD text renders badly|graphics
[JDK-8254956](https://bugs.openjdk.org/browse/JDK-8254956)|[REDO] Memoryleak: Closed focused Stages are not collected with Monocle|graphics
[JDK-8255015](https://bugs.openjdk.org/browse/JDK-8255015)|Inconsistent illumination of 3D shape by PointLight|graphics
[JDK-8269374](https://bugs.openjdk.org/browse/JDK-8269374)|Menu inoperable after setting stage to second monitor|graphics
[JDK-8269638](https://bugs.openjdk.org/browse/JDK-8269638)|Property methods, setters, and getters in printing API should be final|graphics
[JDK-8269639](https://bugs.openjdk.org/browse/JDK-8269639)|[macos] Calling stage.setY(0) twice causes wrong popups location|graphics
[JDK-8276490](https://bugs.openjdk.org/browse/JDK-8276490)|Incorrect path for duplicate x and y values, when path falls outside axis bound|graphics
[JDK-8276915](https://bugs.openjdk.org/browse/JDK-8276915)|Crash on iOS 15.1 in GlassRunnable::dealloc|graphics
[JDK-8278905](https://bugs.openjdk.org/browse/JDK-8278905)|JavaFX: EnumConverter has a typo in the toString method|graphics
[JDK-8279328](https://bugs.openjdk.org/browse/JDK-8279328)|CssParser uses default charset instead of UTF-8|graphics
[JDK-8253351](https://bugs.openjdk.org/browse/JDK-8253351)|MediaPlayer does not display an mp4 if there no speakers connected to the PC's|media
[JDK-8268718](https://bugs.openjdk.org/browse/JDK-8268718)|[macos] Video stops, but audio continues to play when stopTime is reached|media
[JDK-8222455](https://bugs.openjdk.org/browse/JDK-8222455)|JavaFX error loading glass.dll from cache|other
[JDK-8270839](https://bugs.openjdk.org/browse/JDK-8270839)|Remove deprecated implementation methods from Scene|scenegraph
[JDK-8268849](https://bugs.openjdk.org/browse/JDK-8268849)|Update to 612.1 version of WebKit|web
[JDK-8270479](https://bugs.openjdk.org/browse/JDK-8270479)|WebKit 612.1 build fails with Visual Studio 2017|web
[JDK-8272329](https://bugs.openjdk.org/browse/JDK-8272329)|Cherry pick GTK WebKit 2.32.3 changes|web
[JDK-8274107](https://bugs.openjdk.org/browse/JDK-8274107)|Cherry pick GTK WebKit 2.32.4 changes|web
[JDK-8275138](https://bugs.openjdk.org/browse/JDK-8275138)|WebView: UserAgent string is empty for first request|web
[JDK-8276847](https://bugs.openjdk.org/browse/JDK-8276847)|JSException: ReferenceError: Can't find variable: IntersectionObserver|web
[JDK-8277133](https://bugs.openjdk.org/browse/JDK-8277133)|Dragboard contents retrieved all over again during a DND process on WebView|web
[JDK-8277457](https://bugs.openjdk.org/browse/JDK-8277457)|AccessControlException: access denied ("java.net.NetPermission" "getCookieHandler")|web
[JDK-8160597](https://bugs.openjdk.org/browse/JDK-8160597)|IllegalArgumentException when we initiate drag on Image|window-toolkit
[JDK-8227371](https://bugs.openjdk.org/browse/JDK-8227371)|Drag&Drop while holding the CMD key does not work on macOS|window-toolkit
[JDK-8242544](https://bugs.openjdk.org/browse/JDK-8242544)|CMD+ENTER key event crashes the application when invoked on dialog|window-toolkit
[JDK-8269967](https://bugs.openjdk.org/browse/JDK-8269967)|JavaFX should fail fast on macOS below minimum version|window-toolkit
[JDK-8269968](https://bugs.openjdk.org/browse/JDK-8269968)|[REDO] Bump minimum version of macOS for x64 to 10.12|window-toolkit
[JDK-8271398](https://bugs.openjdk.org/browse/JDK-8271398)|GTK3 drag view image swaps red and blue color channels|window-toolkit
[JDK-8274929](https://bugs.openjdk.org/browse/JDK-8274929)|Crash while reading specific clipboard content|window-toolkit
[JDK-8275723](https://bugs.openjdk.org/browse/JDK-8275723)|Crash on macOS 12 in GlassRunnable::dealloc|window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8263112 (not public) | Enhance String Conclusions | graphics
