/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Denise Smith  June 05, 2009 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.listofobjects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ListofObjects {
	public List<Employee> empList;
	public TreeSet<Employee> empTreeSet;
	public ArrayList<Employee> empArrayList;
	public List<Integer> integerList;
	public Employee[] empArray;
	public Integer[] integerArray;
	public boolean[] booleanArray;

	public ListofObjects() {
		empList = new ArrayList<Employee>();
		empArrayList = new ArrayList<Employee>();
		empTreeSet = new TreeSet();
	}

	public List<Employee> getEmpList() {
		return empList;
	}

	public void setEmpList(List<Employee> empList) {
		this.empList = empList;
	}

	public List<Integer> getIntegerList() {
		return integerList;
	}

	public void setIntegerList(List<Integer> integerList) {
		this.integerList = integerList;
	}

	public Employee[] getEmpArray() {
		return empArray;
	}

	public void setEmpArray(Employee[] empArray) {
		this.empArray = empArray;
	}

	public Integer[] getIntegerArray() {
		return integerArray;
	}

	public void setIntegerArray(Integer[] integerArray) {
		this.integerArray = integerArray;
	}

	public boolean[] getBooleanArray() {
		return booleanArray;
	}

	public void setBooleanArray(boolean[] booleanArray) {
		this.booleanArray = booleanArray;
	}

	public ArrayList<Employee> getEmpArrayList() {
		return empArrayList;
	}

	public void setEmpArrayList(ArrayList<Employee> empArrayList) {
		this.empArrayList = empArrayList;
	}

	public boolean equals(Object compareObject) {
		if (!(compareObject instanceof ListofObjects)) {
			return false;
		}
		ListofObjects compareListofObjects = (ListofObjects) compareObject;

		if (empList == null && compareListofObjects.getEmpList() != null) {
			return false;
		}
		if (empList.size() == compareListofObjects.getEmpList().size()) {
			if (!empList.containsAll(compareListofObjects.getEmpList())) {
				return false;
			}
		} else {
			return false;
		}

		if (empArrayList == null
				&& compareListofObjects.getEmpArrayList() != null) {
			return false;
		}
		if (empArrayList.size() == compareListofObjects.getEmpArrayList()
				.size()) {
			if (!empArrayList.containsAll(compareListofObjects
					.getEmpArrayList())) {
				return false;
			}
		} else {
			return false;
		}

		if (empArray == null && compareListofObjects.getEmpArray() != null) {
			return false;
		}
		if (empArray.length == compareListofObjects.getEmpArray().length) {
			for (int i = 0; i < empArray.length; i++) {
				if (!empArray[i].equals(compareListofObjects.getEmpArray()[i])) {
					return false;
				}
			}
		} else {
			return false;
		}

		return true;
	}

	public Set<Employee> getEmpTreeSet() {
		return empTreeSet;
	}

	public void setEmpTreeSet(TreeSet<Employee> empSet) {
		this.empTreeSet = empSet;
	}
}
