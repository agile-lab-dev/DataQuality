import { Component, EventEmitter, Input, Output, OnInit, Inject} from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import * as checks from '../models/checks';
import * as targets from '../models/targets';
import { ChecksService } from '../services/checks.service';
import { DatabaseService } from '../services/databases.service'
import { TargetsService } from '../services/targets.service';
import { InteractionsService } from '../services/interactions.service';
import { MetasService } from '../services/metas.service';
import { TargetDialogComponent } from '../targets.dialog/targets-dialog.component';
import { MetricsService } from '../services/metrics.service';
import { ErrorManager } from '../common/error.manager';
import { DialogConfirmYESNO, DialogConfirmYESNOUtils } from '../common/components/dq-dialog-yesno';
import { SearchService } from '../services/search.service'

/** Classes and more */

enum Type {
    SQL = 'sql',
    SNAPSHOT = 'snapshot',
    TREND = 'trend'
};

// TODO if is the same for all entities, put it in a common file.
abstract class CheckFe<T> {
    isNew: boolean;
    data: T;
    targetIds: string[];
    constructor(source?: T, targetsId?: string[]) {
        this.data = source;
        this.targetIds = targetsId || [];
    }
}


class CheckSnapshotFE extends CheckFe<checks.CheckSnapshot> {

    activeGroupValue: string;

    constructor(snapshotCheck?: checks.CheckSnapshot, targetsId?: string[]) {
        super(snapshotCheck, targetsId);
    }

    get compareMetric() {
        const cM = this.data.parameters.find(p => p.name === 'compareMetric');
        if (cM) {
            return cM.value;
        }
        return null;
    }

    set compareMetric(compareMetric: any) {
        const cM = this.data.parameters.find(p => p.name === 'compareMetric');
        if (cM) {
             cM.value = compareMetric;
        } else {
            this.data.parameters.push({name: 'compareMetric', value: compareMetric})
        }
    }

    get threshold() {
        const threshold = this.data.parameters.find(p => p.name === 'threshold');
        if (threshold) {
            return threshold.value;
        }
        return null;
    }

    get groupValue() {
        this.activeGroupValue = 'metric'
        if (this.data.parameters.find(p => p.name === 'threshold') && this.data.subtype !== 'DIFFER_BY_LT') {
            this.activeGroupValue = 'threshold';
        }
        return this.activeGroupValue;
    }

    set threshold(thresholdValue: any) {
        const threshold = this.data.parameters.find(p => p.name === 'threshold');
        if (threshold) {
            threshold.value = thresholdValue;
        } else {
            this.data.parameters.push({name: 'threshold', value: thresholdValue})
        }
    }

    get parametersValue() {
        return JSON.stringify(this.data.parameters, null, 2);
    }
    
    set parametersValue(parameters: string) {
        try {
            this.data.parameters = JSON.parse(parameters);
        } catch {
            // parsing error during editing
            return;
        }
    }

    refreshParams() {
        // for server side validation only one parameter between
        // metric parameter or threshold parameter is permitted
        if (this.activeGroupValue === 'metric') {
            this.data.parameters = this.data.parameters.filter(p => p.name !== 'threshold')
        } else if (this.activeGroupValue === 'threshold') {
            this.data.parameters = this.data.parameters.filter(p => p.name !== 'compareMetric')
        }
    }
}

class CheckTrendFE extends CheckFe<checks.CheckTrend> {
    constructor(trendCheck?: checks.CheckTrend, targetsId?: string[]) {
        super(trendCheck, targetsId);
    }

    get threshold() {
        const threshold = this.data.parameters.find(p => p.name === 'threshold');
        if (threshold) {
            return threshold.value;
        }
        return null;
    }

    set threshold(thresholdValue: any) {
        const threshold = this.data.parameters.find(p => p.name === 'threshold');
        if (threshold) {
            threshold.value = thresholdValue;
        } else {
            this.data.parameters.push({name: 'threshold', value: thresholdValue})
        }
    }

    get timeWindow() {
        const timewindow = this.data.parameters.find(p => p.name === 'timewindow');
        if (timewindow) {
            return timewindow.value;
        }
        return null;
    }

