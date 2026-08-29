/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#undef IMPL


#include <WebCore/DOMException.h>
#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLInputElement.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>
#include <wtf/text/StringToIntegerConversion.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLInputElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAccept(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::acceptAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setAccept(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::acceptAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::altAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::altAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->autocomplete());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAutofocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setAutofocus(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDefaultChecked(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::checkedAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setDefaultChecked(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::checkedAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getChecked(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->checked();
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setChecked(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setChecked(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDirName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::dirnameAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setDirName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::dirnameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDisabled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setDisabled(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getForm(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLFormElement>(WTF::getPtr(IMPL->form()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormAction(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->formAction());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormEnctype(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->formEnctype());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormMethod(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->formMethod());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormNoValidate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::formnovalidateAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setFormNoValidate(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::formnovalidateAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::formtargetAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setFormTarget(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::formtargetAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getHeight(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->height();
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getIndeterminate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->indeterminate();
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setIndeterminate(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setIndeterminate(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMax(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::maxAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setMax(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::maxAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMaxLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->maxLength();
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setMaxLength(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setMaxLength(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::minAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setMin(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::minAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMultiple(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::multipleAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setMultiple(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::multipleAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getNameAttribute());
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getPattern(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::patternAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setPattern(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::patternAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getPlaceholder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::placeholderAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setPlaceholder(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::placeholderAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getReadOnly(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::readonlyAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setReadOnly(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::readonlyAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getRequired(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setRequired(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getSize(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, String::number(IMPL->size()));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setSize(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSize(parseIntegerAllowingTrailingJunk<int>(StringView(WKJString(value, value_length))).value());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getURLAttribute(WebCore::HTMLNames::srcAttr).string());
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::srcAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getStep(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::stepAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setStep(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::stepAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->value());
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getValueAsDate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->valueAsDate().approximateMonotonicTime().get_time_value();
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueAsDate(int64_t peer, int64_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    double convertedValue = static_cast<double>(value);
    IMPL->setValueAsDate(value);
}

WKJ_EXPORT double wkj_dom_HTMLInputElement_getValueAsNumber(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->valueAsNumber();
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueAsNumber(int64_t peer, double value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setValueAsNumber(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getWidth(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->width();
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getWillValidate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->validationMessage());
}

WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getLabels(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->labels()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getUseMap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::usemapAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setUseMap(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::usemapAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

WKJ_EXPORT void wkj_dom_HTMLInputElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, AtomString {WKJString(value, value_length)});
}


// Functions
WKJ_EXPORT void wkj_dom_HTMLInputElement_stepUp(int64_t peer, int32_t n)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->stepUp(n));
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_stepDown(int64_t peer, int32_t n)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->stepDown(n));
}


WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_checkValidity(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(AtomString {WKJString(error, error_length)});
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_select(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->select();
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_setRangeText(int64_t peer, const uint16_t* replacement, int32_t replacement_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setRangeText(AtomString {WKJString(replacement, replacement_length)}));
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_setRangeTextEx(int64_t peer, const uint16_t* replacement, int32_t replacement_length, int32_t start, int32_t end, const uint16_t* selectionMode, int32_t selectionMode_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setRangeText(AtomString {WKJString(replacement, replacement_length)}
            , start
            , end
            , AtomString{WKJString(selectionMode, selectionMode_length)}));
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_click(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->click();
}


WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueForUser(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setValueForUser(AtomString{WKJString(value, value_length)});
}


}
