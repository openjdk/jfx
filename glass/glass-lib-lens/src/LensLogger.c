/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include "LensCommon.h"

#include <assert.h>
#include <execinfo.h>
#include <sys/types.h>
#include <linux/unistd.h>
#include <sys/syscall.h>
#include <unistd.h>

jint glass_log_level;

static jobject glass_logger;
static JavaVM *glass_vm;

static jmethodID glass_log_severe;
static jmethodID glass_log_warning;
static jmethodID glass_log_info;
static jmethodID glass_log_config;
static jmethodID glass_log_fine;
static jmethodID glass_log_finer;
static jmethodID glass_log_finest;

/* If a log message (including its file name and line number) contains one of
 * the backtraceTags, a stack dump will be output at that point.
 * backtraceTags are defined as a comma-separated list in the environment
 * variable LENS_BACKTRACE */
static char **backtraceTags = NULL;

void glass_logger_init(JavaVM *vm, JNIEnv *env) {
    jclass c_LensLogger;
    jclass c_PlatformLogger;
    jmethodID m_getLogger;
    jmethodID m_getLevel;
    jobject logger;
    const char *lensBacktrace = getenv("LENS_BACKTRACE");
    glass_vm = vm;
    glass_log_level = 0x7fffffff; // Integer.MAX_VALUE, meaning no logging
    c_LensLogger = (*env)->FindClass(env, "com/sun/glass/ui/lens/LensLogger");
    if (c_LensLogger == NULL) {
        fprintf(stderr,
                "Could not find class com.sun.glass.ui.lens.LensLogger\n");
        return;
    }
    c_PlatformLogger = (*env)->FindClass(env,
                                         "sun/util/logging/PlatformLogger");
    if (c_PlatformLogger == NULL) {
        fprintf(stderr,
                "Could not find class sun/util/logging/PlatformLogger\n");
        return;
    }
    m_getLogger = (*env)->GetStaticMethodID(env, c_LensLogger, "getLogger",
                                            "()Lsun/util/logging/PlatformLogger;");
    if (m_getLogger == NULL) {
        fprintf(stderr,
                "Could not find method sun.util.logging.LensLogger.getLogger\n");
        return;
    }
    m_getLevel = (*env)->GetMethodID(env, c_PlatformLogger, "getLevel",
                                     "()I");
    if (m_getLevel == NULL) {
        fprintf(stderr, "Could not find method "
                "sun.util.logging.PlatformLogger.getLevel\n");
        return;
    }
    glass_log_severe = (*env)->GetMethodID(env, c_PlatformLogger,
                                           "severe", "(Ljava/lang/String;)V");
    glass_log_warning = (*env)->GetMethodID(env, c_PlatformLogger,
                                            "warning", "(Ljava/lang/String;)V");
    glass_log_info = (*env)->GetMethodID(env, c_PlatformLogger,
                                         "info", "(Ljava/lang/String;)V");
    glass_log_config = (*env)->GetMethodID(env, c_PlatformLogger,
                                           "config", "(Ljava/lang/String;)V");
    glass_log_fine = (*env)->GetMethodID(env, c_PlatformLogger,
                                         "fine", "(Ljava/lang/String;)V");
    glass_log_finer = (*env)->GetMethodID(env, c_PlatformLogger,
                                          "finer", "(Ljava/lang/String;)V");
    glass_log_finest = (*env)->GetMethodID(env, c_PlatformLogger,
                                           "finest", "(Ljava/lang/String;)V");
    if (glass_log_severe == NULL || glass_log_warning == NULL
            || glass_log_info == NULL || glass_log_config == NULL
            || glass_log_fine == NULL || glass_log_finer == NULL
            || glass_log_finest == NULL) {
        fprintf(stderr, "Could not locate logging methods in "
                "sun.util.logging.PlatformLogger\n");
        return;
    }

    logger = (*env)->CallStaticObjectMethod(env, c_LensLogger, m_getLogger);
    glass_logger = (*env)->NewGlobalRef(env, logger);
    if (glass_logger != NULL) {
        glass_log_level = (*env)->CallIntMethod(env, glass_logger, m_getLevel);
    }
    GLASS_LOG_INFO("Log level %i", glass_log_level);
    // Check LENS_BACKTRACE for backtrace tags
    if (lensBacktrace) {
        int tagCount;
        int tagIndex = 0;
        int index;
        int i;
        char *s = strdup(lensBacktrace);
        int sLen = strlen(lensBacktrace);
        if (s == NULL) {
            fprintf(stderr, "Backtrace disabled, no memory for tags\n");
        } else {
            fprintf(stderr, "LENS_BACKTRACE: %s\n", s);
            // tokenize the tag list in-place and count the tags
            tagCount = 1;
            for (index = 0; index < sLen; index++) {
                if (s[index] == ',') {
                    if (index > 0 && s[index - 1] == '\\') { // escaped comma
                        // un-escape the comma by shifting the rest of the
                        // string left one character
                        int j;
                        for (j = index; s[j] != '\000'; j++) {
                            s[j - 1] = s[j];
                        }
                        sLen --;
                        s[sLen] = '\000';
                    } else { // comma separator
                        s[index] = '\000';
                        tagCount++;
                    }
                }
            }
            // set up backtraceTags as a null-terminated list of tags
            backtraceTags = (char **) calloc(tagCount + 1, sizeof(char *));
            if (backtraceTags == NULL) {
                fprintf(stderr, "Backtrace disabled, no memory for tags\n");
            } else {
                tagIndex = 0;
                backtraceTags[tagIndex] = s;
                for (index = 0; index < sLen; index++) {
                    if (s[index] == '\000') {
                        if (strlen(backtraceTags[tagIndex]) == 0) {
                            // an empty string cannot be a tag. skip this tag.
                            backtraceTags[tagIndex] = s + index + 1;
                        } else {
                            backtraceTags[++tagIndex] = s + index + 1;
                        }
                    }
                }
                if (strlen(backtraceTags[0]) == 0) {
                    fprintf(stderr,
                            "LENS_BACKTRACE ignored, it does not define any tags\n");
                    free(backtraceTags);
                    backtraceTags = NULL;
                } else {
                    backtraceTags[tagIndex + 1] = NULL;
                    for (tagIndex = 0; backtraceTags[tagIndex]; tagIndex++) {
                        fprintf(stderr, "LENS_BACKTRACE[%i]='%s'\n",
                                tagIndex, backtraceTags[tagIndex]);
                    }
                }
            }
        }
    } // if (lensBacktrace)
}

