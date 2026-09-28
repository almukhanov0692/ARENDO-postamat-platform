package kz.arendo.device.modbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Routes logical cell I/O to the correct Modbus module and local channel. */
public final class ModbusIoGateway {
    private final ModbusRtuClient client;
    private final ModbusCellMap map;

    public ModbusIoGateway(ModbusRtuClient client, ModbusCellMap map) {
        if (client == null || map == null) {
            throw new IllegalArgumentException("Modbus client and cell map are required");
        }
        this.client = client;
        this.map = map;
    }

    /** Reads each configured module once, then returns inputs in logical cell order. */
    public synchronized boolean[] readCellInputs() throws IOException {
        return readMappedPoints(true);
    }

    /** Reads relay output states for diagnostics, not physical door-state reporting. */
    public synchronized boolean[] readCellOutputStates() throws IOException {
        return readMappedPoints(false);
    }

    private boolean[] readMappedPoints(boolean inputs) throws IOException {
        Map<Integer, boolean[]> modulePoints = new HashMap<>();
        for (ModbusCellMap.Module module : map.getModules()) {
            int pointCount = pointCountForModule(module.getSlaveId(), inputs);
            if (pointCount == 0) {
                modulePoints.put(module.getSlaveId(), new boolean[0]);
                continue;
            }
            boolean[] points = inputs
                    ? client.readDiscreteInputs(module.getSlaveId(), 0, pointCount)
                    : client.readCoils(module.getSlaveId(), 0, pointCount);
            modulePoints.put(module.getSlaveId(), points);
        }

        boolean[] cellStates = new boolean[map.getCells().size()];
        for (int index = 0; index < cellStates.length; index++) {
            ModbusCellMap.Cell cell = map.getCell(index);
            if (inputs && !cell.hasDoorInput()) {
                continue;
            }
            int address = inputs ? cell.getInputAddress() : cell.getOutputAddress();
            cellStates[index] = modulePoints.get(cell.getSlaveId())[address];
        }
        return cellStates;
    }

    public synchronized void writeCellOutput(int cellIndex, boolean enabled) throws IOException {
        ModbusCellMap.Cell cell = map.getCell(cellIndex);
        client.writeSingleCoil(cell.getSlaveId(), cell.getOutputAddress(), enabled);
    }

    public ModbusCellMap getMap() {
        return map;
    }

    private int pointCountForModule(int slaveId, boolean inputs) {
        int pointCount = 0;
        for (ModbusCellMap.Cell cell : map.getCells()) {
            if (cell.getSlaveId() == slaveId) {
                int address = inputs ? cell.getInputAddress() : cell.getOutputAddress();
                pointCount = Math.max(pointCount, address + 1);
            }
        }
        return pointCount;
    }
}
