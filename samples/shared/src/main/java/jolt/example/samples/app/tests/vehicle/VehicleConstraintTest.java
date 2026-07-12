package jolt.example.samples.app.tests.vehicle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import jolt.JoltNew;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.enums.EOverrideMassProperties;
import jolt.example.samples.app.jolt.Layers;
import jolt.Jolt;
import jolt.core.Color;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.math.Vec4;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.collision.shape.BoxShape;
import jolt.physics.collision.shape.OffsetCenterOfMassShapeSettings;
import jolt.physics.collision.shape.Shape;
import jolt.physics.vehicle.ArrayVehicleAntiRollBar;
import jolt.physics.vehicle.ArrayWheelSettings;
import jolt.physics.vehicle.VehicleCollisionTester;
import jolt.physics.vehicle.VehicleCollisionTesterCastCylinder;
import jolt.physics.vehicle.VehicleCollisionTesterCastSphere;
import jolt.physics.vehicle.VehicleCollisionTesterRay;
import jolt.physics.vehicle.VehicleConstraint;
import jolt.physics.vehicle.VehicleConstraintSettings;
import jolt.physics.vehicle.VehicleController;
import jolt.physics.vehicle.WheelSettings;
import jolt.physics.vehicle.WheelSettingsWV;
import jolt.physics.vehicle.WheeledVehicleController;
import jolt.physics.vehicle.WheeledVehicleControllerSettings;

public class VehicleConstraintTest extends VehicleTest {

    private static float sInitialRollAngle = 0;
    private static float sMaxRollAngle = MathUtils.degreesToRadians * 60.0f;
    private static float sMaxSteeringAngle = MathUtils.degreesToRadians * 30.0f;
    private static int sCollisionMode = 2;
    private static boolean sFourWheelDrive = false;
    private static boolean sAntiRollbar = true;
    private static boolean sLimitedSlipDifferentials = true;
    private static boolean sOverrideGravity = false;					///< If true, gravity is overridden to always oppose the ground normal
    private static float sMaxEngineTorque = 500.0f;
    private static float sClutchStrength = 10.0f;
    private static float sFrontCasterAngle = 0.0f;
    private static float sFrontKingPinAngle = 0.0f;
    private static float sFrontCamber = 0.0f;
    private static float sFrontToe = 0.0f;
    private static float sFrontSuspensionForwardAngle = 0.0f;
    private static float sFrontSuspensionSidewaysAngle = 0.0f;
    private static float sFrontSuspensionMinLength = 0.3f;
    private static float sFrontSuspensionMaxLength = 0.5f;
    private static float sFrontSuspensionFrequency = 1.5f;
    private static float sFrontSuspensionDamping = 0.5f;
    private static float sRearSuspensionForwardAngle = 0.0f;
    private static float sRearSuspensionSidewaysAngle = 0.0f;
    private static float sRearCasterAngle = 0.0f;
    private static float sRearKingPinAngle = 0.0f;
    private static float sRearCamber = 0.0f;
    private static float sRearToe = 0.0f;
    private static float sRearSuspensionMinLength = 0.3f;
    private static float sRearSuspensionMaxLength = 0.5f;
    private static float sRearSuspensionFrequency = 1.5f;
    private static float sRearSuspensionDamping = 0.5f;

    private Body mCarBody; ///< The vehicle
    private VehicleConstraint mVehicleConstraint; ///< The vehicle constraint
    private VehicleCollisionTester[] mTesters = new VehicleCollisionTester[3];

    private float mForward = 0.0f;
    private float mPreviousForward = 1.0f; ///< Keeps track of last car direction so we know when to brake and when to accelerate
    private float mRight = 0.0f;
    private float mBrake = 0.0f;
    private float mHandBrake = 0.0f;

    private Vec3 tempVec3_1;
    private Vec4 tempVec4_1;
    private Vec4 tempVec4_2;
    private Vec4 tempVec4_3;
    private Vec4 tempVec4_4;
    private Mat44 tempMat44_1;

