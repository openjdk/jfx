/*
 *  Copyright (C) 2005-2024 Apple Inc. All rights reserved.
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

#include <new>
#include <stdlib.h>
#include <wtf/DebugHeap.h>
#include <wtf/MallocCommon.h>
#include <wtf/StdLibExtras.h>

#if !USE(SYSTEM_MALLOC)
#include <bmalloc/BPlatform.h>
// Enable USE(LIBPAS)
// FIXME: Replaces uses of `#if !USE(SYSTEM_MALLOC) \n #if BUSE(LIBPAS)` with `#if USE(LIBPAS)`
#if BUSE(LIBPAS)
#define USE_LIBPAS 1
#endif
#endif

namespace WTF {

// There are several malloc-related macros to annotate class / struct. If these annotations are attached,
// allocation is handled by libpas or bmalloc, depending on which if any of these memory allocators is available.
//
// TLDR; Here is a quick guidance.
//   1. If your class / struct /template is derived from a base class which uses:
//          WTF_MAKE_TZONE_ALLOCATED
//          WTF_MAKE_TZONE_ALLOCATED_EXPORT
//          WTF_MAKE_STRUCT_TZONE_ALLOCATED
//          WTF_MAKE_TZONE_ALLOCATED_TEMPLATE
//          WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_EXPORT
//          WTF_MAKE_COMPACT_TZONE_ALLOCATED
//          WTF_MAKE_COMPACT_TZONE_ALLOCATED_EXPORT
//      then you must use the same TZONE_ALLOCATED annotation as well. If your class is a template, use one of
//      the template forms (WTF_MAKE_TZONE_ALLOCATED_TEMPLATE or WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_EXPORT).
//      If your class is not a template, use one of the non-template forms.
//
//   2. If your class / struct is derived from a base class which uses:
//          WTF_MAKE_TZONE_OR_ISO_ALLOCATED
//          WTF_MAKE_TZONE_OR_ISO_ALLOCATED_EXPORT
//          WTF_MAKE_TZONE_OR_ISO_ALLOCATED_TEMPLATE
//          WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED
//          WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED_EXPORT
//      then you must use the same TZONE_OR_ISO_ALLOCATED annotation as well. If your class is a template, use the
//      template forms (WTF_MAKE_TZONE_OR_ISO_ALLOCATED_TEMPLATE). If your class is not a template, use one of the
//      non-template forms.
//
//   3. If your class / struct / template is a DOM object, use a TZONE_OR_ISO_ALLOCATED annotation.
//
//   4. If your class / struct is particularly memory consuming and if you think tracking footprint of your class
//      is helpful for memory-reduction work, use WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER /
//      WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER.
//
//   5. For classes / structs that are fixed sized, use a TZONE_ALLOCATED annotation
//
//   6. Otherwise, use WTF_MAKE_FAST_ALLOCATED / WTF_MAKE_STRUCT_FAST_ALLOCATED.
//
//   7. If your template uses a WTF_MAKE_TZONE_ALLOCATED_TEMPLATE or WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_EXPORT
//      annotation, then you must use WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_IMPL or
//      WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_IMPL_WITH_MULTIPLE_OR_SPECIALIZED_PARAMETERS after the template definition
//      (in the header file as well if that's there the template is defined).
//
//   8. If your template uses a WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED or WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED_EXPORT
//      annotation, then you must use WTF_MAKE_TZONE_OR_ISO_ALLOCATED_TEMPLATE_IMPL after the template definition
//      (in the header file as well if that's there the template is defined).
//
// Let's explain the differences in detail.
//
// - WTF_MAKE_FAST_ALLOCATED
// - WTF_MAKE_STRUCT_FAST_ALLOCATED
//     class / struct is allocated with FastMalloc (bmalloc if it is available). We encourage using FastMalloc for all the class / struct allocations if possible
//     to avoid using system malloc. If a class is not having WTF_MAKE_FAST_ALLOCATED, the class will be allocated with system malloc, which is less efficient
//     compared to FastMalloc in terms of performance and memory footprint. We would like to minimize the use of system malloc.
//     These annotations should be the default choice for allocations.
//     WTF_MAKE_FAST_ALLOCATED is for classes and WTF_MAKE_STRUCT_FAST_ALLOCATED is for structs. The difference between them is how we
//     use `public:` / `private:` access specifiers in the expanded macro.
//
// - WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(ClassName)
// - WTF_MAKE_STRUCT_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(ClassName)
//     Normally, these are identical to WTF_MAKE_FAST_ALLOCATED.
//     When the MallocHeapBreakdown debugging feature is enabled, these macros act differently. bmalloc is switched
//     to using system malloc internally, and bmalloc creates a named MallocZone per this annotation. MallocZone allows us to track heap usage
//     per this annotation, offering quick way of breaking down memory usage.
//     To enable MallocHeapBreakdown, set ENABLE_MALLOC_HEAP_BREAKDOWN in WTF and BENABLE_MALLOC_HEAP_BREAKDOWN in bmalloc.
//     For the details of MallocHeapBreakdown, please look at ChangeLog of https://trac.webkit.org/changeset/253987/webkit.
//
// - WTF_MAKE_TZONE_ALLOCATED(ClassName)
// - WTF_MAKE_TZONE_ALLOCATED_EXPORT(ClassName, exportMacro)
// - WTF_MAKE_STRUCT_TZONE_ALLOCATED(ClassName)
// - WTF_MAKE_TZONE_ALLOCATED_TEMPLATE(ClassName)
// - WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_EXPORT(ClassName, exportMacro)
// - WTF_MAKE_COMPACT_TZONE_ALLOCATED(ClassName)
// - WTF_MAKE_COMPACT_TZONE_ALLOCATED_EXPORT(ClassName, exportMacro)
//     class / struct / template is allocated from a fixed set of shared libpas heaps with the same size and alignment. Each set of shared heaps is
//     known as a TZoneTypeBuckets. The particular bucket is selected using SHA 256 hashing on a process startup value along with particulars of the
//     class / struct being allocated. This sharing provides protections from type confusion and use-after-free bugs, but with less memory overhead of
//     IsoHeap allocated objects. This is the preferred allocation method.
//     For example, if Event is annotated with WTF_MAKE_TZONE_ALLOCATED(Event), all derived classes of Event must be annotated with
//     WTF_MAKE_TZONE_ALLOCATED(XXX). When you annotate a class with WTF_MAKE_TZONE_ALLOCATED(XXX), you need to add WTF_MAKE_TZONE_ALLOCATED_IMPL(XXX)
//     in cpp file side.
//     Because WTF_MAKE_TZONE_ALLOCATED_IMPL defines functions on the cpp side, you sometimes need to annotate these functions with export macros when your
//     class is used outside of the component defining your class (e.g. your class is in WebCore and it is also used in WebKit). In this case,
//     you can use WTF_MAKE_TZONE_ALLOCATED_EXPORT(XXX). to annotate these functions with appropriate export macros:
//        e.g. WTF_MAKE_TZONE_ALLOCATED_EXPORT(Special, JS_EXPORT_PRIVATE)).
//
//     WTF_MAKE_STRUCT_TZONE_ALLOCATED is the same as WTF_MAKE_TZONE_ALLOCATED, but for structs.
//
//     WTF_MAKE_TZONE_ALLOCATED_TEMPLATE and WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_EXPORT are the template analogs of WTF_MAKE_TZONE_ALLOCATED and
//     WTF_MAKE_TZONE_ALLOCATED_EXPORT. Templates definitions annotated with these macros must be followed by WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_IMPL
//     or WTF_MAKE_TZONE_ALLOCATED_TEMPLATE_IMPL_WITH_MULTIPLE_OR_SPECIALIZED_PARAMETERS to provide the instantiation of statics used by the allocator.
//
//     WTF_MAKE_COMPACT_TZONE_ALLOCATED and WTF_MAKE_COMPACT_TZONE_ALLOCATED_EXPORT are analogs of WTF_MAKE_TZONE_ALLOCATED and
//     WTF_MAKE_TZONE_ALLOCATED_EXPORT, but or special classes whose object pointers need to be stored in some custom scheme e.g. in CompactPtrs.
//     An example of this is the StringImpl class.
//
//     The WTF_MAKE_TZONE_OR_ISO_ALLOCATED family of macros is just like WTF_MAKE_TZONE_ALLOCATED, except they will fall back to WTF_MAKE_ISO_ALLOCATED,
//     when TZone allocation is disabled, but IsoHeap allocation is enabled.
//
// - WTF_MAKE_ISO_ALLOCATED(ClassName)
// - WTF_MAKE_ISO_ALLOCATED_EXPORT(ClassName, exportMacro)
//     Note, these annotations are legacy and should not be used for new code.
//     class / struct is allocated from bmalloc IsoHeap. IsoHeap assigns virtual address only for particular type,
//     so that this avoids use-after-free based type punning. We are adopting IsoHeap mainly for class / struct which is exposed to user JavaScript (e.g. DOM objects).
//     For example , all the derived classes of ScriptWrappable must be allocated in IsoHeap.
//     Unlike the other macros, you need to annotate each derived class with WTF_MAKE_ISO_ALLOCATED if your base class is annotated with WTF_MAKE_ISO_ALLOCATED.
//     When you annotate the class with WTF_MAKE_ISO_ALLOCATED(XXX), you need to add WTF_MAKE_ISO_ALLOCATED_IMPL(XXX) in cpp file side.
//     Because WTF_MAKE_ISO_ALLOCATED_IMPL defines functions in cpp side, you sometimes need to annotate these functions with export macros when your class is
//     used outside of the component defining your class (e.g. your class is in WebCore and it is also used in WebKit). In this case, you can use WTF_MAKE_ISO_ALLOCATED_EXPORT
//     to annotate these functions with appropriate export macros: e.g. WTF_MAKE_ISO_ALLOCATED_EXPORT(IDBTransaction, WEBCORE_EXPORT).

#if !defined(NDEBUG)
WTF_EXPORT_PRIVATE void fastSetMaxSingleAllocationSize(size_t);
#endif

WTF_EXPORT_PRIVATE bool isFastMallocEnabled();

// These functions call CRASH() if an allocation fails.
WTF_EXPORT_PRIVATE void* fastMalloc(size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastZeroedMalloc(size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastCalloc(size_t numElements, size_t elementSize) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastRealloc(void*, size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE char* fastStrDup(const char*) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastMemDup(const void*, size_t);

WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastMalloc(size_t);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastZeroedMalloc(size_t);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastCalloc(size_t numElements, size_t elementSize);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastRealloc(void*, size_t);

WTF_EXPORT_PRIVATE void fastFree(void*);

// Allocations from fastAlignedMalloc() must be freed using fastAlignedFree().
WTF_EXPORT_PRIVATE void* fastAlignedMalloc(size_t alignment, size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* tryFastAlignedMalloc(size_t alignment, size_t);
WTF_EXPORT_PRIVATE void fastAlignedFree(void*);

// These functions behave like their non-compact counterparts, but guarantee
// that the pointer returned can be stored as a CompactPtr or PackedPtr.

WTF_EXPORT_PRIVATE void* fastCompactMalloc(size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastCompactZeroedMalloc(size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastCompactCalloc(size_t numElements, size_t elementSize) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastCompactRealloc(void*, size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastCompactMalloc(size_t);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastCompactZeroedMalloc(size_t);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastCompactCalloc(size_t numElements, size_t elementSize);
WTF_EXPORT_PRIVATE TryMallocReturnValue tryFastCompactRealloc(void*, size_t);
WTF_EXPORT_PRIVATE char* fastCompactStrDup(const char*) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* fastCompactMemDup(const void*, size_t);
WTF_EXPORT_PRIVATE void* fastCompactAlignedMalloc(size_t alignment, size_t) RETURNS_NONNULL;
WTF_EXPORT_PRIVATE void* tryFastCompactAlignedMalloc(size_t alignment, size_t);

WTF_EXPORT_PRIVATE size_t fastMallocSize(const void*);

// FIXME: This is non-helpful; fastMallocGoodSize will be removed soon.
WTF_EXPORT_PRIVATE size_t fastMallocGoodSize(size_t);

WTF_EXPORT_PRIVATE void releaseFastMallocFreeMemory();
WTF_EXPORT_PRIVATE void releaseFastMallocFreeMemoryForThisThread();

WTF_EXPORT_PRIVATE void fastCommitAlignedMemory(void*, size_t);
WTF_EXPORT_PRIVATE void fastDecommitAlignedMemory(void*, size_t);

WTF_EXPORT_PRIVATE void fastEnableMiniMode(bool forceMiniMode = false);

WTF_EXPORT_PRIVATE void fastDisableScavenger();

// allocate with guard pages at a rate of 1/guardMallocRate
WTF_EXPORT_PRIVATE void forceEnablePGM(uint16_t guardMallocRate);

struct FastMallocStatistics {
    size_t reservedVMBytes;
    size_t committedVMBytes;
    size_t freeListBytes;
};
WTF_EXPORT_PRIVATE FastMallocStatistics fastMallocStatistics();

WTF_EXPORT_PRIVATE void fastMallocDumpMallocStats();

// This defines a type which holds an unsigned integer and is the same
// size as the minimally aligned memory allocation.
typedef unsigned long long AllocAlignmentInteger;

// C++ STL allocator implementation. You can integrate fastMalloc into STL containers.
// e.g. std::unordered_map<Key, Value, std::hash<Key>, std::equal_to<Key>, FastAllocator<std::pair<const Key, Value>>>.
template<typename T>
class FastAllocator {
public:
    using value_type = T;

    FastAllocator() = default;

    template<typename U> FastAllocator(const FastAllocator<U>&) { }

    T* allocate(size_t count)
    {
        return static_cast<T*>(fastMalloc(sizeof(T) * count));
    }

    void deallocate(T* pointer, size_t)
    {
        fastFree(pointer);
    }

    template <typename U>
    struct rebind {
        using other = FastAllocator<U>;
    };
};

template<typename T, typename U> inline bool operator==(const FastAllocator<T>&, const FastAllocator<U>&) { return true; }

struct FastCompactMalloc;

struct FastMalloc {
    static void* malloc(size_t size) { return fastMalloc(size); }

    static void* tryMalloc(size_t size)
    {
        auto result = tryFastMalloc(size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void* zeroedMalloc(size_t size) { return fastZeroedMalloc(size); }

    static void* tryZeroedMalloc(size_t size)
    {
        auto result = tryFastZeroedMalloc(size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void* realloc(void* p, size_t size) { return fastRealloc(p, size); }

    static void* tryRealloc(void* p, size_t size)
    {
        auto result = tryFastRealloc(p, size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void free(void* p) { fastFree(p); }

    static void fastFree(void* p) { ::WTF::fastFree(p); }

    static constexpr ALWAYS_INLINE size_t nextCapacity(size_t capacity)
    {
        return capacity + capacity / 4 + 1;
    }

    using CompactMalloc = FastCompactMalloc;
};

struct FastAlignedMalloc {
    static void* alignedMalloc(size_t alignment, size_t size) { return fastAlignedMalloc(alignment, size); }
    static void* tryAlignedMalloc(size_t alignment, size_t size) { return tryFastAlignedMalloc(alignment, size); }
    static void free(void* p) { fastAlignedFree(p); }
};

struct FastCompactMalloc {
    static void* malloc(size_t size) { return fastCompactMalloc(size); }

    static void* tryMalloc(size_t size)
    {
        auto result = tryFastCompactMalloc(size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void* zeroedMalloc(size_t size) { return fastCompactZeroedMalloc(size); }

    static void* tryZeroedMalloc(size_t size)
    {
        auto result = tryFastCompactZeroedMalloc(size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void* realloc(void* p, size_t size) { return fastCompactRealloc(p, size); }

    static void* tryRealloc(void* p, size_t size)
    {
        auto result = tryFastCompactRealloc(p, size);
        void* realResult;
        if (result.getValue(realResult))
            return realResult;
        return nullptr;
    }

    static void free(void* p) { fastFree(p); }

    static void fastFree(void* p) { ::WTF::fastFree(p); }

    static constexpr ALWAYS_INLINE size_t nextCapacity(size_t capacity)
    {
        return capacity + capacity / 4 + 1;
    }
};

template<typename T>
struct FastFree {
    static_assert(std::is_trivially_destructible<T>::value);

    void operator()(T* pointer) const
    {
        fastFree(const_cast<typename std::remove_cv<T>::type*>(pointer));
    }
};

template<typename T>
struct FastFree<T[]> {
    static_assert(std::is_trivially_destructible<T>::value);

    void operator()(T* pointer) const
    {
        fastFree(const_cast<typename std::remove_cv<T>::type*>(pointer));
    }
};

} // namespace WTF

#if !defined(NDEBUG)
using WTF::fastSetMaxSingleAllocationSize;
#endif

template<typename T>
struct AllowCompactPointers : std::false_type { };

#define WTF_ALLOW_COMPACT_POINTERS_TO_INCOMPLETE_TYPE(className) \
template<> \
struct AllowCompactPointers<className*> : std::true_type { }

#define WTF_ALLOW_COMPACT_POINTERS_IMPL \
    constexpr static bool allowCompactPointers = true

#define WTF_ALLOW_COMPACT_POINTERS \
public: \
    WTF_ALLOW_COMPACT_POINTERS_IMPL; \
private: \
using __thisIsAlsoHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_ALLOW_STRUCT_COMPACT_POINTERS \
public: \
    WTF_ALLOW_COMPACT_POINTERS_IMPL; \
using __thisIsAlsoHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

template<typename T>
inline constexpr std::enable_if_t<WTF::IsTypeComplete<std::remove_pointer_t<T>>, bool> allowCompactPointers()
{
    return std::remove_pointer_t<T>::allowCompactPointers;
}

template<typename T>
inline constexpr std::enable_if_t<!WTF::IsTypeComplete<std::remove_pointer_t<T>>, bool> allowCompactPointers()
{
    // We want to support compact pointers to incomplete types too, so we have this fallback:
    // if a type is incomplete, AllowCompactPointers can be specialized on its pointer type,
    // in which case we'll return its value. This is mostly accomplished using the below
    // WTF_ALLOW_COMPACT_POINTERS_TO_INCOMPLETE_TYPE macro.
    return AllowCompactPointers<std::remove_const_t<std::remove_pointer_t<T>>*>::value;
}

using WTF::FastAlignedMalloc;
using WTF::FastAllocator;
using WTF::FastMalloc;
using WTF::FastCompactMalloc;
using WTF::FastFree;
using WTF::isFastMallocEnabled;
using WTF::fastCalloc;
using WTF::fastFree;
using WTF::fastMalloc;
using WTF::fastMallocGoodSize;
using WTF::fastMallocSize;
using WTF::fastMemDup;
using WTF::fastRealloc;
using WTF::fastStrDup;
using WTF::fastZeroedMalloc;
using WTF::tryFastAlignedMalloc;
using WTF::tryFastCalloc;
using WTF::tryFastMalloc;
using WTF::tryFastZeroedMalloc;
using WTF::fastAlignedMalloc;
using WTF::fastAlignedFree;
using WTF::fastCompactCalloc;
using WTF::fastCompactMalloc;
using WTF::fastCompactMemDup;
using WTF::fastCompactRealloc;
using WTF::fastCompactStrDup;
using WTF::fastCompactZeroedMalloc;
using WTF::tryFastCompactAlignedMalloc;
using WTF::tryFastCompactCalloc;
using WTF::tryFastCompactMalloc;
using WTF::tryFastCompactZeroedMalloc;
using WTF::fastCompactAlignedMalloc;

#define WTF_MAKE_FAST_ALLOCATED_IMPL \
    void* operator new(size_t, void* p) { return p; } \
    void* operator new[](size_t, void* p) { return p; } \
    \
    void* operator new(size_t size) \
    { \
        return ::WTF::fastMalloc(size); \
    } \
    \
    void operator delete(void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    \
    void* operator new[](size_t size) \
    { \
        return ::WTF::fastMalloc(size); \
    } \
    \
    void operator delete[](void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    void* operator new(size_t, NotNullTag, void* location) \
    { \
        ASSERT(location); \
        return location; \
    } \
    static void freeAfterDestruction(void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    using WTFIsFastMallocAllocated = int; \

#define WTF_MAKE_FAST_COMPACT_ALLOCATED_IMPL \
    WTF_ALLOW_COMPACT_POINTERS_IMPL; \
    void* operator new(size_t, void* p) { return p; } \
    void* operator new[](size_t, void* p) { return p; } \
    \
    void* operator new(size_t size) \
    { \
        return ::WTF::fastCompactMalloc(size); \
    } \
    \
    void operator delete(void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    \
    void* operator new[](size_t size) \
    { \
        return ::WTF::fastCompactMalloc(size); \
    } \
    \
    void operator delete[](void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    void* operator new(size_t, NotNullTag, void* location) \
    { \
        ASSERT(location); \
        return location; \
    } \
    static void freeAfterDestruction(void* p) \
    { \
        ::WTF::fastFree(p); \
    } \
    using WTFIsFastMallocAllocated = int; \

// FIXME: WTF_MAKE_FAST_ALLOCATED should take class name so that we can create malloc_zone per this macro.
// https://bugs.webkit.org/show_bug.cgi?id=205702
#define WTF_MAKE_FAST_ALLOCATED \
public: \
    WTF_MAKE_FAST_ALLOCATED_IMPL \
private: \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_FAST_ALLOCATED \
    WTF_MAKE_FAST_ALLOCATED_IMPL \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_FAST_COMPACT_ALLOCATED \
public: \
    WTF_MAKE_FAST_COMPACT_ALLOCATED_IMPL \
private: \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_FAST_COMPACT_ALLOCATED \
    WTF_MAKE_FAST_COMPACT_ALLOCATED_IMPL \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#if ENABLE(MALLOC_HEAP_BREAKDOWN)

#define WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(classname) \
    void* operator new(size_t, void* p) { return p; } \
    void* operator new[](size_t, void* p) { return p; } \
    \
    void* operator new(size_t size) \
    { \
        return classname##Malloc::malloc(size); \
    } \
    \
    void operator delete(void* p) \
    { \
        classname##Malloc::free(p); \
    } \
    \
    void* operator new[](size_t size) \
    { \
        return classname##Malloc::malloc(size); \
    } \
    \
    void operator delete[](void* p) \
    { \
        classname##Malloc::free(p); \
    } \
    void* operator new(size_t, NotNullTag, void* location) \
    { \
        ASSERT(location); \
        return location; \
    } \
    static void freeAfterDestruction(void* p) \
    { \
        classname##Malloc::free(p); \
    } \
    using WTFIsFastMallocAllocated = int; \

#define WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(classname) \
public: \
    WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(classname) \
private: \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
private: \
public: \
    WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
    WTF_ALLOW_COMPACT_POINTERS; \
    WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(className)

#define WTF_MAKE_STRUCT_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
    WTF_ALLOW_STRUCT_COMPACT_POINTERS; \
    WTF_MAKE_STRUCT_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(className)

#else // ENABLE(MALLOC_HEAP_BREAKDOWN)

#define WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
    WTF_MAKE_FAST_ALLOCATED_IMPL

#define WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
public: \
    WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
private: \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
public: \
    WTF_MAKE_FAST_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
    WTF_MAKE_FAST_COMPACT_ALLOCATED_IMPL

#define WTF_MAKE_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
public: \
    WTF_MAKE_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
private: \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER(className) \
public: \
    WTF_MAKE_FAST_COMPACT_ALLOCATED_WITH_HEAP_IDENTIFIER_IMPL(className) \
using __thisIsHereToForceASemicolonAfterThisMacro UNUSED_TYPE_ALIAS = int

#endif // ENABLE(MALLOC_HEAP_BREAKDOWN)

// delete(T*, std::destroying_delete_t, size_t) is preferred over delete(void*)
// in overload resolution, so we can use it to interpose before calling delete(void*).
// Note: WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR must be declared in every subclass.
#define WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR_IMPL(T) \
void operator delete(T* object, std::destroying_delete_t, size_t size) { \
    ASSERT_UNUSED(size, sizeof(T) == size); \
    object->T::~T(); \
    if (UNLIKELY(object->checkedPtrCountWithoutThreadCheck())) { \
        zeroBytes(object); \
        return; \
    } \
    T::operator delete(object); \
} \
using WTFDidOverrideDeleteForCheckedPtr = int;

// Note: WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR must be declared in the most derived subclass.
#define WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR(ClassName) \
public: \
    WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR_IMPL(ClassName) \
private: \
using __thisIsHereToForceASemicolonAfterWTFOverrideDelete UNUSED_TYPE_ALIAS = int

#define WTF_STRUCT_OVERRIDE_DELETE_FOR_CHECKED_PTR(ClassName) \
    WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR_IMPL(ClassName) \
using __thisIsHereToForceASemicolonAfterWTFOverrideDelete UNUSED_TYPE_ALIAS = int
