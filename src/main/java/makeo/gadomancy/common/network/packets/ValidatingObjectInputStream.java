package makeo.gadomancy.common.network.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.List;

import makeo.gadomancy.common.Gadomancy;

public class ValidatingObjectInputStream extends ObjectInputStream {

    private static final List<String> WHITELIST = Arrays
            .asList("java.util.HashMap", "java.lang.Integer", "java.lang.Number", "[Ljava.lang.String;");

    public ValidatingObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        if (!WHITELIST.contains(name)) {
            Gadomancy.log.warn(Gadomancy.securityMarker, "Received packet containing disallowed class: " + name);
            throw new RuntimeException();
        }
        return super.resolveClass(desc);
    }
}
