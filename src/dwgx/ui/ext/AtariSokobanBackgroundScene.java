package dwgx.ui.ext;

import java.util.Arrays;

/**
 * Atari-style Sokoban mini-game for the main-menu shader background.
 * Gameplay logic mirrors the original Shadertoy map rules so level layout and collision stay aligned.
 */
    public final class AtariSokobanBackgroundScene implements MainMenuBackgroundScene
{
    private static final int MODE_LOADING = 0;
    private static final int MODE_PLAYING = 1;
    private static final int MODE_CELEBRATING = 2;
    private static final int MODE_LAMENTING = 3;

    private static final int TILE_SOLID = 1;
    private static final int TILE_SPIKES_SMASHED = 2;
    private static final int TILE_EXIT = 3;
    private static final int TILE_SPIKES = 4;
    private static final int TILE_DOOR = 6;
    private static final int TILE_EMPTY = 9;

    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 10;
    private static final int GRID_W = 20;
    private static final int GRID_H = 12;

    private static final int LWJGL_KEY_R = 19;
    private static final int LWJGL_KEY_LEFT = 203;
    private static final int LWJGL_KEY_RIGHT = 205;
    private static final int LWJGL_KEY_UP = 200;
    private static final int LWJGL_KEY_DOWN = 208;

    private static final float RESET_AFTER_WIN = 1.50F;
    private static final float RESET_AFTER_DEATH = 0.75F;
    private static final float ROCK_STEP_INTERVAL = 0.065F;

    private int currentLevel = LEVEL_MIN;
    private int mode = MODE_LOADING;
    private float modeTimer;
    private float doorPulse;

    private int playerX = 4;
    private int playerY = 2;
    private int boxX = -1;
    private int boxY = -1;
    private int targetX = -1;
    private int targetY = -1;
    private int rockX = -1;
    private int rockY = -1;
    private int rockVx;
    private int rockVy;
    private float rockStepTimer;

    private boolean doorOpen;
    private final boolean[][] smashedSpikes = new boolean[GRID_W][GRID_H];

    private long lastUpdateMs;
    private boolean queuedMove;
    private int queuedDx;
    private int queuedDy;
    private boolean queuedReset;

    public String fragmentShaderSource()
    {
        return AtariSokobanFragmentShaderTemplate.source();
    }

    public MainMenuSplashShader.TextureAlphaMode textureAlphaMode()
    {
        return MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA;
    }

    public void initialize()
    {
        if (this.lastUpdateMs == 0L)
        {
            this.loadLevel(this.currentLevel);
        }
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return false;
        }

        if (typedChar == 'r' || typedChar == 'R' || keyCode == LWJGL_KEY_R)
        {
            this.queuedReset = true;
            return true;
        }

        int dx = 0;
        int dy = 0;

        if (keyCode == LWJGL_KEY_LEFT || typedChar == 'a' || typedChar == 'A')
        {
            dx = -1;
        }
        else if (keyCode == LWJGL_KEY_RIGHT || typedChar == 'd' || typedChar == 'D')
        {
            dx = 1;
        }
        else if (keyCode == LWJGL_KEY_UP || typedChar == 'w' || typedChar == 'W')
        {
            dy = 1;
        }
        else if (keyCode == LWJGL_KEY_DOWN || typedChar == 's' || typedChar == 'S')
        {
            dy = -1;
        }

        if (dx == 0 && dy == 0)
        {
            return false;
        }

        if (this.mode != MODE_PLAYING)
        {
            this.queuedReset = true;
        }
        else
        {
            this.queuedMove = true;
            this.queuedDx = dx;
            this.queuedDy = dy;
        }

