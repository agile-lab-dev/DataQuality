import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import 'rxjs/Rx';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {
    constructor(private router: Router) {}

    ngOnInit() {
        return;
    }
}
