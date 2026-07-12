package jolt;

import com.github.xpenatan.jParser.loader.JParserLibraryLoader;
import com.github.xpenatan.jParser.loader.JParserLibraryLoaderListener;
import com.github.xpenatan.jparser.runtime.RuntimeLoader;

/**
 * @author xpenatan
 */
public class JoltLoader {

    /*[-JNI;-NATIVE]
        #include "JoltCustom.h"
    */

    /*[-FFM;-NATIVE]
        #include "JoltCustom.h"
    */

    public static void init(JParserLibraryLoaderListener listener) {
        RuntimeLoader.init(new JParserLibraryLoaderListener() {
            @Override
            public void onLoad(boolean idl_isSuccess, Throwable idl_t) {
                if(idl_isSuccess) {
                    JParserLibraryLoader.load("jolt", listener);
                }
                else {
                    listener.onLoad(false, idl_t);
                }
            }
        });
    }
}
