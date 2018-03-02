import { Component, EventEmitter, Input, Output, OnInit, Inject, Injectable, Pipe, PipeTransform } from '@angular/core';
import { MdDialog, MdDialogRef, MD_DIALOG_DATA, MdSnackBar } from '@angular/material';
import { MailsComponent } from '../mails/mails.component';
import * as targets from '../models/targets';
import { TargetsService } from '../services/targets.service';
import { ChecksDialogComponent } from '../checks.dialog/checks-dialog.component';
import { InteractionsService } from '../services/interactions.service';
import { ErrorManager } from '../common/error.manager';

abstract class TargetFe<T> {
  isNew: boolean;
  data: T;
  constructor(target: T) {
    this.data = target;
  }
}

class Target extends TargetFe<targets.Target> {
  constructor(target: targets.Target, isNew?: boolean) {
    super(target);
    this.isNew = isNew;
  }

  addMailToMailingList(mail: string) {
    this.data.mails.push(new targets.Mail(mail));
  }

  removeMailFromMailingList(mail: string) {
    this.data.mails = this.data.mails.filter( m => m.address === mail);
  }
}


@Pipe({
  name: 'targetTypeFilter',
  pure: false
})
@Injectable()
export class TargetTypePipe implements PipeTransform {
  transform(items: Target[], args: string): any {
      return items.filter(item => item.data.targetType === args);
  }
}

@Component({
  selector: 'app-targets',
  templateUrl: './targets.component.html',
  styleUrls: ['./targets.component.css']
})
export class TargetsComponent implements OnInit {

  @Output() onOpen = new EventEmitter<string>();
  @Output() onClose = new EventEmitter<string>();


  targetTypes = [
    'SYSTEM',
    'FILE-METRICS',
    'COLUMN-METRICS',
    'COMPOSED-METRICS',
    'CHECKS'
 ];

 fileFormats = [
   'csv'
 ];
 savemodes = [
   'append'
 ];

 delimiters = [
  ',',
  ';',
  '|',
  '.',
  ':'
  ]

  checkId: string;

  targets: Array<Target> = [];

  errorValidation: Array<any> = [];

  constructor(public dialogRef: MdDialogRef<TargetsComponent>,
    @Inject(MD_DIALOG_DATA) public data: any,
    public dialog: MdDialog,
    public targetsService: TargetsService,
    public interactionsService: InteractionsService,
    public snackBar: MdSnackBar
  ) {}

  openMailsDialog(target: Target) {
    const dialogRef = this.dialog.open(MailsComponent, {
      data: target.data.mails.map(m => m.address)
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result !== undefined) {
        target.data.mails = (<string[]>result).map(m => new targets.Mail(m));
      }
    });
  }

  openChecksDialog(target: Target) {
    const dialogRef = this.dialog.open(ChecksDialogComponent, {
      data: target.data.checks.map(c => c.checkId),
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result !== undefined) {
        target.data.checks = (<string[]>result).map(c => new targets.Check(c));
      }
    });
  }

  closeTargetsView() {
    // when close the dialog return a list of database ids that was be created correctly
    this.dialogRef.close();
  }

  open() {
    return;
  }

  close() {
    this.restoreValidation();
    this.onClose.emit('targets');
  }

  getTargets(checkId: string) {
    this.targetsService.getAllTargets(checkId).then(targetsRequest => {
      const targets = targetsRequest.targets;

      targets.filter(tr => tr.targetType === 'SYSTEM').forEach(target => {
         this.targetsService.getTargetDetails(target.id).then(targetsDetails => {
           this.targets.push(new Target(targetsDetails));
         });
       });

       targets.filter(tr => tr.targetType !== 'SYSTEM').forEach(target => {
        this.targetsService.getTargetDetails(target.id).then(targetsDetails => {
          this.targets.push(new Target(targetsDetails));
        });
      });
    });
  }


  addTarget(type: string) {
    this.targets.push(new Target(new targets.Target('NEW TARGET', type), true));
  }

  createTarget(target: Target) {
    this.restoreValidation();
    this.targetsService.addTarget(target.data).then(response => {
      target.isNew = false;
      this.snackBarMessage('Target Created!');
    })
    .catch(err => this.errorManager(err));
  }

  updateTarget(target: Target) {
    this.restoreValidation();
    this.targetsService.updateTarget(target.data.id, target.data)
      .then(r => this.snackBarMessage('Target Updated!'))
      .catch(err => this.errorManager(err));
  }

  deleteTarget(target: Target) {
    this.restoreValidation();
    this.targetsService.deleteTarget(target.data.id).then(response => {
      this.targets = this.targets.filter(t => t.data.id !== target.data.id)
    })
    .catch(err => this.errorManager(err));
  }

  ngOnInit() {
    this.checkId = this.interactionsService.getCheckId();
    this.getTargets(this.checkId);
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

}
