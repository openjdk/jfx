# Release Notes for JavaFX 25

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 25 release. JavaFX 25 requires JDK 23 or later. JDK 25 is recommended.

## Important Changes

### JavaFX 25 Requires JDK 23 or Later

JavaFX 25 is compiled with `--release 23` and thus requires JDK 23 or later in order to run. If you attempt to run with an older JDK, the Java launcher will exit with an error message indicating that the `javafx.base` module cannot be read.

See [JDK-8359387](https://bugs.openjdk.org/browse/JDK-8359387) for more information.

### JavaFX Requires GTK 3.20 or Later on Linux

On Linux platforms, JavaFX requires GTK3 version 3.20 or later. An exception will be thrown when initializing the JavaFX runtime if the GTK 3 library cannot be loaded or is older than 3.20.

See [JDK-8359396](https://bugs.openjdk.org/browse/JDK-8359396) for more information.

### JavaFX Applications No Longer Need `--sun-misc-unsafe-memory-access=allow`

It is no longer necessary to pass `--sun-misc-unsafe-memory-access=allow` to `java` on the command line when running JavaFX applications. The bug that formerly caused this option to be needed has been fixed.

See [JDK-8334137](https://bugs.openjdk.org/browse/JDK-8334137) for more information.

## Removed Features and Options

### A Deprecated `TransitionEvent` Constructor Has Been Removed

The `TransitionEvent(EventType, StyleableProperty, Duration)` constructor was deprecated for removal in JavaFX 24 and has now been removed. Application developers should use `TransitionEvent(EventType, StyleableProperty, String, Duration)` instead.

See [JDK-8353617](https://bugs.openjdk.org/browse/JDK-8353617) for more information.

## List of New Features

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8355774](https://bugs.openjdk.org/browse/JDK-8355774) | RichTextArea: provide mechanism for CSS styling of highlights | controls
[JDK-8357594](https://bugs.openjdk.org/browse/JDK-8357594) | Additional geometry-based Text/TextFlow APIs | controls
[JDK-8313424](https://bugs.openjdk.org/browse/JDK-8313424) | JavaFX controls in the title bar (Preview) | graphics
[JDK-8314482](https://bugs.openjdk.org/browse/JDK-8314482) | TextFlow: TabStopPolicy | graphics
[JDK-8345348](https://bugs.openjdk.org/browse/JDK-8345348) | CSS media feature queries | graphics
[JDK-8341670](https://bugs.openjdk.org/browse/JDK-8341670) | [Text,TextFlow] Public API for Text Layout Info | graphics

## List of Other Enhancements

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8313556](https://bugs.openjdk.org/browse/JDK-8313556) | Create implementation of NSAccessibilitySlider protocol | accessibility
[JDK-8313558](https://bugs.openjdk.org/browse/JDK-8313558) | Create implementation of NSAccessibilityStepper protocol | accessibility
[JDK-8350316](https://bugs.openjdk.org/browse/JDK-8350316) | Create implementation of NSAccessibilityProgressIndicator protocol | accessibility
[JDK-8351773](https://bugs.openjdk.org/browse/JDK-8351773) | Create implementation of NSAccessibilityGroup protocol | accessibility
[JDK-8356983](https://bugs.openjdk.org/browse/JDK-8356983) | Create implementation of NSAccessibilityImage protocol | accessibility
[JDK-8359257](https://bugs.openjdk.org/browse/JDK-8359257) | Create accessibility protocol for TabGroup component | accessibility
[JDK-8361379](https://bugs.openjdk.org/browse/JDK-8361379) | [macos] Refactor accessibility code to retrieve attribute by name | accessibility
[JDK-8359387](https://bugs.openjdk.org/browse/JDK-8359387) | Bump minimum JDK version for JavaFX to JDK 23 | build
[JDK-8335547](https://bugs.openjdk.org/browse/JDK-8335547) | Support multi-line prompt text for TextArea | controls
[JDK-8353617](https://bugs.openjdk.org/browse/JDK-8353617) | Remove deprecated TransitionEvent constructor | graphics
[JDK-8358255](https://bugs.openjdk.org/browse/JDK-8358255) | Factor out boilerplate code of EventHandler properties in Scene and Window | graphics
[JDK-8337960](https://bugs.openjdk.org/browse/JDK-8337960) | Improve performance of mfwrapper by reusing GStreamer media buffers for decoded video | media
[JDK-8349373](https://bugs.openjdk.org/browse/JDK-8349373) | Support JavaFX preview features | other
[JDK-8359396](https://bugs.openjdk.org/browse/JDK-8359396) | [Linux] Require Gtk3 >= 3.20 for glass-gtk | window-toolkit

See the API docs for a list of [new APIs](https://openjfx.io/javadoc/25/new-list.html) and [deprecated APIs](https://openjfx.io/javadoc/25/deprecated-list.html) in each release.

## List of Fixed Bugs

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8357157](https://bugs.openjdk.org/browse/JDK-8357157) | Exception thrown from AnimationTimer freezes application | animation
[JDK-8170720](https://bugs.openjdk.org/browse/JDK-8170720) | VetoableListDecorator: Indexes to remove are not aggregated | base
[JDK-8233179](https://bugs.openjdk.org/browse/JDK-8233179) | VetoableListDecorator#sort throws IllegalArgumentException "duplicate children" | base
[JDK-8347753](https://bugs.openjdk.org/browse/JDK-8347753) | VetoableListDecorator doesn't accept its own sublists for bulk operations | base
[JDK-8351038](https://bugs.openjdk.org/browse/JDK-8351038) | ConcurrentModificationException in EventType constructor | base
[JDK-8351276](https://bugs.openjdk.org/browse/JDK-8351276) | Prevent redundant computeValue calls when a chain of mappings becomes observed | base
[JDK-8358770](https://bugs.openjdk.org/browse/JDK-8358770) | incubator.richtext pom missing dependency on incubator.input | build
[JDK-8361713](https://bugs.openjdk.org/browse/JDK-8361713) | JavaFX API docs overview is missing an intro section | build
[JDK-8089080](https://bugs.openjdk.org/browse/JDK-8089080) | [TextArea] Caret disappear after pressing backspace to clear the content | controls
[JDK-8185887](https://bugs.openjdk.org/browse/JDK-8185887) | TableRowSkinBase fails to correctly virtualize cells in horizontal direction | controls
[JDK-8207333](https://bugs.openjdk.org/browse/JDK-8207333) | [Linux, macOS] Column sorting is triggered always after context menu request on table header | controls
[JDK-8252566](https://bugs.openjdk.org/browse/JDK-8252566) | TreeTableView: broken row layout for fixedCellSize | controls
[JDK-8276326](https://bugs.openjdk.org/browse/JDK-8276326) | Empty Cells in TableView supposedly after using setFixedCellSize() | controls
[JDK-8277000](https://bugs.openjdk.org/browse/JDK-8277000) | Tree-/TableRowSkin: replace listener to fixedCellSize by live lookup | controls
[JDK-8299753](https://bugs.openjdk.org/browse/JDK-8299753) | Tree/TableView: Column Resizing With Fractional Scale | controls
[JDK-8299755](https://bugs.openjdk.org/browse/JDK-8299755) | Tree/TableView: Cursor Decouples From Divider When Resizing With Fractional Scale | controls
[JDK-8333275](https://bugs.openjdk.org/browse/JDK-8333275) | ComboBox: adding an item from editor changes editor text | controls
[JDK-8335587](https://bugs.openjdk.org/browse/JDK-8335587) | TextInputControl: Binding prompt text that contains linebreak causes exception | controls
[JDK-8340344](https://bugs.openjdk.org/browse/JDK-8340344) | The first item in TreeView is not aligned in the beginning | controls
[JDK-8341281](https://bugs.openjdk.org/browse/JDK-8341281) | Root TreeItem with null value breaks TreeTableView | controls
[JDK-8346824](https://bugs.openjdk.org/browse/JDK-8346824) | Collapsing tree view causes rendering issues | controls
[JDK-8347392](https://bugs.openjdk.org/browse/JDK-8347392) | Thread-unsafe implementation of c.s.j.scene.control.skin.Utils | controls
[JDK-8348100](https://bugs.openjdk.org/browse/JDK-8348100) | Tooltips cannot be instantiated on background thread | controls
[JDK-8349091](https://bugs.openjdk.org/browse/JDK-8349091) | Charts: exception initializing in a background thread | controls
[JDK-8349098](https://bugs.openjdk.org/browse/JDK-8349098) | TabPane: exception initializing in a background thread | controls
[JDK-8349105](https://bugs.openjdk.org/browse/JDK-8349105) | Pagination: exception initializing in a background thread | controls
[JDK-8349255](https://bugs.openjdk.org/browse/JDK-8349255) | TitledPane: exception initializing in a background thread | controls
[JDK-8349756](https://bugs.openjdk.org/browse/JDK-8349756) | Memory leak in PaginationSkin when setting page count / index | controls
[JDK-8349758](https://bugs.openjdk.org/browse/JDK-8349758) | Memory leak in TreeTableView | controls
[JDK-8350976](https://bugs.openjdk.org/browse/JDK-8350976) | MenuBarSkin: exception initializing in a background thread | controls
[JDK-8351047](https://bugs.openjdk.org/browse/JDK-8351047) | TitledPane should handle titles that are resizable | controls
[JDK-8351368](https://bugs.openjdk.org/browse/JDK-8351368) | RichTextArea: exception pasting from Word | controls
[JDK-8351878](https://bugs.openjdk.org/browse/JDK-8351878) | RichTextArea: copy/paste issues | controls
[JDK-8355012](https://bugs.openjdk.org/browse/JDK-8355012) | JavaFX modena.css -fx-highlight-text-fill bug | controls
[JDK-8355415](https://bugs.openjdk.org/browse/JDK-8355415) | RichTextArea: NPE in VFlow::scrollCaretToVisible | controls
[JDK-8355615](https://bugs.openjdk.org/browse/JDK-8355615) | ConcurrentModificationException creating MenuBar on background thread | controls
[JDK-8357393](https://bugs.openjdk.org/browse/JDK-8357393) | RichTextArea: fails to properly save text attributes | controls
[JDK-8364049](https://bugs.openjdk.org/browse/JDK-8364049) | ToolBar shows overflow menu with fractional scale | controls
[JDK-8364088](https://bugs.openjdk.org/browse/JDK-8364088) | ToolBarSkin: NPE in select() | controls
[JDK-8281384](https://bugs.openjdk.org/browse/JDK-8281384) | Random chars on paste from Windows clipboard | graphics
[JDK-8318985](https://bugs.openjdk.org/browse/JDK-8318985) | [macos] Incorrect 3D lighting on macOS 14 and later | graphics
[JDK-8334137](https://bugs.openjdk.org/browse/JDK-8334137) | Marlin: replace sun.misc.Unsafe memory access methods with FFM | graphics
[JDK-8342530](https://bugs.openjdk.org/browse/JDK-8342530) | Specifying "@Nx" scaling level in ImageStorage should only load that specific level | graphics
[JDK-8349256](https://bugs.openjdk.org/browse/JDK-8349256) | Update PipeWire to 1.3.81 | graphics
[JDK-8350149](https://bugs.openjdk.org/browse/JDK-8350149) | VBox ignores bias of child controls when fillWidth is set to false | graphics
[JDK-8351067](https://bugs.openjdk.org/browse/JDK-8351067) | Enforce Platform threading use | graphics
[JDK-8351867](https://bugs.openjdk.org/browse/JDK-8351867) | No UI changes while iconified | graphics
[JDK-8353632](https://bugs.openjdk.org/browse/JDK-8353632) | [Linux] Undefined reference to PlatformSupport::OBSERVED_SETTINGS with C++14 | graphics
[JDK-8353845](https://bugs.openjdk.org/browse/JDK-8353845) | com.sun.javafx.css.BitSet.equals(null) throws NPE | graphics
[JDK-8354797](https://bugs.openjdk.org/browse/JDK-8354797) | Parent.needsLayoutProperty() should return read-only getter | graphics
[JDK-8354813](https://bugs.openjdk.org/browse/JDK-8354813) | Parent.isNeedsLayout() may return wrong value in property listener | graphics
[JDK-8357004](https://bugs.openjdk.org/browse/JDK-8357004) | Windows platform color changes are not picked up in some cases | graphics
[JDK-8358454](https://bugs.openjdk.org/browse/JDK-8358454) | Wrong \<br> tags in cssref.html  | graphics
[JDK-8329227](https://bugs.openjdk.org/browse/JDK-8329227) | Seek might hang with fMP4 H.265/HEVC or H.265/HEVC over HTTP/FILE | media
[JDK-8357714](https://bugs.openjdk.org/browse/JDK-8357714) | AudioClip.play crash on macOS when loading resource from jar | media
[JDK-8088343](https://bugs.openjdk.org/browse/JDK-8088343) | Race condition in javafx.concurrent.Task::cancel | other
[JDK-8350048](https://bugs.openjdk.org/browse/JDK-8350048) | Enforce threading restrictions for show and hide methods in Window, Control, and Skin | other
[JDK-8245602](https://bugs.openjdk.org/browse/JDK-8245602) | Ensemble8: HTMLEditor Toolbar gets scrolled out of view | samples
[JDK-8146479](https://bugs.openjdk.org/browse/JDK-8146479) | Scene is black after stage is restored (content changed while minimized) | scenegraph
[JDK-8340322](https://bugs.openjdk.org/browse/JDK-8340322) | Update WebKit to 620.1 | web
[JDK-8352162](https://bugs.openjdk.org/browse/JDK-8352162) | Update libxml2 to 2.13.8 | web
[JDK-8352164](https://bugs.openjdk.org/browse/JDK-8352164) | Update libxslt to 1.1.43 | web
[JDK-8354876](https://bugs.openjdk.org/browse/JDK-8354876) | Update SQLite to 3.49.1 | web
[JDK-8354940](https://bugs.openjdk.org/browse/JDK-8354940) | Fail to sign in to Microsoft sites with WebView | web
[JDK-8176813](https://bugs.openjdk.org/browse/JDK-8176813) | Mac: Failure to exit full-screen programmatically in some cases | window-toolkit
[JDK-8348095](https://bugs.openjdk.org/browse/JDK-8348095) | [Linux] Menu shows up in wrong position when using i3 windows manager in full screen mode | window-toolkit
[JDK-8351733](https://bugs.openjdk.org/browse/JDK-8351733) | Crash when creating too many nested event loops | window-toolkit
[JDK-8353314](https://bugs.openjdk.org/browse/JDK-8353314) | macOS: Inconsistent fullscreen behavior | window-toolkit
[JDK-8353548](https://bugs.openjdk.org/browse/JDK-8353548) | [macOS] DragEvent.getScreenY() returns incorrect value in secondary monitor | window-toolkit
[JDK-8354478](https://bugs.openjdk.org/browse/JDK-8354478) | Improve StageStyle documentation | window-toolkit
[JDK-8354631](https://bugs.openjdk.org/browse/JDK-8354631) | [macos] OpenURIHandler events not received by AWT when JavaFX is primary toolkit | window-toolkit
[JDK-8356652](https://bugs.openjdk.org/browse/JDK-8356652) | Input field ignores custom input source characters | window-toolkit
[JDK-8357584](https://bugs.openjdk.org/browse/JDK-8357584) | [XWayland] [OL10] Robot.mousePress() is delivered to wrong place | window-toolkit
[JDK-8363813](https://bugs.openjdk.org/browse/JDK-8363813) | Missing null check in GlassScreen | window-toolkit

## List of Security fixes

NONE
