package com.siheal.usbserialport.parser

import com.hd.serialport.config.MeasureStatus
import com.hd.serialport.usb_driver.UsbSerialPort
import com.hd.serialport.utils.L
import com.siheal.usbserialport.device.Device
import com.siheal.usbserialport.result.ParserResult
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by hd on 2017/8/28 .
 * parse the data from the usb port (or serial port)
 */
abstract class Parser {

    protected var readThread: Thread? = null

    protected var writeThread: Thread? = null

    protected val writeComplete = AtomicBoolean(false)

    protected var device: Device? = null

    protected var port: UsbSerialPort? = null

    protected var devicePath:String?=null

    protected val buffer = ByteBuffer.allocate(16 * 1024)!!

    fun parser(device: Device) {
        this.device = device
        if (readThread == null) readThread = Thread(Runnable { reading(device) })
        readThread!!.start()
        if (writeThread == null) writeThread = Thread(Runnable { writing(device) })
        writeThread!!.start()
    }

    private fun writing(device: Device) {
        while (device.status == MeasureStatus.RUNNING && !writeComplete.get()) {
             asyncWrite()
        }
        L.d("writing thread stop :"+device.status+"="+writeComplete.get())
    }

    private fun reading(device: Device) {
        while (device.status == MeasureStatus.RUNNING) {
            val entity = device.dataQueue.take()
            port = entity.port
            devicePath = entity.path
            buffer.put(entity.data)
            parser(entity.data)
        }
        L.d("reading thread stop")
    }

    fun write(byteArray: ByteArray, delay: Long = 0) {
        device?.write(byteArray, delay)
    }

    fun writeInitializationInstructAgain(delay: Long = 0) {
        device?.write(device!!.initializationInstruct(), delay)
    }

    fun error(msg: String?=null) {
        device?.error(msg)
        clear()
    }

    fun complete(result: ParserResult, stop: Boolean) {
        device?.complete(result, stop)
        if (stop){
            clear()
            saveDevice()
        }
    }

    private fun saveDevice() {
        L.d("save device :"+device?.aioDeviceType+"="+port+"="+devicePath)
    }

    private fun clear() {
        buffer.clear()
        if (!readThread!!.isAlive && !readThread!!.isInterrupted)
            readThread!!.interrupt()
        readThread = null
        if (!writeThread!!.isAlive && !writeThread!!.isInterrupted)
            writeThread!!.interrupt()
        writeThread = null
    }

    /**
     * allows asynchronous persistence parsing
     * parser complete please call [complete]
     * parser error please call [error]
     */
    abstract fun parser(data: ByteArray)

    /**
     * allow asynchronous to be written{[write] or [writeInitializationInstructAgain]} all the time
     */
    open fun asyncWrite() {}
}