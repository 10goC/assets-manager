export interface AssetsManagerPlugin {
	getExternalStorageDir(): Promise<ExternalStorageDir>;
}

export interface ExternalStorageDir {
	path: string,
	uri: string
}