/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * GENERATED FILE -- do not edit. Regenerate with:
 *
 *   perl buildtools/ffm-web/gen-wkj-constants.pl \
 *        --java modules/javafx.web/src/main/java \
 *        --native modules/javafx.web/src/main/native \
 *        --out <this file>
 *
 * The 315 constants the WebKit C++ shares with the Java side.
 *
 * These used to arrive through 23 separate `com_sun_webkit_*.h` headers emitted by
 * `javac -h`. Nothing in this repository runs `javac -h` -- modules/javafx.web/pom.xml
 * has no -h argument and no CMake file invokes it -- so none of those headers exists
 * here and a from-source WebKit build needed an out-of-band step to produce them.
 * Generating one checked-in header from the same Java sources removes that dependency
 * and, with it, the last reason for this C++ to know anything about JNI name mangling.
 *
 * The names are kept in their original mangled spelling so that the change to each
 * call site is an include swap and nothing else. Each value is followed by the Java
 * declaration it was read from, so a reviewer can check any one of them by eye.
 */

#ifndef WKJ_CONSTANTS_H
#define WKJ_CONSTANTS_H


/* --- com_sun_webkit_ContextMenuItem --- */
#define com_sun_webkit_ContextMenuItem_ACTION_TYPE                     0            /* com/sun/webkit/ContextMenuItem.java:31 */
#define com_sun_webkit_ContextMenuItem_SEPARATOR_TYPE                  1            /* com/sun/webkit/ContextMenuItem.java:32 */
#define com_sun_webkit_ContextMenuItem_SUBMENU_TYPE                    2            /* com/sun/webkit/ContextMenuItem.java:33 */

/* --- com_sun_webkit_CursorManager --- */
#define com_sun_webkit_CursorManager_ALIAS                             36           /* com/sun/webkit/CursorManager.java:73 */
#define com_sun_webkit_CursorManager_CELL                              31           /* com/sun/webkit/CursorManager.java:68 */
#define com_sun_webkit_CursorManager_COLUMN_RESIZE                     19           /* com/sun/webkit/CursorManager.java:56 */
#define com_sun_webkit_CursorManager_CONTEXT_MENU                      32           /* com/sun/webkit/CursorManager.java:69 */
#define com_sun_webkit_CursorManager_COPY                              39           /* com/sun/webkit/CursorManager.java:76 */
#define com_sun_webkit_CursorManager_CROSS                             1            /* com/sun/webkit/CursorManager.java:38 */
#define com_sun_webkit_CursorManager_EAST_PANNING                      22           /* com/sun/webkit/CursorManager.java:59 */
#define com_sun_webkit_CursorManager_EAST_RESIZE                       7            /* com/sun/webkit/CursorManager.java:44 */
#define com_sun_webkit_CursorManager_EAST_WEST_RESIZE                  16           /* com/sun/webkit/CursorManager.java:53 */
#define com_sun_webkit_CursorManager_GRAB                              41           /* com/sun/webkit/CursorManager.java:78 */
#define com_sun_webkit_CursorManager_GRABBING                          42           /* com/sun/webkit/CursorManager.java:79 */
#define com_sun_webkit_CursorManager_HAND                              2            /* com/sun/webkit/CursorManager.java:39 */
#define com_sun_webkit_CursorManager_HELP                              6            /* com/sun/webkit/CursorManager.java:43 */
#define com_sun_webkit_CursorManager_MIDDLE_PANNING                    21           /* com/sun/webkit/CursorManager.java:58 */
#define com_sun_webkit_CursorManager_MOVE                              3            /* com/sun/webkit/CursorManager.java:40 */
#define com_sun_webkit_CursorManager_NONE                              40           /* com/sun/webkit/CursorManager.java:77 */
#define com_sun_webkit_CursorManager_NORTH_EAST_PANNING                24           /* com/sun/webkit/CursorManager.java:61 */
#define com_sun_webkit_CursorManager_NORTH_EAST_RESIZE                 9            /* com/sun/webkit/CursorManager.java:46 */
#define com_sun_webkit_CursorManager_NORTH_EAST_SOUTH_WEST_RESIZE      17           /* com/sun/webkit/CursorManager.java:54 */
#define com_sun_webkit_CursorManager_NORTH_PANNING                     23           /* com/sun/webkit/CursorManager.java:60 */
#define com_sun_webkit_CursorManager_NORTH_RESIZE                      8            /* com/sun/webkit/CursorManager.java:45 */
#define com_sun_webkit_CursorManager_NORTH_SOUTH_RESIZE                15           /* com/sun/webkit/CursorManager.java:52 */
#define com_sun_webkit_CursorManager_NORTH_WEST_PANNING                25           /* com/sun/webkit/CursorManager.java:62 */
#define com_sun_webkit_CursorManager_NORTH_WEST_RESIZE                 10           /* com/sun/webkit/CursorManager.java:47 */
#define com_sun_webkit_CursorManager_NORTH_WEST_SOUTH_EAST_RESIZE      18           /* com/sun/webkit/CursorManager.java:55 */
#define com_sun_webkit_CursorManager_NOT_ALLOWED                       34           /* com/sun/webkit/CursorManager.java:71 */
#define com_sun_webkit_CursorManager_NO_DROP                           33           /* com/sun/webkit/CursorManager.java:70 */
#define com_sun_webkit_CursorManager_POINTER                           0            /* com/sun/webkit/CursorManager.java:37 */
#define com_sun_webkit_CursorManager_PROGRESS                          35           /* com/sun/webkit/CursorManager.java:72 */
#define com_sun_webkit_CursorManager_ROW_RESIZE                        20           /* com/sun/webkit/CursorManager.java:57 */
#define com_sun_webkit_CursorManager_SOUTH_EAST_PANNING                27           /* com/sun/webkit/CursorManager.java:64 */
#define com_sun_webkit_CursorManager_SOUTH_EAST_RESIZE                 12           /* com/sun/webkit/CursorManager.java:49 */
#define com_sun_webkit_CursorManager_SOUTH_PANNING                     26           /* com/sun/webkit/CursorManager.java:63 */
#define com_sun_webkit_CursorManager_SOUTH_RESIZE                      11           /* com/sun/webkit/CursorManager.java:48 */
#define com_sun_webkit_CursorManager_SOUTH_WEST_PANNING                28           /* com/sun/webkit/CursorManager.java:65 */
#define com_sun_webkit_CursorManager_SOUTH_WEST_RESIZE                 13           /* com/sun/webkit/CursorManager.java:50 */
#define com_sun_webkit_CursorManager_TEXT                              4            /* com/sun/webkit/CursorManager.java:41 */
#define com_sun_webkit_CursorManager_VERTICAL_TEXT                     30           /* com/sun/webkit/CursorManager.java:67 */
#define com_sun_webkit_CursorManager_WAIT                              5            /* com/sun/webkit/CursorManager.java:42 */
#define com_sun_webkit_CursorManager_WEST_PANNING                      29           /* com/sun/webkit/CursorManager.java:66 */
#define com_sun_webkit_CursorManager_WEST_RESIZE                       14           /* com/sun/webkit/CursorManager.java:51 */
#define com_sun_webkit_CursorManager_ZOOM_IN                           37           /* com/sun/webkit/CursorManager.java:74 */
#define com_sun_webkit_CursorManager_ZOOM_OUT                          38           /* com/sun/webkit/CursorManager.java:75 */

