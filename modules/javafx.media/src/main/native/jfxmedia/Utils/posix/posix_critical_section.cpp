/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <errno.h>
#include <Utils/JfxCriticalSection.h>

CJfxCriticalSection::CJfxCriticalSection() {
    int res;
    pthread_mutexattr_t mattr;

    /* initialize an attribute to default value */
    res = pthread_mutexattr_init(&mattr);
    if (res != 0) {
        fprintf(stderr,"in CJfxCriticalSection::CJfxCriticalSection(): pthread_mutexattr_init() failed res = %d\n",res);
        return;
    }

    res = pthread_mutexattr_settype(&mattr, PTHREAD_MUTEX_RECURSIVE);
    if (res != 0) {
        fprintf(stderr,"in CJfxCriticalSection::CJfxCriticalSection(): pthread_mutexattr_settype() failed res = %d\n",res);
        return;
    }

    /* initialize critical section mutex */
    res = pthread_mutex_init(&m_CriticalSection, &mattr);
    if (res != 0) {
        fprintf(stderr,"in CJfxCriticalSection::Create(): pthread_mutex_init() failed res = %d\n",res);
        return;
    }
}

CJfxCriticalSection::~CJfxCriticalSection() {
    pthread_mutex_destroy(&m_CriticalSection);
}

CJfxCriticalSection* CJfxCriticalSection::Create() {
    CJfxCriticalSection* new_critical_section = NULL;
    new_critical_section = new CJfxCriticalSection();
    return new_critical_section;
}

bool CJfxCriticalSection::Enter() {
    int res = pthread_mutex_lock(&m_CriticalSection);
    if (res != 0) {
        fprintf(stderr, "in enterSystemCriticalSection: pthread_mutex_lock failed res = %d", res);
        return 0;
    }
    return 1;
}

bool CJfxCriticalSection::TryEnter() {
    int res = pthread_mutex_trylock(&m_CriticalSection);
    if (res != 0) {
        if (res == EBUSY) {
            // The mutex could not be acquired because it was already locked
            return 0;
        }

        // Other error
        fprintf(stderr, "in enterSystemCriticalSection: pthread_mutex_trylock failed res = %d", res);
        return 0;
    }

    // The mutex acquired
    return 1;
}

void CJfxCriticalSection::Exit() {
    int res = pthread_mutex_unlock(&m_CriticalSection);
    if (res != 0) {
        fprintf(stderr, "in exitSystemCriticalSection: pthread_mutex_unlock failed res = %d", res);
    }
}
