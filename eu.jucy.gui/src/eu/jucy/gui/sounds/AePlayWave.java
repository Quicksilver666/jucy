package eu.jucy.gui.sounds;


import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import uc.DCClient;

import eu.jucy.gui.Application;


 
public class AePlayWave implements Runnable {
 
	/*public static void main(String[] args) {
		new AePlayWave("C:\\Users\\quicksilver\\Desktop\\downloads\\blip.wav").run();
	} */
	
	public static void playWav(String audioKey) {
		DCClient.execute(new AePlayWave(audioKey));
	}
	
	
	private String filename;
 
	private Position curPosition;
 
	private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
 
	enum Position {
		LEFT, RIGHT, NORMAL
	};
 
	private AePlayWave(String wavfile) {
		this(wavfile,Position.NORMAL);
	}
 
	private AePlayWave(String wavfile, Position p) {
		filename = wavfile;
		curPosition = p;
	}
 
	public void run() {
		Bundle bundle = Platform.getBundle(Application.PLUGIN_ID);
		Path path = new Path(filename); 
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		
		/*File soundFile = new File(filename);
		if (!soundFile.exists()) {
			System.err.println("Wave file not found: " + filename);
			return;
		} */
		
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(url.openStream());
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
 
		AudioFormat format = audioInputStream.getFormat();
		SourceDataLine auline = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
 
		try {
			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(format);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
 
		if (auline.isControlSupported(FloatControl.Type.PAN)) {
			FloatControl pan = (FloatControl) auline
					.getControl(FloatControl.Type.PAN);
			if (curPosition == Position.RIGHT)
				pan.setValue(1.0f);
			else if (curPosition == Position.LEFT)
				pan.setValue(-1.0f);
		} 
 
		auline.start();
		int nBytesRead = 0;
		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
 
		try {
			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0)
					auline.write(abData, 0, nBytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			auline.drain();
			auline.close();
		}
 
	}
}
