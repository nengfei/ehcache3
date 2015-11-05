/*
 * Copyright Terracotta, Inc.
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
package org.ehcache.internal.classes;

import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ClassInstanceProviderTest {

  @Test
  public void testNewInstanceUsingAliasAndNoArgs() throws Exception {
    ClassInstanceProvider<String, TestService> classInstanceProvider = new ClassInstanceProvider<String, TestService>(null, (Class)ClassInstanceConfiguration.class);

    classInstanceProvider.preconfigured.put("test stuff", new ClassInstanceConfiguration<TestService>(TestService.class));
    TestService obj = classInstanceProvider.newInstance("test stuff", (ServiceConfiguration) null);

    assertThat(obj.theString, is(nullValue()));
  }

  @Test
  public void testNewInstanceUsingAliasAndArg() throws Exception {
    ClassInstanceProvider<String, TestService> classInstanceProvider = new ClassInstanceProvider<String, TestService>(null, (Class)ClassInstanceConfiguration.class);

    classInstanceProvider.preconfigured.put("test stuff", new ClassInstanceConfiguration<TestService>(TestService.class, "test string"));
    TestService obj = classInstanceProvider.newInstance("test stuff", (ServiceConfiguration<?>) null);

    assertThat(obj.theString, equalTo("test string"));
  }

  @Test
  public void testNewInstanceUsingServiceConfig() throws Exception {
    ClassInstanceProvider<String, TestService> classInstanceProvider = new ClassInstanceProvider<String, TestService>(null, (Class)ClassInstanceConfiguration.class);

    TestServiceConfiguration config = new TestServiceConfiguration();
    TestService obj = classInstanceProvider.newInstance("test stuff", config);

    assertThat(obj.theString, is(nullValue()));
  }

  @Test
  public void testNewInstanceUsingServiceConfigFactory() throws Exception {
    TestServiceProviderConfiguration factoryConfig = new TestServiceProviderConfiguration();
    factoryConfig.getDefaults().put("test stuff", new ClassInstanceConfiguration<TestService>(TestService.class));

    ClassInstanceProvider<String, TestService> classInstanceProvider = new ClassInstanceProvider<String, TestService>(factoryConfig, (Class)ClassInstanceConfiguration.class);
    classInstanceProvider.start(null);

    TestService obj = classInstanceProvider.newInstance("test stuff", (ServiceConfiguration) null);
    assertThat(obj.theString, is(nullValue()));
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testReleaseInstanceByAnotherProvider() throws Exception {
    ClassInstanceProvider<String, String> classInstanceProvider = new ClassInstanceProvider<String, String>(null, null);
    
    classInstanceProvider.releaseInstance("foo");
  }

  @Test
  public void testReleaseCloseableInstance() throws Exception {
    ClassInstanceProvider<String, Closeable> classInstanceProvider = new ClassInstanceProvider<String, Closeable>(null, null);
    Closeable closeable = mock(Closeable.class);
    classInstanceProvider.created.add(closeable);

    classInstanceProvider.releaseInstance(closeable);
    verify(closeable).close();
  }

  @Test(expected = IOException.class)
  public void testReleaseCloseableInstanceThrows() throws Exception {
    ClassInstanceProvider<String, Closeable> classInstanceProvider = new ClassInstanceProvider<String, Closeable>(null, null);
    Closeable closeable = mock(Closeable.class);
    doThrow(IOException.class).when(closeable).close();
    classInstanceProvider.created.add(closeable);

    classInstanceProvider.releaseInstance(closeable);
  }

  public static class TestService implements Service {
    public final String theString;

    public TestService() {
      this(null);
    }

    public TestService(String theString) {
      this.theString = theString;
    }

    @Override
    public void start(ServiceProvider serviceProvider) {
    }

    @Override
    public void stop() {
    }
  }

  public static class TestServiceConfiguration extends ClassInstanceConfiguration<TestService> implements ServiceConfiguration<TestService> {
    public TestServiceConfiguration() {
      super(TestService.class);
    }

    @Override
    public Class<TestService> getServiceType() {
      return TestService.class;
    }
  }

  public static class TestServiceProviderConfiguration extends ClassInstanceProviderConfiguration<String, TestService> implements ServiceConfiguration<TestService> {
    @Override
    public Class<TestService> getServiceType() {
      return TestService.class;
    }
  }

}
