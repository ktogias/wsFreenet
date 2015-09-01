/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.pluginmanager.PluginRespirator;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
    String[] allowedHosts;
    PluginRespirator pr;
    String indynetPluginName;

    public WSFreenetServer( int port, String[] allowedHosts, PluginRespirator pr, String indynetPluginName) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        this.allowedHosts = allowedHosts;
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
    }

    public WSFreenetServer( InetSocketAddress address, String[] allowedHosts  ) {
        super( address );
    }
    
    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        String remoteAddress = ws.getRemoteSocketAddress().getAddress().getHostAddress();
        if (!Arrays.asList(allowedHosts).contains(remoteAddress)){
            ws.closeConnection(-1, "Not Allowed");
        }
    }

    @Override
    public void onClose(WebSocket ws, int i, String string, boolean bln) {
        
    }

    @Override
    public void onMessage(WebSocket ws, String message) {
        try {
            String resource = ws.getResourceDescriptor();
            Handler handler = Util.getHandler(resource, ws, message, pr, indynetPluginName);
            handler.handle();
        } catch (ClassNotFoundException ex) {
            ws.send(Util.getNotFoundErrorReply().toJSONString());
            ws.close(404, "Resource not found");
        } catch (IllegalArgumentException ex) {
            ws.send(Util.getBadRequestErrorReply().toJSONString());
        } catch (Exception ex) {
            ws.send(Util.getServerErrorReply().toJSONString());
            Logger.getLogger(WSFreenetServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void onMessage(WebSocket ws, ByteBuffer data) {
        try {
            String resource = ws.getResourceDescriptor();
            Handler handler = Util.getHandler(resource, ws, data, pr);
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