/* --- com_sun_webkit_FileSystem --- */
#define com_sun_webkit_FileSystem_TYPE_DIRECTORY                       2            /* com/sun/webkit/FileSystem.java:45 */
#define com_sun_webkit_FileSystem_TYPE_FILE                            1            /* com/sun/webkit/FileSystem.java:44 */
#define com_sun_webkit_FileSystem_TYPE_UNKNOWN                         0            /* com/sun/webkit/FileSystem.java:43 */

/* --- com_sun_webkit_LoadListenerClient --- */
#define com_sun_webkit_LoadListenerClient_CONNECTION_REFUSED           4            /* com/sun/webkit/LoadListenerClient.java:65 */
#define com_sun_webkit_LoadListenerClient_CONNECTION_RESET             5            /* com/sun/webkit/LoadListenerClient.java:69 */
#define com_sun_webkit_LoadListenerClient_CONNECTION_TIMED_OUT         7            /* com/sun/webkit/LoadListenerClient.java:77 */
#define com_sun_webkit_LoadListenerClient_CONTENTTYPE_RECEIVED         13           /* com/sun/webkit/LoadListenerClient.java:40 */
#define com_sun_webkit_LoadListenerClient_CONTENT_RECEIVED             10           /* com/sun/webkit/LoadListenerClient.java:37 */
#define com_sun_webkit_LoadListenerClient_DOCUMENT_AVAILABLE           14           /* com/sun/webkit/LoadListenerClient.java:41 */
#define com_sun_webkit_LoadListenerClient_FILE_NOT_FOUND               11           /* com/sun/webkit/LoadListenerClient.java:95 */
#define com_sun_webkit_LoadListenerClient_ICON_RECEIVED                12           /* com/sun/webkit/LoadListenerClient.java:39 */
#define com_sun_webkit_LoadListenerClient_INVALID_RESPONSE             9            /* com/sun/webkit/LoadListenerClient.java:86 */
#define com_sun_webkit_LoadListenerClient_LOAD_FAILED                  5            /* com/sun/webkit/LoadListenerClient.java:35 */
#define com_sun_webkit_LoadListenerClient_LOAD_STOPPED                 6            /* com/sun/webkit/LoadListenerClient.java:36 */
#define com_sun_webkit_LoadListenerClient_MALFORMED_URL                2            /* com/sun/webkit/LoadListenerClient.java:57 */
#define com_sun_webkit_LoadListenerClient_NO_ROUTE_TO_HOST             6            /* com/sun/webkit/LoadListenerClient.java:73 */
#define com_sun_webkit_LoadListenerClient_PAGE_FINISHED                1            /* com/sun/webkit/LoadListenerClient.java:32 */
#define com_sun_webkit_LoadListenerClient_PAGE_REDIRECTED              2            /* com/sun/webkit/LoadListenerClient.java:33 */
#define com_sun_webkit_LoadListenerClient_PAGE_REPLACED                3            /* com/sun/webkit/LoadListenerClient.java:34 */
#define com_sun_webkit_LoadListenerClient_PAGE_STARTED                 0            /* com/sun/webkit/LoadListenerClient.java:31 */
#define com_sun_webkit_LoadListenerClient_PERMISSION_DENIED            8            /* com/sun/webkit/LoadListenerClient.java:82 */
#define com_sun_webkit_LoadListenerClient_PROGRESS_CHANGED             30           /* com/sun/webkit/LoadListenerClient.java:46 */
#define com_sun_webkit_LoadListenerClient_RESOURCE_FAILED              23           /* com/sun/webkit/LoadListenerClient.java:45 */
#define com_sun_webkit_LoadListenerClient_RESOURCE_FINISHED            22           /* com/sun/webkit/LoadListenerClient.java:44 */
#define com_sun_webkit_LoadListenerClient_RESOURCE_REDIRECTED          21           /* com/sun/webkit/LoadListenerClient.java:43 */
#define com_sun_webkit_LoadListenerClient_RESOURCE_STARTED             20           /* com/sun/webkit/LoadListenerClient.java:42 */
#define com_sun_webkit_LoadListenerClient_SSL_HANDSHAKE                3            /* com/sun/webkit/LoadListenerClient.java:61 */
#define com_sun_webkit_LoadListenerClient_TITLE_RECEIVED               11           /* com/sun/webkit/LoadListenerClient.java:38 */
#define com_sun_webkit_LoadListenerClient_TOO_MANY_REDIRECTS           10           /* com/sun/webkit/LoadListenerClient.java:91 */
#define com_sun_webkit_LoadListenerClient_UNKNOWN_ERROR                99           /* com/sun/webkit/LoadListenerClient.java:99 */
#define com_sun_webkit_LoadListenerClient_UNKNOWN_HOST                 1            /* com/sun/webkit/LoadListenerClient.java:53 */

