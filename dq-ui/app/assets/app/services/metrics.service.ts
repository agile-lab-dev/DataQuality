import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import { MetricResponse, Metric, MetricFile, MetricColumn, MetricParameterBase, MetricComposed } from '../models/metrics';
import { URL, Parameter } from '../common/url';

@Injectable()
export class MetricsService {
  private headers = new Headers({'Content-Type': 'application/json'});
  private baseMetricUrl = 'dataquality/metric';

  constructor(private http: Http) { }

  /* Metrics */

  getAllMetrics(sourceId?: string, page?: number, filter?: string): Promise<MetricResponse> {

    const prameters: Array<Parameter> = [];
    if (sourceId) {
      prameters.push(Parameter.of('src', sourceId));
    }

    if (page >= 0) {
      prameters.push(Parameter.of('page', page));
    }

    if (filter) {
      prameters.push(Parameter.of('filter', filter));
    }

    const prametersString = '?' + URL.encodeQueryData(prameters);

    
    return this.http.get(`${this.baseMetricUrl}${prametersString}`)
      .toPromise()
      .then(response => response.json() as MetricResponse)
      .catch(this.handleError);
  }

  // metric -> file

  getMetricDetails(id: string): Promise<MetricFile> {
    return this.http.get(`${this.baseMetricUrl}/file/${id}`)
               .toPromise()
               .then(response => response.json() as MetricFile)
               .catch(this.handleError);
  }

  addFileMetric(mf: MetricFile): Promise<MetricFile> {
    return this.http
      .post(`${this.baseMetricUrl}/file`, JSON.stringify(mf), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as MetricFile)
      .catch(this.handleError);
  }

  updateMetric(id: string, mf: MetricFile): Promise<void> {
    return this.http
      .put(`${this.baseMetricUrl}/file/${id}`, JSON.stringify(mf), {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  deleteMetric(id: string): Promise<void> {
    return this.http.delete(`${this.baseMetricUrl}/file/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // metric -> column

  getColumnMetricDetails(id: string): Promise<MetricColumn> {
    return this.http.get(`${this.baseMetricUrl}/column/${id}`)
               .toPromise()
               .then(response => response.json() as MetricColumn)
               .catch(this.handleError);
  }

  addColumnMetric(mc: MetricColumn): Promise<MetricColumn> {
    return this.http
      .post(`${this.baseMetricUrl}/column`, JSON.stringify(mc), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as MetricColumn)
      .catch(this.handleError);
  }

  updateColumnMetric(id: string, mc: MetricColumn): Promise<void> {
    return this.http
      .put(`${this.baseMetricUrl}/column/${id}`, JSON.stringify(mc), {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  deleteColumnMetric(id: string): Promise<void> {
    return this.http.delete(`${this.baseMetricUrl}/column/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // metric -> column -> param

  addMetricParameter(id: string, mpb: MetricParameterBase): Promise<void> {
    return this.http
      .post(`${this.baseMetricUrl}/column/${id}/param`, JSON.stringify(mpb), {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  deleteColumnParamMetric(id: string, mp: MetricParameterBase): Promise<void> {
    return this.http.delete(`${this.baseMetricUrl}/column/${id}/param?name=${mp.name}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  // metric -> composed

  getComposedMetricDetails(id: string): Promise<MetricComposed> {
    return this.http.get(`${this.baseMetricUrl}/composed/${id}`)
               .toPromise()
               .then(response => response.json() as MetricComposed)
  }

  addComposedColumnMetric(mc: MetricComposed): Promise<MetricComposed> {
    return this.http
      .post(`${this.baseMetricUrl}/composed`, JSON.stringify(mc), {headers: this.headers})
      .toPromise()
      .then(res => res.json() as MetricComposed)
      .catch(this.handleError);
  }

  updateComposedColumnMetric(id: string, mc: MetricComposed): Promise<void> {
    return this.http
      .put(`${this.baseMetricUrl}/composed/${id}`, JSON.stringify(mc), {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  deleteComposedColumnMetric(id: string): Promise<void> {
    return this.http.delete(`${this.baseMetricUrl}/composed/${id}`, {headers: this.headers})
      .toPromise()
      .then(() => null)
      .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
}
