package com.minecolonies.network.messages;

import com.minecolonies.colony.Colony;
import com.minecolonies.colony.ColonyManager;
import com.minecolonies.colony.buildings.Building;
import com.minecolonies.util.ChunkCoordUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ChunkCoordinates;

/**
 * Adds a entry to the builderRequired map
 * Created: May 26, 2014
 *
 * @author Colton
 */
public class BuildRequestMessage implements IMessage, IMessageHandler<BuildRequestMessage, IMessage>
{
    private int              colonyId;
    private ChunkCoordinates buildingId;
    private int              mode;


    public static final int BUILD  = 0;
    public static final int REPAIR = 1;


    public BuildRequestMessage(){}

    public BuildRequestMessage(Building.View building, int mode)
    {
        this.colonyId = building.getColony().getID();
        this.buildingId = building.getID();
        this.mode = mode;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(colonyId);
        ChunkCoordUtils.writeToByteBuf(buf, buildingId);
        buf.writeInt(mode);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        colonyId = buf.readInt();
        buildingId = ChunkCoordUtils.readFromByteBuf(buf);
        mode = buf.readInt();
    }

    @Override
    public IMessage onMessage(BuildRequestMessage message, MessageContext ctx)
    {
        Colony colony = ColonyManager.getColony(message.colonyId);
        if (colony == null)
        {
            return null;
        }

        Building building = colony.getBuilding(message.buildingId);
        if (building == null)
        {
            return null;
        }

        switch(message.mode)
        {
            case BUILD:
                building.requestUpgrade();
                break;
            case REPAIR:
                building.requestRepair();
                break;
        }

        return null;
    }
}