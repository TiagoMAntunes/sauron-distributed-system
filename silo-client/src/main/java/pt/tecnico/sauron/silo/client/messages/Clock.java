package pt.tecnico.sauron.silo.client.messages;

import java.util.ArrayList;
import java.util.List;

class Clock {
    private ArrayList<Integer> updates;

    public Clock(List<Integer> updates) {
        this.updates = new ArrayList<>(updates);
    }

    public Clock(int n) {
        this.updates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            this.updates.add(0);
        }
    }

    public List<Integer> getList() {
        return this.updates;
    }

    public boolean isMoreRecent(List<Integer> arr) {
        if (arr.size() != this.updates.size()) return false; // Shouldn't ever happen though!

        for (int i = 0; i < arr.size(); i++) {
            if (this.updates.get(i) < arr.get(i)) return false; // This arr is not more recent than arr
        }

        return true;
    }


    /**
     * In-place update to maintain the reference to clock object
     * @param updates
     */
    public void update(List<Integer> updates) {
        for (int i = 0; i < updates.size(); i++)
            this.updates.set(i, Math.max(this.updates.get(i), updates.get(i))); // new timestamp is the maximum values for each position of the two
    }
}