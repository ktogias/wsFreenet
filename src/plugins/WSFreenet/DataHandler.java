/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.PluginRespirator;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ktogias
 */
public class DataHandler extends Handler{

    public DataHandler(WebSocket ws, String message, PluginRespirator pr, String indynetPluginName) {
        super(ws, message, pr, indynetPluginName);
    }
    
    public DataHandler(WebSocket ws, ByteBuffer data, PluginRespirator pr, String indynetPluginName) {
        super(ws, data, pr, indynetPluginName);
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
            }
        } catch (ParseException ex) {
            sendJsonParseErrorReply();
        } catch (MissingFieldException ex) {
            sendMissingFieldErrorReply(ex.getMessage());
        }
    }
    
    private void handleInsert(){
        ws.send(message);
    }
    
    private void handleData(){
        ws.send(data);
    }
    
}
