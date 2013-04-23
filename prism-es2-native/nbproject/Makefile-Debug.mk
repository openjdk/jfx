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
CC=gcc.exe
CCC=g++.exe
CXX=g++.exe
FC=gfortran.exe
AS=as.exe

# Macros
CND_PLATFORM=Cygwin_4.x-Windows
CND_DLIB_EXT=dll
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/src/GLContext.o \
	${OBJECTDIR}/src/windows/WinGLFactory.o \
	${OBJECTDIR}/src/windows/WinGLContext.o \
	${OBJECTDIR}/src/macosx/MacGLPixelFormat.o \
	${OBJECTDIR}/src/eglfb/EGLFBGLPixelFormat.o \
	${OBJECTDIR}/src/eglfb/EGLFBGLFactory.o \
	${OBJECTDIR}/src/eglfb/EGLFBGLContext.o \
	${OBJECTDIR}/src/macosx/MacOSXWindowSystemInterface.o \
	${OBJECTDIR}/src/eglx11/EGLX11GLContext.o \
	${OBJECTDIR}/src/GLDrawable.o \
	${OBJECTDIR}/src/eglfb/eglUtils.o \
	${OBJECTDIR}/src/ios/IOSGLDrawable.o \
	${OBJECTDIR}/src/GLPixelFormat.o \
	${OBJECTDIR}/src/ios/IOSGLContext.o \
	${OBJECTDIR}/src/eglx11/EGLX11GLFactory.o \
	${OBJECTDIR}/src/windows/WinGLPixelFormat.o \
	${OBJECTDIR}/src/eglx11/EGLX11GLPixelFormat.o \
	${OBJECTDIR}/src/macosx/MacGLContext.o \
	${OBJECTDIR}/src/windows/WinGLDrawable.o \
	${OBJECTDIR}/src/ios/IOSGLFactory.o \
	${OBJECTDIR}/src/GLFactory.o \
	${OBJECTDIR}/src/x11/X11GLDrawable.o \
	${OBJECTDIR}/src/macosx/MacGLDrawable.o \
	${OBJECTDIR}/src/x11/X11GLFactory.o \
	${OBJECTDIR}/src/x11/X11GLContext.o \
	${OBJECTDIR}/src/eglfb/EGLFBGLDrawable.o \
	${OBJECTDIR}/src/eglx11/EGLX11GLDrawable.o \
	${OBJECTDIR}/src/macosx/MacGLFactory.o \
	${OBJECTDIR}/src/ios/IOSWindowSystemInterface.o \
	${OBJECTDIR}/src/x11/X11GLPixelFormat.o \
	${OBJECTDIR}/src/eglfb/wrapped_egl.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libprism-es2-native.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libprism-es2-native.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -shared -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libprism-es2-native.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/src/GLContext.o: src/GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/GLContext.o src/GLContext.c

${OBJECTDIR}/src/windows/WinGLFactory.o: src/windows/WinGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/windows
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/windows/WinGLFactory.o src/windows/WinGLFactory.c

${OBJECTDIR}/src/windows/WinGLContext.o: src/windows/WinGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/windows
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/windows/WinGLContext.o src/windows/WinGLContext.c

${OBJECTDIR}/src/macosx/MacGLPixelFormat.o: src/macosx/MacGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src/macosx
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/macosx/MacGLPixelFormat.o src/macosx/MacGLPixelFormat.c

${OBJECTDIR}/src/eglfb/EGLFBGLPixelFormat.o: src/eglfb/EGLFBGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/EGLFBGLPixelFormat.o src/eglfb/EGLFBGLPixelFormat.c

${OBJECTDIR}/src/eglfb/EGLFBGLFactory.o: src/eglfb/EGLFBGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/EGLFBGLFactory.o src/eglfb/EGLFBGLFactory.c

${OBJECTDIR}/src/eglfb/EGLFBGLContext.o: src/eglfb/EGLFBGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/EGLFBGLContext.o src/eglfb/EGLFBGLContext.c

${OBJECTDIR}/src/macosx/MacOSXWindowSystemInterface.o: src/macosx/MacOSXWindowSystemInterface.m 
	${MKDIR} -p ${OBJECTDIR}/src/macosx
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/macosx/MacOSXWindowSystemInterface.o src/macosx/MacOSXWindowSystemInterface.m

${OBJECTDIR}/src/eglx11/EGLX11GLContext.o: src/eglx11/EGLX11GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglx11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglx11/EGLX11GLContext.o src/eglx11/EGLX11GLContext.c

