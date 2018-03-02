import { Injectable } from '@angular/core';


@Injectable()
export class InteractionsService {
    sourceId: string;
    metricId: string;
    checkId: string;

    getSourceId() {
        return this.sourceId;
    }

    getMetricId() {
        return this.metricId;
    }

    getCheckId() {
        return this.checkId;
    }

    setSourceId(sourceId: string) {
        this.sourceId = sourceId;
    }

    setMetricId(metricId: string) {
        this.metricId = metricId;
    }

    setCheckId(checkId: string) {
        this.checkId = checkId;
    }
}
