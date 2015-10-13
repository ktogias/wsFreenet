/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.clients.fcp.FCPPluginConnection;
import freenet.clients.fcp.FCPPluginMessage;
import freenet.node.FSParseException;
import freenet.pluginmanager.FredPluginFCPMessageHandler;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import java.io.IOException;
import java.util.List;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class AuthHandler extends Handler {

    public AuthHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) {
        super(ws, message, pr, indynetPluginName, dataInserts, dataFetches);
    }

    @Override
    public void handle() {
        try {
            super.handle();
            if (action.equalsIgnoreCase("signup")) {
                handleSignup();
            } else {
                sendActionNotImplementedErrorReply();
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } catch (MissingFieldException ex) {
            sendMissingFieldErrorReply(ex.getMessage());
        }
    }

    private void handleSignup() {
        String username = (String) jsonmessage.get("username");
        if (username == null) {
            this.sendMissingFieldErrorReply("Missing username field!");
            return;
        }
        String password = (String) jsonmessage.get("password");
        if (password == null) {
            this.sendMissingFieldErrorReply("Missing password field!");
            return;
        }
        try {
            signup(username, password);
        } catch (IOException ex) {
            sendServerErrorReply();
        } catch (PluginNotFoundException ex) {
            sendActionNotImplementedErrorReply("Indynet plugin is not loaded!");
        }
    }

    private void signup(String username, String password) throws IOException, PluginNotFoundException {
        FCPPluginConnection connection = pr.connectToOtherPlugin(indynetPluginName, new SignupCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "userauth.signup");
        params.putSingle("username", username);
        params.putSingle("password", password);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage);
    }

    private class SignupCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success && fcppm.params.get("origin").equalsIgnoreCase("UserAuth")) {
                JSONObject response = createJSONReplyMessage("success");
                response.put("insertedURI", fcppm.params.get("insertedURI"));
                ws.send(response.toJSONString());
            } else if (fcppm.params.get("origin").equalsIgnoreCase("UserAuth") 
                    && fcppm.params.get("status").equalsIgnoreCase("Failure")){
                sendServerErrorReply(fcppm.errorCode);
            } else if (fcppm.params.get("origin").equalsIgnoreCase("InsertCallback") 
                    && fcppm.params.get("status").equalsIgnoreCase("ReceivedEvent")){
                JSONObject response = createJSONReplyMessage("progress");
                try {
                    JSONObject obj = Util.SimpleFieldSetToJSONObject(fcppm.params);
                    response.putAll(obj);
                } catch (FSParseException ex) {
                    response.put("error", Util.exceptionToJson(ex));
                }
                ws.send(response.toJSONString());
            }
            return FCPPluginMessage.construct();
        }
    }

    

}
