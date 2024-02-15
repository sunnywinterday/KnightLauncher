package com.luuqui.launcher.flamingo;

import com.luuqui.launcher.LauncherGlobals;
import com.luuqui.launcher.flamingo.data.Server;
import com.luuqui.launcher.flamingo.data.Status;
import com.luuqui.util.RequestUtil;
import com.luuqui.util.SystemUtil;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.luuqui.launcher.flamingo.Log.log;

public class Flamingo {

  public static List<Server> getServerList() {
    List<Server> servers = new ArrayList<>();

    try {
      JSONObject response = RequestUtil.makeRequest("GET", LauncherGlobals.FLAMINGO_ENDPOINT + "/server-list/", new String[]{ "machineId=" + SystemUtil.getHashedMachineId() });
      log.info("Got server list from flamingo: " + response);

      // we got an empty server list. we return the empty servers list object.
      if(response.toString().equalsIgnoreCase("{}")) return servers;

      for(Object serverJsonObj : response.getJSONArray("serverlist")) {
        JSONObject serverJson = (JSONObject) serverJsonObj;

        Server server = new Server();
        server.name = serverJson.getString("name");
        server.description = serverJson.getString("description");
        server.managedBy = serverJson.getString("managedBy");
        server.beta = serverJson.getInt("beta");
        server.version = serverJson.getString("version");
        server.deployMethod = serverJson.getInt("deployMethod");
        server.deployUrl = serverJson.getString("deployUrl");
        server.playerCountUrl = serverJson.getString("playerCountUrl");
        server.siteUrl = serverJson.getString("siteUrl");
        server.communityUrl = serverJson.getString("communityUrl");
        server.sourceCodeUrl = serverJson.getString("sourceCodeUrl");
        server.announceType = serverJson.getString("announceType");
        server.announceContent = serverJson.getString("announceContent");
        server.enabled = serverJson.getInt("enabled");

        servers.add(server);
      }
    } catch (Exception e) {
      log.error(e);
    }

    return servers;
  }

  public static String activateBetaCode(String code) {
    try {
      JSONObject response = RequestUtil.makeRequest("POST", LauncherGlobals.FLAMINGO_ENDPOINT + "/beta-code/activate/" + code, new String[]{"machineId=" + SystemUtil.getHashedMachineId()});
      log.info("Got response for beta code activation: " + response);

      return response.getString("result");
    } catch (Exception e) {
      log.error(e);
      return "failure";
    }
  }

  public static Status getStatus() {
    try {
      JSONObject response = RequestUtil.makeRequest("GET", LauncherGlobals.FLAMINGO_ENDPOINT + "/status/", new String[]{});
      log.info("Got status from flamingo: " + response);

      Status status = new Status();
      status.version = response.getString("version");
      status.uptime = response.getLong("uptime");

      return status;
    } catch (Exception e) {
      log.error(e);
      return new Status();
    }
  }

}
