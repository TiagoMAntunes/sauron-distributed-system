package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Vector clock.
 */
public class VectorClock {
    private ArrayList<Integer> updates = new ArrayList<>();

    /**
     * Instantiates a new Vector clock.
     *
     * @param list the list
     */
//Initialize ArrayList from List (can create VectorClock directly from what grpc message returns)
    public VectorClock(List<Integer> list) {
        this.updates = new ArrayList<>(list);
    }

    /**
     * Inc update.
     *
     * @param index the index
     */
    public void incUpdate(int index) {
        int currentValue = this.updates.get(index);
        this.updates.set(index, currentValue+1);
    }

    /**
     * Gets update.
     *
     * @param index the index
     * @return the update
     */
    public int getUpdate(int index) {
        return this.updates.get(index);
    }

    /**
     * Is more recent boolean.
     *
     * @param v the v
     * @return the boolean
     */
//Compares to another VectorClock and checks if more recent
    public boolean isMoreRecent(VectorClock v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) <= v.getUpdate(i))
                return false;
        }
        return true;
    }

    /**
     * Merge.
     *
     * @param v the v
     */
//TODO Confirm this is the way to do it
    //TODO Ensure we are not only updating the vectorclock but actually making the changes
    public void merge(VectorClock v) {
        for( int i = 0; i < updates.size(); i++) {
            if (this.updates.get(i) < v.getUpdate(i))
                this.updates.set(i, v.getUpdate(i));
        }
    }
}