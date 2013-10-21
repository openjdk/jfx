/* config.h.  Generated from config.h.in by configure.  */
/* config.h.in.  Generated from configure.ac by autoheader.  */

/* PREFIX -- specifically added for Windows for easier moving */
#define PREFIX "C:\\gstreamer"

/* Define if building universal (internal helper macro) */
#undef AC_APPLE_UNIVERSAL_BUILD

/* poll doesn't work on devices */
/* #undef BROKEN_POLL */

/* Buffer alignment to use */
/* #undef BUFFER_ALIGNMENT */

/* Buffer alignment by malloc default */
#define BUFFER_ALIGNMENT_MALLOC 1

/* Buffer alignment by pagesize */
/* #undef BUFFER_ALIGNMENT_PAGESIZE */

/* Define to 1 if translation of program messages to the user's native
   language is requested. */
#undef ENABLE_NLS

/* Subunit protocol result output */
#define ENABLE_SUBUNIT 0

/* gettext package name */
#define GETTEXT_PACKAGE "gstreamer-0.10"

/* data dir */
#define GST_DATADIR PREFIX "\\share"

/* Define if option parsing is disabled */
#define GST_DISABLE_OPTION_PARSING 1

/* Define if pipeline parsing code is disabled */
#define GST_DISABLE_PARSE 1

/* Extra platform specific plugin suffix */
#define GST_EXTRA_MODULE_SUFFIX ".dll"

/* macro to use to show function name */
#undef GST_FUNCTION

/* Defined if gcov is enabled to force a rebuild due to config.h changing */
#undef GST_GCOV_ENABLED

/* Defined when registry scanning through fork is unsafe */
#undef GST_HAVE_UNSAFE_FORK

/* Default errorlevel to use */
#define GST_LEVEL_DEFAULT GST_LEVEL_ERROR

/* GStreamer license */
#define GST_LICENSE "LGPL"

/* library major.minor version */
#define GST_MAJORMINOR "0.10"

/* package name in plugins */
#define GST_PACKAGE_NAME "GStreamer source release"

/* package origin */
#define GST_PACKAGE_ORIGIN "Unknown package origin"

/* GStreamer package release date/time for plugins as YYYY-MM-DD */
#define GST_PACKAGE_RELEASE_DATETIME "2011-06-15"

/* location of the installed gst-plugin-scanner */
#define GST_PLUGIN_SCANNER_INSTALLED "/usr/local/libexec/gstreamer-0.10/gst-plugin-scanner"

/* Define to 1 if you have the MacOS X function CFLocaleCopyCurrent in the
   CoreFoundation framework. */
#undef HAVE_CFLOCALECOPYCURRENT

/* Define to 1 if you have the MacOS X function CFPreferencesCopyAppValue in
   the CoreFoundation framework. */
#undef HAVE_CFPREFERENCESCOPYAPPVALUE

/* Define to 1 if you have the `clock_gettime' function. */
#undef HAVE_CLOCK_GETTIME

/* Define if the host CPU is an Alpha */
#undef HAVE_CPU_ALPHA

/* Define if the host CPU is an ARM */
#undef HAVE_CPU_ARM

/* Define if the host CPU is a CRIS */
#undef HAVE_CPU_CRIS

/* Define if the host CPU is a CRISv32 */
#undef HAVE_CPU_CRISV32

/* Define if the host CPU is a HPPA */
#undef HAVE_CPU_HPPA

/* Define if the host CPU is an x86 */
#define HAVE_CPU_I386 1

/* Define if the host CPU is a IA64 */
#undef HAVE_CPU_IA64

/* Define if the host CPU is a M68K */
#undef HAVE_CPU_M68K

/* Define if the host CPU is a MIPS */
#undef HAVE_CPU_MIPS

/* Define if the host CPU is a PowerPC */
#undef HAVE_CPU_PPC

/* Define if the host CPU is a 64 bit PowerPC */
#undef HAVE_CPU_PPC64

/* Define if the host CPU is a S390 */
#undef HAVE_CPU_S390

/* Define if the host CPU is a SPARC */
#undef HAVE_CPU_SPARC

/* Define if the host CPU is a x86_64 */
#undef HAVE_CPU_X86_64

/* Define if the GNU dcgettext() function is already present or preinstalled.
   */
