package kz.arendo.device.modbus;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

public final class UsbSerialTransport implements SerialTransport {
    private final UsbSerialPort port;

    public UsbSerialTransport(UsbSerialPort port) {
        this.port = port;
    }

    @Override
    public void write(byte[] data, int timeoutMs) throws IOException {
        port.write(data, timeoutMs);
    }

    @Override
    public int read(byte[] target, int timeoutMs) throws IOException {
        return port.read(target, timeoutMs);
    }
}

