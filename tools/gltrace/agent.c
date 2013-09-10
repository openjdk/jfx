/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <string.h>
 
#include <jvmti.h>

#include "os.h"
#include "iolib.h"

/*
 *    Init/fini
 */

static char tools_envvar[1024];

static void init() __attribute__ ((constructor));
static void
init()
{
    char *lib = getenv(OS_ENV_PRELOAD);
    if (lib == NULL) return;
    strncpy(tools_envvar, "JAVA_TOOL_OPTIONS=-agentpath:", sizeof(tools_envvar));
    strncat(tools_envvar, lib, sizeof(tools_envvar));
    putenv(tools_envvar);
}

static void fini() __attribute__ ((destructor));
static void fini()
{
}

static JavaVM   *jvm;
static jvmtiEnv *jvmti;
static jclass classThread;
static jmethodID mthdDumpStack;

static void JNICALL jvmti_VMInit(jvmtiEnv*, JNIEnv*, jthread);
static void JNICALL jvmti_VMStart(jvmtiEnv*, JNIEnv*);

static jvmtiEventCallbacks      callbacks =
{
          jvmti_VMInit,                 // 50   jvmtiEventVMInit;
          NULL,                         // 51   jvmtiEventVMDeath;
          NULL,                         // 52   jvmtiEventThreadStart;
          NULL,                         // 53   jvmtiEventThreadEnd;
          NULL,                         // 54   jvmtiEventClassFileLoadHook;
          NULL,                         // 55   jvmtiEventClassLoad;
          NULL,                         // 56   jvmtiEventClassPrepare;
          jvmti_VMStart,                // 57   jvmtiEventVMStart;
          NULL,                         // 58   jvmtiEventException;
          NULL,                         // 59   jvmtiEventExceptionCatch;
          NULL,                         // 60   jvmtiEventSingleStep;
          NULL,                         // 61   jvmtiEventFramePop;
          NULL,                         // 62   jvmtiEventBreakpoint;
          NULL,                         // 63   jvmtiEventFieldAccess;
          NULL,                         // 64   jvmtiEventFieldModification;
          NULL,                         // 65   jvmtiEventMethodEntry;
          NULL,                         // 66   jvmtiEventMethodExit;
          NULL,                         // 67   jvmtiEventNativeMethodBind;
          NULL,                         // 68   jvmtiEventCompiledMethodLoad;
          NULL,                         // 69   jvmtiEventCompiledMethodUnload;
          NULL,                         // 70   jvmtiEventDynamicCodeGenerated;
          NULL,                         // 71   jvmtiEventDataDumpRequest;
          NULL,                         // 72   jvmtiEventDataResetRequest;
          NULL,                         // 73   jvmtiEventMonitorWait;
          NULL,                         // 74   jvmtiEventMonitorWaited;
          NULL,                         // 75   jvmtiEventMonitorContendedEnter;
          NULL,                         // 76   jvmtiEventMonitorContendedEntered;
          NULL,                         // 77   jvmtiEventMonitorContendedExit;
          NULL,                         // 78   jvmtiEventReserved;
          NULL,                         // 79   jvmtiEventReserved;
          NULL,                         // 80   jvmtiEventReserved;
          NULL,                         // 81   jvmtiEventGarbageCollectionStart;
          NULL,                         // 82   jvmtiEventGarbageCollectionFinish;
          NULL,                         // 83   jvmtiEventObjectFree;
          NULL                          // 84   jvmtiEventVMObjectAlloc;
};

JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *_jvm, char *options, void *reserved)
{
    jvmtiError err;

    jvm = _jvm;
    if ((*jvm)->GetEnv(jvm, (void**)&jvmti, JVMTI_VERSION_1_1) != JNI_OK || jvmti == NULL) {
        return JNI_ERR;
    }
    if ((*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks)) != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL);
    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);

    return err;
}

static void JNICALL
jvmti_VMStart( jvmtiEnv *jvmti, JNIEnv *jni )
{
}

static void
gltrace_putMark(JNIEnv *jni, jclass klass, jstring jstr)
{
    uint64_t bgn = gethrtime();
    const char *str = (*jni)->GetStringUTFChars(jni, jstr, 0);
    putCmd(OPC_MARK);
    putString(str);
    (*jni)->ReleaseStringUTFChars(jni, jstr, str);
    uint64_t end = gethrtime();
    putTime(bgn, end);
}

static const JNINativeMethod class_methods[] = {
    { "_putMark",  "(Ljava/lang/String;)V", &gltrace_putMark }
};

static void JNICALL
jvmti_VMInit(jvmtiEnv *jvmti, JNIEnv *jni, jthread jthr)
{
    classThread = (*jni)->FindClass(jni, "java/lang/Thread");
    mthdDumpStack = (*jni)->GetStaticMethodID(jni, classThread, "dumpStack", "()V");
    
    /* GLTrace */
    jclass classGLTrace = (*jni)->FindClass(jni, "com/sun/javafx/logging/GLTrace");
    if ((*jni)->ExceptionOccurred(jni)) {
        (*jni)->ExceptionClear(jni);
    }
    if (classGLTrace == NULL) {
        return;
    }
    if ((*jni)->RegisterNatives(jni, classGLTrace, class_methods, (jint)1 ) != 0) {
//        fprintf(stderr, "ERROR: GLTrace methods not registered\n" );
        return;
    }
    
    /* Set GLTrace.init to true */
    jfieldID initField = (*jni)->GetStaticFieldID(jni, classGLTrace, "init", "Z" );
    (*jni)->SetStaticBooleanField(jni, classGLTrace, initField, JNI_TRUE );
    if ((*jni)->GetStaticBooleanField(jni, classGLTrace, initField ) != JNI_TRUE) {
//        fprintf(stderr, "ERROR: GLTrace.init NOT set\n");
        return;
    }
}

static void
dump_java_stack()
{
    if (jvm == NULL) return;
    JNIEnv *jni = NULL;
    if ((*jvm)->GetEnv(jvm, (void**)&jni, JNI_VERSION_1_2) == 0 && jni != NULL) {
        (*jni)->CallStaticVoidMethod(jni, classThread, mthdDumpStack);
    } 
}