/* --- com_sun_webkit_WebKitNative --- */
#define com_sun_webkit_WebKitNative_WKJ_ABI_VERSION                    1            /* com/sun/webkit/WebKitNative.java:71 */
#define com_sun_webkit_WebKitNative_WKJ_EXC_DOM                        1            /* com/sun/webkit/WebKitNative.java:90 */
#define com_sun_webkit_WebKitNative_WKJ_EXC_EVENT                      2            /* com/sun/webkit/WebKitNative.java:91 */
#define com_sun_webkit_WebKitNative_WKJ_EXC_NONE                       0            /* com/sun/webkit/WebKitNative.java:89 */
#define com_sun_webkit_WebKitNative_WKJ_EXC_RANGE                      3            /* com/sun/webkit/WebKitNative.java:92 */
#define com_sun_webkit_WebKitNative_WKJ_EXC_UNDEFINED                  4            /* com/sun/webkit/WebKitNative.java:93 */

/* --- com_sun_webkit_WebPage --- */
#define com_sun_webkit_WebPage_DEFAULT_BACKGROUND_INT_RGBA             4294967295   /* com/sun/webkit/WebPage.java:84 */
#define com_sun_webkit_WebPage_DND_DST_CHANGE                          2            /* com/sun/webkit/WebPage.java:893 */
#define com_sun_webkit_WebPage_DND_DST_DROP                            4            /* com/sun/webkit/WebPage.java:895 */
#define com_sun_webkit_WebPage_DND_DST_ENTER                           0            /* com/sun/webkit/WebPage.java:891 */
#define com_sun_webkit_WebPage_DND_DST_EXIT                            3            /* com/sun/webkit/WebPage.java:894 */
#define com_sun_webkit_WebPage_DND_DST_OVER                            1            /* com/sun/webkit/WebPage.java:892 */
#define com_sun_webkit_WebPage_DND_SRC_CHANGE                          102          /* com/sun/webkit/WebPage.java:899 */
#define com_sun_webkit_WebPage_DND_SRC_DROP                            104          /* com/sun/webkit/WebPage.java:901 */
#define com_sun_webkit_WebPage_DND_SRC_ENTER                           100          /* com/sun/webkit/WebPage.java:897 */
#define com_sun_webkit_WebPage_DND_SRC_EXIT                            103          /* com/sun/webkit/WebPage.java:900 */
#define com_sun_webkit_WebPage_DND_SRC_OVER                            101          /* com/sun/webkit/WebPage.java:898 */
#define com_sun_webkit_WebPage_MAX_FRAME_QUEUE_SIZE                    10           /* com/sun/webkit/WebPage.java:83 */

/* --- com_sun_webkit_dom_JSObject --- */
#define com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT                  0            /* com/sun/webkit/dom/JSObject.java:36 */
#define com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT                 1            /* com/sun/webkit/dom/JSObject.java:37 */
#define com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT               2            /* com/sun/webkit/dom/JSObject.java:38 */

/* --- com_sun_webkit_event_WCFocusEvent --- */
#define com_sun_webkit_event_WCFocusEvent_BACKWARD                     1            /* com/sun/webkit/event/WCFocusEvent.java:41 */
#define com_sun_webkit_event_WCFocusEvent_FOCUS_GAINED                 2            /* com/sun/webkit/event/WCFocusEvent.java:35 */
#define com_sun_webkit_event_WCFocusEvent_FOCUS_LOST                   3            /* com/sun/webkit/event/WCFocusEvent.java:36 */
#define com_sun_webkit_event_WCFocusEvent_FORWARD                      0            /* com/sun/webkit/event/WCFocusEvent.java:40 */
#define com_sun_webkit_event_WCFocusEvent_UNKNOWN                      -1           /* com/sun/webkit/event/WCFocusEvent.java:39 */
#define com_sun_webkit_event_WCFocusEvent_WINDOW_ACTIVATED             0            /* com/sun/webkit/event/WCFocusEvent.java:33 */
#define com_sun_webkit_event_WCFocusEvent_WINDOW_DEACTIVATED           1            /* com/sun/webkit/event/WCFocusEvent.java:34 */

/* --- com_sun_webkit_event_WCInputMethodEvent --- */
#define com_sun_webkit_event_WCInputMethodEvent_CARET_POSITION_CHANGED 1            /* com/sun/webkit/event/WCInputMethodEvent.java:33 */
#define com_sun_webkit_event_WCInputMethodEvent_INPUT_METHOD_TEXT_CHANGED 0            /* com/sun/webkit/event/WCInputMethodEvent.java:32 */

