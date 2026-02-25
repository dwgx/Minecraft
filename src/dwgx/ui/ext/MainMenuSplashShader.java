package dwgx.ui.ext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBShaderObjects;
import client.runtime.lwjgl.GlfwWindow;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

/**
 * Shared shader program used by main-menu splash text and shader-driven backgrounds.
 * Supports scene-defined fragment source and Flappy/Atari uniform sets.
 */
public final class MainMenuSplashShader
{
    public enum TextureAlphaMode
    {
        USE_SOURCE_ALPHA,
        IGNORE_SOURCE_ALPHA
    }

    private static final Logger LOGGER = LogManager.getLogger(MainMenuSplashShader.class);
    private static final String VERTEX_SOURCE =
        "#version 120\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
        "    vTexCoord = gl_MultiTexCoord0.xy;\n" +
        "    vColor = gl_Color;\n" +
        "}\n";
    private static final String FALLBACK_FRAGMENT_SOURCE =
        "#version 120\n" +
        "uniform sampler2D uTex;\n" +
        "uniform float uTime;\n" +
        "uniform vec3 uBaseColor;\n" +
        "uniform float uUseTexAlpha;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "    vec4 texel = texture2D(uTex, vTexCoord);\n" +
        "    float wave = 0.5 + 0.5 * sin(vTexCoord.x * 16.0 + vTexCoord.y * 6.0 + uTime * 3.0);\n" +
        "    vec3 pulse = mix(uBaseColor * 0.62, vec3(1.0), wave);\n" +
        "    float texMode = uUseTexAlpha > 0.5 ? 1.0 : 0.0;\n" +
        "    if (texMode > 0.5 && texel.a <= 0.001) {\n" +
        "        discard;\n" +
        "    }\n" +
        "    vec3 texRgb = texMode > 0.5 ? texel.rgb : vec3(1.0);\n" +
        "    float texAlpha = texMode > 0.5 ? texel.a : 1.0;\n" +
        "    vec4 outColor = vec4(pulse * texRgb, texAlpha) * vColor;\n" +
        "    gl_FragColor = outColor;\n" +
        "}\n";

    private final String fragmentSource;

    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private int uniformTexture = -1;
    private int uniformTime = -1;
    private int uniformResolution = -1;
    private int uniformBaseColor = -1;
    private int uniformUseTexAlpha = -1;

    private int uniformGameTick = -1;
    private int uniformBirdY = -1;
    private int uniformBirdFrame = -1;
    private int uniformBirdAlive = -1;

    private int uniformControlEnabled = -1;
    private int uniformAtariPlayer = -1;
    private int uniformAtariBox = -1;
    private int uniformAtariTarget = -1;
    private int uniformAtariRock = -1;
    private int uniformAtariDoorOpen = -1;
    private int uniformAtariMode = -1;
    private int uniformAtariFlash = -1;
    private int uniformAtariLevel = -1;

    private boolean failed;
    private boolean active;
    private long startAtMs = System.currentTimeMillis();

    private float flappyGameTick;
    private float flappyBirdY = 110.0F;
    private float flappyBirdFrame;
    private boolean flappyAlive = true;

    private float atariPlayerX = -1.0F;
    private float atariPlayerY = -1.0F;
    private float atariBoxX = -1.0F;
    private float atariBoxY = -1.0F;
    private float atariTargetX = -1.0F;
    private float atariTargetY = -1.0F;
    private float atariRockX = -1.0F;
    private float atariRockY = -1.0F;
    private float atariDoorOpen;
    private float atariMode;
    private float atariFlash;
    private float atariLevel = 1.0F;
    private boolean controlEnabled;

    public MainMenuSplashShader()
    {
        this(FlappyBirdFragmentShaderTemplate.source());
    }

    public MainMenuSplashShader(String fragmentSource)
    {
        String candidate = fragmentSource;

        if (candidate == null || candidate.trim().isEmpty())
        {
            candidate = FALLBACK_FRAGMENT_SOURCE;
        }

        this.fragmentSource = candidate;
    }

