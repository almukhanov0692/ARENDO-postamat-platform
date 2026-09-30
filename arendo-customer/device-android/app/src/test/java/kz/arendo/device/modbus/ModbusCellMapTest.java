package kz.arendo.device.modbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class ModbusCellMapTest {
    @Test
    public void exhibitionMapStaysOnOneModuleAndKeepsTenChannels() {
        ModbusCellMap map = ModbusCellMap.exhibition10();

        assertEquals(10, map.getCells().size());
        assertEquals("10", map.getCell(9).getCode());
        assertEquals(1, map.getCell(9).getSlaveId());
        assertFalse(map.getCell(9).hasDoorInput());
        assertTrue(map.getCell(3).hasDoorInput());
        assertEquals(4, map.getCell(3).getInputChannel());
        assertEquals(10, map.getCell(9).getOutputChannel());
    }

    @Test
    public void proposedCell18UsesSecondModuleLocalChannelTwo() {
        ModbusCellMap map = ModbusCellMap.proposedSequential28(1, 2);
        ModbusCellMap.Cell cell18 = map.getCell(17);

        assertEquals("18", cell18.getCode());
        assertEquals(2, cell18.getSlaveId());
        assertEquals(2, cell18.getInputChannel());
        assertEquals(2, cell18.getOutputChannel());
        assertEquals(1, cell18.getInputAddress());
        assertEquals(1, cell18.getOutputAddress());
    }

    @Test
    public void proposed28CellMapUsesBothSixteenPointModulesWithoutInventingSpareDoors() {
        ModbusCellMap map = ModbusCellMap.proposedSequential28(1, 2);

        assertEquals(28, map.getCells().size());

        ModbusCellMap.Cell cell16 = map.getCell(15);
        assertEquals("16", cell16.getCode());
        assertEquals(1, cell16.getSlaveId());
        assertEquals(16, cell16.getInputChannel());
        assertEquals(16, cell16.getOutputChannel());
        assertEquals(15, cell16.getInputAddress());
        assertEquals(15, cell16.getOutputAddress());

        ModbusCellMap.Cell cell17 = map.getCell(16);
        assertEquals("17", cell17.getCode());
        assertEquals(2, cell17.getSlaveId());
        assertEquals(1, cell17.getInputChannel());
        assertEquals(1, cell17.getOutputChannel());

        ModbusCellMap.Cell cell28 = map.getCell(27);
        assertEquals("28", cell28.getCode());
        assertEquals(2, cell28.getSlaveId());
        assertEquals(12, cell28.getInputChannel());
        assertEquals(12, cell28.getOutputChannel());
        assertEquals(11, cell28.getInputAddress());
        assertEquals(11, cell28.getOutputAddress());
    }

    @Test
    public void rejectsDuplicateModuleAddresses() {
        try {
            ModbusCellMap.proposedSequential28(1, 1);
            fail("Expected duplicate slave IDs to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Two Modbus modules need different slave IDs", expected.getMessage());
        }
    }

    @Test
    public void rejectsDuplicatePhysicalOutputMapping() {
        try {
            new ModbusCellMap(Collections.singletonList(new ModbusCellMap.Module("io-1", 1)),
                    Arrays.asList(
                            new ModbusCellMap.Cell("01", 1, 0, 0),
                            new ModbusCellMap.Cell("02", 1, 1, 0)));
            fail("Expected duplicate output address to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Modbus output point is mapped more than once", expected.getMessage());
        }
    }
}
