import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import * as sources from '../models/sources';
import { SourcesService } from '../services/sources.service';
import { DatabaseService } from '../services/databases.service';
import { InteractionsService } from '../services/interactions.service';
import { DialogAlert } from '../common/components/dq-dialogs';
import { ErrorManager } from '../common/error.manager';
import { DialogConfirmYESNO, DialogConfirmYESNOUtils } from '../common/components/dq-dialog-yesno';
import { KeyfieldsEditorComponent } from '../keyfields-editor/keyfields-editor.component';
import { EditorDialogComponent } from '../editor.dialog/editor-dialog.component';
import 'codemirror/mode/sql/sql'

enum Type {
  TABLE = 'TABLE',
  HDFS = 'HDFS',
  HIVE = 'HIVE',
  VIRTUAL = 'VIRTUAL'
};

abstract class FeSource<T> {
  isNew: boolean;
  data: T;
  constructor(source?: T) {
    this.data = source;
  }
}

class HiveTableFE extends FeSource<sources.HiveTable> {
  dateFe: Date;
  constructor(hiveTableSource?: sources.HiveTable) {
      super(hiveTableSource);
      this.dateFe = new Date(this.data.date);
  }

  updateDate() {
    this.data.date = this.dateFe.toString() === 'Invalid Date' ? null : this.dateFe.toISOString();
  }
}

class HdfsFileFE extends FeSource<sources.HdfsFile> {

  dateFe: Date;
  constructor(hdfsFileSource?: sources.HdfsFile) {
    super(hdfsFileSource);
    this.dateFe = new Date(this.data.date);
  }

  updateDate() {
    this.data.date = this.dateFe.toString() === 'Invalid Date' ? null : this.dateFe.toISOString();
  }

  get schemaFe() {
    return JSON.stringify(this.data.schema, null, 2);
  }

  set schemaFe(v: string) {
    this.data.schema = JSON.parse(v);
  }

  get schemaValue() {
      if (this.data.schema && this.data.schema.length !== 0) {
        return 'schema'
      }
      return 'schemaPath'
  }
 
}

class DbTableFE extends FeSource<sources.DbTable> {
  constructor(dbTableSource?: sources.DbTable) {
    super(dbTableSource);
  }
}

class VirtualSourceFE extends FeSource<sources.Virtual> {
  

  constructor(virtual?: sources.Virtual) {
      super(virtual);
  }
}

@Component({
  selector: 'app-sources',
  templateUrl: './sources.component.html',
  styleUrls: ['./sources.component.css']
})
export class SourcesComponent implements OnInit {

  @Output() onOpen = new EventEmitter<string>();
  @Output() onClose = new EventEmitter<string>();

  // used to resolve enum in the view
  Type = Type;

  file_types = [
    'csv',
    'avro'
  ];

   source_types = [
    'HDFS',
    'TABLE',
    'HIVE'
   ];

   virtualSourceTypes = [
    'JOIN_SQL',
    'FILTER_SQL'
   ];

   separators = [
     ',',
     ';',
     '|',
     '.',
     ':'
   ];


  hdfsSources: Array<HdfsFileFE> = [];
  tableSources: Array<DbTableFE> = [];
  hiveSources: Array<HiveTableFE> = [];
  virtualSources: Array<VirtualSourceFE> = [];

  hdfsNumberPages: number;
  tableNumberPages: number;
  hiveNumberPages: number;
  virtualNumberPages: number;

  sourceIds: Array<string>;

  databaseIds: String[];

  errorValidation: Array<any> = [];

  constructor(public sourcesService: SourcesService,
    public databasesService: DatabaseService,
    public interactionsService: InteractionsService,
    public dialog: MdDialog,
    public snackBar: MdSnackBar) {}

  open(sourceId: string) {
    this.interactionsService.setSourceId(sourceId);
    this.onOpen.emit('metrics');
  }

  close() {
    this.restoreValidation();
    this.onClose.emit('sources');
  }

