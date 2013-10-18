#
# MacOs fxplugins Makefile
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

BASE_DIR = ../../..

GLIB_VERSION = 2.28.8
GLIB_DIR = $(BASE_DIR)/3rd_party/glib/glib-$(GLIB_VERSION)

SRCBASE_DIR = $(BASE_DIR)/plugins
OBJBASE_DIR = $(BUILD_DIR)/obj/fxplugins

DIRLIST = audioconverter       \
          progressbuffer       \
          progressbuffer/posix \
          javasource           \
          avcdecoder

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

CFLAGS = -arch x86_64              \
	 -fPIC                     \
	 -Werror=implicit-function-declaration \
         -DENABLE_SOURCE_SEEKING=1 \
         -DENABLE_PULL_MODE=1      \
         -DGST_DISABLE_GST_DEBUG   \
         -DGST_DISABLE_LOADSAVE    \
         -DHAVE_STDINT_H           \
         -DGSTREAMER_LITE          \
         -DOSX                     \
         -msse2                    \
         -mmacosx-version-min=10.7

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif

INCLUDES = $(addprefix -I$(SRCBASE_DIR)/,$(DIRLIST))   \
	   -I$(SRCBASE_DIR)                            \
	   -I$(BASE_DIR)/gstreamer-lite/gstreamer      \
	   -I$(BASE_DIR)/gstreamer-lite/gstreamer/libs \
           -I$(GLIB_DIR)                               \
           -I$(GLIB_DIR)/glib                          \
           -I$(GLIB_DIR)/gmodule                       \
           -I$(GLIB_DIR)/build/osx

LDFLAGS = -mmacosx-version-min=10.7 -arch x86_64 \
          -L$(BUILD_DIR) -Wl,-install_name,@rpath/$(TARGET_NAME) \
	  -lgstreamer-lite -lglib-lite -framework ApplicationServices \
          -framework QuartzCore -framework VideoDecodeAcceleration -framework AudioToolbox -framework CoreFoundation

C_SOURCES = fxplugins.c                        \
            audioconverter/audioconverter.c    \
            progressbuffer/progressbuffer.c    \
            progressbuffer/hlsprogressbuffer.c \
            progressbuffer/posix/filecache.c   \
            javasource/javasource.c            \
            javasource/marshal.c               \
            avcdecoder/avcdecoder.c            

ifneq ($(strip $(ON2_SRCDIR)),)
DIRLIST += vp6

INCLUDES += -I$(ON2_SRCDIR)/include \
            -I$(ON2_SRCDIR)/src     \
            -I$(ON2_SRCDIR)/config/mac

C_SOURCES += vp6/flvdemux.c    \
             vp6/flvmetadata.c \
             vp6/flvparser.c   \
             vp6/vp6decoder.c

CFLAGS += -DENABLE_ON2_DECODER
endif

OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))
OBJECTS  = $(patsubst %.c,$(OBJBASE_DIR)/%.o,$(C_SOURCES))

.PHONY: default list

default: $(TARGET)

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJECTS): | $(OBJ_DIRS) $(TARGET_DIRS)

$(OBJ_DIRS):
	mkdir -p $(OBJ_DIRS)

$(TARGET_DIRS):
	mkdir -p $(TARGET_DIRS)

$(TARGET): $(OBJECTS)
	$(CC) -dynamiclib $(OBJECTS) $(ON2_LIB) $(LDFLAGS) -o $@
