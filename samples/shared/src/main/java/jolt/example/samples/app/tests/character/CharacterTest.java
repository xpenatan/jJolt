package jolt.example.samples.app.tests.character;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jolt.CharacterSettings;
import jolt.JoltNew;
import jolt.JoltTemp;
import jolt.RVec3;
import jolt.enums.EActivation;
import jolt.enums.EGroundState;
import jolt.example.samples.app.jolt.Layers;
import jolt.example.samples.app.tests.Test;
import jolt.example.samples.app.tests.shapes.ShapeHelper;
import jolt.gdx.JoltGdx;
import jolt.geometry.Plane;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.character.Character;
import jolt.physics.collision.shape.CapsuleShape;
import jolt.physics.collision.shape.Shape;

public class CharacterTest extends Test {

    private Character mCharacter;

    private static float cCharacterHeightStanding = 1.35f;
    private static float cCharacterRadiusStanding = 0.3f;
    private static float sCharacterSpeed = 6.0f;
    private static float sJumpSpeed = 4.0f;
    private float cCollisionTolerance = 0.05f;

    private Vec3 control_input;
    private boolean mJump;
    private boolean sControlMovementDuringJump = true;

    @Override
    public void initialize() {
        control_input = JoltNew.Vec3();

        // Create floor for reference
        createFloor();


        Body box = ShapeHelper.createBox(Layers.MOVING, mBodyInterface, 100, new Vector3(1, 1, 1), new Vector3(0, 10, 0), Quat.sIdentity());
        mBodyInterface.AddBody(box.GetID(), EActivation.Activate);

        Vec3 position = JoltTemp.Vec3_1(0, 1.5f * cCharacterHeightStanding + cCharacterRadiusStanding, 0);
        CapsuleShape capsuleShape = new CapsuleShape(0.5f * cCharacterHeightStanding, cCharacterRadiusStanding);
        Shape mStandingShape = JoltNew.RotatedTranslatedShapeSettings(position, Quat.sIdentity(), capsuleShape).Create().Get();

        // Create 'player' character
        CharacterSettings settings = new CharacterSettings();
        settings.set_mMaxSlopeAngle(MathUtils.degRad * 45.0f);
        settings.set_mLayer(Layers.MOVING);
        settings.set_mShape(mStandingShape);
        settings.set_mFriction(0.5f);
        settings.set_mSupportingVolume(new Plane(Vec3.sAxisY(), -cCharacterRadiusStanding)); // Accept contacts that touch the lower sphere of the capsule
        RVec3 rVec3 = RVec3.sZero();
        mCharacter = new Character(settings, rVec3, Quat.sIdentity(), 0, mPhysicsSystem);
        mCharacter.AddToPhysicsSystem(EActivation.Activate);
    }

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        if(!isPlaying) {
            return;
        }

        // Cancel movement in opposite direction of normal when touching something we can't walk up
        Vec3 movement_direction = control_input;
        EGroundState ground_state = mCharacter.GetGroundState();
        if (ground_state == EGroundState.OnSteepGround || ground_state == EGroundState.NotSupported)
        {
            Vec3 normal = mCharacter.GetGroundNormal();
            normal.SetY(0.0f);
            float dot = normal.Dot(movement_direction);
            if (dot < 0.0f)  {
                movement_direction.Sub((normal.MulFloat(dot)).DivFloat(normal.LengthSq()));
            }
        }

        if (sControlMovementDuringJump || mCharacter.IsSupported())
        {
            // Update velocity
            Vec3 current_velocity = mCharacter.GetLinearVelocity();
            Vec3 desired_velocity = JoltTemp.Vec3_1(movement_direction.MulFloat(sCharacterSpeed));
            if (!desired_velocity.IsNearZero() || current_velocity.GetY() < 0.0f || !mCharacter.IsSupported())
                desired_velocity.SetY(current_velocity.GetY());

            Vec3 new_velocity = JoltTemp.Vec3_2(current_velocity.MulFloat(0.75f)).AddVec3(desired_velocity.MulFloat(0.25f));

            // Jump
            if(mJump) {
                if (ground_state == EGroundState.OnGround) {
                    new_velocity.Add(JoltTemp.Vec3_4(0, sJumpSpeed, 0));
                }
            }

            // Update the velocity
            mCharacter.SetLinearVelocity(new_velocity);
        }
    }

    @Override
    public void postPhysicsUpdate(boolean isPlaying, float deltaTime) {
        if(isPlaying) {
            // Fetch the new ground properties
            mCharacter.PostSimulation(cCollisionTolerance);
        }
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

        // Rotate controls to align with the camera
        Vec3 cam_fwd = JoltGdx.convert(camera.direction, JoltTemp.Vec3_2());
        cam_fwd.SetY(0.0f);
        cam_fwd = cam_fwd.NormalizedOr(Vec3.sAxisX());
        Quat rotation = Quat.sFromTo(Vec3.sAxisX(), cam_fwd);
        this.control_input.Set(rotation.MulVec3(control_input));

        // Check actions
        mJump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    Vector3 initialOffset = new Vector3(0f, 5f, 10f);

    @Override
    public void updateCamera(CameraInputController cameraController) {
        RVec3 rVec3 = mCharacter.GetPosition();

        JoltGdx.convert(rVec3, cameraController.target);

        // Compute offset based on camera direction and zoom level
        float distance = initialOffset.len() * 7 / initialOffset.len();
        Vector3 offset = camera.direction.cpy().scl(-distance); // Move opposite to direction
        offset.y += 2;

        // Set camera position to vehicle position plus offset
        camera.position.set(cameraController.target).add(offset);
    }

    @Override
    public void dispose() {
        mCharacter.RemoveFromPhysicsSystem();
        mCharacter.dispose();
        control_input.dispose();
    }
}