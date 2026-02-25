package dwgx.ui.ext;

import client.runtime.lwjgl.GlfwWindow;

/**
 * FlappyBird 风格主菜单背景场景。
 * 负责输入、状态推进、碰撞检测和提示文案。
 */
public final class FlappyBirdBackgroundScene implements MainMenuBackgroundScene
{
    private static final float PIPE_WIDTH = 26.0F;
    private static final float PIPE_BOTTOM = 39.0F;
    private static final float VERTICAL_PIPE_GAP = 55.0F;
    private static final float PIPE_MIN_HEIGHT = 20.0F;
    private static final float PIPE_MAX_HEIGHT = 70.0F;
    private static final float PIPE_GROUP_SIZE = 8.0F;
    private static final float PIPE_SPACING = 100.0F;
    private static final float BIRD_X = 105.0F;
    private static final float BIRD_WIDTH = 14.0F;
    private static final float BIRD_HEIGHT = 12.0F;
    private static final float BIRD_START_Y = 110.0F;
    private static final float GRAVITY = -180.0F;
    private static final float FLAP_VELOCITY = 78.0F;
    private static final int LWJGL_KEY_SPACE = 57;

    private float gameTick;
    private float birdY = BIRD_START_Y;
    private float birdVelocity;
    private float wingFrame;
    private boolean birdAlive = true;
    private int score;
    private int bestScore;
    private long lastUpdateMs;
    private boolean jumpQueued;

    public String fragmentShaderSource()
    {
        return FlappyBirdFragmentShaderTemplate.source();
    }

    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA;
    }

    public void initialize()
    {
        if (this.lastUpdateMs == 0L)
        {
            this.resetGame();
        }
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return false;
        }

        if (typedChar == ' ' || keyCode == LWJGL_KEY_SPACE)
        {
            this.jumpQueued = true;
            return true;
        }

        return false;
    }

    public void tick(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            this.lastUpdateMs = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();

        if (this.lastUpdateMs == 0L)
        {
            this.lastUpdateMs = now;
            return;
        }

        float deltaSeconds = (float)(now - this.lastUpdateMs) / 1000.0F;
        this.lastUpdateMs = now;

        if (deltaSeconds <= 0.0F)
        {
            return;
        }

        if (deltaSeconds > 0.05F)
        {
            deltaSeconds = 0.05F;
        }

        this.wingFrame += deltaSeconds * 12.0F;

        if (this.jumpQueued)
        {
            this.jumpQueued = false;

            if (!this.birdAlive)
            {
                this.resetGame();
                return;
            }

            this.birdVelocity = FLAP_VELOCITY;
        }

        if (!this.birdAlive)
        {
            return;
        }

        this.gameTick += deltaSeconds * 60.0F;
        this.birdVelocity += GRAVITY * deltaSeconds;
        this.birdY += this.birdVelocity * deltaSeconds;

        float levelHeight = this.resolveLevelHeight();

        if (this.birdY <= PIPE_BOTTOM)
        {
            this.birdY = PIPE_BOTTOM;
            this.markGameOver();
            return;
        }

        if (this.birdY + BIRD_HEIGHT >= levelHeight - 1.0F)
        {
            this.birdY = levelHeight - BIRD_HEIGHT - 1.0F;
            this.markGameOver();
            return;
        }

        if (this.hasPipeCollision())
        {
            this.markGameOver();
            return;
        }

        this.score = Math.max(0, (int)Math.floor((this.gameTick + BIRD_X) / PIPE_SPACING));
        this.bestScore = Math.max(this.bestScore, this.score);
    }

    public void applyShaderUniforms(MainMenuSplashShader shader)
    {
        if (shader == null)
        {
            return;
        }

        shader.setFlappyState(this.gameTick, this.birdY, this.wingFrame, this.birdAlive, true);
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return null;
        }

        if (this.birdAlive)
        {
            return "空格跳跃  分数: " + this.score + "  最高: " + this.bestScore;
        }

        return "失败了，按空格重新开始  本次: " + this.score + "  最高: " + this.bestScore;
    }

    private boolean hasPipeCollision()
    {
        float cycleLength = PIPE_SPACING * PIPE_GROUP_SIZE;
        float offset = this.gameTick % cycleLength;
        float pipeX = -offset;
        float birdLeft = BIRD_X;
        float birdRight = BIRD_X + BIRD_WIDTH;
        float birdBottom = this.birdY;
        float birdTop = this.birdY + BIRD_HEIGHT;

        for (int i = 0; i < 12; ++i)
        {
            float pipeLeft = pipeX;
            float pipeRight = pipeX + PIPE_WIDTH;

            if (birdRight >= pipeLeft && birdLeft <= pipeRight)
            {
                float bottomHeight = this.resolveBottomPipeHeight(i);
                float gapBottom = PIPE_BOTTOM + bottomHeight;
                float gapTop = gapBottom + VERTICAL_PIPE_GAP;

                if (birdBottom < gapBottom || birdTop > gapTop)
                {
                    return true;
                }
            }

            pipeX += PIPE_SPACING;
        }

        return false;
    }

    private float resolveBottomPipeHeight(int index)
    {
        float center = (PIPE_MAX_HEIGHT + PIPE_MIN_HEIGHT) / 2.0F;
        float upperMid = (center + PIPE_MAX_HEIGHT) / 2.0F;
        float lowerMid = (center + PIPE_MIN_HEIGHT) / 2.0F;
        int cycle = index % 8;

        if (cycle == 1 || cycle == 3)
        {
            return upperMid;
        }

        if (cycle == 2)
        {
            return PIPE_MAX_HEIGHT;
        }

        if (cycle == 5 || cycle == 7)
        {
            return lowerMid;
        }

        if (cycle == 6)
        {
            return PIPE_MIN_HEIGHT;
        }

        return center;
    }

    private float resolveLevelHeight()
    {
        float levelHeight = (float)Math.max(1, GlfwWindow.getHeight()) / 2.0F;

        if (levelHeight >= 320.0F)
        {
            levelHeight /= 2.0F;
        }

        if (levelHeight < 100.0F)
        {
            levelHeight *= 2.0F;
        }

        return levelHeight;
    }

    private void markGameOver()
    {
        if (!this.birdAlive)
        {
            return;
        }

        this.birdAlive = false;
        this.bestScore = Math.max(this.bestScore, this.score);
    }

    private void resetGame()
    {
        this.gameTick = 0.0F;
        this.birdY = BIRD_START_Y;
        this.birdVelocity = 0.0F;
        this.wingFrame = 0.0F;
        this.birdAlive = true;
        this.score = 0;
        this.jumpQueued = false;
        this.lastUpdateMs = System.currentTimeMillis();
    }
}

