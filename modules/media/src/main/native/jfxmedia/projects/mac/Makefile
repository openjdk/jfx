#
# Mac OS X jfxmedia makefile
#

# OUTPUT_DIR, BUILD_TYPE and BASE_NAME are passed on the command line or from the 
# parent make process
BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
SRCBASE_DIR = ../..
OBJBASE_DIR = $(BUILD_DIR)/obj/jfxmedia

BASE_DIR = ../../..
GSTREAMER_LITE_DIR = $(BASE_DIR)/gstreamer/gstreamer-lite
GLIB_LITE_DIR = $(BASE_DIR)/gstreamer/3rd_party/glib/glib-2.28.8

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

# separate library for QTKit based platform
QTK_NAME = lib$(BASE_NAME)_qtkit.dylib
QTK_LIB = $(BUILD_DIR)/$(QTK_NAME)

# Set up base search path for source and headers
VPATH = $(SRCBASE_DIR):$(GENERATED_HEADERS_DIR):$(JAVA_HOME)/include:$(JAVA_HOME)/include/darwin

# ------------------------------------------------------------------
# Base compile/link flags

CFLAGS = -arch x86_64 \
         -pipe \
         -fPIC                     \
         -Werror=implicit-function-declaration \
         -DTARGET_OS_MAC=1         \
         -D_GNU_SOURCE             \
         -msse2                    \
         -mmacosx-version-min=10.7

ifeq ($(BUILD_TYPE), release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif


INCLUDES = -I$(JAVA_HOME)/include \
           -I$(JAVA_HOME)/include/darwin \
           -I$(SRCBASE_DIR) \
           -I$(SRCBASE_DIR)/jni \
           -I$(GENERATED_HEADERS_DIR)


LDFLAGS = -mmacosx-version-min=10.7 \
          -arch x86_64 \
          -L$(BUILD_DIR) \
          -lobjc \
          -framework Cocoa \
          -framework CoreVideo

# ------------------------------------------------------------------
# jfxmedia compile/link flags

JFXMEDIA_CFLAGS = $(CFLAGS) \
         -DGST_REMOVE_DEPRECATED   \
         -DGST_DISABLE_GST_DEBUG   \
         -DGST_DISABLE_LOADSAVE    \
         -DGST_DISABLE_XML         \
         -DGSTREAMER_LITE          \
         -DJFXMEDIA_JNI_EXPORTS    \
         -DHAVE_CONFIG_H           \

JFXMEDIA_INCLUDES = $(INCLUDES) \
           -I$(GLIB_LITE_DIR)/ \
           -I$(GLIB_LITE_DIR)/glib \
           -I$(GLIB_LITE_DIR)/gmodule \
           -I$(GLIB_LITE_DIR)/build/osx \
           -I$(GSTREAMER_LITE_DIR)/gstreamer \
           -I$(GSTREAMER_LITE_DIR)/gst-plugins-base/gst-libs \
           -I$(GSTREAMER_LITE_DIR)/gstreamer/libs

JFXMEDIA_LDFLAGS = $(LDFLAGS) \
          -Wl,-install_name,@rpath/$(TARGET_NAME) \
          -lgstreamer-lite \
          -lglib-lite

# ------------------------------------------------------------------
# jfxmedia sources/objects

JFXMEDIA_SOURCES =                                             \
              MediaManagement/Media.cpp                        \
              MediaManagement/MediaManager.cpp                 \
              Locator/Locator.cpp                              \
              Locator/LocatorStream.cpp                        \
              PipelineManagement/Pipeline.cpp                  \
              PipelineManagement/PipelineFactory.cpp           \
              PipelineManagement/VideoFrame.cpp                \
              PipelineManagement/Track.cpp                     \
              PipelineManagement/AudioTrack.cpp                \
              PipelineManagement/VideoTrack.cpp                \
              PipelineManagement/SubtitleTrack.cpp             \
              jni/JavaPlayerEventDispatcher.cpp                \
              jni/JniUtils.cpp                                 \
              jni/com_sun_media_jfxmedia_logging_Logger.cpp    \
              jni/Logger.cpp                                   \
              jni/JavaMediaWarningListener.cpp                 \
              jni/JavaInputStreamCallbacks.cpp                 \
              jni/JavaBandsHolder.cpp                          \
              jni/NativeVideoBuffer.cpp                        \
              Utils/MediaWarningDispatcher.cpp                 \
              Utils/LowLevelPerf.cpp                           \
              Utils/posix/posix_critical_section.cpp           \
              platform/gstreamer/GstAudioEqualizer.cpp         \
              platform/gstreamer/GstAudioPlaybackPipeline.cpp  \
              platform/gstreamer/GstAudioSpectrum.cpp          \
              platform/gstreamer/GstAVPlaybackPipeline.cpp     \
              platform/gstreamer/GstElementContainer.cpp       \
              platform/gstreamer/GstJniUtils.cpp               \
              platform/gstreamer/GstMediaManager.cpp           \
              platform/gstreamer/GstPipelineFactory.cpp        \
              platform/gstreamer/GstVideoFrame.cpp             \
              platform/gstreamer/GstPlatform.cpp               \
              platform/gstreamer/GstEqualizerBand.cpp          \
              platform/gstreamer/GstMedia.cpp                  \
              platform/gstreamer/GstMediaPlayer.cpp            \
              Utils/ColorConverter.c                           \
              Utils/JObjectPeers.m                             \
              Utils/JavaUtils.m                                \
              Utils/MTObjectProxy.m                            \
              platform/osx/OSXPlatform.mm                      \
              platform/osx/OSXMediaPlayer.mm                   \
              platform/osx/CVVideoFrame.mm

JFXMEDIA_OBJECTS = \
    $(patsubst %.c, $(OBJBASE_DIR)/%.o, $(filter %.c, $(JFXMEDIA_SOURCES))) \
    $(patsubst %.m, $(OBJBASE_DIR)/%.o, $(filter %.m, $(JFXMEDIA_SOURCES))) \
    $(patsubst %.cpp, $(OBJBASE_DIR)/%.o, $(filter %.cpp, $(JFXMEDIA_SOURCES))) \
    $(patsubst %.mm, $(OBJBASE_DIR)/%.o, $(filter %.mm, $(JFXMEDIA_SOURCES)))

# ------------------------------------------------------------------
# Rules

.PHONY: default
default: $(TARGET) $(QTK_LIB)


# ------------------------------------------------------------------
# jfxmedia rules

# auto-dependencies
-include $(JFXMEDIA_OBJECTS:.o=.d)

$(OBJBASE_DIR)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CC) $(JFXMEDIA_CFLAGS) $(JFXMEDIA_INCLUDES) -MD -MF $(OBJBASE_DIR)/$*.d -x c++ -c $< -o $@