/* --- com_sun_webkit_event_WCKeyEvent --- */
#define com_sun_webkit_event_WCKeyEvent_KEY_PRESSED                    1            /* com/sun/webkit/event/WCKeyEvent.java:34 */
#define com_sun_webkit_event_WCKeyEvent_KEY_RELEASED                   2            /* com/sun/webkit/event/WCKeyEvent.java:35 */
#define com_sun_webkit_event_WCKeyEvent_KEY_TYPED                      0            /* com/sun/webkit/event/WCKeyEvent.java:33 */
#define com_sun_webkit_event_WCKeyEvent_VK_BACK                        8            /* com/sun/webkit/event/WCKeyEvent.java:38 */
#define com_sun_webkit_event_WCKeyEvent_VK_DELETE                      46           /* com/sun/webkit/event/WCKeyEvent.java:51 */
#define com_sun_webkit_event_WCKeyEvent_VK_DOWN                        40           /* com/sun/webkit/event/WCKeyEvent.java:49 */
#define com_sun_webkit_event_WCKeyEvent_VK_END                         35           /* com/sun/webkit/event/WCKeyEvent.java:44 */
#define com_sun_webkit_event_WCKeyEvent_VK_ESCAPE                      27           /* com/sun/webkit/event/WCKeyEvent.java:41 */
#define com_sun_webkit_event_WCKeyEvent_VK_HOME                        36           /* com/sun/webkit/event/WCKeyEvent.java:45 */
#define com_sun_webkit_event_WCKeyEvent_VK_INSERT                      45           /* com/sun/webkit/event/WCKeyEvent.java:50 */
#define com_sun_webkit_event_WCKeyEvent_VK_LEFT                        37           /* com/sun/webkit/event/WCKeyEvent.java:46 */
#define com_sun_webkit_event_WCKeyEvent_VK_NEXT                        34           /* com/sun/webkit/event/WCKeyEvent.java:43 */
#define com_sun_webkit_event_WCKeyEvent_VK_OEM_PERIOD                  190          /* com/sun/webkit/event/WCKeyEvent.java:52 */
#define com_sun_webkit_event_WCKeyEvent_VK_PRIOR                       33           /* com/sun/webkit/event/WCKeyEvent.java:42 */
#define com_sun_webkit_event_WCKeyEvent_VK_RETURN                      13           /* com/sun/webkit/event/WCKeyEvent.java:40 */
#define com_sun_webkit_event_WCKeyEvent_VK_RIGHT                       39           /* com/sun/webkit/event/WCKeyEvent.java:48 */
#define com_sun_webkit_event_WCKeyEvent_VK_TAB                         9            /* com/sun/webkit/event/WCKeyEvent.java:39 */
#define com_sun_webkit_event_WCKeyEvent_VK_UP                          38           /* com/sun/webkit/event/WCKeyEvent.java:47 */

/* --- com_sun_webkit_event_WCMouseEvent --- */
#define com_sun_webkit_event_WCMouseEvent_BUTTON1                      1            /* com/sun/webkit/event/WCMouseEvent.java:41 */
#define com_sun_webkit_event_WCMouseEvent_BUTTON2                      2            /* com/sun/webkit/event/WCMouseEvent.java:42 */
#define com_sun_webkit_event_WCMouseEvent_BUTTON3                      4            /* com/sun/webkit/event/WCMouseEvent.java:43 */
#define com_sun_webkit_event_WCMouseEvent_MOUSE_DRAGGED                3            /* com/sun/webkit/event/WCMouseEvent.java:36 */
#define com_sun_webkit_event_WCMouseEvent_MOUSE_MOVED                  2            /* com/sun/webkit/event/WCMouseEvent.java:35 */
#define com_sun_webkit_event_WCMouseEvent_MOUSE_PRESSED                0            /* com/sun/webkit/event/WCMouseEvent.java:33 */
#define com_sun_webkit_event_WCMouseEvent_MOUSE_RELEASED               1            /* com/sun/webkit/event/WCMouseEvent.java:34 */
#define com_sun_webkit_event_WCMouseEvent_MOUSE_WHEEL                  4            /* com/sun/webkit/event/WCMouseEvent.java:37 */
#define com_sun_webkit_event_WCMouseEvent_NOBUTTON                     0            /* com/sun/webkit/event/WCMouseEvent.java:40 */

