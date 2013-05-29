/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "ContextMenu.h"
#include "ContextMenuItem.h"
#include "JavaEnv.h"

#include "com_sun_webkit_ContextMenuItem.h"

static JGClass jContextMenuItemClass;
static jmethodID createContextMenuItemMID = 0;
static jmethodID getTypeMID = 0;
static jmethodID setTypeMID = 0;
static jmethodID getActionMID = 0;
static jmethodID setActionMID = 0;
static jmethodID getTitleMID = 0;
static jmethodID setTitleMID = 0;
static jmethodID isEnabledMID = 0;
static jmethodID setEnabledMID = 0;
static jmethodID setCheckedMID = 0;
static jmethodID setSubmenuMID = 0;
static jmethodID getSubmenuMID = 0;

namespace WebCore {

static PlatformMenuItemDescription createPlatformMenuItemDescription()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!jContextMenuItemClass) {
        jContextMenuItemClass  = JLClass(env->FindClass("com/sun/webkit/ContextMenuItem"));
        ASSERT(jContextMenuItemClass);

        createContextMenuItemMID = env->GetStaticMethodID(jContextMenuItemClass, "fwkCreateContextMenuItem",
                                                          "()Lcom/sun/webkit/ContextMenuItem;");
        ASSERT(createContextMenuItemMID);

        getTypeMID = env->GetMethodID(jContextMenuItemClass, "fwkGetType", "()I");
        ASSERT(getTypeMID);

        setTypeMID = env->GetMethodID(jContextMenuItemClass, "fwkSetType", "(I)V");
        ASSERT(setTypeMID);

        getActionMID = env->GetMethodID(jContextMenuItemClass, "fwkGetAction", "()I");
        ASSERT(getActionMID);

        setActionMID = env->GetMethodID(jContextMenuItemClass, "fwkSetAction", "(I)V");
        ASSERT(setActionMID);

        getTitleMID = env->GetMethodID(jContextMenuItemClass, "fwkGetTitle", "()Ljava/lang/String;");
        ASSERT(getTitleMID);

        setTitleMID = env->GetMethodID(jContextMenuItemClass, "fwkSetTitle", "(Ljava/lang/String;)V");
        ASSERT(setTitleMID);

        isEnabledMID = env->GetMethodID(jContextMenuItemClass, "fwkIsEnabled", "()Z");
        ASSERT(isEnabledMID);

        setEnabledMID = env->GetMethodID(jContextMenuItemClass, "fwkSetEnabled", "(Z)V");
        ASSERT(setEnabledMID);

        setCheckedMID = env->GetMethodID(jContextMenuItemClass, "fwkSetChecked", "(Z)V");
        ASSERT(setCheckedMID);

        getSubmenuMID = env->GetMethodID(jContextMenuItemClass, "fwkGetSubmenu",
                                         "()Lcom/sun/webkit/ContextMenu;");
        ASSERT(getSubmenuMID);

        setSubmenuMID = env->GetMethodID(jContextMenuItemClass, "fwkSetSubmenu",
                                         "(Lcom/sun/webkit/ContextMenu;)V");
        ASSERT(setSubmenuMID);
    }

    JGObject jContextMenuItem(env->CallStaticObjectMethod(jContextMenuItemClass, createContextMenuItemMID));
    CheckAndClearException(env);

    return jContextMenuItem;
}

/*
 * ContextMenuItem is an utility class to create/configure instance of
 * PlatformMenuItemDescription.  So, instance of ContextMenuItem owns
 * instance of PlatformMenuItemDescription which is stored in
 * m_platformDescription field.  It can loose the ownership or
 * get ownership of another PlatformMenuItemDescription and
 * PlatformMenuDescription.  See method' comments for more information
 * about the ownership.
 */
ContextMenuItem::ContextMenuItem(PlatformMenuItemDescription descr)
    : m_platformDescription(descr)
{
}


ContextMenuItem::ContextMenuItem(ContextMenu* subMenu)
{
    if (!subMenu->itemCount()) {
        // m_platformDescription is left empty, meaning the item is not supported by platform
        return;
    }
    m_platformDescription = createPlatformMenuItemDescription();
    setType(SubmenuType);
    setAction(ContextMenuItemTagNoAction);
    setSubMenu(subMenu);
}

