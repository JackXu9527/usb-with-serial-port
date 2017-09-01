/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hd.serialport.usb_driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.Keep;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike wakerly (opensource@hoho.com)
 */
public class UsbSerialProber {

    private final ProbeTable mProbeTable;

    public UsbSerialProber(ProbeTable probeTable) {
        mProbeTable = probeTable;
    }

    public static UsbSerialProber getDefaultProber() {
        return new UsbSerialProber(getDefaultProbeTable());
    }

    private static ProbeTable getDefaultProbeTable() {
        final ProbeTable probeTable = new ProbeTable();
        probeTable.addDriver(CdcAcmSerialDriver.class);//cdc
        probeTable.addDriver(Cp21xxSerialDriver.class);//cp21xx系列
        probeTable.addDriver(FtdiSerialDriver.class);//ftd
        probeTable.addDriver(ProlificSerialDriver.class);//pl2303
        probeTable.addDriver(Ch34xSerialDriver.class);//ch340
        return probeTable;
    }

    /**
     * Finds and builds all possible {@link UsbSerialDriver UsbSerialDrivers}
     * from the currently-attached {@link UsbDevice} hierarchy. This method does
     * not require permission from the Android USB system, since it does not
     * open any of the devices.
     *
     * @param usbManager
     * @return a list, possibly empty, of all compatible drivers
     */
    @Keep
    public List<UsbSerialDriver> findAllDrivers(final UsbManager usbManager) {
        final List<UsbSerialDriver> result = new ArrayList<UsbSerialDriver>();
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice usbDevice = deviceIterator.next();
            UsbSerialDriver driver = probeDevice(usbDevice);
            if (driver != null) {
                result.add(driver);
            }
        }
        return result;
    }

    /**
     * Probes a single device for a compatible driver.
     *
     * @param usbDevice the usb device to probe
     *
     * @return a new {@link UsbSerialDriver} compatible with this device, or
     * {@code null} if none available.
     */
    @Keep
    public UsbSerialDriver probeDevice(final UsbDevice usbDevice) {
        final int vendorId = usbDevice.getVendorId();
        final int productId = usbDevice.getProductId();
        final Class<? extends UsbSerialDriver> driverClass = mProbeTable.findDriver(vendorId, productId);
        if (driverClass != null) {
            final UsbSerialDriver driver;
            try {
                final Constructor<? extends UsbSerialDriver> constructor = driverClass.getConstructor(UsbDevice.class);
                driver = constructor.newInstance(usbDevice);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return driver;
        }
        return null;
    }
}