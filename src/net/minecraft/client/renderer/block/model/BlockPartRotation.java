package net.minecraft.client.renderer.block.model;

import net.minecraft.util.EnumFacing;
import client.runtime.lwjgl.LegacyVec3f;

public class BlockPartRotation
{
    public final LegacyVec3f origin;
    public final EnumFacing.Axis axis;
    public final float angle;
    public final boolean rescale;

    public BlockPartRotation(LegacyVec3f originIn, EnumFacing.Axis axisIn, float angleIn, boolean rescaleIn)
    {
        this.origin = originIn;
        this.axis = axisIn;
        this.angle = angleIn;
        this.rescale = rescaleIn;
    }
}
