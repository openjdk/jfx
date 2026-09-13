# Release Notes for JavaFX 23

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 23 release. JavaFX 23 requires JDK 21 or later.

## Important Changes

### JavaFX 23 Requires JDK 21 or Later

JavaFX 23 is compiled with `--release 21` and thus requires JDK 21 or later in order to run. If you attempt to run with an older JDK, the Java launcher will exit with an error message indicating that the `javafx.base` module cannot be read.

See [JDK-8321603](https://bugs.openjdk.org/browse/JDK-8321603) for more information.

### Clicking on the Scrollbar Track of Virtualized Controls Scrolls by Viewport Length

Clicking on the scrollbar track of virtualized controls, such as `ListView`, `TreeView`, `TableView`, and `TreeTableView`, now scrolls by the viewport length rather than the length of the empty cell. Furthermore, cells are no longer aligned to the top or bottom of the viewport after scrolling.

See [JDK-8323511](https://bugs.openjdk.org/browse/JDK-8323511) for more information.

### Keyboard Scrolling in Virtualized Controls

Keyboard scrolling key bindings, `alt-ctrl-arrows` (`option-command-arrows` on macOS), have been added to virtualized controls such as  `ListView`, `TreeView`, `TableView`, and `TreeTableView` to improve accessibility.

See [JDK-8313138](https://bugs.openjdk.org/browse/JDK-8313138) for more information.

## List of New Features

Issue Key|Summary|Subcomponent
---------|-------|------------
[JDK-8092102](https://bugs.openjdk.org/browse/JDK-8092102)|Labeled: textTruncated property|controls
[JDK-8313138](https://bugs.openjdk.org/browse/JDK-8313138)|Scrollbar Keyboard enhancement|controls
[JDK-8267565](https://bugs.openjdk.org/browse/JDK-8267565)|Support "@3x" and greater high-density image naming convention|graphics
[JDK-8311895](https://bugs.openjdk.org/browse/JDK-8311895)|CSS Transitions|graphics
[JDK-8282999](https://bugs.openjdk.org/browse/JDK-8282999)|Add support for EXT-X-MEDIA tag in HTTP Live Streaming|media

## List of Other Enhancements

Issue Key|Summary|Subcomponent
---------|-------|------------
[JDK-8321603](https://bugs.openjdk.org/browse/JDK-8321603)|Bump minimum JDK version for JavaFX to JDK 21|build
[JDK-8322748](https://bugs.openjdk.org/browse/JDK-8322748)|Caret blinking in JavaFX should only stop when caret moves|controls
[JDK-8322964](https://bugs.openjdk.org/browse/JDK-8322964)|Optimize performance of CSS selector matching|graphics
[JDK-8324182](https://bugs.openjdk.org/browse/JDK-8324182)|Deprecate for removal SimpleSelector and CompoundSelector classes|graphics
[JDK-8325900](https://bugs.openjdk.org/browse/JDK-8325900)|Emit a warning on macOS if AWT has set the NSAppearance|graphics

## List of Fixed Bugs

Issue Key|Summary|Subcomponent
---------|-------|------------
[JDK-8309374](https://bugs.openjdk.org/browse/JDK-8309374)|Accessibility Focus Rectangle on ListItem is not drawn when ListView is shown for first time|accessibility
[JDK-8329705](https://bugs.openjdk.org/browse/JDK-8329705)|Add missing Application thread checks to platform specific a11y methods|accessibility
[JDK-8330462](https://bugs.openjdk.org/browse/JDK-8330462)|StringIndexOutOfBoundException when typing anything into TextField|accessibility
[JDK-8332748](https://bugs.openjdk.org/browse/JDK-8332748)|Grammatical errors in animation API docs|animation
[JDK-8271865](https://bugs.openjdk.org/browse/JDK-8271865)|SortedList::getViewIndex behaves not correctly for some index values|base
[JDK-8324797](https://bugs.openjdk.org/browse/JDK-8324797)|Code example in JavaDoc of ObservableValue#when doesn't compile|base
[JDK-8331616](https://bugs.openjdk.org/browse/JDK-8331616)|ChangeListener is not triggered when the InvalidationListener is removed|base
[JDK-8088923](https://bugs.openjdk.org/browse/JDK-8088923)|IOOBE when adding duplicate categories to the BarChart|controls
[JDK-8186188](https://bugs.openjdk.org/browse/JDK-8186188)|TableColumHeader: initial auto-size broken if has graphic|controls
[JDK-8193286](https://bugs.openjdk.org/browse/JDK-8193286)|IntegerSpinnerFactory does not wrap value correctly|controls
[JDK-8198830](https://bugs.openjdk.org/browse/JDK-8198830)|BarChart: auto-range of CategoryAxis not working on dynamically setting data|controls
[JDK-8242553](https://bugs.openjdk.org/browse/JDK-8242553)|IntegerSpinner and DoubleSpinner do not wrap around values correctly in some cases|controls
[JDK-8273349](https://bugs.openjdk.org/browse/JDK-8273349)|Check uses of Stream::peek in controls and replace as needed|controls
[JDK-8273657](https://bugs.openjdk.org/browse/JDK-8273657)|TextField: all text content must be selected initially|controls
[JDK-8279140](https://bugs.openjdk.org/browse/JDK-8279140)|ComboBox can lose selected value on item change via setAll|controls
[JDK-8301900](https://bugs.openjdk.org/browse/JDK-8301900)|TextArea: Committing text with ENTER in an IME window inserts newline|controls
[JDK-8307117](https://bugs.openjdk.org/browse/JDK-8307117)|TextArea: wrapText property ignored when changing font|controls
[JDK-8314754](https://bugs.openjdk.org/browse/JDK-8314754)|Minor ticks are not getting updated both the axes in LineChart|controls
[JDK-8319844](https://bugs.openjdk.org/browse/JDK-8319844)|Text/TextFlow.hitTest() is incorrect in RTL orientation|controls
[JDK-8323511](https://bugs.openjdk.org/browse/JDK-8323511)|Scrollbar Click jumps inconsistent amount of pixels|controls
[JDK-8323615](https://bugs.openjdk.org/browse/JDK-8323615)|PopupControl.skin.setSkin(Skin) fails to call dispose() on discarded Skin|controls
[JDK-8324327](https://bugs.openjdk.org/browse/JDK-8324327)|ColorPicker shows a white rectangle on clicking on picker|controls
[JDK-8324939](https://bugs.openjdk.org/browse/JDK-8324939)|Editable TableView loses focus after commit|controls
[JDK-8325154](https://bugs.openjdk.org/browse/JDK-8325154)|resizeColumnToFitContent is slower than it needs to be|controls
[JDK-8325402](https://bugs.openjdk.org/browse/JDK-8325402)|TreeTableRow updateItem() does not check item with isItemChanged(..)|controls
[JDK-8325798](https://bugs.openjdk.org/browse/JDK-8325798)|Spinner throws uncatchable exception on tab out from garbled text|controls
[JDK-8327727](https://bugs.openjdk.org/browse/JDK-8327727)|Changing the row factory of a TableView does not recreate the rows|controls
[JDK-8328577](https://bugs.openjdk.org/browse/JDK-8328577)|Toolbar's overflow button overlaps the items|controls
[JDK-8330304](https://bugs.openjdk.org/browse/JDK-8330304)|MenuBar: Invisible Menu works incorrectly with keyboard arrows|controls
[JDK-8330590](https://bugs.openjdk.org/browse/JDK-8330590)|TextInputControl: previous word fails with Bhojpuri characters|controls
[JDK-8331214](https://bugs.openjdk.org/browse/JDK-8331214)|Doc: update spec for SpinnerFactory classes|controls
[JDK-8334739](https://bugs.openjdk.org/browse/JDK-8334739)|XYChart and (Stacked)AreaChart properties return incorrect beans|controls
[JDK-8089373](https://bugs.openjdk.org/browse/JDK-8089373)|Translation from character to key code is not sufficient|graphics
[JDK-8260013](https://bugs.openjdk.org/browse/JDK-8260013)|Snapshot does not work for nodes in a subscene|graphics
[JDK-8289115](https://bugs.openjdk.org/browse/JDK-8289115)|Touch events is not dispatched after upgrade to JAVAFX17+|graphics
[JDK-8307980](https://bugs.openjdk.org/browse/JDK-8307980)|Rotate Transformation never invalidates inverseCache|graphics
[JDK-8311124](https://bugs.openjdk.org/browse/JDK-8311124)|[Windows] User installed font 8281327 fix does not work for all cases |graphics
[JDK-8311492](https://bugs.openjdk.org/browse/JDK-8311492)|FontSmoothingType LCD produces wrong color when transparency is used|graphics
[JDK-8312603](https://bugs.openjdk.org/browse/JDK-8312603)|ArrayIndexOutOfBoundsException in Marlin when scaleX is 0|graphics
[JDK-8314215](https://bugs.openjdk.org/browse/JDK-8314215)|Trailing Spaces before Line Breaks Affect the Center Alignment of Text|graphics
[JDK-8322251](https://bugs.openjdk.org/browse/JDK-8322251)|[Linux] JavaFX is not displaying CJK on Ubuntu 23.10 and later|graphics
[JDK-8322619](https://bugs.openjdk.org/browse/JDK-8322619)|Parts of SG no longer update during rendering - overlapping - culling - dirty|graphics
[JDK-8324233](https://bugs.openjdk.org/browse/JDK-8324233)|Update JPEG Image Decoding Software to 9f|graphics
[JDK-8331603](https://bugs.openjdk.org/browse/JDK-8331603)|Cleanup native AbstractSurface methods getRGBImpl, setRGBImpl|graphics
[JDK-8332251](https://bugs.openjdk.org/browse/JDK-8332251)|javadoc: incorrect method references in Region and PopupControl|graphics
[JDK-8332863](https://bugs.openjdk.org/browse/JDK-8332863)|Crash in JPEG decoder if we enable MEM_STATS|graphics
[JDK-8338478](https://bugs.openjdk.org/browse/JDK-8338478)|[macos] Crash in CoreText with certain strings using JDK 22 or later|graphics
[JDK-8320912](https://bugs.openjdk.org/browse/JDK-8320912)|IME should commit on focus change|localization
[JDK-8146918](https://bugs.openjdk.org/browse/JDK-8146918)|ConcurrentModificationException in MediaPlayer|media
[JDK-8308955](https://bugs.openjdk.org/browse/JDK-8308955)|MediaPlayer/AudioClip skip data on seek/loop|media
[JDK-8328603](https://bugs.openjdk.org/browse/JDK-8328603)|HLS video stream fails to render consistently|media
[JDK-8270996](https://bugs.openjdk.org/browse/JDK-8270996)|javadoc: missing comments in serialized classes|other
[JDK-8325073](https://bugs.openjdk.org/browse/JDK-8325073)|javadoc warnings: missing @param tags and other issues|other
[JDK-8087444](https://bugs.openjdk.org/browse/JDK-8087444)|CornerRadii with different horizontal and vertical values treated as uniform|scenegraph
[JDK-8090267](https://bugs.openjdk.org/browse/JDK-8090267)|JFXPanel Input Problem|swing
[JDK-8322784](https://bugs.openjdk.org/browse/JDK-8322784)|JFXPanel calls InputMethodRequests on wrong thread|swing
[JDK-8324239](https://bugs.openjdk.org/browse/JDK-8324239)|JFXPanelHiDPITest fails on Windows 11|swing
[JDK-8318614](https://bugs.openjdk.org/browse/JDK-8318614)|Update WebKit to 617.1|web
[JDK-8322703](https://bugs.openjdk.org/browse/JDK-8322703)|Intermittent crash in WebView in a JFXPanel from IME calls on macOS|web
[JDK-8323879](https://bugs.openjdk.org/browse/JDK-8323879)|constructor Path(Path) which takes another Path object fail to draw on canvas html|web
[JDK-8323880](https://bugs.openjdk.org/browse/JDK-8323880)|Caret rendered at wrong position in case of a click event on RTL text|web
[JDK-8324326](https://bugs.openjdk.org/browse/JDK-8324326)|Update ICU4C to 74.2|web
[JDK-8324337](https://bugs.openjdk.org/browse/JDK-8324337)|Cherry-pick WebKit 617.1 stabilization fixes|web
[JDK-8325258](https://bugs.openjdk.org/browse/JDK-8325258)|Additional WebKit 617.1 fixes from WebKitGTK 2.42.5|web
[JDK-8326989](https://bugs.openjdk.org/browse/JDK-8326989)|Text selection issues on WebView after WebKit 617.1|web
[JDK-8329011](https://bugs.openjdk.org/browse/JDK-8329011)|Update SQLite to 3.45.3|web
[JDK-8331748](https://bugs.openjdk.org/browse/JDK-8331748)|Update libxml2 to 2.12.6|web
[JDK-8331765](https://bugs.openjdk.org/browse/JDK-8331765)|Websocket callbacks are not executed after WebKit 617.1 update|web
[JDK-8332539](https://bugs.openjdk.org/browse/JDK-8332539)|Update libxml2 to 2.12.7|web
[JDK-8334713](https://bugs.openjdk.org/browse/JDK-8334713)|WebKit build failed on LoongArch64 because currentStackPointer is undefined|web
[JDK-8088172](https://bugs.openjdk.org/browse/JDK-8088172)|Mac: On German keyboard, pressing <+><q> inserts two apostrophes instead of one|window-toolkit
[JDK-8089803](https://bugs.openjdk.org/browse/JDK-8089803)|[Mac, TextArea] Japanese IME, caret moves to the next line when pressing Return to select a candidate|window-toolkit
[JDK-8299738](https://bugs.openjdk.org/browse/JDK-8299738)|ISE if Platform::exit called with fullScreen Stage on macOS 13|window-toolkit
[JDK-8320965](https://bugs.openjdk.org/browse/JDK-8320965)|Scrolling on a touch enabled display fails on Wayland |window-toolkit
[JDK-8324232](https://bugs.openjdk.org/browse/JDK-8324232)|KeyEvent.getCode() is null inside JFXPanel|window-toolkit
[JDK-8325591](https://bugs.openjdk.org/browse/JDK-8325591)|[Mac] DRAG_DONE reports null transferMode when destination is external|window-toolkit
[JDK-8326619](https://bugs.openjdk.org/browse/JDK-8326619)|Stage.sizeToScene() on maximized/fullscreen Stage breaks the Window|window-toolkit
[JDK-8326712](https://bugs.openjdk.org/browse/JDK-8326712)|Robot tests fail on XWayland|window-toolkit
[JDK-8327177](https://bugs.openjdk.org/browse/JDK-8327177)|macOS: wrong GlobalRef deleted in GlassMenu|window-toolkit
[JDK-8329821](https://bugs.openjdk.org/browse/JDK-8329821)|[Linux] When using i3 WM, menus are incorrectly sized|window-toolkit
[JDK-8335216](https://bugs.openjdk.org/browse/JDK-8335216)|[windows] Missing error check for GetSystemDirectory in glass|window-toolkit
[JDK-8335630](https://bugs.openjdk.org/browse/JDK-8335630)|Crash if Platform::exit called with fullScreen Stage on macOS 14|window-toolkit

## List of Security fixes

Issue Key|Summary|Subcomponent
---------|-------|------------
JDK-8313040 (not public)|Enhanced Font handling|graphics
JDK-8313064 (not public)|General enhancements of image handling|graphics
JDK-8313072 (not public)|Enhanced handling of Fonts|graphics
JDK-8322236 (not public)|Build failure after JDK-8313064|graphics
JDK-8313032 (not public)|Enhanced handling of Glass|window-toolkit
JDK-8320441 (not public)|Additonal fix for JDK-8313032|window-toolkit
