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

#import "GlassAccessible.h"
#import "GlassMacros.h"
#import "com_sun_glass_ui_mac_MacAccessible.h"
#import "com_sun_glass_ui_mac_MacVariant.h"
#import "common.h"

@implementation GlassAccessible

- (id)initWithEnv:(JNIEnv*)env accessible:(jobject)acc
{
    self = [super init];
    if (self != nil) {
        self->jAccessible = (*env)->NewGlobalRef(env, acc);
    }
    return self;
}

- (void)dealloc
{
    GET_MAIN_JENV_NOWARN;
    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, jAccessible);
        GLASS_CHECK_EXCEPTION(env);
    }
    jAccessible = NULL;
    [super dealloc];
}

- (jobject)getJAccessible
{
    return self->jAccessible;
}

/* Attributes */

- (NSArray *)accessibilityAttributeNames
{
    jlongArray jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jlongArray)(*env)->CallObjectMethod(env, self->jAccessible, jAccessibilityAttributeNames);
    GLASS_CHECK_EXCEPTION(env);
    return jArrayToNSArray(env, jresult, jLongToID);
}

- (id)accessibilityAttributeValue:(NSString *)attribute
{
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue, (jlong)attribute);
    GLASS_CHECK_EXCEPTION(env);
    return variantToID(env, jresult);
}

- (BOOL)accessibilityIsAttributeSettable:(NSString *)attribute
{
    jboolean jresult = FALSE;
    GET_MAIN_JENV;
    if (env == NULL) return FALSE;
    jresult = (*env)->CallBooleanMethod(env, self->jAccessible, jAccessibilityIsAttributeSettable, (jlong)attribute);
    GLASS_CHECK_EXCEPTION(env);
    return jresult;
}

- (void)accessibilitySetValue:(id)value forAttribute:(NSString *)attribute
{
    GET_MAIN_JENV;
    if (env == NULL) return;
    (*env)->CallVoidMethod(env, self->jAccessible, jAccessibilitySetValue, (jlong)value, (jlong)attribute);
    GLASS_CHECK_EXCEPTION(env);
}

- (NSUInteger)accessibilityIndexOfChild:(id)child
{
    jlong jresult = 0;
    GET_MAIN_JENV;
    if (env == NULL) return 0;
    jresult = (*env)->CallLongMethod(env, self->jAccessible, jAccessibilityIndexOfChild, (jlong)child);
    GLASS_CHECK_EXCEPTION(env);
    if (jresult == -1) return [super accessibilityIndexOfChild: child];
    return jresult;
}

/*
- (BOOL)accessibilitySetOverrideValue:(id)value forAttribute:(NSString *)attribute
*/

- (NSUInteger)accessibilityArrayAttributeCount:(NSString *)attribute
{
    jint jresult = -1;
    GET_MAIN_JENV;
    if (env == NULL) return -1;
    jresult = (*env)->CallIntMethod(env, self->jAccessible, jAccessibilityArrayAttributeCount, (jlong)attribute);
    GLASS_CHECK_EXCEPTION(env);
    if (jresult == -1) return [super accessibilityArrayAttributeCount: attribute];
    return jresult;
}

- (NSArray *)accessibilityArrayAttributeValues:(NSString *)attribute index:(NSUInteger)index maxCount:(NSUInteger)maxCount
{
    jlongArray jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jlongArray)(*env)->CallObjectMethod(env, self->jAccessible, jAccessibilityArrayAttributeValues, (jlong)attribute, (jint)index, (jint)maxCount);
    GLASS_CHECK_EXCEPTION(env);
    if (jresult == NULL) return [super accessibilityArrayAttributeValues: attribute index: index maxCount: maxCount];
    return jArrayToNSArray(env, jresult, jLongToID);
}

/* Parameterized Attributes */

