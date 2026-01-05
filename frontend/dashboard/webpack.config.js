const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');

module.exports = (env, argv) => {
    const isProduction = argv.mode === 'production';

    return {
        entry: './src/index.tsx',
        output: {
            filename: isProduction ? 'bundle.[contenthash].js' : 'bundle.js',
            path: path.resolve(__dirname, 'dist'),
            publicPath: '/',
            clean: true
        },
        resolve: {
            extensions: ['.tsx', '.ts', '.js', '.jsx'],
        },
        module: {
            rules: [
                {
                    test: /\.tsx?$/,
                    use: {
                        loader: 'ts-loader',
                        options: {
                            transpileOnly: !isProduction
                        }
                    },
                    exclude: /node_modules/
                },
                {
                    test: /\.css$/,
                    use: ['style-loader', 'css-loader']
                }
            ]
        },
        plugins: [
            new HtmlWebpackPlugin({
                template: './public/index.html',
                filename: 'index.html',
                inject: 'body',
                minify: isProduction ? {
                    removeComments: true,
                    collapseWhitespace: true,
                    removeRedundantAttributes: true,
                    useShortDoctype: true,
                    removeEmptyAttributes: true,
                    removeStyleLinkTypeAttributes: true,
                    keepClosingSlash: true,
                    minifyJS: true,
                    minifyCSS: true,
                    minifyURLs: true
                } : false
            }),

            new webpack.DefinePlugin({
                'process.env.REACT_APP_API_URL': JSON.stringify(process.env.REACT_APP_API_URL || 'http://localhost:8080/api'),
                'process.env.NODE_ENV': JSON.stringify(isProduction ? 'production' : 'development')
            })
        ],
        devServer: {
            static: {
                directory: path.join(__dirname, 'dist'),
            },
            compress: true,
            port: 3000,
            hot: true,
            historyApiFallback: true,
            client: {
                logging: 'info',
                overlay: {
                    errors: true,
                    warnings: false,
                },
                progress: true,
            },
            proxy: {
                '/api': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                }
            }
        },
        devtool: isProduction ? 'source-map' : 'eval-source-map',
        mode: isProduction ? 'production' : 'development',
        stats: {
            children: true,
            errorDetails: true
        }
    };
};