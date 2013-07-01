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

JDK_FRAMEWORK = /System/Library/Frameworks/JavaVM.framework

FILES_export = \
	com/sun/glass/events/DndEvent.java \
	com/sun/glass/events/KeyEvent.java \
	com/sun/glass/events/MouseEvent.java \
	com/sun/glass/events/TouchEvent.java \
	com/sun/glass/events/ViewEvent.java \
	com/sun/glass/events/WindowEvent.java \
	com/sun/glass/events/SwipeGesture.java \
	com/sun/glass/events/mac/NpapiEvent.java \
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
	com/sun/glass/ui/mac/MacApplication.java \
	com/sun/glass/ui/mac/MacClipboardDelegate.java \
	com/sun/glass/ui/mac/MacCommonDialogs.java \
	com/sun/glass/ui/mac/MacCursor.java \
	com/sun/glass/ui/mac/MacDnDClipboard.java \
	com/sun/glass/ui/mac/MacMenuBarDelegate.java \
	com/sun/glass/ui/mac/MacMenuDelegate.java \
	com/sun/glass/ui/mac/MacPasteboard.java \
	com/sun/glass/ui/mac/MacPixels.java \
	com/sun/glass/ui/mac/MacPlatformFactory.java \
	com/sun/glass/ui/mac/MacRobot.java \
	com/sun/glass/ui/mac/MacSystemClipboard.java \
	com/sun/glass/ui/mac/MacTimer.java \
	com/sun/glass/ui/mac/MacView.java \
	com/sun/glass/ui/mac/MacWindow.java \
	com/sun/glass/ui/mac/MacGestureSupport.java \
	com/sun/glass/ui/accessible/mac/MacAccessibleRoot.java \
	com/sun/glass/ui/accessible/mac/MacAccessibleBaseProvider.java \
	com/sun/glass/ui/accessible/mac/MacAccessibleToggleProvider.java \
	com/sun/glass/ui/accessible/mac/MacAccessibleSelectionProvider.java \
	com/sun/glass/ui/accessible/mac/MacAccessibleSelectionItemProvider.java

JAVAH = $(JDK_HOME)/bin/javah

COMMON_PARAMS = -mmacosx-version-min=10.7 -isysroot $(MACOSX_SDK_PATH) -arch i386 -arch x86_64 -F$(JDK_FRAMEWORK)/Frameworks
CC_PARAMS = \
	-std=c99 -Fd$(BUILD_DIR) -c \
	-I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -I$(BUILD_DIR) -I$(SRC_DIR) \
	$(COMMON_PARAMS)
LINK = g++
LINK_PARAMS = \
	-framework AppKit \
	-framework ApplicationServices \
	-framework JavaVM \
	-framework JavaRuntimeSupport \
	-framework OpenGL \
	-framework QuartzCore \
	-dynamiclib -o $(OUT_LIB) -lobjc \
	$(COMMON_PARAMS)
ifneq ($(CONF), Debug)
	CC_PARAMS += -O3 -DNDEBUG -Werror
else
	CC_PARAMS += -DDEBUG
	LINK_PARAMS += -g
endif

JAVAH_CLASSES = $(subst /,.,$(FILES_export:%.java=%))
JAVAH_CLASS_FILES = $(patsubst %.java, %.class, $(addprefix $(CLASS_DIR)/, $(FILES_export)))

OBJS = $(patsubst $(SRC_DIR)/%.m, $(BUILD_DIR)/%.obj, $(wildcard $(SRC_DIR)/*.m))

build : .build-pre $(BUILD_DIR)/.jni_includes $(OUT_LIB) .build-post

$(BUILD_DIR)/.jni_includes: $(JAVAH_CLASS_FILES)
	$(JAVAH) -force -d $(BUILD_DIR) -jni -classpath "$(CLASS_DIR)" $(subst .class,,$(subst /,.,$(subst $(CLASS_DIR)/,, $?)))
	touch $(BUILD_DIR)/.jni_includes

$(OUT_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.obj : $(SRC_DIR)/%.m
	$(CC) $(CC_PARAMS) -o $@ $<

.build-pre :
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

.build-post :

all : build

clean :
	rm -rf $(BUILD_DIR) $(DIST_DIR)