- (NSArray *)accessibilityParameterizedAttributeNames
{
    jlongArray jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jlongArray)(*env)->CallObjectMethod(env, self->jAccessible, jAccessibilityParameterizedAttributeNames);
    GLASS_CHECK_EXCEPTION(env);
    return jArrayToNSArray(env, jresult, jLongToID);
}

- (id)accessibilityAttributeValue:(NSString *)attribute forParameter:(id)parameter
{
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValueForParameter, (jlong)attribute, (jlong)parameter);
    GLASS_CHECK_EXCEPTION(env);
    return variantToID(env, jresult);
}

/* Actions */

- (NSArray *)accessibilityActionNames
{
    jlongArray jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jlongArray)(*env)->CallObjectMethod(env, self->jAccessible, jAccessibilityActionNames);
    GLASS_CHECK_EXCEPTION(env);
    return jArrayToNSArray(env, jresult, jLongToID);
}

- (NSString *)accessibilityActionDescription:(NSString *)action
{
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityActionDescription, (jlong)action);
    GLASS_CHECK_EXCEPTION(env);
    return jStringToNSString(env, jresult);
}

- (void)accessibilityPerformAction:(NSString *)action
{
    GET_MAIN_JENV;
    if (env == NULL) return;
    (*env)->CallVoidMethod(env, self->jAccessible, jAccessibilityPerformAction, (jlong)action);
    GLASS_CHECK_EXCEPTION(env);
}

/* Querying Elements */

- (BOOL)accessibilityIsIgnored
{
    BOOL result = FALSE;
    GET_MAIN_JENV;
    if (env == NULL) return FALSE;
    result = (BOOL)(*env)->CallBooleanMethod(env, self->jAccessible, jAccessibilityIsIgnored);
    GLASS_CHECK_EXCEPTION(env);
    return result;
}

- (id)accessibilityHitTest:(NSPoint)point
{
    id result = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    result = (id)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityHitTest, point.x, point.y);
    GLASS_CHECK_EXCEPTION(env);
    return result;
}

- (id)accessibilityFocusedUIElement
{
    id result = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    result = (id)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityFocusedUIElement);
    GLASS_CHECK_EXCEPTION(env);
    return result;
}

- (BOOL)accessibilityNotifiesWhenDestroyed
{
    return YES;
}

@end

NSArray* jArrayToNSArray(JNIEnv *env, jarray srcArray, jMapper mapper) {
    if (srcArray == NULL) return NULL;
    jsize size = (*env)->GetArrayLength(env, srcArray);

    // Create an autoreleased mutable array
    NSMutableArray *array = [NSMutableArray arrayWithCapacity: size];
    if (size <= 0) return array;

    /* Do not use GetPrimitiveArrayCritical, variantToID() can be used recursively */
    if (mapper == jVariantToID) {
        for (CFIndex index = 0; index < size; index++) {
            jobject element = (*env)->GetObjectArrayElement(env, srcArray, index);
            if ((*env)->ExceptionCheck(env)) return NULL;
            id variant = variantToID(env, element);
            if (variant) {
                [array addObject: variant];
            }
        }
        return array;
    }

    void* ptr = (*env)->GetPrimitiveArrayCritical(env, srcArray, 0);
    if (ptr) {
        @try {
            for (CFIndex index = 0; index < size; index++) {
                [array addObject: mapper(ptr, index)];
            }
        }
        @finally {
            (*env)->ReleasePrimitiveArrayCritical(env, srcArray, ptr, 0);
        }
    }
    return array;
}

NSString* jStringToNSString(JNIEnv *env, jstring string) {
    if (string == NULL) return NULL;
    jsize length = (*env)->GetStringLength(env, string);
    NSString *result = NULL;
    const jchar *chars =(*env)->GetStringCritical(env, string, 0);
    if (chars) {
        @try {
            result = [NSString stringWithCharacters: chars length: length];
        }
        @finally {
            (*env)->ReleaseStringCritical(env, string, chars);
        }
    }
    return result;
}

