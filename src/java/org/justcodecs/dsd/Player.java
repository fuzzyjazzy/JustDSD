package org.justcodecs.dsd;

import java.io.File;
import java.io.FileNotFoundException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.justcodecs.dsd.Decoder.DecodeException;
import org.justcodecs.dsd.Decoder.PCMFormat;

public class Player {

	public static void main(String[] args) {
		System.out.printf("Java DSD player for PCM DAC  (c) 2014 D. Rogatkin%n");
		if (args.length == 0) {
			System.out.printf("Please use with at least one .dsf, .dff, or [SACD].iso file argument%n");
			System.exit(-1);
			return;
		}

		for (String f : args) {
			try {
				new Player().play(f);
			} catch (DecodeException e) {
				System.out.printf("Couldn't play %s, becasue %s%n", f, e);
				e.printStackTrace();
			}
		}
	}
	
	Decoder createDecoder() {
		return new Decoder();
	}

	public void play(String f) throws Decoder.DecodeException {
		Decoder decoder = createDecoder();
		long sampleCount = 0;
		try {
			DSDFormat dsd;
			if (f.endsWith(".dsf")) {
				dsd = new DSFFormat();
			} else if (f.endsWith(".iso")) {
				dsd = new DISOFormat();
			} else
				dsd = new DFFFormat();
			dsd.init(new Utils.RandomDSDStream(new File(f)));
			decoder.init(dsd);
			System.out.printf("Playing ... %s%n", dsd);
			PCMFormat pcmf = new PCMFormat();
			pcmf.sampleRate = 44100 * 2 * 2;
			pcmf.bitsPerSample = 16;
			//System.out.printf("clip: %x %x  %x-%x%n",((1 << pcmf.bitsPerSample) - 1) >> 1, 1 << pcmf.bitsPerSample, Short.MAX_VALUE, Short.MIN_VALUE); 
			pcmf.channels = 2;
			AudioFormat af = new AudioFormat(pcmf.sampleRate, pcmf.bitsPerSample, pcmf.channels, true, pcmf.lsb);
			SourceDataLine dl = AudioSystem.getSourceDataLine(af);
			decoder.setPCMFormat(pcmf);
			dl.open();
			dl.start();
			int[][] samples = new int[pcmf.channels][2048];
			int channels = (pcmf.channels > 2 ? 2 : pcmf.channels);
			int bytesChannelSample = pcmf.bitsPerSample / 8;
			int bytesSample = channels * bytesChannelSample;
			byte[] playBuffer = new byte[bytesSample * 2048];
			decoder.seek(0);
			boolean testSeek = false;
			do {
				int nsampl = decoder.decodePCM(samples);
				if (nsampl <= 0)
					break;
				int bp = 0;
				for (int s = 0; s < nsampl; s++) {
					for (int c = 0; c < channels; c++) {
						//System.out.printf("%x", samples[c][s]);
						for (int b = 0; b < bytesChannelSample; b++)
							playBuffer[bp++] = (byte) ((samples[c][s] >> (b * 8)) & 255);
					}
				}
				//for (int k=0;k<bp; k++)
				//System.out.printf("%x", playBuffer[k]);
				dl.write(playBuffer, 0, bp);
				sampleCount += nsampl;
				if (testSeek && sampleCount > pcmf.sampleRate *10) {
					decoder.seek((long)decoder.getSampleRate()*(3136));
					testSeek = false;
				}
			} while (true);
			dl.stop();
			dl.close();
		} catch (FileNotFoundException e) {
			throw new Decoder.DecodeException("Not found " + f, e);
		} catch (LineUnavailableException e) {
			throw new Decoder.DecodeException("Can't play this format", e);
		}
		decoder.dispose();
		System.out.printf("Total samples: %d%n", sampleCount);
	}

}
