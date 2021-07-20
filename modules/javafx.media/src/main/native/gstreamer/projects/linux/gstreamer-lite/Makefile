#
# Makefile for libgstreamer-lite
#

BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

BASE_DIR = ../../..
SRCBASE_DIR = $(BASE_DIR)/gstreamer-lite
OBJBASE_DIR = $(BUILD_DIR)/obj/gstreamer-lite

DIRLIST = gstreamer/gst \
          gstreamer/libs/gst/base \
          gstreamer/libs/gst/controller \
          gst-plugins-base/gst-libs/gst/audio \
          gst-plugins-base/gst-libs/gst/interfaces \
          gst-plugins-base/gst-libs/gst/video \
          gst-plugins-base/gst-libs/gst/tag \
          gst-plugins-base/gst-libs/gst/riff \
          gst-plugins-base/gst-libs/gst/app \
	  projects/plugins \
          gstreamer/plugins/elements \
          gst-plugins-base/gst-libs/gst/fft \
          gst-plugins-base/gst-libs/gst/pbutils \
          gst-plugins-base/gst/typefind \
          gst-plugins-base/gst/audioconvert \
          gst-plugins-base/gst/volume \
          gst-plugins-good/gst/audioparsers \
          gst-plugins-good/gst/isomp4 \
          gst-plugins-base/ext/alsa \
          gst-plugins-good/gst/audiofx \
          gst-plugins-good/gst/equalizer \
          gst-plugins-good/gst/spectrum \
          gst-plugins-good/gst/wavparse \
          gst-plugins-bad/gst/aiff \
          gst-plugins-base/gst/app

TARGET = $(BUILD_DIR)/lib$(BASE_NAME).so

CFLAGS =-fPIC                   \
        -Wformat                \
        -Wextra                 \
        -Wformat-security       \
        -fstack-protector       \
        -Werror=implicit-function-declaration \
        -Werror=trampolines     \
        -D_GNU_SOURCE           \
        -DGST_REMOVE_DEPRECATED \
        -DGSTREAMER_LITE        \
        -DHAVE_CONFIG_H         \
        -DOUTSIDE_SPEEX         \
        -DLINUX                 \
        -DGST_DISABLE_GST_DEBUG \
        -DGST_DISABLE_LOADSAVE  \
        -ffunction-sections -fdata-sections

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -g -Wall
endif

INCLUDES = -I$(BASE_DIR)/plugins                          \
	   -I$(SRCBASE_DIR)/projects/build/linux/common   \
           -I$(SRCBASE_DIR)/gstreamer                     \
           -I$(SRCBASE_DIR)/gstreamer/libs                \
           -I$(SRCBASE_DIR)/gstreamer/gst/parse           \
           -I$(SRCBASE_DIR)/gst-plugins-base              \
           -I$(SRCBASE_DIR)/gst-plugins-base/gst-libs     \
           -I$(SRCBASE_DIR)/projects/plugins              \
           -I$(SRCBASE_DIR)/gst-plugins-base/gst-libs     \
           -I$(SRCBASE_DIR)/gst-plugins-good/gst-libs     \
           -I$(SRCBASE_DIR)/gst-plugins-good/gst/isomp4   \
           -I$(SRCBASE_DIR)/gst-plugins-bad/gst-libs

PACKAGES_INCLUDES := $(shell pkg-config --cflags alsa glib-2.0)
PACKAGES_LIBS := $(shell pkg-config --libs alsa glib-2.0 gobject-2.0 gmodule-2.0 gthread-2.0)

LDFLAGS = -L$(BUILD_DIR) -lm $(PACKAGES_LIBS) \
          -z relro \
          -static-libgcc -static-libstdc++ -Wl,--gc-sections

ifeq ($(ARCH), x32)
    CFLAGS += -m32
    LDFLAGS += -m32
endif

