package servicenow.json;

import servicenow.core.*;

public class KeySetTableReaderFactory extends TableReaderFactory {
	
	public KeySetTableReaderFactory(Table table, Writer writer) {
		super(table, writer);
	}
	
	public KeySetTableReader createReader() {
		KeySetTableReader reader = new KeySetTableReader(this.table);
		reader.setBaseQuery(baseQuery);
		reader.setUpdatedRange(updatedRange);
		reader.setCreatedRange(createdRange);
		reader.setWriter(writer);
		reader.setReaderName(readerName);
		return reader;
	}

}