        return true;
    }

    public void tick(boolean sceneEnabled)
    {
        long now = System.currentTimeMillis();

        if (!sceneEnabled)
        {
            this.lastUpdateMs = now;
            this.queuedMove = false;
            return;
        }

        if (this.lastUpdateMs == 0L)
        {
            this.lastUpdateMs = now;
            return;
        }

        float dt = (float)(now - this.lastUpdateMs) / 1000.0F;
        this.lastUpdateMs = now;

        if (dt <= 0.0F)
        {
            return;
        }

        if (dt > 0.05F)
        {
            dt = 0.05F;
        }

        if (this.queuedReset)
        {
            this.queuedReset = false;
            this.queuedMove = false;
            this.mode = MODE_LOADING;
            this.loadLevel(this.currentLevel);
            return;
        }

        if (this.mode == MODE_LOADING)
        {
            this.loadLevel(this.currentLevel);
            return;
        }

        this.doorPulse = Math.max(0.0F, this.doorPulse - dt);

        if (this.mode == MODE_CELEBRATING)
        {
            this.modeTimer -= dt;

            if (this.modeTimer <= 0.0F)
            {
                this.currentLevel = this.wrapLevel(this.currentLevel + 1);
                this.mode = MODE_LOADING;
                this.loadLevel(this.currentLevel);
            }

            this.queuedMove = false;
            return;
        }

        if (this.mode == MODE_LAMENTING)
        {
            this.modeTimer -= dt;

            if (this.modeTimer <= 0.0F)
            {
                this.mode = MODE_LOADING;
                this.loadLevel(this.currentLevel);
            }

            this.queuedMove = false;
            return;
        }

        if (this.queuedMove)
        {
            this.applyMove(this.queuedDx, this.queuedDy);
            this.queuedMove = false;
        }

        this.updateRock(dt);

        if (this.mode == MODE_PLAYING && this.hasRock() && this.playerX == this.rockX && this.playerY == this.rockY)
        {
            this.triggerDeath();
        }
    }

    public void applyShaderUniforms(MainMenuSplashShader shader)
    {
        if (shader == null)
        {
            return;
        }

        shader.setAtariState(
            (float)this.playerX,
            (float)this.playerY,
            (float)this.boxX,
            (float)this.boxY,
            (float)this.targetX,
            (float)this.targetY,
            (float)this.rockX,
            (float)this.rockY,
            this.doorOpen ? 1.0F : 0.0F,
            (float)this.mode,
            this.doorPulse,
            (float)this.currentLevel,
            true
        );
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return null;
        }

        if (this.mode == MODE_CELEBRATING)
        {
            return "Atari Sokoban L" + this.currentLevel + ": clear";
        }

        if (this.mode == MODE_LAMENTING)
        {
            return "Atari Sokoban L" + this.currentLevel + ": death - R to reset";
        }

        return "Atari Sokoban L" + this.currentLevel + ": WASD/Arrows move, R reset";
    }

    private void loadLevel(int level)
    {
        this.currentLevel = this.wrapLevel(level);
        this.mode = MODE_PLAYING;
        this.modeTimer = 0.0F;
        this.doorPulse = 0.0F;
        this.doorOpen = false;

        this.boxX = -1;
        this.boxY = -1;
        this.targetX = -1;
        this.targetY = -1;
        this.rockX = -1;
        this.rockY = -1;
        this.rockVx = 0;
        this.rockVy = 0;
        this.rockStepTimer = 0.0F;

        this.clearSmashedSpikes();

        if (this.currentLevel == 1)
        {
            this.playerX = 4;
            this.playerY = 2;
        }
        else if (this.currentLevel == 2)
        {
            this.playerX = 17;
            this.playerY = 9;
        }
        else if (this.currentLevel == 3)
        {
            this.playerX = 9;
            this.playerY = 9;
            this.boxX = 10;
            this.boxY = 4;
            this.targetX = 6;
            this.targetY = 7;
        }
        else if (this.currentLevel == 4)
        {
            this.playerX = 3;
            this.playerY = 9;
            this.boxX = 4;
            this.boxY = 2;
            this.targetX = 15;
            this.targetY = 2;
        }
        else if (this.currentLevel == 5)
        {
            this.playerX = 7;
            this.playerY = 7;
            this.boxX = 7;
            this.boxY = 4;
            this.targetX = 15;
            this.targetY = 2;
            this.rockX = 9;
            this.rockY = 7;
        }
        else if (this.currentLevel == 6)
        {
            this.playerX = 7;
            this.playerY = 7;
            this.boxX = 2;
            this.boxY = 6;
            this.targetX = 1;
            this.targetY = 6;
            this.rockX = 11;
            this.rockY = 8;
        }
        else if (this.currentLevel == 7)
        {
            this.playerX = 11;
            this.playerY = 5;
            this.boxX = 2;
            this.boxY = 4;
            this.targetX = 1;
            this.targetY = 6;
            this.rockX = 9;
            this.rockY = 5;
        }
        else if (this.currentLevel == 8)
        {
            this.playerX = 14;
            this.playerY = 7;
            this.boxX = 13;
            this.boxY = 5;
            this.targetX = 14;
            this.targetY = 3;
            this.rockX = 9;
            this.rockY = 3;
        }
        else if (this.currentLevel == 9)
        {
            this.playerX = 18;
            this.playerY = 10;
            this.boxX = 2;
            this.boxY = 9;
            this.targetX = 17;
            this.targetY = 2;
            this.rockX = 17;
            this.rockY = 9;
        }
        else
        {
            this.playerX = 9;
            this.playerY = 5;
        }

        // Keep collision semantics consistent with shader rendering:
        // a rock occupying spikes should count as smashed immediately.
        if (this.hasRock())
        {
            this.markSpikeSmashed(this.rockX, this.rockY);
        }

        this.lastUpdateMs = System.currentTimeMillis();
    }

    private void clearSmashedSpikes()
    {
        for (int x = 0; x < GRID_W; ++x)
        {
            Arrays.fill(this.smashedSpikes[x], false);
        }
    }

    private int wrapLevel(int level)
    {
        if (level < LEVEL_MIN)
        {
            return LEVEL_MIN;
        }

        if (level > LEVEL_MAX)
        {
            return LEVEL_MIN;
        }

        return level;
    }

    private void applyMove(int dx, int dy)
    {
        if ((dx == 0 && dy == 0) || this.mode != MODE_PLAYING)
        {
            return;
        }

        int nextX = this.playerX + dx;
        int nextY = this.playerY + dy;
        int destTile = this.tileAt(nextX, nextY);

        if (destTile == TILE_SOLID || (destTile == TILE_DOOR && !this.doorOpen))
        {
            return;
        }

        if (this.hasBox() && nextX == this.boxX && nextY == this.boxY)
        {
            int boxDestX = nextX + dx;
            int boxDestY = nextY + dy;
            int boxDestTile = this.tileAt(boxDestX, boxDestY);

            if ((boxDestTile != TILE_EMPTY && boxDestTile != TILE_SPIKES_SMASHED)
                || (this.hasRock() && boxDestX == this.rockX && boxDestY == this.rockY))
            {
                return;
            }

            this.boxX = boxDestX;
            this.boxY = boxDestY;

            if (this.hasTarget() && boxDestX == this.targetX && boxDestY == this.targetY)
            {
                this.boxX = -1;
                this.boxY = -1;
                this.targetX = -1;
                this.targetY = -1;
                this.doorOpen = true;
                this.doorPulse = 1.0F;
            }
        }
        else if (this.hasRock() && nextX == this.rockX && nextY == this.rockY)
        {
            if (this.rockVx != 0 || this.rockVy != 0)
            {
                return;
            }

            int rockDestX = nextX + dx;
            int rockDestY = nextY + dy;
            int rockDestTile = this.tileAt(rockDestX, rockDestY);

            if ((this.hasBox() && rockDestX == this.boxX && rockDestY == this.boxY)
                || (rockDestTile != TILE_EMPTY
                    && rockDestTile != TILE_SPIKES
                    && rockDestTile != TILE_SPIKES_SMASHED
                    && !(rockDestTile == TILE_DOOR && this.doorOpen)))
            {
                return;
            }

            this.rockX = rockDestX;
            this.rockY = rockDestY;
            this.markSpikeSmashed(this.rockX, this.rockY);
            this.rockVx = dx;
            this.rockVy = dy;
            this.rockStepTimer = ROCK_STEP_INTERVAL;
        }

        this.playerX = nextX;
        this.playerY = nextY;

        if (destTile == TILE_EXIT)
        {
            this.mode = MODE_CELEBRATING;
            this.modeTimer = RESET_AFTER_WIN;
            return;
        }

        if (destTile == TILE_SPIKES)
        {
            this.triggerDeath();
        }
    }

    private void updateRock(float dt)
    {
        if (!this.hasRock() || (this.rockVx == 0 && this.rockVy == 0) || this.mode != MODE_PLAYING)
        {
            return;
        }

        this.rockStepTimer -= dt;

        while (this.rockStepTimer <= 0.0F)
        {
            this.rockStepTimer += ROCK_STEP_INTERVAL;
            int nextX = this.rockX + this.rockVx;
            int nextY = this.rockY + this.rockVy;
            int rockDestTile = this.tileAt(nextX, nextY);

            if (rockDestTile == TILE_SOLID
                || (rockDestTile == TILE_DOOR && !this.doorOpen)
                || (this.hasBox() && nextX == this.boxX && nextY == this.boxY))
            {
                this.rockVx = 0;
                this.rockVy = 0;
                this.rockStepTimer = 0.0F;
                return;
            }

            this.rockX = nextX;
            this.rockY = nextY;
            this.markSpikeSmashed(this.rockX, this.rockY);

            if (this.playerX == this.rockX && this.playerY == this.rockY)
            {
                this.triggerDeath();
                this.rockVx = 0;
                this.rockVy = 0;
                this.rockStepTimer = 0.0F;
                return;
            }
        }
    }

    private void triggerDeath()
    {
        if (this.mode != MODE_PLAYING)
        {
            return;
        }

        this.mode = MODE_LAMENTING;
        this.modeTimer = RESET_AFTER_DEATH;
    }

    private boolean hasBox()
    {
        return this.boxX >= 0 && this.boxY >= 0;
    }

    private boolean hasTarget()
    {
        return this.targetX >= 0 && this.targetY >= 0;
    }

    private boolean hasRock()
    {
        return this.rockX >= 0 && this.rockY >= 0;
    }

    private boolean isInsideGrid(int x, int y)
    {
        return x >= 0 && x < GRID_W && y >= 0 && y < GRID_H;
    }

    private boolean isSpikeSmashed(int x, int y)
    {
        return this.isInsideGrid(x, y) && this.smashedSpikes[x][y];
    }

    private void markSpikeSmashed(int x, int y)
    {
        if (!this.isInsideGrid(x, y))
        {
            return;
        }

        if (this.baseTile(x, y) == TILE_SPIKES)
        {
            this.smashedSpikes[x][y] = true;
        }
    }

    private static int mirrorX(int x)
    {
        return 10 - (int)Math.ceil(Math.abs(9.5D - (double)x));
    }

    private int tileAt(int x, int y)
    {
        int tile = this.baseTile(x, y);

        if (tile == TILE_SPIKES && (this.isSpikeSmashed(x, y) || (this.hasRock() && this.rockX == x && this.rockY == y)))
        {
            return TILE_SPIKES_SMASHED;
        }

        return tile;
    }

    private int baseTile(int x, int y)
    {
        int tile = TILE_EMPTY;

        if (y < 1 || y > 10 || x < 1 || x > 18)
        {
            tile = TILE_SOLID;
        }

        int pmx = mirrorX(x);

        if (this.currentLevel == 1)
        {
            if (x == 17 && y == 9)
            {
                tile = TILE_EXIT;
            }
            else if ((y != 1 && pmx == 9)
                || (y == 4 && pmx >= 8)
                || ((y == 5 || y == 8) && pmx >= 5)
                || ((y == 6 || y == 7) && pmx >= 4)
                || (pmx == 1 && y == 2)
                || (pmx == 1 && y == 3))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 2)
        {
            if ((x == 18 && y == 8) || (x == 17 && y == 6) || (x == 13 && y == 7)
                || (x == 12 && y == 2) || (x == 6 && y == 1) || (x == 6 && y == 8))
            {
                tile = TILE_SPIKES;
            }
            else if (x == 9 && y == 9)
            {
                tile = TILE_EXIT;
            }
            else if ((pmx >= 3 && pmx <= 5 && y >= 8)
                || (pmx == 6 && y == 9)
                || (pmx == 1 && y >= 6 && y <= 7)
                || (y == 6 && pmx >= 7)
                || (y >= 4 && y <= 5 && pmx >= 7 && pmx <= 8)
                || (y >= 2 && y <= 3 && pmx == 8))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 3)
        {
            if (x == 18 && y == 3)
            {
                tile = TILE_EXIT;
            }
            else if (x == 17 && y == 5)
            {
                tile = TILE_DOOR;
            }
            else if ((x == 6 && y == 10) || (x == 13 && y == 10) || (x == 7 && y == 5))
            {
                tile = TILE_SPIKES;
            }
            else if ((pmx == 1 && y >= 9)
                || (pmx >= 3 && pmx <= 6 && y <= 6)
                || (pmx == 9 && y == 3)
                || (x == 18 && y == 5)
                || (y == 1 && pmx <= 6))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 4)
        {
            if (x == 10 && y == 9)
            {
                tile = TILE_EXIT;
            }
            else if (x == 13 && y == 8)
            {
                tile = TILE_DOOR;
            }
            else if ((x == 6 && y == 8) || (x == 10 && y == 1) || (x == 4 && y == 3))
            {
                tile = TILE_SPIKES;
            }
            else if ((y == 10 && x >= 8)
                || (y >= 8 && pmx == 5)
                || (pmx == 1 && (y == 6 || y == 9))
                || (y == 8 && pmx >= 7)
                || (y == 7 && pmx >= 8)
                || (y >= 5 && y <= 6 && pmx == 9)
                || (y == 5 && pmx >= 4 && pmx <= 5)
                || (pmx == 2 && (y == 2 || y == 4))
                || (pmx == 6 && y <= 4)
                || (y == 1 && (pmx == 5 || pmx == 8)))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 5)
        {
            if (x == 10 && y == 1)
            {
                tile = TILE_EXIT;
            }
            else if (x == 9 && y == 3)
            {
                tile = TILE_DOOR;
            }
            else if ((x == 6 || x == 12 || y == 9 || y == 5) && x > 5 && x < 13 && y < 10 && y > 4)
            {
                tile = TILE_SPIKES;
            }
            else if (pmx <= 2
                || (y == 10 && (pmx == 3 || pmx == 5))
                || (y == 4 && pmx == 3)
                || (y == 3 && pmx >= 8)
                || (y == 2 && pmx == 8)
                || (y == 1 && pmx == 7))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 6)
        {
            if (x == 9 && y == 1)
            {
                tile = TILE_EXIT;
            }
            else if (x == 10 && y == 3)
            {
                tile = TILE_DOOR;
            }
            else if ((y == 4 && x >= 8 && x <= 11) || (pmx > 4 && y == 8 && x != 12))
            {
                tile = TILE_SPIKES;
            }
            else if ((y == 10 && pmx == 3)
                || (y >= 8 && (pmx == 4 || pmx == 8))
                || (y == 9 && pmx == 6)
                || (y >= 3 && y <= 5 && pmx >= 3 && pmx <= 4)
                || (y == 4 && pmx == 7)
                || (y <= 4 && pmx == 6)
                || (y == 3 && (pmx == 5 || pmx >= 8))
                || (y == 2 && pmx == 3)
                || (y == 1 && pmx == 1))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 7)
        {
            if (x == 17 && y == 1)
            {
                tile = TILE_EXIT;
            }
            else if (x == 17 && y == 3)
            {
                tile = TILE_DOOR;
            }
            else if ((pmx == 4 || pmx == 7 || y == 7 || y == 3) && x > 3)
            {
                tile = TILE_SPIKES;
            }
            else if ((x == 9 && y == 10)
                || (y == 5 && pmx == 1)
                || (y == 9 && pmx == 1)
                || (x >= 16 && y <= 2 && x != 17))
            {
                tile = TILE_SOLID;
            }
        }
        else if (this.currentLevel == 8)
        {
            if (x == 6 && y == 2)
            {
                tile = TILE_EXIT;
            }
            else if (x == 4 && y == 4)
            {
                tile = TILE_DOOR;
            }
            else if (y >= 9 || y <= 1 || x <= 3 || x >= 16
                || (x == 7 && y != 4 && y != 6)
                || (y == 8 && x >= 7 && x <= 12)
                || (x == 12 && (y == 2 || y == 5 || y == 7))
                || (x == 10 && y == 4)
                || (x == 11 && y == 4))
            {
                tile = TILE_SOLID;
            }
            else if ((y == 4 && x >= 5 && x <= 9) || (x == 7 && y == 6))
            {
                tile = TILE_SPIKES;
            }
        }
        else if (this.currentLevel == 9)
        {
            if (x == 9 && y == 1)
            {
                tile = TILE_EXIT;
            }
            else if (x == 9 && y == 2)
            {
                tile = TILE_DOOR;
            }
            else if ((x >= 8 && x <= 10 && y <= 2) || (y == 6 && x <= 4))
            {
                tile = TILE_SOLID;
            }
            else if (pmx <= 4)
            {
                tile = TILE_EMPTY;
            }
            else
            {
                tile = TILE_SPIKES;
            }
        }
        else if (this.currentLevel == 10)
        {
            if ((pmx == 8 && y == 7) || (pmx == 7 && y == 5) || (y == 4 && pmx >= 8))
            {
                tile = TILE_SOLID;
            }
        }

        return tile;
    }
}

