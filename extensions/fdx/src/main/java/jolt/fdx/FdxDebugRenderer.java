package jolt.fdx;

import com.github.xpenatan.jparser.runtime.helper.NativeFloatArray;
import io.github.libfdx.core.FdxException;
import io.github.libfdx.graphics.GraphicsContext;
import io.github.libfdx.graphics.ImmediateModeRenderer;
import io.github.libfdx.math.Matrix4;
import jolt.core.Color;
import jolt.enums.ECastShadow;
import jolt.enums.ECullMode;
import jolt.enums.EDrawMode;
import jolt.math.Mat44;
import jolt.math.Vec3;
import jolt.math.Vec4;
import jolt.physics.PhysicsSystem;
import jolt.physics.body.BodyManagerDrawSettings;
import jolt.renderer.DebugRendererEm;

public class FdxDebugRenderer extends DebugRendererEm {
    private static final int JOLT_FLOATS_PER_VERTEX = 12;
    private static final float[] IDENTITY_MATRIX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    private final ImmediateModeRenderer renderer;
    private final boolean ownsRenderer;
    private boolean enabled = true;
    private boolean drawShapeWireframe;

    public FdxDebugRenderer(GraphicsContext graphics) {
        this(new ImmediateModeRenderer(graphics), true);
    }

    public FdxDebugRenderer(ImmediateModeRenderer renderer) {
        this(renderer, false);
    }

    private FdxDebugRenderer(ImmediateModeRenderer renderer, boolean ownsRenderer) {
        if (renderer == null) {
            throw new FdxException("ImmediateModeRenderer cannot be null");
        }
        this.renderer = renderer;
        this.ownsRenderer = ownsRenderer;
    }

    public void render(Matrix4 viewProjection) {
        if (viewProjection == null) {
            throw new FdxException("View-projection matrix cannot be null");
        }
        render(viewProjection.values());
    }

    public void render(float[] viewProjection) {
        if (!enabled) {
            renderer.clear3D();
            return;
        }
        renderer.render3D(viewProjection);
        renderer.clear3D();
    }

    public void clear() {
        renderer.clear3D();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDrawShapeWireframe() {
        return drawShapeWireframe;
    }

    @Override
    public void DrawBodies(PhysicsSystem system, BodyManagerDrawSettings inDrawSettings) {
        drawShapeWireframe = inDrawSettings != null && inDrawSettings.get_mDrawShapeWireframe();
        super.DrawBodies(system, inDrawSettings);
    }

    @Override
    public void DrawCylinder(Mat44 inMatrix, float inHalfHeight, float inRadius, Color inColor,
            ECastShadow inCastShadow, EDrawMode inDrawMode) {
        EDrawMode drawMode = drawShapeWireframe ? EDrawMode.EDrawMode_Wireframe : inDrawMode;
        super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, inCastShadow, drawMode);
    }

    @Override
    protected void DrawMesh(int id, Mat44 inModelMatrix, NativeFloatArray vertices, Color inModelColor,
            ECullMode inCullMode, EDrawMode inDrawMode) {
        if (!enabled || vertices == null || vertices.getSize() == 0) {
            return;
        }
        int floatCount = vertices.getSize();
        if (floatCount % JOLT_FLOATS_PER_VERTEX != 0) {
            throw new FdxException("Jolt debug mesh vertex data must be 12 floats per vertex");
        }
        int vertexCount = floatCount / JOLT_FLOATS_PER_VERTEX;

        float[] transform = inModelMatrix != null ? JoltFdx.toMatrix4(inModelMatrix).values() : IDENTITY_MATRIX;
        float[] modelColor = color(inModelColor);
        if (inDrawMode == EDrawMode.EDrawMode_Wireframe) {
            for (int i = 0; i + 1 < vertexCount; i += 2) {
                line(vertex(vertices, i, transform, modelColor),
                        vertex(vertices, i + 1, transform, modelColor));
            }
            return;
        }
        if (vertexCount < 3) {
            return;
        }
        for (int i = 0; i + 2 < vertexCount; i += 3) {
            DebugVertex v1 = vertex(vertices, i, transform, modelColor);
            DebugVertex v2 = vertex(vertices, i + 1, transform, modelColor);
            DebugVertex v3 = vertex(vertices, i + 2, transform, modelColor);
            line(v1, v2);
            line(v2, v3);
            line(v3, v1);
        }
    }

    @Override
    protected void DrawLine(Vec3 inFrom, Vec3 inTo, Color inColor) {
        if (!enabled) {
            return;
        }
        float[] color = color(inColor);
        renderer.line3D(inFrom.GetX(), inFrom.GetY(), inFrom.GetZ(), inTo.GetX(), inTo.GetY(), inTo.GetZ(),
                color[0], color[1], color[2], color[3]);
    }

    @Override
    protected void DrawTriangle(Vec3 inV1, Vec3 inV2, Vec3 inV3, Color inColor, ECastShadow inCastShadow) {
        if (!enabled) {
            return;
        }
        float[] color = color(inColor);
        line(inV1, inV2, color);
        line(inV2, inV3, color);
        line(inV3, inV1, color);
    }

    @Override
    protected void onNativeDispose() {
        clear();
        if (ownsRenderer) {
            renderer.dispose();
        }
    }

    private void line(DebugVertex from, DebugVertex to) {
        float red = (from.red + to.red) * 0.5f;
        float green = (from.green + to.green) * 0.5f;
        float blue = (from.blue + to.blue) * 0.5f;
        float alpha = (from.alpha + to.alpha) * 0.5f;
        renderer.line3D(from.x, from.y, from.z, to.x, to.y, to.z, red, green, blue, alpha);
    }

    private void line(Vec3 from, Vec3 to, float[] color) {
        renderer.line3D(from.GetX(), from.GetY(), from.GetZ(), to.GetX(), to.GetY(), to.GetZ(),
                color[0], color[1], color[2], color[3]);
    }

    private static DebugVertex vertex(NativeFloatArray vertices, int index, float[] matrix, float[] modelColor) {
        int offset = index * JOLT_FLOATS_PER_VERTEX;
        float x = vertices.getValue(offset);
        float y = vertices.getValue(offset + 1);
        float z = vertices.getValue(offset + 2);
        float red = clamp(vertices.getValue(offset + 8) * modelColor[0]);
        float green = clamp(vertices.getValue(offset + 9) * modelColor[1]);
        float blue = clamp(vertices.getValue(offset + 10) * modelColor[2]);
        float alpha = clamp(vertices.getValue(offset + 11) * modelColor[3]);
        return new DebugVertex(
                matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
                matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
                matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14],
                red, green, blue, alpha);
    }

    private static float[] color(Color color) {
        if (color == null) {
            return new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        }
        Vec4 vec = color.ToVec4();
        return new float[] {
                clamp(vec.GetX()),
                clamp(vec.GetY()),
                clamp(vec.GetZ()),
                clamp(vec.GetW())
        };
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static final class DebugVertex {
        private final float x;
        private final float y;
        private final float z;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private DebugVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }
}
