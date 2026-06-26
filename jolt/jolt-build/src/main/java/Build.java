import com.github.xpenatan.jParser.builder.BuildMultiTarget;
import com.github.xpenatan.jParser.builder.targets.AndroidTarget;
import com.github.xpenatan.jParser.builder.targets.EmscriptenTarget;
import com.github.xpenatan.jParser.builder.targets.LinuxTarget;
import com.github.xpenatan.jParser.builder.targets.MacTarget;
import com.github.xpenatan.jParser.builder.targets.WindowsMSVCTarget;
import com.github.xpenatan.jParser.builder.tool.BuildToolListener;
import com.github.xpenatan.jParser.builder.tool.BuildToolOptions;
import com.github.xpenatan.jParser.builder.tool.BuilderTool;
import com.github.xpenatan.jParser.idl.IDLClassOrEnum;
import com.github.xpenatan.jParser.idl.IDLHelper;
import com.github.xpenatan.jParser.idl.IDLReader;
import com.github.xpenatan.jParser.idl.IDLRenaming;
import java.util.ArrayList;

public class Build {

    private static boolean double_precision = false;

    public static void main(String[] args) {
        String libName = "jolt";
        String modulePrefix = "jolt";
        String basePackage = "jolt";
        String sourcePath =  "/build/jolt";
//        String sourcePath =  "E:\\Dev\\Projects\\cpp\\JoltPhysics";

        IDLHelper.cppConverter = idlType -> {
            if(idlType.equals("unsigned long long")) {
                return "uint64";
            }
            return null;
        };
        IDLHelper.javaConverter = idlType -> {
            if(idlType.equals("IDLArray")) {
                return "NativeArray";
            }
            if(idlType.equals("IDLFloatArray")) {
                return "NativeFloatArray";
            }
            if(idlType.equals("IDLString")) {
                return "NativeString";
            }
            if(idlType.equals("BodyID[]")) {
                return "IDLArrayBodyID";
            }
            if(idlType.equals("NativeArrayBodyID")) {
                return "IDLArrayBodyID";
            }
            return null;
        };

        BuildToolOptions.BuildToolParams data = new BuildToolOptions.BuildToolParams();
        data.libName = libName;
        data.idlName = libName;
        data.webModuleName = libName;
        data.packageName = basePackage;
        data.cppSourcePath = sourcePath;
        data.modulePrefix = modulePrefix;

        BuildToolOptions op = new BuildToolOptions(data, args);
        op.addAdditionalIDLRefPath(IDLReader.getRuntimeHelperFile());
        if(double_precision) {
            op.addAdditionalIDLPath(IDLReader.parseFile(op.getCPPPath() + "jolt_double.idl"));
        }
        else {
            op.addAdditionalIDLPath(IDLReader.parseFile(op.getCPPPath() + "jolt_float.idl"));
        }

        BuilderTool.build(op, new BuildToolListener() {
            @Override
            public void onAddTarget(BuildToolOptions op, IDLReader idlReader, ArrayList<BuildMultiTarget> targets) {
                if(op.containsArg("web_wasm")) {
                    targets.add(getTeaVMTarget(op, idlReader));
                }
                if(op.containsArg("windows64_jni")) {
                    targets.add(getWindowTarget(op, false));
                }
                if(op.containsArg("linux64_jni")) {
                    targets.add(getLinuxTarget(op, false));
                }
                if(op.containsArg("mac64_jni")) {
                    targets.add(getMacTarget(op, false, false));
                }
                if(op.containsArg("macArm_jni")) {
                    targets.add(getMacTarget(op, true, false));
                }
                if(op.containsArg("android_jni")) {
                    targets.add(getAndroidTarget(op));
                }
//                if(op.containsArg("iOS")) {
//                    targets.add(getIOSTarget(op));
//                }
                if(op.containsArg("windows64_ffm")) {
                    targets.add(getWindowTarget(op, true));
                }
                if(op.containsArg("linux64_ffm")) {
                    targets.add(getLinuxTarget(op, true));
                }
                if(op.containsArg("mac64_ffm")) {
                    targets.add(getMacTarget(op, false, true));
                }
                if(op.containsArg("macArm_ffm")) {
                    targets.add(getMacTarget(op, true, true));
                }
            }
        }, new IDLRenaming() {
            @Override
            public String obtainNewPackage(IDLClassOrEnum idlClassOrEnum, String classPackage) {
                // This remove duplicate jolt name in package.
                // The reason for this is that the lib name start with jolt and there is an already c++ jolt subfolder
                return classPackage.replace("jolt", "");
            }
        });
    }

