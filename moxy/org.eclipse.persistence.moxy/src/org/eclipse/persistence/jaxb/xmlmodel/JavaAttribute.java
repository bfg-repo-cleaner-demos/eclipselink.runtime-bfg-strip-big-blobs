//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-661 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.06.08 at 10:23:42 AM EDT 
//


package org.eclipse.persistence.jaxb.xmlmodel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for java-attribute complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="java-attribute">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="java-attribute" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "java-attribute")
@XmlSeeAlso({
    XmlValue.class,
    XmlAnyElement.class,
    XmlElementRefs.class,
    org.eclipse.persistence.jaxb.xmlmodel.XmlAttribute.class,
    XmlAnyAttribute.class,
    XmlElements.class,
    XmlElementRef.class,
    XmlTransient.class,
    XmlElement.class
})
public abstract class JavaAttribute {

    @javax.xml.bind.annotation.XmlAttribute(name = "java-attribute")
    protected String javaAttribute;

    /**
     * Gets the value of the javaAttribute property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJavaAttribute() {
        return javaAttribute;
    }

    /**
     * Sets the value of the javaAttribute property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJavaAttribute(String value) {
        this.javaAttribute = value;
    }

}
