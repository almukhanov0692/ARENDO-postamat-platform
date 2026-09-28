package kz.arendo.device.modbus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ModbusIoGatewayTest {
    @Test
    public void bsm0410DemoReadsOnlyItsFourAvailableDoorInputs() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        ModbusCellMap map = ModbusCellMap.exhibition10();
        ModbusIoGateway gateway = new ModbusIoGateway(new ModbusRtuClient(transport), map);

        boolean[] inputs = gateway.readCellInputs();

        assertEquals(10, inputs.length);
        assertTrue(map.getCell(0).hasDoorInput());
        assertTrue(map.getCell(3).hasDoorInput());
        assertFalse(map.getCell(4).hasDoorInput());
        assertArrayEquals(ModbusRtuClient.frame(1, 0x02, 0, 4), transport.writes.get(0));
    }

    @Test
    public void readsEachModuleAndMapsCell18InputFromSecondModule() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        ModbusIoGateway gateway = new ModbusIoGateway(new ModbusRtuClient(transport),
                ModbusCellMap.proposedSequential28(1, 2));

        boolean[] inputs = gateway.readCellInputs();

        assertFalse(inputs[16]);
        assertTrue(inputs[17]);
        assertFalse(inputs[27]);
        assertArrayEquals(ModbusRtuClient.frame(1, 0x02, 0, 16), transport.writes.get(0));
        assertArrayEquals(ModbusRtuClient.frame(2, 0x02, 0, 12), transport.writes.get(1));
    }

    @Test
    public void cell18OutputWriteTargetsSlaveTwoChannelTwo() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        ModbusIoGateway gateway = new ModbusIoGateway(new ModbusRtuClient(transport),
                ModbusCellMap.proposedSequential28(1, 2));

        gateway.writeCellOutput(17, true);

        assertArrayEquals(ModbusRtuClient.frame(2, 0x05, 1, 0xFF00), transport.writes.get(0));
    }

    @Test
    public void readsRelayStateForCell18FromSecondModuleSeparatelyFromDoorInputs()
            throws Exception {
        RecordingTransport transport = new RecordingTransport();
        ModbusIoGateway gateway = new ModbusIoGateway(new ModbusRtuClient(transport),
                ModbusCellMap.proposedSequential28(1, 2));

        boolean[] outputs = gateway.readCellOutputStates();

        assertFalse(outputs[16]);
        assertTrue(outputs[17]);
        assertFalse(outputs[27]);
        assertArrayEquals(ModbusRtuClient.frame(1, 0x01, 0, 16), transport.writes.get(0));
        assertArrayEquals(ModbusRtuClient.frame(2, 0x01, 0, 12), transport.writes.get(1));
    }

    private static final class RecordingTransport implements SerialTransport {
        private final List<byte[]> writes = new ArrayList<>();
        private byte[] response;
        private boolean delivered;

        @Override
        public void write(byte[] data, int timeoutMs) {
            byte[] request = data.clone();
            writes.add(request);
            delivered = false;
            int function = request[1] & 0xFF;
            if (function == 0x01 || function == 0x02) {
                int slaveId = request[0] & 0xFF;
                int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
                int dataLength = (quantity + 7) / 8;
                byte[] body = new byte[3 + dataLength];
                body[0] = request[0];
                body[1] = request[1];
                body[2] = (byte) dataLength;
                if (slaveId == 2) {
                    body[3] = 0x02; // Module 2, local point 2 is active.
                }
                response = withCrc(body);
            } else if (function == 0x05) {
                response = request;
            } else {
                throw new AssertionError("Unexpected Modbus function " + function);
            }
        }

        @Override
        public int read(byte[] target, int timeoutMs) throws IOException {
            if (delivered) {
                return 0;
            }
            System.arraycopy(response, 0, target, 0, response.length);
            delivered = true;
            return response.length;
        }

        private static byte[] withCrc(byte[] body) {
            byte[] result = new byte[body.length + 2];
            System.arraycopy(body, 0, result, 0, body.length);
            int crc = ModbusRtuClient.crc16(body, 0, body.length);
            result[body.length] = (byte) crc;
            result[body.length + 1] = (byte) (crc >> 8);
            return result;
        }
    }
}
