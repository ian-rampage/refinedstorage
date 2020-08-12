package com.raoulvdberge.refinedstorage.apiimpl.network.node;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternProvider;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerUpgrade;
import com.raoulvdberge.refinedstorage.inventory.listener.ListenerNetworkNode;
import com.raoulvdberge.refinedstorage.item.ItemUpgrade;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import com.raoulvdberge.refinedstorage.util.WorldUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NetworkNodeCrafter extends NetworkNode implements ICraftingPatternContainer {
    public enum CrafterMode {
        IGNORE,
        SIGNAL_UNLOCKS_AUTOCRAFTING,
        SIGNAL_LOCKS_AUTOCRAFTING,
        PULSE_INSERTS_NEXT_SET;

        public static CrafterMode getById(int id) {
            if (id >= 0 && id < values().length) {
                return values()[id];
            }

            return IGNORE;
        }
    }

    public static final String ID = "crafter";

    public static final String DEFAULT_NAME = "gui.refinedstorage:crafter";

    private static final String NBT_DISPLAY_NAME = "DisplayName";
    private static final String NBT_UUID = "CrafterUuid";
    private static final String NBT_MODE = "Mode";
    private static final String NBT_LOCKED = "Locked";
    private static final String NBT_WAS_POWERED = "WasPowered";

    private ItemHandlerBase patternsInventory = new ItemHandlerBase(9, new ListenerNetworkNode(this), s -> isValidPatternInSlot(world, s)) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            if (!world.isRemote) {
                invalidate();
            }

            if (network != null) {
                network.getCraftingManager().rebuild();
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public static boolean isValidPatternInSlot(World world, ItemStack stack) {
        return stack.getItem() instanceof ICraftingPatternProvider && ((ICraftingPatternProvider) stack.getItem()).create(world, stack, null).isValid();
    }

    private List<ICraftingPattern> patterns = new ArrayList<>();

    private ItemHandlerUpgrade upgrades = new ItemHandlerUpgrade(4, new ListenerNetworkNode(this), ItemUpgrade.TYPE_SPEED);

    // Used to prevent infinite recursion on getRootContainer() when there's e.g. two crafters facing each other.
    private boolean visited = false;

    private CrafterMode mode = CrafterMode.IGNORE;
    private boolean locked = false;
    private boolean wasPowered;

    @Nullable
    private String displayName;

    @Nullable
    private UUID uuid = null;

    public NetworkNodeCrafter(World world, BlockPos pos) {
        super(world, pos);
    }

    private void invalidate() {
        patterns.clear();

        for (int i = 0; i < patternsInventory.getSlots(); ++i) {
            ItemStack patternStack = patternsInventory.getStackInSlot(i);

            if (!patternStack.isEmpty()) {
                ICraftingPattern pattern = ((ICraftingPatternProvider) patternStack.getItem()).create(world, patternStack, this);

                if (pattern.isValid()) {
                    patterns.add(pattern);
                }
            }
        }
    }

    @Override
    public int getEnergyUsage() {
        return RS.INSTANCE.config.crafterUsage + upgrades.getEnergyUsage() + (RS.INSTANCE.config.crafterPerPatternUsage * patterns.size());
    }

    @Override
    public void updateNetworkNode() {
        super.updateNetworkNode();

        if (ticks == 1) {
            invalidate();
        }

        if (mode == CrafterMode.PULSE_INSERTS_NEXT_SET) {
            if (world.isBlockPowered(pos)) {
                this.wasPowered = true;

                markNetworkNodeDirty();
            } else if (wasPowered) {
                this.wasPowered = false;
                this.locked = false;

                markNetworkNodeDirty();
            }
        }
    }

    @Override
    protected void onConnectedStateChange(INetwork network, boolean state) {
        super.onConnectedStateChange(network, state);

        network.getCraftingManager().rebuild();
    }

    @Override
    public void onDisconnected(INetwork network) {
        super.onDisconnected(network);

        network.getCraftingManager().getTasks().stream()
            .filter(task -> task.getPattern().getContainer().getPosition().equals(pos))
            .forEach(task -> network.getCraftingManager().cancel(task.getId()));
    }

    @Override
    protected void onDirectionChanged() {
        if (network != null) {
            network.getCraftingManager().rebuild();
        }
    }

    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);

        StackUtils.readItems(patternsInventory, 0, tag);

        if (API.instance().getOneSixMigrationHelper().migratePatternInventory(patternsInventory)) {
            markNetworkNodeDirty();
        }

        StackUtils.readItems(upgrades, 1, tag);

        if (tag.hasKey(NBT_DISPLAY_NAME)) {
            displayName = tag.getString(NBT_DISPLAY_NAME);
        }

        if (tag.hasUniqueId(NBT_UUID)) {
            uuid = tag.getUniqueId(NBT_UUID);
        }

        if (tag.hasKey(NBT_MODE)) {
            mode = CrafterMode.getById(tag.getInteger(NBT_MODE));
        }

        if (tag.hasKey(NBT_LOCKED)) {
            locked = tag.getBoolean(NBT_LOCKED);
        }

        if (tag.hasKey(NBT_WAS_POWERED)) {
            wasPowered = tag.getBoolean(NBT_WAS_POWERED);
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        super.write(tag);

        StackUtils.writeItems(patternsInventory, 0, tag);
        StackUtils.writeItems(upgrades, 1, tag);

        if (displayName != null) {
            tag.setString(NBT_DISPLAY_NAME, displayName);
        }

        if (uuid != null) {
            tag.setUniqueId(NBT_UUID, uuid);
        }

        tag.setInteger(NBT_MODE, mode.ordinal());
        tag.setBoolean(NBT_LOCKED, locked);
        tag.setBoolean(NBT_WAS_POWERED, wasPowered);

        return tag;
    }

    @Override
    public int getSpeedUpgradeCount() {
        return upgrades.getUpgradeCount(ItemUpgrade.TYPE_SPEED);
    }

    @Override
    @Nullable
    public IItemHandler getConnectedInventory() {
        ICraftingPatternContainer proxy = getRootContainer();
        if (proxy == null) {
            return null;
        }

        return WorldUtils.getItemHandler(proxy.getFacingTile(), proxy.getDirection().getOpposite());
    }

    @Nullable
    @Override
    public IFluidHandler getConnectedFluidInventory() {
        ICraftingPatternContainer proxy = getRootContainer();
        if (proxy == null) {
            return null;
        }

        return WorldUtils.getFluidHandler(proxy.getFacingTile(), proxy.getDirection().getOpposite());
    }

    @Override
    @Nullable
    public TileEntity getConnectedTile() {
        ICraftingPatternContainer proxy = getRootContainer();
        if (proxy == null) {
            return null;
        }

        return proxy.getFacingTile();
    }

    @Override
    public List<ICraftingPattern> getPatterns() {
        return patterns;
    }

    @Override
    @Nullable
    public IItemHandlerModifiable getPatternInventory() {
        return patternsInventory;
    }

    @Override
    public String getName() {
        if (displayName != null) {
            return displayName;
        }

        TileEntity facing = getConnectedTile();

        if (facing instanceof IWorldNameable && ((IWorldNameable) facing).getName() != null) {
            return ((IWorldNameable) facing).getName();
        }

        if (facing != null) {
            return world.getBlockState(facing.getPos()).getBlock().getTranslationKey() + ".name";
        }

        return DEFAULT_NAME;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public BlockPos getPosition() {
        return pos;
    }

    public CrafterMode getMode() {
        return mode;
    }

    public void setMode(CrafterMode mode) {
        this.mode = mode;
        this.wasPowered = false;
        this.locked = false;

        this.markNetworkNodeDirty();
    }

    public IItemHandler getPatternItems() {
        return patternsInventory;
    }

    public IItemHandler getUpgrades() {
        return upgrades;
    }

    @Override
    public IItemHandler getDrops() {
        return new CombinedInvWrapper(patternsInventory, upgrades);
    }

    @Override
    public boolean hasConnectivityState() {
        return true;
    }

    @Override
    @Nullable
    public ICraftingPatternContainer getRootContainer() {
        if (visited) {
            return null;
        }

        INetworkNode facing = API.instance().getNetworkNodeManager(world).getNode(pos.offset(getDirection()));
        if (!(facing instanceof ICraftingPatternContainer) || facing.getNetwork() != network) {
            return this;
        }

        visited = true;
        ICraftingPatternContainer facingContainer = ((ICraftingPatternContainer) facing).getRootContainer();
        visited = false;

        return facingContainer;
    }

    public Optional<ICraftingPatternContainer> getRootContainerNotSelf() {
        ICraftingPatternContainer root = getRootContainer();

        if (root != null && root != this) {
            return Optional.of(root);
        }

        return Optional.empty();
    }

    @Override
    public UUID getUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();

            markNetworkNodeDirty();
        }

        return uuid;
    }

    @Override
    public boolean isLocked() {
        Optional<ICraftingPatternContainer> root = getRootContainerNotSelf();
        if (root.isPresent()) {
            return root.get().isLocked();
        }

        switch (mode) {
            case IGNORE:
                return false;
            case SIGNAL_LOCKS_AUTOCRAFTING:
                return world.isBlockPowered(pos);
            case SIGNAL_UNLOCKS_AUTOCRAFTING:
                return !world.isBlockPowered(pos);
            case PULSE_INSERTS_NEXT_SET:
                return locked;
            default:
                return false;
        }
    }

    @Override
    public void onUsedForProcessing() {
        Optional<ICraftingPatternContainer> root = getRootContainerNotSelf();
        if (root.isPresent()) {
            root.get().onUsedForProcessing();

            return;
        }

        if (mode == CrafterMode.PULSE_INSERTS_NEXT_SET) {
            this.locked = true;

            markNetworkNodeDirty();
        }
    }
}
