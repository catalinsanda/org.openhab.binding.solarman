package org.openhab.binding.solarman.internal.modbus;

/**
 * @author Catalin Sanda - Initial contribution
 */
public class CRC16Modbus {
    private static final int[] CRC_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = crc >>> 1;
                }
            }
            CRC_TABLE[i] = crc;
        }
    }

    public static int calculate(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ b) & 0xFF];
        }
        return crc;
    }
}
