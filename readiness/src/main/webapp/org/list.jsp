<!DOCTYPE html>
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://techreadiness.net/components" prefix="ui"%>

<html>
<head>
<title><s:text name="organization.list.pageTitle" /></title>
</head>
<body>
	<ui:dataView taskFlow="${taskFlow}" filterSelectable="true" title="%{getText('tab.organizations.title')}">
		<ui:dataViewFilters viewDefType="ORG_DATAGRID">
			<c:if test="${showAllOrgsCheckboxDisplayed}">
				<ui:dataViewFilter name="%{getText('organization.list.searchAll')}" code="showAll" beanName="showAllOrganizationsSelectionHandler" type="checkbox" primary="true"/>
			</c:if>
			<ui:dataViewFilter code="parentOrg" name="%{getText('core.parent')} %{getText('core.organization')}" beanName="parentOrgSelectionFilterForOrgList" nameKey="name" valueKey="orgId"/>
			<ui:dataViewFilter code="orgType" name="%{getText('core.organization')} %{getText('core.type')}" beanName="orgTypeSelectionFilterHandlerForOrgList" valueKey="key" nameKey="value" multiple="true"/>
		</ui:dataViewFilters>
		
        <ui:shoppingCart name="%{getText('core.organizations')}">
            <s:property value="name"/>
        </ui:shoppingCart>

		<s:form>
			<ui:datagrid itemProvider="${orgItemProvider}" value="orgSearchGrid" var="org" viewDef="${dataGridView}" selectable="true" rowValue="orgId" columnSelectable="true">
				<ui:dataGridColumn code="name">
					<ui:flyout text="${org.name}" title="Org Details" namespace="/organization" action="details" orgId="${org.orgId}" reset="true" showTaskLinks="true"/>
				</ui:dataGridColumn>
				<ui:dataGridColumn code="orgType" name="%{getText('organization.list.orgTypeHeader')}" displayOrder="3">
					<s:property value="org.orgTypeName"/>
				</ui:dataGridColumn>
				<ui:dataGridColumn code="parentOrg" name="%{getText('core.parent')} %{getText('core.organization')}" displayOrder="4">
					<s:property value="org.parentOrgName"/>
				</ui:dataGridColumn>
			</ui:datagrid>
		</s:form>
	</ui:dataView>
</body>
</html>