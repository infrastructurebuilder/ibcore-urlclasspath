/*
 * @formatter:off
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * @formatter:on
 */
package org.infrastructurebuilder.util.core.fs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassGraph.ClasspathElementFilter;
import io.github.classgraph.ClassGraph.ClasspathElementURLFilter;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList.ResourceFilter;
import io.github.classgraph.ScanResult;

public class ClasspathConfig {
  private final AtomicReference<ClassGraph> graph = new AtomicReference<>();
  private final Map<String, Object> c;

  public ClasspathConfig(Map<String, Object> config) {
    this.c = Optional.ofNullable(config).orElse(new HashMap<>());
  }

  public Optional<Integer> getInteger(String key, Integer def) {
    Integer ret = null;
    var obj = c.getOrDefault(key, def);
    if (obj instanceof String s) {
      try {
        ret = Integer.parseInt(s);
      } catch (NumberFormatException nfe) {
        throw new RuntimeException("Error parsing int from {}".formatted(s));
      }
    }
    return Optional.ofNullable(ret);

  }

  private boolean bool(String key) {
    Boolean ret = null;
    var obj = c.getOrDefault(key, null);
    if (obj instanceof String s) {
      ret = Boolean.parseBoolean(s);
    }
    return Optional.ofNullable(ret).orElse(false);
  }

  public AndResourceFilter filter() {
    AndResourceFilter arf = new AndResourceFilter();
    if (bool("resourcesOnly")) // else keeps us from mutually excluding
      arf.add(NON_CLASSFILE_FILTER);
    else if (bool("classesOnly"))
      arf.add(CLASSFILE_FILTER);
    return arf;
  }

