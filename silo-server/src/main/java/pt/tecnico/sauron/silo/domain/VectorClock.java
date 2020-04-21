package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.List;

public class VectorClock {
    private ArrayList<Integer> updates = new ArrayList<>();

    //Initialize ArrayList from List (can create VectorClock directly from what grpc message returns)
    public VectorClock(List<Integer> list) {
        this.updates = new ArrayList<>(list);
    }

    public void incUpdate(int index) {
        int currentValue = this.updates.get(index);
        this.updates.set(index, currentValue+1);
    }

    public int getUpdate(int index) {
        return this.updates.get(index);
    }

    //Compares to another VectorClock and checks if more recent
    public boolean isMoreRecent(VectorClock v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) <= v.getUpdate(i))
                return false;
        }
        return true;
    }

    //TODO Confirm this is the way to do it
    //TODO Ensure we are not only updating the vectorclock but actually making the changes
    public void merge(VectorClock v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < v.getUpdate(i))
                this.updates.set(i, v.getUpdate(i));
        }
    }
}