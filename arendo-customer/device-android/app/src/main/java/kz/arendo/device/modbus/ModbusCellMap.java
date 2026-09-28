package kz.arendo.device.modbus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validated mapping from logical cells to per-module Modbus points. */
public final class ModbusCellMap {
    public static final int BSM_CHANNEL_COUNT = 16;
    public static final int NO_INPUT = -1;

    public static final class Module {
        private final String key;
        private final int slaveId;

        public Module(String key, int slaveId) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Module key must not be empty");
            }
            if (slaveId < 1 || slaveId > 247) {
                throw new IllegalArgumentException("slaveId must be 1..247");
            }
            this.key = key;
            this.slaveId = slaveId;
        }

        public String getKey() {
            return key;
        }

        public int getSlaveId() {
            return slaveId;
        }
    }

    public static final class Cell {
        private final String code;
        private final int slaveId;
        private final int inputAddress;
        private final int outputAddress;

        public Cell(String code, int slaveId, int inputAddress, int outputAddress) {
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Cell code must not be empty");
            }
            checkInputAddress(inputAddress);
            checkPointAddress(outputAddress);
            this.code = code;
            this.slaveId = slaveId;
            this.inputAddress = inputAddress;
            this.outputAddress = outputAddress;
        }

        public String getCode() {
            return code;
        }

        public int getSlaveId() {
            return slaveId;
        }

        /** Zero-based Modbus input point; X1 is address 0. */
        public int getInputAddress() {
            return inputAddress;
        }

        /** Zero-based Modbus output point; Y1 is address 0. */
        public int getOutputAddress() {
            return outputAddress;
        }

        public int getInputChannel() {
            return hasDoorInput() ? inputAddress + 1 : 0;
        }

        public boolean hasDoorInput() {
            return inputAddress != NO_INPUT;
        }

        public int getOutputChannel() {
            return outputAddress + 1;
        }
    }

    private final List<Module> modules;
    private final List<Cell> cells;
    private final Map<Integer, Module> modulesBySlaveId;

    public ModbusCellMap(List<Module> modules, List<Cell> cells) {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("At least one Modbus module is required");
        }
        if (cells == null || cells.isEmpty()) {
            throw new IllegalArgumentException("At least one cell is required");
        }

        Map<Integer, Module> bySlave = new HashMap<>();
        Set<String> moduleKeys = new HashSet<>();
        for (Module module : modules) {
            if (module == null || bySlave.put(module.getSlaveId(), module) != null) {
                throw new IllegalArgumentException("Modbus module slave IDs must be unique");
            }
            if (!moduleKeys.add(module.getKey())) {
                throw new IllegalArgumentException("Modbus module keys must be unique");
            }
        }

        Set<String> cellCodes = new HashSet<>();
        Set<String> inputPoints = new HashSet<>();
        Set<String> outputPoints = new HashSet<>();
        Set<Integer> referencedSlaves = new HashSet<>();
        for (Cell cell : cells) {
            if (cell == null || !bySlave.containsKey(cell.getSlaveId())) {
                throw new IllegalArgumentException("Cell references an unknown Modbus module");
            }
            referencedSlaves.add(cell.getSlaveId());
            if (!cellCodes.add(cell.getCode())) {
                throw new IllegalArgumentException("Cell codes must be unique");
            }
            if (cell.hasDoorInput()
                    && !inputPoints.add(pointKey(cell.getSlaveId(), cell.getInputAddress()))) {
                throw new IllegalArgumentException("Modbus input point is mapped more than once");
            }
            if (!outputPoints.add(pointKey(cell.getSlaveId(), cell.getOutputAddress()))) {
                throw new IllegalArgumentException("Modbus output point is mapped more than once");
            }
        }
        if (referencedSlaves.size() != bySlave.size()) {
            throw new IllegalArgumentException("Every Modbus module must have at least one cell");
        }

        this.modules = Collections.unmodifiableList(new ArrayList<>(modules));
        this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
        this.modulesBySlaveId = Collections.unmodifiableMap(bySlave);
    }

    /** Current BSM-0410RB demo: ten outputs, with the four available inputs on D01-D04. */
    public static ModbusCellMap exhibition10() {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            int inputAddress = index < 4 ? index : NO_INPUT;
            cells.add(new Cell(twoDigits(index + 1), 1, inputAddress, index));
        }
        return new ModbusCellMap(Collections.singletonList(new Module("io-1", 1)), cells);
    }

    /**
     * Proposed, not commissioned: D01-D16 on module 1 and D17-D28 on module 2.
     * Use only as a routing/test fixture until the physical cabinet map is approved.
     */
    public static ModbusCellMap proposedSequential28(int firstSlaveId, int secondSlaveId) {
        if (firstSlaveId == secondSlaveId) {
            throw new IllegalArgumentException("Two Modbus modules need different slave IDs");
        }
        List<Module> modules = new ArrayList<>();
        modules.add(new Module("io-1", firstSlaveId));
        modules.add(new Module("io-2", secondSlaveId));

        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < 28; index++) {
            int moduleIndex = index < BSM_CHANNEL_COUNT ? 0 : 1;
            int localAddress = index < BSM_CHANNEL_COUNT
                    ? index : index - BSM_CHANNEL_COUNT;
            int slaveId = modules.get(moduleIndex).getSlaveId();
            cells.add(new Cell(twoDigits(index + 1), slaveId, localAddress, localAddress));
        }
        return new ModbusCellMap(modules, cells);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public Cell getCell(int index) {
        return cells.get(index);
    }

    public Module getModule(int slaveId) {
        return modulesBySlaveId.get(slaveId);
    }

    private static void checkPointAddress(int address) {
        if (address < 0 || address >= BSM_CHANNEL_COUNT) {
            throw new IllegalArgumentException("BSM point address must be 0..15");
        }
    }

    private static void checkInputAddress(int address) {
        if (address != NO_INPUT) {
            checkPointAddress(address);
        }
    }

    private static String pointKey(int slaveId, int address) {
        return slaveId + ":" + address;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
