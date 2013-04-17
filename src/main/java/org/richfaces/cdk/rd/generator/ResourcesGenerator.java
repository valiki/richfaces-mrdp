/**
 * License Agreement.
 *
 * Rich Faces - Natural Ajax for Java Server Faces (JSF)
 *
 * Copyright (C) 2007 Exadel, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.richfaces.cdk.rd.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import sun.misc.URLClassPath;

/**
 * @author Anton Belevich
 *
 */
public class ResourcesGenerator {
	
	private File assemblyFile;
	
	private Log log;
	
	private Collection<String> resources;
	
	private List <String> includesBefore; 
	
	private List <String> includesAfter;
	
	private ResourceAssembler assembler; 
	
	public ResourcesGenerator(Log log) {
		this.log = log;
	}

	public void doAssembly() {
		if(resources != null) {
			if(includesBefore != null && !includesBefore.isEmpty()) {
				iterate(includesBefore);
			}
			
			if(resources != null && !resources.isEmpty()) {
				iterate(resources);
			}
			
			if(includesAfter != null && !includesAfter.isEmpty()) {
				iterate(includesAfter);
			}
		}
	}
	
	private void iterate(Collection<String> resources) {
		for (String resourceName: resources) {
			
			URL resource = getResourceURL(resourceName);
			log.info("concatenate resource: " + resource);
			if(resource != null) {
				if (assembler != null) {
					assembler.assembly(resource);
				}	
			}
			
		}
	}
		
	private URL getResourceURL(String resourceName) {
	    
		ClassLoader classLoader =  Thread.currentThread().getContextClassLoader();
		URL resource = classLoader.getResource(resourceName);
		
		try {
			if(resource == null) {
				//resolve framework script path
				Class clazz = classLoader.loadClass(resourceName);
				Object obj = clazz.newInstance();
				Method method = clazz.getMethod("getPath", new Class [0]);
				String path = (String) method.invoke(obj, new Object[0]);
				System.out.println("--- concatenating resource from component ---");
				resource = classLoader.getResource(path);
			} 
		} catch (Exception e) {
			log.error("Error process: " + resourceName + "\n" + e.getMessage(), e);
		}
		
		return resource;
	}

    private URL getResourceFromClassLoader(String resourceName, ClassLoader classLoader) {
        URL jarUrl = null;
        URL fileUrl = null;
        //this is terrible hack to load resources from system path by first priority and only than files from jars
        ClassRealm loader = (ClassRealm)classLoader;
        try {
            final Field field = URLClassLoader.class.getDeclaredField("ucp");
            field.setAccessible(true);
            final URLClassPath ucp = (URLClassPath) field.get(loader);
            final Field loadersField = URLClassPath.class.getDeclaredField("loaders");
            loadersField.setAccessible(true);
            final List loaders = (List) loadersField.get(ucp);
            for(int i=0;i<loaders.size();i++){
                Object l = loaders.get(i);
                try {
                    final Method method = l.getClass().getDeclaredMethod("findResource",String.class,boolean.class);
                    method.setAccessible(true);
                    URL res = (URL) method.invoke(l, resourceName,false);
                    if ( res!=null ) {
                        if(res.toString().startsWith("jar")){
                            jarUrl = res;
                        }
                        if(res.toString().startsWith("file")){
                            fileUrl = res;
                            break;
                        }
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        URL resource = null;
        if(fileUrl!=null){
            resource = fileUrl;
        }else
        if(jarUrl!=null){
            resource = jarUrl;
        }else{
            resource = classLoader.getResource(resourceName);
        }
        return resource;
    }
	
	public void writeToFile() {
		if(assemblyFile != null) {
			
			if(assemblyFile.exists()) {
				assemblyFile.delete();
			}
			
			try {
				assemblyFile.createNewFile(); 
				
			} catch (IOException e) {
				log.error("Error create assembly File: " + assemblyFile.getPath(),e);
			}
			
			assembler.writeToFile(assemblyFile);
		}
	}

	public File getAssemblyFile() {
		return assemblyFile;
	}

	public void setAssemblyFile(File assemblyFile) {
		this.assemblyFile = assemblyFile;
	}

	public Collection<String> getResources() {
		return resources;
	}

	public void setResources(Collection<String> resources) {
		this.resources = resources;
	}

	public List<String> getIncludesBefore() {
		return includesBefore;
	}

	public void setIncludesBefore(List<String> includesBefore) {
		this.includesBefore = includesBefore;
	}

	public List<String> getIncludesAfter() {
		return includesAfter;
	}

	public void setIncludesAfter(List<String> includesAfter) {
		this.includesAfter = includesAfter;
	}

	public ResourceAssembler getAssembler() {
		return assembler;
	}

	public void setAssembler(ResourceAssembler assembler) {
		this.assembler = assembler;
	}
}
