package pt.tecnico.sauron.silo.client.messages;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;

public class Cache {
    //TODO do I need to use two
    private Map<Request, Message> cache = new HashMap<>();
    private Map<Integer, Request> orderCache = new HashMap<>();
    private int currentSize;
    private int nextToReplace;
    private int maxSize;

    public Cache(int max) {
        this.maxSize = max;
        this.currentSize = 0;
        this.nextToReplace = 1;
    }

    public void insertReqRes(Request req, Message res) {
        //Checks if already in cache if so updates
        if (!inCache(req)) {
            //If there's still space adds a new entry to the cache
            if(this.currentSize < this.maxSize - 1) {
                cache.put(req, res);
                this.currentSize++;
                orderCache.put(this.currentSize,req); //Saves the order when it entered the ache
            } else {
                //If no space in cache deletes the oldest entry
                Request reqDel = this.orderCache.get(this.nextToReplace);
                this.cache.remove(reqDel);
                //Saves order of new entry in cache
                orderCache.put(this.nextToReplace,req);
                if(this.nextToReplace == this.maxSize) {
                    this.nextToReplace = 0;
                }
                this.nextToReplace++;
                //Saves new value
                cache.put(req, res);
            }
            
        } else {
            //Update value
            cache.replace(req, res);
        }
    }

    public boolean inCache(Request req) {
        return this.cache.containsKey(req);
    }

    @Override
    public String toString() {
        String res = String.format("---- CACHE ----\nSize: %d;\nCache: %s;\nOrder:%s\n ---- ENDCACHE ----",this.currentSize,this.cache,this.orderCache);
        return res;
    }

	public Message getValue(Request req, Message res) {
        if (inCache(req)) return cache.get(req);
        System.out.println("Not in cache");
        // If element not present, just consider it as the new valid element
        insertReqRes(req, res);
        return res;
	}

	public void reset() {
        this.cache = new HashMap<>();
        this.orderCache = new HashMap<>();
	}

}    