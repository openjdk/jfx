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
	${OBJECTDIR}/_ext/1063624794/ImageLoader.o \
	${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o \
	${OBJECTDIR}/_ext/1063624794/jni_utils.o \
	${OBJECTDIR}/_ext/1742650920/jpegloader.o \
	${OBJECTDIR}/_ext/1433180815/jcapimin.o \
	${OBJECTDIR}/_ext/1433180815/jcapistd.o \
	${OBJECTDIR}/_ext/1433180815/jccoefct.o \
	${OBJECTDIR}/_ext/1433180815/jccolor.o \
	${OBJECTDIR}/_ext/1433180815/jcdctmgr.o \
	${OBJECTDIR}/_ext/1433180815/jchuff.o \
	${OBJECTDIR}/_ext/1433180815/jcinit.o \
	${OBJECTDIR}/_ext/1433180815/jcmainct.o \
	${OBJECTDIR}/_ext/1433180815/jcmarker.o \
	${OBJECTDIR}/_ext/1433180815/jcmaster.o \
	${OBJECTDIR}/_ext/1433180815/jcomapi.o \
	${OBJECTDIR}/_ext/1433180815/jcparam.o \
	${OBJECTDIR}/_ext/1433180815/jcprepct.o \
	${OBJECTDIR}/_ext/1433180815/jcsample.o \
	${OBJECTDIR}/_ext/1433180815/jctrans.o \
	${OBJECTDIR}/_ext/1433180815/jdapimin.o \
	${OBJECTDIR}/_ext/1433180815/jdapistd.o \
	${OBJECTDIR}/_ext/1433180815/jdcoefct.o \
	${OBJECTDIR}/_ext/1433180815/jdcolor.o \
	${OBJECTDIR}/_ext/1433180815/jddctmgr.o \
	${OBJECTDIR}/_ext/1433180815/jdhuff.o \
	${OBJECTDIR}/_ext/1433180815/jdinput.o \
	${OBJECTDIR}/_ext/1433180815/jdmainct.o \
	${OBJECTDIR}/_ext/1433180815/jdmarker.o \
	${OBJECTDIR}/_ext/1433180815/jdmaster.o \
	${OBJECTDIR}/_ext/1433180815/jdmerge.o \
	${OBJECTDIR}/_ext/1433180815/jdpostct.o \
	${OBJECTDIR}/_ext/1433180815/jdsample.o \
	${OBJECTDIR}/_ext/1433180815/jdtrans.o \
	${OBJECTDIR}/_ext/1433180815/jerror.o \
	${OBJECTDIR}/_ext/1433180815/jfdctflt.o \
	${OBJECTDIR}/_ext/1433180815/jfdctfst.o \
	${OBJECTDIR}/_ext/1433180815/jfdctint.o \
	${OBJECTDIR}/_ext/1433180815/jidctflt.o \
	${OBJECTDIR}/_ext/1433180815/jidctfst.o \
	${OBJECTDIR}/_ext/1433180815/jidctint.o \
	${OBJECTDIR}/_ext/1433180815/jmemmgr.o \
	${OBJECTDIR}/_ext/1433180815/jmemnobs.o \
	${OBJECTDIR}/_ext/1433180815/jquant1.o \
	${OBJECTDIR}/_ext/1433180815/jquant2.o \
	${OBJECTDIR}/_ext/1433180815/jutils.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared

${OBJECTDIR}/_ext/1063624794/ImageLoader.o: ../../modules/graphics/src/main/native-iio/ios/ImageLoader.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1063624794/ImageLoader.o ../../modules/graphics/src/main/native-iio/ios/ImageLoader.m

${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o: ../../modules/graphics/src/main/native-iio/ios/com_sun_javafx_iio_ios_IosImageLoader.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o ../../modules/graphics/src/main/native-iio/ios/com_sun_javafx_iio_ios_IosImageLoader.m

${OBJECTDIR}/_ext/1063624794/jni_utils.o: ../../modules/graphics/src/main/native-iio/ios/jni_utils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1063624794/jni_utils.o ../../modules/graphics/src/main/native-iio/ios/jni_utils.c

${OBJECTDIR}/_ext/1742650920/jpegloader.o: ../../modules/graphics/src/main/native-iio/jpegloader.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1742650920
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1742650920/jpegloader.o ../../modules/graphics/src/main/native-iio/jpegloader.c

${OBJECTDIR}/_ext/1433180815/jcapimin.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcapimin.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcapimin.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcapimin.c

${OBJECTDIR}/_ext/1433180815/jcapistd.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcapistd.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcapistd.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcapistd.c

${OBJECTDIR}/_ext/1433180815/jccoefct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jccoefct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jccoefct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jccoefct.c

${OBJECTDIR}/_ext/1433180815/jccolor.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jccolor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jccolor.o ../../modules/graphics/src/main/native-iio/libjpeg7/jccolor.c

${OBJECTDIR}/_ext/1433180815/jcdctmgr.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcdctmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcdctmgr.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcdctmgr.c

${OBJECTDIR}/_ext/1433180815/jchuff.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jchuff.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jchuff.o ../../modules/graphics/src/main/native-iio/libjpeg7/jchuff.c

${OBJECTDIR}/_ext/1433180815/jcinit.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcinit.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcinit.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcinit.c

${OBJECTDIR}/_ext/1433180815/jcmainct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcmainct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcmainct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcmainct.c

${OBJECTDIR}/_ext/1433180815/jcmarker.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcmarker.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcmarker.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcmarker.c

${OBJECTDIR}/_ext/1433180815/jcmaster.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcmaster.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcmaster.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcmaster.c

${OBJECTDIR}/_ext/1433180815/jcomapi.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcomapi.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcomapi.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcomapi.c

${OBJECTDIR}/_ext/1433180815/jcparam.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcparam.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcparam.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcparam.c

${OBJECTDIR}/_ext/1433180815/jcprepct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcprepct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcprepct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcprepct.c

${OBJECTDIR}/_ext/1433180815/jcsample.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jcsample.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jcsample.o ../../modules/graphics/src/main/native-iio/libjpeg7/jcsample.c

${OBJECTDIR}/_ext/1433180815/jctrans.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jctrans.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jctrans.o ../../modules/graphics/src/main/native-iio/libjpeg7/jctrans.c

${OBJECTDIR}/_ext/1433180815/jdapimin.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdapimin.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdapimin.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdapimin.c

${OBJECTDIR}/_ext/1433180815/jdapistd.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdapistd.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdapistd.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdapistd.c

${OBJECTDIR}/_ext/1433180815/jdcoefct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdcoefct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdcoefct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdcoefct.c

${OBJECTDIR}/_ext/1433180815/jdcolor.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdcolor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdcolor.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdcolor.c

${OBJECTDIR}/_ext/1433180815/jddctmgr.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jddctmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jddctmgr.o ../../modules/graphics/src/main/native-iio/libjpeg7/jddctmgr.c

${OBJECTDIR}/_ext/1433180815/jdhuff.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdhuff.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdhuff.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdhuff.c

${OBJECTDIR}/_ext/1433180815/jdinput.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdinput.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdinput.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdinput.c

${OBJECTDIR}/_ext/1433180815/jdmainct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdmainct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdmainct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdmainct.c

${OBJECTDIR}/_ext/1433180815/jdmarker.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdmarker.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdmarker.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdmarker.c

${OBJECTDIR}/_ext/1433180815/jdmaster.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdmaster.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdmaster.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdmaster.c

${OBJECTDIR}/_ext/1433180815/jdmerge.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdmerge.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdmerge.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdmerge.c

${OBJECTDIR}/_ext/1433180815/jdpostct.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdpostct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdpostct.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdpostct.c

${OBJECTDIR}/_ext/1433180815/jdsample.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdsample.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdsample.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdsample.c

${OBJECTDIR}/_ext/1433180815/jdtrans.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jdtrans.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jdtrans.o ../../modules/graphics/src/main/native-iio/libjpeg7/jdtrans.c

${OBJECTDIR}/_ext/1433180815/jerror.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jerror.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jerror.o ../../modules/graphics/src/main/native-iio/libjpeg7/jerror.c

${OBJECTDIR}/_ext/1433180815/jfdctflt.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctflt.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jfdctflt.o ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctflt.c

${OBJECTDIR}/_ext/1433180815/jfdctfst.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctfst.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jfdctfst.o ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctfst.c

${OBJECTDIR}/_ext/1433180815/jfdctint.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctint.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jfdctint.o ../../modules/graphics/src/main/native-iio/libjpeg7/jfdctint.c

${OBJECTDIR}/_ext/1433180815/jidctflt.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jidctflt.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jidctflt.o ../../modules/graphics/src/main/native-iio/libjpeg7/jidctflt.c

${OBJECTDIR}/_ext/1433180815/jidctfst.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jidctfst.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jidctfst.o ../../modules/graphics/src/main/native-iio/libjpeg7/jidctfst.c

${OBJECTDIR}/_ext/1433180815/jidctint.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jidctint.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jidctint.o ../../modules/graphics/src/main/native-iio/libjpeg7/jidctint.c

${OBJECTDIR}/_ext/1433180815/jmemmgr.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jmemmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jmemmgr.o ../../modules/graphics/src/main/native-iio/libjpeg7/jmemmgr.c

${OBJECTDIR}/_ext/1433180815/jmemnobs.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jmemnobs.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jmemnobs.o ../../modules/graphics/src/main/native-iio/libjpeg7/jmemnobs.c

${OBJECTDIR}/_ext/1433180815/jquant1.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jquant1.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jquant1.o ../../modules/graphics/src/main/native-iio/libjpeg7/jquant1.c

${OBJECTDIR}/_ext/1433180815/jquant2.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jquant2.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jquant2.o ../../modules/graphics/src/main/native-iio/libjpeg7/jquant2.c

${OBJECTDIR}/_ext/1433180815/jutils.o: ../../modules/graphics/src/main/native-iio/libjpeg7/jutils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} $@.d
	$(COMPILE.c) -g -I../../modules/graphics/build/generated-src/headers/iio/win  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1433180815/jutils.o ../../modules/graphics/src/main/native-iio/libjpeg7/jutils.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
