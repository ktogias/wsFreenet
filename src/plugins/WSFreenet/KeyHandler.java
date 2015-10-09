/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.clients.fcp.FCPPluginConnection;
import freenet.clients.fcp.FCPPluginMessage;
import freenet.crypt.RandomSource;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.pluginmanager.FredPluginFCPMessageHandler;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class KeyHandler extends Handler {

    public KeyHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) {
        super(ws, message, pr, indynetPluginName, dataInserts, dataFetches);
    }

    @Override
    public void handle() {
        try {
            super.handle();
            if (action.equalsIgnoreCase("register")) {
                handleRegister();
            } else if (action.equalsIgnoreCase("resolve")) {
                handleResolve();
            } else if (action.equalsIgnoreCase("generate")){
                handleGenerate();
            } else {
                sendActionNotImplementedErrorReply();
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } catch (MissingFieldException ex) {
            sendMissingFieldErrorReply(ex.getMessage());
        }
    }

    private void handleRegister() {
        String name = (String) jsonmessage.get("name");
        if (name == null) {
            this.sendMissingFieldErrorReply("Missing name field!");
            return;
        }
        String requestKey = (String) jsonmessage.get("requestKey");
        if (requestKey == null) {
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

    private void register(String name, String requestKey) throws IOException, PluginNotFoundException {
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new RegisterCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "resolver.register");
        params.putSingle("name", name);
        params.putSingle("requestKey", requestKey);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }

    private void handleResolve() {
        String name = (String) jsonmessage.get("name");
        if (name == null) {
            this.sendMissingFieldErrorReply("Missing name field!");
            return;
        }
        try {
            resolve(name);
        } catch (PluginNotFoundException ex) {
            sendActionNotImplementedErrorReply("Indynet plugin is not loaded!");
        } catch (IOException ex) {
            sendServerErrorReply();
        }
    }
    
    private void handleGenerate() {
        String type = (String) jsonmessage.get("type");
        if (type == null) {
            this.sendMissingFieldErrorReply("Missing type field!");
            return;
        }
        String filename = (String) jsonmessage.get("filename");
        if (filename == null){
            filename = "";
        }
        int version;
        try {
            version = ((Long) jsonmessage.get("version")).intValue();
        }
        catch (NullPointerException ex){
            version = 0;
        }
        if (version < 0){
            this.sendWrongFieldValueErrorReply("Version number must not be negative");
            return;
        }
        if (type.equalsIgnoreCase("USK") && filename.isEmpty()){
            this.sendMissingFieldErrorReply("Filename is required for USK key generation, filename field is missing");
            return;
        }
        generate(type, filename, version);
    }
    
    
    
    private void resolve(String name) throws PluginNotFoundException, IOException {
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new ResolveCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "resolver.resolve");
        params.putSingle("name", name);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }
    
    private void generate(String type, String filename, int version){
        if (!type.equalsIgnoreCase("SSK") && !type.equalsIgnoreCase("USK")){
            this.sendErrorReply("not supported", "Generation of requested type of key is not supported.");
            return;
        }
        final RandomSource r = pr.getNode().random;
        if (!filename.isEmpty()){
            filename+="-"+version;
        }
        InsertableClientSSK key = InsertableClientSSK.createRandom(r, filename);
        FreenetURI insertURI = key.getInsertURI();
        FreenetURI requestURI = key.getURI();
        if (type.equalsIgnoreCase("USK")){
            insertURI = insertURI.uskForSSK();
            requestURI = requestURI.uskForSSK();
        }
        JSONObject response = createJSONReplyMessage("success");
        response.put("insertURI", insertURI.toString());
        response.put("requestURI", requestURI.toString());
        ws.send(response.toJSONString());
    }
    
    private void fetch(String key){
        
    }

    private class RegisterCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success && fcppm.params.get("origin").equalsIgnoreCase("Resolver")) {
                JSONObject response = createJSONReplyMessage("success");
                response.put("resolveURI", fcppm.params.get("resolveURI"));
                ws.send(response.toJSONString());
            } else if (fcppm.params.get("origin").equalsIgnoreCase("Resolver") 
                    && fcppm.params.get("status").equalsIgnoreCase("Failure")){
                sendServerErrorReply(fcppm.errorCode);
            } else if (fcppm.params.get("origin").equalsIgnoreCase("InsertCallback") 
                    && fcppm.params.get("status").equalsIgnoreCase("ReceivedEvent")){
                JSONObject response = createJSONReplyMessage("progress");
                response.put("eventclass", fcppm.params.get("eventclass"));
                response.put("eventcode", fcppm.params.get("eventcode"));
                response.put("eventdescription", fcppm.params.get("eventdescription"));
                ws.send(response.toJSONString());
            }
            return FCPPluginMessage.construct();
        }
    }

    private class ResolveCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success && fcppm.params.get("origin").equalsIgnoreCase("Resolver")) {
                JSONObject response = createJSONReplyMessage("success");
                response.put("requestKey", fcppm.params.get("requestKey"));
                ws.send(response.toJSONString());
            } else if (fcppm.params.get("origin").equalsIgnoreCase("Resolver") 
                    && fcppm.params.get("status").equalsIgnoreCase("Failure")){
                sendServerErrorReply(fcppm.errorCode);
            } else if (fcppm.params.get("origin").equalsIgnoreCase("InsertCallback") 
                    && fcppm.params.get("status").equalsIgnoreCase("ReceivedEvent")){
                JSONObject response = createJSONReplyMessage("progress");
                response.put("eventclass", fcppm.params.get("eventclass"));
                response.put("eventcode", fcppm.params.get("eventcode"));
                response.put("eventdescription", fcppm.params.get("eventdescription"));
                ws.send(response.toJSONString());
            }
            return FCPPluginMessage.construct();
        }
    }

}
