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
#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>
#include <dlfcn.h>
#include "DalvikProxySelector.h"
#include "com_oracle_dalvik_VMLauncher.h"

#define FULL_VERSION "1.7.0_04-ea-b19"
#define DOT_VERSION "1.7.0_04"

typedef jint JNI_CreateJavaVM_func(JavaVM **pvm, void **penv, void *args);

typedef jint JLI_Launch_func(int argc, char ** argv, /* main argc, argc */
        int jargc, const char** jargv,          /* java args */
        int appclassc, const char** appclassv,  /* app classpath */
        const char* fullversion,                /* full version defined */
        const char* dotversion,                 /* dot version defined */
        const char* pname,                      /* program name */
        const char* lname,                      /* launcher name */
        jboolean javaargs,                      /* JAVA_ARGS */
        jboolean cpwildcard,                    /* classpath wildcard*/
        jboolean javaw,                         /* windows-only javaw */
        jint ergo                               /* ergonomics class policy */
);

JavaVM *dalvikJavaVMPtr = NULL;
JNIEnv *dalvikJNIEnvPtr = NULL;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    //Save dalvik global JavaVM pointer
    dalvikJavaVMPtr = vm;
    __android_log_print(3,"JVM", "JNI_OnLoad calling GetEnv()");
    JNIEnv* env = NULL;
    (*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4);
    __android_log_print(3,"JVM", "JNI_OnLoad calling initDalvikProxySelectorData()");
    initDalvikProxySelectorData(env);
    __android_log_print(3,"JVM", "JNI_OnLoad returning()");
    return JNI_VERSION_1_4;
}

static void logArgs(int argc, char** argv) {
    int i;
    
    for (i = 0; i < argc; i++) {
        __android_log_print(3,"JVM", "arg[%d]: %s", i, argv[i]);
    }
}

static jint launchJVM(int argc, char** argv) {
    logArgs(argc, argv);

   void* libjli = dlopen("libjli.so", RTLD_LAZY | RTLD_GLOBAL);
        __android_log_print(3,"JVM", "JLI lib = %x", (int)libjli);
   if (NULL == libjli) {
       return 0;
   }
        __android_log_print(3,"JVM", "Found JLI lib");

   JLI_Launch_func *pJLI_Launch =
          (JLI_Launch_func *)dlsym(libjli, "JLI_Launch");

        __android_log_print(3,"JVM", "JLI_Launch = 0x%x", *(int*)&pJLI_Launch);

   if (NULL == pJLI_Launch) {
        __android_log_print(3,"JVM", "JLI_Launch = NULL");
       return 0;
   }

        __android_log_print(3,"JVM", "Calling JLI_Launch");
   return pJLI_Launch(argc, argv, 0, NULL, 0, NULL, FULL_VERSION,
                          DOT_VERSION, *argv, *argv, JNI_FALSE, JNI_FALSE,
                          JNI_FALSE, 0);
}

/*
 * Class:     com_oracle_embedded_launcher_VMLauncher
 * Method:    launchJVM
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_dalvik_VMLauncher_launchJVM
  (JNIEnv *env, jclass clazz, jobjectArray argsArray) {
   jint res = 0;
   char **argv = NULL;
   int i;

        // Save dalvik JNIEnv pointer for JVM launch thread
        dalvikJNIEnvPtr = env;

        if (argsArray == NULL) {
          __android_log_print(3,"LaunchJVM", " args array null, returning ");
       //handle error
       return 0;
   }

   int argc = (*env)->GetArrayLength(env, argsArray);

   argv = calloc( (argc+1), sizeof(jbyte*) );

   //copy args
   for (i = 0; i < argc; i++) {
       jstring stringElement = (jstring) (*env)->GetObjectArrayElement(env, argsArray, i);
       const jbyte *ansiString = (*env)->GetStringUTFChars(env, stringElement, NULL);
       if (ansiString == NULL) {
           //handle error
           return 0;
       }
       argv[i] = calloc( (strlen(ansiString)+1), sizeof(jbyte) );
       if (argv[i] == NULL) {
           //handle error
           return 0;
       }
       strcpy(argv[i], ansiString);
       (*env)->ReleaseStringUTFChars(env, stringElement, (const char*)ansiString);
   }
   //add NULL element
   argv[argc] = NULL;
        __android_log_print(3,"LaunchJVM", " Done processing args ");

   res = launchJVM(argc, argv);

   //free args
   for (i = 0; i < argc; i++) {
       free(argv[i]);
   }
   free(argv);

   return res;
}


