/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.WSFreenet;

import freenet.io.AllowedHosts;
import freenet.pluginmanager.PluginRespirator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
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
    boolean ssl;

    public WSFreenetServer( int port, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts, boolean ssl, String sslKeystoreFile, String sslKeystoreFilePassword) throws UnknownHostException, KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, SignatureException, InvalidKeyException, IOException, FileNotFoundException, CertificateException, CertificateEncodingException, UnrecoverableKeyException, KeyManagementException {
        super( new InetSocketAddress( port ) );
        this.allowedHosts = new AllowedHosts(String.join(",", allowedHosts));
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
        this.ssl = ssl;
        if (ssl){
            initSSL(sslKeystoreFile, sslKeystoreFilePassword);
        }
    }
    
    public WSFreenetServer( int port, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts) throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, SignatureException, InvalidKeyException, IOException, UnknownHostException, FileNotFoundException, CertificateException, CertificateEncodingException, UnrecoverableKeyException, KeyManagementException{
        this(port, allowedHosts, pr, indynetPluginName, dataInserts, false, "", "");
    }

    public WSFreenetServer( InetSocketAddress address, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts, boolean ssl,  String sslKeystoreFile, String sslKeystoreFilePassword) throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, SignatureException, InvalidKeyException, IOException, FileNotFoundException, CertificateException, CertificateEncodingException, UnrecoverableKeyException, KeyManagementException {
        super( address );
        this.allowedHosts = new AllowedHosts(String.join(",", allowedHosts));
        this.pr = pr;
        this.indynetPluginName = indynetPluginName;
        this.dataInserts = dataInserts;
        this.ssl = ssl;
        if (ssl){
            initSSL(sslKeystoreFile, sslKeystoreFilePassword);
        }
    }
    
    public WSFreenetServer( InetSocketAddress address, String[] allowedHosts, PluginRespirator pr, String indynetPluginName, Map<Integer, List<DataInsert>> dataInserts) throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, SignatureException, InvalidKeyException, IOException, UnknownHostException, FileNotFoundException, CertificateException, CertificateEncodingException, UnrecoverableKeyException, KeyManagementException{
        this(address, allowedHosts, pr, indynetPluginName, dataInserts, false, "", "");
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
    
    private void initSSL(String filename, String password) throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, IllegalStateException, SignatureException, InvalidKeyException, FileNotFoundException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException{
        KeyStore keystore = KeyStore.getInstance( "JKS" );
        File keyfile = new File( filename );
        if(keyfile.exists() && !keyfile.isDirectory()) {
             keystore.load( new FileInputStream( keyfile ), password.toCharArray());
        }
        else {
            keystore.load(null, null);
            X509Certificate cert = generateCertificate();
            keystore.setCertificateEntry("default", cert);
            keystore.store(new FileOutputStream(keyfile), password.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( keystore, password.toCharArray() );
        TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
        tmf.init( keystore );
        SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
        this.setWebSocketFactory(new DefaultSSLWebSocketServerFactory( sslContext ));
    }
    
    private X509Certificate generateCertificate() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, IllegalStateException, SignatureException, InvalidKeyException{
        // yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // in 10 years
        Date validityEndDate = new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000);
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024, new SecureRandom());
        
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=wsFreenet Plugin");
        
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName); // use the same
        certGen.setNotBefore(validityBeginDate);
        certGen.setNotAfter(validityEndDate);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
        
        return cert;

    }

    
}
