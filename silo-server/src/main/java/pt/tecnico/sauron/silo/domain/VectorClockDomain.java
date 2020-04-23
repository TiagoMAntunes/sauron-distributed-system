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

    public void incUpdate(int index) {
        int currentValue = this.updates.get(index);
        this.updates.set(index, currentValue+1);
    }

    public int getUpdate(int index) {
        return this.updates.get(index);
    }

    //Compares to another VectorClockDomain and checks if more recent
    public boolean isMoreRecent(VectorClockDomain v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < v.getUpdate(i)) /* TODO change to <= */ /* TODO change this to return true when receives a vectorclock at zero  */
                return false;
        }
        return true;
    }

    //TODO Confirm this is the way to do it
    //TODO Ensure we are not only updating the vectorclock but actually making the changes
    public void merge(VectorClockDomain v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < v.getUpdate(i))
                this.updates.set(i, v.getUpdate(i));
        }
    }

    @Override
    public String toString() {
        String vectorStr = "[";
        for( int i = 0; i < updates.size(); i++) {
            vectorStr += getUpdate(i) + ",";
        }
        vectorStr = vectorStr.substring(0,vectorStr.length() - 1) + ']';
        return vectorStr;
    }

    public ArrayList<Integer> getList() {
        return this.updates;
    }
}