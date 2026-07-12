package jolt.example.samples.app.tests;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import jolt.JoltNew;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.example.samples.app.jolt.JoltInstance;
import jolt.example.samples.app.jolt.Layers;
import jolt.example.samples.app.math.Perlin;
import jolt.gdx.JoltDebugRenderer;
import jolt.geometry.Triangle;
import jolt.geometry.TriangleList;
import jolt.math.Float3;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.PhysicsSystem;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyInterface;
import jolt.physics.collision.PhysicsMaterialList;
import jolt.physics.collision.shape.BoxShape;
import jolt.physics.collision.shape.Shape;
import jolt.physics.collision.shape.ShapeResult;

public abstract class Test {

    protected JoltInstance joltInstance = null;
    protected PhysicsSystem mPhysicsSystem = null;
    protected BodyInterface mBodyInterface = null;
    protected JoltDebugRenderer mDebugRenderer = null;

    protected PerspectiveCamera camera;
    protected Matrix4 cameraPivot = new Matrix4();

    public void setJoltInstance(JoltInstance joltInstance) {
        this.joltInstance = joltInstance;
        this.mPhysicsSystem = joltInstance.getPhysicsSystem();
        mBodyInterface = mPhysicsSystem.GetBodyInterface();
    }

    public void setRenderer(JoltDebugRenderer mDebugRenderer) {
        this.mDebugRenderer = mDebugRenderer;
    }

    public void dispose() {

    }

    public void prePhysicsUpdate(boolean isPlaying) {
    }

    public void postPhysicsUpdate(boolean isPlaying, float deltaTime) {
    }

    public void processInput() {
    }

    public void renderUI(Batch batch, BitmapFont font) {}

    public final void initializeCamera(PerspectiveCamera camera) {
        this.camera = camera;
    }

    public void updateCamera(CameraInputController cameraController) {
    }

    public void initialize() {}

    protected float getWorldScale() { return 1.0f; }

    protected Body createFloor() {
        return createFloor(200f);
    }

    protected Body createFloor(float inSize) {
        float scale = getWorldScale();
        Vec3 inHalfExtent = JoltNew.Vec3(scale * (0.5f * inSize), scale * 1.0f, scale * (0.5f * inSize));
        Vec3 inPosition = JoltNew.Vec3(0.0f, scale * -1.0f, 0.0f);
        Quat inRotation = Quat.sIdentity();
        BoxShape bodyShape = new BoxShape(inHalfExtent, 0.0f);
        BodyCreationSettings bodySettings = JoltNew.BodyCreationSettings(bodyShape, inPosition, inRotation, EMotionType.Static, Layers.NON_MOVING);
        Body body = mBodyInterface.CreateBody(bodySettings);
        mBodyInterface.AddBody(body.GetID(), EActivation.DontActivate);
        bodySettings.dispose();
        inHalfExtent.dispose();
        inPosition.dispose();
        return body;
    }

    private float height(float x, float y) {
        return MathUtils.sin(x / 2) * MathUtils.cos(y / 3);
    }

