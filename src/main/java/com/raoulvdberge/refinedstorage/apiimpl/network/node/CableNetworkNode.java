package com.raoulvdberge.refinedstorage.apiimpl.network.node;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.cover.CoverManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class CableNetworkNode extends NetworkNode implements ICoverable {
    public static final ResourceLocation ID = new ResourceLocation(RS.ID, "cable");

    private static final String NBT_COVERS = "Covers";

    private CoverManager coverManager = new CoverManager(this);

    public CableNetworkNode(World world, BlockPos pos) {
        super(world, pos);
    }

    @Override
    public int getEnergyUsage() {
        return RS.SERVER_CONFIG.getCable().getUsage();
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public CoverManager getCoverManager() {
        return coverManager;
    }

    @Override
    public boolean canConduct(@Nullable Direction direction) {
        return coverManager.canConduct(direction);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);

        tag.put(NBT_COVERS, coverManager.writeToNbt());

        return tag;
    }

    @Override
    public void read(CompoundNBT tag) {
        super.read(tag);

        if (tag.contains(NBT_COVERS)) {
            coverManager.readFromNbt(tag.getList(NBT_COVERS, Constants.NBT.TAG_COMPOUND));
        }
    }

    @Nullable
    @Override
    public IItemHandler getDrops() {
        return coverManager.getAsInventory();
    }
}