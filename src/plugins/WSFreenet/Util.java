/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.node.FSParseException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
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
        try {
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println(message);
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static public Handler getHandler(String resource, WebSocket ws, String message, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        resource = resource.substring(1,2).toUpperCase() + resource.substring(2).toLowerCase();
        Class<?> handler = Class.forName("plugins.WSFreenet."+resource+"Handler");
        Class[] types = {WebSocket.class, String.class, PluginRespirator.class, String.class, List.class, List.class};
        Constructor constructor = handler.getConstructor(types);
        Object[] parameters = {ws, message, pr, indynetPluginName, dataInserts, dataFetches};
        Handler handlerInstance = (Handler)constructor.newInstance(parameters);
        return handlerInstance;
    }
    
    static public Handler getHandler(String resource, WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName, List<DataInsert> dataInserts, List<DataFetch> dataFetches) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        resource = resource.substring(1,2).toUpperCase() + resource.substring(2).toLowerCase();
        Class<?> handler = Class.forName("plugins.WSFreenet."+resource+"Handler");
        Class[] types = {WebSocket.class, ByteBuffer.class, PluginRespirator.class, String.class, List.class, List.class};
        Constructor constructor = handler.getConstructor(types);
        Object[] parameters = {ws, data, pr, indynetPluginName, dataInserts, dataFetches};
        Handler handlerInstance = (Handler)constructor.newInstance(parameters);
        return handlerInstance;
    }
    
    static public JSONObject parseJSONMessage(String message) throws ParseException{
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(message);
        return json;
    }
    
    static public JSONObject getErrorReply(String refmid, String code, String message){
        JSONObject reply = new JSONObject();
        reply.put("type", "reply");
        if (refmid != null){
            reply.put("refmid", refmid);
        }
        reply.put("result", "error");
        reply.put("errorcode", code);
        reply.put("errormessage", message);
        return reply;
    }
    
    static public JSONObject getNotFoundErrorReply(){
        return getNotFoundErrorReply(null);
    }
    
    static public JSONObject getNotFoundErrorReply(String refmid){
        return getNotFoundErrorReply(refmid, "Resource not found!");
    }
    
    static public JSONObject getNotFoundErrorReply(String refmid, String message){
        return getErrorReply(refmid,"404", message);
    }
    
    static public JSONObject getBadRequestErrorReply(){
        return getBadRequestErrorReply(null);
    }
    
    static public JSONObject getBadRequestErrorReply(String refmid){
        return getBadRequestErrorReply(refmid, "Bad request!");
    }
    
    static public JSONObject getBadRequestErrorReply(String refmid, String message){
        return getErrorReply(refmid, "400", message);
    }
    
    static public JSONObject getServerErrorReply(){
        return getServerErrorReply(null);
    }
    
    static public JSONObject getServerErrorReply(String refmid){
        return getServerErrorReply(refmid, "Server error!");
    }
    
    static public JSONObject getServerErrorReply(String refmid, String message){
        return getErrorReply(refmid, "500", message);
    }
    
    static public JSONObject getNotImplementedErrorReply(String refmid){
        return getServerErrorReply(refmid, "Not Implemented!");
    }
    
    static public JSONObject getNotImplementedErrorReply(String refmid, String message){
        return getErrorReply(refmid, "501", message);
    }
    
    static public JSONObject SimpleFieldSetToJSONObject(SimpleFieldSet params) throws FSParseException{
        JSONObject obj = new JSONObject();
        Iterator<String> keyIterator = params.keyIterator();
        while (keyIterator.hasNext()){
            String key = keyIterator.next();
            obj.put(key, params.get(key));
        }
        return obj;
    }
    
    public static JSONObject exceptionToJson(Exception ex) {
        JSONObject errorObject = new JSONObject();
        errorObject.put("exception", ex.getClass().getName());
        errorObject.put("message", ex.getMessage());
        JSONArray trace = new JSONArray();
        for (StackTraceElement element: ex.getStackTrace()){
            trace.add(element.toString());
        }
        errorObject.put("trace", trace);
        return errorObject;
    }
}
