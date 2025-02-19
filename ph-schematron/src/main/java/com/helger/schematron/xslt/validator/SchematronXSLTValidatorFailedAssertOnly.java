/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.schematron.xslt.validator;

import javax.annotation.Nonnull;

import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;

import com.helger.commons.state.EValidity;

/**
 * A special implementation of {@link ISchematronXSLTValidator} that only
 * handles failed asserts as failures, but not successful reports.
 * 
 * @author Philip Helger
 */
public class SchematronXSLTValidatorFailedAssertOnly implements ISchematronXSLTValidator
{
  @Nonnull
  public EValidity getSchematronValidity (@Nonnull final SchematronOutputType aSO)
  {
    for (final Object aObj : aSO.getActivePatternAndFiredRuleAndFailedAssert ())
      if (aObj instanceof FailedAssert)
        return EValidity.INVALID;
    return EValidity.VALID;
  }
}
