package org.telegram.messenger;

public class TenseiNetBoost {
    public static final int DL_SMALL = 1024 * 128;
    public static final int DL_BIG = 1024 * 512;
    public static final int DL_PAR = 8;
    public static final int DL_PAR_BIG = 16;
    public static final int UL_PAR = 8;

    public static int uploadChunkKb(long totalBytes, long maxParts) {
        try {
            long need = (totalBytes + 1024L * maxParts - 1) / (1024L * maxParts);
            if (need <= 0) {
                need = 1;
            }
            if (need <= 512) {
                return 1024;
            }
            if (need <= 1024) {
                return 1024;
            }
            long chunk = 1024;
            while (need > chunk && chunk < 8192) {
                chunk *= 2;
            }
            return (int) chunk;
        } catch (Throwable ignore) {
            return 512;
        }
    }
}
