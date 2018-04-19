import { Injectable } from '@angular/core';
import { Headers, Http, ResponseContentType } from '@angular/http';

import 'rxjs/add/operator/toPromise';

import 'file-saver';

@Injectable()
export class ConfigService {
    private headers = new Headers({'Content-Type': 'application/json'});
    private baseConfigUrl = 'dataquality/config';

    fileName: string;

    constructor(private http: Http) { }

    downloadConfiguration(fileName?: string) {
        this.fileName = fileName || 'dataquality.conf';
        return this.http.get(`${this.baseConfigUrl}`, { responseType: ResponseContentType.Text })
        .subscribe(
            data => {
                const mediaType = 'text/plain';
                const blob = new Blob([(<any>(data))._body], {type: mediaType});
                saveAs(blob, this.fileName); 
            }, 
            error => console.log('Error downloading the file.'), 
            () => console.log('OK')
        );
    }

    // uploadConfiguration -> implemented using ng2-file-upload
    getUploadURL(): string {
        return `${this.baseConfigUrl}`;
    }

    resetConfiguration(): Promise<void> {
        return this.http.delete(`${this.baseConfigUrl}`, {headers: this.headers})
            .toPromise()
            .then(() => null)
            .catch(this.handleError);
    }

    private handleError(error: any): Promise<any> {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }
}
