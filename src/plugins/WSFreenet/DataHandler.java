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

    public DataHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) {
        super(ws, message, pr, indynetPluginName, dataInserts, dataFetches);
    }
    
    public DataHandler(WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) {
        super(ws, data, pr, indynetPluginName, dataInserts, dataFetches);
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
                } else if (action.equalsIgnoreCase("fetch")){
                    handleFetch();
                }
                else if (action.equalsIgnoreCase("clearinsertqueue")) {
                    handleClearInsertQueue();
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
        DataInsert newInsert = new DataInsert(this, refmid, contentType, insertKey, filename, priority, realtime);
        dataInserts.add(newInsert);
        JSONObject response = createJSONReplyMessage("status");
        response.put("status", "INSERT_METADATA_RECEIVED");
        response.put("hint", "wait for senddata request to send data");
        ws.send(response.toJSONString());
        if (canRequestData()){
            newInsert.requestData();
        }
    }
    
    private void handleData(){
        DataInsert insert;
        if ((insert = getInsertWaitingForData()) == null){
            this.sendBadRequestErrorReply("Data rejected: No metadata message has been received for this data!");
            return;
        }
        insert.setData(data);
        this.refmid = insert.getRefmid();
        JSONObject response = createJSONReplyMessage("status");
        response.put("status", "INSERT_DATA_RECEIVED");
        ws.send(response.toJSONString());
        try {
            insert.insert();
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

    private void handleClearInsertQueue() {
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
    
    private void handleFetch(){
         String url = (String) jsonmessage.get("url");
         Short priority = (Short)jsonmessage.getOrDefault("priority", RequestStarter.INTERACTIVE_PRIORITY_CLASS);
         Boolean realtime = (Boolean)jsonmessage.getOrDefault("realtime", false);
         if (url == null) {
            this.sendMissingFieldErrorReply("Missing url field!");
            return;
        }
        DataFetch newFetch = new DataFetch(this, refmid, url, priority, realtime);
        newFetch.fetch();
        dataFetches.add(newFetch);
    }
    
    private Boolean canRequestData(){
        Boolean canRequestData = true; 
        for(DataInsert insert: dataInserts){
            if (insert.requestForDataSent() && !insert.hasGotData()){
                canRequestData = false;
                break;
            }
        }
        return canRequestData;
    }
    
    private DataInsert getInsertWaitingForData(){
        for(DataInsert insert: dataInserts){
            if (insert.requestForDataSent() && !insert.hasGotData()){
                return insert;
            }
        }
        return null;
    }

    
}
