/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - June 29/2009 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.jaxb.xmlmodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-type"/>
 *         &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-root-element"/>
 *         &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-see-also"/>
 *         &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-java-type-adapter"/>
 *         &lt;element name="java-attributes">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}java-attribute" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="xml-transient" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="xml-customizer" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="xml-accessor-type" type="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-access-type" default="PUBLIC_MEMBER" />
 *       &lt;attribute name="xml-accessor-order" type="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}xml-access-order" default="UNDEFINED" />
 *       &lt;attribute name="xml-inline-binary-data" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
@javax.xml.bind.annotation.XmlType(name = "", propOrder = {

})
@javax.xml.bind.annotation.XmlRootElement(name = "java-type")
public class JavaType {

    @javax.xml.bind.annotation.XmlElement(name = "xml-type", required = true)
    protected org.eclipse.persistence.jaxb.xmlmodel.XmlType xmlType;
    @javax.xml.bind.annotation.XmlElement(name = "xml-root-element", required = true)
    protected org.eclipse.persistence.jaxb.xmlmodel.XmlRootElement xmlRootElement;
    @XmlList
    @javax.xml.bind.annotation.XmlElement(name = "xml-see-also", required = true)
    protected List<String> xmlSeeAlso;
    @javax.xml.bind.annotation.XmlElement(name = "xml-java-type-adapter", required = true)
    protected XmlJavaTypeAdapter xmlJavaTypeAdapter;
    @javax.xml.bind.annotation.XmlElement(name = "java-attributes", required = true)
    protected JavaType.JavaAttributes javaAttributes;
    @javax.xml.bind.annotation.XmlAttribute
    protected String name;
    @javax.xml.bind.annotation.XmlAttribute(name = "xml-transient")
    protected Boolean xmlTransient;
    @javax.xml.bind.annotation.XmlAttribute(name = "xml-customizer")
    protected String xmlCustomizer;
    @javax.xml.bind.annotation.XmlAttribute(name = "xml-accessor-type")
    protected org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType xmlAccessorType;
    @javax.xml.bind.annotation.XmlAttribute(name = "xml-accessor-order")
    protected XmlAccessOrder xmlAccessorOrder;
    @javax.xml.bind.annotation.XmlAttribute(name = "xml-inline-binary-data")
    protected Boolean xmlInlineBinaryData;

    /**
     * Gets the value of the xmlType property.
     * 
     * @return
     *     possible object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlType }
     *     
     */
    public org.eclipse.persistence.jaxb.xmlmodel.XmlType getXmlType() {
        return xmlType;
    }

    /**
     * Sets the value of the xmlType property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlType }
     *     
     */
    public void setXmlType(org.eclipse.persistence.jaxb.xmlmodel.XmlType value) {
        this.xmlType = value;
    }

    /**
     * Gets the value of the xmlRootElement property.
     * 
     * @return
     *     possible object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlRootElement }
     *     
     */
    public org.eclipse.persistence.jaxb.xmlmodel.XmlRootElement getXmlRootElement() {
        return xmlRootElement;
    }

    /**
     * Sets the value of the xmlRootElement property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlRootElement }
     *     
     */
    public void setXmlRootElement(org.eclipse.persistence.jaxb.xmlmodel.XmlRootElement value) {
        this.xmlRootElement = value;
    }

    /**
     * Gets the value of the xmlSeeAlso property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xmlSeeAlso property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXmlSeeAlso().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getXmlSeeAlso() {
        if (xmlSeeAlso == null) {
            xmlSeeAlso = new ArrayList<String>();
        }
        return this.xmlSeeAlso;
    }

    /**
     * Gets the value of the xmlJavaTypeAdapter property.
     * 
     * @return
     *     possible object is
     *     {@link XmlJavaTypeAdapter }
     *     
     */
    public XmlJavaTypeAdapter getXmlJavaTypeAdapter() {
        return xmlJavaTypeAdapter;
    }

    /**
     * Sets the value of the xmlJavaTypeAdapter property.
     * 
     * @param value
     *     allowed object is
     *     {@link XmlJavaTypeAdapter }
     *     
     */
    public void setXmlJavaTypeAdapter(XmlJavaTypeAdapter value) {
        this.xmlJavaTypeAdapter = value;
    }

    /**
     * Gets the value of the javaAttributes property.
     * 
     * @return
     *     possible object is
     *     {@link JavaType.JavaAttributes }
     *     
     */
    public JavaType.JavaAttributes getJavaAttributes() {
        return javaAttributes;
    }