    set timeWindow(timeWindowValue: any) {
        const timewindow = this.data.parameters.find(p => p.name === 'timewindow');
        console.log(timeWindowValue)
        if (timewindow) {
            timewindow.value = timeWindowValue;
        } else {
            this.data.parameters.push({name: 'timewindow', value: timeWindowValue})
        }
    }

    get targetNumber() {
        const targetNumber = this.data.parameters.find(p => p.name === 'targetNumber');
        if (targetNumber) {
            return targetNumber.value;
        }
        return null;
    }

    set targetNumber(targetNumberValue: any) {
        const targetNumber = this.data.parameters.find(p => p.name === 'targetNumber');
        if (targetNumber) {
            targetNumber.value = targetNumberValue;
        } else {
            this.data.parameters.push({name: 'targetNumber', value: targetNumberValue})
        }
    }

    get startDate() {
        const startDate = this.data.parameters.find(p => p.name === 'startDate');
        if (startDate) {
            return startDate.value;
        }
        return null;
    }

    set startDate(startDateValue: any) {
        const targetNumber = this.data.parameters.find(p => p.name === 'startDate');
        console.log(startDateValue);
        if (targetNumber) {
            targetNumber.value = startDateValue
        } else {
            this.data.parameters.push({name: 'startDate', value: startDateValue})
        }
    }


}


@Component({
  selector: 'app-checks',
  templateUrl: './checks.component.html',
  styleUrls: ['./checks.component.css']
})
export class ChecksComponent implements OnInit {

    @Output() onOpen = new EventEmitter<string>();
    @Output() onClose = new EventEmitter<string>();

    checkTypes =  ['sql', 'snapshot', 'trend'];
    snapshotCheckSubTypes: string[];

    rules = ['date', 'record'];

    trendCheckSubTypes: string[];

    /* sqlChecks: Array<CheckSqlFE> = []; */
    snapshotChecks: Array<CheckSnapshotFE> = [];
    trendChecks: Array<CheckTrendFE> = [];

    snapshotCheckElements: number;
    trendCheckElements: number;

    snapshotCheckSearch: string;
    trendCheckSearch: string;

    databaseIds: string[];
    @Input() metricId: string;

    // current check targets list
    currentCheckTargets: string[] = [];

    // metrics of current active source
    compareMetrics: string[];

    errorValidation: Array<any> = [];

    constructor(
        private checkService: ChecksService,
        public metasService: MetasService,
        private databasesService: DatabaseService,
        private targetService: TargetsService,
        private interactionsService: InteractionsService,
        private metricsService: MetricsService,
        public searchService: SearchService,
        private dialog: MdDialog,
        public snackBar: MdSnackBar
    ) {}

    open(checkId: string) {
        this.interactionsService.setCheckId(checkId);
        this.onOpen.emit('targets');
    }

    close() {
        this.restoreValidation();
        this.onClose.emit('checks');
    }

    getChecks(metricId: string): void {
        this.getPagedChecks(Type.SNAPSHOT, metricId, 0)
        this.getPagedChecks(Type.TREND, metricId, 0)
    };

    getPagedChecks(checkType: Type, metricId?: string, page?: number): void {
        this.checkService.getAllChecks(null, metricId, page, checkType).then(checksRequest => {
          const checks = checksRequest.checks;
          switch (checkType) {
            case Type.SNAPSHOT: {
              this.snapshotChecks =  [];
              this.snapshotCheckElements = checksRequest.count
              checks.forEach(snapshotCheck => {
                this.checkService.getSnapshotCheckDetails(snapshotCheck.id).then(snapshotCheckDetails => {
                    this.targetService.getAllTargets(snapshotCheck.id).then(t => {
                        const targetsId = t.targets.filter(tr => tr.targetType === 'SYSTEM' ).map(ts => ts.id);
                        this.snapshotChecks.push(new CheckSnapshotFE(snapshotCheckDetails, targetsId));
                    });
                });
            });

            } break;
            case Type.TREND: {
              this.trendChecks =  [];
              this.trendCheckElements = checksRequest.count
              checks.forEach(trendCheck => {
                this.checkService.getTrendCheckDetails(trendCheck.id).then(trendCheckDetails => {
                    this.targetService.getAllTargets(trendCheckDetails.id).then(t => {
                        const targetsId = t.targets.filter(tr => tr.targetType === 'SYSTEM' ).map(ts => ts.id);
                        this.trendChecks.push(new CheckTrendFE(trendCheckDetails, targetsId));
                    });
                });
            });
            } break;
          }
        });
      }
    

