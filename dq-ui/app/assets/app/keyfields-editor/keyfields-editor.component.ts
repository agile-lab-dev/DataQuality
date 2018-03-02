import { Component, Inject } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';

@Component({
  selector: 'keyfields-editor',
  templateUrl: './keyfields-editor.component.html',
  styleUrls: ['./keyfields-editor.component.css']
})
export class KeyfieldsEditorComponent {


  keyfields: string[];

  keyField: string;

  errorValidation: string;

  constructor(
    public dialogRef: MdDialogRef<KeyfieldsEditorComponent>,
    @Inject(MD_DIALOG_DATA) public data: string[]) {
      this.keyfields = data;
     };

  addKeyField(keyField: string) {
    if (keyField) {
      if (keyField !== '') {
        this.keyfields.push(keyField);
        this.keyField = '';
        this.errorValidation = '';
      } else {
        this.errorValidation = 'Empty keyfield not allowed';
      }
    } else {
      this.errorValidation = 'Empty keyfield not allowed';
    }
  }


  removeKeyField(keyfield: string) {
    this.keyfields = this.keyfields.filter(kf => kf !== keyfield);
  }

  done() {
    // on close return new keyfield list
    this.dialogRef.close(this.keyfields);
  }

}