/**
 * Embedded FlappyBird fragment shader source.
 */
final class FlappyBirdFragmentShaderTemplate
{
    private static final String[] LINES = new String[]
    {
        "#version 120",
        "uniform sampler2D uTex;",
        "uniform float iTime;",
        "uniform vec2 iResolution;",
        "uniform float uGameTick;",
        "uniform float uBirdY;",
        "uniform float uBirdFrame;",
        "uniform float uBirdAlive;",
        "uniform float uControlEnabled;",
        "uniform float uUseTexAlpha;",
        "varying vec2 vTexCoord;",
        "varying vec4 vColor;",
        "",
        "// FlappyBird by Ben Raziel. Feb 2014",
        "//",
        "// Based on the shader by HLorenzi",
        "// https://www.shadertoy.com/view/Msj3zD",
        "",
        "// Helper functions for drawing sprites",
        "#define RGB(r,g,b) vec4(float(r)/255.0,float(g)/255.0,float(b)/255.0,1.0)",
        "#define SPRROW(x,a,b,c,d,e,f,g,h, i,j,k,l,m,n,o,p) (x <= 7 ? SPRROW_H(a,b,c,d,e,f,g,h) : SPRROW_H(i,j,k,l,m,n,o,p))",
        "#define SPRROW_H(a,b,c,d,e,f,g,h) (a+4.0*(b+4.0*(c+4.0*(d+4.0*(e+4.0*(f+4.0*(g+4.0*(h))))))))",
        "#define SECROW(x,a,b,c,d,e,f,g,h) (x <= 3 ? SECROW_H(a,b,c,d) : SECROW_H(e,f,g,h))",
        "#define SECROW_H(a,b,c,d) (a+8.0*(b+8.0*(c+8.0*(d))))",
        "#define SELECT(x,i) mod(floor(i/pow(4.0,float(x))),4.0)",
        "#define SELECTSEC(x,i) mod(floor(i/pow(8.0,float(x))),8.0)",
        "",
        "// drawing consts",
        "const float PIPE_WIDTH = 26.0; // px",
        "const float PIPE_BOTTOM = 39.0; // px",
        "const float PIPE_HOLE_HEIGHT = 12.0; // px",
        "const vec4 PIPE_OUTLINE_COLOR = RGB(84, 56, 71);",
        "",
        "// gameplay consts",
        "const float HORZ_PIPE_DISTANCE = 100.0; // px;",
        "const float VERT_PIPE_DISTANCE = 55.0; // px;",
        "const float PIPE_MIN = 20.0;",
        "const float PIPE_MAX = 70.0;",
        "const float PIPE_PER_CYCLE = 8.0;",
        "",
        "vec4 fragColor;",
        "",
        "float flappyTick()",
        "{",
        "    return uControlEnabled > 0.5 ? uGameTick : iTime * 60.0;",
        "}",
        "",
        "void drawHorzRect(float yCoord, float minY, float maxY, vec4 color)",
        "{",
        "    if ((yCoord >= minY) && (yCoord < maxY)) {",
        "        fragColor = color;",
        "    }",
        "}",
        "",
        "void drawLowBush(int x, int y)",
        "{",
        "    if (y < 0 || y > 3 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    float col = 0.0; // 0 = transparent",
        "",
        "    if (y ==  3) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,0.,0.,1.,1.,2.,2., 2.,2.,1.,1.,0.,0.,0.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,1.,1.,2.,2.,2., 2.,2.,2.,1.,1.,0.,0.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,1.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,1.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(87,201,111);",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(100,224,117);",
        "    }",
        "}",
        "",
        "void drawHighBush(int x, int y)",
        "{",
        "    if (y < 0 || y > 6 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    float col = 0.0; // 0 = transparent",
        "",
        "    if (y ==  6) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,0.,0.,0.,1.,1.,2.,2., 2.,2.,1.,1.,0.,0.,0.,0.);",
        "    if (y ==  4) col = SPRROW(x,0.,0.,1.,1.,2.,2.,2.,2., 2.,2.,2.,2.,1.,1.,0.,0.);",
        "    if (y ==  3) col = SPRROW(x,0.,1.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,1.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  1) col = SPRROW(x,1.,2.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,2.,1.);",
        "    if (y ==  0) col = SPRROW(x,1.,2.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,2.,1.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(87,201,111);",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(100,224,117);",
        "    }",
        "}",
        "",
        "void drawCloud(int x, int y)",
        "{",
        "    if (y < 0 || y > 6 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    float col = 0.0; // 0 = transparent",
        "",
        "    if (y ==  6) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,0.,0.,0.,1.,1.,2.,2., 2.,2.,1.,1.,0.,0.,0.,0.);",
        "    if (y ==  4) col = SPRROW(x,0.,0.,1.,1.,2.,2.,2.,2., 2.,2.,2.,2.,1.,1.,0.,0.);",
        "    if (y ==  3) col = SPRROW(x,0.,1.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,1.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  1) col = SPRROW(x,1.,2.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,2.,1.);",
        "    if (y ==  0) col = SPRROW(x,1.,2.,2.,2.,2.,2.,2.,2., 2.,2.,2.,2.,2.,2.,2.,1.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(218,246,216);",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(233,251,218);",
        "    }",
        "}",
        "",
        "void drawBirdF0(int x, int y)",
        "{",
        "    if (y < 0 || y > 11 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    // pass 0 - draw black, white and yellow",
        "    float col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,1.,1.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,1.,1.,3.,3., 3.,1.,2.,2.,1.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,1.,3.,3.,3.,3., 1.,2.,2.,2.,2.,1.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,0.,1.,3.,3.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  7) col = SPRROW(x,0.,1.,3.,3.,3.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  6) col = SPRROW(x,0.,1.,3.,3.,3.,3.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,1.,1.,1.,1.,1.,3.,3., 3.,3.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  4) col = SPRROW(x,1.,2.,2.,2.,2.,2.,1.,3., 3.,1.,2.,2.,2.,2.,2.,1.);",
        "    if (y ==  3) col = SPRROW(x,1.,2.,2.,2.,2.,1.,3.,3., 1.,2.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  2) col = SPRROW(x,1.,2.,2.,2.,1.,3.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,1.,1.,1.,1.,3.,3.,3., 3.,3.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,1.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(82,56,70); // outline color (black)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(250,250,250); // eye color (white)",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(247, 182, 67); // normal yellow color",
        "    }",
        "",
        "    // pass 1 - draw red, light yellow and dark yellow",
        "    col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,0.,0.,3.,3., 3.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,0.,3.,3.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,0.,0.,3.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  7) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  6) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  4) col = SPRROW(x,0.,3.,0.,0.,0.,3.,0.,0., 0.,0.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  3) col = SPRROW(x,0.,0.,0.,0.,0.,0.,2.,2., 0.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,0.,3.,0.,2.,2.,2., 2.,0.,1.,1.,1.,1.,0.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,0.,0.,2.,2.,2., 2.,2.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(249, 58, 28); // mouth color (red)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(222, 128, 55); // brown",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(249, 214, 145); // light yellow",
        "    }",
        "}",
        "",
        "void drawBirdF1(int x, int y)",
        "{",
        "    if (y < 0 || y > 11 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    // pass 0 - draw black, white and yellow",
        "    float col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,1.,1.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,1.,1.,3.,3., 3.,1.,2.,2.,1.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,1.,3.,3.,3.,3., 1.,2.,2.,2.,2.,1.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,0.,1.,3.,3.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  7) col = SPRROW(x,0.,1.,3.,3.,3.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  6) col = SPRROW(x,0.,1.,1.,1.,1.,1.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  5) col = SPRROW(x,1.,2.,2.,2.,2.,2.,1.,3., 3.,3.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  4) col = SPRROW(x,1.,2.,2.,2.,2.,2.,1.,3., 3.,1.,2.,2.,2.,2.,2.,1.);",
        "    if (y ==  3) col = SPRROW(x,0.,1.,1.,1.,1.,1.,3.,3., 1.,2.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,1.,3.,3.,3.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,1.,1.,3.,3.,3., 3.,3.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,1.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(82,56,70); // outline color (black)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(250,250,250); // eye color (white)",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(247, 182, 67); // normal yellow color",
        "    }",
        "",
        "    // pass 1 - draw red, light yellow and dark yellow",
        "    col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,0.,0.,3.,3., 3.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,0.,3.,3.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,0.,0.,3.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  7) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  6) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  4) col = SPRROW(x,0.,3.,0.,0.,0.,3.,0.,0., 0.,0.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  3) col = SPRROW(x,0.,0.,0.,0.,0.,0.,2.,2., 0.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,0.,2.,2.,2.,2.,2., 2.,0.,1.,1.,1.,1.,0.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,0.,0.,2.,2.,2., 2.,2.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(249, 58, 28); // mouth color (red)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(222, 128, 55); // brown",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(249, 214, 145); // light yellow",
        "    }",
        "}",
        "",
        "void drawBirdF2(int x, int y)",
        "{",
        "    if (y < 0 || y > 11 || x < 0 || x > 15) {",
        "        return;",
        "    }",
        "",
        "    // pass 0 - draw black, white and yellow",
        "    float col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,1.,1., 1.,1.,1.,1.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,1.,1.,3.,3., 3.,1.,2.,2.,1.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,1.,3.,3.,3.,3., 1.,2.,2.,2.,2.,1.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,1.,1.,1.,3.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  7) col = SPRROW(x,1.,2.,2.,2.,1.,3.,3.,3., 1.,2.,2.,2.,1.,2.,1.,0.);",
        "    if (y ==  6) col = SPRROW(x,1.,2.,2.,2.,2.,1.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  5) col = SPRROW(x,1.,2.,2.,2.,2.,1.,3.,3., 3.,3.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  4) col = SPRROW(x,0.,1.,2.,2.,2.,1.,3.,3., 3.,1.,2.,2.,2.,2.,2.,1.);",
        "    if (y ==  3) col = SPRROW(x,0.,1.,1.,1.,1.,3.,3.,3., 1.,2.,1.,1.,1.,1.,1.,1.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,1.,3.,3.,3.,3.,3., 3.,1.,2.,2.,2.,2.,1.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,1.,1.,3.,3.,3., 3.,3.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,1.,1.,1., 1.,1.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(82,56,70); // outline color (black)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(250,250,250); // eye color (white)",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(247, 182, 67); // normal yellow color",
        "    }",
        "",
        "    // pass 1 - draw red, light yellow and dark yellow",
        "    col = 0.0; // 0 = transparent",
        "    if (y == 11) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y == 10) col = SPRROW(x,0.,0.,0.,0.,0.,0.,3.,3., 3.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  9) col = SPRROW(x,0.,0.,0.,0.,3.,3.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  8) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  7) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  6) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  5) col = SPRROW(x,0.,3.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  4) col = SPRROW(x,0.,0.,3.,3.,3.,0.,0.,0., 0.,0.,1.,1.,1.,1.,1.,0.);",
        "    if (y ==  3) col = SPRROW(x,0.,0.,0.,0.,0.,2.,2.,2., 0.,1.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  2) col = SPRROW(x,0.,0.,0.,2.,2.,2.,2.,2., 2.,0.,1.,1.,1.,1.,0.,0.);",
        "    if (y ==  1) col = SPRROW(x,0.,0.,0.,0.,0.,2.,2.,2., 2.,2.,0.,0.,0.,0.,0.,0.);",
        "    if (y ==  0) col = SPRROW(x,0.,0.,0.,0.,0.,0.,0.,0., 0.,0.,0.,0.,0.,0.,0.,0.);",
        "",
        "    col = SELECT(mod(float(x),8.0),col);",
        "    if (col == 1.0) {",
        "        fragColor = RGB(249, 58, 28); // mouth color (red)",
        "    }",
        "    else if (col == 2.0) {",
        "        fragColor = RGB(222, 128, 55); // brown",
        "    }",
        "    else if (col == 3.0) {",
        "        fragColor = RGB(249, 214, 145); // light yellow",
        "    }",
        "}",
        "",
        "vec2 getLevelPixel(vec2 fragCoord)",
        "{",
        "    // Get the current game pixel",
        "    // (Each game pixel is two screen pixels)",
        "    //  (or four, if the screen is larger)",
        "    float x = fragCoord.x / 2.0;",
        "    float y = fragCoord.y / 2.0;",
        "",
        "    if (iResolution.y >= 640.0) {",
        "        x /= 2.0;",
        "        y /= 2.0;",
        "    }",
        "",
        "    if (iResolution.y < 200.0) {",
        "        x *= 2.0;",
        "        y *= 2.0;",
        "    }",
        "",
        "    return vec2(x,y);",
        "}",
        "",
        "vec2 getLevelBounds()",
        "{",
        "    // same logic as getLevelPixel, but returns the boundaries of the screen",
        "",
        "    float x = iResolution.x / 2.0;",
        "    float y = iResolution.y / 2.0;",
        "",
        "    if (iResolution.y >= 640.0) {",
        "        x /= 2.0;",
        "        y /= 2.0;",
        "    }",
        "",
        "    if (iResolution.y < 200.0) {",
        "        x *= 2.0;",
        "        y *= 2.0;",
        "    }",
        "",
        "    return vec2(x,y);",
        "}",
        "",
        "void drawGround(vec2 co)",
        "{",
        "    drawHorzRect(co.y, 0.0, 31.0, RGB(221, 216, 148));",
        "    drawHorzRect(co.y, 31.0, 32.0, RGB(208, 167, 84)); // shadow below the green sprites",
        "}",
        "",
        "void drawGreenStripes(vec2 co)",
        "{",
        "    int f = int(mod(flappyTick(), 6.0));",
        "",
        "    drawHorzRect(co.y, 32.0, 33.0, RGB(86, 126, 41)); // shadow blow",
        "",
        "    const float MIN_Y = 33.0;",
        "    const float HEIGHT = 6.0;",
        "",
        "    vec4 darkGreen  = RGB(117, 189, 58);",
        "    vec4 lightGreen = RGB(158, 228, 97);",
        "",
        "    // draw diagonal stripes, and animate them",
        "    if ((co.y >= MIN_Y) && (co.y < MIN_Y+HEIGHT)) {",
        "        float yPos = co.y - MIN_Y - float(f);",
        "        float xPos = mod((co.x - yPos), HEIGHT);",
        "",
        "        if (xPos >= HEIGHT / 2.0) {",
        "            fragColor = darkGreen;",
        "        }",
        "        else {",
        "            fragColor = lightGreen;",
        "        }",
        "    }",
        "",
        "    drawHorzRect(co.y, 37.0, 38.0, RGB(228, 250, 145)); // shadow highlight above",
        "    drawHorzRect(co.y, 38.0, 39.0, RGB(84, 56, 71)); // black separator",
        "}",
        "",
        "void drawTile(int type, vec2 tileCorner, vec2 co)",
        "{",
        "    if ((co.x < tileCorner.x) || (co.x > (tileCorner.x + 16.0)) ||",
        "        (co.y < tileCorner.y) || (co.y > (tileCorner.y + 16.0)))",
        "    {",
        "        return;",
        "    }",
        "",
        "    int modX = int(mod(co.x - tileCorner.x, 16.0));",
        "    int modY = int(mod(co.y - tileCorner.y, 16.0));",
        "",
        "    if (type == 0){",
        "        drawLowBush(modX, modY);",
        "    }",
        "    else if (type == 1) {",
        "        drawHighBush(modX, modY);",
        "    }",
        "    else if (type == 2) {",
        "        drawCloud(modX, modY);",
        "    }",
        "    else if (type == 3) {",
        "        drawBirdF0(modX, modY);",
        "    }",
        "    else if (type == 4) {",
        "        drawBirdF1(modX, modY);",
        "    }",
        "    else if (type == 5) {",
        "        drawBirdF2(modX, modY);",
        "    }",
        "}",
        "",
        "void drawVertLine(vec2 co, float xPos, float yStart, float yEnd, vec4 color)",
        "{",
        "    if ((co.x >= xPos) && (co.x < (xPos + 1.0)) && (co.y >= yStart) && (co.y < yEnd)) {",
        "        fragColor = color;",
        "    }",
        "}",
        "",
        "void drawHorzLine(vec2 co, float yPos, float xStart, float xEnd, vec4 color)",
        "{",
        "    if ((co.y >= yPos) && (co.y < (yPos + 1.0)) && (co.x >= xStart) && (co.x < xEnd)) {",
        "        fragColor = color;",
        "    }",
        "}",
        "",
        "void drawHorzGradientRect(vec2 co, vec2 bottomLeft, vec2 topRight, vec4 leftColor, vec4 rightColor)",
        "{",
        "    if ((co.x < bottomLeft.x) || (co.y < bottomLeft.y) ||",
        "        (co.x > topRight.x) || (co.y > topRight.y))",
        "    {",
        "        return;",
        "    }",
        "",
        "    float distanceRatio = (co.x - bottomLeft.x) / (topRight.x - bottomLeft.x);",
        "",
        "    fragColor = (1.0 - distanceRatio) * leftColor + distanceRatio * rightColor;",
        "}",
        "",
        "void drawBottomPipe(vec2 co, float xPos, float height)",
        "{",
        "    if ((co.x < xPos) || (co.x > (xPos + PIPE_WIDTH)) ||",
        "        (co.y < PIPE_BOTTOM) || (co.y > (PIPE_BOTTOM + height)))",
        "    {",
        "        return;",
        "    }",
        "",
        "    // draw the bottom part of the pipe",
        "    // outlines",
        "    float bottomPartEnd = PIPE_BOTTOM - PIPE_HOLE_HEIGHT + height;",
        "    drawVertLine(co, xPos+1.0, PIPE_BOTTOM, bottomPartEnd, PIPE_OUTLINE_COLOR);",
        "    drawVertLine(co, xPos+PIPE_WIDTH-2.0, PIPE_WIDTH, bottomPartEnd, PIPE_OUTLINE_COLOR);",
        "",
        "    // gradient fills",
        "    drawHorzGradientRect(co, vec2(xPos+2.0, PIPE_BOTTOM), vec2(xPos + 10.0, bottomPartEnd), RGB(133, 168, 75), RGB(228, 250, 145));",
        "    drawHorzGradientRect(co, vec2(xPos+10.0, PIPE_BOTTOM), vec2(xPos + 20.0, bottomPartEnd), RGB(228, 250, 145), RGB(86, 126, 41));",
        "    drawHorzGradientRect(co, vec2(xPos+20.0, PIPE_BOTTOM), vec2(xPos + 24.0, bottomPartEnd), RGB(86, 126, 41), RGB(86, 126, 41));",
        "",
        "    // shadows",
        "    drawHorzLine(co, bottomPartEnd - 1.0, xPos + 2.0, xPos+PIPE_WIDTH-2.0, RGB(86, 126, 41));",
        "",
        "    // draw the pipe opening",
        "    // outlines",
        "    drawVertLine(co, xPos, bottomPartEnd, bottomPartEnd + PIPE_HOLE_HEIGHT, PIPE_OUTLINE_COLOR);",
        "    drawVertLine(co, xPos+PIPE_WIDTH-1.0, bottomPartEnd, bottomPartEnd + PIPE_HOLE_HEIGHT, PIPE_OUTLINE_COLOR);",
        "    drawHorzLine(co, bottomPartEnd, xPos, xPos+PIPE_WIDTH-1.0, PIPE_OUTLINE_COLOR);",
        "    drawHorzLine(co, bottomPartEnd + PIPE_HOLE_HEIGHT-1.0, xPos, xPos+PIPE_WIDTH-1.0, PIPE_OUTLINE_COLOR);",
        "",
        "    // gradient fills",
        "    float gradientBottom = bottomPartEnd + 1.0;",
        "    float gradientTop = bottomPartEnd + PIPE_HOLE_HEIGHT - 1.0;",
        "    drawHorzGradientRect(co, vec2(xPos+1.0, gradientBottom), vec2(xPos + 5.0, gradientTop), RGB(221, 234, 131), RGB(228, 250, 145));",
        "    drawHorzGradientRect(co, vec2(xPos+5.0, gradientBottom), vec2(xPos + 22.0, gradientTop), RGB(228, 250, 145), RGB(86, 126, 41));",
        "    drawHorzGradientRect(co, vec2(xPos+22.0, gradientBottom), vec2(xPos + 25.0, gradientTop), RGB(86, 126, 41), RGB(86, 126, 41));",
        "",
        "    // shadows",
        "    drawHorzLine(co, gradientBottom, xPos+1.0, xPos+25.0, RGB(86, 126, 41));",
        "    drawHorzLine(co, gradientTop-1.0, xPos+1.0, xPos+25.0, RGB(122, 158, 67));",
        "}",
        "",
        "void drawTopPipe(vec2 co, float xPos, float height)",
        "{",
        "    vec2 bounds = getLevelBounds();",
        "",
        "    if ((co.x < xPos) || (co.x > (xPos + PIPE_WIDTH)) ||",
        "        (co.y < (bounds.y - height)) || (co.y > bounds.y))",
        "    {",
        "        return;",
        "    }",
        "",
        "    // draw the bottom part of the pipe",
        "    // outlines",
        "    float bottomPartEnd = bounds.y + PIPE_HOLE_HEIGHT - height;",
        "    drawVertLine(co, xPos+1.0, bottomPartEnd, bounds.y, PIPE_OUTLINE_COLOR);",
        "    drawVertLine(co, xPos+PIPE_WIDTH-2.0, bottomPartEnd, bounds.y, PIPE_OUTLINE_COLOR);",
        "",
        "    // gradient fills",
        "    drawHorzGradientRect(co, vec2(xPos+2.0, bottomPartEnd), vec2(xPos + 10.0, bounds.y), RGB(133, 168, 75), RGB(228, 250, 145));",
        "    drawHorzGradientRect(co, vec2(xPos+10.0, bottomPartEnd), vec2(xPos + 20.0, bounds.y), RGB(228, 250, 145), RGB(86, 126, 41));",
        "    drawHorzGradientRect(co, vec2(xPos+20.0, bottomPartEnd), vec2(xPos + 24.0, bounds.y), RGB(86, 126, 41), RGB(86, 126, 41));",
        "",
        "    // shadows",
        "    drawHorzLine(co, bottomPartEnd+1.0, xPos + 2.0, xPos+PIPE_WIDTH-2.0, RGB(86, 126, 41));",
        "",
        "    // draw the pipe opening",
        "    // outlines",
        "    drawVertLine(co, xPos, bottomPartEnd - PIPE_HOLE_HEIGHT, bottomPartEnd, PIPE_OUTLINE_COLOR);",
        "    drawVertLine(co, xPos+PIPE_WIDTH-1.0, bottomPartEnd - PIPE_HOLE_HEIGHT, bottomPartEnd, PIPE_OUTLINE_COLOR);",
        "    drawHorzLine(co, bottomPartEnd, xPos, xPos+PIPE_WIDTH, PIPE_OUTLINE_COLOR);",
        "    drawHorzLine(co, bottomPartEnd - PIPE_HOLE_HEIGHT, xPos, xPos+PIPE_WIDTH-1.0, PIPE_OUTLINE_COLOR);",
        "",
        "    // gradient fills",
        "    float gradientBottom = bottomPartEnd - PIPE_HOLE_HEIGHT + 1.0;",
        "    float gradientTop = bottomPartEnd;",
        "    drawHorzGradientRect(co, vec2(xPos+1.0, gradientBottom), vec2(xPos + 5.0, gradientTop), RGB(221, 234, 131), RGB(228, 250, 145));",
        "    drawHorzGradientRect(co, vec2(xPos+5.0, gradientBottom), vec2(xPos + 22.0, gradientTop), RGB(228, 250, 145), RGB(86, 126, 41));",
        "    drawHorzGradientRect(co, vec2(xPos+22.0, gradientBottom), vec2(xPos + 25.0, gradientTop), RGB(86, 126, 41), RGB(86, 126, 41));",
        "",
        "    // shadows",
        "    drawHorzLine(co, gradientBottom, xPos+1.0, xPos+25.0, RGB(122, 158, 67));",
        "    drawHorzLine(co, gradientTop-1.0, xPos+1.0, xPos+25.0, RGB(86, 126, 41));",
        "}",
        "",
        "void drawBushGroup(vec2 bottomCorner, vec2 co)",
        "{",
        "    drawTile(0, bottomCorner, co);",
        "    bottomCorner.x += 13.0;",
        "",
        "    drawTile(1, bottomCorner, co);",
        "    bottomCorner.x += 13.0;",
        "",
        "    drawTile(0, bottomCorner, co);",
        "}",
        "",
        "void drawBushes(vec2 co)",
        "{",
        "    drawHorzRect(co.y, 39.0, 70.0, RGB(100, 224, 117));",
        "",
        "    for (int i = 0; i < 20; i++) {",
        "        float xOffset = float(i) * 45.0;",
        "        drawBushGroup(vec2(xOffset, 70.0), co);",
        "        drawBushGroup(vec2(xOffset+7.0, 68.0), co);",
        "        drawBushGroup(vec2(xOffset-16.0, 65.0), co);",
        "    }",
        "}",
        "",
        "void drawClouds(vec2 co)",
        "{",
        "    for (int i = 0; i < 20; i++) {",
        "        float xOffset = float(i) * 40.0;",
        "        drawTile(2, vec2(xOffset, 95.0), co);",
        "        drawTile(2, vec2(xOffset+14.0, 91.0), co);",
        "        drawTile(2, vec2(xOffset+28.0, 93.0), co);",
        "    }",
        "",
        "    drawHorzRect(co.y, 70.0, 95.0, RGB(233,251,218));",
        "}",
        "",
        "void drawPipePair(vec2 co, float xPos, float bottomPipeHeight)",
        "{",
        "    vec2 bounds = getLevelBounds();",
        "    float topPipeHeight = bounds.y - (VERT_PIPE_DISTANCE + PIPE_BOTTOM + bottomPipeHeight);",
        "",
        "    drawBottomPipe(co, xPos, bottomPipeHeight);",
        "    drawTopPipe(co, xPos, topPipeHeight);",
        "}",
        "",
        "void drawPipes(vec2 co)",
        "{",
        "    // calculate the starting position of the pipes according to the current frame",
        "    float animationCycleLength = HORZ_PIPE_DISTANCE * PIPE_PER_CYCLE; // the number of frames after which the animation should repeat itself",
        "    int f = int(mod(flappyTick(), animationCycleLength));",
        "    float xPos = -float(f);",
        "",
        "    float center = (PIPE_MAX + PIPE_MIN) / 2.0;",
        "    float halfTop = (center + PIPE_MAX) / 2.0;",
        "    float halfBottom = (center + PIPE_MIN) / 2.0;",
        "",
        "    for (int i = 0; i < 12; i++)",
        "    {",
        "        float yPos = center;",
        "        int cycle = int(mod(float(i),8.0));",
        "",
        "        if ((cycle == 1) || (cycle == 3)){",
        "            yPos = halfTop;",
        "        }",
        "        else if (cycle == 2) {",
        "            yPos = PIPE_MAX;",
        "        }",
        "        else if ((cycle == 5) || (cycle == 7)) {",
        "            yPos = halfBottom;",
        "        }",
        "        else if (cycle == 6){",
        "            yPos = PIPE_MIN;",
        "        }",
        "",
        "        drawPipePair(co, xPos, yPos);",
        "        xPos += HORZ_PIPE_DISTANCE;",
        "    }",
        "}",
        "",
        "void drawBird(vec2 co)",
        "{",
        "    float yPos;",
        "    int animFrame;",
        "",
        "    if (uControlEnabled > 0.5) {",
        "        yPos = uBirdY;",
        "        animFrame = int(mod(floor(uBirdFrame), 3.0));",
        "    }",
        "    else {",
        "        float animationCycleLength = HORZ_PIPE_DISTANCE * PIPE_PER_CYCLE; // the number of frames after which the animation should repeat itself",
        "        int cycleFrame = int(mod(flappyTick(), animationCycleLength));",
        "        float fCycleFrame = float(cycleFrame);",
        "",
        "        const float START_POS = 110.0;",
        "        const float SPEED = 2.88;",
        "        const float UPDOWN_DELTA = 0.16;",
        "        const float ACCELERATION = -0.0975;",
        "        float jumpFrame = float(int(mod(iTime * 60.0, 30.0)));",
        "        int horzDist = int(HORZ_PIPE_DISTANCE);",
        "",
        "        yPos = START_POS + SPEED * jumpFrame + ACCELERATION * pow(jumpFrame, 2.0);",
        "        float speedDelta = UPDOWN_DELTA * mod(fCycleFrame, HORZ_PIPE_DISTANCE);",
        "        int prevUpCycles = 0;",
        "        int prevDownCycles = 0;",
        "        int cycleCount = int(fCycleFrame / HORZ_PIPE_DISTANCE);",
        "",
        "        for (int i = 0; i < 10; i++) {",
        "            if (i <= cycleCount) {",
        "                if (i == 1) {",
        "                    prevUpCycles++;",
        "                }",
        "",
        "                if ((i >= 2) && (i < 6)) {",
        "                    prevDownCycles++;",
        "                }",
        "                if (i >= 6) {",
        "                    prevUpCycles++;",
        "                }",
        "            }",
        "        }",
        "",
        "        yPos += ((float(prevUpCycles - prevDownCycles)) * HORZ_PIPE_DISTANCE * UPDOWN_DELTA);",
        "",
        "        if (((cycleFrame >= 0) && (cycleFrame < horzDist)) ||",
        "            ((cycleFrame >= 5*horzDist) && (cycleFrame < 9*horzDist))) {",
        "            yPos += speedDelta;",
        "        }",
        "        else {",
        "            yPos -= speedDelta;",
        "        }",
        "",
        "        animFrame = int(mod(iTime * 7.0, 3.0));",
        "    }",
        "",
        "    if (animFrame == 0) drawTile(3, vec2(105, int(yPos)), co);",
        "    if (animFrame == 1) drawTile(4, vec2(105, int(yPos)), co);",
        "    if (animFrame == 2) drawTile(5, vec2(105, int(yPos)), co);",
        "",
        "    if (uControlEnabled > 0.5 && uBirdAlive < 0.5) {",
        "        if (co.x >= 104.0 && co.x <= 122.0 && co.y >= yPos - 1.0 && co.y <= yPos + 13.0) {",
        "            fragColor = RGB(249, 58, 28);",
        "        }",
        "    }",
        "}",
        "",
        "void mainImage(out vec4 iFragColor, in vec2 fragCoord)",
        "{",
        "    vec2 levelPixel = getLevelPixel(fragCoord);",
        "",
        "    fragColor = RGB(113, 197, 207); // draw the blue sky background",
        "",
        "    drawGround(levelPixel);",
        "    drawGreenStripes(levelPixel);",
        "    drawClouds(levelPixel);",
        "    drawBushes(levelPixel);",
        "    drawPipes(levelPixel);",
        "    drawBird(levelPixel);",
        "",
        "    iFragColor = fragColor;",
        "}",
        "",
        "void main()",
        "{",
        "    vec4 texel = texture2D(uTex, vTexCoord);",
        "    float texMode = uUseTexAlpha > 0.5 ? 1.0 : 0.0;",
        "",
        "    if (texMode > 0.5 && texel.a <= 0.001)",
        "    {",
        "        discard;",
        "    }",
        "",
        "    vec4 sceneColor = vec4(0.0);",
        "    mainImage(sceneColor, gl_FragCoord.xy);",
        "    vec3 texRgb = texMode > 0.5 ? texel.rgb : vec3(1.0);",
        "    float texAlpha = texMode > 0.5 ? texel.a : 1.0;",
        "    gl_FragColor = vec4(sceneColor.rgb * texRgb * vColor.rgb, texAlpha * vColor.a);",
        "}"
    };
    private static final String SOURCE = buildSource();

    private FlappyBirdFragmentShaderTemplate()
    {
    }

    static String source()
    {
        return SOURCE;
    }

    private static String buildSource()
    {
        StringBuilder builder = new StringBuilder(32768);

        for (int i = 0; i < LINES.length; ++i)
        {
            builder.append(LINES[i]).append('\n');
        }

        return builder.toString();
    }
}

