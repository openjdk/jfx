/*
 *  Copyright (c) 2003-2010, Mark Borgerding. All rights reserved.
 *  This file is part of KISS FFT - https://github.com/mborgerding/kissfft
 *
 *  SPDX-License-Identifier: BSD-3-Clause
 *  See COPYING file for more information.
 */

#ifndef KISS_FFT_F32_H
#define KISS_FFT_F32_H

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <stdint.h>
#include <glib.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 ATTENTION!
 If you would like a :
 -- a utility that will handle the caching of fft objects
 -- real-only (no imaginary time component ) FFT
 -- a multi-dimensional FFT
 -- a command-line utility to perform ffts
 -- a command-line utility to perform fast-convolution filtering

 Then see kfc.h kiss_fftr_f32.h kiss_fft_f32nd.h fftutil.c kiss_fastfir.c
  in the tools/ directory.
*/

#define KISS_FFT_F32_MALLOC g_malloc
#define KISS_FFT_F32_FREE g_free
#define kiss_fft_f32_scalar float

typedef struct {
    kiss_fft_f32_scalar r;
    kiss_fft_f32_scalar i;
}kiss_fft_f32_cpx;

typedef struct kiss_fft_f32_state* kiss_fft_f32_cfg;

/*
 *  kiss_fft_f32_alloc
 *
 *  Initialize a FFT (or IFFT) algorithm's cfg/state buffer.
 *
 *  typical usage:      kiss_fft_f32_cfg mycfg=kiss_fft_f32_alloc(1024,0,NULL,NULL);
 *
 *  The return value from fft_alloc is a cfg buffer used internally
 *  by the fft routine or NULL.
 *
 *  If lenmem is NULL, then kiss_fft_f32_alloc will allocate a cfg buffer using malloc.
 *  The returned value should be free()d when done to avoid memory leaks.
 *
 *  The state can be placed in a user supplied buffer 'mem':
 *  If lenmem is not NULL and mem is not NULL and *lenmem is large enough,
 *      then the function places the cfg in mem and the size used in *lenmem
 *      and returns mem.
 *
 *  If lenmem is not NULL and ( mem is NULL or *lenmem is not large enough),
 *      then the function returns NULL and places the minimum cfg
 *      buffer size in *lenmem.
 * */

kiss_fft_f32_cfg kiss_fft_f32_alloc(int nfft,int inverse_fft,void * mem,size_t * lenmem);

/*
 * kiss_fft_f32(cfg,in_out_buf)
 *
 * Perform an FFT on a complex input buffer.
 * for a forward FFT,
 * fin should be  f[0] , f[1] , ... ,f[nfft-1]
 * fout will be   F[0] , F[1] , ... ,F[nfft-1]
 * Note that each element is complex and can be accessed like
    f[k].r and f[k].i
 * */
void kiss_fft_f32(kiss_fft_f32_cfg cfg,const kiss_fft_f32_cpx *fin,kiss_fft_f32_cpx *fout);

/*
 A more generic version of the above function. It reads its input from every Nth sample.
 * */
void kiss_fft_f32_stride(kiss_fft_f32_cfg cfg,const kiss_fft_f32_cpx *fin,kiss_fft_f32_cpx *fout,int fin_stride);

/* If kiss_fft_f32_alloc allocated a buffer, it is one contiguous
   buffer and can be simply free()d when no longer needed*/
#define kiss_fft_f32_free KISS_FFT_F32_FREE

/*
 Cleans up some memory that gets managed internally. Not necessary to call, but it might clean up
 your compiler output to call this before you exit.
*/
void kiss_fft_f32_cleanup(void);


/*
 * Returns the smallest integer k, such that k>=n and k has only "fast" factors (2,3,5)
 */
int kiss_fft_f32_next_fast_size(int n);

/* for real ffts, we need an even size */
#define kiss_fftr_f32_next_fast_size_real(n) \
        (kiss_fft_f32_next_fast_size( ((n)+1)>>1)<<1)

#ifdef __cplusplus
}
#endif

#endif
