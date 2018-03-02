import { Component, Inject, OnInit } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import { Database } from '../models/databases';
import { CheckSql } from '../models/checks';
import { DatabaseService } from '../services/databases.service';
import { ChecksService } from '../services/checks.service';
import { ErrorManager } from '../common/error.manager';
import { DialogConfirmYESNO, DialogConfirmYESNOUtils } from '../common/components/dq-dialog-yesno';
import { DatabaseShortList } from '../models/databases'
import { EditorDialogComponent } from '../editor.dialog/editor-dialog.component';
import 'codemirror/mode/sql/sql'
import { SearchService } from '../services/search.service'


class DatabaseFe {
  isNew: boolean;
  data: Database;

  constructor(db: Database, isNew: boolean) {
    this.data = db;
    this.isNew = isNew;
  }
}

class SqlCheckFe {
  isNew: boolean;
  data: CheckSql;

  constructor(sqlCheck: CheckSql, isNew: boolean) {
    this.data = sqlCheck;
    this.isNew = isNew;
  }
}

// TODO: refactoring to avoid code duplication for checks view
@Component({
  selector: 'app-databases',
  templateUrl: './databases.component.html',
  styleUrls: ['./databases.component.css']
})
export class DatabasesComponent implements OnInit {

  databaseTypes = [
      'SQLITE'
  ];

  checkTypes = [
      'COUNT_EQ_ZERO',
      'COUNT_NOT_EQ_ZERO'
  ];

  showChecks: boolean;

  databases: DatabaseFe[] = [];

  databaseElements: number;

  sqlChecks: SqlCheckFe[] = [];

  sqlCheckElements: number;

  errorValidation: Array<any> = [];

  // current active dattabase connection
  activeDatabaseId: string;

  sqlCheckSearch: string

  constructor(
    public dialogRef: MdDialogRef<DatabasesComponent>,
    @Inject(MD_DIALOG_DATA) public data: any, 
    public databasesService: DatabaseService,
    public checksService: ChecksService,
    public searchService: SearchService,
    private dialog: MdDialog,
    public snackBar: MdSnackBar
  ) { };


  getDatabases() {
    this.getPagedDatabases(0);
  }

  getPagedDatabases(page?: number) {
    this.databasesService.getAllDatabases(page).then(databasesRequest => {
      const databases = <DatabaseShortList[]>databasesRequest.databases;
      this.databases =  [];
      this.databaseElements = databasesRequest.count
      databases.forEach(database => {
         this.databasesService.getDatabaseDetails(database.id).then(databaseDetails => {
           this.databases.push(new DatabaseFe(databaseDetails, false));
         });
       });
    });
  }


  addDatabase() {
    this.databases.push(new DatabaseFe(new Database('NEW_DATABASE'), true))
  }

  createDatabase(database: DatabaseFe) {
    this.restoreValidation();
    this.databasesService.addDatabase(database.data).then(response => {
      database.isNew = false;
      this.activeDatabaseId = database.data.id;
      this.snackBarMessage('Database Updated!');
    })
    .catch(err => this.errorManager(err));
  }

  updateDatabase(database: DatabaseFe) {
    this.restoreValidation();
    this.databasesService.updateDatabase(database.data.id, database.data).then(response => {
      database.isNew = false;
      this.snackBarMessage('Database Updated!');
    })
    .catch(err => this.errorManager(err));
  }

  deleteDatabase(database: DatabaseFe) {
    this.restoreValidation();
    DialogConfirmYESNOUtils.areYouSure(
      this.dialog,
      () => {
              if (database.isNew) {
                this.databases = this.databases.filter(db => db.data.id !== database.data.id)
              } else {
                this.databasesService.deleteDatabase(database.data.id).then(response => {
                  this.databases = this.databases.filter(db => db.data.id !== database.data.id)
                });
              }
      });
    
  }

  closeDatabaseView() {
    // when close the dialog return a list of database ids that was be created correctly
    this.dialogRef.close(this.databases.filter(db => !db.isNew).map(db => db.data.id));
  }

  getChecks(databaseId: string): void {
    this.getPagedChecks(databaseId)
  };

