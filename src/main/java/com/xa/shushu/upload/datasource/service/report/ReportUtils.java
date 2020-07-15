package com.xa.shushu.upload.datasource.service.report;

public class ReportUtils {

    private static Report INSTANCE = new Report();

    /**
     * 统计读取文件字节数
     *
     * @param byteAmount
     */
    public static void readBytes(int byteAmount, long time) {
        INSTANCE.totalByte.add(byteAmount);
        INSTANCE.nanoTime = time;

        long curSec = System.currentTimeMillis() / 1000;

        if (curSec != INSTANCE.timestamp) {
            if (INSTANCE.secondReadByte > INSTANCE.maxReadByte) {
                INSTANCE.maxReadByte = INSTANCE.secondReadByte;
            }
            INSTANCE.secondReadByte = 0;
        }
        INSTANCE.secondReadByte += byteAmount;
    }

    public static void rows(int rowsNum) {
        INSTANCE.rows += rowsNum;
    }

    public static void http(int size, long cost) {
        INSTANCE.totalPost += size;
        INSTANCE.totalCost += cost;
        INSTANCE.times += 1;
        INSTANCE.avgCost = INSTANCE.totalCost / Math.max(1, INSTANCE.times);
        INSTANCE.maxCost = (int) Math.max(cost, INSTANCE.maxCost);
        INSTANCE.minCost = (int) Math.min(cost, INSTANCE.minCost);
    }

    public static Report get() {
        return INSTANCE;
    }
}
