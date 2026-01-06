// File: frontend/widget/src/loader.ts
// Builds to a single file clients include; it then pulls the real bundles and delegates.

type LoaderOptions = {
    baseUrl?: string;                 // optional override for where bundles live
    bundles?: string[];               // optional explicit bundle filenames
    mode?: 'es' | 'umd';              // how to load the bundles
};

const LOADER_FLAG = '__bt_loader_v1';

// determine directory of current script (works when hosted on GitHub Pages)
function getLoaderBase(): string {
    const cur = document.currentScript as HTMLScriptElement | null;
    if (cur && cur.src) {
        const u = new URL(cur.src, location.href);
        u.pathname = u.pathname.replace(/\/[^\/]*$/, '/');
        return u.href;
    }
    // fallback to a conventional dist folder relative to origin
    return location.origin + '/dist/';
}

function loadScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = url;
        s.async = false; // preserve order for UMD
        s.onload = () => resolve();
        s.onerror = () => reject(new Error('Failed to load ' + url));
        document.head.appendChild(s);
    });
}

async function fetchBundleManifest(base: string): Promise<string[] | null> {
    try {
        const res = await fetch(base + 'bundles.json', { cache: 'no-cache' });
        if (!res.ok) return null;
        const data = await res.json();
        return Array.isArray(data) ? data : null;
    } catch {
        return null;
    }
}

async function loadESModule(url: string): Promise<void> {
    // prevent bundlers from inlining the import; browsers will fetch the module
    await import(/* webpackIgnore: true */ url);
}

async function loadBundlesSequential(urls: string[], mode: 'es' | 'umd') {
    if (mode === 'es') {
        // load in parallel (or change to sequential if order matters)
        await Promise.all(urls.map(u => loadESModule(u)));
    } else {
        for (const u of urls) {
            await loadScript(u);
        }
    }
}

// Resolve bundle filenames/URLs using Option B (manifest) with a fallback
async function resolveBundles(base: string, mode: 'es' | 'umd', opts: LoaderOptions): Promise<string[]> {
    const defaultESMBundles = ['part-a.module.js', 'part-b.module.js', 'bugtracker.main.module.js'];
    const defaultUMDBundlesFallback = [
        'bugtracker.bundle.js',
        '335.bugtracker.bundle.js',
        '354.bugtracker.bundle.js'
    ];

    if (opts.bundles && opts.bundles.length) return opts.bundles;

    const manifest = await fetchBundleManifest(base);
    if (manifest && manifest.length) return manifest;

    return mode === 'es' ? defaultESMBundles : defaultUMDBundlesFallback;
}

/**
 * Public initialize exposed to clients.
 * Example usage by client:
 * <script src="https://.../bugtracker.loader.js"></script>
 * <script>BugTracker.initialize('my-project')</script>
 */
async function initialize(projectId: string, opts: LoaderOptions = {}): Promise<void> {
    const base = (opts.baseUrl || getLoaderBase()).replace(/\/+$/, '/') ;
    const mode = opts.mode || 'umd';

    try {
        const bundles = await resolveBundles(base, mode, opts);

        // Support manifest entries that may already be absolute URLs
        const urls = bundles.map(b => /^https?:\/\//i.test(b) ? b : base + b);

        await loadBundlesSequential(urls, mode);

        // After loading the real bundles the real BugTracker should be attached to window.BugTracker
        const globalAny = window as any;
        if (globalAny.BugTracker && !globalAny.BugTracker[LOADER_FLAG] && typeof globalAny.BugTracker.initialize === 'function') {
            // delegate to real implementation (may return a Promise)
            return globalAny.BugTracker.initialize(projectId);
        } else {
            // real bundle didn't expose the expected API — fail gracefully
            return Promise.reject(new Error('BugTracker not found after loading bundles'));
        }
    } catch (e) {
        // bubble up the error for client code to handle
        return Promise.reject(e);
    }
}

// expose loader as the initial global API
const loader = {
    initialize,
    [LOADER_FLAG]: true
};
(window as any).BugTracker = loader;

export default loader;
