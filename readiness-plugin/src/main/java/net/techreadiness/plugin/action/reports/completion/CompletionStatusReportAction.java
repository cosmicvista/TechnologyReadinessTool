package net.techreadiness.plugin.action.reports.completion;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import net.techreadiness.annotation.CoreSecured;
import net.techreadiness.plugin.action.reports.GeoChartBean;
import net.techreadiness.plugin.action.reports.ReportAction;
import net.techreadiness.plugin.action.reports.ReportExport;
import net.techreadiness.plugin.action.reports.ReportExportCsv;
import net.techreadiness.plugin.action.reports.ReportExportPdf;
import net.techreadiness.plugin.action.reports.ReportItemProvider;
import net.techreadiness.plugin.action.reports.ReportItemProvider.ExportType;
import net.techreadiness.plugin.service.reports.ReportsService;
import net.techreadiness.security.CorePermissionCodes;
import net.techreadiness.service.ServiceContext;
import net.techreadiness.service.object.Org;
import net.techreadiness.service.object.OrgPart;
import net.techreadiness.service.object.Scope;
import net.techreadiness.ui.tags.datagrid.DataGridState;
import net.techreadiness.ui.util.ConversationScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CompletionStatusReportAction extends ReportAction {
	private static final long serialVersionUID = 1L;

	private String orgCode;
	private String stateCode;
	private String stateName;

	private GeoChartBean geoChart;
	private Map<String, String> orgSummary;
	private Org currentOrg;
	private boolean dualConsortium;

	@Inject
	private ReportsService reportsService;
	@Inject
	@Qualifier("CompletionStatusItemProvider")
	private ReportItemProvider reportItemProvider;

	@ConversationScoped(value = "completionDataGridState")
	private DataGridState<?> reportGrid;

	@CoreSecured({ CorePermissionCodes.READY_CUSTOMER_ASSESSMENT_COMPLETION_RPT })
	@Action(value = "completionStatus", results = { @Result(name = "completion", location = "/net/techreadiness/plugin/action/reports/completion/completion.jsp") })
	public String completionStatus() throws Exception {
		getData();
		buildBreadcrumbs(currentOrg, consortium, "completionStatus");
		return "completion";
	}

	private void getData() throws Exception {
		ServiceContext context = getServiceContext();

		// check if we are passed an org code
		// we should get this passed to us on every request outside the initial request (clicking on tab)
		if (orgCode == null || orgCode.equals("")) {
			Org org = context.getOrg();
			orgCode = org.getCode();
		}

		currentOrg = orgService.getByCode(context, orgCode);
		if (currentOrg == null || !userService.hasAccessToOrg(context, context.getUserId(), currentOrg.getOrgId())) {
			if (currentOrg == null) {
				throw new Exception("Org could not be found: " + orgCode);
			}
			throw new Exception("User(" + context.getUserId() + ") doesn't have access to org (" + currentOrg.getOrgId()
					+ ")");
		}

		// get previous org off the session, if different, reset paging
		Map<String, Object> session = getSession();
		if (session.get("prevOrgSubmission") == null || !currentOrg.getId().equals(session.get("prevOrgSubmission"))) {
			reportGrid.setPage(1);
		}
		session.put("prevOrgSubmission", currentOrg.getId());

		// check if user has access to dual consortium
		dualConsortium = userService.hasPermission(context, CorePermissionCodes.CORE_CUSTOMER_CHANGE_GLOBAL_SCOPE);
		// if true, check to make sure the current org has access to both consortiums
		List<OrgPart> orgParts = orgPartService.findOrgPartsForOrg(context, currentOrg.getOrgId());
		if (dualConsortium) {
			dualConsortium = orgParts != null && orgParts.size() > 1;
		}
		// if consortium is null, set consortium to the consortium in the service context
		if (consortium == null) {
			consortium = Consortium.getConsortium(context.getScope().getCode());
		}

		// get the consortiums scope object
		Scope consortiumScope = scopeService.getByScopePath(consortium.path);

		// set the appropriate data for the report item provider
		reportItemProvider.setConsortium(consortiumScope);
		reportItemProvider.setSnapshotWindow(getCurrentSnapshotWindow());
		reportItemProvider.setOrg(currentOrg);
		reportItemProvider.setMinimumRecommendedFlag(getMinimumRecommendedFlag());

		Collection<Map<String, String>> result = reportsService.retrieveSummaryForOrg(
				getCurrentSnapshotWindow().getSnapshotWindowId(), currentOrg.getOrgId(), getMinimumRecommendedFlag())
				.getRows();
		orgSummary = Iterables.getFirst(result, new HashMap<String, String>());

		if (currentOrg.getOrgTypeName().equals("Readiness")) {

			// query the map data
			Collection<Map<String, String>> states = reportsService.retrieveSummaryForChildOrgs(
					getCurrentSnapshotWindow().getSnapshotWindowId(), currentOrg.getOrgId(), getMinimumRecommendedFlag(),
					true, 0, 0).getRows();
			// put state data into geochart
			geoChart = new GeoChartBean();
			geoChart.setHoverLabel("States with Submitted Data Input");
			if (viewBy != null && viewBy.equals(ViewBy.table)) {
				geoChart.setWidth(285);
				geoChart.setHeight(175);
			}

			for (Map<String, String> data : states) {
				geoChart.addPercentData(data.get("orgCode"),
						StringUtils.remove(data.get("orgName"), "(" + data.get("localOrgCode") + ")"),
						data.get("localOrgCode"), data.get("percentComplete"));
			}

		} else if (currentOrg.getOrgTypeName().equals("State")) {

			// set state code and name (needed for image on page)
			stateCode = currentOrg.getLocalCode().toUpperCase();
			stateName = currentOrg.getName();
		} else if (currentOrg.getOrgTypeName().equals("District")) {
			Org state = orgService.getById(context, currentOrg.getParentOrgId());
			// set state code and name (needed for image on page)
			stateCode = state.getLocalCode().toUpperCase();
			stateName = state.getName();
		} else if (currentOrg.getOrgTypeName().equals("School")) {
			Org state = orgService.getParentOrgOfType(getServiceContext(), currentOrg.getOrgId(), "state");
			// set state code and name (needed for image on page)
			stateCode = state.getLocalCode().toUpperCase();
			stateName = state.getName();
		}
	}

	@Override
	protected ReportExport getReportExport(ExportType type) throws Exception {
		// populate reportItemProvider
		setFileName("CompletionStatus");
		getData();
		List<String> columnLabels, columnKeys;
		if (!currentOrg.getOrgTypeName().equals("District") && !currentOrg.getOrgTypeName().equals("School")) {
			columnLabels = Lists.newArrayList("Organization", "Org Code", "Completion Status");
			columnKeys = Lists.newArrayList("orgName", "orgCode", "percentComplete");
		} else {
			columnLabels = Lists.newArrayList("Organization", "Org Code", "Completion Status");
			columnKeys = Lists.newArrayList("orgName", "orgCode", "dataEntryComplete");
		}
		if (ExportType.csv.equals(type)) {
			columnLabels.add(0, "Parent Organization");
			columnKeys.add(0, "parentOrgName");
			if (currentOrg.getOrgTypeName().equals("District")) {
				columnLabels.add(1, "School Classification");
				columnKeys.add(1, "schoolType");
			}
		}

		switch (ExportType.valueOf(fileType)) {
		case csv: {
			ReportExportCsv csv = new ReportExportCsv();
			csv.setColumnLabels(columnLabels);
			csv.setColumnKeys(columnKeys);
			csv.setData(reportItemProvider.export(type));
			return csv;
		}
		case pdf: {
			StringBuilder subtitle = new StringBuilder();
			if (currentOrg.getOrgTypeName().equals("School")) {
				subtitle.append("<b>").append(orgSummary.get("orgName")).append(" - ");
				subtitle.append("Indicates the Data are");
				if (orgSummary.get("dataEntryComplete") != null && !orgSummary.get("dataEntryComplete").equals("Yes")) {
					subtitle.append(" not");
				}
				subtitle.append(" Complete");
			} else {
				subtitle.append("<b>")
						.append(currentOrg.getOrgTypeName().equals("Readiness") ? consortium.toString() : orgSummary
								.get("orgName")).append(" - ");
				subtitle.append(orgSummary.get("percentComplete")).append("</b> of Schools ")
						.append(" have Indicated the Data are Complete");
			}

			ReportExportPdf pdf = new ReportExportPdf.Builder(columnLabels, columnKeys, reportItemProvider.export(type))
					.title("Completion Status Report").subtitle(subtitle.toString()).consortium(consortium.toString())
					.aggregationMessage("Data as of " + orgSummary.get("createDate") + " - Reports are updated hourly")
					.build();
			if (!getReturn().equals("org")) {
				pdf.setTemplate("reports/wrapper-completion-map.jrxml");
				pdf.setMapUrl(geoChart.getUrl());
			} else {
				if (currentOrg.getOrgTypeName().equals("District") || currentOrg.getOrgTypeName().equals("School")) {
					pdf.setTemplate("reports/wrapper-completion.jrxml");
				} else {
					pdf.setTemplate("reports/wrapper-completion-org.jrxml");
				}
			}
			return pdf;
		}
		}

		return null;
	}

	@Override
	protected ReportExport getAllSchoolsReportExport(ExportType type) throws Exception {
		return null;
	}

	public String getOrgCode() {
		return orgCode;
	}

	public void setOrgCode(String selectedOrgCode) {
		orgCode = selectedOrgCode;
	}

	public GeoChartBean getGeoChart() {
		return geoChart;
	}

	public void setGeoChart(GeoChartBean geoChart) {
		this.geoChart = geoChart;
	}

	private String getReturn() {
		return currentOrg != null && currentOrg.getOrgTypeName().equals("Readiness") ? getViewBy() : "org";
	}

	public ReportItemProvider getReportItemProvider() {
		return reportItemProvider;
	}

	public DataGridState<?> getReportGrid() {
		return reportGrid;
	}

	public void setReportGrid(DataGridState<?> reportGrid) {
		this.reportGrid = reportGrid;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public boolean isDualConsortium() {
		return dualConsortium;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public Map<String, String> getOrgSummary() {
		return orgSummary;
	}

	public Org getCurrentOrg() {
		return currentOrg;
	}

}
