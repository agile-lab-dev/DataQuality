import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { DatabaseResponse, Database } from '../models/databases';

import { URL, Parameter } from '../common/url';

@Injectable()
export class DatabaseService {
  private headers = new Headers({'Content-Type': 'application/json'});
  private baseDatabaseUrl = 'dataquality/database';

  constructor(private http: Http) { }

  /* Databases */

  getAllDatabases(page?: number): Promise<DatabaseResponse> {

    const prameters: Array<Parameter> = [];

    if (page >= 0) {
      prameters.push(Parameter.of('page', page));
    }

    const prametersString = '?' + URL.encodeQueryData(prameters);

    return this.http.get(`${this.baseDatabaseUrl}${prametersString}`)
               .toPromise()
               .then(response => response.json() as DatabaseResponse)
               .catch(this.handleError);
  }

  getDatabaseIds(filter?: string): Promise<DatabaseResponse> {
    
        const prameters: Array<Parameter> = [];
    
        if (filter) {
          prameters.push(Parameter.of('filter', filter));
        }
    
        const prametersString = '?' + URL.encodeQueryData(prameters);
    
        return this.http.get(`${this.baseDatabaseUrl}/list/id${prametersString}`)
                   .toPromise()
                   .then(response => response.json() as DatabaseResponse)
                   .catch(this.handleError);
      }

  getDatabaseDetails(id: string): Promise<Database> {
    return this.http.get(`${this.baseDatabaseUrl}/${id}`)
               .toPromise()
               .then(response => response.json() as Database)
               .catch(this.handleError);
  }

  addDatabase(db: Database): Promise<Database> {
    return this.http
      .post(this.baseDatabaseUrl, JSON.stringify(db), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as Database)
      .catch(this.handleError);
  }

  updateDatabase(id: string, db: Database): Promise<Database> {
    return this.http
      .put(`${this.baseDatabaseUrl}/${id}`, JSON.stringify(db), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as Database)
      .catch(this.handleError);
  }

  deleteDatabase(id: string): Promise<void> {
    return this.http.delete(`${this.baseDatabaseUrl}/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }


  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
