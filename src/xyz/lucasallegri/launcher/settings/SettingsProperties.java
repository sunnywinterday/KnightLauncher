package xyz.lucasallegri.launcher.settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import xyz.lucasallegri.logging.KnightLog;
import xyz.lucasallegri.util.FileUtil;

public class SettingsProperties {

	public static Properties prop = new Properties();
	private static String propPath = System.getProperty("user.dir") + File.separator + "KnightLauncher.properties";
	
	public static void setup() {
		try {
			if(!FileUtil.fileExists(propPath)) {
				File file = new File(propPath);
				file.createNewFile();
				fillWithBaseProp();
			}
		} catch (IOException e) {
			KnightLog.logException(e);
		}
	}
	
	private static void fillWithBaseProp() throws IOException {
		String baseProp = 	"platform=" + System.lineSeparator() +
							"rebuilds=";
		BufferedWriter writer = new BufferedWriter(new FileWriter(propPath, true));
		writer.append(baseProp);
		writer.close();
	}
	
	public static String getValue(String key) {
        try (InputStream is = new FileInputStream(propPath)) {
        	prop.load(is);
            return prop.getProperty(key);
        } catch (IOException e) {
        	KnightLog.logException(e);
        }
		return null;
	}
	
	public static void setValue(String key, String value) {
		try (OutputStream os = new FileOutputStream(propPath)) {
			prop.setProperty(key, value);
			prop.store(new FileOutputStream(propPath), null);
		} catch(IOException e) {
			KnightLog.logException(e);
		}
	}
	
	public static void loadFromProp() {
		Settings.gamePlatform = getValue("platform");
		Settings.doRebuilds = getValue("rebuilds").startsWith("true") ? true : false;
	}
	
}
