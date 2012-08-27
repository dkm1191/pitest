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

package org.pitest.mutationtest;

import org.pitest.coverage.CoverageDatabase;
import org.pitest.functional.F;
import org.pitest.mutationtest.report.MutationHtmlReportListener;
import org.pitest.mutationtest.report.ResultOutputStrategy;
import org.pitest.mutationtest.report.SourceLocator;

public class HtmlReportFactory implements ListenerFactory {

  private final ResultOutputStrategy outputStrategy;

  public HtmlReportFactory(final ResultOutputStrategy outputStrategy) {
    this.outputStrategy = outputStrategy;
  }

  public MutationResultListener getListener(final CoverageDatabase coverage,
      final long startTime, final SourceLocator locator) {
    return new MutationHtmlReportListener(coverage, this.outputStrategy,
        locator);
  }

  public static F<ResultOutputStrategy, ListenerFactory> createFactoryFunction() {
    return new F<ResultOutputStrategy, ListenerFactory>() {

      public ListenerFactory apply(final ResultOutputStrategy outputStrategy) {
        return new HtmlReportFactory(outputStrategy);
      }

    };
  }

}
