import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import { DialogAlert } from '../common/components/dq-dialogs';
import * as metrics from '../models/metrics';
import { MetricsService } from '../services/metrics.service';
import { MetasService } from '../services/metas.service';
import { MetaParameter, ParamItem } from '../models/metas';
import { InteractionsService } from '../services/interactions.service';
import { ErrorManager } from '../common/error.manager';
import { DialogConfirmYESNO, DialogConfirmYESNOUtils } from '../common/components/dq-dialog-yesno';
import { SearchService } from '../services/search.service'

enum Type {
  COLUMN = 'COLUMN',
  FILE = 'FILE',
  COMPOSED = 'COMPOSED'
};

class Field {
  name: string;
  value?: string;

  constructor(name: string, value?: string) {
    this.name = name;
    this.value = value || null;
  }
}



abstract class MetricFe<T> {
  isNew: boolean;
  data: T;
  constructor(metric: T) {
    this.data = metric;
  }
}


class MetricColumnFe extends MetricFe<metrics.MetricColumn> {


  paramFields: Array<ParamItem> = [];
  
  constructor(columnMetric: metrics.MetricColumn) {
    super(columnMetric);  
  }

  get parametersValue() {
    return JSON.stringify(this.data.parameters, null, 2);
  }

  set parametersValue(parameters: string) {
   try {
    this.data.parameters = JSON.parse(parameters);
   }catch {
     // parsing error during editing
     return;
   }
  }

  refreshParamFields() {
    const currMetricTypeParams = this.paramFields.find(p => p.metricType === this.data.name)
    if (currMetricTypeParams && this.data.parameters.length > 0) {
      currMetricTypeParams.parameters = this.data.parameters.map(p => p as MetaParameter)
    }
  }

  resetParamFields() {
    this.paramFields
    .filter(p => p.parameters !== undefined)
    .forEach((paramField) => {
      paramField.parameters.forEach((field) => {
        field.value = null;
      })
    })
  }

  refreshParams() {
    this.data.parameters = this.paramFields
                                .filter(p => p.parameters !== undefined) // filter out all empty prameters
                                .map(p => p.parameters).reduce((result, current) => {
                                  return result.concat(current)
                                }, [])
                                .filter(p => p.value !== null)
  }

  get column1() {
    return this.data.columns[0]
  }

  set column1(column: string) {
    (<Array<string>>this.data.columns)[0] = column;
  }

  get column2() {
    return this.data.columns[1]
  }

  set column2(column: string) {
    (<Array<string>>this.data.columns)[1] = column;
  }
  
}

class MetricFileFe extends MetricFe<metrics.MetricFile> {
  constructor(fileMetric: metrics.MetricFile) {
    super(fileMetric);
  }
}



@Component({
  selector: 'app-metrics',
  templateUrl: './metrics.component.html',
  styleUrls: ['./metrics.component.css']
})
export class MetricsComponent implements OnInit {

  @Output() onOpen = new EventEmitter<string>();
  @Output() onClose = new EventEmitter<string>();

  fileMetricDescriptors: string[] = [];
  columnMetricDescriptors: string[] = []; 

 multipleColumMetrics = [
  'LEVENSHTEIN_DISTANCE',
  'DAY_DISTANCE',
  'COLUMN_EQ'
 ];

  columnMetricSearch: string;
  fileMetricSearch: string;

  // current metrics source related
  sourceId: string;

  Type = Type;

  columnMetrics: Array<MetricColumnFe> = [];
  fileMetrics: Array<MetricFileFe> = [];

  columnMetricsElements: number;
  fileMetricsElements: number;

  errorValidation: Array<any> = [];

  constructor(
    public metricsService: MetricsService,
    public metasService: MetasService,
    public searchService: SearchService,
    public interactionsService: InteractionsService,
    public dialog: MdDialog,
    public snackBar: MdSnackBar) {}


  open(metricId: string) {
    this.interactionsService.setMetricId(metricId);
    this.onOpen.emit('checks');
  }

  close() {
    this.restoreValidation();
    this.onClose.emit('metrics');
  }

  getMetrics(sourceId: string) {
    // get the first page of each metric type
    this.getPagedMetrics(Type.COLUMN, sourceId, 0)
    this.getPagedMetrics(Type.FILE, sourceId, 0)
  }

