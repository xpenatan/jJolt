package jolt.example.samples.app.math;

public class Perlin {
    // Static array equivalent to stb_perlin_randtab

    private static final short[] PERLIN_RANDTAB = {
            23, 125, 161, 52, 103, 117, 70, 37, 247, 101, 203, 169, 124, 126, 44, 123,
            152, 238, 145, 45, 171, 114, 253, 10, 192, 136, 4, 157, 249, 30, 35, 72,
            175, 63, 77, 90, 181, 16, 96, 111, 133, 104, 75, 162, 93, 56, 66, 240,
            8, 50, 84, 229, 49, 210, 173, 239, 141, 1, 87, 18, 2, 198, 143, 57,
            225, 160, 58, 217, 168, 206, 245, 204, 199, 6, 73, 60, 20, 230, 211, 233,
            94, 200, 88, 9, 74, 155, 33, 15, 219, 130, 226, 202, 83, 236, 42, 172,
            165, 218, 55, 222, 46, 107, 98, 154, 109, 67, 196, 178, 127, 158, 13, 243,
            65, 79, 166, 248, 25, 224, 115, 80, 68, 51, 184, 128, 232, 208, 151, 122,
            26, 212, 105, 43, 179, 213, 235, 148, 146, 89, 14, 195, 28, 78, 112, 76,
            250, 47, 24, 251, 140, 108, 186, 190, 228, 170, 183, 139, 39, 188, 244, 246,
            132, 48, 119, 144, 180, 138, 134, 193, 82, 182, 120, 121, 86, 220, 209, 3,
            91, 241, 149, 85, 205, 150, 113, 216, 31, 100, 41, 164, 177, 214, 153, 231,
            38, 71, 185, 174, 97, 201, 29, 95, 7, 92, 54, 254, 191, 118, 34, 221,
            131, 11, 163, 99, 234, 81, 227, 147, 156, 176, 17, 142, 69, 12, 110, 62,
            27, 255, 0, 194, 59, 116, 242, 252, 19, 21, 187, 53, 207, 129, 64, 135,
            61, 40, 167, 237, 102, 223, 106, 159, 197, 189, 215, 137, 36, 32, 22, 5,
            // Second copy
            23, 125, 161, 52, 103, 117, 70, 37, 247, 101, 203, 169, 124, 126, 44, 123,
            152, 238, 145, 45, 171, 114, 253, 10, 192, 136, 4, 157, 249, 30, 35, 72,
            175, 63, 77, 90, 181, 16, 96, 111, 133, 104, 75, 162, 93, 56, 66, 240,
            8, 50, 84, 229, 49, 210, 173, 239, 141, 1, 87, 18, 2, 198, 143, 57,
            225, 160, 58, 217, 168, 206, 245, 204, 199, 6, 73, 60, 20, 230, 211, 233,
            94, 200, 88, 9, 74, 155, 33, 15, 219, 130, 226, 202, 83, 236, 42, 172,
            165, 218, 55, 222, 46, 107, 98, 154, 109, 67, 196, 178, 127, 158, 13, 243,
            65, 79, 166, 248, 25, 224, 115, 80, 68, 51, 184, 128, 232, 208, 151, 122,
            26, 212, 105, 43, 179, 213, 235, 148, 146, 89, 14, 195, 28, 78, 112, 76,
            250, 47, 24, 251, 140, 108, 186, 190, 228, 170, 183, 139, 39, 188, 244, 246,
            132, 48, 119, 144, 180, 138, 134, 193, 82, 182, 120, 121, 86, 220, 209, 3,
            91, 241, 149, 85, 205, 150, 113, 216, 31, 100, 41, 164, 177, 214, 153, 231,
            38, 71, 185, 174, 97, 201, 29, 95, 7, 92, 54, 254, 191, 118, 34, 221,
            131, 11, 163, 99, 234, 81, 227, 147, 156, 176, 17, 142, 69, 12, 110, 62,
            27, 255, 0, 194, 59, 116, 242, 252, 19, 21, 187, 53, 207, 129, 64, 135,
            61, 40, 167, 237, 102, 223, 106, 159, 197, 189, 215, 137, 36, 32, 22, 5
    };

    private static float perlinLerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int perlinFastFloor(float a) {
        int ai = (int) a;
        return (a < ai) ? ai - 1 : ai;
    }

    private static final float[][] BASIS = {
            { 1, 1, 0 },
            { -1, 1, 0 },
            { 1, -1, 0 },
            { -1, -1, 0 },
            { 1, 0, 1 },
            { -1, 0, 1 },
            { 1, 0, -1 },
            { -1, 0, -1 },
            { 0, 1, 1 },
            { 0, -1, 1 },
            { 0, 1, -1 },
            { 0, -1, -1 }
    };

