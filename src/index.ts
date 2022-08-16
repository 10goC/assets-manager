import { registerPlugin } from '@capacitor/core';

import type { AssetsManagerPlugin } from './definitions';

const AssetsManager = registerPlugin<AssetsManagerPlugin>('AssetsManager', {
  web: () => import('./web').then(m => new m.AssetsManagerWeb()),
});

export * from './definitions';
export { AssetsManager };
