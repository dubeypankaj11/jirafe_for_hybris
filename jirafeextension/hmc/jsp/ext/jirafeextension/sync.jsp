<%@include file="../../head.inc" %>

<%@page import="de.hybris.platform.util.Config"%>
<%@page import="org.jirafe.constants.JirafeextensionConstants"%>
<%@page import="org.jirafe.hmc.administration.SyncDisplayChip" %>

<% final SyncDisplayChip displayChip = (SyncDisplayChip) request.getAttribute(SyncDisplayChip.CHIP_KEY); %>

<div class="syncDisplayChip">
	<table>
		<tr>
			<td><div class="xp-button">
				<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.REFRESH) %>');setScrollAndSubmit();return false;"><span class="label">Refresh</span></a>
			</div></td>
		</tr>
	</table>
	<table>
		<tr>
			<td class="sectionheader">
				<div class="sh">Historical sync</div>
			</td>
		</tr>
		<tr><td><table class="listtable">
			<tr>
				<th>Site</th>
				<th>Status</th>
				<th></th>
			</tr>
		<%
			for (final String siteName: displayChip.getConnectionConfig().getSiteNames()) {
				final String status = displayChip.getHistoricalSyncStatus(siteName);
		%>
			<tr>
				<td><%= siteName.toUpperCase() %></td>
				<td><%= status %></td>
				<td>
		<%
				if (status.startsWith("disable")) {
		%>
					<div class="xp-button-disabled">
						<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.REFRESH) %>');setScrollAndSubmit();return false;"><span class="label">disabled</span></a>
					</div>
		<%
				} else if (status.startsWith("RUNNING")) {
		%>
					<div class="xp-button">
						<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.STOP_SYNC) %>', '<%= siteName %>');setScrollAndSubmit();return false;"><span class="label">Stop sync</span></a>
					</div>
		<%
				} else {
		%>
					<div class="xp-button">
						<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.START_SYNC) %>', '<%= siteName %>');setScrollAndSubmit();return false;"><span class="label">Start sync</span></a>
					</div>
		<% 		} %>
				</td>
			</tr>
		<% 	} %>
		</table></td></tr>
	</table>
</div>