  getSources(): void {
    // get firs page for all source types
    this.getPagedSource(Type.HDFS, 0);
    this.getPagedSource(Type.TABLE, 0);
    this.getPagedSource(Type.HIVE, 0);
    this.getPagedSource(Type.VIRTUAL, 0);

    this.sourcesService.getSourceIds().then(sourcesRequest => {
      this.sourceIds = <string[]> sourcesRequest.sources
    });
  }


  getPagedSource(sourceType: Type, page?: number): void {
    this.sourcesService.getAllSources(null, page, sourceType).then(sourcesRequest => {
      const sources = <sources.Source[]>sourcesRequest.sources;
      switch (sourceType) {
        case Type.HDFS: {
          this.hdfsSources =  [];
          this.hdfsNumberPages = sourcesRequest.count
          sources.forEach(hdfsSource => {
            this.sourcesService.getFileDetails(hdfsSource.id).then(hdfsSourceDetails => {
              this.hdfsSources.push(new HdfsFileFE(hdfsSourceDetails));
            });
          });
        } break;
        case Type.TABLE: {
          this.tableSources =  [];
          this.tableNumberPages = sourcesRequest.count
          sources.forEach(tableSource => {
            this.sourcesService.getDBTableDetails(tableSource.id).then(tableSourceDetails => {
              this.tableSources.push(new DbTableFE(tableSourceDetails));
            });
          });
        } break;
        case Type.HIVE: {
          this.hiveSources = [];
          this.hiveNumberPages = sourcesRequest.count
          sources.forEach(hiveSource => {
            this.sourcesService.getHiveTableDetails(hiveSource.id).then(hiveSourceDetails => {
                this.hiveSources.push(new HiveTableFE(hiveSourceDetails));
            });
          });
        } break;
        case Type.VIRTUAL: {
          this.virtualSources = [];
          this.virtualNumberPages = sourcesRequest.count
          sources.forEach(virtualSource => {
            this.sourcesService.getVirtualSourceDetails(virtualSource.id).then(virtualSourceDetails => {
                this.virtualSources.push(new VirtualSourceFE(virtualSourceDetails));
            });
          });
        } break;
      }
    });
  }

  updateSource(sourceType: string, source: any) {
    this.restoreValidation();

    let updatePromise: Promise<any>;

    if (sourceType === Type.HDFS) {
      source.updateDate();
      updatePromise = this.sourcesService.updateHdfsFile(source.data.id, source.data)
    }
    if (sourceType === Type.HIVE) {
      source.updateDate();
      updatePromise = this.sourcesService.updateHiveTable(source.data.id, source.data);
    }
    if (sourceType === Type.TABLE) {
      updatePromise = this.sourcesService.updateDBTable(source.data.id, source.data);
    }
    if (sourceType === Type.VIRTUAL) {
      updatePromise = this.sourcesService.updateVirtualSource(source.data.id, source.data);
    }

    updatePromise.then(() => this.snackBarMessage('Source Updated!'))
    .catch(err => this.errorManager(err));
  }

  deleteSource(sourceType: string, source: any) {
    this.restoreValidation();

    let deletePromise: Promise<any>;

    DialogConfirmYESNOUtils.areYouSure(
      this.dialog,
      () => {
              if (source.isNew) {
                this.hdfsSources = this.hdfsSources.filter(s => s.data.id !== source.data.id);
                this.hiveSources = this.hiveSources.filter(s => s.data.id !== source.data.id);
                this.tableSources = this.tableSources.filter(s => s.data.id !== source.data.id);
                this.virtualSources = this.virtualSources.filter(s => s.data.id !== source.data.id);
              } else {
                if (sourceType === Type.HDFS) {
                  deletePromise = this.sourcesService.deleteHdfsFile(source.data.id).then(() => {
                    this.hdfsSources = this.hdfsSources.filter(s => s.data.id !== source.data.id);
                  });
                }
                if (sourceType === Type.HIVE) {
                  deletePromise = this.sourcesService.deleteHiveTable(source.data.id).then(() => {
                    this.hiveSources = this.hiveSources.filter(s => s.data.id !== source.data.id);
                  });
                }
                if (sourceType === Type.TABLE) {
                  deletePromise = this.sourcesService.deleteDBTable(source.data.id).then(() => {
                    this.tableSources = this.tableSources.filter(s => s.data.id !== source.data.id);
                  });
                }
                if (sourceType === Type.VIRTUAL) {
                  deletePromise = this.sourcesService.deleteVirtualSource(source.data.id).then(() => {
                    this.virtualSources = this.virtualSources.filter(s => s.data.id !== source.data.id);
                  })
                }
              }

              deletePromise.then(() => {
                this.sourceIds = this.sourceIds.filter(id => id !== source.data.id)
              } ).catch(error => this.deleteWarning())
            });
  }

