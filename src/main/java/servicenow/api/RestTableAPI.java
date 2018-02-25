package servicenow.api;

import java.io.IOException;
import java.net.URI;
import org.json.JSONObject;
import org.slf4j.Logger;

public class RestTableAPI extends TableAPI {

	final private Logger logger = Log.logger(this.getClass());
	
	public RestTableAPI(Table table) {
		super(table);
	}

	public Instance getInstance() {
		return getSession().getInstance();
	}
	
	private URI getURI(String api, Key sys_id, Parameters params) {
		assert api != null;
		String path = "api/now/" + api + "/" + table.getName();
		if (sys_id != null) path += "/" + sys_id.toString();
		URI uri = session.getURI(path, params);
		return uri;
	}
		
	public TableStats getStats(EncodedQuery filter, boolean includeDates) throws IOException {
		TableStats stats = new TableStats();
		Parameters params = new Parameters();
		if (filter != null) params.add("sysparm_query", filter.toString());
		String aggregateFields = "sys_created_on";
		params.add("sysparm_count", "true");
		if (includeDates) {
			params.add("sysparm_min_fields", aggregateFields);
			params.add("sysparm_max_fields", aggregateFields);			
		}
		URI uri = getURI("stats", null, params);
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.GET, null);
		JSONObject responseObj = request.execute();
		request.checkForInsufficientRights();
		JSONObject result = responseObj.getJSONObject("result").getJSONObject("stats");
		stats.count = Integer.parseInt(result.getString("count"));
		if (includeDates) {
			JSONObject minValues = result.getJSONObject("min");
			JSONObject maxValues = result.getJSONObject("max");
			DateTime minCreated = new DateTime(minValues.getString("sys_created_on"));
			DateTime maxCreated = new DateTime(maxValues.getString("sys_created_on")); 
			stats.created = new DateTimeRange(minCreated, maxCreated);
		}
		logger.info(Log.PROCESS, String.format("getStats query=\"%s\" count=%s", filter, stats.count));
		return stats;		
	}
	
	public Record getRecord(Key key) throws IOException {
		URI uri = getURI("table", key, null);
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.GET, null);
		JSONObject responseObj = request.execute();
		request.checkForInsufficientRights();		
		if (responseObj.has("error")) {
			JSONObject errorObj = responseObj.getJSONObject("error");
			String message = errorObj.getString("message");
			if (message.equalsIgnoreCase("No record found")) return null;
			throw new JsonResponseError(responseObj.toString());
		}
		JSONObject resultObj =  responseObj.getJSONObject("result");
		if (resultObj == null) return null;
		JsonRecord rec = new JsonRecord(this.table, resultObj);
		return rec;
	}

	public RecordList getRecords() throws IOException {
		return getRecords((Parameters) null);
	}

	public RecordList getRecords(String fieldname, String fieldvalue, boolean displayValues) throws IOException {
		Parameters params = new Parameters();
		params.add(fieldname, fieldvalue);
		params.add("sysparm_display_value", displayValues ? "all" : "false");
		params.add("sysparm_exclude_reference_link", "true");
		return getRecords(params);
	}

	public RecordList getRecords(EncodedQuery query, boolean displayValue) throws IOException {
		Parameters params = new Parameters();
		if (query != null) params.add("sysparm_query", query.toString());
		params.add("sysparm_display_value", displayValue ? "all" : "false");
		params.add("sysparm_exclude_reference_link", "true");
		return getRecords(params);
	}
	
	public RecordList getRecords(Parameters params) throws IOException {		
		URI uri = getURI("table", null, params);
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.GET, null);
		JSONObject responseObj = request.execute();
		request.checkForInsufficientRights();		
		assert responseObj.has("result");
		RecordList list = new RecordList(table, responseObj, "result");
		return list;
	}

	public InsertResponse insertRecord(Parameters fields) throws IOException {
		URI uri = getURI("table", null, null);
		JSONObject requestObj = fields.toJSON();
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.POST, requestObj);
		JSONObject responseObj = request.execute();
		request.checkForInsufficientRights();		
		assert responseObj.has("result");
		JSONObject resultObj = responseObj.getJSONObject("result");
		JsonRecord rec = new JsonRecord(this.table, resultObj);
		return rec;
	}

	public void updateRecord(Key key, Parameters fields) throws IOException {
		URI uri = getURI("table", key, null);
		JSONObject requestObj = fields.toJSON();
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.PUT, requestObj);
		JSONObject responseObj = request.execute();
		request.checkForInsufficientRights();		
		JSONObject resultObj = responseObj.getJSONObject("result");
		@SuppressWarnings("unused") // discard the response
		JsonRecord rec = new JsonRecord(this.table, resultObj);
	}
	
	public boolean deleteRecord(Key key) throws IOException {
		URI uri = getURI("table", key, null);
		JsonRequest request = new JsonRequest(client, uri, HttpMethod.DELETE, null);
		JSONObject responseObj = request.execute();
		if (responseObj == null) return true;
		if (request.recordNotFound()) return false;
		throw new JsonResponseException(responseObj);
	}
	
	@Override
	public RestTableReader getDefaultReader() throws IOException {
		return new RestTableReader(this.table);
	}
	
}