ContextMenuItem::ContextMenuItem(ContextMenuItemType type, ContextMenuAction action,
                                 const String& title, ContextMenu* subMenu)
{
    if (title.isEmpty()) {
        // m_platformDescription is left empty, meaning the item is not supported by platform
        return;
    }
    m_platformDescription = createPlatformMenuItemDescription();
    setType(type);
    setAction(action);
    setEnabled(true);
    setSubMenu(subMenu);
    setTitle(title);
}

ContextMenuItem::~ContextMenuItem()
{
}

/*
 * Returns (lost ownership of) current platform description
 * and rest state of the CM to default one.
  */
PlatformMenuItemDescription ContextMenuItem::releasePlatformDescription()
{
    PlatformMenuItemDescription descr = m_platformDescription;
    if (descr) {
        m_platformDescription = createPlatformMenuItemDescription();
    }
    return descr;
}

ContextMenuItemType ContextMenuItem::type() const
{
    if (!m_platformDescription) {
        return ActionType;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    jint jtype = env->CallIntMethod(m_platformDescription, getTypeMID);
    CheckAndClearException(env);
    switch (jtype) {
    case com_sun_webkit_ContextMenuItem_ACTION_TYPE:
        return ActionType;
    case com_sun_webkit_ContextMenuItem_SEPARATOR_TYPE:
        return SeparatorType;
    case com_sun_webkit_ContextMenuItem_SUBMENU_TYPE:
        return SubmenuType;
    default:
        ASSERT(false);
    }
    return ActionType;
}

void ContextMenuItem::setType(ContextMenuItemType type)
{
    if (!m_platformDescription) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    jint jtype = com_sun_webkit_ContextMenuItem_ACTION_TYPE;
    if (SeparatorType == type) {
        jtype = com_sun_webkit_ContextMenuItem_SEPARATOR_TYPE;
    } else if (SubmenuType == type) {
        jtype = com_sun_webkit_ContextMenuItem_SUBMENU_TYPE;
    }
    env->CallVoidMethod(m_platformDescription, setTypeMID, jtype);
    CheckAndClearException(env);
}

ContextMenuAction ContextMenuItem::action() const
{
    if (!m_platformDescription) {
        return ContextMenuItemTagNoAction;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    jint action = env->CallIntMethod(m_platformDescription, getActionMID);
    CheckAndClearException(env);
    return static_cast<ContextMenuAction>(action);
}

void ContextMenuItem::setAction(ContextMenuAction action)
{
    if (!m_platformDescription) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    env->CallVoidMethod(m_platformDescription, setActionMID, action);
    CheckAndClearException(env);
}

String ContextMenuItem::title() const
{
    if (!m_platformDescription) {
        return String(StringImpl::empty());
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    JLString jtitle(static_cast<jstring>(env->CallObjectMethod(m_platformDescription, getTitleMID)));
    CheckAndClearException(env);
    return String(env, jtitle);
}

void ContextMenuItem::setTitle(const String& title)
{
    if (!m_platformDescription) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    env->CallVoidMethod(m_platformDescription, setTitleMID, title.isEmpty() ? NULL : (jstring)title.toJavaString(env));
    CheckAndClearException(env);
}

PlatformMenuDescription ContextMenuItem::platformSubMenu() const
{
    if (!m_platformDescription) {
        return NULL;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    JLObject submenu(env->CallObjectMethod(m_platformDescription, getSubmenuMID));
    CheckAndClearException(env);

    return submenu;
}

/*
 * this method gets ownership of platform descripton of sepcified ContextMenu.
 */
void ContextMenuItem::setSubMenu(ContextMenu* subMenu)
{
    if (!m_platformDescription || !subMenu || !subMenu->itemCount()) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    JLObject submenu(subMenu->releasePlatformDescription());
    env->CallVoidMethod(m_platformDescription, setSubmenuMID, (jobject)submenu);
    CheckAndClearException(env);
}

void ContextMenuItem::setChecked(bool checked)
{
    if (!m_platformDescription) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    env->CallVoidMethod(m_platformDescription, setCheckedMID, bool_to_jbool(checked));
    CheckAndClearException(env);
}

bool ContextMenuItem::enabled() const
{
    if (!m_platformDescription) {
        return false;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    jboolean enabled = env->CallBooleanMethod(m_platformDescription, isEnabledMID);
    CheckAndClearException(env);
    return jbool_to_bool(enabled);
}

void ContextMenuItem::setEnabled(bool enabled)
{
    if (!m_platformDescription) {
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    env->CallVoidMethod(m_platformDescription, setEnabledMID, bool_to_jbool(enabled));
    CheckAndClearException(env);
}

} // namespace WebCore