    private WheeledVehicleController temp = WheeledVehicleController.native_new();

    @Override
    public void initialize() {
        super.initialize();
        tempVec3_1 = JoltNew.Vec3();
        tempVec4_1 = JoltNew.Vec4();
        tempVec4_2 = JoltNew.Vec4();
        tempVec4_3 = JoltNew.Vec4();
        tempVec4_4 = JoltNew.Vec4();
        tempMat44_1 = JoltNew.Mat44();

        float wheel_radius = 0.3f;
        float wheel_width = 0.1f;
        float half_vehicle_length = 2.0f;
        float half_vehicle_width = 0.9f;
        float half_vehicle_height = 0.2f;

        // Create collision testers
        mTesters[0] = new VehicleCollisionTesterRay(Layers.MOVING);
        mTesters[1] = new VehicleCollisionTesterCastSphere(Layers.MOVING, 0.5f * wheel_width);
        mTesters[2] = new VehicleCollisionTesterCastCylinder(Layers.MOVING);

        // Create vehicle body
        Vec3 position = JoltNew.Vec3(0, 5, 0);
        Shape car_shape = new OffsetCenterOfMassShapeSettings(JoltNew.Vec3(0, -half_vehicle_height, 0), new BoxShape(JoltNew.Vec3(half_vehicle_width, half_vehicle_height, half_vehicle_length))).Create().Get();
        BodyCreationSettings car_body_settings = JoltNew.BodyCreationSettings(car_shape, position, Quat.sRotation(Vec3.sAxisZ(), sInitialRollAngle), EMotionType.Dynamic, Layers.MOVING);
        car_body_settings.set_mOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        car_body_settings.get_mMassPropertiesOverride().set_mMass(1500.0f);
        mCarBody = mBodyInterface.CreateBody(car_body_settings);
        car_body_settings.dispose();
        mBodyInterface.AddBody(mCarBody.GetID(), EActivation.Activate);

        // Create vehicle constraint
        VehicleConstraintSettings vehicle = new VehicleConstraintSettings();
        vehicle.set_mDrawConstraintSize(0.1f);
        vehicle.set_mMaxPitchRollAngle(sMaxRollAngle);

        // Suspension direction
        Vec3 front_suspension_dir = JoltNew.Vec3(MathUtils.tan(sFrontSuspensionSidewaysAngle), -1, MathUtils.tan(sFrontSuspensionForwardAngle));
        Vec3 front_steering_axis = JoltNew.Vec3(-MathUtils.tan(sFrontKingPinAngle), 1, -MathUtils.tan(sFrontCasterAngle));
        Vec3 front_wheel_up = JoltNew.Vec3(MathUtils.sin(sFrontCamber), MathUtils.cos(sFrontCamber), 0);
        Vec3 front_wheel_forward = JoltNew.Vec3(-MathUtils.sin(sFrontToe), 0, MathUtils.cos(sFrontToe));
        Vec3 rear_suspension_dir = JoltNew.Vec3(MathUtils.tan(sRearSuspensionSidewaysAngle), -1, MathUtils.tan(sRearSuspensionForwardAngle));
        Vec3 rear_steering_axis = JoltNew.Vec3(-MathUtils.tan(sRearKingPinAngle), 1, -MathUtils.tan(sRearCasterAngle));
        Vec3 rear_wheel_up = JoltNew.Vec3(MathUtils.sin(sRearCamber), MathUtils.cos(sRearCamber), 0);
        Vec3 rear_wheel_forward = JoltNew.Vec3(-MathUtils.sin(sRearToe), 0, MathUtils.cos(sRearToe));
        Vec3 flip_x = JoltNew.Vec3(-1, 1, 1);

        // Wheels, left front
        WheelSettingsWV w1 = new WheelSettingsWV();
        w1.get_mPosition().Set(half_vehicle_width, -0.9f * half_vehicle_height, half_vehicle_length - 2.0f * wheel_radius);
        w1.set_mSuspensionDirection(front_suspension_dir.Normalized());
        w1.set_mSteeringAxis(front_steering_axis.Normalized());
        w1.set_mWheelUp(front_wheel_up);
        w1.set_mWheelForward(front_wheel_forward);
        w1.set_mSuspensionMinLength(sFrontSuspensionMinLength);
        w1.set_mSuspensionMaxLength(sFrontSuspensionMaxLength);
        w1.get_mSuspensionSpring().set_mFrequency(sFrontSuspensionFrequency);
        w1.get_mSuspensionSpring().set_mDamping(sFrontSuspensionDamping);
        w1.set_mMaxSteerAngle(sMaxSteeringAngle);
        w1.set_mMaxHandBrakeTorque(0.0f); // Front wheel doesn't have hand brake

        // Right front
        WheelSettingsWV w2 = new WheelSettingsWV();
        w2.get_mPosition().Set(-half_vehicle_width, -0.9f * half_vehicle_height, half_vehicle_length - 2.0f * wheel_radius);
        w2.set_mSuspensionDirection(flip_x.MulVec3(front_suspension_dir.Normalized()));
        w2.set_mSteeringAxis(flip_x.MulVec3(front_steering_axis.Normalized()));
        w2.set_mWheelUp(flip_x.MulVec3(front_wheel_up));
        w2.set_mWheelForward(flip_x.MulVec3(front_wheel_forward));
        w2.set_mSuspensionMinLength(sFrontSuspensionMinLength);
        w2.set_mSuspensionMaxLength(sFrontSuspensionMaxLength);
        w2.get_mSuspensionSpring().set_mFrequency(sFrontSuspensionFrequency);
        w2.get_mSuspensionSpring().set_mDamping(sFrontSuspensionDamping);
        w2.set_mMaxSteerAngle(sMaxSteeringAngle);
        w2.set_mMaxHandBrakeTorque(0.0f); // Front wheel doesn't have hand brake

        // Left rear
        WheelSettingsWV w3 = new WheelSettingsWV();
        w3.get_mPosition().Set(half_vehicle_width, -0.9f * half_vehicle_height, -half_vehicle_length + 2.0f * wheel_radius);
        w3.set_mSuspensionDirection(rear_suspension_dir.Normalized());
        w3.set_mSteeringAxis(rear_steering_axis.Normalized());
        w3.set_mWheelUp(rear_wheel_up);
        w3.set_mWheelForward(rear_wheel_forward);
        w3.set_mSuspensionMinLength(sRearSuspensionMinLength);
        w3.set_mSuspensionMaxLength(sRearSuspensionMaxLength);
        w3.get_mSuspensionSpring().set_mFrequency(sRearSuspensionFrequency);
        w3.get_mSuspensionSpring().set_mDamping(sRearSuspensionDamping);
        w3.set_mMaxSteerAngle(0.0f);

        // Right rear
        WheelSettingsWV w4 = new WheelSettingsWV();
        w4.get_mPosition().Set(-half_vehicle_width, -0.9f * half_vehicle_height, -half_vehicle_length + 2.0f * wheel_radius);
        w4.set_mSuspensionDirection(flip_x.MulVec3(rear_suspension_dir.Normalized()));
        w4.set_mSteeringAxis(flip_x.MulVec3(rear_steering_axis.Normalized()));
        w4.set_mWheelUp(flip_x.MulVec3(rear_wheel_up));
        w4.set_mWheelForward(flip_x.MulVec3(rear_wheel_forward));
        w4.set_mSuspensionMinLength(sRearSuspensionMinLength);
        w4.set_mSuspensionMaxLength(sRearSuspensionMaxLength);
        w4.get_mSuspensionSpring().set_mFrequency(sRearSuspensionFrequency);
        w4.get_mSuspensionSpring().set_mDamping(sRearSuspensionDamping);
        w4.set_mMaxSteerAngle(0.0f);

        front_suspension_dir.dispose();
        front_steering_axis.dispose();
        front_wheel_up.dispose();
        front_wheel_forward.dispose();
        rear_suspension_dir.dispose();
        rear_steering_axis.dispose();
        rear_wheel_up.dispose();
        rear_wheel_forward.dispose();
        flip_x.dispose();

        ArrayWheelSettings wheelSettingsArray = new ArrayWheelSettings();
        wheelSettingsArray.push_back(w1);
        wheelSettingsArray.push_back(w2);
        wheelSettingsArray.push_back(w3);
        wheelSettingsArray.push_back(w4);
        vehicle.set_mWheels(wheelSettingsArray);

        w1.set_mRadius(wheel_radius);
        w1.set_mWidth(wheel_width);
        w2.set_mRadius(wheel_radius);
        w2.set_mWidth(wheel_width);
        w3.set_mRadius(wheel_radius);
        w3.set_mWidth(wheel_width);
        w4.set_mRadius(wheel_radius);
        w4.set_mWidth(wheel_width);

        WheeledVehicleControllerSettings controller = new WheeledVehicleControllerSettings();
        vehicle.set_mController(controller);

        // Differential
        controller.get_mDifferentials().resize(sFourWheelDrive ? 2 : 1);
        controller.get_mDifferentials().at(0).set_mLeftWheel(0);
        controller.get_mDifferentials().at(0).set_mRightWheel(1);
        if (sFourWheelDrive)
        {
            controller.get_mDifferentials().at(1).set_mLeftWheel(2);
            controller.get_mDifferentials().at(1).set_mRightWheel(3);

            // Split engine torque
            controller.get_mDifferentials().at(1).set_mEngineTorqueRatio(0.5f);
            controller.get_mDifferentials().at(0).set_mEngineTorqueRatio(0.5f);
        }

        // Anti rollbars
        if (sAntiRollbar)
        {
            ArrayVehicleAntiRollBar mAntiRollBars = vehicle.get_mAntiRollBars();
            mAntiRollBars.resize(2);
            mAntiRollBars.at(0).set_mLeftWheel(0);
            mAntiRollBars.at(0).set_mRightWheel(1);
            mAntiRollBars.at(1).set_mLeftWheel(2);
            mAntiRollBars.at(1).set_mRightWheel(3);
        }

        mVehicleConstraint = new VehicleConstraint(mCarBody, vehicle);

        mPhysicsSystem.AddConstraint(mVehicleConstraint);
        mPhysicsSystem.AddStepListener(mVehicleConstraint);
    }

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        super.prePhysicsUpdate(isPlaying);

