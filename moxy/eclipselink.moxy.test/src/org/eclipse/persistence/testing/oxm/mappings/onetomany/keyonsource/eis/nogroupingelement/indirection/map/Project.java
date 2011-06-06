/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.onetomany.keyonsource.eis.nogroupingelement.indirection.map;

public class Project {

	public static String PRIORITY_1 ="one";
	public static String PRIORITY_2 ="two";
	public static String PRIORITY_3 ="three";

	private long id;
	private String name;
	private String type;
	private String description;
    	
    public Project()
    {
	    this.name = "";
      this.type = "";
	    this.description = "";
    }
    public String getDescription(){
	    return description;
    }
    
    public String getType(){
	    return type;
    }
   
    public long getId()
    {
	    return id;
    }
    public String getName()
    {
	    return name;
    }

    public void setDescription(String description)
    {
	    this.description = description;
    }
    
    public void setType(String type)
    {
	    this.type = type;
    }
    
    public void setName(String name)
    {
	    this.name = name;
    }
    public void setId(long id)
    {
	    this.id = id;
    }
    
    public String toString()
    {
      return "Project: " + this.hashCode() + " id:"+ this.getId()+" " +this.getName() + " " +this.getType() + " "+ this.getDescription() + " ";
    } 
    
    
    public boolean equals(Object object)
    {
     if(!(object instanceof Project))
      return false;
      
     Project projectObject = (Project)object;
     if( (this.getId() == projectObject.getId()) &&
         (this.getDescription().equals(projectObject.getDescription())) &&
         (this.getName().equals(projectObject.getName())) &&
         (this.getType().equals(projectObject.getType())) ) 
          return true;
          
      return false;
      
   }
   
}
