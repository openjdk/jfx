#include "config.h"
#include "ThemeJava.h"
#include <wtf/NeverDestroyed.h>
namespace WebCore {
Theme& Theme::singleton()
{
    static NeverDestroyed<ThemeJava> theme;
    return theme;
}

}
