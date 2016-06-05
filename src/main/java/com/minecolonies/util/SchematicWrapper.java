package com.minecolonies.util;

import com.minecolonies.blocks.AbstractBlockHut;
import com.minecolonies.blocks.ModBlocks;
import com.minecolonies.configuration.Configurations;
import com.schematica.client.util.RotationHelper;
import com.schematica.world.schematic.SchematicFormat;
import com.schematica.world.storage.Schematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Interface for using the Schematica codebase
 *
 * @author Colton
 */
public final class SchematicWrapper
{
    /**
     * The minecraft world this schematic is displayed in.
     */
    private World world;

    /**
     * The schematic world this schematic comes from.
     */
    private Schematic schematicWorld;

    /**
     * The anchor position this schematic will be
     * placed on in the minecraft world.
     */
    private BlockPos position;

    /**
     * The name this schematic has.
     */
    private String name;

    /**
     * The position we use as our uninitialized value.
     */
    private static final BlockPos NULL_POS = new BlockPos(-1, -1, -1);

    /**
     * The SchematicWorld position we are at. Defaulted to NULL_POS.
     */
    private final BlockPos.MutableBlockPos progressPos = new BlockPos.MutableBlockPos(-1, -1, -1);

    private static final int NUMBER_OF_ROTATIONS = 4;

    private static final int REVERSE_ROTATION = 3;

    /**
     * Load a schematic into this world.
     *
     * @param worldObj the world to load in
     * @param name     the schematics name
     */
    public SchematicWrapper(World worldObj, String name)
    {
        this(worldObj, getResourceLocation(name), name);
    }

    /**
     * Load a schematic into this world.
     *
     * @param worldObj the world to load in
     * @param res      the resource location of this schematic
     * @param name     the schematics name
     */
    private SchematicWrapper(World worldObj, ResourceLocation res, String name)
    {
        this(worldObj, SchematicFormat.readFromStream(getStream(res)), name);
    }

    /**
     * Create a new Schematic.
     *
     * @param worldObj       the world to show it in
     * @param schematicWorld the SchematicWorld it comes from
     * @param name           the name this schematic has
     */
    private SchematicWrapper(World worldObj, Schematic schematicWorld, String name)
    {
        world = worldObj;
        this.schematicWorld = schematicWorld;
        this.name = name;
    }

    /**
     * Generate the stream from a resource location.
     *
     * @param res the location to pull the stream from
     * @return a stream from this location
     */
    private static InputStream getStream(ResourceLocation res)
    {
        try
        {
            if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            {
                return Minecraft.getMinecraft().getResourceManager().getResource(res).getInputStream();
            }
            else
            {
                //todo: add more checks here
                return SchematicWrapper.class.getResourceAsStream(String.format("/assets/%s/%s", res.getResourceDomain(), res.getResourcePath()));
            }
        }
        catch(IOException e)
        {
            throw new IllegalStateException("Could not load stream!", e);
        }
    }

    /**
     * Generate a resource location from a schematics name.
     *
     * @param name the schematics name
     * @return the resource location pointing towards the schematic
     */
    private static ResourceLocation getResourceLocation(@NotNull String name)
    {
        return new ResourceLocation("minecolonies:schematics/" + name + ".schematic");
    }

    /**
     * Load a schematic into this world
     * and place it in the right position and rotation.
     *
     * @param worldObj  the world to load it in
     * @param name      the schematics name
     * @param pos       coordinates
     * @param rotations number of times rotated
     */
    public static void loadAndPlaceSchematicWithRotation(World worldObj, String name, BlockPos pos, int rotations)
    {
        try
        {
            SchematicWrapper schematic = new SchematicWrapper(worldObj, name);
            schematic.rotate(rotations);
            schematic.placeSchematic(pos);
        }
        catch(IllegalStateException e)
        {
            Log.logger.warn("Could not load schematic!", e);
        }
    }

