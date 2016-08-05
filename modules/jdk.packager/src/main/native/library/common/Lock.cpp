/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include "Lock.h"


Lock::Lock(void) {
    Initialize();
}

Lock::Lock(bool Value) {
    Initialize();

    if (Value == true) {
        Enter();
    }
}

void Lock::Initialize() {
#ifdef WINDOWS
    InitializeCriticalSectionAndSpinCount(&FCriticalSection, 0x00000400);
#endif //WINDOWS
#ifdef MAC
    //FMutex =  PTHREAD_RECURSIVE_MUTEX_INITIALIZER;
#endif //MAC
#ifdef LINUX
    //FMutex =  PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;
#endif //LINUX
}

Lock::~Lock(void) {
#ifdef WINDOWS
    DeleteCriticalSection(&FCriticalSection);
#endif //WINDOWS
#ifdef POSIX
    pthread_mutex_unlock(&FMutex);
#endif //POSIX
}

void Lock::Enter() {
#ifdef WINDOWS
    EnterCriticalSection(&FCriticalSection);
#endif //WINDOWS
#ifdef POSIX
    pthread_mutex_lock(&FMutex);
#endif //POSIX
}

void Lock::Leave() {
#ifdef WINDOWS
    LeaveCriticalSection(&FCriticalSection);
#endif //WINDOWS
#ifdef POSIX
    pthread_mutex_unlock(&FMutex);
#endif //POSIX
}

bool Lock::TryEnter() {
    bool result = false;
#ifdef WINDOWS
    if (TryEnterCriticalSection (&FCriticalSection) != 0)
        result = true;
#endif //WINDOWS
#ifdef POSIX
    if (pthread_mutex_lock(&FMutex) == 0)
        result = true;
#endif //POSIX
    return result;
}