/* --- com_sun_webkit_graphics_GraphicsDecoder --- */
#define com_sun_webkit_graphics_GraphicsDecoder_BEGINTRANSPARENCYLAYER 22           /* com/sun/webkit/graphics/GraphicsDecoder.java:57 */
#define com_sun_webkit_graphics_GraphicsDecoder_CLEARRECT_FFFF         36           /* com/sun/webkit/graphics/GraphicsDecoder.java:68 */
#define com_sun_webkit_graphics_GraphicsDecoder_CLIP_PATH              14           /* com/sun/webkit/graphics/GraphicsDecoder.java:49 */
#define com_sun_webkit_graphics_GraphicsDecoder_CONCATTRANSFORM_FFFFFF 39           /* com/sun/webkit/graphics/GraphicsDecoder.java:71 */
#define com_sun_webkit_graphics_GraphicsDecoder_COPYREGION             40           /* com/sun/webkit/graphics/GraphicsDecoder.java:72 */
#define com_sun_webkit_graphics_GraphicsDecoder_DECODERQ               41           /* com/sun/webkit/graphics/GraphicsDecoder.java:73 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWELLIPSE            19           /* com/sun/webkit/graphics/GraphicsDecoder.java:54 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWFOCUSRING          20           /* com/sun/webkit/graphics/GraphicsDecoder.java:55 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWICON               9            /* com/sun/webkit/graphics/GraphicsDecoder.java:44 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE              8            /* com/sun/webkit/graphics/GraphicsDecoder.java:43 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWLINE               7            /* com/sun/webkit/graphics/GraphicsDecoder.java:42 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWPATTERN            10           /* com/sun/webkit/graphics/GraphicsDecoder.java:45 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWPOLYGON            6            /* com/sun/webkit/graphics/GraphicsDecoder.java:41 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWRECT               16           /* com/sun/webkit/graphics/GraphicsDecoder.java:51 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWSCROLLBAR          34           /* com/sun/webkit/graphics/GraphicsDecoder.java:67 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWSTRING             29           /* com/sun/webkit/graphics/GraphicsDecoder.java:64 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWSTRING_FAST        31           /* com/sun/webkit/graphics/GraphicsDecoder.java:65 */
#define com_sun_webkit_graphics_GraphicsDecoder_DRAWWIDGET             33           /* com/sun/webkit/graphics/GraphicsDecoder.java:66 */
#define com_sun_webkit_graphics_GraphicsDecoder_ENDTRANSPARENCYLAYER   23           /* com/sun/webkit/graphics/GraphicsDecoder.java:58 */
#define com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFF          47           /* com/sun/webkit/graphics/GraphicsDecoder.java:79 */
#define com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFFI         0            /* com/sun/webkit/graphics/GraphicsDecoder.java:36 */
#define com_sun_webkit_graphics_GraphicsDecoder_FILL_PATH              25           /* com/sun/webkit/graphics/GraphicsDecoder.java:60 */
#define com_sun_webkit_graphics_GraphicsDecoder_FILL_ROUNDED_RECT      48           /* com/sun/webkit/graphics/GraphicsDecoder.java:80 */
#define com_sun_webkit_graphics_GraphicsDecoder_GETIMAGE               26           /* com/sun/webkit/graphics/GraphicsDecoder.java:61 */
#define com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIACONTROL     44           /* com/sun/webkit/graphics/GraphicsDecoder.java:76 */
#define com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIAPLAYER      38           /* com/sun/webkit/graphics/GraphicsDecoder.java:70 */
#define com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIA_TIMETRACK  45           /* com/sun/webkit/graphics/GraphicsDecoder.java:77 */
#define com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIA_VOLUMETRACK 46           /* com/sun/webkit/graphics/GraphicsDecoder.java:78 */
#define com_sun_webkit_graphics_GraphicsDecoder_RESTORESTATE           13           /* com/sun/webkit/graphics/GraphicsDecoder.java:48 */
#define com_sun_webkit_graphics_GraphicsDecoder_ROTATE                 43           /* com/sun/webkit/graphics/GraphicsDecoder.java:75 */
#define com_sun_webkit_graphics_GraphicsDecoder_SAVESTATE              12           /* com/sun/webkit/graphics/GraphicsDecoder.java:47 */
#define com_sun_webkit_graphics_GraphicsDecoder_SCALE                  27           /* com/sun/webkit/graphics/GraphicsDecoder.java:62 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETALPHA               21           /* com/sun/webkit/graphics/GraphicsDecoder.java:56 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETCLIP_IIII           15           /* com/sun/webkit/graphics/GraphicsDecoder.java:50 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETCOMPOSITE           17           /* com/sun/webkit/graphics/GraphicsDecoder.java:52 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETFILLCOLOR           1            /* com/sun/webkit/graphics/GraphicsDecoder.java:37 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETSHADOW              28           /* com/sun/webkit/graphics/GraphicsDecoder.java:63 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETSTROKECOLOR         3            /* com/sun/webkit/graphics/GraphicsDecoder.java:39 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETSTROKESTYLE         2            /* com/sun/webkit/graphics/GraphicsDecoder.java:38 */
#define com_sun_webkit_graphics_GraphicsDecoder_SETSTROKEWIDTH         4            /* com/sun/webkit/graphics/GraphicsDecoder.java:40 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_FILL_GRADIENT      49           /* com/sun/webkit/graphics/GraphicsDecoder.java:81 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_CAP           52           /* com/sun/webkit/graphics/GraphicsDecoder.java:84 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_DASH          51           /* com/sun/webkit/graphics/GraphicsDecoder.java:83 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_JOIN          53           /* com/sun/webkit/graphics/GraphicsDecoder.java:85 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_MITER_LIMIT        54           /* com/sun/webkit/graphics/GraphicsDecoder.java:86 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_PERSPECTIVE_TRANSFORM 56           /* com/sun/webkit/graphics/GraphicsDecoder.java:88 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_STROKE_GRADIENT    50           /* com/sun/webkit/graphics/GraphicsDecoder.java:82 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_TEXT_MODE          55           /* com/sun/webkit/graphics/GraphicsDecoder.java:87 */
#define com_sun_webkit_graphics_GraphicsDecoder_SET_TRANSFORM          42           /* com/sun/webkit/graphics/GraphicsDecoder.java:74 */
#define com_sun_webkit_graphics_GraphicsDecoder_STROKEARC              18           /* com/sun/webkit/graphics/GraphicsDecoder.java:53 */
#define com_sun_webkit_graphics_GraphicsDecoder_STROKERECT_FFFFF       37           /* com/sun/webkit/graphics/GraphicsDecoder.java:69 */
#define com_sun_webkit_graphics_GraphicsDecoder_STROKE_PATH            24           /* com/sun/webkit/graphics/GraphicsDecoder.java:59 */
#define com_sun_webkit_graphics_GraphicsDecoder_TRANSLATE              11           /* com/sun/webkit/graphics/GraphicsDecoder.java:46 */

/* --- com_sun_webkit_graphics_RenderMediaControls --- */
#define com_sun_webkit_graphics_RenderMediaControls_BACKGROUND         8            /* com/sun/webkit/graphics/RenderMediaControls.java:49 */
#define com_sun_webkit_graphics_RenderMediaControls_CURRENT_TIME       14           /* com/sun/webkit/graphics/RenderMediaControls.java:60 */
#define com_sun_webkit_graphics_RenderMediaControls_DISABLED_MUTE_BUTTON 6            /* com/sun/webkit/graphics/RenderMediaControls.java:44 */
#define com_sun_webkit_graphics_RenderMediaControls_DISABLED_PLAY_BUTTON 3            /* com/sun/webkit/graphics/RenderMediaControls.java:40 */
#define com_sun_webkit_graphics_RenderMediaControls_FULLSCREEN_BUTTON  7            /* com/sun/webkit/graphics/RenderMediaControls.java:46 */
#define com_sun_webkit_graphics_RenderMediaControls_MUTE_BUTTON        4            /* com/sun/webkit/graphics/RenderMediaControls.java:42 */
#define com_sun_webkit_graphics_RenderMediaControls_PAUSE_BUTTON       2            /* com/sun/webkit/graphics/RenderMediaControls.java:39 */
#define com_sun_webkit_graphics_RenderMediaControls_PLAY_BUTTON        1            /* com/sun/webkit/graphics/RenderMediaControls.java:38 */
#define com_sun_webkit_graphics_RenderMediaControls_REMAINING_TIME     15           /* com/sun/webkit/graphics/RenderMediaControls.java:61 */
#define com_sun_webkit_graphics_RenderMediaControls_SLIDER_TYPE_TIME   0            /* com/sun/webkit/graphics/RenderMediaControls.java:226 */
#define com_sun_webkit_graphics_RenderMediaControls_SLIDER_TYPE_VOLUME 1            /* com/sun/webkit/graphics/RenderMediaControls.java:227 */
#define com_sun_webkit_graphics_RenderMediaControls_TIME_SLIDER_THUMB  10           /* com/sun/webkit/graphics/RenderMediaControls.java:53 */
#define com_sun_webkit_graphics_RenderMediaControls_TIME_SLIDER_TRACK  9            /* com/sun/webkit/graphics/RenderMediaControls.java:52 */

