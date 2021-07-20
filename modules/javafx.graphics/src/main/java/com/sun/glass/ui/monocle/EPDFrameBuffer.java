/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.EPDSystem.FbVarScreenInfo;
import com.sun.glass.ui.monocle.EPDSystem.IntStructure;
import com.sun.glass.ui.monocle.EPDSystem.MxcfbUpdateData;
import com.sun.glass.ui.monocle.EPDSystem.MxcfbWaveformModes;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.util.Logging;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;

/**
 * Represents the standard Linux frame buffer device interface plus the custom
 * extensions to that interface provided by the Electrophoretic Display
 * Controller (EPDC) frame buffer driver.
 * <p>
 * The Linux frame buffer device interface is documented in <cite>The Frame
 * Buffer Device API</cite> found in the Ubuntu package called <i>linux-doc</i>
 * (see <i>/usr/share/doc/linux-doc/fb/api.txt.gz</i>).</p>
 * <p>
 * The EPDC frame buffer driver extensions are documented in the <cite>i.MX
 * Linux Reference Manual</cite> available on the
 * <a href="https://www.nxp.com/">NXP website</a> (registration required). On
 * the NXP home page, click Products, ARM Processors, i.MX Application
 * Processors, and then i.MX 6 Processors, for example. Select the i.MX6SLL
 * Product in the chart; then click the Documentation tab. Look for a download
 * with a label for Linux documents, like L4.1.15_2.1.0_LINUX_DOCS, under the
 * Supporting Information section. After downloading and expanding the archive,
 * the reference manual is found in the <i>doc</i> directory as the file
 * <i>i.MX_Linux_Reference_Manual.pdf</i>.</p>
 */
class EPDFrameBuffer {

    /**
     * The arithmetic right shift value to convert a bit depth to a byte depth.
     */
    private static final int BITS_TO_BYTES = 3;

    /**
     * The delay in milliseconds between the completion of all updates in the
     * EPDC driver and when the driver powers down the EPDC and display power
     * supplies.
     */
    private static final int POWERDOWN_DELAY = 1_000;

    /**
     * Linux system error: ENOTTY 25 Inappropriate ioctl for device.
     */
    private static final int ENOTTY = 25;

    private final PlatformLogger logger = Logging.getJavaFXLogger();
    private final EPDSettings settings;
    private final LinuxSystem system;
    private final EPDSystem driver;
    private final long fd;

    private final int xres;
    private final int yres;
    private final int xresVirtual;
    private final int yresVirtual;
    private final int xoffset;
    private final int yoffset;
    private final int bitsPerPixel;
    private final int bytesPerPixel;
    private final int byteOffset;
    private final MxcfbUpdateData updateData;
    private final MxcfbUpdateData syncUpdate;

    private int updateMarker;
    private int lastMarker;

