/*
 * Copyright (C) 2008-2019 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "ExecutableAllocator.h"

#if ENABLE(JIT)

#include "CodeProfiling.h"
#include "ExecutableAllocationFuzz.h"
#include "JSCInlines.h"
#include <wtf/FileSystem.h>
#include <wtf/MetaAllocator.h>
#include <wtf/PageReservation.h>
#include <wtf/ProcessID.h>
#include <wtf/SystemTracing.h>
#include <wtf/WorkQueue.h>

#if OS(DARWIN)
#include <mach/mach_time.h>
#include <sys/mman.h>
#endif

#if PLATFORM(IOS_FAMILY)
#include <wtf/cocoa/Entitlements.h>
#endif

#include "LinkBuffer.h"
#include "MacroAssembler.h"

#if PLATFORM(COCOA)
#define HAVE_REMAP_JIT 1
#endif

#if HAVE(REMAP_JIT)
#if CPU(ARM64) && PLATFORM(IOS_FAMILY)
#define USE_EXECUTE_ONLY_JIT_WRITE_FUNCTION 1
#endif
#endif

#if OS(DARWIN)
#include <mach/mach.h>
extern "C" {
    /* Routine mach_vm_remap */
#ifdef mig_external
    mig_external
#else
    extern
#endif /* mig_external */
    kern_return_t mach_vm_remap
    (
     vm_map_t target_task,
     mach_vm_address_t *target_address,
     mach_vm_size_t size,
     mach_vm_offset_t mask,
     int flags,
     vm_map_t src_task,
     mach_vm_address_t src_address,
     boolean_t copy,
     vm_prot_t *cur_protection,
     vm_prot_t *max_protection,
     vm_inherit_t inheritance
     );
}

#endif

