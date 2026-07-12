package jolt.example.samples.app.tests.character;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jolt.JoltNew;
import jolt.JoltTemp;
import jolt.core.Color;
import jolt.enums.EActivation;
import jolt.enums.EGroundState;
import jolt.enums.EMotionType;
import jolt.example.samples.app.jolt.Layers;
import jolt.example.samples.app.tests.Test;
import jolt.gdx.JoltGdx;
import jolt.geometry.Plane;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyID;
import jolt.physics.character.CharacterContactListener;
import jolt.physics.character.CharacterVirtual;
import jolt.physics.character.CharacterVirtualSettings;
import jolt.physics.character.ExtendedUpdateSettings;
import jolt.physics.collision.DefaultObjectLayerFilter;
import jolt.physics.collision.broadphase.DefaultBroadPhaseLayerFilter;
import jolt.physics.collision.shape.CapsuleShape;
import jolt.physics.collision.shape.CylinderShape;
import jolt.physics.collision.shape.RotatedTranslatedShapeSettings;
import jolt.physics.collision.shape.StaticCompoundShapeSettings;

public class CharacterSpaceShipTest extends Test {

    static float cCharacterHeightStanding = 1.35f;
    static float cCharacterRadiusStanding = 0.3f;
    static float cCharacterSpeed = 6.0f;
    static float cJumpSpeed = 4.0f;

    private CharacterVirtual mCharacter;

    // The space ship
    private BodyID mSpaceShip;

    // Previous frame space ship transform
    private Mat44 mSpaceShipPrevTransform;

    // Space ship velocity
    private Vec3 mSpaceShipLinearVelocity;
    private Vec3 mSpaceShipAngularVelocity;

    // Global time
    private float mTime = 0.0f;

    // Player input
    private Vec3 mDesiredVelocity;
    private boolean mJump = false;
    private boolean mWasJump = false;

    @Override
    public void initialize() {
        mSpaceShipLinearVelocity = JoltNew.Vec3();
        mSpaceShipAngularVelocity = JoltNew.Vec3();
        mDesiredVelocity = JoltNew.Vec3();
        mSpaceShipPrevTransform = JoltNew.Mat44();

        float cSpaceShipHeight = 2.0f;
        float cSpaceShipRingHeight = 0.2f;
        float cSpaceShipRadius = 100.0f;
        Vec3 cShipInitialPosition = JoltTemp.Vec3_1(-25, 15, 0);

        // Create floor for reference
        createFloor();

        // Create 'player' character
        CharacterVirtualSettings settings = new CharacterVirtualSettings();
        Vec3 inPosition = JoltTemp.Vec3_2(0, 0.5f * cCharacterHeightStanding + cCharacterRadiusStanding, 0);
        CapsuleShape capsuleShape = new CapsuleShape(0.5f * cCharacterHeightStanding, cCharacterRadiusStanding);
        RotatedTranslatedShapeSettings shapeSettings = JoltNew.RotatedTranslatedShapeSettings(inPosition, Quat.sIdentity(), capsuleShape);
        settings.set_mShape(shapeSettings.Create().Get());
        settings.set_mSupportingVolume(new Plane(Vec3.sAxisY(), -cCharacterRadiusStanding)); // Accept contacts that touch the lower sphere of the capsule
        Vec3 position = cShipInitialPosition.AddVec3(JoltTemp.Vec3_3(0, cSpaceShipHeight, 0));
        mCharacter = new CharacterVirtual(settings, position, Quat.sIdentity(), mPhysicsSystem);
        CharacterContactListener listener;
        mCharacter.SetListener(listener = new CharacterContactListener() {
            @Override
            protected void OnAdjustBodyVelocity(CharacterVirtual inCharacter, Body inBody2, Vec3 ioLinearVelocity, Vec3 ioAngularVelocity) {
                // Cancel out velocity of space ship, we move relative to this which means we don't feel any of the acceleration of the ship (= engage inertial dampeners!)
                ioLinearVelocity.Sub(mSpaceShipLinearVelocity);
                ioAngularVelocity.Sub(mSpaceShipAngularVelocity);
            }
        });
        listener.set_OnAdjustBodyVelocity(true);

        // Create the space ship
        StaticCompoundShapeSettings compound = new StaticCompoundShapeSettings();
        compound.native_releaseOwnership();
        for(float h = cSpaceShipRingHeight; h < cSpaceShipHeight; h += cSpaceShipRingHeight) {
            float val = cSpaceShipRadius - cSpaceShipHeight - cSpaceShipRingHeight + h;
            float radius = (float)Math.sqrt((cSpaceShipRadius * cSpaceShipRadius) - (val * val));
            compound.AddShape(Vec3.sZero(), Quat.sIdentity(), new CylinderShape(h, radius));
        }
        BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(compound, cShipInitialPosition, Quat.sIdentity(), EMotionType.Kinematic, Layers.MOVING);
        mSpaceShip = mBodyInterface.CreateAndAddBody(bodyCreationSettings, EActivation.Activate);

        mSpaceShipPrevTransform.Set(mBodyInterface.GetCenterOfMassTransform(mSpaceShip));
    }

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        float deltaTime = Gdx.graphics.getDeltaTime();
        Vec3 gravity = JoltTemp.Vec3_1();
        Vec3 current_vertical_velocity = JoltTemp.Vec3_2();
        Mat44 new_space_ship_transform = JoltTemp.Mat44_1();

