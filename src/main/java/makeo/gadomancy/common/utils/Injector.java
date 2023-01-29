package makeo.gadomancy.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import sun.misc.Unsafe;

import com.google.common.base.Throwables;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 02.12.13 18:45
 */
public class Injector {

    static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    Class<?> clazz;
    Object object;

    public Injector(Object object, Class<?> clazz) {
        this.object = object;
        this.clazz = clazz;
    }

    public Injector() {
        this(null, null);
    }

    public Injector(Object object) {
        this(object, object.getClass());
    }

    public Injector(Class<?> clazz) {
        this.object = null;
        this.clazz = clazz;
    }

    public Injector(String clazz) throws IllegalArgumentException {
        this.object = null;
        try {
            this.clazz = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class does not exist!");
        }
    }

    public void setObjectClass(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getObjectClass() {
        return this.clazz;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return this.object;
    }

    public <T> T invokeConstructor(Object... params) {
        return this.invokeConstructor(this.extractClasses(params), params);
    }

    public <T> T invokeConstructor(Class<?> clazz, Object param) {
        return this.invokeConstructor(new Class[] { clazz }, param);
    }

    public <T> T invokeConstructor(Class<?>[] classes, Object... params) {
        try {
            Constructor<?> constructor = this.clazz.getDeclaredConstructor(classes);
            this.object = constructor.newInstance(params);
            return (T) this.object;
        } catch (Exception e) { // NoSuchMethodException | InvocationTargetException | InstantiationException |
            // IllegalAccessException
            e.printStackTrace();
        }
        return null;
    }

    public <T> T invokeUnsafeConstructor(Class<?>[] paramTypes, Object... params) {
        try {
            final Method constructor = this.clazz.getMethod("gadomancyRawCreate", paramTypes);
            Object created = constructor.invoke(null, params);
            return (T) created;
        } catch (Throwable e) {
            Throwables.propagate(e);
        }
        throw new IllegalStateException();
    }

    public <T> T invokeMethod(String name, Object... params) {
        return this.invokeMethod(name, this.extractClasses(params), params);
    }

    public <T> T invokeMethod(String name, Class clazz, Object param) {
        return this.invokeMethod(name, new Class[] { clazz }, param);
    }

    public <T> T invokeMethod(String name, Class[] classes, Object... params) {
        try {
            Method method = this.clazz.getDeclaredMethod(name, classes);
            return this.invokeMethod(method, params);
        } catch (Exception e) { // NoSuchMethodException | ClassCastException
            e.printStackTrace();
        }
        return null;
    }

    public <T> T invokeMethod(Method method, Object... params) {
        try {
            method.setAccessible(true);
            Object result = method.invoke(this.object, params);
            if (result != null) return (T) result;
        } catch (Exception e) { // InvocationTargetException | IllegalAccessException | ClassCastException
            e.printStackTrace();
        }
        return null;
    }

    private Class[] extractClasses(Object... objects) {
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) classes[i] = objects[i].getClass();
        return classes;
    }

    public boolean setField(String name, Object value) {
        try {
            return this.setField(this.clazz.getDeclaredField(name), value);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setField(Field field, Object value) {
        try {
            if (Modifier.isFinal(field.getModifiers())) {
                if (value != null && !field.getType().isAssignableFrom(value.getClass())) {
                    throw new ClassCastException("Can't assign " + value.getClass() + " to " + field.getType());
                }
                Object base = object;
                if (object == null) {
                    base = UNSAFE.staticFieldBase(field);
                }
                final long offset = Modifier.isStatic(field.getModifiers()) ? UNSAFE.staticFieldOffset(field)
                        : UNSAFE.objectFieldOffset(field);
                UNSAFE.putObject(base, offset, value);
                return true;
            }

            field.setAccessible(true);
            field.set(this.object, value);
            return true;
        } catch (Exception e) { // IllegalAccessException | NoSuchFieldException
            e.printStackTrace();
            return false;
        }
    }

    public boolean setFieldInt(Field field, int value) {
        try {
            if (Modifier.isFinal(field.getModifiers())) {
                if (!field.getType().equals(int.class)) {
                    throw new ClassCastException("Can't assign int to " + field.getType());
                }
                Object base = object;
                if (object == null) {
                    base = UNSAFE.staticFieldBase(field);
                }
                final long offset = Modifier.isStatic(field.getModifiers()) ? UNSAFE.staticFieldOffset(field)
                        : UNSAFE.objectFieldOffset(field);
                UNSAFE.putInt(base, offset, value);
                return true;
            }

            field.setAccessible(true);
            field.setInt(this.object, value);
            return true;
        } catch (Exception e) { // IllegalAccessException | NoSuchFieldException
            e.printStackTrace();
            return false;
        }
    }

    public <T> T getField(String name) {
        try {
            return this.getField(this.clazz.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getField(Field field) {
        try {
            field.setAccessible(true);
            Object result = field.get(this.object);
            if (result != null) return (T) result;
        } catch (Exception e) { // IllegalAccessException | ClassCastException
            e.printStackTrace();
        }
        return null;
    }

    public Method findMethod(Class returnType, Class... paramTypes) {
        return Injector.findMethod(this.clazz, returnType, paramTypes);
    }

    public Field findField(Class type) {
        return Injector.findField(this.clazz, type);
    }

    public static Method findMethod(Class clazz, Class returnType, Class[] paramTypes) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (Arrays.equals(m.getParameterTypes(), paramTypes) && m.getReturnType().equals(returnType)) {
                return m;
            }
        }
        return null;
    }

    public static <E> Method findMethod(Class<? super E> clazz, String[] methodNames, Class<?>... methodTypes) {
        return ReflectionHelper.findMethod(clazz, null, methodNames, methodTypes);
    }

    public static Field findField(Class clazz, String... names) {
        return ReflectionHelper.findField(clazz, names);
    }

    public static Field findField(Class clazz, Class type) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getType().equals(type)) {
                return f;
            }
        }
        return null;
    }

    public static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method getMethod(String name, Class clazz, Class... classes) {
        if (clazz == null) return null;

        try {
            return clazz.getDeclaredMethod(name, classes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method getMethod(String name, String clazz, Class... classes) {
        return Injector.getMethod(name, Injector.getClass(clazz), classes);
    }

    public static Field getField(String name, Class clazz) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}