namespace JSC {

using namespace WTF;

#if defined(FIXED_EXECUTABLE_MEMORY_POOL_SIZE_IN_MB) && FIXED_EXECUTABLE_MEMORY_POOL_SIZE_IN_MB > 0
static const size_t fixedExecutableMemoryPoolSize = FIXED_EXECUTABLE_MEMORY_POOL_SIZE_IN_MB * 1024 * 1024;
#elif CPU(ARM)
static const size_t fixedExecutableMemoryPoolSize = 16 * 1024 * 1024;
#elif CPU(ARM64)
static const size_t fixedExecutableMemoryPoolSize = 128 * 1024 * 1024;
#elif CPU(X86_64)
static const size_t fixedExecutableMemoryPoolSize = 1024 * 1024 * 1024;
#else
static const size_t fixedExecutableMemoryPoolSize = 32 * 1024 * 1024;
#endif

#if CPU(ARM)
static const double executablePoolReservationFraction = 0.15;
#else
static const double executablePoolReservationFraction = 0.25;
#endif

#if ENABLE(SEPARATED_WX_HEAP)
JS_EXPORT_PRIVATE bool useFastPermisionsJITCopy { false };
JS_EXPORT_PRIVATE JITWriteSeparateHeapsFunction jitWriteSeparateHeapsFunction;
#endif

#if !USE(EXECUTE_ONLY_JIT_WRITE_FUNCTION) && HAVE(REMAP_JIT)
static uintptr_t startOfFixedWritableMemoryPool;
#endif

class FixedVMPoolExecutableAllocator;
static FixedVMPoolExecutableAllocator* allocator = nullptr;

static bool s_isJITEnabled = true;
static bool isJITEnabled()
{
#if PLATFORM(IOS_FAMILY) && (CPU(ARM64) || CPU(ARM))
    return processHasEntitlement("dynamic-codesigning") && s_isJITEnabled;
#else
    return s_isJITEnabled;
#endif
}

void ExecutableAllocator::setJITEnabled(bool enabled)
{
    ASSERT(!allocator);
    if (s_isJITEnabled == enabled)
        return;

    s_isJITEnabled = enabled;

#if PLATFORM(IOS_FAMILY) && (CPU(ARM64) || CPU(ARM))
    if (!enabled) {
        constexpr size_t size = 1;
        constexpr int protection = PROT_READ | PROT_WRITE | PROT_EXEC;
        constexpr int flags = MAP_PRIVATE | MAP_ANON | MAP_JIT;
        constexpr int fd = OSAllocator::JSJITCodePages;
        void* allocation = mmap(nullptr, size, protection, flags, fd, 0);
        const void* executableMemoryAllocationFailure = reinterpret_cast<void*>(-1);
        RELEASE_ASSERT_WITH_MESSAGE(allocation && allocation != executableMemoryAllocationFailure, "We should not have allocated executable memory before disabling the JIT.");
        RELEASE_ASSERT_WITH_MESSAGE(!munmap(allocation, size), "Unmapping executable memory should succeed so we do not have any executable memory in the address space");
        RELEASE_ASSERT_WITH_MESSAGE(mmap(nullptr, size, protection, flags, fd, 0) == executableMemoryAllocationFailure, "Allocating executable memory should fail after setJITEnabled(false) is called.");
    }
#endif
}

class FixedVMPoolExecutableAllocator final : public MetaAllocator {
    WTF_MAKE_FAST_ALLOCATED;
public:
    FixedVMPoolExecutableAllocator()
        : MetaAllocator(jitAllocationGranule) // round up all allocations to 32 bytes
    {
        if (!isJITEnabled())
            return;

        size_t reservationSize;
        if (Options::jitMemoryReservationSize())
            reservationSize = Options::jitMemoryReservationSize();
        else
            reservationSize = fixedExecutableMemoryPoolSize;
        reservationSize = std::max(roundUpToMultipleOf(pageSize(), reservationSize), pageSize() * 2);

        auto tryCreatePageReservation = [] (size_t reservationSize) {
#if OS(LINUX)
            // If we use uncommitted reservation, mmap operation is recorded with small page size in perf command's output.
            // This makes the following JIT code logging broken and some of JIT code is not recorded correctly.
            // To avoid this problem, we use committed reservation if we need perf JITDump logging.
            if (Options::logJITCodeForPerf())
                return PageReservation::reserveAndCommitWithGuardPages(reservationSize, OSAllocator::JSJITCodePages, EXECUTABLE_POOL_WRITABLE, true);
#endif
            return PageReservation::reserveWithGuardPages(reservationSize, OSAllocator::JSJITCodePages, EXECUTABLE_POOL_WRITABLE, true);
        };

        m_reservation = tryCreatePageReservation(reservationSize);
        if (m_reservation) {
            ASSERT(m_reservation.size() == reservationSize);
            void* reservationBase = m_reservation.base();

#if ENABLE(FAST_JIT_PERMISSIONS) && !ENABLE(SEPARATED_WX_HEAP)
            RELEASE_ASSERT(os_thread_self_restrict_rwx_is_supported());
            os_thread_self_restrict_rwx_to_rx();

#else // not ENABLE(FAST_JIT_PERMISSIONS) or ENABLE(SEPARATED_WX_HEAP)
#if ENABLE(FAST_JIT_PERMISSIONS)
            if (os_thread_self_restrict_rwx_is_supported()) {
                useFastPermisionsJITCopy = true;
                os_thread_self_restrict_rwx_to_rx();
            } else
#endif
            if (Options::useSeparatedWXHeap()) {
                // First page of our JIT allocation is reserved.
                ASSERT(reservationSize >= pageSize() * 2);
                reservationBase = (void*)((uintptr_t)reservationBase + pageSize());
                reservationSize -= pageSize();
                initializeSeparatedWXHeaps(m_reservation.base(), pageSize(), reservationBase, reservationSize);
            }
#endif // not ENABLE(FAST_JIT_PERMISSIONS) or ENABLE(SEPARATED_WX_HEAP)

            addFreshFreeSpace(reservationBase, reservationSize);

            ASSERT(bytesReserved() == reservationSize); // Since our executable memory is fixed-sized, bytesReserved is never changed after initialization.

            void* reservationEnd = reinterpret_cast<uint8_t*>(reservationBase) + reservationSize;

            m_memoryStart = MacroAssemblerCodePtr<ExecutableMemoryPtrTag>(tagCodePtr<ExecutableMemoryPtrTag>(reservationBase));
            m_memoryEnd = MacroAssemblerCodePtr<ExecutableMemoryPtrTag>(tagCodePtr<ExecutableMemoryPtrTag>(reservationEnd));
        }
    }

    virtual ~FixedVMPoolExecutableAllocator();

    void* memoryStart() { return m_memoryStart.untaggedExecutableAddress(); }
    void* memoryEnd() { return m_memoryEnd.untaggedExecutableAddress(); }
    bool isJITPC(void* pc) { return memoryStart() <= pc && pc < memoryEnd(); }

protected:
    FreeSpacePtr allocateNewSpace(size_t&) override
    {
        // We're operating in a fixed pool, so new allocation is always prohibited.
        return nullptr;
    }