$(OBJBASE_DIR)/%.o: %.c
	@mkdir -p $(dir $@)
	$(CC) $(JFXMEDIA_CFLAGS) $(JFXMEDIA_INCLUDES) -MD -MF $(OBJBASE_DIR)/$*.d -x c -c $< -o $@

$(OBJBASE_DIR)/%.o: %.mm
	@mkdir -p $(dir $@)
	$(CC) $(JFXMEDIA_CFLAGS) $(JFXMEDIA_INCLUDES) -MD -MF $(OBJBASE_DIR)/$*.d -x objective-c++ -c $< -o $@

$(OBJBASE_DIR)/%.o: %.m
	@mkdir -p $(dir $@)
	$(CC) $(JFXMEDIA_CFLAGS) $(JFXMEDIA_INCLUDES) -MD -MF $(OBJBASE_DIR)/$*.d -x objective-c -c $< -o $@

$(TARGET): $(JFXMEDIA_OBJECTS)
	@mkdir -p $(dir $@)
	$(LINK) -dynamiclib $(JFXMEDIA_LDFLAGS) $(JFXMEDIA_OBJECTS) -o $@


# ------------------------------------------------------------------
# QTKit platform lib rules

QTK_OBJBASE = $(OBJBASE_DIR)/qtk
QTK_LDFLAGS = $(LDFLAGS) \
          -Wl,-install_name,@rpath/$(QTK_NAME) \
          -framework QTKit

QTK_SOURCES = platform/osx/QTKMediaPlayer.mm
QTK_OBJECTS = $(patsubst %.mm,$(QTK_OBJBASE)/%.o,$(QTK_SOURCES))

-include $(QTK_OBJECTS:.o=.d)

$(QTK_OBJBASE)/%.o: %.mm
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) $(INCLUDES) -MD -MF $(QTK_OBJBASE)/$*.d -x objective-c++ -c $< -o $@

$(QTK_LIB): $(TARGET) $(QTK_OBJECTS)
	@mkdir -p $(dir $@)
	$(LINK)  $(QTK_LDFLAGS) -dynamiclib $(QTK_OBJECTS) -l$(BASE_NAME) -o $@
