#
# MacOs jfxmedia makefile
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

SRCBASE_DIR = ../..
OBJBASE_DIR = $(BUILD_DIR)/obj/jfxmedia

BASE_DIR = ../../..
GSTREAMER_LITE_DIR = $(BASE_DIR)/gstreamer/gstreamer-lite
GLIB_LITE_DIR = $(BASE_DIR)/gstreamer/3rd_party/glib/glib-2.28.8

DIRLIST = MediaManagement    \
          Locator 	     \
          PipelineManagement \
          jni                \
          Utils 	     \
          Utils/posix 	     \
          platform/gstreamer \
          platform/osx       \
          Projects/utils

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

CFLAGS = -arch x86_64 \
	 -fPIC                     \
	 -Werror=implicit-function-declaration \
         -DTARGET_OS_MAC=1         \
         -D_GNU_SOURCE             \
         -DGST_REMOVE_DEPRECATED   \
         -DGST_DISABLE_GST_DEBUG   \
         -DGST_DISABLE_LOADSAVE    \
         -DGST_DISABLE_XML         \
         -DHAVE_CONFIG_H           \
         -DJFXMEDIA_JNI_EXPORTS    \
         -DGSTREAMER_LITE          \
         -msse2                    \
         -mmacosx-version-min=10.7

ifeq ($(BUILD_TYPE), release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif

BASE_INCLUDES = -I$(JAVA_HOME)/include \
		-I$(JAVA_HOME)/include/darwin \
		-I$(SRCBASE_DIR) \
		-I$(SRCBASE_DIR)/jni \
		-I$(GENERATED_HEADERS_DIR)

INCLUDES = $(BASE_INCLUDES) \
           -I$(GLIB_LITE_DIR)/ \
           -I$(GLIB_LITE_DIR)/glib \
           -I$(GLIB_LITE_DIR)/gmodule \
           -I$(GLIB_LITE_DIR)/build/osx \
	   -I$(GSTREAMER_LITE_DIR)/gstreamer \
           -I$(GSTREAMER_LITE_DIR)/gst-plugins-base/gst-libs \
	   -I$(GSTREAMER_LITE_DIR)/gstreamer/libs

LDFLAGS = -mmacosx-version-min=10.7 -arch x86_64 \
	  -L$(BUILD_DIR) -Wl,-install_name,@rpath/$(TARGET_NAME) \
          -lgstreamer-lite -lglib-lite -lobjc \
          -framework CoreVideo -framework CoreAudio -framework QTKit \
          -framework Cocoa -framework Carbon -framework CoreServices -framework JavaVM

CPP_SOURCES =                                                  \
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

C_SOURCES =   Utils/ColorConverter.c

OBJCMM_SOURCES =                                               \
              platform/osx/OSXPlatform.mm                      \
              platform/osx/OSXMediaPlayer.mm                   \
              platform/osx/QTKMediaPlayer.mm                   \
              platform/osx/CVVideoFrame.mm

OBJCM_SOURCES =                                                \
              Utils/JObjectPeers.m                             \
              Utils/JavaUtils.m                                \
              Utils/MTObjectProxy.m

OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))
OBJECTS  = $(patsubst %.cpp,$(OBJBASE_DIR)/%.o,$(CPP_SOURCES)) \
           $(patsubst %.c,$(OBJBASE_DIR)/%.o,$(C_SOURCES)) \
           $(patsubst %.mm,$(OBJBASE_DIR)/%.o,$(OBJCMM_SOURCES)) \
           $(patsubst %.m,$(OBJBASE_DIR)/%.o,$(OBJCM_SOURCES))

.PHONY: default

default: $(TARGET)

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.cpp
	$(CC) $(CFLAGS) $(INCLUDES) -x c++ -c $< -o $@

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -x c -c $< -o $@

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.mm
	$(CC) $(CFLAGS) $(INCLUDES) -x objective-c++ -c $< -o $@

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.m
	$(CC) $(CFLAGS) $(INCLUDES) -x objective-c -c $< -o $@

$(OBJECTS): | $(OBJ_DIRS) $(TARGET_DIRS)

$(OBJ_DIRS):
	mkdir -p $(OBJ_DIRS)

$(TARGET_DIRS):
	mkdir -p $(TARGET_DIRS)

$(TARGET): $(OBJECTS)
	$(LINK) -dynamiclib $(OBJECTS) $(LDFLAGS) -o $@

