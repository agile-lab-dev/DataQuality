import { NativeDateAdapter } from '@angular/material';

export const DQ_DATE_FORMATS = {
    parse: {
        dateInput: {month: 'numeric', year: 'numeric', day: 'numeric'}
    },
    display: {
        dateInput: 'input',
        monthYearLabel: {year: 'numeric', month: 'short'},
        dateA11yLabel: {year: 'numeric', month: 'long', day: 'numeric'},
        monthYearA11yLabel: {year: 'numeric', month: 'long'},
    }
 };

export class DqDateAdapter extends NativeDateAdapter {

    /** parse(value: any): Date | null {
        console.log(value);
        return new Date(value);
    };**/

    parse(value: any): Date | null {
        console.log(value);
        if ((typeof value === 'string') && (value.indexOf('-') > -1)) {
          const str = value.split('/');

          const year = Number(str[2]);
          const month = Number(str[1]) - 1;
          const date = Number(str[0]);
          return new Date(year, month, date);
        } else if ((typeof value === 'string') && value === '') {
          return new Date();
        }
        const timestamp = typeof value === 'number' ? value : Date.parse(value);
        return isNaN(timestamp) ? null : new Date(timestamp);
      }

    format(date: Date, displayFormat: Object): string {

        if (displayFormat === 'input') {
            const day = date.getDate();
            const month = date.getMonth() + 1;
            const year = date.getFullYear();
            return `${year}-${month}-${day}`;
        } else {
            return date.toDateString();
        }
    }
}
