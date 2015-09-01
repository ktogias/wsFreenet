/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.PluginRespirator;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class Util {
    static public String[] JSONArrayToStringArray(JSONArray jsarray){
        String [] sarray = new String [jsarray.size()];
        for (int i=0;i<sarray.length;i++){
            sarray[i] = jsarray.get(i).toString();
        }
        return sarray;
    }
    
    static public void debug(String filename, String message){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename, "UTF-8");
            writer.println(message);
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static public Handler getHandler(String resource, WebSocket ws, String message, PluginRespirator pr, String indynetPluginName) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        resource = resource.substring(1,2).toUpperCase() + resource.substring(2).toLowerCase();
        Class<?> handler = Class.forName("plugins.WSFreenet."+resource+"Handler");
        Class[] types = {WebSocket.class, String.class, PluginRespirator.class, String.class};
        Constructor constructor = handler.getConstructor(types);
        Object[] parameters = {ws, message, pr, indynetPluginName};
        Handler handlerInstance = (Handler)constructor.newInstance(parameters);
        return handlerInstance;
    }
    
    static public Handler getHandler(String resource, WebSocket ws, ByteBuffer data, PluginRespirator pr) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        resource = resource.substring(0,1).toUpperCase() + resource.substring(1).toLowerCase();
        Class<?> handler = Class.forName(resource+"Handler");
        Class[] types = {WebSocket.class, ByteBuffer.class, PluginRespirator.class};
        Constructor constructor = handler.getConstructor(types);
        Object[] parameters = {ws, data, pr};
        Handler handlerInstance = (Handler)constructor.newInstance(parameters);
        return handlerInstance;
    }
    
    static public JSONObject parseJSONMessage(String message) throws ParseException{
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(message);
        return json;
    }
    
    static public JSONObject getErrorReply(int code, String message){
        JSONObject reply = new JSONObject();
        reply.put("error", true);
        reply.put("code", code);
        reply.put("message", message);
        return reply;
    }
    
    
    static public JSONObject getNotFoundErrorReply(){
        return getNotFoundErrorReply("Resource not found!");
    }
    
    static public JSONObject getNotFoundErrorReply(String message){
        return getErrorReply(404, message);
    }
    
    static public JSONObject getBadRequestErrorReply(){
        return getBadRequestErrorReply("Bad request!");
    }
    
    static public JSONObject getBadRequestErrorReply(String message){
        return getErrorReply(400, message);
    }
    
    static public JSONObject getServerErrorReply(){
        return getServerErrorReply("Server error!");
    }
    
    static public JSONObject getServerErrorReply(String message){
        return getErrorReply(500, message);
    }
    
    static public JSONObject getNotImplementedErrorReply(){
        return getServerErrorReply("Not Implemented!");
    }
    
    static public JSONObject getNotImplementedErrorReply(String message){
        return getErrorReply(501, message);
    }
    
    
}