    private static BuildMultiTarget getWindowTarget(BuildToolOptions op, boolean isFFM) {
        BuildMultiTarget multiTarget = new BuildMultiTarget();
        String libBuildCPPPath = op.getModuleBuildCPPPath();
        String sourceDir = op.getSourceDir();
        String api = isFFM ? "ffm" : "jni";
        String idlDir = libBuildCPPPath + "/src/idl";
        String runtimeDir = libBuildCPPPath + "/src/runtime";

//        WindowsMSVCTarget.DEBUG_BUILD = true;

        // Make a static library
        WindowsMSVCTarget compileStaticTarget = new WindowsMSVCTarget();
        compileStaticTarget.libDirSuffix += api;
        compileStaticTarget.isStatic = true;
        compileStaticTarget.cppFlags.add("-std:c++17");
        compileStaticTarget.headerDirs.add("-I" + sourceDir);
        compileStaticTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        compileStaticTarget.headerDirs.add("-I" + idlDir);
        compileStaticTarget.headerDirs.add("-I" + runtimeDir);
        compileStaticTarget.cppInclude.add(sourceDir + "/Jolt/**.cpp");
        compileStaticTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        compileStaticTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        compileStaticTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        compileStaticTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        compileStaticTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        multiTarget.add(compileStaticTarget);

        // Compile glue code and link
        WindowsMSVCTarget linkTarget = new WindowsMSVCTarget();
        linkTarget.libDirSuffix += api;
        if(isFFM) {
            linkTarget.setupFFMGlueCode(libBuildCPPPath);
        }
        else {
            linkTarget.setupJNIGlueCode(libBuildCPPPath);
        }
        linkTarget.cppFlags.add("-std:c++17");
        linkTarget.headerDirs.add("-I" + sourceDir);
        linkTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        linkTarget.headerDirs.add("-I" + idlDir);
        linkTarget.headerDirs.add("-I" + runtimeDir);
        linkTarget.linkerFlags.add("/WHOLEARCHIVE:" + libBuildCPPPath + "/libs/windows/vc/" + api + "/jolt64_.lib");
        linkTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        linkTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        linkTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        linkTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        linkTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        linkTarget.linkerFlags.add("-DLL");
        multiTarget.add(linkTarget);

        return multiTarget;
    }

    private static BuildMultiTarget getLinuxTarget(BuildToolOptions op, boolean isFFM) {
        BuildMultiTarget multiTarget = new BuildMultiTarget();
        String libBuildCPPPath = op.getModuleBuildCPPPath();
        String sourceDir = op.getSourceDir();
        String api = isFFM ? "ffm" : "jni";
        String idlDir = libBuildCPPPath + "/src/idl";
        String runtimeDir = libBuildCPPPath + "/src/runtime";

        // Make a static library
        LinuxTarget compileStaticTarget = new LinuxTarget();
        compileStaticTarget.libDirSuffix += api;
        compileStaticTarget.isStatic = true;
        compileStaticTarget.cppFlags.add("-std=c++17");
        compileStaticTarget.cppFlags.add("-fPIC");
        compileStaticTarget.headerDirs.add("-I" + sourceDir);
        compileStaticTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        compileStaticTarget.headerDirs.add("-I" + idlDir);
        compileStaticTarget.headerDirs.add("-I" + runtimeDir);
        compileStaticTarget.cppInclude.add(sourceDir + "/Jolt/**.cpp");
        compileStaticTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        compileStaticTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        compileStaticTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        compileStaticTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        compileStaticTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        multiTarget.add(compileStaticTarget);

        // Compile glue code and link
        LinuxTarget linkTarget = new LinuxTarget();
        linkTarget.libDirSuffix += api;
        if(isFFM) {
            linkTarget.setupFFMGlueCode(libBuildCPPPath);
        }
        else {
            linkTarget.setupJNIGlueCode(libBuildCPPPath);
        }
        linkTarget.cppFlags.add("-std=c++17");
        linkTarget.cppFlags.add("-fPIC");
        linkTarget.headerDirs.add("-I" + sourceDir);
        linkTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        linkTarget.headerDirs.add("-I" + idlDir);
        linkTarget.headerDirs.add("-I" + runtimeDir);
        linkTarget.linkerFlags.add("-Wl,--whole-archive");
        linkTarget.linkerFlags.add(libBuildCPPPath + "/libs/linux/" + api + "/libjolt64_.a");
        linkTarget.linkerFlags.add("-Wl,--no-whole-archive");
        linkTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        linkTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        linkTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        linkTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        linkTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        multiTarget.add(linkTarget);

        return multiTarget;
    }

