/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.PluginRespirator;
import java.nio.ByteBuffer;
import java.util.List;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public abstract class Handler {
    protected WebSocket ws;
    protected String message;
    protected ByteBuffer data;
    protected PluginRespirator pr;
    protected String indynetPluginName;
    protected String refmid;
    protected String action;
    protected JSONObject jsonmessage;
    protected List<DataInsert> dataInserts;
    protected List<DataFetch> dataFetches;
    
    
    public Handler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches){
        this.ws = ws;
        this.message = message;
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
        this.dataFetches = dataFetches;
    }
    
    public Handler(WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches){
        this.ws = ws;
        this.data = data;
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
        this.dataFetches = dataFetches;
    }
    
    public void handle() throws ParseException, MissingFieldException{
        if (message != null){
            jsonmessage = Util.parseJSONMessage(message);
            refmid = (String)jsonmessage.get("mid");
            if (refmid == null){
                throw new MissingFieldException("No mid specified!");
            }
            action = (String)jsonmessage.get("action");
            if (action == null){
                throw new MissingFieldException("No action specified!");
            }
        }
    }
    
    public void sendErrorReply(String code, String message){
        ws.send(Util.getErrorReply(refmid, code, message).toJSONString());
    }
    
    public void sendNotFoundErrorReply(){
        ws.send(Util.getNotFoundErrorReply(refmid).toJSONString());
    }
    
    public void sendNotFoundErrorReply(String message){
        ws.send(Util.getNotFoundErrorReply(refmid, message).toJSONString());
    }
    
    public void sendBadRequestErrorReply(){
        ws.send(Util.getBadRequestErrorReply(refmid).toJSONString());
    }
    
    public void sendBadRequestErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(refmid, message).toJSONString());
    }
    
    public void sendServerErrorReply(){
        ws.send(Util.getServerErrorReply(refmid).toJSONString());
    }
    
    public void sendServerErrorReply(String message){
        ws.send(Util.getServerErrorReply(refmid, message).toJSONString());
    }
    
    public void sendActionNotImplementedErrorReply(){
        ws.send(Util.getNotImplementedErrorReply(refmid).toJSONString());
    }
    
    public void sendActionNotImplementedErrorReply(String message){
        ws.send(Util.getNotImplementedErrorReply(refmid, message).toJSONString());
    }
    
    public void sendJsonParseErrorReply(){
        sendJsonParseErrorReply("Could not parse JSON!");
    }
    
    public void sendJsonParseErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(refmid, message).toJSONString());
    }
    
    public void sendMissingFieldErrorReply(){
        sendMissingFieldErrorReply("A required field is missing from sent object!");
    }
    
    public void sendMissingFieldErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(refmid, message).toJSONString());
    }
    
    public void sendWrongFieldValueErrorReply(){
        sendWrongFieldValueErrorReply("Wrong field value in sent object!");
    }
    
    public void sendWrongFieldValueErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(refmid, message).toJSONString());
    }
    
    public JSONObject parseJSONMessage(String message) throws ParseException{
        return Util.parseJSONMessage(message);
    }
    
    public JSONObject createJSONReplyMessage(String result){
        JSONObject response = new JSONObject();
        response.put("type", "reply");
        response.put("refmid", refmid);
        response.put("result", result.toLowerCase());
        return response;
    }
    
    public JSONObject createJSONRequestMessage(String action){
        JSONObject request = new JSONObject();
        request.put("type", "request");
        request.put("refmid", refmid);
        request.put("action", action);
        return request;
    }
    
    public List<DataInsert> getDataInserts(){
        return dataInserts;
    }
    
    public List<DataFetch> getDataFetches(){
        return dataFetches;
    }
    
    public PluginRespirator getPluginRespirator(){
        return pr;
    }
    
    public WebSocket getWebSocket(){
        return ws;
    }
    
    public String getIndynetPluginName(){
        return indynetPluginName;
    }

}
