var webpack = require('webpack');
var path = require('path');

module.exports = {
  entry: {
    app: ['./app/main.js']
  },
  output: {
    path: path.resolve(__dirname, "build/dist"),
    filename: 'browser-bundle.js'
  },
  devtool: 'source-map',
  module: {
    loaders: [
      {
        exclude: /(node_modules|bower_components)/,
        test: /\.js|jsx$/,
        loader: 'babel-loader',
        query: {
          cacheDirectory: true,
          plugins: [
            'transform-decorators-legacy'
          ],
          presets: ['es2015', 'react', 'stage-1']
        }
      },
      {
        exclude: /(node_modules|bower_components)/,
        test: /\.scss$/,
        loader: 'style!css!sass'
      },
      {
        exclude: /(node_modules|bower_components)/,
        test: /\.svg|png|jpg|ico$/,
        loader: 'file-loader'
      },
      {
        test: /\.json$/,
        loader: 'json-loader'

      }
    ]
  },
  resolve: {
    root: [
      path.resolve('./app/service'),
      path.resolve('./app/components'),
      path.resolve('./app/utils')
    ]
  },
  // Require the webpack and react-hot-loader plugins
  plugins: [
    new webpack.HotModuleReplacementPlugin()
  ]
};
