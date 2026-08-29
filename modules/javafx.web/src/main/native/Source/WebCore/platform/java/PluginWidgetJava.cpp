/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"
#include "ContainerNodeInlines.h"
#include "EventNames.h"
#include "FocusController.h"
#include "FrameView.h"
#include "Image.h"
#include "PlatformJavaClasses.h"
#include "MouseEvent.h"
#include "NotImplemented.h"
#include "PluginWidgetJava.h"
#include "RenderBox.h"
#include "StringJava.h"
#include "WKJDOMUtils.h"
#include "NodeDocument.h"

#include <webkit_java_api.h>


namespace WebCore {

/*
 * The four cached method ids, the five cached field ids (the pData long plus the four float
 * fields of WCRectangle), the cached WCRectangle class and the exported initIDs that filled
 * them are all gone: they are the plugin_widget_ section of WKJHostTheme. See the report and
 * FFM-AUDIT-wtf-webcore.md 15.5 row 14 - this is one of only two WRAPPER verdicts in the
 * whole tree that deletes a Java `native` declaration.
 */

extern "C" {

/*
 * The three entry points below used to read the PluginWidgetJava* out of the receiver's
 * `pData` field with GetLongField. It is now an explicit parameter, which is what removed
 * the last cached field id from this directory; the `if (pThis)` guard is unchanged.
 */

WKJ_EXPORT void wkj_plugin_widget_invalidate_rect(int64_t pluginWidget, int32_t x, int32_t y,
                                                  int32_t width, int32_t height)
{
    WKJCallScope wkjScope;
    PluginWidgetJava *pThis = static_cast<PluginWidgetJava*>(wkj_to_ptr(pluginWidget));
    if(pThis)
        pThis->invalidateWindowlessPluginRect( IntRect(x, y, width, height) );
}

WKJ_EXPORT void wkj_plugin_widget_set_focused(int64_t pluginWidget, int32_t isFocused)
{
    WKJCallScope wkjScope;
    PluginWidgetJava *pThis = static_cast<PluginWidgetJava*>(wkj_to_ptr(pluginWidget));
    if(pThis)
        pThis->focusPluginElement( isFocused != 0 );
}

/*
 * The JNI version read four float fields off the WCRectangle it was given and returned a
 * freshly constructed one, or null when the peer was 0. The caller now provides both
 * buffers; a return of 0 is the null return. The truncation through an int rect and back is
 * the existing behaviour, kept.
 */
WKJ_EXPORT int32_t wkj_plugin_widget_convert_to_page(int64_t pluginWidget,
                                                     const float inXYWH[4], float outXYWH[4])
{
    WKJCallScope wkjScope;
    PluginWidgetJava *pThis = static_cast<PluginWidgetJava*>(wkj_to_ptr(pluginWidget));
    if(pThis){
        IntRect irc(
            (int)inXYWH[0],
            (int)inXYWH[1],
            (int)inXYWH[2],
            (int)inXYWH[3]);
        pThis->convertToPage(irc);
        outXYWH[0] = static_cast<float>(irc.x());
        outXYWH[1] = static_cast<float>(irc.y());
        outXYWH[2] = static_cast<float>(irc.width());
        outXYWH[3] = static_cast<float>(irc.height());
        return 1;
    }
    return 0;
}


} // extern "C"

PluginWidgetJava::PluginWidgetJava(
    wkj_ref wfh,
    HTMLPlugInElement* element,
    const String& url,
    const String& mimeType,
    const Vector<AtomString>& paramNames,
    const Vector<AtomString>& paramValues)
      : m_element(element),
        m_url(url),
        m_mimeType(mimeType),
        m_paramNames(paramNames),
        m_paramValues(paramValues)
{
    //TODO: have to be moved into setParent(non-null)
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->plugin_widget_create)
        return;

    WKJStringArg urlArg(url);
    WKJStringArg mimeTypeArg(mimeType);
    // WebKit builds these two in lockstep, so one count describes both, as the two
    // separate Java arrays always had the same length.
    WKJStringArrayArg pNames(paramNames);
    WKJStringArrayArg pValues(paramValues);
    ASSERT(pNames.count() == pValues.count());

    // width and height were declared and never assigned, so 0 is what Java always received.
    int32_t width = 0, height = 0;
    WKJHandle obj { cb->plugin_widget_create(
        wfh,
        width, height,
        urlArg.data(), urlArg.length(),
        mimeTypeArg.data(), mimeTypeArg.length(),
        pNames.data(), pNames.lengths(),
        pValues.data(), pValues.lengths(),
        pNames.count()) };
    wkjCheckAndClearException();

    ASSERT(obj);
    if (obj) {
        setPlatformWidget(obj);
        if (cb->plugin_widget_set_peer) {
            cb->plugin_widget_set_peer(obj.get(), wkj_from_ptr(this));
            wkjCheckAndClearException();
        }
        setSelfVisible(true);
        setParentVisible(true);
    }
}

void PluginWidgetJava::invalidateRect(const IntRect&)
{
    notImplemented();
}

PluginWidgetJava::~PluginWidgetJava() {
}

