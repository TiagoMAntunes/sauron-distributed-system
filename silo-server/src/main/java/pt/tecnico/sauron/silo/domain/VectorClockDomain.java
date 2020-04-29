package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VectorClockDomain {
    private ArrayList<Integer> updates = new ArrayList<>();

    //Initialize ArrayList from List (can create VectorClockDomain directly from what grpc message returns)
    public VectorClockDomain(List<Integer> list) {
        this.updates = new ArrayList<>(list);
    }

    public VectorClockDomain(int nReplicas) {
        this.updates = new ArrayList<>(Collections.nCopies(nReplicas, 0));
    }

    public synchronized void incUpdate(int index) {
        int currentValue = this.updates.get(index);
        this.updates.set(index, currentValue+1);
    }

    public synchronized int getUpdate(int index) {
        return this.updates.get(index);
    }

    public synchronized void setUpdate(int index, int val) {
        this.updates.set(index,val);
    }
 
    //Compares to another VectorClockDomain and checks if more recent
    public synchronized boolean isMoreRecent(VectorClockDomain v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < v.getUpdate(i))  
                return false;
        }
        return true;
    }

    //Gets valid prev from request by removing the origin
    public synchronized boolean isMoreRecent(VectorClockDomain v, int origin) {
        ArrayList<Integer> n = new ArrayList<>(v.getList());
        n.set(origin, n.get(origin) - 1);
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < n.get(i))  
                return false;
        }
        return true;
    }

    @Override
    public synchronized String toString() {
        String vectorStr = "[";
        for( int i = 0; i < updates.size(); i++) {
            vectorStr += getUpdate(i) + ",";
        }
        vectorStr = vectorStr.substring(0,vectorStr.length() - 1) + ']';
        return vectorStr;
    }

    public synchronized ArrayList<Integer> getList() {
        return this.updates;
    }

    public synchronized void clear() {
        this.updates = new ArrayList<>(Collections.nCopies(updates.size(), 0));
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (! (o instanceof VectorClockDomain)) return false;
        VectorClockDomain d = (VectorClockDomain) o;

        for(int i=0; i< this.updates.size(); i++) {
            if (this.updates.get(i) != d.getList().get(i)) {
                return false;
            }
        }
        return true;
    }

    public synchronized ArrayList<ArrayList<Integer>> moreRecentIndexes(VectorClockDomain replica) {
        ArrayList<ArrayList<Integer>> list = new ArrayList<>();; 
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < replica.getList().size(); i++) {
            if(replica.getList().get(i) < this.updates.get(i)) {
                indexes.add(i);
                values.add(replica.getList().get(i));
            }
        }
        list.add(indexes);
        list.add(values);
        return list;
    }

	public synchronized VectorClockDomain getCopy() {
        ArrayList<Integer> arr = new ArrayList<>();
        for (Integer i : this.updates)
            arr.add(Integer.valueOf(i));
        return new VectorClockDomain(arr);
	}
}