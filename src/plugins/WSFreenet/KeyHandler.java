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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class KeyHandler extends Handler {

    public KeyHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName) {
        super(ws, message, pr, indynetPluginName);
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
            sendActionNotImplementedErrorReply("Indynet plugin is nit loaded!");
        } catch (IOException ex) {
            sendServerErrorReply();
        }
    }

    private void resolve(String name) throws PluginNotFoundException, IOException {
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new ResolveCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "resolver.resolve");
        params.putSingle("name", name);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }
    
    private void handleGenerate() {
        String type = (String) jsonmessage.get("type");
        if (type == null) {
            this.sendMissingFieldErrorReply("Missing type field!");
            return;
        }
        String filename = (String) jsonmessage.getOrDefault("filename", "");
        int version = ((Long) jsonmessage.getOrDefault("version", new Long(0))).intValue();
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

    private class RegisterCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success) {
                JSONObject response = createJSONReplyMessage("success");
                response.put("resolveURI", fcppm.params.get("resolveURI"));
                ws.send(response.toJSONString());
            } else {
                sendServerErrorReply(fcppm.errorCode + " " + fcppm.errorMessage);
            }
            return FCPPluginMessage.construct();
        }
    }

    private class ResolveCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success) {
                JSONObject response = createJSONReplyMessage("found");
                try {
                    JSONObject resolveObject = parseJSONMessage(fcppm.params.get("json"));
                    response.put("answer", resolveObject);
                    ws.send(response.toJSONString());
                } catch (ParseException ex) {
                    sendJsonParseErrorReply("Error parshing returned object!");
                }
                
            } else {
                JSONObject response = createJSONReplyMessage("not found");
                JSONObject answer = new JSONObject();
                answer.put("errorcode", fcppm.errorCode);
                answer.put("errormessage", fcppm.errorMessage);
                response.put("answer", answer);
                ws.send(response.toJSONString());
            }
            return FCPPluginMessage.construct();
        }
    }

}
