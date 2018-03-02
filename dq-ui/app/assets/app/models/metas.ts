export enum ParamType {
    METRIC
}

export class MetaParameter {
    name: string;
    paramType?: ParamType;
    description?: string;
    value?: any
}   

export class ParamItem {
    metricType: string;
    parameters: MetaParameter[];
  
    constructor(mType: string, parameters: MetaParameter[]) {
      this.metricType = mType;
      this.parameters = parameters;
    }
  }

export class Meta {
    id: string;
    description: string;
    parameters: MetaParameter[];
}



export class CheckMeta extends Meta {
    checkType: string;
    withMetric: boolean;
}
