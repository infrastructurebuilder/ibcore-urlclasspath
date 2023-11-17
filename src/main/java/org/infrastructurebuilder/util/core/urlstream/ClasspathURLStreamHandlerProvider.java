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
package org.infrastructurebuilder.util.core.urlstream;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class ClasspathURLStreamHandlerProvider extends URLStreamHandlerProvider {

  public ClasspathURLStreamHandlerProvider() {
    System.out.println("Loaded " + getClass().getCanonicalName());
  }
  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    System.out.println("Handling " + protocol + "?");
    if ("classpath".equals(protocol)) {
      return new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
          return ClassLoader.getSystemClassLoader().getResource(u.getPath()).openConnection();
        }
      };
    }
    return null;
  }

}
