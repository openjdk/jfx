#ifdef ANDROID_NDK

#include "wm/LensWindowManager.h"
#include "com_sun_glass_ui_lens_LensApplication.h"
#include "androidLens.h"

jboolean lens_input_initialize(JNIEnv *env) {
    uint32_t flags = 0;
    flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_MULTITOUCH;
    glass_application_notifyDeviceEvent(env, flags, 1);
    return JNI_TRUE;
}

void lens_input_shutdown() {    
    JavaVM *glass_vm = glass_application_GetVM();
    (*glass_vm)->DetachCurrentThread(glass_vm);    
}

#endif