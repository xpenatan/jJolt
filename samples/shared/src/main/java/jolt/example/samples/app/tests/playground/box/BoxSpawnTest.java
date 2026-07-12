package jolt.example.samples.app.tests.playground.box;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
//import imgui.ImGui;
//import imgui.idl.helper.IDLBool;
//import imgui.idl.helper.IDLFloat;
//import imgui.idl.helper.IDLInt;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import jolt.JoltNew;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.example.samples.app.jolt.Layers;
import jolt.example.samples.app.tests.Test;
import jolt.example.samples.app.tests.playground.box.shader.MyPBRDepthShaderProvider;
import jolt.example.samples.app.tests.playground.box.shader.MyPBRShaderProvider;
import jolt.gdx.JoltGdx;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.body.Body;
import jolt.physics.body.BodyCreationSettings;
import jolt.physics.body.BodyID;
import jolt.physics.body.BodyInterface;
import jolt.physics.body.MassProperties;
import jolt.physics.collision.shape.BoxShape;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class BoxSpawnTest extends Test {

    private int resetDelaySeconds = 14;

    private long timeNow;
    private long time;

    private boolean wasPlaying;
    private int totalCubes = 3000;
    private int cubeCount = 0;

    private Array<CubeData> cubes = new Array<>();
    private CubeData groundData;

    private Vec3 tempVec3;
    private Quat tempQuat;
    private Quaternion tempQuaternion;
    private Matrix4 tempRotationMatrix;
    private Model cubeModel;
    private Model cubeModelInstanced;
    private ModelInstance hardwareCubeModelInstance;
    private Matrix4 instanceTransform = new Matrix4();
    private FloatBuffer offsets;

    private SceneManager sceneManager;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    private Texture checkerBoardTexture;
    private Texture boxTexture;
    private float boxRestitution = 0.8f;
    private boolean randomRotation = false;
    private boolean renderModels = true;
    private boolean hardwareModelInstance = false;

    public void initialize() {
        mDebugRenderer.setEnable(false);
        tempVec3 = JoltNew.Vec3();
        tempQuat = JoltNew.Quat();
        tempQuaternion = new Quaternion();
        tempRotationMatrix = new Matrix4();

        sceneManager = new SceneManager( new MyPBRShaderProvider(), new MyPBRDepthShaderProvider() );
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(-0.9f, -1, -1);
        light.direction.nor();
        light.color.set(Color.WHITE);
        light.intensity = 8.8f;
        sceneManager.environment.add(light);

        sceneManager.setAmbientLight(0.5f);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        createModels();
        resetBoxes();
        setupHardwareInstance();
        updateModels();
    }

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        if(isPlaying) {
            if(!wasPlaying) {
                time = System.currentTimeMillis() + (time - timeNow);
            }
            timeNow = System.currentTimeMillis();
            long timeout = resetDelaySeconds * 1000L;
            if(timeNow - time > timeout) {
                resetBoxes();
                setupHardwareInstance();
                time = System.currentTimeMillis();
            }
        }
        wasPlaying = isPlaying;
    }

    private void createModels() {
        boxTexture = new Texture(Gdx.files.internal("data/badlogic.jpg"));
        boxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        final Material material = new Material(PBRTextureAttribute.createBaseColorTexture(boxTexture),
                FloatAttribute.createShininess(4f));
        ModelBuilder builder = new ModelBuilder();
        cubeModel = builder.createBox(1, 1, 1, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        cubeModelInstanced = builder.createBox(1, 1, 1, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

        float groundWidth = 60f;
        float groundHeight = 0.3f;
        float groundDepth = 60f;
        checkerBoardTexture = mDebugRenderer.createCheckerBoardTexture();
        final Material groundMaterial = new Material(
                PBRTextureAttribute.createBaseColorTexture(checkerBoardTexture),
                ColorAttribute.createDiffuse(1, 1, 1, 1)
        );
        int attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorUnpacked;
        Model groundBox = builder.createBox(groundWidth, groundHeight, groundDepth, groundMaterial, attributes);
        groundData = createBox(new ModelInstance(groundBox), -2, -1, 0, -2, 0, 0, 0, 0, groundWidth, groundHeight, groundDepth, 0, 0, 1);
    }

    private void setupHardwareInstance() {
        if(!hardwareModelInstance) {
            return;
        }
        int instanceCount = cubes.size;
        hardwareCubeModelInstance = new ModelInstance(cubeModelInstanced);
        for(int i = 0; i < hardwareCubeModelInstance.nodes.first().parts.size; i++) {
            Mesh mesh = hardwareCubeModelInstance.nodes.first().parts.get(i).meshPart.mesh;
            mesh.enableInstancedRendering(true, instanceCount,
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
                    new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3) );

            offsets = BufferUtils.newFloatBuffer(instanceCount * 16);
            for(int j = 0; j < instanceCount; j++) {
                float angle = 0;
                instanceTransform.setToRotationRad(Vector3.Y, angle);
                instanceTransform.setTranslation(0, 0, 0);
                offsets.put(instanceTransform.tra().getValues());
            }
            ((Buffer)offsets).position(0);
            mesh.setInstanceData(offsets);
        }
    }

    @Override
    public void postPhysicsUpdate(boolean isPlaying, float deltaTime) {
        if(isPlaying) {
            updateModels();
        }

        if(renderModels) {
            renderModels();
        }
    }

    private void updateModels() {
        for(int i = 0; i < cubes.size; i++) {
            int targetIndex = i * 16;
            CubeData cubeData = cubes.get(i);
            Body body = cubeData.body;
            Mat44 mat44 = body.GetWorldTransform();
            instanceTransform.idt();
            JoltGdx.convert(mat44, instanceTransform);

            if(cubeData.modelInstance != null) {
                cubeData.modelInstance.transform.set(instanceTransform);
            }
            else {
                offsets.position(targetIndex);
                offsets.put(instanceTransform.tra().getValues());
                Mesh mesh = hardwareCubeModelInstance.nodes.first().parts.first().meshPart.mesh;
                mesh.updateInstanceData(targetIndex, instanceTransform.getValues());
            }
        }
    }

    private void renderModels() {
        JoltGdx.convert(groundData.body.GetWorldTransform(), groundData.modelInstance.transform);

        Array<RenderableProvider> renderableProviders = sceneManager.getRenderableProviders();
        for(int i = 0; i < cubes.size; i++) {
            CubeData cubeData = cubes.get(i);
            ModelInstance modelInstance = cubeData.modelInstance;
            if(modelInstance != null) {
                renderableProviders.add(modelInstance);
            }
        }
        renderableProviders.add(groundData.modelInstance);
        if(hardwareCubeModelInstance != null) {
            renderableProviders.add(hardwareCubeModelInstance);
        }
        sceneManager.setCamera(camera);
        sceneManager.update(Gdx.graphics.getDeltaTime());
        sceneManager.render();
        renderableProviders.clear();
    }

    private void resetBoxes() {
        if(hardwareCubeModelInstance != null) {
            for(int i = 0; i < hardwareCubeModelInstance.nodes.first().parts.size; i++) {
                Mesh mesh = hardwareCubeModelInstance.nodes.first().parts.get(i).meshPart.mesh;
                mesh.disableInstancedRendering();
            }
        }

        hardwareCubeModelInstance = null;
        BodyInterface bodyInterface = mPhysicsSystem.GetBodyInterface();
        for(CubeData cubeData : cubes) {
            Body body = cubeData.body;
            BodyID bodyID = body.GetID();
            bodyInterface.RemoveBody(bodyID);
            bodyInterface.DestroyBody(bodyID);
        }
        cubes.clear();
        mPhysicsSystem.OptimizeBroadPhase();

        int base = (int) Math.round(Math.cbrt(totalCubes));
        int maxX = base;
        int maxY = base;
        int maxZ = base;

        float multi = 1.3f;
        int offsetY = 20;
        int offsetX = -8;
        int offsetZ = -6;
        cubeCount = 0;
        for(int i = 0; i < maxX; i++) {
            for(int j = 0; j < maxY; j++) {
                for(int k = 0; k < maxZ; k++) {
                    if(cubeCount < totalCubes) {
                        float x = (i + offsetX) * multi;
                        float y = (j + offsetY) * multi;
                        float z = (k + offsetZ) * multi;
                        float axisX = 1;
                        float axisY = 1;
                        float axisZ = 1;
                        if(randomRotation) {
                            axisX = MathUtils.random(0, 360);
                            axisY = MathUtils.random(0, 360);
                            axisZ = MathUtils.random(0, 360);
                        }
                        float r = 1f;
                        float g = 1f;
                        float b = 1f;
                        ModelInstance instance = null;
                        if(!hardwareModelInstance) {
                            instance = new ModelInstance(cubeModel);
                        }

                        CubeData box = createBox(instance, cubeCount, 0.4f, x, y, z, axisX, axisY, axisZ, 1, 1, 1, r, g, b);
                        cubes.add(box);
                        cubeCount++;

                        if(i == maxX-1 && j == maxY-1 && k == maxZ-1) {
                            if(cubeCount != totalCubes) {
                                maxX++;
                            }
                        }
                    }
                    else {
                        return;
                    }
                }
            }
        }
    }

    private CubeData createBox(ModelInstance modelInstance, int userData, float mass, float x, float y, float z, float axiX, float axiY, float axiZ, float x1, float y1, float z1, float colorR, float colorG, float colorB) {
        tempVec3.Set(x1 / 2f, y1 / 2f, z1 / 2f);
        BoxShape bodyShape = new BoxShape(tempVec3);

        EMotionType motionType = EMotionType.Dynamic;
        int layer = Layers.MOVING;

        tempRotationMatrix.idt();
        tempRotationMatrix.rotate(Vector3.X, axiX);
        tempRotationMatrix.rotate(Vector3.Y, axiY);
        tempRotationMatrix.rotate(Vector3.Z, axiZ);
        tempRotationMatrix.getRotation(tempQuaternion);

        tempVec3.Set(x, y, z);
        tempQuat.Set(tempQuaternion.x, tempQuaternion.y, tempQuaternion.z, tempQuaternion.w);

        MassProperties massProperties = bodyShape.GetMassProperties();
        if(mass > 0.0f) {
            massProperties.set_mMass(mass);
        }
        else if(mass < 0.0f) {
            motionType = EMotionType.Static;
            layer = Layers.NON_MOVING;
        }

        BodyCreationSettings bodySettings = JoltNew.BodyCreationSettings(bodyShape, tempVec3, tempQuat, motionType, layer);
        bodySettings.set_mMassPropertiesOverride(massProperties);
        bodySettings.set_mRestitution(boxRestitution);
        Body body = mBodyInterface.CreateBody(bodySettings);
        body.SetUserData(userData);
        bodySettings.dispose();

        CubeData cubeData = new CubeData();
        cubeData.body = body;
        cubeData.modelInstance = modelInstance;
        mBodyInterface.AddBody(body.GetID(), EActivation.Activate);
        return cubeData;
    }

    @Override
    public void dispose() {
        super.dispose();
        checkerBoardTexture.dispose();
        boxTexture.dispose();
        cubeModel.dispose();
        cubeModelInstanced.dispose();
        tempVec3.dispose();
        tempQuat.dispose();

        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }

    @Override
    public void renderUI(Batch batch, BitmapFont font) {
//        IDLBool.TMP_1.set(hardwareModelInstance);
//        if(ImGui.Checkbox("Hardware Model Instance", IDLBool.TMP_1)) {
//            hardwareModelInstance = IDLBool.TMP_1.getValue();
//        }
//        IDLBool.TMP_1.set(renderModels);
//        if(ImGui.Checkbox("Render Models", IDLBool.TMP_1)) {
//            renderModels = IDLBool.TMP_1.getValue();
//        }
//        IDLBool.TMP_1.set(randomRotation);
//        if(ImGui.Checkbox("Random Rotation", IDLBool.TMP_1)) {
//            randomRotation = IDLBool.TMP_1.getValue();
//        }
//        IDLFloat.TMP_1.set(boxRestitution);
//        if(ImGui.SliderFloat("Box Restitution", IDLFloat.TMP_1, 0.0f, 1.0f, "%.1f")) {
//            boxRestitution = IDLFloat.TMP_1.getValue();
//        }
//        IDLInt.TMP_1.set(resetDelaySeconds);
//        if(ImGui.SliderInt("Delay Seconds", IDLInt.TMP_1, 1, 20)) {
//            resetDelaySeconds = IDLInt.TMP_1.getValue();
//        }
//        IDLInt.TMP_1.set(totalCubes);
//        if(ImGui.SliderInt("Max Cubes", IDLInt.TMP_1, 9, 16000)) {
//            totalCubes = IDLInt.TMP_1.getValue();
//        }
//        ImGui.Text("Cubes: " + cubeCount);
    }

    static class CubeData {
        public Body body;
        public ModelInstance modelInstance;
    }
}