    private static BuildMultiTarget getMacTarget(BuildToolOptions op, boolean isArm, boolean isFFM) {
        BuildMultiTarget multiTarget = new BuildMultiTarget();
        String libBuildCPPPath = op.getModuleBuildCPPPath();
        String sourceDir = op.getSourceDir();
        String api = isFFM ? "ffm" : "jni";
        String idlDir = libBuildCPPPath + "/src/idl";
        String runtimeDir = libBuildCPPPath + "/src/runtime";

        // Make a static library
        MacTarget compileStaticTarget = new MacTarget(isArm);
        compileStaticTarget.libDirSuffix += api;
        compileStaticTarget.isStatic = true;
        compileStaticTarget.cppFlags.add("-std=c++17");
        compileStaticTarget.cppFlags.add("-fPIC");
        compileStaticTarget.headerDirs.add("-I" + sourceDir);
        compileStaticTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        compileStaticTarget.headerDirs.add("-I" + idlDir);
        compileStaticTarget.headerDirs.add("-I" + runtimeDir);
        compileStaticTarget.cppInclude.add(sourceDir + "/Jolt/**.cpp");
        compileStaticTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        compileStaticTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        compileStaticTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        compileStaticTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        compileStaticTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        multiTarget.add(compileStaticTarget);

        // Compile glue code and link
        MacTarget linkTarget = new MacTarget(isArm);
        linkTarget.libDirSuffix += api;
        if(isFFM) {
            linkTarget.setupFFMGlueCode(libBuildCPPPath);
        }
        else {
            linkTarget.setupJNIGlueCode(libBuildCPPPath);
        }
        linkTarget.cppFlags.add("-std=c++17");
        linkTarget.cppFlags.add("-fPIC");
        linkTarget.headerDirs.add("-I" + sourceDir);
        linkTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        linkTarget.headerDirs.add("-I" + idlDir);
        linkTarget.headerDirs.add("-I" + runtimeDir);
        if(isArm) {
            linkTarget.linkerFlags.add("-Wl,-force_load");
            linkTarget.linkerFlags.add(libBuildCPPPath + "/libs/mac/arm/" + api + "/libjolt64_.a");
        }
        else {
            linkTarget.linkerFlags.add("-Wl,-force_load");
            linkTarget.linkerFlags.add(libBuildCPPPath + "/libs/mac/" + api + "/libjolt64_.a");
        }
        linkTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        linkTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        linkTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        linkTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        linkTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        multiTarget.add(linkTarget);

        return multiTarget;
    }

    private static BuildMultiTarget getTeaVMTarget(BuildToolOptions op, IDLReader idlReader) {
        BuildMultiTarget multiTarget = new BuildMultiTarget();
        String libBuildCPPPath = op.getModuleBuildCPPPath();
        String sourceDir = op.getSourceDir();
        String idlDir = libBuildCPPPath + "/src/idl";
        String runtimeDir = libBuildCPPPath + "/src/runtime";

        EmscriptenTarget.DEBUG_BUILD = false;

        // Make a static library
        EmscriptenTarget compileStaticTarget = new EmscriptenTarget();
        compileStaticTarget.isStatic = true;
        compileStaticTarget.cppFlags.add("-std=c++17");
        compileStaticTarget.compileGlueCode = false;
        compileStaticTarget.headerDirs.add("-I" + sourceDir);
        compileStaticTarget.headerDirs.add("-I" + op.getCustomSourceDir());
        compileStaticTarget.headerDirs.add("-I" + idlDir);
        compileStaticTarget.headerDirs.add("-I" + runtimeDir);
        compileStaticTarget.cppInclude.add(sourceDir + "/Jolt/**.cpp");
        compileStaticTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        compileStaticTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        compileStaticTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        compileStaticTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        compileStaticTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        compileStaticTarget.cppFlags.add("-msimd128");
        compileStaticTarget.cppFlags.add("-msse4.2");
        multiTarget.add(compileStaticTarget);

        // Compile glue code and link
        EmscriptenTarget linkTarget = new EmscriptenTarget();
        linkTarget.mainModuleName = "runtime";
        linkTarget.idlReader = idlReader;
        linkTarget.cppFlags.add("-std=c++17");
        linkTarget.headerDirs.add("-I" + sourceDir);
        linkTarget.headerDirs.add("-I" + idlDir);
        linkTarget.headerDirs.add("-I" + runtimeDir);
        linkTarget.headerDirs.add("-include" + op.getCustomSourceDir() + "JoltCustom.h");
        linkTarget.linkerFlags.add("-Wl,--whole-archive");
        linkTarget.linkerFlags.add(libBuildCPPPath + "/libs/emscripten/jolt_.a");
        linkTarget.linkerFlags.add("-Wl,--no-whole-archive");
        linkTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
        linkTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
        linkTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
        linkTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
        linkTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
        linkTarget.cppFlags.add("-msimd128");
        linkTarget.cppFlags.add("-msse4.2");
        linkTarget.linkerFlags.add("-sSIDE_MODULE=1");
        linkTarget.linkerFlags.add("-lc++abi"); // C++ ABI (exceptions, thread_atexit, etc.)
        linkTarget.linkerFlags.add("-lc++"); // C++ STL (std::cout, std::string, etc.)
        linkTarget.linkerFlags.add("-lc"); // C standard library (fopen, fclose, printf, etc.)
        multiTarget.add(linkTarget);

        return multiTarget;
    }

