/*
 * Copyright (C) 2007-2019 Apple Inc. All rights reserved.
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include "config.h"
#include "JSDOMWindowCustom.h"

#include "DOMWindowIndexedDatabase.h"
#include "DOMWindowWebDatabase.h"
#include "Frame.h"
#include "HTMLCollection.h"
#include "HTMLDocument.h"
#include "HTMLFrameOwnerElement.h"
#include "HTTPParsers.h"
#include "JSDOMBindingSecurity.h"
#include "JSDOMConvertNullable.h"
#include "JSDOMConvertNumbers.h"
#include "JSDOMConvertStrings.h"
#include "JSDatabase.h"
#include "JSDatabaseCallback.h"
#include "JSEvent.h"
#include "JSEventListener.h"
#include "JSHTMLAudioElement.h"
#include "JSHTMLCollection.h"
#include "JSHTMLOptionElement.h"
#include "JSIDBFactory.h"
#include "JSRemoteDOMWindow.h"
#include "JSWindowProxy.h"
#include "JSWorker.h"
#include "Location.h"
#include "RuntimeEnabledFeatures.h"
#include "ScheduledAction.h"
#include "Settings.h"
#include "WebCoreJSClientData.h"
#include <JavaScriptCore/BuiltinNames.h>
#include <JavaScriptCore/HeapAnalyzer.h>
#include <JavaScriptCore/JSCInlines.h>
#include <JavaScriptCore/JSFunction.h>
#include <JavaScriptCore/JSMicrotask.h>
#include <JavaScriptCore/Lookup.h>
#include <JavaScriptCore/Structure.h>

#if ENABLE(USER_MESSAGE_HANDLERS)
#include "JSWebKitNamespace.h"
#endif


namespace WebCore {
using namespace JSC;

EncodedJSValue JSC_HOST_CALL jsDOMWindowInstanceFunctionShowModalDialog(ExecState*);
EncodedJSValue JSC_HOST_CALL jsDOMWindowInstanceFunctionOpenDatabase(ExecState*);

void JSDOMWindow::visitAdditionalChildren(SlotVisitor& visitor)
{
    if (Frame* frame = wrapped().frame())
        visitor.addOpaqueRoot(frame);

    visitor.addOpaqueRoot(&wrapped());

    // Normally JSEventTargetCustom.cpp's JSEventTarget::visitAdditionalChildren() would call this. But
    // even though DOMWindow is an EventTarget, JSDOMWindow does not subclass JSEventTarget, so we need
    // to do this here.
    wrapped().visitJSEventListeners(visitor);
}

#if ENABLE(USER_MESSAGE_HANDLERS)
static EncodedJSValue jsDOMWindowWebKit(ExecState* exec, EncodedJSValue thisValue, PropertyName)
{
    VM& vm = exec->vm();
    JSDOMWindow* castedThis = toJSDOMWindow(vm, JSValue::decode(thisValue));
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, castedThis->wrapped()))
        return JSValue::encode(jsUndefined());
    return JSValue::encode(toJS(exec, castedThis->globalObject(), castedThis->wrapped().webkitNamespace()));
}
#endif

template <DOMWindowType windowType>
bool jsDOMWindowGetOwnPropertySlotRestrictedAccess(JSDOMGlobalObject* thisObject, AbstractDOMWindow& window, ExecState& state, PropertyName propertyName, PropertySlot& slot, const String& errorMessage)
{
    VM& vm = state.vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    auto& builtinNames = static_cast<JSVMClientData*>(vm.clientData)->builtinNames();

    // https://html.spec.whatwg.org/#crossorigingetownpropertyhelper-(-o,-p-)

    // These are the functions we allow access to cross-origin (DoNotCheckSecurity in IDL).
    // Always provide the original function, on a fresh uncached function object.
    if (propertyName == builtinNames.blurPublicName()) {
        slot.setCustom(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontEnum), windowType == DOMWindowType::Remote ? nonCachingStaticFunctionGetter<jsRemoteDOMWindowInstanceFunctionBlur, 0> : nonCachingStaticFunctionGetter<jsDOMWindowInstanceFunctionBlur, 0>);
        return true;
    }
    if (propertyName == builtinNames.closePublicName()) {
        slot.setCustom(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontEnum), windowType == DOMWindowType::Remote ? nonCachingStaticFunctionGetter<jsRemoteDOMWindowInstanceFunctionClose, 0> : nonCachingStaticFunctionGetter<jsDOMWindowInstanceFunctionClose, 0>);
        return true;
    }
    if (propertyName == builtinNames.focusPublicName()) {
        slot.setCustom(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontEnum), windowType == DOMWindowType::Remote ? nonCachingStaticFunctionGetter<jsRemoteDOMWindowInstanceFunctionFocus, 0> : nonCachingStaticFunctionGetter<jsDOMWindowInstanceFunctionFocus, 0>);
        return true;
    }
    if (propertyName == builtinNames.postMessagePublicName()) {
        slot.setCustom(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontEnum), windowType == DOMWindowType::Remote ? nonCachingStaticFunctionGetter<jsRemoteDOMWindowInstanceFunctionPostMessage, 0> : nonCachingStaticFunctionGetter<jsDOMWindowInstanceFunctionPostMessage, 2>);
        return true;
    }

    // When accessing cross-origin known Window properties, we always use the original property getter,
    // even if the property was removed / redefined. As of early 2016, this matches Firefox and Chrome's
    // behavior.
    auto* classInfo = windowType == DOMWindowType::Remote ? JSRemoteDOMWindow::info() : JSDOMWindow::info();
    if (auto* entry = classInfo->staticPropHashTable->entry(propertyName)) {
        // Only allow access to these specific properties.
        if (propertyName == builtinNames.locationPublicName()
            || propertyName == builtinNames.closedPublicName()
            || propertyName == vm.propertyNames->length
            || propertyName == builtinNames.selfPublicName()
            || propertyName == builtinNames.windowPublicName()
            || propertyName == builtinNames.framesPublicName()
            || propertyName == builtinNames.openerPublicName()
            || propertyName == builtinNames.parentPublicName()
            || propertyName == builtinNames.topPublicName()) {
            bool shouldExposeSetter = propertyName == builtinNames.locationPublicName();
            CustomGetterSetter* customGetterSetter = CustomGetterSetter::create(vm, entry->propertyGetter(), shouldExposeSetter ? entry->propertyPutter() : nullptr);
            slot.setCustomGetterSetter(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::CustomAccessor | JSC::PropertyAttribute::DontEnum), customGetterSetter);
            return true;
        }

        // For any other entries in the static property table, deny access. (Early return also prevents
        // named getter from returning frames with matching names - this seems a little questionable, see
        // FIXME comment on prototype search below.)
        throwSecurityError(state, scope, errorMessage);
        slot.setUndefined();
        return false;
    }

    // Check for child frames by name before built-in properties to match Mozilla. This does
    // not match IE, but some sites end up naming frames things that conflict with window
    // properties that are in Moz but not IE. Since we have some of these, we have to do it
    // the Moz way.
    // FIXME: Add support to named attributes on RemoteFrames.
    auto* frame = window.frame();
    if (frame && is<Frame>(*frame)) {
        if (auto* scopedChild = downcast<Frame>(*frame).tree().scopedChild(propertyNameToAtomString(propertyName))) {
            slot.setValue(thisObject, JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontDelete | JSC::PropertyAttribute::DontEnum, toJS(&state, scopedChild->document()->domWindow()));
            return true;
        }
    }

    if (handleCommonCrossOriginProperties(thisObject, vm, propertyName, slot))
        return true;

    throwSecurityError(state, scope, errorMessage);
    slot.setUndefined();
    return false;
}
template bool jsDOMWindowGetOwnPropertySlotRestrictedAccess<DOMWindowType::Local>(JSDOMGlobalObject*, AbstractDOMWindow&, ExecState&, PropertyName, PropertySlot&, const String&);
template bool jsDOMWindowGetOwnPropertySlotRestrictedAccess<DOMWindowType::Remote>(JSDOMGlobalObject*, AbstractDOMWindow&, ExecState&, PropertyName, PropertySlot&, const String&);

// https://html.spec.whatwg.org/#crossorigingetownpropertyhelper-(-o,-p-)
bool handleCommonCrossOriginProperties(JSObject* thisObject, VM& vm, PropertyName propertyName, PropertySlot& slot)
{
    auto& propertyNames =  vm.propertyNames;
    if (propertyName == propertyNames->builtinNames().thenPublicName() || propertyName == propertyNames->toStringTagSymbol || propertyName == propertyNames->hasInstanceSymbol || propertyName == propertyNames->isConcatSpreadableSymbol) {
        slot.setValue(thisObject, JSC::PropertyAttribute::ReadOnly | JSC::PropertyAttribute::DontEnum, jsUndefined());
        return true;
    }
    return false;
}

// Property access sequence is:
// (1) indexed properties,
// (2) regular own properties,
// (3) named properties (in fact, these shouldn't be on the window, should be on the NPO).
bool JSDOMWindow::getOwnPropertySlot(JSObject* object, ExecState* state, PropertyName propertyName, PropertySlot& slot)
{
    // (1) First, indexed properties.
    // Hand off all indexed access to getOwnPropertySlotByIndex, which supports the indexed getter.
    if (Optional<unsigned> index = parseIndex(propertyName))
        return getOwnPropertySlotByIndex(object, state, index.value(), slot);

    auto* thisObject = jsCast<JSDOMWindow*>(object);

    // Hand off all cross-domain access to jsDOMWindowGetOwnPropertySlotRestrictedAccess.
    String errorMessage;
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(*state, thisObject->wrapped(), errorMessage))
        return jsDOMWindowGetOwnPropertySlotRestrictedAccess<DOMWindowType::Local>(thisObject, thisObject->wrapped(), *state, propertyName, slot, errorMessage);

    // FIXME: this needs more explanation.
    // (Particularly, is it correct that this exists here but not in getOwnPropertySlotByIndex?)
    slot.setWatchpointSet(thisObject->m_windowCloseWatchpoints);

    // (2) Regular own properties.
    PropertySlot slotCopy = slot;
    if (Base::getOwnPropertySlot(thisObject, state, propertyName, slot)) {
        auto* frame = thisObject->wrapped().frame();

        // Detect when we're getting the property 'showModalDialog', this is disabled, and has its original value.
        bool isShowModalDialogAndShouldHide = propertyName == static_cast<JSVMClientData*>(state->vm().clientData)->builtinNames().showModalDialogPublicName()
            && (!frame || !DOMWindow::canShowModalDialog(*frame))
            && slot.isValue() && isHostFunction(slot.getValue(state, propertyName), jsDOMWindowInstanceFunctionShowModalDialog);
        // Unless we're in the showModalDialog special case, we're done.
        if (!isShowModalDialogAndShouldHide)
            return true;
        slot = slotCopy;
    }

#if ENABLE(USER_MESSAGE_HANDLERS)
    if (propertyName == static_cast<JSVMClientData*>(state->vm().clientData)->builtinNames().webkitPublicName() && thisObject->wrapped().shouldHaveWebKitNamespaceForWorld(thisObject->world())) {
        slot.setCacheableCustom(thisObject, JSC::PropertyAttribute::DontDelete | JSC::PropertyAttribute::ReadOnly, jsDOMWindowWebKit);
        return true;
    }
#endif

    return false;
}

// Property access sequence is:
// (1) indexed properties,
// (2) regular own properties,
// (3) named properties (in fact, these shouldn't be on the window, should be on the NPO).
bool JSDOMWindow::getOwnPropertySlotByIndex(JSObject* object, ExecState* state, unsigned index, PropertySlot& slot)
{
    VM& vm = state->vm();
    auto* thisObject = jsCast<JSDOMWindow*>(object);
    auto& window = thisObject->wrapped();
    auto* frame = window.frame();

    // Indexed getters take precendence over regular properties, so caching would be invalid.
    slot.disableCaching();

    String errorMessage;
    Optional<bool> cachedIsCrossOriginAccess;
    auto isCrossOriginAccess = [&] {
        if (!cachedIsCrossOriginAccess)
            cachedIsCrossOriginAccess = !BindingSecurity::shouldAllowAccessToDOMWindow(*state, window, errorMessage);
        return *cachedIsCrossOriginAccess;
    };

    // (1) First, indexed properties.
    // These are also allowed cross-origin, so come before the access check.
    if (frame && index < frame->tree().scopedChildCount()) {
        slot.setValue(thisObject, static_cast<unsigned>(JSC::PropertyAttribute::ReadOnly), toJS(state, frame->tree().scopedChild(index)->document()->domWindow()));
        return true;
    }

    // Hand off all cross-domain/frameless access to jsDOMWindowGetOwnPropertySlotRestrictedAccess.
    if (isCrossOriginAccess())
        return jsDOMWindowGetOwnPropertySlotRestrictedAccess<DOMWindowType::Local>(thisObject, window, *state, Identifier::from(vm, index), slot, errorMessage);

    // (2) Regular own properties.
    return Base::getOwnPropertySlotByIndex(thisObject, state, index, slot);
}

void JSDOMWindow::doPutPropertySecurityCheck(JSObject* cell, ExecState* state, PropertyName propertyName, PutPropertySlot&)
{
    VM& vm = state->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    auto* thisObject = jsCast<JSDOMWindow*>(cell);
    if (!thisObject->wrapped().frame())
        return;

    String errorMessage;
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(*state, thisObject->wrapped(), errorMessage)) {
        // We only allow setting "location" attribute cross-origin.
        if (propertyName == static_cast<JSVMClientData*>(vm.clientData)->builtinNames().locationPublicName())
            return;
        throwSecurityError(*state, scope, errorMessage);
        return;
    }
}

bool JSDOMWindow::put(JSCell* cell, ExecState* state, PropertyName propertyName, JSValue value, PutPropertySlot& slot)
{
    VM& vm = state->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    auto* thisObject = jsCast<JSDOMWindow*>(cell);
    if (!thisObject->wrapped().frame())
        return false;

    String errorMessage;
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(*state, thisObject->wrapped(), errorMessage)) {
        // We only allow setting "location" attribute cross-origin.
        if (propertyName == static_cast<JSVMClientData*>(vm.clientData)->builtinNames().locationPublicName()) {
            bool putResult = false;
            if (lookupPut(state, propertyName, thisObject, value, *s_info.staticPropHashTable, slot, putResult))
                return putResult;
            return false;
        }
        throwSecurityError(*state, scope, errorMessage);
        return false;
    }

    return Base::put(thisObject, state, propertyName, value, slot);
}

bool JSDOMWindow::putByIndex(JSCell* cell, ExecState* exec, unsigned index, JSValue value, bool shouldThrow)
{
    auto* thisObject = jsCast<JSDOMWindow*>(cell);
    if (!thisObject->wrapped().frame() || !BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped()))
        return false;

    return Base::putByIndex(thisObject, exec, index, value, shouldThrow);
}

bool JSDOMWindow::deleteProperty(JSCell* cell, ExecState* exec, PropertyName propertyName)
{
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(cell);
    // Only allow deleting properties by frames in the same origin.
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), ThrowSecurityError))
        return false;
    return Base::deleteProperty(thisObject, exec, propertyName);
}

bool JSDOMWindow::deletePropertyByIndex(JSCell* cell, ExecState* exec, unsigned propertyName)
{
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(cell);
    // Only allow deleting properties by frames in the same origin.
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), ThrowSecurityError))
        return false;
    return Base::deletePropertyByIndex(thisObject, exec, propertyName);
}

void JSDOMWindow::analyzeHeap(JSCell* cell, HeapAnalyzer& analyzer)
{
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(cell);
    auto& location = thisObject->wrapped().location();
    analyzer.setLabelForCell(cell, location.href());

    Base::analyzeHeap(cell, analyzer);
}

// https://html.spec.whatwg.org/#crossoriginproperties-(-o-)
template <CrossOriginObject objectType>
static void addCrossOriginPropertyNames(VM& vm, PropertyNameArray& propertyNames)
{
    auto& builtinNames = static_cast<JSVMClientData*>(vm.clientData)->builtinNames();
    switch (objectType) {
    case CrossOriginObject::Location: {
        static const Identifier* const properties[] = { &builtinNames.hrefPublicName(), &vm.propertyNames->replace };
        for (auto* property : properties)
            propertyNames.add(*property);
        break;
    }
    case CrossOriginObject::Window: {
        static const Identifier* const properties[] = {
            &builtinNames.blurPublicName(), &builtinNames.closePublicName(), &builtinNames.closedPublicName(),
            &builtinNames.focusPublicName(), &builtinNames.framesPublicName(), &vm.propertyNames->length,
            &builtinNames.locationPublicName(), &builtinNames.openerPublicName(), &builtinNames.parentPublicName(),
            &builtinNames.postMessagePublicName(), &builtinNames.selfPublicName(), &builtinNames.topPublicName(),
            &builtinNames.windowPublicName()
        };

        for (auto* property : properties)
            propertyNames.add(*property);
        break;
    }
    }
}

// https://html.spec.whatwg.org/#crossoriginownpropertykeys-(-o-)
template <CrossOriginObject objectType>
void addCrossOriginOwnPropertyNames(JSC::ExecState& state, JSC::PropertyNameArray& propertyNames)
{
    auto& vm = state.vm();
    addCrossOriginPropertyNames<objectType>(vm, propertyNames);

    static const Identifier* const properties[] = {
        &vm.propertyNames->builtinNames().thenPublicName(), &vm.propertyNames->toStringTagSymbol, &vm.propertyNames->hasInstanceSymbol, &vm.propertyNames->isConcatSpreadableSymbol
    };

    for (auto* property : properties)
        propertyNames.add(*property);

}
template void addCrossOriginOwnPropertyNames<CrossOriginObject::Window>(JSC::ExecState&, JSC::PropertyNameArray&);
template void addCrossOriginOwnPropertyNames<CrossOriginObject::Location>(JSC::ExecState&, JSC::PropertyNameArray&);

static void addScopedChildrenIndexes(ExecState& state, DOMWindow& window, PropertyNameArray& propertyNames)
{
    auto* document = window.document();
    if (!document)
        return;

    auto* frame = document->frame();
    if (!frame)
        return;

    VM& vm = state.vm();
    unsigned scopedChildCount = frame->tree().scopedChildCount();
    for (unsigned i = 0; i < scopedChildCount; ++i)
        propertyNames.add(Identifier::from(vm, i));
}

// https://html.spec.whatwg.org/#windowproxy-ownpropertykeys
void JSDOMWindow::getOwnPropertyNames(JSObject* object, ExecState* exec, PropertyNameArray& propertyNames, EnumerationMode mode)
{
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(object);

    addScopedChildrenIndexes(*exec, thisObject->wrapped(), propertyNames);

    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), DoNotReportSecurityError)) {
        if (mode.includeDontEnumProperties())
            addCrossOriginOwnPropertyNames<CrossOriginObject::Window>(*exec, propertyNames);
        return;
    }
    Base::getOwnPropertyNames(thisObject, exec, propertyNames, mode);
}

bool JSDOMWindow::defineOwnProperty(JSC::JSObject* object, JSC::ExecState* exec, JSC::PropertyName propertyName, const JSC::PropertyDescriptor& descriptor, bool shouldThrow)
{
    JSC::VM& vm = exec->vm();
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(object);
    // Only allow defining properties in this way by frames in the same origin, as it allows setters to be introduced.
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), ThrowSecurityError))
        return false;

    // Don't allow shadowing location using accessor properties.
    if (descriptor.isAccessorDescriptor() && propertyName == Identifier::fromString(vm, "location"))
        return false;

    return Base::defineOwnProperty(thisObject, exec, propertyName, descriptor, shouldThrow);
}

JSValue JSDOMWindow::getPrototype(JSObject* object, ExecState* exec)
{
    JSDOMWindow* thisObject = jsCast<JSDOMWindow*>(object);
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), DoNotReportSecurityError))
        return jsNull();

    return Base::getPrototype(object, exec);
}

bool JSDOMWindow::preventExtensions(JSObject*, ExecState* exec)
{
    auto scope = DECLARE_THROW_SCOPE(exec->vm());

    throwTypeError(exec, scope, "Cannot prevent extensions on this object"_s);
    return false;
}

String JSDOMWindow::toStringName(const JSObject* object, ExecState* exec)
{
    auto* thisObject = jsCast<const JSDOMWindow*>(object);
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(exec, thisObject->wrapped(), DoNotReportSecurityError))
        return "Object"_s;
    return "Window"_s;
}

// Custom Attributes

JSValue JSDOMWindow::event(ExecState& state) const
{
    Event* event = currentEvent();
    if (!event)
        return jsUndefined();
    return toJS(&state, const_cast<JSDOMWindow*>(this), event);
}

// Custom functions

class DialogHandler {
public:
    explicit DialogHandler(ExecState& exec)
        : m_exec(exec)
    {
    }

    void dialogCreated(DOMWindow&);
    JSValue returnValue() const;

private:
    ExecState& m_exec;
    RefPtr<Frame> m_frame;
};

inline void DialogHandler::dialogCreated(DOMWindow& dialog)
{
    VM& vm = m_exec.vm();
    m_frame = dialog.frame();

    // FIXME: This looks like a leak between the normal world and an isolated
    //        world if dialogArguments comes from an isolated world.
    JSDOMWindow* globalObject = toJSDOMWindow(m_frame.get(), normalWorld(vm));
    if (JSValue dialogArguments = m_exec.argument(1))
        globalObject->putDirect(vm, Identifier::fromString(vm, "dialogArguments"), dialogArguments);
}

inline JSValue DialogHandler::returnValue() const
{
    VM& vm = m_exec.vm();
    JSDOMWindow* globalObject = toJSDOMWindow(m_frame.get(), normalWorld(vm));
    if (!globalObject)
        return jsUndefined();
    Identifier identifier = Identifier::fromString(vm, "returnValue");
    PropertySlot slot(globalObject, PropertySlot::InternalMethodType::Get);
    if (!JSGlobalObject::getOwnPropertySlot(globalObject, &m_exec, identifier, slot))
        return jsUndefined();
    return slot.getValue(&m_exec, identifier);
}

JSValue JSDOMWindow::showModalDialog(ExecState& state)
{
    VM& vm = state.vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    if (UNLIKELY(state.argumentCount() < 1))
        return throwException(&state, scope, createNotEnoughArgumentsError(&state));

    String urlString = convert<IDLNullable<IDLDOMString>>(state, state.argument(0));
    RETURN_IF_EXCEPTION(scope, JSValue());
    String dialogFeaturesString = convert<IDLNullable<IDLDOMString>>(state, state.argument(2));
    RETURN_IF_EXCEPTION(scope, JSValue());

    DialogHandler handler(state);

    wrapped().showModalDialog(urlString, dialogFeaturesString, activeDOMWindow(state), firstDOMWindow(state), [&handler](DOMWindow& dialog) {
        handler.dialogCreated(dialog);
    });

    return handler.returnValue();
}

JSValue JSDOMWindow::queueMicrotask(ExecState& state)
{
    VM& vm = state.vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    if (UNLIKELY(state.argumentCount() < 1))
        return throwException(&state, scope, createNotEnoughArgumentsError(&state));

    JSValue functionValue = state.uncheckedArgument(0);
    if (UNLIKELY(!functionValue.isFunction(vm)))
        return JSValue::decode(throwArgumentMustBeFunctionError(state, scope, 0, "callback", "Window", "queueMicrotask"));

    scope.release();
    Base::queueMicrotask(JSC::createJSMicrotask(vm, functionValue));
    return jsUndefined();
}

DOMWindow* JSDOMWindow::toWrapped(VM& vm, JSValue value)
{
    if (!value.isObject())
        return nullptr;
    JSObject* object = asObject(value);
    if (object->inherits<JSDOMWindow>(vm))
        return &jsCast<JSDOMWindow*>(object)->wrapped();
    if (object->inherits<JSWindowProxy>(vm)) {
        if (auto* jsDOMWindow = jsDynamicCast<JSDOMWindow*>(vm, jsCast<JSWindowProxy*>(object)->window()))
            return &jsDOMWindow->wrapped();
    }
    return nullptr;
}

void JSDOMWindow::setOpener(JSC::ExecState& state, JSC::JSValue value)
{
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(&state, wrapped(), ThrowSecurityError))
        return;

    if (value.isNull()) {
        wrapped().disownOpener();
        return;
    }
    VM& vm = state.vm();
    replaceStaticPropertySlot(vm, this, Identifier::fromString(vm, "opener"), value);
}

JSValue JSDOMWindow::self(JSC::ExecState&) const
{
    return globalThis();
}

JSValue JSDOMWindow::window(JSC::ExecState&) const
{
    return globalThis();
}

JSValue JSDOMWindow::frames(JSC::ExecState&) const
{
    return globalThis();
}

static inline JSC::EncodedJSValue jsDOMWindowInstanceFunctionOpenDatabaseBody(JSC::ExecState* state, typename IDLOperation<JSDOMWindow>::ClassParameter castedThis, JSC::ThrowScope& throwScope)
{
    if (!BindingSecurity::shouldAllowAccessToDOMWindow(state, castedThis->wrapped(), ThrowSecurityError))
        return JSValue::encode(jsUndefined());
    auto& impl = castedThis->wrapped();
    if (UNLIKELY(state->argumentCount() < 4))
        return throwVMError(state, throwScope, createNotEnoughArgumentsError(state));
    auto name = convert<IDLDOMString>(*state, state->uncheckedArgument(0));
    RETURN_IF_EXCEPTION(throwScope, encodedJSValue());
    auto version = convert<IDLDOMString>(*state, state->uncheckedArgument(1));
    RETURN_IF_EXCEPTION(throwScope, encodedJSValue());
    auto displayName = convert<IDLDOMString>(*state, state->uncheckedArgument(2));
    RETURN_IF_EXCEPTION(throwScope, encodedJSValue());
    auto estimatedSize = convert<IDLUnsignedLong>(*state, state->uncheckedArgument(3));
    RETURN_IF_EXCEPTION(throwScope, encodedJSValue());

    if (!RuntimeEnabledFeatures::sharedFeatures().webSQLEnabled()) {
        if (name != "null" || version != "null" || displayName != "null" || estimatedSize)
            propagateException(*state, throwScope, Exception(UnknownError, "Web SQL is deprecated"_s));
        return JSValue::encode(constructEmptyObject(state, castedThis->globalObject()->objectPrototype()));
    }

    auto creationCallback = convert<IDLNullable<IDLCallbackFunction<JSDatabaseCallback>>>(*state, state->argument(4), *castedThis->globalObject(), [](JSC::ExecState& state, JSC::ThrowScope& scope) {
        throwArgumentMustBeFunctionError(state, scope, 4, "creationCallback", "Window", "openDatabase");
    });
    RETURN_IF_EXCEPTION(throwScope, encodedJSValue());
    return JSValue::encode(toJS<IDLNullable<IDLInterface<Database>>>(*state, *castedThis->globalObject(), throwScope, WebCore::DOMWindowWebDatabase::openDatabase(impl, WTFMove(name), WTFMove(version), WTFMove(displayName), WTFMove(estimatedSize), WTFMove(creationCallback))));
}

template<> inline JSDOMWindow* IDLOperation<JSDOMWindow>::cast(ExecState& state)
{
    return toJSDOMWindow(state.vm(), state.thisValue().toThis(&state, NotStrictMode));
}

EncodedJSValue JSC_HOST_CALL jsDOMWindowInstanceFunctionOpenDatabase(JSGlobalObject*, CallFrame* callFrame)
{
    return IDLOperation<JSDOMWindow>::call<jsDOMWindowInstanceFunctionOpenDatabaseBody>(*callFrame, "openDatabase");
}

// // FIXME-java: Currently WebSQL feature is not implemented completely in JavaFX port.
// #if !PLATFORM(JAVA)
// JSValue JSDOMWindow::openDatabase(JSC::ExecState& state) const
// {
//     VM& vm = state.vm();
//     StringImpl* name = PropertyName(static_cast<JSVMClientData*>(vm.clientData)->builtinNames().openDatabasePublicName()).publicName();
//     if (RuntimeEnabledFeatures::sharedFeatures().webSQLEnabled())
//         return JSFunction::create(vm, state.lexicalGlobalObject(), 4, name, jsDOMWindowInstanceFunctionOpenDatabase, NoIntrinsic);

//     return JSFunction::createFunctionThatMasqueradesAsUndefined(vm, state.lexicalGlobalObject(), 4, name, jsDOMWindowInstanceFunctionOpenDatabase, NoIntrinsic);
// }

// void JSDOMWindow::setOpenDatabase(JSC::ExecState& state, JSC::JSValue value)
// {
//     if (!BindingSecurity::shouldAllowAccessToDOMWindow(&state, wrapped(), ThrowSecurityError))
//         return;

//     VM& vm = state.vm();
//     replaceStaticPropertySlot(vm, this, Identifier::fromString(vm, "openDatabase"), value);
// }
// #endif

} // namespace WebCore