    updateCheck(checkType: string, check: any) {
        this.restoreValidation();

        if (checkType === Type.SNAPSHOT) {
            console.log(check)
            check.refreshParams();
            this.checkService.updateSnapshotCheck(check.data.id, check.data).then(updateCheck => {

                let deletePromises: Promise<void>[] = [];
                // remove current check from targets
                this.currentCheckTargets.forEach(tId => {
                    deletePromises.push(this.targetService.deleteTargetCheck(tId, new targets.Check(updateCheck.id)));
                })
                // wait untill all target are updated
                Promise.all(deletePromises).then(result => {
                     // add current check to targets
                    (<string[]>check.targetIds).forEach(tId => {
                        this.targetService.addTargetCheck(tId, new targets.Check(updateCheck.id)).then();
                    });
                });
                this.snackBarMessage('Check Updated!');
            })
            .catch(err => this.errorManager(err));
        }
        if (checkType === Type.TREND) {
            this.checkService.updateTrendCheck(check.data.id, check.data).then(updateCheck => {
               
                let deletePromises: Promise<void>[] = [];
                // remove current check from targets
                this.currentCheckTargets.forEach(tId => {
                    deletePromises.push(this.targetService.deleteTargetCheck(tId, new targets.Check(updateCheck.id)));
                });
                // wait untill all target are updated
                Promise.all(deletePromises).then(result => {
                     // add current check to targets
                    (<string[]>check.targetIds).forEach(tId => {
                        this.targetService.addTargetCheck(tId, new targets.Check(updateCheck.id)).then();
                    });
                });
                this.snackBarMessage('Check Updated!');
            })
            .catch(err => this.errorManager(err));
        }
    }

    deleteCheck(checkType: string, check: any) {
        this.restoreValidation();

        DialogConfirmYESNOUtils.areYouSure(
            this.dialog,
            () => {
                    if (check.isNew) {
                        this.snapshotChecks = this.snapshotChecks.filter(c => c.data.id !== check.data.id);
                        this.trendChecks = this.trendChecks.filter(c => c.data.id !== check.data.id);
                    } else {
                        if (checkType === Type.SNAPSHOT) {
                            this.checkService.deleteSnapshotCheck(check.data.id).then(() => {
                            this.snapshotChecks = this.snapshotChecks.filter(c => c.data.id !== check.data.id);
                            })
                            .catch(err => this.errorManager(err));
                        }
                        if (checkType === Type.TREND) {
                                this.checkService.deleteTrendCheck(check.data.id).then(() => {
                                this.trendChecks = this.trendChecks.filter(c => c.data.id !== check.data.id);
                            })
                            .catch(err => this.errorManager(err));
                        }
                    }
        });
    }

    createCheck(checkType: string, check: any) {
        if (checkType === Type.SNAPSHOT) {
            check.refreshParams();
            this.checkService.addSnapshotCheck(check.data).then(response => {
                check.isNew = false;
                this.snackBarMessage('Check Created!');
            });
        }
        if (checkType === Type.TREND) {
                this.checkService.addTrendCheck(check.data).then(response => {
                check.isNew = false;
                this.snackBarMessage('Check Created!');
            });
        }
    }

    addCheck(checkType: string) {
        if (checkType === Type.SNAPSHOT) {
            const newSnapshotCheck = new CheckSnapshotFE(new checks.CheckSnapshot('NEW SNAPSHOT', Type.SNAPSHOT, '', '', this.metricId, []));
            newSnapshotCheck.isNew = true;
            this.snapshotChecks.push(newSnapshotCheck);
        }
        if (checkType === Type.TREND) {
            const newTrendCheck = new CheckTrendFE(new checks.CheckTrend('NEW TREND', Type.TREND, '', '', this.metricId, [], ''));
            newTrendCheck.isNew = true;
            this.trendChecks.push(newTrendCheck);
        }
    }

    getDatabaseIds() {
        this.databasesService.getDatabaseIds().then(dbResponse => {
            this.databaseIds = <string[]>dbResponse.databases;
        });
    }

