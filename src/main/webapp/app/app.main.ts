import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { ProdConfig } from './blocks/config/prod.config';
import { ArtemisAppModule } from './app.module';

ProdConfig();

if (module['hot']) {
    module['hot'].accept();
    if ('production' !== process.env.NODE_ENV) {
        console.clear();
    }
}

platformBrowserDynamic()
    .bootstrapModule(ArtemisAppModule, { preserveWhitespaces: true })
    .then(platformRef => {})
    .catch(err => console.error(err));