  void validate() {
    try {
      if (graph.get() == null) {
        ClassGraph cg;
        Object o = null;
        try {
          o = c.getOrDefault("prebuiltClassGraph", new ClassGraph());
          cg = (ClassGraph) o;
        } catch (ClassCastException cce) {
          throw new RuntimeException("Bad prebuiltClassGraph {}".formatted(o), cce);
        }
        if (bool("verbose"))
          cg = cg.verbose();

        cg = cg.acceptClasses(strArr("acceptClasses"))//
            .acceptClasspathElementsContainingResourcePath(strArr("acceptClasspathElementsContainingResourcePath"))//
            .acceptJars(strArr("acceptJars"))//
            .acceptLibOrExtJars(strArr("acceptLibOrExtJars"))//
            .acceptModules(strArr("acceptModules"))//
            .acceptPackages(strArr("acceptPackages"))//
            .acceptPackagesNonRecursive(strArr("acceptPackagesNonRecursive"))//
            .acceptPaths(strArr("acceptPaths"))//
            .acceptPathsNonRecursive(strArr("acceptPathsNonRecursive"))//
        ;

        if (bool("disableJars"))
          cg = cg.disableJarScanning();
        if (bool("disableDirs"))
          cg = cg.disableDirScanning();
        if (bool("disableModules"))
          cg = cg.disableModuleScanning();
        if (bool("disableNestedJars"))
          cg = cg.disableNestedJarScanning();
        if (bool("disableRuntimeInvisibleAnnotations"))
          cg = cg.disableRuntimeInvisibleAnnotations();

        if (bool("enableAllInfo"))
          cg = cg.enableAllInfo();
        else {
          if (bool("enableClassInfo"))
            cg = cg.enableClassInfo();
          if (bool("enableFieldInfo"))
            cg = cg.enableFieldInfo();
          if (bool("enableMethodInfo"))
            cg = cg.enableMethodInfo();
          if (bool("enableAnnotationInfo"))
            cg = cg.enableAnnotationInfo();
          if (bool("enableStaticFinalFieldConstantInitializerValues"))
            cg = cg.enableStaticFinalFieldConstantInitializerValues();
          if (bool("ignoreClassVisibility"))
            cg = cg.ignoreClassVisibility();
          if (bool("ignoreFieldVisibility"))
            cg = cg.ignoreFieldVisibility();
          if (bool("ignoreMethodVisibility"))
            cg = cg.ignoreMethodVisibility();
        }

        if (bool("enableExternalClasses"))
          cg = cg.enableExternalClasses();
        if (bool("enableInterClassDependencies"))
          cg = cg.enableInterClassDependencies();
        if (bool("enableMemoryMapping"))
          cg = cg.enableMemoryMapping();
        if (bool("enableMultiReleaseVersions"))
          cg = cg.enableMultiReleaseVersions();
        if (bool("enableRealtimeLogging"))
          cg = cg.enableRealtimeLogging();
        if (bool("enableRemoteJarScanning"))
          cg = cg.enableRemoteJarScanning();
        if (bool("enableSystemJarsAndModules"))
          cg = cg.enableSystemJarsAndModules();

        for (String eus1 : strArr("enableURLScheme"))
          cg = cg.enableURLScheme(eus1);

        if (c.containsKey("filterClasspathElements")) {
          Object o1 = c.get("filterClasspathElements");
          ClasspathElementFilter[] ccc = new ClasspathElementFilter[0];
          if (o1 instanceof ClasspathElementFilter q) {
            ccc = new ClasspathElementFilter[1];
            ccc[0] = q;
          } else if (o1 instanceof ClasspathElementFilter[] qc) {
            ccc = qc;
          } else {
            throw new ClassCastException("ClasspathElementFilter must be singleton or array {}".formatted(o1));
          }
          for (ClasspathElementFilter cpe : ccc)
            cg = cg.filterClasspathElements(cpe);
        }

        if (c.containsKey("filterClasspathElementsByURL")) {
          Object o1 = c.get("filterClasspathElementsByURL");
          ClasspathElementURLFilter[] ccc = new ClasspathElementURLFilter[0];
          if (o1 instanceof ClasspathElementURLFilter q) {
            ccc = new ClasspathElementURLFilter[1];
            ccc[0] = q;
          } else if (o1 instanceof ClasspathElementURLFilter[] qc) {
            ccc = qc;
          } else {
            throw new ClassCastException("ClasspathElementFilter must be singleton or array {}".formatted(o1));
          }
          for (ClasspathElementURLFilter cpe : ccc)
            cg = cg.filterClasspathElementsByURL(cpe);
        }

        if (bool("ignoreParentClassLoaders"))
          cg = cg.ignoreParentClassLoaders();
        if (bool("ignoreParentModuleLayers"))
          cg = cg.ignoreParentModuleLayers();
        if (bool("initializeLoadedClasses"))
          cg = cg.initializeLoadedClasses();

        cg = cg.rejectClasses(strArr("rejectClasses"))//
            .rejectClasspathElementsContainingResourcePath(strArr("rejectClasspathElementsContainingResourcePath"))//
            .rejectJars(strArr("rejectJars"))//
            .rejectLibOrExtJars(strArr("rejectLibOrExtJars"))//
            .rejectModules(strArr("rejectModules"))//
            .rejectPackages(strArr("rejectPackages"))//
            .rejectPaths(strArr("rejectPaths"));//

        Integer i = getInteger("maxBufferedJarRAMSize", null).orElse(null);
        if (i != null)
          cg = cg.setMaxBufferedJarRAMSize(i);

        ClassLoader[] ocl = (ClassLoader[]) c.getOrDefault("overrideClassLoaders", new ClassLoader[0]);
        if (ocl.length > 0)
          cg = cg.overrideClassLoaders(ocl);

        Object ocp = c.get("overrideClasspath");
        if (ocp != null) {
          if (ocp instanceof String ocps)
            cg = cg.overrideClasspath(ocps);
          else if (ocp instanceof Iterable<?> jj)
            cg = cg.overrideClasspath(jj);
          else
            cg = cg.overrideClasspath(ocp);
        }

        Object ml = c.get("overrideModuleLayers");
        if (ml != null)
          cg = cg.overrideModuleLayers(ml);

        this.graph.compareAndSet(null, cg);
      }
    } catch (Throwable t) {
      throw new IllegalArgumentException(t);
    }
  }

  ScanResult scan() {
    if (graph.get() == null)
      validate();

    Integer threads = getInteger("threads", null).orElse(null);
    return threads != null ? graph.get().scan(threads) : graph.get().scan();

  }

  private String[] strArr(String string) {
    return strArr(string, new String[0]);
  }

  private String[] strArr(String key, String[] def) {
    Object v = c.getOrDefault(key, def);
    if (v instanceof String s) {
      v = s.split(",");
    }
    try {
      return (String[]) v;
    } catch (ClassCastException cce) {
      throw new RuntimeException("Cannot cast %s to String[]".formatted(v), cce);
    }
  }

  private static final ResourceFilter CLASSFILE_FILTER = new ResourceFilter() {
    @Override
    public boolean accept(final Resource resource) {
      final String path = resource.getPath();
      if (!path.endsWith(".class") || path.length() < 7) {
        return false;
      }
      // Check filename is not simply ".class"
      final char c = path.charAt(path.length() - 7);
      return c != '/' && c != '.';
    }
  };

  private static final ResourceFilter NON_CLASSFILE_FILTER = new ResourceFilter() {

    @Override
    public boolean accept(Resource resource) {
      return !CLASSFILE_FILTER.accept(resource);
    }

  };

  @Override
  public int hashCode() {
    return Objects.hash(c);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ClasspathConfig other = (ClasspathConfig) obj;
    return Objects.equals(c, other.c);
  }

}
