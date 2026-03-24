package com.atruedev.bletoolkit.registry

import com.atruedev.kmpble.scanner.uuidFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class BluetoothUuidNamesTest {

    @Test
    fun heartRateServiceNameResolved() {
        val name = BluetoothUuidNames.serviceName(uuidFrom("180d"))
        assertEquals("Heart Rate", name)
    }

    @Test
    fun batteryServiceNameResolved() {
        val name = BluetoothUuidNames.serviceName(uuidFrom("180f"))
        assertEquals("Battery", name)
    }

    @Test
    fun deviceInfoServiceNameResolved() {
        val name = BluetoothUuidNames.serviceName(uuidFrom("180a"))
        assertEquals("Device Information", name)
    }

    @Test
    fun unknownServiceReturnsNull() {
        val name = BluetoothUuidNames.serviceName(uuidFrom("ffff"))
        assertNull(name)
    }

    @Test
    fun heartRateMeasurementCharacteristicResolved() {
        val name = BluetoothUuidNames.characteristicName(uuidFrom("2a37"))
        assertEquals("Heart Rate Measurement", name)
    }

    @Test
    fun batteryLevelCharacteristicResolved() {
        val name = BluetoothUuidNames.characteristicName(uuidFrom("2a19"))
        assertEquals("Battery Level", name)
    }

    @Test
    fun unknownCharacteristicReturnsNull() {
        val name = BluetoothUuidNames.characteristicName(uuidFrom("ffff"))
        assertNull(name)
    }

    @Test
    fun nordicCompanyNameResolved() {
        val name = BluetoothUuidNames.companyName(0x0059)
        assertEquals("Nordic Semiconductor", name)
    }

    @Test
    fun appleCompanyNameResolved() {
        val name = BluetoothUuidNames.companyName(0x004C)
        assertEquals("Apple", name)
    }

    @Test
    fun unknownCompanyReturnsNull() {
        val name = BluetoothUuidNames.companyName(0xFFFF)
        assertNull(name)
    }

    @Test
    fun cccdDescriptorNameResolved() {
        val name = BluetoothUuidNames.descriptorName(uuidFrom("2902"))
        assertEquals("Client Characteristic Configuration", name)
    }

    @Test
    fun userDescriptionDescriptorNameResolved() {
        val name = BluetoothUuidNames.descriptorName(uuidFrom("2901"))
        assertEquals("Characteristic User Description", name)
    }

    @Test
    fun unknownDescriptorReturnsNull() {
        val name = BluetoothUuidNames.descriptorName(uuidFrom("ffff"))
        assertNull(name)
    }
}
