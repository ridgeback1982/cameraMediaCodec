package com.android.testtool;

import java.nio.ByteBuffer;

public final class BitBufferLite {

	private final ByteBuffer mBuffer;
	private int mAvailableBits;
	private int mRestBits;
	 
	public BitBufferLite(final ByteBuffer buffer) {
		mBuffer = buffer;
		mAvailableBits = 0;
		mRestBits = 0;
	}
	 
	public boolean getBit() {
		return getBits(1) != 0;
	}
	 
	public int getBits(final int nBits) {
		if (nBits < 0 || nBits > 32) {
			throw new IllegalArgumentException();
		}
		if (nBits == 0) {
			return 0;
		}
		 
		long bits = mRestBits;
		int collected = mAvailableBits;
		while (collected < nBits) {
			bits = (bits << 8) | (mBuffer.get() & 0xFF);
			collected += 8;
		}
		
		mAvailableBits = collected - nBits;
		assert mAvailableBits < 8;
		final int result = (int) (bits >> mAvailableBits);
		mRestBits = (int) (bits & ((1 << mAvailableBits) - 1));
		return result;
	}
}