${OBJECTDIR}/src/GLDrawable.o: src/GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/GLDrawable.o src/GLDrawable.c

${OBJECTDIR}/src/eglfb/eglUtils.o: src/eglfb/eglUtils.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/eglUtils.o src/eglfb/eglUtils.c

${OBJECTDIR}/src/ios/IOSGLDrawable.o: src/ios/IOSGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/ios
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/ios/IOSGLDrawable.o src/ios/IOSGLDrawable.c

${OBJECTDIR}/src/GLPixelFormat.o: src/GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/GLPixelFormat.o src/GLPixelFormat.c

${OBJECTDIR}/src/ios/IOSGLContext.o: src/ios/IOSGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/ios
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/ios/IOSGLContext.o src/ios/IOSGLContext.c

${OBJECTDIR}/src/eglx11/EGLX11GLFactory.o: src/eglx11/EGLX11GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglx11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglx11/EGLX11GLFactory.o src/eglx11/EGLX11GLFactory.c

${OBJECTDIR}/src/windows/WinGLPixelFormat.o: src/windows/WinGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src/windows
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/windows/WinGLPixelFormat.o src/windows/WinGLPixelFormat.c

${OBJECTDIR}/src/eglx11/EGLX11GLPixelFormat.o: src/eglx11/EGLX11GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglx11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglx11/EGLX11GLPixelFormat.o src/eglx11/EGLX11GLPixelFormat.c

${OBJECTDIR}/src/macosx/MacGLContext.o: src/macosx/MacGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/macosx
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/macosx/MacGLContext.o src/macosx/MacGLContext.c

${OBJECTDIR}/src/windows/WinGLDrawable.o: src/windows/WinGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/windows
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/windows/WinGLDrawable.o src/windows/WinGLDrawable.c

${OBJECTDIR}/src/ios/IOSGLFactory.o: src/ios/IOSGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/ios
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/ios/IOSGLFactory.o src/ios/IOSGLFactory.c

${OBJECTDIR}/src/GLFactory.o: src/GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/GLFactory.o src/GLFactory.c

${OBJECTDIR}/src/x11/X11GLDrawable.o: src/x11/X11GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/x11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/x11/X11GLDrawable.o src/x11/X11GLDrawable.c

${OBJECTDIR}/src/macosx/MacGLDrawable.o: src/macosx/MacGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/macosx
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/macosx/MacGLDrawable.o src/macosx/MacGLDrawable.c

${OBJECTDIR}/src/x11/X11GLFactory.o: src/x11/X11GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/x11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/x11/X11GLFactory.o src/x11/X11GLFactory.c

${OBJECTDIR}/src/x11/X11GLContext.o: src/x11/X11GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/src/x11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/x11/X11GLContext.o src/x11/X11GLContext.c

${OBJECTDIR}/src/eglfb/EGLFBGLDrawable.o: src/eglfb/EGLFBGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/EGLFBGLDrawable.o src/eglfb/EGLFBGLDrawable.c

${OBJECTDIR}/src/eglx11/EGLX11GLDrawable.o: src/eglx11/EGLX11GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglx11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglx11/EGLX11GLDrawable.o src/eglx11/EGLX11GLDrawable.c

${OBJECTDIR}/src/macosx/MacGLFactory.o: src/macosx/MacGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/src/macosx
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/macosx/MacGLFactory.o src/macosx/MacGLFactory.c

${OBJECTDIR}/src/ios/IOSWindowSystemInterface.o: src/ios/IOSWindowSystemInterface.m 
	${MKDIR} -p ${OBJECTDIR}/src/ios
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/ios/IOSWindowSystemInterface.o src/ios/IOSWindowSystemInterface.m

${OBJECTDIR}/src/x11/X11GLPixelFormat.o: src/x11/X11GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/src/x11
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/x11/X11GLPixelFormat.o src/x11/X11GLPixelFormat.c

${OBJECTDIR}/src/eglfb/wrapped_egl.o: src/eglfb/wrapped_egl.c 
	${MKDIR} -p ${OBJECTDIR}/src/eglfb
	${RM} $@.d
	$(COMPILE.c) -g -Isrc/eglfb -Isrc/eglx11 -Isrc/ios -Isrc/macosx -Isrc/windows -Isrc/x11 -Ibuild  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/eglfb/wrapped_egl.o src/eglfb/wrapped_egl.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libprism-es2-native.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
