package com.mygdx.generator;

import com.badlogic.gdx.utils.Array;

public enum StartEnum {
	A(2, 1, 0),
	B(0, 1, 2),
	C(-3, -1, 0);
	
	int[] positions;
	StartEnum(int... positions) {
		this.positions = positions;	
	}
	
	public Array<Note> getNotes(KeyEnum key, NoteEnum base, int octave) {
		Note[] notes = new Note[positions.length];
		for (int i = 0; i < positions.length; i++) {
			notes[i] = new Note(base.add(key, base, positions[i], octave), (i == positions.length - 1) ? NoteLengthEnum.HALF : NoteLengthEnum.EIGTH);
		}
		return new Array<Note>(notes);
	}
	
	public int getLastPosition() {
		return positions[positions.length-1];
	}
}
