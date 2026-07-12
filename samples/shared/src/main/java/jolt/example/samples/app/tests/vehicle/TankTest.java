package jolt.example.samples.app.tests.vehicle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import jolt.Jolt;
import jolt.JoltNew;
import jolt.core.Color;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.enums.EMotorState;
import jolt.enums.EOverrideMassProperties;
import jolt.example.samples.app.jolt.Layers;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.collision.GroupFilter;
import jolt.physics.collision.GroupFilterTable;
import jolt.physics.collision.shape.BoxShape;
import jolt.physics.collision.shape.CylinderShape;
import jolt.physics.collision.shape.OffsetCenterOfMassShapeSettings;
import jolt.physics.collision.shape.Shape;
import jolt.physics.constraints.HingeConstraint;
import jolt.physics.constraints.HingeConstraintSettings;
import jolt.physics.constraints.MotorSettings;
import jolt.physics.vehicle.TrackedVehicleController;
import jolt.physics.vehicle.TrackedVehicleControllerSettings;
import jolt.physics.vehicle.VehicleCollisionTesterRay;
import jolt.physics.vehicle.VehicleConstraint;
import jolt.physics.vehicle.VehicleConstraintSettings;
import jolt.physics.vehicle.VehicleController;
import jolt.physics.vehicle.VehicleTrackSettings;
import jolt.physics.vehicle.WheelSettings;
import jolt.physics.vehicle.WheelSettingsTV;
import jolt.physics.vehicle.Wheels;

public class TankTest extends VehicleTest {
    Body mTankBody;                                  ///< The body of the tank
    Body mTurretBody;                                ///< The body of the turret of the tank
    Body mBarrelBody;                                ///< The body of the barrel of the tank
    VehicleConstraint mVehicleConstraint;            ///< The vehicle constraint
    HingeConstraint mTurretHinge;                    ///< Hinge connecting tank body and turret
    HingeConstraint mBarrelHinge;                    ///< Hinge connecting tank turret and barrel
    float mReloadTime = 0.0f;                        ///< How long it still takes to reload the main gun
    Vec3 mCameraPivot = Vec3.sZero();                ///< The camera pivot, recorded before the physics update to align with the drawn world

    // Player input
    float mForward = 0.0f;
    float mPreviousForward = 1.0f;                   ///< Keeps track of last car direction so we know when to brake and when to accelerate
    float mLeftRatio = 0.0f;
    float mRightRatio = 0.0f;
    float mBrake = 0.0f;
    float mTurretHeading = 0.0f;
    float mBarrelPitch = 0.0f;
    boolean mFire = false;

    private Vec3[] wheel_pos;

    private TrackedVehicleController temp = TrackedVehicleController.native_new();

    @Override
    public void initialize() {
        super.initialize();
        createTank();
    }

