package com.example.postamatmodbus;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Small Modbus RTU master for the demo board. Addresses are zero-based. */
public final class ModbusRtuClient {
    private final UsbSerialPort port;

    public ModbusRtuClient(UsbSerialPort port) {
        this.port = port;
    }

    public boolean[] readDiscreteInputs(int slaveId, int startAddress, int quantity)
            throws IOException {
        byte[] response = transact(
                frame(slaveId, 0x02, (startAddress >> 8) & 0xFF, startAddress & 0xFF,
                        (quantity >> 8) & 0xFF, quantity & 0xFF),
                0x02, 5 + ((quantity + 7) / 8));

        int byteCount = response[2] & 0xFF;
        int expectedBytes = (quantity + 7) / 8;
        if (byteCount != expectedBytes) {
            throw new IOException("Unexpected input byte count: " + byteCount);
        }

        boolean[] values = new boolean[quantity];
        for (int i = 0; i < quantity; i++) {
            values[i] = ((response[3 + (i / 8)] >> (i % 8)) & 1) != 0;
        }
        return values;
    }

    public void writeSingleCoil(int slaveId, int address, boolean enabled) throws IOException {
        int value = enabled ? 0xFF00 : 0x0000;
        byte[] response = transact(
                frame(slaveId, 0x05, (address >> 8) & 0xFF, address & 0xFF,
                        (value >> 8) & 0xFF, value & 0xFF),
                0x05, 8);

        for (int i = 0; i < 6; i++) {
            if (response[i] != requestByte(slaveId, address, value, i)) {
                throw new IOException("Relay write echo mismatch");
            }
        }
    }

    private byte requestByte(int slaveId, int address, int value, int index) {
        int[] bytes = {
                slaveId & 0xFF, 0x05, (address >> 8) & 0xFF, address & 0xFF,
                (value >> 8) & 0xFF, value & 0xFF
        };
        return (byte) bytes[index];
    }

    private byte[] transact(byte[] request, int expectedFunction, int expectedLength)
            throws IOException {
        port.write(request, 1000);

        ByteArrayOutputStream received = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        long deadline = System.currentTimeMillis() + 1200;
        int targetLength = expectedLength;

        while (System.currentTimeMillis() < deadline) {
            int count = port.read(buffer, 100);
            if (count <= 0) {
                continue;
            }
            received.write(buffer, 0, count);
            byte[] current = received.toByteArray();
            if (current.length >= 2 && (current[1] & 0x80) != 0) {
                targetLength = 5;
            } else if (current.length >= 3 && (current[1] & 0xFF) == expectedFunction
                    && expectedFunction == 0x02) {
                targetLength = 5 + (current[2] & 0xFF);
            }
            if (current.length >= targetLength) {
                byte[] response = new byte[targetLength];
                System.arraycopy(current, 0, response, 0, targetLength);
                validate(response, expectedFunction);
                return response;
            }
        }
        throw new IOException("Modbus response timeout");
    }

    private void validate(byte[] response, int expectedFunction) throws IOException {
        if (response.length < 5) {
            throw new IOException("Short Modbus response");
        }
        int function = response[1] & 0xFF;
        if ((function & 0x80) != 0) {
            throw new IOException("Modbus exception: code " + (response[2] & 0xFF));
        }
        if (function != expectedFunction) {
            throw new IOException("Unexpected Modbus function: " + function);
        }
        int receivedCrc = (response[response.length - 2] & 0xFF)
                | ((response[response.length - 1] & 0xFF) << 8);
        int calculatedCrc = crc16(response, 0, response.length - 2);
        if (receivedCrc != calculatedCrc) {
            throw new IOException("Modbus CRC mismatch");
        }
    }

    private byte[] frame(int slaveId, int function, int a, int b, int c, int d) {
        byte[] frame = new byte[8];
        frame[0] = (byte) slaveId;
        frame[1] = (byte) function;
        frame[2] = (byte) a;
        frame[3] = (byte) b;
        frame[4] = (byte) c;
        frame[5] = (byte) d;
        int crc = crc16(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    private int crc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= data[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xA001 : crc >> 1;
            }
        }
        return crc & 0xFFFF;
    }
}