    private static BuildMultiTarget getAndroidTarget(BuildToolOptions op) {
        BuildMultiTarget multiTarget = new BuildMultiTarget();
        String sourceDir = op.getSourceDir();
        String libBuildCPPPath = op.getModuleBuildCPPPath();
        String idlDir = libBuildCPPPath + "/src/idl";
        String runtimeDir = libBuildCPPPath + "/src/runtime";

        AndroidTarget.ApiLevel apiLevel = AndroidTarget.ApiLevel.Android_10_29;
        ArrayList<AndroidTarget.Target> targets = new ArrayList<>();

        targets.add(AndroidTarget.Target.x86);
        targets.add(AndroidTarget.Target.x86_64);
        targets.add(AndroidTarget.Target.armeabi_v7a);
        targets.add(AndroidTarget.Target.arm64_v8a);

        for(int i = 0; i < targets.size(); i++) {
            AndroidTarget.Target target = targets.get(i);

            // Make a static library
            AndroidTarget compileStaticTarget = new AndroidTarget(target, apiLevel);
            compileStaticTarget.isStatic = true;
            compileStaticTarget.cppFlags.add("-std=c++17");
            compileStaticTarget.cppCompiler.add("-fPIC");
            compileStaticTarget.headerDirs.add("-I" + sourceDir);
            compileStaticTarget.headerDirs.add("-I" + op.getCustomSourceDir());
            compileStaticTarget.headerDirs.add("-I" + idlDir);
            compileStaticTarget.headerDirs.add("-I" + runtimeDir);
            compileStaticTarget.cppInclude.add(sourceDir + "/Jolt/**.cpp");
            compileStaticTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
            compileStaticTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
            compileStaticTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
            compileStaticTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
            compileStaticTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
            multiTarget.add(compileStaticTarget);

            // Compile glue code and link
            AndroidTarget linkTarget = new AndroidTarget(target, apiLevel);
            linkTarget.setupJNIGlueCode(libBuildCPPPath);
            linkTarget.cppFlags.add("-std=c++17");
            linkTarget.cppCompiler.add("-fPIC");
            linkTarget.headerDirs.add("-I" + sourceDir);
            linkTarget.headerDirs.add("-I" + op.getCustomSourceDir());
            linkTarget.headerDirs.add("-I" + idlDir);
            linkTarget.headerDirs.add("-I" + runtimeDir);
            linkTarget.linkerFlags.add("-Wl,--whole-archive");
            linkTarget.linkerFlags.add(libBuildCPPPath + "/libs/android/" + target.getFolder() +"/lib" + op.libName + ".a");
            linkTarget.linkerFlags.add("-Wl,--no-whole-archive");
            linkTarget.cppFlags.add("-DJPH_DEBUG_RENDERER");
            linkTarget.cppFlags.add("-DJPH_DISABLE_CUSTOM_ALLOCATOR");
            linkTarget.cppFlags.add("-DJPH_ENABLE_ASSERTS");
            linkTarget.cppFlags.add("-DJPH_CROSS_PLATFORM_DETERMINISTIC");
            linkTarget.cppFlags.add("-DJPH_OBJECT_LAYER_BITS=32");
            linkTarget.linkerFlags.add("-Wl,-z,max-page-size=16384");
            multiTarget.add(linkTarget);
        }
        return multiTarget;
    }
}
