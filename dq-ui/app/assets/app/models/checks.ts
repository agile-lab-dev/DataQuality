import { Page } from './page'

export class CheckResponse extends Page {
    checks: Check[];
}

export class Check {
    id: string;
    cType: string;
    subtype: string;
    description: string;
    constructor(id: string,
        cType: string,
        subtype: string,
        description: string) {
            this.id = id;
            this.cType = cType;
            this.subtype = subtype;
            this.description = description;
        }
}

export class CheckParameter {
    owner?: string;
    name: string;
    value?: string;
    constructor(name: string) {
        this.name = name;
    }
}

export class CheckSql extends Check {
    database: string;
    query: string;
    constructor(id: string,
        cType?: string,
        subtype?: string,
        description?: string,
        database?: string,
        query?: string) {
            super(id, cType, subtype, description);
            this.database = database;
            this.query = query;
        }
}

export class CheckSnapshot extends Check {
    metric: string;
    parameters: CheckParameter[];
    constructor(id: string,
        cType: string,
        subtype: string,
        description: string,
        metric: string,
        parameters: CheckParameter[]) {
            super(id, cType, subtype, description);
            this.metric = metric;
            this.parameters = parameters;
        }
}

export class CheckTrend extends CheckSnapshot {
    rule: string;
    constructor(id: string,
        cType: string,
        subtype: string,
        description: string,
        metric: string,
        parameters: CheckParameter[],
        rule: string) {
            super(id,
                cType,
                subtype,
                description,
                metric,
                parameters);
                this.rule = rule;
        }
}
