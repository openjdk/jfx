/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "ContextMenu.h"
#include "ContextMenuItem.h"
#include "Frame.h"
#include "FrameView.h"
#include "JavaEnv.h"
#include "Page.h"
#include "WebPage.h"

#include "com_sun_webkit_ContextMenu.h"

#include "FrameTree.h"

static jclass getJContextMenuClass()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static JGClass jContextMenuClass(env->FindClass("com/sun/webkit/ContextMenu"));
    ASSERT(jContextMenuClass);
    return (jclass)jContextMenuClass;
}

static JLObject createPlatformMenuDescription()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getJContextMenuClass(),
            "fwkCreateContextMenu",
            "()Lcom/sun/webkit/ContextMenu;");
    ASSERT(mid);

    JLObject jContextMenu(env->CallStaticObjectMethod(getJContextMenuClass(), mid));
    ASSERT(jContextMenu);
    CheckAndClearException(env);

    return jContextMenu;
}

namespace WebCore {

// ContextMenu is a utility class to create/configure instance of
// PlatformMenuDescription.  So, instance of ContextMenu owns
// instance of PlatformMenuDescription which is stored in
// m_platformDescription field. It can loose the ownership or
// get ownership of another PlatformMenuDescription and
// PlatformMenuItemDescription. See method's comments for more
// information about the ownership.
ContextMenu::ContextMenu()
    : m_platformDescription(createPlatformMenuDescription())
{
}

ContextMenu::ContextMenu(const PlatformMenuDescription descr)
    : m_platformDescription(descr)
{
    // Note: the ctor seems to be useless.
}

ContextMenu::~ContextMenu()
{
}

// This method transfers ownership of platform description of provided CMI
// from the CMI to this ContextMenu.
// Note: the method is not called.
void ContextMenu::insertItem(unsigned position, ContextMenuItem& menuItem)
{
    if (!m_platformDescription || !menuItem.isSupportedByPlatform()) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJContextMenuClass(),
        "fwkInsertItem", "(Lcom/sun/webkit/ContextMenuItem;I)V");
    ASSERT(mid);

    JLObject item(menuItem.releasePlatformDescription());
    env->CallVoidMethod(m_platformDescription, mid, (jobject)item, position);
    CheckAndClearException(env);
}


// This method transfers ownership of platform description of provided CMI
// from the CMI to this ContextMenu.
void ContextMenu::appendItem(ContextMenuItem& menuItem)
{
    if (!m_platformDescription || !menuItem.isSupportedByPlatform()) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJContextMenuClass(),
        "fwkAppendItem", "(Lcom/sun/webkit/ContextMenuItem;)V");
    ASSERT(mid);

    JLObject item(menuItem.releasePlatformDescription());
    env->CallVoidMethod(m_platformDescription, mid, (jobject)item);
    CheckAndClearException(env);
}

// This method should find an item with specified action, clone it,
// and return ContextMenuItem wrapped around this clone.
ContextMenuItem* ContextMenu::itemWithAction(unsigned)
{
    // most likely we do not need this method.  at least for now it
    // is only used in Windows port of WebKit
    notImplemented();
    return 0;
}

ContextMenuItem* ContextMenu::itemAtIndex(unsigned, const PlatformMenuDescription)
{
    // we do not need to implement this method since it is only used in Windows port
    // see http://bugs.webkit.org/show_bug.cgi?id=17366
    notImplemented();
    return 0;
}

unsigned ContextMenu::itemCount() const
{
    if (!m_platformDescription) {
        return 0;
    }

    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJContextMenuClass(),
        "fwkGetItemCount", "()I");
    ASSERT(mid);

    jint count = env->CallIntMethod(m_platformDescription, mid);
    CheckAndClearException(env);

    return count;
}

void ContextMenu::show(ContextMenuController* ctrl, const IntPoint& loc)
{
    ASSERT(m_platformDescription);
    if (m_platformDescription) {
        JNIEnv* env = WebCore_GetJavaEnv();

        static jmethodID mid = env->GetMethodID(
                getJContextMenuClass(),
                "fwkShow",
                "(Lcom/sun/webkit/WebPage;JII)V");
        ASSERT(mid);

        env->CallVoidMethod(
                m_platformDescription,
                mid,
                (jobject) WebPage::jobjectFromPage(ctrl->page()),
                ptr_to_jlong(ctrl),
                loc.x(),
                loc.y());
        CheckAndClearException(env);
    }
}

PlatformMenuDescription ContextMenu::platformDescription() const
{
    return m_platformDescription;
}

void ContextMenu::setPlatformDescription(PlatformMenuDescription descr)
{
    if (descr != m_platformDescription) {
        m_platformDescription = descr;
    }
}

Vector<ContextMenuItem> contextMenuItemVector(PlatformMenuDescription menu)
{
    Vector<ContextMenuItem> menuItemVector;
    notImplemented();
    return menuItemVector;
}

// Returns (lost ownership of) current platform description
// and reset the state of the CM to the default one.
PlatformMenuDescription ContextMenu::releasePlatformDescription()
{
    PlatformMenuDescription descr = m_platformDescription;
    m_platformDescription = createPlatformMenuDescription();
    return descr;
}
} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_ContextMenu_twkHandleItemSelected
    (JNIEnv* env, jobject self, jlong menuCtrlPData, jint itemAction)
{
    ContextMenuController* cmc = static_cast<ContextMenuController*>jlong_to_ptr(menuCtrlPData);

    // This item is used to pass the action to the menu controller.
    // TODO: this doesn't look good, consider refactoring.
    static ContextMenuItem contextMenuItem(ActionType, ContextMenuItemTagNoAction, String("aux"));
    contextMenuItem.setAction((ContextMenuAction)itemAction);

    cmc->contextMenuItemSelected(&contextMenuItem);
}

#ifdef __cplusplus
}
#endif
