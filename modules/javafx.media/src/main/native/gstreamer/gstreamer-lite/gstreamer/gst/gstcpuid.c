// SPDX-FileCopyrightText: 2025 L. E. Segovia <amy@centricular.com>
// SPDX-License-Identifier: BSD-3-Clause

/* Implementation is based on Xsimd's CPUID routine */
/* https://github.com/xtensor-stack/xsimd/blob/c8d69510cce459ab5d55b950d3a6d4f997d3c70f/include/xsimd/config/xsimd_cpuid.hpp */

/***************************************************************************
 * Copyright (c) Johan Mabille, Sylvain Corlay, Wolf Vollprecht and         *
 * Martin Renou                                                             *
 * Copyright (c) QuantStack                                                 *
 * Copyright (c) Serge Guelton                                              *
 *                                                                          *
 * Distributed under the terms of the BSD 3-Clause License.                 *
 *                                                                          *
 * The full license is in the file LICENSE, distributed with this software. *
 ****************************************************************************/

#include "gstcpuid.h"

// define G_ALWAYS_INLINE to force MSVC to get rid of the interstitial
// CPUID init below
// (simplified form, this is a C file)
#if !GLIB_CHECK_VERSION (2, 74, 0)
#if g_macro__has_attribute(__always_inline__)
# define G_ALWAYS_INLINE __attribute__ ((__always_inline__))
#elif defined (_MSC_VER)
  /* Use MSVC specific syntax.  */
# define G_ALWAYS_INLINE __forceinline
#else
# define G_ALWAYS_INLINE        /* empty */
#endif
#endif

#if defined(__x86_64__) || defined(__i386__) || defined(_M_AMD64) || defined(_M_IX86)
#define GST_CPUID_CHECK_X86 1
#elif defined(__aarch64__) || defined(_M_ARM64) || defined(__ARM_NEON) || defined(_M_ARM)
#define GST_CPUID_CHECK_ARM 1
#endif

#if defined(__linux__) && defined(__ARM_ARCH)
#include <asm/hwcap.h>
#include <sys/auxv.h>
#ifndef HWCAP_NEON              // Some Android NDKs lack it.
#define HWCAP_NEON (1 << 12)
#endif
#endif

#ifdef _MSC_VER
// Contains the definition of __cpuidex
#include <intrin.h>
#endif

#ifdef __APPLE__
#include <TargetConditionals.h>
#endif

#include <string.h>

#ifdef GST_CPUID_CHECK_X86
#ifndef _MSC_VER
#include <cpuid.h>
#include <immintrin.h>
#endif

static inline guint32
_get_xcr0_low (void)
{
  guint32 xcr0;

#if (defined(_MSC_VER) && _MSC_VER >= 1400)
  // On GCC/Clang this builtin requires xsave, so inline asm is needed --Amy
  // https://lists.llvm.org/pipermail/cfe-commits/Week-of-Mon-20190114/258295.html
  xcr0 = (guint32) _xgetbv (0);
#elif defined(__GNUC__)

__asm__ ("xorl %%ecx, %%ecx\n" "xgetbv\n":"=a" (xcr0)
:
#if defined(__i386__)
:    "ecx", "edx"
#else
:    "rcx", "rdx"
#endif
      );

#else /* _MSC_VER < 1400 */
#error "_MSC_VER < 1400 is not supported"
#endif /* _MSC_VER && _MSC_VER >= 1400 */
  return xcr0;
}

static inline void
_get_cpuid (int reg[4], int level, int count)
{
#if defined(_MSC_VER)
  __cpuidex (reg, level, count);

#elif defined(__INTEL_COMPILER)
  __cpuid (reg, level);

#elif defined(__GNUC__) || defined(__clang__)
  __cpuid_count (level, count, reg[0], reg[1], reg[2], reg[3]);

#if defined(__i386__) && defined(__PIC__)
  // %ebx may be the PIC register
__asm__ ("xchg{l}\t{%%}ebx, %1\n\t" "cpuid\n\t" "xchg{l}\t{%%}ebx, %1\n\t":"=a" (reg[0]), "=r" (reg[1]), "=c" (reg[2]),
      "=d" (reg
          [3])
:    "0" (level), "2" (count));

#else
__asm__ ("cpuid\n\t":"=a" (reg[0]), "=b" (reg[1]), "=c" (reg[2]),
      "=d" (reg[3])
:    "0" (level), "2" (count));
#endif
#endif
}
#endif // __x86_64__

typedef struct
{
  guint8 mmx;
  guint8 mmxext;
  guint8 _3dnow;

  guint8 sse2;
  guint8 sse3;
  guint8 ssse3;
  guint8 sse4_1;
  guint8 sse4_2;

  guint8 avx;
  guint8 avx2;

  guint8 neon;
  guint8 neon64;
} GstCpuid;

static GstCpuid cpuid;