    void notifyNeedPage(void* page, size_t count) override
    {
#if USE(MADV_FREE_FOR_JIT_MEMORY)
        UNUSED_PARAM(page);
        UNUSED_PARAM(count);
#else
        m_reservation.commit(page, pageSize() * count);
#endif
    }

    void notifyPageIsFree(void* page, size_t count) override
    {
#if USE(MADV_FREE_FOR_JIT_MEMORY)
        for (;;) {
            int result = madvise(page, pageSize() * count, MADV_FREE);
            if (!result)
                return;
            ASSERT(result == -1);
            if (errno != EAGAIN) {
                RELEASE_ASSERT_NOT_REACHED(); // In debug mode, this should be a hard failure.
                break; // In release mode, we should just ignore the error - not returning memory to the OS is better than crashing, especially since we _will_ be able to reuse the memory internally anyway.
            }
        }
#else
        m_reservation.decommit(page, pageSize() * count);
#endif
    }

private:
#if OS(DARWIN) && HAVE(REMAP_JIT)
    void initializeSeparatedWXHeaps(void* stubBase, size_t stubSize, void* jitBase, size_t jitSize)
    {
        mach_vm_address_t writableAddr = 0;

        // Create a second mapping of the JIT region at a random address.
        vm_prot_t cur, max;
        int remapFlags = VM_FLAGS_ANYWHERE;
#if defined(VM_FLAGS_RANDOM_ADDR)
        remapFlags |= VM_FLAGS_RANDOM_ADDR;
#endif
        kern_return_t ret = mach_vm_remap(mach_task_self(), &writableAddr, jitSize, 0,
            remapFlags,
            mach_task_self(), (mach_vm_address_t)jitBase, FALSE,
            &cur, &max, VM_INHERIT_DEFAULT);

        bool remapSucceeded = (ret == KERN_SUCCESS);
        if (!remapSucceeded)
            return;

        // Assemble a thunk that will serve as the means for writing into the JIT region.
        MacroAssemblerCodeRef<JITThunkPtrTag> writeThunk = jitWriteThunkGenerator(reinterpret_cast<void*>(writableAddr), stubBase, stubSize);

        int result = 0;

#if USE(EXECUTE_ONLY_JIT_WRITE_FUNCTION)
        // Prevent reading the write thunk code.
        result = vm_protect(mach_task_self(), reinterpret_cast<vm_address_t>(stubBase), stubSize, true, VM_PROT_EXECUTE);
        RELEASE_ASSERT(!result);
#endif

        // Prevent writing into the executable JIT mapping.
        result = vm_protect(mach_task_self(), reinterpret_cast<vm_address_t>(jitBase), jitSize, true, VM_PROT_READ | VM_PROT_EXECUTE);
        RELEASE_ASSERT(!result);

        // Prevent execution in the writable JIT mapping.
        result = vm_protect(mach_task_self(), static_cast<vm_address_t>(writableAddr), jitSize, true, VM_PROT_READ | VM_PROT_WRITE);
        RELEASE_ASSERT(!result);

        // Zero out writableAddr to avoid leaking the address of the writable mapping.
        memset_s(&writableAddr, sizeof(writableAddr), 0, sizeof(writableAddr));

#if ENABLE(SEPARATED_WX_HEAP)
        jitWriteSeparateHeapsFunction = reinterpret_cast<JITWriteSeparateHeapsFunction>(writeThunk.code().executableAddress());
#endif
    }

#if CPU(ARM64) && USE(EXECUTE_ONLY_JIT_WRITE_FUNCTION)
    MacroAssemblerCodeRef<JITThunkPtrTag> jitWriteThunkGenerator(void* writableAddr, void* stubBase, size_t stubSize)
    {
        using namespace ARM64Registers;
        using TrustedImm32 = MacroAssembler::TrustedImm32;

        MacroAssembler jit;

        jit.tagReturnAddress();
        jit.move(MacroAssembler::TrustedImmPtr(writableAddr), x7);
        jit.addPtr(x7, x0);

        jit.move(x0, x3);
        MacroAssembler::Jump smallCopy = jit.branch64(MacroAssembler::Below, x2, MacroAssembler::TrustedImm64(64));

        jit.add64(TrustedImm32(32), x3);
        jit.and64(TrustedImm32(-32), x3);
        jit.loadPair64(x1, x12, x13);
        jit.loadPair64(x1, TrustedImm32(16), x14, x15);
        jit.sub64(x3, x0, x5);
        jit.addPtr(x5, x1);

        jit.loadPair64(x1, x8, x9);
        jit.loadPair64(x1, TrustedImm32(16), x10, x11);
        jit.add64(TrustedImm32(32), x1);
        jit.sub64(x5, x2);
        jit.storePair64(x12, x13, x0);
        jit.storePair64(x14, x15, x0, TrustedImm32(16));
        MacroAssembler::Jump cleanup = jit.branchSub64(MacroAssembler::BelowOrEqual, TrustedImm32(64), x2);

        MacroAssembler::Label copyLoop = jit.label();
        jit.storePair64WithNonTemporalAccess(x8, x9, x3);
        jit.storePair64WithNonTemporalAccess(x10, x11, x3, TrustedImm32(16));
        jit.add64(TrustedImm32(32), x3);
        jit.loadPair64WithNonTemporalAccess(x1, x8, x9);
        jit.loadPair64WithNonTemporalAccess(x1, TrustedImm32(16), x10, x11);
        jit.add64(TrustedImm32(32), x1);
        jit.branchSub64(MacroAssembler::Above, TrustedImm32(32), x2).linkTo(copyLoop, &jit);

        cleanup.link(&jit);
        jit.add64(x2, x1);
        jit.loadPair64(x1, x12, x13);
        jit.loadPair64(x1, TrustedImm32(16), x14, x15);
        jit.storePair64(x8, x9, x3);
        jit.storePair64(x10, x11, x3, TrustedImm32(16));
        jit.addPtr(x2, x3);
        jit.storePair64(x12, x13, x3, TrustedImm32(32));
        jit.storePair64(x14, x15, x3, TrustedImm32(48));
        jit.ret();

        MacroAssembler::Label local0 = jit.label();
        jit.load64(x1, PostIndex(8), x6);
        jit.store64(x6, x3, PostIndex(8));
        smallCopy.link(&jit);
        jit.branchSub64(MacroAssembler::AboveOrEqual, TrustedImm32(8), x2).linkTo(local0, &jit);
        MacroAssembler::Jump local2 = jit.branchAdd64(MacroAssembler::Equal, TrustedImm32(8), x2);
        MacroAssembler::Label local1 = jit.label();
        jit.load8(x1, PostIndex(1), x6);
        jit.store8(x6, x3, PostIndex(1));
        jit.branchSub64(MacroAssembler::NotEqual, TrustedImm32(1), x2).linkTo(local1, &jit);
        local2.link(&jit);
        jit.ret();

        auto stubBaseCodePtr = MacroAssemblerCodePtr<LinkBufferPtrTag>(tagCodePtr<LinkBufferPtrTag>(stubBase));
        LinkBuffer linkBuffer(jit, stubBaseCodePtr, stubSize);
        // We don't use FINALIZE_CODE() for two reasons.
        // The first is that we don't want the writeable address, as disassembled instructions,
        // to appear in the console or anywhere in memory, via the PrintStream buffer.
        // The second is we can't guarantee that the code is readable when using the
        // asyncDisassembly option as our caller will set our pages execute only.
        return linkBuffer.finalizeCodeWithoutDisassembly<JITThunkPtrTag>();
    }
#else // not CPU(ARM64) && USE(EXECUTE_ONLY_JIT_WRITE_FUNCTION)
    static void genericWriteToJITRegion(off_t offset, const void* data, size_t dataSize)
    {
        memcpy((void*)(startOfFixedWritableMemoryPool + offset), data, dataSize);
    }

