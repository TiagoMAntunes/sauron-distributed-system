package pt.tecnico.sauron.silo.client.messages;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;

public class Cache {
    //TODO do I need to use two
    private Map<Request, Message> cache = new HashMap<>();
    private Map<Integer, Request> orderCache = new HashMap<>();
    private int currentSize;
    private int maxSize;

    private Message testMessage;
    private boolean test;
    public Cache(int max) {
        this.maxSize = max;
        this.currentSize = 0;
    }

    public void insertReqRes(Request req, Message res) {
       
        if (!inCache(req)) {
            System.out.println("Not cache");
            cache.put(req, res);
            this.currentSize++;
            System.out.print(this);
        } else {
            System.out.println("Already in cache");
        }
        
    }

    public boolean inCache(Request req) {
        return this.cache.containsKey(req);
    }

    @Override
    public String toString() {
        String res = String.format("---- CACHE ----\nSize: %d;\nCache: %s;\n ---- ENDCACHE ----",this.currentSize,this.cache);
        return res;
    }

}    