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
import java.util.Iterator;
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
			new Main();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String zipFileName = "packet.zip";
	private String scriptFileName = "script.json";
	
	public Main() throws Exception
	{
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
	
	private void readScriptFile(Reader r, ZipFile zip) throws Exception
	{
		JSONTokener tokener = new JSONTokener(r);
		JSONObject mainBlock = (JSONObject) tokener.nextValue();
		
		File cache = new File(getCacheLocation());
		
		if(mainBlock.has("inserts"))
		{
			JSONArray inserts = mainBlock.getJSONArray("inserts");
			
			for(Iterator<Object> it = inserts.iterator(); it.hasNext();)
			{
				JSONObject insert = (JSONObject)it.next();
				
				processInsertDownload(insert);
			}
			
			File mods = new File(getCurrentLocation(), "mods");
			
			for(Iterator<Object> it = inserts.iterator(); it.hasNext();)
			{
				JSONObject insert = (JSONObject)it.next();
				String expectedFile = insert.getString("expected-file");
				
				File inF = new File(cache, expectedFile);
				File outF = new File(mods, expectedFile);
				
				Files.copy(inF.toPath(), outF.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
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
	
	
	
	private void processInsertDownload(JSONObject obj) throws Exception
	{
		String link = obj.getString("link");
		String expectedFile = obj.getString("expected-file");
		
		File expected = new File(getDownloadLocation(), expectedFile);
		
		openWebsite(link);

		File result = new File(getCacheLocation(), expectedFile); 
		
		while(!expected.exists()) { Thread.sleep(1000l); }
		
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
	
	private void openWebsite(String url) throws Exception
	{
		URL website = new URL(url);
		
		Desktop desktop = Desktop.getDesktop();
		
		desktop.browse(website.toURI());
	}
	
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
