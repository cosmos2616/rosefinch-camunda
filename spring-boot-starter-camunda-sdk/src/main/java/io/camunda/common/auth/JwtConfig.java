/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
 */
package io.camunda.common.auth;

import java.util.HashMap;
import java.util.Map;

/** Contains mapping between products and their JWT credentials */
public class JwtConfig {

  private final Map<Product, JwtCredential> map;

  public JwtConfig() {
    map = new HashMap<>();
  }

  public void addProduct(final Product product, final JwtCredential jwtCredential) {
    map.put(product, jwtCredential);
  }

  public JwtCredential getProduct(final Product product) {
    return map.get(product);
  }

  public Map<Product, JwtCredential> getMap() {
    return map;
  }
}