id variantToID(JNIEnv *env, jobject variant) {
    if (variant == NULL) return NULL;
    jint type = (*env)->GetIntField(env, variant, jVariantType);
    switch (type) {
    case com_sun_glass_ui_mac_MacVariant_NSArray_id: {
        jlongArray longArray = (*env)->GetObjectField(env, variant, jVariantLongArray);
        return jArrayToNSArray(env, longArray, jLongToID);
    }
    case com_sun_glass_ui_mac_MacVariant_NSArray_int: {
        jintArray intArray = (*env)->GetObjectField(env, variant, jVariantIntArray);
        return jArrayToNSArray(env, intArray, jIntToNSNumber);
    }
    case com_sun_glass_ui_mac_MacVariant_NSObject: {
        return (id)(*env)->GetLongField(env, variant, jVariantLong1);
    }
    case com_sun_glass_ui_mac_MacVariant_NSString: {
        jstring string = (*env)->GetObjectField(env, variant, jVariantString);
        return jStringToNSString(env, string);
    }
    case com_sun_glass_ui_mac_MacVariant_NSDictionary: {
        jlongArray longArray = (*env)->GetObjectField(env, variant, jVariantLongArray);
        jobjectArray objectArray = (*env)->GetObjectField(env, variant, jVariantVariantArray);
        NSArray* keys = jArrayToNSArray(env, longArray, jLongToID);
        NSArray* values = jArrayToNSArray(env, objectArray, jVariantToID);
        if (keys != NULL && values != NULL && [keys count] == [values count]) {
            return [NSDictionary dictionaryWithObjects: values forKeys: keys];
        }
        return NULL;
    }
    case com_sun_glass_ui_mac_MacVariant_NSAttributedString: {
        jstring string = (*env)->GetObjectField(env, variant, jVariantString);
        NSString* nsString = jStringToNSString(env, string);
        if (nsString == NULL) return NULL;
        NSMutableAttributedString *attrString = [[NSMutableAttributedString alloc] initWithString:nsString];
        jobjectArray stylesArray = (*env)->GetObjectField(env, variant, jVariantVariantArray);
        if (stylesArray) {
            /* Do not use GetPrimitiveArrayCritical for stylesArray */
            jsize stylesCount = (*env)->GetArrayLength(env, stylesArray);

            for (jsize index = 0; index < stylesCount; index++) {
                /* jStyle is a special MacVariant with key, location, and length */
                jobject jStyle = (*env)->GetObjectArrayElement(env, stylesArray, index);
                if ((*env)->ExceptionCheck(env)) return NULL;
                id value = variantToID(env, jStyle);
                if (value) {
                    jint location = (*env)->GetIntField(env, jStyle, jVariantLocation);
                    jint length = (*env)->GetIntField(env, jStyle, jVariantLength);
                    jlong key = (*env)->GetLongField(env, jStyle, jVariantKey);
                    if (key) {
                        [attrString addAttribute: (NSString *)key value: value range: NSMakeRange(location, length)];
                    }
                }
            }
        }
        return [attrString autorelease];
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_point: {
        jfloat x = (*env)->GetFloatField(env, variant, jVariantFloat1);
        jfloat y = (*env)->GetFloatField(env, variant, jVariantFloat2);
        return [NSValue valueWithPoint: NSMakePoint(x,y)];
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_size: {
        jfloat width = (*env)->GetFloatField(env, variant, jVariantFloat1);
        jfloat height = (*env)->GetFloatField(env, variant, jVariantFloat2);
        return [NSValue valueWithSize: NSMakeSize(width, height)];
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_rectangle: {
        jfloat x = (*env)->GetFloatField(env, variant, jVariantFloat1);
        jfloat y = (*env)->GetFloatField(env, variant, jVariantFloat2);
        jfloat width = (*env)->GetFloatField(env, variant, jVariantFloat3);
        jfloat height = (*env)->GetFloatField(env, variant, jVariantFloat4);
        return [NSValue valueWithRect: NSMakeRect(x, y, width, height)];
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_range: {
        jint start = (*env)->GetIntField(env, variant, jVariantInt1);
        jint length = (*env)->GetIntField(env, variant, jVariantInt2);
        return [NSValue valueWithRange: NSMakeRange(start, length)];
    }
    case com_sun_glass_ui_mac_MacVariant_NSNumber_Boolean: {
        jint value = (*env)->GetIntField(env, variant, jVariantInt1);
        return [NSNumber numberWithBool: value];
    }
    case com_sun_glass_ui_mac_MacVariant_NSNumber_Double: {
        jdouble value = (*env)->GetDoubleField(env, variant, jVariantDouble1);
        return [NSNumber numberWithDouble: value];
    }
    case com_sun_glass_ui_mac_MacVariant_NSNumber_Int: {
        jint value = (*env)->GetIntField(env, variant, jVariantInt1);
        return [NSNumber numberWithInt: value];
    }
    case com_sun_glass_ui_mac_MacVariant_NSDate: {
        jlong value = (*env)->GetLongField(env, variant, jVariantLong1);
        return [NSDate dateWithTimeIntervalSince1970: value];
    }
    }
    return NULL;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _initEnum
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1initEnum
  (JNIEnv *env, jclass jClass, jstring jEnumName)
{
    NSString *enumName = jStringToNSString(env, jEnumName);
    if (enumName == NULL) return FALSE;
    NSString *className = [NSString stringWithFormat: @"com/sun/glass/ui/mac/MacAccessible$%@", enumName];
    NSString *valuesSignature = [NSString stringWithFormat: @"()[Lcom/sun/glass/ui/mac/MacAccessible$%@;", enumName];

    jclass jEnumClass = (*env)->FindClass(env, [className UTF8String]);
    if (jEnumClass == NULL) return FALSE;
    jmethodID jValues = (*env)->GetStaticMethodID(env, jEnumClass, "values", [valuesSignature UTF8String]);
    if ((*env)->ExceptionCheck(env)) return FALSE;
    jmethodID jToString = (*env)->GetMethodID(env, jEnumClass, "toString", "()Ljava/lang/String;");
    if ((*env)->ExceptionCheck(env)) return FALSE;
    jfieldID jPtr = (*env)->GetFieldID(env, jEnumClass, "ptr", "J");
    if ((*env)->ExceptionCheck(env)) return FALSE;
    jobjectArray values = (jobjectArray)(*env)->CallStaticObjectMethod(env, jEnumClass, jValues);
    if ((*env)->ExceptionCheck(env)) return FALSE;
    if (values == NULL) return FALSE;
    jsize length = (*env)->GetArrayLength(env, values);
    if (length == 0) return FALSE;
    CFBundleRef bundle = CFBundleGetBundleWithIdentifier(CFSTR("com.apple.Cocoa"));
    if (bundle == NULL) return FALSE;
    int i = 0;
    NSString *customNamePrefix = @"AX";
    while (i < length) {
        jobject value = (*env)->GetObjectArrayElement(env, values, i++);
        jstring name = (jstring)(*env)->CallObjectMethod(env, value, jToString);
        if ((*env)->ExceptionCheck(env)) return FALSE;
        NSString* nsName = jStringToNSString(env, name);
        if (nsName == NULL) return FALSE;
        if ((*env)->ExceptionCheck(env)) return FALSE;
        NSRange range = [nsName rangeOfString: customNamePrefix];
        CFStringRef *data;
        if (range.location == 0) {
            nsName = [[NSString alloc] initWithString: nsName];
            data = (CFStringRef*)&nsName;
        } else {
            data = CFBundleGetDataPointerForName(bundle, (CFStringRef)nsName);
        }
        if (data == NULL) return FALSE;
        (*env)->SetLongField(env, value, jPtr, (jlong)*data);
    }
    return TRUE;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1initIDs
  (JNIEnv *env, jclass jClass)
{
    if (jAccessibleClass == NULL) {
        jAccessibleClass = (*env)->NewGlobalRef(env, jClass);
        jAccessibilityAttributeNames = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityAttributeNames", "()[J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityIsAttributeSettable = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityIsAttributeSettable", "(J)Z");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityAttributeValue = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityAttributeValue", "(J)Lcom/sun/glass/ui/mac/MacVariant;");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityActionNames = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityActionNames", "()[J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityActionDescription = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityActionDescription", "(J)Ljava/lang/String;");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityIndexOfChild = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityIndexOfChild", "(J)J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityFocusedUIElement = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityFocusedUIElement", "()J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityIsIgnored = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityIsIgnored", "()Z");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityHitTest = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityHitTest", "(FF)J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityPerformAction = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityPerformAction", "(J)V");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityParameterizedAttributeNames = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityParameterizedAttributeNames", "()[J");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityAttributeValueForParameter = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityAttributeValueForParameter", "(JJ)Lcom/sun/glass/ui/mac/MacVariant;");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilitySetValue = (*env)->GetMethodID(env, jAccessibleClass, "accessibilitySetValue", "(JJ)V");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityArrayAttributeCount = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityArrayAttributeCount", "(J)I");
        if ((*env)->ExceptionCheck(env)) return;
        jAccessibilityArrayAttributeValues = (*env)->GetMethodID(env, jAccessibleClass, "accessibilityArrayAttributeValues", "(JII)[J");
        if ((*env)->ExceptionCheck(env)) return;
    }
    if (jVariantClass == NULL) {
        /* Ignoring GlassHelper#FindClass */
        jclass weakVariantClass = (*env)->FindClass(env, "com/sun/glass/ui/mac/MacVariant");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantClass = (*env)->NewGlobalRef(env, weakVariantClass);
        jVariantInit = (*env)->GetMethodID(env, jVariantClass, "<init>", "()V");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantType = (*env)->GetFieldID(env, jVariantClass, "type", "I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantLongArray = (*env)->GetFieldID(env, jVariantClass, "longArray", "[J");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantIntArray = (*env)->GetFieldID(env, jVariantClass, "intArray", "[I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantStringArray = (*env)->GetFieldID(env, jVariantClass, "stringArray", "[Ljava/lang/String;");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantInt1 = (*env)->GetFieldID(env, jVariantClass, "int1", "I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantInt2 = (*env)->GetFieldID(env, jVariantClass, "int2", "I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantLong1 = (*env)->GetFieldID(env, jVariantClass, "long1", "J");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantFloat1 = (*env)->GetFieldID(env, jVariantClass, "float1", "F");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantFloat2 = (*env)->GetFieldID(env, jVariantClass, "float2", "F");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantFloat3 = (*env)->GetFieldID(env, jVariantClass, "float3", "F");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantFloat4 = (*env)->GetFieldID(env, jVariantClass, "float4", "F");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantDouble1 = (*env)->GetFieldID(env, jVariantClass, "double1", "D");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantString = (*env)->GetFieldID(env, jVariantClass, "string", "Ljava/lang/String;");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantVariantArray = (*env)->GetFieldID(env, jVariantClass, "variantArray", "[Lcom/sun/glass/ui/mac/MacVariant;");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantLocation = (*env)->GetFieldID(env, jVariantClass, "location", "I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantLength = (*env)->GetFieldID(env, jVariantClass, "length", "I");
        if ((*env)->ExceptionCheck(env)) return;
        jVariantKey = (*env)->GetFieldID(env, jVariantClass, "key", "J");
        if ((*env)->ExceptionCheck(env)) return;
    }

    jLongToID = ^(void* data, CFIndex index) {
        return (id)((jlong*)data)[index];
    };

    jIntToNSNumber = ^(void* data, CFIndex index) {
        return (id)[NSNumber numberWithInt: ((jint*)data)[index]];
    };

    /* Special case, handled internally in jArrayToNSArray() */
    jVariantToID = NULL;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _createGlassAccessible
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1createGlassAccessible
  (JNIEnv *env, jobject jAccessible)
{
    GlassAccessible* accessible = NULL;
    accessible = [[GlassAccessible alloc] initWithEnv: env accessible: jAccessible];
    return ptr_to_jlong(accessible);
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _destroyGlassAccessible
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1destroyGlassAccessible
  (JNIEnv *env, jobject jAccessible, jlong macAccessible)
{
    GlassAccessible* accessible = (GlassAccessible*)jlong_to_ptr(macAccessible);
    [accessible release];
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    idToMacVariant
 * Signature: (JI)Lcom/sun/glass/ui/mac/MacVariant;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacAccessible_idToMacVariant
  (JNIEnv *env, jclass jClass, jlong id, jint type)
{
    jobject jVariant = (*env)->NewObject(env, jVariantClass, jVariantInit);
    if (jVariant == NULL) return NULL;
    (*env)->SetIntField(env, jVariant, jVariantType, type);

    switch (type) {

    case com_sun_glass_ui_mac_MacVariant_NSNumber_Boolean: {
        NSNumber* n = (NSNumber*)id;
        BOOL value = [n boolValue];
        (*env)->SetIntField(env, jVariant, jVariantInt1, (jint)value);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSNumber_Double: {
        NSNumber* n = (NSNumber*)id;
        double value = [n doubleValue];
        (*env)->SetDoubleField(env, jVariant, jVariantDouble1, value);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSNumber_Int: {
        NSNumber* n = (NSNumber*)id;
        int value = [n intValue];
        (*env)->SetIntField(env, jVariant, jVariantInt1, value);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSString: {
        NSString* n = (NSString*)id;
        jstring value = (*env)->NewStringUTF(env, [n UTF8String]);
        if (value) {
            (*env)->SetObjectField(env, jVariant, jVariantString, value);
        }
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSArray_id: {
        NSArray* n = (NSArray*)id;
        NSUInteger count = [n count];
        jlongArray result = (*env)->NewLongArray(env, count);
        if (result) {
            jlong* data = (*env)->GetPrimitiveArrayCritical(env, result, 0);
            if (data) {
                for (NSUInteger index = 0; index < count; index++) {
                    data[index] = (jlong) [n objectAtIndex: index];
                }
                (*env)->ReleasePrimitiveArrayCritical(env, result, data, 0);
                (*env)->SetObjectField(env, jVariant, jVariantLongArray, result);
            }
        }
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSArray_int: {
        NSArray* n = (NSArray*)id;
        NSUInteger count = [n count];
        jintArray result = (*env)->NewIntArray(env, count);
        if (result) {
            jint* data = (*env)->GetPrimitiveArrayCritical(env, result, 0);
            if (data) {
                for (NSUInteger index = 0; index < count; index++) {
                    NSNumber* value = (NSNumber*)[n objectAtIndex: index];
                    data[index] = [value intValue];
                }
                (*env)->ReleasePrimitiveArrayCritical(env, result, data, 0);
                (*env)->SetObjectField(env, jVariant, jVariantIntArray, result);
            }
        }
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_point: {
        NSValue* n = (NSValue*)id;
        NSPoint value = [n pointValue];
        (*env)->SetFloatField(env, jVariant, jVariantFloat1, value.x);
        (*env)->SetFloatField(env, jVariant, jVariantFloat2, value.y);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_size: {
        NSValue* n = (NSValue*)id;
        NSSize value = [n sizeValue];
        (*env)->SetFloatField(env, jVariant, jVariantFloat1, value.width);
        (*env)->SetFloatField(env, jVariant, jVariantFloat2, value.height);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_rectangle: {
        NSValue* n = (NSValue*)id;
        NSRect value = [n rectValue];
        (*env)->SetFloatField(env, jVariant, jVariantFloat1, value.origin.x);
        (*env)->SetFloatField(env, jVariant, jVariantFloat2, value.origin.y);
        (*env)->SetFloatField(env, jVariant, jVariantFloat3, value.size.width);
        (*env)->SetFloatField(env, jVariant, jVariantFloat4, value.size.height);
        break;
    }
    case com_sun_glass_ui_mac_MacVariant_NSValue_range: {
        NSValue* n = (NSValue*)id;
        NSRange value = [n rangeValue];
        (*env)->SetIntField(env, jVariant, jVariantInt1, value.location);
        (*env)->SetIntField(env, jVariant, jVariantInt2, value.length);
        break;
    }
    }
    return jVariant;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityUnignoredAncestor
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityUnignoredAncestor
  (JNIEnv *env, jclass jClass, jlong macAccessible)
{
    GlassAccessible* accessible = (GlassAccessible*)jlong_to_ptr(macAccessible);
    return ptr_to_jlong(NSAccessibilityUnignoredAncestor(accessible));
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityUnignoredChildren
 * Signature: ([J)[J
 */
JNIEXPORT jlongArray JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityUnignoredChildren
  (JNIEnv *env, jclass jClass, jlongArray originalChildren)
{

    NSArray* children = jArrayToNSArray(env, originalChildren, jLongToID);
    if (children == NULL) return NULL;
    NSArray* n = NSAccessibilityUnignoredChildren(children);
    NSUInteger count = [n count];
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result) {
        jlong* data = (*env)->GetPrimitiveArrayCritical(env, result, 0);
        if (data) {
            for (NSUInteger index = 0; index < count; index++) {
                data[index] = (jlong) [n objectAtIndex: index];
            }
        }
        (*env)->ReleasePrimitiveArrayCritical(env, result, data, 0);
    }
    return result;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    getString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacAccessible_getString
  (JNIEnv *env, jclass jClass, jlong nsString)
{
    NSString *string = (NSString*)jlong_to_ptr(nsString);
    return (*env)->NewStringUTF(env, [string UTF8String]);
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    isEqualToString
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacAccessible_isEqualToString
  (JNIEnv *env, jclass jClass, jlong nsString1, jlong nsString2)
{
    NSString *string1 = (NSString*)jlong_to_ptr(nsString1);
    NSString *string2 = (NSString*)jlong_to_ptr(nsString2);
    return [string1 isEqualToString: string2];
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityPostNotification
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityPostNotification
  (JNIEnv *env, jclass jClass, jlong element, jlong notification)
{
    NSAccessibilityPostNotification((id)jlong_to_ptr(element), (NSString*)jlong_to_ptr(notification));
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityRoleDescription
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityRoleDescription
  (JNIEnv *env, jclass jClass, jlong nsRole, jlong nsSubrole)
{
    NSString *role = (NSString*)jlong_to_ptr(nsRole);
    NSString *subrole = (NSString*)jlong_to_ptr(nsSubrole);
    NSString *desc = NSAccessibilityRoleDescription(role, subrole);
    return desc ? (*env)->NewStringUTF(env, [desc UTF8String]) : NULL;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityActionDescription
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityActionDescription
  (JNIEnv *env, jclass jClass, jlong nsAction)
{
    NSString *action = (NSString*)jlong_to_ptr(nsAction);
    NSString *desc = NSAccessibilityActionDescription(action);
    return desc ? (*env)->NewStringUTF(env, [desc UTF8String]) : NULL;
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    GlassAccessibleToMacAccessible
 * Signature: (J)Lcom/sun/glass/ui/mac/MacAccessible;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacAccessible_GlassAccessibleToMacAccessible
  (JNIEnv *env, jclass jClass, jlong glassAccessible)
{
    GlassAccessible* accessible = (GlassAccessible*)jlong_to_ptr(glassAccessible);
    return accessible ? [accessible getJAccessible] : NULL;
}

