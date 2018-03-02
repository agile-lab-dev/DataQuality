import { Component, Inject, OnInit } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';

@Component({
  templateUrl: './config-download-dialog.component.html'
})
export class ConfigDowloadDialogComponent implements OnInit {


  fileName: string;

  constructor(
    public dialogRef: MdDialogRef<ConfigDowloadDialogComponent>,
    @Inject(MD_DIALOG_DATA) public data: string) {
        this.fileName = data;
     };

  download() {
    this.dialogRef.close(this.fileName);
  }

  cancel() {
    this.dialogRef.close();
  }


  ngOnInit() {
    return;
  }

}