    /**
     * Place a schematic into the world.
     *
     * @param pos coordinates
     */
    private void placeSchematic(BlockPos pos)
    {
        setLocalPosition(pos);

        List<BlockPos> delayedBlocks = new ArrayList<>();

        for(int j = 0; j < schematicWorld.getHeight(); j++)
        {
            for(int k = 0; k < schematicWorld.getLength(); k++)
            {
                for(int i = 0; i < schematicWorld.getWidth(); i++)
                {
                    BlockPos localPos = new BlockPos(i, j, k);
                    IBlockState localState = this.schematicWorld.getBlockState(localPos);
                    Block localBlock = localState.getBlock();

                    BlockPos worldPos = pos.add(localPos);
                    IBlockState worldState = world.getBlockState(worldPos);

                    if(localBlock == ModBlocks.blockSubstitution)
                    {
                        continue;
                    }
                    else if(localBlock == Blocks.air && !worldState.getBlock().getMaterial().isSolid())
                    {
                        world.setBlockToAir(worldPos);
                    }
                    else if(localBlock.getMaterial().isSolid())
                    {
                        world.setBlockState(worldPos, localState, 0x03);
                        worldState = world.getBlockState(worldPos);

                        if(worldState.getBlock() == localBlock)
                        {
                            if(worldState != localState)
                            {
                                world.setBlockState(worldPos, localState, 0x03);
                            }
                            localBlock.onBlockAdded(world, worldPos, localState);
                        }
                    }
                    else
                    {
                        delayedBlocks.add(localPos);
                    }

                    TileEntity tileEntity = schematicWorld.getTileEntity(localPos);
                    if(tileEntity != null)
                    {
                        world.setTileEntity(worldPos, tileEntity);
                    }
                }
            }
        }

        for(BlockPos coords : delayedBlocks)
        {
            IBlockState localState = this.schematicWorld.getBlockState(coords);
            Block localBlock = localState.getBlock();
            BlockPos newWorldPos = pos.add(coords);

            world.setBlockState(newWorldPos, localState, 0x03);
            if(world.getBlockState(newWorldPos).getBlock() == localBlock)
            {
                if(world.getBlockState(newWorldPos) != localState)
                {
                    world.setBlockState(newWorldPos, localState, 0x03);
                }
                localBlock.onBlockAdded(world, newWorldPos, localState);
            }
        }
    }

    private void rotate(EnumFacing facing)
    {
        try
        {
            schematicWorld = RotationHelper.rotate(schematicWorld, facing, true);
        }
        catch (RotationHelper.RotationException e)
        {
            Log.logger.debug(e);
        }
    }

    /**
     * Rotate this schematic.
     *
     * @param times how many times to rotate the schematic.
     */
    public void rotate(int times)
    {
        if(times % NUMBER_OF_ROTATIONS == REVERSE_ROTATION)
        {
            //reverse rotate
            rotate(EnumFacing.DOWN);
        }
        else
        {
            for (int i = 0; i < times; i++)
            {
                //normal rotate
                rotate(EnumFacing.UP);
            }
        }
    }

    public static String saveSchematic(World world, BlockPos from, BlockPos to)
    {
        if(world == null || from == null || to == null)
        {
            throw new IllegalArgumentException("Invalid method call, arguments can't be null. Contact a developer.");
        }

        Schematic schematic = scanSchematic(world, from, to);

        String fileName = LanguageHandler.format("item.scepterSteel.scanFormat", schematic.getType(), System.currentTimeMillis());
        File file = new File(getScanDirectory(world), fileName);

        if(SchematicFormat.writeToFile(file, schematic))
        {
            return LanguageHandler.format("item.scepterSteel.scanSuccess", fileName);
        }
        return LanguageHandler.format("item.scepterSteel.scanFailure");
    }

    private static File getScanDirectory(World world)
    {
        File minecolonies;
        if(world.isRemote)
        {
            minecolonies = new File(Minecraft.getMinecraft().mcDataDir, "minecolonies/");
        }
        else
        {
            minecolonies = MinecraftServer.getServer().getFile("minecolonies/");
        }
        checkDirectory(minecolonies);

        File scans = new File(minecolonies, "scans/");
        checkDirectory(scans);
        return scans;
    }

    private static void checkDirectory(File directory)
    {
        if(!directory.exists() && !directory.mkdirs())
        {
            Log.logger.error("Directory doesn't exist and failed to be created: " + directory.toString());
        }
    }

    private static Schematic scanSchematic(World world, BlockPos from, BlockPos to)
    {
        //todo: split up this method
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());
        short width = (short) (Math.abs(maxX - minX) + 1);
        short height = (short) (Math.abs(maxY - minY) + 1);
        short length = (short) (Math.abs(maxZ - minZ) + 1);

        BlockPos minPos = new BlockPos(minX, minY, minZ);

        Schematic schematic = new Schematic(new ItemStack(Blocks.red_mushroom), width, height, length);

        BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos(0, 0, 0);

