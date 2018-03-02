import { Component, Inject, OnInit } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';
import { ChecksService } from '../services/checks.service'

class Check {
    id: string;
    checked: boolean;

    constructor(id: string, checked: boolean) {
        this.id = id;
        this.checked = checked;
    }
}

@Component({
  templateUrl: './checks-dialog.component.html',
  styleUrls: ['./checks-dialog.component.css']
})
export class ChecksDialogComponent implements OnInit {


  checks: Check[];
  activeChecks: Check[];

  constructor(
    public checksService: ChecksService,
    public dialogRef: MdDialogRef<ChecksDialogComponent>,
    @Inject(MD_DIALOG_DATA) public data: string[]) {
        // actually target checks, so are active by default.
        this.activeChecks = data.map(c => new Check(c, true));
     };

  done() {
    // on close return new target checks list
    this.dialogRef.close(this.checks.filter(c => c.checked).map(c => c.id));
  }

  getChecks() {
      this.checksService.getAllChecks().then(response => {
          // by default a check is not selected
          const allChecks = response.checks.map(c => new Check(c.id, false));

          this.checks = this.mergeActiveChecks(allChecks, this.activeChecks);
      })
  }

  checkedChange(check: Check) {
    check.checked = check.checked ? false : true;
  }

  ngOnInit() {
    this.getChecks();
  }

  private mergeActiveChecks(allChecks: Check[], activeChecks: Check[]): Check[] {
    const allItems = allChecks.concat(activeChecks).filter(c => c.id !== undefined);
    const mergedItems = new Map();
    allItems.forEach((value) => {
      if (!mergedItems.get(value.id)) {
        mergedItems.set(value.id, value);
      } else {
        value.checked = value.checked || mergedItems.get(value.id).checked;
        mergedItems.set(value.id, value);
      }
    });
    return Array.from(mergedItems.values()) ;
  }

}
