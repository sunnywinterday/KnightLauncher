package com.luuqui.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import static com.luuqui.util.Log.log;

public class SystemUtil {

  private static final String OS = System.getProperty("os.name").toLowerCase();

  public static boolean isWindows() {
    return OS.contains("win");
  }

  public static boolean isMac() {
    return OS.contains("mac");
  }

  public static boolean isUnix() {
    return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
  }

  public static boolean is64Bit() {
    boolean is64Bit;
    if (isWindows()) {
      is64Bit = (System.getenv("ProgramFiles(x86)") != null);
    } else {
      is64Bit = (System.getProperty("os.arch").indexOf("64") != -1);
    }
    return is64Bit;
  }

  public static boolean isARM() {
    boolean isARM = false;
    if (System.getProperty("os.arch").contains("arm")) {
      isARM = true;
    }
    return isARM;
  }

  @Deprecated
  public static boolean hasValidJavaHome() {
    return System.getProperty("java.home").contains("1.6") ||
            System.getProperty("java.home").contains("1.7") ||
            System.getProperty("java.home").contains("1.8");
  }

  private static String getMachineId() {
    String machineId = null;

    if(isWindows()) {
      machineId = ProcessUtil.runAndCapture(new String[]{ "cmd.exe", "/C", "wmic csproduct get UUID" })[0];
      machineId = machineId.substring(machineId.indexOf("\n")).trim();
    }

    if(isUnix()) {
       machineId = ProcessUtil.runAndCapture(new String[]{ "/bin/bash", "-c", "cat /etc/machine-id" })[0];
       machineId = machineId.replaceAll("\\r|\\n", "").trim();
    }

    // TODO: Verify this even works on Mac systems.
    if(isMac()) {
      try {
        String command = "system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }'";
        StringBuffer output = new StringBuffer();

        Process SerNumProcess = Runtime.getRuntime().exec(command);
        BufferedReader sNumReader = new BufferedReader(new InputStreamReader(SerNumProcess.getInputStream()));

        String line = "";
        while ((line = sNumReader.readLine()) != null) {
          output.append(line + "\n");
        }

        machineId = output.toString().substring(output.indexOf("UUID: "), output.length()).replace("UUID: ", "");

        SerNumProcess.waitFor();
        sNumReader.close();
      } catch (Exception e) {
        log.error(e);
      }
    }

    return machineId;
  }

  public static String getHashedMachineId() {
    String machineId = getMachineId();
    String hashedMachineId = null;

    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(machineId.getBytes(StandardCharsets.UTF_8));
      UUID uuid = UUID.nameUUIDFromBytes(messageDigest.digest());
      hashedMachineId = uuid.toString();
    } catch (Exception e) {
      log.error(e);
    }

    return hashedMachineId;
  }

}