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
	${OBJECTDIR}/_ext/284620591/GLContext.o \
	${OBJECTDIR}/_ext/284620591/GLDrawable.o \
	${OBJECTDIR}/_ext/284620591/GLFactory.o \
	${OBJECTDIR}/_ext/284620591/GLPixelFormat.o \
	${OBJECTDIR}/_ext/2081220488/EGLFBGLContext.o \
	${OBJECTDIR}/_ext/2081220488/EGLFBGLDrawable.o \
	${OBJECTDIR}/_ext/2081220488/EGLFBGLFactory.o \
	${OBJECTDIR}/_ext/2081220488/EGLFBGLPixelFormat.o \
	${OBJECTDIR}/_ext/2081220488/eglUtils.o \
	${OBJECTDIR}/_ext/2081220488/wrapped_egl.o \
	${OBJECTDIR}/_ext/93341516/EGLX11GLContext.o \
	${OBJECTDIR}/_ext/93341516/EGLX11GLDrawable.o \
	${OBJECTDIR}/_ext/93341516/EGLX11GLFactory.o \
	${OBJECTDIR}/_ext/93341516/EGLX11GLPixelFormat.o \
	${OBJECTDIR}/_ext/1092801073/IOSGLContext.o \
	${OBJECTDIR}/_ext/1092801073/IOSGLDrawable.o \
	${OBJECTDIR}/_ext/1092801073/IOSGLFactory.o \
	${OBJECTDIR}/_ext/1092801073/IOSWindowSystemInterface.o \
	${OBJECTDIR}/_ext/316558947/MacGLContext.o \
	${OBJECTDIR}/_ext/316558947/MacGLDrawable.o \
	${OBJECTDIR}/_ext/316558947/MacGLFactory.o \
	${OBJECTDIR}/_ext/316558947/MacGLPixelFormat.o \
	${OBJECTDIR}/_ext/316558947/MacOSXWindowSystemInterface.o \
	${OBJECTDIR}/_ext/1747355461/WinGLContext.o \
	${OBJECTDIR}/_ext/1747355461/WinGLDrawable.o \
	${OBJECTDIR}/_ext/1747355461/WinGLFactory.o \
	${OBJECTDIR}/_ext/1747355461/WinGLPixelFormat.o \
	${OBJECTDIR}/_ext/1092788646/X11GLContext.o \
	${OBJECTDIR}/_ext/1092788646/X11GLDrawable.o \
	${OBJECTDIR}/_ext/1092788646/X11GLFactory.o \
	${OBJECTDIR}/_ext/1092788646/X11GLPixelFormat.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-es2.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-es2.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-es2.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared

${OBJECTDIR}/_ext/284620591/GLContext.o: ../../modules/graphics/src/main/native-prism-es2/GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/284620591
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284620591/GLContext.o ../../modules/graphics/src/main/native-prism-es2/GLContext.c

${OBJECTDIR}/_ext/284620591/GLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/284620591
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284620591/GLDrawable.o ../../modules/graphics/src/main/native-prism-es2/GLDrawable.c

${OBJECTDIR}/_ext/284620591/GLFactory.o: ../../modules/graphics/src/main/native-prism-es2/GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/284620591
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284620591/GLFactory.o ../../modules/graphics/src/main/native-prism-es2/GLFactory.c

${OBJECTDIR}/_ext/284620591/GLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/284620591
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284620591/GLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/GLPixelFormat.c

${OBJECTDIR}/_ext/2081220488/EGLFBGLContext.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/EGLFBGLContext.o ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLContext.c

${OBJECTDIR}/_ext/2081220488/EGLFBGLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/EGLFBGLDrawable.o ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLDrawable.c

${OBJECTDIR}/_ext/2081220488/EGLFBGLFactory.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/EGLFBGLFactory.o ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLFactory.c

${OBJECTDIR}/_ext/2081220488/EGLFBGLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/EGLFBGLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/eglfb/EGLFBGLPixelFormat.c

${OBJECTDIR}/_ext/2081220488/eglUtils.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/eglUtils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/eglUtils.o ../../modules/graphics/src/main/native-prism-es2/eglfb/eglUtils.c

${OBJECTDIR}/_ext/2081220488/wrapped_egl.o: ../../modules/graphics/src/main/native-prism-es2/eglfb/wrapped_egl.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/2081220488
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2081220488/wrapped_egl.o ../../modules/graphics/src/main/native-prism-es2/eglfb/wrapped_egl.c

${OBJECTDIR}/_ext/93341516/EGLX11GLContext.o: ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/93341516
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/93341516/EGLX11GLContext.o ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLContext.c

