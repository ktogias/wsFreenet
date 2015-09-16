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
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
    
    @Override
    public void terminate() {
        try {
            server.stop();
            dataInserts.clear();
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
            String url = pr.getToadletContainer().getURL();
            URI uri = new URI(url);
            Integer freenetPort = uri.getPort();
            JSONObject config = readJsonConfig();
            serverPort = ((Long)config.getOrDefault("port", freenetPort.longValue()+1)).intValue();
            String [] allowedHosts = Util.JSONArrayToStringArray((JSONArray)config.getOrDefault("allowedHosts", new JSONArray()));
            JSONObject SSL = (JSONObject)config.getOrDefault("SSL", null);
            Boolean enableSSL = false;
            String keyStore = "";
            String keyStorePassword = "";
            String keyPassword = "";
            if (SSL != null){
                enableSSL = (Boolean)SSL.getOrDefault("enable", false);
                keyStore = (String)SSL.getOrDefault("keyStore", "");
                keyStorePassword = (String)SSL.getOrDefault("keyStorePassword", "");
                keyPassword = (String)SSL.getOrDefault("keyPassword", "");
            }
            server = new WSFreenetServer(serverPort, allowedHosts, pr, INDYNET_PLUGIN_NAME, dataInserts, enableSSL, keyStore, keyStorePassword, keyPassword);
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