/* --- com_sun_webkit_graphics_RenderMediaControls_TimeSliderTrackThickness --- */
#define com_sun_webkit_graphics_RenderMediaControls_TimeSliderTrackThickness 3            /* com/sun/webkit/graphics/RenderMediaControls.java:136 */

/* --- com_sun_webkit_graphics_RenderMediaControls --- */
#define com_sun_webkit_graphics_RenderMediaControls_UNMUTE_BUTTON      5            /* com/sun/webkit/graphics/RenderMediaControls.java:43 */
#define com_sun_webkit_graphics_RenderMediaControls_VOLUME_CONTAINER   11           /* com/sun/webkit/graphics/RenderMediaControls.java:55 */
#define com_sun_webkit_graphics_RenderMediaControls_VOLUME_THUMB       13           /* com/sun/webkit/graphics/RenderMediaControls.java:58 */
#define com_sun_webkit_graphics_RenderMediaControls_VOLUME_TRACK       12           /* com/sun/webkit/graphics/RenderMediaControls.java:57 */

/* --- com_sun_webkit_graphics_RenderMediaControls_VolumeTrackThickness --- */
#define com_sun_webkit_graphics_RenderMediaControls_VolumeTrackThickness 1            /* com/sun/webkit/graphics/RenderMediaControls.java:203 */

/* --- com_sun_webkit_graphics_RenderMediaControls_log --- */
#define com_sun_webkit_graphics_RenderMediaControls_log                0            /* com/sun/webkit/graphics/RenderMediaControls.java:292 */

/* --- com_sun_webkit_graphics_RenderTheme --- */
#define com_sun_webkit_graphics_RenderTheme_BACKGROUND                 0            /* com/sun/webkit/graphics/RenderTheme.java:52 */
#define com_sun_webkit_graphics_RenderTheme_BUTTON                     1            /* com/sun/webkit/graphics/RenderTheme.java:35 */
#define com_sun_webkit_graphics_RenderTheme_CHECKED                    1            /* com/sun/webkit/graphics/RenderTheme.java:44 */
#define com_sun_webkit_graphics_RenderTheme_CHECK_BOX                  2            /* com/sun/webkit/graphics/RenderTheme.java:36 */
#define com_sun_webkit_graphics_RenderTheme_ENABLED                    4            /* com/sun/webkit/graphics/RenderTheme.java:46 */
#define com_sun_webkit_graphics_RenderTheme_FOCUSED                    8            /* com/sun/webkit/graphics/RenderTheme.java:47 */
#define com_sun_webkit_graphics_RenderTheme_FOREGROUND                 1            /* com/sun/webkit/graphics/RenderTheme.java:53 */
#define com_sun_webkit_graphics_RenderTheme_HOVERED                    32           /* com/sun/webkit/graphics/RenderTheme.java:49 */
#define com_sun_webkit_graphics_RenderTheme_INDETERMINATE              2            /* com/sun/webkit/graphics/RenderTheme.java:45 */
#define com_sun_webkit_graphics_RenderTheme_MENU_LIST                  4            /* com/sun/webkit/graphics/RenderTheme.java:38 */
#define com_sun_webkit_graphics_RenderTheme_MENU_LIST_BUTTON           5            /* com/sun/webkit/graphics/RenderTheme.java:39 */
#define com_sun_webkit_graphics_RenderTheme_METER                      8            /* com/sun/webkit/graphics/RenderTheme.java:42 */
#define com_sun_webkit_graphics_RenderTheme_PRESSED                    16           /* com/sun/webkit/graphics/RenderTheme.java:48 */
#define com_sun_webkit_graphics_RenderTheme_PROGRESS_BAR               7            /* com/sun/webkit/graphics/RenderTheme.java:41 */
#define com_sun_webkit_graphics_RenderTheme_RADIO_BUTTON               3            /* com/sun/webkit/graphics/RenderTheme.java:37 */
#define com_sun_webkit_graphics_RenderTheme_READ_ONLY                  64           /* com/sun/webkit/graphics/RenderTheme.java:50 */
#define com_sun_webkit_graphics_RenderTheme_SLIDER                     6            /* com/sun/webkit/graphics/RenderTheme.java:40 */
#define com_sun_webkit_graphics_RenderTheme_TEXT_FIELD                 0            /* com/sun/webkit/graphics/RenderTheme.java:34 */

/* --- com_sun_webkit_graphics_ScrollBarTheme --- */
#define com_sun_webkit_graphics_ScrollBarTheme_BACK_BUTTON_END_PART    32           /* com/sun/webkit/graphics/ScrollBarTheme.java:39 */
#define com_sun_webkit_graphics_ScrollBarTheme_BACK_BUTTON_START_PART  1            /* com/sun/webkit/graphics/ScrollBarTheme.java:34 */
#define com_sun_webkit_graphics_ScrollBarTheme_BACK_TRACK_PART         4            /* com/sun/webkit/graphics/ScrollBarTheme.java:36 */
#define com_sun_webkit_graphics_ScrollBarTheme_FORWARD_BUTTON_END_PART 64           /* com/sun/webkit/graphics/ScrollBarTheme.java:40 */
#define com_sun_webkit_graphics_ScrollBarTheme_FORWARD_BUTTON_START_PART 2            /* com/sun/webkit/graphics/ScrollBarTheme.java:35 */
#define com_sun_webkit_graphics_ScrollBarTheme_FORWARD_TRACK_PART      16           /* com/sun/webkit/graphics/ScrollBarTheme.java:38 */
#define com_sun_webkit_graphics_ScrollBarTheme_HORIZONTAL_SCROLLBAR    0            /* com/sun/webkit/graphics/ScrollBarTheme.java:44 */
#define com_sun_webkit_graphics_ScrollBarTheme_NO_PART                 0            /* com/sun/webkit/graphics/ScrollBarTheme.java:33 */
#define com_sun_webkit_graphics_ScrollBarTheme_SCROLLBAR_BG_PART       128          /* com/sun/webkit/graphics/ScrollBarTheme.java:41 */
#define com_sun_webkit_graphics_ScrollBarTheme_THUMB_PART              8            /* com/sun/webkit/graphics/ScrollBarTheme.java:37 */
#define com_sun_webkit_graphics_ScrollBarTheme_TRACK_BG_PART           256          /* com/sun/webkit/graphics/ScrollBarTheme.java:42 */
#define com_sun_webkit_graphics_ScrollBarTheme_VERTICAL_SCROLLBAR      1            /* com/sun/webkit/graphics/ScrollBarTheme.java:45 */

