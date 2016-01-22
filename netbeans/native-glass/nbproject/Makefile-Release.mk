#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-Linux-x86
CND_DLIB_EXT=so
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/1568819164/GlassApplication.o \
	${OBJECTDIR}/_ext/1568819164/GlassCommonDialogs.o \
	${OBJECTDIR}/_ext/1568819164/GlassCursor.o \
	${OBJECTDIR}/_ext/1568819164/GlassDnDClipboard.o \
	${OBJECTDIR}/_ext/1568819164/GlassPixels.o \
	${OBJECTDIR}/_ext/1568819164/GlassRobot.o \
	${OBJECTDIR}/_ext/1568819164/GlassSystemClipboard.o \
	${OBJECTDIR}/_ext/1568819164/GlassTimer.o \
	${OBJECTDIR}/_ext/1568819164/GlassView.o \
	${OBJECTDIR}/_ext/1568819164/GlassWindow.o \
	${OBJECTDIR}/_ext/1568819164/glass_dnd.o \
	${OBJECTDIR}/_ext/1568819164/glass_evloop.o \
	${OBJECTDIR}/_ext/1568819164/glass_general.o \
	${OBJECTDIR}/_ext/1568819164/glass_gtkcompat.o \
	${OBJECTDIR}/_ext/1568819164/glass_key.o \
	${OBJECTDIR}/_ext/1568819164/glass_window.o \
	${OBJECTDIR}/_ext/1568819164/glass_window_ime.o \
	${OBJECTDIR}/_ext/1568817389/GlassApplication.o \
	${OBJECTDIR}/_ext/1568817389/GlassCursor.o \
	${OBJECTDIR}/_ext/1568817389/GlassDragDelegate.o \
	${OBJECTDIR}/_ext/1568817389/GlassGestureSupport.o \
	${OBJECTDIR}/_ext/1568817389/GlassHelper.o \
	${OBJECTDIR}/_ext/1568817389/GlassKey.o \
	${OBJECTDIR}/_ext/1568817389/GlassPasteboard.o \
	${OBJECTDIR}/_ext/1568817389/GlassRobot.o \
	${OBJECTDIR}/_ext/1568817389/GlassScreen.o \
	${OBJECTDIR}/_ext/1568817389/GlassStatics.o \
	${OBJECTDIR}/_ext/1568817389/GlassTimer.o \
	${OBJECTDIR}/_ext/1568817389/GlassView.o \
	${OBJECTDIR}/_ext/1568817389/GlassViewController.o \
	${OBJECTDIR}/_ext/1568817389/GlassViewDelegate.o \
	${OBJECTDIR}/_ext/1568817389/GlassViewGL.o \
	${OBJECTDIR}/_ext/1568817389/GlassWindow.o \
	${OBJECTDIR}/_ext/1388619080/LensApplication.o \
	${OBJECTDIR}/_ext/1388619080/LensCursor.o \
	${OBJECTDIR}/_ext/1388619080/LensCursorImages.o \
	${OBJECTDIR}/_ext/1388619080/LensInputEvents.o \
	${OBJECTDIR}/_ext/1388619080/LensLogger.o \
	${OBJECTDIR}/_ext/1388619080/LensPixels.o \
	${OBJECTDIR}/_ext/1388619080/LensRobot.o \
	${OBJECTDIR}/_ext/1388619080/LensScreen.o \
	${OBJECTDIR}/_ext/1388619080/LensView.o \
	${OBJECTDIR}/_ext/1388619080/LensWindow.o \
	${OBJECTDIR}/_ext/1049586152/android.o \
	${OBJECTDIR}/_ext/1170728052/fbCursor.o \
	${OBJECTDIR}/_ext/1170728052/fbDispman.o \
	${OBJECTDIR}/_ext/1170728052/wrapped_bcm.o \
	${OBJECTDIR}/_ext/228741247/nullCursor.o \
	${OBJECTDIR}/_ext/1804231149/androidInput.o \
	${OBJECTDIR}/_ext/1804231149/androidLens.o \
	${OBJECTDIR}/_ext/239661476/udevInput.o \
	${OBJECTDIR}/_ext/660835749/x11Input.o \
	${OBJECTDIR}/_ext/125406599/rfb.o \
	${OBJECTDIR}/_ext/774031757/LensWindowManager.o \
	${OBJECTDIR}/_ext/774031757/robot.o \
	${OBJECTDIR}/_ext/1962315986/androidScreen.o \
	${OBJECTDIR}/_ext/1962315986/dfbScreen.o \
	${OBJECTDIR}/_ext/1962315986/fbdevScreen.o \
	${OBJECTDIR}/_ext/1962315986/headlessScreen.o \
	${OBJECTDIR}/_ext/1962315986/x11ContainerScreen.o \
	${OBJECTDIR}/_ext/1568813995/GlassAccessible.o \
	${OBJECTDIR}/_ext/1568813995/GlassApplication.o \
	${OBJECTDIR}/_ext/1568813995/GlassCursor.o \
	${OBJECTDIR}/_ext/1568813995/GlassDialogs.o \
	${OBJECTDIR}/_ext/1568813995/GlassDragSource.o \
	${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Npapi.o \
	${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Overrides.o \
	${OBJECTDIR}/_ext/1568813995/GlassFrameBufferObject.o \
	${OBJECTDIR}/_ext/1568813995/GlassFullscreenWindow.o \
	${OBJECTDIR}/_ext/1568813995/GlassGestureSupport.o \
	${OBJECTDIR}/_ext/1568813995/GlassHelper.o \
	${OBJECTDIR}/_ext/1568813995/GlassHostView.o \
	${OBJECTDIR}/_ext/1568813995/GlassKey.o \
	${OBJECTDIR}/_ext/1568813995/GlassLayer3D.o \
	${OBJECTDIR}/_ext/1568813995/GlassMacros.o \
	${OBJECTDIR}/_ext/1568813995/GlassMenu.o \
	${OBJECTDIR}/_ext/1568813995/GlassNSEvent.o \
	${OBJECTDIR}/_ext/1568813995/GlassOffscreen.o \
	${OBJECTDIR}/_ext/1568813995/GlassPasteboard.o \
	${OBJECTDIR}/_ext/1568813995/GlassPixels.o \
	${OBJECTDIR}/_ext/1568813995/GlassRobot.o \
	${OBJECTDIR}/_ext/1568813995/GlassScreen.o \
	${OBJECTDIR}/_ext/1568813995/GlassStatics.o \
	${OBJECTDIR}/_ext/1568813995/GlassSystemClipboard.o \
	${OBJECTDIR}/_ext/1568813995/GlassTimer.o \
	${OBJECTDIR}/_ext/1568813995/GlassTouches.o \
	${OBJECTDIR}/_ext/1568813995/GlassView.o \
	${OBJECTDIR}/_ext/1568813995/GlassView2D.o \
	${OBJECTDIR}/_ext/1568813995/GlassView3D+Remote.o \
	${OBJECTDIR}/_ext/1568813995/GlassView3D.o \
	${OBJECTDIR}/_ext/1568813995/GlassViewDelegate.o \
	${OBJECTDIR}/_ext/1568813995/GlassWindow+Java.o \
	${OBJECTDIR}/_ext/1568813995/GlassWindow+Overrides.o \
	${OBJECTDIR}/_ext/1568813995/GlassWindow.o \
	${OBJECTDIR}/_ext/1568813995/ProcessInfo.o \
	${OBJECTDIR}/_ext/1568813995/RemoteLayerSupport.o \
	${OBJECTDIR}/_ext/1568804126/BaseWnd.o \
	${OBJECTDIR}/_ext/1568804126/CommonDialogs.o \
	${OBJECTDIR}/_ext/1568804126/CommonDialogs_COM.o \
	${OBJECTDIR}/_ext/1568804126/CommonDialogs_Standard.o \
	${OBJECTDIR}/_ext/1568804126/FullScreenWindow.o \
	${OBJECTDIR}/_ext/1568804126/GlassAccessible.o \
	${OBJECTDIR}/_ext/1568804126/GlassApplication.o \
	${OBJECTDIR}/_ext/1568804126/GlassClipboard.o \
	${OBJECTDIR}/_ext/1568804126/GlassCursor.o \
	${OBJECTDIR}/_ext/1568804126/GlassDnD.o \
	${OBJECTDIR}/_ext/1568804126/GlassInputTextInfo.o \
	${OBJECTDIR}/_ext/1568804126/GlassMenu.o \
	${OBJECTDIR}/_ext/1568804126/GlassScreen.o \
	${OBJECTDIR}/_ext/1568804126/GlassView.o \
	${OBJECTDIR}/_ext/1568804126/GlassWindow.o \
	${OBJECTDIR}/_ext/1568804126/KeyTable.o \
	${OBJECTDIR}/_ext/1568804126/ManipulationEvents.o \
	${OBJECTDIR}/_ext/1568804126/Pixels.o \
	${OBJECTDIR}/_ext/1568804126/Robot.o \
	${OBJECTDIR}/_ext/1568804126/Timer.o \
	${OBJECTDIR}/_ext/1568804126/Utils.o \
	${OBJECTDIR}/_ext/1568804126/ViewContainer.o \
	${OBJECTDIR}/_ext/1568804126/common.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-glass.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-glass.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-glass.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared -fPIC

${OBJECTDIR}/_ext/1568819164/GlassApplication.o: ../../modules/graphics/src/main/native-glass/gtk/GlassApplication.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassApplication.o ../../modules/graphics/src/main/native-glass/gtk/GlassApplication.cpp

${OBJECTDIR}/_ext/1568819164/GlassCommonDialogs.o: ../../modules/graphics/src/main/native-glass/gtk/GlassCommonDialogs.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassCommonDialogs.o ../../modules/graphics/src/main/native-glass/gtk/GlassCommonDialogs.cpp

${OBJECTDIR}/_ext/1568819164/GlassCursor.o: ../../modules/graphics/src/main/native-glass/gtk/GlassCursor.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassCursor.o ../../modules/graphics/src/main/native-glass/gtk/GlassCursor.cpp

${OBJECTDIR}/_ext/1568819164/GlassDnDClipboard.o: ../../modules/graphics/src/main/native-glass/gtk/GlassDnDClipboard.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassDnDClipboard.o ../../modules/graphics/src/main/native-glass/gtk/GlassDnDClipboard.cpp

${OBJECTDIR}/_ext/1568819164/GlassPixels.o: ../../modules/graphics/src/main/native-glass/gtk/GlassPixels.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassPixels.o ../../modules/graphics/src/main/native-glass/gtk/GlassPixels.cpp

${OBJECTDIR}/_ext/1568819164/GlassRobot.o: ../../modules/graphics/src/main/native-glass/gtk/GlassRobot.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassRobot.o ../../modules/graphics/src/main/native-glass/gtk/GlassRobot.cpp

${OBJECTDIR}/_ext/1568819164/GlassSystemClipboard.o: ../../modules/graphics/src/main/native-glass/gtk/GlassSystemClipboard.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassSystemClipboard.o ../../modules/graphics/src/main/native-glass/gtk/GlassSystemClipboard.cpp

${OBJECTDIR}/_ext/1568819164/GlassTimer.o: ../../modules/graphics/src/main/native-glass/gtk/GlassTimer.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassTimer.o ../../modules/graphics/src/main/native-glass/gtk/GlassTimer.cpp

${OBJECTDIR}/_ext/1568819164/GlassView.o: ../../modules/graphics/src/main/native-glass/gtk/GlassView.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassView.o ../../modules/graphics/src/main/native-glass/gtk/GlassView.cpp

${OBJECTDIR}/_ext/1568819164/GlassWindow.o: ../../modules/graphics/src/main/native-glass/gtk/GlassWindow.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/GlassWindow.o ../../modules/graphics/src/main/native-glass/gtk/GlassWindow.cpp

${OBJECTDIR}/_ext/1568819164/glass_dnd.o: ../../modules/graphics/src/main/native-glass/gtk/glass_dnd.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_dnd.o ../../modules/graphics/src/main/native-glass/gtk/glass_dnd.cpp

${OBJECTDIR}/_ext/1568819164/glass_evloop.o: ../../modules/graphics/src/main/native-glass/gtk/glass_evloop.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_evloop.o ../../modules/graphics/src/main/native-glass/gtk/glass_evloop.cpp

${OBJECTDIR}/_ext/1568819164/glass_general.o: ../../modules/graphics/src/main/native-glass/gtk/glass_general.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_general.o ../../modules/graphics/src/main/native-glass/gtk/glass_general.cpp

${OBJECTDIR}/_ext/1568819164/glass_gtkcompat.o: ../../modules/graphics/src/main/native-glass/gtk/glass_gtkcompat.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_gtkcompat.o ../../modules/graphics/src/main/native-glass/gtk/glass_gtkcompat.cpp

${OBJECTDIR}/_ext/1568819164/glass_key.o: ../../modules/graphics/src/main/native-glass/gtk/glass_key.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_key.o ../../modules/graphics/src/main/native-glass/gtk/glass_key.cpp

${OBJECTDIR}/_ext/1568819164/glass_window.o: ../../modules/graphics/src/main/native-glass/gtk/glass_window.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_window.o ../../modules/graphics/src/main/native-glass/gtk/glass_window.cpp

${OBJECTDIR}/_ext/1568819164/glass_window_ime.o: ../../modules/graphics/src/main/native-glass/gtk/glass_window_ime.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568819164
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568819164/glass_window_ime.o ../../modules/graphics/src/main/native-glass/gtk/glass_window_ime.cpp

${OBJECTDIR}/_ext/1568817389/GlassApplication.o: ../../modules/graphics/src/main/native-glass/ios/GlassApplication.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassApplication.o ../../modules/graphics/src/main/native-glass/ios/GlassApplication.m

${OBJECTDIR}/_ext/1568817389/GlassCursor.o: ../../modules/graphics/src/main/native-glass/ios/GlassCursor.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassCursor.o ../../modules/graphics/src/main/native-glass/ios/GlassCursor.m

${OBJECTDIR}/_ext/1568817389/GlassDragDelegate.o: ../../modules/graphics/src/main/native-glass/ios/GlassDragDelegate.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassDragDelegate.o ../../modules/graphics/src/main/native-glass/ios/GlassDragDelegate.m

${OBJECTDIR}/_ext/1568817389/GlassGestureSupport.o: ../../modules/graphics/src/main/native-glass/ios/GlassGestureSupport.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassGestureSupport.o ../../modules/graphics/src/main/native-glass/ios/GlassGestureSupport.m

${OBJECTDIR}/_ext/1568817389/GlassHelper.o: ../../modules/graphics/src/main/native-glass/ios/GlassHelper.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassHelper.o ../../modules/graphics/src/main/native-glass/ios/GlassHelper.m

${OBJECTDIR}/_ext/1568817389/GlassKey.o: ../../modules/graphics/src/main/native-glass/ios/GlassKey.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassKey.o ../../modules/graphics/src/main/native-glass/ios/GlassKey.m

${OBJECTDIR}/_ext/1568817389/GlassPasteboard.o: ../../modules/graphics/src/main/native-glass/ios/GlassPasteboard.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassPasteboard.o ../../modules/graphics/src/main/native-glass/ios/GlassPasteboard.m

${OBJECTDIR}/_ext/1568817389/GlassRobot.o: ../../modules/graphics/src/main/native-glass/ios/GlassRobot.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassRobot.o ../../modules/graphics/src/main/native-glass/ios/GlassRobot.m

${OBJECTDIR}/_ext/1568817389/GlassScreen.o: ../../modules/graphics/src/main/native-glass/ios/GlassScreen.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassScreen.o ../../modules/graphics/src/main/native-glass/ios/GlassScreen.m

${OBJECTDIR}/_ext/1568817389/GlassStatics.o: ../../modules/graphics/src/main/native-glass/ios/GlassStatics.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassStatics.o ../../modules/graphics/src/main/native-glass/ios/GlassStatics.m

${OBJECTDIR}/_ext/1568817389/GlassTimer.o: ../../modules/graphics/src/main/native-glass/ios/GlassTimer.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassTimer.o ../../modules/graphics/src/main/native-glass/ios/GlassTimer.m

${OBJECTDIR}/_ext/1568817389/GlassView.o: ../../modules/graphics/src/main/native-glass/ios/GlassView.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassView.o ../../modules/graphics/src/main/native-glass/ios/GlassView.m

${OBJECTDIR}/_ext/1568817389/GlassViewController.o: ../../modules/graphics/src/main/native-glass/ios/GlassViewController.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassViewController.o ../../modules/graphics/src/main/native-glass/ios/GlassViewController.m

${OBJECTDIR}/_ext/1568817389/GlassViewDelegate.o: ../../modules/graphics/src/main/native-glass/ios/GlassViewDelegate.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassViewDelegate.o ../../modules/graphics/src/main/native-glass/ios/GlassViewDelegate.m

${OBJECTDIR}/_ext/1568817389/GlassViewGL.o: ../../modules/graphics/src/main/native-glass/ios/GlassViewGL.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassViewGL.o ../../modules/graphics/src/main/native-glass/ios/GlassViewGL.m

${OBJECTDIR}/_ext/1568817389/GlassWindow.o: ../../modules/graphics/src/main/native-glass/ios/GlassWindow.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568817389
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568817389/GlassWindow.o ../../modules/graphics/src/main/native-glass/ios/GlassWindow.m

${OBJECTDIR}/_ext/1388619080/LensApplication.o: ../../modules/graphics/src/main/native-glass/lens/LensApplication.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensApplication.o ../../modules/graphics/src/main/native-glass/lens/LensApplication.c

${OBJECTDIR}/_ext/1388619080/LensCursor.o: ../../modules/graphics/src/main/native-glass/lens/LensCursor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensCursor.o ../../modules/graphics/src/main/native-glass/lens/LensCursor.c

${OBJECTDIR}/_ext/1388619080/LensCursorImages.o: ../../modules/graphics/src/main/native-glass/lens/LensCursorImages.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensCursorImages.o ../../modules/graphics/src/main/native-glass/lens/LensCursorImages.c

${OBJECTDIR}/_ext/1388619080/LensInputEvents.o: ../../modules/graphics/src/main/native-glass/lens/LensInputEvents.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensInputEvents.o ../../modules/graphics/src/main/native-glass/lens/LensInputEvents.c

${OBJECTDIR}/_ext/1388619080/LensLogger.o: ../../modules/graphics/src/main/native-glass/lens/LensLogger.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensLogger.o ../../modules/graphics/src/main/native-glass/lens/LensLogger.c

${OBJECTDIR}/_ext/1388619080/LensPixels.o: ../../modules/graphics/src/main/native-glass/lens/LensPixels.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensPixels.o ../../modules/graphics/src/main/native-glass/lens/LensPixels.c

${OBJECTDIR}/_ext/1388619080/LensRobot.o: ../../modules/graphics/src/main/native-glass/lens/LensRobot.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensRobot.o ../../modules/graphics/src/main/native-glass/lens/LensRobot.c

${OBJECTDIR}/_ext/1388619080/LensScreen.o: ../../modules/graphics/src/main/native-glass/lens/LensScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensScreen.o ../../modules/graphics/src/main/native-glass/lens/LensScreen.c

${OBJECTDIR}/_ext/1388619080/LensView.o: ../../modules/graphics/src/main/native-glass/lens/LensView.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensView.o ../../modules/graphics/src/main/native-glass/lens/LensView.c

${OBJECTDIR}/_ext/1388619080/LensWindow.o: ../../modules/graphics/src/main/native-glass/lens/LensWindow.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1388619080
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1388619080/LensWindow.o ../../modules/graphics/src/main/native-glass/lens/LensWindow.c

${OBJECTDIR}/_ext/1049586152/android.o: ../../modules/graphics/src/main/native-glass/lens/android/android.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1049586152
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1049586152/android.o ../../modules/graphics/src/main/native-glass/lens/android/android.c

${OBJECTDIR}/_ext/1170728052/fbCursor.o: ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/fbCursor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1170728052
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1170728052/fbCursor.o ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/fbCursor.c

${OBJECTDIR}/_ext/1170728052/fbDispman.o: ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/fbDispman.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1170728052
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1170728052/fbDispman.o ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/fbDispman.c

${OBJECTDIR}/_ext/1170728052/wrapped_bcm.o: ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/wrapped_bcm.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1170728052
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1170728052/wrapped_bcm.o ../../modules/graphics/src/main/native-glass/lens/cursor/fbCursor/wrapped_bcm.c

${OBJECTDIR}/_ext/228741247/nullCursor.o: ../../modules/graphics/src/main/native-glass/lens/cursor/nullCursor/nullCursor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/228741247
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/228741247/nullCursor.o ../../modules/graphics/src/main/native-glass/lens/cursor/nullCursor/nullCursor.c

${OBJECTDIR}/_ext/1804231149/androidInput.o: ../../modules/graphics/src/main/native-glass/lens/input/android/androidInput.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1804231149
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1804231149/androidInput.o ../../modules/graphics/src/main/native-glass/lens/input/android/androidInput.c

${OBJECTDIR}/_ext/1804231149/androidLens.o: ../../modules/graphics/src/main/native-glass/lens/input/android/androidLens.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1804231149
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1804231149/androidLens.o ../../modules/graphics/src/main/native-glass/lens/input/android/androidLens.c

${OBJECTDIR}/_ext/239661476/udevInput.o: ../../modules/graphics/src/main/native-glass/lens/input/udev/udevInput.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/239661476
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/239661476/udevInput.o ../../modules/graphics/src/main/native-glass/lens/input/udev/udevInput.c

${OBJECTDIR}/_ext/660835749/x11Input.o: ../../modules/graphics/src/main/native-glass/lens/input/x11Container/x11Input.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/660835749
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/660835749/x11Input.o ../../modules/graphics/src/main/native-glass/lens/input/x11Container/x11Input.c

${OBJECTDIR}/_ext/125406599/rfb.o: ../../modules/graphics/src/main/native-glass/lens/lensRFB/rfb.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/125406599
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/125406599/rfb.o ../../modules/graphics/src/main/native-glass/lens/lensRFB/rfb.c

${OBJECTDIR}/_ext/774031757/LensWindowManager.o: ../../modules/graphics/src/main/native-glass/lens/wm/LensWindowManager.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/774031757
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/774031757/LensWindowManager.o ../../modules/graphics/src/main/native-glass/lens/wm/LensWindowManager.c

${OBJECTDIR}/_ext/774031757/robot.o: ../../modules/graphics/src/main/native-glass/lens/wm/robot.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/774031757
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/774031757/robot.o ../../modules/graphics/src/main/native-glass/lens/wm/robot.c

${OBJECTDIR}/_ext/1962315986/androidScreen.o: ../../modules/graphics/src/main/native-glass/lens/wm/screen/androidScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1962315986
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1962315986/androidScreen.o ../../modules/graphics/src/main/native-glass/lens/wm/screen/androidScreen.c

${OBJECTDIR}/_ext/1962315986/dfbScreen.o: ../../modules/graphics/src/main/native-glass/lens/wm/screen/dfbScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1962315986
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1962315986/dfbScreen.o ../../modules/graphics/src/main/native-glass/lens/wm/screen/dfbScreen.c

${OBJECTDIR}/_ext/1962315986/fbdevScreen.o: ../../modules/graphics/src/main/native-glass/lens/wm/screen/fbdevScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1962315986
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1962315986/fbdevScreen.o ../../modules/graphics/src/main/native-glass/lens/wm/screen/fbdevScreen.c

${OBJECTDIR}/_ext/1962315986/headlessScreen.o: ../../modules/graphics/src/main/native-glass/lens/wm/screen/headlessScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1962315986
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1962315986/headlessScreen.o ../../modules/graphics/src/main/native-glass/lens/wm/screen/headlessScreen.c

${OBJECTDIR}/_ext/1962315986/x11ContainerScreen.o: ../../modules/graphics/src/main/native-glass/lens/wm/screen/x11ContainerScreen.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1962315986
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1962315986/x11ContainerScreen.o ../../modules/graphics/src/main/native-glass/lens/wm/screen/x11ContainerScreen.c

${OBJECTDIR}/_ext/1568813995/GlassAccessible.o: ../../modules/graphics/src/main/native-glass/mac/GlassAccessible.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassAccessible.o ../../modules/graphics/src/main/native-glass/mac/GlassAccessible.m

${OBJECTDIR}/_ext/1568813995/GlassApplication.o: ../../modules/graphics/src/main/native-glass/mac/GlassApplication.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassApplication.o ../../modules/graphics/src/main/native-glass/mac/GlassApplication.m

${OBJECTDIR}/_ext/1568813995/GlassCursor.o: ../../modules/graphics/src/main/native-glass/mac/GlassCursor.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassCursor.o ../../modules/graphics/src/main/native-glass/mac/GlassCursor.m

${OBJECTDIR}/_ext/1568813995/GlassDialogs.o: ../../modules/graphics/src/main/native-glass/mac/GlassDialogs.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassDialogs.o ../../modules/graphics/src/main/native-glass/mac/GlassDialogs.m

${OBJECTDIR}/_ext/1568813995/GlassDragSource.o: ../../modules/graphics/src/main/native-glass/mac/GlassDragSource.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassDragSource.o ../../modules/graphics/src/main/native-glass/mac/GlassDragSource.m

${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Npapi.o: ../../modules/graphics/src/main/native-glass/mac/GlassEmbeddedWindow+Npapi.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Npapi.o ../../modules/graphics/src/main/native-glass/mac/GlassEmbeddedWindow+Npapi.m

${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Overrides.o: ../../modules/graphics/src/main/native-glass/mac/GlassEmbeddedWindow+Overrides.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassEmbeddedWindow+Overrides.o ../../modules/graphics/src/main/native-glass/mac/GlassEmbeddedWindow+Overrides.m

${OBJECTDIR}/_ext/1568813995/GlassFrameBufferObject.o: ../../modules/graphics/src/main/native-glass/mac/GlassFrameBufferObject.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassFrameBufferObject.o ../../modules/graphics/src/main/native-glass/mac/GlassFrameBufferObject.m

${OBJECTDIR}/_ext/1568813995/GlassFullscreenWindow.o: ../../modules/graphics/src/main/native-glass/mac/GlassFullscreenWindow.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassFullscreenWindow.o ../../modules/graphics/src/main/native-glass/mac/GlassFullscreenWindow.m

${OBJECTDIR}/_ext/1568813995/GlassGestureSupport.o: ../../modules/graphics/src/main/native-glass/mac/GlassGestureSupport.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassGestureSupport.o ../../modules/graphics/src/main/native-glass/mac/GlassGestureSupport.m

${OBJECTDIR}/_ext/1568813995/GlassHelper.o: ../../modules/graphics/src/main/native-glass/mac/GlassHelper.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassHelper.o ../../modules/graphics/src/main/native-glass/mac/GlassHelper.m

${OBJECTDIR}/_ext/1568813995/GlassHostView.o: ../../modules/graphics/src/main/native-glass/mac/GlassHostView.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassHostView.o ../../modules/graphics/src/main/native-glass/mac/GlassHostView.m

${OBJECTDIR}/_ext/1568813995/GlassKey.o: ../../modules/graphics/src/main/native-glass/mac/GlassKey.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassKey.o ../../modules/graphics/src/main/native-glass/mac/GlassKey.m

${OBJECTDIR}/_ext/1568813995/GlassLayer3D.o: ../../modules/graphics/src/main/native-glass/mac/GlassLayer3D.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassLayer3D.o ../../modules/graphics/src/main/native-glass/mac/GlassLayer3D.m

${OBJECTDIR}/_ext/1568813995/GlassMacros.o: ../../modules/graphics/src/main/native-glass/mac/GlassMacros.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassMacros.o ../../modules/graphics/src/main/native-glass/mac/GlassMacros.m

${OBJECTDIR}/_ext/1568813995/GlassMenu.o: ../../modules/graphics/src/main/native-glass/mac/GlassMenu.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassMenu.o ../../modules/graphics/src/main/native-glass/mac/GlassMenu.m

${OBJECTDIR}/_ext/1568813995/GlassNSEvent.o: ../../modules/graphics/src/main/native-glass/mac/GlassNSEvent.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassNSEvent.o ../../modules/graphics/src/main/native-glass/mac/GlassNSEvent.m

${OBJECTDIR}/_ext/1568813995/GlassOffscreen.o: ../../modules/graphics/src/main/native-glass/mac/GlassOffscreen.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassOffscreen.o ../../modules/graphics/src/main/native-glass/mac/GlassOffscreen.m

${OBJECTDIR}/_ext/1568813995/GlassPasteboard.o: ../../modules/graphics/src/main/native-glass/mac/GlassPasteboard.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassPasteboard.o ../../modules/graphics/src/main/native-glass/mac/GlassPasteboard.m

${OBJECTDIR}/_ext/1568813995/GlassPixels.o: ../../modules/graphics/src/main/native-glass/mac/GlassPixels.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassPixels.o ../../modules/graphics/src/main/native-glass/mac/GlassPixels.m

${OBJECTDIR}/_ext/1568813995/GlassRobot.o: ../../modules/graphics/src/main/native-glass/mac/GlassRobot.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassRobot.o ../../modules/graphics/src/main/native-glass/mac/GlassRobot.m

${OBJECTDIR}/_ext/1568813995/GlassScreen.o: ../../modules/graphics/src/main/native-glass/mac/GlassScreen.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassScreen.o ../../modules/graphics/src/main/native-glass/mac/GlassScreen.m

${OBJECTDIR}/_ext/1568813995/GlassStatics.o: ../../modules/graphics/src/main/native-glass/mac/GlassStatics.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassStatics.o ../../modules/graphics/src/main/native-glass/mac/GlassStatics.m

${OBJECTDIR}/_ext/1568813995/GlassSystemClipboard.o: ../../modules/graphics/src/main/native-glass/mac/GlassSystemClipboard.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassSystemClipboard.o ../../modules/graphics/src/main/native-glass/mac/GlassSystemClipboard.m

${OBJECTDIR}/_ext/1568813995/GlassTimer.o: ../../modules/graphics/src/main/native-glass/mac/GlassTimer.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassTimer.o ../../modules/graphics/src/main/native-glass/mac/GlassTimer.m

${OBJECTDIR}/_ext/1568813995/GlassTouches.o: ../../modules/graphics/src/main/native-glass/mac/GlassTouches.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassTouches.o ../../modules/graphics/src/main/native-glass/mac/GlassTouches.m

${OBJECTDIR}/_ext/1568813995/GlassView.o: ../../modules/graphics/src/main/native-glass/mac/GlassView.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassView.o ../../modules/graphics/src/main/native-glass/mac/GlassView.m

${OBJECTDIR}/_ext/1568813995/GlassView2D.o: ../../modules/graphics/src/main/native-glass/mac/GlassView2D.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassView2D.o ../../modules/graphics/src/main/native-glass/mac/GlassView2D.m

${OBJECTDIR}/_ext/1568813995/GlassView3D+Remote.o: ../../modules/graphics/src/main/native-glass/mac/GlassView3D+Remote.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassView3D+Remote.o ../../modules/graphics/src/main/native-glass/mac/GlassView3D+Remote.m

${OBJECTDIR}/_ext/1568813995/GlassView3D.o: ../../modules/graphics/src/main/native-glass/mac/GlassView3D.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassView3D.o ../../modules/graphics/src/main/native-glass/mac/GlassView3D.m

${OBJECTDIR}/_ext/1568813995/GlassViewDelegate.o: ../../modules/graphics/src/main/native-glass/mac/GlassViewDelegate.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassViewDelegate.o ../../modules/graphics/src/main/native-glass/mac/GlassViewDelegate.m

${OBJECTDIR}/_ext/1568813995/GlassWindow+Java.o: ../../modules/graphics/src/main/native-glass/mac/GlassWindow+Java.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassWindow+Java.o ../../modules/graphics/src/main/native-glass/mac/GlassWindow+Java.m

${OBJECTDIR}/_ext/1568813995/GlassWindow+Overrides.o: ../../modules/graphics/src/main/native-glass/mac/GlassWindow+Overrides.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassWindow+Overrides.o ../../modules/graphics/src/main/native-glass/mac/GlassWindow+Overrides.m

${OBJECTDIR}/_ext/1568813995/GlassWindow.o: ../../modules/graphics/src/main/native-glass/mac/GlassWindow.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/GlassWindow.o ../../modules/graphics/src/main/native-glass/mac/GlassWindow.m

${OBJECTDIR}/_ext/1568813995/ProcessInfo.o: ../../modules/graphics/src/main/native-glass/mac/ProcessInfo.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/ProcessInfo.o ../../modules/graphics/src/main/native-glass/mac/ProcessInfo.m

${OBJECTDIR}/_ext/1568813995/RemoteLayerSupport.o: ../../modules/graphics/src/main/native-glass/mac/RemoteLayerSupport.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568813995
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568813995/RemoteLayerSupport.o ../../modules/graphics/src/main/native-glass/mac/RemoteLayerSupport.m

${OBJECTDIR}/_ext/1568804126/BaseWnd.o: ../../modules/graphics/src/main/native-glass/win/BaseWnd.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/BaseWnd.o ../../modules/graphics/src/main/native-glass/win/BaseWnd.cpp

${OBJECTDIR}/_ext/1568804126/CommonDialogs.o: ../../modules/graphics/src/main/native-glass/win/CommonDialogs.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/CommonDialogs.o ../../modules/graphics/src/main/native-glass/win/CommonDialogs.cpp

${OBJECTDIR}/_ext/1568804126/CommonDialogs_COM.o: ../../modules/graphics/src/main/native-glass/win/CommonDialogs_COM.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/CommonDialogs_COM.o ../../modules/graphics/src/main/native-glass/win/CommonDialogs_COM.cpp

${OBJECTDIR}/_ext/1568804126/CommonDialogs_Standard.o: ../../modules/graphics/src/main/native-glass/win/CommonDialogs_Standard.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/CommonDialogs_Standard.o ../../modules/graphics/src/main/native-glass/win/CommonDialogs_Standard.cpp

${OBJECTDIR}/_ext/1568804126/FullScreenWindow.o: ../../modules/graphics/src/main/native-glass/win/FullScreenWindow.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/FullScreenWindow.o ../../modules/graphics/src/main/native-glass/win/FullScreenWindow.cpp

${OBJECTDIR}/_ext/1568804126/GlassAccessible.o: ../../modules/graphics/src/main/native-glass/win/GlassAccessible.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassAccessible.o ../../modules/graphics/src/main/native-glass/win/GlassAccessible.cpp

${OBJECTDIR}/_ext/1568804126/GlassApplication.o: ../../modules/graphics/src/main/native-glass/win/GlassApplication.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassApplication.o ../../modules/graphics/src/main/native-glass/win/GlassApplication.cpp

${OBJECTDIR}/_ext/1568804126/GlassClipboard.o: ../../modules/graphics/src/main/native-glass/win/GlassClipboard.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassClipboard.o ../../modules/graphics/src/main/native-glass/win/GlassClipboard.cpp

${OBJECTDIR}/_ext/1568804126/GlassCursor.o: ../../modules/graphics/src/main/native-glass/win/GlassCursor.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassCursor.o ../../modules/graphics/src/main/native-glass/win/GlassCursor.cpp

${OBJECTDIR}/_ext/1568804126/GlassDnD.o: ../../modules/graphics/src/main/native-glass/win/GlassDnD.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassDnD.o ../../modules/graphics/src/main/native-glass/win/GlassDnD.cpp

${OBJECTDIR}/_ext/1568804126/GlassInputTextInfo.o: ../../modules/graphics/src/main/native-glass/win/GlassInputTextInfo.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassInputTextInfo.o ../../modules/graphics/src/main/native-glass/win/GlassInputTextInfo.cpp

${OBJECTDIR}/_ext/1568804126/GlassMenu.o: ../../modules/graphics/src/main/native-glass/win/GlassMenu.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassMenu.o ../../modules/graphics/src/main/native-glass/win/GlassMenu.cpp

${OBJECTDIR}/_ext/1568804126/GlassScreen.o: ../../modules/graphics/src/main/native-glass/win/GlassScreen.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassScreen.o ../../modules/graphics/src/main/native-glass/win/GlassScreen.cpp

${OBJECTDIR}/_ext/1568804126/GlassView.o: ../../modules/graphics/src/main/native-glass/win/GlassView.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassView.o ../../modules/graphics/src/main/native-glass/win/GlassView.cpp

${OBJECTDIR}/_ext/1568804126/GlassWindow.o: ../../modules/graphics/src/main/native-glass/win/GlassWindow.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/GlassWindow.o ../../modules/graphics/src/main/native-glass/win/GlassWindow.cpp

${OBJECTDIR}/_ext/1568804126/KeyTable.o: ../../modules/graphics/src/main/native-glass/win/KeyTable.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/KeyTable.o ../../modules/graphics/src/main/native-glass/win/KeyTable.cpp

${OBJECTDIR}/_ext/1568804126/ManipulationEvents.o: ../../modules/graphics/src/main/native-glass/win/ManipulationEvents.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/ManipulationEvents.o ../../modules/graphics/src/main/native-glass/win/ManipulationEvents.cpp

${OBJECTDIR}/_ext/1568804126/Pixels.o: ../../modules/graphics/src/main/native-glass/win/Pixels.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/Pixels.o ../../modules/graphics/src/main/native-glass/win/Pixels.cpp

${OBJECTDIR}/_ext/1568804126/Robot.o: ../../modules/graphics/src/main/native-glass/win/Robot.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/Robot.o ../../modules/graphics/src/main/native-glass/win/Robot.cpp

${OBJECTDIR}/_ext/1568804126/Timer.o: ../../modules/graphics/src/main/native-glass/win/Timer.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/Timer.o ../../modules/graphics/src/main/native-glass/win/Timer.cpp

${OBJECTDIR}/_ext/1568804126/Utils.o: ../../modules/graphics/src/main/native-glass/win/Utils.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/Utils.o ../../modules/graphics/src/main/native-glass/win/Utils.cpp

${OBJECTDIR}/_ext/1568804126/ViewContainer.o: ../../modules/graphics/src/main/native-glass/win/ViewContainer.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/ViewContainer.o ../../modules/graphics/src/main/native-glass/win/ViewContainer.cpp

${OBJECTDIR}/_ext/1568804126/common.o: ../../modules/graphics/src/main/native-glass/win/common.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1568804126
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1568804126/common.o ../../modules/graphics/src/main/native-glass/win/common.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-glass.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
