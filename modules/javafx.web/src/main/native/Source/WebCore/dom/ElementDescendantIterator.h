/*
 * Copyright (C) 2014 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Element.h"
#include "ElementIteratorAssertions.h"
#include "ElementTraversal.h"
#include <wtf/Vector.h>

namespace WebCore {

class ElementDescendantIterator {
public:
    ElementDescendantIterator();
    explicit ElementDescendantIterator(Element* current);

    ElementDescendantIterator& operator++();
    ElementDescendantIterator& operator--();

    Element& operator*();
    Element* operator->();

    bool operator==(const ElementDescendantIterator& other) const;
    bool operator!=(const ElementDescendantIterator& other) const;

    void dropAssertions();

private:
    Element* m_current;
    Vector<Element*, 16> m_ancestorSiblingStack;

#if ASSERT_ENABLED
    ElementIteratorAssertions m_assertions;
#endif
};

class ElementDescendantConstIterator {
public:
    ElementDescendantConstIterator();
    explicit ElementDescendantConstIterator(const Element*);

    ElementDescendantConstIterator& operator++();

    const Element& operator*() const;
    const Element* operator->() const;

    bool operator==(const ElementDescendantConstIterator& other) const;
    bool operator!=(const ElementDescendantConstIterator& other) const;

    void dropAssertions();

private:
    const Element* m_current;
    Vector<Element*, 16> m_ancestorSiblingStack;

#if ASSERT_ENABLED
    ElementIteratorAssertions m_assertions;
#endif
};

class ElementDescendantIteratorAdapter {
public:
    ElementDescendantIteratorAdapter(ContainerNode& root);
    ElementDescendantIterator begin();
    ElementDescendantIterator end();
    ElementDescendantIterator last();

private:
    ContainerNode& m_root;
};

class ElementDescendantConstIteratorAdapter {
public:
    ElementDescendantConstIteratorAdapter(const ContainerNode& root);
    ElementDescendantConstIterator begin() const;
    ElementDescendantConstIterator end() const;
    ElementDescendantConstIterator last() const;

private:
    const ContainerNode& m_root;
};

ElementDescendantIteratorAdapter elementDescendants(ContainerNode&);
ElementDescendantConstIteratorAdapter elementDescendants(const ContainerNode&);

// ElementDescendantIterator

inline ElementDescendantIterator::ElementDescendantIterator()
    : m_current(nullptr)
{
}

inline ElementDescendantIterator::ElementDescendantIterator(Element* current)
    : m_current(current)
#if ASSERT_ENABLED
    , m_assertions(current)
#endif
{
    m_ancestorSiblingStack.uncheckedAppend(nullptr);
}

inline void ElementDescendantIterator::dropAssertions()
{
#if ASSERT_ENABLED
    m_assertions.clear();
#endif
}

ALWAYS_INLINE ElementDescendantIterator& ElementDescendantIterator::operator++()
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());

    Element* firstChild = ElementTraversal::firstChild(*m_current);
    Element* nextSibling = ElementTraversal::nextSibling(*m_current);

    if (firstChild) {
        if (nextSibling)
            m_ancestorSiblingStack.append(nextSibling);
        m_current = firstChild;
        return *this;
    }

    if (nextSibling) {
        m_current = nextSibling;
        return *this;
    }

    m_current = m_ancestorSiblingStack.takeLast();

#if ASSERT_ENABLED
    // Drop the assertion when the iterator reaches the end.
    if (!m_current)
        m_assertions.dropEventDispatchAssertion();
#endif

    return *this;
}

ALWAYS_INLINE ElementDescendantIterator& ElementDescendantIterator::operator--()
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());

    Element* previousSibling = ElementTraversal::previousSibling(*m_current);

    if (!previousSibling) {
        m_current = m_current->parentElement();
        // The stack optimizes for forward traversal only, this just maintains consistency.
        if (m_current->nextSibling() && m_current->nextSibling() == m_ancestorSiblingStack.last())
            m_ancestorSiblingStack.removeLast();
        return *this;
    }

    Element* deepestSibling = previousSibling;
    while (Element* lastChild = ElementTraversal::lastChild(*deepestSibling))
        deepestSibling = lastChild;
    ASSERT(deepestSibling);

    if (deepestSibling != previousSibling)
        m_ancestorSiblingStack.append(m_current);

    m_current = deepestSibling;

#if ASSERT_ENABLED
    // Drop the assertion when the iterator reaches the end.
    if (!m_current)
        m_assertions.dropEventDispatchAssertion();
#endif

    return *this;
}

inline Element& ElementDescendantIterator::operator*()
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());
    return *m_current;
}

inline Element* ElementDescendantIterator::operator->()
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());
    return m_current;
}

inline bool ElementDescendantIterator::operator==(const ElementDescendantIterator& other) const
{
    ASSERT(!m_assertions.domTreeHasMutated());
    return m_current == other.m_current;
}

inline bool ElementDescendantIterator::operator!=(const ElementDescendantIterator& other) const
{
    return !(*this == other);
}

// ElementDescendantConstIterator

inline ElementDescendantConstIterator::ElementDescendantConstIterator()
    : m_current(nullptr)
{
}

inline ElementDescendantConstIterator::ElementDescendantConstIterator(const Element* current)
    : m_current(current)
#if ASSERT_ENABLED
    , m_assertions(current)
#endif
{
    m_ancestorSiblingStack.uncheckedAppend(nullptr);
}

inline void ElementDescendantConstIterator::dropAssertions()
{
#if ASSERT_ENABLED
    m_assertions.clear();
#endif
}

ALWAYS_INLINE ElementDescendantConstIterator& ElementDescendantConstIterator::operator++()
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());

    Element* firstChild = ElementTraversal::firstChild(*m_current);
    Element* nextSibling = ElementTraversal::nextSibling(*m_current);

    if (firstChild) {
        if (nextSibling)
            m_ancestorSiblingStack.append(nextSibling);
        m_current = firstChild;
        return *this;
    }

    if (nextSibling) {
        m_current = nextSibling;
        return *this;
    }

    m_current = m_ancestorSiblingStack.takeLast();

#if ASSERT_ENABLED
    // Drop the assertion when the iterator reaches the end.
    if (!m_current)
        m_assertions.dropEventDispatchAssertion();
#endif

    return *this;
}

inline const Element& ElementDescendantConstIterator::operator*() const
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());
    return *m_current;
}

inline const Element* ElementDescendantConstIterator::operator->() const
{
    ASSERT(m_current);
    ASSERT(!m_assertions.domTreeHasMutated());
    return m_current;
}

inline bool ElementDescendantConstIterator::operator==(const ElementDescendantConstIterator& other) const
{
    ASSERT(!m_assertions.domTreeHasMutated());
    return m_current == other.m_current;
}

inline bool ElementDescendantConstIterator::operator!=(const ElementDescendantConstIterator& other) const
{
    return !(*this == other);
}

// ElementDescendantIteratorAdapter

inline ElementDescendantIteratorAdapter::ElementDescendantIteratorAdapter(ContainerNode& root)
    : m_root(root)
{
}

inline ElementDescendantIterator ElementDescendantIteratorAdapter::begin()
{
    return ElementDescendantIterator(ElementTraversal::firstChild(m_root));
}

inline ElementDescendantIterator ElementDescendantIteratorAdapter::end()
{
    return ElementDescendantIterator();
}

inline ElementDescendantIterator ElementDescendantIteratorAdapter::last()
{
    return ElementDescendantIterator(ElementTraversal::lastWithin(m_root));
}

// ElementDescendantConstIteratorAdapter

inline ElementDescendantConstIteratorAdapter::ElementDescendantConstIteratorAdapter(const ContainerNode& root)
    : m_root(root)
{
}

inline ElementDescendantConstIterator ElementDescendantConstIteratorAdapter::begin() const
{
    return ElementDescendantConstIterator(ElementTraversal::firstChild(m_root));
}

inline ElementDescendantConstIterator ElementDescendantConstIteratorAdapter::end() const
{
    return ElementDescendantConstIterator();
}

inline ElementDescendantConstIterator ElementDescendantConstIteratorAdapter::last() const
{
    return ElementDescendantConstIterator(ElementTraversal::lastWithin(m_root));
}

// Standalone functions

inline ElementDescendantIteratorAdapter elementDescendants(ContainerNode& root)
{
    return ElementDescendantIteratorAdapter(root);
}

inline ElementDescendantConstIteratorAdapter elementDescendants(const ContainerNode& root)
{
    return ElementDescendantConstIteratorAdapter(root);
}

} // namespace WebCore

namespace std {
template <> struct iterator_traits<WebCore::ElementDescendantIterator> {
    typedef WebCore::Element value_type;
};
template <> struct iterator_traits<WebCore::ElementDescendantConstIterator> {
    typedef const WebCore::Element value_type;
};
}
