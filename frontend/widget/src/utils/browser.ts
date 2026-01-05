export function getBrowserInfo() {
    const ua = navigator.userAgent;
    let name = 'Unknown';
    let version = 'Unknown';
    const plugins = Array.from(navigator.plugins).map(p => p.name);

    // Browser detection
    if (ua.includes('Chrome') && !ua.includes('Edg')) {
        const match = ua.match(/Chrome\/(\d+\.\d+)/);
        name = 'Chrome';
        version = match ? match[1] : 'Unknown';
    } else if (ua.includes('Firefox')) {
        const match = ua.match(/Firefox\/(\d+\.\d+)/);
        name = 'Firefox';
        version = match ? match[1] : 'Unknown';
    } else if (ua.includes('Safari') && !ua.includes('Chrome')) {
        const match = ua.match(/Version\/(\d+\.\d+)/);
        name = 'Safari';
        version = match ? match[1] : 'Unknown';
    } else if (ua.includes('Edg')) {
        const match = ua.match(/Edg\/(\d+\.\d+)/);
        name = 'Edge';
        version = match ? match[1] : 'Unknown';
    }

    return { name, version, plugins };
}

export function getOSInfo() {
    const ua = navigator.userAgent;
    let name = 'Unknown';

    if (ua.includes('Windows')) name = 'Windows';
    else if (ua.includes('Mac')) name = 'macOS';
    else if (ua.includes('Linux')) name = 'Linux';
    else if (ua.includes('Android')) name = 'Android';
    else if (ua.includes('iOS') || ua.includes('iPhone') || ua.includes('iPad')) {
        name = 'iOS';
    }

    return { name };
}

export function getDeviceInfo() {
    const ua = navigator.userAgent;
    let type: 'desktop' | 'mobile' | 'tablet' = 'desktop';

    if (/Mobile|Android|iPhone|iPad|iPod/i.test(ua)) {
        type = 'mobile';
        if (/iPad/i.test(ua)) {
            type = 'tablet';
        }
    }

    return { type };
}

export function getConnectionInfo() {
    const connection = (navigator as any).connection;
    let type = 'unknown';

    if (connection) {
        type = connection.effectiveType || 'unknown';
    }

    return { type };
}