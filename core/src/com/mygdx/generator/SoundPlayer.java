package com.mygdx.generator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.utils.Array;

// Base code taken from WolinLabs
public class SoundPlayer {

	public static final int SAMPLING_RATE = 44100;            // Audio sampling rate
	private final int SAMPLE_SIZE = 2;                  // Audio sample size in bytes
	private final float VOLUME = .3f;
	private boolean isPlaying = false;

	public boolean play(final AudioDevice player, int BPM, Array<Note> notes) throws InterruptedException {
		if (notes.size == 0 || isPlaying) return false;
		isPlaying = true;

		int timeCalc = 0;
		for (Note n: notes) {
			timeCalc += n.getNumberSamples(BPM, SAMPLING_RATE) + Note.getSpacing(BPM, SAMPLING_RATE);
		}

		final int TIME = (int) Math.ceil((double)timeCalc/SAMPLING_RATE);						  // Time in seconds to play

		int noteIndex = 0;
		double fFreq = notes.get(noteIndex).freq;                         // Frequency of sine wave in hz
		int noteSampleLength = notes.get(noteIndex).getNumberSamples(BPM, SAMPLING_RATE);

		//Position through the sine wave as a percentage (i.e. 0 to 1 is 0 to 2*PI)
		double fCyclePosition = 0;

		int ctSamplesTotal = SAMPLING_RATE*TIME;
		int totalValuesWritten = 0;

		boolean isBreak = false;

		Array<Short> sample = new Array<Short>(ctSamplesTotal);

		double fCycleInc = fFreq/SAMPLING_RATE;  // Fraction of cycle between samples

		for (int i=0; i < ctSamplesTotal; i++) {

			sample.add((short)(Short.MAX_VALUE * VOLUME * Math.sin(2*Math.PI * fCyclePosition)));

			totalValuesWritten++;
			if (totalValuesWritten >= noteSampleLength) {
				totalValuesWritten = 0;
				isBreak = !isBreak;
				if (isBreak) {
					fCycleInc = 1;
					noteSampleLength = Note.getSpacing(BPM, SAMPLING_RATE);
				} else {
					noteIndex++;
					if (noteIndex > notes.size - 1) {
						break;
					}
					fFreq = notes.get(noteIndex).freq;
					noteSampleLength = notes.get(noteIndex).getNumberSamples(BPM, SAMPLING_RATE);
					fCycleInc = fFreq/SAMPLING_RATE;
				}
			}
			fCyclePosition += fCycleInc;
			if (fCyclePosition > 1)
				fCyclePosition -= 1;
		}
		//while (sample.size() % SAMPLING_RATE != 0) {
		//	sample.add(sample.get(sample.size()-1));
		//}
		final short[] musicArray = new short[sample.size];
		for (int i = 0; i < sample.size; i++) {
			musicArray[i] = sample.get(i);
		}
		
		short[] fillArray = new short[SAMPLING_RATE / 4];
		double lastVal = (double)musicArray[musicArray.length-1];
		double rateOfDecrease = lastVal / fillArray.length;
		for (int i = 0; i < fillArray.length; i++) {
			lastVal -= rateOfDecrease;
			fillArray[i] = (short)lastVal;
		}
		
		final short[] sampleArray = new short[musicArray.length + fillArray.length];

		for (int i = 0; i < musicArray.length; i++) {
			sampleArray[i] = musicArray[i];
		}
		for (int i = 0; i < fillArray.length; i++) {
			sampleArray[musicArray.length + i] = fillArray[i];
		}
			
		new Thread(new Runnable() {
			@Override
			public void run() {				
				player.writeSamples(sampleArray, 0, sampleArray.length);
				player.dispose();
				isPlaying = false;
			}
		}).start();
		return true;
	}
	/*
	public void play(int BPM, List<Note> notes) throws InterruptedException {
		if (!line.isOpen() || notes.isEmpty() || isPlaying) return;
		System.out.println("PLAYING");
		isPlaying = true;

		int timeCalc = 0;
		for (Note n: notes) {
			timeCalc += n.getNumberSamples(BPM, SAMPLING_RATE) + Note.getSpacing(BPM, SAMPLING_RATE);
		}

		final int TIME = (int) Math.ceil((double)timeCalc/SAMPLING_RATE);						  // Time in seconds to play

		int noteIndex = 0;
		double fFreq = notes.get(noteIndex).freq;                         // Frequency of sine wave in hz
		int noteSampleLength = notes.get(noteIndex).getNumberSamples(BPM, SAMPLING_RATE);

		//Position through the sine wave as a percentage (i.e. 0 to 1 is 0 to 2*PI)
		double fCyclePosition = 0;

		line.start();

		// Make our buffer size match audio system's buffer
		ByteBuffer cBuf = ByteBuffer.allocate(line.getBufferSize());   

		int ctSamplesTotal = SAMPLING_RATE*TIME;
		int totalValuesWritten = 0;


		//On each pass main loop fills the available free space in the audio buffer
		//Main loop creates audio samples for sine wave, runs until we tell the thread to exit
		//Each sample is spaced 1/SAMPLING_RATE apart in time

		boolean isDone = false;
		boolean isBreak = false;

		while (ctSamplesTotal>0) {
			double fCycleInc = fFreq/SAMPLING_RATE;  // Fraction of cycle between samples

			cBuf.clear();                            // Discard samples from previous pass

			// Figure out how many samples we can add
			int ctSamplesThisPass = line.available()/SAMPLE_SIZE;

			for (int i=0; i < ctSamplesThisPass; i++) {

				cBuf.putShort((short)(Short.MAX_VALUE * VOLUME * Math.sin(2*Math.PI * fCyclePosition)));

				totalValuesWritten++;
				if (totalValuesWritten >= noteSampleLength) {
					totalValuesWritten = 0;
					isBreak = !isBreak;
					if (isBreak) {
						fCycleInc = 1;
						noteSampleLength = Note.getSpacing(BPM, SAMPLING_RATE);
					} else {
						noteIndex++;
						if (noteIndex > notes.size() - 1) {
							isDone = true;
							break;
						}
						fFreq = notes.get(noteIndex).freq;
						noteSampleLength = notes.get(noteIndex).getNumberSamples(BPM, SAMPLING_RATE);
						fCycleInc = fFreq/SAMPLING_RATE;
					}
				}
				fCyclePosition += fCycleInc;
				if (fCyclePosition > 1)
					fCyclePosition -= 1;
			}

			//Write sine samples to the line buffer.  If the audio buffer is full, this will 
			// block until there is room (we never write more samples than buffer will hold)
			line.write(cBuf.array(), 0, cBuf.position());            
			ctSamplesTotal -= ctSamplesThisPass;     // Update total number of samples written

			//Wait until the buffer is at least half empty  before we add more
			while (line.getBufferSize()/2 < line.available())
				Thread.sleep(1);

			if (isDone) {
				while (line.available() < SAMPLING_RATE) {
					Thread.sleep(1);
				}
				line.drain();
				isPlaying = false;
				return;
			}
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				player.writeSamples(sample, 0, count);
			}
		}).start();
	}
	 */
}


