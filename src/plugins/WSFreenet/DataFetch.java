/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginRespirator;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author ktogias
 */
class DataFetch {
    private final Handler handler;
    private final String refmid;
    private final Short priority;
    private final Boolean realtime;
    private ByteBuffer data = null;
    private Boolean requestForDataSent;
    
    private final String url;
    
    

    public DataFetch(Handler handler, String refmid, String url, Short priority, Boolean realtime) {
        this.refmid = refmid;
        this.url = url;
        this.priority = priority;
        this.realtime = realtime;
        this.handler = handler;
        this.requestForDataSent = false;
    }

    public Boolean hasGotData(){
        return data != null;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
        DataFetch nextInsert = getNextFetch();
        if (nextInsert != null && !nextInsert.requestForDataSent()){
//            nextInsert.requestData();
        }
    }

    public String getRefmid(){
        return refmid;
    }
    
    public PluginRespirator getPluginRespirator(){
        return handler.getPluginRespirator();
    }
    
    public Handler getHandler(){
        return handler;
    }

    public void fetch(){
       
        
    }
    
    public void sendData() {
        JSONObject request = handler.createJSONRequestMessage("senddata");
        handler.ws.send(request.toJSONString());
        this.requestForDataSent = true;
    }
    
    public boolean requestForDataSent(){
        return this.requestForDataSent;
    }
    
    public DataFetch getNextFetch(){
        try {
            int nextIndex = handler.getDataFetches().indexOf(this)+1;
            return handler.getDataFetches().get(nextIndex);
        }
        catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    
    
    
}
