#
# MacOS glib-lite Makefile
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

SRCBASE_DIR = ../../../3rd_party/glib/
LIBFFI_DIR = ../../../3rd_party/libffi
OBJBASE_DIR = $(BUILD_DIR)/obj/glib-lite

DIRLIST = glib \
	  glib/libcharset/ \
	  glib/gnulib/ \
	  glib/pcre/ \
	  gobject/ \
	  gthread/ \
	  gmodule/

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

CFLAGS += -fPIC \
         -Werror=implicit-function-declaration \
         -DG_DISABLE_CAST_CHECKS \
         -DGLIB_COMPILATION \
         -DGOBJECT_COMPILATION \
         -DLIBDIR=\"/irrelevant/lib\" \
         -DG_LOG_DOMAIN=\"GLib\" \
         -DGSTREAMER_LITE \
         -DG_DISABLE_DEPRECATED \
         -DG_DISABLE_ASSERT \
         -DPCRE_STATIC \
         -DLINK_SIZE=2 \
         -DMAX_NAME_SIZE=32 \
         -DMAX_NAME_COUNT=10000 \
         -DNEWLINE=-1 \
         -DPOSIX_MALLOC_THRESHOLD=10 \
         -DMATCH_LIMIT=10000000 \
         -DMATCH_LIMIT_RECURSION=10000000

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif

INCLUDES = -I$(SRCBASE_DIR) \
	   -I$(SRCBASE_DIR)/glib \
	   -I$(SRCBASE_DIR)/build/osx \
	   -I$(LIBFFI_DIR)/include \
	   -I$(LIBFFI_DIR)/include/mac/x64

LDFLAGS += -Wl,-install_name,@rpath/$(TARGET_NAME) \
           -Wl,-framework \
           -Wl,CoreServices \
           -liconv \
           -L$(BUILD_DIR) -lffi

C_SOURCES = glib/garray.c \
            glib/gasyncqueue.c \
            glib/gatomic.c \
            glib/gbacktrace.c \
            glib/gbase64.c \
            glib/gbytes.c \
            glib/gbookmarkfile.c \
            glib/gbitlock.c \
            glib/gcharset.c \
            glib/gchecksum.c \
            glib/gconvert.c \
            glib/gdataset.c \
            glib/gdate.c \
            glib/gdatetime.c \
            glib/gdir.c \
            glib/gerror.c \
            glib/genviron.c \
            glib/ghmac.c \
            glib/gfileutils.c \
            glib/ghash.c \
            glib/ghook.c \
            glib/giochannel.c \
            glib/giounix.c \
            glib/gkeyfile.c \
            glib/glib-init.c \
            glib/glib-private.c \
            glib/glist.c \
            glib/gmain.c \
            glib/gmappedfile.c \
            glib/gmarkup.c \
            glib/gmem.c \
            glib/gmessages.c \
            glib/ggettext.c \
            glib/gnode.c \
            glib/goption.c \
            glib/gpattern.c \
            glib/gpoll.c \
            glib/gprimes.c \
            glib/gprintf.c \
            glib/gqsort.c \
            glib/gquark.c \
            glib/gqueue.c \
            glib/grand.c \
            glib/gregex.c \
            glib/grcbox.c \
            glib/grefcount.c \
            glib/gscanner.c \
            glib/gsequence.c \
            glib/gspawn.c \
            glib/gshell.c \
            glib/gslice.c \
            glib/gslist.c \
            glib/gstdio.c \
            glib/gstrfuncs.c \
            glib/gstring.c \
            glib/gstringchunk.c \
            glib/gtestutils.c \
            glib/gthread.c \
            glib/gthread-posix.c \
            glib/gthreadpool.c \
            glib/gtimer.c \
            glib/gtimezone.c \
            glib/gtrashstack.c \
	    glib/gtranslit.c \
            glib/gtree.c \
            glib/gunibreak.c \
            glib/gunidecomp.c \
            glib/guniprop.c \
            glib/gurifuncs.c \
            glib/gutf8.c \
            glib/gutils.c \
            glib/ghostutils.c \
            glib/gvarianttype.c \
            glib/gvariant.c \
            glib/gvariant-core.c \
            glib/gvariant-serialiser.c \
            glib/gvarianttypeinfo.c \
            glib/gwakeup.c \
            glib/glib-unix.c \
            glib/libcharset/localcharset.c \
            glib/gnulib/asnprintf.c \
            glib/gnulib/printf-args.c \
            glib/gnulib/printf-parse.c \
            glib/gnulib/printf.c \
            glib/gnulib/vasnprintf.c \
            glib/pcre/pcre_chartables.c \
	    glib/pcre/pcre_compile.c \
	    glib/pcre/pcre_config.c \
	    glib/pcre/pcre_dfa_exec.c \
	    glib/pcre/pcre_exec.c \
	    glib/pcre/pcre_fullinfo.c \
	    glib/pcre/pcre_get.c \
	    glib/pcre/pcre_globals.c \
	    glib/pcre/pcre_newline.c \
	    glib/pcre/pcre_ord2utf8.c \
	    glib/pcre/pcre_study.c \
	    glib/pcre/pcre_tables.c \
	    glib/pcre/pcre_xclass.c \
            gobject/gatomicarray.c \
            gobject/gbinding.c \
            gobject/gboxed.c \
            gobject/gclosure.c \
            gobject/genums.c \
            gobject/gobject.c \
            gobject/gparam.c \
            gobject/gparamspecs.c \
            gobject/gsignal.c \
            gobject/gsourceclosure.c \
            gobject/gtype.c \
            gobject/gtypemodule.c \
            gobject/gtypeplugin.c \
            gobject/gvalue.c \
            gobject/gvaluearray.c \
            gobject/gvaluetransform.c \
            gobject/gvaluetypes.c \
            gobject/gmarshal.c \
            gthread/gthread-impl.c \
            gmodule/gmodule.c

OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))
OBJECTS  = $(patsubst %.c,$(OBJBASE_DIR)/%.o,$(C_SOURCES))

.PHONY: default

default: $(TARGET)

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJECTS): | $(OBJ_DIRS) $(TARGET_DIRS)

$(OBJ_DIRS):
	mkdir -p $(OBJ_DIRS)

$(TARGET_DIRS):
	mkdir -p $(TARGET_DIRS)

$(TARGET): $(OBJECTS)
	$(LINKER) -dynamiclib $(OBJECTS) $(LDFLAGS) -o $@
