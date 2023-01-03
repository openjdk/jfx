# Release Notes for JavaFX 17

## Introduction

The following notes describe important changes and information about this release. In some cases, the descriptions provide links to additional detailed information about an issue or a change.

As of JDK 11 the JavaFX modules are delivered separately from the JDK. These release notes cover the standalone JavaFX 17 release. JavaFX 17 requires JDK 11 or later.

## Important Changes

### The src.zip file moved to the top directory of the JavaFX sdk

The `src.zip` file, which is delivered as part of the JavaFX SDK in support of IDEs, has moved from the `lib` directory to the top directory of the JavaFX SDK.
Application developers may need to adjust their IDE settings so that the IDE can locate the file in its new location.

## List of Enhancement

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8258777](https://bugs.openjdk.org/browse/JDK-8258777)|SkinBase: add api to un-/register invalidation-/listChange listeners|controls
[JDK-8267554](https://bugs.openjdk.org/browse/JDK-8267554)|Support loading stylesheets from data-URIs|controls
[JDK-8223717](https://bugs.openjdk.org/browse/JDK-8223717)|javafx printing: Support Specifying Print to File in the API|graphics
[JDK-8234920](https://bugs.openjdk.org/browse/JDK-8234920)|Add SpotLight to the selection of 3D light types|graphics
[JDK-8259718](https://bugs.openjdk.org/browse/JDK-8259718)|Remove the Marlin rasterizer (single-precision)|graphics
[JDK-8267551](https://bugs.openjdk.org/browse/JDK-8267551)|Support loading images from inline data-URIs|graphics
[JDK-8268120](https://bugs.openjdk.org/browse/JDK-8268120)|Allow hardware cursor to be used on Monocle-EGL platforms|graphics
[JDK-8258499](https://bugs.openjdk.org/browse/JDK-8258499)|JavaFX: Move src.zip out of the lib directory|other
[JDK-8252935](https://bugs.openjdk.org/browse/JDK-8252935)|Add treeShowing listener only when needed|scenegraph
[JDK-8259680](https://bugs.openjdk.org/browse/JDK-8259680)|Need API to query states of CAPS LOCK and NUM LOCK keys|scenegraph
[JDK-8092439](https://bugs.openjdk.org/browse/JDK-8092439)|[Monocle] Refactor monocle SPI to allow support for multiple screens|graphics

## List of Fixed Bugs

Issue key|Summary|Subcomponent
---------|-------|------------
[JDK-8185447](https://bugs.openjdk.org/browse/JDK-8185447)|The special high-contrast mode of JavaFX Controls in Japanese environment do not work.|accessibility
[JDK-8263322](https://bugs.openjdk.org/browse/JDK-8263322)|Calling Application.launch on FX thread should throw IllegalStateException, but causes deadlock|application-lifecycle
[JDK-8260468](https://bugs.openjdk.org/browse/JDK-8260468)|Wrong behavior of LocalDateTimeStringConverter|base
[JDK-8260475](https://bugs.openjdk.org/browse/JDK-8260475)|Deprecate for removal protected access members in DateTimeStringConverter|base
[JDK-8264770](https://bugs.openjdk.org/browse/JDK-8264770)|BidirectionalBinding should use InvalidationListener to prevent boxing|base
[JDK-8267505](https://bugs.openjdk.org/browse/JDK-8267505)|{List,Set,Map}PropertyBase::bind should check against identity|base
[JDK-8089589](https://bugs.openjdk.org/browse/JDK-8089589)|[ListView] ScrollBar content moves toward-backward during scrolling.|controls
[JDK-8089913](https://bugs.openjdk.org/browse/JDK-8089913)|CSS pseudo classes missing by default for some controls|controls
[JDK-8137323](https://bugs.openjdk.org/browse/JDK-8137323)|Incorrect parsing of mnemonic in controls text|controls
[JDK-8165214](https://bugs.openjdk.org/browse/JDK-8165214)|ListView.EditEvent.getIndex() does not return the correct index|controls
[JDK-8186904](https://bugs.openjdk.org/browse/JDK-8186904)|TableColumnHeader: resize cursor lost on right click|controls
[JDK-8187229](https://bugs.openjdk.org/browse/JDK-8187229)|Tree/TableCell: cancel event must return correct editing location|controls
[JDK-8189354](https://bugs.openjdk.org/browse/JDK-8189354)|Change.getRemoved() list contains incorrect selected items when a TreeItem is collapsed|controls
[JDK-8196065](https://bugs.openjdk.org/browse/JDK-8196065)|ListChangeListener getRemoved() returns items that were not removed.|controls
[JDK-8204568](https://bugs.openjdk.org/browse/JDK-8204568)|Relative CSS-Attributes don't work all time|controls
[JDK-8208088](https://bugs.openjdk.org/browse/JDK-8208088)|Memory Leak in ControlAcceleratorSupport|controls
[JDK-8228363](https://bugs.openjdk.org/browse/JDK-8228363)|ContextMenu.show with side=TOP does not work the first time in the presence of CSS|controls
[JDK-8239138](https://bugs.openjdk.org/browse/JDK-8239138)|StyleManager should use a BufferedInputStream|controls
[JDK-8244075](https://bugs.openjdk.org/browse/JDK-8244075)|Accelerator of ContextMenu's MenuItem is not removed when ContextMenu is removed from Scene|controls
[JDK-8252238](https://bugs.openjdk.org/browse/JDK-8252238)|TableView: Editable (pseudo-editable) cells should respect the row editability|controls
[JDK-8256283](https://bugs.openjdk.org/browse/JDK-8256283)|IndexOutOfBoundsException when sorting a TreeTableView|controls
[JDK-8258663](https://bugs.openjdk.org/browse/JDK-8258663)|Fixed size TableCells are not removed from sene graph when column is removed|controls
[JDK-8261460](https://bugs.openjdk.org/browse/JDK-8261460)|Incorrect CSS applied to ContextMenu on DialogPane|controls
[JDK-8261840](https://bugs.openjdk.org/browse/JDK-8261840)|Submenus close to screen borders are no longer repositioned|controls
[JDK-8263807](https://bugs.openjdk.org/browse/JDK-8263807)|Button types of a DialogPane are set twice, returns a wrong button|controls
[JDK-8264157](https://bugs.openjdk.org/browse/JDK-8264157)|Items of non-editable ComboBox cannot be selected using up/down keys|controls
[JDK-8264127](https://bugs.openjdk.org/browse/JDK-8264127)|ListCell editing status is true, when index changes while editing|controls
[JDK-8264677](https://bugs.openjdk.org/browse/JDK-8264677)|MemoryLeak: Progressindicator leaks, when treeShowing is false|controls
[JDK-8265206](https://bugs.openjdk.org/browse/JDK-8265206)|Tree-/TableCell: editing state not updated on cell re-use|controls
[JDK-8265210](https://bugs.openjdk.org/browse/JDK-8265210)|TreeCell: cell editing state not updated on cell re-use|controls
[JDK-8265669](https://bugs.openjdk.org/browse/JDK-8265669)|AccumCell should not be visible|controls
[JDK-8266539](https://bugs.openjdk.org/browse/JDK-8266539)|[TreeView]: Change.getRemoved() contains null item when deselecting a TreeItem|controls
[JDK-8266966](https://bugs.openjdk.org/browse/JDK-8266966)|Wrong CSS properties are applied to other nodes after fix for JDK-8204568|controls
[JDK-8267094](https://bugs.openjdk.org/browse/JDK-8267094)|TreeCell: cancelEvent must return correct editing location|controls
[JDK-8267392](https://bugs.openjdk.org/browse/JDK-8267392)|ENTER key press on editable TableView throws NPE|controls
[JDK-8269026](https://bugs.openjdk.org/browse/JDK-8269026)|PasswordField doesn't render bullet character on Android|controls
[JDK-8269136](https://bugs.openjdk.org/browse/JDK-8269136)|Tree/TablePosition: must not throw NPE on instantiating with null table|controls
[JDK-8270314](https://bugs.openjdk.org/browse/JDK-8270314)|TreeTableCell: inconsistent naming for tableRow and tableColumn property methods|controls
[JDK-8165749](https://bugs.openjdk.org/browse/JDK-8165749)|java.lang.RuntimeException: dndGesture.dragboard is null in dragDrop|graphics
[JDK-8210199](https://bugs.openjdk.org/browse/JDK-8210199)|[linux / macOS] fileChooser can't handle emojis|graphics
[JDK-8211362](https://bugs.openjdk.org/browse/JDK-8211362)|Restrict export of libjpeg symbols from libjavafx_iio.so|graphics
[JDK-8217955](https://bugs.openjdk.org/browse/JDK-8217955)|Problems with touch input and JavaFX 11|graphics
[JDK-8239589](https://bugs.openjdk.org/browse/JDK-8239589)|JavaFX UI will not repaint after reconnecting via Remote Desktop|graphics
[JDK-8252099](https://bugs.openjdk.org/browse/JDK-8252099)|JavaFX does not render Myanmar script correctly|graphics
[JDK-8258986](https://bugs.openjdk.org/browse/JDK-8258986)|getColor throws IOOBE when PixelReader reads the same pixel twice|graphics
[JDK-8259046](https://bugs.openjdk.org/browse/JDK-8259046)|ViewPainter.ROOT_PATHS holds reference to Scene causing memory leak|graphics
[JDK-8262396](https://bugs.openjdk.org/browse/JDK-8262396)|Update Mesa 3-D Headers to version 21.0.3|graphics
[JDK-8262802](https://bugs.openjdk.org/browse/JDK-8262802)|Wrong context origin coordinates when using EGL and HiDPI|graphics
[JDK-8263402](https://bugs.openjdk.org/browse/JDK-8263402)|MemoryLeak: Node hardreferences it's previous Parent after csslayout and getting removed from the scene|graphics
[JDK-8267160](https://bugs.openjdk.org/browse/JDK-8267160)|Monocle mouse never get ENTERED state|graphics
[JDK-8267314](https://bugs.openjdk.org/browse/JDK-8267314)|Loading some animated GIFs fails with ArrayIndexOutOfBoundsException: Index 4096 out of bounds for length 4096|graphics
[JDK-8259356](https://bugs.openjdk.org/browse/JDK-8259356)|MediaPlayer's seek freezes video|media
[JDK-8262365](https://bugs.openjdk.org/browse/JDK-8262365)|Update GStreamer to version 1.18.3|media
[JDK-8262366](https://bugs.openjdk.org/browse/JDK-8262366)|Update glib to version 2.66.7|media
[JDK-8264737](https://bugs.openjdk.org/browse/JDK-8264737)|JavaFX media stream stops playing after reconnecting via Remote Desktop|media
[JDK-8266860](https://bugs.openjdk.org/browse/JDK-8266860)|[macos] Incorrect duration reported for HLS live streams|media
[JDK-8267819](https://bugs.openjdk.org/browse/JDK-8267819)|CoInitialize/CoUninitialize should be called on same thread|media
[JDK-8268152](https://bugs.openjdk.org/browse/JDK-8268152)|gstmpegaudioparse does not provides timestamps for HLS MP3 streams|media
[JDK-8268219](https://bugs.openjdk.org/browse/JDK-8268219)|hlsprogressbuffer should provide PTS after GStreamer update|media
[JDK-8269147](https://bugs.openjdk.org/browse/JDK-8269147)|Update GStreamer to version 1.18.4|media
[JDK-8252783](https://bugs.openjdk.org/browse/JDK-8252783)|Remove the css Selector and ShapeConverter constructors|scenegraph
[JDK-8264162](https://bugs.openjdk.org/browse/JDK-8264162)|PickResult.toString() is missing the closing square bracket|scenegraph
[JDK-8264330](https://bugs.openjdk.org/browse/JDK-8264330)|Scene MouseHandler is referencing removed nodes|scenegraph
[JDK-8270246](https://bugs.openjdk.org/browse/JDK-8270246)|Deprecate for removal implementation methods in Scene|scenegraph
[JDK-8254836](https://bugs.openjdk.org/browse/JDK-8254836)|Cherry pick GTK WebKit 2.30.3 changes|web
[JDK-8259555](https://bugs.openjdk.org/browse/JDK-8259555)|Webkit crashes on Apple Silicon|web
[JDK-8259635](https://bugs.openjdk.org/browse/JDK-8259635)|Update to 610.2 version of WebKit|web
[JDK-8260163](https://bugs.openjdk.org/browse/JDK-8260163)|IrresponsiveScriptTest.testInfiniteLoopInScript unit test fails on Windows|web
[JDK-8260165](https://bugs.openjdk.org/browse/JDK-8260165)|CSSFilterTest.testCSSFilterRendering system test fails|web
[JDK-8260245](https://bugs.openjdk.org/browse/JDK-8260245)|Update ICU4C to version 68.2|web
[JDK-8260257](https://bugs.openjdk.org/browse/JDK-8260257)|[Linux] WebView no longer reacts to some mouse events|web
[JDK-8263788](https://bugs.openjdk.org/browse/JDK-8263788)|JavaFX application freezes completely after some time when using the WebView|web
[JDK-8264501](https://bugs.openjdk.org/browse/JDK-8264501)|UIWebView for iOS is deprecated|web
[JDK-8264990](https://bugs.openjdk.org/browse/JDK-8264990)|WebEngine crashes with segfault when not loaded through system classloader|web
[JDK-8269131](https://bugs.openjdk.org/browse/JDK-8269131)|Update libxml2 to version 2.9.12|web
[JDK-8206253](https://bugs.openjdk.org/browse/JDK-8206253)|No/Wrong scroll events from touch input in window mode|window-toolkit
[JDK-8231558](https://bugs.openjdk.org/browse/JDK-8231558)|[macos] Platform.exit causes assertion error on macOS 10.15 or later|window-toolkit
[JDK-8240640](https://bugs.openjdk.org/browse/JDK-8240640)|[macos] Wrong focus behaviour with multiple Alerts|window-toolkit
[JDK-8248126](https://bugs.openjdk.org/browse/JDK-8248126)|JavaFX ignores HiDPI scaling settings on some linux platforms|window-toolkit
[JDK-8249737](https://bugs.openjdk.org/browse/JDK-8249737)|java.lang.RuntimeException: Too many touch points reported|window-toolkit
[JDK-8258381](https://bugs.openjdk.org/browse/JDK-8258381)|[macos] Exception when input emoji using Chinese input method|window-toolkit
[JDK-8263169](https://bugs.openjdk.org/browse/JDK-8263169)|[macOS] JavaFX windows open as tabs when system preference for documents is set|window-toolkit
[JDK-8266743](https://bugs.openjdk.org/browse/JDK-8266743)|Crash on macOS 10.11 due to ignored @available 10.12 check|window-toolkit

## List of Security fixes

Issue key|Summary|Subcomponent
---------|-------|------------
JDK-8263112 (not public) | Enhance String Conclusions | graphics 
