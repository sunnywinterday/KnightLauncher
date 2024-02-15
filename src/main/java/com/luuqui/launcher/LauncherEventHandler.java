package com.luuqui.launcher;

import com.luuqui.discord.DiscordRPC;
import com.luuqui.launcher.flamingo.data.Server;
import com.luuqui.launcher.mods.ModLoader;
import com.luuqui.launcher.settings.GameSettings;
import com.luuqui.launcher.settings.Settings;
import com.luuqui.launcher.settings.SettingsEventHandler;
import com.luuqui.util.*;
import org.apache.commons.io.FileUtils;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static com.luuqui.launcher.Log.log;

public class LauncherEventHandler {

  private static final String[] RPC_COMMAND_LINE = new String[] { ".\\KnightLauncher\\modules\\skdiscordrpc\\SK-DiscordRPC.exe" };

  public static void launchGameEvent() {

    Thread launchThread = new Thread(() -> {

      // disable server switching during launch procedure
      LauncherGUI.serverList.setEnabled(false);

      if(LauncherApp.selectedServer.name.equalsIgnoreCase("Official")) {
        // official servers launch procedure
        if (ModLoader.mountRequired) ModLoader.mount();
        SettingsEventHandler.saveAdditionalArgs();
        SettingsEventHandler.saveConnectionSettings();
        GameSettings.load();

        if (Settings.gamePlatform.startsWith("Steam")) {

          try {
            SteamUtil.startGameById(99900);
          } catch (Exception e) {
            log.error(e);
          }

        } else {

          if (SystemUtil.isWindows()) {
            ProcessUtil.run(LauncherGlobals.GETDOWN_ARGS_WIN, true);
          } else {
            ProcessUtil.run(LauncherGlobals.GETDOWN_ARGS, true);
          }

        }

        log.info("Starting game", "platform", Settings.gamePlatform);
        if (Settings.useIngameRPC) ProcessUtil.run(RPC_COMMAND_LINE, true);
      } else {
        // third party server launch procedure
        Server selectedServer = LauncherApp.selectedServer;
        String sanitizedServerName = getSanitizedServerName(selectedServer.name);

        ProgressBar.startTask();
        ProgressBar.setBarMax(2);
        ProgressBar.setState("Retrieving data from " + LauncherApp.selectedServer.name + "...");
        ProgressBar.setBarValue(0);

        if(!FileUtil.fileExists(LauncherGlobals.USER_DIR + "/thirdparty/" + sanitizedServerName + "/version.txt")) {
          // we did not download this third party client, time to get it from the deploy url.

          ProgressBar.setState("Downloading " + LauncherApp.selectedServer.name + "... (this might take a while)");
          ProgressBar.setBarValue(1);

          boolean downloadCompleted = false;
          int downloadAttempts = 0;

          while(downloadAttempts <= 3 && !downloadCompleted) {
            downloadAttempts++;
            log.info("Downloading a third party client: " + sanitizedServerName,
              "attempts", downloadAttempts);
            try {
              FileUtils.copyURLToFile(
                new URL(selectedServer.deployUrl + "/" + selectedServer.version + ".zip"),
                new File(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip"),
                0,
                0
              );
              Compressor.unzip(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip",
                LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName, false);
              FileUtil.deleteFile(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip");
              downloadCompleted = true;
            } catch (IOException e) {
              // Just keep retrying.
              log.error(e);
            }
          }
        }

        // let's see if we need to update the third party client
        try {
          String localVersion = FileUtil.readFile(LauncherGlobals.USER_DIR + "/thirdparty/" + sanitizedServerName + "/version.txt");
          if(!selectedServer.version.equalsIgnoreCase(localVersion)) {
            log.info("Updating third party client: " + selectedServer.name);

            ProgressBar.setState("Updating " + LauncherApp.selectedServer.name + "... (this might take a while)");
            ProgressBar.setBarValue(1);

            boolean downloadCompleted = false;
            int downloadAttempts = 0;

            while (downloadAttempts <= 3 && !downloadCompleted) {
              downloadAttempts++;
              log.info("Downloading a third party client: " + sanitizedServerName,
                "attempts", downloadAttempts);
              try {
                FileUtils.copyURLToFile(
                  new URL(selectedServer.deployUrl + "/" + selectedServer.version + ".zip"),
                  new File(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip"),
                  0,
                  0
                );
                Compressor.unzip(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip",
                  LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName, false);
                FileUtil.deleteFile(LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\bundle.zip");
                downloadCompleted = true;
              } catch (IOException e) {
                // Just keep retrying.
                log.error(e);
              }

            }
          }
          log.info("Third party client up to date: " + selectedServer.name);
        } catch (IOException e) {
          log.error(e);
        }

        // we already have the client files,
        // the client is up-to-date, or the download has finished.
        // and so we start it up!
        ProgressBar.setState("Starting " + LauncherApp.selectedServer.name + "...");
        ProgressBar.setBarValue(2);

        ProcessUtil.runFromDirectory(getThirdPartyClientStartCommand(selectedServer), LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName,true);

        ProgressBar.finishTask();
      }

      DiscordRPC.getInstance().stop();
      if (!Settings.keepOpen) {
        LauncherGUI.launcherGUIFrame.dispose();
        System.exit(1);
      }

      // re-enable server switching
      LauncherGUI.serverList.setEnabled(true);

    });
    launchThread.start();

  }

  public static void launchGameAltEvent() {

    Thread launchAltThread = new Thread(() -> {

      if(LauncherApp.selectedServer.name.equalsIgnoreCase("Official")) {
        // official servers alt launch procedure
        if (!SystemUtil.isWindows()) {
          ProcessUtil.run(LauncherGlobals.ALT_CLIENT_ARGS, true);
        } else {
          ProcessUtil.run(LauncherGlobals.ALT_CLIENT_ARGS_WIN, true);
        }

        DiscordRPC.getInstance().stop();
      } else {
        // third party alt launch procedure
      }

    });
    launchAltThread.start();

  }

  public static void updateServerList(List<Server> servers) {
    LauncherApp.serverList.clear();
    Server official = new Server("Official");
    official.playerCountUrl = LauncherApp.getSteamPlayerCountString();
    LauncherApp.serverList.add(official);

    for(Server server : servers) {
      if(server.beta == 1) server.name += " (Beta)";
      LauncherGUI.serverList.addItem(server.name);
      LauncherApp.serverList.add(server);

      // make sure we have a folder to later download the client
      FileUtil.createDir(LauncherGlobals.USER_DIR + "/thirdparty/" + getSanitizedServerName(server.name));
    }

    LauncherApp.selectedServer = official;
    selectedServerChanged(null);
  }

  public static void selectedServerChanged(ActionEvent event) {
    Server selectedServer = findServerInServerList((String) LauncherGUI.serverList.getSelectedItem());

    if(selectedServer != null) {
      if(selectedServer.name.equalsIgnoreCase("Official")) {
        LauncherGUI.launchButton.setText("Play Now");
        LauncherGUI.launchButton.setToolTipText("Play Now");
        LauncherGUI.launchButton.setEnabled(selectedServer.enabled == 1);
        LauncherGUI.playerCountLabel.setText(selectedServer.playerCountUrl);
      } else {
        LauncherGUI.launchButton.setText("Play " + selectedServer.name);
        LauncherGUI.launchButton.setToolTipText("Play " + selectedServer.name);
        LauncherGUI.launchButton.setEnabled(selectedServer.enabled == 1);

        // TODO: Fetch player count.
        LauncherGUI.playerCountLabel.setText("Player count unavailable.");
      }
      LauncherApp.selectedServer = selectedServer;
    } else {
      // fallback to official in rare error scenario
      LauncherGUI.serverList.setSelectedIndex(0);
      LauncherApp.selectedServer = findServerInServerList("Official");
    }
  }

  private static Server findServerInServerList(String serverName) {
    List<Server> results = LauncherApp.serverList.stream()
      .filter(s -> serverName.equals(s.name)).collect(Collectors.toList());
    return results.isEmpty() ? null : results.get(0);
  }

  private static String getSanitizedServerName(String serverName) {
    return serverName.toLowerCase().replace(" ", "-")
      .replace("(", "").replace(")", "");
  }

  private static String[] getThirdPartyClientStartCommand(Server server) {
    String[] args;
    String sanitizedServerName = getSanitizedServerName(server.name);
    if(SystemUtil.isWindows()) {
      args = new String[]{
        LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\java_vm\\bin\\java",
        "-classpath",
        LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/config.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/projectx-config.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/projectx-pcode.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/lwjgl.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/lwjgl_util.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/jinput.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/jshortcut.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-beanutils.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-digester.jar;" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-logging.jar;",
        "-Dcom.threerings.getdown=false",
        "-Xms256M",
        "-Xmx512M",
        "-XX:+AggressiveOpts",
        "-XX:SoftRefLRUPolicyMSPerMB=10",
        "-Djava.library.path=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./native",
        "-Dorg.lwjgl.util.NoChecks=true",
        "-Dsun.java2d.d3d=false",
        "-Dappdir=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + ".",
        "-Dresource_dir=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./rsrc",
        "com.threerings.projectx.client.ProjectXApp",
      };
    } else {
      args = new String[]{
        LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + "\\java\\bin\\java",
        "-classpath",
        LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/config.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/projectx-config.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/projectx-pcode.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/lwjgl.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/lwjgl_util.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/jinput.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/jshortcut.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-beanutils.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-digester.jar:" +
          LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./code/commons-logging.jar:",
        "-Dcom.threerings.getdown=false",
        "-Xms256M",
        "-Xmx512M",
        "-XX:+AggressiveOpts",
        "-XX:SoftRefLRUPolicyMSPerMB=10",
        "-Djava.library.path=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./native",
        "-Dorg.lwjgl.util.NoChecks=true",
        "-Dsun.java2d.d3d=false",
        "-Dappdir=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + ".",
        "-Dresource_dir=" + LauncherGlobals.USER_DIR + "\\thirdparty\\" + sanitizedServerName + File.separator + "./rsrc",
        "com.threerings.projectx.client.ProjectXApp",
      };
    }

    return args;
  }

}