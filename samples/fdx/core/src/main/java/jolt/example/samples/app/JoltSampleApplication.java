package jolt.example.samples.app;

import io.github.libfdx.Fdx;
import io.github.libfdx.application.Application;
import io.github.libfdx.application.ApplicationAdapter;
import io.github.libfdx.assets.AssetDescriptor;
import io.github.libfdx.assets.AssetManager;
import io.github.libfdx.assets.DefaultAssetManager;
import io.github.libfdx.core.FdxException;
import io.github.libfdx.core.Logger;
import io.github.libfdx.display.Display;
import io.github.libfdx.graphics.camera.Camera;
import io.github.libfdx.graphics.camera.CameraProjection;
import io.github.libfdx.graphics.GraphicsContext;
import io.github.libfdx.graphics.LoadOp;
import io.github.libfdx.graphics.g3d.DefaultModelInstance;
import io.github.libfdx.graphics.g3d.DirectionalLight;
import io.github.libfdx.graphics.g3d.Environment3D;
import io.github.libfdx.graphics.g3d.G3DAssetLoaders;
import io.github.libfdx.graphics.g3d.Model;
import io.github.libfdx.graphics.g3d.ModelBatch;
import io.github.libfdx.graphics.g3d.ModelBuilder;
import io.github.libfdx.input.Input;
import io.github.libfdx.input.InputAdapter;
import io.github.libfdx.input.Key;
import io.github.libfdx.input.KeyEvent;
import io.github.libfdx.math.Color;
import io.github.libfdx.math.Matrix4;
import jolt.JoltLoader;
import jolt.fdx.FdxDebugRenderer;
import jolt.physics.body.BodyManagerDrawSettings;

public final class JoltSampleApplication extends ApplicationAdapter {
    private static final String GLTF_CUBE = "cube.gltf";

    private final long exitAfterFrames;
    private Application application;
    private Display display;
    private Logger logger;
    private AssetManager assets;
    private GraphicsContext graphics;
    private Input input;
    private InputAdapter inputProcessor;
    private ModelBatch modelBatch;
    private Camera camera;
    private JoltSampleWorld world;
    private FdxDebugRenderer debugRenderer;
    private BodyManagerDrawSettings debugDrawSettings;
    private Model floorModel;
    private Model cubeModel;
    private DefaultModelInstance floorInstance;
    private DefaultModelInstance cubeInstance;
    private boolean debugEnabled;
    private boolean created;
    private volatile boolean joltLoaded;
    private volatile Throwable joltLoadError;
    private long renderedFrames;

    public JoltSampleApplication() {
        this(0L);
    }

    public JoltSampleApplication(long exitAfterFrames) {
        this(exitAfterFrames, true);
    }

    public JoltSampleApplication(long exitAfterFrames, boolean debugEnabled) {
        this.exitAfterFrames = exitAfterFrames;
        this.debugEnabled = debugEnabled;
    }

    @Override
    public void create(Fdx fdx) {
        application = fdx.app();
        display = fdx.displays().main();
        graphics = fdx.graphics().main();
        input = fdx.input();
        logger = fdx.logger();
        installDebugToggle();

        assets = new DefaultAssetManager(fdx.files());
        G3DAssetLoaders.register(assets, graphics);
        assets.load(AssetDescriptor.of(GLTF_CUBE, Model.class));
        assets.finishLoading();

        modelBatch = new ModelBatch(graphics);
        modelBatch.environment(new Environment3D()
                .ambientColor(new Color(0.24f, 0.25f, 0.28f, 1.0f))
                .add(new DirectionalLight().direction(-0.35f, -0.75f, -1.0f).intensity(1.4f)));

        ModelBuilder builder = new ModelBuilder(graphics);
        floorModel = builder.box("jolt-floor", 22.0f, 0.5f, 22.0f);
        cubeModel = assets.get(GLTF_CUBE, Model.class);
        floorInstance = new DefaultModelInstance(floorModel)
                .transform(Matrix4.translation(0.0f, -0.25f, 0.0f));
        cubeInstance = new DefaultModelInstance(cubeModel);

        camera = new Camera()
                .projection(CameraProjection.PERSPECTIVE)
                .fieldOfView(67.0f)
                .nearFar(0.1f, 200.0f)
                .position(7.5f, 5.5f, 9.0f)
                .lookAt(0.0f, 1.6f, 0.0f);

        JoltLoader.init((success, throwable) -> {
            if (success) {
                joltLoaded = true;
            }
            else {
                joltLoadError = throwable != null ? throwable
                        : new FdxException("jJolt runtime loader returned failure");
            }
        });
        if (joltLoadError != null) {
            throw new FdxException("Failed to load the jJolt runtime", joltLoadError);
        }
        if (joltLoaded) {
            createWorld();
        }
    }