static inline void
_get_supported_sets (void)
{
  memset (&cpuid, 0, sizeof (GstCpuid));
#if defined(_WIN32) && defined(_M_ARM64)
  // On Windows, for desktop applications, we are on always on ARMv8 (aarch64)
  // See https://gitlab.freedesktop.org/gstreamer/orc/-/commit/d89d0f7f26c1a39c5067e7ee9f46b72e51aec1d5
  cpuid.neon = TRUE;
  cpuid.neon64 = TRUE;
#elif defined (__APPLE__) && defined (__arm64__) && TARGET_OS_OSX
  // Consider all Arm64 Apple devices as including NEON -Amy
  // See https://gitlab.freedesktop.org/gstreamer/orc/-/commit/7a60e2074d425b7ad1192ff48ac87af4246a04c4
  cpuid.neon = TRUE;
  cpuid.neon64 = TRUE;
#elif defined(__ARM_ARCH)
  // If Linux, rely on getauxval; otherwise search Arm macros
  // https://developer.arm.com/documentation/dui0774/b/other-compiler-specific-features/predefined-macros
#if defined(__linux__) && (!defined(__ANDROID_API__) || __ANDROID_API__ >= 18)
  cpuid.neon = (getauxval (AT_HWCAP) & HWCAP_NEON) != 0 ? TRUE : FALSE;
#elif defined(__ARM_NEON) || defined(__aarch64__)
  cpuid.neon = TRUE;
#endif
#if defined(__aarch64__)
  cpuid.neon64 = cpuid.neon;
#endif

#elif defined(GST_CPUID_CHECK_X86)
  int regs1[4];
  _get_cpuid (regs1, 0x80000000, 0x0);

  // Supports extended functions?
  if (regs1[0] >= 0x80000001) {
    int regsext[4];
    _get_cpuid (regsext, 0x80000001, 0);

    // AMD extensions
    cpuid.mmxext = regsext[3] >> 22 & 1;
    cpuid._3dnow = regsext[3] >> 31 & 1;
  }

  _get_cpuid (regs1, 0x1, 0x0);

  // OS can explicitly disable the usage of SSE/AVX extensions
  // by setting an appropriate flag in CR0 register
  //
  // https://docs.kernel.org/admin-guide/hw-vuln/gather_data_sampling.html

  guint8 sse_state_os_enabled = 1;
  guint8 avx_state_os_enabled = 1;

  // OSXSAVE: A value of 1 indicates that the OS has set CR4.OSXSAVE[bit
  // 18] to enable XSETBV/XGETBV instructions to access XCR0 and
  // to support processor extended state management using
  // XSAVE/XRSTOR.
  const guint8 osxsave = regs1[2] >> 27 & 1;
  if (osxsave) {
    const guint32 xcr0 = _get_xcr0_low ();

    sse_state_os_enabled = xcr0 >> 1 & 1;
    avx_state_os_enabled = xcr0 >> 2 & sse_state_os_enabled;
  }

  cpuid.mmx = regs1[3] >> 23 & 1;
  cpuid.sse2 = regs1[3] >> 26 & sse_state_os_enabled;
  cpuid.sse2 = regs1[3] >> 26 & sse_state_os_enabled;
  cpuid.sse3 = regs1[2] >> 0 & sse_state_os_enabled;
  cpuid.ssse3 = regs1[2] >> 9 & sse_state_os_enabled;
  cpuid.sse4_1 = regs1[2] >> 19 & sse_state_os_enabled;
  cpuid.sse4_2 = regs1[2] >> 20 & sse_state_os_enabled;

  cpuid.avx = regs1[2] >> 28 & avx_state_os_enabled;

  int regs7[4];
  _get_cpuid (regs7, 0x7, 0x0);
  cpuid.avx2 = regs7[1] >> 5 & avx_state_os_enabled;
#endif
}

G_ALWAYS_INLINE static inline void
_gst_cpuid_initialize_supported_sets (void)
{
  static gsize once = 0;
  if (g_once_init_enter (&once)) {
    _get_supported_sets ();
    g_once_init_leave (&once, 1);
  }
}

/**
 * gst_cpuid_supports_x86_mmx
 *
 * Since: 1.28
 *
 * Returns: %TRUE if MMX is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_mmx (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.mmx;
}

/**
 * gst_cpuid_supports_x86_mmxext
 *
 * Since: 1.28
 *
 * Returns: %TRUE if extended MMX is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_mmxext (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.mmxext;
}

/**
 * gst_cpuid_supports_x86_3dnow
 *
 * Since: 1.28
 *
 * Returns: %TRUE if 3DNow! is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_3dnow (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid._3dnow;
}

/**
 * gst_cpuid_supports_x86_sse2
 *
 * Since: 1.28
 *
 * Returns: %TRUE if SSE2 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_sse2 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.sse2;
}

/**
 * gst_cpuid_supports_x86_sse3
 *
 * Since: 1.28
 *
 * Returns: %TRUE if SSE3 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_sse3 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.sse3;
}

/**
 * gst_cpuid_supports_x86_ssse3
 *
 * Since: 1.28
 *
 * Returns: %TRUE if SSSE3 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_ssse3 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.ssse3;
}

/**
 * gst_cpuid_supports_x86_sse4_1
 *
 * Since: 1.28
 *
 * Returns: %TRUE if SSE4.1 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_sse4_1 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.sse4_1;
}

/**
 * gst_cpuid_supports_x86_sse4_2
 *
 * Since: 1.28
 *
 * Returns: %TRUE if SSSE3 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_sse4_2 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.sse4_2;
}

/**
 * gst_cpuid_supports_x86_avx
 *
 * Since: 1.28
 *
 * Returns: %TRUE if AVX is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_avx (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.avx;
}

/**
 * gst_cpuid_supports_x86_avx2
 *
 * Since: 1.28
 *
 * Returns: %TRUE if avx2 is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_x86_avx2 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.avx2;
}

/**
 * gst_cpuid_supports_arm_neon
 *
 * Since: 1.28
 *
 * Returns: %TRUE if NEON (32-bit) is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_arm_neon (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.neon;
}

/**
 * gst_cpuid_supports_arm_neon64
 *
 * Since: 1.28
 *
 * Returns: %TRUE if NEON (64-bit) is supported by the CPU, %FALSE otherwise.
 */

gboolean
gst_cpuid_supports_arm_neon64 (void)
{
  _gst_cpuid_initialize_supported_sets ();
  return cpuid.neon64;
}
