import { Component, Input, OnInit, ViewChild, Inject } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA } from '@angular/material';
import { FileUploader } from 'ng2-file-upload';
import { ConfigService } from '../services/config.service';
import { DialogConfirmYESNO } from '../common/components/dq-dialog-yesno';

import 'rxjs/Rx';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.css']
})
export class ConfigComponent implements OnInit {
    @ViewChild('myInput')
    myInputVariable: any;

    public uploader: FileUploader = new FileUploader(
        {
            url: this.configService.getUploadURL(),
            disableMultipart: false,
            itemAlias: 'configuration',
            queueLimit: 1
        });

    constructor(private configService: ConfigService, public dialog: MdDialog) {}

    downloadConfigFile() {
        this.configService.downloadConfiguration();
    }

    resetConfig() {
        const dialogRef = this.dialog.open(DialogConfirmYESNO,
            {
                width: '250px',
                data: { }
            }
        );

        dialogRef.afterClosed().subscribe(result => {
            console.log(result);
            if (result) {
                this.configService.resetConfiguration();
            }
        });
    }

    clean() {
        this.uploader.clearQueue();
        this.myInputVariable.nativeElement.value = '';
    }

    ngOnInit() {
        return;
    }
}
