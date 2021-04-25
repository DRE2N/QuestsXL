/**
 * PacketWrapper - ProtocolLib wrappers for Minecraft packets
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erethon.questsxl.tools.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;

public class WrapperPlayServerMultiBlockChange extends AbstractPacket {
	public static final PacketType TYPE =
			PacketType.Play.Server.MULTI_BLOCK_CHANGE;

	public WrapperPlayServerMultiBlockChange() {
		super(new PacketContainer(TYPE), TYPE);
		handle.getModifier().writeDefaults();
	}

	public WrapperPlayServerMultiBlockChange(PacketContainer packet) {
		super(packet, TYPE);
	}

	/**
	 * Retrieve the chunk that has been altered.
	 * 
	 * @return The current chunk
	 */
	public ChunkCoordIntPair getChunk() {
		return handle.getChunkCoordIntPairs().read(0);
	}

	/**
	 * Set the chunk that has been altered.
	 * 
	 * @param value - new value
	 */
	public void setChunk(Location value) {
		// Converts to chunk coordinates
		int x = value.getBlockX() >> 4;
		int y = value.getBlockY() >> 4;
		int z = value.getBlockZ() >> 4;
		BlockPosition pos = new BlockPosition(x, y, z);
		handle.getSectionPositions().write(0, pos);
	}

	/**
	 * Set the record data using the given helper array.
	 *
	 */
	public void setRecords(ArrayList<WrappedBlockData> blockDat, ArrayList<Short> blockPositions)  {
		WrappedBlockData[] blockData = blockDat.toArray(new WrappedBlockData[0]);
		Short[] blockLocsShort = blockPositions.toArray(new Short[0]);
		short[] blockLocations = ArrayUtils.toPrimitive(blockLocsShort);


		handle.getBlockDataArrays().writeSafely(0, blockData);
		handle.getShortArrays().writeSafely(0, blockLocations);
	}


	public short getShortLoc(Location loc) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		x = x & 0xF;
		y = y & 0xF;
		z = z & 0xF;
		return (short) (x << 8 | z << 4 | y << 0);
	}
}
