package com.avgusrname.createbuildingwands.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WandGeometryUtil {
    
    public static List<BlockPos> lineBlockPositions(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();

        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));

        if (steps == 0) {
            positions.add(start);
            return positions;
        }

        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        double stepZ = (double) dz / steps;

        double currentX = start.getX();
        double currentY = start.getY();
        double currentZ = start.getZ();

        for (int i = 0; i <= steps; i++) {
            BlockPos currentPos = new BlockPos(
                (int) Math.round(currentX),
                (int) Math.round(currentY),
                (int) Math.round(currentZ)
            );

            if (positions.isEmpty() || !positions.get(positions.size() - 1).equals(currentPos)) {
                positions.add(currentPos);
            }

            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
        }
        
        if (!positions.contains(end)) {
            positions.add(end);
        }

        return positions;
    }

    public static List<BlockPos> planeBlockPositions(BlockPos start, BlockPos end, Direction face) {
        List<BlockPos> positions = new ArrayList<>();

        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        int fixedCoord;
        if (face.getAxis() == Direction.Axis.Y) {
            fixedCoord = end.getY();
        }
        else if (face.getAxis() == Direction.Axis.Z) {
            fixedCoord = end.getZ();
        }
        else {
            fixedCoord = end.getX();
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (face.getAxis() == Direction.Axis.Y && y == fixedCoord) {
                        positions.add(new BlockPos(x, y, z));
                    }
                    else if (face.getAxis() == Direction.Axis.X && x == fixedCoord) {
                        positions.add(new BlockPos(x, y, z));
                    }
                    else if (face.getAxis() == Direction.Axis.Z && z == fixedCoord) {
                        positions.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    public static List<BlockPos> cubeBlockPositions(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        // one of the axes must remain the same
        // could add logic for sloped planes but begin with straight ones first
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos currentPos = new BlockPos(
                        x,
                        y,
                        z
                    );
                    if (positions.isEmpty() || !positions.get(positions.size() - 1).equals(currentPos)) {
                        positions.add(currentPos);
                    }
                }
            }
        }
        return positions;
    }
}