  getPagedMetrics(metricType: Type, sourceId?: string, page?: number): void {
    this.metricsService.getAllMetrics(sourceId, page, metricType).then(metricsRequest => {
      const metrics = metricsRequest.metrics;
      switch (metricType) {
        case Type.COLUMN: {
          this.columnMetrics =  [];
          this.columnMetricsElements = metricsRequest.count
          metrics.forEach(columnMetrics => {
            this.metricsService.getColumnMetricDetails(columnMetrics.id).then(columnMetricsDetails => {
             const colMetric = new MetricColumnFe(columnMetricsDetails);
             this.getParamField().then(params => {
              colMetric.paramFields = params
              colMetric.refreshParamFields();
              this.columnMetrics.push(colMetric);
             }
            )
            });
          });
        } break;
        case Type.FILE: {
          this.fileMetrics =  [];
          this.fileMetricsElements = metricsRequest.count
          metrics.forEach(fileMetric => {
            this.metricsService.getMetricDetails(fileMetric.id).then(fileMetricDetails => {
              this.fileMetrics.push(new MetricFileFe(fileMetricDetails));
            });
          });
        } break;
      }
    });
  }

  addMetric(metricType: string) {
    if (metricType === Type.COLUMN) {
      const newColumnMetric = new MetricColumnFe(new metrics.MetricColumn('NEW METRIC'));
      newColumnMetric.isNew = true;
      this.getParamField().then(params => {
              newColumnMetric.paramFields = params
      });
      this.columnMetrics.push(newColumnMetric);
    }
    if (metricType === Type.FILE) {
      const newFileMetric = new MetricFileFe(new metrics.MetricFile('NEW METRIC'));
      newFileMetric.isNew = true;
      this.fileMetrics.push(newFileMetric);
    }
  }

  createMetric(metricType: string, metric: any) {
    this.restoreValidation();

    // update static metric fields
    metric.data.mType = metricType
    metric.data.source = this.sourceId;
    if (metricType === Type.COLUMN) {
      metric.refreshParams();
      this.metricsService.addColumnMetric(metric.data).then(response => {
        metric.isNew = false;
        this.snackBarMessage('Metric Created!');
      })
      .catch(err => this.errorManager(err));
    }
    if (metricType === Type.FILE) {
      this.metricsService.addFileMetric(metric.data).then(response => {
        metric.isNew = false;
        this.snackBarMessage('Metric Created!');
      })
      .catch(err => this.errorManager(err));
    }
    if (metricType === Type.COMPOSED) {
      this.metricsService.addComposedColumnMetric(metric.data).then(response => {
        metric.isNew = false;
        this.snackBarMessage('Metric Created!');
      })
      .catch(err => this.errorManager(err));
    }
  }

  updateMetric(metricType: string, metric: any) {
    this.restoreValidation();

    if (metricType === Type.COLUMN) {
      metric.refreshParams();
      this.metricsService.updateColumnMetric(metric.data.id, metric.data)
        .then(() => this.snackBarMessage('Metric Updated!'))
        .catch(err => this.errorManager(err));
    }
    if (metricType === Type.FILE) {
      this.metricsService.updateMetric(metric.data.id, metric.data)
        .then(() => this.snackBarMessage('Metric Updated!'))
        .catch(err => this.errorManager(err));
    }
    if (metricType === Type.COMPOSED) {
      this.metricsService.updateComposedColumnMetric(metric.data.id, metric.data)
        .then(() => this.snackBarMessage('Metric Updated!'))
        .catch(err => this.errorManager(err));
    }
  }


  deleteMetric(metricType: string, metric: any) {
    this.restoreValidation();

    DialogConfirmYESNOUtils.areYouSure(
      this.dialog,
      () => {
              if (metric.isNew) {
                this.columnMetrics = this.columnMetrics.filter(m => m.data.id !== metric.data.id);
                this.fileMetrics = this.fileMetrics.filter(m => m.data.id !== metric.data.id);
              } else {
                if (metricType === Type.COLUMN) {
                  this.metricsService.deleteColumnMetric(metric.data.id).then(() => {
                    this.columnMetrics = this.columnMetrics.filter(m => m.data.id !== metric.data.id);
                  }
                  ).catch(error => this.deleteWarning());
                }
                if (metricType === Type.FILE) {
                  this.metricsService.deleteMetric(metric.data.id).then(() => {
                    this.fileMetrics = this.fileMetrics.filter(m => m.data.id !== metric.data.id);
                  }).catch(error => this.deleteWarning());
                }
              }
    });
  }

