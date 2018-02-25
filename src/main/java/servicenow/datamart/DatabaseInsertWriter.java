package servicenow.datamart;

import servicenow.api.*;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.slf4j.Logger;

public class DatabaseInsertWriter extends DatabaseTableWriter {

	protected DatabaseInsertStatement insertStmt;

	final private Logger logger = Log.logger(this.getClass());
	
	public DatabaseInsertWriter(String name, Database db, Table table, String sqlTableName) throws IOException, SQLException {
		super(name, db, table, sqlTableName);
	}

	@Override
	public void open() throws SQLException, IOException {
		super.open();
		insertStmt = new DatabaseInsertStatement(this.db, this.sqlTableName, columns);
	}
	
	@Override
	void writeRecord(Record rec) throws SQLException {
		Key key = rec.getKey();
		logger.trace(Log.PROCESS, "Insert " + key);
		try {
			insertStmt.insert(rec);
			writerMetrics.incrementInserted();
		}
		catch (SQLIntegrityConstraintViolationException e) {
			logger.trace(Log.PROCESS, "Failed/Skipped " + key);
			writerMetrics.incrementSkipped();
		}
	}

}