${OBJECTDIR}/_ext/93341516/EGLX11GLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/93341516
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/93341516/EGLX11GLDrawable.o ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLDrawable.c

${OBJECTDIR}/_ext/93341516/EGLX11GLFactory.o: ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/93341516
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/93341516/EGLX11GLFactory.o ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLFactory.c

${OBJECTDIR}/_ext/93341516/EGLX11GLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/93341516
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/93341516/EGLX11GLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/eglx11/EGLX11GLPixelFormat.c

${OBJECTDIR}/_ext/1092801073/IOSGLContext.o: ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092801073
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092801073/IOSGLContext.o ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLContext.c

${OBJECTDIR}/_ext/1092801073/IOSGLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092801073
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092801073/IOSGLDrawable.o ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLDrawable.c

${OBJECTDIR}/_ext/1092801073/IOSGLFactory.o: ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092801073
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092801073/IOSGLFactory.o ../../modules/graphics/src/main/native-prism-es2/ios/IOSGLFactory.c

${OBJECTDIR}/_ext/1092801073/IOSWindowSystemInterface.o: ../../modules/graphics/src/main/native-prism-es2/ios/IOSWindowSystemInterface.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092801073
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092801073/IOSWindowSystemInterface.o ../../modules/graphics/src/main/native-prism-es2/ios/IOSWindowSystemInterface.m

${OBJECTDIR}/_ext/316558947/MacGLContext.o: ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/316558947
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/316558947/MacGLContext.o ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLContext.c

${OBJECTDIR}/_ext/316558947/MacGLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/316558947
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/316558947/MacGLDrawable.o ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLDrawable.c

${OBJECTDIR}/_ext/316558947/MacGLFactory.o: ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/316558947
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/316558947/MacGLFactory.o ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLFactory.c

${OBJECTDIR}/_ext/316558947/MacGLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/316558947
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/316558947/MacGLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/macosx/MacGLPixelFormat.c

${OBJECTDIR}/_ext/316558947/MacOSXWindowSystemInterface.o: ../../modules/graphics/src/main/native-prism-es2/macosx/MacOSXWindowSystemInterface.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/316558947
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/316558947/MacOSXWindowSystemInterface.o ../../modules/graphics/src/main/native-prism-es2/macosx/MacOSXWindowSystemInterface.m

${OBJECTDIR}/_ext/1747355461/WinGLContext.o: ../../modules/graphics/src/main/native-prism-es2/windows/WinGLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1747355461
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1747355461/WinGLContext.o ../../modules/graphics/src/main/native-prism-es2/windows/WinGLContext.c

${OBJECTDIR}/_ext/1747355461/WinGLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/windows/WinGLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1747355461
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1747355461/WinGLDrawable.o ../../modules/graphics/src/main/native-prism-es2/windows/WinGLDrawable.c

${OBJECTDIR}/_ext/1747355461/WinGLFactory.o: ../../modules/graphics/src/main/native-prism-es2/windows/WinGLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1747355461
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1747355461/WinGLFactory.o ../../modules/graphics/src/main/native-prism-es2/windows/WinGLFactory.c

${OBJECTDIR}/_ext/1747355461/WinGLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/windows/WinGLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1747355461
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1747355461/WinGLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/windows/WinGLPixelFormat.c

${OBJECTDIR}/_ext/1092788646/X11GLContext.o: ../../modules/graphics/src/main/native-prism-es2/x11/X11GLContext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092788646
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092788646/X11GLContext.o ../../modules/graphics/src/main/native-prism-es2/x11/X11GLContext.c

${OBJECTDIR}/_ext/1092788646/X11GLDrawable.o: ../../modules/graphics/src/main/native-prism-es2/x11/X11GLDrawable.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092788646
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092788646/X11GLDrawable.o ../../modules/graphics/src/main/native-prism-es2/x11/X11GLDrawable.c

${OBJECTDIR}/_ext/1092788646/X11GLFactory.o: ../../modules/graphics/src/main/native-prism-es2/x11/X11GLFactory.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092788646
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092788646/X11GLFactory.o ../../modules/graphics/src/main/native-prism-es2/x11/X11GLFactory.c

${OBJECTDIR}/_ext/1092788646/X11GLPixelFormat.o: ../../modules/graphics/src/main/native-prism-es2/x11/X11GLPixelFormat.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1092788646
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/prismES2/win  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1092788646/X11GLPixelFormat.o ../../modules/graphics/src/main/native-prism-es2/x11/X11GLPixelFormat.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-es2.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
