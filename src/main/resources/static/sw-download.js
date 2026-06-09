self.addEventListener('install', (event) => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});

const downloads = new Map();

self.addEventListener('message', (event) => {
    const { type, downloadId, filename } = event.data;

    if (type === 'REGISTER_DOWNLOAD') {
        const bufferedChunks = [];
        let controllerRef = null;
        let isClosed = false;

        const stream = new ReadableStream({
            start(controller) {
                controllerRef = controller;
                while (bufferedChunks.length > 0) {
                    controller.enqueue(bufferedChunks.shift());
                }
            }
        });

        downloads.set(downloadId, {
            stream,
            filename,
            enqueue(chunk) {
                if (isClosed) return;
                const data = new Uint8Array(chunk);
                if (controllerRef) {
                    controllerRef.enqueue(data);
                } else {
                    bufferedChunks.push(data);
                }
            },
            close() {
                isClosed = true;
                if (controllerRef) {
                    controllerRef.close();
                }
                downloads.delete(downloadId);
            },
            error(err) {
                isClosed = true;
                if (controllerRef) {
                    controllerRef.error(err);
                }
                downloads.delete(downloadId);
            }
        });
    } else if (type === 'CHUNK') {
        const download = downloads.get(event.data.downloadId);
        if (download) {
            download.enqueue(event.data.chunk);
        }
    } else if (type === 'COMPLETE') {
        const download = downloads.get(event.data.downloadId);
        if (download) {
            download.close();
        }
    } else if (type === 'ABORT') {
        const download = downloads.get(event.data.downloadId);
        if (download) {
            download.error(new Error('Aborted'));
        }
    }
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    if (url.pathname === '/sw-download') {
        const downloadId = url.searchParams.get('id');
        event.respondWith(
            new Promise((resolve) => {
                let attempts = 0;
                const check = () => {
                    const download = downloads.get(downloadId);
                    if (download && download.stream) {
                        resolve(new Response(download.stream, {
                            headers: {
                                'Content-Type': 'application/octet-stream',
                                'Content-Disposition': `attachment; filename*=UTF-8''${encodeURIComponent(download.filename)}`
                            }
                        }));
                    } else if (attempts < 100) {
                        attempts++;
                        setTimeout(check, 10);
                    } else {
                        resolve(new Response('Download not ready', { status: 404 }));
                    }
                };
                check();
            })
        );
    }
});
