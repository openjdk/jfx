/*
 *  Copyright (C) 2005-2023 Apple Inc. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public License
 *  along with this library; see the file COPYING.LIB.  If not, write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 *  Boston, MA 02110-1301, USA.
 *
 */

#pragma once

#include <wtf/Platform.h>

#if USE(CF) || defined(__OBJC__)

#include <algorithm>
#include <cstddef>
#include <wtf/HashTraits.h>
#include <wtf/NeverDestroyed.h>

#if USE(CF)
#include <CoreFoundation/CoreFoundation.h>
#endif

#ifdef __OBJC__
#import <Foundation/Foundation.h>
#endif

#ifndef CF_BRIDGED_TYPE
#define CF_BRIDGED_TYPE(T)
#endif

#ifndef CF_RELEASES_ARGUMENT
#define CF_RELEASES_ARGUMENT
#endif

#ifndef CF_RETURNS_RETAINED
#define CF_RETURNS_RETAINED
#endif

#ifndef NS_RELEASES_ARGUMENT
#define NS_RELEASES_ARGUMENT
#endif

#ifndef NS_RETURNS_RETAINED
#define NS_RETURNS_RETAINED
#endif

#ifndef __OBJC__
typedef struct objc_object *id;
#endif

// Because ARC enablement is a compile-time choice, and we compile this header
// both ways, we need a separate copy of our code when ARC is enabled.
#if __has_feature(objc_arc)
#define adoptNS adoptNSArc
#define RetainPtr RetainPtrArc
#endif

