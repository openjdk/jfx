/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>
#import <jni.h>

@interface GlassAccessible : NSObject {
@private
    jobject jAccessible;
}
- (id)initWithEnv:(JNIEnv*)env accessible:(jobject)jAccessible;
- (jobject)getJAccessible;
@end

/* The following blocks are used by jArrayToNSArray() to convert java types
 * within a java array to a native type to be stored in the resulting NSArray.
 */
typedef id (^jMapper)(void*, CFIndex);
jMapper jLongToID;
jMapper jIntToNSNumber;
jMapper jVariantToID;

id variantToID(JNIEnv *env, jobject variant);
NSString* jStringToNSString(JNIEnv *env, jstring string);
NSArray* jArrayToNSArray(JNIEnv *env, jarray srcArray, jMapper);

/* Accessible class IDs for classes, methods, and fields (this could be in GlassStatics.h) */

jclass jAccessibleClass;

jmethodID jAccessibilityAttributeNames;
jmethodID jAccessibilityAttributeValue;
jmethodID jAccessibilityActionNames;
jmethodID jAccessibilityIsIgnored;
jmethodID jAccessibilityFocusedUIElement;
jmethodID jAccessibilityHitTest;
jmethodID jAccessibilityPerformAction;
jmethodID jAccessibilityParameterizedAttributeNames;
jmethodID jAccessibilityAttributeValueForParameter;
jmethodID jAccessibilityIsAttributeSettable;
jmethodID jAccessibilityActionDescription;
jmethodID jAccessibilityIndexOfChild;
jmethodID jAccessibilitySetValue;
jmethodID jAccessibilityArrayAttributeCount;
jmethodID jAccessibilityArrayAttributeValues;

jclass jVariantClass;

jmethodID jVariantInit;
jfieldID jVariantType;
jfieldID jVariantLongArray;
jfieldID jVariantIntArray;
jfieldID jVariantStringArray;
jfieldID jVariantInt1;
jfieldID jVariantInt2;
jfieldID jVariantLong1;
jfieldID jVariantFloat1;
jfieldID jVariantFloat2;
jfieldID jVariantFloat3;
jfieldID jVariantFloat4;
jfieldID jVariantDouble1;
jfieldID jVariantString;
jfieldID jVariantVariantArray;
jfieldID jVariantLocation;
jfieldID jVariantLength;
jfieldID jVariantKey;