void glass_logf(
    int level,
    const char *func, const char *path, int line,
    const char *format, ...) {
    JNIEnv *env;
    jmethodID m;
    jstring s;
    va_list argList;
    char buffer[4096];
    int length;
    int threadID;
    jboolean attachedThread = JNI_FALSE;
    jthrowable pendingException;
    char *file;

    //trim path from filename
    file = strrchr(path , '/') + 1;

    // Include in the log the thread ID and location of the message
    threadID = syscall(__NR_gettid);
    length = snprintf(buffer, sizeof(buffer),
                      "%i %s:%i %s: ", threadID, file, line, func);

    va_start(argList, format);
    length += vsnprintf(buffer + length, sizeof(buffer) - length,
                        format, argList);
    va_end(argList);

    if (backtraceTags) {
        int i;
        for (i = 0; backtraceTags[i] != NULL; i++) {
            if (strstr(buffer, backtraceTags[i]) != NULL) {
                fprintf(stderr, "LENS_BACKTRACE: Start backtrace on on tag '%s'\n",
                        backtraceTags[i]);
                glass_backtrace();
                fprintf(stderr, "LENS_BACKTRACE: End backtrace\n");
                break;
            }
        }
    }

    // Get a JNIEnv, either by looking it up or by attaching this thread
    // to the VM.
    (*glass_vm)->GetEnv(glass_vm, (void **) &env, JNI_VERSION_1_6);
    if (env == NULL) {
        (*glass_vm)->AttachCurrentThread(glass_vm, (void **) &env, NULL);
        attachedThread = JNI_TRUE;
        snprintf(buffer + length, sizeof(buffer) - length,
                 " (Not a VM thread)");
    }
    if (env == NULL) {
        // No JNIEnv available, so write the message to stderr
        // and we are done.
        fprintf(stderr, "(Cannot attach to VM): %s\n", buffer);
        return;
    }

    pendingException = (*env)->ExceptionOccurred(env);
    if (pendingException != NULL) {
        // If there is an exception pending then we won't be able to make a
        // JNI up-call until we clear the exception. So we clear the
        // exception and re-throw it before returning from this function.
        (*env)->ExceptionClear(env);
    }

    s = (*env)->NewStringUTF(env, buffer);

    // We don't have a single logging method to which we can provide the
    // logging level of the message and expect it to do the right thing.
    // Instead we have seven logging methods and we need to decide which to
    // call based on the message logging level.
    if (level >= GLASS_LOG_LEVEL_SEVERE) {
        m = glass_log_severe;
    } else if (level >= GLASS_LOG_LEVEL_WARNING) {
        m = glass_log_warning;
    } else if (level >= GLASS_LOG_LEVEL_INFO) {
        m = glass_log_info;
    } else if (level >= GLASS_LOG_LEVEL_CONFIG) {
        m = glass_log_config;
    } else if (level >= GLASS_LOG_LEVEL_FINE) {
        m = glass_log_fine;
    } else if (level >= GLASS_LOG_LEVEL_FINER) {
        m = glass_log_finer;
    } else {
        m = glass_log_finest;
    }
    (*env)->CallVoidMethod(env, glass_logger, m, s);
    (*env)->DeleteLocalRef(env, s);
    if (attachedThread) {
        // If we attached this thread to the VM in order to make a JNI call
        // then we should detach it again. This is going to kill performance
        // but bad performance in logging will be better than having logging
        // have the side effect of leaving the thread attached.
        (*glass_vm)->DetachCurrentThread(glass_vm);
    }
    if (pendingException != NULL) {
        (*env)->Throw(env, pendingException);
        (*env)->DeleteLocalRef(env, pendingException);
    }
}

