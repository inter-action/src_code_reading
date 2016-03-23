"use strict";

const qs = require('querystring');//nodejs module used to parse querystring
const url = require('url');

module.exports = {

  handleRedirectMessage(origin) {
    return qs.parse(url.parse(origin).query).requrl || origin;
  }
};
