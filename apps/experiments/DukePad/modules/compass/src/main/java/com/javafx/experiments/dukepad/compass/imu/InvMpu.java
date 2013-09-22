/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.compass.imu;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public class InvMpu {
    public static final short INV_X_GYRO      = 0x40;
    public static final short INV_Y_GYRO      = 0x20;
    public static final short INV_Z_GYRO      = 0x10;
    public static final short INV_XYZ_GYRO    = INV_X_GYRO | INV_Y_GYRO | INV_Z_GYRO;
    public static final short INV_XYZ_ACCEL   = 0x08;
    public static final short INV_XYZ_COMPASS = 0x01;
    
    public static final short MPU_INT_STATUS_DATA_READY       = 0x0001;
    public static final short MPU_INT_STATUS_DMP              = 0x0002;
    public static final short MPU_INT_STATUS_PLL_READY        = 0x0004;
    public static final short MPU_INT_STATUS_I2C_MST          = 0x0008;
    public static final short MPU_INT_STATUS_FIFO_OVERFLOW    = 0x0010;
    public static final short MPU_INT_STATUS_ZMOT             = 0x0020;
    public static final short MPU_INT_STATUS_MOT              = 0x0040;
    public static final short MPU_INT_STATUS_FREE_FALL        = 0x0080;
    public static final short MPU_INT_STATUS_DMP_0            = 0x0100;
    public static final short MPU_INT_STATUS_DMP_1            = 0x0200;
    public static final short MPU_INT_STATUS_DMP_2            = 0x0400;
    public static final short MPU_INT_STATUS_DMP_3            = 0x0800;
    public static final short MPU_INT_STATUS_DMP_4            = 0x1000;
    public static final short MPU_INT_STATUS_DMP_5            = 0x2000;

    void mpu_write_mem(int CFG_20, int i, byte[] tmp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /* Hardware registers needed by driver. */
    private static class gyro_reg_s {
        short who_am_i;
        short rate_div;
        short lpf;
        short prod_id;
        short user_ctrl;
        short fifo_en;
        short gyro_cfg;
        short accel_cfg;
        short accel_cfg2;
        short lp_accel_odr;
        short motion_thr;
        short motion_dur;
        short fifo_count_h;
        short fifo_r_w;
        short raw_gyro;
        short raw_accel;
        short temp;
        short int_enable;
        short dmp_int_status;
        short int_status;
        short accel_intel;
        short pwr_mgmt_1;
        short pwr_mgmt_2;
        short int_pin_cfg;
        short mem_r_w;
        short accel_offs;
        short i2c_mst;
        short bank_sel;
        short mem_start_addr;
        short prgm_start_h;
        short s0_addr;
        short s0_reg;
        short s0_ctrl;
        short s1_addr;
        short s1_reg;
        short s1_ctrl;
        short s4_ctrl;
        short s0_do;
        short s1_do;
        short i2c_delay_ctrl;
        short raw_compass;
        /* The I2C_MST_VDDIO bit is in this register. */
        short yg_offs_tc;
    };
    
    /* Information specific to a particular device. */
    private static class hw_s {
        short addr;
        int max_fifo;
        short num_reg;
        int temp_sens;
        short temp_offset;
        int bank_size;
        int compass_fsr;
    };
    
    /* When entering motion interrupt mode, the driver keeps track of the
     * previous state so that it can be restored at a later time.
     * TODO: This is tacky. Fix it.
     */
    private static class motion_int_cache_s {
        int gyro_fsr;
        short accel_fsr;
        int lpf;
        int sample_rate;
        short sensors_on;
        short fifo_sensors;
        short dmp_on;
    };
    
    /* Cached chip configuration data.
     * TODO: A lot of these can be handled with a bitmask.
     */
    private static class chip_cfg_s {
        /* Matches gyro_cfg >> 3 & 0x03 */
        gyro_fsr_e gyro_fsr;
        /* Matches accel_cfg >> 3 & 0x03 */
        accel_fsr_e accel_fsr;
        /* Enabled sensors. Uses same masks as fifo_en, NOT pwr_mgmt_2. */
        short sensors;
        /* Matches config register. */
        lpf_e lpf;
        clock_sel_e clk_src;
        /* Sample rate, NOT rate divider. */
        int sample_rate;
        /* Matches fifo_en register. */
        short fifo_enable;
        /* Matches int enable register. */
        short int_enable;
        /* 1 if devices on auxiliary I2C bus appear on the primary. */
        short bypass_mode;
        /* 1 if half-sensitivity.
         * NOTE: This doesn't belong here, but everything else in hw_s is const,
         * and this allows us to save some precious RAM.
         */
        short accel_half;
        /* 1 if device in low-power accel-only mode. */
        boolean lp_accel_mode;
        /* 1 if interrupts are only triggered on motion events. */
        boolean int_motion_only;
        motion_int_cache_s cache = new motion_int_cache_s();
        /* 1 for active low interrupts. */
        boolean active_low_int;
        /* 1 for latched interrupts. */
        boolean latched_int;
        /* 1 if DMP is enabled. */
        boolean dmp_on;
        /* Ensures that DMP will only be loaded once. */
        boolean dmp_loaded;
        /* Sampling rate used when DMP is enabled. */
        int dmp_sample_rate;
        /* Compass sample rate. */
        int compass_sample_rate;
        short compass_addr;
        short[] mag_sens_adj = new short[3];
    };
    
    /* Information for self-test. */
    private static class test_s {
        long gyro_sens;
        long accel_sens;
        short reg_rate_div;
        short reg_lpf;
        short reg_gyro_fsr;
        short reg_accel_fsr;
        int wait_ms;
        short packet_thresh;
        float min_dps;
        float max_dps;
        float max_gyro_var;
        float min_g;
        float max_g;
        float max_accel_var;
    };
    
    /* Gyro driver state variables. */
    private static class gyro_state_s {
        final gyro_reg_s reg = new gyro_reg_s();
        final hw_s hw = new hw_s();
        final chip_cfg_s chip_cfg = new chip_cfg_s();
        final test_s test = new test_s();
    };
    
    /* Filter configurations. */
    private static enum lpf_e {
        INV_FILTER_256HZ_NOLPF2,
        INV_FILTER_188HZ,
        INV_FILTER_98HZ,
        INV_FILTER_42HZ,
        INV_FILTER_20HZ,
        INV_FILTER_10HZ,
        INV_FILTER_5HZ,
        INV_FILTER_2100HZ_NOLPF,
        NUM_FILTER,
        INVALID
    };
    
    /* Full scale ranges. */
    private static enum gyro_fsr_e {
        INV_FSR_250DPS,
        INV_FSR_500DPS,
        INV_FSR_1000DPS,
        INV_FSR_2000DPS,
        NUM_GYRO_FSR,
        INVALID
    };
    
    /* Full scale ranges. */
    private static enum accel_fsr_e {
        INV_FSR_2G,
        INV_FSR_4G,
        INV_FSR_8G,
        INV_FSR_16G,
        NUM_ACCEL_FSR,
        INVALID
    };
    
    /* Clock sources. */
    private static enum clock_sel_e {
        INV_CLK_INTERNAL,
        INV_CLK_PLL,
        NUM_CLK
    };
    
    /* Low-power accel wakeup rates. */
    private static enum lp_accel_rate_e {
        INV_LPA_1_25HZ,
        INV_LPA_5HZ,
        INV_LPA_20HZ,
        INV_LPA_40HZ
    };
    
    private static final short BIT_I2C_MST_VDDIO   = 0x80;
    private static final short BIT_FIFO_EN         = 0x40;
    private static final short BIT_DMP_EN          = 0x80;
    private static final short BIT_FIFO_RST        = 0x04;
    private static final short BIT_DMP_RST         = 0x08;
    private static final short BIT_FIFO_OVERFLOW   = 0x10;
    private static final short BIT_DATA_RDY_EN     = 0x01;
    private static final short BIT_DMP_INT_EN      = 0x02;
    private static final short BIT_MOT_INT_EN      = 0x40;
    private static final short BITS_FSR            = 0x18;
    private static final short BITS_LPF            = 0x07;
    private static final short BITS_HPF            = 0x07;
    private static final short BITS_CLK            = 0x07;
    private static final short BIT_FIFO_SIZE_1024  = 0x40;
    private static final short BIT_FIFO_SIZE_2048  = 0x80;
    private static final short BIT_FIFO_SIZE_4096  = 0xC0;
    private static final short BIT_RESET           = 0x80;
    private static final short BIT_SLEEP           = 0x40;
    private static final short BIT_S0_DELAY_EN     = 0x01;
    private static final short BIT_S2_DELAY_EN     = 0x04;
    private static final short BITS_SLAVE_LENGTH   = 0x0F;
    private static final short BIT_SLAVE_BYTE_SW   = 0x40;
    private static final short BIT_SLAVE_GROUP     = 0x10;
    private static final short BIT_SLAVE_EN        = 0x80;
    private static final short BIT_I2C_READ        = 0x80;
    private static final short BITS_I2C_MASTER_DLY = 0x1F;
    private static final short BIT_AUX_IF_EN       = 0x20;
    private static final short BIT_ACTL            = 0x80;
    private static final short BIT_LATCH_EN        = 0x20;
    private static final short BIT_ANY_RD_CLR      = 0x10;
    private static final short BIT_BYPASS_EN       = 0x02;
    private static final short BITS_WOM_EN         = 0xC0;
    private static final short BIT_LPA_CYCLE       = 0x20;
    private static final short BIT_STBY_XA         = 0x20;
    private static final short BIT_STBY_YA         = 0x10;
    private static final short BIT_STBY_ZA         = 0x08;
    private static final short BIT_STBY_XG         = 0x04;
    private static final short BIT_STBY_YG         = 0x02;
    private static final short BIT_STBY_ZG         = 0x01;
    private static final short BIT_STBY_XYZA       = BIT_STBY_XA | BIT_STBY_YA | BIT_STBY_ZA;
    private static final short BIT_STBY_XYZG       = BIT_STBY_XG | BIT_STBY_YG | BIT_STBY_ZG;
    
    private static final short SUPPORTS_AK89xx_HIGH_SENS   = 0x00;
    private static final short AK89xx_FSR                  = 9830;
    
    private static final short AKM_REG_WHOAMI      = 0x00;
    
    private static final short AKM_REG_ST1         = 0x02;
    private static final short AKM_REG_HXL         = 0x03;
    private static final short AKM_REG_ST2         = 0x09;
    
    private static final short AKM_REG_CNTL        = 0x0A;
    private static final short AKM_REG_ASTC        = 0x0C;
    private static final short AKM_REG_ASAX        = 0x10;
    private static final short AKM_REG_ASAY        = 0x11;
    private static final short AKM_REG_ASAZ        = 0x12;
    
    private static final short AKM_DATA_READY      = 0x01;
    private static final short AKM_DATA_OVERRUN    = 0x02;
    private static final short AKM_OVERFLOW        = 0x80;
    private static final short AKM_DATA_ERROR      = 0x40;
    
    private static final short AKM_BIT_SELF_TEST   = 0x40;
    
    private static final short AKM_POWER_DOWN          = 0x00 | SUPPORTS_AK89xx_HIGH_SENS;
    private static final short AKM_SINGLE_MEASUREMENT  = 0x01 | SUPPORTS_AK89xx_HIGH_SENS;
    private static final short AKM_FUSE_ROM_ACCESS     = 0x0F | SUPPORTS_AK89xx_HIGH_SENS;
    private static final short AKM_MODE_SELF_TEST      = 0x08 | SUPPORTS_AK89xx_HIGH_SENS;
    
    private static final short AKM_WHOAMI      = 0x48;
    
    private static gyro_state_s st = new gyro_state_s();
    static {
        st.reg.who_am_i       = 0x75;
        st.reg.rate_div       = 0x19;
        st.reg.lpf            = 0x1A;
        st.reg.prod_id        = 0x0C;
        st.reg.user_ctrl      = 0x6A;
        st.reg.fifo_en        = 0x23;
        st.reg.gyro_cfg       = 0x1B;
        st.reg.accel_cfg      = 0x1C;
        st.reg.motion_thr     = 0x1F;
        st.reg.motion_dur     = 0x20;
        st.reg.fifo_count_h   = 0x72;
        st.reg.fifo_r_w       = 0x74;
        st.reg.raw_gyro       = 0x43;
        st.reg.raw_accel      = 0x3B;
        st.reg.temp           = 0x41;
        st.reg.int_enable     = 0x38;
        st.reg.dmp_int_status = 0x39;
        st.reg.int_status     = 0x3A;
        st.reg.pwr_mgmt_1     = 0x6B;
        st.reg.pwr_mgmt_2     = 0x6C;
        st.reg.int_pin_cfg    = 0x37;
        st.reg.mem_r_w        = 0x6F;
        st.reg.accel_offs     = 0x06;
        st.reg.i2c_mst        = 0x24;
        st.reg.bank_sel       = 0x6D;
        st.reg.mem_start_addr = 0x6E;
        st.reg.prgm_start_h   = 0x70;
        st.reg.raw_compass    = 0x49;
        st.reg.yg_offs_tc     = 0x01;
        st.reg.s0_addr        = 0x25;
        st.reg.s0_reg         = 0x26;
        st.reg.s0_ctrl        = 0x27;
        st.reg.s1_addr        = 0x28;
        st.reg.s1_reg         = 0x29;
        st.reg.s1_ctrl        = 0x2A;
        st.reg.s4_ctrl        = 0x34;
        st.reg.s0_do          = 0x63;
        st.reg.s1_do          = 0x64;
        st.reg.i2c_delay_ctrl = 0x67;
        
        st.hw.addr           = 0x68;
        st.hw.max_fifo       = 1024;
        st.hw.num_reg        = 118;
        st.hw.temp_sens      = 340;
        st.hw.temp_offset    = -521;
        st.hw.bank_size      = 256;
        st.hw.compass_fsr    = AK89xx_FSR;
        
        st.test.gyro_sens      = 32768/250;
        st.test.accel_sens     = 32768/16;
        st.test.reg_rate_div   = 0;    /* 1kHz. */
        st.test.reg_lpf        = 1;    /* 188Hz. */
        st.test.reg_gyro_fsr   = 0;    /* 250dps. */
        st.test.reg_accel_fsr  = 0x18; /* 16g. */
        st.test.wait_ms        = 50;
        st.test.packet_thresh  = 5;    /* 5% */
        st.test.min_dps        = 10.f;
        st.test.max_dps        = 105.f;
        st.test.max_gyro_var   = 0.14f;
        st.test.min_g          = 0.3f;
        st.test.max_g          = 0.95f;
        st.test.max_accel_var  = 0.14f;
    }
    
    private static final int MAX_PACKET_LENGTH = 12;
    
    private static final int MAX_COMPASS_SAMPLE_RATE = 100;
    
    private I2CBus bus = null;
    private I2CDevice device = null;
    
    public boolean set_i2c(int i2c_bus, short i2c_address) {
        st.hw.addr = i2c_address;
        try {
            bus = I2CFactory.getInstance(i2c_bus);
            device = bus.getDevice(st.hw.addr);
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
        
    /**
     *  @brief      Enable/disable data ready interrupt.
     *  If the DMP is on, the DMP interrupt is enabled. Otherwise, the data ready
     *  interrupt is used.
     *  @param[in]  enable      1 to enable interrupt.
     *  @return     0 if successful.
     */
    private boolean set_int_enable(boolean enable)
    {
        byte tmp;
        
        if (st.chip_cfg.dmp_on) {
            if (enable)
                tmp = BIT_DMP_INT_EN;
            else
                tmp = 0x00;
            try {
                device.write(st.reg.int_enable, tmp);
            } catch (IOException ex) {
                return false;
            }
            st.chip_cfg.int_enable = tmp;
        } else {
            if (st.chip_cfg.sensors == 0)
                return false;
            if (enable && st.chip_cfg.int_enable != 0)
                return true;
            if (enable)
                tmp = BIT_DATA_RDY_EN;
            else
                tmp = 0x00;
            try {
                device.write(st.reg.int_enable, tmp);
            } catch (IOException ex) {
                return false;
            }
            st.chip_cfg.int_enable = tmp;
        }
        return true;
    }
    
    /**
     *  @brief      Initialize hardware.
     *  Initial configuration:\n
     *  Gyro FSR: +/- 2000DPS\n
     *  Accel FSR +/- 2G\n
     *  DLPF: 42Hz\n
     *  FIFO rate: 50Hz\n
     *  Clock source: Gyro PLL\n
     *  FIFO: Disabled.\n
     *  Data ready interrupt: Disabled, active low, unlatched.
     *  @param[in]  int_param   Platform-specific parameters to interrupt API.
     *  @return     0 if successful.
     */
    public boolean mpu_init(boolean useMag)
    {
        try {
            byte[] data = new byte[6];
            byte rev;
            
            /* Reset device. */
            data[0] = (byte)BIT_RESET;
            device.write(st.reg.pwr_mgmt_1, data[0]);
            TimeUnit.MILLISECONDS.sleep(100);
            
            /* Wake up chip. */
            data[0] = 0x00;
            device.write(st.reg.pwr_mgmt_1, data[0]);
            
            /* Check product revision. */
            device.read(st.reg.accel_offs, data, 0, 6);
            
            rev = (byte) (((data[5] & 0x01) << 2) | ((data[3] & 0x01) << 1) | (data[1] & 0x01));
            
            if (rev != 0) {
                /* Congrats, these parts are better. */
                if (rev == 1)
                    st.chip_cfg.accel_half = 1;
                else if (rev == 2)
                    st.chip_cfg.accel_half = 0;
                else {
                    System.out.printf("Unsupported software product rev %d.\n", rev);
                    return false;
                }
            } else {
                device.read(st.reg.prod_id, data, 0, 1);
                rev = (byte)(data[0] & 0x0F);
                if (rev == 0) {
                    System.out.printf("Product ID read as 0 indicates device is either incompatible or an MPU3050.\n");
                    return false;
                } else if (rev == 4) {
                    System.out.printf("Half sensitivity part found.\n");
                    st.chip_cfg.accel_half = 1;
                } else
                    st.chip_cfg.accel_half = 0;
            }
            
            /* Set to invalid values to ensure no I2C writes are skipped. */
            st.chip_cfg.sensors = 0xFF;
            st.chip_cfg.gyro_fsr = gyro_fsr_e.INVALID;
            st.chip_cfg.accel_fsr = accel_fsr_e.INVALID;
            st.chip_cfg.lpf = lpf_e.INVALID;
            st.chip_cfg.sample_rate = 0xFFFF;
            st.chip_cfg.fifo_enable = 0xFF;
            st.chip_cfg.bypass_mode = -1;
            st.chip_cfg.compass_sample_rate = 0xFFFF;
            /* mpu_set_sensors always preserves this setting. */
            st.chip_cfg.clk_src = clock_sel_e.INV_CLK_PLL;
            /* Handled in next call to mpu_set_bypass. */
            st.chip_cfg.active_low_int = true;
            st.chip_cfg.latched_int = false;
            st.chip_cfg.int_motion_only = false;
            st.chip_cfg.lp_accel_mode = false;
            st.chip_cfg.cache.gyro_fsr = 0;
            st.chip_cfg.cache.accel_fsr = 0;
            st.chip_cfg.cache.lpf = 0;
            st.chip_cfg.cache.sample_rate = 0;
            st.chip_cfg.cache.sensors_on = 0;
            st.chip_cfg.cache.fifo_sensors = 0;
            st.chip_cfg.cache.dmp_on = 0;
            st.chip_cfg.dmp_on = false;
            st.chip_cfg.dmp_loaded = false;
            st.chip_cfg.dmp_sample_rate = 0;
            
            if (!mpu_set_gyro_fsr(2000))
                return false;
            if (!mpu_set_accel_fsr(2))
                return false;
            if (!mpu_set_lpf(42))
                return false;
            if (!mpu_set_sample_rate(50))
                return false;
            if (!mpu_configure_fifo((short)0))
                return false;
            
            if (useMag) {
                setup_compass();
                if (!mpu_set_compass_sample_rate(10))
                    return false;
            }
            
            mpu_set_sensors((short)0);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return false;
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
            return false;
        }
        
        return true;
    }
    
    /**
     *  @brief      Enter low-power accel-only mode.
     *  In low-power accel mode, the chip goes to sleep and only wakes up to sample
     *  the accelerometer at one of the following frequencies:
     *  \n MPU6050: 1.25Hz, 5Hz, 20Hz, 40Hz
     *  \n MPU6500: 1.25Hz, 2.5Hz, 5Hz, 10Hz, 20Hz, 40Hz, 80Hz, 160Hz, 320Hz, 640Hz
     *  \n If the requested rate is not one listed above, the device will be set to
     *  the next highest rate. Requesting a rate above the maximum supported
     *  frequency will result in an error.
     *  \n To select a fractional wake-up frequency, round down the value passed to
     *  @e rate.
     *  @param[in]  rate        Minimum sampling rate, or zero to disable LP
     *                          accel mode.
     *  @return     0 if successful.
     */
    boolean mpu_lp_accel_mode(int rate)
    {
        byte[] tmp = new byte[2];
        
        if (rate > 40)
            return false;
        
        if (rate == 0) {
            mpu_set_int_latched(false);
            tmp[0] = 0;
            tmp[1] = BIT_STBY_XYZG;
            try {
                device.write(st.reg.pwr_mgmt_1, tmp, 0, 2);
            } catch (IOException ex) {
                return false;
            }
            st.chip_cfg.lp_accel_mode = false;
            return true;
        }
        /* For LP accel, we automatically configure the hardware to produce latched
         * interrupts. In LP accel mode, the hardware cycles into sleep mode before
         * it gets a chance to deassert the interrupt pin; therefore, we shift this
         * responsibility over to the MCU.
         *
         * Any register read will clear the interrupt.
         */
        mpu_set_int_latched(true);
        tmp[0] = BIT_LPA_CYCLE;
        lp_accel_rate_e lp_accel_rate;
        if (rate == 1) {
            lp_accel_rate = lp_accel_rate_e.INV_LPA_1_25HZ;
            mpu_set_lpf(5);
        } else if (rate <= 5) {
            lp_accel_rate = lp_accel_rate_e.INV_LPA_5HZ;
            mpu_set_lpf(5);
        } else if (rate <= 20) {
            lp_accel_rate = lp_accel_rate_e.INV_LPA_20HZ;
            mpu_set_lpf(10);
        } else {
            lp_accel_rate = lp_accel_rate_e.INV_LPA_40HZ;
            mpu_set_lpf(20);
        }
        tmp[1] = (byte) ((lp_accel_rate.ordinal() << 6) | BIT_STBY_XYZG);
        try {
            device.write(st.reg.pwr_mgmt_1, tmp, 0, 2);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.sensors = INV_XYZ_ACCEL;
        st.chip_cfg.clk_src = clock_sel_e.INV_CLK_INTERNAL;
        st.chip_cfg.lp_accel_mode = true;
        mpu_configure_fifo((short)0);
        
        return true;
    }
    
    /**
     *  @brief      Read temperature data directly from the registers.
     *  @param[out] data        Data in q16 format.
     *  @param[out] timestamp   Timestamp in milliseconds. Null if not needed.
     *  @return     0 if successful.
     */
    boolean mpu_get_temperature(long[] data, long[] timestamp)
    {
        if (st.chip_cfg.sensors == 0)
            return false;

        byte[] tmp = new byte[2];
        try {
            device.write(st.reg.temp, tmp, 0, 2);
        } catch (IOException ex) {
            return false;
        }
        short raw = ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get();

        if (timestamp != null && timestamp.length > 0)
            timestamp[0] = System.currentTimeMillis();

        data[0] = (long)((35 + ((raw - (float)st.hw.temp_offset) / st.hw.temp_sens)) * 65536L);
        return true;
    }

    /**
     *  @brief      Push biases to the accel bias registers.
     *  This function expects biases relative to the current sensor output, and
     *  these biases will be added to the factory-supplied values.
     *  @param[in]  accel_bias  New biases.
     *  @return     0 if successful.
     */
    boolean mpu_set_accel_bias(long[] accel_bias)
    {
        if (accel_bias != null && accel_bias.length != 3) {
            return false;
        }
        if (accel_bias[0] == 0 && accel_bias[1] == 0 && accel_bias[2] == 0)
            return true;

        byte[] data = new byte[6];
        short[] accel_hw = new short[3];
        short[] got_accel = new short[3];
        short[] fg = new short[3];

        try {
            device.read(0x03, data, 0, 3);
        } catch (IOException ex) {
            return false;
        }
        
        fg[0] = (short) (((data[0] >> 4) + 8) & 0xf);
        fg[1] = (short) (((data[1] >> 4) + 8) & 0xf);
        fg[2] = (short) (((data[2] >> 4) + 8) & 0xf);

        accel_hw[0] = (short)(accel_bias[0] * 2 / (64 + fg[0]));
        accel_hw[1] = (short)(accel_bias[1] * 2 / (64 + fg[1]));
        accel_hw[2] = (short)(accel_bias[2] * 2 / (64 + fg[2]));

        try {
            device.read(0x06, data, 0, 6);
        } catch (IOException ex) {
            return false;
        }
        
        got_accel[0] = (short) (((short)data[0] << 8) | data[1]);
        got_accel[1] = (short) (((short)data[2] << 8) | data[3]);
        got_accel[2] = (short) (((short)data[4] << 8) | data[5]);

        accel_hw[0] += got_accel[0];
        accel_hw[1] += got_accel[1];
        accel_hw[2] += got_accel[2];

        data[0] = (byte) ((accel_hw[0] >> 8) & 0xff);
        data[1] = (byte) ((accel_hw[0]) & 0xff);
        data[2] = (byte) ((accel_hw[1] >> 8) & 0xff);
        data[3] = (byte) ((accel_hw[1]) & 0xff);
        data[4] = (byte) ((accel_hw[2] >> 8) & 0xff);
        data[5] = (byte) ((accel_hw[2]) & 0xff);

        try {
            device.write(0x06, data, 0, 6);
        } catch (IOException ex) {
            return false;
        }
        
        return true;
    }
    
    /**
     *  @brief  Reset FIFO read/write pointers.
     *  @return 0 if successful.
     */
    public boolean mpu_reset_fifo() {
        if (st.chip_cfg.sensors == 0)
            return false;

        try {
            byte data = 0;
            device.write(st.reg.int_enable, data);
            device.write(st.reg.fifo_en, data);
            device.write(st.reg.user_ctrl, data);

            if (st.chip_cfg.dmp_on) {
                data = BIT_FIFO_RST | BIT_DMP_RST;
                device.write(st.reg.user_ctrl, data);
                TimeUnit.MILLISECONDS.sleep(50);
                data = (byte) (BIT_DMP_EN | BIT_FIFO_EN);
                if ((st.chip_cfg.sensors & INV_XYZ_COMPASS) != 0)
                    data |= BIT_AUX_IF_EN;
                device.write(st.reg.user_ctrl, data);
                if (st.chip_cfg.int_enable != 0)
                    data = BIT_DMP_INT_EN;
                else
                    data = 0;
                device.write(st.reg.int_enable, data);
                data = 0;
                device.write(st.reg.fifo_en, data);
            } else {
                data = BIT_FIFO_RST;
                device.write(st.reg.user_ctrl, data);
                if ((st.chip_cfg.bypass_mode != 0) || ((st.chip_cfg.sensors & INV_XYZ_COMPASS) == 0))
                    data = BIT_FIFO_EN;
                else
                    data = BIT_FIFO_EN | BIT_AUX_IF_EN;
                device.write(st.reg.user_ctrl, data);
                TimeUnit.MILLISECONDS.sleep(50);
                if (st.chip_cfg.int_enable != 0)
                    data = BIT_DATA_RDY_EN;
                else
                    data = 0;
                device.write(st.reg.int_enable, data);
                device.write(st.reg.fifo_en, (byte) st.chip_cfg.fifo_enable);
            }
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            return false;
        }
        return true;
    }
    
    /**
     *  @brief      Set the gyro full-scale range.
     *  @param[in]  fsr Desired full-scale range.
     *  @return     0 if successful.
     */
    boolean mpu_set_gyro_fsr(int fsr)
    {
        if (st.chip_cfg.sensors == 0)
            return false;
        
        gyro_fsr_e gyro_fsr;
        switch (fsr) {
            case 250:
                gyro_fsr = gyro_fsr_e.INV_FSR_250DPS;
                break;
            case 500:
                gyro_fsr = gyro_fsr_e.INV_FSR_500DPS;
                break;
            case 1000:
                gyro_fsr = gyro_fsr_e.INV_FSR_1000DPS;
                break;
            case 2000:
                gyro_fsr = gyro_fsr_e.INV_FSR_2000DPS;
                break;
            default:
                return false;
        }
        
        if (st.chip_cfg.gyro_fsr == gyro_fsr)
            return true;
        try {
            byte data = (byte)(gyro_fsr.ordinal() << 3);
            device.write(st.reg.gyro_cfg, data);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.gyro_fsr = gyro_fsr;
        return true;
    }
    
    /**
     *  @brief      Get the accel full-scale range.
     *  @param[out] fsr Current full-scale range.
     *  @return     0 if successful.
     */
    boolean mpu_get_accel_fsr(int[] fsr)
    {
        switch (st.chip_cfg.accel_fsr) {
        case INV_FSR_2G:
            fsr[0] = 2;
            break;
        case INV_FSR_4G:
            fsr[0] = 4;
            break;
        case INV_FSR_8G:
            fsr[0] = 8;
            break;
        case INV_FSR_16G:
            fsr[0] = 16;
            break;
        default:
            return false;
        }
        if (st.chip_cfg.accel_half != 0)
            fsr[0] <<= 1;
        return true;
    }
    
    /**
     *  @brief      Set the accel full-scale range.
     *  @param[in]  fsr Desired full-scale range.
     *  @return     0 if successful.
     */
    boolean mpu_set_accel_fsr(int fsr)
    {
        if (st.chip_cfg.sensors == 0)
            return false;
        
        accel_fsr_e accel_fsr;
        switch (fsr) {
            case 2:
                accel_fsr = accel_fsr_e.INV_FSR_2G;
                break;
            case 4:
                accel_fsr = accel_fsr_e.INV_FSR_4G;
                break;
            case 8:
                accel_fsr = accel_fsr_e.INV_FSR_8G;
                break;
            case 16:
                accel_fsr = accel_fsr_e.INV_FSR_16G;
                break;
            default:
                return false;
        }
        
        if (st.chip_cfg.accel_fsr == accel_fsr)
            return true;
        try {
            byte data = (byte)(accel_fsr.ordinal() << 3);
            device.write(st.reg.accel_cfg, data);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.accel_fsr = accel_fsr;
        return true;
    }
    
    /**
     *  @brief      Set digital low pass filter.
     *  The following LPF settings are supported: 188, 98, 42, 20, 10, 5.
     *  @param[in]  lpf Desired LPF setting.
     *  @return     0 if successful.
     */
    boolean mpu_set_lpf(int lpf)
    {
        if (st.chip_cfg.sensors == 0)
            return false;
        
        lpf_e mpu_lpf;
        if (lpf >= 188)
            mpu_lpf = lpf_e.INV_FILTER_188HZ;
        else if (lpf >= 98)
            mpu_lpf = lpf_e.INV_FILTER_98HZ;
        else if (lpf >= 42)
            mpu_lpf = lpf_e.INV_FILTER_42HZ;
        else if (lpf >= 20)
            mpu_lpf = lpf_e.INV_FILTER_20HZ;
        else if (lpf >= 10)
            mpu_lpf = lpf_e.INV_FILTER_10HZ;
        else
            mpu_lpf = lpf_e.INV_FILTER_5HZ;
        
        if (st.chip_cfg.lpf == mpu_lpf)
            return true;
        try {
            byte data = (byte)mpu_lpf.ordinal();
            device.write(st.reg.lpf, data);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.lpf = mpu_lpf;
        return true;
    }
    
    /**
     *  @brief      Set sampling rate.
     *  Sampling rate must be between 4Hz and 1kHz.
     *  @param[in]  rate    Desired sampling rate (Hz).
     *  @return     0 if successful.
     */
    boolean mpu_set_sample_rate(int rate)
    {
        if (st.chip_cfg.sensors == 0)
            return false;
        
        if (st.chip_cfg.dmp_on)
            return false;
        else {
            if (st.chip_cfg.lp_accel_mode) {
                if ((rate != 0) && (rate <= 40)) {
                    /* Just stay in low-power accel mode. */
                    mpu_lp_accel_mode(rate);
                    return true;
                }
                /* Requested rate exceeds the allowed frequencies in LP accel mode,
                 * switch back to full-power mode.
                 */
                mpu_lp_accel_mode(0);
            }
            if (rate < 4)
                rate = 4;
            else if (rate > 1000)
                rate = 1000;
            
            byte data = (byte) (1000 / rate - 1);
            try {
                device.write(st.reg.rate_div, data);
            } catch (IOException ex) {
                return false;
            }
            
            st.chip_cfg.sample_rate = 1000 / (1 + data);
            
            mpu_set_compass_sample_rate(Math.min(st.chip_cfg.compass_sample_rate, MAX_COMPASS_SAMPLE_RATE));
            
            /* Automatically set LPF to 1/2 sampling rate. */
            mpu_set_lpf(st.chip_cfg.sample_rate / 2);
            return true;
        }
    }
    
    /**
     *  @brief      Set compass sampling rate.
     *  The compass on the auxiliary I2C bus is read by the MPU hardware at a
     *  maximum of 100Hz. The actual rate can be set to a fraction of the gyro
     *  sampling rate.
     *
     *  \n WARNING: The new rate may be different than what was requested. Call
     *  mpu_get_compass_sample_rate to check the actual setting.
     *  @param[in]  rate    Desired compass sampling rate (Hz).
     *  @return     0 if successful.
     */
    boolean mpu_set_compass_sample_rate(int rate)
    {
        if (rate == 0 || rate > st.chip_cfg.sample_rate || rate > MAX_COMPASS_SAMPLE_RATE)
            return false;
        
        byte div = (byte)(st.chip_cfg.sample_rate / rate - 1);
        try {
            device.write(st.reg.s4_ctrl, div);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.compass_sample_rate = st.chip_cfg.sample_rate / (div + 1);
        return true;
    }
    
    /**
     *  @brief      Select which sensors are pushed to FIFO.
     *  @e sensors can contain a combination of the following flags:
     *  \n INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
     *  \n INV_XYZ_GYRO
     *  \n INV_XYZ_ACCEL
     *  @param[in]  sensors Mask of sensors to push to FIFO.
     *  @return     0 if successful.
     */
    boolean mpu_configure_fifo(short sensors)
    {
        boolean result = true;
        
        /* Compass data isn't going into the FIFO. Stop trying. */
        sensors &= ~INV_XYZ_COMPASS;
        
        if (st.chip_cfg.dmp_on) {
            return true;
        } else {
            if (st.chip_cfg.sensors == 0) {
                return false;
            }
            short prev = st.chip_cfg.fifo_enable;
            st.chip_cfg.fifo_enable = (short)(sensors & st.chip_cfg.sensors);
            if (st.chip_cfg.fifo_enable != sensors) {
                /* You're not getting what you asked for. Some sensors are
                 * asleep.
                 */
                result = false;
            } else {
                result = true;
            }
            if (sensors != 0 || st.chip_cfg.lp_accel_mode) {
                set_int_enable(true);
            } else {
                set_int_enable(false);
            }
            if (sensors != 0) {
                if (!mpu_reset_fifo()) {
                    st.chip_cfg.fifo_enable = prev;
                    return false;
                }
            }
        }
        
        return result;
    }
    
    
    /**
     *  @brief      Turn specific sensors on/off.
     *  @e sensors can contain a combination of the following flags:
     *  \n INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
     *  \n INV_XYZ_GYRO
     *  \n INV_XYZ_ACCEL
     *  \n INV_XYZ_COMPASS
     *  @param[in]  sensors    Mask of sensors to wake.
     *  @return     0 if successful.
     */
    boolean mpu_set_sensors(short sensors)
    {
        byte data;
        if ((sensors & INV_XYZ_GYRO) != 0)
            data = (byte)clock_sel_e.INV_CLK_PLL.ordinal();
        else if (sensors != 0)
            data = 0;
        else
            data = BIT_SLEEP;
        try {
            device.write(st.reg.pwr_mgmt_1, data);
        } catch (IOException ex) {
            st.chip_cfg.sensors = 0;
            return false;
        }
        st.chip_cfg.clk_src = clock_sel_e.values()[data & ~BIT_SLEEP];
        
        data = 0;
        if ((sensors & INV_X_GYRO) == 0)
            data |= BIT_STBY_XG;
        if ((sensors & INV_Y_GYRO) == 0)
            data |= BIT_STBY_YG;
        if ((sensors & INV_Z_GYRO) == 0)
            data |= BIT_STBY_ZG;
        if ((sensors & INV_XYZ_ACCEL) == 0)
            data |= BIT_STBY_XYZA;
        try {
            device.write(st.reg.pwr_mgmt_2, data);
        } catch (IOException ex) {
            st.chip_cfg.sensors = 0;
            return false;
        }
        
        if ((sensors != 0) && (sensors != INV_XYZ_ACCEL))
            /* Latched interrupts only used in LP accel mode. */
            mpu_set_int_latched(false);
        
        byte user_ctrl;
        try {
            user_ctrl = (byte)device.read(st.reg.user_ctrl);
        } catch (IOException ex) {
            return false;
        }
        /* Handle AKM power management. */
        if ((sensors & INV_XYZ_COMPASS) != 0) {
            data = AKM_SINGLE_MEASUREMENT;
            user_ctrl |= BIT_AUX_IF_EN;
        } else {
            data = AKM_POWER_DOWN;
            user_ctrl &= ~BIT_AUX_IF_EN;
        }
        if (st.chip_cfg.dmp_on)
            user_ctrl |= BIT_DMP_EN;
        else
            user_ctrl &= ~BIT_DMP_EN;
        try {
            device.write(st.reg.s1_do, data);
        } catch (IOException ex) {
            return false;
        }
        /* Enable/disable I2C master mode. */
        try {
            device.write(st.reg.user_ctrl, user_ctrl);
        } catch (IOException ex) {
            return false;
        }
        
        st.chip_cfg.sensors = sensors;
        st.chip_cfg.lp_accel_mode = false;
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException ex) {
        }
        return true;
    }
    
    /**
     *  @brief      Read the MPU interrupt status registers.
     *  @param[out] status  Mask of interrupt bits.
     *  @return     0 if successful.
     */
    public boolean mpu_get_int_status(short[] status)
    {
        if (st.chip_cfg.sensors == 0)
            return false;
        
        byte[] tmp = new byte[2];
        try {
            device.read(st.reg.dmp_int_status, tmp, 0, 2);
        } catch (IOException ex) {
            return false;
        }
        
        ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(status);
        assert status[0] == (short)((tmp[0] << 8) | tmp[1]);
        return true;
    }
    
    /**
     *  @brief      Get one unparsed packet from the FIFO.
     *  This function should be used if the packet is to be parsed elsewhere.
     *  @param[in]  length  Length of one FIFO packet.
     *  @param[in]  data    FIFO packet.
     *  @param[in]  more    Number of remaining packets.
     */
    boolean mpu_read_fifo_stream(byte[] data, short[] more) {
        if (!st.chip_cfg.dmp_on)
            return false;
        if (st.chip_cfg.sensors == 0)
            return false;
        
        byte[] tmp = new byte[2];
        try {
            device.read(st.reg.fifo_count_h, tmp, 0, 2);
        } catch (IOException ex) {
            return false;
        }
        
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(new byte[2]);
        bb.put(tmp);
        int fifo_count = bb.getInt(0);
        assert fifo_count == (((tmp[0]&0xFF) << 8) | (tmp[1]&0xFF));
        
        if (fifo_count < data.length) {
            more[0] = 0;
            return false;
        }
        if (fifo_count > (st.hw.max_fifo/2)) {
            /* FIFO is 50% full, better check overflow bit. */
            tmp = new byte[1];
            try {
                device.read(st.reg.int_status, tmp, 0, 1);
            } catch (IOException ex) {
                return false;
            }
            if ((tmp[0] & BIT_FIFO_OVERFLOW) != 0) {
                mpu_reset_fifo();
                return false;
            }
        }
        
        try {
            device.read(st.reg.fifo_r_w, data, 0, data.length);
        } catch (IOException ex) {
            return false;
        }
        more[0] = (short)((fifo_count / data.length) - 1);
        return true;
    }
    
    /**
     *  @brief      Set device to bypass mode.
     *  @param[in]  bypass_on   1 to enable bypass mode.
     *  @return     0 if successful.
     */
    boolean mpu_set_bypass(short bypass_on)
    {
        if (st.chip_cfg.bypass_mode == bypass_on)
            return true;
        
        try {
            byte tmp;
            if (bypass_on != 0) {
                tmp = (byte)device.read(st.reg.user_ctrl);
                tmp &= ~BIT_AUX_IF_EN;
                device.write(st.reg.user_ctrl, tmp);
                TimeUnit.MILLISECONDS.sleep(3);
                tmp = BIT_BYPASS_EN;
                if (st.chip_cfg.active_low_int)
                    tmp |= BIT_ACTL;
                if (st.chip_cfg.latched_int)
                    tmp |= BIT_LATCH_EN | BIT_ANY_RD_CLR;
                device.write(st.reg.int_pin_cfg, tmp);
            } else {
                /* Enable I2C master mode if compass is being used. */
                tmp = (byte)device.read(st.reg.user_ctrl);
                if ((st.chip_cfg.sensors & INV_XYZ_COMPASS) != 0)
                    tmp |= BIT_AUX_IF_EN;
                else
                    tmp &= ~BIT_AUX_IF_EN;
                device.write(st.reg.user_ctrl, tmp);
                TimeUnit.MILLISECONDS.sleep(3);
                if (st.chip_cfg.active_low_int)
                    tmp = (byte)BIT_ACTL;
                else
                    tmp = 0;
                if (st.chip_cfg.latched_int)
                    tmp |= BIT_LATCH_EN | BIT_ANY_RD_CLR;
                device.write(st.reg.int_pin_cfg, tmp);
            }
            st.chip_cfg.bypass_mode = bypass_on;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            return false;
        }
        return true;
    }
    
    /**
     *  @brief      Enable latched interrupts.
     *  Any MPU register will clear the interrupt.
     *  @param[in]  enable  1 to enable, 0 to disable.
     *  @return     0 if successful.
     */
    boolean mpu_set_int_latched(boolean enable)
    {
        if (st.chip_cfg.latched_int == enable)
            return true;
        
        byte tmp;
        if (enable)
            tmp = BIT_LATCH_EN | BIT_ANY_RD_CLR;
        else
            tmp = 0;
        if (st.chip_cfg.bypass_mode != 0)
            tmp |= BIT_BYPASS_EN;
        if (st.chip_cfg.active_low_int)
            tmp |= BIT_ACTL;
        
        try {
            device.write(st.reg.int_pin_cfg, tmp);
        } catch (IOException ex) {
            return false;
        }
        st.chip_cfg.latched_int = enable;
        return true;
    }
    
    /**
     *  @brief      Write to the DMP memory.
     *  This function prevents I2C writes past the bank boundaries. The DMP memory
     *  is only accessible when the chip is awake.
     *  @param[in]  mem_addr    Memory location (bank << 8 | start address)
     *  @param[in]  length      Number of bytes to write.
     *  @param[in]  data        Bytes to write to memory.
     *  @return     0 if successful.
     */
    boolean mpu_write_mem(short mem_addr, byte[] data, int offset, int length)
    {
        if (data == null)
            return false;
        if (st.chip_cfg.sensors == 0)
            return false;
        
        byte[] tmp = new byte[2];
        ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(mem_addr);
        assert tmp[0] == (byte)(mem_addr >> 8);
        assert tmp[1] == (byte)(mem_addr & 0xFF);
        
        /* Check bank boundaries. */
        if ((tmp[1]&0xFF) + length > st.hw.bank_size)
            return false;
        
        try {
            device.write(st.reg.bank_sel, tmp, 0, 2);
            device.write(st.reg.mem_r_w, data, offset, length);
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    /**
     *  @brief      Read from the DMP memory.
     *  This function prevents I2C reads past the bank boundaries. The DMP memory
     *  is only accessible when the chip is awake.
     *  @param[in]  mem_addr    Memory location (bank << 8 | start address)
     *  @param[in]  length      Number of bytes to read.
     *  @param[out] data        Bytes read from memory.
     *  @return     0 if successful.
     */
    boolean mpu_read_mem(short mem_addr, byte[] data, int offset, int length)
    {
        
        if (data == null)
            return false;
        if (st.chip_cfg.sensors == 0)
            return false;
        
        byte[] tmp = new byte[2];
        ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(mem_addr);
        assert tmp[0] == (byte)(mem_addr >> 8);
        assert tmp[1] == (byte)(mem_addr & 0xFF);
        
        /* Check bank boundaries. */
        if ((tmp[1]&0xFF) + length > st.hw.bank_size)
            return false;
        
        try {
            device.write(st.reg.bank_sel, tmp, 0, 2);
            device.read(st.reg.mem_r_w, data, offset, length);
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    /**
     *  @brief      Load and verify DMP image.
     *  @param[in]  length      Length of DMP image.
     *  @param[in]  firmware    DMP code.
     *  @param[in]  start_addr  Starting address of DMP code memory.
     *  @param[in]  sample_rate Fixed sampling rate used when DMP is enabled.
     *  @return     0 if successful.
     */
    boolean mpu_load_firmware(int length, byte[] firmware, short start_addr, int sample_rate)
    {
        if (st.chip_cfg.dmp_loaded) {
            /* DMP should only be loaded once. */
            return false;
        }
        
        int this_write;
        /* Must divide evenly into st.hw->bank_size to avoid bank crossings. */
        int load_chunk = 16;
        byte[] cur = new byte[load_chunk];
        byte[] tmp = new byte[2];
        
        if (firmware == null) {
            return false;
        }
        for (int ii = 0; ii < length; ii += this_write) {
            this_write = Math.min(load_chunk, length - ii);
            if (!mpu_write_mem((short)ii, firmware, ii, this_write)) {
                return false;
            }
            if (!mpu_read_mem((short)ii, cur, 0, this_write)) {
                return false;
            }
            if (!Arrays.equals(Arrays.copyOfRange(firmware, ii, ii+this_write), 
                    (cur.length == this_write) ? cur : Arrays.copyOfRange(cur, 0, this_write))) {
                return false;
            }
        }
        
        /* Set program start address. */
        ByteBuffer.wrap(tmp).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(start_addr);
        assert tmp[0] == (start_addr >> 8);
        assert tmp[1] == (start_addr & 0xFF);
        try {
            device.write(st.reg.prgm_start_h, tmp, 0, 2);
        } catch (IOException ex) {
            return false;
        }
        
        st.chip_cfg.dmp_loaded = true;
        st.chip_cfg.dmp_sample_rate = sample_rate;
        return true;
    }
    
    /**
     *  @brief      Enable/disable DMP support.
     *  @param[in]  enable  1 to turn on the DMP.
     *  @return     0 if successful.
     */
    boolean mpu_set_dmp_state(boolean enable)
    {
        if (st.chip_cfg.dmp_on == enable)
            return true;
        
        try {
            byte tmp;
            if (enable) {
                if (!st.chip_cfg.dmp_loaded)
                    return false;
                /* Disable data ready interrupt. */
                set_int_enable(false);
                /* Disable bypass mode. */
                mpu_set_bypass((short)0);
                /* Keep constant sample rate, FIFO rate controlled by DMP. */
                mpu_set_sample_rate(st.chip_cfg.dmp_sample_rate);
                /* Remove FIFO elements. */
                tmp = 0;
                device.write(0x23, tmp);
                st.chip_cfg.dmp_on = true;
                /* Enable DMP interrupt. */
                set_int_enable(true);
                mpu_reset_fifo();
            } else {
                /* Disable DMP interrupt. */
                set_int_enable(false);
                /* Restore FIFO settings. */
                tmp = (byte)st.chip_cfg.fifo_enable;
                device.write(0x23, tmp);
                st.chip_cfg.dmp_on = false;
                mpu_reset_fifo();
            }
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    /* This initialization is similar to the one in ak8975.c. */
    private boolean setup_compass()
    {
        byte[] data = new byte[4];
        short akm_addr;
        
        mpu_set_bypass((short)1);
        
        /* Find compass. Possible addresses range from 0x0C to 0x0F. */
        for (akm_addr = 0x0C; akm_addr <= 0x0F; akm_addr++) {
            boolean result = true;
            try {
                I2CDevice dev = bus.getDevice(akm_addr);
                dev.read(AKM_REG_WHOAMI, data, 0, 1);
            } catch (IOException ex) {
                result = false;
            }
            if (result && (data[0] == AKM_WHOAMI))
                break;
        }
        
        if (akm_addr > 0x0F) {
            /* TODO: Handle this case in all compass-related functions. */
            System.out.printf("Compass not found.\n");
            return false;
        }
        
        try {
            st.chip_cfg.compass_addr = akm_addr;
            I2CDevice compassDevice = bus.getDevice(st.chip_cfg.compass_addr);
            
            data[0] = AKM_POWER_DOWN;
            compassDevice.write(AKM_REG_CNTL, data[0]);
            TimeUnit.MILLISECONDS.sleep(1);
            
            data[0] = AKM_FUSE_ROM_ACCESS;
            compassDevice.write(AKM_REG_CNTL, data[0]);
            TimeUnit.MILLISECONDS.sleep(1);
            
            /* Get sensitivity adjustment data from fuse ROM. */
            compassDevice.read(AKM_REG_ASAX, data, 0, 3);
            st.chip_cfg.mag_sens_adj[0] = (short)((data[0] & 0xFF) + 128);
            st.chip_cfg.mag_sens_adj[1] = (short)((data[1] & 0xFF) + 128);
            st.chip_cfg.mag_sens_adj[2] = (short)((data[2] & 0xFF) + 128);
            
            data[0] = AKM_POWER_DOWN;
            compassDevice.write(AKM_REG_CNTL, data[0]);
            TimeUnit.MILLISECONDS.sleep(1);
            
            mpu_set_bypass((short)0);
            
            /* Set up master mode, master clock, and ES bit. */
            data[0] = 0x40;
            device.write(st.reg.i2c_mst, data[0]);
            
            /* Slave 0 reads from AKM data registers. */
            data[0] = (byte) (BIT_I2C_READ | st.chip_cfg.compass_addr);
            device.write(st.reg.s0_addr, data[0]);
            
            /* Compass reads start at this register. */
            data[0] = AKM_REG_ST1;
            device.write(st.reg.s0_reg, data[0]);
            
            /* Enable slave 0, 8-byte reads. */
            data[0] = (byte)(BIT_SLAVE_EN | 8);
            device.write(st.reg.s0_ctrl, data[0]);
            
            /* Slave 1 changes AKM measurement mode. */
            data[0] = (byte)st.chip_cfg.compass_addr;
            device.write(st.reg.s1_addr, data[0]);
            
            /* AKM measurement mode register. */
            data[0] = AKM_REG_CNTL;
            device.write(st.reg.s1_reg, data[0]);
            
            /* Enable slave 1, 1-byte writes. */
            data[0] = (byte)(BIT_SLAVE_EN | 1);
            device.write(st.reg.s1_ctrl, data[0]);
            
            /* Set slave 1 data. */
            data[0] = AKM_SINGLE_MEASUREMENT;
            device.write(st.reg.s1_do, data[0]);
            
            /* Trigger slave 0 and slave 1 actions at each sample. */
            data[0] = 0x03;
            device.write(st.reg.i2c_delay_ctrl, data[0]);
            
            /* For the MPU9150, the auxiliary I2C bus needs to be set to VDD. */
            data[0] = (byte)BIT_I2C_MST_VDDIO;
            device.write(st.reg.yg_offs_tc, data[0]);
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            return false;
        }
        return true;
    }
    
    /**
     *  @brief      Read raw compass data.
     *  @param[out] data        Raw data in hardware units.
     *  @param[out] timestamp   Timestamp in milliseconds. Null if not needed.
     *  @return     0 if successful.
     */
    public boolean mpu_get_compass_reg(short[] data, long[] timestamp)
    {
        byte[] tmp = new byte[8];
        
        if ((st.chip_cfg.sensors & INV_XYZ_COMPASS) == 0)
            return false;
        
        try {
            device.read(st.reg.raw_compass, tmp, 0, 8);
        } catch (IOException ex) {
            return false;
        }
        
        /* AK8975 doesn't have the overrun error bit. */
        if ((tmp[0] & AKM_DATA_READY) == 0)
            return false;
        if ((tmp[7] & AKM_OVERFLOW) != 0 || (tmp[7] & AKM_DATA_ERROR) != 0)
            return false;
        
        ByteBuffer.wrap(tmp, 1, 6).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
        assert data[0] == (short)(((tmp[2]&0xFF) << 8) | (tmp[1]&0xFF));
        assert data[1] == (short)(((tmp[4]&0xFF) << 8) | (tmp[3]&0xFF));
        assert data[2] == (short)(((tmp[6]&0xFF) << 8) | (tmp[5]&0xFF));
        
        data[0] = (short) (((long)data[0] * st.chip_cfg.mag_sens_adj[0]) >> 8);
        data[1] = (short) (((long)data[1] * st.chip_cfg.mag_sens_adj[1]) >> 8);
        data[2] = (short) (((long)data[2] * st.chip_cfg.mag_sens_adj[2]) >> 8);
        
        if (timestamp != null && timestamp.length > 0)
            timestamp[0] = System.currentTimeMillis();

        return true;
    }
}
