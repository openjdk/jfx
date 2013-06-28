#
#  There exist several targets which are by default empty and which can be
#  used for execution of your targets. These targets are usually executed
#  before and after some main targets. They are:
#
#     .build-pre:              called before 'build' target
#     .build-post:             called after 'build' target
#     .clean-pre:              called before 'clean' target
#     .clean-post:             called after 'clean' target
#
#  Targets beginning with '.' are not intended to be called on their own.
#
#  Main targets can be executed directly, and they are:
#
#     build                    build a specific configuration
#     clean                    remove built files from a configuration
#
# NOCDDL

# Environment
MKDIR           = mkdir
CP              = cp

IOS_SDK_BASE    := $(shell xcode-select -print-path)
IOS_DEV         = $(IOS_SDK_BASE)/Platforms/$(IOS_PLATFORM).platform/Developer
IOS_SDK         = $(IOS_DEV)/SDKs/$(IOS_PLATFORM)$(IOS_VERSION).sdk

CC              = clang
LINK            = libtool

OBJ_SUFFIX  = o
OUTPUT_FLAG = -o # trailing blank required
GLASS_LIB   = $(DIST_DIR)/libglass-$(IOS_ARCH).a

BUILD_ARCH_DIR  = $(BUILD_DIR)/$(IOS_ARCH)

LINK_PARAMS = -static -arch_only $(IOS_ARCH) -syslibroot $(IOS_SDK) $(COMMON_PARAMS) -framework Foundation -framework CoreGraphics -framework UIKit -framework QuartzCore -framework OpenGLES -L$(IOS_SDK)/usr/lib -o $(GLASS_LIB)
CC_PARAMS   = -x objective-c -arch $(IOS_ARCH) -isysroot $(IOS_SDK) -miphoneos-version-min=6.1 -fmessage-length=0 -std=c99
CC_PARAMS  += -fno-common -Wall -fno-strict-aliasing -fwrapv -fpascal-strings -fobjc-abi-version=2 -fobjc-legacy-dispatch
CC_PARAMS  += -Fd$(BUILD_ARCH_DIR) $(COMMON_PARAMS) -I$(SRC_DIR) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c

FILES_export = \
	com/sun/glass/events/DndEvent.java \
	com/sun/glass/events/KeyEvent.java \
	com/sun/glass/events/MouseEvent.java \
	com/sun/glass/events/TouchEvent.java \
	com/sun/glass/events/ViewEvent.java \
	com/sun/glass/events/WindowEvent.java \
	com/sun/glass/events/SwipeGesture.java \
	com/sun/glass/ui/Application.java \
	com/sun/glass/ui/Clipboard.java \
	com/sun/glass/ui/CommonDialogs.java \
	com/sun/glass/ui/Cursor.java \
	com/sun/glass/ui/Menu.java \
	com/sun/glass/ui/MenuBar.java \
	com/sun/glass/ui/MenuItem.java \
	com/sun/glass/ui/Pixels.java \
	com/sun/glass/ui/Screen.java \
	com/sun/glass/ui/Timer.java \
	com/sun/glass/ui/View.java \
	com/sun/glass/ui/Window.java \
	com/sun/glass/ui/ios/IosApplication.java \
	com/sun/glass/ui/ios/IosClipboardDelegate.java \
	com/sun/glass/ui/ios/IosCursor.java \
	com/sun/glass/ui/ios/IosGestureSupport.java \
	com/sun/glass/ui/ios/IosMenuBarDelegate.java \
	com/sun/glass/ui/ios/IosMenuDelegate.java \
	com/sun/glass/ui/ios/IosPasteboard.java \
	com/sun/glass/ui/ios/IosPixels.java \
	com/sun/glass/ui/ios/IosPlatformFactory.java \
	com/sun/glass/ui/ios/IosRobot.java \
	com/sun/glass/ui/ios/IosTimer.java \
	com/sun/glass/ui/ios/IosView.java \
	com/sun/glass/ui/ios/IosWindow.java

JAVAH = $(JDK_HOME)/bin/javah

JAVAH_CLASSES = $(subst /,.,$(FILES_export:%.java=%))
JAVAH_CLASS_FILES = $(patsubst %.java, %.class, $(addprefix $(CLASS_DIR)/, $(FILES_export)))

OBJS = $(patsubst $(SRC_DIR)/%.m, $(BUILD_ARCH_DIR)/%.o, $(wildcard $(SRC_DIR)/*.m))

ifneq ($(CONF), Debug)
    CC_PARAMS += -O3 -DNDEBUG
else
    CC_PARAMS += -DDEBUG -gdwarf-2
    LINK_PARAMS += -g
endif

all : build

# build
build : .build-pre $(BUILD_DIR)/.jni_includes $(OUT_LIB) .build-post

$(BUILD_DIR)/.jni_includes: $(JAVAH_CLASS_FILES)
	$(JAVAH) -force -d $(BUILD_DIR) -jni -classpath "$(CLASS_DIR)" $(subst .class,,$(subst /,.,$(subst $(CLASS_DIR)/,, $?)))
	touch $(BUILD_DIR)/.jni_includes

$(OUT_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_ARCH_DIR)/%.o : $(SRC_DIR)/%.m
	$(CC) $(CC_PARAMS) -o $@ $<

.build-pre :
	@mkdir -p $(BUILD_DIR) $(BUILD_ARCH_DIR) $(DIST_DIR)

.build-post :

# clean
clean : .clean-pre .clean-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clean-pre :

.clean-post :

