package jolt.gdx;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import jolt.RVec3;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.math.Vec4;

public final class JoltGdx {

    private JoltGdx() {
    }

    public static Matrix4 convert(Mat44 in, Matrix4 out) {
        float[] outArray = out.val;
        // Column-major order: copy each column directly
        for (int col = 0; col < 4; col++) {
            Vec4 vec4 = in.GetColumn4(col);
            for (int row = 0; row < 4; row++) {
                float val = vec4.GetComponent(row);
                outArray[col * 4 + row] = val; // m[col][row]
            }
        }
        return out;
    }

    public static Mat44 convert(Matrix4 in, Mat44 out) {
        float[] outArray = in.val; // Source Matrix4 data
        for (int col = 0; col < 4; col++) {
            // Extract column elements and create Vec4
            Vec4 vec4 = out.GetColumn4(col);
            vec4.Set(
                outArray[col * 4 + 0], // Row 0
                outArray[col * 4 + 1], // Row 1
                outArray[col * 4 + 2], // Row 2
                outArray[col * 4 + 3]  // Row 3
            );
            out.SetColumn4(col, vec4); // Set column in destination Mat44
        }
        return out;
    }

    public static Vector3 convert(Vec3 in, Vector3 out) {
        out.set(in.GetX(), in.GetY(), in.GetZ());
        return out;
    }

    public static Vec3 convert(Vector3 in, Vec3 out) {
        out.Set(in.x, in.y, in.z);
        return out;
    }

    public static Vector3 convert(RVec3 in, Vector3 out) {
        out.set(in.GetX(), in.GetY(), in.GetZ());
        return out;
    }

    public static RVec3 convert(Vector3 in, RVec3 out) {
        out.Set(in.x, in.y, in.z);
        return out;
    }

    public static Vector4 convert(Vec4 in, Vector4 out) {
        out.set(in.GetX(), in.GetY(), in.GetZ(), in.GetW());
        return out;
    }

    public static Vec4 convert(Vector4 in, Vec4 out) {
        out.Set(in.x, in.y, in.z, in.w);
        return out;
    }

    public static Quaternion convert(Quat in, Quaternion out) {
        out.set(in.GetX(), in.GetY(), in.GetZ(), in.GetW());
        return out;
    }

    public static Quat convert(Quaternion in, Quat out) {
        out.Set(in.x, in.y, in.z, in.w);
        return out;
    }
}
