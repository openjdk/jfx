#
#  There exist several targets which are by default empty and which can be
#  used for execution of your targets. These targets are usually executed
#  before and after some main targets. They are:
#
#     .build-pre:              called before 'build' target
#     .build-post:             called after 'build' target
#     .clean-pre:              called before 'clean' target
#     .clean-post:             called after 'clean' target
#     .clobber-pre:            called before 'clobber' target
#     .clobber-post:           called after 'clobber' target
#     .all-pre:                called before 'all' target
#     .all-post:               called after 'all' target
#     .help-pre:                called before 'help' target
#     .help-post:               called after 'help' target
#
#  Targets beginning with '.' are not intended to be called on their own.
#
#  Main targets can be executed directly, and they are:
#
#     build                    build a specific configuration
#     clean                    remove built files from a configuration
#     clobber                  remove all built files
#     all                      build all configurations
#     help                     print help mesage
#
#  Targets .build-impl, .clean-impl, .clobber-impl, .all-impl, and
#  .help-impl are implemented in nbproject/makefile-impl.mk.
#
# NOCDDL


# Environment
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
RANLIB=ranlib

# NOTE: the following variables are passed in from
# the javafx-iio/build-*.xml files
ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif
ifndef JFXIIO_CP
	JFXIIO_CP	= ../javafx-iio/build/classes
endif
ifndef CONF
	CONF		= Release
endif

