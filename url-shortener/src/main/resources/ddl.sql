CREATE TABLE IF NOT EXISTS url_mappings (
	alias text NOT NULL PRIMARY KEY,
	url text,
	creation_date long
);
CREATE INDEX IF NOT EXISTS old_date_indicator ON url_mappings(creation_date ASC);