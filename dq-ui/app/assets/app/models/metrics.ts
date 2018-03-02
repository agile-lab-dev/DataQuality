import { Page } from './page'
export class MetricResponse extends Page {
    metrics: Metric[];
}

export class Metric {
    id: string;
    name: string;
    description: string;
    mType: string;
    constructor(id: string, name: string, description: string, mType: string) {
        this.id = id;
        this.name = name || 'default name';
        this.description = description;
        this.mType = mType;
    }
}

export class MetricFile extends Metric {
    source: string;
    constructor(id: string, name?: string, description?: string, source?: string, mType?: string) {
        super(id, name, description, mType);
        this.source = source;
    }
}

export class MetricParameterBase {
    owner?: string;
    name: string;
    value?: string;
    constructor(name: string) {
        this.name = name;
    }
}

export class MetricColumn extends MetricFile {
    columns: Array<string>;
    parameters: MetricParameterBase[];
    constructor(id: string, name?: string, description?: string, source?: string,
        type?: string, columns?:  Array<string>, parameters?: MetricParameterBase[]) {
        super(id, name, description, source, type);
        this.columns = columns || [];
        this.parameters = parameters || [];
    }
}

export class MetricComposed extends Metric {
    formula: string;
    constructor(id: string, mType?: string, name?: string, description?: string, formula?: string, ) {
        super(id, name, description, mType);
        this.formula = formula;
        this.mType = 'COMPOSED'
    }
}
