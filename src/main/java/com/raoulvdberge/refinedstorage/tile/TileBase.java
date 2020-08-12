package com.raoulvdberge.refinedstorage.tile;

import com.raoulvdberge.refinedstorage.tile.data.TileDataManager;
import com.raoulvdberge.refinedstorage.tile.direction.DirectionHandlerTile;
import com.raoulvdberge.refinedstorage.tile.direction.IDirectionHandler;
import com.raoulvdberge.refinedstorage.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public abstract class TileBase extends TileEntity {
    protected static final String NBT_DIRECTION = "Direction";

    private EnumFacing clientDirection = EnumFacing.NORTH;
    protected IDirectionHandler directionHandler = new DirectionHandlerTile();
    protected TileDataManager dataManager = new TileDataManager(this);

    public void setDirection(EnumFacing direction) {
        clientDirection = direction;

        directionHandler.setDirection(direction);

        world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock(), true);

        markNetworkNodeDirty();
    }

    public EnumFacing getDirection() {
        return world.isRemote ? clientDirection : directionHandler.getDirection();
    }

    public TileDataManager getDataManager() {
        return dataManager;
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        directionHandler.writeToTileNbt(tag);

        return tag;
    }

    public NBTTagCompound writeUpdate(NBTTagCompound tag) {
        tag.setInteger(NBT_DIRECTION, directionHandler.getDirection().ordinal());

        return tag;
    }

    public void read(NBTTagCompound tag) {
        directionHandler.readFromTileNbt(tag);
    }

    public void readUpdate(NBTTagCompound tag) {
        boolean doRender = canCauseRenderUpdate(tag);

        clientDirection = EnumFacing.byIndex(tag.getInteger(NBT_DIRECTION));

        if (doRender) {
            WorldUtils.updateBlock(world, pos);
        }
    }

    protected boolean canCauseRenderUpdate(NBTTagCompound tag) {
        return true;
    }

    @Override
    public final NBTTagCompound getUpdateTag() {
        return writeUpdate(super.getUpdateTag());
    }

    @Nullable
    @Override
    public final SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readUpdate(packet.getNbtCompound());
    }

    @Override
    public final void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);

        readUpdate(tag);
    }

    @Override
    public final void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        read(tag);
    }

    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound tag) {
        return write(super.writeToNBT(tag));
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Nullable
    public IItemHandler getDrops() {
        return null;
    }

    @Override
    public void markDirty() {
        // Delegating to a secondary method name due to but in mine craft obfuscation
        // TODO review against markDirty logic - very hard to tell if this correct, or if we should be overriding
        // markNetworkNodeDirty to point at markDirty()
        markNetworkNodeDirty();
    }

    // @Volatile: Copied with some changes from the super method (avoid sending neighbor updates, it's not needed)
    public void markNetworkNodeDirty() {
        if (world != null) {
            world.markChunkDirty(pos, this);
        }
    }
}
