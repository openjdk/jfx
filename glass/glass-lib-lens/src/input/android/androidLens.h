/* 
 * File:   androidLens.h
 * Author: tb115823
 *
 * Created on May 27, 2013, 4:37 PM
 */

#ifndef ANDROIDLENS_H
#define	ANDROIDLENS_H

#ifdef	__cplusplus
extern "C" {
#endif

    jboolean lens_input_initialize(JNIEnv *env);

    void lens_input_shutdown();

#ifdef	__cplusplus
}
#endif

#endif	/* ANDROIDLENS_H */

