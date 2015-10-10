/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.client.events.ClientEvent;
import freenet.client.events.ClientEventListener;
import freenet.client.events.ExpectedHashesEvent;
import freenet.client.events.FinishedCompressionEvent;
import freenet.client.events.SplitfileProgressEvent;
import freenet.client.events.StartedCompressionEvent;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.ResumeFailedException;
import java.sql.Timestamp;
import java.util.TimeZone;
import org.json.simple.JSONObject;

/**
 *
 * @author ktogias
 */
class InsertCallbackOld implements ClientPutCallback, RequestClient, ClientEventListener{
    private final RandomAccessBucket bucket;
    private ClientPutter clientPutter;
    private final boolean persistent;
    private final boolean realtime;
    private final DataInsert insertObject;
    private InsertContext context;
    
    public InsertCallbackOld(DataInsert insertObject, InsertContext ictx, RandomAccessBucket bucket, boolean persistent, boolean realtime) {
        this.bucket = bucket;
        this.persistent = persistent;
        this.realtime = realtime;
        this.insertObject = insertObject;
        this.context = ictx;
    }
    /**
     * Setter for clientPutter
     *
     * @param clientPutter ClientPutter : The ClientPutter object
     */
    public void setClientPutter(ClientPutter clientPutter) {
        this.clientPutter = clientPutter;
    }

    /**
     * Method to cancel the insert
     *
     * When called the onging insert is cancelled and rhe bucket is destroyed
     */
    public void cancel() {
        clientPutter.cancel(insertObject.getPluginRespirator().getNode().clientCore.clientContext);
        bucket.free();
        unsubscribeFromContextEvents();
        insertObject.getHandler().getDataInserts().remove(insertObject);
        insertObject.getHandler().sendErrorReply("CANCELED" , "Data insert was canceled!");
    }

    @Override
    public void onGeneratedURI(FreenetURI furi, BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "GeneratedURI");
        response.put("URI", furi.toString());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onGeneratedMetadata(Bucket bucket, BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "GeneratedMetadata");
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onFetchable(BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "Fetchable");
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onSuccess(BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("success");
        response.put("requestURI", bcp.getURI().toString());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
        bucket.free();
        unsubscribeFromContextEvents();
        insertObject.getHandler().getDataInserts().remove(insertObject);
    }

    @Override
    public void onFailure(InsertException ie, BaseClientPutter bcp) {
        insertObject.getHandler().sendErrorReply("INSERT_FAILURE", "Insert failed");
        bucket.free();
        unsubscribeFromContextEvents();
        insertObject.getHandler().getDataInserts().remove(insertObject);
    }

    @Override
    public void onResume(ClientContext cc) throws ResumeFailedException {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("status");
        response.put("triger", "onResume");
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public RequestClient getRequestClient() {
        return this;
    }

    @Override
    public boolean persistent() {
        return persistent;
    }

    @Override
    public boolean realTimeFlag() {
        return realtime;
    }

    @Override
    public void receive(ClientEvent ce, ClientContext cc) {
        if(ce instanceof SplitfileProgressEvent) {
            handleEvent((SplitfileProgressEvent)ce);
        }
        else if (ce instanceof StartedCompressionEvent){
            handleEvent((StartedCompressionEvent)ce);
        }
        else if (ce instanceof FinishedCompressionEvent){
            handleEvent((FinishedCompressionEvent)ce);
        }
        else if (ce instanceof ExpectedHashesEvent){
            handleEvent((ExpectedHashesEvent)ce);
        }
        else {
            handleEvent(ce);
        }
        
        
    }

    public void subscribeToContextEvents(){
        context.eventProducer.addEventListener(this);
    }
    
    public void unsubscribeFromContextEvents(){
        context.eventProducer.removeEventListener(this);
    }
    
    private void handleEvent(SplitfileProgressEvent ce){
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "SplitfileProgress");
        response.put("finalizedTotal", ce.finalizedTotal);
        response.put("totalBlocks", ce.totalBlocks);
        response.put("succeedBlocks", ce.succeedBlocks);
        response.put("failedBlocks", ce.failedBlocks);
        response.put("fatallyFailedBlocks", ce.fatallyFailedBlocks);
        response.put("minSuccessFetchBlocks", ce.minSuccessFetchBlocks);
        response.put("minSuccessfulBlocks", ce.minSuccessfulBlocks);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        if (ce.latestSuccess != null){
            Timestamp latestSuccess = new Timestamp(ce.latestSuccess.getTime());
            response.put("latestSuccess", latestSuccess.toLocalDateTime().toString());
        }
        if (ce.latestFailure != null){
            Timestamp latestFailure = new Timestamp(ce.latestFailure.getTime());
            response.put("latestFailure", latestFailure.toLocalDateTime().toString());
        }
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(StartedCompressionEvent ce){
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "StartedCompression");
        response.put("codec", ce.codec.name);
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(FinishedCompressionEvent ce){
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "FinishedCompression");
        response.put("codec", ce.codec);
        response.put("originalSize", ce.originalSize);
        response.put("compressedSize", ce.compressedSize);
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(ExpectedHashesEvent ce){
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "ExpectedHashes");
        response.put("hashesLength", ce.hashes.length);
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(ClientEvent ce){
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", ce.getClass().getName());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    
}