#undef HAVE_DCGETTEXT

/* Defined if we have dladdr () */
#undef HAVE_DLADDR

/* Define to 1 if you have the <dlfcn.h> header file. */
#undef HAVE_DLFCN_H

/* Define to 1 if you have the `fgetpos' function. */
#define HAVE_FGETPOS 1

/* Define to 1 if fseeko (and presumably ftello) exists and is declared. */
#undef HAVE_FSEEKO

/* Define to 1 if you have the `fsetpos' function. */
#define HAVE_FSETPOS 1

/* Define to 1 if you have the `ftello' function. */
#undef HAVE_FTELLO

/* defined if the compiler implements __func__ */
#undef HAVE_FUNC

/* defined if the compiler implements __FUNCTION__ */
#undef HAVE_FUNCTION

/* Define to 1 if you have the `getpagesize' function. */
#undef HAVE_GETPAGESIZE

/* Define if the GNU gettext() function is already present or preinstalled. */
#undef HAVE_GETTEXT

/* Have GMP library */
#undef HAVE_GMP

/* Define to 1 if you have the `gmtime_r' function. */
#undef HAVE_GMTIME_R

/* Have GSL library */
#undef HAVE_GSL

/* Define if you have the iconv() function and it works. */
#undef HAVE_ICONV

/* Define to 1 if you have the <inttypes.h> header file. */
#undef HAVE_INTTYPES_H

/* Define to 1 if you have the `nsl' library (-lnsl). */
#undef HAVE_LIBNSL

/* Define to 1 if you have the `resolv' library (-lresolv). */
#undef HAVE_LIBRESOLV

/* Define to 1 if you have the `socket' library (-lsocket). */
#undef HAVE_LIBSOCKET

/* Define if libxml2 is available */
#define HAVE_LIBXML2 1

/* Define to 1 if you have the `localtime_r' function. */
#undef HAVE_LOCALTIME_R
/* Define to 1 if you have the <memory.h> header file. */
#undef HAVE_MEMORY_H

/* Define to 1 if you have a working `mmap' system call. */
#undef HAVE_MMAP

/* Have a monotonic clock */
#undef HAVE_MONOTONIC_CLOCK

/* Defined if compiling for OSX */
/* #undef HAVE_OSX */

/* Define to 1 if you have the `poll' function. */
#undef HAVE_POLL

/* Define to 1 if you have the `posix_memalign' function. */
#undef HAVE_POSIX_MEMALIGN

/* Have posix timers */
#undef HAVE_POSIX_TIMERS

/* Define to 1 if you have the `ppoll' function. */
#undef HAVE_PPOLL

/* defined if the compiler implements __PRETTY_FUNCTION__ */
#undef HAVE_PRETTY_FUNCTION

/* Defined if we have printf specifier extensions available */
#undef HAVE_PRINTF_EXTENSION

/* Define to 1 if you have the <process.h> header file. */
#define HAVE_PROCESS_H 1

/* Define to 1 if you have the `pselect' function. */
#undef HAVE_PSELECT

/* Define to 1 if you have the <pthread.h> header file. */
#undef HAVE_PTHREAD_H

/* Define if RDTSC is available */
#undef HAVE_RDTSC

/* Define to 1 if you have the `register_printf_function' function. */
#undef HAVE_REGISTER_PRINTF_FUNCTION

/* Define to 1 if you have the `register_printf_specifier' function. */
#undef HAVE_REGISTER_PRINTF_SPECIFIER

/* Define to 1 if you have the `sigaction' function. */
#undef HAVE_SIGACTION

/* Define to 1 if you have the <stdint.h> header file. */
#undef HAVE_STDINT_H

/* Define to 1 if you have the <stdio_ext.h> header file. */
#undef HAVE_STDIO_EXT_H

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <strings.h> header file. */
#undef HAVE_STRINGS_H

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/param.h> header file. */
#undef HAVE_SYS_PARAM_H

/* Define to 1 if you have the <sys/prctl.h> header file. */
#undef HAVE_SYS_PRCTL_H

/* Define to 1 if you have the <sys/socket.h> header file. */
#undef HAVE_SYS_SOCKET_H

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/times.h> header file. */
#undef HAVE_SYS_TIMES_H

