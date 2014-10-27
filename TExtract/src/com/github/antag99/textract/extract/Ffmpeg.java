package com.github.antag99.textract.extract;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

class Ffmpeg {
	private static final Logger logger = LogManager.getLogger(Ffmpeg.class);

	private static final String cmd;
	private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

	static {
		// Non-windows users will have to install ffmpeg
		String tmpCmd = "ffmpeg";
		
		if(isWindows) {
			File ffmpegExecutable = null;
			try {
				// Try to create a temporary file for the executable
				ffmpegExecutable = File.createTempFile("ffmpeg", ".exe");
			} catch(IOException ex) {
				// Dump the executable in the local directory instead
				ffmpegExecutable = new File("ffmpeg.exe");
			}
			
			URL ffmpegExecutableResource = Ffmpeg.class.getResource("/ffmpeg.exe");
			
			if(ffmpegExecutableResource == null) {
				throw new RuntimeException("ffmpeg.exe not found in classpath");
			}
			
			try {
				FileUtils.copyURLToFile(ffmpegExecutableResource, ffmpegExecutable);
				
				tmpCmd = ffmpegExecutable.getAbsolutePath();
			} catch(IOException ex) {
				logger.error("Failed to copy ffmpeg executable!");
				// We can still try, the user might have ffmpeg installed
			}
		}
		
		cmd = tmpCmd;
	}

	public static void convert(File input, File output) {
		StringBuilder command = new StringBuilder();
		command.append(cmd);
		command.append(" -i ");
		if(isWindows) {
			command.append('"');
		}
		command.append(FilenameUtils.separatorsToSystem(FilenameUtils.normalize(input.getAbsolutePath())));
		if(isWindows) {
			command.append('"');
		}
		command.append(" ");
		command.append("-acodec pcm_s16le");
		command.append(' ');
		command.append("-nostdin");
		command.append(' ');
		command.append("-ab 128k");
		command.append(" ");
		if(isWindows) {
			command.append('"');
		}
		command.append(FilenameUtils.separatorsToSystem(FilenameUtils.normalize(output.getAbsolutePath())));
		if(isWindows) {
			command.append('"');
		}
		
		logger.debug("Executing " + command);

		try {
			Process process = Runtime.getRuntime().exec(command.toString());
			if(process.waitFor() != 0) {
				logger.error("Ffmpeg exited with abnormal exit code: " + process.exitValue());
			}
			IOUtils.copy(process.getErrorStream(), System.err);
			IOUtils.copy(process.getInputStream(), System.out);
		} catch(Throwable ex) {
			logger.error("An error has occured when executing ffmpeg", ex);
		}
	}
}
