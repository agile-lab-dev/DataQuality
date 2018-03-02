import { Component, EventEmitter, Input, Output, OnInit, Inject, Injectable, Pipe, PipeTransform } from '@angular/core';

@Pipe({name: 'entries', pure: false})
export class KeysPipe implements PipeTransform {
  transform(value: Map<any, any>, args: string[]): any {
    const keys = [];
    for (const key of Array.from( value.keys()) ) {
        keys.push({key: key, value: value.get(key)});
     }
    return keys;
  }
}
