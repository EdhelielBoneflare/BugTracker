const path = require('path');

module.exports = {
    entry: './src/types.ts',
    output: {
        filename: 'bugtracker.bundle.js',
        path: path.resolve(__dirname, 'dist'),
        library: {
            name: 'BugTracker',
            type: 'umd'
        },
        globalObject: "typeof self !== 'undefined' ? self : this"
    },
    resolve: {
        extensions: ['.ts', '.js']
    },
    module: {
        rules: [
            {
                test: /\.ts$/,
                use: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    mode: 'production'
};