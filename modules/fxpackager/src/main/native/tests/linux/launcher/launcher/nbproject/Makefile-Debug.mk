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
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/404549893/launcher.o


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
LDLIBSOPTIONS=-ldl

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk /home/cbensen/src/projects/packager/JavaFXSceneBuilder2.0/launcher

/home/cbensen/src/projects/packager/JavaFXSceneBuilder2.0/launcher: ${OBJECTFILES}
	${MKDIR} -p /home/cbensen/src/projects/packager/JavaFXSceneBuilder2.0
	${LINK.cc} -o /home/cbensen/src/projects/packager/JavaFXSceneBuilder2.0/launcher ${OBJECTFILES} ${LDLIBSOPTIONS}

${OBJECTDIR}/_ext/404549893/launcher.o: ../../../../launcher/linux/launcher.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/404549893
	${RM} "$@.d"
	$(COMPILE.cc) -g -DDEBUG -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/404549893/launcher.o ../../../../launcher/linux/launcher.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} /home/cbensen/src/projects/packager/JavaFXSceneBuilder2.0/launcher

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