    openTargetsDialog(check: any) {
        this.currentCheckTargets = check.targetIds;
        const dialogRef = this.dialog.open(
            TargetDialogComponent,
            {
                disableClose: true,
                data: check.targetIds
            }
        );

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                check.targetIds = result;
            }
        });
    }

    loadMetrics() {
        this.compareMetrics = [];
        // load all metrics of current source
        this.metricsService.getAllMetrics(this.interactionsService.sourceId).then(metricResponse => {
            this.compareMetrics = metricResponse.metrics.map(m => m.id);
        })
    }


    getChecksSubtypes() {
        this.metasService.getMetaEntityIds('check', 'trend').then(checkMetasResponse => {
          this.trendCheckSubTypes = checkMetasResponse
        });
    
        this.metasService.getMetaEntityIds('check', 'snapshot').then(checkMetasResponse => {
          this.snapshotCheckSubTypes = checkMetasResponse
        });
      }


      getSearchResults(metricType: string, page?: number) {
        switch (metricType) {
          case Type.SNAPSHOT: {
            this.searchService
            .search('check', this.snapshotCheckSearch, 'metric', this.metricId, page)
            .then(searchResult => {
              this.snapshotCheckElements = searchResult.size;
              this.snapshotChecks = [];
              searchResult.results.forEach(searchId => {
                this.checkService.getSnapshotCheckDetails(searchId).then(snapshotCheckDetails => {
                    this.targetService.getAllTargets(searchId).then(t => {
                        const targetsId = t.targets.filter(tr => tr.targetType === 'SYSTEM' ).map(ts => ts.id);
                        this.snapshotChecks.push(new CheckSnapshotFE(snapshotCheckDetails, targetsId));
                    });
                });
              })
            })
          } break;
          case Type.TREND: {
            this.searchService
            .search('check', this.trendCheckSearch, 'metric', this.metricId, page)
            .then(searchResult => {
              this.trendCheckElements = searchResult.size;
              this.trendChecks = []
              searchResult.results.forEach(searchId => {
                this.checkService.getTrendCheckDetails(searchId).then(trendCheckDetails => {
                    this.targetService.getAllTargets(trendCheckDetails.id).then(t => {
                        const targetsId = t.targets.filter(tr => tr.targetType === 'SYSTEM' ).map(ts => ts.id);
                        this.trendChecks.push(new CheckTrendFE(trendCheckDetails, targetsId));
                    });
                });
              })
            })
          } break;
        }
      }
    
      /**called when search input is changed */
      inputChange(checkType: Type) {
        
        let isSearchEmty = false;
        switch (checkType) {
          case Type.SNAPSHOT: {
            isSearchEmty = this.snapshotCheckSearch === '';
          } break;
          case Type.TREND: {
            isSearchEmty = this.trendCheckSearch === '';
          } break;
        }

        if (isSearchEmty) {
            this.getPagedChecks(checkType, this.metricId, 0);
        } else {
            this.getSearchResults(checkType);
        }
        
      }
    
      /**
       * Clear Search and show all result
       */
      clearSearch(checkType: Type) {
        // clear search varaible
        switch (checkType) {
          case Type.SNAPSHOT: {
            this.snapshotCheckSearch = ''
          } break;
          case Type.TREND: {
            this.trendCheckSearch = ''
          } break;
        }
    
        // show first page elements
        this.getPagedChecks(checkType, this.metricId, 0)
      }
    

    ngOnInit() {
        console.log(this.metricId);
        this.metricId = this.metricId || this.interactionsService.getMetricId();
        this.getChecks(this.metricId);
        this.getChecksSubtypes();
        this.getDatabaseIds();
        this.loadMetrics();
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

    pageChange(pageEvent: any, checkType: Type) {

      let searchTerm = '';
      switch (checkType) {
        case Type.SNAPSHOT: {
          searchTerm = this.snapshotCheckSearch;
        } break;
        case Type.TREND: {
          searchTerm = this.trendCheckSearch;
        } break;
      }

      // search term not empty
      if (searchTerm) {
        this.getSearchResults(checkType, pageEvent.pageIndex)
      } else {
        this.getPagedChecks(checkType, this.metricId, pageEvent.pageIndex)
      }
    }

      toggleChange(changeEvent: any, check: any) {
        const ch = this.snapshotChecks.find(c => c.data.id === check.data.id)
        ch.activeGroupValue = changeEvent.value
        ch.refreshParams();
      }
}
