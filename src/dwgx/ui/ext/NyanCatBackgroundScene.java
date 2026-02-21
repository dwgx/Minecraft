package dwgx.ui.ext;

/**
 * Nyan Cat style shader-only background scene.
 */
public final class NyanCatBackgroundScene implements MainMenuBackgroundScene
{
    public String fragmentShaderSource()
    {
        return NyanCatFragmentShaderTemplate.source();
    }

    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA;
    }

    public void initialize()
    {
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        return false;
    }

    public void tick(boolean sceneEnabled)
    {
    }

    public void applyShaderUniforms(MainMenuSplashShader shader)
    {
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        return sceneEnabled ? "Nyan Cat: shader visual mode" : null;
    }
}

final class NyanCatFragmentShaderTemplate
{
    private NyanCatFragmentShaderTemplate()
    {
    }

    static String source()
    {
        return String.join("\n", LINES);
    }

    private static final String[] LINES = new String[]
    {
        "#version 120",
        "uniform float iTime;",
        "uniform vec2 iResolution;",
        "varying vec4 vColor;",
        "",
        "vec3 pal(float t, vec3 a, vec3 b, vec3 c, vec3 d)",
        "{",
        "    return a + b * cos(6.28318 * (c * t + d));",
        "}",
        "",
        "float hash(vec2 p)",
        "{",
        "    float h = dot(p, vec2(127.1, 311.7));",
        "    return fract(sin(h) * 43758.5453123);",
        "}",
        "",
        "float mapValue(float value, float min1, float max1, float min2, float max2)",
        "{",
        "    return min2 + (value - min1) * (max2 - min2) / max(0.0001, max1 - min1);",
        "}",
        "",
        "bool inRect(vec2 p, vec2 minP, vec2 maxP)",
        "{",
        "    return p.x >= minP.x && p.x <= maxP.x && p.y >= minP.y && p.y <= maxP.y;",
        "}",
        "",
        "vec4 nyanSprite(vec2 uv, float frame)",
        "{",
        "    vec4 outCol = vec4(0.0);",
        "",
        "    if (uv.x <= 0.001 || uv.y <= 0.001 || uv.x >= 0.999 || uv.y >= 0.999)",
        "    {",
        "        return outCol;",
        "    }",
        "",
        "    vec2 p = floor(uv * vec2(48.0, 32.0));",
        "    float legShift = mod(frame, 2.0) < 0.5 ? 0.0 : 1.0;",
        "",
        "    if (inRect(p, vec2(8.0, 14.0), vec2(13.0, 17.0))) outCol = vec4(0.60, 0.60, 0.63, 1.0);",
        "    if (inRect(p, vec2(7.0, 15.0 + legShift), vec2(8.0, 16.0 + legShift))) outCol = vec4(0.47, 0.47, 0.50, 1.0);",
        "",
        "    if (inRect(p, vec2(14.0, 10.0), vec2(30.0, 22.0))) outCol = vec4(0.96, 0.82, 0.68, 1.0);",
        "    if (inRect(p, vec2(15.0, 11.0), vec2(29.0, 21.0))) outCol = vec4(0.98, 0.68, 0.78, 1.0);",
        "",
        "    if (inRect(p, vec2(17.0, 13.0), vec2(17.0, 13.0))) outCol = vec4(0.85, 0.28, 0.55, 1.0);",
        "    if (inRect(p, vec2(20.0, 17.0), vec2(20.0, 17.0))) outCol = vec4(0.28, 0.75, 0.98, 1.0);",
        "    if (inRect(p, vec2(23.0, 14.0), vec2(23.0, 14.0))) outCol = vec4(0.98, 0.92, 0.30, 1.0);",
        "    if (inRect(p, vec2(26.0, 18.0), vec2(26.0, 18.0))) outCol = vec4(0.48, 0.93, 0.48, 1.0);",
        "",
        "    if (inRect(p, vec2(30.0, 10.0), vec2(40.0, 21.0))) outCol = vec4(0.62, 0.62, 0.66, 1.0);",
        "    if (inRect(p, vec2(31.0, 21.0), vec2(33.0, 24.0))) outCol = vec4(0.62, 0.62, 0.66, 1.0);",
        "    if (inRect(p, vec2(37.0, 21.0), vec2(39.0, 24.0))) outCol = vec4(0.62, 0.62, 0.66, 1.0);",
        "    if (inRect(p, vec2(31.0, 11.0), vec2(39.0, 20.0))) outCol = vec4(0.73, 0.73, 0.77, 1.0);",
        "",
        "    if (inRect(p, vec2(34.0, 16.0), vec2(34.0, 16.0))) outCol = vec4(0.12, 0.12, 0.15, 1.0);",
        "    if (inRect(p, vec2(37.0, 16.0), vec2(37.0, 16.0))) outCol = vec4(0.12, 0.12, 0.15, 1.0);",
        "    if (inRect(p, vec2(35.0, 14.0), vec2(36.0, 14.0))) outCol = vec4(0.12, 0.12, 0.15, 1.0);",
        "",
        "    if (inRect(p, vec2(16.0, 8.0 + legShift), vec2(18.0, 10.0 + legShift))) outCol = vec4(0.52, 0.52, 0.56, 1.0);",
        "    if (inRect(p, vec2(22.0, 8.0), vec2(24.0, 10.0))) outCol = vec4(0.52, 0.52, 0.56, 1.0);",
        "    if (inRect(p, vec2(31.0, 8.0), vec2(33.0, 10.0))) outCol = vec4(0.52, 0.52, 0.56, 1.0);",
        "    if (inRect(p, vec2(37.0, 8.0 + legShift), vec2(39.0, 10.0 + legShift))) outCol = vec4(0.52, 0.52, 0.56, 1.0);",
        "",
        "    if (inRect(p, vec2(14.0, 10.0), vec2(30.0, 22.0))",
        "        && (p.x == 14.0 || p.x == 30.0 || p.y == 10.0 || p.y == 22.0))",
        "    {",
        "        outCol = vec4(0.20, 0.16, 0.12, 1.0);",
        "    }",
        "",
        "    return outCol;",
        "}",
        "",
        "void main()",
        "{",
        "    vec2 uv = gl_FragCoord.xy / max(1.0, iResolution.y);",
        "",
        "    vec2 newUv = uv;",
        "    newUv *= 2.0;",
        "    newUv -= vec2(2.5, 0.55);",
        "    newUv.y -= sin(1.5 * 6.0 + iTime * 1.5) * 0.4;",
        "    newUv = clamp(newUv, 0.0, 1.0);",
        "",
        "    float frame = floor(mod(iTime * 10.0, 6.0));",
        "    vec3 bg = vec3(0.10, 0.10, 0.40);",
        "",
        "    float littleWave = cos(uv.x * 20.0 + iTime * 2.0) * 0.03;",
        "    float bigWave = cos(uv.x * 3.0 + iTime * 1.5) * 0.2;",
        "    float waveOffset = littleWave + bigWave;",
        "",
        "    float pixelSize = 1.0 / max(1.0, iResolution.y);",
        "    float cellSize = 8.0 * pixelSize;",
        "    float starUVx = uv.x + pixelSize * float(int(iTime * 500.0));",
        "",
        "    starUVx = floor(starUVx / cellSize) * cellSize;",
        "    uv.x = floor(uv.x / cellSize) * cellSize;",
        "    uv.y = floor(uv.y / cellSize) * cellSize;",
        "",
        "    if (hash(vec2(starUVx, uv.y) * 0.25) > 0.985)",
        "    {",
        "        bg = vec3(0.7, 0.7, 1.0);",
        "    }",
        "",
        "    vec2 rainbowUv = uv;",
        "    rainbowUv.y += waveOffset;",
        "",
        "    float uvRB1 = 0.42;",
        "    float uvRB2 = 0.57;",
        "    float rainbowWidenFactor = 1.0 - rainbowUv.x / 1.5;",
        "    rainbowWidenFactor = pow(rainbowWidenFactor, 2.0);",
        "    uvRB1 -= mix(0.0, 0.3, rainbowWidenFactor);",
        "    uvRB2 += mix(0.0, 0.3, rainbowWidenFactor);",
        "",
        "    if (rainbowUv.x < 1.4 && rainbowUv.y > uvRB1 && rainbowUv.y < uvRB2)",
        "    {",
        "        float t = mapValue(rainbowUv.y, uvRB1, uvRB2, 0.25, 1.2);",
        "        float x = 1.0 / 8.0;",
        "        t = floor(t / x) * x + 0.05;",
        "        bg = pal(t, vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.33, 0.67));",
        "    }",
        "",
        "    vec4 nyan = nyanSprite(newUv, frame);",
        "    vec3 col = mix(bg, nyan.rgb, nyan.a);",
        "    gl_FragColor = vec4(col, 1.0) * vColor;",
        "}"
    };
}
