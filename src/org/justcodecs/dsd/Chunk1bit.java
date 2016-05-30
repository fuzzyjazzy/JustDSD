package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

/**
* This class is used with WSDFormat.
* 
* This class is based on ChunkFRM8 written by D. Rogatkin.
* 
* @author J. Fujimori
* @version 1.0 2016-04-03
*/

class Chunk1bit extends BaseChunk {
	byte[] header = new byte[2048];
	long dataEnd;
	long dataStart = 2048;

	//HashMap<String, Object> metaAttrs;
	//String encoding;
	
	@Override
	void read(DSDStream ds) throws DecodeException {
		try {
			ds.seek(0L);
			ds.readFully(header, 0, 2048);
			//System.out.println("ID=" + new String(header, 0, 4));
			dataEnd = ds.length();
			/*
			metaAttrs = new HashMap<String, Object>();
			for (;;) {
				// read local chunks
				BaseChunk c = BaseChunk.create(ds, this);
				//System.out.printf("--->%s%n", c);
				if (c instanceof ChunkPROP) {
					props = (ChunkPROP)c;
					break;
				}
			}
			*/
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

	public int samplingFreq() {
		return  intFrom4bytes(header, 36);
	}

	public int channels() {
		return  header[44];
	}

	//-- Bigendian
	final public int intFrom4bytes(byte[] buf, int at) {
		return ( (((int)buf[at] & 0xff) << 24) + (((int)buf[at+1] & 0xff) <<16) + (((int)buf[at+2] & 0xff) <<8) + ((int)buf[at+3] & 0xff) );
	}

}