    public void setFlappyState(float gameTick, float birdY, float birdFrame, boolean alive, boolean controlEnabled)
    {
        this.flappyGameTick = gameTick;
        this.flappyBirdY = birdY;
        this.flappyBirdFrame = birdFrame;
        this.flappyAlive = alive;
        this.controlEnabled = controlEnabled;
    }

    public void setAtariState(
        float playerX,
        float playerY,
        float boxX,
        float boxY,
        float targetX,
        float targetY,
        float rockX,
        float rockY,
        float doorOpen,
        float mode,
        float flash,
        float level,
        boolean controlEnabled
    )
    {
        this.atariPlayerX = playerX;
        this.atariPlayerY = playerY;
        this.atariBoxX = boxX;
        this.atariBoxY = boxY;
        this.atariTargetX = targetX;
        this.atariTargetY = targetY;
        this.atariRockX = rockX;
        this.atariRockY = rockY;
        this.atariDoorOpen = doorOpen;
        this.atariMode = mode;
        this.atariFlash = flash;
        this.atariLevel = level;
        this.controlEnabled = controlEnabled;
    }

    public boolean begin(int baseColor)
    {
        return this.begin(baseColor, TextureAlphaMode.USE_SOURCE_ALPHA);
    }

    public boolean begin(int baseColor, boolean useTextureAlpha)
    {
        return this.begin(baseColor, useTextureAlpha ? TextureAlphaMode.USE_SOURCE_ALPHA : TextureAlphaMode.IGNORE_SOURCE_ALPHA);
    }

    public boolean begin(int baseColor, TextureAlphaMode alphaMode)
    {
        if (!isShaderPathAvailable())
        {
            return false;
        }

        if (!this.ensureProgram())
        {
            return false;
        }

        TextureAlphaMode mode = alphaMode == null ? TextureAlphaMode.USE_SOURCE_ALPHA : alphaMode;

        try
        {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.glUseProgram(this.programId);
            OpenGlHelper.glUniform1i(this.uniformTexture, 0);

            this.setUniform1f(this.uniformUseTexAlpha, mode == TextureAlphaMode.USE_SOURCE_ALPHA ? 1.0F : 0.0F);
            this.setUniform1f(this.uniformTime, (float)(System.currentTimeMillis() - this.startAtMs) / 1000.0F);
            this.setUniform2f(this.uniformResolution, (float)Math.max(1, GlfwWindow.getWidth()), (float)Math.max(1, GlfwWindow.getHeight()));

            this.setUniform1f(this.uniformGameTick, this.flappyGameTick);
            this.setUniform1f(this.uniformBirdY, this.flappyBirdY);
            this.setUniform1f(this.uniformBirdFrame, this.flappyBirdFrame);
            this.setUniform1f(this.uniformBirdAlive, this.flappyAlive ? 1.0F : 0.0F);

            this.setUniform1f(this.uniformControlEnabled, this.controlEnabled ? 1.0F : 0.0F);
            this.setUniform2f(this.uniformAtariPlayer, this.atariPlayerX, this.atariPlayerY);
            this.setUniform2f(this.uniformAtariBox, this.atariBoxX, this.atariBoxY);
            this.setUniform2f(this.uniformAtariTarget, this.atariTargetX, this.atariTargetY);
            this.setUniform2f(this.uniformAtariRock, this.atariRockX, this.atariRockY);
            this.setUniform1f(this.uniformAtariDoorOpen, this.atariDoorOpen);
            this.setUniform1f(this.uniformAtariMode, this.atariMode);
            this.setUniform1f(this.uniformAtariFlash, this.atariFlash);
            this.setUniform1f(this.uniformAtariLevel, this.atariLevel);

            float r = (float)(baseColor >> 16 & 255) / 255.0F;
            float g = (float)(baseColor >> 8 & 255) / 255.0F;
            float b = (float)(baseColor & 255) / 255.0F;
            this.setUniform3f(this.uniformBaseColor, r, g, b);
            this.active = true;
            return true;
        }
        catch (Throwable throwable)
        {
            this.active = false;
            OpenGlHelper.glUseProgram(0);
            this.markFailed("Main menu shader activation failed.", throwable);
            return false;
        }
    }

