package com.minecolonies.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockHutFarmer extends BlockHut
{
    public final String name = "blockHutFarmer";

    protected BlockHutFarmer()
    {
        super();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        //TODO
        return null;
    }
}