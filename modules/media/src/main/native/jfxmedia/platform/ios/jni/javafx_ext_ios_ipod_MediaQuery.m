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

#include "javafx_ext_ios_ipod_MediaQuery.h"

#import "iPodAccess.h"


// TODO: would it be better to have one instance of IPodAccess per one Java MediaQuery?
// http://javafx-jira.kenai.com/browse/RT-27005
static IPodAccess* ipa;

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nCreateQuery
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nCreateQuery
(JNIEnv *env, jobject obj) {

    ipa = [[IPodAccess alloc] init];
    if (ipa) {
        [ipa createQuery];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nAddNumberPredicate
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nAddNumberPredicate
(JNIEnv *env, jobject obj, jint predicateKey, jint predicateValue) {

    if (ipa) {
        [ipa addNumberPredicateForKey: (int) predicateKey
                                value: (int) predicateValue];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nAddStringPredicate
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nAddStringPredicate
(JNIEnv *env, jobject obj, jint predicateKey, jstring jsPredicateValue) {

    if (ipa) {

        const char *predicateNativeString = (*env)->GetStringUTFChars(env, jsPredicateValue, 0);
        NSString *predicateValue = [NSString stringWithCString: predicateNativeString
                                                      encoding: NSUTF8StringEncoding];
        (*env)->ReleaseStringUTFChars(env, jsPredicateValue, predicateNativeString);

        [ipa addStringPredicateForKey: (int) predicateKey
                                value: predicateValue];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nSetGroupingType
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nSetGroupingType
(JNIEnv *env, jobject obj, jint groupingType) {

    if (ipa) {
        [ipa setGroupingType: (int) groupingType];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nFillItemList
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nFillItemList
(JNIEnv *env, jobject obj) {

    if (ipa) {
        [ipa fillItemListOfMediaQuery: obj jniEnv: env];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nFillCollections
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nFillCollections
(JNIEnv *env, jobject obj) {

    if (ipa) {
        [ipa fillCollectionsOfMediaQuery: obj jniEnv: env];
    }
}

/*
 * Class:     javafx_ext_ios_ipod_MediaQuery
 * Method:    nDisposeQuery
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nDisposeQuery
(JNIEnv *env, jobject obj) {

    if (ipa) {
        [ipa disposeQuery];
        [ipa release];
    }
}

