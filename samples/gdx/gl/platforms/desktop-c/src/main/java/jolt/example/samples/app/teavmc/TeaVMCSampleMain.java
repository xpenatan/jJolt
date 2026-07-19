package jolt.example.samples.app.teavmc;

import com.github.xpenatan.jParser.loader.JParserLibraryLoaderListener;
import com.github.xpenatan.jparser.runtime.RuntimeLoader;
import gen.c.jolt.JoltInterface;
import gen.c.jolt.JoltLoader;
import gen.c.jolt.JoltNew;
import gen.c.jolt.JoltSettings;
import gen.c.jolt.math.Quat;
import gen.c.jolt.math.Vec3;
import gen.c.jolt.enums.EActivation;
import gen.c.jolt.enums.EMotionType;
import gen.c.jolt.physics.PhysicsSystem;
import gen.c.jolt.physics.StateRecorderImpl;
import gen.c.jolt.physics.body.Body;
import gen.c.jolt.physics.body.BodyCreationSettings;
import gen.c.jolt.physics.body.BodyID;
import gen.c.jolt.physics.body.BodyInterface;
import gen.c.jolt.physics.collision.ObjectLayerPairFilterTable;
import gen.c.jolt.physics.collision.broadphase.BroadPhaseLayer;
import gen.c.jolt.physics.collision.broadphase.BroadPhaseLayerInterfaceTable;
import gen.c.jolt.physics.collision.broadphase.ObjectVsBroadPhaseLayerFilterTable;
import gen.c.jolt.physics.collision.shape.BoxShape;
import gen.c.jolt.physics.collision.shape.TaperedCylinderShapeSettings;
import gen.c.jolt.physics.constraints.PathConstraintPathHermite;

public final class TeaVMCSampleMain {
    private static final String TITLE = "jJolt Samples - TeaVM C";

    private TeaVMCSampleMain() {
    }

