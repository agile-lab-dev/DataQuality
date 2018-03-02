import { Injectable } from '@angular/core';
import { Headers, Http, ResponseContentType } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { Search } from '../models/search'
import { URL, Parameter } from '../common/url';

@Injectable()
export class SearchService {
    private headers = new Headers({'Content-Type': 'application/json'});
    private baseConfigUrl = 'dataquality/search';

    fileName: string;

    constructor(private http: Http) { }

    search(entity: string, query: string, refType?: string, refId?: string, page?: number): Promise<Search> {

        const prameters: Array<Parameter> = [];

        prameters.push(Parameter.of('query', query));

        if (page >= 0) {
            prameters.push(Parameter.of('page', page));
        }

        if (refType) {
            prameters.push(Parameter.of('ref_type', refType));
        }

        if (refId) {
            prameters.push(Parameter.of('ref_id', refId));
        }
    
        const prametersString = '?' + URL.encodeQueryData(prameters);

        return this.http.get(`${this.baseConfigUrl}/${entity}${prametersString}`, { responseType: ResponseContentType.Text })
        .toPromise()
        .then( resposnse => resposnse.json() as Search)
    }

    private handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }
}
