import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { CheckMeta, ParamItem } from '../models/metas';

import { URL, Parameter } from '../common/url';

/**
 * This service manage all meta of an entity
 */
@Injectable()
export class MetasService {
  private headers = new Headers({'Content-Type': 'application/json'});
  private baseMetaUrl = 'dataquality/meta';

  constructor(private http: Http) { }

  /** Return a metas id list of selected entity */
  getMetaEntityIds(entity: string, filter?: string): Promise<string[]> {

        const prameters: Array<Parameter> = [];

        if (filter) {
            prameters.push(Parameter.of('filter', filter));
        }

        const prametersString = '?' + URL.encodeQueryData(prameters);

        return this.http.get(`${this.baseMetaUrl}/${entity}/id${prametersString}`)
               .toPromise()
               .then(response => response.json() as string[])
               .catch(this.handleError);
    }

    /** Return a metas list of selected entity */
    getEntityMetas(entity: string, filter?: string): Promise<CheckMeta[]> {
        
        const prameters: Array<Parameter> = [];
        
        if (filter) {
            prameters.push(Parameter.of('filter', filter));
        }
        
        const prametersString = '?' + URL.encodeQueryData(prameters);
        
        return this.http.get(`${this.baseMetaUrl}/${entity}${prametersString}`)
                       .toPromise()
                       .then(response => response.json() as CheckMeta[])
                       .catch(this.handleError);
    }

    /** Return a metas list of selected entity */
    getEntityMetaById(entity: string, id: string): Promise<CheckMeta[]> {
        
        return this.http.get(`${this.baseMetaUrl}/${entity}/${id}`)
                       .toPromise()
                       .then(response => response.json() as CheckMeta)
                       .catch(this.handleError);
    }


    getMetaParameters(entity: string): Promise<ParamItem[]> {
        
        return this.getEntityMetas(entity)
        .then(metas => {
            return metas.map(meta => new ParamItem(meta.id, meta.parameters))
        });
    }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
