import { Component, Inject, OnInit } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';


@Component({
  templateUrl: './editor-dialog.component.html'
})
export class EditorDialogComponent implements OnInit {


  content: string;
  config: any;

  constructor(
    public dialogRef: MdDialogRef<EditorDialogComponent>,
    @Inject(MD_DIALOG_DATA) public data: any) {
      console.log(data)
        this.content = data.content;
        this.config = data.config;
     };

  done() {
    this.dialogRef.close(this.content);
  }

  cancel() {
    this.dialogRef.close();
  }


  ngOnInit() {
    return;
  }

}