    public void end()
    {
        if (!this.active)
        {
            return;
        }

        this.active = false;
        OpenGlHelper.glUseProgram(0);
    }

    public void close()
    {
        this.end();
        this.deleteProgram();
    }

    private boolean ensureProgram()
    {
        if (this.programId != 0)
        {
            return true;
        }

        if (this.failed)
        {
            return false;
        }

        try
        {
            this.vertexShaderId = this.compileShader(OpenGlHelper.GL_VERTEX_SHADER, VERTEX_SOURCE);

            try
            {
                this.fragmentShaderId = this.compileShader(OpenGlHelper.GL_FRAGMENT_SHADER, this.fragmentSource);
            }
            catch (Throwable primaryError)
            {
                LOGGER.warn("Primary menu fragment shader failed, switching to fallback: {}", primaryError.getMessage());
                this.fragmentShaderId = this.compileShader(OpenGlHelper.GL_FRAGMENT_SHADER, FALLBACK_FRAGMENT_SOURCE);
            }

            this.programId = OpenGlHelper.glCreateProgram();
            OpenGlHelper.glAttachShader(this.programId, this.vertexShaderId);
            OpenGlHelper.glAttachShader(this.programId, this.fragmentShaderId);
            OpenGlHelper.glLinkProgram(this.programId);

            if (OpenGlHelper.glGetProgrami(this.programId, OpenGlHelper.GL_LINK_STATUS) == 0)
            {
                String info = OpenGlHelper.glGetProgramInfoLog(this.programId, 32768);
                throw new IllegalStateException("Program link failed: " + info);
            }

            this.uniformTexture = OpenGlHelper.glGetUniformLocation(this.programId, "uTex");
            this.uniformTime = OpenGlHelper.glGetUniformLocation(this.programId, "iTime");

            if (this.uniformTime < 0)
            {
                this.uniformTime = OpenGlHelper.glGetUniformLocation(this.programId, "uTime");
            }

            if (this.uniformTime < 0)
            {
                this.uniformTime = OpenGlHelper.glGetUniformLocation(this.programId, "time");
            }

            this.uniformResolution = OpenGlHelper.glGetUniformLocation(this.programId, "iResolution");

            if (this.uniformResolution < 0)
            {
                this.uniformResolution = OpenGlHelper.glGetUniformLocation(this.programId, "resolution");
            }

            this.uniformBaseColor = OpenGlHelper.glGetUniformLocation(this.programId, "uBaseColor");
            this.uniformUseTexAlpha = OpenGlHelper.glGetUniformLocation(this.programId, "uUseTexAlpha");
            this.uniformGameTick = OpenGlHelper.glGetUniformLocation(this.programId, "uGameTick");
            this.uniformBirdY = OpenGlHelper.glGetUniformLocation(this.programId, "uBirdY");
            this.uniformBirdFrame = OpenGlHelper.glGetUniformLocation(this.programId, "uBirdFrame");
            this.uniformBirdAlive = OpenGlHelper.glGetUniformLocation(this.programId, "uBirdAlive");
            this.uniformControlEnabled = OpenGlHelper.glGetUniformLocation(this.programId, "uControlEnabled");
            this.uniformAtariPlayer = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariPlayer");
            this.uniformAtariBox = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariBox");
            this.uniformAtariTarget = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariTarget");
            this.uniformAtariRock = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariRock");
            this.uniformAtariDoorOpen = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariDoorOpen");
            this.uniformAtariMode = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariMode");
            this.uniformAtariFlash = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariFlash");
            this.uniformAtariLevel = OpenGlHelper.glGetUniformLocation(this.programId, "uAtariLevel");

            this.startAtMs = System.currentTimeMillis();
            return true;
        }
        catch (Throwable throwable)
        {
            this.markFailed("Main menu shader initialization failed.", throwable);
            this.deleteProgram();
            return false;
        }
    }

