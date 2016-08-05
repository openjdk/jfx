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
	${OBJECTDIR}/_ext/1944146143/Exports.o \
	${OBJECTDIR}/_ext/1944146143/FilePath.o \
	${OBJECTDIR}/_ext/1944146143/GenericPlatform.o \
	${OBJECTDIR}/_ext/1944146143/Helpers.o \
	${OBJECTDIR}/_ext/1944146143/Java.o \
	${OBJECTDIR}/_ext/1944146143/JavaUserPreferences.o \
	${OBJECTDIR}/_ext/1944146143/JavaVirtualMachine.o \
	${OBJECTDIR}/_ext/1944146143/LinuxPlatform.o \
	${OBJECTDIR}/_ext/1944146143/Lock.o \
	${OBJECTDIR}/_ext/1944146143/Macros.o \
	${OBJECTDIR}/_ext/1944146143/Messages.o \
	${OBJECTDIR}/_ext/1944146143/Package.o \
	${OBJECTDIR}/_ext/1944146143/Platform.o \
	${OBJECTDIR}/_ext/1944146143/PlatformString.o \
	${OBJECTDIR}/_ext/1944146143/PlatformThread.o \
	${OBJECTDIR}/_ext/1944146143/PosixPlatform.o \
	${OBJECTDIR}/_ext/1944146143/PropertyFile.o \
	${OBJECTDIR}/_ext/1944146143/WinLauncher.o \
	${OBJECTDIR}/_ext/1944146143/WindowsPlatform.o \
	${OBJECTDIR}/main.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/cppapplication_1

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/cppapplication_1: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/cppapplication_1 ${OBJECTFILES} ${LDLIBSOPTIONS}

${OBJECTDIR}/_ext/1944146143/Exports.o: ../../../win/windows/Exports.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Exports.o ../../../win/windows/Exports.cpp

${OBJECTDIR}/_ext/1944146143/FilePath.o: ../../../win/windows/FilePath.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/FilePath.o ../../../win/windows/FilePath.cpp

${OBJECTDIR}/_ext/1944146143/GenericPlatform.o: ../../../win/windows/GenericPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/GenericPlatform.o ../../../win/windows/GenericPlatform.cpp

${OBJECTDIR}/_ext/1944146143/Helpers.o: ../../../win/windows/Helpers.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Helpers.o ../../../win/windows/Helpers.cpp

${OBJECTDIR}/_ext/1944146143/Java.o: ../../../win/windows/Java.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Java.o ../../../win/windows/Java.cpp

${OBJECTDIR}/_ext/1944146143/JavaUserPreferences.o: ../../../win/windows/JavaUserPreferences.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/JavaUserPreferences.o ../../../win/windows/JavaUserPreferences.cpp

${OBJECTDIR}/_ext/1944146143/JavaVirtualMachine.o: ../../../win/windows/JavaVirtualMachine.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/JavaVirtualMachine.o ../../../win/windows/JavaVirtualMachine.cpp

${OBJECTDIR}/_ext/1944146143/LinuxPlatform.o: ../../../win/windows/LinuxPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/LinuxPlatform.o ../../../win/windows/LinuxPlatform.cpp

${OBJECTDIR}/_ext/1944146143/Lock.o: ../../../win/windows/Lock.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Lock.o ../../../win/windows/Lock.cpp

${OBJECTDIR}/_ext/1944146143/Macros.o: ../../../win/windows/Macros.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Macros.o ../../../win/windows/Macros.cpp

${OBJECTDIR}/_ext/1944146143/Messages.o: ../../../win/windows/Messages.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Messages.o ../../../win/windows/Messages.cpp

${OBJECTDIR}/_ext/1944146143/Package.o: ../../../win/windows/Package.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Package.o ../../../win/windows/Package.cpp

${OBJECTDIR}/_ext/1944146143/Platform.o: ../../../win/windows/Platform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/Platform.o ../../../win/windows/Platform.cpp

${OBJECTDIR}/_ext/1944146143/PlatformString.o: ../../../win/windows/PlatformString.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/PlatformString.o ../../../win/windows/PlatformString.cpp

${OBJECTDIR}/_ext/1944146143/PlatformThread.o: ../../../win/windows/PlatformThread.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/PlatformThread.o ../../../win/windows/PlatformThread.cpp

${OBJECTDIR}/_ext/1944146143/PosixPlatform.o: ../../../win/windows/PosixPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/PosixPlatform.o ../../../win/windows/PosixPlatform.cpp

${OBJECTDIR}/_ext/1944146143/PropertyFile.o: ../../../win/windows/PropertyFile.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/PropertyFile.o ../../../win/windows/PropertyFile.cpp

${OBJECTDIR}/_ext/1944146143/WinLauncher.o: ../../../win/windows/WinLauncher.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/WinLauncher.o ../../../win/windows/WinLauncher.cpp

${OBJECTDIR}/_ext/1944146143/WindowsPlatform.o: ../../../win/windows/WindowsPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1944146143
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1944146143/WindowsPlatform.o ../../../win/windows/WindowsPlatform.cpp

${OBJECTDIR}/main.o: main.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/main.o main.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/cppapplication_1

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
