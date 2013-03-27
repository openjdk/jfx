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
XCODEBUILD      = xcodebuild
LIPO            = lipo

GLASS_XCODE_PROJECT = glass.xcodeproj
GLASS_LIB_NAME      = libglass.a

#ifndef CONF
#	CONF = Release
#endif
#ifndef DIST_DIR
#	DIST_DIR = dist
#endif
#ifndef BUILD_DIR
#	BUILD_DIR = build
#endif
#ifndef CLASS_DIR
#	CLASS_DIR = ../glass-mat/build/classes
#endif
#ifndef SRC_DIR
#	SRC_DIR	= src
#endif
#ifndef OUT_LIB
#	OUT_LIB	= $(DIST_DIR)/libglass.dylib
#endif

#ifnded JDK_HOME
#	JDK_HOME = $(shell /usr/libexec/java_home)
#endif

JDK_FRAMEWORK = /System/Library/Frameworks/JavaVM.framework

PKGSRC_DIR = $(SRC_DIR)/com/sun/glass/ui

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

#OBJS = $(patsubst $(PKGSRC_DIR)/%.m, $(BUILD_DIR)/%.obj, $(wildcard $(PKGSRC_DIR)/*.m))

ifeq ($(CONF), Debug)
	CONFIGURATION = Debug
else
	CONFIGURATION = Release
endif

GLASS_X86_LIB_PATH = $(BUILD_DIR)/$(CONFIGURATION)-iphonesimulator/$(GLASS_LIB_NAME)
GLASS_ARM_LIB_PATH = $(BUILD_DIR)/$(CONFIGURATION)-iphoneos/$(GLASS_LIB_NAME)

all : build

# build
build : .build-pre $(BUILD_DIR)/.jni_includes xcodebuild .build-post

$(BUILD_DIR)/.jni_includes: $(JAVAH_CLASS_FILES)
	$(JAVAH) -force -d $(BUILD_DIR) -jni -classpath "$(CLASS_DIR)" $(subst .class,,$(subst /,.,$(subst $(CLASS_DIR)/,, $?)))
	touch $(BUILD_DIR)/.jni_includes

xcodebuild :
	$(XCODEBUILD) -project $(GLASS_XCODE_PROJECT) -configuration $(CONFIGURATION) -sdk iphonesimulator EXTRA_CFLAGS=$(EXTRA_CFLAGS)
	$(XCODEBUILD) -project $(GLASS_XCODE_PROJECT) -configuration $(CONFIGURATION) -sdk iphoneos EXTRA_CFLAGS=$(EXTRA_CFLAGS)

.build-pre :
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

.build-post :
	$(LIPO) -create -output $(OUT_LIB) $(GLASS_X86_LIB_PATH) $(GLASS_ARM_LIB_PATH)

# clean
clean : .clean-pre .clean-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clean-pre :

.clean-post :

.PHONY: xcodebuild
