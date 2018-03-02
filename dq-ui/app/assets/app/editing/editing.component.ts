import { Component } from '@angular/core';

class NavigationItem {
  name: string;
  active: boolean;
  show: boolean;
  children_id: string;
  constructor(name: string, active: boolean, show: boolean, children_id: string) {
    this.name = name;
    this.active = active;
    this.show = show;
    this.children_id = children_id;
  }
}

@Component({
  selector: 'app-editing',
  templateUrl: './editing.component.html',
  styleUrls: ['./editing.component.css']
})
export class EditingComponent {
  data: { [key: string]: NavigationItem; } = {
  'sources' : new NavigationItem(
    'sources',
    true,
    true,
    'metrics',
  ),
  'metrics': new NavigationItem(
    'metrics',
    true,
    false,
    'checks',
  ),
  'checks' : new NavigationItem(
    'checks',
    true,
    false,
    'targets',
  ),
  'targets' : new NavigationItem(
    'targets',
    true,
    false,
    undefined
  ),
};

  isShow(elementId: string) {
     return this.data[elementId].show;
  }

  onOpen(navId: string) {
    this.data[navId].show = true;
  }

  onClose(navId: string) {
    this.closeChildren(this.data[navId]);
  }

  private closeChildren(elem: NavigationItem) {
    if (elem.children_id === undefined) {
      return;
    } else {
      this.data[elem.children_id].show = false;
      this.closeChildren(this.data[elem.children_id]);
    }
  }

}
