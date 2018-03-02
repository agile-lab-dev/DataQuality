import { Component, Inject } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA } from '@angular/material';
/**
 * Internal component. Don't move in a external file.
 */
@Component({
    selector: 'dialog-confirm-yesno',
    template: `<h2 md-dialog-title>Are you sure?</h2>
    <md-dialog-actions>
        <button md-button [md-dialog-close]="true">Yes</button>
        <button md-button (click)="onNoClick()">No</button>
    </md-dialog-actions>`,
    })

export class DialogConfirmYESNO {
    constructor(
        private dialog: MdDialogRef<DialogConfirmYESNO>,
        @Inject(MD_DIALOG_DATA) public data: any) {}

    onNoClick(): void {
        this.dialog.close();
    }
}

export class DialogConfirmYESNOUtils {
    public static areYouSure(dialog: MdDialog,  cb: () => void) {
        const dialogRef = dialog.open(DialogConfirmYESNO,
            {
                width: '250px',
                data: { }
            }
        );
    
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                cb();
            }
        });
      }
}
