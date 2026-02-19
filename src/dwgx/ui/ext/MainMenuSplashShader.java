package dwgx.ui.ext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/**
 * Lightweight GLSL program for animated splash text tint.
 */
public final class MainMenuSplashShader
{
    private static final Logger LOGGER = LogManager.getLogger(MainMenuSplashShader.class);
    private static final String FRAGMENT_RESOURCE_PATH = "/dwgx/ui/ext/flappybird_frag.glsl";
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
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "    vec4 texel = texture2D(uTex, vTexCoord);\n" +
        "    float wave = 0.5 + 0.5 * sin(vTexCoord.x * 16.0 + vTexCoord.y * 6.0 + uTime * 3.0);\n" +
        "    vec3 pulse = mix(uBaseColor * 0.62, vec3(1.0), wave);\n" +
        "    vec4 outColor = vec4(pulse, 1.0) * texel * vColor;\n" +
        "    if (outColor.a <= 0.001) {\n" +
        "        discard;\n" +
        "    }\n" +
        "    gl_FragColor = outColor;\n" +
        "}\n";
    private static final String FRAGMENT_SOURCE = loadFragmentSource();

    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private int uniformTex = -1;
    private int uniformTime = -1;
    private int uniformResolution = -1;
    private int uniformBaseColor = -1;
    private boolean failed;
    private boolean active;
    private long startAtMs = System.currentTimeMillis();

    public boolean begin(int baseColor)
    {
        if (!UiExtensionManager.isSplashShaderEnabled() || !isShaderPathAvailable())
        {
            return false;
        }

        if (!this.ensureProgram())
        {
            return false;
        }

        try
        {
            OpenGlHelper.glUseProgram(this.programId);
            OpenGlHelper.glUniform1i(this.uniformTex, 0);
            this.setUniform1f(this.uniformTime, (float)(System.currentTimeMillis() - this.startAtMs) / 1000.0F);
            this.setUniform2f(this.uniformResolution, (float)Math.max(1, Display.getWidth()), (float)Math.max(1, Display.getHeight()));

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
            this.markFailed("Failed to activate splash shader program.", throwable);
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
                this.fragmentShaderId = this.compileShader(OpenGlHelper.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
            }
            catch (Throwable primaryError)
            {
                LOGGER.warn("Primary splash fragment shader failed; using fallback fragment shader.", primaryError);
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

            this.uniformTex = OpenGlHelper.glGetUniformLocation(this.programId, "uTex");
            this.uniformTime = OpenGlHelper.glGetUniformLocation(this.programId, "iTime");
            if (this.uniformTime < 0)
            {
                this.uniformTime = OpenGlHelper.glGetUniformLocation(this.programId, "uTime");
            }

            this.uniformResolution = OpenGlHelper.glGetUniformLocation(this.programId, "iResolution");
            this.uniformBaseColor = OpenGlHelper.glGetUniformLocation(this.programId, "uBaseColor");
            this.startAtMs = System.currentTimeMillis();
            return true;
        }
        catch (Throwable throwable)
        {
            this.markFailed("Splash shader initialization failed.", throwable);
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

        this.uniformTex = -1;
        this.uniformTime = -1;
        this.uniformResolution = -1;
        this.uniformBaseColor = -1;
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

        return GLContext.getCapabilities().OpenGL20 || GLContext.getCapabilities().GL_ARB_shader_objects;
    }

    private static String loadFragmentSource()
    {
        String source = readResource(FRAGMENT_RESOURCE_PATH);

        if (source == null || source.trim().isEmpty())
        {
            LOGGER.warn("Splash shader resource {} not found, using fallback fragment shader.", FRAGMENT_RESOURCE_PATH);
            return FALLBACK_FRAGMENT_SOURCE;
        }

        return source;
    }

    private static String readResource(String path)
    {
        InputStream stream = MainMenuSplashShader.class.getResourceAsStream(path);

        if (stream == null)
        {
            return null;
        }

        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream(8192))
        {
            byte[] buffer = new byte[4096];
            int read;

            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }

            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException ioException)
        {
            LOGGER.warn("Failed to read splash shader resource {}.", path, ioException);
            return null;
        }
    }

    private void setUniform1f(int location, float value)
    {
        if (location < 0)
        {
            return;
        }

        if (GLContext.getCapabilities().OpenGL20)
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

        if (GLContext.getCapabilities().OpenGL20)
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

        if (GLContext.getCapabilities().OpenGL20)
        {
            GL20.glUniform3f(location, x, y, z);
        }
        else
        {
            ARBShaderObjects.glUniform3fARB(location, x, y, z);
        }
    }
}