void PluginWidgetJava::paint(
    GraphicsContext& context,
    const IntRect& rc /*page coordinates*/,
    SecurityOriginPaintPolicy,
    RegionContext*) {
    //Widget::paint(context, rc);
    /*
    if (!m_isStarted) {
        // Draw the "missing plugin" image
        paintMissingPluginIcon(context, rect);
        return;
    }
    */
    //if (context.paintingDisabled())
        //return;

    PlatformWidget obj = platformWidget();
    const WKJHostTheme* cb = wkjTheme();
    if (obj && cb && cb->plugin_widget_paint){
        context.save();
        /*
         * BUG PRESERVED. The second argument is declared in Java as a
         * com.sun.webkit.graphics.WCGraphicsContext, but what has always been passed is
         * context.platformContext(), i.e. a WebCore::PlatformContextJava* - a C++ pointer,
         * not a Java object. The ABI declares the parameter as the int64_t it really is so
         * that the mistake is visible rather than hidden behind a cast, and so that Java
         * receives a value it can reject instead of a bogus object reference. Deciding what
         * a plugin widget should actually be handed to draw with is a behaviour change and
         * belongs in its own commit.
         */
        cb->plugin_widget_paint(
            obj.get(),
            wkj_from_ptr(context.platformContext()),
            rc.x(), rc.y(), rc.width(), rc.height());
        context.restore();
    }
}


void PluginWidgetJava::convertToPage(IntRect&)
{
    if (!isVisible())
        return;

    if (!m_element || !m_element->renderer())
        return;

    RenderBox* renderer = downcast<RenderBox>(m_element->renderer()); // FIXME-java: recheck
    if(renderer){
        renderer->offsetFromContainer(*renderer->container(), LayoutPoint());
    }

}

void PluginWidgetJava::setFrameRect(const IntRect& rect)
{
    if (m_element->document().printing())
        return;

    if (rect != frameRect())
        Widget::setFrameRect(rect);

    updatePluginWidget();
}

void PluginWidgetJava::frameRectsChanged()
{
    updatePluginWidget();
}

void PluginWidgetJava::updatePluginWidget()
{
    if (!parent())
        return;

    FrameView* frameView = static_cast<FrameView*>(parent());
    IntRect windowRect(frameView->contentsToWindow(frameRect().location()), frameRect().size());
    PlatformWidget obj = platformWidget();
    const WKJHostTheme* cb = wkjTheme();
    if(obj && cb && cb->plugin_widget_set_native_container_bounds){
        cb->plugin_widget_set_native_container_bounds(
            obj.get(),
            (int32_t)windowRect.x(),
            (int32_t)windowRect.y(),
            (int32_t)windowRect.width(),
            (int32_t)windowRect.height());

    }
}


void PluginWidgetJava::invalidateWindowlessPluginRect(
    const IntRect& rect //client coordinates
){
    if (!isVisible())
        return;

    if (!m_element || !m_element->renderer())
        return;

    RenderBox* renderer = downcast<RenderBox>(m_element->renderer()); //XXX: recheck
    if(renderer){
        renderer->repaintRectangle(rect);
    }
}

//look at "void PluginView::focusPluginElement()"
void PluginWidgetJava::focusPluginElement(bool)
{
/*
    if( isFocused ){
        // Focus the plugin
        Frame *parentFrame = static_cast<FrameView*>(parent())->frame();
        if (Page* page = parentFrame->page())
            page->focusController()->setFocusedFrame(parentFrame);
        parentFrame->document()->setFocusedNode(m_element);
    }
*/
}

void PluginWidgetJava::handleEvent(Event& event)
{
    PlatformWidget obj = platformWidget();
    const WKJHostTheme* cb = wkjTheme();
    int32_t cancelBubble = 0;
    if (obj && cb && cb->plugin_widget_handle_mouse_event && event.isMouseEvent()) {
        MouseEvent* me = static_cast<MouseEvent*>(&event);
        //look at "void PluginView::handleMouseEvent(MouseEvent* event)"
        //takes into account zoomFactor for offsetX, offsetY
        IntPoint p = static_cast<FrameView*>(parent())->contentsToWindow(
            IntPoint(me->pageX(), me->pageY()));
        WKJStringArg type(me->type().string());
        cancelBubble = cb->plugin_widget_handle_mouse_event(
            obj.get(),
            type.data(), type.length(),
            (int32_t)p.x(),
            (int32_t)p.y(),
            (int32_t)me->screenX(),
            (int32_t)me->screenY(),
            (int32_t)me->button(),
            me->buttonDown() ? 1 : 0,
            me->altKey() ? 1 : 0,
            me->metaKey() ? 1 : 0,
            me->ctrlKey() ? 1 : 0,
            me->shiftKey() ? 1 : 0,
            (int64_t)me->timeStamp().approximateWallTime().secondsSinceEpoch().milliseconds());
        wkjCheckAndClearException();
    }

    if(cancelBubble) {
        event.setDefaultHandled();
        event.cancelBubble();
    } else {
        Widget::handleEvent(event);
    }
}

}
