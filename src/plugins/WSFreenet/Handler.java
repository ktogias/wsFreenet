/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.PluginRespirator;
import java.nio.ByteBuffer;
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
    
    
    public Handler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName){
        this.ws = ws;
        this.message = message;
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
    }
    
    public Handler(WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName){
        this.ws = ws;
        this.data = data;
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
    }
    
    abstract public void handle();
    
    public void sendErrorReply(int code, String message){
        ws.send(Util.getErrorReply(code, message).toJSONString());
    }
    
    public void sendNotFoundErrorReply(){
        ws.send(Util.getNotFoundErrorReply().toJSONString());
    }
    
    public void sendNotFoundErrorReply(String message){
        ws.send(Util.getNotFoundErrorReply(message).toJSONString());
    }
    
    public void sendBadRequestErrorReply(){
        ws.send(Util.getBadRequestErrorReply().toJSONString());
    }
    
    public void sendBadRequestErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(message).toJSONString());
    }
    
    public void sendServerErrorReply(){
        ws.send(Util.getServerErrorReply().toJSONString());
    }
    
    public void sendServerErrorReply(String message){
        ws.send(Util.getServerErrorReply(message).toJSONString());
    }
    
    public void sendActionNotImplementedErrorReply(){
        ws.send(Util.getNotImplementedErrorReply().toJSONString());
    }
    
    public void sendActionNotImplementedErrorReply(String message){
        ws.send(Util.getNotImplementedErrorReply(message).toJSONString());
    }
    
    public void sendJsonParseErrorReply(){
        sendJsonParseErrorReply("Could not parse JSON!");
    }
    
    public void sendJsonParseErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(message).toJSONString());
    }
    
    public void sendMissingFieldErrorReply(){
        sendMissingFieldErrorReply("A required field is missing from sent object!");
    }
    
    public void sendMissingFieldErrorReply(String message){
        ws.send(Util.getBadRequestErrorReply(message).toJSONString());
    }
    
    JSONObject parseJSONMessage(String message) throws ParseException{
        return Util.parseJSONMessage(message);
    }
}
