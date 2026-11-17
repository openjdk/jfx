# Release Notes for JavaFX 26

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

These release notes cover the standalone JavaFX 26 release. JavaFX 26 requires JDK 24 or later. JDK 26 is recommended.

## Important Changes

### JavaFX 26 Requires JDK 24 or Later

JavaFX 26 is compiled with `--release 24` and thus requires JDK 24 or later in order to run. If you attempt to run with an older JDK, the Java launcher will exit with an error message indicating that the `javafx.base` module cannot be read.

See [JDK-8365402](https://bugs.openjdk.org/browse/JDK-8365402) for more information.

### Owned Windows No Longer Move When Owner Moves on macOS

On macOS, an owned (child) window used to move along with its owner, whenever the owner window was moved. This behavior caused the owned window to disappear when moved to a secondary screen. The former behavior of owned windows was also inconsistent with the behavior on Linux and Windows platforms. This fix makes the behavior of owned windows consistent across platforms.

See [JDK-8252373](https://bugs.openjdk.org/browse/JDK-8252373) for more information.

## Removed Features and Options

### The FXPermission Class Has Been Removed

The FXPermission class was terminally deprecated in JavaFX 24 and has now been removed. Following the removal of support for the Java Security Manager from JavaFX, all uses of the FXPermission class were removed. As such, the class serves no purpose anymore and has been removed from JavaFX.

See [JDK-8359759](https://bugs.openjdk.org/browse/JDK-8359759) for more information.

## List of New Features

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8372203](https://bugs.openjdk.org/browse/JDK-8372203) | Piecewise linear easing function | animation
[JDK-8091429](https://bugs.openjdk.org/browse/JDK-8091429) | ObservableList&lt;E&gt;#replaceRange(int from, int to, Collection&lt;? extends E&gt; col) | base
[JDK-8370446](https://bugs.openjdk.org/browse/JDK-8370446) | Support dialogs with StageStyle.EXTENDED (Preview) | controls
[JDK-8371070](https://bugs.openjdk.org/browse/JDK-8371070) | RichParagraph enhancements | controls
[JDK-8374035](https://bugs.openjdk.org/browse/JDK-8374035) | RichTextArea: add insertStyles property | controls
[JDK-8271024](https://bugs.openjdk.org/browse/JDK-8271024) | Implement macOS Metal Rendering Pipeline | graphics
[JDK-8358450](https://bugs.openjdk.org/browse/JDK-8358450) | Viewport characteristics media features | graphics
[JDK-8361286](https://bugs.openjdk.org/browse/JDK-8361286) | Allow enabling of background loading for images loaded from an InputStream | graphics
[JDK-8365635](https://bugs.openjdk.org/browse/JDK-8365635) | Add MOUSE_DRAG_DONE event type | graphics
[JDK-8369836](https://bugs.openjdk.org/browse/JDK-8369836) | Update HeaderBar API | graphics
[JDK-8324941](https://bugs.openjdk.org/browse/JDK-8324941) | POC for Headless platform for JavaFX | window-toolkit

## List of Other Enhancements

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8358092](https://bugs.openjdk.org/browse/JDK-8358092) | Create accessibility protocol implementation that covers various type of menu items | accessibility
[JDK-8368972](https://bugs.openjdk.org/browse/JDK-8368972) | Create implementation of menu bar accessibility component | accessibility
[JDK-8358820](https://bugs.openjdk.org/browse/JDK-8358820) | Allow interpolation outside of range [0,1] | animation
[JDK-8373038](https://bugs.openjdk.org/browse/JDK-8373038) | Interpolator factories should follow method naming convention | animation
[JDK-8359759](https://bugs.openjdk.org/browse/JDK-8359759) | Remove the FXPermission class | base
[JDK-8365402](https://bugs.openjdk.org/browse/JDK-8365402) | Bump minimum JDK version for JavaFX to JDK 24 | build
[JDK-8359599](https://bugs.openjdk.org/browse/JDK-8359599) | Calling refresh() for all virtualized controls recreates all cells instead of refreshing the cells | controls
[JDK-8366201](https://bugs.openjdk.org/browse/JDK-8366201) | RichTextArea: remove allowUndo parameter | controls
[JDK-8368478](https://bugs.openjdk.org/browse/JDK-8368478) | RichTextArea: add IME support | controls
[JDK-8370140](https://bugs.openjdk.org/browse/JDK-8370140) | RichTextArea: line endings | controls
[JDK-8371183](https://bugs.openjdk.org/browse/JDK-8371183) | RichTextModel: ContentChange.isEdit incorrect undoing style modification | controls
[JDK-8374347](https://bugs.openjdk.org/browse/JDK-8374347) | StyleAttributeMap.Builder.setParagraphDirection() | controls
[JDK-8092379](https://bugs.openjdk.org/browse/JDK-8092379) | GridPane should not render extra gaps when entire rows or columns are unmanaged | scenegraph
[JDK-8341560](https://bugs.openjdk.org/browse/JDK-8341560) | Better documentation for KeyCombinations/KeyCodes/KeyEvents | scenegraph
[JDK-8375243](https://bugs.openjdk.org/browse/JDK-8375243) | Improve javafx.scene.layout package documentation | scenegraph
[JDK-8362091](https://bugs.openjdk.org/browse/JDK-8362091) | Window title bar should reflect scene color scheme | window-toolkit

See the API docs for a list of [new APIs](https://openjfx.io/javadoc/26/new-list.html) and [deprecated APIs](https://openjfx.io/javadoc/26/deprecated-list.html) in each release.

## List of Fixed Bugs

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
[JDK-8374329](https://bugs.openjdk.org/browse/JDK-8374329) | PasswordField ignores custom accessible text | accessibility
[JDK-8378366](https://bugs.openjdk.org/browse/JDK-8378366) | Accessibility issues in CSS Reference Guide and Introduction to FXML | accessibility
[JDK-8378507](https://bugs.openjdk.org/browse/JDK-8378507) | JavaFX CSS Reference skips heading levels: &lt;h2&gt; followed by &lt;h4&gt; | accessibility
[JDK-8184166](https://bugs.openjdk.org/browse/JDK-8184166) | SortedList does not free up memory | base
[JDK-8373885](https://bugs.openjdk.org/browse/JDK-8373885) | Compilation failure due to a warning when compiling with Java 25 | build
[JDK-8296653](https://bugs.openjdk.org/browse/JDK-8296653) | ComboBox promptText is not displayed when the value is reset | controls
[JDK-8318095](https://bugs.openjdk.org/browse/JDK-8318095) | TextArea/TextFlow: wrong layout in RTL mode | controls
[JDK-8322486](https://bugs.openjdk.org/browse/JDK-8322486) | ColorPicker: blurry popup | controls
[JDK-8341286](https://bugs.openjdk.org/browse/JDK-8341286) | TreeView: visual artifacts when setting new root with null value | controls
[JDK-8356770](https://bugs.openjdk.org/browse/JDK-8356770) | TreeTableView not updated after removing a TreeItem with children and adding it to another parent | controls
[JDK-8359154](https://bugs.openjdk.org/browse/JDK-8359154) | Intermittent system test failures on macOS | controls
[JDK-8364777](https://bugs.openjdk.org/browse/JDK-8364777) | RichTextArea: cursor over scrollbar | controls
[JDK-8366202](https://bugs.openjdk.org/browse/JDK-8366202) | RichTextArea: wrong style used for typed text | controls
[JDK-8366739](https://bugs.openjdk.org/browse/JDK-8366739) | ToolBar: overflow menu with fractional scale (2) | controls
[JDK-8367602](https://bugs.openjdk.org/browse/JDK-8367602) | Regression: TabPane with wrapped label calculates wrong initial size | controls
[JDK-8369085](https://bugs.openjdk.org/browse/JDK-8369085) | RichTextArea SELECT_PARAGRAPH to include line separator | controls
[JDK-8370253](https://bugs.openjdk.org/browse/JDK-8370253) | CodeArea: NPE on copy | controls
[JDK-8370652](https://bugs.openjdk.org/browse/JDK-8370652) | Control and ScrollPaneSkin should snap computed width/height values to prevent scrollbars<br>appearing due to rounding errors | controls
[JDK-8371067](https://bugs.openjdk.org/browse/JDK-8371067) | RichTextArea: requestLayout by inline node doesn't reach VFlow | controls
[JDK-8371069](https://bugs.openjdk.org/browse/JDK-8371069) | RichTextArea: caret with inline node | controls
[JDK-8371080](https://bugs.openjdk.org/browse/JDK-8371080) | RichTextArea: missing styles in Caspian | controls
[JDK-8371859](https://bugs.openjdk.org/browse/JDK-8371859) | Dialog unnecessarily invokes DialogPane.requestLayout() | controls
[JDK-8371981](https://bugs.openjdk.org/browse/JDK-8371981) | Wrong split caret positioning with mixed text | controls
[JDK-8372298](https://bugs.openjdk.org/browse/JDK-8372298) | RichTextArea: missing API: applyParagraphStyle | controls
[JDK-8372438](https://bugs.openjdk.org/browse/JDK-8372438) | SimpleViewOnlyStyledModel: non-text paragraphs | controls
[JDK-8373098](https://bugs.openjdk.org/browse/JDK-8373098) | RichTextArea: NPE in InputMethodRequests | controls
[JDK-8373193](https://bugs.openjdk.org/browse/JDK-8373193) | RichTextArea: exceptions specifying position beyond the document end | controls
[JDK-8373908](https://bugs.openjdk.org/browse/JDK-8373908) | XYChart (ScatteredChart) leaks memory when removing data | controls
[JDK-8374208](https://bugs.openjdk.org/browse/JDK-8374208) | Remove superfluous field in XYChart | controls
[JDK-8374909](https://bugs.openjdk.org/browse/JDK-8374909) | CodeArea: Exception in IME | controls
[JDK-8376138](https://bugs.openjdk.org/browse/JDK-8376138) | RichTextArea: getModelStyleAttrs might return wrong attributes | controls
[JDK-8330559](https://bugs.openjdk.org/browse/JDK-8330559) | Trailing space not rendering correctly in TextFlow in RTL mode | graphics
[JDK-8335748](https://bugs.openjdk.org/browse/JDK-8335748) | Rippling of frame on scrolling | graphics
[JDK-8351357](https://bugs.openjdk.org/browse/JDK-8351357) | Add canary system test checking if Stage receives focus on show | graphics
[JDK-8358130](https://bugs.openjdk.org/browse/JDK-8358130) | [Metal] Merge shader compilation tasks | graphics
[JDK-8359108](https://bugs.openjdk.org/browse/JDK-8359108) | Mac - When Swing starts First, native application menu doesn't work for JavaFX | graphics
[JDK-8367557](https://bugs.openjdk.org/browse/JDK-8367557) | Extended stage seems to hang after drag and drop | graphics
[JDK-8367991](https://bugs.openjdk.org/browse/JDK-8367991) | Update RegionBackground tests to use ScreenCaptureTestWatcher utility | graphics
[JDK-8368166](https://bugs.openjdk.org/browse/JDK-8368166) | Media query should accept multiple rules | graphics
[JDK-8368508](https://bugs.openjdk.org/browse/JDK-8368508) | Missing info in JavaFX CSS Reference Guide | graphics
[JDK-8368511](https://bugs.openjdk.org/browse/JDK-8368511) | Incorrect "Group" name after click on link in JavaFX CSS Reference Guide | graphics
[JDK-8368629](https://bugs.openjdk.org/browse/JDK-8368629) | Texture.update sometimes invoked for a disposed Texture | graphics
[JDK-8368631](https://bugs.openjdk.org/browse/JDK-8368631) | Avoid updating disposed MTLTexture | graphics
[JDK-8368879](https://bugs.openjdk.org/browse/JDK-8368879) | Intermittent crash on exit when disposing MTLRTTextureData | graphics
[JDK-8369306](https://bugs.openjdk.org/browse/JDK-8369306) | Implement invokeAndWait and finishTerminating on headless platform | graphics
[JDK-8370912](https://bugs.openjdk.org/browse/JDK-8370912) | Formatting error on JavaDoc because of missing quotation mark | graphics
[JDK-8372275](https://bugs.openjdk.org/browse/JDK-8372275) | NPE in D3DResourceFactory.createPresentable | graphics
[JDK-8375466](https://bugs.openjdk.org/browse/JDK-8375466) | Metal rendering pipeline crashes on virtualized OS | graphics
[JDK-8361648](https://bugs.openjdk.org/browse/JDK-8361648) | Update Glib to 2.84.3 | media
[JDK-8366217](https://bugs.openjdk.org/browse/JDK-8366217) | Update GStreamer to 1.26.5 | media
[JDK-8371052](https://bugs.openjdk.org/browse/JDK-8371052) | Update libFFI to 3.5.2 | media
[JDK-8373058](https://bugs.openjdk.org/browse/JDK-8373058) | Media crash when playing MP3 over JRT protocol on macOS 26 | media
[JDK-8364405](https://bugs.openjdk.org/browse/JDK-8364405) | Intermittent MenuDoubleShortcutTest failure on Linux and macOS | other
[JDK-8368219](https://bugs.openjdk.org/browse/JDK-8368219) | Skip MenuDoubleShortcutTest on macOS & Linux | other
[JDK-8368375](https://bugs.openjdk.org/browse/JDK-8368375) | Skip intermittently failing SWTCursorsTest | other
[JDK-8360940](https://bugs.openjdk.org/browse/JDK-8360940) | Layout stops updating when using Parent#setNeedsLayout(true) due to incorrect state<br>management | scenegraph
[JDK-8372761](https://bugs.openjdk.org/browse/JDK-8372761) | Prevent degenerate transforms for zero-size Scene/SubScene | scenegraph
[JDK-8374515](https://bugs.openjdk.org/browse/JDK-8374515) | javafx.scene.PropertyHelper causes uncessary silent NullPointerException | scenegraph
[JDK-8255248](https://bugs.openjdk.org/browse/JDK-8255248) | NullPointerException in JFXPanel due to race condition in HostContainer | swing
[JDK-8334593](https://bugs.openjdk.org/browse/JDK-8334593) | Adding, removing and then adding a JFXPanel again leads to NullPointerException | swing
[JDK-8371128](https://bugs.openjdk.org/browse/JDK-8371128) | NullPointerException occurs due to double cleanup of SwingNode | swing
[JDK-8371386](https://bugs.openjdk.org/browse/JDK-8371386) | Fix potential NPE in SwingNode | swing
[JDK-8328684](https://bugs.openjdk.org/browse/JDK-8328684) | HellowWebView demo crashes when a webpage is scrolled | web
[JDK-8356982](https://bugs.openjdk.org/browse/JDK-8356982) | Update WebKit to 622.1 | web
[JDK-8360270](https://bugs.openjdk.org/browse/JDK-8360270) | Websocket communication issues with Vaadin applications through webview | web
[JDK-8361644](https://bugs.openjdk.org/browse/JDK-8361644) | Update ICU4C to 77.1 | web
[JDK-8366744](https://bugs.openjdk.org/browse/JDK-8366744) | Update SQLite to 3.50.4 | web
[JDK-8367578](https://bugs.openjdk.org/browse/JDK-8367578) | Additional WebKit 622.1 fixes from WebKitGTK 2.48.7 | web
[JDK-8368691](https://bugs.openjdk.org/browse/JDK-8368691) | Update libxml2 to 2.14.6 | web
[JDK-8370235](https://bugs.openjdk.org/browse/JDK-8370235) | WebKit build fails on Windows 32-bit and Linux 32-bit after JDK-8367578 | web
[JDK-8370632](https://bugs.openjdk.org/browse/JDK-8370632) | Additional libxslt 1.1.43 fixes | web
[JDK-8252373](https://bugs.openjdk.org/browse/JDK-8252373) | [macOS] Stage with owner disappears when moved to another screen | window-toolkit
[JDK-8269630](https://bugs.openjdk.org/browse/JDK-8269630) | Bad clipboard data causes JVM to crash | window-toolkit
[JDK-8326428](https://bugs.openjdk.org/browse/JDK-8326428) | [Linux] UI scaling factor cannot be fractional when using KDE | window-toolkit
[JDK-8346281](https://bugs.openjdk.org/browse/JDK-8346281) | [Windows] RenderScale doesn't update to HiDPI changes  | window-toolkit
[JDK-8350479](https://bugs.openjdk.org/browse/JDK-8350479) | SW pipeline should use default pipeline in Glass | window-toolkit
[JDK-8355990](https://bugs.openjdk.org/browse/JDK-8355990) | [macOS] Restoring a maximized stage does not update the window size | window-toolkit
[JDK-8360886](https://bugs.openjdk.org/browse/JDK-8360886) | Cmd + plus shortcut does not work reliably | window-toolkit
[JDK-8364547](https://bugs.openjdk.org/browse/JDK-8364547) | Window size may be incorrect when constrained to min or max | window-toolkit
[JDK-8364687](https://bugs.openjdk.org/browse/JDK-8364687) | Enable headless with -Dglass.platform=headless | window-toolkit
[JDK-8366986](https://bugs.openjdk.org/browse/JDK-8366986) | [Win] Incorrect position and size after hiding iconified stage | window-toolkit
[JDK-8367045](https://bugs.openjdk.org/browse/JDK-8367045) | [Linux] Dead keys not working | window-toolkit
[JDK-8367370](https://bugs.openjdk.org/browse/JDK-8367370) | Accent color platform preference not updating in macOS 26 (Tahoe) | window-toolkit
[JDK-8368021](https://bugs.openjdk.org/browse/JDK-8368021) | Window buttons of extended RTL stage are on the wrong side | window-toolkit
[JDK-8371106](https://bugs.openjdk.org/browse/JDK-8371106) | [macOS] Min/max window height is incorrect for EXTENDED StageStyle | window-toolkit
[JDK-8371302](https://bugs.openjdk.org/browse/JDK-8371302) | [Windows] Stage coordinates in secondary display are not properly updated after changing<br>settings of first display | window-toolkit

## List of Security fixes

Issue Key | Summary | Subcomponent
--------- | ------- | ------------
JDK-8361719 (not public) | Enhance Handling of URIs | application-lifecycle
JDK-8362535 (not public) | Update libxslt support | web
JDK-8368704 (not public) | Better glyph handling | web