        if (mRight != 0.0f || mForward != 0.0f || mBrake != 0.0f || mHandBrake != 0.0f)
            mBodyInterface.ActivateBody(mCarBody.GetID());

        VehicleController vehicleController = mVehicleConstraint.GetController();
        WheeledVehicleController controller = temp;
        controller.native_copy(vehicleController);

        // Update vehicle statistics
        controller.GetEngine().set_mMaxTorque(sMaxEngineTorque);
        controller.GetTransmission().set_mClutchStrength(sClutchStrength);

        // Pass the input on to the constraint
        controller.SetDriverInput(mForward, mRight, mBrake, mHandBrake);

        mVehicleConstraint.SetVehicleCollisionTester(mTesters[sCollisionMode]);

        // Draw our wheels (this needs to be done in the pre update since we draw the bodies too in the state before the step)
        for (int w = 0; w < 4; ++w)
        {
            WheelSettings settings = mVehicleConstraint.GetWheel(w).GetSettings();
            Mat44 wheel_transform = mVehicleConstraint.GetWheelWorldTransform(w, Vec3.sAxisY(), Vec3.sAxisX()); // The cylinder we draw is aligned with Y so we specify that as rotational axis
            mDebugRenderer.DrawCylinder(wheel_transform, 0.5f * settings.get_mWidth(), settings.get_mRadius(), Color.get_sGreen());
        }
    }

    @Override
    public void processInput() {
        // Determine acceleration and brake
        mForward = 0.0f;
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            mForward = 1.0f;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            mForward = -1.0f;
        }

        // Check if we're reversing direction
        mBrake = 0.0f;
        if(mPreviousForward * mForward < 0.0f) {
            // Get vehicle velocity in local space to the body of the vehicle
            float velocity = (mCarBody.GetRotation().Conjugated().MulVec3(mCarBody.GetLinearVelocity()).GetZ());
            if((mForward > 0.0f && velocity < -0.1f) || (mForward < 0.0f && velocity > 0.1f)) {
                // Brake while we've not stopped yet
                mForward = 0.0f;
                mBrake = 1.0f;
            }
            else {
                // When we've come to a stop, accept the new direction
                mPreviousForward = mForward;
            }
        }

        // Hand brake will cancel gas pedal
        mHandBrake = 0.0f;
        if(Gdx.input.isKeyPressed(Input.Keys.Z)) {
            mForward = 0.0f;
            mHandBrake = 1.0f;
        }

        // Steering
        mRight = 0.0f;
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            mRight = -1.0f;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            mRight = 1.0f;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        mPhysicsSystem.RemoveStepListener(mVehicleConstraint);
    }

    private Vector3 vec = new Vector3();

    @Override
    public void updateCamera(CameraInputController cameraController) {
        updateCameraPivot();
        camera.up.set(0, 1, 0);
        cameraPivot.getTranslation(vec);
        camera.position.set(vec.x, vec.y, vec.z);

        float forwardX = cameraPivot.val[8];
        float forwardY = cameraPivot.val[9];
        float forwardZ = cameraPivot.val[10];

        camera.lookAt(
                vec.x + forwardX,
                vec.y + forwardY,
                vec.z + forwardZ
        );
    }

    private void updateCameraPivot() {
        // Get the forward direction of the car (Z axis in local space)
        Vec3 fwd = mCarBody.GetRotation().RotateAxisZ();
        fwd.SetY(-0.2f); // Project onto XZ plane
        float len = fwd.Length();
        if (len != 0.0f)
            fwd.Div(len);
        else
            fwd = Vec3.sAxisZ();

        // Calculate up and right vectors
        Vec3 up = Vec3.sAxisY();
        Vec3 right = up.Cross(fwd);

        // Get the car's position
        Vec3 carPosition = mCarBody.GetPosition();

        // Add offset to position the camera behind the car
        // Move 5 units back and 2 units up from the car's position
        tempVec3_1.Set(
                carPosition.GetX() - fwd.GetX() * 5.0f,
                carPosition.GetY() + 2.0f,
                carPosition.GetZ() - fwd.GetZ() * 5.0f
        );

        // Create the transformation matrix for the camera pivot
        // The camera will look at the car from this pivot point
        tempVec4_1.Set(right.GetX(), right.GetY(), right.GetZ(), 0f);
        tempVec4_2.Set(up.GetX(), up.GetY(), up.GetZ(), 0f);
        tempVec4_3.Set(fwd.GetX(), fwd.GetY(), fwd.GetZ(), 0f);
        tempVec4_4.Set(tempVec3_1.GetX(), tempVec3_1.GetY(), tempVec3_1.GetZ(), 1f);

        tempMat44_1.SetColumn4(0, tempVec4_1);
        tempMat44_1.SetColumn4(1, tempVec4_2);
        tempMat44_1.SetColumn4(2, tempVec4_3);
        tempMat44_1.SetColumn4(3, tempVec4_4);

        getMat44Data(tempMat44_1, cameraPivot);
    }

    // Function to extract Mat44 data into a flat float array
    void getMat44Data(Mat44 mat, Matrix4 matrix4) {
        float[] outArray = matrix4.val;
        // Column-major order: copy each column directly
        for (int col = 0; col < 4; col++) {
            Vec4 vec4 = mat.GetColumn4(col);
            for (int row = 0; row < 4; row++) {
                float val = vec4.GetComponent(row);
                outArray[col * 4 + row] = val; // m[col][row]
            }
        }
    }
}
