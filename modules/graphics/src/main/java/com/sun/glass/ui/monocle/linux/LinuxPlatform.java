package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.monocle.NativeCursor;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativeScreen;
import com.sun.glass.ui.monocle.NullCursor;
import com.sun.glass.ui.monocle.input.InputDeviceRegistry;
import com.sun.glass.utils.NativeLibLoader;

/** LinuxPlatform matches any Linux system */
public class LinuxPlatform extends NativePlatform {

    public LinuxPlatform() {
        NativeLibLoader.loadLibrary("glass_monocle");
    }

    @Override
    protected InputDeviceRegistry createInputDeviceRegistry() {
        return new LinuxInputDeviceRegistry(false);
    }

    @Override
    protected NativeCursor createCursor() {
        return new NullCursor();
    }

    @Override
    protected NativeScreen createScreen() {
        return new FBDevScreen();
    }
}
