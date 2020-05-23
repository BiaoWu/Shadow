package com.tencent.shadow.dynamic.host;

import java.util.LinkedList;

/**
 * @author BiaoWu
 */
public class ProcessClassLoader extends ClassLoader {
    private static final ProcessClassLoader CLASS_LOADER = new ProcessClassLoader();

    private LinkedList<ClassLoader> classLoaders = new LinkedList<>();
    private static final Object lock = new Object();

    private ProcessClassLoader(){
        super(ProcessClassLoader.class.getClassLoader());
    }

    public static ProcessClassLoader getInstance(){
        return CLASS_LOADER;
    }

    public static void addClassLoader(ClassLoader classLoader){
        synchronized (lock){
            getInstance().classLoaders.add(classLoader);
        }
    }

    public static void removeClassLoader(ClassLoader classLoader){
        synchronized (lock){
            getInstance().classLoaders.remove(classLoader);
        }

    }

    public static void clearClassLoader(){
        synchronized (lock){
            getInstance().classLoaders.clear();
        }

    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {

            }

            if (c == null) {
                for (ClassLoader loader : classLoaders) {
                    try {
                        c = loader.loadClass(name);
                        if (c != null) {
                            break;
                        }
                    } catch (ClassNotFoundException e) {

                    }
                }

                if (c == null) {
                    throw new ClassNotFoundException(name);
                }
            }
        }
        return c;
    }

    /**
     * 从apk中读取接口的实现
     *
     * @param clazz     接口类
     * @param className 实现类的类名
     * @param <T>       接口类型
     * @return 所需接口
     * @throws Exception
     */
    <T> T getInterface(Class<T> clazz, String className) throws Exception {
        try {
            Class<?> interfaceImplementClass = loadClass(className);
            Object interfaceImplement = interfaceImplementClass.newInstance();
            return clazz.cast(interfaceImplement);
        } catch (ClassNotFoundException | InstantiationException
                | ClassCastException | IllegalAccessException e) {
            throw new Exception(e);
        }
    }
}