        for(int x = minX; x <= maxX; x++)
        {
            for(int y = minY; y <= maxY; y++)
            {
                for(int z = minZ; z <= maxZ; z++)
                {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockPos localPos = worldPos.subtract(minPos);

                    IBlockState blockState = world.getBlockState(worldPos);
                    schematic.setBlockState(localPos, blockState);

                    if (blockState.getBlock() instanceof AbstractBlockHut)
                    {
                        if (BlockPosUtil.equals(offset, 0, 0, 0))
                        {
                            BlockPosUtil.set(offset, localPos);
                        }
                        else
                        {
                            schematic.setBlockState(localPos, Blocks.air.getDefaultState());
                            Log.logger.warn("Scan contained multiple AbstractBlockHut's ignoring this one");
                        }
                    }

                    TileEntity tileEntity = world.getTileEntity(worldPos);
                    if (tileEntity != null)//creates a new tileEntity and formats its data for saving
                    {                     //a new tileEntity is needed to prevent changes to the real one
                        NBTTagCompound tileEntityNBT = new NBTTagCompound();
                        tileEntity.writeToNBT(tileEntityNBT);

                        tileEntity = TileEntity.createAndLoadEntity(tileEntityNBT);
                        tileEntity.setPos(tileEntity.getPos().subtract(minPos));
                        schematic.setTileEntity(tileEntity.getPos(),tileEntity);
                    }
                }
            }
        }
        if (BlockPosUtil.equals(offset, 0, 0, 0))
        {
            offset.set(width/2, 0, length/2);
        }

        schematic.setOffset(offset);


        AxisAlignedBB region = AxisAlignedBB.fromBounds(minX, minY, minZ, maxX, maxY, maxZ);
        List<EntityHanging> entityHangings = world.getEntitiesWithinAABB(EntityHanging.class, region);
        List<EntityMinecart> entityMinecarts = world.getEntitiesWithinAABB(EntityMinecart.class, region);

        entityHangings.stream().filter(entity -> entity != null).forEach(schematic::addEntity);
        entityMinecarts.stream().filter(minecart -> minecart != null).forEach(schematic::addEntity);

