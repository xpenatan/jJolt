package jolt.example.samples.app.teavmc;

import com.github.xpenatan.jParser.loader.JParserLibraryLoaderListener;
import com.github.xpenatan.jparser.runtime.RuntimeLoader;
import gen.c.jolt.Jolt;
import gen.c.jolt.JoltLoader;
import gen.c.jolt.JoltNew;
import gen.c.jolt.core.Factory;
import gen.c.jolt.math.Quat;
import gen.c.jolt.math.Vec3;

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

        Jolt.Init();
        Factory factory = JoltNew.Factory();
        Factory.set_sInstance(factory);
        Jolt.RegisterTypes();

        try {
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
            Jolt.UnregisterTypes();
            Factory.set_sInstance(Factory.NULL);
            factory.dispose();
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
