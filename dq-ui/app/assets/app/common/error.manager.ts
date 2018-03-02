import { MdSnackBar } from '@angular/material';

export class ErrorManager {
    /**
     * Prepare the validation error for the frontend.
     * @param obj 
     */
    public static prepareErrorMessage(obj: any) {

        const boby = JSON.parse(obj._body);
        const keys = Object.keys(boby);
        const ret = [];

        for (let i = 0; i < keys.length; i++) {
            ret.push({
                field: keys[i],
                message: boby[keys[i]]
            });
        }

        return ret;
    }
}
