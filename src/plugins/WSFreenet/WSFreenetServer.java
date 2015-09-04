/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.io.AllowedHosts;
import freenet.pluginmanager.PluginRespirator;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 *
 * @author ktogias
 */
public class WSFreenetServer extends WebSocketServer {
    AllowedHosts allowedHosts;
    PluginRespirator pr;
    String indynetPluginName;
    Map<Integer, List<DataInsert>> dataInserts;

    public WSFreenetServer( int port, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        this.allowedHosts = new AllowedHosts(String.join(",", allowedHosts));
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
    }

    public WSFreenetServer( InetSocketAddress address, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts ) {
        super( address );
        this.allowedHosts = new AllowedHosts(String.join(",", allowedHosts));
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
    }
    
    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        if (!allowedHosts.allowed(ws.getRemoteSocketAddress().getAddress())){
            ws.closeConnection(-1, "Not Allowed");
            return;
        }
    
        Collections.synchronizedList(new ArrayList<DataInsert>());
        dataInserts.put(ws.hashCode(), Collections.synchronizedList(new ArrayList<DataInsert>()));
    }

    @Override
    public void onClose(WebSocket ws, int i, String string, boolean bln) {
        dataInserts.get(ws.hashCode()).clear();
        dataInserts.remove(ws.hashCode());
    }

    @Override
    public void onMessage(WebSocket ws, String message) {
        try {
            String resource = ws.getResourceDescriptor();
            Handler handler = Util.getHandler(resource, ws, message, pr, indynetPluginName, dataInserts.get(ws.hashCode()));
            handler.handle();
        } catch (ClassNotFoundException ex) {
            ws.send(Util.getNotFoundErrorReply().toJSONString());
            ws.close(404, "Resource not found");
        } catch (IllegalArgumentException ex) {
            ws.send(Util.getBadRequestErrorReply().toJSONString());
        } catch (Exception ex) {
            ws.send(Util.getServerErrorReply(null,ex.getMessage()+ex.toString()).toJSONString());
            Logger.getLogger(WSFreenetServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void onMessage(WebSocket ws, ByteBuffer data) {
        try {
            String resource = ws.getResourceDescriptor();
            Handler handler = Util.getHandler(resource, ws, data, pr, indynetPluginName, dataInserts.get(ws.hashCode()));
            handler.handle();
        } catch (ClassNotFoundException ex) {
            ws.close(404, "Resource not found");
        } catch (IllegalArgumentException ex) {
            ws.close(400, "Bad request");
        } catch (Exception ex) {
            Logger.getLogger(WSFreenetServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onError(WebSocket ws, Exception excptn) {
       
    }
    
    
    
}