    /**
     * Creates a new {@code EPDFrameBuffer} for the given frame buffer device.
     * The geometry of the Linux frame buffer is shown below for various color
     * depths and rotations on a sample system, as printed by the <i>fbset</i>
     * command. The first three are for landscape mode, while the last three are
     * for portrait.
     * <pre>{@code
     * geometry 800 600 800 640 32 (line length: 3200)
     * geometry 800 600 800 1280 16 (line length: 1600)
     * geometry 800 600 800 1280 8 (line length: 800)
     *
     * geometry 600 800 608 896 32 (line length: 2432)
     * geometry 600 800 608 1792 16 (line length: 1216)
     * geometry 600 800 608 1792 8 (line length: 608)
     * }</pre>
     *
     * @implNote {@code MonocleApplication} creates a {@code Screen} which
     * requires that the width be set to {@link #xresVirtual} even though only
     * the first {@link #xres} pixels of each row are visible. The EPDC driver
     * supports panning only in the y-direction, so it is not possible to center
     * the visible resolution horizontally when these values differ. The JavaFX
     * application should be left-aligned in this case and ignore the few extra
     * pixels on the right of its screen.
     *
     * @param fbPath the frame buffer device path, such as <i>/dev/fb0</i>
     * @throws IOException if an error occurs when opening the frame buffer
     * device or when getting or setting the frame buffer configuration
     * @throws IllegalArgumentException if the EPD settings specify an
     * unsupported color depth
     */
    EPDFrameBuffer(String fbPath) throws IOException {
        settings = EPDSettings.newInstance();
        system = LinuxSystem.getLinuxSystem();
        driver = EPDSystem.getEPDSystem();
        fd = system.open(fbPath, LinuxSystem.O_RDWR);
        if (fd == -1) {
            throw new IOException(system.getErrorMessage());
        }

        /*
         * Gets the current settings of the frame buffer device.
         */
        var screen = new FbVarScreenInfo();
        getScreenInfo(screen);

        /*
         * Changes the settings of the frame buffer from the system properties.
         *
         * See the section, "Format configuration," in "The Frame Buffer Device
         * API" for details. Note that xoffset is always zero, and yoffset can
         * be modified only by panning in the y-direction with the IOCTL call to
         * LinuxSystem.FBIOPAN_DISPLAY.
         */
        screen.setBitsPerPixel(screen.p, settings.bitsPerPixel);
        screen.setGrayscale(screen.p, settings.grayscale);
        switch (settings.bitsPerPixel) {
            case Byte.SIZE:
                // rgba 8/0,8/0,8/0,0/0 (set by driver when grayscale > 0)
                screen.setRed(screen.p, 0, 0);
                screen.setGreen(screen.p, 0, 0);
                screen.setBlue(screen.p, 0, 0);
                screen.setTransp(screen.p, 0, 0);
                break;
            case Short.SIZE:
                // rgba 5/11,6/5,5/0,0/0
                screen.setRed(screen.p, 5, 11);
                screen.setGreen(screen.p, 6, 5);
                screen.setBlue(screen.p, 5, 0);
                screen.setTransp(screen.p, 0, 0);
                break;
            case Integer.SIZE:
                // rgba 8/16,8/8,8/0,8/24
                screen.setRed(screen.p, 8, 16);
                screen.setGreen(screen.p, 8, 8);
                screen.setBlue(screen.p, 8, 0);
                screen.setTransp(screen.p, 8, 24);
                break;
            default:
                String msg = MessageFormat.format("Unsupported color depth: {0} bpp", settings.bitsPerPixel);
                logger.severe(msg);
                throw new IllegalArgumentException(msg);
        }
        screen.setActivate(screen.p, EPDSystem.FB_ACTIVATE_FORCE);
        screen.setRotate(screen.p, settings.rotate);
        setScreenInfo(screen);

        /*
         * Gets and logs the new settings of the frame buffer device.
         */
        getScreenInfo(screen);
        logScreenInfo(screen);
        xres = screen.getXRes(screen.p);
        yres = screen.getYRes(screen.p);
        xresVirtual = screen.getXResVirtual(screen.p);
        yresVirtual = screen.getYResVirtual(screen.p);
        xoffset = screen.getOffsetX(screen.p);
        yoffset = screen.getOffsetY(screen.p);
        bitsPerPixel = screen.getBitsPerPixel(screen.p);
        bytesPerPixel = bitsPerPixel >>> BITS_TO_BYTES;
        byteOffset = (xoffset + yoffset * xresVirtual) * bytesPerPixel;

        /*
         * Allocates objects for reuse to avoid creating new direct byte buffers
         * outside of the Java heap on each display update.
         */
        updateData = new MxcfbUpdateData();
        syncUpdate = createDefaultUpdate(xres, yres);
    }

    /**
     * Gets the variable screen information of the frame buffer. Run the
     * <i>fbset</i> command as <i>root</i> to print the screen information.
     *
     * @param screen the object representing the variable screen information
     * @throws IOException if an error occurs getting the information
     */
    private void getScreenInfo(FbVarScreenInfo screen) throws IOException {
        int rc = system.ioctl(fd, LinuxSystem.FBIOGET_VSCREENINFO, screen.p);
        if (rc != 0) {
            system.close(fd);
            throw new IOException(system.getErrorMessage());
        }
    }

