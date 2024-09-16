#include "HistoryItemClientJava.h"

namespace WebCore {

HistoryItemClientJava& HistoryItemClientJava::singleton()
{
    static NeverDestroyed<Ref<HistoryItemClientJava>> client { adoptRef(*new HistoryItemClientJava) };
    return client.get().get();
}
void historyItemChangedImpl(const HistoryItem& item) {
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID notifyItemChangedMID = initMethod(env, getJEntryClass(), "notifyItemChanged", "()V");
    if (item.hostObject()) {
        env->CallVoidMethod(item.hostObject(), notifyItemChangedMID);
        WTF::CheckAndClearException(env);
    }
}
void HistoryItemClientJava::historyItemChanged(const WebCore::HistoryItem& item)
{
   historyItemChangedImpl(item);
}
}//Webcore
