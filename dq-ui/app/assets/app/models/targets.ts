export class TargetResponse {
    targets: Target[];
}

export class TargetBase {
    id: string;
    targetType: string;
    constructor(id: string, targetType: string) {
        this.id = id;
        this.targetType = targetType;
    }
}

export class Target extends TargetBase {
    fileFormat: string; 
    path: string;
    delimiter: string;
    savemode: string;
    partitions: number;
    mails: Mail[];
    checks: Check[];
    constructor(id: string,
        targetType?: string,
        fileFormat?: string,
        path?: string,
        delimiter?: string,
        savemode?: string,
        partitions?: number,
        mails?: Mail[],
        checks?: Check[]) {
            super(id, targetType);
            this.fileFormat = fileFormat;
            this.path = path;
            this.delimiter = delimiter;
            this.savemode = savemode;
            this.partitions = partitions;
            this.mails = mails || [];
            this.checks = checks || [];
        }
}

export class Mail {
    address: string;
    constructor(mail: string) {
        this.address = mail;
    }
}

export class Check {
    checkId: string;
    constructor(check: string) {
        this.checkId = check;
    }
}
