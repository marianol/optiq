/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.eigenbase.resource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Defining wrapper classes around resources that allow the compiler to check
 * whether the resources exist and have the argument types that your code
 * expects.
 */
public class Resources {
  private Resources() {}

  public static <T> T create(Class<T> clazz) {
    //noinspection unchecked
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
        new Class[] {clazz},
        new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            final Class<?> returnType = method.getReturnType();
            final Class[] types = {Locale.class, Method.class, Object[].class};
            final Constructor<?> constructor =
                returnType.getConstructor(types);
            return constructor.newInstance(
                EigenbaseResource.getThreadOrDefaultLocale(),
                method,
                args != null ? args : new Object[0]);
          }
        });
  }

  /** Resource instance. It contains the resource method (which
     * serves to identify the resource), the locale with which we
     * expect to render the resource, and any arguments. */
  static class Inst {
    private final Locale locale;
    protected final Method method;
    protected final Object[] args;

    public Inst(Locale locale, Method method, Object... args) {
      this.method = method;
      this.args = args;
      this.locale = locale;
    }

    public ResourceBundle bundle() {
      return EigenbaseResource.instance(locale);
    }

    public Inst localize(Locale locale) {
      return new Inst(locale, method, args);
    }

    public boolean validate(boolean fail) {
      // TODO: if bundle does not contain resource, throw
      // TODO: if method return does not match resource type, throw
      // TODO: if method parameters do not match tokens, throw
      // TODO: if there is no base message, throw
      // TODO: check that base message matches what is in resource file
      // TODO: if it is an ExInst, check that the exception class is an
      //  exception,
      //  and has constructor(String) and constructor(String, Throwable); and
      //  try creating an exception; and check that ExceptionClass is specified
      final BaseMessage baseMessage = method.getAnnotation(BaseMessage.class);
      return true;
    }

    public String str() {
      String message = raw();
      MessageFormat format = new MessageFormat(message);
      format.setLocale(locale);
      return format.format(args);
    }

    public String raw() {
      return bundle().getString(key());
    }

    private String key() {
      final Resource resource = method.getAnnotation(Resource.class);
      if (resource != null) {
        return resource.value();
      } else {
        final String name = method.getName();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
      }
    }
  }

  /** Sub-class of {@link Inst} that can throw an exception. */
  public static class ExInst<T extends Exception> extends Inst {
    public ExInst(Locale locale, Method method, Object... args) {
      super(locale, method, args);
    }

    @Override public Inst localize(Locale locale) {
      return new ExInst<T>(locale, method, args);
    }

    public T ex() {
      return ex(null);
    }

    public T ex(Throwable cause) {
      try {
        //noinspection unchecked
        final Class<? extends Exception> exceptionClass = getExceptionClass();
        final Constructor<? extends Exception> constructor =
            exceptionClass.getConstructor(String.class, Throwable.class);
        final String str = str();
        return (T) constructor.newInstance(str, cause);
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    private Class<? extends Exception> getExceptionClass() {
      final ExceptionClass exceptionClass =
          method.getAnnotation(ExceptionClass.class);
      return exceptionClass.value();
    }
  }

  /** The message in the default locale. */
  @Retention(RetentionPolicy.RUNTIME)
  @interface BaseMessage {
    String value();
  }

  /** The name of the property in the resource file. */
  @Retention(RetentionPolicy.RUNTIME)
  @interface Resource {
    String value();
  }

  /** The name of the class of exception to throw. */
  @Retention(RetentionPolicy.RUNTIME)
  @interface ExceptionClass {
    Class<? extends Exception> value();
  }

  /** Property of a resource. */
  @Retention(RetentionPolicy.RUNTIME)
  @interface Property {
    String name();
    String value();
  }
}

// End Resources.java
