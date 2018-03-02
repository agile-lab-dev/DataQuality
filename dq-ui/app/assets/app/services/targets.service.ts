import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { URL, Parameter } from '../common/url';

import {TargetResponse, TargetBase, Target, Mail, Check } from '../models/targets';

@Injectable()
export class TargetsService {
    private headers = new Headers({'Content-Type': 'application/json'});
    private baseTargetUrl = 'dataquality/target';

    constructor(private http: Http) { }

    /* Target */

    getAllTargets(checkId?: string, page?: number, filter?: string): Promise<TargetResponse> {

        const prameters: Array<Parameter> = [];
        if (checkId) {
            prameters.push(Parameter.of('chk', checkId));
          }
      
          if (page >= 0) {
            prameters.push(Parameter.of('page', page));
          }
      
          if (filter) {
            prameters.push(Parameter.of('filter', filter));
          }
      
          const prametersString = '?' + URL.encodeQueryData(prameters);

        return this.http.get(`${this.baseTargetUrl}${prametersString}`)
            .toPromise()
            .then(response => response.json() as TargetResponse)
            .catch(this.handleError);
    }

    getTargetDetails(id: string): Promise<Target> {
        return this.http.get(`${this.baseTargetUrl}/${id}`)
            .toPromise()
            .then(response => response.json() as Target)
            .catch(this.handleError);
    }

    addTarget(t: Target): Promise<Target> {
        return this.http
            .post(`${this.baseTargetUrl}`, JSON.stringify(t), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as Target)
            .catch(this.handleError);
    }

    updateTarget(id: string, mc: Target): Promise<Target> {
        return this.http
            .put(`${this.baseTargetUrl}/${id}`, JSON.stringify(mc), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as Target)
            .catch(this.handleError);
    }

    deleteTarget(id: string): Promise<void> {
        return this.http.delete(`${this.baseTargetUrl}/${id}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    // target -> mail

    getTargetMailList(id: string): Promise<Mail> {
        return this.http.get(`${this.baseTargetUrl}/${id}/mail`)
            .toPromise()
            .then(response => response.json() as Mail)
            .catch(this.handleError);
    }

    addTargetMail(id: string, t: Mail): Promise<Mail> {
        return this.http
            .post(`${this.baseTargetUrl}/${id}/mail`, JSON.stringify(t), {headers: this.headers})
            .toPromise()
            .then(res => res.json() as Mail)
            .catch(this.handleError);
    }

    deleteTargetMail(id: string, t: Mail): Promise<void> {
        return this.http
            .delete(`${this.baseTargetUrl}/${id}/mail?mail=${t.address}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    // target -> check

    getTargetCheckList(id: string): Promise<Target> {
        return this.http.get(`${this.baseTargetUrl}/${id}/check`)
            .toPromise()
            .then(response => response.json() as Target)
            .catch(this.handleError);
    }

    addTargetCheck(id: string, c: Check): Promise<void> {
        return this.http
            .post(`${this.baseTargetUrl}/${id}/check`, JSON.stringify(c), {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    deleteTargetCheck(id: string, c: Check): Promise<void> {
        return this.http
            .delete(`${this.baseTargetUrl}/${id}/check?check=${c.checkId}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    private handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }

}
