import { WebPlugin } from '@capacitor/core';
import type { AssetsManagerPlugin, ExternalStorageDir } from './definitions';

export class AssetsManagerWeb extends WebPlugin implements AssetsManagerPlugin {
	
	async getExternalStorageDir(): Promise<ExternalStorageDir> {
		return new Promise((resolve) => {
			resolve({
				path: '',
				uri: ''
			});
		});
	}

}
