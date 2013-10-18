#include <jni.h>
/* Header for class javafx_ext_ios_ipod_MediaQuery */

#ifndef _Included_javafx_ext_ios_ipod_MediaQuery
#define _Included_javafx_ext_ios_ipod_MediaQuery
#ifdef __cplusplus
extern "C" {
#endif
    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nCreateQuery
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nCreateQuery
    (JNIEnv *, jobject);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nAddNumberPredicate
     * Signature: (II)V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nAddNumberPredicate
    (JNIEnv *, jobject, jint, jint);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nAddStringPredicate
     * Signature: (ILjava/lang/String;)V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nAddStringPredicate
    (JNIEnv *, jobject, jint, jstring);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nSetGroupingType
     * Signature: (I)V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nSetGroupingType
    (JNIEnv *, jobject, jint);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nFillItemList
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nFillItemList
    (JNIEnv *, jobject);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nFillCollections
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nFillCollections
    (JNIEnv *, jobject);

    /*
     * Class:     javafx_ext_ios_ipod_MediaQuery
     * Method:    nDisposeQuery
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_javafx_ext_ios_ipod_MediaQuery_nDisposeQuery
    (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