    private static final byte[] INDICES = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            0, 9, 1, 11,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    private static float perlinGrad(int hash, float x, float y, float z) {
        float[] grad = BASIS[INDICES[hash & 63]];
        return grad[0] * x + grad[1] * y + grad[2] * z;
    }

    public static float perlinNoise3(float x, float y, float z, int x_wrap, int y_wrap, int z_wrap) {
        float u, v, w;
        float n000, n001, n010, n011, n100, n101, n110, n111;
        float n00, n01, n10, n11;
        float n0, n1;

        int x_mask = (x_wrap - 1) & 255;
        int y_mask = (y_wrap - 1) & 255;
        int z_mask = (z_wrap - 1) & 255;
        int px = perlinFastFloor(x);
        int py = perlinFastFloor(y);
        int pz = perlinFastFloor(z);
        int x0 = px & x_mask, x1 = (px + 1) & x_mask;
        int y0 = py & y_mask, y1 = (py + 1) & y_mask;
        int z0 = pz & z_mask, z1 = (pz + 1) & z_mask;
        int r0, r1, r00, r01, r10, r11;

        x -= px; u = perlinEase(x);
        y -= py; v = perlinEase(y);
        z -= pz; w = perlinEase(z);

        r0 = PERLIN_RANDTAB[x0] & 0xFF;  // Convert byte to unsigned int
        r1 = PERLIN_RANDTAB[x1] & 0xFF;

        r00 = PERLIN_RANDTAB[r0 + y0] & 0xFF;
        r01 = PERLIN_RANDTAB[r0 + y1] & 0xFF;
        r10 = PERLIN_RANDTAB[r1 + y0] & 0xFF;
        r11 = PERLIN_RANDTAB[r1 + y1] & 0xFF;

        n000 = perlinGrad(PERLIN_RANDTAB[r00 + z0] & 0xFF, x, y, z);
        n001 = perlinGrad(PERLIN_RANDTAB[r00 + z1] & 0xFF, x, y, z - 1);
        n010 = perlinGrad(PERLIN_RANDTAB[r01 + z0] & 0xFF, x, y - 1, z);
        n011 = perlinGrad(PERLIN_RANDTAB[r01 + z1] & 0xFF, x, y - 1, z - 1);
        n100 = perlinGrad(PERLIN_RANDTAB[r10 + z0] & 0xFF, x - 1, y, z);
        n101 = perlinGrad(PERLIN_RANDTAB[r10 + z1] & 0xFF, x - 1, y, z - 1);
        n110 = perlinGrad(PERLIN_RANDTAB[r11 + z0] & 0xFF, x - 1, y - 1, z);
        n111 = perlinGrad(PERLIN_RANDTAB[r11 + z1] & 0xFF, x - 1, y - 1, z - 1);

        n00 = perlinLerp(n000, n001, w);
        n01 = perlinLerp(n010, n011, w);
        n10 = perlinLerp(n100, n101, w);
        n11 = perlinLerp(n110, n111, w);

        n0 = perlinLerp(n00, n01, v);
        n1 = perlinLerp(n10, n11, v);

        return perlinLerp(n0, n1, u);
    }

    private static float perlinEase(float a) {
        return (((a * 6 - 15) * a + 10) * a * a * a);
    }

    public static float perlinRidgeNoise3(float x, float y, float z, float lacunarity, float gain, float offset, int octaves, int x_wrap, int y_wrap, int z_wrap) {
        float frequency = 1.0f;
        float prev = 1.0f;
        float amplitude = 0.5f;
        float sum = 0.0f;

        for (int i = 0; i < octaves; i++) {
            float r = perlinNoise3(x * frequency, y * frequency, z * frequency, x_wrap, y_wrap, z_wrap);
            r = r < 0 ? -r : r; // abs()
            r = offset - r;
            r = r * r;
            sum += r * amplitude * prev;
            prev = r;
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return sum;
    }

    public static float perlinFBMNoise3(float x, float y, float z, float lacunarity, float gain, int octaves, int x_wrap, int y_wrap, int z_wrap) {
        float frequency = 1.0f;
        float amplitude = 1.0f;
        float sum = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += perlinNoise3(x * frequency, y * frequency, z * frequency, x_wrap, y_wrap, z_wrap) * amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return sum;
    }

    public static float perlinTurbulenceNoise3(float x, float y, float z, float lacunarity, float gain, int octaves, int x_wrap, int y_wrap, int z_wrap) {
        float frequency = 1.0f;
        float amplitude = 1.0f;
        float sum = 0.0f;

        for (int i = 0; i < octaves; i++) {
            float r = perlinNoise3(x * frequency, y * frequency, z * frequency, x_wrap, y_wrap, z_wrap) * amplitude;
            r = r < 0 ? -r : r; // abs()
            sum += r;
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return sum;
    }
}
