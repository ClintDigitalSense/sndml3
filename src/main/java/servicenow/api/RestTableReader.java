package servicenow.api;

import java.io.IOException;
import java.sql.SQLException;

public class RestTableReader extends TableReader {

	final protected RestTableAPI apiREST;
	private boolean statsEnabled = false;
	protected TableStats stats = null;
	
	private final int DEFAULT_PAGE_SIZE = 200;
	
	public RestTableReader(Table table) {
		super(table);
		this.apiREST = table.rest();
	}
			
	public int getDefaultPageSize() {
		return DEFAULT_PAGE_SIZE;
	}
	
	public RestTableReader enableStats(boolean value) {
		this.statsEnabled = value;
		return this;
	}

	public void initialize() throws IOException {		
		super.initialize();
		EncodedQuery query = getQuery();
		logger.debug(Log.INIT, String.format("initialize statsEnabled=%b query=\"%s\"", statsEnabled, query));
		if (statsEnabled) {
			stats = apiREST.getStats(query, false);
			setExpected(stats.getCount());
			logger.debug(Log.INIT, String.format("expected=%d", getExpected()));	
		}
	}
	
	public RestTableReader call() throws IOException, SQLException, InterruptedException {
		assert initialized;
		Writer writer = getWriter();
		assert writer != null;
		setLogContext();
		int rowCount = 0;
		boolean finished = false;
		if (statsEnabled && stats.count == 0) {
			finished = true;
			logger.debug(Log.PROCESS, "expecting 0 rows; bypassing query");
		}
		int offset = 0;
		int pageSize = getPageSize();
		assert pageSize > 0;
		while (!finished) {
			Parameters params = new Parameters();
			params.add("sysparm_offset", Integer.toString(offset));
			params.add("sysparm_limit", Integer.toString(pageSize));
			params.add("sysparm_exclude_reference_link", "true");			
			params.add("sysparm_display_value", displayValue ? "all" : "false");
			if (!EncodedQuery.isEmpty(getQuery())) params.add("sysparm_query", getQuery().toString());
			if (fieldNames != null) params.add("sysparm_fields", fieldNames.toString());
			if (viewName != null) params.add("sysparm_view", viewName);
			RecordList recs = apiREST.getRecords(params);
			getReaderMetrics().increment(recs.size());
			writer.processRecords(this, recs);			
			rowCount += recs.size();
			offset += recs.size();
			finished = (recs.size() < pageSize);
			logger.debug(Log.PROCESS, String.format("processed %d rows", rowCount));
			if (maxRows != null && rowCount > maxRows)
				throw new RowCountExceededException(table, 
					String.format("processed %d rows (MaxRows=%d)", rowCount, maxRows));
		}
		if (statsEnabled) {
			if (rowCount != getExpected()) {
				logger.error(Log.PROCESS, 
					String.format("Expected %d rows but processed %d rows", getExpected(), rowCount));
			}
		}
		return this;
	}
		
}
