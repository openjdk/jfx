# Release Notes for JavaFX 15

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 15 release. JavaFX 15 requires JDK 11 or later.


## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8241582](https://bugs.openjdk.java.net/browse/JDK-8241582)|Infinite animation does not start from the end when started with a negative rate|animation
[JDK-8240688](https://bugs.openjdk.java.net/browse/JDK-8240688)|Remove the JavaBeanXxxPropertyBuilders constructors|base
[JDK-8196586](https://bugs.openjdk.java.net/browse/JDK-8196586)|Remove use of deprecated finalize methods from javafx property objects|base
[JDK-8237469](https://bugs.openjdk.java.net/browse/JDK-8237469)|Inherited styles don't update when node is moved|controls
[JDK-8236840](https://bugs.openjdk.java.net/browse/JDK-8236840)|Memory leak when switching ButtonSkin|controls
[JDK-8237453](https://bugs.openjdk.java.net/browse/JDK-8237453)|[TabPane] Incorrect arrow key traversal through tabs after reordering|controls
[JDK-8236839](https://bugs.openjdk.java.net/browse/JDK-8236839)|System menubar removed when other menubars are created or modified|controls
[JDK-8236259](https://bugs.openjdk.java.net/browse/JDK-8236259)|MemoryLeak in ProgressIndicator|controls
[JDK-8230809](https://bugs.openjdk.java.net/browse/JDK-8230809)|HTMLEditor formatting lost when selecting all (CTRL-A)|controls
[JDK-8245499](https://bugs.openjdk.java.net/browse/JDK-8245499)|Text input controls should show handles on iOS|controls
[JDK-8245282](https://bugs.openjdk.java.net/browse/JDK-8245282)|Button/Combo Behavior: memory leak on dispose|controls
[JDK-8241249](https://bugs.openjdk.java.net/browse/JDK-8241249)|NPE in TabPaneSkin.perfromDrag|controls
[JDK-8241999](https://bugs.openjdk.java.net/browse/JDK-8241999)|ChoiceBox: incorrect toggle selected for uncontained selectedItem|controls
[JDK-8242001](https://bugs.openjdk.java.net/browse/JDK-8242001)|ChoiceBox: must update value on setting SelectionModel, part 2|controls
[JDK-8241737](https://bugs.openjdk.java.net/browse/JDK-8241737)|TabPaneSkin memory leak on replacing selectionModel|controls
[JDK-8242548](https://bugs.openjdk.java.net/browse/JDK-8242548)|Wrapped labeled controls using -fx-line-spacing cut text off|controls
[JDK-8242489](https://bugs.openjdk.java.net/browse/JDK-8242489)|ChoiceBox: initially toggle not sync'ed to selection |controls
[JDK-8242167](https://bugs.openjdk.java.net/browse/JDK-8242167)|ios keyboard handling|controls
[JDK-8242163](https://bugs.openjdk.java.net/browse/JDK-8242163)|Android keyboard integration fails|controls
[JDK-8089134](https://bugs.openjdk.java.net/browse/JDK-8089134)|[2D traversal, RTL] TraversalEngine only handles left/right key traversal correctly in RTL for top-level engine in ToolBar|controls
[JDK-8089828](https://bugs.openjdk.java.net/browse/JDK-8089828)|RTL Orientation, the flag of a mnemonic is not placed under the mnemonic letter.|controls
[JDK-8087555](https://bugs.openjdk.java.net/browse/JDK-8087555)|[ChoiceBox] uncontained value not shown|controls
[JDK-8244110](https://bugs.openjdk.java.net/browse/JDK-8244110)|NPE in MenuButtonSkinBase change listener|controls
[JDK-8244647](https://bugs.openjdk.java.net/browse/JDK-8244647)|Wrong first layout pass of Scrollbar controls on touch supported devices|controls
[JDK-8244421](https://bugs.openjdk.java.net/browse/JDK-8244421)|Wrong scrollbar position on touch enabled devices|controls
[JDK-8244657](https://bugs.openjdk.java.net/browse/JDK-8244657)|ChoiceBox/ToolBarSkin: misbehavior on switching skin|controls
[JDK-8241455](https://bugs.openjdk.java.net/browse/JDK-8241455)|Memory leak on replacing selection/focusModel|controls
[JDK-8241710](https://bugs.openjdk.java.net/browse/JDK-8241710)|NullPointerException while entering empty submenu with "arrow right"|controls
[JDK-8237926](https://bugs.openjdk.java.net/browse/JDK-8237926)|Potential memory leak of model data in javafx.scene.control.ListView|controls
[JDK-8235480](https://bugs.openjdk.java.net/browse/JDK-8235480)|Regression: [RTL] Arrow keys navigation doesn't respect TableView orientation|controls
[JDK-8175358](https://bugs.openjdk.java.net/browse/JDK-8175358)|Memory leak when moving MenuButton into another Scene|controls
[JDK-8198402](https://bugs.openjdk.java.net/browse/JDK-8198402)|ToggleButton.setToggleGroup causes memory leak when button is removed via ToggleGroup.getToggles() |controls
[JDK-8246195](https://bugs.openjdk.java.net/browse/JDK-8246195)|ListViewSkin/Behavior: misbehavior on switching skin|controls
[JDK-8245575](https://bugs.openjdk.java.net/browse/JDK-8245575)|Show the ContextMenu of input controls with long press gesture on iOS|controls
[JDK-8244418](https://bugs.openjdk.java.net/browse/JDK-8244418)|MenuBar: IOOB exception on requestFocus on empty bar|controls
[JDK-8176270](https://bugs.openjdk.java.net/browse/JDK-8176270)|Adding ChangeListener to TextField.selectedTextProperty causes StringOutOfBoundsException|controls
[JDK-8227619](https://bugs.openjdk.java.net/browse/JDK-8227619)|Potential memory leak in javafx.scene.control.ListView|controls
[JDK-8244824](https://bugs.openjdk.java.net/browse/JDK-8244824)|TableView : Incorrect German translation|controls
[JDK-8237602](https://bugs.openjdk.java.net/browse/JDK-8237602)|TabPane doesn't respect order of TabPane.getTabs() list|controls
[JDK-8193800](https://bugs.openjdk.java.net/browse/JDK-8193800)|TreeTableView selection changes on sorting|controls
[JDK-8244112](https://bugs.openjdk.java.net/browse/JDK-8244112)|Skin implementations: must not violate contract of dispose |controls
[JDK-8234959](https://bugs.openjdk.java.net/browse/JDK-8234959)|FXMLLoader does not populate ENGINE_SCOPE Bindings with FILENAME and ARGV|fxml
[JDK-8245456](https://bugs.openjdk.java.net/browse/JDK-8245456)|MacPasteboard throws ClassCastException on static builds|graphics
[JDK-8244735](https://bugs.openjdk.java.net/browse/JDK-8244735)|Error on iOS passing keys with unicode values greater than 255|graphics
[JDK-8242577](https://bugs.openjdk.java.net/browse/JDK-8242577)|Cell selection fails on iOS most of the times|graphics
[JDK-8243255](https://bugs.openjdk.java.net/browse/JDK-8243255)|Font size is large in JavaFX app with enabled Monocle on Raspberry Pi|graphics
[JDK-8157224](https://bugs.openjdk.java.net/browse/JDK-8157224)|isNPOTSupported check is too strict|graphics
[JDK-8240262](https://bugs.openjdk.java.net/browse/JDK-8240262)|iOS refresh rate is capped to 30 Hz|graphics
[JDK-8240265](https://bugs.openjdk.java.net/browse/JDK-8240265)|iOS: Unnecessary logging on pinch gestures |graphics
[JDK-8237770](https://bugs.openjdk.java.net/browse/JDK-8237770)|Error creating fragment phong shader on iOS|graphics
[JDK-8202296](https://bugs.openjdk.java.net/browse/JDK-8202296)|Monocle MouseInput doesn't send keyboard modifiers in events.|graphics
[JDK-8245635](https://bugs.openjdk.java.net/browse/JDK-8245635)|GlassPasteboard::getUTFs fails on iOS|graphics
[JDK-8240264](https://bugs.openjdk.java.net/browse/JDK-8240264)|iOS: Unnecessary logging on every pulse when GL context changes|graphics
[JDK-8201570](https://bugs.openjdk.java.net/browse/JDK-8201570)|Get two bytes for the Linux input event type, not four|graphics
[JDK-8241370](https://bugs.openjdk.java.net/browse/JDK-8241370)|Crash in JPEGImageLoader after fix for JDK-8212034|graphics
[JDK-8212034](https://bugs.openjdk.java.net/browse/JDK-8212034)|Potential memory leaks in jpegLoader.c in error case|graphics
[JDK-8237782](https://bugs.openjdk.java.net/browse/JDK-8237782)|Only read advances up to the minimum of the numHorMetrics or the available font data.|graphics
[JDK-8237833](https://bugs.openjdk.java.net/browse/JDK-8237833)|Check glyph size before adding to glyph texture cache.|graphics
[JDK-8239107](https://bugs.openjdk.java.net/browse/JDK-8239107)|Update libjpeg to version 9d|graphics
[JDK-8201567](https://bugs.openjdk.java.net/browse/JDK-8201567)|QuantumRenderer modifies buffer in use by JavaFX Application Thread|graphics
[JDK-8246348](https://bugs.openjdk.java.net/browse/JDK-8246348)|Crash in libpango on Ubuntu 20.04 with some unicode chars|graphics
[JDK-8246204](https://bugs.openjdk.java.net/browse/JDK-8246204)|No 3D support for newer Intel graphics drivers on Linux|graphics
[JDK-8242530](https://bugs.openjdk.java.net/browse/JDK-8242530)|[macos] Some audio files miss spectrum data when another audio file plays first|media
[JDK-8240694](https://bugs.openjdk.java.net/browse/JDK-8240694)|[macos 10.15] JavaFX Media hangs on some video files on Catalina|media
[JDK-8236832](https://bugs.openjdk.java.net/browse/JDK-8236832)|[macos 10.15] JavaFX Application hangs on video play on Catalina|media
[JDK-8239095](https://bugs.openjdk.java.net/browse/JDK-8239095)|Upgrade libFFI to the latest 3.3 version|media
[JDK-8250238](https://bugs.openjdk.java.net/browse/JDK-8250238)|Media fails to load libav 58 library when using modules from maven central|media
[JDK-8241629](https://bugs.openjdk.java.net/browse/JDK-8241629)|[macos10.15] Long startup delay playing media over https on Catalina|media
[JDK-8214699](https://bugs.openjdk.java.net/browse/JDK-8214699)|Node.getPseudoClassStates must return the same instance on every call|scenegraph
[JDK-8247163](https://bugs.openjdk.java.net/browse/JDK-8247163)|JFXPanel throws exception on click when no Scene is set|swing
[JDK-8220484](https://bugs.openjdk.java.net/browse/JDK-8220484)|JFXPanel renders a slanted image with a hidpi monitor scale of 125% or 175%|swing
[JDK-8239454](https://bugs.openjdk.java.net/browse/JDK-8239454)|LLIntData : invalid opcode returned for 16 and 32 bit wide instructions|web
[JDK-8239109](https://bugs.openjdk.java.net/browse/JDK-8239109)|Update SQLite to version 3.31.1|web
[JDK-8240211](https://bugs.openjdk.java.net/browse/JDK-8240211)|Stack overflow on Windows 32-bit can lead to crash|web
[JDK-8240218](https://bugs.openjdk.java.net/browse/JDK-8240218)|IOS Webkit implementation broken|web
[JDK-8238526](https://bugs.openjdk.java.net/browse/JDK-8238526)|Cherry pick GTK WebKit 2.26.3 changes|web
[JDK-8223298](https://bugs.openjdk.java.net/browse/JDK-8223298)|SVG patterns are drawn wrong|web
[JDK-8242209](https://bugs.openjdk.java.net/browse/JDK-8242209)|Increase web native thread stack size for x86 mode|web
[JDK-8233942](https://bugs.openjdk.java.net/browse/JDK-8233942)|Update to 609.1 version of WebKit|web
[JDK-8237889](https://bugs.openjdk.java.net/browse/JDK-8237889)|Update libxml2 to version 2.9.10|web
[JDK-8244579](https://bugs.openjdk.java.net/browse/JDK-8244579)|Windows "User Objects" leakage with WebView|web
[JDK-8208169](https://bugs.openjdk.java.net/browse/JDK-8208169)|can not print selected pages of web page|web
[JDK-8191758](https://bugs.openjdk.java.net/browse/JDK-8191758)|Match WebKit's font weight rendering with JavaFX|web
[JDK-8234471](https://bugs.openjdk.java.net/browse/JDK-8234471)|Canvas in webview displayed with wrong scale on Windows|web
[JDK-8247963](https://bugs.openjdk.java.net/browse/JDK-8247963)|Update SQLite to version 3.32.3|web
[JDK-8236971](https://bugs.openjdk.java.net/browse/JDK-8236971)|[macos] Gestures handled incorrectly due to missing events|window-toolkit
[JDK-8176499](https://bugs.openjdk.java.net/browse/JDK-8176499)|Dependence on java.util.Timer freezes screen when OS time resets backwards|window-toolkit
[JDK-8236685](https://bugs.openjdk.java.net/browse/JDK-8236685)|[macOs] Remove obsolete file dialog subclasses|window-toolkit
[JDK-8248381](https://bugs.openjdk.java.net/browse/JDK-8248381)|Create a daemon thread for MonocleTimer|window-toolkit
[JDK-8248490](https://bugs.openjdk.java.net/browse/JDK-8248490)|[macOS] Undecorated stage does not minimize|window-toolkit

## List of Enhancements

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8242523](https://bugs.openjdk.java.net/browse/JDK-8242523)|Update the Animation and ClipEnvelope classes|animation
[JDK-8240692](https://bugs.openjdk.java.net/browse/JDK-8240692)|Cleanup of the javafx property objects|base
[JDK-8244417](https://bugs.openjdk.java.net/browse/JDK-8244417)|support static build for Windows|build
[JDK-8238080](https://bugs.openjdk.java.net/browse/JDK-8238080)|FXMLLoader: if script engines implement javax.script.Compilable compile scripts|fxml
[JDK-8227425](https://bugs.openjdk.java.net/browse/JDK-8227425)|Add support for e-paper displays on i.MX6 devices|graphics
[JDK-8238954](https://bugs.openjdk.java.net/browse/JDK-8238954)|Improve performance of tiled snapshot rendering |graphics
[JDK-8238755](https://bugs.openjdk.java.net/browse/JDK-8238755)|allow to create static lib for javafx.media on linux|media
[JDK-8208761](https://bugs.openjdk.java.net/browse/JDK-8208761)|Update constant collections to use the new immutable collections|other
[JDK-8246357](https://bugs.openjdk.java.net/browse/JDK-8246357)|Allow static build of webkit library on linux|web

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8245422 (not public) | Better Pisces rasterizing | graphics
JDK-8241108 (not public) | Glib improvements | media
JDK-8236798 (not public) | Enhance FX scripting support | web

NOTE:

JavaScript programs that are run in the context of a web page loaded by WebEngine can communicate with Java objects passed from the application to the JavaScript program. JavaScript programs that reference java.lang.Class objects are now limited to the following methods:

```
getCanonicalName
getEnumConstants
getFields
getMethods
getName
getPackageName
getSimpleName
getSuperclass
getTypeName
getTypeParameters
isAssignableFrom
isArray
isEnum
isInstance
isInterface
isLocalClass
isMemberClass
isPrimitive
isSynthetic
toGenericString
toString
```

No methods can be called on the following classes:
```
java.lang.ClassLoader
java.lang.Module
java.lang.Runtime
java.lang.System

java.lang.invoke.*
java.lang.module.*
java.lang.reflect.*
java.security.*
sun.misc.*
```
