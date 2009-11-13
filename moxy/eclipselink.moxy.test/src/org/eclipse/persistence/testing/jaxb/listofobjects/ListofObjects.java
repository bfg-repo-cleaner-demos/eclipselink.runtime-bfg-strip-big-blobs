/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlTransient;

public class ListofObjects {
	public List<Employee> empList;
	public TreeSet<Employee> empTreeSet;
	public ArrayList<Employee> empArrayList;
	public List<Integer> integerList;
	public Employee[] empArray;
	public Integer[] integerArray;
	public Float[] floatArray;
	public int[] intArray;
	public boolean[] booleanArray;
	public HashMap<String, Integer> stringIntegerHashMap;
	public LinkedList<Integer> integerLinkedList;
	public Hashtable<String, Employee> stringEmployeeHashtable;	
	public Stack<BigDecimal> bigDecimalStack;

	public ListofObjects() {
		empList = new ArrayList<Employee>();
		empArrayList = new ArrayList<Employee>();
		empTreeSet = new TreeSet();
		integerLinkedList = new LinkedList<Integer>();
		stringEmployeeHashtable = new Hashtable();
		bigDecimalStack = new Stack();
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
		
		if (integerLinkedList == null && compareListofObjects.getIntegerLinkedList() != null) {
			return false;
		}
		if (integerLinkedList.size() == compareListofObjects.getIntegerLinkedList().size()) {
			if (!integerLinkedList.containsAll(compareListofObjects.getIntegerLinkedList())) {
				return false;
			}
		} else {
			return false;
		}

		if (empTreeSet == null && compareListofObjects.getEmpTreeSet() != null) {
			return false;
		}
		if (empTreeSet.size() == compareListofObjects.getEmpTreeSet().size()) {
			if (!empTreeSet.containsAll(compareListofObjects.getEmpTreeSet())) {
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
		
		if(booleanArray == null && compareListofObjects.booleanArray !=null){
			return false;
		}
		if (booleanArray.length == compareListofObjects.booleanArray.length) {
			for (int i = 0; i < booleanArray.length; i++) {
				if (booleanArray[i] != compareListofObjects.booleanArray[i]) {
					return false;
				}
			}
		} else {
			return false;
		}
		
		if(intArray == null && compareListofObjects.intArray !=null){
			return false;
		}
		if (intArray.length == compareListofObjects.intArray.length) {
			for (int i = 0; i < intArray.length; i++) {
				if (intArray[i] != compareListofObjects.intArray[i]) {
					return false;
				}
			}
		} else {
			return false;
		}
		
		if(integerArray == null && compareListofObjects.integerArray !=null){
			return false;
		}
		if (integerArray.length == compareListofObjects.integerArray.length) {
			for (int i = 0; i < integerArray.length; i++) {
				if (!integerArray[i].equals(compareListofObjects.integerArray[i])) {
					return false;
				}
			}
		} else {
			return false;
		}

		if(floatArray == null && compareListofObjects.floatArray !=null){
			if(compareListofObjects.floatArray.length >0){
				return false;
			}
		}else if (floatArray.length == compareListofObjects.floatArray.length) {
			for (int i = 0; i < floatArray.length; i++) {
				if (!floatArray[i].equals(compareListofObjects.floatArray[i])) {
					return false;
				}
			}
		} else {
			return false;
		}
		
		if(stringIntegerHashMap.size() == compareListofObjects.stringIntegerHashMap.size()){
			if(!stringIntegerHashMap.entrySet().containsAll(compareListofObjects.stringIntegerHashMap.entrySet())){
				return false;
			}		
		}else{
			return false;
		}
		
		if(stringEmployeeHashtable.size() == compareListofObjects.stringEmployeeHashtable.size()){
			if(!stringEmployeeHashtable.entrySet().containsAll(compareListofObjects.stringEmployeeHashtable.entrySet())){
				return false;
			}		
		}else{
			return false;
		}
		
		if (bigDecimalStack == null && compareListofObjects.getBigDecimalStack() != null) {
			return false;
		}
		if (bigDecimalStack.size() == compareListofObjects.getBigDecimalStack().size()) {
			if (!bigDecimalStack.containsAll(compareListofObjects.getBigDecimalStack())) {
				return false;
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

	public HashMap<String, Integer> getStringIntegerHashMap() {
		return stringIntegerHashMap;
	}

	public void setStringIntegerHashMap(
			HashMap<String, Integer> stringIntegerHashMap) {
		this.stringIntegerHashMap = stringIntegerHashMap;
	}

	public LinkedList<Integer> getIntegerLinkedList() {
		return integerLinkedList;
	}

	public void setIntegerLinkedList(LinkedList<Integer> integerLinkedList) {
		this.integerLinkedList = integerLinkedList;
	}

	public Hashtable<String, Employee> getStringEmployeeHashtable() {
		return stringEmployeeHashtable;
	}

	public void setStringEmployeeHashtable(Hashtable<String, Employee> stringEmployeeHashtable) {
		this.stringEmployeeHashtable = stringEmployeeHashtable;
	}

	public Stack<BigDecimal> getBigDecimalStack() {
		return bigDecimalStack;
	}

	public void setBigDecimalStack(Stack<BigDecimal> bigDecimalStack) {
		this.bigDecimalStack = bigDecimalStack;
	}
}