  deleteWarning() {
    const dialogRef = this.dialog.open(DialogAlert, {
      data: 'You can not delete a metrics with bounded checks'
    });
  }

  createSource(sourceType: string, source: any) {

    this.restoreValidation();

    let createPromise: Promise<any>;

    if (sourceType === Type.HDFS) {
      source.updateDate();
      createPromise = this.sourcesService.addHdfsFile(source.data).then(response => {
        source.isNew = false;
      })
    }
    if (sourceType === Type.HIVE) {
      source.updateDate();
      createPromise = this.sourcesService.addHiveTable(source.data).then(response => {
          source.isNew = false;
      })
    }
    if (sourceType === Type.TABLE) {
      createPromise = this.sourcesService.addDBTable(source.data).then(response => {
        source.isNew = false;
        
      })
    }
    if (sourceType === Type.VIRTUAL) {
      createPromise =  this.sourcesService.addVirtualSources(source.data).then(response => {
        source.isNew = false;
      })
      
    }

    createPromise.then(() => {
      this.sourceIds.push(source.data.id);
      this.snackBarMessage('Source Created!');
    }).catch(err => this.errorManager(err));
  }

  addSource(sourceType: string) {
    if (sourceType === Type.HDFS) {
      const newHdfsSource = new HdfsFileFE(new sources.HdfsFile('NEW SOURCE'));
      newHdfsSource.isNew = true;
      this.hdfsSources.push(newHdfsSource);
    }
    if (sourceType === Type.HIVE) {
      const newHiveSource = new HiveTableFE(new sources.HiveTable('NEW SOURCE'));
      newHiveSource.isNew = true;
      this.hiveSources.push(newHiveSource);
    }
    if (sourceType === Type.TABLE) {
      const newTableSource = new DbTableFE(new sources.DbTable('NEW SOURCE'))
      newTableSource.isNew = true;
      this.tableSources.push(newTableSource);
    }
    if (sourceType === Type.VIRTUAL) {
      const newVirtualSource = new VirtualSourceFE(new sources.Virtual('NEW SOURCE'))
      newVirtualSource.isNew = true;
      this.virtualSources.push(newVirtualSource);
    }
  }

  getDatabaseIds() {
    this.databasesService.getDatabaseIds().then(dbResponse => {
      this.databaseIds = <string[]>dbResponse.databases
    });
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

  openKeyfieldsEditorDialog(source: FeSource<any>) {
    const dialogRef = this.dialog.open(KeyfieldsEditorComponent, {
      data: source.data.keyFields,
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result !== undefined) {
        source.data.keyFields = <string[]>result;
      }
    });
  }

  openSQLEditorDialog(source: FeSource<any>) {
    const dialogRef = this.dialog.open(EditorDialogComponent, {
      data: {
        content: source.data.query,
        config: { lineNumbers: true, mode: 'text/x-hive' }
      },
      disableClose: true,
      width: '50%'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result !== undefined) {
        source.data.query = <string>result;
      }
    });
  }

  pageCount() {
    return;
  }

  pageChange(pageEvent: any, sourceType: Type) {
    this.getPagedSource(sourceType, pageEvent.pageIndex)
  }


  ngOnInit() {
    this.getSources();
    this.getDatabaseIds();
  }

}
