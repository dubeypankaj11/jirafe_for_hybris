<%@include file="../../head.inc" %>
<%@page import="org.jirafe.hmc.administration.DataMapsDisplayChip" %>

<% final DataMapsDisplayChip displayChip = (DataMapsDisplayChip) request.getAttribute(DataMapsDisplayChip.CHIP_KEY); %>

<div class="dataMapsDisplayChip">
	<form>
		<table class="listtable">
			<tr>
				<td colspan="2">
					<table>
						<tr>
							<td>Type</td>
							<td>
								<select name="type">
									<% for (final String type: displayChip.getJirafeMappingsDao().getAllMappedTypes()) { %>
										<%= "<option" + (type.equals(displayChip.type) ? " selected" : "") + ">" + type + "</option>" %>
									<% } %>
								</select>
							</td>
						</tr>
						<tr>
							<td>PK</td>
							<td><input name="pk" value="<%= displayChip.pk %>"></td>
							<td><div class="xp-button">
								<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.PK_PICKER) %>');setScrollAndSubmit();return false;"><span class="label">...</span></a>
							</div></td>
						</tr>
						<tr>
							<td>Site</td>
							<td>
								<select name="siteName">
									<% for (final String siteName: displayChip.getConnectionConfig().getSiteNames()) { %>
										<%= "<option" + (siteName.equals(displayChip.siteName) ? " selected" : "") + ">" + siteName + "</option>" %>
									<% } %>
								</select>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<table>
						<tr>
							<td><div class="xp-button">
								<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.LOAD_MAP) %>');setScrollAndSubmit();return false;"><span class="label">Reload map</span></a>
							</div></td>
							<td><div class="xp-button">
								<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.TEST_MAP) %>');setScrollAndSubmit();return false;"><span class="label">Run map</span></a>
							</div></td>
							<td><div class="xp-button">
								<a href="#" onclick="setEvent('<%= displayChip.getEventID(displayChip.SAVE_MAP) %>');setScrollAndSubmit();return false;"><span class="label">Save map</span></a>
							</div></td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<th>Filter</th><th>Filter result</th>
			</tr>
			<tr>
				<td><textarea name="filter" rows="1" cols="80"><%= displayChip.filter %></textarea></td>
				<td><textarea name="filterOutput" rows="1" cols="80"><%= displayChip.filterOutput %></textarea></td>
			</tr>
			<tr>
				<th>Data map</th><th>Data map output</th>
			</tr>
			<tr>
				<td><textarea name="dataMap" rows="24" cols="80"><%= displayChip.dataMap %></textarea></td>
				<td><textarea name="output" rows="24" cols="80"><%= displayChip.output %></textarea></td>
			</tr>
		</table>
	</form>
</div>
