package dwgx.ui.ext;

/**
 * Built-in CRT terminal shader adapted from user supplied Shadertoy-style code.
 *
 * This keeps "External GLSL" usable even when no local file path is configured.
 */
public final class BuiltinMainMenuGlslShader
{
    private BuiltinMainMenuGlslShader()
    {
    }

    public static String source()
    {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("#version 120\n");
        sb.append("uniform sampler2D uTex;\n");
        sb.append("uniform float uTime;\n");
        sb.append("uniform vec2 uResolution;\n");
        sb.append("varying vec2 vTexCoord;\n");
        sb.append("varying vec4 vColor;\n");
        sb.append("#define iTime uTime\n");
        sb.append("#define iResolution vec3(uResolution, 1.0)\n");
        sb.append("#define iChannel0 uTex\n");
        sb.append("#define WIDTH 0.48\n");
        sb.append("#define HEIGHT 0.3\n");
        sb.append("#define CURVE 3.0\n");
        sb.append("#define SMOOTH 0.004\n");
        sb.append("#define SHINE 0.66\n");
        sb.append("#define BEZEL_COL vec4(0.8, 0.8, 0.6, 0.0)\n");
        sb.append("#define PHOSPHOR_COL vec4(0.2, 1.0, 0.2, 0.0)\n");
        sb.append("#define REFLECTION_BLUR_ITERATIONS 5\n");
        sb.append("#define REFLECTION_BLUR_SIZE 0.04\n");
        sb.append("#define FONT_SIZE vec2(10.0,20.0)\n");
        sb.append("#define ROWCOLS vec2(80.0,24.0)\n");
        sb.append("\n");
        sb.append("float rand(vec2 co){ return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453); }\n");
        sb.append("\n");
        sb.append("float roundSquare(vec2 p, vec2 b, float r){ return length(max(abs(p)-b,0.0))-r; }\n");
        sb.append("float stdRS(vec2 uv, float r){ return roundSquare(uv - 0.5, vec2(WIDTH, HEIGHT) + r, 0.05); }\n");
        sb.append("vec2 CurvedSurface(vec2 uv, float r){ return r * uv / sqrt(max(0.001, r * r - dot(uv, uv))); }\n");
        sb.append("\n");
        sb.append("vec2 crtCurve(vec2 uv, float r, bool content){\n");
        sb.append("    uv = (uv / uResolution.xy - 0.5) / vec2(uResolution.y/uResolution.x, 1.0) * 2.0;\n");
        sb.append("    uv = CurvedSurface(uv, CURVE * r);\n");
        sb.append("    if(content){ uv *= 0.5 / vec2(WIDTH, HEIGHT); }\n");
        sb.append("    return uv * 0.5 + 0.5;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("vec2 borderReflect(vec2 p, float r){\n");
        sb.append("    float eps = 0.0001;\n");
        sb.append("    vec2 epsx = vec2(eps,0.0);\n");
        sb.append("    vec2 epsy = vec2(0.0,eps);\n");
        sb.append("    vec2 b = (1.0 + vec2(r)) * 0.5;\n");
        sb.append("    r /= 3.0;\n");
        sb.append("    p -= 0.5;\n");
        sb.append("    vec2 n = vec2(roundSquare(p-epsx,b,r)-roundSquare(p+epsx,b,r), roundSquare(p-epsy,b,r)-roundSquare(p+epsy,b,r)) / eps;\n");
        sb.append("    float d = roundSquare(p, b, r);\n");
        sb.append("    return p + 0.5 + d * n;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("float somePlasma(vec2 uv){\n");
        sb.append("    uv /= iResolution.xy;\n");
        sb.append("    uv *= ROWCOLS;\n");
        sb.append("    uv = ceil(uv) / ROWCOLS;\n");
        sb.append("    float color = 0.0;\n");
        sb.append("    color += 0.7*sin(0.5*uv.x + iTime/5.0);\n");
        sb.append("    color += 3.0*sin(1.6*uv.y + iTime/5.0);\n");
        sb.append("    color += 1.0*sin(10.0*(uv.y*sin(iTime/2.0) + uv.x*cos(iTime/5.0)) + iTime/2.0);\n");
        sb.append("    float cx = uv.x + 0.5*sin(iTime/2.0);\n");
        sb.append("    float cy = uv.y + 0.5*cos(iTime/4.0);\n");
        sb.append("    color += 0.4*sin(sqrt(100.0*cx*cx + 100.0*cy*cy + 1.0) + iTime);\n");
        sb.append("    color += 0.9*sin(sqrt(75.0*cx*cx + 25.0*cy*cy + 1.0) + iTime);\n");
        sb.append("    color += -1.4*sin(sqrt(256.0*cx*cx + 25.0*cy*cy + 1.0) + iTime);\n");
        sb.append("    color += 0.3*sin(0.5*uv.y + uv.x + sin(iTime));\n");
        sb.append("    return 17.0*(0.5 + 0.499*sin(color))*(0.7 + sin(iTime)*0.3);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("float textLines(vec2 uvG){\n");
        sb.append("    float wt = 5.0*(iTime + 0.5*sin(iTime*1.4) + 0.2*sin(iTime*2.9));\n");
        sb.append("    vec2 uvGt = uvG + vec2(0.0, floor(wt));\n");
        sb.append("    float ll = rand(vec2(uvGt.y, -1.0)) * ROWCOLS.x;\n");
        sb.append("    if (uvG.y > ROWCOLS.y - 2.0){\n");
        sb.append("        float p = floor(min(ll, fract(wt)*ROWCOLS.x));\n");
        sb.append("        if (ceil(uvG.x) == p) return 2.0;\n");
        sb.append("        if (ceil(uvG.x) > p) return 0.0;\n");
        sb.append("    }\n");
        sb.append("    if (uvGt.x > 5.0 && rand(uvGt) < 0.075) return 0.0;\n");
        sb.append("    if (max(5.0, uvGt.x) > ll) return 0.0;\n");
        sb.append("    return rand(uvGt)*15.0 + 2.0;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("float roundLine(vec2 p, vec2 a, vec2 b){\n");
        sb.append("    b -= a + vec2(1.0,0.0);\n");
        sb.append("    p -= a;\n");
        sb.append("    float f = length(p - clamp(dot(p,b)/max(0.0001, dot(b,b)), 0.0, 1.0)*b);\n");
        sb.append("    if (uResolution.y < 320.0) return smoothstep(1.0, 0.9, f);\n");
        sb.append("    if (uResolution.y < 720.0) return smoothstep(0.75, 0.5, f);\n");
        sb.append("    return smoothstep(1.0, 0.0, f);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("float vt220Font(vec2 p, float c){\n");
        sb.append("    if (c < 1.0) return 0.0;\n");
        sb.append("    float col = floor(mod(c, 16.0));\n");
        sb.append("    if (p.y < 2.0 || p.y > 18.0) return 0.0;\n");
        sb.append("    float s = 0.0;\n");
        sb.append("    s += roundLine(p, vec2(2.0 + mod(col, 3.0), 3.0), vec2(8.0, 3.0));\n");
        sb.append("    s += roundLine(p, vec2(1.0, 7.0 + mod(col, 5.0)), vec2(8.0, 7.0 + mod(col, 5.0)));\n");
        sb.append("    s += roundLine(p, vec2(2.0, 11.0), vec2(9.0 - mod(col, 2.0), 11.0));\n");
        sb.append("    s += roundLine(p, vec2(2.0 + mod(col, 2.0), 15.0), vec2(8.0, 15.0));\n");
        sb.append("    return clamp(s, 0.0, 1.0);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("vec4 renderBuffer(vec2 fragCoord){\n");
        sb.append("    vec2 uv = vec2(fragCoord.x, iResolution.y - fragCoord.y);\n");
        sb.append("    vec2 uvT = vec2(80.0,24.0) * FONT_SIZE * uv / iResolution.xy;\n");
        sb.append("    vec2 uvG = floor(ROWCOLS * uv / iResolution.xy);\n");
        sb.append("    float val;\n");
        sb.append("    float prog = sin(iTime*0.5);\n");
        sb.append("    if (prog < -0.1) val = somePlasma(fragCoord);\n");
        sb.append("    else if (prog < 0.1) val = rand(uvG * iTime) * 17.0;\n");
        sb.append("    else val = textLines(uvG);\n");
        sb.append("    return vt220Font(uvT - uvG * FONT_SIZE, val) * PHOSPHOR_COL;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("void main(){\n");
        sb.append("    vec2 fc = gl_FragCoord.xy;\n");
        sb.append("    vec2 uvC = crtCurve(fc, 1.0, true);\n");
        sb.append("    vec2 uvS = crtCurve(fc, 1.0, false);\n");
        sb.append("    vec2 uvE = crtCurve(fc, 1.25, false);\n");
        sb.append("    bool lightsOn = sin(fract(iTime/23.0)+2.74) + 0.05*abs(sin(iTime*1000.0)) < 0.0;\n");
        sb.append("    vec4 c = vec4(0.0);\n");
        sb.append("    vec4 src = renderBuffer(fc);\n");
        sb.append("    if (lightsOn){\n");
        sb.append("        c += max(0.0, SHINE - distance(uvS, vec2(0.5, 1.0))) * smoothstep(SMOOTH/2.0, -SMOOTH/2.0, stdRS(uvS + vec2(0.0,0.03), 0.0));\n");
        sb.append("        c += max(0.0, 0.33 - 0.5*distance(uvS, vec2(0.5, 0.5))) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvS, 0.0));\n");
        sb.append("        vec4 b = BEZEL_COL;\n");
        sb.append("        c += b * smoothstep(-SMOOTH, SMOOTH, roundSquare(uvE-vec2(0.5), vec2(WIDTH, HEIGHT) + 0.05, 0.05)) * smoothstep(SMOOTH, -SMOOTH, roundSquare(uvE-vec2(0.5), vec2(WIDTH, HEIGHT) + 0.15, 0.05));\n");
        sb.append("    }else{\n");
        sb.append("        c += max(0.0, 0.2 - 0.3*distance(uvS, vec2(0.5, 0.5))) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvS, 0.0));\n");
        sb.append("        c += BEZEL_COL * 0.2 * smoothstep(-SMOOTH, SMOOTH, stdRS(uvE, 0.05)) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.15));\n");
        sb.append("        for (int i = 0; i < REFLECTION_BLUR_ITERATIONS; i++){\n");
        sb.append("            vec2 uvR = borderReflect(uvC + (vec2(rand(uvC+float(i)), rand(uvC+float(i)+0.1))-0.5)*REFLECTION_BLUR_SIZE, 0.05);\n");
        sb.append("            c += (PHOSPHOR_COL - BEZEL_COL*0.2) * texture2D(iChannel0, uvR) / float(REFLECTION_BLUR_ITERATIONS) * smoothstep(-SMOOTH, SMOOTH, stdRS(uvS, 0.0)) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.05));\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    if (uvC.x > 0.0 && uvC.x < 1.0 && uvC.y > 0.0 && uvC.y < 1.0){\n");
        sb.append("        c += src + texture2D(iChannel0, uvC) * 0.08;\n");
        sb.append("    }\n");
        sb.append("    gl_FragColor = c * vColor;\n");
        sb.append("}\n");
        return sb.toString();
    }
}