JFXIIO_SRC	= src
JFXIIO_JPEG_SRC	= src/libjpeg7
JFXIIO_MACOSX_SRC = src/macosx
JFXIIO_IOS_SRC= src/ios
JFXIIO_IOS_PKG = com.sun.javafx.iio.ios
JFXIIO_PKG	= com.sun.javafx.iio.jpeg
JFXIIO_PKG_PATH = $(subst .,/,$(JFXIIO_PKG))
JFXIIO_IOS_PKG_PATH = $(subst .,/,$(JFXIIO_IOS_PKG))
JFXIIO_JNI_PKG = $(subst .,_,$(JFXIIO_PKG))
JFXIIO_IOS_JNI_PKG = $(subst .,_,$(JFXIIO_IOS_PKG))
JNI_INCLUDES	:= $(patsubst $(JFXIIO_CP)/$(JFXIIO_PKG_PATH)/%.class,$(BUILD_DIR)/$(JFXIIO_JNI_PKG)_%.h,$(wildcard $(JFXIIO_CP)/$(JFXIIO_PKG_PATH)/*.class))
INCLUDES	= \
	$(JFXIIO_JPEG_SRC)/*.h

SYSTEM_UNAME := $(shell uname)

ifneq (,$(findstring CYGWIN,$(SYSTEM_UNAME)))
	fix_path = $(shell cygpath $1 $2)

	ifndef JDK_HOME
		JDK_HOME = c:/jdk1.6.0
	endif

	OBJ_SUFFIX  = obj
	OUTPUT_FLAG = -Fo
	JFXIIO_LIB	= $(DIST_DIR)/javafx-iio.dll
	JFXIIO_MAP	= $(DIST_DIR)/javafx-iio.map
	JFXIIO_PDB	= $(DIST_DIR)/javafx-iio.pdb

	LINK		=  link.exe
	CC		=  cl.exe

	LINK_PARAMS	= -dll -out:$(JFXIIO_LIB) -pdb:$(JFXIIO_PDB) -map:$(JFXIIO_MAP) -nologo /manifest /opt:REF /incremental:no
	CC_PARAMS	+= -nologo /D_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -W3 -EHsc -DUNICODE -D_UNICODE -DLOGGING -DWIN32 -DIAL -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I$(JFXIIO_SRC) -I$(JFXIIO_JPEG_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
	ifeq ($(CONF), Release)
		CC_PARAMS += -Ox -MD
	else
		LINK_PARAMS += /debug
		CC_PARAMS += -Zi -Od -DDEBUG -MDd
	endif
endif

ifeq ($(SYSTEM_UNAME), SunOS)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /java/re/jdk/1.6.0_10/latest/binaries/solaris-i586
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	JFXIIO_LIB	= $(DIST_DIR)/libjavafx-iio.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
        ifeq ($(CC), parfait-gcc)
		LINK = parfait-gcc
	else
		CC = gcc
		LINK = gcc
	endif
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(JFXIIO_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(JFXIIO_SRC) -I$(JFXIIO_JPEG_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -c
endif

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /home/java2d/jdk/jdk1.6.0_10
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	JFXIIO_LIB	= $(DIST_DIR)/libjavafx-iio.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Wl,--no-as-needed
	LINK		= $(CC)
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(JFXIIO_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(JFXIIO_SRC) -I$(JFXIIO_JPEG_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c

endif

ifeq ($(SYSTEM_UNAME), Darwin)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = $(shell /usr/libexec/java_home)
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required

	ifeq ($(IS_IOS),true)
		IOS_SDK_BASE := $(shell xcode-select -print-path)
		IOS_DEV = $(IOS_SDK_BASE)/Platforms/$(IOS_PLATFORM).platform/Developer
		IOS_SDK = $(IOS_DEV)/SDKs/$(IOS_PLATFORM)$(IOS_VERSION).sdk

		CC = $(IOS_DEV)/usr/bin/gcc
		LINK = libtool
		JFXIIO_LIB	= $(DIST_DIR)/libjavafx-iio-$(IOS_ARCH).a
		
		ifeq ($(CONF),Debug)
			COMMON_PARAMS = -gdwarf-2
		endif
		LINK_PARAMS = -static -arch_only $(IOS_ARCH) -syslibroot $(IOS_SDK) $(COMMON_PARAMS) -framework Foundation -framework ImageIO -framework UIKit -L$(IOS_SDK)/usr/lib  -o $(JFXIIO_LIB)
		CC_PARAMS   = -arch $(IOS_ARCH) -isysroot $(IOS_SDK) -miphoneos-version-min=5.0
		CC_PARAMS  += -fobjc-abi-version=2 -fobjc-legacy-dispatch -fno-common -Wall -fno-strict-aliasing -fwrapv -fpascal-strings -O2 -ffast-math
		CC_PARAMS  += -Fd$(BUILD_DIR) $(COMMON_PARAMS)  -I$(JFXIIO_SRC) -I$(JFXIIO_IOS_SRC) -I$(JFXIIO_JPEG_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
		
		OBJS += $(patsubst $(JFXIIO_IOS_SRC)/%.m,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(JFXIIO_IOS_SRC)/*.m))
		OBJS += $(patsubst $(JFXIIO_IOS_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(JFXIIO_IOS_SRC)/*.c))
		
		JNI_INCLUDES	+= $(patsubst $(JFXIIO_CP)/$(JFXIIO_IOS_PKG_PATH)/%.class,$(BUILD_DIR)/$(JFXIIO_IOS_JNI_PKG)_%.h,$(wildcard $(JFXIIO_CP)/$(JFXIIO_IOS_PKG_PATH)/*.class))
	else
		JFXIIO_LIB	= $(DIST_DIR)/libjavafx-iio.dylib

        	ifeq ($(CC), parfait-gcc)
			COMMON_PARAMS = -mmacosx-version-min=10.4
		else
			COMMON_PARAMS = -mmacosx-version-min=10.4 -arch i386 -arch x86_64 #-arch ppc
		endif
		LINK		= $(CC)
		LINK_PARAMS	= -framework ApplicationServices -dynamiclib $(COMMON_PARAMS) -o $(JFXIIO_LIB)
		CC_PARAMS	= -O2 -ffast-math -F/System/Library/Frameworks/ApplicationServices.framework/Frameworks -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(JFXIIO_SRC) -I$(JFXIIO_JPEG_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	endif
endif


# The following should automatically include all .c files in the src/libjpeg7 folder.
OBJS		+= $(patsubst $(JFXIIO_JPEG_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(JFXIIO_JPEG_SRC)/*.c))
# Can manually add more files here if needed.
OBJS		+= \
	$(BUILD_DIR)/jpegloader.$(OBJ_SUFFIX)

JAVAH  = $(call fix_path,-u,$(JDK_HOME)/bin/javah)

# build
build: .build-pre $(JFXIIO_LIB) .build-post

# generate parfait report, make must be launched with
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-g++
parfait: build
	parfait $(JFXIIO_LIB).bc -g $(DIST_DIR)/parfait


$(JFXIIO_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(JFXIIO_JPEG_SRC)/%.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/jpegloader.$(OBJ_SUFFIX) : $(JFXIIO_SRC)/jpegloader.c  $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/$(JFXIIO_JNI_PKG)_%.h : $(JFXIIO_CP)/$(JFXIIO_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(JFXIIO_CP) $(JFXIIO_PKG).$*

ifeq ($(SYSTEM_UNAME), Darwin)
ifeq ($(IS_IOS),true)

$(BUILD_DIR)/$(JFXIIO_IOS_JNI_PKG)_%.h : $(JFXIIO_CP)/$(JFXIIO_IOS_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(JFXIIO_CP) $(JFXIIO_IOS_PKG).$*

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(JFXIIO_IOS_SRC)/%.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(JFXIIO_IOS_SRC)/%.m $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

endif
endif

sanity:
	@if [ ! -d "$(JDK_HOME)" ]; then \
		echo "ERROR: path to Java 6 SDK: $(JDK_HOME) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_BOOTDIR variable."; \
		echo "Sanity check failed."; \
		exit 1; \
	fi;

.build-pre: sanity
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

.build-post:
# Add your post 'build' code here...


# clean
#clean: .clean-pre .clean-impl .clean-post
clean: .clean-pre .clean-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clean-pre:
# Add your pre 'clean' code here...

.clean-post:
# Add your post 'clean' code here...


# clobber
#clobber: .clobber-pre .clobber-impl .clobber-post
clobber: .clobber-pre .clobber-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clobber-pre:

.clobber-post:
# Add your post 'clobber' code here...


# all
#all: .all-pre .all-impl .all-post
all: .all-pre build .all-post

.all-pre:
# Add your pre 'all' code here...

.all-post:
# Add your post 'all' code here...


# help
help: .help-pre .help-impl .help-post

.help-pre:
# Add your pre 'help' code here...

.help-post:
# Add your post 'help' code here...

.PRECIOUS: $(JNI_INCLUDES)

# include project implementation makefile
include nbproject/Makefile-impl.mk
