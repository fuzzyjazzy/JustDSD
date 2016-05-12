package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

/**
 * WSD format is defined by 1-bit Consortium.
 * WSD file format specification ver1.1 (in Japanese) is available
 * http://1bitcons.acoust.ias.sci.waseda.ac.jp/pdf/wsd_file_format_ver1_1.pdf
 * 
 * This class is based on DFFFormat written by D. Rogatkin.
 * 
 * @author J. Fujimori
 * @version 1.0 2016-04-03
 */
public class WSDFormat extends DSDFormat<byte[]> {
	Chunk1bit frm;
	byte buff[];
	int block = 2048*2; //This buffer size should match the DoPDAC's
	long filePosition;

	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream, metadataCharset);
		if (c instanceof Chunk1bit == false)
			throw new DecodeException("Invalid .wsd format, no 1bit chunk", null);
		frm = (Chunk1bit) c;
	}


	@Override
	boolean readDataBlock() throws DecodeException {
		try {
			if (filePosition >= frm.dataEnd)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;

			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int toRead = block * getNumChannels();
			if (toRead > frm.dataEnd - filePosition)
				toRead = (int) (frm.dataEnd - filePosition);
			dsdStream.readFully(buff, delta, toRead);
			filePosition += toRead;
			// System.out.printf("%s%n", Utils.toHexString(0, 100, buff));
			// if (true)
			// throw new DecodeException("test", null);
			bufPos = 0;
			bufEnd = toRead + delta;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		return true;
	}

	@Override
	public int getSampleRate() {
		return frm.samplingFreq();
	}

	@Override
	public long getSampleCount() {
		return (frm.dataEnd - frm.dataStart) * (8 / getNumChannels());
	}

	@Override
	public int getNumChannels() {
		return frm.channels();
	}

	@Override
	void initBuffers(int overrun) {
		buff = new byte[(block + overrun) * frm.channels()];
	}

	@Override
	boolean isMSB() {
		return true;
	}

	@Override
	byte[] getSamples() {
		return buff;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		try {
			if (sampleNum == 0)
				dsdStream.seek(frm.dataStart);
			else if (sampleNum > 0 && sampleNum < getSampleCount()) {
				// no accuracy for position in block
				//long block = sampleNum / (fmt.blockSize * 8);
				dsdStream.seek(frm.dataStart + sampleNum / (frm.channels() * 8));
				//throw new DecodeException("Pending", null);
			} else 
				throw new DecodeException("Trying to seek non existing sample "+sampleNum, null);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

}
