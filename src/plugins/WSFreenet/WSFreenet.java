/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.PluginRespirator;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
public class WSFreenet implements FredPlugin, FredPluginThreadless{

    PluginRespirator pr; 
    WSFreenetServer server;
    final static String CONFIG_FILE = "wsfreenet.config.json";
    final static String INDYNET_PLUGIN_NAME = "plugins.Indynet.Indynet";
    
    @Override
    public void terminate() {
        try {
            server.stop();
        } catch (IOException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void runPlugin(PluginRespirator pr) {
        try {
            String url = pr.getToadletContainer().getURL();
            URI uri = new URI(url);
            Integer freenetPort = uri.getPort();
            JSONObject config = readJsonConfig();
            Integer port = ((Long)config.getOrDefault("port", freenetPort.longValue()+1)).intValue();
            String [] allowedHosts = Util.JSONArrayToStringArray((JSONArray)config.getOrDefault("allowedHosts", new JSONArray()));
            server = new WSFreenetServer(port, allowedHosts, pr, INDYNET_PLUGIN_NAME);
            server.start();
        } catch (URISyntaxException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(WSFreenet.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private JSONObject readJsonConfig() throws FileNotFoundException, IOException, ParseException{
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(new FileReader(CONFIG_FILE));
        return config;
    }
    
}
