#
# MacOs glib-lite Makefile
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

SRCBASE_DIR = ../../../3rd_party/glib/glib-2.28.8/
OBJBASE_DIR = $(BUILD_DIR)/obj/glib-lite

DIRLIST = glib \
	  glib/libcharset/ \
	  glib/gnulib/ \
	  gobject/ \
	  gthread/ \
	  gmodule/

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

CFLAGS = -arch x86_64 \
	 -fPIC \
	 -Werror=implicit-function-declaration \
	 -DG_DISABLE_CAST_CHECKS \
	 -DGLIB_COMPILATION \
         -DGOBJECT_COMPILATION \
	 -DLIBDIR=\"/irrelevant/lib\" \
         -DG_LOG_DOMAIN=\"GLib\" \
         -DGSTREAMER_LITE \
         -DG_DISABLE_DEPRECATED \
         -DG_DISABLE_ASSERT \
	 -mmacosx-version-min=10.7

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif

INCLUDES = -I$(SRCBASE_DIR) \
	   -I$(SRCBASE_DIR)/glib \
	   -I$(SRCBASE_DIR)/build/osx

LDFLAGS = -mmacosx-version-min=10.7 -arch x86_64 \
          -Wl,-install_name,@rpath/$(TARGET_NAME) -Wl,-framework -Wl,CoreServices -liconv 

C_SOURCES = glib/garray.c \
            glib/gasyncqueue.c \
            glib/gatomic.c \
            glib/gbacktrace.c \
            glib/gbase64.c \
            glib/gbookmarkfile.c \
            glib/gbitlock.c \
            glib/gbuffer.c \
            glib/gcache.c \
            glib/gchecksum.c \
            glib/gcompletion.c \
            glib/gconvert.c \
            glib/gdataset.c \
            glib/gdate.c \
            glib/gdatetime.c \
            glib/gdir.c \
            glib/gerror.c \
            glib/gfileutils.c \
            glib/ghash.c \
            glib/ghook.c \
            glib/giochannel.c \
            glib/giounix.c \
            glib/gkeyfile.c \
            glib/glist.c \
            glib/gmain.c \
            glib/gmappedfile.c \
            glib/gmarkup.c \
            glib/gmem.c \
            glib/gmessages.c \
            glib/gnode.c \
            glib/goption.c \
            glib/gpattern.c \
            glib/gpoll.c \
            glib/gprimes.c \
            glib/gprintf.c \
            glib/gqsort.c \
            glib/gqueue.c \
            glib/grand.c \
            glib/grel.c \
            glib/gscanner.c \
            glib/gsequence.c \
            glib/gshell.c \
            glib/gspawn.c \
            glib/gslice.c \
            glib/gslist.c \
            glib/gstdio.c \
            glib/gstrfuncs.c \
            glib/gstring.c \
            glib/gtestutils.c \
            glib/gthread.c \
            glib/gthreadpool.c \
            glib/gtimer.c \
            glib/gtimezone.c \
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
            glib/libcharset/localcharset.c \
            glib/gnulib/asnprintf.c \
            glib/gnulib/printf-args.c \
            glib/gnulib/printf-parse.c \
            glib/gnulib/printf.c \
            glib/gnulib/vasnprintf.c \
            gobject/gatomicarray.c \
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
	$(CC) -dynamiclib $(OBJECTS) $(LDFLAGS) -o $@
