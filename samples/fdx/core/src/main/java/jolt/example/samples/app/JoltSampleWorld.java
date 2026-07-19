package jolt.example.samples.app;

import io.github.libfdx.math.Matrix4;
import jolt.JoltInterface;
import jolt.JoltNew;
import jolt.JoltSettings;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.PhysicsSystem;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyID;
import jolt.physics.body.BodyInterface;
import jolt.physics.collision.ObjectLayerPairFilterTable;
import jolt.physics.collision.broadphase.BroadPhaseLayer;
import jolt.physics.collision.broadphase.BroadPhaseLayerInterfaceTable;
import jolt.physics.collision.broadphase.ObjectVsBroadPhaseLayerFilterTable;
import jolt.physics.collision.shape.BoxShape;

final class JoltSampleWorld {
    private static final int LAYER_NON_MOVING = 0;
    private static final int LAYER_MOVING = 1;
    private static final int LAYER_COUNT = 2;
    private static final int BROAD_PHASE_LAYER_COUNT = 2;

    private final PhysicsSystem physicsSystem;
    private final JoltInterface joltInterface;
    private final BroadPhaseLayer broadPhaseNonMoving;
    private final BroadPhaseLayer broadPhaseMoving;
    private final BroadPhaseLayerInterfaceTable broadPhaseLayerInterface;
    private final ObjectLayerPairFilterTable objectLayerPairFilter;
    private final ObjectVsBroadPhaseLayerFilterTable objectVsBroadPhaseLayerFilter;
    private final Body floorBody;
    private final Body dynamicBoxBody;

    JoltSampleWorld() {
        objectLayerPairFilter = new ObjectLayerPairFilterTable(LAYER_COUNT);
        objectLayerPairFilter.EnableCollision(LAYER_NON_MOVING, LAYER_MOVING);
        objectLayerPairFilter.EnableCollision(LAYER_MOVING, LAYER_MOVING);

        broadPhaseLayerInterface = new BroadPhaseLayerInterfaceTable(LAYER_COUNT, BROAD_PHASE_LAYER_COUNT);
        broadPhaseNonMoving = new BroadPhaseLayer((short)0);
        broadPhaseMoving = new BroadPhaseLayer((short)1);
        broadPhaseLayerInterface.MapObjectToBroadPhaseLayer(LAYER_NON_MOVING, broadPhaseNonMoving);
        broadPhaseLayerInterface.MapObjectToBroadPhaseLayer(LAYER_MOVING, broadPhaseMoving);
        objectVsBroadPhaseLayerFilter = new ObjectVsBroadPhaseLayerFilterTable(
                broadPhaseLayerInterface, BROAD_PHASE_LAYER_COUNT, objectLayerPairFilter, LAYER_COUNT);

        JoltSettings settings = new JoltSettings();
        settings.set_mMaxBodies(1024);
        settings.set_mMaxBodyPairs(2048);
        settings.set_mMaxContactConstraints(1024);
        settings.set_mTempAllocatorSize(10 * 1024 * 1024);
        settings.set_mMaxWorkerThreads(16);
        settings.set_mBroadPhaseLayerInterface(broadPhaseLayerInterface);
        settings.set_mObjectVsBroadPhaseLayerFilter(objectVsBroadPhaseLayerFilter);
        settings.set_mObjectLayerPairFilter(objectLayerPairFilter);
        joltInterface = new JoltInterface(settings);
        settings.dispose();

        physicsSystem = joltInterface.GetPhysicsSystem();
        floorBody = createBox(0.0f, -0.25f, 0.0f, 11.0f, 0.25f, 11.0f, EMotionType.Static, LAYER_NON_MOVING);
        dynamicBoxBody = createBox(0.0f, 6.0f, 0.0f, 0.5f, 0.5f, 0.5f, EMotionType.Dynamic, LAYER_MOVING);
    }

    void update(float deltaSeconds) {
        float step = deltaSeconds > 0.0f ? deltaSeconds : 1.0f / 60.0f;
        int collisionSteps = step > 1.0f / 55.0f ? 2 : 1;
        joltInterface.Step(step, collisionSteps);
        Vec3 position = dynamicBoxBody.GetPosition();
        if (position.GetY() < -10.0f) {
            resetDynamicBox();
        }
    }

    Matrix4 dynamicBoxTransform() {
        return bodyTransform(dynamicBoxBody);
    }

    PhysicsSystem physicsSystem() {
        return physicsSystem;
    }

    private Body createBox(float x, float y, float z, float halfX, float halfY, float halfZ,
            EMotionType motionType, int layer) {
        Vec3 halfExtent = JoltNew.Vec3(halfX, halfY, halfZ);
        Vec3 position = JoltNew.Vec3(x, y, z);
        Quat rotation = Quat.sIdentity();
        BoxShape shape = new BoxShape(halfExtent, 0.0f);
        BodyCreationSettings settings = JoltNew.BodyCreationSettings(shape, position, rotation, motionType, layer);
        Body body = physicsSystem.GetBodyInterface().CreateBody(settings);
        physicsSystem.GetBodyInterface().AddBody(body.GetID(),
                motionType == EMotionType.Dynamic ? EActivation.Activate : EActivation.DontActivate);
        settings.dispose();
        halfExtent.dispose();
        position.dispose();
        return body;
    }

    private Matrix4 bodyTransform(Body body) {
        Vec3 position = body.GetPosition();
        Quat rotation = body.GetRotation();
        return Matrix4.translation(position.GetX(), position.GetY(), position.GetZ())
                .mul(Matrix4.rotationQuaternion(rotation.GetX(), rotation.GetY(), rotation.GetZ(), rotation.GetW()));
    }

    private void resetDynamicBox() {
        BodyInterface bodyInterface = physicsSystem.GetBodyInterface();
        BodyID id = dynamicBoxBody.GetID();
        Vec3 position = JoltNew.Vec3(0.0f, 6.0f, 0.0f);
        Quat rotation = Quat.sIdentity();
        Vec3 linearVelocity = JoltNew.Vec3(0.0f, 0.0f, 0.0f);
        Vec3 angularVelocity = JoltNew.Vec3(0.0f, 0.0f, 0.0f);
        bodyInterface.SetPositionRotationAndVelocity(id, position, rotation, linearVelocity, angularVelocity);
        bodyInterface.ActivateBody(id);
        position.dispose();
        linearVelocity.dispose();
        angularVelocity.dispose();
    }

    void dispose() {
        BodyInterface bodyInterface = physicsSystem.GetBodyInterface();
        destroyBody(bodyInterface, dynamicBoxBody);
        destroyBody(bodyInterface, floorBody);
        broadPhaseNonMoving.dispose();
        broadPhaseMoving.dispose();
        joltInterface.dispose();
    }

    private void destroyBody(BodyInterface bodyInterface, Body body) {
        if (body == null) {
            return;
        }
        BodyID id = body.GetID();
        bodyInterface.RemoveBody(id);
        bodyInterface.DestroyBody(id);
    }
}
