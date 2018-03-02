import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FlexLayoutModule } from '@angular/flex-layout';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MdListModule, MdButtonModule, MdCheckboxModule, MdToolbarModule, MdIconModule, MdInputModule,
MdSlideToggleModule, MdSelectModule, MdExpansionModule,
MdDatepickerModule, MdNativeDateModule, MdIconRegistry, MdMenuModule, MdCardModule,
MdDialogModule, MdTooltipModule, MdSortModule, MdProgressSpinnerModule, MdButtonToggleModule, MdSliderModule,
MdSnackBarModule, MdPaginatorModule, MdAutocompleteModule } from '@angular/material';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';
import { EditingComponent } from './editing/editing.component';
import { SourcesComponent } from './sources/sources.component';
import { DatabasesComponent } from './databases/databases.component';
import { MetricsComponent } from './metrics/metrics.component';
import { ChecksComponent } from './checks/checks.component';
import { TargetsComponent, TargetTypePipe } from './targets/targets.component';
import { MailsComponent } from './mails/mails.component';
import { ConfigComponent } from './config/config.component';
import { MenuComponent } from './menu/menu.component';
import { ChecksDialogComponent } from './checks.dialog/checks-dialog.component';
import { TargetDialogComponent } from './targets.dialog/targets-dialog.component';
import { ConfigDowloadDialogComponent } from './config.download.dialog/config-download-dialog.component';
import { MetricsComposedDialogComponent } from './metrics-composed.dialog/metrics-composed-dialog.component';

import { SourcesService } from './services/sources.service';
import { DatabaseService } from './services/databases.service';
import { MetricsService } from './services/metrics.service';
import { ChecksService } from './services/checks.service';
import { TargetsService } from './services/targets.service';
import { ConfigService } from './services/config.service';
import { InteractionsService } from './services/interactions.service';
import { MetasService } from './services/metas.service';
import { SearchService } from './services/search.service';

import { FileSelectDirective } from 'ng2-file-upload';

import { DialogAlert } from './common/components/dq-dialogs';
import { DialogConfirmYESNO } from './common/components/dq-dialog-yesno';

import { KeyfieldsEditorComponent } from './keyfields-editor/keyfields-editor.component';
import {CodemirrorModule} from 'ng2-codemirror';
import { EditorDialogComponent } from './editor.dialog/editor-dialog.component';


const routes: Routes = [
  { path: 'dataquality', redirectTo: 'dataquality/editing', pathMatch: 'full' },
  { path: 'dataquality/uploadfile', component: ConfigComponent },
  { path: 'dataquality/editing', component: EditingComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    EditingComponent,
    SourcesComponent,
    DatabasesComponent,
    MetricsComponent,
    ChecksComponent,
    TargetsComponent,
    MailsComponent,
    ConfigComponent,
    MenuComponent,
    FileSelectDirective,
    DialogConfirmYESNO,
    TargetTypePipe,
    ChecksDialogComponent,
    TargetDialogComponent,
    ConfigDowloadDialogComponent,
    DialogAlert,
    DialogConfirmYESNO,
    MetricsComposedDialogComponent,
    KeyfieldsEditorComponent,
    EditorDialogComponent
  ],
  entryComponents: [
    DatabasesComponent,
    DialogConfirmYESNO,
    MailsComponent,
    TargetsComponent,
    ChecksDialogComponent,
    TargetDialogComponent,
    ConfigDowloadDialogComponent,
    DialogAlert,
    DialogConfirmYESNO,
    MetricsComposedDialogComponent,
    KeyfieldsEditorComponent,
    EditorDialogComponent
  ],
  imports: [
    HttpModule,
    RouterModule.forRoot(routes),
    BrowserModule,
    FlexLayoutModule,
    MdToolbarModule,
    MdListModule,
    MdIconModule,
    MdButtonModule,
    FormsModule,
    MdInputModule,
    MdSlideToggleModule,
    MdSelectModule,
    BrowserAnimationsModule,
    MdExpansionModule,
    MdCheckboxModule,
    MdDatepickerModule,
    MdNativeDateModule,
    MdMenuModule,
    MdCardModule,
    MdDialogModule,
    MdTooltipModule,
    MdButtonToggleModule,
    MdSliderModule,
    MdSnackBarModule,
    MdPaginatorModule,
    MdAutocompleteModule,
    CodemirrorModule
  ],
  exports: [RouterModule],
  providers: [SourcesService, DatabaseService, MetricsService, ChecksService, TargetsService, ConfigService, MdIconRegistry,
    InteractionsService, MetasService, SearchService],
  bootstrap: [AppComponent]
})
export class AppModule { }