    /**
     * Sets the value of the javaAttributes property.
     * 
     * @param value
     *     allowed object is
     *     {@link JavaType.JavaAttributes }
     *     
     */
    public void setJavaAttributes(JavaType.JavaAttributes value) {
        this.javaAttributes = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the xmlTransient property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isXmlTransient() {
        if (xmlTransient == null) {
            return false;
        } else {
            return xmlTransient;
        }
    }

    /**
     * Sets the value of the xmlTransient property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setXmlTransient(Boolean value) {
        this.xmlTransient = value;
    }
    
    /**
     * Indicates if xmlTransient has been set, i.e. is non-null.
     *  
     * @return true is xmlTransient is non-null, false otherwise
     */
    public boolean isSetXmlTransient() {
        return xmlTransient != null;
    }

    /**
     * Gets the value of the xmlCustomizer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXmlCustomizer() {
        return xmlCustomizer;
    }

    /**
     * Sets the value of the xmlCustomizer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXmlCustomizer(String value) {
        this.xmlCustomizer = value;
    }

    /**
     * Gets the value of the xmlAccessorType property.
     * 
     * @return
     *     possible object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType }
     *     
     */
    public org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType getXmlAccessorType() {
        if (xmlAccessorType == null) {
            return org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType.PUBLIC_MEMBER;
        } else {
            return xmlAccessorType;
        }
    }

    /**
     * Sets the value of the xmlAccessorType property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType }
     *     
     */
    public void setXmlAccessorType(org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType value) {
        this.xmlAccessorType = value;
    }

    /**
     * Indicates if xmlAccessorType has been set, i.e. is non-null.
     * 
     * @return true if xmlAccessorType is non-null, false otherwise
     */
    public boolean isSetXmlAccessorType() {
        return xmlAccessorType != null;
    }

    /**
     * Gets the value of the xmlAccessorOrder property.
     * 
     * @return
     *     possible object is
     *     {@link XmlAccessOrder }
     *     
     */
    public XmlAccessOrder getXmlAccessorOrder() {
        if (xmlAccessorOrder == null) {
            return XmlAccessOrder.UNDEFINED;
        } else {
            return xmlAccessorOrder;
        }
    }

    /**
     * Sets the value of the xmlAccessorOrder property.
     * 
     * @param value
     *     allowed object is
     *     {@link XmlAccessOrder }
     *     
     */
    public void setXmlAccessorOrder(XmlAccessOrder value) {
        this.xmlAccessorOrder = value;
    }
    
    /**
     * Indicates if xmlAccessorOrder has been set, i.e. is non-null.
     * 
     * @return true if xmlAccessorOrder is non-null, false otherwise
     */
    public boolean isSetXmlAccessorOrder() {
        return xmlAccessorOrder != null;
    }

    /**
     * Gets the value of the xmlInlineBinaryData property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isXmlInlineBinaryData() {
        if (xmlInlineBinaryData == null) {
            return false;
        } else {
            return xmlInlineBinaryData;
        }
    }

    /**
     * Sets the value of the xmlInlineBinaryData property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setXmlInlineBinaryData(Boolean value) {
        this.xmlInlineBinaryData = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element ref="{http://www.eclipse.org/eclipselink/xsds/persistence/oxm}java-attribute" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
    @javax.xml.bind.annotation.XmlType(name = "", propOrder = {
        "javaAttribute"
    })
    public static class JavaAttributes {

        @javax.xml.bind.annotation.XmlElementRef(name = "java-attribute", namespace = "http://www.eclipse.org/eclipselink/xsds/persistence/oxm", type = JAXBElement.class)
        protected List<JAXBElement<? extends JavaAttribute>> javaAttribute;

        /**
         * Gets the value of the javaAttribute property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the javaAttribute property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getJavaAttribute().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link JAXBElement }{@code <}{@link org.eclipse.persistence.jaxb.xmlmodel.XmlAttribute }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlValue }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlAnyElement }{@code >}
         * {@link JAXBElement }{@code <}{@link JavaAttribute }{@code >}
         * {@link JAXBElement }{@code <}{@link org.eclipse.persistence.jaxb.xmlmodel.XmlElementRef }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlElementRefs }{@code >}
         * {@link JAXBElement }{@code <}{@link org.eclipse.persistence.jaxb.xmlmodel.XmlElement }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlAnyAttribute }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlJavaTypeAdapter }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlElements }{@code >}
         * {@link JAXBElement }{@code <}{@link XmlTransient }{@code >}
         * 
         * 
         */
        public List<JAXBElement<? extends JavaAttribute>> getJavaAttribute() {
            if (javaAttribute == null) {
                javaAttribute = new ArrayList<JAXBElement<? extends JavaAttribute>>();
            }
            return this.javaAttribute;
        }

    }

}
