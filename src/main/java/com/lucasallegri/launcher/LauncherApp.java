package com.lucasallegri.launcher;

import com.lucasallegri.dialog.DialogWarning;
import com.lucasallegri.discord.DiscordRPCInstance;
import com.lucasallegri.launcher.mods.ModListGUI;
import com.lucasallegri.launcher.settings.Settings;
import com.lucasallegri.launcher.settings.SettingsGUI;
import com.lucasallegri.launcher.settings.SettingsProperties;
import com.lucasallegri.util.*;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.JMarsDarkTheme;
import mdlaf.themes.MaterialLiteTheme;
import net.sf.image4j.codec.ico.ICOEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import static com.lucasallegri.launcher.Log.log;

public class LauncherApp {

  protected static LauncherGUI lgui;
  protected static SettingsGUI sgui;
  protected static ModListGUI mgui;
  protected static JVMPatcher jvmPatcher;
  protected static DiscordRPCInstance rpc = new DiscordRPCInstance(LauncherGlobals.RPC_CLIENT_ID);

  public static void main(String[] args) {

    LauncherApp app = new LauncherApp();

    if (SystemUtil.is64Bit() && SystemUtil.isWindows() && !Settings.jvmPatched) {
      jvmPatcher = app.composeJVMPatcher(app);
    } else {
      lgui = app.composeLauncherGUI(app);
      sgui = app.composeSettingsGUI(app);
      mgui = app.composeModListGUI(app);
    }

    new PostInitRoutine(app);
  }

  public LauncherApp () {
    setupFileLogging();
    logVMInfo();
    checkStartLocation();
    setupHTTPSProtocol();
    SettingsProperties.setup();
    SettingsProperties.loadFromProp();
    setupLauncherStyle();
    Locale.setup();
    FontManager.setup();
    rpc.start();
    KeyboardController.start();
    checkDirectories();
    if (SystemUtil.isWindows()) checkShortcut();
  }

  private LauncherGUI composeLauncherGUI(LauncherApp app) {
    EventQueue.invokeLater(() -> {
      try {
        lgui = new LauncherGUI(app);
        lgui.switchVisibility();
      } catch (Exception e) {
        log.error(e);
      }
    });
    return lgui;
  }

  private SettingsGUI composeSettingsGUI(LauncherApp app) {
    EventQueue.invokeLater(() -> {
      try {
        sgui = new SettingsGUI(app);
      } catch (Exception e) {
        log.error(e);
      }
    });
    return sgui;
  }

  private ModListGUI composeModListGUI(LauncherApp app) {
    EventQueue.invokeLater(() -> {
      try {
        mgui = new ModListGUI(app);
      } catch (Exception e) {
        log.error(e);
      }
    });
    return mgui;
  }

  private JVMPatcher composeJVMPatcher(LauncherApp app) {
    EventQueue.invokeLater(() -> {
      try {
        jvmPatcher = new JVMPatcher(app);
      } catch (Exception e) {
        log.error(e);
      }
    });
    return jvmPatcher;
  }

  public static DiscordRPCInstance getRPC() {
    return rpc;
  }

  private static void checkDirectories() {
    FileUtil.createDir("mods");
    FileUtil.createDir("KnightLauncher/images/");
    FileUtil.createDir("KnightLauncher/modules/");
  }

  // Checking if we're being ran inside the game's directory, "getdown-pro.jar" should always be present if so.
  private static void checkStartLocation() {
    if (!FileUtil.fileExists("./getdown-pro.jar")) {
      String pathWarning = "The .jar file appears to be placed in the wrong directory. " +
              "In some cases this is due to a false positive and can be ignored. Knight Launcher will attempt to launch normally."
              + System.lineSeparator() + "If this persists try using the Batch (KnightLauncher_windows.bat) file for Windows " +
              "or the Shell (KnightLauncher_mac_linux.sh) file for OSX/Linux.";
      if (SystemUtil.isWindows()) {
        pathWarning += System.lineSeparator() + "Additionally, we've detected the following Steam path: " + SteamUtil.getGamePathWindows();
      }
      log.warning(pathWarning);
      DialogWarning.push(pathWarning);
      //if (SystemUtil.isWindows()) DesktopUtil.openDir(SteamUtil.getGamePathWindows());
    }
  }

  // Create a shortcut to the application if there's none.
  private static void checkShortcut() {
    if (Settings.createShortcut
            && !FileUtil.fileExists(DesktopUtil.getPathToDesktop() + "/" + LauncherGlobals.SHORTCUT_FILE_NAME)) {

      BufferedImage bimg = ImageUtil.loadImageWithinJar("/img/icon-128.png");
      try {
        ICOEncoder.write(bimg, new File(LauncherGlobals.USER_DIR + "/KnightLauncher/images/icon-128.ico"));
      } catch (IOException e) {
        log.error(e);
      }

      DesktopUtil.createShellLink(System.getProperty("java.home") + "\\bin\\javaw.exe",
              "-jar \"" + LauncherGlobals.USER_DIR + "\\KnightLauncher.jar\"",
              LauncherGlobals.USER_DIR,
              LauncherGlobals.USER_DIR + "\\KnightLauncher\\images\\icon-128.ico",
              "Start KnightLauncher",
              LauncherGlobals.SHORTCUT_FILE_NAME
      );
    }
  }

  private static void setupLauncherStyle() {
    IconFontSwing.register(FontAwesome.getIconFont());
    try {
      UIManager.setLookAndFeel(new MaterialLookAndFeel());

      switch (Settings.launcherStyle) {
        case "dark":
          MaterialLookAndFeel.changeTheme(new JMarsDarkTheme());
          break;
        default:
          MaterialLookAndFeel.changeTheme(new MaterialLiteTheme());
          break;
      }
    } catch (UnsupportedLookAndFeelException e) {
      log.error(e);
    }
  }

  private static void setupHTTPSProtocol() {
    System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    System.setProperty("http.agent", "Mozilla/5.0");
    System.setProperty("https.agent", "Mozilla/5.0");
  }

  private static void setupFileLogging() {
    File logFile = new File("knightlauncher.log");
    File oldLogFile = new File("old-knightlauncher.log");

    if (logFile.exists()) {
      logFile.renameTo(oldLogFile);
    }

    try {
      PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile)), true);
      System.setOut(printStream);
      System.setErr(printStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void logVMInfo() {
    log.info("------------ VM Info ------------");
    log.info("OS Name: " + System.getProperty("os.name"));
    log.info("OS Arch: " + System.getProperty("os.arch"));
    log.info("OS Vers: " + System.getProperty("os.version"));
    log.info("Java Home: " + System.getProperty("java.home"));
    log.info("Java Vers: " + System.getProperty("java.version"));
    log.info("User Name: " + System.getProperty("user.name"));
    log.info("User Home: " + System.getProperty("user.home"));
    log.info("Current Directory: " + System.getProperty("user.dir"));
    log.info("---------------------------------");
  }

}
