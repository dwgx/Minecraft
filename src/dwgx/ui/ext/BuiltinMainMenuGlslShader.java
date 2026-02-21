package dwgx.ui.ext;

/**
 * Built-in CRT terminal shader converted from user supplied Shadertoy-style source.
 */
public final class BuiltinMainMenuGlslShader
{
    private static final String SOURCE =
        "#version 120\n" +
        "\n" +
        "uniform sampler2D uTex;\n" +
        "uniform float uTime;\n" +
        "uniform vec2 uResolution;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec4 vColor;\n" +
        "\n" +
        "#define iTime uTime\n" +
        "#define iResolution vec3(uResolution, 1.0)\n" +
        "#define iMouse vec4(0.0)\n" +
        "#define iChannel0 uTex\n" +
        "\n" +
        "#define LIGHTS_ON (sin(fract(iTime/23.0)+2.74) + 0.05*abs(sin(iTime*1000.0)) < 0.0)\n" +
        "#define WIDTH 0.48\n" +
        "#define HEIGHT 0.3\n" +
        "#define CURVE 3.0\n" +
        "#define SMOOTH 0.004\n" +
        "#define SHINE 0.66\n" +
        "#define BEZEL_COL vec4(0.8, 0.8, 0.6, 0.0)\n" +
        "#define REFLECTION_BLUR_ITERATIONS 5\n" +
        "#define REFLECTION_BLUR_SIZE 0.04\n" +
        "\n" +
        "#define FONT_SIZE vec2(10.0,20.0)\n" +
        "#define ROWCOLS vec2(80.0, 24.0)\n" +
        "#define PHOSPHOR_COL vec4(0.2, 1.0, 0.2, 0.0)\n" +
        "\n" +
        "float rand(vec2 co)\n" +
        "{\n" +
        "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "vec2 CurvedSurface(vec2 uv, float r)\n" +
        "{\n" +
        "    return r * uv / sqrt(max(0.0001, r * r - dot(uv, uv)));\n" +
        "}\n" +
        "\n" +
        "vec2 crtCurve(vec2 uv, float r, bool content, bool shine)\n" +
        "{\n" +
        "    r = CURVE * r;\n" +
        "    uv = (uv / iResolution.xy - 0.5) / vec2(iResolution.y/iResolution.x, 1.0) * 2.0;\n" +
        "    uv = CurvedSurface(uv, r);\n" +
        "    if(content) uv *= 0.5 / vec2(WIDTH, HEIGHT);\n" +
        "    uv = (uv / 2.0) + 0.5;\n" +
        "    return uv;\n" +
        "}\n" +
        "\n" +
        "float roundSquare(vec2 p, vec2 b, float r)\n" +
        "{\n" +
        "    return length(max(abs(p)-b,0.0))-r;\n" +
        "}\n" +
        "\n" +
        "float stdRS(vec2 uv, float r)\n" +
        "{\n" +
        "    return roundSquare(uv - 0.5, vec2(WIDTH, HEIGHT) + r, 0.05);\n" +
        "}\n" +
        "\n" +
        "vec2 borderReflect(vec2 p, float r)\n" +
        "{\n" +
        "    float eps = 0.0001;\n" +
        "    vec2 epsx = vec2(eps,0.0);\n" +
        "    vec2 epsy = vec2(0.0,eps);\n" +
        "    vec2 b = (1.0+vec2(r,r))* 0.5;\n" +
        "    r /= 3.0;\n" +
        "    p -= 0.5;\n" +
        "    vec2 normal = vec2(roundSquare(p-epsx,b,r)-roundSquare(p+epsx,b,r),\n" +
        "                       roundSquare(p-epsy,b,r)-roundSquare(p+epsy,b,r))/eps;\n" +
        "    float d = roundSquare(p, b, r);\n" +
        "    p += 0.5;\n" +
        "    return p + d*normal;\n" +
        "}\n" +
        "\n" +
        "float somePlasma(vec2 uv)\n" +
        "{\n" +
        "    uv /= iResolution.xy;\n" +
        "    uv *= ROWCOLS;\n" +
        "    uv = ceil(uv);\n" +
        "    uv /= ROWCOLS;\n" +
        "\n" +
        "    float color = 0.0;\n" +
        "    color += 0.7*sin(0.5*uv.x + iTime/5.0);\n" +
        "    color += 3.0*sin(1.6*uv.y + iTime/5.0);\n" +
        "    color += 1.0*sin(10.0*(uv.y * sin(iTime/2.0) + uv.x * cos(iTime/5.0)) + iTime/2.0);\n" +
        "    float cx = uv.x + 0.5*sin(iTime/2.0);\n" +
        "    float cy = uv.y + 0.5*cos(iTime/4.0);\n" +
        "    color += 0.4*sin(sqrt(100.0*cx*cx + 100.0*cy*cy + 1.0) + iTime);\n" +
        "    color += 0.9*sin(sqrt(75.0*cx*cx + 25.0*cy*cy + 1.0) + iTime);\n" +
        "    color += -1.4*sin(sqrt(256.0*cx*cx + 25.0*cy*cy + 1.0) + iTime);\n" +
        "    color += 0.3 * sin(0.5*uv.y + uv.x + sin(iTime));\n" +
        "    return 17.0*(0.5+0.499*sin(color))*(0.7+sin(iTime)*0.3);\n" +
        "}\n" +
        "\n" +
        "float textLines(vec2 uvG)\n" +
        "{\n" +
        "    float wt = 5.0 * (iTime + 0.5*sin(iTime*1.4) + 0.2*sin(iTime*2.9));\n" +
        "    vec2 uvGt = uvG + vec2(0.0, floor(wt));\n" +
        "    float ll = rand(vec2(uvGt.y, -1.0)) * ROWCOLS.x;\n" +
        "\n" +
        "    if (uvG.y > ROWCOLS.y - 2.0){\n" +
        "        if (ceil(uvG.x) == floor(min(ll, fract(wt)*ROWCOLS.x)))\n" +
        "            return 2.0;\n" +
        "        if (ceil(uvG.x) > floor(min(ll, fract(wt)*ROWCOLS.x)))\n" +
        "            return 0.0;\n" +
        "    }\n" +
        "    if (uvGt.x > 5.0 && rand(uvGt) < 0.075)\n" +
        "        return 0.0;\n" +
        "    if (max(5.0, uvGt.x) > ll)\n" +
        "        return 0.0;\n" +
        "\n" +
        "    return rand(uvGt)*15.0 + 2.0;\n" +
        "}\n" +
        "\n" +
        "float roundLine(vec2 p, vec2 a, vec2 b)\n" +
        "{\n" +
        "    b -= a + vec2(1.0,0.0);\n" +
        "    p -= a;\n" +
        "    float f = length(p-clamp(dot(p,b)/max(0.0001,dot(b,b)),0.0,1.0)*b);\n" +
        "    if (iResolution.y < 320.0)\n" +
        "        return smoothstep(1.0, 0.9, f);\n" +
        "    else if (iResolution.y < 720.0)\n" +
        "        return smoothstep(0.75, 0.5, f);\n" +
        "    else\n" +
        "        return smoothstep(1.0, 0.0, f);\n" +
        "}\n" +
        "\n" +
        "#define l(y,a,b) roundLine(p, vec2(float(a), float(y)), vec2(float(b), float(y)))\n" +
        "\n" +
        "float vt220Font(vec2 p, float c)\n" +
        "{\n" +
        "    if (c < 1.0) return 0.0;\n" +
        "    if(p.y > 16.0){\n" +
        "        if(c > 2.0) return 0.0;\n" +
        "        if(c > 1.0) return l(17,1,9);\n" +
        "    }\n" +
        "    if(p.y > 14.0){\n" +
        "        if(c > 16.0) return l(15,3,8);\n" +
        "        if(c > 15.0) return l(15,1,8);\n" +
        "        if(c > 14.0) return l(15,1,3)+ l(15,7,9);\n" +
        "        if(c > 13.0) return l(15,2,8);\n" +
        "        if(c > 12.0) return l(15,1,9);\n" +
        "        if(c > 11.0) return l(15,2,8);\n" +
        "        if(c > 10.0) return l(15,1,3)+ l(15,6,8);\n" +
        "        if(c > 9.0) return l(15,4,6);\n" +
        "        if(c > 8.0) return l(15,2,4)+ l(15,5,7);\n" +
        "        if(c > 7.0) return l(15,2,8);\n" +
        "        if(c > 6.0) return l(15,2,8);\n" +
        "        if(c > 5.0) return l(15,2,8);\n" +
        "        if(c > 4.0) return l(15,2,9);\n" +
        "        if(c > 3.0) return l(15,1,8);\n" +
        "        if(c > 2.0) return l(15,2,9);\n" +
        "    }\n" +
        "    if(p.y > 12.0){\n" +
        "        if(c > 16.0) return l(13,2,4)+ l(13,7,9);\n" +
        "        if(c > 15.0) return l(13,2,4)+ l(13,7,9);\n" +
        "        if(c > 14.0) return l(13,1,3)+ l(13,7,9);\n" +
        "        if(c > 13.0) return l(13,1,3)+ l(13,7,9);\n" +
        "        if(c > 12.0) return l(13,1,3);\n" +
        "        if(c > 11.0) return l(13,4,6);\n" +
        "        if(c > 10.0) return l(13,2,4)+ l(13,5,9);\n" +
        "        if(c > 9.0) return l(13,2,8);\n" +
        "        if(c > 8.0) return l(13,2,4)+ l(13,5,7);\n" +
        "        if(c > 7.0) return l(13,1,3)+ l(13,7,9);\n" +
        "        if(c > 6.0) return l(13,1,3)+ l(13,7,9);\n" +
        "        if(c > 5.0) return l(13,1,3)+ l(13,7,9);\n" +
        "        if(c > 4.0) return l(13,1,3)+ l(15,2,9);\n" +
        "        if(c > 3.0) return l(13,1,4)+ l(13,7,9);\n" +
        "        if(c > 2.0) return l(13,1,3)+ l(13,6,9);\n" +
        "    }\n" +
        "    if(p.y > 10.0){\n" +
        "        if(c > 16.0) return l(11,1,3);\n" +
        "        if(c > 15.0) return l(11,2,4)+ l(11,7,9);\n" +
        "        if(c > 14.0) return l(11,1,9);\n" +
        "        if(c > 13.0) return l(11,7,9);\n" +
        "        if(c > 12.0) return l(11,2,5);\n" +
        "        if(c > 11.0) return l(11,4,6);\n" +
        "        if(c > 10.0) return l(11,3,5)+ l(11,6,8);\n" +
        "        if(c > 9.0) return l(11,4,6)+ l(11,7,9);\n" +
        "        if(c > 8.0) return l(11,1,8);\n" +
        "        if(c > 7.0) return l(11,1,3)+ l(11,7,9);\n" +
        "        if(c > 6.0) return l(11,1,3)+ l(11,7,9);\n" +
        "        if(c > 5.0) return l(11,1,3)+ l(11,7,9);\n" +
        "        if(c > 4.0) return l(11,1,3);\n" +
        "        if(c > 3.0) return l(11,1,3)+ l(11,7,9);\n" +
        "        if(c > 2.0) return l(11,2,9);\n" +
        "    }\n" +
        "    if(p.y > 8.0){\n" +
        "        if(c > 16.0) return l(9,1,3);\n" +
        "        if(c > 15.0) return l(9,2,8);\n" +
        "        if(c > 14.0) return l(9,1,3)+ l(9,7,9);\n" +
        "        if(c > 13.0) return l(9,4,8);\n" +
        "        if(c > 12.0) return l(9,4,8);\n" +
        "        if(c > 11.0) return l(9,4,6);\n" +
        "        if(c > 10.0) return l(9,4,6);\n" +
        "        if(c > 9.0) return l(9,2,8);\n" +
        "        if(c > 8.0) return l(9,2,4)+ l(9,5,7);\n" +
        "        if(c > 7.0) return l(9,1,3)+ l(9,7,9);\n" +
        "        if(c > 6.0) return l(9,1,3)+ l(9,7,9);\n" +
        "        if(c > 5.0) return l(9,1,3)+ l(9,7,9);\n" +
        "        if(c > 4.0) return l(9,1,3)+ l(9,7,9);\n" +
        "        if(c > 3.0) return l(9,1,4)+ l(9,7,9);\n" +
        "        if(c > 2.0) return l(9,7,9);\n" +
        "    }\n" +
        "    if(p.y > 6.0){\n" +
        "        if(c > 16.0) return l(7,1,3);\n" +
        "        if(c > 15.0) return l(7,2,4)+ l(7,7,9);\n" +
        "        if(c > 14.0) return l(7,2,4)+ l(7,6,8);\n" +
        "        if(c > 13.0) return l(7,5,7);\n" +
        "        if(c > 12.0) return l(7,7,9);\n" +
        "        if(c > 11.0) return l(7,2,6);\n" +
        "        if(c > 10.0) return l(7,2,4)+ l(7,5,7);\n" +
        "        if(c > 9.0) return l(7,1,3)+ l(7,4,6);\n" +
        "        if(c > 8.0) return l(7,1,8);\n" +
        "        if(c > 7.0) return l(7,2,8);\n" +
        "        if(c > 6.0) return l(7,2,8);\n" +
        "        if(c > 5.0) return l(7,2,8);\n" +
        "        if(c > 4.0) return l(7,2,8);\n" +
        "        if(c > 3.0) return l(7,1,8);\n" +
        "        if(c > 2.0) return l(7,2,8);\n" +
        "    }\n" +
        "    if(p.y > 4.0){\n" +
        "        if(c > 16.0) return l(5,2,4)+ l(5,7,9);\n" +
        "        if(c > 15.0) return l(5,2,4)+ l(5,7,9);\n" +
        "        if(c > 14.0) return l(5,3,7);\n" +
        "        if(c > 13.0) return l(5,6,8);\n" +
        "        if(c > 12.0) return l(5,1,3)+ l(5,7,9);\n" +
        "        if(c > 11.0) return l(5,3,6);\n" +
        "        if(c > 10.0) return l(5,1,5)+ l(5,6,8);\n" +
        "        if(c > 9.0) return l(5,2,8);\n" +
        "        if(c > 8.0) return l(5,2,4)+ l(5,5,7);\n" +
        "        if(c > 7.0) return 0.0;\n" +
        "        if(c > 6.0) return 0.0;\n" +
        "        if(c > 5.0) return 0.0;\n" +
        "        if(c > 4.0) return 0.0;\n" +
        "        if(c > 3.0) return l(5,1,3);\n" +
        "        if(c > 2.0) return 0.0;\n" +
        "    }\n" +
        "    if(p.y > 2.0){\n" +
        "        if(c > 16.0) return l(3,3,8);\n" +
        "        if(c > 15.0) return l(3,1,8);\n" +
        "        if(c > 14.0) return l(3,4,6);\n" +
        "        if(c > 13.0) return l(3,1,9);\n" +
        "        if(c > 12.0) return l(3,2,8);\n" +
        "        if(c > 11.0) return l(3,4,6);\n" +
        "        if(c > 10.0) return l(3,2,4)+ l(3,7,9);\n" +
        "        if(c > 9.0) return l(3,4,6);\n" +
        "        if(c > 8.0) return l(3,2,4)+ l(3,5,7);\n" +
        "        if(c > 7.0) return l(3,2,4)+ l(3,6,8);\n" +
        "        if(c > 6.0) return l(3,1,3)+ l(3,4,7);\n" +
        "        if(c > 5.0) return l(3,2,4)+ l(3,6,8);\n" +
        "        if(c > 4.0) return 0.0;\n" +
        "        if(c > 3.0) return l(3,1,3);\n" +
        "        if(c > 2.0) return 0.0;\n" +
        "    }\n" +
        "    else{\n" +
        "        if(c > 7.0) return 0.0;\n" +
        "        if(c > 6.0) return l(1,2,5)+ l(1,6,8);\n" +
        "    }\n" +
        "    return 0.0;\n" +
        "}\n" +
        "\n" +
        "vec4 renderBuffer(vec2 fragCoord)\n" +
        "{\n" +
        "    float val = 0.0;\n" +
        "\n" +
        "    vec2 uv = vec2(fragCoord.x, iResolution.y - fragCoord.y);\n" +
        "    vec2 uvT = vec2(80.0, 24.0) * FONT_SIZE * uv / iResolution.xy;\n" +
        "    vec2 uvG = floor(ROWCOLS * uv / iResolution.xy);\n" +
        "\n" +
        "    float prog = sin(iTime*0.5);\n" +
        "    if(prog < -0.1)\n" +
        "        val = somePlasma(fragCoord.xy);\n" +
        "    else if(prog < 0.1)\n" +
        "        val = rand(uvG * iTime) * 17.0;\n" +
        "    else\n" +
        "        val = textLines(uvG);\n" +
        "\n" +
        "    return vt220Font(uvT - uvG * FONT_SIZE, val) * PHOSPHOR_COL;\n" +
        "}\n" +
        "\n" +
        "void mainImage(out vec4 c, in vec2 fragCoord)\n" +
        "{\n" +
        "    c = vec4(0.0, 0.0, 0.0, 0.0);\n" +
        "\n" +
        "    vec2 uvC = crtCurve(fragCoord, 1.0, true, false);\n" +
        "    vec2 uvS = crtCurve(fragCoord, 1.0, false, false);\n" +
        "    vec2 uvE = crtCurve(fragCoord, 1.25, false, false);\n" +
        "\n" +
        "    if (LIGHTS_ON) {\n" +
        "        const float ambient = 0.33;\n" +
        "        vec2 uvSh = crtCurve(fragCoord, 1.0, false, true);\n" +
        "        c += max(0.0, SHINE - distance(uvSh, vec2(0.5, 1.0))) * smoothstep(SMOOTH/2.0, -SMOOTH/2.0, stdRS(uvS + vec2(0.0, 0.03), 0.0));\n" +
        "        c += max(0.0, ambient - 0.5*distance(uvS, vec2(0.5,0.5))) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvS, 0.0));\n" +
        "\n" +
        "        uvSh = crtCurve(fragCoord, 1.25, false, true);\n" +
        "        vec4 b = vec4(0.0, 0.0, 0.0, 0.0);\n" +
        "        for(int i=0; i<12; i++) {\n" +
        "            b += (clamp(BEZEL_COL + rand(uvSh+float(i))*0.05-0.025, 0.0, 1.0) + rand(uvE+1.0+float(i))*0.25 * cos((uvSh.x-0.5)*3.1415*1.5))/12.0;\n" +
        "        }\n" +
        "\n" +
        "        const float HHW = 0.5 * HEIGHT/WIDTH;\n" +
        "        c += b/3.0*(1.0 + smoothstep(HHW - 0.025, HHW + 0.025, abs(atan(uvS.x-0.5, uvS.y-0.5))/3.1415)\n" +
        "            + smoothstep(HHW + 0.025, HHW - 0.025, abs(atan(uvS.x-0.5, 0.5-uvS.y))/3.1415))\n" +
        "            * smoothstep(-SMOOTH, SMOOTH, stdRS(uvS, 0.0))\n" +
        "            * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.05));\n" +
        "\n" +
        "        c += (b - 0.4)\n" +
        "            * smoothstep(-SMOOTH*2.0, SMOOTH*2.0, roundSquare(uvE-vec2(0.5, 0.505), vec2(WIDTH, HEIGHT) + 0.05, 0.05))\n" +
        "            * smoothstep(SMOOTH*2.0, -SMOOTH*2.0, roundSquare(uvE-vec2(0.5, 0.495), vec2(WIDTH, HEIGHT) + 0.05, 0.05));\n" +
        "\n" +
        "        c += b\n" +
        "            * smoothstep(-SMOOTH, SMOOTH, roundSquare(uvE-vec2(0.5, 0.5), vec2(WIDTH, HEIGHT) + 0.05, 0.05))\n" +
        "            * smoothstep(SMOOTH, -SMOOTH, roundSquare(uvE-vec2(0.5, 0.5), vec2(WIDTH, HEIGHT) + 0.15, 0.05));\n" +
        "\n" +
        "        c += (b - 0.4)\n" +
        "            * smoothstep(-SMOOTH*2.0, SMOOTH*2.0, roundSquare(uvE-vec2(0.5, 0.495), vec2(WIDTH, HEIGHT) + 0.15, 0.05))\n" +
        "            * smoothstep(SMOOTH*2.0, -SMOOTH*2.0, roundSquare(uvE-vec2(0.5, 0.505), vec2(WIDTH, HEIGHT) + 0.15, 0.05));\n" +
        "\n" +
        "        c += max(0.0, (1.0 - 2.0* fragCoord.y/iResolution.y)) * vec4(1.0, 1.0, 1.0, 0.0)\n" +
        "            * smoothstep(-0.25, 0.25, roundSquare(uvC - vec2(0.5, -0.2), vec2(WIDTH+0.25, HEIGHT-0.15), 0.1))\n" +
        "            * smoothstep(-SMOOTH*2.0, SMOOTH*2.0, roundSquare(uvE-vec2(0.5, 0.5), vec2(WIDTH, HEIGHT) + 0.15, 0.05));\n" +
        "    }\n" +
        "    else {\n" +
        "        const float ambient = 0.2;\n" +
        "        c += max(0.0, ambient - 0.3*distance(uvS, vec2(0.5,0.5))) * smoothstep(SMOOTH, -SMOOTH, stdRS(uvS, 0.0));\n" +
        "\n" +
        "        c += BEZEL_COL * ambient * 0.7\n" +
        "            * smoothstep(-SMOOTH, SMOOTH, stdRS(uvS, 0.0))\n" +
        "            * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.05));\n" +
        "\n" +
        "        c -= BEZEL_COL\n" +
        "            * smoothstep(-SMOOTH*2.0, SMOOTH*10.0, stdRS(uvE, 0.05))\n" +
        "            * smoothstep(SMOOTH*2.0, -SMOOTH*2.0, stdRS(uvE, 0.05));\n" +
        "\n" +
        "        c += BEZEL_COL * ambient\n" +
        "            * smoothstep(-SMOOTH, SMOOTH, stdRS(uvE, 0.05))\n" +
        "            * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.15));\n" +
        "\n" +
        "        for(int i = 0; i < REFLECTION_BLUR_ITERATIONS; i++)\n" +
        "        {\n" +
        "            vec2 uvR = borderReflect(uvC + (vec2(rand(uvC+float(i)), rand(uvC+float(i)+0.1))-0.5)*REFLECTION_BLUR_SIZE, 0.05);\n" +
        "            c += (PHOSPHOR_COL - BEZEL_COL*ambient) * texture2D(iChannel0, uvR) / float(REFLECTION_BLUR_ITERATIONS)\n" +
        "                * smoothstep(-SMOOTH, SMOOTH, stdRS(uvS, 0.0))\n" +
        "                * smoothstep(SMOOTH, -SMOOTH, stdRS(uvE, 0.05));\n" +
        "        }\n" +
        "\n" +
        "        c += (texture2D(iChannel0, uvC) + texture2D(iChannel0, uvC + vec2(0.003, 0.003)) + texture2D(iChannel0, uvC - vec2(0.003, 0.003)))\n" +
        "            * smoothstep(0.0, -SMOOTH*20.0, stdRS(uvS, -0.02)) * 0.5;\n" +
        "    }\n" +
        "\n" +
        "    if (uvC.x > 0.0 && uvC.x < 1.0 && uvC.y > 0.0 && uvC.y < 1.0)\n" +
        "        c += texture2D(iChannel0, uvC) + renderBuffer(fragCoord);\n" +
        "}\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "    vec4 c;\n" +
        "    mainImage(c, gl_FragCoord.xy);\n" +
        "    gl_FragColor = c * vColor;\n" +
        "}\n";

    private BuiltinMainMenuGlslShader()
    {
    }

    public static String source()
    {
        return SOURCE;
    }
}