    private void createTank() {
        final float wheel_radius = 0.3f;
        final float wheel_width = 0.1f;
        final float half_vehicle_length = 3.2f;
        final float half_vehicle_width = 1.7f;
        final float half_vehicle_height = 0.5f;
        final float suspension_min_length = 0.3f;
        final float suspension_max_length = 0.5f;
        final float suspension_frequency = 1.0f;

        final float half_turret_width = 1.4f;
        final float half_turret_length = 2.0f;
        final float half_turret_height = 0.4f;

        final float half_barrel_length = 1.5f;
        final float barrel_radius = 0.1f;
        final float barrel_rotation_offset = 0.2f;

        wheel_pos = new Vec3[]{
                JoltNew.Vec3(0.0f, -0.0f, 2.95f),
                JoltNew.Vec3(0.0f, -0.3f, 2.1f),
                JoltNew.Vec3(0.0f, -0.3f, 1.4f),
                JoltNew.Vec3(0.0f, -0.3f, 0.7f),
                JoltNew.Vec3(0.0f, -0.3f, 0.0f),
                JoltNew.Vec3(0.0f, -0.3f, -0.7f),
                JoltNew.Vec3(0.0f, -0.3f, -1.4f),
                JoltNew.Vec3(0.0f, -0.3f, -2.1f),
                JoltNew.Vec3(0.0f, -0.0f, -2.75f)
        };

        // Create filter to prevent body, turret and barrel from colliding
        GroupFilter filter = new GroupFilterTable();

        // Create tank body
        Vec3 body_position = JoltNew.Vec3(0, 2, 0);
        Shape tank_body_shape = new OffsetCenterOfMassShapeSettings(JoltNew.Vec3(0, -half_vehicle_height, 0), new BoxShape(JoltNew.Vec3(half_vehicle_width, half_vehicle_height, half_vehicle_length))).Create().Get();
        BodyCreationSettings tank_body_settings = JoltNew.BodyCreationSettings(tank_body_shape, body_position, Quat.sIdentity(), EMotionType.Dynamic, Layers.MOVING);
        tank_body_settings.get_mCollisionGroup().SetGroupFilter(filter);
        tank_body_settings.get_mCollisionGroup().SetGroupID(0);
        tank_body_settings.get_mCollisionGroup().SetSubGroupID(0);
        tank_body_settings.set_mOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        tank_body_settings.get_mMassPropertiesOverride().set_mMass(4000.0f);
        mTankBody = mBodyInterface.CreateBody(tank_body_settings);
        mBodyInterface.AddBody(mTankBody.GetID(), EActivation.Activate);

        // Create vehicle constraint
        VehicleConstraintSettings vehicle = new VehicleConstraintSettings();
        vehicle.set_mDrawConstraintSize(0.1f);
        vehicle.set_mMaxPitchRollAngle(MathUtils.degreesToRadians * 60.0f);

        TrackedVehicleControllerSettings controller = new TrackedVehicleControllerSettings();
        vehicle.set_mController(controller);

        for (int t = 0; t < 2; ++t)
        {
            VehicleTrackSettings track = controller.get_mTracks(t);

            // Last wheel is driven wheel
            track.set_mDrivenWheel((int)(vehicle.get_mWheels().size() + wheel_pos.length - 1));

            for (int wheel = 0; wheel < wheel_pos.length; ++wheel)
            {
                WheelSettingsTV w = new WheelSettingsTV();
                w.set_mPosition(wheel_pos[wheel]);
                w.get_mPosition().SetX(t == 0? half_vehicle_width : -half_vehicle_width);
                w.set_mRadius(wheel_radius);
                w.set_mWidth(wheel_width);
                w.set_mSuspensionMinLength(suspension_min_length);
                w.set_mSuspensionMaxLength(wheel == 0 || wheel == wheel_pos.length - 1? suspension_min_length : suspension_max_length);
                w.get_mSuspensionSpring().set_mFrequency(suspension_frequency);

                // Add the wheel to the vehicle
                track.get_mWheels().push_back(vehicle.get_mWheels().size());
                vehicle.get_mWheels().push_back(w);
            }
        }

        mVehicleConstraint = new VehicleConstraint(mTankBody, vehicle);
        mVehicleConstraint.SetVehicleCollisionTester(new VehicleCollisionTesterRay(Layers.MOVING));

        mPhysicsSystem.AddConstraint(mVehicleConstraint);
        mPhysicsSystem.AddStepListener(mVehicleConstraint);

        // Create turret
        Vec3 turret_position = JoltNew.Vec3(0, half_vehicle_height + half_turret_height, 0).Add(body_position);
        BodyCreationSettings turret_body_setings = JoltNew.BodyCreationSettings(new BoxShape(JoltNew.Vec3(half_turret_width, half_turret_height, half_turret_length)), turret_position, Quat.sIdentity(), EMotionType.Dynamic, Layers.MOVING);
        turret_body_setings.get_mCollisionGroup().SetGroupFilter(filter);
        turret_body_setings.get_mCollisionGroup().SetGroupID(0);
        turret_body_setings.get_mCollisionGroup().SetSubGroupID(0);
        turret_body_setings.set_mOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        turret_body_setings.get_mMassPropertiesOverride().set_mMass(2000.0f);
        mTurretBody = mBodyInterface.CreateBody(turret_body_setings);
        mBodyInterface.AddBody(mTurretBody.GetID(), EActivation.Activate);

        // Attach turret to body
        HingeConstraintSettings turret_hinge = new HingeConstraintSettings();
        Vec3 turretPoint = body_position.AddVec3(JoltNew.Vec3(0, half_vehicle_height, 0));
        turret_hinge.set_mPoint1(turretPoint);
        turret_hinge.set_mPoint2(turretPoint);
        turret_hinge.set_mHingeAxis1(Vec3.sAxisY());
        turret_hinge.set_mHingeAxis2(Vec3.sAxisY());
        turret_hinge.set_mNormalAxis1(Vec3.sAxisZ());
        turret_hinge.set_mNormalAxis2(Vec3.sAxisZ());
        turret_hinge.set_mMotorSettings(new MotorSettings(0.5f, 1.0f));
        mTurretHinge = HingeConstraint.native_new();
        mTurretHinge.native_copy(turret_hinge.Create(mTankBody, mTurretBody)); // TODO create return TwoBodyConstraint ?
        mTurretHinge.native_takeOwnership();
        mTurretHinge.SetMotorState(EMotorState.Position);
        mPhysicsSystem.AddConstraint(mTurretHinge);

        // Create barrel
        Vec3 barrel_position = JoltNew.Vec3(0, 0, half_turret_length + half_barrel_length - barrel_rotation_offset).Add(turret_position);
        BodyCreationSettings barrel_body_setings = JoltNew.BodyCreationSettings(new CylinderShape(half_barrel_length, barrel_radius), barrel_position, Quat.sRotation(Vec3.sAxisX(), 0.5f * MathUtils.PI), EMotionType.Dynamic, Layers.MOVING);
        barrel_body_setings.get_mCollisionGroup().SetGroupFilter(filter);
        barrel_body_setings.get_mCollisionGroup().SetGroupID(0);
        barrel_body_setings.get_mCollisionGroup().SetSubGroupID(0);
        barrel_body_setings.set_mOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        barrel_body_setings.get_mMassPropertiesOverride().set_mMass(200.0f);
        mBarrelBody = mBodyInterface.CreateBody(barrel_body_setings);
        mBodyInterface.AddBody(mBarrelBody.GetID(), EActivation.Activate);

        // Attach barrel to turret
        HingeConstraintSettings barrel_hinge = new HingeConstraintSettings();
        Vec3 barrelPoint = barrel_position.SubVec3(JoltNew.Vec3(0, 0, half_barrel_length));
        barrel_hinge.set_mPoint1(barrelPoint);
        barrel_hinge.set_mPoint2(barrelPoint);
        Vec3 negAxisX = Vec3.sAxisX().Mul(-1);
        barrel_hinge.set_mHingeAxis1(negAxisX);
        barrel_hinge.set_mHingeAxis2(negAxisX);
        barrel_hinge.set_mNormalAxis1(Vec3.sAxisZ());
        barrel_hinge.set_mNormalAxis2(Vec3.sAxisZ());
        barrel_hinge.set_mLimitsMin(MathUtils.degreesToRadians * -10.0f);
        barrel_hinge.set_mLimitsMax(MathUtils.degreesToRadians * 40.0f);
        barrel_hinge.set_mMotorSettings(new MotorSettings(10.0f, 1.0f));

        mBarrelHinge = HingeConstraint.native_new();
        mBarrelHinge.native_copy(barrel_hinge.Create(mTurretBody, mBarrelBody)); // TODO create return TwoBodyConstraint ?
        mBarrelHinge.native_takeOwnership();
        mBarrelHinge.SetMotorState(EMotorState.Position);
        mPhysicsSystem.AddConstraint(mBarrelHinge);
    }

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        super.prePhysicsUpdate(isPlaying);
        if(mTankBody == null) {
            return;
        }

