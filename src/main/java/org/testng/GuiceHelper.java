package org.testng;

import static java.util.Collections.unmodifiableList;
import static org.testng.internal.Utils.isStringEmpty;
import static org.testng.internal.Utils.isStringNotEmpty;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import java.util.function.BiPredicate;
import org.testng.annotations.Guice;
import org.testng.collections.Lists;
import org.testng.internal.ClassHelper;
import org.testng.internal.ClassImpl;
import org.testng.internal.InstanceCreator;
import org.testng.internal.annotations.AnnotationHelper;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

public class GuiceHelper {
  private final ITestContext context;

  GuiceHelper(ITestContext context) {
    this.context = context;
  }

  Injector getInjector(IClass iClass, IInjectorFactory injectorFactory) {
    Guice guice =
        AnnotationHelper.findAnnotationSuperClasses(Guice.class, iClass.getRealClass());
    if (guice == null) {
      return null;
    }
    if (iClass instanceof TestClass) {
      iClass = ((TestClass) iClass).getIClass();
    }
    if (!(iClass instanceof ClassImpl)) {
      return null;
    }
    Injector parentInjector = ((ClassImpl) iClass).getParentInjector(injectorFactory);

    List<Module> classLevelModules = getModules(guice, parentInjector, iClass.getRealClass());
    Module parentModule = getParentModule(context);
    if (parentModule != null) {
      context.addGuiceModule(parentModule);
      BiPredicate<Module, Module> condition = (m,n) -> m.getClass().equals(n.getClass());
      classLevelModules = Lists.merge(classLevelModules, Collections.singletonList(parentModule), condition);
    }
    List<Module> moduleLookup = Lists.newArrayList(classLevelModules);

    // Get an injector with the class's modules + any defined parent module installed
    // Reuse the previous injector, if any, but don't create a child injector as JIT bindings can conflict
    Injector injector = context.getInjector(moduleLookup);
    if (injector == null) {
      injector = createInjector(context, injectorFactory, classLevelModules);
      context.addInjector(classLevelModules, injector);
    }
    return injector;
  }

  @SuppressWarnings("unchecked")
  private static Module getParentModule(ITestContext context) {
    if (isStringEmpty(context.getSuite().getParentModule())) {
      return null;
    }
    Class<?> parentModule = ClassHelper.forName(context.getSuite().getParentModule());
    if (parentModule == null) {
      throw new TestNGException(
              "Cannot load parent Guice module class: " + context.getSuite().getParentModule());
    }
    if (!Module.class.isAssignableFrom(parentModule)) {
      throw new TestNGException("Provided class is not a Guice module: " + parentModule.getName());
    }
    if (!context.getGuiceModules((Class<? extends Module>) parentModule).isEmpty()) {
      return context.getGuiceModules((Class<? extends Module>) parentModule).get(0);
    }
    Module obj;
    try {
      Constructor<?> moduleConstructor = parentModule.getDeclaredConstructor(ITestContext.class);
      obj = (Module) InstanceCreator.newInstance(moduleConstructor, context);
    } catch (NoSuchMethodException e) {
      obj = (Module) InstanceCreator.newInstance(parentModule);
    }
      context.addGuiceModule(obj);
    return obj;
  }

  public static Injector createInjector(ITestContext context,
      IInjectorFactory injectorFactory, List<Module> moduleInstances) {
    Module parentModule = getParentModule(context);
    BiPredicate<Module, Module> condition = (m,n) -> m.getClass().equals(n.getClass());
    List<Module> fullModules = Lists.newArrayList(moduleInstances);
    if (parentModule != null) {
      fullModules = Lists.merge(fullModules, Collections.singletonList(parentModule), condition);
    }
    Stage stage = Stage.DEVELOPMENT;
    String stageString = context.getSuite().getGuiceStage();
    if (isStringNotEmpty(stageString)) {
      stage = Stage.valueOf(stageString);
    }
    fullModules.forEach(context::addGuiceModule);
    return injectorFactory.getInjector(stage, fullModules.toArray(new Module[0]));
  }

  private List<Module> getModules(Guice guice, Injector parentInjector, Class<?> testClass) {
    List<Module> result = Lists.newArrayList();
    BiPredicate<Module, Module> condition = (m,n) -> m.getClass().equals(n.getClass());
    for (Class<? extends Module> moduleClass : guice.modules()) {
      List<Module> modules = context.getGuiceModules(moduleClass);
      if (modules != null && !modules.isEmpty()) {
        result.addAll(modules);
        result = Lists.merge(result, modules, condition);
      } else {
        Module instance = parentInjector.getInstance(moduleClass);
        result = Lists.merge(result, Collections.singletonList(instance), condition);
        context.addGuiceModule(instance);
      }
    }
    Class<? extends IModuleFactory> factory = guice.moduleFactory();
    if (factory != IModuleFactory.class) {
      IModuleFactory factoryInstance = parentInjector.getInstance(factory);
      Module module = factoryInstance.createModule(context, testClass);
      if (module != null) {
        result = Lists.merge(result, Collections.singletonList(module), condition);
      }
    }
    result = Lists.merge(result, LazyHolder.getSpiModules(), condition);
    result = Lists.merge(result, context.getGuiceModules(), condition);
    return result;
  }

  private static final class LazyHolder {
    private static final List<Module> spiModules;

    static {
      List<Module> modules = new ArrayList<>();
      for (IModule module : ServiceLoader.load(IModule.class)) {
        modules.add(module.getModule());
      }
      spiModules = unmodifiableList(modules);
    }

    public static List<Module> getSpiModules() {
      return spiModules;
    }
  }
}