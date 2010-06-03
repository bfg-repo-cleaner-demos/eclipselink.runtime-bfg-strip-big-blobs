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
 *     Oracle - initial API and implementation from Oracle TopLink
 *     ailitchev - Bug 244124 in 2.1 - Add AttributeGroup for nesting and LoadGroup support  
 ******************************************************************************/
package org.eclipse.persistence.queries;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.queries.AttributeItem;

/**
 * A FetchGroup is a performance enhancement that allows a group of attributes
 * of an object to be loaded on demand, which means that the data for an
 * attribute might not loaded from the underlying data source until an explicit
 * access call for the attribute first occurs. It avoids loading all data of the
 * object's attributes, in which the user is interested in only a subset of
 * them. A great deal of caution and careful system use case analysis should be
 * use when using the fetch group feature, as the extra round-trip would well
 * offset the gain from the deferred loading in many cases.
 * <p>
 * FetchGroup usage is only possible when an entity class implements the
 * {@link FetchGroupTracker} interface so that the FetchGroup can be stored in
 * the entity. The entity must also use the provided check methods to ensure the
 * attributes are loaded prior to use. In general this support is enabled
 * through weaving of the entity classes. If an entity class does not implement
 * {@link FetchGroupTracker} no FetchGroup functionality will be supported and
 * attempted use of a FetchGroup in a query will not result in the expected
 * behavior.
 * <p>
 * FetchGroups are defined in 3 ways:
 * <ul>
 * <li>A {@link FetchGroupManager#getDefaultFetchGroup()} is created and stored
 * on the {@link FetchGroupManager} during metadata processing if any of the
 * basic ({@link DirectToFieldMapping}) are configured to be loaded directly.
 * <li>A named FetchGroup can be defined and added to the
 * {@link FetchGroupManager}. For JPA users this can be accomplished using
 * annotation (@FetchGroup) or in an eclipselink-orm.xml. For JPA and native
 * users named groups can be defined in code and added to the
 * {@link FetchGroupManager#addFetchGroup(FetchGroup)}. Adding named groups in
 * code is typically done in a {@link DescriptorCustomizer}and should be done
 * before the session is initialized at login. To use a named FetchGroup on a
 * query the native {@link ObjectLevelReadQuery#setFetchGroupName(String)} can
 * be used of for JPA users the {@link QueryHints#FETCH_GROUP_NAME} an be used.
 * <li>A dynamic FetchGroup can be created within the application and used on a
 * query. For native API usage this is done using
 * {@link ObjectLevelReadQuery#setFetchGroup(FetchGroup)} while JPA users
 * generally use the {@link QueryHints#FETCH_GROUP}.
 * </ul>
 * <p>
 * When a query is executed only one FetchGroup will be used. The order of
 * precedence is:
 * <ol>
 * <li>If a FetchGroup is specified on a query it will be used.
 * <li>If no FetchGroup is specified but a FetchGroup name is specified and the
 * FetchGroupManager has a group by this name it will be used.
 * <li>If neither a FetchGroup nor a FetchGroup name is specified on the query
 * an the FetchGroupManager has a default group then it will be used.
 * <li>If none of these conditions are met then no FetchGroup will be used when
 * executing a query. <br/>
 * <i>Note: This includes the execution of queries to populate lazy and eager
 * relationships.
 * <p>
 * <b>Loading:</b> A FetchGroup can optionally specify that it needs its
 * included relationships loaded. This can be done using
 * {@link #setShouldLoad(boolean)} and {@link #setShouldLoadAll(boolean)} as
 * well as the corresponding configurations in the @FetchGroup annotation and
 * the <fetch-group> element in the eclipselink-orm.xml. When this si configured
 * the FetchGroup will also function as a {@link LoadGroup} causing all of its
 * specified relationships to be populated prior to returning the results form
 * the query execution.
 * 
 * @see FetchGroupManager
 * @see QueryHints#FETCH_GROUP
 * @see LoadGroup
 * 
 * @author King Wang, dclarke, ailitchev
 * @since TopLink 10.1.3
 */
public class FetchGroup extends AttributeGroup {

    /**
     * Indicates whether this group should be also used as a {@link LoadGroup}
     * when processing the query result.
     */
    private boolean shouldLoad;
    
    public FetchGroup() {
        super();
    }

    public FetchGroup(String name) {
        super(name);
    }

    /**
     * Return the attribute names on the current FetchGroup. This does not
     * include the attributes on nested FetchGroups
     * 
     * @deprecated Use {@link AttributeGroup#getAttributeNames()}
     */
    @Deprecated
    public Set<String> getAttributes() {
        return getAttributeNames();
    }

