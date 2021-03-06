package net.techreadiness.plugin.action.org.survey;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import net.techreadiness.annotation.CoreSecured;
import net.techreadiness.navigation.taskflow.org.OrgTaskFlowData;
import net.techreadiness.navigation.taskflow.org.OrgTaskFlowDefinition;
import net.techreadiness.plugin.action.org.ContactByReadyOrgItemProvider;
import net.techreadiness.plugin.action.org.ReadyOrgItemProvider;
import net.techreadiness.plugin.action.org.ReadyOrgPartItemProvider;
import net.techreadiness.security.CorePermissionCodes;
import net.techreadiness.service.ConfigService;
import net.techreadiness.service.OrganizationService;
import net.techreadiness.service.ScopeService;
import net.techreadiness.service.common.ViewDef;
import net.techreadiness.service.exception.ServiceException;
import net.techreadiness.service.object.Contact;
import net.techreadiness.service.object.Org;
import net.techreadiness.service.object.OrgPart;
import net.techreadiness.service.object.Scope;
import net.techreadiness.ui.BaseAction;
import net.techreadiness.ui.tags.datagrid.DataGridState;
import net.techreadiness.ui.task.Task;
import net.techreadiness.ui.task.TaskFlowState;
import net.techreadiness.ui.util.ConversationScoped;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;

import com.google.common.collect.Lists;

public class DetailsAction extends BaseAction {

	private static final long serialVersionUID = 1L;

	private Long orgId;
	private Map<String, String> org;

	private boolean showTaskLinks;
	private boolean reset;

	private ViewDef viewDef;
	private ViewDef contactsViewDef;
	private ViewDef orgPartViewDef;

	@Inject
	private ConfigService configService;

	@Inject
	private OrganizationService orgService;

	@Inject
	private ScopeService scopeService;

	@Inject
	ContactByReadyOrgItemProvider contactByReadyOrgItemProvider;

	@Inject
	ReadyOrgItemProvider readyOrgItemProvider;

	@Inject
	ReadyOrgPartItemProvider readyOrgPartItemProvider;

	@ConversationScoped
	private DataGridState<Contact> contactDataGridState;

	@ConversationScoped
	private DataGridState<OrgPart> orgPartDataGridState;

	private Scope orgPartScope;

	@Inject
	private OrgTaskFlowDefinition orgTaskFlow;
	@Inject
	private OrgTaskFlowData orgTaskFlowData;

	@Override
	@CoreSecured({ CorePermissionCodes.READY_CUSTOMER_SURVEY_ACCESS })
	@Action(value = "details", results = { @Result(name = "success", location = "/net/techreadiness/plugin/action/readyOrg/survey/details.jsp") })
	public String execute() throws ServiceException {
		if (org == null) {
			Org o = orgService.getById(getServiceContext(), getOrgId());
			org = o.getAsMap();
			org.put("parentOrgName", o.getParentOrgName());
		}
		if (reset) {
			contactDataGridState.getFilters().clear();
			orgPartDataGridState.getFilters().clear();
		}

		orgPartScope = scopeService.getScopeWithOrgParts(getServiceContext());

		contactByReadyOrgItemProvider.setOrgId(getOrgId());

		readyOrgPartItemProvider.setOrgId(getOrgId());
		readyOrgPartItemProvider.setScopeId(getServiceContext().getScopeId());

		return SUCCESS;
	}

	@Action(value = "detailsEditOrg", results = { @Result(name = "success", location = "/task/org/altOrgTaskFlowBegin") })
	public String detailsEditOrg() throws ServiceException {
		return routeToTask(OrgTaskFlowDefinition.TASK_UPDATE_ORG);
	}

	@Action(value = "detailsEditContact", results = { @Result(name = "success", type = "redirect", location = "/task/org/altOrgTaskFlowBegin") })
	public String detailsEditContact() throws ServiceException {
		return routeToTask(OrgTaskFlowDefinition.TASK_EDIT_CONTACTS);
	}

	@Action(value = "detailsAddContact", results = { @Result(name = "success", type = "redirect", location = "/task/org/altOrgTaskFlowBegin") })
	public String detailsAddContact() throws ServiceException {
		return routeToTask(OrgTaskFlowDefinition.TASK_EDIT_CONTACTS);
	}

	@Action(value = "detailsEditParticipation", results = { @Result(name = "success", type = "redirect", location = "/task/org/altOrgTaskFlowBegin") })
	public String detailsEditParticipations() throws ServiceException {
		return routeToTask(OrgTaskFlowDefinition.TASK_PARICIPATIONS);
	}

	@Action(value = "detailsAddParticipation", results = { @Result(name = "success", type = "redirect", location = "/task/org/altOrgTaskFlowBegin") })
	public String detailsAddParticipations() throws ServiceException {
		return routeToTask(OrgTaskFlowDefinition.TASK_PARICIPATIONS);
	}

	private String routeToTask(String taskName) {
		Set<Org> orgs = new HashSet<>();
		orgs.add(orgService.getById(getServiceContext(), getOrgId()));
		Task task = orgTaskFlow.getTask(taskName);
		TaskFlowState state = new TaskFlowState();
		state.setTasks(Lists.newArrayList(task));
		orgTaskFlowData.setTaskFlowState(state);
		orgTaskFlowData.setOrgs(orgs);

		return SUCCESS;
	}

	public Long getOrgId() {
		return orgId;
	}

	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}

	public Map<String, String> getOrg() {
		return org;
	}

	public void setOrg(Map<String, String> org) {
		this.org = org;
	}

	public boolean isShowTaskLinks() {
		return showTaskLinks;
	}

	public void setShowTaskLinks(boolean showTaskLinks) {
		this.showTaskLinks = showTaskLinks;
	}

	public boolean isReset() {
		return reset;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}

	public ViewDef getViewDef() {
		return viewDef;
	}

	public void setViewDef(ViewDef viewDef) {
		this.viewDef = viewDef;
	}

	public ViewDef getContactsViewDef() {
		return contactsViewDef;
	}

	public void setContactsViewDef(ViewDef contactsViewDef) {
		this.contactsViewDef = contactsViewDef;
	}

	public ViewDef getOrgPartViewDef() {
		return orgPartViewDef;
	}

	public void setOrgPartViewDef(ViewDef orgPartViewDef) {
		this.orgPartViewDef = orgPartViewDef;
	}

	public ConfigService getConfigService() {
		return configService;
	}

	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}

	public OrganizationService getOrgService() {
		return orgService;
	}

	public void setOrgService(OrganizationService orgService) {
		this.orgService = orgService;
	}

	public ScopeService getScopeService() {
		return scopeService;
	}

	public void setScopeService(ScopeService scopeService) {
		this.scopeService = scopeService;
	}

	public ContactByReadyOrgItemProvider getContactByReadyOrgItemProvider() {
		return contactByReadyOrgItemProvider;
	}

	public ReadyOrgPartItemProvider getReadyOrgPartItemProvider() {
		return readyOrgPartItemProvider;
	}

	public void setOrgPartDataGridState(DataGridState<OrgPart> orgPartDataGridState) {
		this.orgPartDataGridState = orgPartDataGridState;
	}

	public Scope getOrgPartScope() {
		return orgPartScope;
	}
}
