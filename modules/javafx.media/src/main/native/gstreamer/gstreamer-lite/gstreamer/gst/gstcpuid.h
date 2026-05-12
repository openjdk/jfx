// SPDX-FileCopyrightText: 2025 L. E. Segovia <amy@centricular.com>
// SPDX-License-Identifier: BSD-3-Clause

#ifndef __GST_CPUID_H__
#define __GST_CPUID_H__

#include <gst/gstconfig.h>
#include <glib.h>

G_BEGIN_DECLS

GST_API
gboolean gst_cpuid_supports_x86_mmx(void);
GST_API
gboolean gst_cpuid_supports_x86_mmxext(void);
GST_API
gboolean gst_cpuid_supports_x86_3dnow(void);

GST_API
gboolean gst_cpuid_supports_x86_sse2(void);
GST_API
gboolean gst_cpuid_supports_x86_sse3(void);
GST_API
gboolean gst_cpuid_supports_x86_ssse3(void);
GST_API
gboolean gst_cpuid_supports_x86_sse4_1(void);
GST_API
gboolean gst_cpuid_supports_x86_sse4_2(void);

GST_API
gboolean gst_cpuid_supports_x86_avx(void);
GST_API
gboolean gst_cpuid_supports_x86_avx2(void);

GST_API
gboolean gst_cpuid_supports_arm_neon(void);
GST_API
gboolean gst_cpuid_supports_arm_neon64(void);

G_END_DECLS

#endif
