/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "EventNames.h"
#include "FocusController.h"
#include "FrameView.h"
#include "Image.h"
#include "JavaEnv.h"
#include "MouseEvent.h"
#include "NotImplemented.h"
#include "PluginWidgetJava.h"
#include "RenderBox.h"
#include "StringJava.h"

#include "com_sun_webkit_WCPluginWidget.h"

namespace WebCore {

jmethodID pluginWidgetPaintMID;
jmethodID pluginWidgetCreateMID;
jmethodID pluginWidgetBlurMID;
jmethodID pluginWidgetFWKHandleMouseEventMID;
jmethodID pluginWidgetFWKSetNativeContainerBoundsMID;
jfieldID  pluginWidgetPDataFID;

/************************************************************************
 * WCRectangle fields
 */

jfieldID xFID;
jfieldID yFID;
jfieldID widthFID;
jfieldID heightFID;
jmethodID wcRectCTOR;
JGClass clwcRectangle;

extern "C" {
JNIEXPORT void JNICALL Java_com_sun_webkit_WCPluginWidget_initIDs(JNIEnv* env, jclass pluginWidgetClass)
{
    pluginWidgetPaintMID = env->GetMethodID(pluginWidgetClass, "paint",
       "(Lcom/sun/webkit/graphics/WCGraphicsContext;IIII)V");
    ASSERT(pluginWidgetPaintMID);

    pluginWidgetCreateMID = env->GetStaticMethodID(pluginWidgetClass, "create",
       "(Lcom/sun/webkit/WebPage;IILjava/lang/String;"
       "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)"
       "Lcom/sun/webkit/WCPluginWidget;");
    ASSERT(pluginWidgetCreateMID);

    pluginWidgetFWKSetNativeContainerBoundsMID = env->GetMethodID(
        pluginWidgetClass, "fwkSetNativeContainerBounds",
        "(IIII)V");
    ASSERT(pluginWidgetFWKSetNativeContainerBoundsMID);

    pluginWidgetFWKHandleMouseEventMID = env->GetMethodID(pluginWidgetClass,
        "fwkHandleMouseEvent", "(Ljava/lang/String;IIIIIZZZZZJ)Z");
    ASSERT(pluginWidgetFWKHandleMouseEventMID);

    pluginWidgetPDataFID = env->GetFieldID(pluginWidgetClass, "pData", "J");
    ASSERT(pluginWidgetPDataFID);


    clwcRectangle = JLClass(env->FindClass("com/sun/webkit/graphics/WCRectangle"));
    ASSERT(clwcRectangle);

    wcRectCTOR = env->GetMethodID(clwcRectangle, "<init>", "(FFFF)V");
    ASSERT(wcRectCTOR);

    xFID = env->GetFieldID(clwcRectangle, "x", "F");
    ASSERT(xFID);

    yFID = env->GetFieldID(clwcRectangle, "y", "F");
    ASSERT(yFID);

    widthFID = env->GetFieldID(clwcRectangle, "w", "F");
    ASSERT(widthFID);

    heightFID = env->GetFieldID(clwcRectangle, "h", "F");
    ASSERT(heightFID);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_WCPluginWidget_twkInvalidateWindowlessPluginRect
    (JNIEnv* env,jobject self, jint x, jint y, jint width, jint height)
{
    PluginWidgetJava *pThis = ((PluginWidgetJava *)jlong_to_ptr(env->GetLongField(self, pluginWidgetPDataFID)));
    if(pThis)
        pThis->invalidateWindowlessPluginRect( IntRect(x, y, width, height) );
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WCPluginWidget_twkSetPlugunFocused
    (JNIEnv* env, jobject self, jboolean isFocused)
{
    PluginWidgetJava *pThis = ((PluginWidgetJava *)jlong_to_ptr(env->GetLongField(self, pluginWidgetPDataFID)));
    if(pThis)
        pThis->focusPluginElement( isFocused );
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WCPluginWidget_twkConvertToPage
    (JNIEnv* env, jobject self, jobject rc)
{
    PluginWidgetJava *pThis = ((PluginWidgetJava *)jlong_to_ptr(env->GetLongField(self, pluginWidgetPDataFID)));
    if(pThis){
        IntRect irc(
            (int)env->GetFloatField(rc, xFID),
            (int)env->GetFloatField(rc, yFID),
            (int)env->GetFloatField(rc, widthFID),
            (int)env->GetFloatField(rc, heightFID));
        pThis->convertToPage(irc);
        return env->NewObject(
            clwcRectangle,
            wcRectCTOR,
            jdouble(irc.x()),
            jdouble(irc.y()),
            jdouble(irc.width()),
            jdouble(irc.height()));
    }
    return NULL;
}


} // extern "C"

PluginWidgetJava::PluginWidgetJava(
    jobject wfh,
    HTMLPlugInElement *element,
    const IntSize &size,
    const String &url,
    const String &mimeType,
    const Vector<String> &paramNames,
    const Vector<String> &paramValues)
:   m_element(element),
    m_size(size),
    m_url(url),
    m_mimeType(mimeType),
    m_paramNames(paramNames),
    m_paramValues(paramValues)
{
    //TODO: have to be moved into setParent(non-null)
    {
        JNIEnv* env = WebCore_GetJavaEnv();
        JLString urlJavaString(url.toJavaString(env));
        JLString mimeTypeJavaString(mimeType.toJavaString(env));

        //better to delegate this upto org/webkit/webcore/platform/api/WebPage
        //as for "createScrollView"
        JLClass cls(env->FindClass("com/sun/webkit/WCPluginWidget"));
        ASSERT(cls);

        jobjectArray pNames = strVect2JArray(env, paramNames);
        jobjectArray pValues = strVect2JArray(env, paramValues);

        JLObject obj(env->CallStaticObjectMethod(
                                                           cls,
                                                           pluginWidgetCreateMID,
                                                           wfh,
                                                           size.width(), size.height(),
                                                           (jstring)urlJavaString,
                                                           (jstring)mimeTypeJavaString,
                                                           pNames, pValues));
        CheckAndClearException(env);

        ASSERT(obj);
        if (obj) {
            setPlatformWidget(obj);
            env->SetLongField(obj, pluginWidgetPDataFID, ptr_to_jlong(this));
            setSelfVisible(true);
            setParentVisible(true);
        }
    }
}

void PluginWidgetJava::invalidateRect(const IntRect&)
{
    notImplemented();
}

PluginWidgetJava::~PluginWidgetJava() {
}

void PluginWidgetJava::paint(
    GraphicsContext *context,
    const IntRect &rc //page coordinates
){
    //Widget::paint(context, rc);
    /*
    if (!m_isStarted) {
        // Draw the "missing plugin" image
        paintMissingPluginIcon(context, rect);
        return;
    }
    */
    if (context->paintingDisabled())
        return;

    jobject obj = platformWidget();
    if(NULL!=obj){
        JNIEnv *env = WebCore_GetJavaEnv();
        context->save();
        env->CallVoidMethod(
            obj,
            pluginWidgetPaintMID,
            context->platformContext(),
            rc.x(), rc.y(), rc.width(), rc.height());
        context->restore();
    }
}


void PluginWidgetJava::convertToPage(IntRect& rect)
{
    if (!isVisible())
        return;

    if (!m_element || !m_element->renderer())
        return;

    RenderBox* renderer = toRenderBox(m_element->renderer());
    if(renderer){
       LayoutSize offset(
           renderer->offsetFromContainer(renderer->container(), LayoutPoint()));
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

    ASSERT(parent()->isFrameView());

    FrameView* frameView = static_cast<FrameView*>(parent());
    IntRect windowRect(frameView->contentsToWindow(frameRect().location()), frameRect().size());
    jobject obj = platformWidget();
    if( NULL!=obj ){
        JNIEnv *env = WebCore_GetJavaEnv();
        env->CallVoidMethod(
            obj,
            pluginWidgetFWKSetNativeContainerBoundsMID,
            (jint)windowRect.x(),
            (jint)windowRect.y(),
            (jint)windowRect.width(),
            (jint)windowRect.height());

    }
}


void PluginWidgetJava::invalidateWindowlessPluginRect(
    const IntRect& rect //client coordinates
){
    if (!isVisible())
        return;

    if (!m_element || !m_element->renderer())
        return;

    RenderBox* renderer = toRenderBox(m_element->renderer());
    if(renderer){
        renderer->repaintRectangle(rect);
    }
}

//look at "void PluginView::focusPluginElement()"
void PluginWidgetJava::focusPluginElement(bool isFocused)
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

void PluginWidgetJava::handleEvent(Event *event)
{
    JNIEnv *env = WebCore_GetJavaEnv();
    jobject obj = platformWidget();
    jboolean cancelBubble = false;
    if(NULL!=obj){
        //Frame *fr1 = Frame::frameForWidget(this);
        //Frame *fr2 = static_cast<FrameView*>(parent())->frame();
        if( event->type() == eventNames().blurEvent || event->type() == eventNames().focusEvent){

        } if( event->isMouseEvent() ){
            MouseEvent *me = (MouseEvent *)event;
            if(NULL!=obj){
                //look at "void PluginView::handleMouseEvent(MouseEvent* event)"
                //takes into account zoomFactor for offsetX, offsetY
                IntPoint p = static_cast<FrameView*>(parent())->contentsToWindow(
                    IntPoint(me->pageX(), me->pageY()));
                cancelBubble = env->CallBooleanMethod(
                    obj,
                    pluginWidgetFWKHandleMouseEventMID,
                    (jstring)me->type().string().toJavaString(env),
                    (jint)p.x(),
                    (jint)p.y(),
                    (jint)me->screenX(),
                    (jint)me->screenY(),
                    (jint)me->button(),
                    (jboolean)me->buttonDown(),
                    (jboolean)me->altKey(),
                    (jboolean)me->metaKey(),
                    (jboolean)me->ctrlKey(),
                    (jboolean)me->shiftKey(),
                    (jlong)me->timeStamp() );
            }
        }
    }
    if(cancelBubble) {
        event->setDefaultHandled();
        event->cancelBubble();
    } else {
        Widget::handleEvent(event);
    }
}

}
