package xyz.lucasallegri.dialog;

import javax.swing.JOptionPane;

import xyz.lucasallegri.launcher.LanguageManager;

public class DialogError {
	
	public static void push(String msg) {
		JOptionPane.showMessageDialog(null,
			    msg,
			    "Knight Launcher Error",
			    JOptionPane.ERROR_MESSAGE
			    );
	}
	
	public static void pushTranslated(String msg) {
		JOptionPane.showMessageDialog(null,
			    msg,
			    LanguageManager.getValue("t.error"),
			    JOptionPane.ERROR_MESSAGE
			    );
	}
	
}
