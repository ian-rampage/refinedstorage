package com.raoulvdberge.refinedstorage.apiimpl.storage.cache;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.IStorage;
import com.raoulvdberge.refinedstorage.api.storage.IStorageProvider;
import com.raoulvdberge.refinedstorage.api.storage.cache.IStorageCache;
import com.raoulvdberge.refinedstorage.api.storage.cache.IStorageCacheListener;
import com.raoulvdberge.refinedstorage.api.util.IStackList;
import com.raoulvdberge.refinedstorage.api.util.StackListResult;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ItemStorageCache implements IStorageCache<ItemStack> {
    public static final Consumer<INetwork> INVALIDATE = network -> network.getItemStorageCache().invalidate();

    private final INetwork network;
    private final CopyOnWriteArrayList<IStorage<ItemStack>> storages = new CopyOnWriteArrayList<>();
    private final IStackList<ItemStack> list = API.instance().createItemStackList();
    private final IStackList<ItemStack> craftables = API.instance().createItemStackList();
    private final List<IStorageCacheListener<ItemStack>> listeners = new LinkedList<>();
    private final List<StackListResult<ItemStack>> batchedChanges = new ArrayList<>();

    public ItemStorageCache(INetwork network) {
        this.network = network;
    }

    @Override
    public void invalidate() {
        storages.clear();

        network.getNodeGraph().all().stream()
            .filter(node -> node.isActive() && node instanceof IStorageProvider)
            .forEach(node -> ((IStorageProvider) node).addItemStorages(storages));

        list.clear();

        sort();

        for (IStorage<ItemStack> storage : storages) {
            if (storage.getAccessType() == AccessType.INSERT) {
                continue;
            }

            for (ItemStack stack : storage.getStacks()) {
                if (!stack.isEmpty()) {
                    add(stack, stack.getCount(), true, false);
                }
            }
        }

        listeners.forEach(IStorageCacheListener::onInvalidated);
    }

    @Override
    public void add(@Nonnull ItemStack stack, int size, boolean rebuilding, boolean batched) {
        StackListResult<ItemStack> result = list.add(stack, size);

        if (!rebuilding) {
            if (!batched) {
                listeners.forEach(l -> l.onChanged(result));
            } else {
                batchedChanges.add(result);
            }
        }
    }

    @Override
    public void remove(@Nonnull ItemStack stack, int size, boolean batched) {
        StackListResult<ItemStack> result = list.remove(stack, size);

        if (result != null) {
            if (!batched) {
                listeners.forEach(l -> l.onChanged(result));
            } else {
                batchedChanges.add(result);
            }
        }
    }

    @Override
    public void flush() {
        if (!batchedChanges.isEmpty()) {
            if (batchedChanges.size() > 1) {
                listeners.forEach(l -> l.onChangedBulk(batchedChanges));
            } else {
                batchedChanges.forEach(change -> listeners.forEach(l -> l.onChanged(change)));
            }

            batchedChanges.clear();
        }
    }

    @Override
    public void addListener(IStorageCacheListener<ItemStack> listener) {
        listeners.add(listener);

        listener.onAttached();
    }

    @Override
    public void removeListener(IStorageCacheListener<ItemStack> listener) {
        listeners.remove(listener);
    }

    @Override
    public void reAttachListeners() {
        listeners.forEach(IStorageCacheListener::onAttached);
    }

    @Override
    public void sort() {
        storages.sort(IStorage.COMPARATOR);
    }

    @Override
    public IStackList<ItemStack> getList() {
        return list;
    }

    @Override
    public IStackList<ItemStack> getCraftablesList() {
        return craftables;
    }

    @Override
    public List<IStorage<ItemStack>> getStorages() {
        return storages;
    }
}