/* --- com_sun_webkit_graphics_WCGradient --- */
#define com_sun_webkit_graphics_WCGradient_PAD                         1            /* com/sun/webkit/graphics/WCGradient.java:35 */
#define com_sun_webkit_graphics_WCGradient_REFLECT                     2            /* com/sun/webkit/graphics/WCGradient.java:36 */
#define com_sun_webkit_graphics_WCGradient_REPEAT                      3            /* com/sun/webkit/graphics/WCGradient.java:37 */

/* --- com_sun_webkit_graphics_WCGraphicsContext --- */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_CLEAR      0            /* com/sun/webkit/graphics/WCGraphicsContext.java:34 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_COPY       1            /* com/sun/webkit/graphics/WCGraphicsContext.java:35 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_DESTINATION_ATOP 9            /* com/sun/webkit/graphics/WCGraphicsContext.java:43 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_DESTINATION_IN 7            /* com/sun/webkit/graphics/WCGraphicsContext.java:41 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_DESTINATION_OUT 8            /* com/sun/webkit/graphics/WCGraphicsContext.java:42 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_DESTINATION_OVER 6            /* com/sun/webkit/graphics/WCGraphicsContext.java:40 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_HIGHLIGHT  12           /* com/sun/webkit/graphics/WCGraphicsContext.java:46 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_PLUS_DARKER 11           /* com/sun/webkit/graphics/WCGraphicsContext.java:45 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_PLUS_LIGHTER 13           /* com/sun/webkit/graphics/WCGraphicsContext.java:47 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_SOURCE_ATOP 5            /* com/sun/webkit/graphics/WCGraphicsContext.java:39 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_SOURCE_IN  3            /* com/sun/webkit/graphics/WCGraphicsContext.java:37 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_SOURCE_OUT 4            /* com/sun/webkit/graphics/WCGraphicsContext.java:38 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_SOURCE_OVER 2            /* com/sun/webkit/graphics/WCGraphicsContext.java:36 */
#define com_sun_webkit_graphics_WCGraphicsContext_COMPOSITE_XOR        10           /* com/sun/webkit/graphics/WCGraphicsContext.java:44 */

/* --- com_sun_webkit_graphics_WCMediaPlayer --- */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_DECODE_ERROR 6            /* com/sun/webkit/graphics/WCMediaPlayer.java:113 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_EMPTY      0            /* com/sun/webkit/graphics/WCMediaPlayer.java:107 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_FORMAT_ERROR 4            /* com/sun/webkit/graphics/WCMediaPlayer.java:111 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_IDLE       1            /* com/sun/webkit/graphics/WCMediaPlayer.java:108 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADED     3            /* com/sun/webkit/graphics/WCMediaPlayer.java:110 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADING    2            /* com/sun/webkit/graphics/WCMediaPlayer.java:109 */
#define com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_NETWORK_ERROR 5            /* com/sun/webkit/graphics/WCMediaPlayer.java:112 */
#define com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_AUTO             2            /* com/sun/webkit/graphics/WCMediaPlayer.java:123 */
#define com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_METADATA         1            /* com/sun/webkit/graphics/WCMediaPlayer.java:122 */
#define com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_NONE             0            /* com/sun/webkit/graphics/WCMediaPlayer.java:121 */
#define com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_CURRENT_DATA 2            /* com/sun/webkit/graphics/WCMediaPlayer.java:117 */
#define com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_ENOUGH_DATA 4            /* com/sun/webkit/graphics/WCMediaPlayer.java:119 */
#define com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_FUTURE_DATA 3            /* com/sun/webkit/graphics/WCMediaPlayer.java:118 */
#define com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_METADATA 1            /* com/sun/webkit/graphics/WCMediaPlayer.java:116 */
#define com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_NOTHING 0            /* com/sun/webkit/graphics/WCMediaPlayer.java:115 */

/* --- com_sun_webkit_graphics_WCPathIterator --- */
#define com_sun_webkit_graphics_WCPathIterator_SEG_CLOSE               4            /* com/sun/webkit/graphics/WCPathIterator.java:36 */
#define com_sun_webkit_graphics_WCPathIterator_SEG_CUBICTO             3            /* com/sun/webkit/graphics/WCPathIterator.java:35 */
#define com_sun_webkit_graphics_WCPathIterator_SEG_LINETO              1            /* com/sun/webkit/graphics/WCPathIterator.java:33 */
#define com_sun_webkit_graphics_WCPathIterator_SEG_MOVETO              0            /* com/sun/webkit/graphics/WCPathIterator.java:32 */
#define com_sun_webkit_graphics_WCPathIterator_SEG_QUADTO              2            /* com/sun/webkit/graphics/WCPathIterator.java:34 */

/* --- com_sun_webkit_graphics_WCPath --- */
#define com_sun_webkit_graphics_WCPath_RULE_EVENODD                    1            /* com/sun/webkit/graphics/WCPath.java:54 */
#define com_sun_webkit_graphics_WCPath_RULE_NONZERO                    0            /* com/sun/webkit/graphics/WCPath.java:45 */

/* --- com_sun_webkit_graphics_WCRenderQueue --- */
#define com_sun_webkit_graphics_WCRenderQueue_MAX_QUEUE_SIZE           524288       /* com/sun/webkit/graphics/WCRenderQueue.java:41 */

