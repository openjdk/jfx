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
 * WKJHandle - an owning reference to a Java object, replacing the JLocalRef and JGlobalRef
 * templates of wtf/java/JavaRef.h.
 *
 * A Java object reaches native code as a wkj_ref: an id assigned by the Java-side registry,
 * with 0 meaning null (FFM-ABI-CONTRACT.md section 3). Native code never holds a Java
 * reference, so the local/global distinction disappears: there is one kind of reference and
 * one class holding it. WKJHandle reproduces the ownership semantics JavaRef.h had, on top
 * of host->core.retain and host->core.release.
 *
 * WHICH JNI CONSTRUCTOR MAPS ONTO WHICH - read this before rewriting any call site.
 *
 * JavaRef.h has two constructors with the same shape and opposite ownership, and getting
 * them the wrong way round produces a double release that shows up much later as a
 * use-after-free. The rule that separates them is simple once seen:
 *
 *   **A constructor taking a RAW reference consumes it. A constructor taking another
 *   wrapper copies.**
 *
 *   JavaRef.h                                   WKJHandle
 *   -----------------------------------------   ----------------------------------------
 *   JLObject o;                                 WKJHandle h;
 *   JLObject o(jref);            adopts raw     WKJHandle h { id };          adopts
 *   JLObject o(jref, true);      copies         WKJHandle::retained(id)      retains
 *   JGObject g(jref);            CONSUMES raw   WKJHandle h { id };          adopts
 *        (JavaRef.h:157 wraps jref in a temporary JLocalRef that adopts it, takes a
 *         global ref, and deletes the local one as the temporary dies - so the caller's
 *         reference is gone afterwards. One id in, one id owned: the same as adopting.)
 *   JGObject g(localRef);        copies         WKJHandle::retained(o.get()) retains
 *   JGObject g2(g);              copies         WKJHandle h2 { h };          retains
 *   JLObject o2(o);              copies         WKJHandle h2 { h };          retains
 *   (no analogue)                               WKJHandle h2 { WTF::move(h) }; steals
 *   o2 = o;                      release+copy   h2 = h;                      release+retain
 *   ~JLObject() / ~JGObject()    releases       ~WKJHandle()                 releases
 *   the cast on o                raw, still owned by o    h.get()
 *   o.releaseLocal() / o.releaseGlobal()        h.leakRef()   ownership handed to caller
 *
 * There is no analogue of JGlobalRef's *implicit* local-to-global promotion, because there
 * is only one kind of id. Wherever the JNI code promoted a local to a global to make it
 * outlive the native method, the id already outlives it - which is also the hazard: JNI
 * reclaimed leaked local references when the native method returned, and a registry
 * reclaims nothing. Every id needs a named owner.
 *
 * Differences from JavaRef.h, all deliberate:
 *
 *   - Construction from a raw wkj_ref is explicit. JLocalRef's was implicit because it took
 *     a pointer type; wkj_ref is an integer, and an implicit constructor would swallow any
 *     integer expression by accident.
 *   - There is no implicit conversion back to the raw id either (JavaRef.h had
 *     `operator const T&`). Call get() where the id is passed on, so that every place the
 *     library hands an id to Java is visible.
 *   - Copy assignment keeps the guard JLocalRef::operator= had (JavaRef.h:115): assigning a
 *     handle that already holds the same id is a no-op. That guard is load-bearing, not
 *     cosmetic. Without it, self-assignment through two wrappers would release the id and
 *     then retain the id it had just released.
 *
 * Ids are handles, not objects. The one rule the ABI imposes is that every id obtained from
 * retain or retain_weak is released exactly once; whether the registry mints a fresh id per
 * retain (the JNI model) or interns by object identity with a reference count is its own
 * choice, and this class is correct under both. See WKJHostCore in webkit_java_api.h. One
 * consequence to keep in mind while rewriting: `a == b` compares ids, so it only answers
 * "same object" if the registry interns. Use host->core.equals where the question is really
 * about the objects. No comparison of two handles exists in the tree today - every use of
 * the comparison operators on a JLObject/JGObject-family value is a null test - so nothing
 * currently depends on the answer.
 *
 * Threading: retain and release run on whatever thread holds the handle, exactly as
 * NewGlobalRef and DeleteGlobalRef did. The Java implementations of both must therefore be
 * safe on any thread; the registry is a ConcurrentHashMap.
 */

#pragma once

#include <webkit_java_api.h>

/*
 * Mints a new strong id for the object `ref` names, or 0 for a null ref, an uninstalled
 * host or a NULL slot. This is JLocalRef::copy / JGlobalRef::copy, whose guard was
 * `(env && ref)`.
 */
inline wkj_ref WKJRetain(wkj_ref ref)
{
    if (!ref || !wkj_host || !wkj_host->core.retain)
        return 0;
    return wkj_host->core.retain(ref);
}

