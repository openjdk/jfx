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

OSX_PLATFORM_BASE = $(SRCBASE_DIR)/platform/osx

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

# separate library for QTKit based platform
QTK_NAME = lib$(BASE_NAME)_qtkit.dylib
QTK_LIB = $(BUILD_DIR)/$(QTK_NAME)

AVF_NAME = lib$(BASE_NAME)_avf.dylib
AVF_LIB = $(BUILD_DIR)/$(AVF_NAME)

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

ifeq ($(BUILD_TYPE), Release)
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
              jni/NativeAudioEqualizer.cpp                     \
              jni/NativeAudioSpectrum.cpp                      \
              jni/NativeEqualizerBand.cpp                      \
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

.PHONY: default, checklibs

default: $(TARGET) $(QTK_LIB) $(AVF_LIB)
	@echo "Ensuring $(TARGET_NAME) does not link against QuickTime..."
	! nm -m "$(TARGET)" | grep -E "(QTKit|QuickTime)"
	@echo "Ensuring $(AVF_NAME) does not link against QuickTime..."
	! nm -m "$(AVF_LIB)" | grep -E "(QTKit|QuickTime)"


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
# AVFoundation platform lib rules

ifeq ($(BUILD_TYPE), Release)
    AVF_XCRUN_FLAGS =
else
    # add a -v flag to show the build environment when xcrun is invoked
    AVF_XCRUN_FLAGS = -v
endif

# The AVFoundation code uses ARC which is only supported by the clang compiler, so we'll
# force the use of clang for this module.
# We use --sdk macosx to force it to use the latest macosx SDK, which should be 10.8 or later
# add -l to show the full command line used to invoke clang++
AVF_CPP = xcrun -l $(AVF_XCRUN_FLAGS) clang++

AVF_OBJBASE = $(OBJBASE_DIR)/avf

# We need a full path to the right SDK
AVF_SYSROOT = $(shell xcodebuild -sdk macosx -version | grep '^Path: ' | sed 's/Path: //')

# we have to remove -mmacosx-version-min=% from CFLAGS and LDFLAGS and set it to
# 10.8 since this module requires features only in 10.8 and later
AVF_LDFLAGS = $(filter-out -mmacosx-version-min=%, $(LDFLAGS)) \
              -isysroot "$(AVF_SYSROOT)" \
              -iframework"$(AVF_SYSROOT)/System/Library/Frameworks" \
              -mmacosx-version-min=10.8 \
              -Wl,-install_name,@rpath/$(AVF_NAME) \
              -framework AVFoundation \
              -framework CoreMedia \
              -framework Accelerate \
              -framework AudioUnit \
              -framework MediaToolbox

AVF_CFLAGS = $(filter-out -mmacosx-version-min=%, $(CFLAGS)) \
             -isysroot "$(AVF_SYSROOT)" \
             -iframework"$(AVF_SYSROOT)/System/Library/Frameworks" \
             -mmacosx-version-min=10.8 \
             -DCA_AU_USE_FAST_DISPATCH=1 \
             -DCA_BASIC_AU_FEATURES=1 \
             -DCA_NO_AU_HOST_CALLBACKS=1 \
             -DCA_NO_AU_UI_FEATURES=1 \
             -DCA_USE_AUDIO_PLUGIN_ONLY=1 \
             -fobjc-arc

# CoreAudio Utility Classes provided by Apple
COREAUDIO_UTILITY_CLASSES = \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/AUBase.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/AUInputElement.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/AUOutputElement.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/AUPlugInDispatch.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/AUScopeElement.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase/ComponentBase.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/OtherBases/AUEffectBase.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/Utility/AUBuffer.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CAAudioChannelLayout.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CASpectralProcessor.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CAStreamBasicDescription.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CAVectorUnit.cpp

COREAUDIO_INCLUDES = \
        -I$(OSX_PLATFORM_BASE)/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/AUBase \
        -I$(OSX_PLATFORM_BASE)/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/OtherBases \
        -I$(OSX_PLATFORM_BASE)/CoreAudioUtilityClasses/CoreAudio/AudioUnits/AUPublic/Utility \
        -I$(OSX_PLATFORM_BASE)/CoreAudioUtilityClasses/CoreAudio/PublicUtility

AVF_SOURCES = \
        platform/osx/avf/AVFMediaPlayer.mm \
        platform/osx/avf/AVFAudioProcessor.mm \
        platform/osx/avf/AVFAudioEqualizer.cpp \
        platform/osx/avf/AVFAudioSpectrumUnit.cpp \
        platform/osx/avf/AVFSoundLevelUnit.cpp \
        platform/osx/avf/AVFKernelProcessor.cpp \
        $(COREAUDIO_UTILITY_CLASSES)


AVF_OBJECTS = $(patsubst %.mm,$(AVF_OBJBASE)/%.o,$(filter %.mm, $(AVF_SOURCES))) \
              $(patsubst %.cpp,$(AVF_OBJBASE)/%.o,$(filter %.cpp, $(AVF_SOURCES)))

AVF_INCLUDES = \
    $(INCLUDES) \
    $(COREAUDIO_INCLUDES) \
    -I$(OSX_PLATFORM_BASE)

-include $(AVF_OBJECTS:.o=.d)

$(AVF_OBJBASE)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(AVF_CPP) $(AVF_CFLAGS) $(AVF_INCLUDES) -MD -MF $(AVF_OBJBASE)/$*.d -x c++ -c $< -o $@

$(AVF_OBJBASE)/%.o: %.mm
	@mkdir -p $(dir $@)
	$(AVF_CPP) $(AVF_CFLAGS) $(AVF_INCLUDES) -MD -MF $(AVF_OBJBASE)/$*.d -x objective-c++ -c $< -o $@

$(AVF_LIB): $(TARGET) $(AVF_OBJECTS)
	@mkdir -p $(dir $@)
	$(AVF_CPP)  $(AVF_LDFLAGS) -dynamiclib $(AVF_OBJECTS) -l$(BASE_NAME) -o $@


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
