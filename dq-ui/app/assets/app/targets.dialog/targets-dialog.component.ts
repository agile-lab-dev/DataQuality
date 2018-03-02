import { Component, Inject, OnInit } from '@angular/core';
import {MdDialog, MdDialogRef, MD_DIALOG_DATA} from '@angular/material';
import { TargetsService } from '../services/targets.service'

class Target {
    id: string;
    checked: boolean;

    constructor(id: string, checked: boolean) {
        this.id = id;
        this.checked = checked;
    }
}

@Component({
  templateUrl: './targets-dialog.component.html'
})
export class TargetDialogComponent implements OnInit {

  targets: Target[];
  activeTargets: Target[];

  constructor(
    public targetService: TargetsService,
    public dialogRef: MdDialogRef<TargetDialogComponent>,
    @Inject(MD_DIALOG_DATA) public data: string[]) {
        // actually check targets, so are active by default.
        this.activeTargets = data.map(c => new Target(c, true));
     };

  done() {
    // on close return new target checks list
    this.dialogRef.close(this.targets.filter(c => c.checked).map(c => c.id));
  }

  getTargets() {
      this.targetService.getAllTargets().then(response => {
          // by default a target is not selected
          const allTarget = response.targets.filter(t => t.targetType === 'SYSTEM' ).map(c => new Target(c.id, false));

          this.targets = this.mergeActiveTarget(allTarget, this.activeTargets);
      })
  }

  checkedChange(target: Target) {
    target.checked = target.checked ? false : true;
  }

  ngOnInit() {
    this.getTargets();
  }

  private mergeActiveTarget(allTarget: Target[], activeTargets: Target[]): Target[] {
    const allItems = allTarget.concat(activeTargets).filter(c => c.id !== undefined);
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
