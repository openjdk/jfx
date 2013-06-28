/* jconfig.h.  Generated from jconfig.cfg by configure.  */
/* jconfig.cfg --- source file edited by configure script */
/* see jconfig.txt for explanations */

#ifdef WIN32

/* jconfig.vc --- jconfig.h for Microsoft Visual C++ on Windows 95 or NT. */
/* see jconfig.txt for explanations */

#define HAVE_PROTOTYPES
#define HAVE_UNSIGNED_CHAR
#define HAVE_UNSIGNED_SHORT
/* #define void char */
/* #define const */
#undef CHAR_IS_UNSIGNED
#define HAVE_STDDEF_H
#define HAVE_STDLIB_H
#undef NEED_BSD_STRINGS
#undef NEED_SYS_TYPES_H
#undef NEED_FAR_POINTERS        /* we presume a 32-bit flat memory model */
#undef NEED_SHORT_EXTERNAL_NAMES
#undef INCOMPLETE_TYPES_BROKEN

/* Define "boolean" as unsigned char, not int, per Windows custom */
#ifndef __RPCNDR_H__            /* don't conflict if rpcndr.h already read */
typedef unsigned char boolean;
#endif
#define HAVE_BOOLEAN            /* prevent jmorecfg.h from redefining it */


#ifdef JPEG_INTERNALS

#undef RIGHT_SHIFT_IS_UNSIGNED

#endif /* JPEG_INTERNALS */

#ifdef JPEG_CJPEG_DJPEG

#define BMP_SUPPORTED           /* BMP image file format */
#define GIF_SUPPORTED           /* GIF image file format */
#define PPM_SUPPORTED           /* PBMPLUS PPM/PGM image file format */
#undef RLE_SUPPORTED            /* Utah RLE image file format */
#define TARGA_SUPPORTED         /* Targa image file format */

#define TWO_FILE_COMMANDLINE    /* optional */
#define USE_SETMODE             /* Microsoft has setmode() */
#undef NEED_SIGNAL_CATCHER
#undef DONT_USE_B_MODE
#undef PROGRESS_REPORT          /* optional */

#endif /* JPEG_CJPEG_DJPEG */

#else /* !WIN32 */

#define HAVE_PROTOTYPES 1
#define HAVE_UNSIGNED_CHAR 1
#define HAVE_UNSIGNED_SHORT 1
/* #undef void */
/* #undef const */
/* #undef CHAR_IS_UNSIGNED */
#define HAVE_STDDEF_H 1
#define HAVE_STDLIB_H 1
#define HAVE_LOCALE_H 1
/* #undef NEED_BSD_STRINGS */
/* #undef NEED_SYS_TYPES_H */
/* #undef NEED_FAR_POINTERS */
/* #undef NEED_SHORT_EXTERNAL_NAMES */
/* Define this if you get warnings about undefined structures. */
/* #undef INCOMPLETE_TYPES_BROKEN */

#ifdef JPEG_INTERNALS

/* #undef RIGHT_SHIFT_IS_UNSIGNED */
#define INLINE __inline__
/* These are for configuring the JPEG memory manager. */
/* #undef DEFAULT_MAX_MEM */
/* #undef NO_MKTEMP */

#endif /* JPEG_INTERNALS */

#ifdef JPEG_CJPEG_DJPEG

#define BMP_SUPPORTED           /* BMP image file format */
#define GIF_SUPPORTED           /* GIF image file format */
#define PPM_SUPPORTED           /* PBMPLUS PPM/PGM image file format */
/* #undef RLE_SUPPORTED */
#define TARGA_SUPPORTED         /* Targa image file format */

/* #undef TWO_FILE_COMMANDLINE */
/* #undef NEED_SIGNAL_CATCHER */
/* #undef DONT_USE_B_MODE */

/* Define this if you want percent-done progress reports from cjpeg/djpeg. */
/* #undef PROGRESS_REPORT */

#endif /* JPEG_CJPEG_DJPEG */
#endif /* ! Win32 */