        return schematic;
    }

    /**
     * Find the next block that doesn't already exist in the world.
     *
     * @return true if a new block is found and false if there is no next block.
     */
    public boolean findNextBlock()
    {
        int count = 0;
        do
        {
            count++;
            if(!incrementBlock())
            {
                return false;
            }

        }
        while(doesSchematicBlockEqualWorldBlock() && count < Configurations.maxBlocksCheckedByBuilder);

        return true;
    }

    public boolean incrementBlock()
    {
        if(this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(-1, 0, 0);
        }

        this.progressPos.set(this.progressPos.getX() + 1, this.progressPos.getY(), this.progressPos.getZ());
        if(this.progressPos.getX() == schematicWorld.getWidth())
        {
            this.progressPos.set(0, this.progressPos.getY(), this.progressPos.getZ() + 1);
            if(this.progressPos.getZ() == schematicWorld.getLength())
            {
                this.progressPos.set(this.progressPos.getX(), this.progressPos.getY() + 1, 0);
                if(this.progressPos.getY() == schematicWorld.getHeight())
                {
                    reset();
                    return false;
                }
            }
        }

        return true;
    }

    public boolean doesSchematicBlockEqualWorldBlock()
    {
        BlockPos worldPos = this.getBlockPosition();
        IBlockState metadata = schematicWorld.getBlockState(this.getLocalPosition());

        //All worldBlocks are equal the substitution block
        if(metadata.getBlock() == ModBlocks.blockSubstitution)
        {
            return true;
        }

        //For the time being any flower pot is equal to each other.
        if(metadata.getBlock() instanceof BlockFlowerPot && world.getBlockState(worldPos).getBlock() instanceof BlockFlowerPot)
        {
            return true;
        }

        //Stairs facing the same direction are the same stairs they just didn't adapt to close ones.
        if(metadata.getBlock() instanceof BlockStairs
                && world.getBlockState(worldPos).getBlock() instanceof BlockStairs
                && world.getBlockState(worldPos).getValue(BlockStairs.FACING) == metadata.getValue(BlockStairs.FACING)
                && metadata == world.getBlockState(worldPos).getBlock())
        {
            return true;
        }

        if(metadata.getBlock() instanceof BlockDoor)
        {
            return Objects.equals(metadata.getBlock(),
                    BlockPosUtil.getBlock(world, worldPos));
        }
        //had this problem in a superflat world, causes builder to sit doing nothing because placement failed
        return worldPos.getY() <= 0
                || Objects.equals(metadata.getBlock(),
                BlockPosUtil.getBlock(world, worldPos))
                && Objects.equals(metadata,
                BlockPosUtil.getBlockState(world, worldPos));
    }

    public BlockPos getBlockPosition()
    {
        return this.progressPos.add(getOffsetPosition());
    }

    public BlockPos getOffsetPosition()
    {
        return position.subtract(getOffset());
    }

    public BlockPos getOffset()
    {
        return schematicWorld.getOffset();
    }

    public boolean findNextBlockToClear()
    {
        int count = 0;
        do
        {
            count++;
            //decrement because we clear from top to bottom.
            if(!decrementBlock())
            {
                return false;
            }

        }
        //Check for air blocks and if blocks below the hut are different from the schematicWorld
        while((worldBlockAir() || (progressPos.getY() <= getOffset().getY() && doesSchematicBlockEqualWorldBlock())) && count < Configurations.maxBlocksCheckedByBuilder);

        return true;
    }

    private boolean isAirBlock()
    {
        return schematicWorld.getBlockState(this.progressPos).getBlock() == Blocks.air;
    }

    public boolean findNextBlockSolid()
    {
        int count = 0;
        do
        {
            count++;
            if(!incrementBlock())
            {
                return false;
            }

        }
        while ((doesSchematicBlockEqualWorldBlock() || (getBlock() != null && !getBlock().getMaterial().isSolid() && !isAirBlock()))
               && count < Configurations.maxBlocksCheckedByBuilder);

        return true;
    }

    public boolean findNextBlockNonSolid()
    {
        int count = 0;
        do
        {
            count++;
            if(!incrementBlock())
            {
                return false;
            }

        }
        while ((doesSchematicBlockEqualWorldBlock() || (getBlock() != null && getBlock().getMaterial().isSolid() || isAirBlock()))
               && count < Configurations.maxBlocksCheckedByBuilder);

        return true;
    }

    public boolean decrementBlock()
    {
        if(this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(schematicWorld.getWidth(), schematicWorld.getHeight() - 1, schematicWorld.getLength() - 1);
        }

        this.progressPos.set(this.progressPos.getX() - 1, this.progressPos.getY(), this.progressPos.getZ());
        if(this.progressPos.getX() == -1)
        {
            this.progressPos.set(schematicWorld.getWidth() - 1, this.progressPos.getY(), this.progressPos.getZ() - 1);
            if(this.progressPos.getZ() == -1)
            {
                this.progressPos.set(this.progressPos.getX(), this.progressPos.getY() - 1, schematicWorld.getLength() - 1);
                if(this.progressPos.getY() == -1)
                {
                    reset();
                    return false;
                }
            }
        }

        return true;
    }

    private boolean worldBlockAir()
    {
        BlockPos pos = this.getBlockPosition();
        //had this problem in a superflat world, causes builder to sit doing nothing because placement failed
        return pos.getY() <= 0 || world.isAirBlock(pos);
    }

    @Nullable
    public IBlockState getBlockState()
    {
        if(this.progressPos.equals(NULL_POS))
        {
            return null;
        }
        return this.schematicWorld.getBlockState(this.progressPos);
    }

    @Nullable
    public TileEntity getTileEntity()
    {
        if(this.progressPos.equals(NULL_POS))
        {
            return null;
        }
        return this.schematicWorld.getTileEntity(this.progressPos);
    }

    public List<Entity> getEntities()
    {
        return schematicWorld.getEntities();
    }

    @NotNull
    public BlockPos getLocalPosition()
    {
        return this.progressPos.getImmutable();
    }

    public void setLocalPosition(BlockPos localPosition)
    {
        BlockPosUtil.set(this.progressPos, localPosition);
    }

    public BlockPos getPosition()
    {
        return position;
    }

    public void setPosition(BlockPos position)
    {
        this.position = position;
    }

    /**
     * Calculate the item needed to place the current block in the schematic
     *
     * @return an item or null if not initialized
     */
    @Nullable
    public Item getItem()
    {
        Block block = this.getBlock();
        IBlockState metadata = this.getMetadata();
        if(block == null || metadata == null)
        {
            return null;
        }
        ItemStack stack = new ItemStack(Item.getItemFromBlock(block), 1, block.damageDropped(metadata));

        //In some cases getItemFromBlock returns null. We then have to get the item the safer way.
        if(stack.getItem() == null)
        {
            stack = new ItemStack(block.getItem(this.world, this.getBlockPosition()));
        }
        return stack.getItem();
    }

    /**
     * Calculate the current block in the schematic
     *
     * @return the current block or null if not initialized
     */
    @Nullable
    public Block getBlock()
    {
        IBlockState state = getBlockState();
        if(state == null)
        {
            return null;
        }
        return state.getBlock();
    }

    /**
     * Calculate the current block's metadata in the schematic
     *
     * @return the current block's metadata or null if not initialized
     */
    public IBlockState getMetadata()
    {
        return getBlockState();
    }

    public String getName()
    {
        return name;
    }

    public int getHeight()
    {
        return schematicWorld.getHeight();
    }

    public int getWidth()
    {
        return schematicWorld.getWidth();
    }

    public int getLength()
    {
        return schematicWorld.getLength();
    }

    public void reset()
    {
        BlockPosUtil.set(this.progressPos, NULL_POS);
    }

    public Schematic getSchematic()
    {
        return schematicWorld;
    }
}