        // Assure the tank stays active as we're controlling the turret with the mouse
        mBodyInterface.ActivateBody(mTankBody.GetID());

        // Pass the input on to the constraint
        VehicleController vehicleController = mVehicleConstraint.GetController();
        TrackedVehicleController controller = temp;
        controller.native_copy(vehicleController);

        controller.SetDriverInput(mForward, mLeftRatio, mRightRatio, mBrake);

        Wheels wheels = mVehicleConstraint.GetWheels();
        int size = wheels.size();
        for (int w = 0; w < size; ++w)
        {
		    WheelSettings settings = wheels.at(w).GetSettings();
            Mat44 wheel_transform = mVehicleConstraint.GetWheelWorldTransform(w, Vec3.sAxisY(), Vec3.sAxisX()); // The cylinder we draw is aligned with Y so we specify that as rotational axis
            mDebugRenderer.DrawCylinder(wheel_transform, 0.5f * settings.get_mWidth(), settings.get_mRadius(), Color.get_sGreen());
        }
    }

    public void processInput() {
        if(mTankBody == null) {
            return;
        }

        final float min_velocity_pivot_turn = 1.0f;

        // Determine acceleration and brake
        mForward = 0.0f;
        mBrake = 0.0f;

        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            mBrake = 1.0f;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            mForward = 1.0f;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            mForward = -1.0f;
        }

        // Steering
        mLeftRatio = 1.0f;
        mRightRatio = 1.0f;
        float velocity = (mTankBody.GetRotation().Conjugated().MulVec3(mTankBody.GetLinearVelocity()).GetZ());
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
        {
            if (mBrake == 0.0f && mForward == 0.0f && Math.abs(velocity) < min_velocity_pivot_turn)
            {
                // Pivot turn
                mLeftRatio = -1.0f;
                mForward = 1.0f;
            }
            else
                mLeftRatio = 0.6f;
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
        {
            if (mBrake == 0.0f && mForward == 0.0f && Math.abs(velocity) < min_velocity_pivot_turn)
            {
                // Pivot turn
                mRightRatio = -1.0f;
                mForward = 1.0f;
            }
            else
                mRightRatio = 0.6f;
        }

        // Check if we're reversing direction
        if (mPreviousForward * mForward < 0.0f)
        {
            // Get vehicle velocity in local space to the body of the vehicle
            if ((mForward > 0.0f && velocity < -0.1f) || (mForward < 0.0f && velocity > 0.1f))
            {
                // Brake while we've not stopped yet
                mForward = 0.0f;
                mBrake = 1.0f;
            }
            else
            {
                // When we've come to a stop, accept the new direction
                mPreviousForward = mForward;
            }
        }
    }


    @Override
    public void dispose() {
        super.dispose();
        mPhysicsSystem.RemoveStepListener(mVehicleConstraint);
        for(int i = 0; i < wheel_pos.length; i++) {
            wheel_pos[i].dispose();
        }
    }
}