    MacroAssemblerCodeRef<JITThunkPtrTag> jitWriteThunkGenerator(void* address, void*, size_t)
    {
        startOfFixedWritableMemoryPool = reinterpret_cast<uintptr_t>(address);
        void* function = reinterpret_cast<void*>(&genericWriteToJITRegion);
#if CPU(ARM_THUMB2)
        // Handle thumb offset
        uintptr_t functionAsInt = reinterpret_cast<uintptr_t>(function);
        functionAsInt -= 1;
        function = reinterpret_cast<void*>(functionAsInt);
#endif
        auto codePtr = MacroAssemblerCodePtr<JITThunkPtrTag>(tagCFunctionPtr<JITThunkPtrTag>(function));
        return MacroAssemblerCodeRef<JITThunkPtrTag>::createSelfManagedCodeRef(codePtr);
    }
#endif // CPU(ARM64) && USE(EXECUTE_ONLY_JIT_WRITE_FUNCTION)

#else // OS(DARWIN) && HAVE(REMAP_JIT)
    void initializeSeparatedWXHeaps(void*, size_t, void*, size_t)
    {
    }
#endif

private:
    PageReservation m_reservation;
    MacroAssemblerCodePtr<ExecutableMemoryPtrTag> m_memoryStart;
    MacroAssemblerCodePtr<ExecutableMemoryPtrTag> m_memoryEnd;
};

FixedVMPoolExecutableAllocator::~FixedVMPoolExecutableAllocator()
{
    m_reservation.deallocate();
}

void ExecutableAllocator::initializeUnderlyingAllocator()
{
    ASSERT(!allocator);
    allocator = new FixedVMPoolExecutableAllocator();
    CodeProfiling::notifyAllocator(allocator);
}

bool ExecutableAllocator::isValid() const
{
    if (!allocator)
        return Base::isValid();
    return !!allocator->bytesReserved();
}

bool ExecutableAllocator::underMemoryPressure()
{
    if (!allocator)
        return Base::underMemoryPressure();
    return allocator->bytesAllocated() > allocator->bytesReserved() / 2;
}

double ExecutableAllocator::memoryPressureMultiplier(size_t addedMemoryUsage)
{
    if (!allocator)
        return Base::memoryPressureMultiplier(addedMemoryUsage);
    ASSERT(allocator->bytesAllocated() <= allocator->bytesReserved());
    size_t bytesAllocated = allocator->bytesAllocated() + addedMemoryUsage;
    size_t bytesAvailable = static_cast<size_t>(
        allocator->bytesReserved() * (1 - executablePoolReservationFraction));
    if (bytesAllocated >= bytesAvailable)
        bytesAllocated = bytesAvailable;
    double result = 1.0;
    size_t divisor = bytesAvailable - bytesAllocated;
    if (divisor)
        result = static_cast<double>(bytesAvailable) / divisor;
    if (result < 1.0)
        result = 1.0;
    return result;
}

RefPtr<ExecutableMemoryHandle> ExecutableAllocator::allocate(size_t sizeInBytes, void* ownerUID, JITCompilationEffort effort)
{
    if (!allocator)
        return Base::allocate(sizeInBytes, ownerUID, effort);
    if (Options::logExecutableAllocation()) {
        MetaAllocator::Statistics stats = allocator->currentStatistics();
        dataLog("Allocating ", sizeInBytes, " bytes of executable memory with ", stats.bytesAllocated, " bytes allocated, ", stats.bytesReserved, " bytes reserved, and ", stats.bytesCommitted, " committed.\n");
    }

    if (effort != JITCompilationCanFail && Options::reportMustSucceedExecutableAllocations()) {
        dataLog("Allocating ", sizeInBytes, " bytes of executable memory with JITCompilationMustSucceed.\n");
        WTFReportBacktrace();
    }

    if (effort == JITCompilationCanFail
        && doExecutableAllocationFuzzingIfEnabled() == PretendToFailExecutableAllocation)
        return nullptr;

    if (effort == JITCompilationCanFail) {
        // Don't allow allocations if we are down to reserve.
        size_t bytesAllocated = allocator->bytesAllocated() + sizeInBytes;
        size_t bytesAvailable = static_cast<size_t>(
            allocator->bytesReserved() * (1 - executablePoolReservationFraction));
        if (bytesAllocated > bytesAvailable) {
            if (Options::logExecutableAllocation())
                dataLog("Allocation failed because bytes allocated ", bytesAllocated,  " > ", bytesAvailable, " bytes available.\n");
            return nullptr;
        }
    }

    RefPtr<ExecutableMemoryHandle> result = allocator->allocate(sizeInBytes, ownerUID);
    if (!result) {
        if (effort != JITCompilationCanFail) {
            dataLog("Ran out of executable memory while allocating ", sizeInBytes, " bytes.\n");
            CRASH();
        }
        return nullptr;
    }

#if CPU(ARM64E)
    void* start = allocator->memoryStart();
    void* end = allocator->memoryEnd();
    void* resultStart = result->start().untaggedPtr();
    void* resultEnd = result->end().untaggedPtr();
    RELEASE_ASSERT(start <= resultStart && resultStart < end);
    RELEASE_ASSERT(start < resultEnd && resultEnd <= end);
#endif
    return result;
}

bool ExecutableAllocator::isValidExecutableMemory(const AbstractLocker& locker, void* address)
{
    if (!allocator)
        return Base::isValidExecutableMemory(locker, address);
    return allocator->isInAllocatedMemory(locker, address);
}

Lock& ExecutableAllocator::getLock() const
{
    if (!allocator)
        return Base::getLock();
    return allocator->getLock();
}

size_t ExecutableAllocator::committedByteCount()
{
    if (!allocator)
        return Base::committedByteCount();
    return allocator->bytesCommitted();
}

#if ENABLE(META_ALLOCATOR_PROFILE)
void ExecutableAllocator::dumpProfile()
{
    if (!allocator)
        return;
    allocator->dumpProfile();
}
#endif

void* startOfFixedExecutableMemoryPoolImpl()
{
    if (!allocator)
        return nullptr;
    return allocator->memoryStart();
}

void* endOfFixedExecutableMemoryPoolImpl()
{
    if (!allocator)
        return nullptr;
    return allocator->memoryEnd();
}

bool isJITPC(void* pc)
{
    return allocator && allocator->isJITPC(pc);
}

void dumpJITMemory(const void* dst, const void* src, size_t size)
{
    ASSERT(Options::dumpJITMemoryPath());

#if OS(DARWIN)
    static int fd = -1;
    static uint8_t* buffer;
    static constexpr size_t bufferSize = fixedExecutableMemoryPoolSize;
    static size_t offset = 0;
    static Lock dumpJITMemoryLock;
    static bool needsToFlush = false;
    static auto flush = [](const AbstractLocker&) {
        if (fd == -1) {
            String path = Options::dumpJITMemoryPath();
            path = path.replace("%pid", String::number(getCurrentProcessID()));
            fd = open(FileSystem::fileSystemRepresentation(path).data(), O_CREAT | O_TRUNC | O_APPEND | O_WRONLY | O_EXLOCK | O_NONBLOCK, 0666);
            RELEASE_ASSERT(fd != -1);
        }
        write(fd, buffer, offset);
        offset = 0;
        needsToFlush = false;
    };

    static std::once_flag once;
    static LazyNeverDestroyed<Ref<WorkQueue>> flushQueue;
    std::call_once(once, [] {
        buffer = bitwise_cast<uint8_t*>(malloc(bufferSize));
        flushQueue.construct(WorkQueue::create("jsc.dumpJITMemory.queue", WorkQueue::Type::Serial, WorkQueue::QOS::Background));
        std::atexit([] {
            LockHolder locker(dumpJITMemoryLock);
            flush(locker);
            close(fd);
            fd = -1;
        });
    });

    static auto enqueueFlush = [](const AbstractLocker&) {
        if (needsToFlush)
            return;

        needsToFlush = true;
        flushQueue.get()->dispatchAfter(Seconds(Options::dumpJITMemoryFlushInterval()), [] {
            LockHolder locker(dumpJITMemoryLock);
            if (!needsToFlush)
                return;
            flush(locker);
        });
    };

    static auto write = [](const AbstractLocker& locker, const void* src, size_t size) {
        if (UNLIKELY(offset + size > bufferSize))
            flush(locker);
        memcpy(buffer + offset, src, size);
        offset += size;
        enqueueFlush(locker);
    };

    LockHolder locker(dumpJITMemoryLock);
    uint64_t time = mach_absolute_time();
    uint64_t dst64 = bitwise_cast<uintptr_t>(dst);
    uint64_t size64 = size;
    TraceScope(DumpJITMemoryStart, DumpJITMemoryStop, time, dst64, size64);
    write(locker, &time, sizeof(time));
    write(locker, &dst64, sizeof(dst64));
    write(locker, &size64, sizeof(size64));
    write(locker, src, size);
#else
    UNUSED_PARAM(dst);
    UNUSED_PARAM(src);
    UNUSED_PARAM(size);
    RELEASE_ASSERT_NOT_REACHED();
#endif
}

} // namespace JSC

#endif // ENABLE(JIT)

namespace JSC {

static ExecutableAllocator* executableAllocator;

void ExecutableAllocator::initialize()
{
    executableAllocator = new ExecutableAllocator;
}

ExecutableAllocator& ExecutableAllocator::singleton()
{
    ASSERT(executableAllocator);
    return *executableAllocator;
}

} // namespace JSC
