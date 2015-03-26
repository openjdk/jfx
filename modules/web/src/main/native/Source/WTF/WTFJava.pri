# -------------------------------------------------------------------
# Project file for WTF
#
# See 'Tools/qmake/README' for an overview of the build system
# -------------------------------------------------------------------

DEFINES += BUILDING_WTF
DEFINES += STATICALLY_LINKED_WITH_WTF

VPATH += $$PWD/wtf

INCLUDEPATH += $$PWD/wtf \
               $$PWD/wtf/java \
               $$PWD/../WebCore/platform

HEADERS += \
    ASCIICType.h \
    Assertions.h \
    Atomics.h \
    AVLTree.h \
    Bitmap.h \
    BitVector.h \
    BloomFilter.h \
    BoundsCheckedPointer.h \
    BumpPointerAllocator.h \
    ByteOrder.h \
    CheckedArithmetic.h \
    Compiler.h \
    CryptographicallyRandomNumber.h \
    CurrentTime.h \
    DateMath.h \
    DecimalNumber.h \
    Decoder.h \
    DataLog.h \ 
    Deque.h \
    DisallowCType.h \
    dtoa.h \
    dtoa/bignum-dtoa.h \
    dtoa/bignum.h \
    dtoa/cached-powers.h \
    dtoa/diy-fp.h \
    dtoa/double-conversion.h \
    dtoa/double.h \
    dtoa/fast-dtoa.h \
    dtoa/fixed-dtoa.h \
    dtoa/strtod.h \
    dtoa/utils.h \
    DynamicAnnotations.h \
    Encoder.h \
    ExportMacros.h \
    FastMalloc.h \
    FeatureDefines.h \
    FilePrintStream.h \
    Forward.h \
    FunctionDispatcher.h \
    Functional.h \
    GetPtr.h \
    GregorianDateTime.h \
    HashCountedSet.h \
    HashFunctions.h \
    HashIterators.h \
    HashMap.h \
    HashSet.h \
    HashTable.h \
    HashTraits.h \
    HexNumber.h \
    ListHashSet.h \
    Locker.h \
    MainThread.h \
    MathExtras.h \
    MD5.h \
    MediaTime.h \
    MessageQueue.h \
    MetaAllocator.h \
    MetaAllocatorHandle.h \
    Noncopyable.h \
    NumberOfCores.h \
    RAMSize.h \
    OSAllocator.h \
    OSRandomSource.h \
    OwnPtr.h \
    OwnPtrCommon.h \
    PackedIntVector.h \
    PageAllocation.h \
    PageAllocationAligned.h \
    PageBlock.h \
    PageReservation.h \
    ParallelJobs.h \
    ParallelJobsGeneric.h \
    ParallelJobsLibdispatch.h \
    ParallelJobsOpenMP.h \
    PassOwnPtr.h \
    PassRefPtr.h \
    Platform.h \
    PossiblyNull.h \
    PrintStream.h \
    ProcessID.h \
    RandomNumber.h \
    RandomNumberSeed.h \
    RawPointer.h \
    RedBlackTree.h \
    RefCounted.h \
    RefCountedLeakCounter.h \
    RefPtr.h \
    RefPtrHashMap.h \
    RetainPtr.h \
    SHA1.h \
    SaturatedArithmetic.h \
    Spectrum.h \
    StackBounds.h \
    StaticConstructors.h \
    StdLibExtras.h \
    StringExtras.h \
    StringHasher.h \
    StringPrintStream.h \
    TCPackedCache.h \
    TCSpinLock.h \
    TCSystemAlloc.h \
    text/ASCIIFastPath.h \
    text/AtomicString.h \
    text/AtomicStringHash.h \
    text/AtomicStringImpl.h \
    text/Base64.h \
    text/CString.h \
    text/IntegerToStringConversion.h \
    text/StringBuffer.h \
    text/StringBuilder.h \
    text/StringConcatenate.h \
    text/StringHash.h \
    text/StringImpl.h \
    text/StringOperators.h \
    text/TextPosition.h \
    text/WTFString.h \
    threads/BinarySemaphore.h \
    Threading.h \
    ThreadingPrimitives.h \
    ThreadSafeRefCounted.h \
    ThreadSpecific.h \
    unicode/CharacterNames.h \
    unicode/Collator.h \
    unicode/UTF8.h \
    ValueCheck.h \
    Vector.h \
    VectorTraits.h \
    VMTags.h \
    WTFThreadData.h \
    WeakPtr.h