    @Override
    public void render() {
        if (!created) {
            if (joltLoadError != null) {
                throw new FdxException("Failed to load the jJolt runtime", joltLoadError);
            }
            if (!joltLoaded) {
                return;
            }
            createWorld();
        }

        float delta = Math.min(application.deltaTime(), 1.0f / 30.0f);
        world.update(delta);
        cubeInstance.transform(world.dynamicBoxTransform());
        camera.viewport(framebufferWidth(), framebufferHeight()).update();

        modelBatch.begin(LoadOp.clear(0.04f, 0.045f, 0.06f, 1.0f), camera);
        modelBatch.render(floorInstance);
        modelBatch.render(cubeInstance);
        modelBatch.end();
        if (debugEnabled) {
            debugRenderer.DrawBodies(world.physicsSystem(), debugDrawSettings);
            debugRenderer.render(camera.combined());
        }

        renderedFrames++;
        if (renderedFrames % 120L == 0L) {
            logger.info("jJolt sample rendered " + renderedFrames + " frames on " + graphics.providerId());
        }
        if (exitAfterFrames > 0L && renderedFrames >= exitAfterFrames) {
            application.requestExit();
        }
    }

    @Override
    public void dispose() {
        if (input != null && inputProcessor != null) {
            input.removeProcessor(inputProcessor);
            inputProcessor = null;
        }
        if (debugRenderer != null) {
            debugRenderer.dispose();
            debugRenderer = null;
        }
        if (debugDrawSettings != null) {
            debugDrawSettings.dispose();
            debugDrawSettings = null;
        }
        if (modelBatch != null) {
            modelBatch.dispose();
            modelBatch = null;
        }
        if (assets != null) {
            assets.dispose();
            assets = null;
        }
        if (floorModel != null) {
            floorModel.dispose();
            floorModel = null;
        }
        if (world != null) {
            world.dispose();
            world = null;
        }
        if (!created) {
            throw new FdxException("jJolt libfdx sample did not create");
        }
        if (exitAfterFrames > 0L && renderedFrames < exitAfterFrames) {
            throw new FdxException("jJolt libfdx sample rendered " + renderedFrames + " of "
                    + exitAfterFrames + " required frames");
        }
        if (logger != null) {
            logger.info("jJolt libfdx sample disposed after " + renderedFrames + " frames");
        }
    }

    private int framebufferWidth() {
        int width = display.framebufferWidth() > 0 ? display.framebufferWidth() : display.width();
        return width > 0 ? width : 960;
    }

    private int framebufferHeight() {
        int height = display.framebufferHeight() > 0 ? display.framebufferHeight() : display.height();
        return height > 0 ? height : 540;
    }

    private void createWorld() {
        world = new JoltSampleWorld();
        debugRenderer = new FdxDebugRenderer(graphics);
        debugRenderer.setEnabled(debugEnabled);
        debugDrawSettings = new BodyManagerDrawSettings();
        debugDrawSettings.set_mDrawShape(true);
        debugDrawSettings.set_mDrawShapeWireframe(true);
        created = true;
        logger.info("jJolt libfdx sample created with graphics provider " + graphics.providerId()
                + ", glTF asset " + GLTF_CUBE + ", debug renderer " + (debugEnabled ? "enabled" : "disabled"));
    }

    private void installDebugToggle() {
        if (input == null) {
            return;
        }
        inputProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(KeyEvent event) {
                if (event.key() == Key.D) {
                    debugEnabled = !debugEnabled;
                    if (debugRenderer != null) {
                        debugRenderer.setEnabled(debugEnabled);
                        debugRenderer.clear();
                    }
                    logger.info("jJolt debug renderer " + (debugEnabled ? "enabled" : "disabled"));
                    return true;
                }
                return false;
            }
        };
        input.addProcessor(inputProcessor);
    }
}