    /**
     * INTERNAL:
     * Called on attempt to get value of an attribute that hasn't been fetched yet.
     * Returns an error message in case javax.persistence.EntityNotFoundException 
     * should be thrown by the calling method,
     * null otherwise.
     * <p>
     * This method is typically only invoked through woven code in the
     * persistence object introduced when {@link FetchGroupTracker} is woven
     * into the entity.
     */
    public String onUnfetchedAttribute(FetchGroupTracker entity, String attributeName) {
        ReadObjectQuery query = new ReadObjectQuery(entity);
        query.setShouldUseDefaultFetchGroup(false);
        Object result = entity._persistence_getSession().executeQuery(query);
        if (result == null) {
            Object[] args = { query.getSelectionId() };
            return ExceptionLocalization.buildMessage("no_entities_retrieved_for_get_reference", args);
        }
        return null;
    }

    /**
     * INTERNAL:
     * Called on attempt to assign value to an attribute that hasn't been fetched yet.
     * Returns an error message in case javax.persistence.EntityNotFoundException 
     * should be thrown by the calling method,
     * null otherwise.
     * <p>
     * This method is typically only invoked through woven code in the
     * persistence object introduced when {@link FetchGroupTracker} is woven
     * into the entity.
     */
    public String onUnfetchedAttributeForSet(FetchGroupTracker entity, String attributeName) {
        return onUnfetchedAttribute(entity, attributeName);
    }

    /**
     * Configure this group to also act as a {@link LoadGroup} when set to true
     * and load all of the specified relationships so that the entities returned
     * from the query where this group was used have the requested relationships
     * populated. All subsequent attributes added to this group that create a
     * nested group will have this value applied to them.
     * 
     * @see #setShouldLoadAll(boolean) to configure {@link #shouldLoad()} on
     *      nested groups
     */
    public void setShouldLoad(boolean shouldLoad) {
        this.shouldLoad = shouldLoad;
    }
    
    /**
     * Configure this group to also act as a {@link LoadGroup} the same as
     * {@link #setShouldLoad(boolean)}. Additionally this method will apply the
     * provided boolean value to all nested groups already added.
     * 
     * @see #setShouldLoad(boolean) to only configure this grup without
     *      effecting existing nested groups.
     */
    public void setShouldLoadAll(boolean shouldLoad) {
        this.shouldLoad = shouldLoad;
        if(this.hasItems()) {
            Iterator<Map.Entry<String, AttributeItem>> it = getItems().entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, AttributeItem> entry = it.next();
                FetchGroup group = (FetchGroup)entry.getValue().getGroup();
                if(group != null) {
                    group.setShouldLoadAll(shouldLoad);
                }
            }
        }
    }
    
    /**
     * @return true if this group will be used as a {@link LoadGroup}when
     *         processing the results of a query to force the specified
     *         relationships to be loaded.
     */
    public boolean shouldLoad() {
        return this.shouldLoad;
    }
    
    @Override
    protected FetchGroup newGroup(String name, AttributeGroup parent) {
        FetchGroup fetchGroup = new FetchGroup(name);
        if(parent != null) {
            fetchGroup.setShouldLoad(((FetchGroup)parent).shouldLoad());
        }
        return fetchGroup;
    }
    
    @Override
    public boolean isFetchGroup() {
        return true;
    }

    public boolean isEntityFetchGroup() {
        return false;
    }

    /*
     * LoadGroup created with all member groups with shouldLoad set to false dropped.
     */
    public LoadGroup toLoadGroupLoadOnly() {
        if(!this.shouldLoad) {
            return null;
        }
        LoadGroup loadGroup = new LoadGroup(getName());
        if(this.hasItems()) {
            Iterator<Map.Entry<String, AttributeItem>> it = getItems().entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, AttributeItem> entry = it.next();
                FetchGroup group = (FetchGroup)entry.getValue().getGroup();
                if(group != null) {
                    loadGroup.addAttribute(entry.getKey(), group.toLoadGroupLoadOnly());
                } else {
                    loadGroup.addAttribute(entry.getKey());
                }
            }
        }
        if(!loadGroup.getItems().isEmpty()) {
            return loadGroup;
        } else {
            return null;
        }
    }
    
    @Override
    public FetchGroup clone() {
        return (FetchGroup)super.clone();
    }    

    /**
     * Returns FetchGroup corresponding to the passed (possibly nested) attribute.
     */
    @Override
    public FetchGroup getGroup(String attributeNameOrPath) {
        return (FetchGroup)super.getGroup(attributeNameOrPath);
    }

    @Override
    public AttributeItem addAttribute(String attributeNameOrPath, AttributeGroup group) {
        return super.addAttribute(attributeNameOrPath, (group != null ? group.toFetchGroup() : null));
    }

    public AttributeItem addAttribute(String attributeNameOrPath, FetchGroup group) {
        return super.addAttribute(attributeNameOrPath, group);
    }

}
