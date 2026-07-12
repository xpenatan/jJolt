package jolt.fdx;

import io.github.libfdx.math.Matrix4;
import jolt.core.Color;
import jolt.math.Mat44;
import jolt.math.Vec4;

/**
 * Conversion helpers between Jolt generated math types and libfdx math types.
 */
public final class JoltFdx {
    private JoltFdx() {
    }

    public static Matrix4 toMatrix4(Mat44 in) {
        return convert(in, new Matrix4());
    }

    public static Matrix4 convert(Mat44 in, Matrix4 out) {
        float[] values = new float[Matrix4.VALUE_COUNT];
        for (int col = 0; col < 4; col++) {
            Vec4 column = in.GetColumn4(col);
            for (int row = 0; row < 4; row++) {
                values[col * 4 + row] = column.GetComponent(row);
            }
        }
        out.set(values);
        return out;
    }

    public static io.github.libfdx.math.Color toColor(Color in) {
        Vec4 color = in.ToVec4();
        return new io.github.libfdx.math.Color(
                color.GetX(),
                color.GetY(),
                color.GetZ(),
                color.GetW());
    }
}
