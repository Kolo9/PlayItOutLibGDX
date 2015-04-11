package com.mygdx.generator;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class MusicGenerator extends ApplicationAdapter {
	private SpriteBatch batch;
	private Texture img;
	private Stage stage;
	private static OrthographicCamera camera;
	private static BitmapFont font;
	private static TextField textField;
	private static Label label;

	private static NoteEnum base = NoteEnum.B;
	private static KeyEnum key = KeyEnum.MAJOR;
	private static int BPM = 140;

	//private static Random RNG = new Random(System.currentTimeMillis());
	private static int[][] majorPositionProbabilityGrid = new int[][]{
		{2, 4, 6, 7},
		{},
		{0, 4, 6, 7},
		{},
		{0, 2, 6, 7},
		{},
		{0, 2, 4, 7},
		{0, 2, 4, 6}
	};
	private static int[][] minorPositionProbabilityGrid = new int[][]{
		{1, 2, 3, 4, 6, 7},
		{0, 2, 3, 4, 6, 7},
		{0, 1, 3, 4, 6, 7},
		{0, 1, 2, 4, 6, 7},
		{0, 1, 2, 3, 6, 7},
		{},
		{0, 1, 2, 3, 4, 7},
		{0, 1, 2, 3, 4, 6},
	};


	private static int[][] positionProbabilityGrid = (key == KeyEnum.MAJOR) ? majorPositionProbabilityGrid : minorPositionProbabilityGrid;
	private static int[] lengthProbabilityGrid = new int[]{0, 1, 2, 0, 1, 0, 1, 0, 1};

	private static SoundPlayer player;
	private static Array<Note> notes;
	
	private static Table table;

	@Override
	public void create () {	
		batch = new SpriteBatch();
		img = new Texture("playBtn.png");
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 960, 540);

		stage = new Stage(new StretchViewport(camera.viewportWidth, camera.viewportHeight, camera), batch);
		player = new SoundPlayer();

		TextFieldStyle textFieldStyle = new TextFieldStyle();
		textFieldStyle.background = new Image(new Texture("text.png")).getDrawable();
		font = new BitmapFont(Gdx.files.internal("SVBasicManual.fnt"), Gdx.files.internal("SVBasicManual.png"), false, true);
		textFieldStyle.font = font;
		textFieldStyle.fontColor = Color.BLACK;
		
		textField = new TextField("", textFieldStyle);
		textField.setAlignment(Align.center);
		
		LabelStyle labelStyle = new LabelStyle(font, Color.WHITE);
		label = new Label("", labelStyle);
		label.setWrap(true);
		label.setPosition(50, 200);

		ImageButton playBtn = new ImageButton(new Image(img).getDrawable());

		playBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					play(textField.getText().toUpperCase());
				} catch (InterruptedException e) {
					e.printStackTrace();
					Gdx.app.exit();
				}
			}			
		});

		table = new Table();
		table.setFillParent(true);
		table.add(textField).size(camera.viewportWidth - 50, 100);
		table.row();
		table.add(playBtn).size(playBtn.getWidth()/2, playBtn.getHeight()/2);
		
		Gdx.input.setInputProcessor(stage);
		stage.addActor(table);
	}

	private static String md5(final String md5) {
		try {
			final java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			final byte[] array = md.digest(md5.getBytes("UTF-8"));
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (final java.security.NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	private void play(String s) throws InterruptedException {
		String md5 = md5(s);
		String firstHalf = md5.substring(0, 13); //Positions
		String secondHalf = md5.substring(13, 26); //Lengths
		int rngStart = (int) (Long.parseLong(md5.substring(26, 28), 16) % StartEnum.values().length); //Randomness doesn't matter too much, could use as seed
		int rngKey = (int) (Long.parseLong(md5.substring(28, 30), 16) % KeyEnum.values().length);
		int rngBase = (int) (Long.parseLong(md5.substring(30), 16) % NoteEnum.values().length); //273 values with 2 hex digits, divisible by 13, so one repeat at end (not too much of a problem)
		key = KeyEnum.values()[rngKey];
		base = NoteEnum.values()[rngBase];
		Random RNGP = new Random(Long.parseLong(firstHalf, 16));
		Random RNGL = new Random(Long.parseLong(secondHalf, 16));
		notes = new Array<Note>(10);

		
		notes.addAll(StartEnum.values()[rngStart].getNotes(key, base, 1));	
		
		/*
		for (int i = 1; i <= 21; i++) {
			notes.add(new Note(base.addMajor(base, i, 1), NoteLengthEnum.QUARTER));
		}
		*/
		
		double beatsElapsed = 2.0;
		int lastRandom = StartEnum.values()[rngStart].getLastPosition();
		while (beatsElapsed < 7) {
			NoteLengthEnum length = NoteLengthEnum.values()[lengthProbabilityGrid[RNGL.nextInt(lengthProbabilityGrid.length)]];
			notes.add(new Note(base.add(key, base, lastRandom = positionProbabilityGrid[lastRandom][RNGP.nextInt(positionProbabilityGrid[lastRandom].length)], 1), length));
			beatsElapsed += length.frac; 
		}
		while (beatsElapsed < 8) {
			notes.add(new Note(base.add(key, base, lastRandom = positionProbabilityGrid[lastRandom][RNGP.nextInt(positionProbabilityGrid[lastRandom].length)], 1), NoteLengthEnum.EIGTH));
			beatsElapsed += NoteLengthEnum.EIGTH.frac;
		}
		while (beatsElapsed < 11) {
			NoteLengthEnum length = NoteLengthEnum.values()[lengthProbabilityGrid[RNGL.nextInt(lengthProbabilityGrid.length)]];
			notes.add(new Note(base.add(key, base, lastRandom = positionProbabilityGrid[lastRandom][RNGP.nextInt(positionProbabilityGrid[lastRandom].length)], 1), length));
			beatsElapsed += length.frac; 
		}
		while (beatsElapsed < 12) {
			notes.add(new Note(base.add(key, base, lastRandom = positionProbabilityGrid[lastRandom][RNGP.nextInt(positionProbabilityGrid[lastRandom].length)], 1), NoteLengthEnum.EIGTH));
			beatsElapsed += NoteLengthEnum.EIGTH.frac;
		}
		notes.add(new Note(base.f(1), NoteLengthEnum.WHOLE));
		
		
		BPM = RNGP.nextInt(16) + 135;
		
		//System.out.println(base + " " + key);
		
		if (player.play(Gdx.audio.newAudioDevice(SoundPlayer.SAMPLING_RATE, true), BPM, notes)) {
			label.setText("Key: " + base + " " + key + "\nBPM: " + BPM);	
		}
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		camera.update();

		batch.setProjectionMatrix(camera.combined);

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
		
		batch.begin();
		batch.setColor(1, 1, 1, 1);
		label.draw(batch, 1);
		batch.end();
	}

	@Override
	public void dispose() {
		img.dispose();
		batch.dispose();
	}
}
