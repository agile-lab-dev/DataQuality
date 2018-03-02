import { Component, Input, OnInit, ViewChild, Inject } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA } from '@angular/material';

import 'rxjs/Rx';

/**
 * Dialog component that show an allert message.
 */
@Component({
    selector: 'dialog-alert',
    template: `<h2 md-dialog-title>{{message}}</h2>
    <md-dialog-actions>
        <button md-button [md-dialog-close]="true">OK</button>
    </md-dialog-actions>`,
    })

export class DialogAlert {

    message: string;

    constructor(
        private dialogRef: MdDialogRef<DialogAlert>,
        @Inject(MD_DIALOG_DATA) public data: string) {
            this.message = data;
        }
}
