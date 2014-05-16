<%@page import="de.hybris.platform.servicelayer.search.FlexibleSearchService"%>
<%@page import="java.util.Collections"%>
<%@include file="../../head.inc" %>

<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="org.jirafe.hmc.administration.StatusDisplayChip" %>

<% final StatusDisplayChip displayChip = (StatusDisplayChip) request.getAttribute(StatusDisplayChip.CHIP_KEY); %>

<div class="statusDisplayChip">
	<table>
		<tr>
			<td><div class="xp-button">
				<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.REFRESH) %>');setScrollAndSubmit();return false;"><span class="label">Refresh</span></a>
			</div></td>
			<td><div class="xp-button">
				<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.EXPORT) %>');setScrollAndSubmit();return false;"><span class="label">Export Configuration...</span></a>
			</div></td>
		</tr>
	</table>
	<table>
		<tr><td class="sectionheader" colspan="2"><div class="sh">Basic info</div></td></tr>
		<% for (final List<String> row: displayChip.getBasicInfo()) { %>
		<tr>
			<td><%= row.get(0) %></td>
			<td><%= row.get(1) %></td>
		</tr>
		<% } %>
	</table>
	<table>
		<tr><td class="sectionheader"><div class="sh">Cronjob status</div></td></tr>
		<tr><td><table class="listtable">
			<tr>
				<th>Name</th>
				<th>Enabled</th>
				<th>Last run</th>
			</tr>
			<% for (final List<String> row: displayChip.getCronjobStatus()) { %>
			<tr>
				<td><%= row.get(0) %></td>
				<td><%= row.get(1) %></td>
				<td><%= row.get(2) %></td>
			</tr>
			<% } %>
		</table></td></tr>
	</table>
	<table>
		<tr><td class="sectionheader" colspan="3"><div class="sh">Connection status</div></td></tr>
		<%
			List<List<String>> connectionStatus = displayChip.getConnectionStatus();
			if (connectionStatus == null) {
		%>
		<tr><td colspan="3">No sites defined!</td></tr>
		<%
			} else {
				for (final List<String> row: connectionStatus) {
		%>
				<tr>
					<td><%= row.get(0).toUpperCase() %></td>
					<td><%= row.get(1) %></td>
					<td><%= row.get(2) %></td>
				</tr>
		<%
				}
			}
		%>
	</table>
	<table>
		<tr><td class="sectionheader"><div class="sh">Synchronization status</div></td></tr>
		<tr><td><table class="listtable">
			<tr>
				<th>Type</th>
				<th>Status</th>
				<th>Count</th>
			</tr>
		<%
			final Map<String,List<List<String>>> syncStatus = displayChip.getSyncStatus();
			for (final String siteName: syncStatus.keySet()) {
		%>
		<tr><th colspan="3"><%= siteName.toUpperCase() %></th></tr>
		<%
				List<List<String>> syncStatusRows = syncStatus.get(siteName);
				if (syncStatusRows == null) {
		%>
		<tr><td colspan="3">No data found.</td></tr>
		<%
				} else {
					for (final List<String> row: syncStatusRows) {
		%>
		<tr>
			<td><%= row.get(0) %></td>
			<td><%= row.get(1) %></td>
			<td><%= row.get(2) %></td>
		</tr>
		<%
					}
				}
			}
		%>
		</table></td></tr>
	</table>
	<table>
		<tr><td class="sectionheader" colspan="2"><div class="sh">Properties</div></td></tr>
		<% for (final List<String> row: displayChip.getProperties()) { %>
		<tr>
			<td><%= row.get(0) %></td>
			<td><%= row.get(1) %></td>
		</tr>
		<% } %>
	</table>
</div>
