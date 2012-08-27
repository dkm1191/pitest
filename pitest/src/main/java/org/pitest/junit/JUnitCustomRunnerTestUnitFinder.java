/*
 * Copyright 2010 Henry Coles
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runners.Parameterized;
import org.pitest.extension.TestUnit;
import org.pitest.extension.TestUnitFinder;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.Option;
import org.pitest.internal.IsolationUtils;
import org.pitest.junit.adapter.AdaptedJUnitTestUnit;
import org.pitest.reflection.IsAnotatedWith;
import org.pitest.reflection.Reflection;

public class JUnitCustomRunnerTestUnitFinder implements TestUnitFinder {

  public Collection<TestUnit> findTestUnits(final Class<?> clazz) {

    final Runner runner = AdaptedJUnitTestUnit.createRunner(clazz);
    if (isNotARunnableTest(runner, clazz.getName())) {
      return Collections.emptyList();
    }

    if (Filterable.class.isAssignableFrom(runner.getClass())
        && !shouldTreatAsOneUnit(clazz)) {
      return splitIntoFilteredUnits(runner.getDescription());
    } else {
      return Collections.<TestUnit> singletonList(new AdaptedJUnitTestUnit(
          clazz, Option.<Filter> none()));
    }
  }

  private boolean isNotARunnableTest(final Runner runner, final String className) {
    return (runner == null)
        || runner.getClass().isAssignableFrom(ErrorReportingRunner.class)
        || isParameterizedTest(runner) || isAJUnitThreeErrorOrWarning(runner)
        || isJUnitThreeSuiteMethodNotForOwnClass(runner, className);
  }

  private boolean isAJUnitThreeErrorOrWarning(final Runner runner) {
    return !runner.getDescription().getChildren().isEmpty()
        && runner.getDescription().getChildren().get(0).getClassName()
            .startsWith("junit.framework.TestSuite");
  }

  private boolean shouldTreatAsOneUnit(final Class<?> clazz) {
    final Set<Method> methods = Reflection.allMethods(clazz);
    return hasAnnotation(methods, BeforeClass.class)
        || hasAnnotation(methods, AfterClass.class);
  }

  private boolean hasAnnotation(final Set<Method> methods,
      final Class<? extends Annotation> annotation) {
    return FCollection.contains(methods, IsAnotatedWith.instance(annotation));
  }

  private boolean isParameterizedTest(final Runner runner) {
    return Parameterized.class.isAssignableFrom(runner.getClass());
  }

  private boolean isJUnitThreeSuiteMethodNotForOwnClass(final Runner runner,
      final String className) {
    // use strings in case this hack blows up due to internal junit change
    return runner.getClass().getName()
        .equals("org.junit.internal.runners.SuiteMethod")
        && !runner.getDescription().getClassName().equals(className);
  }

  private Collection<TestUnit> splitIntoFilteredUnits(
      final Description description) {
    return FCollection.filter(description.getChildren(), isTest()).map(
        descriptionToTestUnit());

  }

  private F<Description, TestUnit> descriptionToTestUnit() {
    return new F<Description, TestUnit>() {

      public TestUnit apply(final Description a) {
        return descriptionToTest(a);
      }

    };
  }

  private F<Description, Boolean> isTest() {
    return new F<Description, Boolean>() {

      public Boolean apply(final Description a) {
        return a.isTest();
      }

    };
  }

  private TestUnit descriptionToTest(final Description description) {

    Class<?> clazz = description.getTestClass();
    if (clazz == null) {
      clazz = IsolationUtils.convertForClassLoader(
          IsolationUtils.getContextClassLoader(), description.getClassName());
    }
    return new AdaptedJUnitTestUnit(clazz,
        Option.some(createFilterFor(description)));
  }

  private Filter createFilterFor(final Description description) {
    return new DescriptionFilter(description.toString());
  }

}
