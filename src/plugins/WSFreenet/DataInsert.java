/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.ClientMetadata;
import freenet.client.HighLevelSimpleClient;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.ClientPutter;
import freenet.client.events.SimpleEventProducer;
import freenet.clients.fcp.FCPPluginConnection;
import freenet.clients.fcp.FCPPluginMessage;
import freenet.keys.FreenetURI;
import freenet.node.FSParseException;
import freenet.pluginmanager.FredPluginFCPMessageHandler;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import freenet.support.api.RandomAccessBucket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author ktogias
 */
class DataInsert {
    private final Handler handler;
    private final String refmid;
    private final String contentType;
    private final String insertKey;
    private final String filename;
    private final Short priority;
    private final Boolean persistent;
    private final Boolean realtime;
    private final int version;
    private ByteBuffer data = null;
    private Boolean requestForDataSent;
    
    

    public DataInsert(Handler handler, String refmid, String contentType, String insertKey, String filename, int version, Short priority, Boolean persistent, Boolean realtime) {
        this.refmid = refmid;
        this.contentType = contentType;
        this.insertKey = insertKey;
        this.filename = filename;
        this.version = version;
        this.priority = priority;
        this.persistent = persistent;
        this.realtime = realtime;
        this.handler = handler;
        this.requestForDataSent = false;
    }

    public Boolean hasGotData(){
        return data != null;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
        DataInsert nextInsert = getNextInsert();
        if (nextInsert != null && !nextInsert.requestForDataSent()){
            nextInsert.requestData();
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

    public void insert() throws InsertWithEmptyDataException, IOException, InsertException, MalformedURLException, PluginNotFoundException{
        if (this.data == null){
            throw new InsertWithEmptyDataException("Insert called but data is empty!");
        }
        int len = data.remaining();
        PluginRespirator pr = handler.getPluginRespirator();
        RandomAccessBucket bucket = pr.getToadletContainer().getBucketFactory().makeBucket(len);
        OutputStream os = bucket.getOutputStream();
        os.write(data.array());
        os.close(); 
        bucket.setReadOnly();
        FCPPluginConnection connection = pr.connectToOtherPlugin(handler.getIndynetPluginName(), new InsertCallback());
        SimpleFieldSet params = new SimpleFieldSet(false);
        params.putSingle("action", "insertData");
        params.putSingle("insertKey", insertKey);
        params.putSingle("filename", filename);
        params.putSingle("contentType", contentType);
        params.put("version", version);
        params.put("persistent", persistent);
        params.put("realtime", realtime);
        params.put("priorityClass", priority);
        FCPPluginMessage fcpMessage = FCPPluginMessage.construct(params, bucket);
        connection.send(fcpMessage);
    }
    
    public void requestData() {
        JSONObject request = handler.createJSONRequestMessage("senddata");
        handler.ws.send(request.toJSONString());
        this.requestForDataSent = true;
    }
    
    public boolean requestForDataSent(){
        return this.requestForDataSent;
    }
    
    public DataInsert getNextInsert(){
        try {
            int nextIndex = handler.getDataInserts().indexOf(this)+1;
            return handler.getDataInserts().get(nextIndex);
        }
        catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    
    private class InsertCallback implements FredPluginFCPMessageHandler.ClientSideFCPMessageHandler {

        @Override
        public FCPPluginMessage handlePluginFCPMessage(FCPPluginConnection fcppc, FCPPluginMessage fcppm) {
            if (fcppm.success && fcppm.params.get("origin").equalsIgnoreCase("DataInsert")) {
                JSONObject response = handler.createJSONReplyMessage("success");
                response.put("insertedURI", fcppm.params.get("insertedURI"));
                handler.getWebSocket().send(response.toJSONString());
            } else if (fcppm.params.get("origin").equalsIgnoreCase("DataInsert") 
                    && fcppm.params.get("status").equalsIgnoreCase("Failure")){
                handler.sendServerErrorReply(fcppm.errorCode);
            } else if (fcppm.params.get("origin").equalsIgnoreCase("InsertCallback") 
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
