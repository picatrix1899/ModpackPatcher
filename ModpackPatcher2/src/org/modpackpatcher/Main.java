package org.modpackpatcher;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Main
{

	public static void main(String[] args)
	{
		try
		{
			new Main(args);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String zipFileName = "packet.zip";
	private String scriptFileName = "script.json";
	
	public Main(String[] args) throws Exception
	{
		Map<String,List<String>> arguments = parseArguments(args);
		
		if(arguments.containsKey("packet"))
			this.zipFileName = arguments.get("packet").get(0);
		
		
		File f = new File(getCurrentLocation(), this.zipFileName);
		
		ZipFile zip = new ZipFile(f);
		
		File cache = new File(getCacheLocation());
		if(!cache.exists()) cache.mkdirs();
		
		if(zip.getEntry(this.scriptFileName) != null)
		{
			ZipEntry entry = zip.getEntry(this.scriptFileName);
			
			try(InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry)))
			{
				readScriptFile(reader, zip);
			}
		}
	}
	
	public static Map<String,List<String>> parseArguments(String[] args)
	{
		Map<String,List<String>> out = new HashMap<>();
		
		String current = "";
		
		if(args.length > 0)
		{
			for(String s : args)
			{
				if(s.startsWith("-"))
				{
					current = s.replaceFirst("-", "");
					out.put(current, new ArrayList<>());
				}
				else
				{
					if(out.containsKey(current))
					{
						out.get(current).add(s);
					}
				}
			}
		}
		
		return out;
	}
	
	private void readScriptFile(Reader r, ZipFile zip) throws Exception
	{
		JSONTokener tokener = new JSONTokener(r);
		JSONObject mainBlock = (JSONObject) tokener.nextValue();
		
		File cache = new File(getCacheLocation());
		
		processDownloads(mainBlock);
		
		if(mainBlock.has("extracts"))
		{
			JSONArray extracts = mainBlock.getJSONArray("extracts");
			
			for(Iterator<Object> it = extracts.iterator(); it.hasNext();)
			{
				JSONObject extract = (JSONObject)it.next();
				
				processExtract(extract, zip);
			}
			
			File config = new File(getCurrentLocation(), "config");
			
			for(Iterator<Object> it = extracts.iterator(); it.hasNext();)
			{
				JSONObject extract = (JSONObject)it.next();
				String expectedFile = extract.getString("output");
				
				File inF = new File(cache, expectedFile);
				File outF = new File(config, expectedFile);
				
				outF.mkdirs();
				Files.copy(inF.toPath(), outF.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		if(mainBlock.has("removes"))
		{
			JSONArray removes = mainBlock.getJSONArray("removes");
			
			for(Iterator<Object> it = removes.iterator(); it.hasNext();)
			{
				File f = new File(getCurrentLocation(), (String)it.next());
				f.delete();
			}
		}
	}
	
	private void processDownloads(JSONObject obj) throws Exception
	{
		if(obj.has("downloads"))
		{
			JSONArray inserts = obj.getJSONArray("downloads");
			
			List<DownloadEntry> downloads = new ArrayList<>();
			
			for(Iterator<Object> it = inserts.iterator(); it.hasNext();)
			{
				DownloadEntry e = processDownloadScript((JSONObject)it.next());
				if(e != null) downloads.add(e);
			}
			
			for(DownloadEntry e : downloads)
				processDownload(e);
				
		}
	}
	
	private DownloadEntry processDownloadScript(JSONObject obj) throws Exception
	{
		DownloadEntry out = new DownloadEntry();
		out.link = obj.getString("link");
		out.file = obj.getString("file");
		return out;
	}
	
	private void processDownload(DownloadEntry entry) throws Exception
	{
		openWebsite(entry.link);
		
		File file = new File(getDownloadLocation(), entry.file);
		
		System.out.print("Waiting for Download of File \"" + entry.file + "\" from \"" + entry.link +"\" ...");
		boolean success = testForFile(file, 60, 1000l);
		if(success)
		{
			System.out.println("Success!");
			System.out.print("Copying File \"" + entry.file + "\" from Downloads to Cache...");
			Files.copy(file.toPath(), new File(getCacheLocation(), entry.file).toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Finished!");
		}
		else
		{
			System.out.println("Failed!");
			System.exit(-1);
		}
	}
	
	private boolean testForFile(File file, int maxRetries, long wait) throws Exception
	{
		int retry = 0;
		
		while(!file.exists())
		{
			if(retry <= maxRetries)
			{
				retry++;
				Thread.sleep(wait);
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}
	
	private void test()
	{
		
		

		File result = new File(getCacheLocation(), expectedFile); 
		
		
		
		Files.copy(expected.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
	
	private void processExtract(JSONObject obj, ZipFile zip) throws Exception
	{
		File f = new File(getCacheLocation(), obj.getString("path"));
		f.mkdirs();
		f.delete();
		f.createNewFile();
		
		try(InputStream stream = zip.getInputStream(zip.getEntry(obj.getString("output"))))
		{
			byte[] buffer = new byte[stream.available()];
			
			stream.read(buffer);
			
			try(FileOutputStream outStream = new FileOutputStream(f))
			{
				outStream.write(buffer);
				outStream.flush();
			}
		}
	}
	
	private void openWebsite(String url) throws Exception { Desktop.getDesktop().browse(new URL(url).toURI()); }
	
	private String getDownloadLocation()
	{
		return System.getProperty("user.home") + "\\Downloads";
	}
	
	private String getCacheLocation()
	{
		return getCurrentLocation() + "\\patcher_cache";
	}
	
	private String getCurrentLocation()
	{
		return System.getProperty("user.dir");
	}
}
