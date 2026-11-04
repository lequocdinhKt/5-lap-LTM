package lab5.service;

import lab5.model.SyncActivity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SyncActivityLog implements Consumer<SyncActivity> {
    private static final int MAX_ITEMS = 200;
    private final CopyOnWriteArrayList<SyncActivity> items = new CopyOnWriteArrayList<>();

    @Override
    public void accept(SyncActivity activity) {
        items.add(0, activity);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
    }

    public List<SyncActivity> snapshot() {
        return List.copyOf(items);
    }
}
