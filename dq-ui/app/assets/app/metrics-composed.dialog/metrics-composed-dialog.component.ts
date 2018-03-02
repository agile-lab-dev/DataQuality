import { Component, Inject, OnInit } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import { Database } from '../models/databases'
import { CheckSql } from '../models/checks'
import { MetricComposed } from '../models/metrics';
import { MetricsService } from '../services/metrics.service';
import { ErrorManager } from '../common/error.manager';
import { SearchService } from '../services/search.service'


class MetricComposedFe {
  isNew: boolean;
  data: MetricComposed;
  constructor(metric: MetricComposed) {
    this.data = metric;
  }
}

@Component({
  selector: 'metrics-composed-dialog',
  templateUrl: './metrics-composed-dialog.component.html',
  styleUrls: ['./metrics-composed-dialog.component.css']
})
export class MetricsComposedDialogComponent implements OnInit {

  showChecks: boolean;

  composedMetrics: Array<MetricComposedFe> = []

  composedMetricElements: number;

  errorValidation: Array<any> = [];

  activeComposedMetricId: string;

  composedMetricSearch: string;

  constructor(
    public dialogRef: MdDialogRef<MetricsComposedDialogComponent>,
    @Inject(MD_DIALOG_DATA) public data: any, 
    public metricsService: MetricsService,
    private searchService: SearchService,
    public snackBar: MdSnackBar
  ) { };


  getMetrics() {
    this.getPagedMetrics('COMPOSED', 0)
  }

  getPagedMetrics(metricType?: string, page?: number) {
    this.metricsService.getAllMetrics(null, page, metricType).then(metricsRequest => {
      const metrics = metricsRequest.metrics;
      this.composedMetrics = []
      this.composedMetricElements = metricsRequest.count;
       metrics.forEach(composedMetric => {
         this.metricsService.getComposedMetricDetails(composedMetric.id).then(composedMetricDetails => {
             this.composedMetrics.push(new MetricComposedFe(composedMetricDetails));
         });
       });
    });
  }

  addMetric(metricType: string) {
    if (metricType === 'COMPOSED') {
      const newComposedMetric = new MetricComposedFe(new MetricComposed('NEW METRIC', 'COMPOSED', ''))
      newComposedMetric.isNew = true;
      this.composedMetrics.push(newComposedMetric);
    }
  }

  createMetric(metricType: string, metric: any) {
    this.restoreValidation();
    if (metricType === 'COMPOSED') {
      this.metricsService.addComposedColumnMetric(metric.data).then(response => {
        metric.isNew = false;
        this.snackBarMessage('Composed Metric Created');
      })
      .catch(err => this.errorManager(err));
    }
  }

  updateMetric(metricType: string, metric: any) {
    this.restoreValidation();

    if (metricType === 'COMPOSED') {
      this.metricsService.updateComposedColumnMetric(metric.data.id, metric.data)
        .then(r => this.snackBarMessage('Composed Metric Updated'))
        .catch(err => this.errorManager(err));
    }
  }


  deleteMetric(metricType: string, metric: any) {
    if (metric.isNew) {
      this.composedMetrics = this.composedMetrics.filter(m => m.data.id !== metric.data.id);
    } else {
      if (metricType === 'COMPOSED') {
        this.metricsService.deleteComposedColumnMetric(metric.data.id).then(() => {
          this.composedMetrics = this.composedMetrics.filter(m => m.data.id !== metric.data.id);
        });
      }
    }

  }

  getSearchResults(metricType: string, page?: number) {
        this.searchService
        .search('metric', this.composedMetricSearch, null, null, page)
        .then(searchResult => {
          this.composedMetricElements = searchResult.size
          this.composedMetrics = [];
          searchResult.results.forEach(searchId => {
            this.metricsService.getComposedMetricDetails(searchId).then(composedMetricDetails => {
              this.composedMetrics.push(new MetricComposedFe(composedMetricDetails));
          });
          })
        })
    }

  /**called when search input is changed */
  inputChange() {
    this.getSearchResults('COMPOSED');
  }

  /**
   * Clear Search and show all result
   */
  clearSearch() {
    // clear search varaible
    this.composedMetricSearch = ''

    // show first page elements
    this.getPagedMetrics('COMPOSED', 0)
  }


  closeComposedMetricsView() {
    this.dialogRef.close();
  }

  openChecks() {
    // show current composed metric checks
    this.showChecks = true;
  }

  openMetric(metric: MetricComposedFe) {
    this.activeComposedMetricId = metric.data.id;
    this.showChecks = false;
  }

  ngOnInit() {
    this.getMetrics();
  }

  errorManager(err: any) {
    this.errorValidation = ErrorManager.prepareErrorMessage(err);
  }

  restoreValidation() {
    this.errorValidation = [];
  }

  snackBarMessage(message: string, action?: string) {
    this.snackBar.open(message, action || 'Done!', {
      duration: 2000
    });
  }

  pageChange(pageEvent: any) {

    // search term not empty
    if (this.composedMetricSearch) {
      this.getSearchResults('COMPOSED', pageEvent.pageIndex)
    } else {
      this.getPagedMetrics('COMPOSED', pageEvent.pageIndex)
    }
  }

}