    private int compileShader(int type, String source)
    {
        int shader = OpenGlHelper.glCreateShader(type);
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        OpenGlHelper.glShaderSource(shader, ByteBuffer.wrap(bytes));
        OpenGlHelper.glCompileShader(shader);

        if (OpenGlHelper.glGetShaderi(shader, OpenGlHelper.GL_COMPILE_STATUS) == 0)
        {
            String info = OpenGlHelper.glGetShaderInfoLog(shader, 32768);
            OpenGlHelper.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + info);
        }

        return shader;
    }

    private void deleteProgram()
    {
        if (this.vertexShaderId != 0)
        {
            OpenGlHelper.glDeleteShader(this.vertexShaderId);
            this.vertexShaderId = 0;
        }

        if (this.fragmentShaderId != 0)
        {
            OpenGlHelper.glDeleteShader(this.fragmentShaderId);
            this.fragmentShaderId = 0;
        }

        if (this.programId != 0)
        {
            OpenGlHelper.glDeleteProgram(this.programId);
            this.programId = 0;
        }

        this.uniformTexture = -1;
        this.uniformTime = -1;
        this.uniformResolution = -1;
        this.uniformBaseColor = -1;
        this.uniformUseTexAlpha = -1;
        this.uniformGameTick = -1;
        this.uniformBirdY = -1;
        this.uniformBirdFrame = -1;
        this.uniformBirdAlive = -1;
        this.uniformControlEnabled = -1;
        this.uniformAtariPlayer = -1;
        this.uniformAtariBox = -1;
        this.uniformAtariTarget = -1;
        this.uniformAtariRock = -1;
        this.uniformAtariDoorOpen = -1;
        this.uniformAtariMode = -1;
        this.uniformAtariFlash = -1;
        this.uniformAtariLevel = -1;
    }

    private void markFailed(String message, Throwable throwable)
    {
        if (this.failed)
        {
            return;
        }

        this.failed = true;
        LOGGER.warn(message, throwable);
    }

    private static boolean isShaderPathAvailable()
    {
        if (!OpenGlHelper.areShadersSupported())
        {
            return false;
        }

        return GL.getCapabilities().OpenGL20 || GL.getCapabilities().GL_ARB_shader_objects;
    }

    private void setUniform1f(int location, float value)
    {
        if (location < 0)
        {
            return;
        }

        if (GL.getCapabilities().OpenGL20)
        {
            GL20.glUniform1f(location, value);
        }
        else
        {
            ARBShaderObjects.glUniform1fARB(location, value);
        }
    }

    private void setUniform2f(int location, float x, float y)
    {
        if (location < 0)
        {
            return;
        }

        if (GL.getCapabilities().OpenGL20)
        {
            GL20.glUniform2f(location, x, y);
        }
        else
        {
            ARBShaderObjects.glUniform2fARB(location, x, y);
        }
    }

    private void setUniform3f(int location, float x, float y, float z)
    {
        if (location < 0)
        {
            return;
        }

        if (GL.getCapabilities().OpenGL20)
        {
            GL20.glUniform3f(location, x, y, z);
        }
        else
        {
            ARBShaderObjects.glUniform3fARB(location, x, y, z);
        }
    }

    private void setUniform4f(int location, float x, float y, float z, float w)
    {
        if (location < 0)
        {
            return;
        }

        if (GL.getCapabilities().OpenGL20)
        {
            GL20.glUniform4f(location, x, y, z, w);
        }
        else
        {
            ARBShaderObjects.glUniform4fARB(location, x, y, z, w);
        }
    }
}
