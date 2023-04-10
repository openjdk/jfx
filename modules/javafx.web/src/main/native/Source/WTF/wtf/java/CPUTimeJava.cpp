#include "config.h"
#include <wtf/CPUTime.h>

namespace WTF {

std::optional<CPUTime> CPUTime::get()
{
    return std::nullopt;
}

Seconds CPUTime::forCurrentThread()
{
    return Seconds {};
}

}

