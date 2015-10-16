//============================== ==============================
//
// webpack 的 multiple entry points 的一个实例
//   webpack 会动态分析依赖, 加入 a.js 和 b.js 引用了同一个库, webpack 分析之后, 会将其打包到 *.chunk.js 中
//   之后便异步加载这个 *.chunk.js
//   有的时候, 我们已经加载了这个公共库, 并不需要异步加载, 这个时候, webpack 会直接 require 这个公共库。
//   此时 webpack 并不会动态生成 *.chunk.js 类似的文件
//
//   当然所有的通过 required 函数引用的库, 必须通过 webpack 统一处理, 以便在 modules 中注册。
//      详情见：
//          [](https://github.com/webpack/webpack/tree/master/examples/multiple-entry-points)
//
//       webpack.config.js:
//
//       var path = require("path");
//       var CommonsChunkPlugin = require("../../lib/optimize/CommonsChunkPlugin");
//       module.exports = {
//         entry: {
//             pageA: "./pageA",
//             pageB: "./pageB"
//         },
//         output: {
//             path: path.join(__dirname, "js"),
//             filename: "[name].bundle.js",
//             chunkFilename: "[id].chunk.js"
//         },
//         plugins: [
//             new CommonsChunkPlugin("commons.js") // 公用 commons.js
//         ]
//       }
//
//   TODOS:
//      类似的公用多个 像 commons.js 这种情形, 是如何处理和配置的
//
//============================== ==============================


// 所有加载的模块都会放到 modules 里边


/******/ (function(modules) { // webpackBootstrap
/******/    // install a JSONP callback for chunk loading
/******/    var parentJsonpFunction = window["webpackJsonp"];
/******/    window["webpackJsonp"] = function webpackJsonpCallback(chunkIds, moreModules) {
/******/        // add "moreModules" to the modules object,
/******/        // then flag all "chunkIds" as loaded and fire callback
/******/        var moduleId, chunkId, i = 0, callbacks = [];
/******/        for(;i < chunkIds.length; i++) {
/******/            chunkId = chunkIds[i];
/******/            if(installedChunks[chunkId])//如果这个模块存在回调， 合并回调
/******/                callbacks.push.apply(callbacks, installedChunks[chunkId]);
/******/            installedChunks[chunkId] = 0;//移除这个模块的回调存储
/******/        }
// 储存模块数组 (moreModules) 到 modules 变量中, 即注册 moreModules into modules
/******/        for(moduleId in moreModules) {
/******/            modules[moduleId] = moreModules[moduleId];
/******/        }

// 注意这个地方, 这个地方如果这段函数执行的时候, window['webpackJsonp'] 没有指定的时候, 这个地方是 null.
// 所以这个地方并不会执行, 当然也就不存在死循环, 这个地方应该是给外部的一个扩展。
/******/        if(parentJsonpFunction) parentJsonpFunction(chunkIds, moreModules);

// 初始化所有依赖这个模块的 模块
/******/        while(callbacks.length)
/******/            callbacks.shift().call(null, __webpack_require__);

// 如果 moreModule 和 modules 中 index = 0 的位置存在模块， 则初始化这个模块
/******/        if(moreModules[0]) {
/******/            installedModules[0] = 0;
/******/            return __webpack_require__(0);
/******/        }
/******/    };

/******/    // The module cache
/******/    var installedModules = {};

/******/    // object to store loaded and loading chunks
/******/    // "0" means "already loaded"， 这个模块已经被加载了，是否初始化过不一定
/******/    // Array means "loading", array contains callbacks， 如果是 数组 表示这个模块正在被请求
/******/    var installedChunks = {
/******/        3:0
/******/    };

//============================== ==============================
// 触发各个 module 的 初始化过程
//
//============================== ==============================

/******/    // The require function
/******/    function __webpack_require__(moduleId) {

/******/        // Check if module is in cache
/******/        if(installedModules[moduleId])
/******/            return installedModules[moduleId].exports;

/******/        // Create a new module (and put it into the cache)
/******/        var module = installedModules[moduleId] = {
/******/            exports: {},
/******/            id: moduleId,
/******/            loaded: false
/******/        };

/******/        // Execute the module function
/******/        modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/        // Flag the module as loaded
/******/        module.loaded = true;

/******/        // Return the exports of the module
/******/        return module.exports;
/******/    }

//============================== ==============================
//
// 如果这个模块已经加载, 则调用 callback (各个模块的初始化)
// 如果这个模块还没有加载完成，则将模块依赖的回调保存，储存到 installedChunks 中
// 如果这个模块还没有被请求过, 保存模块的依赖回调, 异步请求加载这个 js 模块
//
// 注意这个地方的 chunkId, 和 moduleId 是不一样的
//
//============================== ==============================
/******/    // This file contains only the entry chunk.
/******/    // The chunk loading function for additional chunks
/******/    __webpack_require__.e = function requireEnsure(chunkId, callback) {
/******/        // "0" is the signal for "already loaded"
/******/        if(installedChunks[chunkId] === 0)
/******/            return callback.call(null, __webpack_require__);

/******/        // an array means "currently loading".
/******/        if(installedChunks[chunkId] !== undefined) {
/******/            installedChunks[chunkId].push(callback);
/******/        } else {
/******/            // start chunk loading
/******/            installedChunks[chunkId] = [callback];
/******/            var head = document.getElementsByTagName('head')[0];
/******/            var script = document.createElement('script');
/******/            script.type = 'text/javascript';
/******/            script.charset = 'utf-8';
/******/            script.async = true;

/******/            script.src = __webpack_require__.p + "" + chunkId + ".chunk.js";
/******/            head.appendChild(script);
/******/        }
/******/    };

/******/    // expose the modules object (__webpack_modules__)
/******/    __webpack_require__.m = modules;

/******/    // expose the module cache
/******/    __webpack_require__.c = installedModules;

/******/    // __webpack_public_path__
/******/    __webpack_require__.p = "js/";
/******/ })
/************************************************************************/
/******/ ([
/* 0 */,
/* 1 */
/*!*******************!*\
  !*** ./common.js ***!
  \*******************/
/***/ function(module, exports) {

    module.exports = "Common";

/***/ }
/******/ ]);