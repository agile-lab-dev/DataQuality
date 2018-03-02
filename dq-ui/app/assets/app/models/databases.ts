import { Page } from './page'

export class DatabaseResponse extends Page {
    databases: DatabaseShortList[] | string[];
}

export class DatabaseShortList {
    id: string;
    host: string;
    constructor(id: string, host: string) { this.id = id; this.host = host; }
}

export class Database extends DatabaseShortList {
    subtype: string;
    port: number;
    service: string;
    user: string;
    password: string;
    constructor(id: string, host?: string, subtype?: string, port?: number, service?: string, user?: string, password?: string) {
         super(id, host);
         this.subtype = subtype;
         this.port = port;
         this.service = service;
         this.user = user;
         this.password = password;
    }
}
