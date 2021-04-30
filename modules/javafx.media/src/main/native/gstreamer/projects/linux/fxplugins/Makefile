#
# Linux Makefile for fxplugins
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

SRCBASE_DIR = ../../../plugins
OBJBASE_DIR = $(BUILD_DIR)/obj/fxplugins

DIRLIST = progressbuffer       \
          progressbuffer/posix \
          javasource

TARGET = $(BUILD_DIR)/lib$(BASE_NAME).so

CFLAGS = -fPIC                   \
         -Wformat                \
         -Wextra                 \
         -Wformat-security       \
         -fstack-protector       \
         -Werror=implicit-function-declaration \
         -Werror=trampolines     \
         -fbuiltin               \
         -DHAVE_STDINT_H         \
         -DLINUX                 \
         -DENABLE_PULL_MODE      \
         -DENABLE_SOURCE_SEEKING \
         -D__MEDIALIB_OLD_NAMES  \
         -DGST_DISABLE_LOADSAVE  \
         -DGST_DISABLE_GST_DEBUG \
         -DGSTREAMER_LITE \
         -ffunction-sections -fdata-sections

ifneq ($(ARCH), aarch64)
    CFLAGS += -msse2
endif

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -g -Wall
endif


INCLUDES = -I$(SRCBASE_DIR) \
	   $(addprefix -I$(SRCBASE_DIR)/,$(DIRLIST)) \
           -I../../../gstreamer-lite/gstreamer       \
           -I../../../gstreamer-lite/gstreamer/libs

PACKAGES_INCLUDES := $(shell pkg-config --cflags glib-2.0)
PACKAGES_LIBS := $(shell pkg-config --libs glib-2.0 gobject-2.0)

LDFLAGS = -L$(BUILD_DIR) -lgstreamer-lite $(PACKAGES_LIBS) \
          -z relro \
          -static-libgcc -static-libstdc++ -Wl,--gc-sections

ifeq ($(ARCH), x32)
    CFLAGS += -m32
    LDFLAGS += -m32
endif

SOURCES = fxplugins.c                        \
          progressbuffer/progressbuffer.c    \
          progressbuffer/hlsprogressbuffer.c \
          progressbuffer/posix/filecache.c   \
          javasource/javasource.c            \
          javasource/marshal.c

OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))
OBJECTS = $(patsubst %.c,$(OBJBASE_DIR)/%.o,$(SOURCES))

.PHONY: default list

default: $(TARGET)

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(PACKAGES_INCLUDES) -c $< -o $@

$(OBJECTS): | $(OBJ_DIRS) $(TARGET_DIRS)

$(OBJ_DIRS):
	mkdir -p $(OBJ_DIRS)

$(TARGET_DIRS):
	mkdir -p $(TARGET_DIRS)

$(TARGET): $(OBJECTS)
	$(LINKER) -shared $(OBJECTS) $(LDFLAGS) -o $@
