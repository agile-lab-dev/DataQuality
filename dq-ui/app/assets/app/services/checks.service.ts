import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { CheckResponse, CheckParameter, CheckSql, CheckSnapshot, CheckTrend } from '../models/checks';
import { URL, Parameter } from '../common/url';


@Injectable()
export class ChecksService {
    private headers = new Headers({'Content-Type': 'application/json'});
    private baseCheckUrl = 'dataquality/check';

    constructor(private http: Http) { }

    /* Checks */

    getAllChecks(databaseId?: string, metricId?: string, page?: number, filter?: string): Promise<CheckResponse> {

        const prameters: Array<Parameter> = [];
        if (databaseId) {
          prameters.push(Parameter.of('db', databaseId));
        }
    
        if (metricId) {
          prameters.push(Parameter.of('met', metricId));
        }

        if (page >= 0) {
            prameters.push(Parameter.of('page', page));
          }
  
    
        if (filter) {
          prameters.push(Parameter.of('filter', filter));
        }

        const prametersString = '?' + URL.encodeQueryData(prameters);
        
        return this.http.get(`${this.baseCheckUrl}${prametersString}`)
                    .toPromise()
                    .then(response => response.json() as CheckResponse)
                    .catch(this.handleError);
    }

    // check -> param

    addCheckParameter(id: string, cp: CheckParameter): Promise<void> {
        return this.http
            .post(`${this.baseCheckUrl}/${id}/param`, JSON.stringify(cp), {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    deleteCheckParameter(id: string, cp: CheckParameter): Promise<void> {
        return this.http.delete(`${this.baseCheckUrl}/${id}/param`, {headers: this.headers, body: JSON.stringify(cp)})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    // check -> sql

    getSqlCheckDetails(id: string): Promise<CheckSql> {
        return this.http.get(`${this.baseCheckUrl}/sql/${id}`)
                    .toPromise()
                    .then(response => response.json() as CheckSql)
                    .catch(this.handleError);
    }

    addSqlCheck(cs: CheckSql): Promise<CheckSql> {
        return this.http
            .post(`${this.baseCheckUrl}/sql`, JSON.stringify(cs), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckSql)
            .catch(this.handleError);
    }

    updateSqlCheck(id: string, mc: CheckSql): Promise<CheckSql> {
        return this.http
            .put(`${this.baseCheckUrl}/sql/${id}`, JSON.stringify(mc), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckSql)
            .catch(this.handleError);
    }

    deleteSqlCheck(id: string): Promise<void> {
        return this.http.delete(`${this.baseCheckUrl}/sql/${id}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    // check -> snapshot

    getSnapshotCheckDetails(id: string): Promise<CheckSnapshot> {
        return this.http.get(`${this.baseCheckUrl}/snapshot/${id}`)
                    .toPromise()
                    .then(response => response.json() as CheckSnapshot)
                    .catch(this.handleError);
    }

    addSnapshotCheck(cs: CheckSnapshot): Promise<CheckSnapshot> {
        return this.http
            .post(`${this.baseCheckUrl}/snapshot`, JSON.stringify(cs), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckSnapshot)
            .catch(this.handleError);
    }

    updateSnapshotCheck(id: string, cs: CheckSnapshot): Promise<CheckSnapshot> {
        return this.http
            .put(`${this.baseCheckUrl}/snapshot/${id}`, JSON.stringify(cs), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckSnapshot)
            .catch(this.handleError);
    }

    deleteSnapshotCheck(id: string): Promise<void> {
        return this.http.delete(`${this.baseCheckUrl}/snapshot/${id}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    // check -> trend

    getTrendCheckDetails(id: string): Promise<CheckTrend> {
        return this.http.get(`${this.baseCheckUrl}/trend/${id}`)
                    .toPromise()
                    .then(response => response.json() as CheckTrend)
                    .catch(this.handleError);
    }

    addTrendCheck(cs: CheckTrend): Promise<CheckTrend> {
        return this.http
            .post(`${this.baseCheckUrl}/trend`, JSON.stringify(cs), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckTrend)
            .catch(this.handleError);
    }

    updateTrendCheck(id: string, cs: CheckTrend): Promise<CheckTrend> {
        return this.http
            .put(`${this.baseCheckUrl}/trend/${id}`, JSON.stringify(cs), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as CheckTrend)
            .catch(this.handleError);
    }

    deleteTrendCheck(id: string): Promise<void> {
        return this.http.delete(`${this.baseCheckUrl}/trend/${id}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    private handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }

}