    /**
     * Sets the variable screen information of the frame buffer.
     * <p>
     * "To ensure that the EPDC driver receives the initialization request, the
     * {@code activate} field of the {@code fb_var_screeninfo} parameter should
     * be set to {@code FB_ACTIVATE_FORCE}." [EPDC Panel Initialization,
     * <cite>i.MX Linux Reference Manual</cite>]</p>
     * <p>
     * To request a change to 8-bit grayscale format, the bits per pixel must be
     * set to 8 and the grayscale value must be set to one of the two valid
     * grayscale format values: {@code GRAYSCALE_8BIT} or
     * {@code GRAYSCALE_8BIT_INVERTED}. [Grayscale Framebuffer Selection,
     * <cite>i.MX Linux Reference Manual</cite>]</p>
     *
     * @param screen the object representing the variable screen information
     * @throws IOException if an error occurs setting the information
     */
    private void setScreenInfo(FbVarScreenInfo screen) throws IOException {
        int rc = system.ioctl(fd, LinuxSystem.FBIOPUT_VSCREENINFO, screen.p);
        if (rc != 0) {
            system.close(fd);
            throw new IOException(system.getErrorMessage());
        }
    }

    /**
     * Logs the variable screen information of the frame buffer, depending on
     * the logging level.
     *
     * @param screen the object representing the variable screen information
     */
    private void logScreenInfo(FbVarScreenInfo screen) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Frame buffer geometry: {0} {1} {2} {3} {4}",
                    screen.getXRes(screen.p), screen.getYRes(screen.p),
                    screen.getXResVirtual(screen.p), screen.getYResVirtual(screen.p),
                    screen.getBitsPerPixel(screen.p));
            logger.fine("Frame buffer rgba: {0}/{1},{2}/{3},{4}/{5},{6}/{7}",
                    screen.getRedLength(screen.p), screen.getRedOffset(screen.p),
                    screen.getGreenLength(screen.p), screen.getGreenOffset(screen.p),
                    screen.getBlueLength(screen.p), screen.getBlueOffset(screen.p),
                    screen.getTranspLength(screen.p), screen.getTranspOffset(screen.p));
            logger.fine("Frame buffer grayscale: {0}", screen.getGrayscale(screen.p));
        }
    }

    /**
     * Creates the default update data with values from the EPD system
     * properties, setting all fields except for the update marker. Reusing the
     * update data object avoids creating a new one for each update request.
     *
     * @implNote An update mode of {@link EPDSystem#UPDATE_MODE_FULL} would make
     * the {@link EPDSettings#NO_WAIT} system property useless by changing all
     * non-colliding updates into colliding ones, so this method sets the
     * default update mode to {@link EPDSystem#UPDATE_MODE_PARTIAL}.
     *
     * @param width the width of the update region
     * @param height the height of the update region
     * @return the default update data with all fields set but the update marker
     */
    private MxcfbUpdateData createDefaultUpdate(int width, int height) {
        var update = new MxcfbUpdateData();
        update.setUpdateRegion(update.p, 0, 0, width, height);
        update.setWaveformMode(update.p, settings.waveformMode);
        update.setUpdateMode(update.p, EPDSystem.UPDATE_MODE_PARTIAL);
        update.setTemp(update.p, EPDSystem.TEMP_USE_AMBIENT);
        update.setFlags(update.p, settings.flags);
        return update;
    }

    /**
     * Defines a mapping for common waveform modes. This mapping must be
     * configured for the automatic waveform mode selection to function
     * properly. Each of the parameters should be set to one of the following:
     * <ul>
     * <li>{@link EPDSystem#WAVEFORM_MODE_INIT}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_DU}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_GC16}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_GC4}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_A2}</li>
     * </ul>
     *
     * @implNote This method fails on the Kobo Glo HD Model N437 with the error
     * ENOTTY (25), "Inappropriate ioctl for device." The driver on that device
     * uses an extended structure with four additional integers, changing its
     * size and its corresponding request code. This method could use the
     * extended structure, but the driver on the Kobo Glo HD ignores it and
     * returns immediately, anyway. Furthermore, newer devices support both the
     * current structure and the extended one, but define the extra fields in a
     * different order. Therefore, simply use the current structure and ignore
     * an error of ENOTTY, picking up the default values for any extra fields.
     *
     * @param init the initialization mode for clearing the screen to all white
     * @param du the direct update mode for changing any gray values to either
     * all black or all white
     * @param gc4 the mode for 4-level (2-bit) grayscale images and text
     * @param gc8 the mode for 8-level (3-bit) grayscale images and text
     * @param gc16 the mode for 16-level (4-bit) grayscale images and text
     * @param gc32 the mode for 32-level (5-bit) grayscale images and text
     */
    private void setWaveformModes(int init, int du, int gc4, int gc8, int gc16, int gc32) {
        var modes = new MxcfbWaveformModes();
        modes.setModes(modes.p, init, du, gc4, gc8, gc16, gc32);
        int rc = system.ioctl(fd, driver.MXCFB_SET_WAVEFORM_MODES, modes.p);
        if (rc != 0 && system.errno() != ENOTTY) {
            logger.severe("Failed setting waveform modes: {0} ({1})",
                    system.getErrorMessage(), system.errno());
        }
    }

    /**
     * Sets the temperature to be used by the EPDC driver in subsequent panel
     * updates. Note that this temperature setting may be overridden by setting
     * the temperature in a specific update to anything other than
     * {@link EPDSystem#TEMP_USE_AMBIENT}.
     *
     * @param temp the temperature in degrees Celsius
     */
    private void setTemperature(int temp) {
        int rc = driver.ioctl(fd, driver.MXCFB_SET_TEMPERATURE, temp);
        if (rc != 0) {
            logger.severe("Failed setting temperature to {2} degrees Celsius: {0} ({1})",
                    system.getErrorMessage(), system.errno(), temp);
        }
    }

    /**
     * Selects between automatic and region update mode. In region update mode,
     * updates must be submitted with an IOCTL call to
     * {@link EPDSystem#MXCFB_SEND_UPDATE}. In automatic mode, updates are
     * generated by the driver when it detects that pages in a frame buffer
     * memory region have been modified.
     * <p>
     * Automatic mode is available only when it has been enabled in the Linux
     * kernel by the option CONFIG_FB_MXC_EINK_AUTO_UPDATE_MODE. You can find
     * the configuration options used to build the kernel in a file under
     * <i>/proc</i> or <i>/boot</i>, such as <i>/proc/config.gz</i>.</p>
     *
     * @param mode the automatic update mode, one of:
     * <ul>
     * <li>{@link EPDSystem#AUTO_UPDATE_MODE_REGION_MODE}</li>
     * <li>{@link EPDSystem#AUTO_UPDATE_MODE_AUTOMATIC_MODE}</li>
     * </ul>
     */
    private void setAutoUpdateMode(int mode) {
        int rc = driver.ioctl(fd, driver.MXCFB_SET_AUTO_UPDATE_MODE, mode);
        if (rc != 0) {
            logger.severe("Failed setting auto-update mode to {2}: {0} ({1})",
                    system.getErrorMessage(), system.errno(), mode);
        }
    }

    /**
     * Requests the entire visible region of the frame buffer to be updated to
     * the display.
     *
     * @param updateMode the update mode, one of:
     * <ul>
     * <li>{@link EPDSystem#UPDATE_MODE_PARTIAL}</li>
     * <li>{@link EPDSystem#UPDATE_MODE_FULL}</li>
     * </ul>
     * @param waveformMode the waveform mode, one of:
     * <ul>
     * <li>{@link EPDSystem#WAVEFORM_MODE_INIT}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_DU}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_GC16}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_GC4}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_A2}</li>
     * <li>{@link EPDSystem#WAVEFORM_MODE_AUTO}</li>
     * </ul>
     * @param flags a bit mask composed of the following flag values:
     * <ul>
     * <li>{@link EPDSystem#EPDC_FLAG_ENABLE_INVERSION}</li>
     * <li>{@link EPDSystem#EPDC_FLAG_FORCE_MONOCHROME}</li>
     * <li>{@link EPDSystem#EPDC_FLAG_USE_DITHERING_Y1}</li>
     * <li>{@link EPDSystem#EPDC_FLAG_USE_DITHERING_Y4}</li>
     * </ul>
     * @return the marker to identify this update in a subsequence call to
     * {@link #waitForUpdateComplete}
     */
    private int sendUpdate(int updateMode, int waveformMode, int flags) {
        updateData.setUpdateRegion(updateData.p, 0, 0, xres, yres);
        updateData.setUpdateMode(updateData.p, updateMode);
        updateData.setTemp(updateData.p, EPDSystem.TEMP_USE_AMBIENT);
        updateData.setFlags(updateData.p, flags);
        return sendUpdate(updateData, waveformMode);
    }

    /**
     * Requests an update to the display, allowing for the reuse of the update
     * data object. The waveform mode is reset because the update data could
     * have been used in a previous update. In that case, the waveform mode may
     * have been modified by the EPDC driver with the actual mode selected. The
     * update marker is overwritten with the next sequential marker.
     *
     * @param update the data describing the update; the waveform mode and
     * update marker are overwritten
     * @param waveformMode the waveform mode for this update
     * @return the marker to identify this update in a subsequence call to
     * {@link #waitForUpdateComplete}
     */
    private int sendUpdate(MxcfbUpdateData update, int waveformMode) {
        /*
         * The IOCTL call to MXCFB_WAIT_FOR_UPDATE_COMPLETE returns the error
         * "Invalid argument (22)" when passed an update marker of zero.
         */
        updateMarker++;
        if (updateMarker == 0) {
            updateMarker++;
        }
        update.setWaveformMode(update.p, waveformMode);
        update.setUpdateMarker(update.p, updateMarker);
        int rc = system.ioctl(fd, driver.MXCFB_SEND_UPDATE, update.p);
        if (rc != 0) {
            logger.severe("Failed sending update {2}: {0} ({1})",
                    system.getErrorMessage(), system.errno(), Integer.toUnsignedLong(updateMarker));
        } else if (logger.isLoggable(Level.FINER)) {
            logger.finer("Sent update: {0} x {1}, waveform {2}, selected {3}, flags 0x{4}, marker {5}",
                    update.getUpdateRegionWidth(update.p), update.getUpdateRegionHeight(update.p),
                    waveformMode, update.getWaveformMode(update.p),
                    Integer.toHexString(update.getFlags(update.p)).toUpperCase(),
                    Integer.toUnsignedLong(updateMarker));
        }
        return updateMarker;
    }

    /**
     * Blocks and waits for a previous update request to complete.
     *
     * @param marker the marker to identify a particular update, returned by
     * {@link #sendUpdate(MxcfbUpdateData, int)}
     */
    private void waitForUpdateComplete(int marker) {
        /*
         * This IOCTL call returns: 0 if the marker was not found because the
         * update already completed or failed, negative (-1) with the error
         * "Connection timed out (110)" if the wait timed out after 5 seconds,
         * or positive if the wait occurred and completed (see
         * "wait_for_completion_timeout" in "kernel/sched/completion.c").
         */
        int rc = driver.ioctl(fd, driver.MXCFB_WAIT_FOR_UPDATE_COMPLETE, marker);
        if (rc < 0) {
            logger.severe("Failed waiting for update {2}: {0} ({1})",
                    system.getErrorMessage(), system.errno(), Integer.toUnsignedLong(marker));
        } else if (rc == 0 && logger.isLoggable(Level.FINER)) {
            logger.finer("Update completed before wait: marker {0}",
                    Integer.toUnsignedLong(marker));
        }
    }

    /**
     * Sets the delay between the completion of all updates in the driver and
     * when the driver should power down the EPDC and display power supplies. To
     * disable powering down entirely, use the delay value
     * {@link EPDSystem#FB_POWERDOWN_DISABLE}.
     *
     * @param delay the delay in milliseconds
     */
    private void setPowerdownDelay(int delay) {
        int rc = driver.ioctl(fd, driver.MXCFB_SET_PWRDOWN_DELAY, delay);
        if (rc != 0) {
            logger.severe("Failed setting power-down delay to {2}: {0} ({1})",
                    system.getErrorMessage(), system.errno(), delay);
        }
    }

    /**
     * Gets the current power-down delay from the EPDC driver.
     *
     * @return the delay in milliseconds
     */
    private int getPowerdownDelay() {
        var integer = new IntStructure();
        int rc = system.ioctl(fd, driver.MXCFB_GET_PWRDOWN_DELAY, integer.p);
        if (rc != 0) {
            logger.severe("Failed getting power-down delay: {0} ({1})",
                    system.getErrorMessage(), system.errno());
        }
        return integer.get(integer.p);
    }

    /**
     * Selects a scheme for the flow of updates within the driver.
     *
     * @param scheme the update scheme, one of:
     * <ul>
     * <li>{@link EPDSystem#UPDATE_SCHEME_SNAPSHOT}</li>
     * <li>{@link EPDSystem#UPDATE_SCHEME_QUEUE}</li>
     * <li>{@link EPDSystem#UPDATE_SCHEME_QUEUE_AND_MERGE}</li>
     * </ul>
     */
    private void setUpdateScheme(int scheme) {
        int rc = driver.ioctl(fd, driver.MXCFB_SET_UPDATE_SCHEME, scheme);
        if (rc != 0) {
            logger.severe("Failed setting update scheme to {2}: {0} ({1})",
                    system.getErrorMessage(), system.errno(), scheme);
        }
    }

    /**
     * Initializes the EPDC frame buffer device, setting the update scheme to
     * {@link EPDSystem#UPDATE_SCHEME_SNAPSHOT}.
     */
    void init() {
        setWaveformModes(EPDSystem.WAVEFORM_MODE_INIT, EPDSystem.WAVEFORM_MODE_DU,
                EPDSystem.WAVEFORM_MODE_GC4, EPDSystem.WAVEFORM_MODE_GC16,
                EPDSystem.WAVEFORM_MODE_GC16, EPDSystem.WAVEFORM_MODE_GC16);
        setTemperature(EPDSystem.TEMP_USE_AMBIENT);
        setAutoUpdateMode(EPDSystem.AUTO_UPDATE_MODE_REGION_MODE);
        setPowerdownDelay(POWERDOWN_DELAY);
        setUpdateScheme(EPDSystem.UPDATE_SCHEME_SNAPSHOT);
    }

    /**
     * Clears the display panel. The visible frame buffer should be cleared with
     * zeros when called. This method sends two direct updates (all black
     * followed by all white) to refresh the screen and clear any ghosting
     * effects, and returns when both updates are complete.
     * <p>
     * <strong>This method is not thread safe</strong>, but it is invoked only
     * once from the Event Thread during initialization.</p>
     */
    void clear() {
        lastMarker = sendUpdate(EPDSystem.UPDATE_MODE_FULL,
                EPDSystem.WAVEFORM_MODE_DU, 0);
        lastMarker = sendUpdate(EPDSystem.UPDATE_MODE_FULL,
                EPDSystem.WAVEFORM_MODE_DU, EPDSystem.EPDC_FLAG_ENABLE_INVERSION);
        waitForUpdateComplete(lastMarker);
    }

    /**
     * Sends the updated contents of the Linux frame buffer to the EPDC driver,
     * optionally synchronizing with the driver by first waiting for the
     * previous update to complete.
     * <p>
     * <strong>This method is not thread safe</strong>, but it is invoked only
     * from the JavaFX Application Thread.</p>
     */
    void sync() {
        if (!settings.noWait) {
            waitForUpdateComplete(lastMarker);
        }
        lastMarker = sendUpdate(syncUpdate, settings.waveformMode);
    }

    /**
     * Gets the number of bytes from the beginning of the frame buffer to the
     * start of its visible resolution.
     *
     * @return the offset in bytes
     */
    int getByteOffset() {
        return byteOffset;
    }

    /**
     * Creates an off-screen byte buffer equal in resolution to the virtual
     * resolution of the frame buffer, but with 32 bits per pixel.
     *
     * @return a 32-bit pixel buffer matching the resolution of the frame buffer
     */
    ByteBuffer getOffscreenBuffer() {
        /*
         * In this case, a direct byte buffer outside of the normal heap is
         * faster than a non-direct byte buffer on the heap. The frame rate is
         * roughly 10 to 40 percent faster for a framebuffer with 8 bits per
         * pixel and 40 to 60 percent faster for a framebuffer with 16 bits per
         * pixel, depending on the device processor and screen size.
         */
        int size = xresVirtual * yres * Integer.BYTES;
        return ByteBuffer.allocateDirect(size);
    }

    /**
     * Creates a new mapping of the Linux frame buffer device into memory.
     *
     * @implNote The virtual y-resolution reported by the device driver can be
     * wrong, as shown by the following example on the Kobo Glo HD Model N437
     * which reports 2,304 pixels when the correct value is 1,152 pixels
     * (6,782,976 / 5,888). Therefore, this method cannot use the frame buffer
     * virtual resolution to calculate its size.
     *
     * <pre>{@code
     * $ sudo fbset -i
     *
     * mode "1448x1072-46"
     * # D: 80.000 MHz, H: 50.188 kHz, V: 46.385 Hz
     * geometry 1448 1072 1472 2304 32
     * timings 12500 16 102 4 4 28 2
     * rgba 8/16,8/8,8/0,8/24
     * endmode
     *
     * Frame buffer device information:
     * Name        : mxc_epdc_fb
     * Address     : 0x88000000
     * Size        : 6782976
     * Type        : PACKED PIXELS
     * Visual      : TRUECOLOR
     * XPanStep    : 1
     * YPanStep    : 1
     * YWrapStep   : 0
     * LineLength  : 5888
     * Accelerator : No
     * }</pre>
     *
     * @return a byte buffer containing the mapping of the Linux frame buffer
     * device if successful; otherwise {@code null}
     */
    ByteBuffer getMappedBuffer() {
        ByteBuffer buffer = null;
        int size = xresVirtual * yres * bytesPerPixel;
        logger.fine("Mapping frame buffer: {0} bytes", size);
        long addr = system.mmap(0l, size, LinuxSystem.PROT_WRITE, LinuxSystem.MAP_SHARED, fd, 0);
        if (addr == LinuxSystem.MAP_FAILED) {
            logger.severe("Failed mapping {2} bytes of frame buffer: {0} ({1})",
                    system.getErrorMessage(), system.errno(), size);
        } else {
            buffer = C.getC().NewDirectByteBuffer(addr, size);
        }
        return buffer;
    }

    /**
     * Deletes the mapping of the Linux frame buffer device.
     *
     * @param buffer the byte buffer containing the mapping of the Linux frame
     * buffer device
     */
    void releaseMappedBuffer(ByteBuffer buffer) {
        int size = buffer.capacity();
        logger.fine("Unmapping frame buffer: {0} bytes", size);
        int rc = system.munmap(C.getC().GetDirectBufferAddress(buffer), size);
        if (rc != 0) {
            logger.severe("Failed unmapping {2} bytes of frame buffer: {0} ({1})",
                    system.getErrorMessage(), system.errno(), size);
        }
    }

    /**
     * Closes the Linux frame buffer device.
     */
    void close() {
        system.close(fd);
    }

    /**
     * Gets the native handle to the Linux frame buffer device.
     *
     * @return the frame buffer device file descriptor
     */
    long getNativeHandle() {
        return fd;
    }

    /**
     * Gets the frame buffer width in pixels. See the notes for the
     * {@linkplain EPDFrameBuffer#EPDFrameBuffer constructor} above.
     *
     * @implNote When using an 8-bit, unrotated, and uninverted frame buffer in
     * the Y8 pixel format, the Kobo Clara HD Model N249 works only when this
     * method returns the visible x-resolution ({@code xres}) instead of the
     * normal virtual x-resolution ({@code xresVirtual}).
     *
     * @return the width in pixels
     */
    int getWidth() {
        return settings.getWidthVisible ? xres : xresVirtual;
    }

    /**
     * Gets the frame buffer height in pixels.
     *
     * @return the height in pixels
     */
    int getHeight() {
        return yres;
    }

    /**
     * Gets the frame buffer color depth in bits per pixel.
     *
     * @return the color depth in bits per pixel
     */
    int getBitDepth() {
        return bitsPerPixel;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[width={1} height={2} bitDepth={3}]",
                getClass().getName(), getWidth(), getHeight(), getBitDepth());
    }
}