SOURCES = gstreamer/gst/gst.c \
          gstreamer/gst/gstallocator.c \
          gstreamer/gst/gstatomicqueue.c \
          gstreamer/gst/gstbin.c \
          gstreamer/gst/gstbuffer.c \
          gstreamer/gst/gstbufferlist.c \
          gstreamer/gst/gstbufferpool.c \
          gstreamer/gst/gstbus.c \
          gstreamer/gst/gstcaps.c \
          gstreamer/gst/gstcapsfeatures.c \
          gstreamer/gst/gstchildproxy.c \
          gstreamer/gst/gstclock.c \
          gstreamer/gst/gstcontext.c \
          gstreamer/gst/gstcontrolbinding.c \
          gstreamer/gst/gstcontrolsource.c \
          gstreamer/gst/gstdatetime.c \
          gstreamer/gst/gstdebugutils.c \
          gstreamer/gst/gstdynamictypefactory.c \
          gstreamer/gst/gstelement.c \
          gstreamer/gst/gstelementfactory.c \
          gstreamer/gst/gstenumtypes.c \
          gstreamer/gst/gsterror.c \
          gstreamer/gst/gstevent.c \
          gstreamer/gst/gstformat.c \
          gstreamer/gst/gstghostpad.c \
          gstreamer/gst/gstinfo.c \
          gstreamer/gst/gstiterator.c \
          gstreamer/gst/gstmemory.c \
          gstreamer/gst/gstmessage.c \
          gstreamer/gst/gstmeta.c \
          gstreamer/gst/gstminiobject.c \
          gstreamer/gst/gstobject.c \
          gstreamer/gst/gstpad.c \
          gstreamer/gst/gstpadtemplate.c \
          gstreamer/gst/gstparamspecs.c \
          gstreamer/gst/gstparse.c \
          gstreamer/gst/gstpipeline.c \
          gstreamer/gst/gstplugin.c \
          gstreamer/gst/gstpluginfeature.c \
          gstreamer/gst/gstpluginloader.c \
          gstreamer/gst/gstpoll.c \
          gstreamer/gst/gstprotection.c \
          gstreamer/gst/gstquark.c \
          gstreamer/gst/gstquery.c \
          gstreamer/gst/gstregistry.c \
          gstreamer/gst/gstregistrybinary.c \
          gstreamer/gst/gstregistrychunks.c \
          gstreamer/gst/gstsample.c \
          gstreamer/gst/gstsegment.c \
          gstreamer/gst/gststructure.c \
          gstreamer/gst/gstsystemclock.c \
          gstreamer/gst/gststreamcollection.c \
          gstreamer/gst/gststreams.c \
          gstreamer/gst/gsttaglist.c \
          gstreamer/gst/gsttagsetter.c \
          gstreamer/gst/gsttask.c \
          gstreamer/gst/gsttaskpool.c \
          gstreamer/gst/gsttoc.c \
          gstreamer/gst/gsttocsetter.c \
          gstreamer/gst/gsttracer.c \
          gstreamer/gst/gsttracerfactory.c \
          gstreamer/gst/gsttracerrecord.c \
          gstreamer/gst/gsttracerutils.c \
          gstreamer/gst/gsttypefind.c \
          gstreamer/gst/gsttypefindfactory.c \
          gstreamer/gst/gsturi.c \
          gstreamer/gst/gstutils.c \
          gstreamer/gst/gstvalue.c \
          gstreamer/libs/gst/base/gstadapter.c \
          gstreamer/libs/gst/base/gstbaseparse.c \
          gstreamer/libs/gst/base/gstbasesink.c \
          gstreamer/libs/gst/base/gstbasesrc.c \
          gstreamer/libs/gst/base/gstbasetransform.c \
          gstreamer/libs/gst/base/gstbitreader.c \
          gstreamer/libs/gst/base/gstbytereader.c \
          gstreamer/libs/gst/base/gstbytewriter.c \
          gstreamer/libs/gst/base/gstcollectpads.c \
          gstreamer/libs/gst/base/gstdataqueue.c \
          gstreamer/libs/gst/base/gstflowcombiner.c \
          gstreamer/libs/gst/base/gstpushsrc.c \
          gstreamer/libs/gst/base/gstqueuearray.c \
          gstreamer/libs/gst/base/gsttypefindhelper.c \
          gst-plugins-base/gst-libs/gst/app/gstapp-marshal.c \
          gst-plugins-base/gst-libs/gst/app/gstappsink.c \
          gst-plugins-base/gst-libs/gst/audio/audio.c \
          gst-plugins-base/gst-libs/gst/audio/audio-channels.c \
          gst-plugins-base/gst-libs/gst/audio/audio-channel-mixer.c \
          gst-plugins-base/gst-libs/gst/audio/audio-buffer.c \
          gst-plugins-base/gst-libs/gst/audio/audio-converter.c \
          gst-plugins-base/gst-libs/gst/audio/audio-resampler.c \
          gst-plugins-base/gst-libs/gst/audio/audio-enumtypes.c \
          gst-plugins-base/gst-libs/gst/audio/audio-quantize.c \
          gst-plugins-base/gst-libs/gst/audio/audio-format.c \
          gst-plugins-base/gst-libs/gst/audio/audio-info.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiobasesink.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiobasesrc.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudioclock.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiodecoder.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudioencoder.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiofilter.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudioiec61937.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiometa.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiopack-dist.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudioringbuffer.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiosink.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudiosrc.c \
          gst-plugins-base/gst-libs/gst/audio/gstaudioutilsprivate.c \
          gst-plugins-base/gst-libs/gst/audio/streamvolume.c \
          gst-plugins-base/gst-libs/gst/fft/gstfft.c \
          gst-plugins-base/gst-libs/gst/fft/gstfftf32.c \
          gst-plugins-base/gst-libs/gst/fft/kiss_fft_f32.c \
          gst-plugins-base/gst-libs/gst/fft/kiss_fftr_f32.c \
          gst-plugins-base/gst-libs/gst/pbutils/codec-utils.c \
          gst-plugins-base/gst-libs/gst/pbutils/descriptions.c \
          gst-plugins-base/gst-libs/gst/pbutils/gstdiscoverer.c \
          gst-plugins-base/gst-libs/gst/pbutils/gstdiscoverer-types.c \
          gst-plugins-base/gst-libs/gst/pbutils/gstpluginsbaseversion.c \
          gst-plugins-base/gst-libs/gst/pbutils/missing-plugins.c \
          gst-plugins-base/gst-libs/gst/pbutils/pbutils.c \
          gst-plugins-base/gst-libs/gst/pbutils/pbutils-enumtypes.c \
          gst-plugins-base/gst-libs/gst/riff/riff-media.c \
          gst-plugins-base/gst-libs/gst/riff/riff-read.c \
          gst-plugins-base/gst-libs/gst/riff/riff.c \
          gst-plugins-base/gst-libs/gst/tag/gstid3tag.c \
          gst-plugins-base/gst-libs/gst/tag/gsttagdemux.c \
          gst-plugins-base/gst-libs/gst/tag/id3v2.c \
          gst-plugins-base/gst-libs/gst/tag/id3v2frames.c \
          gst-plugins-base/gst-libs/gst/tag/lang.c \
          gst-plugins-base/gst-libs/gst/tag/tags.c \
          gst-plugins-base/gst-libs/gst/tag/tag-enumtypes.c \
          gst-plugins-base/gst-libs/gst/video/video.c \
          gst-plugins-base/gst-libs/gst/video/video-chroma.c \
          gst-plugins-base/gst-libs/gst/video/video-color.c \
          gst-plugins-base/gst-libs/gst/video/video-converter.c \
          gst-plugins-base/gst-libs/gst/video/video-format.c \
          gst-plugins-base/gst-libs/gst/video/video-enumtypes.c \
          gst-plugins-base/gst-libs/gst/video/video-info.c \
          gst-plugins-base/gst-libs/gst/video/video-multiview.c \
          gst-plugins-base/gst-libs/gst/video/video-orc-dist.c \
          gst-plugins-base/gst-libs/gst/video/video-tile.c \
          gst-plugins-good/gst/audiofx/audiofx.c \
          gst-plugins-good/gst/audiofx/audiopanorama.c \
          gst-plugins-good/gst/audiofx/audiopanoramaorc.c \
          gst-plugins-base/gst/audioconvert/plugin.c \
          gst-plugins-base/gst/audioconvert/gstaudioconvert.c \
          gst-plugins-bad/gst/aiff/aiff.c \
          gst-plugins-bad/gst/aiff/aiffparse.c \
          gst-plugins-base/gst/app/gstapp.c \
          gst-plugins-base/gst/typefind/gsttypefindfunctions.c \
          gst-plugins-good/gst/audioparsers/gstmpegaudioparse.c \
          gst-plugins-good/gst/audioparsers/parsersplugin.c \
          gst-plugins-good/gst/equalizer/gstiirequalizer.c \
          gst-plugins-good/gst/equalizer/gstiirequalizernbands.c \
          gst-plugins-good/gst/isomp4/isomp4-plugin.c \
          gst-plugins-good/gst/isomp4/qtdemux.c \
          gst-plugins-good/gst/isomp4/gstisoff.c \
          gst-plugins-good/gst/isomp4/qtdemux_dump.c \
          gst-plugins-good/gst/isomp4/qtdemux_lang.c \
          gst-plugins-good/gst/isomp4/qtdemux_tags.c \
          gst-plugins-good/gst/isomp4/qtdemux_tree.c \
          gst-plugins-good/gst/isomp4/qtdemux_types.c \
          gst-plugins-good/gst/spectrum/gstspectrum.c \
          gst-plugins-good/gst/wavparse/gstwavparse.c \
          gstreamer/plugins/elements/gstelements.c \
          gstreamer/plugins/elements/gstqueue.c \
          gstreamer/plugins/elements/gsttypefindelement.c \
          gst-plugins-base/gst/volume/gstvolume.c \
          gst-plugins-base/gst/volume/gstvolumeorc-dist.c \
          gst-plugins-base/ext/alsa/gstalsaplugin.c \
          gst-plugins-base/ext/alsa/gstalsa.c \
          gst-plugins-base/ext/alsa/gstalsadeviceprobe.c \
          gst-plugins-base/ext/alsa/gstalsasink.c \
          projects/plugins/gstplugins-lite.c

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
