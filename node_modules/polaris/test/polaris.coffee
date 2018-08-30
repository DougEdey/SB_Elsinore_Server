assert = require 'assert'
http = require 'http'
nodemailer = require 'nodemailer'
sinon = require 'sinon'
supertest = require 'supertest'

sinon.stub(nodemailer, 'createTransport').callsFake ->
  sendMail: (options, callback) ->
    callback null

polaris = require '../main'

describe 'Configuration', ->

  it 'Should allow setting config', ->
    assert polaris.config

    polaris.config.listen.host = '0.0.0.0'

    assert.equal '0.0.0.0', polaris.config.listen.host
    assert polaris.config.listen.port

  it 'Should load config from a file', ->
    polaris.loadConfig './config-example.json'

    assert.equal 'SMTP', polaris.config.transport.name

describe 'Request handler', ->
  app = null

  before (done) ->
    polaris.config.recipients.baz =
      to: ['foo@bar.com']
      redirect: 'http://example.com/'

    polaris.runServer (err, server) ->
      app = server
      done()

  it 'Should require recipient', ->
    supertest(app)
      .post('/')
      .send(from: 'foo', message: 'bar')
      .expect(400)

  it 'Should require from', ->
    supertest(app)
      .post('/')
      .send(recipient: 'baz', message: 'bar')
      .expect(400)

  it 'Should require message', ->
    supertest(app)
      .post('/')
      .send(recipient: 'baz', from: 'foo')
      .expect(400)

  it 'Should send a message', ->
    supertest(app)
      .post('/')
      .send(recipient: 'baz', from: 'foo', message: 'test')
      .expect(302)

  it 'Should send a message with attachments', ->
    supertest(app)
      .post('/')
      .field('recipient', 'baz')
      .field('from', 'foo')
      .field('message', 'test')
      .attach('README.md', './README.md')
      .expect(302)

describe 'Server', ->

  it 'Should start a server and listen', (done) ->
    listenCalled = false

    sinon.stub(http, 'createServer').callsFake ->
      listen: (port, host, listenDone) ->
        listenCalled = true
        listenDone()

    polaris.runServer ->
      assert http.createServer.called
      assert listenCalled

      http.createServer.restore()

      done()