/* --- com_sun_webkit_graphics_WCStroke --- */
#define com_sun_webkit_graphics_WCStroke_BEVEL_JOIN                    2            /* com/sun/webkit/graphics/WCStroke.java:52 */
#define com_sun_webkit_graphics_WCStroke_BUTT_CAP                      0            /* com/sun/webkit/graphics/WCStroke.java:43 */
#define com_sun_webkit_graphics_WCStroke_DASHED_STROKE                 3            /* com/sun/webkit/graphics/WCStroke.java:38 */
#define com_sun_webkit_graphics_WCStroke_DOTTED_STROKE                 2            /* com/sun/webkit/graphics/WCStroke.java:37 */
#define com_sun_webkit_graphics_WCStroke_MITER_JOIN                    0            /* com/sun/webkit/graphics/WCStroke.java:50 */
#define com_sun_webkit_graphics_WCStroke_NO_STROKE                     0            /* com/sun/webkit/graphics/WCStroke.java:35 */
#define com_sun_webkit_graphics_WCStroke_ROUND_CAP                     1            /* com/sun/webkit/graphics/WCStroke.java:44 */
#define com_sun_webkit_graphics_WCStroke_ROUND_JOIN                    1            /* com/sun/webkit/graphics/WCStroke.java:51 */
#define com_sun_webkit_graphics_WCStroke_SOLID_STROKE                  1            /* com/sun/webkit/graphics/WCStroke.java:36 */
#define com_sun_webkit_graphics_WCStroke_SQUARE_CAP                    2            /* com/sun/webkit/graphics/WCStroke.java:45 */

/* --- com_sun_webkit_network_CookieStore --- */
#define com_sun_webkit_network_CookieStore_MAX_BUCKET_SIZE             50           /* com/sun/webkit/network/CookieStore.java:50 */
#define com_sun_webkit_network_CookieStore_TOTAL_COUNT_LOWER_THRESHOLD 3000         /* com/sun/webkit/network/CookieStore.java:51 */
#define com_sun_webkit_network_CookieStore_TOTAL_COUNT_UPPER_THRESHOLD 4000         /* com/sun/webkit/network/CookieStore.java:52 */

/* --- com_sun_webkit_network_NetworkContext --- */
#define com_sun_webkit_network_NetworkContext_DEFAULT_HTTP2_MAX_CONNECTIONS 20           /* com/sun/webkit/network/NetworkContext.java:67 */
#define com_sun_webkit_network_NetworkContext_DEFAULT_HTTP_MAX_CONNECTIONS 5            /* com/sun/webkit/network/NetworkContext.java:61 */
#define com_sun_webkit_network_NetworkContext_THREAD_POOL_KEEP_ALIVE_TIME 10000        /* com/sun/webkit/network/NetworkContext.java:56 */
#define com_sun_webkit_network_NetworkContext_THREAD_POOL_SIZE         20           /* com/sun/webkit/network/NetworkContext.java:51 */

/* --- com_sun_webkit_network_URLLoaderBase --- */
#define com_sun_webkit_network_URLLoaderBase_ALLOW_UNASSIGNED          1            /* com/sun/webkit/network/URLLoaderBase.java:32 */

/* --- com_sun_webkit_network_URLLoader --- */
#define com_sun_webkit_network_URLLoader_MAX_BUF_COUNT                 3            /* com/sun/webkit/network/URLLoader.java:68 */

/* --- com_sun_webkit_plugin_Plugin --- */
#define com_sun_webkit_plugin_Plugin_EVENT_BEFOREACTIVATE              -4           /* com/sun/webkit/plugin/Plugin.java:33 */
#define com_sun_webkit_plugin_Plugin_EVENT_FOCUSCHANGE                 -1           /* com/sun/webkit/plugin/Plugin.java:34 */

/* --- com_sun_webkit_text_TextBreakIterator --- */
#define com_sun_webkit_text_TextBreakIterator_CHARACTER_ITERATOR       0            /* com/sun/webkit/text/TextBreakIterator.java:38 */
#define com_sun_webkit_text_TextBreakIterator_IS_TEXT_BREAK            7            /* com/sun/webkit/text/TextBreakIterator.java:51 */
#define com_sun_webkit_text_TextBreakIterator_IS_WORD_TEXT_BREAK       8            /* com/sun/webkit/text/TextBreakIterator.java:52 */
#define com_sun_webkit_text_TextBreakIterator_LINE_ITERATOR            2            /* com/sun/webkit/text/TextBreakIterator.java:40 */
#define com_sun_webkit_text_TextBreakIterator_SENTENCE_ITERATOR        3            /* com/sun/webkit/text/TextBreakIterator.java:41 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_CURRENT       4            /* com/sun/webkit/text/TextBreakIterator.java:48 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_FIRST         0            /* com/sun/webkit/text/TextBreakIterator.java:44 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_FOLLOWING     6            /* com/sun/webkit/text/TextBreakIterator.java:50 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_LAST          1            /* com/sun/webkit/text/TextBreakIterator.java:45 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_NEXT          2            /* com/sun/webkit/text/TextBreakIterator.java:46 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_PRECEDING     5            /* com/sun/webkit/text/TextBreakIterator.java:49 */
#define com_sun_webkit_text_TextBreakIterator_TEXT_BREAK_PREVIOUS      3            /* com/sun/webkit/text/TextBreakIterator.java:47 */
#define com_sun_webkit_text_TextBreakIterator_WORD_ITERATOR            1            /* com/sun/webkit/text/TextBreakIterator.java:39 */

/* --- com_sun_webkit_text_TextNormalizer --- */
#define com_sun_webkit_text_TextNormalizer_FORM_NFC                    0            /* com/sun/webkit/text/TextNormalizer.java:34 */
#define com_sun_webkit_text_TextNormalizer_FORM_NFD                    1            /* com/sun/webkit/text/TextNormalizer.java:35 */
#define com_sun_webkit_text_TextNormalizer_FORM_NFKC                   2            /* com/sun/webkit/text/TextNormalizer.java:36 */
#define com_sun_webkit_text_TextNormalizer_FORM_NFKD                   3            /* com/sun/webkit/text/TextNormalizer.java:37 */

#endif /* WKJ_CONSTANTS_H */
