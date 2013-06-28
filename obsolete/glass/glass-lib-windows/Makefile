
# Environment
MKDIR		= mkdir
CP		= cp
MAKEDEPEND      = makedepend
RM		= rm

# The TMPINCLUDE_DIR will contain fake system headers to prevent makedepend from generating useless warnings.
# We intentionally don't want to point makedepend to the real system headers to speed up the
# dependencies generation process.
TMPINCLUDE_DIR = $(BUILD_DIR)/tmpinclude

SRC_DIR		= src
OUT_LIB		= $(DIST_DIR)/glass.dll
OUT_MAP		= $(DIST_DIR)/glass.map
OUT_PDB		= $(DIST_DIR)/glass.pdb

FILES_export = \
			com/sun/glass/ui/Screen.java \
			com/sun/glass/ui/Cursor.java \
			com/sun/glass/ui/Window.java \
			com/sun/glass/ui/View.java \
			com/sun/glass/ui/Robot.java \
			com/sun/glass/ui/CommonDialogs.java \
			com/sun/glass/ui/Pixels.java \
			com/sun/glass/ui/win/WinApplication.java \
			com/sun/glass/ui/win/WinDnDClipboard.java \
			com/sun/glass/ui/win/WinSystemClipboard.java \
			com/sun/glass/ui/win/WinMenuBarDelegate.java \
			com/sun/glass/ui/win/WinMenuDelegate.java \
			com/sun/glass/ui/win/WinMenuImpl.java \
			com/sun/glass/ui/win/WinCursor.java \
			com/sun/glass/ui/win/WinWindow.java \
			com/sun/glass/ui/win/WinView.java \
			com/sun/glass/ui/win/WinRobot.java \
			com/sun/glass/ui/win/WinPixels.java \
			com/sun/glass/ui/win/WinTimer.java \
                        com/sun/glass/ui/win/WinCommonDialogs.java \
			com/sun/glass/ui/accessible/win/WinAccessibleRoot.java \
			com/sun/glass/ui/accessible/win/WinAccessibleBaseProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleToggleProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleSelectionItemProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleSelectionProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleRangeValueProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleGridProvider.java \
			com/sun/glass/ui/accessible/win/WinAccessibleGridItemProvider.java \
			com/sun/glass/events/WindowEvent.java \
			com/sun/glass/events/ViewEvent.java \
			com/sun/glass/events/KeyEvent.java \
			com/sun/glass/events/MouseEvent.java \
			com/sun/glass/events/DndEvent.java \
			com/sun/glass/events/TouchEvent.java

FILES_cpp	= \
			Utils.cpp \
			BaseWnd.cpp \
			GlassApplication.cpp \
			GlassWindow.cpp \
			GlassView.cpp \
			GlassMenu.cpp \
			GlassScreen.cpp \
			GlassCursor.cpp \
			GlassClipboard.cpp \
			GlassDnD.cpp \
			GlassInputTextInfo.cpp \
			KeyTable.cpp \
			FullScreenWindow.cpp \
			ManipulationEvents.cpp \
			ViewContainer.cpp \
			Robot.cpp \
			Pixels.cpp \
                        CommonDialogs_COM.cpp \
                        CommonDialogs_Standard.cpp \
			CommonDialogs.cpp \
			Timer.cpp \
			AccessibleRoot.cpp \
			AccessibleBaseProvider.cpp \
			AccessibleBasePatternProvider.cpp \
			AccessibleSelectionItemProvider.cpp \
			AccessibleToggleProvider.cpp \
			AccessibleRangeValueProvider.cpp \
			AccessibleGridProvider.cpp \
			AccessibleGridItemProvider.cpp \
			AccessibleSelectionProvider.cpp

FILES_src_cpp = $(addprefix $(SRC_DIR)/, $(FILES_cpp))

# These are used to please makedepend
FILES_fake_system_headers = \
			jni.h \
			windows.h \
			assert.h \
			windowsx.h \
			ole2.h \
			stdio.h \
			tchar.h \
			comdef.h \
			comutil.h \
			string.h \
			malloc.h \
			shlobj.h \
			hash_map \
			hash_set \
			wchar.h \
			commdlg.h \
			imm.h \
			new \
			memory \
			vector \
			mmsystem.h \
			shlobj.h \
			shobjidl.h \
			Manipulations.h \
			Manipulations_i.c \
			Tpcshrd.h \
			UIAutomation.h \
			Dwmapi.h
JAVAH	= $(shell cygpath -u $(JDK_HOME)/bin/javah)
LINK	= link.exe
CC	= cl.exe
RC  = rc.exe

