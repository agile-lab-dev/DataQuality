import { Component } from '@angular/core';
import { MdDialog } from '@angular/material';
import { DatabasesComponent } from './databases/databases.component';
import { TargetsComponent } from './targets/targets.component';
import { MetricsComposedDialogComponent } from './metrics-composed.dialog/metrics-composed-dialog.component';
import { ConfigService } from './services/config.service';
import { ConfigDowloadDialogComponent } from './config.download.dialog/config-download-dialog.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(public dialog: MdDialog, private configService: ConfigService) {}

  openDatabaseDialog(): void {
    const dialogRef = this.dialog.open(DatabasesComponent, {
      width: '968px',
      data: {  }
    });
    dialogRef.disableClose = true;

    dialogRef.afterClosed().subscribe(result => {
        console.log('databases updated!');
        console.log(result);
    });
  }

  openTargetsDialog(): void {
    const dialogRef = this.dialog.open(TargetsComponent, {
      width: '968px',
      data: {  }
    });
  }

  openComposedMetricsDialog(): void {
    const dialogRef = this.dialog.open(MetricsComposedDialogComponent, {
      width: '968px',
      data: {  }
    });
  }

  

  downloadConfigFile() {

    const dialogRef = this.dialog.open(ConfigDowloadDialogComponent, {
      data: 'dataquality'
    });

    dialogRef.afterClosed().subscribe(fileName => {
      // file name returned?
      if (fileName !== undefined) {
        // download file
        this.configService.downloadConfiguration(fileName + '.conf');
      } // otherwise do nothing
     
    })
    
  }

}