unix: HEADERS += ThreadIdentifierDataPthreads.h

SOURCES += \
    Assertions.cpp \
    Atomics.cpp \
    BitVector.cpp \
    CryptographicallyRandomNumber.cpp \
    CurrentTime.cpp \
    CompilationThread.cpp \
    DateMath.cpp \
    DataLog.cpp \
    DecimalNumber.cpp \
    PrintStream.cpp \
    FastBitVector.cpp \
#    cf/RunLoopCF.cpp \
    dtoa.cpp \
    dtoa/bignum-dtoa.cc \
    dtoa/bignum.cc \
    dtoa/cached-powers.cc \
    dtoa/diy-fp.cc \
    dtoa/double-conversion.cc \
    dtoa/fast-dtoa.cc \
    dtoa/fixed-dtoa.cc \
    dtoa/strtod.cc \
    FastMalloc.cpp \
    FilePrintStream.cpp \
    FunctionDispatcher.cpp \
    GregorianDateTime.cpp \
    gobject/GRefPtr.cpp \
    HashTable.cpp \
    MD5.cpp \
    MainThread.cpp \
    MediaTime.cpp \
    MetaAllocator.cpp \
    NumberOfCores.cpp \
    RAMSize.cpp \
    OSRandomSource.cpp \
    PageAllocationAligned.cpp \
    PageBlock.cpp \
    ParallelJobsGeneric.cpp \
    PrintStream.cpp \
    RandomNumber.cpp \
    RefCountedLeakCounter.cpp \
    SHA1.cpp \
    StackBounds.cpp \
    StringPrintStream.cpp \
    SixCharacterHash.cpp \
    TCSystemAlloc.cpp \
    Threading.cpp \
    WTFThreadData.cpp \
    text/AtomicStringTable.cpp \
    text/AtomicString.cpp \
    text/Base64.cpp \
    text/CString.cpp \
    text/StringBuilder.cpp \
    text/StringImpl.cpp \
    text/StringStatics.cpp \
    text/WTFString.cpp \
    unicode/CollatorDefault.cpp \
    unicode/icu/CollatorICU.cpp \
    unicode/UTF8.cpp

contains(DEFINES, WTF_USE_CF=1) {
    SOURCES += \
#       text/cf/AtomicStringCF.cpp \
        text/cf/StringImplCF.cpp \
        text/cf/StringCF.cpp \
#       text/cf/StringViewCF.cpp \
}
    
unix: SOURCES += \
    OSAllocatorPosix.cpp \
    ThreadIdentifierDataPthreads.cpp \
    ThreadingPthreads.cpp

win*|wince*: SOURCES += \
    OSAllocatorWin.cpp \
    ThreadSpecificWin.cpp \
    ThreadingWin.cpp

win32 {
    SOURCES += \
        threads/win/BinarySemaphoreWin.cpp
    INCLUDEPATH += $$PWD/wtf/threads
} else {
    SOURCES += \
        threads/BinarySemaphore.cpp
}


*-g++*:QMAKE_CXXFLAGS_RELEASE -= -O2
*-g++*:QMAKE_CXXFLAGS_RELEASE += -O3

*sh4* {
    QMAKE_CXXFLAGS += -mieee -w
    QMAKE_CFLAGS   += -mieee -w
}
