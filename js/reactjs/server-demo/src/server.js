var http = require('http'),
    browserify = require('browserify'),
    literalify = require('literalify'),
    React = require('react');

var App = require('./app');

http.createServer(function(req, res) {
  if (req.url == '/') {
    res.setHeader('Content-Type', 'text/html');
    var props = {
      items: [
        'Item 0',
        'Item 1'
      ]
    };
    
    var html = React.renderToStaticMarkup(
      <body>
        <div id="content" dangerouslySetInnerHTML={{__html: // 服务器先生成静态的 html 发送给客户端
          React.renderToString(<App items={props.items}/>)
        }} />
        <script dangerouslySetInnerHTML={{__html:
        'var APP_PROPS = ' + JSON.stringify(props) + ';' //这端, 动态发请求, 让客户端 React 处理
        }}/>
        <script src="//fb.me/react-0.13.3.min.js"/>
        <script src="/bundle.js"/>
      </body>
    );
    res.end(html);

  } else if (req.url == '/bundle.js') {
    res.setHeader('Content-Type', 'text/javascript');
    browserify()
      .add('./browser.js')
      .transform(literalify.configure({react: 'window.React'})) // 将 react 字符串 替换为 window.react
      .bundle()
      .pipe(res);

  } else {
    res.statusCode = 404;
    res.end();
  }
}).listen(3000, function(err) {
  if (err) throw err;
  console.log('Listening on 3000...');
})

