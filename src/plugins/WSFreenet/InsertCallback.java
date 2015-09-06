/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.ResumeFailedException;
import org.json.simple.JSONObject;

/**
 *
 * @author ktogias
 */
class InsertCallback implements ClientPutCallback, RequestClient{
    private final RandomAccessBucket bucket;
    private ClientPutter clientPutter;
    private final boolean persistent;
    private final boolean realtime;
    private final DataInsert insertObject;
    
    public InsertCallback(DataInsert insertObject, RandomAccessBucket bucket, boolean persistent, boolean realtime) {
        this.bucket = bucket;
        this.persistent = persistent;
        this.realtime = realtime;
        this.insertObject = insertObject;
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
        insertObject.getHandler().getDataInserts().remove(insertObject);
        insertObject.getHandler().sendErrorReply("CANCELED" , "Data insert was canceled!");
    }

    @Override
    public void onGeneratedURI(FreenetURI furi, BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("status");
        response.put("triger", "onGeneratedURI");
        response.put("URI", furi.toString());
        response.put("MinSuccessFetchBlocks", bcp.getMinSuccessFetchBlocks());
        response.put("LatestSuccess", bcp.getLatestSuccess());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onGeneratedMetadata(Bucket bucket, BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("status");
        response.put("triger", "onGeneratedMetadata");
        response.put("MinSuccessFetchBlocks", bcp.getMinSuccessFetchBlocks());
        response.put("LatestSuccess", bcp.getLatestSuccess());
        response.put("bucketsize", bucket.size());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onFetchable(BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("status");
        response.put("triger", "onFetchable");
        response.put("MinSuccessFetchBlocks", bcp.getMinSuccessFetchBlocks());
        response.put("LatestSuccess", bcp.getLatestSuccess());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
    }

    @Override
    public void onSuccess(BaseClientPutter bcp) {
        JSONObject response = insertObject.getHandler().createJSONReplyMessage("success");
        response.put("requestURI", bcp.getURI().toString());
        insertObject.getHandler().getWebSocket().send(response.toJSONString());
        bucket.free();
        insertObject.getHandler().getDataInserts().remove(insertObject);
    }

    @Override
    public void onFailure(InsertException ie, BaseClientPutter bcp) {
        insertObject.getHandler().sendErrorReply("INSERT_FAILURE", "Insert failed");
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
}
