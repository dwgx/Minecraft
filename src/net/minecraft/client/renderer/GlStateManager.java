package net.minecraft.client.renderer;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GlStateManager
{
    private static GlStateManager.AlphaState alphaState = new GlStateManager.AlphaState();
    private static GlStateManager.BooleanState lightingState = new GlStateManager.BooleanState(2896);
    private static GlStateManager.BooleanState[] lightState = new GlStateManager.BooleanState[8];
    private static GlStateManager.ColorMaterialState colorMaterialState = new GlStateManager.ColorMaterialState();
    private static GlStateManager.BlendState blendState = new GlStateManager.BlendState();
    private static GlStateManager.DepthState depthState = new GlStateManager.DepthState();
    private static GlStateManager.FogState fogState = new GlStateManager.FogState();
    private static GlStateManager.CullState cullState = new GlStateManager.CullState();
    private static GlStateManager.PolygonOffsetState polygonOffsetState = new GlStateManager.PolygonOffsetState();
    private static GlStateManager.ColorLogicState colorLogicState = new GlStateManager.ColorLogicState();
    private static GlStateManager.TexGenState texGenState = new GlStateManager.TexGenState();
    private static GlStateManager.ClearState clearState = new GlStateManager.ClearState();
    private static GlStateManager.StencilState stencilState = new GlStateManager.StencilState();
    private static GlStateManager.BooleanState normalizeState = new GlStateManager.BooleanState(2977);
    private static int activeTextureUnit = 0;
    private static GlStateManager.TextureState[] textureState = new GlStateManager.TextureState[8];
    private static int activeShadeModel = 7425;
    private static GlStateManager.BooleanState rescaleNormalState = new GlStateManager.BooleanState(32826);
    private static GlStateManager.ColorMask colorMaskState = new GlStateManager.ColorMask();
    private static GlStateManager.Color colorState = new GlStateManager.Color();

    // --- Modern GL state tracking (Phase 1 upgrade) ---
    private static int currentProgram = 0;
    private static int currentVao = 0;
    private static int currentArrayBuffer = 0;
    private static int currentElementBuffer = 0;

    // --- State snapshot for NanoVG frame isolation ---
    private static GlStateManager.StateSnapshot savedSnapshot;

    public static void pushAttrib()
    {
        GL11.glPushAttrib(8256);
    }

    public static void popAttrib()
    {
        GL11.glPopAttrib();
    }

    public static void disableAlpha()
    {
        alphaState.alphaTest.setDisabled();
    }

    public static void enableAlpha()
    {
        alphaState.alphaTest.setEnabled();
    }

    public static void alphaFunc(int func, float ref)
    {
        if (func != alphaState.func || ref != alphaState.ref)
        {
            alphaState.func = func;
            alphaState.ref = ref;
            GL11.glAlphaFunc(func, ref);
        }
    }

    public static void enableLighting()
    {
        lightingState.setEnabled();
    }

    public static void disableLighting()
    {
        lightingState.setDisabled();
    }

    public static void enableLight(int light)
    {
        lightState[light].setEnabled();
    }

    public static void disableLight(int light)
    {
        lightState[light].setDisabled();
    }

    public static void enableColorMaterial()
    {
        colorMaterialState.colorMaterial.setEnabled();
    }

    public static void disableColorMaterial()
    {
        colorMaterialState.colorMaterial.setDisabled();
    }

    public static void colorMaterial(int face, int mode)
    {
        if (face != colorMaterialState.face || mode != colorMaterialState.mode)
        {
            colorMaterialState.face = face;
            colorMaterialState.mode = mode;
            GL11.glColorMaterial(face, mode);
        }
    }

    public static void disableDepth()
    {
        depthState.depthTest.setDisabled();
    }

    public static void enableDepth()
    {
        depthState.depthTest.setEnabled();
    }

    public static void depthFunc(int depthFunc)
    {
        if (depthFunc != depthState.depthFunc)
        {
            depthState.depthFunc = depthFunc;
            GL11.glDepthFunc(depthFunc);
        }
    }

    public static void depthMask(boolean flagIn)
    {
        if (flagIn != depthState.maskEnabled)
        {
            depthState.maskEnabled = flagIn;
            GL11.glDepthMask(flagIn);
        }
    }

    public static void disableBlend()
    {
        blendState.blend.setDisabled();
    }

    public static void enableBlend()
    {
        blendState.blend.setEnabled();
    }

    public static void blendFunc(int srcFactor, int dstFactor)
    {
        if (srcFactor != blendState.srcFactor || dstFactor != blendState.dstFactor)
        {
            blendState.srcFactor = srcFactor;
            blendState.dstFactor = dstFactor;
            GL11.glBlendFunc(srcFactor, dstFactor);
        }
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha)
    {
        if (srcFactor != blendState.srcFactor || dstFactor != blendState.dstFactor || srcFactorAlpha != blendState.srcFactorAlpha || dstFactorAlpha != blendState.dstFactorAlpha)
        {
            blendState.srcFactor = srcFactor;
            blendState.dstFactor = dstFactor;
            blendState.srcFactorAlpha = srcFactorAlpha;
            blendState.dstFactorAlpha = dstFactorAlpha;
            OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        }
    }

    public static void enableFog()
    {
        fogState.fog.setEnabled();
    }

    public static void disableFog()
    {
        fogState.fog.setDisabled();
    }

    public static void setFog(int param)
    {
        if (param != fogState.mode)
        {
            fogState.mode = param;
            GL11.glFogi(GL11.GL_FOG_MODE, param);
        }
    }

    public static void setFogDensity(float param)
    {
        if (param != fogState.density)
        {
            fogState.density = param;
            GL11.glFogf(GL11.GL_FOG_DENSITY, param);
        }
    }

    public static void setFogStart(float param)
    {
        if (param != fogState.start)
        {
            fogState.start = param;
            GL11.glFogf(GL11.GL_FOG_START, param);
        }
    }

    public static void setFogEnd(float param)
    {
        if (param != fogState.end)
        {
            fogState.end = param;
            GL11.glFogf(GL11.GL_FOG_END, param);
        }
    }

    public static void enableCull()
    {
        cullState.cullFace.setEnabled();
    }

    public static void disableCull()
    {
        cullState.cullFace.setDisabled();
    }

    public static void cullFace(int mode)
    {
        if (mode != cullState.mode)
        {
            cullState.mode = mode;
            GL11.glCullFace(mode);
        }
    }

    public static void enablePolygonOffset()
    {
        polygonOffsetState.polygonOffsetFill.setEnabled();
    }

    public static void disablePolygonOffset()
    {
        polygonOffsetState.polygonOffsetFill.setDisabled();
    }

    public static void doPolygonOffset(float factor, float units)
    {
        if (factor != polygonOffsetState.factor || units != polygonOffsetState.units)
        {
            polygonOffsetState.factor = factor;
            polygonOffsetState.units = units;
            GL11.glPolygonOffset(factor, units);
        }
    }

    public static void enableColorLogic()
    {
        colorLogicState.colorLogicOp.setEnabled();
    }

    public static void disableColorLogic()
    {
        colorLogicState.colorLogicOp.setDisabled();
    }

    public static void colorLogicOp(int opcode)
    {
        if (opcode != colorLogicState.opcode)
        {
            colorLogicState.opcode = opcode;
            GL11.glLogicOp(opcode);
        }
    }

    public static void enableTexGenCoord(GlStateManager.TexGen p_179087_0_)
    {
        texGenCoord(p_179087_0_).textureGen.setEnabled();
    }

    public static void disableTexGenCoord(GlStateManager.TexGen p_179100_0_)
    {
        texGenCoord(p_179100_0_).textureGen.setDisabled();
    }

    public static void texGen(GlStateManager.TexGen texGen, int param)
    {
        GlStateManager.TexGenCoord glstatemanager$texgencoord = texGenCoord(texGen);

        if (param != glstatemanager$texgencoord.param)
        {
            glstatemanager$texgencoord.param = param;
            GL11.glTexGeni(glstatemanager$texgencoord.coord, GL11.GL_TEXTURE_GEN_MODE, param);
        }
    }

    public static void texGen(GlStateManager.TexGen p_179105_0_, int pname, FloatBuffer params)
    {
        GL11.glTexGenfv(texGenCoord(p_179105_0_).coord, pname, params);
    }

    private static GlStateManager.TexGenCoord texGenCoord(GlStateManager.TexGen p_179125_0_)
    {
        switch (p_179125_0_)
        {
            case S:
                return texGenState.s;

            case T:
                return texGenState.t;

            case R:
                return texGenState.r;

            case Q:
                return texGenState.q;

            default:
                return texGenState.s;
        }
    }

    public static void setActiveTexture(int texture)
    {
        if (activeTextureUnit != texture - OpenGlHelper.defaultTexUnit)
        {
            activeTextureUnit = texture - OpenGlHelper.defaultTexUnit;
            OpenGlHelper.setActiveTexture(texture);
        }
    }

    public static void enableTexture2D()
    {
        textureState[activeTextureUnit].texture2DState.setEnabled();
    }

    public static void disableTexture2D()
    {
        textureState[activeTextureUnit].texture2DState.setDisabled();
    }

    public static int generateTexture()
    {
        return GL11.glGenTextures();
    }

    public static void deleteTexture(int texture)
    {
        GL11.glDeleteTextures(texture);

        for (GlStateManager.TextureState glstatemanager$texturestate : textureState)
        {
            if (glstatemanager$texturestate.textureName == texture)
            {
                glstatemanager$texturestate.textureName = -1;
            }
        }
    }

    public static void bindTexture(int texture)
    {
        if (texture != textureState[activeTextureUnit].textureName)
        {
            textureState[activeTextureUnit].textureName = texture;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public static void enableNormalize()
    {
        normalizeState.setEnabled();
    }

    public static void disableNormalize()
    {
        normalizeState.setDisabled();
    }

    public static void shadeModel(int mode)
    {
        if (mode != activeShadeModel)
        {
            activeShadeModel = mode;
            GL11.glShadeModel(mode);
        }
    }

    public static void enableRescaleNormal()
    {
        rescaleNormalState.setEnabled();
    }

    public static void disableRescaleNormal()
    {
        rescaleNormalState.setDisabled();
    }

    private static int viewportX;
    private static int viewportY;
    private static int viewportW;
    private static int viewportH;

    public static void viewport(int x, int y, int width, int height)
    {
        if (x != viewportX || y != viewportY || width != viewportW || height != viewportH)
        {
            viewportX = x;
            viewportY = y;
            viewportW = width;
            viewportH = height;
            GL11.glViewport(x, y, width, height);
        }
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha)
    {
        if (red != colorMaskState.red || green != colorMaskState.green || blue != colorMaskState.blue || alpha != colorMaskState.alpha)
        {
            colorMaskState.red = red;
            colorMaskState.green = green;
            colorMaskState.blue = blue;
            colorMaskState.alpha = alpha;
            GL11.glColorMask(red, green, blue, alpha);
        }
    }

    public static void clearDepth(double depth)
    {
        if (depth != clearState.depth)
        {
            clearState.depth = depth;
            GL11.glClearDepth(depth);
        }
    }

    public static void clearColor(float red, float green, float blue, float alpha)
    {
        if (red != clearState.color.red || green != clearState.color.green || blue != clearState.color.blue || alpha != clearState.color.alpha)
        {
            clearState.color.red = red;
            clearState.color.green = green;
            clearState.color.blue = blue;
            clearState.color.alpha = alpha;
            GL11.glClearColor(red, green, blue, alpha);
        }
    }

    public static void clear(int mask)
    {
        GL11.glClear(mask);
    }

    public static void matrixMode(int mode)
    {
        GL11.glMatrixMode(mode);
    }

    public static void loadIdentity()
    {
        GL11.glLoadIdentity();
    }

    public static void pushMatrix()
    {
        GL11.glPushMatrix();
    }

    public static void popMatrix()
    {
        GL11.glPopMatrix();
    }

    public static void getFloat(int pname, FloatBuffer params)
    {
        GL11.glGetFloatv(pname, params);
    }

    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar)
    {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void rotate(float angle, float x, float y, float z)
    {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void scale(float x, float y, float z)
    {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z)
    {
        GL11.glScaled(x, y, z);
    }

    public static void translate(float x, float y, float z)
    {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z)
    {
        GL11.glTranslated(x, y, z);
    }

    public static void multMatrix(FloatBuffer matrix)
    {
        GL11.glMultMatrixf(matrix);
    }

    public static void color(float colorRed, float colorGreen, float colorBlue, float colorAlpha)
    {
        if (colorRed != colorState.red || colorGreen != colorState.green || colorBlue != colorState.blue || colorAlpha != colorState.alpha)
        {
            colorState.red = colorRed;
            colorState.green = colorGreen;
            colorState.blue = colorBlue;
            colorState.alpha = colorAlpha;
            GL11.glColor4f(colorRed, colorGreen, colorBlue, colorAlpha);
        }
    }

    public static void color(float colorRed, float colorGreen, float colorBlue)
    {
        color(colorRed, colorGreen, colorBlue, 1.0F);
    }

    public static void resetColor()
    {
        colorState.red = colorState.green = colorState.blue = colorState.alpha = -1.0F;
    }

    /**
     * @deprecated Display lists are a legacy GL pattern. Prefer VBO rendering.
     *             Still used by RenderGlobal (sky/stars) and ModelRenderer (entity models).
     */
    @Deprecated
    public static void callList(int list)
    {
        GL11.glCallList(list);
    }

    // ========== Modern GL state management (Phase 1) ==========

    /**
     * Bind a shader program with redundancy elimination.
     */
    public static void useProgram(int program)
    {
        if (program != currentProgram)
        {
            currentProgram = program;
            GL20.glUseProgram(program);
        }
    }

    /**
     * Bind a VAO with redundancy elimination.
     */
    public static void bindVertexArray(int vao)
    {
        if (vao != currentVao)
        {
            currentVao = vao;

            try
            {
                GL30.glBindVertexArray(vao);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    /**
     * Bind an array buffer (GL_ARRAY_BUFFER) with redundancy elimination.
     */
    public static void bindArrayBuffer(int buffer)
    {
        if (buffer != currentArrayBuffer)
        {
            currentArrayBuffer = buffer;
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        }
    }

    /**
     * Bind an element buffer (GL_ELEMENT_ARRAY_BUFFER) with redundancy elimination.
     */
    public static void bindElementBuffer(int buffer)
    {
        if (buffer != currentElementBuffer)
        {
            currentElementBuffer = buffer;
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer);
        }
    }

    /**
     * Invalidate tracked program/VAO/buffer state.
     * Call after external code (NanoVG) may have changed these bindings.
     */
    public static void invalidateModernState()
    {
        currentProgram = -1;
        currentVao = -1;
        currentArrayBuffer = -1;
        currentElementBuffer = -1;
    }

    // ========== Stencil state (deobfuscated) ==========

    public static void stencilFunc(int func, int ref, int mask)
    {
        GlStateManager.StencilFunc sf = stencilState.stencilFunc;

        if (func != sf.func || ref != sf.ref || mask != sf.mask)
        {
            sf.func = func;
            sf.ref = ref;
            sf.mask = mask;
            GL11.glStencilFunc(func, ref, mask);
        }
    }

    public static void stencilOp(int sfail, int dpfail, int dppass)
    {
        if (sfail != stencilState.sfail || dpfail != stencilState.dpfail || dppass != stencilState.dppass)
        {
            stencilState.sfail = sfail;
            stencilState.dpfail = dpfail;
            stencilState.dppass = dppass;
            GL11.glStencilOp(sfail, dpfail, dppass);
        }
    }

    public static void stencilMask(int mask)
    {
        if (mask != stencilState.writeMask)
        {
            stencilState.writeMask = mask;
            GL11.glStencilMask(mask);
        }
    }

    // ========== State snapshot / restore (for NanoVG isolation) ==========

    /**
     * Save current GL state tracked by GlStateManager.
     * Used before NanoVG frame to preserve Minecraft's rendering state.
     */
    public static void saveState()
    {
        savedSnapshot = new StateSnapshot(
            alphaState.alphaTest.currentState,
            alphaState.func,
            alphaState.ref,
            blendState.blend.currentState,
            blendState.srcFactor,
            blendState.dstFactor,
            blendState.srcFactorAlpha,
            blendState.dstFactorAlpha,
            depthState.depthTest.currentState,
            depthState.maskEnabled,
            depthState.depthFunc,
            cullState.cullFace.currentState,
            cullState.mode,
            colorState.red,
            colorState.green,
            colorState.blue,
            colorState.alpha,
            activeTextureUnit,
            textureState[0].textureName,
            textureState[0].texture2DState.currentState,
            currentProgram,
            currentVao,
            currentArrayBuffer,
            currentElementBuffer
        );
    }

    /**
     * Restore GL state from the last {@link #saveState()} call.
     * Used after NanoVG frame to restore Minecraft's rendering state.
     */
    public static void restoreState()
    {
        if (savedSnapshot == null)
        {
            return;
        }

        StateSnapshot s = savedSnapshot;
        savedSnapshot = null;

        // Force-reset tracked state so the next set call actually issues GL commands.
        // NanoVG may have changed real GL state without going through GlStateManager.

        // Program / VAO / buffers
        currentProgram = -1;
        useProgram(s.program);
        currentVao = -1;
        bindVertexArray(s.vao);
        currentArrayBuffer = -1;
        bindArrayBuffer(s.arrayBuffer);
        currentElementBuffer = -1;
        bindElementBuffer(s.elementBuffer);

        // Texture unit 0
        if (activeTextureUnit != s.activeTexUnit)
        {
            activeTextureUnit = -1;
            setActiveTexture(s.activeTexUnit + OpenGlHelper.defaultTexUnit);
        }

        textureState[0].textureName = -1;
        bindTexture(s.texture0Name);
        textureState[0].texture2DState.currentState = !s.texture2DEnabled;
        textureState[0].texture2DState.setState(s.texture2DEnabled);

        // Alpha
        alphaState.alphaTest.currentState = !s.alphaEnabled;
        alphaState.alphaTest.setState(s.alphaEnabled);
        alphaState.func = -1;
        alphaState.ref = -1.0F;
        alphaFunc(s.alphaFunc, s.alphaRef);

        // Blend
        blendState.blend.currentState = !s.blendEnabled;
        blendState.blend.setState(s.blendEnabled);
        blendState.srcFactor = -1;
        blendState.dstFactor = -1;
        blendState.srcFactorAlpha = -1;
        blendState.dstFactorAlpha = -1;
        tryBlendFuncSeparate(s.blendSrc, s.blendDst, s.blendSrcAlpha, s.blendDstAlpha);

        // Depth
        depthState.depthTest.currentState = !s.depthEnabled;
        depthState.depthTest.setState(s.depthEnabled);
        depthState.maskEnabled = !s.depthMask;
        depthMask(s.depthMask);
        depthState.depthFunc = -1;
        depthFunc(s.depthFunc);

        // Cull
        cullState.cullFace.currentState = !s.cullEnabled;
        cullState.cullFace.setState(s.cullEnabled);
        cullState.mode = -1;
        cullFace(s.cullMode);

        // Color
        colorState.red = -1.0F;
        color(s.colorR, s.colorG, s.colorB, s.colorA);
    }

    /**
     * Drain any accumulated GL errors to prevent stale error state.
     */
    public static void drainGlErrors()
    {
        for (int i = 0; i < 64; ++i)
        {
            if (GL11.glGetError() == GL11.GL_NO_ERROR)
            {
                break;
            }
        }
    }

    static
    {
        for (int i = 0; i < 8; ++i)
        {
            lightState[i] = new GlStateManager.BooleanState(16384 + i);
        }

        for (int j = 0; j < 8; ++j)
        {
            textureState[j] = new GlStateManager.TextureState();
        }
    }

    static class AlphaState
    {
        public GlStateManager.BooleanState alphaTest;
        public int func;
        public float ref;

        private AlphaState()
        {
            this.alphaTest = new GlStateManager.BooleanState(3008);
            this.func = 519;
            this.ref = -1.0F;
        }
    }

    static class BlendState
    {
        public GlStateManager.BooleanState blend;
        public int srcFactor;
        public int dstFactor;
        public int srcFactorAlpha;
        public int dstFactorAlpha;

        private BlendState()
        {
            this.blend = new GlStateManager.BooleanState(3042);
            this.srcFactor = 1;
            this.dstFactor = 0;
            this.srcFactorAlpha = 1;
            this.dstFactorAlpha = 0;
        }
    }

    static class BooleanState
    {
        private final int capability;
        private boolean currentState = false;

        public BooleanState(int capabilityIn)
        {
            this.capability = capabilityIn;
        }

        public void setDisabled()
        {
            this.setState(false);
        }

        public void setEnabled()
        {
            this.setState(true);
        }

        public void setState(boolean state)
        {
            if (state != this.currentState)
            {
                this.currentState = state;

                if (state)
                {
                    GL11.glEnable(this.capability);
                }
                else
                {
                    GL11.glDisable(this.capability);
                }
            }
        }
    }

    static class ClearState
    {
        public double depth;
        public GlStateManager.Color color;
        public int field_179204_c;

        private ClearState()
        {
            this.depth = 1.0D;
            this.color = new GlStateManager.Color(0.0F, 0.0F, 0.0F, 0.0F);
            this.field_179204_c = 0;
        }
    }

    static class Color
    {
        public float red = 1.0F;
        public float green = 1.0F;
        public float blue = 1.0F;
        public float alpha = 1.0F;

        public Color()
        {
        }

        public Color(float redIn, float greenIn, float blueIn, float alphaIn)
        {
            this.red = redIn;
            this.green = greenIn;
            this.blue = blueIn;
            this.alpha = alphaIn;
        }
    }

    static class ColorLogicState
    {
        public GlStateManager.BooleanState colorLogicOp;
        public int opcode;

        private ColorLogicState()
        {
            this.colorLogicOp = new GlStateManager.BooleanState(3058);
            this.opcode = 5379;
        }
    }

    static class ColorMask
    {
        public boolean red;
        public boolean green;
        public boolean blue;
        public boolean alpha;

        private ColorMask()
        {
            this.red = true;
            this.green = true;
            this.blue = true;
            this.alpha = true;
        }
    }

    static class ColorMaterialState
    {
        public GlStateManager.BooleanState colorMaterial;
        public int face;
        public int mode;

        private ColorMaterialState()
        {
            this.colorMaterial = new GlStateManager.BooleanState(2903);
            this.face = 1032;
            this.mode = 5634;
        }
    }

    static class CullState
    {
        public GlStateManager.BooleanState cullFace;
        public int mode;

        private CullState()
        {
            this.cullFace = new GlStateManager.BooleanState(2884);
            this.mode = 1029;
        }
    }

    static class DepthState
    {
        public GlStateManager.BooleanState depthTest;
        public boolean maskEnabled;
        public int depthFunc;

        private DepthState()
        {
            this.depthTest = new GlStateManager.BooleanState(2929);
            this.maskEnabled = true;
            this.depthFunc = 513;
        }
    }

    static class FogState
    {
        public GlStateManager.BooleanState fog;
        public int mode;
        public float density;
        public float start;
        public float end;

        private FogState()
        {
            this.fog = new GlStateManager.BooleanState(2912);
            this.mode = 2048;
            this.density = 1.0F;
            this.start = 0.0F;
            this.end = 1.0F;
        }
    }

    static class PolygonOffsetState
    {
        public GlStateManager.BooleanState polygonOffsetFill;
        public GlStateManager.BooleanState polygonOffsetLine;
        public float factor;
        public float units;

        private PolygonOffsetState()
        {
            this.polygonOffsetFill = new GlStateManager.BooleanState(32823);
            this.polygonOffsetLine = new GlStateManager.BooleanState(10754);
            this.factor = 0.0F;
            this.units = 0.0F;
        }
    }

    static class StencilFunc
    {
        public int func;
        public int ref;
        public int mask;

        private StencilFunc()
        {
            this.func = 519;
            this.ref = 0;
            this.mask = -1;
        }
    }

    static class StencilState
    {
        public GlStateManager.StencilFunc stencilFunc;
        public int writeMask;
        public int sfail;
        public int dpfail;
        public int dppass;

        private StencilState()
        {
            this.stencilFunc = new GlStateManager.StencilFunc();
            this.writeMask = -1;
            this.sfail = 7680;
            this.dpfail = 7680;
            this.dppass = 7680;
        }
    }

    public static enum TexGen
    {
        S,
        T,
        R,
        Q;
    }

    static class TexGenCoord
    {
        public GlStateManager.BooleanState textureGen;
        public int coord;
        public int param = -1;

        public TexGenCoord(int p_i46254_1_, int p_i46254_2_)
        {
            this.coord = p_i46254_1_;
            this.textureGen = new GlStateManager.BooleanState(p_i46254_2_);
        }
    }

    static class TexGenState
    {
        public GlStateManager.TexGenCoord s;
        public GlStateManager.TexGenCoord t;
        public GlStateManager.TexGenCoord r;
        public GlStateManager.TexGenCoord q;

        private TexGenState()
        {
            this.s = new GlStateManager.TexGenCoord(8192, 3168);
            this.t = new GlStateManager.TexGenCoord(8193, 3169);
            this.r = new GlStateManager.TexGenCoord(8194, 3170);
            this.q = new GlStateManager.TexGenCoord(8195, 3171);
        }
    }

    static class TextureState
    {
        public GlStateManager.BooleanState texture2DState;
        public int textureName;

        private TextureState()
        {
            this.texture2DState = new GlStateManager.BooleanState(3553);
            this.textureName = 0;
        }
    }

    /**
     * Immutable snapshot of tracked GL state for save/restore cycles.
     */
    static class StateSnapshot
    {
        final boolean alphaEnabled;
        final int alphaFunc;
        final float alphaRef;
        final boolean blendEnabled;
        final int blendSrc;
        final int blendDst;
        final int blendSrcAlpha;
        final int blendDstAlpha;
        final boolean depthEnabled;
        final boolean depthMask;
        final int depthFunc;
        final boolean cullEnabled;
        final int cullMode;
        final float colorR;
        final float colorG;
        final float colorB;
        final float colorA;
        final int activeTexUnit;
        final int texture0Name;
        final boolean texture2DEnabled;
        final int program;
        final int vao;
        final int arrayBuffer;
        final int elementBuffer;

        StateSnapshot(
            boolean alphaEnabled, int alphaFunc, float alphaRef,
            boolean blendEnabled, int blendSrc, int blendDst, int blendSrcAlpha, int blendDstAlpha,
            boolean depthEnabled, boolean depthMask, int depthFunc,
            boolean cullEnabled, int cullMode,
            float colorR, float colorG, float colorB, float colorA,
            int activeTexUnit, int texture0Name, boolean texture2DEnabled,
            int program, int vao, int arrayBuffer, int elementBuffer)
        {
            this.alphaEnabled = alphaEnabled;
            this.alphaFunc = alphaFunc;
            this.alphaRef = alphaRef;
            this.blendEnabled = blendEnabled;
            this.blendSrc = blendSrc;
            this.blendDst = blendDst;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.depthEnabled = depthEnabled;
            this.depthMask = depthMask;
            this.depthFunc = depthFunc;
            this.cullEnabled = cullEnabled;
            this.cullMode = cullMode;
            this.colorR = colorR;
            this.colorG = colorG;
            this.colorB = colorB;
            this.colorA = colorA;
            this.activeTexUnit = activeTexUnit;
            this.texture0Name = texture0Name;
            this.texture2DEnabled = texture2DEnabled;
            this.program = program;
            this.vao = vao;
            this.arrayBuffer = arrayBuffer;
            this.elementBuffer = elementBuffer;
        }
    }
}