/*
 * Mints a new weak id, which does not keep the object reachable. Only LiveConnect needs
 * this (JobjectWrapper takes NewWeakGlobalRef by default); everything else wants WKJRetain.
 */
inline wkj_ref WKJRetainWeak(wkj_ref ref)
{
    if (!ref || !wkj_host || !wkj_host->core.retain_weak)
        return 0;
    return wkj_host->core.retain_weak(ref);
}

/*
 * Drops the id `ref`, strong or weak. This is JLocalRef::clear / JGlobalRef::clear, whose
 * guard was `(env && m_jref)`.
 */
inline void WKJRelease(wkj_ref ref)
{
    if (ref && wkj_host && wkj_host->core.release)
        wkj_host->core.release(ref);
}

/* True while `ref` still names a live object; false once a weak id's object has gone. */
inline bool WKJIsLive(wkj_ref ref)
{
    if (!ref || !wkj_host || !wkj_host->core.is_live)
        return false;
    return wkj_host->core.is_live(ref) != 0;
}

class WKJHandle final {
public:
    WKJHandle() = default;

    /* Adopts one reference to `ref`; does not add one. */
    explicit WKJHandle(wkj_ref ref)
        : m_ref(ref)
    {
    }

    /* Mints a new strong id for the same object and adopts it (JLocalRef's bycopy = true). */
    static WKJHandle retained(wkj_ref ref)
    {
        return WKJHandle(WKJRetain(ref));
    }

    /* Mints a new weak id for the same object and adopts it. */
    static WKJHandle retainedWeak(wkj_ref ref)
    {
        return WKJHandle(WKJRetainWeak(ref));
    }
    WKJHandle(const WKJHandle& other)
        : m_ref(WKJRetain(other.m_ref))
    {
    }

    WKJHandle(WKJHandle&& other) noexcept
        : m_ref(other.m_ref)
    {
        other.m_ref = 0;
    }

    ~WKJHandle()
    {
        WKJRelease(m_ref);
        m_ref = 0;
    }

    WKJHandle& operator=(const WKJHandle& other)
    {
        if (m_ref != other.m_ref) {
            WKJRelease(m_ref);
            m_ref = WKJRetain(other.m_ref);
        }
        return *this;
    }

    WKJHandle& operator=(WKJHandle&& other) noexcept
    {
        if (this != &other) {
            WKJRelease(m_ref);
            m_ref = other.m_ref;
            other.m_ref = 0;
        }
        return *this;
    }

    /* Releases the held reference and becomes null. */
    void clear()
    {
        WKJRelease(m_ref);
        m_ref = 0;
    }

    /* The held id. Ownership stays with this handle; do not release it. */
    wkj_ref get() const { return m_ref; }

    /*
     * The held id, with ownership handed to the caller: the handle becomes null and will
     * not release it. This is releaseLocal() / releaseGlobal().
     */
    [[nodiscard]] wkj_ref leakRef()
    {
        wkj_ref ref = m_ref;
        m_ref = 0;
        return ref;
    }

    bool isNull() const { return !m_ref; }
    bool operator!() const { return !m_ref; }
    explicit operator bool() const { return m_ref != 0; }

private:
    wkj_ref m_ref { 0 };
};

inline bool operator==(const WKJHandle& a, const WKJHandle& b) { return a.get() == b.get(); }
inline bool operator!=(const WKJHandle& a, const WKJHandle& b) { return a.get() != b.get(); }

/* Comparison against a raw id, so that `handle == 0` reads as "is null". */
inline bool operator==(const WKJHandle& a, wkj_ref b) { return a.get() == b; }
inline bool operator!=(const WKJHandle& a, wkj_ref b) { return a.get() != b; }
inline bool operator==(wkj_ref a, const WKJHandle& b) { return a == b.get(); }
inline bool operator!=(wkj_ref a, const WKJHandle& b) { return a != b.get(); }

/*
 * Aliases for the JavaRef.h typedefs that still name something real.
 *
 * JLObject and JGObject both meant "a reference to a Java object, owned by this scope",
 * which is exactly WKJHandle; the local/global distinction was about how the JVM tracked
 * the reference and has no counterpart in a registry of ids. Both therefore map onto the
 * one alias below, and code that used to choose between them no longer chooses.
 *
 * The remaining typedefs have no analogue and get none, because the thing they referred to
 * no longer crosses the boundary:
 *
 *   JLClass, JGClass  - a class reference cached from FindClass so that method ids could be
 *                       looked up on it. The host table replaces class lookup and method ids
 *                       outright; a handle to a java.lang.Class has nothing left to do.
 *   JLString, JGString- a jstring. Strings cross as (const uint16_t*, int32_t) in and as a
 *                       caller-provided buffer out (contract 13); neither is an object.
 *   JLObjectArray,    - Java arrays. Arrays cross as (const T*, int32_t) in and as a
 *   JGObjectArray,      caller-provided out-buffer plus a count out (contract 2), so there
 *   JLByteArray         is no array object for native code to hold.
 */
using WKJObject = WKJHandle;