  getMetricDescriptors() {
    this.metasService.getMetaEntityIds('metric', 'file').then(metricMetasResponse => {
      this.fileMetricDescriptors = metricMetasResponse
    });

    this.metasService.getMetaEntityIds('metric', 'column').then(metricMetasResponse => {
      this.columnMetricDescriptors = metricMetasResponse
    });
  }

  deleteWarning() {
    const dialogRef = this.dialog.open(DialogAlert, {
      data: 'You can not delete a source with bounded metrics'
    });
  }

  getParamField(): Promise<ParamItem[]> {
    return this.metasService.getMetaParameters('metric').then(parameterMetas => {
      return parameterMetas;
    })
  }

  getSearchResults(metricType: string, page?: number) {
    switch (metricType) {
      case Type.COLUMN: {
        this.searchService
        .search('metric', this.columnMetricSearch, 'source', this.sourceId, page)
        .then(searchResult => {
          this.columnMetricsElements = searchResult.size;
          this.columnMetrics = [];
          searchResult.results.forEach(searchId => {
            this.metricsService.getColumnMetricDetails(searchId).then(columnMetricsDetails => {
              const colMetric = new MetricColumnFe(columnMetricsDetails);
              this.getParamField().then(params => {
               colMetric.paramFields = params
               colMetric.refreshParamFields();
               this.columnMetrics.push(colMetric);
              }
             )
             });
          })
        })
      } break;
      case Type.FILE: {
        this.searchService
        .search('metric', this.fileMetricSearch, 'source', this.sourceId, page)
        .then(searchResult => {
          this.fileMetricsElements = searchResult.size;
          this.fileMetrics = []
          searchResult.results.forEach(searchId => {
            this.metricsService.getMetricDetails(searchId).then(fileMetricDetails => {
              this.fileMetrics.push(new MetricFileFe(fileMetricDetails));
            });
          })
        })
      } break;
    }
  }

  /**called when search input is changed */
  inputChange(metricType: Type) {
    this.getSearchResults(metricType);
  }

  /**
   * Clear Search and show all result
   */
  clearSearch(metricType: Type) {
    // clear search varaible
    switch (metricType) {
      case Type.COLUMN: {
        this.columnMetricSearch = ''
      } break;
      case Type.FILE: {
        this.fileMetricSearch = ''
      } break;
    }

    // show first page elements
    this.getPagedMetrics(metricType, this.sourceId, 0)
  }

  ngOnInit() {
    this.sourceId = this.interactionsService.getSourceId();
    this.getMetricDescriptors();
    this.getMetrics(this.sourceId);
  }

  errorManager(err: any) {
    const errors: Array<any> = ErrorManager.prepareErrorMessage(err);
    this.errorValidation = [];
    for (const entry of errors) {
      // parameter errors have no key on returned error json
        if (entry.field === '') {
            for ( const paramError of entry.message) {
              this.errorValidation.push({
                  field: 'paramError',
                  message: paramError
              })
            }
        } else {
          this.errorValidation.push(entry)
        }
    }
    
  }

  restoreValidation() {
    this.errorValidation = [];
  }

  snackBarMessage(message: string, action?: string) {
    this.snackBar.open(message, action || 'Done!', {
      duration: 2000
    });
  }

  pageChange(pageEvent: any, metricType: Type) {

      let searchTerm = '';
      switch (metricType) {
        case Type.COLUMN: {
          searchTerm = this.columnMetricSearch;
        } break;
        case Type.FILE: {
          searchTerm = this.fileMetricSearch;
        } break;
      }

      // search term not empty
      if (searchTerm) {
        this.getSearchResults(metricType, pageEvent.pageIndex)
      } else {
        this.getPagedMetrics(metricType, this.sourceId, pageEvent.pageIndex)
      }
     
  }

  metricTypeChanged(event: any, metric: any) {
    metric.data.parameters = []
    metric.resetParamFields();
  }

}
