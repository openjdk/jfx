# Release Notes for JavaFX 27

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 27 release. JavaFX 27 requires JDK 25 or later. JDK 27 is recommended.

## Important Changes

### JavaFX 27 Requires JDK 25 or Later

JavaFX 27 is compiled with `--release 25` and thus requires JDK 25 or later in order to run. If you attempt to run with an older JDK, the Java launcher will exit with an error message indicating that the `javafx.base` module cannot be read.

See [JDK-8376601](https://bugs.openjdk.org/browse/JDK-8376601) for more information.

### Metal Is the Default Rendering Pipeline on macOS

Metal is now the default rendering pipeline on macOS, replacing the OpenGL-based ES2 pipeline. Applications will benefit from a modern, faster and low power graphics API. If needed, applications can revert to the ES2 pipeline by setting the `-Dprism.order=es2` system property.

See [JDK-8373091](https://bugs.openjdk.org/browse/JDK-8373091) for more information.

## List of New Features

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8356042](https://bugs.openjdk.org/browse/JDK-8356042) ([CSR](https://bugs.openjdk.org/browse/JDK-8382308)) | RichTextArea: tab stops attributes | controls
[JDK-8366198](https://bugs.openjdk.org/browse/JDK-8366198) ([CSR](https://bugs.openjdk.org/browse/JDK-8388059)) | RichTextArea: embedded image, text background, wavy underline attributes | controls
[JDK-8364149](https://bugs.openjdk.org/browse/JDK-8364149) ([CSR](https://bugs.openjdk.org/browse/JDK-8374976)) | Conditional stylesheet imports | graphics
[JDK-8373091](https://bugs.openjdk.org/browse/JDK-8373091) ([CSR](https://bugs.openjdk.org/browse/JDK-8376147)) | Make Metal the default JavaFX rendering pipeline for macOS | graphics
[JDK-8374804](https://bugs.openjdk.org/browse/JDK-8374804) ([CSR](https://bugs.openjdk.org/browse/JDK-8384139)) | ConditionalFeature media queries | graphics
[JDK-8374822](https://bugs.openjdk.org/browse/JDK-8374822) ([CSR](https://bugs.openjdk.org/browse/JDK-8386998)) | Platform media query | graphics
[JDK-8386617](https://bugs.openjdk.org/browse/JDK-8386617) ([CSR](https://bugs.openjdk.org/browse/JDK-8387000)) | JavaFX controls in the title bar | graphics

## List of Other Enhancements

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8343133](https://bugs.openjdk.org/browse/JDK-8343133) | Create implementation of NSAccessibilityNavigableStaticText protocol | accessibility
[JDK-8379150](https://bugs.openjdk.org/browse/JDK-8379150) | [macos] [a11y] Create implementation of accessibility protocol for hyperlink | accessibility
[JDK-8376601](https://bugs.openjdk.org/browse/JDK-8376601) ([CSR](https://bugs.openjdk.org/browse/JDK-8376603)) | Bump minimum JDK version for JavaFX to JDK 25 | build
[JDK-8370902](https://bugs.openjdk.org/browse/JDK-8370902) | RichTextArea: migrate to new text APIs | controls
[JDK-8374809](https://bugs.openjdk.org/browse/JDK-8374809) | [RichTextArea] accessibility | controls
[JDK-8380933](https://bugs.openjdk.org/browse/JDK-8380933) | TableView sorting very slow when many cells selected | controls
[JDK-8385533](https://bugs.openjdk.org/browse/JDK-8385533) | Cell "focused" PseudoClass handling is unnecessary | controls
[JDK-8377904](https://bugs.openjdk.org/browse/JDK-8377904) | Replace Double.parseDouble() with CssNumberParser | graphics
[JDK-8378895](https://bugs.openjdk.org/browse/JDK-8378895) | Reduce object allocations in Renderer.getPeerInstance() | graphics
[JDK-8378970](https://bugs.openjdk.org/browse/JDK-8378970) | Don't use exceptions for flow control in CssParser.colorValueOfString() | graphics
[JDK-8385459](https://bugs.openjdk.org/browse/JDK-8385459) | Animations should respect reducedMotion preference | other
[JDK-8389371](https://bugs.openjdk.org/browse/JDK-8389371) | cssref: explain font properties | other
[JDK-8377427](https://bugs.openjdk.org/browse/JDK-8377427) | Reduce substring allocations in Color.web(String, double) | scenegraph
[JDK-8358823](https://bugs.openjdk.org/browse/JDK-8358823) | Improve documentation of custom header bars | window-toolkit
[JDK-8374630](https://bugs.openjdk.org/browse/JDK-8374630) | Replace GtkFileChooserDialog with GtkFileChooserNative to allow for better Flatpak integration | window-toolkit
[JDK-8377350](https://bugs.openjdk.org/browse/JDK-8377350) | [iOS] Add support for UIWindowScene | window-toolkit

See the API docs for a list of [new APIs](https://openjfx.io/javadoc/27/new-list.html) and [deprecated APIs](https://openjfx.io/javadoc/27/deprecated-list.html) in each release.

## List of Fixed Bugs

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8195614](https://bugs.openjdk.org/browse/JDK-8195614) | FilteredList throws ArrayIndexOutOfBoundsException if created with 1 element | base
[JDK-8195750](https://bugs.openjdk.org/browse/JDK-8195750) | FilteredList throws ArrayIndexOutOfBoundsException on ListChangeEvent with multiple change | base
[JDK-8208758](https://bugs.openjdk.org/browse/JDK-8208758) | ListChangeBuilder doesn't shift indices after idempotent change pairs | base
[JDK-8237868](https://bugs.openjdk.org/browse/JDK-8237868) | java.lang.IndexOutOfBoundsException in FilteredList | base
[JDK-8359020](https://bugs.openjdk.org/browse/JDK-8359020) | TabObservableList.reorder changes content of filtered list | base
[JDK-8387655](https://bugs.openjdk.org/browse/JDK-8387655) | java.lang.NullPointerException: Cannot invoke "java.lang.Boolean.booleanValue()" because "newValue" is null | base
[JDK-8181411](https://bugs.openjdk.org/browse/JDK-8181411) | Performance problem with TreeTableView selectAll() | controls
[JDK-8202066](https://bugs.openjdk.org/browse/JDK-8202066) | ListView with large number of rows is extremly slow when changing current multiple selection to a subset  | controls
[JDK-8256142](https://bugs.openjdk.org/browse/JDK-8256142) | TreeView: initial focused item incorrect | controls
[JDK-8274928](https://bugs.openjdk.org/browse/JDK-8274928) | JavaFX virtual keyboard missing ^ character | controls
[JDK-8301283](https://bugs.openjdk.org/browse/JDK-8301283) | Util methods for computing text/width height giving up some performance | controls
[JDK-8303060](https://bugs.openjdk.org/browse/JDK-8303060) | ChoiceBox: adding ~1000 items takes a long time | controls
[JDK-8311505](https://bugs.openjdk.org/browse/JDK-8311505) | Deselection of TableView rows is slow | controls
[JDK-8338145](https://bugs.openjdk.org/browse/JDK-8338145) | ComboBox popup is in wrong location on first showing | controls
[JDK-8351094](https://bugs.openjdk.org/browse/JDK-8351094) | macOS: MenuBar with custom menu items disappears | controls
[JDK-8365938](https://bugs.openjdk.org/browse/JDK-8365938) ([CSR](https://bugs.openjdk.org/browse/JDK-8379663)) | FileChooser: initialDirectory must be a valid folder | controls
[JDK-8372398](https://bugs.openjdk.org/browse/JDK-8372398) | [macOS] Shown ContextMenu does not close when macOS system menu is clicked | controls
[JDK-8375444](https://bugs.openjdk.org/browse/JDK-8375444) | MenuButton - textTruncated Property is always false | controls
[JDK-8376492](https://bugs.openjdk.org/browse/JDK-8376492) | NullPointer in ContextMenu sub-menu when graphic and style classes are changed while the menu is open | controls
[JDK-8377393](https://bugs.openjdk.org/browse/JDK-8377393) | RichTextArea: failed to export null color attribute value | controls
[JDK-8379662](https://bugs.openjdk.org/browse/JDK-8379662) | TreeTableView: initial focused item incorrect | controls
[JDK-8380216](https://bugs.openjdk.org/browse/JDK-8380216) | [macos] Missing dispatch_release in the GlassAplication class | controls
[JDK-8380308](https://bugs.openjdk.org/browse/JDK-8380308) | TreeView: selection of many rows is slow | controls
[JDK-8380926](https://bugs.openjdk.org/browse/JDK-8380926) | HeavyweightDialog does not check for a valid stage | controls
[JDK-8380935](https://bugs.openjdk.org/browse/JDK-8380935) | TextFieldSkin caret width calculation is broken | controls
[JDK-8384006](https://bugs.openjdk.org/browse/JDK-8384006) | ComboBox text does not update on String converter update | controls
[JDK-8384806](https://bugs.openjdk.org/browse/JDK-8384806) | ComboBox converter does not properly update contained null values | controls
[JDK-8385666](https://bugs.openjdk.org/browse/JDK-8385666) | Fix for JDK-8384806 broke combo box cell graphics | controls
[JDK-8385959](https://bugs.openjdk.org/browse/JDK-8385959) | Prompt text should be visible in empty focused TextInputControls | controls
[JDK-8154847](https://bugs.openjdk.org/browse/JDK-8154847) | Rendering is incorrect or not visible with StageStyle.UNIFIED on some graphics cards | graphics
[JDK-8369348](https://bugs.openjdk.org/browse/JDK-8369348) | Failed assertion with scissor rect when Metal API Validation is enabled | graphics
[JDK-8373688](https://bugs.openjdk.org/browse/JDK-8373688) | Wrong render scale is used if Window is on another screen when Scene is sized | graphics
[JDK-8375070](https://bugs.openjdk.org/browse/JDK-8375070) | NPE in Scene.ClickGenerator::preProcess when mouse button is none | graphics
[JDK-8375227](https://bugs.openjdk.org/browse/JDK-8375227) | Silent OOBE in NGGroup::renderContent | graphics
[JDK-8375363](https://bugs.openjdk.org/browse/JDK-8375363) | StyleHelper.resetToInitialValues() interrupts animated properties | graphics
[JDK-8375561](https://bugs.openjdk.org/browse/JDK-8375561) | Class NGGroup is in need of some cleanup | graphics
[JDK-8377153](https://bugs.openjdk.org/browse/JDK-8377153) | JavaFX FlowPane layout causing improper wrapping of TextFlow nodes with max-width styling | graphics
[JDK-8379209](https://bugs.openjdk.org/browse/JDK-8379209) | Uninitialised variable in pathApplierFunctionFast of coretext.c | graphics
[JDK-8379211](https://bugs.openjdk.org/browse/JDK-8379211) | Uninitialised memory in Java_com_sun_javafx_font_freetype_OSFreetype_FT_1Outline_1Decompose | graphics
[JDK-8379257](https://bugs.openjdk.org/browse/JDK-8379257) | Update JPEG Image Decoding Software to 10 | graphics
[JDK-8382883](https://bugs.openjdk.org/browse/JDK-8382883) | JavaFX on Linux crashed after some time when ES2Context.makeCurrent() was called | graphics
[JDK-8383783](https://bugs.openjdk.org/browse/JDK-8383783) | MediaQueryParser should reject query lists without comma separator | graphics
[JDK-8386590](https://bugs.openjdk.org/browse/JDK-8386590) | GridPane - content-biased child spanning multiple columns/rows is sized ignoring hgap/vgap | graphics
[JDK-8387337](https://bugs.openjdk.org/browse/JDK-8387337) | GlassClipboard: Replace deprecated hash_map and hash_set | graphics
[JDK-8378510](https://bugs.openjdk.org/browse/JDK-8378510) | Provide media support for libavcodec version 62 | media
[JDK-8379206](https://bugs.openjdk.org/browse/JDK-8379206) | 4 Null pointer dereference defect groups in 4 glib files | media
[JDK-8379210](https://bugs.openjdk.org/browse/JDK-8379210) | Null pointer dereference in gst_value_compare_fraction of gstvalue.c | media
[JDK-8379213](https://bugs.openjdk.org/browse/JDK-8379213) | 3 Null pointer dereference defect groups in dlmalloc.c | media
[JDK-8379561](https://bugs.openjdk.org/browse/JDK-8379561) | Remove UNICODE related code from GLib | media
[JDK-8381447](https://bugs.openjdk.org/browse/JDK-8381447) | Remove G_DISABLE_CHECKS compiler flag on Windows to align GLib/GStreamer compilation with macOS/Linux | media
[JDK-8384809](https://bugs.openjdk.org/browse/JDK-8384809) | Update GStreamer to 1.28.3 | media
[JDK-8375016](https://bugs.openjdk.org/browse/JDK-8375016) | Several catch blocks for NullPointerExceptions exist in the codebase | other
[JDK-8388526](https://bugs.openjdk.org/browse/JDK-8388526) | [Windows] Crash in Platform.Preferences refresh on OS theme change when using FXCanvas | other
[JDK-8384814](https://bugs.openjdk.org/browse/JDK-8384814) | [macOS] Trackpad scroll gestures suddenly stop | scenegraph
[JDK-8389093](https://bugs.openjdk.org/browse/JDK-8389093) | Typos in the JavaFX CSS Reference Guide | scenegraph
[JDK-8341852](https://bugs.openjdk.org/browse/JDK-8341852) | Fix potential threading issue in WebView's RTImage | web
[JDK-8364680](https://bugs.openjdk.org/browse/JDK-8364680) | HelloWebView demo crashes when msn webpage is scrolled | web
[JDK-8368572](https://bugs.openjdk.org/browse/JDK-8368572) | Update WebKit to 623.1 | web
[JDK-8375084](https://bugs.openjdk.org/browse/JDK-8375084) | Update libxslt to 1.1.45 | web
[JDK-8377099](https://bugs.openjdk.org/browse/JDK-8377099) | Additional WebKit 623.1 fixes from WebKitGTK 2.50.4 | web
[JDK-8377930](https://bugs.openjdk.org/browse/JDK-8377930) | Additional WebKit 623.1 fixes from WebKitGTK 2.50.5 | web
[JDK-8378226](https://bugs.openjdk.org/browse/JDK-8378226) | Animated GIFs do not animate after WebKit 620.1 update | web
[JDK-8379336](https://bugs.openjdk.org/browse/JDK-8379336) | Update libxml2 to 2.15.2 | web
[JDK-8380557](https://bugs.openjdk.org/browse/JDK-8380557) | Additional WebKit 623.1 fixes from WebKitGTK 2.50.6 | web
[JDK-8384522](https://bugs.openjdk.org/browse/JDK-8384522) | Update libxml2 to 2.15.3 | web
[JDK-8263959](https://bugs.openjdk.org/browse/JDK-8263959) | Unexpected disable behaviour on macOS MenuBar | window-toolkit
[JDK-8335541](https://bugs.openjdk.org/browse/JDK-8335541) | [macOS] Help system menu won't hide or it is shown twice | window-toolkit
[JDK-8352298](https://bugs.openjdk.org/browse/JDK-8352298) | FileChooser: inconsistent behavior with setInitialName() | window-toolkit
[JDK-8377316](https://bugs.openjdk.org/browse/JDK-8377316) | [iOS] App crashes due to GlassThreadDataKey not initialized | window-toolkit
[JDK-8377706](https://bugs.openjdk.org/browse/JDK-8377706) | [iOS] UI API called on a background thread: UIView layer | window-toolkit
[JDK-8381517](https://bugs.openjdk.org/browse/JDK-8381517) | GlassViewDelegate::convertNSStringToJString can return uninitialized value | window-toolkit
[JDK-8382165](https://bugs.openjdk.org/browse/JDK-8382165) | Emojis used in Stage.setTitle are not rendered and break application switching on Linux platform | window-toolkit
[JDK-8387626](https://bugs.openjdk.org/browse/JDK-8387626) | java.lang.NullPointerException: Cannot invoke "com.sun.glass.ui.Window.isMinimized()" because "this.platformWindow" is null | window-toolkit

## List of Security fixes

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
JDK-8373527 (not public) | Improve Graphics playback | graphics
JDK-8379207 (not public) | Improve audio conversion | media
JDK-8383129 (not public) | Better Handling of MP4 Files | media
JDK-8383143 (not public) | Enhance Playlist Loading | media
JDK-8378277 (not public) | Improve Editor selection | web
JDK-8383092 (not public) | Enhance WebView Resource Loading | web