LINK_PARAMS	= -dll -out:$(OUT_LIB) -pdb:$(OUT_PDB) -map:$(OUT_MAP) -nologo /opt:REF /incremental:no /manifest delayimp.lib gdi32.lib urlmon.lib Comdlg32.lib winmm.lib imm32.lib shell32.lib Uiautomationcore.lib dwmapi.lib /DELAYLOAD:user32.dll /DELAYLOAD:urlmon.dll /DELAYLOAD:winmm.dll /DELAYLOAD:shell32.dll /DELAYLOAD:Uiautomationcore.dll /DELAYLOAD:dwmapi.dll
CC_PARAMS	+= -nologo -W3 -WX /D_STATIC_CPPLIB /D_DISABLE_DEPRECATE_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -EHsc -DUNICODE -D_UNICODE -DWIN32 -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I. -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
ifeq ($(CONF), Release)
	CC_PARAMS += -Zi -O2 -MD
else
	LINK_PARAMS += /debug
	CC_PARAMS += -Zi -Od -DDEBUG -MDd
endif

FILE_pch_h = common.h
FILE_pch = $(BUILD_DIR)/common.pch
FILE_pch_cpp = common.cpp
FILE_pch_o = $(BUILD_DIR)/common.obj
FILES_pch_headers = $(SRC_DIR)/Utils.h $(SRC_DIR)/OleUtils.h

FILES_o  = $(patsubst %.cpp, %.obj, $(addprefix $(BUILD_DIR)/, $(notdir $(FILES_cpp))))

JAVAH_CLASSES = $(subst /,.,$(FILES_export:%.java=%))
JAVAH_CLASS_FILES = $(patsubst %.java, %.class, $(addprefix $(CLASS_DIR)/, $(FILES_export)))

$(BUILD_DIR)/.jni_includes: $(JAVAH_CLASS_FILES)
		$(JAVAH) -force -d $(BUILD_DIR) -jni -classpath "$(CLASS_DIR)" $(subst .class,,$(subst /,.,$(subst $(CLASS_DIR)/,, $?)))
		touch $(BUILD_DIR)/.jni_includes

# We use a .tag* target to prevent make from building the Depend.mak target when it encounters the
# -include $(BUILD_DIR)/Depend.mak line just a few lines below.
# sed calls:
#    1. Replace the directory of the .obj files to BUILD_DIR
#    2. Remove fake dependencies
#    3. Remove empty rules that appear after the second sed call
$(BUILD_DIR)/.tag_Depend.mak: $(BUILD_DIR)/.jni_includes $(FILES_src_cpp) $(wildcard $(SRC_DIR)/*.h)
		mkdir -p $(TMPINCLUDE_DIR)
		touch $(addprefix $(TMPINCLUDE_DIR)/, $(FILES_fake_system_headers))
		rm -rf $(BUILD_DIR)/Depend.mak
		cp Depend.mak $(BUILD_DIR)
		$(MAKEDEPEND) -Y -I${BUILD_DIR} -I. -I$(TMPINCLUDE_DIR) -f$(BUILD_DIR)/Depend.mak -s"# makedepend marker line" -o.obj ${FILES_src_cpp}
		sed -i 's@$(SRC_DIR)@$(BUILD_DIR)@' $(BUILD_DIR)/Depend.mak
		sed -i 's@ $(TMPINCLUDE_DIR)[^ ]*@@g' $(BUILD_DIR)/Depend.mak
		sed -i '/:[ ]*$$/d' $(BUILD_DIR)/Depend.mak
		rm -rf $(TMPINCLUDE_DIR)
		touch $(BUILD_DIR)/.tag_Depend.mak

-include $(BUILD_DIR)/Depend.mak

$(OUT_LIB) : $(FILE_pch_o) $(FILES_o) $(BUILD_DIR)/GlassResources.res
	$(LINK) $(LINK_PARAMS) $^

$(FILE_pch_o) : $(SRC_DIR)/$(FILE_pch_cpp) $(FILES_pch_headers)
	$(CC) $(CC_PARAMS) /Yc$(FILE_pch_h) -Fp$(FILE_pch) -Fo$@ $<

$(BUILD_DIR)/%.obj : $(SRC_DIR)/%.cpp $(JNI_INCLUDES)
	$(CC) $(CC_PARAMS) /Yu$(FILE_pch_h) -Fp$(FILE_pch) -Fo$@ $<

$(BUILD_DIR)/%.obj : $(SRC_DIR)/%.c $(JNI_INCLUDES)
	$(CC) $(CC_PARAMS) /Yu$(FILE_pch_h) -Fp$(FILE_pch) -Fo$@ $<

$(BUILD_DIR)/GlassResources.res : $(SRC_DIR)/GlassResources.rc $(SRC_DIR)/*.cur
	$(RC) -nologo -Fo$@ $< 

build: sanity init $(BUILD_DIR)/.jni_includes $(BUILD_DIR)/.tag_Depend.mak $(OUT_LIB)

sanity:
	@if [ ! -d $(JDK_HOME) ]; then \
		echo "ERROR: path to Java SDK: $(JDK_HOME) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_BOOTDIR variable."; \
		echo "Sanity check failed."; \
		exit 1; \
	fi;

init:
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

clean:
	@rm -rf $(BUILD_DIR) $(DIST_DIR)

clobber: clean

all: build
