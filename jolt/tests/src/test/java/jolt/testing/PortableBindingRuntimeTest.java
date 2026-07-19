package jolt.testing;

import com.github.xpenatan.jparser.runtime.helper.NativeString;
import jolt.AssertFailedHandler;
import jolt.BodyActivationListenerEm;
import jolt.JoltInterface;
import jolt.JoltLoader;
import jolt.JoltNew;
import jolt.JoltSettings;
import jolt.JoltTemp;
import jolt.RVec3;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.geometry.Plane;
import jolt.math.Float3;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.PhysicsSettings;
import jolt.physics.PhysicsSystem;
import jolt.physics.StateRecorderImpl;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyID;
import jolt.physics.body.BodyInterface;
import jolt.physics.body.BodyActivationListener;
import jolt.physics.character.CharacterContactListener;
import jolt.physics.character.CharacterContactSettings;
import jolt.physics.character.CharacterVirtual;
import jolt.physics.character.CharacterVirtualContact;
import jolt.physics.character.CharacterVirtualSettings;
import jolt.physics.collision.DefaultObjectLayerFilter;
import jolt.physics.collision.ObjectLayerPairFilter;
import jolt.physics.collision.broadphase.BroadPhaseLayer;
import jolt.physics.collision.broadphase.BroadPhaseLayerInterfaceTable;
import jolt.physics.collision.broadphase.DefaultBroadPhaseLayerFilter;
import jolt.physics.collision.broadphase.ObjectVsBroadPhaseLayerFilterTable;
import jolt.physics.collision.shape.BoxShape;
import jolt.physics.collision.shape.CapsuleShape;
import jolt.physics.collision.shape.ShapeCastSettings;
import jolt.physics.collision.shape.SubShapeID;
import jolt.physics.collision.shape.TaperedCylinderShapeSettings;
import jolt.physics.constraints.PathConstraintPathHermite;
import jolt.physics.raddoll.RagdollSettings;
import jolt.physics.softbody.SoftBodyCreationSettings;
import jolt.physics.softbody.SoftBodySharedSettings;
import jolt.physics.softbody.SoftBodySharedSettingsRodBendTwist;
import jolt.physics.softbody.SoftBodySharedSettingsRodStretchShear;
import jolt.physics.softbody.SoftBodySharedSettingsVertex;
import jolt.physics.vehicle.TrackedVehicleControllerSettings;
import jolt.physics.vehicle.VehicleEngineSettings;
import jolt.physics.vehicle.VehicleTrackSettings;
import jolt.renderer.DebugRendererEm;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PortableBindingRuntimeTest {
    private static final int NON_MOVING = 0;
    private static final int MOVING = 1;

    @BeforeClass
    public static void loadNativeLibraries() throws Exception {
        final CountDownLatch loaded = new CountDownLatch(1);
        final AtomicReference<Throwable> loadError = new AtomicReference<Throwable>();
        JoltLoader.init((success, error) -> {
            if(!success) {
                loadError.set(error != null? error : new IllegalStateException("Jolt loader returned false"));
            }
            loaded.countDown();
        });
        assertTrue("Timed out while loading the Jolt native library", loaded.await(30, TimeUnit.SECONDS));
        if(loadError.get() != null) {
            throw new AssertionError("Unable to load Jolt", loadError.get());
        }
    }

    @Test
    public void lifecycleFallingBodyUserDataCallbacksAndStateRestore() {
        World world = new World();
        try {
            BodyID floor = world.createBox(0.0f, -0.5f, 0.0f, 10.0f, 0.5f, 10.0f,
                    EMotionType.Static, NON_MOVING, 0);
            BodyID falling = world.createBox(0.0f, 5.0f, 0.0f, 0.5f, 0.5f, 0.5f,
                    EMotionType.Dynamic, MOVING, (int)0xFEDCBA98L);

            assertTrue(world.jolt.GetPhysicsSystem() == world.physicsSystem);
            assertTrue(world.jolt.GetTempAllocator().native_getAddressLong() != 0);
            assertTrue(world.jolt.GetJobSystem().native_getAddressLong() != 0);
            assertFalse(world.broadPhase.native_hasOwnership());
            assertFalse(world.objectVsBroadPhase.native_hasOwnership());
            assertFalse(world.objectLayerPairs.native_hasOwnership());

            assertEquals((int)0xFEDCBA98L, world.bodyInterface.GetUserData(falling));
            assertEquals(0xFEDCBA98L, Integer.toUnsignedLong(world.bodyInterface.GetUserData(falling)));
            assertTrue("Object-layer callback was not invoked", world.objectLayerPairs.calls.get() > 0);
            assertTrue("Activation callback was not invoked", world.activationListener.activated.get() > 0);
            assertEquals((int)0xFEDCBA98L, world.activationListener.lastUserData);

            world.bodyInterface.SetMaxLinearVelocity(falling, 123.0f);
            world.bodyInterface.SetMaxAngularVelocity(falling, 45.0f);
            assertEquals(123.0f, world.bodyInterface.GetMaxLinearVelocity(falling), 0.001f);
            assertEquals(45.0f, world.bodyInterface.GetMaxAngularVelocity(falling), 0.001f);
            world.bodyInterface.SetIsSensor(falling, true);
            assertTrue(world.bodyInterface.IsSensor(falling));
            world.bodyInterface.SetIsSensor(falling, false);

            float savedY = world.bodyInterface.GetPosition(falling).GetY();
            StateRecorderImpl recorder = new StateRecorderImpl();
            try {
                world.physicsSystem.SaveState(recorder);
                world.step(30);
                assertTrue(world.bodyInterface.GetPosition(falling).GetY() < savedY);
                recorder.Rewind();
                assertTrue(world.physicsSystem.RestoreState(recorder));
                assertEquals(savedY, world.bodyInterface.GetPosition(falling).GetY(), 0.001f);
            }
            finally {
                recorder.dispose();
            }

            world.step(180);
            float finalY = world.bodyInterface.GetPosition(falling).GetY();
            assertTrue("Dynamic body did not fall", finalY < savedY - 1.0f);
            assertTrue("Dynamic body fell through the floor", finalY > -0.25f);
            assertTrue(world.physicsSystem.GetBounds().native_getAddressLong() != 0);
            assertTrue(world.physicsSystem.GetBroadPhaseQuery().GetBounds().native_getAddressLong() != 0);
            assertTrue(world.bodyInterface.IsAdded(floor));
        }
        finally {
            world.close();
        }

        assertTrue(world.jolt.isDisposed());
        assertTrue(world.broadPhase.native_isNULL());
        assertTrue(world.objectVsBroadPhase.native_isNULL());
        assertTrue(world.objectLayerPairs.native_isNULL());
        try {
            new JoltInterface(world.settings);
            fail("Consumed JoltSettings must not be reusable");
        }
        catch(IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("already"));
        }
    }

    @Test
    public void characterContactCarriesFullDataAndLegacyNormalIsNegated() {
        World world = new World();
        CharacterVirtual character = null;
        ContactProbe listener = null;
        DefaultBroadPhaseLayerFilter broadPhaseFilter = null;
        DefaultObjectLayerFilter objectLayerFilter = null;
        CharacterVirtualSettings settings = null;
        CapsuleShape shape = null;
        Plane supportingVolume = null;
        Vec3 position = null;
        Vec3 downwardVelocity = null;
        Vec3 gravity = null;
        try {
            world.createBox(0.0f, -0.5f, 0.0f, 10.0f, 0.5f, 10.0f,
                    EMotionType.Static, NON_MOVING, 0x12345678);

            shape = new CapsuleShape(0.5f, 0.3f);
            settings = new CharacterVirtualSettings();
            settings.set_mShape(shape);
            shape.native_releaseOwnership();
            supportingVolume = new Plane(Vec3.sAxisY(), -0.3f);
            settings.set_mSupportingVolume(supportingVolume);
            position = new Vec3(0.0f, 2.0f, 0.0f);
            character = new CharacterVirtual(settings, position, Quat.sIdentity(), world.physicsSystem);

            listener = new ContactProbe();
            listener.set_OnContactAddedFull(true);
            listener.set_OnContactAdded(true);
            character.SetListener(listener);

            broadPhaseFilter = new DefaultBroadPhaseLayerFilter(world.objectVsBroadPhase, MOVING);
            objectLayerFilter = new DefaultObjectLayerFilter(world.objectLayerPairs, MOVING);
            downwardVelocity = new Vec3(0.0f, -4.0f, 0.0f);
            gravity = new Vec3(0.0f, -9.81f, 0.0f);
            for(int i = 0; i < 120 && listener.fullContacts == 0; i++) {
                character.SetLinearVelocity(downwardVelocity);
                character.Update(1.0f / 60.0f, gravity, broadPhaseFilter, objectLayerFilter,
                        JoltTemp.BodyFilter(), JoltTemp.ShapeFilter(), world.jolt.GetTempAllocator());
            }

            assertTrue("Full character contact callback was not invoked", listener.fullContacts > 0);
            assertTrue("Legacy character contact callback was not invoked", listener.legacyContacts > 0);
            assertEquals(-listener.fullNormalX, listener.legacyNormalX, 0.0001f);
            assertEquals(-listener.fullNormalY, listener.legacyNormalY, 0.0001f);
            assertEquals(-listener.fullNormalZ, listener.legacyNormalZ, 0.0001f);
            assertEquals(0x12345678, listener.contactUserData);
        }
        finally {
            if(character != null) character.SetListener(CharacterContactListener.NULL);
            if(character != null) character.dispose();
            if(listener != null) listener.dispose();
            if(broadPhaseFilter != null) broadPhaseFilter.dispose();
            if(objectLayerFilter != null) objectLayerFilter.dispose();
            if(settings != null) settings.dispose();
            if(shape != null) shape.native_reset();
            if(supportingVolume != null) supportingVolume.dispose();
            if(position != null) position.dispose();
            if(downwardVelocity != null) downwardVelocity.dispose();
            if(gravity != null) gravity.dispose();
            world.close();
        }
    }

    @Test
    public void rodsTaperedCylinderHermiteAndNewSettingsAreUsable() {
        World world = new World();
        try {
            TaperedCylinderShapeSettings tapered = new TaperedCylinderShapeSettings(1.5f, 0.5f, 0.75f, 0.05f);
            try {
                assertEquals(0.5f, tapered.get_mTopRadius(), 0.0001f);
                assertEquals(0.75f, tapered.get_mBottomRadius(), 0.0001f);
                assertTrue(tapered.Create().IsValid());
            }
            finally {
                tapered.dispose();
            }

            PathConstraintPathHermite path = new PathConstraintPathHermite();
            Vec3 p0 = new Vec3(0.0f, 0.0f, 0.0f);
            Vec3 p1 = new Vec3(1.0f, 0.0f, 0.0f);
            Vec3 tangent = new Vec3(1.0f, 0.0f, 0.0f);
            Vec3 normal = new Vec3(0.0f, 1.0f, 0.0f);
            try {
                path.AddPoint(p0, tangent, normal);
                path.AddPoint(p1, tangent, normal);
                path.SetIsLooping(true);
                assertTrue(path.IsLooping());
            }
            finally {
                path.dispose();
                p0.dispose();
                p1.dispose();
                tangent.dispose();
                normal.dispose();
            }

            SoftBodySharedSettings shared = createThreeVertexRod();
            Vec3 softPosition = new Vec3(0.0f, 2.0f, 0.0f);
            SoftBodyCreationSettings creation = new SoftBodyCreationSettings(shared, softPosition, Quat.sIdentity(), MOVING);
            shared.native_releaseOwnership();
            try {
                assertEquals(2, shared.get_mRodStretchShearConstraints().size());
                assertEquals(1, shared.get_mRodBendTwistConstraints().size());
                assertEquals(1.0f, shared.get_mRodStretchShearConstraints().at(0).get_mLength(), 0.001f);
                creation.set_mVertexRadius(0.025f);
                creation.set_mFacesDoubleSided(true);
                assertEquals(0.025f, creation.get_mVertexRadius(), 0.0001f);
                assertTrue(creation.get_mFacesDoubleSided());
            }
            finally {
                creation.dispose();
                softPosition.dispose();
                shared.native_reset();
            }

            ShapeCastSettings shapeCast = new ShapeCastSettings();
            PhysicsSettings physicsSettings = new PhysicsSettings();
            try {
                shapeCast.set_mExtraConvexRadius(0.125f);
                assertEquals(0.125f, shapeCast.get_mExtraConvexRadius(), 0.0001f);
                physicsSettings.set_mManifoldTolerance(0.002f);
                assertEquals(0.002f, physicsSettings.get_mManifoldTolerance(), 0.0001f);
            }
            finally {
                shapeCast.dispose();
                physicsSettings.dispose();
            }
        }
        finally {
            world.close();
        }
    }

    @Test
    public void representativeRagdollVehicleAndDebugApisExecute() {
        World world = new World();
        try {
            world.createBox(0.0f, -0.5f, 0.0f, 2.0f, 0.5f, 2.0f,
                    EMotionType.Static, NON_MOVING, 0);

            RagdollSettings ragdoll = new RagdollSettings();
            try {
                ragdoll.CalculateBodyIndexToConstraintIndex();
                ragdoll.CalculateConstraintIndexToBodyIdxPair();
                assertEquals(0, ragdoll.get_mParts().size());
            }
            finally {
                ragdoll.dispose();
            }

            TrackedVehicleControllerSettings tracked = new TrackedVehicleControllerSettings();
            try {
                VehicleEngineSettings engine = tracked.get_mEngine();
                engine.set_mMaxTorque(650.0f);
                assertEquals(650.0f, engine.get_mMaxTorque(), 0.001f);
                VehicleTrackSettings leftTrack = tracked.get_mTracks(0);
                leftTrack.set_mMaxBrakeTorque(900.0f);
                assertEquals(900.0f, leftTrack.get_mMaxBrakeTorque(), 0.001f);
            }
            finally {
                tracked.dispose();
            }

            DebugRendererEm renderer = new DebugRendererEm();
            try {
                renderer.Initialize();
                renderer.DrawBodies(world.physicsSystem);
                renderer.DrawConstraints(world.physicsSystem);
            }
            finally {
                renderer.dispose();
            }
        }
        finally {
            world.close();
        }
    }

    private static SoftBodySharedSettings createThreeVertexRod() {
        SoftBodySharedSettings shared = new SoftBodySharedSettings();
        for(int i = 0; i < 3; i++) {
            SoftBodySharedSettingsVertex vertex = new SoftBodySharedSettingsVertex();
            Float3 position = new Float3((float)i, 0.0f, 0.0f);
            try {
                vertex.set_mPosition(position);
                vertex.set_mInvMass(i == 0? 0.0f : 1.0f);
                shared.get_mVertices().push_back(vertex);
            }
            finally {
                position.dispose();
                vertex.dispose();
            }
        }

        for(int i = 0; i < 2; i++) {
            SoftBodySharedSettingsRodStretchShear rod = new SoftBodySharedSettingsRodStretchShear(i, i + 1, 0.0f);
            try {
                shared.get_mRodStretchShearConstraints().push_back(rod);
            }
            finally {
                rod.dispose();
            }
        }
        SoftBodySharedSettingsRodBendTwist bend = new SoftBodySharedSettingsRodBendTwist(0, 1, 0.0f);
        try {
            shared.get_mRodBendTwistConstraints().push_back(bend);
        }
        finally {
            bend.dispose();
        }
        shared.CalculateRodProperties();
        return shared;
    }

    private static final class ContactProbe extends CharacterContactListener {
        int fullContacts;
        int legacyContacts;
        int contactUserData;
        float fullNormalX;
        float fullNormalY;
        float fullNormalZ;
        float legacyNormalX;
        float legacyNormalY;
        float legacyNormalZ;

        @Override
        protected void OnContactAdded(CharacterVirtual inCharacter, CharacterVirtualContact inContact,
                CharacterContactSettings ioSettings) {
            Vec3 normal = inContact.get_mContactNormal();
            fullNormalX = normal.GetX();
            fullNormalY = normal.GetY();
            fullNormalZ = normal.GetZ();
            contactUserData = inContact.get_mUserData();
            fullContacts++;
        }

        @Override
        protected void OnContactAdded(CharacterVirtual inCharacter, BodyID inBodyID2, SubShapeID inSubShapeID2,
                RVec3 inContactPosition, Vec3 inContactNormal, CharacterContactSettings ioSettings) {
            legacyNormalX = inContactNormal.GetX();
            legacyNormalY = inContactNormal.GetY();
            legacyNormalZ = inContactNormal.GetZ();
            legacyContacts++;
        }
    }

    private static final class CountingObjectLayerPairFilter extends ObjectLayerPairFilter {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        protected boolean ShouldCollide(int inLayer1, int inLayer2) {
            calls.incrementAndGet();
            return (inLayer1 == MOVING && inLayer2 == MOVING)
                    || (inLayer1 == MOVING && inLayer2 == NON_MOVING)
                    || (inLayer1 == NON_MOVING && inLayer2 == MOVING);
        }
    }

    private static final class ActivationProbe extends BodyActivationListenerEm {
        final AtomicInteger activated = new AtomicInteger();
        volatile int lastUserData;

        @Override
        protected void OnBodyActivated(BodyID inBodyID, int inBodyUserData) {
            lastUserData = inBodyUserData;
            activated.incrementAndGet();
        }
    }

    private static final class AssertProbe extends AssertFailedHandler {
        @Override
        protected void OnAssertFailed(NativeString inExpression, NativeString inMessage, NativeString inFile, int inLine) {
            throw new AssertionError("Native Jolt assertion at line " + inLine);
        }
    }

    private static final class World implements AutoCloseable {
        final CountingObjectLayerPairFilter objectLayerPairs;
        final BroadPhaseLayerInterfaceTable broadPhase;
        final ObjectVsBroadPhaseLayerFilterTable objectVsBroadPhase;
        final BroadPhaseLayer nonMovingLayer;
        final BroadPhaseLayer movingLayer;
        final AssertProbe assertHandler;
        final JoltSettings settings;
        final JoltInterface jolt;
        final PhysicsSystem physicsSystem;
        final BodyInterface bodyInterface;
        final ActivationProbe activationListener;
        final List<BodyID> bodies = new ArrayList<BodyID>();

        World() {
            objectLayerPairs = new CountingObjectLayerPairFilter();
            broadPhase = new BroadPhaseLayerInterfaceTable(2, 2);
            nonMovingLayer = new BroadPhaseLayer((short)NON_MOVING);
            movingLayer = new BroadPhaseLayer((short)MOVING);
            broadPhase.MapObjectToBroadPhaseLayer(NON_MOVING, nonMovingLayer);
            broadPhase.MapObjectToBroadPhaseLayer(MOVING, movingLayer);
            objectVsBroadPhase = new ObjectVsBroadPhaseLayerFilterTable(broadPhase, 2, objectLayerPairs, 2);
            assertHandler = new AssertProbe();

            settings = new JoltSettings();
            settings.set_mMaxBodies(1024);
            settings.set_mMaxBodyPairs(2048);
            settings.set_mMaxContactConstraints(1024);
            settings.set_mTempAllocatorSize(4 * 1024 * 1024);
            settings.set_mMaxWorkerThreads(0);
            settings.set_mBroadPhaseLayerInterface(broadPhase);
            settings.set_mObjectVsBroadPhaseLayerFilter(objectVsBroadPhase);
            settings.set_mObjectLayerPairFilter(objectLayerPairs);
            settings.set_mAssertFailedHandler(assertHandler);
            jolt = new JoltInterface(settings);
            settings.dispose();

            physicsSystem = jolt.GetPhysicsSystem();
            bodyInterface = physicsSystem.GetBodyInterface();
            activationListener = new ActivationProbe();
            physicsSystem.SetBodyActivationListener(activationListener);
        }

        BodyID createBox(float x, float y, float z, float halfX, float halfY, float halfZ,
                EMotionType motionType, int layer, int userData) {
            Vec3 halfExtent = new Vec3(halfX, halfY, halfZ);
            Vec3 position = new Vec3(x, y, z);
            BoxShape shape = new BoxShape(halfExtent, 0.0f);
            BodyCreationSettings creation = JoltNew.BodyCreationSettings(shape, position, Quat.sIdentity(), motionType, layer);
            // BodyCreationSettings stores a RefConst<Shape>. Transfer the raw
            // constructor ownership to Jolt's reference-counted graph.
            shape.native_releaseOwnership();
            try {
                creation.set_mUserData(userData);
                creation.set_mMaxLinearVelocity(250.0f);
                creation.set_mMaxAngularVelocity(100.0f);
                Body body = bodyInterface.CreateBody(creation);
                body.ApplyBodyCreationSettings(creation, broadPhase);
                BodyID id = new BodyID(body.GetID().GetIndexAndSequenceNumber());
                bodyInterface.SetUserData(id, userData);
                bodyInterface.AddBody(id, motionType == EMotionType.Dynamic? EActivation.Activate : EActivation.DontActivate);
                bodies.add(id);
                return id;
            }
            finally {
                creation.dispose();
                shape.native_reset();
                halfExtent.dispose();
                position.dispose();
            }
        }

        void step(int count) {
            for(int i = 0; i < count; i++) {
                jolt.Step(1.0f / 60.0f, 1);
            }
        }

        @Override
        public void close() {
            for(int i = bodies.size() - 1; i >= 0; i--) {
                BodyID id = bodies.get(i);
                if(bodyInterface.IsAdded(id)) {
                    bodyInterface.RemoveBody(id);
                }
                bodyInterface.DestroyBody(id);
                id.dispose();
            }
            bodies.clear();
            physicsSystem.SetBodyActivationListener(BodyActivationListener.NULL);
            activationListener.dispose();
            jolt.dispose();
            assertHandler.dispose();
            nonMovingLayer.dispose();
            movingLayer.dispose();
        }
    }
}
