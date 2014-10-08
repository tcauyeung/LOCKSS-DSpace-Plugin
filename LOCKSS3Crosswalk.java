package org.dspace.app.oai;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.util.MetadataExposure;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.IConverter;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.dspace.core.LogManager;
import org.apache.log4j.Logger;

import org.dspace.app.oai.OAIDCCrosswalk;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.w3c.dom.ls.*;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

public class LOCKSS3Crosswalk extends Crosswalk
{
	Properties crosswalkProps;
	
    /** Location of config file */
    private static final String configFilePath = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separator
            + "config"
            + File.separator
            + "crosswalks"
            + File.separator + "oaidc.properties";	
			
    /** log4j logger */
    private static Logger log = Logger.getLogger(OAIDCCrosswalk.class);			
	
	public LOCKSS3Crosswalk(Properties properties)
	{
        super("http://www.openarchives.org/OAI/2.0/oai_dc/ "
                + "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
				
		crosswalkProps = new Properties();
		        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(configFilePath);
            crosswalkProps.load(fis);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(
                    "Wrong configuration for OAI_DC", e);
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException ioe)
                {
                    log.error(ioe);
                }
            }
        }
	}
	
    public boolean isAvailableFor(Object nativeItem)
    {
        // We have DC for everything
        return true;
    }	
	
	public String createMetadata(Object nativeItem)
			throws CannotDisseminateFormatException
	{
		OAIDCCrosswalk oaidc_crosswalk = new OAIDCCrosswalk(crosswalkProps);
		String baseDoc = oaidc_crosswalk.createMetadata(nativeItem);
		
        Item item = ((HarvestedItemInfo) nativeItem).item;		
		
		try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    		InputSource is = new InputSource();
    		is.setCharacterStream(new StringReader(baseDoc));

    		Document doc = db.parse(is);
			
			NodeList nodes = doc.getElementsByTagName("dc:identifier");
			
			Text a = doc.createTextNode("LOCKSS system has permission to collect, preserve, and serve this Archival Unit"); 
			Element p = doc.createElement("dc:rights"); 
			p.appendChild(a); 

			nodes.item(0).getParentNode().appendChild(p);
			
			// Added to gather source links
        	int BundleIdx = 0;
        	try 
			{
            	Bundle[] Bundles = item.getBundles("ORIGINAL");
            
           		for (int bundleCount=0; bundleCount < Bundles.length; bundleCount++)
           		{
               		Bitstream[] Bitstreams = Bundles[bundleCount].getBitstreams();
               		for (int streamCount=0; streamCount < Bitstreams.length; streamCount++)
               		{
              	  		if(!Bitstreams[streamCount].getFormat().isInternal())
				  		{
	           	  			BundleIdx = Bitstreams[streamCount].getSequenceID();
						
							a = doc.createTextNode(ConfigurationManager.getProperty("dspace.url") + 
		                    					"bitstream/" + 
		                    					item.getHandle() + 
		                    					"/" + BundleIdx + 
		                    					"/" + Bitstreams[streamCount
												].getName().trim());
											
							p = doc.createElement("dc:source");
							p.appendChild(a);
						
							nodes.item(0).getParentNode().insertBefore(p, nodes.item(0));
                		}
                	}  
            	}
        	} catch (SQLException e) { ; } 		
			// End source link initial section
		
			return this.getStringFromDoc(doc);
		}
		catch(Exception e) { ; }
		
		return "No text to return";
	}
	
	public String getStringFromDoc(Document doc)
	{
    	DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
    	LSSerializer lsSerializer = domImplementation.createLSSerializer();
		
		// Strips the leading XML tag before returning text
    	return lsSerializer.writeToString(doc).substring(40);
	}
}