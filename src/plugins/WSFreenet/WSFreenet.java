/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.clients.fcp.FCPPluginConnection;
import freenet.clients.fcp.FCPPluginMessage;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginFCPMessageHandler;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class WSFreenet implements FredPlugin, FredPluginThreadless, FredPluginFCPMessageHandler.ServerSideFCPMessageHandler{

    PluginRespirator pr; 
    WSFreenetServer server;
    final static String CONFIG_FILE = "wsfreenet.config.json";
    final static String INDYNET_PLUGIN_NAME = "plugins.Indynet.Indynet";
    Integer serverPort;
    Boolean serverStarted = false;
    Map<Integer, List<DataInsert>> dataInserts;
    Map<Integer, List<DataFetch>> dataFetches;
    
    @Override
    public void terminate() {
        try {
            server.stop();
            dataInserts.clear();
            dataFetches.clear();
        } catch (IOException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void runPlugin(PluginRespirator pr) {
        try {
            dataInserts = new ConcurrentHashMap<Integer, List<DataInsert>>();
            dataFetches = new ConcurrentHashMap<Integer, List<DataFetch>>();
            String url = pr.getToadletContainer().getURL();
            URI uri = new URI(url);
            Integer freenetPort = uri.getPort();
            JSONObject config = readJsonConfig();
            try {
            serverPort = ((Long)config.get("port")).intValue();
            }
            catch (NullPointerException ex){
                serverPort = freenetPort+1;
            }
             String [] allowedHosts;
            try {
            allowedHosts = Util.JSONArrayToStringArray((JSONArray)config.get("allowedHosts"));
            }
            catch (NullPointerException ex){
                allowedHosts = Util.JSONArrayToStringArray(new JSONArray());
            }
            JSONObject SSL = (JSONObject)config.get("SSL");
            Boolean enableSSL = false;
            String keyStore = "";
            String keyStorePassword = "";
            String keyPassword = "";
            if (SSL != null){
                enableSSL = (Boolean)SSL.get("enable");
                keyStore = (String)SSL.get("keyStore");
                if (keyStore == null){
                    keyStore = "";
                }
                keyStorePassword = (String)SSL.get("keyStorePassword");
                if (keyStorePassword == null){
                    keyStorePassword = "";
                }
                
                keyPassword = (String)SSL.get("keyPassword");
                if (keyPassword == null){
                    keyPassword = "";
                }
            }
            server = new WSFreenetServer(serverPort, allowedHosts, pr, INDYNET_PLUGIN_NAME, dataInserts, dataFetches, enableSSL, keyStore, keyStorePassword, keyPassword);
            server.start();
            serverStarted = true;
        } catch (Exception ex) {
            Util.debug("debug.txt", ex.getClass().getName()+" "+ex.getMessage()+" "+Arrays.toString(ex.getStackTrace()));
        } 
    }
    
    private JSONObject readJsonConfig() throws FileNotFoundException, IOException, ParseException{
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(new FileReader(CONFIG_FILE));
        return config;
    }

    @Override
    public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
        String action = fcppm.params.get("action");
        if (action.equalsIgnoreCase("getstatus")){
            SimpleFieldSet params = new SimpleFieldSet(false);
            params.put("serverPort", serverPort);
            params.put("serverStarted", serverStarted);
            return FCPPluginMessage.constructReplyMessage(fcppm, params, null, true, "", "");
        }
        else if (action.equalsIgnoreCase("getserverport")){
            SimpleFieldSet params = new SimpleFieldSet(false);
            params.put("serverPort", serverPort);
            return FCPPluginMessage.constructReplyMessage(fcppm, params, null, true, "", "");
        }
        else if (action.equalsIgnoreCase("getserverstarted")){
            SimpleFieldSet params = new SimpleFieldSet(false);
            params.put("serverStarted", serverStarted);
            return FCPPluginMessage.constructReplyMessage(fcppm, params, null, true, "", "");
        }
        else {
            return FCPPluginMessage.constructErrorReply(fcppm, "NOT_SUPPORTED", "WSFreenet: Action not supported.");
        }
    }
    
}
