const path = require('path');
const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    entry: './src/index.ts',
    output: {
        filename: 'bugtracker.bundle.js',
        path: path.resolve(__dirname, 'dist'),
        library: {
            name: 'BugTracker',
            type: 'umd',
            export: 'default'
        },
        globalObject: "typeof self !== 'undefined' ? self : this",
        clean: true
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
    mode: 'production',
    devtool: false,
    optimization: {
        splitChunks: false,
        runtimeChunk: false,
        minimizer: [
            new TerserPlugin({
                extractComments: false,
                terserOptions: {
                    compress: {
                        drop_console: false
                    }
                }
            })
        ]
    },
    externals: {
        html2canvas: 'html2canvas'
    }
};