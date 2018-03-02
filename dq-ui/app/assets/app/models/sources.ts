export class SourceResponse {
  sources: Source[] | string[];
  count: number;
  last_page: number;
}

export class Source {
  id: string;
  scType: string;
}

export class Virtual {
  id: string;
  keyFields: Array<string>;
  typo: string;
  left: Source;
  right: Source;
  query: string;
  constructor(
    id: string,
    keyFields?: Array<string>,
    typo?: string,
    left?: Source,
    right?: Source,
    query?: string) {
      this.id = id;
      this.keyFields = keyFields || [];
      this.left = left;
      this.right = right;
      this.query = query;
    }
}

export class DbTable {
  id: string;
  keyFields?: Array<string>;
  database: string;
  table: string;
  username: string;
  password: string;
  constructor(
    id: string,
    keyFields?: Array<string>,
    database?: string,
    table?: string,
    username?: string,
    password?: string) {
      this.id = id;
      this.keyFields = keyFields || [];
      this.database = database;
      this.table = table;
      this.username = username;
      this.password = password;
    }
}

export class HiveTable {
  id: string;
  keyFields?: Array<string>;
  date: string;
  query: string;
  constructor(id: string, keyFields?: Array<string>, date?: string, query?: string) {
    this.id = id;
    this.keyFields = keyFields || [];
    this.date = date;
    this.query = query;
  }
}

export class HdfsFile {
  id: string;
  keyFields?: Array<string>;
  path: string;
  fileType: string;
  separator: string;
  header: boolean;
  schema: SchemaItem[];
  schema_path: string;
  date: string;
  constructor(id: string,
    keyFields?: Array<string>,
    path?: string,
    fileType?: string,
    separator?: string,
    header?: boolean,
    schema?: SchemaItem[],
    schema_path?: string,
    date?: string) {
      this.id = id;
      this.keyFields = keyFields || [];
      this.path = path;
      this.fileType = fileType;
      this.separator = separator;
      this.header = header;
      this.schema = schema;
      this.schema_path = schema_path;
      this.date = date;
    }
}

export class SchemaItem {
  name: string;
  type: string;
  constructor(name: string, type: string) {
    this.name = name;
    this.type = type;
  }
}

export class FileFiledBase {
  fieldName: string;
  constructor(fieldName: string) {
    this.fieldName = fieldName;
  }
}

export class FileField extends FileFiledBase {
  owner: string;
  fieldType: string;
  constructor(fieldName: string, owner: string, fieldType: string) {
    super(fieldName);
    this.owner = owner;
    this.fieldType = fieldType;
  }
}
