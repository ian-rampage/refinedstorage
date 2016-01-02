package storagecraft.render.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import storagecraft.tile.TileCable;

public class CableModel extends ModelBase
{
	public static final ResourceLocation CABLE_RESOURCE = new ResourceLocation("storagecraft:textures/blocks/cable.png");

	private ModelRenderer core;
	private ModelRenderer up;
	private ModelRenderer down;
	private ModelRenderer north;
	private ModelRenderer east;
	private ModelRenderer south;
	private ModelRenderer west;

	public CableModel()
	{
		core = new ModelRenderer(this, 0, 0);
		core.addBox(6F, 6F, 6F, 4, 4, 4);
		core.setTextureSize(16, 16);

		up = new ModelRenderer(this, 0, 0);
		up.addBox(6F, 10F, 6F, 4, 6, 4);
		up.setTextureSize(16, 16);

		down = new ModelRenderer(this, 0, 0);
		down.addBox(6F, 0F, 6F, 4, 6, 4);
		down.setTextureSize(16, 16);

		north = new ModelRenderer(this, 0, 0);
		north.addBox(6F, 6F, 0F, 4, 4, 6);
		north.setTextureSize(16, 16);

		east = new ModelRenderer(this, 0, 0);
		east.addBox(10F, 6F, 6F, 6, 4, 4);
		east.setTextureSize(16, 16);

		south = new ModelRenderer(this, 0, 0);
		south.addBox(6F, 6F, 10F, 4, 4, 6);
		south.setTextureSize(16, 16);

		west = new ModelRenderer(this, 0, 0);
		west.addBox(0F, 6F, 6F, 6, 4, 4);
		west.setTextureSize(16, 16);
	}

	public void render(TileEntity tile)
	{
		Minecraft.getMinecraft().renderEngine.bindTexture(CABLE_RESOURCE);

		core.render(0.0625F);

		if (tile != null)
		{
			TileCable cable = (TileCable) tile;

			if (cable.hasConnection(EnumFacing.UP))
			{
				up.render(0.0625F);
			}

			if (cable.hasConnection(EnumFacing.DOWN))
			{
				down.render(0.0625F);
			}

			if (cable.hasConnection(EnumFacing.NORTH))
			{
				north.render(0.0625F);
			}

			if (cable.hasConnection(EnumFacing.EAST))
			{
				east.render(0.0625F);
			}

			if (cable.hasConnection(EnumFacing.SOUTH))
			{
				south.render(0.0625F);
			}

			if (cable.hasConnection(EnumFacing.WEST))
			{
				west.render(0.0625F);
			}
		}
	}
}
