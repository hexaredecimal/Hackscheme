/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package package_manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;

/**
 *
 * @author hexaredecimal
 */
public class FileDownloader {

	private static void _downloadFile(String name, String fileURL, String savePath) throws IOException {
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// Check HTTP response code
		if (responseCode == HttpURLConnection.HTTP_OK) {
			InputStream inputStream = httpConn.getInputStream();
			FileOutputStream outputStream = new FileOutputStream(savePath);
			int bytesRead;
			int count = 0;
			byte[] buffer = new byte[4096];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				if (count % 10 == 0) {
				}
				outputStream.write(buffer, 0, bytesRead);
				count += bytesRead;  
			}
			outputStream.close();
			inputStream.close();
			System.out.println("File downloaded: " + (count +1) + " bytes written");
		} else {
			System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
	}

	public static boolean downloadGZ(String pkg_name, String url, String dest) {
		try {
			_downloadFile(pkg_name, url, dest);
		} catch (IOException ex) {
			System.err.println("Error: cannot fetch from url: ".concat(url));
			System.exit(1);
		}
		return true;
	}

	public static void extractGZ(String path, String pkg_name) {
		String output = path.substring(0, path.indexOf(pkg_name));
		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
		System.out.println("Done extracting");
		try {
			File input = new File(path);
			archiver.extract(input, new File(output));
			input.delete(); 
		} catch (IOException ex) {
			System.err.println("Error: Failed to extract packaged tar.gz file to packages directory");
			System.exit(101);
		}
	}
}
