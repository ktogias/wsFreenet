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
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginRespirator;
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
    private final Boolean realtime;
    private ByteBuffer data = null;
    private Boolean requestForDataSent;
    
    

    public DataInsert(Handler handler, String refmid, String contentType, String insertKey, String filename, Short priority, Boolean realtime) {
        this.refmid = refmid;
        this.contentType = contentType;
        this.insertKey = insertKey;
        this.filename = filename;
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

    public void insert() throws InsertWithEmptyDataException, IOException, InsertException, MalformedURLException{
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
        ClientMetadata metadata = new ClientMetadata(contentType);
        String key = insertKey;
        if (!filename.isEmpty()){
            if (!key.endsWith("/")){
                key+= "/";
            }
            key+=filename;
        }
        FreenetURI insertUri = new FreenetURI(insertKey+filename);
        InsertBlock ib = new InsertBlock(bucket, metadata, insertUri);
        HighLevelSimpleClient client = pr.getHLSimpleClient();
        InsertContext ictx = new InsertContext(client.getInsertContext(true), new SimpleEventProducer());
        InsertCallback callback = new InsertCallback(this, ictx, bucket, false, realtime);
        callback.subscribeToContextEvents();
        ClientPutter pu = 
            client.insert(ib, null, false, ictx, callback, priority);
        callback.setClientPutter(pu);
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
    
}
