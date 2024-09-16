#pragma once

#include <WebCore/HistoryItem.h>
#include <WebCore/PlatformJavaClasses.h>

namespace WebCore {
class HistoryItemClientJava final : public HistoryItemClient {
public:
    static HistoryItemClientJava& singleton();
private:
    HistoryItemClientJava() = default;
    void historyItemChanged(const WebCore::HistoryItem&) final;
};
} // namespace WebCore

