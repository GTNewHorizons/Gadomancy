package makeo.gadomancy.client.effect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CheckpointingSet<E> implements Iterable<E> {

    private final List<E> main = new ArrayList<>(64);
    private final List<E> toAdd = new ArrayList<>();
    private final List<E> toRemove = new ArrayList<>();

    public void add(E elem) {
        synchronized (toAdd) {
            toAdd.add(elem);
        }
    }

    public void remove(E elem) {
        synchronized (toRemove) {
            toRemove.add(elem);
        }
    }

    public void clear() {
        synchronized (toAdd) {
            synchronized (toRemove) {
                toAdd.clear();
                toRemove.clear();
                main.clear();
            }
        }
    }

    public void update() {
        synchronized (toAdd) {
            if (!toAdd.isEmpty()) {
                for (int i = 0; i < toAdd.size(); i++) {
                    E elem = toAdd.get(i);
                    if (!main.contains(elem)) {
                        main.add(elem);
                    }
                }
                toAdd.clear();
            }
        }
        synchronized (toRemove) {
            if (!toRemove.isEmpty()) {
                for (int i = 0; i < toRemove.size(); i++) {
                    main.remove(toRemove.get(i));
                }
                toRemove.clear();
            }
        }
    }

    public int size() {
        return main.size();
    }

    public E get(int index) {
        return main.get(index);
    }

    @Override
    public Iterator<E> iterator() {
        return main.iterator();
    }
}
