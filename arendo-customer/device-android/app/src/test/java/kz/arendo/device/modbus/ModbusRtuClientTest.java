package kz.arendo.device.modbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public final class ModbusRtuClientTest {
    @Test
    public void crcMatchesKnownReadRequest() {
        byte[] request = {0x01, 0x02, 0x00, 0x00, 0x00, 0x04};
        assertEquals(0xC979, ModbusRtuClient.crc16(request, 0, request.length));
    }

    @Test
    public void frameUsesLowCrcByteFirst() {
        assertArrayEquals(new byte[] {0x01, 0x02, 0x00, 0x00, 0x00, 0x04,
                        (byte) 0x79, (byte) 0xC9},
                ModbusRtuClient.frame(1, 0x02, 0, 4));
    }

    @Test
    public void readsFourDiscreteInputsFromMockTransport() throws Exception {
        byte[] body = {0x01, 0x02, 0x01, 0x05};
        byte[] response = withCrc(body);
        FakeTransport transport = new FakeTransport(response);

        boolean[] values = new ModbusRtuClient(transport).readDiscreteInputs(1, 0, 4);

        assertTrue(values[0]);
        assertFalse(values[1]);
        assertTrue(values[2]);
        assertFalse(values[3]);
        assertArrayEquals(ModbusRtuClient.frame(1, 0x02, 0, 4), transport.lastWrite);
    }

    @Test
    public void writesSingleCoilAndValidatesEcho() throws Exception {
        byte[] response = ModbusRtuClient.frame(1, 0x05, 2, 0xFF00);
        FakeTransport transport = new FakeTransport(response);

        new ModbusRtuClient(transport).writeSingleCoil(1, 2, true);

        assertArrayEquals(response, transport.lastWrite);
    }

    private static byte[] withCrc(byte[] body) {
        byte[] result = new byte[body.length + 2];
        System.arraycopy(body, 0, result, 0, body.length);
        int crc = ModbusRtuClient.crc16(body, 0, body.length);
        result[body.length] = (byte) crc;
        result[body.length + 1] = (byte) (crc >> 8);
        return result;
    }

    private static final class FakeTransport implements SerialTransport {
        private final byte[] response;
        private boolean delivered;
        private byte[] lastWrite;

        private FakeTransport(byte[] response) {
            this.response = response;
        }

        @Override
        public void write(byte[] data, int timeoutMs) {
            lastWrite = data.clone();
        }

        @Override
        public int read(byte[] target, int timeoutMs) throws IOException {
            if (delivered) {
                return 0;
            }
            if (response.length > target.length) {
                throw new IOException("Test response is too long");
            }
            System.arraycopy(response, 0, target, 0, response.length);
            delivered = true;
            return response.length;
        }
    }
}
