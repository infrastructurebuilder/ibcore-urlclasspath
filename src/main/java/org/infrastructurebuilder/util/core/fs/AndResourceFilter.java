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

import java.util.ArrayList;
import java.util.List;

import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList.ResourceFilter;

public class AndResourceFilter implements ResourceFilter {
  private final List<ResourceFilter> filters = new ArrayList<>();

  public AndResourceFilter() {
    this(new ArrayList<>());
  }

  public AndResourceFilter(List<ResourceFilter> list) {
    if (list != null)
      filters.addAll(list);
  }

  public void add(ResourceFilter rf) {
    if (rf != null)
      filters.add(rf);
  }

  @Override
  public boolean accept(Resource resource) {
    boolean accept = true;
    for (ResourceFilter f : filters) {
      if (!f.accept(resource)) {
        accept = false;
        break;
      }
    }
    return accept;
  }

}
