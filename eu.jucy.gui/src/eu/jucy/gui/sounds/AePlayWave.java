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

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import uc.DCClient;

import eu.jucy.gui.Application;


 
public class AePlayWave implements Runnable {
 
	
	private static final Logger logger = LoggerFactory.make();
	
	/*public static void main(String[] args) {
		new AePlayWave("C:\\Users\\quicksilver\\Desktop\\downloads\\blip.wav").run();
	} */
	
	public static void playWav(String audioKey) {
		DCClient.execute(new AePlayWave(audioKey));
	}
	
	
	private String filename;
 
	private Position curPosition;
 
	private static final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
 
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
		
		
		AudioInputStream audioInputStream = null;
		SourceDataLine auline = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(url.openStream());


			AudioFormat format = audioInputStream.getFormat();

			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);


			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(format);


			if (auline.isControlSupported(FloatControl.Type.PAN)) {
				FloatControl pan = (FloatControl) auline
				.getControl(FloatControl.Type.PAN);
				if (curPosition == Position.RIGHT) {
					pan.setValue(1.0f);
				} else if (curPosition == Position.LEFT) {
					pan.setValue(-1.0f);
				}
			} 

			auline.start();
			int nBytesRead = 0;
			byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];


			while (nBytesRead != -1) {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0) {
					auline.write(abData, 0, nBytesRead);
				}
			}
		} catch (IOException e) {
			logger.warn(e,e);
			return;
		} catch (UnsupportedAudioFileException e) {
			logger.warn(e,e);
		} catch (LineUnavailableException e) {
			logger.warn(e,e);
		} finally {
			if (auline !=null) {
				auline.drain();
				auline.close();
			}
		}
 
	}
}