/* Define to 1 if you have the <sys/time.h> header file. */
#undef HAVE_SYS_TIME_H

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <sys/utsname.h> header file. */
#undef HAVE_SYS_UTSNAME_H

/* Define to 1 if you have the <sys/wait.h> header file. */
#undef HAVE_SYS_WAIT_H

/* Have tm_gmtoff field in struct tm */
#undef HAVE_TM_GMTOFF

/* Define to 1 if you have the <ucontext.h> header file. */
#undef HAVE_UCONTEXT_H

/* Have __uint128_t type */
/* #undef HAVE_UINT128_T */

/* defined if unaligned memory access works correctly */
#undef HAVE_UNALIGNED_ACCESS

/* Define to 1 if you have the <unistd.h> header file. */
#undef HAVE_UNISTD_H

/* Define if valgrind should be used */
#undef HAVE_VALGRIND

/* Define to 1 if you have the <valgrind/valgrind.h> header file. */
#undef HAVE_VALGRIND_VALGRIND_H

/* Defined if compiling for Windows */
#define HAVE_WIN32 1

/* Define to 1 if you have the <winsock2.h> header file. */
#define HAVE_WINSOCK2_H 1

/* the host CPU */
#define HOST_CPU "i686"

/* library dir */
#ifdef _DEBUG
#  define LIBDIR PREFIX "\\debug\\lib"
#else
#  define LIBDIR PREFIX "\\lib"
#endif

/* gettext locale dir */
#define LOCALEDIR PREFIX "\\share\\locale"

/* Define to the sub-directory in which libtool stores uninstalled libraries.
   */
#undef LT_OBJDIR

/* Define if you have no native inet_aton() function. */
#undef NO_INET_ATON

/* Define to 1 if your C compiler doesn't accept -c and -o together. */
#undef NO_MINUS_C_MINUS_O

/* Name of package */
#define PACKAGE "gstreamer"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT "http://bugzilla.gnome.org/enter_bug.cgi?product=GStreamer"

/* Define to the full name of this package. */
#define PACKAGE_NAME "GStreamer"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "GStreamer 0.10.35"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "gstreamer"

/* Define to the home page for this package. */
#define PACKAGE_URL ""

/* Define to the version of this package. */
#define PACKAGE_VERSION "0.10.35"

/* directory where plugins are located */
#ifdef _DEBUG
#  define PLUGINDIR PREFIX "\\debug\\lib\\gstreamer-0.10"
#else
#  define PLUGINDIR PREFIX "\\lib\\gstreamer-0.10"
#endif

/* The size of `char', as computed by sizeof. */
/* #undef SIZEOF_CHAR */

/* The size of `int', as computed by sizeof. */
/* #undef SIZEOF_INT */

/* The size of `long', as computed by sizeof. */
/* #undef SIZEOF_LONG */

/* The size of `short', as computed by sizeof. */
/* #undef SIZEOF_SHORT */

/* The size of `void*', as computed by sizeof. */
/* #undef SIZEOF_VOIDP */

/* Define to 1 if you have the ANSI C header files. */
#undef STDC_HEADERS

/* Define if we should poison deallocated memory */
#undef USE_POISONING

/* Version number of package */
#define VERSION "0.10.35"

/* Define WORDS_BIGENDIAN to 1 if your processor stores words with the most
   significant byte first (like Motorola and SPARC, unlike Intel). */
#if defined AC_APPLE_UNIVERSAL_BUILD
# if defined __BIG_ENDIAN__
#  define WORDS_BIGENDIAN 1
# endif
#else
# ifndef WORDS_BIGENDIAN
#  undef WORDS_BIGENDIAN
# endif
#endif

/* Number of bits in a file offset, on hosts where this is settable. */
#undef _FILE_OFFSET_BITS

/* Define to 1 to make fseeko visible on some hosts (e.g. glibc 2.2). */
#undef _LARGEFILE_SOURCE

/* Define for large files, on AIX-style hosts. */
#undef _LARGE_FILES

/* We need at least WinXP SP2 for __stat64 */
#undef __MSVCRT_VERSION__

/* Define to `__inline__' or `__inline' if that's what the C compiler
   calls it, or to nothing if 'inline' is not supported under any name.  */
#ifndef __cplusplus
#define inline __inline
#endif

#define DISABLE_ORC