  getPagedChecks(databaseId?: string, page?: number) {
    // clear up current checks before download new
    this.sqlChecks = [];
    this.checksService.getAllChecks(databaseId, null, page, 'sql').then(checksRequest => {
        const allChecks = checksRequest.checks;
        this.sqlCheckElements = checksRequest.count
        this.sqlChecks = []
        allChecks.forEach(sqlCheck => {
            this.checksService.getSqlCheckDetails(sqlCheck.id).then(sqlCheckDetails => {
                this.sqlChecks.push(new SqlCheckFe(sqlCheckDetails, false));
            });
        });
    });
  }

  updateCheck(check: any) {
      this.checksService.updateSqlCheck(check.data.id, check.data)
        .then(r => this.snackBarMessage('Check Updated!'))
        .catch(err => this.errorManager(err));
  }

  deleteCheck(check: any) {
    this.restoreValidation();
    DialogConfirmYESNOUtils.areYouSure(
      this.dialog,
      () => {
              if (check.isNew) {
                this.sqlChecks = this.sqlChecks.filter(c => c.data.id !== check.data.id);
              } else {
                this.checksService.deleteSqlCheck(check.data.id).then(() => {
                  this.sqlChecks = this.sqlChecks.filter(c => c.data.id !== check.data.id);
                });
              }
      });
  }

  createCheck(check: any) {
      this.restoreValidation();
      this.checksService.addSqlCheck(check.data).then(response => {
          check.isNew = false;
          this.snackBarMessage('Check Updated!');
      })
      .catch(err => this.errorManager(err));
  }

  addCheck(checkType: string) {
        const newSqlCheck = new SqlCheckFe(new CheckSql('NEW CHECKS', 'SQL' , '', '', this.activeDatabaseId, ''), true)
        this.sqlChecks.push(newSqlCheck);
  }

  openChecks() {
    // show current database checks
    this.showChecks = true;
    // retrive current database checks
    this.getChecks(this.activeDatabaseId);
  }

  openDatabase(database: DatabaseFe) {
    this.restoreValidation();
    // hide previous database checks
    this.showChecks = false;
    this.activeDatabaseId = database.data.id
  }

  ngOnInit() {
    this.getDatabases();
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


  getCheckSearchResults(page?: number) {
      this.searchService
      .search('check', this.sqlCheckSearch, 'database', this.activeDatabaseId, page)
      .then(searchResult => {
        this.sqlCheckElements = searchResult.size;
        this.sqlChecks = [];
        searchResult.results.forEach(searchId => {
          this.checksService.getSqlCheckDetails(searchId).then(sqlCheckDetails => {
            this.sqlChecks.push(new SqlCheckFe(sqlCheckDetails, false));
        });
        })
      })
    }

      /**called when search input is changed */
    inputSearchCheckChange() {

        if (this.sqlCheckSearch) {
          this.getCheckSearchResults(0);
        } else {
          this.getPagedChecks(this.activeDatabaseId, 0)
        }
        
    }
    
    /**
    * Clear Search and show all result
    */
    clearCheckSearch() {
        // clear search varaible
        this.sqlCheckSearch = '';
    
        // show first page elements
        this.getPagedChecks(this.activeDatabaseId, 0)
      }

pageChange(pageEvent: any, entityType: string) {

  switch (entityType) {
    case 'DATABASE': {
      this.getPagedDatabases(pageEvent.pageIndex)
    } break;
    case 'CHECK': {
      if (this.sqlCheckSearch) {
        this.getPagedChecks(this.activeDatabaseId, pageEvent.pageIndex)
      } else {
        this.getCheckSearchResults(pageEvent.pageIndex)
      }
    } break;
  }
  
}

openSQLEditorDialog(check: any) {
  const dialogRef = this.dialog.open(EditorDialogComponent, {
    data: {
      content: check.data.query,
      config: { lineNumbers: true, mode: 'text/x-hive' }
    },
    disableClose: true,
    width: '50%'
  });

  dialogRef.afterClosed().subscribe(result => {
    if (result !== undefined) {
      check.data.query = <string>result;
    }
  });
}


}