final class AtariSokobanFragmentShaderTemplate
{
    private AtariSokobanFragmentShaderTemplate()
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
        "uniform vec2 uAtariPlayer;",
        "uniform vec2 uAtariBox;",
        "uniform vec2 uAtariTarget;",
        "uniform vec2 uAtariRock;",
        "uniform float uAtariDoorOpen;",
        "uniform float uAtariMode;",
        "uniform float uAtariFlash;",
        "uniform float uAtariLevel;",
        "varying vec4 vColor;",
        "",
        "const vec2 ATARI_REZ = vec2(160.0, 96.0);",
        "const float TILE_SOLID = 1.0;",
        "const float TILE_SPIKES_SMASHED = 2.0;",
        "const float TILE_EXIT = 3.0;",
        "const float TILE_SPIKES = 4.0;",
        "const float TILE_DOOR = 6.0;",
        "const float TILE_EMPTY = 9.0;",
        "",
        "const float MODE_PLAYING = 1.0;",
        "const float MODE_CELEBRATING = 2.0;",
        "const float MODE_LAMENTING = 3.0;",
        "",
        "bool sameTile(vec2 a, vec2 b)",
        "{",
        "    return abs(a.x - b.x) < 0.5 && abs(a.y - b.y) < 0.5;",
        "}",
        "",
        "float mirrorX(float x)",
        "{",
        "    return 10.0 - ceil(abs(9.5 - x));",
        "}",
        "",
        "float hash(vec2 p)",
        "{",
        "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);",
        "}",
        "",
        "float baseTile(vec2 pos, float level)",
        "{",
        "    float x = pos.x;",
        "    float y = pos.y;",
        "    float tile = TILE_EMPTY;",
        "",
        "    if (y < 1.0 || y > 10.0 || x < 1.0 || x > 18.0)",
        "    {",
        "        tile = TILE_SOLID;",
        "    }",
        "",
        "    float pmx = mirrorX(x);",
        "",
        "    if (abs(level - 1.0) < 0.5)",
        "    {",
        "        if (x == 17.0 && y == 9.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if ((y != 1.0 && pmx == 9.0)",
        "            || (y == 4.0 && pmx >= 8.0)",
        "            || ((y == 5.0 || y == 8.0) && pmx >= 5.0)",
        "            || ((y == 6.0 || y == 7.0) && pmx >= 4.0)",
        "            || (pmx == 1.0 && y == 2.0)",
        "            || (pmx == 1.0 && y == 3.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 2.0) < 0.5)",
        "    {",
        "        if ((x == 18.0 && y == 8.0) || (x == 17.0 && y == 6.0) || (x == 13.0 && y == 7.0)",
        "            || (x == 12.0 && y == 2.0) || (x == 6.0 && y == 1.0) || (x == 6.0 && y == 8.0))",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if (x == 9.0 && y == 9.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if ((pmx >= 3.0 && pmx <= 5.0 && y >= 8.0)",
        "            || (pmx == 6.0 && y == 9.0)",
        "            || (pmx == 1.0 && y >= 6.0 && y <= 7.0)",
        "            || (y == 6.0 && pmx >= 7.0)",
        "            || (y >= 4.0 && y <= 5.0 && pmx >= 7.0 && pmx <= 8.0)",
        "            || (y >= 2.0 && y <= 3.0 && pmx == 8.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 3.0) < 0.5)",
        "    {",
        "        if (x == 18.0 && y == 3.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 17.0 && y == 5.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((x == 6.0 && y == 10.0) || (x == 13.0 && y == 10.0) || (x == 7.0 && y == 5.0))",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if ((pmx == 1.0 && y >= 9.0)",
        "            || (pmx >= 3.0 && pmx <= 6.0 && y <= 6.0)",
        "            || (pmx == 9.0 && y == 3.0)",
        "            || (x == 18.0 && y == 5.0)",
        "            || (y == 1.0 && pmx <= 6.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 4.0) < 0.5)",
        "    {",
        "        if (x == 10.0 && y == 9.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 13.0 && y == 8.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((x == 6.0 && y == 8.0) || (x == 10.0 && y == 1.0) || (x == 4.0 && y == 3.0))",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if ((y == 10.0 && x >= 8.0)",
        "            || (y >= 8.0 && pmx == 5.0)",
        "            || (pmx == 1.0 && (y == 6.0 || y == 9.0))",
        "            || (y == 8.0 && pmx >= 7.0)",
        "            || (y == 7.0 && pmx >= 8.0)",
        "            || (y >= 5.0 && y <= 6.0 && pmx == 9.0)",
        "            || (y == 5.0 && pmx >= 4.0 && pmx <= 5.0)",
        "            || (pmx == 2.0 && (y == 2.0 || y == 4.0))",
        "            || (pmx == 6.0 && y <= 4.0)",
        "            || (y == 1.0 && (pmx == 5.0 || pmx == 8.0)))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 5.0) < 0.5)",
        "    {",
        "        if (x == 10.0 && y == 1.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 9.0 && y == 3.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((x == 6.0 || x == 12.0 || y == 9.0 || y == 5.0) && x > 5.0 && x < 13.0 && y < 10.0 && y > 4.0)",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if (pmx <= 2.0",
        "            || (y == 10.0 && (pmx == 3.0 || pmx == 5.0))",
        "            || (y == 4.0 && pmx == 3.0)",
        "            || (y == 3.0 && pmx >= 8.0)",
        "            || (y == 2.0 && pmx == 8.0)",
        "            || (y == 1.0 && pmx == 7.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 6.0) < 0.5)",
        "    {",
        "        if (x == 9.0 && y == 1.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 10.0 && y == 3.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((y == 4.0 && x >= 8.0 && x <= 11.0) || (pmx > 4.0 && y == 8.0 && x != 12.0))",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if ((y == 10.0 && pmx == 3.0)",
        "            || (y >= 8.0 && (pmx == 4.0 || pmx == 8.0))",
        "            || (y == 9.0 && pmx == 6.0)",
        "            || (y >= 3.0 && y <= 5.0 && pmx >= 3.0 && pmx <= 4.0)",
        "            || (y == 4.0 && pmx == 7.0)",
        "            || (y <= 4.0 && pmx == 6.0)",
        "            || (y == 3.0 && (pmx == 5.0 || pmx >= 8.0))",
        "            || (y == 2.0 && pmx == 3.0)",
        "            || (y == 1.0 && pmx == 1.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 7.0) < 0.5)",
        "    {",
        "        if (x == 17.0 && y == 1.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 17.0 && y == 3.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((pmx == 4.0 || pmx == 7.0 || y == 7.0 || y == 3.0) && x > 3.0)",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "        else if ((x == 9.0 && y == 10.0)",
        "            || (y == 5.0 && pmx == 1.0)",
        "            || (y == 9.0 && pmx == 1.0)",
        "            || (x >= 16.0 && y <= 2.0 && x != 17.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "    else if (abs(level - 8.0) < 0.5)",
        "    {",
        "        if (x == 6.0 && y == 2.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 4.0 && y == 4.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if (y >= 9.0 || y <= 1.0 || x <= 3.0 || x >= 16.0",
        "            || (x == 7.0 && y != 4.0 && y != 6.0)",
        "            || (y == 8.0 && x >= 7.0 && x <= 12.0)",
        "            || (x == 12.0 && (y == 2.0 || y == 5.0 || y == 7.0))",
        "            || (x == 10.0 && y == 4.0)",
        "            || (x == 11.0 && y == 4.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "        else if ((y == 4.0 && x >= 5.0 && x <= 9.0) || (x == 7.0 && y == 6.0))",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "    }",
        "    else if (abs(level - 9.0) < 0.5)",
        "    {",
        "        if (x == 9.0 && y == 1.0)",
        "        {",
        "            tile = TILE_EXIT;",
        "        }",
        "        else if (x == 9.0 && y == 2.0)",
        "        {",
        "            tile = TILE_DOOR;",
        "        }",
        "        else if ((x >= 8.0 && x <= 10.0 && y <= 2.0) || (y == 6.0 && x <= 4.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "        else if (pmx <= 4.0)",
        "        {",
        "            tile = TILE_EMPTY;",
        "        }",
        "        else",
        "        {",
        "            tile = TILE_SPIKES;",
        "        }",
        "    }",
        "    else if (abs(level - 10.0) < 0.5)",
        "    {",
        "        if ((pmx == 8.0 && y == 7.0) || (pmx == 7.0 && y == 5.0) || (y == 4.0 && pmx >= 8.0))",
        "        {",
        "            tile = TILE_SOLID;",
        "        }",
        "    }",
        "",
        "    return tile;",
        "}",
        "",
        "float tileAt(vec2 pos, float level)",
        "{",
        "    float tile = baseTile(pos, level);",
        "",
        "    if (tile == TILE_SPIKES && sameTile(pos, uAtariRock))",
        "    {",
        "        return TILE_SPIKES_SMASHED;",
        "    }",
        "",
        "    return tile;",
        "}",
        "",
        "vec3 levelBgColor(float level)",
        "{",
        "    if (level <= 3.5) return vec3(0.00, 0.00, 1.00);",
        "    if (level <= 6.5) return vec3(0.00, 0.50, 0.50);",
        "    if (level <= 9.5) return vec3(0.25, 0.25, 0.25);",
        "    return vec3(0.05, 0.05, 0.08);",
        "}",
        "",
        "vec3 levelFgColor(float level)",
        "{",
        "    if (level <= 3.5) return vec3(0.00, 0.50, 1.00);",
        "    if (level <= 6.5) return vec3(1.00, 0.00, 0.50);",
        "    if (level <= 9.5) return vec3(0.00, 0.50, 0.50);",
        "    return vec3(0.80, 0.80, 1.00);",
        "}",
        "",
        "void main()",
        "{",
        "    vec2 resolution = max(iResolution, vec2(1.0));",
        "    vec2 uv = gl_FragCoord.xy / resolution;",
        "",
        "    uv = 2.1 * uv - 1.05;",
        "    vec2 vignetteOffset = pow(abs(uv), vec2(8.0));",
        "    float vignette = 1.0 - 0.5 * max(vignetteOffset.x, vignetteOffset.y);",
        "    uv += uv * uv.yx * uv.yx * vec2(0.025, 0.05);",
        "    float cornerInset = 0.15;",
        "    vec2 cornerVec = max(abs(uv) + cornerInset - 1.0, 0.0) / cornerInset;",
        "    vignette *= 1.0 - smoothstep(1.0, 1.05, length(cornerVec));",
        "    uv = 0.5 * uv + 0.5;",
        "",
        "    if (uv.x <= 0.0 || uv.y <= 0.0 || uv.x >= 1.0 || uv.y >= 1.0)",
        "    {",
        "        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0) * vColor;",
        "        return;",
        "    }",
        "",
        "    vec2 atariXy = uv * ATARI_REZ;",
        "    vec2 pixel = floor(atariXy);",
        "    vec2 tilePos = floor(pixel / 8.0);",
        "    vec2 p = mod(pixel, 8.0);",
        "    vec2 pc = p - vec2(4.0);",
        "    vec2 apc = abs(pc);",
        "",
        "    float tile = tileAt(tilePos, uAtariLevel);",
        "",
        "    vec3 boxColor = vec3(0.0, 1.0, 0.5);",
        "    vec3 bgColor = levelBgColor(uAtariLevel);",
        "    vec3 fgColor = levelFgColor(uAtariLevel);",
        "    vec3 col = bgColor;",
        "",
        "    if (uAtariMode > MODE_LAMENTING - 0.1)",
        "    {",
        "        if (mod(pixel.y + 100.0 * iTime, 27.0) > 8.0)",
        "        {",
        "            col += vec3(0.1);",
        "        }",
        "    }",
        "    else if (uAtariFlash > 0.0)",
        "    {",
        "        col = mix(bgColor, boxColor, clamp(uAtariFlash, 0.0, 1.0));",
        "    }",
        "",
        "    if (abs(uAtariLevel - 10.0) < 0.5)",
        "    {",
        "        float n = hash(pixel + floor(iTime * 17.0));",
        "        col = 0.25 + 0.5 * cos(6.28318 * (vec3(0.25, 0.37, 0.45) + n * vec3(0.1, 0.2, 0.3) + 0.2 * iTime));",
        "    }",
        "",
        "    if (sameTile(tilePos, uAtariPlayer) && uAtariMode < MODE_LAMENTING - 0.1)",
        "    {",
        "        if (p.x >= 1.0 && p.x < 7.0 && p.y >= 1.0 && p.y < 7.0)",
        "        {",
        "            col = vec3(1.0, 0.5, 0.25);",
        "        }",
        "    }",
        "    else if (sameTile(tilePos, uAtariBox))",
        "    {",
        "        if (abs(apc.x + apc.y - 1.0) < 0.1 || (abs(apc.x - 2.0) < 0.1 && apc.y < 3.0) || (abs(apc.y - 2.0) < 0.1 && apc.x < 3.0))",
        "        {",
        "            col = boxColor;",
        "        }",
        "    }",
        "    else if (sameTile(tilePos, uAtariTarget))",
        "    {",
        "        if (abs(apc.x + apc.y - 1.0) > 0.1 && apc.x < 2.0 && apc.y < 2.0)",
        "        {",
        "            col = boxColor;",
        "        }",
        "    }",
        "    else if (sameTile(tilePos, uAtariRock))",
        "    {",
        "        float radius = length(p - vec2(4.0, 4.0));",
        "        if (radius <= 3.6)",
        "        {",
        "            col = vec3(1.0);",
        "        }",
        "    }",
        "    else if (tile == TILE_EXIT)",
        "    {",
        "        if (p.x < 1.0 || p.x >= 7.0 || p.y < 1.0 || p.y >= 7.0)",
        "        {",
        "            col = vec3(1.0, 0.5, 0.25);",
        "        }",
        "    }",
        "    else if (tile == TILE_SOLID)",
        "    {",
        "        col = fgColor;",
        "    }",
        "    else if (tile == TILE_SPIKES)",
        "    {",
        "        if (apc.x + apc.y < 3.0 || (abs(apc.x - 2.0) < 0.1 && abs(apc.y - 2.0) < 0.1))",
        "        {",
        "            col = vec3(1.0);",
        "            if (uAtariMode > MODE_LAMENTING - 0.1 && sameTile(tilePos, uAtariPlayer))",
        "            {",
        "                col = vec3(1.0, 0.0, 0.0);",
        "            }",
        "        }",
        "    }",
        "    else if (tile == TILE_SPIKES_SMASHED)",
        "    {",
        "        if ((p.x == 4.0 && p.y == 4.0) || (p.x == 6.0 && p.y == 5.0) || (p.x == 2.0 && p.y == 2.0) || (p.x == 5.0 && p.y == 2.0))",
        "        {",
        "            col = vec3(1.0);",
        "        }",
        "    }",
        "    else if (tile == TILE_DOOR)",
        "    {",
        "        if (uAtariDoorOpen < 0.5)",
        "        {",
        "            if (p.y == 3.0 || p.y == 4.0)",
        "            {",
        "                col = boxColor;",
        "            }",
        "        }",
        "        else",
        "        {",
        "            if (p.x < 1.0 || p.x > 6.0)",
        "            {",
        "                col = boxColor;",
        "            }",
        "        }",
        "    }",
        "",
        "    float scanAmt = mix(0.62, 1.0, step(0.5, fract(atariXy.y)));",
        "    float flicker = mod(floor(iTime * 60.0), 4.0) >= 2.0 ? 1.0 : 0.95;",
        "    col *= scanAmt * flicker * max(vignette, 0.0);",
        "",
        "    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0) * vColor;",
        "}"
    };
}