void glass_backtrace() {
    JNIEnv *env;
    int i;
    void *stack[128];
    int depth = backtrace(stack, 128);
    fflush(stdout); // get stderr and stdout in sync
    // C backtrace
    for (i = 0; i < depth; i++) {
        fprintf(stderr, "LENS_BACKTRACE: ");
        backtrace_symbols_fd(stack + i, 1, STDERR_FILENO);
    }
    // Java backtrace
    (*glass_vm)->GetEnv(glass_vm, (void **) &env, JNI_VERSION_1_6);
    if (env) {
        jthrowable pendingException = (*env)->ExceptionOccurred(env);
        jclass throwableClass, stackTraceElementClass, threadClass;
        jobject throwable, thread, threadName;
        jarray stackTraceElements;
        jmethodID constructor, fillInStackTrace, getStackTrace;
        jmethodID stackTraceElementToString;
        jmethodID currentThread, getName;
        int stackTraceLength;
        int i;
        const char *nameChars;
        if (pendingException != NULL) {
            (*env)->ExceptionClear(env);
        }

        // We could just throw a new exception and call ExceptionDescribe to
        // get a stack trace. Instead we call
        //   new Throwable().fillInStackTrace().getStackTrace()
        // and print out each StackTraceElement separately. This gets neater
        // output and makes sure that all output goes to stderr.

        throwableClass = (*env)->FindClass(env, "java/lang/Throwable");
        assert(throwableClass);
        stackTraceElementClass = (*env)->FindClass(env, "java/lang/StackTraceElement");
        assert(stackTraceElementClass);
        threadClass = (*env)->FindClass(env, "java/lang/Thread");
        assert(threadClass);
        constructor = (*env)->GetMethodID(env, throwableClass, "<init>", "()V");
        assert(constructor);
        fillInStackTrace = (*env)->GetMethodID(env, throwableClass,
                                               "fillInStackTrace",
                                               "()Ljava/lang/Throwable;");
        assert(fillInStackTrace);
        getStackTrace = (*env)->GetMethodID(env, throwableClass,
                                            "getStackTrace",
                                            "()[Ljava/lang/StackTraceElement;");
        assert(getStackTrace);
        stackTraceElementToString = (*env)->GetMethodID(
                env, stackTraceElementClass, "toString", "()Ljava/lang/String;");
        assert(stackTraceElementToString);
        currentThread = (*env)->GetStaticMethodID(env, threadClass,
                                                  "currentThread",
                                                  "()Ljava/lang/Thread;");
        assert(currentThread);
        getName = (*env)->GetMethodID(env, threadClass,
                                      "getName", "()Ljava/lang/String;");
        assert(getName);

        throwable = (*env)->NewObject(env, throwableClass, constructor);
        assert(throwable);
        (*env)->DeleteLocalRef(env,
                              (*env)->CallObjectMethod(env, throwable,
                                                     fillInStackTrace));
        stackTraceElements = (*env)->CallObjectMethod(env,
                                                      throwable, getStackTrace);
        assert(stackTraceElements);
        stackTraceLength = (*env)->GetArrayLength(env, stackTraceElements);
        for (i = 0; i < stackTraceLength; i++) {
            jobject stackTraceElement = (*env)->GetObjectArrayElement(
                    env, stackTraceElements, i);
            jstring s;
            const char *cs;
            assert(stackTraceElement);
            s = (jstring) (*env)->CallObjectMethod(env, stackTraceElement,
                                                   stackTraceElementToString);
            cs = (*env)->GetStringUTFChars(env, s, NULL);
            assert(cs);
            fprintf(stderr, "LENS_BACKTRACE: %s\n", cs);
            (*env)->ReleaseStringUTFChars(env, s, cs);
            (*env)->DeleteLocalRef(env, s);
            (*env)->DeleteLocalRef(env, stackTraceElement);
        }
        (*env)->DeleteLocalRef(env, stackTraceElements);
        (*env)->DeleteLocalRef(env, throwable);
        (*env)->DeleteLocalRef(env, throwableClass);
        if (pendingException != NULL) {
            (*env)->Throw(env, pendingException);
            (*env)->DeleteLocalRef(env, pendingException);
        }
        thread = (*env)->CallStaticObjectMethod(env, threadClass, currentThread);
        threadName = (*env)->CallObjectMethod(env, thread, getName);
        nameChars = (*env)->GetStringUTFChars(env, threadName, NULL);
        assert(nameChars);
        fprintf(stderr, "LENS_BACKTRACE: Java thread '%s'\n", nameChars);
        (*env)->ReleaseStringUTFChars(env, threadName, nameChars);
        (*env)->DeleteLocalRef(env, threadName);
        (*env)->DeleteLocalRef(env, thread);
    } else {
        fprintf(stderr, "LENS_BACKTRACE: Not a Java thread\n");
    }
    fflush(stderr);
}