        if(isPlaying) {
            // Update scene time
            mTime += deltaTime;
            // Update the character so it stays relative to the space ship
            new_space_ship_transform.Set(mBodyInterface.GetCenterOfMassTransform(mSpaceShip));
            {
                Vec3 vec3 = mSpaceShipPrevTransform.Inversed().MulVec3(mCharacter.GetPosition());
                mCharacter.SetPosition(new_space_ship_transform.MulVec3(vec3));
            }
            // Update the character rotation and its up vector to match the new up vector of the ship
            mCharacter.SetUp(new_space_ship_transform.GetAxisY());
            mCharacter.SetRotation(new_space_ship_transform.GetQuaternion());
        }

        // Draw character pre update (the sim is also drawn pre update)
        // Note that we have first updated the position so that it matches the new position of the ship
        mCharacter.GetShape().Draw(mDebugRenderer, mCharacter.GetCenterOfMassTransform(), Vec3.sOne(), Color.get_sGreen(), false, true);

        if(!isPlaying) {
            return;
        }

        // Determine new character velocity
        {
            float floatVal = mCharacter.GetLinearVelocity().Dot(mSpaceShipPrevTransform.GetAxisY());
            current_vertical_velocity.Set(mCharacter.GetUp().MulFloat(floatVal));
        }
        Vec3 ground_velocity = mCharacter.GetGroundVelocity();
        Vec3 new_velocity;
        if(mCharacter.GetGroundState() == EGroundState.OnGround // If on ground
                && (current_vertical_velocity.GetY() - ground_velocity.GetY()) < 0.1f) // And not moving away from ground
        {
            // Assume velocity of ground when on ground
            new_velocity = ground_velocity;

            // Jump
            if(mJump)
                new_velocity.Add(mCharacter.GetUp().MulFloat(cJumpSpeed));
        }
        else
            new_velocity = current_vertical_velocity;

        // Gravity always acts relative to the ship
        gravity.Set(new_space_ship_transform.Multiply3x3(mPhysicsSystem.GetGravity()));
        new_velocity.Add(gravity.MulFloat(deltaTime));

        // Transform player input to world space
        new_velocity.Add(new_space_ship_transform.Multiply3x3(mDesiredVelocity));

        // Update character velocity
        mCharacter.SetLinearVelocity(new_velocity);

        // Update the character position
        ExtendedUpdateSettings update_settings = JoltTemp.ExtendedUpdateSettings();
        DefaultBroadPhaseLayerFilter defaultBroadPhaseLayerFilter = JoltTemp.DefaultBroadPhaseLayerFilter(joltInstance.getObjectVsBroadPhaseLayerFilter(), Layers.MOVING);
        DefaultObjectLayerFilter defaultObjectLayerFilter = JoltTemp.DefaultObjectLayerFilter(joltInstance.getObjectLayerPairFilter(), Layers.MOVING);
        mCharacter.ExtendedUpdate(deltaTime,
                gravity,
                update_settings,
                defaultBroadPhaseLayerFilter,
                defaultObjectLayerFilter,
                JoltTemp.BodyFilter(),
                JoltTemp.ShapeFilter(),
                joltInstance.getTempAllocator());

        mSpaceShipPrevTransform.Set(new_space_ship_transform);

        updateShipVelocity();
    }

    private void updateShipVelocity() {
        // Make it a rocky ride...
        mSpaceShipLinearVelocity.Set(JoltTemp.Vec3_1(MathUtils.sin(mTime), 0, MathUtils.cos(mTime)).MulFloat(50.0f));
        mSpaceShipAngularVelocity.Set(JoltTemp.Vec3_2(MathUtils.sin(2.0f * mTime), 1, MathUtils.cos(2.0f * mTime)).MulFloat(0.5f));

        mBodyInterface.SetLinearAndAngularVelocity(mSpaceShip, mSpaceShipLinearVelocity, mSpaceShipAngularVelocity);
    }

    @Override
    public void processInput() {
        // Determine controller input
        Vec3 control_input = JoltTemp.Vec3_1();
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) control_input.SetZ(-1);
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) control_input.SetZ(1);
        if(Gdx.input.isKeyPressed(Input.Keys.UP)) control_input.SetX(1);
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) control_input.SetX(-1);
        if(control_input.NotEquals(Vec3.sZero())) {
            control_input = control_input.Normalized();
        }

        // Calculate the desired velocity in local space to the ship based on the camera forward
        Mat44 new_space_ship_transform = mBodyInterface.GetCenterOfMassTransform(mSpaceShip);

        Vec3 cam_fwd = new_space_ship_transform.GetRotation().Multiply3x3Transposed(JoltGdx.convert(camera.direction, JoltTemp.Vec3_2()));
        cam_fwd.SetY(0.0f);
        cam_fwd = cam_fwd.NormalizedOr(Vec3.sAxisX());
        Quat rotation = Quat.sFromTo(Vec3.sAxisX(), cam_fwd);
        control_input = rotation.MulVec3(control_input);

        // Smooth the player input in local space to the ship
        Vec3 playerPos = control_input.MulFloat(0.25f).MulFloat(cCharacterSpeed).AddVec3(mDesiredVelocity.Mul(0.75f));
        mDesiredVelocity.Set(playerPos);

        // Check actions
        mJump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    Vector3 initialOffset = new Vector3(0f, 5f, 10f);

    @Override
    public void updateCamera(CameraInputController cameraController) {
        Vec3 charPosition = mCharacter.GetPosition();

        JoltGdx.convert(charPosition, cameraController.target);

        // Compute offset based on camera direction and zoom level
        float distance = initialOffset.len() * 7 / initialOffset.len();
        Vector3 offset = camera.direction.cpy().scl(-distance); // Move opposite to direction
        offset.y += 1;

        // Set camera position to vehicle position plus offset
        camera.position.set(cameraController.target).add(offset);
    }

    @Override
    public void dispose() {
        mSpaceShipLinearVelocity.dispose();
        mSpaceShipAngularVelocity.dispose();
        mSpaceShipPrevTransform.dispose();
    }
}