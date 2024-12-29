package makeo.gadomancy.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Throwables;

import sun.misc.Unsafe;

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

    public void setObject(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return this.object;
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

    public Field findField(Class type) {
        return Injector.findField(this.clazz, type);
    }

    public static Field findField(Class clazz, Class type) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getType().equals(type)) {
                return f;
            }
        }
        return null;
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

    public static Field getField(String name, Class clazz) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}
