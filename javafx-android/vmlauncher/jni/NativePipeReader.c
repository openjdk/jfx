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
#include <unistd.h>
#include <stdio.h>
#include <android/log.h>
#include "com_oracle_dalvik_NativePipeReader.h"
#include "com_oracle_dalvik_NativePipeReader_StdoutStderrClient.h"

#define NPR_BUF_SIZE 128
static char buffer[NPR_BUF_SIZE + 1];
static int readfd = -1;

/*
 * Class:     com_oracle_dalvik_NativePipeReader
 * Method:    readPipe
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_oracle_dalvik_NativePipeReader_readPipe
  (JNIEnv *env, jobject thiz, jint fd) {
    int bytesread = -1;
    if (fd >= 0) {
        // read data from pipe (should block)
        // __android_log_print(3, "NPR", "Calling read(%d)\n", fd);
        bytesread = read((int)fd, buffer, NPR_BUF_SIZE);
    }
    if (bytesread > -1) {
        buffer[bytesread] = '\0';
        return (*env)->NewStringUTF(env, buffer);
    } else {
        buffer[0] = '\0';
    }
    // __android_log_print(3, "NPR", "Bytes read from pipe = %d\n", bytesread);
    return (*env)->NewStringUTF(env, buffer);
}

/*
 * Class:     com_oracle_dalvik_NativePipeReader_StdoutStderrClient
 * Method:    nativeInitPipe
 * Signature: ()I 
 *  
 * create a new pipe attaching stdout and stderr to the WRITE end 
 *  
 */
JNIEXPORT jint JNICALL Java_com_oracle_dalvik_NativePipeReader_00024StdoutStderrClient_nativeInitPipe
  (JNIEnv *env, jobject thiz) {
    int fd[2];
    int status = pipe(fd);
    if (status >= 0) {
        readfd = fd[0];
        // we have the pipe now connect stdout/stderr
        status = dup2(fd[1], STDOUT_FILENO); // connect stdin
        // __android_log_print(3, "NPR", "connected stdout to pipe %d status = %d\n", fd[1], status);
        status = dup2(fd[1], STDERR_FILENO); // connect sterr
        if (status >= 0) {
            status = fd[0];
        }
    }
    // __android_log_print(3, "NPR", "nativeInitPipe f0 = %d f1 = %d status = =%d", fd[0], fd[1], status);
    printf("nativeInitPipe f0 = %d f1 = %d status = =%d", fd[0], fd[1], status);
    return status;
}

/*
 * Class:     com_oracle_dalvik_NativePipeReader_StdoutStderrClient
 * Method:    nativeCleanupPipe
 * Signature: ()V 
 *  
 * Return stdout and stderr to previous settings and 
 * shut down pipe 
 *  
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_NativePipeReader_00024StdoutStderrClient_nativeCleanupPipe
  (JNIEnv *env, jobject thiz) {
}
