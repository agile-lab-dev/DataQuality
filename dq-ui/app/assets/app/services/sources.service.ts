import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { SourceResponse, Source, DbTable, HiveTable, HdfsFile, FileField, FileFiledBase, Virtual } from '../models/sources';

import { URL, Parameter } from '../common/url';

@Injectable()
export class SourcesService {
  private headers = new Headers({'Content-Type': 'application/json'});
  private baseSourceUrl = 'dataquality/source';

  constructor(private http: Http) { }

  /* Source */

  getAllSources(databaseId?: string, page?: number, filter?: string): Promise<SourceResponse> {

    const prameters: Array<Parameter> = [];
    if (databaseId) {
      prameters.push(Parameter.of('db', databaseId));
    }

    if (page >= 0) {
      prameters.push(Parameter.of('page', page));
    }

    if (filter) {
      prameters.push(Parameter.of('filter', filter));
    }

    const prametersString = '?' + URL.encodeQueryData(prameters);

    return this.http.get(`${this.baseSourceUrl}${prametersString}`)
               .toPromise()
               .then(response => response.json() as SourceResponse)
               .catch(this.handleError);
  }


  getSourceIds(filter?: string): Promise<SourceResponse> {
    
        const prameters: Array<Parameter> = [];
    
        if (filter) {
          prameters.push(Parameter.of('filter', filter));
        }
    
        const prametersString = '?' + URL.encodeQueryData(prameters);
    
        return this.http.get(`${this.baseSourceUrl}/list/id${prametersString}`)
                   .toPromise()
                   .then(response => response.json() as SourceResponse)
                   .catch(this.handleError);
      }

  // source -> database

  getDBTableDetails(id: string): Promise<DbTable> {
    return this.http.get(`${this.baseSourceUrl}/database/${id}`)
               .toPromise()
               .then(response => response.json() as DbTable)
               .catch(this.handleError);
  }

  addDBTable(dbt: DbTable): Promise<DbTable> {
    return this.http
      .post(`${this.baseSourceUrl}/database`, JSON.stringify(dbt), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as DbTable)
      .catch(this.handleError);
  }

  updateDBTable(id: string, db: DbTable): Promise<DbTable> {
    return this.http
      .put(`${this.baseSourceUrl}/database/${id}`, JSON.stringify(db), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as DbTable)
      .catch(this.handleError);
  }

  deleteDBTable(id: string): Promise<void> {
    return this.http.delete(`${this.baseSourceUrl}/database/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // source -> hive

  getHiveTableDetails(id: string): Promise<HiveTable> {
    return this.http.get(`${this.baseSourceUrl}/hive/${id}`)
               .toPromise()
               .then(response => response.json() as HiveTable)
               .catch(this.handleError);
  }

  addHiveTable(hivetable: HiveTable): Promise<HiveTable> {
    return this.http
      .post(`${this.baseSourceUrl}/hive`, JSON.stringify(hivetable), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as HiveTable)
      .catch(this.handleError);
  }

  updateHiveTable(id: string, hivetable: HiveTable): Promise<HiveTable> {
    return this.http
      .put(`${this.baseSourceUrl}/hive/${id}`, JSON.stringify(hivetable), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as HiveTable)
      .catch(this.handleError);
  }

  deleteHiveTable(id: string): Promise<void> {
    return this.http.delete(`${this.baseSourceUrl}/hive/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // source -> hdfs

  getFileDetails(id: string): Promise<HdfsFile> {
    return this.http.get(`${this.baseSourceUrl}/hdfs/${id}`)
               .toPromise()
               .then(response => response.json() as HdfsFile)
               .catch(this.handleError);
  }

  // remember that 'separator' filds is a char. (e.g. ';')
  addHdfsFile(hdfsfile: HdfsFile): Promise<HdfsFile> {
    return this.http
      .post(`${this.baseSourceUrl}/hdfs`, JSON.stringify(hdfsfile), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as HdfsFile)
      .catch(this.handleError);
  }

  // not work, test using a #mock
  // remember that 'separator' filds is a char. (e.g. ';')
  updateHdfsFile(id: string, hdfsfile: HdfsFile): Promise<HdfsFile> {
    return this.http
      .put(`${this.baseSourceUrl}/hdfs/${id}`, JSON.stringify(hdfsfile), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as HdfsFile)
      .catch(this.handleError);
  }

  deleteHdfsFile(id: string): Promise<void> {
    return this.http.delete(`${this.baseSourceUrl}/hdfs/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // source -> hdfs -> schema

  getFileSchema(id: string): Promise<FileField> {
    return this.http.get(`${this.baseSourceUrl}/hdfs/${id}/schema`)
               .toPromise()
               .then(response => response.json() as FileField)
               .catch(this.handleError);
  }

  addFileField(id: string, filefiled: FileField): Promise<FileField> {
    return this.http
      .post(`${this.baseSourceUrl}/hdfs/${id}/schema`, JSON.stringify(filefiled), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as FileField)
      .catch(this.handleError);
  }

  deleteFileField(id: string, ffb: FileFiledBase): Promise<void> {
    return this.http.delete(`${this.baseSourceUrl}/hdfs/${id}/schema?fieldName=${ffb.fieldName}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }


    // source -> vrtual source

    getVirtualSourceDetails(id: string): Promise<Virtual> {
      return this.http.get(`${this.baseSourceUrl}/virtual/${id}`)
                 .toPromise()
                 .then(response => response.json() as Virtual)
                 .catch(this.handleError);
    }
  
    addVirtualSources(virtual: Virtual): Promise<Virtual> {
      return this.http
        .post(`${this.baseSourceUrl}/virtual`, JSON.stringify(virtual), {headers: this.headers})
        .toPromise()
        .then(res => res.json() as Virtual)
        .catch(this.handleError);
    }
  
    updateVirtualSource(id: string, virtual: Virtual): Promise<Virtual> {
      return this.http
        .put(`${this.baseSourceUrl}/virtual/${id}`, JSON.stringify(virtual), {headers: this.headers})
        .toPromise()
        .then(res => res.json() as HiveTable)
        .catch(this.handleError);
    }
  
    deleteVirtualSource(id: string): Promise<void> {
      return this.http.delete(`${this.baseSourceUrl}/${id}`, {headers: this.headers})
        .toPromise()
        .then(() => null)
        .catch(this.handleError);
    }
  

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
