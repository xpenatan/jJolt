package jolt.example.samples.app.tests.vehicle;

import com.badlogic.gdx.math.MathUtils;
import jolt.JoltNew;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.example.samples.app.jolt.Layers;
import jolt.example.samples.app.tests.Test;
import jolt.Jolt;
import jolt.math.ArrayVec3;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.collision.CollisionGroup;
import jolt.physics.collision.GroupFilterTable;
import jolt.physics.collision.shape.BoxShape;
import jolt.physics.collision.shape.ConvexHullShapeSettings;
import jolt.physics.collision.shape.Shape;
import jolt.physics.constraints.DistanceConstraintSettings;

public abstract class VehicleTest extends Test {

    @Override
    public void initialize() {
        // Scene with hilly terrain and some objects to drive into
        Body floor = CreateMeshTerrain();
        floor.SetFriction(1.0f);

        createBridge();

        createWall();

        createRubble();
    }

    private void createBridge() {
        int cChainLength = 20;

        // Build a collision group filter that disables collision between adjacent bodies
        GroupFilterTable group_filter = new GroupFilterTable(cChainLength);
        for (int i = 0; i < cChainLength - 1; ++i) {
            group_filter.DisableCollision(i, i + 1);
        }

        Vec3 part_half_size = JoltNew.Vec3(2.5f, 0.25f, 1.0f);
        Shape part_shape = new BoxShape(part_half_size);

        Vec3 large_part_half_size = JoltNew.Vec3(2.5f, 0.25f, 22.5f);
        Shape large_part_shape = new BoxShape(large_part_half_size);

        Quat first_part_rot = Quat.sRotation(Vec3.sAxisX(), MathUtils.degreesToRadians * -10.0f);

        Vec3 prev_pos = JoltNew.Vec3(-25, 7, 0);
        Body prev_part = null;

        Vec3 temp1 = JoltNew.Vec3();
        Vec3 temp2 = JoltNew.Vec3();

        for (int i = 0; i < cChainLength; ++i) {
            float prevX = prev_pos.GetX();
            float prevY = prev_pos.GetY();
            float prevZ = prev_pos.GetZ();

            Vec3 pos = JoltNew.Vec3(prevX, prevY, (2.0f * part_half_size.GetZ()) + prevZ);
            Vec3 val = JoltNew.Vec3(0, large_part_half_size.GetY() - part_half_size.GetY(), large_part_half_size.GetZ() - part_half_size.GetZ());

            Body part = null;
            if( i == 0) {
                BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(large_part_shape, pos.SubVec3(first_part_rot.MulVec3(val)), first_part_rot, EMotionType.Static, Layers.NON_MOVING);
                part = mBodyInterface.CreateBody(bodyCreationSettings);
                bodyCreationSettings.dispose();
            }
            else {
                BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(part_shape, pos, Quat.sIdentity(), i == 19 ? EMotionType.Static : EMotionType.Dynamic, i == 19 ? Layers.NON_MOVING : Layers.MOVING);
                part = mBodyInterface.CreateBody(bodyCreationSettings);
                bodyCreationSettings.dispose();
            }
            val.dispose();

            CollisionGroup collisionGroup = new CollisionGroup(group_filter, 1, i);
            part.SetCollisionGroup(collisionGroup);
            part.SetFriction(1.0f);
            collisionGroup.dispose();
            mBodyInterface.AddBody(part.GetID(), EActivation.Activate);

            if (prev_part != null) {
                DistanceConstraintSettings dc = new DistanceConstraintSettings();
                temp1.Set(-part_half_size.GetX(), 0, part_half_size.GetZ());
                dc.set_mPoint1(prev_pos.AddVec3(temp1));
                temp2.Set(-part_half_size.GetX(), 0, -part_half_size.GetZ());
                dc.set_mPoint2(pos.AddVec3(temp2));
                mPhysicsSystem.AddConstraint(dc.Create(prev_part, part));

                temp1.Set(part_half_size.GetX(), 0, part_half_size.GetZ());
                dc.set_mPoint1(prev_pos.AddVec3(temp1));
                temp2.Set(part_half_size.GetX(), 0, -part_half_size.GetZ());
                dc.set_mPoint2(pos.AddVec3(temp2));
                mPhysicsSystem.AddConstraint(dc.Create(prev_part, part));
                dc.dispose();
            }
            prev_pos.dispose();
            prev_part = part;
            prev_pos = pos;
        }

//        group_filter.dispose();
        large_part_half_size.dispose();
        part_half_size.dispose();
        temp1.dispose();
        temp2.dispose();
    }

    private void createWall() {
        Vec3 vec3 = JoltNew.Vec3(0.5f, 0.5f, 0.5f);
        Shape box_shape = new BoxShape(vec3);
        vec3.dispose();
        for (int i = 0; i < 3; ++i) {
            for (int j = i / 2; j < 5 - (i + 1) / 2; ++j)
            {
                Vec3 position = JoltNew.Vec3(2.0f + j * 1.0f + ((i & 1) == 1 ? 0.5f : 0.0f), 2.0f + i * 1.0f, 10.0f);
                BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(box_shape, position, Quat.sIdentity(), EMotionType.Dynamic, Layers.MOVING);
                mBodyInterface.CreateAndAddBody(bodyCreationSettings, EActivation.Activate);
                bodyCreationSettings.dispose();
                position.dispose();
            }
        }
    }

    private void createRubble() {
        // Flat and light objects
        Vec3 temp = JoltNew.Vec3(0.5f, 0.1f, 0.5f);
        Shape box_shape = new BoxShape(temp);
        temp.dispose();
        for (int i = 0; i < 5; ++i)
            for (int j = 0; j < 5; ++j)
            {
                Vec3 position = JoltNew.Vec3(-5.0f + j, 2.0f + i * 0.2f, 10.0f + 0.5f * i);
                BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(box_shape, position, Quat.sIdentity(), EMotionType.Dynamic, Layers.MOVING);
                mBodyInterface.CreateAndAddBody(bodyCreationSettings, EActivation.Activate);
                bodyCreationSettings.dispose();
                position.dispose();
            }


        // Light convex shapes
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                // Create random points
                ArrayVec3 points = new ArrayVec3();
                for (int k = 0; k < 20; ++k) {
                    float value = MathUtils.random(0.2f, 0.4f);
                    float theta = MathUtils.PI * MathUtils.random(0.0f, 1.0f);
                    float phi = 2.0f * MathUtils.PI * MathUtils.random(0.0f, 1.0f);
                    Vec3 vec3 = Vec3.sUnitSpherical(theta, phi);
                    Vec3 vec = JoltNew.Vec3(vec3.GetX() * value, vec3.GetY() * value, vec3.GetZ() * value);
                    points.push_back(vec);
                    vec.dispose();
                }
                ConvexHullShapeSettings convexHullShapeSettings = new ConvexHullShapeSettings(points);
                Vec3 vec3 = JoltNew.Vec3(-5.0f + 0.5f * j, 2.0f, 15.0f + 0.5f * i);
                BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(convexHullShapeSettings, vec3, Quat.sIdentity(), EMotionType.Dynamic, Layers.MOVING);
                mBodyInterface.CreateAndAddBody(bodyCreationSettings, EActivation.Activate);
                bodyCreationSettings.dispose();
                points.dispose();
                vec3.dispose();
            }
        }
    }
}