package kz.arendo.device.modbus;

import java.io.IOException;

public interface SerialTransport {
    void write(byte[] data, int timeoutMs) throws IOException;
    int read(byte[] target, int timeoutMs) throws IOException;
}

