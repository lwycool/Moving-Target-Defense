package edu.csu.cs.mdt.ResourceServer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.xacml3.Advice;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Hello world!
 *
 */
public class PDPService
{
	private static Balana balana;
	
	private static Map<String,String> userRole = new HashMap<String, String>();
	
	private static PDP pdp = null;
	
    public void init()
    {
    	initBalana();
    	//while(ProtocolStop);
    }
    
    private static void initBalana(){

        try{
            // using file based policy repository. so set the policy location as system property
            String policyLocation = (new File(".")).getCanonicalPath() + File.separator + "policystore";
            System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY, policyLocation);
        } catch (IOException e) {
            System.err.println("Can not locate policy repository");
        }
        // create default instance of Balana
        balana = Balana.getInstance();
        initUserRoles();
        pdp = getPDPNewInstance();
    }

    /**
     * Returns a new PDP instance with new XACML policies
     *
     * @return a  PDP instance
     */
    private static PDP getPDPNewInstance(){

        PDPConfig pdpConfig = balana.getPdpConfig();
        return new PDP(pdpConfig);
    }
    
    private static void initUserRoles()
    {
    	userRole.put("sanjay", "faculty");
    	userRole.put("indrajit","faculty");
    	userRole.put("andres", "graduate");
    	userRole.put("dieudo", "graduate");
    	userRole.put("daniel", "graduate");
    	userRole.put("tom", "undergrad");
    	userRole.put("alice", "undergrad");
    	userRole.put("bob", "undergrad");
    	userRole.put("sharon", "undergrad");
    	userRole.put("mike", "undergrad");
    }
    
    private static String getUserRole(String userName)
    {
    	return userRole.get(userName);
    }
    
    public static String createXACMLRequest(String userName, String resource, String operation){

        return  "<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"true\">\n"+
        		"<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">\n"+
                "<Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" IncludeInResult=\"false\">\n"+
                "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"+resource+"</AttributeValue>\n"+
                "</Attribute>\n"+
                "</Attributes>\n"+
                "<Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">\n"+
                "<Attribute AttributeId=\"http://wso2.org/claims/role\" IncludeInResult=\"false\">\n"+
                "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"+getUserRole(userName)+"</AttributeValue>\n"+
                "</Attribute>\n"+
                "</Attributes>\n"+
                "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">\n"+
                "<Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" IncludeInResult=\"false\">\n"+
                "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"+operation+"</AttributeValue>\n"+
                "</Attribute>\n"+
                "</Attributes>\n"+
                "</Request>";

    }
    public boolean requestAccess(String userName, String resource, String operation)
    {
    	String request = createXACMLRequest(userName,resource,operation);
        //String request = createXACMLRequest("bob", "Food", 2, 40);
    	boolean res = false;
        String response = pdp.evaluate(request);
        String result = response.substring(response.indexOf("<Decision>", 0)+10, response.indexOf("</Decision>", 0));
        if(result.equalsIgnoreCase("NotApplicable")|| result.equalsIgnoreCase("Deny"))
        {
        	res = false;
        }
        else
        {
        	res = true;
        }
        return res;	
    }
}

