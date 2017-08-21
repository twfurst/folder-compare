package com.flatironsjouve.furst.utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CheckRefAgainstFolder {
	//*****XPath setup*****
	private XPathFactory XPF;
	private XPath XP;
	//*****XML Setup*****
	private DocumentBuilderFactory DBF;
	private DocumentBuilder DB;
	//*****XPath statements*****
	private final String dmRefXP = "//dmRef";
	private final String dmCodeXp = "dmRefIdent/dmCode";
	//*****ArrayList to store info*****
	private ArrayList<String> filesAsDm = new ArrayList<String>();
	private ArrayList<String> missingDmRef = new ArrayList<String>();
	//*****Map to store unique dmRef*****
	private Map<String, String> uniqueRefs = new HashMap<String, String>();
	
	public CheckRefAgainstFolder()
	{
		initDb();
		initXp();
		Scanner input = new Scanner(System.in);
		System.out.println("Enter path to folder: ");
		String inputPath = input.nextLine();
		input.close();
		File[] pathFile = new File(inputPath).listFiles();
		
		processDirContents(pathFile);
	}
	
	public CheckRefAgainstFolder(String path)
	{
		initDb();
		initXp();
		File[] pathFile = new File(path).listFiles();
		
		processDirContents(pathFile);
	}
	
	private void initDb()
	{
		DBF = DocumentBuilderFactory.newInstance();
		try 
		{
			DB = DBF.newDocumentBuilder();
		} 
		catch (ParserConfigurationException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initXp()
	{
		XPF = XPathFactory.newInstance();
		XP = XPF.newXPath();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if(args.length == 0)
		{
			new CheckRefAgainstFolder();
		}
		else if(args.length == 1)
		{
			new CheckRefAgainstFolder(args[0]);
		}
		else
		{
			System.out.print("Too many arguments supplied. Either supply one folder path, or none.");
		}
	}
	
	private void processDirContents(File[] folder)
	{
		for(File f: folder)
		{
			if(f.getName().endsWith(".xml") || f.getName().endsWith(".XML"))
			{
				System.out.println("Processing " + f.getName() + "...");
				filesAsDm.add(f.getName().substring(0, f.getName().lastIndexOf(".")));
				processXmlFile(f);
			}
		}
		System.out.println("There are " + uniqueRefs.size() + " unique data module references in all files");
		findMissingMods();
	}
	
	private void processXmlFile(File xml)
	{
		try 
		{
			Document doc = DB.parse(xml);
			NodeList allDmRef = (NodeList)XP.compile(dmRefXP).evaluate(doc, XPathConstants.NODESET);
			System.out.println("\t"+ xml.getName() + " has " + allDmRef.getLength() + " dmRefs");
			for(int i = 0; i < allDmRef.getLength(); i++)
			{
				Node dmref = allDmRef.item(i);
				//System.out.println(dmref.getFirstChild().getNodeName());
				Node dmcode = (Node)XP.compile(dmCodeXp).evaluate(dmref, XPathConstants.NODE);
				NamedNodeMap atts = dmcode.getAttributes();
				
				/* Example dmCode:
				 * 
				 * <dmCode assyCode="00" disassyCode="01" disassyCodeVariant="A" infoCode="012"
                            infoCodeVariant="A" itemLocationCode="D" modelIdentCode="FSIAERO"
                            subSubSystemCode="0" subSystemCode="0" systemCode="00"
                            systemDiffCode="A"/>
				 */
				
				String modelic = atts.getNamedItem("modelIdentCode").getNodeValue();
				String sysdiff = atts.getNamedItem("systemDiffCode").getNodeValue();
				String system = atts.getNamedItem("systemCode").getNodeValue();
				String subsys = atts.getNamedItem("subSystemCode").getNodeValue();
				String subsubsys = atts.getNamedItem("subSubSystemCode").getNodeValue();
				String assy = atts.getNamedItem("assyCode").getNodeValue();
				String disassy = atts.getNamedItem("disassyCode").getNodeValue();
				String disassyv = atts.getNamedItem("disassyCodeVariant").getNodeValue();
				String info = atts.getNamedItem("infoCode").getNodeValue();
				String infov = atts.getNamedItem("infoCodeVariant").getNodeValue();
				String item = atts.getNamedItem("itemLocationCode").getNodeValue();
				
				String dmc = "DMC-" + modelic + "-" + sysdiff + "-" + system + "-" + subsys + subsubsys + "-"
						+ assy + "-" + disassy + disassyv + "-" + info + infov + "-" + item;
				System.out.println("\tAdding " + dmc + " to unique list");
				uniqueRefs.put(dmc, dmc);
				
			}
		} 
		catch (SAXException | IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void findMissingMods()
	{
		for(String dmc : uniqueRefs.keySet())
		{
			if(!filesAsDm.contains(dmc))
			{
				missingDmRef.add(dmc);
			}
		}
		
		if(missingDmRef.size() > 0)
		{
			System.out.println("Missing " + missingDmRef.size() + " XML files:");
			for(String d : missingDmRef)
			{
				System.out.println("\t" + d);
			}
		}
		else
		{
			System.out.println("All dmRefs accounted for");
		}
	}

}
