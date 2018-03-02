import { Component, Inject, OnInit } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';

@Component({
  selector: 'app-mails',
  templateUrl: './mails.component.html',
  styleUrls: ['./mails.component.css']
})
export class MailsComponent implements OnInit {


  mails: string[];

  newMail: string;

  errorValidation: string;

  constructor(
    public dialogRef: MdDialogRef<MailsComponent>,
    @Inject(MD_DIALOG_DATA) public data: string[]) {
      this.mails = data;
     };

  addMail(mail: string) {
    if (mail) {
      if (mail !== '' && this.validateEmail(mail)) {
        this.mails.push(mail);
        this.newMail = '';
        this.errorValidation = '';
      } else {
        this.errorValidation = 'Complete with a valid mail.';
      }
    }
  }

  validateEmail(email: string) {
    const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
  }

  removeMail(mail: string) {
    this.mails = this.mails.filter(m => m !== mail);
  }

  done() {
    // on close return new mailing list
    this.dialogRef.close(this.mails);
  }

  ngOnInit() {
    return;
  }

}
