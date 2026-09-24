package kz.arendo.device.modbus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Minimal synchronized Modbus RTU master for discrete inputs and coils. */
public final class ModbusRtuClient {
    private final SerialTransport transport;

    public ModbusRtuClient(SerialTransport transport) {
        this.transport = transport;
    }

    public synchronized boolean[] readDiscreteInputs(int slaveId, int startAddress, int quantity)
            throws IOException {
        checkRange(slaveId, startAddress, quantity);
        int payloadBytes = (quantity + 7) / 8;
        byte[] response = transact(frame(slaveId, 0x02, startAddress, quantity),
                0x02, 5 + payloadBytes);
        int byteCount = response[2] & 0xFF;
        if (byteCount != payloadBytes) {
            throw new IOException("Unexpected input byte count: " + byteCount);
        }
        boolean[] values = new boolean[quantity];
        for (int index = 0; index < quantity; index++) {
            values[index] = ((response[3 + index / 8] >> (index % 8)) & 1) != 0;
        }
        return values;
    }

    public synchronized void writeSingleCoil(int slaveId, int address, boolean enabled)
            throws IOException {
        checkRange(slaveId, address, 1);
        int value = enabled ? 0xFF00 : 0x0000;
        byte[] request = frame(slaveId, 0x05, address, value);
        byte[] response = transact(request, 0x05, 8);
        for (int index = 0; index < 6; index++) {
            if (response[index] != request[index]) {
                throw new IOException("Coil write echo mismatch");
            }
        }
    }

    private byte[] transact(byte[] request, int expectedFunction, int expectedLength)
            throws IOException {
        transport.write(request, 1000);
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        long deadline = System.currentTimeMillis() + 1200;
        int targetLength = expectedLength;
        while (System.currentTimeMillis() < deadline) {
            int count = transport.read(buffer, 100);
            if (count <= 0) {
                continue;
            }
            received.write(buffer, 0, count);
            byte[] current = received.toByteArray();
            if (current.length >= 2 && (current[1] & 0x80) != 0) {
                targetLength = 5;
            } else if (current.length >= 3 && expectedFunction == 0x02) {
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

    static byte[] frame(int slaveId, int function, int address, int value) {
        byte[] frame = new byte[8];
        frame[0] = (byte) slaveId;
        frame[1] = (byte) function;
        frame[2] = (byte) (address >> 8);
        frame[3] = (byte) address;
        frame[4] = (byte) (value >> 8);
        frame[5] = (byte) value;
        int crc = crc16(frame, 0, 6);
        frame[6] = (byte) crc;
        frame[7] = (byte) (crc >> 8);
        return frame;
    }

    static int crc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int index = offset; index < offset + length; index++) {
            crc ^= data[index] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xA001 : crc >> 1;
            }
        }
        return crc & 0xFFFF;
    }

    private static void validate(byte[] response, int expectedFunction) throws IOException {
        if (response.length < 5) {
            throw new IOException("Short Modbus response");
        }
        int function = response[1] & 0xFF;
        if ((function & 0x80) != 0) {
            throw new IOException("Modbus exception: " + (response[2] & 0xFF));
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

    private static void checkRange(int slaveId, int address, int quantity) {
        if (slaveId < 1 || slaveId > 247) {
            throw new IllegalArgumentException("slaveId must be 1..247");
        }
        if (address < 0 || address > 0xFFFF || quantity < 1 || quantity > 2000
                || address + quantity > 0x10000) {
            throw new IllegalArgumentException("Invalid Modbus address range");
        }
    }
}

