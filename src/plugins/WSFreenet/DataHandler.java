/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.InsertException;
import freenet.node.RequestStarter;
import freenet.pluginmanager.PluginRespirator;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class DataHandler extends Handler{

    public DataHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts) {
        super(ws, message, pr, indynetPluginName, dataInserts);
    }
    
    public DataHandler(WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts) {
        super(ws, data, pr, indynetPluginName, dataInserts);
    }
    
    @Override
    public void handle() {
        try {
            super.handle();
            if (data != null){
                handleData();
            }
            else if (message != null){
                if (action.equalsIgnoreCase("insert")) {
                    handleInsert();
                }
                else if (action.equalsIgnoreCase("clearqueue")) {
                    handleClearQueue();
                }
                else {
                    sendActionNotImplementedErrorReply();
                }
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } catch (MissingFieldException ex) {
            sendMissingFieldErrorReply(ex.getMessage());
        }
    }
    
    private void handleInsert(){
        if (!dataInserts.isEmpty() && !dataInserts.get(dataInserts.size()-1).hasGotData()){
            this.sendBadRequestErrorReply("Last metadata message is waiting for data!");
            return;
        }
        String contentType = (String)jsonmessage.get("contentType");
        if (contentType == null){
            this.sendMissingFieldErrorReply("Missing contentType field!");
            return;
        }
        String insertKey = (String)jsonmessage.get("insertKey");
        if (insertKey == null){
            this.sendMissingFieldErrorReply("Missing insertKey field!");
            return;
        }
        String filename = (String)jsonmessage.get("filename");
        Short priority = (Short)jsonmessage.getOrDefault("priority", RequestStarter.INTERACTIVE_PRIORITY_CLASS);
        Boolean realtime = (Boolean)jsonmessage.getOrDefault("realtime", false);
        dataInserts.add(new DataInsert(this, refmid, contentType, insertKey, filename, priority, realtime));
        JSONObject response = createJSONReplyMessage("status");
        response.put("status", "INSERT_METADATA_RECEIVED");
        ws.send(response.toJSONString());
    }
    
    private void handleData(){
        DataInsert lastInsert;
        if (dataInserts.isEmpty() || (lastInsert = dataInserts.get(dataInserts.size()-1)).hasGotData()){
            this.sendBadRequestErrorReply("No metadata message has been sent for this data!");
            return;
        }
        lastInsert.setData(data);
        this.refmid = lastInsert.getRefmid();
        JSONObject response = createJSONReplyMessage("status");
        response.put("status", "INSERT_DATA_RECEIVED");
        ws.send(response.toJSONString());
        try {
            lastInsert.insert();
            response = createJSONReplyMessage("status");
            response.put("status", "INSERT_INITIATED");
            ws.send(response.toJSONString());
        } catch (MalformedURLException ex){
            this.sendWrongFieldValueErrorReply("Wrong insert Key or filename! "+ex.getMessage());
        } catch (InsertWithEmptyDataException ex) {
            this.sendServerErrorReply(ex.getMessage());
        } catch (IOException ex) {
            this.sendServerErrorReply(ex.getMessage());
        } catch (InsertException ex) {
            this.sendServerErrorReply(ex.getMessage());
        }
        
    }

    private void handleClearQueue() {
        try {
            JSONObject response = createJSONReplyMessage("success");
            if (!dataInserts.isEmpty()){
                dataInserts.clear();
                response.put("message", "Insert queue cleared");
            }
            else {
                response.put("message", "Insert queue already empty");
            }
            ws.send(response.toJSONString());
        }
        catch (Exception ex){
            this.sendServerErrorReply(ex.getMessage()+" "+ex.toString());
        }
    }
    
}