    public static void main(String[] args) {
        System.out.println(TITLE);
        System.out.println("runtime=TeaVM C");
        System.out.println("graphics=headless native C");
        System.out.println("binding=jolt-c");

        RuntimeLoader.init(requiredLoader("runtime"));
        JoltLoader.init(requiredLoader("jolt"));

        ObjectLayerPairFilterTable pairFilter = new ObjectLayerPairFilterTable(1);
        pairFilter.EnableCollision(0, 0);
        BroadPhaseLayerInterfaceTable broadPhase = new BroadPhaseLayerInterfaceTable(1, 1);
        BroadPhaseLayer broadPhaseLayer = new BroadPhaseLayer((short)0);
        broadPhase.MapObjectToBroadPhaseLayer(0, broadPhaseLayer);
        ObjectVsBroadPhaseLayerFilterTable objectVsBroadPhase =
                new ObjectVsBroadPhaseLayerFilterTable(broadPhase, 1, pairFilter, 1);

        JoltSettings settings = new JoltSettings();
        settings.set_mMaxBodies(64);
        settings.set_mMaxBodyPairs(64);
        settings.set_mMaxContactConstraints(64);
        settings.set_mMaxWorkerThreads(0);
        settings.set_mBroadPhaseLayerInterface(broadPhase);
        settings.set_mObjectVsBroadPhaseLayerFilter(objectVsBroadPhase);
        settings.set_mObjectLayerPairFilter(pairFilter);
        JoltInterface joltInterface = new JoltInterface(settings);
        settings.dispose();

        try {
            PhysicsSystem physicsSystem = joltInterface.GetPhysicsSystem();
            BodyInterface bodyInterface = physicsSystem.GetBodyInterface();
            BodyID floor = createBox(bodyInterface, 0.0f, -0.5f, 10.0f, 0.5f,
                    EMotionType.Static, 0, 0);
            BodyID falling = createBox(bodyInterface, 0.0f, 5.0f, 0.5f, 0.5f,
                    EMotionType.Dynamic, 0, (int)0xFEDCBA98L);
            try {
                if(Integer.toUnsignedLong(bodyInterface.GetUserData(falling)) != 0xFEDCBA98L) {
                    throw new AssertionError("32-bit user data did not preserve its unsigned bit pattern");
                }

                float savedY = bodyInterface.GetPosition(falling).GetY();
                StateRecorderImpl recorder = new StateRecorderImpl();
                try {
                    physicsSystem.SaveState(recorder);
                    for(int i = 0; i < 30; i++) {
                        joltInterface.Step(1.0f / 60.0f, 1);
                    }
                    recorder.Rewind();
                    if(!physicsSystem.RestoreState(recorder)) {
                        throw new AssertionError("Unable to restore TeaVM C physics state");
                    }
                    if(Math.abs(bodyInterface.GetPosition(falling).GetY() - savedY) > 0.001f) {
                        throw new AssertionError("Restored TeaVM C body position does not match");
                    }
                }
                finally {
                    recorder.dispose();
                }

                for(int i = 0; i < 180; i++) {
                    joltInterface.Step(1.0f / 60.0f, 1);
                }
                float finalY = bodyInterface.GetPosition(falling).GetY();
                if(finalY >= savedY - 1.0f || finalY <= -0.25f) {
                    throw new AssertionError("Unexpected TeaVM C falling-body position: " + finalY);
                }

                TaperedCylinderShapeSettings tapered = new TaperedCylinderShapeSettings(1.5f, 0.5f, 0.75f, 0.05f);
                try {
                    if(!tapered.Create().IsValid()) {
                        throw new AssertionError("TeaVM C tapered cylinder creation failed");
                    }
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
                    if(!path.IsLooping()) {
                        throw new AssertionError("TeaVM C Hermite path did not retain loop state");
                    }
                }
                finally {
                    path.dispose();
                    p0.dispose();
                    p1.dispose();
                    tangent.dispose();
                    normal.dispose();
                }
            }
            finally {
                bodyInterface.RemoveBody(falling);
                bodyInterface.DestroyBody(falling);
                falling.dispose();
                bodyInterface.RemoveBody(floor);
                bodyInterface.DestroyBody(floor);
                floor.dispose();
            }

            Vec3 vector = JoltNew.Vec3(3.0f, 4.0f, 12.0f);
            try {
                float length = vector.Length();
                if(Math.abs(length - 13.0f) > 0.001f) {
                    throw new AssertionError("Unexpected Vec3 length: " + length);
                }
                System.out.println("jolt-c Vec3 length=" + length);
            }
            finally {
                vector.dispose();
            }

            if(!Quat.sIdentity().IsNormalized()) {
                throw new AssertionError("Identity quaternion should be normalized");
            }
            System.out.println("jolt-c Quat identity normalized=true");
            System.out.println("PASS: jJolt TeaVM C sample executed");
        }
        finally {
            broadPhaseLayer.dispose();
            joltInterface.dispose();
        }
    }

    private static BodyID createBox(BodyInterface bodyInterface, float x, float y, float halfX, float halfY,
            EMotionType motionType, int layer, int userData) {
        Vec3 halfExtent = new Vec3(halfX, halfY, halfX);
        Vec3 position = new Vec3(x, y, 0.0f);
        BoxShape shape = new BoxShape(halfExtent, 0.0f);
        BodyCreationSettings creation = JoltNew.BodyCreationSettings(shape, position, Quat.sIdentity(), motionType, layer);
        shape.native_releaseOwnership();
        try {
            creation.set_mUserData(userData);
            Body body = bodyInterface.CreateBody(creation);
            BodyID id = new BodyID(body.GetID().GetIndexAndSequenceNumber());
            bodyInterface.AddBody(id, motionType == EMotionType.Dynamic? EActivation.Activate : EActivation.DontActivate);
            return id;
        }
        finally {
            creation.dispose();
            shape.native_reset();
            halfExtent.dispose();
            position.dispose();
        }
    }

    private static JParserLibraryLoaderListener requiredLoader(String libraryName) {
        return (success, error) -> {
            if(!success) {
                throw new IllegalStateException("Unable to load " + libraryName, error);
            }
        };
    }
}
