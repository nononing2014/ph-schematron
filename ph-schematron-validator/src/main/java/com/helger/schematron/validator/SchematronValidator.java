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
package com.helger.schematron.validator;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.transform.Source;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.error.EErrorLevel;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.microdom.IMicroNode;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.xml.sax.CollectingSAXErrorHandler;
import com.helger.commons.xml.transform.TransformSourceFactory;
import com.helger.schematron.CSchematron;
import com.helger.schematron.relaxng.RelaxNGCompactSchemaCache;

/**
 * Helper class that validates a Schematron against the RelaxNG Compact scheme.
 * This class is not suitable for validating an XML instance against a
 * Schematron instance.
 *
 * @author Philip Helger
 */
@Immutable
public final class SchematronValidator
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SchematronValidator.class);

  @PresentForCodeCoverage
  private static final SchematronValidator s_aInstance = new SchematronValidator ();

  private SchematronValidator ()
  {}

  /**
   * Check if the passed micro node is a valid schematron instance.
   *
   * @param aNode
   *        The micro node to check. May be <code>null</code>.
   * @return <code>true</code> if the schematron is valid, <code>false</code>
   *         otherwise.
   */
  public static boolean isValidSchematron (@Nullable final IMicroNode aNode)
  {
    if (aNode == null)
      return false;

    return isValidSchematron (TransformSourceFactory.create (MicroWriter.getXMLString (aNode)));
  }

  /**
   * Check if the passed DOM node is a valid schematron instance.
   *
   * @param aNode
   *        The DOM node to check. May be <code>null</code>.
   * @return <code>true</code> if the schematron is valid, <code>false</code>
   *         otherwise.
   */
  public static boolean isValidSchematron (@Nullable final Node aNode)
  {
    if (aNode == null)
      return false;

    return isValidSchematron (TransformSourceFactory.create (aNode));
  }

  /**
   * Check if the passed resource is a valid schematron instance.
   *
   * @param aRes
   *        The resource to check. May be <code>null</code>.
   * @return <code>true</code> if the schematron is valid, <code>false</code>
   *         otherwise.
   */
  public static boolean isValidSchematron (@Nullable final IReadableResource aRes)
  {
    if (aRes == null)
      return false;

    return isValidSchematron (TransformSourceFactory.create (aRes));
  }

  /**
   * Check if the passed source is a valid schematron instance.
   *
   * @param aSource
   *        The source to check. May be <code>null</code>.
   * @return <code>true</code> if the schematron is valid, <code>false</code>
   *         otherwise.
   */
  public static boolean isValidSchematron (@Nullable final Source aSource)
  {
    if (aSource == null)
      return false;

    try
    {
      // Get a validator from the RNC schema.
      final Validator aValidator = RelaxNGCompactSchemaCache.getInstance ().getValidator (CSchematron.SCHEMATRON_RNC);

      // Ensure a collecting error handler is set
      final ErrorHandler aOldEH = aValidator.getErrorHandler ();
      CollectingSAXErrorHandler aCEH;
      if (aOldEH instanceof CollectingSAXErrorHandler)
        aCEH = (CollectingSAXErrorHandler) aOldEH;
      else
      {
        aCEH = new CollectingSAXErrorHandler (aOldEH);
        aValidator.setErrorHandler (aCEH);
      }

      // Perform validation
      aValidator.validate (aSource, null);

      // Check results
      return aCEH.getResourceErrors ().getMostSevereErrorLevel ().isLessSevereThan (EErrorLevel.ERROR);
    }
    catch (final SAXException ex)
    {
      // Document is not valid in respect to XML
    }
    catch (final IOException ex)
    {
      s_aLogger.warn ("Failed to read source " + aSource, ex);
    }
    return false;
  }
}
