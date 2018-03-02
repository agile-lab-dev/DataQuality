
export class Parameter {
    param: string;
    value: any;

    static of(param: string, value: any): Parameter {
        return new Parameter(param, value);
    }

    private constructor(param: string, value: any) {
        this.param = param;
        this.value = value;
    }


}
export class URL {
    static encodeQueryData(params: Array<Parameter>) {
        const ret = [];
        for (const param of params){
          ret.push(encodeURIComponent(param.param) + '=' + encodeURIComponent(param.value));
        }
        return ret.join('&');
     }
}
