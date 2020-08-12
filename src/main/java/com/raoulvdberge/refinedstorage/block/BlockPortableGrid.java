package com.raoulvdberge.refinedstorage.block;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.block.enums.PortableGridDiskState;
import com.raoulvdberge.refinedstorage.block.enums.PortableGridType;
import com.raoulvdberge.refinedstorage.block.info.BlockDirection;
import com.raoulvdberge.refinedstorage.block.info.BlockInfoBuilder;
import com.raoulvdberge.refinedstorage.item.itemblock.ItemBlockPortableGrid;
import com.raoulvdberge.refinedstorage.render.IModelRegistration;
import com.raoulvdberge.refinedstorage.render.collision.CollisionGroup;
import com.raoulvdberge.refinedstorage.render.constants.ConstantsPortableGrid;
import com.raoulvdberge.refinedstorage.render.meshdefinition.ItemMeshDefinitionPortableGrid;
import com.raoulvdberge.refinedstorage.render.model.baked.BakedModelFullbright;
import com.raoulvdberge.refinedstorage.tile.grid.portable.TilePortableGrid;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class BlockPortableGrid extends BlockBase {
    public static final PropertyEnum TYPE = PropertyEnum.create("type", PortableGridType.class);
    public static final PropertyEnum DISK_STATE = PropertyEnum.create("disk_state", PortableGridDiskState.class);
    public static final PropertyBool CONNECTED = PropertyBool.create("connected");

    public BlockPortableGrid() {
        super(BlockInfoBuilder.forId("portable_grid").tileEntity(TilePortableGrid::new).create());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels(IModelRegistration modelRegistration) {
        modelRegistration.setStateMapper(this, new StateMap.Builder().ignore(TYPE).build());
        modelRegistration.setModelMeshDefinition(this, new ItemMeshDefinitionPortableGrid());

        modelRegistration.addBakedModelOverride(info.getId(), base -> new BakedModelFullbright(
            base,
            RS.ID + ":blocks/disks/leds"
        ));
    }

    @Override
    @Nullable
    public BlockDirection getDirection() {
        return BlockDirection.HORIZONTAL;
    }

    @Override
    public Item createItem() {
        return new ItemBlockPortableGrid(this);
    }

    @Override
    public List<CollisionGroup> getCollisions(TileEntity tile, IBlockState state) {
        return Collections.singletonList(ConstantsPortableGrid.COLLISION);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        if (!world.isRemote) {
            ((TilePortableGrid) world.getTileEntity(pos)).onPassItemContext(stack);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(((TilePortableGrid) world.getTileEntity(pos)).getAsItem());
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TilePortableGrid portableGrid = (TilePortableGrid) world.getTileEntity(pos);

        return super.getActualState(state, world, pos)
            .withProperty(DISK_STATE, portableGrid.getDiskState())
            .withProperty(CONNECTED, portableGrid.isConnected());
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockStateBuilder()
            .add(TYPE)
            .add(DISK_STATE)
            .add(CONNECTED)
            .build();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, meta == 0 ? PortableGridType.NORMAL : PortableGridType.CREATIVE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE) == PortableGridType.NORMAL ? 0 : 1;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            API.instance().getGridManager().openGrid(TilePortableGrid.FACTORY_ID, (EntityPlayerMP) player, pos);

            ((TilePortableGrid) world.getTileEntity(pos)).onOpened();
        }

        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
