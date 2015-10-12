/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
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
class FetchCallbackOld implements ClientGetCallback, RequestClient, ClientEventListener{
    private ClientGetter clientGetter;
    private final DataFetch fetchObject;
    private final FetchContext context;
    boolean persistent;
    boolean realtime;
    
    public FetchCallbackOld(DataFetch fetchObject, FetchContext fctx, boolean persistent, boolean realtime) {
        this.fetchObject = fetchObject;
        this.context = fctx;
        this.persistent = persistent;
        this.realtime = realtime;
    }
    /**
     * Setter for clientPutter
     *
     * @param clientPutter ClientPutter : The ClientPutter object
     */
    public void setClientGetter(ClientGetter clientGetter) {
        this.clientGetter = clientGetter;
    }

    /**
     * Method to cancel the insert
     *
     * When called the onging insert is cancelled and rhe bucket is destroyed
     */
    public void cancel() {
        clientGetter.cancel(fetchObject.getPluginRespirator().getNode().clientCore.clientContext);
        unsubscribeFromContextEvents();
        fetchObject.getHandler().getDataFetches().remove(fetchObject);
        fetchObject.getHandler().sendErrorReply("CANCELED" , "Data fetch was canceled!");
    }

    @Override
    public RequestClient getRequestClient() {
        return this;
    }
    
    
    @Override
    public void onSuccess(FetchResult fr, ClientGetter cg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onFailure(FetchException fe, ClientGetter cg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onResume(ClientContext cc) throws ResumeFailedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        JSONObject response = fetchObject.getHandler().createJSONReplyMessage("progress");
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
        fetchObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(StartedCompressionEvent ce){
        JSONObject response = fetchObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "StartedCompression");
        response.put("codec", ce.codec.name);
        fetchObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(FinishedCompressionEvent ce){
        JSONObject response = fetchObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "FinishedCompression");
        response.put("codec", ce.codec);
        response.put("originalSize", ce.originalSize);
        response.put("compressedSize", ce.compressedSize);
        fetchObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(ExpectedHashesEvent ce){
        JSONObject response = fetchObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", "ExpectedHashes");
        response.put("hashesLength", ce.hashes.length);
        fetchObject.getHandler().getWebSocket().send(response.toJSONString());
    }
    
    private void handleEvent(ClientEvent ce){
        JSONObject response = fetchObject.getHandler().createJSONReplyMessage("progress");
        response.put("event", ce.getClass().getName());
        fetchObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    
    
    
}