namespace WTF {

// RetainPtr can point to NS or CF objects, e.g. RetainPtr<NSDictionary> or RetainPtr<CFDictionaryRef>.

template<typename T> class RetainPtr;

template<typename T> constexpr bool IsNSType = std::is_convertible_v<T, id>;
template<typename T> using RetainPtrType = std::conditional_t<IsNSType<T>, std::remove_pointer_t<T>, T>;

template<typename T> constexpr RetainPtr<T> adoptCF(T CF_RELEASES_ARGUMENT) WARN_UNUSED_RETURN;

#ifdef __OBJC__
template<typename T> RetainPtr<RetainPtrType<T>> adoptNS(T NS_RELEASES_ARGUMENT) WARN_UNUSED_RETURN;
#endif

template<typename T> class RetainPtr {
public:
    using ValueType = std::remove_pointer_t<T>;
    using PtrType = ValueType*;

    RetainPtr() = default;
    RetainPtr(PtrType);

    RetainPtr(const RetainPtr&);
    template<typename U> RetainPtr(const RetainPtr<U>&);

    constexpr RetainPtr(RetainPtr&& o) : m_ptr(toStorageType(o.leakRef())) { }
    template<typename U> constexpr RetainPtr(RetainPtr<U>&& o) : m_ptr(toStorageType(checkType(o.leakRef()))) { }

    // Hash table deleted values, which are only constructed and never copied or destroyed.
    constexpr RetainPtr(HashTableDeletedValueType) : m_ptr(toStorageType(hashTableDeletedValue())) { }
    constexpr bool isHashTableDeletedValue() const { return m_ptr == toStorageType(hashTableDeletedValue()); }

    ~RetainPtr();

    void clear();

    template<typename U = PtrType>
    std::enable_if_t<IsNSType<U> && std::is_same_v<U, PtrType>, PtrType> leakRef() NS_RETURNS_RETAINED WARN_UNUSED_RETURN {
        return fromStorageType(std::exchange(m_ptr, nullptr));
    }

    template<typename U = PtrType>
    std::enable_if_t<!IsNSType<U> && std::is_same_v<U, PtrType>, PtrType> leakRef() CF_RETURNS_RETAINED WARN_UNUSED_RETURN {
        return fromStorageType(std::exchange(m_ptr, nullptr));
    }

#if HAVE(CFAUTORELEASE)
    PtrType autorelease();
#endif

#ifdef __OBJC__
    id bridgingAutorelease();
#endif

    constexpr PtrType get() const { return fromStorageType(m_ptr); }
    constexpr PtrType operator->() const { return fromStorageType(m_ptr); }
    constexpr explicit operator PtrType() const { return fromStorageType(m_ptr); }
    constexpr explicit operator bool() const { return m_ptr; }

    constexpr bool operator!() const { return !m_ptr; }

    RetainPtr& operator=(const RetainPtr&);
    template<typename U> RetainPtr& operator=(const RetainPtr<U>&);
    RetainPtr& operator=(PtrType);
    template<typename U> RetainPtr& operator=(U*);

    RetainPtr& operator=(RetainPtr&&);
    template<typename U> RetainPtr& operator=(RetainPtr<U>&&);

    void swap(RetainPtr&);

    template<typename U> friend constexpr RetainPtr<U> adoptCF(U CF_RELEASES_ARGUMENT) WARN_UNUSED_RETURN;

#ifdef __OBJC__
    template<typename U> friend RetainPtr<RetainPtrType<U>> adoptNS(U NS_RELEASES_ARGUMENT) WARN_UNUSED_RETURN;
#endif

private:
    enum AdoptTag { Adopt };
    constexpr RetainPtr(PtrType ptr, AdoptTag) : m_ptr(toStorageType(ptr)) { }

    static constexpr PtrType checkType(PtrType ptr) { return ptr; }

    // ARC will try to retain/release this value, but it looks like a tagged immediate, so retain/release ends up being a no-op -- see _objc_registerTaggedPointerClass.
    static constexpr PtrType hashTableDeletedValue() { return fromStorageType(reinterpret_cast<CFTypeRef>(-1)); }

#ifdef __OBJC__
    template<typename U = PtrType>
    static constexpr std::enable_if_t<IsNSType<U> && std::is_same_v<U, PtrType>, PtrType> fromStorageTypeHelper(CFTypeRef ptr)
    {
        return (__bridge PtrType)const_cast<CF_BRIDGED_TYPE(id) void*>(ptr);
    }
    template<typename U = PtrType>
    static constexpr std::enable_if_t<!IsNSType<U> && std::is_same_v<U, PtrType>, PtrType> fromStorageTypeHelper(CFTypeRef ptr)
    {
        return (PtrType)const_cast<CF_BRIDGED_TYPE(id) void*>(ptr);
    }
    static constexpr PtrType fromStorageType(CFTypeRef ptr) { return fromStorageTypeHelper<PtrType>(ptr); }
    static constexpr CFTypeRef toStorageType(id ptr) { return (__bridge CFTypeRef)ptr; }
    static constexpr CFTypeRef toStorageType(CFTypeRef ptr) { return ptr; }
#else
    static constexpr PtrType fromStorageType(CFTypeRef ptr)
    {
        return (PtrType)const_cast<CF_BRIDGED_TYPE(id) void*>(ptr);
    }
    static constexpr CFTypeRef toStorageType(PtrType ptr) { return (CFTypeRef)ptr; }
#endif

    CFTypeRef m_ptr { nullptr };
};

template<typename T> RetainPtr(T) -> RetainPtr<RetainPtrType<T>>;

// Helper function for creating a RetainPtr using template argument deduction.
template<typename T> RetainPtr<RetainPtrType<T>> retainPtr(T) WARN_UNUSED_RETURN;

template<typename T> inline RetainPtr<T>::~RetainPtr()
{
    if (auto ptr = std::exchange(m_ptr, nullptr))
        CFRelease(ptr);
}

template<typename T> inline RetainPtr<T>::RetainPtr(PtrType ptr)
    : m_ptr(toStorageType(ptr))
{
    if (m_ptr)
        CFRetain(m_ptr);
}

template<typename T> inline RetainPtr<T>::RetainPtr(const RetainPtr& o)
    : RetainPtr(o.get())
{
}

template<typename T> template<typename U> inline RetainPtr<T>::RetainPtr(const RetainPtr<U>& o)
    : RetainPtr(o.get())
{
}

template<typename T> inline void RetainPtr<T>::clear()
{
    if (auto ptr = std::exchange(m_ptr, nullptr))
        CFRelease(ptr);
}

#if HAVE(CFAUTORELEASE)
template<typename T> inline auto RetainPtr<T>::autorelease() -> PtrType
{
#ifdef __OBJC__
    if constexpr (IsNSType<PtrType>)
        return CFBridgingRelease(std::exchange(m_ptr, nullptr));
#endif
    if (m_ptr)
        CFAutorelease(m_ptr);
    return leakRef();
}
#endif // PLATFORM(COCOA)

#ifdef __OBJC__

// FIXME: It would be better if we could base the return type on the type that is toll-free bridged with T rather than using id.
template<typename T> inline id RetainPtr<T>::bridgingAutorelease()
{
    static_assert(!IsNSType<PtrType>, "Don't use bridgingAutorelease for Objective-C pointer types.");
    return CFBridgingRelease(leakRef());
}

#endif

template<typename T> inline RetainPtr<T>& RetainPtr<T>::operator=(const RetainPtr& o)
{
    RetainPtr ptr = o;
    swap(ptr);
    return *this;
}

template<typename T> template<typename U> inline RetainPtr<T>& RetainPtr<T>::operator=(const RetainPtr<U>& o)
{
    RetainPtr ptr = o;
    swap(ptr);
    return *this;
}

template<typename T> inline RetainPtr<T>& RetainPtr<T>::operator=(PtrType optr)
{
    RetainPtr ptr = optr;
    swap(ptr);
    return *this;
}

template<typename T> template<typename U> inline RetainPtr<T>& RetainPtr<T>::operator=(U* optr)
{
    RetainPtr ptr = optr;
    swap(ptr);
    return *this;
}

template<typename T> inline RetainPtr<T>& RetainPtr<T>::operator=(RetainPtr&& o)
{
    RetainPtr ptr = WTFMove(o);
    swap(ptr);
    return *this;
}

template<typename T> template<typename U> inline RetainPtr<T>& RetainPtr<T>::operator=(RetainPtr<U>&& o)
{
    RetainPtr ptr = WTFMove(o);
    swap(ptr);
    return *this;
}

template<typename T> inline void RetainPtr<T>::swap(RetainPtr& o)
{
    std::swap(m_ptr, o.m_ptr);
}

template<typename T> inline void swap(RetainPtr<T>& a, RetainPtr<T>& b)
{
    a.swap(b);
}

template<typename T, typename U> constexpr bool operator==(const RetainPtr<T>& a, const RetainPtr<U>& b)
{
    return a.get() == b.get();
}

template<typename T, typename U> constexpr bool operator==(const RetainPtr<T>& a, U* b)
{
    return a.get() == b;
}

template<typename T, typename U> constexpr bool operator==(T* a, const RetainPtr<U>& b)
{
    return a == b.get();
}

template<typename T> constexpr RetainPtr<T> adoptCF(T CF_RELEASES_ARGUMENT ptr)
{
#ifdef __OBJC__
    static_assert(!IsNSType<T>, "Don't use adoptCF with Objective-C pointer types, use adoptNS.");
#endif
    return RetainPtr<T>(ptr, RetainPtr<T>::Adopt);
}

#ifdef __OBJC__
template<typename T> RetainPtr<RetainPtrType<T>> adoptNS(T NS_RELEASES_ARGUMENT ptr)
{
    static_assert(IsNSType<T>, "Don't use adoptNS with Core Foundation pointer types, use adoptCF.");
#if __has_feature(objc_arc)
    return ptr;
#else
    return { ptr, RetainPtr<RetainPtrType<T>>::Adopt };
#endif
}
#endif

template<typename T> inline RetainPtr<RetainPtrType<T>> retainPtr(T ptr)
{
    return ptr;
}

template<typename T> struct IsSmartPtr<RetainPtr<T>> {
    static constexpr bool value = true;
    static constexpr bool isNullable = true;
};

template<typename P> struct HashTraits<RetainPtr<P>> : SimpleClassHashTraits<RetainPtr<P>> {
};

template<typename P> struct DefaultHash<RetainPtr<P>> : PtrHash<RetainPtr<P>> { };

template<typename P> struct RetainPtrObjectHashTraits : SimpleClassHashTraits<RetainPtr<P>> {
    static const RetainPtr<P>& emptyValue()
    {
        static NeverDestroyed<RetainPtr<P>> null;
        return null;
    }

    static bool isEmptyValue(const RetainPtr<P>& value) { return !value; }
};

template<typename P> struct RetainPtrObjectHash {
    static unsigned hash(const RetainPtr<P>& o)
    {
        ASSERT_WITH_MESSAGE(o.get(), "attempt to use null RetainPtr in HashTable");
        return static_cast<unsigned>(CFHash(o.get()));
    }
    static bool equal(const RetainPtr<P>& a, const RetainPtr<P>& b)
    {
        return CFEqual(a.get(), b.get());
    }
    static constexpr bool safeToCompareToEmptyOrDeleted = false;
};

inline bool safeCFEqual(CFTypeRef a, CFTypeRef b)
{
    return (!a && !b) || (a && b && CFEqual(a, b));
}

inline CFHashCode safeCFHash(CFTypeRef a)
{
    return a ? CFHash(a) : 0;
}

} // namespace WTF

using WTF::RetainPtr;
using WTF::adoptCF;
using WTF::retainPtr;
using WTF::safeCFEqual;
using WTF::safeCFHash;

#ifdef __OBJC__
using WTF::adoptNS;
#endif

#endif // USE(CF) || defined(__OBJC__)
