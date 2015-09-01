/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.clients.fcp.FCPPluginConnection;
import freenet.clients.fcp.FCPPluginMessage;
import freenet.pluginmanager.FredPluginFCPMessageHandler;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import java.io.IOException;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class KeyHandler extends Handler{

    public KeyHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName) {
        super(ws, message, pr, indynetPluginName);
    }

    @Override
    public void handle() {
        try {
            JSONObject m = parseJSONMessage(message);
            String action = (String)m.get("action");
            if (action == null){
                sendMissingFieldErrorReply("No action specified!");
                return;
            }
            if (action.equalsIgnoreCase("register")){
                handleRegister(m);
            }
            else if (action.equalsIgnoreCase("resolve")){
                handleResolve(m);
            }
            else {
                sendActionNotImplementedErrorReply();
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } 
    }
    
    public void handleRegister (JSONObject m){
        String name = (String)m.get("name");
        if (name == null){
            this.sendMissingFieldErrorReply("Missing name field!");
            return;
        }
        String requestKey = (String)m.get("requestKey");
        if (requestKey == null){
            this.sendMissingFieldErrorReply("Missing requestKey field!");
            return;
        }
        try {
            register(name, requestKey);
        } catch (IOException ex) {
            sendServerErrorReply();
        } catch (PluginNotFoundException ex) {
            sendActionNotImplementedErrorReply("Indynet plugin is nit loaded!");
        }
    }
    
    public void register (String name, String requestKey) throws IOException, PluginNotFoundException{
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new RegisterCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "resolver.register");
        params.putSingle("name", name);
        params.putSingle("requestKey", requestKey);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }
    
    public void handleResolve(JSONObject m){
        String name = (String)m.get("name");
        if (name == null){
            this.sendMissingFieldErrorReply("Missing name field!");
            return;
        }
        try {
            resolve(name);
        } catch (PluginNotFoundException ex) {
            sendActionNotImplementedErrorReply("Indynet plugin is nit loaded!");
        } catch (IOException ex) {
            sendServerErrorReply();
        }
    }
    
    public void resolve(String name) throws PluginNotFoundException, IOException{
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new ResolveCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "resolver.resolve");
        params.putSingle("name", name);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }
    
    private class RegisterCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {
        
        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success){
                JSONObject response = new JSONObject();
                response.put("resolveURI", fcppm.params.get("resolveURI"));
                ws.send(response.toJSONString());
            }
            else {
                sendServerErrorReply(fcppm.errorCode+" "+fcppm.errorMessage);
            }
            return FCPPluginMessage.construct();
        }      
    }
    
    private class ResolveCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {
        
        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success){
                ws.send(fcppm.params.get("json"));
            }
            else {
                sendServerErrorReply(fcppm.errorCode+" "+fcppm.errorMessage);
            }
            return FCPPluginMessage.construct();
        }      
    }
    
    
}
