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
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author ktogias
 */
class DataFetch {
    private final Handler handler;
    private final String refmid;
    private final Short priority;
    private final Boolean persistent;
    private final Boolean realtime;
    private final String url;
    
    private String mimeType = null;
    private ByteBuffer data = null;
    
    
    
    

    public DataFetch(Handler handler, String refmid, String url, Short priority, Boolean persistent, Boolean realtime) {
        this.refmid = refmid;
        this.url = url;
        this.priority = priority;
        this.persistent = persistent;
        this.realtime = realtime;
        this.handler = handler;
    }

    public Boolean hasGotData(){
        return data != null;
    }

    public ByteBuffer getData() throws GetDataWithEmptyDataException {
        if (data == null){
            throw new GetDataWithEmptyDataException("Get data called but data is empty!");
        }
        return data;
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

    public void fetch() throws PluginNotFoundException, IOException{
        PluginRespirator pr = handler.getPluginRespirator();
        FCPPluginConnection connection = pr.connectToOtherPlugin(handler.getIndynetPluginName(), new FetchCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "fetchData");
        params.putSingle("url", url);
        params.put("persistent", persistent);
        params.put("realtime", realtime);
        params.put("priorityClass", priority);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, null);
        connection.send(fcpMessage); 
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
    
    public String getMimeType() throws GetMimeTypeWithEmptyMimeTypeException{
        if (mimeType == null){
            throw new GetMimeTypeWithEmptyMimeTypeException("Get MimeType called but data is empty!");
        }
        return mimeType;
    }
    
    private class FetchCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success && fcppm.params.get("origin").equalsIgnoreCase("FetchData")) {
                JSONObject response = handler.createJSONReplyMessage("success");
                response.put("fetchedURI", fcppm.params.get("fetchedURI"));
                handler.getWebSocket().send(response.toJSONString());
                try {
                    mimeType = fcppm.params.get("mimeType");
                    data = Util.bucketToByteBuffer(fcppm.data);
                    JSONObject request = handler.createJSONReplyMessage("dataready");
                    handler.ws.send(request.toJSONString());
                } catch (IOException ex) {
                    handler.sendServerErrorReply("Failed to prepare fetched data!");
                }
            } 
            else if (fcppm.params.get("origin").equalsIgnoreCase("FetchData") 
                    && fcppm.params.get("status").equalsIgnoreCase("progress")){
                JSONObject response = handler.createJSONReplyMessage("progress");
                try {
                    JSONObject obj = Util.SimpleFieldSetToJSONObject(fcppm.params);
                    response.putAll(obj);
                } catch (FSParseException ex) {
                    response.put("error", Util.exceptionToJson(ex));
                }
                handler.getWebSocket().send(response.toJSONString());
                
            } else if (fcppm.params.get("origin").equalsIgnoreCase("FetchData") 
                    && fcppm.params.get("status").equalsIgnoreCase("Failure")){
                handler.sendServerErrorReply(fcppm.errorCode);
                getHandler().getDataFetches().remove(DataFetch.this);
            } else if (fcppm.params.get("origin").equalsIgnoreCase("FetchCallback") 
                    && fcppm.params.get("status").equalsIgnoreCase("ReceivedEvent")){
                JSONObject response = handler.createJSONReplyMessage("progress");
                try {
                    JSONObject obj = Util.SimpleFieldSetToJSONObject(fcppm.params);
                    response.putAll(obj);
                } catch (FSParseException ex) {
                    response.put("error", Util.exceptionToJson(ex));
                }
                handler.getWebSocket().send(response.toJSONString());
            }
            
            return FCPPluginMessage.construct();
        }
    }
    
}
