package net.minecraft.util;

public class SampleLogger {
    public static final int CAPACITY = 240;
    private final long[] samples = new long[240];
    private int start;
    private int size;

    public void logSample(long pSample) {
        int i = this.wrapIndex(this.start + this.size);
        this.samples[i] = pSample;
        if (this.size < 240) {
            ++this.size;
        } else {
            this.start = this.wrapIndex(this.start + 1);
        }
    }

    public int capacity() {
        return this.samples.length;
    }

    public int size() {
        return this.size;
    }

    public long get(int pIndex) {
        if (pIndex >= 0 && pIndex < this.size) {
            return this.samples[this.wrapIndex(this.start + pIndex)];
        } else {
            throw new IndexOutOfBoundsException(pIndex + " out of bounds for length " + this.size);
        }
    }

    private int wrapIndex(int pIndex) {
        return pIndex % 240;
    }

    public void reset() {
        this.start = 0;
        this.size = 0;
    }
}