    private void createMeshFloor(int n,  int cellSize, int maxHeight, float posX, float posY, float posZ) {
        // Create regular grid of triangles

        TriangleList triangles = new TriangleList();
        triangles.resize(n * n * 2);
        for (int x = 0; x < n; ++x)
            for (int z = 0; z < n; ++z) {
                float center = n * cellSize / 2.0f;

                float x1 = cellSize * x - center;
                float z1 = cellSize * z - center;
                float x2 = x1 + cellSize;
                float z2 = z1 + cellSize;

                {
                    Triangle t = triangles.at((x * n + z) * 2);
                    Float3 v1 = t.get_mV(0);
                    v1.set_x(x1);
                    v1.set_y(height(x, z));
                    v1.set_z(z1);
                    Float3 v2 = t.get_mV(1);
                    v2.set_x(x1);
                    v2.set_y(height(x, z + 1));
                    v2.set_z(z2);
                    Float3 v3 = t.get_mV(2);
                    v3.set_x(x2);
                    v3.set_y(height(x + 1, z + 1));
                    v3.set_z(z2);
                }

                {
                    Triangle t = triangles.at((x * n + z) * 2 + 1);
                    Float3 v1 = t.get_mV(0);
                    v1.set_x(x1);
                    v1.set_y(height(x, z));
                    v1.set_z(z1);
                    Float3 v2 = t.get_mV(1);
                    v2.set_x(x2);
                    v2.set_y(height(x + 1, z + 1));
                    v2.set_z(z2);
                    Float3 v3 = t.get_mV(2);
                    v3.set_x(x2);
                    v3.set_y(height(x + 1, z));
                    v3.set_z(z1);
                }
            }
        PhysicsMaterialList materials = new PhysicsMaterialList();
        ShapeResult shapeResult = JoltNew.MeshShapeSettings(triangles, materials).Create();
        triangles.dispose();
        materials.dispose();
        boolean hasError = shapeResult.HasError();
        boolean isValid = shapeResult.IsValid();
        System.out.println("ShapeResult hasError: " + hasError);
        System.out.println("ShapeResult isValid: " + isValid);
//        IDLString idlString = shapeResult.GetError();
//        long cPointer = idlString.getCPointer();
//        String data = idlString.data();
//        System.out.println("ShapeResult GetError: " + data);
        Shape shape = shapeResult.Get();
        // Create body
        Vec3 vec3 = JoltNew.Vec3(posX, posY, posZ);
        BodyCreationSettings creationSettings = JoltNew.BodyCreationSettings(shape, vec3, JoltNew.Quat(0, 0, 0, 1), EMotionType.Static, Layers.NON_MOVING);
        Body body = mBodyInterface.CreateBody(creationSettings);
        creationSettings.dispose();
        vec3.dispose();
        addToScene(body, 0xc7c7c7);
    }

    void addToScene(Body body, int color) {
        mBodyInterface.AddBody(body.GetID(), EActivation.Activate);

//        addToThreeScene(body, color);
    }

    float GetWorldScale() { return 1.0f; }

    protected Body CreateMeshTerrain() {
        float scale = GetWorldScale();
        int n = 100;
        float max_height = scale * 3.0f;
        float cell_size = scale * 1.0f;

        float[][] heights = new float[n + 1][n + 1];
        for (int x = 0; x <= n; ++x)
            for (int z = 0; z <= n; ++z)
                heights[x][z] = max_height * Perlin.perlinNoise3((float)x * 8.0f / n, 0, (float)z * 8.0f / n, 256, 256, 256);

        // Create regular grid of triangles
        TriangleList triangles = new TriangleList();
        for (int x = 0; x < n; ++x) {
            for (int z = 0; z < n; ++z)
            {
                float center = n * cell_size / 2;

                float x1 = cell_size * x - center;
                float z1 = cell_size * z - center;
                float x2 = x1 + cell_size;
                float z2 = z1 + cell_size;

                Vec3 v1 = JoltNew.Vec3(x1, heights[x][z], z1);
                Vec3 v2 = JoltNew.Vec3(x2, heights[x + 1][z], z1);
                Vec3 v3 = JoltNew.Vec3(x1, heights[x][z + 1], z2);
                Vec3 v4 = JoltNew.Vec3(x2, heights[x + 1][z + 1], z2);

                Triangle triangle1 = new Triangle(v1, v3, v4);
                Triangle triangle2 = new Triangle(v1, v4, v2);
                triangles.push_back(triangle1);
                triangles.push_back(triangle2);

                triangle1.dispose();
                triangle2.dispose();
                v1.dispose();
                v2.dispose();
                v3.dispose();
                v4.dispose();
            }
        }

        int NON_MOVING = 4;
        BodyCreationSettings bodyCreationSettings = JoltNew.BodyCreationSettings(JoltNew.MeshShapeSettings(triangles), Vec3.sZero(), Quat.sIdentity(), EMotionType.Static, NON_MOVING);
        Body floor = mBodyInterface.CreateBody(bodyCreationSettings);
        mBodyInterface.AddBody(floor.GetID(), EActivation.DontActivate);
        triangles.dispose();
        bodyCreationSettings.dispose();
        return floor;
    }

    public void renderUI() {

    }
}