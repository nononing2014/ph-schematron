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
package com.helger.schematron.xslt;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.xml.serialize.write.XMLWriter;
import com.helger.commons.xml.serialize.write.XMLWriterSettings;
import com.helger.commons.xml.transform.TransformSourceFactory;
import com.helger.commons.xml.transform.XMLTransformerFactory;
import com.helger.schematron.xslt.SCHTransformerCustomizer.EStep;

/**
 * The XSLT preprocessor used to convert a Schematron XML document into an XSLT
 * document. This implementation uses JAXP with Saxon to be used as the
 * respective parser.
 *
 * @author Philip Helger
 */
@NotThreadSafe
final class SchematronProviderXSLTFromSCH implements ISchematronXSLTBasedProvider
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SchematronProviderXSLTFromSCH.class);

  /**
   * The classpath directory where the Schematron 2 XSLT files reside.
   */
  private static final String SCHEMATRON_DIRECTORY_XSLT2 = "schematron/20100414-xslt2/";

  /**
   * The class path to first XSLT to be applied.
   */
  private static final String XSLT2_STEP1 = SCHEMATRON_DIRECTORY_XSLT2 + "iso_dsdl_include.xsl";

  /**
   * The class path to second XSLT to be applied.
   */
  private static final String XSLT2_STEP2 = SCHEMATRON_DIRECTORY_XSLT2 + "iso_abstract_expand.xsl";

  /**
   * The class path to third and last XSLT to be applied.
   */
  private static final String XSLT2_STEP3 = SCHEMATRON_DIRECTORY_XSLT2 + "iso_svrl_for_xslt2.xsl";

  /**
   * This flag is for debugging purposes only. Only used during development.
   */
  private static final boolean SAVE_INTERMEDIATE_FILES = false;

  private static Templates s_aStep1;
  private static Templates s_aStep2;
  private static Templates s_aStep3;

  private final IReadableResource m_aSchematronResource;
  private Document m_aSchematronXSLTDoc;
  private Templates m_aSchematronXSLTTemplates;

  /**
   * Constructor
   *
   * @param aSchematronResource
   *        SCH resource
   * @param aTransformerCustomizer
   *        The customizer for XSLT {@link Transformer} objects. May not be
   *        <code>null</code>.
   */
  public SchematronProviderXSLTFromSCH (@Nonnull final IReadableResource aSchematronResource,
                                        @Nonnull final SCHTransformerCustomizer aTransformerCustomizer)
  {
    m_aSchematronResource = ValueEnforcer.notNull (aSchematronResource, "SchematronResource");
    ValueEnforcer.notNull (aTransformerCustomizer, "TransformerCustomizer");

    try
    {
      // prepare all steps
      if (s_aStep1 == null)
        s_aStep1 = XMLTransformerFactory.newTemplates (new ClassPathResource (XSLT2_STEP1));
      if (s_aStep2 == null)
        s_aStep2 = XMLTransformerFactory.newTemplates (new ClassPathResource (XSLT2_STEP2));
      if (s_aStep3 == null)
        s_aStep3 = XMLTransformerFactory.newTemplates (new ClassPathResource (XSLT2_STEP3));

      // perform step 1 (Schematron -> ResultStep1)
      final DOMResult aResult1 = new DOMResult ();
      final Transformer aTransformer1 = s_aStep1.newTransformer ();
      aTransformerCustomizer.customize (EStep.SCH2XSLT_1, aTransformer1);
      aTransformer1.transform (TransformSourceFactory.create (aSchematronResource), aResult1);

      // perform step 2 (ResultStep1 -> ResultStep2)
      final DOMResult aResult2 = new DOMResult ();
      final Transformer aTransformer2 = s_aStep2.newTransformer ();
      aTransformerCustomizer.customize (EStep.SCH2XSLT_2, aTransformer2);
      aTransformer2.transform (TransformSourceFactory.create (aResult1.getNode ()), aResult2);

      if (SAVE_INTERMEDIATE_FILES)
      {
        final String sXML = XMLWriter.getXMLString (aResult2.getNode ());
        SimpleFileIO.writeFile (new File ("test-minified",
                                          FilenameHelper.getWithoutPath (aSchematronResource.getPath ()) +
                                              ".min-xslt.sch"),
                                sXML,
                                XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
      }

      // perform step 3 (ResultStep2 -> ResultStep3XSL)
      final DOMResult aResult3 = new DOMResult ();
      final Transformer aTransformer3 = s_aStep3.newTransformer ();
      aTransformerCustomizer.customize (EStep.SCH2XSLT_3, aTransformer3);
      aTransformer3.transform (TransformSourceFactory.create (aResult2.getNode ()), aResult3);

      // Save the underlying XSLT document....
      // Note: Saxon 6.5.5 does not allow to clone the document node!!!!
      m_aSchematronXSLTDoc = (Document) aResult3.getNode ();

      if (SAVE_INTERMEDIATE_FILES)
      {
        final String sXML = XMLWriter.getXMLString (m_aSchematronXSLTDoc);
        SimpleFileIO.writeFile (new File ("test-final", FilenameHelper.getWithoutPath (aSchematronResource.getPath ()) +
                                                        ".xslt"), sXML, XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
      }

      // compile result of step 3
      m_aSchematronXSLTTemplates = XMLTransformerFactory.newTemplates (TransformSourceFactory.create (m_aSchematronXSLTDoc));
    }
    catch (final Throwable t)
    {
      s_aLogger.error ("Schematron preprocessor error", t);
    }
  }

  @Nonnull
  public IReadableResource getSchematronResource ()
  {
    return m_aSchematronResource;
  }

  public boolean isValidSchematron ()
  {
    return m_aSchematronXSLTTemplates != null;
  }

  @Nullable
  public Document getXSLTDocument ()
  {
    return m_aSchematronXSLTDoc;
  }

  @Nullable
  public Transformer getXSLTTransformer () throws TransformerConfigurationException
  {
    return m_aSchematronXSLTTemplates == null ? null : m_aSchematronXSLTTemplates.newTransformer ();
  }
}
