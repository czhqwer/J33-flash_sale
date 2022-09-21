package cn.wolfcode.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@ServerEndpoint("/{token}")
@Component
public class WebSocketServer {
    private Session session;
    public static ConcurrentHashMap<String,WebSocketServer> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam( "token") String token){
        System.out.println("OnOpen()客户端连接===>"+token);
        this.session = session;
        clients.put(token,this);
    }

    @OnClose
    public void onClose(@PathParam( "token") String token){
        clients.remove(token);
    }

    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }
}