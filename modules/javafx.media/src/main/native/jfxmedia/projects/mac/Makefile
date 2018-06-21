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
GLIB_LITE_DIR = $(BASE_DIR)/gstreamer/3rd_party/glib/glib-2.56.1

OSX_PLATFORM_BASE = $(SRCBASE_DIR)/platform/osx

TARGET_NAME = lib$(BASE_NAME).dylib
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

AVF_NAME = lib$(BASE_NAME)_avf.dylib
AVF_LIB = $(BUILD_DIR)/$(AVF_NAME)

# Set up base search path for source and headers
VPATH = $(SRCBASE_DIR):$(GENERATED_HEADERS_DIR):$(JAVA_HOME)/include:$(JAVA_HOME)/include/darwin

# ------------------------------------------------------------------
# Base compile/link flags

CFLAGS += -pipe \
         -fPIC                     \
         -Werror=implicit-function-declaration \
         -DTARGET_OS_MAC=1         \
         -D_GNU_SOURCE             \
         -msse2

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

LDFLAGS += -L$(BUILD_DIR) \
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
         -DG_DISABLE_DEPRECATED    \
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

default: $(TARGET) $(AVF_LIB)
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
	$(LINKER) -dynamiclib $(JFXMEDIA_LDFLAGS) $(JFXMEDIA_OBJECTS) -o $@


# ------------------------------------------------------------------
# AVFoundation platform lib rules

AVF_OBJBASE = $(OBJBASE_DIR)/avf

AVF_LDFLAGS = $(LDFLAGS) \
              -Wl,-install_name,@rpath/$(AVF_NAME) \
              -framework AVFoundation \
              -framework CoreMedia \
              -framework Accelerate \
              -framework AudioUnit \
              -framework MediaToolbox

AVF_CFLAGS = $(CFLAGS) \
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
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CADebugPrintf.cpp \
        platform/osx/CoreAudioUtilityClasses/CoreAudio/PublicUtility/CAHostTimeBase.cpp \
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
	$(CC) $(AVF_CFLAGS) $(AVF_INCLUDES) -MD -MF $(AVF_OBJBASE)/$*.d -x c++ -c $< -o $@

$(AVF_OBJBASE)/%.o: %.mm
	@mkdir -p $(dir $@)
	$(CC) $(AVF_CFLAGS) $(AVF_INCLUDES) -MD -MF $(AVF_OBJBASE)/$*.d -x objective-c++ -c $< -o $@

$(AVF_LIB): $(TARGET) $(AVF_OBJECTS)
	@mkdir -p $(dir $@)
	$(LINKER)  $(AVF_LDFLAGS) -dynamiclib $(AVF_OBJECTS) -l$(BASE_NAME) -o $@
