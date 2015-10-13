/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.InsertException;
import freenet.node.RequestStarter;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.List;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class DataHandler extends Handler {

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
            if (data != null) {
                handleData();
            } else if (message != null) {
                if (action.equalsIgnoreCase("insert")) {
                    handleInsert();
                } else if (action.equalsIgnoreCase("fetch")) {
                    handleFetch();
                } else if (action.equalsIgnoreCase("sendmimetype")){
                    handleSendMimeType();
                } else if (action.equalsIgnoreCase("senddata")){
                    handleSendData();
                } else if (action.equalsIgnoreCase("clearinsertqueue")) {
                    handleClearInsertQueue();
                } else if (action.equalsIgnoreCase("clearfetchqueue")) {
                    handleClearFetchQueue();
                }else {
                    sendActionNotImplementedErrorReply();
                }
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } catch (MissingFieldException ex) {
            sendMissingFieldErrorReply(ex.getMessage());
        }
    }

    private void handleInsert() {
        String contentType = (String) jsonmessage.get("contentType");
        if (contentType == null) {
            this.sendMissingFieldErrorReply("Missing contentType field!");
            return;
        }
        String insertKey = (String) jsonmessage.get("insertKey");
        if (insertKey == null) {
            this.sendMissingFieldErrorReply("Missing insertKey field!");
            return;
        }
        String filename = (String) jsonmessage.get("filename");
        
        Integer version = (Integer) jsonmessage.get("version");
        if (version == null) {
            version = -1;
        }
        
        Short priority = (Short) jsonmessage.get("priority");
        if (priority == null) {
            priority = RequestStarter.INTERACTIVE_PRIORITY_CLASS;
        }
        
        Boolean persistent = (Boolean) jsonmessage.get("persistent");
        if (persistent == null) {
            persistent = false;
        }

        Boolean realtime = (Boolean) jsonmessage.get("realtime");
        if (realtime == null) {
            realtime = false;
        }
        DataInsert newInsert = new DataInsert(this, refmid, contentType, insertKey, filename, version, priority, persistent, realtime);
        dataInserts.add(newInsert);
        JSONObject response = createJSONReplyMessage("status");
        response.put("status", "INSERT_METADATA_RECEIVED");
        response.put("hint", "wait for senddata request to send data");
        ws.send(response.toJSONString());
        if (canRequestData()) {
            newInsert.requestData();
        }
    }

    private void handleData() {
        DataInsert insert;
        if ((insert = getInsertWaitingForData()) == null) {
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
        } catch (MalformedURLException ex) {
            this.sendWrongFieldValueErrorReply("Wrong insert Key or filename! " + ex.getMessage());
        } catch (InsertWithEmptyDataException ex) {
            this.sendServerErrorReply(ex.getMessage());
        } catch (IOException ex) {
            this.sendServerErrorReply(ex.getMessage());
        } catch (InsertException ex) {
            this.sendServerErrorReply(ex.getMessage());
        } catch (PluginNotFoundException ex) {
            this.sendServerErrorReply(ex.getMessage());
        }

    }

    private void handleClearInsertQueue() {
        try {
            JSONObject response = createJSONReplyMessage("success");
            if (!dataInserts.isEmpty()) {
                dataInserts.clear();
                response.put("message", "Insert queue cleared");
            } else {
                response.put("message", "Insert queue already empty");
            }
            ws.send(response.toJSONString());
        } catch (Exception ex) {
            this.sendServerErrorReply(ex.getMessage() + " " + ex.toString());
        }
    }

    private void handleFetch() {
        try {
            String url = (String) jsonmessage.get("url");
            if (url == null) {
                this.sendMissingFieldErrorReply("Missing url field!");
                return;
            }
            Short priority = (Short) jsonmessage.get("priority");
            if (priority == null) {
                priority = RequestStarter.INTERACTIVE_PRIORITY_CLASS;
            }
            Boolean realtime = (Boolean) jsonmessage.get("realtime");
            if (realtime == null) {
                realtime = false;
            }
            Boolean persistent = (Boolean) jsonmessage.get("persistent");
            if (persistent == null) {
                persistent = false;
            }
            
            DataFetch newFetch = new DataFetch(this, refmid, url, priority, persistent, realtime);
            newFetch.fetch();
            dataFetches.add(newFetch);
            JSONObject response = createJSONReplyMessage("status");
            response.put("status", "FETCH_STARTED");
            response.put("hint", "wait for dataready message");
            ws.send(response.toJSONString());
        } catch (PluginNotFoundException ex) {
            this.sendServerErrorReply("Indynet plugin is not loaded!");
        } catch (IOException ex) {
            this.sendServerErrorReply("Interplugin communication error!");
        }
    }
    
    private void handleSendMimeType(){
        String fetchmid = (String) jsonmessage.get("fetchmid");
        if (fetchmid == null) {
            this.sendMissingFieldErrorReply("Missing fetchmid field!");
            return;
        }
        DataFetch fetch = getDataFetch(fetchmid);
        if (fetch == null){
            this.sendBadRequestErrorReply("Fetch request not found!");
            return;
        }
        JSONObject response = createJSONReplyMessage("success");
        try {
            response.put("mimeType", fetch.getMimeType());
            ws.send(response.toJSONString());
        } catch (GetMimeTypeWithEmptyMimeTypeException ex) {
           this.sendBadRequestErrorReply("MimeType is empty!");
        }
    }
    
    private void handleSendData(){
        String fetchmid = (String) jsonmessage.get("fetchmid");
        if (fetchmid == null) {
            this.sendMissingFieldErrorReply("Missing fetchmid field!");
            return;
        }
        DataFetch fetch = getDataFetch(fetchmid);
        if (fetch == null){
            this.sendBadRequestErrorReply("Fetch request not found!");
            return;
        }
        try {
            ws.send(fetch.getData());
        } catch (GetDataWithEmptyDataException ex) {
            this.sendBadRequestErrorReply("Data is empty!");
        }
    }
    
    private void handleClearFetchQueue() {
        try {
            JSONObject response = createJSONReplyMessage("success");
            if (!dataFetches.isEmpty()) {
                dataFetches.clear();
                response.put("message", "Fetch queue cleared");
            } else {
                response.put("message", "Fetch queue already empty");
            }
            ws.send(response.toJSONString());
        } catch (Exception ex) {
            this.sendServerErrorReply(ex.getMessage() + " " + ex.toString());
        }
    }

    private Boolean canRequestData() {
        Boolean canRequestData = true;
        for (DataInsert insert : dataInserts) {
            if (insert.requestForDataSent() && !insert.hasGotData()) {
                canRequestData = false;
                break;
            }
        }
        return canRequestData;
    }

    private DataInsert getInsertWaitingForData() {
        for (DataInsert insert : dataInserts) {
            if (insert.requestForDataSent() && !insert.hasGotData()) {
                return insert;
            }
        }
        return null;
    }
    
    
    private DataFetch getDataFetch(String fetchmid){
        for (DataFetch fetch : dataFetches) {
            if (fetch.getRefmid().equals(fetchmid)){
                return fetch;
            }
        }
        return null;
    }

}
