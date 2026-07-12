package jolt.example.samples.app.tests.shapes;

import com.badlogic.gdx.math.Vector3;
import jolt.JoltNew;
import jolt.JoltTemp;
import jolt.enums.EMotionType;
import jolt.example.samples.app.jolt.Layers;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyInterface;
import jolt.physics.collision.shape.BoxShape;

public class ShapeHelper {
    public static Body createBox(int layer, BodyInterface mBodyInterface, int userData, Vector3 inHalfExtent, Vector3 inPosition, Quat inRotation) {
        Vec3 inHalfExtentJolt = JoltTemp.Vec3_1(inHalfExtent.x, inHalfExtent.y, inHalfExtent.z);
        Vec3 inPositionJolt = JoltTemp.Vec3_2(inPosition.x, inPosition.y, inPosition.z);
        BoxShape bodyShape = new BoxShape(inHalfExtentJolt);
        BodyCreationSettings bodySettings = JoltNew.BodyCreationSettings(bodyShape, inPositionJolt, inRotation, EMotionType.Dynamic, Layers.MOVING);
        Body body = mBodyInterface.CreateBody(bodySettings);
        bodySettings.dispose();
        body.SetUserData(userData);
        return body;
    }
}
