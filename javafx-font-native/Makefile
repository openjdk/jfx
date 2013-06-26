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
# the javafx-font/build-*.xml files
ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif
ifndef JFXFONT_CP
	JFXFONT_CP	= ../javafx-font/build/classes
endif
ifndef JFXFONT_JAVA
	JFXFONT_JAVA = ../javafx-font/src
endif
ifndef JAVAH_CP
    JAVAH_CP = "../javafx-font/build/classes:../../rt/javafx-geom/build/classes:../../rt/javafx-ui-common/build/classes"
endif

ifndef CONF
	CONF		= Release
endif

ifeq ($(CONF), Release)
# -DNDEBUG is important to disable assert, widely used in t2k.
		CC_PARAMS +=  -DNDEBUG
endif

JFXFONT_SRC	= src
INCLUDES = $(wildcard $(JFXFONT_SRC)/*.h)

ifndef JNI_FILES
	JNI_FILES = \
		com/sun/javafx/font/PrismFontFactory.java \
		com/sun/javafx/font/FontConfigManager.java \
		com/sun/javafx/font/DFontDecoder.java \
		com/sun/javafx/font/MacFontFinder.java \
		com/sun/javafx/font/coretext/OS.java \
		com/sun/javafx/font/directwrite/OS.java 
endif 

JNI_INCLUDES = $(addprefix $(BUILD_DIR)/,$(subst /,_,$(subst .java,.h, $(JNI_FILES))))

SYSTEM_UNAME := $(shell uname)
JAVAH_FIX_CP = $(JAVAH_CP)

ifneq (,$(findstring CYGWIN,$(SYSTEM_UNAME)))
	fix_path = $(shell cygpath $1 $2)

	ifndef JDK_HOME
		JDK_HOME = c:/jdk1.6.0
	endif
	OBJ_SUFFIX  = obj
	OUTPUT_FLAG = -Fo
	JFXFONT_LIB	= $(DIST_DIR)/javafx-font.dll
	JFXFONT_MAP	= $(DIST_DIR)/javafx-font.map
	JFXFONT_PDB	= $(DIST_DIR)/javafx-font.pdb

	JAVAH_FIX_CP = $(call fix_path,-mp,$(JAVAH_CP))

	LINK		=  link.exe
	CC		=  cl.exe

# Need to link against some windows platform libs for font lookup.
# advapi32.lib - for Windows Registry functions
# user32.lib - for GetDC
# gdi32.lib - for EnumFontFamilies

	LINK_PARAMS	= -dll -out:$(JFXFONT_LIB) -pdb:$(JFXFONT_PDB) -map:$(JFXFONT_MAP) -nologo /manifest /opt:REF /incremental:no advapi32.lib gdi32.lib user32.lib dwrite.lib d2d1.lib windowscodecs.lib ole32.lib
	CC_PARAMS	+= -nologo /D_STATIC_CPPLIB /D_DISABLE_DEPRECATE_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -W3 -EHsc -DJFXFONT_PLUS -DLOGGING -DWIN32 -DIAL -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I$(JFXFONT_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
	ifeq ($(CONF), Release)
		CC_PARAMS += -Ox -MD
	else
		LINK_PARAMS += /debug
		CC_PARAMS += -MDd -Zi -Od -DDEBUG
	endif
endif

ifeq ($(SYSTEM_UNAME), SunOS)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /java/re/jdk/1.6.0_10/latest/binaries/solaris-i586
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	JFXFONT_LIB	= $(DIST_DIR)/libjavafx-font.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
    ifeq ($(CC), parfait-gcc)
		LINK = parfait-gcc
	else
		CC = gcc
		LINK = gcc
	endif
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(JFXFONT_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -DJFXFONT_PLUS -I$(JFXFONT_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -c
endif

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /home/java2d/jdk/jdk1.6.0_10
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	JFXFONT_LIB	= $(DIST_DIR)/libjavafx-font.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Wl,--no-as-needed
# Link using g++ because of C++ usage. This gets us lstdc++ in the
# dependencies. Alternatively link it statically using gcc by adding the
# following variable definition at the end of the gcc link command line.
# LINK_EXTRA_PARAMS = -Wl,-Bstatic -lstdc++ -Wl,-Bdynamic
# The advantage of g++ dynamic linking is smaller .so. But JDK has used
# static linking with gcc because the ABI was not stable. Hopefully that's
# changed in the Linux distros we'd support.
	ifeq ($(CC), parfait-gcc)
		LINK = parfait-g++
	else
		LINK = g++
	endif
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(JFXFONT_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -DJFXFONT_PLUS -I$(JFXFONT_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c

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
	
		JFXFONT_LIB	= $(DIST_DIR)/libjavafx-font-$(IOS_ARCH).a

		ifeq ($(CONF),Debug)
			COMMON_PARAMS = -gdwarf-2
		endif
		LINK_PARAMS = -static -arch_only $(IOS_ARCH) -syslibroot $(IOS_SDK) $(COMMON_PARAMS) -framework Foundation -framework CoreText -L$(IOS_SDK)/usr/lib -o $(JFXFONT_LIB)
		CC_PARAMS   = -arch $(IOS_ARCH) -isysroot $(IOS_SDK) -miphoneos-version-min=5.0
		CC_PARAMS  += -fno-common -Wall -fno-strict-aliasing -fwrapv  -fpascal-strings -O2 -ffast-math
		CC_PARAMS  += -Fd$(BUILD_DIR) $(COMMON_PARAMS) -DJFXFONT_PLUS -I$(JFXFONT_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	else
		JFXFONT_LIB	= $(DIST_DIR)/libjavafx-font.dylib

		ifeq ($(CC), parfait-gcc)
			COMMON_PARAMS = -mmacosx-version-min=10.6
			LINK = parfait-g++
		else
			COMMON_PARAMS = -mmacosx-version-min=10.6 -arch i386 -arch x86_64 #-arch ppc
			LINK = g++
		endif
		LINK_PARAMS = -framework ApplicationServices -dynamiclib $(COMMON_PARAMS) -o $(JFXFONT_LIB)
		CC_PARAMS = -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -DJFXFONT_PLUS -I$(JFXFONT_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	endif
endif


OBJS += $(patsubst $(JFXFONT_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(JFXFONT_SRC)/*.c))
OBJS += $(patsubst $(JFXFONT_SRC)/%.cpp,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(JFXFONT_SRC)/*.cpp))

JAVAH	= $(call fix_path,-u,$(JDK_HOME)/bin/javah)

# build
build: .build-pre $(JFXFONT_LIB) .build-post

# generate parfait report, make must be launched with
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-g++
parfait: build
	parfait $(JFXFONT_LIB).bc -g $(DIST_DIR)/parfait

$(JFXFONT_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

VPATH = $(JFXFONT_SRC)

$(BUILD_DIR)/%.$(OBJ_SUFFIX): %.cpp $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX): %.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

.SECONDEXPANSION:
$(BUILD_DIR)/%.h: $(JFXFONT_CP)/$$(subst _,/,%).class
	$(JAVAH) -force -o $@ -jni -classpath "$(JAVAH_FIX_CP)" $(subst _,.,$*)

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
